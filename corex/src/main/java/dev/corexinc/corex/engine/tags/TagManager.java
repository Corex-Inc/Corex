package dev.corexinc.corex.engine.tags;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.addons.AddonManager;
import dev.corexinc.corex.engine.addons.AddonOwner;
import dev.corexinc.corex.engine.addons.AddonOwnership;
import dev.corexinc.corex.api.tags.Attribute;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class TagManager {

    private static final Map<String, Function<Attribute, AbstractTag>> baseTags = new HashMap<>();

    public static void registerBaseTag(String name, Function<Attribute, AbstractTag> function) {
        AddonOwner owner = AddonManager.requireOwner("the base tag '" + name + "'");
        if (owner == null) {
            return;
        }

        baseTags.put(name, function);
        AddonManager.noteHandler(function, owner);
        AddonOwnership.claim(AddonOwnership.Kind.BASE_TAG, name, owner);
    }

    public static AbstractTag executeBaseTag(Attribute attribute) {
        Function<Attribute, AbstractTag> function = baseTags.get(attribute.getName());

        if (function != null) {
            AbstractTag tag = function.apply(attribute);
            attribute.fulfill(1);
            return tag;
        }

        return null;
    }

    public static Function<Attribute, AbstractTag> getBaseTag(String name) {
        return baseTags.get(name);
    }

    public static Set<String> getBaseTagNames() {
        return baseTags.keySet();
    }
}