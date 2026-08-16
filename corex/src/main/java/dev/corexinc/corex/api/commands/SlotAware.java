package dev.corexinc.corex.api.commands;

import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.compiler.SlotTable;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Implemented by commands that write queue definitions under names known at compile time.
 *
 * <p>The compiler assigns every such name a fixed array index, so reads and writes at
 * runtime become plain array access instead of a map lookup. A command that does not
 * implement this interface still works — its definitions simply resolve through the
 * slower name-based path.</p>
 *
 * @since 1.0.0
 */
@AvailableSince("1.0.0")
public interface SlotAware {

    /**
     * Returns the definition names this instruction writes, or an empty list when none
     * of them are statically known.
     *
     * <p>Called once at compile time. Names returned here are guaranteed to be present
     * in the {@link SlotTable} passed to {@link #bindSlots}.</p>
     */
    @NotNull
    @AvailableSince("1.0.0")
    List<String> writtenDefinitions(@NotNull Instruction instruction);

    /**
     * Caches the resolved slot indices on the instruction, normally in
     * {@link Instruction#slotData}.
     *
     * <p>Called once at compile time, after every name from {@link #writtenDefinitions}
     * has been interned.</p>
     */
    @AvailableSince("1.0.0")
    void bindSlots(@NotNull Instruction instruction, @NotNull SlotTable table);
}
