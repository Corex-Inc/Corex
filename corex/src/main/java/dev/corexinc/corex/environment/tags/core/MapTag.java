package dev.corexinc.corex.environment.tags.core;

import com.google.gson.JsonElement;
import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.utils.scripts.JsonHelper;
import org.jspecify.annotations.NonNull;

import java.util.*;

/* @doc object
 *
 * @Name MapTag
 * @Prefix map
 * @Format
 * The identity format for MapTags is a replica of property syntax - square brackets surrounded a semicolon separated list of key=value pairs.
 * For example, a map of "taco" to "food", "chicken" to "animal", and "bob" to "person" would be "map@[taco=food;chicken=animal;bob=person]"
 * A map with zero items in it is simply 'map@[]'.
 *
 * @Description
 * A MapTag represents a mapping of keys to values.
 * Keys are plain text, case-sensitive.
 * Values can be anything, even lists or maps themselves.
 *
 * Any given key can only appear in a map once (ie, no duplicate keys).
 * Values can be duplicated into multiple keys exclude issue.
 *
 * Order of keys is preserved.
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
         * @Name isEmpty
         * @RawName <MapTag.isEmpty>
         * @Object MapTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns true if the map contains no entries, false otherwise.
         * @Usage
         * // Narrates "true"
         * - narrate <map[].isEmpty>
         *
         * @Implements MapTag.is_empty
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isEmpty", (attr, obj) -> new ElementTag(obj.map.isEmpty()));

        /* @doc tag
         *
         * @Name keys
         * @RawName <MapTag.keys>
         * @Object MapTag
         * @ReturnType ListTag
         * @NoArg
         * @Description
         * Returns a list of all keys in this map.
         * @Usage
         * // Narrates 'a|b|c'
         * - narrate <map[a=1;b=2;c=3].keys>
         *
         * @Implements MapTag.keys
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "keys", (attr, obj) -> {
            ListTag result = new ListTag();
            obj.map.keySet().forEach(result::addString);
            return result;
        });

        /* @doc tag
         *
         * @Name find[]
         * @RawName <MapTag.find[<value>|...]>
         * @Object MapTag
         * @ReturnType ElementTag, ListTag
         * @ArgRequired
         * @Description
         * Returns the first key whose value matches the given value.
         * If a list of values is given, returns a list of matching keys (skipping values not found).
         * @Usage
         * // Narrates "b"
         * - narrate <map[a=1;b=2;c=3].find[2]>
         * @Usage
         * // Narrates "a|c"
         * - narrate <map[a=1;b=2;c=3].find[1|3]>
         *
         * @Implements MapTag.find[<value>|...]
         */
        TAG_PROCESSOR.registerTag(AbstractTag.class, "find", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            ListTag values = new ListTag(attr.getParam());
            if (values.size() == 1) {
                String target = values.get(0);
                return obj.map.entrySet().stream()
                        .filter(e -> e.getValue().equals(target))
                        .findFirst()
                        .map(e -> (AbstractTag) new ElementTag(e.getKey()))
                        .orElse(null);
            }
            ListTag result = new ListTag();
            for (AbstractTag value : values.getList()) {
                String target = value.identify();
                obj.map.entrySet().stream()
                        .filter(e -> e.getValue().equals(target))
                        .findFirst()
                        .ifPresent(e -> result.addString(e.getKey()));
            }
            return result;
        }).test("2");

        /* @doc tag
         *
         * @Name include[]
         * @RawName <MapTag.include[<map>]>
         * @Object MapTag
         * @ReturnType MapTag
         * @ArgRequired
         * @Description
         * Returns a copy of the map merged with the given map.
         * Keys from the input map overwrite existing keys on conflict.
         * @Usage
         * // Narrates "map@[a=1;b=99;c=3]"
         * - narrate <map[a=1;b=2].include[b=99;c=3]>
         *
         * @Implements MapTag.include[<map>]
         */
        TAG_PROCESSOR.registerTag(MapTag.class, "include", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            MapTag result = new MapTag();
            result.map.putAll(obj.map);
            result.map.putAll(new MapTag(attr.getParam()).map);
            return result;
        }).test("<map[testKey=testValue]>");

        /* @doc tag
         *
         * @Name reverse
         * @RawName <MapTag.reverse>
         * @Object MapTag
         * @ReturnType MapTag
         * @NoArg
         * @Description
         * Returns a copy of the map with the entry order reversed.
         * @Usage
         * // Narrates "map@[c=3;b=2;a=1]"
         * - narrate <map[a=1;b=2;c=3].reverse>
         *
         * @Implements MapTag.reverse
         */
        TAG_PROCESSOR.registerTag(MapTag.class, "reverse", (attr, obj) -> {
            List<Map.Entry<String, String>> entries = new ArrayList<>(obj.map.entrySet());
            Collections.reverse(entries);
            MapTag result = new MapTag();
            entries.forEach(e -> result.map.put(e.getKey(), e.getValue()));
            return result;
        });

        /* @doc tag
         *
         * @Name toList[]
         * @RawName <MapTag.toList[(<separator>)]>
         * @Object MapTag
         * @ReturnType ListTag
         * @Description
         * Converts the map into a ListTag of entries.
         * By default, each entry is formatted as "key=value".
         * Optionally specify a separator to use instead of '='.
         * @Usage
         * // Narrates "a=1|b=2|c=3"
         * - narrate <map[a=1;b=2;c=3].toList>
         * @Usage
         * // Narrates "a: 1|b: 2|c: 3"
         * - narrate <map[a=1;b=2;c=3].toList[: ]>
         *
         * @Implements MapTag.to_list
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "toList", (attr, obj) -> {
            String sep = attr.hasParam() ? attr.getParam() : "=";
            ListTag result = new ListTag();
            obj.map.forEach((k, v) -> result.addString(k + sep + v));
            return result;
        });

        /* @doc tag
         *
         * @Name toPairLists
         * @RawName <MapTag.toPairLists>
         * @Object MapTag
         * @ReturnType ListTag(ListTag)
         * @NoArg
         * @Description
         * Converts the map into a list of two-element lists, each containing [key, value].
         * Useful for foreach loops where both the key and value are needed.
         * @Usage
         * // Narrates "a is set to 1", then "b is set to 2", then "c is set to 3"
         * - foreach <map[a=1;b=2;c=3].toPairLists> as:pair:
         *     - narrate "<[pair].get[1]> is set to <[pair].get[2]>"
         *
         * @Implements MapTag.to_pair_lists
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "toPairLists", (attr, obj) -> {
            ListTag result = new ListTag();
            obj.map.forEach((k, v) -> {
                ListTag pair = new ListTag();
                pair.addString(k);
                pair.addObject(ObjectFetcher.pickObject(v));
                result.addObject(pair);
            });
            return result;
        });

        /* @doc tag
         *
         * @Name values
         * @RawName <MapTag.values>
         * @Object MapTag
         * @ReturnType ListTag
         * @NoArg
         * @Description
         * Returns a list of all values in this map, in insertion order.
         * @Usage
         * // Narrates "1|2|3"
         * - narrate <map[a=1;b=2;c=3].values>
         *
         * @Implements MapTag.values
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "values", (attr, obj) -> {
            ListTag result = new ListTag();
            obj.map.values().forEach(v -> result.addObject(ObjectFetcher.pickObject(v)));
            return result;
        });

        /* @doc tag
         *
         * @Name contains[]
         * @RawName <MapTag.contains[<key>|...]>
         * @Object MapTag
         * @ReturnType ElementTag(Boolean)
         * @ArgRequired
         * @Description
         * Returns whether the map contains the specified key.
         * If a list is given as input, returns true only if ALL specified keys are present.
         * @Usage
         * // Narrates "true"
         * - narrate <map[a=1;b=2].contains[a]>
         * @Usage
         * // Narrates "false" (c is missing)
         * - narrate <map[a=1;b=2].contains[a|b|c]>
         *
         * @Implements MapTag.contains[<key>|...]
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "contains", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            for (AbstractTag key : new ListTag(attr.getParam()).getList()) {
                if (!obj.map.containsKey(key.identify())) return new ElementTag(false);
            }
            return new ElementTag(true);
        }).test("a");

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
         * - narrate <map.with[root.leaf=myvalue].get[root.leaf]>
         * @Usage
         * // Narrates 'myvalue'
         * - definemap mymap:
         *     root:
         *         leaf: myvalue
         * - narrate <[mymap].get[root.leaf]>
         *
         * @Implements MapTag.deep_get[<key>|...], MapTag.get[<key>|...]
         */
        TAG_PROCESSOR.registerTag(AbstractTag.class, "get", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            ListTag keys = new ListTag(attr.getParam());

            if (keys.size() > 1) {
                ListTag result = new ListTag();
                for (AbstractTag keyTag : keys.getList()) {
                    String rawKey = keyTag.identify();
                    if (rawKey.contains("@") || !rawKey.contains(".")) {
                        AbstractTag found = obj.getObject(rawKey);
                        if (found != null) result.addObject(found);
                    } else {
                        AbstractTag current = obj;
                        for (String part : rawKey.split("\\.", -1)) {
                            if (!(current instanceof MapTag mapTag)) { current = null; break; }
                            current = mapTag.getObject(part);
                        }
                        if (current != null) result.addObject(current);
                    }
                }
                return result;
            }

            String rawKey = attr.getParam();
            if (rawKey.contains("@") || !rawKey.contains(".")) {
                return obj.getObject(rawKey);
            }

            AbstractTag current = obj;
            for (String key : rawKey.split("\\.", -1)) {
                if (!(current instanceof MapTag mapTag)) return null;
                current = mapTag.getObject(key);
            }
            return current;
        }).test("a");

        /* @doc tag
         *
         * @Name getSubset[]
         * @RawName <MapTag.getSubset[<key>|...]>
         * @Object MapTag
         * @ReturnType MapTag
         * @ArgRequired
         * @Description
         * Returns a sub-map containing only the specified keys, ordered by the input list.
         * Keys not present in the map are silently skipped.
         * @Usage
         * // Narrates "map@[b=2;a=1]"
         * - narrate <map[a=1;b=2;c=3].getSubset[b|a]>
         *
         * @Implements MapTag.get_subset[<key>|...]
         */
        TAG_PROCESSOR.registerTag(MapTag.class, "getSubset", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            MapTag result = new MapTag();
            for (AbstractTag key : new ListTag(attr.getParam()).getList()) {
                String k = key.identify();
                String val = obj.map.get(k);
                if (val != null) result.map.put(k, val);
            }
            return result;
        }).test("a");

        /* @doc tag
         *
         * @Name with[]
         * @RawName <MapTag.with[<map>]>
         * @Object MapTag
         * @ReturnType MapTag
         * @ArgRequired
         * @Description
         * Returns a copy of the map with the specified key set to the given value.
         * Supports deep key paths separated by '.' - no separate deep_with needed.
         * @Usage
         * // Narrates "map@[a=1;b=99;c=3]"
         * - narrate <map[a=1;b=2;c=3].with[b=99]>
         * @Usage
         * // Deep path - sets root.leaf = myvalue inside a nested map
         * - narrate <map[].with[root.leaf=myvalue]>
         *
         * @Implements MapTag.with[<key>].as[<value>], MapTag.deep_with[<key>].as[<value>]
         */
        TAG_PROCESSOR.registerTag(MapTag.class, "with", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            MapTag param = new MapTag(attr.getParam());
            MapTag result = new MapTag();
            result.map.putAll(obj.map);
            param.map.forEach((k, v) -> result.putDeepObject(k, ObjectFetcher.pickObject(v)));
            return result;
        }).test("c=3");

        /* @doc tag
         *
         * @Name exclude[]
         * @RawName <MapTag.exclude[<key>|...]>
         * @Object MapTag
         * @ReturnType MapTag
         * @ArgRequired
         * @Description
         * Returns a copy of the map with the specified key(s) removed.
         * Accepts a list to remove multiple keys at once.
         * @Usage
         * // Narrates "map@[a=1;c=3]"
         * - narrate <map[a=1;b=2;c=3].exclude[b]>
         * @Usage
         * // Narrates "map@[b=2]"
         * - narrate <map[a=1;b=2;c=3].exclude[a|c]>
         *
         * @Implements MapTag.exclude[<key>|...]
         */
        TAG_PROCESSOR.registerTag(MapTag.class, "exclude", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            MapTag result = new MapTag();
            result.map.putAll(obj.map);
            for (AbstractTag key : new ListTag(attr.getParam()).getList()) {
                result.map.remove(key.identify());
            }
            return result;
        }).test("b");

        /* @doc tag
         *
         * @Name invert
         * @RawName <MapTag.invert>
         * @Object MapTag
         * @ReturnType MapTag
         * @NoArg
         * @Description
         * Returns an inverted copy of the map - keys become values and values become keys.
         * If multiple original values are identical, the last matching key wins.
         * @Usage
         * // Narrates "map@[food=taco;drink=water]"
         * - narrate <map[taco=food;water=drink].invert>
         *
         * @Implements MapTag.invert
         */
        TAG_PROCESSOR.registerTag(MapTag.class, "invert", (attr, obj) -> {
            MapTag result = new MapTag();
            obj.map.forEach((k, v) -> result.putObject(v, new ElementTag(k)));
            return result;
        });

        /* @doc tag
         *
         * @Name sortValue
         * @RawName <MapTag.sortValue>
         * @Object MapTag
         * @ReturnType MapTag
         * @NoArg
         * @Description
         * Returns a copy of the map sorted lexicographically by its values.
         * @Usage
         * // Narrates "map@[a=1;b=2;c=3]"
         * - narrate <map[c=3;a=1;b=2].sortValue>
         *
         * @Implements MapTag.sort_by_value
         */
        TAG_PROCESSOR.registerTag(MapTag.class, "sortValue", (attr, obj) -> {
            List<Map.Entry<String, String>> entries = new ArrayList<>(obj.map.entrySet());
            entries.sort(Map.Entry.comparingByValue());
            MapTag result = new MapTag();
            entries.forEach(e -> result.map.put(e.getKey(), e.getValue()));
            return result;
        });

        /* @doc tag
         *
         * @Name toJson
         * @RawName <MapTag.toJson>
         * @Object MapTag
         * @ReturnType ElementTag
         * @NoArg
         *
         * @Description
         * Converts the MapTag into a strict JSON string.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "toJson", (attr, obj) -> {
            JsonElement json = JsonHelper.toJson(obj);

            /* @doc tag
             *
             * @Name toJson.pretty
             * @RawName <MapTag.toJson.pretty>
             * @Object MapTag
             * @ReturnType ElementTag
             * @NoArg
             *
             * @Description
             * Converts the MapTag into an element with nicely formatted multiline JSON.
             */
            if (attr.matchesNext("pretty")) {
                attr.fulfill(1);
                return new ElementTag(JsonHelper.toPrettyString(json));
            }
            return new ElementTag(json.toString());
        });
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

    public void putDeepObject(String key, AbstractTag tag) {
        if (key.contains("@") || !key.contains(".")) {
            putObject(key, tag);
            return;
        }

        try {
            Double.parseDouble(key);
            putObject(key, tag);
            return;
        } catch (NumberFormatException ignored) {}

        String[] parts = key.split("\\.", 2);
        AbstractTag existing = getObject(parts[0]);
        MapTag nested = existing instanceof MapTag m ? new MapTag(m.map) : new MapTag();
        nested.putDeepObject(parts[1], tag);
        putObject(parts[0], nested);
    }

    @Override public @NonNull String getPrefix() { return prefix; }

    @Override
    public @NonNull String identify() {
        List<String> pairs = new ArrayList<>();
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

    public void remove(String key) {
        if (key != null) {
            map.remove(key.toLowerCase());
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