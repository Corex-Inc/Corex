package dev.corexinc.corex.api.scripts;

import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.NotNull;

/**
 * A comment Corex found while normalizing a script, on its way to being thrown away.
 *
 * <p>Comments are the one thing that exists in the file and in no artifact after it, so an addon
 * that wants to read directives out of them would otherwise have to ask for the whole file as text.
 * This is the safe version of that: the comments arrive already separated from code, strings and
 * indentation, and there is nothing here to break by reading them.</p>
 *
 * @param line  the 1-based line the comment started on, counted in the file as the text passes
 *              saw it, so a pass that inserted lines earlier has shifted this.
 * @param text  the comment body, without the {@code //} or {@code /*} markers, trimmed.
 * @param block {@code true} for a {@code /*} comment, {@code false} for a {@code //} one.
 * @since 1.0.0
 */
@AvailableSince("1.0.0")
public record ScriptComment(int line, @NotNull String text, boolean block) {

    /**
     * Reads the value of a {@code key:value} directive.
     *
     * <pre>{@code
     * // using:combat
     * comment.directive("using")   // "combat"
     * }</pre>
     *
     * @param key the directive name, without the colon.
     * @return the trimmed value, or {@code null} when this comment is not that directive.
     */
    public String directive(@NotNull String key) {
        if (!text.startsWith(key)) {
            return null;
        }
        String rest = text.substring(key.length()).stripLeading();
        if (rest.isEmpty() || rest.charAt(0) != ':') {
            return null;
        }
        return rest.substring(1).trim();
    }
}
