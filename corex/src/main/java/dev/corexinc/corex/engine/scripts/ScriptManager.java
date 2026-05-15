package dev.corexinc.corex.engine.scripts;

import com.google.gson.Gson;
import dev.corexinc.corex.api.commands.DataBlockCommand;
import dev.corexinc.corex.api.containers.AbstractContainer;
import dev.corexinc.corex.api.containers.PathType;
import dev.corexinc.corex.engine.CorexRegistry;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.compiler.ScriptCompiler;
import dev.corexinc.corex.engine.utils.CorexLogger;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ScriptManager {

    private static final Map<String, AbstractContainer> containers = new HashMap<>();
    private static final Gson GSON = new Gson();
    public static long lastReloadTime = System.currentTimeMillis();

    private static Path dataFolder;
    private static CorexRegistry registry;

    public static void loadScripts() {
        containers.clear();
        File scriptsFolder = new File(dataFolder.toFile(), "scripts");
        if (!scriptsFolder.exists()) {
            scriptsFolder.mkdirs();
            File readme = new File(scriptsFolder, "readme.txt");
            try (var stream = ScriptManager.class.getResourceAsStream("/scripts/readme.txt")) {
                if (stream != null) Files.copy(stream, readme.toPath());
            } catch (Exception ignored) {}
        }

        List<File> files = new ArrayList<>();
        findScriptsRecursively(scriptsFolder, files);
        int loadedCount = 0;

        for (File file : files) {
            try {
                List<String> rawLines = Files.readAllLines(file.toPath());
                String cleanYaml = ScriptPreprocessor.preprocess(rawLines);

                Map<String, Object> parsed = new Yaml().load(cleanYaml);
                if (parsed == null) continue;

                for (Map.Entry<String, Object> entry : parsed.entrySet()) {
                    String scriptName = entry.getKey();
                    if (!(entry.getValue() instanceof Map<?, ?> rawSection)) continue;

                    @SuppressWarnings("unchecked")
                    Map<String, Object> section = (Map<String, Object>) rawSection;

                    if (!(section.get("type") instanceof String type)) continue;

                    Class<? extends AbstractContainer> clazz = registry.getContainerClass(type);
                    if (clazz == null) {
                        CorexLogger.warn("Script " + scriptName + " is using unknown type: " + type);
                        continue;
                    }

                    AbstractContainer container = clazz.getDeclaredConstructor().newInstance();
                    container.init(scriptName, GSON.toJsonTree(section).getAsJsonObject());

                    for (String path : flatKeys(section)) {
                        if (container.resolvePath(path) == PathType.SCRIPT) {
                            List<?> rawCommands = getNestedList(section, path);
                            if (rawCommands != null) container.addCompiledScript(path, compileBlock(rawCommands));
                        }
                    }

                    containers.put(scriptName, container);
                    loadedCount++;
                }
            } catch (Exception e) {
                CorexLogger.error("ERROR while reloading script " + file.getName() + ": " + e.getMessage());
            }
        }
        CorexLogger.success("Loaded <aqua>" + loadedCount + "</aqua> containers!");
    }

    private static Set<String> flatKeys(Map<String, Object> map) {
        Set<String> keys = new LinkedHashSet<>();
        collectFlatKeys(map, "", keys);
        return keys;
    }

    private static void collectFlatKeys(Map<?, ?> map, String prefix, Set<String> keys) {
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String fullKey = prefix.isEmpty() ? entry.getKey().toString() : prefix + "." + entry.getKey();
            keys.add(fullKey);
            if (entry.getValue() instanceof Map<?, ?> nested) collectFlatKeys(nested, fullKey, keys);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<?> getNestedList(Map<String, Object> root, String path) {
        Object current = root;
        for (String part : path.split("\\.")) {
            if (!(current instanceof Map<?, ?> map)) return null;
            current = ((Map<String, Object>) map).get(part);
        }
        return current instanceof List<?> list ? list : null;
    }

    private static void findScriptsRecursively(File folder, List<File> list) {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) findScriptsRecursively(file, list);
            else if (file.getName().endsWith(".cx")) list.add(file);
        }
    }

    public static <T extends AbstractContainer> List<T> getContainersByType(Class<T> type) {
        List<T> result = new ArrayList<>();
        for (AbstractContainer container : containers.values()) {
            if (type.isInstance(container)) result.add(type.cast(container));
        }
        return result;
    }

    public static void reloadScripts() {
        lastReloadTime = System.currentTimeMillis();
        loadScripts();
    }

    public static AbstractContainer getContainer(String name) {
        return containers.get(name);
    }

    public static void setDataFolder(Path path) { dataFolder = path; }
    public static void setRegistry(CorexRegistry r) { registry = r; }

    public static Instruction[] compileBlock(List<?> rawList) {
        List<Instruction> bytecode = new ArrayList<>();

        for (Object obj : rawList) {
            if (obj instanceof String str) {
                Instruction inst = ScriptCompiler.compile(str);
                if (inst != null) bytecode.add(inst);
            }
            else if (obj instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    String rawKey = entry.getKey().toString().trim();
                    String cmdLine = rawKey.endsWith(":")
                            ? rawKey.substring(0, rawKey.length() - 1).trim()
                            : rawKey;

                    Object value = entry.getValue();

                    if (value instanceof List<?> inner) {
                        compileListEntry(cmdLine, inner, bytecode);
                    }
                    else if (value instanceof Map<?, ?> innerMap) {
                        compileMapEntry(cmdLine, innerMap, bytecode);
                    }
                }
            }
        }

        return bytecode.toArray(new Instruction[0]);
    }

    private static void compileListEntry(String cmdLine, List<?> inner, List<Instruction> bytecode) {
        Instruction probe = ScriptCompiler.compile(cmdLine, null);
        if (probe == null) return;

        if (probe.command instanceof DataBlockCommand) {
            probe.customData = inner;
            bytecode.add(probe);
        } else {
            Instruction inst = ScriptCompiler.compile(cmdLine, compileBlock(inner));
            if (inst != null) bytecode.add(inst);
        }
    }

    private static void compileMapEntry(String cmdLine, Map<?, ?> innerMap, List<Instruction> bytecode) {
        Instruction inst = ScriptCompiler.compile(cmdLine, null);
        if (inst == null) return;
        inst.customData = innerMap;
        bytecode.add(inst);
    }
}