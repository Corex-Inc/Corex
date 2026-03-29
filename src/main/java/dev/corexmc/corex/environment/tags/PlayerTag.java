package dev.corexmc.corex.environment.tags;

import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.api.tags.Attribute;
import dev.corexmc.corex.api.tags.TagProcessor;
import dev.corexmc.corex.engine.tags.TagManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PlayerTag implements AbstractTag {

    private String prefix = "p";
    private final OfflinePlayer offlinePlayer;

    public static final TagProcessor<PlayerTag> PROCESSOR = new TagProcessor<>();

    public static void register() {
        TagManager.registerBaseTag("player", (attribute) -> {
            if (attribute.getQueue() != null && attribute.getQueue().getPlayer() != null) {
                return attribute.getQueue().getPlayer();
            }
            return new ElementTag("null");
        });


        PROCESSOR.registerTag("name", (attribute, object) -> {
            String name = object.offlinePlayer.getName();
            return new ElementTag(name != null ? name : "Unknown");
        });

        PROCESSOR.registerTag("isOnline", (attribute, object) -> {
            return new ElementTag(String.valueOf(object.offlinePlayer.isOnline()));
        });

        PROCESSOR.registerTag("uuid", (attribute, object) -> {
            return new ElementTag(object.offlinePlayer.getUniqueId().toString());
        });
    }

    public PlayerTag(UUID uuid) {
        this.offlinePlayer = Bukkit.getOfflinePlayer(uuid);
    }

    public PlayerTag(Player player) {
        this.offlinePlayer = player;
    }

    public OfflinePlayer getOfflinePlayer() {
        return offlinePlayer;
    }

    public Player getPlayer() {
        return offlinePlayer.getPlayer();
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public AbstractTag setPrefix(String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public String identify() {
        return prefix + "@" + offlinePlayer.getUniqueId().toString();
    }

    @Override
    public AbstractTag getAttribute(Attribute attribute) {
        return PROCESSOR.process(this, attribute);
    }
}