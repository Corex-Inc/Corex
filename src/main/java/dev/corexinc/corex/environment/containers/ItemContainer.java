package dev.corexinc.corex.environment.containers;

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
import org.bukkit.configuration.ConfigurationSection;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ItemContainer implements AbstractContainer {

    private String name;
    private ConfigurationSection rawData;

    @Override public @NonNull String getType() { return "item"; }

    @Override
    public void init(@NonNull String name, @NonNull ConfigurationSection section) {
        this.name = name;
        this.rawData = section;
        ItemCache.put(name, buildItem());
    }

    @Override
    public @NonNull PathType resolvePath(@NonNull String path) {
        return PathType.IGNORE;
    }

    private ItemTag buildItem() {
        String materialName = rawData.getString("material");
        if (materialName == null) {
            CorexLogger.error("Item '" + name + "' lacks a material key.");
            return new ItemTag("air");
        }

        ItemTag item = new ItemTag(materialName);
        item.setScriptName(this.name);

        ScriptQueue dummyQueue = new ScriptQueue("ItemBuilder", new Instruction[0], false, null);

        String displayName = rawData.getString("display name");
        if (displayName != null) {
            AbstractTag parsedName = Objects.requireNonNull(ScriptCompiler.parseArg(displayName)).evaluate(dummyQueue);
            item.applyMechanism("displayName", parsedName);
        }

        List<String> loreList = rawData.getStringList("lore");
        if (!loreList.isEmpty()) {
            ListTag loreTag = new ListTag("");
            for (String line : loreList) {
                AbstractTag parsedLine = Objects.requireNonNull(ScriptCompiler.parseArg(line)).evaluate(dummyQueue);
                loreTag.addObject(parsedLine);
            }
            item.applyMechanism("lore", loreTag);
        }

        ConfigurationSection mechanismsData = rawData.getConfigurationSection("mechanisms");
        if (mechanismsData != null) {
            for (String key : mechanismsData.getKeys(false)) {
                AbstractTag valueTag = parseYamlValueToTag(mechanismsData, key, dummyQueue);
                item.applyMechanism(key, valueTag);
            }
        }

        return item;
    }

    private AbstractTag parseYamlValueToTag(ConfigurationSection section, String key, ScriptQueue queue) {
        if (section.isList(key)) {
            ListTag listTag = new ListTag("");
            for (String listVal : section.getStringList(key)) {
                AbstractTag parsed = Objects.requireNonNull(ScriptCompiler.parseArg(listVal)).evaluate(queue);
                listTag.addString(parsed.identify());
            }
            return listTag;
        } else if (section.isConfigurationSection(key)) {
            ConfigurationSection subSection = section.getConfigurationSection(key);
            MapTag mapTag = new MapTag("");
            if (subSection != null) {
                for (String subKey : subSection.getKeys(false)) {
                    mapTag.putObject(subKey, parseYamlValueToTag(subSection, subKey, queue));
                }
            }
            return mapTag;
        } else {
            String rawStr = section.getString(key);
            if (rawStr == null) return new ElementTag("");
            return Objects.requireNonNull(ScriptCompiler.parseArg(rawStr)).evaluate(queue);
        }
    }

    @Override public @NonNull String getName() { return name; }
    @Override public @NonNull ConfigurationSection getData() { return rawData; }
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