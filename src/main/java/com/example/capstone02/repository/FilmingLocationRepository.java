package com.example.capstone02.repository;

import com.example.capstone02.entity.FilmingLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FilmingLocationRepository extends JpaRepository<FilmingLocation, Long> {

    // 영화 ID로 모든 촬영지 조회
    List<FilmingLocation> findByMovieId(Long movieId);

    // 영화 ID로 모든 촬영지 삭제
    @Transactional
    void deleteAllByMovieId(Long movieId);

    // 특정 영화의 촬영지가 존재하는지 확인
    boolean existsByMovieId(Long movieId);
}