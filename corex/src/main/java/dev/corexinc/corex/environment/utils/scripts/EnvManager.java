package dev.corexinc.corex.environment.utils.scripts;

import dev.corexinc.corex.engine.utils.CorexLogger;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnvManager {

    private static final Map<String, String> secrets = new HashMap<>();

    public static void load(File dataFolder) {
        secrets.clear();
        File envFile = new File(dataFolder, "secrets.env");

        if (!envFile.exists()) {
            try {
                dataFolder.mkdirs();
                envFile.createNewFile();
            } catch (Exception ignored) {}
        }

        try {
            List<String> lines = Files.readAllLines(envFile.toPath());
            for (String line : lines) {
                String trimmed = line.trim();
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
