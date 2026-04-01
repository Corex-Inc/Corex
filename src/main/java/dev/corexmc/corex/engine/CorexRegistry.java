package dev.corexmc.corex.engine;

import dev.corexmc.corex.api.commands.AbstractCommand;
import dev.corexmc.corex.api.containers.AbstractContainer;
import dev.corexmc.corex.api.tags.AbstractFormatter;
import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.engine.registry.FormatRegistry;
import dev.corexmc.corex.engine.registry.ScriptCommandRegistry;
import dev.corexmc.corex.engine.utils.CorexLogger;
import dev.corexmc.corex.engine.utils.debugging.Debugger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CorexRegistry {
    private final ScriptCommandRegistry scriptCommandRegistry;
    private final FormatRegistry formatRegistry;
    private final java.util.List<Class<? extends AbstractTag>> registeredTagClasses = new java.util.ArrayList<>();
    private final List<Class<? extends AbstractFormatter>> registeredFormatterClasses = new ArrayList<>();
    private final Map<String, Class<? extends AbstractContainer>> registeredContainerClasses = new HashMap<>();

    public CorexRegistry() {
        this.scriptCommandRegistry = new ScriptCommandRegistry();
        this.formatRegistry = new FormatRegistry();
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
                    registeredTagClasses.add(clazz.asSubclass(AbstractTag.class));
                    CorexLogger.info("Tag registered: <yellow>" + clazz.getSimpleName() + "</yellow>");
                }

                else if (AbstractFormatter.class.isAssignableFrom(clazz)) {
                    AbstractFormatter formatter =
                            (AbstractFormatter) clazz.getDeclaredConstructor().newInstance();

                    formatRegistry.register(formatter);
                    registeredFormatterClasses.add(clazz.asSubclass(dev.corexmc.corex.api.tags.AbstractFormatter.class));
                    CorexLogger.info("FormatTag registered: <yellow><" + formatter.getName() + "></yellow>");
                }

                else if (AbstractContainer.class.isAssignableFrom(clazz)) {
                    AbstractContainer dummy = (AbstractContainer) clazz.getDeclaredConstructor().newInstance();

                    registeredContainerClasses.put(dummy.getType().toLowerCase(), clazz.asSubclass(AbstractContainer.class));
                    dev.corexmc.corex.engine.utils.CorexLogger.info("Script container registered: <yellow>" + dummy.getType() + "</yellow>");
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

    public FormatRegistry getFormats() {
        return formatRegistry;
    }

    public java.util.List<Class<? extends AbstractTag>> getRegisteredTagClasses() {
        return registeredTagClasses;
    }

    public List<Class<? extends AbstractFormatter>> getRegisteredFormatterClasses() {
        return registeredFormatterClasses;
    }

    public Class<? extends AbstractContainer> getContainerClass(String type) {
        return registeredContainerClasses.get(type.toLowerCase());
    }
}
