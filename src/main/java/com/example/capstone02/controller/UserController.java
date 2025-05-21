package com.example.capstone02.controller;

import com.example.capstone02.config.JwtTokenProvider;
import com.example.capstone02.dto.TripPlanDto;
import com.example.capstone02.dto.UserResponseDto;
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
     * 사용자의 여행 계획 목록 조회 (이미지 포함)
     */
    @GetMapping("/trip-plans")
    public ResponseEntity<List<TripPlanDto>> getUserTripPlans() {
        String email = getCurrentUserEmail();

        if (email == null) {
            log.warn("사용자 인증 정보를 찾을 수 없습니다");
            return ResponseEntity.badRequest().build();
        }

        try {
            // 이미지 정보가 포함된 여행 계획 DTO 반환
            List<TripPlanDto> tripPlansWithImages = userService.getUserTripPlansWithImages(email);
            log.info("사용자 {}의 여행 계획 {}개 조회 완료 (이미지 포함)", email, tripPlansWithImages.size());

            return ResponseEntity.ok(tripPlansWithImages);
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
        else if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }

        return null;
    }
}