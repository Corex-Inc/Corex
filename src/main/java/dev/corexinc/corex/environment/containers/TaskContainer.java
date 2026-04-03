package dev.corexinc.corex.environment.containers;

import dev.corexinc.corex.api.containers.AbstractContainer;
import dev.corexinc.corex.api.containers.PathType;
import dev.corexinc.corex.engine.compiler.Instruction;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskContainer implements AbstractContainer {

    private String name;
    private ConfigurationSection rawData;
    private final Map<String, Instruction[]> compiledScripts = new HashMap<>();
    private List<String> definitions = new ArrayList<>();

    @Override public @NonNull String getType() { return "task"; }

    @Override
    public void init(@NonNull String name, @NonNull ConfigurationSection section) {
        this.name = name;
        this.rawData = section;
        String defs = section.getString("definitions");
        if (defs != null) {
            this.definitions = List.of(defs.replace(" ", "").split("\\|"));
        }
    }

    @Override
    public @NonNull List<String> getDefinitions() {
        return definitions;
    }

    @Override public @NonNull String getName() { return name; }
    @Override public @NonNull ConfigurationSection getData() { return rawData; }

    @Override
    public @NonNull PathType resolvePath(@NonNull String path) {
        String lower = path.toLowerCase();

        if (lower.equals("type") || lower.equals("definitions")) return PathType.IGNORE;
        if (lower.startsWith("data")) return PathType.DATA;

        return PathType.SCRIPT;
    }

    @Override
    public void addCompiledScript(@NonNull String path, Instruction[] bytecode) {
        compiledScripts.put(path.toLowerCase(), bytecode);
    }

    @Override
    public Instruction[] getScript(@NonNull String path) {
        return compiledScripts.get(path.toLowerCase());
    }
}