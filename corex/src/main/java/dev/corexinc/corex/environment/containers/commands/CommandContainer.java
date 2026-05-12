package dev.corexinc.corex.environment.containers.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.corexinc.corex.api.containers.AbstractContainer;
import dev.corexinc.corex.api.containers.PathType;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.tags.core.ContextTag;
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
    private JsonObject data;

    private String description = "";
    private String usage       = "";
    private List<String> aliases       = List.of();
    private List<CommandArgumentSpec> argumentSpecs = List.of();

    private final Map<String, Instruction[]> scripts = new HashMap<>();

    @Override public @NonNull String getType() { return "command"; }
    @Override public @NonNull String getName()  { return name != null ? name : ""; }

    @Override
    public void init(@NonNull String name, @NonNull JsonObject section) {
        this.name        = name;
        this.data        = section;
        this.description = getString(section, "description", "");
        this.usage       = getString(section, "usage", "/" + name);
        this.aliases     = getStringList(section, "aliases");
        this.argumentSpecs = parseArguments(
                section.has("arguments") && section.get("arguments").isJsonObject()
                        ? section.getAsJsonObject("arguments")
                        : null
        );
    }

    @Override public @NonNull JsonObject getData() { return data; }

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
        String raw = getString(data, "definitions", "");
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

    private static @NonNull List<CommandArgumentSpec> parseArguments(@Nullable JsonObject section) {
        if (section == null) return List.of();

        List<CommandArgumentSpec> specs = new ArrayList<>();

        for (String argName : section.keySet()) {
            JsonElement argEl = section.get(argName);
            if (!argEl.isJsonObject()) continue;
            JsonObject argObj = argEl.getAsJsonObject();

            String typeName  = getString(argObj, "type", "word");
            boolean optional = getBoolean(argObj, "optional", false);

            Map<String, Object> options = new LinkedHashMap<>();
            for (String key : argObj.keySet()) {
                if (!key.equals("type") && !key.equals("optional")) {
                    options.put(key, argObj.get(key));
                }
            }

            specs.add(new CommandArgumentSpec(argName, typeName, optional, Map.copyOf(options)));
        }

        return Collections.unmodifiableList(specs);
    }

    private static String getString(JsonObject obj, String key, String defaultValue) {
        JsonElement el = obj.get(key);
        return (el != null && !el.isJsonNull()) ? el.getAsString() : defaultValue;
    }

    private static boolean getBoolean(JsonObject obj, String key, boolean defaultValue) {
        JsonElement el = obj.get(key);
        return (el != null && !el.isJsonNull()) ? el.getAsBoolean() : defaultValue;
    }

    private static List<String> getStringList(JsonObject obj, String key) {
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonArray()) return List.of();
        JsonArray arr = el.getAsJsonArray();
        List<String> result = new ArrayList<>(arr.size());
        for (JsonElement entry : arr) result.add(entry.getAsString());
        return Collections.unmodifiableList(result);
    }
}