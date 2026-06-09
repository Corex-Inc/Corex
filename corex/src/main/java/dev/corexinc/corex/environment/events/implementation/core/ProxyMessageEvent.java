package dev.corexinc.corex.environment.events.implementation.core;

import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.shared.network.protocol.clientbound.ClientBoundMessagePacket;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProxyMessageEvent implements AbstractEvent {

    private static ProxyMessageEvent INSTANCE;

    public ProxyMessageEvent() {
        INSTANCE = this;
    }

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NonNull String getName() { return "ProxyMessageEvent"; }

    @Override
    public @NonNull String getSyntax() { return "proxy|server message"; }

    @Override
    public void addScript(@NonNull EventData data) {
        scripts.add(data);
    }

    @Override
    public void initListener() {
        isRegistered = !scripts.isEmpty();
    }

    @Override
    public void reset() {
        isRegistered = false;
        scripts.clear();
    }

    private void fireInternal(ClientBoundMessagePacket packet) {
        if (!isRegistered || scripts.isEmpty()) return;

        String channel    = packet.getChannel();
        String fromServer = packet.getSourceServer() != null
                ? packet.getSourceServer()
                : "proxy";
        Map<String, String> rawData = packet.getData();

        ContextTag context = null;

        for (EventData data : scripts) {
            String channelSwitch = data.getSwitch("channel");
            if (channelSwitch != null && !channelSwitch.equalsIgnoreCase(channel)) continue;

            String fromSwitch = data.getSwitch("from");
            if (fromSwitch != null && !fromSwitch.equalsIgnoreCase(fromServer)) continue;

            if (context == null) {
                context = buildContext(channel, fromServer, rawData);
            }

            EventRegistry.fire(data, null, context);
        }
    }

    public static void fire(ClientBoundMessagePacket packet) {
        if (INSTANCE != null) {
            INSTANCE.fireInternal(packet);
        }
    }

    private static ContextTag buildContext(String channel, String fromServer, Map<String, String> rawData) {
        MapTag dataMap = new MapTag();
        rawData.forEach((k, v) -> dataMap.putObject(k, new ElementTag(v)));

        return new ContextTag()
                .put("channel", new ElementTag(channel))
                .put("from",    new ElementTag(fromServer))
                .put("data",    dataMap);
    }
}