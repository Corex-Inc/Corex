package dev.corexinc.corex.engine.addons;

import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.NotNull;

/**
 * Whoever registered a component into Corex.
 *
 * <p>Every command, tag, event and mechanism in the engine belongs to exactly one owner: Corex
 * itself, or one addon. The owner is what lets an error message say where a broken tag came
 * from instead of leaving the server owner guessing which plugin to report the bug to.</p>
 *
 * <p>Owners are produced by {@link AddonResolver} from the running platform, so the engine never
 * has to know what a "plugin" is.</p>
 *
 * @param name    the display name, as the platform reports it.
 * @param version the version string, empty when the platform has none.
 * @param origin  what kind of code this is.
 * @since 1.0.0
 */
@AvailableSince("1.0.0")
public record AddonOwner(@NotNull String name, @NotNull String version, @NotNull Origin origin) {

    /** Where a registration came from. */
    public enum Origin {

        /** Corex's own environment loader. */
        CORE,

        /** A plugin that implements {@code AbstractCorexAddon}. */
        ADDON,

        /** A plugin that does not implement {@code AbstractCorexAddon} and may not register. */
        FOREIGN
    }

    /** The engine itself. Everything in {@code EnvironmentLoader} is owned by this. */
    public static final AddonOwner CORE = new AddonOwner("Corex", "", Origin.CORE);

    /**
     * Returns whether this is Corex itself rather than an addon.
     *
     * @return {@code true} for the built-in owner.
     */
    public boolean isCore() {
        return origin == Origin.CORE;
    }

    /**
     * Returns whether this owner is allowed to register anything.
     *
     * @return {@code true} for Corex and for marked addons.
     */
    public boolean mayRegister() {
        return origin != Origin.FOREIGN;
    }

    /**
     * Returns the name to print in a log line, already phrased for a sentence.
     *
     * @return {@code "Corex"} or {@code "addon 'Name'"}.
     */
    @NotNull
    public String label() {
        return origin == Origin.CORE ? "Corex" : "addon '" + name + "'";
    }

    /**
     * Returns the name with its version, for the startup summary.
     *
     * @return {@code "Name v1.0"}, or just the name when the platform reports no version.
     */
    @NotNull
    public String fullName() {
        return version.isEmpty() ? name : name + " v" + version;
    }
}
