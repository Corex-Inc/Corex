package dev.corexinc.corex.nms.v1_21;

import dev.corexinc.corex.environment.utils.adapters.InventoryAdapter;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class InventoryAdapterImpl implements InventoryAdapter {

    @Override
    public Inventory openMenu(Player player, String typeName, Component title, Map<Integer, ItemStack> items) {
        Inventory inventory = create(typeName, title);
        if (inventory == null) return null;

        int size = inventory.getSize();
        items.forEach((slot, item) -> {
            if (slot >= 0 && slot < size && item != null) inventory.setItem(slot, item);
        });

        player.openInventory(inventory);
        return inventory;
    }

    private Inventory create(String typeName, Component title) {
        String upper = typeName == null ? "GENERIC_9X3" : typeName.toUpperCase();

        int rows = chestRows(upper);
        if (rows > 0) return Bukkit.createInventory(null, rows * 9, title);

        InventoryType type = mapType(upper);
        return type != null ? Bukkit.createInventory(null, type, title) : null;
    }

    private static int chestRows(String upper) {
        return switch (upper) {
            case "GENERIC_9X1" -> 1;
            case "GENERIC_9X2" -> 2;
            case "GENERIC_9X3" -> 3;
            case "GENERIC_9X4" -> 4;
            case "GENERIC_9X5" -> 5;
            case "GENERIC_9X6" -> 6;
            default -> 0;
        };
    }

    private static InventoryType mapType(String upper) {
        return switch (upper) {
            case "GENERIC_3X3", "CRAFTER_3X3" -> InventoryType.DROPPER;
            case "HOPPER" -> InventoryType.HOPPER;
            case "SHULKER_BOX" -> InventoryType.SHULKER_BOX;
            case "BREWING_STAND" -> InventoryType.BREWING;
            case "FURNACE" -> InventoryType.FURNACE;
            case "BLAST_FURNACE" -> InventoryType.BLAST_FURNACE;
            case "SMOKER" -> InventoryType.SMOKER;
            case "ANVIL" -> InventoryType.ANVIL;
            case "GRINDSTONE" -> InventoryType.GRINDSTONE;
            case "CARTOGRAPHY_TABLE" -> InventoryType.CARTOGRAPHY;
            case "MERCHANT" -> InventoryType.MERCHANT;
            case "SMITHING" -> InventoryType.SMITHING;
            case "LOOM" -> InventoryType.LOOM;
            case "STONECUTTER" -> InventoryType.STONECUTTER;
            case "ENCHANTMENT" -> InventoryType.ENCHANTING;
            case "BEACON" -> InventoryType.BEACON;
            case "LECTERN" -> InventoryType.LECTERN;
            case "CRAFTING" -> InventoryType.WORKBENCH;
            default -> null;
        };
    }
}
