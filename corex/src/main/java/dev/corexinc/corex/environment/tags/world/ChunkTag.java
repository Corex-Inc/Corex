package dev.corexinc.corex.environment.tags.world;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.tags.Flaggable;
import dev.corexinc.corex.engine.flags.trackers.AbstractFlagTracker;
import dev.corexinc.corex.engine.flags.trackers.PdcFlagTracker;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.exceptions.RegionRelocateException;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.area.CuboidTag;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/* @doc object
 *
 * @Name ChunkTag
 * @Prefix ch
 *
 * @Format
 * The identity format for chunks is `ch@world,x,z`.
 * Where x and z are chunk coordinates (block coordinates divided by 16).
 * For example: `ch@world,5,-3`.
 *
 * @Description
 * A ChunkTag represents a 16x16 column of blocks within a world.
 * Chunk coordinates differ from block coordinates — to get the chunk coordinate
 * of a block, divide its X or Z by 16 (rounding down).
 */
public class ChunkTag implements AbstractTag, Flaggable {

    private static final String PREFIX = "ch";

    private final Chunk chunk;

    public static final TagProcessor<ChunkTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag("chunk", attr -> {
            if (!attr.hasParam()) return null;
            Object fetched = ObjectFetcher.pickObject(attr.getParam());
            if (fetched instanceof LocationTag locationTag) {
                Location loc = locationTag.getLocation();
                if (!SchedulerAdapter.isRegionOwner(loc)) throw new RegionRelocateException(loc);
                return new ChunkTag(loc.getChunk());
            }
            Location chunkCenter = parseChunkCenter(attr.getParam());
            if (chunkCenter != null && !SchedulerAdapter.isRegionOwner(chunkCenter)) throw new RegionRelocateException(chunkCenter);
            ChunkTag tag = new ChunkTag(attr.getParam());
            return tag.getChunk() != null ? tag : null;
        });

        ObjectFetcher.registerFetcher(PREFIX, raw -> {
            Location chunkCenter = parseChunkCenter(raw);
            if (chunkCenter != null && !SchedulerAdapter.isRegionOwner(chunkCenter)) throw new RegionRelocateException(chunkCenter);
            ChunkTag tag = new ChunkTag(raw);
            return tag.getChunk() != null ? tag : null;
        });

        /* @doc tag
         *
         * @Name x
         * @RawName <ChunkTag.x>
         * @Object ChunkTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the X coordinate of this chunk in chunk-space (not block-space).
         *
         * @Implements ChunkTag.x
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "x", (attr, obj) ->
                new ElementTag(obj.chunk.getX()));

        /* @doc tag
         *
         * @Name z
         * @RawName <ChunkTag.z>
         * @Object ChunkTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the Z coordinate of this chunk in chunk-space (not block-space).
         *
         * @Implements ChunkTag.z
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "z", (attr, obj) ->
                new ElementTag(obj.chunk.getZ()));

        /* @doc tag
         *
         * @Name world
         * @RawName <ChunkTag.world>
         * @Object ChunkTag
         * @ReturnType WorldTag
         * @NoArg
         * @Description
         * Returns the world this chunk belongs to.
         *
         * @Implements ChunkTag.world
         */
        TAG_PROCESSOR.registerTag(WorldTag.class, "world", (attr, obj) ->
                new WorldTag(obj.chunk.getWorld()));

