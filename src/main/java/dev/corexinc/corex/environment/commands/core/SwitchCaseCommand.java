package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import org.jspecify.annotations.NonNull;

public class SwitchCaseCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "case";
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<text>|...]";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return -1;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        Debugger.error(queue, "Command '" + getName() + "' can ONLY be used inside 'switch'!", 0);
    }
}