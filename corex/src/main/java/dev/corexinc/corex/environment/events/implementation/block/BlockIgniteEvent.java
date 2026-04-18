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
import dev.corexinc.corex.environment.tags.world.MaterialTag;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name BlockIgnite
 *
 * @Events
 * <block> ignites
 *
 * @Switches
 * cause:<cause> - Matches the cause of the ignition (e.g., LAVA, FLINT_AND_STEEL, LIGHTNING).
 *
 * @Cancellable
 *
 * @Description
 * Fires when a block is set on fire.
 *
 * @Context
 * <context.location> - returns the LocationTag of the block that was set on fire.
 * <context.material> - returns the MaterialTag of the block that was set on fire.
 * <context.entity> - returns the EntityTag of the entity that ignited the block (if any).
 * <context.originLocation> - returns the LocationTag of the fire block that ignited this block (if any).
 * <context.cause> - returns an ElementTag of the cause of the ignition.
 *
 * @Usage
 * // Prevents lava from starting fires.
 * on * ignites cause:LAVA:
 * - return cancelled
 *
 * @Usage
 * // Narrates when a player ignites TNT.
 * on tnt ignites cause:FLINT_AND_STEEL:
 * - narrate "TNT has been ignited by <context.entity.name>!"
 */
public class BlockIgniteEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockIgnite";
    }

    @Override
    public @NotNull String getSyntax() {
        return "<block> ignites";
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
    public void onBlockIgnite(org.bukkit.event.block.BlockIgniteEvent event) {
        String blockMaterial = event.getBlock().getType().name().toLowerCase();
        String cause = event.getCause().name().toUpperCase();

        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("block", 0, blockMaterial)) {
                continue;
            }

            String causeSwitch = data.getSwitch("cause");
            if (causeSwitch != null) {
                if (!causeSwitch.equalsIgnoreCase(cause) && !causeSwitch.equals("*") && !causeSwitch.equalsIgnoreCase("any")) {
                    continue;
                }
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("material", new MaterialTag(event.getBlock()));
                context.put("cause", new ElementTag(cause));

                if (event.getIgnitingEntity() != null) {
                    context.put("entity", new EntityTag(event.getIgnitingEntity()));
                }

                if (event.getIgnitingBlock() != null) {
                    context.put("originLocation", new LocationTag(event.getIgnitingBlock().getLocation()));
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