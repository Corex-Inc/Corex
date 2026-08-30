package dev.corexinc.corex.engine.network;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/**
 * Writes a payload in the shape a plugin that has never heard of Corex expects.
 *
 * <p>When a message is aimed at a namespaced channel it leaves the Corex protocol entirely: no
 * frame header, no signature, nothing the receiving plugin would have to know about. What goes on
 * the wire is a single string in Java's modified UTF-8, which is what
 * {@code ByteStreams.newDataInput(bytes).readUTF()} reads. That call is the near universal first
 * line of a Bungee style plugin message handler, so it is the one format most likely to be
 * understood on the other end.</p>
 *
 * @since 1.0.0
 */
public final class ForeignChannelPayload {

    private ForeignChannelPayload() {}

    public static byte @NotNull [] encode(@NotNull String payload) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeUTF(payload);
        }
        catch (IOException e) {
            throw new IllegalStateException("Writing to a byte array cannot fail", e);
        }
        return bytes.toByteArray();
    }
}
