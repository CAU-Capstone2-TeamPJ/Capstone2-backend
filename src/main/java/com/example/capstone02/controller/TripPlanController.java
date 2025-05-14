package com.example.capstone02.controller;

import com.example.capstone02.dto.TripPlanRequestDto;
import com.example.capstone02.entity.TripPlan;
import com.example.capstone02.service.TripPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/saved-trip-plans")
@RequiredArgsConstructor
@Slf4j
public class TripPlanController {

    private final TripPlanService tripPlanService;

    /**
     * 여행 계획 저장 (인증된 사용자만)
     */
    @PostMapping
    public ResponseEntity<TripPlan> saveTripPlan(
            @RequestBody SaveTripPlanRequestDto request,
            @AuthenticationPrincipal OAuth2User principal) {

        log.info("여행 계획 저장 요청: {}", request);

        if (request.getTripPlanRequest() == null || request.getName() == null || request.getName().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // 사용자 이메일 가져오기
        String userEmail = null;
        if (principal != null) {
            userEmail = principal.getAttribute("email");
        }

        TripPlan savedPlan = tripPlanService.createAndSaveTripPlan(
                request.getTripPlanRequest(),
                request.getName(),
                userEmail);

        return ResponseEntity.ok(savedPlan);
    }

    /**
     * 저장된 여행 계획 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<TripPlan> getTripPlan(@PathVariable Long id) {
        Optional<TripPlan> tripPlan = tripPlanService.getTripPlanById(id);
        return tripPlan.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * 영화별 여행 계획 조회
     */
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<TripPlan>> getTripPlansByMovie(@PathVariable Long movieId) {
        List<TripPlan> tripPlans = tripPlanService.getTripPlansByMovieId(movieId);
        return ResponseEntity.ok(tripPlans);
    }

    /**
     * 국가별 여행 계획 조회
     */
    @GetMapping("/country/{country}")
    public ResponseEntity<List<TripPlan>> getTripPlansByCountry(@PathVariable String country) {
        List<TripPlan> tripPlans = tripPlanService.getTripPlansByCountry(country);
        return ResponseEntity.ok(tripPlans);
    }

    /**
     * 컨셉별 여행 계획 조회
     */
    @GetMapping("/concept/{concept}")
    public ResponseEntity<List<TripPlan>> getTripPlansByConcept(@PathVariable String concept) {
        List<TripPlan> tripPlans = tripPlanService.getTripPlansByConcept(concept);
        return ResponseEntity.ok(tripPlans);
    }

    /**
     * 현재 로그인한 사용자의 여행 계획 조회
     */
    @GetMapping("/my-plans")
    public ResponseEntity<List<TripPlan>> getMyTripPlans(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.badRequest().build();
        }

        String email = principal.getAttribute("email");
        if (email == null) {
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok(principal.getAttribute("user_trip_plans"));
    }

    /**
     * 모든 여행 계획 조회
     */
    @GetMapping
    public ResponseEntity<List<TripPlan>> getAllTripPlans() {
        List<TripPlan> tripPlans = tripPlanService.getAllTripPlans();
        return ResponseEntity.ok(tripPlans);
    }

    /**
     * 여행 계획 삭제 (본인 소유만 가능)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTripPlan(
            @PathVariable Long id,
            @AuthenticationPrincipal OAuth2User principal) {

        if (principal == null) {
            return ResponseEntity.badRequest().build();
        }

        // 삭제하려는 계획 조회
        Optional<TripPlan> tripPlanOpt = tripPlanService.getTripPlanById(id);

        if (tripPlanOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TripPlan tripPlan = tripPlanOpt.get();

        // 사용자 소유 확인 (본인 소유가 아니면 삭제 불가)
        if (tripPlan.getUser() != null) {
            String userEmail = principal.getAttribute("email");

            // 사용자 이메일과 일치하지 않으면 삭제 불가
            if (userEmail == null || !userEmail.equals(tripPlan.getUser().getEmail())) {
                return ResponseEntity.status(403).build(); // 접근 권한 없음
            }
        }

        tripPlanService.deleteTripPlan(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 여행 계획 저장 요청 DTO
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SaveTripPlanRequestDto {
        private String name;  // 여행 계획 이름
        private TripPlanRequestDto tripPlanRequest;  // 여행 계획 요청 정보
    }
}