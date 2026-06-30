package dev.corexinc.corex.api.commands;

import dev.corexinc.corex.api.tags.AbstractTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * The result of {@link ArgumentSchema#bind} — a type-safe bag of evaluated arguments.
 *
 * <p>All values are already resolved and cast.  Absent optional args return
 * {@code null} unless a default was declared in the schema.
 *
 * <h3>Usage</h3>
 * <pre>{@code
 * ArgumentSet args = SCHEMA.bind(instruction, queue);
 * if (args == null) return;
 *
 * PlayerTag  player = args.prefix("target");
 * ElementTag loop   = args.prefix("loop");   // default "PLAY_ONCE" if not provided
 * ElementTag text   = args.linear(0);
 *
 * if (args.hasPrefix("silent")) { ... }
 * }</pre>
 */
public final class ArgumentSet {

    static final String NS_PREFIX = "p";
    static final String NS_LINEAR = "l";

    private final Map<String, AbstractTag> data = new HashMap<>();
    private final Map<String, Boolean> flags = new HashMap<>();

    ArgumentSet() {}

    void putPrefix(String key, @Nullable AbstractTag value) {
        data.put(NS_PREFIX + ":" + key, value);
    }

    void putLinear(int index, @Nullable AbstractTag value) {
        data.put(NS_LINEAR + ":" + index, value);
    }

    void putFlag(String key, boolean value) {
        flags.put(key.toLowerCase(), value);
    }

    /**
     * Returns the evaluated prefix arg cast to {@code T}.
     *
     * @param key the prefix name (e.g. {@code "target"})
     * @return the resolved tag, or {@code null} if optional and absent
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends AbstractTag> T prefix(@NotNull String key) {
        return (T) data.get(NS_PREFIX + ":" + key);
    }

    /**
     * Returns {@code true} if a value is present for this prefix arg
     * (either provided by the user or resolved from a default).
     */
    public boolean hasPrefix(@NotNull String key) {
        AbstractTag v = data.get(NS_PREFIX + ":" + key);
        return v != null;
    }

    /**
     * Returns the evaluated linear (positional) arg cast to {@code T}.
     *
     * @param index the zero-based position
     * @return the resolved tag, or {@code null} if optional and absent
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends AbstractTag> T linear(int index) {
        return (T) data.get(NS_LINEAR + ":" + index);
    }

    /**
     * Returns {@code true} if a value is present at this positional index.
     */
    public boolean hasLinear(int index) {
        AbstractTag v = data.get(NS_LINEAR + ":" + index);
        return v != null;
    }

    /**
     * Returns {@code true} if the flag was present in the script line.
     */
    public boolean flag(@NotNull String name) {
        return flags.getOrDefault(name.toLowerCase(), false);
    }
}