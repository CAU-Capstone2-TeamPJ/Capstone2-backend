package com.example.capstone02.service;

import com.example.capstone02.dto.MovieDetailDto;
import com.example.capstone02.dto.MovieDetailResponseDto;
import com.example.capstone02.entity.Movie;
import com.example.capstone02.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class MovieDetailService {
    private final WebClient webClient;
    private final WebClient longTimeoutWebClient;
    private final MovieRepository movieRepository;

    @Value("${tmdb.api-key}")
    private String apiKey;

    @Value("${tmdb.base-url}")
    private String baseUrl;

    // 영화 정보 처리 상태 조회를 위한 Map (메모리에 임시 저장)
    private static final Map<Long, Boolean> processingStatusMap = new HashMap<>();

    public MovieDetailService(
            WebClient webClient,
            @Qualifier("longTimeoutWebClient") WebClient longTimeoutWebClient,
            MovieRepository movieRepository) {
        this.webClient = webClient;
        this.longTimeoutWebClient = longTimeoutWebClient;
        this.movieRepository = movieRepository;
    }

    /**
     * TMDB API에서 영화 정보를 가져와 저장 (기본 타임아웃 사용)
     */
    @Transactional
    public Mono<MovieDetailDto> fetchAndSaveMovieDetail(Long movieId) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/movie/" + movieId)
                .queryParam("api_key", apiKey)
                .queryParam("language", "ko-KR")
                .queryParam("append_to_response", "credits,images")
                .queryParam("include_image_language", "en,null,ko")
                .build()
                .toUriString();

        log.info("TMDB API 요청 URL: {}", url);

        // API 요청 실패 시 재시도 로직 추가
        return webClient.get()
                .uri(url)
                .header("accept", "application/json")
                .retrieve()
                .bodyToMono(MovieDetailResponseDto.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(3))
                        .maxBackoff(Duration.ofSeconds(10))
                        .doBeforeRetry(signal -> log.warn("TMDB API 요청 재시도: {}/3", signal.totalRetries() + 1)))
                .doOnNext(response -> log.info("TMDB API 응답: images={}, crew={}",
                        response.getImages() != null ?
                                (response.getImages().getBackdrops() != null ? response.getImages().getBackdrops().size() : 0) : 0,
                        response.getCredits() != null ?
                                (response.getCredits().getCrew() != null ? response.getCredits().getCrew().size() : 0) : 0))
                .map(this::convertToEntity)
                .map(movie -> movieRepository.save(movie))
                .map(MovieDetailDto::fromEntity);
    }

    /**
     * 영화 정보를 비동기적으로 가져와 저장 (긴 타임아웃 사용)
     */
    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<MovieDetailDto> fetchAndSaveMovieDetailAsync(Long movieId) {
        log.info("영화 ID {}에 대한 정보 비동기 요청 시작", movieId);

        // 처리 상태 '처리 중'으로 설정
        setProcessingStatus(movieId, false);

        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/movie/" + movieId)
                        .queryParam("api_key", apiKey)
                        .queryParam("language", "ko-KR")
                        .queryParam("append_to_response", "credits,images")
                        .queryParam("include_image_language", "en,null,ko")
                        .build()
                        .toUriString();

                log.info("TMDB API 비동기 요청 URL: {}", url);

                // 긴 타임아웃 설정의 WebClient 사용
                MovieDetailResponseDto response = longTimeoutWebClient.get()
                        .uri(url)
                        .header("accept", "application/json")
                        .retrieve()
                        .bodyToMono(MovieDetailResponseDto.class)
                        .retryWhen(Retry.backoff(3, Duration.ofSeconds(5))
                                .maxBackoff(Duration.ofMinutes(2))
                                .doBeforeRetry(signal ->
                                        log.warn("TMDB API 비동기 요청 재시도 {}/3: {}",
                                                signal.totalRetries() + 1,
                                                signal.failure().getMessage())))
                        .block(); // 동기식 블록, 타임아웃은 WebClient 설정을 따름

                log.info("TMDB API 비동기 요청 완료");

                Movie movie = convertToEntity(response);
                Movie savedMovie = movieRepository.save(movie);

                // 처리 상태 '완료'로 설정
                setProcessingStatus(movieId, true);

                return MovieDetailDto.fromEntity(savedMovie);
            } catch (Exception e) {
                log.error("영화 ID {}에 대한 정보 비동기 처리 중 오류: {}", movieId, e.getMessage(), e);

                // 오류 발생해도 처리 상태 '완료'로 설정
                setProcessingStatus(movieId, true);

                throw new RuntimeException("영화 정보 처리 실패: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 영화 ID로 영화 정보 처리 상태 조회
     */
    public boolean isProcessingComplete(Long movieId) {
        return processingStatusMap.getOrDefault(movieId, true); // 기본값은 완료 상태
    }

    /**
     * 영화 ID로 영화 정보 처리 상태 설정
     */
    public void setProcessingStatus(Long movieId, boolean isComplete) {
        processingStatusMap.put(movieId, isComplete);
    }

    /**
     * 영화 ID로 영화 정보를 비동기적으로 요청하고 상태 추적
     */
    public Map<String, Object> requestMovieDetailAsync(Long movieId) {
        // 이미 처리 중인 경우 중복 요청 방지
        if (processingStatusMap.getOrDefault(movieId, true) == false) {
            log.info("영화 ID {}에 대한 정보 처리가 이미 진행 중입니다", movieId);
            return Map.of(
                    "movieId", movieId,
                    "processing", true,
                    "message", "데이터 처리가 진행 중입니다."
            );
        }

        // DB에 이미 존재하는지 확인
        Optional<Movie> existingMovie = movieRepository.findById(movieId);
        if (existingMovie.isPresent()) {
            return Map.of(
                    "movieId", movieId,
                    "processing", false,
                    "hasData", true,
                    "message", "데이터가 이미 존재합니다."
            );
        }

        // 처리 상태를 '처리 중'으로 설정
        setProcessingStatus(movieId, false);

        // 비동기 요청 실행
        fetchAndSaveMovieDetailAsync(movieId)
                .thenAccept(movieDetail -> {
                    log.info("영화 ID {}에 대한 정보 비동기 처리 완료", movieId);
                    setProcessingStatus(movieId, true); // 처리 완료로 상태 변경
                })
                .exceptionally(ex -> {
                    log.error("영화 ID {}에 대한 정보 비동기 처리 실패: {}", movieId, ex.getMessage());
                    setProcessingStatus(movieId, true); // 오류 발생해도 처리 완료로 표시
                    return null;
                });

        return Map.of(
                "movieId", movieId,
                "processing", true,
                "message", "데이터 요청이 시작되었습니다. 몇 분 후에 확인해주세요."
        );
    }

    /**
     * 영화 ID로 영화 상세 정보 조회 (촬영지 국가 정보 포함)
     */
    @Transactional(readOnly = true)
    public MovieDetailDto getMovieDetail(Long movieId) {
        return movieRepository.findByIdWithFilmingLocations(movieId)
                .map(MovieDetailDto::fromEntity)
                .orElseThrow(() -> new RuntimeException("영화를 찾을 수 없습니다: " + movieId));
    }

    private Movie convertToEntity(MovieDetailResponseDto dto) {
        // 날짜 변환
        LocalDate releaseDate = null;
        if (dto.getReleaseDate() != null && !dto.getReleaseDate().isEmpty()) {
            try {
                releaseDate = LocalDate.parse(dto.getReleaseDate());
            } catch (Exception e) {
                log.warn("날짜 파싱 오류: {}", dto.getReleaseDate(), e);
                releaseDate = null;
            }
        }

        // 장르 변환
        List<Movie.Genre> genres = dto.getGenres() != null ?
                dto.getGenres().stream()
                        .map(genre -> new Movie.Genre(genre.getId(), genre.getName()))
                        .collect(Collectors.toList()) :
                new ArrayList<>();

        // 캐스트 변환
        List<Movie.Cast> cast = new ArrayList<>();
        if (dto.getCredits() != null && dto.getCredits().getCast() != null) {
            cast = dto.getCredits().getCast().stream()
                    .map(c -> new Movie.Cast(c.getId(), c.getName(), c.getCharacter(), c.getProfilePath()))
                    .collect(Collectors.toList());
        }

        // 감독 정보 찾기
        String director = "";
        String directorProfilePath = null;
        Long directorId = null;

        if (dto.getCredits() != null && dto.getCredits().getCrew() != null) {
            Optional<MovieDetailResponseDto.Crew> directorOpt = dto.getCredits().getCrew().stream()
                    .filter(c -> "Directing".equals(c.getDepartment()) && "Director".equals(c.getJob()))
                    .findFirst();

            if (directorOpt.isPresent()) {
                MovieDetailResponseDto.Crew directorCrew = directorOpt.get();
                director = directorCrew.getName();
                directorProfilePath = directorCrew.getProfilePath();
                directorId = directorCrew.getId();
                log.info("감독 정보 찾음: {}", director);
            } else {
                log.info("감독 정보를 찾을 수 없음");
            }
        }

        // 이미지 변환
        List<Movie.MovieImage> images = new ArrayList<>();

        // 백드롭 이미지 처리
        if (dto.getImages() != null && dto.getImages().getBackdrops() != null && !dto.getImages().getBackdrops().isEmpty()) {
            log.info("백드롭 이미지 수: {}", dto.getImages().getBackdrops().size());

            List<Movie.MovieImage> backdrops = dto.getImages().getBackdrops().stream()
                    .map(image -> new Movie.MovieImage(
                            image.getFilePath(),
                            image.getAspectRatio(),
                            image.getHeight(),
                            image.getWidth(),
                            image.getVoteAverage(),
                            image.getVoteCount(),
                            Movie.MovieImage.ImageType.BACKDROP
                    ))
                    .collect(Collectors.toList());
            images.addAll(backdrops);
        } else {
            log.warn("백드롭 이미지 없음");
        }

        // 포스터 이미지 처리
        if (dto.getImages() != null && dto.getImages().getPosters() != null && !dto.getImages().getPosters().isEmpty()) {
            log.info("포스터 이미지 수: {}", dto.getImages().getPosters().size());

            List<Movie.MovieImage> posters = dto.getImages().getPosters().stream()
                    .map(image -> new Movie.MovieImage(
                            image.getFilePath(),
                            image.getAspectRatio(),
                            image.getHeight(),
                            image.getWidth(),
                            image.getVoteAverage(),
                            image.getVoteCount(),
                            Movie.MovieImage.ImageType.POSTER
                    ))
                    .collect(Collectors.toList());
            images.addAll(posters);
        } else {
            log.warn("포스터 이미지 없음");
        }

        // 이미지가 없는 경우에도 기본 포스터/백드롭 경로는 있을 수 있음
        if (images.isEmpty() && dto.getBackdropPath() != null) {
            log.info("기본 백드롭 이미지 추가: {}", dto.getBackdropPath());
            images.add(new Movie.MovieImage(
                    dto.getBackdropPath(),
                    1.778, // 기본 비율
                    1080,  // 기본 높이
                    1920,  // 기본 너비
                    0.0,   // 기본 평점
                    0,     // 기본 투표수
                    Movie.MovieImage.ImageType.BACKDROP
            ));
        }

        if (images.isEmpty() && dto.getPosterPath() != null) {
            log.info("기본 포스터 이미지 추가: {}", dto.getPosterPath());
            images.add(new Movie.MovieImage(
                    dto.getPosterPath(),
                    0.667, // 기본 비율
                    1500,  // 기본 높이
                    1000,  // 기본 너비
                    0.0,   // 기본 평점
                    0,     // 기본 투표수
                    Movie.MovieImage.ImageType.POSTER
            ));
        }

        return Movie.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .originalTitle(dto.getOriginalTitle())
                .overview(dto.getOverview())
                .posterPath(dto.getPosterPath())
                .backdropPath(dto.getBackdropPath())
                .releaseDate(releaseDate)
                .voteAverage(dto.getVoteAverage())
                .voteCount(dto.getVoteCount())
                .director(director)
                .directorProfilePath(directorProfilePath)
                .directorId(directorId)
                .genres(genres)
                .cast(cast)
                .images(images)
                .build();
    }
}