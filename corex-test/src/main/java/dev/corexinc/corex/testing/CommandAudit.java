package dev.corexinc.corex.testing;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.engine.CorexRegistry;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.registry.BoundCommand;
import dev.corexinc.corex.engine.registry.CommandMetadata;
import dev.corexinc.corex.engine.registry.SyntaxSlot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Checks that commands are declared correctly, so mistakes surface when the build runs
 * rather than the first time a script happens to use them.
 *
 * <p>Corex validates the same things when the plugin loads, but there it can only write to
 * the console - a server owner sees a red line and a command that quietly does nothing.
 * Running this from a test turns the same problems into a failed build.
 *
 * <pre>{@code
 * @Test
 * void commandsAreWellFormed() {
 *     CorexRegistry registry = CorexTestEnvironment.bootstrap();
 *     registry.register(TeleportCommand.class, HealCommand.class);
 *
 *     CommandAudit.ofRegistry(registry).assertValid();
 * }
 * }</pre>
 *
 * <p>That sweeps Corex's own commands too. To check only your own, narrow by package:
 *
 * <pre>{@code
 * CommandAudit.ofRegistry(registry).fromPackage("com.example.myaddon").assertValid();
 * }</pre>
 *
 * <p>{@link #of(AbstractCommand...)} takes explicit instances, for the rarer case of
 * checking a command that is not registered anywhere.
 *
 * <p>Note this is a build-time check, not a compile-time one: the binding is resolved by
 * reflection, so nothing but running the check can prove a signature lines up.
 */
public final class CommandAudit {

    private final List<AbstractCommand> commands;

    private CommandAudit(List<AbstractCommand> commands) {
        this.commands = commands;
    }

    /**
     * Audits every command the registry knows about, Corex's own included.
     * <p>
     * This is the usual entry point: register your commands as you normally would and the
     * audit picks them up, so adding a command never means remembering to add it here too.
     */
    public static CommandAudit ofRegistry(CorexRegistry registry) {
        return new CommandAudit(new ArrayList<>(registry.getRegisteredCommands()));
    }

    /**
     * Audits explicitly given commands, for ones that are not registered anywhere.
     */
    public static CommandAudit of(AbstractCommand... commands) {
        return new CommandAudit(Arrays.asList(commands));
    }

    /**
     * Narrows the audit to commands whose class sits in {@code packagePrefix} or below,
     * so an addon can check its own without Corex's results mixed in.
     */
    public CommandAudit fromPackage(String packagePrefix) {
        List<AbstractCommand> filtered = new ArrayList<>();
        for (AbstractCommand command : commands) {
            if (command.getClass().getName().startsWith(packagePrefix)) filtered.add(command);
        }
        return new CommandAudit(filtered);
    }

    /**
     * Returns every problem found, keyed by command name. Empty means all is well.
     */
    public Map<String, List<String>> findProblems() {
        Map<String, List<String>> problems = new LinkedHashMap<>();
        Map<String, String> takenNames = new LinkedHashMap<>();

        for (AbstractCommand command : commands) {
            List<String> found = new ArrayList<>();
            String where = command.getClass().getSimpleName();

            inspect(command, found);
            checkNames(command, where, takenNames, found);

            if (!found.isEmpty()) problems.put(command.getName(), found);
        }
        return problems;
    }

    /**
     * Fails the test with every problem listed, or returns quietly when there are none.
     */
    public void assertValid() {
        Map<String, List<String>> problems = findProblems();
        if (problems.isEmpty()) return;

        StringBuilder message = new StringBuilder("Command declaration problems:\n");
        for (Map.Entry<String, List<String>> entry : problems.entrySet()) {
            message.append("\n  ").append(entry.getKey()).append(':');
            for (String problem : entry.getValue()) {
                message.append("\n    - ").append(problem);
            }
        }
        throw new AssertionError(message.toString());
    }

    private void inspect(AbstractCommand command, List<String> found) {
        List<SyntaxSlot> slots;
        try {
            slots = new CommandMetadata(command).getSlots();
        } catch (Exception e) {
            found.add("syntax '" + command.getSyntax() + "' could not be parsed: " + e);
            return;
        }

        BoundCommand bound = BoundCommand.bind(command, slots, found::add);

        if (bound == null && !hasClassicRun(command)) {
            found.add("implements neither form of run(...) - override run(ScriptQueue, Instruction), "
                    + "or declare run(...) with one parameter per syntax argument");
        }

        checkArgCounts(command, slots, found);
    }

    /**
     * getMinArgs and getMaxArgs gate the compiler before the syntax is even consulted, so a
     * command whose bounds contradict its own syntax rejects scripts that look correct.
     */
    private void checkArgCounts(AbstractCommand command, List<SyntaxSlot> slots, List<String> found) {
        int required = 0;
        int total = slots.size();
        for (SyntaxSlot slot : slots) if (slot.required()) required++;

        int max = command.getMaxArgs();
        if (max != -1 && max < required) {
            found.add("getMaxArgs() is " + max + " but the syntax requires " + required
                    + " argument(s) - scripts using it would be rejected as too long");
        }
        if (command.getMinArgs() > total && total > 0) {
            found.add("getMinArgs() is " + command.getMinArgs() + " but the syntax only declares "
                    + total + " argument(s)");
        }
    }

    private void checkNames(AbstractCommand command, String where,
                            Map<String, String> taken, List<String> found) {
        if (command.getName().isBlank()) {
            found.add("has a blank name");
            return;
        }
        register(command.getName(), where, taken, found);
        for (String alias : command.getAlias()) {
            if (alias.isBlank()) found.add("declares a blank alias");
            else register(alias, where, taken, found);
        }
    }

    private void register(String name, String where, Map<String, String> taken, List<String> found) {
        String previous = taken.putIfAbsent(name.toLowerCase(), where);
        if (previous != null && !previous.equals(where)) {
            found.add("name or alias '" + name + "' is already used by " + previous
                    + " - the later registration silently wins");
        }
    }

    private static boolean hasClassicRun(AbstractCommand command) {
        try {
            return !command.getClass()
                    .getMethod("run", ScriptQueue.class, Instruction.class)
                    .isDefault();
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
