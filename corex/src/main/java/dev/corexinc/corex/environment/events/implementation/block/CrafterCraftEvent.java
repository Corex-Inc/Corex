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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name CrafterCraft
 *
 * @Events
 * crafter crafts <item>
 *
 * @Cancellable
 *
 * @Description
 * Fires when a crafter block crafts an item.
 *
 * @Context
 * <context.location> - returns the LocationTag of the crafter block.
 * <context.item> - returns the ItemTag being crafted.
 * <context.recipeId> - returns an ElementTag of the ID of the recipe formed.
 *
 * @Returns
 * item:<ItemTag> - Sets the item being crafted (this still consumes the original ingredients).
 *
 * @Usage
 * // Prevents crafters from making TNT.
 * on crafter crafts tnt:
 * - return cancelled
 *
 * @Usage
 * // Changes the result of a crafted diamond sword to a wooden sword.
 * on crafter crafts diamond_sword:
 * - return item:i@wooden_sword
 */
public class CrafterCraftEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "CrafterCraft";
    }

    @Override
    public @NotNull String getSyntax() {
        return "crafter crafts <item>";
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
    public void onCrafterCraft(org.bukkit.event.block.CrafterCraftEvent event) {
        String itemMaterial = event.getResult().getType().name().toLowerCase();
        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("item", 0, itemMaterial)) {
                continue;
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("item", new ItemTag(event.getResult()));
                context.put("recipeId", new ElementTag(event.getRecipe().getKey().toString()));
            }

            ScriptQueue queue = EventRegistry.fire(data, null, context);
            if (queue.isCancelled()) event.setCancelled(true);

            String returnedItem = EventReturn.getPrefixed(queue.getReturns(), "item");
            if (returnedItem != null) {
                ItemTag itemTag = new ItemTag(returnedItem);
                if (itemTag.getItemStack() != null) {
                    event.setResult(itemTag.getItemStack());
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