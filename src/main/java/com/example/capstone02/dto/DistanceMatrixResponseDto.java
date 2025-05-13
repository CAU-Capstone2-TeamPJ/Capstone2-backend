package com.example.capstone02.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Google Distance Matrix API 응답 DTO
 */
@Data
public class DistanceMatrixResponseDto {

    @JsonProperty("destination_addresses")
    private List<String> destinationAddresses;

    @JsonProperty("origin_addresses")
    private List<String> originAddresses;

    private List<Row> rows;
    private String status;

    @Data
    public static class Row {
        private List<Element> elements;
    }

    @Data
    public static class Element {
        private Distance distance;
        private Duration duration;
        private String status;
    }

    @Data
    public static class Distance {
        private String text;
        private int value;  // 거리 (미터 단위)
    }

    @Data
    public static class Duration {
        private String text;
        private int value;  // 시간 (초 단위)
    }
}