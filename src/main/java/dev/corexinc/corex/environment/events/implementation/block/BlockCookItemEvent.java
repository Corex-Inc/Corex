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
import dev.corexinc.corex.environment.tags.world.MaterialTag;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name BlockCookItem
 *
 * @Events
 * <block> cooks <item> (into <result>)
 *
 * @Cancellable
 *
 * @Description
 * Fires when an item is smelted or cooked by a block.
 *
 * @Context
 * <context.location> - returns the LocationTag of the block cooking the item.
 * <context.source> - returns the ItemTag that is being cooked.
 * <context.result> - returns the ItemTag that is the result of the cooking.
 *
 * @Returns
 * ItemTag - Sets the item that is the result of the cooking.
 *
 * @Usage
 * // Prevents beef from being cooked specifically in a campfire.
 * on block cooks beef block:campfire:
 * - return cancelled
 *
 * @Usage
 * // Changes the output of iron ore to give a diamond instead when smelted into an iron ingot.
 * on block cooks iron_ore into iron_ingot:
 * - return <item[diamond]>
 */
public class BlockCookItemEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockCookItem";
    }

    @Override
    public @NotNull String getSyntax() {
        return "<block> cooks <item> (into <result>)";
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
    public void onBlockCook(org.bukkit.event.block.BlockCookEvent event) {
        String sourceItem = event.getSource().getType().name().toLowerCase();
        String resultItem = event.getResult().getType().name().toLowerCase();
        String blockMaterial = event.getBlock().getType().name().toLowerCase();

        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("item", 0, sourceItem)) {
                continue;
            }

            if (data.getArgument("result", 0) != null) {
                if (!data.isGenericMatch("result", 0, resultItem)) {
                    continue;
                }
            }

            if (!data.isGenericMatch("block", 0, blockMaterial)) {
                continue;
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("source", new ItemTag(event.getSource()));
                context.put("result", new ItemTag(event.getResult()));
            }

            ScriptQueue queue = EventRegistry.fire(data, null, context);
            if (queue.isCancelled()) event.setCancelled(true);

            for (AbstractTag tag : queue.getReturns()) {
                if (tag instanceof ItemTag itemTag) {
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