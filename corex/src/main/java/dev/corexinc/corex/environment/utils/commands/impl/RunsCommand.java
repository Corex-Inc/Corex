package dev.corexinc.corex.environment.utils.commands.impl;

import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.utils.commands.CommandParser;
import dev.corexinc.corex.environment.utils.commands.TabCompleter;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class RunsCommand implements BasicCommand {

    private static final Map<String, ScriptQueue> activeQueues = new HashMap<>();

    @Override
    public @NonNull Collection<String> suggest(@NonNull CommandSourceStack commandSourceStack, String @NonNull[] args) {
        return TabCompleter.getSuggestions(args);
    }

    @Override
    public String permission() {
        return "corex.command.runs";
    }

    @Override
    public void execute(@NonNull CommandSourceStack commandSourceStack, String @NonNull[] args) {
        CommandSender sender = commandSourceStack.getSender();

        if (args.length == 0) {
            sender.sendMessage("§b[Corex] §fUsage: /runs <command> <args>");
            return;
        }

        String senderId = (sender instanceof Player) ? ((Player) sender).getUniqueId().toString() : "CONSOLE";
        String rawLine = String.join(" ", args);

        Instruction[] instructions = CommandParser.compileScript(rawLine);

        if (instructions.length == 0) {
            return;
        }

        ScriptQueue queue = activeQueues.get(senderId);

        if (queue == null || queue.isCancelled() || queue.isStopped()) {
            PlayerTag linkedPlayer = (sender instanceof Player) ? new PlayerTag((Player) sender) : null;

            queue = new ScriptQueue(
                    "SessionQueue_" + sender.getName(),
                    new Instruction[0],
                    false,
                    linkedPlayer
            );
            queue.setKeepAlive(true);
            activeQueues.put(senderId, queue);
            queue.start();
            sender.sendMessage("§b[Corex] §7New queue session created. Use §f- stop§7 to kill it.");
        }

        for (Instruction inst : instructions) {
            queue.injectInstructions(inst);
        }
    }
}