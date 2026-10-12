package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Pair;

@Tag("unit")
class JoinByRangeAcquisitionPass2Review20261010Test extends TestBase {
    @ParameterizedTest
    @CsvSource({ "false, false", "true, false", "false, true", "true, true" })
    void acquisitionFailureClosesBothInputsAndPreservesTheOriginal(final boolean error, final boolean sharedFailure) {
        final Throwable failure = error ? new AssertionError("iterator acquisition") : new IllegalStateException("iterator acquisition");
        final Throwable leftCloseFailure = sharedFailure ? failure : new IllegalArgumentException("left close");
        final Throwable rightCloseFailure = sharedFailure ? failure : new IllegalArgumentException("right close");
        final AtomicInteger acquisitions = new AtomicInteger();
        final AtomicInteger reads = new AtomicInteger();
        final AtomicInteger predicates = new AtomicInteger();
        final List<String> closes = new ArrayList<>();
        final Stream<Integer> left = Stream.of(1, 2).onClose(() -> {
            closes.add("left");
            throwUnchecked(leftCloseFailure);
        });
        final Stream<Integer> right = new AcquisitionStream(List.of(1, 2), acquisitions, reads, failure).onClose(() -> {
            closes.add("right");
            throwUnchecked(rightCloseFailure);
        });

        try {
            assertSame(failure, assertThrows(Throwable.class, () -> left.joinByRange(right, (a, b) -> {
                predicates.incrementAndGet();
                return a.equals(b);
            })));
            assertEquals(1, acquisitions.get());
            assertEquals(0, reads.get());
            assertEquals(0, predicates.get());
            assertEquals(List.of("left", "right"), closes);
            assertEquals(sharedFailure ? List.of() : List.of(leftCloseFailure, rightCloseFailure), Arrays.asList(failure.getSuppressed()));
            assertTrue(left.isClosed());
            assertTrue(right.isClosed());
            left.close();
            right.close();
            assertEquals(List.of("left", "right"), closes);
        } finally {
            closeAfterAssertion(left, right);
        }
    }

    @Test
    void validationClosesReceiverWithoutAcquiringTheIndependentRightInput() {
        final AtomicInteger acquisitions = new AtomicInteger();
        final AtomicInteger reads = new AtomicInteger();
        final AtomicInteger leftCloses = new AtomicInteger();
        final AtomicInteger rightCloses = new AtomicInteger();
        final IllegalArgumentException closeFailure = new IllegalArgumentException("left close");
        final Stream<Integer> left = Stream.of(1, 2).onClose(() -> {
            leftCloses.incrementAndGet();
            throw closeFailure;
        });
        final Stream<Integer> right = new AcquisitionStream(List.of(1, 2), acquisitions, reads, null).onClose(rightCloses::incrementAndGet);

        try {
            final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> left.joinByRange(right, null));
            assertTrue(failure.getMessage().contains("predicate"));
            assertEquals(List.of(closeFailure), Arrays.asList(failure.getSuppressed()));
            assertEquals(1, leftCloses.get());
            assertEquals(0, acquisitions.get());
            assertEquals(0, reads.get());
            assertEquals(0, rightCloses.get());
            assertEquals(List.of(1, 2), right.toList());
            assertEquals(1, rightCloses.get());
        } finally {
            closeAfterAssertion(left, right);
        }
    }

    @Test
    void successfulAcquisitionKeepsTraversalLazyAndPreservesNullGroups() {
        final AtomicInteger acquisitions = new AtomicInteger();
        final AtomicInteger reads = new AtomicInteger();
        final AtomicInteger leftCloses = new AtomicInteger();
        final AtomicInteger rightCloses = new AtomicInteger();
        final Stream<Integer> left = Stream.of(1, 2).onClose(leftCloses::incrementAndGet);
        final Stream<Integer> right = new AcquisitionStream(Arrays.asList(null, 1, 2), acquisitions, reads, null).onClose(rightCloses::incrementAndGet);

        try (Stream<Pair<Integer, List<Integer>>> joined = left.joinByRange(right, (a, b) -> b == null || a.equals(b))) {
            assertEquals(1, acquisitions.get());
            assertEquals(0, reads.get());
            assertEquals(0, leftCloses.get());
            assertEquals(0, rightCloses.get());
            assertEquals(List.of(Pair.of(1, Arrays.asList(null, 1)), Pair.of(2, List.of(2))), joined.toList());
        }
        assertEquals(3, reads.get());
        assertEquals(1, leftCloses.get());
        assertEquals(1, rightCloses.get());
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
        // Also release sources when testing the old construction path, which left them open.
        for (final Stream<?> stream : streams) {
            try {
                stream.close();
            } catch (final Throwable ignored) {
            }
        }
    }
}
