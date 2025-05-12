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
    private Images images;

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
        private String character;

        @JsonProperty("profile_path")
        private String profilePath;
    }

    @Data
    public static class Images {
        private List<Image> backdrops;
        private List<Image> posters;
    }

    @Data
    public static class Image {
        @JsonProperty("file_path")
        private String filePath;

        @JsonProperty("aspect_ratio")
        private Double aspectRatio;

        private Integer height;
        private Integer width;

        @JsonProperty("vote_average")
        private Double voteAverage;

        @JsonProperty("vote_count")
        private Integer voteCount;

        @JsonProperty("iso_639_1")
        private String languageCode;
    }
}