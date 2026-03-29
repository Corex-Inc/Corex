package dev.corexmc.corex.engine.queue;

import dev.corexmc.corex.Corex;
import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.engine.utils.CorexLogger;
import dev.corexmc.corex.environment.tags.PlayerTag; // Не забудь импортировать!
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ScriptQueue {
    private final String id;
    private final List<CommandEntry> entries;
    private int currentIndex = 0;

    private boolean isPaused = false;
    private boolean isAsync;

    private PlayerTag linkedPlayer;

    private final ConcurrentHashMap<String, AbstractTag> definitions = new ConcurrentHashMap<>();

    public ScriptQueue(String id, List<CommandEntry> entryList, boolean isAsync, PlayerTag linkedPlayer) {
        this.id = id;
        this.entries = entryList;
        this.isAsync = isAsync;
        this.linkedPlayer = linkedPlayer;
    }

    public void start() {
        executeNext();
    }

    public void executeNext() {
        while (!isPaused && currentIndex < entries.size()) {
            CommandEntry entry = entries.get(currentIndex);
            currentIndex++;

            AbstractCommand command = Corex.getInstance().getRegistry().getScriptCommands().getCommand(entry.getCommandName());

            if (command != null) {
                try {
                    command.run(this, entry);
                } catch (Exception e) {
                    CorexLogger.error("Ошибка при выполнении команды " + entry.getCommandName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                CorexLogger.warn("Неизвестная скриптовая команда: " + entry.getCommandName());
            }
        }
    }

    public void delay(Plugin plugin, long ticks) {
        this.isPaused = true;

        if (isAsync) {
            Bukkit.getAsyncScheduler().runDelayed(plugin, task -> resume(), ticks * 50, TimeUnit.MILLISECONDS);
        } else {
            Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task -> resume(), ticks);
        }
    }

    public void resume() {
        this.isPaused = false;
        executeNext();
    }

    public void define(String name, AbstractTag value) {
        if (value == null) {
            definitions.remove(name.toLowerCase());
        } else {
            definitions.put(name.toLowerCase(), value);
        }
    }

    public AbstractTag getDefinition(String name) {
        return definitions.get(name.toLowerCase());
    }

    public PlayerTag getPlayer() {
        AbstractTag definedPlayer = getDefinition("__player");
        if (definedPlayer instanceof PlayerTag) {
            return (PlayerTag) definedPlayer;
        }
        return linkedPlayer;
    }
}