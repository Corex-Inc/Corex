package dev.corexinc.corex.environment.commands.player;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.commands.ArgumentSchema;
import dev.corexinc.corex.api.commands.ArgumentSet;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.world.ItemTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/* @doc command
 *
 * @Name Give
 * @Syntax give [<player>|...] [<item>|...] (amount:<#>)
 * @RequiredArgs 2
 * @MaxArgs 3
 * @ShortDescription Gives items to one or more players.
 *
 * @Implements Give
 *
 * @Description
 * Puts the given items into each target player's inventory.
 * Whatever doesn't fit is dropped on the ground at the player's feet.
 *
 * Both arguments accept lists. Every item in the item list goes to every player
 * in the target list, so a single line can hand out a full kit to a whole team.
 *
 * Items resolve from ItemTags (including item containers) and from plain
 * material names. The amount: prefix sets the stack size per item and defaults to 1.
 *
 * @Usage
 * // Give the player a diamond.
 * - give <player> diamond
 *
 * @Usage
 * // Give a custom item from an item container.
 * - give <player> <item[mySword]>
 *
 * @Usage
 * // Give 64 arrows and a bow to every online player.
 * - give <server.onlinePlayers> bow|arrow amount:64
 */
public class GiveCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "give";
    }

    private static final ArgumentSchema SCHEMA = ArgumentSchema.of()
            .requireLinear(0, ListTag.class)
            .requireLinear(1, ListTag.class)
            .optionalPrefix("amount", ElementTag.class, "1")
            .build();

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        ArgumentSet args = SCHEMA.bind(instruction, queue);
        if (args == null) return;

        ListTag targetList = args.linear(0);
        ListTag itemListTag = args.linear(1);
        boolean failed = false;

        List<PlayerTag> targets = targetList.filter(PlayerTag.class, queue);
        if (targets.isEmpty()) {
            Debugger.echoError(queue, "No valid targets found!");
            failed = true;
        }

        List<ItemTag> items = new ArrayList<>(itemListTag.filter(ItemTag.class, queue));

        for (AbstractTag tag : itemListTag.filter(ElementTag.class, queue)) {
            ItemTag resolved = new ItemTag(tag.identify());
            if (resolved.getItemStack() != null) items.add(resolved);
        }

        if (items.isEmpty()) {
            Debugger.echoError(queue, "No valid items found!");
            failed = true;
        }

        ElementTag amountTag = args.prefix("amount");
        int quantity = Math.max(1, amountTag.asInt());

        Debugger.report(queue, instruction,
                "Amount", quantity,
                "Items", itemListTag.identify(),
                "Targets", targetList.identify()
        );
        if (failed) return;

        for (PlayerTag playerTag : targets) {
            Player player = playerTag.getPlayer();
            if (player == null || !player.isOnline()) continue;

            Location loc = player.getLocation();

            for (ItemTag itemTag : items) {
                ItemStack is = itemTag.getItemStack().clone();
                is.setAmount(quantity);

                Map<Integer, ItemStack> remaining = player.getInventory().addItem(is);

                for (ItemStack leftover : remaining.values()) {
                    player.getWorld().dropItemNaturally(loc, leftover);
                }
            }
        }
    }

    @Override
    public boolean isAsyncSafe() {
        return false;
    }

    @Override
    public @NonNull String getSyntax() {
        return "[<player>|...] [<item>|...] (amount:<#>)";
    }

    @Override
    public int getMinArgs() {
        return 2;
    }
    @Override
    public int getMaxArgs() {
        return 3;
    }
}