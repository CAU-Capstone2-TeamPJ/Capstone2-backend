package com.example.capstone02.controller;

import com.example.capstone02.dto.TripPlanDto;
import com.example.capstone02.entity.TripPlan;
import com.example.capstone02.service.TripPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/trip-plans")
@RequiredArgsConstructor
@Slf4j
public class TripPlanDeleteController {

    private final TripPlanService tripPlanService;

    /**
     * 여행 계획 ID로 계획 삭제 (권한 검증)
     */
    @DeleteMapping("/{planId}")
    public ResponseEntity<?> deleteTripPlan(@PathVariable Long planId) {
        String userEmail = getCurrentUserEmail();

        if (userEmail == null) {
            return ResponseEntity.badRequest().body("인증된 사용자 정보가 필요합니다.");
        }

        try {
            // 삭제하려는 계획이 존재하는지 확인
            Optional<TripPlan> tripPlanOpt = tripPlanService.getTripPlanById(planId);
            if (tripPlanOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // 사용자 권한 검증과 함께 삭제
            tripPlanService.deleteTripPlan(planId, userEmail);

            return ResponseEntity.ok().body("여행 계획 ID " + planId + "가 성공적으로 삭제되었습니다.");
        } catch (AccessDeniedException e) {
            log.warn("사용자 {}가 여행 계획 {}에 대한 삭제 권한이 없습니다", userEmail, planId);
            return ResponseEntity.status(403).body("삭제 권한이 없습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("여행 계획 ID {}의 삭제 중 오류 발생: {}", planId, e.getMessage());
            return ResponseEntity.internalServerError().body("계획 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 영화 ID로 해당 영화의 최근 생성 일정 한 개 삭제
     */
    @DeleteMapping("/movie/{movieId}/latest")
    public ResponseEntity<?> deleteLatestTripPlanByMovieId(@PathVariable Long movieId) {
        String userEmail = getCurrentUserEmail();

        if (userEmail == null) {
            return ResponseEntity.badRequest().body("인증된 사용자 정보가 필요합니다.");
        }

        try {
            boolean deleted = tripPlanService.deleteLatestTripPlanByMovieId(movieId, userEmail);

            if (deleted) {
                return ResponseEntity.ok().body("영화 ID " + movieId + "의 최근 생성 일정이 삭제되었습니다.");
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (AccessDeniedException e) {
            log.warn("사용자 {}가 영화 {}의 최근 일정 삭제 권한이 없습니다", userEmail, movieId);
            return ResponseEntity.status(403).body("삭제 권한이 없습니다: " + e.getMessage());
        } catch (Exception e) {
            log.error("영화 ID {}의 일정 삭제 중 오류 발생: {}", movieId, e.getMessage());
            return ResponseEntity.internalServerError().body("일정 삭제 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 현재 인증된 사용자의 이메일 가져오기
     */
    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            log.warn("인증된 사용자 정보가 없습니다");
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