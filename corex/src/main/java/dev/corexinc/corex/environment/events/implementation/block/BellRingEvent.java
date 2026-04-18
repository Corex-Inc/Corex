package dev.corexinc.corex.environment.events.implementation.block;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name BellRing
 *
 * @Events
 * bell rings
 *
 * @Switches
 * direction:<direction> - Matches the BlockFace direction the bell was rung from, e.g., NORTH, SOUTH
 *
 * @Cancellable
 *
 * @Description
 * Fires when a bell block is rung.
 * This can be triggered by a player interacting with it or a projectile hitting it.
 *
 * @Context
 * <context.location> - returns the LocationTag of the bell being rung.
 * <context.sourceEntity> - returns the EntityTag of the entity that rung the bell, if any.
 * <context.direction> - returns an ElementTag of the direction the bell was rung in.
 *
 * @Usage
 * // Stops players from ringing bells from the north side
 * on block bell rings direction:NORTH:
 * - return cancelled
 *
 * @Usage
 * // Narrates when a bell is rung
 * on block bell rings:
 * - narrate "A bell was rung at <context.location>!"
 */
public class BellRingEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BellRing";
    }

    @Override
    public @NotNull String getSyntax() {
        return "bell rings";
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
    public void onBellRing(org.bukkit.event.block.BellRingEvent event) {
        String direction = event.getDirection().name().toUpperCase();

        PlayerTag player = null;
        if (event.getEntity() instanceof Player p) {
            player = new PlayerTag(p);
        }

        ContextTag context = null;

        for (EventData data : scripts) {
            String dirSwitch = data.getSwitch("direction");
            if (dirSwitch != null) {
                if (!dirSwitch.equalsIgnoreCase(direction) && !dirSwitch.equals("*") && !dirSwitch.equalsIgnoreCase("any")) {
                    continue;
                }
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("direction", new ElementTag(direction));

                if (event.getEntity() != null) {
                    context.put("sourceEntity", new EntityTag(event.getEntity()));
                }
            }

            ScriptQueue queue = EventRegistry.fire(data, player, context);
            if (queue.isCancelled()) event.setCancelled(true);
        }
    }

    @Override
    public void reset() {
        isRegistered = false;
        scripts.clear();
    }
}