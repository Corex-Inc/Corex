package dev.corexmc.corex.environment.commands.player;

import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.engine.compiler.Instruction;
import dev.corexmc.corex.engine.queue.ScriptQueue;
import dev.corexmc.corex.environment.tags.PlayerTag;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public class TeleportCommand implements AbstractCommand {

    private final String name = "teleport";

    private String syntax = "[<text>] (targets:<player>|...) (from:<uuid>)";

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction entry) {
        PlayerTag queuePlayer = queue.getPlayer();
        Player player = queuePlayer.getPlayer();
        /*
        Corex.getSchedulerAdapter().run(() -> {

        });
        */
        if (queue.isAcync()) {
//            player.teleportAsync();
            return;
        }
//        player.teleport();
    }

    @Override
    public @NonNull String getName() {
        return name;
    }

    @Override
    public @NonNull String getSyntax() {
        return syntax;
    }

    @Override
    public void setSyntax(@NonNull String syntax) {
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
