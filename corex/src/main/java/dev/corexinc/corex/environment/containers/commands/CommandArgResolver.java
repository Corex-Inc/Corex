package dev.corexinc.corex.environment.containers.commands;

import dev.corexinc.corex.api.tags.AbstractTag;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

@FunctionalInterface
public interface CommandArgResolver {
    @Nullable AbstractTag resolve(@NonNull CommandArgumentSpec spec);
}
