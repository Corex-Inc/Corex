package dev.corexinc.corex.environment.events.implementation.block;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.tags.world.MaterialTag;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name BlockPhysics
 *
 * @Events
 * <block> physics
 *
 * @Switches
 * adjacent:<block> - Only process the event if the block itself or an immediately adjacent block matches the specified material.
 *
 * @Cancellable
 *
 * @Warning
 * This event may fire extremely rapidly. Use with caution.
 * Note: Redstone wire, comparators, and repeaters are hard-coded to be ignored by this event to prevent server lag.
 *
 * @Description
 * Fires when a block undergoes a physics update (e.g. sand falling, water flowing).
 *
 * @Context
 * <context.location> - returns the LocationTag of the block.
 * <context.material> - returns the MaterialTag of the block updating.
 * <context.changedMaterial> - returns the MaterialTag of the block type that caused the physics update.
 *
 * @Usage
 * // Prevent sand from falling when updated.
 * on sand physics:
 * - return cancelled
 */
public class BlockPhysicsEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    private static final BlockFace[] ADJACENT_FACES = {
            BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN, BlockFace.SELF
    };

    @Override
    public @NotNull String getName() {
        return "BlockPhysics";
    }

    @Override
    public @NotNull String getSyntax() {
        return "<block> physics";
    }

    @Override
    public void addScript(@NotNull EventData data) {
        scripts.add(data);
    }

    @Override
    public void initListener() {
        if (!isRegistered && !scripts.isEmpty()) {
            Bukkit.getPluginManager().registerEvents(this, Corex.getInstance());
            isRegistered = true;
        }
    }

    @EventHandler
    public void onBlockPhysics(org.bukkit.event.block.BlockPhysicsEvent event) {
        Material changedType = event.getChangedType();
        if (changedType == Material.REDSTONE_WIRE || changedType == Material.COMPARATOR || changedType == Material.REPEATER) {
            return;
        }

        Block block = event.getBlock();
        String materialName = block.getType().name().toLowerCase();
        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("block", 0, materialName)) {
                continue;
            }

            String adjacentSwitch = data.getSwitch("adjacent");
            if (adjacentSwitch != null) {
                String cleanAdjacent = adjacentSwitch.replace("minecraft:", "").toLowerCase();
                boolean found = false;
                for (BlockFace face : ADJACENT_FACES) {
                    if (block.getRelative(face).getType().name().toLowerCase().equals(cleanAdjacent)) {
                        found = true;
                        break;
                    }
                }
                if (!found) continue;
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(block.getLocation()));
                context.put("material", new MaterialTag(block));
                context.put("changedMaterial", new MaterialTag(event.getChangedType()));
            }

            ScriptQueue queue = EventRegistry.fire(data, null, context);
            if (queue.isCancelled()) event.setCancelled(true);
        }
    }

    @Override
    public void reset() {
        isRegistered = false;
        scripts.clear();
    }
}