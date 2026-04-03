package dev.corexinc.corex.engine.compiler;

import dev.corexinc.corex.engine.registry.FormatRegistry;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.TagManager;

public interface CompiledArgument {
    String evaluate(ScriptQueue queue);

    class Static implements CompiledArgument {
        private final String text;
        public Static(String text) { this.text = text; }
        @Override public String evaluate(ScriptQueue queue) { return text; }
    }

    class Mixed implements CompiledArgument {
        private final CompiledArgument[] parts;
        public Mixed(CompiledArgument[] parts) { this.parts = parts; }

        @Override
        public String evaluate(ScriptQueue queue) {
            StringBuilder sb = new StringBuilder();
            for (CompiledArgument part : parts) sb.append(part.evaluate(queue));
            return sb.toString();
        }
    }

    class PreSlicedDynamic implements CompiledArgument {
        private final TagNode[] nodes;
        private final CompiledArgument fallback;
        private final String rawFullTag;

        public PreSlicedDynamic(TagNode[] nodes, CompiledArgument fallback, String rawFullTag) {
            this.nodes = nodes;
            this.fallback = fallback;
            this.rawFullTag = rawFullTag;
        }

        @Override
        public String evaluate(ScriptQueue queue) {
            Attribute attr = new Attribute(nodes, queue);
            AbstractTag currentObj = null;

            FormatRegistry formats =
                    Corex.getInstance().getRegistry().getFormats();

            if (formats.isFormat(attr.getName())) {
                currentObj = formats.get(attr.getName()).parse(attr);
                attr.fulfill(1);
            } else {
                currentObj = TagManager.executeBaseTag(attr);
            }

            while (attr.hasNext()) {
                if (currentObj == null) {
                    if (attr.getName().equals("ifNull") && attr.hasParam()) {
                        currentObj = ObjectFetcher.pickObject(attr.getParam());
                        attr.fulfill(1);
                        continue;
                    }
                    break;
                }
                AbstractTag nextObj = currentObj.getAttribute(attr);
                if (nextObj == null) break;
                currentObj = nextObj;
                attr.fulfill(1);
            }

            if (currentObj == null) {
                if (fallback != null) return fallback.evaluate(queue);

                return rawFullTag;
            }
            return currentObj.identify();
        }
    }
}