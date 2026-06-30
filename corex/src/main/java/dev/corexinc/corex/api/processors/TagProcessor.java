package dev.corexinc.corex.api.processors;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.utils.ServerVersion;
import org.jetbrains.annotations.ApiStatus.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Manages the registration and execution of sub-tags (attributes) for a specific {@link AbstractTag} adapters.
 *
 * <p>Each custom tag class (e.g., {@code PlayerTag}) should hold a static instance of this processor
 * to handle its own logic chains. This promotes a modular architecture where each object type
 * defines its own "vocabulary".</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class MyTag implements AbstractTag {
 *     public static final TagProcessor<MyTag> PROCESSOR = new TagProcessor<>();
 *
 *     public static void register() {
 *         PROCESSOR.registerTag(ElementTag.class, "name", (attribute, object) -> {
 *             return new ElementTag(object.getName());
 *         });
 *     }
 *
 *     @Override
 *     public AbstractTag getAttribute(Attribute attribute) {
 *         return PROCESSOR.process(this, attribute);
 *     }
 * }
 * }</pre>
 *
 * @param <T> the type of {@link AbstractTag} this processor is associated with.
 * @since 1.0.0
 */
@AvailableSince("1.0.0")
public final class TagProcessor<T extends AbstractTag> {

    /**
     * Internal registry of sub-tags mapped by their names.
     */
    private final Map<String, TagData<T>> registeredTags = new HashMap<>();

    /**
     * Whether {@link #process} should fall back to {@link GlobalTagProcessor#PROCESSOR}
     * when no local sub-tag matches the attribute name. {@code true} by default.
     */
    private boolean useGlobalTags = true;

    /**
     * Disables fallthrough to {@link GlobalTagProcessor} for this processor instance.
     *
     * <p>By default, if no locally registered sub-tag matches an attribute name,
     * {@link #process} delegates to {@link GlobalTagProcessor#PROCESSOR}. Calling this
     * restricts the processor to only its own registered tags.</p>
     *
     * @return {@code this} for chaining.
     */
    public TagProcessor<T> disableGlobalTags() {
        this.useGlobalTags = false;
        return this;
    }

    /**
     * Default constructor for the TagProcessor. Global tag fallthrough is enabled by default;
     * call {@link #disableGlobalTags()} to opt out.
     */
    public TagProcessor() {}

    /**
     * Registers a new sub-tag handler with metadata about the return type.
     *
     * <p>If version constraints ({@link TagRegistration#setAvailableSince} or
     * {@link TagRegistration#setAvailableBefore}) are specified and the current server version
     * does not satisfy them, the tag will not be added to the registry at all.</p>
     *
     * @param <R>        the specific type of tag that the handler returns.
     * @param returnType the class of the returned tag (e.g., {@code ElementTag.class}).
     * @param name       the name of the sub-tag (e.g., {@code "toUppercase"}).
     * @param action     the handler function that receives the current {@link Attribute}
     *                   and the object instance {@code T}.
     * @return a {@link TagRegistration} for fluent configuration of the registered tag.
     * @throws NullPointerException if any of the arguments are {@code null}.
     */
    public <R extends AbstractTag> TagRegistration<T> registerTag(
            @NotNull final Class<R> returnType,
            @NotNull final String name,
            @NotNull final BiFunction<@NotNull Attribute, @NotNull T, R> action
    ) {
        @SuppressWarnings("unchecked")
        TagData<T> data = new TagData<>(returnType, (BiFunction<Attribute, T, AbstractTag>) action);
        return new TagRegistration<>(data, name, registeredTags);
    }

    /**
     * Returns the internal registry of all sub-tags that passed version checks.
     *
     * @return the registered tags map, keyed by tag name.
     */
    public Map<String, TagData<T>> getRegisteredTags() {
        return registeredTags;
    }

