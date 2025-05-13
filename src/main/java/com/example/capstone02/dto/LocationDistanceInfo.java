package com.example.capstone02.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 장소 간 거리 정보 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationDistanceInfo {
    private Long fromLocationId;    // 출발 장소 ID
    private Long toLocationId;      // 도착 장소 ID
    private String fromLocationName; // 출발 장소 이름
    private String toLocationName;   // 도착 장소 이름
    private Integer travelTimeMinutes; // 이동 시간 (분 단위)
    private Integer distanceMeters;    // 이동 거리 (미터 단위)

    // 두 장소가 모두 정보를 가지고 있는지 확인
    public boolean isComplete() {
        return fromLocationId != null && toLocationId != null &&
                travelTimeMinutes != null && distanceMeters != null;
    }
}