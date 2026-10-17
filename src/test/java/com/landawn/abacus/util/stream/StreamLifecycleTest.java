package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Seq;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.stream.Stream.WindowHandler;

/** Stream lifecycle contracts. */
public class StreamLifecycleTest extends StreamTestSupport {

    // addSubscriber

    @Test
    public void test_addSubscriber() {
        assertDoesNotThrow(() -> {
            {
                N.println("injectNewStream" + Strings.repeat("=", 80));
                Stream.range(0, 10).addSubscriber(ss -> ss.filter(it -> it % 2 != 0).println()).filter(it -> it % 2 == 0).prepend(100).limit(3).println();
                N.sleep(100);
                Stream.range(0, 10).addSubscriber(ss -> ss.filter(it -> it % 2 != 0).println()).filter(it -> it % 2 == 0).prepend(100).println();
                N.sleep(100);
                N.println(Strings.repeat("=", 80));
            }

            {
                N.println("filterAndInjectNewStream" + Strings.repeat("=", 80));
                Stream.range(0, 10).filterWhileAddSubscriber(it -> it % 2 == 0, Stream::println).prepend(100).limit(3).println();
                N.sleep(100);
                Stream.range(0, 10).filterWhileAddSubscriber(it -> it % 2 == 0, ss -> ss.limit(2).println()).prepend(100).println();
                N.sleep(100);
                N.println(Strings.repeat("=", 80));
            }

            {
                N.println("takeWhileAndInjectNewStream" + Strings.repeat("=", 80));
                Stream.range(0, 10).takeWhileAddSubscriber(it -> it < 5, Stream::println).prepend(100).limit(3).println();
                N.sleep(100);
                Stream.range(0, 10).takeWhileAddSubscriber(it -> it < 5, ss -> ss.limit(2).println()).prepend(100).println();
                N.sleep(100);
                N.println(Strings.repeat("=", 80));
            }

            {
                N.println("dropWhileAndInjectNewStream" + Strings.repeat("=", 80));
                Stream.range(0, 10).dropWhileAddSubscriber(it -> it < 5, Stream::println).prepend(100).limit(3).println();
                N.sleep(100);
                Stream.range(0, 10).dropWhileAddSubscriber(it -> it < 5, ss -> ss.limit(2).println()).prepend(100).println();
                N.sleep(100);
                N.println(Strings.repeat("=", 80));
            }

            {
                N.println("dropWhileAndInjectNewStream" + Strings.repeat("=", 80));
                final Holder<Object> resultHolder = new Holder<>();
                Stream.range(0, 10)
                        .onClose(() -> N.println("closing main Stream - 1"))
                        .dropWhileAddSubscriber(it -> it < 5, ss -> resultHolder.setValue(ss.onClose(() -> N.println("closing inject Stream")).join(", ")))
                        .prepend(100)
                        .limit(3)
                        .onClose(() -> N.println("closing main Stream - 2"))
                        .println();
                N.sleep(100);
                N.println(resultHolder);
                Stream.range(0, 10)
                        .onClose(() -> N.println("closing main Stream - 1"))
                        .dropWhileAddSubscriber(it -> it < 5,
                                ss -> resultHolder.setValue(ss.limit(2).mapToInt(e -> e).onClose(() -> N.println("closing inject Stream")).sum()))
                        .prepend(100)
                        .onClose(() -> N.println("closing main Stream - 2"))
                        .println();
                N.println(resultHolder);
                N.println(Strings.repeat("=", 80));
            }

            {
                N.println("dropWhileAndInjectNewStream" + Strings.repeat("=", 80));
                Stream.range(0, 10).dropWhileAddSubscriber(it -> it < 5, Stream::println).prepend(100).limit(3).println();
                N.sleep(100);
                Stream.range(0, 10).dropWhileAddSubscriber(it -> it < 5, ss -> ss.limit(2).println()).prepend(100).println();
                N.sleep(100);
                N.println(Strings.repeat("=", 80));
            }
        });
    }

