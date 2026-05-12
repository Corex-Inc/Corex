package dev.corexinc.corex.environment.events.implementation.player;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.SchedulerAdapter;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.events.EventReturn;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name PlayerChat
 *
 * @Events
 * player chats
 *
 * @Switches
 * contains:<text> - Matches if the message contains the specified text.
 *
 * @Cancellable
 *
 * @Description
 * Fires when a player types a message into the chat.
 * Note: The script execution is automatically synchronized with the main server thread, making it completely safe to use Bukkit API (like giving items or spawning entities) inside this event.
 *
 * @Context
 * <context.message> - returns an ElementTag of the chat message.
 * <context.originalMessage> - returns an ElementTag of the ORIGINAL player chat message.
 *
 * @Returns
 * message:<ElementTag> - Sets the chat message to the returned text.
 *
 * @Usage
 * // Greets the player if they say "hello".
 * on player chats:
 * - narrate "Hello to you too!"
 *
 * @Usage
 * // Prevents players from saying a bad word anywhere in their message.
 * on player chats contains:badword:
 * - return cancelled
 *
 * @Usage
 * // Appends a smiley face to any message.
 * on player chats:
 * - return message:<context.message>
 */
public class PlayerChatEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "PlayerChat";
    }

    @Override
    public @NotNull String getSyntax() {
        return "player chats";
    }

    @Override
    public void addScript(@NotNull EventData data) {
        scripts.add(data);
    }

    @Override
    public void initListener() {
        if (!isRegistered && !scripts.isEmpty()) {
            Bukkit.getPluginManager().registerEvents(this, Corex.getInstance());
            isRegistered = true;
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Component message = event.message();

        PlayerTag player = new PlayerTag(event.getPlayer());
        ContextTag context = null;

        for (EventData data : scripts) {
            if (!data.isGenericMatch("message", 0, String.valueOf(message))) {
                continue;
            }

            String containsSwitch = data.getSwitch("contains");
            if (containsSwitch != null) {
                if (!String.valueOf(message).toLowerCase().contains(containsSwitch.toLowerCase())) {
                    continue;
                }
            }

            if (context == null) {
                context = new ContextTag();
                context.put("message", new ElementTag(message));
                context.put("originalMessage", new ElementTag(event.originalMessage()));
            }

            final ContextTag finalContext = context;

            Runnable logic = () -> {
                ScriptQueue queue = EventRegistry.fire(data, player, finalContext);
                if (queue.isCancelled()) event.setCancelled(true);

                String messageStr = EventReturn.getPrefixed(queue.getReturns(), "message");
                if (messageStr != null) {
                    event.message(new ElementTag(messageStr).asComponent());
                }
            };

            if (event.isAsynchronous()) {
                try {
                    SchedulerAdapter.get().run(logic);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                logic.run();
            }
        }
    }

    @Override
    public void reset() {
        isRegistered = false;
        scripts.clear();
    }
}