package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.MechanismProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Adjustable;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import io.papermc.paper.plugin.configuration.PluginMeta;
import org.bukkit.Bukkit;
import org.bukkit.permissions.Permission;
import org.bukkit.plugin.Plugin;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/* @doc object
 *
 * @Name PluginTag
 * @Prefix plugin
 * @IdentifyFormat plugin@Corex
 *
 * @Format
 * The identity format for plugins is the plugin name as written in its plugin.yml, for example
 * `plugin@Corex`. Names are matched case-insensitively, so `<plugin[corex]>` and `<plugin[Corex]>`
 * are the same plugin.
 *
 * @Description
 * A PluginTag is a single plugin on the server: what its plugin.yml says about it, and whether it
 * is currently running.
 *
 * A PluginTag only ever exists for a plugin the server actually loaded. A name nothing matches
 * gives null, so `<plugin[NotHere].exists>` returns 'false' and `.ifNull[...]` catches it.
 * A plugin that is loaded but disabled still resolves, see <@link tag PluginTag.isEnabled>.
 *
 * The tag stores only the name and looks the plugin up on every access, so a PluginTag kept in a
 * definition survives the plugin being disabled and enabled again.
 *
 * @Usage
 * // Report the running Corex version.
 * - narrate "Corex <plugin[Corex].version>"
 *
 * @Usage
 * // Take a different path when an optional dependency is not there.
 * - if !<plugin[Vault].exists>:
 *   - narrate "Economy features are off, Vault is not installed."
 */
public class PluginTag implements AbstractTag, Adjustable {

    private static final String PREFIX = "plugin";

    public static final TagProcessor<PluginTag> TAG_PROCESSOR = new TagProcessor<>();
    public static final MechanismProcessor<PluginTag> MECHANISM_PROCESSOR = new MechanismProcessor<>();

    private final String name;

    public PluginTag(@NonNull Plugin plugin) {
        this.name = plugin.getName();
    }

    public PluginTag(@Nullable String raw) {
        if (raw == null) {
            this.name = "";
            return;
        }

        String stripped = raw.trim();
        if (stripped.toLowerCase().startsWith(PREFIX + "@")) {
            stripped = stripped.substring(PREFIX.length() + 1);
        }

        this.name = stripped;
    }

