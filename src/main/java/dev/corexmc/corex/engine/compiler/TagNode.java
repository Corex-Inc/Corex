package dev.corexmc.corex.engine.compiler;

public class TagNode {
    public final String name;
    public final CompiledArgument param;

    public TagNode(String name, CompiledArgument param) {
        this.name = name;
        this.param = param;
    }
}