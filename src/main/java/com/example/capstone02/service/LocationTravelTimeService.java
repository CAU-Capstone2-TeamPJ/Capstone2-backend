package com.example.capstone02.service;

import com.example.capstone02.dto.LocationDistanceInfo;
import com.example.capstone02.entity.FilmingLocation;
import com.example.capstone02.entity.LocationTravelTime;
import com.example.capstone02.repository.LocationTravelTimeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 촬영지 간 이동 시간을 계산하고 관리하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LocationTravelTimeService {

    private final FilmingLocationService filmingLocationService;
    private final GoogleMapsDistanceService distanceService;
    private final LocationTravelTimeRepository locationTravelTimeRepository;

    private static final int SECONDS_PER_MINUTE = 60;

    /**
     * 영화 ID로 해당 영화의 모든 촬영지 간 이동 시간 정보 가져오기
     *
     * @param movieId 영화 ID
     * @return 촬영지 간 이동 시간 정보 목록
     */
    @Transactional(readOnly = true)
    public List<LocationDistanceInfo> getLocationTravelTimesByMovieId(Long movieId) {
        log.info("영화 ID {}의 촬영지 간 이동 시간 정보 조회", movieId);

        List<LocationTravelTime> travelTimes = locationTravelTimeRepository.findByMovieId(movieId);

        // 엔티티를 DTO로 변환
        return travelTimes.stream()
                .map(this::convertToDistanceInfo)
                .collect(Collectors.toList());
    }

    /**
     * 영화 ID와 필터링된 촬영지 ID 목록으로 해당하는 이동 시간 정보만 가져오기
     *
     * @param movieId 영화 ID
     * @param locationIds 필터링된 촬영지 ID 목록
     * @return 필터링된 촬영지 간 이동 시간 정보 목록
     */
    @Transactional(readOnly = true)
    public List<LocationDistanceInfo> getFilteredTravelTimes(Long movieId, List<Long> locationIds) {
        log.info("영화 ID {}의 필터링된 촬영지 간 이동 시간 정보 조회 ({} 개 장소)", movieId, locationIds.size());

        List<LocationTravelTime> travelTimes = locationTravelTimeRepository.findByMovieIdAndLocationIds(movieId, locationIds);

        // 엔티티를 DTO로 변환
        return travelTimes.stream()
                .filter(tt -> locationIds.contains(tt.getFromLocationId()) && locationIds.contains(tt.getToLocationId()))
                .map(this::convertToDistanceInfo)
                .collect(Collectors.toList());
    }

    /**
     * 영화 ID로 해당 영화의 모든 촬영지 간 이동 시간 계산 및 저장
     *
     * @param movieId 영화 ID
     * @return 계산된 이동 시간 정보 목록
     */
    @Transactional
    public List<LocationDistanceInfo> calculateAndSaveTravelTimes(Long movieId) {
        log.info("영화 ID {}의 촬영지 간 이동 시간 계산 및 저장 시작", movieId);

        // 1. 기존 데이터 삭제
        locationTravelTimeRepository.deleteByMovieId(movieId);

        // 2. 영화 촬영지 정보 가져오기
        List<FilmingLocation> locations = filmingLocationService.getFilmingLocationsByMovieId(movieId);

        if (locations.isEmpty()) {
            log.warn("영화 ID {}의 촬영지 정보가 없습니다", movieId);
            return new ArrayList<>();
        }

        // 3. 위도/경도 정보 없는 장소 필터링
        locations = locations.stream()
                .filter(location -> location.getLatitude() != null && location.getLongitude() != null)
                .collect(Collectors.toList());

        log.info("영화 ID {}의 촬영지 수: {}", movieId, locations.size());

        // 4. 장소 간 이동 시간 계산
        List<LocationTravelTime> travelTimes = new ArrayList<>();
        int locationCount = locations.size();

        for (int i = 0; i < locationCount; i++) {
            for (int j = i + 1; j < locationCount; j++) {
                FilmingLocation fromLocation = locations.get(i);
                FilmingLocation toLocation = locations.get(j);

                // 이동 시간 계산 (Google Maps API 사용)
                int[] result = distanceService.calculateDistance(
                        fromLocation.getLatitude(), fromLocation.getLongitude(),
                        toLocation.getLatitude(), toLocation.getLongitude());

                // 정방향 (a -> b) 저장
                LocationTravelTime forwardTravelTime = LocationTravelTime.builder()
                        .movieId(movieId)
                        .fromLocationId(fromLocation.getId())
                        .toLocationId(toLocation.getId())
                        .fromLocationName(fromLocation.getName())
                        .toLocationName(toLocation.getName())
                        .distanceMeters(result[0])
                        .travelTimeMinutes(result[1] / SECONDS_PER_MINUTE)
                        .build();

                travelTimes.add(forwardTravelTime);

                // 역방향 (b -> a) 저장 (동일한 값)
                LocationTravelTime backwardTravelTime = LocationTravelTime.builder()
                        .movieId(movieId)
                        .fromLocationId(toLocation.getId())
                        .toLocationId(fromLocation.getId())
                        .fromLocationName(toLocation.getName())
                        .toLocationName(fromLocation.getName())
                        .distanceMeters(result[0])
                        .travelTimeMinutes(result[1] / SECONDS_PER_MINUTE)
                        .build();

                travelTimes.add(backwardTravelTime);
            }
        }

        // 5. 계산된 이동 시간 정보 저장
        locationTravelTimeRepository.saveAll(travelTimes);

        log.info("영화 ID {}의 촬영지 간 이동 시간 계산 및 저장 완료: {}개 경로", movieId, travelTimes.size());

        // 6. 저장된 정보를 DTO로 변환하여 반환
        return travelTimes.stream()
                .map(this::convertToDistanceInfo)
                .collect(Collectors.toList());
    }

    /**
     * 비동기로 영화 ID 목록의 모든 촬영지 간 이동 시간 계산 및 저장
     *
     * @param movieIds 영화 ID 목록
     * @return 완료 메시지
     */
    @Async
    @Transactional
    public CompletableFuture<String> calculateAndSaveTravelTimesForMovies(List<Long> movieIds) {
        log.info("{}개 영화의 촬영지 간 이동 시간 계산 작업 시작", movieIds.size());

        int count = 0;
        for (Long movieId : movieIds) {
            try {
                calculateAndSaveTravelTimes(movieId);
                count++;
                log.info("영화 ID {} 처리 완료 ({}/{})", movieId, count, movieIds.size());
            } catch (Exception e) {
                log.error("영화 ID {} 처리 중 오류 발생: {}", movieId, e.getMessage(), e);
            }
        }

        return CompletableFuture.completedFuture(
                String.format("%d개 영화 중 %d개 처리 완료", movieIds.size(), count));
    }

    /**
     * 출발지(위도/경도)와 촬영지 간 이동 시간 계산
     *
     * @param originLat 출발지 위도
     * @param originLng 출발지 경도
     * @param locations 촬영지 목록
     * @return 출발지와 각 촬영지 간 이동 시간 정보 목록
     */
    public List<LocationDistanceInfo> calculateDistancesFromOrigin(
            Double originLat, Double originLng, List<FilmingLocation> locations) {

        if (originLat == null || originLng == null) {
            return new ArrayList<>();
        }

        List<LocationDistanceInfo> distanceInfos = new ArrayList<>();

        for (FilmingLocation location : locations) {
            int[] result = distanceService.calculateDistance(
                    originLat, originLng,
                    location.getLatitude(), location.getLongitude());

            LocationDistanceInfo info = LocationDistanceInfo.builder()
                    .fromLocationId(0L) // 출발지는 ID 0으로 표기
                    .toLocationId(location.getId())
                    .fromLocationName("Origin")
                    .toLocationName(location.getName())
                    .distanceMeters(result[0])
                    .travelTimeMinutes(result[1] / SECONDS_PER_MINUTE)
                    .build();

            distanceInfos.add(info);
        }

        return distanceInfos;
    }

    /**
     * LocationTravelTime 엔티티를 LocationDistanceInfo DTO로 변환
     */
    private LocationDistanceInfo convertToDistanceInfo(LocationTravelTime travelTime) {
        return LocationDistanceInfo.builder()
                .fromLocationId(travelTime.getFromLocationId())
                .toLocationId(travelTime.getToLocationId())
                .fromLocationName(travelTime.getFromLocationName())
                .toLocationName(travelTime.getToLocationName())
                .distanceMeters(travelTime.getDistanceMeters())
                .travelTimeMinutes(travelTime.getTravelTimeMinutes())
                .build();
    }
}