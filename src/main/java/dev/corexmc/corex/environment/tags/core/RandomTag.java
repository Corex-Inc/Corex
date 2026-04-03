package dev.corexmc.corex.environment.tags.core;

import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.api.tags.Attribute;
import dev.corexmc.corex.api.processors.TagProcessor;
import dev.corexmc.corex.engine.tags.ObjectFetcher;
import org.jspecify.annotations.NonNull;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class RandomTag implements AbstractTag {

    private static final String PREFIX = "random";
    public static final TagProcessor<RandomTag> PROCESSOR = new TagProcessor<>();

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

        PROCESSOR.registerTag(ElementTag.class, "uuid", (attr, obj) ->
                new ElementTag(UUID.randomUUID().toString()));

        PROCESSOR.registerTag(ElementTag.class, "int", (attr, obj) -> {
            int max = attr.hasParam() ? new ElementTag(attr.getParam()).asInt() : Integer.MAX_VALUE;
            if (max <= 0) max = 1;
            return new ElementTag(obj.getRandom().nextInt(max));
        });

        PROCESSOR.registerTag(ElementTag.class, "decimal", (attr, obj) ->
                new ElementTag(obj.getRandom().nextDouble()));

        PROCESSOR.registerTag(ElementTag.class, "boolean", (attr, obj) ->
                new ElementTag(obj.getRandom().nextBoolean()));
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

    @Override public AbstractTag getAttribute(@NonNull Attribute attribute) { return PROCESSOR.process(this, attribute); }
    @Override public @NonNull TagProcessor<RandomTag> getProcessor() { return PROCESSOR; }
    @Override public String getTestValue() { return PREFIX + "@"; }
}