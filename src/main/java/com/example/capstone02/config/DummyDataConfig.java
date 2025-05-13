//package com.example.capstone02.config;
//
//import com.example.capstone02.entity.FilmingLocation;
//import com.example.capstone02.entity.Movie;
//import com.example.capstone02.repository.FilmingLocationRepository;
//import com.example.capstone02.repository.MovieRepository;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Profile;
//
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Configuration
//@RequiredArgsConstructor
//@Slf4j
//public class DummyDataConfig {
//
//    private final MovieRepository movieRepository;
//    private final FilmingLocationRepository filmingLocationRepository;
//    private final Random random = new Random();
//
//    // 추천 키워드 목록
//    private final List<String> RECOMMENDATION_KEYWORDS = Arrays.asList(
//            "식당", "카페", "액티비티", "쇼핑", "자연", "힐링", "먹방", "관광", "문화", "예술", "역사", "공포", "포토스팟", "야경"
//    );
//
//    // 주변 키워드 랜덤 생성을 위한 목록
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
//            // 서울 촬영지 5개 생성
//            List<FilmingLocation> seoulLocations = Arrays.asList(
//                    createFilmingLocation(movie, "자하문터널", "대한민국",
//                            "영화 오프닝과 클라이막스 장면이 촬영된 터널. 기우가 자신의 미래를 생각하며 걷던 장소.",
//                            "서울특별시 종로구 자하문로", 0.85, 1275, 0.5,
//                            getRandomRecommendationKeywords(2), getRandomNearbyKeywords(3)),
//
//                    createFilmingLocation(movie, "스카이피자", "대한민국",
//                            "기우가 친구 민혁을 만나 취직 제안을 받은 피자집. 동네 곳곳을 내려다볼 수 있는 위치에 있다.",
//                            "서울특별시 종로구 창신동 독서당로 219", 0.72, 1080, 1.2,
//                            getRandomRecommendationKeywords(3), getRandomNearbyKeywords(3)),
//
//                    createFilmingLocation(movie, "건국대학교", "대한민국",
//                            "민혁의 대학 장면이 촬영된 곳. 박 사장의 딸 다혜에게 영어 과외를 추천하는 장면도 이곳에서 촬영되었다.",
//                            "서울특별시 광진구 능동로 120", 0.65, 975, 2.0,
//                            getRandomRecommendationKeywords(1), getRandomNearbyKeywords(3)),
//
//                    createFilmingLocation(movie, "현대아파트", "대한민국",
//                            "기정이 제시카 역할로 다혜의 미술 선생님이 되기 위해 면접을 보는 장소. 고급 주택가를 상징한다.",
//                            "서울특별시 강남구 압구정동 369", 0.78, 1170, 0.8,
//                            getRandomRecommendationKeywords(2), getRandomNearbyKeywords(3)),
//
//                    createFilmingLocation(movie, "한강공원", "대한민국",
//                            "대홍수 후 김 가족이 체육관으로 대피하는 장면에서 나오는 공원. 침수된 집을 떠나 피난처를 찾는 모습이 촬영되었다.",
//                            "서울특별시 용산구 이촌동 한강로", 0.61, 915, 1.5,
//                            getRandomRecommendationKeywords(3), getRandomNearbyKeywords(3))
//            );
//
//            // 부산 촬영지 5개 생성
//            List<FilmingLocation> busanLocations = Arrays.asList(
//                    createFilmingLocation(movie, "영도대교", "대한민국",
//                            "김 가족이 폭우 속에서 도시를 지나는 장면. 도시의 급격한 지형 차이를 보여주는 상징적 장소.",
//                            "부산광역시 영도구 영도대교", 0.73, 1095, 0.7,
//                            getRandomRecommendationKeywords(1), getRandomNearbyKeywords(3)),
//
//                    createFilmingLocation(movie, "감천문화마을", "대한민국",
//                            "김 가족이 사는 반지하 주택가를 연상시키는 장소. 계단식 주택가의 모습이 영화의 계층 상징과 유사하다.",
//                            "부산광역시 사하구 감내2로 203", 0.82, 1230, 2.5,
//                            getRandomRecommendationKeywords(4), getRandomNearbyKeywords(3)),
//
//                    createFilmingLocation(movie, "광안리해수욕장", "대한민국",
//                            "홍수 장면의 넓은 물을 촬영한 배경 중 하나. 도시가 물에 잠기는 모습을 표현하는 데 활용되었다.",
//                            "부산광역시 수영구 광안해변로 219", 0.58, 870, 3.0,
//                            getRandomRecommendationKeywords(3), getRandomNearbyKeywords(3)),
//
//                    createFilmingLocation(movie, "부산시민공원", "대한민국",
//                            "박 사장 가족이 야외에서 캠핑하는 장면의 배경. 넓은 잔디밭과 깔끔한 환경이 부유층의 여유를 상징한다.",
//                            "부산광역시 부산진구 시민공원로 73", 0.67, 1005, 2.0,
//                            getRandomRecommendationKeywords(2), getRandomNearbyKeywords(3)),
//
//                    createFilmingLocation(movie, "해운대 더베이 101", "대한민국",
//                            "박 사장이 회사 동료들과 만남을 가지는 고급 레스토랑 장면. 해운대의 화려한 야경이 부의 상징으로 나타난다.",
//                            "부산광역시 해운대구 동백로 52", 0.70, 1050, 1.8,
//                            getRandomRecommendationKeywords(3), getRandomNearbyKeywords(3))
//            );
//
//            // 세종 촬영지 6개 생성
//            List<FilmingLocation> sejongLocations = Arrays.asList(
//                    createFilmingLocation(movie, "세종호수공원", "대한민국",
//                            "박 사장 가족이 여가 시간을 보내는 공원 장면. 넓은 호수와 정돈된 환경이 특징이다.",
//                            "세종특별자치시 세종로 3238", 0.64, 960, 1.5,
//                            getRandomRecommendationKeywords(3), getRandomNearbyKeywords(3)),
//
//                    createFilmingLocation(movie, "세종 정부청사", "대한민국",
//                            "기우가 취업 면접을 보러 가는 장면의 배경. 현대적인 건물이 권위와 성공을 상징한다.",
//                            "세종특별자치시 도움5로 20", 0.59, 885, 1.0,
//                            getRandomRecommendationKeywords(1), getRandomNearbyKeywords(3)),
//
//                    createFilmingLocation(movie, "세종 주상복합단지", "대한민국",
//                            "박 사장 가족이 사는 고급 주택을 외부에서 촬영한 장소. 수직적 구조가 사회 계층을 상징한다.",
//                            "세종특별자치시 갈매로 363", 0.83, 1245, 0.8,
//                            getRandomRecommendationKeywords(2), getRandomNearbyKeywords(3)),
//
//                    createFilmingLocation(movie, "금강 세종보", "대한민국",
//                            "홍수 장면 일부가 촬영된 장소. 수문이 열리며 물이 쏟아지는 모습이 도시를 집어삼키는 홍수를 표현한다.",
//                            "세종특별자치시 연기면 세종리", 0.68, 1020, 1.2,
//                            getRandomRecommendationKeywords(2), getRandomNearbyKeywords(3)),
//
//                    createFilmingLocation(movie, "세종 중앙공원", "대한민국",
//                            "다송이 생일 파티 장면의 야외 촬영 장소. 넓은 잔디밭과 깔끔한 공간이 부유한 가정의 생활을 보여준다.",
//                            "세종특별자치시 보듬3로 92", 0.76, 1140, 2.5,
//                            getRandomRecommendationKeywords(3), getRandomNearbyKeywords(3)),
//
//                    createFilmingLocation(movie, "세종 첫마을", "대한민국",
//                            "김 가족이 집을 잃은 후 임시로 머무는 대피소 장면. 도시 계획에 따라 정리된 공간이 난민이 된 가족과 대비된다.",
//                            "세종특별자치시 한누리대로 194", 0.63, 945, 1.0,
//                            getRandomRecommendationKeywords(2), getRandomNearbyKeywords(3))
//            );
//
//            // 모든 촬영지 저장
//            filmingLocationRepository.saveAll(seoulLocations);
//            filmingLocationRepository.saveAll(busanLocations);
//            filmingLocationRepository.saveAll(sejongLocations);
//
//            log.info("영화 '{}'에 대한 16개의 촬영지 더미 데이터가 생성되었습니다.", movie.getTitle());
//            log.info("- 서울: {} 개", seoulLocations.size());
//            log.info("- 부산: {} 개", busanLocations.size());
//            log.info("- 세종: {} 개", sejongLocations.size());
//        };
//    }
//
//    /**
//     * 촬영지 객체 생성 헬퍼 메서드
//     */
//    private FilmingLocation createFilmingLocation(
//            Movie movie, String name, String country, String description,
//            String address, double mentionRate, int mentionCount, double durationTime,
//            List<String> recommendationKeywords, List<String> nearbyKeywords) {
//
//        FilmingLocation location = FilmingLocation.builder()
//                .movie(movie)
//                .name(name)
//                .country(country)
//                .description(description)
//                .address(address)
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
//     * 랜덤 추천 키워드 생성
//     */
//    private List<String> getRandomRecommendationKeywords(int count) {
//        if (count <= 0) return Collections.emptyList();
//        if (count > RECOMMENDATION_KEYWORDS.size()) count = RECOMMENDATION_KEYWORDS.size();
//
//        // 중복 없이 랜덤하게 count개 선택
//        List<String> shuffled = new ArrayList<>(RECOMMENDATION_KEYWORDS);
//        Collections.shuffle(shuffled, random);
//        return shuffled.subList(0, count);
//    }
//
//    /**
//     * 랜덤 주변 키워드 생성
//     */
//    private List<String> getRandomNearbyKeywords(int count) {
//        if (count <= 0) return Collections.emptyList();
//        if (count > NEARBY_KEYWORDS.size()) count = NEARBY_KEYWORDS.size();
//
//        // 중복 없이 랜덤하게 count개 선택
//        List<String> shuffled = new ArrayList<>(NEARBY_KEYWORDS);
//        Collections.shuffle(shuffled, random);
//        return shuffled.subList(0, count);
//    }
//}