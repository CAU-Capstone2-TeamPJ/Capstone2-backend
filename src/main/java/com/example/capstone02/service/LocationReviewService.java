package com.example.capstone02.service;

import com.example.capstone02.dto.LocationReviewDto;
import com.example.capstone02.dto.LocationReviewRequestDto;
import com.example.capstone02.entity.FilmingLocation;
import com.example.capstone02.entity.LocationReview;
import com.example.capstone02.entity.User;
import com.example.capstone02.repository.FilmingLocationRepository;
import com.example.capstone02.repository.LocationReviewRepository;
import com.example.capstone02.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationReviewService {

    private final LocationReviewRepository locationReviewRepository;
    private final FilmingLocationRepository filmingLocationRepository;
    private final UserRepository userRepository;

    /**
     * 촬영지 리뷰 등록
     */
    @Transactional
    public LocationReviewDto createReview(Long locationId, String userEmail, LocationReviewRequestDto requestDto) {
        FilmingLocation location = filmingLocationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("촬영지를 찾을 수 없습니다: " + locationId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userEmail));

        LocationReview review = LocationReview.builder()
                .location(location)
                .user(user)
                .content(requestDto.getContent())
                .rating(requestDto.getRating())
                .imageUrl(requestDto.getImageUrl())
                .build();

        LocationReview savedReview = locationReviewRepository.save(review);

        return convertToDto(savedReview);
    }

    /**
     * 촬영지 리뷰 수정
     */
    @Transactional
    public LocationReviewDto updateReview(Long reviewId, String userEmail, LocationReviewRequestDto requestDto) {
        LocationReview review = locationReviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다: " + reviewId));

        // 리뷰 작성자 확인
        if (!review.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("리뷰를 수정할 권한이 없습니다.");
        }

        review.setContent(requestDto.getContent());
        review.setRating(requestDto.getRating());
        review.setImageUrl(requestDto.getImageUrl());

        LocationReview updatedReview = locationReviewRepository.save(review);

        return convertToDto(updatedReview);
    }

    /**
     * 촬영지 리뷰 삭제
     */
    @Transactional
    public void deleteReview(Long reviewId, String userEmail) {
        LocationReview review = locationReviewRepository.findById(reviewId)
                .orElseThrow(() -> new RuntimeException("리뷰를 찾을 수 없습니다: " + reviewId));

        // 리뷰 작성자 확인
        if (!review.getUser().getEmail().equals(userEmail)) {
            throw new RuntimeException("리뷰를 삭제할 권한이 없습니다.");
        }

        locationReviewRepository.delete(review);
    }

    /**
     * 촬영지 리뷰 목록 조회
     */
    @Transactional(readOnly = true)
    public List<LocationReviewDto> getReviewsByLocationId(Long locationId) {
        List<LocationReview> reviews = locationReviewRepository.findByLocationId(locationId);
        return reviews.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 촬영지 리뷰 목록 페이징 조회
     */
    @Transactional(readOnly = true)
    public Page<LocationReviewDto> getReviewsByLocationId(Long locationId, Pageable pageable) {
        Page<LocationReview> reviewPage = locationReviewRepository.findByLocationId(locationId, pageable);
        return reviewPage.map(this::convertToDto);
    }

    /**
     * 사용자가 작성한 리뷰 목록 조회
     */
    @Transactional(readOnly = true)
    public List<LocationReviewDto> getReviewsByUserEmail(String email) {
        List<LocationReview> reviews = locationReviewRepository.findByUserEmail(email);
        return reviews.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * LocationReview 엔티티를 DTO로 변환
     */
    private LocationReviewDto convertToDto(LocationReview review) {
        return LocationReviewDto.builder()
                .id(review.getId())
                .locationId(review.getLocation().getId())
                .locationName(review.getLocation().getName())
                .userEmail(review.getUser().getEmail())
                .userName(review.getUser().getName())
                .userProfileImage(review.getUser().getPicture())
                .content(review.getContent())
                .rating(review.getRating())
                .imageUrl(review.getImageUrl())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}