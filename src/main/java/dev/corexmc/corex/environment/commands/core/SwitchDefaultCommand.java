package dev.corexmc.corex.environment.commands.core;

import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.engine.compiler.Instruction;
import dev.corexmc.corex.engine.queue.ScriptQueue;
import dev.corexmc.corex.engine.utils.debugging.Debugger;
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