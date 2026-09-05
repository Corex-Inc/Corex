package dev.corexinc.corex.environment.flags.trackers;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.flags.trackers.AbstractFlagTracker;
import dev.corexinc.corex.engine.utils.Position;
import dev.corexinc.corex.environment.utils.BukkitSchedulerAdapter;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

public class PdcFlagTracker extends AbstractFlagTracker {

    private final PersistentDataHolder holder;
    private final String trackerId;

    public PdcFlagTracker(PersistentDataHolder holder, String trackerId) {
        this.holder = holder;
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
        if (holder instanceof Entity entity) {
            return Optional.of(BukkitSchedulerAdapter.toPosition(entity.getLocation()));
        }
        return Optional.empty();
    }

    private NamespacedKey getKey(String rootKey) {
        return new NamespacedKey(Corex.getInstance(), "flag_" + rootKey.toLowerCase());
    }

    private String rawOf(String rootKey) {
        return holder.getPersistentDataContainer().get(getKey(rootKey), PersistentDataType.STRING);
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
        PersistentDataContainer pdc = holder.getPersistentDataContainer();
        pdc.set(getKey(rootKey), PersistentDataType.STRING, encode(expireTimeMs, value));
    }

    @Override
    protected void deleteRaw(String rootKey) {
        holder.getPersistentDataContainer().remove(getKey(rootKey));
    }
}
