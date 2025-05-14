package com.example.capstone02.controller;

import com.example.capstone02.dto.TripPlanRequestDto;
import com.example.capstone02.dto.TripPlanResponseDto;
import com.example.capstone02.entity.TripPlan;
import com.example.capstone02.entity.User;
import com.example.capstone02.repository.UserRepository;
import com.example.capstone02.service.TripPlanService;
import com.example.capstone02.service.TripPlanningService;
import com.example.capstone02.util.ConceptKeywordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/trip-plans")
@RequiredArgsConstructor
@Slf4j
public class TripPlanningController {

    private final TripPlanningService tripPlanningService;
    private final TripPlanService tripPlanService;
    private final ConceptKeywordMapper conceptKeywordMapper;
    private final UserRepository userRepository;

    /**
     * 여행 경로 계획 생성 API - 생성 후 자동 저장
     */
    @PostMapping
    public ResponseEntity<TripPlanResponseWithIdDto> createTripPlan(@RequestBody TripPlanRequestDto request) {
        log.info("여행 경로 계획 요청: {}", request);

        // 필수 파라미터 확인
        if (request.getMovieId() == null) {
            return ResponseEntity.badRequest().build();
        }

        // 여행 계획 생성
        TripPlanResponseDto tripPlan = tripPlanningService.createTripPlan(request);

        // 자동 저장 - 이름은 "영화 제목 + 컨셉 + UUID"로 자동 생성
        String planName = generatePlanName(request);

        // 현재 인증된 사용자 정보 가져오기
        String userEmail = getCurrentUserEmail();
        log.info("현재 인증된 사용자 이메일: {}", userEmail);

        // 여행 계획 저장
        TripPlan savedPlan = tripPlanService.createAndSaveTripPlan(request, planName, userEmail);

        // 응답 DTO 생성 (기존 TripPlanResponseDto + 저장된 계획 ID)
        TripPlanResponseWithIdDto responseWithId = new TripPlanResponseWithIdDto(
                tripPlan.getDailyRoutes(),
                tripPlan.getTotalDays(),
                tripPlan.getTotalLocations(),
                tripPlan.getTotalTravelTimeMinutes(),
                savedPlan.getId(),
                planName
        );

        return ResponseEntity.ok(responseWithId);
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
        log.info("인증 주체 정보: 클래스={}, 값={}",
                principal.getClass().getName(), principal);

        String email = null;

        // JWT 토큰 인증의 경우 principal이 이메일 문자열
        if (principal instanceof String) {
            email = (String) principal;
        }
        // Spring Security UserDetails 사용 시
        else if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        }

        if (email != null) {
            // 사용자가 실제로 존재하는지 확인
            Optional<User> userOpt = userRepository.findByEmail(email);
            if (!userOpt.isPresent()) {
                log.warn("이메일 {}에 해당하는 사용자가 데이터베이스에 없습니다", email);
                return null;
            }
        }

        return email;
    }

    /**
     * 계획 이름 자동 생성
     */
    private String generatePlanName(TripPlanRequestDto request) {
        // 국가
        String country = request.getCountry() != null ? request.getCountry() : "";

        // 컨셉 결합
        String conceptText;

        if (request.getConcepts() != null && !request.getConcepts().isEmpty()) {
            // 다중 컨셉 - 최대 2개까지만 표시하고 나머지는 "외"로 처리
            if (request.getConcepts().size() == 1) {
                conceptText = request.getConcepts().get(0);
            } else if (request.getConcepts().size() == 2) {
                conceptText = request.getConcepts().get(0) + ", " + request.getConcepts().get(1);
            } else {
                conceptText = request.getConcepts().get(0) + " 외 " + (request.getConcepts().size() - 1) + "개";
            }
        } else if (request.getConcept() != null && !request.getConcept().isEmpty()) {
            // 이전 버전과의 호환성 - 단일 컨셉
            conceptText = request.getConcept();
        } else {
            // 컨셉 없음
            conceptText = "여행 계획";
        }

        // UUID의 일부만 사용해서 고유한 이름 생성
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        return String.format("%s의 %s 여행 (%s)", country, conceptText, uniqueId);
    }

    /**
     * 컨셉 키워드 유효성 검증
     */
    @GetMapping("/validate-concept")
    public ResponseEntity<Boolean> validateConcept(@RequestParam String concept) {
        // 컨셉 유효성 검증
        List<String> allConcepts = conceptKeywordMapper.getAllConcepts();
        return ResponseEntity.ok(allConcepts.contains(concept));
    }

    /**
     * 응답 확장 DTO - 저장 ID와 이름 추가
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TripPlanResponseWithIdDto extends TripPlanResponseDto {
        private Long savedPlanId;
        private String planName;

        public TripPlanResponseWithIdDto(
                List<DailyRouteDto> dailyRoutes,
                Integer totalDays,
                Integer totalLocations,
                Integer totalTravelTimeMinutes,
                Long savedPlanId,
                String planName) {
            super(dailyRoutes, totalDays, totalLocations, totalTravelTimeMinutes);
            this.savedPlanId = savedPlanId;
            this.planName = planName;
        }
    }
}