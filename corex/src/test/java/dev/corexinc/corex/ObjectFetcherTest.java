package dev.corexinc.corex;

import dev.corexinc.corex.engine.tags.ObjectFetcher;
import dev.corexinc.corex.environment.tags.core.ElementTag;
import dev.corexinc.corex.environment.tags.core.ListTag;
import dev.corexinc.corex.testing.CorexTestEnvironment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class ObjectFetcherTest {

    @BeforeAll
    public static void setup() {
        CorexTestEnvironment.bootstrap();
    }

    /** A fetcher that throws on the text must not blow up the caller - it just isn't that type. */
    @Test
    public void throwingFetcherFallsBackToText() {
        String raw = "p@" + UUID.randomUUID() + "|e@" + UUID.randomUUID();
        assertInstanceOf(ElementTag.class, ObjectFetcher.pickObject(raw));
        assertEquals(raw, ObjectFetcher.pickObject(raw).identify());
    }

    @Test
    public void workingFetcherStillWins() {
        assertInstanceOf(ListTag.class, ObjectFetcher.pickObject("li@a|b"));
    }
}
