package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Adjustable;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import org.jspecify.annotations.NonNull;
import java.util.HashMap;
import java.util.Map;

/* @doc command
 *
 * @Name Adjust
 * @Syntax adjust [<object>|...] [<mechanism>:<value>/<map>]
 * @RequiredArgs 2
 * @MaxArgs 2
 * @ShortDescription Adjusts an object's mechanism.
 *
 * @Implements Adjust
 *
 * @Description
 * Many object tag types contains options and properties that need to be adjusted.
 * Corex employs a mechanism interface to deal with those adjustments.
 * To easily accomplish this, use this command with a valid object mechanism, and sometimes accompanying value.
 *
 * You can optionally adjust a MapTag of mechanisms to values.
 *
 *
 * @Usage
 * // Use to set a custom display name on an entity.
 * - adjust <[someEntity]> customName:ANGRY!
 *
 * @Usage
 * // Use to set the skin of every online player.
 * - adjust <server.onlinePlayers> skin:Notch
 *
 * @Usage
 * // Use to adjust a MapTag of mechanisms.
 * - adjust <player> <map[maxHealth=10;health=4]>
 */
public class AdjustCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "adjust";
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<object>|...] [<mechanism>:<value>/<map>]";
    }

    @Override
    public int getMinArgs() {
        return 2;
    }

    @Override
    public int getMaxArgs() {
        return 2;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        String targetsRaw = instruction.getLinear(0, queue);
        AbstractTag mechInput = instruction.getLinearObject(1, queue);

        if (targetsRaw == null) {
            Debugger.echoError(queue, "Targets cannot be null!");
            return;
        }
        if (mechInput == null) {
            Debugger.echoError(queue, "Mechanism input cannot be null!");
            return;
        }

        ListTag targets = new ListTag(targetsRaw);
        Map<String, AbstractTag> mechanisms = new HashMap<>();

        if (mechInput instanceof MapTag map) {
            for (String key : map.keySet()) {
                mechanisms.put(key, map.getObject(key));
            }
        }
        else {
            String str = mechInput.identify();
            int colonIndex = str.indexOf(':');

            if (colonIndex > 0) {
                String mechName = str.substring(0, colonIndex);
                String mechValue = str.substring(colonIndex + 1);
                mechanisms.put(mechName, ObjectFetcher.pickObject(mechValue));
            } else {
                mechanisms.put(str, new ElementTag(""));
            }
        }

        for (AbstractTag target : targets.getList()) {

            if (target == null) {
                Debugger.echoError(queue, "Encountered null target!");
                continue;
            }

            AbstractTag current = target;

            for (Map.Entry<String, AbstractTag> entry : mechanisms.entrySet()) {
                String mech = entry.getKey();
                AbstractTag value = entry.getValue();

                if (current instanceof Adjustable adjustable) {
                    current = adjustable.applyMechanism(mech, value);
                }
                else {
                    Debugger.echoError(queue, "Mechanisms cannot be applied to the '<yellow>" + current.identify() + "</yellow>'!");
                    break;
                }
            }
        }

        Debugger.report(queue, instruction,
                "Targets", targets.identify(),
                "Mechanisms", mechInput.identify()
        );
    }
}