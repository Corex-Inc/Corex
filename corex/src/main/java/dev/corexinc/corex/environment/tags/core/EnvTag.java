package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.utils.scripts.EnvManager;
import org.jspecify.annotations.NonNull;

/* @[objecttype]
 *
 * @Name EnvTag
 * @Prefix env
 * @Format
 * The identity format for secrets is simply the env key (as defined by the file 'secrets.env' in the Corex folder).
 *
 * @Description
 * A EnvTag represents a value that should never be exposed to logs or tags.
 * For example: authorization tokens, API keys, etc.
 *
 * A EnvTag is made of a 'key', and a 'value'.
 * The key is a simple name, like 'my_bot_token', that is safe to show in logs/etc.
 * The value is the actual internal data that must be kept secret, often a generated code.
 *
 * The keys and values must be defined in the 'secrets.env' file inside the Corex folder.
 * The contents of that file would look something like:
 * my_bot_token=abc123.123abc
 * my_api_key=1a2b3c4d5e6f
 *
 * The above example defines EnvTag 'my_bot_token' as 'abc123.123abc',
 * meaning you could then use '<env[my_bot_token]>' in the input to a command that parses secrets to have it understand the real value to input should be 'abc123.123abc'
 * However if you use the same tag in for example a narrate command, it would just narrate 'env@my_bot_token', keeping your real value safe.
 *
 * There is intentionally no tag that can read the value of a secret.
 *
 * You can reload the secrets file via "/run reload"
 */
public class EnvTag implements AbstractTag {

    private static final String prefix = "env";
    private final String key;
    private final String hiddenValue;

    public static final TagProcessor<EnvTag> TAG_PROCESSOR = new TagProcessor<>();

    public static void register() {

        BaseTagProcessor.registerBaseTag(prefix, (attr) -> {
            if (attr.hasParam()) return new EnvTag(attr.getParam());
            return null;
        });

        ObjectFetcher.registerFetcher(prefix, EnvTag::new);

        /* @doc tag
         *
         * @Name key
         * @RawName <EnvTag.key>
         * @Object EnvTag
         * @ReturnType ElementTag
         * @NoArg
         * @Description
         * Returns the env key for this env object.
         *
         * @Implements SecretTag.key
         */
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