package dev.corexinc.corex.engine.flags;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.corexinc.corex.engine.utils.CorexLogger;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DatabaseManager {

    private static final Map<String, HikariDataSource> pools = new ConcurrentHashMap<>();

    public static HikariDataSource getPool(File dbFile) {
        String path = dbFile.getAbsolutePath();

        return pools.computeIfAbsent(path, k -> {
            dbFile.getParentFile().mkdirs();

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + path);
            config.setPoolName("Corex-SQL-" + dbFile.getName());

            config.addDataSourceProperty("journal_mode", "WAL");
            config.addDataSourceProperty("synchronous", "NORMAL");

            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);

            HikariDataSource ds = new HikariDataSource(config);
            initTable(ds);
            return ds;
        });
    }

    private static void initTable(HikariDataSource ds) {
        String sql = "CREATE TABLE IF NOT EXISTS flags (" +
                "tracker_id VARCHAR(255) NOT NULL, " +
                "key_name VARCHAR(255) NOT NULL, " +
                "value TEXT, " +
                "expire_time BIGINT, " +
                "PRIMARY KEY (tracker_id, key_name))";

        try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            CorexLogger.error("Error creating SQL table: " + e.getMessage());
        }
    }

    public static void closeAll() {
        for (HikariDataSource ds : pools.values()) {
            if (!ds.isClosed()) ds.close();
        }
        pools.clear();
    }
}