/*
 * Copyright (C) 2016, 2017, 2018, 2019 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util.stream;

import java.io.Serial;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Spliterator;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.annotation.LazyEvaluation;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.BiMap;
import com.landawn.abacus.util.BooleanList;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.ByteList;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.Comparators;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.DoubleList;
import com.landawn.abacus.util.ExceptionUtil;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.IndexedByte;
import com.landawn.abacus.util.IndexedChar;
import com.landawn.abacus.util.IndexedDouble;
import com.landawn.abacus.util.IndexedFloat;
import com.landawn.abacus.util.IndexedInt;
import com.landawn.abacus.util.IndexedLong;
import com.landawn.abacus.util.IndexedShort;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.IntList;
import com.landawn.abacus.util.InternalUtil;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.LongList;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Numbers;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.ShortList;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Wrapper;
import com.landawn.abacus.util.cs;
import com.landawn.abacus.util.u.Optional;

/**
 * Abstract base class for stream implementations, providing common functionality and utilities
 * for both sequential and parallel stream operations.
 *
 * <p>This class serves as the foundation for all stream types in the framework, managing:
 * <ul>
 *   <li>Stream lifecycle and close handlers for resource management</li>
 *   <li>Parallel execution with configurable thread pools and executors</li>
 *   <li>Sorted stream state tracking with comparator support</li>
 *   <li>Common terminal and intermediate operations</li>
 * </ul>
 *
 * <p>The class uses a lazy evaluation model, deferring computation until a terminal operation
 * is invoked. It supports both sequential and parallel execution modes, with sophisticated
 * thread pool management to avoid deadlocks and optimize resource usage.
 *
 * <p>Thread Pool Management:
 * This class maintains a shared thread pool for parallel operations. The pool is sized based
 * on platform capabilities (Android vs non-Android) and automatically creates temporary executors
 * when the default pool is heavily utilized to prevent deadlocks.
 *
 * <p>Resource Management:
 * Streams implement AutoCloseable and maintain a chain of close handlers that are executed
 * when the stream is closed, either explicitly or after a terminal operation completes.
 *
 * @param <T> the type of stream elements
 * @param <A> the type of array for toArray operations
 * @param <P> the type of predicate used in filtering operations (e.g., {@code Predicate<T>} for object streams)
 * @param <C> the type of consumer used in consumption operations (e.g., {@code Consumer<T>} for object streams)
 * @param <OT> the type of Optional returned by operations like {@code first()}, {@code last()}
 * @param <IT> the type of indexed element returned by {@code indexed()} operation
 * @param <ITER> the type of iterator returned by {@code iterator()} method
 * @param <S> the specific stream implementation type (self-type for fluent API)
 */
