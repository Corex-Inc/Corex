package dev.corexinc.corex.api.processors;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Manages the registration and execution of sub-tags (attributes) for a specific {@link AbstractTag} implementation.
 *
 * <p>Each custom tag class (e.g., {@code PlayerTag}) should hold a static instance of this processor
 * to handle its own logic chains. This promotes a modular architecture where each object type
 * defines its own "vocabulary".</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class MyTag implements AbstractTag {
 *     // 1. Create the processor
 *     public static final TagProcessor<MyTag> PROCESSOR = new TagProcessor<>();
 *
 *     public static void register() {
 *         // 2. Register sub-tags
 *         // This handles <mytag.name>
 *         PROCESSOR.registerTag(ElementTag.class, "name", (attribute, object) -> {
 *             return new ElementTag(object.getName());
 *         });
 *     }
 *
 *     @Override
 *     public AbstractTag getAttribute(Attribute attribute) {
 *         // 3. Delegate execution to the processor
 *         return PROCESSOR.process(this, attribute);
 *     }
 * }
 * }</pre>
 *
 * @param <T> the type of {@link AbstractTag} this processor is associated with.
 * @since 1.0.0
 */
@ApiStatus.AvailableSince("1.0.0")
public final class TagProcessor<T extends AbstractTag> {

    /**
     * Internal registry of sub-tags mapped by their lowercase names.
     */
    private final Map<String, TagData<T>> registeredTags = new HashMap<>();

    /**
     * Default constructor for the TagProcessor.
     */
    public TagProcessor() {
    }

    /**
     * Registers a new sub-tag handler with metadata about the return type.
     *
     * <p>This allows the engine to understand the tag hierarchy and perform
     * optimizations or static analysis in the future.</p>
     *
     * @param <R>        the specific type of tag that the handler returns.
     * @param returnType the class of the returned tag (e.g., {@code ElementTag.class}).
     *                   Used for documentation and type-safety metadata.
     * @param name       the name of the sub-tag (e.g., "to_uppercase").
     *                   It will be stored and matched in <b>lowercase</b>.
     * @param action     the handler function that receives the current {@link Attribute}
     *                   and the object instance {@code T}.
     *
     * @throws NullPointerException if any of the arguments are {@code null}.
     */
    @ApiStatus.AvailableSince("1.0.0")
    public <R extends AbstractTag> TagRegistration<T> registerTag(
            @NotNull final Class<R> returnType,
            @NotNull final String name,
            @NotNull final BiFunction<@NotNull Attribute, @NotNull T, R> action
    ) {
        @SuppressWarnings("unchecked")
        TagData<T> data = new TagData<>(returnType, (BiFunction<Attribute, T, AbstractTag>) action);
        registeredTags.put(name, data);
        return new TagRegistration<>(data);
    }

    public Map<String, TagData<T>> getRegisteredTags() { // Todo дописать документацию
        return registeredTags;
    }

    /**
     * Processes an attribute for a given object instance.
     *
     * <p>This method is typically invoked inside an {@link AbstractTag#getAttribute(Attribute)}
     * implementation. It looks up the handler by the attribute's name and executes the
     * associated logic.</p>
     *
     * @param object    the tag instance being processed.
     * @param attribute the current attribute data from the tag chain.
     * @return the resulting {@link AbstractTag} from the handler, or {@code null}
     *         if no sub-tag matches the name.
     */
    @Nullable
    @Contract(pure = true)
    @ApiStatus.Internal
    @ApiStatus.AvailableSince("1.0.0")
    public AbstractTag process(@NotNull final T object, @NotNull final Attribute attribute) {
        final TagData<T> data = registeredTags.get(attribute.getName());
        if (data != null) {
            return data.action.apply(attribute, object);
        }
        return null;
    }

    /**
     * Private data container for registered sub-tag handlers.
     *
     * @param <T> the owner tag type.
     */
    public  static final class TagData<T extends AbstractTag> {
        /**
         * The expected type of the object returned by this handler.
         */
        public final Class<? extends AbstractTag> returnType;
        public boolean isStatic = false;
        public String testParam = null;
        public String[] testChain = null;
        public boolean skipTest = false;

        /**
         * The functional logic of the sub-tag.
         */
        private final BiFunction<Attribute, T, AbstractTag> action;

        private TagData(Class<? extends AbstractTag> returnType, BiFunction<Attribute, T, AbstractTag> action) {
            this.returnType = returnType;
            this.action = action;
        }
    }

    public static class TagRegistration<T extends AbstractTag> {
        private final TagData<T> data;

        TagRegistration(TagData<T> data) {
            this.data = data;
        }

        public TagRegistration<T> test(String param, String... chain) {
            this.data.testParam = param;
            this.data.testChain = chain;
            return this;
        }

        public TagRegistration<T> ignoreTest() {
            this.data.skipTest = true;
            return this;
        }
    }
}