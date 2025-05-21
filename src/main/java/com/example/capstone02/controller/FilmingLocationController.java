package com.example.capstone02.controller;

import com.example.capstone02.dto.FilmingLocationDetailDto;
import com.example.capstone02.entity.FilmingLocation;
import com.example.capstone02.service.FilmingLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
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
     * (동기 처리 - 클라이언트는 응답을 받을 때까지 대기)
     */
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<FilmingLocationDto>> getFilmingLocationsByMovieId(@PathVariable Long movieId) {
        String logPrefix = "[동기조회][" + movieId + "]";
        log.info("{} 영화 ID {}의 촬영지 정보 조회 요청 - 시간: {}", logPrefix, movieId, LocalDateTime.now());

        long startTime = System.currentTimeMillis();

        List<FilmingLocation> filmingLocations = filmingLocationService.getFilmingLocationsByMovieId(movieId);

        // 촬영지 정보가 없으면 파이썬 서버에서 가져오기
        if (filmingLocations.isEmpty()) {
            log.info("{} 영화 ID {}의 촬영지 정보가 없어 파이썬 서버에서 가져옵니다 - 시간: {}",
                    logPrefix, movieId, LocalDateTime.now());

            try {
                // 동기 처리 - 응답이 올 때까지 블로킹됨
                filmingLocations = filmingLocationService.fetchAndSaveFilmingLocations(movieId);
                log.info("{} 영화 ID {}의 촬영지 정보 {}개 가져오기 완료 - 시간: {}",
                        logPrefix, movieId, filmingLocations.size(), LocalDateTime.now());
            } catch (Exception e) {
                log.error("{} 영화 ID {}의 촬영지 정보 가져오기 실패: {} - 시간: {}",
                        logPrefix, movieId, e.getMessage(), LocalDateTime.now(), e);
                return ResponseEntity.internalServerError().body(null);
            }
        } else {
            log.info("{} 영화 ID {}의 촬영지 정보 {}개 DB에서 조회 완료 - 시간: {}",
                    logPrefix, movieId, filmingLocations.size(), LocalDateTime.now());
        }

        // 엔티티를 DTO로 변환
        List<FilmingLocationDto> locationDtos = filmingLocations.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

        long endTime = System.currentTimeMillis();
        log.info("{} 영화 ID {}의 촬영지 정보 조회 응답 - {}개 장소, 소요 시간: {}ms - 시간: {}",
                logPrefix, movieId, locationDtos.size(), (endTime - startTime), LocalDateTime.now());

        return ResponseEntity.ok(locationDtos);
    }

    /**
     * 영화 ID로 촬영지 정보를 강제로 파이썬 서버에서 새로 가져오기
     * (동기 처리 - 클라이언트는 응답을 받을 때까지 대기)
     */
    @GetMapping("/fetch/movie/{movieId}")
    public ResponseEntity<List<FilmingLocationDto>> fetchFilmingLocationsByMovieId(@PathVariable Long movieId) {
        String logPrefix = "[강제동기요청][" + movieId + "]";
        log.info("{} 영화 ID {}의 촬영지 정보 강제 갱신 요청 - 시간: {}",
                logPrefix, movieId, LocalDateTime.now());

        long startTime = System.currentTimeMillis();

        try {
            // 동기 처리 - 응답이 올 때까지 블로킹됨
            List<FilmingLocation> filmingLocations = filmingLocationService.fetchAndSaveFilmingLocations(movieId);
            log.info("{} 영화 ID {}의 촬영지 정보 {}개 강제 갱신 완료 - 시간: {}",
                    logPrefix, movieId, filmingLocations.size(), LocalDateTime.now());

            // 엔티티를 DTO로 변환
            List<FilmingLocationDto> locationDtos = filmingLocations.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());

            long endTime = System.currentTimeMillis();
            log.info("{} 영화 ID {}의 촬영지 정보 강제 갱신 응답 - {}개 장소, 소요 시간: {}ms - 시간: {}",
                    logPrefix, movieId, locationDtos.size(), (endTime - startTime), LocalDateTime.now());

            return ResponseEntity.ok(locationDtos);
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("{} 영화 ID {}의 촬영지 정보 강제 갱신 실패: {} - 소요 시간: {}ms, 시간: {}",
                    logPrefix, movieId, e.getMessage(), (endTime - startTime), LocalDateTime.now(), e);
            return ResponseEntity.internalServerError().body(null);
        }
    }

    /**
     * 영화 ID로 촬영지 정보를 비동기적으로 요청하고 상태 반환
     * (비동기 처리 - 클라이언트는 즉시 응답 받고 처리는 백그라운드에서 진행)
     */
    @GetMapping("/async/movie/{movieId}")
    public ResponseEntity<Map<String, Object>> requestFilmingLocationsAsync(@PathVariable Long movieId) {
        String logPrefix = "[비동기요청][" + movieId + "]";
        log.info("{} 영화 ID {}의 촬영지 정보 비동기 요청 - 시간: {}",
                logPrefix, movieId, LocalDateTime.now());

        // 기존에 정보가 있는지 확인
        boolean hasData = !filmingLocationService.getFilmingLocationsByMovieId(movieId).isEmpty();
        log.debug("{} 영화 ID {}의 기존 데이터 존재 여부: {}", logPrefix, movieId, hasData);

        // 현재 처리 상태 확인
        boolean isComplete = filmingLocationService.isProcessingComplete(movieId);
        log.debug("{} 영화 ID {}의 처리 완료 상태: {}", logPrefix, movieId, isComplete);

        // 데이터가 없고 처리가 완료 상태이면 새로 요청
        if (!hasData && isComplete) {
            log.info("{} 영화 ID {}에 대한 촬영지 정보 비동기 요청 시작 - 시간: {}",
                    logPrefix, movieId, LocalDateTime.now());

            // 비동기 요청 시작 - 응답을 기다리지 않고 바로 반환
            filmingLocationService.requestFilmingLocationsAsync(movieId);

            log.info("{} 영화 ID {}에 대한 촬영지 정보 비동기 요청 전송 완료 - 시간: {}",
                    logPrefix, movieId, LocalDateTime.now());
        } else if (!isComplete) {
            log.info("{} 영화 ID {}에 대한 촬영지 정보 비동기 요청 진행 중 - 시간: {}",
                    logPrefix, movieId, LocalDateTime.now());
        } else {
            log.info("{} 영화 ID {}에 대한 촬영지 정보 이미 존재 - 시간: {}",
                    logPrefix, movieId, LocalDateTime.now());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("movieId", movieId);
        response.put("hasData", hasData);
        response.put("processing", !filmingLocationService.isProcessingComplete(movieId));
        response.put("message", hasData ?
                "데이터가 이미 존재합니다." :
                (isComplete ? "데이터 요청이 시작되었습니다. 몇 분 후에 확인해주세요." : "데이터 처리가 진행 중입니다."));

        log.info("{} 영화 ID {}의 비동기 요청 상태 응답: hasData={}, processing={} - 시간: {}",
                logPrefix, movieId, hasData, !isComplete, LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    /**
     * 영화 ID로 촬영지 정보 처리 상태 확인
     * (동기 처리 - 단순 상태 확인)
     */
    @GetMapping("/status/movie/{movieId}")
    public ResponseEntity<Map<String, Object>> checkProcessingStatus(@PathVariable Long movieId) {
        String logPrefix = "[상태확인][" + movieId + "]";
        log.info("{} 영화 ID {}의 처리 상태 확인 요청 - 시간: {}",
                logPrefix, movieId, LocalDateTime.now());

        boolean hasData = !filmingLocationService.getFilmingLocationsByMovieId(movieId).isEmpty();
        boolean isComplete = filmingLocationService.isProcessingComplete(movieId);

        log.info("{} 영화 ID {}의 상태: hasData={}, isComplete={} - 시간: {}",
                logPrefix, movieId, hasData, isComplete, LocalDateTime.now());

        Map<String, Object> response = new HashMap<>();
        response.put("movieId", movieId);
        response.put("hasData", hasData);
        response.put("processingComplete", isComplete);
        response.put("message", isComplete ?
                (hasData ? "데이터가 준비되었습니다." : "데이터가 없습니다. 요청이 필요합니다.") :
                "데이터 처리가 진행 중입니다.");

        return ResponseEntity.ok(response);
    }

    /**
     * 영화 ID로 모든 촬영지의 지리 정보, 이미지, 주변 장소 업데이트
     * (동기 처리 - 클라이언트는 응답을 받을 때까지 대기)
     */
    @GetMapping("/update-geo/movie/{movieId}")
    public ResponseEntity<String> updateGeoInfoForMovieLocations(@PathVariable Long movieId) {
        String logPrefix = "[위치업데이트][" + movieId + "]";
        log.info("{} 영화 ID {}의 촬영지 위치 정보 업데이트 요청 - 시간: {}",
                logPrefix, movieId, LocalDateTime.now());

        long startTime = System.currentTimeMillis();

        try {
            // 동기 처리 - 업데이트가 완료될 때까지 블로킹됨
            filmingLocationService.updateAllLocationsGeoAndPlaces(movieId);

            long endTime = System.currentTimeMillis();
            log.info("{} 영화 ID {}의 촬영지 위치 정보 업데이트 완료 - 소요 시간: {}ms - 시간: {}",
                    logPrefix, movieId, (endTime - startTime), LocalDateTime.now());

            return ResponseEntity.ok("영화 ID " + movieId + "의 모든 촬영지 위치 정보가 성공적으로 업데이트되었습니다.");
        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            log.error("{} 영화 ID {}의 촬영지 위치 정보 업데이트 중 오류 발생: {} - 소요 시간: {}ms - 시간: {}",
                    logPrefix, movieId, e.getMessage(), (endTime - startTime), LocalDateTime.now(), e);

            return ResponseEntity.internalServerError().body("오류 발생: " + e.getMessage());
        }
    }

    /**
     * 영화 ID로 촬영지 정보 조회 (위도/경도, 이미지, 주변 장소 포함)
     * (동기 처리 - 단순 조회)
     */
    @GetMapping("/movie/{movieId}/detailed")
    public ResponseEntity<List<FilmingLocationDetailedDto>> getDetailedFilmingLocationsByMovieId(@PathVariable Long movieId) {
        String logPrefix = "[상세조회][" + movieId + "]";
        log.info("{} 영화 ID {}의 상세 촬영지 정보 조회 요청 - 시간: {}",
                logPrefix, movieId, LocalDateTime.now());

        long startTime = System.currentTimeMillis();

        List<FilmingLocation> filmingLocations = filmingLocationService.getFilmingLocationsByMovieId(movieId);

        // 엔티티를 DetailedDTO로 변환
        List<FilmingLocationDetailedDto> locationDtos = filmingLocations.stream()
                .map(this::convertToDetailedDto)
                .collect(Collectors.toList());

        long endTime = System.currentTimeMillis();
        log.info("{} 영화 ID {}의 상세 촬영지 정보 조회 응답 - {}개 장소, 소요 시간: {}ms - 시간: {}",
                logPrefix, movieId, locationDtos.size(), (endTime - startTime), LocalDateTime.now());

        return ResponseEntity.ok(locationDtos);
    }

    /**
     * 영화 ID에 대한 처리 로그 조회
     * (동기 처리 - 단순 조회)
     */
    @GetMapping("/logs/movie/{movieId}")
    public ResponseEntity<Map<String, Object>> getProcessingLogs(@PathVariable Long movieId) {
        String logPrefix = "[로그조회][" + movieId + "]";
        log.info("{} 영화 ID {}의 처리 로그 조회 요청 - 시간: {}",
                logPrefix, movieId, LocalDateTime.now());

        List<String> logs = filmingLocationService.getRequestLogs(movieId);
        boolean hasData = !filmingLocationService.getFilmingLocationsByMovieId(movieId).isEmpty();
        boolean isComplete = filmingLocationService.isProcessingComplete(movieId);

        Map<String, Object> response = new HashMap<>();
        response.put("movieId", movieId);
        response.put("hasData", hasData);
        response.put("processingComplete", isComplete);
        response.put("logs", logs);
        response.put("logsCount", logs.size());

        log.info("{} 영화 ID {}의 처리 로그 조회 응답 - {}개 로그 - 시간: {}",
                logPrefix, movieId, logs.size(), LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    /**
     * 장소 ID로 촬영지 상세 정보 조회 API
     */
    @GetMapping("/{locationId}")
    public ResponseEntity<FilmingLocationDetailDto> getFilmingLocationById(@PathVariable Long locationId) {
        String logPrefix = "[장소조회][" + locationId + "]";
        log.info("{} 장소 ID {}의 상세 정보 조회 요청", logPrefix, locationId);

        try {
            FilmingLocationDetailDto locationDetail = filmingLocationService.getFilmingLocationById(locationId);
            log.info("{} 장소 ID {}('{}')의 상세 정보 조회 성공",
                    logPrefix, locationId, locationDetail.getName());

            return ResponseEntity.ok(locationDetail);
        } catch (Exception e) {
            log.error("{} 장소 ID {}의 상세 정보 조회 중 오류 발생: {}",
                    logPrefix, locationId, e.getMessage(), e);

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(null);
        }
    }

//    /**
//     * 영화 ID와 장소 ID로 촬영지 상세 정보 조회 API
//     */
//    @GetMapping("/movie/{movieId}/location/{locationId}")
//    public ResponseEntity<FilmingLocationDetailDto> getFilmingLocationByMovieIdAndLocationId(
//            @PathVariable Long movieId, @PathVariable Long locationId) {
//
//        String logPrefix = "[장소조회][영화:" + movieId + "][장소:" + locationId + "]";
//        log.info("{} 영화 ID {}, 장소 ID {}의 상세 정보 조회 요청", logPrefix, movieId, locationId);
//
//        try {
//            FilmingLocationDetailDto locationDetail =
//                    filmingLocationService.getFilmingLocationByMovieIdAndLocationId(movieId, locationId);
//
//            log.info("{} 영화 ID {}, 장소 ID {}('{}')의 상세 정보 조회 성공",
//                    logPrefix, movieId, locationId, locationDetail.getName());
//
//            return ResponseEntity.ok(locationDetail);
//        } catch (Exception e) {
//            log.error("{} 영화 ID {}, 장소 ID {}의 상세 정보 조회 중 오류 발생: {}",
//                    logPrefix, movieId, locationId, e.getMessage(), e);
//
//            return ResponseEntity.status(HttpStatus.NOT_FOUND)
//                    .body(null);
//        }
//    }

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