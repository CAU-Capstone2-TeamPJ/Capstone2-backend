package com.example.capstone02.controller;

import com.example.capstone02.dto.UserResponseDto;
import com.example.capstone02.entity.TripPlan;
import com.example.capstone02.entity.User;
import com.example.capstone02.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * 현재 로그인한 사용자 정보 조회
     */
    @GetMapping
    public ResponseEntity<UserResponseDto> getCurrentUser(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.ok(null);
        }

        String email = principal.getAttribute("email");
        User user = userService.getUserByEmail(email);

        return ResponseEntity.ok(UserResponseDto.fromEntity(user));
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
     * 사용자의 여행 계획 목록 조회
     */
    @GetMapping("/trip-plans")
    public ResponseEntity<List<TripPlan>> getUserTripPlans(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) {
            return ResponseEntity.ok(null);
        }

        String email = principal.getAttribute("email");
        List<TripPlan> tripPlans = userService.getUserTripPlans(email);

        return ResponseEntity.ok(tripPlans);
    }

    /**
     * 유저 프로필 업데이트
     */
    @PutMapping("/profile")
    public ResponseEntity<UserResponseDto> updateProfile(
            @AuthenticationPrincipal OAuth2User principal,
            @RequestBody Map<String, String> profileData) {

        if (principal == null) {
            return ResponseEntity.badRequest().build();
        }

        String email = principal.getAttribute("email");
        String name = profileData.get("name");

        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        User updatedUser = userService.updateUserProfile(email, name);
        return ResponseEntity.ok(UserResponseDto.fromEntity(updatedUser));
    }
}