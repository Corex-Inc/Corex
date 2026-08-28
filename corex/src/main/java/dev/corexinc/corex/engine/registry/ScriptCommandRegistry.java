package dev.corexinc.corex.engine.registry;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.CorexLogger;

import java.util.HashMap;
import java.util.Map;

public class ScriptCommandRegistry {

    private static final Map<String, CommandMetadata> commands = new HashMap<>();

    public void register(AbstractCommand command) {
        CommandMetadata meta = new CommandMetadata(command);

        if (meta.bound == null && !implementsClassicRun(command)) {
            CorexLogger.error("Command '<yellow>" + command.getName() + "</yellow>' ("
                    + command.getClass().getSimpleName() + ") implements neither form of run(...)!");
            CorexLogger.error("  <gray>Override run(ScriptQueue, Instruction), or declare run(...) "
                    + "with one parameter per argument in its syntax.");
        }

        commands.put(command.getName().toLowerCase(), meta);

        for (String alias : command.getAlias()) {
            commands.put(alias.toLowerCase(), meta);
        }
    }

    /**
     * True when the command overrides the classic form rather than inheriting the
     * interface default, which only throws.
     */
    private static boolean implementsClassicRun(AbstractCommand command) {
        try {
            return !command.getClass()
                    .getMethod("run", ScriptQueue.class, Instruction.class)
                    .isDefault();
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    public CommandMetadata getMetadata(String name) {
        return commands.get(name.toLowerCase());
    }

    public AbstractCommand getCommand(String name) {
        CommandMetadata meta = getMetadata(name);
        return meta != null ? meta.command : null;
    }

    public static Map<String, CommandMetadata> getCommands() {
        return commands;
    }
}