    public static void register() {
        BaseTagProcessor.registerBaseTag(PREFIX, attr -> {
            if (!attr.hasParam()) return null;
            PluginTag tag = new PluginTag(attr.getParam());
            return tag.getPlugin() != null ? tag : null;
        });

        ObjectFetcher.registerFetcher(PREFIX, raw -> {
            PluginTag tag = new PluginTag(raw);
            return tag.getPlugin() != null ? tag : null;
        });

        /* @doc tag
         *
         * @Name id
         * @RawName <PluginTag.id>
         * @Object PluginTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the plugin's namespace, its name lowercased. That is the form the server uses in
         * namespaced keys, and it matches what the same tag returns on the Velocity port, where the
         * id is the real handle a plugin is known by.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "id", (attr, obj) -> {
            Plugin plugin = obj.getPlugin();
            return plugin == null ? null : new ElementTag(plugin.namespace());
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name name
         * @RawName <PluginTag.name>
         * @Object PluginTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the plugin name exactly as written in its plugin.yml, whatever casing the tag
         * was built with.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "name", (attr, obj) -> {
            Plugin plugin = obj.getPlugin();
            return plugin == null ? null : new ElementTag(plugin.getName());
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name isEnabled
         * @RawName <PluginTag.isEnabled>
         * @Object PluginTag
         * @ReturnType ElementTag(Boolean)
         * @NoArg
         * @Async
         * @Description
         * Returns 'true' if the plugin is currently enabled. A plugin that is loaded but disabled
         * resolves to a tag just fine and returns 'false' here.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isEnabled", (attr, obj) -> {
            Plugin plugin = obj.getPlugin();
            return new ElementTag(plugin != null && plugin.isEnabled());
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name version
         * @RawName <PluginTag.version>
         * @Object PluginTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the version string from the plugin.yml, such as '1.4.2'. It is whatever the
         * author wrote there, so do not count on it being a number you can compare.
         *
         * @Usage
         * // Print the version of every plugin on the server.
         * - foreach <server.plugins> as:entry:
         *   - narrate "<[entry].name> v<[entry].version>"
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "version", (attr, obj) -> {
            PluginMeta meta = obj.getMeta();
            return meta == null ? null : new ElementTag(meta.getVersion());
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name description
         * @RawName <PluginTag.description>
         * @Object PluginTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the plugin's description line, or null when the plugin.yml has none.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "description", (attr, obj) -> {
            PluginMeta meta = obj.getMeta();
            if (meta == null || meta.getDescription() == null) return null;
            return new ElementTag(meta.getDescription());
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name website
         * @RawName <PluginTag.website>
         * @Object PluginTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the website from the plugin.yml. Most plugins set none, in which case this
         * returns null.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "website", (attr, obj) -> {
            PluginMeta meta = obj.getMeta();
            if (meta == null || meta.getWebsite() == null) return null;
            return new ElementTag(meta.getWebsite());
        }).setAsyncSafe().ignoreTest();

        /* @doc tag
         *
         * @Name authors
         * @RawName <PluginTag.authors>
         * @Object PluginTag
         * @ReturnType ListTag(ElementTag)
         * @NoArg
         * @Async
         * @Description
         * Returns the authors listed in the plugin.yml, in the order they were written.
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "authors", (attr, obj) -> {
            PluginMeta meta = obj.getMeta();
            return meta == null ? null : toList(meta.getAuthors());
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name contributors
         * @RawName <PluginTag.contributors>
         * @Object PluginTag
         * @ReturnType ListTag(ElementTag)
         * @NoArg
         * @Async
         * @Description
         * Returns the contributors listed in the plugin.yml. This is a separate list from the
         * authors and is empty on most plugins.
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "contributors", (attr, obj) -> {
            PluginMeta meta = obj.getMeta();
            return meta == null ? null : toList(meta.getContributors());
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name apiVersion
         * @RawName <PluginTag.apiVersion>
         * @Object PluginTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the Minecraft API version the plugin declares, such as '1.21'. That is the
         * version it was written against, not the version the server runs. Legacy plugins declare
         * none and return null here.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "apiVersion", (attr, obj) -> {
            PluginMeta meta = obj.getMeta();
            if (meta == null || meta.getAPIVersion() == null) return null;
            return new ElementTag(meta.getAPIVersion());
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name mainClass
         * @RawName <PluginTag.mainClass>
         * @Object PluginTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the fully qualified name of the plugin's main class, for example
         * 'dev.corexinc.corex.Corex'.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "mainClass", (attr, obj) -> {
            PluginMeta meta = obj.getMeta();
            return meta == null ? null : new ElementTag(meta.getMainClass());
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name dependencies
         * @RawName <PluginTag.dependencies>
         * @Object PluginTag
         * @ReturnType ListTag(ElementTag)
         * @NoArg
         * @Async
         * @Description
         * Returns the names of the plugins this one hard depends on, the ones that must be there
         * for it to load at all. These are names, not PluginTags, because a dependency may well be
         * a plugin that is not installed. Feed a name to <@link tag plugin> to get the tag.
         *
         * @Usage
         * // Warn about a hard dependency that is not running.
         * - foreach <plugin[Corex].dependencies> as:dependency:
         *   - if !<plugin[<[dependency]>].isEnabled.ifNull[false]>:
         *     - narrate "<[dependency]> is missing."
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "dependencies", (attr, obj) -> {
            PluginMeta meta = obj.getMeta();
            return meta == null ? null : toList(meta.getPluginDependencies());
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name softDependencies
         * @RawName <PluginTag.softDependencies>
         * @Object PluginTag
         * @ReturnType ListTag(ElementTag)
         * @NoArg
         * @Async
         * @Description
         * Returns the names of the plugins this one loads after when they are present, but runs
         * without. Most of them are usually not installed.
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "softDependencies", (attr, obj) -> {
            PluginMeta meta = obj.getMeta();
            return meta == null ? null : toList(meta.getPluginSoftDependencies());
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name loadBefore
         * @RawName <PluginTag.loadBefore>
         * @Object PluginTag
         * @ReturnType ListTag(ElementTag)
         * @NoArg
         * @Async
         * @Description
         * Returns the names of the plugins this one asks to be loaded before, a soft dependency
         * declared from the other side.
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "loadBefore", (attr, obj) -> {
            PluginMeta meta = obj.getMeta();
            return meta == null ? null : toList(meta.getLoadBeforePlugins());
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name provides
         * @RawName <PluginTag.provides>
         * @Object PluginTag
         * @ReturnType ListTag(ElementTag)
         * @NoArg
         * @Async
         * @Description
         * Returns the names this plugin claims to provide, which other plugins may depend on as if
         * they were real plugins. A fork usually provides the name of the plugin it replaces.
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "provides", (attr, obj) -> {
            PluginMeta meta = obj.getMeta();
            return meta == null ? null : toList(meta.getProvidedPlugins());
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name loadOrder
         * @RawName <PluginTag.loadOrder>
         * @Object PluginTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns when the plugin is enabled, either 'startup' (before the worlds load) or
         * 'postworld' (after them, which is the default).
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "loadOrder", (attr, obj) -> {
            PluginMeta meta = obj.getMeta();
            return meta == null ? null : new ElementTag(meta.getLoadOrder().name().toLowerCase());
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name permissions
         * @RawName <PluginTag.permissions>
         * @Object PluginTag
         * @ReturnType ListTag(ElementTag)
         * @NoArg
         * @Async
         * @Description
         * Returns the permission nodes the plugin declares in its plugin.yml, such as
         * 'corex.reload'. Nodes a plugin registers in code at runtime are not in here.
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "permissions", (attr, obj) -> {
            PluginMeta meta = obj.getMeta();
            if (meta == null) return null;
            ListTag list = new ListTag();
            for (Permission permission : meta.getPermissions()) list.addString(permission.getName());
            return list;
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name dataFolder
         * @RawName <PluginTag.dataFolder>
         * @Object PluginTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the path of the plugin's own folder, normally 'plugins/TheName'. The folder does
         * not have to exist, a plugin that never writes anything never creates it.
         *
         * @Usage
         * // List the files another plugin keeps.
         * - narrate <server.listFiles[<plugin[Corex].dataFolder>]>
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "dataFolder", (attr, obj) -> {
            Plugin plugin = obj.getPlugin();
            if (plugin == null) return null;
            return new ElementTag(plugin.getDataFolder().getPath().replace('\\', '/'));
        }).setAsyncSafe();

        /* @doc mechanism
         *
         * @Name enabled
         * @Object PluginTag
         * @Input ElementTag(Boolean)
         * @Description
         * Enables or disables the plugin at runtime, the same thing a plugin manager does.
         * Setting it to the state the plugin is already in does nothing.
         *
         * @Warning
         * Disabling a running plugin is not the clean shutdown it looks like. Its own disable logic
         * runs, but plugins depending on it stay enabled and keep calling into it, and whatever it
         * registered outside the event system (packet listeners, tasks held by other plugins,
         * static state) stays behind. Enabling it again does not re-run the server's load phase
         * either. Restart the server instead unless you know the plugin tolerates this.
         *
         * @Usage
         * // Turn a plugin off.
         * - adjust <plugin[SomePlugin]> enabled:false
         */
        MECHANISM_PROCESSOR.registerMechanism("enabled", (obj, val) -> {
            Plugin plugin = obj.getPlugin();
            if (plugin == null || !(val instanceof ElementTag element) || !element.isBoolean()) {
                return obj;
            }

            boolean shouldEnable = element.asBoolean();
            if (shouldEnable == plugin.isEnabled()) {
                return obj;
            }

            if (shouldEnable) {
                Bukkit.getPluginManager().enablePlugin(plugin);
            }
            else {
                Bukkit.getPluginManager().disablePlugin(plugin);
            }

            return obj;
        });

        /* @doc mechanism
         *
         * @Name reloadConfig
         * @Object PluginTag
         * @Description
         * Makes the plugin re-read its config.yml from disk, the call most '/plugin reload' commands
         * make. It only refreshes the plugin's config object: values it copied into its own fields
         * at startup keep the old data, so how much this changes is up to the plugin.
         *
         * @Usage
         * // Re-read a config file a script just edited.
         * - adjust <plugin[Corex]> reloadConfig
         */
        MECHANISM_PROCESSOR.registerMechanism("reloadConfig", (obj, val) -> {
            Plugin plugin = obj.getPlugin();
            if (plugin != null) {
                plugin.reloadConfig();
            }
            return obj;
        });

        /* @doc mechanism
         *
         * @Name saveConfig
         * @Object PluginTag
         * @Description
         * Writes the plugin's config back to its config.yml. What lands on disk is the config the
         * plugin holds in memory right now, so anything it changed at runtime is saved with it, and
         * comments in the file are lost the way they always are with Bukkit configs.
         *
         * @Usage
         * // Persist whatever the plugin currently has loaded.
         * - adjust <plugin[Corex]> saveConfig
         */
        MECHANISM_PROCESSOR.registerMechanism("saveConfig", (obj, val) -> {
            Plugin plugin = obj.getPlugin();
            if (plugin != null) {
                plugin.saveConfig();
            }
            return obj;
        });

        /* @doc mechanism
         *
         * @Name saveDefaultConfig
         * @Object PluginTag
         * @Description
         * Copies the config.yml packed in the plugin's jar into its data folder, creating the folder
         * if it is not there. Does nothing when the file already exists, so it never overwrites a
         * config an admin edited. A plugin that ships no config.yml is left alone.
         *
         * @Usage
         * // Make sure a fresh install has its config on disk.
         * - adjust <plugin[Corex]> saveDefaultConfig
         */
        MECHANISM_PROCESSOR.registerMechanism("saveDefaultConfig", (obj, val) -> {
            Plugin plugin = obj.getPlugin();
            if (plugin != null) {
                plugin.saveDefaultConfig();
            }
            return obj;
        });

        /* @doc mechanism
         *
         * @Name saveResource
         * @Object PluginTag
         * @Input ElementTag
         * @Description
         * Extracts a file packed in the plugin's jar into its data folder, keeping the path it has
         * in the jar ('lang/en.yml' lands in 'plugins/TheName/lang/en.yml'). Use forward slashes.
         * An existing file is never overwritten, so this cannot reset a file back to its default.
         * A path the jar does not contain logs an error and changes nothing.
         *
         * @Usage
         * // Lay down a language file the plugin ships but does not extract itself.
         * - adjust <plugin[SomePlugin]> saveResource:lang/en.yml
         */
        MECHANISM_PROCESSOR.registerMechanism("saveResource", (obj, val) -> {
            Plugin plugin = obj.getPlugin();
            if (plugin == null || !(val instanceof ElementTag element)) {
                return obj;
            }

            String path = element.asString();
            if (path.isBlank()) {
                Debugger.error("saveResource: no resource path given for plugin '" + plugin.getName() + "'.");
                return obj;
            }

            try {
                plugin.saveResource(path, false);
            }
            catch (IllegalArgumentException exception) {
                Debugger.error("saveResource: '" + path + "' is not in the jar of plugin '"
                        + plugin.getName() + "'.");
            }

            return obj;
        });
    }

