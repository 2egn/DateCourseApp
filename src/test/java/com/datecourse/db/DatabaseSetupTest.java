package com.datecourse.db;

import com.datecourse.maintenance.DatabaseSetup;
import com.datecourse.model.*;
import com.zaxxer.hikari.HikariDataSource;
import org.h2.tools.RunScript;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** 관리 준비와 일반 실행의 경계를 격리 DB에서 확인한다. */
class DatabaseSetupTest {
    private HikariDataSource pool() {
        return DatabasePool.create(new DbSettings("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL", "sa", ""));
    }
    private void base(HikariDataSource pool) throws Exception {
        try (var c=pool.getConnection()) {
            for (String file:List.of("02_create_tables.sql","07_saved_courses.sql","08_saved_course_price_estimates.sql"))
                try (var reader=Files.newBufferedReader(Path.of("data/sql",file))) { RunScript.execute(c,reader); }
        }
    }
    private void execute(HikariDataSource pool,String sql) throws SQLException {
        try (var c=pool.getConnection();var s=c.createStatement()) { s.execute(sql); }
    }
    private String value(HikariDataSource pool,String sql) throws SQLException {
        try (var c=pool.getConnection();var s=c.createStatement();var row=s.executeQuery(sql)) { row.next();return row.getString(1); }
    }
    private Set<String> tables(HikariDataSource pool) throws SQLException {
        var result=new TreeSet<String>();
        try (var c=pool.getConnection();var s=c.createStatement();var rows=s.executeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA='PUBLIC'")) {
            while(rows.next())result.add(rows.getString(1));
        }
        return result;
    }
    private Map<String,String> counts(HikariDataSource pool) throws SQLException {
        var result=new TreeMap<String,String>();
        for(String table:tables(pool))result.put(table,value(pool,"SELECT COUNT(*) FROM "+table));
        return result;
    }
    @Test void missingOneTimePriceMigrationStopsBeforeAnyPreparationWrites() throws Exception {
        try(var pool=pool()) {
            try(var c=pool.getConnection()) {
                for(String file:List.of("02_create_tables.sql","07_saved_courses.sql"))
                    try(var reader=Files.newBufferedReader(Path.of("data/sql",file))) { RunScript.execute(c,reader); }
            }
            var before=tables(pool);
            assertThrows(SQLException.class,()->DatabaseSetup.prepare(pool));
            assertEquals(before,tables(pool));
        }
    }
    @Test void startupNeverCreatesAdministratorOrWidensPasswordColumn() throws Exception {
        try(var pool=pool()) {
            execute(pool,"CREATE TABLE members(password_hash VARCHAR(60) NOT NULL)");
            execute(pool,"INSERT INTO members VALUES('existing-hash')");
            var before=tables(pool);
            assertThrows(DatabaseSchema.SetupRequiredException.class,()->new MemberRepository(pool).initialize());
            assertEquals(before,tables(pool));
            try(var c=pool.getConnection();var s=c.createStatement();var rows=s.executeQuery("SELECT password_hash FROM members WHERE 1=0")) {
                assertEquals(60,rows.getMetaData().getPrecision(1));
            }
            DatabaseSetup.prepareMembership(pool);
            assertEquals("existing-hash",value(pool,"SELECT password_hash FROM members"));
            assertTrue(tables(pool).contains("APP_ADMINS"));
            try(var c=pool.getConnection();var s=c.createStatement();var rows=s.executeQuery("SELECT password_hash FROM members WHERE 1=0")) {
                assertEquals(255,rows.getMetaData().getPrecision(1));
            }
        }
    }
    @Test void explicitSetupIsRepeatableAndPreservesAccountsCoursesReportsAndAdministratorPassword() throws Exception {
        try(var pool=pool()) {
            base(pool);DatabaseSetup.prepare(pool);
            execute(pool,"INSERT INTO members(member_id,email,password_hash,name) VALUES(7,'review@example.test','existing-member-hash','Demo')");
            execute(pool,"UPDATE app_admins SET password_hash='administrator-reset-hash'");
            var place=new Place(1,"review-place","장소","MEAL",35,129,5000,0);
            var session=new UserSession(7,"Demo");
            new SavedCourseRepository(pool).saveWithoutPreview(session,null,new RoutePlan(35,129,2,1000,"코스",List.of(place)));
            new InformationReportRepository(pool).submit(session,place,InformationReport.Field.PRICE,"6000","확인","https://example.test");
            var before=counts(pool);DatabaseSetup.prepare(pool);DatabaseSetup.prepare(pool);
            assertEquals(before,counts(pool));
            assertEquals("existing-member-hash",value(pool,"SELECT password_hash FROM members"));
            assertEquals("administrator-reset-hash",value(pool,"SELECT password_hash FROM app_admins"));
            assertEquals("1",value(pool,"SELECT COUNT(*) FROM place_catalog_lock"));
        }
    }
    @Test void normalRepositoriesDoNotCreateMissingFeatureTables() throws Exception {
        try(var pool=pool()) {
            base(pool);var before=tables(pool);
            assertThrows(DatabaseSchema.SetupRequiredException.class,()->new MemberRepository(pool).initialize());
            assertThrows(DatabaseSchema.SetupRequiredException.class,()->new InformationReportRepository(pool).apply(List.of()));
            assertThrows(DatabaseSchema.SetupRequiredException.class,()->new SavedCourseRepository(pool).initializeTransport());
            assertThrows(DatabaseSchema.SetupRequiredException.class,()->new SavedCourseRepository(pool).initializePreviews());
            assertThrows(DatabaseSchema.SetupRequiredException.class,()->new PlaceCatalogRepository(pool).initialize());
            assertEquals(before,tables(pool));
        }
    }
    @Test void preparedApplicationCanReadAndSaveWithNoSchemaChangePermissions() throws Exception {
        try(var admin=pool()) {
            base(admin);DatabaseSetup.prepare(admin);
            execute(admin,"INSERT INTO members(member_id,email,password_hash,name) VALUES(7,'restricted@example.test','test-hash','Demo')");
            execute(admin,"CREATE USER app_user PASSWORD 'test-only-password'");
            for(String table:tables(admin))execute(admin,"GRANT SELECT,INSERT,UPDATE,DELETE ON "+table+" TO app_user");
            String url=admin.getJdbcUrl().split(";")[0];
            try(var restricted=DatabasePool.create(new DbSettings(url,"app_user","test-only-password"))) {
                new MemberRepository(restricted).initialize();
                var session=new UserSession(7,"Demo");
                var place=new Place(1,"restricted-place","장소","MEAL",35,129,5000,0);
                var courses=new SavedCourseRepository(restricted);
                var course=courses.saveWithoutPreview(session,null,new RoutePlan(35,129,2,1000,"코스",List.of(place)));
                assertEquals(course,courses.findAll(session).getFirst());
                var reports=new InformationReportRepository(restricted);
                assertTrue(reports.submit(session,place,InformationReport.Field.PRICE,"6000","확인","https://example.test")>0);
                assertEquals(1,reports.apply(List.of(place)).size());
                assertThrows(SQLException.class,()->execute(restricted,"CREATE TABLE forbidden(id INT)"));
            }
        }
    }
}
