package com.example.capstone02.controller;

import com.example.capstone02.dto.MovieSearchResponseDto;
import com.example.capstone02.service.MovieSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieSearchController {
    private final MovieSearchService movieSearchService;

    @GetMapping("/search")
    public Mono<MovieSearchResponseDto> searchMovies(
            @RequestParam String query,
            @RequestParam(defaultValue = "1") int page) {
        return movieSearchService.searchMoviesByTitle(query, page);
    }
}