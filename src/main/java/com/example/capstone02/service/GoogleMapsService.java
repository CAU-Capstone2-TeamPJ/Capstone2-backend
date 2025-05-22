package com.example.capstone02.service;

import com.example.capstone02.dto.PlaceDetailsDto;
import com.example.capstone02.dto.PlaceDetailsResponseDto;
import com.example.capstone02.dto.PlaceSearchResultDto;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.PlacesApi;
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import com.google.maps.model.LatLng;

import com.google.maps.model.PlacesSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
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
            return generateDummyCoordinates(address);

        } catch (ApiException | InterruptedException | IOException e) {
            log.error("지오코딩 API 에러: {}", e.getMessage());
            return generateDummyCoordinates(address);
        }
    }

    /**
     * 특정 장소의 이미지 가져오기 (도로명 주소, 위도/경도, 장소명 사용)
     */
    public List<String> getSpecificPlaceImages(String address, Double latitude, Double longitude, String placeName) {
        try {
            List<String> imageUrls = new ArrayList<>();
            String placeId = null;

            log.info("특정 장소 이미지 검색 시작 - 장소명: {}, 주소: {}, 좌표: ({}, {})",
                    placeName, address, latitude, longitude);

            // 1단계: 장소명과 좌표를 이용해 특정 장소 검색
            if (placeName != null && !placeName.trim().isEmpty() && latitude != null && longitude != null) {
                placeId = findPlaceByNameAndLocation(placeName, latitude, longitude);
                log.info("장소명+좌표 검색 결과 Place ID: {}", placeId);
            }

            // 2단계: 장소명으로 찾지 못했다면 주소로 검색
            if (placeId == null && address != null && !address.trim().isEmpty()) {
                placeId = findPlaceByAddress(address);
                log.info("주소 검색 결과 Place ID: {}", placeId);
            }

            // 3단계: 좌표만으로 주변 검색 (장소명을 키워드로 활용)
            if (placeId == null && latitude != null && longitude != null) {
                placeId = findPlaceByCoordinatesAndKeyword(latitude, longitude, placeName);
                log.info("좌표+키워드 검색 결과 Place ID: {}", placeId);
            }

            // 4단계: Place ID로 장소 상세 정보 및 이미지 가져오기
            if (placeId != null) {
                imageUrls = getPlaceImagesByPlaceId(placeId);
                log.info("Place ID {}로부터 {}개 이미지 획득", placeId, imageUrls.size());
            }

            // 5단계: 특정 장소 이미지가 없으면 주변 이미지로 대체
            if (imageUrls.isEmpty() && latitude != null && longitude != null) {
                log.info("특정 장소 이미지가 없어 주변 이미지로 대체합니다. 장소: {}", placeName);
                imageUrls = getNearbyPlaceImages(latitude, longitude);
                log.info("주변 장소에서 {}개 이미지 획득", imageUrls.size());
            }

            // 6단계: 그래도 없으면 더미 이미지
            if (imageUrls.isEmpty()) {
                imageUrls = generateDummyImageUrls(latitude, longitude, placeName);
                log.info("더미 이미지 {}개 생성", imageUrls.size());
            }

            log.info("최종 결과: {}개 이미지 반환", imageUrls.size());
            return imageUrls;

        } catch (Exception e) {
            log.error("특정 장소 이미지 가져오기 에러: {}", e.getMessage(), e);
            return generateDummyImageUrls(latitude, longitude, placeName);
        }
    }

    /**
     * 장소명과 좌표로 특정 장소 찾기
     */
    private String findPlaceByNameAndLocation(String placeName, double latitude, double longitude) {
        try {
            String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";
            String finalUrl = url + "?location=" + latitude + "," + longitude +
                    "&radius=100" +  // 100m 반경으로 좁게 검색
                    "&keyword=" + placeName +
                    "&key=" + apiKey;

            log.debug("장소명+좌표 검색 URL: {}", finalUrl);

            PlaceSearchResultDto result = webClient.get()
                    .uri(finalUrl)
                    .retrieve()
                    .bodyToMono(PlaceSearchResultDto.class)
                    .block();

            if (result != null && result.getResults() != null && !result.getResults().isEmpty()) {
                String foundPlaceId = result.getResults().get(0).getPlaceId();
                log.debug("장소명+좌표 검색 성공: Place ID = {}", foundPlaceId);
                return foundPlaceId;
            }

        } catch (Exception e) {
            log.error("장소명으로 검색 실패: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 주소로 특정 장소 찾기
     */
    private String findPlaceByAddress(String address) {
        try {
            String url = "https://maps.googleapis.com/maps/api/place/findplacefromtext/json";
            String finalUrl = url + "?input=" + address +
                    "&inputtype=textquery" +
                    "&fields=place_id" +
                    "&key=" + apiKey;

            log.debug("주소 검색 URL: {}", finalUrl);

            PlaceDetailsDto response = webClient.get()
                    .uri(finalUrl)
                    .retrieve()
                    .bodyToMono(PlaceDetailsDto.class)
                    .block();

            if (response != null && response.getCandidates() != null && !response.getCandidates().isEmpty()) {
                String foundPlaceId = response.getCandidates().get(0).getPlaceId();
                log.debug("주소 검색 성공: Place ID = {}", foundPlaceId);
                return foundPlaceId;
            }

        } catch (Exception e) {
            log.error("주소로 검색 실패: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 좌표와 키워드로 특정 장소 찾기
     */
    private String findPlaceByCoordinatesAndKeyword(double latitude, double longitude, String keyword) {
        try {
            String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";
            String finalUrl = url + "?location=" + latitude + "," + longitude +
                    "&radius=500";  // 500m 반경으로 확장

            if (keyword != null && !keyword.trim().isEmpty()) {
                finalUrl += "&keyword=" + keyword;
            }

            finalUrl += "&key=" + apiKey;

            log.debug("좌표+키워드 검색 URL: {}", finalUrl);

            PlaceSearchResultDto result = webClient.get()
                    .uri(finalUrl)
                    .retrieve()
                    .bodyToMono(PlaceSearchResultDto.class)
                    .block();

            if (result != null && result.getResults() != null && !result.getResults().isEmpty()) {
                String foundPlaceId = result.getResults().get(0).getPlaceId();
                log.debug("좌표+키워드 검색 성공: Place ID = {}", foundPlaceId);
                return foundPlaceId;
            }

        } catch (Exception e) {
            log.error("좌표+키워드로 검색 실패: {}", e.getMessage());
        }

        return null;
    }

    /**
     * Place ID로 장소의 이미지들 가져오기 (WebClient 사용)
     */
    private List<String> getPlaceImagesByPlaceId(String placeId) {
        try {
            String url = "https://maps.googleapis.com/maps/api/place/details/json";
            String finalUrl = url + "?place_id=" + placeId +
                    "&fields=photos" +
                    "&key=" + apiKey;

            log.debug("Place Details API 호출 URL: {}", finalUrl);

            PlaceDetailsResponseDto response = webClient.get()
                    .uri(finalUrl)
                    .retrieve()
                    .bodyToMono(PlaceDetailsResponseDto.class)
                    .block();

            List<String> imageUrls = new ArrayList<>();

            if (response != null && response.getResult() != null &&
                    response.getResult().getPhotos() != null && !response.getResult().getPhotos().isEmpty()) {

                // 최대 10개 이미지 가져오기
                int photoCount = Math.min(response.getResult().getPhotos().size(), 10);

                for (int i = 0; i < photoCount; i++) {
                    String photoReference = response.getResult().getPhotos().get(i).getPhotoReference();
                    String photoUrl = "https://maps.googleapis.com/maps/api/place/photo"
                            + "?maxwidth=1200"
                            + "&photo_reference=" + photoReference
                            + "&key=" + apiKey;
                    imageUrls.add(photoUrl);
                }

                log.debug("Place ID {}에서 {}개 이미지 URL 생성", placeId, imageUrls.size());
            } else {
                log.debug("Place ID {}에 사진이 없습니다.", placeId);
            }

            return imageUrls;

        } catch (Exception e) {
            log.error("Place ID로 이미지 가져오기 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 주변 장소 이미지 가져오기 (fallback용)
     */
    private List<String> getNearbyPlaceImages(double latitude, double longitude) {
        try {
            List<String> imageUrls = new ArrayList<>();
            String url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json";

            String finalUrl = url + "?location=" + latitude + "," + longitude +
                    "&radius=200" +  // 200m 반경
                    "&key=" + apiKey;

            log.debug("주변 장소 이미지 검색 URL: {}", finalUrl);

            PlaceSearchResultDto result = webClient.get()
                    .uri(finalUrl)
                    .retrieve()
                    .bodyToMono(PlaceSearchResultDto.class)
                    .block();

            if (result != null && result.getResults() != null) {
                result.getResults().stream()
                        .filter(place -> place.getPhotos() != null && !place.getPhotos().isEmpty())
                        .limit(5)  // 주변 장소는 5개만
                        .forEach(place -> {
                            String photoReference = place.getPhotos().get(0).getPhotoReference();
                            String photoUrl = "https://maps.googleapis.com/maps/api/place/photo"
                                    + "?maxwidth=1200"
                                    + "&photo_reference=" + photoReference
                                    + "&key=" + apiKey;
                            imageUrls.add(photoUrl);
                        });

                log.debug("주변 장소에서 {}개 이미지 URL 생성", imageUrls.size());
            }

            return imageUrls;
        } catch (Exception e) {
            log.error("주변 장소 이미지 가져오기 에러: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 더미 이미지 URL들 생성
     */
    private List<String> generateDummyImageUrls(Double latitude, Double longitude, String placeName) {
        List<String> dummyUrls = new ArrayList<>();
        String locationId = "unknown";

        if (latitude != null && longitude != null) {
            locationId = String.format("%.4f_%.4f", latitude, longitude).replace(".", "_");
        }

        String placeId = placeName != null ? placeName.replaceAll("[^a-zA-Z0-9가-힣]", "_") : "unknown";

        for (int i = 0; i < 5; i++) {
            dummyUrls.add("https://example.com/images/place_" + locationId + "_" + placeId + "_" + i + ".jpg");
        }

        log.info("더미 이미지 생성: 장소 '{}' -> {} 개 이미지", placeName, dummyUrls.size());
        return dummyUrls;
    }

    /**
     * 더미 좌표 생성
     */
    private LatLng generateDummyCoordinates(String address) {
        double baseLat = 37.5665;
        double baseLng = 126.9780;

        int hashCode = Math.abs(address.hashCode());
        double latOffset = (hashCode % 100) * 0.001;
        double lngOffset = ((hashCode / 100) % 100) * 0.001;

        latOffset = (hashCode % 2 == 0) ? latOffset : -latOffset;
        lngOffset = ((hashCode / 10) % 2 == 0) ? lngOffset : -lngOffset;

        log.info("더미 좌표 생성: 주소 '{}' -> 위도 {}, 경도 {}",
                address, baseLat + latOffset, baseLng + lngOffset);

        return new LatLng(baseLat + latOffset, baseLng + lngOffset);
    }

    /**
     * 위도/경도 기준으로 주변 이미지 가져오기 (기존 메서드 유지 - 호환성)
     */
    @Deprecated
    public List<String> getLocationImages(double latitude, double longitude) {
        return getNearbyPlaceImages(latitude, longitude);
    }

    /**
     * 키워드로 장소 검색 (기존 메서드 유지)
     */
    public List<String> searchPlacesByKeyword(double latitude, double longitude, String keyword) {
        try {
            GeoApiContext context = getGeoContext();
            LatLng location = new LatLng(latitude, longitude);

            PlacesSearchResponse placesResponse = PlacesApi.textSearchQuery(context, keyword)
                    .location(location)
                    .radius(1000)
                    .await();

            context.shutdown();

            List<String> placeIds = Arrays.stream(placesResponse.results)
                    .limit(2)
                    .map(place -> place.placeId)
                    .collect(Collectors.toList());

            if (placeIds.isEmpty()) {
                placeIds = generateDummyPlaceIds(latitude, longitude, keyword);
            }

            return placeIds;

        } catch (ApiException | InterruptedException | IOException e) {
            log.error("장소 검색 API 에러 (키워드: {}): {}", keyword, e.getMessage());
            return generateDummyPlaceIds(latitude, longitude, keyword);
        }
    }

    private List<String> generateDummyPlaceIds(double latitude, double longitude, String keyword) {
        List<String> dummyIds = new ArrayList<>();
        String base = String.format("%.4f_%.4f_%s", latitude, longitude, keyword);
        String id1 = "dummy_place_" + UUID.nameUUIDFromBytes(base.getBytes()).toString().substring(0, 8);
        String id2 = "dummy_place_" + UUID.nameUUIDFromBytes((base + "_2").getBytes()).toString().substring(0, 8);

        dummyIds.add(id1);
        dummyIds.add(id2);

        log.info("더미 장소 ID 생성: 좌표({}, {}), 키워드 '{}' -> {}",
                latitude, longitude, keyword, dummyIds);

        return dummyIds;
    }
}