package dev.corexinc.corex.environment.commands.player;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.MaterialTag;
import dev.corexinc.corex.environment.utils.BukkitSchedulerAdapter;
import dev.corexinc.corex.environment.utils.adapters.PlayerAdapter;
import dev.corexinc.corex.environment.utils.nms.NMSHandler;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/* @doc command
 *
 * @Name Toast
 * @Syntax toast[<text>] (icon:<material>) (frame:task/challenge/goal) (targets:<player>|...)
 * @RequiredArgs 1
 * @MaxArgs 4
 * @ShortDescription Sends a visual advancement popup (toast) to players.
 *
 * @Implements Toast
 *
 * @Description
 * Displays a pop-up advancement notification (often referred to as a "toast") in the top right corner of the player's screen.
 *
 * If no targets are specified, the toast is sent to the player attached to the current script queue.
 *
 * You can customize the visual appearance using the following arguments:
 * - `icon:` The material to display inside the popup. Defaults to DIAMOND if invalid or unspecified.
 * - `frame:` Determines the background shape and sound of the toast. Valid options:
 *   - `task` (Default): A standard green rounded notification.
 *   - `goal`: A standard green rounded notification, traditionally used for major progress.
 *   - `challenge`: A decorative purple notification that plays a distinct success sound.
 *
 * @Tags
 * None
 *
 * @Usage
 * // Show a simple notification to the attached player indicating a quest has started.
 * - toast "<&a>Quest Started!" icon:book
 *
 * @Usage
 * // Announce a major achievement to all online players with a special purple popup and sound.
 * - toast "<&l><&d>Server Goal Reached!" frame:challenge icon:nether_star targets:<server.onlinePlayers>
 *
 * @Usage
 * // Send a basic toast with default settings (Diamond icon, Task frame).
 * - toast "You discovered a new area."
 */
public class ToastCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "toast";
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<text>] (icon:<material>) (frame:task/challenge/goal) (targets:<player>|...)";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 4;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        AbstractTag textTag = instruction.getLinearObject(0, queue);
        if (textTag == null) {
            Debugger.echoError(queue, "Toast message cannot be empty!");
            return;
        }

        String frameRaw = instruction.getPrefix("frame", queue);
        final String frame = frameRaw != null ? frameRaw.toLowerCase() : "task";
        if (!frame.equals("task") && !frame.equals("challenge") && !frame.equals("goal")) {
            Debugger.echoError(queue, "Invalid frame '" + frame + "'. Valid frames are: task, challenge, goal.");
            return;
        }

        AbstractTag iconTag = instruction.getPrefixObject("icon", queue);
        Material tempIcon = Material.DIAMOND;
        if (iconTag != null) {
            MaterialTag matTag = iconTag instanceof MaterialTag ? (MaterialTag) iconTag : new MaterialTag(iconTag.identify());
            Material parsed = matTag.getMaterial();

            if (parsed != null && parsed.isItem()) {
                tempIcon = parsed;
            } else {
                Debugger.echoError(queue, "Invalid icon material: " + iconTag.identify() + ". Falling back to DIAMOND.");
            }
        }
        final Material iconMaterial = tempIcon;

        List<Player> targetPlayers = getTargets(queue, instruction);
        if (targetPlayers.isEmpty()) return;

        final Component message = textTag.asComponent() != null ? textTag.asComponent() : Component.text(textTag.identify());

        Debugger.report(queue, instruction,
                "Message", textTag.identify(),
                "Icon", iconMaterial.name(),
                "Frame", frame,
                "Targets_Count", targetPlayers.size()
        );

        PlayerAdapter adapter = NMSHandler.get().get(PlayerAdapter.class);
        if (adapter == null) {
            Debugger.echoError(queue, "ToastCommand could not find an active ToastAdapter for this server version! NMS implementation is required.");
            return;
        }

        for (Player player : targetPlayers) {
            ((BukkitSchedulerAdapter) SchedulerAdapter.get()).runEntity(player, () -> adapter.sendToast(player, message, iconMaterial, frame));
        }
    }

    private List<Player> getTargets(ScriptQueue queue, Instruction instruction) {
        String targetsRaw = instruction.getPrefix("targets", queue);
        List<Player> targetPlayers = new ArrayList<>();

        if (targetsRaw != null) {
            new ListTag(targetsRaw).filter(PlayerTag.class, queue).forEach(p -> {
                Player player = p.getPlayer();
                if (player != null && player.isOnline()) targetPlayers.add(player);
            });
        } else {
            PlayerTag queuePlayer = queue.getPlayer();
            if (queuePlayer != null && queuePlayer.getPlayer() != null && queuePlayer.getPlayer().isOnline()) {
                targetPlayers.add(queuePlayer.getPlayer());
            }
        }

        if (targetPlayers.isEmpty()) {
            if (targetsRaw == null) {
                Debugger.echoError(queue, "No online targets found and no player attached to the queue.");
            }
        }

        return targetPlayers;
    }
}