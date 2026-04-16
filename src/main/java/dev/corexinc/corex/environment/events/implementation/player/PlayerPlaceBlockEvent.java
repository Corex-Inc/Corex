package dev.corexinc.corex.environment.events.implementation.player;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.ItemTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.tags.world.MaterialTag;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name PlayerPlaceBlock
 *
 * @Events
 * player places <block>
 *
 * @Switches
 * using:<hand> (Matches the hand used to place the block. Can be HAND or OFFHAND)
 *
 * @Cancellable
 *
 * @Description
 * Fires when a player places a block in the world.
 *
 * @Context
 * <context.location> - returns a LocationTag of the block that was placed.
 * <context.material> - returns a MaterialTag of the block that was placed.
 * <context.item> - returns an ItemTag of the item the player used to place the block.
 * <context.hand> - returns an ElementTag of the hand used (HAND or OFFHAND).
 * <context.canBuild> - returns an ElementTag(Boolean) indicating if the player is allowed to build here.
 * <context.against> - returns a LocationTag of the block this block was placed against.
 *
 * @Returns
 * ElementTag(Boolean) - Sets whether the player is allowed to build the block (overrides default behavior).
 *
 * @Usage
 * // Greets the player when they place a dirt block.
 * on player places dirt:
 * - narrate "You placed dirt!"
 *
 * @Usage
 * // Stops players from placing any block using their offhand.
 * on player places block using:OFFHAND:
 * - return cancelled
 */
public class PlayerPlaceBlockEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "PlayerPlaceBlock";
    }

    @Override
    public @NotNull String getSyntax() {
        return "player places <block>";
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
    public void onPlace(BlockPlaceEvent event) {
        String blockMaterial = event.getBlockPlaced().getType().name().toLowerCase();
        String handUsed = event.getHand().name().replace("_", "").toUpperCase();

        PlayerTag player = new PlayerTag(event.getPlayer());
        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("block", 0, blockMaterial)) {
                continue;
            }

            String usingSwitch = data.getSwitch("using");
            if (usingSwitch != null) {
                if (!usingSwitch.equalsIgnoreCase(handUsed) && !usingSwitch.equals("*") && !usingSwitch.equalsIgnoreCase("any")) {
                    continue;
                }
            }

            if (context == null) {
                context = new ContextTag();
                context.put("material", new MaterialTag(event.getBlockPlaced()));
                context.put("location", new LocationTag(event.getBlockPlaced().getLocation()));
                context.put("item", new ItemTag(event.getItemInHand()));
                context.put("against", new LocationTag(event.getBlockAgainst().getLocation()));
                context.put("hand", new ElementTag(handUsed));
                context.put("canBuild", new ElementTag(event.canBuild()));
            }

            ScriptQueue queue = EventRegistry.fire(data, player, context);
            if (queue.isCancelled()) event.setCancelled(true);

            for (AbstractTag tag : queue.getReturns()) {
                if (tag instanceof ElementTag elementTag && elementTag.isBoolean()) {
                    event.setBuild(elementTag.asBoolean());
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