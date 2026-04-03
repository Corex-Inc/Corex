package dev.corexmc.corex.environment;

import dev.corexmc.corex.api.processors.BaseTagProcessor;
import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.engine.CorexRegistry;
import dev.corexmc.corex.engine.tags.ObjectFetcher;
import dev.corexmc.corex.environment.events.EventRegistry;
// Commands
import dev.corexmc.corex.environment.commands.core.*;
import dev.corexmc.corex.environment.commands.player.*;
// Containers
import dev.corexmc.corex.environment.containers.*;
// Events
import dev.corexmc.corex.environment.events.impl.core.*;
// Formatters
import dev.corexmc.corex.environment.flags.IfGlobalFlag;
import dev.corexmc.corex.environment.formatters.*;
// BaseTags
import dev.corexmc.corex.environment.tags.core.*;
import dev.corexmc.corex.environment.tags.player.*;
import dev.corexmc.corex.environment.tags.world.*;

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
                DeltaTimeEvent.class
        );
    }
}
