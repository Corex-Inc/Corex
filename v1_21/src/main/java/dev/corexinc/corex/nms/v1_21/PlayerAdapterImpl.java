package dev.corexinc.corex.nms.v1_21;

import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.environment.utils.adapters.PlayerAdapter;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class PlayerAdapterImpl implements PlayerAdapter {
    @Override
    public void sendReconfiguration(Player player) {
        SchedulerAdapter.runEntity(player, () -> {
            ServerPlayer nmsPlayer = ((CraftPlayer) player).getHandle();
            nmsPlayer.connection.switchToConfig();
            monitorTransition(nmsPlayer);
        });
    }

    private void monitorTransition(ServerPlayer nmsPlayer) {
        SchedulerAdapter.run(() -> {

            Object listener = nmsPlayer.connection.connection.getPacketListener();

            if (listener instanceof ServerConfigurationPacketListenerImpl configListener) {
                configListener.startConfiguration();
            } else {
                SchedulerAdapter.runLater(() -> monitorTransition(nmsPlayer), 1);
            }
        });
    }
}
