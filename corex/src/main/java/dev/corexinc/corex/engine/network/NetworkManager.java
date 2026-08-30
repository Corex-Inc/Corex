package dev.corexinc.corex.engine.network;

import dev.corexinc.corex.engine.network.packets.NetworkMessagePacket;
import dev.corexinc.corex.engine.network.packets.ScriptPacket;
import dev.corexinc.corex.engine.utils.CorexLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicLong;

/**
 * The one place that decides what an incoming Corex packet is allowed to do.
 *
 * <h2>Trust model</h2>
 * <p>A backend receives plugin messages through the same listener whether they came from the proxy
 * or from a player's own client, and nothing in the Bukkit API distinguishes the two. The proxy
 * side drops client sent frames before they are forwarded, which covers a correctly configured
 * network; the signature covers everything else, including a backend that is reachable directly.</p>
 *
 * <p>So the rules are:</p>
 * <ul>
 *   <li>A secret is configured here and the frame is unsigned or fails to verify: dropped.</li>
 *   <li>A privileged packet (anything that runs a command or a script) with no verified
 *       signature: dropped, with a line telling the owner to set the secret.</li>
 *   <li>A privileged packet while remote execution is switched off in config: dropped.</li>
 *   <li>Everything else: handed to the registered handler.</li>
 * </ul>
 *
 * <p>A plain message stays usable with no secret at all, since it only reaches script logic the
 * server owner wrote themselves. Nothing that grants a capability does.</p>
 *
 * @since 1.0.0
 */
public final class NetworkManager {

    private static final long DROP_WARN_INTERVAL_NANOS = 5_000_000_000L;

    private static final AtomicLong lastDropWarnNanos = new AtomicLong(Long.MIN_VALUE);
    private static final AtomicLong suppressedDrops = new AtomicLong();

    @Nullable
    private static ProxyTransport transport;

    private static byte @Nullable [] secretKey;

    private static boolean remoteExecutionAllowed;

    @Nullable
    private static NetworkMessageHandler messageHandler;

    @Nullable
    private static NetworkExecutionHandler executionHandler;

    private NetworkManager() {}

    public static void configure(@Nullable String secret, boolean remoteExecutionAllowed) {
        secretKey = PacketCodec.toKey(secret);
        NetworkManager.remoteExecutionAllowed = remoteExecutionAllowed;
    }

    public static void setTransport(@Nullable ProxyTransport transport) {
        NetworkManager.transport = transport;
    }

    public static @Nullable ProxyTransport getTransport() {
        return transport;
    }

    public static void setMessageHandler(@Nullable NetworkMessageHandler handler) {
        messageHandler = handler;
    }

    public static void setExecutionHandler(@Nullable NetworkExecutionHandler handler) {
        executionHandler = handler;
    }

    public static boolean hasSecret() {
        return secretKey != null;
    }

    public static boolean isAvailable() {
        return transport != null;
    }

    public static byte @NotNull [] encode(@NotNull CorexPacket packet) {
        return PacketCodec.encode(packet, secretKey);
    }

    /**
     * Sends a packet through the active transport, refusing up front anything that cannot work.
     *
     * @param packet the packet to send.
     * @return whether it went out, and why not when it did not.
     */
    public static @NotNull SendResult send(@NotNull CorexPacket packet) {
        ProxyTransport active = transport;
        if (active == null) {
            return SendResult.failed("the Corex network layer is not running on this server");
        }
        if (packet.type().isPrivileged() && secretKey == null) {
            return SendResult.failed("sending " + packet.type().name()
                    + " needs a shared secret, set CX_NETWORK_SECRET in secrets.env on every server");
        }
        if (!active.isConnected()) {
            return SendResult.failed("the " + active.name() + " transport has no route right now");
        }
        return active.send(packet);
    }

    /**
     * Decodes one received frame and checks that it is authentic enough to be acted on or passed
     * along, without deciding what to do with it.
     *
     * <p>This is what the proxy relay uses: it must satisfy itself that a frame really came from a
     * backend that holds the secret, but the config gate on running commands belongs to the
     * server that would run them, not to the machine forwarding the bytes.</p>
     *
     * <p>Never throws: a frame that cannot be trusted or parsed is dropped with a throttled log
     * line, because the sender may well be a client trying to make this method throw.</p>
     *
     * @param frame the raw bytes received.
     * @return the packet, or null when it was dropped.
     */
    public static @Nullable CorexPacket verifyAndDecode(byte @NotNull [] frame) {
        PacketCodec.Decoded decoded;
        try {
            decoded = PacketCodec.decode(frame, secretKey);
        }
        catch (PacketFormatException e) {
            warnDropped("a malformed Corex packet (" + e.getMessage() + ")");
            return null;
        }

        CorexPacket packet = decoded.packet();
        PacketType type = packet.type();

        if (secretKey != null && !decoded.verified()) {
            warnDropped("an unsigned " + type.name() + " packet while a secret is configured");
            return null;
        }

        if (type.isPrivileged() && !decoded.verified()) {
            warnDropped("an unsigned " + type.name() + " packet, it must be signed: "
                    + "set CX_NETWORK_SECRET in secrets.env on every server");
            return null;
        }

        return packet;
    }

    public static void handleFrame(byte @NotNull [] frame) {
        CorexPacket packet = verifyAndDecode(frame);
        if (packet == null) return;
        handleDecoded(packet);
    }

    /**
     * Applies this server's own gate to an already verified packet and acts on it.
     *
     * <p>Split out from {@link #handleFrame} for the proxy, which verifies a frame in order to
     * route it and only then finds out that the target was itself.</p>
     *
     * @param packet a packet that came back from {@link #verifyAndDecode}.
     */
    public static void handleDecoded(@NotNull CorexPacket packet) {
        if (packet.type().isPrivileged() && !remoteExecutionAllowed) {
            warnDropped("a " + packet.type().name() + " packet, remote execution is switched off "
                    + "in the network section of config.yml");
            return;
        }

        dispatch(packet);
    }

    public static void shutdown() {
        transport = null;
        messageHandler = null;
        executionHandler = null;
        secretKey = null;
        remoteExecutionAllowed = false;
    }

    private static void dispatch(CorexPacket packet) {
        switch (packet) {
            case NetworkMessagePacket message -> {
                NetworkMessageHandler handler = messageHandler;
                if (handler == null) {
                    warnDropped("a MESSAGE packet on channel \"" + message.getChannel()
                            + "\", nothing on this server listens for one");
                    return;
                }
                handler.onNetworkMessage(message.getChannel(), message.getData(), message.getSource());
            }
            case ScriptPacket script -> {
                NetworkExecutionHandler handler = executionHandler;
                if (handler == null) {
                    warnDropped("a SCRIPT packet, nothing on this server can run one");
                    return;
                }
                handler.onScript(script);
            }
            default -> warnDropped("an unhandled packet type " + packet.type().name());
        }
    }

    private static void warnDropped(String reason) {
        long now = System.nanoTime();
        long previous = lastDropWarnNanos.get();

        if (previous != Long.MIN_VALUE && now - previous < DROP_WARN_INTERVAL_NANOS) {
            suppressedDrops.incrementAndGet();
            return;
        }
        if (!lastDropWarnNanos.compareAndSet(previous, now)) {
            suppressedDrops.incrementAndGet();
            return;
        }

        long suppressed = suppressedDrops.getAndSet(0L);
        CorexLogger.warn("Dropped " + reason
                + (suppressed > 0 ? " (and " + suppressed + " more in the last few seconds)" : ""));
    }
}
