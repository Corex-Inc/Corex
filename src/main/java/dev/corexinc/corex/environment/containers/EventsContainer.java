package dev.corexinc.corex.environment.containers;

import dev.corexinc.corex.api.containers.AbstractContainer;
import dev.corexinc.corex.api.containers.PathType;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.environment.events.EventRegistry;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NonNull;

public class EventsContainer implements AbstractContainer {

    private String name;
    private ConfigurationSection rawData;

    @Override public @NonNull String getType() {
        return "events";
    }

    @Override
    public void init(@NonNull String name, @NonNull ConfigurationSection section) {
        this.name = name;
        this.rawData = section;
    }

    @Override
    public @NonNull PathType resolvePath(@NonNull String path) {
        if (path.startsWith("events.")) return PathType.SCRIPT;
        if (path.equalsIgnoreCase("type")) return PathType.IGNORE;
        return PathType.DATA;
    }

    @Override
    public void addCompiledScript(@NonNull String path, Instruction[] bytecode) {
        String eventLine = path.substring(7);

        EventRegistry.mapScript(eventLine, bytecode);
    }

    @Override
    public Instruction[] getScript(@NonNull String path) {
        return null;
    }

    @Override
    public org.bukkit.configuration.@NonNull ConfigurationSection getData() {
        return rawData;
    }

    @Override
    public @NonNull String getName() {
        return name;
    }
}