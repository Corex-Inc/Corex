package dev.corexinc.corex.environment.utils.commands;

import dev.corexinc.corex.engine.compiler.ScriptCompiler;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.registry.ScriptCommandRegistry;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.Collection;
import java.util.List;

public class RunCommand implements BasicCommand {

    @Override
    public @NonNull Collection<String> suggest(@NonNull CommandSourceStack commandSourceStack, String @NonNull [] args) {
        if (args.length <= 1) {
            return ScriptCommandRegistry.getCommands().keySet();
        }
        return List.of();
    }

    @Override
    public String permission() {
        return "corex.command.run";
    }

    @Override
    public void execute(@NonNull CommandSourceStack commandSourceStack, String @NonNull [] args) {
        if (args.length == 0) {
            commandSourceStack.getSender().sendMessage("§b[Corex] §fUsage: /run <command> <args>");
        }

        String rawLine = String.join(" ", args);
        Instruction instruction = ScriptCompiler.compile(rawLine);

        PlayerTag linkedPlayer = (commandSourceStack.getSender() instanceof Player) ? new PlayerTag((Player) commandSourceStack.getSender()) : null;

        ScriptQueue queue = new ScriptQueue(
                "RunQueue_" + System.currentTimeMillis(),
                new Instruction[]{ instruction },
                false,
                linkedPlayer
        );
        queue.start();
    }
}