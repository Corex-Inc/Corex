package dev.corexinc.corex.engine.network.packets;

import dev.corexinc.corex.engine.network.CorexPacket;
import dev.corexinc.corex.engine.network.PacketBuffer;
import org.jetbrains.annotations.NotNull;

/**
 * Base for every packet that names where it is going and where it came from.
 *
 * <h2>Targets</h2>
 * <ul>
 *   <li>{@link #PROXY}, the empty string, means the proxy itself handles the packet and forwards
 *       nothing. This is what a script gets when it leaves {@code to:} off.</li>
 *   <li>{@link #BROADCAST} means every backend except the one that sent it.</li>
 *   <li>Anything else names one backend.</li>
 * </ul>
 *
 * <p>{@code source} is the opposite of a target: the sender leaves it empty and the proxy
 * overwrites it with the name of the server the packet actually arrived from, which is the only
 * reason the receiving side can trust it.</p>
 *
 * @since 1.0.0
 */
public abstract class RoutedPacket implements CorexPacket {

    /**
     * Target value meaning "the proxy handles this itself".
     */
    public static final String PROXY = "";

    /**
     * Target value meaning "every server except the one that sent this".
     */
    public static final String BROADCAST = "*";

    private String target = PROXY;
    private String source = "";

    protected RoutedPacket() {}

    protected RoutedPacket(@NotNull String target) {
        this.target = target;
    }

    public @NotNull String getTarget() {
        return target;
    }

    public @NotNull String getSource() {
        return source;
    }

    public void setSource(@NotNull String source) {
        this.source = source;
    }

    public boolean isProxyTarget() {
        return PROXY.equals(target);
    }

    public boolean isBroadcast() {
        return BROADCAST.equals(target);
    }

    protected void writeRouting(@NotNull PacketBuffer buffer) {
        buffer.writeString(target);
        buffer.writeString(source);
    }

    protected void readRouting(@NotNull PacketBuffer buffer) {
        target = buffer.readString();
        source = buffer.readString();
    }
}
