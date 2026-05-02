package dev.corexinc.corex.environment.events.implementation.block;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.environment.events.EventReturn;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.DurationTag;
import dev.corexinc.corex.environment.tags.world.ItemTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.tags.world.MaterialTag;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name BlockBurnsItem
 *
 * @Events
 * <block> burns <item>
 *
 * @Cancellable
 *
 * @Description
 * Fires when a block (such as a furnace, smoker, or blast furnace) burns an item used as fuel.
 *
 * @Context
 * <context.location> - returns the LocationTag of the furnace block.
 * <context.material> - returns the MaterialTag of the furnace block.
 * <context.item> - returns the ItemTag of the fuel item being burned.
 *
 * @Returns
 * duration:<DurationTag> - Sets how long this fuel item will burn for.
 *
 * @Usage
 * // Prevents blast furnaces from burning coal.
 * on blast_furnace burns coal:
 * - return cancelled
 *
 * @Usage
 * // Makes wooden sticks burn for 10 seconds in any furnace-like block.
 * on any burns stick:
 * - return duration:10s
 */
public class BlockBurnsItemEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockBurnsItem";
    }

    @Override
    public @NotNull String getSyntax() {
        return "<block> burns <item>";
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
    public void onFurnaceBurns(FurnaceBurnEvent event) {
        String blockMaterial = event.getBlock().getType().name().toLowerCase();
        String itemMaterial = event.getFuel().getType().name().toLowerCase();

        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("block", 0, blockMaterial)) {
                continue;
            }

            if (!data.isGenericMatch("item", 2, itemMaterial)) {
                continue;
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("material", new MaterialTag(event.getBlock()));
                context.put("item", new ItemTag(event.getFuel()));
            }

            ScriptQueue queue = EventRegistry.fire(data, null, context);
            if (queue.isCancelled()) event.setCancelled(true);

            String durationRaw = EventReturn.getPrefixed(queue.getReturns(), "duration");
            if (durationRaw != null) {
                DurationTag durationTag = new DurationTag(durationRaw);
                event.setBurnTime((int) durationTag.getTicks());
            }
        }
    }

    @Override
    public void reset() {
        isRegistered = false;
        scripts.clear();
    }
}