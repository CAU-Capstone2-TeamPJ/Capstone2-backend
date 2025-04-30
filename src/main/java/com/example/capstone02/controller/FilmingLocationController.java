package com.example.capstone02.controller;

import com.example.capstone02.entity.FilmingLocation;
import com.example.capstone02.repository.FilmingLocationRepository;
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
@RequestMapping("/api/filming-locations")
@Tag(name = "촬영지 관리", description = "촬영지 정보 관리 API")
public class FilmingLocationController {

    private final FilmingLocationRepository locationRepository;

    @Autowired
    public FilmingLocationController(FilmingLocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    @Operation(summary = "모든 촬영지 조회", description = "시스템에 등록된 모든 촬영지 정보를 조회합니다.")
    @GetMapping
    public ResponseEntity<List<FilmingLocation>> getAllLocations() {
        return ResponseEntity.ok(locationRepository.findAll());
    }

    @Operation(summary = "촬영지 상세 조회", description = "ID로 특정 촬영지 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "촬영지 조회 성공",
                    content = @Content(schema = @Schema(implementation = FilmingLocation.class))),
            @ApiResponse(responseCode = "404", description = "촬영지를 찾을 수 없음", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<FilmingLocation> getLocationById(
            @Parameter(description = "촬영지 ID", required = true) @PathVariable Long id) {
        return locationRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "촬영지 등록", description = "새로운 촬영지 정보를 등록합니다.")
    @PostMapping
    public ResponseEntity<FilmingLocation> createLocation(
            @RequestBody FilmingLocation location) {
        return ResponseEntity.ok(locationRepository.save(location));
    }

    @Operation(summary = "촬영지 정보 수정", description = "기존 촬영지 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<FilmingLocation> updateLocation(
            @Parameter(description = "촬영지 ID", required = true) @PathVariable Long id,
            @RequestBody FilmingLocation locationDetails) {

        return locationRepository.findById(id)
                .map(location -> {
                    location.setName(locationDetails.getName());
                    location.setAddress(locationDetails.getAddress());
                    location.setLatitude(locationDetails.getLatitude());
                    location.setLongitude(locationDetails.getLongitude());
                    location.setMovie(locationDetails.getMovie());
                    return ResponseEntity.ok(locationRepository.save(location));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "촬영지 삭제", description = "촬영지 정보를 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLocation(
            @Parameter(description = "촬영지 ID", required = true) @PathVariable Long id) {

        return locationRepository.findById(id)
                .map(location -> {
                    locationRepository.delete(location);
                    return ResponseEntity.ok().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "영화별 촬영지 목록 조회", description = "특정 영화의 모든 촬영지 정보를 조회합니다.")
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<FilmingLocation>> getLocationsByMovie(
            @Parameter(description = "영화 ID", required = true) @PathVariable Long movieId) {

        List<FilmingLocation> locations = locationRepository.findByMovieId(movieId);
        return ResponseEntity.ok(locations);
    }
}