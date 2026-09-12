package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class CountByTest extends TestBase {

    @Test
    void countByWorksWithCheckedMapsAndPreservesEncounterOrder() {
        final List<String> values = Arrays.asList("日本", "café", "日本", null, null);
        for (final boolean iterator : new boolean[] { false, true }) {
            final Map<String, Integer> result = count(values, Function.identity(), CountByTest::checkedMap, iterator);
            assertEquals(2, result.get("日本"));
            assertEquals(1, result.get("café"));
            assertEquals(2, result.get(null));
            assertEquals(Arrays.asList("日本", "café", null), new ArrayList<>(result.keySet()));
        }
    }

    @Test
    void extractionFailureLeavesOnlyIntegerCountsInTheSuppliedMap() {
        for (final boolean iterator : new boolean[] { false, true }) {
            final Map<String, Integer> retained = checkedMap();
            final IllegalStateException failure = new IllegalStateException("extractor failed");
            final Function<String, String> extractor = value -> {
                if (value.equals("fail")) {
                    throw failure;
                }
                return value;
            };
            assertSame(failure, assertThrows(IllegalStateException.class, () -> count(List.of("a", "a", "fail"), extractor, () -> retained, iterator)));
            assertEquals(Map.of("a", 2), retained);
            assertEquals(Integer.class, ((Map<?, ?>) retained).get("a").getClass());
        }
    }

    @Test
    void countByUsesTheSuppliedMapsKeySemantics() {
        final String first = new String("same");
        final String second = new String("same");
        for (final boolean iterator : new boolean[] { false, true }) {
            final Map<String, Integer> identity = count(List.of(first, second, first), Function.identity(), IdentityHashMap::new, iterator);
            assertEquals(2, identity.size());
            assertEquals(2, identity.get(first));
            assertEquals(1, identity.get(second));
            final Map<String, Integer> sorted = count(List.of("b", "a", "A"), Function.identity(), () -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER),
                    iterator);
            assertEquals(Map.of("a", 2, "b", 1), sorted);
            assertEquals(List.of("a", "b"), new ArrayList<>(sorted.keySet()));
        }
    }

    @Test
    void countByChecksOverflowAndSupportsExistingCounts() {
        for (final boolean iterator : new boolean[] { false, true }) {
            final Map<String, Integer> retained = checkedMap();
            retained.put("a", Integer.MAX_VALUE - 1);
            retained.put("null", null);
            assertSame(retained, count(List.of("a", "null"), Function.identity(), () -> retained, iterator));
            assertEquals(Integer.MAX_VALUE, retained.get("a"));
            assertEquals(1, retained.get("null"));
            assertThrows(ArithmeticException.class, () -> count(List.of("a"), Function.identity(), () -> retained, iterator));
            assertEquals(Integer.MAX_VALUE, retained.get("a"));
            assertEquals(1, retained.get("null"));
        }
    }

    @Test
    void countByPreservesNullEmptyAndCallbackValidation() {
        final Function<String, String> identity = Function.identity();
        assertTrue(N.countBy((Iterable<String>) null, identity).isEmpty());
        assertTrue(N.countBy((Iterator<String>) null, identity).isEmpty());
        assertTrue(N.countBy(List.<String> of(), identity).isEmpty());
        assertTrue(N.countBy(Collections.<String> emptyIterator(), identity).isEmpty());
        for (final boolean iterator : new boolean[] { false, true }) {
            final Map<String, Integer> empty = Map.of();
            assertSame(empty, count(List.of(), value -> {
                throw new AssertionError();
            }, () -> empty, iterator));
        }
        assertThrows(IllegalArgumentException.class, () -> N.countBy(List.<String> of(), null));
        assertThrows(IllegalArgumentException.class, () -> N.countBy(Collections.<String> emptyIterator(), null));
        assertThrows(IllegalArgumentException.class, () -> N.countBy(List.<String> of(), identity, null));
        assertThrows(IllegalArgumentException.class, () -> N.countBy(Collections.<String> emptyIterator(), identity, null));
    }

    private static Map<String, Integer> checkedMap() {
        return Collections.checkedMap(new LinkedHashMap<>(), String.class, Integer.class);
    }

    private static Map<String, Integer> count(final List<String> values, final Function<String, String> extractor,
            final Supplier<Map<String, Integer>> supplier, final boolean iterator) {
        return iterator ? N.countBy(values.iterator(), extractor, supplier) : N.countBy(values, extractor, supplier);
    }
}
