package dev.corexinc.corex.environment.formatters;

import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.environment.tags.core.ColorTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.MarkupTag;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

/* @doc formatter
 *
 * @Name &color
 * @Syntax &color[<color>]
 * @Description
 * Applies a color or text style to the subsequent text.
 * This formatter supports hexadecimal color codes (e.g., `#RRGGBB` or `RRGGBB`)
 * for granular color control, or legacy Minecraft color and formatting codes via its aliases.
 * When using a hex code, it will be converted to the Minecraft extended format.
 *
 * @Implements &color[<color>]
 */

/* @doc formatter
 *
 * @Name &0
 * @Description
 * @NoArg
 * @Description
 * Applies the 'Black' color to the text.
 *
 * @Implements &0
 */

/* @doc formatter
 *
 * @Name &1
 * @Description
 * @NoArg
 * @Description
 * Applies the 'Dark Blue' color to the text.
 *
 * @Implements &1
 */

/* @doc formatter
 *
 * @Name &2
 * @Description
 * @NoArg
 * @Description
 * Applies the 'Dark Green' color to the text.
 *
 * @Implements &2
 */

/* @doc formatter
 *
 * @Name &3
 * @Description
 * @NoArg
 * @Description
 * Applies the 'Dark Aqua' (Cyan) color to the text.
 *
 * @Implements &3
 */

/* @doc formatter
 *
 * @Name &4
 * @Description
 * @NoArg
 * @Description
 * Applies the 'Dark Red' color to the text.
 *
 * @Implements &4
 */

/* @doc formatter
 *
 * @Name &5
 * @Description
 * @NoArg
 * @Description
 * Applies the 'Dark Purple' (Indigo) color to the text.
 *
 * @Implements &5
 */

/* @doc formatter
 *
 * @Name &6
 * @Description
 * @NoArg
 * @Description
 * Applies the 'Gold' color to the text.
 *
 * @Implements &6
 */

/* @doc formatter
 *
 * @Name &7
 * @Description
 * @NoArg
 * @Description
 * Applies the 'Gray' color to the text.
 *
 * @Implements &7
 */

/* @doc formatter
 *
 * @Name &8
 * @Description
 * @NoArg
 * @Description
 * Applies the 'Dark Gray' color to the text.
 *
 * @Implements &8
 */

/* @doc formatter
 *
 * @Name &9
 * @Description
 * @NoArg
 * @Description
 * Applies the 'Blue' color to the text.
 *
 * @Implements &9
 */

/* @doc formatter
 *
 * @Name &a
 * @Description
 * @NoArg
 * @Description
 * Applies the 'Green' color to the text.
 *
 * @Implements &a
 */

/* @doc formatter
 *
 * @Name &b
 * @Description
 * @NoArg
 * @Description
 * Applies the 'Aqua' (Light Cyan) color to the text.
 *
 * @Implements &b
 */

/* @doc formatter
 *
 * @Name &c
 * @Description
 * @NoArg
 * @Description
 * Applies the 'Red' color to the text.
 *
 * @Implements &c
 */

/* @doc formatter
 *
 * @Name &d
 * @Description
 * @NoArg
 * @Description
 * Applies the 'Light Purple' (Pink) color to the text.
 *
 * @Implements &d
 */

/* @doc formatter
 *
 * @Name &e
 * @Description
 * @NoArg
 * @Description
 * Applies the 'Yellow' color to the text.
 *
 * @Implements &e
 */

/* @doc formatter
 *
 * @Name &f
 * @Description
 * @NoArg
 * @Description
 * Applies the 'White' color to the text.
 *
 * @Implements &f
 */

/* @doc formatter
 *
 * @Name &l
 * @Description
 * @NoArg
 * @Description
 * Applies 'Bold' formatting to the text.
 *
 * @Implements &l
 */

/* @doc formatter
 *
 * @Name &m
 * @Description
 * @NoArg
 * @Description
 * Applies 'Strikethrough' formatting to the text.
 *
 * @Implements &m
 */

/* @doc formatter
 *
 * @Name &n
 * @Description
 * @NoArg
 * @Description
 * Applies 'Underline' formatting to the text.
 *
 * @Implements &n
 */

/* @doc formatter
 *
 * @Name &o
 * @Description
 * @NoArg
 * @Description
 * Applies 'Italic' formatting to the text.
 *
 * @Implements &o
 */

/* @doc formatter
 *
 * @Name &r
 * @Description
 * @NoArg
 * @Description
 * Resets all previous color and formatting styles to default.
 *
 * @Implements &r
 */
public class ColorFormatter implements AbstractFormatter {

    private static final Map<Character, MarkupTag> LEGACY_MARKUP = Map.ofEntries(
            Map.entry('0', new MarkupTag("<black>")),
            Map.entry('1', new MarkupTag("<dark_blue>")),
            Map.entry('2', new MarkupTag("<dark_green>")),
            Map.entry('3', new MarkupTag("<dark_aqua>")),
            Map.entry('4', new MarkupTag("<dark_red>")),
            Map.entry('5', new MarkupTag("<dark_purple>")),
            Map.entry('6', new MarkupTag("<gold>")),
            Map.entry('7', new MarkupTag("<gray>")),
            Map.entry('8', new MarkupTag("<dark_gray>")),
            Map.entry('9', new MarkupTag("<blue>")),
            Map.entry('a', new MarkupTag("<green>")),
            Map.entry('b', new MarkupTag("<aqua>")),
            Map.entry('c', new MarkupTag("<red>")),
            Map.entry('d', new MarkupTag("<light_purple>")),
            Map.entry('e', new MarkupTag("<yellow>")),
            Map.entry('f', new MarkupTag("<white>")),
            Map.entry('l', new MarkupTag("<bold>")),
            Map.entry('m', new MarkupTag("<strikethrough>")),
            Map.entry('n', new MarkupTag("<underlined>")),
            Map.entry('o', new MarkupTag("<italic>")),
            Map.entry('r', new MarkupTag("<reset>"))
    );

    private static final ElementTag EMPTY = new ElementTag("");

    @Override
    public @NonNull String getName() {
        return "&color";
    }

    @Override
    public @NonNull List<String> getAlias() {
        return List.of("&0","&1","&2","&3",
                "&4","&5","&6","&7",
                "&8","&9","&a","&b",
                "&c","&d","&e","&f",
                "&l","&m","&n","&o","&r");
    }

    @Override
    public @NonNull AbstractTag parse(@NonNull Attribute attribute) {
        String name = attribute.getName();

        if (name.length() == 2 && name.startsWith("&")) {
            MarkupTag markup = LEGACY_MARKUP.get(name.charAt(1));
            return markup != null ? markup : EMPTY;
        }

        if (attribute.hasParam()) {
            AbstractTag object = attribute.getParamObject();
            String hex;
            if (object instanceof ColorTag colorTag) {
                hex = colorTag.getHex(false);
            } else {
                hex = object.identify();
            }
            if (hex.startsWith("#")) hex = hex.substring(1);

            if (hex.length() == 6) {
                return new MarkupTag("<color:#" + hex.toLowerCase() + ">");
            }
        }

        return EMPTY;
    }
}