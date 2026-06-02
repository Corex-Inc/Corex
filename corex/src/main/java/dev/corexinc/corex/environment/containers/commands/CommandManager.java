package dev.corexinc.corex.environment.containers.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.PlayerIdentity;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ContextTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.utils.adapters.CommandAdapter;
import dev.corexinc.corex.environment.utils.nms.NMSHandler;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("UnstableApiUsage")
public final class CommandManager {

    public static final CommandManager INSTANCE = new CommandManager();

    private static final long ALLOWED_CACHE_TTL_MS = 5_000L;
    private static final long TAB_CACHE_TTL_MS = 250L;

    private volatile Collection<CommandContainer> containers = List.of();
    private volatile Set<String> registeredNames = Set.of();
    private final Map<String, CachedAllowedEntry> allowedCache = new ConcurrentHashMap<>();
    private final Map<String, CachedTabEntry>     tabCache     = new ConcurrentHashMap<>();

    private CommandManager() {}

    public void syncAll(@NonNull Commands registrar) {
        CommandAdapter adapter = NMSHandler.get().get(CommandAdapter.class);
        CommandTreeBuilder.Platform<CommandSourceStack> platform = new PaperPlatform();
        Set<String> oldNames = registeredNames;
        Set<String> newNames = new HashSet<>();

        for (CommandContainer container : containers) {
            if (!canRegister(container, oldNames, adapter)) continue;
            try {
                LiteralArgumentBuilder<CommandSourceStack> root = CommandTreeBuilder.build(container, platform);
                registrar.register(root.build(), container.getDescription(), container.getAliases());
                newNames.addAll(container.getAllAliases());
            } catch (Exception e) {
                Debugger.error("Failed to register command '" + container.getName() + "': " + e.getMessage());
            }
        }
        registeredNames = newNames;
    }

    public void updateContainers(@NonNull Collection<CommandContainer> newContainers) {
        this.containers = List.copyOf(newContainers);
        allowedCache.clear();
        tabCache.clear();
    }

    public void injectNew(@NonNull CommandContainer container) {
        CommandAdapter adapter = NMSHandler.get().get(CommandAdapter.class);
        if (adapter == null) {
            Debugger.error("CommandAdapter not available - cannot inject '" + container.getName() + "' at runtime");
            return;
        }
        if (!canRegister(container, registeredNames, adapter)) return;

        adapter.injectCommand(container);

        Set<String> updated = new HashSet<>(registeredNames);
        updated.addAll(container.getAllAliases());
        registeredNames = updated;

        adapter.syncCommandTree();
    }

    public void reinjectAll(@NonNull Collection<CommandContainer> toInject) {
        CommandAdapter adapter = NMSHandler.get().get(CommandAdapter.class);
        if (adapter == null) {
            Debugger.error("CommandAdapter not available - cannot re-inject commands at runtime");
            return;
        }

        Set<String> oldNames = registeredNames;
        Set<String> newNames = new HashSet<>();
        List<CommandContainer> registrable = new ArrayList<>();

        for (CommandContainer container : toInject) {
            if (!canRegister(container, oldNames, adapter)) continue;
            registrable.add(container);
            newNames.addAll(container.getAllAliases());
        }

        for (String name : oldNames) {
            if (newNames.contains(name)) continue;
            try {
                adapter.removeCommand(name);
            } catch (Exception e) {
                Debugger.error("Failed to remove command '" + name + "': " + e.getMessage());
            }
        }

        for (CommandContainer container : registrable) {
            try {
                adapter.injectCommand(container);
            } catch (Exception e) {
                Debugger.error("Failed to re-inject command '" + container.getName() + "': " + e.getMessage());
            }
        }

        registeredNames = newNames;
        adapter.syncCommandTree();
    }

    private boolean canRegister(@NonNull CommandContainer container, @NonNull Set<String> ours, @Nullable CommandAdapter adapter) {
        if (container.isOverride()) return true;
        if (ours.contains(container.getName())) return true;
        if (adapter != null && adapter.commandExists(container.getName())) {
            Debugger.error("Command '" + container.getName() + "' already exists - set 'override: true' to replace it");
            return false;
        }
        return true;
    }

    public boolean checkNode(@NonNull CommandSender sender, @NonNull CommandContainer container, @NonNull CommandNode node) {
        CommandContainer live = resolveContainer(container.getName());
        if (live == null) return false;

        if (!node.hasRequires()) return true;

        String cacheKey = live.getName() + ':' + node.basePath() + ':' + senderKey(sender);
        CachedAllowedEntry cached = allowedCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) return cached.allowed;

