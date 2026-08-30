package dev.corexinc.corex.environment.network;

import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.network.NetworkExecutionHandler;
import dev.corexinc.corex.engine.network.PacketFormatException;
import dev.corexinc.corex.engine.network.packets.ScriptPacket;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.scripts.ScriptManager;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.engine.utils.Position;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.utils.BukkitSchedulerAdapter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Compiles and runs a script block that arrived from another server, on a Paper backend.
 *
 * <p>Only reached for a packet that {@link dev.corexinc.corex.engine.network.NetworkManager}
 * already signature checked and cleared against the config gate, so there is no trust decision
 * left to make here, only which region the queue belongs on.</p>
 *
 * @since 1.0.0
 */
public final class BukkitNetworkExecutor implements NetworkExecutionHandler {

    @Override
    public void onScript(@NotNull ScriptPacket packet) {
        List<Object> block;
        try {
            block = packet.getBlock();
        }
        catch (PacketFormatException e) {
            CorexLogger.warn("Corex script from " + describeSource(packet)
                    + " could not be read: " + e.getMessage());
            return;
        }

        Instruction[] bytecode = ScriptManager.compileBlock(block);
        if (bytecode == null || bytecode.length == 0) {
            CorexLogger.warn("Corex script from " + describeSource(packet)
                    + " compiled to nothing, check the sending script for commands this server does not have.");
            return;
        }

        UUID playerUuid = packet.getPlayerUuid();
        Player player = playerUuid != null ? Bukkit.getPlayer(playerUuid) : null;

        if (playerUuid != null && player == null) {
            CorexLogger.warn("Corex script from " + describeSource(packet)
                    + " is linked to player " + playerUuid + ", who is not on this server.");
            return;
        }

        PlayerTag linkedPlayer = player != null ? new PlayerTag(player) : null;
        Position anchor = player != null ? BukkitSchedulerAdapter.toPosition(player.getLocation()) : null;

        ScriptQueue queue = new ScriptQueue(
                ScriptQueue.uniqueId("Proxy"),
                bytecode,
                false,
                linkedPlayer,
                anchor
        );

        if (anchor != null) {
            SchedulerAdapter.get().runAt(anchor, queue::start);
        }
        else {
            SchedulerAdapter.get().run(queue::start);
        }
    }

    private static String describeSource(ScriptPacket packet) {
        return packet.getSource().isEmpty() ? "the proxy" : packet.getSource();
    }
}
