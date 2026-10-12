package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class SeqFlatMapCleanupReview20261009Test extends TestBase {

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void closePreventsFurtherSourceReadsAndMapping(final boolean consumeFirstChild) {
        final AtomicInteger sourceChecks = new AtomicInteger();
        final AtomicInteger sourceReads = new AtomicInteger();
        final AtomicInteger mapperCalls = new AtomicInteger();
        final AtomicInteger childCloses = new AtomicInteger();
        final Seq<Integer, RuntimeException> source = Seq.of(new Throwables.Iterator<Integer, RuntimeException>() {
            @Override
            public boolean hasNext() {
                sourceChecks.incrementAndGet();
                return sourceReads.get() < 2;
            }

            @Override
            public Integer next() {
                return sourceReads.incrementAndGet();
            }
        }).flatMap(value -> {
            mapperCalls.incrementAndGet();
            return Seq.<Integer, RuntimeException> of(value).onClose(childCloses::incrementAndGet);
        });

        try (source) {
            // Retain the iterator before closing, as an already-running buffered producer would do.
            final Throwables.Iterator<Integer, RuntimeException> iter = source.iteratorEx();
            if (consumeFirstChild) {
                assertTrue(iter.hasNext());
                assertEquals(1, iter.next());
            }

            final int checksBeforeClose = sourceChecks.get();
            final int expectedCalls = consumeFirstChild ? 1 : 0;
            source.close();

            assertFalse(iter.hasNext());
            assertFalse(iter.hasNext());
            assertThrows(NoSuchElementException.class, iter::next);
            assertEquals(checksBeforeClose, sourceChecks.get(), "cancellation must also stop upstream hasNext callbacks");
            assertEquals(expectedCalls, sourceReads.get());
            assertEquals(expectedCalls, mapperCalls.get());
            assertEquals(expectedCalls, childCloses.get());
        }
    }

    @ParameterizedTest
    @CsvSource({ "false, false", "true, false", "false, true" })
    void cancellationFromTheSourceHasNextStopsAfterOneMapping(final boolean nullChild, final boolean failChildClose) {
        final AtomicReference<Seq<Integer, RuntimeException>> sourceRef = new AtomicReference<>();
        final AtomicInteger sourceChecks = new AtomicInteger();
        final AtomicInteger sourceReads = new AtomicInteger();
        final AtomicInteger mapperCalls = new AtomicInteger();
        final AtomicInteger childCloses = new AtomicInteger();
        final IllegalStateException closeFailure = new IllegalStateException("late child close");
        final Seq<Integer, RuntimeException> source = Seq.of(new Throwables.Iterator<Integer, RuntimeException>() {
            @Override
            public boolean hasNext() {
                sourceChecks.incrementAndGet();
                sourceRef.get().close();
                return true;
            }

            @Override
            public Integer next() {
                return sourceReads.incrementAndGet();
            }
        }).<Integer> flatMap(value -> {
            mapperCalls.incrementAndGet();
            if (nullChild) {
                return null;
            }

            return Seq.of(new Throwables.Iterator<Integer, RuntimeException>() {
                @Override
                public boolean hasNext() {
                    throw new AssertionError("a child returned after cancellation must not be traversed");
                }

                @Override
                public Integer next() {
                    throw new AssertionError("a child returned after cancellation must not be read");
                }

                @Override
                protected void closeResourceInternal() {
                    childCloses.incrementAndGet();
                    if (failChildClose) {
                        throw closeFailure;
                    }
                }
            });
        });
        sourceRef.set(source);

        try (source) {
            final Throwables.Iterator<Integer, RuntimeException> iter = source.iteratorEx();
            // Cancellation happens after the entry guard, so this advance may map one late child.
            if (failChildClose) {
                assertSame(closeFailure, assertThrows(IllegalStateException.class, iter::hasNext));
            } else {
                assertFalse(iter.hasNext());
            }

            assertFalse(iter.hasNext());
            assertFalse(iter.hasNext());
            assertThrows(NoSuchElementException.class, iter::next);
            assertEquals(1, sourceChecks.get());
            assertEquals(1, sourceReads.get());
            assertEquals(1, mapperCalls.get());
            assertEquals(nullChild ? 0 : 1, childCloses.get());
            source.close();
            assertEquals(nullChild ? 0 : 1, childCloses.get());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void failureAfterSourceCancellationIsPropagatedWithoutRepeatingSourceOrMapper(final boolean failSourceRead) {
        final AtomicReference<Seq<Integer, RuntimeException>> sourceRef = new AtomicReference<>();
        final AtomicInteger sourceChecks = new AtomicInteger();
        final AtomicInteger sourceReads = new AtomicInteger();
        final AtomicInteger mapperCalls = new AtomicInteger();
        final IllegalStateException failure = new IllegalStateException("in-progress advance after cancellation");
        final Seq<Integer, RuntimeException> source = Seq.of(new Throwables.Iterator<Integer, RuntimeException>() {
            @Override
            public boolean hasNext() {
                sourceChecks.incrementAndGet();
                sourceRef.get().close();
                return true;
            }

            @Override
            public Integer next() {
                sourceReads.incrementAndGet();
                if (failSourceRead) {
                    throw failure;
                }
                return 1;
            }
        }).<Integer> flatMap(value -> {
            mapperCalls.incrementAndGet();
            throw failure;
        });
        sourceRef.set(source);

        try (source) {
            final Throwables.Iterator<Integer, RuntimeException> iter = source.iteratorEx();
            assertSame(failure, assertThrows(IllegalStateException.class, iter::hasNext));
            assertFalse(iter.hasNext());
            assertFalse(iter.hasNext());
            assertThrows(NoSuchElementException.class, iter::next);
            assertEquals(1, sourceChecks.get());
            assertEquals(1, sourceReads.get());
            assertEquals(failSourceRead ? 0 : 1, mapperCalls.get());
        }
    }

    @Test
    void flatMapChecksEachChildOncePerHasNextAcrossTwoValueChildren() {
        final int[] childChecks = new int[2];

        try (Seq<Integer, RuntimeException> source = Seq.<Integer, RuntimeException> of(0, 1)
                .flatMap(child -> Seq.of(new Throwables.Iterator<Integer, RuntimeException>() {
                    private int next;

                    @Override
                    public boolean hasNext() {
                        childChecks[child]++;
                        return next < 2;
                    }

                    @Override
                    public Integer next() {
                        return child * 2 + next++;
                    }
                }))) {
            final Throwables.Iterator<Integer, RuntimeException> iter = source.iteratorEx();

            for (int child = 0; child < childChecks.length; child++) {
                // Selecting a child and probing an existing child each call hasNext once, matching Stream.flatMap.
                assertTrue(iter.hasNext());
                assertEquals(1, childChecks[child]);
                assertEquals(child * 2, iter.next());
                assertEquals(2, childChecks[child]);

                assertTrue(iter.hasNext());
                assertEquals(3, childChecks[child]);
                assertEquals(child * 2 + 1, iter.next());
                assertEquals(4, childChecks[child]);
            }

            assertFalse(iter.hasNext());
            assertEquals(5, childChecks[0]);
            assertEquals(5, childChecks[1]);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void finalChildClosesBeforeAnExhaustedOrThrowingSourceProbe(final boolean failSourceProbe) throws IOException {
        final AtomicInteger sourceChecks = new AtomicInteger();
        final AtomicInteger sourceReads = new AtomicInteger();
        final AtomicInteger sourceCloses = new AtomicInteger();
        final AtomicInteger childCloses = new AtomicInteger();
        final IOException failure = new IOException("source probe after the final child");
        final Seq<Integer, IOException> source = Seq.of(new Throwables.Iterator<Integer, IOException>() {
            @Override
            public boolean hasNext() throws IOException {
                if (sourceChecks.incrementAndGet() == 1) {
                    return true;
                }

                assertEquals(1, childCloses.get(), "the final child must close before the upstream exhaustion probe");
                if (failSourceProbe) {
                    throw failure;
                }
                return false;
            }

            @Override
            public Integer next() {
                return sourceReads.incrementAndGet();
            }
        }).flatMap(value -> Seq.<Integer, IOException> of(10, 20).onClose(childCloses::incrementAndGet)).onClose(sourceCloses::incrementAndGet);

        try (source) {
            if (failSourceProbe) {
                assertSame(failure, assertThrows(IOException.class, source::toList));
            } else {
                assertEquals(List.of(10, 20), source.toList());
            }

            assertEquals(2, sourceChecks.get());
            assertEquals(1, sourceReads.get());
            assertEquals(1, childCloses.get());
            assertEquals(1, sourceCloses.get());
            source.close();
            assertEquals(1, childCloses.get());
            assertEquals(1, sourceCloses.get());
        }
    }

    @Test
    void childCloseFailurePreventsTheNextSourceProbeAndRemainsPrimary() {
        final AtomicInteger sourceChecks = new AtomicInteger();
        final AtomicInteger sourceCloses = new AtomicInteger();
        final AtomicInteger mapperCalls = new AtomicInteger();
        final AtomicInteger childCloses = new AtomicInteger();
        final AtomicInteger parentCloses = new AtomicInteger();
        final IllegalStateException childFailure = new IllegalStateException("exhausted child close");
        final IllegalArgumentException parentFailure = new IllegalArgumentException("parent cleanup");
        final Seq<Integer, RuntimeException> source = Seq.of(new Throwables.Iterator<Integer, RuntimeException>() {
            @Override
            public boolean hasNext() {
                assertEquals(1, sourceChecks.incrementAndGet(), "a failing child close must prevent the next source probe");
                return true;
            }

            @Override
            public Integer next() {
                return 1;
            }

            @Override
            protected void closeResourceInternal() {
                sourceCloses.incrementAndGet();
            }
        }).flatMap(value -> {
            mapperCalls.incrementAndGet();
            return Seq.<Integer, RuntimeException> of(value).onClose(() -> {
                childCloses.incrementAndGet();
                throw childFailure;
            });
        }).onClose(() -> {
            parentCloses.incrementAndGet();
            throw parentFailure;
        });

        try (source) {
            assertSame(childFailure, assertThrows(IllegalStateException.class, source::toList));
            assertEquals(List.of(parentFailure), List.of(childFailure.getSuppressed()));
            assertEquals(1, sourceChecks.get());
            assertEquals(1, mapperCalls.get());
            assertEquals(1, childCloses.get());
            assertEquals(1, sourceCloses.get(), "upstream cleanup must run even when another close handler fails");
            assertEquals(1, parentCloses.get());
            source.close();
            assertEquals(1, childCloses.get());
            assertEquals(1, sourceCloses.get());
            assertEquals(1, parentCloses.get());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void failedAdvanceDiscardsTheExhaustedChildBeforeRetry(final boolean failChildClose) throws IOException {
        final AtomicInteger sourceChecks = new AtomicInteger();
        final AtomicInteger sourceReads = new AtomicInteger();
        final AtomicInteger mapperCalls = new AtomicInteger();
        final AtomicInteger firstChildChecks = new AtomicInteger();
        final AtomicInteger firstChildReads = new AtomicInteger();
        final AtomicInteger firstChildCloses = new AtomicInteger();
        final AtomicInteger secondChildCloses = new AtomicInteger();
        final IOException probeFailure = new IOException("retryable source probe");
        final IllegalStateException closeFailure = new IllegalStateException("first child close");
        final Seq<Integer, IOException> firstChild = Seq.of(new Throwables.Iterator<Integer, IOException>() {
            @Override
            public boolean hasNext() {
                assertEquals(0, firstChildCloses.get(), "retry must not revisit an exhausted child's iterator after closing it");
                firstChildChecks.incrementAndGet();
                return firstChildReads.get() == 0;
            }

            @Override
            public Integer next() {
                assertEquals(0, firstChildCloses.get());
                firstChildReads.incrementAndGet();
                return 10;
            }
        }).onClose(() -> {
            firstChildCloses.incrementAndGet();
            if (failChildClose) {
                throw closeFailure;
            }
        });
        final Seq<Integer, IOException> source = Seq.of(new Throwables.Iterator<Integer, IOException>() {
            @Override
            public boolean hasNext() throws IOException {
                final int checks = sourceChecks.incrementAndGet();
                if (checks > 1) {
                    assertEquals(1, firstChildCloses.get(), "the previous child must close before the next source probe");
                }
                if (!failChildClose && checks == 2) {
                    throw probeFailure;
                }
                return sourceReads.get() < 2;
            }

            @Override
            public Integer next() {
                return sourceReads.incrementAndGet();
            }
        }).flatMap(value -> {
            mapperCalls.incrementAndGet();
            return value == 1 ? firstChild : Seq.<Integer, IOException> of(20).onClose(secondChildCloses::incrementAndGet);
        });

        try (source) {
            final Throwables.Iterator<Integer, IOException> iter = source.iteratorEx();
            assertTrue(iter.hasNext());
            assertEquals(10, iter.next());
            assertSame(failChildClose ? closeFailure : probeFailure, assertThrows(Exception.class, iter::hasNext));
            assertEquals(1, firstChildCloses.get());
            assertEquals(failChildClose ? 1 : 2, sourceChecks.get());
            final int checksBeforeRetry = firstChildChecks.get();

            assertTrue(iter.hasNext());
            assertEquals(20, iter.next());
            assertFalse(iter.hasNext());
            assertEquals(checksBeforeRetry, firstChildChecks.get());
            assertEquals(1, firstChildReads.get());
            assertEquals(1, firstChildCloses.get());
            assertEquals(1, secondChildCloses.get());
            assertEquals(failChildClose ? 3 : 4, sourceChecks.get());
            assertEquals(2, sourceReads.get());
            assertEquals(2, mapperCalls.get());
            source.close();
            assertEquals(1, firstChildCloses.get());
            assertEquals(1, secondChildCloses.get());
        }
    }

    @Test
    void bufferedCancellationClosesChildReturnedAfterCancellation() throws InterruptedException {
        final CountDownLatch mapperStarted = new CountDownLatch(1);
        final CountDownLatch releaseMapper = new CountDownLatch(1);
        final CountDownLatch childObserved = new CountDownLatch(1);
        final AtomicInteger childCloses = new AtomicInteger();
        final AtomicInteger childReads = new AtomicInteger();
        final Seq<Integer, RuntimeException> lateChild = Seq.<Integer, RuntimeException> of(2).onEach(value -> {
            childReads.incrementAndGet();
            childObserved.countDown();
        }).onClose(() -> {
            childCloses.incrementAndGet();
            childObserved.countDown();
        });
        final Seq<Integer, RuntimeException> source = Seq.<Integer, RuntimeException> of(1, 2).flatMap(value -> {
            if (value == 1) {
                return Seq.of(value);
            }

            mapperStarted.countDown();
            await(releaseMapper);
            return lateChild;
        });

        try {
            try {
                assertEquals(1, source.buffered(1).onEach(value -> await(mapperStarted)).first().orElseThrow());
            } finally {
                releaseMapper.countDown();
            }

            assertTrue(childObserved.await(5, TimeUnit.SECONDS), "the late child was neither closed nor traversed");
            assertEquals(1, childCloses.get());
            assertEquals(0, childReads.get());
            source.close();
            assertEquals(1, childCloses.get());
        } finally {
            releaseMapper.countDown();
            lateChild.close();
            source.close();
        }
    }

    @Test
    void bufferedCancellationClosesTheCurrentChildToReleaseABlockedRead() throws Exception {
        final CountDownLatch readStarted = new CountDownLatch(1);
        final CountDownLatch releaseRead = new CountDownLatch(1);
        final CountDownLatch readFinished = new CountDownLatch(1);
        final AtomicInteger childCloses = new AtomicInteger();
        final Seq<Integer, RuntimeException> child = Seq.of(new Throwables.Iterator<Integer, RuntimeException>() {
            private boolean emitted;

            @Override
            public boolean hasNext() {
                if (!emitted) {
                    return true;
                }

                readStarted.countDown();
                try {
                    await(releaseRead);
                    return false;
                } finally {
                    readFinished.countDown();
                }
            }

            @Override
            public Integer next() {
                emitted = true;
                return 1;
            }

            @Override
            protected void closeResourceInternal() {
                childCloses.incrementAndGet();
                releaseRead.countDown();
            }
        });
        final Seq<Integer, RuntimeException> source = Seq.<Integer, RuntimeException> of(1).flatMap(value -> child);
        final ExecutorService consumer = Executors.newSingleThreadExecutor();

        try {
            final Future<Integer> first = consumer.submit(() -> source.buffered(1).onEach(value -> await(readStarted)).first().orElseThrow());
            assertTrue(readStarted.await(5, TimeUnit.SECONDS), "the producer did not start the blocking read");
            assertEquals(1, first.get(5, TimeUnit.SECONDS));
            assertTrue(readFinished.await(5, TimeUnit.SECONDS), "closing the child did not release its read");
            assertEquals(1, childCloses.get());
        } finally {
            child.close();
            releaseRead.countDown();
            source.close();
            consumer.shutdownNow();
        }
    }

    @Test
    void concurrentCloseFailurePreservesTheActiveChildsPendingValue() throws Exception {
        final CountDownLatch hasNextStarted = new CountDownLatch(1);
        final CountDownLatch releaseHasNext = new CountDownLatch(1);
        final AtomicInteger childChecks = new AtomicInteger();
        final AtomicInteger childReads = new AtomicInteger();
        final AtomicInteger childCloses = new AtomicInteger();
        final AtomicReference<Thread> closingThread = new AtomicReference<>();
        final AtomicReference<Thread> childClosingThread = new AtomicReference<>();
        final IllegalStateException failure = new IllegalStateException("active child close");
        final Seq<Integer, RuntimeException> child = Seq.of(new Throwables.Iterator<Integer, RuntimeException>() {
            private boolean emitted;

            @Override
            public boolean hasNext() {
                childChecks.incrementAndGet();
                hasNextStarted.countDown();
                awaitRelease(releaseHasNext);
                return !emitted;
            }

            @Override
            public Integer next() {
                childReads.incrementAndGet();
                emitted = true;
                return 42;
            }

            @Override
            protected void closeResourceInternal() {
                childClosingThread.set(Thread.currentThread());
                childCloses.incrementAndGet();
                throw failure;
            }
        });
        final Seq<Integer, RuntimeException> source = Seq.<Integer, RuntimeException> of(1).flatMap(value -> child);
        final Throwables.Iterator<Integer, RuntimeException> iter = source.iteratorEx();
        final ExecutorService workers = newDaemonWorkers();

        try {
            final Future<Integer> pendingValue = workers.submit(() -> {
                assertTrue(iter.hasNext());
                return iter.next();
            });
            assertTrue(hasNextStarted.await(5, TimeUnit.SECONDS), "the active child was not entered");

            // The child is owned and cur is in use. Closing must claim the child without clearing cur or
            // waiting for hasNext; its failure belongs to this closing thread, not the blocked producer.
            final Future<?> close = workers.submit(() -> {
                closingThread.set(Thread.currentThread());
                source.close();
            });
            assertSame(failure, assertThrows(ExecutionException.class, () -> close.get(5, TimeUnit.SECONDS)).getCause());
            assertSame(closingThread.get(), childClosingThread.get());
            assertEquals(1, childCloses.get());
            assertEquals(1, childChecks.get());
            assertEquals(0, childReads.get());
            assertFalse(pendingValue.isDone(), "closing must not release the test's blocked read");
            source.close();

            releaseHasNext.countDown();
            assertEquals(42, pendingValue.get(5, TimeUnit.SECONDS));
            assertEquals(1, childReads.get());
            assertEquals(1, childCloses.get());
        } finally {
            releaseHasNext.countDown();
            stopWorkersAndClose(workers, source, child, failure);
        }
    }

    @Test
    void lateChildCloseFailureAfterConcurrentCancellationStaysOnProducerWithoutTraversal() throws Exception {
        final CountDownLatch mapperStarted = new CountDownLatch(1);
        final CountDownLatch releaseMapper = new CountDownLatch(1);
        final AtomicInteger childChecks = new AtomicInteger();
        final AtomicInteger childReads = new AtomicInteger();
        final AtomicInteger childCloses = new AtomicInteger();
        final AtomicReference<Thread> producingThread = new AtomicReference<>();
        final AtomicReference<Thread> childClosingThread = new AtomicReference<>();
        final IllegalStateException failure = new IllegalStateException("late child close after cancellation");
        final Seq<Integer, RuntimeException> child = Seq.of(new Throwables.Iterator<Integer, RuntimeException>() {
            @Override
            public boolean hasNext() {
                childChecks.incrementAndGet();
                return true;
            }

            @Override
            public Integer next() {
                childReads.incrementAndGet();
                return 42;
            }

            @Override
            protected void closeResourceInternal() {
                childClosingThread.set(Thread.currentThread());
                childCloses.incrementAndGet();
                throw failure;
            }
        });
        final Seq<Integer, RuntimeException> source = Seq.<Integer, RuntimeException> of(1).flatMap(value -> {
            producingThread.set(Thread.currentThread());
            mapperStarted.countDown();
            awaitRelease(releaseMapper);
            return child;
        });
        final Throwables.Iterator<Integer, RuntimeException> iter = source.iteratorEx();
        final ExecutorService workers = newDaemonWorkers();

        try {
            final Future<Boolean> reading = workers.submit(iter::hasNext);
            assertTrue(mapperStarted.await(5, TimeUnit.SECONDS), "the mapper did not reach its checkpoint");

            // Cancellation finishes before the mapper returns, so only the producer can own and close
            // this late result. Neither hasNext nor next may be called on it before that close fails.
            workers.submit(source::close).get(5, TimeUnit.SECONDS);
            assertEquals(0, childCloses.get());
            releaseMapper.countDown();

            assertSame(failure, assertThrows(ExecutionException.class, () -> reading.get(5, TimeUnit.SECONDS)).getCause());
            assertSame(producingThread.get(), childClosingThread.get());
            assertEquals(0, childChecks.get());
            assertEquals(0, childReads.get());
            assertEquals(1, childCloses.get());
            source.close();
            child.close();
            assertEquals(1, childCloses.get());
        } finally {
            releaseMapper.countDown();
            stopWorkersAndClose(workers, source, child, failure);
        }
    }

    @Test
    void cancellationDoesNotWaitForAChildCloseAlreadyOwnedByTheProducer() throws Exception {
        final CountDownLatch childCloseStarted = new CountDownLatch(1);
        final CountDownLatch releaseChildClose = new CountDownLatch(1);
        final AtomicInteger firstChildCloses = new AtomicInteger();
        final AtomicInteger lateChildCloses = new AtomicInteger();
        final AtomicInteger sourceChecks = new AtomicInteger();
        final AtomicInteger sourceReads = new AtomicInteger();
        final AtomicInteger mapperCalls = new AtomicInteger();
        final Seq<Integer, RuntimeException> firstChild = Seq.<Integer, RuntimeException> of(1).onClose(() -> {
            firstChildCloses.incrementAndGet();
            childCloseStarted.countDown();
            awaitRelease(releaseChildClose);
        });
        final Seq<Integer, RuntimeException> source = Seq.of(new Throwables.Iterator<Integer, RuntimeException>() {
            @Override
            public boolean hasNext() {
                sourceChecks.incrementAndGet();
                return sourceReads.get() < 3;
            }

            @Override
            public Integer next() {
                return sourceReads.incrementAndGet();
            }
        }).flatMap(value -> {
            mapperCalls.incrementAndGet();
            return value == 1 ? firstChild : childThatMustNotBeTraversed(lateChildCloses);
        });
        final ExecutorService workers = newDaemonWorkers();

        try {
            final Throwables.Iterator<Integer, RuntimeException> iter = source.iteratorEx();
            assertTrue(iter.hasNext());
            assertEquals(1, iter.next());
            final Future<Boolean> advance = workers.submit(iter::hasNext);
            assertTrue(childCloseStarted.await(5, TimeUnit.SECONDS), "the producer did not start closing the previous child");

            // Cancellation must return before this callback is released. Calling the same child's synchronized
            // close again would wait for the producer and deadlock a callback that needs cancellation to finish.
            workers.submit(source::close).get(5, TimeUnit.SECONDS);
            releaseChildClose.countDown();
            assertFalse(advance.get(5, TimeUnit.SECONDS));
            // Closing the exhausted child precedes the cancellation check and the next upstream probe.
            assertEquals(1, sourceChecks.get());
            assertEquals(1, sourceReads.get());
            assertEquals(1, mapperCalls.get());
            assertEquals(0, lateChildCloses.get());
            assertEquals(1, firstChildCloses.get());
            assertFalse(iter.hasNext());
            assertFalse(iter.hasNext());
            assertThrows(NoSuchElementException.class, iter::next);
            assertEquals(1, sourceChecks.get(), "the source must not be probed after cancellation is observed");
            assertEquals(1, sourceReads.get());
            assertEquals(1, mapperCalls.get(), "the remaining source element must not be mapped after cancellation is observed");
            assertEquals(0, lateChildCloses.get());
        } finally {
            releaseChildClose.countDown();
            stopWorkersAndClose(workers, source, firstChild, null);
        }
    }

    @Test
    void lateChildCloseFailureIsPropagatedWithoutRetry() {
        final AtomicInteger childCloses = new AtomicInteger();
        final IllegalStateException failure = new IllegalStateException("late child close");
        final AtomicReference<Seq<Integer, RuntimeException>> sourceRef = new AtomicReference<>();
        final Seq<Integer, RuntimeException> child = Seq.<Integer, RuntimeException> of(2).onClose(() -> {
            childCloses.incrementAndGet();
            throw failure;
        });
        final Seq<Integer, RuntimeException> source = Seq.<Integer, RuntimeException> of(1).flatMap(value -> {
            sourceRef.get().close();
            return child;
        });
        sourceRef.set(source);

        try {
            assertSame(failure, assertThrows(IllegalStateException.class, source::toList));
            assertEquals(1, childCloses.get());
            source.close();
            assertEquals(1, childCloses.get());
        } finally {
            try {
                child.close();
            } catch (final IllegalStateException e) {
                assertSame(failure, e);
            }
            source.close();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void reentrantParentClosePreventsTheNextSourceProbeAndMapping(final boolean emptyChild) {
        final AtomicReference<Seq<Integer, RuntimeException>> sourceRef = new AtomicReference<>();
        final AtomicInteger sourceChecks = new AtomicInteger();
        final AtomicInteger sourceReads = new AtomicInteger();
        final AtomicInteger mapperCalls = new AtomicInteger();
        final AtomicInteger childCloses = new AtomicInteger();
        final AtomicInteger lateChildCloses = new AtomicInteger();
        final Seq<Integer, RuntimeException> source = Seq.of(new Throwables.Iterator<Integer, RuntimeException>() {
            @Override
            public boolean hasNext() {
                sourceChecks.incrementAndGet();
                return sourceReads.get() < 3;
            }

            @Override
            public Integer next() {
                return sourceReads.incrementAndGet();
            }
        }).flatMap(value -> {
            mapperCalls.incrementAndGet();
            if (value != 1) {
                return childThatMustNotBeTraversed(lateChildCloses);
            }

            final Seq<Integer, RuntimeException> child = emptyChild ? Seq.empty() : Seq.of(value);
            return child.onClose(() -> {
                childCloses.incrementAndGet();
                sourceRef.get().close();
            });
        });
        sourceRef.set(source);

        try (source) {
            final Throwables.Iterator<Integer, RuntimeException> iter = source.iteratorEx();
            assertEquals(emptyChild ? List.of() : List.of(1), source.toList());
            assertFalse(iter.hasNext());
            assertFalse(iter.hasNext());
            assertThrows(NoSuchElementException.class, iter::next);
            assertEquals(1, sourceChecks.get());
            assertEquals(1, sourceReads.get());
            assertEquals(1, mapperCalls.get());
            assertEquals(1, childCloses.get());
            assertEquals(0, lateChildCloses.get());
            source.close();
            assertEquals(1, childCloses.get());
            assertEquals(0, lateChildCloses.get());
        }
    }

    @Test
    void mapperFailureRemainsPrimaryAfterPreviousChildIsClosed() {
        final AtomicInteger childCloses = new AtomicInteger();
        final IllegalStateException mapperFailure = new IllegalStateException("mapper");
        final IllegalArgumentException closeFailure = new IllegalArgumentException("source close");
        final Seq<Integer, RuntimeException> source = Seq.<Integer, RuntimeException> of(1, 2).flatMap(value -> {
            if (value == 2) {
                assertEquals(1, childCloses.get(), "the previous child must close before the next mapper call");
                throw mapperFailure;
            }

            return Seq.<Integer, RuntimeException> of(value).onClose(childCloses::incrementAndGet);
        }).onClose(() -> {
            throw closeFailure;
        });

        assertSame(mapperFailure, assertThrows(IllegalStateException.class, source::toList));
        assertEquals(List.of(closeFailure), List.of(mapperFailure.getSuppressed()));
        assertEquals(1, childCloses.get());
        source.close();
    }

    @Test
    void fullTraversalClosesEveryChildOnceAndAcceptsNullChildren() {
        final AtomicInteger childCloses = new AtomicInteger();
        final Seq<Integer, RuntimeException> source = Seq.<Integer, RuntimeException> of(1, 2, 3)
                .flatMap(value -> value == 2 ? null : Seq.<Integer, RuntimeException> of(value).onClose(childCloses::incrementAndGet));

        assertEquals(List.of(1, 3), source.toList());
        assertEquals(2, childCloses.get());
        source.close();
        assertEquals(2, childCloses.get());
    }

    @Test
    void failingChildCloseIsNotRetriedDuringTerminalCleanup() {
        final AtomicInteger childCloses = new AtomicInteger();
        final IllegalStateException failure = new IllegalStateException("child close");
        final Seq<Integer, RuntimeException> source = Seq.<Integer, RuntimeException> of(1, 2)
                .flatMap(value -> Seq.<Integer, RuntimeException> of(value).onClose(() -> {
                    childCloses.incrementAndGet();
                    throw failure;
                }));

        assertSame(failure, assertThrows(IllegalStateException.class, source::toList));
        assertEquals(1, childCloses.get());
        source.close();
        assertEquals(1, childCloses.get());
    }

    private static Seq<Integer, RuntimeException> childThatMustNotBeTraversed(final AtomicInteger childCloses) {
        return Seq.of(new Throwables.Iterator<Integer, RuntimeException>() {
            @Override
            public boolean hasNext() {
                throw new AssertionError("a child returned after cancellation must not be traversed");
            }

            @Override
            public Integer next() {
                throw new AssertionError("a child returned after cancellation must not be read");
            }

            @Override
            protected void closeResourceInternal() {
                childCloses.incrementAndGet();
            }
        });
    }

    private static ExecutorService newDaemonWorkers() {
        return Executors.newFixedThreadPool(2, task -> {
            final Thread thread = new Thread(task, "seq-flatmap-cleanup-test");
            thread.setDaemon(true);
            return thread;
        });
    }

    private static void stopWorkersAndClose(final ExecutorService workers, final Seq<?, ?> source, final Seq<?, ?> child,
            final RuntimeException expectedFailure) throws InterruptedException {
        workers.shutdownNow();
        // Callers release their gates first. Never acquire a stream monitor if a regressed worker still holds it;
        // daemon workers and a bounded join let the test fail instead of hanging the test process.
        assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS), "cleanup workers did not finish after their gates were released");

        try {
            closeIgnoringExpectedFailure(source, expectedFailure);
        } finally {
            closeIgnoringExpectedFailure(child, expectedFailure);
        }
    }

    private static void closeIgnoringExpectedFailure(final Seq<?, ?> source, final RuntimeException expectedFailure) {
        try {
            source.close();
        } catch (final RuntimeException failure) {
            if (failure != expectedFailure) {
                throw failure;
            }
        }
    }

    private static void awaitRelease(final CountDownLatch latch) {
        try {
            // Longer than the main thread's five-second Future deadline: a blocked close must not pass
            // merely because the traversal callback timed out and released the contested monitor first.
            assertTrue(latch.await(15, TimeUnit.SECONDS), "timed out waiting for traversal to be released");
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static void await(final CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "timed out coordinating buffered traversal");
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
