package com.example.capstone02.controller;

import com.example.capstone02.dto.TripPlanDto;
import com.example.capstone02.dto.TripPlanRequestDto;
import com.example.capstone02.entity.TripPlan;
import com.example.capstone02.service.TripPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/saved-trip-plans")
@RequiredArgsConstructor
@Slf4j
public class TripPlanController {

    private final TripPlanService tripPlanService;

    /**
     * 여행 계획 저장
     */
    @PostMapping
    public ResponseEntity<TripPlanDto> saveTripPlan(@RequestBody SaveTripPlanRequestDto request) {
        log.info("여행 계획 저장 요청: {}", request);

        if (request.getTripPlanRequest() == null || request.getName() == null || request.getName().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        // 현재 인증된 사용자 정보 가져오기
        String userEmail = getCurrentUserEmail();

        TripPlan savedPlan = tripPlanService.createAndSaveTripPlan(request.getTripPlanRequest(), request.getName(), userEmail);
        return ResponseEntity.ok(TripPlanDto.fromEntity(savedPlan));
    }

    /**
     * 저장된 여행 계획 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<TripPlanDto> getTripPlan(@PathVariable Long id) {
        Optional<TripPlan> tripPlanOpt = tripPlanService.getTripPlanById(id);
        return tripPlanOpt
                .map(tripPlan -> ResponseEntity.ok(TripPlanDto.fromEntity(tripPlan)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 영화별 여행 계획 조회
     */
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<TripPlanDto>> getTripPlansByMovie(@PathVariable Long movieId) {
        List<TripPlan> tripPlans = tripPlanService.getTripPlansByMovieId(movieId);
        List<TripPlanDto> tripPlanDtos = tripPlans.stream()
                .map(TripPlanDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tripPlanDtos);
    }

    /**
     * 국가별 여행 계획 조회
     */
    @GetMapping("/country/{country}")
    public ResponseEntity<List<TripPlanDto>> getTripPlansByCountry(@PathVariable String country) {
        List<TripPlan> tripPlans = tripPlanService.getTripPlansByCountry(country);
        List<TripPlanDto> tripPlanDtos = tripPlans.stream()
                .map(TripPlanDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tripPlanDtos);
    }

    /**
     * 컨셉별 여행 계획 조회
     */
    @GetMapping("/concept/{concept}")
    public ResponseEntity<List<TripPlanDto>> getTripPlansByConcept(@PathVariable String concept) {
        List<TripPlan> tripPlans = tripPlanService.getTripPlansByConcept(concept);
        List<TripPlanDto> tripPlanDtos = tripPlans.stream()
                .map(TripPlanDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tripPlanDtos);
    }

    /**
     * 모든 여행 계획 조회
     */
    @GetMapping
    public ResponseEntity<List<TripPlanDto>> getAllTripPlans() {
        List<TripPlan> tripPlans = tripPlanService.getAllTripPlans();
        List<TripPlanDto> tripPlanDtos = tripPlans.stream()
                .map(TripPlanDto::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(tripPlanDtos);
    }

//    /**
//     * 여행 계획 삭제 (본인 소유만 가능)
//     */
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> deleteTripPlan(@PathVariable Long id) {
//        // 현재 인증된 사용자 정보 가져오기
//        String userEmail = getCurrentUserEmail();
//
//        // 삭제하려는 계획 조회
//        Optional<TripPlan> tripPlanOpt = tripPlanService.getTripPlanById(id);
//
//        if (tripPlanOpt.isEmpty()) {
//            return ResponseEntity.notFound().build();
//        }
//
//        TripPlan tripPlan = tripPlanOpt.get();
//
//        // 사용자 소유 확인 (본인 소유가 아니면 삭제 불가)
//        if (tripPlan.getUser() != null && userEmail != null) {
//            // 사용자 이메일과 일치하지 않으면 삭제 불가
//            if (!userEmail.equals(tripPlan.getUser().getEmail())) {
//                return ResponseEntity.status(403).build(); // 접근 권한 없음
//            }
//        }
//
//        tripPlanService.deleteTripPlan(id);
//        return ResponseEntity.noContent().build();
//    }

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