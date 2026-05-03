package dev.corexinc.corex.environment.events.implementation.block;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.environment.events.EventReturn;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.DurationTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.world.ItemTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.tags.world.MaterialTag;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.FurnaceStartSmeltEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name BlockStartsSmelting
 *
 * @Events
 * <block> starts smelting <item>
 *
 * @Description
 * Fires when a furnace, blast furnace, or smoker starts smelting an item.
 *
 * @Context
 * <context.location> - returns the LocationTag of the furnace block.
 * <context.material> - returns the MaterialTag of the furnace block.
 * <context.item> - returns the ItemTag of the item being smelted.
 * <context.recipeId> - returns an ElementTag of the recipe ID being used.
 * <context.totalCookTime> - returns a DurationTag of the total time it will take to smelt the item.
 *
 * @Returns
 * duration:<DurationTag> - Sets the total cook time for the item being smelted.
 *
 * @Usage
 * // Speeds up the smelting process for iron ore in blast furnaces.
 * on blast_furnace starts smelting iron_ore:
 * - return duration:2s
 */
public class BlockStartsSmeltingEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockStartsSmelting";
    }

    @Override
    public @NotNull String getSyntax() {
        return "<block> starts smelting <item>";
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
    public void onFurnaceStartsSmelting(FurnaceStartSmeltEvent event) {
        String blockMaterial = event.getBlock().getType().name().toLowerCase();
        String itemMaterial = event.getSource().getType().name().toLowerCase();

        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("block", 0, blockMaterial)) {
                continue;
            }

            if (!data.isGenericMatch("item", 3, itemMaterial)) {
                continue;
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("material", new MaterialTag(event.getBlock()));
                context.put("item", new ItemTag(event.getSource()));
                context.put("recipeId", new ElementTag(event.getRecipe().getKey().toString()));
                context.put("totalCookTime", new DurationTag(event.getTotalCookTime() + "t"));
            }

            ScriptQueue queue = EventRegistry.fire(data, null, context);

            String durationRaw = EventReturn.getPrefixed(queue.getReturns(), "duration");
            if (durationRaw != null) {
                DurationTag durationTag = new DurationTag(durationRaw);
                event.setTotalCookTime((int) durationTag.getTicks());
            }
        }
    }

    @Override
    public void reset() {
        isRegistered = false;
        scripts.clear();
    }
}