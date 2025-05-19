package com.example.capstone02.service;

import com.example.capstone02.dto.DistanceMatrixResponseDto;
import com.google.maps.DistanceMatrixApi;
import com.google.maps.DistanceMatrixApiRequest;
import com.google.maps.GeoApiContext;
import com.google.maps.errors.ApiException;
import com.google.maps.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleMapsDistanceService {

    private final WebClient webClient;

    @Value("${google.maps.api-key}")
    private String apiKey;

    // 지구 반경 (킬로미터)
    private static final double EARTH_RADIUS_KM = 6371.0;

    // GeoApiContext 빈 생성
    private GeoApiContext getGeoContext() {
        return new GeoApiContext.Builder()
                .apiKey(apiKey)
                .build();
    }

    /**
     * Google Distance Matrix API를 사용하여 두 장소 간의 거리와 시간을 계산
     *
     * @param originLat 출발지 위도
     * @param originLng 출발지 경도
     * @param destLat 도착지 위도
     * @param destLng 도착지 경도
     * @return 거리(미터)와 시간(초) 정보가 담긴 배열, [0]: 거리(미터), [1]: 시간(초)
     */
    public int[] calculateDistance(double originLat, double originLng, double destLat, double destLng) {
        try {
            GeoApiContext context = getGeoContext();

            LatLng origin = new LatLng(originLat, originLng);
            LatLng destination = new LatLng(destLat, destLng);

            DistanceMatrixApiRequest request = DistanceMatrixApi.newRequest(context)
                    .origins(origin)
                    .destinations(destination)
                    .mode(TravelMode.DRIVING);

            DistanceMatrix result = request.await();
            context.shutdown();

            if (result.rows.length > 0 && result.rows[0].elements.length > 0) {
                DistanceMatrixElement element = result.rows[0].elements[0];

                if (element.status == DistanceMatrixElementStatus.OK) {
                    int distance = (int) element.distance.inMeters;
                    int duration = (int) element.duration.inSeconds;

                    log.debug("API 거리 계산 결과: origin=({}, {}), dest=({}, {}), 거리={}m, 시간={}초",
                            originLat, originLng, destLat, destLng, distance, duration);

                    return new int[] { distance, duration };
                }
            }

            log.warn("API 거리 계산 결과가 없습니다: origin=({}, {}), dest=({}, {})",
                    originLat, originLng, destLat, destLng);

            // API 결과가 없을 경우 직선 거리로 계산한 결과를 사용
            return calculateHaversineDistance(originLat, originLng, destLat, destLng);

        } catch (ApiException | InterruptedException | IOException e) {
            log.error("거리 계산 API 에러: {}", e.getMessage());
            // API 호출 실패 시 직선 거리로 계산
            return calculateHaversineDistance(originLat, originLng, destLat, destLng);
        }
    }

    /**
     * 하버사인 공식을 사용한 두 지점 간의 직선 거리 계산 (API 호출 실패 시 대체용)
     * 거리에 기반하여 근사적인 시간도 계산 (평균 차량 속도 60km/h 가정)
     */
    private int[] calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        // 위도, 경도를 라디안으로 변환
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        // 하버사인 공식
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // 거리 (킬로미터)
        double distanceKm = EARTH_RADIUS_KM * c;

        // 미터 단위 변환
        int distanceMeters = (int) (distanceKm * 1000);

        // 시간 계산 (평균 속도 60km/h 가정)
        // 시간(시간) = 거리(km) / 속도(km/h)
        double timeHours = distanceKm / 60.0;
        int timeSeconds = (int) (timeHours * 3600); // 초 단위 변환

        log.debug("직선 거리 계산 결과: lat1={}, lon1={}, lat2={}, lon2={}, 거리={}m, 시간={}초",
                lat1, lon1, lat2, lon2, distanceMeters, timeSeconds);

        return new int[] { distanceMeters, timeSeconds };
    }

    /**
     * 여러 장소들 간의 거리 행렬을 계산 (WebClient 사용, 대량 데이터 처리용)
     *
     * @param origins 출발지 위도/경도 목록 ("lat,lng" 형식)
     * @param destinations 도착지 위도/경도 목록 ("lat,lng" 형식)
     * @return 거리 행렬 응답
     */
    public DistanceMatrixResponseDto calculateDistanceMatrix(List<String> origins, List<String> destinations) {
        try {
            String baseUrl = "https://maps.googleapis.com/maps/api/distancematrix/json";

            // 요청 URL 생성
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl)
                    .queryParam("origins", String.join("|", origins))
                    .queryParam("destinations", String.join("|", destinations))
                    .queryParam("mode", "driving")
                    .queryParam("key", apiKey)
                    .build()
                    .toUriString();

            // API 호출
            DistanceMatrixResponseDto response = webClient.get()
                    .uri(url)
                    .retrieve()
                    .bodyToMono(DistanceMatrixResponseDto.class)
                    .block();

            return response;

        } catch (Exception e) {
            log.error("거리 행렬 계산 API 에러: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 거리 행렬 배치 계산 (API 비용 절감 및 성능 향상용)
     * @param locations 위도/경도 좌표 목록 (0번은 출발지)
     * @return 거리 행렬 (locations.size() x locations.size() 크기)
     */
    public int[][] calculateDistanceMatrixBatch(List<LatLng> locations) {
        int size = locations.size();
        int[][] distanceMatrix = new int[size][size];

        try {
            log.info("거리 행렬 배치 계산 시작: {}개 장소", size);

            // 좌표 문자열 목록 생성
            List<String> locationStrings = new ArrayList<>();
            for (LatLng location : locations) {
                locationStrings.add(location.lat + "," + location.lng);
            }

            // 구글 API 호출
            DistanceMatrixResponseDto response = calculateDistanceMatrix(locationStrings, locationStrings);

            if (response != null && "OK".equals(response.getStatus())) {
                // 응답 결과로 거리 행렬 구성
                for (int i = 0; i < size; i++) {
                    for (int j = 0; j < size; j++) {
                        if (i == j) {
                            distanceMatrix[i][j] = 0; // 동일 위치는 거리 0
                            continue;
                        }

                        try {
                            DistanceMatrixResponseDto.Element element = response.getRows().get(i).getElements().get(j);
                            if ("OK".equals(element.getStatus())) {
                                // 시간 (초) 저장
                                distanceMatrix[i][j] = element.getDuration().getValue();
                            } else {
                                // API 실패 시 직선 거리 계산
                                LatLng origin = locations.get(i);
                                LatLng dest = locations.get(j);
                                int[] result = calculateHaversineDistance(origin.lat, origin.lng, dest.lat, dest.lng);
                                distanceMatrix[i][j] = result[1]; // 시간 (초)
                            }
                        } catch (Exception e) {
                            // 예외 발생 시 직선 거리 계산
                            LatLng origin = locations.get(i);
                            LatLng dest = locations.get(j);
                            int[] result = calculateHaversineDistance(origin.lat, origin.lng, dest.lat, dest.lng);
                            distanceMatrix[i][j] = result[1]; // 시간 (초)
                        }
                    }
                }

                log.info("거리 행렬 배치 계산 완료: {}x{} 크기", size, size);
            } else {
                log.error("거리 행렬 API 응답 오류: {}", response != null ? response.getStatus() : "null response");
                // API 호출 실패 시 모든 장소 쌍에 대해 직선 거리 계산
                for (int i = 0; i < size; i++) {
                    for (int j = 0; j < size; j++) {
                        if (i == j) {
                            distanceMatrix[i][j] = 0;
                            continue;
                        }

                        LatLng origin = locations.get(i);
                        LatLng dest = locations.get(j);
                        int[] result = calculateHaversineDistance(origin.lat, origin.lng, dest.lat, dest.lng);
                        distanceMatrix[i][j] = result[1]; // 시간 (초)
                    }
                }
            }
        } catch (Exception e) {
            log.error("거리 행렬 계산 중 예외 발생: {}", e.getMessage());
            // 예외 발생 시 모든 장소 쌍에 대해 직선 거리 계산
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    if (i == j) {
                        distanceMatrix[i][j] = 0;
                        continue;
                    }

                    LatLng origin = locations.get(i);
                    LatLng dest = locations.get(j);
                    int[] result = calculateHaversineDistance(origin.lat, origin.lng, dest.lat, dest.lng);
                    distanceMatrix[i][j] = result[1]; // 시간 (초)
                }
            }
        }

        return distanceMatrix;
    }
}