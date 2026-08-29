package dev.corexinc.corex.api.addons;

import org.jetbrains.annotations.ApiStatus.AvailableSince;

/**
 * Declares a plugin to be a Corex addon.
 *
 * <p>Implement this on the plugin main class. It carries no methods and no lifecycle: Corex never
 * calls back into an addon, the addon registers what it wants when it wants. The interface exists
 * so the engine can tell an addon apart from any other plugin that happens to be on the
 * classpath, and refuse registrations from the latter.</p>
 *
 * <h2>Paper</h2>
 *
 * <p>Register from {@code onLoad()}. Bukkit runs every plugin {@code onLoad()} before the first
 * {@code onEnable()}, and Corex compiles scripts in its own {@code onEnable()}, so an addon that
 * registers in {@code onLoad()} is always in place before the first script is compiled no matter
 * what order the plugins loaded in.</p>
 *
 * <pre>{@code
 * public class MyAddon extends JavaPlugin implements AbstractCorexAddon {
 *
 *     @Override
 *     public void onLoad() {
 *         CorexRegistrar.open(this)
 *                 .register(MyCommand.class, MyTag.class, MyEvent.class)
 *                 .close();
 *     }
 * }
 * }</pre>
 *
 * <p>The addon must depend on Corex in its {@code paper-plugin.yml}, or Paper never links the
 * addon classloader to Corex and this interface will not resolve at runtime:</p>
 *
 * <pre>{@code
 * dependencies:
 *   server:
 *     Corex:
 *       load: BEFORE
 *       required: true
 * }</pre>
 *
 * <h2>Velocity</h2>
 *
 * <p>The proxy has no split between loading and enabling, so the window sits inside
 * {@code ProxyInitializeEvent} instead: Corex builds its registries in the first handler and
 * compiles in the last one, leaving every normal-priority handler in between. Subscribe the usual
 * way and register there.</p>
 *
 * <pre>{@code
 * @Plugin(id = "myaddon", dependencies = @Dependency(id = "corex"))
 * public class MyAddon implements AbstractCorexAddon {
 *
 *     @Subscribe
 *     public void onInit(ProxyInitializeEvent event) {
 *         CorexRegistrar.open(this)
 *                 .register(MyCommand.class, MyTag.class)
 *                 .close();
 *     }
 * }
 * }</pre>
 *
 * <p>The {@code @Dependency} is what puts Corex's classes on the addon's classloader, and what
 * makes the proxy load Corex first.</p>
 *
 * <p>On both platforms, registering later is refused: the compiler has already passed by, and a
 * command nobody compiled against would only look like it works.</p>
 *
 * @see CorexRegistrar
 * @since 1.0.0
 */
@AvailableSince("1.0.0")
public interface AbstractCorexAddon {
}
