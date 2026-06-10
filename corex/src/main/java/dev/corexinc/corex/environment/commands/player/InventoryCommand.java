package dev.corexinc.corex.environment.commands.player;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.data.actions.AbstractDataAction;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Flaggable;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.flags.trackers.AbstractFlagTracker;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.scripts.ScriptManager;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.DurationTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.tags.inventory.InventoryTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.ItemTag;
import dev.corexinc.corex.environment.utils.BukkitSchedulerAdapter;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* @doc command
 *
 * @Name Inventory
 * @Syntax inventory [open/close/set/add/remove/clear/adjust/flag] (<mechanism>:<value>/<map> | <key>:<value>) (inventory:<inventory>) (slot:<#>) (items:<item>|...) (expire:<duration>) (targets:<player>|...) (def.<key>:<value>) (def:<map>)
 * @RequiredArgs 1
 * @MaxArgs 2
 * @ShortDescription Opens, closes, or edits inventories.
 *
 * @Implements Inventory
 *
 * @Description
 * Manages inventories for players and live containers.
 *
 * Actions:
 * - "open" shows an inventory to the targets. The inventory may be a generic inline inventory or a
 *   reference to an inventory script container. When backed by a script, "def.<key>:<value>" (or
 *   "def:<map>") overrides the container's definitions before its slots and procedural items are built.
 * - "close" closes whatever inventory the targets currently have open.
 * Slots are 1-based: "slot:1" is the first slot.
 *
 * - "set" writes the given "items:" into a live inventory starting at "slot:" (default 1).
 * - "add" inserts the given "items:" into the first available slots of a live inventory.
 * - "remove" removes the given "items:" from a live inventory.
 * - "clear" empties a live inventory, or a single "slot:" when specified.
 * - "adjust" applies a "<mechanism>:<value>" (or a MapTag of mechanisms) to the item in "slot:" and writes it back.
 * - "flag" sets a flag "<key>:<value>" on the item in "slot:" (with optional "expire:").
 *   A data action may be used instead of a plain value (e.g. "uses:++", "scores:|+:5", "tag:!" to delete).
 *
 * The "targets:" argument (open/close) defaults to the linked player.
 * The "inventory:" argument (set/add/remove/clear) defaults to the linked player's live inventory.
 * Editing actions require a live inventory, such as <player.inventory>.
 *
 * @Usage
 * // Open a generic locked menu for the player.
 * - inventory open inventory:<inventory[gui=true;inventoryType=GENERIC_3X3;title=Hello;content=<list[stone|apple]>]>
 *
 * @Usage
 * // Open a script inventory and override its 'key' definition.
 * - inventory open inventory:myInv targets:<server.online_players> def.key:<item[diamond]>
 *
 * @Usage
 * // Put a diamond into the player's 9th slot.
 * - inventory set slot:9 items:<item[diamond]>
 *
 * @Usage
 * // Add several items to a player's inventory.
 * - inventory add items:<item[bread]>|<item[apple]> targets:<player>
 *
 * @Usage
 * // Wipe the player's inventory.
 * - inventory clear
 *
 * @Usage
 * // Rename the item in the first slot.
 * - inventory adjust slot:1 displayName:Legendary
 *
 * @Usage
 * // Adjust a MapTag of mechanisms on the item in the first slot.
 * - inventory adjust slot:1 <map[displayName=Legendary;amount=1]>
 *
 * @Usage
 * // Flag the item in the first slot as soulbound to the player.
 * - inventory flag slot:1 soulbound:<player.name>
 *
 * @Usage
 * // Increment a numeric flag on the item in slot 5.
 * - inventory flag slot:5 uses:++
 *
 * @Usage
 * // Remove a flag from the item in slot 5.
 * - inventory flag slot:5 soulbound:!
 */
public class InventoryCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "inventory";
    }

    @Override
    public @NonNull String getSyntax() {
        return "[open/close/set/add/remove/clear/adjust/flag] (inventory:<inventory>) (slot:<#>) (items:<item>|...) (targets:<player>|...) (def.<key>:<value>) (def:<map>)";
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
    public boolean isAsyncSafe() {
        return false;
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        String action = instruction.getLinear(0, queue);
        if (action == null) {
            Debugger.echoError(queue, "Inventory command requires an action (open/close/set/add/remove/clear).");
            return;
        }

        switch (action.toLowerCase()) {
            case "open" -> open(queue, instruction);
            case "close" -> close(queue, instruction);
            case "set" -> set(queue, instruction);
            case "add" -> add(queue, instruction);
            case "remove" -> remove(queue, instruction);
            case "clear" -> clear(queue, instruction);
            case "adjust" -> adjust(queue, instruction);
            case "flag" -> flag(queue, instruction);
            default -> Debugger.echoError(queue, "Inventory command unknown action '" + action + "'.");
        }
    }

    private void open(ScriptQueue queue, Instruction instruction) {
        List<Player> targets = resolveTargets(instruction.getPrefixObject("targets", queue), queue);
        if (targets.isEmpty()) {
            Debugger.echoError(queue, "Inventory command 'open' requires at least one online target.");
            return;
        }

        AbstractTag inventoryArg = instruction.getPrefixObject("inventory", queue);
        if (inventoryArg == null) {
            Debugger.echoError(queue, "Inventory command 'open' requires the inventory: argument.");
            return;
        }

        InventoryTag inventory = asInventory(inventoryArg);

        MapTag overrides = collectOverrides(queue, instruction);
        if (!overrides.isEmpty() && inventory.isScript()) {
            inventory = inventory.rebuild(overrides, queue.getPlayer());
        }

        Debugger.report(queue, instruction,
                "Action", "open",
                "Inventory", inventory.identify(),
                "Targets", String.valueOf(targets.size()),
                "Definitions", overrides.identify()
        );

        for (Player target : targets) inventory.open(target);
    }

    private void close(ScriptQueue queue, Instruction instruction) {
        List<Player> targets = resolveTargets(instruction.getPrefixObject("targets", queue), queue);
        if (targets.isEmpty()) {
            Debugger.echoError(queue, "Inventory command 'close' requires at least one online target.");
            return;
        }

        Debugger.report(queue, instruction, "Action", "close", "Targets", String.valueOf(targets.size()));

        for (Player target : targets) {
            SchedulerAdapter.get().runAt(BukkitSchedulerAdapter.toPosition(target.getLocation()), target::closeInventory);
        }
    }

    private void set(ScriptQueue queue, Instruction instruction) {
        InventoryTag inventory = resolveLiveInventory(queue, instruction);
        if (inventory == null) return;

        List<ItemTag> items = collectItems(queue, instruction);
        if (items.isEmpty()) {
            Debugger.echoError(queue, "Inventory command 'set' requires items:.");
            return;
        }

        int startIndex = slotIndex(queue, instruction, 0);
        Inventory bukkit = inventory.getLiveInventory();

        Debugger.report(queue, instruction,
                "Action", "set",
                "Inventory", inventory.identify(),
                "Slot", String.valueOf(startIndex + 1),
                "Items", String.valueOf(items.size())
        );

        edit(inventory, () -> {
            int size = bukkit.getSize();
            int index = startIndex;
            for (ItemTag item : items) {
                if (index >= 0 && index < size) bukkit.setItem(index, item.getItemStack());
                index++;
            }
        });
    }

    private void add(ScriptQueue queue, Instruction instruction) {
        InventoryTag inventory = resolveLiveInventory(queue, instruction);
        if (inventory == null) return;

        List<ItemTag> items = collectItems(queue, instruction);
        if (items.isEmpty()) {
            Debugger.echoError(queue, "Inventory command 'add' requires items:.");
            return;
        }

        Inventory bukkit = inventory.getLiveInventory();
        ItemStack[] stacks = items.stream().map(ItemTag::getItemStack).toArray(ItemStack[]::new);

        Debugger.report(queue, instruction,
                "Action", "add", "Inventory", inventory.identify(), "Items", String.valueOf(items.size()));

        edit(inventory, () -> bukkit.addItem(stacks));
    }

    private void remove(ScriptQueue queue, Instruction instruction) {
        InventoryTag inventory = resolveLiveInventory(queue, instruction);
        if (inventory == null) return;

        List<ItemTag> items = collectItems(queue, instruction);
        if (items.isEmpty()) {
            Debugger.echoError(queue, "Inventory command 'remove' requires items:.");
            return;
        }

        Inventory bukkit = inventory.getLiveInventory();
        ItemStack[] stacks = items.stream().map(ItemTag::getItemStack).toArray(ItemStack[]::new);

        Debugger.report(queue, instruction,
                "Action", "remove", "Inventory", inventory.identify(), "Items", String.valueOf(items.size()));

        edit(inventory, () -> bukkit.removeItem(stacks));
    }

    private void clear(ScriptQueue queue, Instruction instruction) {
        InventoryTag inventory = resolveLiveInventory(queue, instruction);
        if (inventory == null) return;

        String rawSlot = instruction.getPrefix("slot", queue);
        Integer index = rawSlot != null ? new ElementTag(rawSlot).asInt() - 1 : null;
        Inventory bukkit = inventory.getLiveInventory();

        Debugger.report(queue, instruction,
                "Action", "clear", "Inventory", inventory.identify(), "Slot", index == null ? "all" : String.valueOf(index + 1));

        edit(inventory, () -> {
            if (index == null) {
                bukkit.clear();
            } else if (index >= 0 && index < bukkit.getSize()) {
                bukkit.setItem(index, null);
            }
        });
    }

    private void adjust(ScriptQueue queue, Instruction instruction) {
        InventoryTag inventory = resolveLiveInventory(queue, instruction);
        if (inventory == null) return;

        AbstractTag mechInput = instruction.getLinearObject(1, queue);
        if (mechInput == null) {
            Debugger.echoError(queue, "Inventory command 'adjust' requires a <mechanism>:<value> or a MapTag.");
            return;
        }

        Map<String, AbstractTag> mechanisms = new LinkedHashMap<>();
        if (mechInput instanceof MapTag map) {
            for (String key : map.keySet()) mechanisms.put(key, map.getObject(key));
        } else {
            String raw = mechInput.identify();
            int colonIndex = raw.indexOf(':');
            if (colonIndex > 0) {
                mechanisms.put(raw.substring(0, colonIndex), ObjectFetcher.pickObject(raw.substring(colonIndex + 1)));
            } else {
                mechanisms.put(raw, new ElementTag(""));
            }
        }

        int index = slotIndex(queue, instruction, 0);
        Inventory bukkit = inventory.getLiveInventory();

        Debugger.report(queue, instruction,
                "Action", "adjust",
                "Inventory", inventory.identify(),
                "Slot", String.valueOf(index + 1),
                "Mechanisms", mechInput.identify()
        );

        edit(inventory, () -> {
            ItemTag item = itemAt(bukkit, index, "adjust");
            if (item == null) return;

            for (Map.Entry<String, AbstractTag> mechanism : mechanisms.entrySet()) {
                item = (ItemTag) item.applyMechanism(mechanism.getKey(), mechanism.getValue());
            }
            bukkit.setItem(index, item.getItemStack());
        });
    }

    private void flag(ScriptQueue queue, Instruction instruction) {
        InventoryTag inventory = resolveLiveInventory(queue, instruction);
        if (inventory == null) return;

        AbstractTag flagInput = instruction.getLinearObject(1, queue);
        if (flagInput == null) {
            Debugger.echoError(queue, "Inventory command 'flag' requires a <key>:<value>.");
            return;
        }

        String raw = flagInput.identify();
        int colonIndex = findColonOutsideBrackets(raw);
        if (colonIndex < 0) {
            Debugger.echoError(queue, "Inventory command 'flag' expects <key>:<value> or <key>:<action>.");
            return;
        }

        String keyPath = raw.substring(0, colonIndex);
        String actionStr = raw.substring(colonIndex + 1);
        AbstractDataAction action = ScriptManager.getRegistry().findAction(actionStr);
        if (action == null) {
            Debugger.echoError(queue, "Inventory command 'flag' has an unknown action: " + actionStr);
            return;
        }
        String param = action.extractParam(actionStr);

        int index = slotIndex(queue, instruction, 0);
        long durationMs = instruction.getPrefixObject("expire", queue) instanceof DurationTag duration
                ? duration.getMilliseconds() : 0;
        Inventory bukkit = inventory.getLiveInventory();

        Debugger.report(queue, instruction,
                "Action", "flag",
                "Inventory", inventory.identify(),
                "Slot", String.valueOf(index + 1),
                "Flag", keyPath,
                "Data", ":" + action.getSymbol() + " " + param
        );

        edit(inventory, () -> {
            ItemTag item = itemAt(bukkit, index, "flag");
            if (item == null) return;

            AbstractFlagTracker tracker = ((Flaggable) item).getFlagTracker();
            if (tracker == null) return;

            AbstractTag current = tracker.getFlag(keyPath);
            AbstractTag result = action.apply(current, param, null, queue);
            tracker.setFlag(keyPath, result, durationMs);
            bukkit.setItem(index, item.getItemStack());
        });
    }

    private static int findColonOutsideBrackets(String value) {
        int depth = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') depth--;
            else if (c == ':' && depth == 0) return i;
        }
        return -1;
    }

    private ItemTag itemAt(Inventory bukkit, int index, String action) {
        if (index < 0 || index >= bukkit.getSize()) {
            CorexLogger.error("Inventory '" + action + "' slot " + (index + 1) + " is out of range (1-" + bukkit.getSize() + ").");
            return null;
        }
        ItemStack stack = bukkit.getItem(index);
        if (stack == null || stack.getType().isAir()) {
            CorexLogger.error("Inventory '" + action + "' found no item in slot " + (index + 1) + ".");
            return null;
        }
        return new ItemTag(stack);
    }

    private void edit(InventoryTag inventory, Runnable task) {
        Location location = inventory.getHolderLocation();
        if (location != null) {
            SchedulerAdapter.get().runAt(BukkitSchedulerAdapter.toPosition(location), task);
        } else {
            SchedulerAdapter.get().run(task);
        }
    }

    private InventoryTag resolveLiveInventory(ScriptQueue queue, Instruction instruction) {
        AbstractTag inventoryArg = instruction.getPrefixObject("inventory", queue);

        InventoryTag inventory;
        if (inventoryArg != null) {
            inventory = asInventory(inventoryArg);
        } else if (queue.getPlayer() instanceof PlayerTag playerTag && playerTag.getPlayer() != null) {
            inventory = new InventoryTag(playerTag.getPlayer().getInventory());
        } else {
            Debugger.echoError(queue, "Inventory command requires an inventory: argument or a linked player.");
            return null;
        }

        if (!inventory.isLive()) {
            Debugger.echoError(queue, "Inventory editing requires a live inventory, such as <player.inventory>.");
            return null;
        }
        return inventory;
    }

    private InventoryTag asInventory(AbstractTag argument) {
        return argument instanceof InventoryTag tag ? tag : new InventoryTag(argument.identify());
    }

    private List<ItemTag> collectItems(ScriptQueue queue, Instruction instruction) {
        AbstractTag itemsArg = instruction.getPrefixObject("items", queue);
        if (itemsArg == null) return List.of();

        ListTag list = itemsArg instanceof ListTag listTag ? listTag : new ListTag(itemsArg.identify());
        List<ItemTag> result = new ArrayList<>();
        for (AbstractTag entry : list.getList()) {
            ItemTag item = entry instanceof ItemTag itemTag ? itemTag : new ItemTag(entry.identify());
            if (item.getItemStack() != null) {
                result.add(item);
            } else {
                Debugger.echoError(queue, "Inventory command could not resolve item '" + entry.identify() + "'.");
            }
        }
        return result;
    }

    private int slotIndex(ScriptQueue queue, Instruction instruction, int fallbackIndex) {
        String raw = instruction.getPrefix("slot", queue);
        return raw != null ? new ElementTag(raw).asInt() - 1 : fallbackIndex;
    }

    private MapTag collectOverrides(ScriptQueue queue, Instruction instruction) {
        MapTag overrides = new MapTag();

        for (Map.Entry<String, CompiledArgument> entry : instruction.prefixArgs.entrySet()) {
            if (entry.getKey().startsWith("def.")) {
                overrides.putObject(entry.getKey().substring(4), entry.getValue().evaluate(queue));
            }
        }

        String defRaw = instruction.getPrefix("def", queue);
        if (defRaw != null && ObjectFetcher.pickObject(defRaw) instanceof MapTag map) {
            for (String key : map.keySet()) overrides.putObject(key, map.getObject(key));
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
