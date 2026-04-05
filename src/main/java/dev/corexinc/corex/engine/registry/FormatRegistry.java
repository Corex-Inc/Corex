package dev.corexinc.corex.engine.registry;

import dev.corexinc.corex.api.tags.AbstractFormatter;
import java.util.HashMap;
import java.util.Map;

public class FormatRegistry {

    private final Map<String, AbstractFormatter> formats = new HashMap<>();

    public void register(AbstractFormatter formatter) {
        formats.put(formatter.getName(), formatter);
        formatter.getAlias();
        for (String alias : formatter.getAlias()) {
            formats.put(alias, formatter);
        }
    }

    public AbstractFormatter get(String tag) {
        return formats.get(tag);
    }

    public boolean isFormat(String tag) {
        return formats.containsKey(tag);
    }
}