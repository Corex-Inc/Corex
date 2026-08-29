package dev.corexinc.corex.engine.tags;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.addons.AddonManager;
import dev.corexinc.corex.engine.addons.AddonOwner;
import dev.corexinc.corex.engine.addons.AddonOwnership;
import dev.corexinc.corex.environment.tags.core.ElementTag;

import java.util.*;
import java.util.function.Function;

public class ObjectFetcher {

    private static final Map<String, Function<String, AbstractTag>> fetchers = new HashMap<>();
    private static final Map<String, String> typeNamesToPrefixes = new HashMap<>();

    private static final StackWalker CALLER_WALKER = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    public static void registerFetcher(String prefix, Function<String, AbstractTag> constructor) {
        AddonOwner owner = AddonManager.requireOwner("the object prefix '" + prefix + "@'");
        if (owner == null) {
            return;
        }

        fetchers.put(prefix.toLowerCase(), constructor);
        AddonManager.noteHandler(constructor, owner);
        AddonOwnership.claim(AddonOwnership.Kind.FETCHER, prefix.toLowerCase(), owner);

        String callerName = CALLER_WALKER.getCallerClass().getSimpleName().toLowerCase();
        if (callerName.endsWith("tag")) {
            typeNamesToPrefixes.put(callerName.substring(0, callerName.length() - 3), prefix.toLowerCase());
        }
    }

    public static String getPrefixForName(String name) {
        if (fetchers.containsKey(name.toLowerCase())) return name.toLowerCase();
        return typeNamesToPrefixes.get(name.toLowerCase());
    }

    public static AbstractTag pickObject(String value) {
        if (value == null) return new ElementTag("null");

        int atIndex = value.indexOf('@');
        if (atIndex > 0) {
            Function<String, AbstractTag> constructor = fetchers.get(value.substring(0, atIndex).toLowerCase());
            if (constructor != null) {
                AbstractTag result = constructor.apply(value.substring(atIndex + 1));
                if (result != null) return result;
            }
        }

        return new ElementTag(value);
    }

    public static List<String> splitIgnoringBrackets(String str, char delimiter) {
        List<String> result = new ArrayList<>();
        int brackets = 0;
        int start = 0;
        boolean escaped = false;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);

            if (escaped) { escaped = false; continue; }
            if (c == '\\') { escaped = true; continue; }

            if (c == '[') brackets++;
            else if (c == ']') brackets--;
            else if (brackets == 0 && c == delimiter) {
                result.add(str.substring(start, i));
                start = i + 1;
            }
        }

        result.add(str.substring(start));
        return result;
    }

    public static Set<String> getPrefixes() {
        return fetchers.keySet();
    }
}