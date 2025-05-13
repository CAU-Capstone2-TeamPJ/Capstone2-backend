package com.example.capstone02.repository;

import com.example.capstone02.entity.TripPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripPlanRepository extends JpaRepository<TripPlan, Long> {
    List<TripPlan> findByMovieId(Long movieId);
    List<TripPlan> findByCountry(String country);
    List<TripPlan> findByConcept(String concept);
}