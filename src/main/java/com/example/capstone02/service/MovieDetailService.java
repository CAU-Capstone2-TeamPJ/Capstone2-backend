package com.example.capstone02.service;

import com.example.capstone02.dto.MovieDetailDto;
import com.example.capstone02.dto.MovieDetailResponseDto;
import com.example.capstone02.entity.Movie;
import com.example.capstone02.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieDetailService {
    private final WebClient webClient;
    private final MovieRepository movieRepository;

    @Value("${tmdb.api-key}")
    private String apiKey;

    @Value("${tmdb.base-url}")
    private String baseUrl;

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

        return webClient.get()
                .uri(url)
                .header("accept", "application/json")
                .retrieve()
                .bodyToMono(MovieDetailResponseDto.class)
                .doOnNext(response -> log.info("TMDB API 응답: images={}, crew={}",
                        response.getImages() != null ?
                                (response.getImages().getBackdrops() != null ? response.getImages().getBackdrops().size() : 0) : 0,
                        response.getCredits() != null ?
                                (response.getCredits().getCrew() != null ? response.getCredits().getCrew().size() : 0) : 0))
                .map(this::convertToEntity)
                .map(movie -> movieRepository.save(movie))
                .map(MovieDetailDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public MovieDetailDto getMovieDetail(Long movieId) {
        return movieRepository.findById(movieId)
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