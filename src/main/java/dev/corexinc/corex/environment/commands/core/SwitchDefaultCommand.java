package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import org.jspecify.annotations.NonNull;

public class SwitchDefaultCommand implements AbstractCommand {
    @Override public @NonNull String getName() { return "default"; }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        Debugger.echoError("ERROR: Command '- default' can ONLY be used inside '- switch'!");
    }

    @Override public @NonNull String getSyntax() { return ""; }
    @Override public int getMinArgs() { return 0; }
    @Override public int getMaxArgs() { return 0; }
}