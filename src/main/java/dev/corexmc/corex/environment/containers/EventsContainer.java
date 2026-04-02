package dev.corexmc.corex.environment.containers;

import dev.corexmc.corex.api.containers.AbstractContainer;
import dev.corexmc.corex.api.containers.PathType;
import dev.corexmc.corex.engine.compiler.Instruction;
import dev.corexmc.corex.environment.events.EventRegistry;
import org.bukkit.configuration.ConfigurationSection;

public class EventsContainer implements AbstractContainer {

    private String name;
    private ConfigurationSection rawData;

    @Override public String getType() {
        return "events";
    }

    @Override
    public void init(String name, ConfigurationSection section) {
        this.name = name;
        this.rawData = section;
    }

    @Override
    public PathType resolvePath(String path) {
        if (path.startsWith("events.")) return PathType.SCRIPT;
        if (path.equalsIgnoreCase("type")) return PathType.IGNORE;
        return PathType.DATA;
    }

    @Override
    public void addCompiledScript(String path, Instruction[] bytecode) {
        String eventLine = path.substring(7);

        EventRegistry.mapScript(eventLine, bytecode);
    }

    @Override
    public Instruction[] getScript(String path) {
        return null;
    }

    @Override
    public org.bukkit.configuration.ConfigurationSection getData() {
        return rawData;
    }

    @Override
    public String getName() {
        return name;
    }
}