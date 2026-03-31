package dev.corexmc.corex.engine.utils;

import java.io.File;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnvManager {

    private final Map<String, String> secrets = new HashMap<>();

    public void load(File dataFolder) {
        secrets.clear();
        File envFile = new File(dataFolder, "secrets.env");

        if (!envFile.exists()) {
            try { envFile.createNewFile(); } catch (Exception ignored) {}
            return;
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
            CorexLogger.success("Loaded <aqua>" + secrets.size() + "</aqua> secrets from secrets.env");
        } catch (Exception e) {
            CorexLogger.error("Error while reading secrets.env: " + e.getMessage());
        }
    }

    public String getSecret(String key) {
        return secrets.get(key);
    }
}