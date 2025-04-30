package com.example.capstone02.dto;

import com.google.maps.model.TravelMode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LocationDistanceDTO {
    private Long originId;
    private String originName;
    private Long destinationId;
    private String destinationName;
    private long distanceInMeters;
    private TravelMode travelMode;

    // 거리를 킬로미터로 반환하는 편의 메서드
    public double getDistanceInKilometers() {
        return distanceInMeters / 1000.0;
    }
}