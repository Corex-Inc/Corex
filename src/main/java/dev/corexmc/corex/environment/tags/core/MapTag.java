package dev.corexmc.corex.environment.tags.core;

import dev.corexmc.corex.api.processors.BaseTagProcessor;
import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.api.tags.Attribute;
import dev.corexmc.corex.api.processors.TagProcessor;
import dev.corexmc.corex.engine.tags.ObjectFetcher;
import org.jspecify.annotations.NonNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapTag implements AbstractTag {

    private static String prefix = "map";
    private final Map<String, String> map = new LinkedHashMap<>();

    public static final TagProcessor<MapTag> PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag(prefix, attr -> new MapTag(attr.getParam()));

        ObjectFetcher.registerFetcher(prefix, MapTag::new);

        PROCESSOR.registerTag(ElementTag.class, "size", (attr, obj) -> new ElementTag(obj.map.size()));

        PROCESSOR.registerTag(ListTag.class, "keys", (attr, obj) -> new ListTag(String.join("|", obj.map.keySet())));

        PROCESSOR.registerTag(AbstractTag.class, "get", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            String val = obj.map.get(attr.getParam());
            return val != null ? ObjectFetcher.pickObject(val) : null;
        }).test("a");

    }

    public MapTag(String raw) {
        if (raw == null || raw.isEmpty()) return;

        if (raw.startsWith("[") && raw.endsWith("]")) {
            raw = raw.substring(1, raw.length() - 1);
        }

        List<String> pairs = ObjectFetcher.splitIgnoringBrackets(raw, ';');

        for (String pair : pairs) {
            int eqIndex = pair.indexOf('=');
            if (eqIndex > 0) {
                map.put(pair.substring(0, eqIndex), pair.substring(eqIndex + 1));
            }
        }
    }

    public java.util.Set<String> keySet() {
        return map.keySet();
    }

    public AbstractTag getObject(String key) {
        String val = map.get(key);
        return val != null ? ObjectFetcher.pickObject(val) : null;
    }


    @Override public @NonNull String getPrefix() { return prefix; }

    @Override
    public @NonNull String identify() {
        List<String> pairs = new java.util.ArrayList<>();
        map.forEach((k, v) -> pairs.add(k + "=" + v));
        return prefix + "@[" + String.join(";", pairs) + "]";
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) { return PROCESSOR.process(this, attribute); }

    @Override
    public TagProcessor<MapTag> getProcessor() {
        return PROCESSOR;
    }

    @Override
    public String getTestValue() {
        return "map@[a=1;b=2]";
    }
}