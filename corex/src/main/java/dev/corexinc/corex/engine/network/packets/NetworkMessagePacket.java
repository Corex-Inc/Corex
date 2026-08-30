package dev.corexinc.corex.engine.network.packets;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.network.PacketBuffer;
import dev.corexinc.corex.engine.network.PacketType;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A named message with an arbitrary tag payload.
 *
 * <h2>Two kinds of channel</h2>
 * <p>A channel with no namespace, {@code bossDown}, is a Corex channel: the proxy wraps the
 * message back up and the receiving backend fires the {@code proxy message} script event on it.
 * A channel that looks like a Minecraft channel identifier, {@code my:customchannel}, is a real
 * plugin messaging channel: the proxy unwraps the payload and sends it there instead, so a plugin
 * that has nothing to do with Corex picks it up through its own listener.</p>
 *
 * <p>The split is on the namespace separator because a Minecraft channel always has one, so a
 * bare word can never collide with a real channel.</p>
 *
 * <h2>Why the payload is an identify string</h2>
 * <p>It travels as the {@code identify()} of whatever tag the script passed and is rebuilt through
 * {@link ObjectFetcher#pickObject(String)} on arrival. Going through the engine's own serialiser
 * is what lets a nested MapTag survive the trip: a hand rolled {@code key=value} split would
 * corrupt any value containing a separator character.</p>
 *
 * @since 1.0.0
 */
public class NetworkMessagePacket extends RoutedPacket {

    private String channel = "";
    private String payload = "";

    public NetworkMessagePacket() {}

    public NetworkMessagePacket(@NotNull String target, @NotNull String channel, @Nullable AbstractTag data) {
        super(target);
        this.channel = channel;
        this.payload = data != null ? data.identify() : "";
    }

    @Override
    public @NotNull PacketType type() {
        return PacketType.MESSAGE;
    }

    public @NotNull String getChannel() {
        return channel;
    }

    public boolean isPluginChannel() {
        return channel.indexOf(':') >= 0;
    }

    public @NotNull String getRawPayload() {
        return payload;
    }

    public @Nullable AbstractTag getData() {
        return payload.isEmpty() ? null : ObjectFetcher.pickObject(payload);
    }

    @Override
    public void write(@NotNull PacketBuffer buffer) {
        writeRouting(buffer);
        buffer.writeString(channel);
        buffer.writeString(payload);
    }

    @Override
    public void read(@NotNull PacketBuffer buffer) {
        readRouting(buffer);
        channel = buffer.readString();
        payload = buffer.readString();
    }
}
