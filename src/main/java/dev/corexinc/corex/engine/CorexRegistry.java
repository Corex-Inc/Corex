package dev.corexinc.corex.engine;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.containers.AbstractContainer;
import dev.corexinc.corex.api.data.actions.AbstractDataAction;
import dev.corexinc.corex.api.flags.AbstractGlobalFlag;
import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.registry.FormatRegistry;
import dev.corexinc.corex.engine.registry.ScriptCommandRegistry;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CorexRegistry {

    private final ScriptCommandRegistry scriptCommandRegistry;
    private final FormatRegistry formatRegistry;
    private final List<Class<? extends AbstractTag>> registeredTagClasses = new ArrayList<>();
    private final List<Class<? extends AbstractFormatter>> registeredFormatterClasses = new ArrayList<>();
    private final Map<String, Class<? extends AbstractContainer>> registeredContainerClasses = new HashMap<>();
    private final Map<String, AbstractGlobalFlag> globalFlags = new HashMap<>();

    private final Map<String, AbstractDataAction> exactActions = new HashMap<>(8);
    private final List<AbstractDataAction> prefixActions = new ArrayList<>(4);
    private AbstractDataAction fallbackAction;

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
                }

                else if (AbstractTag.class.isAssignableFrom(clazz)) {
                    java.lang.reflect.Method method = clazz.getDeclaredMethod("register");
                    method.invoke(null);
                    registeredTagClasses.add(clazz.asSubclass(AbstractTag.class));
                }

                else if (AbstractFormatter.class.isAssignableFrom(clazz)) {
                    AbstractFormatter formatter = (AbstractFormatter) clazz.getDeclaredConstructor().newInstance();
                    formatRegistry.register(formatter);
                    registeredFormatterClasses.add(clazz.asSubclass(AbstractFormatter.class));
                }

                else if (AbstractContainer.class.isAssignableFrom(clazz)) {
                    AbstractContainer dummy = (AbstractContainer) clazz.getDeclaredConstructor().newInstance();
                    registeredContainerClasses.put(dummy.getType().toLowerCase(), clazz.asSubclass(AbstractContainer.class));
                }

                else if (AbstractGlobalFlag.class.isAssignableFrom(clazz)) {
                    AbstractGlobalFlag flag = (AbstractGlobalFlag) clazz.getDeclaredConstructor().newInstance();
                    globalFlags.put(flag.getName().toLowerCase(), flag);
                }

                else if (AbstractDataAction.class.isAssignableFrom(clazz)) {
                    AbstractDataAction action = (AbstractDataAction) clazz.getDeclaredConstructor().newInstance();
                    registerAction(action);
                }

                else {
                    CorexLogger.warn("Class " + clazz.getSimpleName() + " not found!");
                }

            } catch (NoSuchMethodException e) {
                Debugger.error("Class " + clazz.getSimpleName() + " doesn't have required method!", e);
            } catch (Exception e) {
                Debugger.error("Error registering " + clazz.getSimpleName() + ": " + e.getMessage(), e);
            }
        }
    }

    private void registerAction(@NonNull AbstractDataAction action) {
        String symbol = action.getSymbol();
        if (symbol.isEmpty()) {
            fallbackAction = action;
        } else if (action.isPrefix()) {
            prefixActions.add(action);
        } else {
            exactActions.put(symbol, action);
        }
    }

    @Nullable
    public AbstractDataAction findAction(@NonNull String actionStr) {
        AbstractDataAction exact = exactActions.get(actionStr);
        if (exact != null) return exact;

        for (AbstractDataAction prefix : prefixActions) {
            if (actionStr.startsWith(prefix.getSymbol())) return prefix;
        }

        return fallbackAction;
    }

    public ScriptCommandRegistry getScriptCommands() { return scriptCommandRegistry; }
    public FormatRegistry getFormats() { return formatRegistry; }
    public List<Class<? extends AbstractTag>> getRegisteredTagClasses() { return registeredTagClasses; }
    public List<Class<? extends AbstractFormatter>> getRegisteredFormatterClasses() { return registeredFormatterClasses; }
    public Class<? extends AbstractContainer> getContainerClass(String type) { return registeredContainerClasses.get(type.toLowerCase()); }
    public AbstractGlobalFlag getGlobalFlag(String name) { return globalFlags.get(name.toLowerCase()); }
}