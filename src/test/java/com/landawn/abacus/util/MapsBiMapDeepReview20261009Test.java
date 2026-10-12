package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class MapsBiMapDeepReview20261009Test extends TestBase {
    @Test
    void replaceKeysRejectsComparatorEquivalentKeysAndRestoresOriginalEntries() {
        final BiMap<String, Integer> map = caseInsensitiveMap();

        assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(map, key -> key.equals("x") ? "a" : "A"));

        assertEquals(Map.of("x", 1, "y", 2), map);
        assertEquals(Map.of(1, "x", 2, "y"), map.inverse());
    }

    @Test
    void replaceKeysMergesComparatorEquivalentKeys() {
        final BiMap<String, Integer> map = caseInsensitiveMap();
        final AtomicInteger merges = new AtomicInteger();

        Maps.replaceKeys(map, key -> key.equals("x") ? "a" : "A", (left, right) -> {
            merges.incrementAndGet();
            return left + right;
        });

        assertEquals(1, merges.get());
        assertEquals(1, map.size());
        assertEquals(3, map.get("A"));
        assertEquals("a", map.getByValue(3));
        assertFalse(map.inverse().containsKey(1));
        assertFalse(map.inverse().containsKey(2));
    }

    @Test
    void replaceKeysPreservesDistinctIdentityKeysWithEqualContents() {
        for (final boolean merge : new boolean[] { false, true }) {
            final BiMap<String, Integer> map = new BiMap<>(IdentityHashMap::new, LinkedHashMap::new);
            final String firstKey = new String("same");
            final String secondKey = new String("same");
            map.put("x", 1);
            map.put("y", 2);

            if (merge) {
                Maps.replaceKeys(map, key -> key.equals("x") ? firstKey : secondKey, Integer::sum);
            } else {
                Maps.replaceKeys(map, key -> key.equals("x") ? firstKey : secondKey);
            }

            assertEquals(2, map.size());
            assertEquals(1, map.get(firstKey));
            assertEquals(2, map.get(secondKey));
            assertSame(firstKey, map.getByValue(1));
            assertSame(secondKey, map.getByValue(2));
        }
    }

    @Test
    void replaceKeysPreservesDistinctIdentityValuesWithEqualContents() {
        for (final boolean merge : new boolean[] { false, true }) {
            final BiMap<String, String> map = new BiMap<>(LinkedHashMap::new, IdentityHashMap::new);
            final String firstValue = new String("same");
            final String secondValue = new String("same");
            map.put("x", firstValue);
            map.put("y", secondValue);

            if (merge) {
                Maps.replaceKeys(map, key -> "new-" + key, (left, right) -> left);
            } else {
                Maps.replaceKeys(map, key -> "new-" + key);
            }

            assertEquals(2, map.size());
            assertSame(firstValue, map.get("new-x"));
            assertSame(secondValue, map.get("new-y"));
            assertEquals("new-x", map.getByValue(firstValue));
            assertEquals("new-y", map.getByValue(secondValue));
        }
    }

    @Test
    void replaceKeysRestoresOriginalEntriesWhenComparatorMergeFails() {
        final BiMap<String, Integer> map = caseInsensitiveMap();
        final IllegalStateException failure = new IllegalStateException("merge failed");

        assertSame(failure, assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(map, key -> key.equals("x") ? "a" : "A", (left, right) -> {
            throw failure;
        })));

        assertEquals(Map.of("x", 1, "y", 2), map);
        assertEquals(Map.of(1, "x", 2, "y"), map.inverse());
    }

    private static BiMap<String, Integer> caseInsensitiveMap() {
        final BiMap<String, Integer> map = new BiMap<>(() -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER), LinkedHashMap::new);
        map.put("x", 1);
        map.put("y", 2);
        return map;
    }
}
