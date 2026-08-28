package dev.corexinc.corex.environment.flags;

import dev.corexinc.corex.api.flags.AbstractGlobalFlag;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.PlayerIdentity;
import org.jspecify.annotations.NonNull;

public class PlayerGlobalFlag implements AbstractGlobalFlag {
    @Override
    public @NonNull String getName() {
        return "player";
    }

    @Override
    public boolean execute(@NonNull ScriptQueue queue, @NonNull Instruction instruction, @NonNull CompiledArgument value) {
        AbstractTag evaluated = value.evaluate(queue);
        if (evaluated instanceof PlayerIdentity) {
            AbstractTag previous = queue.getDefinition("__player");
            queue.define("__player", evaluated);

            instruction.command.run(queue, instruction);
            queue.define("__player", previous);

            return false;
        }
        return true;
    }
}
