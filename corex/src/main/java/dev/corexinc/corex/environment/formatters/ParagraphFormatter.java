package dev.corexinc.corex.environment.formatters;

import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.jspecify.annotations.NonNull;

import java.util.List;

/* @doc formatter
 *
 * @Name &pg
 * @Aliases &ss
 * @NoArg
 * @Description
 * Returns the internal Minecraft color/formatting prefix symbol: §  (section sign, U+00A7).
 * Useful when you need to produce a literal § in output without it being interpreted
 * as a color code by the script parser.
 *
 * @Usage
 * - narrate <&ss>
 *
 * @Implements &ss
 */
public class ParagraphFormatter implements AbstractFormatter {

    private static final AbstractTag INSTANCE = new ElementTag("§");

    @Override
    public @NonNull String getName() {
        return "&pg";
    }

    @Override
    public @NotNull @Unmodifiable List<String> getAlias() {
        return List.of("&ss");
    }

    @Override
    public @NonNull AbstractTag parse(@NonNull Attribute attribute) {
        return INSTANCE;
    }
}