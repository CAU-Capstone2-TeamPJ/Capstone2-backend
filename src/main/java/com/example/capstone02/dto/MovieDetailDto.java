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
    private String director;
    private String directorProfilePath;
    private Long directorId;
    private List<GenreDto> genres = new ArrayList<>();
    private List<CastDto> cast = new ArrayList<>();
    private List<ImageDto> images = new ArrayList<>();

    // 좋아요 관련 필드 추가
    private Integer likesCount;
    private Boolean isLiked;

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
        private String character;
        private String profilePath;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ImageDto {
        private String filePath;
        private Double aspectRatio;
        private Integer height;
        private Integer width;
        private String type;  // "BACKDROP" 또는 "POSTER"
    }

    // Entity -> DTO 변환
    public static MovieDetailDto fromEntity(Movie movie) {
        List<GenreDto> genreDtos = movie.getGenres().stream()
                .map(genre -> new GenreDto(genre.getId(), genre.getName()))
                .collect(Collectors.toList());

        List<CastDto> castDtos = movie.getCast().stream()
                .map(cast -> new CastDto(cast.getId(), cast.getName(), cast.getCharacter(), cast.getProfilePath()))
                .collect(Collectors.toList());

        // 이미지 변환 (최대 10개의 배경 이미지만 포함)
        List<ImageDto> imageDtos = movie.getImages().stream()
                .filter(image -> image.getType() == Movie.MovieImage.ImageType.BACKDROP)
                .limit(10)
                .map(image -> new ImageDto(
                        image.getFilePath(),
                        image.getAspectRatio(),
                        image.getHeight(),
                        image.getWidth(),
                        image.getType().name()
                ))
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
                .director(movie.getDirector())
                .directorProfilePath(movie.getDirectorProfilePath())
                .directorId(movie.getDirectorId())
                .genres(genreDtos)
                .cast(castDtos)
                .images(imageDtos)
                .likesCount(0)  // 기본값 설정
                .isLiked(false) // 기본값 설정
                .build();
    }
}