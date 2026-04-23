package dev.corexinc.corex.environment.events.implementation.core;

import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;
import java.util.LinkedHashSet;
import java.util.Set;


/* @doc event
 *
 * @Name Websocket
 *
 * @Events
 * websocket opens
 * websocket message
 * websocket closes
 *
 * @Switches
 * id:<name> - Matches only events from the websocket connection with the given ID.
 *
 * @Description
 * Fires when a managed websocket connection changes state or receives a message.
 *
 * Use "websocket opens" to react when a connection is first established.
 * Use "websocket message" to process incoming data from the remote server.
 * Use "websocket closes" to clean up state or reconnect after the connection drops.
 *
 * Each script line must contain exactly one of the three keywords.
 * The optional "id:" switch filters events to a single named connection.
 * Without it, the script fires for all connections.
 *
 * @Context
 * <context.id>      - returns an ElementTag of the connection ID. Available in all three variants.
 * <context.message> - returns an ElementTag of the received message. Available in "message" only.
 * <context.code>    - returns an ElementTag(Number) of the close code. Available in "closes" only.
 * <context.reason>  - returns an ElementTag of the close reason string. Available in "closes" only.
 *
 * @Usage
 * // Log when any websocket opens.
 * on websocket opens:
 * - narrate "Connection <context.id> opened."
 *
 * @Usage
 * // Handle incoming messages from a specific connection.
 * on websocket message id:mySocket:
 * - narrate "Received: <context.message>"
 *
 * @Usage
 * // Reconnect automatically when the connection closes unexpectedly.
 * on websocket closes id:mySocket:
 * - if <context.code> != 1000:
 *   - websocket connect mySocket wss://example.com/ws
 */
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
        return "websocket opens|message|closes";
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