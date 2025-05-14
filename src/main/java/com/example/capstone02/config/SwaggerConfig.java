package com.example.capstone02.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Bean
    public OpenAPI openAPI() {
        // JWT Bearer 인증만 사용
        return new OpenAPI()
                .info(new Info().title("영화 촬영지 API")
                        .description("영화 촬영지 정보 및 거리 계산 API\n\n" +
                                "인증 방법:\n" +
                                "1. 브라우저에서 `/oauth2/authorization/google`로 접속하여 구글 로그인\n" +
                                "2. 로그인 성공 후 토큰을 받아 아래 Authorize 버튼에 입력 (Bearer 접두사 없이)")
                        .version("v1.0.0")
                        .license(new License().name("Apache 2.0").url("http://springdoc.org")))
                .servers(List.of(new Server().url("/").description("Default Server URL")))
                .components(new Components()
                        .addSecuritySchemes("JWT", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 토큰을 입력하세요 (Bearer 접두사 없이)")))
                .addSecurityItem(new SecurityRequirement().addList("JWT"));
    }
}