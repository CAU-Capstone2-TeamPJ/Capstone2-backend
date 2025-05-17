package com.example.capstone02.controller;

import com.example.capstone02.dto.MovieDetailDto;
import com.example.capstone02.service.MovieDetailService;
import com.example.capstone02.service.MovieLikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/movies/ranking")
@RequiredArgsConstructor
@Slf4j
public class MovieRankingController {

    private final MovieLikeService movieLikeService;

    /**
     * 좋아요 수 기준으로 인기 영화 목록 조회
     */
    @GetMapping("/popular")
    public ResponseEntity<List<MovieDetailDto>> getPopularMovies(
            @RequestParam(value = "limit", defaultValue = "10") int limit) {

        log.info("인기 영화 목록 조회 요청: limit={}", limit);

        // 좋아요 순으로 정렬된 영화 목록 가져오기
        List<MovieDetailDto> popularMovies = movieLikeService.getMoviesByLikesCount();

        // 요청된 수량만큼 잘라서 반환
        int resultSize = Math.min(limit, popularMovies.size());
        List<MovieDetailDto> result = popularMovies.subList(0, resultSize);

        return ResponseEntity.ok(result);
    }
}