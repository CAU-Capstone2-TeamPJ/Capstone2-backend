package com.example.capstone02.service;

import com.example.capstone02.dto.MovieDetailDto;
import com.example.capstone02.dto.MovieLikeDto;
import com.example.capstone02.entity.Movie;
import com.example.capstone02.entity.MovieLike;
import com.example.capstone02.entity.User;
import com.example.capstone02.repository.MovieLikeRepository;
import com.example.capstone02.repository.MovieRepository;
import com.example.capstone02.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieLikeService {

    private final MovieLikeRepository movieLikeRepository;
    private final MovieRepository movieRepository;
    private final UserRepository userRepository;

    /**
     * 영화 좋아요 상태 전환
     */
    @Transactional
    public MovieLikeDto toggleLike(Long movieId, String userEmail) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("영화를 찾을 수 없습니다: " + movieId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userEmail));

        boolean isLiked = movieLikeRepository.existsByMovieAndUser(movie, user);

        if (isLiked) {
            // 이미 좋아요 상태라면 좋아요 취소
            movieLikeRepository.deleteByMovieAndUser(movie, user);
            log.info("사용자 {} 영화 {} 좋아요 취소", userEmail, movieId);
            isLiked = false;
        } else {
            // 좋아요가 없다면 좋아요 추가
            MovieLike movieLike = MovieLike.builder()
                    .movie(movie)
                    .user(user)
                    .build();
            movieLikeRepository.save(movieLike);
            log.info("사용자 {} 영화 {} 좋아요 추가", userEmail, movieId);
            isLiked = true;
        }

        // 영화의 좋아요 수 조회
        Long likesCount = movieLikeRepository.countByMovieId(movieId);

        return MovieLikeDto.builder()
                .movieId(movieId)
                .likesCount(likesCount.intValue())
                .isLiked(isLiked)
                .build();
    }

    /**
     * 영화의 좋아요 상태와 개수 조회
     */
    @Transactional(readOnly = true)
    public MovieLikeDto getLikeStatus(Long movieId, String userEmail) {
        // 사용자 정보 확인
        boolean isLiked = false;
        if (userEmail != null) {
            User user = userRepository.findByEmail(userEmail).orElse(null);
            if (user != null) {
                Movie movie = movieRepository.findById(movieId).orElse(null);
                if (movie != null) {
                    isLiked = movieLikeRepository.existsByMovieAndUser(movie, user);
                }
            }
        }

        // 영화의 좋아요 수 조회
        Long likesCount = movieLikeRepository.countByMovieId(movieId);

        return MovieLikeDto.builder()
                .movieId(movieId)
                .likesCount(likesCount.intValue())
                .isLiked(isLiked)
                .build();
    }

    /**
     * 좋아요 순으로 영화 ID 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Long> getMovieIdsByLikesCountDesc() {
        return movieLikeRepository.findMovieIdsByLikesCountDesc();
    }

    /**
     * 좋아요 순으로 영화 목록 조회
     */
    @Transactional(readOnly = true)
    public List<MovieDetailDto> getMoviesByLikesCount() {
        List<Long> movieIds = getMovieIdsByLikesCountDesc();

        return movieIds.stream()
                .map(id -> {
                    try {
                        Movie movie = movieRepository.findById(id).orElse(null);
                        if (movie != null) {
                            MovieDetailDto dto = MovieDetailDto.fromEntity(movie);
                            // 좋아요 수 설정
                            Long likesCount = movieLikeRepository.countByMovieId(id);
                            dto.setLikesCount(likesCount.intValue());
                            return dto;
                        }
                        return null;
                    } catch (Exception e) {
                        log.error("영화 정보 조회 실패: {}", id, e);
                        return null;
                    }
                })
                .filter(movie -> movie != null)
                .collect(Collectors.toList());
    }

    /**
     * 사용자가 좋아요한 영화 목록 조회
     */
    @Transactional(readOnly = true)
    public List<MovieDetailDto> getLikedMoviesByUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userEmail));

        List<Movie> likedMovies = movieLikeRepository.findMoviesByUserId(user.getId());

        return likedMovies.stream()
                .map(movie -> {
                    MovieDetailDto dto = MovieDetailDto.fromEntity(movie);
                    // 좋아요 수 설정
                    Long likesCount = movieLikeRepository.countByMovieId(movie.getId());
                    dto.setLikesCount(likesCount.intValue());
                    dto.setIsLiked(true); // 사용자가 좋아요한 영화이므로 true로 설정
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * 사용자가 좋아요한 영화 ID 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Long> getLikedMovieIdsByUser(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userEmail));

        return movieLikeRepository.findMovieIdsByUserId(user.getId());
    }
}