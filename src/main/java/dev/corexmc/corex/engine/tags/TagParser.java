package dev.corexmc.corex.engine.tags;

import dev.corexmc.corex.api.tags.AbstractTag;
import dev.corexmc.corex.api.tags.Attribute;
import dev.corexmc.corex.engine.queue.ScriptQueue;
import dev.corexmc.corex.engine.utils.debugging.Debugger;

import java.util.ArrayList;
import java.util.List;

public class TagParser {

    private final List<Object> pieces = new ArrayList<>();

    private TagParser(String rawText) {
        StringBuilder buffer = new StringBuilder();
        int tagDepth = 0;

        for (char c : rawText.toCharArray()) {
            if (c == '<') {
                if (tagDepth == 0) {
                    if (!buffer.isEmpty()) {
                        pieces.add(buffer.toString());
                        buffer.setLength(0);
                    }
                } else {
                    buffer.append(c);
                }
                tagDepth++;
            } else if (c == '>') {
                tagDepth--;
                if (tagDepth == 0) {
                    pieces.add(new TagPiece(buffer.toString()));
                    buffer.setLength(0);
                } else {
                    buffer.append(c);
                }
            } else {
                buffer.append(c);
            }
        }
        if (!buffer.isEmpty()) {
            pieces.add(buffer.toString());
        }
    }

    public static TagParser parse(String text) {
        return new TagParser(text);
    }

    public String evaluate(ScriptQueue queue) {
        StringBuilder result = new StringBuilder();

        for (Object piece : pieces) {
            if (piece instanceof String) {
                result.append(piece);
            } else if (piece instanceof TagPiece) {
                String rawTag = ((TagPiece) piece).raw;

                dev.corexmc.corex.engine.compiler.TagNode[] nodes =
                        dev.corexmc.corex.engine.compiler.ScriptCompiler.parseTagNodes(rawTag);

                Attribute attribute = new Attribute(nodes, queue);

                AbstractTag currentObj = null;
                boolean failed = false;

                dev.corexmc.corex.engine.registry.FormatRegistry formats =
                        dev.corexmc.corex.Corex.getInstance().getRegistry().getFormats();

                if (formats.isFormat(attribute.getName())) {
                    currentObj = formats.get(attribute.getName());
                    attribute.fulfill(1);
                }
                else {
                    currentObj = TagManager.executeBaseTag(attribute);
                }

                if (currentObj == null) failed = true;

                if (!failed) {
                    while (attribute.hasNext()) {
                        AbstractTag nextObj = currentObj.getAttribute(attribute);
                        if (nextObj == null) {
                            failed = true;
                            break;
                        }
                        currentObj = nextObj;
                        attribute.fulfill(1);
                    }
                }

                if (failed) {
                    result.append(rawTag);
                    Debugger.echoError("Tag <" + rawTag + "> not found!");
                } else {
                    String finalValue = currentObj.identify();
                    result.append(finalValue);
                    Debugger.echoTagFill(rawTag, finalValue);
                }
            }
        }
        return result.toString();
    }

    private static class TagPiece {
        final String raw;
        TagPiece(String raw) { this.raw = raw; }
    }
}