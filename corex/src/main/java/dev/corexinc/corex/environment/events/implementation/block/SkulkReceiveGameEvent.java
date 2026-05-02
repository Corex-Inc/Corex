package dev.corexinc.corex.environment.events.implementation.block;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name SkulkReceiveGame
 *
 * @Events
 * sculk sensor receives event
 *
 * @Cancellable
 *
 * @Description
 * Fires when a block (typically a Sculk Sensor) receives a GameEvent vibration.
 * Only process if the GameEvent matches a specific name. See {@link javadoc https://jd.papermc.io/paper/org/bukkit/GameEvent.html}
 *
 * @Context
 * <context.location> - returns the LocationTag of the block receiving the event.
 * <context.entity> - returns the EntityTag of the entity that caused the vibration (may be null).
 * <context.eventName> - returns an ElementTag of the GameEvent name (e.g. 'step', 'block_destroy').
 *
 * @Usage
 * // Prevent sculk sensors from hearing players eating.
 * on sculk sensor receives event event:eat:
 * - return cancelled
 */
public class SkulkReceiveGameEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "SkulkReceiveGame";
    }

    @Override
    public @NotNull String getSyntax() {
        return "sculk sensor triggers (by <event>)";
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
    public void onReceiveGameEvent(org.bukkit.event.block.BlockReceiveGameEvent event) {
        String eventName = event.getEvent().getKey().getKey().toLowerCase();
        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("event", 0, eventName)) {
                continue;
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("eventName", new ElementTag(eventName));

                if (event.getEntity() != null) {
                    context.put("entity", new EntityTag(event.getEntity()));
                }
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