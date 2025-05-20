package com.example.capstone02.repository;

import com.example.capstone02.entity.LocationTravelTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LocationTravelTimeRepository extends JpaRepository<LocationTravelTime, Long> {

    /**
     * 영화 ID와 출발/도착 촬영지 ID로 이동 시간 정보 조회
     */
    Optional<LocationTravelTime> findByMovieIdAndFromLocationIdAndToLocationId(
            Long movieId, Long fromLocationId, Long toLocationId);

    /**
     * 영화 ID로 해당 영화의 모든 촬영지 간 이동 시간 정보 조회
     */
    List<LocationTravelTime> findByMovieId(Long movieId);

    /**
     * 영화 ID와 특정 촬영지 ID로 해당 촬영지와 관련된 모든 이동 시간 정보 조회
     */
    @Query("SELECT ltt FROM LocationTravelTime ltt WHERE ltt.movieId = :movieId " +
            "AND (ltt.fromLocationId = :locationId OR ltt.toLocationId = :locationId)")
    List<LocationTravelTime> findByMovieIdAndLocationId(
            @Param("movieId") Long movieId,
            @Param("locationId") Long locationId);

    /**
     * 영화 ID와 여러 촬영지 ID 목록으로 관련된 모든 이동 시간 정보 조회
     */
    @Query("SELECT ltt FROM LocationTravelTime ltt WHERE ltt.movieId = :movieId " +
            "AND (ltt.fromLocationId IN :locationIds AND ltt.toLocationId IN :locationIds)")
    List<LocationTravelTime> findByMovieIdAndLocationIds(
            @Param("movieId") Long movieId,
            @Param("locationIds") List<Long> locationIds);

    /**
     * 영화 ID로 해당 영화의 모든 촬영지 간 이동 시간 정보 삭제
     */
    void deleteByMovieId(Long movieId);
}