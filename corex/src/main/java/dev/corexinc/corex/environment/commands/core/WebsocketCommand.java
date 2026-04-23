package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.environment.utils.scripts.WebSocketManager;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import org.jspecify.annotations.NonNull;

/* @doc command
 *
 * @Name Websocket
 * @Syntax websocket [connect/send/disconnect] [<id>] (<url>/<message>)
 * @RequiredArgs 2
 * @MaxArgs 3
 * @ShortDescription Manages a persistent websocket connection.
 *
 * @Description
 * Opens, communicates with, or closes a websocket connection identified by a custom ID.
 *
 * Use "connect" with a URL to open a new connection.
 * Use "send" with a message string to transmit data over an open connection.
 * Use "disconnect" or "close" to terminate the connection.
 *
 * The ID you provide is reused across all three actions and is also passed to
 * the WebsocketScriptEvent so you can route events per connection.
 *
 * @Usage
 * // Open a connection to a remote server.
 * - websocket connect mySocket wss://example.com/ws
 *
 * @Usage
 * // Send a message over an open connection.
 * - websocket send mySocket Hello World
 *
 * @Usage
 * // Close the connection.
 * - websocket disconnect mySocket
 */
public class WebsocketCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "websocket";
    }

    @Override
    public @NonNull String getSyntax() {
        return "[connect/send/disconnect] [<id>] (<value>)";
    }

    @Override
    public int getMinArgs() { return 2; }

    @Override
    public int getMaxArgs() { return 3; }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        String action = instruction.getLinear(0, queue);
        String id = instruction.getLinear(1, queue);
        String value = instruction.getLinear(2, queue);

        if (action == null || id == null) return;

        Debugger.report(queue, instruction,
                "Action", action,
                "ID", id,
                "Value", value
        );

        switch (action.toLowerCase()) {
            case "connect":
                if (value == null) {
                    Debugger.echoError(queue, "You must specify a URL to connect to.");
                    return;
                }
                WebSocketManager.connect(id, value);
                break;
            case "send":
                if (value == null) {
                    Debugger.echoError(queue, "You must specify a message to send.");
                    return;
                }
                WebSocketManager.send(id, value);
                break;
            case "close":
            case "disconnect":
                WebSocketManager.disconnect(id);
                break;
            default:
                Debugger.echoError(queue, "Unknown websocket action: '<red>" + action + "</red>'.");
                Debugger.echoError(queue, "Use connect, send, or disconnect.");
        }
    }
}