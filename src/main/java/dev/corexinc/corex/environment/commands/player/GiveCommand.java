package dev.corexinc.corex.environment.commands.player;

import dev.corexinc.corex.api.commands.AbstractCommand;
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

public class GiveCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "give";
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        AbstractTag targetsRaw = instruction.getLinearObject(0, queue);
        List<PlayerTag> targets = new ListTag(targetsRaw.identify()).filter(PlayerTag.class, queue);

        if (targets.isEmpty()) {
            Debugger.error(queue, "no valid targets found!", queue.getDepth());
            return;
        }

        AbstractTag itemsRaw = instruction.getLinearObject(1, queue);
        ListTag itemListTag = new ListTag(itemsRaw.identify());

        List<ItemTag> items = new ArrayList<>(itemListTag.filter(ItemTag.class, queue));

        for (AbstractTag tag : itemListTag.filter(ElementTag.class, queue)) {
            ItemTag resolved = new ItemTag(tag.identify());
            if (resolved.getItemStack() != null) items.add(resolved);
        }

        if (items.isEmpty()) {
            Debugger.error(queue, "no valid items found!", queue.getDepth());
            return;
        }

        int quantity = 1;
        AbstractTag qTag = instruction.getPrefixObject("amount", queue);
        if (qTag instanceof ElementTag el) {
            quantity = Math.max(1, el.asInt());
        }

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

    @Override public boolean isAsyncSafe() { return false; }

    @Override public @NonNull String getSyntax() {
        return "[<player>|...] [<item>|...] (amount:<#>)";
    }

    @Override public int getMinArgs() { return 2; }
    @Override public int getMaxArgs() { return 2; }
}