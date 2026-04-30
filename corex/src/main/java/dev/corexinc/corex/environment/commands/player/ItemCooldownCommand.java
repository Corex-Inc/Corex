package dev.corexinc.corex.environment.commands.player;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.DurationTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.MaterialTag;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/* @doc command
 *
 * @Name ItemCooldown
 * @Syntax itemcooldown [<material>|...] (duration:<duration>) (targets:<player>|...)
 * @RequiredArgs 1
 * @MaxArgs 3
 * @ShortDescription Places a visual and mechanical cooldown on items in a player's inventory.
 *
 * @Implements ItemCooldown
 *
 * @Description
 * Places a cooldown on one or more item materials in the target player's inventory.
 * The player will see a gray sweeping animation over the item and will be prevented from using it
 * (e.g., throwing ender pearls, eating food) until the cooldown expires.
 *
 * If no duration is specified, it defaults to 1 second.
 * A duration of "0s" can be used to instantly clear an existing cooldown.
 *
 * If no targets are specified, it defaults to the player attached to the current script queue.
 * Note: The material must be a valid item. Block-only materials (like WATER or LAVA) will be ignored.
 *
 * @Usage
 * // Use to place a 1-second cooldown on using ender pearls.
 * - itemcooldown ender_pearl
 *
 * @Usage
 * // Use to place a 10-minute cooldown on golden apples.
 * - itemcooldown golden_apple duration:10m
 *
 * @Usage
 * // Use to apply a 5-second cooldown on multiple items for all online players.
 * - itemcooldown ender_pearl|chorus_fruit duration:5s targets:<server.onlinePlayers>
 *
 * @Usage
 * // Use to clear the cooldown of an item immediately.
 * - itemcooldown ender_pearl duration:0s
 */
public class ItemCooldownCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "itemcooldown";
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<material>|...] (duration:<duration>) (targets:<player>|...)";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 3;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        String rawMaterials = instruction.getLinear(0, queue);
        if (rawMaterials == null) {
            Debugger.echoError(queue, "Material argument cannot be null!");
            return;
        }

        List<MaterialTag> materialTags = new ListTag(rawMaterials).filter(MaterialTag.class, queue);
        List<Material> validMaterials = new ArrayList<>();

        for (MaterialTag matTag : materialTags) {
            Material material = matTag.getMaterial();
            if (material != null && material.isItem()) {
                validMaterials.add(material);
            } else {
                Debugger.echoError(queue, "Material '" + matTag.identify() + "' is not a valid item and cannot have a cooldown.");
            }
        }

        if (validMaterials.isEmpty()) {
            Debugger.echoError(queue, "No valid materials provided to set cooldown.");
            return;
        }

        String durationRaw = instruction.getPrefix("duration", queue);
        int durationTicks;
        if (durationRaw != null) {
            durationTicks = (int) new DurationTag(durationRaw).getTicks();
        } else {
            durationTicks = 20;
        }

        String targetsRaw = instruction.getPrefix("targets", queue);
        List<Player> targetPlayers = new ArrayList<>();

        if (targetsRaw != null) {
            List<PlayerTag> playerTags = new ListTag(targetsRaw).filter(PlayerTag.class, queue);
            for (PlayerTag pTag : playerTags) {
                Player player = pTag.getPlayer();
                if (player != null && player.isOnline()) {
                    targetPlayers.add(player);
                }
            }
        } else {
            PlayerTag queuePlayer = queue.getPlayer();
            if (queuePlayer != null && queuePlayer.getOfflinePlayer().isOnline()) {
                targetPlayers.add(queuePlayer.getPlayer());
            }
        }

        if (targetPlayers.isEmpty()) {
            Debugger.echoError(queue, "No valid targets found to apply cooldown.");
            return;
        }

        Debugger.report(queue, instruction,
                "Materials", rawMaterials,
                "Duration", durationTicks + "t",
                "Targets", targetsRaw != null ? targetsRaw : "Attached Player",
                "Targets_Count", targetPlayers.size()
        );

        for (Player player : targetPlayers) {
            SchedulerAdapter.runEntity(player, () -> {
                for (Material material : validMaterials) {
                    player.setCooldown(material, Math.max(0, durationTicks));
                }
            });
        }
    }
}