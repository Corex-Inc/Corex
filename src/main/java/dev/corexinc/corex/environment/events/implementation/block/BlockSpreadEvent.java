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
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name BlockSpread
 *
 * @Events
 * <block> spreads
 *
 * @Cancellable
 *
 * @Description
 * Fires when a block spreads based on world conditions.
 * Examples include fire spreading, mushrooms multiplying, or vines growing to adjacent blocks.
 *
 * @Context
 * <context.sourceLocation> - returns the LocationTag of the block that caused the spread.
 * <context.location> - returns the LocationTag of the newly formed block.
 * <context.material> - returns the MaterialTag of the block that spread.
 *
 * @Usage
 * // Prevents fire from spreading naturally.
 * on fire spreads:
 * - return cancelled
 *
 * @Usage
 * // Narrates when mushrooms spread to a new location.
 * on brown_mushroom spreads:
 * - narrate "A mushroom spread to <context.location>!"
 */
public class BlockSpreadEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockSpread";
    }

    @Override
    public @NotNull String getSyntax() {
        return "<block> spreads";
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
    public void onBlockSpread(org.bukkit.event.block.BlockSpreadEvent event) {
        String blockMaterial = event.getSource().getType().name().toLowerCase();

        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("block", 0, blockMaterial)) {
                continue;
            }

            if (context == null) {
                context = new ContextTag();
                context.put("sourceLocation", new LocationTag(event.getSource().getLocation()));
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("material", new MaterialTag(event.getSource()));
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