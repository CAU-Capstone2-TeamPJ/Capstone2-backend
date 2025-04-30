package com.example.capstone02.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "filming_locations")
@Getter
@Setter
@NoArgsConstructor
public class FilmingLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String address;

    private double latitude;

    private double longitude;

    // 영화와의 관계 설정
    @ManyToOne
    @JoinColumn(name = "movie_id")
    private Movie movie;

    // 생성자, 기타 메서드 등 필요한 코드 추가
}