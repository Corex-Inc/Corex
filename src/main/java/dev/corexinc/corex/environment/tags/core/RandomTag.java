package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import org.bukkit.util.noise.SimplexNoiseGenerator;
import org.jspecify.annotations.NonNull;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/* @doc object
 *
 * @Name RandomTag
 * @Prefix random
 * @Format
 * The identity format for RandomTags is 'random@' followed by an optional numeric seed.
 * For example, 'random@' for a standard random source, or 'random@12345' for a seeded source.
 *
 * @Description
 * A RandomTag represents a source of randomness used to generate values.
 *
 * By default, a RandomTag uses the system's thread-local random generator.
 * If a seed is provided, the generator becomes deterministic, meaning it will produce the exact same
 * sequence of values every time the same seed is used. This is particularly useful for
 * procedural generation or debugging.
 */
public class RandomTag implements AbstractTag {

    private static final String PREFIX = "random";
    public static final TagProcessor<RandomTag> TAG_PROCESSOR = new TagProcessor<>();

    private static final RandomTag SHARED_INSTANCE = new RandomTag();

    private final Random seededRandom;
    private final Long seed;

    public static void register() {
        ObjectFetcher.registerFetcher(PREFIX, raw -> {
            if (raw == null || raw.isEmpty()) return SHARED_INSTANCE;
            try {
                return new RandomTag(Long.parseLong(raw));
            } catch (NumberFormatException e) {
                return SHARED_INSTANCE;
            }
        });

        /* @doc tag
         *
         * @Name uuid
         * @RawName <RandomTag.uuid>
         * @Object RandomTag
         * @ReturnType ElementTag
         * @Description
         * Returns a random unique ID.
         *
         * @Implements util.random_uuid
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "uuid", (attr, obj) ->
                new ElementTag(UUID.randomUUID().toString()));

        /* @doc tag
         *
         * @Name int[]
         * @RawName <RandomTag.int[(<#>)]>
         * @Object RandomTag
         * @ReturnType ElementTag(Number)
         * @Description
         * Returns a random integer number from 0 to max integer.
         *
         * @Implements util.random.int[<#>].to[<#>]
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "int", (attr, obj) -> {
            Random r = obj.getRandom();
            if (!attr.hasParam()) {
                return new ElementTag(r.nextInt());
            }

            int min = new ElementTag(attr.getParam()).asInt();

            /* @doc tag
             *
             * @Name int[].to[]
             * @RawName <RandomTag.int[<#>].to[<#>]>
             * @Object RandomTag
             * @ReturnType ElementTag(Decimal)
             * @Description
             * Returns a random decimal number between the 2 specified decimal numbers, inclusive.
             * @Usage
             * // Will narrate '2', or '5', or '9', or any other int in range.
             * - narrate <util.random.decimal[1].to[10]>
             *
             * @Usage
             * // Will narrate '1457', '9832', or any other int to max.
             * - narrate <util.random.int>
             *
             * @Implements RandomTag.int[<#>].to[<#>]
             */
            if (attr.matchesNext("to") && attr.hasNextParam()) {
                int max = new ElementTag(attr.getNextParam()).asInt();
                attr.fulfill(1);

                if (min >= max) return new ElementTag(min);
                return new ElementTag(r.nextInt(max - min + 1) + min);
            }

            return new ElementTag(r.nextInt(Math.max(1, min)));
        });

        /* @doc tag
         *
         * @Name decimal[]
         * @RawName <RandomTag.decimal>
         * @Object RandomTag
         * @ReturnType ElementTag(Decimal)
         * @Description
         * Returns a random decimal number from 0 to 1.
         *
         * @Implements util.random_decimal
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "decimal", (attr, obj) -> {
            Random r = obj.getRandom();

            if (!attr.hasParam()) {
                return new ElementTag(r.nextDouble());
            }

            double min = new ElementTag(attr.getParam()).asDouble();

            /* @doc tag
             *
             * @Name decimal[].to[]
             * @RawName <RandomTag.decimal[<#.#>].to[<#.#>]>
             * @Object RandomTag
             * @ReturnType ElementTag(Decimal)
             * @Description
             * Returns a random decimal number between the 2 specified decimal numbers, inclusive.
             * @Usage
             * // Will narrate '1.5', or '1.75', or '1.01230123', or any other decimal in range.
             * - narrate <util.random.decimal[1].to[2]>
             *
             * @Implements RandomTag.decimal[<#.#>].to[<#.#>]
             */
            if (attr.matchesNext("to") && attr.hasNextParam()) {
                double max = new ElementTag(attr.getNextParam()).asDouble();
                attr.fulfill(1);

                return new ElementTag(min + (max - min) * r.nextDouble());
            }

            return new ElementTag(r.nextDouble() * min);
        });

        /* @doc tag
         *
         * @Name boolean
         * @RawName <RandomTag.boolean>
         * @Object RandomTag
         * @ReturnType ElementTag(Boolean)
         * @Description
         * Returns a random boolean (true or false). Essentially a coin flip.
         *
         * @Implements util.random_boolean
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "boolean", (attr, obj) ->
                new ElementTag(obj.getRandom().nextBoolean()));

        /* @doc tag
         *
         * @Name simplex[]
         * @RawName <util.random_simplex[x=<#.#>;(y=<#.#>);(z=<#.#>)]>
         * @Object util
         * @ReturnType ElementTag(Decimal)
         * @Description
         * Returns a pseudo-random decimal number from -1 to 1, based on a Simplex Noise algorithm. See {@link url https://en.wikipedia.org/wiki/Simplex_noise}
         * Input map is like "x=1.0", or "x=1.0;y=2.0", or "x=1.0;y=2.0;z=3" or "x=1;y=2;z=3"
         * (That is: 1d, 2d, 3d, or 4d).
         *
         * @Implements util.random_simplex
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "simplex", (attr, obj) -> {
            if (!attr.hasParam()) return new ElementTag(0);

            double x = 0, y = 0, z = 0;
            String param = attr.getParam();

            if (param.contains("=")) {
                if (!param.startsWith("[")) param = "[" + param + "]";
                MapTag map = new MapTag(param);

                AbstractTag tX = map.getObject("x");
                if (tX != null) x = new ElementTag(tX.identify()).asDouble();

                AbstractTag tY = map.getObject("y");
                if (tY != null) y = new ElementTag(tY.identify()).asDouble();

                AbstractTag tZ = map.getObject("z");
                if (tZ != null) z = new ElementTag(tZ.identify()).asDouble();
            } else {
                x = new ElementTag(param).asDouble();
            }

            return new ElementTag(SimplexNoiseGenerator.getNoise(x, y, z));
        }).test("x=1;y=2;z=14");
    }

    public RandomTag(String raw) {
        if (raw == null || raw.isEmpty() || raw.equals("test_init")) {
            this.seededRandom = null;
            this.seed = null;
        } else {
            long s;
            try { s = Long.parseLong(raw); } catch (Exception e) { s = 0; }
            this.seed = s;
            this.seededRandom = new Random(s);
        }
    }
    private RandomTag() {
        this.seededRandom = null;
        this.seed = null;
    }

    public RandomTag(long seed) {
        this.seededRandom = new Random(seed);
        this.seed = seed;
    }

    public static RandomTag getShared() {
        return SHARED_INSTANCE;
    }

    public Random getRandom() {
        return seededRandom != null ? seededRandom : ThreadLocalRandom.current();
    }

    @Override public @NonNull String getPrefix() { return PREFIX; }

    @Override
    public @NonNull String identify() {
        return seed != null ? PREFIX + "@" + seed : PREFIX + "@";
    }

    @Override public AbstractTag getAttribute(@NonNull Attribute attribute) { return TAG_PROCESSOR.process(this, attribute); }
    @Override public @NonNull TagProcessor<RandomTag> getProcessor() { return TAG_PROCESSOR; }
    @Override public String getTestValue() { return PREFIX + "@"; }
}