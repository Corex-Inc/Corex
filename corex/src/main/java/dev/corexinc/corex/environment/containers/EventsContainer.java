package dev.corexinc.corex.environment.containers;

import com.google.gson.JsonObject;
import dev.corexinc.corex.api.containers.AbstractContainer;
import dev.corexinc.corex.api.containers.PathType;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.environment.events.EventRegistry;
import org.jspecify.annotations.NonNull;

public class EventsContainer implements AbstractContainer {

    private String name;
    private JsonObject rawData;

    @Override public @NonNull String getType() { return "events"; }

    @Override
    public void init(@NonNull String name, @NonNull JsonObject section) {
        this.name = name;
        this.rawData = section;
    }

    @Override
    public @NonNull PathType resolvePath(@NonNull String path) {
        if (path.equalsIgnoreCase("type")) return PathType.IGNORE;
        if (path.equalsIgnoreCase("events")) return PathType.IGNORE;
        if (path.startsWith("events.") && !path.substring(7).contains(".")) return PathType.SCRIPT;
        return PathType.IGNORE;
    }

    @Override
    public void addCompiledScript(@NonNull String path, Instruction[] bytecode) {
        EventRegistry.mapScript(path.substring(7), bytecode);
    }

    @Override public Instruction[] getScript(@NonNull String path) { return null; }
    @Override public @NonNull JsonObject getData() { return rawData; }
    @Override public @NonNull String getName() { return name; }
}