package com.example.capstone02.controller;

import com.example.capstone02.dto.MovieDetailDto;
import com.example.capstone02.service.MovieDetailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieDetailController {
    private final MovieDetailService movieDetailService;

    /**
     * TMDB에서 영화 상세 정보를 가져와 데이터베이스에 저장
     * @param movieId TMDB 영화 ID
     * @return 저장된 영화 정보
     */
    @GetMapping("/fetch/{movieId}")
    public Mono<ResponseEntity<MovieDetailDto>> fetchAndSaveMovieDetail(@PathVariable Long movieId) {
        return movieDetailService.fetchAndSaveMovieDetail(movieId)
                .map(ResponseEntity::ok);
    }

    /**
     * 데이터베이스에서 영화 상세 정보 조회
     * @param movieId 영화 ID
     * @return 영화 정보
     */
    @GetMapping("/{movieId}")
    public ResponseEntity<MovieDetailDto> getMovieDetail(@PathVariable Long movieId) {
        return ResponseEntity.ok(movieDetailService.getMovieDetail(movieId));
    }
}