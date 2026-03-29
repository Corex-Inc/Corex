package dev.corexmc.corex.environment.commands;

import dev.corexmc.corex.engine.queue.CommandEntry;
import dev.corexmc.corex.engine.queue.ScriptQueue;
import dev.corexmc.corex.environment.tags.PlayerTag;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class ExCommand extends Command {

    public ExCommand() {
        super("ex", "Execute Corex script command", "/ex <command>", List.of("execute", "corex"));
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendMessage("§b[Corex] §fUsage: /ex <command> <args>");
            return true;
        }

        String rawLine = String.join(" ", args);
        CommandEntry entry = new CommandEntry(rawLine);

        PlayerTag linkedPlayer = (sender instanceof Player) ? new PlayerTag((Player) sender) : null;

        ScriptQueue queue = new ScriptQueue(
                "EX_" + System.currentTimeMillis(),
                Collections.singletonList(entry),
                false,
                linkedPlayer
        );
        queue.start();

        return true;
    }
}