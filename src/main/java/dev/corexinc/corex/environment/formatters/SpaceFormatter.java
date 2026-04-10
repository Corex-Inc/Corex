package dev.corexinc.corex.environment.formatters;

import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;

import java.util.List;

/* @doc formatter
 *
 * @Name sp
 * @Aliases &space, &sp
 * @NoArg
 * @Description
 * Inserts a single space character into the text.
 * This formatter is useful for explicitly adding spaces, especially when dealing with complex tag parsing or concatenated strings where a literal space might be consumed.
 *
 * @Usage
 * // Narrates "Hello World" with a guaranteed space between.
 * - narrate "Hello<sp>World"
 *
 * @Implements &sp
 */
public class SpaceFormatter implements AbstractFormatter {
    private static final AbstractTag INSTANCE = new ElementTag(" ");

    @Override public @NonNull String getName() { return "sp"; }
    @Override public @NonNull List<String> getAlias() { return List.of("&sp", "space"); }

    @Override
    public @NonNull AbstractTag parse(@NonNull Attribute attribute) {
        return INSTANCE;
    }
}