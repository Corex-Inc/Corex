package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import org.jspecify.annotations.NonNull;

public class SwitchCaseCommand implements AbstractCommand {
    @Override public @NonNull String getName() { return "case"; }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        Debugger.echoError("ERROR: Command '- case' can ONLY be used inside '- switch'!");
    }

    @Override public @NonNull String getSyntax() { return "[<text>|...]"; }
    @Override public int getMinArgs() { return 1; }
    @Override public int getMaxArgs() { return 99; }
}