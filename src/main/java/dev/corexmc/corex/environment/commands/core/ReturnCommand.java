package dev.corexmc.corex.environment.commands.core;

import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.engine.compiler.Instruction;
import dev.corexmc.corex.engine.queue.ScriptQueue;
import dev.corexmc.corex.engine.tags.ObjectFetcher;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class ReturnCommand implements AbstractCommand {

    @Override public @NonNull String getName() { return "return"; }
    @Override public @NonNull List<String> getAlias() { return List.of("determine"); }

    @Override
    public void run(@NonNull ScriptQueue queue, Instruction instruction) {
        String value = instruction.getLinear(0, queue);

        if (value != null) {
            queue.addReturn(ObjectFetcher.pickObject(value));
        }

        if (!instruction.hasFlag("passive")) {
            queue.stopEntireQueue();
        }
    }

    @Override public @NonNull String getSyntax() { return "[<value>] (passive)"; }
    @Override public int getMinArgs() { return 1; }
    @Override public int getMaxArgs() { return 2; }
}