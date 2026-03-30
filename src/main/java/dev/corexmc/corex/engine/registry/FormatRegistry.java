package dev.corexmc.corex.engine.registry;

import dev.corexmc.corex.api.tags.AbstractFormatter;
import java.util.HashMap;
import java.util.Map;

public class FormatRegistry {
    private final Map<String, String> formats = new HashMap<>();

    public void register(AbstractFormatter formatter) {
        String value = formatter.getValue();

        formats.put(formatter.getName().toLowerCase(), value);

        if (formatter.getAlias() != null) {
            for (String alias : formatter.getAlias()) {
                formats.put(alias.toLowerCase(), value);
            }
        }
    }

    public String get(String tag) {
        return formats.get(tag.toLowerCase());
    }

    public boolean isFormat(String tag) {
        return formats.containsKey(tag.toLowerCase());
    }
}