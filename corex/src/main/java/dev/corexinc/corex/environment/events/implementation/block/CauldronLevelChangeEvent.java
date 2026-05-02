package dev.corexinc.corex.environment.events.implementation.block;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.events.EventReturn;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Levelled;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name CauldronLevelChange
 *
 * @Events
 * cauldron level changes|raises|lowers
 *
 * @Switches
 * cause:<cause> - Matches the reason the cauldron level changed. See {@link javadoc https://jd.papermc.io/paper/org/bukkit/event/block/CauldronLevelChangeEvent.ChangeReason.html}.
 *
 * @Cancellable
 *
 * @Description
 * Fires when a cauldron's level changes.
 * The <state> argument can be 'changes', 'raises', or 'lowers'.
 *
 * @Context
 * <context.location> - returns the LocationTag of the cauldron that changed.
 * <context.entity> - returns the EntityTag of the entity that caused the level change (if any).
 * <context.cause> - returns an ElementTag of the reason that the cauldron level changed.
 * <context.oldLevel> - returns an ElementTag(Number) of the previous cauldron level.
 * <context.newLevel> - returns an ElementTag(Number) of the new cauldron level.
 *
 * @Returns
 * level:<ElementTag> - Sets the new level of the cauldron (0-3).
 *
 * @Usage
 * // Prevents players from taking water out of the cauldron using glass bottles.
 * on water_cauldron level lowers cause:BOTTLE_FILL:
 * - return cancelled
 *
 * @Usage
 * // Fills the cauldron to the max level immediately when any water is added.
 * on water_cauldron level raises:
 * - return level:3
 */
public class CauldronLevelChangeEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockCauldronLevelChange";
    }

    @Override
    public @NotNull String getSyntax() {
        return "cauldron level changes|raises|lowers";
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
    public void onCauldronLevelChange(org.bukkit.event.block.CauldronLevelChangeEvent event) {
        String blockMaterial = event.getBlock().getType().name().toLowerCase();

        int oldLevel = event.getBlock().getBlockData() instanceof Levelled levelledOld ? levelledOld.getLevel() : 0;
        int newLevel = event.getNewState().getBlockData() instanceof Levelled levelledNew ? levelledNew.getLevel() : 0;

        String cause = event.getReason().name().toUpperCase();

        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("block", 0, blockMaterial)) {
                continue;
            }

            String stateArg = data.getArgument("state", 0);
            String raw = data.rawLine;
            if (stateArg != null) {
                if (raw.contains("raises") && newLevel <= oldLevel) continue;
                if (raw.contains("lowers") && newLevel >= oldLevel) continue;
                if (raw.contains("changes")) continue;
            }

            String causeSwitch = data.getSwitch("cause");
            if (causeSwitch != null) {
                if (!causeSwitch.equalsIgnoreCase(cause) && !causeSwitch.equals("*")) {
                    continue;
                }
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("cause", new ElementTag(cause));
                context.put("oldLevel", new ElementTag(oldLevel));
                context.put("newLevel", new ElementTag(newLevel));

                if (event.getEntity() != null) {
                    context.put("entity", new EntityTag(event.getEntity()));
                }
            }

            ScriptQueue queue = EventRegistry.fire(data, null, context);
            if (queue.isCancelled()) event.setCancelled(true);

            String levelStr = EventReturn.getPrefixed(queue.getReturns(), "level");
            if (levelStr != null) {
                ElementTag el = new ElementTag(levelStr);
                if (el.isInt()) {
                    int requestedLevel = el.asInt();
                    BlockState cauldronState = event.getNewState();

                    if (requestedLevel <= 0) {
                        cauldronState.setType(Material.CAULDRON);
                    } else if (requestedLevel <= 3) {
                        if (cauldronState.getType() != Material.WATER_CAULDRON && cauldronState.getType() != Material.LAVA_CAULDRON && cauldronState.getType() != Material.POWDER_SNOW_CAULDRON) {
                            cauldronState.setType(event.getBlock().getType() == Material.CAULDRON ? Material.WATER_CAULDRON : event.getBlock().getType());
                        }
                        if (cauldronState.getBlockData() instanceof Levelled levelled) {
                            levelled.setLevel(requestedLevel);
                            cauldronState.setBlockData(levelled);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void reset() {
        isRegistered = false;
        scripts.clear();
    }
}