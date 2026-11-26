package com.datecourse.db;

import com.datecourse.model.Place;
import com.datecourse.model.PlaceDetails;
import com.datecourse.model.PriceEstimate;
import com.datecourse.service.PlaceSource;
import com.fasterxml.jackson.databind.JsonNode;
// DB의 JSON 문자열을 장소 속성으로 읽기 위한 라이브러리
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * - DB 장소 데이터를 Place 모델로 변환하는 조회용 클래스
 * - 공공 카탈로그 우선 조회, 없으면 기존 통합 장소 자료 사용
 */
public class PlaceRepository implements PlaceSource {
    private final DataSource dataSource;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlaceRepository(DataSource dataSource){
        this.dataSource = dataSource;
    }

    // 공공 카탈로그가 있으면 우선 반환하고 없으면 기존 JSON 데이터 조회
    public List<Place> findAll() throws SQLException, IOException{
        var catalog = new PlaceCatalogRepository(dataSource).findImported();
        if (catalog.isPresent()) return catalog.get();
        String sql = """
                SELECT record_id, data_payload
                FROM data_records
                WHERE source_file = ?
                ORDER BY record_id
                """;
        List<Place> places = new ArrayList<>();

        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql)){
            stmt.setString(1,"integrated_places.csv");

            try (ResultSet result = stmt.executeQuery()){
                while(result.next()){
                    JsonNode data = objectMapper.readTree(
                            result.getString("data_payload"));

                    Place place = new Place(
                            result.getLong("record_id"),
                            data.path("id").asText(),
                            data.path("name").asText(),
                            data.path("category").asText(),
                            data.path("lat").asDouble(),
                            data.path("lon").asDouble(),
                            data.path("price").isIntegralNumber() && data.path("price").canConvertToInt()
                                    && data.path("price").asInt() > 0
                                    ? PriceEstimate.exact(data.path("price").asInt()) : PriceEstimate.unknown(),
                            data.path("rating").asDouble(),
                            data.path("details").isObject()?objectMapper.treeToValue(data.path("details"),PlaceDetails.class):PlaceDetails.EMPTY
                    );
                    places.add(place);
                }
            }
        }
        return places;
    }

}
