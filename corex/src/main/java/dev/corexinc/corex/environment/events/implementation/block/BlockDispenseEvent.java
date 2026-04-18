package dev.corexinc.corex.environment.events.implementation.block;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.world.ItemTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name BlockDispense
 *
 * @Events
 * <block> dispenses <item>
 *
 * @Cancellable
 *
 * @Description
 * Fires when a block dispenses a single item.
 *
 * @Context
 * <context.location> - returns the LocationTag of the dispenser.
 * <context.item> - returns the ItemTag of the item being dispensed.
 * <context.velocity> - returns a LocationTag vector of the velocity the item will be shot at.
 *
 * @Returns
 * LocationTag - Sets the velocity the item will be shot at.
 * ItemTag - Sets the item being shot.
 *
 * @Usage
 * // Prevents dispensers from shooting arrows.
 * on dispenser dispenses arrow:
 * - return cancelled
 *
 * @Usage
 * // Replaces dispensed dirt with stone.
 * on dispenser dispenses dirt:
 * - return i@stone
 */
public class BlockDispenseEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockDispense";
    }

    @Override
    public @NotNull String getSyntax() {
        return "<block> dispenses <item>";
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
    public void onBlockDispense(org.bukkit.event.block.BlockDispenseEvent event) {
        String blockMaterial = event.getBlock().getType().name().toLowerCase();
        String itemMaterial = event.getItem().getType().name().toLowerCase();

        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("block", 0, blockMaterial)) {
                continue;
            }
            if (!data.isGenericMatch("item", 0, itemMaterial)) {
                continue;
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("item", new ItemTag(event.getItem()));

                org.bukkit.Location velocityLoc = new org.bukkit.Location(null, event.getVelocity().getX(), event.getVelocity().getY(), event.getVelocity().getZ());
                context.put("velocity", new LocationTag(velocityLoc));
            }

            ScriptQueue queue = EventRegistry.fire(data, null, context);
            if (queue.isCancelled()) event.setCancelled(true);

            for (AbstractTag tag : queue.getReturns()) {
                if (tag instanceof ItemTag itemTag) {
                    event.setItem(itemTag.getItemStack());
                } else if (tag instanceof LocationTag locTag) {
                    event.setVelocity(locTag.getLocation().toVector());
                }
            }
        }
    }

    @Override
    public void reset() {
        isRegistered = false;
        scripts.clear();
    }
}