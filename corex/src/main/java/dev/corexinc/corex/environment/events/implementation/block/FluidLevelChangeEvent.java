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
 * @Name BlockFluidLevelChange
 *
 * @Events
 * <fluid> level changes
 *
 * @Cancellable
 *
 * @Description
 * Fires when a fluid block's level changes (usually when it goes down/evaporates).
 * Note that '<liquid> spreads' is fired when a liquid first spreads out.
 *
 * @Context
 * <context.location> - returns the LocationTag of the fluid block changing level.
 * <context.oldMaterial> - returns the MaterialTag of the fluid before the change.
 * <context.newMaterial> - returns the MaterialTag of the block after the change (sometimes air).
 *
 * @Usage
 * // Prevent water from drying up.
 * on water level changes:
 * - return cancelled
 */
public class FluidLevelChangeEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockFluidLevelChange";
    }

    @Override
    public @NotNull String getSyntax() {
        return "<fluid> level changes";
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
    public void onFluidLevelChange(org.bukkit.event.block.FluidLevelChangeEvent event) {
        String fluidMaterial = event.getBlock().getType().name().toLowerCase();
        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("fluid", 0, fluidMaterial)) {
                continue;
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("oldMaterial", new MaterialTag(event.getBlock()));
                context.put("newMaterial", new MaterialTag(event.getNewData()));
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