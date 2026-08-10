package dev.corexinc.corex.api.properties;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.environment.tags.core.ColorTag;
import dev.corexinc.corex.environment.tags.core.DurationTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.tags.core.QuaternionTag;
import dev.corexinc.corex.environment.tags.world.ItemTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;

/**
 * The ready-made {@link PropertyType} codecs.
 *
 * <p>These cover essentially every input shape a mechanism takes, so a property declaration is
 * usually just {@code PropertyTypes.BOOLEAN} or {@code PropertyTypes.enumOf(Display.Billboard.class)}
 * and no hand-written validation at all.</p>
 *
 * @since 1.0.0
 */
@AvailableSince("1.0.0")
public final class PropertyTypes {

    private PropertyTypes() {}

    /** Accepts "true"/"false" (any case). Anything else is rejected. */
    public static final PropertyType<Boolean> BOOLEAN = new PropertyType<>() {
        @Override
        public Boolean parse(@NotNull AbstractTag input) {
            String raw = input.identify().trim();
            if (raw.equalsIgnoreCase("true")) return Boolean.TRUE;
            if (raw.equalsIgnoreCase("false")) return Boolean.FALSE;
            return null;
        }

        @Override
        public @NotNull AbstractTag write(@NotNull Boolean value) {
            return new ElementTag(value.booleanValue());
        }

        @Override
        public @NotNull Class<? extends AbstractTag> tagClass() {
            return ElementTag.class;
        }

        @Override
        public @NotNull String describeInput() {
            return "a boolean (true/false)";
        }
    };

    /** Accepts any whole number within int range. */
    public static final PropertyType<Integer> INTEGER = numeric("a whole number",
            element -> element.isDouble() ? Integer.valueOf(element.asInt()) : null,
            value -> new ElementTag(value.intValue()));

    /** Accepts any whole number within long range. */
    public static final PropertyType<Long> LONG = numeric("a whole number",
            element -> element.isDouble() ? Long.valueOf(element.asLong()) : null,
            value -> new ElementTag(value.longValue()));

    /** Accepts any decimal number. */
    public static final PropertyType<Double> DOUBLE = numeric("a decimal number",
            element -> element.isDouble() ? Double.valueOf(element.asDouble()) : null,
            value -> new ElementTag(value.doubleValue()));

    /** Accepts any decimal number, narrowed to a float. */
    public static final PropertyType<Float> FLOAT = numeric("a decimal number",
            element -> element.isDouble() ? Float.valueOf((float) element.asDouble()) : null,
            value -> new ElementTag(value.doubleValue()));

    /** Accepts any text as-is. */
    public static final PropertyType<String> STRING = new PropertyType<>() {
        @Override
        public String parse(@NotNull AbstractTag input) {
            return input.identify();
        }

        @Override
        public @NotNull AbstractTag write(@NotNull String value) {
            return new ElementTag(value);
        }

        @Override
        public @NotNull Class<? extends AbstractTag> tagClass() {
            return ElementTag.class;
        }

        @Override
        public @NotNull String describeInput() {
            return "text";
        }
    };

    /**
     * Accepts any tag and keeps its rendered form, so colors and formatters survive.
     * This is the type for anything the player actually sees.
     */
    public static final PropertyType<Component> TEXT = new PropertyType<>() {
        @Override
        public Component parse(@NotNull AbstractTag input) {
            return input.asComponent();
        }

        @Override
        public @NotNull AbstractTag write(@NotNull Component value) {
            return new ElementTag(value);
        }

        @Override
        public @NotNull Class<? extends AbstractTag> tagClass() {
            return ElementTag.class;
        }

        @Override
        public @NotNull String describeInput() {
            return "text";
        }
    };

    /**
     * Accepts either a duration ("2s", "40t", "1m30s") or a plain tick count, and yields ticks.
     * Writes back as a plain tick number.
     */
    public static final PropertyType<Integer> TICKS = new PropertyType<>() {
        @Override
        public Integer parse(@NotNull AbstractTag input) {
            DurationTag duration = DurationTag.tryParse(input);
            if (duration != null) return (int) Math.round(duration.getTicks());
            ElementTag element = asElement(input);
            return element.isDouble() ? Integer.valueOf(element.asInt()) : null;
        }

        @Override
        public @NotNull AbstractTag write(@NotNull Integer value) {
            return new ElementTag(value.intValue());
        }

        @Override
        public @NotNull Class<? extends AbstractTag> tagClass() {
            return ElementTag.class;
        }

        @Override
        public @NotNull String describeInput() {
            return "a duration (e.g. '2s') or a tick count";
        }
    };

    /** Accepts a LocationTag, or anything parseable as one. */
    public static final PropertyType<LocationTag> LOCATION =
            converting(LocationTag.class, LocationTag::new, "a location");

    /** Accepts a QuaternionTag, or anything parseable as one. */
    public static final PropertyType<QuaternionTag> QUATERNION =
            converting(QuaternionTag.class, QuaternionTag::new, "a quaternion");

    /** Accepts a ColorTag, a hex string, or a named color. */
    public static final PropertyType<ColorTag> COLOR =
            converting(ColorTag.class, ColorTag::new, "a color");

    /** Accepts an ItemTag, or an item identifier. */
    public static final PropertyType<ItemTag> ITEM =
            converting(ItemTag.class, ItemTag::new, "an item");

    /** Accepts a MapTag, or 'key=value;...' text. */
    public static final PropertyType<MapTag> MAP =
            converting(MapTag.class, MapTag::new, "a map of key=value pairs");

    /** Accepts a ListTag, or a pipe-separated list. A single value becomes a one-element list. */
    public static final PropertyType<ListTag> LIST =
            converting(ListTag.class, ListTag::new, "a list");

