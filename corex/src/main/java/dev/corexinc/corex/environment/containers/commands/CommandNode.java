package dev.corexinc.corex.environment.containers.commands;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CommandNode {

    private final String name;
    private final boolean argument;
    private final CommandArgumentSpec spec;

    private final List<String> aliases;

    private final String basePath;
    private final String argPath;
    private final List<CommandNode> pathChain;

    private final boolean hasScript;
    private final boolean hasSuggests;
    private final boolean hasRequires;

    private final List<CommandNode> children = new ArrayList<>();

    CommandNode(@NonNull String name,
                boolean argument,
                @Nullable CommandArgumentSpec spec,
                @NonNull List<String> aliases,
                @NonNull String basePath,
                @NonNull String argPath,
                @Nullable List<CommandNode> parentChain,
                boolean hasScript,
                boolean hasSuggests,
                boolean hasRequires) {
        this.name = name;
        this.argument = argument;
        this.spec = spec;
        this.aliases = aliases;
        this.basePath = basePath;
        this.argPath = argPath;
        this.hasScript = hasScript;
        this.hasSuggests = hasSuggests;
        this.hasRequires = hasRequires;

        List<CommandNode> chain = new ArrayList<>(parentChain != null ? parentChain : List.of());
        if (parentChain != null) chain.add(this);
        this.pathChain = Collections.unmodifiableList(chain);
    }

    void addChild(@NonNull CommandNode child) { children.add(child); }

    public @NonNull String name() { return name; }
    public boolean isArgument() { return argument; }
    public boolean isLiteral() { return !argument; }
    public @Nullable CommandArgumentSpec spec() { return spec; }
    public @NonNull List<String> aliases() { return aliases; }
    public @NonNull String basePath() { return basePath; }
    public @NonNull String argPath() { return argPath; }
    public @NonNull List<CommandNode> children() { return children; }
    public boolean hasChildren() { return !children.isEmpty(); }

    public boolean hasScript() { return hasScript; }
    public boolean hasSuggestions() { return hasSuggests; }
    public boolean hasRequires() { return hasRequires; }

    public @NonNull String scriptPath()   { return basePath + ".script"; }
    public @NonNull String suggestsPath() { return basePath + ".suggests"; }
    public @NonNull String requiresPath() { return basePath + ".requires"; }

    public @NonNull List<CommandNode> pathChain() { return pathChain; }

    public @NonNull List<CommandNode> ancestorChain() {
        return pathChain.isEmpty() ? pathChain : pathChain.subList(0, pathChain.size() - 1);
    }
}
