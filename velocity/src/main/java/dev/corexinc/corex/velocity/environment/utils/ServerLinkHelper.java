package dev.corexinc.corex.velocity.environment.utils;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.util.ServerLink;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.utils.ServerLinkFormat;
import dev.corexinc.corex.environment.tags.core.ElementTag;

import java.util.ArrayList;
import java.util.List;

public final class ServerLinkHelper {

    private ServerLinkHelper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static List<ServerLink> parse(AbstractTag value) {
        List<ServerLink> links = new ArrayList<>();

        for (ServerLinkFormat.Link link : ServerLinkFormat.parse(value)) {
            if (link.builtInType() != null) {
                links.add(ServerLink.serverLink(ServerLink.Type.valueOf(link.builtInType()), link.url().toString()));
            }
            else {
                links.add(ServerLink.serverLink(new ElementTag(link.label()).asComponent(), link.url().toString()));
            }
        }

        return links;
    }

    public static boolean isSupported(Player player) {
        return player.getProtocolVersion().getProtocol() >= ProtocolVersion.MINECRAFT_1_21.getProtocol();
    }
}
