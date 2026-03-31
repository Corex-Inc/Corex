package dev.corexmc.corex.environment.commands.core;

import dev.corexmc.corex.Corex;
import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.engine.compiler.CompiledArgument;
import dev.corexmc.corex.engine.compiler.Instruction;
import dev.corexmc.corex.engine.queue.ScriptQueue;
import dev.corexmc.corex.engine.scripts.ScriptManager;
import dev.corexmc.corex.engine.scripts.TaskScript;
import dev.corexmc.corex.engine.utils.debugging.Debugger;
import dev.corexmc.corex.environment.tags.core.ElementTag;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

public class DoCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() { return "do"; }

    @Override
    public @NonNull List<String> getAlias() {
        return List.of("run");
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        String scriptName = instruction.getLinear(0, queue);
        if (scriptName == null) return;

        TaskScript script = ScriptManager.getTaskScript(scriptName);
        if (script == null) {
            Debugger.echoError("Script Container '" + scriptName + "' not found!");
            return;
        }

        ScriptQueue newQueue = new ScriptQueue(
                "Queue_" + scriptName + "_" + System.currentTimeMillis(),
                script.getBytecode(),
                false,
                queue.getPlayer()
        );

        for (Map.Entry<String, CompiledArgument> entry : instruction.prefixArgs.entrySet()) {
            String prefix = entry.getKey();
            String prefixLower = prefix.toLowerCase();

            if (prefixLower.startsWith("def.") || prefixLower.startsWith("definition.")) {
                int offset = prefixLower.startsWith("definition.") ? 11 : 4;
                String defName = prefix.substring(offset);
                String defValue = entry.getValue().evaluate(queue);

                newQueue.define(defName, new ElementTag(defValue));
            }
        }

        newQueue.start();
    }

    @Override public @NonNull String getSyntax() { return "[<script>] (def.<key>:<value>)"; }
    @Override public int getMinArgs() { return 1; }
    @Override public int getMaxArgs() { return 10; }
}