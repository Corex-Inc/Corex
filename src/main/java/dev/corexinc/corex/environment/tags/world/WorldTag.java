package dev.corexinc.corex.environment.tags.world;

import dev.corexinc.corex.Corex;
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
import dev.corexinc.corex.environment.tags.world.RegionTag;
import java.util.UUID;

/* @doc object
 *
 * @Name WorldTag
 * @Prefix w
 * @Format
 * The identity format for worlds is the name of the world it should be associated with.
 * For example, to reference the world named 'world1', use simply 'world1'.
 * World names are case-insensitive.
 *
 * @Description
 * A WorldTag represents a world on the server.
 *
 * This object type is flaggable.
 * Flags on this object type will be stored in the world folder in a file named 'denizen_flags.dat', like "server/world/denizen_flags.dat".
 */
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

        /* @doc tag
         *
         * @Name name
         * @RawName <WorldTag.name>
         * @Object WorldTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the display name of the world.
         *
         * @Implements WorldTag.name
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "name", (attr, obj) ->
                new ElementTag(obj.world.getName()));

        /* @doc tag
         *
         * @Name uuid
         * @RawName <WorldTag.uuid>
         * @Object WorldTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the uuid of the world.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "uuid", (attr, obj) ->
                new ElementTag(obj.world.getUID().toString()));

        /* @doc tag
         *
         * @Name environment
         * @RawName <WorldTag.environment>
         * @Object WorldTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the environment type of the world, which can be NORMAL, NETHER, or THE_END.
         *
         * @Implements WorldTag.environment
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "environment", (attr, obj) ->
                new ElementTag(obj.world.getEnvironment().name()));

        /* @doc tag
         *
         * @Name time
         * @RawName <WorldTag.time>
         * @Object WorldTag
         * @ReturnType ElementTag(Number)
         * @Mechanism WorldTag.time
         * @NoArg
         * @Description
         * Returns the current in-game time of this world.
         *
         * @Implements WorldTag.time
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "time", (attr, obj) ->
                new ElementTag(obj.world.getTime()));

        /* @doc tag
         *
         * @Name fullTime
         * @RawName <WorldTag.fullTime>
         * @Object WorldTag
         * @ReturnType DurationTag
         * @NoArg
         * @Description
         * Returns the in-game time of this world as a duration.
         *
         * @Implements WorldTag.time_full
         */
        TAG_PROCESSOR.registerTag(DurationTag.class, "fullTime", (attr, obj) ->
                new DurationTag(obj.world.getFullTime()));

        /* @doc tag
         *
         * @Name players
         * @RawName <WorldTag.players>
         * @Object WorldTag
         * @ReturnType ListTag(PlayerTag)
         * @NoArg
         * @Description
         * Returns a list of all online players currently in this world.
         *
         * @Implements WorldTag.players
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "players", (attr, obj) -> {
            ListTag listTag = new ListTag("");
            for (Player player : obj.world.getPlayers()) {
                listTag.addObject(new PlayerTag(player));
            }
            return listTag;
        });

        /* @doc tag
         *
         * @Name seed
         * @RawName <WorldTag.seed>
         * @Object WorldTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the seed used for world generation.
         *
         * @Implements WorldTag.seed
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "seed", (attr, obj) ->
                new ElementTag(obj.world.getSeed()));

        /* @doc tag
         *
         * @Name difficulty
         * @RawName <WorldTag.difficulty>
         * @Object WorldTag
         * @ReturnType ElementTag
         * @Mechanism WorldTag.difficulty
         * @NoArg
         * @Description
         * Returns the current difficulty level name.
         *
         * @Implements WorldTag.difficulty
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "difficulty", (attr, obj) ->
                new ElementTag(obj.world.getDifficulty().name()));

        /* @doc tag
         *
         * @Name isStorming
         * @RawName <WorldTag.isStorming>
         * @Object WorldTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether it is currently storming in this world.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isStorming", (attr, obj) ->
                new ElementTag(obj.world.hasStorm()));

        /* @doc tag
         *
         * @Name isThundering
         * @RawName <WorldTag.isThundering>
         * @Object WorldTag
         * @ReturnType ElementTag(Boolean)
         * @Mechanism WorldTag.isThundering
         * @NoArg
         * @Description
         * Returns whether the world is currently experiencing thunder.
         *
         * @Implements WorldTag.thundering
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isThundering", (attr, obj) ->
                new ElementTag(obj.world.isThundering()));

        /* @doc tag
         *
         * @Name spawn
         * @RawName <WorldTag.spawn>
         * @Object WorldTag
         * @ReturnType LocationTag
         * @Mechanism WorldTag.spawn
         * @NoArg
         * @Description
         * Returns the world's spawn location.
         *
         * @Implements WorldTag.spawn_location
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "spawn", (attr, obj) ->
                new LocationTag(obj.world.getSpawnLocation()));

        /* @doc tag
         *
         * @Name regions
         * @RawName <WorldTag.regions>
         * @Object WorldTag
         * @ReturnType ListTag(RegionTag)
         * @NoArg
         * @Description
         * Returns a list of all unique tick-regions (processing threads) currently active in this world.
         * On Folia/Canvas servers, this shows how the world is split into concurrent threads.
         *
         * @Usage
         * // Count how many threads are currently processing the world
         * - narrate "World <player.world.name> is currently split into <player.world.regions.size> threads."
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "regions", (attr, obj) -> {
            ListTag list = new ListTag();
            if (!Corex.isFolia()) {
                list.addObject(new RegionTag(obj.getWorld(), 0, 0));
            } else {
                for (RegionTag rt : RegionTag.FoliaSupport.getAllRegions(obj.getWorld())) {
                    list.addObject(rt);
                }
            }
            return list;
        });
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