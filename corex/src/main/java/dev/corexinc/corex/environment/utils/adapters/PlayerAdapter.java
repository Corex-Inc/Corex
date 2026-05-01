package dev.corexinc.corex.environment.utils.adapters;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public interface PlayerAdapter {
    void sendReconfiguration(Player player);

    void sendToast(Player player, Component message, Material icon, String frame);
}
