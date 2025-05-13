package com.example.capstone02.service;

import com.example.capstone02.dto.TripPlanRequestDto;
import com.example.capstone02.dto.TripPlanResponseDto;
import com.example.capstone02.entity.TripDay;
import com.example.capstone02.entity.TripLocation;
import com.example.capstone02.entity.TripPlan;
import com.example.capstone02.repository.TripPlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripPlanService {

    private final TripPlanRepository tripPlanRepository;
    private final TripPlanningService tripPlanningService;
    private final FilmingLocationService filmingLocationService;

    /**
     * 여행 계획 생성 및 저장
     */
    @Transactional
    public TripPlan createAndSaveTripPlan(TripPlanRequestDto requestDto, String name) {
        try {
            // 여행 계획 생성
            TripPlanResponseDto planDto = tripPlanningService.createTripPlan(requestDto);

            // 영화 제목 조회
            String movieTitle = filmingLocationService.getMovieTitle(requestDto.getMovieId());

            // TripPlan 엔티티 생성
            TripPlan tripPlan = TripPlan.builder()
                    .name(name)
                    .movieId(requestDto.getMovieId())
                    .movieTitle(movieTitle)
                    .country(requestDto.getCountry())
                    .concept(requestDto.getConcept())
                    .travelHours(requestDto.getTravelHours())
                    .totalDays(planDto.getTotalDays())
                    .totalLocations(planDto.getTotalLocations())
                    .totalTravelTimeMinutes(planDto.getTotalTravelTimeMinutes())
                    .build();

            // Empty collection 초기화
            if (tripPlan.getTripDays() == null) {
                tripPlan.setTripDays(new ArrayList<>());
            }

            // TripDay 및 TripLocation 생성
            List<TripDay> tripDays = new ArrayList<>();
            for (TripPlanResponseDto.DailyRouteDto dayDto : planDto.getDailyRoutes()) {
                TripDay tripDay = TripDay.builder()
                        .tripPlan(tripPlan)
                        .day(dayDto.getDay())
                        .travelTimeMinutes(dayDto.getTravelTimeMinutes())
                        .build();

                // Empty collection 초기화
                if (tripDay.getLocations() == null) {
                    tripDay.setLocations(new ArrayList<>());
                }

                List<TripLocation> locations = new ArrayList<>();
                for (TripPlanResponseDto.LocationRouteDto locDto : dayDto.getLocations()) {
                    // 추천 키워드 공유 참조 문제 해결을 위해 수정
                    TripLocation location = TripLocation.builder()
                            .tripDay(tripDay)
                            .locationId(locDto.getLocationId())
                            .locationName(locDto.getLocationName())
                            .address(locDto.getAddress())
                            .latitude(locDto.getLatitude())
                            .longitude(locDto.getLongitude())
                            .visitOrder(locDto.getVisitOrder())
                            .travelTimeToNext(locDto.getTravelTimeToNext())
                            .travelDistanceToNext(locDto.getTravelDistanceToNext())
                            .concept(locDto.getConcept())
                            .build();

                    // Collection 초기화 확인
                    if (location.getRecommendationKeywords() == null) {
                        location.setRecommendationKeywords(new ArrayList<>());
                    }

                    // 리스트 복사를 통해 추천 키워드 설정
                    if (locDto.getRecommendationKeywords() != null) {
                        List<String> keywordsCopy = new ArrayList<>(locDto.getRecommendationKeywords());
                        location.setRecommendationKeywords(keywordsCopy);
                    }

                    locations.add(location);
                }

                tripDay.setLocations(locations);
                tripDays.add(tripDay);
            }

            tripPlan.setTripDays(tripDays);

            // 저장
            return tripPlanRepository.save(tripPlan);
        } catch (Exception e) {
            log.error("여행 계획 저장 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("여행 계획 저장 중 오류가 발생했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * 여행 계획 조회
     */
    @Transactional(readOnly = true)
    public Optional<TripPlan> getTripPlanById(Long id) {
        return tripPlanRepository.findById(id);
    }

    /**
     * 영화별 여행 계획 조회
     */
    @Transactional(readOnly = true)
    public List<TripPlan> getTripPlansByMovieId(Long movieId) {
        return tripPlanRepository.findByMovieId(movieId);
    }

    /**
     * 국가별 여행 계획 조회
     */
    @Transactional(readOnly = true)
    public List<TripPlan> getTripPlansByCountry(String country) {
        return tripPlanRepository.findByCountry(country);
    }

    /**
     * 컨셉별 여행 계획 조회
     */
    @Transactional(readOnly = true)
    public List<TripPlan> getTripPlansByConcept(String concept) {
        return tripPlanRepository.findByConcept(concept);
    }

    /**
     * 모든 여행 계획 조회
     */
    @Transactional(readOnly = true)
    public List<TripPlan> getAllTripPlans() {
        return tripPlanRepository.findAll();
    }

    /**
     * 여행 계획 삭제
     */
    @Transactional
    public void deleteTripPlan(Long id) {
        tripPlanRepository.deleteById(id);
    }
}