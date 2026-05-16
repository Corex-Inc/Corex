package dev.corexinc.corex.velocity.environment.tags.core;

import com.velocitypowered.api.proxy.ProxyServer;
import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.velocity.CorexVelocity;
import dev.corexinc.corex.velocity.environment.tags.player.PlayerTag;
import org.jspecify.annotations.NonNull;

import java.net.InetSocketAddress;

public class VelocityTag implements AbstractTag {

    private static final String PREFIX = "velocity";

    public static final TagProcessor<VelocityTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag(PREFIX, (attribute) -> new VelocityTag());

        TAG_PROCESSOR.registerTag(ElementTag.class, "version", (attribute, object) ->
                new ElementTag(proxy().getVersion().getVersion()));

        TAG_PROCESSOR.registerTag(ListTag.class, "players", (attribute, object) -> {
            ListTag list = new ListTag();
            for (var player : proxy().getAllPlayers()) {
                list.addObject(new PlayerTag(player));
            }
            return list;
        });

        TAG_PROCESSOR.registerTag(ListTag.class, "servers", (attribute, object) -> {
            ListTag list = new ListTag();
            for (var server : proxy().getAllServers()) {
                list.addObject(new ServerTag(server));
            }
            return list;
        });

        TAG_PROCESSOR.registerTag(ElementTag.class, "address", (attribute, object) -> {
            InetSocketAddress address = proxy().getBoundAddress();
            return new ElementTag(address.getHostString() + ":" + address.getPort());
        });

        TAG_PROCESSOR.registerTag(ElementTag.class, "host", (attribute, object) ->
                new ElementTag(proxy().getBoundAddress().getHostString()));

        TAG_PROCESSOR.registerTag(ElementTag.class, "port", (attribute, object) ->
                new ElementTag(proxy().getBoundAddress().getPort()));

        TAG_PROCESSOR.registerTag(ElementTag.class, "maxPlayers", (attribute, object) ->
                new ElementTag(proxy().getConfiguration().getShowMaxPlayers()));
    }

    private static ProxyServer proxy() {
        return CorexVelocity.getInstance().getServer();
    }

    @Override
    public @NonNull String getPrefix() {
        return PREFIX;
    }

    @Override
    public @NonNull String identify() {
        return PREFIX + "@";
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<VelocityTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public @NonNull String getTestValue() {
        return "velocity@";
    }
}