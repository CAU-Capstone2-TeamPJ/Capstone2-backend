package com.example.capstone02.controller;

import com.example.capstone02.dto.LocationDistanceInfo;
import com.example.capstone02.service.LocationTravelTimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 영화 촬영지 간 이동 시간 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/api/travel-times")
@RequiredArgsConstructor
@Slf4j
public class LocationTravelTimeController {

    private final LocationTravelTimeService locationTravelTimeService;

    /**
     * 영화 ID로 해당 영화의 모든 촬영지 간 이동 시간 정보 조회 API
     */
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<LocationDistanceInfo>> getTravelTimesByMovieId(@PathVariable Long movieId) {
        log.info("영화 ID {}의 촬영지 간 이동 시간 정보 조회 요청", movieId);

        List<LocationDistanceInfo> travelTimes = locationTravelTimeService.getLocationTravelTimesByMovieId(movieId);
        return ResponseEntity.ok(travelTimes);
    }

    /**
     * 영화 ID로 해당 영화의 모든 촬영지 간 이동 시간 계산 및 저장 API
     */
    @PostMapping("/calculate/movie/{movieId}")
    public ResponseEntity<List<LocationDistanceInfo>> calculateTravelTimes(@PathVariable Long movieId) {
        log.info("영화 ID {}의 촬영지 간 이동 시간 계산 및 저장 요청", movieId);

        List<LocationDistanceInfo> travelTimes = locationTravelTimeService.calculateAndSaveTravelTimes(movieId);
        return ResponseEntity.ok(travelTimes);
    }

    /**
     * 다수의 영화 ID 목록을 받아 모든 영화의 촬영지 간 이동 시간 계산 및 저장 API (비동기 처리)
     */
    @PostMapping("/calculate/batch")
    public ResponseEntity<String> calculateTravelTimesBatch(@RequestBody List<Long> movieIds) {
        log.info("{}개 영화의 촬영지 간 이동 시간 계산 및 저장 일괄 처리 요청", movieIds.size());

        CompletableFuture<String> future = locationTravelTimeService.calculateAndSaveTravelTimesForMovies(movieIds);

        return ResponseEntity.accepted().body("이동 시간 계산 작업이 비동기로 시작되었습니다. " + movieIds.size() + "개 영화에 대한 처리가 진행됩니다.");
    }
}