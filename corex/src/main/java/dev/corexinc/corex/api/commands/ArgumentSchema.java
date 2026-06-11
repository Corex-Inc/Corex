package dev.corexinc.corex.api.commands;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Defines a reusable, immutable argument schema for a command.
 *
 * <p>Create it once as a {@code static final} field in your command class,
 * then call {@link #bind(Instruction, ScriptQueue)} inside {@code run()} to get a
 * fully-validated {@link ArgumentSet}.  All type-checking, casting, and error reporting
 * happens inside {@code bind()} — returning {@code null} on any failure.
 *
 * <h3>Quick example</h3>
 * <pre>{@code
 * private static final ArgSchema SCHEMA = ArgSchema.of()
 *     .requirePrefix("target",  PlayerTag.class,  PlayerTag::valueOf)
 *     .requirePrefix("model",   ElementTag.class)
 *     .optionalPrefix("loop",   ElementTag.class, "PLAY_ONCE")
 *     .requireLinear(0,         ElementTag.class)
 *     .build();
 *
 * public void run(ScriptQueue queue, Instruction instruction) {
 *     ArgumentSet args = SCHEMA.bind(instruction, queue);
 *     if (args == null) return;
 *
 *     PlayerTag  player = args.prefix("target");
 *     ElementTag loop   = args.prefix("loop");   // never null — has default
 *     ElementTag first  = args.linear(0);
 * }
 * }</pre>
 *
 * <p><b>Manual checking still works</b> — this system is completely opt-in.
 * If you skip the schema, {@link Instruction#getPrefix} / {@link Instruction#getLinear}
 * behave exactly as before.
 */
public final class ArgumentSchema {

    private enum ArgumentKind { PREFIX, LINEAR, FLAG }

    private record ArgumentDef(
            ArgumentKind kind,
            String  key,
            int linearIndex,
            Class<? extends AbstractTag> type,
            Function<String, ? extends AbstractTag> parser,
            boolean required,
            String  defaultRaw
    ) {}

    private final List<ArgumentDef> defs;

    private ArgumentSchema(List<ArgumentDef> defs) {
        this.defs = List.copyOf(defs);
    }

    public static Builder of() {
        return new Builder();
    }

    public static final class Builder {

        private final List<ArgumentDef> defs = new ArrayList<>();

        /**
         * Declares a <b>required</b> prefix argument.
         *
         * <p>The arg is resolved by evaluating the prefix value at runtime.
         * If the evaluated result is not assignable to {@code type}, the raw string
         * is wrapped in an {@link ElementTag} and returned (safe for simple text args).
         *
         * @param key    the prefix name (e.g. {@code "target"})
         * @param type   the expected tag type
         */
        public <T extends AbstractTag> Builder requirePrefix(
                @NotNull String key,
                @NotNull Class<T> type
        ) {
            return requirePrefix(key, type, null);
        }

        /**
         * Declares a <b>required</b> prefix argument with an explicit parser.
         *
         * <p>The parser is invoked when the evaluated tag is not already of {@code type},
         * receiving the raw {@link AbstractTag#identify()} string.
         */
        public <T extends AbstractTag> Builder requirePrefix(
                @NotNull String key,
                @NotNull Class<T> type,
                @Nullable Function<String, T> parser
        ) {
            defs.add(new ArgumentDef(ArgumentKind.PREFIX, key, -1, type, parser, true, null));
            return this;
        }

        /**
         * Declares an <b>optional</b> prefix argument.
         *
         * <p>If absent, {@link ArgumentSet#prefix} returns {@code null}.
         */
        public <T extends AbstractTag> Builder optionalPrefix(
                @NotNull String key,
                @NotNull Class<T> type
        ) {
            return optionalPrefix(key, type, null, null);
        }

        /**
         * Declares an <b>optional</b> prefix argument with a string default.
         *
         * <p>If absent, the default is parsed as an {@link ElementTag} (or via {@code parser}
         * if provided) and returned from {@link ArgumentSet#prefix} — so callers never get {@code null}.
         */
        public <T extends AbstractTag> Builder optionalPrefix(
                @NotNull String key,
                @NotNull Class<T> type,
                @Nullable String defaultRaw
        ) {
            return optionalPrefix(key, type, null, defaultRaw);
        }

        /**
         * Full optional-prefix declaration with explicit parser and default.
         */
        public <T extends AbstractTag> Builder optionalPrefix(
                @NotNull String key,
                @NotNull Class<T> type,
                @Nullable Function<String, T> parser,
                @Nullable String defaultRaw
        ) {
            defs.add(new ArgumentDef(ArgumentKind.PREFIX, key, -1, type, parser, false, defaultRaw));
            return this;
        }

        /**
         * Declares a <b>required</b> positional argument at {@code index}.
         */
        public <T extends AbstractTag> Builder requireLinear(
                int index,
                @NotNull Class<T> type
        ) {
            return requireLinear(index, type, null);
        }

        /**
         * Declares a <b>required</b> positional argument with an explicit parser.
         */
        public <T extends AbstractTag> Builder requireLinear(
                int index,
                @NotNull Class<T> type,
                @Nullable Function<String, T> parser
        ) {
            defs.add(new ArgumentDef(ArgumentKind.LINEAR, String.valueOf(index), index, type, parser, true, null));
            return this;
        }

        /**
         * Declares an <b>optional</b> positional argument at {@code index}.
         */
        public <T extends AbstractTag> Builder optionalLinear(
                int index,
                @NotNull Class<T> type,
                @Nullable String defaultRaw
        ) {
            defs.add(new ArgumentDef(ArgumentKind.LINEAR, String.valueOf(index), index, type, null, false, defaultRaw));
            return this;
        }

        /**
         * Declares an optional flag: {@code (forced)}.
         * Access via {@link ArgumentSet#flag(String)} — returns {@code true} if present.
         * Flags are not validated at compile time (the compiler already handles them),
         * this just makes the intent explicit in the schema for documentation purposes.
         */
        public Builder optionalFlag(String name) {
            defs.add(new ArgumentDef(ArgumentKind.FLAG, name, -1, ElementTag.class, null, false, null));
            return this;
        }

        /**
         * Finalizes the schema.
         */
        public ArgumentSchema build() {
            return new ArgumentSchema(defs);
        }
    }

    /**
     * Validates and casts all declared arguments against the given {@code instruction}.
     *
     * <p>On the first validation failure an error is logged and {@code null} is returned
     * (matching the "return early" pattern in {@code run()}).
     *
     * @return a fully-populated {@link ArgumentSet}, or {@code null} on any error.
     */
    @Nullable
    public ArgumentSet bind(@NotNull Instruction instruction, @NotNull ScriptQueue queue) {
        ArgumentSet result = new ArgumentSet();

        for (ArgumentDef def : defs) {
            AbstractTag resolved = resolve(def, instruction, queue);
            if (resolved == null && def.required()) {
                if (def.kind() == ArgumentKind.PREFIX) {
                    CorexLogger.error("ARG ERROR: Command '" + instruction.command.getName()
                            + "' requires prefix '" + def.key() + ":<" + def.type().getSimpleName() + ">'!");
                } else {
                    CorexLogger.error("ARG ERROR: Command '" + instruction.command.getName()
                            + "' requires positional arg #" + def.linearIndex()
                            + " of type " + def.type().getSimpleName() + "!");
                }
                return null;
            }
            if (def.kind() == ArgumentKind.PREFIX) result.putPrefix(def.key(), resolved);
            else result.putLinear(def.linearIndex(), resolved);
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractTag> T resolve(ArgumentDef def, Instruction instruction, ScriptQueue queue) {
        AbstractTag raw;

        if (def.kind() == ArgumentKind.PREFIX) {
            CompiledArgument compiled = instruction.prefixArgs.get(def.key());
            if (compiled == null) {
                return defaultOf(def);
            }
            raw = compiled.evaluate(queue);
        } else {
            raw = instruction.getLinearObject(def.linearIndex(), queue);
            if (raw == null) {
                return defaultOf(def);
            }
        }

        if (def.type().isInstance(raw)) {
            return (T) def.type().cast(raw);
        }

        if (def.parser() != null) {
            try {
                T parsed = (T) ((Function<String, AbstractTag>) def.parser()).apply(raw.identify());
                if (parsed != null) return parsed;
            } catch (Exception e) {
                CorexLogger.error("ARG ERROR: Failed to parse '" + raw.identify()
                        + "' as " + def.type().getSimpleName() + ": " + e.getMessage());
                return null;
            }
        }

        if (def.type().isAssignableFrom(ElementTag.class)) {
            return (T) new ElementTag(raw.identify());
        }

        CorexLogger.error("ARG ERROR: Expected " + def.type().getSimpleName()
                + " for arg '" + def.key() + "', got " + raw.getClass().getSimpleName()
                + " ('" + raw.identify() + "'). Did you provide a parser?");
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T extends AbstractTag> T defaultOf(ArgumentDef def) {
        if (def.defaultRaw() == null) return null;

        if (def.parser() != null) {
            try {
                return (T) ((Function<String, AbstractTag>) def.parser()).apply(def.defaultRaw());
            } catch (Exception ignored) {}
        }

        if (def.type().isAssignableFrom(ElementTag.class)) {
            return (T) new ElementTag(def.defaultRaw());
        }

        return null;
    }
}