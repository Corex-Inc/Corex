package dev.corexinc.corex.engine.network;

import dev.corexinc.corex.engine.network.packets.ScriptPacket;
import org.jetbrains.annotations.NotNull;

/**
 * Compiles and runs a script block that arrived over the network, on behalf of the platform.
 *
 * <p>{@link NetworkManager} decides whether a packet is allowed to run at all; an implementation
 * of this interface only decides how. By the time this is called the frame has been signature
 * checked and the config gate has been passed.</p>
 *
 * @since 1.0.0
 */
public interface NetworkExecutionHandler {

    void onScript(@NotNull ScriptPacket packet);
}
