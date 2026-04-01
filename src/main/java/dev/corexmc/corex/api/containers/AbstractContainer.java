package dev.corexmc.corex.api.containers;

import dev.corexmc.corex.engine.compiler.Instruction;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Collections;
import java.util.List;

public interface AbstractContainer {

    String getType();

    String getName();

    void init(String name, ConfigurationSection section);

    ConfigurationSection getData();

    PathType resolvePath(String path);

    void addCompiledScript(String path, Instruction[] bytecode);

    Instruction[] getScript(String path);

    default List<String> getDefinitions() {
        return Collections.emptyList();
    }
}