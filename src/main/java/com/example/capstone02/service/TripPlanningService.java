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
    private final GoogleMapsDistanceService distanceService;
    private final LocationTravelTimeService locationTravelTimeService;
    private final ConceptKeywordMapper conceptKeywordMapper;

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int MAX_ITERATIONS = 100; // K-means 최대 반복 횟수
    private static final String NIGHT_VIEW_KEYWORD = "야경"; // 야경 키워드

    /**
     * 여행 경로 계획 생성
     */
    public TripPlanResponseDto createTripPlan(TripPlanRequestDto request) {
        log.info("여행 경로 계획 생성 시작: {}", request);

        // 1. 영화 ID로 촬영지 정보 조회
        List<FilmingLocation> allLocations = filmingLocationService.getFilmingLocationsByMovieId(request.getMovieId());

        if (allLocations.isEmpty()) {
            log.warn("영화 ID {}의 촬영지 정보가 없습니다", request.getMovieId());
            return createEmptyResponse();
        }

        // 2. 국가 필터링
        if (request.getCountry() != null && !request.getCountry().isEmpty()) {
            allLocations = allLocations.stream()
                    .filter(location -> request.getCountry().equals(location.getCountry()))
                    .collect(Collectors.toList());
        }

        // 3. 컨셉 필터링
        List<String> allConceptKeywords = getConceptKeywords(request);
        if (!allConceptKeywords.isEmpty()) {
            allLocations = filterLocationsByKeywords(allLocations, allConceptKeywords);
        }

        if (allLocations.isEmpty()) {
            log.warn("필터링 후 조건에 맞는 촬영지가 없습니다");
            return createEmptyResponse();
        }

        log.info("필터링 후 촬영지 수: {}", allLocations.size());

        // 위도/경도 정보 없는 장소 필터링
        allLocations = allLocations.stream()
                .filter(location -> location.getLatitude() != null && location.getLongitude() != null)
                .collect(Collectors.toList());

        // 4. 장소 간 이동 시간 계산 및 거리 행렬 생성
        List<LocationDistanceInfo> distanceMatrix = calculateDistanceMatrix(allLocations, request.getOriginLat(), request.getOriginLng());

        // 이동 시간 거리 행렬 로깅
        logDistanceMatrix(distanceMatrix);

        // 5. K-means++ 클러스터링 수행
        int travelMinutesPerDay = (request.getTravelHours() != null ? request.getTravelHours() : 8) * MINUTES_PER_HOUR;
        int totalTravelTime = calculateTotalTravelTime(allLocations, distanceMatrix);
        int k = Math.max(1, (int) Math.ceil((double) totalTravelTime / travelMinutesPerDay));

        log.info("클러스터링 설정 - 총 이동 시간: {}분, 하루 여행 시간: {}분, 필요한 일수(k): {}",
                totalTravelTime, travelMinutesPerDay, k);

        Map<Integer, List<FilmingLocation>> clusters = performKMeansPlusPlus(
                allLocations, k, distanceMatrix, request.getOriginLat(), request.getOriginLng());

        // 6. 각 클러스터 내에서 야경 키워드 있는 장소를 마지막으로 설정하여 경로 계산
        List<TripPlanResponseDto.DailyRouteDto> dailyRoutes = new ArrayList<>();
        int totalLocations = 0;
        int totalTravelTimeMinutes = 0;

        for (int clusterId = 0; clusterId < clusters.size(); clusterId++) {
            List<FilmingLocation> clusterLocations = clusters.get(clusterId);
            if (clusterLocations.isEmpty()) continue;

            totalLocations += clusterLocations.size();

            // 야경 키워드가 있는 장소 찾기
            Optional<FilmingLocation> nightViewLocation = findLocationWithKeyword(clusterLocations, NIGHT_VIEW_KEYWORD);

            // 클러스터 내 최적 경로 계산 (야경 장소가 있으면 마지막으로 고정)
            List<FilmingLocation> optimalRoute = calculateOptimalRouteWithConstraint(
                    clusterLocations, distanceMatrix,
                    request.getOriginLat(), request.getOriginLng(),
                    nightViewLocation.orElse(null), clusterId == 0);

            // 경로 변환하여 DailyRouteDto 생성
            TripPlanResponseDto.DailyRouteDto dailyRoute = createDailyRouteFromLocations(
                    optimalRoute, distanceMatrix, clusterId + 1);

            dailyRoutes.add(dailyRoute);
            totalTravelTimeMinutes += dailyRoute.getTravelTimeMinutes();
        }

        // 7. 클러스터 간 방문 순서 최적화 (출발지에서 각 클러스터 첫 장소까지의 거리 기준)
        List<TripPlanResponseDto.DailyRouteDto> optimizedRoutes = optimizeClusterOrder(
                dailyRoutes, allLocations, distanceMatrix, request.getOriginLat(), request.getOriginLng());

        // 8. 최종 여행 계획 구성
        return TripPlanResponseDto.builder()
                .dailyRoutes(optimizedRoutes)
                .totalDays(optimizedRoutes.size())
                .totalLocations(totalLocations)
                .totalTravelTimeMinutes(totalTravelTimeMinutes)
                .build();
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

    /**
     * 컨셉 키워드 목록 가져오기
     */
    private List<String> getConceptKeywords(TripPlanRequestDto request) {
        List<String> allConceptKeywords = new ArrayList<>();

        // 이전 버전과의 호환성 유지 - 단일 컨셉
        if (request.getConcept() != null && !request.getConcept().isEmpty()) {
            allConceptKeywords.addAll(conceptKeywordMapper.getKeywordsByConcept(request.getConcept()));
        }

        // 다중 컨셉 지원
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
     * 키워드로 장소 필터링
     */
    private List<FilmingLocation> filterLocationsByKeywords(List<FilmingLocation> locations, List<String> keywords) {
        return locations.stream()
                .filter(location -> {
                    List<String> locationKeywords = location.getRecommendationKeywords();
                    return locationKeywords.stream().anyMatch(keywords::contains);
                })
                .collect(Collectors.toList());
    }

    /**
     * 장소 간 이동 시간 거리 행렬 계산
     */
    private List<LocationDistanceInfo> calculateDistanceMatrix(List<FilmingLocation> locations, Double originLat, Double originLng) {
        int locationCount = locations.size();
        Long movieId = null;

        if (!locations.isEmpty()) {
            movieId = locations.get(0).getMovie().getId();
        }

        log.info("장소 간 거리 행렬 계산 시작: {}개 장소, 영화 ID: {}", locationCount, movieId);

        List<LocationDistanceInfo> distanceMatrix = new ArrayList<>();

        // 1. 출발지와 각 장소 사이의 거리 계산 (최초 클러스터 선택용)
        if (originLat != null && originLng != null) {
            List<LocationDistanceInfo> originDistances = locationTravelTimeService.calculateDistancesFromOrigin(
                    originLat, originLng, locations);
            distanceMatrix.addAll(originDistances);
        }

        // 2. 장소 간 이동 시간 가져오기 (DB에 저장된 값)
        if (movieId != null) {
            List<Long> locationIds = locations.stream()
                    .map(FilmingLocation::getId)
                    .collect(Collectors.toList());

            // DB에서 필터링된 장소 간 이동 시간 정보 가져오기
            List<LocationDistanceInfo> savedTravelTimes =
                    locationTravelTimeService.getFilteredTravelTimes(movieId, locationIds);

            if (!savedTravelTimes.isEmpty()) {
                log.info("DB에서 {}개의 이동 시간 정보를 불러왔습니다.", savedTravelTimes.size());
                distanceMatrix.addAll(savedTravelTimes);
            } else {
                log.warn("DB에 저장된 이동 시간 정보가 없습니다. 실시간 계산을 진행합니다.");

                // 3. DB에 저장된 정보가 없을 경우 실시간 계산
                for (int i = 0; i < locationCount; i++) {
                    for (int j = i + 1; j < locationCount; j++) {
                        FilmingLocation fromLocation = locations.get(i);
                        FilmingLocation toLocation = locations.get(j);

                        int[] result = distanceService.calculateDistance(
                                fromLocation.getLatitude(), fromLocation.getLongitude(),
                                toLocation.getLatitude(), toLocation.getLongitude());

                        // a->b 방향 저장
                        LocationDistanceInfo forwardInfo = LocationDistanceInfo.builder()
                                .fromLocationId(fromLocation.getId())
                                .toLocationId(toLocation.getId())
                                .fromLocationName(fromLocation.getName())
                                .toLocationName(toLocation.getName())
                                .distanceMeters(result[0])
                                .travelTimeMinutes(result[1] / SECONDS_PER_MINUTE)
                                .build();

                        distanceMatrix.add(forwardInfo);

                        // b->a 방향은 동일한 값으로 저장 (대칭성)
                        LocationDistanceInfo backwardInfo = LocationDistanceInfo.builder()
                                .fromLocationId(toLocation.getId())
                                .toLocationId(fromLocation.getId())
                                .fromLocationName(toLocation.getName())
                                .toLocationName(fromLocation.getName())
                                .distanceMeters(result[0])
                                .travelTimeMinutes(result[1] / SECONDS_PER_MINUTE)
                                .build();

                        distanceMatrix.add(backwardInfo);
                    }
                }
            }
        }

        log.info("장소 간 거리 행렬 계산 완료: {}개 계산됨", distanceMatrix.size());
        return distanceMatrix;
    }

    /**
     * 거리 행렬 로깅
     */
    private void logDistanceMatrix(List<LocationDistanceInfo> distanceMatrix) {
        log.info("===== 이동 시간 거리 행렬 =====");
        Map<Long, Map<Long, Integer>> distanceMap = new HashMap<>();

        // 맵 형태로 변환
        for (LocationDistanceInfo info : distanceMatrix) {
            if (info.getFromLocationId() == 0) continue; // 출발지는 제외

            distanceMap.computeIfAbsent(info.getFromLocationId(), k -> new HashMap<>())
                    .put(info.getToLocationId(), info.getTravelTimeMinutes());
        }

        // 로그 출력
        for (Map.Entry<Long, Map<Long, Integer>> fromEntry : distanceMap.entrySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append("From ID ").append(fromEntry.getKey()).append(": ");

            for (Map.Entry<Long, Integer> toEntry : fromEntry.getValue().entrySet()) {
                sb.append("[To ID ").append(toEntry.getKey())
                        .append("=").append(toEntry.getValue()).append("분] ");
            }

            log.info(sb.toString());
        }
        log.info("==========================");
    }

    /**
     * 총 이동 시간 계산
     */
    private int calculateTotalTravelTime(List<FilmingLocation> locations, List<LocationDistanceInfo> distanceMatrix) {
        if (locations.size() <= 1) {
            return 0;
        }

        // 최소 신장 트리(MST)를 이용해 대략적인 이동 시간 추정
        Map<Long, Map<Long, Integer>> graph = new HashMap<>();

        // 그래프 구성
        for (LocationDistanceInfo info : distanceMatrix) {
            if (info.getFromLocationId() == 0) continue; // 출발지는 제외

            if (!containsLocation(locations, info.getFromLocationId()) ||
                    !containsLocation(locations, info.getToLocationId())) {
                continue;
            }

            graph.computeIfAbsent(info.getFromLocationId(), k -> new HashMap<>())
                    .put(info.getToLocationId(), info.getTravelTimeMinutes());
        }

        // 임의로 하나의 장소를 선택해 시작
        Long startNodeId = locations.get(0).getId();

        // 프림 알고리즘으로 MST 찾기
        int totalTime = 0;
        Set<Long> visited = new HashSet<>();
        PriorityQueue<Map.Entry<Long, Integer>> pq = new PriorityQueue<>(
                Comparator.comparingInt(Map.Entry::getValue));

        visited.add(startNodeId);

        // 시작 노드의 이웃을 우선순위 큐에 추가
        if (graph.containsKey(startNodeId)) {
            for (Map.Entry<Long, Integer> edge : graph.get(startNodeId).entrySet()) {
                pq.offer(new AbstractMap.SimpleEntry<>(edge.getKey(), edge.getValue()));
            }
        }

        // MST 구성
        while (!pq.isEmpty() && visited.size() < locations.size()) {
            Map.Entry<Long, Integer> edge = pq.poll();
            Long node = edge.getKey();

            if (visited.contains(node)) continue;

            totalTime += edge.getValue();
            visited.add(node);

            // 새 노드의 이웃 추가
            if (graph.containsKey(node)) {
                for (Map.Entry<Long, Integer> nextEdge : graph.get(node).entrySet()) {
                    if (!visited.contains(nextEdge.getKey())) {
                        pq.offer(new AbstractMap.SimpleEntry<>(nextEdge.getKey(), nextEdge.getValue()));
                    }
                }
            }
        }

        // MST에 1.5 가중치를 적용 (TSP 근사)
        return (int) (totalTime * 1.5);
    }

    /**
     * 특정 ID의 장소가 목록에 포함되어 있는지 확인
     */
    private boolean containsLocation(List<FilmingLocation> locations, Long locationId) {
        return locations.stream().anyMatch(loc -> loc.getId().equals(locationId));
    }

    /**
     * K-means++ 클러스터링 수행
     */
    private Map<Integer, List<FilmingLocation>> performKMeansPlusPlus(
            List<FilmingLocation> locations, int k, List<LocationDistanceInfo> distanceMatrix,
            Double originLat, Double originLng) {

        log.info("K-means++ 클러스터링 시작: {}개 장소, {}개 클러스터", locations.size(), k);

        int n = locations.size();
        if (n <= k) {
            // 장소 수가 클러스터 수보다 적거나 같으면 각 장소를 별도 클러스터로
            Map<Integer, List<FilmingLocation>> clusters = new HashMap<>();
            for (int i = 0; i < n; i++) {
                clusters.put(i, Collections.singletonList(locations.get(i)));
            }
            return clusters;
        }

        // 1. K-means++ 방식으로 클러스터 중심 초기화
        List<FilmingLocation> centers = initializeClusterCenters(locations, k, distanceMatrix, originLat, originLng);

        // 2. 각 장소의 클러스터 할당
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
                int nearestCenterId = findNearestCenterId(location, centers, distanceMatrix);
                clusters.get(nearestCenterId).add(location);
            }

            // 빈 클러스터 처리
            handleEmptyClusters(clusters, locations, centers, distanceMatrix);

            // 새 중심점 계산
            List<FilmingLocation> newCenters = calculateNewCenters(clusters, distanceMatrix);

            // 수렴 체크 (중심점이 더 이상 변경되지 않으면)
            boolean centersChanged = !areCentersEqual(centers, newCenters);
            if (!centersChanged) {
                converged = true;
            } else {
                centers = newCenters;
            }

            iteration++;
        }

        log.info("K-means++ 클러스터링 완료: {}회 반복, {}개 클러스터", iteration, clusters.size());

        // 클러스터 결과 로깅
        for (int i = 0; i < clusters.size(); i++) {
            List<FilmingLocation> clusterLocations = clusters.get(i);
            log.info("클러스터 {}: {}개 장소 - {}", i, clusterLocations.size(),
                    clusterLocations.stream().map(FilmingLocation::getName).collect(Collectors.joining(", ")));
        }

        // 출발지와 가장 가까운 클러스터를 0번 클러스터로 조정
        if (originLat != null && originLng != null) {
            adjustClusterOrderByOrigin(clusters, distanceMatrix, originLat, originLng);
        }

        return clusters;
    }

    /**
     * K-means++ 방식으로 클러스터 중심 초기화
     */
    private List<FilmingLocation> initializeClusterCenters(
            List<FilmingLocation> locations, int k, List<LocationDistanceInfo> distanceMatrix,
            Double originLat, Double originLng) {

        List<FilmingLocation> centers = new ArrayList<>();
        Random random = new Random();

        if (originLat != null && originLng != null) {
            // 출발지와 가장 가까운 장소를 첫번째 중심으로 선택
            FilmingLocation firstCenter = locations.stream()
                    .min(Comparator.comparingInt(loc ->
                            distanceMatrix.stream()
                                    .filter(info -> info.getFromLocationId() == 0 && info.getToLocationId().equals(loc.getId()))
                                    .findFirst()
                                    .map(LocationDistanceInfo::getTravelTimeMinutes)
                                    .orElse(Integer.MAX_VALUE)))
                    .orElse(locations.get(0));

            centers.add(firstCenter);
        } else {
            // 출발지 정보가 없으면 랜덤으로 첫번째 장소 선택
            centers.add(locations.get(random.nextInt(locations.size())));
        }

        // K-means++ 방식으로 나머지 중심점 선택
        while (centers.size() < k) {
            // 각 장소에 대해 가장 가까운 중심점까지의 거리 계산
            Map<FilmingLocation, Double> distancesToNearest = new HashMap<>();
            double totalDistance = 0;

            for (FilmingLocation loc : locations) {
                if (centers.contains(loc)) continue;

                int minDistance = minDistanceToAnyCenter(loc, centers, distanceMatrix);
                distancesToNearest.put(loc, (double) minDistance);
                totalDistance += minDistance;
            }

            if (totalDistance == 0) break;

            // 확률적으로 다음 중심점 선택 (거리가 멀수록 높은 확률)
            double rand = random.nextDouble() * totalDistance;
            double cumulativeProb = 0;
            FilmingLocation nextCenter = null;

            for (Map.Entry<FilmingLocation, Double> entry : distancesToNearest.entrySet()) {
                cumulativeProb += entry.getValue();
                if (cumulativeProb >= rand) {
                    nextCenter = entry.getKey();
                    break;
                }
            }

            if (nextCenter != null) {
                centers.add(nextCenter);
            } else if (!distancesToNearest.isEmpty()) {
                // 랜덤 선택이 실패한 경우, 가장 먼 장소 선택
                nextCenter = Collections.max(distancesToNearest.entrySet(), Map.Entry.comparingByValue()).getKey();
                centers.add(nextCenter);
            } else {
                break;
            }
        }

        log.info("K-means++ 중심점 초기화 완료: {}개 중심점", centers.size());
        return centers;
    }

    /**
     * 장소와 중심점들 사이의 최소 거리 계산
     */
    private int minDistanceToAnyCenter(
            FilmingLocation location, List<FilmingLocation> centers, List<LocationDistanceInfo> distanceMatrix) {

        return centers.stream()
                .mapToInt(center ->
                        distanceMatrix.stream()
                                .filter(info ->
                                        (info.getFromLocationId().equals(location.getId()) && info.getToLocationId().equals(center.getId())))
                                .findFirst()
                                .map(LocationDistanceInfo::getTravelTimeMinutes)
                                .orElse(Integer.MAX_VALUE))
                .min()
                .orElse(Integer.MAX_VALUE);
    }

    /**
     * 장소와 가장 가까운 중심점 ID 찾기
     */
    private int findNearestCenterId(
            FilmingLocation location, List<FilmingLocation> centers, List<LocationDistanceInfo> distanceMatrix) {

        int nearestCenterId = 0;
        int minDistance = Integer.MAX_VALUE;

        for (int i = 0; i < centers.size(); i++) {
            FilmingLocation center = centers.get(i);
            int distance = distanceMatrix.stream()
                    .filter(info ->
                            (info.getFromLocationId().equals(location.getId()) && info.getToLocationId().equals(center.getId())))
                    .findFirst()
                    .map(LocationDistanceInfo::getTravelTimeMinutes)
                    .orElse(Integer.MAX_VALUE);

            if (distance < minDistance) {
                minDistance = distance;
                nearestCenterId = i;
            }
        }

        return nearestCenterId;
    }

    /**
     * 빈 클러스터 처리
     */
    private void handleEmptyClusters(
            Map<Integer, List<FilmingLocation>> clusters, List<FilmingLocation> locations,
            List<FilmingLocation> centers, List<LocationDistanceInfo> distanceMatrix) {

        // 빈 클러스터 찾기
        List<Integer> emptyClusters = clusters.entrySet().stream()
                .filter(entry -> entry.getValue().isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        if (emptyClusters.isEmpty()) {
            return;
        }

        // 가장 큰 클러스터 찾기
        Map.Entry<Integer, List<FilmingLocation>> largestCluster = clusters.entrySet().stream()
                .max(Comparator.comparingInt(entry -> entry.getValue().size()))
                .orElse(null);

        if (largestCluster == null || largestCluster.getValue().size() <= 1) {
            return; // 재할당할 클러스터가 없음
        }

        // 빈 클러스터에 가장 큰 클러스터에서 장소 재할당
        for (Integer emptyClusterId : emptyClusters) {
            if (largestCluster.getValue().size() > 1) {
                // 클러스터 중심과 가장 멀리 떨어진 장소 선택
                FilmingLocation farthestLocation = largestCluster.getValue().stream()
                        .max(Comparator.comparingInt(loc -> {
                            FilmingLocation center = centers.get(largestCluster.getKey());
                            return distanceMatrix.stream()
                                    .filter(info ->
                                            (info.getFromLocationId().equals(loc.getId()) && info.getToLocationId().equals(center.getId())))
                                    .findFirst()
                                    .map(LocationDistanceInfo::getTravelTimeMinutes)
                                    .orElse(0);
                        }))
                        .orElse(null);

                if (farthestLocation != null) {
                    largestCluster.getValue().remove(farthestLocation);
                    clusters.get(emptyClusterId).add(farthestLocation);
                    // 새 클러스터의 중심 업데이트
                    centers.set(emptyClusterId, farthestLocation);
                }
            }
        }
    }

    /**
     * 새로운 클러스터 중심점 계산
     */
    private List<FilmingLocation> calculateNewCenters(
            Map<Integer, List<FilmingLocation>> clusters, List<LocationDistanceInfo> distanceMatrix) {

        List<FilmingLocation> newCenters = new ArrayList<>();

        for (int i = 0; i < clusters.size(); i++) {
            List<FilmingLocation> clusterLocations = clusters.get(i);

            if (clusterLocations.isEmpty()) {
                // 빈 클러스터는 이전 중심점 유지
                newCenters.add(null);
                continue;
            }

            if (clusterLocations.size() == 1) {
                // 장소가 하나뿐인 클러스터는 그 장소가 중심
                newCenters.add(clusterLocations.get(0));
                continue;
            }

            // 클러스터 내 모든 장소 쌍 간의 평균 거리 계산
            FilmingLocation bestCenter = clusterLocations.stream()
                    .min(Comparator.comparingDouble(loc ->
                            clusterLocations.stream()
                                    .filter(other -> !other.equals(loc))
                                    .mapToInt(other ->
                                            distanceMatrix.stream()
                                                    .filter(info ->
                                                            (info.getFromLocationId().equals(loc.getId()) && info.getToLocationId().equals(other.getId())))
                                                    .findFirst()
                                                    .map(LocationDistanceInfo::getTravelTimeMinutes)
                                                    .orElse(Integer.MAX_VALUE))
                                    .average()
                                    .orElse(Double.MAX_VALUE)))
                    .orElse(clusterLocations.get(0));

            newCenters.add(bestCenter);
        }

        return newCenters;
    }

    /**
     * 중심점들이 동일한지 확인
     */
    private boolean areCentersEqual(List<FilmingLocation> centers1, List<FilmingLocation> centers2) {
        if (centers1.size() != centers2.size()) {
            return false;
        }

        for (int i = 0; i < centers1.size(); i++) {
            FilmingLocation center1 = centers1.get(i);
            FilmingLocation center2 = centers2.get(i);

            if ((center1 == null && center2 != null) ||
                    (center1 != null && center2 == null) ||
                    (center1 != null && center2 != null && !center1.getId().equals(center2.getId()))) {
                return false;
            }
        }

        return true;
    }

    /**
     * 출발지와 가장 가까운 클러스터를 0번째로 조정
     */
    private void adjustClusterOrderByOrigin(
            Map<Integer, List<FilmingLocation>> clusters, List<LocationDistanceInfo> distanceMatrix,
            Double originLat, Double originLng) {

        if (clusters.size() <= 1) {
            return;
        }

        // 각 클러스터에서 출발지와 가장 가까운 장소를 찾아 거리 계산
        Map<Integer, Integer> clusterDistancesToOrigin = new HashMap<>();

        for (Map.Entry<Integer, List<FilmingLocation>> entry : clusters.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }

            // 클러스터 내 출발지와 가장 가까운 장소 찾기
            FilmingLocation closestLocation = entry.getValue().stream()
                    .min(Comparator.comparingInt(loc ->
                            distanceMatrix.stream()
                                    .filter(info -> info.getFromLocationId() == 0 && info.getToLocationId().equals(loc.getId()))
                                    .findFirst()
                                    .map(LocationDistanceInfo::getTravelTimeMinutes)
                                    .orElse(Integer.MAX_VALUE)))
                    .orElse(entry.getValue().get(0));

            // 출발지와의 거리 저장
            int distanceToOrigin = distanceMatrix.stream()
                    .filter(info -> info.getFromLocationId() == 0 && info.getToLocationId().equals(closestLocation.getId()))
                    .findFirst()
                    .map(LocationDistanceInfo::getTravelTimeMinutes)
                    .orElse(Integer.MAX_VALUE);

            clusterDistancesToOrigin.put(entry.getKey(), distanceToOrigin);
        }

        // 출발지와 가장 가까운 클러스터 찾기
        int closestClusterId = clusterDistancesToOrigin.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);

        // 필요한 경우 클러스터 인덱스 교환
        if (closestClusterId != 0) {
            List<FilmingLocation> temp = clusters.get(0);
            clusters.put(0, clusters.get(closestClusterId));
            clusters.put(closestClusterId, temp);
        }
    }

    /**
     * 특정 키워드를 가진 장소 찾기
     */
    private Optional<FilmingLocation> findLocationWithKeyword(List<FilmingLocation> locations, String keyword) {
        return locations.stream()
                .filter(location -> location.getRecommendationKeywords() != null &&
                        location.getRecommendationKeywords().contains(keyword))
                .findFirst();
    }

    /**
     * 클러스터 내에서 제약 조건이 있는 최적 경로 계산 (Open TSP)
     * - 야경 장소가 있으면 마지막 방문지로 고정
     * - 첫번째 클러스터는 출발지를 고려
     */
    private List<FilmingLocation> calculateOptimalRouteWithConstraint(
            List<FilmingLocation> locations, List<LocationDistanceInfo> distanceMatrix,
            Double originLat, Double originLng, FilmingLocation fixedEndLocation, boolean isFirstCluster) {

        log.info("클러스터 내 최적 경로 계산 시작: {}개 장소, 고정 종료 장소={}",
                locations.size(), fixedEndLocation != null ? fixedEndLocation.getName() : "없음");

        if (locations.size() <= 1) {
            return new ArrayList<>(locations);
        }

        // 고정 장소가 있으면 제외하고 계산 후 마지막에 추가
        List<FilmingLocation> locationsToRoute = new ArrayList<>(locations);
        if (fixedEndLocation != null) {
            locationsToRoute.remove(fixedEndLocation);
        }

        if (locationsToRoute.isEmpty()) {
            return new ArrayList<>(locations); // 고정 장소만 있는 경우
        }

        // 경로 계산에 사용할 장소 목록
        List<FilmingLocation> route = new ArrayList<>();

        // 첫번째 장소 선택
        FilmingLocation first;
        if (isFirstCluster && originLat != null && originLng != null) {
            // 출발지와 가장 가까운 장소를 첫번째로 선택
            first = locationsToRoute.stream()
                    .min(Comparator.comparingInt(loc ->
                            distanceMatrix.stream()
                                    .filter(info -> info.getFromLocationId() == 0 && info.getToLocationId().equals(loc.getId()))
                                    .findFirst()
                                    .map(LocationDistanceInfo::getTravelTimeMinutes)
                                    .orElse(Integer.MAX_VALUE)))
                    .orElse(locationsToRoute.get(0));
        } else {
            // 임의로 첫번째 장소 선택
            first = locationsToRoute.get(0);
        }

        route.add(first);
        locationsToRoute.remove(first);

        // 최근접 이웃 알고리즘으로 남은 장소들 연결 (Open TSP)
        FilmingLocation current = first;
        while (!locationsToRoute.isEmpty()) {
            FilmingLocation next = findNearestLocation(current, locationsToRoute, distanceMatrix);
            route.add(next);
            locationsToRoute.remove(next);
            current = next;
        }

        // 고정 장소가 있으면 마지막에 추가
        if (fixedEndLocation != null) {
            route.add(fixedEndLocation);
        }

        log.info("최적 경로 계산 완료: {}개 장소, 경로={}",
                route.size(), route.stream().map(FilmingLocation::getName).collect(Collectors.joining(" -> ")));

        return route;
    }

    /**
     * 현재 장소에서 가장 가까운 다음 장소 찾기
     */
    private FilmingLocation findNearestLocation(
            FilmingLocation current, List<FilmingLocation> candidates, List<LocationDistanceInfo> distanceMatrix) {

        return candidates.stream()
                .min(Comparator.comparingInt(loc ->
                        distanceMatrix.stream()
                                .filter(info -> info.getFromLocationId().equals(current.getId()) &&
                                        info.getToLocationId().equals(loc.getId()))
                                .findFirst()
                                .map(LocationDistanceInfo::getTravelTimeMinutes)
                                .orElse(Integer.MAX_VALUE)))
                .orElse(candidates.get(0));
    }

    /**
     * 클러스터 방문 순서 최적화 (Open TSP)
     */
    private List<TripPlanResponseDto.DailyRouteDto> optimizeClusterOrder(
            List<TripPlanResponseDto.DailyRouteDto> dailyRoutes,
            List<FilmingLocation> allLocations,
            List<LocationDistanceInfo> distanceMatrix,
            Double originLat, Double originLng) {

        if (dailyRoutes.size() <= 1) {
            return dailyRoutes;
        }

        log.info("클러스터 간 방문 순서 최적화 시작: {}개 클러스터", dailyRoutes.size());

        // 각 클러스터의 첫 번째 장소 ID 목록
        Map<Integer, Long> firstLocationIds = new HashMap<>();
        for (int i = 0; i < dailyRoutes.size(); i++) {
            TripPlanResponseDto.DailyRouteDto route = dailyRoutes.get(i);
            if (!route.getLocations().isEmpty()) {
                firstLocationIds.put(i, route.getLocations().get(0).getLocationId());
            }
        }

        // 출발지에서 각 클러스터 첫 장소까지의 거리 계산
        Map<Integer, Integer> distancesToOrigin = new HashMap<>();
        for (Map.Entry<Integer, Long> entry : firstLocationIds.entrySet()) {
            int clusterId = entry.getKey();
            Long firstLocationId = entry.getValue();

            int distance = distanceMatrix.stream()
                    .filter(info -> info.getFromLocationId() == 0 && info.getToLocationId().equals(firstLocationId))
                    .findFirst()
                    .map(LocationDistanceInfo::getTravelTimeMinutes)
                    .orElse(Integer.MAX_VALUE);

            distancesToOrigin.put(clusterId, distance);
        }

        // 출발지와 가장 가까운 클러스터부터 방문하는 순서 계산 (Open TSP)
        List<Integer> clusterOrder = new ArrayList<>();
        Set<Integer> visitedClusters = new HashSet<>();

        // 출발지와 가장 가까운 클러스터부터 시작
        int currentCluster = distancesToOrigin.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(0);

        clusterOrder.add(currentCluster);
        visitedClusters.add(currentCluster);

        // 클러스터 간의 거리 행렬 생성
        Map<Integer, Map<Integer, Integer>> interClusterDistances = calculateInterClusterDistances(
                firstLocationIds, distanceMatrix);

        // 최근접 이웃 방식으로 나머지 클러스터 방문 순서 결정
        while (visitedClusters.size() < dailyRoutes.size()) {
            int nextCluster = -1;
            int minDistance = Integer.MAX_VALUE;

            for (int i = 0; i < dailyRoutes.size(); i++) {
                if (visitedClusters.contains(i)) continue;

                int distance = interClusterDistances.getOrDefault(currentCluster, Collections.emptyMap())
                        .getOrDefault(i, Integer.MAX_VALUE);

                if (distance < minDistance) {
                    minDistance = distance;
                    nextCluster = i;
                }
            }

            if (nextCluster == -1) break;

            clusterOrder.add(nextCluster);
            visitedClusters.add(nextCluster);
            currentCluster = nextCluster;
        }

        // 방문 순서대로 일정 재배치
        List<TripPlanResponseDto.DailyRouteDto> optimizedRoutes = new ArrayList<>();
        for (int i = 0; i < clusterOrder.size(); i++) {
            TripPlanResponseDto.DailyRouteDto route = dailyRoutes.get(clusterOrder.get(i));
            route.setDay(i + 1); // 일차 번호 재조정
            optimizedRoutes.add(route);
        }

        log.info("클러스터 방문 순서 최적화 완료: {}",
                clusterOrder.stream().map(String::valueOf).collect(Collectors.joining(" -> ")));

        return optimizedRoutes;
    }

    /**
     * 클러스터 간 거리 계산
     */
    private Map<Integer, Map<Integer, Integer>> calculateInterClusterDistances(
            Map<Integer, Long> firstLocationIds, List<LocationDistanceInfo> distanceMatrix) {

        Map<Integer, Map<Integer, Integer>> interClusterDistances = new HashMap<>();

        for (Map.Entry<Integer, Long> fromEntry : firstLocationIds.entrySet()) {
            int fromCluster = fromEntry.getKey();
            Long fromLocationId = fromEntry.getValue();

            Map<Integer, Integer> distances = new HashMap<>();

            for (Map.Entry<Integer, Long> toEntry : firstLocationIds.entrySet()) {
                int toCluster = toEntry.getKey();
                if (fromCluster == toCluster) continue;

                Long toLocationId = toEntry.getValue();

                int distance = distanceMatrix.stream()
                        .filter(info -> info.getFromLocationId().equals(fromLocationId) &&
                                info.getToLocationId().equals(toLocationId))
                        .findFirst()
                        .map(LocationDistanceInfo::getTravelTimeMinutes)
                        .orElse(Integer.MAX_VALUE);

                distances.put(toCluster, distance);
            }

            interClusterDistances.put(fromCluster, distances);
        }

        return interClusterDistances;
    }

    /**
     * 장소 리스트를 일일 경로 DTO로 변환
     */
    private TripPlanResponseDto.DailyRouteDto createDailyRouteFromLocations(
            List<FilmingLocation> locations, List<LocationDistanceInfo> distanceMatrix, int day) {

        List<TripPlanResponseDto.LocationRouteDto> locationRouteDtos = new ArrayList<>();
        int totalTravelTime = 0;

        for (int i = 0; i < locations.size(); i++) {
            FilmingLocation location = locations.get(i);

            // 다음 장소까지의 이동 시간과 거리 계산
            Integer travelTimeToNext = null;
            Integer distanceToNext = null;

            if (i < locations.size() - 1) {
                FilmingLocation nextLocation = locations.get(i + 1);

                LocationDistanceInfo distanceInfo = distanceMatrix.stream()
                        .filter(info ->
                                info.getFromLocationId().equals(location.getId()) &&
                                        info.getToLocationId().equals(nextLocation.getId()))
                        .findFirst()
                        .orElse(null);

                if (distanceInfo != null) {
                    travelTimeToNext = distanceInfo.getTravelTimeMinutes();
                    distanceToNext = distanceInfo.getDistanceMeters();
                    totalTravelTime += travelTimeToNext;
                }
            }

            // 장소의 컨셉 결정 (첫 번째 일치하는 키워드의 컨셉)
            String concept = null;
            for (String keyword : location.getRecommendationKeywords()) {
                concept = conceptKeywordMapper.getConceptByKeyword(keyword);
                if (concept != null) break;
            }

            // LocationRouteDto 생성
            TripPlanResponseDto.LocationRouteDto locationRouteDto = TripPlanResponseDto.LocationRouteDto.builder()
                    .locationId(location.getId())
                    .locationName(location.getName())
                    .address(location.getAddress())
                    .latitude(location.getLatitude())
                    .longitude(location.getLongitude())
                    .visitOrder(i + 1)
                    .travelTimeToNext(travelTimeToNext)
                    .travelDistanceToNext(distanceToNext)
                    .recommendationKeywords(location.getRecommendationKeywords())
                    .concept(concept)
                    .build();

            locationRouteDtos.add(locationRouteDto);
        }

        // DailyRouteDto 생성
        return TripPlanResponseDto.DailyRouteDto.builder()
                .day(day)
                .locations(locationRouteDtos)
                .travelTimeMinutes(totalTravelTime)
                .build();
    }
}