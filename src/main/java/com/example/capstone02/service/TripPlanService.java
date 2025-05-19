package com.example.capstone02.service;

import com.example.capstone02.dto.TripPlanRequestDto;
import com.example.capstone02.dto.TripPlanResponseDto;
import com.example.capstone02.entity.TripDay;
import com.example.capstone02.entity.TripLocation;
import com.example.capstone02.entity.TripPlan;
import com.example.capstone02.entity.User;
import com.example.capstone02.repository.TripPlanRepository;
import com.example.capstone02.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    private final UserRepository userRepository;

    /**
     * 여행 계획 생성 및 저장 (사용자 정보 포함)
     */
    @Transactional
    public TripPlan createAndSaveTripPlan(TripPlanRequestDto requestDto, String name, String userEmail) {
        try {
            // 여행 계획 생성
            TripPlanResponseDto planDto = tripPlanningService.createTripPlan(requestDto);

            // 영화 제목 조회
            String movieTitle = filmingLocationService.getMovieTitle(requestDto.getMovieId());

            // 사용자 정보 조회
            User user = null;
            if (userEmail != null) {
                user = userRepository.findByEmail(userEmail)
                        .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));
            }

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
                    .user(user) // 사용자 정보 추가
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
     * 기존 메소드 - 사용자 정보 없이 여행 계획 생성 (호환성 유지)
     */
    @Transactional
    public TripPlan createAndSaveTripPlan(TripPlanRequestDto requestDto, String name) {
        return createAndSaveTripPlan(requestDto, name, null);
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
     * 여행 계획 삭제 (ID로 삭제)
     */
    @Transactional
    public void deleteTripPlan(Long id) {
        tripPlanRepository.deleteById(id);
        log.info("여행 계획 ID {}가 삭제되었습니다.", id);
    }

    /**
     * 여행 계획 삭제 (ID로 삭제, 사용자 권한 검증)
     */
    @Transactional
    public void deleteTripPlan(Long id, String userEmail) {
        TripPlan tripPlan = tripPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("여행 계획을 찾을 수 없습니다: " + id));

        // 사용자 권한 검증 (본인이 생성한 계획만 삭제 가능)
        if (tripPlan.getUser() != null && userEmail != null) {
            User user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));

            if (!tripPlan.getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("이 여행 계획을 삭제할 권한이 없습니다.");
            }
        } else if (tripPlan.getUser() != null) {
            // 사용자 정보가 있는 계획인데 사용자 이메일이 제공되지 않은 경우
            throw new AccessDeniedException("권한 검증을 위한 사용자 정보가 필요합니다.");
        }

        tripPlanRepository.delete(tripPlan);
        log.info("여행 계획 ID {}가 사용자 {}에 의해 삭제되었습니다.", id, userEmail);
    }

    /**
     * 영화 ID로 최근 생성 일정 한 개 삭제 (사용자 이메일 검증)
     */
    @Transactional
    public boolean deleteLatestTripPlanByMovieId(Long movieId, String userEmail) {
        List<TripPlan> tripPlans = tripPlanRepository.findByMovieId(movieId);

        if (tripPlans.isEmpty()) {
            log.warn("영화 ID {}에 대한 여행 계획이 없습니다", movieId);
            return false;
        }

        // 사용자 정보 확인
        User user = null;
        if (userEmail != null) {
            user = userRepository.findByEmail(userEmail)
                    .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userEmail));
        }

        // 생성일 기준으로 내림차순 정렬하여 가장 최근 일정을 찾음
        TripPlan latestPlan = tripPlans.stream()
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .findFirst()
                .orElse(null);

        if (latestPlan == null) {
            return false;
        }

        // 사용자 권한 검증 (본인이 생성한 일정만 삭제 가능)
        if (latestPlan.getUser() != null && user != null) {
            if (!latestPlan.getUser().getId().equals(user.getId())) {
                log.warn("사용자 {}가 다른 사용자의 여행 계획을 삭제하려고 시도했습니다", userEmail);
                throw new AccessDeniedException("이 여행 계획을 삭제할 권한이 없습니다.");
            }
        }

        // 일정 삭제
        tripPlanRepository.delete(latestPlan);
        log.info("영화 ID {}의 최근 여행 계획(ID: {})이 삭제되었습니다", movieId, latestPlan.getId());

        return true;
    }

    /**
     * 특정 사용자의 여행 계획 조회
     */
    @Transactional(readOnly = true)
    public List<TripPlan> getTripPlansByUserId(Long userId) {
        return tripPlanRepository.findByUserId(userId);
    }
}