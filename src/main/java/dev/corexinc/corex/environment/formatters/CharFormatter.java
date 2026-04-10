package dev.corexinc.corex.environment.formatters;

import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;

/* @doc formatter
 *
 * @Name &char
 * @ArgRequired
 * @Description
 * Converts a given integer representing a character's Unicode (or ASCII) code point into its corresponding character string.
 * This formatter is useful for dynamically generating characters based on their numerical codes.
 *
 * @Usage
 * // Returns "A", as 65 is the ASCII code for 'A'.
 * - narrate "Character for 65 is: <&char[65]>"
 *
 * // Returns "!", as 33 is the ASCII code for '!'.
 * - narrate "The symbol is: <&char[33]>"
 *
 * @Implements &chr[<character>]
 */
public class CharFormatter implements AbstractFormatter {
    @Override public @NonNull String getName() { return "&char"; }

    @Override
    public @NonNull AbstractTag parse(@NonNull Attribute attribute) {
        if (!attribute.hasParam()) return new ElementTag("?");

        try {
            int code = Integer.parseInt(attribute.getParam());
            return new ElementTag(String.valueOf((char) code));
        } catch (Exception e) {
            return null;
        }
    }

    @Override public String getTestParam() { return "65"; }
}