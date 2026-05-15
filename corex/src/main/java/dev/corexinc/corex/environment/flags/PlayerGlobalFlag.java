package dev.corexinc.corex.environment.flags;

import dev.corexinc.corex.api.flags.AbstractGlobalFlag;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import org.jspecify.annotations.NonNull;

public class PlayerGlobalFlag implements AbstractGlobalFlag {
    @Override
    public @NonNull String getName() {
        return "player";
    }

    @Override
    public boolean execute(@NonNull ScriptQueue queue, @NonNull Instruction instruction, @NonNull CompiledArgument value) {
        if (value.evaluate(queue) instanceof PlayerTag playerTag) {
            AbstractTag previous = queue.getDefinition("__player");
            queue.define("__player", playerTag);

            instruction.command.run(queue, instruction);
            queue.define("__player", previous);

            return false;
        }
        return true;
    }
}
