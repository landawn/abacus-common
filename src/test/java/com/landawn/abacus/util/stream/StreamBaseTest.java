package com.landawn.abacus.util.stream;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.BooleanList;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.Wrapper;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings;
import com.landawn.abacus.util.stream.BaseStream.SplitStrategy;
import com.landawn.abacus.util.stream.StreamBase.LocalArrayDeque;
import com.landawn.abacus.util.stream.StreamBase.LocalRunnable;

public class StreamBaseTest extends TestBase {

    @Test
    public void testSkipAndLimitHonorsLongMaxValueForUnboundedSource() {
        for (long offset : new long[] { 0, 1 }) {
            List<Long> advances = new ArrayList<>();
            ObjIteratorEx<Integer> source = new ObjIteratorEx<>() {
                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public Integer next() {
                    return 1;
                }

                @Override
                boolean supportsFailureAtomicAdvance() {
                    return true;
                }

                @Override
                public void advance(final long n) {
                    advances.add(n);
                }
            };

            try (Stream<Integer> limited = Stream.of(source).skipAndLimit(offset, Long.MAX_VALUE)) {
                ObjIteratorEx<Integer> iter = limited.iteratorEx();
                iter.advance(Long.MAX_VALUE);
                Assertions.assertFalse(iter.hasNext());
                Assertions.assertThrows(NoSuchElementException.class, iter::next);
                Assertions.assertEquals(offset == 0 ? List.of(Long.MAX_VALUE) : List.of(offset, Long.MAX_VALUE), advances);
            }
        }
    }

    @Test
    public void testIfEmptyRetriesSourceInspectionAndRunsActionOnce() {
        for (int type = 0; type < 8; type++) {
            for (boolean actionFails : new boolean[] { false, true }) {
                AtomicInteger inspections = new AtomicInteger();
                AtomicInteger actions = new AtomicInteger();
                Stream<Integer> source = Stream.of(new Iterator<Integer>() {
                    @Override
                    public boolean hasNext() {
                        if (inspections.getAndIncrement() == 0) {
                            throw new IllegalStateException("empty source inspection failed");
                        }
                        return false;
                    }

                    @Override
                    public Integer next() {
                        throw new NoSuchElementException();
                    }
                });
                Runnable action = () -> {
                    actions.incrementAndGet();
                    if (actionFails) {
                        throw new IllegalArgumentException("empty action failed");
                    }
                };
                try (BaseStream<?, ?, ?, ?, ?, ?, ?, ?> stream = switch (type) {
                    case 0 -> source.ifEmpty(action);
                    case 1 -> source.mapToByte(Number::byteValue).ifEmpty(action);
                    case 2 -> source.mapToChar(value -> (char) value.intValue()).ifEmpty(action);
                    case 3 -> source.mapToShort(Number::shortValue).ifEmpty(action);
                    case 4 -> source.mapToInt(Number::intValue).ifEmpty(action);
                    case 5 -> source.mapToLong(Number::longValue).ifEmpty(action);
                    case 6 -> source.mapToFloat(Number::floatValue).ifEmpty(action);
                    case 7 -> source.mapToDouble(Number::doubleValue).ifEmpty(action);
                    default -> throw new AssertionError(type);
                }) {
                    Iterator<?> iterator = stream.iterator();
                    Assertions.assertThrows(IllegalStateException.class, iterator::hasNext);
                    Assertions.assertEquals(0, actions.get());
                    if (actionFails) {
                        Assertions.assertThrows(IllegalArgumentException.class, iterator::hasNext);
                    } else {
                        Assertions.assertFalse(iterator.hasNext());
                    }
                    Assertions.assertFalse(iterator.hasNext());
                    Assertions.assertEquals(1, actions.get(), "stream type " + type);
                    Assertions.assertThrows(NoSuchElementException.class, iterator::next);
                }
            }
        }
    }

    @Test
    public void testArrayTopRetriesAfterComparatorFailure() {
        for (int type = 0; type < 6; type++) {
            for (boolean nextFirst : new boolean[] { false, true }) {
                java.util.concurrent.atomic.AtomicInteger comparisons = new java.util.concurrent.atomic.AtomicInteger();
                java.util.Comparator<Number> comparator = (left, right) -> {
                    if (comparisons.getAndIncrement() == 0) {
                        throw new IllegalStateException("top comparison failed");
                    }
                    return Double.compare(left.doubleValue(), right.doubleValue());
                };
                try (BaseStream<?, ?, ?, ?, ?, ?, ?, ?> stream = switch (type) {
                    case 0 -> Stream.of(3, 1, 4, 2).top(2, comparator);
                    case 1 -> ShortStream.of((short) 3, (short) 1, (short) 4, (short) 2).top(2, comparator);
                    case 2 -> IntStream.of(3, 1, 4, 2).top(2, comparator);
                    case 3 -> LongStream.of(3, 1, 4, 2).top(2, comparator);
                    case 4 -> FloatStream.of(3, 1, 4, 2).top(2, comparator);
                    case 5 -> DoubleStream.of(3, 1, 4, 2).top(2, comparator);
                    default -> throw new AssertionError(type);
                }) {
                    java.util.Iterator<?> iterator = stream.iterator();
                    if (nextFirst) {
                        Assertions.assertThrows(IllegalStateException.class, iterator::next);
                    } else {
                        Assertions.assertThrows(IllegalStateException.class, iterator::hasNext);
                    }
                    java.util.List<Integer> actual = new java.util.ArrayList<>();
                    iterator.forEachRemaining(value -> actual.add(((Number) value).intValue()));
                    java.util.Collections.sort(actual);
                    Assertions.assertEquals(java.util.Arrays.asList(3, 4), actual, "array type " + type);
                    Assertions.assertThrows(java.util.NoSuchElementException.class, iterator::next);
                }
            }
        }
    }

    @Test
    public void testDropWhileContinuesDroppingAfterPredicateFailure() {
        for (int type = 0; type < 8; type++) {
            for (boolean iteratorSource : new boolean[] { false, true }) {
                for (int failAt : new int[] { 1, 2 }) {
                    AtomicBoolean failed = new AtomicBoolean();
                    IllegalStateException failure = new IllegalStateException("predicate failed");
                    java.util.function.IntPredicate predicate = value -> {
                        Assertions.assertTrue(value <= 3, "predicate must stop at the first retained element");
                        if (value == failAt && failed.compareAndSet(false, true)) {
                            throw failure;
                        }
                        return value < 3;
                    };

                    try (BaseStream<?, ?, ?, ?, ?, ?, ?, ?> stream = dropWhileRegressionStream(type, iteratorSource, predicate)) {
                        Iterator<?> iterator = stream.iterator();
                        Assertions.assertSame(failure, Assertions.assertThrows(IllegalStateException.class, iterator::hasNext));
                        Assertions.assertTrue(iterator.hasNext());
                        Assertions.assertTrue(iterator.hasNext());
                        Object first = iterator.next();
                        Object second = iterator.next();
                        Assertions.assertEquals(3, first instanceof Character value ? value.charValue() : ((Number) first).intValue());
                        Assertions.assertEquals(4, second instanceof Character value ? value.charValue() : ((Number) second).intValue());
                        Assertions.assertFalse(iterator.hasNext());
                        Assertions.assertThrows(NoSuchElementException.class, iterator::next);
                    }
                }
            }
        }
    }

    private BaseStream<?, ?, ?, ?, ?, ?, ?, ?> dropWhileRegressionStream(int type, boolean iteratorSource, java.util.function.IntPredicate predicate) {
        return switch (type) {
            case 0 -> (iteratorSource ? Stream.of(ObjIteratorEx.of(1, 2, 3, 4)) : Stream.of(1, 2, 3, 4)).dropWhile(predicate::test);
            case 1 -> (iteratorSource ? ByteStream.of(ByteIteratorEx.of((byte) 1, (byte) 2, (byte) 3, (byte) 4))
                    : ByteStream.of((byte) 1, (byte) 2, (byte) 3, (byte) 4)).dropWhile(predicate::test);
            case 2 -> (iteratorSource ? CharStream.of(CharIteratorEx.of((char) 1, (char) 2, (char) 3, (char) 4))
                    : CharStream.of((char) 1, (char) 2, (char) 3, (char) 4)).dropWhile(predicate::test);
            case 3 -> (iteratorSource ? ShortStream.of(ShortIteratorEx.of((short) 1, (short) 2, (short) 3, (short) 4))
                    : ShortStream.of((short) 1, (short) 2, (short) 3, (short) 4)).dropWhile(predicate::test);
            case 4 -> (iteratorSource ? IntStream.of(IntIteratorEx.of(1, 2, 3, 4)) : IntStream.of(1, 2, 3, 4)).dropWhile(predicate::test);
            case 5 -> (iteratorSource ? LongStream.of(LongIteratorEx.of(1, 2, 3, 4)) : LongStream.of(1, 2, 3, 4))
                    .dropWhile(value -> predicate.test((int) value));
            case 6 -> (iteratorSource ? FloatStream.of(FloatIteratorEx.of(1, 2, 3, 4)) : FloatStream.of(1, 2, 3, 4))
                    .dropWhile(value -> predicate.test((int) value));
            case 7 -> (iteratorSource ? DoubleStream.of(DoubleIteratorEx.of(1, 2, 3, 4)) : DoubleStream.of(1, 2, 3, 4))
                    .dropWhile(value -> predicate.test((int) value));
            default -> throw new AssertionError(type);
        };
    }

    @Test
    public void testRejectedParallelSubmissionStopsNestedIteration() throws Exception {
        assertParallelFailureStopsNestedIteration(true);
    }

    @Test
    public void testParallelWorkerFailureStopsNestedIteration() throws Exception {
        assertParallelFailureStopsNestedIteration(false);
    }

