package dev.corexinc.corex.engine.compiler.args;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.SlotTable;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.MapTag;

import java.util.function.Consumer;

public class DefinitionArg implements CompiledArgument {

    private final String name;
    private final String[] path;
    private final CompiledArgument fallback;
    private final String rawFullTag;

    private SlotTable slotOwner;
    private int slot = SlotTable.NO_SLOT;

    public DefinitionArg(String fullPath, CompiledArgument fallback, String rawFullTag) {
        this.fallback = fallback;
        this.rawFullTag = rawFullTag;
        if (fullPath.indexOf('.') < 0) {
            this.name = fullPath;
            this.path = null;
        } else {
            String[] parts = fullPath.split("\\.", -1);
            this.name = parts[0];
            this.path = parts;
        }
    }

    public String rootName() {
        return name;
    }

    public void bindSlot(SlotTable owner, int index) {
        this.slotOwner = owner;
        this.slot = index;
    }

    public AbstractTag resolve(ScriptQueue queue) {
        AbstractTag current = slot >= 0 && queue.slotTable() == slotOwner
                ? queue.readSlot(slot)
                : queue.getDefinition(name);

        if (path != null) {
            for (int i = 1; i < path.length && current != null; i++) {
                current = current instanceof MapTag map ? map.getObject(path[i]) : null;
            }
        }

        return current;
    }

    @Override
    public AbstractTag evaluate(ScriptQueue queue) {
        AbstractTag current = resolve(queue);

        if (current == null) {
            if (fallback != null) return fallback.evaluate(queue);

            String escapedTag = rawFullTag.replace("<", "\\<");
            Debugger.echoError(queue, "Definition '<red>" + name + "</red>' returned null.");
            Debugger.echoError(queue, "Tag \\<<yellow>" + escapedTag + "</yellow>> is invalid!");
            return new ElementTag(rawFullTag);
        }

        return current;
    }

    @Override
    public void visitChildren(Consumer<CompiledArgument> visitor) {
        if (fallback != null) visitor.accept(fallback);
    }

    @Override
    public String getRaw() {
        return "<" + rawFullTag + ">";
    }
}
