package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.Tag("unit")
public class WrapperSelfRenderingTest {
    @Test
    void collectionWrappersRecognizeTheirOwnIdentityIncludingReverseViews() {
        final var backing = new ArrayList<>();
        final var collection = ImmutableCollection.wrap(backing);
        backing.add(collection);
        assertEquals("[(this Collection)]", collection.toString());
        final var identity = new IdentityHashSet<>();
        identity.add(identity);
        assertEquals("[(this Collection)]", identity.toString());
        backing.clear();
        final var reverse = ImmutableList.wrap(backing).reversed();
        backing.add("\uD83D\uDE00");
        backing.add(reverse);
        assertEquals("[(this Collection), \uD83D\uDE00]", reverse.toString());
    }

    @Test
    void mapWrappersRenderDirectSelfAndKeepOrdinaryValues() {
        final var cache = new ConcurrentCacheMap<String, Object>(8);
        cache.put("self", cache);
        assertEquals("{self=(this Map)}", cache.toString());
        final var backing = new LinkedHashMap<String, Object>();
        final var immutable = ImmutableMap.wrap(backing);
        backing.put("self", immutable);
        backing.put("null", null);
        assertEquals("{self=(this Map), null=null}", immutable.toString());
        final var properties = new Properties<String, Object>();
        properties.put("self", properties);
        properties.put("text", "\uD83D\uDE00");
        assertEquals("{self=(this Map), text=\uD83D\uDE00}", properties.toString());
        assertEquals("{}", new Properties<>().toString());
        assertEquals("[]", ImmutableList.empty().toString());
    }
}
