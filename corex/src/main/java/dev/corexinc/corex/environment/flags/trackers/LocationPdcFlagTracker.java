package dev.corexinc.corex.environment.flags.trackers;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.flags.trackers.AbstractFlagTracker;
import dev.corexinc.corex.engine.utils.Position;
import dev.corexinc.corex.environment.utils.BukkitSchedulerAdapter;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public class LocationPdcFlagTracker extends AbstractFlagTracker {

    private final Location location;
    private final String trackerId;

    public LocationPdcFlagTracker(Location location, String trackerId) {
        this.location = location;
        this.trackerId = trackerId;
    }

    @Override
    public boolean isAsyncSafeCleanup() {
        return false;
    }

    @Override
    public String getTrackerId() {
        return trackerId;
    }

    @Override
    public Optional<Position> getSchedulerPosition() {
        return Optional.of(BukkitSchedulerAdapter.toPosition(location));
    }

    public Location getLocation() {
        return location;
    }

    private NamespacedKey getKey(String rootKey) {
        String coordPrefix = "loc_" + location.getBlockX() + "_" + location.getBlockY() + "_" + location.getBlockZ() + "_";
        return new NamespacedKey(Corex.getInstance(), coordPrefix + rootKey.toLowerCase());
    }

    private PersistentDataContainer getPdc() {
        if (location.getWorld() == null) return null;
        if (!location.getWorld().isChunkLoaded(location.getBlockX() >> 4, location.getBlockZ() >> 4)) return null;
        return location.getChunk().getPersistentDataContainer();
    }

    private String rawOf(String rootKey) {
        PersistentDataContainer pdc = getPdc();
        return pdc != null ? pdc.get(getKey(rootKey), PersistentDataType.STRING) : null;
    }

    @Override
    protected String readRaw(String rootKey) {
        String raw = rawOf(rootKey);
        return raw != null ? resolveEncoded(rootKey, raw) : null;
    }

    @Override
    protected long readRawExpire(String rootKey) {
        String raw = rawOf(rootKey);
        return raw != null ? decodeExpire(raw) : 0L;
    }

    @Override
    protected void writeRaw(String rootKey, String value, long expireTimeMs) {
        PersistentDataContainer pdc = getPdc();
        if (pdc != null) {
            pdc.set(getKey(rootKey), PersistentDataType.STRING, encode(expireTimeMs, value));
        }
    }

    @Override
    protected void deleteRaw(String rootKey) {
        PersistentDataContainer pdc = getPdc();
        if (pdc != null) {
            pdc.remove(getKey(rootKey));
        }
    }
}
