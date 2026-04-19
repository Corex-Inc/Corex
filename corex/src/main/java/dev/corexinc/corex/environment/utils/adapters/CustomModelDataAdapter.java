package dev.corexinc.corex.environment.utils.adapters;

import dev.corexinc.corex.api.tags.AbstractTag;
import org.bukkit.inventory.ItemStack;

public interface CustomModelDataAdapter {

    void applyCustomModelData(ItemStack item, AbstractTag tag);

    Object getCustomModelData(ItemStack item);

}