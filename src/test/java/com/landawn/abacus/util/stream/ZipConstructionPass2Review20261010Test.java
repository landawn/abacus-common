package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.function.Function;

@Tag("unit")
class ZipConstructionPass2Review20261010Test extends TestBase {
    @ParameterizedTest
    @CsvSource({ "0, false, false", "0, true, false", "0, false, true", "0, true, true", "1, false, false", "1, true, false", "1, false, true", "1, true, true",
            "3, false, false", "3, true, false", "3, false, true", "3, true, true" })
    void defaultsSnapshotFailureClosesAcquiredInputs(final int threads, final boolean error, final boolean sharedFailure) {
        final Throwable failure = error ? new AssertionError("defaults snapshot") : new IllegalStateException("defaults snapshot");
        final Throwable firstCloseFailure = sharedFailure ? failure : new IllegalArgumentException("first close");
        final Throwable secondCloseFailure = sharedFailure ? failure : new AssertionError("second close");
        final AtomicInteger acquisitions = new AtomicInteger();
        final AtomicInteger reads = new AtomicInteger();
        final AtomicInteger snapshots = new AtomicInteger();
        final AtomicInteger calls = new AtomicInteger();
        final List<String> closes = new ArrayList<>();
        final Stream<Integer> first = new AcquisitionStream(List.of(1), acquisitions, reads, null).onClose(() -> {
            closes.add("first");
            throwUnchecked(firstCloseFailure);
        });
        final Stream<Integer> second = new AcquisitionStream(List.of(2), acquisitions, reads, null).onClose(() -> {
            closes.add("second");
            throwUnchecked(secondCloseFailure);
        });
        final List<Integer> defaults = failingDefaults(2, snapshots, failure);

        try {
            assertSame(failure, assertThrows(Throwable.class, () -> zip(threads, List.of(first, second), defaults, values -> {
                calls.incrementAndGet();
                return new ArrayList<>(values);
            })));
            assertEquals(2, acquisitions.get());
            assertEquals(1, snapshots.get());
            assertEquals(0, reads.get());
            assertEquals(0, calls.get());
            assertEquals(List.of("first", "second"), closes);
            assertTrue(first.isClosed());
            assertTrue(second.isClosed());
            assertEquals(sharedFailure ? List.of() : List.of(firstCloseFailure, secondCloseFailure), Arrays.asList(failure.getSuppressed()));
            first.close();
            second.close();
            assertEquals(List.of("first", "second"), closes);
        } finally {
            closeAfterAssertion(first, second);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 3 })
    void validationDoesNotAcquireOrCloseInputs(final int threads) {
        final AtomicInteger acquisitions = new AtomicInteger();
        final AtomicInteger reads = new AtomicInteger();
        final AtomicInteger closes = new AtomicInteger();
        final Stream<Integer> source = new AcquisitionStream(List.of(1), acquisitions, reads, null).onClose(closes::incrementAndGet);

        try {
            assertThrows(IllegalArgumentException.class, () -> zip(threads, List.of(source), List.of(), ArrayList::new));
            assertThrows(IllegalArgumentException.class, () -> zip(threads, List.of(source), List.of(0), null));
            assertEquals(0, acquisitions.get());
            assertEquals(0, reads.get());
            assertEquals(0, closes.get());
            assertFalse(source.isClosed());
            assertEquals(List.of(1), source.toList());
            assertEquals(1, closes.get());
        } finally {
            closeAfterAssertion(source);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 3 })
    void acquisitionFailureStillClosesOnlyTheOpenedPrefix(final int threads) {
        final IllegalStateException failure = new IllegalStateException("second iterator");
        final AtomicInteger acquisitions = new AtomicInteger();
        final AtomicInteger reads = new AtomicInteger();
        final AtomicInteger snapshots = new AtomicInteger();
        final List<String> closes = new ArrayList<>();
        final Stream<Integer> first = new AcquisitionStream(List.of(1), acquisitions, reads, null).onClose(() -> closes.add("first"));
        final Stream<Integer> second = new AcquisitionStream(List.of(2), acquisitions, reads, failure).onClose(() -> closes.add("second"));
        final Stream<Integer> third = new AcquisitionStream(List.of(3), acquisitions, reads, null).onClose(() -> closes.add("third"));
        final List<Integer> defaults = failingDefaults(3, snapshots, new AssertionError("must not snapshot"));

        try {
            assertSame(failure, assertThrows(IllegalStateException.class, () -> zip(threads, List.of(first, second, third), defaults, ArrayList::new)));
            assertEquals(2, acquisitions.get());
            assertEquals(0, snapshots.get());
            assertEquals(0, reads.get());
            assertEquals(List.of("first"), closes);
            assertTrue(first.isClosed());
            assertFalse(second.isClosed());
            assertFalse(third.isClosed());
            assertEquals(List.of(3), third.toList());
        } finally {
            closeAfterAssertion(first, second, third);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 3 })
    void successSnapshotsDefaultsAndPreservesPaddedNullValues(final int threads) {
        final AtomicInteger reads = new AtomicInteger();
        final AtomicInteger acquisitions = new AtomicInteger();
        final AtomicInteger closes = new AtomicInteger();
        final Stream<Integer> first = new AcquisitionStream(Arrays.asList(1, null), acquisitions, reads, null).onClose(closes::incrementAndGet);
        final Stream<Integer> second = new AcquisitionStream(List.of(10, 20, 30), acquisitions, reads, null).onClose(closes::incrementAndGet);
        final List<Integer> defaults = new ArrayList<>(List.of(7, 8, 99));
        final List<List<Integer>> expected = Arrays.asList(Arrays.asList(1, 10, 99), Arrays.asList(null, 20, 99), Arrays.asList(7, 30, 99));

        try (Stream<List<Integer>> result = zip(threads, Arrays.asList(first, second, null), defaults, ArrayList::new)) {
            assertEquals(2, acquisitions.get());
            assertEquals(0, reads.get());
            assertEquals(0, closes.get());
            defaults.set(0, -1);
            defaults.set(2, -99);
            final List<List<Integer>> actual = result.toList();
            assertEquals(expected.size(), actual.size());
            if (threads <= 1) {
                assertEquals(expected, actual);
            } else {
                assertEquals(new HashSet<>(expected), new HashSet<>(actual));
            }
        } finally {
            closeAfterAssertion(first, second);
        }
        assertEquals(5, reads.get());
        assertEquals(2, closes.get());
    }

