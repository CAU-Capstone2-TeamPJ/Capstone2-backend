package com.example.capstone02.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class PlaceDetailsDto {
    @JsonProperty("candidates")
    private List<CandidateDto> candidates;

    @JsonProperty("status")
    private String status;

    @Data
    public static class CandidateDto {
        @JsonProperty("place_id")
        private String placeId;

        @JsonProperty("name")
        private String name;

        @JsonProperty("formatted_address")
        private String formattedAddress;

        @JsonProperty("geometry")
        private GeometryDto geometry;
    }

    @Data
    public static class GeometryDto {
        @JsonProperty("location")
        private LocationDto location;
    }

    @Data
    public static class LocationDto {
        @JsonProperty("lat")
        private double lat;

        @JsonProperty("lng")
        private double lng;
    }
}