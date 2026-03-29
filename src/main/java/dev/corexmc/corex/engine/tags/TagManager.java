package dev.corexmc.corex.engine.tags;

import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.api.tags.Attribute;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class TagManager {

    private static final Map<String, Function<Attribute, AbstractTag>> baseTags = new HashMap<>();

    public static void registerBaseTag(String name, Function<Attribute, AbstractTag> function) {
        baseTags.put(name.toLowerCase(), function);
    }

    public static AbstractTag executeBaseTag(Attribute attribute) {
        String baseName = attribute.getName().toLowerCase();
        Function<Attribute, AbstractTag> function = baseTags.get(baseName);

        if (function != null) {
            AbstractTag tag = function.apply(attribute);
            attribute.fulfill(1);
            return tag;
        }

        return null;
    }
}