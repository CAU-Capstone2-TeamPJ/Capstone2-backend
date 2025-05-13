package com.example.capstone02.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilmingLocationResponseDto {
    private Long movieId;
    private List<LocationInfo> locations;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationInfo {
        private String name;           // 촬영지 이름
        private String country;        // 국가
        private String description;    // 설명
        private String address;        // 도로명 주소
        private Double durationTime;   // 평균 체류 시간 (시간 단위)
        private Double mentionRate;    // 언급율
        private Integer mentionCount;  // 언급된 수
        private List<String> recommendationKeywords; // 추천 키워드 리스트
        private List<String> nearbyKeywords;         // 주변 키워드 리스트
    }
}