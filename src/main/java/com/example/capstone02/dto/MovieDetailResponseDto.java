package com.example.capstone02.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class MovieDetailResponseDto {
    private Long id;
    private String title;

    @JsonProperty("original_title")
    private String originalTitle;

    private String overview;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("backdrop_path")
    private String backdropPath;

    @JsonProperty("release_date")
    private String releaseDate;

    @JsonProperty("vote_average")
    private Double voteAverage;

    @JsonProperty("vote_count")
    private Integer voteCount;

    private List<Genre> genres;
    private Credits credits;

    @Data
    public static class Genre {
        private Integer id;
        private String name;
    }

    @Data
    public static class Credits {
        private List<Cast> cast;
    }

    @Data
    public static class Cast {
        private Long id;
        private String name;

        @JsonProperty("character")
        private String character; // JSON으로부터 매핑되는 필드명은 유지

        @JsonProperty("profile_path")
        private String profilePath;
    }
}