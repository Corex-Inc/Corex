package dev.corexmc.corex.environment.events;

import org.bukkit.event.Listener;

public interface AbstractEvent extends Listener {

    String getName();

    String getSyntax();

    void addScript(EventData data);

    void initListener();
    void reset();
}