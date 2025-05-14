package com.example.capstone02.dto;

import com.example.capstone02.entity.TripPlan;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    // Entity -> DTO 변환
    public static TripPlanDto fromEntity(TripPlan tripPlan) {
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
                .build();
    }
}