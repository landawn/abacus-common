package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class StreamDeferredTailReview20261009Test extends TestBase {

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void bufferedJoinClosesTailCreatedAfterShortCircuit(final boolean streamRightSide) throws InterruptedException {
        final CountDownLatch tailStarted = new CountDownLatch(1);
        final CountDownLatch releaseTail = new CountDownLatch(1);
        final CountDownLatch tailObserved = new CountDownLatch(1);
        final AtomicInteger tailCloses = new AtomicInteger();
        final AtomicInteger sourceCloses = new AtomicInteger();
        final Function<Iterator<Integer>, Stream<Integer>> tailMapper = remaining -> {
            tailStarted.countDown();
            await(releaseTail);

            return Stream.of(new ObjIteratorEx<Integer>() {
                @Override
                public boolean hasNext() {
                    tailObserved.countDown();
                    return remaining.hasNext();
                }

                @Override
                public Integer next() {
                    return remaining.next();
                }
            }).onClose(() -> {
                tailCloses.incrementAndGet();
                tailObserved.countDown();
            });
        };

        final Stream<Integer> source = join(streamRightSide, tailMapper).onClose(sourceCloses::incrementAndGet);

        try {
            // Hold the consumer until its buffered reader is inside the tail factory, then let first() close the join.
            assertEquals(1, source.buffered(1).onEach(value -> await(tailStarted)).first().orElseThrow());
            assertEquals(1, sourceCloses.get());
        } finally {
            releaseTail.countDown();
            source.close();
        }

        assertTrue(tailObserved.await(5, TimeUnit.SECONDS), "the buffered reader did not finish creating its tail");
        assertEquals(1, tailCloses.get(), "a tail returned after the join closes must still be closed");
        source.close();
        assertEquals(1, tailCloses.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void fullyConsumedJoinClosesTailExactlyOnce(final boolean streamRightSide) {
        final AtomicInteger tailCloses = new AtomicInteger();
        final Stream<Integer> source = join(streamRightSide, remaining -> Stream.of(remaining).onClose(tailCloses::incrementAndGet));

        assertEquals(List.of(1, 2, 3), source.toList());
        assertEquals(1, tailCloses.get());
        source.close();
        assertEquals(1, tailCloses.get());
    }

    @Test
    void closingAnUntraversedJoinDoesNotCreateTheTail() {
        final AtomicInteger tailCalls = new AtomicInteger();
        final AtomicInteger rightCloses = new AtomicInteger();
        final Stream<Integer> source = Stream.<Integer> empty()
                .joinByRange(Stream.of(1, 2).onClose(rightCloses::incrementAndGet), Integer::equals, Collectors.toList(), (left, matches) -> left,
                        remaining -> {
                            tailCalls.incrementAndGet();
                            return Stream.of(remaining);
                        });

        source.close();
        assertEquals(0, tailCalls.get());
        assertEquals(1, rightCloses.get());
    }

    private static Stream<Integer> join(final boolean streamRightSide, final Function<Iterator<Integer>, Stream<Integer>> tailMapper) {
        if (streamRightSide) {
            return Stream.of(1).joinByRange(Stream.of(1, 2, 3), Integer::equals, Collectors.toList(), (left, matches) -> left, tailMapper);
        }

        return Stream.of(1).joinByRange(List.of(1, 2, 3).iterator(), Integer::equals, Collectors.toList(), (left, matches) -> left, tailMapper);
    }

    private static void await(final CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "timed out coordinating the buffered reader");
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
