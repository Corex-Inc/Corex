package dev.corexinc.corex.environment.utils.scripts;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;

import java.util.Map;

public class JsonHelper {

    private static final Gson PRETTY_GSON = new GsonBuilder().setPrettyPrinting().create();

    public static AbstractTag fromJson(JsonElement json) {
        if (json.isJsonObject()) {
            MapTag map = new MapTag();
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
                map.putObject(entry.getKey(), fromJson(entry.getValue()));
            }
            return map;
        } else if (json.isJsonArray()) {
            ListTag list = new ListTag();
            for (JsonElement element : json.getAsJsonArray()) {
                list.addObject(fromJson(element));
            }
            return list;
        } else if (json.isJsonPrimitive()) {
            JsonPrimitive prim = json.getAsJsonPrimitive();
            if (prim.isBoolean()) return new ElementTag(prim.getAsBoolean());
            if (prim.isNumber()) return new ElementTag(prim.getAsDouble());
            return new ElementTag(prim.getAsString());
        } else {
            return null;
        }
    }

    public static JsonElement toJson(AbstractTag tag) {
        if (tag instanceof MapTag mapTag) {
            JsonObject obj = new JsonObject();
            for (String key : mapTag.keySet()) {
                obj.add(key, toJson(mapTag.getObject(key)));
            }
            return obj;
        } else if (tag instanceof ListTag listTag) {
            JsonArray arr = new JsonArray();
            for (AbstractTag item : listTag.getList()) {
                arr.add(toJson(item));
            }
            return arr;
        } else {
            ElementTag el = (tag instanceof ElementTag) ? (ElementTag) tag : new ElementTag(tag.identify());
            if (el.isBoolean()) return new JsonPrimitive(el.asBoolean());
            if (el.isInt()) return new JsonPrimitive(el.asLong());
            if (el.isDouble()) return new JsonPrimitive(el.asDouble());
            return new JsonPrimitive(el.asString());
        }
    }

    public static String toPrettyString(JsonElement json) {
        return PRETTY_GSON.toJson(json);
    }
}