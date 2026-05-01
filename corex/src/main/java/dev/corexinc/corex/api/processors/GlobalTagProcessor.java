package dev.corexinc.corex.api.processors;

import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Adjustable;
import dev.corexinc.corex.api.tags.Flaggable;
import dev.corexinc.corex.engine.flags.trackers.AbstractFlagTracker;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
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

        PROCESSOR.registerTag(ElementTag.class, "equals", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            return new ElementTag(obj.identify().equals(attr.getParamObject().identify()));
        });

        PROCESSOR.registerTag(ElementTag.class, "equalsIgnoreCase", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            return new ElementTag(obj.identify().equalsIgnoreCase(attr.getParamObject().identify()));
        });

        PROCESSOR.registerTag(AbstractTag.class, "as", (attr, obj) -> {
            if (!attr.hasParam()) return null;
            String type = attr.getParam().toLowerCase();

            String targetPrefix = ObjectFetcher.getPrefixForName(type);
            if (targetPrefix == null) {
                Debugger.echoError(attr.getQueue(), "Unknown object type '<yellow>" + type + "</yellow>'");
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

        PROCESSOR.registerTag(AbstractTag.class, "flag", (attr, obj) -> {
            if (!(obj instanceof Flaggable flaggable)) return null;
            if (!attr.hasParam()) return null;

            AbstractFlagTracker tracker = flaggable.getFlagTracker();

            return tracker.getFlag(attr.getParam());
        });

        PROCESSOR.registerTag(ElementTag.class, "hasFlag", (attr, obj) -> {
            if (!(obj instanceof Flaggable flaggable)) return null;
            if (!attr.hasParam()) return null;

            AbstractFlagTracker tracker = flaggable.getFlagTracker();

            return new ElementTag(tracker.getFlag(attr.getParam()) != null);
        });
    }
}