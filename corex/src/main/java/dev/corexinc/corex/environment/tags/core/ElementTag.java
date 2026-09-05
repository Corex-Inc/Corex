package dev.corexinc.corex.environment.tags.core;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.engine.utils.CorexSerializer;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.utils.scripts.JsonHelper;
import net.kyori.adventure.text.Component;
import org.jspecify.annotations.NonNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
 *
 * @Implements ElementTag
 */
public class ElementTag implements AbstractTag {

    private static final byte NUM_NOT_NUMERIC = 1;
    private static final byte NUM_LONG = 2;
    private static final byte NUM_DOUBLE = 3;

    private final String prefix;
    private String element;

    private long cachedLong;
    private double cachedDouble;
    private volatile byte numericKind;

    public static final TagProcessor<ElementTag> TAG_PROCESSOR = new TagProcessor<>();

    public ElementTag(String string) {
        this.prefix = "el";
        if (string == null) {
            this.element = "";
        } else {
            this.element = string.regionMatches(true, 0, "el@", 0, 3) ? string.substring(3) : string;
        }
    }

    public ElementTag(Component component) {
        this.prefix = "el";
        this.element = CorexSerializer.LEGACY.serialize(component);
    }

    public ElementTag(boolean bool) {
        this.prefix = "boolean";
        this.element = String.valueOf(bool);
        this.numericKind = NUM_NOT_NUMERIC;
    }

    public ElementTag(int integer) {
        this.prefix = "number";
        this.cachedLong = integer;
        this.cachedDouble = integer;
        this.numericKind = NUM_LONG;
    }

    public ElementTag(long lng) {
        this.prefix = "number";
        this.cachedLong = lng;
        this.cachedDouble = lng;
        this.numericKind = NUM_LONG;
    }

    public ElementTag(double dbl) {
        this.prefix = "decimal";
        if (dbl == (long) dbl) {
            this.cachedLong = (long) dbl;
            this.cachedDouble = dbl;
            this.numericKind = NUM_LONG;
        } else {
            this.cachedDouble = dbl;
            this.numericKind = NUM_DOUBLE;
        }
    }

    private byte resolveNumericKind() {
        byte kind = numericKind;
        if (kind == 0) {
            kind = parseNumeric();
        }
        return kind;
    }

    private byte parseNumeric() {
        String value = element;
        int length = value.length();
        if (length == 0) {
            numericKind = NUM_NOT_NUMERIC;
            return NUM_NOT_NUMERIC;
        }
        char first = value.charAt(0);
        boolean plausible = (first >= '0' && first <= '9') || first == '-' || first == '+'
                || first == '.' || first == 'N' || first == 'I' || first == ' ';
        if (!plausible) {
            numericKind = NUM_NOT_NUMERIC;
            return NUM_NOT_NUMERIC;
        }
        int digitStart = (first == '-' || first == '+') ? 1 : 0;
        int digitCount = length - digitStart;
        if (digitCount >= 1 && digitCount <= 18) {
            boolean allDigits = true;
            long parsed = 0;
            for (int i = digitStart; i < length; i++) {
                char c = value.charAt(i);
                if (c < '0' || c > '9') {
                    allDigits = false;
                    break;
                }
                parsed = parsed * 10 + (c - '0');
            }
            if (allDigits) {
                cachedLong = first == '-' ? -parsed : parsed;
                cachedDouble = cachedLong;
                numericKind = NUM_LONG;
                return NUM_LONG;
            }
        }
        try {
            cachedDouble = Double.parseDouble(value);
            numericKind = NUM_DOUBLE;
            return NUM_DOUBLE;
        } catch (NumberFormatException ignored) {
            numericKind = NUM_NOT_NUMERIC;
            return NUM_NOT_NUMERIC;
        }
    }

    public boolean isBoolean() {
        String value = element;
        if (value == null) return false;
        return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false");
    }

    public boolean isDouble() {
        return resolveNumericKind() != NUM_NOT_NUMERIC;
    }

    public boolean isInt() {
        return resolveNumericKind() == NUM_LONG
                && cachedLong >= Integer.MIN_VALUE && cachedLong <= Integer.MAX_VALUE;
    }

    public boolean asBoolean() {
        String value = element;
        return value != null && value.equalsIgnoreCase("true");
    }

