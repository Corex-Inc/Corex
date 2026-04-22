package dev.corexinc.corex.environment.utils.scripts;

import dev.corexinc.corex.api.flags.AbstractGlobalFlag;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.tags.core.MapTag;

import java.util.Map;

public class CommandHelper {

    public static void saveResult(ScriptQueue queue, Instruction instruction, MapTag result) {
        if (instruction.globalFlags == null || instruction.globalFlags.isEmpty()) return;

        for (Map.Entry<AbstractGlobalFlag, CompiledArgument> entry : instruction.globalFlags.entrySet()) {
            if (entry.getKey().getName().equalsIgnoreCase("save")) {

                String saveName = entry.getValue().evaluate(queue).identify();

                queue.define(saveName, result);
                return;
            }
        }
    }
}