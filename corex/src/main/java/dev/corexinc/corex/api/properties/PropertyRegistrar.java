package dev.corexinc.corex.api.properties;

import dev.corexinc.corex.api.processors.MechanismProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Registers a tag and its matching mechanism from a single declaration.
 *
 * <p>Most object data is a property: something a script can both read ({@code <entity.billboard>})
 * and write ({@code adjust <[e]> billboard:center}). Declared by hand that is two registrations,
 * two null-checks, a cast, an enum parse, and two doc blocks - repeated for every field. A
 * PropertyRegistrar collapses that to the parts that actually differ: the name, the
 * {@link PropertyType}, how to read the value, and how to write it.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * PROPERTIES.property("billboard", PropertyTypes.enumOf(Display.Billboard.class))
 *         .read(entity -> entity.asDisplay() == null ? null : entity.asDisplay().getBillboard())
 *         .write(LiveEntityView::setBillboard)
 *         .register();
 * }</pre>
 *
 * <p>The generated mechanism parses and validates the input through the type before the writer
 * runs, so a writer never sees a bad value and never needs to check for one. Invalid input is
 * reported with the property name and the accepted input shape, and the write is skipped.</p>
 *
 * <p>A property may be read-only (no {@link Definition#write}) or write-only
 * (no {@link Definition#read}).</p>
 *
 * @param <T> the tag type that owns these properties.
 * @param <W> the "write target" a mechanism applies to. For a plain object this is {@code T}
 *            itself; for entities it is the live view shared by real and packet entities.
 * @since 1.0.0
 */
@AvailableSince("1.0.0")
public final class PropertyRegistrar<T extends AbstractTag, W> {

    /**
     * Routes a validated write to its target.
     *
     * <p>This is the hook that lets an object decide what "applying" means - a spawned entity
     * mutates immediately while an unspawned blueprint only records the value, and both are
     * expressed by one binder rather than by every property.</p>
     */
    @FunctionalInterface
    public interface Binder<T extends AbstractTag, W> {
        /**
         * Records the accepted value on the object and hands back the live target to write to.
         *
         * <p>Returning the target instead of taking a callback keeps the write path allocation
         * free: the registrar calls the writer directly, so no closure is captured per adjust.</p>
         *
         * @param object   the object being adjusted.
         * @param property the property name, for objects that record values by name.
         * @param value    the accepted input, to store as-is when the object records values.
         *                 This is the original tag rather than a re-serialized copy, so a rich
         *                 value (a component carrying a player head, say) survives being recorded
         *                 on a blueprint and applied later. Reads normalize it back through the
         *                 property type, so storage stays lossless without leaking raw spellings.
         * @return the live target to apply the write to, or {@code null} when the object has no
         *         live state (an unspawned blueprint) and recording the value is the whole job.
         */
        @Nullable
        W bind(@NotNull T object, @NotNull String property, @NotNull AbstractTag value);
    }

    private final TagProcessor<T> tagProcessor;
    private final MechanismProcessor<T> mechanismProcessor;
    private final Binder<T, W> binder;
    private final String objectName;
    private BiFunction<T, String, AbstractTag> recordedValues;

    public PropertyRegistrar(@NotNull String objectName,
                             @NotNull TagProcessor<T> tagProcessor,
                             @NotNull MechanismProcessor<T> mechanismProcessor,
                             @NotNull Binder<T, W> binder) {
        this.objectName = objectName;
        this.tagProcessor = tagProcessor;
        this.mechanismProcessor = mechanismProcessor;
        this.binder = binder;
    }

    /**
     * Supplies a fallback for reads: when the live value is unavailable (an unspawned blueprint,
     * or a packet entity whose state cannot be queried back), the tag returns the value previously
     * written for that property instead of nothing.
     */
    public PropertyRegistrar<T, W> withRecordedValues(@NotNull BiFunction<T, String, AbstractTag> reader) {
        this.recordedValues = reader;
        return this;
    }

    /**
     * Begins a property declaration. Call {@link Definition#register()} to commit it.
     */
    public <V> Definition<T, W, V> property(@NotNull String name, @NotNull PropertyType<V> type) {
        return new Definition<>(this, name, type);
    }

    /**
     * A property being declared.
     */
    public static final class Definition<T extends AbstractTag, W, V> {

        private final PropertyRegistrar<T, W> owner;
        private final String name;
        private final PropertyType<V> type;
        private Function<T, V> reader;
        private BiConsumer<W, V> writer;
        private boolean asyncSafe = false;
        private boolean testable = false;
        private boolean clearable = false;
        private String availableSince;

        private Definition(PropertyRegistrar<T, W> owner, String name, PropertyType<V> type) {
            this.owner = owner;
            this.name = name;
            this.type = type;
        }

        /**
         * How to read the current value. Returning {@code null} falls back to the recorded value
         * (see {@link #withRecordedValues}), or yields nothing when there is none.
         */
        public Definition<T, W, V> read(@NotNull Function<T, V> reader) {
            this.reader = reader;
            return this;
        }

        /**
         * How to apply a parsed value. The value has already been validated by the property type.
         */
        public Definition<T, W, V> write(@NotNull BiConsumer<W, V> writer) {
            this.writer = writer;
            return this;
        }

        /**
         * Lets the mechanism take {@code !} to unset the value, passing {@code null} to the
         * writer. Only declare this when the underlying setter accepts null - an undyed shulker
         * or a display with no glow override, say.
         */
        public Definition<T, W, V> clearable() {
            this.clearable = true;
            return this;
        }

        /** Marks the generated tag safe to evaluate on an async queue. */
        public Definition<T, W, V> asyncSafe() {
            this.asyncSafe = true;
            return this;
        }

        /**
         * Opts the generated tag into the auto-test framework. Properties are excluded by default
         * because most of them only exist on one entity/item shape, which the single test sample
         * cannot satisfy.
         */
        public Definition<T, W, V> testable() {
            this.testable = true;
            return this;
        }

        /** Registers the tag only on server versions at or above {@code version}. */
        public Definition<T, W, V> availableSince(@NotNull String version) {
            this.availableSince = version;
            return this;
        }

        /**
         * Commits the declaration, registering the tag, the mechanism, or both.
         */
        @SuppressWarnings("unchecked")
        public void register() {
            if (reader != null) {
                Class<AbstractTag> returnType = (Class<AbstractTag>) type.tagClass();
                TagProcessor.TagRegistration<T> registration = owner.tagProcessor.registerTag(
                        returnType, name, (attribute, object) -> {
                            V value = reader.apply(object);
                            if (value != null) return type.write(value);
                            return owner.recorded(object, name, type);
                        });
                if (asyncSafe) registration.setAsyncSafe();
                if (availableSince != null) registration.setAvailableSince(availableSince);
                if (!testable) registration.ignoreTest();
            }

            if (writer != null) {
                owner.mechanismProcessor.registerMechanism(name, (object, raw) -> {
                    if (clearable && PropertyTypes.isClearInput(raw)) {
                        W cleared = owner.binder.bind(object, name, raw);
                        if (cleared != null) writer.accept(cleared, null);
                        return object;
                    }

                    V value = type.parse(raw);
                    if (value == null) {
                        Debugger.echoError(null, "Invalid input '" + raw.identify() + "' for mechanism '"
                                + owner.objectName + "." + name + "' - expected " + type.describeInput() + ".");
                        return object;
                    }
                    W target = owner.binder.bind(object, name, raw);
                    if (target != null) writer.accept(target, value);
                    return object;
                });
            }
        }
    }

    @Nullable
    private <V> AbstractTag recorded(T object, String property, PropertyType<V> type) {
        if (recordedValues == null) return null;
        AbstractTag stored = recordedValues.apply(object, property);
        if (stored == null) return null;
        V parsed = type.parse(stored);
        return parsed != null ? type.write(parsed) : stored;
    }
}
