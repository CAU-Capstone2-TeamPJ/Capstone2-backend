package com.example.capstone02.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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

        @JsonProperty("durationTime")
        private Double durationTime;   // 평균 체류 시간 (시간 단위)

        @JsonProperty("mentionRate")
        private Double mentionRate;    // 언급율

        @JsonProperty("mentionCount")
        private Integer mentionCount;  // 언급된 수

        @JsonProperty("recommendKeywords")  // 파이썬 서버에서 반환하는 필드명
        private List<String> recommendationKeywords; // 스프링에서 사용하는 필드명

        @JsonProperty("nearbyKeywords")
        private List<String> nearbyKeywords;         // 주변 키워드 리스트
    }
}