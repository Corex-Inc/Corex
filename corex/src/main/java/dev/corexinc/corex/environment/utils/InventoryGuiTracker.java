package dev.corexinc.corex.environment.utils;

import dev.corexinc.corex.Corex;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

public class InventoryGuiTracker implements Listener {

    private static final Set<Inventory> lockedInventories =
            Collections.synchronizedSet(Collections.newSetFromMap(new IdentityHashMap<>()));
    private static volatile boolean registered = false;

    public static synchronized void ensureRegistered() {
        if (registered) return;
        Bukkit.getPluginManager().registerEvents(new InventoryGuiTracker(), Corex.getInstance());
        registered = true;
    }

    public static void lock(Inventory inventory) {
        ensureRegistered();
        lockedInventories.add(inventory);
    }

    public static void unlock(Inventory inventory) {
        lockedInventories.remove(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!lockedInventories.contains(top)) return;

        if (event.getClickedInventory() == top) {
            event.setCancelled(true);
            return;
        }

        switch (event.getAction()) {
            case MOVE_TO_OTHER_INVENTORY, COLLECT_TO_CURSOR -> event.setCancelled(true);
            default -> {}
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory top = event.getView().getTopInventory();
        if (!lockedInventories.contains(top)) return;

        int topSize = top.getSize();
        for (int rawSlot : event.getRawSlots()) {
            if (rawSlot < topSize) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        lockedInventories.remove(event.getView().getTopInventory());
    }
}
