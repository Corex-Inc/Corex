package dev.corexinc.corex.environment.commands.player;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.scripts.ScriptManager;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.containers.DialogContainer;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.utils.BukkitSchedulerAdapter;
import dev.corexinc.corex.environment.utils.adapters.DialogAdapter;
import dev.corexinc.corex.environment.utils.dialog.DialogSpec;
import dev.corexinc.corex.environment.utils.nms.NMSHandler;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/* @doc command
 *
 * @Name Dialog
 * @Syntax dialog [open/close] (<dialog>) (targets:<player>|...) (def.<key>:<value>) (def:<map/list>)
 * @RequiredArgs 1
 * @MaxArgs 2
 * @ShortDescription Shows or closes a native dialog screen.
 *
 * @Implements Dialog
 *
 * @Description
 * Opens or closes a native client dialog screen (Paper Dialog API, requires 1.21.9+).
 *
 * Use "open" with a dialog script container to display it. Inputs declared in the container are
 * exposed to button scripts as <context.<key>>, and the pressed button index as <context.button>.
 *
 * When backed definitions exist, "def.<key>:<value>" (or "def:<map/list>") overrides them before the dialog is built.
 *
 * The "targets:" argument defaults to the linked player.
 *
 * @Usage
 * // Open a dialog for the player.
 * - dialog open myDialog
 *
 * @Usage
 * // Open a dialog for several players with a definition override.
 * - dialog open shop targets:<server.onlinePlayers> def.balance:<player.flag[coins]>
 *
 * @Usage
 * // Close the dialog for the player.
 * - dialog close
 */
public class DialogCommand implements AbstractCommand, DialogAdapter.Callback {

    @Override
    public @NonNull String getName() {
        return "dialog";
    }

    @Override
    public @NonNull String getSyntax() {
        return "[open/close] (<dialog>) (targets:<player>|...) (def.<key>:<value>) (def:<map/list>)";
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
        return false;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        String action = instruction.getLinear(0, queue);
        if (action == null) {
            Debugger.echoError(queue, "Dialog command requires an action (open/close).");
            return;
        }

        switch (action.toLowerCase()) {
            case "open" -> open(queue, instruction);
            case "close" -> close(queue, instruction);
            default -> Debugger.echoError(queue, "Dialog command unknown action '" + action + "'.");
        }
    }

    private void open(ScriptQueue queue, Instruction instruction) {
        List<Player> targets = resolveTargets(instruction.getPrefixObject("targets", queue), queue);
        if (targets.isEmpty()) {
            Debugger.echoError(queue, "Dialog command 'open' requires at least one online target.");
            return;
        }

        String dialogName = instruction.getLinear(1, queue);
        if (dialogName == null || !(ScriptManager.getContainer(dialogName) instanceof DialogContainer container)) {
            Debugger.echoError(queue, "Dialog command could not find a dialog container named '" + dialogName + "'.");
            return;
        }

        DialogAdapter adapter = NMSHandler.get().get(DialogAdapter.class);
        if (adapter == null) {
            Debugger.echoError(queue, "Dialogs require server version 1.21.7 or newer.");
            return;
        }

        MapTag overrides = collectOverrides(queue, instruction);
        DialogSpec spec = container.build(overrides, queue.getPlayer());

        Debugger.report(queue, instruction,
                "Action", "open",
                "Dialog", dialogName,
                "Targets", String.valueOf(targets.size()),
                "Definitions", overrides.identify()
        );

        for (Player target : targets) {
            SchedulerAdapter.get().runAt(BukkitSchedulerAdapter.toPosition(target.getLocation()),
                    () -> adapter.show(target, spec, this));
        }
    }

    private void close(ScriptQueue queue, Instruction instruction) {
        List<Player> targets = resolveTargets(instruction.getPrefixObject("targets", queue), queue);
        if (targets.isEmpty()) {
            Debugger.echoError(queue, "Dialog command 'close' requires at least one online target.");
            return;
        }

        DialogAdapter adapter = NMSHandler.get().get(DialogAdapter.class);
        if (adapter == null) return;

        Debugger.report(queue, instruction, "Action", "close", "Targets", String.valueOf(targets.size()));

        for (Player target : targets) {
            SchedulerAdapter.get().runAt(BukkitSchedulerAdapter.toPosition(target.getLocation()),
                    () -> adapter.close(target));
        }
    }

    @Override
    public void onButton(Player player, String dialogName, String buttonId, Map<String, String> responses) {
        if (!(ScriptManager.getContainer(dialogName) instanceof DialogContainer container)) return;

        Instruction[] script = container.getButtonScript(buttonId);
        if (script == null) return;

        ContextTag context = new ContextTag();
        responses.forEach((key, value) -> context.put(key, ObjectFetcher.pickObject(value)));
        context.put("button", new ElementTag(buttonId));

        ScriptQueue queue = new ScriptQueue(ScriptQueue.uniqueId("Dialog_" + dialogName), script, false, new PlayerTag(player));
        queue.setContext(context);
        queue.start();
    }

    private MapTag collectOverrides(ScriptQueue queue, Instruction instruction) {
        MapTag overrides = new MapTag();

        for (Map.Entry<String, CompiledArgument> entry : instruction.prefixArgs.entrySet()) {
            if (entry.getKey().startsWith("def.")) {
                overrides.putObject(entry.getKey().substring(4), entry.getValue().evaluate(queue));
            }
        }

        String defRaw = instruction.getPrefix("def", queue);
        if (defRaw != null) {
            AbstractTag defTag = ObjectFetcher.pickObject(defRaw);

            if (defTag instanceof MapTag map) {
                for (String key : map.keySet()) overrides.putObject(key, map.getObject(key));
            } else {
                ListTag list = (defTag instanceof ListTag) ? (ListTag) defTag : new ListTag(defTag.identify());
                List<String> keys = new ArrayList<>();
                String dialogName = instruction.getLinear(1, queue);
                if (dialogName != null && ScriptManager.getContainer(dialogName) instanceof DialogContainer container) {
                    keys = container.getDefinitions();
                }
                for (int i = 0; i < list.size() && i < keys.size(); i++) {
                    overrides.putObject(keys.get(i), ObjectFetcher.pickObject(list.get(i)));
                }
            }
        }

        return overrides;
    }

    private List<Player> resolveTargets(AbstractTag argument, ScriptQueue queue) {
        List<Player> players = new ArrayList<>();

        if (argument == null) {
            if (queue.getPlayer() instanceof PlayerTag playerTag && playerTag.getPlayer() != null) {
                players.add(playerTag.getPlayer());
            }
            return players;
        }

        ListTag list = argument instanceof ListTag listTag ? listTag : new ListTag(argument.identify());
        for (PlayerTag playerTag : list.filter(PlayerTag.class, queue)) {
            Player player = playerTag.getPlayer();
            if (player != null) players.add(player);
        }
        return players;
    }
}