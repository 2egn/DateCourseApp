package kr.ac.university.localdb.db;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PlaceRepositoryPriceTest {
    @Test void legacyZeroNullAndMissingPricesRemainUnknown() throws Exception {
        var source = new JdbcDataSource();
        source.setURL("jdbc:h2:mem:" + UUID.randomUUID());
        try (var connection = source.getConnection(); var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE data_records(record_id BIGINT,source_file VARCHAR(255),data_payload VARCHAR(10000))");
            try (var insert = connection.prepareStatement("INSERT INTO data_records VALUES(?, 'integrated_places.csv', ? )")) {
                int index = 1;
                for (String suffix : new String[]{",\"price\":5000", ",\"price\":0", ",\"price\":null", ""}) {
                    insert.setInt(1, index++);
                    insert.setString(2, "{\"id\":\"sample\",\"name\":\"예시\",\"category\":\"CAFE\",\"lat\":35.15,\"lon\":129.06" + suffix + "}");
                    insert.executeUpdate();
                }
            }
            var places = new PlaceRepository(source).findAll();
            assertEquals(5000, places.get(0).price());
            for (var place : places.subList(1, places.size())) {
                assertFalse(place.priceEstimate().usablePerPerson());
                assertEquals("가격 미확인", place.priceText());
                assertThrows(IllegalStateException.class, place::price);
            }
        }
    }
}
