//package com.example.capstone02.config;
//
//import com.example.capstone02.entity.FilmingLocation;
//import com.example.capstone02.entity.Movie;
//import com.example.capstone02.repository.FilmingLocationRepository;
//import com.example.capstone02.repository.MovieRepository;
//import com.example.capstone02.service.GoogleMapsService;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Profile;
//
//import java.util.*;
//
//@Configuration
//@RequiredArgsConstructor
//@Slf4j
//public class DummyDataConfig {
//
//    private final MovieRepository movieRepository;
//    private final FilmingLocationRepository filmingLocationRepository;
//    private final GoogleMapsService googleMapsService;
//
//    // 추천 키워드 목록
//    private final List<String> RECOMMENDATION_KEYWORDS = Arrays.asList(
//            "식당", "카페", "액티비티", "쇼핑", "자연", "힐링", "먹방", "관광", "문화", "예술", "역사", "공포", "포토스팟", "야경"
//    );
//
//    // 주변 키워드 목록
//    private final List<String> NEARBY_KEYWORDS = Arrays.asList(
//            "주차장", "버스정류장", "지하철역", "공원", "편의점", "마트", "약국", "화장실", "은행", "ATM",
//            "카페거리", "맛집거리", "야시장", "백화점", "호텔", "숙박시설", "베이커리", "병원", "경찰서", "소방서",
//            "문화센터", "미술관", "박물관", "도서관", "영화관", "전시장", "공연장", "체육관", "수영장", "학교",
//            "대학교", "관광안내소", "기념품샵", "사진관", "잔디밭", "유치원", "주민센터", "마을회관", "터미널", "기차역"
//    );
//
//    /**
//     * 개발 환경에서만 더미 데이터 생성
//     * application.yml에 spring.profiles.active=dev로 설정시 활성화
//     */
//    @Bean
//    @Profile("dev")
//    public CommandLineRunner initDummyData() {
//        return args -> {
//            log.info("더미 데이터 생성 시작");
//
//            // 영화 ID 496243 조회 (기생충)
//            Optional<Movie> movieOpt = movieRepository.findById(496243L);
//
//            // 영화가 없으면 로그 출력 후 종료
//            if (movieOpt.isEmpty()) {
//                log.warn("영화 ID 496243을 찾을 수 없습니다. 먼저 영화 정보를 가져오세요.");
//                return;
//            }
//
//            Movie movie = movieOpt.get();
//
//            // 기존 촬영지 데이터가 있으면 삭제
//            if (filmingLocationRepository.existsByMovieId(movie.getId())) {
//                log.info("영화 ID {}의 기존 촬영지 정보를 삭제합니다", movie.getId());
//                filmingLocationRepository.deleteAllByMovieId(movie.getId());
//            }
//
//            // 서울 촬영지 2개 생성
//            List<FilmingLocation> seoulLocations = Arrays.asList(
//                    createFilmingLocation(movie, "자하문터널", "대한민국",
//                            "영화 오프닝과 클라이막스 장면이 촬영된 터널. 기우가 자신의 미래를 생각하며 걷던 장소.",
//                            "서울특별시 종로구 자하문로", 37.5842, 126.9682, 0.85, 1275, 0.5,
//                            Arrays.asList("관광", "포토스팟", "역사"),
//                            Arrays.asList("지하철역", "공원")),
//
//                    createFilmingLocation(movie, "현대아파트", "대한민국",
//                            "기정이 제시카 역할로 다혜의 미술 선생님이 되기 위해 면접을 보는 장소. 고급 주택가를 상징한다.",
//                            "서울특별시 강남구 압구정동 369", 37.5276, 127.0409, 0.78, 1170, 0.8,
//                            Arrays.asList("관광", "문화", "야경"),
//                            Arrays.asList("카페거리", "백화점"))
//            );
//
//            // 부산 촬영지 2개 생성
//            List<FilmingLocation> busanLocations = Arrays.asList(
//                    createFilmingLocation(movie, "영도대교", "대한민국",
//                            "김 가족이 폭우 속에서 도시를 지나는 장면. 도시의 급격한 지형 차이를 보여주는 상징적 장소.",
//                            "부산광역시 영도구 영도대교", 35.0975, 129.0403, 0.73, 1095, 0.7,
//                            Arrays.asList("포토스팟", "야경", "자연"),
//                            Arrays.asList("관광안내소", "기념품샵")),
//
//                    createFilmingLocation(movie, "해운대 더베이 101", "대한민국",
//                            "박 사장이 회사 동료들과 만남을 가지는 고급 레스토랑 장면. 해운대의 화려한 야경이 부의 상징으로 나타난다.",
//                            "부산광역시 해운대구 동백로 52", 35.1568, 129.1460, 0.70, 1050, 1.8,
//                            Arrays.asList("식당", "카페", "먹방"),
//                            Arrays.asList("호텔", "마트"))
//            );
//
//            // 뉴욕 촬영지 1개 생성 (가상)
//            List<FilmingLocation> nyLocations = Arrays.asList(
//                    createFilmingLocation(movie, "센트럴 파크", "미국",
//                            "기생충의 미국 프로모션을 위한 특별 장면이 촬영된 곳. 뉴욕의 상징적인 공원.",
//                            "Central Park, New York, NY 10022", 40.7812, -73.9665, 0.65, 975, 1.5,
//                            Arrays.asList("자연", "힐링", "액티비티"),
//                            Arrays.asList("박물관", "카페거리"))
//            );
//
//            // 모든 촬영지 저장
//            filmingLocationRepository.saveAll(seoulLocations);
//            filmingLocationRepository.saveAll(busanLocations);
//            filmingLocationRepository.saveAll(nyLocations);
//
//            log.info("영화 '{}'에 대한 5개의 촬영지 더미 데이터가 생성되었습니다.", movie.getTitle());
//            log.info("- 서울: {} 개", seoulLocations.size());
//            log.info("- 부산: {} 개", busanLocations.size());
//            log.info("- 뉴욕: {} 개", nyLocations.size());
//
//            // 각 촬영지의 주변 장소 ID 업데이트 (실제 API 호출은 제외하고 더미 데이터만 설정)
//            updateDummyPlaceIds(seoulLocations);
//            updateDummyPlaceIds(busanLocations);
//            updateDummyPlaceIds(nyLocations);
//
//            // 영화 이미지 더미 데이터 추가
//            updateDummyImages(seoulLocations);
//            updateDummyImages(busanLocations);
//            updateDummyImages(nyLocations);
//
//            // 저장
//            filmingLocationRepository.saveAll(seoulLocations);
//            filmingLocationRepository.saveAll(busanLocations);
//            filmingLocationRepository.saveAll(nyLocations);
//
//            log.info("촬영지 이미지 및 주변 장소 정보 업데이트 완료");
//        };
//    }
//
//    /**
//     * 촬영지 객체 생성 헬퍼 메서드
//     */
//    private FilmingLocation createFilmingLocation(
//            Movie movie, String name, String country, String description,
//            String address, Double latitude, Double longitude,
//            Double mentionRate, Integer mentionCount, Double durationTime,
//            List<String> recommendationKeywords, List<String> nearbyKeywords) {
//
//        FilmingLocation location = FilmingLocation.builder()
//                .movie(movie)
//                .name(name)
//                .country(country)
//                .description(description)
//                .address(address)
//                .latitude(latitude)
//                .longitude(longitude)
//                .mentionRate(mentionRate)
//                .mentionCount(mentionCount)
//                .durationTime(durationTime)
//                .build();
//
//        location.setRecommendationKeywords(recommendationKeywords);
//        location.setNearbyKeywords(nearbyKeywords);
//
//        return location;
//    }
//
//    /**
//     * 촬영지의 주변 장소 ID 더미 데이터 업데이트
//     */
//    private void updateDummyPlaceIds(List<FilmingLocation> locations) {
//        for (FilmingLocation location : locations) {
//            Map<String, String> nearbyPlaceIds = new HashMap<>();
//
//            // 각 주변 키워드에 대해 2개의 더미 장소 ID 생성
//            for (String keyword : location.getNearbyKeywords()) {
//                String placeId1 = "dummy_place_" + UUID.randomUUID().toString().substring(0, 8);
//                String placeId2 = "dummy_place_" + UUID.randomUUID().toString().substring(0, 8);
//                nearbyPlaceIds.put(keyword, placeId1 + "," + placeId2);
//            }
//
//            location.setNearbyPlaceIds(nearbyPlaceIds);
//        }
//    }
//
//    /**
//     * 촬영지 이미지 더미 데이터 업데이트
//     */
//    private void updateDummyImages(List<FilmingLocation> locations) {
//        for (FilmingLocation location : locations) {
//            List<String> images = new ArrayList<>();
//
//            // 10개의 더미 이미지 URL 생성
//            for (int i = 0; i < 10; i++) {
//                // 실제 이미지 URL 대신 더미 URL 사용
//                String imageUrl = "https://example.com/images/location_" +
//                        location.getId() + "_" + i + ".jpg";
//                images.add(imageUrl);
//            }
//
//            location.setImages(images);
//        }
//    }
//}