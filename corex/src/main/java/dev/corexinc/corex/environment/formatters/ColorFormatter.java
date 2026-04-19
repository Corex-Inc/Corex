package dev.corexinc.corex.environment.formatters;

import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.environment.tags.core.ColorTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;

import java.util.List;

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
 * Applies the 'Black' color to the text.
 *
 * @Implements &0
 */

/* @doc formatter
 *
 * @Name &1
 * @Description
 * @NoArg
 * Applies the 'Dark Blue' color to the text.
 *
 * @Implements &1
 */

/* @doc formatter
 *
 * @Name &2
 * @Description
 * @NoArg
 * Applies the 'Dark Green' color to the text.
 *
 * @Implements &2
 */

/* @doc formatter
 *
 * @Name &3
 * @Description
 * @NoArg
 * Applies the 'Dark Aqua' (Cyan) color to the text.
 *
 * @Implements &3
 */

/* @doc formatter
 *
 * @Name &4
 * @Description
 * @NoArg
 * Applies the 'Dark Red' color to the text.
 *
 * @Implements &4
 */

/* @doc formatter
 *
 * @Name &5
 * @Description
 * @NoArg
 * Applies the 'Dark Purple' (Indigo) color to the text.
 *
 * @Implements &5
 */

/* @doc formatter
 *
 * @Name &6
 * @Description
 * @NoArg
 * Applies the 'Gold' color to the text.
 *
 * @Implements &6
 */

/* @doc formatter
 *
 * @Name &7
 * @Description
 * @NoArg
 * Applies the 'Gray' color to the text.
 *
 * @Implements &7
 */

/* @doc formatter
 *
 * @Name &8
 * @Description
 * @NoArg
 * Applies the 'Dark Gray' color to the text.
 *
 * @Implements &8
 */

/* @doc formatter
 *
 * @Name &9
 * @Description
 * @NoArg
 * Applies the 'Blue' color to the text.
 *
 * @Implements &9
 */

/* @doc formatter
 *
 * @Name &a
 * @Description
 * @NoArg
 * Applies the 'Green' color to the text.
 *
 * @Implements &a
 */

/* @doc formatter
 *
 * @Name &b
 * @Description
 * @NoArg
 * Applies the 'Aqua' (Light Cyan) color to the text.
 *
 * @Implements &b
 */

/* @doc formatter
 *
 * @Name &c
 * @Description
 * @NoArg
 * Applies the 'Red' color to the text.
 *
 * @Implements &c
 */

/* @doc formatter
 *
 * @Name &d
 * @Description
 * @NoArg
 * Applies the 'Light Purple' (Pink) color to the text.
 *
 * @Implements &d
 */

/* @doc formatter
 *
 * @Name &e
 * @Description
 * @NoArg
 * Applies the 'Yellow' color to the text.
 *
 * @Implements &e
 */

/* @doc formatter
 *
 * @Name &f
 * @Description
 * @NoArg
 * Applies the 'White' color to the text.
 *
 * @Implements &f
 */

/* @doc formatter
 *
 * @Name &l
 * @Description
 * @NoArg
 * Applies 'Bold' formatting to the text.
 *
 * @Implements &l
 */

/* @doc formatter
 *
 * @Name &m
 * @Description
 * @NoArg
 * Applies 'Strikethrough' formatting to the text.
 *
 * @Implements &m
 */

/* @doc formatter
 *
 * @Name &n
 * @Description
 * @NoArg
 * Applies 'Underline' formatting to the text.
 *
 * @Implements &n
 */

/* @doc formatter
 *
 * @Name &o
 * @Description
 * @NoArg
 * Applies 'Italic' formatting to the text.
 *
 * @Implements &o
 */

/* @doc formatter
 *
 * @Name &r
 * @Description
 * @NoArg
 * Resets all previous color and formatting styles to default.
 *
 * @Implements &r
 */
public class ColorFormatter implements AbstractFormatter {

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
    public @NonNull AbstractTag parse(Attribute attribute) {
        String name = attribute.getName();

        if (name.length() == 2 && name.startsWith("&")) {
            char code = name.charAt(1);
            return new ElementTag("§" + code);
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
                StringBuilder hexResult = new StringBuilder("§x");
                for (char c : hex.toCharArray()) {
                    hexResult.append('§').append(c);
                }
                return new ElementTag(hexResult.toString());
            }
        }

        return new ElementTag("");
    }
}