package com.example.capstone02.service;

import com.example.capstone02.dto.MovieSearchResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class MovieSearchService {
    private final WebClient webClient;

    @Value("${tmdb.api-key}")
    private String apiKey;

    @Value("${tmdb.base-url}")
    private String baseUrl;

    public Mono<MovieSearchResponseDto> searchMoviesByTitle(String query, int page) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/search/movie")
                .queryParam("api_key", apiKey)
                .queryParam("query", query)
                .queryParam("language", "ko-KR")
                .queryParam("page", page)
                .build()
                .toUriString();

        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(MovieSearchResponseDto.class);
    }
}