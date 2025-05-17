package com.example.capstone02.controller;

import com.example.capstone02.dto.LocationReviewDto;
import com.example.capstone02.dto.LocationReviewRequestDto;
import com.example.capstone02.service.LocationReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/filming-locations")
@RequiredArgsConstructor
@Slf4j
public class LocationReviewController {

    private final LocationReviewService locationReviewService;

    /**
     * 촬영지 리뷰 등록
     */
    @PostMapping("/{locationId}/reviews")
    public ResponseEntity<LocationReviewDto> createReview(
            @PathVariable Long locationId,
            @Valid @RequestBody LocationReviewRequestDto requestDto) {

        String userEmail = getCurrentUserEmail();
        if (userEmail == null) {
            return ResponseEntity.badRequest().build();
        }

        LocationReviewDto review = locationReviewService.createReview(locationId, userEmail, requestDto);
        return ResponseEntity.ok(review);
    }

    /**
     * 촬영지 리뷰 수정
     */
    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<LocationReviewDto> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody LocationReviewRequestDto requestDto) {

        String userEmail = getCurrentUserEmail();
        if (userEmail == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            LocationReviewDto updatedReview = locationReviewService.updateReview(reviewId, userEmail, requestDto);
            return ResponseEntity.ok(updatedReview);
        } catch (RuntimeException e) {
            log.error("리뷰 수정 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 촬영지 리뷰 삭제
     */
    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        String userEmail = getCurrentUserEmail();
        if (userEmail == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            locationReviewService.deleteReview(reviewId, userEmail);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            log.error("리뷰 삭제 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 촬영지 리뷰 목록 조회
     */
    @GetMapping("/{locationId}/reviews")
    public ResponseEntity<List<LocationReviewDto>> getReviewsByLocationId(@PathVariable Long locationId) {
        List<LocationReviewDto> reviews = locationReviewService.getReviewsByLocationId(locationId);
        return ResponseEntity.ok(reviews);
    }

//    /**
//     * 촬영지 리뷰 목록 페이징 조회
//     */
//    @GetMapping("/{locationId}/reviews/page")
//    public ResponseEntity<Page<LocationReviewDto>> getReviewsByLocationIdPaged(
//            @PathVariable Long locationId,
//            @PageableDefault(size = 10) Pageable pageable) {
//
//        Page<LocationReviewDto> reviewPage = locationReviewService.getReviewsByLocationId(locationId, pageable);
//        return ResponseEntity.ok(reviewPage);
//    }

    /**
     * 내가 작성한 리뷰 목록 조회
     */
    @GetMapping("/my-reviews")
    public ResponseEntity<List<LocationReviewDto>> getMyReviews() {
        String userEmail = getCurrentUserEmail();
        if (userEmail == null) {
            return ResponseEntity.badRequest().build();
        }

        List<LocationReviewDto> reviews = locationReviewService.getReviewsByUserEmail(userEmail);
        return ResponseEntity.ok(reviews);
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