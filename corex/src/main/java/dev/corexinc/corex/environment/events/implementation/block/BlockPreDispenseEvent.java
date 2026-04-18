package dev.corexinc.corex.environment.events.implementation.block;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.world.ItemTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name BlockPreDispense
 *
 * @Events
 * <block> tries to dispense <item>
 *
 * @Cancellable
 *
 * @Description
 * Fires before a block dispenses an item.
 * This event fires before the dispenser fully processes a drop, allowing cancellation of sound effects
 * and access to the exact slot being dispensed from.
 * For the standard dispense event, see {@link event BlockDispense}.
 *
 * @Context
 * <context.location> - returns the LocationTag of the dispenser.
 * <context.item> - returns the ItemTag of the item about to be dispensed.
 * <context.slot> - returns an ElementTag(Number) of the 1-based inventory slot that will be dispensed from.
 *
 * @Usage
 * // Prevents a dispenser from shooting arrows and avoids playing the dispense sound.
 * on dispenser tries to dispense arrow:
 * - return cancelled
 */
public class BlockPreDispenseEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockPreDispense";
    }

    @Override
    public @NotNull String getSyntax() {
        return "<block> tries to dispense <item>";
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
    public void onBlockPreDispense(io.papermc.paper.event.block.BlockPreDispenseEvent event) {
        String blockMaterial = event.getBlock().getType().name().toLowerCase();
        String itemMaterial = event.getItemStack().getType().name().toLowerCase();

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
                context.put("item", new ItemTag(event.getItemStack()));
                context.put("slot", new ElementTag(event.getSlot() + 1));
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