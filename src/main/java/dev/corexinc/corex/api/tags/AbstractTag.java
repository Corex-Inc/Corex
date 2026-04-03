package dev.corexinc.corex.api.tags;

import dev.corexinc.corex.api.processors.TagProcessor;
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

    /**
     * Provides a sample raw value that can be used to recreate this object type.
     * <p>
     * This value is used by the automated testing framework to verify all registered
     * sub-tags (attributes) of this object class.
     * <p>
     * <b>Examples:</b>
     * <ul>
     *   <li>PlayerTag: {@code "p@00000000-0000-0000-0000-000000000000"}</li>
     *   <li>ElementTag: {@code "hello_world"}</li>
     *   <li>LocationTag: {@code "l@100,64,100,world"}</li>
     * </ul>
     *
     * @return a raw string representing a valid instance of this tag.
     */
    @Nullable
    @OverrideOnly
    @AvailableSince("1.0.0")
    String getTestValue();

    /**
     * Gets the {@link TagProcessor} responsible for handling sub-tags for this object.
     * <p>
     * The processor contains the mapping of attribute names to their respective
     * logic lambdas. It is used by both the Runtime Engine and the Compiler
     * for optimization and static analysis.
     *
     * @return the processor instance for this tag type.
     */
    @NotNull
    @OverrideOnly
    @AvailableSince("1.0.0")
    TagProcessor<? extends AbstractTag> getProcessor();
}