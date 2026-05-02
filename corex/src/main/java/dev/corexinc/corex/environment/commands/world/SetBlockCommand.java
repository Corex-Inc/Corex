package dev.corexinc.corex.environment.commands.world;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.generators.ScriptedChunkGenerator;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.tags.world.MaterialTag;
import dev.corexinc.corex.environment.tags.world.area.AbstractAreaObject;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/* @doc command
 *
 * @Name SetBlock
 * @Syntax setblock [<location>|.../<area>] [<material>|...] (no_physics) (naturally:<tool>) (delayed) (max_delay_ms:<#>) (chance:<percent>|...)
 * @RequiredArgs 2
 * @MaxArgs 6
 * @ShortDescription Sets blocks in the world.
 *
 * @Implements ModifyBlock
 *
 * @Description
 * Changes blocks in the world at the given location(s) or area object.
 *
 * Accepts a single location, a pipe-separated list of locations,
 * or an area object (CuboidTag, EllipsoidTag, PolygonTag).
 *
 * Specify (no_physics) to place blocks without triggering physics updates.
 * Useful for blocks like water, portals, or floating sand.
 * This does NOT suppress physics for an extended period of time.
 *
 * Specify (naturally:<tool>) to break each block naturally before placing the new one,
 * causing it to drop items as if mined with the given tool.
 * Use 'air' as the material alongside this to simply break blocks with drops.
 *
 * Specify (delayed) to spread block modifications across ticks, avoiding lag on large regions.
 * Optionally specify (max_delay_ms:<#>) to control how many milliseconds are spent per tick (defaults to 50).
 *
 * Specify (chance:<percent>|...) to assign a probability to each material.
 * Percentages correspond positionally to the material list.
 * If the total is below 100, the remaining probability results in no change.
 *
 * The setblock command is ~waitable. Refer to Language:~waitable.
 *
 * When called from inside a chunk generator section, the command writes directly to the
 * ChunkData buffer. In that context, no_physics, naturally, and delayed are ignored.
 *
 * @Usage
 * // Set the block at the player's location to stone.
 * - setblock <player.location> stone
 *
 * @Usage
 * // Fill a cuboid with stone or dirt, equal chance.
 * - setblock <[myCuboid]> stone|dirt chance:50|50
 *
 * @Usage
 * // Break all blocks in an ellipsoid naturally using a diamond pickaxe.
 * - setblock <[myEllipsoid]> air naturally:diamond_pickaxe
 *
 * @Usage
 * // Slowly fill a large area with glass, spending at most 30ms per tick.
 * - ~setblock <[myCuboid]> glass delayed max_delay_ms:30
 */
public class SetBlockCommand implements AbstractCommand {

    private static final int DEFAULT_MAX_DELAY_MS = 50;

    private record PlacementContext(
            List<Location> allBlocks,
            List<List<Location>> chunkGroups,
            List<MaterialTag> materials,
            List<Double> chances,
            boolean noPhysics,
            ItemStack tool,
            int maxMs
    ) {}

    @Override
    public @NonNull String getName() { return "setblock"; }

    @Override
    public @NonNull String getSyntax() {
        return "[<location>|.../<area>] [<material>|...] (no_physics) (naturally:<tool>) (delayed) (max_delay_ms:<#>) (chance:<percent>|...)";
    }

    @Override
    public int getMinArgs() { return 2; }

    @Override
    public int getMaxArgs() { return 6; }

    @Override
    public boolean setCanBeWaitable() { return true; }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        String locationsRaw  = instruction.getLinear(0, queue);
        String materialsRaw  = instruction.getLinear(1, queue);

        if (locationsRaw == null) { Debugger.echoError(queue, "Locations cannot be null!"); return; }
        if (materialsRaw == null) { Debugger.echoError(queue, "Materials cannot be null!"); return; }

        Object rawChunkData = queue.getTempData(ScriptedChunkGenerator.TEMP_CHUNK_DATA);
        if (rawChunkData instanceof ChunkGenerator.ChunkData chunkData) {
            runGeneratorSetBlock(queue, instruction, locationsRaw, materialsRaw,
                    instruction.getPrefix("chance", queue), chunkData);
            return;
        }

        String naturallyRaw  = instruction.getPrefix("naturally", queue);
        String maxDelayMsRaw = instruction.getPrefix("max_delay_ms", queue);
        String chanceRaw     = instruction.getPrefix("chance", queue);
        boolean noPhysics    = instruction.getPrefix("no_physics", queue) != null;
        boolean delayed      = instruction.getPrefix("delayed", queue) != null;

