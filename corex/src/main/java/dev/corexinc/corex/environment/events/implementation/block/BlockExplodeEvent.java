package dev.corexinc.corex.environment.events.implementation.block;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name BlockExplode
 *
 * @Events
 * <block> explodes
 *
 * @Cancellable
 *
 * @Description
 * Fires when a block explodes (like a bed in the nether or a respawn anchor).
 * For TNT, refer to the {@link event EntityExplode} event instead.
 *
 * @Context
 * <context.location> - returns the LocationTag of the exploding block.
 * <context.blocks> - returns a ListTag of LocationTags of the blocks that blew up.
 * <context.strength> - returns an ElementTag(Decimal) of the strength of the explosion.
 *
 * @Returns
 * ListTag - Sets a new list of LocationTags that are to be affected by the explosion.
 * ElementTag(Decimal) - Changes the strength (yield) of the explosion.
 *
 * @Usage
 * // Prevents beds from exploding in the nether.
 * on red_bed explodes:
 * - return cancelled
 *
 * @Usage
 * // Makes respawn anchor explosions twice as strong.
 * on respawn_anchor explodes:
 * - return <context.strength.mul[2]>
 */
public class BlockExplodeEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "BlockExplode";
    }

    @Override
    public @NotNull String getSyntax() {
        return "<block> explodes";
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
    public void onBlockExplode(org.bukkit.event.block.BlockExplodeEvent event) {
        String blockMaterial = event.getBlock().getType().name().toLowerCase();
        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("block", 0, blockMaterial)) {
                continue;
            }

            if (context == null) {
                context = new ContextTag();
                context.put("location", new LocationTag(event.getBlock().getLocation()));
                context.put("strength", new ElementTag(event.getYield()));

                ListTag blocksList = new ListTag();
                for (Block b : event.blockList()) {
                    blocksList.addObject(new LocationTag(b.getLocation()));
                }
                context.put("blocks", blocksList);
            }

            ScriptQueue queue = EventRegistry.fire(data, null, context);
            if (queue.isCancelled()) event.setCancelled(true);

            for (AbstractTag tag : queue.getReturns()) {
                String raw = tag.identify();

                ElementTag el = tag instanceof ElementTag ? (ElementTag) tag : new ElementTag(raw);
                if (el.isDouble()) {
                    event.setYield((float) el.asDouble());
                    continue;
                }

                ListTag listTag = tag instanceof ListTag ? (ListTag) tag : new ListTag(raw);

                if (!listTag.getList().isEmpty()) {
                    boolean isBlockList = false;
                    List<Block> tempBlocks = new ArrayList<>();

                    for (AbstractTag listElement : listTag.getList()) {
                        String elRaw = listElement.identify();

                        if (elRaw.startsWith("l@") || elRaw.contains(",")) {
                            isBlockList = true;
                            LocationTag locTag = listElement instanceof LocationTag ? (LocationTag) listElement : new LocationTag(elRaw);

                            if (locTag.getLocation().getWorld() != null) {
                                tempBlocks.add(locTag.getLocation().getBlock());
                            }
                        }
                    }

                    if (isBlockList) {
                        event.blockList().clear();
                        event.blockList().addAll(tempBlocks);
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