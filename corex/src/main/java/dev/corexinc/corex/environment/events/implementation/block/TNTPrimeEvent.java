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
 * @Name BlockTNTPrime
 *
 * @Events
 * tnt primes
 *
 * @Switches
 * cause:<cause> - Matches the reason the TNT was primed. Check {@link javadoc https://jd.papermc.io/paper/org/bukkit/event/block/TNTPrimeEvent.PrimeCause.html}
 *
 * @Cancellable
 *
 * @Description
 * Fires when a TNT block is activated and begins its countdown to explode.
 *
 * @Context
 * <context.location> - returns the LocationTag of the TNT block being primed.
 * <context.cause> - returns an ElementTag of the PrimeCause.
 * <context.entity> - returns the EntityTag of the entity that caused the TNT to prime, if applicable.
 * <context.sourceBlock> - returns the LocationTag of the block that caused the prime (like a fire block or redstone block), if applicable.
 *
 * @Usage
 * // Prevents players from manually igniting TNT with flint and steel.
 * on tnt primes cause:PLAYER:
 * - narrate "You cannot ignite TNT manually!" targets:<context.entity>
 * - return cancelled
 *
 * @Usage
 * // Broadcasts when TNT is activated by an explosion.
 * on tnt primes cause:EXPLOSION:
 * - announce "Chain reaction! TNT ignited at <context.location>!"
 */
public class TNTPrimeEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockTNTPrime";
    }

    @Override
    public @NotNull String getSyntax() {
        return "tnt primes";
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
    public void onTntPrime(org.bukkit.event.block.TNTPrimeEvent event) {
        String cause = event.getCause().name();
        ContextTag context = null;

        for (EventData data : scripts) {
            String causeSwitch = data.getSwitch("cause");
            if (causeSwitch != null && !causeSwitch.equalsIgnoreCase(cause) && !causeSwitch.equals("*")) {
                continue;
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("cause", new ElementTag(cause));

                if (event.getPrimingEntity() != null) {
                    context.put("entity", new EntityTag(event.getPrimingEntity()));
                }

                if (event.getPrimingBlock() != null) {
                    context.put("sourceBlock", new LocationTag(event.getPrimingBlock().getLocation()));
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