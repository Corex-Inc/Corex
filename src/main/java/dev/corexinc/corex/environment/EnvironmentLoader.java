package dev.corexinc.corex.environment;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.CorexRegistry;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
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
import dev.corexinc.corex.environment.formatters.*;
// BaseTags
import dev.corexinc.corex.environment.tags.core.*;
import dev.corexinc.corex.environment.tags.player.*;
import dev.corexinc.corex.environment.tags.world.*;

public class EnvironmentLoader {
    public static void registerDefaults(CorexRegistry registry) {

        // DefinitionTag
        BaseTagProcessor.registerBaseTag("", (attribute) -> {
            if (attribute.hasParam() && attribute.getQueue() != null) {
                AbstractTag def = attribute.getQueue().getDefinition(attribute.getParam());
                if (def != null) {
                    return ObjectFetcher.pickObject(def.identify());
                }
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

                // Tags
                ElementTag.class,
                PlayerTag.class,
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

                // Formatters
                NewLineFormatter.class,
                SpaceFormatter.class,
                CharFormatter.class,

                // Script containers
                TaskContainer.class,
                EventsContainer.class,

                // Global Flags
                IfGlobalFlag.class
        );

        // Events
        EventRegistry.register(
                DeltaTimeEvent.class,
                PlayerJoinEvent.class,
                PlayerBreakBlockEvent.class
        );
    }
}