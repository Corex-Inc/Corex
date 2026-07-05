package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.engine.utils.CorexSerializer;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

/* @doc object
 *
 * @Name ComponentTag
 * @Prefix component
 *
 * @Format
 * A ComponentTag wraps a fully-built Adventure text component. It has no plain-text
 * format you write by hand; the engine produces it when a tag chain mixes colored
 * or formatted pieces together (for example a MixedArg built from formatters).
 *
 * @Description
 * Most tags are text and become colored only when sent to a player. A ComponentTag
 * is the exception: it already holds a rich component with colors, click and hover
 * events, fonts and the rest baked in. Commands that draw text (narrate, title,
 * actionbar) render it as-is instead of re-parsing color codes.
 *
 * You rarely construct one on purpose. It shows up as the result of combining
 * formatter output, and it carries that formatting through the rest of the chain
 * without flattening it back to a raw string.
 *
 * @Implements ComponentTag
 */
public class ComponentTag implements AbstractTag {

    private final Component component;

    public ComponentTag(Component component) {
        this.component = component;
    }

    @Override
    public @NonNull Component asComponent() {
        return this.component;
    }

    public @NonNull String identify() {
        return CorexSerializer.LEGACY.serialize(component);
    }

    @Override
    public @NonNull String getPrefix() {
        return "component";
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return null;
    }

    @Override
    public @Nullable String getTestValue() {
        return "";
    }

    @Override
    public @NonNull TagProcessor<? extends AbstractTag> getProcessor() {
        return null;
    }
}