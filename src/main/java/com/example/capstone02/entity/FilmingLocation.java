package com.example.capstone02.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "filming_locations")
public class FilmingLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    @Column(nullable = false)
    private String name;        // 촬영지 이름

    private String country;     // 국가

    @Column(columnDefinition = "TEXT")
    private String description; // 설명

    private String address;     // 도로명 주소

    // 위도, 경도 추가
    private Double latitude;    // 위도
    private Double longitude;   // 경도

    private Double durationTime; // 평균 체류 시간 (시간 단위)

    private Double mentionRate; // 언급율
    private Integer mentionCount; // 언급된 수

    @ElementCollection
    @CollectionTable(name = "location_recommendation_keywords", joinColumns = @JoinColumn(name = "location_id"))
    @Column(name = "keyword")
    private List<String> recommendationKeywords = new ArrayList<>(); // 추천 키워드 리스트

    @ElementCollection
    @CollectionTable(name = "location_nearby_keywords", joinColumns = @JoinColumn(name = "location_id"))
    @Column(name = "keyword")
    private List<String> nearbyKeywords = new ArrayList<>(); // 주변 키워드 리스트

    // 구글 맵 이미지 URL 추가
    @ElementCollection
    @CollectionTable(name = "location_images", joinColumns = @JoinColumn(name = "location_id"))
    @Column(name = "image_url", columnDefinition = "TEXT")
    private List<String> images = new ArrayList<>(); // 구글맵 이미지 URL 리스트

    // 주변 키워드에 대응하는 구글 장소 ID 맵핑
    @ElementCollection
    @CollectionTable(name = "location_nearby_places", joinColumns = @JoinColumn(name = "location_id"))
    @MapKeyColumn(name = "keyword")
    @Column(name = "place_ids", columnDefinition = "TEXT")
    private Map<String, String> nearbyPlaceIds = new HashMap<>(); // 키워드 별 구글 장소 ID (콤마로 구분해서 저장)

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}