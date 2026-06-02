package dev.corexinc.corex.environment.containers.commands;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.corexinc.corex.api.containers.AbstractContainer;
import dev.corexinc.corex.api.containers.PathType;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.compiler.ScriptCompiler;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.PlayerIdentity;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CommandContainer implements AbstractContainer {

    private static final Set<String> RESERVED = Set.of(
            "type", "then", "script", "suggests", "requires",
            "aliases", "redirect"
    );

    private String name;
    private JsonObject data;

    private String description = "";
    private List<String> aliases = List.of();
    private boolean override = false;
    private CommandNode root;

    private final Map<String, Instruction[]> scripts = new HashMap<>();

    @Override public @NonNull String getType() { return "command"; }
    @Override public @NonNull String getName() { return name != null ? name : ""; }

    @Override
    public void init(@NonNull String name, @NonNull JsonObject section) {
        this.name        = name;
        this.data        = section;
        this.description = getString(section, "description", "");
        this.aliases     = parseTagList(section, "aliases");
        this.override    = getBoolean(section, "override", false);

        JsonObject tree = section.has("tree") && section.get("tree").isJsonObject()
                ? section.getAsJsonObject("tree")
                : new JsonObject();
        this.root = parseRoot(tree);
    }

    @Override public @NonNull JsonObject getData() { return data; }

    @Override
    public @NonNull PathType resolvePath(@NonNull String path) {
        if (path.endsWith(".script") || path.endsWith(".suggests") || path.endsWith(".requires")) {
            return PathType.SCRIPT;
        }
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

    public @NonNull String getDescription() { return description; }
    public @NonNull List<String> getAliases() { return aliases; }
    public boolean isOverride() { return override; }
    public @NonNull CommandNode getTree() { return root; }

    public @NonNull List<String> getAllAliases() {
        List<String> all = new ArrayList<>(aliases.size() + 1);
        all.add(name);
        all.addAll(aliases);
        return all;
    }

    public @Nullable ScriptQueue createQueue(@NonNull String section, @Nullable PlayerIdentity player, @Nullable ContextTag context) {
        Instruction[] bytecode = scripts.get(section);
        if (bytecode == null) return null;

        ScriptQueue queue = new ScriptQueue(
                "MCCommand_" + name + "_" + section + "_" + System.nanoTime(),
                bytecode,
                false,
                player,
                true
        );

        if (context != null) queue.setContext(context);
        return queue;
    }

    private @NonNull CommandNode parseRoot(@NonNull JsonObject tree) {
        CommandNode node = new CommandNode(
                name, false, null, List.of(),
                "tree", "", null,
                tree.has("script"), false, tree.has("requires"));
        parseChildren(node, tree, "tree", "", List.of());
        return node;
    }

    private void parseChildren(@NonNull CommandNode parent,
                               @NonNull JsonObject parentObj,
                               @NonNull String parentBase,
                               @NonNull String parentArg,
                               @NonNull List<CommandNode> parentChain) {
        if (!parentObj.has("then") || !parentObj.get("then").isJsonObject()) return;

        JsonObject then = parentObj.getAsJsonObject("then");
        for (String key : then.keySet()) {
            JsonElement el = then.get(key);
            if (!el.isJsonObject()) continue;
            JsonObject obj = el.getAsJsonObject();

            String basePath = parentBase + ".then." + key;
            String argPath  = parentArg.isEmpty() ? key : parentArg + "." + key;
            boolean argument = obj.has("type");

            CommandArgumentSpec spec = argument
                    ? new CommandArgumentSpec(key, getString(obj, "type", "word"), parseOptions(obj))
                    : null;

            CommandNode node = new CommandNode(
                    key,
                    argument,
                    spec,
                    argument ? List.of() : parseTagList(obj, "aliases"),
                    basePath,
                    argPath,
                    parentChain,
                    obj.has("script"),
                    obj.has("suggests"),
                    obj.has("requires"));

            parent.addChild(node);
            parseChildren(node, obj, basePath, argPath, node.pathChain());
        }
    }

    private static @NonNull Map<String, Object> parseOptions(@NonNull JsonObject obj) {
        Map<String, Object> options = new LinkedHashMap<>();
        for (String key : obj.keySet()) {
            if (RESERVED.contains(key)) continue;
            JsonElement el = obj.get(key);
            if (!el.isJsonPrimitive()) continue;
            JsonPrimitive prim = el.getAsJsonPrimitive();
            if (prim.isNumber())       options.put(key, prim.getAsNumber());
            else if (prim.isBoolean()) options.put(key, prim.getAsBoolean());
            else                       options.put(key, prim.getAsString());
        }
        return Map.copyOf(options);
    }

    private static String getString(JsonObject obj, String key, String defaultValue) {
        JsonElement el = obj.get(key);
        return (el != null && !el.isJsonNull()) ? el.getAsString() : defaultValue;
    }

    private static boolean getBoolean(JsonObject obj, String key, boolean defaultValue) {
        JsonElement el = obj.get(key);
        return (el != null && !el.isJsonNull()) ? el.getAsBoolean() : defaultValue;
    }

    private static List<String> parseTagList(@NonNull JsonObject obj, @NonNull String key) {
        JsonElement el = obj.get(key);
        if (el == null || !el.isJsonPrimitive()) return List.of();

        String raw = el.getAsString();
        if (raw.isBlank()) return List.of();

        List<AbstractTag> items = toListTag(raw).getList();
        List<String> result = new ArrayList<>(items.size());
        for (AbstractTag item : items) result.add(item.identify());
        return Collections.unmodifiableList(result);
    }

    private static @NonNull ListTag toListTag(@NonNull String raw) {
        if (raw.indexOf('<') >= 0) {
            try {
                AbstractTag resolved = ScriptCompiler.parseArg(raw).evaluate(null);
                if (resolved instanceof ListTag list) return list;
                if (resolved != null) return new ListTag(resolved.identify());
            } catch (Exception ignored) {}
        }
        AbstractTag tag = ObjectFetcher.pickObject(raw);
        return tag instanceof ListTag list ? list : new ListTag(raw);
    }
}
