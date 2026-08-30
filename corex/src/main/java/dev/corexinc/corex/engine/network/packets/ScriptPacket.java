package dev.corexinc.corex.engine.network.packets;

import dev.corexinc.corex.engine.network.PacketBuffer;
import dev.corexinc.corex.engine.network.PacketType;
import dev.corexinc.corex.engine.network.ScriptBlockCodec;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * An uncompiled script block for the receiving side to compile and run.
 *
 * <p>Privileged: the lines are compiled against the receiving server's registry, so acting on an
 * unsigned one would hand whoever sent it that server's whole command set.</p>
 *
 * <p>The player, when present, comes from the {@code player:} global flag on the sending
 * instruction rather than from an argument of its own. The command has no {@code player:} prefix
 * because the global flag already owns that word, and a command prefix by the same name would sit
 * underneath it and never be read.</p>
 *
 * @since 1.0.0
 */
public class ScriptPacket extends RoutedPacket {

    private String block = "[]";

    @Nullable
    private UUID playerUuid;

    public ScriptPacket() {}

    public ScriptPacket(@NotNull String target, @NotNull List<?> block, @Nullable UUID playerUuid) {
        super(target);
        this.block = ScriptBlockCodec.encode(block);
        this.playerUuid = playerUuid;
    }

    @Override
    public @NotNull PacketType type() {
        return PacketType.SCRIPT;
    }

    public @NotNull List<Object> getBlock() {
        return ScriptBlockCodec.decode(block);
    }

    public @Nullable UUID getPlayerUuid() {
        return playerUuid;
    }

    @Override
    public void write(@NotNull PacketBuffer buffer) {
        writeRouting(buffer);
        buffer.writeString(block);
        buffer.writeNullableUuid(playerUuid);
    }

    @Override
    public void read(@NotNull PacketBuffer buffer) {
        readRouting(buffer);
        block = buffer.readString();
        playerUuid = buffer.readNullableUuid();
    }
}
