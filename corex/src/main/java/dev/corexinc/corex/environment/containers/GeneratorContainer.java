package dev.corexinc.corex.environment.containers;

import com.google.gson.JsonObject;
import dev.corexinc.corex.api.containers.AbstractContainer;
import dev.corexinc.corex.api.containers.PathType;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GeneratorContainer implements AbstractContainer {

    public static final String SECTION_BASE_HEIGHT    = "baseHeight";
    public static final String SECTION_NOISE          = "noise";
    public static final String SECTION_SURFACE        = "surface";
    public static final String SECTION_BEDROCK        = "bedrock";
    public static final String SECTION_CAVES          = "caves";
    public static final String SECTION_BIOME          = "biome";
    public static final String SECTION_POPULATORS     = "populators";
    public static final String SECTION_CAN_SPAWN      = "canSpawn";
    public static final String SECTION_SPAWN_LOCATION = "spawnLocation";

    static final Set<String> KNOWN_SECTIONS = Set.of(
            SECTION_BASE_HEIGHT, SECTION_NOISE, SECTION_SURFACE, SECTION_BEDROCK,
            SECTION_CAVES, SECTION_BIOME, SECTION_POPULATORS, SECTION_CAN_SPAWN, SECTION_SPAWN_LOCATION
    );

    private String name;
    private JsonObject data;
    private final Map<String, Instruction[]> scripts = new HashMap<>();
    private final Set<String> vanillaFirst = new HashSet<>();

    @Override public @NotNull String getType() { return "generator"; }
    @Override public @NotNull String getName() { return name != null ? name : ""; }

    @Override
    public void init(@NotNull String name, @NotNull JsonObject section) {
        this.name = name;
        this.data = section;
        parseVanillaFirst(section);
    }

    @Override public @NotNull JsonObject getData() { return data; }

    @Override
    public @NotNull PathType resolvePath(@NotNull String path) {
        if (path.equals("type") || path.equals("definitions") || path.equals("vanillaFirst")) return PathType.IGNORE;
        if (KNOWN_SECTIONS.contains(path)) return PathType.SCRIPT;
        return PathType.DATA;
    }

    @Override
    public void addCompiledScript(@NotNull String path, @NotNull Instruction[] bytecode) {
        scripts.put(path, bytecode);
    }

    @Override
    public @Nullable Instruction[] getScript(@NotNull String path) {
        return scripts.get(path);
    }

    @Override
    public @NotNull List<String> getDefinitions() {
        if (data == null) return List.of();
        var el = data.get("definitions");
        if (el == null || !el.isJsonPrimitive()) return List.of();
        String raw = el.getAsString().replace(" ", "");
        return raw.isBlank() ? List.of() : List.of(raw.split("\\|"));
    }

    public boolean hasSection(@NotNull String section) { return scripts.containsKey(section); }
    public boolean isVanillaFirst(@NotNull String section) { return vanillaFirst.contains(section); }

    @Nullable
    public ScriptQueue createQueue(@NotNull String section, @Nullable ContextTag context) {
        return createQueue(section, context, new MapTag());
    }

    @Nullable
    public ScriptQueue createQueue(@NotNull String section, @Nullable ContextTag context, @NotNull MapTag instanceDefs) {
        Instruction[] bytecode = scripts.get(section);
        if (bytecode == null) return null;
        ScriptQueue queue = new ScriptQueue("Generator_" + name + "_" + section + "_" + System.nanoTime(), bytecode, false, null, true);
        if (context != null) queue.setContext(context);
        instanceDefs.keySet().forEach(k -> queue.define(k, instanceDefs.getObject(k)));
        return queue;
    }

    @Nullable
    public ScriptQueue runSection(@NotNull String section, @Nullable ContextTag context) {
        return runSection(section, context, new MapTag());
    }

    @Nullable
    public ScriptQueue runSection(@NotNull String section, @Nullable ContextTag context, @NotNull MapTag instanceDefs) {
        ScriptQueue queue = createQueue(section, context, instanceDefs);
        if (queue == null) return null;
        queue.start();
        return queue;
    }

    private void parseVanillaFirst(@NotNull JsonObject section) {
        var el = section.get("vanillaFirst");
        if (el == null || !el.isJsonPrimitive()) return;
        String raw = el.getAsString().replace(" ", "");
        if (raw.isBlank()) return;
        for (String entry : raw.split("\\|")) {
            String key = entry.strip();
            if (!key.isEmpty() && KNOWN_SECTIONS.contains(key)) vanillaFirst.add(key);
        }
    }
}