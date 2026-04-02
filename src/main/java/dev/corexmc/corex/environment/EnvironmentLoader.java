package dev.corexmc.corex.environment;

import dev.corexmc.corex.api.processors.BaseTagProcessor;
import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.engine.CorexRegistry;
import dev.corexmc.corex.engine.tags.ObjectFetcher;
import dev.corexmc.corex.environment.commands.core.*;
import dev.corexmc.corex.environment.commands.player.*;
import dev.corexmc.corex.environment.containers.*;
import dev.corexmc.corex.environment.events.EventRegistry;
import dev.corexmc.corex.environment.events.impl.core.DeltaTimeEvent;
import dev.corexmc.corex.environment.formatters.*;
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
                SwitchCaseCommand.class,
                SwitchDefaultCommand.class,
                IfCommand.class,
                IfElseCommand.class,

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

                // Formatters
                NewLineFormatter.class,
                SpaceFormatter.class,
                CharFormatter.class,

                // Script containers
                TaskContainer.class,
                EventsContainer.class
        );

        // Events
        EventRegistry.register(
                DeltaTimeEvent.class
        );
    }
}
