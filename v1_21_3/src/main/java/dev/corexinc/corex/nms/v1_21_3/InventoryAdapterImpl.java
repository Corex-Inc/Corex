package dev.corexinc.corex.nms.v1_21_3;

import dev.corexinc.corex.environment.utils.adapters.InventoryAdapter;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;

import java.util.Map;

public class InventoryAdapterImpl implements InventoryAdapter {

    @Override
    public Inventory openMenu(Player player, String typeName, Component title, Map<Integer, ItemStack> items) {
        MenuType type = resolve(typeName);
        if (type == null) return null;

        InventoryView view = type.create(player, title);
        Inventory top = view.getTopInventory();

        int size = top.getSize();
        items.forEach((slot, item) -> {
            if (slot >= 0 && slot < size && item != null) top.setItem(slot, item);
        });

        player.openInventory(view);
        return top;
    }

    private MenuType resolve(String typeName) {
        if (typeName == null) return null;
        try {
            NamespacedKey key = NamespacedKey.minecraft(typeName.toLowerCase());
            return RegistryAccess.registryAccess().getRegistry(RegistryKey.MENU).get(key);
        } catch (Exception exception) {
            return null;
        }
    }
}
