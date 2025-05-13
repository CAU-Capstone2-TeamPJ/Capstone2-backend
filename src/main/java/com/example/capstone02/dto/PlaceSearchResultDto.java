package com.example.capstone02.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class PlaceSearchResultDto {
    @JsonProperty("results")
    private List<PlaceDto> results;

    @JsonProperty("status")
    private String status;

    @Data
    public static class PlaceDto {
        @JsonProperty("place_id")
        private String placeId;

        @JsonProperty("name")
        private String name;

        @JsonProperty("photos")
        private List<PhotoDto> photos;

        @JsonProperty("geometry")
        private GeometryDto geometry;
    }

    @Data
    public static class PhotoDto {
        @JsonProperty("photo_reference")
        private String photoReference;

        @JsonProperty("height")
        private int height;

        @JsonProperty("width")
        private int width;
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