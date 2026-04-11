package dev.corexinc.corex.engine.scripts;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.containers.AbstractContainer;
import dev.corexinc.corex.api.containers.PathType;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.compiler.ScriptCompiler;
import dev.corexinc.corex.engine.utils.CorexLogger;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptManager {

    private static final Map<String, AbstractContainer> containers = new HashMap<>();
    public static long lastReloadTime = System.currentTimeMillis();

    public static void loadScripts(File scriptsFolder) {
        containers.clear();
        if (!scriptsFolder.exists()) scriptsFolder.mkdirs();

        List<File> files = new ArrayList<>();
        findScriptsRecursively(scriptsFolder, files);

        int loadedCount = 0;

        for (File file : files) {
            try {
                List<String> rawLines = Files.readAllLines(file.toPath());
                String cleanYamlString = ScriptPreprocessor.preprocess(rawLines);

                YamlConfiguration yaml = new YamlConfiguration();
                yaml.loadFromString(cleanYamlString);

                for (String scriptName : yaml.getKeys(false)) {
                    String type = yaml.getString(scriptName + ".type");
                    if (type == null) continue;

                    Class<? extends AbstractContainer> clazz = Corex.getInstance().getRegistry().getContainerClass(type);
                    if (clazz == null) {
                        CorexLogger.warn("Script " + scriptName + " is using unknown type: " + type);
                        continue;
                    }

                    AbstractContainer container = clazz.getDeclaredConstructor().newInstance();
                    org.bukkit.configuration.ConfigurationSection section = yaml.getConfigurationSection(scriptName);
                    container.init(scriptName, section);

                    assert section != null;
                    for (String path : section.getKeys(true)) {

                        PathType pathType = container.resolvePath(path);

                        if (pathType == PathType.SCRIPT) {
                            List<?> rawCommands = section.getList(path);

                            if (rawCommands != null) {
                                Instruction[] bytecode = compileBlock(rawCommands);
                                container.addCompiledScript(path, bytecode);
                            }
                        }
                    }

                    containers.put(scriptName.toLowerCase(), container);
                    loadedCount++;
                }
            } catch (Exception e) {
                CorexLogger.error("ERROR while reloading script " + file.getName() + ": " + e.getMessage());
            }
        }
        CorexLogger.success("Loaded <aqua>" + loadedCount + "</aqua> containers!");
    }

    private static void findScriptsRecursively(File folder, List<File> list) {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) findScriptsRecursively(file, list);
            else if (file.getName().endsWith(".cx")) list.add(file);
        }
    }

    public static void reloadScripts() {
        File scriptsFolder = new File(Corex.getInstance().getDataFolder(), "scripts");
        lastReloadTime = System.currentTimeMillis();
        loadScripts(scriptsFolder);
    }

    public static AbstractContainer getContainer(String name) {
        return containers.get(name.toLowerCase());
    }

    public static Instruction[] compileBlock(List<?> rawList) {
        List<Instruction> bytecode = new ArrayList<>();

        for (Object obj : rawList) {
            if (obj instanceof String) {
                Instruction inst = ScriptCompiler.compile((String) obj);
                if (inst != null) bytecode.add(inst);
            }
            else if (obj instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    String cmdLine = entry.getKey().toString().replace(":", "").trim();

                    if (entry.getValue() instanceof List) {
                        Instruction[] inner = compileBlock((List<?>) entry.getValue());
                        Instruction inst = ScriptCompiler.compile(cmdLine, inner);
                        if (inst != null) bytecode.add(inst);
                    }
                }
            }
        }
        return bytecode.toArray(new Instruction[0]);
    }
}