        boolean allowed = runRequiresScript(sender, live, node);
        allowedCache.put(cacheKey, new CachedAllowedEntry(allowed));
        return allowed;
    }

    public void dispatch(@NonNull CommandSender sender, @NonNull String input,
                         @NonNull CommandContainer container, @NonNull CommandNode node,
                         @NonNull CommandArgResolver resolver) {
        if (!node.hasScript()) return;

        CommandContainer live = resolveContainer(container.getName());
        if (live == null) return;

        ContextTag context = baseContext(sender, live, input)
                .put("args", buildArgs(node.pathChain(), resolver))
                .put("path", pathList(node));

        ScriptQueue queue = live.createQueue(node.scriptPath(), playerIdentity(sender), context);
        if (queue != null) queue.start();
    }

    public @NonNull CompletableFuture<Suggestions> complete(@NonNull CommandSender sender,
                                                            @NonNull SuggestionsBuilder builder,
                                                            @NonNull CommandContainer container,
                                                            @NonNull CommandNode node,
                                                            @NonNull CommandArgResolver resolver) {
        if (!node.hasSuggestions()) return builder.buildFuture();

        CommandContainer live = resolveContainer(container.getName());
        if (live == null) return builder.buildFuture();

        String remaining = builder.getRemaining();
        String lower = remaining.toLowerCase();

        List<AbstractTag> suggestions = cachedSuggestions(sender, builder.getInput(), remaining, live, node, resolver);

        for (AbstractTag item : suggestions) {
            String text = item.identify();
            if (text.toLowerCase().startsWith(lower)) builder.suggest(text);
        }
        return builder.buildFuture();
    }

    private @NonNull MapTag buildArgs(@NonNull List<CommandNode> chain, @NonNull CommandArgResolver resolver) {
        MapTag args = new MapTag();
        for (CommandNode node : chain) {
            if (!node.isArgument()) continue;
            AbstractTag value = resolver.resolve(node.spec());
            if (value == null) continue;
            String key = node.hasChildren() ? node.argPath() + ".this" : node.argPath();
            args.putDeepObject(key, value);
        }
        return args;
    }

    private @NonNull List<AbstractTag> cachedSuggestions(@NonNull CommandSender sender,
                                                         @NonNull String fullInput,
                                                         @NonNull String remaining,
                                                         @NonNull CommandContainer container,
                                                         @NonNull CommandNode node,
                                                         @NonNull CommandArgResolver resolver) {
        String prefix = fullInput.substring(0, fullInput.length() - remaining.length());
        String cacheKey = node.basePath() + '\0' + senderKey(sender) + '\0' + prefix;
        CachedTabEntry cached = tabCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) return cached.items;

        List<AbstractTag> fresh = runSuggestScript(sender, fullInput, remaining, container, node, resolver);
        tabCache.put(cacheKey, new CachedTabEntry(fresh));
        return fresh;
    }

    private @NonNull List<AbstractTag> runSuggestScript(@NonNull CommandSender sender,
                                                        @NonNull String fullInput,
                                                        @NonNull String remaining,
                                                        @NonNull CommandContainer container,
                                                        @NonNull CommandNode node,
                                                        @NonNull CommandArgResolver resolver) {
        ContextTag context = baseContext(sender, container, fullInput)
                .put("args", buildArgs(node.ancestorChain(), resolver))
                .put("arg",  new ElementTag(remaining))
                .put("path", pathList(node));

        ScriptQueue queue = container.createQueue(node.suggestsPath(), playerIdentity(sender), context);
        if (queue == null) return List.of();
        queue.start();

        List<AbstractTag> returns = queue.getReturns();
        if (returns.isEmpty()) return List.of();
        AbstractTag first = returns.getFirst();
        return first instanceof ListTag list ? list.getList() : List.of();
    }

    private boolean runRequiresScript(@NonNull CommandSender sender, @NonNull CommandContainer container, @NonNull CommandNode node) {
        ContextTag context = baseContext(sender, container, container.getName())
                .put("path", pathList(node));

        ScriptQueue queue = container.createQueue(node.requiresPath(), playerIdentity(sender), context);
        if (queue == null) return true;
        queue.start();

        List<AbstractTag> returns = queue.getReturns();
        if (returns.isEmpty()) return false;
        return returns.getFirst().identify().equalsIgnoreCase("true");
    }

    private @NonNull ContextTag baseContext(@NonNull CommandSender sender, @NonNull CommandContainer container, @NonNull String input) {
        return new ContextTag()
                .put("sender",   resolveSender(sender))
                .put("label",    new ElementTag(container.getName()))
                .put("alias",    new ElementTag(resolveAlias(input, container)))
                .put("aliases",  buildAliasesList(container))
                .put("raw_args", new ElementTag(extractRawArgs(input)));
    }

    private @NonNull ListTag pathList(@NonNull CommandNode node) {
        ListTag list = new ListTag();
        for (CommandNode n : node.pathChain()) list.addString(n.name());
        return list;
    }

    private @Nullable PlayerIdentity playerIdentity(@NonNull CommandSender sender) {
        return sender instanceof Player player ? new PlayerTag(player) : null;
    }

    private @NonNull AbstractTag resolveSender(@NonNull CommandSender sender) {
        return switch (sender) {
            case Player player            -> new PlayerTag(player);
            case BlockCommandSender block -> new LocationTag(block.getBlock().getLocation());
            case Entity entity            -> new EntityTag(entity);
            default                       -> new ElementTag("CONSOLE");
        };
    }

    private @NonNull String resolveAlias(@NonNull String input, @NonNull CommandContainer container) {
        String firstToken = input.trim().split("\\s+")[0].replaceFirst("^/", "").toLowerCase();
        for (String alias : container.getAllAliases()) {
            if (alias.equalsIgnoreCase(firstToken)) return alias;
        }
        return container.getName();
    }

    private @NonNull String extractRawArgs(@NonNull String input) {
        int spaceIndex = input.indexOf(' ');
        return spaceIndex < 0 ? "" : input.substring(spaceIndex + 1).trim();
    }

    private @NonNull ListTag buildAliasesList(@NonNull CommandContainer container) {
        ListTag list = new ListTag();
        container.getAllAliases().forEach(list::addString);
        return list;
    }

    private @NonNull String senderKey(@NonNull CommandSender sender) {
        if (sender instanceof Player player) return player.getUniqueId().toString();
        return sender.getClass().getSimpleName();
    }

    private @Nullable CommandContainer resolveContainer(@NonNull String name) {
        for (CommandContainer container : containers) {
            if (container.getName().equalsIgnoreCase(name)) return container;
        }
        return null;
    }

    private final class PaperPlatform implements CommandTreeBuilder.Platform<CommandSourceStack> {

        @Override
        public ArgumentType<?> argumentType(CommandArgumentSpec spec) {
            ArgumentTypeRegistry.Entry entry = ArgumentTypeRegistry.get(spec.typeName());
            if (entry == null) throw new IllegalStateException("Unknown argument type '" + spec.typeName() + "'");
            return entry.adapter().buildType(spec);
        }

        @Override
        public boolean requires(CommandSourceStack source, CommandContainer container, CommandNode node) {
            return checkNode(source.getSender(), container, node);
        }

        @Override
        public int execute(CommandContext<CommandSourceStack> ctx, CommandContainer container, CommandNode node) {
            dispatch(ctx.getSource().getSender(), ctx.getInput(), container, node, paperResolver(ctx));
            return Command.SINGLE_SUCCESS;
        }

        @Override
        public CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder,
                                                       CommandContainer container, CommandNode node) {
            return complete(ctx.getSource().getSender(), builder, container, node, paperResolver(ctx));
        }

        private CommandArgResolver paperResolver(CommandContext<CommandSourceStack> ctx) {
            return spec -> {
                ArgumentTypeRegistry.Entry entry = ArgumentTypeRegistry.get(spec.typeName());
                return entry == null ? null : entry.adapter().resolveValue(ctx, spec);
            };
        }
    }

    private static final class CachedAllowedEntry {
        final boolean allowed;
        final long    expiresAt;

        CachedAllowedEntry(boolean allowed) {
            this.allowed   = allowed;
            this.expiresAt = System.currentTimeMillis() + ALLOWED_CACHE_TTL_MS;
        }

        boolean isExpired() { return System.currentTimeMillis() >= expiresAt; }
    }

    private static final class CachedTabEntry {
        final List<AbstractTag> items;
        final long              expiresAt;

        CachedTabEntry(@NonNull List<AbstractTag> items) {
            this.items     = items;
            this.expiresAt = System.currentTimeMillis() + TAB_CACHE_TTL_MS;
        }

        boolean isExpired() { return System.currentTimeMillis() >= expiresAt; }
    }
}
