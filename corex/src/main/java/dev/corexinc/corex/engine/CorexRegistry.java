package dev.corexinc.corex.engine;

import dev.corexinc.corex.api.commands.AbstractCommand;
import dev.corexinc.corex.api.containers.AbstractContainer;
import dev.corexinc.corex.api.data.actions.AbstractDataAction;
import dev.corexinc.corex.api.flags.AbstractGlobalFlag;
import dev.corexinc.corex.api.scripts.AbstractPreprocessor;
import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.addons.AddonManager;
import dev.corexinc.corex.engine.addons.AddonOwner;
import dev.corexinc.corex.engine.addons.AddonOwnership;
import dev.corexinc.corex.engine.registry.FormatRegistry;
import dev.corexinc.corex.engine.registry.RegistryExtension;
import dev.corexinc.corex.engine.registry.ScriptCommandRegistry;
import dev.corexinc.corex.engine.scripts.PreprocessorRegistry;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import java.util.*;

/**
 * The engine's component registry.
 *
 * <p>Registration is gated: Corex fills this from its own environment loader, and an addon fills
 * it through {@code CorexRegistrar.open(this)}. Anything else calling {@link #register} is refused
 * and reported by name, so a component can always be traced back to whoever supplied it.</p>
 */
public class CorexRegistry {

    private static final List<RegistryExtension> extensions = new ArrayList<>(1);

