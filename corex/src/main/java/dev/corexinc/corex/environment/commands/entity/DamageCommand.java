package dev.corexinc.corex.environment.commands.entity;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.NamespacedKey;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/* @doc command
 *
 * @Name Damage
 * @Syntax damage [<entity>|...] [<#.#>] (source:<entity>) (cause:<causeType>)
 * @RequiredArgs 2
 * @MaxArgs 4
 * @ShortDescription Hurts the player or a list of entities.
 *
 * @Implements Hurt
 *
 * @Description
 * Does damage to a list of entities, or to any single entity.
 *
 * @Usage
 * // Used to damage an entity.
 * - damage <entity> 6.7
 *
 * @Usage
 * // Used to deal damage to an entity from the player.
 * - damage <entity> 1.0 source:<player>
 *
 * @Usage
 * // Used to deal damage to a player from an entity using an arrow
 * - damage <player> 10.0 source:<entity> cause:ARROW
 */

public class DamageCommand implements AbstractCommand {
    @Override
    public @NotNull String getName() {
        return "damage";
    }

    @Override
    public void run(@NotNull ScriptQueue queue, @NotNull Instruction instruction) {

        AbstractTag entity = null;
        ElementTag amount = null;

        List<Entity> entitiesToDamage = new ArrayList<>();

        AbstractTag source = instruction.getPrefixObject("source", queue);
        String cause = instruction.getPrefix("cause", queue);

        switch (instruction.getLinearObject(0, queue)) {
            case EntityTag e -> entity = e;
            case PlayerTag p -> entity = p;
            case ElementTag a -> amount = a;
            case ListTag l -> {
                entitiesToDamage.addAll(l.filter(PlayerTag.class, queue).stream().map(PlayerTag::getPlayer).toList());
                entitiesToDamage.addAll(l.filter(EntityTag.class, queue).stream().map(EntityTag::getEntity).toList());
            }
            case null -> {}
            default -> Debugger.echoError(queue, "Unknown data type for this command. Takes 1 argument: <entity> or damage amount");
        }

        switch (instruction.getLinearObject(1, queue)) {
            case EntityTag e -> entity = e;
            case PlayerTag p -> entity = p;
            case ElementTag a -> amount = a;
            case ListTag l -> {
                entitiesToDamage.addAll(l.filter(PlayerTag.class, queue).stream().map(PlayerTag::getPlayer).toList());
                entitiesToDamage.addAll(l.filter(EntityTag.class, queue).stream().map(EntityTag::getEntity).toList());
            }
            case null -> {}
            default -> Debugger.echoError(queue, "Unknown data type for this command. Takes 2 argument: <entity> or damage amount");
        }

        if (entity == null || amount == null) {
            Debugger.echoError(queue, "Unable to determine the Entity or Amount");
        }

        if (entitiesToDamage.isEmpty()) {

            if (entity instanceof PlayerTag playertag) {
                entitiesToDamage.add(playertag.getPlayer());
            } else if (entity instanceof EntityTag entitytag) {
                entitiesToDamage.add(entitytag.getEntity());
            }

        }

        if (entitiesToDamage.isEmpty()) {
            Debugger.echoError(queue, "Could not resolve any target entities.");
        }

        DamageType damageType = null;
        if (cause != null) {
            NamespacedKey key = NamespacedKey.minecraft(cause.toLowerCase());
            damageType = RegistryAccess.registryAccess().getRegistry(RegistryKey.DAMAGE_TYPE).get(key);
            if (damageType == null) {
                Debugger.echoError(queue, "An invalid CAUSE type has been specified");
            }
        }

        DamageType actualDamageType = damageType;
        if (actualDamageType == null) {
            actualDamageType = RegistryAccess.registryAccess()
                    .getRegistry(RegistryKey.DAMAGE_TYPE)
                    .get(NamespacedKey.minecraft("generic"));
        }

        DamageSource.Builder builder = DamageSource.builder(actualDamageType);

        if (source instanceof LocationTag locationTag) {
            builder.withDamageLocation(locationTag.getLocation());
        }
        else if (source instanceof EntityTag entityTag) {
            builder.withDirectEntity(entityTag.getEntity());
        }
        else if (source instanceof PlayerTag playerTag) {
            builder.withDirectEntity(playerTag.getPlayer());
        }

        DamageSource finalSource = builder.build();

        Debugger.report(queue, instruction,
                "Amount", amount,
                "Targets", new ListTag(entitiesToDamage).identify(),
                "Cause", cause,
                "Source", source.identify());

        for (Entity target : entitiesToDamage) {
            if (target != null && !target.isDead() && target instanceof Damageable damageable && amount != null) {
                damageable.damage(amount.asDouble(), finalSource);
            }
            else {
                Debugger.echoError(queue, "No damage was dealt to the entity. Possible causes: The entity does not exist, the entity is dead, or the entity cannot take damage.");
            }
        }
    }

    @Override
    public @NotNull String getSyntax() {
        return "[<entity>] [<#.#>] (source:<entity>) (cause:<causeType>)";
    }

    @Override
    public int getMinArgs() {
        return 2;
    }

    @Override
    public int getMaxArgs() {
        return 4;
    }
}
