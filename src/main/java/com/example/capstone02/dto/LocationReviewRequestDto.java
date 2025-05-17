package com.example.capstone02.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationReviewRequestDto {
    @NotBlank(message = "리뷰 내용은 필수입니다")
    @Size(min = 10, max = 1000, message = "리뷰 내용은 10자 이상 1000자 이하여야 합니다")
    private String content;

    @Min(value = 1, message = "별점은 최소 1점 이상이어야 합니다")
    @Max(value = 5, message = "별점은 최대 5점 이하여야 합니다")
    private Integer rating;

    private String imageUrl;
}