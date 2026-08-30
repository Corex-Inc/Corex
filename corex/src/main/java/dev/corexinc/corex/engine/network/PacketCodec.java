package dev.corexinc.corex.engine.network;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Turns a {@link CorexPacket} into a frame and back, optionally authenticated.
 *
 * <h2>Frame layout</h2>
 * <pre>
 * [1]  protocol version
 * [1]  flags            bit 0 = signed
 * [32] HMAC-SHA256      present only when signed, computed over the four fields below
 * [1]  packet id
 * [N]  packet payload
 * </pre>
 *
 * <p>The signature covers the version and flag bytes as well as the body, so neither the protocol
 * version nor the signed bit can be edited in transit without invalidating it.</p>
 *
 * <h2>Verify before parse</h2>
 * <p>{@link #decode} checks the signature before it reads a single field of the payload. On a
 * signed setup that means a forged or corrupt frame never reaches
 * {@link CorexPacket#read(PacketBuffer)} at all, so the decoder's bounds checks are a second line
 * of defence rather than the only one.</p>
 *
 * @since 1.0.0
 */
public final class PacketCodec {

    /**
     * Version byte written into every frame. Bumped whenever the layout of an existing packet
     * changes, so a half updated network refuses to talk rather than misreading fields.
     */
    public static final byte PROTOCOL_VERSION = 1;

    private static final String MAC_ALGORITHM = "HmacSHA256";
    private static final int MAC_LENGTH = 32;
    private static final int FLAG_SIGNED = 0x01;

    /**
     * Domain separation label mixed into every derived key. Changing it invalidates every key on
     * the network at once, so treat it as part of the protocol version.
     */
    private static final String KEY_LABEL = "corex-network-v1";

    private PacketCodec() {}

    /**
     * The result of decoding a frame: the packet itself, plus whether its signature was checked
     * and found valid. Policy on what an unverified packet may do lives in {@link NetworkManager}.
     *
     * @param packet   the decoded packet.
     * @param verified true when the frame carried a signature that matched the configured key.
     */
    public record Decoded(@NotNull CorexPacket packet, boolean verified) {}

    public static byte @NotNull [] encode(@NotNull CorexPacket packet, byte @Nullable [] key) {
        PacketBuffer payload = PacketBuffer.writer();
        packet.write(payload);

        byte flags = (byte) (key != null ? FLAG_SIGNED : 0);
        byte[] body = buildBody(packet.type().id(), payload.toByteArray());

        PacketBuffer frame = PacketBuffer.writer();
        frame.writeByte(PROTOCOL_VERSION);
        frame.writeByte(flags);
        if (key != null) {
            frame.writeBytes(sign(key, PROTOCOL_VERSION, flags, body));
        }
        frame.writeBytes(body);
        return frame.toByteArray();
    }

    /**
     * Parses a frame back into a packet.
     *
     * @param frame the bytes received off the wire.
     * @param key   the shared secret, or null when this side has none configured.
     * @return the decoded packet and whether its signature checked out.
     * @throws PacketFormatException if the frame is truncated, uses an unknown version or packet
     *                               id, or carries a signature that does not match {@code key}.
     */
    public static @NotNull Decoded decode(byte @NotNull [] frame, byte @Nullable [] key) {
        PacketBuffer buffer = PacketBuffer.reader(frame);

        int version = buffer.readUnsignedByte();
        if (version != PROTOCOL_VERSION) {
            throw new PacketFormatException(
                    "Unsupported Corex protocol version " + version + ", expected " + PROTOCOL_VERSION);
        }

        int flags = buffer.readUnsignedByte();
        boolean signed = (flags & FLAG_SIGNED) != 0;

        byte[] declaredMac = signed ? buffer.readBytes(MAC_LENGTH) : null;
        byte[] body = buffer.readBytes(buffer.readableBytes());

        boolean verified = false;
        if (signed && key != null) {
            byte[] expected = sign(key, (byte) version, (byte) flags, body);
            if (!MessageDigest.isEqual(expected, declaredMac)) {
                throw new PacketFormatException("Corex packet signature does not match");
            }
            verified = true;
        }

        PacketBuffer bodyBuffer = PacketBuffer.reader(body);
        PacketType type = PacketType.byId(bodyBuffer.readUnsignedByte());
        CorexPacket packet = type.create();
        packet.read(bodyBuffer);
        return new Decoded(packet, verified);
    }

    /**
     * Derives the HMAC key from the configured secret string.
     *
     * <p>The secret is never used as the key directly. Corex will happily take the proxy's own
     * forwarding secret so that a network works with no extra setup, and that string already has a
     * job: it authenticates player forwarding handshakes. Running it through a fixed label first
     * means the bytes signing a Corex packet are not the bytes Velocity checks, so neither
     * protocol can be attacked through the other. It also normalises any secret, long or short, to
     * the 32 bytes HMAC-SHA256 wants.</p>
     *
     * @param secret the raw secret, or null when none is configured.
     * @return the derived key, or null when {@code secret} is null or blank.
     */
    public static byte @Nullable [] toKey(@Nullable String secret) {
        if (secret == null || secret.isBlank()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance(MAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), MAC_ALGORITHM));
            return mac.doFinal(KEY_LABEL.getBytes(StandardCharsets.UTF_8));
        }
        catch (Exception e) {
            throw new PacketFormatException("Failed to derive the network key: " + e.getMessage(), e);
        }
    }

    private static byte[] buildBody(int packetId, byte[] payload) {
        byte[] body = new byte[payload.length + 1];
        body[0] = (byte) packetId;
        System.arraycopy(payload, 0, body, 1, payload.length);
        return body;
    }

    private static byte[] sign(byte[] key, byte version, byte flags, byte[] body) {
        try {
            Mac mac = Mac.getInstance(MAC_ALGORITHM);
            mac.init(new SecretKeySpec(key, MAC_ALGORITHM));
            mac.update(version);
            mac.update(flags);
            mac.update(body);
            return Arrays.copyOf(mac.doFinal(), MAC_LENGTH);
        } catch (Exception e) {
            throw new PacketFormatException("Failed to compute the packet signature: " + e.getMessage(), e);
        }
    }
}
