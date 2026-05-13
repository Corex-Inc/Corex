package dev.corexinc.corex.engine.flags.trackers;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.utils.Position;
import dev.corexinc.corex.environment.utils.BukkitSchedulerAdapter;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public class LocationPdcFlagTracker extends AbstractFlagTracker {

    private final Location loc;
    private final String trackerId;

    public LocationPdcFlagTracker(Location loc, String trackerId) {
        this.loc = loc;
        this.trackerId = trackerId;
        this.registerTracker();
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
        return Optional.of(BukkitSchedulerAdapter.toPosition(loc));
    }

    public Location getLocation() {
        return loc;
    }

    private NamespacedKey getKey(String rootKey) {
        String coordPrefix = "loc_" + loc.getBlockX() + "_" + loc.getBlockY() + "_" + loc.getBlockZ() + "_";
        return new NamespacedKey(Corex.getInstance(), coordPrefix + rootKey.toLowerCase());
    }

    private PersistentDataContainer getPdc() {
        if (!loc.getWorld().isChunkLoaded(loc.getBlockX() >> 4, loc.getBlockZ() >> 4)) return null;
        return loc.getChunk().getPersistentDataContainer();
    }

    @Override
    protected String readRaw(String rootKey) {
        PersistentDataContainer pdc = getPdc();
        if (pdc == null) return null;

        NamespacedKey key = getKey(rootKey);
        if (!pdc.has(key, PersistentDataType.STRING)) return null;

        String raw = pdc.get(key, PersistentDataType.STRING);
        if (raw == null) return null;

        int sepIndex = raw.indexOf(';');
        if (sepIndex == -1) return raw;

        long expireTime = Long.parseLong(raw.substring(0, sepIndex));
        String value = raw.substring(sepIndex + 1);

        if (expireTime > 0 && System.currentTimeMillis() >= expireTime) {
            deleteRaw(rootKey);
            return null;
        }

        return value;
    }

    @Override
    protected void writeRaw(String rootKey, String value, long expireTimeMs) {
        PersistentDataContainer pdc = getPdc();
        if (pdc != null) {
            pdc.set(getKey(rootKey), PersistentDataType.STRING, expireTimeMs + ";" + value);
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