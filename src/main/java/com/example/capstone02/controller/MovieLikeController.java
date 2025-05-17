package com.example.capstone02.controller;

import com.example.capstone02.dto.MovieLikeDto;
import com.example.capstone02.dto.MovieDetailDto;
import com.example.capstone02.service.MovieDetailService;
import com.example.capstone02.service.MovieLikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
@Slf4j
public class MovieLikeController {

    private final MovieLikeService movieLikeService;
    private final MovieDetailService movieDetailService;

    /**
     * 영화 좋아요 상태 전환 (토글)
     */
    @PostMapping("/{movieId}/like")
    public ResponseEntity<MovieLikeDto> toggleLike(@PathVariable Long movieId) {
        String userEmail = getCurrentUserEmail();
        if (userEmail == null) {
            return ResponseEntity.badRequest().build();
        }

        MovieLikeDto result = movieLikeService.toggleLike(movieId, userEmail);
        return ResponseEntity.ok(result);
    }

    /**
     * 좋아요 순으로 영화 목록 조회
     */
    @GetMapping("/popular")
    public ResponseEntity<List<MovieDetailDto>> getMoviesByLikesCount() {
        String userEmail = getCurrentUserEmail();
        List<MovieDetailDto> movies = movieLikeService.getMoviesByLikesCount();

        // 사용자 로그인 시 각 영화에 대한 사용자 좋아요 상태 설정
        if (userEmail != null) {
            for (MovieDetailDto movie : movies) {
                MovieLikeDto likeInfo = movieLikeService.getLikeStatus(movie.getId(), userEmail);
                movie.setIsLiked(likeInfo.getIsLiked());
            }
        }

        return ResponseEntity.ok(movies);
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