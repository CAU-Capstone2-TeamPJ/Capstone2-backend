package com.example.capstone02.service;

import com.example.capstone02.dto.MovieDetailDto;
import com.example.capstone02.dto.MovieDetailResponseDto;
import com.example.capstone02.entity.Movie;
import com.example.capstone02.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MovieDetailService {
    private final WebClient webClient;
    private final MovieRepository movieRepository;

    @Value("${tmdb.api-key}")
    private String apiKey;

    @Value("${tmdb.base-url}")
    private String baseUrl;

    @Transactional
    public Mono<MovieDetailDto> fetchAndSaveMovieDetail(Long movieId) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/movie/" + movieId)
                .queryParam("api_key", apiKey)
                .queryParam("append_to_response", "credits")
                .queryParam("language", "ko-KR")
                .build()
                .toUriString();

        return webClient.get()
                .uri(url)
                .header("accept", "application/json")
                .retrieve()
                .bodyToMono(MovieDetailResponseDto.class)
                .map(this::convertToEntity)
                .map(movie -> movieRepository.save(movie))
                .map(MovieDetailDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public MovieDetailDto getMovieDetail(Long movieId) {
        return movieRepository.findById(movieId)
                .map(MovieDetailDto::fromEntity)
                .orElseThrow(() -> new RuntimeException("영화를 찾을 수 없습니다: " + movieId));
    }

    private Movie convertToEntity(MovieDetailResponseDto dto) {
        // 날짜 변환
        LocalDate releaseDate = null;
        if (dto.getReleaseDate() != null && !dto.getReleaseDate().isEmpty()) {
            try {
                releaseDate = LocalDate.parse(dto.getReleaseDate());
            } catch (Exception e) {
                // 날짜 파싱 오류 처리
                releaseDate = null;
            }
        }

        // 장르 변환
        List<Movie.Genre> genres = dto.getGenres() != null ?
                dto.getGenres().stream()
                        .map(genre -> new Movie.Genre(genre.getId(), genre.getName()))
                        .collect(Collectors.toList()) :
                new ArrayList<>();

        // 캐스트 변환
        List<Movie.Cast> cast = new ArrayList<>();
        if (dto.getCredits() != null && dto.getCredits().getCast() != null) {
            cast = dto.getCredits().getCast().stream()
                    .map(c -> new Movie.Cast(c.getId(), c.getName(), c.getCharacter(), c.getProfilePath()))
                    .collect(Collectors.toList());
        }

        return Movie.builder()
                .id(dto.getId())
                .title(dto.getTitle())
                .originalTitle(dto.getOriginalTitle())
                .overview(dto.getOverview())
                .posterPath(dto.getPosterPath())
                .backdropPath(dto.getBackdropPath())
                .releaseDate(releaseDate)
                .voteAverage(dto.getVoteAverage())
                .voteCount(dto.getVoteCount())
                .genres(genres)
                .cast(cast)
                .build();
    }
}