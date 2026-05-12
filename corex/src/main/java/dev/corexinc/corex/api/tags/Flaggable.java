package dev.corexinc.corex.api.tags;

import dev.corexinc.corex.engine.flags.trackers.AbstractFlagTracker;
import org.jetbrains.annotations.ApiStatus.*;

/**
 * Indicates that an {@link AbstractTag} can store and retrieve persistent data (flags).
 * <p>
 * Implementing this interface allows the object to be used with the {@code - flag} command
 * and provides access to the {@code .flag[<name>]} and {@code .hasFlag[<name>]} tags.
 *
 * @since 1.0.0
 */
public interface Flaggable {

    /**
     * Returns the flag tracker associated with this object.
     * <p>
     * The tracker is responsible for the physical storage logic (e.g., SQL, PDC, or Memory).
     *
     * @return a non-null {@link AbstractFlagTracker} instance.
     */
    @OverrideOnly
    @AvailableSince("1.0.0")
    AbstractFlagTracker getFlagTracker();
}