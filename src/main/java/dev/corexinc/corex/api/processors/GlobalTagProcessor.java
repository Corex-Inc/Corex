package dev.corexinc.corex.api.processors;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Adjustable;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.engine.utils.CorexLogger;
import dev.corexinc.corex.engine.utils.debugging.Debugger;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.MapTag;

public class GlobalTagProcessor {

    public static final TagProcessor<AbstractTag> PROCESSOR = new TagProcessor<>();

    public static void register() {

        PROCESSOR.registerTag(ElementTag.class, "prefix", (attr, obj) ->
                new ElementTag(obj.getPrefix()));

        PROCESSOR.registerTag(ElementTag.class, "exists", (attr, obj) ->
                new ElementTag(true));

        PROCESSOR.registerTag(AbstractTag.class, "as", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            String type = attr.getParam().toLowerCase();

            String targetPrefix = ObjectFetcher.getPrefixForName(type);
            if (targetPrefix == null) {
                Debugger.echoError(attr.getQueue(), "Tag .as[]: Unknown object type '<yellow>" + type + "</yellow>'");
                return null;
            }

            String forceCastString = targetPrefix + "@" + obj.identify();
            return ObjectFetcher.pickObject(forceCastString);
        });

        PROCESSOR.registerTag(AbstractTag.class, "with", (attr, obj) -> {
            if (!(obj instanceof Adjustable adj)) return null;
            if (!attr.hasParam()) return null;

            Adjustable instance = adj.duplicate();

            MapTag map = new MapTag(attr.getParam());

            for (String key : map.keySet()) {
                instance = (Adjustable) instance.applyMechanism(key, map.getObject(key));
            }

            return instance;
        });

        PROCESSOR.registerTag(AbstractTag.class, "withMap", (attr, obj) -> {
            if (!(obj instanceof Adjustable adj)) return null;
            if (!attr.hasParam()) return null;

            AbstractTag mapObj = ObjectFetcher.pickObject(attr.getParam());
            if (!(mapObj instanceof MapTag map)) return null;

            Adjustable instance = adj.duplicate();

            for (String key : map.keySet()) {
                instance = (Adjustable) instance.applyMechanism(key, map.getObject(key));
            }

            return instance;
        });
    }
}