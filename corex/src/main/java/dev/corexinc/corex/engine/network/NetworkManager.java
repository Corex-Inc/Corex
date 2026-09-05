package dev.corexinc.corex.engine.network;

import dev.corexinc.corex.engine.network.packets.NetworkMessagePacket;
import dev.corexinc.corex.engine.network.packets.ScriptPacket;
import dev.corexinc.corex.engine.utils.CorexLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
 *   <li>A verified frame older than the replay window, or carrying a nonce already seen inside
 *       it: dropped, so a captured frame cannot be sent twice.</li>
 *   <li>An unverified frame from a source that sent more than the configured number in the last
 *       second: dropped, so a client cannot flood script queues with plain messages.</li>
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

    /**
     * How far a frame's timestamp may sit from this server's clock before it is refused.
     */
    public static final long DEFAULT_REPLAY_WINDOW_MS = 30_000L;

    /**
     * Unverified frames one source may deliver per second before the rest are dropped.
     */
    public static final int DEFAULT_UNSIGNED_RATE_LIMIT = 20;

    private static final int NONCE_PRUNE_THRESHOLD = 4_096;

    private static final Map<Long, Long> seenNonces = new ConcurrentHashMap<>();
    private static final Map<String, long[]> unsignedCounters = new ConcurrentHashMap<>();

    private static volatile long replayWindowMs = DEFAULT_REPLAY_WINDOW_MS;
    private static volatile int unsignedRateLimit = DEFAULT_UNSIGNED_RATE_LIMIT;

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
        configure(secret, remoteExecutionAllowed, DEFAULT_REPLAY_WINDOW_MS, DEFAULT_UNSIGNED_RATE_LIMIT);
    }

    /**
     * Applies the network section of the config.
     *
     * @param secret                 the shared secret, or null for none.
     * @param remoteExecutionAllowed whether privileged packets may run here.
     * @param replayWindowMs         how old a verified frame may be; {@code 0} keeps the default.
     * @param unsignedRateLimit      unverified frames per source per second; {@code 0} keeps the
     *                               default, a negative value disables the limit.
     */
    public static void configure(@Nullable String secret, boolean remoteExecutionAllowed,
                                 long replayWindowMs, int unsignedRateLimit) {
        secretKey = PacketCodec.toKey(secret);
        NetworkManager.remoteExecutionAllowed = remoteExecutionAllowed;
        NetworkManager.replayWindowMs = replayWindowMs > 0 ? replayWindowMs : DEFAULT_REPLAY_WINDOW_MS;
        NetworkManager.unsignedRateLimit = unsignedRateLimit == 0 ? DEFAULT_UNSIGNED_RATE_LIMIT : unsignedRateLimit;
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
        PacketCodec.Decoded decoded = decodeChecked(frame);
        return decoded != null ? decoded.packet() : null;
    }

    private static @Nullable PacketCodec.Decoded decodeChecked(byte @NotNull [] frame) {
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

        if (decoded.verified() && isReplay(decoded)) {
            warnDropped("a replayed or stale " + type.name() + " packet");
            return null;
        }

        return decoded;
    }

    private static boolean isReplay(PacketCodec.Decoded decoded) {
        long now = System.currentTimeMillis();
        if (Math.abs(now - decoded.timestamp()) > replayWindowMs) {
            return true;
        }
        long expiresAt = now + replayWindowMs;
        if (seenNonces.putIfAbsent(decoded.nonce(), expiresAt) != null) {
            return true;
        }
        if (seenNonces.size() > NONCE_PRUNE_THRESHOLD) {
            pruneNonces(now);
        }
        return false;
    }

    private static void pruneNonces(long now) {
        Iterator<Map.Entry<Long, Long>> entries = seenNonces.entrySet().iterator();
        while (entries.hasNext()) {
            if (entries.next().getValue() < now) entries.remove();
        }
    }

    private static boolean overUnsignedLimit(String sourceKey) {
        int limit = unsignedRateLimit;
        if (limit < 0) return false;

        long now = System.currentTimeMillis();
        long[] counter = unsignedCounters.computeIfAbsent(sourceKey, key -> new long[] {now, 0L});
        synchronized (counter) {
            if (now - counter[0] >= 1_000L) {
                counter[0] = now;
                counter[1] = 0L;
            }
            counter[1]++;
            return counter[1] > limit;
        }
    }

    /**
     * Forgets the unsigned frame counter of a source, for when the source goes away.
     *
     * @param sourceKey the key handed to {@link #handleFrame(byte[], String)}.
     */
    public static void forgetSource(@NotNull String sourceKey) {
        unsignedCounters.remove(sourceKey);
    }

    /**
     * Decodes, checks and acts on one received frame.
     *
     * @param frame     the raw bytes received.
     * @param sourceKey who delivered it, used to rate limit unverified frames per source.
     */
    public static void handleFrame(byte @NotNull [] frame, @NotNull String sourceKey) {
        PacketCodec.Decoded decoded = decodeChecked(frame);
        if (decoded == null) return;

        if (!decoded.verified() && overUnsignedLimit(sourceKey)) {
            warnDropped("an unsigned " + decoded.packet().type().name() + " packet from " + sourceKey
                    + ", over the unsigned-rate-limit of " + unsignedRateLimit + " per second");
            return;
        }

        handleDecoded(decoded.packet());
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
        seenNonces.clear();
        unsignedCounters.clear();
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
