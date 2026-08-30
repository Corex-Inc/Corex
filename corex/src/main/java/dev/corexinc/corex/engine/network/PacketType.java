package dev.corexinc.corex.engine.network;

import dev.corexinc.corex.engine.network.packets.HelloPacket;
import dev.corexinc.corex.engine.network.packets.NetworkMessagePacket;
import dev.corexinc.corex.engine.network.packets.ScriptPacket;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * The complete set of Corex proxy packets.
 *
 * <p>An enum rather than a registry map: the id table is built once in a static initialiser, a
 * duplicate id is a compile time impossibility, and there is no bootstrap call to forget. The
 * previous design registered types from the static initialisers of the packet classes themselves,
 * which meant the protocol only existed after something happened to touch every class.</p>
 *
 * @since 1.0.0
 */
public enum PacketType {

    /**
     * A named message carrying an arbitrary tag payload, delivered either to the
     * {@code proxy message} script event or straight to another plugin's channel.
     */
    MESSAGE(0x00, false, NetworkMessagePacket::new),

    /**
     * An uncompiled script block to compile and run on the receiving side.
     */
    SCRIPT(0x01, true, ScriptPacket::new),

    /**
     * A socket connected backend naming itself to the proxy, sent once per connection.
     */
    HELLO(0x02, true, HelloPacket::new);

    private static final PacketType[] BY_ID = buildIdTable();

    private final int id;
    private final boolean privileged;
    private final Supplier<? extends CorexPacket> factory;

    PacketType(int id, boolean privileged, Supplier<? extends CorexPacket> factory) {
        this.id = id;
        this.privileged = privileged;
        this.factory = factory;
    }

    public int id() {
        return id;
    }

    /**
     * Whether acting on this packet requires a verified signature.
     *
     * <p>A privileged packet makes the receiver do something a player could not otherwise ask for,
     * so it is refused outright when no shared secret is configured. A plain
     * {@link #MESSAGE} only reaches script logic the server owner wrote, or a channel they named,
     * so it stays usable on an unsigned setup.</p>
     *
     * @return true when the packet may only be acted on after signature verification.
     */
    public boolean isPrivileged() {
        return privileged;
    }

    public @NotNull CorexPacket create() {
        return factory.get();
    }

    /**
     * Looks a type up by its wire id.
     *
     * @param id the id read off the wire.
     * @return the matching type.
     * @throws PacketFormatException if no type uses that id.
     */
    public static @NotNull PacketType byId(int id) {
        if (id < 0 || id >= BY_ID.length || BY_ID[id] == null) {
            throw new PacketFormatException("Unknown Corex packet id: 0x" + Integer.toHexString(id));
        }
        return BY_ID[id];
    }

    private static PacketType[] buildIdTable() {
        int highest = 0;
        for (PacketType type : values()) {
            highest = Math.max(highest, type.id);
        }
        PacketType[] table = new PacketType[highest + 1];
        for (PacketType type : values()) {
            if (table[type.id] != null) {
                throw new IllegalStateException(
                        "Duplicate Corex packet id 0x" + Integer.toHexString(type.id)
                                + " on " + type.name() + " and " + table[type.id].name());
            }
            table[type.id] = type;
        }
        return table;
    }
}
