package dev.corexinc.corex.engine.compiler;

import dev.corexinc.corex.api.commands.SlotAware;
import dev.corexinc.corex.engine.compiler.args.DefinitionArg;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class SlotAllocator {

    private SlotAllocator() {}

    public static void allocate(Instruction[] bytecode) {
        if (bytecode == null || bytecode.length == 0) return;

        SlotTable table = new SlotTable();
        List<DefinitionArg> reads = new ArrayList<>();

        collect(bytecode, table, reads);
        if (table.size() == 0) return;

        for (DefinitionArg read : reads) {
            read.bindSlot(table, table.indexOf(read.rootName()));
        }
        publish(bytecode, table);
    }

    private static void collect(Instruction[] bytecode, SlotTable table, List<DefinitionArg> reads) {
        Consumer<CompiledArgument> walker = new Consumer<>() {
            @Override
            public void accept(CompiledArgument argument) {
                if (argument instanceof DefinitionArg definition) {
                    table.intern(definition.rootName());
                    reads.add(definition);
                }
                argument.visitChildren(this);
            }
        };

        for (Instruction instruction : bytecode) {
            if (instruction.command instanceof SlotAware aware) {
                for (String written : aware.writtenDefinitions(instruction)) {
                    if (written != null) table.intern(written);
                }
            }

            for (CompiledArgument argument : instruction.linearArgs) walker.accept(argument);
            for (CompiledArgument argument : instruction.prefixArgs.values()) walker.accept(argument);
            for (CompiledArgument argument : instruction.globalFlags.values()) walker.accept(argument);

            if (instruction.innerBlock != null) collect(instruction.innerBlock, table, reads);
        }
    }

    private static void publish(Instruction[] bytecode, SlotTable table) {
        for (Instruction instruction : bytecode) {
            instruction.slots = table;
            if (instruction.command instanceof SlotAware aware) aware.bindSlots(instruction, table);
            if (instruction.innerBlock != null) publish(instruction.innerBlock, table);
        }
    }
}
