package dev.corexinc.corex.environment.tags.utils;

import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.exceptions.RegionRelocateException;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.BiomeTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.tags.world.MaterialTag;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.generator.structure.Structure;
import org.bukkit.util.BiomeSearchResult;
import org.bukkit.util.StructureSearchResult;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/* @doc object
 *
 * @Name FindTag
 * @Prefix find
 *
 * @Description
 * An internal utility tag used for searching entities, players, and blocks within a radius
 * of a specific location. Usually accessed via `<LocationTag.find>`.
 */
public class FindTag implements AbstractTag {

    private static final String PREFIX = "find";
    public static final TagProcessor<FindTag> TAG_PROCESSOR = new TagProcessor<>();

    private final Location center;

    @SuppressWarnings("deprecation")
    public static void register() {
        ObjectFetcher.registerFetcher(PREFIX, FindTag::new);

        /* @doc tag
         * @Name entities[].within[]
         * @RawName <FindTag.entities[(<matcher>)].within[<#.#>]>
         * @Object FindTag
         * @ReturnType ListTag(EntityTag)
         * @Description
         * Returns a list of all entities within the specified radius.
         * Optionally accepts a matcher parameter to filter entities by type or properties.
         *
         * @Implements LocationTag.find.entities[(<matcher>)].within[<#.#>]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "entities", (attr, obj) -> {
            String matcher = attr.hasParam() ? attr.getParam() : null;
            if (attr.matchesNext("within") && attr.hasNextParam()) {
                double radius = new ElementTag(attr.getNextParam()).asDouble();
                attr.fulfill(1);

                World world = obj.center.getWorld();
                if (world == null) return null;
                if (!SchedulerAdapter.isRegionOwner(obj.center)) throw new RegionRelocateException(obj.center);

                List<AbstractTag> found = new ArrayList<>();
                for (Entity entity : world.getNearbyEntities(obj.center, radius, radius, radius)) {
                    EntityTag et = new EntityTag(entity);
                    if (matcher == null || et.tryAdvancedMatcher(matcher)) {
                        found.add(et);
                    }
                }
                found.sort(Comparator.comparingDouble(e -> ((EntityTag) e).getEntity().getLocation().distanceSquared(obj.center)));
                return new ListTag(found);
            }
            return null;
        }).test("*", "within[10]");

        /* @doc tag
         * @Name livingEntities[].within[]
         * @RawName <FindTag.livingEntities[(<matcher>)].within[<#.#>]>
         * @Object FindTag
         * @ReturnType ListTag(EntityTag)
         * @Description
         * Returns a list of all living entities within the specified radius.
         * This excludes items, experience orbs, etc.
         * Optionally accepts a matcher parameter.
         *
         * @Implements LocationTag.find_living_entities.within[<#.#>]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "livingEntities", (attr, obj) -> {
            String matcher = attr.hasParam() ? attr.getParam() : null;
            if (attr.matchesNext("within") && attr.hasNextParam()) {
                double radius = new ElementTag(attr.getNextParam()).asDouble();
                attr.fulfill(1);

                World world = obj.center.getWorld();
                if (world == null) return null;
                if (!SchedulerAdapter.isRegionOwner(obj.center)) throw new RegionRelocateException(obj.center);

                List<AbstractTag> found = new ArrayList<>();
                for (Entity entity : world.getNearbyEntities(obj.center, radius, radius, radius)) {
                    if (entity instanceof LivingEntity) {
                        EntityTag et = new EntityTag(entity);
                        if (matcher == null || et.tryAdvancedMatcher(matcher)) {
                            found.add(et);
                        }
                    }
                }
                found.sort(Comparator.comparingDouble(e -> ((EntityTag) e).getEntity().getLocation().distanceSquared(obj.center)));
                return new ListTag(found);
            }
            return null;
        }).test("*", "within[10]");

        /* @doc tag
         * @Name players.within[]
         * @RawName <FindTag.players.within[<#.#>]>
         * @Object FindTag
         * @ReturnType ListTag(PlayerTag)
         * @Description
         * Returns a list of all players currently standing within the specified radius.
         *
         * @Implements LocationTag.find_players_within[<#.#>]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "players", (attr, obj) -> {
            if (attr.matchesNext("within") && attr.hasNextParam()) {
                double radius = new ElementTag(attr.getNextParam()).asDouble();
                attr.fulfill(1);

                World world = obj.center.getWorld();
                if (world == null) return null;
                if (!SchedulerAdapter.isRegionOwner(obj.center)) throw new RegionRelocateException(obj.center);

                List<AbstractTag> found = new ArrayList<>();
                for (Player player : world.getPlayers()) {
                    if (player.getLocation().distance(obj.center) <= radius) {
                        found.add(new PlayerTag(player));
                    }
                }
                found.sort(Comparator.comparingDouble(e -> ((PlayerTag) e).getPlayer().getLocation().distanceSquared(obj.center)));
                return new ListTag(found);
            }
            return null;
        }).test(null, "within[10]");

        /* @doc tag
         * @Name blocks[].within[]
         * @RawName <FindTag.blocks[(<matcher>)].within[<#.#>]>
         * @Object FindTag
         * @ReturnType ListTag(LocationTag)
         * @Description
         * Scans the surrounding chunks and returns a list of all block locations within the specified radius.
         * Optionally accepts a MaterialTag matcher to filter specific block types.
         *
         * @Implements LocationTag.find_blocks[(<matcher>)].within[<#.#>]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "blocks", (attr, obj) -> {
            String matcher = attr.hasParam() ? attr.getParam() : null;
            if (attr.matchesNext("within") && attr.hasNextParam()) {
                double radius = new ElementTag(attr.getNextParam()).asDouble();
                attr.fulfill(1);

                World world = obj.center.getWorld();
                if (world == null) return null;
                if (!SchedulerAdapter.isRegionOwner(obj.center)) throw new RegionRelocateException(obj.center);

                int r = (int) Math.ceil(radius);
                int cx = obj.center.getBlockX();
                int cy = obj.center.getBlockY();
                int cz = obj.center.getBlockZ();

                List<AbstractTag> found = new ArrayList<>();
                for (int x = -r; x <= r; x++) {
                    for (int y = -r; y <= r; y++) {
                        for (int z = -r; z <= r; z++) {
                            Location loc = new Location(world, cx + x, cy + y, cz + z);
                            if (loc.distance(obj.center) <= radius) {
                                LocationTag lt = new LocationTag(loc);
                                if (matcher == null || new MaterialTag(loc.getBlock()).tryAdvancedMatcher(matcher)) {
                                    found.add(lt);
                                }
                            }
                        }
                    }
                }
                found.sort(Comparator.comparingDouble(e -> ((LocationTag) e).getLocation().distanceSquared(obj.center)));
                return new ListTag(found);
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         * @Name surfaceBlocks[].within[]
         * @RawName <FindTag.surfaceBlocks[(<matcher>)].within[<#.#>]>
         * @Object FindTag
         * @ReturnType ListTag(LocationTag)
         * @Description
         * Returns a list of all surface blocks within the specified radius.
         * A block is considered a surface if the two blocks directly above it are air.
         *
         * @Implements LocationTag.find.surface_blocks[(<material>|...)].within[<#.#>]
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "surfaceBlocks", (attr, obj) -> {
            String matcher = attr.hasParam() ? attr.getParam() : null;
            if (attr.matchesNext("within") && attr.hasNextParam()) {
                double radius = new ElementTag(attr.getNextParam()).asDouble();
                attr.fulfill(1);

                World world = obj.center.getWorld();
                if (world == null) return null;
                if (!SchedulerAdapter.isRegionOwner(obj.center)) throw new RegionRelocateException(obj.center);

                int r = (int) Math.ceil(radius);
                int cx = obj.center.getBlockX();
                int cy = obj.center.getBlockY();
                int cz = obj.center.getBlockZ();

                List<AbstractTag> found = new ArrayList<>();
                for (int x = -r; x <= r; x++) {
                    for (int y = -r; y <= r; y++) {
                        for (int z = -r; z <= r; z++) {
                            Location loc = new Location(world, cx + x, cy + y, cz + z);
                            if (loc.distance(obj.center) <= radius) {
                                Material type = loc.getBlock().getType();
                                if (type != Material.AIR
                                        && loc.clone().add(0, 1, 0).getBlock().getType() == Material.AIR
                                        && loc.clone().add(0, 2, 0).getBlock().getType() == Material.AIR) {

                                    LocationTag lt = new LocationTag(loc);
                                    if (matcher == null || new MaterialTag(type).tryAdvancedMatcher(matcher)) {
                                        found.add(lt);
                                    }
                                }
                            }
                        }
                    }
                }
                found.sort(Comparator.comparingDouble(e -> ((LocationTag) e).getLocation().distanceSquared(obj.center)));
                return new ListTag(found);
            }
            return null;
        }).ignoreTest();

        /* @doc tag
         * @Name biome[]
         * @RawName <FindTag.biome[<biome>]>
         * @Object FindTag
         * @ReturnType LocationTag
         * @Description
         * Searches for and returns the location of the nearest block matching the specified biome.
         * Note that this process can be heavy on server performance.
         *
         * @Implements LocationTag.find_nearest_biome[<biome>]
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "biome", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            World world = obj.center.getWorld();
            if (world == null) return null;
            if (!SchedulerAdapter.isRegionOwner(obj.center)) throw new RegionRelocateException(obj.center);

            BiomeTag biomeTag = attr.getParamObject(BiomeTag.class, BiomeTag::new);
            if (biomeTag == null || biomeTag.getBiomeKey() == null) return null;

            Biome biome = org.bukkit.Registry.BIOME.get(biomeTag.getBiomeKey());
            if (biome == null) return null;

            BiomeSearchResult result = world.locateNearestBiome(obj.center, 5000, biome);
            if (result == null) return null;
            return new LocationTag(result.getLocation());
        }).ignoreTest();

        /* @doc tag
         *
         * @Name structure[]
         * @RawName <FindTag.structure[<map>]>
         * @Object FindTag
         * @ReturnType LocationTag
         * @ArgRequired
         * @Description
         * Finds the closest vanilla or custom structure matching the parameters within a radius.
         * Input is a MapTag:[structure=desert_pyramid;radius=100;unexplored=false]
         *
         * @Implements LocationTag.find_structure
         */
        TAG_PROCESSOR.registerTag(LocationTag.class, "structure", (attr, obj) -> {
            if (!attr.hasParam()) return null;

            String param = attr.getParam();
            if (!param.startsWith("[")) param = "[" + param + "]";
            MapTag input = new MapTag(param);

            AbstractTag structTag = input.getObject("structure");
            AbstractTag radiusTag = input.getObject("radius");
            if (structTag == null || radiusTag == null) return null;

            NamespacedKey key = NamespacedKey.fromString(structTag.identify().toLowerCase());
            if (key == null) return null;

            Structure structure = Registry.STRUCTURE.get(key);
            if (structure == null) return null;

            int radius = new ElementTag(radiusTag.identify()).asInt();
            boolean unexplored = false;
            AbstractTag unexpTag = input.getObject("unexplored");
            if (unexpTag != null) unexplored = new ElementTag(unexpTag.identify()).asBoolean();

            Location loc = obj.center;
            if (loc.getWorld() == null) return null;

            StructureSearchResult res = loc.getWorld().locateNearestStructure(loc, structure, radius, unexplored);
            if (res != null) {
                return new LocationTag(res.getLocation());
            }
            return null;
        }).ignoreTest();
    }

    public FindTag(Location center) {
        this.center = center;
    }

    public FindTag(String raw) {
        this.center = new LocationTag(raw).getLocation();
    }

    @Override
    public @NonNull String getPrefix() {
        return PREFIX;
    }

    @Override
    public @NonNull String identify() {
        return PREFIX + "@";
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<FindTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public @NonNull String getTestValue() {
        return "find@1,1,1,world";
    }
}