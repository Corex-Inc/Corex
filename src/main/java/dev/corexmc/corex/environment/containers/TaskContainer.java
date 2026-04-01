package dev.corexmc.corex.environment.containers;

import dev.corexmc.corex.api.containers.AbstractContainer;
import dev.corexmc.corex.api.containers.PathType;
import dev.corexmc.corex.engine.compiler.Instruction;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TaskContainer implements AbstractContainer {

    private String name;
    private ConfigurationSection rawData;
    private final Map<String, Instruction[]> compiledScripts = new HashMap<>();
    private List<String> definitions = new ArrayList<>();

    @Override public String getType() { return "task"; }

    @Override
    public void init(String name, ConfigurationSection section) {
        this.name = name;
        this.rawData = section;
        String defs = section.getString("definitions");
        if (defs != null) {
            this.definitions = List.of(defs.replace(" ", "").split("\\|"));
        }
    }

    @Override
    public java.util.List<String> getDefinitions() {
        return definitions;
    }

    @Override public String getName() { return name; }
    @Override public ConfigurationSection getData() { return rawData; }

    @Override
    public PathType resolvePath(String path) {
        String lower = path.toLowerCase();

        if (lower.equals("type") || lower.equals("definitions")) return PathType.IGNORE;
        if (lower.startsWith("data") || lower.startsWith("my_data")) return PathType.DATA;

        // Если это не data, значит это список команд! Компилируем!
        return PathType.SCRIPT;
    }

    @Override
    public void addCompiledScript(String path, Instruction[] bytecode) {
        compiledScripts.put(path.toLowerCase(), bytecode);
    }

    @Override
    public Instruction[] getScript(String path) {
        return compiledScripts.get(path.toLowerCase());
    }
}