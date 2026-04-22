package dev.corexinc.corex.environment.utils.commands.impl;

import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.utils.commands.CommandParser;
import dev.corexinc.corex.environment.utils.commands.TabCompleter;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import java.util.Collection;

public class RunCommand implements BasicCommand {

    @Override
    public @NonNull Collection<String> suggest(@NonNull CommandSourceStack commandSourceStack, String @NonNull[] args) {
        return TabCompleter.getSuggestions(args);
    }

    @Override
    public String permission() {
        return "corex.command.run";
    }

    @Override
    public void execute(@NonNull CommandSourceStack commandSourceStack, String @NonNull[] args) {
        if (args.length == 0) {
            commandSourceStack.getSender().sendMessage("§b[Corex] §fUsage: /run <command> <args>");
            return;
        }

        String rawLine = String.join(" ", args);
        Instruction[] instructions = CommandParser.compileScript(rawLine);

        if (instructions.length == 0) {
            return;
        }

        PlayerTag linkedPlayer = (commandSourceStack.getSender() instanceof Player) ? new PlayerTag((Player) commandSourceStack.getSender()) : null;

        ScriptQueue queue = new ScriptQueue(
                "RunQueue_" + System.currentTimeMillis(),
                instructions,
                false,
                linkedPlayer
        );
        queue.start();
    }
}