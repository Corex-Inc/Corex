package dev.corexinc.corex.environment.events.implementation.block;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.tags.world.MaterialTag;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name BlockAttemptBuild
 *
 * @Events
 * block attempts to build <block>
 *
 * @Switches
 * on:<block> - Matches the material of the block that is being replaced or built on
 *
 * @Description
 * Fires when an attempt is made to build a block on another block.
 * This is a physical check and not necessarily caused by a player placing a block.
 * Prefer the {@link event PlayerPlaceBlock} event for listening to player actions.
 *
 * @Context
 * <context.location> - returns the LocationTag of the block space being built in.
 * <context.oldMaterial> - returns the MaterialTag of the block currently at the location.
 * <context.newMaterial> - returns the MaterialTag of the block attempting to be built.
 * <context.buildable> - returns an ElementTag(Boolean) indicating whether the block can be built.
 *
 * @Returns
 * ElementTag(Boolean) - Sets whether the block is allowed to be built or not.
 *
 * @Usage
 * // Prevents dirt from being placed on top of stone
 * on block attempts to build dirt on:stone:
 * - return false
 *
 * @Usage
 * // Allows a block to be built even if it physically shouldn't be able to
 * on block attempts to build anvil:
 * - return true
 */
public class BlockAttemptBuildEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockAttemptBuild";
    }

    @Override
    public @NotNull String getSyntax() {
        return "<block> attempts to build <block>";
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
    public void onBlockCanBuild(BlockCanBuildEvent event) {
        String newMaterial = event.getBlockData().getMaterial().name().toLowerCase();
        String oldMaterial = event.getBlock().getType().name().toLowerCase();

        PlayerTag player = event.getPlayer() != null ? new PlayerTag(event.getPlayer()) : null;
        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("block", 0, oldMaterial)) {
                continue;
            }

            if (!data.isGenericMatch("block", 1, newMaterial)) {
                continue;
            }

            String onSwitch = data.getSwitch("on");
            if (onSwitch != null) {
                boolean match = onSwitch.equals("*") || onSwitch.equalsIgnoreCase("any") ||
                        onSwitch.equalsIgnoreCase(oldMaterial) ||
                        onSwitch.equalsIgnoreCase("minecraft:" + oldMaterial) ||
                        oldMaterial.equalsIgnoreCase("minecraft:" + onSwitch);

                if (!match) continue;
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("oldMaterial", new MaterialTag(event.getBlock()));
                context.put("newMaterial", new MaterialTag(event.getBlockData().getMaterial()));
                context.put("buildable", new ElementTag(event.isBuildable()));
            }

            ScriptQueue queue = EventRegistry.fire(data, player, context);

            for (AbstractTag tag : queue.getReturns()) {
                if (tag instanceof ElementTag elementTag && elementTag.isBoolean()) {
                    event.setBuildable(elementTag.asBoolean());
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