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
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name PistonExtend
 *
 * @Events
 * <block> extends
 *
 * @Cancellable
 *
 * @Description
 * Fires when a piston extends.
 *
 * @Context
 * <context.location> - returns the LocationTag of the piston.
 * <context.material> - returns the MaterialTag of the piston.
 * <context.length> - returns an ElementTag of the number of blocks that will be moved.
 * <context.blocks> - returns a ListTag of all block LocationTags about to be moved.
 * <context.sticky> - returns an ElementTag(Boolean) of whether the piston is sticky.
 * <context.direction> - returns a LocationTag (vector) of the direction that blocks will move.
 *
 * @Usage
 * // Prevent sticky pistons from extending.
 * on sticky_piston extends:
 * - return cancelled
 */
public class PistonExtendEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "PistonExtend";
    }

    @Override
    public @NotNull String getSyntax() {
        return "<block> extends";
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
    public void onPistonExtends(org.bukkit.event.block.BlockPistonExtendEvent event) {
        String blockMaterial = event.getBlock().getType().name().toLowerCase();
        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("block", 0, blockMaterial)) {
                continue;
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("material", new MaterialTag(event.getBlock()));
                context.put("sticky", new ElementTag(event.isSticky()));
                context.put("length", new ElementTag(event.getBlocks().size()));

                Vector dir = event.getDirection().getDirection();
                context.put("direction", new LocationTag(new Location(null, dir.getX(), dir.getY(), dir.getZ())));

                List<String> blockLocations = new ArrayList<>();
                for (Block b : event.getBlocks()) {
                    blockLocations.add(new LocationTag(b.getLocation()).identify());
                }

                context.put("blocks", new ListTag(String.join("|", blockLocations)));
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