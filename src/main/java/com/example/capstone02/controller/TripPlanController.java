package com.example.capstone02.controller;

import com.example.capstone02.dto.TripPlanDto;
import com.example.capstone02.dto.TripPlanRequestDto;
import com.example.capstone02.entity.TripPlan;
import com.example.capstone02.service.TripPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/saved-trip-plans")
@RequiredArgsConstructor
@Slf4j
public class TripPlanController {

    private final TripPlanService tripPlanService;

//    /**
//     * 여행 계획 저장
//     */
//    @PostMapping
//    public ResponseEntity<TripPlanDto> saveTripPlan(@RequestBody SaveTripPlanRequestDto request) {
//        log.info("여행 계획 저장 요청: {}", request);
//
//        if (request.getTripPlanRequest() == null || request.getName() == null || request.getName().isEmpty()) {
//            return ResponseEntity.badRequest().build();
//        }
//
//        // 현재 인증된 사용자 정보 가져오기
//        String userEmail = getCurrentUserEmail();
//
//        TripPlan savedPlan = tripPlanService.createAndSaveTripPlan(request.getTripPlanRequest(), request.getName(), userEmail);
//        return ResponseEntity.ok(TripPlanDto.fromEntity(savedPlan));
//    }

    /**
     * 저장된 여행 계획 조회 (이미지 포함)
     */
    @GetMapping("/{id}")
    public ResponseEntity<TripPlanDto> getTripPlan(@PathVariable Long id) {
        try {
            TripPlanDto tripPlanDto = tripPlanService.getTripPlanDtoWithImages(id);
            return ResponseEntity.ok(tripPlanDto);
        } catch (RuntimeException e) {
            log.error("여행 계획 조회 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 영화별 여행 계획 조회 (이미지 포함)
     */
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<TripPlanDto>> getTripPlansByMovie(@PathVariable Long movieId) {
        List<TripPlan> tripPlans = tripPlanService.getTripPlansByMovieId(movieId);
        List<TripPlanDto> tripPlanDtos = tripPlanService.getTripPlanDtosWithImages(tripPlans);
        return ResponseEntity.ok(tripPlanDtos);
    }

    /**
     * 국가별 여행 계획 조회 (이미지 포함)
     */
    @GetMapping("/country/{country}")
    public ResponseEntity<List<TripPlanDto>> getTripPlansByCountry(@PathVariable String country) {
        List<TripPlan> tripPlans = tripPlanService.getTripPlansByCountry(country);
        List<TripPlanDto> tripPlanDtos = tripPlanService.getTripPlanDtosWithImages(tripPlans);
        return ResponseEntity.ok(tripPlanDtos);
    }

    /**
     * 컨셉별 여행 계획 조회 (이미지 포함)
     */
    @GetMapping("/concept/{concept}")
    public ResponseEntity<List<TripPlanDto>> getTripPlansByConcept(@PathVariable String concept) {
        List<TripPlan> tripPlans = tripPlanService.getTripPlansByConcept(concept);
        List<TripPlanDto> tripPlanDtos = tripPlanService.getTripPlanDtosWithImages(tripPlans);
        return ResponseEntity.ok(tripPlanDtos);
    }

    /**
     * 모든 여행 계획 조회 (이미지 포함)
     */
    @GetMapping
    public ResponseEntity<List<TripPlanDto>> getAllTripPlans() {
        List<TripPlan> tripPlans = tripPlanService.getAllTripPlans();
        List<TripPlanDto> tripPlanDtos = tripPlanService.getTripPlanDtosWithImages(tripPlans);
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

    /**
     * 여행 이름 수정
     */
    @PatchMapping("/{tripPlanId}/name")
    public ResponseEntity<?> updateTripPlanName(
            @PathVariable Long tripPlanId,
            @RequestBody Map<String, String> requestBody) {

        String logPrefix = "[여행이름수정API][" + tripPlanId + "]";
        log.info("{} 여행 이름 수정 요청", logPrefix);

        // 현재 사용자 이메일 가져오기
        String userEmail = getCurrentUserEmail();
        if (userEmail == null) {
            log.warn("{} 사용자 인증 정보를 찾을 수 없습니다", logPrefix);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "로그인이 필요합니다."));
        }

        // 요청 데이터 검증
        String newName = requestBody.get("name");
        if (newName == null || newName.trim().isEmpty()) {
            log.warn("{} 여행 이름이 제공되지 않았습니다", logPrefix);
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "여행 이름을 입력해주세요."));
        }

        try {
            // 여행 이름 수정
            TripPlan updatedTripPlan = tripPlanService.updateTripPlanName(tripPlanId, newName, userEmail);

            log.info("{} 여행 이름 수정 성공 - 새 이름: '{}'", logPrefix, newName);

            // 성공 응답
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tripPlanId", updatedTripPlan.getId());
            response.put("name", updatedTripPlan.getName());
            response.put("message", "여행 이름이 성공적으로 수정되었습니다.");
            response.put("updatedAt", updatedTripPlan.getUpdatedAt());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("{} 여행 이름 수정 실패: {}", logPrefix, e.getMessage());

            // 오류 응답
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());

            if (e.getMessage().contains("권한이 없습니다")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            } else if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
            } else {
                return ResponseEntity.badRequest().body(errorResponse);
            }
        }
    }

}