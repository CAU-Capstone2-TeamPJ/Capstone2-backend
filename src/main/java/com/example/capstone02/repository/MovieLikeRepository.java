package com.example.capstone02.repository;

import com.example.capstone02.entity.Movie;
import com.example.capstone02.entity.MovieLike;
import com.example.capstone02.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MovieLikeRepository extends JpaRepository<MovieLike, Long> {
    Optional<MovieLike> findByMovieAndUser(Movie movie, User user);
    boolean existsByMovieAndUser(Movie movie, User user);
    void deleteByMovieAndUser(Movie movie, User user);

    @Query("SELECT COUNT(ml) FROM MovieLike ml WHERE ml.movie.id = :movieId")
    Long countByMovieId(Long movieId);

    @Query("SELECT m.id FROM Movie m LEFT JOIN MovieLike ml ON m.id = ml.movie.id GROUP BY m.id ORDER BY COUNT(ml.id) DESC")
    List<Long> findMovieIdsByLikesCountDesc();
}