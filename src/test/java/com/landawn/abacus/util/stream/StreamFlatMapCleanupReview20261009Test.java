package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.ShortIterator;

@Tag("unit")
class StreamFlatMapCleanupReview20261009Test extends TestBase {

    @ParameterizedTest(name = "{0}")
    @MethodSource("flatMapPaths")
    void closeLeavesTheTraversingIteratorIntact(final FlatMapPath path) throws Exception {
        final AtomicInteger mapperCalls = new AtomicInteger();
        final AtomicInteger childCloses = new AtomicInteger();
        final Stream<?> flattened = flatten(path, value -> {
            mapperCalls.incrementAndGet();
            return child(path.method().getReturnType(), number(value), () -> {
            }, childCloses::incrementAndGet);
        });

        try (flattened) {
            final Iterator<?> iterator = flattened.iterator();
            assertTrue(iterator.hasNext());

            // Model cancellation between a reader's hasNext() and next(). Closing the child must leave the
            // reader's iterator intact so the already pending element is neither skipped nor replaced.
            flattened.close();

            assertEquals(1, childCloses.get());
            assertEquals(1, number(iterator.next()));
            assertEquals(1, mapperCalls.get());
        }

        assertEquals(1, childCloses.get());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("flatMapPaths")
    void bufferedShortCircuitClosesLateMappedStream(final FlatMapPath path) throws Exception {
        final CountDownLatch mapperStarted = new CountDownLatch(1);
        final CountDownLatch releaseMapper = new CountDownLatch(1);
        final CountDownLatch mappedObserved = new CountDownLatch(1);
        final AtomicInteger mapperCalls = new AtomicInteger();
        final AtomicInteger lateCloses = new AtomicInteger();
        final AtomicInteger lateReads = new AtomicInteger();
        final Stream<?> flattened = flatten(path, ignored -> {
            if (mapperCalls.incrementAndGet() == 1) {
                return child(path.method().getReturnType(), 1, () -> {
                }, () -> {
                });
            }

            mapperStarted.countDown();
            await(releaseMapper);
            return child(path.method().getReturnType(), 2, () -> {
                lateReads.incrementAndGet();
                mappedObserved.countDown();
            }, () -> {
                lateCloses.incrementAndGet();
                mappedObserved.countDown();
            });
        });

        try {
            // Keep the mapper in flight until first() has closed the buffered pipeline.
            assertEquals(1, number(flattened.buffered(1).onEach(ignored -> await(mapperStarted)).first().orElseThrow()));
        } finally {
            releaseMapper.countDown();
            flattened.close();
        }

        assertTrue(mappedObserved.await(5, TimeUnit.SECONDS), "the buffered reader did not finish its mapper");
        assertEquals(1, lateCloses.get(), "a mapped stream returned after pipeline closure must be closed");
        assertEquals(0, lateReads.get(), "a mapped stream returned after pipeline closure must not be traversed");
        flattened.close();
        assertEquals(1, lateCloses.get());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("flatMapPaths")
    @SuppressWarnings("deprecation")
    void closingBeforeTraversalPreventsEveryLaterMapperCall(final FlatMapPath path) throws Exception {
        final AtomicInteger mapperCalls = new AtomicInteger();
        final Stream<?> flattened = flatten(path, value -> {
            mapperCalls.incrementAndGet();
            return child(path.method().getReturnType(), number(value), () -> {
            }, () -> {
            });
        });
        final var iterator = flattened.iterator();

        flattened.close();
        assertFalse(iterator.hasNext());
        assertFalse(iterator.hasNext());
        assertEquals(0, mapperCalls.get());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("flatMapPaths")
    @SuppressWarnings("deprecation")
    void advancingWithinAChildChecksItsIteratorOnce(final FlatMapPath path) throws Exception {
        final AtomicInteger childChecks = new AtomicInteger();
        final Stream<?> flattened = flatten(path, value -> countedChild(path.method().getReturnType(), number(value), childChecks));

        try (flattened) {
            final var iterator = flattened.iterator();
            assertTrue(iterator.hasNext());
            assertEquals(1, childChecks.get());
            assertTrue(iterator.hasNext());
            assertEquals(2, childChecks.get(), "the per-element path must not repeat the child's hasNext callback");

            final List<Integer> values = new ArrayList<>();

            while (iterator.hasNext()) {
                values.add(number(iterator.next()));
            }

            assertEquals(List.of(1, 11, 2, 12), values);
            assertFalse(iterator.hasNext());
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("ownershipPaths")
    void pipelineCloseDoesNotWaitForAChildCallbackAlreadyClaimedByTheReader(final FlatMapPath path) throws Exception {
        final CountDownLatch childCloseStarted = new CountDownLatch(1);
        final CountDownLatch releaseChildClose = new CountDownLatch(1);
        final CountDownLatch childCloseCompleted = new CountDownLatch(1);
        final AtomicInteger mapperCalls = new AtomicInteger();
        final Stream<?> flattened = flatten(path, value -> {
            mapperCalls.incrementAndGet();
            return child(path.method().getReturnType(), number(value), () -> {
            }, () -> {
                childCloseStarted.countDown();
                await(releaseChildClose, 15);
                childCloseCompleted.countDown();
            });
        });
        final FutureTask<Integer> terminal = startReader("flatMap buffered consumer",
                () -> number(flattened.buffered(1).onEach(ignored -> await(childCloseStarted)).first().orElseThrow()));

        try {
            // A second child.close() would block behind the reader's callback and prevent first() from completing.
            assertEquals(1, terminal.get(5, TimeUnit.SECONDS));
            assertEquals(1, childCloseCompleted.getCount());
        } finally {
            releaseChildClose.countDown();
            finish(terminal);
            await(childCloseCompleted);
            flattened.close();
        }

        assertEquals(1, mapperCalls.get());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("flatMapPaths")
    void cancellationClosesChildWhileItsIteratorIsBeingAcquired(final FlatMapPath path) throws Exception {
        final CountDownLatch acquiring = new CountDownLatch(1);
        final CountDownLatch releaseAcquisition = new CountDownLatch(1);
        final AtomicInteger childCloses = new AtomicInteger();
        final ChildProbe child = new ChildProbe(path.method().getReturnType(), () -> {
            acquiring.countDown();
            await(releaseAcquisition, 15);
        }, () -> {
        }, childCloses::incrementAndGet);
        final Stream<?> flattened = flatten(path, ignored -> child.stream);
        final Iterator<?> iterator = flattened.iterator();
        final FutureTask<Boolean> reader = startReader("flatMap iterator acquisition", iterator::hasNext);
        FutureTask<Void> closer = null;

        try {
            await(acquiring);
            closer = startReader("flatMap acquisition cancellation", () -> {
                flattened.close();
                return null;
            });

            // Publication must precede iterator acquisition: close must find the child before iteratorEx returns.
            closer.get(5, TimeUnit.SECONDS);
            assertEquals(1, childCloses.get());
            assertEquals(1, releaseAcquisition.getCount());
            assertEquals(0, child.hasNextCalls.get());
            releaseAcquisition.countDown();
            assertTrue(reader.get(5, TimeUnit.SECONDS));
            assertEquals(1, number(iterator.next()));
            assertFalse(iterator.hasNext());
            flattened.close();
            assertEquals(1, child.iteratorRequests.get());
            assertEquals(1, childCloses.get());
        } finally {
            releaseAcquisition.countDown();
            finish(reader);
            finish(closer);
            flattened.close();
            child.stream.close();
        }
    }

    @ParameterizedTest(name = "{0}, throwingClose={1}, insideNext={2}")
    @MethodSource("activeChildReadCases")
    void cancellationPreservesAnActiveChildRead(final FlatMapPath path, final boolean throwingClose, final boolean insideNext) throws Exception {
        final CountDownLatch reading = new CountDownLatch(1);
        final CountDownLatch releaseRead = new CountDownLatch(1);
        final AtomicBoolean blockRead = new AtomicBoolean();
        final AtomicInteger childCloses = new AtomicInteger();
        final AtomicInteger mapperCalls = new AtomicInteger();
        final IllegalStateException failure = new IllegalStateException("active child close");
        final ChildProbe child = new ChildProbe(path.method().getReturnType(), () -> {
        }, () -> {
            if (blockRead.get()) {
                reading.countDown();
                await(releaseRead, 15);
            }
        }, () -> {
            childCloses.incrementAndGet();
            if (throwingClose) {
                throw failure;
            }
        });
        final Stream<?> flattened = flatten(path, ignored -> {
            mapperCalls.incrementAndGet();
            return child.stream;
        });
        final Iterator<?> iterator = flattened.iterator();

        if (insideNext) {
            assertTrue(iterator.hasNext());
        }

        blockRead.set(true);
        final FutureTask<Integer> reader = startReader("flatMap active child read", () -> {
            if (!insideNext) {
                assertTrue(iterator.hasNext(), "cancellation must not discard the pending element");
            }
            return number(iterator.next());
        });
        FutureTask<Void> closer = null;

        try {
            await(reading);
            closer = startReader("flatMap active child cancellation", () -> {
                flattened.close();
                return null;
            });

            // Also close during the child.hasNext() inside outer next(): clearing cur there used to cause an NPE
            // between its two reads. A throwing child close belongs to the closer, not the blocked reader.
            assertCloseResult(closer, throwingClose, failure);
            assertEquals(1, childCloses.get());
            assertEquals(1, releaseRead.getCount());
            releaseRead.countDown();
            assertEquals(1, reader.get(5, TimeUnit.SECONDS));
            assertFalse(iterator.hasNext());
            assertEquals(1, mapperCalls.get());
            flattened.close();
            assertEquals(1, childCloses.get());
        } finally {
            releaseRead.countDown();
            finish(reader);
            finish(closer);
            flattened.close();
            child.stream.close();
        }
    }

    @ParameterizedTest(name = "{0}, throwingClose={1}")
    @MethodSource("lateChildCases")
    void lateChildIsClosedWithoutAcquiringItsIterator(final FlatMapPath path, final boolean throwingClose) throws Exception {
        final CountDownLatch mapping = new CountDownLatch(1);
        final CountDownLatch releaseMapper = new CountDownLatch(1);
        final AtomicInteger childCloses = new AtomicInteger();
        final IllegalStateException failure = new IllegalStateException("late child close");
        final ChildProbe child = new ChildProbe(path.method().getReturnType(), () -> {
        }, () -> {
        }, () -> {
            childCloses.incrementAndGet();
            if (throwingClose) {
                throw failure;
            }
        });
        final Stream<?> flattened = flatten(path, ignored -> {
            mapping.countDown();
            await(releaseMapper, 15);
            return child.stream;
        });
        final Iterator<?> iterator = flattened.iterator();
        final FutureTask<Boolean> reader = startReader("flatMap late mapper", iterator::hasNext);
        FutureTask<Void> closer = null;

        try {
            await(mapping);
            closer = startReader("flatMap late mapper cancellation", () -> {
                flattened.close();
                return null;
            });
            closer.get(5, TimeUnit.SECONDS);
            assertEquals(0, childCloses.get());

            // The closer has finished before this child is returned. Its cleanup, including any failure, must
            // therefore happen on the producing thread before even obtaining the child's iterator.
            releaseMapper.countDown();
            if (throwingClose) {
                assertSame(failure, assertThrows(ExecutionException.class, () -> reader.get(5, TimeUnit.SECONDS)).getCause());
            } else {
                assertFalse(reader.get(5, TimeUnit.SECONDS));
            }
            assertEquals(1, childCloses.get());
            assertEquals(0, child.iteratorRequests.get());
            assertEquals(0, child.hasNextCalls.get());
            assertEquals(0, child.nextCalls.get());
            flattened.close();
            assertEquals(1, childCloses.get());
        } finally {
            releaseMapper.countDown();
            finish(reader);
            finish(closer);
            flattened.close();
            child.stream.close();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void reentrantPipelineCloseFromAChildCallbackStopsMapping(final boolean iteratorSource) {
        final AtomicInteger mapperCalls = new AtomicInteger();
        final AtomicInteger childCloses = new AtomicInteger();
        final AtomicReference<Stream<Integer>> result = new AtomicReference<>();
        final Stream<Integer> source = iteratorSource ? Stream.of(List.of(1, 2).iterator()) : Stream.of(1, 2);
        final Stream<Integer> flattened = source.flatMap(value -> {
            mapperCalls.incrementAndGet();
            return Stream.of(value).onClose(() -> {
                childCloses.incrementAndGet();
                result.get().close();
            });
        });
        result.set(flattened);

        assertEquals(List.of(1), flattened.toList());
        assertEquals(1, mapperCalls.get());
        assertEquals(1, childCloses.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void closesThePreviousChildBeforeInvokingTheNextMapper(final boolean iteratorSource) {
        final List<String> events = new ArrayList<>();
        final Stream<Integer> source = iteratorSource ? Stream.of(List.of(1, 2).iterator()) : Stream.of(1, 2);

        assertEquals(List.of(1, 2), source.flatMap(value -> {
            events.add("map" + value);
            return Stream.of(value).onClose(() -> events.add("close" + value));
        }).toList());
        assertEquals(List.of("map1", "close1", "map2", "close2"), events);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("flatMapPaths")
    void normalTraversalClosesEveryMappedStreamOnce(final FlatMapPath path) throws Exception {
        final AtomicInteger childCloses = new AtomicInteger();
        final Stream<?> flattened = flatten(path, value -> child(path.method().getReturnType(), number(value), () -> {
        }, childCloses::incrementAndGet));

        assertEquals(List.of(1, 2), flattened.map(StreamFlatMapCleanupReview20261009Test::number).toList());
        assertEquals(2, childCloses.get());
        flattened.close();
        assertEquals(2, childCloses.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void nullMappedStreamDoesNotRetainThePreviousChild(final boolean iteratorSource) {
        final AtomicInteger childCloses = new AtomicInteger();
        final Stream<Integer> source = iteratorSource ? Stream.of(List.of(1, 2, 3).iterator()) : Stream.of(1, 2, 3);

        assertEquals(List.of(1, 3), source.flatMap(value -> value == 2 ? null : Stream.of(value).onClose(childCloses::incrementAndGet)).toList());
        assertEquals(2, childCloses.get());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void throwingChildCloseIsNotRetriedByPipelineCleanup(final boolean iteratorSource) {
        final AtomicInteger childCloses = new AtomicInteger();
        final IllegalStateException failure = new IllegalStateException("child close failed");
        final Stream<Integer> source = iteratorSource ? Stream.of(List.of(1, 2).iterator()) : Stream.of(1, 2);
        final Stream<Integer> flattened = source.flatMap(value -> Stream.of(value).onClose(() -> {
            childCloses.incrementAndGet();
            throw failure;
        }));

        assertSame(failure, assertThrows(IllegalStateException.class, flattened::toList));
        assertEquals(1, childCloses.get());
        flattened.close();
        assertEquals(1, childCloses.get());
    }

    private record FlatMapPath(Method method, boolean iteratorSource) {
        @Override
        public String toString() {
            return method.getDeclaringClass().getSimpleName() + "." + method.getName() + (iteratorSource ? " iterator" : " array");
        }
    }

    private static List<FlatMapPath> flatMapPaths() {
        final List<FlatMapPath> paths = new ArrayList<>();

        // Exercise each supported primitive/object conversion for both sequential implementations.
        for (final Class<?> source : List.of(Stream.class, CharStream.class, ByteStream.class, ShortStream.class, IntStream.class, LongStream.class,
                FloatStream.class, DoubleStream.class)) {
            Arrays.stream(source.getDeclaredMethods())
                    .filter(method -> method.getName().matches("flatMap(?:To(?:Obj|Char|Byte|Short|Int|Long|Float|Double))?")
                            && method.getParameterCount() == 1)
                    .sorted(Comparator.comparing(Method::getName))
                    .forEach(method -> {
                        paths.add(new FlatMapPath(method, false));
                        paths.add(new FlatMapPath(method, true));
                    });
        }

        return paths;
    }

    private static List<FlatMapPath> ownershipPaths() {
        // Object output embeds ownership in the iterator; primitive output uses a separate holder.
        return flatMapPaths().stream()
                .filter(path -> path.method().getName().equals("flatMap")
                        && (path.method().getDeclaringClass() == Stream.class || path.method().getDeclaringClass() == IntStream.class))
                .toList();
    }

    private static List<Arguments> activeChildReadCases() {
        final List<Arguments> cases = new ArrayList<>();
        for (final FlatMapPath path : flatMapPaths()) {
            for (final boolean throwingClose : new boolean[] { false, true }) {
                for (final boolean insideNext : new boolean[] { false, true }) {
                    cases.add(Arguments.of(path, throwingClose, insideNext));
                }
            }
        }
        return cases;
    }

    private static List<Arguments> lateChildCases() {
        final List<Arguments> cases = new ArrayList<>();
        for (final FlatMapPath path : flatMapPaths()) {
            cases.add(Arguments.of(path, false));
            cases.add(Arguments.of(path, true));
        }
        return cases;
    }

    private static final class ChildProbe {
        final AtomicInteger iteratorRequests = new AtomicInteger();
        final AtomicInteger hasNextCalls = new AtomicInteger();
        final AtomicInteger nextCalls = new AtomicInteger();
        final BaseStream<?, ?, ?, ?, ?, ?, ?, ?> stream;

        ChildProbe(final Class<?> type, final Runnable acquiring, final Runnable reading, final Runnable closing) {
            final Iterator<Integer> values = new Iterator<>() {
                private boolean emitted;

                @Override
                public boolean hasNext() {
                    hasNextCalls.incrementAndGet();
                    reading.run();
                    return !emitted;
                }

                @Override
                public Integer next() {
                    nextCalls.incrementAndGet();
                    if (emitted) {
                        throw new NoSuchElementException();
                    }
                    emitted = true;
                    return 1;
                }
            };

            // Override the actual output type's iteratorEx so every flatMap path can be paused during
            // acquisition of its own typed child iterator, before any traversal callbacks are invoked.
            if (type == Stream.class) {
                stream = new IteratorStream<>(values) {
                    @Override
                    ObjIteratorEx<Integer> iteratorEx() {
                        iteratorRequests.incrementAndGet();
                        final ObjIteratorEx<Integer> iterator = super.iteratorEx();
                        acquiring.run();
                        return iterator;
                    }
                }.onClose(closing);
            } else if (type == CharStream.class) {
                stream = new IteratorCharStream(new CharIterator() {
                    @Override
                    public boolean hasNext() {
                        return values.hasNext();
                    }

                    @Override
                    public char nextChar() {
                        return (char) values.next().intValue();
                    }
                }) {
                    @Override
                    CharIteratorEx iteratorEx() {
                        iteratorRequests.incrementAndGet();
                        final CharIteratorEx iterator = super.iteratorEx();
                        acquiring.run();
                        return iterator;
                    }
                }.onClose(closing);
            } else if (type == ByteStream.class) {
                stream = new IteratorByteStream(new ByteIterator() {
                    @Override
                    public boolean hasNext() {
                        return values.hasNext();
                    }

                    @Override
                    public byte nextByte() {
                        return values.next().byteValue();
                    }
                }) {
                    @Override
                    ByteIteratorEx iteratorEx() {
                        iteratorRequests.incrementAndGet();
                        final ByteIteratorEx iterator = super.iteratorEx();
                        acquiring.run();
                        return iterator;
                    }
                }.onClose(closing);
            } else if (type == ShortStream.class) {
                stream = new IteratorShortStream(new ShortIterator() {
                    @Override
                    public boolean hasNext() {
                        return values.hasNext();
                    }

                    @Override
                    public short nextShort() {
                        return values.next().shortValue();
                    }
                }) {
                    @Override
                    ShortIteratorEx iteratorEx() {
                        iteratorRequests.incrementAndGet();
                        final ShortIteratorEx iterator = super.iteratorEx();
                        acquiring.run();
                        return iterator;
                    }
                }.onClose(closing);
            } else if (type == IntStream.class) {
                stream = new IteratorIntStream(new IntIterator() {
                    @Override
                    public boolean hasNext() {
                        return values.hasNext();
                    }

                    @Override
                    public int nextInt() {
                        return values.next();
                    }
                }) {
                    @Override
                    IntIteratorEx iteratorEx() {
                        iteratorRequests.incrementAndGet();
                        final IntIteratorEx iterator = super.iteratorEx();
                        acquiring.run();
                        return iterator;
                    }
                }.onClose(closing);
            } else if (type == LongStream.class) {
                stream = new IteratorLongStream(new LongIterator() {
                    @Override
                    public boolean hasNext() {
                        return values.hasNext();
                    }

                    @Override
                    public long nextLong() {
                        return values.next().longValue();
                    }
                }) {
                    @Override
                    LongIteratorEx iteratorEx() {
                        iteratorRequests.incrementAndGet();
                        final LongIteratorEx iterator = super.iteratorEx();
                        acquiring.run();
                        return iterator;
                    }
                }.onClose(closing);
            } else if (type == FloatStream.class) {
                stream = new IteratorFloatStream(new FloatIterator() {
                    @Override
                    public boolean hasNext() {
                        return values.hasNext();
                    }

                    @Override
                    public float nextFloat() {
                        return values.next().floatValue();
                    }
                }) {
                    @Override
                    FloatIteratorEx iteratorEx() {
                        iteratorRequests.incrementAndGet();
                        final FloatIteratorEx iterator = super.iteratorEx();
                        acquiring.run();
                        return iterator;
                    }
                }.onClose(closing);
            } else if (type == DoubleStream.class) {
                stream = new IteratorDoubleStream(new DoubleIterator() {
                    @Override
                    public boolean hasNext() {
                        return values.hasNext();
                    }

                    @Override
                    public double nextDouble() {
                        return values.next().doubleValue();
                    }
                }) {
                    @Override
                    DoubleIteratorEx iteratorEx() {
                        iteratorRequests.incrementAndGet();
                        final DoubleIteratorEx iterator = super.iteratorEx();
                        acquiring.run();
                        return iterator;
                    }
                }.onClose(closing);
            } else {
                throw new AssertionError(type);
            }
        }
    }

    private static <T> FutureTask<T> startReader(final String name, final Callable<T> action) {
        final FutureTask<T> result = new FutureTask<>(action);
        final Thread thread = new Thread(result, name);
        // A broken implementation can deadlock on stream monitors, which interruption cannot release.
        thread.setDaemon(true);
        thread.start();
        return result;
    }

    private static void assertCloseResult(final Future<?> closer, final boolean throwingClose, final RuntimeException failure) throws Exception {
        if (throwingClose) {
            assertSame(failure, assertThrows(ExecutionException.class, () -> closer.get(5, TimeUnit.SECONDS)).getCause());
        } else {
            closer.get(5, TimeUnit.SECONDS);
        }
    }

    private static void finish(final Future<?> task) throws Exception {
        if (task != null) {
            try {
                task.get(5, TimeUnit.SECONDS);
            } catch (final ExecutionException ignored) {
                // The scenario asserts worker failures explicitly; cleanup only waits for them to finish.
            }
        }
    }

    private static Stream<?> flatten(final FlatMapPath path, final Function<Object, Object> mapper) throws ReflectiveOperationException {
        final Class<?> mapperType = path.method().getParameterTypes()[0];
        final Object mapperProxy = Proxy.newProxyInstance(mapperType.getClassLoader(), new Class<?>[] { mapperType },
                (proxy, method, args) -> mapper.apply(args[0]));
        final Object flattened = path.method().invoke(source(path.method().getDeclaringClass(), path.iteratorSource()), mapperProxy);

        return flattened instanceof Stream<?> stream ? stream : (Stream<?>) path.method().getReturnType().getMethod("boxed").invoke(flattened);
    }

    private static Object source(final Class<?> type, final boolean iteratorSource) {
        return switch (type.getSimpleName()) {
            case "Stream" -> iteratorSource ? Stream.of(List.of(1, 2).iterator()) : Stream.of(1, 2);
            case "CharStream" -> iteratorSource ? CharStream.of(CharIterator.of((char) 1, (char) 2)) : CharStream.of((char) 1, (char) 2);
            case "ByteStream" -> iteratorSource ? ByteStream.of(ByteIterator.of((byte) 1, (byte) 2)) : ByteStream.of((byte) 1, (byte) 2);
            case "ShortStream" -> iteratorSource ? ShortStream.of(ShortIterator.of((short) 1, (short) 2)) : ShortStream.of((short) 1, (short) 2);
            case "IntStream" -> iteratorSource ? IntStream.of(IntIterator.of(1, 2)) : IntStream.of(1, 2);
            case "LongStream" -> iteratorSource ? LongStream.of(LongIterator.of(1L, 2L)) : LongStream.of(1L, 2L);
            case "FloatStream" -> iteratorSource ? FloatStream.of(FloatIterator.of(1f, 2f)) : FloatStream.of(1f, 2f);
            case "DoubleStream" -> iteratorSource ? DoubleStream.of(DoubleIterator.of(1d, 2d)) : DoubleStream.of(1d, 2d);
            default -> throw new AssertionError(type);
        };
    }

    private static Object child(final Class<?> type, final int value, final Runnable observed, final Runnable close) {
        return switch (type.getSimpleName()) {
            case "Stream" -> Stream.of(value).onEach(ignored -> observed.run()).onClose(close);
            case "CharStream" -> CharStream.of((char) value).onEach(ignored -> observed.run()).onClose(close);
            case "ByteStream" -> ByteStream.of((byte) value).onEach(ignored -> observed.run()).onClose(close);
            case "ShortStream" -> ShortStream.of((short) value).onEach(ignored -> observed.run()).onClose(close);
            case "IntStream" -> IntStream.of(value).onEach(ignored -> observed.run()).onClose(close);
            case "LongStream" -> LongStream.of(value).onEach(ignored -> observed.run()).onClose(close);
            case "FloatStream" -> FloatStream.of(value).onEach(ignored -> observed.run()).onClose(close);
            case "DoubleStream" -> DoubleStream.of(value).onEach(ignored -> observed.run()).onClose(close);
            default -> throw new AssertionError(type);
        };
    }

    private static Object countedChild(final Class<?> type, final int value, final AtomicInteger checks) {
        final Stream<Integer> child = Stream.of(new Iterator<>() {
            private int cursor;

            @Override
            public boolean hasNext() {
                checks.incrementAndGet();
                return cursor < 2;
            }

            @Override
            public Integer next() {
                if (cursor >= 2) {
                    throw new NoSuchElementException();
                }

                return value + 10 * cursor++;
            }
        });

        return switch (type.getSimpleName()) {
            case "Stream" -> child;
            case "CharStream" -> child.mapToChar(v -> (char) v.intValue());
            case "ByteStream" -> child.mapToByte(Integer::byteValue);
            case "ShortStream" -> child.mapToShort(Integer::shortValue);
            case "IntStream" -> child.mapToInt(Integer::intValue);
            case "LongStream" -> child.mapToLong(Integer::longValue);
            case "FloatStream" -> child.mapToFloat(Integer::floatValue);
            case "DoubleStream" -> child.mapToDouble(Integer::doubleValue);
            default -> throw new AssertionError(type);
        };
    }

    private static int number(final Object value) {
        return value instanceof Character character ? character : ((Number) value).intValue();
    }

    private static void await(final CountDownLatch latch) {
        await(latch, 5);
    }

    private static void await(final CountDownLatch latch, final int timeoutSeconds) {
        try {
            // Blocking worker gates use 15 seconds, beyond the caller's 5-second completion deadline, so a
            // blocked close cannot appear to succeed just because the worker's gate timed out first.
            assertTrue(latch.await(timeoutSeconds, TimeUnit.SECONDS), "timed out coordinating the buffered reader");
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
