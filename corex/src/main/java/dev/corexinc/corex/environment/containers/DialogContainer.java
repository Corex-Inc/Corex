package dev.corexinc.corex.environment.containers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.corexinc.corex.api.containers.AbstractContainer;
import dev.corexinc.corex.api.containers.PathType;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.compiler.ScriptCompiler;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.scripts.ScriptManager;
import dev.corexinc.corex.engine.utils.PlayerIdentity;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.tags.world.ItemTag;
import dev.corexinc.corex.environment.utils.dialog.DialogSpec;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.*;

public class DialogContainer implements AbstractContainer {

    private String name;
    private JsonObject rawData;
    private List<String> definitions = new ArrayList<>();
    private final Map<String, Instruction[]> buttonScripts = new HashMap<>();

    @Override
    public @NonNull String getType() {
        return "dialog";
    }

    @Override
    public void init(@NonNull String name, @NonNull JsonObject section) {
        this.name = name;
        this.rawData = section;

        JsonElement definitionsEl = section.get("definitions");
        if (definitionsEl != null && definitionsEl.isJsonPrimitive()) {
            this.definitions = List.of(definitionsEl.getAsString().replace(" ", "").split("\\|"));
        }

        JsonObject buttons = childObject(section, "buttons");
        if (buttons != null) {
            for (String key : buttons.keySet()) {
                if (!buttons.get(key).isJsonObject()) continue;
                JsonElement scriptEl = buttons.get(key).getAsJsonObject().get("script");
                if (scriptEl != null && scriptEl.isJsonArray() && jsonToRaw(scriptEl) instanceof List<?> rawList) {
                    buttonScripts.put(key, ScriptManager.compileBlock(rawList));
                }
            }
        }
    }

    @Override
    public @NonNull PathType resolvePath(@NonNull String path) {
        return PathType.IGNORE;
    }

    @Override
    public void addCompiledScript(@NonNull String path, Instruction[] bytecode) {}

    @Override
    public Instruction[] getScript(@NonNull String path) {
        return null;
    }

    @Nullable
    public Instruction[] getButtonScript(String id) {
        return buttonScripts.get(id);
    }

    @Override
    public @NonNull List<String> getDefinitions() {
        return definitions;
    }

    public DialogSpec build(@NonNull MapTag overrides, @Nullable PlayerIdentity player) {
        return build(overrides, player, new HashSet<>());
    }

    private DialogSpec build(MapTag overrides, PlayerIdentity player, Set<String> visited) {
        visited.add(name.toLowerCase());

        ScriptQueue queue = new ScriptQueue(ScriptQueue.uniqueId("Dialog_" + name), new Instruction[0], false, player, true);
        for (String key : overrides.keySet()) {
            queue.define(key, overrides.getObject(key));
        }

        JsonObject base = childObject(rawData, "base");

        DialogSpec spec = new DialogSpec();
        spec.name = name;
        spec.type = baseString(base, "type", "notice").toLowerCase();
        spec.title = component(baseString(base, "title", ""), queue);
        spec.canCloseWithEscape = !"false".equalsIgnoreCase(baseString(base, "canCloseWithEscape", "true"));
        spec.afterAction = baseString(base, "afterAction", "close").toLowerCase();
        spec.columns = (int) number(baseString(base, "columns", "2"), 2);
        String externalTitle = baseString(base, "externalTitle", "");
        if (!externalTitle.isEmpty()) spec.externalTitle = component(externalTitle, queue);

        parseBodies(spec, queue);
        parseInputs(spec, queue);
        parseButtons(spec, queue);
        parseChildren(spec, queue, player, visited);

        return spec;
    }

    private void parseBodies(DialogSpec spec, ScriptQueue queue) {
        JsonObject bodies = childObject(rawData, "bodies");
        if (bodies == null) return;

        for (String key : bodies.keySet()) {
            if (!bodies.get(key).isJsonObject()) continue;
            JsonObject obj = bodies.get(key).getAsJsonObject();
            DialogSpec.Body body = new DialogSpec.Body();

            String type = string(obj, "type", obj.has("item") ? "item" : "message").toLowerCase();
            if (type.equals("item")) {
                body.type = "item";
                AbstractTag itemTag = evaluate(string(obj, "item", "air"), queue);
                body.item = itemTag instanceof ItemTag item ? item.getItemStack() : new ItemTag(itemTag.identify()).getItemStack();
                if (obj.has("description")) body.description = component(obj.get("description").getAsString(), queue);
            } else {
                body.type = "message";
                body.text = component(string(obj, "message", ""), queue);
                body.width = (int) number(string(obj, "width", "200"), 200);
            }
            spec.bodies.add(body);
        }
    }

