package dev.corexinc.corex.environment.events.implementation.player;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.environment.events.AbstractEvent;
import dev.corexinc.corex.environment.events.EventData;
import dev.corexinc.corex.environment.events.EventRegistry;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;
import java.util.ArrayList;
import java.util.List;

/* @doc event
 *
 * @Name PlayerInput
 *
 * @Events
 * player inputs
 *
 * @Switches
 * forward:<boolean> (Matches if the player is inputting forward movement)
 * backward:<boolean> (Matches if the player is inputting backward movement)
 * left:<boolean> (Matches if the player is inputting left movement)
 * right:<boolean> (Matches if the player is inputting right movement)
 * jump:<boolean> (Matches if the player is inputting jump)
 * sneak:<boolean> (Matches if the player is inputting sneak)
 *
 * @Description
 * Fires when a player sends updated movement or control inputs to the server.
 *
 * @Context
 * <context.forward> - returns an ElementTag(Boolean) indicating if the player is pressing the forward key.
 * <context.backward> - returns an ElementTag(Boolean) indicating if the player is pressing the backward key.
 * <context.left> - returns an ElementTag(Boolean) indicating if the player is pressing the left key.
 * <context.right> - returns an ElementTag(Boolean) indicating if the player is pressing the right key.
 * <context.jump> - returns an ElementTag(Boolean) indicating if the player is pressing the jump key.
 * <context.sneak> - returns an ElementTag(Boolean) indicating if the player is pressing the sneak key.
 *
 * @Usage
 * // Narrates when the player presses the jump key.
 * on player inputs jump:true:
 * - narrate "You pressed the jump key!"
 */
public class PlayerInputEvent implements AbstractEvent {

    private boolean isRegistered = false;
    private final List<EventData> scripts = new ArrayList<>();

    @Override
    public @NotNull String getName() {
        return "PlayerInput";
    }

    @Override
    public @NotNull String getSyntax() {
        return "player inputs";
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
    public void onInput(org.bukkit.event.player.PlayerInputEvent event) {
        PlayerTag player = new PlayerTag(event.getPlayer());
        ContextTag context = null;

        for (EventData data : scripts) {
            String forwardSwitch = data.getSwitch("forward");
            if (forwardSwitch != null && event.getInput().isForward() != Boolean.parseBoolean(forwardSwitch)) {
                continue;
            }

            String backwardSwitch = data.getSwitch("backward");
            if (backwardSwitch != null && event.getInput().isBackward() != Boolean.parseBoolean(backwardSwitch)) {
                continue;
            }

            String leftSwitch = data.getSwitch("left");
            if (leftSwitch != null && event.getInput().isLeft() != Boolean.parseBoolean(leftSwitch)) {
                continue;
            }

            String rightSwitch = data.getSwitch("right");
            if (rightSwitch != null && event.getInput().isRight() != Boolean.parseBoolean(rightSwitch)) {
                continue;
            }

            String jumpSwitch = data.getSwitch("jump");
            if (jumpSwitch != null && event.getInput().isJump() != Boolean.parseBoolean(jumpSwitch)) {
                continue;
            }

            String sneakSwitch = data.getSwitch("sneak");
            if (sneakSwitch != null && event.getInput().isSneak() != Boolean.parseBoolean(sneakSwitch)) {
                continue;
            }

            if (context == null) {
                context = new ContextTag();
                context.put("forward", new ElementTag(event.getInput().isForward()));
                context.put("backward", new ElementTag(event.getInput().isBackward()));
                context.put("left", new ElementTag(event.getInput().isLeft()));
                context.put("right", new ElementTag(event.getInput().isRight()));
                context.put("jump", new ElementTag(event.getInput().isJump()));
                context.put("sneak", new ElementTag(event.getInput().isSneak()));
            }

            EventRegistry.fire(data, player, context);
        }
    }

    @Override
    public void reset() {
        isRegistered = false;
        scripts.clear();
    }
}