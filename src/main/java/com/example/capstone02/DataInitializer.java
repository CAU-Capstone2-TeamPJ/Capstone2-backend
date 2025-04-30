package com.example.capstone02;

import com.example.capstone02.entity.FilmingLocation;
import com.example.capstone02.entity.Movie;
import com.example.capstone02.repository.FilmingLocationRepository;
import com.example.capstone02.repository.MovieRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    public CommandLineRunner loadData(MovieRepository movieRepository,
                                      FilmingLocationRepository locationRepository) {
        return args -> {
            // 기존 데이터가 없는 경우에만 테스트 데이터 추가
            if (movieRepository.count() == 0) {
                // 영화 1: 기생충
                Movie parasite = new Movie();
                parasite.setTitle("기생충");
                parasite.setReleaseYear(2019);
                parasite.setDirector("봉준호");
                movieRepository.save(parasite);

                // 기생충 촬영지들
                FilmingLocation location1 = new FilmingLocation();
                location1.setName("자하문터널 계단");
                location1.setAddress("서울특별시 종로구 부암동");
                location1.setLatitude(37.5883);
                location1.setLongitude(126.9688);
                location1.setMovie(parasite);
                locationRepository.save(location1);

                FilmingLocation location2 = new FilmingLocation();
                location2.setName("돌산공원");
                location2.setAddress("인천광역시 동구 화수동");
                location2.setLatitude(37.4865);
                location2.setLongitude(126.6383);
                location2.setMovie(parasite);
                locationRepository.save(location2);

                FilmingLocation location3 = new FilmingLocation();
                location3.setName("스카이프라임 아파트");
                location3.setAddress("서울특별시 성북구 정릉동");
                location3.setLatitude(37.6023);
                location3.setLongitude(127.0118);
                location3.setMovie(parasite);
                locationRepository.save(location3);

                FilmingLocation location4 = new FilmingLocation();
                location4.setName("동작구 아트홀");
                location4.setAddress("서울특별시 동작구 노량진동");
                location4.setLatitude(37.5124);
                location4.setLongitude(126.9456);
                location4.setMovie(parasite);
                locationRepository.save(location4);

                FilmingLocation location5 = new FilmingLocation();
                location5.setName("우성아파트");
                location5.setAddress("서울특별시 동작구 상도동");
                location5.setLatitude(37.5042);
                location5.setLongitude(126.9533);
                location5.setMovie(parasite);
                locationRepository.save(location5);

                // 영화 2: 올드보이
                Movie oldboy = new Movie();
                oldboy.setTitle("올드보이");
                oldboy.setReleaseYear(2003);
                oldboy.setDirector("박찬욱");
                movieRepository.save(oldboy);

                // 올드보이 촬영지들
                FilmingLocation location6 = new FilmingLocation();
                location6.setName("청량리역");
                location6.setAddress("서울특별시 동대문구 전농동");
                location6.setLatitude(37.5804);
                location6.setLongitude(127.0484);
                location6.setMovie(oldboy);
                locationRepository.save(location6);

                FilmingLocation location7 = new FilmingLocation();
                location7.setName("현대고등학교");
                location7.setAddress("서울특별시 강남구 압구정동");
                location7.setLatitude(37.5279);
                location7.setLongitude(127.0329);
                location7.setMovie(oldboy);
                locationRepository.save(location7);

                FilmingLocation location8 = new FilmingLocation();
                location8.setName("녹사평역");
                location8.setAddress("서울특별시 용산구 이태원동");
                location8.setLatitude(37.5345);
                location8.setLongitude(126.9867);
                location8.setMovie(oldboy);
                locationRepository.save(location8);

                FilmingLocation location9 = new FilmingLocation();
                location9.setName("서대문형무소");
                location9.setAddress("서울특별시 서대문구 현저동");
                location9.setLatitude(37.5746);
                location9.setLongitude(126.9594);
                location9.setMovie(oldboy);
                locationRepository.save(location9);

                // 영화 3: 한산: 용의 출현
                Movie hansan = new Movie();
                hansan.setTitle("한산: 용의 출현");
                hansan.setReleaseYear(2022);
                hansan.setDirector("김한민");
                movieRepository.save(hansan);

                // 한산 촬영지들
                FilmingLocation location10 = new FilmingLocation();
                location10.setName("외연도");
                location10.setAddress("충청남도 보령시 오천면");
                location10.setLatitude(36.2254);
                location10.setLongitude(126.5061);
                location10.setMovie(hansan);
                locationRepository.save(location10);

                FilmingLocation location11 = new FilmingLocation();
                location11.setName("남해 상주해수욕장");
                location11.setAddress("경상남도 남해군 상주면");
                location11.setLatitude(34.7166);
                location11.setLongitude(127.9654);
                location11.setMovie(hansan);
                locationRepository.save(location11);

                FilmingLocation location12 = new FilmingLocation();
                location12.setName("순천만 국가정원");
                location12.setAddress("전라남도 순천시 오천동");
                location12.setLatitude(34.9403);
                location12.setLongitude(127.4996);
                location12.setMovie(hansan);
                locationRepository.save(location12);

                FilmingLocation location13 = new FilmingLocation();
                location13.setName("진해해양공원");
                location13.setAddress("경상남도 창원시 진해구");
                location13.setLatitude(35.1495);
                location13.setLongitude(128.6706);
                location13.setMovie(hansan);
                locationRepository.save(location13);

                FilmingLocation location14 = new FilmingLocation();
                location14.setName("통영 세병관");
                location14.setAddress("경상남도 통영시 문화동");
                location14.setLatitude(34.8425);
                location14.setLongitude(128.4214);
                location14.setMovie(hansan);
                locationRepository.save(location14);
            }
        };
    }
}