    /**
     * Processes an attribute for a given object instance.
     *
     * <p>Looks up the handler by the attribute's name and executes it.
     * Version compatibility is already guaranteed at registration time —
     * if a tag is present in the registry, it has passed all version checks.</p>
     *
     * <p>If the resolving queue ({@link Attribute#getQueue()}) is asynchronous and the
     * matched sub-tag was not marked safe via {@link TagRegistration#setAsyncSafe()},
     * the handler is skipped: an error is reported through {@link Debugger#echoError}
     * and {@code null} is returned instead of invoking it.</p>
     *
     * @param object    the tag instance being processed.
     * @param attribute the current attribute data from the tag chain.
     * @return the resulting {@link AbstractTag} from the handler, {@code null}
     *         if no sub-tag matches the attribute name, or {@code null} if the matched
     *         sub-tag is not async-safe and the queue is running asynchronously.
     */
    @Nullable
    @Contract(pure = true)
    @Internal
    @AvailableSince("1.0.0")
    public AbstractTag process(@NotNull final T object, @NotNull final Attribute attribute) {
        final TagData<T> data = registeredTags.get(attribute.getName());
        if (data != null) {
            final ScriptQueue queue = attribute.getQueue();
            if (queue != null && queue.isAsync() && !data.isAsyncSafe) {
                Debugger.echoError(queue, "Attempt to evaluate a <aqua>sync</aqua> tag '" + attribute.getName()
                        + "' in an <purple>async</purple> queue!");
                Debugger.echoError(queue,
                        "Queue halted immediately for safety.");
                queue.stopEntireQueue();
                return null;
            }
            return data.action.apply(attribute, object);
        }
        if (useGlobalTags && this != GlobalTagProcessor.PROCESSOR) {
            return GlobalTagProcessor.PROCESSOR.process(object, attribute);
        }
        return null;
    }

    /**
     * Data container for a registered sub-tag handler.
     *
     * @param <T> the owner tag type.
     */
    public static final class TagData<T extends AbstractTag> {

        /** The expected return type of this handler. */
        public final Class<? extends AbstractTag> returnType;

        /**
         * Whether this sub-tag is safe to evaluate on an asynchronous {@link ScriptQueue}.
         *
         * <p>{@code false} by default — a tag must explicitly opt in via
         * {@link TagRegistration#setAsyncSafe()}. Tags that touch live Bukkit/world state
         * (e.g. {@code location.power}, anything reading block/entity data off the main
         * thread) must <b>not</b> opt in. Pure computation on already-resolved data
         * (string/number/list manipulation, etc.) is generally safe.</p>
         *
         * <p>Enforced in {@link #process}: if the owning queue is async
         * ({@link ScriptQueue#isAsync()}) and this is {@code false}, the handler is not
         * invoked — an error is logged via {@link Debugger#echoError} and {@code null}
         * is returned instead.</p>
         */
        public boolean isAsyncSafe = false;

        /**
         * Sample input value used by the auto-testing framework,
         * set via {@link TagRegistration#test}.
         */
        public String testParam = null;

        /**
         * Expected attribute chain the auto-testing framework should traverse,
         * set via {@link TagRegistration#test}.
         */
        public String[] testChain = null;

        /**
         * If {@code true}, this sub-tag is excluded from auto-testing.
         * Set via {@link TagRegistration#ignoreTest()}.
         */
        public boolean skipTest = false;

        /**
         * Minimum server version required for this sub-tag (inclusive).
         * {@code null} means no lower bound.
         *
         * @see TagRegistration#setAvailableSince(String)
         */
        @Nullable
        public String availableSince = null;

        /**
         * Upper server version bound for this sub-tag (exclusive).
         * {@code null} means no upper bound.
         *
         * @see TagRegistration#setAvailableBefore(String)
         */
        @Nullable
        public String availableBefore = null;

        /** The functional logic of this sub-tag. */
        private final BiFunction<Attribute, T, AbstractTag> action;

        private TagData(Class<? extends AbstractTag> returnType, BiFunction<Attribute, T, AbstractTag> action) {
            this.returnType = returnType;
            this.action = action;
        }
    }

    /**
     * Fluent builder returned by {@link #registerTag} for configuring a sub-tag after registration.
     *
     * <p>Calling {@link #setAvailableSince} or {@link #setAvailableBefore} triggers an immediate re-evaluation:
     * the tag is either added to or removed from the registry depending on the current server version.</p>
     *
     * @param <T> the owner tag type.
     */
    public static class TagRegistration<T extends AbstractTag> {

