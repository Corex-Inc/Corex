package dev.corexinc.corex.velocity.environment.network;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.network.NetworkExecutionHandler;
import dev.corexinc.corex.engine.network.PacketFormatException;
import dev.corexinc.corex.engine.network.packets.ScriptPacket;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.scripts.ScriptManager;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.velocity.environment.tags.player.PlayerTag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Compiles and runs a script block on the proxy itself.
 *
 * <p>This is what makes {@code - proxy script:} with no {@code to:} useful: the block is compiled
 * against the proxy's registry, so it can reach the proxy only commands and tags a backend does
 * not have.</p>
 *
 * @since 1.0.0
 */
public final class VelocityNetworkExecutor implements NetworkExecutionHandler {

    private final ProxyServer server;

    public VelocityNetworkExecutor(@NotNull ProxyServer server) {
        this.server = server;
    }

    @Override
    public void onScript(@NotNull ScriptPacket packet) {
        List<Object> block;
        try {
            block = packet.getBlock();
        }
        catch (PacketFormatException e) {
            CorexLogger.warn("Corex script from " + packet.getSource()
                    + " could not be read: " + e.getMessage());
            return;
        }

        Instruction[] bytecode = ScriptManager.compileBlock(block);
        if (bytecode.length == 0) {
            CorexLogger.warn("Corex script from " + packet.getSource()
                    + " compiled to nothing, check the sending script for commands the proxy does not have.");
            return;
        }

        UUID playerUuid = packet.getPlayerUuid();
        PlayerTag linkedPlayer = null;

        if (playerUuid != null) {
            Optional<com.velocitypowered.api.proxy.Player> player = server.getPlayer(playerUuid);
            if (player.isEmpty()) {
                CorexLogger.warn("Corex script from " + packet.getSource()
                        + " is linked to player " + playerUuid + ", who is not on this proxy.");
                return;
            }
            linkedPlayer = new PlayerTag(player.get());
        }

        ScriptQueue queue = new ScriptQueue(
                ScriptQueue.uniqueId("Proxy"),
                bytecode,
                false,
                linkedPlayer
        );

        SchedulerAdapter.get().run(queue::start);
    }
}
