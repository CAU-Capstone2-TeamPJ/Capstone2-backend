package com.example.capstone02.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationReviewDto {
    private Long id;
    private Long locationId;
    private String locationName;
    private String userEmail;
    private String userName;
    private String userProfileImage;
    private String content;
    private Integer rating;
    private String imageUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}