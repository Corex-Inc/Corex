package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import org.jspecify.annotations.NonNull;

import java.util.*;

/* @doc object
 *
 * @Name MapTag
 * @Prefix map
 * @Format
 * The identity format for MapTags is a replica of property syntax - square brackets surrounded a semi-colon separated list of key=value pairs.
 * For example, a map of "taco" to "food", "chicken" to "animal", and "bob" to "person" would be "map@[taco=food;chicken=animal;bob=person]"
 * A map with zero items in it is simply 'map@[]'.
 *
 * @Description
 * A MapTag represents a mapping of keys to values.
 * Keys are plain text, case-insensitive.
 * Values can be anything, even lists or maps themselves.
 *
 * Any given key can only appear in a map once (ie, no duplicate keys).
 * Values can be duplicated into multiple keys without issue.
 *
 * Order of keys is preserved. Casing in keys is preserved in the object but ignored for map lookups.
 */
public class MapTag implements AbstractTag {

    private static final String prefix = "map";
    private final Map<String, String> map = new LinkedHashMap<>();

    public static final TagProcessor<MapTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {
        BaseTagProcessor.registerBaseTag(prefix, attr -> new MapTag(attr.getParam()));

        ObjectFetcher.registerFetcher(prefix, MapTag::new);

        /* @doc tag
         *
         * @Name size
         * @RawName <MapTag.size>
         * @Object MapTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the size of the map - that is, how many key/value pairs are within it.
         * @Usage
         * // Narrates "2"
         * - narrate <map[a=1;b=2].size>
         *
         * @Implements MapTag.size
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "size", (attr, obj) -> new ElementTag(obj.map.size()));

        /* @doc tag
         *
         * @Name keys
         * @RawName <MapTag.keys>
         * @Object MapTag
         * @ReturnType ListTag
         * @NoArg
         * @Description
         * Returns a list of all keys in this map.
         * @example
         * // Narrates a list of 'a|b|c'
         * - narrate <map[a=1;b=2;c=3].keys>
         *
         * @Implements MapTag.keys
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "keys", (attr, obj) -> new ListTag(String.join("|", obj.map.keySet())));

        /* @doc tag
         *
         * @Name get[]
         * @RawName <MapTag.get[<key>|...]>
         * @Object MapTag
         * @ReturnType ObjectTag
         * @ArgRequired
         * @Description
         * Returns the object value at the specified key, using deep key paths separated by the '.' symbol.
         * If a list is given as input, returns a list of values.
         * @Usage
         * // Narrates 'myvalue'
         * - narrate <map.with[root].as[<map[leaf=myvalue]>].get[root.leaf]>
         * @Usage
         * // Narrates 'myvalue'
         * - definemap mymap:
         *     root:
         *         leaf: myvalue
         * - narrate <[mymap].get[root.leaf]>
         * // The below will also get the same result ('myvalue') using the definition tag's special automatic deep get syntax:
         * - narrate <[mymap.root.leaf]>
         *
         * @Implements MapTag.deep_get[<key>|...], MapTag.get[<key>|...]
         */
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

    public MapTag() {}

    public MapTag(Map<String, ?> javaMap) {
        if (javaMap == null) return;
        for (Map.Entry<String, ?> entry : javaMap.entrySet()) {
            Object value = entry.getValue();
            if (value == null) continue;
            AbstractTag tag = value instanceof List<?> list
                    ? new ListTag(list)
                    : ObjectFetcher.pickObject(value.toString());
            map.put(entry.getKey(), tag.identify());
        }
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

    public Set<String> keySet() {
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