package dev.corexinc.corex.environment.utils.commands.impl;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.Position;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.utils.commands.CommandParser;
import dev.corexinc.corex.environment.utils.commands.TabCompleter;
import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jspecify.annotations.NonNull;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.UUID;

public class RunsCommand implements BasicCommand, Listener {

    private static final Map<String, ScriptQueue> activeQueues = new ConcurrentHashMap<>();
    private static boolean listenerRegistered = false;

    public RunsCommand() {
        if (!listenerRegistered) {
            Bukkit.getPluginManager().registerEvents(this, Corex.getInstance());
            listenerRegistered = true;
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        ScriptQueue queue = activeQueues.remove(event.getPlayer().getUniqueId().toString());
        if (queue != null) {
            queue.stopEntireQueue();
        }
    }

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
        Player executor = (sender instanceof Player p) ? p : null;

        if (queue == null || queue.isCancelled() || queue.isStopped()) {
            PlayerTag linkedPlayer = (executor != null) ? new PlayerTag(executor) : null;

            Position anchor = (executor != null) ? toPosition(executor.getLocation()) : null;

            queue = new ScriptQueue(
                    "SessionQueue_" + sender.getName(),
                    new Instruction[0],
                    false,
                    linkedPlayer,
                    anchor
            );
            queue.setKeepAlive(true);
            if (executor != null) queue.setDebugObserver(executor);
            activeQueues.put(senderId, queue);
            queue.start();
            sender.sendMessage("§b[Corex] §7New queue session created. Use §f- stop§7 to kill it.");
        } else if (executor != null) {
            queue.setAnchorPosition(toPosition(executor.getLocation()));
            queue.setDebugObserver(executor);
        }

        for (Instruction inst : instructions) {
            queue.injectInstructions(inst);
        }
    }

    private static Position toPosition(Location loc) {
        UUID worldId = (loc.getWorld() != null) ? loc.getWorld().getUID() : null;
        return Position.of(worldId, loc.getX(), loc.getY(), loc.getZ());
    }
}