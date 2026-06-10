package dev.corexinc.corex.environment.utils.adapters;

import dev.corexinc.corex.environment.utils.dialog.DialogSpec;
import org.bukkit.entity.Player;

import java.util.Map;

public interface DialogAdapter {

    void show(Player player, DialogSpec spec, Callback callback);

    void close(Player player);

    interface Callback {
        void onButton(Player player, String dialogName, String buttonId, Map<String, String> responses);
    }
}
