package com.datecourse.db;

// 데이터베이스 연결 관리 최적화용 import 
import com.zaxxer.hikari.HikariDataSource;

/** 데이터베이스 커넥션 부하 방지용 lazy pool 구성 */
public final class DatabasePool {
    private DatabasePool() {}

    public static HikariDataSource create(DbSettings settings) {
        HikariDataSource pool = new HikariDataSource();
        pool.setPoolName("membership-db");
        pool.setJdbcUrl(settings.url());
        pool.setUsername(settings.user());
        pool.setPassword(settings.password());
        pool.setMaximumPoolSize(2);
        pool.setMinimumIdle(1);
        pool.setConnectionTimeout(10_000);
        pool.setValidationTimeout(3_000);
        pool.setMaxLifetime(600_000);
        pool.setKeepaliveTime(0);
        if (settings.url().startsWith("jdbc:mysql:")) {
            // 데이터베이스에게 요청하는 쿼리를 batch 형태로 일괄적 전송하게 처리, 쿼리 최적화용
            pool.addDataSourceProperty("rewriteBatchedStatements", "true");
            pool.addDataSourceProperty("connectTimeout", "5000");
            pool.addDataSourceProperty("socketTimeout", "10000");
        }
        return pool;
    }
}
