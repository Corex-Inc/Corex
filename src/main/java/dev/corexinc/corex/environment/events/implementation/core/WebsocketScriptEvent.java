package dev.corexinc.corex.environment.events.implementation.core;

import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;

import java.util.LinkedHashSet;
import java.util.Set;

public class WebsocketScriptEvent implements AbstractEvent {

    private static final Set<EventData> openScripts = new LinkedHashSet<>();
    private static final Set<EventData> messageScripts = new LinkedHashSet<>();
    private static final Set<EventData> closeScripts = new LinkedHashSet<>();

    @Override
    public @NonNull String getName() {
        return "Websocket";
    }

    @Override
    public @NonNull String getSyntax() {
        return "websocket";
    }

    @Override
    public void addScript(@NonNull EventData data) {
        String raw = data.rawLine;
        if (raw.contains("opens")) openScripts.add(data);
        else if (raw.contains("message")) messageScripts.add(data);
        else if (raw.contains("closes")) closeScripts.add(data);
    }

    @Override
    public void initListener() {}

    @Override
    public void reset() {
        openScripts.clear();
        messageScripts.clear();
        closeScripts.clear();
    }

    public static void fireOpen(String id) {
        ContextTag context = new ContextTag();
        context.put("id", new ElementTag(id));
        fireGroup(openScripts, id, context);
    }

    public static void fireMessage(String id, String message) {
        ContextTag context = new ContextTag();
        context.put("id", new ElementTag(id));
        context.put("message", new ElementTag(message));
        fireGroup(messageScripts, id, context);
    }

    public static void fireClose(String id, int code, String reason) {
        ContextTag context = new ContextTag();
        context.put("id", new ElementTag(id));
        context.put("code", new ElementTag(code));
        context.put("reason", new ElementTag(reason));
        fireGroup(closeScripts, id, context);
    }

    private static void fireGroup(Set<EventData> scripts, String id, ContextTag context) {
        for (EventData data : scripts) {
            String requiredId = data.getSwitch("id");
            if (requiredId == null || requiredId.equalsIgnoreCase(id)) {
                EventRegistry.fire(data, null, context);
            }
        }
    }
}