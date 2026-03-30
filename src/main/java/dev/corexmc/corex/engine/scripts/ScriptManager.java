package dev.corexmc.corex.engine.scripts;

import dev.corexmc.corex.engine.compiler.Instruction;
import dev.corexmc.corex.engine.compiler.ScriptCompiler;
import dev.corexmc.corex.engine.utils.CorexLogger;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptManager {

    private final Map<String, TaskScript> taskScripts = new HashMap<>();

    public void loadScripts(File scriptsFolder) {
        taskScripts.clear();
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

                    if ("task".equalsIgnoreCase(type)) {
                        List<String> rawCommands = yaml.getStringList(scriptName + ".script");

                        List<Instruction> bytecodeList = new ArrayList<>();
                        for (String cmdLine : rawCommands) {
                            Instruction inst = ScriptCompiler.compile(cmdLine);
                            if (inst != null) bytecodeList.add(inst);
                        }

                        TaskScript task = new TaskScript(scriptName, bytecodeList.toArray(new Instruction[0]));
                        taskScripts.put(scriptName.toLowerCase(), task);
                        loadedCount++;
                    }
                }
            } catch (Exception e) {
                CorexLogger.error("ERROR while reloading " + file.getName() + ": " + e.getMessage());
            }
        }
        CorexLogger.success("Reloaded <aqua>" + loadedCount + "</aqua> scripts!");
    }

    private void findScriptsRecursively(File folder, List<File> list) {
        File[] files = folder.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                findScriptsRecursively(file, list);
            } else if (file.getName().endsWith(".crx")) {
                list.add(file);
            }
        }
    }

    public TaskScript getTaskScript(String name) {
        return taskScripts.get(name.toLowerCase());
    }

    public void reloadScripts() {
        taskScripts.clear();

        File scriptsFolder = new File(dev.corexmc.corex.Corex.getInstance().getDataFolder(), "scripts");
        loadScripts(scriptsFolder);
    }

}