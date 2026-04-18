package dev.corexinc.corex.environment.tags.world;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.QueueTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/* @doc object
 *
 * @Name RegionTag
 * @Prefix reg
 * @Format
 * The identity format for RegionTags is 'reg@global' for the global thread, or 'reg@world,x,z'
 * where x and z are the coordinates of the central chunk of that region (processing unit).
 *
 * @Description
 * A RegionTag represents a "tick-region" (thread) on Folia/Canvas servers, or the entire world on regular Paper.
 * This tag is extremely useful for monitoring performance (TPS) of specific areas of the server
 * and retrieving a list of players located within the same processing thread.
 *
 * @Usage
 * // Get the region where the player is standing
 * - define region <region[<player.location>]>
 * - narrate "There are currently <[region].tps> TPS in your region"
 */
public class RegionTag implements AbstractTag {

    private static final String PREFIX = "reg";
    private final boolean isGlobal;
    private final World world;
    private final int chunkX;
    private final int chunkZ;

    public static final TagProcessor<RegionTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag("region", attr -> {
            if (!attr.hasParam()) return new RegionTag("global");
            AbstractTag fetched = ObjectFetcher.pickObject(attr.getParam());
            if (fetched instanceof LocationTag loc) {
                return new RegionTag(loc.getLocation().getWorld(), loc.getLocation().getBlockX() >> 4, loc.getLocation().getBlockZ() >> 4);
            }
            return new RegionTag(attr.getParam());
        });

        ObjectFetcher.registerFetcher(PREFIX, RegionTag::new);