    @Test
    public void testAddSubscriber() throws Exception {
        Holder<List<Integer>> subscriberResult = new Holder<>();
        List<Integer> mainResult = Stream.of(1, 2, 3, 4, 5).addSubscriber(newStream -> subscriberResult.setValue(newStream.toList())).toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), mainResult);
        assertNotNull(subscriberResult.value());
        assertEquals(5, subscriberResult.value().size());
    }

    @Test
    public void test_addSubscriber_2() {
        assertDoesNotThrow(() -> {
            {
                N.println("addSubscriber twice" + Strings.repeat("=", 80));
                Stream.range(0, 30)
                        .addSubscriber(ss -> ss.filter(it -> it % 2 != 0).println())
                        .filter(it -> it % 2 == 0)
                        .addSubscriber(ss -> ss.filter(it -> it % 3 == 0).parallel(10).peek(it -> N.sleep(it % 6 == 0 ? 100 : 0)).println())
                        .prepend(100)
                        .limit(3)
                        .println();
            }
        });
    }

    @Test
    @Timeout(10)
    public void testAddSubscriber_BasicFunctionality() throws Exception {
        Stream.of(1, 2, 3, 4, 5).addSubscriber(s -> {
            s.forEach(x -> {
                subscriberStreamResults.add(x);
            });
        }).forEach(x -> {
            mainStreamResults.add(x);
        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), mainStreamResults);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), subscriberStreamResults);
    }

    @Test
    public void testAddSubscriber_ClosedStream() {
        Stream<Integer> closedStream = Stream.of(1, 2, 3);
        closedStream.close();

        assertThrows(IllegalStateException.class, () -> {
            closedStream.addSubscriber(s -> s.forEach(x -> {
            }));
        });
    }

    @Test
    @Timeout(10)
    public void testAddSubscriber_EmptyStream() throws Exception {
        Stream.<Integer> empty().addSubscriber(s -> {
            s.forEach(subscriberStreamResults::add);
        }).forEach(mainStreamResults::add);

        assertTrue(mainStreamResults.isEmpty());
        assertTrue(subscriberStreamResults.isEmpty());
    }

    @Test
    public void testAddSubscriber_NullConsumer() {
        assertThrows(IllegalArgumentException.class, () -> {
            Stream.of(1, 2, 3).addSubscriber(null).forEach(x -> {
            });
        });
    }

    @Test
    @Timeout(10)
    public void testAddSubscriber_NullElements() throws Exception {

        Stream.of(1, null, 3, null, 5).addSubscriber(s -> {
            s.forEach(subscriberStreamResults::add);
        }).forEach(x -> {
            mainStreamResults.add(x);
        });

        assertEquals(Arrays.asList(1, null, 3, null, 5), mainStreamResults);
        assertEquals(Arrays.asList(1, null, 3, null, 5), subscriberStreamResults);
    }

    @Test
    @Timeout(10)
    public void testAddSubscriber_skip_limit() throws Exception {
        Stream.of(1, 2, 3, 4, 5).addSubscriber(s -> {
            s.skip(2).forEach(x -> {
                subscriberStreamResults.add(x);
            });
        }).skip(1).limit(2).forEach(x -> {
            mainStreamResults.add(x);
        });

        assertEquals(Arrays.asList(2, 3), mainStreamResults);
        assertEquals(Arrays.asList(3, 4, 5), subscriberStreamResults);
    }

    @Test
    public void testAddSubscriber_timeout() throws Exception {
        MutableBoolean mainStreamClosed = MutableBoolean.of(false);

        assertThrows(IllegalStateException.class, () -> Stream.of(1, 2, 3, 4, 5).addSubscriber(s -> {
            s.peek(Fn.sleep(100)).forEach(x -> {
                subscriberStreamResults.add(x);
            });
        }, 2, 10, customExecutor).onClose(() -> mainStreamClosed.setTrue()).forEach(x -> {
            mainStreamResults.add(x);
        }));

        assertTrue(mainStreamClosed.value());
    }

    @Test
    @Timeout(10)
    public void testAddSubscriberWithParameters_CustomConfiguration() throws Exception {
        Stream.of(1, 2, 3, 4, 5).addSubscriber(s -> {
            s.forEach(subscriberStreamResults::add);
        }, 10, 1000, customExecutor).forEach(x -> {
            mainStreamResults.add(x);
        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), mainStreamResults);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), subscriberStreamResults);
    }

    @Test
    public void testAddSubscriberWithParameters_InvalidMaxWait() {
        assertThrows(IllegalArgumentException.class, () -> {
            Stream.of(1, 2, 3).addSubscriber(s -> {
            }, 10, -1, customExecutor);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            Stream.of(1, 2, 3).addSubscriber(s -> {
            }, 10, 0, customExecutor);
        });
    }

    @Test
    public void testAddSubscriberWithParameters_InvalidQueueSize() {
        assertThrows(IllegalArgumentException.class, () -> {
            Stream.of(1, 2, 3).addSubscriber(s -> {
            }, -1, 1000, customExecutor);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            Stream.of(1, 2, 3).addSubscriber(s -> {
            }, 0, 1000, customExecutor);
        });
    }

    @Test
    public void testAddSubscriberWithParameters_NullExecutor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Stream.of(1, 2, 3).addSubscriber(s -> {
            }, 10, 1000, null).count();
        });
    }

    @Test
    public void testAddSubscriberPreservesInterruptedOffer() {
        assertSubscriberOfferPreservesInterrupt(0);
    }

    @Test
    public void testAsyncTerminalWrappersAlwaysCloseStream() throws Exception {
        final AtomicInteger closeCount = new AtomicInteger();
        Stream<Integer> source = Stream.of(1, 2, 3).onClose(closeCount::incrementAndGet);

        source.runAsync(s -> {
            // A terminal wrapper must close even when the callback itself does not consume the stream.
        }).get();
        assertEquals(1, closeCount.get());
        assertThrows(IllegalStateException.class, source::count);

        source = Stream.of(1, 2, 3).onClose(closeCount::incrementAndGet);
        assertEquals(7, source.callAsync(s -> 7, customExecutor).get());
        assertEquals(2, closeCount.get());
        assertThrows(IllegalStateException.class, source::count);

        source = Stream.of(1, 2, 3).onClose(closeCount::incrementAndGet);
        final Stream<Integer> failedRunSource = source;
        assertThrows(ExecutionException.class, () -> failedRunSource.runAsync(s -> {
            throw new IOException("expected");
        }, customExecutor).get());
        assertEquals(3, closeCount.get());
        assertThrows(IllegalStateException.class, source::count);

        source = Stream.of(1, 2, 3).onClose(closeCount::incrementAndGet);
        final Stream<Integer> failedCallSource = source;
        assertThrows(ExecutionException.class, () -> failedCallSource.callAsync(s -> {
            throw new IOException("expected");
        }).get());
        assertEquals(4, closeCount.get());
        assertThrows(IllegalStateException.class, source::count);
    }

    @Test
    public void testAsyncTerminalWrappersCloseStreamWhenSchedulingIsRejected() {
        final RejectedExecutionException rejection = new RejectedExecutionException("expected");
        final AtomicInteger closeCount = new AtomicInteger();
        final Stream<Integer> runSource = Stream.of(1).onClose(closeCount::incrementAndGet);

        assertSame(rejection, assertThrows(RejectedExecutionException.class, () -> runSource.runAsync(s -> s.count(), command -> {
            throw rejection;
        })));
        assertEquals(1, closeCount.get());
        assertThrows(IllegalStateException.class, runSource::count);

        final Stream<Integer> callSource = Stream.of(1).onClose(closeCount::incrementAndGet);
        assertSame(rejection, assertThrows(RejectedExecutionException.class, () -> callSource.callAsync(s -> s.count(), command -> {
            throw rejection;
        })));
        assertEquals(2, closeCount.get());
        assertThrows(IllegalStateException.class, callSource::count);

        final IllegalStateException closeFailure = new IllegalStateException("close failed");
        final Stream<Integer> closeFailingSource = Stream.of(1).onClose(() -> {
            throw closeFailure;
        });
        final RejectedExecutionException thrown = assertThrows(RejectedExecutionException.class, () -> closeFailingSource.runAsync(s -> s.count(), command -> {
            throw rejection;
        }));

        assertSame(rejection, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(closeFailure, thrown.getSuppressed()[0]);
        assertThrows(IllegalStateException.class, closeFailingSource::count);
    }

    @Test
    public void testBufferedClosesSourceWithoutTraversal() {
        for (int overload = 0; overload < 3; overload++) {
            for (int mode = 0; mode < 3; mode++) {
                final AtomicInteger closeCalls = new AtomicInteger();
                final AtomicInteger nextCalls = new AtomicInteger();
                final Stream<Integer> source = Stream.of(countingIterator(Arrays.asList(1, 2), nextCalls)).onClose(closeCalls::incrementAndGet);
                final Stream<Integer> result = overload == 0 ? source.buffered()
                        : overload == 1 ? source.buffered(2) : source.buffered(new ArrayBlockingQueue<>(2));

                if (mode == 0) {
                    result.close();
                } else if (mode == 1) {
                    assertEquals(0, result.limit(0).count());
                } else {
                    assertEquals(Arrays.asList(1, 2), result.toList());
                }

                result.close();
                assertEquals(1, closeCalls.get(), "source must close exactly once");
                assertEquals(mode == 2 ? 2 : 0, nextCalls.get(), "closing must preserve lazy traversal");
                assertThrows(IllegalStateException.class, source::count);
            }
        }
    }

    @Test
    public void testcallAsync_WithExecutor() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Integer result = Stream.of(1, 2, 3).callAsync(s -> s.reduce(0, Integer::sum), executor).get();
            assertEquals(6, result);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testCartesianProductNullCollectionClosesWithoutConsumingSource() {
        final AtomicInteger consumed = new AtomicInteger();
        final AtomicInteger closed = new AtomicInteger();
        final Stream<Integer> stream = Stream.of(1, 2).peek(value -> consumed.incrementAndGet()).onClose(closed::incrementAndGet);

        assertTrue(assertThrows(IllegalArgumentException.class, () -> stream.cartesianProduct((Collection<Collection<Integer>>) null)).getMessage()
                .contains("cs"));
        assertEquals(0, consumed.get());
        assertEquals(1, closed.get());
        assertThrows(IllegalStateException.class, stream::count);
    }

    @Test
    public void testCompositeCloseBeforeConsumptionSeesLateSourceCloseHandlers() {
        final AtomicInteger closedCount = new AtomicInteger();
        final Stream<Integer> source = Stream.of(1);
        final Stream<Integer> concatenated = Stream.concat(Collections.singletonList(source));

        source.onClose(closedCount::incrementAndGet);
        concatenated.close();
        assertEquals(1, closedCount.get());

        closedCount.set(0);
        final Stream<Integer> singleSource = Stream.of(1);
        final Stream<Integer> zipWithCollection = singleSource.zipWith(Collections.singletonList(2), Integer::sum);

        singleSource.onClose(closedCount::incrementAndGet);
        zipWithCollection.close();
        assertEquals(1, closedCount.get());

        closedCount.set(0);
        final Stream<Integer> left = Stream.of(1);
        final Stream<Integer> right = Stream.of(2);
        final Stream<Integer> zipped = Stream.zip(left, right, Integer::sum);

        left.onClose(closedCount::incrementAndGet);
        right.onClose(closedCount::incrementAndGet);
        zipped.close();
        assertEquals(2, closedCount.get());

        closedCount.set(0);
        final IntStream intSource = IntStream.of(1);
        final IntStream intConcatenated = IntStream.concat(Collections.singletonList(intSource));

        intSource.onClose(closedCount::incrementAndGet);
        intConcatenated.close();
        assertEquals(1, closedCount.get());
    }

    // concat

    @Test
    public void testConcat_ClosePreservesFirstErrorAndContinuesClosing() {
        final AssertionError failure = new AssertionError("first close failure");
        final AtomicInteger laterCloseCalls = new AtomicInteger();
        final Stream<Integer> first = createStream(1).onClose(() -> {
            throw failure;
        });
        final Stream<Integer> second = createStream(2).onClose(laterCloseCalls::incrementAndGet);
        final Stream<Integer> concatenated = Stream.concat(first, second);

        final AssertionError thrown = assertThrows(AssertionError.class, concatenated::close);

        assertSame(failure, thrown);
        assertEquals(1, laterCloseCalls.get());
    }

    @Test
    public void testConcat_CollectionOfStreams_CloseHandlers() {
        List<AtomicBoolean> closedFlags = new ArrayList<>();
        List<Stream<Integer>> streams = new ArrayList<>();

        for (int i = 0; i < 3; i++) {
            AtomicBoolean closed = new AtomicBoolean(false);
            closedFlags.add(closed);
            final int base = i * 10;
            streams.add(createStream(base, base + 1).onClose(() -> closed.set(true)));
        }

        try (Stream<Integer> concatenated = Stream.concat(streams)) {
            List<Integer> result = concatenated.toList();
            assertEquals(Arrays.asList(0, 1, 10, 11, 20, 21), result);
        }

        for (AtomicBoolean flag : closedFlags) {
            assertTrue(flag.get());
        }
    }

    @Test
    public void testConcat_StreamClose() {
        AtomicBoolean stream1Closed = new AtomicBoolean(false);
        AtomicBoolean stream2Closed = new AtomicBoolean(false);

        Stream<String> stream1 = createStream01("a", "b").onClose(() -> stream1Closed.set(true));
        Stream<String> stream2 = createStream01("c", "d").onClose(() -> stream2Closed.set(true));

        try (Stream<String> concatenated = Stream.concat(stream1, stream2)) {
            assertEquals(Arrays.asList("a", "b", "c", "d"), concatenated.toList());
        }

        assertTrue(stream1Closed.get());
        assertTrue(stream2Closed.get());
    }

    @Test
    public void testDefaultIfEmptyNullSupplierClosesEveryStreamType() {
        final BaseStream<?, ?, ?, ?, ?, ?, ?, ?>[] sources = { Stream.of(1), EntryStream.of("key", 1), ByteStream.of((byte) 1), CharStream.of('a'),
                ShortStream.of((short) 1), IntStream.of(1), LongStream.of(1L), FloatStream.of(1F), DoubleStream.of(1D) };

        for (final BaseStream<?, ?, ?, ?, ?, ?, ?, ?> original : sources) {
            final AtomicInteger closeCalls = new AtomicInteger();
            final BaseStream<?, ?, ?, ?, ?, ?, ?, ?> source = original.onClose(closeCalls::incrementAndGet);

            assertThrows(IllegalArgumentException.class, () -> source.defaultIfEmpty(null));
            assertEquals(1, closeCalls.get(), "a null supplier must close the source");
            source.close();
            assertEquals(1, closeCalls.get());
            assertThrows(IllegalStateException.class, () -> source.defaultIfEmpty(null));
        }
    }

    // defer

    // Neighbours unchanged: a deferred stream that returns ANOTHER deferred stream, and the supplied stream is closed once.
    @Test
    public void testDefer_otherDeferredStreamAndInnerCloseUnchanged() {
        final Stream<Integer> other = Stream.defer(() -> Stream.of(5, 6));
        assertEquals(Arrays.asList(5, 6), Stream.defer(() -> other).toList());

        final AtomicInteger innerCloses = new AtomicInteger();
        assertEquals(Arrays.asList(1, 2, 3), Stream.defer(() -> Stream.of(1, 2, 3).onClose(innerCloses::incrementAndGet)).toList());
        assertEquals(1, innerCloses.get());

        assertEquals(6, IntStream.defer(() -> IntStream.of(1, 2, 3)).parallel(2).sum());
        assertEquals(0, IntStream.defer(() -> null).count());
    }

    // Closing a self-returning deferred stream, before or after a failed traversal, runs the supplier once, does not
    // recurse, and runs the stream's own close handlers once.
    @Test
    public void testDefer_selfReturningStreamCloseIsSafe() {
        final AtomicInteger calls = new AtomicInteger();
        @SuppressWarnings("unchecked")
        final Stream<Object>[] s1 = new Stream[1];
        s1[0] = Stream.defer(() -> {
            calls.incrementAndGet();
            return s1[0];
        });
        assertDoesNotThrow(() -> s1[0].close());
        assertEquals(1, calls.get());

        calls.set(0);
        final AtomicInteger closes = new AtomicInteger();
        @SuppressWarnings("unchecked")
        final Stream<Object>[] s2 = new Stream[1];
        s2[0] = Stream.defer(() -> {
            calls.incrementAndGet();
            return s2[0];
        });
        final Stream<Object> withHandler = s2[0].onClose(closes::incrementAndGet);
        assertThrows(IllegalStateException.class, withHandler::count);
        assertDoesNotThrow(() -> withHandler.close());
        assertEquals(1, calls.get());
        assertEquals(1, closes.get());
    }

    @Test
    public void testDeferCloseInvokesSupplierAndCloseHandlers() {
        final AtomicInteger calls = new AtomicInteger();
        final AtomicInteger closed = new AtomicInteger();
        final Stream<Integer> stream = Stream.defer(() -> {
            calls.incrementAndGet();
            return Stream.of(1).onClose(closed::incrementAndGet);
        });

        stream.close();

        assertEquals(1, calls.get());
        assertEquals(1, closed.get());
    }

    // dropWhileAddSubscriber

    @Test
    public void testDropWhileAddSubscriber() throws Exception {
        Holder<List<Integer>> subscriberResult = new Holder<>();
        List<Integer> mainResult = Stream.of(1, 2, 3, 4, 5)
                .dropWhileAddSubscriber(x -> x <= 3, newStream -> subscriberResult.setValue(newStream.toList()))
                .toList();

        assertEquals(Arrays.asList(4, 5), mainResult);
        assertNotNull(subscriberResult.value());
    }

    @Test
    @Timeout(10)
    public void testDropWhileAddSubscriber_BasicFunctionality() throws Exception {
        Stream.of(1, 2, 3, 4, 5).dropWhileAddSubscriber(x -> x < 3, s -> {
            s.forEach(subscriberStreamResults::add);
        }).forEach(x -> {
            mainStreamResults.add(x);
        });

        assertEquals(Arrays.asList(3, 4, 5), mainStreamResults);
        assertEquals(Arrays.asList(1, 2), subscriberStreamResults);
    }

    @Test
    @Timeout(10)
    public void testDropWhileAddSubscriber_DropAll() throws Exception {
        Stream.of(1, 2, 3, 4, 5).dropWhileAddSubscriber(x -> x <= 10, s -> {
            s.forEach(subscriberStreamResults::add);
        }).forEach(x -> {
            mainStreamResults.add(x);
        });

        assertTrue(mainStreamResults.isEmpty());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), subscriberStreamResults);
    }

    @Test
    @Timeout(10)
    public void testDropWhileAddSubscriber_DropNone() throws Exception {
        Stream.of(1, 2, 3, 4, 5).dropWhileAddSubscriber(x -> x > 10, s -> {
            s.forEach(subscriberStreamResults::add);
        }).forEach(x -> {
            mainStreamResults.add(x);
        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), mainStreamResults);
        assertTrue(subscriberStreamResults.isEmpty());
    }

    @Test
    public void testDropWhileAddSubscriber_NullPredicate() {
        assertThrows(IllegalArgumentException.class, () -> {
            Stream.of(1, 2, 3).dropWhileAddSubscriber(null, s -> {
            }).count();
        });
    }

    @Test
    @Timeout(10)
    public void testDropWhileAddSubscriberWithParameters_CustomConfiguration() throws Exception {
        Stream.of(1, 2, 3, 4, 5).dropWhileAddSubscriber(x -> x <= 2, s -> {
            s.forEach(subscriberStreamResults::add);
        }, 100, 5000, customExecutor).forEach(x -> {
            mainStreamResults.add(x);
        });

        assertEquals(Arrays.asList(3, 4, 5), mainStreamResults);
        assertEquals(Arrays.asList(1, 2), subscriberStreamResults);
    }

    @Test
    public void testDropWhileSubscriberPreservesInterruptedOffer() {
        assertSubscriberOfferPreservesInterrupt(3);
    }

    @Test
    public void testFilterSubscriberPreservesInterruptedOffer() {
        assertSubscriberOfferPreservesInterrupt(1);
    }

    // filterWhileAddSubscriber

    @Test
    public void testFilterWhileAddSubscriber() throws Exception {
        Holder<List<Integer>> subscriberResult = new Holder<>();
        List<Integer> mainResult = Stream.of(1, 2, 3, 4, 5)
                .filterWhileAddSubscriber(x -> x <= 3, newStream -> subscriberResult.setValue(newStream.toList()))
                .toList();

        assertEquals(Arrays.asList(1, 2, 3), mainResult);
        assertNotNull(subscriberResult.value());
    }

    @Test
    @Timeout(10)
    public void testFilterWhileAddSubscriber_AllMatch() throws Exception {
        Stream.of(1, 3, 5, 7, 9).filterWhileAddSubscriber(x -> x % 2 == 1, s -> {
            s.forEach(subscriberStreamResults::add);
        }).forEach(x -> {
            mainStreamResults.add(x);
        });

        assertEquals(Arrays.asList(1, 3, 5, 7, 9), mainStreamResults);
        assertTrue(subscriberStreamResults.isEmpty());
    }

    @Test
    @Timeout(10)
    public void testFilterWhileAddSubscriber_BasicFunctionality() throws Exception {
        Stream.of(1, 2, 3, 4, 5).filterWhileAddSubscriber(x -> x % 2 == 1, s -> {
            s.forEach(subscriberStreamResults::add);
        }).forEach(x -> {
            mainStreamResults.add(x);
        });

        assertEquals(Arrays.asList(1, 3, 5), mainStreamResults);
        assertEquals(Arrays.asList(2, 4), subscriberStreamResults);
    }

    @Test
    @Timeout(10)
    public void testFilterWhileAddSubscriber_NoneMatch() throws Exception {
        Stream.of(2, 4, 6, 8, 10).filterWhileAddSubscriber(x -> x % 2 == 1, s -> {
            s.forEach(subscriberStreamResults::add);
        }).forEach(x -> {
            mainStreamResults.add(x);
        });

        assertTrue(mainStreamResults.isEmpty());
        assertEquals(Arrays.asList(2, 4, 6, 8, 10), subscriberStreamResults);
    }

    @Test
    public void testFilterWhileAddSubscriber_NullPredicate() {
        assertThrows(IllegalArgumentException.class, () -> {
            Stream.of(1, 2, 3).filterWhileAddSubscriber(null, s -> {
            }).count();
        });
    }

    @Test
    @Timeout(30)
    public void testFilterWhileAddSubscriberStartsSubscriberOnCloseWhenNothingFilteredOut() throws Exception {
        final AtomicInteger submitted = new AtomicInteger();
        final ExecutorService pool = Executors.newSingleThreadExecutor();

        try {
            final Executor executor = command -> {
                submitted.incrementAndGet();
                pool.execute(command);
            };

            final AtomicInteger subscriberCount = new AtomicInteger(-1);
            final Stream<Integer> stream = Stream.of(1, 3, 5)
                    .filterWhileAddSubscriber(x -> x % 2 == 1, s -> subscriberCount.set((int) s.count()), 64, 30000, executor);
            final Iterator<Integer> iter = stream.iterator();

            assertEquals(1, iter.next());
            assertEquals(3, iter.next());
            assertEquals(5, iter.next());
            assertFalse(iter.hasNext());
            assertEquals(0, submitted.get());

            stream.close();

            assertEquals(1, submitted.get());
            assertEquals(0, subscriberCount.get());
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @Timeout(30)
    public void testFilterWhileAddSubscriberStartsSubscriberOnlyOnFirstFilteredOutElement() throws Exception {
        final AtomicInteger submitted = new AtomicInteger();
        final ExecutorService pool = Executors.newSingleThreadExecutor();

        try {
            final Executor executor = command -> {
                submitted.incrementAndGet();
                pool.execute(command);
            };

            final List<Integer> sentToSubscriber = Collections.synchronizedList(new ArrayList<>());

            try (Stream<Integer> stream = Stream.of(1, 3, 4, 5)
                    .filterWhileAddSubscriber(x -> x % 2 == 1, s -> s.forEach(sentToSubscriber::add), 64, 30000, executor)) {
                final Iterator<Integer> iter = stream.iterator();

                assertEquals(1, iter.next());
                assertEquals(0, submitted.get()); // elements kept by the main stream do not start the subscriber
                assertEquals(3, iter.next());
                assertEquals(0, submitted.get());
                assertEquals(5, iter.next()); // 4 is filtered out and sent to the subscriber
                assertEquals(1, submitted.get());
            }

            assertEquals(Arrays.asList(4), sentToSubscriber);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    @Timeout(10)
    public void testFilterWhileAddSubscriberWithParameters_CustomConfiguration() throws Exception {
        Stream.of(1, 2, 3, 4, 5).filterWhileAddSubscriber(x -> x > 3, s -> {
            s.forEach(subscriberStreamResults::add);
        }, 100, 5000, customExecutor).forEach(x -> {
            mainStreamResults.add(x);
        });

        assertEquals(Arrays.asList(4, 5), mainStreamResults);
        assertEquals(Arrays.asList(1, 2, 3), subscriberStreamResults);
    }

    @Test
    public void testClosingFlatMapClosesCurrentMappedStream() {
        final Stream<Integer> mappedStream = Stream.of(10, 20);
        final Stream<Integer> flattened = Stream.of(1).flatMap(ignored -> mappedStream);

        assertEquals(10, flattened.iteratorEx().next().intValue());
        flattened.close();

        assertThrows(IllegalStateException.class, () -> mappedStream.count());
    }

    @Test
    public void testFlatMapClosesMappedStreamsAcrossImplementations() {
        assertFlatMapClosesMappedStreams(Stream.of(0, 1));
        assertFlatMapClosesMappedStreams(Stream.of(Arrays.asList(0, 1).iterator()));
        assertFlatMapClosesMappedStreams(Stream.of(0, 1).parallel(2));
        assertFlatMapClosesMappedStreams(Stream.of(Arrays.asList(0, 1).iterator()).parallel(2));
    }

    @Test
    public void testFlatMapToPrimitiveClosesMappedStreamsAcrossImplementations() {
        assertFlatMapToPrimitiveClosesMappedStreams(() -> Stream.of(0, 1));
        assertFlatMapToPrimitiveClosesMappedStreams(() -> Stream.of(Arrays.asList(0, 1).iterator()));
        assertFlatMapToPrimitiveClosesMappedStreams(() -> Stream.of(0, 1).parallel(2));
        assertFlatMapToPrimitiveClosesMappedStreams(() -> Stream.of(Arrays.asList(0, 1).iterator()).parallel(2));
    }

    @Test
    public void testFrom_ClosedJavaStream() {
        java.util.stream.Stream<String> javaStream = java.util.stream.Stream.of("a", "b", "c");
        javaStream.close();

        Stream<String> stream = Stream.from(javaStream);

        try {
            stream.toList();
            fail("Expected exception from closed java stream");
        } catch (IllegalStateException e) {
        }
    }

    @Test
    public void testInterval_CancellationBehavior() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);

        Stream<Integer> intervalStream = trackStream(Stream.interval(10, () -> counter.incrementAndGet()));

        List<Integer> results = intervalStream.limit(3).toList();

        assertEquals(Arrays.asList(1, 2, 3), results);

        Thread.sleep(50);
        assertTrue(counter.get() <= 4);
    }

    @Test
    public void testMaxWaitClosesSourceWithoutTraversal() {
        for (final boolean suppliedDefault : new boolean[] { false, true }) {
            for (int mode = 0; mode < 3; mode++) {
                final AtomicInteger closeCalls = new AtomicInteger();
                final AtomicInteger nextCalls = new AtomicInteger();
                final Stream<Integer> source = Stream.of(countingIterator(Arrays.asList(1, 2), nextCalls)).onClose(closeCalls::incrementAndGet);
                // This verifies closure, so allow a generous worker-startup delay before emitting a timeout value.
                final Stream<Integer> result = suppliedDefault ? source.maxWait(Duration.ofSeconds(5), () -> 99) : source.maxWait(Duration.ofSeconds(5), 99);

                if (mode == 0) {
                    result.close();
                } else if (mode == 1) {
                    assertEquals(0, result.limit(0).count());
                } else {
                    assertEquals(Arrays.asList(1, 2), result.toList());
                }

                result.close();
                assertEquals(1, closeCalls.get(), "source must close exactly once");
                assertEquals(mode == 2 ? 2 : 0, nextCalls.get(), "closing must preserve lazy traversal");
                assertThrows(IllegalStateException.class, source::count);
            }
        }
    }

    @Test
    @Timeout(10)
    public void testMultipleSubscribers_Chaining() throws Exception {
        List<Integer> subscriber1Results = new ArrayList<>();
        List<Integer> subscriber2Results = new ArrayList<>();

        Stream.of(1, 2, 3, 4, 5, 6).addSubscriber(s -> {
            s.filter(x -> x % 2 == 0).forEach(subscriber1Results::add);
        }).filterWhileAddSubscriber(x -> x % 3 != 0, s -> {
            s.forEach(subscriber2Results::add);
        }).forEach(x -> {
            mainStreamResults.add(x);
        });

        assertEquals(Arrays.asList(1, 2, 4, 5), mainStreamResults);
        assertEquals(Arrays.asList(2, 4, 6), subscriber1Results);
        assertEquals(Arrays.asList(3, 6), subscriber2Results);
    }

    // ofLines

    @Test
    public void testOfLines_ReaderClosed() throws IOException {
        String content = "line1\nline2\nline3";
        StringReader reader = new StringReader(content);

        List<String> lines;
        try (Stream<String> stream = Stream.ofLines(reader, true)) {
            lines = stream.toList();
        }

        assertEquals(3, lines.size());

        try {
            reader.read();
            fail("Reader should be closed");
        } catch (IOException e) {
        }
    }

    @Test
    public void testOfLines_ReaderWithCloseFlag() throws IOException {
        String content = "a\nb\nc";
        StringReader reader = new StringReader(content);

        try (Stream<String> stream = Stream.ofLines(reader, true)) {
            assertEquals(Arrays.asList("a", "b", "c"), stream.toList());
        }

        StringReader reader2 = new StringReader(content);
        Stream<String> stream2 = Stream.ofLines(reader2, false);
        assertEquals(Arrays.asList("a", "b", "c"), stream2.toList());
    }

    @Test
    public void testOfLinesFromReader_autoClose() throws IOException {
        StringReader reader = new StringReader("a\nb");
        try (Stream<String> lines = Stream.ofLines(reader, true)) {
            List<String> result = lines.toList();
            assertEquals(Arrays.asList("a", "b"), result);
        }
    }

    @Test
    public void testOfLinesFromReaderWithCloseOption() throws IOException {
        String content = "test1\ntest2";
        StringReader reader = new StringReader(content);

        try (Stream<String> stream = Stream.ofLines(reader, true)) {
            List<String> result = stream.toList();
            assertEquals(Arrays.asList("test1", "test2"), result);
        }

    }

    // onClose

    @Test
    public void testOnClose() {
        AtomicBoolean closed = new AtomicBoolean(false);
        Stream<Integer> stream = Stream.of(1, 2, 3).onClose(() -> closed.set(true));
        stream.count();
        stream.close();
        assertTrue(closed.get());
    }

    @Test
    public void testOnCloseMultipleHandlers() {
        AtomicInteger counter = new AtomicInteger(0);
        Stream<Integer> stream = Stream.of(1, 2, 3).onClose(counter::incrementAndGet).onClose(counter::incrementAndGet);
        stream.count();
        stream.close();
        assertEquals(2, counter.get());
    }

    @Test
    public void testStreamCreatedAfterOnClose() {
        AtomicBoolean closed = new AtomicBoolean(false);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).onClose(() -> closed.set(true)).count());
        assertTrue(closed.get());
        closed.set(false);
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).onClose(() -> closed.set(true)).skip(1).count());
        assertTrue(closed.get());
        closed.set(false);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).onClose(() -> closed.set(true)).toArray());
        assertTrue(closed.get());
        closed.set(false);
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).onClose(() -> closed.set(true)).skip(1).toArray());
        assertTrue(closed.get());
        closed.set(false);
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).onClose(() -> closed.set(true)).toList());
        assertTrue(closed.get());
        closed.set(false);
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).onClose(() -> closed.set(true)).skip(1).toList());
        assertTrue(closed.get());
        closed.set(false);
        assertEquals(5, Stream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> closed.set(true)).count());
        assertTrue(closed.get());
        closed.set(false);
        assertEquals(4, Stream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> closed.set(true)).skip(1).count());
        assertTrue(closed.get());
        closed.set(false);
        assertArrayEquals(new Integer[] { 1, 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> closed.set(true)).toArray());
        assertTrue(closed.get());
        closed.set(false);
        assertArrayEquals(new Integer[] { 2, 3, 4, 5 }, Stream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> closed.set(true)).skip(1).toArray());
        assertTrue(closed.get());
        closed.set(false);
        assertEquals(N.toList(1, 2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> closed.set(true)).toList());
        assertTrue(closed.get());
        closed.set(false);
        assertEquals(N.toList(2, 3, 4, 5), Stream.of(1, 2, 3, 4, 5).map(e -> e).onClose(() -> closed.set(true)).skip(1).toList());
        assertTrue(closed.get());
    }

    @Test
    public void testParallelMergeClosesInputStreamsOnCloseWithoutConsumption() {
        // regression: the size >= 4 path returned a lazy stream without close handlers for the inputs,
        // so closing the result without consuming it never closed the source streams
        final AtomicInteger closedCount = new AtomicInteger();
        final List<Stream<Integer>> sources = new ArrayList<>();

        for (int i = 0; i < 4; i++) {
            sources.add(Stream.of(1, 2, 3).onClose(closedCount::incrementAndGet));
        }

        Stream.parallelMerge(sources, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND, 4).close();

        assertEquals(4, closedCount.get());
    }

    @Test
    public void testParallelZipClosesOpenedSourceWhenSiblingIterateFails() {
        final AtomicInteger closed = new AtomicInteger();
        final Stream<Integer> a = Stream.of(1, 2).onClose(closed::incrementAndGet);
        final Stream<Integer> b = Stream.of(3);
        b.close();

        assertThrows(IllegalStateException.class, () -> Stream.parallelZip(a, b, Integer::sum, 2).toList());
        assertEquals(1, closed.get());
    }

    @Test
    public void testPrimitiveFlatMapClosesMappedStreamsAcrossArrayAndIteratorSources() {
        assertByteFlatMapClosesMappedStream(ByteStream.of((byte) 1));
        assertByteFlatMapClosesMappedStream(ByteStream.of(ByteIterator.of((byte) 1)));
        assertCharFlatMapClosesMappedStream(CharStream.of('a'));
        assertCharFlatMapClosesMappedStream(CharStream.of(CharIterator.of('a')));
        assertShortFlatMapClosesMappedStream(ShortStream.of((short) 1));
        assertShortFlatMapClosesMappedStream(ShortStream.of(ShortIterator.of((short) 1)));
        assertIntFlatMapClosesMappedStream(IntStream.of(1));
        assertIntFlatMapClosesMappedStream(IntStream.of(IntIterator.of(1)));
        assertLongFlatMapClosesMappedStream(LongStream.of(1L));
        assertLongFlatMapClosesMappedStream(LongStream.of(LongIterator.of(1L)));
        assertFloatFlatMapClosesMappedStream(FloatStream.of(1F));
        assertFloatFlatMapClosesMappedStream(FloatStream.of(FloatIterator.of(1F)));
        assertDoubleFlatMapClosesMappedStream(DoubleStream.of(1D));
        assertDoubleFlatMapClosesMappedStream(DoubleStream.of(DoubleIterator.of(1D)));
    }

    // rangeClosed

    @Test
    public void testRangeClosed() {
        Stream<Integer> stream = Stream.rangeClosed(1, 5);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), stream.toList());
    }

    @Test
    public void testRangeClosed_Int() {
        List<Integer> range = Stream.rangeClosed(1, 5).toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), range);
    }

    @Test
    public void testRangeClosed_IntWithStep() {
        List<Integer> range = Stream.rangeClosed(1, 10, 3).toList();

        assertEquals(Arrays.asList(1, 4, 7, 10), range);
    }

    @Test
    public void testRangeClosed_Long() {
        List<Long> range = Stream.rangeClosed(5L, 8L).toList();

        assertEquals(Arrays.asList(5L, 6L, 7L, 8L), range);
    }

    @Test
    public void testRangeClosed_LongWithStep() {
        List<Long> range = Stream.rangeClosed(0L, 20L, 5L).toList();

        assertEquals(Arrays.asList(0L, 5L, 10L, 15L, 20L), range);
    }

    @Test
    public void testRangeClosed_NegativeSteps() {
        List<Integer> intRange = Stream.rangeClosed(10, 0, -2).toList();
        assertEquals(Arrays.asList(10, 8, 6, 4, 2, 0), intRange);

        List<Long> longRange = Stream.rangeClosed(15L, 5L, -5L).toList();
        assertEquals(Arrays.asList(15L, 10L, 5L), longRange);
    }

    @Test
    public void testRangeClosedInt() {
        List<Integer> result = Stream.rangeClosed(1, 5).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testRangeClosedIntWithBy() {
        List<Integer> result = Stream.rangeClosed(1, 10, 3).toList();
        assertEquals(Arrays.asList(1, 4, 7, 10), result);
    }

    @Test
    public void testRangeClosedLong() {
        List<Long> result = Stream.rangeClosed(1L, 5L).toList();
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), result);
    }

    @Test
    public void testRangeClosedLongWithBy() {
        List<Long> result = Stream.rangeClosed(1L, 10L, 3L).toList();
        assertEquals(Arrays.asList(1L, 4L, 7L, 10L), result);
    }

    @Test
    public void testRangeClosedWithStep() {
        Stream<Integer> stream = Stream.rangeClosed(0, 10, 2);
        assertEquals(Arrays.asList(0, 2, 4, 6, 8, 10), stream.toList());
    }

    @Test
    public void testResourceManagement() throws IOException {
        File testFile = Files.createTempFile(tempFolder, "resource-test", ".txt").toFile();
        Files.write(testFile.toPath(), Arrays.asList("line1", "line2", "line3"));

        AtomicBoolean streamClosed = new AtomicBoolean(false);

        try (Stream<String> stream = Stream.ofLines(testFile).onClose(() -> streamClosed.set(true))) {
            assertEquals(3, stream.count());
        }

        assertTrue(streamClosed.get());
    }

    @Test
    public void testrunAsyncWithExecutor() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            AtomicInteger sum = new AtomicInteger(0);
            Stream.of(1, 2, 3).runAsync(s -> s.forEach(e -> sum.addAndGet(e)), executor).get();
            assertEquals(6, sum.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testSjpsClosesReturnedJdkStream() {
        {
            AtomicBoolean closed = new AtomicBoolean(false);

            assertEquals(1, Stream.of(1).sjps(s -> s.onClose(() -> closed.set(true)).map(x -> x)).count());
            assertTrue(closed.get());
        }
        {
            AtomicBoolean closed = new AtomicBoolean(false);

            assertEquals(1, Stream.of(1).parallel().sjps(s -> s.onClose(() -> closed.set(true)).map(x -> x)).count());
            assertTrue(closed.get());
        }
    }

    @Test
    @Timeout(10)
    public void testSubscriberStream_CloseHandling() throws Exception {
        AtomicBoolean subscriberClosed = new AtomicBoolean(false);

        Stream.of(1, 2, 3).addSubscriber(s -> {
            s.onClose(() -> subscriberClosed.set(true)).forEach(subscriberStreamResults::add);
        }).forEach(mainStreamResults::add);

        assertEquals(Arrays.asList(1, 2, 3), mainStreamResults);
        assertEquals(Arrays.asList(1, 2, 3), subscriberStreamResults);
        assertTrue(subscriberClosed.get());
    }

    @Test
    @Timeout(10)
    public void testSubscriberStream_ExceptionHandling() throws Exception {
        AtomicBoolean exceptionCaught = new AtomicBoolean(false);

        try {
            Stream.of(1, 2, 3, 4, 5).addSubscriber(s -> {
                try {
                    s.forEach(x -> {
                        if (x == 3) {
                            throw new RuntimeException("Test exception");
                        }
                        subscriberStreamResults.add(x);
                    });
                } catch (Exception e) {
                    exceptionCaught.set(true);
                }
            }).forEach(mainStreamResults::add);
        } catch (Exception e) {
        }

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), mainStreamResults);
        assertTrue(exceptionCaught.get());
    }

    @Test
    @Timeout(10)
    public void testSubscriberStreams_IndependentOperations() throws Exception {
        List<Integer> mainProcessed = new ArrayList<>();
        List<Integer> subscriberProcessed = new ArrayList<>();

        Stream.of(1, 2, 3, 4, 5).addSubscriber(s -> {
            s.filter(x -> x % 2 == 0).map(x -> x * 10).forEach(subscriberProcessed::add);
        }).filter(x -> x % 2 == 1).map(x -> x * 100).forEach(x -> {
            mainProcessed.add(x);
        });

        assertEquals(Arrays.asList(100, 300, 500), mainProcessed);
        assertEquals(Arrays.asList(20, 40), subscriberProcessed);
    }

    // takeWhileAddSubscriber

    @Test
    public void testTakeWhileAddSubscriber() throws Exception {
        Holder<List<Integer>> subscriberResult = new Holder<>();
        List<Integer> mainResult = Stream.of(1, 2, 3, 4, 5)
                .takeWhileAddSubscriber(x -> x <= 3, newStream -> subscriberResult.setValue(newStream.toList()))
                .toList();

        assertEquals(Arrays.asList(1, 2, 3), mainResult);
        assertNotNull(subscriberResult.value());
    }

    @Test
    @Timeout(10)
    public void testTakeWhileAddSubscriber_BasicFunctionality() throws Exception {
        Stream.of(1, 2, 3, 4, 5).takeWhileAddSubscriber(x -> x < 3, s -> {
            s.forEach(subscriberStreamResults::add);
        }).forEach(x -> {
            mainStreamResults.add(x);
        });

        assertEquals(Arrays.asList(1, 2), mainStreamResults);
        assertEquals(Arrays.asList(3, 4, 5), subscriberStreamResults);
    }

    @Test
    public void testTakeWhileAddSubscriber_NullPredicate() {
        assertThrows(IllegalArgumentException.class, () -> {
            Stream.of(1, 2, 3).takeWhileAddSubscriber(null, s -> {
            }).count();
        });
    }

    @Test
    @Timeout(10)
    public void testTakeWhileAddSubscriber_TakeAll() throws Exception {
        Stream.of(1, 2, 3, 4, 5).takeWhileAddSubscriber(x -> x <= 10, s -> {
            s.forEach(subscriberStreamResults::add);
        }).forEach(x -> {
            mainStreamResults.add(x);
        });

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), mainStreamResults);
        assertTrue(subscriberStreamResults.isEmpty());
    }

    @Test
    @Timeout(10)
    public void testTakeWhileAddSubscriber_TakeNone() throws Exception {
        Stream.of(1, 2, 3, 4, 5).takeWhileAddSubscriber(x -> x > 10, s -> {
            s.forEach(subscriberStreamResults::add);
        }).forEach(x -> {
            mainStreamResults.add(x);
        });

        assertTrue(mainStreamResults.isEmpty());
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), subscriberStreamResults);
    }

    @Test
    @Timeout(10)
    public void testTakeWhileAddSubscriberWithExecutor_CustomExecutor() throws Exception {
        Stream.of(1, 2, 3, 4, 5).takeWhileAddSubscriber(x -> x <= 3, s -> {
            s.forEach(subscriberStreamResults::add);
        }, customExecutor).forEach(x -> {
            mainStreamResults.add(x);
        });

        assertEquals(Arrays.asList(1, 2, 3), mainStreamResults);
        assertEquals(Arrays.asList(4, 5), subscriberStreamResults);
    }

    @Test
    public void testTakeWhileAddSubscriberWithExecutor_NullExecutor() {
        assertThrows(IllegalArgumentException.class, () -> {
            Stream.of(1, 2, 3).takeWhileAddSubscriber(x -> x < 5, s -> {
            }, null).count();
        });
    }

    @Test
    public void testTakeWhileSubscriberPreservesInterruptedOffer() {
        assertSubscriberOfferPreservesInterrupt(2);
    }

    /**
     * {@code transform}/{@code sps}/{@code psp} hand the pipeline to a caller-supplied function and link this
     * stream's {@code close()} onto the result, so that closing the result releases this one. The function was
     * evaluated in the argument expression, so when it threw, {@code linkCloseToThis} never ran and this
     * stream - plus any file or JDBC handle behind it - leaked. {@code linkCloseToThis} already closes this
     * stream when the function returns {@code null} ("a function that returns null cannot be chained; still
     * release this stream rather than leak it"); a throwing function is the same situation, and the sibling
     * {@code Seq.transform} already closes on failure.
     */
    @Test
    public void test_transformSpsPsp_closeUpstreamWhenFunctionThrows_regression_20260918() {
        // Reference behaviour: Seq.transform has always closed the source on failure.
        final AtomicBoolean seqClosed = new AtomicBoolean(false);
        final Seq<Integer, RuntimeException> seq = Seq.<Integer, RuntimeException> of(1, 2, 3).onClose(() -> seqClosed.set(true));
        assertThrows(IllegalStateException.class, () -> seq.transform(s -> {
            throw new IllegalStateException("boom");
        }));
        assertTrue(seqClosed.get());

        // Before the fix each of these left the upstream stream open.
        final AtomicBoolean transformClosed = new AtomicBoolean(false);
        final Stream<Integer> forTransform = Stream.of(1, 2, 3).onClose(() -> transformClosed.set(true));
        assertThrows(IllegalStateException.class, () -> forTransform.transform(s -> {
            throw new IllegalStateException("boom");
        }));
        assertTrue(transformClosed.get(), "transform must close the upstream when the transfer function throws");

        final AtomicBoolean spsClosed = new AtomicBoolean(false);
        final Stream<Integer> forSps = Stream.of(1, 2, 3).onClose(() -> spsClosed.set(true));
        assertThrows(IllegalStateException.class, () -> forSps.sps(s -> {
            throw new IllegalStateException("boom");
        }));
        assertTrue(spsClosed.get(), "sps must close the upstream when the ops function throws");

        final AtomicBoolean pspClosed = new AtomicBoolean(false);
        final Stream<Integer> forPsp = Stream.of(1, 2, 3).onClose(() -> pspClosed.set(true));
        assertThrows(IllegalStateException.class, () -> forPsp.psp(s -> {
            throw new IllegalStateException("boom");
        }));
        assertTrue(pspClosed.get(), "psp must close the upstream when the ops function throws");

        // A function that succeeds must still hand ownership to the result, not close early.
        final AtomicBoolean okClosed = new AtomicBoolean(false);
        final Stream<Integer> ok = Stream.of(1, 2, 3).onClose(() -> okClosed.set(true));
        final Stream<Integer> mapped = ok.transform(s -> s.map(i -> i * 2));
        assertFalse(okClosed.get(), "a successful transform must not close the upstream eagerly");
        assertEquals(Arrays.asList(2, 4, 6), mapped.toList());
        mapped.close();
        assertTrue(okClosed.get(), "closing the result must release the upstream");
    }

    @Test
    public void testBoundedWindowCountClose_eventTimeContinuesFromLastEvent() {
        // regression: when a window closed early on element count in event-time mode, the next window
        // started at the wall clock instead of the last event time, silently dropping buffered events
        final long t0 = System.currentTimeMillis();
        final WindowHandler<Long, List<Long>> handler = WindowHandler.of(Long::longValue);

        final List<List<Long>> windows = Stream.of(t0 - 100_000, t0 - 99_000, t0 - 98_000, t0 - 97_000)
                .window(Duration.ofHours(1), 2, () -> t0 - 200_000, handler, Collectors.toList())
                .toList();

        assertEquals(N.asList(N.asList(t0 - 100_000, t0 - 99_000), N.asList(t0 - 98_000, t0 - 97_000)), windows);
    }

    @Test
    public void testTimeWindowsRejectOverflowingBoundariesAndCloseSource() {
        final WindowHandler<Long, List<Long>> handler = WindowHandler.of((Long timestamp) -> timestamp);

        for (final boolean sliding : new boolean[] { false, true }) {
            for (final boolean initialBoundary : new boolean[] { false, true }) {
                for (final boolean iteratorSource : new boolean[] { false, true }) {
                    final AtomicInteger closeCalls = new AtomicInteger();
                    final long start = initialBoundary ? 1 : sliding ? Long.MAX_VALUE - 3 : Long.MAX_VALUE - 2;
                    final long event = initialBoundary ? 0 : sliding ? Long.MAX_VALUE - 3 : Long.MAX_VALUE - 1;
                    final Duration duration = Duration.ofMillis(initialBoundary ? Long.MAX_VALUE : 2);
                    final Stream<Long> source = (iteratorSource ? Stream.of(Arrays.asList(event).iterator()) : Stream.of(event))
                            .onClose(closeCalls::incrementAndGet);
                    final Stream<List<Long>> windows = sliding ? source.window(duration, duration, () -> start, handler, Collectors.toList())
                            : source.window(duration, 1, () -> start, handler, Collectors.toList());

                    assertThrows(ArithmeticException.class, windows::toList);
                    windows.close();
                    assertEquals(1, closeCalls.get(), "an unrepresentable boundary must still close the source exactly once");
                    assertThrows(IllegalStateException.class, source::count);
                }
            }
        }

        final long start = Long.MAX_VALUE - 4;
        assertEquals(Arrays.asList(Arrays.asList(start)),
                Stream.of(start).window(Duration.ofMillis(2), Duration.ofMillis(2), () -> start, handler, Collectors.toList()).toList());
        assertEquals(Arrays.asList(Arrays.asList(start)), Stream.of(start).window(Duration.ofMillis(2), 1, () -> start, handler, Collectors.toList()).toList());
    }

    // zip

    @Test
    public void testCollectionZipCloseHandlersUseSourceSnapshot() {
        final AtomicInteger closedCount = new AtomicInteger();
        List<Stream<Integer>> sources = new ArrayList<>(
                Arrays.asList(Stream.of(1).onClose(closedCount::incrementAndGet), Stream.of(2).onClose(closedCount::incrementAndGet)));
        Stream<Integer> zipped = Stream.zip(sources, values -> values.get(0) + values.get(1));

        sources.clear();
        zipped.close();
        assertEquals(2, closedCount.get());

        closedCount.set(0);
        sources = new ArrayList<>(Arrays.asList(Stream.of(1).onClose(closedCount::incrementAndGet), Stream.of(2).onClose(closedCount::incrementAndGet)));
        zipped = Stream.zip(sources, Arrays.asList(0, 0), values -> values.get(0) + values.get(1));

        sources.clear();
        zipped.close();
        assertEquals(2, closedCount.get());

        closedCount.set(0);
        sources = new ArrayList<>(Arrays.asList(Stream.of(1).onClose(closedCount::incrementAndGet), Stream.of(2).onClose(closedCount::incrementAndGet)));
        zipped = Stream.parallelZip(sources, values -> values.get(0) + values.get(1), 2);

        sources.clear();
        zipped.close();
        assertEquals(2, closedCount.get());

        closedCount.set(0);
        sources = new ArrayList<>(Arrays.asList(Stream.of(1).onClose(closedCount::incrementAndGet), Stream.of(2).onClose(closedCount::incrementAndGet)));
        zipped = Stream.parallelZip(sources, Arrays.asList(0, 0), values -> values.get(0) + values.get(1), 2);

        sources.clear();
        zipped.close();
        assertEquals(2, closedCount.get());
    }

    @Test
    public void testPrimitiveCollectionZipCloseHandlersUseSourceSnapshots() {
        final AtomicInteger closedCount = new AtomicInteger();

        List<ByteStream> byteStreams = new ArrayList<>(Collections.singletonList(ByteStream.of((byte) 1).onClose(closedCount::incrementAndGet)));
        assertCollectionZipClosesSnapshottedSource(byteStreams, Stream.zip(byteStreams, values -> values[0]), closedCount);
        byteStreams = new ArrayList<>(Collections.singletonList(ByteStream.of((byte) 1).onClose(closedCount::incrementAndGet)));
        assertCollectionZipClosesSnapshottedSource(byteStreams, Stream.zip(byteStreams, new byte[] { 0 }, values -> values[0]), closedCount);

        List<ShortStream> shortStreams = new ArrayList<>(Collections.singletonList(ShortStream.of((short) 1).onClose(closedCount::incrementAndGet)));
        assertCollectionZipClosesSnapshottedSource(shortStreams, Stream.zip(shortStreams, values -> values[0]), closedCount);
        shortStreams = new ArrayList<>(Collections.singletonList(ShortStream.of((short) 1).onClose(closedCount::incrementAndGet)));
        assertCollectionZipClosesSnapshottedSource(shortStreams, Stream.zip(shortStreams, new short[] { 0 }, values -> values[0]), closedCount);

        List<CharStream> charStreams = new ArrayList<>(Collections.singletonList(CharStream.of('a').onClose(closedCount::incrementAndGet)));
        assertCollectionZipClosesSnapshottedSource(charStreams, Stream.zip(charStreams, values -> values[0]), closedCount);
        charStreams = new ArrayList<>(Collections.singletonList(CharStream.of('a').onClose(closedCount::incrementAndGet)));
        assertCollectionZipClosesSnapshottedSource(charStreams, Stream.zip(charStreams, new char[] { 0 }, values -> values[0]), closedCount);

        List<IntStream> intStreams = new ArrayList<>(Collections.singletonList(IntStream.of(1).onClose(closedCount::incrementAndGet)));
        assertCollectionZipClosesSnapshottedSource(intStreams, Stream.zip(intStreams, values -> values[0]), closedCount);
        intStreams = new ArrayList<>(Collections.singletonList(IntStream.of(1).onClose(closedCount::incrementAndGet)));
        assertCollectionZipClosesSnapshottedSource(intStreams, Stream.zip(intStreams, new int[] { 0 }, values -> values[0]), closedCount);

        List<LongStream> longStreams = new ArrayList<>(Collections.singletonList(LongStream.of(1L).onClose(closedCount::incrementAndGet)));
        assertCollectionZipClosesSnapshottedSource(longStreams, Stream.zip(longStreams, values -> values[0]), closedCount);
        longStreams = new ArrayList<>(Collections.singletonList(LongStream.of(1L).onClose(closedCount::incrementAndGet)));
        assertCollectionZipClosesSnapshottedSource(longStreams, Stream.zip(longStreams, new long[] { 0 }, values -> values[0]), closedCount);

        List<FloatStream> floatStreams = new ArrayList<>(Collections.singletonList(FloatStream.of(1F).onClose(closedCount::incrementAndGet)));
        assertCollectionZipClosesSnapshottedSource(floatStreams, Stream.zip(floatStreams, values -> values[0]), closedCount);
        floatStreams = new ArrayList<>(Collections.singletonList(FloatStream.of(1F).onClose(closedCount::incrementAndGet)));
        assertCollectionZipClosesSnapshottedSource(floatStreams, Stream.zip(floatStreams, new float[] { 0 }, values -> values[0]), closedCount);

        List<DoubleStream> doubleStreams = new ArrayList<>(Collections.singletonList(DoubleStream.of(1D).onClose(closedCount::incrementAndGet)));
        assertCollectionZipClosesSnapshottedSource(doubleStreams, Stream.zip(doubleStreams, values -> values[0]), closedCount);
        doubleStreams = new ArrayList<>(Collections.singletonList(DoubleStream.of(1D).onClose(closedCount::incrementAndGet)));
        assertCollectionZipClosesSnapshottedSource(doubleStreams, Stream.zip(doubleStreams, new double[] { 0 }, values -> values[0]), closedCount);
    }

    @Test
    public void testZipClosesOpenedSourceWhenSiblingIterateFails() {
        final AtomicInteger closed = new AtomicInteger();
        final Stream<Integer> a = Stream.of(1, 2).onClose(closed::incrementAndGet);
        final Stream<Integer> b = Stream.of(3);
        b.close();

        assertThrows(IllegalStateException.class, () -> Stream.zip(a, b, Integer::sum).toList());
        assertEquals(1, closed.get());
    }

    @Test
    public void testZipCollectionClosesOnlyOpenedPrefixWhenLaterIterateFails() {
        final AtomicInteger closedA = new AtomicInteger();
        final AtomicInteger closedC = new AtomicInteger();
        final Stream<Integer> a = Stream.of(1).onClose(closedA::incrementAndGet);
        final Stream<Integer> b = Stream.of(2);
        b.close();
        final Stream<Integer> c = Stream.of(3).onClose(closedC::incrementAndGet);

        assertThrows(IllegalStateException.class, () -> Stream.zip(Arrays.asList(a, b, c), list -> 0).toList());
        assertEquals(1, closedA.get());
        assertEquals(0, closedC.get());
        assertEquals(Arrays.asList(3), c.toList());
    }

    @Test
    public void testZipDoesNotCloseUnopenedSiblingWhenFirstIterateFails() {
        final AtomicInteger closed = new AtomicInteger();
        final Stream<Integer> a = Stream.of(1, 2);
        a.close();
        final Stream<Integer> b = Stream.of(3, 4).onClose(closed::incrementAndGet);

        assertThrows(IllegalStateException.class, () -> Stream.zip(a, b, Integer::sum).toList());
        assertEquals(0, closed.get());
        assertEquals(Arrays.asList(3, 4), b.toList());
    }

}
