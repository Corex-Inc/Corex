package dev.corexmc.corex.engine.registry;

import dev.corexmc.corex.api.tags.AbstractFormatter;
import dev.corexmc.corex.api.tags.AbstractTag;

import java.util.HashMap;
import java.util.Map;

public class FormatRegistry {

    private final Map<String, AbstractTag> formats = new HashMap<>();

    public void register(AbstractFormatter formatter) {
        AbstractTag value = formatter.getObject();
        formats.put(formatter.getName(), value);

        if (formatter.getAlias() != null) {
            for (String alias : formatter.getAlias()) {
                formats.put(alias, value);
            }
        }
    }

    public AbstractTag get(String tag) {
        return formats.get(tag);
    }

    public boolean isFormat(String tag) {
        return formats.containsKey(tag);
    }
}