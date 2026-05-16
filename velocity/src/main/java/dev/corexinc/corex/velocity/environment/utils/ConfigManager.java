package dev.corexinc.corex.velocity.environment.utils;

import dev.corexinc.corex.engine.utils.CorexLogger;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class ConfigManager {

    private final Path file;
    private final String resourcePath;
    private Map<String, Object> data = new HashMap<>();

    public ConfigManager(Path dataFolder, String fileName) {
        this.file = dataFolder.resolve(fileName);
        this.resourcePath = "/" + fileName;
    }

    public void load() {
        if (Files.notExists(file)) {
            try {
                Files.createDirectories(file.getParent());
                try (InputStream in = ConfigManager.class.getResourceAsStream(resourcePath)) {
                    if (in != null) Files.copy(in, file);
                    else Files.createFile(file);
                }
            } catch (IOException e) {
                CorexLogger.error("Failed to create config " + file.getFileName() + ": " + e.getMessage());
            }
        }

        try (var reader = Files.newBufferedReader(file)) {
            Map<String, Object> loaded = new Yaml().load(reader);
            data = loaded != null ? loaded : new HashMap<>();
        } catch (IOException e) {
            CorexLogger.error("Failed to load config " + file.getFileName() + ": " + e.getMessage());
        }
    }

    public void reload() {
        load();
    }

    public void save() {
        try (var writer = Files.newBufferedWriter(file)) {
            new Yaml().dump(data, writer);
        } catch (IOException e) {
            CorexLogger.error("Failed to save config " + file.getFileName() + ": " + e.getMessage());
        }
    }

    public void set(String path, Object value) {
        String[] keys = path.split("\\.");
        Map<String, Object> current = data;

        for (int i = 0; i < keys.length - 1; i++) {
            Object next = current.get(keys[i]);
            if (!(next instanceof Map<?, ?>)) {
                Map<String, Object> newMap = new HashMap<>();
                current.put(keys[i], newMap);
                current = newMap;
            } else {
                current = (Map<String, Object>) next;
            }
        }

        current.put(keys[keys.length - 1], value);
    }


    private Object resolve(String path) {
        String[] keys = path.split("\\.");
        Object current = data;

        for (String key : keys) {
            if (!(current instanceof Map<?, ?> map)) return null;
            current = ((Map<String, Object>) map).get(key);
        }

        return current;
    }

    public String getString(String path, String def) {
        Object val = resolve(path);
        return val instanceof String s ? s : def;
    }

    public int getInt(String path, int def) {
        Object val = resolve(path);
        return val instanceof Number n ? n.intValue() : def;
    }

    public boolean getBoolean(String path, boolean def) {
        Object val = resolve(path);
        return val instanceof Boolean b ? b : def;
    }

    public double getDouble(String path, double def) {
        Object val = resolve(path);
        return val instanceof Number n ? n.doubleValue() : def;
    }

    public boolean contains(String path) {
        return resolve(path) != null;
    }
}