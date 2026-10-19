package kr.ac.university.localdb.db;

import com.datecourse.model.Place;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class PlaceRepository {
    private final DataSource dataSource;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlaceRepository(DataSource dataSource){
        this.dataSource = dataSource;
    }

    public List<Place> findAll() throws SQLException, IOException{
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
                            data.path("price").asInt(),
                            data.path("rating").asDouble()
                    );
                    places.add(place);
                }
            }
        }
        return places;
    }

}
