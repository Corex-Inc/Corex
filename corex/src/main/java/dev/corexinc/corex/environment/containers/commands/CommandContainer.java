package dev.corexinc.corex.environment.containers.commands;

import dev.corexinc.corex.api.containers.AbstractContainer;
import dev.corexinc.corex.api.containers.PathType;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class CommandContainer implements AbstractContainer {

    public static final String SECTION_SCRIPT       = "script";
    public static final String SECTION_TAB_COMPLETE = "tab complete";
    public static final String SECTION_ALLOWED      = "allowed";

    private static final Set<String> KNOWN_SECTIONS = Set.of(
            SECTION_SCRIPT, SECTION_TAB_COMPLETE, SECTION_ALLOWED
    );

    private static final Set<String> METADATA_KEYS = Set.of(
            "type", "definitions", "description", "usage", "aliases", "arguments"
    );

    private String name;
    private ConfigurationSection data;

    private String description = "";
    private String usage       = "";
    private List<String> aliases       = List.of();
    private List<CommandArgumentSpec> argumentSpecs = List.of();

    private final Map<String, Instruction[]> scripts = new HashMap<>();

    @Override public @NonNull String getType() { return "command"; }
    @Override public @NonNull String getName()  { return name != null ? name : ""; }

    @Override
    public void init(@NonNull String name, @NonNull ConfigurationSection section) {
        this.name        = name;
        this.data        = section;
        this.description = section.getString("description", "");
        this.usage       = section.getString("usage", "/" + name);
        this.aliases     = section.getStringList("aliases");
        this.argumentSpecs = parseArguments(section.getConfigurationSection("arguments"));
    }

    @Override public @NonNull ConfigurationSection getData() { return data; }

    @Override
    public @NonNull PathType resolvePath(@NonNull String path) {
        if (METADATA_KEYS.contains(path.toLowerCase())) return PathType.IGNORE;
        if (KNOWN_SECTIONS.contains(path))              return PathType.SCRIPT;
        return PathType.DATA;
    }

    @Override
    public void addCompiledScript(@NonNull String path, @NonNull Instruction[] bytecode) {
        scripts.put(path, bytecode);
    }

    @Override
    public @Nullable Instruction[] getScript(@NonNull String path) {
        return scripts.get(path);
    }

    @Override
    public @NonNull List<String> getDefinitions() {
        if (data == null) return List.of();
        String raw = data.getString("definitions", "");
        return raw.isBlank() ? List.of() : List.of(raw.replace(" ", "").split("\\|"));
    }

    public @NonNull String getDescription()  { return description; }
    public @NonNull String getUsage()        { return usage; }
    public @NonNull List<String> getAliases() { return aliases; }

    public @NonNull List<String> getAllAliases() {
        List<String> all = new ArrayList<>(aliases.size() + 1);
        all.add(name);
        all.addAll(aliases);
        return all;
    }

    public @NonNull List<CommandArgumentSpec> getArgumentSpecs() { return argumentSpecs; }

    public boolean hasSection(@NonNull String section) {
        return scripts.containsKey(section);
    }

    public @Nullable ScriptQueue createQueue(@NonNull String section, @Nullable ContextTag context) {
        Instruction[] bytecode = scripts.get(section);
        if (bytecode == null) return null;

        ScriptQueue queue = new ScriptQueue(
                "MCCommand_" + name + "_" + section + "_" + System.nanoTime(),
                bytecode,
                false,
                null,
                true
        );

        if (context != null) queue.setContext(context);
        return queue;
    }

    private static @NonNull List<CommandArgumentSpec> parseArguments(@Nullable ConfigurationSection section) {
        if (section == null) return List.of();

        List<CommandArgumentSpec> specs = new ArrayList<>();

        for (String argName : section.getKeys(false)) {
            ConfigurationSection argSection = section.getConfigurationSection(argName);
            if (argSection == null) continue;

            String typeName = argSection.getString("type", "word");
            boolean optional = argSection.getBoolean("optional", false);

            Map<String, Object> options = new LinkedHashMap<>();
            for (String key : argSection.getKeys(false)) {
                if (!key.equals("type") && !key.equals("optional")) {
                    options.put(key, argSection.get(key));
                }
            }

            specs.add(new CommandArgumentSpec(argName, typeName, optional, Map.copyOf(options)));
        }

        return Collections.unmodifiableList(specs);
    }
}