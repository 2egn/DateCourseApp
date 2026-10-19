package kr.ac.university.localdb;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Proxy;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ReplaceIntegratedDataTest {
    private static final String INSERT =
            "INSERT INTO data_records(category_id,source_file,source_row_number,data_payload) VALUES ";

    @Test
    void ignoresSchemaSwitchesAndScriptTransactionControl() {
        for (String statement : new String[]{
                "USE university_project;", " use defaultdb; ", "USE	another_database;",
                "-- comment", "", "START TRANSACTION;", "COMMIT;"}) {
            assertTrue(ReplaceIntegratedData.shouldSkipStatement(statement), statement);
        }
    }

    @Test
    void preservesDataModificationStatements() {
        for (String statement : new String[]{
                "INSERT INTO categories VALUES (1, 'MEAL');",
                "DELETE FROM data_records WHERE source_file='old.csv';",
                "UPDATE categories SET category_name='Cafe' WHERE category_id=1;"}) {
            assertFalse(ReplaceIntegratedData.shouldSkipStatement(statement), statement);
        }
    }

    @Test
    void actualDatasetUses25InsertsInsteadOf6139() throws Exception {
        List<String> batches = new ArrayList<>();
        List<String> setup = new ArrayList<>();
        List<Integer> progress = new ArrayList<>();
        Statement recording = (Statement) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{Statement.class}, (proxy, method, args) -> {
                    if (method.getName().equals("executeUpdate")) {
                        batches.add((String) args[0]);
                        return 1;
                    }
                    if (method.getName().equals("execute")) {
                        setup.add((String) args[0]);
                        return false;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
        ReplaceIntegratedData.executeScript(recording,
                Files.readAllLines(Path.of("database/05_replace_integrated_data.sql")), 250, progress::add);
        assertEquals(25, batches.size());
        assertEquals(6139, progress.get(progress.size() - 1));
        assertEquals(250, progress.get(0));
        long totalValues = batches.stream().mapToLong(sql ->
                (sql.length() - sql.replace("'integrated_places.csv'", "").length())
                        / "'integrated_places.csv'".length()).sum();
        assertEquals(6139, totalValues);
        assertTrue(setup.stream().noneMatch(sql -> sql.strip().toUpperCase(Locale.ROOT).startsWith("USE ")));
        assertEquals(5, setup.size(), "Two deletes and three category inserts must be preserved");
    }

    @Test
    void executesPartialBatchesBeforeOtherStatementsInOrder() throws Exception {
        try (Connection c = testDatabase(); Statement s = c.createStatement()) {
            createTable(s);
            List<Integer> progress = new ArrayList<>();
            ReplaceIntegratedData.executeScript(s, List.of(
                    row(1), "UPDATE data_records SET data_payload='updated';", row(2), row(3)), 2, progress::add);
            assertEquals(List.of(1, 3), progress);
            try (ResultSet r = s.executeQuery("SELECT data_payload FROM data_records ORDER BY source_row_number")) {
                assertTrue(r.next()); assertEquals("updated", r.getString(1));
                assertTrue(r.next()); assertEquals("original", r.getString(1));
                assertTrue(r.next()); assertEquals("original", r.getString(1));
                assertFalse(r.next());
            }
        }
    }

    @Test
    void failingBatchCanRollBackEarlierBatchesAndDeletion() throws Exception {
        try (Connection c = testDatabase(); Statement s = c.createStatement()) {
            createTable(s);
            s.executeUpdate(row(99));
            c.setAutoCommit(false);
            assertThrows(SQLException.class, () -> ReplaceIntegratedData.executeScript(s,
                    List.of("DELETE FROM data_records;", row(1), row(2), row(2), row(3)), 2, count -> {}));
            c.rollback();
            try (ResultSet r = s.executeQuery("SELECT source_row_number FROM data_records")) {
                assertTrue(r.next()); assertEquals(99, r.getInt(1)); assertFalse(r.next());
            }
        }
    }

    private Connection testDatabase() throws SQLException {
        return DriverManager.getConnection("jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL", "sa", "");
    }

    private void createTable(Statement statement) throws SQLException {
        statement.execute("CREATE TABLE data_records(category_id INT,source_file VARCHAR(100),"
                + "source_row_number INT PRIMARY KEY,data_payload VARCHAR(100))");
    }

    private String row(int number) {
        return INSERT + "(1,'integrated_places.csv'," + number + ",'original');";
    }
}