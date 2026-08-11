package dev.corexinc.corex.engine.queue;

import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A definition whose value changes in place between loop iterations.
 * <p>
 * Loop commands ({@code repeat}, {@code foreach}, {@code while}) store one instance
 * of this class in the queue's definition map once, then mutate it every iteration
 * instead of re-writing the map. {@link ScriptQueue#getDefinition(String)} resolves
 * it to a regular immutable tag via {@link #snapshot()}, so scripts never observe
 * the mutable holder itself.
 *
 * @since 1.0.0
 */
public abstract class MutableDefinition implements AbstractTag {

    /**
     * Returns the current value as a regular immutable tag.
     */
    public abstract AbstractTag snapshot();

    @Override
    public @NotNull String identify() {
        AbstractTag snap = snapshot();
        return snap != null ? snap.identify() : "null";
    }

    @Override
    public @NotNull String getPrefix() {
        AbstractTag snap = snapshot();
        return snap != null ? snap.getPrefix() : "el";
    }

    @Override
    public @Nullable AbstractTag getAttribute(@NotNull Attribute attribute) {
        AbstractTag snap = snapshot();
        return snap != null ? snap.getAttribute(attribute) : null;
    }

    @Override
    public @Nullable String getTestValue() {
        return null;
    }

    @Override
    public @NotNull TagProcessor<? extends AbstractTag> getProcessor() {
        return ElementTag.TAG_PROCESSOR;
    }

    public static final class OfInt extends MutableDefinition {
        public int value;

        public OfInt(int value) {
            this.value = value;
        }

        @Override
        public AbstractTag snapshot() {
            return new ElementTag(value);
        }
    }

    public static final class OfTag extends MutableDefinition {
        public AbstractTag current;

        public OfTag(AbstractTag current) {
            this.current = current;
        }

        @Override
        public AbstractTag snapshot() {
            return current;
        }
    }
}
