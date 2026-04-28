package dev.corexinc.corex.environment.commands.entity;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import org.jetbrains.annotations.NotNull;

public class DamageCommand implements AbstractCommand {
    @Override
    public @NotNull String getName() {
        return "damage";
    }

    @Override
    public void run(@NotNull ScriptQueue queue, @NotNull Instruction instruction) {
    }

    @Override
    public @NotNull String getSyntax() {
        return "уйня";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 4;
    }
}
