package com.example.capstone02.dto;

import com.example.capstone02.entity.FilmingLocation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 촬영지 상세 정보 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilmingLocationDetailDto {
    private Long id;
    private Long movieId;
    private String movieTitle;
    private String name;        // 촬영지 이름
    private String country;     // 국가
    private String city;        // 도시
    private String description; // 설명
    private String address;     // 도로명 주소
    private Double latitude;    // 위도
    private Double longitude;   // 경도
    private Double durationTime; // 평균 체류 시간 (시간 단위)
    private Double mentionRate; // 언급율
    private Integer mentionCount; // 언급된 수
    private List<String> recommendationKeywords = new ArrayList<>(); // 추천 키워드 리스트
    private List<String> nearbyKeywords = new ArrayList<>(); // 주변 키워드 리스트
    private List<String> images = new ArrayList<>(); // 구글맵 이미지 URL 리스트
    private Map<String, String> nearbyPlaceIds; // 키워드 별 구글 장소 ID

    /**
     * FilmingLocation 엔티티를 DTO로 변환
     */
    public static FilmingLocationDetailDto fromEntity(FilmingLocation location) {
        return FilmingLocationDetailDto.builder()
                .id(location.getId())
                .movieId(location.getMovie() != null ? location.getMovie().getId() : null)
                .movieTitle(location.getMovie() != null ? location.getMovie().getTitle() : null)
                .name(location.getName())
                .country(location.getCountry())
                .city(location.getCity())
                .description(location.getDescription())
                .address(location.getAddress())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .durationTime(location.getDurationTime())
                .mentionRate(location.getMentionRate())
                .mentionCount(location.getMentionCount())
                .recommendationKeywords(location.getRecommendationKeywords() != null ?
                        new ArrayList<>(location.getRecommendationKeywords()) : new ArrayList<>())
                .nearbyKeywords(location.getNearbyKeywords() != null ?
                        new ArrayList<>(location.getNearbyKeywords()) : new ArrayList<>())
                .images(location.getImages() != null ?
                        new ArrayList<>(location.getImages()) : new ArrayList<>())
                .nearbyPlaceIds(location.getNearbyPlaceIds())
                .build();
    }
}