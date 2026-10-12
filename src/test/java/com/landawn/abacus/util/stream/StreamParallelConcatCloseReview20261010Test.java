package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.stream.ObjIteratorEx.BufferedIterator;

/**
 * Close and failure behaviour of the buffered iterators behind {@code parallelConcat} and {@code buffered}. The
 * release of queued values after close is checked separately by the slow-test class
 * StreamParallelConcatMemoryReview20261010Test.
 */
class StreamParallelConcatCloseReview20261010Test extends TestBase {
    @Test
    void closingStopsTraversalEvenWithQueuedValues() {
        final CountDownLatch queued = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final Iterator<Object> source = new Iterator<>() {
            private int count;

            @Override
            public boolean hasNext() {
                if (count == 4) {
                    queued.countDown();
                    await(release);
                }
                return count < 4;
            }

            @Override
            public Object next() {
                count++;
                return new Object();
            }
        };
        final Stream<Object> combined = Stream.parallelConcat(List.of(Stream.of(source)), 1, 8);
        try {
            final ObjIteratorEx<Object> iterator = combined.iteratorEx();
            assertTrue(iterator.hasNext());
            await(queued);
            combined.close();
            assertFalse(iterator.hasNext());
            assertThrows(NoSuchElementException.class, iterator::next);
        } finally {
            release.countDown();
            combined.close();
        }
    }

    @Test
    void sharedBufferCloseBetweenHasNextAndNextDoesNotFabricateNull() {
        final BufferedIterator<Object> buffered = spy(Stream.buffered(List.of(new Object()).iterator(), new ArrayBlockingQueue<>(2), null, true));
        try {
            doAnswer(invocation -> {
                final boolean result = (boolean) invocation.callRealMethod();
                if (result) {
                    buffered.closeResource();
                }
                return result;
            }).when(buffered).hasNext();
            assertThrows(NoSuchElementException.class, buffered::next);
        } finally {
            buffered.closeResource();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void nullValuesAndPrefixBeforeWorkerFailureArePreserved(final boolean streamSources) {
        final Object value = new Object();
        final IllegalStateException failure = new IllegalStateException("worker");
        final Iterator<Object> source = new Iterator<>() {
            private int count;

            @Override
            public boolean hasNext() {
                if (count == 3) {
                    throw failure;
                }
                return true;
            }

            @Override
            public Object next() {
                return count++ == 1 ? value : null;
            }
        };
        try (Stream<Object> stream = streamSources ? Stream.parallelConcat(List.of(Stream.of(source)), 1, 4)
                : Stream.parallelConcatIterators(List.of(source), 1, 4)) {
            final ObjIteratorEx<Object> iterator = stream.iteratorEx();
            assertEquals(Arrays.asList(null, value, null), Arrays.asList(iterator.next(), iterator.next(), iterator.next()));
            assertSame(failure, assertThrows(IllegalStateException.class, iterator::hasNext));
        }
    }

    private static void await(final CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "latch timed out");
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
