package com.example.capstone02.util;

import com.google.maps.DistanceMatrixApi;
import com.google.maps.GeoApiContext;
import com.google.maps.model.DistanceMatrix;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GoogleDistanceMatrixService {

    private final GeoApiContext geoApiContext;

    public GoogleDistanceMatrixService(@Value("${google.maps.api-key}") String apiKey) {
        this.geoApiContext = new GeoApiContext.Builder()
                .apiKey(apiKey)
                .build();
    }

    /**
     * 두 위치 간의 거리 계산
     * @param origin 출발지 좌표 (위도, 경도)
     * @param destination 도착지 좌표 (위도, 경도)
     * @param travelMode 이동 수단
     * @return 거리(미터)
     */
    public long calculateDistance(LatLng origin, LatLng destination, TravelMode travelMode) throws Exception {
        DistanceMatrix distanceMatrix = DistanceMatrixApi.newRequest(geoApiContext)
                .origins(origin)
                .destinations(destination)
                .mode(travelMode)
                .await();

        // 결과 반환
        if (distanceMatrix.rows.length > 0 && distanceMatrix.rows[0].elements.length > 0) {
            return distanceMatrix.rows[0].elements[0].distance.inMeters;
        }

        throw new RuntimeException("거리 계산에 실패했습니다.");
    }

    /**
     * 여러 위치 간의 거리 행렬 계산
     * @param locations 위치 목록
     * @param travelMode 이동 수단
     * @return 거리 행렬 (단위: 미터)
     */
    public long[][] calculateDistanceMatrix(List<LatLng> locations, TravelMode travelMode) throws Exception {
        LatLng[] locationsArray = locations.toArray(new LatLng[0]);

        DistanceMatrix distanceMatrix = DistanceMatrixApi.newRequest(geoApiContext)
                .origins(locationsArray)
                .destinations(locationsArray)
                .mode(travelMode)
                .await();

        int size = locations.size();
        long[][] result = new long[size][size];

        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i == j) {
                    result[i][j] = 0; // 같은 위치는 거리 0
                } else {
                    result[i][j] = distanceMatrix.rows[i].elements[j].distance.inMeters;
                }
            }
        }

        return result;
    }

    // 자원 해제
    public void shutdown() {
        geoApiContext.shutdown();
    }
}