package dev.corexinc.corex.engine.utils;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.MapTag;
import org.jetbrains.annotations.ApiStatus.AvailableSince;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Reads the script format for server links, the entries a client shows in its pause menu.
 *
 * <p>The format is one {@link MapTag}: the key is the label, the value is the URL, and the order of
 * the map is the order of the menu. A key spelled exactly like one of the ten built in link types is
 * sent as that type, so the client draws it in the player's own language; anything else is sent as a
 * custom label. The match is case sensitive, so {@code WEBSITE} is the built in type while
 * {@code Website} is a custom label reading "Website".</p>
 *
 * <pre>{@code
 * WEBSITE=https://example.com;BUG_REPORT=https://example.com/bugs;<#5865F2>Discord=https://dsc.gg/corexinc
 * }</pre>
 *
 * <p>Parsing lives here because both front ends take the same format while their APIs disagree on
 * the names: Paper calls the bug report type {@code REPORT_BUG}, Velocity calls it
 * {@code BUG_REPORT}. {@link Link#builtInType()} hands back the Velocity spelling as the canonical
 * one, and the Paper side translates that single case.</p>
 *
 * @since 1.0.0
 */
@AvailableSince("1.0.0")
public final class ServerLinkFormat {

    /**
     * One parsed entry: either a built in type or a custom label, never both.
     *
     * @param builtInType the canonical built in name, or {@code null} for a custom label.
     * @param label       the raw label text to run through the usual component path,
     *                    or {@code null} for a built in type.
     * @param url         the link target, always http or https.
     */
    @AvailableSince("1.0.0")
    public record Link(@Nullable String builtInType, @Nullable String label, @NotNull URI url) {}

    private static final Set<String> BUILT_IN_TYPES = Set.of(
            "BUG_REPORT", "COMMUNITY_GUIDELINES", "SUPPORT", "STATUS", "FEEDBACK",
            "COMMUNITY", "WEBSITE", "FORUMS", "NEWS", "ANNOUNCEMENTS");

    private ServerLinkFormat() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Parses a map of label to URL into links, keeping the map's order.
     *
     * <p>Nothing is applied halfway: a single bad entry fails the whole map, because a menu missing
     * one line is harder to notice than a menu that never changed.</p>
     *
     * @param value the map, or an element holding {@code key=value;key=value} text.
     * @return the links in menu order, empty when the map is empty.
     * @throws IllegalArgumentException if an entry has no usable URL.
     */
    @NotNull
    @AvailableSince("1.0.0")
    public static List<Link> parse(@NotNull AbstractTag value) {
        MapTag map = value instanceof MapTag mapTag ? mapTag : new MapTag(value.identify());
        List<Link> links = new ArrayList<>();

        for (String key : map.keySet()) {
            AbstractTag rawUrl = map.getObject(key);
            if (rawUrl == null) {
                throw new IllegalArgumentException("link '" + key + "' has no URL");
            }

            String builtInType = builtInTypeOf(key);
            String rawText = rawUrl instanceof ElementTag element ? element.asString() : rawUrl.identify();
            links.add(new Link(builtInType, builtInType == null ? key : null, readUrl(key, rawText)));
        }

        return links;
    }

    private static String builtInTypeOf(String key) {
        return BUILT_IN_TYPES.contains(key) ? key : null;
    }

    private static URI readUrl(String key, String raw) {
        URI url;
        try {
            url = new URI(raw);
        }
        catch (Exception exception) {
            throw new IllegalArgumentException("link '" + key + "' has a broken URL: " + raw);
        }

        String scheme = url.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new IllegalArgumentException("link '" + key + "' must be an http or https URL, got: " + raw);
        }

        return url;
    }
}
