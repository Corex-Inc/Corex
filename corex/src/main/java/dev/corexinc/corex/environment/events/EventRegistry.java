package dev.corexinc.corex.environment.events;

import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
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
                CorexLogger.error("Error while registering event! " + clazz.getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
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

            var arguments = pattern.match(finalLine);

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
                "Event_" + System.nanoTime(),
                data.bytecode,
                false,
                player
        );

        if (context != null) {
            queue.setContext(context);
        }

        if (data.isAfter) {
            SchedulerAdapter.get().runLater(queue::start, 1L);
        } else {
            queue.start();
        }

        return queue;
    }
}