package dev.corexinc.corex.environment.events.implementation.block;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.events.EventReturn;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.DurationTag;
import dev.corexinc.corex.environment.tags.world.ItemTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BrewingStartEvent;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name BlockBrewingStart
 *
 * @Events
 * <block> brewing starts
 *
 * @Description
 * Fires when a brewing stand starts brewing a potion.
 *
 * @Context
 * <context.location> - returns the LocationTag of the brewing stand.
 * <context.item> - returns the ItemTag of the ingredient used to brew the potions.
 * <context.brewTime> - returns a DurationTag of the total time it will take to brew the potion.
 *
 * @Returns
 * brewTime:<DurationTag> - Sets the total time for the potion being brewed.
 *
 * @Usage
 * // Makes all potions brew instantly.
 * on brewing_stand brewing starts:
 * - return brewTime:0t
 */
public class BlockBrewingStartEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockBrewingStart";
    }

    @Override
    public @NotNull String getSyntax() {
        return "<block> brewing starts";
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
    @SuppressWarnings("UnstableApiUsage")
    public void onBrewingStart(BrewingStartEvent event) {
        String blockMaterial = event.getBlock().getType().name().toLowerCase();
        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("block", 0, blockMaterial)) {
                continue;
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("item", new ItemTag(event.getSource()));
                context.put("brewTime", new DurationTag(event.getBrewingTime()));
            }

            ScriptQueue queue = EventRegistry.fire(data, null, context);

            String brewTimeStr = EventReturn.getPrefixed(queue.getReturns(), "brewTime");
            if (brewTimeStr != null) {
                DurationTag dur = new DurationTag(brewTimeStr);
                event.setBrewingTime((int) dur.getTicksLong());
            }
        }
    }

    @Override
    public void reset() {
        isRegistered = false;
        scripts.clear();
    }
}