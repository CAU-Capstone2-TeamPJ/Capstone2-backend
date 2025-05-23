package com.example.capstone02.dto;

import com.example.capstone02.entity.TripDay;
import com.example.capstone02.entity.TripLocation;
import com.example.capstone02.entity.TripPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripPlanDto {
    private Long id;
    private String name;
    private Long movieId;
    private String movieTitle;
    private String country;
    private String concept;
    private Integer travelHours;
    private Integer totalDays;
    private Integer totalLocations;
    private Integer totalTravelTimeMinutes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TripDayDto> tripDays;

    // User 정보는 선택적으로 포함
    private UserDto user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripDayDto {
        private Long id;
        private Integer day;
        private Integer travelTimeMinutes;
        private List<TripLocationDto> locations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripLocationDto {
        private Long id;
        private Long locationId;
        private String locationName;
        private String address;
        private Double latitude;
        private Double longitude;
        private Integer visitOrder;
        private Integer travelTimeToNext;
        private Integer travelDistanceToNext;
        private String concept;
        private List<String> recommendationKeywords;
        private List<String> nearbyKeywords; // 주변 키워드 추가
        private Map<String, String> nearbyPlaceIds; // 주변 장소 ID 맵 추가
        private List<String> images; // 장소 이미지 URL 목록
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private Long id;
        private String name;
        private String email;
        private String picture;
    }

    // Entity를 DTO로 변환 (사용자 정보 포함)
    public static TripPlanDto fromEntity(TripPlan tripPlan) {
        // User 정보가 있을 경우 DTO로 변환
        UserDto userDto = null;
        if (tripPlan.getUser() != null) {
            userDto = UserDto.builder()
                    .id(tripPlan.getUser().getId())
                    .name(tripPlan.getUser().getName())
                    .email(tripPlan.getUser().getEmail())
                    .picture(tripPlan.getUser().getPicture())
                    .build();
        }

        // TripDay 정보 변환
        List<TripDayDto> tripDayDtos = new ArrayList<>();
        if (tripPlan.getTripDays() != null) {
            tripDayDtos = tripPlan.getTripDays().stream()
                    .map(TripPlanDto::convertTripDayToDto)
                    .collect(Collectors.toList());
        }

        return TripPlanDto.builder()
                .id(tripPlan.getId())
                .name(tripPlan.getName())
                .movieId(tripPlan.getMovieId())
                .movieTitle(tripPlan.getMovieTitle())
                .country(tripPlan.getCountry())
                .concept(tripPlan.getConcept())
                .travelHours(tripPlan.getTravelHours())
                .totalDays(tripPlan.getTotalDays())
                .totalLocations(tripPlan.getTotalLocations())
                .totalTravelTimeMinutes(tripPlan.getTotalTravelTimeMinutes())
                .createdAt(tripPlan.getCreatedAt())
                .updatedAt(tripPlan.getUpdatedAt())
                .tripDays(tripDayDtos)
                .user(userDto)
                .build();
    }

    // Entity를 DTO로 변환 (사용자 정보 제외)
    public static TripPlanDto fromEntityWithoutUser(TripPlan tripPlan) {
        TripPlanDto dto = fromEntity(tripPlan);
        dto.setUser(null);
        return dto;
    }

    // TripDay 엔티티를 DTO로 변환
    private static TripDayDto convertTripDayToDto(TripDay tripDay) {
        List<TripLocationDto> locationDtos = new ArrayList<>();
        if (tripDay.getLocations() != null) {
            locationDtos = tripDay.getLocations().stream()
                    .map(TripPlanDto::convertTripLocationToDto)
                    .collect(Collectors.toList());
        }

        return TripDayDto.builder()
                .id(tripDay.getId())
                .day(tripDay.getDay())
                .travelTimeMinutes(tripDay.getTravelTimeMinutes())
                .locations(locationDtos)
                .build();
    }

    // TripLocation 엔티티를 DTO로 변환
    private static TripLocationDto convertTripLocationToDto(TripLocation tripLocation) {
        return TripLocationDto.builder()
                .id(tripLocation.getId())
                .locationId(tripLocation.getLocationId())
                .locationName(tripLocation.getLocationName())
                .address(tripLocation.getAddress())
                .latitude(tripLocation.getLatitude())
                .longitude(tripLocation.getLongitude())
                .visitOrder(tripLocation.getVisitOrder())
                .travelTimeToNext(tripLocation.getTravelTimeToNext())
                .travelDistanceToNext(tripLocation.getTravelDistanceToNext())
                .concept(tripLocation.getConcept())
                .recommendationKeywords(tripLocation.getRecommendationKeywords())
                .nearbyKeywords(new ArrayList<>()) // 빈 리스트로 초기화 (서비스에서 설정)
                .nearbyPlaceIds(null) // 기본값 null (서비스에서 설정)
                .images(new ArrayList<>()) // 기본적으로 빈 리스트로 초기화
                .build();
    }
}