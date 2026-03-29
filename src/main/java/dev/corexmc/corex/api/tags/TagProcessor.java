package dev.corexmc.corex.api.tags;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class TagProcessor<T extends AbstractTag> {

    private final Map<String, BiFunction<Attribute, T, AbstractTag>> registeredTags = new HashMap<>();

    public void registerTag(String name, BiFunction<Attribute, T, AbstractTag> action) {
        registeredTags.put(name.toLowerCase(), action);
    }

    public AbstractTag process(T object, Attribute attribute) {
        BiFunction<Attribute, T, AbstractTag> action = registeredTags.get(attribute.getName().toLowerCase());

        if (action != null) {
            return action.apply(attribute, object);
        }

        return null;
    }
}
