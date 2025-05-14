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
@Table(name = "trip_plans")
public class TripPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;                 // 여행 계획 이름
    private Long movieId;                // 관련 영화 ID
    private String movieTitle;           // 영화 제목
    private String country;              // 국가
    private String concept;              // 컨셉
    private Integer travelHours;         // 하루 이동 가능 시간 (시간 단위)
    private Integer totalDays;           // 총 여행 일수
    private Integer totalLocations;      // 총 방문 장소 수
    private Integer totalTravelTimeMinutes; // 총 이동 시간 (분 단위)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;                   // 사용자 (추가된 관계)

    @OneToMany(mappedBy = "tripPlan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TripDay> tripDays = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // 컬렉션 초기화 메소드 추가
    public void addTripDay(TripDay tripDay) {
        if (this.tripDays == null) {
            this.tripDays = new ArrayList<>();
        }
        tripDays.add(tripDay);
        tripDay.setTripPlan(this);
    }
}