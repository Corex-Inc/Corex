package dev.corexinc.corex.environment.events.implementation.block;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.tags.world.MaterialTag;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name PistonRetract
 *
 * @Events
 * <block> retracts
 *
 * @Cancellable
 *
 * @Description
 * Fires when a piston retracts.
 * The <block> argument can be used to match a specific type of piston (e.g. 'piston' or 'sticky_piston').
 *
 * @Context
 * <context.location> - returns the LocationTag of the piston.
 * <context.material> - returns the MaterialTag of the piston.
 * <context.sticky> - returns an ElementTag(Boolean) indicating whether the piston is sticky.
 * <context.blocks> - returns a ListTag of LocationTags of all blocks being moved by the retraction.
 * <context.retractLocation> - returns the LocationTag of the block that will be pulled (if sticky).
 * <context.direction> - returns a LocationTag (Vector) of the direction the blocks are moving.
 *
 * @Usage
 * // Prevents sticky pistons from retracting.
 * on sticky_piston retracts:
 * - return cancelled
 *
 * @Usage
 * // Narrates how many blocks are being pulled.
 * on block retracts:
 * - if <context.sticky>:
 *     - narrate "The piston pulled <context.blocks.size> block(s)!"
 */
public class PistonRetractEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "PistonRetract";
    }

    @Override
    public @NotNull String getSyntax() {
        return "<block> retracts";
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
    public void onPistonRetract(BlockPistonRetractEvent event) {
        String blockMaterial = event.getBlock().getType().name().toLowerCase();
        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("block", 0, blockMaterial) && !data.getArgument("block", 0).equalsIgnoreCase("piston")) {
                continue;
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("material", new MaterialTag(event.getBlock()));
                context.put("sticky", new ElementTag(event.isSticky()));

                context.put("direction", new LocationTag(event.getDirection().getDirection().toLocation(event.getBlock().getWorld())));

                context.put("retractLocation", new LocationTag(event.getBlock().getRelative(event.getDirection().getOppositeFace(), 2).getLocation()));

                ListTag blocksList = new ListTag();
                for (Block block : event.getBlocks()) {
                    blocksList.addObject(new LocationTag(block.getLocation()));
                }
                context.put("blocks", blocksList);
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