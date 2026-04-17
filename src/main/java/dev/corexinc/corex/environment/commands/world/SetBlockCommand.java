package dev.corexinc.corex.environment.commands.world;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.tags.world.MaterialTag;
import dev.corexinc.corex.environment.tags.world.area.AbstractAreaObject;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

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
        String locationsRaw = instruction.getLinear(0, queue);
        String materialsRaw = instruction.getLinear(1, queue);
        String naturallyRaw = instruction.getPrefix("naturally", queue);
        String maxDelayMsRaw = instruction.getPrefix("max_delay_ms", queue);
        String chanceRaw = instruction.getPrefix("chance", queue);
        boolean noPhysics = instruction.getPrefix("no_physics", queue) != null;
        boolean delayed = instruction.getPrefix("delayed", queue) != null;

        if (locationsRaw == null) {
            Debugger.echoError(queue, "Locations cannot be null!");
            return;
        }
        if (materialsRaw == null) {
            Debugger.echoError(queue, "Materials cannot be null!");
            return;
        }

        List<Location> blocks = resolveLocations(locationsRaw);
        if (blocks.isEmpty()) {
            Debugger.echoError(queue, "No valid locations resolved for setblock!");
            return;
        }

        List<MaterialTag> materials = resolveMaterials(materialsRaw);
        if (materials.isEmpty()) {
            Debugger.echoError(queue, "No valid materials resolved for setblock!");
            return;
        }

        List<Double> chances = chanceRaw != null ? resolveChances(chanceRaw, materials.size()) : null;

        ItemStack naturalTool = null;
        if (naturallyRaw != null) {
            MaterialTag toolMaterial = new MaterialTag(naturallyRaw);
            naturalTool = new ItemStack(toolMaterial.getMaterial());
        }

        int maxDelayMs = DEFAULT_MAX_DELAY_MS;
        if (maxDelayMsRaw != null) {
            try {
                maxDelayMs = Integer.parseInt(maxDelayMsRaw);
            } catch (NumberFormatException ignored) {
                Debugger.echoError(queue, "Invalid max_delay_ms '" + maxDelayMsRaw + "', defaulting to " + DEFAULT_MAX_DELAY_MS + ".");
            }
        }

        Debugger.report(queue, instruction,
                "Blocks", String.valueOf(blocks.size()),
                "Materials", materialsRaw,
                "NoPhysics", String.valueOf(noPhysics),
                "Naturally", naturallyRaw != null ? naturallyRaw : "None",
                "Delayed", String.valueOf(delayed),
                "IsWaitable", instruction.isWaitable
        );

        final ItemStack finalTool = naturalTool;
        final int finalMaxMs = maxDelayMs;

        if (delayed) {
            if (instruction.isWaitable) queue.pause();
            applyDelayed(queue, instruction, blocks, materials, chances, noPhysics, finalTool, finalMaxMs, 0);
        } else {
            for (Location loc : blocks) {
                placeBlock(loc, materials, chances, noPhysics, finalTool);
            }
            if (instruction.isWaitable) queue.resume();
        }
    }

    private void applyDelayed(ScriptQueue queue, Instruction instruction,
                              List<Location> blocks, List<MaterialTag> materials,
                              List<Double> chances, boolean noPhysics,
                              ItemStack tool, int maxMs, int fromIndex) {
        Location anchor = blocks.get(fromIndex);

        SchedulerAdapter.runAt(anchor, () -> {
            long tickStart = System.currentTimeMillis();
            int i = fromIndex;

            while (i < blocks.size()) {
                placeBlock(blocks.get(i), materials, chances, noPhysics, tool);
                i++;
                if (System.currentTimeMillis() - tickStart >= maxMs) break;
            }

            if (i < blocks.size()) {
                final int next = i;
                SchedulerAdapter.runLaterAt(blocks.get(next),
                        () -> applyDelayed(queue, instruction, blocks, materials, chances, noPhysics, tool, maxMs, next),
                        1L);
            } else if (instruction.isWaitable) {
                queue.resume();
            }
        });
    }

    private void placeBlock(Location loc, List<MaterialTag> materials, List<Double> chances,
                            boolean noPhysics, ItemStack tool) {
        if (loc.getWorld() == null) return;

        MaterialTag chosen = pickMaterial(materials, chances);
        if (chosen == null) return;

        Block block = loc.getBlock();

        if (tool != null) {
            block.breakNaturally(tool);
        }

        if (noPhysics) {
            block.setBlockData(chosen.getBlockData(), false);
        } else {
            block.setType(chosen.getMaterial());
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

    private List<Location> resolveLocations(String raw) {
        List<Location> result = new ArrayList<>();

        Object fetched = ObjectFetcher.pickObject(raw);
        if (fetched instanceof AbstractAreaObject area) {
            for (LocationTag lt : area.getBlocks()) result.add(lt.getLocation());
            return result;
        }

        for (AbstractTag entry : new ListTag(raw).getList()) {
            Object obj = entry instanceof LocationTag lt ? lt : ObjectFetcher.pickObject(entry.identify());
            if (obj instanceof LocationTag lt) result.add(lt.getLocation());
        }

        return result;
    }

    private List<MaterialTag> resolveMaterials(String raw) {
        List<MaterialTag> result = new ArrayList<>();

        for (AbstractTag entry : new ListTag(raw).getList()) {
            Object obj = entry instanceof MaterialTag mt ? mt : ObjectFetcher.pickObject(entry.identify());
            if (obj instanceof MaterialTag mt) {
                result.add(mt);
            } else {
                result.add(new MaterialTag(entry.identify()));
            }
        }

        return result;
    }

    private List<Double> resolveChances(String raw, int materialCount) {
        List<Double> chances = new ArrayList<>();

        for (AbstractTag entry : new ListTag(raw).getList()) {
            try {
                chances.add(Double.parseDouble(entry.identify()));
            } catch (NumberFormatException ignored) {
                chances.add(0.0);
            }
        }

        while (chances.size() < materialCount) chances.add(0.0);

        return chances;
    }
}