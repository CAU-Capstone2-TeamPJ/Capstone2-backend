package com.example.capstone02.controller;

import com.example.capstone02.config.JwtTokenProvider;
import com.example.capstone02.dto.TripPlanDto;
import com.example.capstone02.dto.UserResponseDto;
import com.example.capstone02.entity.TripPlan;
import com.example.capstone02.entity.User;
import com.example.capstone02.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @GetMapping
    public ResponseEntity<UserResponseDto> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        log.info("현재 인증: {}", authentication);

        // 로그인 확인
        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.ok(null);
        }

        // JWT 인증을 통해 UserDetails 또는 이메일 정보 추출
        String email = null;
        Object principal = authentication.getPrincipal();

        log.info("인증 주체 타입: {}", principal.getClass().getName());

        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
            log.info("UserDetails에서 이메일 추출: {}", email);
        } else if (principal instanceof String) {
            // JWT 인증에서는 principal이 이메일 문자열일 수 있음
            email = (String) principal;
            log.info("문자열에서 이메일 추출: {}", email);
        }

        if (email == null) {
            log.warn("이메일을 추출할 수 없습니다");
            return ResponseEntity.badRequest().build();
        }

        try {
            User user = userService.getUserByEmail(email);
            UserResponseDto responseDto = UserResponseDto.fromEntity(user);
            log.info("사용자 정보 조회 성공: {}", responseDto);
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            log.error("사용자 정보 조회 중 오류 발생", e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 로그인 확인
     */
    @GetMapping("/check")
    public ResponseEntity<Boolean> isLoggedIn() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isLoggedIn = authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal());

        return ResponseEntity.ok(isLoggedIn);
    }

    /**
     * 사용자의 여행 계획 목록 조회 (DTO 변환 추가)
     */
    @GetMapping("/trip-plans")
    public ResponseEntity<List<TripPlanDto>> getUserTripPlans() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            return ResponseEntity.badRequest().build();
        }

        String email = null;
        Object principal = authentication.getPrincipal();

        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else if (principal instanceof String) {
            email = (String) principal;
        }

        if (email == null) {
            return ResponseEntity.badRequest().build();
        }

        try {
            // 엔티티를 DTO로 변환하여 순환 참조 문제 해결
            List<TripPlan> tripPlans = userService.getUserTripPlans(email);
            List<TripPlanDto> tripPlanDtos = tripPlans.stream()
                    .map(TripPlanDto::fromEntity)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(tripPlanDtos);
        } catch (Exception e) {
            log.error("여행 계획 조회 중 오류 발생: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

//    /**
//     * 유저 프로필 업데이트
//     */
//    @PutMapping("/profile")
//    public ResponseEntity<UserResponseDto> updateProfile(@RequestBody Map<String, String> profileData) {
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//
//        if (authentication == null || !authentication.isAuthenticated() ||
//                "anonymousUser".equals(authentication.getPrincipal())) {
//            return ResponseEntity.badRequest().build();
//        }
//
//        String email = null;
//        Object principal = authentication.getPrincipal();
//
//        if (principal instanceof UserDetails) {
//            email = ((UserDetails) principal).getUsername();
//        } else if (principal instanceof String) {
//            email = (String) principal;
//        }
//
//        if (email == null) {
//            return ResponseEntity.badRequest().build();
//        }
//
//        String name = profileData.get("name");
//
//        if (name == null || name.trim().isEmpty()) {
//            return ResponseEntity.badRequest().build();
//        }
//
//        User updatedUser = userService.updateUserProfile(email, name);
//        return ResponseEntity.ok(UserResponseDto.fromEntity(updatedUser));
//    }

    /**
     * JWT 토큰 디버깅용 API
     */
    @GetMapping("/debug-token")
    public ResponseEntity<Map<String, Object>> debugToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().build();
        }

        String token = authHeader.substring(7);
        Map<String, Object> tokenInfo = new HashMap<>();

        try {
            boolean isValid = jwtTokenProvider.validateToken(token);
            String email = jwtTokenProvider.getEmailFromToken(token);

            tokenInfo.put("isValid", isValid);
            tokenInfo.put("email", email);

            if (isValid) {
                User user = userService.getUserByEmail(email);
                tokenInfo.put("userId", user.getId());
                tokenInfo.put("userName", user.getName());
                tokenInfo.put("userRole", user.getRole().name());
            }

            return ResponseEntity.ok(tokenInfo);
        } catch (Exception e) {
            tokenInfo.put("error", e.getMessage());
            return ResponseEntity.ok(tokenInfo);
        }
    }
}