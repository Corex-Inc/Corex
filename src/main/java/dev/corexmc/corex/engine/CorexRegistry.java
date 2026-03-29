package dev.corexmc.corex.engine;

import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.engine.registry.ScriptCommandRegistry;
import dev.corexmc.corex.engine.utils.CorexLogger;
import dev.corexmc.corex.engine.utils.debugging.Debugger;

public class CorexRegistry {
    private final ScriptCommandRegistry scriptCommandRegistry;

    public CorexRegistry() {
        this.scriptCommandRegistry = new ScriptCommandRegistry();
    }

    public void register(Class<?>... injectables) {
        for (Class<?> clazz : injectables) {
            try {
                if (AbstractCommand.class.isAssignableFrom(clazz)) {
                    AbstractCommand command = (AbstractCommand) clazz.getDeclaredConstructor().newInstance();
                    scriptCommandRegistry.register(command);
                    CorexLogger.info("Script command registered: <yellow>- " + command.getName() + "</yellow>");
                }

                else if (AbstractTag.class.isAssignableFrom(clazz)) {
                    java.lang.reflect.Method method = clazz.getDeclaredMethod("register");
                    method.invoke(null);
                    CorexLogger.info("Tag registered: <yellow>" + clazz.getSimpleName() + "</yellow>");
                }

                else {
                    CorexLogger.warn("Class " + clazz.getSimpleName() + " not found!");
                }

            } catch (NoSuchMethodException e) {
                Debugger.echoError("Class " + clazz.getSimpleName() + " doesn't have method!");
            } catch (Exception e) {
                Debugger.echoError("Error with registration " + clazz.getSimpleName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public ScriptCommandRegistry getScriptCommands() {
        return scriptCommandRegistry;
    }

}
