package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/* @doc object
 *
 * @Name ContextTag
 * @Prefix context
 *
 * @Format
 * The ContextTag is accessed implicitly as `context@` within a script.
 * Individual context values are then accessed using dot notation: `context.keyName`.
 *
 * @Description
 * A ContextTag provides dynamic access to data relevant to the current script execution context,
 * typically within an event or a script queue. It acts as a temporary store for information
 * passed into or generated during an event, allowing various pieces of data to be accessed using simple keys.
 * This is especially useful for event scripts to retrieve details about the event that triggered them.
 *
 * @Usage
 * // Access a value 'targetEntity' provided by an event context, displaying its name.
 * - narrate "The target is: <context.targetEntity.name>"
 *
 * // Use a numerical value 'cooldownTime' from the context as an argument for another script.
 * - run MyCooldownScript def.cooldownDuration:<context.cooldownTime>
 *
 * // Check if a specific context key 'myFlag' exists and is true.
 * - if <context.myFlag>:
 *   - narrate "My flag is set in this context!"
 */
public class ContextTag implements AbstractTag {

    private final String prefix = "context";
    private final Map<String, AbstractTag> contextData = new HashMap<>();

    public static final TagProcessor<ContextTag> TAG_PROCESSOR = new TagProcessor<>();
    public ContextTag() {}

    public static void register() {
        BaseTagProcessor.registerBaseTag("context", attr -> {
            if (attr.getQueue().getContext() != null) {
                return attr.getQueue().getContext();
            }
            return null;
        });
    }

    public ContextTag put(String key, AbstractTag value) {
        if (value != null) {
            contextData.put(key, value);
        }
        return this;
    }

    public AbstractTag get(String key) {
        return contextData.get(key);
    }

    @Override public @NonNull String getPrefix() { return prefix; }
    @Override public @NonNull String identify() { return prefix + "@"; }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        AbstractTag result = TAG_PROCESSOR.process(this, attribute);
        if (result != null) return result;

        String key = attribute.getName();
        if (contextData.containsKey(key)) {
            attribute.fulfill(1);
            AbstractTag value = contextData.get(key);
            if (attribute.hasNext()) {
                return value.getAttribute(attribute);
            }
            return value;
        }

        return null;
    }

    @Override
    public @Nullable String getTestValue() {
        return null;
    }

    @Override
    public @NonNull TagProcessor<? extends AbstractTag> getProcessor() {
        return TAG_PROCESSOR;
    }
}