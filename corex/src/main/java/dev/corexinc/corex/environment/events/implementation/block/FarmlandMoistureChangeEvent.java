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
import org.bukkit.block.data.type.Farmland;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.MoistureChangeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name FarmlandMoistureChange
 *
 * @Events
 * farmland moisture level changes
 *
 * @Switches
 * from:<level> - Only process the event when the previous moisture level matches the input.
 * to:<level> - Only process the event when the new moisture level matches the input.
 *
 * @Cancellable
 *
 * @Description
 * Fires when a farmland block's moisture level changes.
 *
 * @Context
 * <context.location> - returns the LocationTag of the farmland block.
 * <context.material> - returns the MaterialTag of the farmland block.
 * <context.oldLevel> - returns an ElementTag(Number) of the previous moisture level.
 * <context.newLevel> - returns an ElementTag(Number) of the new moisture level.
 *
 * @Usage
 * // Announce when farmland begins to dry out.
 * on farmland moisture level changes from:7 to:6:
 * - narrate "Farmland at <context.location> lost its water source and began to dry!"
 */
public class FarmlandMoistureChangeEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockFarmlandMoistureChange";
    }

    @Override
    public @NotNull String getSyntax() {
        return "farmland moisture level changes";
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
    public void onMoistureChange(MoistureChangeEvent event) {
        if (!(event.getBlock().getBlockData() instanceof Farmland oldFarmland)) return;
        if (!(event.getNewState().getBlockData() instanceof Farmland newFarmland)) return;

        int oldMoisture = oldFarmland.getMoisture();
        int newMoisture = newFarmland.getMoisture();

        ContextTag context = null;

        for (EventData data : scripts) {
            String fromSwitch = data.getSwitch("from");
            if (fromSwitch != null && !fromSwitch.equals(String.valueOf(oldMoisture))) {
                continue;
            }

            String toSwitch = data.getSwitch("to");
            if (toSwitch != null && !toSwitch.equals(String.valueOf(newMoisture))) {
                continue;
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("material", new MaterialTag(event.getBlock()));
                context.put("oldLevel", new ElementTag(oldMoisture));
                context.put("newLevel", new ElementTag(newMoisture));
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