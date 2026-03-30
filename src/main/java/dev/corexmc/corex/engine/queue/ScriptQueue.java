package dev.corexmc.corex.engine.queue;

import dev.corexmc.corex.Corex;
import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.engine.compiler.Instruction;
import dev.corexmc.corex.engine.utils.CorexLogger;
import dev.corexmc.corex.environment.tags.PlayerTag;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class ScriptQueue {
    private final String id;

    private final Instruction[] bytecode;
    private int pointer = 0;

    private boolean isPaused = false;
    private boolean isAsync;

    private PlayerTag linkedPlayer;

    public boolean isAcync() {
        return isAcync();
    }

    private final ConcurrentHashMap<String, AbstractTag> definitions = new ConcurrentHashMap<>();

    public ScriptQueue(String id, Instruction[] bytecode, boolean isAsync, PlayerTag linkedPlayer) {
        this.id = id;
        this.bytecode = bytecode;
        this.isAsync = isAsync;
        this.linkedPlayer = linkedPlayer;
    }

    public void start() {
        executeNext();
    }

    public void executeNext() {
        while (!isPaused && pointer < bytecode.length) {
            Instruction inst = bytecode[pointer];
            pointer++;

            try {
                inst.command.run(this, inst);
            } catch (Exception e) {
                CorexLogger.error("ERROR while executing script command in " + id + ": " + e.getMessage());
                e.printStackTrace();
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