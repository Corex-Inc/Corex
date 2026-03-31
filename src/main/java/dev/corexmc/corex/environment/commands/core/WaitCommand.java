package dev.corexmc.corex.environment.commands.core;

import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.engine.compiler.Instruction;
import dev.corexmc.corex.engine.queue.ScriptQueue;
import dev.corexmc.corex.engine.utils.debugging.Debugger;
import dev.corexmc.corex.environment.tags.core.DurationTag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

/**
 * Pauses the current queue for a specified duration.
 *
 * <pre>
 * Syntax:  wait [&lt;duration&gt;]
 *
 * The argument is parsed by {@link DurationTag}, so any valid duration expression works:
 *   wait 5s          →  5 seconds
 *   wait 2m34s       →  2 min 34 sec
 *   wait 40t         →  40 ticks
 *   wait 0.1t        →  0.1 ticks
 *   wait 1h30m       →  1 hour 30 min
 *
 * Default (no argument): 3 seconds (60 ticks).
 * </pre>
 */
public class WaitCommand implements AbstractCommand {

    private static final DurationTag DEFAULT_DURATION = new DurationTag("3s"); // 60 ticks

    @Override
    public @NotNull String getName() {
        return "wait";
    }

    @Override
    public @NotNull @Unmodifiable List<String> getAlias() {
        return List.of("wait", "delay");
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
        DurationTag duration = DEFAULT_DURATION;

        if (instruction.linearArgs != null && instruction.linearArgs.length > 0) {
            String raw = instruction.getLinear(0, queue);
            if (raw != null && !raw.isBlank()) {
                DurationTag parsed = new DurationTag(raw);
                if (parsed.getTicks() > 0) {
                    duration = parsed;
                } else {
                    Debugger.echoError("WaitCommand: could not parse duration '" + raw + "', defaulting to 3s.");
                }
            }
        }

        queue.delay(duration.getTicksLong());
    }
}