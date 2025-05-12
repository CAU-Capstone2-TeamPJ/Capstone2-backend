package com.example.capstone02.controller;

import com.example.capstone02.dto.MovieDetailDto;
import com.example.capstone02.service.MovieDetailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@Slf4j
public class MovieDetailController {

    private final MovieDetailService movieDetailService;

    /**
     * 영화 상세 정보를 조회하는 엔드포인트.
     * 데이터베이스에 없는 경우 TMDB API에서 자동으로 가져와 저장 후 반환
     */
    @GetMapping("/{movieId}")
    public ResponseEntity<MovieDetailDto> getMovieDetail(@PathVariable Long movieId) {
        try {
            // 데이터베이스에서 영화 정보 조회 시도
            MovieDetailDto movieDetail = movieDetailService.getMovieDetail(movieId);
            return ResponseEntity.ok(movieDetail);
        } catch (RuntimeException e) {
            // 데이터베이스에 정보가 없는 경우 TMDB API에서 가져오기
            log.info("영화 ID {}에 대한 정보가 없어 TMDB API에서 가져옵니다", movieId);
            MovieDetailDto fetchedMovieDetail = movieDetailService.fetchAndSaveMovieDetail(movieId).block();
            return ResponseEntity.ok(fetchedMovieDetail);
        }
    }

    /**
     * 강제로 TMDB API에서 영화 정보를 다시 가져오는 엔드포인트.
     * 데이터 업데이트나 개발/테스트 시 사용
     */
    @GetMapping("/fetch/{movieId}")
    public ResponseEntity<MovieDetailDto> fetchAndSaveMovieDetail(@PathVariable Long movieId) {
        MovieDetailDto movieDetail = movieDetailService.fetchAndSaveMovieDetail(movieId).block();
        return ResponseEntity.ok(movieDetail);
    }
}