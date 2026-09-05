package dev.corexinc.corex.velocity.environment.utils;

import dev.corexinc.corex.engine.utils.CorexLogger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.BufferedReader;
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

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            Map<String, Object> loaded = new Yaml(new SafeConstructor(new LoaderOptions())).load(reader);
            data = loaded != null ? loaded : new HashMap<>();
        } catch (IOException e) {
            CorexLogger.error("Failed to load config " + file.getFileName() + ": " + e.getMessage());
        }
    }

    public void reload() {
        load();
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