package dev.corexinc.corex.velocity.environment.utils.commands.impl;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.environment.utils.commands.CommandParser;
import dev.corexinc.corex.environment.utils.commands.TabCompleter;
import dev.corexinc.corex.velocity.environment.tags.player.PlayerTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class VRunCommand implements SimpleCommand {

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            source.sendMessage(Component.text("[Corex] ", NamedTextColor.AQUA)
                    .append(Component.text("Usage: /vrun <command> <args>", NamedTextColor.WHITE)));
            return;
        }

        String rawLine = String.join(" ", args);
        Instruction[] instructions = CommandParser.compileScript(rawLine);

        if (instructions.length == 0) {
            return;
        }

        PlayerTag linkedPlayer = (source instanceof Player p) ? new PlayerTag(p) : null;

        ScriptQueue queue = new ScriptQueue(
                "VRunQueue_" + System.currentTimeMillis(),
                instructions,
                false,
                linkedPlayer
        );
        queue.start();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("corex.command.vrun");
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        return CompletableFuture.supplyAsync(() -> TabCompleter.getSuggestions(invocation.arguments()));
    }
}