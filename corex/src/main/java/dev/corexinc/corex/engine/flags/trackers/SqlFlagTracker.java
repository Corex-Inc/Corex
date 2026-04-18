package dev.corexinc.corex.engine.flags.trackers;

import com.zaxxer.hikari.HikariDataSource;
import dev.corexinc.corex.engine.flags.DatabaseManager;
import dev.corexinc.corex.engine.utils.CorexLogger;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class SqlFlagTracker extends AbstractFlagTracker {

    private final String trackerId;
    private final HikariDataSource dbPool;

    public SqlFlagTracker(File dbFile, String trackerId) {
        this.trackerId = trackerId;
        this.dbPool = DatabaseManager.getPool(dbFile);
        this.registerTracker();
    }

    @Override
    public boolean isAsyncSafeCleanup() {
        return true;
    }

    @Override
    public String getTrackerId() {
        return trackerId;
    }

    @Override
    protected String readRaw(String rootKey) {
        String sql = "SELECT value FROM flags WHERE tracker_id = ? AND key_name = ?";
        try (Connection conn = dbPool.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trackerId);
            ps.setString(2, rootKey);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getString("value");
            }
        } catch (Exception e) {
            CorexLogger.error("SQL Read Error: " + e.getMessage());
        }
        return null;
    }

    @Override
    protected void writeRaw(String rootKey, String value, long expireTimeMs) {
        String sql = "INSERT OR REPLACE INTO flags (tracker_id, key_name, value, expire_time) VALUES (?, ?, ?, ?)";
        try (Connection conn = dbPool.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trackerId);
            ps.setString(2, rootKey);
            ps.setString(3, value);
            ps.setLong(4, expireTimeMs);
            ps.executeUpdate();
        } catch (Exception e) {
            CorexLogger.error("SQL Write Error: " + e.getMessage());
        }
    }

    @Override
    protected void deleteRaw(String rootKey) {
        String sql = "DELETE FROM flags WHERE tracker_id = ? AND key_name = ?";
        try (Connection conn = dbPool.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trackerId);
            ps.setString(2, rootKey);
            ps.executeUpdate();
        } catch (Exception e) {
            CorexLogger.error("SQL Delete Error: " + e.getMessage());
        }
    }
}