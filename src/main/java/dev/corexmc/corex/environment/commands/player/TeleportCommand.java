package dev.corexmc.corex.environment.commands.player;

import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.engine.compiler.Instruction;
import dev.corexmc.corex.engine.queue.ScriptQueue;
import dev.corexmc.corex.engine.utils.SchedulerAdapter;
import dev.corexmc.corex.environment.tags.player.PlayerTag;
import dev.corexmc.corex.environment.tags.world.LocationTag;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

public class TeleportCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "teleport";
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<entity>] [<location>]";
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction entry) {
        PlayerTag queuePlayer = queue.getPlayer();
        Player player = queuePlayer.getPlayer();

        String rawLocation = entry.getLinear(0, queue);
        LocationTag location = new LocationTag(rawLocation);

        if (queue.isAsync()) {
            player.teleportAsync(location.getLocation());
            return;
        }

        SchedulerAdapter.runEntity(player, () -> player.teleport(location.getLocation()));
    }

    @Override
    public void setSyntax(@NonNull String syntax) {
        getSyntax();
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
