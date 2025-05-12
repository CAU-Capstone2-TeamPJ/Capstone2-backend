package com.example.capstone02.repository;

import com.example.capstone02.entity.FilmingLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FilmingLocationRepository extends JpaRepository<FilmingLocation, Long> {
    // 수정: movie_movieId -> movie_id (Movie 엔티티의 id 필드에 맞게 수정)
    List<FilmingLocation> findByMovie_Id(Long movieId);
}