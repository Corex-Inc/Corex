package dev.corexinc.corex.velocity.environment.tags.core;

import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.meta.PluginDependency;
import com.velocitypowered.api.proxy.ProxyServer;
import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.velocity.CorexVelocity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;


/* @doc object
 *
 * @Name PluginTag
 * @Prefix plugin
 * @Modules VELOCITY
 * @IdentifyFormat plugin@corex
 *
 * @Format
 * The identity format for plugins is the plugin id from its velocity-plugin.json, which is always
 * lowercase: `plugin@corex`. The display name works too, so `<plugin[Corex]>` and `<plugin[corex]>`
 * both resolve, and either way the tag identifies itself by id.
 *
 * @Description
 * A PluginTag is one plugin loaded on the proxy and what its velocity-plugin.json says about it.
 * This is the proxy counterpart of the PluginTag on the Paper plugin, and it is deliberately
 * smaller: Velocity plugins cannot be enabled or disabled at runtime and have no config API the
 * proxy knows about, so this tag reads and never writes. It has no mechanisms.
 *
 * A PluginTag only ever exists for a plugin the proxy actually loaded. A name nothing matches gives
 * null, so `<plugin[NotHere].exists>` returns 'false' and `.ifNull[...]` catches it.
 *
 * Plugins on the backend servers are invisible from here. The proxy knows its own plugins only.
 *
 * @Usage
 * // Report the running Corex version.
 * - narrate "Corex <plugin[corex].version>"
 */
public class PluginTag implements AbstractTag {

    private static final String PREFIX = "plugin";

    public static final TagProcessor<PluginTag> TAG_PROCESSOR = new TagProcessor<>();

    private final String id;

    public PluginTag(@NonNull PluginContainer container) {
        this.id = container.getDescription().getId();
    }

    public PluginTag(@Nullable String raw) {
        if (raw == null) {
            this.id = "";
            return;
        }

        String stripped = raw.trim();
        if (stripped.toLowerCase().startsWith(PREFIX + "@")) {
            stripped = stripped.substring(PREFIX.length() + 1);
        }

        this.id = stripped;
    }

