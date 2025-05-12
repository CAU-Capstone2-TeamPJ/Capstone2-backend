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
        private String name;        // 촬영지 이름
        private String country;     // 국가
        private String description; // 설명
        private List<String> keywords; // 키워드 리스트
        private Double latitude;    // 위도
        private Double longitude;   // 경도
        private String address;     // 도로명 주소
        private Double mentionRate; // 언급율
        private Integer mentionCount; // 언급된 수
    }
}