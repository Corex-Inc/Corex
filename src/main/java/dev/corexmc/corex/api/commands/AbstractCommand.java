package dev.corexmc.corex.api.commands;

import dev.corexmc.corex.engine.queue.CommandEntry;
import dev.corexmc.corex.engine.queue.ScriptQueue;

import java.util.List;

public interface AbstractCommand {
    String getName();

    default List<String> getAlias() {
        return List.of(getName());
    };

    void run(ScriptQueue queue, CommandEntry entry);

    void setSyntax(String syntax);

    String getSyntax();

    int getMinArgs();
    int getMaxArgs();
}
