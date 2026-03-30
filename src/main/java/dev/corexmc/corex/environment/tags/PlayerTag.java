package dev.corexmc.corex.environment.tags;

import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.api.tags.Attribute;
import dev.corexmc.corex.api.processors.TagProcessor;
import dev.corexmc.corex.engine.tags.ObjectFetcher;
import dev.corexmc.corex.engine.tags.TagManager;
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
}