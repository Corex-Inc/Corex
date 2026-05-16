package dev.corexinc.corex.velocity.environment.utils;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.GlobalTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.CorexRegistry;
// Commands
import dev.corexinc.corex.environment.commands.core.*;
import dev.corexinc.corex.velocity.environment.commands.core.ReloadCommand;
import dev.corexinc.corex.velocity.environment.commands.player.NarrateCommand;
// Containers
import dev.corexinc.corex.environment.containers.TaskContainer;
// Formatters
import dev.corexinc.corex.environment.formatters.ColorFormatter;
// Tags
import dev.corexinc.corex.environment.tags.core.*;
import dev.corexinc.corex.velocity.environment.tags.player.PlayerTag;

public class VelocityEnvironmentLoader {

    public static void registerDefaults(CorexRegistry registry) {

        // Global Tags
        GlobalTagProcessor.register();

        // DefinitionTag
        BaseTagProcessor.registerBaseTag("", (attribute) -> {
            if (attribute.hasParam()) {
                String fullPath = attribute.getParam();

                if (!fullPath.contains(".")) {
                    return attribute.getQueue().getDefinition(fullPath);
                }

                String[] parts = fullPath.split("\\.", -1);
                AbstractTag current = attribute.getQueue().getDefinition(parts[0]);

                for (int i = 1; i < parts.length; i++) {
                    if (!(current instanceof MapTag map)) {
                        return null;
                    }

                    current = map.getObject(parts[i]);
                }

                return current;
            }

            return null;
        });

        registry.register(

                // Commands
                NarrateCommand.class,
                DoCommand.class,
                ReloadCommand.class,
                DefCommand.class,
                WaitCommand.class,
                RepeatCommand.class,
                ForeachCommand.class,
                WhileCommand.class,
                SwitchCommand.class,
                SwitchCaseCommand.class, // Switch subcommand
                SwitchDefaultCommand.class, // Switch subcommand
                IfCommand.class,
                IfElseCommand.class, // If subcommand
                InjectCommand.class,
                ReturnCommand.class,
                StopCommand.class,

                // Tags
                ElementTag.class,
                PlayerTag.class,
                ListTag.class,
                MapTag.class,
                DurationTag.class,
                ColorTag.class,

                // Formatters
                ColorFormatter.class,

                // Script containers
                TaskContainer.class

        );
    }
}