package dev.corexinc.corex.environment.tags.core;

import dev.corexinc.corex.Corex;
import dev.corexinc.corex.api.processors.BaseTagProcessor;
import dev.corexinc.corex.api.processors.TagProcessor;
import dev.corexinc.corex.api.tags.AbstractTag;
import dev.corexinc.corex.api.tags.Attribute;
import dev.corexinc.corex.api.tags.Flaggable;
import dev.corexinc.corex.engine.flags.trackers.SqlFlagTracker;
import dev.corexinc.corex.engine.tags.ObjectFetcher;
import org.jspecify.annotations.NonNull;

import java.io.File;

public class ServerTag implements AbstractTag, Flaggable {

    private static final String PREFIX = "server";
    public static final TagProcessor<ServerTag> PROCESSOR = new TagProcessor<>();

    private static final ServerTag INSTANCE = new ServerTag();

    private static SqlFlagTracker serverTracker;

    public static void register() {
        BaseTagProcessor.registerBaseTag("server", attr -> INSTANCE);
        ObjectFetcher.registerFetcher(PREFIX, s -> INSTANCE);

        File dbFile = new File(Corex.getInstance().getDataFolder(), "serverFlags.db");
        serverTracker = new SqlFlagTracker(dbFile, "serverGlobal");

        PROCESSOR.registerTag(ElementTag.class, "ram", (attribute, serverTag) -> new ElementTag(Runtime.getRuntime().maxMemory()));
    }

    public ServerTag() {}

    @Override
    public SqlFlagTracker getFlagTracker() {
        return serverTracker;
    }

    @Override public @NonNull String getPrefix() { return PREFIX; }
    @Override public @NonNull String identify() { return PREFIX + "@"; }
    @Override public AbstractTag getAttribute(@NonNull Attribute attribute) { return PROCESSOR.process(this, attribute); }
    @Override public @NonNull TagProcessor<ServerTag> getProcessor() { return PROCESSOR; }
    @Override public @NonNull String getTestValue() { return "server@"; }
}