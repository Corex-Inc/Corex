package dev.corexinc.corex.nms.v1_21_11;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.containers.commands.*;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import dev.corexinc.corex.environment.tags.entity.EntityTag;
import dev.corexinc.corex.environment.tags.player.PlayerTag;
import dev.corexinc.corex.environment.tags.world.ItemTag;
import dev.corexinc.corex.environment.tags.world.LocationTag;
import dev.corexinc.corex.environment.tags.world.WorldTag;
import dev.corexinc.corex.environment.utils.ReflectionHelper;
import dev.corexinc.corex.environment.utils.adapters.CommandAdapter;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.BlockPositionResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.FinePositionResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.EntitySelectorArgumentResolver;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.math.BlockPosition;
import io.papermc.paper.math.FinePosition;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.*;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;
import org.bukkit.*;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

@SuppressWarnings("UnstableApiUsage")
public class CommandAdapterImpl implements CommandAdapter {

    private static volatile boolean typesRegistered = false;

    private static final Map<
            String,
            BiFunction<CommandContext<net.minecraft.commands.CommandSourceStack>, CommandArgumentSpec, AbstractTag>
            > NMS_RESOLVERS = new ConcurrentHashMap<>();

    public CommandAdapterImpl() {
        registerArguments();
    }

    private static final class BuildContextHolder {
        static final CommandBuildContext VALUE = create();

        private static CommandBuildContext create() {
            MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
            return CommandBuildContext.simple(
                    server.registryAccess(),
                    server.getWorldData().enabledFeatures());
        }
    }

    private static CommandBuildContext buildContext() {
        return BuildContextHolder.VALUE;
    }

