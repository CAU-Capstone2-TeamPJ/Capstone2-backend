package com.example.capstone02.security;

import com.example.capstone02.config.JwtTokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String jwt = getJwtFromRequest(request);
            log.debug("JWT 토큰 추출: {}", jwt != null ? jwt.substring(0, Math.min(10, jwt.length())) + "..." : "없음");

            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {
                // JWT 토큰에서 email과 role 추출
                String email = jwtTokenProvider.getEmailFromToken(jwt);
                String role = jwtTokenProvider.getClaimFromToken(jwt, claims -> claims.get("role", String.class));

                log.debug("JWT 토큰 유효: 이메일={}, 역할={}", email, role);

                // SimpleGrantedAuthority 설정
                SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + role);

                // Authentication 객체 생성 (Principal은 email 문자열)
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(email, null, Collections.singletonList(authority));

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.debug("인증 컨텍스트에 Authentication 설정됨");
            } else if (StringUtils.hasText(jwt)) {
                log.debug("유효하지 않은 JWT 토큰");
            }
        } catch (Exception ex) {
            log.error("인증 컨텍스트 설정 중 오류 발생", ex);
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}