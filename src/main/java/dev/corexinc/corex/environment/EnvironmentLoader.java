package dev.corexinc.corex.environment;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.GlobalTagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.CorexRegistry;
import dev.corexinc.corex.environment.commands.world.SetBlockCommand;
import dev.corexinc.corex.environment.events.EventRegistry;
// Commands
import dev.corexinc.corex.environment.commands.core.*;
import dev.corexinc.corex.environment.commands.player.*;
// Containers
import dev.corexinc.corex.environment.containers.*;
// Events
import dev.corexinc.corex.environment.events.implementation.core.*;
import dev.corexinc.corex.environment.events.implementation.player.*;
// Formatters
import dev.corexinc.corex.environment.formatters.*;
// BaseTags
import dev.corexinc.corex.environment.tags.core.*;
import dev.corexinc.corex.environment.tags.entity.*;
import dev.corexinc.corex.environment.tags.player.*;
import dev.corexinc.corex.environment.tags.world.*;
import dev.corexinc.corex.environment.tags.world.area.*;
// DataActions
import dev.corexinc.corex.environment.data.actions.*;
// GlobalFlags
import dev.corexinc.corex.environment.flags.*;

public class EnvironmentLoader {
    public static void registerDefaults(CorexRegistry registry) {
        // Global Tags
        GlobalTagProcessor.register();

        // DefinitionTag
        BaseTagProcessor.registerBaseTag("", (attribute) -> {
            if (attribute.hasParam() && attribute.getQueue() != null) {
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
                WebsocketCommand.class,
                FetchCommand.class,
                FlagCommand.class,
                SetBlockCommand.class,

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
                CuboidTag.class,
                EllipsoidTag.class,
                PolygonTag.class,
                ServerTag.class,

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

        // Events
        EventRegistry.register(
                DeltaTimeEvent.class,
                PlayerJoinEvent.class,
                PlayerBreakBlockEvent.class,
                WebsocketScriptEvent.class,
                FlagExpireEvent.class,
                PlayerPlaceBlockEvent.class,
                PlayerInputEvent.class
        );
    }
}