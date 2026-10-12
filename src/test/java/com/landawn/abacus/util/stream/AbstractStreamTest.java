package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Pair;

/** AbstractStream general contracts. */
public class AbstractStreamTest extends AbstractStreamTestSupport {

    @Test
    public void testHasDuplicates() {
        stream = createStream(1, 2, 3, 2, 4);
        assertTrue(stream.containsDuplicates());

        stream = createStream(1, 2, 3, 4, 5);
        assertFalse(stream.containsDuplicates());
    }

    // iterator

    @Test
    public void test_iterator() {
        Iterator<Integer> it = Stream.of(1, 2, 3).iterator();
        assertTrue(it.hasNext());
        assertEquals(1, it.next());
        assertTrue(it.hasNext());
        assertEquals(2, it.next());
        assertTrue(it.hasNext());
        assertEquals(3, it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testIterator() {
        List<Integer> input = Arrays.asList(1, 2, 3, 4, 5);
        Stream<Integer> stream = createStream(input);

        ObjIterator<Integer> iter = stream.iterator();
        List<Integer> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }

        assertEquals(input, result);
    }

    @Test
    public void testIterator_HasNextAndNext() {
        Stream<Integer> s = Stream.of(10, 20, 30);
        java.util.Iterator<Integer> iter = s.iterator();
        assertTrue(iter.hasNext());
        assertEquals(10, iter.next().intValue());
        assertTrue(iter.hasNext());
        assertEquals(20, iter.next().intValue());
        assertEquals(30, iter.next().intValue());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testIterator_method() {
        stream = createStream(1, 2, 3);
        ObjIterator<Integer> iter = stream.iterator();
        assertTrue(iter.hasNext());
        assertEquals(1, iter.next().intValue());
        assertTrue(iter.hasNext());
        assertEquals(2, iter.next().intValue());
        assertTrue(iter.hasNext());
        assertEquals(3, iter.next().intValue());
        assertFalse(iter.hasNext());
    }

    // orderedPermutations

    @Test
    public void test_orderedPermutations() {
        List<List<Integer>> result = Stream.of(1, 3, 2).orderedPermutations().toList();
        List<List<Integer>> expected = List.of(List.of(1, 2, 3), List.of(1, 3, 2), List.of(2, 1, 3), List.of(2, 3, 1), List.of(3, 1, 2), List.of(3, 2, 1));
        assertEquals(expected, result);
    }

    @Test
    public void testOrderedPermutations() {
        stream = createStream(1, 2, 3);
        Stream<List<Integer>> result = stream.orderedPermutations();
        List<List<Integer>> lists = result.toList();
        assertEquals(6, lists.size());
    }

    @Test
    public void testOrderedPermutations_withComparator() {
        stream = createStream(3, 1, 2);
        List<List<Integer>> result = stream.orderedPermutations(Comparator.naturalOrder()).toList();
        assertTrue(result.size() > 0);
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
    }

    @Test
    public void testOrderedPermutationsWithComparator() {
        stream = createStream(3, 2, 1);
        Stream<List<Integer>> result = stream.orderedPermutations(Comparator.naturalOrder());
        List<List<Integer>> lists = result.toList();
        assertEquals(6, lists.size());
    }

    @Test
    public void test_pairWith() {
        List<Pair<Integer, String>> result = Stream.of(1, 2, 3).pairWith(String::valueOf).toList();
        assertEquals(Arrays.asList(Pair.of(1, "1"), Pair.of(2, "2"), Pair.of(3, "3")), result);
    }

    @Test
    public void testPairWith() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, String>> paired = stream.pairWith(i -> "val" + i);
        List<Pair<Integer, String>> result = paired.toList();
        assertEquals(3, result.size());
        assertEquals(Pair.of(1, "val1"), result.get(0));
    }

}