        private final TagData<T> data;
        private final String name;
        private final Map<String, TagData<T>> registry;

        TagRegistration(TagData<T> data, String name, Map<String, TagData<T>> registry) {
            this.data = data;
            this.name = name;
            this.registry = registry;
            commit();
        }

        /**
         * Evaluates version constraints and either adds or removes the tag from the registry.
         */
        private void commit() {
            if (isVersionCompatible()) {
                registry.put(name, data);
            } else {
                registry.remove(name);
            }
        }

        /**
         * Returns {@code true} if the current server version satisfies
         * both {@link TagData#availableSince} and {@link TagData#availableBefore} constraints.
         *
         * @return {@code true} if version constraints are satisfied.
         */
        private boolean isVersionCompatible() {
            if (data.availableSince != null && !ServerVersion.isAtLeast(data.availableSince)) {
                return false;
            }
            return data.availableBefore == null || !ServerVersion.isAtLeast(data.availableBefore);
        }

        /**
         * Sets the test parameter and chain for automated testing of this sub-tag.
         *
         * @param param the test input value.
         * @param chain the expected attribute chain to traverse.
         * @return {@code this} for chaining.
         */
        public TagRegistration<T> test(String param, String... chain) {
            this.data.testParam = param;
            this.data.testChain = chain;
            return this;
        }

        /**
         * Marks this sub-tag as excluded from automated testing.
         *
         * @return {@code this} for chaining.
         */
        public TagRegistration<T> ignoreTest() {
            this.data.skipTest = true;
            return this;
        }

        /**
         * Registers this sub-tag only on servers running the given version <b>or newer</b> (inclusive).
         *
         * <p>Example:</p>
         * <pre>{@code
         * PROCESSOR.registerTag(ElementTag.class, "customModelData", (attr, obj) -> ...)
         *          .setAvailableSince("1.21.4");
         * // Not registered on 1.21.3 and below.
         * }</pre>
         *
         * @param version minimum required server version, e.g. {@code "1.21.4"}.
         * @return {@code this} for chaining.
         */
        public TagRegistration<T> setAvailableSince(@NotNull String version) {
            this.data.availableSince = version;
            commit();
            return this;
        }

        /**
         * Marks this sub-tag as safe to evaluate on an asynchronous {@link ScriptQueue}.
         *
         * <p>Only call this for handlers that don't touch live Bukkit/world state —
         * pure computation on already-resolved tag data. Do <b>not</b> call it for tags
         * like {@code location.power} that read live block/chunk/entity state, since
         * that's unsafe (and often crash-prone) off the main thread.</p>
         *
         * <p>Example:</p>
         * <pre>{@code
         * // Safe: pure string math, no world access.
         * PROCESSOR.registerTag(ElementTag.class, "toUppercase", (attr, obj) -> ...)
         *          .setAsyncSafe();
         *
         * // Unsafe (default): reads live block state, never call setAsyncSafe() here.
         * PROCESSOR.registerTag(ElementTag.class, "power", (attr, obj) ->
         *          new ElementTag(obj.getBlock().getBlockPower()));
         * }</pre>
         *
         * @return {@code this} for chaining.
         */
        public TagRegistration<T> setAsyncSafe() {
            this.data.isAsyncSafe = true;
            return this;
        }

        /**
         * Registers this sub-tag only on servers running versions <b>strictly older</b>
         * than the given version (exclusive).
         *
         * <p>Example:</p>
         * <pre>{@code
         * PROCESSOR.registerTag(ElementTag.class, "legacyColor", (attr, obj) -> ...)
         *          .setAvailableBefore("1.21.4");
         * // Not registered on 1.21.4 and above.
         * }</pre>
         *
         * @param version the first version where this sub-tag is no longer available.
         * @return {@code this} for chaining.
         */
        public TagRegistration<T> setAvailableBefore(@NotNull String version) {
            this.data.availableBefore = version;
            commit();
            return this;
        }
    }
}