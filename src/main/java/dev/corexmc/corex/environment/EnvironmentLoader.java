package dev.corexmc.corex.environment;

import dev.corexmc.corex.engine.CorexRegistry;
import dev.corexmc.corex.environment.commands.core.DefCommand;
import dev.corexmc.corex.environment.commands.core.ReloadCommand;
import dev.corexmc.corex.environment.commands.core.DoCommand;
import dev.corexmc.corex.environment.commands.player.KickCommand;
import dev.corexmc.corex.environment.commands.player.NarrateCommand;
import dev.corexmc.corex.environment.commands.player.TeleportCommand;
import dev.corexmc.corex.environment.tags.*;

public class EnvironmentLoader {
    public static void registerDefaults(CorexRegistry registry) {
        registry.register(

                // Commands
                NarrateCommand.class,
                TeleportCommand.class,
                DoCommand.class,
                ReloadCommand.class,
                KickCommand.class,
                DefCommand.class,

                // Tags
                ElementTag.class,
                PlayerTag.class,
                ListTag.class,
                MapTag.class,
                MaterialTag.class,
                LocationTag.class
        );
    }
}
