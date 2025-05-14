package com.example.capstone02.oauth;

import com.example.capstone02.config.JwtTokenProvider;
import com.example.capstone02.entity.User;
import com.example.capstone02.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    // 프론트엔드 URL (application.yml에서 설정 가능)
    private final String frontendUrl = "http://localhost:3000";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        // 사용자 정보 찾기
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // JWT 토큰 생성
            String token = jwtTokenProvider.createToken(email, user.getRole().name());

            // 프론트엔드 리다이렉트 URL에 토큰 추가
            String redirectUrl = UriComponentsBuilder.fromUriString(frontendUrl)
                    .queryParam("token", token)
                    .build().toUriString();

            log.info("OAuth2 로그인 성공: {}, 리다이렉트: {}", email, redirectUrl);

            // 리다이렉트
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        } else {
            log.error("OAuth2 로그인 성공했지만 사용자 정보를 찾을 수 없음: {}", email);
            super.onAuthenticationSuccess(request, response, authentication);
        }
    }
}