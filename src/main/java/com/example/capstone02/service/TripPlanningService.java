package com.example.capstone02.service;

import com.example.capstone02.dto.DistanceMatrixResponseDto;
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
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripPlanningService {

    private final FilmingLocationService filmingLocationService;
    private final GoogleMapsDistanceService distanceService;
    private final ConceptKeywordMapper conceptKeywordMapper;

    private static final int SECONDS_PER_MINUTE = 60;
    private static final int MINUTES_PER_HOUR = 60;
    private static final int MAX_ITERATIONS = 100; // K-means 최대 반복 횟수

    /**
     * 여행 경로 계획 생성
     */
    public TripPlanResponseDto createTripPlan(TripPlanRequestDto request) {
        log.info("여행 경로 계획 생성 시작: {}", request);

        // 1. 영화 ID로 촬영지 정보 조회
        List<FilmingLocation> allLocations = filmingLocationService.getFilmingLocationsByMovieId(request.getMovieId());

        if (allLocations.isEmpty()) {
            log.warn("영화 ID {}의 촬영지 정보가 없습니다", request.getMovieId());
            return TripPlanResponseDto.builder()
                    .dailyRoutes(Collections.emptyList())
                    .totalDays(0)
                    .totalLocations(0)
                    .totalTravelTimeMinutes(0)
                    .build();
        }

        // 2. 국가 필터링
        if (request.getCountry() != null && !request.getCountry().isEmpty()) {
            allLocations = allLocations.stream()
                    .filter(location -> request.getCountry().equals(location.getCountry()))
                    .collect(Collectors.toList());
        }

//        // 3. 컨셉 필터링
//        if (request.getConcept() != null && !request.getConcept().isEmpty()) {
//            // 컨셉에 해당하는 키워드 목록 가져오기
//            List<String> conceptKeywords = conceptKeywordMapper.getKeywordsByConcept(request.getConcept());
//
//            // 각 촬영지의 추천 키워드와 컨셉 키워드가 하나라도 일치하는 장소만 필터링
//            allLocations = allLocations.stream()
//                    .filter(location -> {
//                        List<String> locationKeywords = location.getRecommendationKeywords();
//                        return locationKeywords.stream().anyMatch(conceptKeywords::contains);
//                    })
//                    .collect(Collectors.toList());
//        }

        // 3. 컨셉 필터링
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

        // 컨셉 키워드가 있는 경우에만 필터링 적용
        if (!allConceptKeywords.isEmpty()) {
            // 각 촬영지의 추천 키워드와 컨셉 키워드가 하나라도 일치하는 장소만 필터링
            allLocations = allLocations.stream()
                    .filter(location -> {
                        List<String> locationKeywords = location.getRecommendationKeywords();
                        return locationKeywords.stream().anyMatch(allConceptKeywords::contains);
                    })
                    .collect(Collectors.toList());
        }
        if (allLocations.isEmpty()) {
            log.warn("필터링 후 조건에 맞는 촬영지가 없습니다");
            return TripPlanResponseDto.builder()
                    .dailyRoutes(Collections.emptyList())
                    .totalDays(0)
                    .totalLocations(0)
                    .totalTravelTimeMinutes(0)
                    .build();
        }

        log.info("필터링 후 촬영지 수: {}", allLocations.size());

        // 위도/경도 정보 없는 장소 필터링
        allLocations = allLocations.stream()
                .filter(location -> location.getLatitude() != null && location.getLongitude() != null)
                .collect(Collectors.toList());

        // 4. 장소 간 거리 및 시간 계산
        List<LocationDistanceInfo> distanceInfoList = calculateAllDistances(allLocations, request.getOriginLat(), request.getOriginLng());

        // 5. 클러스터링을 위한 데이터 준비
        int totalTravelTimeMinutes = calculateTotalTravelTime(allLocations, distanceInfoList);
        int travelHoursPerDay = request.getTravelHours() != null ? request.getTravelHours() : 8; // 기본값 8시간
        int travelMinutesPerDay = travelHoursPerDay * MINUTES_PER_HOUR;

        // 필요한 일수(클러스터 수) 계산
        int requiredDays = (int) Math.ceil((double) totalTravelTimeMinutes / travelMinutesPerDay);
        if (requiredDays < 1) requiredDays = 1;

        log.info("총 이동 시간: {}분, 하루 이동 가능 시간: {}분, 필요 일수: {}",
                totalTravelTimeMinutes, travelMinutesPerDay, requiredDays);

        // 6. K-means 클러스터링 수행
        Map<Integer, List<FilmingLocation>> clusters = performKMeansClustering(
                allLocations, requiredDays, distanceInfoList, request.getOriginLat(), request.getOriginLng());

        // 7. 각 클러스터 내에서 최적 경로 계산
        List<TripPlanResponseDto.DailyRouteDto> dailyRoutes = calculateOptimalRoutes(
                clusters, distanceInfoList, request.getOriginLat(), request.getOriginLng());

        // 8. 최종 여행 계획 구성
        TripPlanResponseDto tripPlan = TripPlanResponseDto.builder()
                .dailyRoutes(dailyRoutes)
                .totalDays(dailyRoutes.size())
                .totalLocations(allLocations.size())
                .totalTravelTimeMinutes(calculateTotalTravelTimeFromRoutes(dailyRoutes))
                .build();

        log.info("여행 경로 계획 생성 완료: 총 {}일, {}개 장소, 총 이동시간 {}분",
                tripPlan.getTotalDays(), tripPlan.getTotalLocations(), tripPlan.getTotalTravelTimeMinutes());

        return tripPlan;
    }

    /**
     * 모든 장소 간 거리 및 시간 계산
     */
    private List<LocationDistanceInfo> calculateAllDistances(List<FilmingLocation> locations, Double originLat, Double originLng) {
        List<LocationDistanceInfo> distanceInfoList = new ArrayList<>();
        int locationCount = locations.size();

        log.info("장소 간 거리 계산 시작: {}개 장소, 총 {}개 조합", locationCount, locationCount * (locationCount - 1));

        // 출발지와 각 장소 사이의 거리 계산 (최초 클러스터 선택용)
        if (originLat != null && originLng != null) {
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

                distanceInfoList.add(info);
            }
        }

        // 모든 장소 간 거리 계산 (n^2 복잡도)
        for (int i = 0; i < locationCount; i++) {
            for (int j = 0; j < locationCount; j++) {
                if (i != j) { // 같은 장소는 계산 제외
                    FilmingLocation fromLocation = locations.get(i);
                    FilmingLocation toLocation = locations.get(j);

                    int[] result = distanceService.calculateDistance(
                            fromLocation.getLatitude(), fromLocation.getLongitude(),
                            toLocation.getLatitude(), toLocation.getLongitude());

                    LocationDistanceInfo info = LocationDistanceInfo.builder()
                            .fromLocationId(fromLocation.getId())
                            .toLocationId(toLocation.getId())
                            .fromLocationName(fromLocation.getName())
                            .toLocationName(toLocation.getName())
                            .distanceMeters(result[0])
                            .travelTimeMinutes(result[1] / SECONDS_PER_MINUTE)
                            .build();

                    distanceInfoList.add(info);
                }
            }
        }

        log.info("장소 간 거리 계산 완료: {}개 계산됨", distanceInfoList.size());
        return distanceInfoList;
    }

    /**
     * 장소들을 한 번씩 방문하는데 필요한 총 이동 시간 계산
     */
    private int calculateTotalTravelTime(List<FilmingLocation> locations, List<LocationDistanceInfo> distanceInfoList) {
        if (locations.size() <= 1) {
            return 0;
        }

        // 최소 신장 트리(MST)를 이용해 대략적인 이동 시간 추정
        // 실제로는 TSP 문제이지만, 간소화를 위해 MST * 1.5 정도로 추정
        Map<Long, Map<Long, Integer>> graph = new HashMap<>();

        // 그래프 구성
        for (LocationDistanceInfo info : distanceInfoList) {
            if (info.getFromLocationId() == 0) continue; // 출발지는 제외

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
     * K-means 클러스터링 수행
     */
    private Map<Integer, List<FilmingLocation>> performKMeansClustering(
            List<FilmingLocation> locations, int k, List<LocationDistanceInfo> distanceInfoList,
            Double originLat, Double originLng) {

        log.info("K-means 클러스터링 시작: {}개 장소, {}개 클러스터", locations.size(), k);

        int n = locations.size();
        if (n <= k) {
            // 장소 수가 클러스터 수보다 적거나 같으면 각 장소를 별도 클러스터로
            Map<Integer, List<FilmingLocation>> clusters = new HashMap<>();
            for (int i = 0; i < n; i++) {
                clusters.put(i, Collections.singletonList(locations.get(i)));
            }
            return clusters;
        }

        // 1. 클러스터 중심 초기화 (여기서는 실제 장소를 중심으로 사용)
        List<FilmingLocation> centers = initializeClusterCenters(locations, k, distanceInfoList, originLat, originLng);

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
                int nearestCenterId = findNearestCenterId(location, centers, distanceInfoList);
                clusters.get(nearestCenterId).add(location);
            }

            // 빈 클러스터 처리
            handleEmptyClusters(clusters, locations, centers, distanceInfoList);

            // 새 중심점 계산
            List<FilmingLocation> newCenters = calculateNewCenters(clusters, distanceInfoList);

            // 수렴 체크 (중심점이 더 이상 변경되지 않으면)
            boolean centersChanged = !areCentersEqual(centers, newCenters);
            if (!centersChanged) {
                converged = true;
            } else {
                centers = newCenters;
            }

            iteration++;
        }

        log.info("K-means 클러스터링 완료: {}회 반복, {}개 클러스터", iteration, clusters.size());

        // 3. 출발지와 가장 가까운 클러스터를 0번 클러스터로 조정
        if (originLat != null && originLng != null) {
            adjustClusterOrderByOrigin(clusters, distanceInfoList, originLat, originLng);
        }

        return clusters;
    }

    /**
     * 클러스터 중심 초기화 (출발지와 가까운 장소부터 시작)
     */
    private List<FilmingLocation> initializeClusterCenters(
            List<FilmingLocation> locations, int k, List<LocationDistanceInfo> distanceInfoList,
            Double originLat, Double originLng) {

        List<FilmingLocation> centers = new ArrayList<>();

        if (originLat != null && originLng != null) {
            // 출발지와 가장 가까운 장소를 첫번째 중심으로 선택
            FilmingLocation firstCenter = locations.stream()
                    .min(Comparator.comparingInt(loc ->
                            distanceInfoList.stream()
                                    .filter(info -> info.getFromLocationId() == 0 && info.getToLocationId().equals(loc.getId()))
                                    .findFirst()
                                    .map(LocationDistanceInfo::getTravelTimeMinutes)
                                    .orElse(Integer.MAX_VALUE)))
                    .orElse(locations.get(0));

            centers.add(firstCenter);
        } else {
            // 출발지 정보가 없으면 임의로 첫번째 장소 선택
            centers.add(locations.get(0));
        }

        // 나머지 중심점은 기존 중심점과 가장 멀리 떨어진 장소들로 선택 (K-means++ 방식)
        while (centers.size() < k) {
            FilmingLocation nextCenter = locations.stream()
                    .filter(loc -> !centers.contains(loc))
                    .max(Comparator.comparingInt(loc ->
                            minDistanceToAnyCenter(loc, centers, distanceInfoList)))
                    .orElse(null);

            if (nextCenter != null) {
                centers.add(nextCenter);
            } else {
                break; // 더 이상 중심점을 추가할 수 없음
            }
        }

        return centers;
    }

    /**
     * 장소와 중심점들 사이의 최소 거리 계산
     */
    private int minDistanceToAnyCenter(
            FilmingLocation location, List<FilmingLocation> centers, List<LocationDistanceInfo> distanceInfoList) {

        return centers.stream()
                .mapToInt(center ->
                        distanceInfoList.stream()
                                .filter(info ->
                                        (info.getFromLocationId().equals(location.getId()) && info.getToLocationId().equals(center.getId())) ||
                                                (info.getFromLocationId().equals(center.getId()) && info.getToLocationId().equals(location.getId())))
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
            FilmingLocation location, List<FilmingLocation> centers, List<LocationDistanceInfo> distanceInfoList) {

        int nearestCenterId = 0;
        int minDistance = Integer.MAX_VALUE;

        for (int i = 0; i < centers.size(); i++) {
            FilmingLocation center = centers.get(i);
            int distance = distanceInfoList.stream()
                    .filter(info ->
                            (info.getFromLocationId().equals(location.getId()) && info.getToLocationId().equals(center.getId())) ||
                                    (info.getFromLocationId().equals(center.getId()) && info.getToLocationId().equals(location.getId())))
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
     * 빈 클러스터 처리 (가장 큰 클러스터에서 장소 재할당)
     */
    private void handleEmptyClusters(
            Map<Integer, List<FilmingLocation>> clusters, List<FilmingLocation> locations,
            List<FilmingLocation> centers, List<LocationDistanceInfo> distanceInfoList) {

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
                            return distanceInfoList.stream()
                                    .filter(info ->
                                            (info.getFromLocationId().equals(loc.getId()) && info.getToLocationId().equals(center.getId())) ||
                                                    (info.getFromLocationId().equals(center.getId()) && info.getToLocationId().equals(loc.getId())))
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
     * 새로운 클러스터 중심점 계산 (클러스터 내 장소들 간 평균 거리가 최소인 장소)
     */
    private List<FilmingLocation> calculateNewCenters(
            Map<Integer, List<FilmingLocation>> clusters, List<LocationDistanceInfo> distanceInfoList) {

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
                                            distanceInfoList.stream()
                                                    .filter(info ->
                                                            (info.getFromLocationId().equals(loc.getId()) && info.getToLocationId().equals(other.getId())) ||
                                                                    (info.getFromLocationId().equals(other.getId()) && info.getToLocationId().equals(loc.getId())))
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
            Map<Integer, List<FilmingLocation>> clusters, List<LocationDistanceInfo> distanceInfoList,
            Double originLat, Double originLng) {

        if (clusters.size() <= 1) {
            return;
        }

        // 각 클러스터의 대표 장소(중심)와 출발지 사이의 거리 계산
        Map<Integer, Integer> clusterDistancesToOrigin = new HashMap<>();

        for (Map.Entry<Integer, List<FilmingLocation>> entry : clusters.entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }

            // 클러스터 내 출발지와 가장 가까운 장소 찾기
            FilmingLocation closestLocation = entry.getValue().stream()
                    .min(Comparator.comparingInt(loc ->
                            distanceInfoList.stream()
                                    .filter(info -> info.getFromLocationId() == 0 && info.getToLocationId().equals(loc.getId()))
                                    .findFirst()
                                    .map(LocationDistanceInfo::getTravelTimeMinutes)
                                    .orElse(Integer.MAX_VALUE)))
                    .orElse(entry.getValue().get(0));

            // 출발지와의 거리 저장
            int distanceToOrigin = distanceInfoList.stream()
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
     * 각 클러스터 내에서 최적 경로 계산 (TSP 근사 - 가장 가까운 이웃 휴리스틱)
     */
    private List<TripPlanResponseDto.DailyRouteDto> calculateOptimalRoutes(
            Map<Integer, List<FilmingLocation>> clusters, List<LocationDistanceInfo> distanceInfoList,
            Double originLat, Double originLng) {

        List<TripPlanResponseDto.DailyRouteDto> dailyRoutes = new ArrayList<>();

        // 클러스터 간 거리 계산
        Map<Integer, Map<Integer, Integer>> interClusterDistances = calculateInterClusterDistances(
                clusters, distanceInfoList);

        // 클러스터 방문 순서 결정 (0번 클러스터부터 시작하여 가장 가까운 클러스터 방문)
        List<Integer> clusterVisitOrder = calculateClusterVisitOrder(interClusterDistances);

        // 각 클러스터에 대한 일일 경로 계산
        for (int i = 0; i < clusterVisitOrder.size(); i++) {
            int clusterId = clusterVisitOrder.get(i);
            List<FilmingLocation> clusterLocations = clusters.get(clusterId);

            if (clusterLocations.isEmpty()) {
                continue;
            }

            // 클러스터 내 최적 경로 계산
            List<FilmingLocation> optimalRoute = calculateOptimalRouteWithinCluster(
                    clusterLocations, distanceInfoList, originLat, originLng, clusterId == 0);

            // 경로 변환하여 DailyRouteDto 생성
            TripPlanResponseDto.DailyRouteDto dailyRoute = createDailyRouteFromLocations(
                    optimalRoute, distanceInfoList, i + 1);

            dailyRoutes.add(dailyRoute);
        }

        return dailyRoutes;
    }

    /**
     * 클러스터 간 거리 계산 (각 클러스터의 중심점 간 거리)
     */
    private Map<Integer, Map<Integer, Integer>> calculateInterClusterDistances(
            Map<Integer, List<FilmingLocation>> clusters, List<LocationDistanceInfo> distanceInfoList) {

        Map<Integer, Map<Integer, Integer>> interClusterDistances = new HashMap<>();

        for (int i = 0; i < clusters.size(); i++) {
            interClusterDistances.put(i, new HashMap<>());

            for (int j = 0; j < clusters.size(); j++) {
                if (i == j) continue;

                List<FilmingLocation> clusterI = clusters.get(i);
                List<FilmingLocation> clusterJ = clusters.get(j);

                if (clusterI.isEmpty() || clusterJ.isEmpty()) {
                    interClusterDistances.get(i).put(j, Integer.MAX_VALUE);
                    continue;
                }

                // 각 클러스터에서 서로 가장 가까운 장소 쌍의 거리 계산
                int minDistance = Integer.MAX_VALUE;

                for (FilmingLocation locI : clusterI) {
                    for (FilmingLocation locJ : clusterJ) {
                        int distance = distanceInfoList.stream()
                                .filter(info ->
                                        (info.getFromLocationId().equals(locI.getId()) && info.getToLocationId().equals(locJ.getId())) ||
                                                (info.getFromLocationId().equals(locJ.getId()) && info.getToLocationId().equals(locI.getId())))
                                .findFirst()
                                .map(LocationDistanceInfo::getTravelTimeMinutes)
                                .orElse(Integer.MAX_VALUE);

                        if (distance < minDistance) {
                            minDistance = distance;
                        }
                    }
                }

                interClusterDistances.get(i).put(j, minDistance);
            }
        }

        return interClusterDistances;
    }

    /**
     * 클러스터 방문 순서 계산 (0번 클러스터로부터 가장 가까운 순서로)
     */
    private List<Integer> calculateClusterVisitOrder(Map<Integer, Map<Integer, Integer>> interClusterDistances) {
        List<Integer> visitOrder = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();

        // 0번 클러스터부터 시작
        int currentCluster = 0;
        visitOrder.add(currentCluster);
        visited.add(currentCluster);

        // 모든 클러스터를 가장 가까운 순서로 방문
        while (visited.size() < interClusterDistances.size()) {
            int nextCluster = -1;
            int minDistance = Integer.MAX_VALUE;

            Map<Integer, Integer> distances = interClusterDistances.get(currentCluster);

            for (Map.Entry<Integer, Integer> entry : distances.entrySet()) {
                int cluster = entry.getKey();
                int distance = entry.getValue();

                if (!visited.contains(cluster) && distance < minDistance) {
                    minDistance = distance;
                    nextCluster = cluster;
                }
            }

            if (nextCluster == -1) break; // 더 이상 방문할 클러스터 없음

            visitOrder.add(nextCluster);
            visited.add(nextCluster);
            currentCluster = nextCluster;
        }

        return visitOrder;
    }

    /**
     * 클러스터 내에서 최적 경로 계산 (가장 가까운 이웃 휴리스틱)
     */
    private List<FilmingLocation> calculateOptimalRouteWithinCluster(
            List<FilmingLocation> locations, List<LocationDistanceInfo> distanceInfoList,
            Double originLat, Double originLng, boolean isFirstCluster) {

        if (locations.size() <= 1) {
            return new ArrayList<>(locations);
        }

        List<FilmingLocation> route = new ArrayList<>();
        Set<FilmingLocation> unvisited = new HashSet<>(locations);

        // 첫번째 클러스터의 경우, 출발지와 가장 가까운 장소부터 시작
        FilmingLocation current;
        if (isFirstCluster && originLat != null && originLng != null) {
            current = locations.stream()
                    .min(Comparator.comparingInt(loc ->
                            distanceInfoList.stream()
                                    .filter(info -> info.getFromLocationId() == 0 && info.getToLocationId().equals(loc.getId()))
                                    .findFirst()
                                    .map(LocationDistanceInfo::getTravelTimeMinutes)
                                    .orElse(Integer.MAX_VALUE)))
                    .orElse(locations.get(0));
        } else {
            // 다른 클러스터는 임의의 장소부터 시작
            current = locations.get(0);
        }

        route.add(current);
        unvisited.remove(current);

        // 가장 가까운 이웃 알고리즘
        while (!unvisited.isEmpty()) {
            FilmingLocation nearest = findNearestLocation(current, unvisited, distanceInfoList);
            route.add(nearest);
            unvisited.remove(nearest);
            current = nearest;
        }

        return route;
    }

    /**
     * 현재 장소에서 가장 가까운 다음 장소 찾기
     */
    private FilmingLocation findNearestLocation(
            FilmingLocation current, Set<FilmingLocation> candidates, List<LocationDistanceInfo> distanceInfoList) {

        return candidates.stream()
                .min(Comparator.comparingInt(loc ->
                        distanceInfoList.stream()
                                .filter(info ->
                                        (info.getFromLocationId().equals(current.getId()) && info.getToLocationId().equals(loc.getId())) ||
                                                (info.getFromLocationId().equals(loc.getId()) && info.getToLocationId().equals(current.getId())))
                                .findFirst()
                                .map(LocationDistanceInfo::getTravelTimeMinutes)
                                .orElse(Integer.MAX_VALUE)))
                .orElse(null);
    }

    /**
     * 장소 리스트를 일일 경로 DTO로 변환
     */
    private TripPlanResponseDto.DailyRouteDto createDailyRouteFromLocations(
            List<FilmingLocation> locations, List<LocationDistanceInfo> distanceInfoList, int day) {

        List<TripPlanResponseDto.LocationRouteDto> locationRouteDtos = new ArrayList<>();
        int totalTravelTime = 0;

        for (int i = 0; i < locations.size(); i++) {
            FilmingLocation location = locations.get(i);

            // 다음 장소까지의 이동 시간과 거리 계산
            Integer travelTimeToNext = null;
            Integer distanceToNext = null;

            if (i < locations.size() - 1) {
                FilmingLocation nextLocation = locations.get(i + 1);

                LocationDistanceInfo distanceInfo = distanceInfoList.stream()
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

    /**
     * 모든 일일 경로의 총 이동 시간 계산
     */
    private int calculateTotalTravelTimeFromRoutes(List<TripPlanResponseDto.DailyRouteDto> dailyRoutes) {
        return dailyRoutes.stream()
                .mapToInt(TripPlanResponseDto.DailyRouteDto::getTravelTimeMinutes)
                .sum();
    }
}