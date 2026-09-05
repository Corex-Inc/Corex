package dev.corexinc.corex.environment.flags.trackers;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.flags.trackers.AbstractFlagTracker;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

/**
 * Flags stored in an item's own persistent data.
 *
 * <p>An item has no identity of its own, so the first flag written to it also stamps a random id
 * into its data. That id is what expiration tasks are keyed by, and it survives restarts, copies
 * and moves between inventories, unlike the identity of the {@link ItemStack} object in memory.
 * Writes still go to the stack handed to the constructor, so a tracker built from a copy of an
 * inventory item changes the copy.</p>
 */
public class ItemFlagTracker extends AbstractFlagTracker {

    private static final String ID_KEY = "flag_id";

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
        ItemMeta meta = item.getItemMeta();
        String id = meta != null ? meta.getPersistentDataContainer().get(key(ID_KEY), PersistentDataType.STRING) : null;
        return "item@" + (id != null ? id : "unbound@" + item.getType().name().toLowerCase());
    }

    private static NamespacedKey key(String name) {
        return new NamespacedKey(Corex.getInstance(), name);
    }

    private NamespacedKey flagKey(String rootKey) {
        return key("flag_" + rootKey.toLowerCase());
    }

    private String rawOf(String rootKey) {
        ItemMeta meta = item.getItemMeta();
        return meta != null ? meta.getPersistentDataContainer().get(flagKey(rootKey), PersistentDataType.STRING) : null;
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
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(key(ID_KEY), PersistentDataType.STRING)) {
            pdc.set(key(ID_KEY), PersistentDataType.STRING, UUID.randomUUID().toString());
        }
        pdc.set(flagKey(rootKey), PersistentDataType.STRING, encode(expireTimeMs, value));
        item.setItemMeta(meta);
    }

    @Override
    protected void deleteRaw(String rootKey) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.getPersistentDataContainer().remove(flagKey(rootKey));
        item.setItemMeta(meta);
    }
}
