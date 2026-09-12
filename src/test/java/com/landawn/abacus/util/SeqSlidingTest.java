package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.stream.Collectors;

public class SeqSlidingTest extends SeqTestSupport {

    @Test
    public void testSliding() throws Exception {
        assertEquals(Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(2, 3, 4), Arrays.asList(3, 4, 5)), Seq.of(1, 2, 3, 4, 5).sliding(3).toList());
        assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4), Arrays.asList(5, 6)), Seq.of(1, 2, 3, 4, 5, 6).sliding(2, 2).toList());
        assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(4, 5)), Seq.of(1, 2, 3, 4, 5, 6).sliding(2, 3).toList());
        assertEquals(Arrays.asList(Arrays.asList(1)), Seq.of(1).sliding(2, 1).toList());
        assertTrue(Seq.<Integer, Exception> empty().sliding(2).toList().isEmpty());
        assertEquals(Arrays.asList(Arrays.asList("c", "d"), Arrays.asList("d", "e")), Seq.of("a", "b", "c", "d", "e").sliding(2).skip(2).toList());
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).sliding(0));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).sliding(2, 0));
        Seq<Integer, Exception> closed = Seq.of(1, 2, 3);
        closed.close();
        assertThrows(IllegalStateException.class, () -> closed.sliding(2, 1, IntFunctions.ofList()));
    }

    @Test
    public void testSliding_CollectionFactory() throws Exception {
        List<List<Integer>> windows = Seq.of(1, 2, 3, 4).sliding(2, 1, IntFunctions.ofList()).toList();
        assertEquals(Arrays.asList(Arrays.asList(1, 2), Arrays.asList(2, 3), Arrays.asList(3, 4)), windows);
        assertTrue(Seq.of(1, 2, 3, 4).sliding(2, 1, IntFunctions.ofLinkedList()).toList().get(0) instanceof LinkedList);
        assertEquals(new HashSet<>(Arrays.asList(1, 2)), Seq.of(1, 2, 3, 4).sliding(2, 1, HashSet::new).toList().get(0));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).sliding(2, 1, (java.util.function.IntFunction) null).toList());
    }

    @Test
    public void testSliding_Collector() throws Exception {
        assertEquals(Arrays.asList("1,2,3", "2,3,4", "3,4,5"),
                Seq.of(1, 2, 3, 4, 5).sliding(3, Collectors.mapping(String::valueOf, Collectors.joining(","))).toList());
        assertEquals(Arrays.asList(3L, 3L, 3L), Seq.of(1, 2, 3, 4, 5, 6, 7).sliding(3, 2, Collectors.counting()).toList());
        assertEquals(Arrays.asList(6, 12), Seq.of(1, 2, 3, 4, 5).sliding(3, 2, Collectors.summingInt(Integer::intValue)).toList());
        assertEquals(Arrays.asList("1,2", "4,5"), Seq.of(1, 2, 3, 4, 5, 6).map(String::valueOf).sliding(2, 3, Collectors.joining(",")).toList());
        assertEquals(Collections.singletonList("1"), Seq.of(1).map(String::valueOf).sliding(2, 1, Collectors.joining(",")).toList());
        assertTrue(Seq.<Integer, Exception> empty().sliding(2, Collectors.toList()).toList().isEmpty());
    }

    @Test
    public void testSliding_CountAfterPartialIteration() throws Exception {
        final Throwables.Iterator<List<Integer>, Exception> collectionWindows = Seq.<Integer, Exception> of(1, 2, 3, 4, 5, 6).sliding(2, 3).iteratorEx();
        assertEquals(Arrays.asList(1, 2), collectionWindows.next());
        assertEquals(1, collectionWindows.count());
        assertFalse(collectionWindows.hasNext());

        for (int sourceSize = 0; sourceSize <= 8; sourceSize++) {
            final List<Integer> source = new ArrayList<>();
            for (int i = 0; i < sourceSize; i++) {
                source.add(i);
            }
            for (int windowSize = 1; windowSize <= 4; windowSize++) {
                for (int increment = 1; increment <= 5; increment++) {
                    final List<List<Integer>> expected = Seq.<Integer, Exception> of(source).sliding(windowSize, increment).toList();
                    for (int consumed = 0; consumed <= expected.size(); consumed++) {
                        final Throwables.Iterator<List<Integer>, Exception> iter = Seq.<Integer, Exception> of(source)
                                .sliding(windowSize, increment)
                                .iteratorEx();
                        for (int i = 0; i < consumed; i++) {
                            assertEquals(expected.get(i), iter.next());
                        }
                        assertEquals(expected.size() - consumed, iter.count());
                        assertFalse(iter.hasNext());
                    }
                }
            }
        }
    }

    @Test
    public void testSlidingMap() throws Exception {
        assertEquals(Arrays.asList(3, 5, 7), Seq.of(1, 2, 3, 4).slidingMap((a, b) -> a + b).toList());
        assertEquals(Collections.singletonList("1null"),
                Seq.of(1).map(String::valueOf).slidingMap((a, b) -> (a == null ? "null" : a) + (b == null ? "null" : b)).toList());
        assertTrue(Seq.<Integer, Exception> empty().slidingMap((a, b) -> a + b).toList().isEmpty());
        assertEquals(Arrays.asList("12", "34", "5null"),
                Seq.of(1, 2, 3, 4, 5).map(String::valueOf).slidingMap(2, (a, b) -> (a == null ? "null" : a) + (b == null ? "null" : b)).toList());
        assertEquals(Arrays.asList("12", "34"),
                Seq.of(1, 2, 3, 4, 5).map(String::valueOf).slidingMap(2, true, (a, b) -> (a == null ? "null" : a) + (b == null ? "null" : b)).toList());
        assertEquals(Arrays.asList("cd", "de"), Seq.of("a", "b", "c", "d", "e").slidingMap((a, b) -> a + (b == null ? "" : b)).skip(2).toList());
    }

    @Test
    public void testSlidingMap_TriFunction() throws Exception {
        assertEquals(Arrays.asList(6, 9, 12), Seq.of(1, 2, 3, 4, 5).slidingMap((a, b, c) -> a + b + c).toList());
        assertEquals(Collections.singletonList("1nn"),
                Seq.of(1).slidingMap(2, (a, b, c) -> (a == null ? "n" : a) + "" + (b == null ? "n" : b) + "" + (c == null ? "n" : c)).toList());
        assertEquals(Arrays.asList("123", "345", "567"),
                Seq.of(1, 2, 3, 4, 5, 6, 7)
                        .slidingMap(2, (a, b, c) -> (a == null ? "n" : a) + "" + (b == null ? "n" : b) + "" + (c == null ? "n" : c))
                        .toList());
        assertEquals(Arrays.asList("123", "345"),
                Seq.of(1, 2, 3, 4, 5, 6)
                        .slidingMap(2, true, (a, b, c) -> (a == null ? "n" : a) + "" + (b == null ? "n" : b) + "" + (c == null ? "n" : c))
                        .toList());
        assertEquals(Arrays.asList(6, 15), Seq.of(1, 2, 3, 4, 5, 6, 7).slidingMap(3, true, (a, b, c) -> a + b + c).toList());
        assertTrue(Seq.of(1, 2).slidingMap(1, true, (a, b, c) -> a + b + c).toList().isEmpty());
    }
}
