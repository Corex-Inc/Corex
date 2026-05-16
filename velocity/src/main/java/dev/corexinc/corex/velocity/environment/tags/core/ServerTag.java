package dev.corexinc.corex.velocity.environment.tags.core;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.velocity.CorexVelocity;
import dev.corexinc.corex.velocity.environment.tags.player.PlayerTag;
import org.jspecify.annotations.NonNull;

import java.net.InetSocketAddress;

public class ServerTag implements AbstractTag {

    private static final String PREFIX = "server";

    public static final TagProcessor<ServerTag> TAG_PROCESSOR = new TagProcessor<>();

    private final RegisteredServer server;

    public ServerTag(RegisteredServer server) {
        this.server = server;
    }

    public ServerTag(String raw) {
        String clean = raw.toLowerCase().startsWith(PREFIX + "@") ? raw.substring(PREFIX.length() + 1) : raw;
        this.server = CorexVelocity.getInstance().getServer().getServer(clean).orElse(null);
    }

    public boolean isRegistered() {
        return server != null;
    }

    public RegisteredServer getServer() {
        return server;
    }

    public static void register() {
        BaseTagProcessor.registerBaseTag("server", (attribute) -> {
            if (!attribute.hasParam()) return null;
            ServerTag tag = new ServerTag(attribute.getParam());
            return tag.isRegistered() ? tag : null;
        });

        ObjectFetcher.registerFetcher(PREFIX, (name) -> {
            ServerTag tag = new ServerTag(name);
            return tag.isRegistered() ? tag : null;
        });

        TAG_PROCESSOR.registerTag(ElementTag.class, "name", (attribute, object) ->
                new ElementTag(object.server.getServerInfo().getName()));

        TAG_PROCESSOR.registerTag(ElementTag.class, "address", (attribute, object) -> {
            InetSocketAddress address = object.server.getServerInfo().getAddress();
            return new ElementTag(address.getHostString() + ":" + address.getPort());
        });

        TAG_PROCESSOR.registerTag(ElementTag.class, "host", (attribute, object) ->
                new ElementTag(object.server.getServerInfo().getAddress().getHostString()));

        TAG_PROCESSOR.registerTag(ElementTag.class, "port", (attribute, object) ->
                new ElementTag(object.server.getServerInfo().getAddress().getPort()));

        TAG_PROCESSOR.registerTag(ListTag.class, "players", (attribute, object) -> {
            ListTag list = new ListTag();
            for (var player : object.server.getPlayersConnected()) {
                list.addObject(new PlayerTag(player));
            }
            return list;
        });
    }

    @Override
    public @NonNull String getPrefix() {
        return PREFIX;
    }

    @Override
    public @NonNull String identify() {
        return PREFIX + "@" + server.getServerInfo().getName();
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<ServerTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public @NonNull String getTestValue() {
        return "server@lobby";
    }
}