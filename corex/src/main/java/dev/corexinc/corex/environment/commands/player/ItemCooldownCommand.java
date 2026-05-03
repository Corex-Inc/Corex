package dev.corexinc.corex.environment.commands.player;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.DurationTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.ItemTag;
import dev.corexinc.corex.environment.tags.world.MaterialTag;
import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/* @doc command
 *
 * @Name ItemCooldown
 * @Syntax itemcooldown [<material>|<item>|<key>|...] (duration:<duration>) (targets:<player>|...)
 * @RequiredArgs 1
 * @MaxArgs 3
 * @ShortDescription Places a visual and mechanical cooldown on items in a player's inventory.
 *
 * @Implements ItemCooldown
 *
 * @Description
 * Places a cooldown on one or more items in the target player's inventory.
 * The player will see a gray sweeping animation over the item and will be prevented from using it
 * (e.g., throwing ender pearls, eating food) until the cooldown expires.
 *
 * Each pipe-separated element is resolved independently:
 * - MaterialTag (m@stone)                      → setCooldown(Material, ticks)
 * - ItemTag     (i@myCustomItem)               → setCooldown(ItemStack, ticks)
 * - ElementTag matching a material name        → setCooldown(Material, ticks)
 * - ElementTag as a namespaced key (ns:value)  → setCooldown(Key, ticks)
 *
 * If no duration is specified, defaults to 1 second.
 * A duration of "0s" clears an existing cooldown immediately.
 * If no targets are specified, defaults to the player linked to the queue.
 *
 * @Usage
 * // Use to place a 1-second cooldown on ender pearls.
 * - itemcooldown ender_pearl
 *
 * @Usage
 * // Use to place a 10-minute cooldown on a custom item.
 * - itemcooldown i@myCustomSword duration:10m
 *
 * @Usage
 * // Use to apply a 5-second cooldown via namespaced key.
 * - itemcooldown myplugin:special_item duration:5s
 *
 * @Usage
 * // Use to apply cooldowns on a mix of types for all online players.
 * - itemcooldown ender_pearl|i@myItem|myplugin:key duration:5s targets:<server.onlinePlayers>
 *
 * @Usage
 * // Use to clear a cooldown immediately.
 * - itemcooldown ender_pearl duration:0s
 */
public class ItemCooldownCommand implements AbstractCommand {

    private sealed interface CooldownTarget {
        record ByMaterial(Material material) implements CooldownTarget {}
        record ByItem    (ItemStack item)    implements CooldownTarget {}
        record ByKey     (NamespacedKey key)  implements CooldownTarget {}
    }

    @Override public @NonNull String getName()   { return "itemcooldown"; }
    @Override public @NonNull String getSyntax() { return "[<material>|<item>|<key>|...] (duration:<duration>) (targets:<player>|...)"; }
    @Override public int getMinArgs()            { return 1; }
    @Override public int getMaxArgs()            { return 3; }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        String rawArg = instruction.getLinear(0, queue);
        if (rawArg == null) {
            Debugger.echoError(queue, "First argument cannot be null!");
            return;
        }

        List<CooldownTarget> cooldownTargets = new ListTag(rawArg).getList().stream()
                .map(tag -> resolveTarget(tag, queue))
                .filter(Objects::nonNull)
                .toList();

        if (cooldownTargets.isEmpty()) {
            Debugger.echoError(queue, "No valid cooldown targets provided.");
            return;
        }

        int durationTicks = resolveDuration(instruction, queue);
        if (durationTicks < 0) return;

        List<Player> targetPlayers = resolvePlayers(instruction, queue);
        if (targetPlayers.isEmpty()) {
            Debugger.echoError(queue, "No valid targets found to apply cooldown.");
            return;
        }

        String targetsRaw = instruction.getPrefix("targets", queue);
        Debugger.report(queue, instruction,
                "Arg",           rawArg,
                "Duration",      durationTicks + "t",
                "Targets",       targetsRaw != null ? targetsRaw : "Linked Player",
                "Targets_Count", targetPlayers.size()
        );

        final int finalTicks = durationTicks;
        for (Player player : targetPlayers) {
            SchedulerAdapter.runEntity(player, () -> applyCooldowns(player, cooldownTargets, finalTicks));
        }
    }

    private static void applyCooldowns(Player player, List<CooldownTarget> targets, int ticks) {
        for (CooldownTarget target : targets) {
            switch (target) {
                case CooldownTarget.ByMaterial(var mat)  -> player.setCooldown(mat, ticks);
                case CooldownTarget.ByItem    (var item) -> player.setCooldown(item, ticks);
                case CooldownTarget.ByKey     (var key)  -> player.setCooldown(key, ticks);
            }
        }
    }

    private @Nullable CooldownTarget resolveTarget(AbstractTag tag, ScriptQueue queue) {
        return switch (tag) {
            case MaterialTag mt -> {
                if (mt.getMaterial() != null && mt.getMaterial().isItem())
                    yield new CooldownTarget.ByMaterial(mt.getMaterial());
                Debugger.echoError(queue, "'" + mt.identify() + "' is not a valid item material.");
                yield null;
            }
            case ItemTag it -> {
                ItemStack stack = it.getItemStack();
                if (stack != null && stack.getType().isItem())
                    yield new CooldownTarget.ByItem(stack);
                Debugger.echoError(queue, "'" + it.identify() + "' has no valid item stack.");
                yield null;
            }
            default -> resolveFromString(tag.identify(), queue);
        };
    }

    private @Nullable CooldownTarget resolveFromString(String val, ScriptQueue queue) {
        Material mat = Material.matchMaterial(val.toUpperCase());
        if (mat != null) {
            if (mat.isItem()) return new CooldownTarget.ByMaterial(mat);
            Debugger.echoError(queue, "'" + val + "' is not a valid item material.");
            return null;
        }
        NamespacedKey key = NamespacedKey.fromString(val);
        if (key == null) {
            Debugger.echoError(queue, "'" + val + "' is not a valid material name or namespaced key.");
            return null;
        }
        return new CooldownTarget.ByKey(key);
    }

    private int resolveDuration(Instruction instruction, ScriptQueue queue) {
        AbstractTag durationTag = instruction.getPrefixObject("duration", queue);
        if (durationTag == null) return 20;
        try {
            DurationTag dt = durationTag instanceof DurationTag d ? d : new DurationTag(durationTag.identify());
            return Math.max(0, (int) dt.getTicks());
        } catch (Exception e) {
            Debugger.echoError(queue, "Invalid duration: " + durationTag.identify());
            return -1;
        }
    }

    private List<Player> resolvePlayers(Instruction instruction, ScriptQueue queue) {
        String targetsRaw = instruction.getPrefix("targets", queue);
        if (targetsRaw != null) {
            return new ListTag(targetsRaw).filter(PlayerTag.class, queue).stream()
                    .map(PlayerTag::getPlayer)
                    .filter(p -> p != null && p.isOnline())
                    .toList();
        }
        PlayerTag linked = queue.getPlayer();
        if (linked != null && linked.getPlayer() != null && linked.getPlayer().isOnline())
            return List.of(linked.getPlayer());
        return List.of();
    }
}