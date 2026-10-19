package kr.ac.university.localdb.db;

import com.datecourse.model.Place;
import com.zaxxer.hikari.HikariDataSource;

import java.util.List;

public class PlaceQueryDemo {
    public static void main(String[] args) throws Exception {
        try (HikariDataSource pool = DatabasePool.create(DbSettings.load())) {
            PlaceRepository repository = new PlaceRepository(pool);

            List<Place> places = repository.findAll();

            System.out.println("조회한 장소 수: " + places.size());

            places.stream()
                    .limit(3)
                    .forEach(System.out::println);
        }
    }
}