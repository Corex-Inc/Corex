package dev.corexinc.corex.engine.flags.trackers;

import dev.corexinc.corex.Corex;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ItemFlagTracker extends AbstractFlagTracker {

    private final ItemStack item;

    public ItemFlagTracker(ItemStack item) {
        this.item = item;
    }

    @Override
    public boolean isAsyncSafeCleanup() {
        return false;
    }

    @Override
    public String getTrackerId() {
        return "item@" + item.getType().name().toLowerCase();
    }

    private NamespacedKey getKey(String rootKey) {
        return new NamespacedKey(Corex.getInstance(), "flag_" + rootKey.toLowerCase());
    }

    @Override
    protected String readRaw(String rootKey) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
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
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.getPersistentDataContainer().set(getKey(rootKey), PersistentDataType.STRING, expireTimeMs + ";" + value);
        item.setItemMeta(meta);
    }

    @Override
    protected void deleteRaw(String rootKey) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.getPersistentDataContainer().remove(getKey(rootKey));
        item.setItemMeta(meta);
    }
}
