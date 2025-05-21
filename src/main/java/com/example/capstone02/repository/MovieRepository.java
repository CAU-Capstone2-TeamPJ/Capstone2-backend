package com.example.capstone02.repository;

import com.example.capstone02.entity.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MovieRepository extends JpaRepository<Movie, Long> {
    Optional<Movie> findByTitle(String title);

    /**
     * 영화 ID로 촬영지 정보를 포함한 영화 상세 정보 조회
     */
    @Query("SELECT m FROM Movie m LEFT JOIN FETCH m.filmingLocations WHERE m.id = :movieId")
    Optional<Movie> findByIdWithFilmingLocations(@Param("movieId") Long movieId);
}