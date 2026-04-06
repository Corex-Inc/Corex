package dev.corexinc.corex.environment.flags;

import dev.corexinc.corex.api.flags.AbstractGlobalFlag;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.tags.player.PlayerTag;

public class PlayerGlobalFlag implements AbstractGlobalFlag {
    @Override
    public String getName() {
        return "player";
    }

    @Override
    public boolean execute(ScriptQueue queue, Instruction instruction, CompiledArgument value) {
        if (value.evaluate(queue) instanceof PlayerTag playerTag)
            queue.define("__player", playerTag);
        return true;
    }
}
