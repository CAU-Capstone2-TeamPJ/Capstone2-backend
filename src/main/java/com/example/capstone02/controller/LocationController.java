package com.example.capstone02.controller;

import com.example.capstone02.dto.LocationDistanceDTO;
import com.example.capstone02.service.LocationService;
import com.google.maps.model.TravelMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@Tag(name = "촬영지 위치", description = "촬영지 위치 정보 및 거리 계산 API")
public class LocationController {

    private final LocationService locationService;

    @Autowired
    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    /**
     * 두 촬영지 간의 거리 계산
     */
    @Operation(summary = "두 촬영지 간의 거리 계산",
            description = "출발지와 목적지 촬영지 ID 및 이동 수단을 기준으로 거리를 계산합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "거리 계산 성공",
                    content = @Content(schema = @Schema(implementation = LocationDistanceDTO.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 계산 실패", content = @Content)
    })
    @GetMapping("/distance")
    public ResponseEntity<LocationDistanceDTO> getDistanceBetweenLocations(
            @Parameter(description = "출발지 촬영지 ID", required = true) @RequestParam Long from,
            @Parameter(description = "목적지 촬영지 ID", required = true) @RequestParam Long to,
            @Parameter(description = "이동 수단 (DRIVING, WALKING, BICYCLING, TRANSIT)",
                    schema = @Schema(allowableValues = {"DRIVING", "WALKING", "BICYCLING", "TRANSIT"}))
            @RequestParam(defaultValue = "DRIVING") String travelMode) {

        try {
            TravelMode mode = TravelMode.valueOf(travelMode.toUpperCase());
            LocationDistanceDTO distance = locationService.calculateDistanceBetweenLocations(from, to, mode);
            return ResponseEntity.ok(distance);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 특정 영화의 모든 촬영지 간의 거리 행렬 계산
     */
    @Operation(summary = "영화의 모든 촬영지 간 거리 행렬 계산",
            description = "특정 영화의 모든 촬영지 간의 거리 행렬을 계산합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "거리 행렬 계산 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 또는 계산 실패", content = @Content)
    })
    @GetMapping("/distance/movie/{movieId}")
    public ResponseEntity<List<LocationDistanceDTO>> getDistanceMatrixForMovie(
            @Parameter(description = "영화 ID", required = true) @PathVariable Long movieId,
            @Parameter(description = "이동 수단 (DRIVING, WALKING, BICYCLING, TRANSIT)",
                    schema = @Schema(allowableValues = {"DRIVING", "WALKING", "BICYCLING", "TRANSIT"}))
            @RequestParam(defaultValue = "DRIVING") String travelMode) {

        try {
            TravelMode mode = TravelMode.valueOf(travelMode.toUpperCase());
            List<LocationDistanceDTO> distances = locationService.calculateDistanceMatrixForMovie(movieId, mode);
            return ResponseEntity.ok(distances);
        } catch (Exception e) {
            e.printStackTrace(); // or log.error("거리 계산 실패", e);
            return ResponseEntity.badRequest().build();
        }
    }
}