package dev.corexinc.corex.environment.events.implementation.core;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.network.NetworkManager;
import dev.corexinc.corex.engine.network.NetworkMessageHandler;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name ProxyMessage
 *
 * @Events
 * proxy message
 *
 * @Switches
 * channel:<name> - only fires for messages sent on that channel.
 * from:<server> - only fires for messages sent by that server.
 *
 * @Description
 * Fires when another server behind the proxy sends a message with "proxy send".
 *
 * Only Corex channels reach this event, the plain words like "bossDown". A send aimed at a
 * namespaced channel such as "myplugin:data" goes to that plugin messaging channel instead and
 * never comes through here.
 *
 * The sending server is named by the proxy, not by the sender, so "from:" and <context.from> are
 * as trustworthy as the proxy itself. The channel is whatever the sender picked, so treat it as
 * a label rather than a permission.
 *
 * There is no player attached. A message arrives on the server, not on anyone in particular, so
 * put a UUID in the data if the far side needs to know who it was about.
 *
 * @Context
 * <context.channel> - returns an ElementTag of the channel the message was sent on.
 * <context.from> - returns an ElementTag of the server that sent it.
 * <context.data> - returns whatever object the sender attached.
 *
 * @Usage
 * // React to any message on the bossDown channel.
 * on proxy message channel:bossDown:
 * - narrate "<context.from> just killed <context.data.get[boss]>." targets:<server.online_players>
 *
 * @Usage
 * // Listen to one specific server only.
 * on proxy message from:lobby:
 * - narrate "Lobby says: <context.data.get[text]>"
 */
public class ProxyMessageEvent implements AbstractEvent, NetworkMessageHandler {

    private static final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NonNull String getName() {
        return "ProxyMessage";
    }

    @Override
    public @NonNull String getSyntax() {
        return "proxy message";
    }

    @Override
    public void addScript(@NonNull EventData data) {
        scripts.add(data);
    }

    @Override
    public void initListener() {
        NetworkManager.setMessageHandler(this);
    }

    @Override
    public void reset() {
        scripts.clear();
    }

    @Override
    public void onNetworkMessage(@NonNull String channel, @Nullable AbstractTag data, @NonNull String source) {
        if (scripts.isEmpty()) return;

        ContextTag context = null;

        for (EventData script : scripts) {
            String channelSwitch = script.getSwitch("channel");
            if (channelSwitch != null && !channelSwitch.equalsIgnoreCase(channel)) {
                continue;
            }

            String fromSwitch = script.getSwitch("from");
            if (fromSwitch != null && !fromSwitch.equalsIgnoreCase(source)) {
                continue;
            }

            if (context == null) {
                context = buildContext(channel, data, source);
            }

            EventRegistry.fire(script, null, context);
        }
    }

    private static ContextTag buildContext(String channel, @Nullable AbstractTag data, String source) {
        ContextTag context = new ContextTag()
                .put("channel", new ElementTag(channel))
                .put("from", new ElementTag(source));

        if (data != null) {
            context.put("data", data);
        }
        return context;
    }
}
