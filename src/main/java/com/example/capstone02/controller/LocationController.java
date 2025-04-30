package com.example.capstone02.controller;

import com.example.capstone02.dto.LocationDistanceDTO;
import com.example.capstone02.service.LocationService;
import com.google.maps.model.TravelMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
public class LocationController {

    private final LocationService locationService;

    @Autowired
    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    /**
     * 두 촬영지 간의 거리 계산
     */
    @GetMapping("/distance")
    public ResponseEntity<LocationDistanceDTO> getDistanceBetweenLocations(
            @RequestParam Long from,
            @RequestParam Long to,
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
    @GetMapping("/distance/movie/{movieId}")
    public ResponseEntity<List<LocationDistanceDTO>> getDistanceMatrixForMovie(
            @PathVariable Long movieId,
            @RequestParam(defaultValue = "DRIVING") String travelMode) {

        try {
            TravelMode mode = TravelMode.valueOf(travelMode.toUpperCase());
            List<LocationDistanceDTO> distances = locationService.calculateDistanceMatrixForMovie(movieId, mode);
            return ResponseEntity.ok(distances);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}