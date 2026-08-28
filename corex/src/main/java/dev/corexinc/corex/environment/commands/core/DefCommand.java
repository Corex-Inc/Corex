package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.commands.DataBlockCommand;
import dev.corexinc.corex.api.commands.SlotAware;
import dev.corexinc.corex.api.data.actions.AbstractDataAction;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.compiler.ScriptCompiler;
import dev.corexinc.corex.engine.compiler.SlotTable;
import dev.corexinc.corex.engine.compiler.args.StaticArg;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.scripts.ScriptManager;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/* @doc command
 *
 * @Name Def
 * @Syntax def [<key>(:action)] [<value>]
 * @RequiredArgs 1
 * @MaxArgs 2
 * @Aliases define
 * @ShortDescription Defines or modifies a variable in the current queue.
 *
 * @Description
 * Stores a value in the current script queue under the given key.
 *
 * Use dot-notation to target nested keys inside a MapTag (e.g. "player.stats.level").
 * If the map or any intermediate key does not exist, it will be created automatically.
 *
 * Optionally append a data action after a colon to modify the existing value instead of replacing it
 * (e.g. "def count:++" to increment, "def items:|+:sword" to add to a list).
 * Available actions are registered by the engine.
 *
 * If no value argument is provided, the definition is set to null.
 *
 * You can also define a MapTag or ListTag using an inline YAML block.
 * Values inside the block support full tag syntax (e.g. <player.name>).
 * Maps and lists can be nested arbitrarily.
 *
 * @Usage
 * // Store a simple string.
 * - def name John
 *
 * @Usage
 * // Increment a numeric definition.
 * - def score:++
 *
 * @Usage
 * // Set a nested key inside a map definition.
 * - def player.stats.level 10
 *
 * @Usage
 * // Clear a definition.
 * - def tempValue
 *
 * @Usage
 * // Define a MapTag using an inline block.
 * - def myMap:
 *     name: <player.name>
 *     score: 42
 *     active: true
 *
 * @Usage
 * // Define a ListTag using an inline block.
 * - def myList:
 *     - sword
 *     - shield
 *     - <player.item>
 *
 * @Usage
 * // Nested maps and lists are supported.
 * - def player:
 *     name: <player.name>
 *     stats:
 *       level: 10
 *       xp: 500
 *     items:
 *       - sword
 *       - bow
 */
public class DefCommand implements AbstractCommand, DataBlockCommand, SlotAware {

    private static final Pattern DOT_SPLIT = Pattern.compile("\\.", Pattern.LITERAL);

    private record ResolvedDefinition(String keyPath, AbstractDataAction action, String param, String[] parts,
                                      AbstractTag staticValue, CompiledArgument valueArg) {

        ResolvedDefinition(String keyPath, AbstractDataAction action, String param, String[] parts,
                           AbstractTag staticValue) {
            this(keyPath, action, param, parts, staticValue, null);
        }
    }

    /**
     * Splits {@code key:value} on the script's own source text instead of on the evaluated argument.
     * <p>
     * Reading argument 0 evaluates the whole thing, so a line like {@code - def x:<[weights]>} would
     * flatten the list into a pipe-joined string before this command ever sees the colon, only for
     * the action to parse it straight back. Splitting the raw text lets the value stay an object.
     * <p>
     * Only taken when the key is literal and the value actually contains a tag: for plain text the
     * old path costs nothing and stays byte-for-byte identical.
     *
     * @return the resolved definition, or {@code null} to fall through to the general path
     */
    private static ResolvedDefinition resolveFromSource(Instruction instruction) {
        CompiledArgument first = instruction.getLinearArgument(0);
        if (first == null) return null;

        String raw = first.getRaw();
        if (raw == null) return null;

        int colon = findColonOutsideBrackets(raw);
        if (colon <= 0) return null;

        String keyPath = raw.substring(0, colon);
        if (keyPath.indexOf('<') >= 0) return null;

        String actionStr = raw.substring(colon + 1);
        AbstractDataAction action = ScriptManager.getRegistry().findAction(actionStr);
        if (action == null) return null;

        String param = action.extractParam(actionStr);
        if (param == null || param.indexOf('<') < 0) return null;

        CompiledArgument valueArg = ScriptCompiler.parseArg(param);
        if (valueArg == null) return null;

        String[] parts = keyPath.indexOf('.') < 0
                ? new String[] { keyPath }
                : DOT_SPLIT.split(keyPath, -1);
        return new ResolvedDefinition(keyPath, action, param, parts, null, valueArg);
    }

