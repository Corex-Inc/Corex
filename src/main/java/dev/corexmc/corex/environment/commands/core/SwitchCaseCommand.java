package dev.corexmc.corex.environment.commands.core;

import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.engine.compiler.Instruction;
import dev.corexmc.corex.engine.queue.ScriptQueue;
import dev.corexmc.corex.engine.utils.debugging.Debugger;
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