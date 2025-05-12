package com.example.capstone02.dto;

import com.example.capstone02.entity.Movie;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieDetailDto {
    private Long id;
    private String title;
    private String originalTitle;
    private String overview;
    private String posterPath;
    private String backdropPath;
    private LocalDate releaseDate;
    private Double voteAverage;
    private Integer voteCount;
    private List<GenreDto> genres = new ArrayList<>();
    private List<CastDto> cast = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenreDto {
        private Integer id;
        private String name;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CastDto {
        private Long id;
        private String name;
        private String character; // 이름은 유지 (DTO에서는 문제 없음)
        private String profilePath;
    }

    // Entity -> DTO 변환
    public static MovieDetailDto fromEntity(Movie movie) {
        List<GenreDto> genreDtos = movie.getGenres().stream()
                .map(genre -> new GenreDto(genre.getId(), genre.getName()))
                .collect(Collectors.toList());

        List<CastDto> castDtos = movie.getCast().stream()
                .map(cast -> new CastDto(cast.getId(), cast.getName(), cast.getCharacter(), cast.getProfilePath()))
                .collect(Collectors.toList());

        return MovieDetailDto.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .originalTitle(movie.getOriginalTitle())
                .overview(movie.getOverview())
                .posterPath(movie.getPosterPath())
                .backdropPath(movie.getBackdropPath())
                .releaseDate(movie.getReleaseDate())
                .voteAverage(movie.getVoteAverage())
                .voteCount(movie.getVoteCount())
                .genres(genreDtos)
                .cast(castDtos)
                .build();
    }
}