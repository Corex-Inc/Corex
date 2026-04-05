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
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class PlayerJoinEvent implements AbstractEvent {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "PlayerJoin";
    }

    @Override
    public @NotNull String getSyntax() {
        return "player join";
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

    @Override
    public void reset() {
        scripts.clear();
        isRegistered = false;
    }

    @EventHandler
    public void onJoin(org.bukkit.event.player.PlayerJoinEvent event) {

        PlayerTag player = new PlayerTag(event.getPlayer());

        ContextTag context = new ContextTag();
        context.put("message", new ElementTag(event.joinMessage()));

        for (EventData data : scripts) {
            ScriptQueue queue = EventRegistry.fire(data, player, context);

            for (AbstractTag result : queue.getReturns()) {
                String value = result.identify();
                if (value.equalsIgnoreCase("none")) {
                    event.joinMessage(null);
                } else {
                    event.joinMessage(MINI_MESSAGE.deserialize(value));
                }
            }
        }
    }
}