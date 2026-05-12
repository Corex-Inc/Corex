package dev.corexinc.corex.velocity;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import org.jetbrains.annotations.NotNull;

public class TestCommand implements AbstractCommand {
    @Override
    public @NotNull String getName() {
        return "";
    }

    @Override
    public void run(@NotNull ScriptQueue queue, @NotNull Instruction instruction) {

    }

    @Override
    public @NotNull String getSyntax() {
        return "";
    }

    @Override
    public int getMinArgs() {
        return 0;
    }

    @Override
    public int getMaxArgs() {
        return 0;
    }
}
