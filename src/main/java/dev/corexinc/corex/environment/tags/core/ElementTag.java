package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jspecify.annotations.NonNull;

public class ElementTag implements AbstractTag {

    private final String prefix;
    private final String element;

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    public static final TagProcessor<ElementTag> PROCESSOR = new TagProcessor<>();

    public ElementTag(String string) {
        this.prefix = "el";
        if (string == null) {
            new ElementTag(String.valueOf(true));
            this.element = "";
        } else {
            this.element = string.toLowerCase().startsWith(prefix + "@") ? string.substring(3) : string;
        }
    }

    public ElementTag(Component component) {
        this.prefix = "el";
        this.element = MINI_MESSAGE.serialize(component);
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

        PROCESSOR.registerTag(AbstractTag.class, "ifNull", (attr, obj) -> obj);

        PROCESSOR.registerTag(ElementTag.class, "toUppercase", (attr, obj) -> new ElementTag(obj.element.toUpperCase()));
        PROCESSOR.registerTag(ElementTag.class, "toLowercase", (attr, obj) -> new ElementTag(obj.element.toLowerCase()));
        PROCESSOR.registerTag(ElementTag.class, "length", (attr, obj) -> new ElementTag(obj.element.length()));

        PROCESSOR.registerTag(ElementTag.class, "isInteger", (attr, obj) -> new ElementTag(obj.isInt()));
        PROCESSOR.registerTag(ElementTag.class, "isDecimal", (attr, obj) -> new ElementTag(obj.isDouble()));
        PROCESSOR.registerTag(ElementTag.class, "isBoolean", (attr, obj) -> new ElementTag(obj.isBoolean()));

        PROCESSOR.registerTag(ElementTag.class, "add", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            if (obj.isDouble() && new ElementTag(attr.getParam()).isDouble()) {
                return new ElementTag(obj.asDouble() + new ElementTag(attr.getParam()).asDouble());
            }
            return null;
        }).test("5");

        PROCESSOR.registerTag(ElementTag.class, "root", (attr, obj) -> {
            double value = obj.asDouble();
            double degree = attr.hasParam() ? new ElementTag(attr.getParam()).asDouble() : 2.0;

            if (degree == 0) return new ElementTag(1);

            return new ElementTag(Math.pow(value, 1.0 / degree));
        });

        PROCESSOR.registerTag(ElementTag.class, "pow", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            double base = obj.asDouble();
            double exponent = new ElementTag(attr.getParam()).asDouble();

            return new ElementTag(Math.pow(base, exponent));
        }).test("2");

        PROCESSOR.registerTag(ElementTag.class, "ifTrue", (attr, el) -> {
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

        PROCESSOR.registerTag(ElementTag.class, "repeat", (attribute, elementTag) -> {
            if (!attribute.hasParam()) return null;
            return new ElementTag(elementTag.element.repeat(new ElementTag(attribute.getParam()).asInt()));
        }).test("2");
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
        return PROCESSOR.process(this, attribute);
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
        return PROCESSOR;
    }
}