package dev.corexinc.corex.environment.events.implementation.block;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name BlockSpongeAbsorb
 *
 * @Events
 * sponge absorbs water
 *
 * @Cancellable
 *
 * @Warning
 * In some Bukkit versions, this event may double-fire. You might need to use rate-limiting logic in your scripts if you are doing heavy processing.
 *
 * @Description
 * Fires when a sponge block absorbs water blocks around it.
 *
 * @Context
 * <context.location> - returns the LocationTag of the sponge block.
 * <context.blocks> - returns a ListTag of LocationTags representing the water blocks that are being absorbed and removed.
 *
 * @Usage
 * // Prevent sponges from absorbing water in a specific area.
 * on sponge absorbs water:
 * - if <context.location.y> > 100:
 *     - return cancelled
 *
 * @Usage
 * // Announce how much water a sponge soaked up.
 * on sponge absorbs water:
 * - narrate "A sponge just absorbed <context.blocks.size> blocks of water!"
 */
public class SpongeAbsorbEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockSpongeAbsorb";
    }

    @Override
    public @NotNull String getSyntax() {
        return "sponge absorbs water";
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
    public void onSpongeAbsorb(org.bukkit.event.block.SpongeAbsorbEvent event) {
        ContextTag context = null;

        for (EventData data : scripts) {
            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));

                ListTag blocksList = new ListTag();
                for (BlockState state : event.getBlocks()) {
                    blocksList.addObject(new LocationTag(state.getLocation()));
                }
                context.put("blocks", blocksList);
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