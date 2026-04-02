package dev.corexmc.corex.api.tags;

import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.ApiStatus.OverrideOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * Represents a global formatting tag within the Corex engine.
 * <p>
 * Formatters are "utility" base tags that usually return constant symbols,
 * colors, or simple transformations (e.g., {@code <n>}, {@code <sp>}, {@code <&char[65]>}).
 * <p>
 * <b>Optimization:</b> If a formatter is called without dynamic parameters,
 * the Corex Compiler will pre-calculate its value at script load time
 * and store it as a static string.
 */
public interface AbstractFormatter {

    /**
     * Gets the primary name of the formatting tag.
     * This is what users will type inside the brackets (e.g., "n" for {@code <n>}).
     *
     * @return the unique formatter name.
     */
    @NotNull
    @OverrideOnly
    @AvailableSince("1.0.0")
    String getName();

    /**
     * Returns a list of alternative names (aliases) for this formatter.
     * <p>
     * Example: {@code <n>} might have aliases {@code <newline>} or {@code <nl>}.
     *
     * @return an unmodifiable list of lowercase aliases.
     */
    @NotNull
    @OverrideOnly
    @Unmodifiable
    @AvailableSince("1.0.0")
    default List<String> getAlias() {
        return List.of(getName());
    }

    /**
     * Processes the formatter and returns the resulting tag.
     * <p>
     * This method is called when the engine encounters the formatter in a script.
     * It can use the {@link Attribute} to read parameters (e.g., {@code <&char[65]>}).
     *
     * @param attribute the current attribute context.
     * @return the resulting {@link AbstractTag}.
     *
     * @see Attribute#getParam()
     */
    @NotNull
    @OverrideOnly
    @AvailableSince("1.0.0")
    AbstractTag parse(@NotNull Attribute attribute);

    /**
     * Provides a sample parameter string for automated testing.
     * <p>
     * Used by the Corex Auto-Testing framework to verify that the formatter
     * doesn't throw exceptions when given valid input.
     * <p>
     * Example: for a {@code <&char[]>} formatter, this might return {@code "65"}.
     *
     * @return a sample parameter string, or {@code null} if no parameter is needed.
     */
    @Nullable
    @OverrideOnly
    @AvailableSince("1.0.0")
    default String getTestParam() {
        return null;
    }
}