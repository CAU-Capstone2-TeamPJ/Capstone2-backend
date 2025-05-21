package com.example.capstone02.service;

import com.example.capstone02.dto.TripPlanDto;
import com.example.capstone02.entity.FilmingLocation;
import com.example.capstone02.entity.TripPlan;
import com.example.capstone02.entity.User;
import com.example.capstone02.repository.FilmingLocationRepository;
import com.example.capstone02.repository.TripPlanRepository;
import com.example.capstone02.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final TripPlanRepository tripPlanRepository;
    private final FilmingLocationRepository filmingLocationRepository;
    private final GoogleMapsService googleMapsService;
    private final TripPlanService tripPlanService;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 이메일로 사용자 조회
     */
    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));
    }

    /**
     * 사용자 ID로 사용자 조회
     */
    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + id));
    }

    /**
     * 사용자의 여행 계획 목록 조회 - N+1 문제 방지 및 순환 참조 문제 해결
     */
    @Transactional(readOnly = true)
    public List<TripPlan> getUserTripPlans(String email) {
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();

            // 사용자 ID로 직접 TripPlan 조회 (N+1 문제 회피)
            List<TripPlan> tripPlans = tripPlanRepository.findByUserId(user.getId());

            // 각 TripPlan 엔티티의 user 참조를 null로 설정하여 순환 참조 방지
            // (실제 객체 상태를 변경하지 않고 JSON 변환 시 순환 참조만 방지)
            tripPlans.forEach(plan -> plan.setUser(null));

            return tripPlans;
        }

        return Collections.emptyList();
    }

    /**
     * 사용자의 여행 계획 목록 조회 (DTO 변환 및 이미지 포함)
     */
    @Transactional(readOnly = true)
    public List<TripPlanDto> getUserTripPlansWithImages(String email) {
        List<TripPlan> tripPlans = getUserTripPlans(email);
        return tripPlanService.getTripPlanDtosWithImages(tripPlans);
    }

    /**
     * 사용자 프로필 업데이트
     */
    @Transactional
    public User updateUserProfile(String email, String name) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + email));

        user.setName(name);
        return userRepository.save(user);
    }
}