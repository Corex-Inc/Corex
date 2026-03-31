package dev.corexmc.corex.environment.tags.core;

import dev.corexmc.corex.api.processors.TagProcessor;
import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.api.tags.Attribute;
import dev.corexmc.corex.engine.tags.ObjectFetcher;
import dev.corexmc.corex.engine.tags.TagManager;
import dev.corexmc.corex.engine.utils.EnvManager;
import org.jspecify.annotations.NonNull;

public class EnvTag implements AbstractTag {

    private static String prefix = "env";
    private final String key;
    private final String hiddenValue;

    public static final TagProcessor<EnvTag> PROCESSOR = new TagProcessor<>();

    public static void register() {

        TagManager.registerBaseTag(prefix, (attr) -> {
            if (!attr.hasParam()) return null;
            return new EnvTag(attr.getParam());
        });

        ObjectFetcher.registerFetcher(prefix, EnvTag::new);

        PROCESSOR.registerTag(ElementTag.class, "key", (attr, obj) -> {
           return new ElementTag(obj.getKey());
        });
    }

    public EnvTag(String key) {
        this.key = key;
        String value = EnvManager.getSecret(key);
        this.hiddenValue = value != null ? value : "NOT_FOUND";
    }

    public String getKey() {
        return key;
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
        return "env@my_secret_password";
    }

    @Override
    public TagProcessor<? extends AbstractTag> getProcessor() {
        return PROCESSOR;
    }
}