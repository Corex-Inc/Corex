package dev.corexinc.corex.engine.flags.trackers;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.flags.FlagManager;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.Position;
import dev.corexinc.corex.environment.tags.core.MapTag;

import java.util.Optional;
import java.util.regex.Pattern;

public abstract class AbstractFlagTracker {

    public abstract String getTrackerId();
    protected abstract String readRaw(String rootKey);
    protected abstract long readRawExpire(String rootKey);
    protected abstract void writeRaw(String rootKey, String value, long expireTimeMs);
    protected abstract void deleteRaw(String rootKey);
    public abstract boolean isAsyncSafeCleanup();

    private static final Pattern DOT = Pattern.compile("\\.");

    public AbstractFlagTracker() {}

    /**
     * Encodes a value with its expiry the way the PDC backed trackers store it: {@code expire;value}.
     */
    protected static String encode(long expireTimeMs, String value) {
        return expireTimeMs + ";" + value;
    }

    /**
     * Reads the expiry out of an {@link #encode} string, {@code 0} when it carries none.
     */
    protected static long decodeExpire(String raw) {
        int separator = raw.indexOf(';');
        if (separator <= 0) return 0L;
        try {
            return Long.parseLong(raw.substring(0, separator));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    /**
     * Resolves an {@link #encode} string read from storage: drops it when expired, re-arms its
     * expiration task when it has one that outlived a restart, and returns the bare value.
     */
    protected String resolveEncoded(String rootKey, String raw) {
        int separator = raw.indexOf(';');
        if (separator <= 0) return raw;

        long expireTime = decodeExpire(raw);
        String value = raw.substring(separator + 1);

        if (expireTime > 0) {
            if (System.currentTimeMillis() >= expireTime) {
                deleteRaw(rootKey);
                return null;
            }
            FlagManager.ensureScheduled(this, rootKey, expireTime);
        }
        return value;
    }

    public AbstractTag getFlag(String keyPath) {
        String[] parts = DOT.split(keyPath);
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
        String[] parts = DOT.split(keyPath);
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

            writeRaw(rootKey, rootMap.identify(), raw != null ? readRawExpire(rootKey) : 0L);
        }

        if (durationMs > 0) {
            FlagManager.scheduleExpiration(this, keyPath, durationMs);
        } else {
            FlagManager.cancelExpiration(getTrackerId(), keyPath);
        }
    }

    public Optional<Position> getSchedulerPosition() {
        return Optional.empty();
    }
}