@SuppressWarnings({ "java:S6539" })
@LazyEvaluation
abstract class StreamBase<T, A, P, C, OT, IT, ITER extends Iterator<T>, S extends BaseStream<T, A, P, C, OT, IT, ITER, S>>
        implements BaseStream<T, A, P, C, OT, IT, ITER, S> {
    static final Logger logger = LoggerFactory.getLogger(StreamBase.class);

    static final Object NONE = ClassUtil.newNullSentinel();

    static final Random RAND = new SecureRandom();

    static final int DEFAULT_CHARACTERISTICS_OBJ_JDK_STREAM = Spliterator.ORDERED | Spliterator.IMMUTABLE;
    static final int DEFAULT_CHARACTERISTICS_PRIMITIVE_JDK_STREAM = Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL;

    static final String ERROR_MSG_FOR_NO_SUCH_EX = InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX;

    /**
     * Char array with value {@code ['n', 'u', 'l', 'l']}.
     */
    static final char[] NULL_CHAR_ARRAY = Strings.NULL.toCharArray();

    static final char[] ELEMENT_SEPARATOR_CHAR_ARRAY = Strings.ELEMENT_SEPARATOR.toCharArray();

    static final int MAX_WAIT_TIME_FOR_QUEUE_OFFER = 9; // unit is milliseconds
    static final int MAX_WAIT_TIME_FOR_QUEUE_POLL = 7; // unit is milliseconds

    static final int MAX_WAIT_TIME_FOR_QUEUE_OFFER_FOR_ADD_SUBSCRIBER = 30_000; // unit is milliseconds

    static final int MAX_BUFFERED_SIZE = 10240;
    static final int DEFAULT_BUFFERED_SIZE_PER_ITERATOR = 64;

    static final int BATCH_SIZE_FOR_FLUSH = 1000;

    static final int MAX_SIZE_FOR_PRINTLN = 1000;

    @SuppressWarnings("rawtypes")
    static final Comparator NULL_MIN_COMPARATOR = Comparators.nullsFirst();

    @SuppressWarnings("rawtypes")
    static final Comparator NULL_MAX_COMPARATOR = Comparators.nullsLast();

    @SuppressWarnings("rawtypes")
    static final Comparator NATURAL_COMPARATOR = Comparators.naturalOrder();

    @SuppressWarnings("rawtypes")
    static final Comparator REVERSED_COMPARATOR = Comparators.reverseOrder();

    static final Comparator<Character> CHAR_COMPARATOR = Character::compare;

    static final Comparator<Byte> BYTE_COMPARATOR = Byte::compare;

    static final Comparator<Short> SHORT_COMPARATOR = Short::compare;

    static final Comparator<Integer> INT_COMPARATOR = Integer::compare;

    static final Comparator<Long> LONG_COMPARATOR = Long::compare;

    static final Comparator<Float> FLOAT_COMPARATOR = Float::compare;

    static final Comparator<Double> DOUBLE_COMPARATOR = Double::compare;

    static final Comparator<IndexedByte> INDEXED_BYTE_COMPARATOR = (a, b) -> N.compare(a.longIndex(), b.longIndex());

    static final Comparator<IndexedChar> INDEXED_CHAR_COMPARATOR = (a, b) -> N.compare(a.longIndex(), b.longIndex());

    static final Comparator<IndexedShort> INDEXED_SHORT_COMPARATOR = (a, b) -> N.compare(a.longIndex(), b.longIndex());

    static final Comparator<IndexedInt> INDEXED_INT_COMPARATOR = (a, b) -> N.compare(a.longIndex(), b.longIndex());

    static final Comparator<IndexedLong> INDEXED_LONG_COMPARATOR = (a, b) -> N.compare(a.longIndex(), b.longIndex());

    static final Comparator<IndexedFloat> INDEXED_FLOAT_COMPARATOR = (a, b) -> N.compare(a.longIndex(), b.longIndex());

    static final Comparator<IndexedDouble> INDEXED_DOUBLE_COMPARATOR = (a, b) -> N.compare(a.longIndex(), b.longIndex());

    static final Comparator<Indexed<?>> INDEXED_COMPARATOR = (a, b) -> N.compare(a.longIndex(), b.longIndex());

    static final BiMap<Class<?>, Comparator<?>> DEFAULT_COMPARATOR_MAP = new BiMap<>();

    static {
        DEFAULT_COMPARATOR_MAP.put(char.class, CHAR_COMPARATOR);
        DEFAULT_COMPARATOR_MAP.put(byte.class, BYTE_COMPARATOR);
        DEFAULT_COMPARATOR_MAP.put(short.class, SHORT_COMPARATOR);
        DEFAULT_COMPARATOR_MAP.put(int.class, INT_COMPARATOR);
        DEFAULT_COMPARATOR_MAP.put(long.class, LONG_COMPARATOR);
        DEFAULT_COMPARATOR_MAP.put(float.class, FLOAT_COMPARATOR);
        DEFAULT_COMPARATOR_MAP.put(double.class, DOUBLE_COMPARATOR);
        DEFAULT_COMPARATOR_MAP.put(Object.class, NATURAL_COMPARATOR);
    }

    static final Map<Class<?>, Integer> CLS_SEQ_MAP = new BiMap<>();

    static {
        int idx = 0;
        CLS_SEQ_MAP.put(boolean[].class, idx++); // 0
        CLS_SEQ_MAP.put(char[].class, idx++);
        CLS_SEQ_MAP.put(byte[].class, idx++);
        CLS_SEQ_MAP.put(short[].class, idx++);
        CLS_SEQ_MAP.put(int[].class, idx++);
        CLS_SEQ_MAP.put(long[].class, idx++);
        CLS_SEQ_MAP.put(float[].class, idx++);
        CLS_SEQ_MAP.put(double[].class, idx++); // 7
        CLS_SEQ_MAP.put(BooleanList.class, idx++); // 8
        CLS_SEQ_MAP.put(CharList.class, idx++);
        CLS_SEQ_MAP.put(ByteList.class, idx++);
        CLS_SEQ_MAP.put(ShortList.class, idx++);
        CLS_SEQ_MAP.put(IntList.class, idx++);
        CLS_SEQ_MAP.put(LongList.class, idx++);
        CLS_SEQ_MAP.put(FloatList.class, idx++);
        CLS_SEQ_MAP.put(DoubleList.class, idx++); // 15
    }

    static final LocalRunnable EMPTY_CLOSE_HANDLER = () -> {
        // do nothing.
    };

    static final int CORE_THREAD_POOL_SIZE = IOUtil.IS_PLATFORM_ANDROID //
            ? Math.max(64, IOUtil.CPU_CORES * 8) //
            : Math.max(256, IOUtil.CPU_CORES * 16);

    static final int MAX_THREAD_NUM_PER_OPERATION = IOUtil.IS_PLATFORM_ANDROID //
            ? Math.min(16, IOUtil.CPU_CORES) //
            : Math.min(64, IOUtil.CPU_CORES * 8);

    static final int DEFAULT_MAX_THREAD_NUM = Math.min(MAX_THREAD_NUM_PER_OPERATION, IOUtil.CPU_CORES);
    static final int DEFAULT_READING_THREAD_NUM = Math.min(MAX_THREAD_NUM_PER_OPERATION, IOUtil.CPU_CORES);

    static final int RESERVED_POOL_SIZE = CORE_THREAD_POOL_SIZE / 16; // To reduce the chance of deadlock.

    // static final long MAX_WAIT_TO_BREAK_FOR_DEAD_LOCK = 6 * 60 * 1000; // 6 minutes

    static final Splitor DEFAULT_SPLITOR = Splitor.ITERATOR;

    static final int MAX_POOLED_EXECUTOR_SIZE_FOR_VIRTUAL_THREAD = IOUtil.CPU_CORES;

    private static final AtomicInteger ACTIVE_THREAD_NUM = new AtomicInteger();

    static final AsyncExecutor DEFAULT_ASYNC_EXECUTOR;

    static {
        // TODO dead lock if the total thread number started by this stream and its upstream is bigger than CORE_THREAD_POOL_SIZE(or CORE_THREAD_POOL_SIZE_FOR_ANDROID for Android).
        // If the total thread number started by this stream and its down stream is big, please specified its owner {@code Executor} by {@code parallel(..., Executor)}.

        // UPDATE: this deadlock problem has been resolved by using BaseStream.execute(...)

        // Core pool size and maximum pool size must be the same; otherwise it hangs.
        //    final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(//
        //            N.max(64, IOUtil.CPU_CORES * 8), // coreThreadPoolSize
        //            MAX_THREAD_POOL_SIZE, // maxThreadPoolSize
        //            180L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

        // final ThreadPoolExecutor threadPoolExecutor = Executors.newFixedThreadPool(CORE_THREAD_POOL_SIZE);

        final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(//
                CORE_THREAD_POOL_SIZE, // coreThreadPoolSize
                CORE_THREAD_POOL_SIZE, // maxThreadPoolSize
                180L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());

        threadPoolExecutor.setRejectedExecutionHandler((r, executor) -> {
            //    // This should not be called.
            //    if (activeThreadNum.get() > 0) {
            //        activeThreadNum.decrementAndGet();
            //    }

            // Since it should not happen, throw exception.
            throw new RuntimeException("No task should be rejected by thread pool. This is an unexpected behavior");
        });

        threadPoolExecutor.allowCoreThreadTimeOut(true);

        DEFAULT_ASYNC_EXECUTOR = new AsyncExecutor(threadPoolExecutor) {
            @Override
            public ContinuableFuture<Void> execute(final Throwables.Runnable<? extends Exception> command) {
                //    if (threadPoolExecutor.getActiveCount() >= MAX_THREAD_POOL_SIZE) {
                //        throw new RejectedExecutionException("Task is rejected due to exceed max thread pool size: " + MAX_THREAD_POOL_SIZE);
                //    }

                //    if (logger.isDebugEnabled()) {
                //        logger.debug("######: ActiveTaskCount-Executor: {}", activeThreadNum.get());
                //    }

                ACTIVE_THREAD_NUM.incrementAndGet();

                return super.execute(new FutureTask<>(() -> {
                    try {
                        command.run();
                        return null;
                    } finally {
                        ACTIVE_THREAD_NUM.decrementAndGet();

                        //    if (logger.isDebugEnabled()) {
                        //        logger.debug("======: ActiveTaskCount-Pool: {}", threadPoolExecutor.getActiveCount());
                        //    }
                    }
                }) {
                    @Override
                    public boolean cancel(final boolean mayInterruptIfRunning) {

                        // Since it should not happen, throw exception.
                        throw new RuntimeException("No task should be canceled if there is no call Future.cancel. This is an unexpected behavior");
                    }
                });
            }

            @Override
            public <R> ContinuableFuture<R> execute(final Callable<? extends R> command) {
                //    if (threadPoolExecutor.getActiveCount() >= MAX_THREAD_POOL_SIZE) {
                //        throw new RejectedExecutionException("Task is rejected due to exceed max thread pool size: " + MAX_THREAD_POOL_SIZE);
                //    }

                //    if (logger.isDebugEnabled()) {
                //        logger.debug("######: ActiveTaskCount-Executor: {}", activeThreadNum.get());
                //    }

                ACTIVE_THREAD_NUM.incrementAndGet();

                return super.execute(new FutureTask<R>(() -> {
                    try {
                        return command.call();
                    } finally {
                        ACTIVE_THREAD_NUM.decrementAndGet();

                        //    if (logger.isDebugEnabled()) {
                        //        logger.debug("======: ActiveTaskCount-Pool: {}", threadPoolExecutor.getActiveCount());
                        //    }
                    }
                }) {
                    @Override
                    public boolean cancel(final boolean mayInterruptIfRunning) {

                        // Since it should not happen, throw exception.
                        throw new RuntimeException("No task should be canceled if there is no call Future.cancel. This is an unexpected behavior");
                    }
                });
            }
        };

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {

            logger.info("Starting Stream shutdown");

            try {
                threadPoolExecutor.shutdown();

                //noinspection ResultOfMethodCallIgnored
                threadPoolExecutor.awaitTermination(120, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Not all Stream tasks completed successfully before shutdown");
            } finally {
                logger.info("Stream shutdown completed");
            }
        }));
    }

    @SuppressWarnings("rawtypes")
    static final BiConsumer collectingCombiner = (t, u) -> {
        if (t instanceof Collection coll) {
            coll.addAll((Collection) u);
        } else if (t instanceof Map map) {
            map.putAll((Map) u);
        } else if (t instanceof StringBuilder sb) {
            sb.append((StringBuilder) u);
        } else //noinspection ConstantValue
        if (t instanceof Multiset multiset) {
            multiset.addAll((Multiset) u);
        } else if (t instanceof Multimap multimap) {
            multimap.putValues((Multimap) u);
        } else {
            final Class<?> cls = t.getClass();
            final Integer num = CLS_SEQ_MAP.get(cls);

            if (num == null) {
                throw new RuntimeException(cls.getCanonicalName()
                        + " cannot be combined by default. Only Collection/Map/StringBuilder/Multiset/LongMultiset/Multimap/BooleanList/IntList/.../DoubleList are supported");
            }

            switch (num) {
                case 8:
                    ((BooleanList) t).addAll((BooleanList) u);
                    break;
                case 9:
                    ((CharList) t).addAll((CharList) u);
                    break;
                case 10:
                    ((ByteList) t).addAll((ByteList) u);
                    break;
                case 11:
                    ((ShortList) t).addAll((ShortList) u);
                    break;
                case 12:
                    ((IntList) t).addAll((IntList) u);
                    break;
                case 13:
                    ((LongList) t).addAll((LongList) u);
                    break;
                case 14:
                    ((FloatList) t).addAll((FloatList) u);
                    break;
                case 15:
                    ((DoubleList) t).addAll((DoubleList) u);
                    break;

                default:
                    throw new IllegalArgumentException(cls.getCanonicalName()
                            + " cannot be combined by default. Only Collection/Map/StringBuilder/Multiset/LongMultiset/Multimap/BooleanList/IntList/.../DoubleList are supported");
            }
        }
    };

    static volatile boolean isListElementDataFieldGettable = true;
    static final Field listElementDataField;

    static {
        Field tmp = null;

        try {
            tmp = ArrayList.class.getDeclaredField("elementData");
        } catch (final Throwable e) { // NOSONAR
            // ignore.
        }

        listElementDataField = tmp != null && tmp.getType().equals(Object[].class) ? tmp : null;

        if (listElementDataField != null) {
            ClassUtil.setAccessibleQuietly(listElementDataField, true);
        }
    }

    private final boolean sorted;
    private final Comparator<? super T> cmp;
    private Deque<LocalRunnable> closeHandlers;
    private volatile boolean isClosed = false;

    StreamBase(final boolean sorted, final Comparator<? super T> cmp, final Collection<LocalRunnable> closeHandlers) {
        this.closeHandlers = isEmptyCloseHandlers(closeHandlers) ? null
                : (closeHandlers instanceof LocalArrayDeque ? (LocalArrayDeque<LocalRunnable>) closeHandlers : new LocalArrayDeque<>(closeHandlers));

        this.sorted = sorted;
        this.cmp = cmp;
    }

    /**
     * Returns a stream consisting of the elements of this stream in a random order,
     * using the default {@link SecureRandom} instance.
     *
     * <p>This is a stateful intermediate operation.
     *
     * @return a new stream with elements in a randomly shuffled order
     * @see #shuffled(Random)
     */
    @Override
    public S shuffled() {
        return shuffled(RAND);
    }

    /**
     * Returns a stream consisting of elements from this stream, skipping the first {@code offset}
     * elements and then limiting the result to at most {@code maxSize} elements.
     *
     * <p>This is a short-circuit stateful intermediate operation equivalent to
     * {@code skip(offset).limit(maxSize)}.
     *
     * <pre>{@code
     * // Get elements 10 through 19 (0-based):
     * stream.limit(10, 10);
     * }</pre>
     *
     * @param offset the number of leading elements to skip; must be non-negative
     * @param maxSize the maximum number of elements to include; must be non-negative
     * @return a new stream with at most {@code maxSize} elements, starting after skipping {@code offset} elements
     * @throws IllegalArgumentException if {@code offset} or {@code maxSize} is negative
     */
    @Override
    public S limit(final long offset, final long maxSize) {
        assertNotClosed();
        checkArgNotNegative(offset, cs.offset);
        checkArgNotNegative(maxSize, cs.maxSize);

        if (offset == 0) {
            return maxSize == Long.MAX_VALUE ? (S) this : limit(maxSize);
        } else if (maxSize == Long.MAX_VALUE) {
            return skip(offset);
        } else {
            return skip(offset).limit(maxSize);
        }
    }

    /**
     * Returns an Optional containing the element at the given zero-based {@code position}
     * in encounter order, or an empty Optional if the stream has fewer than
     * {@code position + 1} elements.
     *
     * <p>This is a terminal short-circuit operation. The stream is closed after this call.
     *
     * @param position the zero-based index of the element to retrieve; must be non-negative
     * @return an Optional containing the element at {@code position}, or an empty Optional
     *         if the stream has fewer elements
     * @throws IllegalStateException if the stream has already been closed
     * @throws IllegalArgumentException if {@code position} is negative
     */
    @Override
    public OT elementAt(final long position) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(position, cs.position);

        if (position == 0) {
            return first();
        } else {
            //noinspection resource
            return skip(position).first();
        }
    }

    /**
     * Collects all elements of this stream into an {@link ImmutableList}.
     *
     * <p>This is a terminal operation. The stream is closed after this call.
     *
     * @return an {@link ImmutableList} containing all stream elements in encounter order
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public ImmutableList<T> toImmutableList() {
        return ImmutableList.wrap(toList());
    }

    /**
     * Collects all elements of this stream into an {@link ImmutableSet}.
     *
     * <p>This is a terminal operation. The stream is closed after this call.
     * Duplicate elements are silently discarded; the iteration order of the returned
     * set is unspecified.
     *
     * @return an {@link ImmutableSet} containing the distinct elements of this stream
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public ImmutableSet<T> toImmutableSet() {
        return ImmutableSet.wrap(toSet());
    }

    /**
     * Returns an array containing all elements of this stream.
     *
     * <p>This is a terminal operation. The stream is closed after this call.
     *
     * @return an array of type {@code A} containing all stream elements
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public A toArray() {
        return toArray(true);
    }

    protected abstract A toArray(boolean closeStream);

    protected A toArrayForIntermediateOp() {
        // return toArray(false);

        return toArray(true);
    }

    // Try to avoid unnecessary copy of an array if possible
    protected Tuple3<A, Integer, Integer> arrayForIntermediateOp() {
        // final A a = toArray(false);

        final A a = toArray(true);
        return Tuple.of(a, 0, Array.getLength(a));
    }

    protected abstract boolean isEmpty();

    /**
     * Returns a stream with the same elements as this stream, but if the stream is empty,
     * a {@link java.util.NoSuchElementException} will be thrown when the terminal operation
     * is eventually executed.
     *
     * <p>This is an intermediate operation.
     *
     * @return a stream with the same elements, with an empty-check guard that throws when
     *         a terminal operation is invoked on an empty stream
     * @see #throwIfEmpty(Supplier)
     */
    @Override
    public S throwIfEmpty() {
        return throwIfEmpty(Suppliers.newNoSuchElementException());
    }

    /**
     * Returns a stream with the same elements as this stream, but if the stream is empty,
     * the exception produced by {@code exceptionSupplier} will be thrown when the terminal
     * operation is eventually executed.
     *
     * <p>This is an intermediate operation.
     *
     * @param exceptionSupplier a supplier that produces the exception to throw if the stream is empty;
     *                          must not be {@code null}
     * @return a stream with the same elements, with an empty-check guard that throws when
     *         a terminal operation is invoked on an empty stream
     * @throws IllegalArgumentException if {@code exceptionSupplier} is {@code null}
     */
    @Override
    public S throwIfEmpty(final Supplier<? extends RuntimeException> exceptionSupplier) throws IllegalArgumentException {
        checkArgNotNull(exceptionSupplier, cs.exceptionSupplier);

        return ifEmpty(() -> {
            throw exceptionSupplier.get();
        });
    }

    /**
     * Applies the given function to this stream if it is not empty, and returns an Optional
     * containing the result. If the stream is empty, returns an empty Optional.
     *
     * <p>This is a terminal operation. The stream is always closed after this call.
     *
     * <pre>{@code
     * Optional<Long> count = stream.applyIfNotEmpty(Stream::count);
     * }</pre>
     *
     * @param <R> the type of the result produced by the function
     * @param <E> the type of exception that the function may throw
     * @param func the function to apply to this stream if it is non-empty; must not be {@code null}
     * @return an Optional containing the result of applying {@code func} to this stream,
     *         or an empty Optional if the stream is empty
     * @throws IllegalStateException if the stream has already been closed
     * @throws E if the function throws a checked exception
     */
    @Override
    public <R, E extends Exception> Optional<R> applyIfNotEmpty(final Throwables.Function<? super S, ? extends R, E> func) throws IllegalStateException, E {
        assertNotClosed();
        N.checkArgNotNull(func, cs.func);

        try {
            if (isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.ofNullable(func.apply((S) this));
            }
        } finally {
            close();
        }
    }

    /**
     * Passes this stream to the given consumer action if the stream is not empty,
     * returning {@link OrElse#TRUE}. If the stream is empty, the action is not executed
     * and {@link OrElse#FALSE} is returned, allowing optional chaining with {@code orElse(...)}.
     *
     * <p>This is a terminal operation. The stream is always closed after this call.
     *
     * <pre>{@code
     * stream.acceptIfNotEmpty(s -> s.forEach(System.out::println))
     *       .orElse(() -> System.out.println("Stream was empty"));
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the consumer to invoke with this stream if it is non-empty; must not be {@code null}
     * @return {@link OrElse#TRUE} if the stream was non-empty and the action was executed,
     *         {@link OrElse#FALSE} otherwise
     * @throws IllegalStateException if the stream has already been closed
     * @throws E if the action throws a checked exception
     */
    @Override
    public <E extends Exception> OrElse acceptIfNotEmpty(final Throwables.Consumer<? super S, E> action) throws IllegalStateException, E {
        assertNotClosed();
        N.checkArgNotNull(action, cs.action);

        try {
            if (isEmpty()) {
                return OrElse.FALSE;
            } else {
                action.accept((S) this);
                return OrElse.TRUE;
            }
        } finally {
            close();
        }
    }

    /**
     * Prints the elements of this stream to standard output in a comma-separated, bracket-enclosed format.
     * If the stream contains more than {@value #MAX_SIZE_FOR_PRINTLN} elements, the output is truncated
     * and a trailing {@code ", ..."} suffix is appended.
     *
     * <p>This is a terminal operation. The stream is closed after this call.
     *
     * <p>Example output for a stream of {@code [1, 2, 3]}: {@code [1, 2, 3]}
     *
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public void println() {
        final List<T> list = sequential().limit(MAX_SIZE_FOR_PRINTLN + 1).toList();

        if (list.size() > MAX_SIZE_FOR_PRINTLN) {
            N.println(Strings.join(list.subList(0, MAX_SIZE_FOR_PRINTLN), ", ", "[", ", ...]"));
        } else {
            N.println(Strings.join(list, ", ", "[", "]"));
        }
    }

    /**
     * Returns {@code false}, indicating that this stream executes sequentially.
     * Parallel stream implementations override this to return {@code true}.
     *
     * @return {@code false}
     */
    @Override
    public boolean isParallel() {
        return false;
    }

    /**
     * Returns this stream as a sequential stream.
     * Because this base implementation is already sequential, this method returns {@code this}.
     *
     * @return this stream
     */
    @Override
    public S sequential() {
        return (S) this;
    }

    /**
     * Returns an equivalent parallel stream using the default maximum thread count.
     *
     * @return a parallel stream equivalent to this stream
     * @see #parallel(int)
     * @see #parallel(Executor)
     */
    @Override
    public S parallel() {
        return parallel(DEFAULT_MAX_THREAD_NUM);
    }

    /**
     * Returns an equivalent parallel stream using at most {@code maxThreadNum} threads,
     * with the default executor.
     *
     * @param maxThreadNum the maximum number of threads to use for parallel execution;
     *                     must be non-negative. A value of {@code 0} uses the default thread count.
     * @return a parallel stream equivalent to this stream
     * @throws IllegalArgumentException if {@code maxThreadNum} is negative
     */
    @Override
    public S parallel(final int maxThreadNum) {
        return parallel(maxThreadNum, null);
    }

    /**
     * Returns an equivalent parallel stream using the default maximum thread count
     * and the specified executor.
     *
     * @param executor the executor to use for parallel task submission;
     *                 if {@code null}, the default executor is used
     * @return a parallel stream equivalent to this stream
     */
    @Override
    public S parallel(final Executor executor) {
        return parallel(DEFAULT_MAX_THREAD_NUM, executor);
    }

    /**
     * Returns an equivalent parallel stream using at most {@code maxThreadNum} threads
     * and the given executor.
     *
     * @param maxThreadNum the maximum number of threads to use for parallel execution;
     *                     must be non-negative
     * @param executor the executor to use for parallel task submission;
     *                 if {@code null}, the default executor is used
     * @return a parallel stream equivalent to this stream
     * @throws IllegalArgumentException if {@code maxThreadNum} is negative
     */
    @Override
    public S parallel(final int maxThreadNum, final Executor executor) {
        checkArgNotNegative(maxThreadNum, cs.maxThreadNum);

        final AsyncExecutor asyncExecutor = executor == null ? DEFAULT_ASYNC_EXECUTOR : createAsyncExecutor(executor);

        return parallel(checkMaxThreadNum(maxThreadNum, asyncExecutor), DEFAULT_SPLITOR, asyncExecutor, false);
    }

    //    @Override

    /**
     * Returns an equivalent parallel stream configured according to the given {@link ParallelSettings}.
     * The settings specify the maximum thread count, splitor strategy, and executor to use.
     *
     * @param ps the parallel settings to apply; must not be {@code null}
     * @return a parallel stream configured according to {@code ps}
     * @throws IllegalArgumentException if {@code ps} is {@code null} or contains invalid settings
     */
    @SuppressWarnings("deprecation")
    @Override
    public S parallel(final ParallelSettings ps) throws IllegalArgumentException {
        checkArgNotNull(ps, cs.ps);
        checkArgNotNegative(ps.maxThreadNum(), "ParallelSettings.maxThreadNum");

        final int maxThreadNum = ps.maxThreadNum() == 0 ? DEFAULT_MAX_THREAD_NUM : ps.maxThreadNum();
        final Splitor splitor = ps.splitor() == null ? DEFAULT_SPLITOR : ps.splitor();
        final AsyncExecutor asyncExecutor = ps.executor() == null ? DEFAULT_ASYNC_EXECUTOR : createAsyncExecutor(ps.executor());
        final int checkedMaxThreadNum = checkMaxThreadNum(maxThreadNum, asyncExecutor);
        // final int checkedVirtualTaskNum = checkExecutorNumForVirtualThread(checkedMaxThreadNum, ps.executorNumForVirtualThread());

        return parallel(checkedMaxThreadNum, splitor, asyncExecutor, false);
    }

    protected abstract S parallel(final int maxThreadNum, final Splitor splitor, final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads);

    // protected abstract S parallel(final int maxThreadNum, final Splitor splitor, final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads);

    // protected abstract S parallel(final int maxThreadNum, final Splitor splitor, final AsyncExecutor asyncExecutor, final boolean cancelUncompletedThreads);

    static int checkMaxThreadNum(final int maxThreadNum, final AsyncExecutor asyncExecutor) {
        if (maxThreadNum == 0) {
            return DEFAULT_MAX_THREAD_NUM;
        } else if ((asyncExecutor == null || asyncExecutor == DEFAULT_ASYNC_EXECUTOR) && maxThreadNum > MAX_THREAD_NUM_PER_OPERATION) {
            //    throw new IllegalArgumentException("'maxThreadNum' must be >= 0 and <= " + MAX_THREAD_NUM_PER_OPERATION
            //            + " for default Executor. To parallelize Stream with bigger thread number, Please specify Executor.");

            //  should and why if yes?
            // parallel(Executor) will be used for bigger thread number and virtual thread support.
            return MAX_THREAD_NUM_PER_OPERATION;

        }

        return maxThreadNum;
    }

    /**
     * Applies the given stream operations switching to parallel mode if this stream is sequential,
     * or staying parallel if it already is, and then switches back to sequential.
     * This is a shorthand for "sequential-to-parallel-to-sequential" transformation.
     *
     * <p>The parallel execution uses the default thread count and executor.
     *
     * <pre>{@code
     * // Apply expensive mapping in parallel, then continue sequentially:
     * stream.sps(s -> s.map(expensiveMapper)).forEach(consumer);
     * }</pre>
     *
     * @param <SS> the type of the resulting stream
     * @param ops the stream operations to apply; must not be {@code null}
     * @return a sequential stream that is the result of applying {@code ops} in parallel
     * @throws IllegalStateException if the stream has already been closed
     */
    @SuppressWarnings("rawtypes")
    @Override
    public <SS extends BaseStream> SS sps(final Function<? super S, ? extends SS> ops) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return (SS) ops.apply((S) this).sequential();
        } else {
            return (SS) ops.apply(this.parallel()).sequential();
        }
    }

    /**
     * Applies the given stream operations in parallel using at most {@code maxThreadNum} threads,
     * then switches the resulting stream back to sequential mode.
     *
     * @param <SS> the type of the resulting stream
     * @param maxThreadNum the maximum number of threads to use for parallel execution;
     *                     must be non-negative
     * @param ops the stream operations to apply; must not be {@code null}
     * @return a sequential stream that is the result of applying {@code ops} with the specified
     *         parallelism
     * @throws IllegalStateException if the stream has already been closed
     */
    @SuppressWarnings({ "rawtypes" })
    @Override
    public <SS extends BaseStream> SS sps(final int maxThreadNum, final Function<? super S, ? extends SS> ops) throws IllegalStateException {
        assertNotClosed();
        checkArgNotNegative(maxThreadNum, cs.maxThreadNum);

        if (isParallel() && maxThreadNum == maxThreadNum()) {
            return (SS) ops.apply((S) this).sequential();
        } else {
            final int checkedMaxThreadNum = checkMaxThreadNum(maxThreadNum, asyncExecutor());
            // final int checkedVirtualTaskNum = checkExecutorNumForVirtualThread(checkedMaxThreadNum);

            return (SS) ops.apply(parallel(checkedMaxThreadNum, splitor(), asyncExecutor(), cancelUncompletedThreads())).sequential();
        }
    }

    /**
     * Applies the given stream operations in parallel using at most {@code maxThreadNum} threads
     * and the given executor, then switches the resulting stream back to sequential mode.
     *
     * @param <SS> the type of the resulting stream
     * @param maxThreadNum the maximum number of threads to use for parallel execution;
     *                     must be non-negative
     * @param executor the executor to use for parallel task submission;
     *                 if {@code null}, the default executor is used
     * @param ops the stream operations to apply; must not be {@code null}
     * @return a sequential stream that is the result of applying {@code ops} with the specified
     *         parallelism and executor
     * @throws IllegalStateException if the stream has already been closed
     */
    @SuppressWarnings("rawtypes")
    @Override
    public <SS extends BaseStream> SS sps(final int maxThreadNum, final Executor executor, final Function<? super S, ? extends SS> ops)
            throws IllegalStateException {
        assertNotClosed();
        checkArgNotNegative(maxThreadNum, cs.maxThreadNum);

        final AsyncExecutor asyncExecutor = executor == null ? DEFAULT_ASYNC_EXECUTOR : createAsyncExecutor(executor);

        return (SS) ops.apply(parallel(checkMaxThreadNum(maxThreadNum, asyncExecutor), splitor(), asyncExecutor, cancelUncompletedThreads())).sequential();

    }

    //    @Override
    //    @SuppressWarnings("rawtypes")
    //    public <SS extends BaseStream> SS sps(final ParallelSettings ps, final Function<? super S, ? extends SS> ops) {
    //        assertNotClosed();
    //
    //        return (SS) ((StreamBase) ops.apply(parallel(ps))).sequential();
    //    }

    /**
     * Applies the given stream operations switching to sequential mode if this stream is parallel,
     * or staying sequential if it already is, and then switches back to parallel.
     * This is a shorthand for "parallel-to-sequential-to-parallel" transformation.
     *
     * <p>Useful when a portion of the pipeline must run sequentially (e.g., order-sensitive
     * operations) while surrounding stages benefit from parallelism.
     *
     * @param <SS> the type of the resulting stream
     * @param ops the stream operations to apply; must not be {@code null}
     * @return a parallel stream that is the result of applying {@code ops} sequentially
     * @throws IllegalStateException if the stream has already been closed
     */
    @SuppressWarnings("rawtypes")
    @Override
    public <SS extends BaseStream> SS psp(final Function<? super S, ? extends SS> ops) throws IllegalStateException {
        assertNotClosed();

        if (isParallel()) {
            return (SS) ((StreamBase) ops.apply(this.sequential())).parallel(maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads());
        } else {
            return (SS) ops.apply((S) this).parallel();
        }
    }

    /**
     * Transforms this stream into a different stream type by applying the given transfer function.
     * This allows arbitrary conversions between stream types in a pipeline.
     *
     * <pre>{@code
     * IntStream intStream = stream.transform(s -> s.mapToInt(String::length));
     * }</pre>
     *
     * @param <RS> the type of the resulting stream
     * @param transfer a function that converts this stream to the target stream type; must not be {@code null}
     * @return the result of applying {@code transfer} to this stream
     * @throws IllegalStateException if the stream has already been closed
     * @throws IllegalArgumentException if {@code transfer} is {@code null}
     */
    @SuppressWarnings("rawtypes")
    @Override
    public <RS extends BaseStream> RS transform(final Function<? super S, ? extends RS> transfer) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(transfer, cs.transfer);

        // final Supplier<RS> delayInitializer = () ->  transfer.apply((S) this);
        //
        // Stream.defer(delayInitializer)... to RS

        return transfer.apply((S) this); // TODO not lazy evaluation?
    }

    protected int maxThreadNum() {
        // throw new UnsupportedOperationException();

        // ignore, do nothing if it's a sequential stream.
        return 1; //NOSONAR
    }

    protected Splitor splitor() {
        // throw new UnsupportedOperationException();

        // ignore, do nothing if it's a sequential stream.
        return DEFAULT_SPLITOR;
    }

    protected AsyncExecutor asyncExecutor() {
        // throw new UnsupportedOperationException();

        // ignore, do nothing if it's a sequential stream.
        return DEFAULT_ASYNC_EXECUTOR;
    }

    protected Deque<LocalRunnable> closeHandlers() {
        return closeHandlers;
    }

    protected boolean isSorted() {
        return sorted;
    }

    protected Comparator<? super T> comparator() {
        return cmp;
    }

    protected static Executor executor() {
        return DEFAULT_ASYNC_EXECUTOR.getExecutor();
    }

    protected boolean cancelUncompletedThreads() {
        // throw new UnsupportedOperationException();

        // ignore, do nothing if it's a sequential stream.
        return false;
    }

    final AsyncExecutor createAsyncExecutor(final Executor executor) {
        checkArgNotNull(executor, cs.executor);

        return new AsyncExecutor(executor);
    }

    protected boolean isClosed() {
        return isClosed;
    }

    /**
     * Registers a close handler to be executed when this stream is closed.
     * Multiple close handlers may be registered; they are executed in the order they were added.
     *
     * <p>This is an intermediate operation. The handler is wrapped so it runs at most once.
     * No-op handlers ({@code null} or the empty marker) are silently ignored.
     *
     * <pre>{@code
     * Stream<String> stream = Stream.of("a", "b")
     *     .onClose(() -> System.out.println("Stream closed"));
     * }</pre>
     *
     * @param closeHandler the handler to execute when this stream is closed; may be {@code null}
     *                     (silently ignored)
     * @return this stream
     * @throws IllegalStateException if the stream has already been closed
     */
    @Override
    public S onClose(final Runnable closeHandler) throws IllegalStateException {
        assertNotClosed();

        if (isEmptyCloseHandler(closeHandler)) {
            return (S) this;
        }

        this.closeHandlers = mergeCloseHandler(closeHandler);

        return (S) this;
    }

    /**
     * Closes this stream and all registered close handlers.
     *
     * <p>If the stream has already been closed, this method is a no-op.
     * Close handlers are invoked in the order they were registered. If a close handler
     * throws an exception, the remaining handlers are still executed and their exceptions
     * are suppressed into the first one thrown.
     *
     * <p>This method is thread-safe; concurrent calls are serialized.
     */
    @Override
    public synchronized void close() {
        if (isClosed) {
            return;
        }

        if (isEmptyCloseHandlers(closeHandlers)) {
            if (N.notEmpty(closeHandlers)) {
                closeHandlers.clear();
            }

            isClosed = true;
            return;
        }

        //    // Only mark the stream closed if closeHandlers are not empty.
        //    if (isClosed || isEmptyCloseHandlers(closeHandlers)) {
        //        return;
        //    }

        isClosed = true;

        if (logger.isDebugEnabled()) {
            logger.debug("Closing {}", ClassUtil.getSimpleClassName(getClass()));
        }

        close(closeHandlers);

        if (N.notEmpty(closeHandlers)) {
            closeHandlers.clear();
        }
    }

    static void close(final Collection<? extends Runnable> closeHandlers) {
        Throwable ex = null;

        for (final Runnable closeHandler : closeHandlers) {
            try {
                closeHandler.run();
            } catch (final Throwable e) {
                if (ex == null) {
                    ex = e;
                } else {
                    ex.addSuppressed(e);
                }
            }
        }

        if (ex != null) {
            throw toRuntimeException(ex);
        }
    }

    static void closeIterators(final Collection<? extends IteratorEx<?>> iters) {
        Throwable ex = null;

        for (final IteratorEx<?> iter : iters) {
            try {
                iter.closeResource();
            } catch (final Throwable e) {
                if (ex == null) {
                    ex = e;
                } else {
                    ex.addSuppressed(e);
                }
            }
        }

        if (ex != null) {
            throw toRuntimeException(ex);
        }
    }

    @SuppressWarnings("rawtypes")
    static void close(final Holder<? extends BaseStream> holder) {
        if (holder.value() != null) {
            holder.value().close();
        }
    }

    final void assertNotClosed() {
        if (isClosed) {
            throw new IllegalStateException("This stream is already terminated.");
        }
    }

    final void checkIndex(final int index, final int length) {
        if (index < 0 || index >= length) {
            try {
                N.checkElementIndex(index, length);
            } finally {
                close();
            }
        }
    }

    final void checkFromToIndex(final int fromIndex, final int toIndex, final int length) {
        if (fromIndex < 0 || fromIndex > toIndex || toIndex > length) {
            try {
                N.checkFromToIndex(fromIndex, toIndex, length);
            } finally {
                close();
            }
        }
    }

    final void checkFromIndexSize(final int fromIndex, final int size, final int length) {
        if ((length | fromIndex | size) < 0 || size > length - fromIndex) {
            try {
                N.checkFromIndexSize(fromIndex, size, length);
            } finally {
                close();
            }
        }
    }

    final void checkArgPositive(final int arg, final String argNameOrErrorMsg) {
        if (arg <= 0) {
            try {
                N.checkArgPositive(arg, argNameOrErrorMsg);
            } finally {
                close();
            }
        }
    }

    final void checkArgPositive(final long arg, final String argNameOrErrorMsg) {
        if (arg <= 0) {
            try {
                N.checkArgPositive(arg, argNameOrErrorMsg);
            } finally {
                close();
            }
        }
    }

    final void checkArgNotNegative(final int arg, final String argNameOrErrorMsg) {
        if (arg < 0) {
            try {
                N.checkArgNotNegative(arg, argNameOrErrorMsg);
            } finally {
                close();
            }
        }
    }

    final void checkArgNotNegative(final long arg, final String argNameOrErrorMsg) {
        if (arg < 0) {
            try {
                N.checkArgNotNegative(arg, argNameOrErrorMsg);
            } finally {
                close();
            }
        }
    }

    @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
    final <ARG> ARG checkArgNotNull(final ARG obj) {
        if (obj == null) {
            try {
                //noinspection ConstantValue,DataFlowIssue
                N.checkArgNotNull(obj);
            } finally {
                close();
            }
        }

        return obj;
    }

    @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
    final <ARG> ARG checkArgNotNull(final ARG obj, final String errorMessage) {
        if (obj == null) {
            try {
                //noinspection ConstantValue
                N.checkArgNotNull(obj, errorMessage);
            } finally {
                close();
            }
        }

        return obj;
    }

    final void checkArgNotEmpty(final Collection<?> c, final String errorMessage) {
        if (c == null || c.size() == 0) {
            try {
                N.checkArgNotEmpty(c, errorMessage);
            } finally {
                close();
            }
        }
    }

    final void checkArgument(final boolean b, final String errorMessage) {
        if (!b) {
            try {
                //noinspection ConstantValue,DataFlowIssue
                N.checkArgument(b, errorMessage);
            } finally {
                close();
            }
        }
    }

    final void checkArgument(final boolean b, final String errorMessageTemplate, final int p1) {
        if (!b) {
            try {
                //noinspection ConstantValue,DataFlowIssue
                N.checkArgument(b, errorMessageTemplate, p1);
            } finally {
                close();
            }
        }
    }

    final void checkArgument(final boolean b, final String errorMessageTemplate, final long p1) {
        if (!b) {
            try {
                //noinspection ConstantValue,DataFlowIssue
                N.checkArgument(b, errorMessageTemplate, p1);
            } finally {
                close();
            }
        }
    }

    final void checkArgument(final boolean b, final String errorMessageTemplate, final Object p1) {
        if (!b) {
            try {
                //noinspection ConstantValue,DataFlowIssue
                N.checkArgument(b, errorMessageTemplate, p1);
            } finally {
                close();
            }
        }
    }

    final void checkArgument(final boolean b, final String errorMessageTemplate, final int p1, final int p2) {
        if (!b) {
            try {
                //noinspection ConstantValue,DataFlowIssue
                N.checkArgument(b, errorMessageTemplate, p1, p2);
            } finally {
                close();
            }
        }
    }

    final void checkArgument(final boolean b, final String errorMessageTemplate, final long p1, final long p2) {
        if (!b) {
            try {
                //noinspection ConstantValue,DataFlowIssue
                N.checkArgument(b, errorMessageTemplate, p1, p2);
            } finally {
                close();
            }
        }
    }

    final void checkArgument(final boolean b, final String errorMessageTemplate, final Object p1, final Object p2) {
        if (!b) {
            try {
                //noinspection ConstantValue,DataFlowIssue
                N.checkArgument(b, errorMessageTemplate, p1, p2);
            } finally {
                close();
            }
        }
    }

    final void checkArgument(final boolean b, final String errorMessageTemplate, final Object p1, final Object p2, final Object p3) {
        if (!b) {
            try {
                //noinspection ConstantValue,DataFlowIssue
                N.checkArgument(b, errorMessageTemplate, p1, p2, p3);
            } finally {
                close();
            }
        }
    }

    final void checkState(final boolean b) {
        if (!b) {
            try {
                //noinspection ConstantValue,DataFlowIssue
                N.checkState(b);
            } finally {
                close();
            }
        }
    }

    final void checkState(final boolean b, final String errorMessage) {
        if (!b) {
            try {
                //noinspection ConstantValue,DataFlowIssue
                N.checkState(b, errorMessage);
            } finally {
                close();
            }
        }
    }

    final void checkState(final boolean b, final String errorMessageTemplate, final int p1) {
        if (!b) {
            try {
                //noinspection ConstantValue,DataFlowIssue
                N.checkState(b, errorMessageTemplate, p1);
            } finally {
                close();
            }
        }
    }

    final void checkState(final boolean b, final String errorMessageTemplate, final long p1) {
        if (!b) {
            try {
                //noinspection ConstantValue,DataFlowIssue
                N.checkState(b, errorMessageTemplate, p1);
            } finally {
                close();
            }
        }
    }

    final void checkState(final boolean b, final String errorMessageTemplate, final Object p1) {
        if (!b) {
            try {
                //noinspection ConstantValue,DataFlowIssue
                N.checkState(b, errorMessageTemplate, p1);
            } finally {
                close();
            }
        }
    }

    final void checkState(final boolean b, final String errorMessageTemplate, final int p1, final int p2) {
        if (!b) {
            try {
                //noinspection ConstantValue,DataFlowIssue
                N.checkState(b, errorMessageTemplate, p1, p2);
            } finally {
                close();
            }
        }
    }

    final void checkState(final boolean b, final String errorMessageTemplate, final long p1, final long p2) {
        if (!b) {
            try {
                //noinspection ConstantValue,DataFlowIssue
                N.checkState(b, errorMessageTemplate, p1, p2);
            } finally {
                close();
            }
        }
    }

    final void checkState(final boolean b, final String errorMessageTemplate, final Object p1, final Object p2) {
        if (!b) {
            try {
                //noinspection ConstantValue,DataFlowIssue
                N.checkState(b, errorMessageTemplate, p1, p2);
            } finally {
                close();
            }
        }
    }

    final void checkState(final boolean b, final String errorMessageTemplate, final Object p1, final Object p2, final Object p3) {
        if (!b) {
            try {
                //noinspection ConstantValue,DataFlowIssue
                N.checkState(b, errorMessageTemplate, p1, p2, p3);
            } finally {
                close();
            }
        }
    }

    Deque<LocalRunnable> mergeCloseHandler(final Runnable closeHandler) {
        return mergeCloseHandlers(closeHandler, closeHandlers);
    }

    Deque<LocalRunnable> mergeCloseHandler(final Collection<? extends IteratorEx<?>> iters) {
        return mergeCloseHandler(() -> closeIterators(iters));
    }

    Deque<LocalRunnable> closeHandlersForNewStream() {
        return mergeCloseHandlers(() -> {
            if (isClosed) {
                return;
            }

            isClosed = true;
        }, closeHandlers);
    }

    static boolean isEmptyCloseHandler(final Runnable closeHandler) {
        return closeHandler == null || closeHandler == EMPTY_CLOSE_HANDLER;
    }

    static boolean isEmptyCloseHandlers(final Collection<? extends Runnable> closeHandlers) {
        return N.isEmpty(closeHandlers) || (closeHandlers.size() == 1 && isEmptyCloseHandler(N.firstOrNullIfEmpty(closeHandlers)));
    }

    static LocalRunnable newCloseHandler(final Runnable closeHandler) {
        return LocalRunnable.wrap(closeHandler);
    }

    static LocalRunnable newCloseHandler(final AutoCloseable closeable) {
        return LocalRunnable.wrap(closeable);
    }

    @SuppressWarnings("rawtypes")
    static LocalRunnable newCloseHandler(final StreamBase s) {
        if (s == null || isEmptyCloseHandlers(s.closeHandlers)) {
            return EMPTY_CLOSE_HANDLER;
        }

        return s::close;
    }

    @SuppressWarnings("rawtypes")
    static LocalRunnable newCloseHandler(final StreamBase a, final StreamBase b) {
        if ((a == null || isEmptyCloseHandlers(a.closeHandlers)) && (b == null || isEmptyCloseHandlers(b.closeHandlers))) {
            return EMPTY_CLOSE_HANDLER;
        } else if (a == null || isEmptyCloseHandlers(a.closeHandlers)) {
            return b::close;
        } else if (b == null || isEmptyCloseHandlers(b.closeHandlers)) {
            return a::close;
        } else {
            return () -> {
                Throwable throwable = null;

                try {
                    a.close();
                } catch (final Throwable throwable1) {
                    throwable = throwable1;
                }

                try {
                    b.close();
                } catch (final Throwable throwable2) {
                    if (throwable == null) {
                        throwable = throwable2;
                    } else {
                        throwable.addSuppressed(throwable2);
                    }
                }

                if (throwable != null) {
                    throw toRuntimeException(throwable);
                }
            };
        }
    }

    @SuppressWarnings("rawtypes")
    static LocalRunnable newCloseHandler(final Collection<? extends StreamBase> c) {
        if (N.isEmpty(c)) {
            return EMPTY_CLOSE_HANDLER;
        }

        boolean allEmptyHandlers = true;

        for (final StreamBase s : c) {
            if (!(s == null || s.isClosed || isEmptyCloseHandlers(s.closeHandlers))) {
                allEmptyHandlers = false;
                break;
            }
        }

        if (allEmptyHandlers) {
            return EMPTY_CLOSE_HANDLER;
        }

        return () -> {
            Throwable throwable = null;

            for (final StreamBase s : c) {
                if (s == null || s.isClosed || isEmptyCloseHandlers(s.closeHandlers)) {
                    continue;
                }

                try {
                    s.close();
                } catch (final Throwable e) {
                    if (throwable == null) {
                        throwable = e;
                    } else {
                        throwable.addSuppressed(e);
                    }
                }
            }

            if (throwable != null) {
                throw toRuntimeException(throwable);
            }
        };
    }

    static Deque<LocalRunnable> mergeCloseHandlers(final Runnable newCloseHandlerToAdd, final Deque<LocalRunnable> closeHandlers) {
        return mergeCloseHandlers(newCloseHandlerToAdd, closeHandlers, false);
    }

    static Deque<LocalRunnable> mergeCloseHandlers(final Runnable newCloseHandlerToAdd, final Deque<LocalRunnable> closeHandlers,
            final boolean closeNewHandlerFirst) {
        if (isEmptyCloseHandler(newCloseHandlerToAdd)) {
            return closeHandlers;
        }

        final Deque<LocalRunnable> newCloseHandlers = new LocalArrayDeque<>(isEmptyCloseHandlers(closeHandlers) ? 1 : closeHandlers.size() + 1);

        if (closeNewHandlerFirst) {
            newCloseHandlers.add(newCloseHandler(newCloseHandlerToAdd));
        }

        if (!isEmptyCloseHandlers(closeHandlers)) {
            newCloseHandlers.addAll(closeHandlers);
        }

        if (!closeNewHandlerFirst) {
            newCloseHandlers.add(newCloseHandler(newCloseHandlerToAdd));
        }

        return newCloseHandlers;
    }

    static Deque<LocalRunnable> mergeCloseHandlers(final Deque<LocalRunnable> closeHandlersA, final Deque<LocalRunnable> closeHandlersB) {
        if (isEmptyCloseHandlers(closeHandlersA) && closeHandlersB instanceof LocalArrayDeque) {
            return closeHandlersB;
        } else if (closeHandlersA instanceof LocalArrayDeque && isEmptyCloseHandlers(closeHandlersB)) {
            return closeHandlersA;
        } else if (isEmptyCloseHandlers(closeHandlersA) && isEmptyCloseHandlers(closeHandlersB)) {
            return null; // NOSONAR
        }

        final Deque<LocalRunnable> newCloseHandlers = new LocalArrayDeque<>();

        if (!isEmptyCloseHandlers(closeHandlersA)) {
            newCloseHandlers.addAll(closeHandlersA);
        }

        if (!isEmptyCloseHandlers(closeHandlersB)) {
            for (final LocalRunnable h : closeHandlersB) {
                if (!newCloseHandlers.contains(h)) {
                    newCloseHandlers.add(h);
                }
            }
        }

        return newCloseHandlers;
    }

    @SuppressWarnings("rawtypes")
    static LocalRunnable newCloseHandler(final Supplier<? extends StreamBase> streamSupplier) {
        return () -> {
            final StreamBase s = streamSupplier != null ? streamSupplier.get() : null;

            if (s != null && !s.isClosed && !isEmptyCloseHandlers(s.closeHandlers)) {
                s.close();
            }
        };
    }

    @SuppressWarnings("rawtypes")
    static Deque<LocalRunnable> mergeCloseHandlers(final Deque<LocalRunnable> newCloseHandlers, final StreamBase s) {
        return mergeCloseHandlers(s == null ? null : s.closeHandlers, newCloseHandlers);
    }

    static void setError(final Holder<Throwable> errorHolder, final Throwable e) {
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (errorHolder) { //NOSONAR
            if (errorHolder.value() == null) {
                errorHolder.setValue(e);
            } else {
                errorHolder.value().addSuppressed(e);
            }
        }
    }

    CharStream newStream(final char[] a) {
        return newStream(a, false);
    }

    CharStream newStream(final char[] a, final boolean sorted) {
        if (isParallel()) {
            return new ParallelArrayCharStream(a, 0, a.length, sorted, maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads(),
                    closeHandlersForNewStream());
        } else {
            return new ArrayCharStream(a, sorted, closeHandlersForNewStream());
        }
    }

    CharStream newStream(final char[] a, final int fromIndex, final int toIndex) {
        return newStream(a, fromIndex, toIndex, false);
    }

    CharStream newStream(final char[] a, final int fromIndex, final int toIndex, final boolean sorted) {
        if (isParallel()) {
            return new ParallelArrayCharStream(a, fromIndex, toIndex, sorted, maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads(),
                    closeHandlersForNewStream());
        } else {
            return new ArrayCharStream(a, fromIndex, toIndex, sorted, closeHandlersForNewStream());
        }
    }

    CharStream newStream(final CharIterator iter) {
        return newStream(iter, false);
    }

    CharStream newStream(final CharIterator iter, final boolean sorted) {
        return newStream(iter, sorted, closeHandlersForNewStream());
    }

    CharStream newStream(final CharIterator iter, final boolean sorted, final Deque<LocalRunnable> closeHandlers) {
        if (isParallel()) {
            return new ParallelIteratorCharStream(iter, sorted, maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads(), closeHandlers);
        } else {
            return new IteratorCharStream(iter, sorted, closeHandlers);
        }
    }

    ByteStream newStream(final byte[] a) {
        return newStream(a, false);
    }

    ByteStream newStream(final byte[] a, final boolean sorted) {
        if (isParallel()) {
            return new ParallelArrayByteStream(a, 0, a.length, sorted, maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads(),
                    closeHandlersForNewStream());
        } else {
            return new ArrayByteStream(a, sorted, closeHandlersForNewStream());
        }
    }

    ByteStream newStream(final byte[] a, final int fromIndex, final int toIndex) {
        return newStream(a, fromIndex, toIndex, false);
    }

    ByteStream newStream(final byte[] a, final int fromIndex, final int toIndex, final boolean sorted) {
        if (isParallel()) {
            return new ParallelArrayByteStream(a, fromIndex, toIndex, sorted, maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads(),
                    closeHandlersForNewStream());
        } else {
            return new ArrayByteStream(a, fromIndex, toIndex, sorted, closeHandlersForNewStream());
        }
    }

    ByteStream newStream(final ByteIterator iter) {
        return newStream(iter, false);
    }

    ByteStream newStream(final ByteIterator iter, final boolean sorted) {
        return newStream(iter, sorted, closeHandlersForNewStream());
    }

    ByteStream newStream(final ByteIterator iter, final boolean sorted, final Deque<LocalRunnable> closeHandlers) {
        if (isParallel()) {
            return new ParallelIteratorByteStream(iter, sorted, maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads(), closeHandlers);
        } else {
            return new IteratorByteStream(iter, sorted, closeHandlers);
        }
    }

    ShortStream newStream(final short[] a) {
        return newStream(a, false);
    }

    ShortStream newStream(final short[] a, final boolean sorted) {
        if (isParallel()) {
            return new ParallelArrayShortStream(a, 0, a.length, sorted, maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads(),
                    closeHandlersForNewStream());
        } else {
            return new ArrayShortStream(a, sorted, closeHandlersForNewStream());
        }
    }

    ShortStream newStream(final short[] a, final int fromIndex, final int toIndex) {
        return newStream(a, fromIndex, toIndex, false);
    }

    ShortStream newStream(final short[] a, final int fromIndex, final int toIndex, final boolean sorted) {
        if (isParallel()) {
            return new ParallelArrayShortStream(a, fromIndex, toIndex, sorted, maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads(),
                    closeHandlersForNewStream());
        } else {
            return new ArrayShortStream(a, fromIndex, toIndex, sorted, closeHandlersForNewStream());
        }
    }

    ShortStream newStream(final ShortIterator iter) {
        return newStream(iter, false);
    }

    ShortStream newStream(final ShortIterator iter, final boolean sorted) {
        return newStream(iter, sorted, closeHandlersForNewStream());
    }

    ShortStream newStream(final ShortIterator iter, final boolean sorted, final Deque<LocalRunnable> closeHandlers) {
        if (isParallel()) {
            return new ParallelIteratorShortStream(iter, sorted, maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads(), closeHandlers);
        } else {
            return new IteratorShortStream(iter, sorted, closeHandlers);
        }
    }

    IntStream newStream(final int[] a) {
        return newStream(a, false);
    }

    IntStream newStream(final int[] a, final boolean sorted) {
        if (isParallel()) {
            return new ParallelArrayIntStream(a, 0, a.length, sorted, maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads(),
                    closeHandlersForNewStream());
        } else {
            return new ArrayIntStream(a, sorted, closeHandlersForNewStream());
        }
    }

    IntStream newStream(final int[] a, final int fromIndex, final int toIndex) {
        return newStream(a, fromIndex, toIndex, false);
    }

    IntStream newStream(final int[] a, final int fromIndex, final int toIndex, final boolean sorted) {
        if (isParallel()) {
            return new ParallelArrayIntStream(a, fromIndex, toIndex, sorted, maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads(),
                    closeHandlersForNewStream());
        } else {
            return new ArrayIntStream(a, fromIndex, toIndex, sorted, closeHandlersForNewStream());
        }
    }

    IntStream newStream(final IntIterator iter) {
        return newStream(iter, false);
    }

    IntStream newStream(final IntIterator iter, final boolean sorted) {
        return newStream(iter, sorted, closeHandlersForNewStream());
    }

    IntStream newStream(final IntIterator iter, final boolean sorted, final Deque<LocalRunnable> closeHandlers) {
        if (isParallel()) {
            return new ParallelIteratorIntStream(iter, sorted, maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads(), closeHandlers);
        } else {
            return new IteratorIntStream(iter, sorted, closeHandlers);
        }
    }

    LongStream newStream(final long[] a) {
        return newStream(a, false);
    }

    LongStream newStream(final long[] a, final boolean sorted) {
        if (isParallel()) {
            return new ParallelArrayLongStream(a, 0, a.length, sorted, maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads(),
                    closeHandlersForNewStream());
        } else {
            return new ArrayLongStream(a, sorted, closeHandlersForNewStream());
        }
    }

    LongStream newStream(final long[] a, final int fromIndex, final int toIndex) {
        return newStream(a, fromIndex, toIndex, false);
    }

    LongStream newStream(final long[] a, final int fromIndex, final int toIndex, final boolean sorted) {
        if (isParallel()) {
            return new ParallelArrayLongStream(a, fromIndex, toIndex, sorted, maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads(),
                    closeHandlersForNewStream());
        } else {
            return new ArrayLongStream(a, fromIndex, toIndex, sorted, closeHandlersForNewStream());
        }
    }

    LongStream newStream(final LongIterator iter) {
        return newStream(iter, false);
    }

    LongStream newStream(final LongIterator iter, final boolean sorted) {
        return newStream(iter, sorted, closeHandlersForNewStream());
    }

    LongStream newStream(final LongIterator iter, final boolean sorted, final Deque<LocalRunnable> closeHandlers) {
        if (isParallel()) {
            return new ParallelIteratorLongStream(iter, sorted, maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads(), closeHandlers);
        } else {
            return new IteratorLongStream(iter, sorted, closeHandlers);
        }
    }

    FloatStream newStream(final float[] a) {
        return newStream(a, false);
    }

    FloatStream newStream(final float[] a, final boolean sorted) {
        if (isParallel()) {
            return new ParallelArrayFloatStream(a, 0, a.length, sorted, maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads(),
                    closeHandlersForNewStream());
        } else {
            return new ArrayFloatStream(a, sorted, closeHandlersForNewStream());
        }
    }

    FloatStream newStream(final float[] a, final int fromIndex, final int toIndex) {
        return newStream(a, fromIndex, toIndex, false);
    }

    FloatStream newStream(final float[] a, final int fromIndex, final int toIndex, final boolean sorted) {
        if (isParallel()) {
            return new ParallelArrayFloatStream(a, fromIndex, toIndex, sorted, maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads(),
                    closeHandlersForNewStream());
        } else {
            return new ArrayFloatStream(a, fromIndex, toIndex, sorted, closeHandlersForNewStream());
        }
    }

    FloatStream newStream(final FloatIterator iter) {
        return newStream(iter, false);
    }

    FloatStream newStream(final FloatIterator iter, final boolean sorted) {
        return newStream(iter, sorted, closeHandlersForNewStream());
    }

    FloatStream newStream(final FloatIterator iter, final boolean sorted, final Deque<LocalRunnable> closeHandlers) {
        if (isParallel()) {
            return new ParallelIteratorFloatStream(iter, sorted, maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads(), closeHandlers);
        } else {
            return new IteratorFloatStream(iter, sorted, closeHandlers);
        }
    }

    DoubleStream newStream(final double[] a) {
        return newStream(a, false);
    }

    DoubleStream newStream(final double[] a, final boolean sorted) {
        if (isParallel()) {
            return new ParallelArrayDoubleStream(a, 0, a.length, sorted, maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads(),
                    closeHandlersForNewStream());
        } else {
            return new ArrayDoubleStream(a, sorted, closeHandlersForNewStream());
        }
    }

    DoubleStream newStream(final double[] a, final int fromIndex, final int toIndex) {
        return newStream(a, fromIndex, toIndex, false);
    }

    DoubleStream newStream(final double[] a, final int fromIndex, final int toIndex, final boolean sorted) {
        if (isParallel()) {
            return new ParallelArrayDoubleStream(a, fromIndex, toIndex, sorted, maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads(),
                    closeHandlersForNewStream());
        } else {
            return new ArrayDoubleStream(a, fromIndex, toIndex, sorted, closeHandlersForNewStream());
        }
    }

    DoubleStream newStream(final DoubleIterator iter) {
        return newStream(iter, false);
    }

    DoubleStream newStream(final DoubleIterator iter, final boolean sorted) {
        return newStream(iter, sorted, closeHandlersForNewStream());
    }

    DoubleStream newStream(final DoubleIterator iter, final boolean sorted, final Deque<LocalRunnable> closeHandlers) {
        if (isParallel()) {
            return new ParallelIteratorDoubleStream(iter, sorted, maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads(), closeHandlers);
        } else {
            return new IteratorDoubleStream(iter, sorted, closeHandlers);
        }
    }

    <E> Stream<E> newStream(final E[] a) {
        return newStream(a, false, null);
    }

    <E> Stream<E> newStream(final E[] a, final boolean sorted, final Comparator<? super E> comparator) {
        return newStream(a, 0, a.length, sorted, comparator);
    }

    <E> Stream<E> newStream(final E[] a, final int fromIndex, final int toIndex) {
        return newStream(a, fromIndex, toIndex, false, null);
    }

    <E> Stream<E> newStream(final E[] a, final int fromIndex, final int toIndex, final boolean sorted, final Comparator<? super E> comparator) {
        return newStream(a, fromIndex, toIndex, sorted, comparator, closeHandlersForNewStream());
    }

    <E> Stream<E> newStream(final E[] a, final int fromIndex, final int toIndex, final boolean sorted, final Comparator<? super E> comparator,
            final Deque<LocalRunnable> closeHandlers) {
        if (isParallel()) {
            return new ParallelArrayStream<>(a, fromIndex, toIndex, sorted, comparator, maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads(),
                    closeHandlers);
        } else {
            return new ArrayStream<>(a, fromIndex, toIndex, sorted, comparator, closeHandlers);
        }
    }

    <E> Stream<E> newStream(final Iterator<E> iter) {
        return newStream(iter, false, null);
    }

    <E> Stream<E> newStream(final Iterator<E> iter, final boolean sorted, final Comparator<? super E> comparator) {
        return newStream(iter, sorted, comparator, closeHandlersForNewStream());
    }

    <E> Stream<E> newStream(final Iterator<E> iter, final boolean sorted, final Comparator<? super E> comparator, final Deque<LocalRunnable> closeHandlers) {
        if (isParallel()) {
            return new ParallelIteratorStream<>(iter, sorted, comparator, maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads(),
                    closeHandlers);
        } else {
            return new IteratorStream<>(iter, sorted, comparator, closeHandlers);
        }
    }

    <E> Stream<E> newStream(final Stream<E> s) {
        return newStream(s, false, null);
    }

    <E> Stream<E> newStream(final Stream<E> s, final boolean sorted, final Comparator<? super E> comparator) {
        return newStream(s, sorted, comparator, closeHandlersForNewStream());
    }

    <E> Stream<E> newStream(final Stream<E> s, final boolean sorted, final Comparator<? super E> comparator, final Deque<LocalRunnable> closeHandlers) {
        if (isParallel()) {
            return new ParallelIteratorStream<>(s, sorted, comparator, maxThreadNum(), splitor(), asyncExecutor(), cancelUncompletedThreads(), closeHandlers);
        } else {
            return new IteratorStream<>(s, sorted, comparator, closeHandlers);
        }
    }

    static boolean isEmptyRange(final int arrayLength, final int fromIndex, final int toIndex) {
        N.checkFromToIndex(fromIndex, toIndex, arrayLength);

        return fromIndex == toIndex;
    }

    static CharIteratorEx iterate(final CharStream s) {
        return s == null ? CharIteratorEx.empty() : s.iteratorEx();
    }

    static ByteIteratorEx iterate(final ByteStream s) {
        return s == null ? ByteIteratorEx.empty() : s.iteratorEx();
    }

    static ShortIteratorEx iterate(final ShortStream s) {
        return s == null ? ShortIteratorEx.empty() : s.iteratorEx();
    }

    static IntIteratorEx iterate(final IntStream s) {
        return s == null ? IntIteratorEx.empty() : s.iteratorEx();
    }

    static LongIteratorEx iterate(final LongStream s) {
        return s == null ? LongIteratorEx.empty() : s.iteratorEx();
    }

    static FloatIteratorEx iterate(final FloatStream s) {
        return s == null ? FloatIteratorEx.empty() : s.iteratorEx();
    }

    static DoubleIteratorEx iterate(final DoubleStream s) {
        return s == null ? DoubleIteratorEx.empty() : s.iteratorEx();
    }

    static <T> ObjIteratorEx<T> iterate(final Stream<? extends T> s) {
        return s == null ? ObjIteratorEx.empty() : (ObjIteratorEx<T>) s.iteratorEx();
    }

    static <T> List<ObjIteratorEx<T>> iterateAll(final Collection<? extends Stream<? extends T>> streams) {
        if (N.isEmpty(streams)) {
            return new ArrayList<>(0);
        }

        final List<ObjIteratorEx<T>> result = new ArrayList<>(streams.size());

        for (final Stream<? extends T> s : streams) {
            result.add(s == null ? ObjIteratorEx.empty() : (ObjIteratorEx<T>) s.iteratorEx());
        }

        return result;
    }

    static CharIteratorEx charIterator(final ObjIteratorEx<Character> iter) {
        return CharIteratorEx.from(iter);
    }

    static ByteIteratorEx byteIterator(final ObjIteratorEx<Byte> iter) {
        return ByteIteratorEx.from(iter);
    }

    static ShortIteratorEx shortIterator(final ObjIteratorEx<Short> iter) {
        return ShortIteratorEx.from(iter);
    }

    static IntIteratorEx intIterator(final ObjIteratorEx<Integer> iter) {
        return IntIteratorEx.from(iter);
    }

    static LongIteratorEx longIterator(final ObjIteratorEx<Long> iter) {
        return LongIteratorEx.from(iter);
    }

    static FloatIteratorEx floatIterator(final ObjIteratorEx<Float> iter) {
        return FloatIteratorEx.from(iter);
    }

    static DoubleIteratorEx doubleIterator(final ObjIteratorEx<Double> iter) {
        return DoubleIteratorEx.from(iter);
    }

    /**
     * Counts the number of windows produced by a {@code sliding(windowSize, increment)} call
     * over a source of {@code remaining} elements. Includes any trailing partial window.
     * <p>
     * For {@code increment >= windowSize} (non-overlapping windows): {@code ceil(remaining/increment)}.
     * For {@code increment <  windowSize} (overlapping windows):     {@code 1} if {@code remaining <= windowSize},
     * otherwise {@code 1 + ceil((remaining-windowSize)/increment)}.
     *
     * @param remaining the total number of source elements
     * @param windowSize the size of each sliding window
     * @param increment the step size between consecutive window start positions
     * @return the number of windows that will be produced, or {@code 0} if {@code remaining <= 0}
     */
    static long countSlidingWindows(final long remaining, final int windowSize, final int increment) {
        if (remaining <= 0) {
            return 0;
        }
        if (increment >= windowSize) {
            final long result = remaining / increment;
            return result + (remaining % increment > 0 ? 1 : 0);
        }
        if (remaining <= windowSize) {
            return 1;
        }
        final long len = remaining - windowSize;
        return 1 + (len % increment == 0 ? len / increment : len / increment + 1);
    }

    static int sum(final char[] a) {
        if (a == null || a.length == 0) {
            return 0;
        }

        return sum(a, 0, a.length);
    }

    static int sum(final char[] a, final int fromIndex, final int toIndex) {
        long sum = 0;

        for (int i = fromIndex; i < toIndex; i++) {
            sum += a[i];
        }

        return Numbers.toIntExact(sum);
    }

    static int sum(final byte[] a) {
        if (a == null || a.length == 0) {
            return 0;
        }

        return sum(a, 0, a.length);
    }

    static int sum(final byte[] a, final int fromIndex, final int toIndex) {
        long sum = 0;

        for (int i = fromIndex; i < toIndex; i++) {
            sum += a[i];
        }

        return Numbers.toIntExact(sum);
    }

    static int sum(final short[] a) {
        if (a == null || a.length == 0) {
            return 0;
        }

        return sum(a, 0, a.length);
    }

    static int sum(final short[] a, final int fromIndex, final int toIndex) {
        long sum = 0;

        for (int i = fromIndex; i < toIndex; i++) {
            sum += a[i];
        }

        return Numbers.toIntExact(sum);
    }

    static long sumToLong(final int[] a) {
        if (a == null || a.length == 0) {
            return 0;
        }

        return sumToLong(a, 0, a.length);
    }

    static long sumToLong(final int[] a, final int fromIndex, final int toIndex) {
        //    long sum = 0;
        //
        //    for (int i = fromIndex; i < toIndex; i++) {
        //        sum += a[i];
        //    }
        //
        //    return Numbers.toIntExact(sum);

        return N.sumToLong(a, fromIndex, toIndex);
    }

    static long sum(final long[] a) {
        if (a == null || a.length == 0) {
            return 0L;
        }

        return sum(a, 0, a.length);
    }

    static long sum(final long[] a, final int fromIndex, final int toIndex) {
        //    long sum = 0;
        //
        //    for (int i = fromIndex; i < toIndex; i++) {
        //        sum = N.sum(a, fromIndex, toIndex);
        //    }
        //
        //    return sum;

        return N.sum(a, fromIndex, toIndex);
    }

    static double sum(final float[] a) {
        if (a == null || a.length == 0) {
            return 0d;
        }

        return sum(a, 0, a.length);
    }

    static double sum(final float[] a, final int fromIndex, final int toIndex) {
        //noinspection resource
        return N.sum(a, fromIndex, toIndex);
    }

    static double sum(final double[] a) {
        if (a == null || a.length == 0) {
            return 0d;
        }

        return sum(a, 0, a.length);
    }

    static double sum(final double[] a, final int fromIndex, final int toIndex) {
        //noinspection resource
        // return DoubleStream.of(a, fromIndex, toIndex).sum();

        return N.sum(a, fromIndex, toIndex);
    }

    static void complete(final List<ContinuableFuture<Void>> futureList, final Holder<Throwable> eHolder) {
        if (eHolder.value() != null) {
            throwRuntimeException(eHolder);
        }

        try {
            for (final ContinuableFuture<Void> future : futureList) {
                future.get();

                if (eHolder.value() != null) {
                    break;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            if (eHolder.value() != null) {
                throwRuntimeException(eHolder);
            }

            throw toRuntimeException(e);
        }

        if (eHolder.value() != null) {
            throwRuntimeException(eHolder);
        }
    }

    static <E extends Exception> void complete(final List<ContinuableFuture<Void>> futureList, final Holder<Throwable> eHolder, final E throwableTypeToNotUse)
            throws E {
        if (eHolder.value() != null) {
            throwException(eHolder, throwableTypeToNotUse);
        }

        try {
            for (final ContinuableFuture<Void> future : futureList) {
                future.get();

                if (eHolder.value() != null) {
                    break;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            if (eHolder.value() != null) {
                throwException(eHolder, throwableTypeToNotUse);
            }

            throw toRuntimeException(e);
        }

        if (eHolder.value() != null) {
            throwException(eHolder, throwableTypeToNotUse);
        }
    }

    static Deque<LocalRunnable> completeToClose(final MutableBoolean onGoing, final Holder<AsyncExecutor> holderForAsyncExecutorUsed) {
        final Deque<LocalRunnable> closeHandlers = new LocalArrayDeque<>(1);

        closeHandlers.add(() -> {
            onGoing.setFalse();

            if (holderForAsyncExecutorUsed.isNotNull()) {
                shutdownTempExecutor(holderForAsyncExecutorUsed.value());
            }
        });

        return closeHandlers;
    }

    static void completeAndShutdownTempExecutor(final List<ContinuableFuture<Void>> futureList, final Holder<Throwable> eHolder,
            @SuppressWarnings("rawtypes") final Collection<? extends BaseStream> streams, final AsyncExecutor asyncExecutorToUse) {
        try {
            complete(futureList, eHolder);
        } finally {
            try {
                if (eHolder.value() != null) {
                    IOUtil.closeAllQuietly(streams);
                }
            } finally {
                shutdownTempExecutor(asyncExecutorToUse);
            }
        }
    }

    static <E extends Exception> void completeAndShutdownTempExecutor(final List<ContinuableFuture<Void>> futureList, final Holder<Throwable> eHolder,
            @SuppressWarnings("rawtypes") final BaseStream stream, final AsyncExecutor specifiedAsyncExecutor, final AsyncExecutor asyncExecutorToUse)
            throws E {
        try {
            complete(futureList, eHolder, (E) null);
        } finally {
            try (stream) {
                shutdownTempExecutor(asyncExecutorToUse, specifiedAsyncExecutor);
            }
        }
    }

    static <R> R completeAndCollectResult(final List<ContinuableFuture<R>> futureList, final Holder<Throwable> eHolder, final Supplier<R> supplier,
            final BiConsumer<R, R> combiner, @SuppressWarnings("rawtypes") final BaseStream stream, final AsyncExecutor specifiedAsyncExecutor,
            final AsyncExecutor asyncExecutorToUse) {
        if (eHolder.value() != null) {
            stream.close();

            throwException(eHolder, null);
        }

        R container = (R) NONE;

        try {
            for (final ContinuableFuture<R> future : futureList) {
                if (container == NONE) {
                    container = future.get();
                } else {
                    combiner.accept(container, future.get());
                }

                if (eHolder.value() != null) {
                    break;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            if (eHolder.value() != null) {
                throwRuntimeException(eHolder);
            }

            throw toRuntimeException(e);
        } finally {
            try (stream) {
                shutdownTempExecutor(asyncExecutorToUse, specifiedAsyncExecutor);
            }
        }

        if (eHolder.value() != null) {
            throwRuntimeException(eHolder);
        }

        return container == NONE ? supplier.get() : container;
    }

    static int calculateBufferedSize(final int len, final int readThreadNum) {
        final int min = N.max(readThreadNum * 16, 16);
        return N.max(N.min(MAX_BUFFERED_SIZE, len * DEFAULT_BUFFERED_SIZE_PER_ITERATOR), min);
    }

    static int toInt(final long max) {
        return max > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) max;
    }

    static void setError(final Holder<Throwable> errorHolder, final Throwable e, final MutableBoolean onGoing) {
        // Set error handle first, then set onGoing sign.
        // If onGoing sign is set first but error has not been set, threads may stop without throwing exception because errorHolder is empty.
        setError(errorHolder, e);

        onGoing.setFalse();
    }

    static void setStopFlagAndThrowException(final Holder<Throwable> errorHolder, final MutableBoolean onGoing) {
        onGoing.setFalse();

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (errorHolder) { //NOSONAR
            if (errorHolder.value() != null) {
                throw toRuntimeException(errorHolder.getAndSet(null), false);
            }
        }
    }

    static void throwRuntimeException(final Holder<Throwable> eHolder) {
        final Throwable e = eHolder.value();

        if (e instanceof RuntimeException runtimeException) {
            throw runtimeException;
        } else if (e instanceof Error error) {
            throw error;
        } else {
            throw toRuntimeException(e);
        }
    }

    static <E extends Exception> void throwException(final Holder<Throwable> eHolder, @SuppressWarnings("unused") final E throwableTypeToNotUse) throws E { //NOSONAR
        final Throwable e = eHolder.value();

        if (e instanceof Exception) {
            throw (E) e;
        } else if (e instanceof Error error) {
            throw error;
        } else {
            throw toRuntimeException(e);
        }
    }

    static RuntimeException toRuntimeException(final Exception e) {
        return ExceptionUtil.toRuntimeException(e, true);
    }

    static RuntimeException toRuntimeException(final Throwable e) {
        return ExceptionUtil.toRuntimeException(e, true);
    }

    static RuntimeException toRuntimeException(final Throwable e, final boolean throwIfItIsError) {
        return ExceptionUtil.toRuntimeException(e, true, throwIfItIsError);
    }

    static boolean isSameComparator(final Comparator<?> a, final Comparator<?> b) {
        if (a == b) { // NOSONAR
            return true;
        } else if (a == null) {
            return DEFAULT_COMPARATOR_MAP.containsValue(b);
        } else if (b == null) {
            return DEFAULT_COMPARATOR_MAP.containsValue(a);
        } else {
            return (a == NATURAL_COMPARATOR && DEFAULT_COMPARATOR_MAP.containsValue(b)) || (b == NATURAL_COMPARATOR && DEFAULT_COMPARATOR_MAP.containsValue(a)); // NOSONAR
        }
    }

    static Object hashKey(final Object obj) {
        return obj == null ? NONE : (obj.getClass().isArray() ? Wrapper.of(obj) : obj);
    }

    static <T> T[] toArray(final Collection<T> c) {
        if (isListElementDataFieldGettable && listElementDataField != null && c.getClass().equals(ArrayList.class)) {
            try {
                final T[] a = (T[]) listElementDataField.get(c);

                if (a != null) {
                    final int size = c.size();
                    return a.length == size ? a : N.copyOf(a, size);
                }
            } catch (final Throwable e) { // NOSONAR
                // ignore;
                isListElementDataFieldGettable = false;
            }
        }

        return c.toArray((T[]) new Object[c.size()]);
    }

    static <T> List<T> subList(final List<T> list, final int fromIndex, final int toIndex) {
        return list.subList(fromIndex, N.min(list.size(), toIndex));
    }

    static <T> List<T> slice(final T[] a, final int fromIndex, final int toIndex) {
        if (N.isEmpty(a)) {
            return N.emptyList();
        }

        // return N.toList(N.copyOfRange(a, fromIndex, toIndex));

        return N.slice(a, fromIndex, toIndex);
    }

    @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
    protected static AsyncExecutor checkAsyncExecutor(AsyncExecutor asyncExecutor, final int threadNum) {
        if (asyncExecutor == null || asyncExecutor == DEFAULT_ASYNC_EXECUTOR) {

            int activeCount = 0;

            synchronized (DEFAULT_ASYNC_EXECUTOR) {
                activeCount = ACTIVE_THREAD_NUM.get();
                final boolean canUseDefaultAsyncExecutor = CORE_THREAD_POOL_SIZE - activeCount - threadNum > RESERVED_POOL_SIZE;

                if (canUseDefaultAsyncExecutor) {
                    return DEFAULT_ASYNC_EXECUTOR;
                }
            }

            logActiveThreads(activeCount);

            asyncExecutor = new AsyncExecutor(Executors.newFixedThreadPool(threadNum + 1)); // + 1, just in case.
        }

        return asyncExecutor;
    }

    protected static AsyncExecutor execute(final AsyncExecutor asyncExecutorToUse, final int maxThreadNum, final int taskIndex, final Runnable cmd) {
        return execute(asyncExecutorToUse, maxThreadNum, taskIndex, Fn.r2c(cmd));
    }

    protected static AsyncExecutor execute(final AsyncExecutor asyncExecutorToUse, final int maxThreadNum, final int taskIndex, final Callable<?> cmd) {
        return execute(asyncExecutorToUse, maxThreadNum, taskIndex, null, cmd);
    }

    protected static AsyncExecutor execute(final AsyncExecutor asyncExecutorToUse, final int maxThreadNum, final int taskIndex,
            final List<ContinuableFuture<Void>> futureList, final Runnable cmd) {
        return execute(asyncExecutorToUse, maxThreadNum, taskIndex, futureList, Fn.r2c(cmd));
    }

    protected static <R> AsyncExecutor execute(AsyncExecutor asyncExecutorToUse, final int maxThreadNum, final int taskIndex,
            final List<ContinuableFuture<R>> futureList, final Callable<? extends R> cmd) {
        // if (executorNumForVirtualThread == 0 || isVirtualThreadSupported == false) {
        if (asyncExecutorToUse == null) {
            asyncExecutorToUse = DEFAULT_ASYNC_EXECUTOR;
        }

        if (asyncExecutorToUse == DEFAULT_ASYNC_EXECUTOR) {
            int activeCount = 0;

            synchronized (DEFAULT_ASYNC_EXECUTOR) {
                activeCount = ACTIVE_THREAD_NUM.get();

                if (CORE_THREAD_POOL_SIZE - activeCount > RESERVED_POOL_SIZE) {
                    if (futureList == null) {
                        asyncExecutorToUse.execute(cmd);
                    } else {
                        futureList.add(asyncExecutorToUse.execute(cmd));
                    }

                    return asyncExecutorToUse;
                }
            }

            logActiveThreads(activeCount);

            asyncExecutorToUse = new AsyncExecutor(Executors.newFixedThreadPool(maxThreadNum - taskIndex + 1)); // + 1, just in case.
        }

        if (futureList == null) {
            asyncExecutorToUse.execute(cmd);
        } else {
            futureList.add(asyncExecutorToUse.execute(cmd));
        }

        return asyncExecutorToUse;
    }

    static void shutdownTempExecutor(final AsyncExecutor asyncExecutorToUse) {
        shutdownTempExecutor(asyncExecutorToUse, null);
    }

    static void shutdownTempExecutor(final AsyncExecutor asyncExecutorToUse, final AsyncExecutor specifiedAsyncExecutor) {
        if (asyncExecutorToUse == null || asyncExecutorToUse == DEFAULT_ASYNC_EXECUTOR || asyncExecutorToUse == specifiedAsyncExecutor) {
            // continue;
        } else {

            asyncExecutorToUse.shutdown();
        }
    }

    static void logActiveThreads(final int activeCount) {
        logger.info(
                "Creating a new thread pool to avoid deadlock when there are too many active threads running in the default thread pool. This new thread pool will be closed or shut down after execution. Core thread pool size: {}, Current active threads: {}",
                CORE_THREAD_POOL_SIZE, activeCount);
    }

    static boolean canBeSequential(final int maxThreadNum) {
        return maxThreadNum <= 1;
    }

    static boolean canBeSequential(final int maxThreadNum, final int fromIndex, final int toIndex) {
        return maxThreadNum <= 1 || toIndex - fromIndex <= 1;
    }

    @Internal
    interface LocalRunnable extends Runnable {

        /**
         * Wraps a close handler into a one-time executable {@code LocalRunnable}.
         * Returns a no-op handler when {@code closeHandler} is {@code null}, and returns the same instance if it is already a {@code LocalRunnable}.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Runnable closeHandler = () -> cleanup();
         * LocalRunnable local = LocalRunnable.wrap(closeHandler);
         * local.run();
         * }</pre>
         *
         * @param closeHandler the close handler to wrap, may be {@code null}
         * @return a one-time runnable wrapper for the close handler
         */
        static LocalRunnable wrap(final Runnable closeHandler) {
            if (closeHandler == null) {
                return EMPTY_CLOSE_HANDLER;
            } else if (closeHandler instanceof LocalRunnable) {
                return (LocalRunnable) closeHandler;
            }

            return new LocalRunnable() {
                private final AtomicBoolean isClosed = new AtomicBoolean(false);

                @Override
                public void run() {
                    if (isClosed.compareAndSet(false, true)) {
                        closeHandler.run();
                    }
                }
            };
        }

        /**
         * Wraps an {@code AutoCloseable} into a one-time executable {@code LocalRunnable}.
         * The returned runnable closes the given resource at most once.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * AutoCloseable closeable = resource;
         * LocalRunnable local = LocalRunnable.wrap(closeable);
         * local.run();
         * }</pre>
         *
         * @param closeable the closeable resource to wrap, may be {@code null}
         * @return a one-time runnable wrapper that closes the given resource
         */
        static LocalRunnable wrap(final AutoCloseable closeable) {
            return new LocalRunnable() {
                private final AtomicBoolean isClosed = new AtomicBoolean(false);

                @Override
                public void run() {
                    if (isClosed.compareAndSet(false, true)) {
                        IOUtil.close(closeable);
                    }
                }
            };
        }
    }

    @Internal
    static final class LocalArrayDeque<T> extends ArrayDeque<T> {

        @Serial
        private static final long serialVersionUID = -97425473105100734L;

        /**
         * Constructs an empty array deque with an initial capacity sufficient to hold 16 elements.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * LocalArrayDeque<String> deque = new LocalArrayDeque<>();
         * deque.add("first");
         * deque.add("second");
         * }</pre>
         *
         */
        public LocalArrayDeque() {
        }

        /**
         * Constructs an empty array deque with an initial capacity sufficient to hold the specified number of elements.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * LocalArrayDeque<Integer> deque = new LocalArrayDeque<>(100);
         * // Deque is pre-sized to efficiently hold up to 100 elements
         * }</pre>
         *
         * @param initialCapacity the initial capacity of the deque
         */
        public LocalArrayDeque(final int initialCapacity) {
            super(initialCapacity);
        }

        /**
         * Constructs a deque containing the elements of the specified collection, in the order they are returned by the collection's iterator.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * List<String> list = Arrays.asList("a", "b", "c");
         * LocalArrayDeque<String> deque = new LocalArrayDeque<>(list);
         * // Deque now contains: "a", "b", "c"
         * }</pre>
         *
         * @param c the collection whose elements are to be placed into the deque
         * @throws NullPointerException if the specified collection is null
         */
        public LocalArrayDeque(final Collection<? extends T> c) {
            super(c);
        }
    }

}
