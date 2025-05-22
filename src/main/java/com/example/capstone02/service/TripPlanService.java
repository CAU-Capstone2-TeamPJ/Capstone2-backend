package com.example.capstone02.service;

import com.example.capstone02.dto.TripPlanDto;
import com.example.capstone02.dto.TripPlanRequestDto;
import com.example.capstone02.dto.TripPlanResponseDto;
import com.example.capstone02.entity.FilmingLocation;
import com.example.capstone02.entity.TripDay;
import com.example.capstone02.entity.TripLocation;
import com.example.capstone02.entity.TripPlan;
import com.example.capstone02.entity.User;
import com.example.capstone02.repository.FilmingLocationRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripPlanService {

    private final TripPlanRepository tripPlanRepository;
    private final TripPlanningService tripPlanningService;
    private final FilmingLocationService filmingLocationService;
    private final UserRepository userRepository;
    private final FilmingLocationRepository filmingLocationRepository;
    private final GoogleMapsService googleMapsService;

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
     * 여행 계획 조회 (이미지 포함)
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

    /**
     * 여행 계획 DTO 변환 (이미지 정보 포함)
     */
    @Transactional(readOnly = true)
    public TripPlanDto getTripPlanDtoWithImages(Long id) {
        TripPlan tripPlan = tripPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("여행 계획을 찾을 수 없습니다: " + id));

        // 기본 DTO 변환
        TripPlanDto tripPlanDto = TripPlanDto.fromEntity(tripPlan);

        // 각 장소별 이미지 정보 가져오기
        for (TripPlanDto.TripDayDto dayDto : tripPlanDto.getTripDays()) {
            for (TripPlanDto.TripLocationDto locationDto : dayDto.getLocations()) {
                // 원본 FilmingLocation에서 이미지 정보 가져오기
                addImagesForLocation(locationDto);
            }
        }

        return tripPlanDto;
    }

    /**
     * 장소에 이미지 정보 추가
     */
    private void addImagesForLocation(TripPlanDto.TripLocationDto locationDto) {
        try {
            // 원본 촬영지 ID로 FilmingLocation 조회
            Long locationId = locationDto.getLocationId();
            if (locationId == null) {
                log.warn("장소 '{}' 원본 locationId가 없습니다", locationDto.getLocationName());
                return;
            }

            Optional<FilmingLocation> filmingLocationOpt = filmingLocationRepository.findById(locationId);
            if (filmingLocationOpt.isPresent()) {
                FilmingLocation filmingLocation = filmingLocationOpt.get();
                if (filmingLocation.getImages() != null && !filmingLocation.getImages().isEmpty()) {
                    // 원본 장소의 이미지 가져오기
                    locationDto.setImages(new ArrayList<>(filmingLocation.getImages()));
                    log.debug("장소 '{}' 이미지 {}개 추가됨", locationDto.getLocationName(), filmingLocation.getImages().size());
                } else if (locationDto.getLatitude() != null && locationDto.getLongitude() != null) {
                    // 이미지가 없으면 실시간으로 구글맵스에서 이미지 가져오기 시도
                    try {
                        List<String> images = googleMapsService.getLocationImages(
                                locationDto.getLatitude(), locationDto.getLongitude());
                        if (!images.isEmpty()) {
                            locationDto.setImages(images);
                            log.debug("장소 '{}' 실시간 이미지 {}개 추가됨",
                                    locationDto.getLocationName(), images.size());
                        }
                    } catch (Exception e) {
                        log.warn("장소 '{}' 이미지 실시간 조회 실패: {}",
                                locationDto.getLocationName(), e.getMessage());
                    }
                }
            } else {
                log.warn("장소 ID {}에 해당하는 원본 촬영지 정보를 찾을 수 없습니다", locationId);
            }
        } catch (Exception e) {
            log.error("장소 '{}' 이미지 정보 추가 중 오류 발생: {}",
                    locationDto.getLocationName(), e.getMessage());
        }
    }

    /**
     * 리스트 조회 시에도 이미지 정보 포함
     */
    @Transactional(readOnly = true)
    public List<TripPlanDto> getTripPlanDtosWithImages(List<TripPlan> tripPlans) {
        List<TripPlanDto> tripPlanDtos = tripPlans.stream()
                .map(TripPlanDto::fromEntity)
                .collect(Collectors.toList());

        // 각 계획의 모든 장소에 이미지 정보 추가
        for (TripPlanDto tripPlanDto : tripPlanDtos) {
            if (tripPlanDto.getTripDays() != null) {
                for (TripPlanDto.TripDayDto dayDto : tripPlanDto.getTripDays()) {
                    if (dayDto.getLocations() != null) {
                        for (TripPlanDto.TripLocationDto locationDto : dayDto.getLocations()) {
                            addImagesForLocation(locationDto);
                        }
                    }
                }
            }
        }

        return tripPlanDtos;
    }

    /**
     * 여행 이름만 수정
     */
    @Transactional
    public TripPlan updateTripPlanName(Long tripPlanId, String newName, String userEmail) {
        String logPrefix = "[여행이름수정][" + tripPlanId + "]";
        log.info("{} 여행 ID {}의 이름 수정 요청 - 새 이름: '{}', 사용자: {}",
                logPrefix, tripPlanId, newName, userEmail);

        // 1. 여행 계획 조회
        TripPlan tripPlan = tripPlanRepository.findById(tripPlanId)
                .orElseThrow(() -> new RuntimeException("여행 계획을 찾을 수 없습니다: " + tripPlanId));

        // 2. 사용자 권한 확인
        if (!tripPlan.getUser().getEmail().equals(userEmail)) {
            log.warn("{} 권한 없음 - 여행 소유자: {}, 요청자: {}",
                    logPrefix, tripPlan.getUser().getEmail(), userEmail);
            throw new RuntimeException("해당 여행 계획을 수정할 권한이 없습니다.");
        }

        // 3. 여행 이름 유효성 검사
        if (newName == null || newName.trim().isEmpty()) {
            throw new RuntimeException("여행 이름을 입력해주세요.");
        }

        if (newName.trim().length() > 100) { // 예시 길이 제한
            throw new RuntimeException("여행 이름은 100자를 초과할 수 없습니다.");
        }

        // 4. 이전 이름 저장 (로그용)
        String oldName = tripPlan.getName();

        // 5. 여행 이름 수정
        tripPlan.setName(newName.trim());

        // 6. 저장
        TripPlan updatedTripPlan = tripPlanRepository.save(tripPlan);

        log.info("{} 여행 이름 수정 완료 - 이전: '{}' -> 새 이름: '{}'",
                logPrefix, oldName, newName.trim());

        return updatedTripPlan;
    }
}