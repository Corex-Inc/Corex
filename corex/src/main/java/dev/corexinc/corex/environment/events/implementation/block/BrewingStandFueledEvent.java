package dev.corexinc.corex.environment.events.implementation.block;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.events.EventReturn;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.world.ItemTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.BrewingStandFuelEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name BrewingStandFueledEvent
 *
 * @Events
 * brewing stand fueled (with <item>)
 *
 * @Cancellable
 *
 * @Description
 * Fires when a brewing stand receives an item to use as fuel.
 *
 * @Context
 * <context.location> - returns the LocationTag of the brewing stand.
 * <context.item> - returns the ItemTag of the item being inserted as fuel.
 * <context.fuelPower> - returns an ElementTag(Number) of the fuel power level being added.
 * <context.isConsuming> - returns an ElementTag(Boolean) indicating whether the fuel item will be consumed.
 *
 * @Returns
 * fuelPower:<ElementTag> - Sets the fuel power level to be added.
 * isConsuming:<ElementTag> - Sets whether the fuel item should be consumed.
 *
 * @Usage
 * // Doubles the fuel power of blaze powder.
 * on brewing stand fueled with blaze_powder:
 * - return fuelPower:<context.fuelPower.mul[2]>
 *
 * @Usage
 * // Prevents the fuel from being consumed.
 * on brewing stand fueled:
 * - return isConsuming:false
 */
public class BrewingStandFueledEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockFueled";
    }

    @Override
    public @NotNull String getSyntax() {
        return "brewing stand fueled (with <item>)";
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
    public void onBlockFueled(BrewingStandFuelEvent event) {
        String fuelMaterial = event.getFuel().getType().name().toLowerCase();

        ContextTag context = null;

        for (EventData data : scripts) {
            if (data.getArgument("item", 0) != null) {
                if (!data.isGenericMatch("item", 0, fuelMaterial)) {
                    continue;
                }
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("item", new ItemTag(event.getFuel()));
                context.put("fuelPower", new ElementTag(event.getFuelPower()));
                context.put("isConsuming", new ElementTag(event.isConsuming()));
            }

            ScriptQueue queue = EventRegistry.fire(data, null, context);
            if (queue.isCancelled()) event.setCancelled(true);

            String fuelPowerStr = EventReturn.getPrefixed(queue.getReturns(), "fuelPower");
            if (fuelPowerStr != null) {
                event.setFuelPower(new ElementTag(fuelPowerStr).asInt());
            }

            String consumingStr = EventReturn.getPrefixed(queue.getReturns(), "isConsuming");
            if (consumingStr != null) {
                event.setConsuming(new ElementTag(consumingStr).asBoolean());
            }
        }
    }

    @Override
    public void reset() {
        isRegistered = false;
        scripts.clear();
    }
}