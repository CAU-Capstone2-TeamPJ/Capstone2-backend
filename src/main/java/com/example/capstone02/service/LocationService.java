package com.example.capstone02.service;

import com.example.capstone02.dto.LocationDistanceDTO;
import com.example.capstone02.entity.FilmingLocation;
import com.example.capstone02.repository.FilmingLocationRepository;
import com.example.capstone02.util.GoogleDistanceMatrixService;
import com.google.maps.model.LatLng;
import com.google.maps.model.TravelMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class LocationService {

    private final FilmingLocationRepository locationRepository;
    private final GoogleDistanceMatrixService distanceMatrixService;

    @Autowired
    public LocationService(FilmingLocationRepository locationRepository,
                           GoogleDistanceMatrixService distanceMatrixService) {
        this.locationRepository = locationRepository;
        this.distanceMatrixService = distanceMatrixService;
    }

    /**
     * 두 촬영지 간의 거리 계산
     */
    public LocationDistanceDTO calculateDistanceBetweenLocations(Long locationId1, Long locationId2, TravelMode travelMode) throws Exception {
        FilmingLocation location1 = locationRepository.findById(locationId1)
                .orElseThrow(() -> new RuntimeException("촬영지를 찾을 수 없습니다. ID: " + locationId1));

        FilmingLocation location2 = locationRepository.findById(locationId2)
                .orElseThrow(() -> new RuntimeException("촬영지를 찾을 수 없습니다. ID: " + locationId2));

        LatLng origin = new LatLng(location1.getLatitude(), location1.getLongitude());
        LatLng destination = new LatLng(location2.getLatitude(), location2.getLongitude());

        long distanceInMeters = distanceMatrixService.calculateDistance(origin, destination, travelMode);

        return new LocationDistanceDTO(
                location1.getId(),
                location1.getName(),
                location2.getId(),
                location2.getName(),
                distanceInMeters,
                travelMode
        );
    }

    /**
     * 특정 영화의 모든 촬영지 간의 거리 행렬 계산
     */
    public List<LocationDistanceDTO> calculateDistanceMatrixForMovie(Long movieId, TravelMode travelMode) throws Exception {
        List<FilmingLocation> locations = locationRepository.findByMovieId(movieId);

        if (locations.isEmpty()) {
            throw new RuntimeException("해당 영화의 촬영지가 없습니다. Movie ID: " + movieId);
        }

        List<LatLng> latLngs = new ArrayList<>();
        for (FilmingLocation location : locations) {
            latLngs.add(new LatLng(location.getLatitude(), location.getLongitude()));
        }

        long[][] distanceMatrix = distanceMatrixService.calculateDistanceMatrix(latLngs, travelMode);

        List<LocationDistanceDTO> result = new ArrayList<>();
        for (int i = 0; i < locations.size(); i++) {
            for (int j = 0; j < locations.size(); j++) {
                if (i != j) { // 자기 자신과의 거리는 제외
                    result.add(new LocationDistanceDTO(
                            locations.get(i).getId(),
                            locations.get(i).getName(),
                            locations.get(j).getId(),
                            locations.get(j).getName(),
                            distanceMatrix[i][j],
                            travelMode
                    ));
                }
            }
        }

        return result;
    }
}