        /* @doc tag
         *
         * @Name isGlobal
         * @RawName <RegionTag.isGlobal>
         * @Object RegionTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns 'true' if this region is the global region (Global Tick).
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isGlobal", (attr, obj) ->
                new ElementTag(obj.isGlobal));

        /* @doc tag
         *
         * @Name tps
         * @RawName <RegionTag.tps>
         * @Object RegionTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the current TPS (Ticks Per Second) for this specific region.
         * On Folia, this value may differ from other regions.
         * Default value - 20.0.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "tps", (attr, obj) -> {
            if (Corex.isFolia() && !obj.isGlobal) {
                return new ElementTag(FoliaSupport.getTPS(obj));
            }
            return new ElementTag(Bukkit.getServer().getTPS()[0]);
        }).ignoreTest();

        /* @doc tag
         *
         * @Name mspt
         * @RawName <RegionTag.mspt>
         * @Object RegionTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the server's average Milliseconds Per Tick (MSPT).
         * On Folia servers it returns total server (not region) MSPT.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "mspt", (attr, obj) ->
                new ElementTag(Bukkit.getServer().getAverageTickTime())).ignoreTest();

        /* @doc tag
         *
         * @Name players
         * @RawName <RegionTag.players>
         * @Object RegionTag
         * @ReturnType ListTag(PlayerTag)
         * @NoArg
         * @Description
         * Returns a list of all players currently being processed by this region (sharing this thread).
         * For the global region on Folia, this will always return an empty list.
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "players", (attr, obj) -> {
            ListTag list = new ListTag();
            if (obj.isGlobal) {
                if (Corex.isFolia()) return list;
                for (Player p : Bukkit.getOnlinePlayers()) list.addObject(new PlayerTag(p));
                return list;
            }
            if (obj.world == null) return list;

            if (Corex.isFolia()) {
                FoliaSupport.fillPlayers(obj, list);
            } else {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getWorld() == obj.world) {
                        list.addObject(new PlayerTag(p));
                    }
                }
            }
            return list;
        });

        /* @doc tag
         *
         * @Name queues
         * @RawName <RegionTag.queues>
         * @Object RegionTag
         * @ReturnType ListTag(QueueTag)
         * @NoArg
         * @Description
         * Returns a list of all active script queues anchored to this region.
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "queues", (attr, obj) -> {
            ListTag list = new ListTag();
            if (obj.isGlobal) {
                for (ScriptQueue queue : ScriptQueue.getAllQueues()) {
                    if (queue.getAnchorLocation() == null) list.addObject(new QueueTag(queue));
                }
                return list;
            }
            if (obj.world == null) return list;

            if (Corex.isFolia()) {
                FoliaSupport.fillQueues(obj, list);
            } else {
                for (ScriptQueue queue : ScriptQueue.getAllQueues()) {
                    Location anchor = queue.getAnchorLocation();
                    if (anchor != null && anchor.getWorld() == obj.world) {
                        list.addObject(new QueueTag(queue));
                    }
                }
            }
            return list;
        });
    }

    public RegionTag(String raw) {
        if (raw == null || raw.equalsIgnoreCase("global") || raw.equalsIgnoreCase(PREFIX + "@global")) {
            this.isGlobal = true;
            this.world = null;
            this.chunkX = 0;
            this.chunkZ = 0;
            return;
        }

        String clean = raw.toLowerCase().startsWith(PREFIX + "@") ? raw.substring(4) : raw;
        String[] parts = clean.split(",");
        this.isGlobal = false;

        World world = null;
        int chunkX = 0, chunkZ = 0;

        try {
            if (parts.length >= 3) {
                world = Bukkit.getWorld(parts[0]);
                chunkX = Integer.parseInt(parts[1]);
                chunkZ = Integer.parseInt(parts[2]);
            }
        } catch (Exception ignored) {}

        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public RegionTag(World world, int chunkX, int chunkZ) {
        this.isGlobal = false;
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
    }

    public RegionTag(LocationTag locationTag) {
        this.isGlobal = false;
        this.world = locationTag.getLocation().getWorld();
        this.chunkX = locationTag.getLocation().getChunk().getX();
        this.chunkZ = locationTag.getLocation().getChunk().getZ();
    }

    public World getWorld() {
        return world;
    }

    @Override
    public @NonNull String getPrefix() {
        return PREFIX;
    }

    @Override
    public @NonNull String identify() {
        if (isGlobal) {
            return PREFIX + "@global";
        }
        if (Corex.isFolia()) {
            return FoliaSupport.identify(this);
        }
        return PREFIX + "@" + world.getName() + "," + chunkX + "," + chunkZ;
    }

    @Override
    public @Nullable AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<RegionTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public @NonNull String getTestValue() {
        return "reg@global";
    }

    public static class FoliaSupport {

        private static Object getRegionNode(World world, int cx, int cz) {
            try {
                Method getRegionizer = world.getClass().getMethod("getRegionizer");
                Object regionizer = getRegionizer.invoke(world);
                if (regionizer != null) {
                    try {
                        return regionizer.getClass().getMethod("getRegionAtSynchronised", int.class, int.class).invoke(regionizer, cx, cz);
                    } catch (Exception ignored) {
                        return regionizer.getClass().getMethod("getRegionAt", int.class, int.class).invoke(regionizer, cx, cz);
                    }
                }
            } catch (Exception ignored) {}

            try {
                Object nmsWorld = world.getClass().getMethod("getHandle").invoke(world);

                try {
                    return nmsWorld.getClass().getMethod("getRegionAtSynchronised", int.class, int.class).invoke(nmsWorld, cx, cz);
                } catch (Exception ignored) {}

                try {
                    Object tickRegions = nmsWorld.getClass().getField("tickRegions").get(nmsWorld);
                    return tickRegions.getClass().getMethod("getRegionAt", int.class, int.class).invoke(tickRegions, cx, cz);
                } catch (Exception ignored) {}

                try {
                    Object chunkTaskScheduler = nmsWorld.getClass().getField("chunkTaskScheduler").get(nmsWorld);
                    Object regioniser = chunkTaskScheduler.getClass().getField("regioniser").get(chunkTaskScheduler);
                    return regioniser.getClass().getMethod("getRegionAt", int.class, int.class).invoke(regioniser, cx, cz);
                } catch (Exception ignored) {}

            } catch (Exception ignored) {}

            return null;
        }

        private static String identifyNode(Object node) {
            if (node == null) return null;
            String id = CenterExtractor.getCenterId(node);
            if (id != null) return id;
            return String.valueOf(node.hashCode());
        }

        private static boolean isSameRegion(Object nodeA, Object nodeB) {
            if (nodeA == null || nodeB == null) return false;
            if (nodeA == nodeB || nodeA.equals(nodeB)) return true;
            String idA = identifyNode(nodeA);
            String idB = identifyNode(nodeB);
            return idA != null && idA.equals(idB);
        }

        private static String identify(RegionTag tag) {
            Object regionNode = getRegionNode(tag.world, tag.chunkX, tag.chunkZ);
            if (regionNode != null) {
                String centerId = identifyNode(regionNode);
                if (centerId != null) return PREFIX + "@" + tag.world.getName() + "," + centerId;
            }
            return PREFIX + "@" + tag.world.getName() + "," + tag.chunkX + "," + tag.chunkZ;
        }

        private static double getTPS(RegionTag tag) {
            Location loc = new Location(tag.world, tag.chunkX << 4, 0, tag.chunkZ << 4);
            try {
                Method getRegionTPSLoc = Bukkit.getServer().getClass().getMethod("getRegionTPS", Location.class);
                double[] tps = (double[]) getRegionTPSLoc.invoke(Bukkit.getServer(), loc);
                if (tps != null) return tps[0];
            } catch (Exception ignored) {
                try {
                    Method getRegionTPSWorld = Bukkit.getServer().getClass().getMethod("getRegionTPS", World.class, int.class, int.class);
                    double[] tps = (double[]) getRegionTPSWorld.invoke(Bukkit.getServer(), tag.world, tag.chunkX, tag.chunkZ);
                    if (tps != null) return tps[0];
                } catch (Exception ignored2) {}
            }
            return Bukkit.getServer().getTPS()[0];
        }

        private static void fillPlayers(RegionTag tag, ListTag list) {
            Location targetLoc = new Location(tag.world, tag.chunkX << 4, 0, tag.chunkZ << 4);
            boolean isTargetCurrentThread = false;
            try {
                isTargetCurrentThread = Bukkit.isOwnedByCurrentRegion(targetLoc);
            } catch (Exception ignored) {}

            Object targetNode = getRegionNode(tag.world, tag.chunkX, tag.chunkZ);

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld() == tag.world) {
                    Location pLoc;
                    try {
                        pLoc = p.getLocation();
                    } catch (Exception e) {
                        continue;
                    }

                    if (isTargetCurrentThread) {
                        try {
                            if (Bukkit.isOwnedByCurrentRegion(pLoc)) {
                                list.addObject(new PlayerTag(p));
                            }
                        } catch (Exception ignored) {}
                    } else if (targetNode != null) {
                        Object pNode = getRegionNode(tag.world, pLoc.getBlockX() >> 4, pLoc.getBlockZ() >> 4);
                        if (isSameRegion(targetNode, pNode)) {
                            list.addObject(new PlayerTag(p));
                        }
                    }
                }
            }
        }

        private static void fillQueues(RegionTag tag, ListTag list) {
            Location targetLoc = new Location(tag.world, tag.chunkX << 4, 0, tag.chunkZ << 4);
            boolean isTargetCurrentThread = false;
            try {
                isTargetCurrentThread = Bukkit.isOwnedByCurrentRegion(targetLoc);
            } catch (Exception ignored) {}

            Object targetNode = getRegionNode(tag.world, tag.chunkX, tag.chunkZ);

            for (ScriptQueue queue : ScriptQueue.getAllQueues()) {
                Location anchor = queue.getAnchorLocation();
                if (anchor != null && anchor.getWorld() == tag.world) {
                    if (isTargetCurrentThread) {
                        try {
                            if (Bukkit.isOwnedByCurrentRegion(anchor)) {
                                list.addObject(new QueueTag(queue));
                            }
                        } catch (Exception ignored) {}
                    } else if (targetNode != null) {
                        Object qNode = getRegionNode(tag.world, anchor.getBlockX() >> 4, anchor.getBlockZ() >> 4);
                        if (isSameRegion(targetNode, qNode)) {
                            list.addObject(new QueueTag(queue));
                        }
                    }
                }
            }
        }

        public static List<RegionTag> getAllRegions(World world) {
            List<RegionTag> results = new ArrayList<>();
            try {
                Object nmsWorld = world.getClass().getMethod("getHandle").invoke(world);
                Object regioniser = null;

                try {
                    Object scheduler = nmsWorld.getClass().getField("chunkTaskScheduler").get(nmsWorld);
                    regioniser = scheduler.getClass().getField("regioniser").get(scheduler);
                } catch (Exception ignored) {
                    try {
                        regioniser = nmsWorld.getClass().getField("tickRegions").get(nmsWorld);
                    } catch (Exception ignored2) {}
                }

                if (regioniser != null) {
                    Field mapField = null;
                    for (Field f : regioniser.getClass().getDeclaredFields()) {
                        if (f.getType().getName().contains("Long2Object") || f.getName().equals("regionMap")) {
                            mapField = f;
                            break;
                        }
                    }

                    if (mapField != null) {
                        mapField.setAccessible(true);
                        Map<?, ?> map = (Map<?, ?>) mapField.get(regioniser);
                        Set<String> seen = new HashSet<>();

                        for (Object node : map.values()) {
                            String centerId = CenterExtractor.getCenterId(node);
                            if (centerId != null && seen.add(centerId)) {
                                String[] parts = centerId.split(",");
                                results.add(new RegionTag(world, Integer.parseInt(parts[0]), Integer.parseInt(parts[1])));
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Set<String> seen = new HashSet<>();
                for (Player p : world.getPlayers()) {
                    RegionTag rt = new RegionTag(p.getWorld(), p.getLocation().getBlockX() >> 4, p.getLocation().getBlockZ() >> 4);
                    if (seen.add(rt.identify())) results.add(rt);
                }
            }
            return results;
        }
    }

    private static class CenterExtractor {
        private static volatile boolean inited = false;
        private static Field centerField;
        private static Method centerMethod;
        private static Method idMethod;

        private static Method posX;
        private static Method posZ;
        private static Field fieldX;
        private static Field fieldZ;

        private static synchronized void init(Class<?> clazz) {
            if (inited) return;

            for (Field f : clazz.getDeclaredFields()) {
                String name = f.getName().toLowerCase();
                if (name.contains("center") || name.contains("pos") || name.contains("coord") || name.equals("section")) {
                    f.setAccessible(true);
                    centerField = f;
                    break;
                }
            }

            for (Method m : clazz.getDeclaredMethods()) {
                String name = m.getName().toLowerCase();
                if (m.getParameterCount() == 0) {
                    if (name.contains("center") || name.contains("pos")) {
                        m.setAccessible(true);
                        centerMethod = m;
                    } else if (name.equals("id") || name.equals("getid")) {
                        m.setAccessible(true);
                        idMethod = m;
                    }
                }
            }

            inited = true;
        }

        private static String getCenterId(Object region) {
            if (region == null) return null;
            if (!inited) {
                init(region.getClass());
            }

            try {
                Object center = null;
                if (centerField != null) {
                    center = centerField.get(region);
                } else if (centerMethod != null) {
                    center = centerMethod.invoke(region);
                }

                if (center != null) {
                    try {
                        if (posX == null) posX = center.getClass().getMethod("x");
                        if (posZ == null) posZ = center.getClass().getMethod("z");
                        return posX.invoke(center) + "," + posZ.invoke(center);
                    } catch (Exception e) {
                        try {
                            if (fieldX == null) {
                                fieldX = center.getClass().getDeclaredField("x");
                                fieldZ = center.getClass().getDeclaredField("z");
                                fieldX.setAccessible(true);
                                fieldZ.setAccessible(true);
                            }
                            return fieldX.get(center) + "," + fieldZ.get(center);
                        } catch (Exception ignored) {}
                    }
                    return center.toString();
                }

                if (idMethod != null) {
                    return String.valueOf(idMethod.invoke(region));
                }
            } catch (Exception ignored) {}

            return null;
        }
    }
}