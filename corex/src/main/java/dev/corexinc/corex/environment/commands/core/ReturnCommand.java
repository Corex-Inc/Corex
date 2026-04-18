package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import org.jspecify.annotations.NonNull;

import java.util.List;

/* @doc command
 *
 * @Name Return
 * @Syntax return [<value>] (cancelled) (passive)
 * @RequiredArgs 1
 * @MaxArgs 2
 * @ShortDescription Sets the outcome of a script.
 *
 * @Implements Determine
 *
 * @Description
 * Sets the outcome of a script.
 * The most common use case is within script events (for example, to cancel the event).
 * It may be useful in other cases (such as a task script that returns a result, via the save argument).
 *
 * By default, the "return" command will end the queue (similar to {@link command stop}).
 * If you wish to prevent this, specify the "passive" argument.
 *
 * To make multiple returns, simply use the return command multiple times in a row, with the "passive" argument on each.
 *
 * @Tags
 * <QueueTag.returns> - Returns all "returns" of queue.
 * <QueueTag.isCancelled> - Returns "true" if queue has been cancelled
 *
 * @Usage
 * // Use to modify the result of an event.
 * - return message:<context.message.toLowercase>
 *
 * @Usage
 * // Use to cancel an event, but continue running script commands.
 * - return passive cancelled
 *
 * @Usage
 * // Use to returns multiple values.
 * - return something:<[value]> passive
 * - return anotherValue:<[myValue]> passive
 * - return "message:Hi, player!" // Without "passive" flag
 */
public class ReturnCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "return";
    }

    @Override
    public @NonNull List<String> getAlias() {
        return List.of("determine");
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<value>] (cancelled) (passive)";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 2;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, Instruction instruction) {
        String value = instruction.getLinear(0, queue);

        Debugger.report(queue, instruction,
                "Value", value,
                "IsCancelled", instruction.hasFlag("cancelled"),
                "Passive", instruction.hasFlag("passive")
        );

        if (value != null) {
            queue.addReturn(ObjectFetcher.pickObject(value));
        }

        if (instruction.hasFlag("cancelled")) {
            queue.setCancelled(true);
        }

        if (!instruction.hasFlag("passive")) {
            queue.stopEntireQueue();
        }
    }
}