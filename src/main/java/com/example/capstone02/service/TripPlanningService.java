package com.example.capstone02.service;

import com.example.capstone02.dto.LocationDistanceInfo;
import com.example.capstone02.dto.TripPlanRequestDto;
import com.example.capstone02.dto.TripPlanResponseDto;
import com.example.capstone02.entity.FilmingLocation;
import com.example.capstone02.util.ConceptKeywordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripPlanningService {

    private final FilmingLocationService filmingLocationService;
    private final LocationTravelTimeService locationTravelTimeService;
    private final ConceptKeywordMapper conceptKeywordMapper;
    private final GoogleMapsService googleMapsService;

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int MAX_ITERATIONS = 100;
    private static final String NIGHT_VIEW_KEYWORD = "야경";
    private static final int MAX_LOCATIONS_PER_CLUSTER = 9;
    private static final int MIN_LOCATIONS_AFTER_SPLIT = 5;

    /**
     * 여행 경로 계획 생성
     */
    public TripPlanResponseDto createTripPlan(TripPlanRequestDto request) {
        log.info("여행 경로 계획 생성 시작: {}", request);

        // 1. 영화 ID로 촬영지 정보 조회 및 필터링
        List<FilmingLocation> allLocations = getFilteredLocations(request);
        if (allLocations.isEmpty()) {
            log.warn("조건에 맞는 촬영지가 없습니다");
            return createEmptyResponse();
        }

        // 2. 미리 계산된 장소간 이동시간 불러오기
        List<LocationDistanceInfo> distanceMatrix = loadPrecomputedTravelTimes(allLocations);

        // 3. Open TSP로 전체 이동시간 계산 후 클러스터 수(k) 결정
        int totalTravelTimeMinutes = calculateTotalTravelTimeByOpenTSP(allLocations, distanceMatrix);
        int dailyTravelMinutes = (request.getTravelHours() != null ? request.getTravelHours() : 8) * MINUTES_PER_HOUR;
        int k = Math.max(1, (int) Math.ceil((double) totalTravelTimeMinutes / dailyTravelMinutes));

        log.info("총 이동시간: {}분, 하루 이동시간: {}분, 클러스터 수(k): {}",
                totalTravelTimeMinutes, dailyTravelMinutes, k);

        // 4. K-means++ 클러스터링 수행 (하버사인 공식 사용)
        Map<Integer, List<FilmingLocation>> clusters = performKMeansPlusPlus(allLocations, k);

        // 5. 9개 이상 클러스터 분할
        clusters = splitLargeClusters(clusters);

        // 6. 각 클러스터에서 Open TSP 경로 계산 (야경 키워드 도착지 고정)
        List<TripPlanResponseDto.DailyRouteDto> dailyRoutes = createDailyRoutesWithOpenTSP(clusters, distanceMatrix);

        // 7. 최종 응답 생성
        return TripPlanResponseDto.builder()
                .dailyRoutes(dailyRoutes)
                .totalDays(dailyRoutes.size())
                .totalLocations(allLocations.size())
                .totalTravelTimeMinutes(dailyRoutes.stream().mapToInt(TripPlanResponseDto.DailyRouteDto::getTravelTimeMinutes).sum())
                .build();
    }

    /**
     * 필터링된 촬영지 목록 조회
     */
    private List<FilmingLocation> getFilteredLocations(TripPlanRequestDto request) {
        List<FilmingLocation> allLocations = filmingLocationService.getFilmingLocationsByMovieId(request.getMovieId());

        // 국가 필터링
        if (request.getCountry() != null && !request.getCountry().isEmpty()) {
            allLocations = allLocations.stream()
                    .filter(location -> request.getCountry().equals(location.getCountry()))
                    .collect(Collectors.toList());
        }

        // 컨셉 필터링
        List<String> conceptKeywords = getConceptKeywords(request);
        if (!conceptKeywords.isEmpty()) {
            allLocations = allLocations.stream()
                    .filter(location -> location.getRecommendationKeywords().stream()
                            .anyMatch(conceptKeywords::contains))
                    .collect(Collectors.toList());
        }

        // 위도/경도 정보가 있는 장소만 필터링
        return allLocations.stream()
                .filter(location -> location.getLatitude() != null && location.getLongitude() != null)
                .collect(Collectors.toList());
    }

    /**
     * 컨셉 키워드 목록 가져오기
     */
    private List<String> getConceptKeywords(TripPlanRequestDto request) {
        List<String> allConceptKeywords = new ArrayList<>();

        if (request.getConcept() != null && !request.getConcept().isEmpty()) {
            allConceptKeywords.addAll(conceptKeywordMapper.getKeywordsByConcept(request.getConcept()));
        }

        if (request.getConcepts() != null && !request.getConcepts().isEmpty()) {
            for (String concept : request.getConcepts()) {
                if (concept != null && !concept.isEmpty()) {
                    allConceptKeywords.addAll(conceptKeywordMapper.getKeywordsByConcept(concept));
                }
            }
        }

        return allConceptKeywords;
    }

    /**
     * 미리 계산된 장소간 이동시간 불러오기
     */
    private List<LocationDistanceInfo> loadPrecomputedTravelTimes(List<FilmingLocation> locations) {
        if (locations.isEmpty()) {
            return new ArrayList<>();
        }

        Long movieId = locations.get(0).getMovie().getId();
        List<Long> locationIds = locations.stream().map(FilmingLocation::getId).collect(Collectors.toList());

        List<LocationDistanceInfo> travelTimes = locationTravelTimeService.getFilteredTravelTimes(movieId, locationIds);
        log.info("미리 계산된 이동시간 정보 {}개 로드", travelTimes.size());

        return travelTimes;
    }

    /**
     * Open TSP로 전체 이동시간 계산
     */
    private int calculateTotalTravelTimeByOpenTSP(List<FilmingLocation> locations, List<LocationDistanceInfo> distanceMatrix) {
        if (locations.size() <= 1) {
            return 0;
        }

        // 거리 행렬을 Map으로 변환하여 빠른 조회
        Map<String, Integer> travelTimeMap = new HashMap<>();
        for (LocationDistanceInfo info : distanceMatrix) {
            String key = info.getFromLocationId() + "-" + info.getToLocationId();
            travelTimeMap.put(key, info.getTravelTimeMinutes());
        }

        // 최근접 이웃 알고리즘으로 Open TSP 근사 해 구하기
        List<FilmingLocation> route = new ArrayList<>();
        Set<Long> visited = new HashSet<>();

        // 임의의 시작점 선택
        FilmingLocation current = locations.get(0);
        route.add(current);
        visited.add(current.getId());

        int totalTravelTime = 0;

        while (visited.size() < locations.size()) {
            FilmingLocation nearest = null;
            int minTime = Integer.MAX_VALUE;

            for (FilmingLocation location : locations) {
                if (visited.contains(location.getId())) continue;

                String key = current.getId() + "-" + location.getId();
                Integer travelTime = travelTimeMap.get(key);
                if (travelTime != null && travelTime < minTime) {
                    minTime = travelTime;
                    nearest = location;
                }
            }

            if (nearest != null) {
                route.add(nearest);
                visited.add(nearest.getId());
                totalTravelTime += minTime;
                current = nearest;
            } else {
                break;
            }
        }

        log.info("Open TSP 전체 이동시간 계산: {}분", totalTravelTime);
        return totalTravelTime;
    }

    /**
     * K-means++ 클러스터링 수행 (하버사인 공식 사용)
     */
    private Map<Integer, List<FilmingLocation>> performKMeansPlusPlus(List<FilmingLocation> locations, int k) {
        log.info("K-means++ 클러스터링 시작: {}개 장소, {}개 클러스터", locations.size(), k);

        if (locations.size() <= k) {
            Map<Integer, List<FilmingLocation>> clusters = new HashMap<>();
            for (int i = 0; i < locations.size(); i++) {
                clusters.put(i, Collections.singletonList(locations.get(i)));
            }
            return clusters;
        }

        // 1. K-means++ 초기화로 중심점 선택
        List<FilmingLocation> centers = initializeCentersWithKMeansPlusPlus(locations, k);

        // 2. K-means 반복
        Map<Integer, List<FilmingLocation>> clusters = new HashMap<>();
        boolean converged = false;
        int iteration = 0;

        while (!converged && iteration < MAX_ITERATIONS) {
            // 클러스터 초기화
            for (int i = 0; i < k; i++) {
                clusters.put(i, new ArrayList<>());
            }

            // 각 장소를 가장 가까운 중심점에 할당
            for (FilmingLocation location : locations) {
                int nearestCluster = findNearestCluster(location, centers);
                clusters.get(nearestCluster).add(location);
            }

            // 빈 클러스터 처리
            handleEmptyClusters(clusters, locations, centers);

            // 새로운 중심점 계산
            List<FilmingLocation> newCenters = calculateNewCenters(clusters);

            // 수렴 검사
            converged = isCenterConverged(centers, newCenters);
            centers = newCenters;
            iteration++;
        }

        log.info("K-means++ 완료: {}회 반복", iteration);
        return clusters;
    }

    /**
     * K-means++ 방식으로 초기 중심점 선택
     */
    private List<FilmingLocation> initializeCentersWithKMeansPlusPlus(List<FilmingLocation> locations, int k) {
        List<FilmingLocation> centers = new ArrayList<>();
        Random random = new Random();

        // 첫 번째 중심점 랜덤 선택
        centers.add(locations.get(random.nextInt(locations.size())));

        // K-means++ 방식으로 나머지 중심점 선택
        while (centers.size() < k) {
            double[] distances = new double[locations.size()];
            double totalDistance = 0;

            for (int i = 0; i < locations.size(); i++) {
                FilmingLocation location = locations.get(i);
                double minDistance = Double.MAX_VALUE;

                // 가장 가까운 중심점까지의 거리 계산
                for (FilmingLocation center : centers) {
                    double distance = calculateHaversineDistance(location, center);
                    minDistance = Math.min(minDistance, distance);
                }

                distances[i] = minDistance * minDistance; // 거리 제곱
                totalDistance += distances[i];
            }

            // 확률적으로 다음 중심점 선택
            double randomValue = random.nextDouble() * totalDistance;
            double cumulativeDistance = 0;

            for (int i = 0; i < locations.size(); i++) {
                cumulativeDistance += distances[i];
                if (cumulativeDistance >= randomValue) {
                    centers.add(locations.get(i));
                    break;
                }
            }
        }

        return centers;
    }

    /**
     * 하버사인 공식으로 두 지점 간 거리 계산 (km)
     */
    private double calculateHaversineDistance(FilmingLocation loc1, FilmingLocation loc2) {
        final double R = 6371; // 지구 반지름 (km)

        double lat1Rad = Math.toRadians(loc1.getLatitude());
        double lat2Rad = Math.toRadians(loc2.getLatitude());
        double deltaLat = Math.toRadians(loc2.getLatitude() - loc1.getLatitude());
        double deltaLon = Math.toRadians(loc2.getLongitude() - loc1.getLongitude());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                        Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * 장소에서 가장 가까운 클러스터 중심점 찾기
     */
    private int findNearestCluster(FilmingLocation location, List<FilmingLocation> centers) {
        int nearestCluster = 0;
        double minDistance = Double.MAX_VALUE;

        for (int i = 0; i < centers.size(); i++) {
            double distance = calculateHaversineDistance(location, centers.get(i));
            if (distance < minDistance) {
                minDistance = distance;
                nearestCluster = i;
            }
        }

        return nearestCluster;
    }

    /**
     * 빈 클러스터 처리
     */
    private void handleEmptyClusters(Map<Integer, List<FilmingLocation>> clusters,
                                     List<FilmingLocation> locations, List<FilmingLocation> centers) {
        // 빈 클러스터 찾기
        List<Integer> emptyClusters = clusters.entrySet().stream()
                .filter(entry -> entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (emptyClusters.isEmpty()) return;

        // 가장 큰 클러스터에서 가장 먼 점을 빈 클러스터로 이동
        for (Integer emptyCluster : emptyClusters) {
            Map.Entry<Integer, List<FilmingLocation>> largestCluster = clusters.entrySet().stream()
                    .filter(entry -> entry.getValue().size() > 1)
                    .max(Comparator.comparingInt(entry -> entry.getValue().size()))
                    .orElse(null);

            if (largestCluster != null) {
                List<FilmingLocation> clusterLocations = largestCluster.getValue();
                FilmingLocation center = centers.get(largestCluster.getKey());

                // 중심점에서 가장 먼 점 찾기
                FilmingLocation farthest = clusterLocations.stream()
                        .max(Comparator.comparingDouble(loc -> calculateHaversineDistance(loc, center)))
                        .orElse(null);

                if (farthest != null) {
                    clusterLocations.remove(farthest);
                    clusters.get(emptyCluster).add(farthest);
                    centers.set(emptyCluster, farthest);
                }
            }
        }
    }

    /**
     * 새로운 클러스터 중심점 계산 (기하학적 중심)
     */
    private List<FilmingLocation> calculateNewCenters(Map<Integer, List<FilmingLocation>> clusters) {
        List<FilmingLocation> newCenters = new ArrayList<>();

        for (int i = 0; i < clusters.size(); i++) {
            List<FilmingLocation> clusterLocations = clusters.get(i);

            if (clusterLocations.isEmpty()) {
                newCenters.add(null);
                continue;
            }

            // 클러스터 내 모든 점의 중심에 가장 가까운 실제 장소 선택
            double avgLat = clusterLocations.stream().mapToDouble(FilmingLocation::getLatitude).average().orElse(0.0);
            double avgLng = clusterLocations.stream().mapToDouble(FilmingLocation::getLongitude).average().orElse(0.0);

            FilmingLocation newCenter = clusterLocations.stream()
                    .min(Comparator.comparingDouble(loc -> {
                        double deltaLat = loc.getLatitude() - avgLat;
                        double deltaLng = loc.getLongitude() - avgLng;
                        return Math.sqrt(deltaLat * deltaLat + deltaLng * deltaLng);
                    }))
                    .orElse(clusterLocations.get(0));

            newCenters.add(newCenter);
        }

        return newCenters;
    }

    /**
     * 중심점 수렴 여부 확인
     */
    private boolean isCenterConverged(List<FilmingLocation> oldCenters, List<FilmingLocation> newCenters) {
        for (int i = 0; i < oldCenters.size(); i++) {
            FilmingLocation oldCenter = oldCenters.get(i);
            FilmingLocation newCenter = newCenters.get(i);

            if (oldCenter == null && newCenter == null) continue;
            if (oldCenter == null || newCenter == null) return false;
            if (!oldCenter.getId().equals(newCenter.getId())) return false;
        }
        return true;
    }

    /**
     * 9개 이상 클러스터 분할
     */
    private Map<Integer, List<FilmingLocation>> splitLargeClusters(Map<Integer, List<FilmingLocation>> clusters) {
        Map<Integer, List<FilmingLocation>> result = new HashMap<>();
        int nextClusterId = 0;

        for (List<FilmingLocation> clusterLocations : clusters.values()) {
            if (clusterLocations.size() < MAX_LOCATIONS_PER_CLUSTER) {
                result.put(nextClusterId++, new ArrayList<>(clusterLocations));
            } else {
                // 클러스터 분할
                List<List<FilmingLocation>> splitClusters = splitCluster(clusterLocations);
                for (List<FilmingLocation> splitCluster : splitClusters) {
                    result.put(nextClusterId++, splitCluster);
                }
            }
        }

        log.info("클러스터 분할 후: {}개 클러스터", result.size());
        return result;
    }

    /**
     * 단일 클러스터를 여러 개로 분할
     */
    private List<List<FilmingLocation>> splitCluster(List<FilmingLocation> locations) {
        int numSplits = (int) Math.ceil((double) locations.size() / MIN_LOCATIONS_AFTER_SPLIT);
        List<List<FilmingLocation>> splits = new ArrayList<>();

        // 간단한 분할: 순서대로 나누기 (더 정교한 방법 사용 가능)
        for (int i = 0; i < numSplits; i++) {
            splits.add(new ArrayList<>());
        }

        for (int i = 0; i < locations.size(); i++) {
            splits.get(i % numSplits).add(locations.get(i));
        }

        return splits.stream().filter(split -> !split.isEmpty()).collect(Collectors.toList());
    }

    /**
     * Open TSP로 각 클러스터의 일일 경로 생성
     */
    private List<TripPlanResponseDto.DailyRouteDto> createDailyRoutesWithOpenTSP(
            Map<Integer, List<FilmingLocation>> clusters, List<LocationDistanceInfo> distanceMatrix) {

        List<TripPlanResponseDto.DailyRouteDto> dailyRoutes = new ArrayList<>();

        // 거리 행렬을 Map으로 변환
        Map<String, Integer> travelTimeMap = new HashMap<>();
        for (LocationDistanceInfo info : distanceMatrix) {
            String key = info.getFromLocationId() + "-" + info.getToLocationId();
            travelTimeMap.put(key, info.getTravelTimeMinutes());
        }

        for (int day = 0; day < clusters.size(); day++) {
            List<FilmingLocation> clusterLocations = clusters.get(day);
            if (clusterLocations.isEmpty()) continue;

            // 야경 키워드가 있는 장소 찾기
            FilmingLocation nightViewLocation = clusterLocations.stream()
                    .filter(loc -> loc.getRecommendationKeywords().contains(NIGHT_VIEW_KEYWORD))
                    .findFirst()
                    .orElse(null);

            // Open TSP로 최적 경로 계산
            List<FilmingLocation> optimalRoute;
            if (nightViewLocation != null) {
                // 도착지 고정 Open TSP
                optimalRoute = solveOpenTSPWithFixedEnd(clusterLocations, nightViewLocation, travelTimeMap);
            } else {
                // 일반 Open TSP
                optimalRoute = solveOpenTSP(clusterLocations, travelTimeMap);
            }

            // 일일 경로 DTO 생성
            TripPlanResponseDto.DailyRouteDto dailyRoute = createDailyRouteDto(
                    optimalRoute, travelTimeMap, day + 1);
            dailyRoutes.add(dailyRoute);
        }

        return dailyRoutes;
    }

    /**
     * 도착지 고정 Open TSP 해결
     */
    private List<FilmingLocation> solveOpenTSPWithFixedEnd(List<FilmingLocation> locations,
                                                           FilmingLocation fixedEnd,
                                                           Map<String, Integer> travelTimeMap) {
        List<FilmingLocation> route = new ArrayList<>();
        List<FilmingLocation> remainingLocations = new ArrayList<>(locations);
        remainingLocations.remove(fixedEnd);

        if (remainingLocations.isEmpty()) {
            return Collections.singletonList(fixedEnd);
        }

        // 최근접 이웃 알고리즘으로 경로 구성 (마지막 제외)
        FilmingLocation current = remainingLocations.get(0);
        route.add(current);
        remainingLocations.remove(current);

        while (!remainingLocations.isEmpty()) {
            FilmingLocation nearest = findNearestLocation(current, remainingLocations, travelTimeMap);
            route.add(nearest);
            remainingLocations.remove(nearest);
            current = nearest;
        }

        // 고정된 도착지 추가
        route.add(fixedEnd);

        log.info("도착지 고정 Open TSP 완료: {} -> ... -> {}",
                route.get(0).getName(), fixedEnd.getName());

        return route;
    }

    /**
     * 일반 Open TSP 해결
     */
    private List<FilmingLocation> solveOpenTSP(List<FilmingLocation> locations,
                                               Map<String, Integer> travelTimeMap) {
        if (locations.size() <= 1) {
            return new ArrayList<>(locations);
        }

        List<FilmingLocation> route = new ArrayList<>();
        List<FilmingLocation> remainingLocations = new ArrayList<>(locations);

        // 첫 번째 장소 선택 (임의)
        FilmingLocation current = remainingLocations.get(0);
        route.add(current);
        remainingLocations.remove(current);

        // 최근접 이웃 알고리즘
        while (!remainingLocations.isEmpty()) {
            FilmingLocation nearest = findNearestLocation(current, remainingLocations, travelTimeMap);
            route.add(nearest);
            remainingLocations.remove(nearest);
            current = nearest;
        }

        log.info("Open TSP 완료: {}개 장소", route.size());
        return route;
    }

    /**
     * 현재 위치에서 가장 가까운 장소 찾기
     */
    private FilmingLocation findNearestLocation(FilmingLocation current,
                                                List<FilmingLocation> candidates,
                                                Map<String, Integer> travelTimeMap) {
        FilmingLocation nearest = null;
        int minTime = Integer.MAX_VALUE;

        for (FilmingLocation candidate : candidates) {
            String key = current.getId() + "-" + candidate.getId();
            Integer travelTime = travelTimeMap.get(key);
            if (travelTime != null && travelTime < minTime) {
                minTime = travelTime;
                nearest = candidate;
            }
        }

        return nearest != null ? nearest : candidates.get(0);
    }

    /**
     * 일일 경로 DTO 생성
     */
    private TripPlanResponseDto.DailyRouteDto createDailyRouteDto(List<FilmingLocation> route,
                                                                  Map<String, Integer> travelTimeMap,
                                                                  int day) {
        List<TripPlanResponseDto.LocationRouteDto> locationRouteDtos = new ArrayList<>();
        int totalTravelTime = 0;

        for (int i = 0; i < route.size(); i++) {
            FilmingLocation location = route.get(i);

            // 다음 장소까지의 이동 시간 계산
            Integer travelTimeToNext = null;
            if (i < route.size() - 1) {
                FilmingLocation nextLocation = route.get(i + 1);
                String key = location.getId() + "-" + nextLocation.getId();
                travelTimeToNext = travelTimeMap.get(key);
                if (travelTimeToNext != null) {
                    totalTravelTime += travelTimeToNext;
                }
            }

            // 컨셉 결정
            String concept = location.getRecommendationKeywords().stream()
                    .map(conceptKeywordMapper::getConceptByKeyword)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            // 이미지 가져오기
            List<String> images = getLocationImages(location);

            TripPlanResponseDto.LocationRouteDto locationRouteDto = TripPlanResponseDto.LocationRouteDto.builder()
                    .locationId(location.getId())
                    .locationName(location.getName())
                    .address(location.getAddress())
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude())
                    .visitOrder(i + 1)
                    .travelTimeToNext(travelTimeToNext)
                    .recommendationKeywords(location.getRecommendationKeywords())
                    .concept(concept)
                    .images(images)
                    .build();

            locationRouteDtos.add(locationRouteDto);
        }

        return TripPlanResponseDto.DailyRouteDto.builder()
                .day(day)
                .locations(locationRouteDtos)
                .travelTimeMinutes(totalTravelTime)
                .build();
    }

    /**
     * 장소 이미지 가져오기 (수정된 GoogleMapsService 사용)
     */
    private List<String> getLocationImages(FilmingLocation location) {
        List<String> images = new ArrayList<>();

        if (location.getImages() != null && !location.getImages().isEmpty()) {
            images = location.getImages();
        } else if (location.getLatitude() != null && location.getLongitude() != null) {
            try {
                // 특정 장소 이미지 가져오기 (수정된 메서드 사용)
                images = googleMapsService.getSpecificPlaceImages(
                        location.getAddress(),
                        location.getLatitude(),
                        location.getLongitude(),
                        location.getName()
                );
                log.debug("장소 '{}' 이미지 조회: {}개", location.getName(), images.size());
            } catch (Exception e) {
                log.warn("장소 '{}' 이미지 조회 실패: {}", location.getName(), e.getMessage());
            }
        }

        return images;
    }

    /**
     * 빈 응답 생성
     */
    private TripPlanResponseDto createEmptyResponse() {
        return TripPlanResponseDto.builder()
                .dailyRoutes(Collections.emptyList())
                .totalDays(0)
                .totalLocations(0)
                .totalTravelTimeMinutes(0)
                .build();
    }
}