package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class ReturnCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "return";
    }

    @Override
    public @NonNull List<String> getAlias() {
        return List.of("determine");
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<value>] (cancelled) (passive)";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 2;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, Instruction instruction) {
        String value = instruction.getLinear(0, queue);

        if (value != null) {
            queue.addReturn(ObjectFetcher.pickObject(value));
        }

        if (instruction.hasFlag("cancelled")) {
            queue.setCancelled(true);
        }

        if (!instruction.hasFlag("passive")) {
            queue.stopEntireQueue();
        }
    }
}