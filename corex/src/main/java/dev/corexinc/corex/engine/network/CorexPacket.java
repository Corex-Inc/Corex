package dev.corexinc.corex.engine.network;

import org.jetbrains.annotations.NotNull;

/**
 * One message on the Corex proxy protocol.
 *
 * <p>A packet is a mutable holder rather than a record because decoding constructs it empty
 * through {@link PacketType#create()} and then fills it from the wire. Implementations are used
 * from a single thread at a time and are never shared between the encode and decode sides.</p>
 *
 * <p>There is deliberately no separate type per direction. The same {@code MESSAGE} travels
 * backend to proxy and proxy to backend, so a second class would only duplicate the fields; who
 * is allowed to act on a packet is decided by the handler that receives it, not by its type.</p>
 *
 * @since 1.0.0
 */
public interface CorexPacket {

    @NotNull PacketType type();

    /**
     * Writes every field of this packet into {@code buffer}, in the order {@link #read} expects.
     *
     * @param buffer the writer to append to.
     */
    void write(@NotNull PacketBuffer buffer);

    /**
     * Fills this packet from {@code buffer}. Called on an instance fresh from
     * {@link PacketType#create()}.
     *
     * @param buffer the reader to consume.
     * @throws PacketFormatException if the bytes do not match the expected layout.
     */
    void read(@NotNull PacketBuffer buffer);
}
