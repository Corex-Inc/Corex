package dev.corexinc.corex.api.tags;

import dev.corexinc.corex.api.processors.MechanismProcessor;
import org.jetbrains.annotations.ApiStatus.*;
import org.jetbrains.annotations.NotNull;

/**
 * Represents an {@link AbstractTag} that can be modified via the mechanism system.
 * <p>
 * Mechanisms are "setters" for tag objects, used by the {@code - adjust} command
 * or the {@code .with[mech=value]} tag.
 *
 * @since 1.0.0
 */
public interface Adjustable extends AbstractTag {

    /**
     * Creates a deep copy of this object.
     * <p>
     * Since most tags should behave as value-types, this is used to ensure that
     * applying a mechanism to an object does not unintentionally modify the original
     * instance (especially when retrieved from a cache).
     *
     * @return a duplicated instance of this tag.
     */
    @NotNull
    @OverrideOnly
    @AvailableSince("1.0.0")
    Adjustable duplicate();

    /**
     * Applies a specific mechanism to this object.
     * <p>
     * Note: In most cases, this should be called on a {@link #duplicate()} to maintain immutability.
     *
     * @param mechanism the name of the mechanism to apply.
     * @param value     the value to set (can be an ElementTag, ListTag, etc.).
     * @return the modified object (usually {@code this}).
     */
    @NotNull
    @OverrideOnly
    @AvailableSince("1.0.0")
    AbstractTag applyMechanism(@NotNull String mechanism, @NotNull AbstractTag value);

    /**
     * Gets the {@link MechanismProcessor} responsible for handling modifications for this object.
     * <p>
     * This processor stores the mapping of mechanism names to their respective logic lambdas.
     *
     * @return the mechanism processor instance.
     */
    @NotNull
    @OverrideOnly
    @AvailableSince("1.0.0")
    MechanismProcessor<? extends AbstractTag> getMechanismProcessor();
}