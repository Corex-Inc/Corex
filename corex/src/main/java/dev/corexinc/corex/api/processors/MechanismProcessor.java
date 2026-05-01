package dev.corexinc.corex.api.processors;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Adjustable;
import org.jetbrains.annotations.ApiStatus.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Manages the registration and application of mechanisms for a specific {@link AbstractTag}.
 *
 * <p>While tags (processed by {@link TagProcessor}) act as "getters" to retrieve data,
 * mechanisms act as "setters" or "adjustments" used to modify the state of an object.</p>
 *
 * <p>Mechanisms are primarily utilized by the {@code - adjust} command and the
 * {@link Adjustable#applyMechanism} system.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class MyTag implements AbstractTag, Adjustable {
 *     public static final MechanismProcessor<MyTag> MECHANISMS = new MechanismProcessor<>();
 *
 *     public static void register() {
 *         MECHANISMS.registerMechanism("health", (object, value) -> {
 *             double hp = ((ElementTag) value).asDouble();
 *             object.getHandle().setHealth(hp);
 *             return object;
 *         });
 *     }
 *
 *     @Override
 *     public AbstractTag applyMechanism(String mechanism, AbstractTag value) {
 *         return MECHANISMS.process(this, mechanism, value);
 *     }
 * }
 * }</pre>
 *
 * @param <T> the type of {@link AbstractTag} this processor handles.
 * @since 1.0.0
 */
@AvailableSince("1.0.0")
public final class MechanismProcessor<T extends AbstractTag> {

    /**
     * Internal registry of mechanisms mapped by their names.
     */
    private final Map<String, BiFunction<T, AbstractTag, AbstractTag>> mechanisms = new HashMap<>();

    /**
     * Registers a new mechanism handler.
     *
     * @param name   the name of the mechanism (e.g., {@code "health"}).
     * @param action the handler function that receives the object {@code T}
     *               and the value {@link AbstractTag} to be applied.
     *               Should return the modified object.
     * @throws NullPointerException if name or action is {@code null}.
     */
    public void registerMechanism(@NotNull String name, @NotNull BiFunction<T, AbstractTag, AbstractTag> action) {
        mechanisms.put(name, action);
    }

    /**
     * Processes and applies a mechanism to a given object instance.
     *
     * <p>If a registered handler is found for the given name, it is executed.
     * If no handler is found, the method returns the original object unchanged.</p>
     *
     * @param object the tag instance to be modified.
     * @param name   the name of the mechanism to look up.
     * @param value  the value data for the adjustment.
     * @return the resulting {@link AbstractTag} (usually the modified input object).
     */
    @NotNull
    @Contract(pure = false)
    @Internal
    @AvailableSince("1.0.0")
    public AbstractTag process(@NotNull T object, @NotNull String name, @NotNull AbstractTag value) {
        BiFunction<T, AbstractTag, AbstractTag> action = mechanisms.get(name);

        if (action != null) {
            return action.apply(object, value);
        }

        return object;
    }
}