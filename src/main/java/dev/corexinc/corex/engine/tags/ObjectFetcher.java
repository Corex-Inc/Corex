package dev.corexinc.corex.engine.tags;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ObjectFetcher {

    private static final Map<String, Function<String, AbstractTag>> fetchers = new HashMap<>();

    public static void registerFetcher(String prefix, Function<String, AbstractTag> constructor) {
        fetchers.put(prefix.toLowerCase(), constructor);
    }

    public static AbstractTag pickObject(String value) {
        if (value == null) return new ElementTag("null");

        int atIndex = value.indexOf('@');
        if (atIndex > 0) {
            String prefix = value.substring(0, atIndex).toLowerCase();
            Function<String, AbstractTag> constructor = fetchers.get(prefix);

            if (constructor != null) {
                try {
                    return constructor.apply(value.substring(atIndex + 1));
                } catch (Exception ignored) {
                }
            }
        }

        return new ElementTag(value);
    }

    public static List<String> splitIgnoringBrackets(String str, char delimiter) {
        List<String> result = new ArrayList<>();
        int brackets = 0;
        int start = 0;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '[') brackets++;
            else if (c == ']') brackets--;
            else if (brackets == 0 && c == delimiter) {
                result.add(str.substring(start, i));
                start = i + 1;
            }
        }
        if (start <= str.length()) {
            result.add(str.substring(start));
        }
        return result;
    }
}
