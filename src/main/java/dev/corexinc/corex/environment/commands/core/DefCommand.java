package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.data.actions.AbstractDataAction;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.MapTag;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.regex.Pattern;

public class DefCommand implements AbstractCommand {

    private static final Pattern DOT_SPLIT = Pattern.compile("\\.", Pattern.LITERAL);

    @Override public @NonNull String getName() { return "def"; }
    @Override public @NonNull List<String> getAlias() { return List.of("define"); }
    @Override public @NonNull String getSyntax() { return "[<key>(:action)] [<value>]"; }
    @Override public int getMinArgs() { return 1; }
    @Override public int getMaxArgs() { return 2; }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        String rawArg = instruction.getLinear(0, queue);
        if (rawArg == null) {
            Debugger.echoError(queue, "Definition name cannot be null!");
            Debugger.echoError(queue, "Definition name returned <red>null</red>.");
            rawArg = "";
        }

        int colonIndex = findColonOutsideBrackets(rawArg);

        if (colonIndex < 0) {
            String value = instruction.getLinear(1, queue);

            queue.define(rawArg, value == null ? null : ObjectFetcher.pickObject(value));

            Debugger.report(queue, instruction,
                    "Definition", rawArg,
                    "Value", value,
                    "Action", ":"
            );
            return;
        }

        String keyPath = rawArg.substring(0, colonIndex);
        String actionStr = rawArg.substring(colonIndex + 1);

        AbstractDataAction action = Corex.getInstance().getRegistry().findAction(actionStr);
        if (action == null) {
            Debugger.echoError(queue, "DataAction cannot be null!");
            Debugger.echoError(queue, "It seems DataAction is <red>null</red>.");
            return;
        }

        String param = action.extractParam(actionStr);
        AbstractTag secondArg = instruction.getLinearObject(1, queue);

        String[] parts = DOT_SPLIT.split(keyPath, -1);

        try {
            if (parts.length > 1) {
                applyToMap(queue, parts, action, param, secondArg);
            } else {
                AbstractTag current = queue.getDefinition(keyPath);
                AbstractTag result = action.apply(current, param, secondArg, queue);
                queue.define(keyPath, result);
            }
        }
        finally {
            Debugger.report(queue, instruction,
                    "Definition", keyPath,
                    "Value", param,
                    "Action", ":" + action.getSymbol()
            );
        }
    }

    private static void applyToMap(@NonNull ScriptQueue queue, @NonNull String[] parts,
                                   @NonNull AbstractDataAction action, @NonNull String param,
                                   AbstractTag secondArg) {
        String varName = parts[0];
        AbstractTag existing = queue.getDefinition(varName);
        MapTag map = existing instanceof MapTag mt ? mt : new MapTag();

        AbstractTag currentValue = getDeepValue(map, parts, 1);
        AbstractTag newValue = action.apply(currentValue, param, secondArg, queue);
        setDeepValue(map, parts, 1, newValue);

        queue.define(varName, map);
    }

    private static AbstractTag getDeepValue(AbstractTag root, String[] parts, int index) {
        if (index >= parts.length) return root;
        if (!(root instanceof MapTag map)) return null;
        return getDeepValue(map.getObject(parts[index]), parts, index + 1);
    }

    private static void setDeepValue(MapTag root, String[] parts, int index, AbstractTag value) {
        if (index == parts.length - 1) {
            root.putObject(parts[index], value);
            return;
        }
        AbstractTag nested = root.getObject(parts[index]);
        MapTag nestedMap = nested instanceof MapTag mt ? mt : new MapTag();
        setDeepValue(nestedMap, parts, index + 1, value);
        root.putObject(parts[index], nestedMap);
    }

    private static int findColonOutsideBrackets(String s) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') depth--;
            else if (c == ':' && depth == 0) return i;
        }
        return -1;
    }
}