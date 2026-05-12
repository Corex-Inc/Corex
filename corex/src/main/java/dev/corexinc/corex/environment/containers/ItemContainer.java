package dev.corexinc.corex.environment.containers;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.corexinc.corex.api.containers.AbstractContainer;
import dev.corexinc.corex.api.containers.PathType;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.compiler.ScriptCompiler;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.tags.world.ItemTag;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ItemContainer implements AbstractContainer {

    private String name;
    private JsonObject rawData;

    @Override public @NonNull String getType() { return "item"; }

    @Override
    public void init(@NonNull String name, @NonNull JsonObject section) {
        this.name = name;
        this.rawData = section;
        ItemCache.put(name, buildItem());
    }

    @Override
    public @NonNull PathType resolvePath(@NonNull String path) {
        return PathType.IGNORE;
    }

    private ItemTag buildItem() {
        JsonElement materialEl = rawData.get("material");
        if (materialEl == null || materialEl.isJsonNull()) {
            CorexLogger.error("Item '" + name + "' lacks a material key.");
            return new ItemTag("air");
        }

        String materialName = materialEl.getAsString();
        ItemTag item = new ItemTag(materialName);
        item.setScriptName(this.name);

        ScriptQueue dummyQueue = new ScriptQueue("ItemBuilder", new Instruction[0], false, null);

        JsonElement displayNameEl = rawData.get("display name");
        if (displayNameEl != null && !displayNameEl.isJsonNull()) {
            AbstractTag parsedName = Objects.requireNonNull(
                    ScriptCompiler.parseArg(displayNameEl.getAsString())).evaluate(dummyQueue);
            item.applyMechanism("displayName", parsedName);
        }

        JsonElement loreEl = rawData.get("lore");
        if (loreEl != null && loreEl.isJsonArray()) {
            JsonArray loreArray = loreEl.getAsJsonArray();
            if (!loreArray.isEmpty()) {
                ListTag loreTag = new ListTag();
                for (JsonElement lineEl : loreArray) {
                    AbstractTag parsedLine = Objects.requireNonNull(
                            ScriptCompiler.parseArg(lineEl.getAsString())).evaluate(dummyQueue);
                    loreTag.addObject(parsedLine);
                }
                item.applyMechanism("lore", loreTag);
            }
        }

        JsonElement mechanismsEl = rawData.get("mechanisms");
        if (mechanismsEl != null && mechanismsEl.isJsonObject()) {
            JsonObject mechanismsObj = mechanismsEl.getAsJsonObject();
            for (String key : mechanismsObj.keySet()) {
                AbstractTag valueTag = parseJsonValueToTag(mechanismsObj, key, dummyQueue);
                item.applyMechanism(key, valueTag);
            }
        }

        return item;
    }

    private AbstractTag parseJsonValueToTag(JsonObject obj, String key, ScriptQueue queue) {
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return new ElementTag("");

        if (el.isJsonArray()) {
            ListTag listTag = new ListTag();
            for (JsonElement entry : el.getAsJsonArray()) {
                AbstractTag parsed = Objects.requireNonNull(
                        ScriptCompiler.parseArg(entry.getAsString())).evaluate(queue);
                listTag.addString(parsed.identify());
            }
            return listTag;
        } else if (el.isJsonObject()) {
            JsonObject subObj = el.getAsJsonObject();
            MapTag mapTag = new MapTag();
            for (String subKey : subObj.keySet()) {
                mapTag.putObject(subKey, parseJsonValueToTag(subObj, subKey, queue));
            }
            return mapTag;
        } else {
            return Objects.requireNonNull(
                    ScriptCompiler.parseArg(el.getAsString())).evaluate(queue);
        }
    }

    @Override public @NonNull String getName() { return name; }
    @Override public @NonNull JsonObject getData() { return rawData; }
    @Override public void addCompiledScript(@NonNull String path, Instruction[] bytecode) {}
    @Override public Instruction[] getScript(@NonNull String path) { return null; }

    public static class ItemCache {
        private static final Map<String, ItemTag> cache = new HashMap<>();
        public static void put(String name, ItemTag item) { cache.put(name.toLowerCase(), item); }
        public static ItemTag get(String name) {
            ItemTag tag = cache.get(name.toLowerCase());
            return tag != null ? new ItemTag(tag.getItemStack().clone()) : null;
        }
        public static void clear() { cache.clear(); }
    }
}