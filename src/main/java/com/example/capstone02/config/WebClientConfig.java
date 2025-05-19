package com.example.capstone02.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {

    // 파이썬 서버 요청 타임아웃 설정 (15분)
    private static final int TIMEOUT_PYTHON_SERVER = 1800; // 초 단위 (30분)

    // 일반 API 요청 타임아웃 설정 (60초)
    private static final int TIMEOUT_DEFAULT = 60; // 초 단위

    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .defaultHeader("accept", "application/json")
                .clientConnector(new ReactorClientHttpConnector(getHttpClient(TIMEOUT_DEFAULT)))
                .build();
    }

    @Bean(name = "longTimeoutWebClient")
    public WebClient longTimeoutWebClient(WebClient.Builder builder) {
        return builder
                .defaultHeader("accept", "application/json")
                .clientConnector(new ReactorClientHttpConnector(getHttpClient(TIMEOUT_PYTHON_SERVER)))
                .build();
    }

    private HttpClient getHttpClient(int timeoutSeconds) {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutSeconds * 1000)
                .responseTimeout(Duration.ofSeconds(timeoutSeconds))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(timeoutSeconds, TimeUnit.SECONDS)));
    }
}