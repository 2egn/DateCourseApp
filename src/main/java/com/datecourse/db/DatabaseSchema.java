package com.datecourse.db;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/** 앱 시작 -> 로그인 창 연결 흐름 사이에 로그인 창에 필요한 데이터베이스 구조 유효성 검사 클래스  */

public final class DatabaseSchema {
    private DatabaseSchema() {}
    public static final class SetupRequiredException extends SQLException {
        public SetupRequiredException(SQLException cause) {
            super("DB 구조가 준비되지 않았어요. 관리자에게 DB 준비 작업을 요청해 주세요.", cause);
        }
    }
    // 테이블 존재 여부 확인 함수
    public static void requireTables(DataSource source, String... tables) throws SQLException {
        try (Connection connection = source.getConnection()) {
            try (var statement = connection.createStatement()) {
                for (String table : tables) {
                    if (!table.matches("[a-z_]+")) throw new IllegalArgumentException("테이블 이름을 확인해 주세요.");
                    try (var rows = statement.executeQuery("SELECT * FROM " + table + " WHERE 1=0")) {
                        rows.getMetaData();
                    }
                }
            } catch (SQLException failure) { throw new SetupRequiredException(failure); }
        }
    }
    //회원가입, 로그인을 위한 구조 유효성 검사. 
    public static void requireMembership(DataSource source) throws SQLException {
        try (Connection connection = source.getConnection()) {
            try (var statement = connection.createStatement()) {
                try (var rows = statement.executeQuery("SELECT member_id,email,password_hash,name,phone,status,created_at FROM members WHERE 1=0")) {
                    if (rows.getMetaData().getPrecision(3) < 255)
                        throw new SQLException("비밀번호 저장 열의 확장이 필요합니다.");
                }
                try (var rows = statement.executeQuery("SELECT username,password_hash FROM app_admins WHERE 1=0")) { rows.getMetaData(); }
            } catch (SQLException failure) { throw new SetupRequiredException(failure); }
        }
    }
}
