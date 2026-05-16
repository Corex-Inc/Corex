package dev.corexinc.corex.engine.compiler.args;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.CorexSerializer;
import dev.corexinc.corex.environment.tags.core.ComponentTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

public class MixedArg implements CompiledArgument {
    private final CompiledArgument[] parts;

    public MixedArg(CompiledArgument[] parts) {
        this.parts = parts;
    }

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
                    builder.append(CorexSerializer.LEGACY.deserialize(flushedText));

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
                builder.append(CorexSerializer.LEGACY.deserialize(remaining));
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
                    result.append(text, i, i + 14);
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