    private void assertParallelFailureStopsNestedIteration(boolean rejectSubmission) throws Exception {
        for (int sourceKind = 0; sourceKind < 3; sourceKind++) {
            for (int nestingKind = 0; nestingKind < 4; nestingKind++) {
                AtomicBoolean stop = new AtomicBoolean();
                AtomicInteger closeCount = new AtomicInteger();
                AtomicInteger submissions = new AtomicInteger();
                CountDownLatch nestedIterationStarted = new CountDownLatch(1);
                RuntimeException failure = rejectSubmission ? new RejectedExecutionException("rejected after nested iteration started")
                        : new IllegalStateException("peer callback failed");
                ExecutorService worker = Executors.newFixedThreadPool(2);
                ExecutorService caller = Executors.newSingleThreadExecutor();
                Iterable<Integer> unbounded = () -> new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        nestedIterationStarted.countDown();
                        return !stop.get();
                    }

                    @Override
                    public Integer next() {
                        return 1;
                    }
                };
                java.util.concurrent.Executor executor = command -> {
                    if (submissions.getAndIncrement() == 0 || !rejectSubmission) {
                        worker.execute(command);
                    } else {
                        try {
                            Assertions.assertTrue(nestedIterationStarted.await(5, TimeUnit.SECONDS));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new AssertionError(e);
                        }
                        throw failure;
                    }
                };
                Stream<Integer> source = sourceKind == 2 ? Stream.of(Arrays.asList(1, 2).iterator()) : Stream.of(1, 2);
                Stream<Integer> stream = source.onClose(closeCount::incrementAndGet)
                        .parallel(ParallelSettings.builder()
                                .maxThreadNum(2)
                                .splitStrategy(sourceKind == 0 ? SplitStrategy.ARRAY : SplitStrategy.ITERATOR)
                                .executor(executor)
                                .build());
                final int nesting = nestingKind;
                Throwables.Function<Integer, Iterable<Integer>, RuntimeException> mapper = value -> {
                    if (!rejectSubmission && value == 2) {
                        try {
                            Assertions.assertTrue(nestedIterationStarted.await(5, TimeUnit.SECONDS));
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new AssertionError(e);
                        }
                        throw failure;
                    }
                    return nesting == 2 ? Collections.singletonList(value) : unbounded;
                };

                try {
                    java.util.concurrent.Future<Throwable> result = caller.submit(() -> Assertions.assertThrows(RuntimeException.class, () -> {
                        if (nesting == 0) {
                            stream.forEach(mapper, (value, nested) -> {
                            });
                        } else if (nesting == 1) {
                            stream.forEach(mapper, value -> Collections.singletonList(value), (value, nested, inner) -> {
                            });
                        } else if (nesting == 2) {
                            stream.forEach(mapper, value -> unbounded, (value, nested, inner) -> {
                            });
                        } else {
                            stream.flatGroupTo(value -> {
                                mapper.apply(value);
                                return new java.util.AbstractCollection<Integer>() {
                                    @Override
                                    public Iterator<Integer> iterator() {
                                        return unbounded.iterator();
                                    }

                                    @Override
                                    public int size() {
                                        return Integer.MAX_VALUE;
                                    }
                                };
                            }, (key, value) -> value, Collectors.counting(), HashMap::new);
                        }
                    }));

                    Assertions.assertSame(failure, result.get(5, TimeUnit.SECONDS));
                    Assertions.assertEquals(1, closeCount.get());
                    Assertions.assertFalse(worker.isShutdown(), "The supplied executor remains caller-owned");
                    stream.close();
                    Assertions.assertEquals(1, closeCount.get());
                } finally {
                    // Bound the regression even if a worker fails to observe the terminal's shared error.
                    stop.set(true);
                    caller.shutdown();
                    worker.shutdown();
                    Assertions.assertTrue(caller.awaitTermination(5, TimeUnit.SECONDS));
                    Assertions.assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
                    stream.close();
                }
            }
        }
    }

    @Test
    public void testRejectedParallelSubmissionPreservesFailureWhenCloseFails() {
        for (boolean iteratorBacked : new boolean[] { false, true }) {
            for (int terminal = 0; terminal < 4; terminal++) {
                AtomicInteger closeCount = new AtomicInteger();
                RejectedExecutionException rejection = new RejectedExecutionException("rejected submission");
                IllegalStateException closeFailure = new IllegalStateException("close failed");
                Stream<Integer> stream = (iteratorBacked ? Stream.of(Arrays.asList(1, 2).iterator()) : Stream.of(1, 2)).onClose(() -> {
                    closeCount.incrementAndGet();
                    throw closeFailure;
                }).parallel(2, command -> {
                    throw rejection;
                });
                final int operation = terminal;

                Assertions.assertSame(rejection, Assertions.assertThrows(RejectedExecutionException.class, () -> {
                    switch (operation) {
                        case 0 -> stream.forEach(value -> {
                        });
                        case 1 -> stream.collect(ArrayList<Integer>::new, List::add, List::addAll);
                        case 2 -> stream.reduce(0, Integer::sum, Integer::sum);
                        case 3 -> stream.forEach(value -> {
                        }, () -> Assertions.fail("onComplete must not run after rejection"));
                        default -> throw new AssertionError(operation);
                    }
                }));
                Assertions.assertArrayEquals(new Throwable[] { closeFailure }, rejection.getSuppressed());
                Assertions.assertEquals(1, closeCount.get());
                stream.close();
                Assertions.assertEquals(1, closeCount.get());
                Assertions.assertThrows(IllegalStateException.class, stream::count);
            }
        }
    }

    @Test
    public void testParallelTerminalsCloseWhenExecutorRejectsSubmission() {
        for (boolean iteratorBacked : new boolean[] { false, true }) {
            for (int type = 0; type < 8; type++) {
                AtomicInteger closeCount = new AtomicInteger();
                RejectedExecutionException rejection = new RejectedExecutionException("rejected submission");
                java.util.concurrent.Executor executor = command -> {
                    throw rejection;
                };
                final int streamType = type;

                Assertions.assertSame(rejection, Assertions.assertThrows(RejectedExecutionException.class, () -> {
                    switch (streamType) {
                        case 0 -> (iteratorBacked ? Stream.of(Arrays.asList(1, 2, 3).iterator()) : Stream.of(1, 2, 3)).onClose(closeCount::incrementAndGet)
                                .parallel(2, executor)
                                .forEach(value -> {
                                });
                        case 1 -> (iteratorBacked ? ByteStream.of(com.landawn.abacus.util.ByteIterator.of((byte) 1, (byte) 2, (byte) 3))
                                : ByteStream.of((byte) 1, (byte) 2, (byte) 3)).onClose(closeCount::incrementAndGet).parallel(2, executor).forEach(value -> {
                                });
                        case 2 -> (iteratorBacked ? CharStream.of(com.landawn.abacus.util.CharIterator.of('a', 'b', 'c')) : CharStream.of('a', 'b', 'c'))
                                .onClose(closeCount::incrementAndGet)
                                .parallel(2, executor)
                                .forEach(value -> {
                                });
                        case 3 -> (iteratorBacked ? ShortStream.of(com.landawn.abacus.util.ShortIterator.of((short) 1, (short) 2, (short) 3))
                                : ShortStream.of((short) 1, (short) 2, (short) 3)).onClose(closeCount::incrementAndGet).parallel(2, executor).forEach(value -> {
                                });
                        case 4 -> (iteratorBacked ? IntStream.of(com.landawn.abacus.util.IntIterator.of(1, 2, 3)) : IntStream.of(1, 2, 3))
                                .onClose(closeCount::incrementAndGet)
                                .parallel(2, executor)
                                .forEach(value -> {
                                });
                        case 5 -> (iteratorBacked ? LongStream.of(com.landawn.abacus.util.LongIterator.of(1, 2, 3)) : LongStream.of(1, 2, 3))
                                .onClose(closeCount::incrementAndGet)
                                .parallel(2, executor)
                                .forEach(value -> {
                                });
                        case 6 -> (iteratorBacked ? FloatStream.of(com.landawn.abacus.util.FloatIterator.of(1, 2, 3)) : FloatStream.of(1, 2, 3))
                                .onClose(closeCount::incrementAndGet)
                                .parallel(2, executor)
                                .forEach(value -> {
                                });
                        case 7 -> (iteratorBacked ? DoubleStream.of(com.landawn.abacus.util.DoubleIterator.of(1, 2, 3)) : DoubleStream.of(1, 2, 3))
                                .onClose(closeCount::incrementAndGet)
                                .parallel(2, executor)
                                .forEach(value -> {
                                });
                        default -> throw new AssertionError(streamType);
                    }
                }));
                Assertions.assertEquals(1, closeCount.get(), "type=" + type + ", iteratorBacked=" + iteratorBacked);
            }

            for (boolean collecting : new boolean[] { false, true }) {
                AtomicInteger closeCount = new AtomicInteger();
                RejectedExecutionException rejection = new RejectedExecutionException("rejected result submission");
                Stream<Integer> stream = (iteratorBacked ? Stream.of(Arrays.asList(1, 2, 3).iterator()) : Stream.of(1, 2, 3))
                        .onClose(closeCount::incrementAndGet)
                        .parallel(2, command -> {
                            throw rejection;
                        });

                Assertions.assertSame(rejection, Assertions.assertThrows(RejectedExecutionException.class, () -> {
                    if (collecting) {
                        stream.collect(ArrayList<Integer>::new, List::add, List::addAll);
                    } else {
                        stream.reduce(0, Integer::sum);
                    }
                }));
                Assertions.assertEquals(1, closeCount.get());
                Assertions.assertThrows(IllegalStateException.class, stream::count);
            }
        }
    }

    @Test
    public void testRejectedParallelSubmissionWaitsForAcceptedWorkerBeforeClose() throws Exception {
        ExecutorService worker = Executors.newSingleThreadExecutor();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean keepProducing = new AtomicBoolean(true);
        AtomicBoolean callbackFinished = new AtomicBoolean();
        AtomicInteger closeCount = new AtomicInteger();
        AtomicInteger submissions = new AtomicInteger();
        RejectedExecutionException rejection = new RejectedExecutionException("second submission rejected");

        try {
            Stream<Integer> stream = Stream.generate(() -> 1).takeWhile(value -> keepProducing.get()).onClose(() -> {
                Assertions.assertTrue(callbackFinished.get());
                closeCount.incrementAndGet();
            }).parallel(2, command -> {
                if (submissions.getAndIncrement() == 0) {
                    worker.execute(command);
                    try {
                        Assertions.assertTrue(entered.await(5, TimeUnit.SECONDS));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                } else {
                    release.countDown();
                    throw rejection;
                }
            });

            Assertions.assertSame(rejection, Assertions.assertThrows(RejectedExecutionException.class, () -> stream.forEach(value -> {
                entered.countDown();
                Assertions.assertTrue(release.await(5, TimeUnit.SECONDS));
                Assertions.assertEquals(0, closeCount.get());
                callbackFinished.set(true);
            })));
            Assertions.assertEquals(1, closeCount.get());
            Assertions.assertFalse(worker.isShutdown(), "the supplied executor is borrowed");
        } finally {
            keepProducing.set(false);
            release.countDown();
            worker.shutdownNow();
            Assertions.assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void testParallelWorkerSetupFailureStopsOtherWorkers() throws Exception {
        for (int operation = 0; operation < 3; operation++) {
            ExecutorService workers = Executors.newFixedThreadPool(2);
            ExecutorService caller = Executors.newSingleThreadExecutor();
            AtomicBoolean keepProducing = new AtomicBoolean(true);
            AtomicInteger setupCalls = new AtomicInteger();
            AtomicInteger closeCount = new AtomicInteger();
            CountDownLatch secondSupplierReady = new CountDownLatch(1);
            IllegalStateException failure = new IllegalStateException("worker setup failed");
            final int terminalOperation = operation;

            try {
                java.util.concurrent.Future<?> result = caller.submit(() -> {
                    com.landawn.abacus.util.function.Supplier<AtomicInteger> supplier = () -> {
                        if (setupCalls.getAndIncrement() == 0) {
                            try {
                                Assertions.assertTrue(secondSupplierReady.await(5, TimeUnit.SECONDS));
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                throw new AssertionError(e);
                            }
                            throw failure;
                        }
                        secondSupplierReady.countDown();
                        return new AtomicInteger();
                    };

                    if (terminalOperation == 0) {
                        Stream.generate(() -> 1)
                                .takeWhile(value -> keepProducing.get())
                                .onClose(closeCount::incrementAndGet)
                                .parallel(2, workers)
                                .collect(supplier, (count, value) -> count.incrementAndGet(), (left, right) -> left.addAndGet(right.get()));
                    } else if (terminalOperation == 1) {
                        IntStream.generate(() -> 1)
                                .takeWhile(value -> keepProducing.get())
                                .onClose(closeCount::incrementAndGet)
                                .parallel(2, workers)
                                .collect(supplier, (count, value) -> count.incrementAndGet(), (left, right) -> left.addAndGet(right.get()));
                    } else {
                        IntStream.of(new com.landawn.abacus.util.IntIterator() {
                            @Override
                            public boolean hasNext() {
                                if (setupCalls.getAndIncrement() == 0) {
                                    throw failure;
                                }
                                return keepProducing.get();
                            }

                            @Override
                            public int nextInt() {
                                return 1;
                            }
                        }).onClose(closeCount::incrementAndGet).parallel(2, workers).reduce(Integer::sum);
                    }
                });

                java.util.concurrent.ExecutionException thrown = Assertions.assertThrows(java.util.concurrent.ExecutionException.class,
                        () -> result.get(5, TimeUnit.SECONDS));
                Assertions.assertSame(failure, thrown.getCause());
                Assertions.assertEquals(1, closeCount.get());
                Assertions.assertFalse(workers.isShutdown(), "the supplied executor is borrowed");
            } finally {
                keepProducing.set(false);
                secondSupplierReady.countDown();
                workers.shutdownNow();
                caller.shutdownNow();
                Assertions.assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS));
                Assertions.assertTrue(caller.awaitTermination(5, TimeUnit.SECONDS));
            }
        }
    }

    @Test
    public void testConcurrentCollectorSupplierFailureClosesSource() {
        for (boolean iteratorBacked : new boolean[] { false, true }) {
            AtomicInteger closeCount = new AtomicInteger();
            AtomicInteger supplierCalls = new AtomicInteger();
            AtomicInteger submissions = new AtomicInteger();
            IllegalStateException failure = new IllegalStateException("concurrent collector setup failed");
            Stream<Integer> stream = (iteratorBacked ? Stream.of(Arrays.asList(1, 2, 3).iterator()) : Stream.of(1, 2, 3)).onClose(closeCount::incrementAndGet)
                    .parallel(2, command -> {
                        submissions.incrementAndGet();
                        command.run();
                    });
            java.util.stream.Collector<Integer, AtomicInteger, AtomicInteger> collector = java.util.stream.Collector.of(() -> {
                supplierCalls.incrementAndGet();
                throw failure;
            }, (count, value) -> count.addAndGet(value), (left, right) -> left, java.util.stream.Collector.Characteristics.CONCURRENT,
                    java.util.stream.Collector.Characteristics.UNORDERED);

            Assertions.assertSame(failure, Assertions.assertThrows(IllegalStateException.class, () -> stream.collect(collector)));
            Assertions.assertEquals(1, supplierCalls.get());
            Assertions.assertEquals(0, submissions.get());
            Assertions.assertEquals(1, closeCount.get());
            Assertions.assertThrows(IllegalStateException.class, stream::count);
        }
    }

    @SafeVarargs
    private final <T> Stream<T> createStream(T... elements) {
        return Stream.of(elements);
    }

    private <T> Stream<T> createStream(Collection<T> elements) {
        return Stream.of(elements);
    }

    @Test
    public void testStreamTypesAreNotAdvertisedAsImmutable() {
        Assertions.assertFalse(com.landawn.abacus.util.Immutable.class.isAssignableFrom(BaseStream.class));
        Assertions.assertFalse(com.landawn.abacus.util.Immutable.class.isAssignableFrom(Stream.class));
        Assertions.assertFalse(com.landawn.abacus.util.Immutable.class.isAssignableFrom(EntryStream.class));
        Assertions.assertFalse(com.landawn.abacus.util.Immutable.class.isAssignableFrom(IntStream.class));
        Assertions.assertFalse(com.landawn.abacus.util.Immutable.class.isAssignableFrom(LongStream.class));
        Assertions.assertFalse(com.landawn.abacus.util.Immutable.class.isAssignableFrom(FloatStream.class));
        Assertions.assertFalse(com.landawn.abacus.util.Immutable.class.isAssignableFrom(ShortStream.class));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testClosedStateCheckPrecedesValidationAndDelegation() {
        Stream<Integer> stream = Stream.of(1);
        stream.close();

        Assertions.assertThrows(IllegalStateException.class, () -> stream.rateLimited(0));
        Assertions.assertThrows(IllegalStateException.class, () -> stream.delay((java.time.Duration) null));
        Assertions.assertThrows(IllegalStateException.class, () -> stream.parallel(0));
        Assertions.assertThrows(IllegalStateException.class, () -> stream.mapIfNotNull(null));

        ByteStream byteStream = ByteStream.of((byte) 1);
        byteStream.close();
        Assertions.assertThrows(IllegalStateException.class, () -> byteStream.rateLimited((com.landawn.abacus.util.RateLimiter) null));

        IntStream intStream = IntStream.of(1);
        intStream.close();
        Assertions.assertThrows(IllegalStateException.class, () -> intStream.flattmap(null));

        EntryStream<String, Integer> entryStream = EntryStream.of("one", 1);
        entryStream.close();
        Assertions.assertThrows(IllegalStateException.class, () -> entryStream.filterByKey(null));
        Assertions.assertThrows(IllegalStateException.class, () -> entryStream.rateLimited(0));
        Assertions.assertThrows(IllegalStateException.class, () -> entryStream.delay((java.time.Duration) null));

        Stream<Integer> iteratorStream = Stream.of(Arrays.asList(1).iterator());
        iteratorStream.close();
        Assertions.assertThrows(IllegalStateException.class, () -> iteratorStream.foldRight(null));
    }

    // Covers StreamBase.toArray(Collection) via splitAt on iterator-backed stream
    @Test
    public void testToArray_ViaIteratorStreamSplitAt() {
        // Use an Iterator-backed stream so that AbstractStream.splitAt is used (not ArrayStream.splitAt)
        // which calls Stream.toArray(list) -> StreamBase.toArray(Collection)
        Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5).iterator();
        List<Stream<Integer>> parts = Stream.of(iter).splitAt(3).toList();
        Assertions.assertEquals(2, parts.size());
        List<Integer> first = parts.get(0).toList();
        List<Integer> second = parts.get(1).toList();
        Assertions.assertEquals(Arrays.asList(1, 2, 3), first);
        Assertions.assertEquals(Arrays.asList(4, 5), second);
    }

    @Test
    public void testShuffled() {
        Stream<Integer> stream = createStream(1, 2, 3, 4, 5);
        List<Integer> shuffled = stream.shuffled().toList();

        Assertions.assertEquals(5, shuffled.size());
        Assertions.assertTrue(shuffled.containsAll(Arrays.asList(1, 2, 3, 4, 5)));
    }

    @Test
    public void testSkipAndLimit() {
        Stream<Integer> stream1 = createStream(1, 2, 3, 4, 5);
        List<Integer> result1 = stream1.skipAndLimit(2, 2).toList();
        Assertions.assertEquals(Arrays.asList(3, 4), result1);

        Stream<Integer> stream2 = createStream(1, 2, 3, 4, 5);
        List<Integer> result2 = stream2.skipAndLimit(0, 3).toList();
        Assertions.assertEquals(Arrays.asList(1, 2, 3), result2);

        Stream<Integer> stream3 = createStream(1, 2, 3, 4, 5);
        List<Integer> result3 = stream3.skipAndLimit(0, Long.MAX_VALUE).toList();
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5), result3);

        Stream<Integer> stream4 = createStream(1, 2, 3, 4, 5);
        List<Integer> result4 = stream4.skipAndLimit(3, Long.MAX_VALUE).toList();
        Assertions.assertEquals(Arrays.asList(4, 5), result4);

        Stream<Integer> stream5 = createStream(1, 2, 3);
        Assertions.assertThrows(IllegalArgumentException.class, () -> stream5.skipAndLimit(-1, 2));
    }

    @Test
    public void testElementAt() {
        Stream<String> stream1 = createStream("a", "b", "c", "d", "e");
        Assertions.assertEquals("a", stream1.elementAt(0).get());

        Stream<String> stream2 = createStream("a", "b", "c", "d", "e");
        Assertions.assertEquals("c", stream2.elementAt(2).get());

        Stream<String> stream3 = createStream("a", "b", "c", "d", "e");
        Assertions.assertEquals("e", stream3.elementAt(4).get());

        Stream<String> stream4 = createStream("a", "b", "c");
        Assertions.assertThrows(IllegalArgumentException.class, () -> stream4.elementAt(-1));

        Stream<String> stream5 = createStream("a", "b", "c");
        Assertions.assertThrows(NoSuchElementException.class, () -> stream5.elementAt(5).get());

        Stream<String> stream6 = createStream();
        Assertions.assertThrows(NoSuchElementException.class, () -> stream6.elementAt(0).get());
    }

    @Test
    public void testToImmutableList() {
        Stream<Integer> stream = createStream(1, 2, 3, 4, 5);
        ImmutableList<Integer> immutableList = stream.toImmutableList();

        Assertions.assertEquals(5, immutableList.size());
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5), new ArrayList<>(immutableList));

        Stream<Integer> emptyStream = createStream();
        ImmutableList<Integer> emptyList = emptyStream.toImmutableList();
        Assertions.assertTrue(emptyList.isEmpty());
    }

    @Test
    public void testToImmutableSet() {
        Stream<Integer> stream = createStream(1, 2, 3, 3, 4, 5);
        ImmutableSet<Integer> immutableSet = stream.toImmutableSet();

        Assertions.assertEquals(5, immutableSet.size());
        Assertions.assertTrue(immutableSet.containsAll(Arrays.asList(1, 2, 3, 4, 5)));

        Stream<Integer> emptyStream = createStream();
        ImmutableSet<Integer> emptySet = emptyStream.toImmutableSet();
        Assertions.assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testToArray_HelperCopiesOnlyElements() {
        ArrayList<String> list = new ArrayList<>(8);
        list.add("alpha");
        list.add("beta");

        Object[] array = StreamBase.toArray(list);
        Assertions.assertArrayEquals(new Object[] { "alpha", "beta" }, array);
        Assertions.assertEquals(2, array.length);
    }

    @Test
    public void testToArray_NonArrayList() {
        // Test with LinkedList (non-ArrayList) to hit the c.toArray() path
        java.util.LinkedList<String> linked = new java.util.LinkedList<>(Arrays.asList("x", "y", "z"));
        Object[] result = StreamBase.toArray(linked);
        Assertions.assertArrayEquals(new Object[] { "x", "y", "z" }, result);
    }

    @Test
    public void testToArray() {
        Stream<String> stream = createStream("a", "b", "c");
        Object[] array = stream.toArray();

        Assertions.assertArrayEquals(new String[] { "a", "b", "c" }, array);

        Stream<String> emptyStream = createStream();
        Object[] emptyArray = emptyStream.toArray();
        Assertions.assertEquals(0, emptyArray.length);
    }

    @Test
    public void testToArray_Collection() {
        // toArray(Collection) - uses ArrayList reflection path or fallback; actual runtime type is Object[]
        List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
        Object[] result = StreamBase.toArray(list);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(5, result.length);
    }

    @Test
    public void testToArray_LinkedList() {
        // toArray(Collection) - non-ArrayList path; actual runtime type is Object[]
        List<String> list = new java.util.LinkedList<>(Arrays.asList("a", "b", "c"));
        Object[] result = StreamBase.toArray(list);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(3, result.length);
    }

    @Test
    public void testLocalArrayDeque() {
        LocalArrayDeque<String> deque1 = new LocalArrayDeque<>();
        Assertions.assertTrue(deque1.isEmpty());

        LocalArrayDeque<String> deque2 = new LocalArrayDeque<>(10);
        Assertions.assertTrue(deque2.isEmpty());

        List<String> list = Arrays.asList("a", "b", "c");
        LocalArrayDeque<String> deque3 = new LocalArrayDeque<>(list);
        Assertions.assertEquals(3, deque3.size());
        Assertions.assertEquals("a", deque3.getFirst());
        Assertions.assertEquals("c", deque3.getLast());
    }

    @Test
    public void testThrowIfEmpty() {
        Stream<Integer> stream1 = createStream(1, 2, 3);
        Assertions.assertDoesNotThrow(() -> stream1.throwIfEmpty());

        Stream<Integer> stream2 = createStream();
        Assertions.assertThrows(NoSuchElementException.class, () -> stream2.throwIfEmpty().count());
    }

    @Test
    public void testThrowIfEmptyWithSupplier() {
        Stream<Integer> stream1 = createStream(1, 2, 3);
        Assertions.assertDoesNotThrow(() -> stream1.throwIfEmpty(() -> new IllegalStateException("Empty!")));

        Stream<Integer> stream2 = createStream();
        Assertions.assertThrows(IllegalStateException.class, () -> stream2.throwIfEmpty(() -> new IllegalStateException("Custom empty message")).count());

        Stream<Integer> stream3 = createStream();
        Assertions.assertThrows(IllegalArgumentException.class, () -> stream3.throwIfEmpty(null).count());
    }

    @Test
    public void testApplyIfNotEmpty() {
        Stream<Integer> stream1 = createStream(1, 2, 3);
        com.landawn.abacus.util.u.Optional<List<Integer>> result1 = stream1.applyIfNotEmpty(s -> s.toList());
        Assertions.assertTrue(result1.isPresent());
        Assertions.assertEquals(Arrays.asList(1, 2, 3), result1.get());

        Stream<Integer> stream2 = createStream();
        com.landawn.abacus.util.u.Optional<List<Integer>> result2 = stream2.applyIfNotEmpty(s -> s.toList());
        Assertions.assertFalse(result2.isPresent());

        // Verify stream is closed after applyIfNotEmpty
        Stream<Integer> stream3 = createStream(1, 2, 3);
        stream3.applyIfNotEmpty(s -> s.toList());
        Assertions.assertThrows(IllegalStateException.class, () -> stream3.toList());
    }

    @Test
    public void testApplyIfNotEmptyRejectsNullFunction() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> this.<Integer> createStream().applyIfNotEmpty((Throwables.Function<Stream<Integer>, List<Integer>, RuntimeException>) null));
        final AtomicBoolean closed = new AtomicBoolean(false);
        Assertions.assertThrows(IllegalArgumentException.class, () -> createStream(1).onClose(() -> closed.set(true))
                .applyIfNotEmpty((Throwables.Function<Stream<Integer>, List<Integer>, RuntimeException>) null));
        Assertions.assertTrue(closed.get());
    }

    @Test
    public void testAcceptIfNotEmpty() {
        List<Integer> collected = new ArrayList<>();
        Stream<Integer> stream1 = createStream(1, 2, 3);
        OrElse result1 = stream1.acceptIfNotEmpty(s -> collected.addAll(s.toList()));
        Assertions.assertEquals(Arrays.asList(1, 2, 3), collected);

        Stream<Integer> stream2 = createStream();
        AtomicBoolean called = new AtomicBoolean(false);
        stream2.acceptIfNotEmpty(s -> called.set(true));
        Assertions.assertFalse(called.get());

        // Verify stream is closed after acceptIfNotEmpty
        Stream<Integer> stream3 = createStream(1, 2, 3);
        stream3.acceptIfNotEmpty(s -> s.toList());
        Assertions.assertThrows(IllegalStateException.class, () -> stream3.toList());
    }

    @Test
    public void testAcceptIfNotEmptyRejectsNullAction() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> this.<Integer> createStream().acceptIfNotEmpty((Throwables.Consumer<Stream<Integer>, RuntimeException>) null));
        final AtomicBoolean closed = new AtomicBoolean(false);
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> createStream(1).onClose(() -> closed.set(true)).acceptIfNotEmpty((Throwables.Consumer<Stream<Integer>, RuntimeException>) null));
        Assertions.assertTrue(closed.get());
    }

    @Test
    public void testPrintln() {
        Stream<Integer> stream = createStream(1, 2, 3);
        Assertions.assertDoesNotThrow(() -> stream.println());

        Stream<Integer> emptyStream = createStream();
        Assertions.assertDoesNotThrow(() -> emptyStream.println());
    }

    @Test
    public void testIsParallel() {
        Stream<Integer> stream = createStream(1, 2, 3);
        Assertions.assertFalse(stream.isParallel());
    }

    @Test
    public void testSequential() {
        Stream<Integer> stream = createStream(1, 2, 3);
        Stream<Integer> sequential = stream.sequential();

        Assertions.assertSame(stream, sequential);
        Assertions.assertFalse(sequential.isParallel());
    }

    @Test
    public void testParallel() {
        Stream<Integer> stream = createStream(1, 2, 3);
        Stream<Integer> parallel = stream.parallel();

        Assertions.assertTrue(parallel.isParallel());
    }

    @Test
    public void testParallelWithExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Stream<Integer> stream = createStream(1, 2, 3);
            Stream<Integer> parallel = stream.parallel(executor);

            Assertions.assertTrue(parallel.isParallel());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testCollectingCombiner_WithCollection() {
        // Forces the collectingCombiner lambda for Collection path by using parallel collect
        List<Integer> result = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).parallel().collect(com.landawn.abacus.util.stream.Collectors.toList());
        Assertions.assertNotNull(result);
        Assertions.assertEquals(10, result.size());
    }

    @Test
    public void testCollectingCombiner_WithMap() {
        // Forces the collectingCombiner lambda for Map path by using parallel collect
        Map<Integer, Integer> result = Stream.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .parallel()
                .collect(com.landawn.abacus.util.stream.Collectors.toMap(i -> i, i -> i * 2, (a, b) -> a));
        Assertions.assertNotNull(result);
        Assertions.assertEquals(10, result.size());
    }

    @Test
    public void testParallelWithMaxThreadNum() {
        Stream<Integer> stream = createStream(1, 2, 3);

        Stream<Integer> parallel1 = stream.parallel(4);
        Assertions.assertTrue(parallel1.isParallel());

        Stream<Integer> stream2 = createStream(1, 2, 3);
        Stream<Integer> parallel2 = stream2.parallel(0);
        Assertions.assertTrue(parallel2.isParallel());

        Stream<Integer> stream3 = createStream(1, 2, 3);
        Assertions.assertThrows(IllegalArgumentException.class, () -> stream3.parallel(-1));
    }

    @Test
    public void testParallelWithMaxThreadNumAndExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Stream<Integer> stream = createStream(1, 2, 3);
            Stream<Integer> parallel = stream.parallel(4, executor);

            Assertions.assertTrue(parallel.isParallel());

            Stream<Integer> stream2 = createStream(1, 2, 3);
            Assertions.assertThrows(IllegalArgumentException.class, () -> stream2.parallel(-1, executor));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testParallelWithParallelSettings() {
        ParallelSettings ps = ParallelSettings.builder().splitStrategy(SplitStrategy.ARRAY).build();

        Stream<Integer> stream = createStream(1, 2, 3);
        Stream<Integer> parallel = stream.parallel(ps);

        Assertions.assertTrue(parallel.isParallel());

        Stream<Integer> stream2 = createStream(1, 2, 3);
        Assertions.assertThrows(IllegalArgumentException.class, () -> stream2.parallel((ParallelSettings) null));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            ParallelSettings customPs = ParallelSettings.builder().maxThreadNum(4).executor(executor).splitStrategy(SplitStrategy.ARRAY).build();
            Stream<Integer> stream3 = createStream(1, 2, 3);
            Stream<Integer> parallel3 = stream3.parallel(customPs);
            Assertions.assertTrue(parallel3.isParallel());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testCheckMaxThreadNum() {
        int result1 = StreamBase.checkMaxThreadNum(0, null);
        Assertions.assertEquals(StreamBase.DEFAULT_MAX_THREAD_NUM, result1);

        int result2 = StreamBase.checkMaxThreadNum(4, null);
        Assertions.assertEquals(4, result2);

        int result3 = StreamBase.checkMaxThreadNum(1000, StreamBase.DEFAULT_ASYNC_EXECUTOR);
        Assertions.assertEquals(StreamBase.MAX_THREAD_NUM_PER_OPERATION, result3);

        AsyncExecutor customExecutor = new AsyncExecutor(Executors.newFixedThreadPool(2));
        int result4 = StreamBase.checkMaxThreadNum(1000, customExecutor);
        Assertions.assertEquals(1000, result4);
        customExecutor.shutdown();
    }

    @Test
    public void testSps_WithExecutor() {
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            List<Integer> result = Stream.of(1, 2, 3, 4, 5).sps(2, executor, s -> s.filter(i -> i % 2 == 0)).sorted().toList();
            Assertions.assertEquals(Arrays.asList(2, 4), result);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testSpsRejectsNegativeMaxThreadNum() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> createStream(1, 2, 3).sps(-1, s -> s).toList());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Assertions.assertThrows(IllegalArgumentException.class, () -> createStream(1, 2, 3).sps(-1, executor, s -> s).toList());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testSpsAndPspRejectNullOperationsBeforeChangingStreamMode() {
        final java.util.function.Function<Stream<Integer>, Stream<Integer>> nullOps = null;

        Assertions.assertThrows(IllegalArgumentException.class, () -> createStream(1, 2, 3).sps(nullOps));
        Assertions.assertThrows(IllegalArgumentException.class, () -> createStream(1, 2, 3).sps(2, nullOps));

        final ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Assertions.assertThrows(IllegalArgumentException.class, () -> createStream(1, 2, 3).sps(2, executor, nullOps));
        } finally {
            executor.shutdown();
        }

        Assertions.assertThrows(IllegalArgumentException.class, () -> createStream(1, 2, 3).parallel().psp(nullOps));
    }

    @Test
    public void testSpsWithMaxThreadNum() {
        Stream<Integer> stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.sps(4, s -> s.map(x -> x * 2));

        Assertions.assertFalse(result.isParallel());
        assertHaveSameElements(Arrays.asList(2, 4, 6), result.toList());
    }

    @Test
    public void testSpsWithMaxThreadNumAndExecutor() {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Stream<Integer> stream = createStream(1, 2, 3);
            Stream<Integer> result = stream.sps(4, executor, s -> s.map(x -> x * 2));

            Assertions.assertFalse(result.isParallel());
            assertHaveSameElements(Arrays.asList(2, 4, 6), result.toList());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testSps() {
        Stream<Integer> stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.sps(s -> s.map(x -> x * 2));

        Assertions.assertFalse(result.isParallel());
        assertHaveSameElements(Arrays.asList(2, 4, 6), result.toList());

        Stream<Integer> closedStream = createStream(1, 2, 3);
        closedStream.close();
        Assertions.assertThrows(IllegalStateException.class, () -> closedStream.sps(s -> s));
    }

    @Test
    public void testPsp() {
        Stream<Integer> stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.psp(s -> s.map(x -> x * 2));

        Assertions.assertTrue(result.isParallel());
        List<Integer> resultList = result.toList();
        Collections.sort(resultList);
        Assertions.assertEquals(Arrays.asList(2, 4, 6), resultList);

        Stream<Integer> closedStream = createStream(1, 2, 3);
        closedStream.close();
        Assertions.assertThrows(IllegalStateException.class, () -> closedStream.psp(s -> s));
    }

    @Test
    public void testTransform() {
        Stream<Integer> stream = createStream(1, 2, 3);
        List<Integer> result = stream.transform(s -> s.map(x -> x * 2)).toList();

        Assertions.assertEquals(Arrays.asList(2, 4, 6), result);

        Stream<Integer> stream2 = createStream(1, 2, 3);
        Assertions.assertThrows(IllegalArgumentException.class, () -> stream2.transform(null));

        Stream<Integer> closedStream = createStream(1, 2, 3);
        closedStream.close();
        Assertions.assertThrows(IllegalStateException.class, () -> closedStream.transform(s -> s));

        for (boolean parallelResult : new boolean[] { false, true }) {
            AtomicInteger inputClosed = new AtomicInteger();
            AtomicInteger outputClosed = new AtomicInteger();
            Stream<Integer> input = createStream(1, 2).parallel(2).onClose(inputClosed::incrementAndGet);
            try (Stream<Integer> transformed = input.transform(source -> {
                Assertions.assertTrue(source.isParallel());
                Stream<Integer> output = Stream.of(3, 4).onClose(outputClosed::incrementAndGet);
                return parallelResult ? output.parallel(2) : output;
            })) {
                Assertions.assertEquals(parallelResult, transformed.isParallel());
                Assertions.assertEquals(List.of(3, 4), transformed.toList());
            }
            Assertions.assertEquals(1, inputClosed.get());
            Assertions.assertEquals(1, outputClosed.get());
        }
    }

    @Test
    public void testOnClose() {
        AtomicBoolean handlerCalled = new AtomicBoolean(false);
        createStream(1, 2, 3).onClose(() -> handlerCalled.set(true)).close();
        Assertions.assertTrue(handlerCalled.get());
    }

    @Test
    public void testMultipleOnClose() {
        {
            List<Integer> callOrder = new ArrayList<>();

            Stream<Integer> stream = createStream(1, 2, 3);
            stream = stream.onClose(() -> callOrder.add(1)).onClose(() -> callOrder.add(2)).onClose(() -> callOrder.add(3));

            stream.close();

            Assertions.assertEquals(Arrays.asList(1, 2, 3), callOrder);
        }
        {
            List<Integer> callOrder = new ArrayList<>();
            java.util.stream.Stream<Integer> javaStream = java.util.stream.Stream.of(1, 2, 3);
            javaStream.onClose(() -> callOrder.add(1)).onClose(() -> callOrder.add(2)).onClose(() -> callOrder.add(3));
            javaStream.close();
            Assertions.assertEquals(Arrays.asList(1, 2, 3), callOrder);
        }
    }

    @Test
    public void testMultipleCloseHandlersWithExceptions() {
        List<String> closeOrder = new ArrayList<>();

        Stream<Integer> stream = createStream(1, 2, 3).onClose(() -> {
            closeOrder.add("handler1");
            throw new RuntimeException("Error 1");
        }).onClose(() -> {
            closeOrder.add("handler2");
            throw new RuntimeException("Error 2");
        }).onClose(() -> closeOrder.add("handler3"));

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> stream.close());

        Assertions.assertEquals(3, closeOrder.size());

        Assertions.assertTrue(thrown.getSuppressed().length > 0);
    }

    @Test
    public void testRepeatedCloseFailureIsNotSelfSuppressedAndHandlersAreReleased() {
        RuntimeException failure = new RuntimeException("same failure");
        AtomicInteger laterHandlerCalls = new AtomicInteger();

        Stream<Integer> stream = createStream(1, 2, 3).onClose(() -> {
            throw failure;
        }).onClose(() -> {
            throw failure;
        }).onClose(laterHandlerCalls::incrementAndGet);

        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, stream::close);

        Assertions.assertSame(failure, thrown);
        Assertions.assertEquals(0, thrown.getSuppressed().length);
        Assertions.assertEquals(1, laterHandlerCalls.get());
        Assertions.assertTrue(stream.closeHandlers().isEmpty(), "one-shot close handlers must be released even when closing fails");
    }

    @Test
    public void testCloseWithHandlers() {
        List<String> closeOrder = new ArrayList<>();

        Stream<Integer> stream = createStream(1, 2, 3).onClose(() -> closeOrder.add("handler1")).onClose(() -> closeOrder.add("handler2"));

        stream.close();

        Assertions.assertEquals(2, closeOrder.size());
        Assertions.assertTrue(closeOrder.contains("handler1"));
        Assertions.assertTrue(closeOrder.contains("handler2"));
    }

    @Test
    public void testClosingDerivedStreamReleasesParentCloseHandlers() {
        final AtomicInteger closeCalls = new AtomicInteger();
        final Stream<Integer> parent = createStream(1, 2, 3).onClose(closeCalls::incrementAndGet);
        final IntStream derived = parent.newStream(new int[] { 4, 5, 6 });

        derived.close();

        Assertions.assertEquals(1, closeCalls.get());
        Assertions.assertThrows(IllegalStateException.class, parent::count);
        Assertions.assertTrue(parent.closeHandlers().isEmpty(), "closing a derived stream must release the parent's handler deque and its captured references");

        derived.close();
        parent.close();
        Assertions.assertEquals(1, closeCalls.get(), "the propagated close handler must remain one-shot");
    }

    @Test
    public void testClosingExplicitIteratorDerivedStreamsClosesEveryParentSpecialization() {
        final Stream<Integer> objectParent = createStream(1, 2, 3);
        objectParent.flatMap(Stream::of).close();
        Assertions.assertThrows(IllegalStateException.class, objectParent::count);

        final CharStream charParent = CharStream.of('a', 'b');
        charParent.flatMap(CharStream::of).close();
        Assertions.assertThrows(IllegalStateException.class, charParent::count);

        final ByteStream byteParent = ByteStream.of((byte) 1, (byte) 2);
        byteParent.flatMap(ByteStream::of).close();
        Assertions.assertThrows(IllegalStateException.class, byteParent::count);

        final ShortStream shortParent = ShortStream.of((short) 1, (short) 2);
        shortParent.flatMap(ShortStream::of).close();
        Assertions.assertThrows(IllegalStateException.class, shortParent::count);

        final IntStream intParent = IntStream.of(1, 2);
        intParent.flatMap(IntStream::of).close();
        Assertions.assertThrows(IllegalStateException.class, intParent::count);

        final LongStream longParent = LongStream.of(1L, 2L);
        longParent.flatMap(LongStream::of).close();
        Assertions.assertThrows(IllegalStateException.class, longParent::count);

        final FloatStream floatParent = FloatStream.of(1F, 2F);
        floatParent.flatMap(FloatStream::of).close();
        Assertions.assertThrows(IllegalStateException.class, floatParent::count);

        final DoubleStream doubleParent = DoubleStream.of(1D, 2D);
        doubleParent.flatMap(DoubleStream::of).close();
        Assertions.assertThrows(IllegalStateException.class, doubleParent::count);

        final AtomicInteger closeCount = new AtomicInteger();
        final IntStream parentWithCloseHandler = IntStream.of(1).onClose(closeCount::incrementAndGet);
        parentWithCloseHandler.flatMap(IntStream::of).close();
        Assertions.assertEquals(1, closeCount.get());
    }

    @Test
    public void testClosingExecutionModeViewClosesSourceStream() {
        final Stream<Integer> sequentialSource = createStream(1, 2);
        sequentialSource.parallel(2).close();
        Assertions.assertThrows(IllegalStateException.class, sequentialSource::count);

        final Stream<Integer> parallelSource = createStream(1, 2).parallel(2);
        parallelSource.sequential().close();
        Assertions.assertThrows(IllegalStateException.class, parallelSource::count);

        final Stream<Integer> iteratorSequentialSource = Stream.of(Arrays.asList(1, 2).iterator());
        iteratorSequentialSource.parallel(2).close();
        Assertions.assertThrows(IllegalStateException.class, iteratorSequentialSource::count);

        final Stream<Integer> iteratorParallelSource = Stream.of(Arrays.asList(1, 2).iterator()).parallel(2);
        iteratorParallelSource.sequential().close();
        Assertions.assertThrows(IllegalStateException.class, iteratorParallelSource::count);

        final AtomicInteger primitiveCloseCount = new AtomicInteger();
        final IntStream primitiveArraySource = IntStream.of(1, 2).onClose(primitiveCloseCount::incrementAndGet);
        primitiveArraySource.parallel(2).close();
        Assertions.assertEquals(1, primitiveCloseCount.get());
        Assertions.assertThrows(IllegalStateException.class, primitiveArraySource::count);

        final IntStream primitiveIteratorSource = IntStream.of(com.landawn.abacus.util.IntIterator.of(1, 2)).parallel(2);
        primitiveIteratorSource.sequential().close();
        Assertions.assertThrows(IllegalStateException.class, primitiveIteratorSource::count);
    }

    @Test
    public void testClose() {
        Stream<Integer> stream = createStream(1, 2, 3);
        Assertions.assertDoesNotThrow(() -> stream.close());

        Assertions.assertDoesNotThrow(() -> stream.close());

        Assertions.assertThrows(IllegalStateException.class, () -> stream.map(x -> x * 2));
    }

    @Test
    public void testCloseWithException() {
        Stream<Integer> stream = createStream(1, 2, 3).onClose(() -> {
            throw new RuntimeException("Close error");
        });

        Assertions.assertThrows(RuntimeException.class, () -> stream.close());
    }

    @Test
    public void testCloseIterators() {
        List<IteratorEx<?>> iterators = new ArrayList<>();
        AtomicInteger closeCount = new AtomicInteger(0);

        for (int i = 0; i < 3; i++) {
            iterators.add(new IteratorEx<Integer>() {
                @Override
                public boolean hasNext() {
                    return false;
                }

                @Override
                public Integer next() {
                    throw new NoSuchElementException();
                }

                @Override
                public void closeResource() {
                    closeCount.incrementAndGet();
                }
            });
        }

        StreamBase.closeIterators(iterators);
        Assertions.assertEquals(3, closeCount.get());

        List<IteratorEx<?>> iteratorsWithError = new ArrayList<>();
        iteratorsWithError.add(new IteratorEx<Integer>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public Integer next() {
                throw new NoSuchElementException();
            }

            @Override
            public void closeResource() {
                throw new RuntimeException("Close error");
            }
        });

        Assertions.assertThrows(RuntimeException.class, () -> StreamBase.closeIterators(iteratorsWithError));
    }

    @Test
    public void testAssertNotClosed() {
        Stream<Integer> stream = createStream(1, 2, 3);

        Assertions.assertDoesNotThrow(() -> stream.map(x -> x * 2).toList());

        stream.close();

        Assertions.assertThrows(IllegalStateException.class, () -> stream.map(x -> x * 2));
    }

    @Test
    public void testCheckIndex_Valid() {
        // checkIndex with valid index should not throw
        Stream<Integer> s = Stream.of(1, 2, 3, 4, 5);
        // Access checkIndex indirectly via a stream operation on an array stream
        // Using getE (element at index) on a list
        Assertions.assertDoesNotThrow(() -> {
            List<Integer> result = Stream.of(1, 2, 3, 4, 5).skip(0).limit(3).toList();
            Assertions.assertEquals(3, result.size());
        });
    }

    @Test
    public void testCheckIndex_InvalidClosesStream() {
        final Stream<Integer> stream = Stream.of(1, 2, 3);
        final StreamBase<Integer, ?, ?, ?, ?, ?, ?, ?> base = stream;

        Assertions.assertThrows(RuntimeException.class, () -> base.checkIndex(3, 3));
        Assertions.assertThrows(IllegalStateException.class, () -> stream.count());
    }

    @Test
    public void testIsEmptyCloseHandler() {
        Assertions.assertTrue(StreamBase.isEmptyCloseHandler(null));

        Assertions.assertTrue(StreamBase.isEmptyCloseHandler(StreamBase.EMPTY_CLOSE_HANDLER));

        Runnable handler = () -> System.out.println("close");
        Assertions.assertFalse(StreamBase.isEmptyCloseHandler(handler));
    }

    @Test
    public void testIsEmptyCloseHandlers() {
        Assertions.assertTrue(StreamBase.isEmptyCloseHandlers(null));

        Assertions.assertTrue(StreamBase.isEmptyCloseHandlers(new ArrayList<>()));

        List<Runnable> handlers = new ArrayList<>();
        handlers.add(StreamBase.EMPTY_CLOSE_HANDLER);
        Assertions.assertTrue(StreamBase.isEmptyCloseHandlers(handlers));

        handlers.add(() -> System.out.println("close"));
        Assertions.assertFalse(StreamBase.isEmptyCloseHandlers(handlers));
    }

    @Test
    public void testNewCloseHandler() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> StreamBase.newCloseHandler((Runnable) null));

        LocalRunnable localRunnable = LocalRunnable.wrap(Fn.jr(() -> {
        }));
        LocalRunnable handler2 = StreamBase.newCloseHandler(localRunnable);
        Assertions.assertSame(localRunnable, handler2);

        AtomicBoolean called = new AtomicBoolean(false);
        Runnable runnable = () -> called.set(true);
        LocalRunnable handler3 = StreamBase.newCloseHandler(runnable);
        handler3.run();
        Assertions.assertTrue(called.get());

        AtomicBoolean closed = new AtomicBoolean(false);
        AutoCloseable closeable = () -> closed.set(true);
        LocalRunnable handler4 = StreamBase.newCloseHandler(closeable);
        handler4.run();
        Assertions.assertTrue(closed.get());
    }

    @Test
    public void testNewCloseHandler_Collection_Empty() {
        LocalRunnable handler = StreamBase.newCloseHandler((java.util.Collection<Stream<Integer>>) null);
        Assertions.assertEquals(StreamBase.EMPTY_CLOSE_HANDLER, handler);
    }

    @Test
    public void testNewCloseHandler_Collection_WithStreams() {
        AtomicBoolean closed1 = new AtomicBoolean(false);
        AtomicBoolean closed2 = new AtomicBoolean(false);

        Stream<Integer> s1 = Stream.of(1, 2, 3).onClose(() -> closed1.set(true));
        Stream<Integer> s2 = Stream.of(4, 5, 6).onClose(() -> closed2.set(true));

        java.util.Collection<Stream<Integer>> streams = Arrays.asList(s1, s2);
        LocalRunnable handler = StreamBase.newCloseHandler(streams);

        // handler should be non-empty since streams have close handlers
        Assertions.assertNotEquals(StreamBase.EMPTY_CLOSE_HANDLER, handler);
        handler.run();
        Assertions.assertTrue(closed1.get());
        Assertions.assertTrue(closed2.get());
    }

    @Test
    public void testDeferredCloseClosesSuppliedStreamWithoutCloseHandlers() {
        final AtomicInteger closeCalls = new AtomicInteger();
        final ArrayStream<Integer> supplied = new ArrayStream<>(new Integer[] { 1 }) {
            @Override
            public synchronized void close() {
                if (!isClosed()) {
                    closeCalls.incrementAndGet();
                }

                super.close();
            }
        };

        Assertions.assertTrue(StreamBase.isEmptyCloseHandlers(supplied.closeHandlers()));

        final Stream<Integer> deferred = Stream.defer(() -> supplied);
        deferred.close();

        Assertions.assertEquals(1, closeCalls.get());
        Assertions.assertThrows(IllegalStateException.class, supplied::count);
    }

    @Test
    public void testMergeCloseHandlers() {
        Deque<LocalRunnable> handlers = new ArrayDeque<>();
        handlers.add(LocalRunnable.wrap(Fn.jr(() -> {
        })));

        Deque<LocalRunnable> merged1 = StreamBase.mergeCloseHandlers(StreamBase.EMPTY_CLOSE_HANDLER, handlers);
        Assertions.assertSame(handlers, merged1);

        AtomicBoolean called = new AtomicBoolean(false);
        Runnable newHandler = () -> called.set(true);
        Deque<LocalRunnable> merged2 = StreamBase.mergeCloseHandlers(newHandler, null);
        Assertions.assertEquals(1, merged2.size());

        Deque<LocalRunnable> handlersA = new StreamBase.LocalArrayDeque<>();
        handlersA.add(LocalRunnable.wrap(Fn.jr(() -> {
        })));

        Deque<LocalRunnable> handlersB = new StreamBase.LocalArrayDeque<>();
        handlersB.add(LocalRunnable.wrap(Fn.jr(() -> {
        })));

        Deque<LocalRunnable> merged3 = StreamBase.mergeCloseHandlers(handlersA, handlersB);
        Assertions.assertEquals(2, merged3.size());
    }

    @Test
    public void testMergeCloseHandlers_TwoDeques_BothNonEmpty() {
        LocalRunnable r1 = LocalRunnable.wrap(Fn.jr(() -> {
        }));
        LocalRunnable r2 = LocalRunnable.wrap(Fn.jr(() -> {
        }));

        Deque<LocalRunnable> dequeA = new StreamBase.LocalArrayDeque<>();
        dequeA.add(r1);

        Deque<LocalRunnable> dequeB = new StreamBase.LocalArrayDeque<>();
        dequeB.add(r2);

        Deque<LocalRunnable> merged = StreamBase.mergeCloseHandlers(dequeA, dequeB);
        Assertions.assertEquals(2, merged.size());
    }

    @Test
    public void testMergeCloseHandlers_TwoDeques_FirstEmpty() {
        Deque<LocalRunnable> dequeA = null;
        Deque<LocalRunnable> dequeB = new StreamBase.LocalArrayDeque<>();
        LocalRunnable r = LocalRunnable.wrap(Fn.jr(() -> {
        }));
        dequeB.add(r);

        Deque<LocalRunnable> merged = StreamBase.mergeCloseHandlers(dequeA, dequeB);
        Assertions.assertSame(dequeB, merged);
    }

    @Test
    public void testMergeCloseHandlers_TwoDeques_SecondEmpty() {
        Deque<LocalRunnable> dequeA = new StreamBase.LocalArrayDeque<>();
        LocalRunnable r = LocalRunnable.wrap(Fn.jr(() -> {
        }));
        dequeA.add(r);
        Deque<LocalRunnable> dequeB = null;

        Deque<LocalRunnable> merged = StreamBase.mergeCloseHandlers(dequeA, dequeB);
        Assertions.assertSame(dequeA, merged);
    }

    @Test
    public void testMergeCloseHandlers_TwoDeques_BothEmpty() {
        Deque<LocalRunnable> merged = StreamBase.mergeCloseHandlers((Deque<LocalRunnable>) null, (Deque<LocalRunnable>) null);
        Assertions.assertNull(merged);
    }

    @Test
    public void testSetError() {
        Holder<Throwable> errorHolder = Holder.of(null);
        Exception error1 = new Exception("Error 1");
        StreamBase.setError(errorHolder, error1);
        Assertions.assertSame(error1, errorHolder.value());

        Exception error2 = new Exception("Error 2");
        StreamBase.setError(errorHolder, error2);
        Assertions.assertSame(error1, errorHolder.value());
        Assertions.assertEquals(1, errorHolder.value().getSuppressed().length);
        Assertions.assertSame(error2, errorHolder.value().getSuppressed()[0]);

        final AssertionError repeatedError = new AssertionError("repeated");
        Holder<Throwable> repeatedErrorHolder = Holder.of(null);
        StreamBase.setError(repeatedErrorHolder, repeatedError);
        Assertions.assertDoesNotThrow(() -> StreamBase.setError(repeatedErrorHolder, repeatedError));
        Assertions.assertSame(repeatedError, repeatedErrorHolder.value());
        Assertions.assertEquals(0, repeatedErrorHolder.value().getSuppressed().length);
    }

    @Test
    public void testNewStream_IntArray_WithRange_Sequential() {
        int[] result = Stream.of(1, 2, 3, 4, 5).mapToInt(Integer::intValue).skip(1).limit(3).sorted().toArray();
        Assertions.assertArrayEquals(new int[] { 2, 3, 4 }, result);
    }

    @Test
    public void testNewStream_LongArray_WithRange_Sequential() {
        long[] result = Stream.of(1, 2, 3, 4, 5).mapToLong(Integer::longValue).skip(1).limit(3).sorted().toArray();
        Assertions.assertArrayEquals(new long[] { 2L, 3L, 4L }, result);
    }

    @Test
    public void testNewStream_ByteArray_WithRange_Sequential() {
        byte[] result = Stream.of(1, 2, 3, 4, 5).mapToByte(Integer::byteValue).skip(1).limit(3).sorted().toArray();
        Assertions.assertArrayEquals(new byte[] { 2, 3, 4 }, result);
    }

    @Test
    public void testNewStream_ShortArray_WithRange_Sequential() {
        short[] result = Stream.of(1, 2, 3, 4, 5).mapToShort(Integer::shortValue).skip(1).limit(3).sorted().toArray();
        Assertions.assertArrayEquals(new short[] { 2, 3, 4 }, result);
    }

    @Test
    public void testNewStreamCharRange_SequentialAndParallel() {
        Stream<Integer> sequentialBase = createStream(1, 2, 3);
        CharStream sequential = sequentialBase.newStream(new char[] { 'a', 'b', 'c' }, 1, 3, true);
        Assertions.assertFalse(sequential.isParallel());
        Assertions.assertArrayEquals(new char[] { 'b', 'c' }, sequential.toArray());

        Stream<Integer> parallelBase = createStream(1, 2, 3).parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ARRAY).maxThreadNum(2).build());
        CharStream parallel = parallelBase.newStream(new char[] { 'x', 'y', 'z' }, 0, 2, true);
        Assertions.assertTrue(parallel.isParallel());
        Assertions.assertArrayEquals(new char[] { 'x', 'y' }, parallel.toArray());
    }

    @Test
    public void testNewStream_ByteArray_WithRange_Parallel() {
        // Tests newStream(byte[], int, int, boolean) via parallel mapToByte
        byte[] result = Stream.of(1, 2, 3, 4, 5)
                .parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ARRAY).maxThreadNum(2).build())
                .mapToByte(i -> i.byteValue())
                .sorted()
                .toArray();
        Assertions.assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testNewStream_ShortArray_WithRange_Parallel() {
        short[] result = Stream.of(1, 2, 3, 4, 5)
                .parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ARRAY).maxThreadNum(2).build())
                .mapToShort(i -> i.shortValue())
                .sorted()
                .toArray();
        Assertions.assertArrayEquals(new short[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testNewStream_IntArray_WithRange_Parallel() {
        int[] result = Stream.of(1, 2, 3, 4, 5)
                .parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ARRAY).maxThreadNum(2).build())
                .mapToInt(i -> i)
                .sorted()
                .toArray();
        Assertions.assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testNewStream_LongArray_WithRange_Parallel() {
        long[] result = Stream.of(1, 2, 3, 4, 5)
                .parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ARRAY).maxThreadNum(2).build())
                .mapToLong(i -> i.longValue())
                .sorted()
                .toArray();
        Assertions.assertArrayEquals(new long[] { 1L, 2L, 3L, 4L, 5L }, result);
    }

    @Test
    public void testNewStream_FloatArray_WithRange_Parallel() {
        float[] result = Stream.of(1, 2, 3, 4, 5)
                .parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ARRAY).maxThreadNum(2).build())
                .mapToFloat(i -> i.floatValue())
                .sorted()
                .toArray();
        Assertions.assertArrayEquals(new float[] { 1f, 2f, 3f, 4f, 5f }, result, 0.001f);
    }

    @Test
    public void testNewStream_DoubleArray_WithRange_Parallel() {
        double[] result = Stream.of(1, 2, 3, 4, 5)
                .parallel(ParallelSettings.builder().splitStrategy(SplitStrategy.ARRAY).maxThreadNum(2).build())
                .mapToDouble(i -> i.doubleValue())
                .sorted()
                .toArray();
        Assertions.assertArrayEquals(new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 }, result, 0.001);
    }

    @Test
    public void testIsEmptyRange() {
        Assertions.assertTrue(StreamBase.isEmptyRange(5, 2, 2));
        Assertions.assertFalse(StreamBase.isEmptyRange(5, 0, 3));
        Assertions.assertTrue(StreamBase.isEmptyRange(5, 0, 0));

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> StreamBase.isEmptyRange(5, 3, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> StreamBase.isEmptyRange(5, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> StreamBase.isEmptyRange(5, 0, 6));
    }

    @Test
    public void testIterateMethods() {
        CharStream charStream = CharStream.of('a', 'b', 'c');
        CharIteratorEx charIter = StreamBase.iterate(charStream);
        Assertions.assertTrue(charIter.hasNext());
        Assertions.assertEquals('a', charIter.nextChar());

        CharIteratorEx emptyCharIter = StreamBase.iterate((CharStream) null);
        Assertions.assertFalse(emptyCharIter.hasNext());

        ByteStream byteStream = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteIteratorEx byteIter = StreamBase.iterate(byteStream);
        Assertions.assertTrue(byteIter.hasNext());
        Assertions.assertEquals((byte) 1, byteIter.nextByte());

        IntStream intStream = IntStream.of(1, 2, 3);
        IntIteratorEx intIter = StreamBase.iterate(intStream);
        Assertions.assertTrue(intIter.hasNext());
        Assertions.assertEquals(1, intIter.nextInt());

        Stream<String> objStream = Stream.of("a", "b", "c");
        ObjIteratorEx<String> objIter = StreamBase.iterate(objStream);
        Assertions.assertTrue(objIter.hasNext());
        Assertions.assertEquals("a", objIter.next());
    }

    @Test
    public void testIterateShortStream() {
        ShortStream shortStream = ShortStream.of((short) 10, (short) 20, (short) 30);
        ShortIteratorEx shortIter = StreamBase.iterate(shortStream);
        Assertions.assertTrue(shortIter.hasNext());
        Assertions.assertEquals((short) 10, shortIter.nextShort());

        ShortIteratorEx emptyShortIter = StreamBase.iterate((ShortStream) null);
        Assertions.assertFalse(emptyShortIter.hasNext());
    }

    @Test
    public void testIterateLongStream() {
        LongStream longStream = LongStream.of(100L, 200L, 300L);
        LongIteratorEx longIter = StreamBase.iterate(longStream);
        Assertions.assertTrue(longIter.hasNext());
        Assertions.assertEquals(100L, longIter.nextLong());

        LongIteratorEx emptyLongIter = StreamBase.iterate((LongStream) null);
        Assertions.assertFalse(emptyLongIter.hasNext());
    }

    @Test
    public void testIterateFloatDoubleStream() {
        FloatStream floatStream = FloatStream.of(1.5f, 2.5f);
        FloatIteratorEx floatIter = StreamBase.iterate(floatStream);
        Assertions.assertTrue(floatIter.hasNext());
        Assertions.assertEquals(1.5f, floatIter.nextFloat(), 0.001f);

        FloatIteratorEx emptyFloatIter = StreamBase.iterate((FloatStream) null);
        Assertions.assertFalse(emptyFloatIter.hasNext());

        DoubleStream doubleStream = DoubleStream.of(1.5, 2.5);
        DoubleIteratorEx doubleIter = StreamBase.iterate(doubleStream);
        Assertions.assertTrue(doubleIter.hasNext());
        Assertions.assertEquals(1.5, doubleIter.nextDouble(), 0.001);

        DoubleIteratorEx emptyDoubleIter = StreamBase.iterate((DoubleStream) null);
        Assertions.assertFalse(emptyDoubleIter.hasNext());
    }

    @Test
    public void testIterateAll() {
        List<Stream<Integer>> streams = Arrays.asList(Stream.of(1, 2, 3), Stream.of(4, 5, 6), Stream.of(7, 8, 9));

        List<ObjIteratorEx<Integer>> iterators = StreamBase.iterateAll(streams);
        Assertions.assertEquals(3, iterators.size());

        for (int i = 0; i < 3; i++) {
            Assertions.assertTrue(iterators.get(i).hasNext());
            Assertions.assertEquals(i * 3 + 1, iterators.get(i).next());
        }

        List<ObjIteratorEx<Integer>> emptyIterators = StreamBase.iterateAll(null);
        Assertions.assertEquals(0, emptyIterators.size());

        List<ObjIteratorEx<Integer>> emptyIterators2 = StreamBase.iterateAll(new ArrayList<>());
        Assertions.assertEquals(0, emptyIterators2.size());

        List<Stream<Integer>> streamsWithNull = Arrays.asList(Stream.of(1, 2, 3), null, Stream.of(7, 8, 9));

        List<ObjIteratorEx<Integer>> iteratorsWithNull = StreamBase.iterateAll(streamsWithNull);
        Assertions.assertEquals(3, iteratorsWithNull.size());
        Assertions.assertFalse(iteratorsWithNull.get(1).hasNext());
    }

    @Test
    public void testZipOpeningFailurePreservesPrimaryWhenCloseRethrowsIt() {
        for (int sourceCount : new int[] { 2, 3, 4 }) {
            IllegalStateException failure = new IllegalStateException("source opening failed");
            AtomicInteger closed = new AtomicInteger();
            Stream<Integer> first = Stream.of(1).onClose(() -> {
                closed.incrementAndGet();
                throw failure;
            });
            Stream<Integer> second = Stream.of(2).onClose(closed::incrementAndGet);
            Stream<Integer> failing = streamFailingToOpen(failure);

            IllegalStateException actual = Assertions.assertThrows(IllegalStateException.class, () -> {
                if (sourceCount == 2) {
                    Stream.zip(first, failing, Integer::sum);
                } else if (sourceCount == 3) {
                    Stream.zip(first, second, failing, (a, b, c) -> a + b + c);
                } else {
                    Stream.zip(Arrays.asList(first, null, second, failing), values -> values);
                }
            });

            Assertions.assertSame(failure, actual);
            Assertions.assertEquals(0, actual.getSuppressed().length);
            Assertions.assertEquals(sourceCount == 2 ? 1 : 2, closed.get());
            second.close();
            failing.close();
        }
    }

    @Test
    public void testZipOpeningFailureSuppressesCloseErrorAndClosesRemainingSources() {
        for (boolean collectionOverload : new boolean[] { false, true }) {
            IllegalStateException failure = new IllegalStateException("source opening failed");
            AssertionError closeError = new AssertionError("first source close failed");
            IllegalArgumentException closeException = new IllegalArgumentException("second source close failed");
            AtomicInteger closed = new AtomicInteger();
            Stream<Integer> first = Stream.of(1).onClose(() -> {
                closed.incrementAndGet();
                throw closeError;
            });
            Stream<Integer> second = Stream.of(2).onClose(() -> {
                closed.incrementAndGet();
                throw closeException;
            });
            Stream<Integer> failing = streamFailingToOpen(failure);

            IllegalStateException actual = Assertions.assertThrows(IllegalStateException.class, () -> {
                if (collectionOverload) {
                    Stream.zip(Arrays.asList(first, null, second, failing), values -> values);
                } else {
                    Stream.zip(first, second, failing, (a, b, c) -> a + b + c);
                }
            });

            Assertions.assertSame(failure, actual);
            Assertions.assertArrayEquals(new Throwable[] { closeError, closeException }, actual.getSuppressed());
            Assertions.assertEquals(2, closed.get());
            failing.close();
        }
    }

    @Test
    public void testZipOpeningErrorPreservesPrimaryAndLeavesUnopenedSourcesUsable() {
        for (boolean collectionOverload : new boolean[] { false, true }) {
            AssertionError failure = new AssertionError("source opening failed");
            IllegalStateException cleanupFailure = new IllegalStateException("source close failed");
            AtomicInteger closed = new AtomicInteger();
            AtomicInteger unopenedClosed = new AtomicInteger();
            Stream<Integer> first = Stream.of(1).onClose(() -> {
                closed.incrementAndGet();
                throw failure;
            });
            Stream<Integer> second = Stream.of(2).onClose(() -> {
                closed.incrementAndGet();
                throw cleanupFailure;
            });
            Stream<Integer> failing = new IteratorStream<Integer>(Collections.emptyIterator()) {
                @Override
                ObjIteratorEx<Integer> iteratorEx() {
                    throw failure;
                }
            }.onClose(unopenedClosed::incrementAndGet);
            Stream<Integer> suffix = Stream.of(4).onClose(unopenedClosed::incrementAndGet);

            try {
                AssertionError actual = Assertions.assertThrows(AssertionError.class, () -> {
                    if (collectionOverload) {
                        Stream.zip(Arrays.asList(first, null, second, failing, suffix), values -> values);
                    } else {
                        Stream.zip(first, second, failing, (a, b, c) -> a + b + c);
                    }
                });
                Assertions.assertSame(failure, actual);
                Assertions.assertArrayEquals(new Throwable[] { cleanupFailure }, actual.getSuppressed());
                Assertions.assertEquals(2, closed.get());
                Assertions.assertEquals(0, unopenedClosed.get());
                Assertions.assertEquals(List.of(4), suffix.toList());
            } finally {
                failing.close();
                suffix.close();
            }
            Assertions.assertEquals(2, unopenedClosed.get());
        }
    }

    @Test
    public void testEntryStreamTransferPreservesInputModeAndClosesOwnedStreams() {
        for (int scenario = 0; scenario < 6; scenario++) {
            int variant = scenario % 3;
            boolean deferred = variant == 2;
            boolean parallelResult = scenario >= 3;
            AtomicInteger invoked = new AtomicInteger();
            AtomicInteger inputClosed = new AtomicInteger();
            AtomicInteger outputClosed = new AtomicInteger();
            EntryStream<String, Integer> input = EntryStream.of("input", 1).parallel(2).onClose(inputClosed::incrementAndGet);
            com.landawn.abacus.util.function.Function<Stream<java.util.Map.Entry<String, Integer>>, Stream<java.util.Map.Entry<String, Integer>>> transfer = source -> {
                Assertions.assertTrue(source.isParallel());
                invoked.incrementAndGet();
                Stream<java.util.Map.Entry<String, Integer>> output = Stream.<java.util.Map.Entry<String, Integer>> of(
                        new java.util.AbstractMap.SimpleImmutableEntry<>("output", 2)).onClose(outputClosed::incrementAndGet);
                return parallelResult ? output.parallel(2) : output;
            };
            try (EntryStream<String, Integer> result = variant == 0 ? input.transformViaStream(transfer) : input.transformViaStream(transfer, deferred)) {
                Assertions.assertEquals(deferred ? 0 : 1, invoked.get());
                Assertions.assertEquals(parallelResult && !deferred, result.isParallel());
                Assertions.assertEquals(java.util.Map.of("output", 2), result.toMap());
                Assertions.assertEquals(1, invoked.get());
                Assertions.assertEquals(1, inputClosed.get());
                Assertions.assertEquals(1, outputClosed.get());
            }
            Assertions.assertEquals(1, inputClosed.get());
            Assertions.assertEquals(1, outputClosed.get());
        }
    }

    @Test
    public void testJdkTransfersPreserveInputModeAndHonorReturnedMode() {
        for (int type = 0; type < 4; type++) {
            for (int scenario = 0; scenario < 16; scenario++) {
                boolean deferred = (scenario & 1) != 0;
                boolean parallelResult = (scenario & 2) != 0;
                boolean arraySource = (scenario & 4) != 0;
                boolean parallelInput = (scenario & 8) != 0;
                AtomicInteger invoked = new AtomicInteger();
                AtomicInteger inputClosed = new AtomicInteger();
                AtomicInteger outputClosed = new AtomicInteger();
                BaseStream<?, ?, ?, ?, ?, ?, ?, ?> input = switch (type) {
                    case 0 -> arraySource ? Stream.of(1, 2) : Stream.of(Arrays.asList(1, 2).iterator());
                    case 1 -> arraySource ? IntStream.of(1, 2) : IntStream.of(IntIteratorEx.of(new int[] { 1, 2 }));
                    case 2 -> arraySource ? LongStream.of(1, 2) : LongStream.of(LongIteratorEx.of(new long[] { 1, 2 }));
                    default -> arraySource ? DoubleStream.of(1, 2) : DoubleStream.of(DoubleIteratorEx.of(new double[] { 1, 2 }));
                };
                input = (parallelInput ? input.parallel(2) : input).onClose(inputClosed::incrementAndGet);
                BaseStream<?, ?, ?, ?, ?, ?, ?, ?> transformed = switch (type) {
                    case 0 -> ((Stream<Integer>) input).transformViaJdkStream(source -> {
                        Assertions.assertEquals(parallelInput, source.isParallel());
                        invoked.incrementAndGet();
                        java.util.stream.Stream<Integer> output = java.util.stream.Stream.of(3, 4).onClose(outputClosed::incrementAndGet);
                        return parallelResult ? output.parallel() : output;
                    }, deferred);
                    case 1 -> ((IntStream) input).transformViaJdkStream(source -> {
                        Assertions.assertEquals(parallelInput, source.isParallel());
                        invoked.incrementAndGet();
                        java.util.stream.IntStream output = java.util.stream.IntStream.of(3, 4).onClose(outputClosed::incrementAndGet);
                        return parallelResult ? output.parallel() : output;
                    }, deferred);
                    case 2 -> ((LongStream) input).transformViaJdkStream(source -> {
                        Assertions.assertEquals(parallelInput, source.isParallel());
                        invoked.incrementAndGet();
                        java.util.stream.LongStream output = java.util.stream.LongStream.of(3, 4).onClose(outputClosed::incrementAndGet);
                        return parallelResult ? output.parallel() : output;
                    }, deferred);
                    default -> ((DoubleStream) input).transformViaJdkStream(source -> {
                        Assertions.assertEquals(parallelInput, source.isParallel());
                        invoked.incrementAndGet();
                        java.util.stream.DoubleStream output = java.util.stream.DoubleStream.of(3, 4).onClose(outputClosed::incrementAndGet);
                        return parallelResult ? output.parallel() : output;
                    }, deferred);
                };
                try (transformed) {
                    Assertions.assertEquals(deferred ? 0 : 1, invoked.get());
                    Assertions.assertEquals(parallelResult && !deferred, transformed.isParallel());
                    Assertions.assertEquals(2, transformed.count());
                    Assertions.assertEquals(1, invoked.get());
                }
                Assertions.assertEquals(1, inputClosed.get());
                Assertions.assertEquals(1, outputClosed.get());
            }
        }
    }

    @Test
    public void testContainsAllSupportsParallelSourcesAndClosesExactlyOnce() {
        Integer[][] requests = { { 2 }, { 1, 3 }, { 2, 2, 2 }, { 2, 4 }, {} };
        boolean[] expected = { true, true, true, false, true };
        for (boolean arraySource : new boolean[] { false, true }) {
            for (boolean collectionOverload : new boolean[] { false, true }) {
                for (int i = 0; i < requests.length; i++) {
                    AtomicInteger closed = new AtomicInteger();
                    try (Stream<Integer> source = (arraySource ? Stream.of(1, 2, 3) : Stream.of(Arrays.asList(1, 2, 3).iterator())).parallel(2)
                            .onClose(closed::incrementAndGet)) {
                        Assertions.assertTrue(source.isParallel());
                        boolean actual = collectionOverload ? source.containsAll(Arrays.asList(requests[i])) : source.containsAll(requests[i]);
                        Assertions.assertEquals(expected[i], actual);
                        Assertions.assertEquals(1, closed.get());
                    }
                    Assertions.assertEquals(1, closed.get());
                }
            }
        }
    }

    private static Stream<Integer> streamFailingToOpen(final RuntimeException failure) {
        return new IteratorStream<Integer>(Collections.emptyIterator()) {
            @Override
            ObjIteratorEx<Integer> iteratorEx() {
                throw failure;
            }
        };
    }

    @Test
    public void testAppendIfEmptyPreservesOpeningFailureAcrossStreamTypes() {
        for (int type = 0; type < 8; type++) {
            for (boolean arraySource : new boolean[] { false, true }) {
                for (boolean sameFailure : new boolean[] { false, true }) {
                    IllegalStateException failure = new IllegalStateException("fallback opening failed");
                    AssertionError closeError = new AssertionError("fallback close failed");
                    AtomicInteger closed = new AtomicInteger();
                    BaseStream<?, ?, ?, ?, ?, ?, ?, ?> fallback = fallbackFailingToOpen(type, failure).onClose(() -> {
                        closed.incrementAndGet();
                        if (sameFailure) {
                            throw failure;
                        }
                        throw closeError;
                    });

                    try (BaseStream<?, ?, ?, ?, ?, ?, ?, ?> stream = switch (type) {
                        case 0 -> (arraySource ? Stream.<Integer> empty() : Stream.of(ObjIteratorEx.<Integer> empty()))
                                .appendIfEmpty(() -> (Stream<Integer>) fallback);
                        case 1 -> (arraySource ? ByteStream.empty() : ByteStream.of(ByteIteratorEx.empty())).appendIfEmpty(() -> (ByteStream) fallback);
                        case 2 -> (arraySource ? CharStream.empty() : CharStream.of(CharIteratorEx.empty())).appendIfEmpty(() -> (CharStream) fallback);
                        case 3 -> (arraySource ? ShortStream.empty() : ShortStream.of(ShortIteratorEx.empty())).appendIfEmpty(() -> (ShortStream) fallback);
                        case 4 -> (arraySource ? IntStream.empty() : IntStream.of(IntIteratorEx.empty())).appendIfEmpty(() -> (IntStream) fallback);
                        case 5 -> (arraySource ? LongStream.empty() : LongStream.of(LongIteratorEx.empty())).appendIfEmpty(() -> (LongStream) fallback);
                        case 6 -> (arraySource ? FloatStream.empty() : FloatStream.of(FloatIteratorEx.empty())).appendIfEmpty(() -> (FloatStream) fallback);
                        case 7 -> (arraySource ? DoubleStream.empty() : DoubleStream.of(DoubleIteratorEx.empty())).appendIfEmpty(() -> (DoubleStream) fallback);
                        default -> throw new AssertionError(type);
                    }) {
                        IllegalStateException actual = Assertions.assertThrows(IllegalStateException.class, () -> stream.iterator().hasNext());
                        Assertions.assertSame(failure, actual);
                        Assertions.assertArrayEquals(sameFailure ? new Throwable[0] : new Throwable[] { closeError }, actual.getSuppressed());
                        Assertions.assertEquals(1, closed.get());
                    }
                }
            }
        }
    }

    @Test
    public void testPrimitiveCollectionZipClosesOnlyOpenedSourcesOnFailure() {
        for (int type = 1; type < 8; type++) {
            for (boolean padded : new boolean[] { false, true }) {
                for (boolean primitiveResult : new boolean[] { false, true }) {
                    IllegalStateException failure = new IllegalStateException("primitive source opening failed");
                    AssertionError closeError = new AssertionError("first close failed");
                    AtomicInteger openedClosed = new AtomicInteger();
                    AtomicInteger unopenedClosed = new AtomicInteger();
                    BaseStream<?, ?, ?, ?, ?, ?, ?, ?> first = emptyPrimitiveStream(type).onClose(() -> {
                        openedClosed.incrementAndGet();
                        throw closeError;
                    });
                    BaseStream<?, ?, ?, ?, ?, ?, ?, ?> second = emptyPrimitiveStream(type).onClose(() -> {
                        openedClosed.incrementAndGet();
                        throw failure;
                    });
                    BaseStream<?, ?, ?, ?, ?, ?, ?, ?> failing = fallbackFailingToOpen(type, failure).onClose(unopenedClosed::incrementAndGet);
                    BaseStream<?, ?, ?, ?, ?, ?, ?, ?> suffix = emptyPrimitiveStream(type).onClose(unopenedClosed::incrementAndGet);
                    List<BaseStream<?, ?, ?, ?, ?, ?, ?, ?>> sources = Arrays.asList(null, first, second, failing, suffix);
                    final int streamType = type;
                    try {
                        IllegalStateException actual = Assertions.assertThrows(IllegalStateException.class,
                                () -> primitiveCollectionZip(streamType, padded, primitiveResult, sources));
                        Assertions.assertSame(failure, actual);
                        Assertions.assertArrayEquals(new Throwable[] { closeError }, actual.getSuppressed());
                        Assertions.assertEquals(2, openedClosed.get());
                        Assertions.assertEquals(0, unopenedClosed.get());
                        Assertions.assertFalse(suffix.iterator().hasNext(), "Unopened suffix remains usable");
                    } finally {
                        failing.close();
                        suffix.close();
                    }
                }
            }
        }
    }

    @Test
    public void testParallelCollectionZipValidatesThreadCountBeforeOpeningSources() {
        for (int threadCount : new int[] { 0, -1 }) {
            for (boolean padded : new boolean[] { false, true }) {
                AtomicInteger opened = new AtomicInteger();
                AtomicInteger closed = new AtomicInteger();
                try (Stream<Integer> source = new IteratorStream<Integer>(ObjIteratorEx.of(1, 2)) {
                    @Override
                    ObjIteratorEx<Integer> iteratorEx() {
                        opened.incrementAndGet();
                        return super.iteratorEx();
                    }
                }.onClose(closed::incrementAndGet)) {
                    Assertions.assertThrows(IllegalArgumentException.class, () -> {
                        if (padded) {
                            Stream.parallelZip(List.of(source), List.of(0), values -> values.get(0), threadCount);
                        } else {
                            Stream.parallelZip(List.of(source), values -> values.get(0), threadCount);
                        }
                    });
                    Assertions.assertEquals(0, opened.get(), "Invalid options must not acquire source iterators");
                    Assertions.assertEquals(0, closed.get());
                    Assertions.assertEquals(List.of(1, 2), source.toList());
                }
                Assertions.assertEquals(1, closed.get());
            }
        }
    }

    private static BaseStream<?, ?, ?, ?, ?, ?, ?, ?> emptyPrimitiveStream(final int type) {
        return switch (type) {
            case 1 -> ByteStream.empty();
            case 2 -> CharStream.empty();
            case 3 -> ShortStream.empty();
            case 4 -> IntStream.empty();
            case 5 -> LongStream.empty();
            case 6 -> FloatStream.empty();
            case 7 -> DoubleStream.empty();
            default -> throw new AssertionError(type);
        };
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static BaseStream<?, ?, ?, ?, ?, ?, ?, ?> primitiveCollectionZip(final int type, final boolean padded, final boolean primitiveResult,
            final Collection sources) {
        return switch (type) {
            case 1 -> {
                com.landawn.abacus.util.function.ByteNFunction<Byte> zipper = values -> (byte) 0;
                if (primitiveResult) {
                    yield padded ? ByteStream.zip(sources, new byte[sources.size()], zipper) : ByteStream.zip(sources, zipper);
                }
                yield padded ? Stream.zip(sources, new byte[sources.size()], zipper) : Stream.zip(sources, zipper);
            }
            case 2 -> {
                com.landawn.abacus.util.function.CharNFunction<Character> zipper = values -> (char) 0;
                if (primitiveResult) {
                    yield padded ? CharStream.zip(sources, new char[sources.size()], zipper) : CharStream.zip(sources, zipper);
                }
                yield padded ? Stream.zip(sources, new char[sources.size()], zipper) : Stream.zip(sources, zipper);
            }
            case 3 -> {
                com.landawn.abacus.util.function.ShortNFunction<Short> zipper = values -> (short) 0;
                if (primitiveResult) {
                    yield padded ? ShortStream.zip(sources, new short[sources.size()], zipper) : ShortStream.zip(sources, zipper);
                }
                yield padded ? Stream.zip(sources, new short[sources.size()], zipper) : Stream.zip(sources, zipper);
            }
            case 4 -> {
                com.landawn.abacus.util.function.IntNFunction<Integer> zipper = values -> (int) 0;
                if (primitiveResult) {
                    yield padded ? IntStream.zip(sources, new int[sources.size()], zipper) : IntStream.zip(sources, zipper);
                }
                yield padded ? Stream.zip(sources, new int[sources.size()], zipper) : Stream.zip(sources, zipper);
            }
            case 5 -> {
                com.landawn.abacus.util.function.LongNFunction<Long> zipper = values -> (long) 0;
                if (primitiveResult) {
                    yield padded ? LongStream.zip(sources, new long[sources.size()], zipper) : LongStream.zip(sources, zipper);
                }
                yield padded ? Stream.zip(sources, new long[sources.size()], zipper) : Stream.zip(sources, zipper);
            }
            case 6 -> {
                com.landawn.abacus.util.function.FloatNFunction<Float> zipper = values -> (float) 0;
                if (primitiveResult) {
                    yield padded ? FloatStream.zip(sources, new float[sources.size()], zipper) : FloatStream.zip(sources, zipper);
                }
                yield padded ? Stream.zip(sources, new float[sources.size()], zipper) : Stream.zip(sources, zipper);
            }
            case 7 -> {
                com.landawn.abacus.util.function.DoubleNFunction<Double> zipper = values -> (double) 0;
                if (primitiveResult) {
                    yield padded ? DoubleStream.zip(sources, new double[sources.size()], zipper) : DoubleStream.zip(sources, zipper);
                }
                yield padded ? Stream.zip(sources, new double[sources.size()], zipper) : Stream.zip(sources, zipper);
            }
            default -> throw new AssertionError(type);
        };
    }

    private static BaseStream<?, ?, ?, ?, ?, ?, ?, ?> fallbackFailingToOpen(final int type, final RuntimeException failure) {
        return switch (type) {
            case 0 -> streamFailingToOpen(failure);
            case 1 -> new IteratorByteStream(ByteIteratorEx.empty()) {
                @Override
                ByteIteratorEx iteratorEx() {
                    throw failure;
                }
            };
            case 2 -> new IteratorCharStream(CharIteratorEx.empty()) {
                @Override
                CharIteratorEx iteratorEx() {
                    throw failure;
                }
            };
            case 3 -> new IteratorShortStream(ShortIteratorEx.empty()) {
                @Override
                ShortIteratorEx iteratorEx() {
                    throw failure;
                }
            };
            case 4 -> new IteratorIntStream(IntIteratorEx.empty()) {
                @Override
                IntIteratorEx iteratorEx() {
                    throw failure;
                }
            };
            case 5 -> new IteratorLongStream(LongIteratorEx.empty()) {
                @Override
                LongIteratorEx iteratorEx() {
                    throw failure;
                }
            };
            case 6 -> new IteratorFloatStream(FloatIteratorEx.empty()) {
                @Override
                FloatIteratorEx iteratorEx() {
                    throw failure;
                }
            };
            case 7 -> new IteratorDoubleStream(DoubleIteratorEx.empty()) {
                @Override
                DoubleIteratorEx iteratorEx() {
                    throw failure;
                }
            };
            default -> throw new AssertionError(type);
        };
    }

    @Test
    public void testPrimitiveIteratorConversions() {
        ObjIteratorEx<Character> charObjIter = ObjIteratorEx.of('a', 'b', 'c');
        CharIteratorEx charIter = StreamBase.charIterator(charObjIter);
        Assertions.assertEquals('a', charIter.nextChar());

        ObjIteratorEx<Byte> byteObjIter = ObjIteratorEx.of((byte) 1, (byte) 2, (byte) 3);
        ByteIteratorEx byteIter = StreamBase.byteIterator(byteObjIter);
        Assertions.assertEquals((byte) 1, byteIter.nextByte());

        ObjIteratorEx<Integer> intObjIter = ObjIteratorEx.of(1, 2, 3);
        IntIteratorEx intIter = StreamBase.intIterator(intObjIter);
        Assertions.assertEquals(1, intIter.nextInt());

        ObjIteratorEx<Long> longObjIter = ObjIteratorEx.of(1L, 2L, 3L);
        LongIteratorEx longIter = StreamBase.longIterator(longObjIter);
        Assertions.assertEquals(1L, longIter.nextLong());

        ObjIteratorEx<Float> floatObjIter = ObjIteratorEx.of(1.0f, 2.0f, 3.0f);
        FloatIteratorEx floatIter = StreamBase.floatIterator(floatObjIter);
        Assertions.assertEquals(1.0f, floatIter.nextFloat(), 0.001f);

        ObjIteratorEx<Double> doubleObjIter = ObjIteratorEx.of(1.0, 2.0, 3.0);
        DoubleIteratorEx doubleIter = StreamBase.doubleIterator(doubleObjIter);
        Assertions.assertEquals(1.0, doubleIter.nextDouble(), 0.001);
    }

    @Test
    public void testSumCharArray() {
        Assertions.assertEquals(0, StreamBase.sum((char[]) null));
        Assertions.assertEquals(0, StreamBase.sum(new char[0]));

        char[] arr = { 'a', 'b', 'c' };
        Assertions.assertEquals(294, StreamBase.sum(arr));

        Assertions.assertEquals(195, StreamBase.sum(arr, 0, 2));
        Assertions.assertEquals(99, StreamBase.sum(arr, 2, 3));
        Assertions.assertEquals(0, StreamBase.sum(arr, 1, 1));
    }

    @Test
    public void testSumByteArray() {
        Assertions.assertEquals(0, StreamBase.sum((byte[]) null));
        Assertions.assertEquals(0, StreamBase.sum(new byte[0]));

        byte[] arr = { 1, 2, 3, 4, 5 };
        Assertions.assertEquals(15, StreamBase.sum(arr));

        Assertions.assertEquals(6, StreamBase.sum(arr, 0, 3));
        Assertions.assertEquals(9, StreamBase.sum(arr, 3, 5));

        byte[] negArr = { -1, -2, 3 };
        Assertions.assertEquals(0, StreamBase.sum(negArr));
    }

    @Test
    public void testSumShortArray() {
        Assertions.assertEquals(0, StreamBase.sum((short[]) null));
        Assertions.assertEquals(0, StreamBase.sum(new short[0]));

        short[] arr = { 100, 200, 300, 400, 500 };
        Assertions.assertEquals(1500, StreamBase.sum(arr));

        Assertions.assertEquals(600, StreamBase.sum(arr, 0, 3));
        Assertions.assertEquals(900, StreamBase.sum(arr, 3, 5));
    }

    @Test
    public void testSumLongArray() {
        Assertions.assertEquals(0L, StreamBase.sum((long[]) null));
        Assertions.assertEquals(0L, StreamBase.sum(new long[0]));

        long[] arr = { 1000L, 2000L, 3000L, 4000L, 5000L };
        Assertions.assertEquals(15000L, StreamBase.sum(arr));

        Assertions.assertEquals(6000L, StreamBase.sum(arr, 0, 3));
        Assertions.assertEquals(9000L, StreamBase.sum(arr, 3, 5));

        long[] largeArr = { Long.MAX_VALUE / 2, Long.MAX_VALUE / 2 };
        Assertions.assertEquals(Long.MAX_VALUE - 1, StreamBase.sum(largeArr));
    }

    @Test
    public void testSumFloatArray() {
        Assertions.assertEquals(0.0, StreamBase.sum((float[]) null));
        Assertions.assertEquals(0.0, StreamBase.sum(new float[0]));

        float[] arr = { 1.5f, 2.5f, 3.5f };
        Assertions.assertEquals(7.5, StreamBase.sum(arr), 0.001);

        Assertions.assertEquals(4.0, StreamBase.sum(arr, 0, 2), 0.001);
        Assertions.assertEquals(3.5, StreamBase.sum(arr, 2, 3), 0.001);
    }

    @Test
    public void testSumDoubleArray() {
        Assertions.assertEquals(0.0, StreamBase.sum((double[]) null));
        Assertions.assertEquals(0.0, StreamBase.sum(new double[0]));

        double[] arr = { 1.5, 2.5, 3.5, 4.5 };
        Assertions.assertEquals(12.0, StreamBase.sum(arr), 0.001);

        Assertions.assertEquals(4.0, StreamBase.sum(arr, 0, 2), 0.001);
        Assertions.assertEquals(8.0, StreamBase.sum(arr, 2, 4), 0.001);
    }

    @Test
    public void testSumIntArray() {
        Assertions.assertEquals(0, StreamBase.sumToLong((int[]) null));
        Assertions.assertEquals(0, StreamBase.sumToLong(new int[0]));

        int[] arr = { 1000, 2000, 3000, 4000, 5000 };
        Assertions.assertEquals(15000, StreamBase.sumToLong(arr));

        Assertions.assertEquals(6000, StreamBase.sumToLong(arr, 0, 3));
        Assertions.assertEquals(9000, StreamBase.sumToLong(arr, 3, 5));

        int[] largeArr = { Integer.MAX_VALUE, 1 };
        Assertions.assertEquals(Integer.MAX_VALUE + 1L, StreamBase.sumToLong(largeArr));
    }

    @Test
    public void testCompleteToClose() {
        MutableBoolean onGoing = MutableBoolean.of(true);
        Holder<AsyncExecutor> executorHolder = Holder.of(null);

        Deque<LocalRunnable> closeHandlers = StreamBase.completeToClose(onGoing, executorHolder);
        Assertions.assertEquals(1, closeHandlers.size());

        closeHandlers.getFirst().run();
        Assertions.assertFalse(onGoing.value());

        AsyncExecutor executor = new AsyncExecutor(Executors.newFixedThreadPool(2));
        executorHolder.setValue(executor);
        onGoing.setTrue();

        Deque<LocalRunnable> closeHandlers2 = StreamBase.completeToClose(onGoing, executorHolder);
        closeHandlers2.getFirst().run();
        Assertions.assertFalse(onGoing.value());
    }

    @Test
    public void testCompleteAndShutdownTempExecutor() {
        List<ContinuableFuture<Void>> futures = new ArrayList<>();
        Holder<Throwable> errorHolder = Holder.of(null);
        AsyncExecutor asyncExecutor = new AsyncExecutor(Executors.newFixedThreadPool(2));

        futures.add(asyncExecutor.execute(() -> {
        }));
        futures.add(asyncExecutor.execute(() -> {
        }));

        Assertions.assertDoesNotThrow(() -> StreamBase.complete(futures, errorHolder));

        errorHolder.setValue(new RuntimeException("Test error"));
        Assertions.assertThrows(RuntimeException.class, () -> StreamBase.complete(futures, errorHolder));
    }

    @Test
    public void testCompleteWaitsForEveryFutureBeforePropagatingWorkerError() throws Exception {
        final ExecutorService service = Executors.newFixedThreadPool(1);
        final AsyncExecutor executor = new AsyncExecutor(service);
        final RuntimeException primary = new RuntimeException("primary");
        final IllegalStateException secondary = new IllegalStateException("secondary");
        final Holder<Throwable> errorHolder = Holder.<Throwable> of(primary);
        final CountDownLatch siblingStarted = new CountDownLatch(1);
        final CountDownLatch releaseSibling = new CountDownLatch(1);
        final CountDownLatch completionReturned = new CountDownLatch(1);
        final AtomicBoolean siblingFinished = new AtomicBoolean();
        final AtomicReference<Throwable> thrown = new AtomicReference<>();
        final List<ContinuableFuture<Void>> futures = new ArrayList<>();

        final ContinuableFuture<Void> sibling = executor.execute(() -> {
            siblingStarted.countDown();

            try {
                releaseSibling.await();
                siblingFinished.set(true);
                throw secondary;
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });

        Assertions.assertTrue(siblingStarted.await(5, TimeUnit.SECONDS));

        final ContinuableFuture<Void> cancelled = executor.execute(() -> {
            // Queued behind sibling so cancellation is deterministic.
        });
        Assertions.assertTrue(cancelled.cancel(true));
        futures.add(cancelled);
        futures.add(sibling);

        final Thread completingThread = new Thread(() -> {
            try {
                StreamBase.complete(futures, errorHolder);
            } catch (final Throwable e) { // NOSONAR
                thrown.set(e);
            } finally {
                completionReturned.countDown();
            }
        });

        try {
            completingThread.start();

            Assertions.assertFalse(completionReturned.await(200, TimeUnit.MILLISECONDS));
            releaseSibling.countDown();
            Assertions.assertTrue(completionReturned.await(5, TimeUnit.SECONDS));
            Assertions.assertTrue(siblingFinished.get());
            Assertions.assertSame(primary, thrown.get());
            Assertions.assertEquals(1, primary.getSuppressed().length);
            Assertions.assertInstanceOf(java.util.concurrent.CancellationException.class, primary.getSuppressed()[0]);
            Assertions.assertEquals(1, primary.getSuppressed()[0].getSuppressed().length);
            Assertions.assertInstanceOf(java.util.concurrent.ExecutionException.class, primary.getSuppressed()[0].getSuppressed()[0]);
            Assertions.assertSame(secondary, primary.getSuppressed()[0].getSuppressed()[0].getCause());
        } finally {
            releaseSibling.countDown();
            completingThread.join(5000);
            service.shutdownNow();
        }
    }

    @Test
    public void testCompleteAndCollectResultWaitsBeforeClosingAndThrowing() throws Exception {
        final ExecutorService service = Executors.newFixedThreadPool(1);
        final AsyncExecutor executor = new AsyncExecutor(service);
        final CountDownLatch futureStarted = new CountDownLatch(1);
        final CountDownLatch releaseFuture = new CountDownLatch(1);
        final CountDownLatch completionReturned = new CountDownLatch(1);
        final AtomicReference<Throwable> thrown = new AtomicReference<>();
        final List<ContinuableFuture<List<Integer>>> futures = new ArrayList<>();
        final RuntimeException primary = new RuntimeException("primary");
        final Stream<Integer> stream = createStream(1, 2, 3);

        futures.add(executor.execute(() -> {
            futureStarted.countDown();
            releaseFuture.await();
            return new ArrayList<>(Arrays.asList(1, 2));
        }));

        final Thread completingThread = new Thread(() -> {
            try {
                StreamBase.completeAndCollectResult(futures, Holder.<Throwable> of(primary), ArrayList::new, List::addAll, stream, executor, executor);
            } catch (final Throwable e) { // NOSONAR
                thrown.set(e);
            } finally {
                completionReturned.countDown();
            }
        });

        try {
            Assertions.assertTrue(futureStarted.await(5, TimeUnit.SECONDS));

            final ContinuableFuture<List<Integer>> cancelled = executor.execute((java.util.concurrent.Callable<List<Integer>>) ArrayList::new);
            Assertions.assertTrue(cancelled.cancel(true));
            futures.add(0, cancelled);
            completingThread.start();

            Assertions.assertFalse(completionReturned.await(200, TimeUnit.MILLISECONDS));
            Assertions.assertFalse(stream.isClosed());
            releaseFuture.countDown();
            Assertions.assertTrue(completionReturned.await(5, TimeUnit.SECONDS));
            Assertions.assertSame(primary, thrown.get());
            Assertions.assertEquals(1, primary.getSuppressed().length);
            Assertions.assertInstanceOf(java.util.concurrent.CancellationException.class, primary.getSuppressed()[0]);
            Assertions.assertTrue(stream.isClosed());
        } finally {
            releaseFuture.countDown();
            completingThread.join(5000);
            service.shutdownNow();
        }
    }

    @Test
    public void testCompleteAndCollectResult_MergesResultsAndClosesStream() {
        AsyncExecutor executor = new AsyncExecutor(Executors.newFixedThreadPool(2));
        List<ContinuableFuture<List<Integer>>> futures = new ArrayList<>();
        futures.add(executor.execute(() -> new ArrayList<>(Arrays.asList(1, 2))));
        futures.add(executor.execute(() -> new ArrayList<>(Arrays.asList(3, 4))));

        Stream<Integer> stream = createStream(9, 8, 7);

        try {
            List<Integer> result = StreamBase.completeAndCollectResult(futures, Holder.of(null), ArrayList::new, List::addAll, stream, executor, executor);
            Assertions.assertEquals(Arrays.asList(1, 2, 3, 4), result);
            Assertions.assertThrows(IllegalStateException.class, stream::count);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testCalculateBufferedSize() {
        Assertions.assertEquals(64, StreamBase.calculateBufferedSize(1, 1));
        Assertions.assertEquals(640, StreamBase.calculateBufferedSize(10, 1));
        Assertions.assertEquals(6400, StreamBase.calculateBufferedSize(100, 1));

        Assertions.assertEquals(StreamBase.MAX_BUFFERED_SIZE, StreamBase.calculateBufferedSize(1000000, 1));
        Assertions.assertEquals(StreamBase.MAX_BUFFERED_SIZE, StreamBase.calculateBufferedSize(Integer.MAX_VALUE, 1));
        Assertions.assertEquals(StreamBase.MAX_BUFFERED_SIZE, StreamBase.calculateBufferedSize(1, Integer.MAX_VALUE));

        Assertions.assertEquals(256, StreamBase.calculateBufferedSize(1, 16));
        Assertions.assertEquals(1024, StreamBase.calculateBufferedSize(10, 64));
    }

    @Test
    public void testToInt() {
        Assertions.assertEquals(100, StreamBase.toInt(100L));
        Assertions.assertEquals(Integer.MAX_VALUE, StreamBase.toInt(Integer.MAX_VALUE));
        Assertions.assertEquals(0, StreamBase.toInt(0L));
        Assertions.assertEquals(-100, StreamBase.toInt(-100L));

        Assertions.assertEquals(Integer.MAX_VALUE, StreamBase.toInt(Long.MAX_VALUE));
        Assertions.assertEquals(Integer.MAX_VALUE, StreamBase.toInt((long) Integer.MAX_VALUE + 1));
    }

    @Test
    public void testSetStopFlagAndThrowException() {
        MutableBoolean onGoing = MutableBoolean.of(true);
        Holder<Throwable> errorHolder = Holder.of(null);

        Assertions.assertDoesNotThrow(() -> StreamBase.setStopFlagAndThrowException(errorHolder, onGoing));
        Assertions.assertFalse(onGoing.value());

        onGoing.setTrue();
        errorHolder.setValue(new RuntimeException("Test error"));
        Assertions.assertThrows(RuntimeException.class, () -> StreamBase.setStopFlagAndThrowException(errorHolder, onGoing));
        Assertions.assertFalse(onGoing.value());
        Assertions.assertNull(errorHolder.value());
    }

    @Test
    public void testThrowException() {
        Holder<Throwable> errorHolder = Holder.of(new RuntimeException("Test"));
        Assertions.assertThrows(RuntimeException.class, () -> StreamBase.throwException(errorHolder, null));

        errorHolder.setValue(new Exception("Test"));
        Assertions.assertThrows(Exception.class, () -> StreamBase.throwException(errorHolder, null));

        errorHolder.setValue(new Error("Test"));
        Assertions.assertThrows(Error.class, () -> StreamBase.throwException(errorHolder, null));
    }

    @Test
    public void testToRuntimeException() {
        RuntimeException re = new RuntimeException("test");
        RuntimeException result1 = StreamBase.toRuntimeException(re);
        Assertions.assertSame(re, result1);

        Exception e = new Exception("test");
        RuntimeException result2 = StreamBase.toRuntimeException(e);
        Assertions.assertTrue(result2 instanceof RuntimeException);
        Assertions.assertEquals(e, result2.getCause());

        Error error = new Error("test");
        Assertions.assertThrows(Error.class, () -> StreamBase.toRuntimeException(error, true));

        RuntimeException result3 = StreamBase.toRuntimeException(error, false);
        Assertions.assertTrue(result3 instanceof RuntimeException);
    }

    @Test
    public void testIsSameComparator() {
        Comparator<Integer> comp1 = Integer::compare;
        Assertions.assertTrue(StreamBase.isSameComparator(comp1, comp1));

        Assertions.assertTrue(StreamBase.isSameComparator(null, null));

        Assertions.assertTrue(StreamBase.isSameComparator(null, StreamBase.INT_COMPARATOR));
        Assertions.assertTrue(StreamBase.isSameComparator(StreamBase.INT_COMPARATOR, null));

        Assertions.assertTrue(StreamBase.isSameComparator(StreamBase.NATURAL_COMPARATOR, StreamBase.INT_COMPARATOR));
        Assertions.assertTrue(StreamBase.isSameComparator(StreamBase.INT_COMPARATOR, StreamBase.NATURAL_COMPARATOR));

        Assertions.assertFalse(StreamBase.isSameComparator(Comparator.nullsFirst(Integer::compare), Comparator.nullsFirst(String::compareTo)));
    }

    @Test
    public void testIsSameComparator_WithDifferentNaturalOrderVariants() {
        // null is considered same as NATURAL_COMPARATOR
        Assertions.assertTrue(StreamBase.isSameComparator(null, StreamBase.NATURAL_COMPARATOR));
        Assertions.assertTrue(StreamBase.isSameComparator(StreamBase.NATURAL_COMPARATOR, null));

        // Different custom comparators should not be same
        Comparator<String> comp1 = String::compareTo;
        Comparator<String> comp2 = (a, b) -> b.compareTo(a);
        Assertions.assertFalse(StreamBase.isSameComparator(comp1, comp2));
    }

    @Test
    public void testHashKey() {
        Assertions.assertEquals(StreamBase.NONE, StreamBase.hashKey(null));

        String str = "test";
        Assertions.assertEquals(str, StreamBase.hashKey(str));

        Integer num = 42;
        Assertions.assertEquals(num, StreamBase.hashKey(num));

        int[] arr = { 1, 2, 3 };
        Object hashKey = StreamBase.hashKey(arr);
        Assertions.assertTrue(hashKey instanceof Wrapper);

        Object hashKey2 = StreamBase.hashKey(arr);
        Assertions.assertEquals(hashKey, hashKey2);

        int[] arr2 = { 1, 2, 3 };
        Object hashKey3 = StreamBase.hashKey(arr2);
        Assertions.assertEquals(hashKey, hashKey3);
    }

    @Test
    public void testHashKey_NullValue() {
        // hashKey is a static method that wraps arrays
        Integer[] arr = { 1, 2, 3 };
        Object key1 = StreamBase.hashKey(arr);
        Assertions.assertNotNull(key1);
        // hashKey for non-array non-null just returns the value
        Object key2 = StreamBase.hashKey("hello");
        Assertions.assertEquals("hello", key2);
        // hashKey for null returns NONE sentinel
        Object key3 = StreamBase.hashKey(null);
        Assertions.assertNotNull(key3);
    }

    @Test
    public void testSubList() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);

        List<Integer> sub1 = StreamBase.subList(list, 1, 3);
        Assertions.assertEquals(Arrays.asList(2, 3), sub1);

        List<Integer> sub2 = StreamBase.subList(list, 2, 10);
        Assertions.assertEquals(Arrays.asList(3, 4, 5), sub2);

        List<Integer> sub3 = StreamBase.subList(list, 3, 3);
        Assertions.assertTrue(sub3.isEmpty());
    }

    @Test
    public void testSlice() {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        List<Integer> slice1 = StreamBase.slice(arr, 1, 4);
        Assertions.assertEquals(Arrays.asList(2, 3, 4), slice1);

        Integer[] emptyArr = {};
        List<Integer> slice2 = StreamBase.slice(emptyArr, 0, 0);
        Assertions.assertTrue(slice2.isEmpty());

        List<Integer> slice3 = StreamBase.slice(null, 0, 0);
        Assertions.assertTrue(slice3.isEmpty());
    }

    @Test
    public void testCheckAsyncExecutor() {
        AsyncExecutor executor1 = StreamBase.checkAsyncExecutor(null, 2);
        Assertions.assertNotNull(executor1);

        AsyncExecutor executor2 = StreamBase.checkAsyncExecutor(StreamBase.DEFAULT_ASYNC_EXECUTOR, 2);
        Assertions.assertNotNull(executor2);

        AsyncExecutor customExecutor = new AsyncExecutor(Executors.newFixedThreadPool(2));
        AsyncExecutor executor3 = StreamBase.checkAsyncExecutor(customExecutor, 2);
        Assertions.assertSame(customExecutor, executor3);
        customExecutor.shutdown();
    }

    @Test
    public void testExecuteMethods() {
        AsyncExecutor executor = new AsyncExecutor(Executors.newFixedThreadPool(2));
        List<ContinuableFuture<Void>> futures = new ArrayList<>();

        try {
            AtomicBoolean ran1 = new AtomicBoolean(false);
            AsyncExecutor result1 = StreamBase.execute(executor, 2, 0, () -> ran1.set(true));
            Assertions.assertNotNull(result1);
            Thread.sleep(100);
            Assertions.assertTrue(ran1.get());

            AsyncExecutor result2 = StreamBase.execute(executor, 2, 0, () -> "test");
            Assertions.assertNotNull(result2);

            AtomicBoolean ran3 = new AtomicBoolean(false);
            StreamBase.execute(executor, 2, 0, futures, () -> ran3.set(true));
            Assertions.assertEquals(1, futures.size());
            futures.get(0).get();
            Assertions.assertTrue(ran3.get());

        } catch (Exception e) {
            Assertions.fail("Unexpected exception: " + e);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testCanBeSequential() {
        Assertions.assertTrue(StreamBase.canBeSequential(0));
        Assertions.assertTrue(StreamBase.canBeSequential(1));
        Assertions.assertFalse(StreamBase.canBeSequential(2));
        Assertions.assertFalse(StreamBase.canBeSequential(10));

        Assertions.assertTrue(StreamBase.canBeSequential(2, 0, 1));
        Assertions.assertFalse(StreamBase.canBeSequential(2, 0, 2));
        Assertions.assertTrue(StreamBase.canBeSequential(1, 0, 10));
    }

    @Test
    public void testLocalRunnable() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> LocalRunnable.wrap((Runnable) null));

        LocalRunnable localRunnable = LocalRunnable.wrap(Fn.jr(() -> {
        }));
        LocalRunnable wrapped2 = LocalRunnable.wrap(localRunnable);
        Assertions.assertSame(localRunnable, wrapped2);

        AtomicBoolean called = new AtomicBoolean(false);
        Runnable runnable = () -> called.set(true);
        LocalRunnable wrapped3 = LocalRunnable.wrap(runnable);
        wrapped3.run();
        Assertions.assertTrue(called.get());

        called.set(false);
        wrapped3.run();
        Assertions.assertFalse(called.get());

        AtomicBoolean closed = new AtomicBoolean(false);
        AutoCloseable closeable = () -> closed.set(true);
        LocalRunnable wrapped4 = LocalRunnable.wrap(closeable);
        wrapped4.run();
        Assertions.assertTrue(closed.get());
    }

    @Test
    public void testStaticFields() {
        Assertions.assertNotNull(StreamBase.NONE);
        Assertions.assertNotNull(StreamBase.RAND);
        Assertions.assertNotNull(StreamBase.NULL_CHAR_ARRAY);
        Assertions.assertArrayEquals("null".toCharArray(), StreamBase.NULL_CHAR_ARRAY);
        Assertions.assertNotNull(StreamBase.ELEMENT_SEPARATOR_CHAR_ARRAY);

        Assertions.assertTrue(StreamBase.MAX_WAIT_TIME_FOR_QUEUE_OFFER > 0);
        Assertions.assertTrue(StreamBase.MAX_WAIT_TIME_FOR_QUEUE_POLL > 0);
        Assertions.assertTrue(StreamBase.MAX_BUFFERED_SIZE > 0);
        Assertions.assertTrue(StreamBase.DEFAULT_BUFFERED_SIZE_PER_ITERATOR > 0);
        Assertions.assertTrue(StreamBase.BATCH_SIZE_FOR_FLUSH > 0);

        Assertions.assertNotNull(StreamBase.NULL_MIN_COMPARATOR);
        Assertions.assertNotNull(StreamBase.NULL_MAX_COMPARATOR);
        Assertions.assertNotNull(StreamBase.NATURAL_COMPARATOR);
        Assertions.assertNotNull(StreamBase.REVERSED_COMPARATOR);

        Assertions.assertEquals(-1, StreamBase.CHAR_COMPARATOR.compare('a', 'b'));
        Assertions.assertEquals(-1, StreamBase.BYTE_COMPARATOR.compare((byte) 1, (byte) 2));
        Assertions.assertEquals(-1, StreamBase.INT_COMPARATOR.compare(1, 2));
        Assertions.assertEquals(-1, StreamBase.LONG_COMPARATOR.compare(1L, 2L));
        Assertions.assertEquals(-1, StreamBase.FLOAT_COMPARATOR.compare(1.0f, 2.0f));
        Assertions.assertEquals(-1, StreamBase.DOUBLE_COMPARATOR.compare(1.0, 2.0));
    }

    @Test
    public void testCollectingCombiner() {
        BiConsumer<Object, Object> combiner = StreamBase.collectingCombiner;

        List<Integer> list1 = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<Integer> list2 = new ArrayList<>(Arrays.asList(4, 5, 6));
        combiner.accept(list1, list2);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), list1);

        Map<String, Integer> map1 = new HashMap<>();
        map1.put("a", 1);
        Map<String, Integer> map2 = new HashMap<>();
        map2.put("b", 2);
        combiner.accept(map1, map2);
        Assertions.assertEquals(2, map1.size());
        Assertions.assertEquals(1, map1.get("a"));
        Assertions.assertEquals(2, map1.get("b"));

        StringBuilder sb1 = new StringBuilder("Hello");
        StringBuilder sb2 = new StringBuilder(" World");
        combiner.accept(sb1, sb2);
        Assertions.assertEquals("Hello World", sb1.toString());

        BooleanList boolList1 = BooleanList.of(true, false);
        BooleanList boolList2 = BooleanList.of(false, true);
        combiner.accept(boolList1, boolList2);
        Assertions.assertEquals(4, boolList1.size());

        IntList intList1 = IntList.of(1, 2);
        IntList intList2 = IntList.of(3, 4);
        combiner.accept(intList1, intList2);
        Assertions.assertEquals(4, intList1.size());
    }

    @Test
    public void testCollectingCombiner_MultisetAndMultimap() {
        BiConsumer<Object, Object> combiner = StreamBase.collectingCombiner;

        Multiset<String> multiset1 = Multiset.of("a", "a");
        Multiset<String> multiset2 = Multiset.of("a", "b");
        combiner.accept(multiset1, multiset2);
        Assertions.assertEquals(3, multiset1.count("a"));
        Assertions.assertEquals(1, multiset1.count("b"));

        ListMultimap<String, Integer> multimap1 = N.newListMultimap();
        multimap1.put("x", 1);
        ListMultimap<String, Integer> multimap2 = N.newListMultimap();
        multimap2.put("x", 2);
        multimap2.put("y", 3);
        combiner.accept(multimap1, multimap2);
        Assertions.assertEquals(Arrays.asList(1, 2), multimap1.get("x"));
        Assertions.assertEquals(Arrays.asList(3), multimap1.get("y"));

        CharList charList1 = CharList.of('a');
        CharList charList2 = CharList.of('b', 'c');
        combiner.accept(charList1, charList2);
        Assertions.assertEquals(3, charList1.size());
    }

    @Test
    public void testNewPrimitiveStreams_SubRange() {
        Assertions.assertArrayEquals(new byte[] { 2, 3 }, createStream(1).newStream(new byte[] { 1, 2, 3, 4 }, 1, 3, false).toArray());
        Assertions.assertArrayEquals(new short[] { 3, 4 }, createStream(1).newStream(new short[] { 2, 3, 4, 5 }, 1, 3, false).toArray());
        Assertions.assertArrayEquals(new int[] { 4, 5 }, createStream(1).newStream(new int[] { 3, 4, 5, 6 }, 1, 3, false).toArray());
        Assertions.assertArrayEquals(new long[] { 5L, 6L }, createStream(1).newStream(new long[] { 4L, 5L, 6L, 7L }, 1, 3, false).toArray());
    }

    // TODO: Remaining StreamBase coverage gaps are private array/list optimization paths and async collector internals that are not directly observable through the stable public API.

    @Test
    public void testLocalRunnable_ConcurrentRun_InvokesAtMostOnce() throws Exception {
        // Wrap a Runnable and concurrently invoke run() from many threads using a CountDownLatch
        // to start them simultaneously. The underlying handler must execute AT MOST ONCE.
        final int threadCount = 32;
        final AtomicInteger invocationCount = new AtomicInteger(0);
        final Runnable handler = invocationCount::incrementAndGet;
        final LocalRunnable wrapped = LocalRunnable.wrap(handler);

        final java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(threadCount);
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        wrapped.run();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // Release all threads simultaneously
            startLatch.countDown();
            Assertions.assertTrue(doneLatch.await(10, java.util.concurrent.TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        Assertions.assertEquals(1, invocationCount.get(), "Underlying handler must be invoked exactly once across concurrent run() calls");

        // Subsequent run() calls remain a no-op
        wrapped.run();
        Assertions.assertEquals(1, invocationCount.get());
    }

    @Test
    public void testLocalRunnable_ConcurrentRun_AutoCloseableInvokesAtMostOnce() throws Exception {
        final int threadCount = 32;
        final AtomicInteger closeCount = new AtomicInteger(0);
        final AutoCloseable closeable = closeCount::incrementAndGet;
        final LocalRunnable wrapped = LocalRunnable.wrap(closeable);

        final java.util.concurrent.CountDownLatch startLatch = new java.util.concurrent.CountDownLatch(1);
        final java.util.concurrent.CountDownLatch doneLatch = new java.util.concurrent.CountDownLatch(threadCount);
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        try {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        wrapped.run();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            Assertions.assertTrue(doneLatch.await(10, java.util.concurrent.TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
        }

        Assertions.assertEquals(1, closeCount.get(), "Underlying AutoCloseable must be closed exactly once across concurrent run() calls");
    }

    @Test
    public void testCloseRunsRemainingHandlersAfterError() {
        final AtomicInteger closed = new AtomicInteger();
        final AssertionError failure = new AssertionError("first");

        final AssertionError thrown = Assertions.assertThrows(AssertionError.class, () -> Stream.of(1).onClose(() -> {
            throw failure;
        }).onClose(closed::incrementAndGet).close());
        Assertions.assertSame(failure, thrown);
        Assertions.assertEquals(1, closed.get());
    }

    @Test
    public void testGroupingCachesOnlySuccessfullyReadLookahead() {
        for (String type : Arrays.asList("Object", "Byte", "Char", "Short", "Int", "Long", "Float", "Double")) {
            int operationCount = type.equals("Object") ? 9 : 5;
            for (int operation = 0; operation < operationCount; operation++) {
                for (int failureKind = 0; failureKind < 3; failureKind++) {
                    for (int failureAt : new int[] { 2, 3 }) {
                        for (boolean mergeAll : new boolean[] { false, true }) {
                            final int kind = failureKind;
                            final AtomicInteger failures = new AtomicInteger();
                            final String context = type + "/" + operation + "/" + kind + "/" + failureAt + "/" + mergeAll;
                            java.util.Iterator<Integer> source = new java.util.Iterator<>() {
                                private int next = 1;

                                private void fail(int phase) {
                                    if (kind == phase && next == failureAt && failures.getAndIncrement() == 0) {
                                        throw new IllegalStateException("source lookahead");
                                    }
                                }

                                @Override
                                public boolean hasNext() {
                                    fail(0);
                                    return next <= 4;
                                }

                                @Override
                                public Integer next() {
                                    fail(1);
                                    if (next > 4) {
                                        throw new java.util.NoSuchElementException();
                                    }
                                    return next++;
                                }
                            };
                            java.util.function.BiPredicate<Integer, Integer> predicate = (left, right) -> {
                                if (kind == 2 && right == failureAt && failures.getAndIncrement() == 0) {
                                    throw new IllegalStateException("predicate lookahead");
                                }
                                return mergeAll;
                            };

                            try (Stream<Integer> grouped = groupingFailureStream(type, operation, source, predicate)) {
                                java.util.Iterator<Integer> iterator = grouped.iterator();
                                if (!mergeAll && failureAt == 3) {
                                    Assertions.assertEquals(1, iterator.next(), context);
                                }
                                Assertions.assertThrows(IllegalStateException.class, iterator::next, context);
                                List<Integer> remaining = new ArrayList<>();
                                while (iterator.hasNext()) {
                                    Assertions.assertTrue(iterator.hasNext(), context);
                                    remaining.add(iterator.next());
                                }
                                List<Integer> expected = mergeAll ? Arrays.asList(failureAt) : failureAt == 2 ? Arrays.asList(2, 3, 4) : Arrays.asList(3, 4);
                                Assertions.assertEquals(expected, remaining, context);
                                Assertions.assertThrows(java.util.NoSuchElementException.class, iterator::next, context);
                            }
                        }
                    }
                }
            }
        }
    }

    private static Stream<Integer> groupingFailureStream(String type, int operation, java.util.Iterator<Integer> source,
            java.util.function.BiPredicate<Integer, Integer> predicate) {
        switch (type) {
            case "Object": {
                Stream<Integer> stream = Stream.of(source);
                com.landawn.abacus.util.function.BiPredicate<Integer, Integer> pair = predicate::test;
                com.landawn.abacus.util.function.TriPredicate<Integer, Integer, Integer> triple = (first, previous, next) -> predicate.test(previous, next);
                switch (operation) {
                    case 0:
                        return stream.rangeMap(pair, (first, last) -> first);
                    case 1:
                        return stream.collapse(pair).map(values -> values.get(0));
                    case 2:
                        return stream.collapse(pair, (first, next) -> first);
                    case 3:
                        return stream.collapse(triple, (first, next) -> first);
                    case 4:
                        return stream.collapse(pair, (Integer) null, (first, next) -> first == null ? next : first);
                    case 5:
                        return stream.collapse(triple, (Integer) null, (first, next) -> first == null ? next : first);
                    case 6:
                        return stream.collapse(pair, java.util.stream.Collectors.toList()).map(values -> values.get(0));
                    case 7:
                        return stream.collapse(triple, java.util.stream.Collectors.toList()).map(values -> values.get(0));
                    case 8:
                        return stream.collapse(triple).map(values -> values.get(0));
                    default:
                        throw new AssertionError(operation);
                }
            }
            case "Byte": {
                ByteStream stream = Stream.of(source).mapToByte(value -> (byte) value.intValue());
                com.landawn.abacus.util.function.ByteBiPredicate pair = (left, right) -> predicate.test((int) left, (int) right);
                com.landawn.abacus.util.function.ByteTriPredicate triple = (first, previous, next) -> predicate.test((int) previous, (int) next);
                switch (operation) {
                    case 0:
                        return stream.rangeMap(pair, (first, last) -> first).mapToObj(value -> (int) value);
                    case 1:
                        return stream.rangeMapToObj(pair, (first, last) -> (int) first);
                    case 2:
                        return stream.collapse(pair).map(values -> (int) values.get(0));
                    case 3:
                        return stream.collapse(pair, (first, next) -> first).mapToObj(value -> (int) value);
                    case 4:
                        return stream.collapse(triple, (first, next) -> first).mapToObj(value -> (int) value);
                    default:
                        throw new AssertionError(operation);
                }
            }
            case "Char": {
                CharStream stream = Stream.of(source).mapToChar(value -> (char) value.intValue());
                com.landawn.abacus.util.function.CharBiPredicate pair = (left, right) -> predicate.test((int) left, (int) right);
                com.landawn.abacus.util.function.CharTriPredicate triple = (first, previous, next) -> predicate.test((int) previous, (int) next);
                switch (operation) {
                    case 0:
                        return stream.rangeMap(pair, (first, last) -> first).mapToObj(value -> (int) value);
                    case 1:
                        return stream.rangeMapToObj(pair, (first, last) -> (int) first);
                    case 2:
                        return stream.collapse(pair).map(values -> (int) values.get(0));
                    case 3:
                        return stream.collapse(pair, (first, next) -> first).mapToObj(value -> (int) value);
                    case 4:
                        return stream.collapse(triple, (first, next) -> first).mapToObj(value -> (int) value);
                    default:
                        throw new AssertionError(operation);
                }
            }
            case "Short": {
                ShortStream stream = Stream.of(source).mapToShort(value -> (short) value.intValue());
                com.landawn.abacus.util.function.ShortBiPredicate pair = (left, right) -> predicate.test((int) left, (int) right);
                com.landawn.abacus.util.function.ShortTriPredicate triple = (first, previous, next) -> predicate.test((int) previous, (int) next);
                switch (operation) {
                    case 0:
                        return stream.rangeMap(pair, (first, last) -> first).mapToObj(value -> (int) value);
                    case 1:
                        return stream.rangeMapToObj(pair, (first, last) -> (int) first);
                    case 2:
                        return stream.collapse(pair).map(values -> (int) values.get(0));
                    case 3:
                        return stream.collapse(pair, (first, next) -> first).mapToObj(value -> (int) value);
                    case 4:
                        return stream.collapse(triple, (first, next) -> first).mapToObj(value -> (int) value);
                    default:
                        throw new AssertionError(operation);
                }
            }
            case "Int": {
                IntStream stream = Stream.of(source).mapToInt(value -> (int) value.intValue());
                com.landawn.abacus.util.function.IntBiPredicate pair = (left, right) -> predicate.test((int) left, (int) right);
                com.landawn.abacus.util.function.IntTriPredicate triple = (first, previous, next) -> predicate.test((int) previous, (int) next);
                switch (operation) {
                    case 0:
                        return stream.rangeMap(pair, (first, last) -> first).mapToObj(value -> (int) value);
                    case 1:
                        return stream.rangeMapToObj(pair, (first, last) -> (int) first);
                    case 2:
                        return stream.collapse(pair).map(values -> (int) values.get(0));
                    case 3:
                        return stream.collapse(pair, (first, next) -> first).mapToObj(value -> (int) value);
                    case 4:
                        return stream.collapse(triple, (first, next) -> first).mapToObj(value -> (int) value);
                    default:
                        throw new AssertionError(operation);
                }
            }
            case "Long": {
                LongStream stream = Stream.of(source).mapToLong(value -> (long) value.intValue());
                com.landawn.abacus.util.function.LongBiPredicate pair = (left, right) -> predicate.test((int) left, (int) right);
                com.landawn.abacus.util.function.LongTriPredicate triple = (first, previous, next) -> predicate.test((int) previous, (int) next);
                switch (operation) {
                    case 0:
                        return stream.rangeMap(pair, (first, last) -> first).mapToObj(value -> (int) value);
                    case 1:
                        return stream.rangeMapToObj(pair, (first, last) -> (int) first);
                    case 2:
                        return stream.collapse(pair).map(values -> (int) values.get(0));
                    case 3:
                        return stream.collapse(pair, (first, next) -> first).mapToObj(value -> (int) value);
                    case 4:
                        return stream.collapse(triple, (first, next) -> first).mapToObj(value -> (int) value);
                    default:
                        throw new AssertionError(operation);
                }
            }
            case "Float": {
                FloatStream stream = Stream.of(source).mapToFloat(value -> (float) value.intValue());
                com.landawn.abacus.util.function.FloatBiPredicate pair = (left, right) -> predicate.test((int) left, (int) right);
                com.landawn.abacus.util.function.FloatTriPredicate triple = (first, previous, next) -> predicate.test((int) previous, (int) next);
                switch (operation) {
                    case 0:
                        return stream.rangeMap(pair, (first, last) -> first).mapToObj(value -> (int) value);
                    case 1:
                        return stream.rangeMapToObj(pair, (first, last) -> (int) first);
                    case 2:
                        return stream.collapse(pair).map(values -> (int) values.get(0));
                    case 3:
                        return stream.collapse(pair, (first, next) -> first).mapToObj(value -> (int) value);
                    case 4:
                        return stream.collapse(triple, (first, next) -> first).mapToObj(value -> (int) value);
                    default:
                        throw new AssertionError(operation);
                }
            }
            case "Double": {
                DoubleStream stream = Stream.of(source).mapToDouble(value -> (double) value.intValue());
                com.landawn.abacus.util.function.DoubleBiPredicate pair = (left, right) -> predicate.test((int) left, (int) right);
                com.landawn.abacus.util.function.DoubleTriPredicate triple = (first, previous, next) -> predicate.test((int) previous, (int) next);
                switch (operation) {
                    case 0:
                        return stream.rangeMap(pair, (first, last) -> first).mapToObj(value -> (int) value);
                    case 1:
                        return stream.rangeMapToObj(pair, (first, last) -> (int) first);
                    case 2:
                        return stream.collapse(pair).map(values -> (int) values.get(0));
                    case 3:
                        return stream.collapse(pair, (first, next) -> first).mapToObj(value -> (int) value);
                    case 4:
                        return stream.collapse(triple, (first, next) -> first).mapToObj(value -> (int) value);
                    default:
                        throw new AssertionError(operation);
                }
            }
            default:
                throw new AssertionError(type);
        }
    }

    /**
     * The deadlock-avoidance fallback pool must be OWNED by its {@code AsyncExecutor}, so that its threads
     * are daemon threads and {@code shutdownTempExecutor} can actually shut it down.
     *
     * <p>It used to be built as {@code new AsyncExecutor(Executors.newFixedThreadPool(n))}. That constructor
     * records {@code ownsExecutor = false}, so {@code AsyncExecutor.shutdown()} never shut the wrapped pool
     * down, and {@code Executors.defaultThreadFactory()} threads are <b>non-daemon</b> with no core timeout.
     * A process that once tripped this path could therefore never exit: measured with two nested
     * {@code parallel()} levels, the pipeline finished in ~600 ms and the JVM then had to be killed after
     * 60 s with 32 surviving non-daemon threads.
     *
     * <p>The fallback only triggers above {@code CORE_THREAD_POOL_SIZE - RESERVED_POOL_SIZE} active threads
     * (240 on a 16-core machine), so rather than spawning that many real threads the test raises the
     * internal counter directly and restores it afterwards.
     */
    @Test
    public void testFallbackAsyncExecutorUsesDaemonThreads() throws Exception {
        final java.lang.reflect.Field field = StreamBase.class.getDeclaredField("ACTIVE_THREAD_NUM");
        field.setAccessible(true);
        final java.util.concurrent.atomic.AtomicInteger activeThreadNum = (java.util.concurrent.atomic.AtomicInteger) field.get(null);

        final int bump = StreamBase.CORE_THREAD_POOL_SIZE;
        activeThreadNum.addAndGet(bump);

        try {
            final com.landawn.abacus.util.AsyncExecutor fallback = StreamBase.checkAsyncExecutor(null, 2);

            Assertions.assertNotSame(StreamBase.DEFAULT_ASYNC_EXECUTOR, fallback, "the fallback pool should have been created");

            final java.util.concurrent.atomic.AtomicBoolean daemon = new java.util.concurrent.atomic.AtomicBoolean();
            final java.util.concurrent.CountDownLatch done = new java.util.concurrent.CountDownLatch(1);

            fallback.execute(() -> {
                daemon.set(Thread.currentThread().isDaemon());
                done.countDown();
            });

            Assertions.assertTrue(done.await(30, java.util.concurrent.TimeUnit.SECONDS), "the fallback pool never ran the task");
            Assertions.assertTrue(daemon.get(), "the fallback pool must use daemon threads, otherwise the JVM cannot exit");

            StreamBase.shutdownTempExecutor(fallback);
        } finally {
            activeThreadNum.addAndGet(-bump);
        }
    }

    /**
     * An {@code Error} raised by user code inside a parallel worker must reach the caller <b>as an Error</b>.
     *
     * <p>{@code StreamBase.toRuntimeException(Throwable)} used to default {@code throwIfItIsError} to
     * {@code false}, so an {@code AssertionError} or {@code OutOfMemoryError} from a mapper arrived wrapped in
     * a {@code RuntimeException} and was swallowed by an ordinary {@code catch (Exception)}, silently
     * truncating the stream. Sequential pipelines and parallel {@code forEach} always propagated it
     * correctly; these four terminals did not.
     */
    @Test
    public void testParallelPipelinePropagatesAnErrorAsAnError() {
        final List<Integer> data = new ArrayList<>();

        for (int i = 0; i < 8; i++) {
            data.add(i);
        }

        Assertions.assertThrows(AssertionError.class, () -> Stream.of(data).parallel(4).map(x -> {
            throw new AssertionError("boom-" + x);
        }).toList());

        Assertions.assertThrows(AssertionError.class, () -> Stream.of(data.iterator()).parallel(4).map(x -> {
            throw new AssertionError("boom-" + x);
        }).count());

        Assertions.assertThrows(AssertionError.class, () -> Stream.of(data).parallel(4).filter(x -> {
            throw new AssertionError("boom-" + x);
        }).toList());

        Assertions.assertThrows(AssertionError.class, () -> IntStream.range(0, 8).parallel(4).map(x -> {
            throw new AssertionError("boom-" + x);
        }).sum());

        // an ordinary exception must still arrive unchanged
        Assertions.assertThrows(IllegalStateException.class, () -> Stream.of(data).parallel(4).map(x -> {
            throw new IllegalStateException("normal-" + x);
        }).toList());

        // and a healthy parallel pipeline is unaffected
        Assertions.assertEquals(8, Stream.of(data).parallel(4).map(x -> x * 2).toList().size());
    }

    // ------------------------------------------------------------------------------------------------------
    // Stream review 2026-09-09 (pass B) - non-RejectedExecutionException failures during worker submission
    // ------------------------------------------------------------------------------------------------------

    /**
     * The submission helper caught only {@code RejectedExecutionException}, so any other throw from an
     * {@code Executor.execute(...)} escaped before {@code completeAndShutdownTempExecutor(...)}: already-accepted
     * workers were never awaited and the stream was never closed. Reachable without a hostile executor -
     * {@code StreamBase}'s own pool installs a handler that throws a plain {@code RuntimeException}, and its JVM
     * shutdown hook shuts that pool down, so a parallel stream run from a user shutdown hook takes this path.
     *
     * <p>The sibling {@code testParallelTerminalsCloseWhenExecutorRejectsSubmission} covers the
     * {@code RejectedExecutionException} case, which was always handled.
     */
    @Test
    public void testParallelTerminalsCloseWhenSubmissionFailsWithANonRejectedException() {
        for (final boolean iteratorBacked : new boolean[] { false, true }) {
            final AtomicInteger closeCount = new AtomicInteger();
            final java.util.concurrent.Executor executor = command -> {
                throw new IllegalStateException("submission failed");
            };

            Assertions.assertThrows(IllegalStateException.class,
                    () -> (iteratorBacked ? Stream.of(Arrays.asList(1, 2, 3).iterator()) : Stream.of(1, 2, 3)).onClose(closeCount::incrementAndGet)
                            .parallel(2, executor)
                            .forEach(value -> {
                            }));

            Assertions.assertEquals(1, closeCount.get(), "the stream must be closed even when submission fails"); // used to be 0
        }
    }

}
