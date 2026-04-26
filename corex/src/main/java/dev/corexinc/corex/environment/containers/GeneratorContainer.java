package dev.corexinc.corex.environment.containers;

import dev.corexinc.corex.api.containers.AbstractContainer;
import dev.corexinc.corex.api.containers.PathType;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GeneratorContainer implements AbstractContainer {

    public static final String SECTION_BASE_HEIGHT    = "base_height";
    public static final String SECTION_NOISE          = "noise";
    public static final String SECTION_SURFACE        = "surface";
    public static final String SECTION_BEDROCK        = "bedrock";
    public static final String SECTION_CAVES          = "caves";
    public static final String SECTION_BIOME          = "biome";
    public static final String SECTION_POPULATORS     = "populators";
    public static final String SECTION_CAN_SPAWN      = "can_spawn";
    public static final String SECTION_SPAWN_LOCATION = "spawn_location";

    static final Set<String> KNOWN_SECTIONS = Set.of(
            SECTION_BASE_HEIGHT,
            SECTION_NOISE,
            SECTION_SURFACE,
            SECTION_BEDROCK,
            SECTION_CAVES,
            SECTION_BIOME,
            SECTION_POPULATORS,
            SECTION_CAN_SPAWN,
            SECTION_SPAWN_LOCATION
    );

    private String name;
    private ConfigurationSection data;

    private final Map<String, Instruction[]> scripts = new HashMap<>();

    private final Set<String> vanillaFirst = new HashSet<>();

    @Override
    public @NotNull String getType() {
        return "generator";
    }

    @Override
    public @NotNull String getName() {
        return name != null ? name : "";
    }

    @Override
    public void init(@NotNull String name, @NotNull ConfigurationSection section) {
        this.name = name;
        this.data = section;
        parseVanillaFirst(section);
    }

    @Override
    public @NotNull ConfigurationSection getData() {
        return data;
    }

    @Override
    public @NotNull PathType resolvePath(@NotNull String path) {
        if (path.equals("type") || path.equals("definitions") || path.equals("vanilla_first")) {
            return PathType.IGNORE;
        }
        if (KNOWN_SECTIONS.contains(path)) {
            return PathType.SCRIPT;
        }
        return PathType.DATA;
    }

    @Override
    public void addCompiledScript(@NotNull String path, @NotNull Instruction[] bytecode) {
        scripts.put(path.toLowerCase(), bytecode);
    }

    @Override
    public @Nullable Instruction[] getScript(@NotNull String path) {
        return scripts.get(path.toLowerCase());
    }

    @Override
    public @NotNull List<String> getDefinitions() {
        if (data == null) return List.of();
        String raw = data.getString("definitions", "");
        if (raw.isBlank()) return List.of();
        return List.of(raw.replace(" ", "").split("\\|"));
    }

    public boolean hasSection(@NotNull String section) {
        return scripts.containsKey(section.toLowerCase());
    }

    public boolean isVanillaFirst(@NotNull String section) {
        return vanillaFirst.contains(section.toLowerCase());
    }

    @Nullable
    public ScriptQueue createQueue(@NotNull String section, @Nullable ContextTag context) {
        Instruction[] bytecode = scripts.get(section.toLowerCase());
        if (bytecode == null) return null;

        ScriptQueue queue = new ScriptQueue(
                "Generator_" + name + "_" + section + "_" + System.nanoTime(),
                bytecode,
                false,
                null
        );

        if (context != null) {
            queue.setContext(context);
        }

        return queue;
    }

    @Nullable
    public ScriptQueue runSection(@NotNull String section, @Nullable ContextTag context) {
        ScriptQueue queue = createQueue(section, context);
        if (queue == null) return null;
        queue.start();
        return queue;
    }


    private void parseVanillaFirst(@NotNull ConfigurationSection section) {
        String raw = section.getString("vanilla_first", "");
        if (raw.isBlank()) return;

        for (String entry : raw.replace(" ", "").split("\\|")) {
            String key = entry.strip().toLowerCase();
            if (!key.isEmpty() && KNOWN_SECTIONS.contains(key)) {
                vanillaFirst.add(key);
            }
        }
    }
}