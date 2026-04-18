package dev.corexinc.corex.environment.events;

import dev.corexinc.corex.api.tags.AbstractTag;
import java.util.List;

public class EventReturn {

    public static String getPrefixed(List<AbstractTag> returns, String prefix) {
        String search = prefix.toLowerCase() + ":";
        for (AbstractTag tag : returns) {
            String id = tag.identify();
            if (id.toLowerCase().startsWith(search)) {
                return id.substring(search.length());
            }
        }
        return null;
    }
}