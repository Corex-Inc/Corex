package dev.corexinc.corex.environment.tags.world;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.Position;
import dev.corexinc.corex.environment.utils.BukkitSchedulerAdapter;
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
import java.util.concurrent.atomic.AtomicReference;

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

        TAG_PROCESSOR.registerTag(ElementTag.class, "isGlobal", (attr, obj) ->
                new ElementTag(obj.isGlobal));

        TAG_PROCESSOR.registerTag(ElementTag.class, "tps", (attr, obj) -> {
            if (Corex.isFolia() && !obj.isGlobal) {
                return new ElementTag(FoliaSupport.getTPS(obj));
            }
            return new ElementTag(Bukkit.getServer().getTPS()[0]);
        }).ignoreTest();

        TAG_PROCESSOR.registerTag(ElementTag.class, "mspt", (attr, obj) ->
                new ElementTag(Bukkit.getServer().getAverageTickTime())).ignoreTest();

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

        TAG_PROCESSOR.registerTag(ListTag.class, "queues", (attr, obj) -> {
            ListTag list = new ListTag();
            if (obj.isGlobal) {
                for (ScriptQueue queue : ScriptQueue.getAllQueues()) {
                    if (queue.getAnchorPosition() == null) list.addObject(new QueueTag(queue));
                }
                return list;
            }
            if (obj.world == null) return list;

            if (Corex.isFolia()) {
                FoliaSupport.fillQueues(obj, list);
            } else {
                UUID worldId = obj.world.getUID();
                for (ScriptQueue queue : ScriptQueue.getAllQueues()) {
                    Position anchor = queue.getAnchorPosition();
                    if (anchor != null && worldId.equals(anchor.world())) {
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

        @FunctionalInterface
        private interface RegionNodeResolver {
            Object resolve(World world, int cx, int cz) throws Exception;
        }

        private static final AtomicReference<RegionNodeResolver> cachedResolver = new AtomicReference<>();
        private static volatile Method cachedTpsLocMethod;
        private static volatile Method cachedTpsWorldMethod;
        private static volatile boolean tpsProbed = false;

        private static Object getRegionNode(World world, int cx, int cz) {
            RegionNodeResolver resolver = cachedResolver.get();
            if (resolver != null) {
                try {
                    return resolver.resolve(world, cx, cz);
                } catch (Exception e) {
                    return null;
                }
            }
            return probeAndResolve(world, cx, cz);
        }

        private static synchronized Object probeAndResolve(World world, int cx, int cz) {
            RegionNodeResolver existing = cachedResolver.get();
            if (existing != null) {
                try { return existing.resolve(world, cx, cz); } catch (Exception e) { return null; }
            }

            try {
                Method getRegionizer = world.getClass().getMethod("getRegionizer");
                Object regionizer = getRegionizer.invoke(world);
                if (regionizer != null) {
                    Method getAt = findMethod(regionizer.getClass(), "getRegionAtSynchronised", "getRegionAt");
                    if (getAt != null) {
                        Object result = getAt.invoke(regionizer, cx, cz);
                        Method getAtFinal = getAt;
                        Method getRegionizerFinal = getRegionizer;
                        cachedResolver.set((w, x, z) -> {
                            Object reg = getRegionizerFinal.invoke(w);
                            return reg != null ? getAtFinal.invoke(reg, x, z) : null;
                        });
                        return result;
                    }
                }
            } catch (Exception ignored) {}

            try {
                Method getHandle = world.getClass().getMethod("getHandle");
                Object nmsWorld = getHandle.invoke(world);
                Method getAt = findMethod(nmsWorld.getClass(), "getRegionAtSynchronised", "getRegionAt");
                if (getAt != null) {
                    Object result = getAt.invoke(nmsWorld, cx, cz);
                    cachedResolver.set((w, x, z) -> {
                        Object nms = getHandle.invoke(w);
                        return getAt.invoke(nms, x, z);
                    });
                    return result;
                }
            } catch (Exception ignored) {}

            try {
                Method getHandle = world.getClass().getMethod("getHandle");
                Object nmsWorld = getHandle.invoke(world);
                Field tickRegionsField = nmsWorld.getClass().getField("tickRegions");
                Object tickRegions = tickRegionsField.get(nmsWorld);
                Method getAt = tickRegions.getClass().getMethod("getRegionAt", int.class, int.class);
                Object result = getAt.invoke(tickRegions, cx, cz);
                cachedResolver.set((w, x, z) -> {
                    Object nms = getHandle.invoke(w);
                    Object tr = tickRegionsField.get(nms);
                    return getAt.invoke(tr, x, z);
                });
                return result;
            } catch (Exception ignored) {}

            try {
                Method getHandle = world.getClass().getMethod("getHandle");
                Object nmsWorld = getHandle.invoke(world);
                Field schedulerField = nmsWorld.getClass().getField("chunkTaskScheduler");
                Object scheduler = schedulerField.get(nmsWorld);
                Field regioniserField = scheduler.getClass().getField("regioniser");
                Object regioniser = regioniserField.get(scheduler);
                Method getAt = regioniser.getClass().getMethod("getRegionAt", int.class, int.class);
                Object result = getAt.invoke(regioniser, cx, cz);
                cachedResolver.set((w, x, z) -> {
                    Object nms = getHandle.invoke(w);
                    Object sched = schedulerField.get(nms);
                    Object reg = regioniserField.get(sched);
                    return getAt.invoke(reg, x, z);
                });
                return result;
            } catch (Exception ignored) {}

            cachedResolver.set((w, x, z) -> null);
            return null;
        }

        private static Method findMethod(Class<?> cls, String... names) {
            for (String name : names) {
                try {
                    return cls.getMethod(name, int.class, int.class);
                } catch (NoSuchMethodException ignored) {}
            }
            return null;
        }

        private static String identifyNode(Object node) {
            if (node == null) return null;
            String id = CenterExtractor.getCenterId(node);
            return id != null ? id : String.valueOf(node.hashCode());
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
            if (!tpsProbed) probeTpsMethods(tag);

            Location loc = new Location(tag.world, tag.chunkX << 4, 0, tag.chunkZ << 4);
            try {
                if (cachedTpsLocMethod != null) {
                    double[] tps = (double[]) cachedTpsLocMethod.invoke(Bukkit.getServer(), loc);
                    if (tps != null) return tps[0];
                }
                if (cachedTpsWorldMethod != null) {
                    double[] tps = (double[]) cachedTpsWorldMethod.invoke(Bukkit.getServer(), tag.world, tag.chunkX, tag.chunkZ);
                    if (tps != null) return tps[0];
                }
            } catch (Exception ignored) {}

            return Bukkit.getServer().getTPS()[0];
        }

        private static synchronized void probeTpsMethods(RegionTag tag) {
            if (tpsProbed) return;
            try {
                cachedTpsLocMethod = Bukkit.getServer().getClass().getMethod("getRegionTPS", Location.class);
            } catch (Exception ignored) {}
            try {
                cachedTpsWorldMethod = Bukkit.getServer().getClass().getMethod("getRegionTPS", World.class, int.class, int.class);
            } catch (Exception ignored) {}
            tpsProbed = true;
        }

        private static void fillPlayers(RegionTag tag, ListTag list) {
            Location targetLoc = new Location(tag.world, tag.chunkX << 4, 0, tag.chunkZ << 4);
            boolean isTargetCurrentThread = false;
            try { isTargetCurrentThread = Bukkit.isOwnedByCurrentRegion(targetLoc); } catch (Exception ignored) {}

            Object targetNode = isTargetCurrentThread ? null : getRegionNode(tag.world, tag.chunkX, tag.chunkZ);

            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld() != tag.world) continue;
                Location pLoc;
                try { pLoc = p.getLocation(); } catch (Exception e) { continue; }

                if (isTargetCurrentThread) {
                    try {
                        if (Bukkit.isOwnedByCurrentRegion(pLoc)) list.addObject(new PlayerTag(p));
                    } catch (Exception ignored) {}
                } else if (targetNode != null) {
                    Object pNode = getRegionNode(tag.world, pLoc.getBlockX() >> 4, pLoc.getBlockZ() >> 4);
                    if (isSameRegion(targetNode, pNode)) list.addObject(new PlayerTag(p));
                }
            }
        }

        private static void fillQueues(RegionTag tag, ListTag list) {
            Location targetLoc = new Location(tag.world, tag.chunkX << 4, 0, tag.chunkZ << 4);
            boolean isTargetCurrentThread = false;
            try { isTargetCurrentThread = Bukkit.isOwnedByCurrentRegion(targetLoc); } catch (Exception ignored) {}

            Object targetNode = isTargetCurrentThread ? null : getRegionNode(tag.world, tag.chunkX, tag.chunkZ);
            UUID worldId = tag.world.getUID();

            for (ScriptQueue queue : ScriptQueue.getAllQueues()) {
                Position anchor = queue.getAnchorPosition();
                if (anchor == null || !worldId.equals(anchor.world())) continue;

                Location anchorLoc = BukkitSchedulerAdapter.toLocation(anchor);

                if (isTargetCurrentThread) {
                    try {
                        if (Bukkit.isOwnedByCurrentRegion(anchorLoc)) list.addObject(new QueueTag(queue));
                    } catch (Exception ignored) {}
                } else if (targetNode != null) {
                    Object qNode = getRegionNode(tag.world, anchorLoc.getBlockX() >> 4, anchorLoc.getBlockZ() >> 4);
                    if (isSameRegion(targetNode, qNode)) list.addObject(new QueueTag(queue));
                }
            }
        }

        public static List<RegionTag> getAllRegions(World world) {
            List<RegionTag> results = new ArrayList<>();
            try {
                Method getHandle = world.getClass().getMethod("getHandle");
                Object nmsWorld = getHandle.invoke(world);
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
            if (!inited) init(region.getClass());

            try {
                Object center = null;
                if (centerField != null) center = centerField.get(region);
                else if (centerMethod != null) center = centerMethod.invoke(region);

                if (center != null) {
                    try {
                        if (posX == null) posX = center.getClass().getMethod("x");
                        if (posZ == null) posZ = center.getClass().getMethod("z");
                        return posX.invoke(center) + "," + posZ.invoke(center);
                    } catch (Exception ignored) {
                        try {
                            if (fieldX == null) {
                                fieldX = center.getClass().getDeclaredField("x");
                                fieldZ = center.getClass().getDeclaredField("z");
                                fieldX.setAccessible(true);
                                fieldZ.setAccessible(true);
                            }
                            return fieldX.get(center) + "," + fieldZ.get(center);
                        } catch (Exception ignored2) {}
                    }
                    return center.toString();
                }

                if (idMethod != null) return String.valueOf(idMethod.invoke(region));
            } catch (Exception ignored) {}

            return null;
        }
    }
}