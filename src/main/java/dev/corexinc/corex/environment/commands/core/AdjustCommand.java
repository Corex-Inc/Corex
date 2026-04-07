package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import org.jspecify.annotations.NonNull;
import java.util.HashMap;
import java.util.Map;

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

        if (targetsRaw == null || mechInput == null) return;

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
            AbstractTag current = target;
            for (Map.Entry<String, AbstractTag> entry : mechanisms.entrySet()) {
                current = current.applyMechanism(entry.getKey(), entry.getValue());
            }
        }
    }
}