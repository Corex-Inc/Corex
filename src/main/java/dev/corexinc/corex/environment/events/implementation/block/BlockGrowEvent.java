package dev.corexinc.corex.environment.events.implementation.block;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.tags.world.MaterialTag;
import org.bukkit.Bukkit;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name BlockGrow
 *
 * @Events
 * <block> grows
 *
 * @Switches
 * from:<age> - Matches if the block started at a specific age before growing.
 * to:<age> - Matches if the block will end up at a specific age after growing.
 *
 * @Cancellable
 *
 * @Description
 * Fires when a block grows naturally in the world.
 * This triggers for crops like wheat, carrots, sugar cane, cacti, pumpkins, etc.
 *
 * @Context
 * <context.location> - returns the LocationTag of the block that grew.
 * <context.material> - returns the MaterialTag of the block's newly grown state.
 * <context.age> - returns an ElementTag(Number) of the crop's age after growing (if applicable).
 *
 * @Usage
 * // Prevents wheat from reaching its final growth stage.
 * on wheat grows to:7:
 * - return cancelled
 *
 * @Usage
 * // Does something when a cactus grows.
 * on cactus grows:
 * - narrate "A cactus grew at <context.location>!"
 */
public class BlockGrowEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockGrow";
    }

    @Override
    public @NotNull String getSyntax() {
        return "<block> grows";
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
    public void onBlockGrow(org.bukkit.event.block.BlockGrowEvent event) {
        String newMaterial = event.getNewState().getType().name().toLowerCase();

        int oldAge = -1;
        if (event.getBlock().getBlockData() instanceof Ageable oldAgeable) {
            oldAge = oldAgeable.getAge();
        }

        int newAge = -1;
        if (event.getNewState().getBlockData() instanceof Ageable newAgeable) {
            newAge = newAgeable.getAge();
        }

        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("block", 0, newMaterial)) {
                continue;
            }

            String fromSwitch = data.getSwitch("from");
            if (fromSwitch != null) {
                try {
                    if (oldAge == -1 || oldAge != Integer.parseInt(fromSwitch)) {
                        continue;
                    }
                } catch (NumberFormatException ignored) {}
            }

            String toSwitch = data.getSwitch("to");
            if (toSwitch != null) {
                try {
                    if (newAge == -1 || newAge != Integer.parseInt(toSwitch)) {
                        continue;
                    }
                } catch (NumberFormatException ignored) {}
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("material", new MaterialTag(event.getNewState().getBlockData().getMaterial()));

                if (newAge != -1) context.put("age", new ElementTag(newAge));
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