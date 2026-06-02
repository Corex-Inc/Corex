package dev.corexinc.corex.environment.containers.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.concurrent.CompletableFuture;

public final class CommandTreeBuilder {

    public interface Platform<S> {
        ArgumentType<?> argumentType(CommandArgumentSpec spec);
        boolean requires(S source, CommandContainer container, CommandNode node);
        int execute(CommandContext<S> ctx, CommandContainer container, CommandNode node);
        CompletableFuture<Suggestions> suggest(CommandContext<S> ctx, SuggestionsBuilder builder,
                                               CommandContainer container, CommandNode node);
    }

    private CommandTreeBuilder() {}

    public static <S> LiteralArgumentBuilder<S> build(CommandContainer container, Platform<S> platform) {
        LiteralArgumentBuilder<S> root = LiteralArgumentBuilder.literal(container.getName());
        populate(root, container, container.getTree(), platform);
        return root;
    }

    private static <S> void populate(ArgumentBuilder<S, ?> builder, CommandContainer container,
                                     CommandNode node, Platform<S> platform) {
        if (node.hasRequires()) {
            builder.requires(src -> platform.requires(src, container, node));
        }
        if (node.hasScript()) {
            builder.executes(ctx -> platform.execute(ctx, container, node));
        }
        for (CommandNode child : node.children()) {
            com.mojang.brigadier.tree.CommandNode<S> built = buildChild(container, child, platform);
            builder.then(built);
            if (child.isLiteral()) {
                for (String alias : child.aliases()) {
                    builder.then(LiteralArgumentBuilder.<S>literal(alias).redirect(built).build());
                }
            }
        }
    }

    private static <S> com.mojang.brigadier.tree.CommandNode<S> buildChild(
            CommandContainer container, CommandNode child, Platform<S> platform) {

        ArgumentBuilder<S, ?> builder;
        if (child.isArgument()) {
            RequiredArgumentBuilder<S, ?> arg = argument(child.name(), platform.argumentType(child.spec()));
            if (child.hasSuggestions()) {
                arg.suggests((ctx, sb) -> platform.suggest(ctx, sb, container, child));
            }
            builder = arg;
        } else {
            builder = LiteralArgumentBuilder.<S>literal(child.name());
        }
        populate(builder, container, child, platform);
        return builder.build();
    }

    private static <S, T> RequiredArgumentBuilder<S, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }
}
