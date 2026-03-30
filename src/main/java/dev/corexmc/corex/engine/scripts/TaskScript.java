package dev.corexmc.corex.engine.scripts;

import dev.corexmc.corex.engine.compiler.Instruction;

public class TaskScript {
    private final String name;
    private final Instruction[] bytecode;

    public TaskScript(String name, Instruction[] bytecode) {
        this.name = name;
        this.bytecode = bytecode;
    }

    public Instruction[] getBytecode() { return bytecode; }
    public String getName() { return name; }
}