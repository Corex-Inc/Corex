package dev.corexinc.corex.environment.utils;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.utils.ServerLinkFormat;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.bukkit.ServerLinks;

import java.util.ArrayList;
import java.util.List;

public final class ServerLinkHelper {

    private ServerLinkHelper() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static void fill(ServerLinks links, AbstractTag value) {
        List<ServerLinkFormat.Link> parsed = ServerLinkFormat.parse(value);

        for (ServerLinks.ServerLink existing : new ArrayList<>(links.getLinks())) {
            links.removeLink(existing);
        }

        for (ServerLinkFormat.Link link : parsed) {
            if (link.builtInType() != null) {
                links.addLink(bukkitType(link.builtInType()), link.url());
            }
            else {
                links.addLink(new ElementTag(link.label()).asComponent(), link.url());
            }
        }
    }

    private static ServerLinks.Type bukkitType(String canonicalName) {
        return canonicalName.equals("BUG_REPORT")
                ? ServerLinks.Type.REPORT_BUG
                : ServerLinks.Type.valueOf(canonicalName);
    }
}
