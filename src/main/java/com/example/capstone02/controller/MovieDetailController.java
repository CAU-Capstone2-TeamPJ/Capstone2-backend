package com.example.capstone02.controller;

import com.example.capstone02.dto.MovieDetailDto;
import com.example.capstone02.dto.MovieLikeDto;
import com.example.capstone02.service.MovieDetailService;
import com.example.capstone02.service.MovieLikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@Slf4j
public class MovieDetailController {

    private final MovieDetailService movieDetailService;
    private final MovieLikeService movieLikeService;

    /**
     * 영화 상세 정보를 조회하는 엔드포인트.
     * 데이터베이스에 없는 경우 TMDB API에서 자동으로 가져와 저장 후 반환
     * 촬영지 국가 정보도 함께 반환
     */
    @GetMapping("/{movieId}")
    public ResponseEntity<?> getMovieDetail(@PathVariable Long movieId) {
        try {
            // 데이터베이스에서 영화 정보 조회 시도 (촬영지 국가 정보 포함)
            MovieDetailDto movieDetail = movieDetailService.getMovieDetail(movieId);

            // 현재 사용자의 좋아요 상태 및 전체 좋아요 수 조회
            String userEmail = getCurrentUserEmail();
            MovieLikeDto likeInfo = movieLikeService.getLikeStatus(movieId, userEmail);

            // 좋아요 정보 추가
            movieDetail.setLikesCount(likeInfo.getLikesCount());
            movieDetail.setIsLiked(likeInfo.getIsLiked());

            // 로그에 촬영지 국가 정보 출력
            Set<String> filmingCountries = movieDetail.getFilmingCountries();
            if (filmingCountries != null && !filmingCountries.isEmpty()) {
                log.info("영화 ID {} ({})의 촬영지 국가: {}",
                        movieId, movieDetail.getTitle(), String.join(", ", filmingCountries));
            } else {
                log.info("영화 ID {} ({})의 촬영지 국가 정보가 없습니다",
                        movieId, movieDetail.getTitle());
            }

            return ResponseEntity.ok(movieDetail);
        } catch (RuntimeException e) {
            // 데이터베이스에 정보가 없는 경우
            log.info("영화 ID {}에 대한 정보가 없어 TMDB API에서 가져옵니다", movieId);

            try {
                // 비동기 요청 상태 반환
                Map<String, Object> status = movieDetailService.requestMovieDetailAsync(movieId);
                return ResponseEntity.accepted().body(status);
            } catch (Exception fetchEx) {
                log.error("영화 정보 요청 중 오류 발생: {}", fetchEx.getMessage(), fetchEx);
                return ResponseEntity.internalServerError().body(Map.of(
                        "error", "영화 정보를 가져오는 중 오류가 발생했습니다",
                        "message", fetchEx.getMessage()
                ));
            }
        }
    }

    /**
     * 영화 정보 처리 상태 확인
     */
    @GetMapping("/{movieId}/status")
    public ResponseEntity<Map<String, Object>> checkMovieDetailStatus(@PathVariable Long movieId) {
        boolean isComplete = movieDetailService.isProcessingComplete(movieId);

        try {
            // 데이터가 있는지 확인 시도
            movieDetailService.getMovieDetail(movieId);
            return ResponseEntity.ok(Map.of(
                    "movieId", movieId,
                    "hasData", true,
                    "processingComplete", true,
                    "message", "데이터가 준비되었습니다."
            ));
        } catch (RuntimeException e) {
            // 데이터가 없는 경우 처리 상태 확인
            return ResponseEntity.ok(Map.of(
                    "movieId", movieId,
                    "hasData", false,
                    "processingComplete", isComplete,
                    "message", isComplete ?
                            "데이터가 없습니다. 요청이 필요합니다." :
                            "데이터 처리가 진행 중입니다."
            ));
        }
    }

    /**
     * 강제로 TMDB API에서 영화 정보를 다시 가져오는 엔드포인트.
     * 데이터 업데이트나 개발/테스트 시 사용
     */
    @GetMapping("/fetch/{movieId}")
    public ResponseEntity<?> fetchAndSaveMovieDetail(@PathVariable Long movieId) {
        try {
            // 비동기 요청 시작
            Map<String, Object> status = movieDetailService.requestMovieDetailAsync(movieId);
            return ResponseEntity.accepted().body(status);
        } catch (Exception e) {
            log.error("영화 정보 강제 업데이트 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "영화 정보를 가져오는 중 오류가 발생했습니다",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 영화 ID 여러 개를 받아 비동기로 정보를 가져오는 엔드포인트
     */
    @PostMapping("/fetch-batch")
    public ResponseEntity<Map<String, Object>> fetchMovieDetailsBatch(@RequestBody List<Long> movieIds) {
        if (movieIds == null || movieIds.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "영화 ID 목록이 비어 있습니다"
            ));
        }

        int count = 0;
        for (Long movieId : movieIds) {
            try {
                // 데이터베이스에 없는 영화만 요청
                try {
                    movieDetailService.getMovieDetail(movieId);
                    log.info("영화 ID {}는 이미 데이터베이스에 존재합니다", movieId);
                } catch (RuntimeException e) {
                    // 비동기 요청 시작
                    movieDetailService.requestMovieDetailAsync(movieId);
                    count++;
                }
            } catch (Exception e) {
                log.error("영화 ID {} 정보 요청 중 오류 발생: {}", movieId, e.getMessage());
                // 오류가 있어도 계속 진행
            }
        }

        return ResponseEntity.accepted().body(Map.of(
                "requestedCount", count,
                "totalCount", movieIds.size(),
                "message", count + "개 영화에 대한 정보 요청이 시작되었습니다. 완료까지 몇 분이 소요될 수 있습니다."
        ));
    }

    /**
     * 영화 ID로 촬영지 국가 정보만 조회하는 API 추가
     */
    @GetMapping("/{movieId}/filming-countries")
    public ResponseEntity<?> getFilmingCountries(@PathVariable Long movieId) {
        try {
            MovieDetailDto movieDetail = movieDetailService.getMovieDetail(movieId);
            Set<String> filmingCountries = movieDetail.getFilmingCountries();

            if (filmingCountries != null && !filmingCountries.isEmpty()) {
                log.info("영화 ID {} ({})의 촬영지 국가: {}",
                        movieId, movieDetail.getTitle(), String.join(", ", filmingCountries));

                return ResponseEntity.ok(Map.of(
                        "movieId", movieId,
                        "title", movieDetail.getTitle(),
                        "filmingCountries", filmingCountries
                ));
            } else {
                log.info("영화 ID {} ({})의 촬영지 국가 정보가 없습니다",
                        movieId, movieDetail.getTitle());

                return ResponseEntity.ok(Map.of(
                        "movieId", movieId,
                        "title", movieDetail.getTitle(),
                        "filmingCountries", List.of(),
                        "message", "촬영지 국가 정보가 없습니다."
                ));
            }
        } catch (RuntimeException e) {
            log.error("영화 ID {}의 촬영지 국가 정보 조회 중 오류 발생: {}", movieId, e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "영화 정보를 가져오는 중 오류가 발생했습니다",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * 현재 인증된 사용자의 이메일 가져오기
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        // JWT 토큰 인증의 경우 principal이 이메일 문자열
        if (principal instanceof String) {
            return (String) principal;
        }
        // Spring Security UserDetails 사용 시
        else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
            return ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
        }

        return null;
    }
}