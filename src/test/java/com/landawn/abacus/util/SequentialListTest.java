package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class SequentialListTest extends TestBase {
    @Test
    void growingFillUsesOneTraversalAndRetainsGapAndRangeRules() {
        CountingList<Integer> list = new CountingList<>(Collections.nCopies(10_000, 1));
        CommonUtil.fill(list, 0, 10_001, 2);
        assertEquals(Collections.nCopies(10_001, 2), list);
        assertEquals(0, list.indexedSets);
        assertTrue(list.positioningCost <= 1);
        LinkedList<String> gap = new LinkedList<>(List.of("a"));
        CommonUtil.fill(gap, 3, 3, "unused");
        assertEquals(Arrays.asList("a", null, null), gap);
        CommonUtil.fill(gap, 2, 5, "\u4e2d\ud83d\ude00");
        assertEquals(Arrays.asList("a", null, "\u4e2d\ud83d\ude00", "\u4e2d\ud83d\ude00", "\u4e2d\ud83d\ude00"), gap);
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.fill((List<Object>) null, 0, 0, null));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.fill(gap, -1, 0, null));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.fill(gap, 2, 1, null));
        List<String> fixed = Arrays.asList("a", "b");
        assertThrows(UnsupportedOperationException.class, () -> CommonUtil.fill(fixed, 0, 3, "x"));
        assertEquals(List.of("x", "x"), fixed);
    }

    @Test
    void splitVariantsTraverseSequentialInputsOnceAndCopyChunks() {
        List<Integer> values = IntStream.range(0, 10_000).boxed().toList();
        CountingList<Integer> sequential = new CountingList<>(values);
        assertEquals(N.split(values, 123, 9_876, 1), N.split(sequential, 123, 9_876, 1));
        assertEquals(0, sequential.indexedGets);
        assertTrue(sequential.positioningCost <= 1);
        for (boolean smallerFirst : new boolean[] { false, true }) {
            sequential.positioningCost = 0;
            assertEquals(N.splitByChunkCount(values, 9_999, smallerFirst), N.splitByChunkCount(sequential, 9_999, smallerFirst));
            assertTrue(sequential.positioningCost <= 1);
        }
        LinkedList<String> source = new LinkedList<>(Arrays.asList(null, "\u4e2d", "\ud83d\ude00"));
        List<List<String>> chunks = N.split(source, 2);
        source.clear();
        assertEquals(Arrays.asList(null, "\u4e2d"), chunks.get(0));
        chunks.get(1).add("tail");
        assertEquals(List.of("\ud83d\ude00", "tail"), chunks.get(1));
        assertTrue(N.split((Collection<Object>) null, 0, 0, 1).isEmpty());
        assertTrue(N.splitByChunkCount((Collection<Object>) null, 1).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> N.split(List.of(), 0));
        assertThrows(IllegalArgumentException.class, () -> N.splitByChunkCount(List.of(), 0));
        assertThrows(IndexOutOfBoundsException.class, () -> N.split(List.of(1), 0, 2, 1));
    }

    @Test
    void replacementIsLinearAndPreservesPredicateOrderAndPartialFailure() {
        CountingList<Integer> list = new CountingList<>(Collections.nCopies(10_000, 1));
        assertEquals(10_000, N.replaceIf(list, v -> true, 2));
        assertEquals(Collections.nCopies(10_000, 2), list);
        assertEquals(0, list.indexedGets);
        assertEquals(0, list.indexedSets);
        List<String> visited = new ArrayList<>();
        LinkedList<String> words = new LinkedList<>(Arrays.asList(null, "\u4e2d", "stop", "last"));
        IllegalStateException failure = new IllegalStateException("predicate");
        assertSame(failure, assertThrows(IllegalStateException.class, () -> N.replaceIf(words, value -> {
            visited.add(value);
            if ("stop".equals(value)) {
                throw failure;
            }
            return true;
        }, "\ud83d\ude00")));
        assertEquals(Arrays.asList(null, "\u4e2d", "stop"), visited);
        assertEquals(List.of("\ud83d\ude00", "\ud83d\ude00", "stop", "last"), words);
        assertEquals(0, N.replaceIf((List<Object>) null, value -> true, null));
        assertThrows(IllegalArgumentException.class, () -> N.replaceIf(List.of(), null, null));
        assertEquals(0, N.replaceIf(Collections.unmodifiableList(new LinkedList<>(List.of(1))), v -> false, 2));
    }

    @Test
    void randomAccessListsRetainSupportedMutationAndCopyPaths() {
        CopyOnWriteArrayList<Integer> list = new CopyOnWriteArrayList<>(List.of(1, 2, 3));
        CommonUtil.fill(list, 1, 4, 4);
        assertEquals(List.of(1, 4, 4, 4), list);
        assertEquals(3, N.replaceIf(list, v -> v == 4, 2));
        assertEquals(List.of(List.of(1, 2), List.of(2, 2)), N.split(list, 2));
        assertEquals(List.of(List.of(1, 2), List.of(2, 2)), N.splitByChunkCount(list, 2));
    }

    private static final class CountingList<T> extends LinkedList<T> {
        int indexedGets;
        int indexedSets;
        long positioningCost;

        CountingList(Collection<? extends T> values) {
            super(values);
        }

        @Override
        public T get(int index) {
            indexedGets++;
            return super.get(index);
        }

        @Override
        public T set(int index, T value) {
            indexedSets++;
            return super.set(index, value);
        }

        @Override
        public ListIterator<T> listIterator(int index) {
            positioningCost += Math.min(index, size() - index);
            return super.listIterator(index);
        }
    }
}
