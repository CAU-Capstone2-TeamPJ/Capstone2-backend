package com.example.capstone02.repository;

import com.example.capstone02.entity.FilmingLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FilmingLocationRepository extends JpaRepository<FilmingLocation, Long> {

    // 영화 ID로 모든 촬영지 조회
    List<FilmingLocation> findByMovieId(Long movieId);

    // 영화 ID로 모든 촬영지 삭제
    void deleteAllByMovieId(Long movieId);

    // 특정 영화의 촬영지가 존재하는지 확인
    boolean existsByMovieId(Long movieId);

    // 특정 위치(위도/경도) 주변의 모든 촬영지 찾기 (선택적 기능)
    @Query("SELECT f FROM FilmingLocation f WHERE " +
            "6371 * acos(cos(radians(:latitude)) * cos(radians(f.latitude)) * " +
            "cos(radians(f.longitude) - radians(:longitude)) + " +
            "sin(radians(:latitude)) * sin(radians(f.latitude))) < :distance")
    List<FilmingLocation> findLocationsByGeoDistance(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("distance") double distanceInKm);
}