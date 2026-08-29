package dev.corexinc.corex.velocity.environment.utils.commands.impl;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.Player;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.utils.commands.CommandParser;
import dev.corexinc.corex.environment.utils.commands.TabCompleter;
import dev.corexinc.corex.velocity.environment.tags.player.PlayerTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class VRunsCommand implements SimpleCommand {

    private static final Map<String, ScriptQueue> activeQueues = new ConcurrentHashMap<>();

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        ScriptQueue queue = activeQueues.remove(event.getPlayer().getUniqueId().toString());
        if (queue != null) {
            queue.stopEntireQueue();
        }
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            source.sendMessage(Component.text("[Corex] ", NamedTextColor.AQUA)
                    .append(Component.text("Usage: /vruns <command> <args>", NamedTextColor.WHITE)));
            return;
        }

        String senderId = (source instanceof Player player) ? player.getUniqueId().toString() : "CONSOLE";
        String rawLine = String.join(" ", args);

        Instruction[] instructions = CommandParser.compileScript(rawLine);

        if (instructions.length == 0) {
            return;
        }

        ScriptQueue queue = activeQueues.get(senderId);
        Player executor = (source instanceof Player player) ? player : null;

        if (queue == null || queue.isCancelled() || queue.isStopped()) {
            PlayerTag linkedPlayer = (executor != null) ? new PlayerTag(executor) : null;

            queue = new ScriptQueue(
                    "SessionQueue_" + (executor != null ? executor.getUsername() : "CONSOLE"),
                    new Instruction[0],
                    false,
                    linkedPlayer
            );
            queue.setKeepAlive(true);
            if (executor != null) queue.setDebugObserver(executor);
            activeQueues.put(senderId, queue);
            queue.start();
            source.sendMessage(Component.text("[Corex] ", NamedTextColor.AQUA)
                    .append(Component.text("New queue session created. Use ", NamedTextColor.GRAY))
                    .append(Component.text("- stop", NamedTextColor.WHITE))
                    .append(Component.text(" to kill it.", NamedTextColor.GRAY)));
        } else if (executor != null) {
            queue.setDebugObserver(executor);
        }

        for (Instruction inst : instructions) {
            queue.injectInstructions(inst);
        }
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().getPermissionValue("corex.command.vruns").equals(Tristate.TRUE);
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.supplyAsync(() -> TabCompleter.getSuggestions(invocation.arguments()));
    }
}
