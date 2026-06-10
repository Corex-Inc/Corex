package dev.corexinc.corex.environment.utils.adapters;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public interface InventoryAdapter {

    Inventory openMenu(Player player, String typeName, Component title, Map<Integer, ItemStack> items);
}
