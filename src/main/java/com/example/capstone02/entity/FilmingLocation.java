package com.example.capstone02.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}