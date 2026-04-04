package dev.corexinc.corex.environment.commands.core;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.containers.AbstractContainer;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.scripts.ScriptManager;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import org.jspecify.annotations.NonNull;


/* @[command]
 *
 * @Name Inject
 * @Syntax inject [<script>] (path:<name>)
 * @RequiredArgs 1
 * @MaxArgs 2
 * @ShortDescription Runs a script in the current queue.
 *
 * @Implements Inject
 *
 * @Description
 * Injects a script into the current queue.
 * This means this task will run with all the original queue's definitions and tags.
 * It will also now be part of the queue, so any delays or definitions used in the injected script will be accessible in the original queue.
 *
 * @Usage
 * // Injects the MyCustomTask task with path into the current queue
 * - inject MyCustomTask path:myAwesomePath
 */
public class InjectCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "inject";
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<script>] (path:<path>)";
    }

    @Override
    public int getMinArgs() {
        return 1;
    }

    @Override
    public int getMaxArgs() {
        return 2;
    }

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
            Debugger.error(queue, getName() + " container '" + scriptName + "' not found!", 0);
            return;
        }

        Instruction[] bytecode = container.getScript(path);
        if (bytecode == null) {
            Debugger.error(queue, getName() + " path '" + path + "' in script '" + scriptName + "' has no commands!", 0);
            return;
        }

        queue.pushFrame(bytecode, null);
    }
}