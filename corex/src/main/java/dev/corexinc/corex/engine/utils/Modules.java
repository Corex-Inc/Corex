package dev.corexinc.corex.engine.utils;

import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.NotNull;

/**
 * The Corex module (platform) a component belongs to.
 *
 * <p>Corex ships one engine behind several front-ends: the Paper plugin, its regionized forks
 * Folia and Canvas, and the Velocity proxy port. Most of the environment is shared, but some
 * components only make sense on one of them, for example a sub-tag reading the server tick
 * counter, which a proxy does not have. Such components declare their module and are simply
 * not registered on the others.</p>
 *
 * <p>The modules nest: Folia and Canvas are Paper servers, and Canvas is a Folia-style
 * regionized server, so {@code PAPER} covers all three and {@code FOLIA} covers Folia and
 * Canvas. {@code CANVAS} and {@code VELOCITY} cover only themselves.</p>
 *
 * <p>The running module is set once at startup, before anything is registered:</p>
 * <pre>{@code
 * Modules.setCurrent(Modules.VELOCITY);
 * }</pre>
 *
 * @see dev.corexinc.corex.api.processors.TagProcessor.TagRegistration#setAvailableFor(Modules...)
 * @since 1.0.0
 */
@AvailableSince("1.0.0")
public enum Modules {

    /** The Paper plugin, including the Folia and Canvas forks. */
    PAPER,

    /** The Folia fork, including Canvas. */
    FOLIA,

    /** The Canvas fork. */
    CANVAS,

    /** The Velocity proxy port. */
    VELOCITY;

    private static Modules current = PAPER;

    /**
     * Returns whether this module covers the given one.
     *
     * <p>{@code PAPER.includes(CANVAS)} is {@code true} because Canvas is a Paper server,
     * while {@code CANVAS.includes(PAPER)} is {@code false}.</p>
     *
     * @param module the module to test.
     * @return {@code true} if a component declared for this module runs on {@code module}.
     */
    @AvailableSince("1.0.0")
    public boolean includes(@NotNull Modules module) {
        return switch (this) {
            case PAPER -> module == PAPER || module == FOLIA || module == CANVAS;
            case FOLIA -> module == FOLIA || module == CANVAS;
            case CANVAS -> module == CANVAS;
            case VELOCITY -> module == VELOCITY;
        };
    }

    /**
     * Sets the module Corex is currently running as.
     * <p>Call this before registering anything; defaults to {@link #PAPER}.</p>
     *
     * @param module the running module.
     */
    @AvailableSince("1.0.0")
    public static void setCurrent(@NotNull Modules module) {
        current = module;
    }

    /**
     * Gets the module Corex is currently running as.
     *
     * @return the running module, {@link #PAPER} unless {@link #setCurrent} says otherwise.
     */
    @NotNull
    @AvailableSince("1.0.0")
    public static Modules getCurrent() {
        return current;
    }
}
