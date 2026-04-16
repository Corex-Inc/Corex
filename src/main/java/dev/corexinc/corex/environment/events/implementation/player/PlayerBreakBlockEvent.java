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
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.tags.world.MaterialTag;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

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
        PlayerTag player = new PlayerTag(event.getPlayer());

        ContextTag context = new ContextTag();
        context.put("shouldDropItems", new ElementTag(event.isDropItems()));
        context.put("xp", new ElementTag(event.getExpToDrop()));
        context.put("material", new MaterialTag(event.getBlock()));
        context.put("location", new LocationTag(event.getBlock().getLocation()));


        for (EventData data : scripts) {
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
