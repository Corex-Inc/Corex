package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.EnvManager;
import org.jspecify.annotations.NonNull;

public class EnvTag implements AbstractTag {

    private static String prefix = "env";
    private final String key;
    private final String hiddenValue;

    public static final TagProcessor<EnvTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {

        BaseTagProcessor.registerBaseTag(prefix, (attr) -> {
            if (attr.hasParam()) return new EnvTag(attr.getParam());
            return null;
        });

        ObjectFetcher.registerFetcher(prefix, EnvTag::new);

        TAG_PROCESSOR.registerTag(ElementTag.class, "key", (attr, obj) -> new ElementTag(obj.getKey()));
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

    @Override
    public @NonNull String identify() {
        return prefix + "@" + key;
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull String getTestValue() {
        return "env@my_secret_password";
    }

    @Override
    public @NonNull TagProcessor<? extends AbstractTag> getProcessor() {
        return TAG_PROCESSOR;
    }
}