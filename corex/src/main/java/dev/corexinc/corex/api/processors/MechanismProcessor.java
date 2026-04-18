package dev.corexinc.corex.api.processors;

import dev.corexinc.corex.api.tags.AbstractTag;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

@ApiStatus.AvailableSince("1.0.0")
public final class MechanismProcessor<T extends AbstractTag> {

    private final Map<String, BiFunction<T, AbstractTag, AbstractTag>> mechanisms = new HashMap<>();

    public void registerMechanism(@NotNull String name, @NotNull BiFunction<T, AbstractTag, AbstractTag> action) {
        mechanisms.put(name, action);
    }

    public AbstractTag process(@NotNull T object, @NotNull String name, @NotNull AbstractTag value) {
        BiFunction<T, AbstractTag, AbstractTag> action = mechanisms.get(name);

        if (action != null) {
            return action.apply(object, value);
        }

        return object;
    }
}