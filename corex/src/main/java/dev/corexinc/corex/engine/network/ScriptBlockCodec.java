package dev.corexinc.corex.engine.network;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Moves an uncompiled script block across the wire.
 *
 * <p>A block written under {@code - proxy script:} arrives as the raw YAML the author typed: a list
 * whose entries are either a line, or a map from a line ending in a colon to its own nested block.
 * That shape is what {@link dev.corexinc.corex.engine.scripts.ScriptManager#compileBlock} expects,
 * so it is what has to survive the trip, nesting included. Compiled bytecode is not an option, it
 * holds live references and is built against the sending server's registry.</p>
 *
 * <p>JSON is the carrier rather than flattened lines because an {@code if:} or a {@code repeat:}
 * inside the block has structure that indentation alone would force the far side to re-parse.
 * Every leaf is written and read back as a string, so a line like {@code - wait 5} cannot come out
 * the other end as a number.</p>
 *
 * @since 1.0.0
 */
public final class ScriptBlockCodec {

    /**
     * How deeply blocks may nest. Well past anything readable, and low enough that a hostile
     * payload cannot drive the recursive decoder into a {@link StackOverflowError}.
     */
    public static final int MAX_DEPTH = 32;

    private ScriptBlockCodec() {}

    public static @NotNull String encode(@NotNull List<?> block) {
        return toElement(block, 0).toString();
    }

    /**
     * Rebuilds a raw script block, ready for {@code ScriptManager.compileBlock}.
     *
     * @param json the string produced by {@link #encode}.
     * @return the block as a list of lines and nested blocks.
     * @throws PacketFormatException if the string is not the JSON array this codec writes.
     */
    public static @NotNull List<Object> decode(@NotNull String json) {
        JsonElement root;
        try {
            root = JsonParser.parseString(json);
        }
        catch (JsonSyntaxException e) {
            throw new PacketFormatException("Script block is not valid JSON: " + e.getMessage(), e);
        }

        if (!root.isJsonArray()) {
            throw new PacketFormatException("Script block must be a JSON array, got " + root.getClass().getSimpleName());
        }

        Object decoded = fromElement(root, 0);
        @SuppressWarnings("unchecked")
        List<Object> block = (List<Object>) decoded;
        return block;
    }

    private static JsonElement toElement(Object value, int depth) {
        if (depth > MAX_DEPTH) {
            throw new PacketFormatException("Script block nests deeper than " + MAX_DEPTH + " levels");
        }

        if (value instanceof List<?> list) {
            JsonArray array = new JsonArray(list.size());
            for (Object entry : list) {
                array.add(toElement(entry, depth + 1));
            }
            return array;
        }

        if (value instanceof Map<?, ?> map) {
            JsonObject object = new JsonObject();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                object.add(String.valueOf(entry.getKey()), toElement(entry.getValue(), depth + 1));
            }
            return object;
        }

        return new JsonPrimitive(String.valueOf(value));
    }

    private static Object fromElement(JsonElement element, int depth) {
        if (depth > MAX_DEPTH) {
            throw new PacketFormatException("Script block nests deeper than " + MAX_DEPTH + " levels");
        }

        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            List<Object> list = new ArrayList<>(array.size());
            for (JsonElement entry : array) {
                list.add(fromElement(entry, depth + 1));
            }
            return list;
        }

        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                map.put(entry.getKey(), fromElement(entry.getValue(), depth + 1));
            }
            return map;
        }

        if (element.isJsonNull()) {
            throw new PacketFormatException("Script block contains a null entry");
        }

        return element.getAsString();
    }
}