    public static void register() {
        BaseTagProcessor.registerBaseTag(PREFIX, attribute -> {
            if (!attribute.hasParam()) return null;
            PluginTag tag = new PluginTag(attribute.getParam());
            return tag.getContainer() != null ? tag : null;
        });

        ObjectFetcher.registerFetcher(PREFIX, raw -> {
            PluginTag tag = new PluginTag(raw);
            return tag.getContainer() != null ? tag : null;
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
         * Returns the plugin id, the lowercase handle from velocity-plugin.json that the proxy
         * looks plugins up by, such as 'corex'. This is what dependencies name.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "id", (attribute, object) -> {
            PluginDescription description = object.getDescription();
            return description == null ? null : new ElementTag(description.getId());
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
         * Returns the display name, the one written with real capitalisation. A plugin does not
         * have to declare one, in which case this falls back to the id.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "name", (attribute, object) -> {
            PluginDescription description = object.getDescription();
            if (description == null) return null;
            return new ElementTag(description.getName().orElse(description.getId()));
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
         * Always returns 'true'. The proxy has no way to disable a plugin once it is loaded, and a
         * plugin it never loaded gives no tag at all, so there is no state where this is 'false'.
         * It exists so a script asking whether a plugin is up reads the same here and on the Paper
         * plugin, where it means something.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "isEnabled", (attribute, object) ->
                new ElementTag(object.getContainer() != null)).setAsyncSafe();

        /* @doc tag
         *
         * @Name version
         * @RawName <PluginTag.version>
         * @Object PluginTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the version string the plugin declares, such as '1.4.2'. It is whatever the author
         * wrote there, so do not count on it being a number you can compare. Returns null when the
         * plugin declares no version.
         *
         * @Usage
         * // Print the version of every plugin on the proxy.
         * - foreach <proxy.plugins> as:entry:
         *   - narrate "<[entry].name> v<[entry].version.ifNull[?]>"
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "version", (attribute, object) ->
                object.read(PluginDescription::getVersion)).setAsyncSafe();

        /* @doc tag
         *
         * @Name description
         * @RawName <PluginTag.description>
         * @Object PluginTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the plugin's description line, or null when it declares none.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "description", (attribute, object) ->
                object.read(PluginDescription::getDescription)).setAsyncSafe();

        /* @doc tag
         *
         * @Name website
         * @RawName <PluginTag.website>
         * @Object PluginTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the project link, the 'url' field of velocity-plugin.json. Most plugins set none,
         * in which case this returns null. Named after the plugin.yml field so the tag reads the
         * same on the Paper plugin.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "website", (attribute, object) ->
                object.read(PluginDescription::getUrl)).setAsyncSafe();

        /* @doc tag
         *
         * @Name authors
         * @RawName <PluginTag.authors>
         * @Object PluginTag
         * @ReturnType ListTag(ElementTag)
         * @NoArg
         * @Async
         * @Description
         * Returns the authors the plugin declares, in the order they were written.
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "authors", (attribute, object) -> {
            PluginDescription description = object.getDescription();
            if (description == null) return null;
            return toList(description.getAuthors());
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
         * Returns the ids of the plugins this one requires. The proxy refuses to load a plugin whose
         * required dependency is missing, so on a running proxy every id in here is loaded. Optional
         * dependencies are not in this list, see <@link tag PluginTag.softDependencies>.
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "dependencies", (attribute, object) ->
                object.dependencyIds(false)).setAsyncSafe();

        /* @doc tag
         *
         * @Name softDependencies
         * @RawName <PluginTag.softDependencies>
         * @Object PluginTag
         * @ReturnType ListTag(ElementTag)
         * @NoArg
         * @Async
         * @Description
         * Returns the ids of the plugins this one loads after when they are there, but runs without,
         * the ones marked 'optional' in velocity-plugin.json. Named after the plugin.yml field so
         * the tag reads the same on the Paper plugin.
         * These are ids, not PluginTags, because a soft dependency is often not installed. Feed an
         * id to <@link tag plugin> to get the tag.
         *
         * @Usage
         * // Report which optional hooks are actually present.
         * - foreach <plugin[corex].softDependencies> as:dependency:
         *   - if <plugin[<[dependency]>].exists>:
         *     - narrate "Hooked into <[dependency]>."
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "softDependencies", (attribute, object) ->
                object.dependencyIds(true)).setAsyncSafe();

        /* @doc tag
         *
         * @Name provides
         * @RawName <PluginTag.provides>
         * @Object PluginTag
         * @ReturnType ListTag(ElementTag)
         * @NoArg
         * @Async
         * @Description
         * Returns the ids this plugin claims to provide, which other plugins may depend on as if
         * they were real plugins. A fork usually provides the id of the plugin it replaces.
         */
        TAG_PROCESSOR.registerTag(ListTag.class, "provides", (attribute, object) -> {
            PluginDescription description = object.getDescription();
            if (description == null) return null;
            return toList(description.getProvidedIds());
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
         * Returns the path of the folder the proxy hands the plugin, which is 'plugins/' plus the
         * plugin id. The proxy builds that path from the id and injects it, so this is where the
         * plugin's files are unless it went out of its way to write somewhere else. The folder does
         * not have to exist, a plugin that never writes anything never creates it.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "dataFolder", (attribute, object) -> {
            PluginDescription description = object.getDescription();
            return description == null ? null : new ElementTag("plugins/" + description.getId());
        }).setAsyncSafe();

        /* @doc tag
         *
         * @Name source
         * @RawName <PluginTag.source>
         * @Object PluginTag
         * @ReturnType ElementTag
         * @NoArg
         * @Async
         * @Description
         * Returns the path of the jar the plugin was loaded from, normally 'plugins/TheName.jar'.
         * Returns null for a plugin that came from somewhere other than a file, which on a normal
         * proxy means the built-in ones.
         */
        TAG_PROCESSOR.registerTag(ElementTag.class, "source", (attribute, object) -> {
            PluginDescription description = object.getDescription();
            if (description == null) return null;
            Optional<Path> source = description.getSource();
            return source.map(path -> new ElementTag(path.toString().replace('\\', '/'))).orElse(null);
        }).setAsyncSafe();
    }

    private static ProxyServer proxy() {
        return CorexVelocity.getInstance().getServer();
    }

    private static ListTag toList(@Nullable Collection<String> values) {
        ListTag list = new ListTag();
        if (values == null) return list;
        for (String value : values) list.addString(value);
        return list;
    }

    private @Nullable ElementTag read(Function<PluginDescription, Optional<String>> reader) {
        PluginDescription description = getDescription();
        if (description == null) return null;
        Optional<String> value = reader.apply(description);
        return value.map(ElementTag::new).orElse(null);
    }

    private @Nullable ListTag dependencyIds(boolean optional) {
        PluginDescription description = getDescription();
        if (description == null) return null;

        ListTag list = new ListTag();
        for (PluginDependency dependency : description.getDependencies()) {
            if (dependency.isOptional() == optional) list.addString(dependency.getId());
        }
        return list;
    }

    public @Nullable PluginContainer getContainer() {
        if (id.isEmpty()) return null;

        Optional<PluginContainer> exact = proxy().getPluginManager().getPlugin(id);
        if (exact.isPresent()) return exact.get();

        for (PluginContainer container : proxy().getPluginManager().getPlugins()) {
            PluginDescription description = container.getDescription();
            if (description.getId().equalsIgnoreCase(id)) return container;
            if (description.getName().filter(name -> name.equalsIgnoreCase(id)).isPresent()) return container;
        }

        return null;
    }

    private @Nullable PluginDescription getDescription() {
        PluginContainer container = getContainer();
        return container == null ? null : container.getDescription();
    }

    @Override
    public @NonNull String getPrefix() {
        return PREFIX;
    }

    @Override
    public @NonNull String identify() {
        PluginContainer container = getContainer();
        return PREFIX + "@" + (container != null ? container.getDescription().getId() : id);
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
        return "plugin@corex";
    }
}
