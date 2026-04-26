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
 * Chunk coordinates differ from block coordinates - to get the chunk coordinate
 * of a block, divide its X or Z by 16 (rounding down).
 */
public class ChunkTag implements AbstractTag, Flaggable {

    private static final String PREFIX = "ch";

    private final World world;
    private final int x;
    private final int z;

    public static final TagProcessor<ChunkTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag("chunk", attr -> {
            if (!attr.hasParam()) return null;
            Object fetched = ObjectFetcher.pickObject(attr.getParam());
            if (fetched instanceof LocationTag locationTag) {
                Location loc = locationTag.getLocation();
                return new ChunkTag(loc.getWorld(), loc.getBlockX() >> 4, loc.getBlockZ() >> 4);
            }
            ChunkTag tag = new ChunkTag(attr.getParam());
            return tag.getWorld() != null ? tag : null;
        });

        ObjectFetcher.registerFetcher(PREFIX, raw -> {
            ChunkTag tag = new ChunkTag(raw);
            return tag.getWorld() != null ? tag : null;
        });

        TAG_PROCESSOR.registerTag(ElementTag.class, "x", (attr, obj) ->
                new ElementTag(obj.x));

        TAG_PROCESSOR.registerTag(ElementTag.class, "z", (attr, obj) ->
                new ElementTag(obj.z));

        TAG_PROCESSOR.registerTag(WorldTag.class, "world", (attr, obj) ->
                new WorldTag(obj.world));

        TAG_PROCESSOR.registerTag(ElementTag.class, "isLoaded", (attr, obj) ->
                new ElementTag(obj.world != null && obj.world.isChunkLoaded(obj.x, obj.z)));

        TAG_PROCESSOR.registerTag(LocationTag.class, "center", (attr, obj) ->
                new LocationTag(new Location(obj.world, (obj.x << 4) + 8, 0, (obj.z << 4) + 8)));

        TAG_PROCESSOR.registerTag(ListTag.class, "players", (attr, obj) -> {
            ListTag listTag = new ListTag();
            if (obj.world == null || !obj.world.isChunkLoaded(obj.x, obj.z)) return listTag;

            Location center = new Location(obj.world, (obj.x << 4) + 8, 0, (obj.z << 4) + 8);
            if (!SchedulerAdapter.isRegionOwner(center)) throw new RegionRelocateException(center);

            for (Entity entity : obj.getChunk().getEntities()) {
                if (entity instanceof Player player) {
                    listTag.addObject(new PlayerTag(player));
                }
            }
            return listTag;
        });

        TAG_PROCESSOR.registerTag(ListTag.class, "entities", (attr, obj) -> {
            ListTag listTag = new ListTag();
            if (obj.world == null || !obj.world.isChunkLoaded(obj.x, obj.z)) return listTag;

            Location center = new Location(obj.world, (obj.x << 4) + 8, 0, (obj.z << 4) + 8);
            if (!SchedulerAdapter.isRegionOwner(center)) throw new RegionRelocateException(center);

            for (Entity entity : obj.getChunk().getEntities()) {
                listTag.addObject(new EntityTag(entity));
            }
            return listTag;
        });

        TAG_PROCESSOR.registerTag(RegionTag.class, "region", (attr, obj) ->
                new RegionTag(obj.world, obj.x, obj.z));

        TAG_PROCESSOR.registerTag(CuboidTag.class, "cuboid", (attr, obj) -> {
            if (obj.world == null) return null;
            int minX = obj.x << 4;
            int minZ = obj.z << 4;
            return new CuboidTag(
                    new Location(obj.world, minX, obj.world.getMinHeight(), minZ),
                    new Location(obj.world, minX + 16, obj.world.getMaxHeight(), minZ + 16)
            );
        });
    }

    public ChunkTag(@NonNull Chunk chunk) {
        this.world = chunk.getWorld();
        this.x = chunk.getX();
        this.z = chunk.getZ();
    }

    public ChunkTag(@NonNull World world, int x, int z) {
        this.world = world;
        this.x = x;
        this.z = z;
    }

    public ChunkTag(String raw) {
        if (raw == null || raw.isBlank()) {
            this.world = null;
            this.x = 0;
            this.z = 0;
            return;
        }

        String clean = raw.toLowerCase().startsWith(PREFIX + "@") ? raw.substring(PREFIX.length() + 1) : raw;
        String[] parts = clean.trim().split("\\s*,\\s*");

        if (parts.length >= 3) {
            this.world = Bukkit.getWorld(parts[0]);
            this.x = Integer.parseInt(parts[1]);
            this.z = Integer.parseInt(parts[2]);
        } else {
            this.world = null;
            this.x = 0;
            this.z = 0;
        }
    }

    public @Nullable Chunk getChunk() {
        return world != null ? world.getChunkAt(x, z) : null;
    }

    public @Nullable World getWorld() { return world; }

    @Override
    public @NonNull String getPrefix() { return PREFIX; }

    @Override
    public @NonNull String identify() {
        if (world == null) return PREFIX + "@null,0,0";
        return PREFIX + "@" + world.getName() + "," + x + "," + z;
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
        if (world == null) throw new IllegalStateException("Cannot get flags for invalid chunk");
        Location center = new Location(world, (x << 4) + 8, 0, (z << 4) + 8);
        if (!SchedulerAdapter.isRegionOwner(center)) throw new RegionRelocateException(center);

        return new PdcFlagTracker(getChunk(), identify());
    }
}