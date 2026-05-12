package dev.corexinc.corex.nms.v1_21_11;

import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.environment.utils.BukkitSchedulerAdapter;
import dev.corexinc.corex.environment.utils.adapters.PlayerAdapter;
import io.papermc.paper.adventure.PaperAdventure;
import net.kyori.adventure.text.Component;
import net.minecraft.advancements.*;
import net.minecraft.advancements.criterion.ImpossibleTrigger;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Material;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Player;

import java.util.*;

public class PlayerAdapterImpl implements PlayerAdapter {

    private static final String CRITERIA_KEY = "impossible";

    @Override
    public void sendReconfiguration(Player player) {
        ((BukkitSchedulerAdapter) SchedulerAdapter.get()).runEntity(player, () -> {
            ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
            nmsPlayer.connection.switchToConfig();
            monitorTransition(nmsPlayer);
        });
    }

    private void monitorTransition(ServerPlayer nmsPlayer) {
        SchedulerAdapter.get().run(() -> {

            Object listener = nmsPlayer.connection.connection.getPacketListener();

            if (listener instanceof ServerConfigurationPacketListenerImpl configListener) {
                configListener.startConfiguration();
            } else {
                SchedulerAdapter.get().runLater(() -> monitorTransition(nmsPlayer), 1);
            }
        });
    }

    @Override
    public void sendToast(Player player, Component message, Material icon, String frame) {
        AdvancementType type = switch (frame.toLowerCase()) {
            case "goal" -> AdvancementType.GOAL;
            case "challenge" -> AdvancementType.CHALLENGE;
            default -> AdvancementType.TASK;
        };

        net.minecraft.world.item.ItemStack nmsIcon = CraftItemStack.asNMSCopy(new org.bukkit.inventory.ItemStack(icon));
        net.minecraft.network.chat.Component nmsTitle = PaperAdventure.asVanilla(message);
        net.minecraft.network.chat.Component nmsDescription = net.minecraft.network.chat.Component.empty();

        DisplayInfo displayInfo = new DisplayInfo(
                nmsIcon,
                nmsTitle,
                nmsDescription,
                Optional.empty(),
                type,
                true,
                false,
                true
        );

        Identifier id = Identifier.fromNamespaceAndPath("corex", "toast_" + UUID.randomUUID().toString());

        Criterion<?> impossibleCrit = new Criterion<>(new ImpossibleTrigger(), new ImpossibleTrigger.TriggerInstance());
        Map<String, Criterion<?>> criteria = Map.of(CRITERIA_KEY, impossibleCrit);
        AdvancementRequirements requirements = new AdvancementRequirements(List.of(List.of(CRITERIA_KEY)));

        Advancement advancement = new Advancement(
                Optional.empty(),
                Optional.of(displayInfo),
                AdvancementRewards.EMPTY,
                criteria,
                requirements,
                false,
                Optional.empty()
        );

        AdvancementHolder holder = new AdvancementHolder(id, advancement);

        AdvancementProgress progress = new AdvancementProgress();
        progress.update(requirements);
        progress.grantProgress(CRITERIA_KEY);

        ClientboundUpdateAdvancementsPacket addPacket = new ClientboundUpdateAdvancementsPacket(
                false,
                List.of(holder),
                Set.of(),
                Map.of(id, progress),
                true
        );

        ClientboundUpdateAdvancementsPacket removePacket = new ClientboundUpdateAdvancementsPacket(
                false,
                List.of(),
                Set.of(id),
                Map.of(),
                true
        );

        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();

        serverPlayer.connection.send(addPacket);
        serverPlayer.connection.send(removePacket);
    }

    @Override
    public void sendRelativeLookPacket(Player player, float relYaw, float relPitch) {
        ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();

        ClientboundPlayerPositionPacket packet = ClientboundPlayerPositionPacket.of(
                0,
                new PositionMoveRotation(Vec3.ZERO, Vec3.ZERO, relYaw, relPitch),
                EnumSet.allOf(Relative.class)
        );

        nmsPlayer.connection.send(packet);
    }
}
