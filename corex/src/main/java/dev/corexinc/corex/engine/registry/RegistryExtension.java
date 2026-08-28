package dev.corexinc.corex.engine.registry;

import dev.corexinc.corex.engine.CorexRegistry;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.NotNull;

/**
 * Handles a component kind that lives outside the engine.
 *
 * <p>{@link CorexRegistry} dispatches by interface, but not every component interface belongs to
 * the engine: events extend a platform listener, so the engine cannot name their type. The
 * platform layer installs an extension instead, and {@code register} keeps working as one entry
 * point for addons rather than making them remember which registry each kind goes to.</p>
 *
 * @since 1.0.0
 */
@FunctionalInterface
@AvailableSince("1.0.0")
public interface RegistryExtension {

    /**
     * Registers a component if this extension knows the kind.
     *
     * @param component the class Corex could not dispatch itself.
     * @return {@code true} when it was handled, {@code false} to let the next extension try.
     */
    boolean tryRegister(@NotNull Class<?> component);
}
