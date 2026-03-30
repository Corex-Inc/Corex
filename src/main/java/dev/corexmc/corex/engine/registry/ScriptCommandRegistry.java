package dev.corexmc.corex.engine.registry;

import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.engine.utils.CorexLogger;

import java.util.HashMap;
import java.util.Map;

public class ScriptCommandRegistry {

    private static final Map<String, CommandMetadata> commands = new HashMap<>();

    public void register(AbstractCommand command) {
        CommandMetadata meta = new CommandMetadata(command);

        commands.put(command.getName().toLowerCase(), meta);

        for (String alias : command.getAlias()) {
            commands.put(alias.toLowerCase(), meta);
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