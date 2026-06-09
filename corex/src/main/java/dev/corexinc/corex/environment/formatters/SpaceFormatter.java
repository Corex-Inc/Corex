package dev.corexinc.corex.environment.formatters;

import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;

import java.util.List;

/* @doc formatter
 *
 * @Name &nbsp
 * @Aliases nbsp
 * @NoArg
 * @Description
 * Returns a non-breaking space character (U+00A0).
 * Unlike a regular space, a non-breaking space prevents line breaks at its position
 * and is not stripped by parsers that trim whitespace.
 *
 * @Usage
 * - narrate <&nbsp>
 */

/* @doc formatter
 *
 * @Name &sp
 * @Aliases &space, sp
 * @NoArg
 * @Description
 * Inserts a single space character into the text.
 * This formatter is useful for explicitly adding spaces, especially when dealing with complex tag parsing or concatenated strings where a literal space might be consumed.
 *
 * @Usage
 * // Narrates "Hello World" with a guaranteed space between.
 * - narrate "Hello<sp>World"
 *
 * @Implements &sp, &nbsp
 */
public class SpaceFormatter implements AbstractFormatter {
    private static final AbstractTag INSTANCE = new ElementTag(" ");
    private static final AbstractTag NB_INSTANCE = new ElementTag("\u00A0");

    @Override
    public @NonNull String getName() {
        return "sp";
    }

    @Override
    public @NonNull List<String> getAlias() {
        return List.of("&sp", "space", "nbsp", "&nbsp");
    }

    @Override
    public @NonNull AbstractTag parse(@NonNull Attribute attribute) {
        if (attribute.getName().contains("nbsp")) return NB_INSTANCE;
        return INSTANCE;
    }
}