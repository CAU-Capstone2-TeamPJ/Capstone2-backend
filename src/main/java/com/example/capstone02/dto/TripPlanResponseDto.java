package com.example.capstone02.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 여행 경로 계획 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripPlanResponseDto {
    private List<DailyRouteDto> dailyRoutes;  // 일별 경로 목록
    private Integer totalDays;                // 총 일수
    private Integer totalLocations;           // 총 방문 장소 수
    private Integer totalTravelTimeMinutes;   // 총 이동 시간 (분 단위)

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyRouteDto {
        private Integer day;                        // 여행 일차
        private List<LocationRouteDto> locations;   // 방문 장소 목록 (순서대로)
        private Integer travelTimeMinutes;          // 해당 일차의 이동 시간 (분 단위)
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationRouteDto {
        private Long locationId;          // 촬영지 ID
        private String locationName;      // 촬영지 이름
        private String address;           // 주소
        private Double latitude;          // 위도
        private Double longitude;         // 경도
        private Integer visitOrder;       // 방문 순서
        private Integer travelTimeToNext; // 다음 장소까지 이동 시간 (분 단위)
        private Integer travelDistanceToNext; // 다음 장소까지 이동 거리 (미터 단위)
        private List<String> recommendationKeywords; // 추천 키워드
        private String concept;           // 관련 컨셉
        private List<String> images;      // 장소 이미지 URL 목록 (추가)
    }
}