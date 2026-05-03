package dev.corexinc.corex.environment.events.implementation.block;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.events.EventReturn;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name BlockRedstoneRecalculate
 *
 * @Events
 * redstone recalculates
 *
 * @Warning
 * This event fires extremely rapidly! Use carefully to avoid causing server lag.
 *
 * @Description
 * Fires when a redstone block or wire is recalculated (changes its power level).
 * Note: This event is not strictly cancellable in Bukkit, but you can simulate cancellation by returning 'current:0' or keeping it at the old current.
 *
 * @Context
 * <context.location> - returns the LocationTag of the redstone block/wire.
 * <context.oldCurrent> - returns an ElementTag(Number) of the previous redstone power level.
 * <context.newCurrent> - returns an ElementTag(Number) of the new redstone power level.
 *
 * @Returns
 * current:<ElementTag(Number)> - Sets the new redstone power level (0-15).
 *
 * @Usage
 * // Limits all redstone power to a maximum of 5.
 * on redstone recalculates:
 * - if <context.newCurrent> > 5:
 *     - return current:5
 */
public class RedstoneRecalculateEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockRedstoneRecalculate";
    }

    @Override
    public @NotNull String getSyntax() {
        return "redstone recalculates";
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
    public void onRedstoneRecalculate(BlockRedstoneEvent event) {
        ContextTag context = null;

        for (EventData data : scripts) {
            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("oldCurrent", new ElementTag(event.getOldCurrent()));
                context.put("newCurrent", new ElementTag(event.getNewCurrent()));
            }

            ScriptQueue queue = EventRegistry.fire(data, null, context);

            String currentStr = EventReturn.getPrefixed(queue.getReturns(), "current");
            if (currentStr != null) {
                ElementTag el = new ElementTag(currentStr);
                if (el.isInt()) {
                    event.setNewCurrent(el.asInt());
                    context.put("newCurrent", new ElementTag(event.getNewCurrent()));
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