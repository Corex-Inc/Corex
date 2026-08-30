package dev.corexinc.corex.velocity.environment.commands.player;

import com.velocitypowered.api.proxy.Player;
import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.commands.ArgumentSchema;
import dev.corexinc.corex.api.commands.ArgumentSet;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.Instruction;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.DurationTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.velocity.environment.tags.player.PlayerTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.jspecify.annotations.NonNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/* @doc command
 *
 * @Name Title
 * @Syntax title (<text>) (title:<text>) (subtitle:<text>) (fadeIn:<duration>) (stay:<duration>) (fadeOut:<duration>) (targets:<player>|...)
 * @RequiredArgs 0
 * @MaxArgs 6
 * @Modules VELOCITY
 * @ShortDescription Displays a title message to specified players.
 *
 * @Implements Title
 *
 * @Description
 * Shows a large message in the center of the player's screen.
 * An optional subtitle can be displayed below the main title.
 *
 * You can customize how long the title fades in, stays visible, and fades out:
 * - fadeIn — duration of the fade-in animation
 * - stay — how long the title remains on screen
 * - fadeOut — duration of the fade-out animation
 *
 * Default timings are: 1 second fade-in, 3 seconds stay, and 1 second fade-out.
 *
 * Sent from the proxy it survives a backend being down, but a backend sending its own title
 * replaces this one.
 *
 * Note: If you need different text for each player,
 * iterate over them using the `foreach` command and send titles individually.
 *
 * @Usage
 * // Notify players about an upcoming server restart
 * - title "<&c>Server Restarting" subtitle:"<&c>In 1 minute!" stay:1m targets:<proxy.players>
 *
 * @Usage
 * // Inform a player about entering a new area
 * - title title:"<&a>Tatooine" subtitle:"<&6>What a desolate place this is."
 */
public class TitleCommand implements AbstractCommand {

    @Override
    public @NonNull String getName() {
        return "title";
    }

    @Override
    public @NonNull String getSyntax() {
        return "(<text>) (title:<text>) (subtitle:<text>) (fadeIn:<duration>) (stay:<duration>) (fadeOut:<duration>) (targets:<player>|...)";
    }

    @Override
    public int getMinArgs() {
        return 0;
    }

    @Override
    public int getMaxArgs() {
        return 6;
    }

    @Override
    public boolean isAsyncSafe() {
        return true;
    }

    private static final ArgumentSchema SCHEMA = ArgumentSchema.of()
            .optionalLinear(0, AbstractTag.class, null)
            .optionalPrefix("title", AbstractTag.class)
            .optionalPrefix("subtitle", AbstractTag.class)
            .optionalPrefix("fadeIn", DurationTag.class, "1s")
            .optionalPrefix("stay", DurationTag.class, "3s")
            .optionalPrefix("fadeOut", DurationTag.class, "1s")
            .optionalPrefix("targets", ListTag.class)
            .build();

    @Override
    public void run(@NonNull ScriptQueue queue, @NonNull Instruction instruction) {
        ArgumentSet args = SCHEMA.bind(instruction, queue);
        if (args == null) return;

        AbstractTag titleTag = args.prefix("title");
        if (titleTag == null) {
            titleTag = args.linear(0);
        }

        AbstractTag subTitleTag = args.prefix("subtitle");

        if (titleTag == null && subTitleTag == null) {
            Debugger.echoError(queue, "Must specify a title or subtitle!");
            return;
        }

        Component titleComp = buildComponent(titleTag);
        Component subTitleComp = buildComponent(subTitleTag);

        long fadeInMs = durationMs(args.prefix("fadeIn"), 1000L);
        long stayMs = durationMs(args.prefix("stay"), 3000L);
        long fadeOutMs = durationMs(args.prefix("fadeOut"), 1000L);

        Title.Times times = Title.Times.times(
                Duration.ofMillis(fadeInMs),
                Duration.ofMillis(stayMs),
                Duration.ofMillis(fadeOutMs)
        );

        final Title title = Title.title(titleComp, subTitleComp, times);

        ListTag targetsList = args.prefix("targets");
        List<Player> targetPlayers = null;

        if (targetsList != null) {
            targetPlayers = new ArrayList<>();
            for (PlayerTag pTag : targetsList.filter(PlayerTag.class, queue)) {
                Player player = pTag.getPlayer().orElse(null);
                if (player != null && player.isActive()) {
                    targetPlayers.add(player);
                }
            }
        }

        Debugger.report(queue, instruction,
                "Title", titleTag != null ? titleTag.identify() : "None",
                "Subtitle", subTitleTag != null ? subTitleTag.identify() : "None",
                "FadeIn", fadeInMs + "ms",
                "Stay", stayMs + "ms",
                "FadeOut", fadeOutMs + "ms",
                "Targets", targetsList != null ? targetsList.identify() : "Linked Player"
        );

        if (targetPlayers != null) {
            if (targetPlayers.isEmpty()) return;

            for (Player player : targetPlayers) {
                player.showTitle(title);
            }
        } else {
            PlayerTag queuePlayer = (PlayerTag) queue.getPlayer();
            if (queuePlayer != null && queuePlayer.isOnline()) {
                queuePlayer.getPlayer().ifPresent(player -> player.showTitle(title));
            } else {
                Debugger.echoError(queue, "No valid targets found and no player attached to the queue.");
            }
        }
    }

    private long durationMs(DurationTag duration, long defaultMs) {
        return duration != null ? duration.getMilliseconds() : defaultMs;
    }

    private Component buildComponent(AbstractTag text) {
        if (text == null) return Component.empty();
        return text.asComponent();
    }
}
