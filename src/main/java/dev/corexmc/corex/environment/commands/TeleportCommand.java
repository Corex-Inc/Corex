package dev.corexmc.corex.environment.commands;

import dev.corexmc.corex.Corex;
import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.engine.queue.CommandEntry;
import dev.corexmc.corex.engine.queue.ScriptQueue;

public class TeleportCommand implements AbstractCommand {

    private final String name = "teleport";

    private String syntax = "[<text>] (targets:<player>|...) (from:<uuid>)";

    @Override
    public void run(ScriptQueue queue, CommandEntry entry) {
        if (Corex.isFolia()) {
            // Folia code
            return;
        }
        // Bukkit code
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSyntax() {
        return syntax;
    }

    @Override
    public void setSyntax(String syntax) {
        this.syntax = syntax;
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 3;
    }
}
