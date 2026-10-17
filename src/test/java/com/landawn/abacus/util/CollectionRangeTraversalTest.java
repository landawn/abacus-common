package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;

public class CollectionRangeTraversalTest extends TestBase {
    static List<Collection<Integer>> collections() {
        List<Integer> values = List.of(1, 2, 3, 4, 5);
        return List.of(new ArrayList<>(values), new LinkedList<>(values), new ArrayDeque<>(values), new LinkedHashSet<>(values),
                Collections.unmodifiableCollection(new LinkedHashSet<>(values)));
    }

    @ParameterizedTest
    @MethodSource("collections")
    void forwardAndReverseRangesPreserveSourceIndices(Collection<Integer> collection) throws Exception {
        List<Integer> source = new ArrayList<>(collection);
        for (int[] range : new int[][] { { 0, 0 }, { 0, 5 }, { 1, 4 }, { 4, -1 }, { 5, -1 }, { 3, 1 }, { 5, 4 } }) {
            List<Integer> expectedIndices = new ArrayList<>();
            if (range[0] <= range[1])
                for (int i = range[0]; i < range[1]; i++)
                    expectedIndices.add(i);
            else
                for (int i = Math.min(range[0], source.size() - 1); i > range[1]; i--)
                    expectedIndices.add(i);
            List<Integer> expectedValues = expectedIndices.stream().map(source::get).toList();
            List<Integer> actual = new ArrayList<>();
            N.forEach(collection, range[0], range[1], actual::add);
            assertEquals(expectedValues, actual);
            actual.clear();
            List<Integer> actualIndices = new ArrayList<>();
            N.forEachIndexed(collection, range[0], range[1], (i, value) -> {
                actualIndices.add(i);
                actual.add(value);
            });
            assertEquals(expectedValues, actual);
            assertEquals(expectedIndices, actualIndices);
            assertEquals(source, new ArrayList<>(collection));
        }
    }

    @ParameterizedTest
    @MethodSource("collections")
    void suffixAndReverseSearchWorkForEveryCollectionShape(Collection<Integer> collection) {
        assertEquals(List.of(4, 5), N.lastElements(collection, 2));
        assertEquals(List.of(), N.lastElements(collection, 0));
        assertEquals(new ArrayList<>(collection), N.lastElements(collection, 20));
        Iterable<Integer> iterable = collection::iterator;
        assertEquals(List.of(4, 5), N.lastElements(iterable, 2));
        assertEquals(new ArrayList<>(collection), N.lastElements(iterable, 20));
        assertEquals(3, N.findLastIndex(collection, v -> v % 2 == 0).get());
        assertTrue(N.findLastIndex(collection, v -> v > 10).isEmpty());
        assertEquals(4, N.findLast(iterable, v -> v % 2 == 0).get());
    }
}
