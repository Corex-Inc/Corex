package dev.corexinc.corex.environment.tags.world;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.core.DurationTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.generator.WorldInfo;
import org.jspecify.annotations.NonNull;

import java.util.UUID;

/* @doc object
 * @Name WorldTag
 * @Prefix w
 * @Format The identity format for worlds is the name of the world. World names are case-insensitive.
 * @Description A WorldTag represents a world on the server.
 * This is a lazy object that stores the world identity and resolves the Bukkit world only when needed.
 */
public class WorldTag implements AbstractTag {

    private static final String prefix = "w";

    private final String name;
    private final WorldInfo cachedInfo;

    public static final TagProcessor<WorldTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {

        BaseTagProcessor.registerBaseTag("world", attribute -> {
            if (!attribute.hasParam()) {
                return null;
            }
            return new WorldTag(attribute.getParam());
        });

        ObjectFetcher.registerFetcher(prefix, WorldTag::new);

        /* @doc tag
         * @Name name
         * @RawName <WorldTag.name>
         * @Object WorldTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description Returns the world name.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "name", (attr, obj) ->
                new ElementTag(obj.name));

        /* @doc tag
         * @Name uuid
         * @RawName <WorldTag.uuid>
         * @Object WorldTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description Returns the world UUID.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "uuid", (attr, obj) ->
                obj.cachedInfo == null ? null : new ElementTag(obj.cachedInfo.getUID().toString()));

        /* @doc tag
         * @Name seed
         * @RawName <WorldTag.seed>
         * @Object WorldTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description Returns the world seed.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "seed", (attr, obj) ->
                obj.getWorldInfo() == null ? null : new ElementTag(obj.getWorldInfo().getSeed()));

        /* @doc tag
         * @Name minHeight
         * @RawName <WorldTag.minHeight>
         * @Object WorldTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description Returns the minimum world height.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "minHeight", (attr, obj) ->
                obj.getWorldInfo() == null ? null : new ElementTag(obj.getWorldInfo().getMinHeight()));

        /* @doc tag
         * @Name maxHeight
         * @RawName <WorldTag.maxHeight>
         * @Object WorldTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description Returns the maximum world height.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "maxHeight", (attr, obj) ->
                obj.getWorldInfo() == null ? null : new ElementTag(obj.getWorldInfo().getMaxHeight()));

        /* @doc tag
         * @Name environment
         * @RawName <WorldTag.environment>
         * @Object WorldTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description Returns the world environment type.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "environment", (attr, obj) ->
                obj.getWorldInfo() == null ? null : new ElementTag(obj.getWorldInfo().getEnvironment().name()));

        /* @doc tag
         * @Name time
         * @RawName <WorldTag.time>
         * @Object WorldTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description Returns the current world time.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "time", (attr, obj) ->
                obj.getWorld() == null ? null : new ElementTag(obj.getWorld().getTime()));

        /* @doc tag
         * @Name fullTime
         * @RawName <WorldTag.fullTime>
         * @Object WorldTag
         * @ReturnType DurationTag
         * @NoArg
         * @Description Returns the total world time.
         */
        TAG_PROCESSOR.registerTag(DurationTag.class, "fullTime", (attr, obj) ->
                obj.getWorld() == null ? null : new DurationTag(obj.getWorld().getFullTime()));

        /* @doc tag
         * @Name players
         * @RawName <WorldTag.players>
         * @Object WorldTag
         * @ReturnType ListTag(PlayerTag)
         * @NoArg
         * @Description Returns all online players in this world.
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "players", (attr, obj) -> {
            World world = obj.getWorld();
            if (world == null) return null;

            ListTag list = new ListTag();
            for (Player player : world.getPlayers()) {
                list.addObject(new PlayerTag(player));
            }
            return list;
        });

        /* @doc tag
         * @Name spawn
         * @RawName <WorldTag.spawn>
         * @Object WorldTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description Returns the world spawn location.
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "spawn", (attr, obj) ->
                obj.getWorld() == null ? null : new LocationTag(obj.getWorld().getSpawnLocation()));

        /* @doc tag
         * @Name regions
         * @RawName <WorldTag.regions>
         * @Object WorldTag
         * @ReturnType ListTag(RegionTag)
         * @NoArg
         * @Description Returns all active regions in the world.
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "regions", (attr, obj) -> {
            World world = obj.getWorld();
            if (world == null) return null;

            ListTag list = new ListTag();
            if (!Corex.isFolia()) {
                list.addObject(new RegionTag(world, 0, 0));
            } else {
                for (RegionTag region : RegionTag.FoliaSupport.getAllRegions(world)) {
                    list.addObject(region);
                }
            }
            return list;
        });
    }

    public WorldTag(World world) {
        this.name = world.getName();
        this.cachedInfo = world;
    }

    public WorldTag(WorldInfo info) {
        this.name = info.getName();
        this.cachedInfo = info;
    }

    public WorldTag(String raw) {
        if (raw == null || raw.isEmpty()) {
            this.name = null;
            this.cachedInfo = null;
            return;
        }

        String clean = raw.toLowerCase().startsWith(prefix + "@")
                ? raw.substring(2)
                : raw;

        World world = Bukkit.getWorld(clean);

        if (world == null) {
            try {
                world = Bukkit.getWorld(UUID.fromString(clean));
            } catch (IllegalArgumentException ignored) {
            }
        }

        if (world != null) {
            this.name = world.getName();
            this.cachedInfo = world;
        } else {
            this.name = clean;
            this.cachedInfo = null;
        }
    }

    public World getWorld() {
        if (cachedInfo instanceof World w) {
            return w;
        }
        if (cachedInfo != null) {
            World byUuid = Bukkit.getWorld(cachedInfo.getUID());
            if (byUuid != null) return byUuid;
        }
        return Bukkit.getWorld(name);
    }

    public WorldInfo getWorldInfo() {
        World w = getWorld();
        if (w != null) return w;
        return cachedInfo;
    }

    public boolean isLoaded() {
        return getWorld() != null;
    }

    @Override
    public @NonNull String getPrefix() {
        return prefix;
    }

    @Override
    public @NonNull String identify() {
        return prefix + "@" + name;
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