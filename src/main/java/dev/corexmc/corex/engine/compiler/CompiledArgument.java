package dev.corexmc.corex.engine.compiler;

import dev.corexmc.corex.engine.queue.ScriptQueue;
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
}