    public boolean isKnownNumeric() {
        byte kind = numericKind;
        return kind == NUM_LONG || kind == NUM_DOUBLE;
    }

    public int asInt() {
        return (int) asLong();
    }

    public long asLong() {
        byte kind = resolveNumericKind();
        if (kind == NUM_LONG) return cachedLong;
        if (kind == NUM_DOUBLE) return (long) cachedDouble;
        return 0;
    }

    public double asDouble() {
        return resolveNumericKind() == NUM_NOT_NUMERIC ? 0.0 : cachedDouble;
    }

    public String asString() {
        String value = element;
        if (value == null) {
            value = numericKind == NUM_DOUBLE ? Double.toString(cachedDouble) : Long.toString(cachedLong);
            element = value;
        }
        return value;
    }

    private static ElementTag numericParamOf(Attribute attr) {
        AbstractTag raw = attr.getParamObject();
        if (raw == null) return null;
        ElementTag param = raw instanceof ElementTag el ? el : new ElementTag(raw.identify());
        return param.isDouble() ? param : null;
    }

    public static void register() {
        BaseTagProcessor.registerBaseTag("element", (attribute) -> {
            AbstractTag param = attribute.getParamObject();
            if (param instanceof ComponentTag) return param;
            return new ElementTag(param != null ? param.identify() : null);
        });

        TAG_PROCESSOR.registerTag(AbstractTag.class, "ifNull", (attr, obj) -> obj).setAsyncSafe();

        /* @doc tag
         *
         * @Name toUppercase
         * @RawName <ElementTag.toUppercase>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @ArgRequired
         * @Async
         * @Description
         * Returns the value of an element in all uppercase letters.
         *
         * @Implements ElementTag.to_uppercase
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "toUppercase", (attr, obj) -> new ElementTag(obj.asString().toUpperCase())).setAsyncSafe();

        /* @doc tag
         *
         * @Name toLowercase
         * @RawName <ElementTag.toLowercase>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the value of an element in all lowercase letters.
         *
         * @Implements ElementTag.to_lowercase
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "toLowercase", (attr, obj) -> new ElementTag(obj.asString().toLowerCase())).setAsyncSafe();

        /* @doc tag
         *
         * @Name length
         * @RawName <ElementTag.length>
         * @Object ElementTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Async
         * @Description
         * Returns the length of the element.
         *
         * @Implements ElementTag.length
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "length", (attr, obj) -> new ElementTag(obj.asString().length())).setAsyncSafe();

        /* @doc tag
         *
         * @Name substring[]
         * @RawName <ElementTag.substring[<#>].to[<#>]>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @ArgRequired
         * @Async
         * @Description
         * Returns the part of the text between two 1-based positions, both included. Leave off
         * '.to[]' to read to the end. Positions past the end are clamped rather than erroring, so
         * substring[3] on a two character element gives an empty result instead of failing.
         *
         * @Usage
         * // Narrates "ell"
         * - narrate <element[hello].substring[2].to[4]>
         *
         * @Usage
         * // Narrates "llo"
         * - narrate <element[hello].substring[3]>
         *
         * @Implements ElementTag.substring[<#>(,<#>)]
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "substring", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            String value = obj.asString();
            int length = value.length();

            ElementTag fromParam = numericParamOf(attr);
            if (fromParam == null) return null;
            int from = fromParam.asInt() - 1;

            int to = length;
            if (attr.matchesNext("to") && attr.hasNextParam()) {
                ElementTag toParam = attr.getNextParamObject(ElementTag.class, ElementTag::new);
                attr.fulfill(1);
                if (toParam == null || !toParam.isDouble()) return null;
                to = toParam.asInt();
            }

            if (from < 0) from = 0;
            if (to > length) to = length;
            if (from >= to) return new ElementTag("");
            return new ElementTag(value.substring(from, to));
        }).test("2", "to", "3").setAsyncSafe();

        /* @doc tag
         *
         * @Name isInteger
         * @RawName <ElementTag.isInteger>
         * @Object ElementTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Async
         * @Description
         * Returns whether the element is an integer number (a number without a decimal point), within the limits of a Java "long" (64-bit signed integer).
         *
         * @Implements ElementTag.is_integer
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isInteger", (attr, obj) -> new ElementTag(obj.isInt())).setAsyncSafe();

        /* @doc tag
         *
         * @Name isDecimal
         * @RawName <ElementTag.isDecimal>
         * @Object ElementTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Async
         * @Description
         * Returns whether the element is a valid decimal number (the decimal point is optional).
         *
         * @Implements ElementTag.is_decimal
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isDecimal", (attr, obj) -> new ElementTag(obj.isDouble())).setAsyncSafe();

        /* @doc tag
         *
         * @Name isBoolean
         * @RawName <ElementTag.isBoolean>
         * @Object ElementTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Async
         * @Description
         * Returns whether the element is a boolean ("true" or "false").
         *
         * @Implements ElementTag.is_boolean
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isBoolean", (attr, obj) -> new ElementTag(obj.isBoolean())).setAsyncSafe();

        /* @doc tag
         *
         * @Name add[]
         * @RawName <ElementTag.add[<#.#>]>
         * @Object ElementTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Async
         * @Description
         * Returns the element + number.
         *
         * @Implements ElementTag.add
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "add", (attr, obj) -> {
            if (!attr.hasParam() || !obj.isDouble()) return null;
            ElementTag param = numericParamOf(attr);
            if (param == null) return null;
            return new ElementTag(obj.asDouble() + param.asDouble());
        }).test("5").setAsyncSafe();

        /* @doc tag
         *
         * @Name sub[]
         * @RawName <ElementTag.sub[<#.#>]>
         * @Object ElementTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Async
         * @Description
         * Returns the element - number.
         *
         * @Implements ElementTag.sub
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "sub", (attr, obj) -> {
            if (!attr.hasParam() || !obj.isDouble()) return null;
            ElementTag param = numericParamOf(attr);
            if (param == null) return null;
            return new ElementTag(obj.asDouble() - param.asDouble());
        }).test("5").setAsyncSafe();

        /* @doc tag
         *
         * @Name mul[]
         * @RawName <ElementTag.mul[<#.#>]>
         * @Object ElementTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Async
         * @Description
         * Returns the element * number.
         *
         * @Implements ElementTag.mul
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "mul", (attr, obj) -> {
            if (!attr.hasParam() || !obj.isDouble()) return null;
            ElementTag param = numericParamOf(attr);
            if (param == null) return null;
            return new ElementTag(obj.asDouble() * param.asDouble());
        }).test("5").setAsyncSafe();

        /* @doc tag
         *
         * @Name mod[]
         * @RawName <ElementTag.mod[<#.#>]>
         * @Object ElementTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Async
         * @Description
         * Returns the element % number. (Remainder of the element / number)
         *
         * @Implements ElementTag.mod
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "mod", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            ElementTag param = obj.isDouble() ? numericParamOf(attr) : null;
            if (param == null) {
                Debugger.error("Element '" + obj + "' or '" + attr.getParam() + "is not a valid decimal number!");
                return null;
            }
            return new ElementTag(obj.asDouble() % param.asDouble());
        }).test("5").setAsyncSafe();

        /* @doc tag
         *
         * @Name div[]
         * @RawName <ElementTag.div[<#.#>]>
         * @Object ElementTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Async
         * @Description
         * Returns the element / number.
         *
         * @Implements ElementTag.div
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "div", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            ElementTag param = obj.isDouble() ? numericParamOf(attr) : null;
            if (param == null) {
                Debugger.error("Element '" + obj + "' or '" + attr.getParam() + "is not a valid decimal number!");
                return null;
            }
            return new ElementTag(obj.asDouble() / param.asDouble());
        }).test("5").setAsyncSafe();

        /* @doc tag
         *
         * @Name root[]
         * @RawName <ElementTag.root[<#.#>]>
         * @Object ElementTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Async
         * @Description
         * Returns the root of the element.
         * If no number is specified, returns the square root.
         * Null for negative numbers when asking for an even root.
         *
         * @Implements ElementTag.sqrt
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "root", (attr, obj) -> {
            double value = obj.asDouble();
            double degree = 2.0;
            if (attr.hasParam()) {
                ElementTag degreeParam = numericParamOf(attr);
                degree = degreeParam != null ? degreeParam.asDouble() : 0.0;
            }

            if (degree == 0) return new ElementTag(1);

            return new ElementTag(Math.pow(value, 1.0 / degree));
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name pow[]
         * @RawName <ElementTag.pow[<#.#>]>
         * @Object ElementTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Async
         * @Description
         * Returns the element to the power of a number.
         *
         * @Implements ElementTag.power
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "pow", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            double base = obj.asDouble();
            ElementTag param = numericParamOf(attr);
            double exponent = param != null ? param.asDouble() : 0.0;

            return new ElementTag(Math.pow(base, exponent));
        }).test("2").setAsyncSafe();

        /* @doc tag
         *
         * @Name abs
         * @RawName <ElementTag.abs>
         * @Object ElementTag
         * @ReturnType ElementTag(Decimal)
         * @NoArg
         * @Async
         * @Description
         * Returns the absolute value of the element.
         *
         * @Implements ElementTag.abs
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "abs", (attr, obj) -> new ElementTag(Math.abs(obj.asDouble()))).setAsyncSafe();

        /* @doc tag
         *
         * @Name isLessThan[]
         * @RawName <ElementTag.isLessThan[<#.#>]>
         * @Object ElementTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Async
         * @Description
         * Returns true if number is less than param.
         *
         * @Implements ElementTag.is_less_than[]
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isLessThan", (attr, obj) -> {
            if (!attr.hasParam() || !(attr.getParamObject() instanceof ElementTag param) || !param.isDouble()) return null;
            return new ElementTag(obj.asDouble() < param.asDouble());
        }).test("22").setAsyncSafe();

        /* @doc tag
         *
         * @Name isMoreThan[]
         * @RawName <ElementTag.isMoreThan[<#.#>]>
         * @Object ElementTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Async
         * @Description
         * Returns true if number is more than param.
         *
         * @Implements ElementTag.is_more_than[]
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isMoreThan", (attr, obj) -> {
            if (!attr.hasParam() || !(attr.getParamObject() instanceof ElementTag param) || !param.isDouble()) return null;
            return new ElementTag(obj.asDouble() > param.asDouble());
        }).test("22").setAsyncSafe();

        /* @doc tag
         *
         * @Name isLessOrEqual[]
         * @RawName <ElementTag.isLessOrEqual[<#.#>]>
         * @Object ElementTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Async
         * @Description
         * Returns true if number is less than or equal to param.
         *
         * @Implements ElementTag.is_less_than_of_equal_to[]
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isLessOrEqual", (attr, obj) -> {
            if (!attr.hasParam() || !(attr.getParamObject() instanceof ElementTag param) || !param.isDouble()) return null;
            return new ElementTag(obj.asDouble() <= param.asDouble());
        }).test("22").setAsyncSafe();

        /* @doc tag
         *
         * @Name isMoreOrEqual[]
         * @RawName <ElementTag.isMoreOrEqual[<#.#>]>
         * @Object ElementTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Async
         * @Description
         * Returns true if number is more than or equal to param.
         *
         * @Implements ElementTag.is_more_than_of_equal_to[]
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isMoreOrEqual", (attr, obj) -> {
            if (!attr.hasParam() || !(attr.getParamObject() instanceof ElementTag param) || !param.isDouble()) return null;
            return new ElementTag(obj.asDouble() >= param.asDouble());
        }).test("22").setAsyncSafe();

        /* @doc tag
         *
         * @Name ifTrue[].ifFalse[]
         * @RawName <ElementTag.ifTrue[<object>].ifFalse[<object>]>
         * @Object ElementTag
         * @ReturnType ObjectTag
         * @Group element checking
         * @ArgRequired
         * @Async
         * @Description
         * If this element is "true", returns the first given object. If it isn't "true", returns the second given object.
         * If the input objects are tags, only the matching tag will be parsed.
         * For example: "<player.exists.ifTrue[<player.name>].ifFalse[server]>"
         * will return the player's name if there's a player present, or if not will return "server", and won't show any errors from the "<player.name>" tag even without a player linked.
         *
         * @Implements ElementTag.if_true[<object>].if_false[<object>]
         */
        TAG_PROCESSOR.registerTag(AbstractTag.class, "ifTrue", (attr, el) -> {
            boolean isTrue = el.asBoolean();
            AbstractTag result = el;

            if (isTrue && attr.hasParam()) {
                result = attr.getParamObject();
            }

            if (attr.matchesNext("ifFalse")) {
                if (!isTrue && attr.hasNextParam()) {
                    result = attr.getNextParamObject();
                }
                attr.fulfill(1);
            }

            return result;
        }).test("This is true", "ifFalse[This is false!]").setAsyncSafe();

        /* @doc tag
         *
         * @Name repeat[]
         * @RawName <ElementTag.repeat[<#>]>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @ArgRequired
         * @Async
         * @Description
         * Returns a copy of the element, repeated the specified number of times.
         * For example, "hello" .repeat[3] returns "hellohellohello"
         * An input value or zero or a negative number will result in an empty element.
         *
         * @Implements ElementTag.repeat
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "repeat", (attribute, elementTag) -> {
            if (!attribute.hasParam()) return null;
            return new ElementTag(elementTag.asString().repeat(new ElementTag(attribute.getParam()).asInt()));
        }).test("2").setAsyncSafe();

        /* @doc tag
         *
         * @Name parseJson
         * @RawName <ElementTag.parseJson>
         * @Object ElementTag
         * @ReturnType MapTag, ListTag
         * @NoArg
         * @Async
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
        }).setAsyncSafe();

        /* @doc tag
         * @Name urlEncode
         * @RawName <ElementTag.urlEncode>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
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
                new ElementTag(URLEncoder.encode(obj.asString(), StandardCharsets.UTF_8).replace("+", "%20"))).setAsyncSafe();

        /* @doc tag
         * @Name urlDecode
         * @RawName <ElementTag.urlDecode>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
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
        }).setAsyncSafe();

        /* @doc tag
         * @Name cos
         * @RawName <ElementTag.cos>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         *
         * @Description
         * Returns the cosine of the specified object
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "cos", (attr, obj) -> new ElementTag(Math.cos(obj.asDouble()))).setAsyncSafe();

        /* @doc tag
         * @Name acos
         * @RawName <ElementTag.acos>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         *
         * @Description
         * Returns the arccosine of the specified object.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "acos", (attr, obj) -> new ElementTag(Math.acos(obj.asDouble()))).setAsyncSafe();

        /* @doc tag
         * @Name sin
         * @RawName <ElementTag.sin>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         *
         * @Description
         * Returns the sinus of the specified object.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "sin", (attr, obj) -> new ElementTag(Math.sin(obj.asDouble()))).setAsyncSafe();

        /* @doc tag
         * @Name asin
         * @RawName <ElementTag.asin>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         *
         * @Description
         * Returns the arcsine of the specified object.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "asin", (attr, obj) -> new ElementTag(Math.asin(obj.asDouble()))).setAsyncSafe();

        /* @doc tag
         * @Name tgn
         * @RawName <ElementTag.tg>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         *
         * @Description
         * Returns the tangent of the specified object.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "tg", (attr, obj) -> new ElementTag(Math.tan(obj.asDouble()))).setAsyncSafe();

        /* @doc tag
         * @Name tgh
         * @RawName <ElementTag.tgh>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         *
         * @Description
         * Returns the hyperbolic tangent of the specified object.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "tgh", (attr, obj) -> new ElementTag(Math.tanh(obj.asDouble()))).setAsyncSafe();

        /* @doc tag
         * @Name atgn
         * @RawName <ElementTag.atg>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         *
         * @Description
         * Returns the atan of the specified object.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "atg", (attr, obj) -> new ElementTag(Math.atan(obj.asDouble()))).setAsyncSafe();

        /* @doc tag
         * @Name atg2[]
         * @RawName <ElementTag.atg2[<#.#>]>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @ArgRequired
         * @Async
         *
         * @Description
         * Returns the atan2 of the specified object.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "atg2", (attr, obj) -> {
            if (!attr.hasParam() || !obj.isDouble()) return null;
            ElementTag param = numericParamOf(attr);
            if (param == null) return null;
            return new ElementTag(Math.atan2(obj.asDouble(), param.asDouble()));
        }).test("5").setAsyncSafe();

        /* @doc tag
         * @Name ctg
         * @RawName <ElementTag.ctg>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         *
         * @Description
         * Returns the cotangent of the specified object.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "ctg", (attr, obj) -> new ElementTag(1 / Math.tan(obj.asDouble()))).setAsyncSafe();

        /* @doc tag
         * @Name actg
         * @RawName <ElementTag.actg>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         *
         * @Description
         * Returns the arccotangent of the specified object.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "actg", (attr, obj) -> new ElementTag(Math.PI / 2 - Math.atan(1 / Math.tan(obj.asDouble())))).setAsyncSafe();

        /* @doc tag
         * @Name factorial
         * @RawName <ElementTag.factorial>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         *
         * @Description
         * Returns the factorial of the specified object.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "factorial", (attr, obj) -> {
            long result = 1;
            for (int i = 1; i <= obj.asLong(); i++) {
                result *= i;
            }
            return new ElementTag(result);
        }).setAsyncSafe();

        /* @doc tag
         * @Name ln
         * @RawName <ElementTag.ln>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         *
         * @Description
         * Returns the natural logarithm of the specified object.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "ln", (attr, obj) -> new ElementTag(Math.log(obj.asDouble()))).setAsyncSafe();

        /* @doc tag
         * @Name log[]
         * @RawName <ElementTag.log[<#.#>]>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @ArgRequired
         * @Async
         *
         * @Description
         * Returns the logarithm of the specified object.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "log", (attr, obj) -> {
            if (!attr.hasParam() || !obj.isDouble()) return null;
            ElementTag param = numericParamOf(attr);
            if (param == null) return null;
            return new ElementTag(Math.log(obj.asDouble()) / Math.log(param.asDouble()));
        }).test("5").setAsyncSafe();

        /* @doc tag
         *
         * @Name numberToHex
         * @RawName <ElementTag.numberToHex>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Converts an integer to a hexadecimal string.
         * @Usage
         * // Narrates "ff"
         * - narrate <element[255].numberToHex>
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "numberToHex", (attr, obj) -> new ElementTag(Integer.toHexString(obj.asInt()))).setAsyncSafe();

        /* @doc tag
         *
         * @Name max[]
         * @RawName <ElementTag.max[<#.#>]>
         * @Object ElementTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Async
         * @Description
         * Returns the greater of the element and the given number.
         * For example, <element[-5].max[0]> returns 0.
         *
         * @Implements ElementTag.max
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "max", (attr, obj) -> {
            if (!attr.hasParam() || !obj.isDouble()) return null;
            ElementTag param = numericParamOf(attr);
            if (param == null) return null;
            return new ElementTag(Math.max(obj.asDouble(), param.asDouble()));
        }).test("5").setAsyncSafe();

        /* @doc tag
         *
         * @Name min[]
         * @RawName <ElementTag.min[<#.#>]>
         * @Object ElementTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Async
         * @Description
         * Returns the lesser of the element and the given number.
         * For example, <element[360].min[360]> returns 360.
         *
         * @Implements ElementTag.min
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "min", (attr, obj) -> {
            if (!attr.hasParam() || !obj.isDouble()) return null;
            ElementTag param = numericParamOf(attr);
            if (param == null) return null;
            return new ElementTag(Math.min(obj.asDouble(), param.asDouble()));
        }).test("5").setAsyncSafe();

        /* @doc tag
         *
         * @Name round
         * @RawName <ElementTag.round>
         * @Object ElementTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Async
         * @Description
         * Returns the element rounded to the nearest integer.
         * For example, <element[3.7].round> returns 4.
         *
         * @Implements ElementTag.round
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "round", (attr, obj) ->
                new ElementTag(Math.round(obj.asDouble()))).setAsyncSafe();

        /* @doc tag
         *
         * @Name roundTo[]
         * @RawName <ElementTag.roundTo[<#>]>
         * @Object ElementTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Async
         * @Description
         * Returns the element rounded to the specified number of decimal places.
         * Uses HALF_UP rounding mode (i.e. 0.5 rounds up).
         * For example, <element[3.14159].roundTo[2]> returns 3.14.
         *
         * @Usage
         * // Narrates "3.14"
         * - narrate <element[3.14159].roundTo[2]>
         *
         * @Usage
         * // Narrates "100"
         * - narrate <element[99.999].roundTo[0]>
         *
         * @Implements ElementTag.round_to[<#>]
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "roundTo", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            if (!obj.isDouble()) return null;
            int places = new ElementTag(attr.getParam()).asInt();
            if (places < 0) return null;
            double rounded = BigDecimal.valueOf(obj.asDouble())
                    .setScale(places, RoundingMode.HALF_UP)
                    .doubleValue();
            return new ElementTag(rounded);
        }).test("2").setAsyncSafe();

        /* @doc tag
         *
         * @Name roundToPrecision[]
         * @RawName <ElementTag.roundToPrecision[<#.#>]>
         * @Object ElementTag
         * @ReturnType ElementTag(Decimal)
         * @ArgRequired
         * @Async
         * @Description
         * Snaps the element to the nearest multiple of the given step value, rather than to a fixed number of decimal places.
         * Useful for grid-snapping or quantizing a value (e.g. positions, sensitivity steps).
         * For example, <element[0.137].roundToPrecision[0.01]> returns 0.14, and <element[7].roundToPrecision[5]> returns 5.
         * A step of 0 or less returns the element unchanged.
         *
         * @Usage
         * // Narrates "0.14"
         * - narrate <element[0.137].roundToPrecision[0.01]>
         *
         * @Implements ElementTag.round_to_precision[<#.#>]
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "roundToPrecision", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            if (!obj.isDouble()) return null;
            ElementTag stepTag = new ElementTag(attr.getParam());
            if (!stepTag.isDouble()) return null;
            double step = stepTag.asDouble();
            if (step <= 0) return new ElementTag(obj.asDouble());
            double snapped = Math.round(obj.asDouble() / step) * step;
            return new ElementTag(BigDecimal.valueOf(snapped)
                    .setScale(8, RoundingMode.HALF_UP)
                    .stripTrailingZeros()
                    .doubleValue());
        }).test("0.01").setAsyncSafe();

        /* @doc tag
         *
         * @Name floor
         * @RawName <ElementTag.floor>
         * @Object ElementTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Async
         * @Description
         * Returns the largest integer less than or equal to the element (rounds down).
         * For example, <element[3.9].floor> returns 3, and <element[-1.2].floor> returns -2.
         *
         * @Usage
         * // Narrates "3"
         * - narrate <element[3.9].floor>
         *
         * @Implements ElementTag.round_down
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "floor", (attr, obj) -> new ElementTag((long) Math.floor(obj.asDouble()))).setAsyncSafe();

        /* @doc tag
         *
         * @Name ceil
         * @RawName <ElementTag.ceil>
         * @Object ElementTag
         * @ReturnType ElementTag(Number)
         * @NoArg
         * @Async
         * @Description
         * Returns the smallest integer greater than or equal to the element (rounds up).
         * For example, <element[3.1].ceil> returns 4, and <element[-1.9].ceil> returns -1.
         *
         * @Usage
         * // Narrates "4"
         * - narrate <element[3.1].ceil>
         *
         * @Implements ElementTag.round_up
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "ceil", (attr, obj) -> new ElementTag((long) Math.ceil(obj.asDouble()))).setAsyncSafe();

        /* @doc tag
         *
         * @Name padLeft[].with[]
         * @RawName <ElementTag.padLeft[<#>].with[<element>]>
         * @Object ElementTag
         * @ReturnType ElementTag
         * @ArgRequired
         * @Async
         * @Description
         * Left-pads the element to the specified total length using the given fill character or string.
         * If the element is already at or beyond the target length, it is returned unchanged.
         * The optional .with[] segment specifies the pad character (defaults to a space).
         * For example, <element[ff].padLeft[4].with[0]> returns "00ff".
         *
         * @Implements ElementTag.pad_left
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "padLeft", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            int targetLength = new ElementTag(attr.getParam()).asInt();
            String fill = " ";
            if (attr.matchesNext("with") && attr.hasNextParam()) {
                fill = attr.getNextParam();
                attr.fulfill(1);
            }
            if (fill.isEmpty()) fill = " ";
            String value = obj.asString();
            if (value.length() >= targetLength) return new ElementTag(value);
            String pad = fill.repeat((targetLength - value.length() + fill.length() - 1) / fill.length());
            return new ElementTag(pad.substring(0, targetLength - value.length()) + value);
        }).test("2", "with[0]").setAsyncSafe();
    }

    @Override
    public @NonNull String getPrefix() {
        return prefix;
    }

    @Override
    public @NonNull String identify() {
        return asString();
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ElementTag other)) return false;
        return asString().equals(other.asString());
    }

    @Override
    public int hashCode() {
        return asString().hashCode();
    }

    public @NonNull String getTestValue() {
        return "123456789";
    }

    @Override
    public @NonNull TagProcessor<ElementTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public @NonNull Component asComponent() {
        if (identify().isEmpty()) return Component.empty();
        return CorexSerializer.LEGACY.deserialize(identify());
    }
}