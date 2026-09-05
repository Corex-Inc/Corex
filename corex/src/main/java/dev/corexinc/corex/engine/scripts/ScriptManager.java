package dev.corexinc.corex.engine.scripts;

import com.google.gson.Gson;
import dev.corexinc.corex.api.commands.DataBlockCommand;
import dev.corexinc.corex.api.containers.AbstractContainer;
import dev.corexinc.corex.api.containers.PathType;
import dev.corexinc.corex.api.scripts.PreprocessStage;
import dev.corexinc.corex.api.scripts.ScriptComment;
import dev.corexinc.corex.api.scripts.ScriptSource;
import dev.corexinc.corex.engine.CorexRegistry;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.compiler.SlotAllocator;
import dev.corexinc.corex.engine.compiler.ScriptCompiler;
import dev.corexinc.corex.engine.utils.CorexLogger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class ScriptManager {

    private static volatile Map<String, AbstractContainer> containers = Map.of();
    private static final Gson GSON = new Gson();

    private static Path dataFolder;
    private static CorexRegistry registry;

    /**
     * Reads, preprocesses and compiles the whole scriptpack.
     *
     * <p>It runs in two passes. Every file is read, normalized and parsed first, and only once the
     * last one is in does anything get compiled. That ordering is what lets an addon pass see the
     * whole pack before a single command has been resolved, and it means a syntax error is reported
     * against the scripts as they finally are rather than as they were halfway through loading.</p>
     */
    public static void loadScripts() {
        File scriptsFolder = prepareScriptsFolder();

        List<File> files = new ArrayList<>();
        findScriptsRecursively(scriptsFolder, files);

        PreprocessorRegistry preprocessors = registry.getPreprocessors();
        preprocessors.reportChains();

        List<ScriptSource> sources = new ArrayList<>(files.size());
        for (File file : files) {
            ScriptSource source = readSource(file, preprocessors);
            if (source != null) sources.add(source);
        }

        preprocessors.runAllParsed(sources);

        Map<String, AbstractContainer> loaded = new HashMap<>();
        int loadedCount = 0;
        for (ScriptSource source : sources) {
            loadedCount += buildContainers(source, loaded);
        }

        containers = loaded;
        CorexLogger.success("Loaded <aqua>" + loadedCount + "</aqua> containers!");
    }

    private static File prepareScriptsFolder() {
        File scriptsFolder = new File(dataFolder.toFile(), "scripts");
        if (scriptsFolder.exists()) {
            return scriptsFolder;
        }

        scriptsFolder.mkdirs();
        File readme = new File(scriptsFolder, "readme.txt");
        try (java.io.InputStream stream = ScriptManager.class.getResourceAsStream("/scripts/readme.txt")) {
            if (stream != null) Files.copy(stream, readme.toPath());
        } catch (Exception ignored) {}
        return scriptsFolder;
    }

    @SuppressWarnings("unchecked")
    private static ScriptSource readSource(File file, PreprocessorRegistry preprocessors) {
        try {
            ScriptSource source = new ScriptSource(file.toPath(), Files.readAllLines(file.toPath()));
            preprocessors.runRawScript(source);

            if (preprocessors.hasStage(PreprocessStage.COMMENTS)) {
                List<ScriptComment> comments = new ArrayList<>();
                source.setYaml(ScriptNormalizer.preprocess(source.getLines(), comments));
                source.setComments(comments);
                preprocessors.runComments(source);
            } else {
                source.setYaml(ScriptNormalizer.preprocess(source.getLines()));
            }

            preprocessors.runRawYaml(source);

            Map<String, Object> parsed = parseYaml(source);
            if (parsed == null) return null;

            source.setTree((Map<String, Object>) restoreHashes(parsed));
            preprocessors.runParsedYaml(source);

            return source;
        } catch (Exception e) {
            CorexLogger.error("ERROR while reading script " + file.getName() + ": " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> parseYaml(ScriptSource source) {
        try {
            return (Map<String, Object>) new Yaml(new SafeConstructor(new LoaderOptions())).load(source.getYaml());
        } catch (Exception failure) {
            List<String> touched = new ArrayList<>(source.getTouchedBy(PreprocessStage.RAW_SCRIPT));
            touched.addAll(source.getTouchedBy(PreprocessStage.RAW_YAML));

            String blame = touched.isEmpty()
                    ? ""
                    : " It was rewritten by " + String.join(", ", touched) + ", which is the first thing to suspect.";

            CorexLogger.error("Script " + source.getFileName() + " is not valid YAML: "
                    + failure.getMessage() + "." + blame);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static int buildContainers(ScriptSource source, Map<String, AbstractContainer> loaded) {
        Map<String, Object> tree = source.getTree();
        if (tree == null) return 0;

        int built = 0;
        for (Map.Entry<String, Object> entry : tree.entrySet()) {
            String scriptName = entry.getKey();
            if (!(entry.getValue() instanceof Map<?, ?> rawSection)) continue;

            Map<String, Object> section = (Map<String, Object>) rawSection;

            if (!(section.get("type") instanceof String type)) continue;

            Class<? extends AbstractContainer> clazz = registry.getContainerClass(type);
            if (clazz == null) {
                CorexLogger.warn("Script " + scriptName + " is using unknown type: " + type);
                continue;
            }

            try {
                AbstractContainer container = clazz.getDeclaredConstructor().newInstance();
                container.init(scriptName, GSON.toJsonTree(section).getAsJsonObject());

                for (String path : flatKeys(section)) {
                    if (container.resolvePath(path) != PathType.SCRIPT) continue;

                    List<?> rawCommands = getNestedList(section, path);
                    if (rawCommands == null) continue;

                    List<?> block = registry.getPreprocessors()
                            .runScriptBlock(source, scriptName, path, rawCommands);
                    container.addCompiledScript(path, compileScript(block));
                }

                AbstractContainer previous = loaded.put(scriptName, container);
                if (previous != null) {
                    CorexLogger.warn("Duplicate script name '" + scriptName + "' - the definition in "
                            + source.getFileName() + " overrides an earlier one!");
                }
                built++;
            } catch (Exception e) {
                CorexLogger.error("ERROR while compiling script " + scriptName + " in "
                        + source.getFileName() + ": " + e.getMessage());
            }
        }
        return built;
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

    private static List<?> getNestedList(Map<String, Object> root, String path) {
        Object current = resolveNested(root, path);
        return current instanceof List<?> list ? list : null;
    }

    private static Object resolveNested(Map<?, ?> map, String path) {
        if (map.containsKey(path)) return map.get(path);

        int dotIndex = path.length();
        while ((dotIndex = path.lastIndexOf('.', dotIndex - 1)) > 0) {
            Object child = map.get(path.substring(0, dotIndex));
            if (child instanceof Map<?, ?> childMap) {
                Object resolved = resolveNested(childMap, path.substring(dotIndex + 1));
                if (resolved != null) return resolved;
            }
        }
        return null;
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
        loadScripts();
    }

    public static AbstractContainer getContainer(String name) {
        return containers.get(name);
    }

    public static void setDataFolder(Path path) { dataFolder = path; }

    public static Path getDataFolder() { return dataFolder; }
    public static void setRegistry(CorexRegistry r) { registry = r; }

    public static CorexRegistry getRegistry() { return registry; }

    public static Instruction[] compileScript(List<?> rawList) {
        Instruction[] bytecode = compileBlock(rawList);
        SlotAllocator.allocate(bytecode);
        return bytecode;
    }

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

    @SuppressWarnings("unchecked")
    private static Object restoreHashes(Object obj) {
        if (obj instanceof String str) {
            return str.indexOf(ScriptNormalizer.HASH_PLACEHOLDER) == -1
                    ? str
                    : str.replace(ScriptNormalizer.HASH_PLACEHOLDER, '#');
        } else if (obj instanceof List<?> list) {
            boolean changed = false;
            List<Object> newList = new ArrayList<>(list.size());
            for (Object item : list) {
                Object restored = restoreHashes(item);
                if (restored != item) changed = true;
                newList.add(restored);
            }
            return changed ? newList : list;
        } else if (obj instanceof Set<?> set) {
            boolean changed = false;
            Set<Object> newSet = new LinkedHashSet<>(set.size());
            for (Object item : set) {
                Object restored = restoreHashes(item);
                if (restored != item) changed = true;
                newSet.add(restored);
            }
            return changed ? newSet : set;
        } else if (obj instanceof Map<?, ?> map) {
            boolean changed = false;
            Map<Object, Object> newMap = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object key = entry.getKey();
                Object val = entry.getValue();
                Object newKey = restoreHashes(key);
                Object newVal = restoreHashes(val);
                if (newKey != key || newVal != val) changed = true;
                newMap.put(newKey, newVal);
            }
            return changed ? newMap : map;
        }
        return obj;
    }
}