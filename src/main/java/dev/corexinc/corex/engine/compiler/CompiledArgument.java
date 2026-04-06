package dev.corexinc.corex.engine.compiler;

import dev.corexinc.corex.engine.compiler.math.MathNode;
import dev.corexinc.corex.engine.registry.FormatRegistry;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.AbstractFormatter;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.tags.TagManager;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.environment.tags.core.ComponentTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

import java.util.ArrayList;
import java.util.List;

public interface CompiledArgument {
    AbstractTag evaluate(ScriptQueue queue);

    String getRaw();

    class Static implements CompiledArgument {
        private final AbstractTag tag;

        public Static(String text) {
            this.tag = new ElementTag(text);
        }

        public Static(AbstractTag tag) {
            this.tag = tag;
        }

        @Override
        public AbstractTag evaluate(ScriptQueue queue) {
            return tag;
        }

        @Override public String getRaw() { return tag.identify(); }
    }

    class Mixed implements CompiledArgument {
        private final CompiledArgument[] parts;
        public Mixed(CompiledArgument[] parts) { this.parts = parts; }

        @Override
        public AbstractTag evaluate(ScriptQueue queue) {
            TextComponent.Builder builder = Component.text();
            StringBuilder textBuffer = new StringBuilder();
            String lastColors = "";

            for (CompiledArgument part : parts) {
                AbstractTag tag = part.evaluate(queue);

                if (tag instanceof ComponentTag) {
                    if (!textBuffer.isEmpty()) {
                        String flushedText = textBuffer.toString();
                        builder.append(Corex.SERIALIZER.deserialize(flushedText));

                        lastColors = extractLastColors(flushedText);
                        textBuffer.setLength(0);
                    }

                    builder.append(tag.asComponent());

                    textBuffer.append(lastColors);
                } else {
                    textBuffer.append(tag.identify());
                }
            }

            if (!textBuffer.isEmpty()) {
                String remaining = textBuffer.toString();
                if (!remaining.equals(lastColors)) {
                    builder.append(Corex.SERIALIZER.deserialize(remaining));
                }
            }

            return new ComponentTag(builder.build());
        }

        @Override
        public String getRaw() {
            StringBuilder sb = new StringBuilder();
            for (CompiledArgument part : parts) sb.append(part.getRaw());
            return sb.toString();
        }

        private String extractLastColors(String text) {
            StringBuilder result = new StringBuilder();
            int length = text.length();
            for (int i = 0; i < length; i++) {
                char c = text.charAt(i);
                if (c == '§' && i + 1 < length) {
                    char code = text.charAt(i + 1);
                    if (code == 'x' && i + 13 < length) {
                        result.setLength(0);
                        result.append(text.substring(i, i + 14));
                        i += 13;
                    } else if (isColorCode(code)) {
                        result.setLength(0);
                        result.append('§').append(code);
                    } else if (isFormatCode(code)) {
                        result.append('§').append(code);
                    }
                }
            }
            return result.toString();
        }

        private boolean isColorCode(char c) {
            return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F') || c == 'r' || c == 'R';
        }

        private boolean isFormatCode(char c) {
            return (c >= 'k' && c <= 'o') || (c >= 'K' && c <= 'O');
        }
    }

    class PreSlicedDynamic implements CompiledArgument {
        private final TagNode[] nodes;
        private final CompiledArgument fallback;
        private final String rawFullTag;
        private final FormatRegistry formats;

        public PreSlicedDynamic(TagNode[] nodes, CompiledArgument fallback, String rawFullTag) {
            this.nodes = nodes;
            this.fallback = fallback;
            this.rawFullTag = rawFullTag;
            this.formats = Corex.getInstance().getRegistry().getFormats();
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
                return new ElementTag(rawFullTag);
            }
            return currentObj;
        }

        @Override
        public String getRaw() { return rawFullTag; }
    }

    class MathArg implements CompiledArgument {
        private final MathNode node;
        private final String raw;

        public MathArg(MathNode node, String raw) {
            this.node = node;
            this.raw = raw;
        }

        @Override
        public AbstractTag evaluate(ScriptQueue queue) {
            try {
                double result = node.eval(queue);
                return new ElementTag(result);
            } catch (Exception e) {
                CorexLogger.error("ERROR while calculating expression " + raw + ": " + e.getMessage());
                return new ElementTag(0);
            }
        }

        @Override
        public String getRaw() {
            return "";
        }
    }
}