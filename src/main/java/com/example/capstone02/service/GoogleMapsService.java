package com.example.capstone02.service;

import com.example.capstone02.dto.PlaceSearchResultDto;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.PlacesApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;
import com.google.maps.model.PlaceType;
import com.google.maps.model.PlacesSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleMapsService {

    private final WebClient webClient;

    @Value("${google.maps.api-key}")
    private String apiKey;

    // GeoApiContext 빈 생성
    private GeoApiContext getGeoContext() {
        return new GeoApiContext.Builder()
                .apiKey(apiKey)
                .build();
    }

    /**
     * 주소로부터 위도/경도 정보 가져오기
     */
    public LatLng getGeocode(String address) {
        try {
            GeoApiContext context = getGeoContext();
            GeocodingResult[] results = GeocodingApi.geocode(context, address).await();
            context.shutdown();

            if (results.length > 0) {
                return results[0].geometry.location;
            }

            log.warn("주소 '{}' 의 좌표를 찾을 수 없습니다.", address);
            return null;
        } catch (ApiException | InterruptedException | IOException e) {
            log.error("지오코딩 API 에러: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 위도/경도 기준으로 주변 이미지 가져오기
     */
    public List<String> getLocationImages(double latitude, double longitude) {
        try {
            List<String> imageUrls = new ArrayList<>();

            // Place Photos API 사용하기
            String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";

            String finalUrl = url + "?location=" + latitude + "," + longitude +
                    "&radius=500" +  // 500m 반경
                    "&key=" + apiKey;

            // WebClient로 Google Places API 호출
            PlaceSearchResultDto result = webClient.get()
                    .uri(finalUrl)
                    .retrieve()
                    .bodyToMono(PlaceSearchResultDto.class)
                    .block();

            if (result != null && result.getResults() != null) {
                // 최대 10개 이미지만 가져오기
                result.getResults().stream()
                        .filter(place -> place.getPhotos() != null && !place.getPhotos().isEmpty())
                        .limit(10)
                        .forEach(place -> {
                            String photoReference = place.getPhotos().get(0).getPhotoReference();
                            String photoUrl = "https://maps.googleapis.com/maps/api/place/photo"
                                    + "?maxwidth=1200"
                                    + "&photo_reference=" + photoReference
                                    + "&key=" + apiKey;
                            imageUrls.add(photoUrl);
                        });
            }

            return imageUrls;
        } catch (Exception e) {
            log.error("이미지 가져오기 에러: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 키워드로 장소 검색 (최대 2개)
     */
    public List<String> searchPlacesByKeyword(double latitude, double longitude, String keyword) {
        try {
            GeoApiContext context = getGeoContext();
            LatLng location = new LatLng(latitude, longitude);

            PlacesSearchResponse placesResponse = PlacesApi.textSearchQuery(context, keyword)
                    .location(location)
                    .radius(1000) // 1km 반경
                    .await();

            context.shutdown();

            // 최대 2개 장소 ID만 반환
            return Arrays.stream(placesResponse.results)
                    .limit(2)
                    .map(place -> place.placeId)
                    .collect(Collectors.toList());

        } catch (ApiException | InterruptedException | IOException e) {
            log.error("장소 검색 API 에러 (키워드: {}): {}", keyword, e.getMessage());
            return List.of();
        }
    }
}