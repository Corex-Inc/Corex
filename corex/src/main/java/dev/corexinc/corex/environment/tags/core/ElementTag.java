package dev.corexinc.corex.environment.tags.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.utils.scripts.JsonHelper;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NonNull;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/* @doc object
 *
 * @Name ElementTag
 * @Prefix
 * @Format
 * Just the plain text of the element value, no prefix or formatting.
 *
 * @Description
 * ElementTags are simple objects that contain a simple bit of text.
 * Their main usage is within the replaceable tag system,
 * often times returned from the use of another tag that isn't returning a specific object type, such as a location or entity.
 * For example, <player.name> or <list[item1|item2|item3].join[, ]> will both return ElementTags.
 *
 * Pluses to the ElementTag system is the ability to utilize its tag attributes,
 * which can provide a range of functionality that should be familiar from any other programming language,
 * such as 'toUppercase', 'split', 'replace', 'contains', and many more.
 * See 'ElementTag.*' tags for more information.
 *
 * While information fetched from other tags resulting in an ElementTag is often times automatically handled,
 * it may be desirable to utilize element attributes from text/numbers/etc. that aren't already an element object.
 * To accomplish this, the standard 'element' tag base can be used for the creation of a new element.
 * For example: <element[This_is_a_test].toUppercase>
 * will result in the value 'THIS_IS_A_TEST'.
 *
 * Note that while other objects often return their object identifier (p@, li@, e@, etc.), elements usually do not (except special type-validation circumstances).
 */
public class ElementTag implements AbstractTag {

    private final String prefix;
    private final String element;

    public static final TagProcessor<ElementTag> TAG_PROCESSOR = new TagProcessor<>();

    public ElementTag(String string) {
        this.prefix = "el";
        if (string == null) {
            this.element = "";
        } else {
            this.element = string.toLowerCase().startsWith(prefix + "@") ? string.substring(3) : string;
        }
    }

    public ElementTag(Component component) {
        this.prefix = "el";
        this.element = Corex.SERIALIZER.serialize(component);
    }

    public ElementTag(boolean bool) {
        this.prefix = "boolean";
        this.element = String.valueOf(bool);
    }

    public ElementTag(int integer) {
        this.prefix = "number";
        this.element = String.valueOf(integer);
    }

    public ElementTag(long lng) {
        this.prefix = "number";
        this.element = String.valueOf(lng);
    }

    public ElementTag(double dbl) {
        this.prefix = "decimal";
        this.element = (dbl == (long) dbl) ? String.format("%d", (long) dbl) : String.valueOf(dbl);
    }

    public boolean isBoolean() {
        return element.equalsIgnoreCase("true") || element.equalsIgnoreCase("false");
    }

