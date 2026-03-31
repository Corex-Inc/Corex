package dev.corexmc.corex.environment.tags.core;

import dev.corexmc.corex.Corex;
import dev.corexmc.corex.api.processors.TagProcessor;
import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.api.tags.Attribute;
import org.jspecify.annotations.NonNull;

public class EnvTag implements AbstractTag {

    private static String prefix = "env";
    private final String key;
    private final String hiddenValue;

    public static final TagProcessor<EnvTag> PROCESSOR = new TagProcessor<>();

    public static void register() {

        dev.corexmc.corex.engine.tags.TagManager.registerBaseTag(prefix, (attr) -> {
            if (!attr.hasParam()) return null;
            return new EnvTag(attr.getParam());
        });
    }

    public EnvTag(String key) {
        this.key = key;
        String val = Corex.getInstance().getEnvManager().getSecret(key);
        this.hiddenValue = val != null ? val : "NOT_FOUND";
    }

    public String getSecretValue() {
        return hiddenValue;
    }

    @Override public @NonNull String getPrefix() { return prefix; }
    @Override public @NonNull AbstractTag setPrefix(@NonNull String prefix) { EnvTag.prefix = prefix; return this; }

    @Override
    public @NonNull String identify() {
        return prefix + "@" + key;
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return PROCESSOR.process(this, attribute);
    }

    @Override
    public String getTestValue() {
        return "";
    }

    @Override
    public TagProcessor<? extends AbstractTag> getProcessor() {
        return null;
    }
}