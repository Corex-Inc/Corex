package dev.corexmc.corex.environment.commands.core;

import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.api.containers.AbstractContainer;
import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.engine.compiler.CompiledArgument;
import dev.corexmc.corex.engine.compiler.Instruction;
import dev.corexmc.corex.engine.queue.ScriptQueue;
import dev.corexmc.corex.engine.scripts.ScriptManager;
import dev.corexmc.corex.engine.tags.ObjectFetcher;
import dev.corexmc.corex.engine.utils.debugging.Debugger;
import dev.corexmc.corex.environment.tags.core.ElementTag;
import dev.corexmc.corex.environment.tags.core.ListTag;
import dev.corexmc.corex.environment.tags.core.MapTag;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

public class DoCommand implements AbstractCommand {

    @Override public @NonNull String getName() { return "do"; }
    @Override public @NonNull List<String> getAlias() { return List.of("run"); }

    @Override
    public void run(@NonNull ScriptQueue queue, Instruction instruction) {
        String target = instruction.getLinear(0, queue);
        if (target == null) return;

        // ИСПРАВЛЕНО: Безопасное разделение target на ИмяСкрипта и Путь
        String scriptName = target;
        String path = "script";

        int dotIndex = target.indexOf('.');
        if (dotIndex > 0) {
            scriptName = target.substring(0, dotIndex);
            path = target.substring(dotIndex + 1);
        }

        String prefixPath = instruction.getPrefix("path", queue);
        if (prefixPath != null) path = prefixPath;

        AbstractContainer container = ScriptManager.getContainer(scriptName);
        if (container == null) {
            Debugger.echoError("Container '" + scriptName + "' not found!");
            return;
        }

        Instruction[] bytecode = container.getScript(path);
        if (bytecode == null) {
            Debugger.echoError("Path '" + path + "' doesn't contain any commands in " + scriptName);
            return;
        }

        ScriptQueue newQueue = new ScriptQueue(scriptName + "_" + System.currentTimeMillis(), bytecode, false, queue.getPlayer());

        // 1. ВАРИАНТ def.<key>:<value>
        for (Map.Entry<String, CompiledArgument> entry : instruction.prefixArgs.entrySet()) {
            if (entry.getKey().startsWith("def.")) {
                String defName = entry.getKey().substring(4);
                String defValue = entry.getValue().evaluate(queue);
                newQueue.define(defName, ObjectFetcher.pickObject(defValue));
            }
        }

        // 2. ВАРИАНТ def:<map> ИЛИ def:<list>
        String defRaw = instruction.getPrefix("def", queue);
        if (defRaw != null) {
            AbstractTag defTag = ObjectFetcher.pickObject(defRaw);

            if (defTag instanceof MapTag) {
                // Если передали мапу: def:map[player=tizis0;score=10]
                MapTag map = (MapTag) defTag;
                for (String key : map.keySet()) {
                    newQueue.define(key, map.getObject(key));
                }
            } else {
                // Если передали список (или просто текст через пайпы): def:val1|val2|val3
                ListTag list = (defTag instanceof ListTag) ? (ListTag) defTag : new ListTag(defTag.identify());

                // Чтобы это работало, убедись, что в AbstractContainer есть метод getDefinitions()
                // и TaskContainer его переопределяет (как я писал в прошлом сообщении)
                List<String> keys = container.getDefinitions();

                if (keys != null) {
                    for (int i = 0; i < list.size() && i < keys.size(); i++) {
                        newQueue.define(keys.get(i), ObjectFetcher.pickObject(list.get(i)));
                    }
                }
            }
        }

        newQueue.start();
    }

    @Override public @NonNull String getSyntax() { return "- do [<script>] (def.<key>:<value>) (def:<map/list>) (path:<path>)"; }
    @Override public int getMinArgs() { return 1; }
    @Override public int getMaxArgs() { return 10; }
}