    public boolean isDouble() {
        try {
            Double.parseDouble(element);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean isInt() {
        try {
            Integer.parseInt(element);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean asBoolean() {
        return element.equalsIgnoreCase("true");
    }

    public int asInt() {
        return (int) asLong();
    }

    public long asLong() {
        try {
            String cleaned = element;
            int dot = cleaned.indexOf('.');
            if (dot > 0) {
                cleaned = cleaned.substring(0, dot);
            }
            return Long.parseLong(cleaned);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public double asDouble() {
        try {
            return Double.parseDouble(element);
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    public String asString() {
        return element;
    }

    public static void register() {
        BaseTagProcessor.registerBaseTag("element", (attribute) -> new ElementTag(attribute.getParam()));

        TAG_PROCESSOR.registerTag(AbstractTag.class, "ifNull", (attr, obj) -> obj);

        /* @doc tag
         *
         * @Name toUppercase
         * @RawName <ElementTag.toUppercase>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @ArgRequired
         * @Description
         * Returns the value of an element in all uppercase letters.
         *
         * @Implements ElementTag.to_uppercase
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "toUppercase", (attr, obj) -> new ElementTag(obj.element.toUpperCase()));

        /* @doc tag
         *
         * @Name toLowercase
         * @RawName <ElementTag.toLowercase>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the value of an element in all lowercase letters.
         *
         * @Implements ElementTag.to_lowercase
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "toLowercase", (attr, obj) -> new ElementTag(obj.element.toLowerCase()));

        /* @doc tag
         *
         * @Name length
         * @RawName <ElementTag.length>
         * @Object ElementTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Description
         * Returns the length of the element.
         *
         * @Implements ElementTag.length
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "length", (attr, obj) -> new ElementTag(obj.element.length()));

        /* @doc tag
         *
         * @Name isInteger
         * @RawName <ElementTag.isInteger>
         * @Object ElementTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether the element is an integer number (a number without a decimal point), within the limits of a Java "long" (64-bit signed integer).
         *
         * @Implements ElementTag.is_integer
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isInteger", (attr, obj) -> new ElementTag(obj.isInt()));

        /* @doc tag
         *
         * @Name isDecimal
         * @RawName <ElementTag.isDecimal>
         * @Object ElementTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether the element is a valid decimal number (the decimal point is optional).
         *
         * @Implements ElementTag.is_decimal
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isDecimal", (attr, obj) -> new ElementTag(obj.isDouble()));

        /* @doc tag
         *
         * @Name isBoolean
         * @RawName <ElementTag.isBoolean>
         * @Object ElementTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Description
         * Returns whether the element is a boolean ("true" or "false").
         *
         * @Implements ElementTag.is_boolean
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isBoolean", (attr, obj) -> new ElementTag(obj.isBoolean()));

        /* @doc tag
         *
         * @Name add[]
         * @RawName <ElementTag.add[<#.#>]>
         * @Object ElementTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Description
         * Returns the element + number.
         *
         * @Implements ElementTag.add
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "add", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            if (obj.isDouble() && new ElementTag(attr.getParam()).isDouble()) {
                return new ElementTag(obj.asDouble() + new ElementTag(attr.getParam()).asDouble());
            }
            return null;
        }).test("5");

        /* @doc tag
         *
         * @Name sub[]
         * @RawName <ElementTag.sub[<#.#>]>
         * @Object ElementTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Description
         * Returns the element - number.
         *
         * @Implements ElementTag.sub
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "sub", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            if (obj.isDouble() && new ElementTag(attr.getParam()).isDouble()) {
                return new ElementTag(obj.asDouble() - new ElementTag(attr.getParam()).asDouble());
            }
            return null;
        }).test("5");

        /* @doc tag
         *
         * @Name mul[]
         * @RawName <ElementTag.mul[<#.#>]>
         * @Object ElementTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Description
         * Returns the element * number.
         *
         * @Implements ElementTag.mul
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "mul", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            if (obj.isDouble() && new ElementTag(attr.getParam()).isDouble()) {
                return new ElementTag(obj.asDouble() * new ElementTag(attr.getParam()).asDouble());
            }
            return null;
        }).test("5");

        /* @doc tag
         *
         * @Name mod[]
         * @RawName <ElementTag.mod[<#.#>]>
         * @Object ElementTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Description
         * Returns the element % number. (Remainder of the element / number)
         *
         * @Implements ElementTag.mod
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "mod", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            if (obj.isDouble() && new ElementTag(attr.getParam()).isDouble()) {
                return new ElementTag(obj.asDouble() % new ElementTag(attr.getParam()).asDouble());
            } else {
                Debugger.error("Element '" + obj + "' or '" + attr.getParam() + "is not a valid decimal number!");
            }
            return null;
        }).test("5");

        /* @doc tag
         *
         * @Name div[]
         * @RawName <ElementTag.div[<#.#>]>
         * @Object ElementTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Description
         * Returns the element / number.
         *
         * @Implements ElementTag.div
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "div", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            if (obj.isDouble() && new ElementTag(attr.getParam()).isDouble()) {
                return new ElementTag(obj.asDouble() / new ElementTag(attr.getParam()).asDouble());
            } else {
                Debugger.error("Element '" + obj + "' or '" + attr.getParam() + "is not a valid decimal number!");
            }
            return null;
        }).test("5");

        /* @doc tag
         *
         * @Name root[]
         * @RawName <ElementTag.root[<#.#>]>
         * @Object ElementTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Description
         * Returns the root of the element.
         * If no number is specified, returns the square root.
         * Null for negative numbers when asking for an even root.
         *
         * @Implements ElementTag.sqrt
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "root", (attr, obj) -> {
            double value = obj.asDouble();
            double degree = attr.hasParam() ? new ElementTag(attr.getParam()).asDouble() : 2.0;

            if (degree == 0) return new ElementTag(1);

            return new ElementTag(Math.pow(value, 1.0 / degree));
        });

        /* @doc tag
         *
         * @Name pow[]
         * @RawName <ElementTag.pow[<#.#>]>
         * @Object ElementTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Description
         * Returns the element to the power of a number.
         *
         * @Implements ElementTag.power
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "pow", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            double base = obj.asDouble();
            double exponent = new ElementTag(attr.getParam()).asDouble();

            return new ElementTag(Math.pow(base, exponent));
        }).test("2");

        /* @doc tag
         *
         * @Name ifTrue[].ifFalse[]
         * @RawName <ElementTag.ifTrue[<object>].ifFalse[<object>]>
         * @Object ElementTag
         * @ReturnType ObjectTag
         * @Group element checking
         * @ArgRequired
         * @Description
         * If this element is "true", returns the first given object. If it isn't "true", returns the second given object.
         * If the input objects are tags, only the matching tag will be parsed.
         * For example: "<player.exists.ifTrue[<player.name>].ifFalse[server]>"
         * will return the player's name if there's a player present, or if not will return "server", and won't show any errors from the "<player.name>" tag even without a player linked.
         *
         * @Implements ElementTag.if_true[<object>].if_false[<object>]
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "ifTrue", (attr, el) -> {
            boolean isTrue = el.asBoolean();
            String resultText = el.asString();

            if (isTrue && attr.hasParam()) {
                resultText = attr.getParam();
            }

            if (attr.matchesNext("ifFalse")) {
                if (!isTrue && attr.hasNextParam()) {
                    resultText = attr.getNextParam();
                }
                attr.fulfill(1);
            }

            return new ElementTag(resultText);
        }).test("This is true", "ifFalse[This is false!]");

        /* @doc tag
         *
         * @Name repeat[]
         * @RawName <ElementTag.repeat[<#>]>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @ArgRequired
         * @Description
         * Returns a copy of the element, repeated the specified number of times.
         * For example, "hello" .repeat[3] returns "hellohellohello"
         * An input value or zero or a negative number will result in an empty element.
         *
         * @Implements ElementTag.repeat
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "repeat", (attribute, elementTag) -> {
            if (!attribute.hasParam()) return null;
            return new ElementTag(elementTag.element.repeat(new ElementTag(attribute.getParam()).asInt()));
        }).test("2");

        /* @doc tag
         *
         * @Name parseJson
         * @RawName <ElementTag.parseJson>
         * @Object ElementTag
         * @ReturnType MapTag, ListTag
         * @NoArg
         *
         * @Description
         * Parses a JSON formatted string and returns it as a MapTag or ListTag depending on its structure.
         * @Usage
         * // Narrates "bob"
         * - narrate <element[{"name":"bob","age":25}].parseJson.get[name]>
         */
        TAG_PROCESSOR.registerTag(AbstractTag.class, "parseJson", (attr, obj) -> {
            try {
                JsonElement parsed = JsonParser.parseString(obj.asString());
                return JsonHelper.fromJson(parsed);
            } catch (Exception e) {
                Debugger.echoError(attr.getQueue(), "Failed to parse JSON: " + e.getMessage());
                return null;
            }
        });

        /* @doc tag
         * @Name urlEncode
         * @RawName <ElementTag.urlEncode>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @NoArg
         *
         * @Description
         * Encodes the text to be safely used inside a URL.
         * Converts spaces to %20, '#' to %23, '&' to %26, etc.
         *
         * @Usage
         * - define search_query <context.message.urlEncode>
         * - ~fetch "https://api.com/search?q=<[search_query]>"
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "urlEncode", (attr, obj) ->
                new ElementTag(URLEncoder.encode(obj.asString(), StandardCharsets.UTF_8).replace("+", "%20")));

        /* @doc tag
         * @Name urlDecode
         * @RawName <ElementTag.urlDecode>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @NoArg
         *
         * @Description
         * Decodes a URL-encoded string back to normal text.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "urlDecode", (attr, obj) -> {
            try {
                return new ElementTag(URLDecoder.decode(obj.asString(), StandardCharsets.UTF_8));
            } catch (Exception e) {
                return obj;
            }
        });
    }

    @Override
    public @NonNull String getPrefix() {
        return prefix;
    }

    @Override
    public @NonNull String identify() {
        return element;
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ElementTag other)) return false;
        return element.equals(other.element);
    }

    @Override
    public int hashCode() {
        return element.hashCode();
    }

    public @NonNull String getTestValue() { return "123456789"; }

    @Override
    public @NonNull TagProcessor<ElementTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public @NonNull Component asComponent() {
        if (identify().isEmpty()) return Component.empty();
        return Corex.SERIALIZER.deserialize(identify());
    }
}