        List<Location> blocks = resolveLocations(locationsRaw);
        if (blocks.isEmpty()) { Debugger.echoError(queue, "No valid locations resolved for setblock!"); return; }

        List<MaterialTag> materials = resolveMaterials(materialsRaw);
        if (materials.isEmpty()) { Debugger.echoError(queue, "No valid materials resolved for setblock!"); return; }

        ItemStack naturalTool = naturallyRaw != null
                ? new ItemStack(new MaterialTag(naturallyRaw).getMaterial())
                : null;

        int maxDelayMs = DEFAULT_MAX_DELAY_MS;
        if (maxDelayMsRaw != null) {
            try {
                maxDelayMs = Integer.parseInt(maxDelayMsRaw);
            } catch (NumberFormatException ignored) {
                Debugger.echoError(queue, "Invalid max_delay_ms '" + maxDelayMsRaw + "', defaulting to " + DEFAULT_MAX_DELAY_MS + ".");
            }
        }

        Debugger.report(queue, instruction,
                "Blocks",     String.valueOf(blocks.size()),
                "Materials",  materialsRaw,
                "NoPhysics",  String.valueOf(noPhysics),
                "Naturally",  naturallyRaw != null ? naturallyRaw : "None",
                "Delayed",    String.valueOf(delayed),
                "IsWaitable", instruction.isWaitable
        );

        PlacementContext ctx = new PlacementContext(
                blocks,
                Corex.isFolia() ? groupByChunk(blocks) : List.of(),
                materials,
                chanceRaw != null ? resolveChances(chanceRaw, materials.size()) : null,
                noPhysics,
                naturalTool,
                maxDelayMs
        );

