package com.example.capstone02.service;

import com.example.capstone02.dto.FilmingLocationResponseDto;
import com.example.capstone02.dto.MovieInfoRequestDto;
import com.example.capstone02.entity.FilmingLocation;
import com.example.capstone02.entity.Movie;
import com.example.capstone02.repository.FilmingLocationRepository;
import com.example.capstone02.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FilmingLocationService {

    private final WebClient webClient;
    private final MovieRepository movieRepository;
    private final FilmingLocationRepository filmingLocationRepository;

    @Value("${python.server.url}")
    private String pythonServerUrl;

    /**
     * 영화 ID로 촬영지 정보를 파이썬 서버에서 가져와 저장
     */
    @Transactional
    public List<FilmingLocation> fetchAndSaveFilmingLocations(Long movieId) {
        log.info("영화 ID {}에 대한 촬영지 정보 가져오기", movieId);

        // 1. 영화 정보 조회
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("영화를 찾을 수 없습니다: " + movieId));

        // 2. 파이썬 서버에 전송할 영화 정보 준비
        MovieInfoRequestDto requestDto = MovieInfoRequestDto.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .director(movie.getDirector())
                .releaseDate(movie.getReleaseDate() != null ?
                        movie.getReleaseDate().format(DateTimeFormatter.ISO_DATE) : null)
                .build();

        // 3. 파이썬 서버에 요청하여 촬영지 정보 가져오기
        FilmingLocationResponseDto responseDto = webClient.post()
                .uri(pythonServerUrl + "/api/filming-locations")
                .bodyValue(requestDto)
                .retrieve()
                .bodyToMono(FilmingLocationResponseDto.class)
                .block(); // 동기 처리

        if (responseDto == null || responseDto.getLocations() == null || responseDto.getLocations().isEmpty()) {
            log.warn("영화 ID {}에 대한 촬영지 정보가 없습니다", movieId);
            return List.of();
        }

        log.info("영화 ID {}에 대한 촬영지 {}개를 가져왔습니다", movieId, responseDto.getLocations().size());

        // 4. 기존 촬영지 정보 삭제 (업데이트 시)
        if (filmingLocationRepository.existsByMovieId(movieId)) {
            log.info("영화 ID {}의 기존 촬영지 정보를 삭제합니다", movieId);
            filmingLocationRepository.deleteAllByMovieId(movieId);
        }

        // 5. 새로운 촬영지 정보 저장
        List<FilmingLocation> filmingLocations = responseDto.getLocations().stream()
                .map(locationInfo -> {
                    FilmingLocation location = FilmingLocation.builder()
                            .movie(movie)
                            .name(locationInfo.getName())
                            .country(locationInfo.getCountry())
                            .description(locationInfo.getDescription())
                            .latitude(locationInfo.getLatitude())
                            .longitude(locationInfo.getLongitude())
                            .address(locationInfo.getAddress())
                            .mentionRate(locationInfo.getMentionRate())
                            .mentionCount(locationInfo.getMentionCount())
                            .build();

                    // 키워드 설정
                    if (locationInfo.getKeywords() != null) {
                        location.setKeywords(locationInfo.getKeywords());
                    }

                    return location;
                })
                .collect(Collectors.toList());

        return filmingLocationRepository.saveAll(filmingLocations);
    }

    /**
     * 영화 ID로 저장된 촬영지 정보 조회
     */
    @Transactional(readOnly = true)
    public List<FilmingLocation> getFilmingLocationsByMovieId(Long movieId) {
        return filmingLocationRepository.findByMovieId(movieId);
    }

    /**
     * 특정 위치 주변의 촬영지 검색 (선택적 기능)
     */
    @Transactional(readOnly = true)
    public List<FilmingLocation> findNearbyLocations(double latitude, double longitude, double distanceInKm) {
        return filmingLocationRepository.findLocationsByGeoDistance(latitude, longitude, distanceInKm);
    }
}