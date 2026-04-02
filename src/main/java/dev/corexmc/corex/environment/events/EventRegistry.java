package dev.corexmc.corex.environment.events;

import dev.corexmc.corex.engine.compiler.Instruction;
import dev.corexmc.corex.engine.queue.ScriptQueue;
import dev.corexmc.corex.engine.utils.CorexLogger;
import dev.corexmc.corex.engine.utils.SchedulerAdapter;
import dev.corexmc.corex.engine.utils.debugging.Debugger;
import dev.corexmc.corex.environment.tags.core.ContextTag;
import dev.corexmc.corex.environment.tags.player.PlayerTag;

import java.util.ArrayList;
import java.util.List;

public class EventRegistry {

    private static final List<AbstractEvent> registeredEvents = new ArrayList<>();

    public static void register(Class<?>... classes) {
        for (Class<?> clazz : classes) {
            try {
                if (AbstractEvent.class.isAssignableFrom(clazz)) {
                    AbstractEvent event = (AbstractEvent) clazz.getDeclaredConstructor().newInstance();
                    registeredEvents.add(event);
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
            event.reset();
        }
    }

    public static void mapScript(String rawLine, Instruction[] bytecode) {
        boolean isAfter = rawLine.startsWith("after ");
        String cleanLine = rawLine.replaceFirst("^(on |after )", "").trim();

        java.util.Map<String, String> switches = new java.util.HashMap<>();
        StringBuilder lineWithoutSwitches = new StringBuilder();

        for (String word : cleanLine.split(" ")) {
            int colon = word.indexOf(':');
            if (colon > 0 && !word.contains("<")) {
                switches.put(word.substring(0, colon).toLowerCase(), word.substring(colon + 1));
            } else {
                lineWithoutSwitches.append(word).append(" ");
            }
        }

        String finalLine = lineWithoutSwitches.toString().trim().toLowerCase();

        for (AbstractEvent event : registeredEvents) {
            String syntaxPrefix = event.getSyntax().split("<")[0].trim().toLowerCase();

            if (finalLine.startsWith(syntaxPrefix)) {
                event.addScript(new EventData(finalLine, isAfter, bytecode, switches));
                event.initListener();
                return;
            }
        }
        Debugger.echoError("No handler found for event: " + rawLine);
    }

    public static void fire(EventData data, PlayerTag player, ContextTag context) {
        Runnable execute = () -> {
            ScriptQueue queue = new ScriptQueue(
                    "Event_" + System.currentTimeMillis(),
                    data.bytecode,
                    false,
                    player
            );
            if (context != null) {
                queue.setContext(context);
            }
            queue.start();
        };

        if (data.isAfter) {
            SchedulerAdapter.runLater(execute, 1L);
        } else {
            execute.run();
        }
    }
}