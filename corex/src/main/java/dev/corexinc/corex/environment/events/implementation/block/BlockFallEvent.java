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
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name BlockFall
 *
 * @Events
 * <block> falls
 *
 * @Switches
 * landing:<boolean> - Matches if the block is landing (true) or just starting to fall (false).
 *
 * @Cancellable
 *
 * @Description
 * Fires when a block (like sand or gravel) begins to fall, or when a falling block lands.
 *
 * @Context
 * <context.location> - returns the LocationTag of the block space.
 * <context.entity> - returns the EntityTag of the falling block entity.
 * <context.material> - returns the MaterialTag that will be at the location (e.g., 'air' when falling, or 'sand' when landing).
 * <context.isLanding> - returns an ElementTag(Boolean) indicating if the block is landing.
 *
 * @Usage
 * // Prevents sand from falling in the first place.
 * on sand falls landing:false:
 * - return cancelled
 *
 * @Usage
 * // Does something when gravel finally hits the ground.
 * on gravel falls landing:true:
 * - narrate "Gravel has landed at <context.location>!"
 */
public class BlockFallEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockFall";
    }

    @Override
    public @NotNull String getSyntax() {
        return "<block> falls";
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
    public void onBlockFall(EntityChangeBlockEvent event) {
        if (event.getEntityType() != EntityType.FALLING_BLOCK) {
            return;
        }

        boolean isLanding = event.getBlockData().getMaterial() != Material.AIR;
        String actualMaterial = isLanding ? event.getBlockData().getMaterial().name().toLowerCase() : event.getBlock().getType().name().toLowerCase();

        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("block", 0, actualMaterial)) {
                continue;
            }

            String landingSwitch = data.getSwitch("landing");
            if (landingSwitch != null) {
                boolean targetLanding = Boolean.parseBoolean(landingSwitch);
                if (targetLanding != isLanding) {
                    continue;
                }
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("entity", new EntityTag(event.getEntity()));
                context.put("material", new MaterialTag(event.getBlockData()));
                context.put("isLanding", new ElementTag(isLanding));
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