package dev.corexinc.corex.shared.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public sealed interface CorexPacket permits CorexPacket.ServerBound, CorexPacket.ClientBound {

    CorexPacketType<? extends CorexPacket> type();

    void write(ByteBuf buf);
    void read(ByteBuf buf);

    non-sealed interface ServerBound extends CorexPacket {}
    non-sealed interface ClientBound extends CorexPacket {}

    final class CorexPacketType<P extends CorexPacket> {

        private static final Map<Integer, CorexPacketType<?>> BY_ID = new HashMap<>();

        private final int id;
        private final String name;
        private final Supplier<P> constructor;

        private CorexPacketType(int id, String name, Supplier<P> constructor) {
            this.id = id;
            this.name = name;
            this.constructor = constructor;
        }

        public int id() {
            return id;
        }

        public String name() {
            return name;
        }

        public P create() {
            return constructor.get();
        }

        public static <P extends CorexPacket> CorexPacketType<P> of(
                int id, String name, Supplier<P> constructor) {
            if (BY_ID.containsKey(id))
                throw new IllegalStateException(
                        "Duplicate CoreX packet ID 0x" + Integer.toHexString(id) +
                                " - already registered as " + BY_ID.get(id).name);

            CorexPacketType<P> type = new CorexPacketType<>(id, name, constructor);
            BY_ID.put(id, type);
            return type;
        }

        public static CorexPacketType<?> byId(int id) {
            CorexPacketType<?> type = BY_ID.get(id);
            if (type == null)
                throw new IllegalArgumentException(
                        "Unknown CoreX packet ID: 0x" + Integer.toHexString(id));
            return type;
        }

        @Override
        public String toString() {
            return "CorexPacketType[" + name + "(0x" + Integer.toHexString(id) + ")]";
        }
    }

    final class Codec {

        private Codec() {}

        public static CorexPacket decode(ByteBuf buf) {
            int id = buf.readUnsignedByte();
            CorexPacketType<?> type = CorexPacketType.byId(id);
            CorexPacket packet = type.create();
            packet.read(buf);
            return packet;
        }

        public static ByteBuf encode(CorexPacket packet, ByteBufAllocator alloc) {
            ByteBuf buf = alloc.buffer();
            buf.writeByte(packet.type().id());
            packet.write(buf);
            return buf;
        }
    }
}