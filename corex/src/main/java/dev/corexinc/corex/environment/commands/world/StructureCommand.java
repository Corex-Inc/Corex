package dev.corexinc.corex.environment.commands.world;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.tags.world.StructureTag;
import dev.corexinc.corex.environment.tags.world.area.AbstractAreaObject;
import dev.corexinc.corex.environment.tags.world.area.CuboidTag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.structure.Structure;
import org.bukkit.structure.StructureManager;
import org.bukkit.util.BlockVector;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class StructureCommand implements AbstractCommand, Listener {

    private record Bounds(Location origin, BlockVector size) {}
    private static final ThreadLocal<Boolean> DISABLE_PHYSICS = ThreadLocal.withInitial(() -> false);

    public StructureCommand() {
        Bukkit.getPluginManager().registerEvents(this, Corex.getInstance());
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPhysics(BlockPhysicsEvent event) {
        if (DISABLE_PHYSICS.get()) {
            event.setCancelled(true);
        }
    }

    @Override public @NonNull String getName()     { return "structure"; }
    @Override public int getMinArgs()              { return 1; }
    @Override public int getMaxArgs()              { return 11; }
    @Override public boolean setCanBeWaitable()    { return true; }

    @Override
    public @NonNull String getSyntax() {
        return "[place/delete/create] [structure:<structure>] (location:<location>) (offset:<location>) (centered) (rotation:<rotation>) (mirror:<mirror>) (palette:<#>) (integrity:<#>) (saveToDisk) (includeEntities) (noPhysics) (area:<area>)";
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        String action = instruction.getLinear(0, queue);

        if (action == null) {
            Debugger.echoError(queue, "Action cannot be null!");
            return;
        }

        String structureRaw = instruction.getPrefix("structure", queue);
        if (structureRaw == null) structureRaw = instruction.getLinear(1, queue);
        if (structureRaw == null) {
            Debugger.echoError(queue, "Structure cannot be null! Please specify structure:<structure>.");
            return;
        }

        NamespacedKey key = resolveKey(queue, structureRaw);
        if (key == null) return;

        switch (action.toLowerCase()) {
            case "place"  -> handlePlace(queue, instruction, new StructureTag(key));
            case "delete" -> handleDelete(queue, instruction, new StructureTag(key));
            case "create" -> handleCreate(queue, instruction, key);
            default       -> Debugger.echoError(queue, "Unknown action '" + action + "'. Valid: place, delete, create.");
        }
    }

    private void handlePlace(ScriptQueue queue, Instruction instruction, StructureTag structureTag) {
        Structure structure = structureTag.getStructure();
        if (structure == null) {
            Debugger.echoError(queue, "Structure '" + structureTag.identify() + "' could not be loaded!");
            return;
        }

        String locationRaw  = instruction.getPrefix("location",  queue);
        String offsetRaw    = instruction.getPrefix("offset",    queue);
        String rotationRaw  = instruction.getPrefix("rotation",  queue);
        String mirrorRaw    = instruction.getPrefix("mirror",    queue);
        String paletteRaw   = instruction.getPrefix("palette",   queue);
        String integrityRaw = instruction.getPrefix("integrity", queue);

        boolean includeEntities = instruction.hasFlag("include_entities");
        boolean centered        = instruction.hasFlag("centered");
        boolean noPhysics       = instruction.hasFlag("no_physics");

        if (locationRaw == null) {
            Debugger.echoError(queue, "location: is required for 'place'!");
            return;
        }

        StructureRotation rotation = parseRotation(queue, rotationRaw);
        if (rotation == null) return;

        Mirror mirror = parseMirror(queue, mirrorRaw);
        if (mirror == null) return;

        int palette = parsePalette(queue, paletteRaw);
        if (paletteRaw != null && palette == Integer.MIN_VALUE) return;

        float integrity = parseIntegrity(queue, integrityRaw);
        if (integrityRaw != null && integrity < 0) return;

        Object fetchedLoc = ObjectFetcher.pickObject(locationRaw);
        LocationTag locationTag = fetchedLoc instanceof LocationTag lt ? lt : new LocationTag(locationRaw);
        Location loc = locationTag.getLocation().clone();

        if (loc.getWorld() == null) {
            Debugger.echoError(queue, "Location has no world!");
            return;
        }

        if (centered) {
            BlockVector size = structure.getSize();
            double cx = size.getBlockX() / 2.0;
            double cy = size.getBlockY() / 2.0;
            double cz = size.getBlockZ() / 2.0;

            if (mirror == Mirror.FRONT_BACK) cx = -cx;
            else if (mirror == Mirror.LEFT_RIGHT) cz = -cz;

            double shiftX = cx, shiftZ = cz;
            switch (rotation) {
                case CLOCKWISE_90 -> { shiftX = -cz; shiftZ = cx; }
                case CLOCKWISE_180 -> { shiftX = -cx; shiftZ = -cz; }
                case COUNTERCLOCKWISE_90 -> { shiftX = cz; shiftZ = -cx; }
                case NONE -> {}
            }
            loc.subtract(shiftX, cy, shiftZ);
        }

        String offsetIdentify = "none";
        if (offsetRaw != null) {
            Object fetchedOffset = ObjectFetcher.pickObject(offsetRaw);
            LocationTag offsetTag = fetchedOffset instanceof LocationTag lt ? lt : new LocationTag(offsetRaw);
            Location offsetLoc = offsetTag.getLocation();
            loc.add(offsetLoc.getX(), offsetLoc.getY(), offsetLoc.getZ());
            offsetIdentify = offsetTag.identify();
        }

        Debugger.report(queue, instruction,
                "Action",          "place",
                "Structure",       structureTag.identify(),
                "Location",        locationTag.identify(),
                "Offset",          offsetIdentify,
                "Centered",        String.valueOf(centered),
                "Rotation",        rotation.name(),
                "Mirror",          mirror.name(),
                "Palette",         String.valueOf(palette),
                "Integrity",       String.valueOf(integrity),
                "IncludeEntities", String.valueOf(includeEntities),
                "NoPhysics",       String.valueOf(noPhysics),
                "IsWaitable",      instruction.isWaitable
        );

        if (instruction.isWaitable) queue.pause();

        SchedulerAdapter.runAt(loc, () -> {
            try {
                if (noPhysics) {
                    DISABLE_PHYSICS.set(true);
                }
                structure.place(loc, includeEntities, rotation, mirror, palette, integrity, ThreadLocalRandom.current());
            } catch (Exception e) {
                Debugger.echoError(queue, "Failed to place structure: " + e.getMessage());
            } finally {
                if (noPhysics) {
                    DISABLE_PHYSICS.remove();
                }
                if (instruction.isWaitable) queue.resume();
            }
        });
    }

    private void handleDelete(ScriptQueue queue, Instruction instruction, StructureTag structureTag) {
        NamespacedKey key = structureTag.getKey();
        if (key == null) {
            Debugger.echoError(queue, "Cannot resolve key from '" + structureTag.identify() + "'!");
            return;
        }

        Debugger.report(queue, instruction,
                "Action",    "delete",
                "Structure", structureTag.identify()
        );

        if (instruction.isWaitable) queue.pause();

        SchedulerAdapter.runAsync(() -> {
            try {
                Bukkit.getStructureManager().deleteStructure(key, false);
            } catch (Exception e) {
                Debugger.echoError(queue, "Failed to delete structure file for '" + key + "': " + e.getMessage());
            }
            SchedulerAdapter.run(() -> {
                Bukkit.getStructureManager().unregisterStructure(key);
                if (instruction.isWaitable) queue.resume();
            });
        });
    }

    private void handleCreate(ScriptQueue queue, Instruction instruction, NamespacedKey key) {
        String areaRaw          = instruction.getPrefix("area",             queue);
        String locationRaw      = instruction.getPrefix("location",         queue);

        boolean saveToDisk      = instruction.hasFlag("save_to_disk");
        boolean includeEntities = instruction.hasFlag("include_entities");

        if (areaRaw == null) {
            Debugger.echoError(queue, "area: is required for 'create'!");
            return;
        }

        Object fetchedArea = ObjectFetcher.pickObject(areaRaw);
        if (!(fetchedArea instanceof AbstractAreaObject area)) {
            Debugger.echoError(queue, "area: must resolve to a valid area object (CuboidTag, EllipsoidTag, PolygonTag)!");
            return;
        }

        Bounds bounds = computeBounds(area);
        if (bounds == null) {
            Debugger.echoError(queue, "Area contains no blocks!");
            return;
        }
        if (bounds.origin().getWorld() == null) {
            Debugger.echoError(queue, "Area has no valid world!");
            return;
        }
        Location origin;
        if (locationRaw != null) {
            Object fetchedLoc = ObjectFetcher.pickObject(locationRaw);
            LocationTag locationTag = fetchedLoc instanceof LocationTag lt ? lt : new LocationTag(locationRaw);
            origin = locationTag.getLocation();
            if (origin.getWorld() == null) origin.setWorld(bounds.origin().getWorld());
            if (origin.getWorld() == null) {
                Debugger.echoError(queue, "location: has no world and the area provides no fallback world!");
                return;
            }
        } else {
            origin = bounds.origin();
        }

        Debugger.report(queue, instruction,
                "Action",          "create",
                "Structure",       key,
                "Origin",          new LocationTag(origin).identify(),
                "Size",            bounds.size().getBlockX() + "x" + bounds.size().getBlockY() + "x" + bounds.size().getBlockZ(),
                "IncludeEntities", includeEntities,
                "SaveToDisk",      saveToDisk,
                "IsWaitable",      instruction.isWaitable
        );

        if (instruction.isWaitable) queue.pause();

        SchedulerAdapter.runAt(origin, () -> {
            try {
                StructureManager manager = Bukkit.getStructureManager();
                Structure structure = manager.createStructure();
                structure.fill(origin, bounds.size(), includeEntities);

                SchedulerAdapter.runAsync(() -> {
                    if (saveToDisk) {
                        try {
                            manager.saveStructure(key, structure);
                        } catch (Exception e) {
                            Debugger.echoError(queue, "Failed to write structure to disk: " + e.getMessage());
                        }
                    }

                    SchedulerAdapter.run(() -> {
                        try {
                            manager.registerStructure(key, structure);
                        } finally {
                            if (instruction.isWaitable) queue.resume();
                        }
                    });
                });

            } catch (Exception e) {
                Debugger.echoError(queue, "Failed to fill structure: " + e.getMessage());
                if (instruction.isWaitable) queue.resume();
            }
        });
    }

    private @Nullable NamespacedKey resolveKey(ScriptQueue queue, String raw) {
        Object fetched = ObjectFetcher.pickObject(raw);
        NamespacedKey key;
        if (fetched instanceof StructureTag st) {
            key = st.getKey();
        } else {
            key = NamespacedKey.fromString(new StructureTag(raw).getKey() != null
                    ? new StructureTag(raw).getKey().toString()
                    : raw.toLowerCase());
        }
        if (key == null) {
            Debugger.echoError(queue, "Invalid namespaced key: '" + raw + "'.");
            return null;
        }
        return key;
    }

    private @Nullable StructureRotation parseRotation(ScriptQueue queue, @Nullable String raw) {
        if (raw == null) return StructureRotation.NONE;
        try {
            return StructureRotation.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            Debugger.echoError(queue, "Invalid rotation '" + raw + "'. Valid: NONE, CLOCKWISE_90, CLOCKWISE_180, COUNTERCLOCKWISE_90.");
            return null;
        }
    }

    private @Nullable Mirror parseMirror(ScriptQueue queue, @Nullable String raw) {
        if (raw == null) return Mirror.NONE;
        try {
            return Mirror.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            Debugger.echoError(queue, "Invalid mirror '" + raw + "'. Valid: NONE, LEFT_RIGHT, FRONT_BACK.");
            return null;
        }
    }

    private int parsePalette(ScriptQueue queue, @Nullable String raw) {
        if (raw == null) return -1;
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            Debugger.echoError(queue, "Invalid palette: " + raw);
            return Integer.MIN_VALUE;
        }
    }

    private float parseIntegrity(ScriptQueue queue, @Nullable String raw) {
        if (raw == null) return 1.0f;
        try {
            float value = Float.parseFloat(raw);
            if (value < 0.0f || value > 1.0f) {
                Debugger.echoError(queue, "Integrity must be between 0.0 and 1.0.");
                return -1f;
            }
            return value;
        } catch (NumberFormatException e) {
            Debugger.echoError(queue, "Invalid integrity: " + raw);
            return -1f;
        }
    }

    private @Nullable Bounds computeBounds(AbstractAreaObject area) {
        if (area instanceof CuboidTag cuboid) {
            return computeCuboidBounds(cuboid);
        }
        return computeGenericBounds(area);
    }

    private @Nullable Bounds computeCuboidBounds(CuboidTag cuboid) {
        List<double[][]> members = cuboid.getMembers();
        if (members.isEmpty()) return null;

        double minX = Double.MAX_VALUE,  minY = Double.MAX_VALUE,  minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE, maxY = -Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;

        for (double[][] member : members) {
            for (double[] point : member) {
                if (point[0] < minX) minX = point[0]; if (point[0] > maxX) maxX = point[0];
                if (point[1] < minY) minY = point[1]; if (point[1] > maxY) maxY = point[1];
                if (point[2] < minZ) minZ = point[2]; if (point[2] > maxZ) maxZ = point[2];
            }
        }

        int ox = (int) Math.floor(minX), oy = (int) Math.floor(minY), oz = (int) Math.floor(minZ);
        return new Bounds(
                new Location(cuboid.getWorld(), ox, oy, oz),
                new BlockVector(
                        (int) Math.floor(maxX) - ox + 1,
                        (int) Math.floor(maxY) - oy + 1,
                        (int) Math.floor(maxZ) - oz + 1
                )
        );
    }

    private @Nullable Bounds computeGenericBounds(AbstractAreaObject area) {
        List<LocationTag> blocks = area.getBlocks();
        if (blocks.isEmpty()) return null;

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        World world = null;

        for (LocationTag locationTag : blocks) {
            Location loc = locationTag.getLocation();
            if (world == null) world = loc.getWorld();
            int bx = loc.getBlockX(), by = loc.getBlockY(), bz = loc.getBlockZ();
            if (bx < minX) minX = bx; if (bx > maxX) maxX = bx;
            if (by < minY) minY = by; if (by > maxY) maxY = by;
            if (bz < minZ) minZ = bz; if (bz > maxZ) maxZ = bz;
        }

        return new Bounds(
                new Location(world, minX, minY, minZ),
                new BlockVector(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1)
        );
    }
}