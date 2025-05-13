package com.example.capstone02.controller;

import com.example.capstone02.entity.FilmingLocation;
import com.example.capstone02.service.FilmingLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/filming-locations")
@RequiredArgsConstructor
@Slf4j
public class FilmingLocationController {

    private final FilmingLocationService filmingLocationService;

    /**
     * 영화 ID로 촬영지 정보 조회
     * DB에 없으면 파이썬 서버에서 가져오기
     */
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<FilmingLocationDto>> getFilmingLocationsByMovieId(@PathVariable Long movieId) {
        List<FilmingLocation> filmingLocations = filmingLocationService.getFilmingLocationsByMovieId(movieId);

        // 촬영지 정보가 없으면 파이썬 서버에서 가져오기
        if (filmingLocations.isEmpty()) {
            log.info("영화 ID {}의 촬영지 정보가 없어 파이썬 서버에서 가져옵니다", movieId);
            filmingLocations = filmingLocationService.fetchAndSaveFilmingLocations(movieId);
        }

        // 엔티티를 DTO로 변환
        List<FilmingLocationDto> locationDtos = filmingLocations.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(locationDtos);
    }

    /**
     * 영화 ID로 촬영지 정보를 강제로 파이썬 서버에서 새로 가져오기
     */
    @GetMapping("/fetch/movie/{movieId}")
    public ResponseEntity<List<FilmingLocationDto>> fetchFilmingLocationsByMovieId(@PathVariable Long movieId) {
        List<FilmingLocation> filmingLocations = filmingLocationService.fetchAndSaveFilmingLocations(movieId);

        // 엔티티를 DTO로 변환
        List<FilmingLocationDto> locationDtos = filmingLocations.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(locationDtos);
    }

    /**
     * 영화 ID로 모든 촬영지의 지리 정보, 이미지, 주변 장소 업데이트
     */
    @GetMapping("/update-geo/movie/{movieId}")
    public ResponseEntity<String> updateGeoInfoForMovieLocations(@PathVariable Long movieId) {
        try {
            filmingLocationService.updateAllLocationsGeoAndPlaces(movieId);
            return ResponseEntity.ok("영화 ID " + movieId + "의 모든 촬영지 위치 정보가 성공적으로 업데이트되었습니다.");
        } catch (Exception e) {
            log.error("영화 ID {}의 촬영지 위치 정보 업데이트 중 오류 발생: {}", movieId, e.getMessage());
            return ResponseEntity.internalServerError().body("오류 발생: " + e.getMessage());
        }
    }

    /**
     * 영화 ID로 촬영지 정보 조회 (위도/경도, 이미지, 주변 장소 포함)
     */
    @GetMapping("/movie/{movieId}/detailed")
    public ResponseEntity<List<FilmingLocationDetailedDto>> getDetailedFilmingLocationsByMovieId(@PathVariable Long movieId) {
        List<FilmingLocation> filmingLocations = filmingLocationService.getFilmingLocationsByMovieId(movieId);

        // 엔티티를 DetailedDTO로 변환
        List<FilmingLocationDetailedDto> locationDtos = filmingLocations.stream()
                .map(this::convertToDetailedDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(locationDtos);
    }

    // 엔티티를 DTO로 변환하는 헬퍼 메서드
    private FilmingLocationDto convertToDto(FilmingLocation location) {
        return FilmingLocationDto.builder()
                .id(location.getId())
                .movieId(location.getMovie().getId())
                .movieTitle(location.getMovie().getTitle())
                .name(location.getName())
                .country(location.getCountry())
                .description(location.getDescription())
                .address(location.getAddress())
                .durationTime(location.getDurationTime())
                .mentionRate(location.getMentionRate())
                .mentionCount(location.getMentionCount())
                .recommendationKeywords(location.getRecommendationKeywords())
                .nearbyKeywords(location.getNearbyKeywords())
                .build();
    }

    // 엔티티를 DetailedDTO로 변환하는 헬퍼 메서드
    private FilmingLocationDetailedDto convertToDetailedDto(FilmingLocation location) {
        return FilmingLocationDetailedDto.builder()
                .id(location.getId())
                .movieId(location.getMovie().getId())
                .movieTitle(location.getMovie().getTitle())
                .name(location.getName())
                .country(location.getCountry())
                .description(location.getDescription())
                .address(location.getAddress())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .durationTime(location.getDurationTime())
                .mentionRate(location.getMentionRate())
                .mentionCount(location.getMentionCount())
                .recommendationKeywords(location.getRecommendationKeywords())
                .nearbyKeywords(location.getNearbyKeywords())
                .images(location.getImages())
                .nearbyPlaceIds(location.getNearbyPlaceIds())
                .build();
    }

    // 응답용 DTO
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FilmingLocationDto {
        private Long id;
        private Long movieId;
        private String movieTitle;
        private String name;
        private String country;
        private String description;
        private String address;
        private Double durationTime;
        private Double mentionRate;
        private Integer mentionCount;
        private List<String> recommendationKeywords;
        private List<String> nearbyKeywords;
    }

    // 상세 정보 포함된 응답용 DTO
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class FilmingLocationDetailedDto {
        private Long id;
        private Long movieId;
        private String movieTitle;
        private String name;
        private String country;
        private String description;
        private String address;
        private Double latitude;
        private Double longitude;
        private Double durationTime;
        private Double mentionRate;
        private Integer mentionCount;
        private List<String> recommendationKeywords;
        private List<String> nearbyKeywords;
        private List<String> images;
        private Map<String, String> nearbyPlaceIds;
    }
}