    private static synchronized void registerArguments() {
        if (typesRegistered) return;
        typesRegistered = true;

        registerNms("integer", Integer.class,
                spec -> IntegerArgumentType.integer(
                        spec.intOption("min", Integer.MIN_VALUE),
                        spec.intOption("max", Integer.MAX_VALUE)));

        registerNms("float", Float.class,
                spec -> FloatArgumentType.floatArg(
                        spec.floatOption("min", -Float.MAX_VALUE),
                        spec.floatOption("max", Float.MAX_VALUE)));

        registerNms("double", Double.class,
                spec -> DoubleArgumentType.doubleArg(
                        spec.doubleOption("min", -Double.MAX_VALUE),
                        spec.doubleOption("max", Double.MAX_VALUE)));

        registerNms("long", Long.class,
                spec -> LongArgumentType.longArg(
                        spec.longOption("min", Long.MIN_VALUE),
                        spec.longOption("max", Long.MAX_VALUE)));

        registerNms("boolean",      Boolean.class, spec -> BoolArgumentType.bool());
        registerNms("word",         String.class,  spec -> StringArgumentType.word());
        registerNms("string",       String.class,  spec -> StringArgumentType.string());
        registerNms("greedyString", String.class,  spec -> StringArgumentType.greedyString());

        registerPaper("player",
                spec -> EntityArgument.player(),
                spec -> ArgumentTypes.player(),
                (ctx, spec) -> {
                    PlayerSelectorArgumentResolver resolver =
                            ctx.getArgument(spec.name(), PlayerSelectorArgumentResolver.class);
                    try {
                        List<Player> players = resolver.resolve(ctx.getSource());
                        return players.isEmpty() ? null : new PlayerTag(players.getFirst());
                    } catch (CommandSyntaxException e) {
                        throw new RuntimeException(e);
                    }
                },
                (ctx, spec) -> safeGet(() ->
                {
                    try {
                        return new PlayerTag(EntityArgument.getPlayer(ctx, spec.name()).getBukkitEntity());
                    } catch (CommandSyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }));

        registerPaper("players",
                spec -> EntityArgument.players(),
                spec -> ArgumentTypes.players(),
                (ctx, spec) -> {
                    PlayerSelectorArgumentResolver resolver =
                            ctx.getArgument(spec.name(), PlayerSelectorArgumentResolver.class);
                    ListTag list = new ListTag();
                    try {
                        for (Player p : resolver.resolve(ctx.getSource())) list.addObject(new PlayerTag(p));
                    } catch (CommandSyntaxException e) {
                        throw new RuntimeException(e);
                    }
                    return list;
                },
                (ctx, spec) -> safeGet(() -> {
                    ListTag list = new ListTag();
                    try {
                        for (var p : EntityArgument.getPlayers(ctx, spec.name()))
                            list.addObject(new PlayerTag(p.getBukkitEntity()));
                    } catch (CommandSyntaxException e) {
                        throw new RuntimeException(e);
                    }
                    return list;
                }));

        registerPaper("entity",
                spec -> EntityArgument.entity(),
                spec -> ArgumentTypes.entity(),
                (ctx, spec) -> {
                    Collection<Entity> list = resolveEntitiesPaper(ctx, spec);
                    return list.isEmpty() ? null : new EntityTag(list.iterator().next());
                },
                (ctx, spec) -> safeGet(() ->
                {
                    try {
                        return new EntityTag(EntityArgument.getEntity(ctx, spec.name()).getBukkitEntity());
                    } catch (CommandSyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }));

        registerPaper("entities",
                spec -> EntityArgument.entities(),
                spec -> ArgumentTypes.entities(),
                (ctx, spec) -> {
                    ListTag list = new ListTag();
                    for (Entity e : resolveEntitiesPaper(ctx, spec)) list.addObject(new EntityTag(e));
                    return list;
                },
                (ctx, spec) -> safeGet(() -> {
                    ListTag list = new ListTag();
                    try {
                        for (var e : EntityArgument.getEntities(ctx, spec.name()))
                            list.addObject(new EntityTag(e.getBukkitEntity()));
                    } catch (CommandSyntaxException e) {
                        throw new RuntimeException(e);
                    }
                    return list;
                }));

        registerPaper("world",
                spec -> DimensionArgument.dimension(),
                spec -> ArgumentTypes.world(),
                (ctx, spec) -> new WorldTag(ctx.getArgument(spec.name(), World.class)),
                (ctx, spec) -> safeGet(() ->
                {
                    try {
                        return new WorldTag(DimensionArgument.getDimension(ctx, spec.name()).getWorld());
                    } catch (CommandSyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }));

        registerPaper("blockPosition",
                spec -> BlockPosArgument.blockPos(),
                spec -> ArgumentTypes.blockPosition(),
                (ctx, spec) -> {
                    try {
                        BlockPosition pos = ctx.getArgument(spec.name(), BlockPositionResolver.class)
                                .resolve(ctx.getSource());
                        World world = ctx.getSource().getLocation().getWorld();
                        return new LocationTag(new Location(world, pos.blockX(), pos.blockY(), pos.blockZ()));
                    } catch (CommandSyntaxException e) {
                        throw new RuntimeException(e);
                    }
                },
                (ctx, spec) -> safeGet(() -> {
                    BlockPos pos = BlockPosArgument.getBlockPos(ctx, spec.name());
                    World world = ctx.getSource().getLevel().getWorld();
                    return new LocationTag(new Location(world, pos.getX(), pos.getY(), pos.getZ()));
                }));

        registerPaper("finePosition",
                spec -> Vec3Argument.vec3(),
                spec -> ArgumentTypes.finePosition(),
                (ctx, spec) -> {
                    try {
                        FinePosition pos = ctx.getArgument(spec.name(), FinePositionResolver.class)
                                .resolve(ctx.getSource());
                        World world = ctx.getSource().getLocation().getWorld();
                        return new LocationTag(new Location(world, pos.x(), pos.y(), pos.z()));
                    } catch (CommandSyntaxException e) {
                        throw new RuntimeException(e);
                    }
                },
                (ctx, spec) -> safeGet(() -> {
                    Vec3 vec = Vec3Argument.getVec3(ctx, spec.name());
                    World world = ctx.getSource().getLevel().getWorld();
                    return new LocationTag(new Location(world, vec.x, vec.y, vec.z));
                }));

        registerPaper("gameMode",
                spec -> GameModeArgument.gameMode(),
                spec -> ArgumentTypes.gameMode(),
                (ctx, spec) -> new ElementTag(ctx.getArgument(spec.name(), GameMode.class).name()),
                (ctx, spec) -> safeGet(() ->
                {
                    try {
                        return new ElementTag(GameModeArgument.getGameMode(ctx, spec.name()).getName());
                    } catch (CommandSyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }));

        registerPaper("uuid",
                spec -> UuidArgument.uuid(),
                spec -> ArgumentTypes.uuid(),
                (ctx, spec) -> new ElementTag(ctx.getArgument(spec.name(), UUID.class).toString()),
                (ctx, spec) -> safeGet(() ->
                        new ElementTag(UuidArgument.getUuid(ctx, spec.name()).toString())));

        registerPaper("namespacedKey",
                spec -> ArgumentTypes.namespacedKey(),
                spec -> ArgumentTypes.namespacedKey(),
                (ctx, spec) -> new ElementTag(ctx.getArgument(spec.name(), NamespacedKey.class).toString()),
                (ctx, spec) -> new ElementTag(ctx.getArgument(spec.name(), NamespacedKey.class).toString()));

        registerPaper("time",
                spec -> TimeArgument.time(Math.max(0, spec.intOption("min", 0))),
                spec -> ArgumentTypes.time(Math.max(0, spec.intOption("min", 0))),
                (ctx, spec) -> new ElementTag(String.valueOf(ctx.getArgument(spec.name(), Integer.class))),
                (ctx, spec) -> safeGet(() ->
                        new ElementTag(String.valueOf(ctx.getArgument(spec.name(), Integer.class)))));

        registerPaper("itemStack",
                spec -> ItemArgument.item(buildContext()),
                spec -> ItemArgument.item(buildContext()),
                (ctx, spec) -> {
                    try {
                        ItemInput input = ctx.getArgument(spec.name(), ItemInput.class);
                        return new ItemTag(CraftItemStack.asBukkitCopy(input.createItemStack(1, false)));
                    } catch (CommandSyntaxException e) {
                        throw new RuntimeException(e);
                    }
                },
                (ctx, spec) -> safeGet(() -> {
                    ItemInput input = ItemArgument.getItem(ctx, spec.name());
                    try {
                        return new ItemTag(CraftItemStack.asBukkitCopy(input.createItemStack(1, false)));
                    } catch (CommandSyntaxException e) {
                        throw new RuntimeException(e);
                    }
                }));

        registerPaper("blockState",
                spec -> BlockStateArgument.block(buildContext()),
                spec -> BlockStateArgument.block(buildContext()),
                (ctx, spec) -> {
                    BlockInput input = ctx.getArgument(spec.name(), BlockInput.class);
                    return new ElementTag(CraftBlockData.fromData(input.getState()).getAsString());
                },
                (ctx, spec) -> safeGet(() -> {
                    BlockInput input = BlockStateArgument.getBlock(ctx, spec.name());
                    return new ElementTag(CraftBlockData.fromData(input.getState()).getAsString());
                }));
    }

    private static <T> void registerNms(
            String name,
            Class<T> typeClass,
            Function<CommandArgumentSpec, ArgumentType<?>> factory) {

        ArgumentTypeRegistry.register(name, factory, new ArgumentTypeAdapter() {
            @Override
            public @NonNull ArgumentType<?> buildType(@NonNull CommandArgumentSpec spec) {
                return factory.apply(spec);
            }

            @Override
            public @Nullable AbstractTag resolveValue(
                    @NonNull CommandContext<CommandSourceStack> ctx,
                    @NonNull CommandArgumentSpec spec) {
                return safeGet(() -> new ElementTag(String.valueOf(ctx.getArgument(spec.name(), typeClass))));
            }
        });

        NMS_RESOLVERS.put(name, (ctx, spec) ->
                safeGet(() -> new ElementTag(String.valueOf(ctx.getArgument(spec.name(), typeClass)))));
    }

    private static void registerPaper(
            String name,
            Function<CommandArgumentSpec, ArgumentType<?>> nmsFactory,
            Function<CommandArgumentSpec, ArgumentType<?>> paperFactory,
            BiFunction<CommandContext<CommandSourceStack>, CommandArgumentSpec, AbstractTag> paperResolver,
            BiFunction<CommandContext<net.minecraft.commands.CommandSourceStack>, CommandArgumentSpec, AbstractTag> nmsResolver) {

        ArgumentTypeRegistry.register(name, nmsFactory, new ArgumentTypeAdapter() {
            @Override
            public @NonNull ArgumentType<?> buildType(@NonNull CommandArgumentSpec spec) {
                return paperFactory.apply(spec);
            }

            @Override
            public @Nullable AbstractTag resolveValue(
                    @NonNull CommandContext<CommandSourceStack> ctx,
                    @NonNull CommandArgumentSpec spec) {
                return safeGet(() -> paperResolver.apply(ctx, spec));
            }
        });

        NMS_RESOLVERS.put(name, nmsResolver);
    }

    private static Collection<Entity> resolveEntitiesPaper(
            CommandContext<CommandSourceStack> ctx,
            CommandArgumentSpec spec) {
        try {
            return ctx.getArgument(spec.name(), EntitySelectorArgumentResolver.class)
                    .resolve(ctx.getSource());
        } catch (CommandSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void injectCommand(@NonNull CommandContainer container) {
        CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher = getDispatcher();

        removeCommandNodes(dispatcher.getRoot(), container);

        LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> root =
                Commands.literal(container.getName())
                        .requires(source -> CommandManager.INSTANCE.checkAllowedFromNms(
                                source.getBukkitSender(), container));

        List<CommandArgumentSpec> specs = container.getArgumentSpecs();

        if (specs.isEmpty()) {
            root.executes(ctx -> executeFromNms(ctx, container));
            root.then(buildCatchAllArgument(container));
        } else {
            if (specs.getFirst().optional()) {
                root.executes(ctx -> executeFromNms(ctx, container));
            }
            root.then(buildArgumentChain(specs, 0, container));
        }

        dispatcher.register(root);

        for (String alias : container.getAliases()) {
            LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> aliasNode =
                    Commands.literal(alias)
                            .requires(source -> CommandManager.INSTANCE.checkAllowedFromNms(
                                    source.getBukkitSender(), container))
                            .redirect(dispatcher.getRoot().getChild(container.getName()));
            dispatcher.register(aliasNode);
        }
    }

    @Override
    public void syncCommandTree() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.updateCommands();
        }
    }

    private int executeFromNms(
            @NonNull CommandContext<net.minecraft.commands.CommandSourceStack> ctx,
            @NonNull CommandContainer container) {
        MapTag args = buildNmsArgs(ctx, container);
        CommandManager.INSTANCE.executeFromNms(ctx.getSource().getBukkitSender(), ctx.getInput(), args, container);
        return Command.SINGLE_SUCCESS;
    }

    private MapTag buildNmsArgs(
            @NonNull CommandContext<net.minecraft.commands.CommandSourceStack> ctx,
            @NonNull CommandContainer container) {

        MapTag args = new MapTag();

        for (CommandArgumentSpec spec : container.getArgumentSpecs()) {
            var resolver = NMS_RESOLVERS.get(spec.typeName());
            if (resolver == null) continue;

            AbstractTag value = resolver.apply(ctx, spec);
            if (value != null) args.putObject(spec.name(), value);
        }

        return args;
    }

    private ArgumentBuilder<net.minecraft.commands.CommandSourceStack, ?> buildArgumentChain(
            @NonNull List<CommandArgumentSpec> specs,
            int index,
            @NonNull CommandContainer container) {

        CommandArgumentSpec spec = specs.get(index);
        ArgumentTypeRegistry.Entry entry = ArgumentTypeRegistry.get(spec.typeName());

        if (entry == null) throw new IllegalStateException(
                "Unknown argument type '" + spec.typeName() + "' for command '" + container.getName() + "'");

        RequiredArgumentBuilder<net.minecraft.commands.CommandSourceStack, ?> node =
                Commands.argument(spec.name(), entry.nmsFactory().apply(spec));

        boolean isLast         = index == specs.size() - 1;
        boolean nextIsOptional = !isLast && specs.get(index + 1).optional();

        if (isLast || nextIsOptional) {
            node.executes(ctx -> executeFromNms(ctx, container));
        }

        if (container.hasSection(CommandContainer.SECTION_TAB_COMPLETE)) {
            node.suggests((ctx, builder) -> buildSuggestionsFromNms(ctx, builder, container));
        }

        if (!isLast) {
            node.then(buildArgumentChain(specs, index + 1, container));
        }

        return node;
    }

    private @NonNull RequiredArgumentBuilder<net.minecraft.commands.CommandSourceStack, String>
    buildCatchAllArgument(@NonNull CommandContainer container) {

        RequiredArgumentBuilder<net.minecraft.commands.CommandSourceStack, String> node =
                Commands.argument("...", StringArgumentType.greedyString())
                        .executes(ctx -> executeFromNms(ctx, container));

        if (container.hasSection(CommandContainer.SECTION_TAB_COMPLETE)) {
            node.suggests((ctx, builder) -> buildSuggestionsFromNms(ctx, builder, container));
        }

        return node;
    }

    private CompletableFuture<Suggestions> buildSuggestionsFromNms(
            @NonNull CommandContext<net.minecraft.commands.CommandSourceStack> ctx,
            @NonNull SuggestionsBuilder builder,
            @NonNull CommandContainer container) {
        return CommandManager.INSTANCE.buildTabCompletionsFromNms(
                ctx.getSource().getBukkitSender(), builder, container);
    }

    private void removeCommandNodes(
            @NonNull CommandNode<net.minecraft.commands.CommandSourceStack> root,
            @NonNull CommandContainer container) {
        for (String name : container.getAllAliases()) removeNode(root, name);
    }

    @SuppressWarnings("unchecked")
    private void removeNode(
            @NonNull CommandNode<net.minecraft.commands.CommandSourceStack> root,
            @NonNull String name) {
        Map<String, ?> children = (Map<String, ?>) ReflectionHelper.getFieldValue(CommandNode.class, "children", root);
        Map<String, ?> literals = (Map<String, ?>) ReflectionHelper.getFieldValue(CommandNode.class, "literals", root);
        if (children != null) children.remove(name);
        if (literals  != null) literals.remove(name);
    }

    private CommandDispatcher<net.minecraft.commands.CommandSourceStack> getDispatcher() {
        MinecraftServer server = ((CraftServer) Bukkit.getServer()).getServer();
        return server.getCommands().getDispatcher();
    }

    private static <T> @Nullable T safeGet(@NonNull Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (IllegalArgumentException ignored) {
            return null;
        } catch (Exception e) {
            Debugger.error("Unexpected error while resolving argument: " + e);
            return null;
        }
    }
}