package dev.corexinc.corex.api.addons;

import dev.corexinc.corex.engine.CorexRegistry;
import dev.corexinc.corex.engine.addons.AddonManager;
import dev.corexinc.corex.engine.addons.AddonOwner;
import dev.corexinc.corex.engine.addons.AddonResolver;
import dev.corexinc.corex.engine.scripts.ScriptManager;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.NotNull;

/**
 * The one way an addon puts anything into Corex.
 *
 * <p>Opening a registrar claims the registries for one addon, so everything registered until it
 * closes is attributed to that addon: commands, tags, sub-tags added to Corex's own objects,
 * mechanisms, events, containers, formatters, global flags and data actions. The registries stay
 * shut to everyone else, and an error thrown out of an addon component names the addon rather
 * than Corex.</p>
 *
 * <pre>{@code
 * CorexRegistrar.open(this)
 *         .register(MyCommand.class, MyTag.class)
 *         .close();
 * }</pre>
 *
 * <p>It is an {@link AutoCloseable}, so try-with-resources works too and is the safer form when
 * the registration block can throw:</p>
 *
 * <pre>{@code
 * try (CorexRegistrar registrar = CorexRegistrar.open(this)) {
 *     registrar.register(MyCommand.class);
 * }
 * }</pre>
 *
 * <p>Only one registrar may be open at a time. Leaving one open blocks every other addon, which
 * is why closing it is not optional.</p>
 *
 * @see AbstractCorexAddon
 * @since 1.0.0
 */
@AvailableSince("1.0.0")
public final class CorexRegistrar implements AutoCloseable {

    private final AddonOwner owner;
    private boolean closed;

    private CorexRegistrar(AddonOwner owner) {
        this.owner = owner;
    }

    /**
     * Opens the registries for an addon.
     *
     * @param addon the addon, which must be the plugin main class.
     * @return the registrar to pour components into, and to close afterwards.
     * @throws IllegalStateException if the platform has no addon support, the object is not a
     *                               plugin, another registrar is still open, or Corex has already
     *                               compiled its scripts.
     */
    @NotNull
    @AvailableSince("1.0.0")
    public static CorexRegistrar open(@NotNull AbstractCorexAddon addon) {
        AddonResolver resolver = AddonResolver.get();
        if (resolver == null) {
            throw new IllegalStateException("Corex is not running as a plugin here, so it has no "
                    + "addons to register.");
        }

        AddonOwner owner = resolver.describe(addon);
        if (owner == null) {
            throw new IllegalStateException(addon.getClass().getName() + " implements "
                    + "AbstractCorexAddon but is not a plugin. Implement it on the plugin main "
                    + "class, and pass that instance here.");
        }

        AddonManager.openScope(owner);
        return new CorexRegistrar(owner);
    }

    /**
     * Registers components, dispatching each by what it implements the same way Corex registers
     * its own.
     *
     * <p>A tag class must expose the static {@code register()} method every Corex tag has; the
     * other kinds are instantiated through their no-argument constructor. A class that fits none
     * of the component interfaces is reported and skipped, and so is one whose registration
     * throws, so a single broken component cannot take the rest of the addon down.</p>
     *
     * @param components the component classes.
     * @return {@code this}, so the call can be chained into {@link #close()}.
     * @throws IllegalStateException if the registrar has already been closed.
     */
    @NotNull
    @AvailableSince("1.0.0")
    public CorexRegistrar register(@NotNull Class<?>... components) {
        if (closed) {
            throw new IllegalStateException(owner.fullName() + " kept using its registrar after "
                    + "closing it. Open a new one, or close it only once everything is in.");
        }

        CorexRegistry registry = ScriptManager.getRegistry();
        if (registry == null) {
            throw new IllegalStateException("Corex has not built its registry yet. Register from "
                    + "the plugin onLoad(), not from a static initializer.");
        }

        registry.register(components);
        return this;
    }

    /**
     * Closes the registrar, releasing the registries for the next addon.
     */
    @Override
    @AvailableSince("1.0.0")
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        AddonManager.closeScope(owner);
    }
}
