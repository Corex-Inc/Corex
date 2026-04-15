package dev.corexinc.corex.engine.flags.trackers;

import dev.corexinc.corex.Corex;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;

public class PdcFlagTracker extends AbstractFlagTracker {

    private final PersistentDataHolder holder;
    private final String trackerId;

    public PdcFlagTracker(PersistentDataHolder holder, String trackerId) {
        this.holder = holder;
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

    private NamespacedKey getKey(String rootKey) {
        return new NamespacedKey(Corex.getInstance(), "flag_" + rootKey.toLowerCase());
    }

    @Override
    protected String readRaw(String rootKey) {
        PersistentDataContainer pdc = holder.getPersistentDataContainer();
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
        PersistentDataContainer pdc = holder.getPersistentDataContainer();
        String data = expireTimeMs + ";" + value;
        pdc.set(getKey(rootKey), PersistentDataType.STRING, data);
    }

    @Override
    protected void deleteRaw(String rootKey) {
        holder.getPersistentDataContainer().remove(getKey(rootKey));
    }
}