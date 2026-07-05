package dev.corexinc.corex.environment.events;

import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.engine.utils.Position;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.utils.BukkitSchedulerAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EventRegistry {

    private static final List<AbstractEvent> registeredEvents = new ArrayList<>();
    private static final Map<AbstractEvent, EventPattern> patternCache = new HashMap<>();

    public static void register(Class<?>... classes) {
        for (Class<?> clazz : classes) {
            try {
                if (AbstractEvent.class.isAssignableFrom(clazz)) {
                    AbstractEvent event = (AbstractEvent) clazz.getDeclaredConstructor().newInstance();
                    registeredEvents.add(event);
                    patternCache.put(event, new EventPattern(event.getSyntax()));
                } else {
                    CorexLogger.warn("Class " + clazz.getSimpleName() + " is not AbstractEvent!");
                }
            } catch (Exception e) {
                Debugger.error("Error while registering event! " + clazz.getSimpleName(), e);
            }
        }
    }

    public static void resetAll() {
        for (AbstractEvent event : registeredEvents) {
            event.unregister();
            event.reset();
        }
    }

    public static void mapScript(String rawLine, Instruction[] bytecode) {
        boolean isAfter = rawLine.startsWith("after ");
        String cleanLine = rawLine.replaceFirst("^(on |after )", "").trim();

        Map<String, String> switches = new HashMap<>();
        StringBuilder lineWithoutSwitches = new StringBuilder();

        for (String word : cleanLine.split("\\s+")) {
            if (word.contains(":") && !word.contains("<") && !word.startsWith("minecraft:")) {
                String[] parts = word.split(":", 2);
                switches.put(parts[0].toLowerCase(), parts[1]);
            } else {
                lineWithoutSwitches.append(word).append(" ");
            }
        }

        String finalLine = lineWithoutSwitches.toString().trim();

        for (AbstractEvent event : registeredEvents) {
            EventPattern pattern = patternCache.get(event);
            if (pattern == null) continue;

            Map<String, List<String>> arguments = pattern.match(finalLine);

            if (arguments != null) {
                EventData data = new EventData(finalLine, isAfter, bytecode, switches, arguments);
                event.addScript(data);
                event.initListener();
                return;
            }
        }

        Debugger.error("No handler found for event syntax: " + rawLine);
    }

    public static ScriptQueue fire(EventData data, PlayerTag player, ContextTag context) {
        ScriptQueue queue = new ScriptQueue(
                ScriptQueue.uniqueId("Event"),
                data.bytecode,
                false,
                player
        );

        if (context != null) {
            queue.setContext(context);
        }

        Position anchor = resolveAnchor(player, context);
        if (anchor != null) {
            queue.setAnchorPosition(anchor);
        }

        if (data.isAfter) {
            SchedulerAdapter.get().runLater(queue::start, 1L);
        } else {
            queue.start();
        }

        return queue;
    }

    private static Position resolveAnchor(PlayerTag player, ContextTag context) {
        if (player != null && player.getPlayer() != null) {
            return BukkitSchedulerAdapter.toPosition(player.getPlayer().getLocation());
        }
        if (context != null && context.get("location") instanceof LocationTag locationTag && locationTag.getLocation() != null) {
            return BukkitSchedulerAdapter.toPosition(locationTag.getLocation());
        }
        return null;
    }
}