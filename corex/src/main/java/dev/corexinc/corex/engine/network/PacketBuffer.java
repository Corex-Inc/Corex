package dev.corexinc.corex.engine.network;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * A growable byte cursor used to serialise and deserialise {@link CorexPacket} payloads.
 *
 * <p>An instance is either a writer (created by {@link #writer()}) or a reader (created by
 * {@link #reader(byte[])}); mixing the two on one instance is a programming error and is not
 * checked for.</p>
 *
 * <h2>Why every read is bounded</h2>
 * <p>A packet arrives as an untrusted byte array, so the length prefix in front of a string or a
 * collection is attacker controlled. Allocating whatever that prefix says turns a single 5 byte
 * frame into an {@link OutOfMemoryError}. Every length read here is checked against
 * {@link #MAX_STRING_BYTES} or {@link #MAX_COLLECTION_SIZE} <b>and</b> against the number of bytes
 * actually remaining, so a claimed length can never exceed what the frame could possibly hold.</p>
 *
 * @since 1.0.0
 */
public final class PacketBuffer {

    public static final int MAX_STRING_BYTES = 65_536;

    public static final int MAX_COLLECTION_SIZE = 4_096;

    private static final int MAX_VAR_INT_BYTES = 5;

    private byte[] data;
    private int position;
    private int limit;

    private PacketBuffer(byte[] data, int limit) {
        this.data = data;
        this.limit = limit;
        this.position = 0;
    }

    public static @NotNull PacketBuffer writer() {
        return new PacketBuffer(new byte[64], 0);
    }

    public static @NotNull PacketBuffer reader(byte @NotNull [] source) {
        return new PacketBuffer(source, source.length);
    }

    public byte @NotNull [] toByteArray() {
        return Arrays.copyOf(data, limit);
    }

    public int readableBytes() {
        return limit - position;
    }

    public void writeByte(int value) {
        ensureCapacity(1);
        data[limit++] = (byte) value;
    }

    public int readUnsignedByte() {
        require(1);
        return data[position++] & 0xFF;
    }

    public void writeBytes(byte @NotNull [] value) {
        ensureCapacity(value.length);
        System.arraycopy(value, 0, data, limit, value.length);
        limit += value.length;
    }

    public byte @NotNull [] readBytes(int length) {
        if (length < 0) {
            throw new PacketFormatException("Negative byte count: " + length);
        }
        require(length);
        byte[] result = Arrays.copyOfRange(data, position, position + length);
        position += length;
        return result;
    }

    public void writeVarInt(int value) {
        int remaining = value;
        while ((remaining & 0xFFFFFF80) != 0) {
            writeByte((remaining & 0x7F) | 0x80);
            remaining >>>= 7;
        }
        writeByte(remaining);
    }

    public int readVarInt() {
        int result = 0;
        int shift = 0;
        for (int index = 0; index < MAX_VAR_INT_BYTES; index++) {
            int current = readUnsignedByte();
            result |= (current & 0x7F) << shift;
            if ((current & 0x80) == 0) {
                return result;
            }
            shift += 7;
        }
        throw new PacketFormatException("VarInt is longer than " + MAX_VAR_INT_BYTES + " bytes");
    }

    public void writeString(@NotNull String value) {
        byte[] encoded = value.getBytes(StandardCharsets.UTF_8);
        if (encoded.length > MAX_STRING_BYTES) {
            throw new PacketFormatException(
                    "String of " + encoded.length + " bytes exceeds the " + MAX_STRING_BYTES + " byte limit");
        }
        writeVarInt(encoded.length);
        writeBytes(encoded);
    }

    public @NotNull String readString() {
        int length = readVarInt();
        if (length < 0 || length > MAX_STRING_BYTES) {
            throw new PacketFormatException("Declared string length out of range: " + length);
        }
        return new String(readBytes(length), StandardCharsets.UTF_8);
    }

    public void writeStringList(@NotNull List<String> values) {
        if (values.size() > MAX_COLLECTION_SIZE) {
            throw new PacketFormatException(
                    "List of " + values.size() + " entries exceeds the " + MAX_COLLECTION_SIZE + " entry limit");
        }
        writeVarInt(values.size());
        for (String value : values) {
            writeString(value);
        }
    }

    public @NotNull List<String> readStringList() {
        int size = readVarInt();
        if (size < 0 || size > MAX_COLLECTION_SIZE) {
            throw new PacketFormatException("Declared list size out of range: " + size);
        }
        if (size > readableBytes()) {
            throw new PacketFormatException(
                    "Declared list size " + size + " exceeds the " + readableBytes() + " bytes left in the frame");
        }
        List<String> result = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            result.add(readString());
        }
        return result;
    }

    public void writeNullableUuid(@Nullable UUID value) {
        writeByte(value != null ? 1 : 0);
        if (value != null) {
            writeLong(value.getMostSignificantBits());
            writeLong(value.getLeastSignificantBits());
        }
    }

    public @Nullable UUID readNullableUuid() {
        return readUnsignedByte() != 0 ? new UUID(readLong(), readLong()) : null;
    }

    public void writeLong(long value) {
        for (int shift = 56; shift >= 0; shift -= 8) {
            writeByte((int) (value >>> shift));
        }
    }

    public long readLong() {
        long result = 0L;
        for (int index = 0; index < 8; index++) {
            result = (result << 8) | readUnsignedByte();
        }
        return result;
    }

    private void ensureCapacity(int extra) {
        if (limit + extra <= data.length) {
            return;
        }
        int target = data.length;
        while (target < limit + extra) {
            target <<= 1;
        }
        data = Arrays.copyOf(data, target);
    }

    private void require(int amount) {
        if (readableBytes() < amount) {
            throw new PacketFormatException(
                    "Truncated packet: needed " + amount + " bytes, " + readableBytes() + " left");
        }
    }
}