    private final ScriptCommandRegistry scriptCommandRegistry;
    private final FormatRegistry formatRegistry;
    private final PreprocessorRegistry preprocessorRegistry;
    private final List<AbstractCommand> registeredCommands = new ArrayList<>();
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
        this.preprocessorRegistry = new PreprocessorRegistry();
    }

    /**
     * Adds a handler for a component kind the engine cannot dispatch on its own.
     *
     * @param extension the extension, tried in the order extensions were added.
     */
    public static void addExtension(@NonNull RegistryExtension extension) {
        if (!extensions.contains(extension)) {
            extensions.add(extension);
        }
    }

    /**
     * Registers components, dispatching each by the interface it implements.
     *
     * <p>Refused unless the caller is Corex itself or an addon with an open
     * {@code CorexRegistrar}; see {@link AddonManager#requireOwner}. Everything that lands here,
     * including the sub-tags and mechanisms a tag class registers on its way through, is attributed
     * to that owner.</p>
     *
     * <p>A class that fits no component interface and no {@link RegistryExtension} is reported and
     * skipped, and so is one whose registration throws, so a single broken component cannot take
     * the rest of an addon down with it.</p>
     *
     * @param injectables the component classes.
     */
    public void register(Class<?>... injectables) {
        AddonOwner owner = AddonManager.requireOwner(describe(injectables));
        if (owner == null) {
            return;
        }

        AddonOwner previousOwner = AddonManager.enter(owner);
        try {
            for (Class<?> clazz : injectables) {
                registerSingle(clazz, owner);
            }
        }
        finally {
            AddonManager.exit(previousOwner);
        }
    }

    private void registerSingle(Class<?> clazz, AddonOwner owner) {
        try {
            if (AbstractCommand.class.isAssignableFrom(clazz)) {
                AbstractCommand command = (AbstractCommand) clazz.getDeclaredConstructor().newInstance();
                scriptCommandRegistry.register(command);
                registeredCommands.add(command);

                AddonOwnership.claim(AddonOwnership.Kind.COMMAND, command.getName().toLowerCase(), owner);
                for (String alias : command.getAlias()) {
                    AddonOwnership.claim(AddonOwnership.Kind.COMMAND, alias.toLowerCase(), owner);
                }
            }

            else if (AbstractTag.class.isAssignableFrom(clazz)) {
                java.lang.reflect.Method method = clazz.getDeclaredMethod("register");
                method.invoke(null);
                registeredTagClasses.add(clazz.asSubclass(AbstractTag.class));

                AddonOwnership.claim(AddonOwnership.Kind.OBJECT, clazz.getSimpleName(), owner);
            }

            else if (AbstractFormatter.class.isAssignableFrom(clazz)) {
                AbstractFormatter formatter = (AbstractFormatter) clazz.getDeclaredConstructor().newInstance();
                formatRegistry.register(formatter);
                registeredFormatterClasses.add(clazz.asSubclass(AbstractFormatter.class));

                AddonOwnership.claim(AddonOwnership.Kind.FORMATTER, formatter.getName(), owner);
                for (String alias : formatter.getAlias()) {
                    AddonOwnership.claim(AddonOwnership.Kind.FORMATTER, alias, owner);
                }
            }

            else if (AbstractContainer.class.isAssignableFrom(clazz)) {
                AbstractContainer dummy = (AbstractContainer) clazz.getDeclaredConstructor().newInstance();
                registeredContainerClasses.put(dummy.getType().toLowerCase(), clazz.asSubclass(AbstractContainer.class));

                AddonOwnership.claim(AddonOwnership.Kind.CONTAINER, dummy.getType().toLowerCase(), owner);
            }

            else if (AbstractGlobalFlag.class.isAssignableFrom(clazz)) {
                AbstractGlobalFlag flag = (AbstractGlobalFlag) clazz.getDeclaredConstructor().newInstance();
                globalFlags.put(flag.getName().toLowerCase(), flag);

                AddonOwnership.claim(AddonOwnership.Kind.GLOBAL_FLAG, flag.getName().toLowerCase(), owner);
            }

            else if (AbstractPreprocessor.class.isAssignableFrom(clazz)) {
                AbstractPreprocessor preprocessor = (AbstractPreprocessor) clazz.getDeclaredConstructor().newInstance();
                preprocessorRegistry.register(preprocessor, owner);

                AddonOwnership.claim(AddonOwnership.Kind.PREPROCESSOR, preprocessor.getName(), owner);
            }

            else if (AbstractDataAction.class.isAssignableFrom(clazz)) {
                AbstractDataAction action = (AbstractDataAction) clazz.getDeclaredConstructor().newInstance();
                registerAction(action);

                AddonOwnership.claim(AddonOwnership.Kind.DATA_ACTION, action.getSymbol(), owner);
            }

            else if (!registerThroughExtension(clazz)) {
                CorexLogger.warn("Class " + clazz.getSimpleName() + " not found!");
                return;
            }

            AddonManager.noteClass(clazz, owner);

        } catch (NoSuchMethodException e) {
            Debugger.error("Class " + clazz.getSimpleName() + " doesn't have required method!", e);
        } catch (Throwable e) {
            Debugger.error("Error registering " + clazz.getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    private static boolean registerThroughExtension(Class<?> clazz) {
        for (RegistryExtension extension : extensions) {
            if (extension.tryRegister(clazz)) return true;
        }
        return false;
    }

    private static String describe(Class<?>... injectables) {
        if (injectables.length == 0) {
            return "nothing";
        }
        if (injectables.length == 1) {
            return "'" + injectables[0].getSimpleName() + "'";
        }
        return injectables.length + " components starting with '" + injectables[0].getSimpleName() + "'";
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

    public ScriptCommandRegistry getScriptCommands() {
        return scriptCommandRegistry;
    }

    public FormatRegistry getFormats() {
        return formatRegistry;
    }

    /**
     * The addon passes that run while scripts compile.
     *
     * @return the preprocessor registry.
     */
    public PreprocessorRegistry getPreprocessors() {
        return preprocessorRegistry;
    }

    /**
     * Every command registered through this registry, in registration order.
     * <p>
     * Unlike {@code getScriptCommands()}, this lists each command once rather than once per
     * name and alias, which is what a sweep over all commands wants.
     */
    public List<AbstractCommand> getRegisteredCommands() {
        return registeredCommands;
    }

    public List<Class<? extends AbstractTag>> getRegisteredTagClasses() {
        return registeredTagClasses;
    }

    public List<Class<? extends AbstractFormatter>> getRegisteredFormatterClasses() {
        return registeredFormatterClasses;
    }

    public Class<? extends AbstractContainer> getContainerClass(String type) {
        return registeredContainerClasses.get(type.toLowerCase());
    }

    public AbstractGlobalFlag getGlobalFlag(String name) {
        return globalFlags.get(name.toLowerCase());
    }

    public Set<String> getGlobalFlagsNames() {
        return globalFlags.keySet();
    }
}
