package dev.corexinc.corex.environment.utils.scripts;

import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.environment.events.implementation.core.WebsocketScriptEvent;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebSocketManager {

    private static final Map<String, WebSocketClient> clients = new ConcurrentHashMap<>();

    public static void connect(String id, String uriStr) {
        try {
            if (clients.containsKey(id)) {
                clients.get(id).close();
            }

            WebSocketClient client = new WebSocketClient(new URI(uriStr)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    SchedulerAdapter.runLater(() -> WebsocketScriptEvent.fireOpen(id), 1L);
                }

                @Override
                public void onMessage(String message) {
                    SchedulerAdapter.runLater(() -> WebsocketScriptEvent.fireMessage(id, message), 1L);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    SchedulerAdapter.runLater(() -> WebsocketScriptEvent.fireClose(id, code, reason), 1L);
                    clients.remove(id);
                }

                @Override
                public void onError(Exception ex) {
                    CorexLogger.error("WebSocket Error[" + id + "]: " + ex.getMessage());
                }
            };

            clients.put(id, client);
            client.connect();

        } catch (Exception e) {
            CorexLogger.error("Failed to connect WebSocket[" + id + "]: " + e.getMessage());
        }
    }

    public static void send(String id, String message) {
        WebSocketClient client = clients.get(id);
        if (client != null && client.isOpen()) {
            client.send(message);
        } else {
            CorexLogger.warn("Cannot send message. WebSocket [" + id + "] is not connected.");
        }
    }

    public static void disconnect(String id) {
        WebSocketClient client = clients.get(id);
        if (client != null) {
            client.close();
        }
    }

    public static void disconnectAll() {
        for (WebSocketClient client : clients.values()) {
            client.close();
        }
        clients.clear();
    }
}