    private record DefSlot(String name, int slot) {}

    private static String staticRootName(Instruction instruction) {
        if (!(instruction.getLinearArgument(0) instanceof StaticArg staticArg)) return null;
        String raw = staticArg.evaluate(null).identify();
        int colon = findColonOutsideBrackets(raw);
        String keyPath = colon < 0 ? raw : raw.substring(0, colon);
        return keyPath.indexOf('.') < 0 ? keyPath : null;
    }

    @Override
    public @NonNull List<String> writtenDefinitions(@NonNull Instruction instruction) {
        String name = staticRootName(instruction);
        return name != null && !name.isEmpty() ? List.of(name) : List.of();
    }

    @Override
    public void bindSlots(@NonNull Instruction instruction, @NonNull SlotTable table) {
        String name = staticRootName(instruction);
        if (name == null || name.isEmpty()) return;
        instruction.slotData = new DefSlot(name, table.indexOf(name));
    }

    @Override
    public @NonNull String getName() {
        return "def";
    }

    @Override
    public @NonNull List<String> getAlias() {
        return List.of("define");
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<key>(:action)] (<value>)";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 2;
    }

    @Override
    public boolean isAsyncSafe() {
        return true;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {

        if (instruction.customData instanceof ResolvedDefinition resolved) {
            runResolved(queue, instruction, resolved);
            return;
        }

        if (instruction.customData instanceof Map<?, ?> rawMap) {
            handleDataBlock(queue, instruction, rawMap);
            return;
        }

        if (instruction.customData instanceof List<?> rawList) {
            handleDataBlock(queue, instruction, rawList);
            return;
        }

        ResolvedDefinition fromSource = resolveFromSource(instruction);
        if (fromSource != null) {
            instruction.customData = fromSource;
            runResolved(queue, instruction, fromSource);
            return;
        }

        String rawArg = instruction.getLinear(0, queue);
        if (rawArg == null) {
            Debugger.echoError(queue, "Definition name cannot be null!");
            Debugger.echoError(queue, "Definition name returned <red>null</red>.");
            rawArg = "";
        }

        int colonIndex = findColonOutsideBrackets(rawArg);
        ResolvedDefinition resolved;

        if (colonIndex < 0) {
            AbstractTag staticValue = null;
            if (instruction.getLinearArgument(1) instanceof StaticArg staticArg
                    && staticArg.evaluate(null) instanceof ElementTag el
                    && (el.isKnownNumeric() || el.asString().indexOf('@') < 1)) {
                staticValue = el;
            }
            resolved = new ResolvedDefinition(rawArg, null, null, null, staticValue);
        }
        else {
            String keyPath = rawArg.substring(0, colonIndex);
            String actionStr = rawArg.substring(colonIndex + 1);

            AbstractDataAction action = ScriptManager.getRegistry().findAction(actionStr);
            if (action == null) {
                Debugger.echoError(queue, "DataAction cannot be null!");
                Debugger.echoError(queue, "It seems DataAction is <red>null</red>.");
                return;
            }

            String param = action.extractParam(actionStr);
            String[] parts = keyPath.indexOf('.') < 0
                    ? new String[] { keyPath }
                    : DOT_SPLIT.split(keyPath, -1);
            resolved = new ResolvedDefinition(keyPath, action, param, parts, null);
        }

        if (instruction.getLinearArgument(0) instanceof StaticArg) {
            instruction.customData = resolved;
        }

        runResolved(queue, instruction, resolved);
    }

    private void runResolved(@NonNull ScriptQueue queue, @NonNull Instruction instruction,
                             @NonNull ResolvedDefinition resolved) {
        int slot = instruction.slotData instanceof DefSlot cached && instruction.slots == queue.slotTable()
                ? cached.slot()
                : SlotTable.NO_SLOT;

        if (resolved.action() == null) {
            AbstractTag staticValue = resolved.staticValue();
            if (staticValue != null) {
                queue.setDefinition(slot, resolved.keyPath(), staticValue);

                if (Debugger.shouldReport(queue)) {
                    Debugger.report(queue, instruction,
                            "Definition", resolved.keyPath(),
                            "Value", staticValue.identify(),
                            "Action", ":"
                    );
                }
                return;
            }

            AbstractTag valueObj = instruction.getLinearObject(1, queue);
            AbstractTag stored;

            if (valueObj == null) {
                stored = null;
            }
            else if (valueObj instanceof ElementTag el) {
                stored = el.isKnownNumeric() || el.asString().indexOf('@') < 1
                        ? el
                        : ObjectFetcher.pickObject(el.asString());
            }
            else {
                stored = valueObj;
            }

            queue.setDefinition(slot, resolved.keyPath(), stored);

            if (Debugger.shouldReport(queue)) {
                Debugger.report(queue, instruction,
                        "Definition", resolved.keyPath(),
                        "Value", valueObj != null ? valueObj.identify() : null,
                        "Action", ":"
                );
            }
            return;
        }

        AbstractTag secondArg = instruction.getLinearObject(1, queue);
        if (secondArg == null && resolved.valueArg() != null) {
            secondArg = resolved.valueArg().evaluate(queue);
        }

        try {
            if (resolved.parts().length > 1) {
                applyToMap(queue, resolved.parts(), resolved.action(), resolved.param(), secondArg);
            } else {
                AbstractTag current = queue.readDefinition(slot, resolved.keyPath());
                AbstractTag result = resolved.action().apply(current, resolved.param(), secondArg, queue);
                queue.setDefinition(slot, resolved.keyPath(), result);
            }
        }
        finally {
            if (Debugger.shouldReport(queue)) {
                Debugger.report(queue, instruction,
                        "Definition", resolved.keyPath(),
                        "Value", resolved.param(),
                        "Action", ":" + resolved.action().getSymbol()
                );
            }
        }
    }

    private void handleDataBlock(@NonNull ScriptQueue queue, @NonNull Instruction instruction, Map<?, ?> rawMap) {
        String key = instruction.getLinear(0, queue);
        if (key == null) {
            Debugger.echoError(queue, "Definition name cannot be null!");
            return;
        }
        MapTag result = buildMapTag(rawMap, queue);
        queue.define(key, result);
        Debugger.report(queue, instruction,
                "Definition", key,
                "Value", result.identify(),
                "Action", ":"
        );
    }

    private void handleDataBlock(@NonNull ScriptQueue queue, @NonNull Instruction instruction, List<?> rawList) {
        String key = instruction.getLinear(0, queue);
        if (key == null) {
            Debugger.echoError(queue, "Definition name cannot be null!");
            return;
        }
        ListTag result = buildListTag(rawList, queue);
        queue.define(key, result);
        Debugger.report(queue, instruction,
                "Definition", key,
                "Value", result.identify(),
                "Action", ":"
        );
    }

    private AbstractTag buildTag(Object raw, ScriptQueue queue) {
        if (raw instanceof Map<?, ?> m)  return buildMapTag(m, queue);
        if (raw instanceof List<?> l)    return buildListTag(l, queue);
        if (raw == null)                 return null;

        String str = raw.toString();
        CompiledArgument compiled = ScriptCompiler.parseArg(str);
        AbstractTag evaluated = compiled.evaluate(queue);
        return evaluated != null ? evaluated : ObjectFetcher.pickObject(str);
    }

    private MapTag buildMapTag(Map<?, ?> raw, ScriptQueue queue) {
        MapTag map = new MapTag();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            String k = entry.getKey().toString();
            AbstractTag value = buildTag(entry.getValue(), queue);
            map.putObject(k, value);
        }
        return map;
    }

    private ListTag buildListTag(List<?> raw, ScriptQueue queue) {
        ListTag list = new ListTag();
        for (Object item : raw) {
            AbstractTag tag = buildTag(item, queue);
            list.addObject(tag);
        }
        return list;
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