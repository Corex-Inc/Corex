package dev.corexinc.corex.environment.events.implementation.block;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.world.ItemTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.tags.world.MaterialTag;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name BlockShearEntity
 *
 * @Events
 * <block> shears <entity>
 *
 * @Cancellable
 *
 * @Description
 * Fires when a block (like a dispenser) shears a nearby entity (like a sheep).
 *
 * @Context
 * <context.location> - returns the LocationTag of the block doing the shearing.
 * <context.material> - returns the MaterialTag of the block doing the shearing.
 * <context.tool> - returns the ItemTag of the item used to shear the entity.
 * <context.entity> - returns the EntityTag of the sheared entity.
 *
 * @Usage
 * // Prevents dispensers from shearing sheep.
 * on dispenser shears sheep:
 * - return cancelled
 */
public class BlockShearEntityEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockShearEntity";
    }

    @Override
    public @NotNull String getSyntax() {
        return "<block> shears <entity>";
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
    public void onBlockShearEntity(org.bukkit.event.block.BlockShearEntityEvent event) {
        String blockMaterial = event.getBlock().getType().name().toLowerCase();
        String entityType = event.getEntity().getType().name().toLowerCase();

        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("block", 0, blockMaterial)) {
                continue;
            }

            if (!data.isGenericMatch("entity", 0, entityType)) {
                continue;
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("material", new MaterialTag(event.getBlock()));
                context.put("tool", new ItemTag(event.getTool()));
                context.put("entity", new EntityTag(event.getEntity()));
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