package dev.corexinc.corex.engine.addons;

import dev.corexinc.corex.api.addons.AbstractCorexAddon;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Bridges the addon system to the running platform.
 *
 * <p>The engine knows what an owner is but not what a plugin is, so the platform layer supplies
 * one of these at startup, the same way it supplies a
 * {@link dev.corexinc.corex.engine.utils.SchedulerAdapter}:</p>
 *
 * <pre>{@code
 * AddonResolver.set(new BukkitAddonResolver());
 * }</pre>
 *
 * <p>Without a resolver the addon system stays closed: Corex registers its own components as
 * usual, and any attempt to open a registrar is refused.</p>
 *
 * @since 1.0.0
 */
@AvailableSince("1.0.0")
public abstract class AddonResolver {

    private static AddonResolver instance;

    /**
     * Installs the resolver for this platform. Call once, before anything registers.
     *
     * @param resolver the platform's resolver.
     */
    public static void set(@NotNull AddonResolver resolver) {
        instance = resolver;
    }

    /**
     * Returns the installed resolver, or {@code null} when the platform has no addon support.
     *
     * @return the resolver, or {@code null}.
     */
    @Nullable
    public static AddonResolver get() {
        return instance;
    }

    /**
     * Describes an addon that is asking to register something.
     *
     * @param addon the addon instance, normally the plugin's main class.
     * @return its owner, or {@code null} when the object is not a plugin this platform knows.
     */
    @Nullable
    public abstract AddonOwner describe(@NotNull AbstractCorexAddon addon);

    /**
     * Names the moment an addon is supposed to register on this platform, for the messages that
     * turn a mistimed registration down.
     *
     * @return a phrase that fits after "Register from", e.g. {@code "the plugin onLoad()"}.
     */
    @NotNull
    public abstract String registrationHint();

    /**
     * Describes whoever provides a class, used to identify a caller that registers outside of a
     * registrar session.
     *
     * @param clazz the class to look up.
     * @return the owner of the plugin providing the class, or {@code null} when the class does
     *         not come from a plugin at all (Corex itself, the server, the JDK).
     */
    @Nullable
    public abstract AddonOwner ownerOfClass(@NotNull Class<?> clazz);
}
