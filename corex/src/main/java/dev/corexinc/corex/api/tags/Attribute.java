package dev.corexinc.corex.api.tags;

import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.engine.compiler.TagNode;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Represents the current state (context) of a tag evaluation chain.
 * <p>
 * This class acts as a cursor, allowing {@link TagProcessor} implementations to inspect
 * attribute names, retrieve parameters, and navigate through the tag chain (e.g., {@code <player.name.length>}).
 *
 * @since 1.0.0
 */
public class Attribute {

    private final TagNode[] components;
    private int currentIndex = 0;
    private final ScriptQueue queue;

    public Attribute(TagNode[] components, ScriptQueue queue) {
        this.components = components;
        this.queue = queue;
    }

    /**
     * Checks if there are more attributes remaining in the current tag chain.
     *
     * @return {@code true} if there is another piece to process.
     */
    @Contract(pure = true)
    public boolean hasNext() {
        return currentIndex < components.length;
    }

    /**
     * Gets the name of the current attribute piece.
     * <p>
     * For a tag like {@code <player.location.x>}, if the cursor is at the second piece,
     * this will return "location".
     *
     * @return the name of the attribute, or "null" if the index is out of bounds.
     */
    @NotNull
    @Contract(pure = true)
    public String getName() {
        int index = Math.min(currentIndex, components.length - 1);
        if (index < 0) return "null";
        return components[index].name();
    }

    /**
     * Checks if the current attribute piece has a parameter inside brackets {@code [...]}.
     *
     * @return {@code true} if a parameter is present.
     */
    @Contract(pure = true)
    public boolean hasParam() {
        int index = Math.min(currentIndex, components.length - 1);
        if (index < 0) return false;
        return components[index].param() != null;
    }

    /**
     * Retrieves the current attribute parameter as a plain string.
     *
     * @return the string representation of the parameter, or {@code null} if not present.
     */
    @Contract(pure = true)
    public String getParam() {
        AbstractTag tag = getParamObject();
        return tag != null ? tag.identify() : null;
    }

    /**
     * Evaluates the current attribute's parameter and returns it as an {@link AbstractTag}.
     * <p>
     * Use this when the parameter might be a complex object (like a Location or another Player)
     * rather than a simple string.
     *
     * @return the evaluated parameter object, or {@code null} if not present.
     */
    @Contract(pure = true)
    public AbstractTag getParamObject() {
        int index = Math.min(currentIndex, components.length - 1);
        if (index < 0 || components[index].param() == null) return null;
        return components[index].param().evaluate(queue);
    }

    /**
     * Retrieves the current parameter and attempts to cast it to the specified tag type.
     */
    @Contract(pure = true)
    public <T extends AbstractTag> T getParamObject(@NotNull Class<T> type) {
        AbstractTag obj = getParamObject();

        if (obj == null) {
            return null;
        }

        if (type.isInstance(obj)) {
            return type.cast(obj);
        }

        return null;
    }

    /**
     * Advanced parameter retrieval: evaluates the parameter as an object of type {@code T},
     * or uses a fallback parser if the evaluated result is not of that type.
     */
    @Contract(pure = true)
    public <T extends AbstractTag> T getParamObject(@NotNull Class<T> type, @NotNull Function<String, T> parser) {
        T typed = getParamObject(type);

        if (typed != null) {
            return typed;
        }

        String raw = getParam();

        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return parser.apply(raw);
        }
        catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Checks if the next attribute in the chain matches the expected name.
     * Useful for multipart tags like {@code .if_true[...].if_false[...]}.
     *
     * @param expectedName the name to compare against (case-sensitive).
     * @return {@code true} if the next attribute matches.
     */
    @Contract(pure = true)
    public boolean matchesNext(@NotNull String expectedName) {
        if (currentIndex + 1 >= components.length) {
            return false;
        }

        return components[currentIndex + 1].name().equals(expectedName);
    }

    /**
     * Checks if the next attribute in the chain has a parameter.
     */
    @Contract(pure = true)
    public boolean hasNextParam() {
        if (currentIndex + 1 >= components.length) {
            return false;
        }

        return components[currentIndex + 1].param() != null;
    }

    /**
     * Retrieves the next attribute's parameter as a plain string.
     */
    public String getNextParam() {
        AbstractTag tag = getNextParamObject();
        return tag != null ? tag.identify() : null;
    }

    /**
     * Evaluates the next attribute's parameter and returns it as an {@link AbstractTag}.
     */
    public AbstractTag getNextParamObject() {
        if (currentIndex + 1 >= components.length || components[currentIndex + 1].param() == null) {
            return null;
        }

        return components[currentIndex + 1].param().evaluate(queue);
    }

    /**
     * Evaluates the next attribute's parameter and returns it as an {@link AbstractTag}.
     */
    public <T extends AbstractTag> T getNextParamObject(Class<T> type) {
        AbstractTag obj = getNextParamObject();

        if (obj == null) {
            return null;
        }

        if (type.isInstance(obj)) {
            return type.cast(obj);
        }

        return null;
    }

    /**
     * Evaluates the next attribute's parameter and returns it as an {@link AbstractTag}.
     */
    public <T extends AbstractTag> T getNextParamObject(Class<T> type, Function<String, T> parser) {
        T typed = getNextParamObject(type);

        if (typed != null) {
            return typed;
        }

        String raw = getNextParam();

        if (raw == null || raw.isBlank()) {
            return null;
        }

        try {
            return parser.apply(raw);
        }
        catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Advances the evaluation cursor by the specified number of steps.
     * <p>
     * Use this when a single logic block handles multiple parts of a tag chain.
     *
     * @param steps number of attribute pieces to "consume".
     */
    public void fulfill(int steps) {
        this.currentIndex += steps;
    }

    /**
     * Gets the {@link ScriptQueue} in which this tag is being evaluated.
     * Provides access to definitions and queue context.
     *
     * @return the current script queue.
     */
    @NotNull
    @Contract(pure = true)
    public ScriptQueue getQueue() {
        return queue;
    }
}