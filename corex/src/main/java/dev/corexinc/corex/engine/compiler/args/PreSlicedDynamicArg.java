package dev.corexinc.corex.engine.compiler.args;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.compiler.TagNode;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.registry.FormatRegistry;
import dev.corexinc.corex.engine.scripts.ScriptManager;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.tags.TagManager;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ElementTag;

public class PreSlicedDynamicArg implements CompiledArgument {
    private final TagNode[] nodes;
    private final CompiledArgument fallback;
    private final String rawFullTag;
    private final FormatRegistry formats;

    public PreSlicedDynamicArg(TagNode[] nodes, CompiledArgument fallback, String rawFullTag) {
        this.nodes = nodes;
        this.fallback = fallback;
        this.rawFullTag = rawFullTag;
        this.formats = ScriptManager.getRegistry().getFormats();
    }

    @Override
    public AbstractTag evaluate(ScriptQueue queue) {
        Attribute attr = new Attribute(nodes, queue);
        AbstractTag currentObj;

        AbstractFormatter fmt = formats.get(attr.getName());
        if (fmt != null) {
            currentObj = fmt.parse(attr);
            attr.fulfill(1);
        } else {
            currentObj = TagManager.executeBaseTag(attr);
        }

        while (attr.hasNext()) {

            if (currentObj == null) {
                if (attr.getName().equals("ifNull") && attr.hasParam()) {
                    currentObj = ObjectFetcher.pickObject(attr.getParam());
                }
                attr.fulfill(1);
                continue;
            }

            if (attr.getName().equals("ifNull")) {
                attr.fulfill(1);
                continue;
            }

            currentObj = currentObj.getAttribute(attr);
            attr.fulfill(1);
        }

        if (currentObj == null) {
            if (fallback != null) return fallback.evaluate(queue);

            String escapedName = attr.getName().replace("<", "\\<");
            String escapedTag = rawFullTag.replace("<", "\\<");

            Debugger.echoError(queue, "Tag-base '<red>" + escapedName + "</red>' returned null.");
            Debugger.echoError(queue, "Tag \\<<yellow>" + escapedTag + "</yellow>> is invalid!");
            Debugger.echoError(queue, "Unfilled or unrecognized sub-tag(s) '<red>" + escapedName + "</red>' for tag \\<<aqua>" + escapedTag + "</aqua>>!");

            return new ElementTag(rawFullTag);
        }

        return currentObj;
    }

    @Override
    public String getRaw() {
        return "<" + rawFullTag + ">";
    }
}
