package dev.corexmc.corex.environment.commands.core;

import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.engine.compiler.Instruction;
import dev.corexmc.corex.engine.queue.ScriptQueue;
import dev.corexmc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class DefCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() { return "def"; }

    @Override
    public @NonNull List<String> getAlias() { return List.of("define"); }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        String key = instruction.getLinear(0, queue);
        String value = instruction.getLinear(1, queue);

        if (key == null) return;

        queue.define(key, value == null ? null : new ElementTag(value));
    }

    @Override public @NonNull String getSyntax() { return "[<name>] [<value>]"; }
    @Override public int getMinArgs() { return 1; }
    @Override public int getMaxArgs() { return 2; }
}