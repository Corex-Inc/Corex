package dev.corexmc.corex.api.tags;

import dev.corexmc.corex.api.processors.TagProcessor;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a base object within the Corex scripting engine.
 * Every object (Player, Location, Material, etc.) must implement this interface
 * to be processed by the Tag Engine.
 */
public interface AbstractTag {

    /**
     * Returns the unique string representation of the object.
     * This value is used when the tag is fully parsed and needs to be output as text.
     * <p>
     * Example: "p@550e8400-e29b-41d4-a716-446655440000" or "stone"
     *
     * @return a non-null string identifying this object.
     */
    @NotNull
    @OverrideOnly
    @AvailableSince("1.0.0")
    String identify();

    /**
     * Gets the short prefix identifier for this object type.
     * <p>
     * Examples: 'p' for PlayerTag, 'l' for LocationTag, 'el' for ElementTag.
     *
     * @return the prefix string.
     */
    @NotNull
    @OverrideOnly
    @AvailableSince("1.0.0")
    String getPrefix();

    /**
     * Sets a custom prefix for this object instance.
     * Primarily used for internal engine organization or debugging.
     *
     * @param prefix the new prefix to use.
     * @return the same AbstractTag instance (Fluent API).
     */
    @NotNull
    @Contract("_ -> this")
    @AvailableSince("1.0.0")
    AbstractTag setPrefix(@NotNull String prefix);

    /**
     * Processes a sub-tag (attribute) of this object.
     * <p>
     * When the parser encounters a dot (e.g., <player.name>), it calls this method
     * on the Player object with an {@link Attribute} containing "name".
     * <p>
     * <b>Crucial:</b> This method is the entry point for the {@link TagProcessor}.
     *
     * @param attribute the current attribute piece being processed.
     * @return the resulting {@link AbstractTag} for the next piece of the chain,
     *         or {@code null} if the attribute is invalid/not found.
     */
    @Nullable
    @OverrideOnly
    @Contract(pure = true)
    @AvailableSince("1.0.0")
    AbstractTag getAttribute(@NotNull Attribute attribute);

}