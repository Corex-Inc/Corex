package dev.corexinc.corex.velocity.environment.network;

import dev.corexinc.corex.engine.network.CorexPacket;
import dev.corexinc.corex.engine.network.NetworkManager;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.engine.network.packets.HelloPacket;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Accepts sockets from the backends and hands what arrives on them to the relay.
 *
 * <h2>A connection is anonymous until it proves otherwise</h2>
 * <p>The proxy learns which backend it is talking to from a signed {@link HelloPacket}, and nothing
 * else on the connection is acted on before one arrives. A socket that sends anything else first,
 * or a frame that fails its signature check, is closed rather than ignored: unlike a plugin
 * message, this port is open to whoever can reach it, so a bad frame is a reason to stop talking
 * rather than to keep listening.</p>
 *
 * <p>That is also why the transport refuses to start without a shared secret. Without one
 * {@link NetworkManager#verifyAndDecode} would accept an unsigned message from anybody who found
 * the port.</p>
 *
 * @since 1.0.0
 */
public final class WebSocketRelayServer extends WebSocketServer {

    private final ProxyRelay relay;
    private final Map<String, WebSocket> byServer = new ConcurrentHashMap<>();
    private final Map<WebSocket, String> byConnection = new ConcurrentHashMap<>();

    public WebSocketRelayServer(@NotNull InetSocketAddress address, @NotNull ProxyRelay relay) {
        super(address);
        this.relay = relay;
        setReuseAddr(true);
    }

    public @NotNull Set<String> connectedServers() {
        return byServer.keySet();
    }

    public boolean sendTo(@NotNull String server, byte @NotNull [] frame) {
        WebSocket connection = byServer.get(server);
        if (connection == null || !connection.isOpen()) {
            return false;
        }
        try {
            connection.send(frame);
            return true;
        }
        catch (RuntimeException e) {
            CorexLogger.warn("Corex websocket to \"" + server + "\" refused a frame: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void onOpen(WebSocket connection, ClientHandshake handshake) {
        CorexLogger.info("Corex websocket opened from " + connection.getRemoteSocketAddress()
                + ", waiting for it to identify itself.");
    }

    @Override
    public void onClose(WebSocket connection, int code, String reason, boolean remote) {
        String server = byConnection.remove(connection);
        if (server != null) {
            byServer.remove(server, connection);
            CorexLogger.warn("Corex websocket for \"" + server + "\" closed (" + code + " " + reason + ").");
        }
    }

    @Override
    public void onMessage(WebSocket connection, String message) {
        connection.close(1003, "Corex speaks binary frames only");
    }

    @Override
    public void onMessage(WebSocket connection, ByteBuffer message) {
        byte[] frame = new byte[message.remaining()];
        message.get(frame);

        CorexPacket packet = NetworkManager.verifyAndDecode(frame);
        if (packet == null) {
            connection.close(1008, "Corex could not verify that frame");
            return;
        }

        if (packet instanceof HelloPacket hello) {
            bind(connection, hello.getServerName());
            return;
        }

        String server = byConnection.get(connection);
        if (server == null) {
            connection.close(1008, "Corex expects a hello first");
            return;
        }

        relay.routeFrom(packet, server);
    }

    @Override
    public void onError(@Nullable WebSocket connection, Exception e) {
        CorexLogger.warn("Corex websocket server error: " + e.getMessage());
    }

    @Override
    public void onStart() {
        CorexLogger.success("Corex websocket relay listening on " + getAddress() + ".");
    }

    private void bind(WebSocket connection, String server) {
        if (server.isBlank()) {
            connection.close(1008, "Corex needs a server name in the hello");
            return;
        }

        WebSocket previous = byServer.put(server, connection);
        if (previous != null && previous != connection) {
            byConnection.remove(previous);
            previous.close(1008, "another connection claimed this server name");
        }

        byConnection.put(connection, server);
        CorexLogger.success("Corex websocket identified itself as \"" + server + "\".");
    }
}