    /** Accepts any tag untouched, for properties that genuinely take anything. */
    public static final PropertyType<AbstractTag> ANY = new PropertyType<>() {
        @Override
        public AbstractTag parse(@NotNull AbstractTag input) {
            return input;
        }

        @Override
        public @NotNull AbstractTag write(@NotNull AbstractTag value) {
            return value;
        }

        @Override
        public @NotNull Class<? extends AbstractTag> tagClass() {
            return AbstractTag.class;
        }

        @Override
        public @NotNull String describeInput() {
            return "any value";
        }
    };

    /**
     * Accepts any constant of {@code enumClass}, case-insensitively, tolerating a
     * "minecraft:" namespace and '-'/' ' in place of '_'. The error message lists the valid values.
     */
    public static <E extends Enum<E>> PropertyType<E> enumOf(@NotNull Class<E> enumClass) {
        E[] constants = enumClass.getEnumConstants();
        Map<String, E> lookup = new HashMap<>(constants.length * 2);
        StringJoiner names = new StringJoiner(", ");
        for (E constant : constants) {
            lookup.put(constant.name().toUpperCase(Locale.ROOT), constant);
            names.add(constant.name());
        }
        String description = "one of: " + names;

        return new PropertyType<>() {
            @Override
            public E parse(@NotNull AbstractTag input) {
                String raw = input.identify().trim();
                int colon = raw.indexOf(':');
                if (colon >= 0) raw = raw.substring(colon + 1);
                return lookup.get(raw.toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_'));
            }

            @Override
            public @NotNull AbstractTag write(@NotNull E value) {
                return new ElementTag(value.name());
            }

            @Override
            public @NotNull Class<? extends AbstractTag> tagClass() {
                return ElementTag.class;
            }

            @Override
            public @NotNull String describeInput() {
                return description;
            }
        };
    }

    /** Wraps a numeric type so out-of-range input is rejected instead of silently clamped. */
    public static <N extends Number & Comparable<N>> PropertyType<N> range(
            @NotNull PropertyType<N> base, @NotNull N min, @NotNull N max) {

        return new PropertyType<>() {
            @Override
            public N parse(@NotNull AbstractTag input) {
                N value = base.parse(input);
                if (value == null) return null;
                return value.compareTo(min) < 0 || value.compareTo(max) > 0 ? null : value;
            }

            @Override
            public @NotNull AbstractTag write(@NotNull N value) {
                return base.write(value);
            }

            @Override
            public @NotNull Class<? extends AbstractTag> tagClass() {
                return base.tagClass();
            }

            @Override
            public @NotNull String describeInput() {
                return base.describeInput() + " between " + min + " and " + max;
            }
        };
    }

    /**
     * Builds a type out of an existing one by mapping the parsed value to another shape.
     * Use it when a property's Java type is not a tag type, for example a {@code Vector3f}
     * built from a LocationTag.
     */
    public static <A, B> PropertyType<B> mapping(
            @NotNull PropertyType<A> base,
            @NotNull Function<A, B> forward,
            @NotNull Function<B, A> backward) {

        return new PropertyType<>() {
            @Override
            public B parse(@NotNull AbstractTag input) {
                A parsed = base.parse(input);
                return parsed == null ? null : forward.apply(parsed);
            }

            @Override
            public @NotNull AbstractTag write(@NotNull B value) {
                return base.write(backward.apply(value));
            }

            @Override
            public @NotNull Class<? extends AbstractTag> tagClass() {
                return base.tagClass();
            }

            @Override
            public @NotNull String describeInput() {
                return base.describeInput();
            }
        };
    }

    /**
     * The input that means "unset this property" rather than a value: {@code !}, the same symbol
     * the {@code def} command uses to undefine.
     *
     * <p>A word like "none" would be ambiguous - it is a real villager profession and a real
     * armor stand pose - so the clear input is deliberately a symbol no value set contains. Only
     * properties that opt in treat it as a clear; everywhere else it is ordinary input and gets
     * rejected by the type as usual.</p>
     */
    public static boolean isClearInput(@NotNull AbstractTag input) {
        return input.identify().trim().equals(CLEAR_SYMBOL);
    }

    /** The symbol that unsets a clearable property. */
    public static final String CLEAR_SYMBOL = "!";

    private static ElementTag asElement(AbstractTag input) {
        return input instanceof ElementTag element ? element : new ElementTag(input.identify());
    }

    private static <N> PropertyType<N> numeric(String description,
                                               Function<ElementTag, N> reader,
                                               Function<N, AbstractTag> writer) {
        return new PropertyType<>() {
            @Override
            public N parse(@NotNull AbstractTag input) {
                return reader.apply(asElement(input));
            }

            @Override
            public @NotNull AbstractTag write(@NotNull N value) {
                return writer.apply(value);
            }

            @Override
            public @NotNull Class<? extends AbstractTag> tagClass() {
                return ElementTag.class;
            }

            @Override
            public @NotNull String describeInput() {
                return description;
            }
        };
    }

    private static <T extends AbstractTag> PropertyType<T> converting(
            Class<T> type, Function<String, T> parser, String description) {

        return new PropertyType<>() {
            @Override
            public T parse(@NotNull AbstractTag input) {
                if (type.isInstance(input)) return type.cast(input);
                try {
                    return parser.apply(input.identify());
                } catch (Exception ignored) {
                    return null;
                }
            }

            @Override
            public @NotNull AbstractTag write(@NotNull T value) {
                return value;
            }

            @Override
            public @NotNull Class<? extends AbstractTag> tagClass() {
                return type;
            }

            @Override
            public @NotNull String describeInput() {
                return description;
            }
        };
    }

    static String valuesOf(Class<? extends Enum<?>> enumClass) {
        return Arrays.toString(enumClass.getEnumConstants());
    }
}
