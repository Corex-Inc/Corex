package dev.corexinc.corex.shared.network;

import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class PacketBufUtils {

    private PacketBufUtils() {}

    public static void writeString(ByteBuf buf, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        buf.writeShort(bytes.length);
        buf.writeBytes(bytes);
    }

    public static String readString(ByteBuf buf) {
        int length = buf.readShort();
        byte[] bytes = new byte[length];
        buf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static void writeNullableString(ByteBuf buf, @Nullable String value) {
        buf.writeBoolean(value != null);
        if (value != null) writeString(buf, value);
    }

    @Nullable
    public static String readNullableString(ByteBuf buf) {
        return buf.readBoolean() ? readString(buf) : null;
    }

    public static void writeUUID(ByteBuf buf, UUID uuid) {
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }

    public static UUID readUUID(ByteBuf buf) {
        return new UUID(buf.readLong(), buf.readLong());
    }

    public static void writeNullableUUID(ByteBuf buf, @Nullable UUID uuid) {
        buf.writeBoolean(uuid != null);
        if (uuid != null) writeUUID(buf, uuid);
    }

    @Nullable
    public static UUID readNullableUUID(ByteBuf buf) {
        return buf.readBoolean() ? readUUID(buf) : null;
    }

    public static void writeStringList(ByteBuf buf, List<String> list) {
        buf.writeInt(list.size());
        for (String s : list) writeString(buf, s);
    }

    public static List<String> readStringList(ByteBuf buf) {
        int size = buf.readInt();
        List<String> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) list.add(readString(buf));
        return list;
    }

    public static void writeStringMap(ByteBuf buf, Map<String, String> map) {
        buf.writeInt(map.size());
        map.forEach((k, v) -> {
            writeString(buf, k);
            writeString(buf, v);
        });
    }

    public static Map<String, String> readStringMap(ByteBuf buf) {
        int size = buf.readInt();
        Map<String, String> map = new LinkedHashMap<>(size);
        for (int i = 0; i < size; i++) map.put(readString(buf), readString(buf));
        return map;
    }

    public static void writeStringIntMap(ByteBuf buf, Map<String, Integer> map) {
        buf.writeInt(map.size());
        map.forEach((k, v) -> {
            writeString(buf, k);
            buf.writeInt(v);
        });
    }

    public static Map<String, Integer> readStringIntMap(ByteBuf buf) {
        int size = buf.readInt();
        Map<String, Integer> map = new LinkedHashMap<>(size);
        for (int i = 0; i < size; i++) map.put(readString(buf), buf.readInt());
        return map;
    }
}