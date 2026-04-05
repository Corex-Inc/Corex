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

        TAG_PROCESSOR.registerTag(ElementTag.class, "uuid", (attr, obj) ->
                new ElementTag(UUID.randomUUID().toString()));

        TAG_PROCESSOR.registerTag(ElementTag.class, "int", (attr, obj) -> {
            Random r = obj.getRandom();
            if (!attr.hasParam()) {
                return new ElementTag(r.nextInt());
            }

            int min = new ElementTag(attr.getParam()).asInt();

            if (attr.matchesNext("to") && attr.hasNextParam()) {
                int max = new ElementTag(attr.getNextParam()).asInt();
                attr.fulfill(1);

                if (min >= max) return new ElementTag(min);
                return new ElementTag(r.nextInt(max - min + 1) + min);
            }

            return new ElementTag(r.nextInt(Math.max(1, min)));
        });

        TAG_PROCESSOR.registerTag(ElementTag.class, "decimal", (attr, obj) -> {
            Random r = obj.getRandom();

            if (!attr.hasParam()) {
                return new ElementTag(r.nextDouble());
            }

            double min = new ElementTag(attr.getParam()).asDouble();

            if (attr.matchesNext("to") && attr.hasNextParam()) {
                double max = new ElementTag(attr.getNextParam()).asDouble();
                attr.fulfill(1);

                return new ElementTag(min + (max - min) * r.nextDouble());
            }

            return new ElementTag(r.nextDouble() * min);
        });

        TAG_PROCESSOR.registerTag(ElementTag.class, "boolean", (attr, obj) ->
                new ElementTag(obj.getRandom().nextBoolean()));

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