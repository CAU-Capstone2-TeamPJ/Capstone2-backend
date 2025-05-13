package com.example.capstone02.service;

import com.example.capstone02.dto.FilmingLocationResponseDto;
import com.example.capstone02.dto.MovieInfoRequestDto;
import com.example.capstone02.entity.FilmingLocation;
import com.example.capstone02.entity.Movie;
import com.example.capstone02.repository.FilmingLocationRepository;
import com.example.capstone02.repository.MovieRepository;
import com.google.maps.model.LatLng;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FilmingLocationService {

    private final WebClient webClient;
    private final MovieRepository movieRepository;
    private final FilmingLocationRepository filmingLocationRepository;
    private final GoogleMapsService googleMapsService; // Google Maps 서비스 추가

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
     * 모든 촬영지의 위치 정보(위도/경도), 이미지 및 주변 장소 업데이트
     */
    @Transactional
    public void updateAllLocationsGeoAndPlaces(Long movieId) {
        List<FilmingLocation> locations = filmingLocationRepository.findByMovieId(movieId);

        if (locations.isEmpty()) {
            log.warn("영화 ID {}의 촬영지 정보가 없습니다.", movieId);
            return;
        }

        log.info("영화 ID {}의 {}개 촬영지 위치 정보, 이미지 및 주변 장소 정보 업데이트 시작", movieId, locations.size());

        for (FilmingLocation location : locations) {
            try {
                updateLocationGeoAndPlaces(location);
                filmingLocationRepository.save(location);

                // API 호출 제한을 고려하여 각 요청 사이에 짧은 딜레이 추가
                Thread.sleep(1000);
            } catch (Exception e) {
                log.error("촬영지 '{}' 정보 업데이트 중 오류 발생: {}", location.getName(), e.getMessage());
            }
        }

        log.info("영화 ID {}의 촬영지 정보 업데이트 완료", movieId);
    }

    /**
     * 단일 촬영지의 위치 정보(위도/경도), 이미지 및 주변 장소 업데이트
     */
    private void updateLocationGeoAndPlaces(FilmingLocation location) {
        String address = location.getAddress();

        if (address == null || address.isEmpty()) {
            log.warn("촬영지 '{}' 주소 정보가 없습니다.", location.getName());
            return;
        }

        log.info("촬영지 '{}' 위치 정보 및 이미지 가져오기 시작", location.getName());

        // 1. 주소로 위도/경도 가져오기
        LatLng coordinates = googleMapsService.getGeocode(address);

        if (coordinates == null) {
            log.warn("촬영지 '{}' 좌표를 찾을 수 없습니다.", location.getName());
            return;
        }

        // 위도/경도 저장
        location.setLatitude(coordinates.lat);
        location.setLongitude(coordinates.lng);

        log.info("촬영지 '{}' 좌표: 위도 {}, 경도 {}",
                location.getName(), coordinates.lat, coordinates.lng);

        // 2. 위도/경도 기반으로 이미지 가져오기
        List<String> images = googleMapsService.getLocationImages(coordinates.lat, coordinates.lng);

        if (!images.isEmpty()) {
            log.info("촬영지 '{}' 이미지 {}개 찾음", location.getName(), images.size());
            location.setImages(images);
        } else {
            log.warn("촬영지 '{}' 이미지를 찾을 수 없습니다.", location.getName());
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
                    log.info("촬영지 '{}' 키워드 '{}' 장소 {}개 찾음",
                            location.getName(), keyword, placeIds.size());
                } else {
                    log.warn("촬영지 '{}' 키워드 '{}' 관련 장소를 찾을 수 없습니다.",
                            location.getName(), keyword);
                }

                // API 호출 제한을 고려하여 각 요청 사이에 짧은 딜레이 추가
                Thread.sleep(500);
            } catch (Exception e) {
                log.error("촬영지 '{}' 키워드 '{}' 장소 검색 중 오류 발생: {}",
                        location.getName(), keyword, e.getMessage());
            }
        }

        // 주변 장소 ID 저장
        location.setNearbyPlaceIds(nearbyPlaceIds);
    }
}