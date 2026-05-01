package dev.corexinc.corex.engine.compiler;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.flags.AbstractGlobalFlag;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import org.jetbrains.annotations.ApiStatus.*;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Represents a pre-compiled execution unit (opcode) within the Corex Engine.
 * <p>
 * An Instruction contains all the necessary data to execute a command, including
 * pre-parsed arguments, global flags, and nested instruction blocks.
 * By using pre-compiled Instructions, the engine avoids expensive string parsing during runtime.
 *
 * @since 1.0.0
 */
public class Instruction {
    /**
     * The actual command implementation to be executed.
     */
    @NotNull public final AbstractCommand command;

    /**
     * An array of positional (linear) arguments.
     */
    @NotNull public final CompiledArgument[] linearArgs;

    /**
     * A map of prefixed arguments (e.g., {@code targets:<player>}).
     */
    @NotNull public final Map<String, CompiledArgument> prefixArgs;

    /**
     * An array of raw flags/switches (e.g., {@code --silent}).
     */
    @NotNull public final String[] flags;

    /**
     * A nested block of instructions used for braced commands like {@code if}, {@code repeat}, or {@code while}.
     */
    @Nullable public final Instruction[] innerBlock;

    /**
     * A versatile storage field for commands to cache data between executions.
     * <p>
     * <b>Example:</b> The {@code switch} command uses this to store a {@code HashMap} of cases
     * for O(1) lookups after the first run.
     */
    @Nullable public Object customData = null;

    /**
     * Indicates whether the command was prefixed with {@code ~}, meaning the queue
     * should wait for this command to signal completion before proceeding.
     */
    public final boolean isWaitable;

    /**
     * A map of {@link AbstractGlobalFlag} modifiers applied to this specific instruction (e.g., {@code if:<condition>}).
     */
    @NotNull public final Map<AbstractGlobalFlag, CompiledArgument> globalFlags;

    public Instruction(
            @NotNull AbstractCommand command,
            @NotNull CompiledArgument[] linearArgs,
            @NotNull Map<String, CompiledArgument> prefixArgs,
            @NotNull String[] flags,
            @Nullable Instruction[] innerBlock,
            boolean isWaitable,
            @Nullable Map<AbstractGlobalFlag, CompiledArgument> globalFlags
    ) {
        this.command = command;
        this.linearArgs = linearArgs;
        this.prefixArgs = prefixArgs;
        this.flags = flags;
        this.innerBlock = innerBlock;
        this.isWaitable = isWaitable;
        this.globalFlags = globalFlags;
    }

    /**
     * Evaluates a linear argument at the specified index and returns its string representation.
     *
     * @param index the position of the argument.
     * @param queue the queue context for tag evaluation.
     * @return the identified string, or {@code null} if the index is out of bounds.
     */
    @Nullable
    @AvailableSince("1.0.0")
    public String getLinear(int index, @NotNull ScriptQueue queue) {
        AbstractTag tag = getLinearObject(index, queue);
        return tag != null ? tag.identify() : null;
    }

    /**
     * Evaluates a linear argument and returns the resulting {@link AbstractTag} object.
     * <p>
     * Use this method when you need to interact with the object directly (e.g., an ItemTag or PlayerTag)
     * without converting it to a string first.
     *
     * @param index the position of the argument.
     * @param queue the queue context for tag evaluation.
     * @return the evaluated tag object, or {@code null} if not found.
     */
    @Nullable
    @AvailableSince("1.0.0")
    public AbstractTag getLinearObject(int index, @NotNull ScriptQueue queue) {
        if (index < 0 || index >= linearArgs.length) return null;
        return linearArgs[index].evaluate(queue);
    }

    /**
     * Evaluates a prefixed argument and returns its string representation.
     *
     * @param prefix the name of the prefix (e.g., "reason").
     * @param queue  the queue context for tag evaluation.
     * @return the identified string, or {@code null} if the prefix was not provided.
     */
    @Nullable
    @AvailableSince("1.0.0")
    public String getPrefix(@NotNull String prefix, @NotNull ScriptQueue queue) {
        AbstractTag tag = getPrefixObject(prefix, queue);
        return tag != null ? tag.identify() : null;
    }

    /**
     * Evaluates a prefixed argument and returns the resulting {@link AbstractTag} object.
     *
     * @param prefix the name of the prefix.
     * @param queue  the queue context for tag evaluation.
     * @return the evaluated tag object, or {@code null} if not found.
     */
    @Nullable
    @AvailableSince("1.0.0")
    public AbstractTag getPrefixObject(@NotNull String prefix, @NotNull ScriptQueue queue) {
        CompiledArgument arg = prefixArgs.get(prefix.toLowerCase());
        return arg != null ? arg.evaluate(queue) : null;
    }

    /**
     * Checks if a specific flag (switch) was present in the command string.
     *
     * @param flag the flag name to check.
     * @return {@code true} if the flag exists.
     */
    @AvailableSince("1.0.0")
    public boolean hasFlag(@NotNull String flag) {
        for (String f : flags) {
            if (f.equalsIgnoreCase(flag)) return true;
        }
        return false;
    }
}