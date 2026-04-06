package dev.corexinc.corex.environment.tags.world;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.core.DurationTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

public class WorldTag implements AbstractTag {

    private static final String prefix = "w";
    private final World world;

    public static final TagProcessor<WorldTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag("world", attribute -> {
            if (!attribute.hasParam()) return null;
            WorldTag worldTag = new WorldTag(attribute.getParam());
            return worldTag.getWorld() != null ? worldTag : null;
        });

        ObjectFetcher.registerFetcher(prefix, raw -> {
            WorldTag tag = new WorldTag(raw);
            return tag.getWorld() != null ? tag : null;
        });

        TAG_PROCESSOR.registerTag(ElementTag.class, "name", (attr, obj) ->
                new ElementTag(obj.world.getName()));

        TAG_PROCESSOR.registerTag(ElementTag.class, "uuid", (attr, obj) ->
                new ElementTag(obj.world.getUID().toString()));

        TAG_PROCESSOR.registerTag(ElementTag.class, "environment", (attr, obj) ->
                new ElementTag(obj.world.getEnvironment().name()));

        TAG_PROCESSOR.registerTag(ElementTag.class, "time", (attr, obj) ->
                new ElementTag(obj.world.getTime()));

        TAG_PROCESSOR.registerTag(DurationTag.class, "fullTime", (attr, obj) ->
                new DurationTag(obj.world.getFullTime()));

        TAG_PROCESSOR.registerTag(ListTag.class, "players", (attr, obj) -> {
            ListTag listTag = new ListTag("");
            for (Player player : obj.world.getPlayers()) {
                listTag.addObject(new PlayerTag(player));
            }
            return listTag;
        });

        TAG_PROCESSOR.registerTag(ElementTag.class, "seed", (attr, obj) ->
                new ElementTag(obj.world.getSeed()));

        TAG_PROCESSOR.registerTag(ElementTag.class, "difficulty", (attr, obj) ->
                new ElementTag(obj.world.getDifficulty().name()));

        TAG_PROCESSOR.registerTag(ElementTag.class, "isStorming", (attr, obj) ->
                new ElementTag(obj.world.hasStorm()));

        TAG_PROCESSOR.registerTag(ElementTag.class, "isThundering", (attr, obj) ->
                new ElementTag(obj.world.isThundering()));

        TAG_PROCESSOR.registerTag(LocationTag.class, "spawn", (attr, obj) ->
                new LocationTag(obj.world.getSpawnLocation()));
    }

    public WorldTag(World world) {
        this.world = world;
    }

    public WorldTag(String raw) {
        if (raw == null || raw.isEmpty()) {
            this.world = null;
            return;
        }

        String clean = raw.toLowerCase().startsWith(prefix + "@") ? raw.substring(2) : raw;

        World resolved;
        try {
            resolved = Bukkit.getWorld(UUID.fromString(clean));
        } catch (Exception e) {
            resolved = Bukkit.getWorld(clean);
        }
        this.world = resolved;
    }

    public World getWorld() {
        return world;
    }

    @Override
    public @NonNull String getPrefix() {
        return prefix;
    }

    @Override
    public @NonNull String identify() {
        return prefix + "@" + world.getName();
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<WorldTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public @NonNull String getTestValue() {
        return "w@world";
    }
}