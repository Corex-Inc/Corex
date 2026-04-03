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
import dev.corexmc.corex.environment.tags.core.ListTag;
import dev.corexmc.corex.environment.tags.core.MapTag;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

/* @[command]
 *
 * @Name Do
 * @Syntax do [<script>] (path:<name>) (def:<element>|.../def.<name>:<value>) (id:<name>)
 * @RequiredArgs 1
 * @MaxArgs -1
 * @Aliases run
 * @ShortDescription Runs a script in a new queue.
 *
 * @Implements Run
 *
 * @Description
 * Runs a script in a new queue.
 *
 * You must specify a script object to run.
 *
 * Optionally, use the "path:" argument to choose a specific sub-path within a script.
 *
 * Optionally, use the "def:" argument to specify definition values to pass to the script,
 * the definitions will be named via the "definitions:" script key on the script being run.
 *
 * Alternately, use "def.<name>:<value>" to define one or more  named definitions individually.
 *
 * Optionally, specify the "id:" argument to choose a custom queue ID to be used.
 * If none is specified, a randomly generated one will be used. Generally, don't use this argument.
 *
 * @Tags
 * <save[saveName].created_queue> - returns the queue that was started by the run command.
 *
 * @Usage
 * // Use to do a task script named 'MyTask'.
 * - do MyTask
 *
 * @Usage
 * // Use to do a local subscript named 'alt_path'.
 * - do <script> path:alt_path
 *
 * @Usage
 * // Use to do 'MyTask' and pass 3 definitions to it.
 * - do MyTask def:A|Second_Def|Taco
 *
 * @Usage
 * // Use to do 'MyTask' and pass 3 named definitions to it.
 * - do MyTask def.count:5 def.type:Taco def.smell:Tasty
 */
public class DoCommand implements AbstractCommand {

    @Override public @NonNull String getName() { return "do"; }
    @Override public @NonNull List<String> getAlias() { return List.of("run"); }

    @Override
    public void run(@NonNull ScriptQueue queue, Instruction instruction) {
        String target = instruction.getLinear(0, queue);
        if (target == null) return;

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

        for (Map.Entry<String, CompiledArgument> entry : instruction.prefixArgs.entrySet()) {
            if (entry.getKey().startsWith("def.")) {
                String defName = entry.getKey().substring(4);
                String defValue = entry.getValue().evaluate(queue);
                newQueue.define(defName, ObjectFetcher.pickObject(defValue));
            }
        }

        String defRaw = instruction.getPrefix("def", queue);
        if (defRaw != null) {
            AbstractTag defTag = ObjectFetcher.pickObject(defRaw);

            if (defTag instanceof MapTag map) {
                for (String key : map.keySet()) {
                    newQueue.define(key, map.getObject(key));
                }
            } else {
                ListTag list = (defTag instanceof ListTag) ? (ListTag) defTag : new ListTag(defTag.identify());

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

    @Override public @NonNull String getSyntax() { return "[<script>] (def.<key>:<value>) (def:<map/list>) (path:<path>)"; }
    @Override public int getMinArgs() { return 1; }
    @Override public int getMaxArgs() { return -1; }
}