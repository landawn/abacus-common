package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class PowerSetTest extends TestBase {

    @Test
    void equalIdentityElementsUseContiguousBitsAndTheFirstRepresentative() {
        final Set<String> input = Collections.newSetFromMap(new IdentityHashMap<>());
        input.add(new String("same"));
        input.add(new String("same"));
        input.add(new String("same"));
        final String first = input.iterator().next();
        final Set<Set<String>> result = Iterables.powerSet(input);
        assertEquals(2, result.size());
        final List<Set<String>> subsets = new ArrayList<>(result);
        assertEquals(Set.of(Set.of(), Set.of("same")), new HashSet<>(subsets));
        for (final Set<String> subset : subsets) {
            assertEquals(subset.size(), new ArrayList<>(subset).size());
            if (!subset.isEmpty()) {
                assertSame(first, subset.iterator().next());
                assertTrue(subset.contains(new String("same")));
            }
        }
    }

    @Test
    void mixedIdentityDuplicatesProduceEverySubsetExactlyOnce() {
        final Set<String> input = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i = 0; i < 4; i++) {
            input.add(new String("a"));
            input.add(new String("b"));
        }
        final Set<Set<String>> result = Iterables.powerSet(input);
        final Set<Set<String>> expected = Set.of(Set.of(), Set.of("a"), Set.of("b"), Set.of("a", "b"));
        assertEquals(4, result.size());
        assertEquals(expected, result);
        assertEquals(expected.hashCode(), result.hashCode());
        assertEquals(expected, new HashSet<>(result));
        for (final Set<String> subset : result) {
            assertTrue(result.contains(subset));
            assertEquals(subset.size(), new ArrayList<>(subset).size());
        }
    }

    @Test
    void powerSetPreservesNullUnicodeAndItsInputSnapshot() {
        final String value = "\u65E5\u672C\uD83D\uDE00";
        final Set<String> input = new LinkedHashSet<>(Arrays.asList(null, value));
        final Set<Set<String>> result = Iterables.powerSet(input);
        input.clear();
        assertEquals(4, result.size());
        final Set<String> both = new LinkedHashSet<>(Arrays.asList(null, value));
        assertTrue(result.contains(both));
        for (final Set<String> subset : result) {
            if (subset.size() == 2) {
                assertEquals(Arrays.asList(null, value), new ArrayList<>(subset));
            }
        }
        assertEquals(Set.of(Set.of()), Iterables.powerSet(null));
        assertEquals(Set.of(Set.of()), Iterables.powerSet(Set.of()));
    }

    @Test
    void theRawInputLimitRemainsThirtyElements() {
        final Set<Integer> thirty = new LinkedHashSet<>();
        for (int i = 0; i < 30; i++) {
            thirty.add(i);
        }
        assertEquals(1 << 30, Iterables.powerSet(thirty).size());
        thirty.add(30);
        assertThrows(IllegalArgumentException.class, () -> Iterables.powerSet(thirty));
        final Set<String> duplicates = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int i = 0; i < 31; i++) {
            duplicates.add(new String("same"));
        }
        assertThrows(IllegalArgumentException.class, () -> Iterables.powerSet(duplicates));
    }
}
