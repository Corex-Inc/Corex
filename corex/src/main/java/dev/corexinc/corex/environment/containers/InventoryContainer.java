package dev.corexinc.corex.environment.containers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.corexinc.corex.api.containers.AbstractContainer;
import dev.corexinc.corex.api.containers.PathType;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.compiler.ScriptCompiler;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.PlayerIdentity;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.tags.inventory.InventoryTag;
import dev.corexinc.corex.environment.tags.world.ItemTag;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InventoryContainer implements AbstractContainer {

    private static final String PROCEDURAL_PATH = "procedural items";

    private String name;
    private JsonObject rawData;
    private final Map<String, String> definitions = new LinkedHashMap<>();
    private Instruction[] proceduralScript;

    @Override
    public @NonNull String getType() {
        return "inventory";
    }

    @Override
    public void init(@NonNull String name, @NonNull JsonObject section) {
        this.name = name;
        this.rawData = section;

        JsonElement definitionsEl = section.get("definitions");
        if (definitionsEl != null && definitionsEl.isJsonObject()) {
            JsonObject definitionsObj = definitionsEl.getAsJsonObject();
            for (String key : definitionsObj.keySet()) {
                JsonElement value = definitionsObj.get(key);
                if (value != null && value.isJsonPrimitive()) definitions.put(key, value.getAsString());
            }
        }
    }

    @Override
    public @NonNull PathType resolvePath(@NonNull String path) {
        return path.equals(PROCEDURAL_PATH) ? PathType.SCRIPT : PathType.IGNORE;
    }

    @Override
    public void addCompiledScript(@NonNull String path, Instruction[] bytecode) {
        if (path.equals(PROCEDURAL_PATH)) this.proceduralScript = bytecode;
    }

    @Override
    public Instruction[] getScript(@NonNull String path) {
        return path.equals(PROCEDURAL_PATH) ? proceduralScript : null;
    }

    public InventoryTag build(@NonNull MapTag overrides, @Nullable PlayerIdentity player) {
        String typeName = stringValue("inventoryType", "GENERIC_9X3").toUpperCase();

        ScriptQueue queue = new ScriptQueue(
                ScriptQueue.uniqueId("Inventory_" + name),
                proceduralScript != null ? proceduralScript : new Instruction[0],
                false, player, true);

        for (Map.Entry<String, String> entry : definitions.entrySet()) {
            queue.define(entry.getKey(), evaluate(entry.getValue(), queue));
        }
        for (String key : overrides.keySet()) {
            queue.define(key, overrides.getObject(key));
        }

        Component title = Component.empty();
        AbstractTag titleTag = evaluate(stringValue("title", ""), queue);
        if (titleTag != null) title = titleTag.asComponent();

        boolean gui = !"false".equalsIgnoreCase(stringValue("gui", "true"));

        Map<Integer, ItemTag> contents = new LinkedHashMap<>();
        applySlots(queue, contents);
        applyProcedural(queue, contents);

        return new InventoryTag(name, typeName, title, gui, contents);
    }

    private void applySlots(ScriptQueue queue, Map<Integer, ItemTag> contents) {
        JsonElement slotsEl = rawData.get("slots");
        if (slotsEl == null || !slotsEl.isJsonArray()) return;

        JsonArray rows = slotsEl.getAsJsonArray();
        int slot = 0;
        for (JsonElement rowEl : rows) {
            if (!rowEl.isJsonPrimitive()) continue;
            for (String cell : ObjectFetcher.splitIgnoringBrackets(rowEl.getAsString().trim(), ' ')) {
                if (cell.isBlank()) continue;
                String inner = cell.startsWith("[") && cell.endsWith("]")
                        ? cell.substring(1, cell.length() - 1).trim()
                        : cell.trim();
                if (!inner.isEmpty()) {
                    AbstractTag itemTag = evaluate(inner, queue);
                    if (itemTag != null) contents.put(slot, asItem(itemTag));
                }
                slot++;
            }
        }
    }

    private void applyProcedural(ScriptQueue queue, Map<Integer, ItemTag> contents) {
        if (proceduralScript == null) return;
        queue.start();

        ListTag items = new ListTag();
        for (AbstractTag returned : queue.getReturns()) {
            if (returned instanceof ListTag listTag) {
                listTag.getList().forEach(items::addObject);
            } else {
                items.addObject(returned);
            }
        }

        int slot = 0;
        for (AbstractTag entry : items.getList()) {
            while (contents.containsKey(slot)) slot++;
            contents.put(slot++, asItem(entry));
        }
    }

    private ItemTag asItem(AbstractTag tag) {
        return tag instanceof ItemTag item ? item : new ItemTag(tag.identify());
    }

    @Nullable
    private AbstractTag evaluate(String raw, ScriptQueue queue) {
        if (raw == null) return null;
        CompiledArgument argument = ScriptCompiler.parseArg(raw);
        return argument != null ? argument.evaluate(queue) : null;
    }

    private String stringValue(String key, String fallback) {
        JsonElement element = rawData.get(key);
        return element != null && element.isJsonPrimitive() ? element.getAsString() : fallback;
    }

    @Override
    public @NonNull String getName() {
        return name;
    }

    @Override
    public @NonNull JsonObject getData() {
        return rawData;
    }

    @Override
    public @NonNull List<String> getDefinitions() {
        return List.copyOf(definitions.keySet());
    }
}
