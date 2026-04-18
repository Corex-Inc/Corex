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
import org.bukkit.event.block.BlockFadeEvent;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name BlockDisappear
 *
 * @Events
 * <block> disappears
 *
 * @Switches
 * into:<material> - Matches the material the block is turning into (e.g., water when ice melts).
 *
 * @Cancellable
 *
 * @Description
 * Fires when a block fades, melts, or disappears based on world conditions.
 * Examples include ice or snow melting, fire burning out, or coral dying.
 *
 * @Context
 * <context.location> - returns the LocationTag of the block that is disappearing.
 * <context.oldMaterial> - returns the MaterialTag of the block before it disappears.
 * <context.material> - returns the MaterialTag of the block it will become (often air or water).
 *
 * @Usage
 * // Prevents ice from melting into water.
 * on ice disappears into:water:
 * - return cancelled
 *
 * @Usage
 * // Alerts when a fire burns out.
 * on fire disappears:
 * - narrate "The fire went out!"
 */
public class BlockDisappearEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockDisappear";
    }

    @Override
    public @NotNull String getSyntax() {
        return "<block> disappears";
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
    public void onBlockDisappear(BlockFadeEvent event) {
        String blockMaterial = event.getBlock().getType().name().toLowerCase();
        String newMaterial = event.getNewState().getType().name().toLowerCase();

        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("block", 0, blockMaterial)) {
                continue;
            }

            String intoSwitch = data.getSwitch("into");
            if (intoSwitch != null) {
                boolean match = intoSwitch.equals("*") || intoSwitch.equalsIgnoreCase("any") ||
                        intoSwitch.equalsIgnoreCase(newMaterial) ||
                        intoSwitch.equalsIgnoreCase("minecraft:" + newMaterial) ||
                        newMaterial.equalsIgnoreCase("minecraft:" + intoSwitch);

                if (!match) continue;
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("oldMaterial", new MaterialTag(event.getBlock()));
                context.put("material", new MaterialTag(event.getNewState().getBlockData().getMaterial()));
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