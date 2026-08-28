package dev.corexinc.corex.engine.addons;

import dev.corexinc.corex.engine.utils.CorexLogger;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Records which owner provided each named component, so Corex can answer "whose tag is this?"
 * long after registration.
 *
 * <p>Every registry in the engine claims its keys here as it fills up. Two things come out of
 * that: an override is reported the moment it happens, naming both plugins, and a crash inside a
 * handler can be attributed to the addon that supplied it rather than to Corex.</p>
 *
 * <p>Keys are per kind and follow whatever the matching registry uses, so a command is claimed
 * under its lowercase name and a sub-tag under {@code ObjectTag.subTag}.</p>
 *
 * @since 1.0.0
 */
@AvailableSince("1.0.0")
public final class AddonOwnership {

    /** The kind of component a claim refers to. */
    public enum Kind {

        COMMAND("command"),
        OBJECT("object type"),
        SUB_TAG("tag"),
        MECHANISM("mechanism"),
        BASE_TAG("base tag"),
        FETCHER("object fetcher"),
        FORMATTER("formatter"),
        CONTAINER("container type"),
        GLOBAL_FLAG("global flag"),
        DATA_ACTION("data action"),
        EVENT("event"),
        PREPROCESSOR("preprocessor");

        private final String label;

        Kind(String label) {
            this.label = label;
        }

        /**
         * Returns the human-readable name of this kind, for log lines.
         *
         * @return the label, e.g. {@code "object type"}.
         */
        @NotNull
        public String getLabel() {
            return label;
        }
    }

    private static final Map<Kind, Map<String, AddonOwner>> owners = new EnumMap<>(Kind.class);

    private AddonOwnership() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Claims a component for an owner, reporting an override when one displaces another owner's
     * registration.
     *
     * @param kind  the kind of component.
     * @param key   the registry key, exactly as the registry stores it.
     * @param owner the owner claiming it.
     */
    public static void claim(@NotNull Kind kind, @NotNull String key, @NotNull AddonOwner owner) {
        Map<String, AddonOwner> byKey = owners.computeIfAbsent(kind, k -> new HashMap<>());
        AddonOwner previous = byKey.put(key, owner);

        if (previous == null || previous.equals(owner)) {
            return;
        }
        CorexLogger.warn("<yellow>" + owner.name() + "</yellow> overrides the " + kind.getLabel()
                + " '<yellow>" + key + "</yellow>', previously provided by " + previous.label() + ".");
    }

    /**
     * Returns who provided a component.
     *
     * @param kind the kind of component.
     * @param key  the registry key.
     * @return the owner, or {@code null} when nothing was ever claimed under that key.
     */
    @Nullable
    public static AddonOwner ownerOf(@NotNull Kind kind, @NotNull String key) {
        Map<String, AddonOwner> byKey = owners.get(kind);
        return byKey != null ? byKey.get(key) : null;
    }

    /**
     * Returns a sentence naming the addon behind a component, for an error message.
     *
     * @param kind the kind of component.
     * @param key  the registry key.
     * @return the note, or {@code null} when the component is Corex's own or unknown.
     */
    @Nullable
    public static String describe(@NotNull Kind kind, @NotNull String key) {
        AddonOwner owner = ownerOf(kind, key);
        if (owner == null || owner.isCore()) {
            return null;
        }
        return "The " + kind.getLabel() + " '" + key + "' is provided by " + owner.label() + ".";
    }

    /**
     * Forgets every claim, so a fresh load of Corex starts from an empty index.
     */
    public static void reset() {
        owners.clear();
    }

    /**
     * Counts what an owner has claimed, for the startup summary.
     *
     * @param owner the owner to count for.
     * @param kind  the kind of component.
     * @return how many keys of that kind the owner currently holds.
     */
    public static int count(@NotNull AddonOwner owner, @NotNull Kind kind) {
        Map<String, AddonOwner> byKey = owners.get(kind);
        if (byKey == null) {
            return 0;
        }
        int total = 0;
        for (AddonOwner value : byKey.values()) {
            if (value.equals(owner)) total++;
        }
        return total;
    }
}
