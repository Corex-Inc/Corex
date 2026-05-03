package dev.corexinc.corex.environment.events.implementation.block;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.events.EventReturn;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.world.ItemTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name BrewingStandBrewEvent
 *
 * @Events
 * brewing stand brews
 *
 * @Cancellable
 *
 * @Description
 * Fires when a brewing stand brews a potion.
 *
 * @Context
 * <context.location> - returns the LocationTag of the brewing stand.
 * <context.fuelLevel> - returns an ElementTag(Number) of the brewing stand's fuel level.
 * <context.result> - returns a ListTag(ItemTag) of the items that will be brewed.
 *
 * @Returns
 * result:<ListTag> - Sets the items that are brewed.
 *
 * @Usage
 * // Replaces all results with water bottles.
 * on brewing stand brews:
 * - return result:<list[potion|potion|potion]>
 */
public class BrewingStandBrewEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockBrew";
    }

    @Override
    public @NotNull String getSyntax() {
        return "brewing stand brews";
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
    public void onBlockBrew(BrewEvent event) {
        String blockMaterial = event.getBlock().getType().name().toLowerCase();
        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("block", 0, blockMaterial)) {
                continue;
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("fuelLevel", new ElementTag(event.getFuelLevel()));

                ListTag resultsList = new ListTag();
                for (ItemStack item : event.getResults()) {
                    resultsList.addObject(new ItemTag(item));
                }
                context.put("result", resultsList);
            }

            ScriptQueue queue = EventRegistry.fire(data, null, context);
            if (queue.isCancelled()) event.setCancelled(true);

            String resultStr = EventReturn.getPrefixed(queue.getReturns(), "result");
            if (resultStr != null) {
                ListTag list = new ListTag(resultStr);
                event.getResults().clear();
                for (AbstractTag tag : list.getList()) {
                    if (tag instanceof ItemTag itemTag) {
                        event.getResults().add(itemTag.getItemStack());
                    }
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