        if (delayed) {
            if (instruction.isWaitable) queue.pause();
            if (Corex.isFolia()) {
                applyDelayedFolia(queue, instruction, ctx, 0, 0);
            } else {
                applyDelayedPaper(queue, instruction, ctx, 0);
            }
        } else {
            if (Corex.isFolia()) {
                applyImmediateFolia(queue, instruction, ctx);
            } else {
                applyImmediatePaper(queue, instruction, ctx);
            }
        }
    }

    private void runGeneratorSetBlock(ScriptQueue queue, Instruction instruction,
                                      String locationsRaw, String materialsRaw,
                                      String chanceRaw, ChunkGenerator.ChunkData chunkData) {
        List<Location> blocks = resolveLocations(locationsRaw);
        if (blocks.isEmpty()) { Debugger.echoError(queue, "No valid locations resolved!"); return; }

        List<MaterialTag> materials = resolveMaterials(materialsRaw);
        if (materials.isEmpty()) { Debugger.echoError(queue, "No valid materials resolved!"); return; }

        List<Double> chances = chanceRaw != null ? resolveChances(chanceRaw, materials.size()) : null;

        for (Location loc : blocks) {
            MaterialTag chosen = pickMaterial(materials, chances);
            if (chosen == null || !chosen.getMaterial().isBlock() || chosen.getBlockData() == null) continue;
            int relX = loc.getBlockX() & 15;
            int relZ = loc.getBlockZ() & 15;

            chunkData.setBlock(relX, loc.getBlockY(), relZ, chosen.getBlockData());
        }
    }

    private void applyImmediatePaper(ScriptQueue queue, Instruction instruction, PlacementContext ctx) {
        ctx.allBlocks().forEach(loc -> placeBlock(loc, ctx));
        if (instruction.isWaitable) queue.resume();
    }

    private void applyImmediateFolia(ScriptQueue queue, Instruction instruction, PlacementContext ctx) {
        if (instruction.isWaitable) queue.pause();
        AtomicInteger remaining = new AtomicInteger(ctx.chunkGroups().size());
        for (List<Location> group : ctx.chunkGroups()) {
            SchedulerAdapter.runAt(group.getFirst(), () -> {
                group.forEach(loc -> placeBlock(loc, ctx));
                if (instruction.isWaitable && remaining.decrementAndGet() == 0) queue.resume();
            });
        }
    }

    private void applyDelayedPaper(ScriptQueue queue, Instruction instruction,
                                   PlacementContext ctx, int fromIndex) {
        SchedulerAdapter.runLater(() -> {
            long tickStart = System.currentTimeMillis();
            int next = fromIndex;
            List<Location> blocks = ctx.allBlocks();

            while (next < blocks.size()) {
                placeBlock(blocks.get(next++), ctx);
                if (System.currentTimeMillis() - tickStart >= ctx.maxMs()) break;
            }

            if (next < blocks.size()) {
                applyDelayedPaper(queue, instruction, ctx, next);
            } else if (instruction.isWaitable) {
                queue.resume();
            }
        }, 1L);
    }

    private void applyDelayedFolia(ScriptQueue queue, Instruction instruction,
                                   PlacementContext ctx, int groupIndex, int blockIndex) {
        if (groupIndex >= ctx.chunkGroups().size()) {
            if (instruction.isWaitable) queue.resume();
            return;
        }

        List<Location> group = ctx.chunkGroups().get(groupIndex);
        SchedulerAdapter.runAt(group.get(blockIndex), () -> {
            long tickStart = System.currentTimeMillis();
            int i = blockIndex;

            while (i < group.size()) {
                placeBlock(group.get(i++), ctx);
                if (System.currentTimeMillis() - tickStart >= ctx.maxMs()) break;
            }

            if (i < group.size()) {
                final int nextBlock = i;
                SchedulerAdapter.runLaterAt(group.get(nextBlock),
                        () -> applyDelayedFolia(queue, instruction, ctx, groupIndex, nextBlock), 1L);
            } else {
                final int nextGroup = groupIndex + 1;
                if (nextGroup < ctx.chunkGroups().size()) {
                    SchedulerAdapter.runLaterAt(ctx.chunkGroups().get(nextGroup).getFirst(),
                            () -> applyDelayedFolia(queue, instruction, ctx, nextGroup, 0), 1L);
                } else if (instruction.isWaitable) {
                    queue.resume();
                }
            }
        });
    }

    private void placeBlock(Location loc, PlacementContext ctx) {
        if (loc.getWorld() == null) return;
        MaterialTag chosen = pickMaterial(ctx.materials(), ctx.chances());
        if (chosen == null) return;
        Block block = loc.getBlock();
        if (ctx.tool() != null) block.breakNaturally(ctx.tool());

        if (chosen.getBlockData() != null) {
            block.setBlockData(chosen.getBlockData(), !ctx.noPhysics());
        } else {
            block.setType(chosen.getMaterial(), !ctx.noPhysics());
        }
    }

    private MaterialTag pickMaterial(List<MaterialTag> materials, List<Double> chances) {
        if (chances == null) {
            return materials.size() == 1
                    ? materials.getFirst()
                    : materials.get(ThreadLocalRandom.current().nextInt(materials.size()));
        }
        double roll = ThreadLocalRandom.current().nextDouble(100.0);
        double cumulative = 0.0;
        for (int i = 0; i < materials.size(); i++) {
            cumulative += chances.get(i);
            if (roll < cumulative) return materials.get(i);
        }
        return null;
    }

    private List<List<Location>> groupByChunk(List<Location> blocks) {
        return new ArrayList<>(blocks.stream().collect(Collectors.groupingBy(
                loc -> ((long)(loc.getBlockX() >> 4) << 32) | ((loc.getBlockZ() >> 4) & 0xFFFFFFFFL),
                LinkedHashMap::new,
                Collectors.toList()
        )).values());
    }

    private List<Location> resolveLocations(String raw) {
        Object fetched = ObjectFetcher.pickObject(raw);
        if (fetched instanceof AbstractAreaObject area) {
            return area.getBlocks().stream()
                    .map(LocationTag::getLocation)
                    .collect(Collectors.toList());
        }
        return new ListTag(raw).getList().stream()
                .map(entry -> entry instanceof LocationTag lt ? lt : ObjectFetcher.pickObject(entry.identify()))
                .filter(LocationTag.class::isInstance)
                .map(LocationTag.class::cast)
                .map(LocationTag::getLocation)
                .collect(Collectors.toList());
    }

    private List<MaterialTag> resolveMaterials(String raw) {
        return new ListTag(raw).getList().stream()
                .map(entry -> {
                    Object obj = entry instanceof MaterialTag mt ? mt : ObjectFetcher.pickObject(entry.identify());
                    return obj instanceof MaterialTag mt ? mt : new MaterialTag(entry.identify());
                })
                .collect(Collectors.toList());
    }

    private List<Double> resolveChances(String raw, int materialCount) {
        List<Double> chances = new ListTag(raw).getList().stream()
                .map(entry -> {
                    try { return Double.parseDouble(entry.identify()); }
                    catch (NumberFormatException ignored) { return 0.0; }
                })
                .collect(Collectors.toCollection(ArrayList::new));
        while (chances.size() < materialCount) chances.add(0.0);
        return chances;
    }
}