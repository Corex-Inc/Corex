package dev.corexinc.corex.environment.containers.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.queue.ScriptQueue;
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class CommandManager {

    public static final CommandManager INSTANCE = new CommandManager();

    // Allowed-check results are stable for the duration of a player session.
    // 5 s avoids hammering the script engine on every tab press / command dispatch.
    private static final long ALLOWED_CACHE_TTL_MS = 5_000L;

    // Tab-complete scripts run on every keystroke. Cache the suggestion list for a
    // short window so only the *filter* step (startsWith) repeats, not the queue.
    private static final long TAB_CACHE_TTL_MS = 250L;

    private volatile Collection<CommandContainer> containers = List.of();
    private final Map<String, CachedAllowedEntry> allowedCache = new ConcurrentHashMap<>();
    private final Map<String, CachedTabEntry>     tabCache     = new ConcurrentHashMap<>();

    private CommandManager() {}

    public void syncAll(@NonNull Commands registrar) {
        NMSHandler.get().get(CommandAdapter.class);
        for (CommandContainer container : containers) {
            try {
                registerCommand(registrar, container);
            } catch (Exception e) {
                Debugger.error("Failed to register command '" + container.getName() + "': " + e.getMessage());
            }
        }
    }

    public void updateContainers(@NonNull Collection<CommandContainer> newContainers) {
        this.containers = List.copyOf(newContainers);
        allowedCache.clear();
        tabCache.clear();
    }

    private void registerCommand(@NonNull Commands registrar, @NonNull CommandContainer container) {
        LiteralArgumentBuilder<CommandSourceStack> root =
                Commands.literal(container.getName())
                        .requires(source -> checkAllowed(source, container));

        List<CommandArgumentSpec> specs = container.getArgumentSpecs();

        if (specs.isEmpty()) {
            root.executes(ctx -> executeContainer(ctx, container));
            root.then(buildCatchAllArgument(container));
        } else {
            if (specs.getFirst().optional()) {
                root.executes(ctx -> executeContainer(ctx, container));
            }
            root.then(buildArgumentChain(specs, 0, container));
        }

        registrar.register(root.build(), container.getDescription(), container.getAliases());
    }

    private ArgumentBuilder<CommandSourceStack, ?> buildArgumentChain(
            @NonNull List<CommandArgumentSpec> specs,
            int index,
            @NonNull CommandContainer container) {

        CommandArgumentSpec spec  = specs.get(index);
        ArgumentTypeRegistry.Entry entry = ArgumentTypeRegistry.get(spec.typeName());

        if (entry == null) throw new IllegalStateException(
                "Unknown argument type '" + spec.typeName() + "' in command '" +
                        container.getName() + "' argument '" + spec.name() + "'");

        RequiredArgumentBuilder<CommandSourceStack, ?> node =
                Commands.argument(spec.name(), entry.adapter().buildType(spec));

        boolean isLast         = index == specs.size() - 1;
        boolean nextIsOptional = !isLast && specs.get(index + 1).optional();

        if (isLast || nextIsOptional) {
            node.executes(ctx -> executeContainer(ctx, container));
        }

        if (container.hasSection(CommandContainer.SECTION_TAB_COMPLETE)) {
            node.suggests((ctx, builder) -> buildTabCompletions(ctx, builder, container));
        }

        if (!isLast) {
            node.then(buildArgumentChain(specs, index + 1, container));
        }

        return node;
    }

    private @NonNull RequiredArgumentBuilder<CommandSourceStack, String> buildCatchAllArgument(
            @NonNull CommandContainer container) {

        RequiredArgumentBuilder<CommandSourceStack, String> node =
                Commands.argument("...", StringArgumentType.greedyString())
                        .executes(ctx -> executeContainer(ctx, container));

        if (container.hasSection(CommandContainer.SECTION_TAB_COMPLETE)) {
            node.suggests((ctx, builder) -> buildTabCompletions(ctx, builder, container));
        }

        return node;
    }

    private int executeContainer(
            @NonNull CommandContext<CommandSourceStack> ctx,
            @NonNull CommandContainer container) {

        CommandContainer live = resolveContainer(container.getName());
        if (live == null) live = container;

        if (!checkAllowed(ctx.getSource(), live)) return Command.SINGLE_SUCCESS;

        if (!live.hasSection(CommandContainer.SECTION_SCRIPT)) return Command.SINGLE_SUCCESS;

        String rawArgs = extractRawArgs(ctx.getInput());

        ContextTag context = new ContextTag()
                .put("sender",  resolveSender(ctx.getSource().getSender()))
                .put("alias",   new ElementTag(resolveAlias(ctx.getInput(), live)))
                .put("aliases", buildAliasesList(live))
                .put("rawArgs", new ElementTag(rawArgs))
                .put("args",    live.getArgumentSpecs().isEmpty()
                        ? parseRawArgs(rawArgs)
                        : resolveBrigadierArgs(ctx, live));

        ScriptQueue queue = live.createQueue(CommandContainer.SECTION_SCRIPT, context);
        if (queue == null) return Command.SINGLE_SUCCESS;

        queue.start();
        return Command.SINGLE_SUCCESS;
    }

    private @NonNull MapTag resolveBrigadierArgs(
            @NonNull CommandContext<CommandSourceStack> ctx,
            @NonNull CommandContainer container) {

        MapTag args = new MapTag();
        for (CommandArgumentSpec spec : container.getArgumentSpecs()) {
            ArgumentTypeRegistry.Entry entry = ArgumentTypeRegistry.get(spec.typeName());
            if (entry == null) continue;
            try {
                AbstractTag value = entry.adapter().resolveValue(ctx, spec);
                if (value != null) args.putObject(spec.name(), value);
            } catch (IllegalArgumentException ignored) {
            }
        }
        return args;
    }

    private @NonNull CompletableFuture<Suggestions> buildTabCompletions(
            @NonNull CommandContext<CommandSourceStack> ctx,
            @NonNull SuggestionsBuilder builder,
            @NonNull CommandContainer container) {

        CommandContainer live = resolveContainer(container.getName());
        if (live == null) live = container;
        if (!live.hasSection(CommandContainer.SECTION_TAB_COMPLETE)) return builder.buildFuture();

        String fullInput  = builder.getInput();
        String remaining  = builder.getRemaining();

        int    lastSpace  = remaining.lastIndexOf(' ');
        String partial    = (lastSpace < 0 ? remaining : remaining.substring(lastSpace + 1)).toLowerCase();

        List<AbstractTag> suggestions = cachedTabSuggestions(
                ctx.getSource().getSender(), fullInput, remaining, lastSpace, live);

        int tokenStart = lastSpace < 0 ? builder.getStart() : builder.getStart() + lastSpace + 1;
        SuggestionsBuilder tokenBuilder = builder.createOffset(tokenStart);

        for (AbstractTag item : suggestions) {
            String text = item.identify();
            if (text.toLowerCase().startsWith(partial)) tokenBuilder.suggest(text);
        }

        return tokenBuilder.buildFuture();
    }

    public CompletableFuture<Suggestions> buildTabCompletionsFromNms(
            @NonNull CommandSender sender,
            @NonNull SuggestionsBuilder builder,
            @NonNull CommandContainer container) {

        CommandContainer live = resolveContainer(container.getName());
        if (live == null) live = container;
        if (!live.hasSection(CommandContainer.SECTION_TAB_COMPLETE)) return builder.buildFuture();

        String fullInput = builder.getInput();
        String remaining = builder.getRemaining();
        int    lastSpace = remaining.lastIndexOf(' ');
        String partial   = (lastSpace < 0 ? remaining : remaining.substring(lastSpace + 1)).toLowerCase();

        int tokenStart = lastSpace < 0 ? builder.getStart() : builder.getStart() + lastSpace + 1;
        SuggestionsBuilder tokenBuilder = builder.createOffset(tokenStart);

        List<AbstractTag> suggestions = cachedTabSuggestions(sender, fullInput, remaining, lastSpace, live);

        for (AbstractTag item : suggestions) {
            String text = item.identify();
            if (text.toLowerCase().startsWith(partial)) tokenBuilder.suggest(text);
        }

        return tokenBuilder.buildFuture();
    }

    private @NonNull List<AbstractTag> cachedTabSuggestions(
            @NonNull CommandSender sender,
            @NonNull String fullInput,
            @NonNull String remaining,
            int lastSpaceInRemaining,
            @NonNull CommandContainer container) {

        int remStart = fullInput.length() - remaining.length();
        String inputPrefix = lastSpaceInRemaining < 0
                ? fullInput.substring(0, remStart)
                : fullInput.substring(0, remStart + lastSpaceInRemaining + 1);

        String cacheKey = container.getName() + '\0' + senderKey(sender) + '\0' + inputPrefix;
        CachedTabEntry cached = tabCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) return cached.items;

        List<AbstractTag> fresh = runTabScript(sender, fullInput, container);
        tabCache.put(cacheKey, new CachedTabEntry(fresh));
        return fresh;
    }

    private @NonNull List<AbstractTag> runTabScript(
            @NonNull CommandSender sender,
            @NonNull String fullInput,
            @NonNull CommandContainer container) {

        ContextTag context = new ContextTag()
                .put("sender",  resolveSender(sender))
                .put("alias",   new ElementTag(resolveAlias(fullInput, container)))
                .put("aliases", buildAliasesList(container))
                .put("args",    parseCompletedArgs(fullInput))
                .put("input",   new ElementTag(fullInput));

        ScriptQueue queue = container.createQueue(CommandContainer.SECTION_TAB_COMPLETE, context);
        if (queue == null) return List.of();

        queue.start();

        List<AbstractTag> returns = queue.getReturns();
        if (returns.isEmpty()) return List.of();

        AbstractTag first = returns.getFirst();
        return first instanceof ListTag listTag ? listTag.getList() : List.of();
    }

    private boolean checkAllowed(@NonNull CommandSourceStack source, @NonNull CommandContainer container) {
        if (!container.hasSection(CommandContainer.SECTION_ALLOWED)) return true;

        CommandContainer live = resolveContainer(container.getName());
        if (live == null) live = container;

        String cacheKey = live.getName() + ':' + senderKey(source.getSender());
        CachedAllowedEntry cached = allowedCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) return cached.allowed;

        boolean allowed = runAllowedScript(source.getSender(), live);
        allowedCache.put(cacheKey, new CachedAllowedEntry(allowed));
        return allowed;
    }

    public boolean checkAllowedFromNms(@NonNull CommandSender sender, @NonNull CommandContainer container) {
        if (!container.hasSection(CommandContainer.SECTION_ALLOWED)) return true;

        CommandContainer live = resolveContainer(container.getName());
        if (live == null) live = container;

        String cacheKey = live.getName() + ':' + senderKey(sender);
        CachedAllowedEntry cached = allowedCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) return cached.allowed;

        boolean allowed = runAllowedScript(sender, live);
        allowedCache.put(cacheKey, new CachedAllowedEntry(allowed));
        return allowed;
    }

    private boolean runAllowedScript(@NonNull CommandSender sender, @NonNull CommandContainer container) {
        ContextTag context = new ContextTag()
                .put("sender",  resolveSender(sender))
                .put("alias",   new ElementTag(container.getName()))
                .put("aliases", buildAliasesList(container));

        ScriptQueue queue = container.createQueue(CommandContainer.SECTION_ALLOWED, context);
        if (queue == null) return true;

        queue.start();

        List<AbstractTag> returns = queue.getReturns();
        if (returns.isEmpty()) return false;
        return returns.getFirst().identify().equalsIgnoreCase("true");
    }

    public void executeFromNms(
            @NonNull CommandSender sender,
            @NonNull String input,
            @NonNull MapTag args,
            @NonNull CommandContainer container) {

        CommandContainer live = resolveContainer(container.getName());
        if (live == null) live = container;
        if (!live.hasSection(CommandContainer.SECTION_SCRIPT)) return;

        String rawArgs = extractRawArgs(input);
        ContextTag context = new ContextTag()
                .put("sender",  resolveSender(sender))
                .put("alias",   new ElementTag(resolveAlias(input, live)))
                .put("aliases", buildAliasesList(live))
                .put("rawArgs", new ElementTag(rawArgs))
                .put("args",    args.isEmpty() ? parseRawArgs(rawArgs) : args);

        ScriptQueue queue = live.createQueue(CommandContainer.SECTION_SCRIPT, context);
        if (queue == null) return;
        queue.start();
    }

    public void injectNew(@NonNull CommandContainer container) {
        CommandAdapter adapter = NMSHandler.get().get(CommandAdapter.class);
        if (adapter == null) {
            Debugger.error("CommandAdapter not available - cannot inject '" + container.getName() + "' at runtime");
            return;
        }
        adapter.injectCommand(container);
        adapter.syncCommandTree();
    }

    public void reinjectAll(@NonNull Collection<CommandContainer> toInject) {
        CommandAdapter adapter = NMSHandler.get().get(CommandAdapter.class);
        if (adapter == null) {
            Debugger.error("CommandAdapter not available - cannot re-inject commands at runtime");
            return;
        }
        for (CommandContainer container : toInject) {
            try {
                adapter.injectCommand(container);
            } catch (Exception e) {
                Debugger.error("Failed to re-inject command '" + container.getName() + "': " + e.getMessage());
            }
        }
        adapter.syncCommandTree();
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

    private @NonNull ListTag parseRawArgs(@NonNull String rawArgs) {
        ListTag result = new ListTag();
        if (rawArgs.isBlank()) return result;
        for (String token : rawArgs.split(" ")) {
            if (!token.isEmpty()) result.addString(token);
        }
        return result;
    }

    private @NonNull ListTag parseCompletedArgs(@NonNull String input) {
        ListTag result = new ListTag();
        int firstSpace = input.indexOf(' ');
        if (firstSpace < 0) return result;
        String[] tokens = input.substring(firstSpace + 1).split(" ", -1);
        for (int i = 0; i < tokens.length - 1; i++) {
            if (!tokens[i].isEmpty()) result.addString(tokens[i]);
        }
        return result;
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