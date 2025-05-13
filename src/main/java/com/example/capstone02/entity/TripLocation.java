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
@Table(name = "trip_locations")
public class TripLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_day_id")
    private TripDay tripDay;

    private Long locationId;           // 촬영지 ID
    private String locationName;       // 촬영지 이름
    private String address;            // 주소
    private Double latitude;           // 위도
    private Double longitude;          // 경도
    private Integer visitOrder;        // 방문 순서
    private Integer travelTimeToNext;  // 다음 장소까지 이동 시간 (분 단위)
    private Integer travelDistanceToNext; // 다음 장소까지 이동 거리 (미터 단위)
    private String concept;            // 관련 컨셉

    @ElementCollection
    @CollectionTable(name = "trip_location_keywords", joinColumns = @JoinColumn(name = "trip_location_id"))
    @Column(name = "keyword")
    @Builder.Default // Builder 패턴에서도 기본값 사용
    private List<String> recommendationKeywords = new ArrayList<>(); // 추천 키워드

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 키워드 설정 메소드 추가 (null 체크 추가)
    public void setRecommendationKeywords(List<String> keywords) {
        if (this.recommendationKeywords == null) {
            this.recommendationKeywords = new ArrayList<>();
        } else {
            this.recommendationKeywords.clear();
        }

        if (keywords != null) {
            this.recommendationKeywords.addAll(keywords);
        }
    }
}