    private static ListTag toList(@Nullable List<String> values) {
        ListTag list = new ListTag();
        if (values == null) return list;
        for (String value : values) list.addString(value);
        return list;
    }

    public @Nullable Plugin getPlugin() {
        if (name.isEmpty()) return null;

        Plugin exact = Bukkit.getPluginManager().getPlugin(name);
        if (exact != null) return exact;

        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (plugin.getName().equalsIgnoreCase(name)) return plugin;
        }

        return null;
    }

    private @Nullable PluginMeta getMeta() {
        Plugin plugin = getPlugin();
        return plugin == null ? null : plugin.getPluginMeta();
    }

    @Override
    public @NonNull String getPrefix() {
        return PREFIX;
    }

    @Override
    public @NonNull String identify() {
        return PREFIX + "@" + name;
    }

    @Override
    public AbstractTag getAttribute(@NonNull Attribute attribute) {
        return TAG_PROCESSOR.process(this, attribute);
    }

    @Override
    public @NonNull TagProcessor<PluginTag> getProcessor() {
        return TAG_PROCESSOR;
    }

    @Override
    public @NonNull String getTestValue() {
        return "plugin@Corex";
    }

    @Override
    public @NonNull Adjustable duplicate() {
        return new PluginTag(name);
    }

    @Override
    public @NonNull AbstractTag applyMechanism(@NonNull String mechanism, @NonNull AbstractTag value) {
        return MECHANISM_PROCESSOR.process(this, mechanism, value);
    }

    @Override
    public @NonNull MechanismProcessor<? extends AbstractTag> getMechanismProcessor() {
        return MECHANISM_PROCESSOR;
    }
}
