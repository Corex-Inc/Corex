package dev.corexinc.corex.environment;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.engine.CorexRegistry;
import dev.corexinc.corex.environment.events.EventRegistry;
// Commands
import dev.corexinc.corex.environment.commands.core.*;
import dev.corexinc.corex.environment.commands.player.*;
// Containers
import dev.corexinc.corex.environment.containers.*;
// Events
import dev.corexinc.corex.environment.events.implementation.core.*;
// Formatters
import dev.corexinc.corex.environment.events.implementation.player.PlayerBreakBlockEvent;
import dev.corexinc.corex.environment.events.implementation.player.PlayerJoinEvent;
import dev.corexinc.corex.environment.flags.IfGlobalFlag;
import dev.corexinc.corex.environment.flags.PlayerGlobalFlag;
import dev.corexinc.corex.environment.formatters.*;
// BaseTags
import dev.corexinc.corex.environment.tags.core.*;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.player.*;
import dev.corexinc.corex.environment.tags.world.*;

public class EnvironmentLoader {
    public static void registerDefaults(CorexRegistry registry) {

        // DefinitionTag
        BaseTagProcessor.registerBaseTag("", (attribute) -> {
            if (attribute.hasParam() && attribute.getQueue() != null) {
                return attribute.getQueue().getDefinition(attribute.getParam());
            }
            return null;
        });

        registry.register(

                // Commands
                NarrateCommand.class,
                TeleportCommand.class,
                DoCommand.class,
                ReloadCommand.class,
                KickCommand.class,
                DefCommand.class,
                WaitCommand.class,
                RepeatCommand.class,
                SwitchCommand.class,
                SwitchCaseCommand.class, // Switch subcommand
                SwitchDefaultCommand.class, // Switch subcommand
                IfCommand.class,
                IfElseCommand.class, // If subcommand
                InjectCommand.class,
                ReturnCommand.class,
                AdjustCommand.class,
                GiveCommand.class,

                // Tags
                ElementTag.class,
                PlayerTag.class,
                EntityTag.class,
                ListTag.class,
                MapTag.class,
                MaterialTag.class,
                LocationTag.class,
                EnvTag.class,
                DurationTag.class,
                WorldTag.class,
                ContextTag.class,
                UtilTag.class,
                RandomTag.class,
                ItemTag.class,

                // Formatters
                NewLineFormatter.class,
                SpaceFormatter.class,
                CharFormatter.class,
                SpriteFormatter.class,
                ColorFormatter.class,
                HeadFormatter.class,

                // Script containers
                TaskContainer.class,
                EventsContainer.class,
                ItemContainer.class,

                // Global Flags
                IfGlobalFlag.class,
                PlayerGlobalFlag.class
        );

        // Events
        EventRegistry.register(
                DeltaTimeEvent.class,
                PlayerJoinEvent.class,
                PlayerBreakBlockEvent.class
        );
    }
}