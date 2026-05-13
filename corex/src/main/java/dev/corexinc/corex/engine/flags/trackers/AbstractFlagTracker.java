package dev.corexinc.corex.engine.flags.trackers;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.flags.FlagManager;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.Position;
import dev.corexinc.corex.environment.tags.core.MapTag;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractFlagTracker {

    public abstract String getTrackerId();
    protected abstract String readRaw(String rootKey);
    protected abstract void writeRaw(String rootKey, String value, long expireTimeMs);
    protected abstract void deleteRaw(String rootKey);
    public abstract boolean isAsyncSafeCleanup();

    private static final Map<String, AbstractFlagTracker> activeTrackers = new ConcurrentHashMap<>();

    public AbstractFlagTracker() {}

    public AbstractTag getFlag(String keyPath) {
        String[] parts = keyPath.split("\\.");
        String rootKey = parts[0];

        String raw = readRaw(rootKey);
        if (raw == null) return null;

        AbstractTag tag = ObjectFetcher.pickObject(raw);

        for (int i = 1; i < parts.length; i++) {
            if (tag instanceof MapTag map) {
                tag = map.getObject(parts[i]);
            } else {
                return null;
            }
        }
        return tag;
    }

    public void deleteFlagPhysically(String keyPath) {
        this.setFlag(keyPath, null, 0);
    }

    public void setFlag(String keyPath, AbstractTag value, long durationMs) {
        String[] parts = keyPath.split("\\.");
        String rootKey = parts[0];

        long expireTimeMs = durationMs > 0 ? System.currentTimeMillis() + durationMs : 0;

        if (parts.length == 1) {
            if (value == null) deleteRaw(rootKey);
            else writeRaw(rootKey, value.identify(), expireTimeMs);
        } else {
            String raw = readRaw(rootKey);
            MapTag rootMap = (raw != null && ObjectFetcher.pickObject(raw) instanceof MapTag m) ? m : new MapTag("");

            MapTag current = rootMap;
            for (int i = 1; i < parts.length - 1; i++) {
                AbstractTag next = current.getObject(parts[i]);
                if (!(next instanceof MapTag)) {
                    next = new MapTag("");
                    current.putObject(parts[i], next);
                }
                current = (MapTag) next;
            }

            if (value == null) current.remove(parts[parts.length - 1]);
            else current.putObject(parts[parts.length - 1], value);

            writeRaw(rootKey, rootMap.identify(), expireTimeMs);
        }

        if (durationMs > 0) {
            FlagManager.scheduleExpiration(getTrackerId(), keyPath, durationMs);
        }
    }

    protected void registerTracker() {
        activeTrackers.put(getTrackerId().toLowerCase(), this);
    }

    public static AbstractFlagTracker getTracker(String id) {
        return activeTrackers.get(id.toLowerCase());
    }

    public void cleanUpExpiredFlag(String keyPath) {
        this.getFlag(keyPath);
    }

    public Optional<Position> getSchedulerPosition() {
        return Optional.empty();
    }
}