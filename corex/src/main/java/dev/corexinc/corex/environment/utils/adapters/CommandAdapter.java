package dev.corexinc.corex.environment.utils.adapters;

import dev.corexinc.corex.environment.containers.commands.CommandContainer;
import org.jspecify.annotations.NonNull;

public interface CommandAdapter {
    void injectCommand(@NonNull CommandContainer container);
    void syncCommandTree();
}