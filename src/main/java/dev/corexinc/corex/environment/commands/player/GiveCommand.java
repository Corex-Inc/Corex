package dev.corexinc.corex.environment.commands.player;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.containers.ItemContainer;
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

    @Override public @NonNull String getName() {
        return "give";
    }

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {

        AbstractTag targetsObj = instruction.getLinearObject(0, queue);
        AbstractTag itemsObj = instruction.getLinearObject(1, queue);

        if (targetsObj == null || itemsObj == null) {
            Debugger.error(queue, "ERROR: no enough arguments!", queue.getDepth());
            return;
        }

        List<PlayerTag> targets = new ArrayList<>();
        if (targetsObj instanceof PlayerTag pt) {
            targets.add(pt);
        } else if (targetsObj instanceof ListTag lt) {
            targets.addAll(lt.filter(PlayerTag.class));
        } else {
            targets.addAll(new ListTag(targetsObj.identify()).filter(PlayerTag.class));
        }

        List<ItemTag> items = new ArrayList<>();
        switch (itemsObj) {
            case ItemTag it -> items.add(it);
            case ListTag lt -> items.addAll(lt.filter(ItemTag.class));
            case ElementTag el -> {
                ItemTag cached = ItemContainer.ItemCache.get(el.identify());
                if (cached != null) {
                    items.add(cached);
                } else {
                    items.addAll(new ListTag(el.identify()).filter(ItemTag.class));
                }
            }
            default -> {
            }
        }

        if (targets.isEmpty()) {
            Debugger.error(queue, "ERROR: targets not found!", queue.getDepth());
            return;
        }
        if (items.isEmpty()) {
            Debugger.error(queue, "ERROR: items not found!", queue.getDepth());
            return;
        }

        int quantity = 1;
        AbstractTag qTag = instruction.getPrefixObject("quantity", queue);
        if (qTag instanceof ElementTag el) {
            quantity = el.asInt();
        }

        for (PlayerTag playerTag : targets) {
            Player player = playerTag.getPlayer();
            if (player == null || !player.isOnline()) continue;

            Location loc = player.getLocation();

            for (ItemTag itemTag : items) {
                ItemStack is = itemTag.getItemStack().clone();
                is.setAmount(quantity);

                Map<Integer, ItemStack> remaining = player.getInventory().addItem(is);

                if (!remaining.isEmpty()) {
                    for (ItemStack leftover : remaining.values()) {
                        player.getWorld().dropItemNaturally(loc, leftover);
                    }
                }
            }
        }
    }

    @Override public boolean isAsyncSafe() {
        return false;
    }

    @Override public @NonNull String getSyntax() {
        return "[<player>|...] [<item>|...] (quantity:<#>)";
    }

    @Override public int getMinArgs() {
        return 2;
    }
    @Override public int getMaxArgs() {
        return 2;
    }
}