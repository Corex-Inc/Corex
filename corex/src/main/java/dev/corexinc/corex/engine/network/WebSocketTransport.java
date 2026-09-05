package dev.corexinc.corex.engine.network;

import dev.corexinc.corex.engine.network.packets.HelloPacket;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.enums.ReadyState;
import org.java_websocket.handshake.ServerHandshake;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.nio.ByteBuffer;

/**
 * Carries Corex packets over a socket the backend holds open to the proxy.
 *
 * <h2>What this buys over plugin messaging</h2>
 * <p>A plugin message rides a player connection, so an empty server can neither send nor receive
 * one. This connection belongs to the server itself, so an empty lobby is still reachable, which
 * is the whole point: the automation people actually want to run across servers tends to fire
 * exactly when nobody is there.</p>
 *
 * <h2>Threading</h2>
 * <p>Java-WebSocket delivers callbacks on its own reader thread. Nothing script facing may run
 * there, so an incoming frame is handed to the scheduler before it reaches
 * {@link NetworkManager#handleFrame}. Sending is safe from any thread and stays inline, which is
 * what lets the command report a failure to the script that caused it.</p>
 *
 * <h2>Reconnecting</h2>
 * <p>One repeating task owns reconnection. {@link WebSocketClient#onClose} deliberately does not
 * reconnect: the socket drops on both sides of a proxy restart, and two reconnect paths racing
 * produce a pile of half open clients. A handshake that never finishes leaves the state at
 * {@code NOT_YET_CONNECTED} rather than failing, so a client stuck there past the connect timeout
 * counts as dead as well, otherwise one unlucky attempt would park the transport forever.</p>
 *
 * @since 1.0.0
 */
public final class WebSocketTransport implements ProxyTransport {

    /**
     * Largest frame this transport will send. A socket has no 32 kB custom payload ceiling, so the
     * limit here is only about not letting a runaway script queue a huge message.
     */
    public static final int MAX_FRAME_BYTES = 1_000_000;

    private static final long CONNECT_TIMEOUT_MILLIS = 10_000L;

    private final URI uri;
    private final String serverName;
    private final long reconnectPeriodTicks;

    @Nullable
    private volatile WebSocketClient client;

    private volatile boolean running;

    private volatile long connectStartedNanos;

    public WebSocketTransport(@NotNull URI uri, @NotNull String serverName, long reconnectSeconds) {
        this.uri = uri;
        this.serverName = serverName;
        this.reconnectPeriodTicks = Math.max(1L, reconnectSeconds) * 20L;
    }

    public void init() {
        running = true;
        NetworkManager.setTransport(this);
        SchedulerAdapter.get().runAsyncRepeating(this::ensureConnected, 1L, reconnectPeriodTicks);
        CorexLogger.info("Corex network is using a websocket to " + uri + " as \"" + serverName + "\".");
    }

    public void shutdown() {
        running = false;
        WebSocketClient current = client;
        client = null;
        if (current != null) {
            current.close();
        }
    }

    @Override
    public @NotNull String name() {
        return "websocket";
    }

    @Override
    public boolean isConnected() {
        WebSocketClient current = client;
        return current != null && current.isOpen();
    }

    @Override
    public @NotNull SendResult send(@NotNull CorexPacket packet) {
        WebSocketClient current = client;
        if (current == null || !current.isOpen()) {
            return SendResult.failed("the websocket to " + uri + " is not connected");
        }

        byte[] frame;
        try {
            frame = NetworkManager.encode(packet);
        }
        catch (RuntimeException e) {
            return SendResult.failed("the packet could not be encoded: " + e.getMessage());
        }

        if (frame.length > MAX_FRAME_BYTES) {
            return SendResult.failed("the packet is " + frame.length + " bytes, over the "
                    + MAX_FRAME_BYTES + " byte limit for a websocket frame");
        }

        try {
            current.send(frame);
        }
        catch (RuntimeException e) {
            return SendResult.failed("the websocket refused the frame: " + e.getMessage());
        }
        return SendResult.success();
    }

    private void ensureConnected() {
        if (!running) return;

        WebSocketClient current = client;
        if (current != null && !isDead(current)) {
            return;
        }
        if (current != null) {
            current.close();
        }

        WebSocketClient fresh = buildClient();
        client = fresh;
        connectStartedNanos = System.nanoTime();
        try {
            fresh.connect();
        }
        catch (RuntimeException e) {
            CorexLogger.warn("Corex could not open a websocket to " + uri + ": " + e.getMessage());
        }
    }

    private boolean isDead(WebSocketClient current) {
        ReadyState state = current.getReadyState();
        if (state == ReadyState.OPEN) {
            return false;
        }
        if (state == ReadyState.NOT_YET_CONNECTED) {
            return System.nanoTime() - connectStartedNanos > CONNECT_TIMEOUT_MILLIS * 1_000_000L;
        }
        return true;
    }

    private WebSocketClient buildClient() {
        WebSocketClient fresh = new WebSocketClient(uri) {

            @Override
            public void onOpen(ServerHandshake handshake) {
                announce(this);
            }

            @Override
            public void onMessage(String message) {
                CorexLogger.warn("Corex ignored a text frame from the proxy websocket, "
                        + "the protocol is binary only.");
            }

            @Override
            public void onMessage(ByteBuffer message) {
                byte[] frame = new byte[message.remaining()];
                message.get(frame);
                SchedulerAdapter.get().run(() -> NetworkManager.handleFrame(frame, "proxy-websocket"));
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                if (running) {
                    CorexLogger.warn("Corex websocket to " + uri + " closed (" + code + " " + reason
                            + "), retrying.");
                }
            }

            @Override
            public void onError(Exception e) {
                CorexLogger.warn("Corex websocket error: " + e.getMessage());
            }
        };

        fresh.setConnectionLostTimeout((int) (CONNECT_TIMEOUT_MILLIS / 1000L));
        return fresh;
    }

    private void announce(WebSocketClient connection) {
        try {
            connection.send(NetworkManager.encode(new HelloPacket(serverName)));
            CorexLogger.success("Corex websocket connected to " + uri + " as \"" + serverName + "\".");
        }
        catch (RuntimeException e) {
            CorexLogger.warn("Corex could not announce itself over the websocket: " + e.getMessage());
        }
    }
}
