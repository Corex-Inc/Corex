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
 * @Name BlockForm
 *
 * @Events
 * <block> forms
 *
 * @Cancellable
 *
 * @Description
 * Fires when a block is formed based on world conditions.
 * Examples include snow forming in a snowstorm, ice forming in a cold biome, or cobblestone forming from lava and water.
 *
 * @Context
 * <context.location> - returns the LocationTag of the block that is forming.
 * <context.material> - returns the MaterialTag of the block that is forming.
 *
 * @Usage
 * // Prevents ice from forming naturally.
 * on ice forms:
 * - return cancelled
 *
 * @Usage
 * // Narrates when snow forms somewhere.
 * on snow forms:
 * - narrate "Snow has formed at <context.location>!"
 */
public class BlockFormEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockForm";
    }

    @Override
    public @NotNull String getSyntax() {
        return "<block> forms";
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
    public void onBlockForm(org.bukkit.event.block.BlockFormEvent event) {
        String newMaterial = event.getNewState().getType().name().toLowerCase();

        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("block", 0, newMaterial)) {
                continue;
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
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