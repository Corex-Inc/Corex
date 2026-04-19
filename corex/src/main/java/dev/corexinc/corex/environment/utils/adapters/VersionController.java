package dev.corexinc.corex.environment.utils.adapters;

import dev.corexinc.corex.engine.utils.CorexLogger;
import org.bukkit.Bukkit;

public class VersionController {

    private static ItemAdapter itemAdapter;

    static {
        init();
    }

    public static void init() {
        if (itemAdapter != null) return;

        String versionString = Bukkit.getBukkitVersion().split("-")[0];
        String[] parts = versionString.split("\\.");

        int major = 1;
        int minor = 21;
        int patch = 0;

        try {
            major = Integer.parseInt(parts[0]);
            minor = Integer.parseInt(parts[1]);
            if (parts.length > 2) {
                patch = Integer.parseInt(parts[2]);
            }
        } catch (Exception ignored) {}

        String basePackage = VersionController.class.getPackage().getName();

        for (int p = patch; p >= 0; p--) {
            String className = basePackage + ".v" + major + "_" + minor + "_" + p + ".ItemAdapterImpl";

            try {
                Class<?> clazz = Class.forName(className);
                itemAdapter = (ItemAdapter) clazz.getDeclaredConstructor().newInstance();
                CorexLogger.info("NMS Adapter loaded successfully: <aqua>" + className + "</aqua>");
                return;
            } catch (ClassNotFoundException ignored) {
            } catch (Exception e) {
                CorexLogger.error("Failed to instantiate adapter " + className + ": " + e.getMessage());
                break;
            }
        }

        try {
            Class<?> clazz = Class.forName(basePackage + ".v1_21_3.ItemAdapterImpl");
            itemAdapter = (ItemAdapter) clazz.getDeclaredConstructor().newInstance();
            CorexLogger.warn("Exact NMS adapter not found for " + versionString + ". Using v1_21_3 fallback.");
            return;
        } catch (Exception ignored) {}

        CorexLogger.warn("No NMS adapters found! Using No-Op Mock Adapter (Safe for tests).");
    }

    public static ItemAdapter getItemAdapter() {
        if (itemAdapter == null) init();
        return itemAdapter;
    }

    public static boolean isAtLeast(String required) {
        String[] c = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
        String[] r = required.split("\\.");
        for (int i = 0; i < Math.max(c.length, r.length); i++) {
            int cv = i < c.length ? Integer.parseInt(c[i]) : 0;
            int rv = i < r.length ? Integer.parseInt(r[i]) : 0;
            if (cv != rv) return cv > rv;
        }
        return true;
    }
}