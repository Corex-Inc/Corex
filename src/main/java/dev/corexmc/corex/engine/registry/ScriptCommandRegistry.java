package dev.corexmc.corex.engine.registry;

import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.engine.utils.CorexLogger;

import java.util.HashMap;
import java.util.Map;

public class ScriptCommandRegistry {
    private final Map<String, AbstractCommand> commands = new HashMap<>();

    public void register(AbstractCommand command) {
        String name = command.getName().toLowerCase();

        if (commands.containsKey(name)) {
            CorexLogger.warn("Script command '<aqua>" + name + "<white>' is already registered! Override...");
        }

        commands.put(name, command);
    }

    public AbstractCommand getCommand(String name) {
        return commands.get(name.toLowerCase());
    }

    public Map<String, AbstractCommand> getAllCommands() {
        return commands;
    }
}
