package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MapTag implements AbstractTag {

    private static final String prefix = "map";
    private final Map<String, String> map = new LinkedHashMap<>();

    public static final TagProcessor<MapTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag(prefix, attr -> new MapTag(attr.getParam()));

        ObjectFetcher.registerFetcher(prefix, MapTag::new);

        TAG_PROCESSOR.registerTag(ElementTag.class, "size", (attr, obj) -> new ElementTag(obj.map.size()));

        TAG_PROCESSOR.registerTag(ListTag.class, "keys", (attr, obj) -> new ListTag(String.join("|", obj.map.keySet())));

        TAG_PROCESSOR.registerTag(AbstractTag.class, "get", (attr, obj) -> {
            if (!attr.hasParam()) return null;

            AbstractTag current = obj;
            for (String key : attr.getParam().split("\\.", -1)) {
                if (!(current instanceof MapTag mapTag)) return null;
                current = mapTag.getObject(key);
            }
            return current;

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

    public void putObject(String key, AbstractTag tag) {
        if (tag == null) {
            map.remove(key);
        } else {
            map.put(key, tag.identify());
        }
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) { return TAG_PROCESSOR.process(this, attribute); }

    @Override
    public @NonNull TagProcessor<MapTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public @NonNull String getTestValue() {
        return "map@[a=1;b=2]";
    }
}