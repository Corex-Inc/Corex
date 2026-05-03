package dev.corexinc.corex.environment.events.implementation.block;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.tags.world.MaterialTag;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockFromToEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name BlockLiquidSpread
 *
 * @Events
 * <liquid> spreads
 *
 * @Cancellable
 *
 * @Description
 * Fires when a liquid block (water or lava) spreads to an adjacent block.
 *
 * @Context
 * <context.location> - returns the LocationTag of the source liquid block.
 * <context.destination> - returns the LocationTag the liquid is spreading to.
 * <context.material> - returns the MaterialTag of the spreading liquid.
 *
 * @Usage
 * // Prevents lava from spreading anywhere.
 * on lava spreads:
 * - return cancelled
 *
 * @Usage
 * // Prevents any liquid from spreading to a specific height.
 * on liquid spreads:
 * - if <context.destination.y> < 50:
 *     - return cancelled
 */
public class LiquidSpreadEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockLiquidSpread";
    }

    @Override
    public @NotNull String getSyntax() {
        return "<liquid> spreads";
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
    public void onLiquidSpreads(BlockFromToEvent event) {
        if (event.getBlock().getType() == Material.DRAGON_EGG) {
            return;
        }

        String liquidMaterial = event.getBlock().getType().name().toLowerCase();
        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("liquid", 0, liquidMaterial)) {
                continue;
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("destination", new LocationTag(event.getToBlock().getLocation()));
                context.put("material", new MaterialTag(event.getBlock()));
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