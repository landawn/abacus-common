package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@Tag("unit")
class NIndexedBatchTest {
    @ParameterizedTest
    @MethodSource("batchBoundaries")
    void indexedBatchActionsObserveFullAndPartialBatches(final long elementCount, final int batchSize, final List<Long> expected) {
        final BatchObservations run = new BatchObservations();
        final List<Long> runBatches = new ArrayList<>();
        N.runByBatch(repeatedZeros(elementCount), batchSize, run::accept, () -> runBatches.add(run.completeBatch()));

        final BatchObservations call = new BatchObservations();
        final List<Long> callBatches = N.callByBatch(repeatedZeros(elementCount), batchSize, call::accept, call::completeBatch);

        assertEquals(expected, runBatches);
        assertEquals(expected, callBatches);
        assertEquals(elementCount, run.consumed);
        assertEquals(elementCount, call.consumed);
        assertEquals((int) elementCount - 1, run.lastIndex);
        assertEquals((int) elementCount - 1, call.lastIndex);
    }

    private static Stream<Arguments> batchBoundaries() {
        return Stream.of(Arguments.of(0L, 1, List.of()), Arguments.of(0L, Integer.MAX_VALUE, List.of()), Arguments.of(1L, 1, List.of(1L)),
                Arguments.of(1L, 3, List.of(1L)), Arguments.of(6L, 1, List.of(1L, 1L, 1L, 1L, 1L, 1L)), Arguments.of(6L, 3, List.of(3L, 3L)),
                Arguments.of(7L, 3, List.of(3L, 3L, 1L)), Arguments.of(7L, 8, List.of(7L)), Arguments.of(7L, Integer.MAX_VALUE, List.of(7L)));
    }

    @ParameterizedTest
    @CsvSource({ "false, 0", "false, 1", "false, 3", "true, 0", "true, 1", "true, 3" })
    void elementFailureDoesNotCompleteTheFailedBatch(final boolean returnBatchResults, final int failedIndex) {
        final IOException failure = new IOException("element failure");
        final List<Integer> values = List.of(0, 1, 2, 3, 4);
        final Iterator<Integer> source = values.iterator();
        final List<Integer> visited = new ArrayList<>();
        final List<Integer> completed = new ArrayList<>();
        final Throwables.IntObjConsumer<Integer, IOException> consumer = (index, value) -> {
            assertEquals(index, value.intValue());
            visited.add(index);
            if (index == failedIndex) {
                throw failure;
            }
        };

        assertSame(failure, assertThrows(IOException.class, () -> {
            if (returnBatchResults) {
                N.callByBatch(source, 2, consumer, () -> completed.add(visited.size()));
            } else {
                N.runByBatch(source, 2, consumer, () -> completed.add(visited.size()));
            }
        }));

        assertEquals(values.subList(0, failedIndex + 1), visited);
        assertEquals(failedIndex < 2 ? List.of() : List.of(2), completed);
        final List<Integer> remaining = new ArrayList<>();
        source.forEachRemaining(remaining::add);
        assertEquals(values.subList(failedIndex + 1, values.size()), remaining);
    }

    @ParameterizedTest
    @CsvSource({ "false, 1", "false, 2", "false, 3", "true, 1", "true, 2", "true, 3" })
    void batchFailureDoesNotReadMoreElementsOrRepeatTheAction(final boolean returnBatchResults, final int failedBatch) {
        final IOException failure = new IOException("batch failure");
        final List<Integer> values = List.of(0, 1, 2, 3, 4);
        final Iterator<Integer> source = values.iterator();
        final List<Integer> visited = new ArrayList<>();
        final List<Integer> attempted = new ArrayList<>();
        final Throwables.IntObjConsumer<Integer, RuntimeException> consumer = (index, value) -> {
            assertEquals(index, value.intValue());
            visited.add(index);
        };
        final Throwables.Callable<Integer, IOException> action = () -> {
            attempted.add(visited.size());
            if (attempted.size() == failedBatch) {
                throw failure;
            }
            return visited.size();
        };

        assertSame(failure, assertThrows(IOException.class, () -> {
            if (returnBatchResults) {
                N.callByBatch(source, 2, consumer, action);
            } else {
                N.runByBatch(source, 2, consumer, () -> action.call());
            }
        }));

        final int consumed = Math.min(failedBatch * 2, values.size());
        assertEquals(values.subList(0, consumed), visited);
        assertEquals(List.of(2, 4, 5).subList(0, failedBatch), attempted);
        final List<Integer> remaining = new ArrayList<>();
        source.forEachRemaining(remaining::add);
        assertEquals(values.subList(consumed, values.size()), remaining);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    @Tag("slow-test")
    @Timeout(60)
    void indexOverflowDoesNotSplitPartialBatch(final boolean returnBatchResults) {
        // The lazy input crosses the int index boundary without allocating billions of elements.
        final long elementCount = (long) Integer.MAX_VALUE + 3;
        final BatchObservations observations = new BatchObservations();
        final List<Long> batches;
        if (returnBatchResults) {
            batches = N.callByBatch(repeatedZeros(elementCount), Integer.MAX_VALUE, observations::accept, observations::completeBatch);
        } else {
            batches = new ArrayList<>();
            N.runByBatch(repeatedZeros(elementCount), Integer.MAX_VALUE, observations::accept, () -> batches.add(observations.completeBatch()));
        }

        assertEquals(List.of((long) Integer.MAX_VALUE, 3L), batches);
        assertEquals(elementCount, observations.consumed);
        assertEquals(Integer.MIN_VALUE + 1, observations.lastIndex);
    }

    private static Iterator<Integer> repeatedZeros(final long count) {
        return new Iterator<>() {
            private long remaining = count;

            @Override
            public boolean hasNext() {
                return remaining > 0;
            }

            @Override
            public Integer next() {
                if (remaining == 0) {
                    throw new NoSuchElementException();
                }
                remaining--;
                return 0;
            }
        };
    }

    private static final class BatchObservations {
        private long consumed;
        private long previousBatchEnd;
        private int lastIndex = -1;

        void accept(final int index, final Integer ignored) {
            consumed++;
            lastIndex = index;
        }

        long completeBatch() {
            final long size = consumed - previousBatchEnd;
            previousBatchEnd = consumed;
            return size;
        }
    }
}
