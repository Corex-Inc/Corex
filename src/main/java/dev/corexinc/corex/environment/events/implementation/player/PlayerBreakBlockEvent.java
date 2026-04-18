package dev.corexinc.corex.environment.events.implementation.player;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.ItemTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.tags.world.MaterialTag;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name PlayerBreakBlock
 *
 * @Events
 * player breaks <block>
 *
 * @Switches
 * with:<item> - Matches the item the player is holding in their main hand
 *
 * @Cancellable
 *
 * @Description
 * Fires when a player breaks a block in the world.
 *
 * @Context
 * <context.location> - returns a LocationTag of the block that was broken.
 * <context.material> - returns a MaterialTag of the block that was broken.
 * <context.item> - returns an ItemTag of the item the player used to break the block.
 * <context.xp> - returns an ElementTag(Number) of the amount of experience the block will drop.
 * <context.shouldDropItems> - returns an ElementTag(Boolean) indicating if the block will drop items.
 *
 * @Returns
 * ListTag - Returns any list to prevent the block from dropping items.
 * ElementTag(Number) - Sets the amount of experience points the block will drop.
 *
 * @Usage
 * // Narrates a message when the player breaks stone with a diamond pickaxe.
 * on player breaks stone with:diamond_pickaxe:
 * - narrate "You broke a stone block!"
 *
 * @Usage
 * // Prevents the player from breaking any block with their bare hands.
 * on player breaks block with:air:
 * - return cancelled
 */
public class PlayerBreakBlockEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "PlayerBreakBlock";
    }

    @Override
    public @NotNull String getSyntax() {
        return "player breaks <block>";
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
    public void onBreak(BlockBreakEvent event) {
        String blockMaterial = event.getBlock().getType().name().toLowerCase();
        ItemStack heldItem = event.getPlayer().getInventory().getItemInMainHand();
        String itemMaterial = heldItem.getType().name().toLowerCase();

        PlayerTag player = new PlayerTag(event.getPlayer());
        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("block", 0, blockMaterial)) {
                continue;
            }

            String withSwitch = data.getSwitch("with");
            if (withSwitch != null) {
                boolean match = withSwitch.equals("*") || withSwitch.equalsIgnoreCase("any") ||
                        withSwitch.equalsIgnoreCase(itemMaterial) ||
                        withSwitch.equalsIgnoreCase("minecraft:" + itemMaterial) ||
                        itemMaterial.equalsIgnoreCase("minecraft:" + withSwitch);

                if (!match) continue;
            }

            if (context == null) {
                context = new ContextTag();
                context.put("shouldDropItems", new ElementTag(event.isDropItems()));
                context.put("xp", new ElementTag(event.getExpToDrop()));
                context.put("material", new MaterialTag(event.getBlock()));
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("item", new ItemTag(heldItem));
            }

            ScriptQueue queue = EventRegistry.fire(data, player, context);
            if (queue.isCancelled()) event.setCancelled(true);

            for (AbstractTag tag : queue.getReturns()) {
                if (tag instanceof ListTag) {
                    event.setDropItems(false);
                } else if (tag instanceof ElementTag elementTag && elementTag.isInt()) {
                    event.setExpToDrop(elementTag.asInt());
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