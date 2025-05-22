package com.example.capstone02.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class PlaceFindResultDto {

    @JsonProperty("candidates")
    private List<Candidate> candidates;

    @JsonProperty("status")
    private String status;

    @Data
    @NoArgsConstructor
    public static class Candidate {

        @JsonProperty("place_id")
        private String placeId;

        @JsonProperty("name")
        private String name;
    }
}