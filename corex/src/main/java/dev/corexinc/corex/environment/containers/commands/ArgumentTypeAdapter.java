package dev.corexinc.corex.environment.containers.commands;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.corexinc.corex.api.tags.AbstractTag;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public interface ArgumentTypeAdapter {

    @NonNull ArgumentType<?> buildType(@NonNull CommandArgumentSpec spec);

    @Nullable AbstractTag resolveValue(
            @NonNull CommandContext<CommandSourceStack> ctx,
            @NonNull CommandArgumentSpec spec);
}