package com.example.capstone02.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovieInfoRequestDto {
    private Long id;          // 영화 ID
    private String title;     // 영화 제목
    private String director;  // 감독 이름
    private String releaseDate; // 개봉일 (문자열 형식)
}