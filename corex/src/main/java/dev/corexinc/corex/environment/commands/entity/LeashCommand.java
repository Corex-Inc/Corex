package dev.corexinc.corex.environment.commands.entity;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LeashHitch;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/* @doc command
 *
 * @Name Leash
 * @Syntax leash [<entity>|...] (holder:<entity>/<location>) (cancel)
 * @RequiredArgs 1
 * @MaxArgs 3
 * @ShortDescription Attaches or removes a leash on one or more entities.
 *
 * @Description
 * Attaches a leash to one or more living entities.
 * The holder can be another entity or a fence location.
 * If a location is given, a LeashHitch is spawned on it — only fence blocks are valid.
 * Specify 'cancel' to remove the leash from the given entities.
 * If no holder is given and 'cancel' is not specified, defaults to the linked player.
 *
 * @Usage
 * // Leash an entity to the player.
 * - leash <entity> holder:<player>
 *
 * @Usage
 * // Leash an entity to a fence the player is looking at.
 * - leash <entity> holder:<player.cursor_on>
 *
 * @Usage
 * // Remove the leash from an entity.
 * - leash <entity> cancel
 */
public class LeashCommand implements AbstractCommand {

    @Override
    public @NotNull String getName() {
        return "leash";
    }

    @Override
    public @NotNull List<String> getAlias() {
        return List.of();
    }

    @Override
    public @NotNull String getSyntax() {
        return "[<entity>|...] (holder:<entity>/<location>) (cancel)";
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
    public void run(@NotNull ScriptQueue queue, @NotNull Instruction instruction) {
        boolean cancel = instruction.hasFlag("cancel");

        List<LivingEntity> targets = new ArrayList<>();
        switch (instruction.getLinearObject(0, queue)) {
            case PlayerTag p -> targets.add(p.getPlayer());
            case EntityTag e -> {
                if (e.getEntity() instanceof LivingEntity le) targets.add(le);
                else Debugger.echoError(queue, "Entity '" + e.identify() + "' is not a living entity, skipping.");
            }
            case ListTag l -> {
                l.filter(PlayerTag.class, queue).forEach(p -> targets.add(p.getPlayer()));
                l.filter(EntityTag.class, queue).stream()
                        .filter(e -> e.getEntity() instanceof LivingEntity)
                        .forEach(e -> targets.add((LivingEntity) e.getEntity()));
            }
            case null -> {
                Debugger.echoError(queue, "Must specify an entity or list of entities!");
                return;
            }
            default -> {
                Debugger.echoError(queue, "Invalid argument — expected an entity or list of entities.");
                return;
            }
        }

        if (targets.isEmpty()) {
            Debugger.echoError(queue, "No valid living entities found to leash.");
            return;
        }

        Entity holderEntity = null;

        if (!cancel) {
            AbstractTag holderTag = instruction.getPrefixObject("holder", queue);

            switch (holderTag) {
                case null -> {
                    PlayerTag player = (PlayerTag) queue.getPlayer();
                    if (player == null) {
                        Debugger.echoError(queue, "Must specify 'holder:<entity/location>' when there is no linked player!");
                        return;
                    }
                    holderEntity = player.getPlayer();
                }
                case PlayerTag p -> holderEntity = p.getPlayer();
                case EntityTag e -> holderEntity = e.getEntity();
                case LocationTag loc -> {
                    if (!loc.getLocation().getBlock().getType().name().endsWith("_FENCE")) {
                        Debugger.echoError(queue, "Invalid holder location — only fence blocks are permitted!");
                        return;
                    }
                    holderEntity = loc.getLocation().getWorld().spawn(loc.getLocation(), LeashHitch.class);
                }
                default -> {
                    Debugger.echoError(queue, "Invalid holder type — must be an entity or fence location.");
                    return;
                }
            }
        }

        Debugger.report(queue, instruction,
                "targets", new ListTag(targets).identify(),
                "holder", holderEntity != null ? holderEntity.toString() : "none",
                "cancel", cancel);

        final Entity finalHolder = holderEntity;
        for (LivingEntity target : targets) {
            if (target.isDead()) {
                Debugger.echoError(queue, "Skipping '" + target + "': entity is dead.");
                continue;
            }
            target.setLeashHolder(cancel ? null : finalHolder);
        }
    }
}