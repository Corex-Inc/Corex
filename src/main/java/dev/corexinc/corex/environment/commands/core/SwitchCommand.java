package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SwitchCommand implements AbstractCommand {

    @Override public @NonNull String getName() { return "switch"; }

    @Override public java.util.@NonNull List<String> getAlias() { return List.of("choose"); }

    @Override
    @SuppressWarnings("unchecked")
    public void run(@NonNull ScriptQueue queue, Instruction instruction) {
        String switchValue = instruction.getLinear(0, queue);
        if (switchValue == null || instruction.innerBlock == null) return;

        Map<String, Instruction[]> lookupTable;

        if (instruction.customData instanceof Map) {
            lookupTable = (Map<String, Instruction[]>) instruction.customData;
        } else {
            lookupTable = new HashMap<>();

            for (Instruction child : instruction.innerBlock) {
                if (child.command instanceof SwitchCaseCommand) {
                    if (child.innerBlock == null) continue;
                    for (int i = 0; i < child.linearArgs.length; i++) {
                        String caseVal = child.getLinear(i, null);
                        if (caseVal != null) {
                            lookupTable.put(caseVal.toLowerCase(), child.innerBlock);
                        }
                    }
                }
                else if (child.command instanceof SwitchDefaultCommand) {
                    if (child.innerBlock != null) {
                        lookupTable.put("\0DEFAULT", child.innerBlock);
                    }
                }
                else {
                    Debugger.echoError("Unknown command in switch: " + child.command.getName());
                }
            }
            instruction.customData = lookupTable;
        }

        String choiceLow = switchValue.toLowerCase();

        Instruction[] targetBlock = lookupTable.get(choiceLow);

        if (targetBlock == null) {
            targetBlock = lookupTable.get("\0DEFAULT");
        }

        if (targetBlock != null) {
            queue.pushFrame(targetBlock, null);
        }
    }

    @Override public @NonNull String getSyntax() { return "[<value>]"; }
    @Override public int getMinArgs() { return 1; }
    @Override public int getMaxArgs() { return 1; }
}