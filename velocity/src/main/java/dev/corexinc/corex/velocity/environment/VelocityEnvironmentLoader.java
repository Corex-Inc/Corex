package dev.corexinc.corex.velocity.environment;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.GlobalTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.CorexRegistry;
// Commands
import dev.corexinc.corex.environment.commands.core.*;
import dev.corexinc.corex.velocity.environment.commands.core.PingCommand;
import dev.corexinc.corex.velocity.environment.commands.core.ReloadCommand;
import dev.corexinc.corex.velocity.environment.commands.player.ActionBarCommand;
import dev.corexinc.corex.velocity.environment.commands.player.ConnectCommand;
import dev.corexinc.corex.velocity.environment.commands.player.KickCommand;
import dev.corexinc.corex.velocity.environment.commands.player.NarrateCommand;
import dev.corexinc.corex.velocity.environment.commands.player.TitleCommand;
import dev.corexinc.corex.velocity.environment.commands.player.ResourcePackCommand;
// Containers
import dev.corexinc.corex.environment.containers.TaskContainer;
// Formatters
import dev.corexinc.corex.environment.formatters.*;
// Tags
import dev.corexinc.corex.environment.tags.core.*;
import dev.corexinc.corex.environment.tags.utils.RandomTag;
import dev.corexinc.corex.velocity.environment.tags.core.PluginTag;
import dev.corexinc.corex.velocity.environment.tags.core.ServerTag;
import dev.corexinc.corex.velocity.environment.tags.core.VelocityTag;
import dev.corexinc.corex.velocity.environment.tags.player.PlayerTag;
// DataActions
import dev.corexinc.corex.environment.data.actions.*;
// GlobalFlags
import dev.corexinc.corex.environment.flags.*;

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
                ResourcePackCommand.class,
                KickCommand.class,
                ConnectCommand.class,
                TitleCommand.class,
                ActionBarCommand.class,
                DoCommand.class,
                ReloadCommand.class,
                PingCommand.class,
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
                TryCommand.class,
                CatchCommand.class, // Try subcommand
                FinallyCommand.class, // Try subcommand
                AsyncCommand.class,
                AdjustCommand.class,
                FlagCommand.class,
                FetchCommand.class,
                WebsocketCommand.class,
                InjectCommand.class,
                ReturnCommand.class,
                StopCommand.class,

                // Tags
                ElementTag.class,
                PlayerTag.class,
                ServerTag.class,
                PluginTag.class,
                VelocityTag.class,
                ListTag.class,
                MapTag.class,
                DurationTag.class,
                ColorTag.class,
                ContextTag.class,
                EnvTag.class,
                UtilTag.class,
                RandomTag.class,
                QueueTag.class,

                // Formatters
                NewLineFormatter.class,
                SpaceFormatter.class,
                CharFormatter.class,
                SpriteFormatter.class,
                ColorFormatter.class,
                HeadFormatter.class,
                FontFormatter.class,
                GradientFormatter.class,
                KeybindFormatter.class,
                ParagraphFormatter.class,
                ScoreFormatter.class,
                TranslateFormatter.class,

                // Script containers
                TaskContainer.class,

                // Global Flags
                IfGlobalFlag.class,
                PlayerGlobalFlag.class,
                SaveGlobalFlag.class,

                // Data Actions
                IncrementAction.class,
                DecrementAction.class,
                AddNumberAction.class,
                SubNumberAction.class,
                AddToListAction.class,
                RemoveFromListAction.class,
                MergeListsAction.class,
                PutIfAbsentAction.class,
                UndefineAction.class,
                AssignAction.class

        );
    }
}
