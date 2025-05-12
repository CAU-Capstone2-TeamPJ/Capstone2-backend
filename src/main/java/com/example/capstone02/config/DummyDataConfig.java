package com.example.capstone02.config;

import com.example.capstone02.entity.FilmingLocation;
import com.example.capstone02.entity.Movie;
import com.example.capstone02.repository.FilmingLocationRepository;
import com.example.capstone02.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DummyDataConfig {

    private final MovieRepository movieRepository;
    private final FilmingLocationRepository filmingLocationRepository;

    /**
     * 개발 환경에서만 더미 데이터 생성
     * application.yml에 spring.profiles.active=dev로 설정시 활성화
     */
    @Bean
    @Profile("dev")
    public CommandLineRunner initDummyData() {
        return args -> {
            log.info("더미 데이터 생성 시작");

            // 영화 ID 496243 조회 (기생충)
            Optional<Movie> movieOpt = movieRepository.findById(496243L);

            // 영화가 없으면 로그 출력 후 종료
            if (movieOpt.isEmpty()) {
                log.warn("영화 ID 496243을 찾을 수 없습니다. 먼저 영화 정보를 가져오세요.");
                return;
            }

            Movie movie = movieOpt.get();

            // 기존 촬영지 데이터가 있으면 삭제
            if (filmingLocationRepository.existsByMovieId(movie.getId())) {
                log.info("영화 ID {}의 기존 촬영지 정보를 삭제합니다", movie.getId());
                filmingLocationRepository.deleteAllByMovieId(movie.getId());
            }

            // 서울 촬영지 5개 생성
            List<FilmingLocation> seoulLocations = Arrays.asList(
                    FilmingLocation.builder()
                            .movie(movie)
                            .name("자하문터널")
                            .country("대한민국")
                            .description("영화 오프닝과 클라이막스 장면이 촬영된 터널. 기우가 자신의 미래를 생각하며 걷던 장소.")
                            .latitude(37.5834)
                            .longitude(126.9687)
                            .address("서울특별시 종로구 자하문로")
                            .mentionRate(0.85)
                            .mentionCount(1275)
                            .keywords(Arrays.asList("오프닝", "클라이막스", "상징적", "계단"))
                            .build(),

                    FilmingLocation.builder()
                            .movie(movie)
                            .name("스카이피자")
                            .country("대한민국")
                            .description("기우가 친구 민혁을 만나 취직 제안을 받은 피자집. 동네 곳곳을 내려다볼 수 있는 위치에 있다.")
                            .latitude(37.5822)
                            .longitude(126.9854)
                            .address("서울특별시 종로구 창신동 독서당로 219")
                            .mentionRate(0.72)
                            .mentionCount(1080)
                            .keywords(Arrays.asList("피자", "친구", "취직", "제안"))
                            .build(),

                    FilmingLocation.builder()
                            .movie(movie)
                            .name("건국대학교")
                            .country("대한민국")
                            .description("민혁의 대학 장면이 촬영된 곳. 박 사장의 딸 다혜에게 영어 과외를 추천하는 장면도 이곳에서 촬영되었다.")
                            .latitude(37.5412)
                            .longitude(127.0746)
                            .address("서울특별시 광진구 능동로 120")
                            .mentionRate(0.65)
                            .mentionCount(975)
                            .keywords(Arrays.asList("대학교", "과외", "추천", "학생"))
                            .build(),

                    FilmingLocation.builder()
                            .movie(movie)
                            .name("현대아파트")
                            .country("대한민국")
                            .description("기정이 제시카 역할로 다혜의 미술 선생님이 되기 위해 면접을 보는 장소. 고급 주택가를 상징한다.")
                            .latitude(37.5189)
                            .longitude(127.0047)
                            .address("서울특별시 강남구 압구정동 369")
                            .mentionRate(0.78)
                            .mentionCount(1170)
                            .keywords(Arrays.asList("고급", "아파트", "면접", "제시카"))
                            .build(),

                    FilmingLocation.builder()
                            .movie(movie)
                            .name("한강공원")
                            .country("대한민국")
                            .description("대홍수 후 김 가족이 체육관으로 대피하는 장면에서 나오는 공원. 침수된 집을 떠나 피난처를 찾는 모습이 촬영되었다.")
                            .latitude(37.5137)
                            .longitude(126.9742)
                            .address("서울특별시 용산구 이촌동 한강로")
                            .mentionRate(0.61)
                            .mentionCount(915)
                            .keywords(Arrays.asList("홍수", "피난", "재난", "빗물"))
                            .build()
            );

            // 부산 촬영지 5개 생성
            List<FilmingLocation> busanLocations = Arrays.asList(
                    FilmingLocation.builder()
                            .movie(movie)
                            .name("영도대교")
                            .country("대한민국")
                            .description("김 가족이 폭우 속에서 도시를 지나는 장면. 도시의 급격한 지형 차이를 보여주는 상징적 장소.")
                            .latitude(35.0968)
                            .longitude(129.0356)
                            .address("부산광역시 영도구 영도대교")
                            .mentionRate(0.73)
                            .mentionCount(1095)
                            .keywords(Arrays.asList("다리", "폭우", "이동", "지형차"))
                            .build(),

                    FilmingLocation.builder()
                            .movie(movie)
                            .name("감천문화마을")
                            .country("대한민국")
                            .description("김 가족이 사는 반지하 주택가를 연상시키는 장소. 계단식 주택가의 모습이 영화의 계층 상징과 유사하다.")
                            .latitude(35.0986)
                            .longitude(129.0109)
                            .address("부산광역시 사하구 감내2로 203")
                            .mentionRate(0.82)
                            .mentionCount(1230)
                            .keywords(Arrays.asList("계단", "비탈길", "반지하", "주택가"))
                            .build(),

                    FilmingLocation.builder()
                            .movie(movie)
                            .name("광안리해수욕장")
                            .country("대한민국")
                            .description("홍수 장면의 넓은 물을 촬영한 배경 중 하나. 도시가 물에 잠기는 모습을 표현하는 데 활용되었다.")
                            .latitude(35.1531)
                            .longitude(129.1185)
                            .address("부산광역시 수영구 광안해변로 219")
                            .mentionRate(0.58)
                            .mentionCount(870)
                            .keywords(Arrays.asList("바다", "홍수", "물", "도시"))
                            .build(),

                    FilmingLocation.builder()
                            .movie(movie)
                            .name("부산시민공원")
                            .country("대한민국")
                            .description("박 사장 가족이 야외에서 캠핑하는 장면의 배경. 넓은 잔디밭과 깔끔한 환경이 부유층의 여유를 상징한다.")
                            .latitude(35.1687)
                            .longitude(129.0577)
                            .address("부산광역시 부산진구 시민공원로 73")
                            .mentionRate(0.67)
                            .mentionCount(1005)
                            .keywords(Arrays.asList("공원", "캠핑", "여유", "잔디밭"))
                            .build(),

                    FilmingLocation.builder()
                            .movie(movie)
                            .name("해운대 더베이 101")
                            .country("대한민국")
                            .description("박 사장이 회사 동료들과 만남을 가지는 고급 레스토랑 장면. 해운대의 화려한 야경이 부의 상징으로 나타난다.")
                            .latitude(35.1560)
                            .longitude(129.1362)
                            .address("부산광역시 해운대구 동백로 52")
                            .mentionRate(0.70)
                            .mentionCount(1050)
                            .keywords(Arrays.asList("고급", "레스토랑", "야경", "부유층"))
                            .build()
            );

            // 세종 촬영지 6개 생성
            List<FilmingLocation> sejongLocations = Arrays.asList(
                    FilmingLocation.builder()
                            .movie(movie)
                            .name("세종호수공원")
                            .country("대한민국")
                            .description("박 사장 가족이 여가 시간을 보내는 공원 장면. 넓은 호수와 정돈된 환경이 특징이다.")
                            .latitude(36.5005)
                            .longitude(127.2564)
                            .address("세종특별자치시 세종로 3238")
                            .mentionRate(0.64)
                            .mentionCount(960)
                            .keywords(Arrays.asList("호수", "공원", "산책", "여가"))
                            .build(),

                    FilmingLocation.builder()
                            .movie(movie)
                            .name("세종 정부청사")
                            .country("대한민국")
                            .description("기우가 취업 면접을 보러 가는 장면의 배경. 현대적인 건물이 권위와 성공을 상징한다.")
                            .latitude(36.5045)
                            .longitude(127.2494)
                            .address("세종특별자치시 도움5로 20")
                            .mentionRate(0.59)
                            .mentionCount(885)
                            .keywords(Arrays.asList("면접", "정부", "권위", "현대건물"))
                            .build(),

                    FilmingLocation.builder()
                            .movie(movie)
                            .name("세종 주상복합단지")
                            .country("대한민국")
                            .description("박 사장 가족이 사는 고급 주택을 외부에서 촬영한 장소. 수직적 구조가 사회 계층을 상징한다.")
                            .latitude(36.5098)
                            .longitude(127.2612)
                            .address("세종특별자치시 갈매로 363")
                            .mentionRate(0.83)
                            .mentionCount(1245)
                            .keywords(Arrays.asList("고급", "주택", "아파트", "수직구조"))
                            .build(),

                    FilmingLocation.builder()
                            .movie(movie)
                            .name("금강 세종보")
                            .country("대한민국")
                            .description("홍수 장면 일부가 촬영된 장소. 수문이 열리며 물이 쏟아지는 모습이 도시를 집어삼키는 홍수를 표현한다.")
                            .latitude(36.4715)
                            .longitude(127.3003)
                            .address("세종특별자치시 연기면 세종리")
                            .mentionRate(0.68)
                            .mentionCount(1020)
                            .keywords(Arrays.asList("홍수", "댐", "물", "재난"))
                            .build(),

                    FilmingLocation.builder()
                            .movie(movie)
                            .name("세종 중앙공원")
                            .country("대한민국")
                            .description("다송이 생일 파티 장면의 야외 촬영 장소. 넓은 잔디밭과 깔끔한 공간이 부유한 가정의 생활을 보여준다.")
                            .latitude(36.5114)
                            .longitude(127.2463)
                            .address("세종특별자치시 보듬3로 92")
                            .mentionRate(0.76)
                            .mentionCount(1140)
                            .keywords(Arrays.asList("생일", "파티", "잔디밭", "가족"))
                            .build(),

                    FilmingLocation.builder()
                            .movie(movie)
                            .name("세종 첫마을")
                            .country("대한민국")
                            .description("김 가족이 집을 잃은 후 임시로 머무는 대피소 장면. 도시 계획에 따라 정리된 공간이 난민이 된 가족과 대비된다.")
                            .latitude(36.4817)
                            .longitude(127.2556)
                            .address("세종특별자치시 한누리대로 194")
                            .mentionRate(0.63)
                            .mentionCount(945)
                            .keywords(Arrays.asList("대피소", "난민", "임시거처", "홍수피해"))
                            .build()
            );

            // 모든 촬영지 저장
            filmingLocationRepository.saveAll(seoulLocations);
            filmingLocationRepository.saveAll(busanLocations);
            filmingLocationRepository.saveAll(sejongLocations);

            log.info("영화 '{}'에 대한 16개의 촬영지 더미 데이터가 생성되었습니다.", movie.getTitle());
            log.info("- 서울: {} 개", seoulLocations.size());
            log.info("- 부산: {} 개", busanLocations.size());
            log.info("- 세종: {} 개", sejongLocations.size());
        };
    }
}