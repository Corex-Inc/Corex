package dev.corexinc.corex.engine.utils;

import dev.corexinc.corex.Corex;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnvManager {

    private static final Map<String, String> secrets = new HashMap<>();

    // Теперь принимаем инстанс плагина
    public static void load() {
        JavaPlugin plugin = Corex.getInstance();
        secrets.clear();
        File envFile = new File(plugin.getDataFolder(), "secrets.env");

        // Если файла нет в папке плагина
        if (!envFile.exists()) {
            try {
                plugin.saveResource("secrets.env", false);
            } catch (IllegalArgumentException e) {
                try {
                    if (!plugin.getDataFolder().exists()) {
                        plugin.getDataFolder().mkdirs();
                    }
                    envFile.createNewFile();
                } catch (Exception ignored) {}
            }
        }

        try {
            List<String> lines = Files.readAllLines(envFile.toPath());
            for (String line : lines) {
                String trimmed = line.trim();
                // Пропускаем пустые строки и комментарии
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;

                int eqIndex = trimmed.indexOf('=');
                if (eqIndex > 0) {
                    String key = trimmed.substring(0, eqIndex).trim();
                    String value = trimmed.substring(eqIndex + 1).trim();

                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    secrets.put(key, value);
                }
            }
        } catch (Exception e) {
            CorexLogger.error("Error while reading secrets.env: " + e.getMessage());
        }
    }

    public static String getSecret(String key) {
        return secrets.get(key);
    }
}