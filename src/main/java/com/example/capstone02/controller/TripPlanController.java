package com.example.capstone02.controller;

import com.example.capstone02.dto.TripPlanRequestDto;
import com.example.capstone02.entity.TripPlan;
import com.example.capstone02.service.TripPlanService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/saved-trip-plans")
@RequiredArgsConstructor
@Slf4j
public class TripPlanController {

    private final TripPlanService tripPlanService;

    /**
     * 여행 계획 저장
     */
    @PostMapping
    public ResponseEntity<TripPlan> saveTripPlan(@RequestBody SaveTripPlanRequestDto request) {
        log.info("여행 계획 저장 요청: {}", request);

        if (request.getTripPlanRequest() == null || request.getName() == null || request.getName().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        TripPlan savedPlan = tripPlanService.createAndSaveTripPlan(request.getTripPlanRequest(), request.getName());
        return ResponseEntity.ok(savedPlan);
    }

    /**
     * 저장된 여행 계획 조회
     */
    @GetMapping("/{id}")
    public ResponseEntity<TripPlan> getTripPlan(@PathVariable Long id) {
        Optional<TripPlan> tripPlan = tripPlanService.getTripPlanById(id);
        return tripPlan.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    /**
     * 영화별 여행 계획 조회
     */
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<TripPlan>> getTripPlansByMovie(@PathVariable Long movieId) {
        List<TripPlan> tripPlans = tripPlanService.getTripPlansByMovieId(movieId);
        return ResponseEntity.ok(tripPlans);
    }

    /**
     * 국가별 여행 계획 조회
     */
    @GetMapping("/country/{country}")
    public ResponseEntity<List<TripPlan>> getTripPlansByCountry(@PathVariable String country) {
        List<TripPlan> tripPlans = tripPlanService.getTripPlansByCountry(country);
        return ResponseEntity.ok(tripPlans);
    }

    /**
     * 컨셉별 여행 계획 조회
     */
    @GetMapping("/concept/{concept}")
    public ResponseEntity<List<TripPlan>> getTripPlansByConcept(@PathVariable String concept) {
        List<TripPlan> tripPlans = tripPlanService.getTripPlansByConcept(concept);
        return ResponseEntity.ok(tripPlans);
    }

    /**
     * 모든 여행 계획 조회
     */
    @GetMapping
    public ResponseEntity<List<TripPlan>> getAllTripPlans() {
        List<TripPlan> tripPlans = tripPlanService.getAllTripPlans();
        return ResponseEntity.ok(tripPlans);
    }

    /**
     * 여행 계획 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTripPlan(@PathVariable Long id) {
        tripPlanService.deleteTripPlan(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * 여행 계획 저장 요청 DTO
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SaveTripPlanRequestDto {
        private String name;  // 여행 계획 이름
        private TripPlanRequestDto tripPlanRequest;  // 여행 계획 요청 정보
    }
}