        /* @doc tag
         *
         * @Name isLoaded
         * @RawName <ChunkTag.isLoaded>
         * @Object ChunkTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether this chunk is currently loaded into memory.
         * Unloaded chunks are not being ticked and their entities are inactive.
         *
         * @Implements ChunkTag.is_loaded
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isLoaded", (attr, obj) ->
                new ElementTag(obj.chunk.isLoaded()));

        /* @doc tag
         *
         * @Name center
         * @RawName <ChunkTag.center>
         * @Object ChunkTag
         * @ReturnType LocationTag
         * @NoArg
         * @Description
         * Returns the location of the center of this chunk at Y=0.
         * The center is at block offset +8 on both X and Z axes within the chunk.
         *
         * @Implements ChunkTag.cuboid.center
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "center", (attr, obj) ->
                new LocationTag(new Location(
                        obj.chunk.getWorld(),
                        (obj.chunk.getX() << 4) + 8,
                        0,
                        (obj.chunk.getZ() << 4) + 8
                )));

        /* @doc tag
         *
         * @Name players
         * @RawName <ChunkTag.players>
         * @Object ChunkTag
         * @ReturnType ListTag(PlayerTag)
         * @NoArg
         * @Description
         * Returns a list of all online players currently standing inside this chunk.
         *
         * @Implements ChunkTag.players
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "players", (attr, obj) -> {
            ListTag listTag = new ListTag();
            if (!obj.chunk.isLoaded()) return listTag;
            for (Entity entity : obj.chunk.getEntities()) {
                if (entity instanceof Player player) {
                    listTag.addObject(new PlayerTag(player));
                }
            }
            return listTag;
        });

        /* @doc tag
         *
         * @Name entities
         * @RawName <ChunkTag.entities>
         * @Object ChunkTag
         * @ReturnType ListTag(EntityTag)
         * @NoArg
         * @Description
         * Returns a list of all entities currently inside this chunk.
         * The chunk must be loaded — if it is not, an empty list is returned.
         *
         * @Implements ChunkTag.entities
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "entities", (attr, obj) -> {
            ListTag listTag = new ListTag();
            if (!obj.chunk.isLoaded()) return listTag;
            for (Entity entity : obj.chunk.getEntities()) {
                listTag.addObject(new EntityTag(entity));
            }
            return listTag;
        });

        /* @doc tag
         *
         * @Name region
         * @RawName <ChunkTag.region>
         * @Object ChunkTag
         * @ReturnType RegionTag
         * @NoArg
         * @Description
         * Returns the tick-region (processing thread) that manages this chunk.
         * On Folia/Canvas servers this may differ per chunk.
         */
        TAG_PROCESSOR.registerTag(RegionTag.class, "region", (attr, obj) ->
                new RegionTag(obj.chunk.getWorld(), obj.chunk.getX(), obj.chunk.getZ()));

        /* @doc tag
         *
         * @Name cuboid
         * @RawName <ChunkTag.cuboid>
         * @Object ChunkTag
         * @ReturnType CuboidTag
         * @NoArg
         * @Description
         * Returns a CuboidTag representing the exact bounding box of this chunk.
         * Spans from the minimum corner (x*16, -64, z*16) to the maximum corner (x*16+15, 319, z*16+15).
         * Y bounds use standard overworld limits and may not match custom world heights.
         *
         * @Implements ChunkTag.cuboid
         */
        TAG_PROCESSOR.registerTag(CuboidTag.class, "cuboid", (attr, obj) -> {
            int minX = obj.chunk.getX() << 4;
            int minZ = obj.chunk.getZ() << 4;
            double[][] bounds = {{minX, -64, minZ}, {minX + 16, 320, minZ + 16}};
            return new CuboidTag(obj.chunk.getWorld(), java.util.Collections.singletonList(bounds));
        });
    }

    private static @Nullable Location parseChunkCenter(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String clean = raw.toLowerCase().startsWith(PREFIX + "@") ? raw.substring(PREFIX.length() + 1) : raw;
        String[] parts = clean.trim().split("\\s*,\\s*");
        if (parts.length < 3) return null;
        try {
            World world = Bukkit.getWorld(parts[0]);
            if (world == null) return null;
            int cx = Integer.parseInt(parts[1]);
            int cz = Integer.parseInt(parts[2]);
            return new Location(world, (cx << 4) + 8, 0, (cz << 4) + 8);
        } catch (Exception ignored) {
            return null;
        }
    }

    public ChunkTag(@NonNull Chunk chunk) {
        this.chunk = chunk;
    }

    public ChunkTag(String raw) {
        if (raw == null || raw.isBlank()) {
            this.chunk = null;
            return;
        }

        String clean = raw.toLowerCase().startsWith(PREFIX + "@") ? raw.substring(PREFIX.length() + 1) : raw;
        String[] parts = clean.trim().split("\\s*,\\s*");

        Chunk resolved = null;
        try {
            if (parts.length >= 3) {
                World world = Bukkit.getWorld(parts[0]);
                if (world != null) {
                    resolved = world.getChunkAt(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                }
            }
        } catch (Exception ignored) {}

        this.chunk = resolved;
    }

    public @Nullable Chunk getChunk() { return chunk; }

    @Override
    public @NonNull String getPrefix() { return PREFIX; }

    @Override
    public @NonNull String identify() {
        return PREFIX + "@" + chunk.getWorld().getName() + "," + chunk.getX() + "," + chunk.getZ();
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<ChunkTag> getProcessor() { return TAG_PROCESSOR; }

    @Override
    public @NonNull String getTestValue() { return "ch@world,0,0"; }

    @Override
    public AbstractFlagTracker getFlagTracker() {
        return new PdcFlagTracker(chunk, identify());
    }
}