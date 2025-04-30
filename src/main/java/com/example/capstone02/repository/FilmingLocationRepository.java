package com.example.capstone02.repository;

import com.example.capstone02.entity.FilmingLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FilmingLocationRepository extends JpaRepository<FilmingLocation, Long> {
    List<FilmingLocation> findByMovieId(Long movieId);
}