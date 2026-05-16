package dev.corexinc.corex.environment.commands.entity;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/* @doc command
 *
 * @Name Kill
 * @Syntax kill (<entity>|...)
 * @RequiredArgs 0
 * @MaxArgs 1
 * @ShortDescription Kills the player or a list of entities.
 *
 * @Description
 * Kills one or more living entities.
 * If no entity is specified, targets the linked player.
 * Fires death events. Invulnerable entities are force-killed via setHealth(0).
 *
 * @Usage
 * // Kill the linked player.
 * - kill
 *
 * @Usage
 * // Kill a specific entity.
 * - kill <entity>
 *
 * @Usage
 * // Kill all entities in a list.
 * - kill <player>|<npc>|<entity[uuid]>
 */
@SuppressWarnings("UnstableApiUsage")
public class KillCommand implements AbstractCommand {

    private static final DamageSource GENERIC_SOURCE = DamageSource.builder(
            Objects.requireNonNull(RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.DAMAGE_TYPE)
                    .get(NamespacedKey.minecraft("generic")))
    ).build();

    @Override
    public @NotNull String getName() {
        return "kill";
    }

    @Override
    public @NotNull List<String> getAlias() {
        return List.of();
    }

    @Override
    public @NotNull String getSyntax() {
        return "(<entity>|...)";
    }

    @Override
    public int getMinArgs() {
        return 0;
    }

    @Override
    public int getMaxArgs() {
        return 1;
    }

    @Override
    public void run(@NotNull ScriptQueue queue, @NotNull Instruction instruction) {
        List<Entity> targets = new ArrayList<>();

        switch (instruction.getLinearObject(0, queue)) {
            case PlayerTag p -> targets.add(p.getPlayer());
            case EntityTag e -> targets.add(e.getEntity());
            case ListTag l -> {
                l.filter(PlayerTag.class, queue).forEach(p -> targets.add(p.getPlayer()));
                l.filter(EntityTag.class, queue).forEach(e -> targets.add(e.getEntity()));
            }
            case null -> {
                PlayerTag player = (PlayerTag) queue.getPlayer();
                if (player == null) {
                    Debugger.echoError(queue, "Kill command requires an entity argument when there is no linked player!");
                    return;
                }
                targets.add(player.getPlayer());
            }
            default -> {
                Debugger.echoError(queue, "Invalid argument — expected an entity or list of entities.");
                return;
            }
        }

        Debugger.report(queue, instruction, "targets", new ListTag(targets).identify());

        for (Entity target : targets) {
            if (target == null || target.isDead()) {
                Debugger.echoError(queue, "Skipping entity: null or already dead.");
                continue;
            }
            if (!(target instanceof LivingEntity damageable)) {
                Debugger.echoError(queue, "Cannot kill '" + target + "': not a damageable entity.");
                continue;
            }
            if (target.isInvulnerable()) {
                damageable.setHealth(0);
            } else {
                damageable.damage(Objects.requireNonNull(damageable.getAttribute(Attribute.MAX_HEALTH)).getValue() + 1, GENERIC_SOURCE);
                if (damageable.getHealth() > 0) {
                    damageable.setHealth(0);
                }
            }
        }
    }
}