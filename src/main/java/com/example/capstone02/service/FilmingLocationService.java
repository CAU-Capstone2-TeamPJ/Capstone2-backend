package com.example.capstone02.service;

import com.example.capstone02.dto.FilmingLocationDetailDto;
import com.example.capstone02.dto.FilmingLocationResponseDto;
import com.example.capstone02.dto.MovieInfoRequestDto;
import com.example.capstone02.entity.FilmingLocation;
import com.example.capstone02.entity.Movie;
import com.example.capstone02.repository.FilmingLocationRepository;
import com.example.capstone02.repository.MovieRepository;
import com.google.maps.model.LatLng;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FilmingLocationService {

    private final WebClient webClient;
    private final WebClient longTimeoutWebClient;
    private final MovieRepository movieRepository;
    private final FilmingLocationRepository filmingLocationRepository;
    private final GoogleMapsService googleMapsService;

    @Value("${python.server.url}")
    private String pythonServerUrl;

    // 요청 로그를 저장할 맵 추가
    private static final Map<Long, List<String>> requestLogsMap = new HashMap<>();

    public FilmingLocationService(
            WebClient webClient,
            @Qualifier("longTimeoutWebClient") WebClient longTimeoutWebClient,
            MovieRepository movieRepository,
            FilmingLocationRepository filmingLocationRepository,
            GoogleMapsService googleMapsService) {
        this.webClient = webClient;
        this.longTimeoutWebClient = longTimeoutWebClient;
        this.movieRepository = movieRepository;
        this.filmingLocationRepository = filmingLocationRepository;
        this.googleMapsService = googleMapsService;

        log.info("FilmingLocationService 초기화 완료 - Python Server URL: {}", pythonServerUrl);
        log.info("WebClient 타임아웃 설정: 일반={}, 장시간={}",
                getWebClientTimeout(webClient), getWebClientTimeout(longTimeoutWebClient));
    }

    /**
     * WebClient의 타임아웃 설정을 확인하는 메서드
     */
    private String getWebClientTimeout(WebClient client) {
        try {
            Object connector = client.mutate().build();
            // 실제로는 WebClient의 내부 구현을 직접 확인하기 어려워 "설정값을 확인할 수 없음"을 반환
            return "설정값을 확인할 수 없음";
        } catch (Exception e) {
            return "확인 오류: " + e.getMessage();
        }
    }

    /**
     * 영화 ID로 촬영지 정보를 파이썬 서버에서 가져와 저장 (비동기 처리 추가)
     */
    @Async("taskExecutor")
    @Transactional
    public CompletableFuture<List<FilmingLocation>> fetchAndSaveFilmingLocationsAsync(Long movieId) {
        String logPrefix = "[비동기처리][" + movieId + "]";
        log.info("{} 영화 ID {}에 대한 촬영지 정보 비동기 요청 시작 - 시간: {}",
                logPrefix, movieId, LocalDateTime.now());

        addRequestLog(movieId, String.format("비동기 요청 시작 - %s", LocalDateTime.now()));

        return CompletableFuture.supplyAsync(() -> {
            try {
                addRequestLog(movieId, String.format("CompletableFuture 실행 시작 - %s", LocalDateTime.now()));
                List<FilmingLocation> result = fetchAndSaveFilmingLocations(movieId);
                addRequestLog(movieId, String.format("CompletableFuture 실행 완료 - %s - %d개 장소",
                        LocalDateTime.now(), result.size()));
                return result;
            } catch (Exception e) {
                log.error("{} 영화 ID {}에 대한 촬영지 정보 비동기 처리 중 오류: {}",
                        logPrefix, movieId, e.getMessage(), e);
                addRequestLog(movieId, String.format("비동기 처리 오류 - %s - %s",
                        LocalDateTime.now(), e.getMessage()));
                throw new RuntimeException("촬영지 정보 처리 실패: " + e.getMessage(), e);
            }
        });
    }

    /**
     * 영화 ID로 촬영지 정보를 파이썬 서버에서 가져와 저장 (동기 처리)
     */
    @Transactional
    public List<FilmingLocation> fetchAndSaveFilmingLocations(Long movieId) {
        String logPrefix = "[동기처리][" + movieId + "]";
        log.info("{} 영화 ID {}에 대한 촬영지 정보 가져오기 시작 - 시간: {}",
                logPrefix, movieId, LocalDateTime.now());
        addRequestLog(movieId, String.format("동기 요청 시작 - %s", LocalDateTime.now()));

        try {
            // 1. 영화 정보 조회
            Movie movie = movieRepository.findById(movieId)
                    .orElseThrow(() -> new RuntimeException("영화를 찾을 수 없습니다: " + movieId));
            log.debug("{} 영화 정보 조회 완료: {}", logPrefix, movie.getTitle());
            addRequestLog(movieId, String.format("영화 정보 조회 완료 - %s", LocalDateTime.now()));

            // 2. 파이썬 서버에 전송할 영화 정보 준비
            MovieInfoRequestDto requestDto = MovieInfoRequestDto.builder()
                    .id(movie.getId())
                    .title(movie.getTitle())
                    .director(movie.getDirector())
                    .releaseDate(movie.getReleaseDate() != null ?
                            movie.getReleaseDate().format(DateTimeFormatter.ISO_DATE) : null)
                    .build();
            log.debug("{} 파이썬 서버 요청 정보 생성 완료: {}", logPrefix, requestDto);
            addRequestLog(movieId, String.format("요청 정보 준비 완료 - %s - 제목: %s",
                    LocalDateTime.now(), movie.getTitle()));

            // 3. 파이썬 서버에 요청하여 촬영지 정보 가져오기 (타임아웃 증가 및 재시도 로직 추가)
            log.info("{} 파이썬 서버에 요청 시작: {} - 시간: {}",
                    logPrefix, pythonServerUrl, LocalDateTime.now());
            addRequestLog(movieId, String.format("파이썬 서버 요청 시작 - %s - URL: %s",
                    LocalDateTime.now(), pythonServerUrl));

            LocalDateTime requestStartTime = LocalDateTime.now();

            // 재시도 설정 변경: 초기 대기시간 1분(60초), 최대 백오프 5분
            FilmingLocationResponseDto responseDto = longTimeoutWebClient.post()
                    .uri(pythonServerUrl + "/movies")
                    .bodyValue(requestDto)
                    .retrieve()
                    .bodyToMono(FilmingLocationResponseDto.class)
                    .onErrorResume(WebClientResponseException.class, ex -> {
                        String errorBody = ex.getResponseBodyAsString();
                        log.error("{} 파이썬 서버 응답 오류: 상태 코드 {}, 응답 본문: {}",
                                logPrefix, ex.getStatusCode(), errorBody);
                        addRequestLog(movieId, String.format("파이썬 서버 응답 오류 - %s - 상태 코드: %s - 본문: %s",
                                LocalDateTime.now(), ex.getStatusCode(), errorBody));
                        return Mono.error(ex);
                    })
                    .retryWhen(Retry.backoff(3, Duration.ofMinutes(1)) // 초기 대기시간 1분, 3회 재시도
                            .maxBackoff(Duration.ofMinutes(5)) // 최대 백오프 5분으로 설정
                            .filter(ex -> !(ex instanceof WebClientResponseException) ||
                                    (((WebClientResponseException) ex).getStatusCode().is5xxServerError() ||
                                            ((WebClientResponseException) ex).getStatusCode() == HttpStatus.TOO_MANY_REQUESTS))
                            .doBeforeRetry(retrySignal -> {
                                Throwable failure = retrySignal.failure();
                                String errorMessage = (failure instanceof WebClientResponseException) ?
                                        "상태 코드: " + ((WebClientResponseException) failure).getStatusCode() :
                                        failure.getMessage();

                                log.warn("{} 파이썬 서버 요청 재시도 {}/3 ({}분 후): {}",
                                        logPrefix,
                                        retrySignal.totalRetries() + 1,
                                        1 * Math.pow(2, retrySignal.totalRetries()) > 5 ? 5 : 1 * Math.pow(2, retrySignal.totalRetries()),
                                        errorMessage);
                                addRequestLog(movieId, String.format("재시도 %d/3 - %s - %s분 후 재시도",
                                        retrySignal.totalRetries() + 1, LocalDateTime.now(),
                                        1 * Math.pow(2, retrySignal.totalRetries()) > 5 ? 5 : 1 * Math.pow(2, retrySignal.totalRetries())));
                            }))
                    .block(); // 동기 처리, 타임아웃은 WebClient 설정에 따름

            LocalDateTime requestEndTime = LocalDateTime.now();
            Duration requestDuration = Duration.between(requestStartTime, requestEndTime);

            log.info("{} 파이썬 서버 요청 완료 - 소요 시간: {}초",
                    logPrefix, requestDuration.getSeconds());
            addRequestLog(movieId, String.format("파이썬 서버 요청 완료 - %s - 소요 시간: %d초",
                    LocalDateTime.now(), requestDuration.getSeconds()));

            if (responseDto == null || responseDto.getLocations() == null || responseDto.getLocations().isEmpty()) {
                log.warn("{} 영화 ID {}에 대한 촬영지 정보가 없습니다", logPrefix, movieId);
                addRequestLog(movieId, String.format("촬영지 정보 없음 - %s", LocalDateTime.now()));
                return List.of();
            }

            log.info("{} 영화 ID {}에 대한 촬영지 {}개를 가져왔습니다",
                    logPrefix, movieId, responseDto.getLocations().size());
            addRequestLog(movieId, String.format("촬영지 정보 %d개 조회 성공 - %s",
                    responseDto.getLocations().size(), LocalDateTime.now()));

            // 4. 기존 촬영지 정보 삭제 (업데이트 시)
            if (filmingLocationRepository.existsByMovieId(movieId)) {
                log.info("{} 영화 ID {}의 기존 촬영지 정보를 삭제합니다", logPrefix, movieId);
                filmingLocationRepository.deleteAllByMovieId(movieId);
                addRequestLog(movieId, String.format("기존 촬영지 정보 삭제 - %s", LocalDateTime.now()));
            }

            // 5. 새로운 촬영지 정보 저장
            List<FilmingLocation> filmingLocations = responseDto.getLocations().stream()
                    .map(locationInfo -> {
                        FilmingLocation location = FilmingLocation.builder()
                                .movie(movie)
                                .name(locationInfo.getName())
                                .country(locationInfo.getCountry())
                                .description(locationInfo.getDescription())
                                .address(locationInfo.getAddress())
                                .durationTime(locationInfo.getDurationTime())
                                .mentionRate(locationInfo.getMentionRate())
                                .mentionCount(locationInfo.getMentionCount())
                                .build();

                        // 추천 키워드 설정
                        if (locationInfo.getRecommendationKeywords() != null) {
                            location.setRecommendationKeywords(locationInfo.getRecommendationKeywords());
                        }

                        // 주변 키워드 설정
                        if (locationInfo.getNearbyKeywords() != null) {
                            location.setNearbyKeywords(locationInfo.getNearbyKeywords());
                        }

                        return location;
                    })
                    .collect(Collectors.toList());

            List<FilmingLocation> savedLocations = filmingLocationRepository.saveAll(filmingLocations);
            log.info("{} 영화 ID {}에 대한 촬영지 정보 {}개 저장 완료 - 시간: {}",
                    logPrefix, movieId, savedLocations.size(), LocalDateTime.now());
            addRequestLog(movieId, String.format("촬영지 정보 %d개 저장 완료 - %s",
                    savedLocations.size(), LocalDateTime.now()));

            return savedLocations;
        } catch (Exception e) {
            log.error("{} 영화 ID {}에 대한 촬영지 정보 처리 중 예외 발생: {}",
                    logPrefix, movieId, e.getMessage(), e);
            addRequestLog(movieId, String.format("예외 발생 - %s - %s",
                    LocalDateTime.now(), e.getMessage()));
            throw e; // 예외 다시 던지기
        }
    }

    /**
     * 영화 ID로 저장된 촬영지 정보 조회
     */
    @Transactional(readOnly = true)
    public List<FilmingLocation> getFilmingLocationsByMovieId(Long movieId) {
        log.debug("영화 ID {}에 대한 촬영지 정보 조회", movieId);
        return filmingLocationRepository.findByMovieId(movieId);
    }

    /**
     * 촬영지 정보 처리 상태 조회를 위한 Map (메모리에 임시 저장)
     * 키: 영화 ID, 값: 처리 상태 (true: 완료, false: 처리 중)
     */
    private static final Map<Long, Boolean> processingStatusMap = new HashMap<>();

    /**
     * 영화 ID로 촬영지 정보 처리 상태 조회
     */
    public boolean isProcessingComplete(Long movieId) {
        boolean status = processingStatusMap.getOrDefault(movieId, true); // 기본값은 완료 상태
        log.debug("영화 ID {} 처리 상태 조회: {}", movieId, status ? "완료" : "처리 중");
        return status;
    }

    /**
     * 영화 ID로 촬영지 정보 처리 상태 설정
     */
    public void setProcessingStatus(Long movieId, boolean isComplete) {
        log.debug("영화 ID {} 처리 상태 설정: {}", movieId, isComplete ? "완료" : "처리 중");
        processingStatusMap.put(movieId, isComplete);
    }

    /**
     * 영화 ID로 촬영지 정보를 비동기적으로 요청하고 상태 추적
     */
    public void requestFilmingLocationsAsync(Long movieId) {
        String logPrefix = "[비동기요청][" + movieId + "]";

        // 이미 처리 중인 경우 중복 요청 방지
        if (processingStatusMap.getOrDefault(movieId, true) == false) {
            log.info("{} 영화 ID {}에 대한 촬영지 정보 처리가 이미 진행 중입니다", logPrefix, movieId);
            addRequestLog(movieId, String.format("비동기 요청 중복 - 이미 처리 중 - %s", LocalDateTime.now()));
            return;
        }

        // 처리 상태를 '처리 중'으로 설정
        setProcessingStatus(movieId, false);
        addRequestLog(movieId, String.format("비동기 처리 상태 설정: 처리 중 - %s", LocalDateTime.now()));

        // 비동기 요청 실행
        fetchAndSaveFilmingLocationsAsync(movieId)
                .thenAccept(locations -> {
                    log.info("{} 영화 ID {}에 대한 촬영지 정보 비동기 처리 완료: {}개 - 시간: {}",
                            logPrefix, movieId, locations.size(), LocalDateTime.now());
                    setProcessingStatus(movieId, true); // 처리 완료로 상태 변경
                    addRequestLog(movieId, String.format("비동기 처리 완료 - %s - %d개 장소",
                            LocalDateTime.now(), locations.size()));
                })
                .exceptionally(ex -> {
                    log.error("{} 영화 ID {}에 대한 촬영지 정보 비동기 처리 실패: {}",
                            logPrefix, movieId, ex.getMessage());
                    setProcessingStatus(movieId, true); // 오류 발생해도 처리 완료로 표시
                    addRequestLog(movieId, String.format("비동기 처리 실패 - %s - %s",
                            LocalDateTime.now(), ex.getMessage()));
                    return null;
                });
    }

    /**
     * 영화 ID에 대한 요청 로그 추가
     */
    private void addRequestLog(Long movieId, String logMessage) {
        synchronized (requestLogsMap) {
            List<String> logs = requestLogsMap.computeIfAbsent(movieId, k -> new java.util.ArrayList<>());
            // 최대 100개 로그만 저장
            if (logs.size() >= 100) {
                logs.remove(0); // 가장 오래된 로그 제거
            }
            logs.add(logMessage);
        }
    }

    /**
     * 영화 ID에 대한 요청 로그 조회
     */
    public List<String> getRequestLogs(Long movieId) {
        synchronized (requestLogsMap) {
            return new ArrayList<>(requestLogsMap.getOrDefault(movieId, new ArrayList<>()));
        }
    }

    /**
     * 모든 촬영지의 위치 정보(위도/경도), 이미지 및 주변 장소 업데이트
     */
    @Transactional
    public void updateAllLocationsGeoAndPlaces(Long movieId) {
        String logPrefix = "[위치업데이트][" + movieId + "]";
        List<FilmingLocation> locations = filmingLocationRepository.findByMovieId(movieId);

        if (locations.isEmpty()) {
            log.warn("{} 영화 ID {}의 촬영지 정보가 없습니다.", logPrefix, movieId);
            return;
        }

        log.info("{} 영화 ID {}의 {}개 촬영지 위치 정보, 이미지 및 주변 장소 정보 업데이트 시작 - 시간: {}",
                logPrefix, movieId, locations.size(), LocalDateTime.now());
        addRequestLog(movieId, String.format("위치 정보 업데이트 시작 - %s - %d개 장소",
                LocalDateTime.now(), locations.size()));

        int successCount = 0;
        int errorCount = 0;

        for (FilmingLocation location : locations) {
            try {
                log.debug("{} 촬영지 '{}' 업데이트 시작", logPrefix, location.getName());
                updateLocationGeoAndPlaces(location);
                filmingLocationRepository.save(location);
                successCount++;
                log.debug("{} 촬영지 '{}' 업데이트 완료", logPrefix, location.getName());

                // API 호출 제한을 고려하여 각 요청 사이에 짧은 딜레이 추가
                Thread.sleep(1000);
            } catch (Exception e) {
                errorCount++;
                log.error("{} 촬영지 '{}' 정보 업데이트 중 오류 발생: {}",
                        logPrefix, location.getName(), e.getMessage());
            }
        }

        log.info("{} 영화 ID {}의 촬영지 정보 업데이트 완료 - 성공: {}, 실패: {} - 시간: {}",
                logPrefix, movieId, successCount, errorCount, LocalDateTime.now());
        addRequestLog(movieId, String.format("위치 정보 업데이트 완료 - %s - 성공: %d, 실패: %d",
                LocalDateTime.now(), successCount, errorCount));
    }

    /**
     * 단일 촬영지의 위치 정보(위도/경도), 이미지 및 주변 장소 업데이트
     */
    private void updateLocationGeoAndPlaces(FilmingLocation location) {
        String logPrefix = "[장소][" + location.getId() + "]";
        String address = location.getAddress();

        if (address == null || address.isEmpty()) {
            log.warn("{} 촬영지 '{}' 주소 정보가 없습니다.", logPrefix, location.getName());
            return;
        }

        log.info("{} 촬영지 '{}' 위치 정보 및 이미지 가져오기 시작", logPrefix, location.getName());

        // 1. 주소로 위도/경도 가져오기
        LatLng coordinates = googleMapsService.getGeocode(address);

        if (coordinates == null) {
            log.warn("{} 촬영지 '{}' 좌표를 찾을 수 없습니다.", logPrefix, location.getName());
            return;
        }

        // 위도/경도 저장
        location.setLatitude(coordinates.lat);
        location.setLongitude(coordinates.lng);

        log.info("{} 촬영지 '{}' 좌표: 위도 {}, 경도 {}",
                logPrefix, location.getName(), coordinates.lat, coordinates.lng);

        // 2. 위도/경도 기반으로 이미지 가져오기
        List<String> images = googleMapsService.getLocationImages(coordinates.lat, coordinates.lng);

        if (!images.isEmpty()) {
            log.info("{} 촬영지 '{}' 이미지 {}개 찾음", logPrefix, location.getName(), images.size());
            location.setImages(images);
        } else {
            log.warn("{} 촬영지 '{}' 이미지를 찾을 수 없습니다.", logPrefix, location.getName());
        }

        // 3. 주변 키워드 기반으로 장소 검색
        Map<String, String> nearbyPlaceIds = new HashMap<>();

        for (String keyword : location.getNearbyKeywords()) {
            try {
                // 키워드로 장소 검색 (최대 2개)
                List<String> placeIds = googleMapsService.searchPlacesByKeyword(
                        coordinates.lat, coordinates.lng, keyword);

                if (!placeIds.isEmpty()) {
                    // 장소 ID를 콤마로 구분해서 저장
                    nearbyPlaceIds.put(keyword, String.join(",", placeIds));
                    log.info("{} 촬영지 '{}' 키워드 '{}' 장소 {}개 찾음",
                            logPrefix, location.getName(), keyword, placeIds.size());
                } else {
                    log.warn("{} 촬영지 '{}' 키워드 '{}' 관련 장소를 찾을 수 없습니다.",
                            logPrefix, location.getName(), keyword);
                }

                // API 호출 제한을 고려하여 각 요청 사이에 짧은 딜레이 추가
                Thread.sleep(500);
            } catch (Exception e) {
                log.error("{} 촬영지 '{}' 키워드 '{}' 장소 검색 중 오류 발생: {}",
                        logPrefix, location.getName(), keyword, e.getMessage());
            }
        }

        // 주변 장소 ID 저장
        location.setNearbyPlaceIds(nearbyPlaceIds);
    }

    /**
     * 영화 ID로 영화 제목 조회
     */
    public String getMovieTitle(Long movieId) {
        log.debug("영화 ID {}의 제목 조회", movieId);
        return movieRepository.findById(movieId)
                .map(Movie::getTitle)
                .orElse("Unknown Movie");
    }

    /**
     * 장소 ID로 촬영지 정보 조회
     */
    @Transactional(readOnly = true)
    public FilmingLocationDetailDto getFilmingLocationById(Long locationId) {
        FilmingLocation location = filmingLocationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("촬영지 정보를 찾을 수 없습니다: " + locationId));

        log.info("장소 ID {}의 촬영지 '{}' 정보 조회", locationId, location.getName());

        // 위치 정보(위도/경도)가 없으면 주소를 기반으로 위치 정보 가져오기 시도
        if ((location.getLatitude() == null || location.getLongitude() == null) &&
                location.getAddress() != null && !location.getAddress().isEmpty()) {
            try {
                log.info("장소 '{}' 위치 정보 조회 시도 (주소: {})", location.getName(), location.getAddress());

                // 구글 지오코딩 API로 위치 정보 가져오기
                LatLng coordinates = googleMapsService.getGeocode(location.getAddress());

                if (coordinates != null) {
                    location.setLatitude(coordinates.lat);
                    location.setLongitude(coordinates.lng);
                    filmingLocationRepository.save(location);

                    log.info("장소 '{}' 위치 정보 업데이트: 위도 {}, 경도 {}",
                            location.getName(), coordinates.lat, coordinates.lng);
                }
            } catch (Exception e) {
                log.warn("장소 '{}' 위치 정보 조회 실패: {}", location.getName(), e.getMessage());
            }
        }

        // 이미지가 없으면 실시간으로 이미지 조회 시도
        if ((location.getImages() == null || location.getImages().isEmpty()) &&
                location.getLatitude() != null && location.getLongitude() != null) {
            try {
                log.info("장소 '{}' 이미지 조회 시도", location.getName());

                // 구글 Places API로 이미지 가져오기
                List<String> images = googleMapsService.getLocationImages(location.getLatitude(), location.getLongitude());

                if (images != null && !images.isEmpty()) {
                    location.setImages(images);
                    filmingLocationRepository.save(location);

                    log.info("장소 '{}' 이미지 {}개 업데이트", location.getName(), images.size());
                }
            } catch (Exception e) {
                log.warn("장소 '{}' 이미지 조회 실패: {}", location.getName(), e.getMessage());
            }
        }

        return FilmingLocationDetailDto.fromEntity(location);
    }

    /**
     * 영화 ID와 장소 ID로 촬영지 정보 조회 (영화 ID 검증 포함)
     */
    @Transactional(readOnly = true)
    public FilmingLocationDetailDto getFilmingLocationByMovieIdAndLocationId(Long movieId, Long locationId) {
        FilmingLocation location = filmingLocationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("촬영지 정보를 찾을 수 없습니다: " + locationId));

        // 영화 ID 검증
        if (location.getMovie() == null || !location.getMovie().getId().equals(movieId)) {
            throw new RuntimeException("해당 영화의 촬영지가 아닙니다: 영화 ID " + movieId + ", 장소 ID " + locationId);
        }

        log.info("영화 ID {}, 장소 ID {}의 촬영지 '{}' 정보 조회",
                movieId, locationId, location.getName());

        return FilmingLocationDetailDto.fromEntity(location);
    }
}