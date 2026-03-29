package dev.corexmc.corex.environment;

import dev.corexmc.corex.engine.CorexRegistry;
import dev.corexmc.corex.engine.tags.TagManager;
import dev.corexmc.corex.environment.commands.NarrateCommand;
import dev.corexmc.corex.environment.commands.TeleportCommand;
import dev.corexmc.corex.environment.tags.ElementTag;
import dev.corexmc.corex.environment.tags.PlayerTag;

public class EnvironmentLoader {
    public static void registerDefaults(CorexRegistry registry) {
        registry.register(
                NarrateCommand.class,
                TeleportCommand.class,
                ElementTag.class,
                PlayerTag.class
        );
    }
}
