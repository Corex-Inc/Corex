package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.containers.AbstractContainer;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.scripts.ScriptManager;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import org.jspecify.annotations.NonNull;

import java.util.List;
import java.util.Map;

/* @doc command
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
 * // Use to do a local subscript named 'altPath'.
 * - do <script> path:altPath
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

    @Override
    public @NonNull String getName() {
        return "do";
    }

    @Override
    public @NonNull List<String> getAlias() {
        return List.of("run");
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<script>] (def.<key>:<value>) (def:<map/list>) (path:<path>) (id:<name>)";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return -1;
    }

    @Override
    public boolean isAsyncSafe() {
        return true;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
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
            Debugger.echoError(queue, "Container '" + scriptName + "' not found!");
            return;
        }

        Instruction[] bytecode = container.getScript(path);
        if (bytecode == null) {
            Debugger.echoError(queue, "Path '" + path + "' doesn't contain any commands in " + scriptName);
            return;
        }

        String queueId = instruction.getPrefix("id", queue);
        ScriptQueue newQueue = new ScriptQueue(queueId != null ? queueId : scriptName + "_" + System.currentTimeMillis(), bytecode, false, queue.getPlayer());

        MapTag defsMap = new MapTag();

        for (Map.Entry<String, CompiledArgument> entry : instruction.prefixArgs.entrySet()) {
            if (entry.getKey().startsWith("def.")) {
                String defName = entry.getKey().substring(4);
                AbstractTag defValue = entry.getValue().evaluate(queue);
                newQueue.define(defName, defValue);
                defsMap.putObject(defName, defValue);
            }
        }

        String defRaw = instruction.getPrefix("def", queue);
        if (defRaw != null) {
            AbstractTag defTag = ObjectFetcher.pickObject(defRaw);

            if (defTag instanceof MapTag map) {
                for (String key : map.keySet()) {
                    AbstractTag val = map.getObject(key);

                    newQueue.define(key, val);
                    defsMap.putObject(key, val);
                }
            } else {
                ListTag list = (defTag instanceof ListTag) ? (ListTag) defTag : new ListTag(defTag.identify());

                List<String> keys = container.getDefinitions();

                for (int i = 0; i < list.size() && i < keys.size(); i++) {
                    AbstractTag val = ObjectFetcher.pickObject(list.get(i));

                    newQueue.define(keys.get(i), val);
                    defsMap.putObject(keys.get(i), val);
                }
            }
        }

        if (instruction.isWaitable) {
            queue.pause();
            newQueue.setOnFinish(queue::resume);
        }

        Debugger.report(queue, instruction,
                "Script", scriptName,
                "Path", path,
                "Definitions", defsMap.identify(),
                "Queue", newQueue.getId()
        );
        newQueue.start();
    }
}