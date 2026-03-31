package dev.corexmc.corex.engine.compiler;

import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.api.tags.Attribute;
import dev.corexmc.corex.engine.queue.ScriptQueue;
import dev.corexmc.corex.engine.tags.TagManager;
import dev.corexmc.corex.engine.tags.TagParser;

public interface CompiledArgument {
    String evaluate(ScriptQueue queue);

    class Static implements CompiledArgument {
        private final String text;
        public Static(String text) { this.text = text; }
        @Override public String evaluate(ScriptQueue queue) { return text; }
    }

    class Dynamic implements CompiledArgument {
        private final TagParser parser;
        public Dynamic(String text) { this.parser = TagParser.parse(text); }
        @Override public String evaluate(ScriptQueue queue) { return parser.evaluate(queue); }
    }

    class PreSlicedDynamic implements CompiledArgument {
        private final TagNode[] nodes;

        public PreSlicedDynamic(TagNode[] nodes) {
            this.nodes = nodes;
        }

        @Override
        public String evaluate(ScriptQueue queue) {
            Attribute attr = new Attribute(nodes, queue);
            AbstractTag currentObj = null;

            dev.corexmc.corex.engine.registry.FormatRegistry formats =
                    dev.corexmc.corex.Corex.getInstance().getRegistry().getFormats();

            if (formats.isFormat(attr.getName())) {
                currentObj = formats.get(attr.getName());
                attr.fulfill(1);
            } else {
                currentObj = TagManager.executeBaseTag(attr);
            }

            if (currentObj == null) {
                return buildRawTag();
            }

            while (attr.hasNext()) {
                AbstractTag nextObj = currentObj.getAttribute(attr);
                if (nextObj == null) {
                    return buildRawTag();
                }
                currentObj = nextObj;
                attr.fulfill(1);
            }

            return currentObj.identify();
        }

        private String buildRawTag() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < nodes.length; i++) {
                sb.append(nodes[i].name);
                if (i < nodes.length - 1) sb.append(".");
            }
            return sb.toString();
        }
    }
}