package dev.corexinc.corex.environment.tags.player;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public class PlayerTag implements AbstractTag {

    private static String prefix = "p";
    private final OfflinePlayer offlinePlayer;

    public static final TagProcessor<PlayerTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag("player", (attribute) -> {
            if (attribute.hasParam()) {
                return new PlayerTag(attribute.getParam());
            }
            if (attribute.getQueue() != null && attribute.getQueue().getPlayer() != null) {
                return attribute.getQueue().getPlayer();
            }
            return null;
        });

        ObjectFetcher.registerFetcher(prefix, (uuidStr) -> new PlayerTag(UUID.fromString(uuidStr)));


        TAG_PROCESSOR.registerTag(ElementTag.class, "name", (attribute, object) -> {
            String name = object.offlinePlayer.getName();
            return new ElementTag(name != null ? name : "Unknown");
        });

        TAG_PROCESSOR.registerTag(ElementTag.class, "isOnline", (attribute, object) -> new ElementTag(String.valueOf(object.offlinePlayer.isOnline())));

        TAG_PROCESSOR.registerTag(ElementTag.class, "uuid", (attribute, object) -> new ElementTag(object.offlinePlayer.getUniqueId().toString()));

        TAG_PROCESSOR.registerTag(LocationTag.class, "location", (attribute, object) -> new LocationTag(object.getPlayer().getLocation()));
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
            String cleanRaw = raw.toLowerCase().startsWith(prefix + "@") ? raw.substring(2) : raw;

            OfflinePlayer tempPlayer;
            try {
                tempPlayer = Bukkit.getOfflinePlayer(UUID.fromString(cleanRaw));
            } catch (Exception e) {
                tempPlayer = Bukkit.getOfflinePlayer(cleanRaw);
            }
            this.offlinePlayer = tempPlayer;
        }
    }

    @Override
    public @NonNull String getPrefix() {
        return prefix;
    }

    @Override
    public @NonNull String identify() {
        return prefix + "@" + offlinePlayer.getUniqueId().toString();
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<PlayerTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public @NonNull String getTestValue() {
        return "p@465876c1-2a15-4fc0-9f0b-97de13aa46f1";
    }
}