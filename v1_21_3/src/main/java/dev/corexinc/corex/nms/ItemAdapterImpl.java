package dev.corexinc.corex.nms;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.utils.adapters.ItemAdapter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemAdapterImpl implements ItemAdapter {

    @Override
    public Object getCustomModelData(ItemStack item) {
        if (item == null) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null || !meta.hasCustomModelData()) return null;
        return meta.getCustomModelData();
    }

    @Override
    public void applyCustomModelData(ItemStack item, AbstractTag tag) {
        if (item == null || tag == null) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (tag instanceof ElementTag el) {
            meta.setCustomModelData(el.asInt());
            item.setItemMeta(meta);
        }
    }
}