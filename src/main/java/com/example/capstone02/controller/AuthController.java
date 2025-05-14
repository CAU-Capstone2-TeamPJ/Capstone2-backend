//package com.example.capstone02.controller;
//
//import com.example.capstone02.config.JwtTokenProvider;
//import com.example.capstone02.dto.UserResponseDto;
//import com.example.capstone02.entity.User;
//import com.example.capstone02.repository.UserRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
//import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
//import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
//import org.springframework.security.oauth2.core.user.OAuth2User;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@RestController
//@RequestMapping("/api/auth")
//@RequiredArgsConstructor
//@Slf4j
//public class AuthController {
//
//    private final JwtTokenProvider jwtTokenProvider;
//    private final UserRepository userRepository;
//    private final OAuth2AuthorizedClientService clientService;
//
//    /**
//     * 테스트용 개발 계정으로 로그인해 JWT 토큰 발급
//     * 실제 프로덕션에서는 사용하지 않음
//     */
//    @GetMapping("/dev-token")
//    public ResponseEntity<Map<String, String>> getDevToken() {
//        // 개발용 테스트 계정 이메일
//        String testEmail = "test@example.com";
//
//        // 테스트 사용자가 없으면 생성
//        User user = userRepository.findByEmail(testEmail)
//                .orElseGet(() -> {
//                    User newUser = User.builder()
//                            .name("개발 테스트 계정")
//                            .email(testEmail)
//                            .socialId(testEmail)
//                            .role(User.Role.USER)
//                            .picture("https://ui-avatars.com/api/?name=Dev+User")
//                            .build();
//                    return userRepository.save(newUser);
//                });
//
//        // JWT 토큰 생성
//        String token = jwtTokenProvider.createToken(testEmail, user.getRole().name());
//
//        Map<String, String> response = new HashMap<>();
//        response.put("token", token);
//        response.put("tokenType", "Bearer");
//        response.put("message", "이 토큰은 개발 테스트용입니다. Swagger에서 'Authorize' 버튼을 클릭하고 이 토큰을 입력하세요.");
//
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * 현재 인증된 사용자의 JWT 토큰을 발급
//     */
//    @GetMapping("/token")
//    public ResponseEntity<Map<String, String>> getToken(@AuthenticationPrincipal OAuth2User principal) {
//        if (principal == null) {
//            return ResponseEntity.badRequest().build();
//        }
//
//        String email = principal.getAttribute("email");
//        if (email == null) {
//            return ResponseEntity.badRequest().build();
//        }
//
//        // 사용자 정보 조회
//        User user = userRepository.findByEmail(email)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        // JWT 토큰 생성
//        String token = jwtTokenProvider.createToken(email, user.getRole().name());
//
//        Map<String, String> response = new HashMap<>();
//        response.put("token", token);
//        response.put("tokenType", "Bearer");
//
//        return ResponseEntity.ok(response);
//    }
//
//    /**
//     * JWT 토큰의 유효성 검증
//     */
//    @GetMapping("/validate")
//    public ResponseEntity<Boolean> validateToken(@RequestParam String token) {
//        boolean isValid = jwtTokenProvider.validateToken(token);
//        return ResponseEntity.ok(isValid);
//    }
//
//    /**
//     * JWT 토큰으로 사용자 정보 조회
//     */
//    @GetMapping("/me")
//    public ResponseEntity<UserResponseDto> getUserInfo(@RequestHeader("Authorization") String authHeader) {
//        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
//            return ResponseEntity.badRequest().build();
//        }
//
//        String token = authHeader.substring(7);
//        if (!jwtTokenProvider.validateToken(token)) {
//            return ResponseEntity.badRequest().build();
//        }
//
//        String email = jwtTokenProvider.getEmailFromToken(token);
//        User user = userRepository.findByEmail(email)
//                .orElseThrow(() -> new RuntimeException("User not found"));
//
//        return ResponseEntity.ok(UserResponseDto.fromEntity(user));
//    }
//}