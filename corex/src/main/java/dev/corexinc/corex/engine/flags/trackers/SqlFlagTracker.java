package dev.corexinc.corex.engine.flags.trackers;

import com.zaxxer.hikari.HikariDataSource;
import dev.corexinc.corex.engine.flags.DatabaseManager;
import dev.corexinc.corex.engine.flags.FlagManager;
import dev.corexinc.corex.engine.utils.CorexLogger;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class SqlFlagTracker extends AbstractFlagTracker {

    private record CachedFlag(long expireTime, String value) {}

    public static final int DEFAULT_CACHE_SIZE = 10_000;

    private static final CachedFlag ABSENT = new CachedFlag(0, null);
    private static volatile int cacheSize = DEFAULT_CACHE_SIZE;
    private static final Map<String, CachedFlag> CACHE = Collections.synchronizedMap(
            new LinkedHashMap<>(256, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, CachedFlag> eldest) {
                    return size() > cacheSize;
                }
            });

    private final String trackerId;
    private final HikariDataSource dbPool;
    private final String cachePrefix;

    public SqlFlagTracker(File dbFile, String trackerId) {
        this.trackerId = trackerId;
        this.dbPool = DatabaseManager.getPool(dbFile);
        this.cachePrefix = dbFile.getAbsolutePath() + '\0' + trackerId + '\0';
    }

    public static void clearCache() {
        CACHE.clear();
    }

    /**
     * Sets how many root records stay cached across every SQL tracker; {@code 0} keeps the default.
     */
    public static void setCacheSize(int size) {
        cacheSize = size > 0 ? size : DEFAULT_CACHE_SIZE;
    }

    @Override
    public boolean isAsyncSafeCleanup() {
        return true;
    }

    @Override
    public String getTrackerId() {
        return trackerId;
    }

    private String cacheKey(String rootKey) {
        return cachePrefix + rootKey;
    }

    @Override
    protected String readRaw(String rootKey) {
        String cacheKey = cacheKey(rootKey);
        CachedFlag cached = CACHE.get(cacheKey);
        if (cached != null) {
            if (cached.value() == null) return null;
            if (cached.expireTime() > 0 && System.currentTimeMillis() >= cached.expireTime()) {
                deleteRaw(rootKey);
                return null;
            }
            return cached.value();
        }

        String sql = "SELECT value, expire_time FROM flags WHERE tracker_id = ? AND key_name = ?";
        try (Connection conn = dbPool.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, trackerId);
            ps.setString(2, rootKey);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String value = rs.getString("value");
                long expireTime = rs.getLong("expire_time");
                if (expireTime > 0 && System.currentTimeMillis() >= expireTime) {
                    deleteRaw(rootKey);
                    return null;
                }
                if (expireTime > 0) FlagManager.ensureScheduled(this, rootKey, expireTime);
                CACHE.put(cacheKey, new CachedFlag(expireTime, value));
                return value;
            }
            CACHE.put(cacheKey, ABSENT);
        } catch (Exception e) {
            CorexLogger.error("SQL Read Error: " + e.getMessage());
        }
        return null;
    }

    @Override
    protected long readRawExpire(String rootKey) {
        CachedFlag cached = CACHE.get(cacheKey(rootKey));
        if (cached == null && readRaw(rootKey) != null) cached = CACHE.get(cacheKey(rootKey));
        return cached != null ? cached.expireTime() : 0L;
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
            CACHE.put(cacheKey(rootKey), new CachedFlag(expireTimeMs, value));
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
            CACHE.put(cacheKey(rootKey), ABSENT);
        } catch (Exception e) {
            CorexLogger.error("SQL Delete Error: " + e.getMessage());
        }
    }
}
