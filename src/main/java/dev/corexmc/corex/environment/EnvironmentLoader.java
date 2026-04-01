package dev.corexmc.corex.environment;

import dev.corexmc.corex.api.processors.BaseTagProcessor;
import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.engine.CorexRegistry;
import dev.corexmc.corex.engine.tags.ObjectFetcher;
import dev.corexmc.corex.environment.commands.core.DefCommand;
import dev.corexmc.corex.environment.commands.core.ReloadCommand;
import dev.corexmc.corex.environment.commands.core.DoCommand;
import dev.corexmc.corex.environment.commands.core.WaitCommand;
import dev.corexmc.corex.environment.commands.player.KickCommand;
import dev.corexmc.corex.environment.commands.player.NarrateCommand;
import dev.corexmc.corex.environment.commands.player.TeleportCommand;
import dev.corexmc.corex.environment.formatters.CharFormatter;
import dev.corexmc.corex.environment.formatters.NewLineFormatter;
import dev.corexmc.corex.environment.formatters.SpaceFormatter;
import dev.corexmc.corex.environment.tags.core.*;
import dev.corexmc.corex.environment.tags.player.PlayerTag;
import dev.corexmc.corex.environment.tags.world.LocationTag;
import dev.corexmc.corex.environment.tags.world.MaterialTag;
import dev.corexmc.corex.environment.tags.world.WorldTag;

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

                // Formatters
                NewLineFormatter.class,
                SpaceFormatter.class,
                CharFormatter.class
        );
    }
}
