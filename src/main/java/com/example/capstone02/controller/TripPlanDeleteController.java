//package com.example.capstone02.controller;
//
//import com.example.capstone02.service.TripPlanService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/trip-plans")
//@RequiredArgsConstructor
//@Slf4j
//public class TripPlanDeleteController {
//
//    private final TripPlanService tripPlanService;
//
//    /**
//     * 영화 ID로 해당 영화의 최근 생성 일정 한 개 삭제
//     */
//    @DeleteMapping("/movie/{movieId}/latest")
//    public ResponseEntity<?> deleteLatestTripPlanByMovieId(@PathVariable Long movieId) {
//        String userEmail = getCurrentUserEmail();
//
//        if (userEmail == null) {
//            return ResponseEntity.badRequest().body("인증된 사용자 정보가 필요합니다.");
//        }
//
//        try {
//            boolean deleted = tripPlanService.deleteLatestTripPlanByMovieId(movieId, userEmail);
//
//            if (deleted) {
//                return ResponseEntity.ok().body("영화 ID " + movieId + "의 최근 생성 일정이 삭제되었습니다.");
//            } else {
//                return ResponseEntity.notFound().build();
//            }
//        } catch (Exception e) {
//            log.error("영화 ID {}의 일정 삭제 중 오류 발생: {}", movieId, e.getMessage());
//            return ResponseEntity.internalServerError().body("일정 삭제 중 오류가 발생했습니다: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 현재 인증된 사용자의 이메일 가져오기
//     */
//    private String getCurrentUserEmail() {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        if (authentication == null || !authentication.isAuthenticated() ||
//                "anonymousUser".equals(authentication.getPrincipal())) {
//            log.warn("인증된 사용자 정보가 없습니다");
//            return null;
//        }
//
//        Object principal = authentication.getPrincipal();
//
//        // JWT 토큰 인증의 경우 principal이 이메일 문자열
//        if (principal instanceof String) {
//            return (String) principal;
//        }
//        // Spring Security UserDetails 사용 시
//        else if (principal instanceof org.springframework.security.core.userdetails.UserDetails) {
//            return ((org.springframework.security.core.userdetails.UserDetails) principal).getUsername();
//        }
//
//        return null;
//    }
//}