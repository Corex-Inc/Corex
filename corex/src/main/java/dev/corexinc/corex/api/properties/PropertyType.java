package dev.corexinc.corex.api.properties;

import dev.corexinc.corex.api.tags.AbstractTag;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A two-way codec between a script value ({@link AbstractTag}) and a plain Java value.
 *
 * <p>A PropertyType is what lets a property be declared once and still get full input validation:
 * the property says "I hold a {@code Display.Billboard}", and the type handles parsing whatever
 * the script author typed, rejecting garbage, and converting the value back into a tag when it is
 * read. Handlers registered through {@link PropertyRegistrar} therefore receive an already-parsed,
 * already-validated value and never need their own {@code instanceof} / {@code isDouble} checks.</p>
 *
 * <p>Use the ready-made instances in {@link PropertyTypes} - {@code BOOLEAN}, {@code TICKS},
 * {@link PropertyTypes#enumOf(Class)}, and so on. Write a custom implementation only for a value
 * shape none of those cover.</p>
 *
 * @param <V> the Java value this type parses to.
 * @since 1.0.0
 */
@AvailableSince("1.0.0")
public interface PropertyType<V> {

    /**
     * Converts a script value into this type's Java value.
     *
     * @param input the raw tag supplied by the script.
     * @return the parsed value, or {@code null} if the input is not valid for this type.
     *         Returning {@code null} makes the owning mechanism report an error and skip the write.
     */
    @Nullable
    V parse(@NotNull AbstractTag input);

    /**
     * Converts a Java value back into the tag a script reads.
     */
    @NotNull
    AbstractTag write(@NotNull V value);

    /**
     * The tag class {@link #write} produces. Used to declare the return type of generated tags.
     */
    @NotNull
    Class<? extends AbstractTag> tagClass();

    /**
     * Human-readable name of the accepted input, used in error messages
     * (e.g. {@code "a boolean"}, {@code "one of: FIXED, VERTICAL, HORIZONTAL, CENTER"}).
     */
    @NotNull
    String describeInput();
}
