package dev.corexinc.corex.engine.registry;

import dev.corexinc.corex.api.commands.AbstractCommand;

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