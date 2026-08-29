package dev.corexinc.corex.engine.addons;

import dev.corexinc.corex.engine.utils.CorexLogger;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decides who is allowed to register into Corex, and remembers who registered what.
 *
 * <p>Registration is only legal inside an open scope. An addon opens one through
 * {@code CorexRegistrar.open(this)}, pours its components in, and closes it again; Corex opens
 * one around its own environment loader. A registration arriving with no scope open is traced
 * back to its caller: Corex's own code is let through, and anything else is refused with a line
 * naming the plugin. That is what keeps the registries closed to plugins that never declared
 * themselves as addons.</p>
 *
 * <p>Only one scope may be open at a time. Two addons registering at once would make ownership
 * ambiguous, and ownership is the whole point, so the second open is refused and names the addon
 * still holding the lid off.</p>
 *
 * <p>The window closes for good when scripts are compiled ({@link #seal()}): anything registered
 * after that would be invisible to already-compiled scripts, so it is refused with an explanation
 * instead of half working.</p>
 *
 * @since 1.0.0
 */
@AvailableSince("1.0.0")
public final class AddonManager {

    private static final StackWalker CALLER_WALKER =
            StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE);

    private static final ClassLoader CORE_LOADER = AddonManager.class.getClassLoader();

    private static final List<AddonOwner> addons = new ArrayList<>();
    private static final Map<Class<?>, AddonOwner> callerCache = new ConcurrentHashMap<>();
    private static final Map<String, AddonOwner> classOwners = new ConcurrentHashMap<>();
    private static final Set<String> warnedOwners = ConcurrentHashMap.newKeySet();

    private static volatile AddonOwner scopeOwner;
    private static volatile boolean sealed;

    private AddonManager() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Opens the registration scope for an owner.
     *
     * @param owner the owner about to register.
     * @throws IllegalStateException if the window is already sealed, or another scope is open.
     */
    @Internal
    public static void openScope(@NotNull AddonOwner owner) {
        if (sealed) {
            throw new IllegalStateException("Corex has already compiled its scripts, so "
                    + owner.fullName() + " can no longer register anything. Register from "
                    + registrationHint() + " instead, which always runs before that.");
        }
        AddonOwner holder = scopeOwner;
        if (holder != null) {
            throw new IllegalStateException(owner.fullName() + " tried to register while "
                    + holder.label() + " still holds an open registrar. Close it (or use "
                    + "try-with-resources) before anything else registers.");
        }
        scopeOwner = owner;

        if (!owner.isCore() && !addons.contains(owner)) {
            addons.add(owner);
        }
    }

    /**
     * Closes the registration scope.
     *
     * @param owner the owner that opened it.
     */
    @Internal
    public static void closeScope(@NotNull AddonOwner owner) {
        if (scopeOwner != null && !scopeOwner.equals(owner)) {
            CorexLogger.warn(owner.fullName() + " closed a registrar owned by "
                    + scopeOwner.label() + ".");
        }
        scopeOwner = null;
    }

    /**
     * Returns the owner a registration happening right now belongs to, refusing the call when it
     * comes from outside Corex with no registrar open.
     *
     * @param what a short description of what is being registered, for the refusal message.
     * @return the owner, or {@code null} when the registration must not proceed.
     */
    @Internal
    @Nullable
    public static AddonOwner requireOwner(@NotNull String what) {
        AddonOwner scoped = scopeOwner;
        if (scoped != null) {
            return scoped;
        }

        AddonOwner caller = resolveCaller();
        if (caller.isCore()) {
            return caller;
        }

        if (!caller.mayRegister()) {
            CorexLogger.error("<yellow>" + caller.name() + "</yellow> tried to register " + what
                    + " into Corex, but it is not a Corex addon. Implement AbstractCorexAddon on "
                    + "the plugin main class and register through CorexRegistrar.open(this).");
            return null;
        }
        if (sealed) {
            CorexLogger.error(caller.fullName() + " tried to register " + what + " after Corex "
                    + "compiled its scripts. Register from " + registrationHint() + " instead.");
            return null;
        }
        if (warnedOwners.add(caller.name())) {
            CorexLogger.warn(caller.fullName() + " registered " + what + " without opening a "
                    + "registrar. Use CorexRegistrar.open(this) so everything it registers can be "
                    + "attributed to it.");
        }
        return caller;
    }

    /**
     * Returns the owner of a registration happening right now, without refusing anything.
     *
     * <p>Used by the sub-registries a component fills in on its way through
     * {@code CorexRegistry}: they are already behind the gate, and only need to know who to
     * attribute the entry to.</p>
     *
     * @return the current owner, {@link AddonOwner#CORE} when it cannot be narrowed down.
     */
    @Internal
    @NotNull
    public static AddonOwner currentOwner() {
        AddonOwner scoped = scopeOwner;
        return scoped != null ? scoped : resolveCaller();
    }

    /**
     * Makes an owner the active one for the duration of a registration call, so the sub-registries
     * it reaches do not each have to walk the stack to find out who is calling.
     *
     * @param owner the owner to install.
     * @return whatever was active before, to hand back to {@link #exit(AddonOwner)}.
     */
    @Internal
    @Nullable
    public static AddonOwner enter(@NotNull AddonOwner owner) {
        AddonOwner previous = scopeOwner;
        scopeOwner = owner;
        return previous;
    }

    /**
     * Restores the owner that was active before {@link #enter(AddonOwner)}.
     *
     * @param previous the value {@code enter} returned.
     */
    @Internal
    public static void exit(@Nullable AddonOwner previous) {
        scopeOwner = previous;
    }

    /**
     * Closes the registration window. Called once, right before scripts are compiled.
     */
    @Internal
    public static void seal() {
        AddonOwner leaked = scopeOwner;
        if (leaked != null) {
            CorexLogger.warn(leaked.fullName() + " never closed its registrar. Closing it now, "
                    + "but call close() yourself so a later addon is not blocked.");
            scopeOwner = null;
        }
        sealed = true;
        reportAddons();
    }

    /**
     * Returns whether the registration window has closed.
     *
     * @return {@code true} once scripts have been compiled.
     */
    public static boolean isSealed() {
        return sealed;
    }

    /**
     * Drops everything known about addons and reopens the window.
     *
     * <p>Corex calls this as it loads, so a second load in the same JVM (as the test harness
     * does) starts from an empty slate rather than from the previous run's sealed state.</p>
     */
    @Internal
    public static void reset() {
        scopeOwner = null;
        sealed = false;
        addons.clear();
        callerCache.clear();
        classOwners.clear();
        warnedOwners.clear();
        AddonOwnership.reset();
    }

    /**
     * Every addon that registered something, in the order they first registered.
     *
     * @return the loaded addons.
     */
    @NotNull
    public static List<AddonOwner> getAddons() {
        return addons;
    }

    /**
     * Remembers that a class belongs to an owner, so a stack trace running through it can be
     * blamed on the right plugin.
     *
     * @param clazz the registered class.
     * @param owner its owner.
     */
    @Internal
    public static void noteClass(@NotNull Class<?> clazz, @NotNull AddonOwner owner) {
        if (owner.isCore()) {
            return;
        }
        classOwners.put(clazz.getName(), owner);
    }

    /**
     * Remembers the class a registered handler was written in.
     *
     * <p>Handlers are lambdas, and a lambda runtime class name carries the class that declared
     * it, which is the name a stack trace shows.</p>
     *
     * @param handler the registered handler.
     * @param owner   its owner.
     */
    @Internal
    public static void noteHandler(@NotNull Object handler, @NotNull AddonOwner owner) {
        if (owner.isCore()) {
            return;
        }
        String name = handler.getClass().getName();
        int lambda = name.indexOf("$$Lambda");
        classOwners.put(lambda == -1 ? name : name.substring(0, lambda), owner);
    }

    /**
     * Finds the addon whose code appears in a stack trace.
     *
     * @param cause the throwable to inspect, including its causes.
     * @return the addon to blame, or {@code null} when no addon frame appears.
     */
    @Nullable
    public static AddonOwner blame(@Nullable Throwable cause) {
        if (classOwners.isEmpty()) {
            return null;
        }
        for (Throwable current = cause; current != null; current = current.getCause()) {
            for (StackTraceElement element : current.getStackTrace()) {
                AddonOwner owner = classOwners.get(element.getClassName());
                if (owner != null) return owner;
            }
        }
        return null;
    }

    private static void reportAddons() {
        if (addons.isEmpty()) {
            return;
        }
        StringBuilder line = new StringBuilder("Loaded <#8ce6ff>" + addons.size() + "</#8ce6ff> addon");
        if (addons.size() != 1) {
            line.append("s");
        }
        line.append(":");

        for (AddonOwner addon : addons) {
            line.append(" <yellow>").append(addon.fullName()).append("</yellow>");
        }
        CorexLogger.info(line.toString());
    }

    @NotNull
    private static String registrationHint() {
        AddonResolver resolver = AddonResolver.get();
        return resolver != null ? resolver.registrationHint() : "startup, before scripts compile";
    }

    @NotNull
    private static AddonOwner resolveCaller() {
        AddonResolver resolver = AddonResolver.get();
        if (resolver == null) {
            return AddonOwner.CORE;
        }

        Class<?> external = CALLER_WALKER.walk(frames -> frames
                .limit(24)
                .map(StackWalker.StackFrame::getDeclaringClass)
                .filter(clazz -> clazz.getClassLoader() != CORE_LOADER)
                .findFirst()
                .orElse(null));

        if (external == null) {
            return AddonOwner.CORE;
        }
        return callerCache.computeIfAbsent(external, clazz -> {
            AddonOwner owner = resolver.ownerOfClass(clazz);
            return owner != null ? owner : AddonOwner.CORE;
        });
    }
}
