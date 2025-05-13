package com.example.capstone02.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 여행 경로 계획 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripPlanRequestDto {
    private Long movieId;              // 영화 ID
    private String country;            // 국가 (필터링용)
    private List<String> concepts;     // 컨셉 목록 (필터링용) - 중복 선택 가능하도록 변경
    private Integer travelHours;       // 하루 이동 가능 시간 (시간 단위)
    private Double originLat;          // 출발지 위도
    private Double originLng;          // 출발지 경도

    // 하위 호환성을 위한 메소드 - 기존 코드의 getConcept() 호출을 지원
    public String getConcept() {
        if (concepts != null && !concepts.isEmpty()) {
            return concepts.get(0);
        }
        return null;
    }

    // 하위 호환성을 위한 메소드 - 기존 코드의 setConcept() 호출을 지원
    public void setConcept(String concept) {
        if (concepts == null) {
            concepts = new ArrayList<>();
        }
        if (!concepts.contains(concept) && concept != null) {
            concepts.add(concept);
        }
    }
}