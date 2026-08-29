package dev.corexinc.corex.api.tags;

import dev.corexinc.corex.engine.flags.trackers.AbstractFlagTracker;
import org.jetbrains.annotations.ApiStatus.*;
import org.jetbrains.annotations.Nullable;

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
     * <p>
     * May return {@code null} for a tag that no longer points at anything storable, such as a
     * ServerTag naming a backend that is not registered. Callers must handle that: the
     * {@code .flag[<name>]} tag resolves to null, {@code .hasFlag[<name>]} to false, and the
     * {@code - flag} command reports an error.
     *
     * @return the {@link AbstractFlagTracker} for this object, or {@code null} if it has none.
     */
    @Nullable
    @OverrideOnly
    @AvailableSince("1.0.0")
    AbstractFlagTracker getFlagTracker();
}