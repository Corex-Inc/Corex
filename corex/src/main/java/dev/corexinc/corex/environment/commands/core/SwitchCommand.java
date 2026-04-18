package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import org.jspecify.annotations.NonNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* @doc command
 *
 * @Name Switch
 * @Syntax switch [<option>] [<cases>]
 * @RequiredArgs 1
 * @MaxArgs 1
 * @ShortDescription Switches an option from the list of cases.
 *
 * @Implements Choose
 *
 * @Description
 * Switch an option from the list of cases.
 * Intended to replace a long chain of simplistic if/else if or complicated script path selection systems.
 * Simply input the selected option, and the system will automatically jump to the most relevant case input.
 * Cases are given as a sub-set of commands inside the current command (see Usage for samples).
 *
 * Optionally, specify "default" in place of a case to give a result when all other cases fail to match.
 *
 * Cases must be static text. They may not contain tags. For multi-tag comparison, consider the IF command.
 * Any one case line can have multiple values in it - each possible value should be its own argument (separated by spaces).
 *
 * @Usage
 * // Use to choose the only case.
 * - switch 1:
 *   - case 1:
 *     - narrate "Success!"
 *
 * @Usage
 * // Use to switch the default case in /command.
 * - switch <[argument]>:
 *   - case create:
 *     - narrate "Successful created bank account!"
 *   - case delete:
 *     - narrate "Are you sure?"
 *   - default:
 *     - narrate "Unexpected argument!"
 *
 * @Usage
 * // Use for dynamically choosing a case.
 * - switch <[entityType]>:
 *   - case zombie:
 *     - narrate "You slayed an undead zombie!"
 *   - case skeleton:
 *     - narrate "You knocked the bones out of a skeleton!"
 *   - case creeper:
 *     - narrate "You didn't give that creeper a chance to explode!"
 *   - case pig cow chicken:
 *     - narrate "You killed an innocent farm animal!"
 *   - default:
 *     - narrate "You killed a <[entityType].toTitlecase>!"
 */
public class SwitchCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "switch";
    }

    @Override
    public @NonNull List<String> getAlias() {
        return List.of("choose");
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<value>]";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 1;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run(@NonNull ScriptQueue queue, Instruction instruction) {
        String switchValue = instruction.getLinear(0, queue);
        boolean failed = false;
        if (switchValue == null || instruction.innerBlock == null) {
            Debugger.echoError(queue, "Empty 'switch' block are not allowed!");
            Debugger.echoError(queue, "It seems 'switch' block is empty! Or argument not provided.");
            failed = true;
        };

        Map<String, Instruction[]> lookupTable;

        Debugger.report(queue, instruction,
                "Value", switchValue
        );

        if (failed) return;

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
                            String key = caseVal.toLowerCase();
                            if (lookupTable.containsKey(key)) {
                                Debugger.echoError(queue, "Duplicate case value '" + key + "' detected in switch block!");
                            } else {
                                lookupTable.put(key, child.innerBlock);
                            }
                        }
                    }
                } else if (child.command instanceof SwitchDefaultCommand) {
                    if (child.innerBlock != null) {
                        lookupTable.put("\0DEFAULT", child.innerBlock);
                    }
                } else {
                    Debugger.echoError(queue, "Unknown command inside switch block: " + child.command.getName());
                }
            }
            instruction.customData = lookupTable;
        }

        Instruction[] targetBlock = lookupTable.get(switchValue.toLowerCase());

        if (targetBlock == null) {
            targetBlock = lookupTable.get("\0DEFAULT");
        }

        if (targetBlock != null) {
            queue.pushFrame(getName(), targetBlock, null);
        }
    }
}