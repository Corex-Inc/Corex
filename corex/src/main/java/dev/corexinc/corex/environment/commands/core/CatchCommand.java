package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.CorexSerializer;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.jspecify.annotations.NonNull;

import java.util.List;

/* @doc command
 *
 * @Name Catch
 * @Syntax catch (as:<name>): [<commands>]
 * @RequiredArgs 0
 * @MaxArgs 0
 * @ShortDescription Handles errors trapped by a preceding try command.
 *
 * @Description
 * Runs its sub-block only if the preceding try command trapped one or more errors.
 * The trapped errors are exposed as a ListTag through the 'as:' definition name, defaulting to 'error'.
 * If the try block completed without errors, the catch block is skipped.
 *
 * @Usage
 * // Narrate the trapped error message.
 * - try:
 *     - inject nonexistent_script
 * - catch as:err:
 *     - narrate "Failed: <[err].first>"
 */
public class CatchCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "catch";
    }

    @Override
    public @NonNull String getSyntax() {
        return "(as:<name>)";
    }

    @Override
    public int getMinArgs() {
        return 0;
    }

    @Override
    public int getMaxArgs() {
        return 0;
    }

    @Override
    public boolean isAsyncSafe() {
        return true;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        if (!(queue.getTempData("corex_try_caught") instanceof List<?> errors)) {
            Debugger.echoError(queue, "Command 'catch' must follow a 'try'!");
            return;
        }

        if (errors.isEmpty()) return;

        String name = instruction.getPrefix("as", queue);
        if (name == null) name = "error";

        ListTag errorList = new ListTag();
        for (Object error : errors) {
            String legacy = CorexSerializer.LEGACY.serialize(MiniMessage.miniMessage().deserialize(String.valueOf(error)));
            errorList.addObject(new ElementTag(legacy));
        }
        queue.define(name, errorList);

        Debugger.report(queue, instruction, "Errors", errorList.identify());

        if (instruction.innerBlock != null) {
            queue.pushFrame(getName(), instruction.innerBlock, null);
        }
    }
}
