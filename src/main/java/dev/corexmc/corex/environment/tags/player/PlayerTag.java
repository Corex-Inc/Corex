package dev.corexmc.corex.environment.tags.player;

import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.api.tags.Attribute;
import dev.corexmc.corex.api.processors.TagProcessor;
import dev.corexmc.corex.engine.tags.ObjectFetcher;
import dev.corexmc.corex.engine.tags.TagManager;
import dev.corexmc.corex.environment.tags.core.ElementTag;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public class PlayerTag implements AbstractTag {

    private static String prefix = "p";
    private final OfflinePlayer offlinePlayer;

    public static final TagProcessor<PlayerTag> PROCESSOR = new TagProcessor<>();

    public static void register() {
        TagManager.registerBaseTag("player", (attribute) -> {
            if (attribute.getQueue() != null && attribute.getQueue().getPlayer() != null) {
                return attribute.getQueue().getPlayer();
            }
            return new ElementTag("null");
        });

        ObjectFetcher.registerFetcher(prefix, (uuidStr) -> {
            return new PlayerTag(java.util.UUID.fromString(uuidStr));
        });


        PROCESSOR.registerTag(ElementTag.class, "name", (attribute, object) -> {
            String name = object.offlinePlayer.getName();
            return new ElementTag(name != null ? name : "Unknown");
        });

        PROCESSOR.registerTag(ElementTag.class, "isOnline", (attribute, object) -> {
            return new ElementTag(String.valueOf(object.offlinePlayer.isOnline()));
        });

        PROCESSOR.registerTag(ElementTag.class, "uuid", (attribute, object) -> {
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

    public PlayerTag(String raw) {
        if (raw == null || raw.isEmpty()) {
            this.offlinePlayer = null;
        } else {
            String cleanRaw = raw.toLowerCase().startsWith("p@") ? raw.substring(2) : raw;

            org.bukkit.OfflinePlayer tempPlayer;
            try {
                tempPlayer = org.bukkit.Bukkit.getOfflinePlayer(java.util.UUID.fromString(cleanRaw));
            } catch (Exception e) {
                @SuppressWarnings("deprecation")
                org.bukkit.OfflinePlayer p = org.bukkit.Bukkit.getOfflinePlayer(cleanRaw);
                tempPlayer = p;
            }
            this.offlinePlayer = tempPlayer;
        }
    }

    @Override
    public @NonNull String getPrefix() {
        return prefix;
    }

    @Override
    public @NonNull AbstractTag setPrefix(@NonNull String prefix) {
        this.prefix = prefix;
        return this;
    }

    @Override
    public @NonNull String identify() {
        return prefix + "@" + offlinePlayer.getUniqueId().toString();
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return PROCESSOR.process(this, attribute);
    }

    @Override
    public TagProcessor<PlayerTag> getProcessor() {
        return PROCESSOR;
    }

    @Override
    public String getTestValue() {
        return "p@465876c1-2a15-4fc0-9f0b-97de13aa46f1";
    }
}