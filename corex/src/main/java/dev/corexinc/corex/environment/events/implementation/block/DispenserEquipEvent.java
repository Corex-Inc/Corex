package dev.corexinc.corex.environment.events.implementation.block;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.events.EventReturn;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.world.ItemTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockDispenseArmorEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name BlockDispenseArmor
 *
 * @Events
 * dispenser equips <item>
 *
 * @Switches
 * target:<entity> - Only process the event if the entity receiving the armor matches the specified entity type.
 *
 * @Cancellable
 *
 * @Description
 * Fires when a dispenser equips armor onto an entity (player, armor stand, mob, etc.).
 *
 * @Context
 * <context.location> - returns the LocationTag of the dispenser.
 * <context.item> - returns the ItemTag of the armor being equipped.
 * <context.entity> - returns the EntityTag of the entity receiving the armor.
 *
 * @Returns
 * item:<ItemTag> - Sets a new item to be dispensed and equipped.
 *
 * @Usage
 * // Prevents dispensers from equipping zombies with diamond armor.
 * on dispenser equips diamond_*:
 * - if <context.entity.entity_type> == ZOMBIE:
 *     - return cancelled
 *
 * @Usage
 * // Replaces dispensed iron helmets with gold helmets.
 * on dispenser equips iron_helmet:
 * - return item:golden_helmet
 */
public class DispenserEquipEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockDispenseArmor";
    }

    @Override
    public @NotNull String getSyntax() {
        return "dispenser equips <item>";
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
    public void onDispenseArmor(BlockDispenseArmorEvent event) {
        String itemMaterial = event.getItem().getType().name().toLowerCase();
        String targetType = event.getTargetEntity().getType().name().toLowerCase();

        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("item", 0, itemMaterial)) {
                continue;
            }

            String targetSwitch = data.getSwitch("target");
            if (targetSwitch != null && !targetSwitch.equals("*")) {
                String cleanTarget = targetSwitch.replace("minecraft:", "").toLowerCase();
                if (!cleanTarget.equals(targetType)) {
                    continue;
                }
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("item", new ItemTag(event.getItem()));
                context.put("entity", new EntityTag(event.getTargetEntity()));
            }

            ScriptQueue queue = EventRegistry.fire(data, null, context);
            if (queue.isCancelled()) event.setCancelled(true);

            String returnedItem = EventReturn.getPrefixed(queue.getReturns(), "item");
            if (returnedItem != null) {
                ItemTag itemTag = new ItemTag(returnedItem);
                if (itemTag.getItemStack() != null) {
                    event.setItem(itemTag.getItemStack());
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