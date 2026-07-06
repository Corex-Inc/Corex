package dev.corexinc.corex.engine.compiler.args;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.engine.compiler.CompiledArgument;
import dev.corexinc.corex.engine.queue.ScriptQueue;
import dev.corexinc.corex.engine.utils.CorexSerializer;
import dev.corexinc.corex.environment.tags.core.ComponentTag;
import dev.corexinc.corex.environment.tags.core.MarkupTag;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

public class MixedArg implements CompiledArgument {
    private final CompiledArgument[] parts;

    public MixedArg(CompiledArgument[] parts) {
        this.parts = parts;
    }

    @Override
    public AbstractTag evaluate(ScriptQueue queue) {
        StringBuilder markupBuffer = new StringBuilder();
        TagResolver.Builder placeholders = TagResolver.builder();
        int placeholderIndex = 0;

        for (CompiledArgument part : parts) {
            AbstractTag tag = part.evaluate(queue);

            if (tag instanceof MarkupTag markupTag) {
                markupBuffer.append(markupTag.identify());
            } else if (tag instanceof ComponentTag componentTag) {
                String placeholderKey = "corex_insert_" + (placeholderIndex++);
                placeholders.resolver(Placeholder.component(placeholderKey, componentTag.asComponent()));
                markupBuffer.append('<').append(placeholderKey).append('>');
            } else {
                Component legacyPiece = CorexSerializer.LEGACY.deserialize(tag.identify());
                markupBuffer.append(MiniMessage.miniMessage().serialize(legacyPiece));
            }
        }

        Component result = MiniMessage.miniMessage().deserialize(markupBuffer.toString(), placeholders.build());
        return new ComponentTag(result);
    }

    @Override
    public String getRaw() {
        StringBuilder sb = new StringBuilder();
        for (CompiledArgument part : parts) sb.append(part.getRaw());
        return sb.toString();
    }
}