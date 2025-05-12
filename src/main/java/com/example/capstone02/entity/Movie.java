package com.example.capstone02.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "movies")
public class Movie {
    @Id
    private Long id; // TMDB의 영화 ID를 그대로 사용

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String overview;

    private String posterPath;
    private String backdropPath;
    private LocalDate releaseDate;
    private Double voteAverage;
    private Integer voteCount;
    private String originalTitle;

    // 감독 정보 추가
    private String director;
    private String directorProfilePath;
    private Long directorId;

    @ElementCollection
    @CollectionTable(name = "movie_genres", joinColumns = @JoinColumn(name = "movie_id"))
    private List<Genre> genres = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "movie_cast", joinColumns = @JoinColumn(name = "movie_id"))
    private List<Cast> cast = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "movie_images", joinColumns = @JoinColumn(name = "movie_id"))
    private List<MovieImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FilmingLocation> filmingLocations = new ArrayList<>();

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Genre {
        private Integer id;
        private String name;
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Cast {
        private Long id;
        private String name;

        @Column(name = "character_name") // MySQL 예약어 회피
        private String character;

        private String profilePath;
    }

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MovieImage {
        private String filePath;
        private Double aspectRatio;
        private Integer height;
        private Integer width;
        private Double voteAverage;
        private Integer voteCount;

        @Enumerated(EnumType.STRING)
        private ImageType type;

        public enum ImageType {
            BACKDROP, POSTER
        }
    }
}