    private void parseInputs(DialogSpec spec, ScriptQueue queue) {
        JsonObject inputs = childObject(rawData, "inputs");
        if (inputs == null) return;

        for (String mapKey : inputs.keySet()) {
            if (!inputs.get(mapKey).isJsonObject()) continue;
            JsonObject obj = inputs.get(mapKey).getAsJsonObject();
            DialogSpec.Input input = new DialogSpec.Input();

            input.type = string(obj, "type", "text").toLowerCase();
            input.key = string(obj, "key", mapKey);
            input.label = component(string(obj, "label", input.key), queue);

            switch (input.type) {
                case "bool", "boolean" -> {
                    input.type = "bool";
                    input.initialBool = Boolean.parseBoolean(string(obj, "initial", "false"));
                    input.onTrue = resolve(string(obj, "onTrue", "Yes"), queue);
                    input.onFalse = resolve(string(obj, "onFalse", "No"), queue);
                }
                case "number", "range" -> {
                    input.type = "number";
                    input.min = number(string(obj, "start", string(obj, "min", "0")), 0);
                    input.max = number(string(obj, "end", string(obj, "max", "100")), 100);
                    input.step = number(string(obj, "step", "1"), 1);
                    if (obj.has("initial")) input.initialNumber = number(obj.get("initial").getAsString(), input.min);
                }
                case "single_option", "single", "option" -> {
                    input.type = "single_option";
                    JsonObject options = childObject(obj, "options");
                    if (options != null) {
                        for (String optionId : options.keySet()) {
                            DialogSpec.Option option = new DialogSpec.Option();
                            option.id = optionId;
                            option.display = component(options.get(optionId).getAsString(), queue);
                            input.options.add(option);
                        }
                        if (!input.options.isEmpty()) input.options.getFirst().initial = true;
                    }
                }
                default -> {
                    input.type = "text";
                    input.maxLength = (int) number(string(obj, "maxLength", "32"), 32);
                    input.multiline = Boolean.parseBoolean(string(obj, "multiline", "false"));
                    input.initial = resolve(string(obj, "initial", ""), queue);
                }
            }
            spec.inputs.add(input);
        }
    }

    private void parseButtons(DialogSpec spec, ScriptQueue queue) {
        JsonObject buttons = childObject(rawData, "buttons");
        if (buttons == null) return;

        for (String mapKey : buttons.keySet()) {
            if (!buttons.get(mapKey).isJsonObject()) continue;
            JsonObject obj = buttons.get(mapKey).getAsJsonObject();
            DialogSpec.Button button = new DialogSpec.Button();

            button.id = mapKey;
            button.label = component(string(obj, "label", "OK"), queue);
            if (obj.has("tooltip")) button.tooltip = component(obj.get("tooltip").getAsString(), queue);
            button.width = (int) number(string(obj, "width", "150"), 150);

            String type = string(obj, "type", "").toUpperCase();
            if (buttonScripts.containsKey(mapKey) || type.equals("SCRIPT")) {
                button.script = buttonScripts.containsKey(mapKey);
                button.action = "script";
            } else if (type.equals("OPEN_URL") || obj.has("url")) {
                button.action = "url";
                button.value = resolve(string(obj, "url", ""), queue);
            } else if (type.equals("RUN_COMMAND") || obj.has("command")) {
                button.action = "command";
                button.value = resolve(string(obj, "command", ""), queue);
            } else if (type.equals("COPY_TO_CLIPBOARD") || obj.has("text")) {
                button.action = "clipboard";
                button.value = resolve(string(obj, "text", ""), queue);
            } else {
                button.action = "close";
            }
            spec.buttons.add(button);
        }
    }

    private void parseChildren(DialogSpec spec, ScriptQueue queue, PlayerIdentity player, Set<String> visited) {
        if (!spec.type.equals("list")) return;
        JsonElement childrenEl = rawData.get("children");
        if (childrenEl == null || !childrenEl.isJsonArray()) return;

        for (JsonElement element : childrenEl.getAsJsonArray()) {
            String childName = resolve(element.getAsString(), queue);
            if (visited.contains(childName.toLowerCase())) continue;
            if (ScriptManager.getContainer(childName) instanceof DialogContainer child) {
                spec.children.add(child.build(new MapTag(), player, visited));
            }
        }
    }

    @Override
    public @NonNull String getName() {
        return name;
    }

    @Override
    public @NonNull JsonObject getData() {
        return rawData;
    }

    @Nullable
    private JsonObject childObject(JsonObject parent, String key) {
        JsonElement element = parent.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }

    @Nullable
    private AbstractTag evaluate(String raw, ScriptQueue queue) {
        if (raw == null) return null;
        CompiledArgument argument = ScriptCompiler.parseArg(raw);
        return argument != null ? argument.evaluate(queue) : null;
    }

    private String resolve(String raw, ScriptQueue queue) {
        if (raw == null || raw.isEmpty()) return "";
        AbstractTag result = evaluate(raw, queue);
        return result != null ? result.identify() : raw;
    }

    private Component component(String raw, ScriptQueue queue) {
        if (raw == null || raw.isEmpty()) return Component.empty();
        AbstractTag result = evaluate(raw, queue);
        return result != null ? result.asComponent() : Component.text(raw);
    }

    private String baseString(@Nullable JsonObject base, String key, String fallback) {
        if (base != null && base.get(key) != null && base.get(key).isJsonPrimitive()) return base.get(key).getAsString();
        return string(rawData, key, fallback);
    }

    private String string(JsonObject obj, String key, String fallback) {
        JsonElement element = obj.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }

    private float number(String raw, float fallback) {
        try {
            return Float.parseFloat(raw.trim());
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private Object jsonToRaw(JsonElement element) {
        if (element.isJsonArray()) {
            List<Object> list = new ArrayList<>();
            for (JsonElement child : element.getAsJsonArray()) list.add(jsonToRaw(child));
            return list;
        }
        if (element.isJsonObject()) {
            Map<String, Object> map = new LinkedHashMap<>();
            JsonObject obj = element.getAsJsonObject();
            for (String key : obj.keySet()) map.put(key, jsonToRaw(obj.get(key)));
            return map;
        }
        if (element.isJsonNull()) return null;
        return element.getAsJsonPrimitive().getAsString();
    }
}