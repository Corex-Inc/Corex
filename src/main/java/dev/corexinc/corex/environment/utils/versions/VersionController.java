package dev.corexinc.corex.environment.utils.versions;

import dev.corexinc.corex.environment.utils.versions.adapters.CustomModelDataAdapter;
import org.bukkit.Bukkit;

public class VersionController {
    private static final String VERSION = Bukkit.getBukkitVersion();

    private static final CustomModelDataAdapter customModelDataAdapter;

    static {
        if (isAtLeast("1.21.4")) {
            customModelDataAdapter = new dev.corexinc.corex.environment.utils.versions.v1_21_4.CustomModelData();
        } else {
            customModelDataAdapter = new dev.corexinc.corex.environment.utils.versions.v1_21_3.CustomModelData();
        }
    }


    public static CustomModelDataAdapter getCustomModelDataAdapter() {
        return customModelDataAdapter;
    }

    public static boolean isAtLeast(String required) {
        String[] c = VERSION.split("-")[0].split("\\.");
        String[] r = required.split("\\.");
        for (int i = 0; i < Math.max(c.length, r.length); i++) {
            int cv = i < c.length ? Integer.parseInt(c[i]) : 0;
            int rv = i < r.length ? Integer.parseInt(r[i]) : 0;
            if (cv != rv) return cv > rv;
        }
        return true;
    }
}
