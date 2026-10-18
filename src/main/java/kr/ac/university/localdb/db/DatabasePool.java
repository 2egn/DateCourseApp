package kr.ac.university.localdb.db;

import com.zaxxer.hikari.HikariDataSource;

/** Creates a lazy pool: the existing background initialization opens the first connection. */
final class DatabasePool {
    private DatabasePool() {}

    static HikariDataSource create(DbSettings settings) {
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
        // Do not send periodic keepalive queries to an otherwise unused cloud database.
        pool.setKeepaliveTime(0);
        if (settings.url().startsWith("jdbc:mysql:")) {
            pool.addDataSourceProperty("connectTimeout", "5000");
            pool.addDataSourceProperty("socketTimeout", "10000");
        }
        return pool;
    }
}