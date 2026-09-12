package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class BatchViewTest extends TestBase {

    @Test
    void listBatchesCannotPolluteTheSourceAndRemainLive() {
        for (final int size : new int[] { 2, 99 }) {
            final List<Integer> source = new ArrayList<>(List.of(1, 2, 3));
            final List<List<Number>> runBatches = new ArrayList<>();
            N.<Number, RuntimeException> runByBatch(source, size, batch -> {
                assertUnmodifiable(batch);
                runBatches.add(batch);
            });
            final List<List<Number>> callBatches = N.<Number, List<Number>, RuntimeException> callByBatch(source, size, batch -> {
                assertUnmodifiable(batch);
                return batch;
            });
            assertEquals(List.of(1, 2, 3), source);
            assertEquals(runBatches, callBatches);
            assertEquals(size == 2 ? List.of(3) : List.of(1, 2, 3), runBatches.getLast());
            source.set(0, 4);
            assertEquals(4, runBatches.getFirst().getFirst());
            assertEquals(4, callBatches.getFirst().getFirst());
        }
    }

    @Test
    void arrayBatchesHaveTheSameUnmodifiableLiveContract() {
        for (final int size : new int[] { 2, 99 }) {
            final Integer[] source = { 1, 2, 3 };
            final List<List<Number>> runBatches = new ArrayList<>();
            N.<Number, RuntimeException> runByBatch(source, size, batch -> {
                assertUnmodifiable(batch);
                runBatches.add(batch);
            });
            final List<List<Number>> callBatches = N.<Number, List<Number>, RuntimeException> callByBatch(source, size, batch -> {
                assertUnmodifiable(batch);
                return batch;
            });
            assertEquals(List.of(1, 2, 3), Arrays.asList(source));
            source[0] = 4;
            assertEquals(4, runBatches.getFirst().getFirst());
            assertEquals(4, callBatches.getFirst().getFirst());
        }
    }

    @Test
    void iteratorAndNonListIterableBatchesRemainIndependentSnapshots() {
        final List<Integer> source = new ArrayList<>(List.of(1, 2, 3));
        final List<List<Number>> runBatches = new ArrayList<>();
        N.<Number, RuntimeException> runByBatch(source.iterator(), 2, batch -> {
            assertUnmodifiable(batch);
            runBatches.add(batch);
        });
        final Iterable<Integer> iterable = source::iterator;
        final List<List<Number>> callBatches = N.<Number, List<Number>, RuntimeException> callByBatch(iterable, 2, batch -> {
            assertUnmodifiable(batch);
            return batch;
        });
        source.set(0, 4);
        assertEquals(List.of(List.of(1, 2), List.of(3)), runBatches);
        assertEquals(runBatches, callBatches);
    }

    @Test
    void batchCopiesPreserveNullUnicodeAndAllowLocalMutation() {
        final String[] source = { null, "\u65E5\u672C\uD83D\uDE00", "" };
        final List<List<String>> result = N.callByBatch(source, 2, batch -> {
            final List<String> copy = new ArrayList<>(batch);
            copy.add("local");
            return copy;
        });
        assertEquals(Arrays.asList(null, "\u65E5\u672C\uD83D\uDE00", "local"), result.getFirst());
        assertEquals(List.of("", "local"), result.getLast());
        assertEquals(Arrays.asList(null, "\u65E5\u672C\uD83D\uDE00", ""), Arrays.asList(source));
    }

    @Test
    void nullEmptyAndInvalidBatchArgumentsRetainTheirBehavior() {
        final Throwables.Consumer<List<Integer>, RuntimeException> unused = batch -> {
            throw new AssertionError();
        };
        N.runByBatch((Integer[]) null, 2, unused);
        N.runByBatch((Iterable<Integer>) null, 2, unused);
        N.runByBatch((Iterator<Integer>) null, 2, unused);
        N.runByBatch(new Integer[0], 2, unused);
        N.runByBatch(List.<Integer> of(), 2, unused);
        N.runByBatch(List.<Integer> of().iterator(), 2, unused);
        assertTrue(N.callByBatch((Integer[]) null, 2, List::size).isEmpty());
        assertTrue(N.callByBatch((Iterable<Integer>) null, 2, List::size).isEmpty());
        assertTrue(N.callByBatch((Iterator<Integer>) null, 2, List::size).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> N.runByBatch(new Integer[0], 0, unused));
        assertThrows(IllegalArgumentException.class, () -> N.callByBatch(List.<Integer> of(), -1, List::size));
        assertThrows(IllegalArgumentException.class, () -> N.runByBatch(new Integer[0], 1, null));
        assertThrows(IllegalArgumentException.class, () -> N.callByBatch(List.<Integer> of(), 1, null));
    }

    private static void assertUnmodifiable(final List<Number> batch) {
        assertThrows(UnsupportedOperationException.class, () -> batch.set(0, 1.5d));
        assertThrows(UnsupportedOperationException.class, () -> batch.add(1.5d));
        assertThrows(UnsupportedOperationException.class, batch::clear);
        final Iterator<Number> iterator = batch.iterator();
        iterator.next();
        assertThrows(UnsupportedOperationException.class, iterator::remove);
    }
}
