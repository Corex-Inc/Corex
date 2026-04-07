package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.DurationTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/* @doc command
 *
 * @Name Wait
 * @Syntax wait (<duration>)
 * @RequiredArgs 0
 * @MaxArgs 1
 * @Aliases delay
 * @ShortDescription Delays a script for a specified amount of time.
 *
 * @Implements Wait
 *
 * @Description
 * Pauses the script queue for the duration specified. If no duration is specified it defaults to 1 second.
 *
 * @Usage
 * // Use to delay the current queue for 1 minute.
 * - wait 1m
 *
 * @Usage
 * // Use to delay using multiple time types.
 * - wait 1m34s8t
 *
 * @Usage
 * // Use to delay using DurationTag.
 * - wait <duration[1s]>
 */
public class WaitCommand implements AbstractCommand {

    @Override
    public @NotNull String getName() {
        return "wait";
    }

    @Override
    public @NotNull @Unmodifiable List<String> getAlias() {
        return List.of("delay");
    }

    @Override
    public @NotNull String getSyntax() {
        return "wait (<duration>)";
    }

    @Override
    public int getMinArgs() {
        return 0;
    }

    @Override
    public int getMaxArgs() {
        return 1;
    }

    @Override
    public void run(@NotNull ScriptQueue queue, @NotNull Instruction instruction) {
        if (instruction.linearArgs != null && instruction.linearArgs.length > 0) {
            String raw = instruction.getLinear(0, queue);
            if (raw != null && !raw.isBlank()) {
                DurationTag parsed = new DurationTag(raw);
                if (parsed.getTicks() > 0) {
                    queue.delay(parsed.getTicksLong());
                } else {
                    Debugger.error(queue, getName() + " could not parse duration '" + raw + "'", 0);
                }
            }
        }
    }
}