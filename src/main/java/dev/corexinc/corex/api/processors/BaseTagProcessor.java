package dev.corexinc.corex.api.processors;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.engine.tags.TagManager;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Provides a centralized gateway for registering <b>Base Tags</b> within the Corex Engine.
 *
 * <p>A <i>Base Tag</i> is the primary identifier at the start of a tag chain,
 * such as {@code player} in {@code <player.name>}.</p>
 *
 * <h2>Usage Examples</h2>
 *
 * <b>1. Registering a standard object (e.g., PlayerTag):</b>
 * <pre>{@code
 * BaseTagProcessor.registerBaseTag("player", (attribute) -> {
 *     if (attribute.getQueue() != null && attribute.getQueue().getPlayer() != null) {
 *         return attribute.getQueue().getPlayer();
 *     }
 *     return null;
 * });
 * }</pre>
 *
 * <b>2. Registering a utility tag (e.g., UtilTag):</b>
 * <pre>{@code
 * BaseTagProcessor.registerBaseTag("util", (attribute) -> new UtilTag());
 * }</pre>
 *
 * <b>3. Registering a static element constructor:</b>
 * <pre>{@code
 * BaseTagProcessor.registerBaseTag("element", (attribute) -> {
 *     return new ElementTag(attribute.getParam());
 * });
 * }</pre>
 *
 * @since 1.0.0
 */
@ApiStatus.AvailableSince("1.0.0")
public final class BaseTagProcessor {

    private BaseTagProcessor() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Registers a new base tag.
     *
     * @param name     the name of the base tag. Must be unique.
     * @param function the handler for the tag.
     */
    @ApiStatus.AvailableSince("1.0.0")
    public static void registerBaseTag(
            @NotNull final String name,
            @NotNull final Function<@NotNull Attribute, @Nullable AbstractTag> function
    ) {
        TagManager.registerBaseTag(name, function);
    }
}