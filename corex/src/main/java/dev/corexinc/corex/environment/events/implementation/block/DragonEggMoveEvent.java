package dev.corexinc.corex.environment.events.implementation.block;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockFromToEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name DragonEggMove
 *
 * @Events
 * dragon egg moves
 *
 * @Cancellable
 *
 * @Description
 * Fires when a dragon egg teleports.
 *
 * @Context
 * <context.location> - returns the LocationTag the egg started at.
 * <context.destination> - returns the LocationTag the egg teleported to.
 *
 * @Usage
 * // Prevents the dragon egg from teleporting when clicked.
 * on dragon egg moves:
 * - return cancelled
 *
 * @Usage
 * // Narrates the egg's new location.
 * on dragon egg moves:
 * - narrate "The egg warped to <context.destination>!"
 */
public class DragonEggMoveEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "DragonEggMove";
    }

    @Override
    public @NotNull String getSyntax() {
        return "dragon egg moves";
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
    public void onDragonEggMove(BlockFromToEvent event) {
        if (event.getBlock().getType() != Material.DRAGON_EGG) {
            return;
        }

        ContextTag context = null;

        for (EventData data : scripts) {
            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("destination", new LocationTag(event.getToBlock().getLocation()));
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