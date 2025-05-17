package com.example.capstone02.repository;

import com.example.capstone02.entity.LocationReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationReviewRepository extends JpaRepository<LocationReview, Long> {
    List<LocationReview> findByLocationId(Long locationId);
    Page<LocationReview> findByLocationId(Long locationId, Pageable pageable);

    @Query("SELECT lr FROM LocationReview lr WHERE lr.user.email = :email")
    List<LocationReview> findByUserEmail(String email);

    long countByLocationId(Long locationId);
}