    private static Stream<List<Integer>> zip(final int threads, final List<Stream<Integer>> sources, final List<Integer> defaults,
            final Function<? super List<Integer>, ? extends List<Integer>> zipper) {
        return threads == 0 ? Stream.zip(sources, defaults, zipper) : Stream.parallelZip(sources, defaults, zipper, threads);
    }

    private static List<Integer> failingDefaults(final int size, final AtomicInteger snapshots, final Throwable failure) {
        return new AbstractList<>() {
            @Override
            public Integer get(final int index) {
                return 0;
            }

            @Override
            public int size() {
                return size;
            }

            @Override
            public Object[] toArray() {
                snapshots.incrementAndGet();
                throwUnchecked(failure);
                throw new AssertionError("unreachable");
            }
        };
    }

    private static final class AcquisitionStream extends IteratorStream<Integer> {
        private final AtomicInteger acquisitions;
        private final Throwable failure;

        private AcquisitionStream(final List<Integer> values, final AtomicInteger acquisitions, final AtomicInteger reads, final Throwable failure) {
            super(new ObjIterator<>() {
                private final java.util.Iterator<Integer> iterator = values.iterator();

                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public Integer next() {
                    final Integer result = iterator.next();
                    reads.incrementAndGet();
                    return result;
                }
            });
            this.acquisitions = acquisitions;
            this.failure = failure;
        }

        @Override
        ObjIteratorEx<Integer> iteratorEx() {
            acquisitions.incrementAndGet();
            if (failure != null) {
                throwUnchecked(failure);
            }
            return super.iteratorEx();
        }
    }

    private static void throwUnchecked(final Throwable failure) {
        if (failure instanceof Error error) {
            throw error;
        }
        throw (RuntimeException) failure;
    }

    private static void closeAfterAssertion(final Stream<?>... streams) {
        // Also release inputs when demonstrating the old setup path, which left them open.
        for (final Stream<?> stream : streams) {
            try {
                stream.close();
            } catch (final Throwable ignored) {
            }
        }
    }
}
