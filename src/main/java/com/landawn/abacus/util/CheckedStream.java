/*
 * Copyright (C) 2019 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.StreamSupport;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.IntermediateOp;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.annotation.LazyEvaluation;
import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.annotation.TerminalOp;
import com.landawn.abacus.annotation.TerminalOpTriggered;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.parser.JSONParser;
import com.landawn.abacus.parser.JSONSerializationConfig;
import com.landawn.abacus.parser.JSONSerializationConfig.JSC;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Fn.Factory;
import com.landawn.abacus.util.Fn.Fnn;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.stream.Collectors;
import com.landawn.abacus.util.stream.DoubleStream;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.LongStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 * The Stream will be automatically closed after execution(A terminal method is executed/triggered).
 *
 * @author Haiyang Li
 * @param <T>
 * @param <E>
 * @since 1.3
 *
 * @see com.landawn.abacus.util.stream.ParallelizableStream
 * @see Stream
 * @see IntStream
 * @see LongStream
 * @see DoubleStream
 * @see Collectors
 * @see com.landawn.abacus.util.Fn.Fnn
 * @see com.landawn.abacus.util.Comparators
 * @see com.landawn.abacus.util.ExceptionUtil
 */
@LazyEvaluation
@SequentialOnly
@com.landawn.abacus.annotation.Immutable
@SuppressWarnings({ "java:S1192", "java:S1698", "java:S4968", "java:S6539" })
public final class CheckedStream<T, E extends Exception> implements Closeable, Immutable {
    private static final Logger logger = LoggerFactory.getLogger(CheckedStream.class);

    private static final Throwables.Function<OptionalInt, Integer, RuntimeException> GET_AS_INT = OptionalInt::get;

    private static final Throwables.Function<OptionalLong, Long, RuntimeException> GET_AS_LONG = OptionalLong::get;

    private static final Throwables.Function<OptionalDouble, Double, RuntimeException> GET_AS_DOUBLE = OptionalDouble::get;

    @SuppressWarnings("rawtypes")
    private static final Throwables.Function<Optional, Object, RuntimeException> GET_AS_IT = it -> it.orElse(null);

    private static final Throwables.Function<java.util.OptionalInt, Integer, RuntimeException> GET_AS_INT_JDK = java.util.OptionalInt::getAsInt;

    private static final Throwables.Function<java.util.OptionalLong, Long, RuntimeException> GET_AS_LONG_JDK = java.util.OptionalLong::getAsLong;

    private static final Throwables.Function<java.util.OptionalDouble, Double, RuntimeException> GET_AS_DOUBLE_JDK = java.util.OptionalDouble::getAsDouble;

    @SuppressWarnings("rawtypes")
    private static final Throwables.Function<java.util.Optional, Object, RuntimeException> GET_AS_IT_JDK = it -> it.orElse(null);

    private static final Throwables.Predicate<OptionalInt, RuntimeException> IS_PRESENT_INT = OptionalInt::isPresent;

    private static final Throwables.Predicate<OptionalLong, RuntimeException> IS_PRESENT_LONG = OptionalLong::isPresent;

    private static final Throwables.Predicate<OptionalDouble, RuntimeException> IS_PRESENT_DOUBLE = OptionalDouble::isPresent;

    @SuppressWarnings("rawtypes")
    private static final Throwables.Predicate<Optional, RuntimeException> IS_PRESENT_IT = Optional::isPresent;

    private static final Throwables.Predicate<java.util.OptionalInt, RuntimeException> IS_PRESENT_INT_JDK = java.util.OptionalInt::isPresent;

    private static final Throwables.Predicate<java.util.OptionalLong, RuntimeException> IS_PRESENT_LONG_JDK = java.util.OptionalLong::isPresent;

    private static final Throwables.Predicate<java.util.OptionalDouble, RuntimeException> IS_PRESENT_DOUBLE_JDK = java.util.OptionalDouble::isPresent;

    @SuppressWarnings("rawtypes")
    private static final Throwables.Predicate<java.util.Optional, RuntimeException> IS_PRESENT_IT_JDK = java.util.Optional::isPresent;

    private static final Throwables.Function<Map.Entry<Keyed<Object, Object>, Object>, Object, Exception> KK = t -> t.getKey().val();

    private static final Object NONE = new Object();

    private static final Random RAND = new SecureRandom();

    private static final String ERROR_MSG_FOR_NO_SUCH_EX = InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX;

    /**
     *
     * Char array with value {@code "['n', 'u', 'l', 'l']"}.
     */
    private static final char[] NULL_CHAR_ARRAY = Strings.NULL_STRING.toCharArray();

    private static final char[] ELEMENT_SEPARATOR_CHAR_ARRAY = Strings.ELEMENT_SEPARATOR.toCharArray();

    private static final int MAX_WAIT_TIME_FOR_QUEUE_OFFER = 9; // unit is milliseconds
    private static final int MAX_WAIT_TIME_FOR_QUEUE_POLL = 7; // unit is milliseconds

    static final int MAX_WAIT_TIME_FOR_QUEUE_OFFER_FOR_ADD_SUBSCRIBER = 30_000; // unit is milliseconds

    private static final int DEFAULT_BUFFERED_SIZE_PER_ITERATOR = 64;

    @SuppressWarnings("rawtypes")
    private static final Comparator NATURAL_COMPARATOR = Comparators.naturalOrder();

    @SuppressWarnings("rawtypes")
    private static final Comparator REVERSED_COMPARATOR = Comparators.reverseOrder();

    private static final Comparator<Character> CHAR_COMPARATOR = Character::compare;

    private static final Comparator<Byte> BYTE_COMPARATOR = Byte::compare;

    private static final Comparator<Short> SHORT_COMPARATOR = Short::compare;

    private static final Comparator<Integer> INT_COMPARATOR = Integer::compare;

    private static final Comparator<Long> LONG_COMPARATOR = Long::compare;

    private static final Comparator<Float> FLOAT_COMPARATOR = Float::compare;

    private static final Comparator<Double> DOUBLE_COMPARATOR = Double::compare;

    private static final BiMap<Class<?>, Comparator<?>> DEFAULT_COMPARATOR_MAP = new BiMap<>();

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

    private static final LocalRunnable EMPTY_CLOSE_HANDLER = () -> {
        // do nothing.
    };

    private final Throwables.Iterator<T, E> elements;
    private final boolean sorted;
    private final Comparator<? super T> cmp;
    private final Deque<LocalRunnable> closeHandlers;
    private boolean isClosed = false;

    CheckedStream(final Throwables.Iterator<? extends T, ? extends E> iter) {
        this(iter, false, null, null);
    }

    CheckedStream(final Throwables.Iterator<? extends T, ? extends E> iter, final Collection<LocalRunnable> closeHandlers) {
        this(iter, false, null, closeHandlers);
    }

    CheckedStream(final Throwables.Iterator<? extends T, ? extends E> iter, final boolean sorted, final Comparator<? super T> cmp,
            final Collection<LocalRunnable> closeHandlers) {
        this.elements = (Throwables.Iterator<T, E>) iter;
        this.sorted = sorted;
        this.cmp = cmp;
        this.closeHandlers = isEmptyCloseHandlers(closeHandlers) ? null
                : (closeHandlers instanceof LocalArrayDeque ? (LocalArrayDeque<LocalRunnable>) closeHandlers : new LocalArrayDeque<>(closeHandlers));

    }

    /**
     *
     * @param <T>
     * @param <E>
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> empty() {
        return new CheckedStream<>(Throwables.Iterator.empty());
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param stream
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> from(final Stream<? extends T> stream) {
        return newStream(stream, false);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param stream
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> from(final java.util.stream.Stream<? extends T> stream) {
        if (stream == null) {
            return empty();
        }

        return CheckedStream.<T, E> of(stream.iterator()).onClose(stream::close);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param stream
     * @param exceptionType
     * @return
     */
    // Should the name be from?
    public static <T, E extends Exception> CheckedStream<T, E> from(final Stream<? extends T> stream,
            @SuppressWarnings("unused") final Class<E> exceptionType) { //NOSONAR
        return from(stream);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param stream
     * @param exceptionType
     * @return
     */
    // Should the name be from?
    public static <T, E extends Exception> CheckedStream<T, E> from(final java.util.stream.Stream<? extends T> stream,
            @SuppressWarnings("unused") final Class<E> exceptionType) { //NOSONAR
        return from(stream);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param e
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> just(final T e) {
        return of(e);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param e
     * @param exceptionType
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> just(final T e, @SuppressWarnings("unused") final Class<E> exceptionType) { //NOSONAR
        return of(e);
    }

    /**
     * Returns an empty {@code Stream} if the specified {@code t} is null.
     *
     * @param <T>
     * @param <E>
     * @param e
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> ofNullable(final T e) {
        if (e == null) {
            return empty();
        }

        return of(e);
    }

    /**
     * Returns an empty {@code Stream} if the specified {@code t} is null.
     *
     * @param <T>
     * @param <E>
     * @param e
     * @param exceptionType
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> ofNullable(final T e, @SuppressWarnings("unused") final Class<E> exceptionType) { //NOSONAR
        if (e == null) {
            return empty();
        }

        return of(e);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @return
     * @see Stream.of(T[]).checked(SomeCheckedException.class)...
     */
    public static <T, E extends Exception> CheckedStream<T, E> of(final T... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return newStream(new Throwables.Iterator<T, E>() {
            private int position = 0;

            @Override
            public boolean hasNext() throws E {
                return position < len;
            }

            @Override
            public T next() throws E {
                if (position >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return a[position++];
            }

            @Override
            public long count() throws E {
                return len - position; //NOSONAR
            }

            @Override
            public void advance(long n) throws E {
                if (n > len - position) {
                    position = len;
                } else {
                    position += n;
                }
            }
        });
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iterable
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> of(final Iterable<? extends T> iterable) {
        if (iterable == null) {
            return empty();
        }

        return of(iterable.iterator());
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> of(final Iterator<? extends T> iter) {
        if (iter == null) {
            return empty();
        }

        return newStream(Throwables.Iterator.<T, E> of(iter));
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> of(final Throwables.Iterator<? extends T, ? extends E> iter) {
        if (iter == null) {
            return empty();
        }

        return newStream(iter);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <E>
     * @param m
     * @return
     */
    public static <K, V, E extends Exception> CheckedStream<Map.Entry<K, V>, E> of(final Map<K, V> m) {
        if (N.isEmpty(m)) {
            return empty();
        }

        return of(m.entrySet());
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param stream
     * @return
     * @deprecated Use {@link #from(Stream<? extends T>)} instead
     */
    @Deprecated
    public static <T, E extends Exception> CheckedStream<T, E> of(final Stream<? extends T> stream) {
        return from(stream);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param stream
     * @return
     * @deprecated Use {@link #from(java.util.stream.Stream<? extends T>)} instead
     */
    @Deprecated
    public static <T, E extends Exception> CheckedStream<T, E> of(final java.util.stream.Stream<? extends T> stream) {
        return from(stream);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iterable
     * @param exceptionType
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> of(final Iterable<? extends T> iterable,
            @SuppressWarnings("unused") final Class<E> exceptionType) { //NOSONAR
        return of(iterable);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param exceptionType
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> of(final Iterator<? extends T> iter, @SuppressWarnings("unused") final Class<E> exceptionType) { //NOSONAR
        return of(iter);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <E>
     * @param m
     * @param exceptionType
     * @return
     */
    public static <K, V, E extends Exception> CheckedStream<Map.Entry<K, V>, E> of(final Map<K, V> m,
            @SuppressWarnings("unused") final Class<E> exceptionType) { //NOSONAR
        return of(m);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param stream
     * @param exceptionType
     * @return
     * @deprecated Use {@link #from(Stream<? extends T>,Class<E>)} instead
     */
    // Should the name be from?
    @Deprecated
    public static <T, E extends Exception> CheckedStream<T, E> of(final Stream<? extends T> stream, @SuppressWarnings("unused") final Class<E> exceptionType) {
        return from(stream, exceptionType);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param stream
     * @param exceptionType
     * @return
     * @deprecated Use {@link #from(java.util.stream.Stream<? extends T>,Class<E>)} instead
     */
    // Should the name be from?
    @Deprecated
    public static <T, E extends Exception> CheckedStream<T, E> of(final java.util.stream.Stream<? extends T> stream,
            @SuppressWarnings("unused") final Class<E> exceptionType) {
        return from(stream, exceptionType);
    }

    /**
     *
     *
     * @param <E>
     * @param a
     * @return
     */
    public static <E extends Exception> CheckedStream<Boolean, E> of(final boolean[] a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return newStream(new Throwables.Iterator<Boolean, E>() {
            private int position = 0;

            @Override
            public boolean hasNext() throws E {
                return position < len;
            }

            @Override
            public Boolean next() throws E {
                return a[position++];
            }

            @Override
            public long count() throws E {
                return len - position; //NOSONAR
            }

            @Override
            public void advance(long n) throws E {
                if (n > len - position) {
                    position = len;
                } else {
                    position += n;
                }
            }
        });
    }

    /**
     *
     *
     * @param <E>
     * @param a
     * @return
     */
    public static <E extends Exception> CheckedStream<Character, E> of(final char[] a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return newStream(new Throwables.Iterator<Character, E>() {
            private int position = 0;

            @Override
            public boolean hasNext() throws E {
                return position < len;
            }

            @Override
            public Character next() throws E {
                return a[position++];
            }

            @Override
            public long count() throws E {
                return len - position; //NOSONAR
            }

            @Override
            public void advance(long n) throws E {
                if (n > len - position) {
                    position = len;
                } else {
                    position += n;
                }
            }
        });
    }

    /**
     *
     *
     * @param <E>
     * @param a
     * @return
     */
    public static <E extends Exception> CheckedStream<Byte, E> of(final byte[] a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return newStream(new Throwables.Iterator<Byte, E>() {
            private int position = 0;

            @Override
            public boolean hasNext() throws E {
                return position < len;
            }

            @Override
            public Byte next() throws E {
                return a[position++];
            }

            @Override
            public long count() throws E {
                return len - position; //NOSONAR
            }

            @Override
            public void advance(long n) throws E {
                if (n > len - position) {
                    position = len;
                } else {
                    position += n;
                }
            }
        });
    }

    /**
     *
     *
     * @param <E>
     * @param a
     * @return
     */
    public static <E extends Exception> CheckedStream<Short, E> of(final short[] a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return newStream(new Throwables.Iterator<Short, E>() {
            private int position = 0;

            @Override
            public boolean hasNext() throws E {
                return position < len;
            }

            @Override
            public Short next() throws E {
                return a[position++];
            }

            @Override
            public long count() throws E {
                return len - position; //NOSONAR
            }

            @Override
            public void advance(long n) throws E {
                if (n > len - position) {
                    position = len;
                } else {
                    position += n;
                }
            }
        });
    }

    /**
     *
     * @param <E>
     * @param a
     * @return
     */
    public static <E extends Exception> CheckedStream<Integer, E> of(final int[] a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return newStream(new Throwables.Iterator<Integer, E>() {
            private int position = 0;

            @Override
            public boolean hasNext() throws E {
                return position < len;
            }

            @Override
            public Integer next() throws E {
                return a[position++];
            }

            @Override
            public long count() throws E {
                return len - position; //NOSONAR
            }

            @Override
            public void advance(long n) throws E {
                if (n > len - position) {
                    position = len;
                } else {
                    position += n;
                }
            }
        });
    }

    /**
     *
     * @param <E>
     * @param a
     * @return
     */
    public static <E extends Exception> CheckedStream<Long, E> of(final long[] a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return newStream(new Throwables.Iterator<Long, E>() {
            private int position = 0;

            @Override
            public boolean hasNext() throws E {
                return position < len;
            }

            @Override
            public Long next() throws E {
                return a[position++];
            }

            @Override
            public long count() throws E {
                return len - position; //NOSONAR
            }

            @Override
            public void advance(long n) throws E {
                if (n > len - position) {
                    position = len;
                } else {
                    position += n;
                }
            }
        });
    }

    /**
     *
     *
     * @param <E>
     * @param a
     * @return
     */
    public static <E extends Exception> CheckedStream<Float, E> of(final float[] a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return newStream(new Throwables.Iterator<Float, E>() {
            private int position = 0;

            @Override
            public boolean hasNext() throws E {
                return position < len;
            }

            @Override
            public Float next() throws E {
                return a[position++];
            }

            @Override
            public long count() throws E {
                return len - position; //NOSONAR
            }

            @Override
            public void advance(long n) throws E {
                if (n > len - position) {
                    position = len;
                } else {
                    position += n;
                }
            }
        });
    }

    /**
     *
     * @param <E>
     * @param a
     * @return
     */
    public static <E extends Exception> CheckedStream<Double, E> of(final double[] a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return newStream(new Throwables.Iterator<Double, E>() {
            private int position = 0;

            @Override
            public boolean hasNext() throws E {
                return position < len;
            }

            @Override
            public Double next() throws E {
                return a[position++];
            }

            @Override
            public long count() throws E {
                return len - position; //NOSONAR
            }

            @Override
            public void advance(long n) throws E {
                if (n > len - position) {
                    position = len;
                } else {
                    position += n;
                }
            }
        });
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param op
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> of(final Optional<T> op) {
        return op == null || !op.isPresent() ? CheckedStream.<T, E> empty() : CheckedStream.<T, E> of(op.get()); //NOSONAR
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param op
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> of(final java.util.Optional<T> op) {
        return op == null || !op.isPresent() ? CheckedStream.<T, E> empty() : CheckedStream.<T, E> of(op.get()); //NOSONAR
    }

    /**
     *
     *
     * @param <K>
     * @param <E>
     * @param map
     * @return
     */
    public static <K, E extends Exception> CheckedStream<K, E> ofKeys(final Map<K, ?> map) {
        if (N.isEmpty(map)) {
            return empty();
        }

        return of(map.keySet());
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @param <E>
     * @param map
     * @param valueFilter
     * @return
     */
    public static <K, V, E extends Exception> CheckedStream<K, E> ofKeys(final Map<K, V> map, final Throwables.Predicate<? super V, E> valueFilter) {
        if (N.isEmpty(map)) {
            return empty();
        }

        return CheckedStream.<K, V, E> of(map).filter(Fnn.<K, V, E> testByValue(valueFilter)).map(Fnn.<K, V, E> key());
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @param <E>
     * @param map
     * @param filter
     * @return
     */
    public static <K, V, E extends Exception> CheckedStream<K, E> ofKeys(final Map<K, V> map, final Throwables.BiPredicate<? super K, ? super V, E> filter) {
        if (N.isEmpty(map)) {
            return empty();
        }

        return CheckedStream.<K, V, E> of(map).filter(Fn.Entries.ep(filter)).map(Fnn.<K, V, E> key());
    }

    /**
     *
     *
     * @param <V>
     * @param <E>
     * @param map
     * @return
     */
    public static <V, E extends Exception> CheckedStream<V, E> ofValues(final Map<?, V> map) {
        if (N.isEmpty(map)) {
            return empty();
        }

        return of(map.values());
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @param <E>
     * @param map
     * @param keyFilter
     * @return
     */
    public static <K, V, E extends Exception> CheckedStream<V, E> ofValues(final Map<K, V> map, final Throwables.Predicate<? super K, E> keyFilter) {
        if (N.isEmpty(map)) {
            return empty();
        }

        return CheckedStream.<K, V, E> of(map).filter(Fnn.<K, V, E> testByKey(keyFilter)).map(Fnn.<K, V, E> value());
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @param <E>
     * @param map
     * @param filter
     * @return
     */
    public static <K, V, E extends Exception> CheckedStream<V, E> ofValues(final Map<K, V> map, final Throwables.BiPredicate<? super K, ? super V, E> filter) {
        if (N.isEmpty(map)) {
            return empty();
        }

        return CheckedStream.<K, V, E> of(map).filter(Fn.Entries.ep(filter)).map(Fnn.<K, V, E> value());
    }

    /**
     * Lazy evaluation.
     * <br />
     *
     * This is equal to: {@code Seq.just(supplier).flatMap(Throwables.Supplier::get)}.
     *
     * @param <T>
     * @param <E>
     * @param supplier
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> defer(
            final Throwables.Supplier<? extends CheckedStream<? extends T, ? extends E>, ? extends E> supplier) {
        N.checkArgNotNull(supplier, "supplier");

        return CheckedStream.<Throwables.Supplier<? extends CheckedStream<? extends T, ? extends E>, ? extends E>, E> just(supplier)
                .flatMap(Throwables.Supplier::get);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param hasNext
     * @param next
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> iterate(final Throwables.BooleanSupplier<? extends E> hasNext,
            final Throwables.Supplier<? extends T, E> next) {
        N.checkArgNotNull(hasNext, "hasNext");
        N.checkArgNotNull(next, "next");

        return newStream(new Throwables.Iterator<T, E>() {
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() throws E {
                if (!hasNextVal) {
                    hasNextVal = hasNext.getAsBoolean();
                }

                return hasNextVal;
            }

            @Override
            public T next() throws E {
                if (!hasNextVal && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextVal = false;
                return next.get();
            }
        });
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param init
     * @param hasNext
     * @param f
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> iterate(final T init, final Throwables.BooleanSupplier<? extends E> hasNext,
            final Throwables.UnaryOperator<T, ? extends E> f) {
        N.checkArgNotNull(hasNext, "hasNext");
        N.checkArgNotNull(f, "f");

        return newStream(new Throwables.Iterator<T, E>() {
            private final T none = (T) NONE;
            private T t = none;
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() throws E {
                if (!hasNextVal) {
                    hasNextVal = hasNext.getAsBoolean();
                }

                return hasNextVal;
            }

            @Override
            public T next() throws E {
                if (!hasNextVal && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNextVal = false;
                return t = (t == none) ? init : f.apply(t);
            }
        });
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param init
     * @param hasNext
     * @param f
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> iterate(final T init, final Throwables.Predicate<? super T, ? extends E> hasNext,
            final Throwables.UnaryOperator<T, ? extends E> f) {
        N.checkArgNotNull(hasNext, "hasNext");
        N.checkArgNotNull(f, "f");

        return newStream(new Throwables.Iterator<T, E>() {
            private final T none = (T) NONE;
            private T t = none;
            private T cur = none;
            private boolean hasMore = true;
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() throws E {
                if (!hasNextVal && hasMore) {
                    hasNextVal = hasNext.test((cur = (t == none ? init : f.apply(t))));

                    if (!hasNextVal) {
                        hasMore = false;
                    }
                }

                return hasNextVal;
            }

            @Override
            public T next() throws E {
                if (!hasNextVal && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                t = cur;
                cur = none;
                hasNextVal = false;
                return t;
            }
        });
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param init
     * @param f
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> iterate(final T init, final Throwables.UnaryOperator<T, ? extends E> f) {
        N.checkArgNotNull(f, "f");

        return newStream(new Throwables.Iterator<T, E>() {
            private final T none = (T) NONE;
            private T t = none;

            @Override
            public boolean hasNext() throws E {
                return true;
            }

            @Override
            public T next() throws E {
                return t = t == none ? init : f.apply(t);
            }
        });
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param supplier
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> generate(final Throwables.Supplier<T, E> supplier) {
        N.checkArgNotNull(supplier, "supplier");

        return newStream(new Throwables.Iterator<T, E>() {
            @Override
            public boolean hasNext() throws E {
                return true;
            }

            @Override
            public T next() throws E {
                return supplier.get();
            }
        });
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param element
     * @param n
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> repeat(final T element, final long n) {
        N.checkArgNotNegative(n, "n");

        if (n == 0) {
            return empty();
        }

        return newStream(new Throwables.Iterator<T, E>() {
            private long cnt = n;

            @Override
            public boolean hasNext() throws E {
                return cnt > 0;
            }

            @Override
            public T next() throws E {
                if (cnt-- <= 0) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return element;
            }
        });
    }

    /**
     *
     *
     * @param <E>
     * @param startInclusive
     * @param endExclusive
     * @return
     */
    public static <E extends Exception> CheckedStream<Integer, E> range(final int startInclusive, final int endExclusive) {
        return IntStream.range(startInclusive, endExclusive).boxed().<E> checked();
    }

    /**
     *
     *
     * @param <E>
     * @param startInclusive
     * @param endExclusive
     * @param by
     * @return
     */
    public static <E extends Exception> CheckedStream<Integer, E> range(final int startInclusive, final int endExclusive, final int by) {
        return IntStream.range(startInclusive, endExclusive, by).boxed().<E> checked();
    }

    /**
     *
     *
     * @param <E>
     * @param startInclusive
     * @param endExclusive
     * @return
     */
    public static <E extends Exception> CheckedStream<Integer, E> rangeClosed(final int startInclusive, final int endExclusive) {
        return IntStream.rangeClosed(startInclusive, endExclusive).boxed().<E> checked();
    }

    /**
     *
     *
     * @param <E>
     * @param startInclusive
     * @param endExclusive
     * @param by
     * @return
     */
    public static <E extends Exception> CheckedStream<Integer, E> rangeClosed(final int startInclusive, final int endExclusive, final int by) {
        return IntStream.rangeClosed(startInclusive, endExclusive, by).boxed().<E> checked();
    }

    /**
     *
     *
     * @param <E>
     * @param str
     * @param delimiter
     * @return
     */
    public static <E extends Exception> CheckedStream<String, E> split(final CharSequence str, final CharSequence delimiter) {
        return Splitter.with(delimiter).splitToStream(str).checked();
    }

    /**
     *
     * @param file
     * @return
     */
    public static CheckedStream<String, IOException> lines(final File file) {
        return lines(file, IOUtil.DEFAULT_CHARSET);
    }

    /**
     *
     * @param file
     * @param charset
     * @return
     */
    public static CheckedStream<String, IOException> lines(final File file, final Charset charset) {
        N.checkArgNotNull(file, "file");

        final Throwables.Iterator<String, IOException> iter = createLazyLineIterator(file, null, charset, null, true);

        return newStream(iter).onClose(newCloseHandler(iter)); //NOSONAR
    }

    /**
     *
     * @param path
     * @return
     */
    public static CheckedStream<String, IOException> lines(final Path path) {
        return lines(path, IOUtil.DEFAULT_CHARSET);
    }

    /**
     *
     * @param path
     * @param charset
     * @return
     */
    public static CheckedStream<String, IOException> lines(final Path path, final Charset charset) {
        N.checkArgNotNull(path, "path");

        final Throwables.Iterator<String, IOException> iter = createLazyLineIterator(null, path, charset, null, true);

        return newStream(iter).onClose(newCloseHandler(iter));
    }

    /**
     *
     * @param reader
     * @return
     */
    public static CheckedStream<String, IOException> lines(final Reader reader) {
        N.checkArgNotNull(reader, "reader");

        return newStream(createLazyLineIterator(null, null, IOUtil.DEFAULT_CHARSET, reader, false));
    }

    /**
     *
     *
     * @param parentPath
     * @return
     */
    public static CheckedStream<File, IOException> listFiles(final File parentPath) {
        if (!parentPath.exists()) {
            return empty();
        }

        return of(parentPath.listFiles());
    }

    /**
     *
     *
     * @param parentPath
     * @param recursively
     * @return
     */
    public static CheckedStream<File, IOException> listFiles(final File parentPath, final boolean recursively) {
        if (!parentPath.exists()) {
            return empty();
        } else if (!recursively) {
            return of(parentPath.listFiles());
        }

        final Throwables.Iterator<File, IOException> iter = new Throwables.Iterator<>() {
            private final Queue<File> paths = N.asLinkedList(parentPath);
            private File[] subFiles = null;
            private int cursor = 0;

            public boolean hasNext() {
                if ((subFiles == null || cursor >= subFiles.length) && paths.size() > 0) {
                    cursor = 0;
                    subFiles = null;

                    while (paths.size() > 0) {
                        subFiles = paths.poll().listFiles();

                        if (N.notEmpty(subFiles)) {
                            break;
                        }
                    }
                }

                return subFiles != null && cursor < subFiles.length;
            }

            public File next() {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (subFiles[cursor].isDirectory()) {
                    paths.offer(subFiles[cursor]);
                }

                return subFiles[cursor++];
            }
        };

        return newStream(iter);
    }

    /**
     * Creates the lazy line iterator.
     *
     * @param file
     * @param path
     * @param charset
     * @param reader
     * @param closeReader
     * @return
     */
    private static Throwables.Iterator<String, IOException> createLazyLineIterator(final File file, final Path path, final Charset charset, final Reader reader,
            final boolean closeReader) {
        return Throwables.Iterator.defer(new Supplier<Throwables.Iterator<String, IOException>>() {
            private Throwables.Iterator<String, IOException> lazyIter = null;

            @Override
            public synchronized Throwables.Iterator<String, IOException> get() {
                if (lazyIter == null) {
                    lazyIter = new Throwables.Iterator<>() {
                        private BufferedReader bufferedReader;

                        { //NOSONAR
                            if (reader != null) {
                                bufferedReader = reader instanceof BufferedReader ? ((BufferedReader) reader) : new BufferedReader(reader);
                            } else if (file != null) {
                                bufferedReader = IOUtil.newBufferedReader(file, charset == null ? IOUtil.DEFAULT_CHARSET : charset);
                            } else {
                                bufferedReader = IOUtil.newBufferedReader(path, charset == null ? IOUtil.DEFAULT_CHARSET : charset);
                            }
                        }

                        private String cachedLine;
                        private boolean finished = false;

                        public boolean hasNext() throws IOException {
                            if (this.cachedLine != null) {
                                return true;
                            } else if (this.finished) {
                                return false;
                            } else {
                                this.cachedLine = this.bufferedReader.readLine();
                                if (this.cachedLine == null) {
                                    this.finished = true;
                                    return false;
                                } else {
                                    return true;
                                }
                            }
                        }

                        public String next() throws IOException {
                            if (!this.hasNext()) {
                                throw new NoSuchElementException("No more lines");
                            } else {
                                String res = this.cachedLine;
                                this.cachedLine = null;
                                return res;
                            }
                        }

                        protected void closeResource() {
                            if (closeReader) {
                                IOUtil.close(bufferedReader);
                            }
                        }
                    };
                }

                return lazyIter;
            }
        });
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T, E extends Exception> CheckedStream<T, E> concat(final T[]... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return of(Iterators.concat(a));
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T, E extends Exception> CheckedStream<T, E> concat(final Iterable<? extends T>... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return of(Iterators.concat(a));
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T, E extends Exception> CheckedStream<T, E> concat(final Iterator<? extends T>... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return of(Iterators.concat(a));
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param a
     * @return
     */
    @SafeVarargs
    public static <T, E extends Exception> CheckedStream<T, E> concat(final CheckedStream<? extends T, E>... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concat(Array.asList(a));
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> concat(final Collection<? extends CheckedStream<? extends T, E>> c) {
        if (N.isEmpty(c)) {
            return empty();
        }

        return newStream(new Throwables.Iterator<T, E>() {
            private final Iterator<? extends CheckedStream<? extends T, E>> iterators = c.iterator();
            private CheckedStream<? extends T, E> cur;
            private Throwables.Iterator<? extends T, E> iter;

            @Override
            public boolean hasNext() throws E {
                while ((iter == null || !iter.hasNext()) && iterators.hasNext()) {
                    if (cur != null) {
                        cur.close();
                    }

                    cur = iterators.next();
                    iter = cur == null ? null : cur.elements;
                }

                return iter != null && iter.hasNext();
            }

            @Override
            public T next() throws E {
                if ((iter == null || !iter.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return iter.next();
            }
        }, newCloseHandler(c));
    }

    /**
     * Zip together the "a" and "b" arrays until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     *
     * @param <A>
     * @param <B>
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param zipFunction
     * @return
     */
    public static <A, B, T, E extends Exception> CheckedStream<T, E> zip(final A[] a, final B[] b,
            final Throwables.BiFunction<? super A, ? super B, ? extends T, ? extends E> zipFunction) {
        return zip(ObjIterator.of(a), ObjIterator.of(b), zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param c
     * @param zipFunction
     * @return
     */
    public static <A, B, C, T, E extends Exception> CheckedStream<T, E> zip(final A[] a, final B[] b, final C[] c,
            final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends T, ? extends E> zipFunction) {
        return zip(ObjIterator.of(a), ObjIterator.of(b), ObjIterator.of(c), zipFunction);
    }

    /**
     * Zip together the "a" and "b" arrays until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     *
     * @param <A>
     * @param <B>
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param zipFunction
     * @return
     */
    public static <A, B, T, E extends Exception> CheckedStream<T, E> zip(final Iterable<? extends A> a, final Iterable<? extends B> b,
            final Throwables.BiFunction<? super A, ? super B, ? extends T, ? extends E> zipFunction) {
        return zip(N.iterate(a), N.iterate(b), zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param c
     * @param zipFunction
     * @return
     */
    public static <A, B, C, T, E extends Exception> CheckedStream<T, E> zip(final Iterable<? extends A> a, final Iterable<? extends B> b,
            final Iterable<? extends C> c, final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends T, ? extends E> zipFunction) {
        return zip(N.iterate(a), N.iterate(b), N.iterate(c), zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     *
     * @param <A>
     * @param <B>
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param zipFunction
     * @return
     */
    public static <A, B, T, E extends Exception> CheckedStream<T, E> zip(final Iterator<? extends A> a, final Iterator<? extends B> b,
            final Throwables.BiFunction<? super A, ? super B, ? extends T, ? extends E> zipFunction) {
        return newStream(new Throwables.Iterator<T, E>() {
            private final Iterator<? extends A> iterA = a == null ? ObjIterator.<A> empty() : a;
            private final Iterator<? extends B> iterB = b == null ? ObjIterator.<B> empty() : b;

            @Override
            public boolean hasNext() throws E {
                return iterA.hasNext() && iterB.hasNext();
            }

            @Override
            public T next() throws E {
                return zipFunction.apply(iterA.next(), iterB.next());
            }
        });
    }

    /**
     * Zip together the "a", "b" and "c" iterators until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param c
     * @param zipFunction
     * @return
     */
    public static <A, B, C, T, E extends Exception> CheckedStream<T, E> zip(final Iterator<? extends A> a, final Iterator<? extends B> b,
            final Iterator<? extends C> c, final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends T, ? extends E> zipFunction) {
        return newStream(new Throwables.Iterator<T, E>() {
            private final Iterator<? extends A> iterA = a == null ? ObjIterator.<A> empty() : a;
            private final Iterator<? extends B> iterB = b == null ? ObjIterator.<B> empty() : b;
            private final Iterator<? extends C> iterC = c == null ? ObjIterator.<C> empty() : c;

            @Override
            public boolean hasNext() throws E {
                return iterA.hasNext() && iterB.hasNext() && iterC.hasNext();
            }

            @Override
            public T next() throws E {
                return zipFunction.apply(iterA.next(), iterB.next(), iterC.next());
            }
        });
    }

    /**
     * Zip together the "a" and "b" streams until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     *
     * @param <A>
     * @param <B>
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param zipFunction
     * @return
     */
    public static <A, B, T, E extends Exception> CheckedStream<T, E> zip(final CheckedStream<? extends A, E> a, final CheckedStream<? extends B, E> b,
            final Throwables.BiFunction<? super A, ? super B, ? extends T, ? extends E> zipFunction) {
        return newStream(new Throwables.Iterator<T, E>() {
            private final Throwables.Iterator<? extends A, E> iterA = iterate(a);
            private final Throwables.Iterator<? extends B, E> iterB = iterate(b);

            @Override
            public boolean hasNext() throws E {
                return iterA.hasNext() && iterB.hasNext();
            }

            @Override
            public T next() throws E {
                return zipFunction.apply(iterA.next(), iterB.next());
            }
        }, newCloseHandler(Array.asList(a, b)));
    }

    /**
     * Zip together the "a", "b" and "c" streams until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param c
     * @param zipFunction
     * @return
     */
    public static <A, B, C, T, E extends Exception> CheckedStream<T, E> zip(final CheckedStream<? extends A, E> a, final CheckedStream<? extends B, E> b,
            final CheckedStream<? extends C, E> c, final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends T, ? extends E> zipFunction) {
        return newStream(new Throwables.Iterator<T, E>() {
            private final Throwables.Iterator<? extends A, E> iterA = iterate(a);
            private final Throwables.Iterator<? extends B, E> iterB = iterate(b);
            private final Throwables.Iterator<? extends C, E> iterC = iterate(c);

            @Override
            public boolean hasNext() throws E {
                return iterA.hasNext() && iterB.hasNext() && iterC.hasNext();
            }

            @Override
            public T next() throws E {
                return zipFunction.apply(iterA.next(), iterB.next(), iterC.next());
            }
        }, newCloseHandler(Array.asList(a, b, c)));
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     *
     * @param <A>
     * @param <B>
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <A, B, T, E extends Exception> CheckedStream<T, E> zip(final A[] a, final B[] b, final A valueForNoneA, final B valueForNoneB,
            final Throwables.BiFunction<? super A, ? super B, ? extends T, ? extends E> zipFunction) {
        return zip(ObjIterator.of(a), ObjIterator.of(b), valueForNoneA, valueForNoneB, zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <A, B, C, T, E extends Exception> CheckedStream<T, E> zip(final A[] a, final B[] b, final C[] c, final A valueForNoneA, final B valueForNoneB,
            final C valueForNoneC, final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends T, ? extends E> zipFunction) {
        return zip(ObjIterator.of(a), ObjIterator.of(b), ObjIterator.of(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     *
     * @param <A>
     * @param <B>
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <A, B, T, E extends Exception> CheckedStream<T, E> zip(final Iterable<? extends A> a, final Iterable<? extends B> b, final A valueForNoneA,
            final B valueForNoneB, final Throwables.BiFunction<? super A, ? super B, ? extends T, ? extends E> zipFunction) {
        return zip(N.iterate(a), N.iterate(b), valueForNoneA, valueForNoneB, zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <A, B, C, T, E extends Exception> CheckedStream<T, E> zip(final Iterable<? extends A> a, final Iterable<? extends B> b,
            final Iterable<? extends C> c, final A valueForNoneA, final B valueForNoneB, final C valueForNoneC,
            final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends T, ? extends E> zipFunction) {
        return zip(N.iterate(a), N.iterate(b), N.iterate(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     *
     * @param <A>
     * @param <B>
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <A, B, T, E extends Exception> CheckedStream<T, E> zip(final Iterator<? extends A> a, final Iterator<? extends B> b, final A valueForNoneA,
            final B valueForNoneB, final Throwables.BiFunction<? super A, ? super B, ? extends T, ? extends E> zipFunction) {
        return newStream(new Throwables.Iterator<T, E>() {
            private final Iterator<? extends A> iterA = a == null ? ObjIterator.<A> empty() : a;
            private final Iterator<? extends B> iterB = b == null ? ObjIterator.<B> empty() : b;

            @Override
            public boolean hasNext() throws E {
                return iterA.hasNext() || iterB.hasNext();
            }

            @Override
            public T next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return zipFunction.apply(iterA.hasNext() ? iterA.next() : valueForNoneA, iterB.hasNext() ? iterB.next() : valueForNoneB);
            }
        });
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <A, B, C, T, E extends Exception> CheckedStream<T, E> zip(final Iterator<? extends A> a, final Iterator<? extends B> b,
            final Iterator<? extends C> c, final A valueForNoneA, final B valueForNoneB, final C valueForNoneC,
            final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends T, ? extends E> zipFunction) {
        return newStream(new Throwables.Iterator<T, E>() {
            private final Iterator<? extends A> iterA = a == null ? ObjIterator.<A> empty() : a;
            private final Iterator<? extends B> iterB = b == null ? ObjIterator.<B> empty() : b;
            private final Iterator<? extends C> iterC = c == null ? ObjIterator.<C> empty() : c;

            @Override
            public boolean hasNext() throws E {
                return iterA.hasNext() || iterB.hasNext() || iterC.hasNext();
            }

            @Override
            public T next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return zipFunction.apply(iterA.hasNext() ? iterA.next() : valueForNoneA, iterB.hasNext() ? iterB.next() : valueForNoneB,
                        iterC.hasNext() ? iterC.next() : valueForNoneC);
            }
        });
    }

    /**
     * Zip together the "a" and "b" streams until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     *
     * @param <A>
     * @param <B>
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param valueForNoneA
     * @param valueForNoneB
     * @param zipFunction
     * @return
     */
    public static <A, B, T, E extends Exception> CheckedStream<T, E> zip(final CheckedStream<? extends A, E> a, final CheckedStream<? extends B, E> b,
            final A valueForNoneA, final B valueForNoneB, final Throwables.BiFunction<? super A, ? super B, ? extends T, ? extends E> zipFunction) {
        return newStream(new Throwables.Iterator<T, E>() {
            private final Throwables.Iterator<? extends A, E> iterA = iterate(a);
            private final Throwables.Iterator<? extends B, E> iterB = iterate(b);

            @Override
            public boolean hasNext() throws E {
                return iterA.hasNext() || iterB.hasNext();
            }

            @Override
            public T next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return zipFunction.apply(iterA.hasNext() ? iterA.next() : valueForNoneA, iterB.hasNext() ? iterB.next() : valueForNoneB);
            }
        }, newCloseHandler(Array.asList(a, b)));
    }

    /**
     * Zip together the "a", "b" and "c" streams until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA
     * @param valueForNoneB
     * @param valueForNoneC
     * @param zipFunction
     * @return
     */
    public static <A, B, C, T, E extends Exception> CheckedStream<T, E> zip(final CheckedStream<? extends A, E> a, final CheckedStream<? extends B, E> b,
            final CheckedStream<? extends C, E> c, final A valueForNoneA, final B valueForNoneB, final C valueForNoneC,
            final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends T, ? extends E> zipFunction) {
        return newStream(new Throwables.Iterator<T, E>() {
            private final Throwables.Iterator<? extends A, E> iterA = iterate(a);
            private final Throwables.Iterator<? extends B, E> iterB = iterate(b);
            private final Throwables.Iterator<? extends C, E> iterC = iterate(c);

            @Override
            public boolean hasNext() throws E {
                return iterA.hasNext() || iterB.hasNext() || iterC.hasNext();
            }

            @Override
            public T next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return zipFunction.apply(iterA.hasNext() ? iterA.next() : valueForNoneA, iterB.hasNext() ? iterB.next() : valueForNoneB,
                        iterC.hasNext() ? iterC.next() : valueForNoneC);
            }
        }, newCloseHandler(Array.asList(a, b, c)));
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> merge(final T[] a, final T[] b,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        if (N.isEmpty(a)) {
            return of(b);
        } else if (N.isEmpty(b)) {
            return of(a);
        }

        return newStream(new Throwables.Iterator<T, E>() {
            private final int lenA = a.length;
            private final int lenB = b.length;
            private int cursorA = 0;
            private int cursorB = 0;

            @Override
            public boolean hasNext() {
                return cursorA < lenA || cursorB < lenB;
            }

            @Override
            public T next() throws E {
                if (cursorA < lenA) {
                    if (cursorB < lenB) {
                        if (nextSelector.apply(a[cursorA], b[cursorB]) == MergeResult.TAKE_FIRST) {
                            return a[cursorA++];
                        } else {
                            return b[cursorB++];
                        }
                    } else {
                        return a[cursorA++];
                    }
                } else if (cursorB < lenB) {
                    return b[cursorB++];
                } else {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }
            }
        });
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> merge(final T[] a, final T[] b, final T[] c,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return merge(merge(a, b, nextSelector).iteratorEx(), Throwables.Iterator.<T, E> of(N.iterate(c)), nextSelector);
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> merge(final Iterable<? extends T> a, final Iterable<? extends T> b,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return merge(N.iterate(a), N.iterate(b), nextSelector);
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> merge(final Iterable<? extends T> a, final Iterable<? extends T> b,
            final Iterable<? extends T> c, final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return merge(N.iterate(a), N.iterate(b), N.iterate(c), nextSelector);
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> merge(final Iterator<? extends T> a, final Iterator<? extends T> b,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return merge(Throwables.Iterator.<T, E> of(a), Throwables.Iterator.<T, E> of(b), nextSelector);
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> merge(final Iterator<? extends T> a, final Iterator<? extends T> b,
            final Iterator<? extends T> c, final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return merge(merge(a, b, nextSelector).iteratorEx(), Throwables.Iterator.<T, E> of(c), nextSelector);
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> merge(final CheckedStream<? extends T, E> a, final CheckedStream<? extends T, E> b,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return merge(iterate(a), iterate(b), nextSelector).onClose(() -> {
            try {
                if (a != null) {
                    a.close();
                }
            } finally {
                if (b != null) {
                    b.close();
                }
            }
        });
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param c
     * @param nextSelector
     * @return
     */
    public static <T, E extends Exception> CheckedStream<T, E> merge(final CheckedStream<? extends T, E> a, final CheckedStream<? extends T, E> b,
            final CheckedStream<? extends T, E> c, final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return merge(merge(a, b, nextSelector), c, nextSelector);
    }

    static <T, E extends Exception> CheckedStream<T, E> merge(final Throwables.Iterator<? extends T, E> a, final Throwables.Iterator<? extends T, E> b,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return newStream(new Throwables.Iterator<T, E>() {
            private final Throwables.Iterator<T, E> iterA = a == null ? Throwables.Iterator.empty() : (Throwables.Iterator<T, E>) a;
            private final Throwables.Iterator<T, E> iterB = b == null ? Throwables.Iterator.empty() : (Throwables.Iterator<T, E>) b;

            private T nextA = null;
            private T nextB = null;
            private boolean hasNextA = false;
            private boolean hasNextB = false;

            @Override
            public boolean hasNext() throws E {
                return hasNextA || hasNextB || iterA.hasNext() || iterB.hasNext();
            }

            @Override
            public T next() throws E {
                if (hasNextA) {
                    if (iterB.hasNext()) {
                        if (nextSelector.apply(nextA, (nextB = iterB.next())) == MergeResult.TAKE_FIRST) {
                            hasNextA = false;
                            hasNextB = true;
                            return nextA;
                        } else {
                            return nextB;
                        }
                    } else {
                        hasNextA = false;
                        return nextA;
                    }
                } else if (hasNextB) {
                    if (iterA.hasNext()) {
                        if (nextSelector.apply((nextA = iterA.next()), nextB) == MergeResult.TAKE_FIRST) {
                            return nextA;
                        } else {
                            hasNextA = true;
                            hasNextB = false;
                            return nextB;
                        }
                    } else {
                        hasNextB = false;
                        return nextB;
                    }
                } else if (iterA.hasNext()) {
                    if (iterB.hasNext()) {
                        if (nextSelector.apply((nextA = iterA.next()), (nextB = iterB.next())) == MergeResult.TAKE_FIRST) {
                            hasNextB = true;
                            return nextA;
                        } else {
                            hasNextA = true;
                            return nextB;
                        }
                    } else {
                        return iterA.next();
                    }
                } else if (iterB.hasNext()) {
                    return iterB.next();
                } else {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }
            }
        });
    }

    /**
     *
     * @param predicate
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> filter(final Throwables.Predicate<? super T, ? extends E> predicate) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<T, E>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() throws E {
                if (!hasNext) {
                    while (elements.hasNext()) {
                        next = elements.next();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
                        }
                    }
                }

                return hasNext;
            }

            @Override
            public T next() throws E {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }
        }, sorted, cmp, closeHandlers);
    }

    /**
     *
     * @param predicate
     * @param actionOnDroppedItem
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> filter(final Throwables.Predicate<? super T, ? extends E> predicate,
            final Throwables.Consumer<? super T, ? extends E> actionOnDroppedItem) {
        assertNotClosed();

        return filter(value -> {
            if (!predicate.test(value)) {
                actionOnDroppedItem.accept(value);
                return false;
            }

            return true;
        });
    }

    /**
     *
     * @param predicate
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> takeWhile(final Throwables.Predicate<? super T, ? extends E> predicate) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<T, E>() {
            private boolean hasMore = true;
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() throws E {
                if (!hasNext && hasMore && elements.hasNext()) {
                    next = elements.next();

                    if (predicate.test(next)) {
                        hasNext = true;
                    } else {
                        hasMore = false;
                    }
                }

                return hasNext;
            }

            @Override
            public T next() throws E {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }
        }, sorted, cmp, closeHandlers);
    }

    /**
     *
     * @param predicate
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> dropWhile(final Throwables.Predicate<? super T, ? extends E> predicate) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<T, E>() {
            private boolean hasNext = false;
            private T next = null;
            private boolean dropped = false;

            @Override
            public boolean hasNext() throws E {
                if (!hasNext) {
                    if (!dropped) {
                        dropped = true;

                        while (elements.hasNext()) {
                            next = elements.next();

                            if (!predicate.test(next)) {
                                hasNext = true;
                                break;
                            }
                        }
                    } else if (elements.hasNext()) {
                        next = elements.next();
                        hasNext = true;
                    }
                }

                return hasNext;
            }

            @Override
            public T next() throws E {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }

        }, sorted, cmp, closeHandlers);
    }

    /**
     *
     * @param predicate
     * @param actionOnDroppedItem
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> dropWhile(final Throwables.Predicate<? super T, ? extends E> predicate,
            final Throwables.Consumer<? super T, ? extends E> actionOnDroppedItem) {
        assertNotClosed();

        return dropWhile(value -> {
            if (!predicate.test(value)) {
                actionOnDroppedItem.accept(value);
                return false;
            }

            return true;
        });
    }

    /**
     *
     * @param predicate
     * @return
     */
    @Beta
    @IntermediateOp
    public CheckedStream<T, E> skipUntil(final Throwables.Predicate<? super T, ? extends E> predicate) {
        return dropWhile(Fnn.not(predicate));
    }

    /**
     * Distinct and filter by occurrences.
     *
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> distinct() {
        assertNotClosed();

        final Set<Object> set = N.newHashSet();

        return filter(value -> set.add(hashKey(value)));
    }

    /**
     * Distinct and filter by occurrences.
     *
     * @param mergeFunction
     * @return
     * @see #groupBy(Throwables.Function, Throwables.Function, Throwables.BinaryOperator)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public CheckedStream<T, E> distinct(final Throwables.BinaryOperator<T, ? extends E> mergeFunction) {
        return distinctBy(Fnn.identity(), mergeFunction);
    }

    /**
     * Distinct and filter by occurrences.
     *
     * @param occurrencesFilter
     * @return
     * @see #groupBy(Throwables.Function, Collector)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public CheckedStream<T, E> distinct(final Throwables.Predicate<? super Long, ? extends E> occurrencesFilter) {
        assertNotClosed();

        final Supplier<? extends Map<T, Long>> supplier = Suppliers.<T, Long> ofLinkedHashMap();

        final Throwables.Predicate<Map.Entry<T, Long>, ? extends E> predicate = e -> occurrencesFilter.test(e.getValue());

        return newStream(groupBy(Fnn.identity(), Collectors.counting(), supplier).filter(predicate).map(Fnn.key()).iteratorEx(), sorted, cmp, closeHandlers);
    }

    /**
     * Distinct by the value mapped from <code>keyMapper</code> .
     *
     * @param <K>
     * @param keyMapper don't change value of the input parameter.
     * @return
     */
    @IntermediateOp
    public <K> CheckedStream<T, E> distinctBy(final Throwables.Function<? super T, K, ? extends E> keyMapper) {
        assertNotClosed();

        final Set<Object> set = N.newHashSet();

        return filter(value -> set.add(hashKey(keyMapper.apply(value))));
    }

    /**
     * Distinct and filter by occurrences.
     *
     * @param <K>
     * @param keyMapper
     * @param mergeFunction
     * @return
     * @see #groupBy(Throwables.Function, Throwables.Function, Throwables.BinaryOperator)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K> CheckedStream<T, E> distinctBy(final Throwables.Function<? super T, K, ? extends E> keyMapper,
            final Throwables.BinaryOperator<T, ? extends E> mergeFunction) {
        assertNotClosed();

        final Supplier<? extends Map<K, T>> supplier = Suppliers.<K, T> ofLinkedHashMap();

        return groupBy(keyMapper, Fnn.<T, E> identity(), mergeFunction, supplier).map(Fnn.<K, T, E> value());
    }

    /**
     * Distinct and filter by occurrences.
     *
     * @param <K>
     * @param keyMapper
     * @param occurrencesFilter
     * @return
     * @see #groupBy(Throwables.Function, Collector)
     */
    @IntermediateOp
    @TerminalOpTriggered
    @SuppressWarnings("rawtypes")
    public <K> CheckedStream<T, E> distinctBy(final Throwables.Function<? super T, K, ? extends E> keyMapper,
            final Throwables.Predicate<? super Map.Entry<Keyed<K, T>, Long>, ? extends E> occurrencesFilter) {
        assertNotClosed();

        final Supplier<? extends Map<Keyed<K, T>, Long>> supplier = Suppliers.<Keyed<K, T>, Long> ofLinkedHashMap();

        final Throwables.Function<T, Keyed<K, T>, E> keyedMapper = t -> Keyed.of(keyMapper.apply(t), t);

        return newStream(groupBy(keyedMapper, Collectors.counting(), supplier).filter(occurrencesFilter)
                .map((Throwables.Function<Map.Entry<Keyed<K, T>, Long>, T, E>) (Throwables.Function) KK)
                .iteratorEx(), sorted, cmp, closeHandlers);
    }

    /**
     * Distinct and limit by {@code keyMapper}.
     *
     * @param <K>
     * @param keyMapper
     * @param limit
     * @return
     * @see #groupBy(Throwables.Function, Collector)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K> CheckedStream<T, E> distinctLimitBy(final Throwables.Function<? super T, K, ? extends E> keyMapper,
            final Throwables.BiFunction<? super K, ? super List<T>, Integer, ? extends E> limit) {

        final Supplier<Map<K, List<T>>> supplier = Suppliers.<K, List<T>> ofLinkedHashMap();

        return groupBy(keyMapper, Fnn.identity(), supplier) //
                .flatmap(it -> subList(it.getValue(), 0, limit.apply(it.getKey(), it.getValue())));
    }

    /**
     *
     * @param <R>
     * @param mapper
     * @return
     */
    @IntermediateOp
    public <R> CheckedStream<R, E> map(final Throwables.Function<? super T, ? extends R, ? extends E> mapper) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<R, E>() {
            @Override
            public boolean hasNext() throws E {
                return elements.hasNext();
            }

            @Override
            public R next() throws E {
                return mapper.apply(elements.next());
            }
        }, closeHandlers);
    }

    /**
     * Convert the element to a new value if it's not {@code null}, otherwise skip it.
     *
     * @param <R>
     * @param mapper
     * @return
     * @implSpec Same as {@code skipNulls().map(mapper)}.
     */
    @Beta
    @IntermediateOp
    public <R> CheckedStream<R, E> mapIfNotNull(final Throwables.Function<? super T, ? extends R, ? extends E> mapper) {
        return skipNulls().map(mapper);
    }

    /**
     *
     *
     * @param mapperForFirst
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> mapFirst(final Throwables.Function<? super T, ? extends T, ? extends E> mapperForFirst) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<T, E>() {
            private boolean isFirst = true;

            @Override
            public boolean hasNext() throws E {
                return elements.hasNext();
            }

            @Override
            public T next() throws E {
                if (isFirst) {
                    isFirst = false;
                    return mapperForFirst.apply(elements.next());
                } else {
                    return elements.next();
                }
            }
        }, closeHandlers);
    }

    /**
     *
     *
     * @param <R>
     * @param mapperForFirst
     * @param mapperForElse
     * @return
     */
    @IntermediateOp
    public <R> CheckedStream<R, E> mapFirstOrElse(final Throwables.Function<? super T, ? extends R, E> mapperForFirst,
            final Throwables.Function<? super T, ? extends R, E> mapperForElse) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<R, E>() {
            private boolean isFirst = true;

            @Override
            public boolean hasNext() throws E {
                return elements.hasNext();
            }

            @Override
            public R next() throws E {
                if (isFirst) {
                    isFirst = false;
                    return mapperForFirst.apply(elements.next());
                } else {
                    return mapperForElse.apply(elements.next());
                }
            }
        }, closeHandlers);
    }

    /**
     *
     *
     * @param mapperForLast
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> mapLast(final Throwables.Function<? super T, ? extends T, ? extends E> mapperForLast) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<T, E>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() throws E {
                return hasNext || elements.hasNext();
            }

            @Override
            public T next() throws E {
                next = elements.next();

                if (hasNext = elements.hasNext()) {
                    return next;
                } else {
                    return mapperForLast.apply(next);
                }
            }
        }, closeHandlers);
    }

    /**
     *
     *
     * @param <R>
     * @param mapperForLast
     * @param mapperForElse
     * @return
     */
    @IntermediateOp
    public <R> CheckedStream<R, E> mapLastOrElse(final Throwables.Function<? super T, ? extends R, E> mapperForLast,
            final Throwables.Function<? super T, ? extends R, E> mapperForElse) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<R, E>() {
            private T next = null;

            @Override
            public boolean hasNext() throws E {
                return elements.hasNext();
            }

            @Override
            public R next() throws E {
                next = elements.next();

                if (elements.hasNext()) {
                    return mapperForElse.apply(next);
                } else {
                    return mapperForLast.apply(next);
                }
            }
        }, closeHandlers);
    }

    /**
     *
     * @param <R>
     * @param mapper
     * @return
     */
    @IntermediateOp
    public <R> CheckedStream<R, E> flatMap(final Throwables.Function<? super T, ? extends CheckedStream<? extends R, ? extends E>, ? extends E> mapper) {
        assertNotClosed();

        final Throwables.Iterator<R, E> iter = new Throwables.Iterator<>() {
            private CheckedStream<? extends R, ? extends E> s = null;
            private Throwables.Iterator<? extends R, ? extends E> cur = null;

            public boolean hasNext() throws E {
                while (cur == null || !cur.hasNext()) {
                    if (elements.hasNext()) {
                        if (s != null) {
                            s.close();
                            s = null;
                        }

                        s = mapper.apply(elements.next());

                        if (s == null) {
                            cur = null;
                        } else {
                            cur = s.elements;
                        }
                    } else {
                        cur = null;
                        break;
                    }
                }

                return cur != null && cur.hasNext();
            }

            public R next() throws E {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }

            protected void closeResource() {
                if (s != null) {
                    s.close();
                }
            }
        };

        return newStream(iter, mergeCloseHandlers(iter::close, closeHandlers));
    }

    /**
     *
     * @param <R>
     * @param mapper
     * @return
     */
    @IntermediateOp
    public <R> CheckedStream<R, E> flatmap(final Throwables.Function<? super T, ? extends Collection<? extends R>, ? extends E> mapper) { //NOSONAR
        assertNotClosed();

        return newStream(new Throwables.Iterator<R, E>() {
            private Collection<? extends R> c = null;
            private Iterator<? extends R> cur = null;

            @Override
            public boolean hasNext() throws E {
                while ((cur == null || !cur.hasNext()) && elements.hasNext()) {
                    c = mapper.apply(elements.next());
                    cur = N.isEmpty(c) ? null : c.iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public R next() throws E {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }
        }, closeHandlers);
    }

    /**
     *
     * @param <R>
     * @param mapper
     * @return
     */
    @IntermediateOp
    @Beta
    public <R> CheckedStream<R, E> flattMap(final Throwables.Function<? super T, R[], ? extends E> mapper) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<R, E>() {
            private R[] cur = null;
            private int len = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() throws E {
                while (idx >= len) {
                    if (elements.hasNext()) {
                        cur = mapper.apply(elements.next());
                        len = N.len(cur);
                        idx = 0;
                    } else {
                        cur = null;
                        break;
                    }
                }

                return idx < len;
            }

            @Override
            public R next() throws E {
                if (idx >= len && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[idx++];
            }
        }, closeHandlers);
    }

    /**
     *
     * @param <R>
     * @param mapper
     * @return
     */
    @Beta
    @IntermediateOp
    public <R> CheckedStream<R, E> flattmap(final Throwables.Function<? super T, ? extends Stream<? extends R>, ? extends E> mapper) { //NOSONAR
        assertNotClosed();

        final Throwables.Iterator<R, E> iter = new Throwables.Iterator<>() {
            private Stream<? extends R> s = null;
            private Iterator<? extends R> cur = null;

            public boolean hasNext() throws E {
                while (cur == null || !cur.hasNext()) {
                    if (elements.hasNext()) {
                        if (s != null) {
                            s.close();
                            s = null;
                        }

                        s = mapper.apply(elements.next());

                        if (s == null) {
                            cur = null;
                        } else {
                            cur = s.iterator();
                        }
                    } else {
                        cur = null;
                        break;
                    }
                }

                return cur != null && cur.hasNext();
            }

            public R next() throws E {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }

            protected void closeResource() {
                if (s != null) {
                    s.close();
                }
            }
        };

        return newStream(iter, mergeCloseHandlers(iter::close, closeHandlers));
    }

    //    /**
    //     *
    //     * @param <R>
    //     * @param mapper
    //     * @return
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, E> flatMapByStream(final Throwables.Function<? super T, ? extends Stream<? extends R>, ? extends E> mapper) {
    //        assertNotClosed();
    //        final Throwables.Iterator<R, E> iter = new Throwables.Iterator<R, E>() {
    //            private Stream<? extends R> s = null;
    //            private Iterator<? extends R> cur = null;
    //
    //                //            public boolean hasNext() throws E {
    //                while (cur == null || cur.hasNext() == false) {
    //                    if (elements.hasNext()) {
    //                        if (s != null) {
    //                            s.close();
    //                            s = null;
    //                        }
    //
    //                        s = mapper.apply(elements.next());
    //
    //                        if (s == null) {
    //                            cur = null;
    //                        } else {
    //                            cur = s.iterator();
    //                        }
    //                    } else {
    //                        cur = null;
    //                        break;
    //                    }
    //                }
    //
    //                return cur != null && cur.hasNext();
    //            }
    //
    //                //            public R next() throws E {
    //                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                return cur.next();
    //            }
    //
    //                //            public void close()   {
    //                if (s != null) {
    //                    s.close();
    //                }
    //            }
    //        };
    //
    //        final Deque<Throwables.Runnable<? extends E>> newCloseHandlers = new ArrayDeque<>(N.size(closeHandlers) + 1);
    //
    //        if (N.notEmpty(closeHandlers)) {
    //            newCloseHandlers.addAll(closeHandlers);
    //        }
    //
    //        newCloseHandlers.add(newCloseHandler(iter));
    //
    //        return newStream(iter, newCloseHandlers);
    //    }
    //
    //    /**
    //     *
    //     * @param <R>
    //     * @param mapper
    //     * @return
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, E> flatMapByStreamJdk(
    //            final Throwables.Function<? super T, ? extends java.util.stream.Stream<? extends R>, ? extends E> mapper) {
    //        assertNotClosed();
    //        final Throwables.Iterator<R, E> iter = new Throwables.Iterator<R, E>() {
    //            private java.util.stream.Stream<? extends R> s = null;
    //            private Iterator<? extends R> cur = null;
    //
    //                //            public boolean hasNext() throws E {
    //                while (cur == null || cur.hasNext() == false) {
    //                    if (elements.hasNext()) {
    //                        if (s != null) {
    //                            s.close();
    //                            s = null;
    //                        }
    //
    //                        s = mapper.apply(elements.next());
    //
    //                        if (s == null) {
    //                            cur = null;
    //                        } else {
    //                            cur = s.iterator();
    //                        }
    //                    } else {
    //                        cur = null;
    //                        break;
    //                    }
    //                }
    //
    //                return cur != null && cur.hasNext();
    //            }
    //
    //                //            public R next() throws E {
    //                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                return cur.next();
    //            }
    //
    //                //            public void close()   {
    //                if (s != null) {
    //                    s.close();
    //                }
    //            }
    //        };
    //
    //        final Deque<Throwables.Runnable<? extends E>> newCloseHandlers = new ArrayDeque<>(N.size(closeHandlers) + 1);
    //
    //        if (N.notEmpty(closeHandlers)) {
    //            newCloseHandlers.addAll(closeHandlers);
    //        }
    //
    //        newCloseHandlers.add(newCloseHandler(iter));
    //
    //        return newStream(iter, newCloseHandlers);
    //    }

    /**
     *
     *
     * @param <R>
     * @param mapper
     * @return
     * @implNote same as ====>
     * <pre>
     * skipNulls().flatmap(mapper)
     * </pre>
     */
    @Beta
    @IntermediateOp
    public <R> CheckedStream<R, E> flatMapIfNotNull(final Throwables.Function<? super T, ? extends Collection<? extends R>, ? extends E> mapper) {
        return skipNulls().flatmap(mapper);
    }

    /**
     *
     *
     * @param <U>
     * @param <R>
     * @param mapper
     * @param mapper2
     * @return
     * @implNote same as ====>
     * <pre>
     * skipNulls().flatmap(mapper).skipNulls().flatmap(mapper2)
     * </pre>
     */
    @Beta
    @IntermediateOp
    public <U, R> CheckedStream<R, E> flatMapIfNotNull(final Throwables.Function<? super T, ? extends Collection<? extends U>, ? extends E> mapper,
            final Throwables.Function<? super U, ? extends Collection<? extends R>, ? extends E> mapper2) {
        return skipNulls().flatmap(mapper).skipNulls().flatmap(mapper2);
    }

    /**
     *
     *
     * @param mapper
     * @return
     */
    @Beta
    @IntermediateOp
    public CheckedStream<Boolean, E> flatMapToBoolean(final Throwables.Function<? super T, boolean[], ? extends E> mapper) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<Boolean, E>() {
            private boolean[] cur = null;
            private int len = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() throws E {
                while (idx >= len) {
                    if (elements.hasNext()) {
                        cur = mapper.apply(elements.next());
                        len = N.len(cur);
                        idx = 0;
                    } else {
                        cur = null;
                        break;
                    }
                }

                return idx < len;
            }

            @Override
            public Boolean next() throws E {
                if (idx >= len && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[idx++];
            }
        }, closeHandlers);
    }

    /**
     *
     *
     * @param mapper
     * @return
     */
    @Beta
    @IntermediateOp
    public CheckedStream<Character, E> flatMapToChar(final Throwables.Function<? super T, char[], ? extends E> mapper) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<Character, E>() {
            private char[] cur = null;
            private int len = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() throws E {
                while (idx >= len) {
                    if (elements.hasNext()) {
                        cur = mapper.apply(elements.next());
                        len = N.len(cur);
                        idx = 0;
                    } else {
                        cur = null;
                        break;
                    }
                }

                return idx < len;
            }

            @Override
            public Character next() throws E {
                if (idx >= len && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[idx++];
            }
        }, closeHandlers);
    }

    /**
     *
     *
     * @param mapper
     * @return
     */
    @Beta
    @IntermediateOp
    public CheckedStream<Byte, E> flatMapToByte(final Throwables.Function<? super T, byte[], ? extends E> mapper) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<Byte, E>() {
            private byte[] cur = null;
            private int len = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() throws E {
                while (idx >= len) {
                    if (elements.hasNext()) {
                        cur = mapper.apply(elements.next());
                        len = N.len(cur);
                        idx = 0;
                    } else {
                        cur = null;
                        break;
                    }
                }

                return idx < len;
            }

            @Override
            public Byte next() throws E {
                if (idx >= len && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[idx++];
            }
        }, closeHandlers);
    }

    /**
     *
     *
     * @param mapper
     * @return
     */
    @Beta
    @IntermediateOp
    public CheckedStream<Short, E> flatMapToShort(final Throwables.Function<? super T, short[], ? extends E> mapper) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<Short, E>() {
            private short[] cur = null;
            private int len = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() throws E {
                while (idx >= len) {
                    if (elements.hasNext()) {
                        cur = mapper.apply(elements.next());
                        len = N.len(cur);
                        idx = 0;
                    } else {
                        cur = null;
                        break;
                    }
                }

                return idx < len;
            }

            @Override
            public Short next() throws E {
                if (idx >= len && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[idx++];
            }
        }, closeHandlers);
    }

    /**
     *
     *
     * @param mapper
     * @return
     */
    @Beta
    @IntermediateOp
    public CheckedStream<Integer, E> flatMapToInteger(final Throwables.Function<? super T, int[], ? extends E> mapper) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<Integer, E>() {
            private int[] cur = null;
            private int len = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() throws E {
                while (idx >= len) {
                    if (elements.hasNext()) {
                        cur = mapper.apply(elements.next());
                        len = N.len(cur);
                        idx = 0;
                    } else {
                        cur = null;
                        break;
                    }
                }

                return idx < len;
            }

            @Override
            public Integer next() throws E {
                if (idx >= len && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[idx++];
            }
        }, closeHandlers);
    }

    /**
     *
     *
     * @param mapper
     * @return
     */
    @Beta
    @IntermediateOp
    public CheckedStream<Long, E> flatMapToLong(final Throwables.Function<? super T, long[], ? extends E> mapper) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<Long, E>() {
            private long[] cur = null;
            private int len = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() throws E {
                while (idx >= len) {
                    if (elements.hasNext()) {
                        cur = mapper.apply(elements.next());
                        len = N.len(cur);
                        idx = 0;
                    } else {
                        cur = null;
                        break;
                    }
                }

                return idx < len;
            }

            @Override
            public Long next() throws E {
                if (idx >= len && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[idx++];
            }
        }, closeHandlers);
    }

    /**
     *
     *
     * @param mapper
     * @return
     */
    @Beta
    @IntermediateOp
    public CheckedStream<Float, E> flatMapToFloat(final Throwables.Function<? super T, float[], ? extends E> mapper) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<Float, E>() {
            private float[] cur = null;
            private int len = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() throws E {
                while (idx >= len) {
                    if (elements.hasNext()) {
                        cur = mapper.apply(elements.next());
                        len = N.len(cur);
                        idx = 0;
                    } else {
                        cur = null;
                        break;
                    }
                }

                return idx < len;
            }

            @Override
            public Float next() throws E {
                if (idx >= len && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[idx++];
            }
        }, closeHandlers);
    }

    /**
     *
     *
     * @param mapper
     * @return
     */
    @Beta
    @IntermediateOp
    public CheckedStream<Double, E> flatMapToDouble(final Throwables.Function<? super T, double[], ? extends E> mapper) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<Double, E>() {
            private double[] cur = null;
            private int len = 0;
            private int idx = 0;

            @Override
            public boolean hasNext() throws E {
                while (idx >= len) {
                    if (elements.hasNext()) {
                        cur = mapper.apply(elements.next());
                        len = N.len(cur);
                        idx = 0;
                    } else {
                        cur = null;
                        break;
                    }
                }

                return idx < len;
            }

            @Override
            public Double next() throws E {
                if (idx >= len && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur[idx++];
            }
        }, closeHandlers);
    }

    //    /**
    //     *
    //     * @param mapper
    //     * @return
    //     */
    //    @IntermediateOp
    //    public Seq<Integer, E> flatmapToInt(final Throwables.Function<? super T, ? extends int[], ? extends E> mapper) {
    //        final Throwables.Function<T, Seq<Integer, E>, E> mapper2 = new Throwables.Function<T, Seq<Integer, E>, E>() {
    //                //            public Seq<Integer, E> apply(T t) throws E {
    //                return Seq.of(mapper.apply(t));
    //            }
    //        };
    //
    //        return flatMap(mapper2);
    //    }
    //
    //    /**
    //     *
    //     * @param mapper
    //     * @return
    //     */
    //    @IntermediateOp
    //    public Seq<Long, E> flatmapToLong(final Throwables.Function<? super T, ? extends long[], ? extends E> mapper) {
    //        final Throwables.Function<T, Seq<Long, E>, E> mapper2 = new Throwables.Function<T, Seq<Long, E>, E>() {
    //                //            public Seq<Long, E> apply(T t) throws E {
    //                return Seq.of(mapper.apply(t));
    //            }
    //        };
    //
    //        return flatMap(mapper2);
    //    }
    //
    //    /**
    //     *
    //     * @param mapper
    //     * @return
    //     */
    //    @IntermediateOp
    //    public Seq<Double, E> flatmapToDouble(final Throwables.Function<? super T, ? extends double[], ? extends E> mapper) {
    //        final Throwables.Function<T, Seq<Double, E>, E> mapper2 = new Throwables.Function<T, Seq<Double, E>, E>() {
    //                //            public Seq<Double, E> apply(T t) throws E {
    //                return Seq.of(mapper.apply(t));
    //            }
    //        };
    //
    //        return flatMap(mapper2);
    //    }

    /**
     * Note: copied from StreamEx: https://github.com/amaembo/streamex
     *
     * @param <R>
     * @param mapper
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Beta
    @IntermediateOp
    public <R> CheckedStream<R, E> mapPartial(final Throwables.Function<? super T, Optional<? extends R>, E> mapper) {
        return map(mapper).filter((Throwables.Predicate) IS_PRESENT_IT).map(GET_AS_IT);
    }

    /**
     * Note: copied from StreamEx: https://github.com/amaembo/streamex
     *
     * @param mapper
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Beta
    @IntermediateOp
    public CheckedStream<Integer, E> mapPartialToInt(final Throwables.Function<? super T, OptionalInt, E> mapper) {
        return map(mapper).filter((Throwables.Predicate) IS_PRESENT_INT).map(GET_AS_INT);
    }

    /**
     * Note: copied from StreamEx: https://github.com/amaembo/streamex
     *
     * @param mapper
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Beta
    @IntermediateOp
    public CheckedStream<Long, E> mapPartialToLong(final Throwables.Function<? super T, OptionalLong, E> mapper) {
        return map(mapper).filter((Throwables.Predicate) IS_PRESENT_LONG).map(GET_AS_LONG);
    }

    /**
     * Note: copied from StreamEx: https://github.com/amaembo/streamex
     *
     * @param mapper
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Beta
    @IntermediateOp
    public CheckedStream<Double, E> mapPartialToDouble(final Throwables.Function<? super T, OptionalDouble, E> mapper) {
        return map(mapper).filter((Throwables.Predicate) IS_PRESENT_DOUBLE).map(GET_AS_DOUBLE);
    }

    /**
     * Note: copied from StreamEx: https://github.com/amaembo/streamex
     *
     * @param <R>
     * @param mapper
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Beta
    @IntermediateOp
    public <R> CheckedStream<R, E> mapPartialJdk(final Throwables.Function<? super T, java.util.Optional<? extends R>, E> mapper) {
        return map(mapper).filter((Throwables.Predicate) IS_PRESENT_IT_JDK).map(GET_AS_IT_JDK);
    }

    /**
     * Note: copied from StreamEx: https://github.com/amaembo/streamex
     *
     * @param mapper
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Beta
    @IntermediateOp
    public CheckedStream<Integer, E> mapPartialToIntJdk(final Throwables.Function<? super T, java.util.OptionalInt, E> mapper) {
        return map(mapper).filter((Throwables.Predicate) IS_PRESENT_INT_JDK).map(GET_AS_INT_JDK);
    }

    /**
     * Note: copied from StreamEx: https://github.com/amaembo/streamex
     *
     * @param mapper
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Beta
    @IntermediateOp
    public CheckedStream<Long, E> mapPartialToLongJdk(final Throwables.Function<? super T, java.util.OptionalLong, E> mapper) {
        return map(mapper).filter((Throwables.Predicate) IS_PRESENT_LONG_JDK).map(GET_AS_LONG_JDK);
    }

    /**
     * Note: copied from StreamEx: https://github.com/amaembo/streamex
     *
     * @param mapper
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Beta
    @IntermediateOp
    public CheckedStream<Double, E> mapPartialToDoubleJdk(final Throwables.Function<? super T, java.util.OptionalDouble, E> mapper) {
        return map(mapper).filter((Throwables.Predicate) IS_PRESENT_DOUBLE_JDK).map(GET_AS_DOUBLE_JDK);
    }

    /**
     *
     *
     * @param <R>
     * @param mapper
     * @return
     */
    public <R> CheckedStream<R, E> mapMulti(final Throwables.BiConsumer<? super T, ? super Consumer<R>, ? extends E> mapper) {
        final Deque<R> queue = new ArrayDeque<>();

        final Consumer<R> consumer = queue::offer;

        final Throwables.Iterator<T, E> iter = iteratorEx();

        return newStream(new Throwables.Iterator<R, E>() {
            @Override
            public boolean hasNext() throws E {
                if (queue.size() == 0) {
                    while (iter.hasNext()) {
                        mapper.accept(iter.next(), consumer);

                        if (queue.size() > 0) {
                            break;
                        }
                    }
                }

                return queue.size() > 0;
            }

            @Override
            public R next() throws E {
                if (queue.size() == 0 && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return queue.poll();
            }
        }, closeHandlers);
    }

    /**
     *
     * @param <R>
     * @param mapper
     * @return
     */
    @IntermediateOp
    public <R> CheckedStream<R, E> slidingMap(Throwables.BiFunction<? super T, ? super T, R, ? extends E> mapper) {
        return slidingMap(1, mapper);
    }

    /**
     *
     *
     * @param <R>
     * @param increment
     * @param mapper
     * @return
     */
    @IntermediateOp
    public <R> CheckedStream<R, E> slidingMap(int increment, Throwables.BiFunction<? super T, ? super T, R, ? extends E> mapper) {
        return slidingMap(increment, false, mapper);
    }

    /**
     *
     *
     * @param <R>
     * @param increment
     * @param ignoreNotPaired
     * @param mapper
     * @return
     */
    @IntermediateOp
    public <R> CheckedStream<R, E> slidingMap(final int increment, final boolean ignoreNotPaired,
            final Throwables.BiFunction<? super T, ? super T, R, ? extends E> mapper) {
        assertNotClosed();

        checkArgPositive(increment, "increment");

        final int windowSize = 2;

        return newStream(new Throwables.Iterator<R, E>() {
            @SuppressWarnings("unchecked")
            private final T none = (T) NONE;
            private T prev = none;
            private T _1 = none; //NOSONAR

            @Override
            public boolean hasNext() throws E {
                if (increment > windowSize && prev != none) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.next();
                    }

                    prev = none;
                }

                if (ignoreNotPaired && _1 == none && elements.hasNext()) {
                    _1 = elements.next();
                }

                return elements.hasNext();
            }

            @Override
            public R next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (ignoreNotPaired) {
                    final R res = mapper.apply(_1, (prev = elements.next()));
                    _1 = increment == 1 ? prev : none;
                    return res;
                } else {
                    if (increment == 1) {
                        return mapper.apply(prev == none ? elements.next() : prev, (prev = (elements.hasNext() ? elements.next() : null)));
                    } else {
                        return mapper.apply(elements.next(), (prev = (elements.hasNext() ? elements.next() : null)));
                    }
                }
            }
        }, closeHandlers);
    }

    /**
     *
     * @param <R>
     * @param mapper
     * @return
     */
    @IntermediateOp
    public <R> CheckedStream<R, E> slidingMap(Throwables.TriFunction<? super T, ? super T, ? super T, R, ? extends E> mapper) {
        return slidingMap(1, mapper);
    }

    /**
     *
     *
     * @param <R>
     * @param increment
     * @param mapper
     * @return
     */
    @IntermediateOp
    public <R> CheckedStream<R, E> slidingMap(int increment, Throwables.TriFunction<? super T, ? super T, ? super T, R, ? extends E> mapper) {
        return slidingMap(increment, false, mapper);
    }

    /**
     *
     *
     * @param <R>
     * @param increment
     * @param ignoreNotPaired
     * @param mapper
     * @return
     */
    @IntermediateOp
    public <R> CheckedStream<R, E> slidingMap(final int increment, final boolean ignoreNotPaired,
            final Throwables.TriFunction<? super T, ? super T, ? super T, R, ? extends E> mapper) {
        assertNotClosed();

        checkArgPositive(increment, "increment");

        final int windowSize = 3;

        return newStream(new Throwables.Iterator<R, E>() {
            @SuppressWarnings("unchecked")
            private final T none = (T) NONE;
            private T prev = none;
            private T prev2 = none;
            private T _1 = none; //NOSONAR
            private T _2 = none; //NOSONAR

            @Override
            public boolean hasNext() throws E {
                if (increment > windowSize && prev != none) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.next();
                    }

                    prev = none;
                }

                if (ignoreNotPaired) {
                    if (_1 == none && elements.hasNext()) {
                        _1 = elements.next();
                    }

                    if (_2 == none && elements.hasNext()) {
                        _2 = elements.next();
                    }
                }

                return elements.hasNext();
            }

            @Override
            public R next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (ignoreNotPaired) {
                    final R res = mapper.apply(_1, _2, (prev = elements.next()));
                    _1 = increment == 1 ? _2 : (increment == 2 ? prev : none);
                    _2 = increment == 1 ? prev : none;
                    return res;
                } else {
                    if (increment == 1) {
                        return mapper.apply(prev2 == none ? elements.next() : prev2,
                                (prev2 = (prev == none ? (elements.hasNext() ? elements.next() : null) : prev)),
                                (prev = (elements.hasNext() ? elements.next() : null)));

                    } else if (increment == 2) {
                        return mapper.apply(prev == none ? elements.next() : prev, elements.hasNext() ? elements.next() : null,
                                (prev = (elements.hasNext() ? elements.next() : null)));
                    } else {
                        return mapper.apply(elements.next(), elements.hasNext() ? elements.next() : null,
                                (prev = (elements.hasNext() ? elements.next() : null)));
                    }
                }
            }
        }, closeHandlers);
    }

    /**
     *
     * @param <K> the key type
     * @param keyMapper
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K> CheckedStream<Map.Entry<K, List<T>>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper) {
        return groupBy(keyMapper, Suppliers.<K, List<T>> ofMap());
    }

    /**
     *
     * @param <K> the key type
     * @param keyMapper
     * @param mapFactory
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K> CheckedStream<Map.Entry<K, List<T>>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Supplier<? extends Map<K, List<T>>> mapFactory) {
        return groupBy(keyMapper, Fnn.<T, E> identity(), mapFactory);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyMapper
     * @param valueMapper
     * @return
     * @see Collectors#toMultimap(Function, Function)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K, V> CheckedStream<Map.Entry<K, List<V>>, E> groupBy(Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            Throwables.Function<? super T, ? extends V, ? extends E> valueMapper) {
        return groupBy(keyMapper, valueMapper, Suppliers.<K, List<V>> ofMap());
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K, V> CheckedStream<Map.Entry<K, List<V>>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.Function<? super T, ? extends V, ? extends E> valueMapper, final Supplier<? extends Map<K, List<V>>> mapFactory) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<Map.Entry<K, List<V>>, E>() {
            private Iterator<Map.Entry<K, List<V>>> iter = null;

            @Override
            public boolean hasNext() throws E {
                init();
                return iter.hasNext();
            }

            @Override
            public Map.Entry<K, List<V>> next() throws E {
                init();
                return iter.next();
            }

            private void init() throws E {
                if (iter == null) {
                    iter = CheckedStream.this.groupTo(keyMapper, valueMapper, mapFactory).entrySet().iterator();
                }
            }
        }, closeHandlers);

    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K, V> CheckedStream<Map.Entry<K, V>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.Function<? super T, ? extends V, ? extends E> valueMapper, Throwables.BinaryOperator<V, ? extends E> mergeFunction) {
        return groupBy(keyMapper, valueMapper, mergeFunction, Suppliers.<K, V> ofMap());
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @param mapFactory
     * @return
     * @see {@link Fn.Fnn#throwingMerger()}
     * @see {@link Fn.Fnn#replacingMerger()}
     * @see {@link Fn.Fnn#ignoringMerger()}
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K, V> CheckedStream<Map.Entry<K, V>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.Function<? super T, ? extends V, ? extends E> valueMapper, final Throwables.BinaryOperator<V, ? extends E> mergeFunction,
            final Supplier<? extends Map<K, V>> mapFactory) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<Map.Entry<K, V>, E>() {
            private Iterator<Map.Entry<K, V>> iter = null;

            @Override
            public boolean hasNext() throws E {
                init();
                return iter.hasNext();
            }

            @Override
            public Map.Entry<K, V> next() throws E {
                init();
                return iter.next();
            }

            private void init() throws E {
                if (iter == null) {
                    iter = CheckedStream.this.toMap(keyMapper, valueMapper, mergeFunction, mapFactory).entrySet().iterator();
                }
            }
        }, closeHandlers);
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <D>
     * @param keyMapper
     * @param downstream
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K, D> CheckedStream<Map.Entry<K, D>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Collector<? super T, ?, D> downstream) {
        return groupBy(keyMapper, downstream, Suppliers.<K, D> ofMap());
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <D>
     * @param keyMapper
     * @param downstream
     * @param mapFactory
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K, D> CheckedStream<Map.Entry<K, D>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Collector<? super T, ?, D> downstream, final Supplier<? extends Map<K, D>> mapFactory) {
        return groupBy(keyMapper, Fnn.<T, E> identity(), downstream, mapFactory);
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <D>
     * @param keyMapper
     * @param valueMapper
     * @param downstream
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K, V, D> CheckedStream<Map.Entry<K, D>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.Function<? super T, ? extends V, ? extends E> valueMapper, final Collector<? super V, ?, D> downstream) {
        return groupBy(keyMapper, valueMapper, downstream, Suppliers.<K, D> ofMap());
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <D>
     * @param keyMapper
     * @param valueMapper
     * @param downstream
     * @param mapFactory
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K, V, D> CheckedStream<Map.Entry<K, D>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.Function<? super T, ? extends V, ? extends E> valueMapper, final Collector<? super V, ?, D> downstream,
            final Supplier<? extends Map<K, D>> mapFactory) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<Map.Entry<K, D>, E>() {
            private Iterator<Map.Entry<K, D>> iter = null;

            @Override
            public boolean hasNext() throws E {
                init();
                return iter.hasNext();
            }

            @Override
            public Map.Entry<K, D> next() throws E {
                init();
                return iter.next();
            }

            private void init() throws E {
                if (iter == null) {
                    iter = CheckedStream.this.groupTo(keyMapper, valueMapper, downstream, mapFactory).entrySet().iterator();
                }
            }
        }, closeHandlers);
    }

    /**
     *
     * @param predicate
     * @return
     * @see Collectors#partitioningBy(Predicate)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public CheckedStream<Map.Entry<Boolean, List<T>>, E> partitionBy(final Throwables.Predicate<? super T, E> predicate) {
        assertNotClosed();

        return partitionBy(predicate, Collectors.<T> toList());
    }

    /**
     *
     *
     * @param <D>
     * @param predicate
     * @param downstream
     * @return
     * @see Collectors#partitioningBy(Predicate, Collector)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <D> CheckedStream<Map.Entry<Boolean, D>, E> partitionBy(final Throwables.Predicate<? super T, E> predicate,
            final Collector<? super T, ?, D> downstream) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<Entry<Boolean, D>, E>() {
            private Iterator<Entry<Boolean, D>> iter = null;

            @Override
            public boolean hasNext() throws E {
                init();
                return iter.hasNext();
            }

            @Override
            public Entry<Boolean, D> next() throws E {
                init();
                return iter.next();
            }

            private void init() throws E {
                if (iter == null) {
                    iter = CheckedStream.this.partitionTo(predicate, downstream).entrySet().iterator();
                }
            }
        }, closeHandlers);
    }

    /**
     *
     * @param <K>
     * @param keyMapper
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K> CheckedStream<Map.Entry<K, Integer>, E> countBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper) {
        return groupBy(keyMapper, Collectors.countingToInt());
    }

    /**
     *
     * @param collapsible test the current element with its previous element. The first parameter is the previous element of current element, the second parameter is the current element.
     * @return
     */
    @IntermediateOp
    public CheckedStream<List<T>, E> collapse(final Throwables.BiPredicate<? super T, ? super T, ? extends E> collapsible) {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = elements;

        return newStream(new Throwables.Iterator<List<T>, E>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() throws E {
                return hasNext || iter.hasNext();
            }

            @Override
            public List<T> next() throws E {
                final List<T> c = new ArrayList<>();
                c.add(hasNext ? next : (next = iter.next()));

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.next()))) {
                        c.add(next);
                    } else {
                        break;
                    }
                }

                return c;
            }
        }, closeHandlers);
    }

    /**
     *
     * @param <C>
     * @param collapsible test the current element with its previous element. The first parameter is the previous element of current element, the second parameter is the current element.
     * @param supplier
     * @return
     */
    @IntermediateOp
    public <C extends Collection<T>> CheckedStream<C, E> collapse(final Throwables.BiPredicate<? super T, ? super T, ? extends E> collapsible,
            final Supplier<? extends C> supplier) {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = elements;

        return newStream(new Throwables.Iterator<C, E>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() throws E {
                return hasNext || iter.hasNext();
            }

            @Override
            public C next() throws E {
                final C c = supplier.get();
                c.add(hasNext ? next : (next = iter.next()));

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.next()))) {
                        c.add(next);
                    } else {
                        break;
                    }
                }

                return c;
            }
        }, closeHandlers);
    }

    /**
     * Merge series of adjacent elements which satisfy the given predicate using
     * the merger function and return a new stream.
     *
     * <p>Example:
     * <pre>
     * <code>
     * Seq.of(new Integer[0]).collapse((p, c) -> p < c, (r, c) -> r + c) => []
     * Seq.of(1).collapse((p, c) -> p < c, (r, c) -> r + c) => [1]
     * Seq.of(1, 2).collapse((p, c) -> p < c, (r, c) -> r + c) => [3]
     * Seq.of(1, 2, 3).collapse((p, c) -> p < c, (r, c) -> r + c) => [6]
     * Seq.of(1, 2, 3, 3, 2, 1).collapse((p, c) -> p < c, (r, c) -> r + c) => [6, 3, 2, 1]
     * </code>
     * </pre>
     *
     * <br />
     * This method only runs sequentially, even in parallel stream.
     *
     * @param collapsible test the current element with its previous element. The first parameter is the previous element of current element, the second parameter is the current element.
     * @param mergeFunction
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> collapse(final Throwables.BiPredicate<? super T, ? super T, ? extends E> collapsible,
            final Throwables.BiFunction<? super T, ? super T, T, ? extends E> mergeFunction) {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = elements;

        return newStream(new Throwables.Iterator<T, E>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() throws E {
                return hasNext || iter.hasNext();
            }

            @Override
            public T next() throws E {
                T res = hasNext ? next : (next = iter.next());

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.next()))) {
                        res = mergeFunction.apply(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, closeHandlers);
    }

    /**
     *
     * @param <U>
     * @param collapsible test the current element with its previous element. The first parameter is the previous element of current element, the second parameter is the current element.
     * @param init is used by {@code op} to generate the first result value in the series.
     * @param op
     * @return
     */
    @IntermediateOp
    public <U> CheckedStream<U, E> collapse(final Throwables.BiPredicate<? super T, ? super T, ? extends E> collapsible, final U init,
            final Throwables.BiFunction<U, ? super T, U, ? extends E> op) {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = elements;

        return newStream(new Throwables.Iterator<U, E>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() throws E {
                return hasNext || iter.hasNext();
            }

            @Override
            public U next() throws E {
                U res = op.apply(init, hasNext ? next : (next = iter.next()));

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.next()))) {
                        res = op.apply(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, closeHandlers);
    }

    /**
     *
     * @param <R>
     * @param collapsible test the current element with its previous element. The first parameter is the previous element of current element, the second parameter is the current element.
     * @param supplier
     * @param accumulator
     * @return
     */
    @IntermediateOp
    public <R> CheckedStream<R, E> collapse(final Throwables.BiPredicate<? super T, ? super T, ? extends E> collapsible,
            final Throwables.Supplier<R, E> supplier, final Throwables.BiConsumer<? super R, ? super T, ? extends E> accumulator) {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = elements;

        return newStream(new Throwables.Iterator<R, E>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() throws E {
                return hasNext || iter.hasNext();
            }

            @Override
            public R next() throws E {
                final R container = supplier.get();
                accumulator.accept(container, hasNext ? next : (next = iter.next()));

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.next()))) {
                        accumulator.accept(container, next);
                    } else {
                        break;
                    }
                }

                return container;
            }
        }, closeHandlers);
    }

    /**
     *
     * @param <R>
     * @param collapsible test the current element with its previous element. The first parameter is the previous element of current element, the second parameter is the current element.
     * @param collector
     * @return
     */
    @IntermediateOp
    public <R> CheckedStream<R, E> collapse(final Throwables.BiPredicate<? super T, ? super T, ? extends E> collapsible,
            final Collector<? super T, ?, R> collector) {
        assertNotClosed();

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super T> accumulator = (BiConsumer<Object, ? super T>) collector.accumulator();
        final Function<Object, R> finisher = (Function<Object, R>) collector.finisher();

        final Throwables.Iterator<T, E> iter = elements;

        return newStream(new Throwables.Iterator<R, E>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() throws E {
                return hasNext || iter.hasNext();
            }

            @Override
            public R next() throws E {
                final Object container = supplier.get();
                accumulator.accept(container, hasNext ? next : (next = iter.next()));

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.next()))) {
                        accumulator.accept(container, next);
                    } else {
                        break;
                    }
                }

                return finisher.apply(container);
            }
        }, closeHandlers);
    }

    /**
     *
     * @param collapsible test the current element with the first element and previous element in the series. The first parameter is the first element of this series, the second parameter is the previous element and the third parameter is the current element.
     * @return
     */
    @IntermediateOp
    public CheckedStream<List<T>, E> collapse(final Throwables.TriPredicate<? super T, ? super T, ? super T, ? extends E> collapsible) {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = elements;

        return newStream(new Throwables.Iterator<List<T>, E>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() throws E {
                return hasNext || iter.hasNext();
            }

            @Override
            public List<T> next() throws E {
                final T first = hasNext ? next : (next = iter.next());
                final List<T> c = new ArrayList<>();
                c.add(first);

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(first, next, (next = iter.next()))) {
                        c.add(next);
                    } else {
                        break;
                    }
                }

                return c;
            }
        }, closeHandlers);
    }

    /**
     *
     * @param <C>
     * @param collapsible test the current element with the first element and previous element in the series. The first parameter is the first element of this series, the second parameter is the previous element and the third parameter is the current element.
     * @param supplier
     * @return
     */
    @IntermediateOp
    public <C extends Collection<T>> CheckedStream<C, E> collapse(final Throwables.TriPredicate<? super T, ? super T, ? super T, ? extends E> collapsible,
            final Supplier<? extends C> supplier) {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = elements;

        return newStream(new Throwables.Iterator<C, E>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() throws E {
                return hasNext || iter.hasNext();
            }

            @Override
            public C next() throws E {
                final T first = hasNext ? next : (next = iter.next());
                final C c = supplier.get();
                c.add(first);

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(first, next, (next = iter.next()))) {
                        c.add(next);
                    } else {
                        break;
                    }
                }

                return c;
            }
        }, closeHandlers);
    }

    /**
     * Merge series of adjacent elements which satisfy the given predicate using
     * the merger function and return a new stream.
     *
     * <p>Example:
     * <pre>
     * <code>
     * Stream.of(new Integer[0]).collapse((f, p, c) -> f < c, (r, c) -> r + c) => []
     * Stream.of(1).collapse((f, p, c) -> f < c, (r, c) -> r + c) => [1]
     * Stream.of(1, 2).collapse((f, p, c) -> f < c, (r, c) -> r + c) => [3]
     * Stream.of(1, 2, 3).collapse((f, p, c) -> f < c, (r, c) -> r + c) => [6]
     * Stream.of(1, 2, 3, 3, 2, 1).collapse((f, p, c) -> f < c, (r, c) -> r + c) => [11, 1]
     * </code>
     * </pre>
     *
     * <br />
     * This method only runs sequentially, even in parallel stream.
     *
     * @param collapsible test the current element with the first element and previous element in the series. The first parameter is the first element of this series, the second parameter is the previous element and the third parameter is the current element.
     * @param mergeFunction
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> collapse(final Throwables.TriPredicate<? super T, ? super T, ? super T, ? extends E> collapsible,
            final Throwables.BiFunction<? super T, ? super T, T, ? extends E> mergeFunction) {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = elements;

        return newStream(new Throwables.Iterator<T, E>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() throws E {
                return hasNext || iter.hasNext();
            }

            @Override
            public T next() throws E {
                final T first = hasNext ? next : (next = iter.next());
                T res = first;

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(first, next, (next = iter.next()))) {
                        res = mergeFunction.apply(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, closeHandlers);
    }

    /**
     *
     * @param <U>
     * @param collapsible test the current element with the first element and previous element in the series. The first parameter is the first element of this series, the second parameter is the previous element and the third parameter is the current element.
     * @param init is used by {@code op} to generate the first result value in the series.
     * @param op
     * @return
     */
    @IntermediateOp
    public <U> CheckedStream<U, E> collapse(final Throwables.TriPredicate<? super T, ? super T, ? super T, ? extends E> collapsible, final U init,
            final Throwables.BiFunction<U, ? super T, U, ? extends E> op) {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = elements;

        return newStream(new Throwables.Iterator<U, E>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() throws E {
                return hasNext || iter.hasNext();
            }

            @Override
            public U next() throws E {
                final T first = hasNext ? next : (next = iter.next());
                U res = op.apply(init, first);

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(first, next, (next = iter.next()))) {
                        res = op.apply(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, closeHandlers);
    }

    /**
     *
     * @param <R>
     *@param collapsible test the current element with the first element and previous element in the series. The first parameter is the first element of this series, the second parameter is the previous element and the third parameter is the current element.
     * @param supplier
     * @param accumulator
     * @return
     */
    @IntermediateOp
    public <R> CheckedStream<R, E> collapse(final Throwables.TriPredicate<? super T, ? super T, ? super T, ? extends E> collapsible,
            final Throwables.Supplier<R, E> supplier, final Throwables.BiConsumer<? super R, ? super T, ? extends E> accumulator) {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = elements;

        return newStream(new Throwables.Iterator<R, E>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() throws E {
                return hasNext || iter.hasNext();
            }

            @Override
            public R next() throws E {
                final T first = hasNext ? next : (next = iter.next());
                final R container = supplier.get();
                accumulator.accept(container, first);

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(first, next, (next = iter.next()))) {
                        accumulator.accept(container, next);
                    } else {
                        break;
                    }
                }

                return container;
            }
        }, closeHandlers);
    }

    /**
     * Merge series of adjacent elements which satisfy the given predicate using
     * the merger function and return a new stream.
     *
     * <p>Example:
     * <pre>
     * <code>
     * Seq.of(new Integer[0]).collapse((f, p, c) -> f < c, Collectors.summingInt(Fn.unboxI())) => []
     * Seq.of(1).collapse((f, p, c) -> f < c, Collectors.summingInt(Fn.unboxI())) => [1]
     * Seq.of(1, 2).collapse((f, p, c) -> f < c, Collectors.summingInt(Fn.unboxI())) => [3]
     * Seq.of(1, 2, 3).collapse((f, p, c) -> f < c, Collectors.summingInt(Fn.unboxI())) => [6]
     * Seq.of(1, 2, 3, 3, 2, 1).collapse((f, p, c) -> f < c, Collectors.summingInt(Fn.unboxI())) => [11, 1]
     * </code>
     * </pre>
     *
     *
     * @param <R>
     * @param collapsible test the current element with the first element and previous element in the series. The first parameter is the first element of this series, the second parameter is the previous element and the third parameter is the current element.
     * @param collector
     * @return
     */
    @IntermediateOp
    public <R> CheckedStream<R, E> collapse(final Throwables.TriPredicate<? super T, ? super T, ? super T, ? extends E> collapsible,
            final Collector<? super T, ?, R> collector) {
        assertNotClosed();

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super T> accumulator = (BiConsumer<Object, ? super T>) collector.accumulator();
        final Function<Object, R> finisher = (Function<Object, R>) collector.finisher();

        final Throwables.Iterator<T, E> iter = elements;

        return newStream(new Throwables.Iterator<R, E>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() throws E {
                return hasNext || iter.hasNext();
            }

            @Override
            public R next() throws E {
                final T first = hasNext ? next : (next = iter.next());
                final Object container = supplier.get();
                accumulator.accept(container, first);

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(first, next, (next = iter.next()))) {
                        accumulator.accept(container, next);
                    } else {
                        break;
                    }
                }

                return finisher.apply(container);
            }
        }, closeHandlers);
    }

    /**
     *
     * @param accumulator
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> scan(final Throwables.BiFunction<? super T, ? super T, T, ? extends E> accumulator) {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = elements;

        return newStream(new Throwables.Iterator<T, E>() {
            private T res = null;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() throws E {
                return iter.hasNext();
            }

            @Override
            public T next() throws E {
                if (isFirst) {
                    isFirst = false;
                    return (res = iter.next());
                } else {
                    return (res = accumulator.apply(res, iter.next()));
                }
            }
        }, closeHandlers);
    }

    /**
     *
     * @param <U>
     * @param init
     * @param accumulator
     * @return
     */
    @IntermediateOp
    public <U> CheckedStream<U, E> scan(final U init, final Throwables.BiFunction<U, ? super T, U, ? extends E> accumulator) {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = elements;

        return newStream(new Throwables.Iterator<U, E>() {
            private U res = init;

            @Override
            public boolean hasNext() throws E {
                return iter.hasNext();
            }

            @Override
            public U next() throws E {
                return (res = accumulator.apply(res, iter.next()));
            }
        }, closeHandlers);
    }

    /**
     *
     * @param <U>
     * @param init
     * @param accumulator
     * @param initIncluded
     * @return
     */
    @IntermediateOp
    public <U> CheckedStream<U, E> scan(final U init, final Throwables.BiFunction<U, ? super T, U, ? extends E> accumulator, final boolean initIncluded) {
        assertNotClosed();

        if (!initIncluded) {
            return scan(init, accumulator);
        }

        final Throwables.Iterator<T, E> iter = elements;

        return newStream(new Throwables.Iterator<U, E>() {
            private boolean isFirst = true;
            private U res = init;

            @Override
            public boolean hasNext() throws E {
                return isFirst || iter.hasNext();
            }

            @Override
            public U next() throws E {
                if (isFirst) {
                    isFirst = false;
                    return init;
                }

                return (res = accumulator.apply(res, iter.next()));
            }
        }, closeHandlers);
    }

    /**
     *
     * @param c
     * @return
     * @see N#intersection(Collection, Collection)
     */
    public CheckedStream<T, E> intersection(final Collection<?> c) {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        return filter(value -> multiset.getAndRemove(value) > 0);
    }

    /**
     *
     *
     * @param <U>
     * @param mapper
     * @param c
     * @return
     * @see N#intersection(Collection, Collection)
     */
    public <U> CheckedStream<T, E> intersection(final Throwables.Function<? super T, ? extends U, E> mapper, final Collection<U> c) {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        return filter(value -> multiset.getAndRemove(mapper.apply(value)) > 0);
    }

    /**
     *
     * @param c
     * @return
     * @see N#difference(Collection, Collection)
     */
    public CheckedStream<T, E> difference(final Collection<?> c) {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        return filter(value -> multiset.getAndRemove(value) < 1);
    }

    /**
     *
     *
     * @param <U>
     * @param mapper
     * @param c
     * @return
     * @see N#difference(Collection, Collection)
     */
    public <U> CheckedStream<T, E> difference(final Function<? super T, ? extends U> mapper, final Collection<U> c) {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        return filter(value -> multiset.getAndRemove(mapper.apply(value)) < 1);
    }

    /**
     *
     * @param c
     * @return
     * @see N#symmetricDifference(Collection, Collection)
     */
    public CheckedStream<T, E> symmetricDifference(final Collection<T> c) {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        return filter(value -> multiset.getAndRemove(value) < 1).append(CheckedStream.<T, E> of(c).filter(value -> multiset.getAndRemove(value) > 0));
    }

    /**
     *
     * @param defaultValue
     * @return
     * @see #appendIfEmpty(Object...)
     */
    @IntermediateOp
    public CheckedStream<T, E> defaultIfEmpty(final T defaultValue) {
        return appendIfEmpty(defaultValue);
    }

    //    /**
    //     *
    //     * @param defaultValues
    //     * @return
    //     * @see #appendIfEmpty(Object...)
    //     */
    //    @IntermediateOp
    //    public final Seq<T, E> defaultIfEmpty(final Collection<? extends T> defaultValues) {
    //        return appendIfEmpty(defaultValues);
    //    }

    /**
     *
     * @param supplier
     * @return
     * @see #appendIfEmpty(Supplier)
     */
    @IntermediateOp
    public CheckedStream<T, E> defaultIfEmpty(final Supplier<? extends CheckedStream<T, E>> supplier) {
        return appendIfEmpty(supplier);
    }

    /**
     *
     *
     * @param a
     * @return
     */
    @IntermediateOp
    @SafeVarargs
    public final CheckedStream<T, E> prepend(final T... a) {
        return prepend(CheckedStream.of(a));
    }

    /**
     *
     *
     * @param c
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> prepend(final Collection<? extends T> c) {
        return prepend(CheckedStream.of(c));
    }

    /**
     *
     * @param s
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> prepend(final CheckedStream<T, E> s) {
        assertNotClosed();

        return concat(s, this);
    }

    /**
     *
     * @param op
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> prepend(final u.Optional<T> op) { //NOSONAR
        assertNotClosed();

        return prepend(op.isEmpty() ? CheckedStream.<T, E> empty() : CheckedStream.<T, E> just(op.orElseThrow()));
    }

    /**
     *
     *
     * @param a
     * @return
     */
    @IntermediateOp
    @SafeVarargs
    public final CheckedStream<T, E> append(final T... a) {
        return append(CheckedStream.of(a));
    }

    /**
     *
     *
     * @param c
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> append(final Collection<? extends T> c) {
        return append(CheckedStream.of(c));
    }

    /**
     *
     * @param s
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> append(final CheckedStream<T, E> s) {
        assertNotClosed();

        return concat(this, s);
    }

    /**
     *
     * @param op
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> append(final u.Optional<T> op) { //NOSONAR
        assertNotClosed();

        return append(op.isEmpty() ? CheckedStream.<T, E> empty() : CheckedStream.<T, E> just(op.orElseThrow()));
    }

    /**
     *
     *
     * @param a
     * @return
     */
    @IntermediateOp
    @SafeVarargs
    public final CheckedStream<T, E> appendIfEmpty(final T... a) {
        return appendIfEmpty(Arrays.asList(a));
    }

    /**
     *
     *
     * @param c
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> appendIfEmpty(final Collection<? extends T> c) {
        assertNotClosed();

        if (N.isEmpty(c)) {
            return newStream(elements, closeHandlers);
        }

        return newStream(new Throwables.Iterator<T, E>() {
            private Throwables.Iterator<T, E> iter;

            @Override
            public boolean hasNext() throws E {
                if (iter == null) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public T next() throws E {
                if (iter == null) {
                    init();
                }

                return iter.next();
            }

            @Override
            public void advance(long n) throws E {
                if (iter == null) {
                    init();
                }

                iter.advance(n);
            }

            @Override
            public long count() throws E {
                if (iter == null) {
                    init();
                }

                return iter.count();
            }

            private void init() throws E {
                if (iter == null) {
                    if (elements.hasNext()) {
                        iter = elements;
                    } else {
                        iter = Throwables.Iterator.of(c.iterator());
                    }
                }
            }
        }, closeHandlers);
    }

    /**
     * Append if empty.
     *
     * @param supplier
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> appendIfEmpty(final Supplier<? extends CheckedStream<T, E>> supplier) {
        assertNotClosed();

        final Holder<CheckedStream<T, E>> holder = new Holder<>();

        return newStream(new Throwables.Iterator<T, E>() {
            private Throwables.Iterator<T, E> iter;

            @Override
            public boolean hasNext() throws E {
                if (iter == null) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public T next() throws E {
                if (iter == null) {
                    init();
                }

                return iter.next();
            }

            @Override
            public void advance(long n) throws E {
                if (iter == null) {
                    init();
                }

                iter.advance(n);
            }

            @Override
            public long count() throws E {
                if (iter == null) {
                    init();
                }

                return iter.count();
            }

            private void init() throws E {
                if (iter == null) {
                    if (elements.hasNext()) {
                        iter = elements;
                    } else {
                        final CheckedStream<T, E> s = supplier.get();
                        holder.setValue(s);
                        iter = iterate(s);
                    }
                }
            }
        }, closeHandlers).onClose(() -> close(holder));
    }

    //    @SuppressWarnings("rawtypes")
    //    private static final Throwables.Predicate NOT_NULL_MASK = new Throwables.Predicate<Object, RuntimeException>() {
    //            //        public boolean test(final Object t) {
    //            return t != NONE;
    //        }
    //    };
    //
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, E> appendOnError(final T fallbackValue) {
    //        assertNotClosed();
    //
    //        return newStream(new Throwables.Iterator<T, E>() {
    //            private boolean fallbackValueAvaiable = true;
    //
    //                //            public boolean hasNext() throws E {
    //                try {
    //                    return fallbackValueAvaiable && elements.hasNext();
    //                } catch (Exception e) {
    //                    return fallbackValueAvaiable;
    //                }
    //            }
    //
    //                //            public T next() throws E {
    //                if (hasNext() == false) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                try {
    //                    return elements.next();
    //                } catch (Exception e) {
    //                    fallbackValueAvaiable = false;
    //                    return fallbackValue;
    //                }
    //            }
    //        }, false, null, closeHandlers);
    //    }
    //
    //    @Beta
    //    @IntermediateOp
    //    public <XE extends Exception> Seq<T, E> appendOnError(final Class<XE> type, final T fallbackValue) {
    //        assertNotClosed();
    //        this.checkArgNotNull(type, "type");
    //
    //        return newStream(new Throwables.Iterator<T, E>() {
    //            private boolean fallbackValueAvaiable = true;
    //
    //                //            public boolean hasNext() throws E {
    //                try {
    //                    return fallbackValueAvaiable && elements.hasNext();
    //                } catch (Exception e) {
    //                    if (ExceptionUtil.hasCause(e, type)) {
    //                        return fallbackValueAvaiable;
    //                    } else {
    //                        throw (E) e;
    //                    }
    //                }
    //            }
    //
    //                //            public T next() throws E {
    //                if (hasNext() == false) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                try {
    //                    return elements.next();
    //                } catch (Exception e) {
    //                    if (ExceptionUtil.hasCause(e, type)) {
    //                        fallbackValueAvaiable = false;
    //                        return fallbackValue;
    //                    } else {
    //                        throw (E) e;
    //                    }
    //                }
    //            }
    //        }, false, null, closeHandlers);
    //    }
    //
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, E> appendOnError(final Predicate<? super Exception> predicate, final T fallbackValue) {
    //        assertNotClosed();
    //        this.checkArgNotNull(predicate, "predicate");
    //
    //        return newStream(new Throwables.Iterator<T, E>() {
    //            private boolean fallbackValueAvaiable = true;
    //
    //                //            public boolean hasNext() throws E {
    //                try {
    //                    return fallbackValueAvaiable && elements.hasNext();
    //                } catch (Exception e) {
    //                    if (predicate.test(e)) {
    //                        return fallbackValueAvaiable;
    //                    } else {
    //                        throw (E) e;
    //                    }
    //                }
    //            }
    //
    //                //            public T next() throws E {
    //                if (hasNext() == false) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                try {
    //                    return elements.next();
    //                } catch (Exception e) {
    //                    if (predicate.test(e)) {
    //                        fallbackValueAvaiable = false;
    //                        return fallbackValue;
    //                    } else {
    //                        throw (E) e;
    //                    }
    //                }
    //            }
    //        }, false, null, closeHandlers);
    //    }
    //
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, E> appendOnError(final Supplier<Seq<T, E>> fallbackStreamSupplier) {
    //        assertNotClosed();
    //        this.checkArgNotNull(fallbackStreamSupplier, "fallbackStreamSupplier");
    //
    //        final Throwables.Iterator<T, E> iter = new Throwables.Iterator<T, E>() {
    //            private boolean fallbackValueAvaiable = true;
    //            private Throwables.Iterator<T, E> iter = Seq.this.elements;
    //            private Seq<T, E> s = null;
    //
    //                //            public boolean hasNext() throws E {
    //                try {
    //                    return iter.hasNext();
    //                } catch (Exception e) {
    //                    if (fallbackValueAvaiable) {
    //                        useFallbackStream(fallbackStreamSupplier);
    //
    //                        return iter.hasNext();
    //                    } else {
    //                        throw (E) e;
    //                    }
    //                }
    //            }
    //
    //                //            public T next() throws E {
    //                if (hasNext() == false) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                try {
    //                    return iter.next();
    //                } catch (Exception e) {
    //                    if (fallbackValueAvaiable) {
    //                        useFallbackStream(fallbackStreamSupplier);
    //
    //                        if (iter.hasNext()) {
    //                            return iter.next();
    //                        } else {
    //                            return (T) NONE;
    //                        }
    //                    } else {
    //                        throw (E) e;
    //                    }
    //                }
    //            }
    //
    //                //            public void close() {
    //                if ((s != null && N.notEmpty(s.closeHandlers)) || N.notEmpty(Seq.this.closeHandlers)) {
    //                    try {
    //                        if (s != null) {
    //                            s.close();
    //                        }
    //                    } finally {
    //                        Seq.this.close();
    //                    }
    //                }
    //            }
    //
    //            private void useFallbackStream(final Supplier<Seq<T, E>> fallbackStream) {
    //                fallbackValueAvaiable = false;
    //                s = fallbackStream.get();
    //                iter = s.elements;
    //            }
    //        };
    //
    //        return newStream(iter).onClose(newCloseHandler(iter)).filter(NOT_NULL_MASK);
    //    }
    //
    //    @Beta
    //    @IntermediateOp
    //    public <XE extends Exception> Seq<T, E> appendOnError(final Class<XE> type, final Supplier<Seq<T, E>> fallbackStreamSupplier) {
    //        assertNotClosed();
    //        this.checkArgNotNull(type, "type");
    //        this.checkArgNotNull(fallbackStreamSupplier, "fallbackStreamSupplier");
    //
    //        final Throwables.Iterator<T, E> iter = new Throwables.Iterator<T, E>() {
    //            private boolean fallbackValueAvaiable = true;
    //            private Throwables.Iterator<T, E> iter = Seq.this.elements;
    //            private Seq<T, E> s = null;
    //
    //                //            public boolean hasNext() throws E {
    //                try {
    //                    return iter.hasNext();
    //                } catch (Exception e) {
    //                    if (fallbackValueAvaiable && ExceptionUtil.hasCause(e, type)) {
    //                        useFallbackStream(fallbackStreamSupplier);
    //
    //                        return iter.hasNext();
    //                    } else {
    //                        throw (E) e;
    //                    }
    //                }
    //            }
    //
    //                //            public T next() throws E {
    //                if (hasNext() == false) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                try {
    //                    return iter.next();
    //                } catch (Exception e) {
    //                    if (fallbackValueAvaiable && ExceptionUtil.hasCause(e, type)) {
    //                        useFallbackStream(fallbackStreamSupplier);
    //
    //                        if (iter.hasNext()) {
    //                            return iter.next();
    //                        } else {
    //                            return (T) NONE;
    //                        }
    //                    } else {
    //                        throw (E) e;
    //                    }
    //                }
    //            }
    //
    //                //            public void close() {
    //                if ((s != null && N.notEmpty(s.closeHandlers)) || N.notEmpty(Seq.this.closeHandlers)) {
    //                    try {
    //                        if (s != null) {
    //                            s.close();
    //                        }
    //                    } finally {
    //                        Seq.this.close();
    //                    }
    //                }
    //            }
    //
    //            private void useFallbackStream(final Supplier<Seq<T, E>> fallbackStream) {
    //                fallbackValueAvaiable = false;
    //                s = fallbackStream.get();
    //                iter = s.elements;
    //            }
    //        };
    //
    //        return newStream(iter).onClose(newCloseHandler(iter)).filter(NOT_NULL_MASK);
    //    }
    //
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, E> appendOnError(final Predicate<? super Exception> predicate, final Supplier<Seq<T, E>> fallbackStreamSupplier) {
    //        assertNotClosed();
    //        this.checkArgNotNull(predicate, "predicate");
    //        this.checkArgNotNull(fallbackStreamSupplier, "fallbackStreamSupplier");
    //
    //        final Throwables.Iterator<T, E> iter = new Throwables.Iterator<T, E>() {
    //            private boolean fallbackValueAvaiable = true;
    //            private Throwables.Iterator<T, E> iter = Seq.this.elements;
    //            private Seq<T, E> s = null;
    //
    //                //            public boolean hasNext() throws E {
    //                try {
    //                    return iter.hasNext();
    //                } catch (Exception e) {
    //                    if (fallbackValueAvaiable && predicate.test(e)) {
    //                        useFallbackStream(fallbackStreamSupplier);
    //
    //                        return iter.hasNext();
    //                    } else {
    //                        throw (E) e;
    //                    }
    //                }
    //            }
    //
    //                //            public T next() throws E {
    //                if (hasNext() == false) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                try {
    //                    return iter.next();
    //                } catch (Exception e) {
    //                    if (fallbackValueAvaiable && predicate.test(e)) {
    //                        useFallbackStream(fallbackStreamSupplier);
    //
    //                        if (iter.hasNext()) {
    //                            return iter.next();
    //                        } else {
    //                            return (T) NONE;
    //                        }
    //                    } else {
    //                        throw (E) e;
    //                    }
    //                }
    //            }
    //
    //                //            public void close() {
    //                if ((s != null && N.notEmpty(s.closeHandlers)) || N.notEmpty(Seq.this.closeHandlers)) {
    //                    try {
    //                        if (s != null) {
    //                            s.close();
    //                        }
    //                    } finally {
    //                        Seq.this.close();
    //                    }
    //                }
    //            }
    //
    //            private void useFallbackStream(final Supplier<Seq<T, E>> fallbackStream) {
    //                fallbackValueAvaiable = false;
    //                s = fallbackStream.get();
    //                iter = s.elements;
    //            }
    //        };
    //
    //        return newStream(iter).onClose(newCloseHandler(iter)).filter(NOT_NULL_MASK);
    //    }

    /**
     * Throws {@code NoSuchElementException} in terminal operation if this {@code Seq} if empty.
     *
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> throwIfEmpty() {
        final Supplier<CheckedStream<T, E>> tmp = () -> {
            throw new NoSuchElementException();
        };

        return this.appendIfEmpty(tmp);
    }

    /**
     *
     *
     * @param exceptionSupplier
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> throwIfEmpty(final Supplier<? extends E> exceptionSupplier) {
        this.checkArgNotNull(exceptionSupplier, "exceptionSupplier");

        assertNotClosed();

        final Holder<CheckedStream<T, E>> holder = new Holder<>();

        return newStream(new Throwables.Iterator<T, E>() {
            private Throwables.Iterator<T, E> iter;

            @Override
            public boolean hasNext() throws E {
                if (iter == null) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public T next() throws E {
                if (iter == null) {
                    init();
                }

                return iter.next();
            }

            @Override
            public void advance(long n) throws E {
                if (iter == null) {
                    init();
                }

                iter.advance(n);
            }

            @Override
            public long count() throws E {
                if (iter == null) {
                    init();
                }

                return iter.count();
            }

            private void init() throws E {
                if (iter == null) {
                    if (elements.hasNext()) {
                        iter = elements;
                    } else {
                        throw exceptionSupplier.get();
                    }
                }
            }
        }, closeHandlers).onClose(() -> close(holder));
    }

    /**
     *
     *
     * @param <R>
     * @param <E2>
     * @param func
     * @return
     * @throws E
     * @throws E2
     */
    @TerminalOp
    public <R, E2 extends Exception> u.Optional<R> applyIfNotEmpty(final Throwables.Function<? super CheckedStream<T, E>, R, E2> func) throws E, E2 {
        assertNotClosed();

        try {
            if (elements.hasNext()) {
                return Optional.ofNullable(func.apply(this));
            } else {
                return Optional.empty();
            }
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @param <E2>
     * @param action
     * @return
     * @throws E
     * @throws E2
     */
    @TerminalOp
    public <E2 extends Exception> OrElse acceptIfNotEmpty(Throwables.Consumer<? super CheckedStream<T, E>, E2> action) throws E, E2 {
        assertNotClosed();

        try {
            if (elements.hasNext()) {
                action.accept(this);

                return OrElse.TRUE;
            }
        } finally {
            close();
        }

        return OrElse.FALSE;
    }

    /**
     *
     * @param action
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> onEach(final Throwables.Consumer<? super T, ? extends E> action) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<T, E>() {
            @Override
            public boolean hasNext() throws E {
                return elements.hasNext();
            }

            @Override
            public T next() throws E {
                final T next = elements.next();
                action.accept(next);
                return next;
            }
        }, sorted, cmp, closeHandlers);
    }

    /**
     *
     * @param action
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> peek(final Throwables.Consumer<? super T, ? extends E> action) {
        return onEach(action);
    }

    /**
     *
     *
     * @param action
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> peekFirst(final Throwables.Consumer<? super T, ? extends E> action) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<T, E>() {
            private boolean isFirst = true;

            @Override
            public boolean hasNext() throws E {
                return elements.hasNext();
            }

            @Override
            public T next() throws E {
                if (isFirst) {
                    isFirst = false;
                    final T e = elements.next();
                    action.accept(e);
                    return e;
                } else {
                    return elements.next();
                }
            }
        }, sorted, cmp, closeHandlers);
    }

    /**
     *
     *
     * @param action
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> peekLast(final Throwables.Consumer<? super T, ? extends E> action) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<T, E>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() throws E {
                return hasNext || elements.hasNext();
            }

            @Override
            public T next() throws E {
                next = elements.next();

                if (!(hasNext = elements.hasNext())) {
                    action.accept(next);
                }

                return next;
            }
        }, sorted, cmp, closeHandlers);
    }

    /**
     *
     * @param predicate
     * @param action
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> peekIf(final Throwables.Predicate<? super T, E> predicate, final Throwables.Consumer<? super T, E> action) {
        assertNotClosed();

        checkArgNotNull(predicate, "predicate");
        checkArgNotNull(action, "action");

        return peek(it -> {
            if (predicate.test(it)) {
                action.accept(it);
            }
        });
    }

    /**
     *
     * @param predicate The first parameter is the element. The second parameter is the count of iterated elements, starts with 1.
     * @param action
     * @return
     */
    @Beta
    @IntermediateOp
    public CheckedStream<T, E> peekIf(final Throwables.BiPredicate<? super T, ? super Long, E> predicate, final Consumer<? super T> action) {
        assertNotClosed();

        checkArgNotNull(predicate, "predicate");
        checkArgNotNull(action, "action");

        final MutableLong count = MutableLong.of(0);

        return onEach(it -> {
            if (predicate.test(it, count.incrementAndGet())) {
                action.accept(it);
            }
        });
    }

    /**
     *
     * @param chunkSize
     * @return
     */
    @IntermediateOp
    public CheckedStream<Stream<T>, E> split(final int chunkSize) {
        assertNotClosed();

        return splitToList(chunkSize).map(Stream::of);
    }

    /**
     * Returns CheckedStream of {@code List<T>} with consecutive sub sequences of the elements, each of the same size (the final sequence may be smaller).
     *
     *
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @return
     */
    @IntermediateOp
    public CheckedStream<List<T>, E> splitToList(final int chunkSize) {
        return split(chunkSize, Factory.<T> ofList());
    }

    /**
     * Returns CheckedStream of {@code Set<T>} with consecutive sub sequences of the elements, each of the same size (the final sequence may be smaller).
     *
     *
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @return
     */
    @IntermediateOp
    public CheckedStream<Set<T>, E> splitToSet(final int chunkSize) {
        return split(chunkSize, Factory.<T> ofSet());
    }

    /**
     * Returns CheckedStream of {@code C} with consecutive sub sequences of the elements, each of the same size (the final sequence may be smaller).
     *
     *
     * @param <C>
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @param collectionSupplier
     * @return
     */
    @IntermediateOp
    public <C extends Collection<T>> CheckedStream<C, E> split(final int chunkSize, final IntFunction<? extends C> collectionSupplier) {
        assertNotClosed();

        checkArgPositive(chunkSize, "chunkSize");
        checkArgNotNull(collectionSupplier, "collectionSupplier");

        return newStream(new Throwables.Iterator<C, E>() {
            @Override
            public boolean hasNext() throws E {
                return elements.hasNext();
            }

            @Override
            public C next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final C result = collectionSupplier.apply(chunkSize);
                int cnt = 0;

                while (cnt++ < chunkSize && elements.hasNext()) {
                    result.add(elements.next());
                }

                return result;
            }

            @Override
            public long count() throws E {
                final long len = elements.count();
                return len % chunkSize == 0 ? len / chunkSize : len / chunkSize + 1;
            }

            @Override
            public void advance(long n) throws E {
                checkArgNotNegative(n, "n");

                elements.advance(n > Long.MAX_VALUE / chunkSize ? Long.MAX_VALUE : n * chunkSize);
            }
        }, closeHandlers);
    }

    /**
     *
     * @param <R>
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @param collector
     * @return
     */
    @IntermediateOp
    public <R> CheckedStream<R, E> split(final int chunkSize, final Collector<? super T, ?, R> collector) {
        assertNotClosed();

        checkArgPositive(chunkSize, "chunkSize");
        checkArgNotNull(collector, "collector");

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super T> accumulator = (BiConsumer<Object, ? super T>) collector.accumulator();
        final Function<Object, R> finisher = (Function<Object, R>) collector.finisher();

        return newStream(new Throwables.Iterator<R, E>() {
            @Override
            public boolean hasNext() throws E {
                return elements.hasNext();
            }

            @Override
            public R next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final Object container = supplier.get();
                int cnt = 0;

                while (cnt++ < chunkSize && elements.hasNext()) {
                    accumulator.accept(container, elements.next());
                }

                return finisher.apply(container);
            }

            @Override
            public long count() throws E {
                final long len = elements.count();
                return len % chunkSize == 0 ? len / chunkSize : len / chunkSize + 1;
            }

            @Override
            public void advance(long n) throws E {
                checkArgNotNegative(n, "n");

                elements.advance(n > Long.MAX_VALUE / chunkSize ? Long.MAX_VALUE : n * chunkSize);
            }
        }, closeHandlers);
    }

    /**
     *
     *
     * @param predicate
     * @return
     */
    @IntermediateOp
    public CheckedStream<Stream<T>, E> split(final Throwables.Predicate<? super T, ? extends E> predicate) {
        assertNotClosed();

        return splitToList(predicate).map(Stream::of);
    }

    /**
     *
     *
     * @param predicate
     * @return
     */
    @IntermediateOp
    public CheckedStream<List<T>, E> splitToList(final Throwables.Predicate<? super T, ? extends E> predicate) {
        assertNotClosed();

        return split(predicate, Suppliers.<T> ofList());
    }

    /**
     *
     *
     * @param predicate
     * @return
     */
    @IntermediateOp
    public CheckedStream<Set<T>, E> splitToSet(final Throwables.Predicate<? super T, ? extends E> predicate) {
        assertNotClosed();

        return split(predicate, Suppliers.<T> ofSet());
    }

    /**
     *
     *
     * @param <C>
     * @param predicate
     * @param collectionSupplier
     * @return
     */
    @IntermediateOp
    public <C extends Collection<T>> CheckedStream<C, E> split(final Throwables.Predicate<? super T, ? extends E> predicate,
            final Supplier<? extends C> collectionSupplier) {
        assertNotClosed();

        checkArgNotNull(predicate, "predicate");
        checkArgNotNull(collectionSupplier, "collectionSupplier");

        return newStream(new Throwables.Iterator<C, E>() {
            private T next = (T) NONE;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() throws E {
                return next != NONE || elements.hasNext();
            }

            @Override
            public C next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final C result = collectionSupplier.get();
                boolean isFirst = true;

                if (next == NONE) {
                    next = elements.next();
                }

                while (next != NONE) {
                    if (isFirst) {
                        result.add(next);
                        preCondition = predicate.test(next);
                        next = elements.hasNext() ? elements.next() : (T) NONE;
                        isFirst = false;
                    } else if (predicate.test(next) == preCondition) {
                        result.add(next);
                        next = elements.hasNext() ? elements.next() : (T) NONE;
                    } else {

                        break;
                    }
                }

                return result;
            }

        }, closeHandlers);
    }

    /**
     *
     *
     * @param <R>
     * @param predicate
     * @param collector
     * @return
     */
    @IntermediateOp
    public <R> CheckedStream<R, E> split(final Throwables.Predicate<? super T, ? extends E> predicate, final Collector<? super T, ?, R> collector) {
        assertNotClosed();

        checkArgNotNull(predicate, "predicate");
        checkArgNotNull(collector, "collector");

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super T> accumulator = (BiConsumer<Object, ? super T>) collector.accumulator();
        final Function<Object, R> finisher = (Function<Object, R>) collector.finisher();

        return newStream(new Throwables.Iterator<R, E>() {
            private T next = (T) NONE;
            private boolean preCondition = false;

            @Override
            public boolean hasNext() throws E {
                return next != NONE || elements.hasNext();
            }

            @Override
            public R next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final Object container = supplier.get();
                boolean isFirst = true;

                if (next == NONE) {
                    next = elements.next();
                }

                while (next != NONE) {
                    if (isFirst) {
                        accumulator.accept(container, next);
                        preCondition = predicate.test(next);
                        next = elements.hasNext() ? elements.next() : (T) NONE;
                        isFirst = false;
                    } else if (predicate.test(next) == preCondition) {
                        accumulator.accept(container, next);
                        next = elements.hasNext() ? elements.next() : (T) NONE;
                    } else {

                        break;
                    }
                }

                return finisher.apply(container);
            }

        }, closeHandlers);
    }

    /**
     *
     *
     * @param where
     * @return
     */
    @IntermediateOp
    public CheckedStream<CheckedStream<T, E>, E> splitAt(final int where) {
        assertNotClosed();

        checkArgNotNegative(where, "where");

        final Throwables.Iterator<T, E> iter = elements;

        return newStream(new Throwables.Iterator<CheckedStream<T, E>, E>() {
            private int cursor = 0;

            @Override
            public boolean hasNext() throws E {
                return cursor < 2;
            }

            @Override
            public CheckedStream<T, E> next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                CheckedStream<T, E> result = null;

                if (cursor == 0) {
                    final List<T> list = new ArrayList<>();
                    int cnt = 0;

                    while (cnt++ < where && iter.hasNext()) {
                        list.add(iter.next());
                    }

                    result = new CheckedStream<>(Throwables.Iterator.of(list.iterator()), sorted, cmp, null);
                } else {
                    result = new CheckedStream<>(iter, sorted, cmp, null);
                }

                cursor++;

                return result;
            }

            @Override
            public long count() throws E {
                iter.count();

                return 2 - cursor; //NOSONAR
            }

            @Override
            public void advance(long n) throws E {
                if (n == 0) {
                    return;
                } else if (n == 1) {
                    if (cursor == 0) {
                        iter.advance(where);
                    } else {
                        iter.advance(Long.MAX_VALUE);
                    }
                } else {
                    iter.advance(Long.MAX_VALUE);
                }

                cursor = n >= 2 ? 2 : cursor + (int) n;
            }
        }, closeHandlers);
    }

    /**
     *
     *
     * @param where
     * @return
     */
    @IntermediateOp
    public CheckedStream<CheckedStream<T, E>, E> splitAt(final Throwables.Predicate<? super T, ? extends E> where) {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = elements;

        return newStream(new Throwables.Iterator<CheckedStream<T, E>, E>() {
            private int cursor = 0;
            private T next = null;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() throws E {
                return cursor < 2;
            }

            @Override
            public CheckedStream<T, E> next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                CheckedStream<T, E> result = null;

                if (cursor == 0) {
                    final List<T> list = new ArrayList<>();

                    while (iter.hasNext()) {
                        next = iter.next();

                        if (!where.test(next)) {
                            list.add(next);
                        } else {
                            hasNext = true;
                            break;
                        }
                    }

                    result = new CheckedStream<>(Throwables.Iterator.of(list.iterator()), sorted, cmp, null);
                } else {
                    Throwables.Iterator<T, E> iterEx = iter;

                    if (hasNext) {
                        iterEx = new Throwables.Iterator<>() {
                            private boolean isFirst = true;

                            public boolean hasNext() throws E {
                                return isFirst || iter.hasNext();
                            }

                            public T next() throws E {
                                if (!hasNext()) {
                                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                                }

                                if (isFirst) {
                                    isFirst = false;
                                    return next;
                                } else {
                                    return iter.next();
                                }
                            }
                        };
                    }

                    result = new CheckedStream<>(iterEx, sorted, cmp, null);
                }

                cursor++;

                return result;
            }

            @Override
            public long count() throws E {
                iter.count();

                return 2 - cursor; //NOSONAR
            }

            @Override
            public void advance(long n) throws E {
                if (n == 0) {
                    return;
                } else if (n == 1) {
                    if (cursor == 0) {
                        while (iter.hasNext()) {
                            next = iter.next();

                            if (!where.test(next)) {
                                hasNext = true;
                                break;
                            }
                        }
                    } else {
                        iter.advance(Long.MAX_VALUE);
                    }
                } else {
                    iter.advance(Long.MAX_VALUE);
                }

                cursor = n >= 2 ? 2 : cursor + (int) n;
            }

        }, closeHandlers);
    }

    /**
     *
     * @param windowSize
     * @return
     * @see #sliding(int, int)
     */
    @IntermediateOp
    public CheckedStream<Stream<T>, E> sliding(int windowSize) {
        return sliding(windowSize, 1);
    }

    /**
     *
     * @param windowSize
     * @return
     * @see #sliding(int, int)
     */
    @IntermediateOp
    public CheckedStream<List<T>, E> slidingToList(int windowSize) {
        return slidingToList(windowSize, 1);
    }

    /**
     *
     * @param windowSize
     * @param increment
     * @return
     */
    @IntermediateOp
    public CheckedStream<Stream<T>, E> sliding(final int windowSize, final int increment) {
        assertNotClosed();

        return slidingToList(windowSize, increment).map(Stream::of);
    }

    /**
     * Sliding to list.
     *
     * @param windowSize
     * @param increment
     * @return
     */
    @IntermediateOp
    public CheckedStream<List<T>, E> slidingToList(final int windowSize, final int increment) {
        return sliding(windowSize, increment, Factory.<T> ofList());
    }

    /**
     * Sliding to set.
     *
     * @param windowSize
     * @param increment
     * @return
     */
    @IntermediateOp
    public CheckedStream<Set<T>, E> slidingToSet(final int windowSize, final int increment) {
        return sliding(windowSize, increment, Factory.<T> ofSet());
    }

    /**
     *
     * @param <C>
     * @param windowSize
     * @param increment
     * @param collectionSupplier
     * @return
     */
    @IntermediateOp
    public <C extends Collection<T>> CheckedStream<C, E> sliding(final int windowSize, final int increment, final IntFunction<? extends C> collectionSupplier) {
        assertNotClosed();

        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);
        checkArgNotNull(collectionSupplier, "collectionSupplier");

        return newStream(new Throwables.Iterator<C, E>() {
            private Deque<T> queue = null;
            private boolean toSkip = false;

            @Override
            public boolean hasNext() throws E {
                if (toSkip) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.next();
                    }

                    toSkip = false;
                }

                return elements.hasNext();
            }

            @Override
            public C next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (queue == null) {
                    queue = new ArrayDeque<>(N.max(0, windowSize - increment));
                }

                final C result = collectionSupplier.apply(windowSize);
                int cnt = 0;

                if (queue.size() > 0 && increment < windowSize) {
                    cnt = queue.size();

                    result.addAll(queue);

                    if (queue.size() <= increment) {
                        queue.clear();
                    } else {
                        for (int i = 0; i < increment; i++) {
                            queue.removeFirst();
                        }
                    }
                }

                T next = null;

                while (cnt++ < windowSize && elements.hasNext()) {
                    next = elements.next();
                    result.add(next);

                    if (cnt > increment) {
                        queue.add(next);
                    }
                }

                toSkip = increment > windowSize;

                return result;
            }

            @Override
            public long count() throws E {
                final int prevSize = increment >= windowSize ? 0 : (queue == null ? 0 : queue.size());
                final long len = prevSize + elements.count();

                if (len == prevSize) {
                    return 0;
                } else if (len <= windowSize) {
                    return 1;
                } else {
                    final long rlen = len - windowSize;
                    return 1 + (rlen % increment == 0 ? rlen / increment : rlen / increment + 1);
                }
            }

            @Override
            public void advance(long n) throws E {
                checkArgNotNegative(n, "n");

                if (n == 0) {
                    return;
                }

                if (increment >= windowSize) {
                    elements.advance(n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                } else {
                    if (N.isEmpty(queue)) {
                        final long m = ((n - 1) > Long.MAX_VALUE / increment ? Long.MAX_VALUE : (n - 1) * increment);
                        elements.advance(m);
                    } else {
                        final long m = (n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                        final int prevSize = increment >= windowSize ? 0 : (queue == null ? 0 : queue.size()); //NOSONAR

                        if (m < prevSize) {
                            for (int i = 0; i < m; i++) {
                                queue.removeFirst();
                            }
                        } else {
                            if (queue != null) {
                                queue.clear();
                            }

                            elements.advance(m - prevSize);
                        }
                    }

                    if (queue == null) {
                        queue = new ArrayDeque<>(windowSize);
                    }

                    int cnt = queue.size();

                    while (cnt++ < windowSize && elements.hasNext()) {
                        queue.add(elements.next());
                    }
                }
            }
        }, closeHandlers);
    }

    /**
     *
     * @param <R>
     * @param windowSize
     * @param increment
     * @param collector
     * @return
     */
    @IntermediateOp
    public <R> CheckedStream<R, E> sliding(final int windowSize, final int increment, final Collector<? super T, ?, R> collector) {
        assertNotClosed();

        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);
        checkArgNotNull(collector, "collector");

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super T> accumulator = (BiConsumer<Object, ? super T>) collector.accumulator();
        final Function<Object, R> finisher = (Function<Object, R>) collector.finisher();

        return newStream(new Throwables.Iterator<R, E>() {
            private Deque<T> queue = null;
            private boolean toSkip = false;

            @Override
            public boolean hasNext() throws E {
                if (toSkip) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.next();
                    }

                    toSkip = false;
                }

                return elements.hasNext();
            }

            @Override
            public R next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (increment < windowSize && queue == null) {
                    queue = new ArrayDeque<>(windowSize - increment);
                }

                final Object container = supplier.get();
                int cnt = 0;

                if (increment < windowSize && queue.size() > 0) {
                    cnt = queue.size();

                    for (T e : queue) {
                        accumulator.accept(container, e);
                    }

                    if (queue.size() <= increment) {
                        queue.clear();
                    } else {
                        for (int i = 0; i < increment; i++) {
                            queue.removeFirst();
                        }
                    }
                }

                T next = null;

                while (cnt++ < windowSize && elements.hasNext()) {
                    next = elements.next();
                    accumulator.accept(container, next);

                    if (cnt > increment) {
                        queue.add(next);
                    }
                }

                toSkip = increment > windowSize;

                return finisher.apply(container);
            }

            @Override
            public long count() throws E {
                final int prevSize = increment >= windowSize ? 0 : (queue == null ? 0 : queue.size());
                final long len = prevSize + elements.count();

                if (len == prevSize) {
                    return 0;
                } else if (len <= windowSize) {
                    return 1;
                } else {
                    final long rlen = len - windowSize;
                    return 1 + (rlen % increment == 0 ? rlen / increment : rlen / increment + 1);
                }
            }

            @Override
            public void advance(long n) throws E {
                checkArgNotNegative(n, "n");

                if (n == 0) {
                    return;
                }

                if (increment >= windowSize) {
                    elements.advance(n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                } else {
                    if (N.isEmpty(queue)) {
                        final long m = ((n - 1) > Long.MAX_VALUE / increment ? Long.MAX_VALUE : (n - 1) * increment);
                        elements.advance(m);
                    } else {
                        final long m = (n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                        final int prevSize = increment >= windowSize ? 0 : (queue == null ? 0 : queue.size()); //NOSONAR

                        if (m < prevSize) {
                            for (int i = 0; i < m; i++) {
                                queue.removeFirst();
                            }
                        } else {
                            if (queue != null) {
                                queue.clear();
                            }

                            elements.advance(m - prevSize);
                        }
                    }

                    if (queue == null) {
                        queue = new ArrayDeque<>(windowSize);
                    }

                    int cnt = queue.size();

                    while (cnt++ < windowSize && elements.hasNext()) {
                        queue.add(elements.next());
                    }
                }
            }
        }, closeHandlers);
    }

    /**
     *
     * @param n
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> skip(final long n) {
        assertNotClosed();
        checkArgNotNegative(n, "n");

        //    if (n == 0) {
        //        return newStream(elements, sorted, cmp, closeHandlers);
        //    }

        if (n == 0) {
            return this;
        }

        return newStream(new Throwables.Iterator<T, E>() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() throws E {
                if (!skipped) {
                    skipped = true;
                    advance(n);
                }

                return elements.hasNext();
            }

            @Override
            public T next() throws E {
                if (!skipped) {
                    skipped = true;
                    advance(n);
                }

                return elements.next();
            }
        }, sorted, cmp, closeHandlers);
    }

    /**
     *
     *
     * @param n
     * @param action
     * @return
     */
    public CheckedStream<T, E> skip(final long n, final Throwables.Consumer<? super T, ? extends E> action) {
        assertNotClosed();

        checkArgNotNegative(n, "n");
        checkArgNotNull(action, "action");

        if (n == 0) {
            return this;
        }

        final Throwables.Predicate<T, E> filter = new Throwables.Predicate<>() {
            final MutableLong cnt = MutableLong.of(n);

            public boolean test(T value) {
                return cnt.getAndDecrement() > 0;
            }
        };

        return dropWhile(filter, action);
    }

    /**
     *
     *
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> skipNulls() {
        return filter(Fnn.notNull());
    }

    /**
     *
     *
     * @return
     * @deprecated Use {@link #skipNulls()} instead
     */
    @Deprecated
    @IntermediateOp
    public CheckedStream<T, E> skipNull() {
        return skipNulls();
    }

    /**
     *
     * @param maxSize
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> limit(final long maxSize) {
        assertNotClosed();

        checkArgNotNegative(maxSize, "maxSize");

        return newStream(new Throwables.Iterator<T, E>() {
            private long cnt = 0;

            @Override
            public boolean hasNext() throws E {
                return cnt < maxSize && elements.hasNext();
            }

            @Override
            public T next() throws E {
                if (cnt >= maxSize) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                cnt++;
                return elements.next();
            }

        }, sorted, cmp, closeHandlers);
    }

    //    /**
    //     *
    //     * @param from
    //     * @param to
    //     * @return
    //     * @deprecated
    //     */
    //    @Deprecated
    //    public Seq<T, E> slice(final long from, final long to) {
    //        checkArgNotNegative(from, "from");
    //        checkArgNotNegative(to, "to");
    //        checkArgument(to >= from, "'to' can't be less than `from`");
    //
    //        return from == 0 ? limit(to) : skip(from).limit(to - from);
    //    }

    /**
     *
     *
     * @param n
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> top(int n) {
        assertNotClosed();

        return top(n, (Comparator<T>) Comparators.nullsFirst());
    }

    /**
     *
     *
     * @param n
     * @param comparator
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> top(final int n, final Comparator<? super T> comparator) {
        assertNotClosed();

        checkArgPositive(n, "n");

        return newStream(new Throwables.Iterator<T, E>() {
            private boolean initialized = false;
            private T[] aar = null;
            private int cursor = 0;
            private int to;

            @Override
            public boolean hasNext() throws E {
                if (!initialized) {
                    init();
                }

                return cursor < to;
            }

            @Override
            public T next() throws E {
                if (!initialized) {
                    init();
                }

                if (cursor >= to) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return aar[cursor++];
            }

            @Override
            public long count() throws E {
                if (!initialized) {
                    init();
                }

                return to - cursor; //NOSONAR
            }

            @Override
            public void advance(long n) throws E {
                if (!initialized) {
                    init();
                }

                cursor = n < to - cursor ? cursor + (int) n : to;
            }

            //                //    public <A> A[] toArray(A[] b) throws E {
            //        if (initialized == false) {
            //            init();
            //        }
            //
            //        b = b.length >= to - cursor ? b : (A[]) N.newArray(b.getClass().getComponentType(), to - cursor);
            //
            //        N.copy(aar, cursor, b, 0, to - cursor);
            //
            //        return b;
            //    }

            private void init() throws E {
                if (!initialized) {
                    initialized = true;
                    if (sorted && isSameComparator(comparator, cmp)) {
                        final LinkedList<T> queue = new LinkedList<>();

                        while (elements.hasNext()) {
                            if (queue.size() >= n) {
                                queue.poll();
                            }

                            queue.offer(elements.next());
                        }

                        aar = (T[]) queue.toArray();
                    } else {
                        final Queue<T> heap = new PriorityQueue<>(n, comparator);

                        T next = null;
                        while (elements.hasNext()) {
                            next = elements.next();

                            if (heap.size() >= n) {
                                if (comparator.compare(next, heap.peek()) > 0) {
                                    heap.poll();
                                    heap.offer(next);
                                }
                            } else {
                                heap.offer(next);
                            }
                        }

                        aar = (T[]) heap.toArray();
                    }

                    to = aar.length;
                }
            }
        }, false, null, closeHandlers);
    }

    /**
     * A queue with size up to <code>n</code> will be maintained to filter out the last <code>n</code> elements.
     * It may cause <code>out of memory error</code> if <code>n</code> is big enough.
     *
     * <br />
     *
     * All the elements will be loaded to get the last {@code n} elements and the Stream will be closed after that, if a terminal operation is triggered.
     *
     * @param n
     * @return
     * @see Stream#last(int)
     */
    @Beta
    @IntermediateOp
    public CheckedStream<T, E> last(final int n) {
        assertNotClosed();

        checkArgNotNegative(n, "n");

        if (n == 0) {
            return limit(0);
        }

        return newStream(new Throwables.Iterator<T, E>() {
            private Iterator<T> iter;
            private boolean initialized = false;

            @Override
            public boolean hasNext() throws E {
                if (!initialized) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public T next() throws E {
                if (!initialized) {
                    init();
                }

                return iter.next();
            }

            private void init() throws E {
                if (!initialized) {
                    initialized = true;

                    final Deque<T> deque = new ArrayDeque<>(Math.min(1024, n));

                    try {
                        while (elements.hasNext()) {
                            if (deque.size() >= n) {
                                deque.pollFirst();
                            }

                            deque.offerLast(elements.next());
                        }
                    } finally {
                        CheckedStream.this.close();
                    }

                    iter = deque.iterator();
                }
            }
        }, sorted, cmp, closeHandlers);
    }

    /**
     *
     *
     * @param n
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> skipLast(final int n) {
        assertNotClosed();

        if (n <= 0) {
            return newStream(elements, sorted, cmp, closeHandlers);
        }

        return newStream(new Throwables.Iterator<T, E>() {
            private Deque<T> deque = null;

            @Override
            public boolean hasNext() throws E {
                if (deque == null) {
                    deque = new ArrayDeque<>(Math.min(1024, n));

                    while (deque.size() < n && elements.hasNext()) {
                        deque.offerLast(elements.next());
                    }
                }

                return elements.hasNext();
            }

            @Override
            public T next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                deque.offerLast(elements.next());

                return deque.pollFirst();
            }

        }, sorted, cmp, closeHandlers);
    }

    /**
     *
     *
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    public CheckedStream<T, E> reversed() {
        assertNotClosed();

        return newStream(new Throwables.Iterator<T, E>() {
            private boolean initialized = false;
            private T[] aar;
            private int cursor;

            @Override
            public boolean hasNext() throws E {
                if (!initialized) {
                    init();
                }

                return cursor > 0;
            }

            @Override
            public T next() throws E {
                if (!initialized) {
                    init();
                }

                if (cursor <= 0) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return aar[--cursor];
            }

            @Override
            public long count() throws E {
                if (!initialized) {
                    init();
                }

                return cursor;
            }

            @Override
            public void advance(long n) throws E {
                if (!initialized) {
                    init();
                }

                cursor = n < cursor ? cursor - (int) n : 0;
            }

            private void init() throws E {
                if (!initialized) {
                    initialized = true;
                    aar = (T[]) CheckedStream.this.toArrayForIntermediateOp();
                    cursor = aar.length;
                }
            }
        }, false, null, closeHandlers);
    }

    /**
     *
     *
     * @param distance
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    public CheckedStream<T, E> rotated(final int distance) {
        assertNotClosed();

        if (distance == 0) {
            return newStream(elements, closeHandlers);
        }

        return newStream(new Throwables.Iterator<T, E>() {
            private boolean initialized = false;
            private T[] aar;
            private int len;
            private int start;
            private int cnt = 0;

            @Override
            public boolean hasNext() throws E {
                if (!initialized) {
                    init();
                }

                return cnt < len;
            }

            @Override
            public T next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return aar[(start + cnt++) % len];
            }

            @Override
            public long count() throws E {
                if (!initialized) {
                    init();
                }

                return len - cnt; //NOSONAR
            }

            @Override
            public void advance(long n) throws E {
                if (!initialized) {
                    init();
                }

                cnt = n < len - cnt ? cnt + (int) n : len;
            }

            private void init() throws E {
                if (!initialized) {
                    initialized = true;
                    aar = (T[]) CheckedStream.this.toArrayForIntermediateOp();
                    len = aar.length;

                    if (len > 0) {
                        start = distance % len;

                        if (start < 0) {
                            start += len;
                        }

                        start = len - start;
                    }
                }
            }
        }, false, null, closeHandlers);
    }

    /**
     *
     *
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    public CheckedStream<T, E> shuffled() {
        return shuffled(RAND);
    }

    /**
     *
     *
     * @param rnd
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    public CheckedStream<T, E> shuffled(final Random rnd) {
        assertNotClosed();

        return lazyLoad(a -> {
            N.shuffle(a, rnd);
            return a;
        }, false, null);
    }

    /**
     *
     *
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    public CheckedStream<T, E> sorted() {
        return sorted(NATURAL_COMPARATOR);
    }

    /**
     *
     * @param comparator
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    public CheckedStream<T, E> sorted(final Comparator<? super T> comparator) {
        assertNotClosed();

        final Comparator<? super T> cmp = comparator == null ? NATURAL_COMPARATOR : comparator; //NOSONAR

        if (sorted && cmp == this.cmp) {
            return newStream(elements, sorted, comparator, closeHandlers);
        }

        return lazyLoad(a -> {
            N.sort((T[]) a, cmp);

            return a;
        }, true, cmp);
    }

    /**
     *
     *
     * @param keyMapper
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    public CheckedStream<T, E> sortedByInt(ToIntFunction<? super T> keyMapper) {
        final Comparator<? super T> cmpToUse = Comparators.comparingInt(keyMapper);

        return sorted(cmpToUse);
    }

    /**
     *
     *
     * @param keyMapper
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    public CheckedStream<T, E> sortedByLong(ToLongFunction<? super T> keyMapper) {
        final Comparator<? super T> cmpToUse = Comparators.comparingLong(keyMapper);

        return sorted(cmpToUse);
    }

    /**
     *
     *
     * @param keyMapper
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    public CheckedStream<T, E> sortedByDouble(ToDoubleFunction<? super T> keyMapper) {
        final Comparator<? super T> cmpToUse = Comparators.comparingDouble(keyMapper);

        return sorted(cmpToUse);
    }

    /**
     *
     * @param keyMapper
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    @SuppressWarnings("rawtypes")
    public CheckedStream<T, E> sortedBy(final Function<? super T, ? extends Comparable> keyMapper) {
        assertNotClosed();

        final Comparator<? super T> comparator = Comparators.comparingBy(keyMapper);

        return sorted(comparator);
    }

    /**
     *
     *
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    public CheckedStream<T, E> reverseSorted() {
        return sorted(REVERSED_COMPARATOR);
    }

    /**
     *
     *
     * @param comparator
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    public CheckedStream<T, E> reverseSorted(Comparator<? super T> comparator) {
        final Comparator<? super T> cmpToUse = Comparators.reverseOrder(comparator);

        return sorted(cmpToUse);
    }

    /**
     *
     *
     * @param keyMapper
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    public CheckedStream<T, E> reverseSortedByInt(ToIntFunction<? super T> keyMapper) {
        final Comparator<? super T> cmpToUse = Comparators.reversedComparingInt(keyMapper);

        return sorted(cmpToUse);
    }

    /**
     *
     *
     * @param keyMapper
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    public CheckedStream<T, E> reverseSortedByLong(ToLongFunction<? super T> keyMapper) {
        final Comparator<? super T> cmpToUse = Comparators.reversedComparingLong(keyMapper);

        return sorted(cmpToUse);
    }

    /**
     *
     *
     * @param keyMapper
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    public CheckedStream<T, E> reverseSortedByDouble(ToDoubleFunction<? super T> keyMapper) {
        final Comparator<? super T> cmpToUse = Comparators.reversedComparingDouble(keyMapper);

        return sorted(cmpToUse);
    }

    /**
     *
     *
     * @param keyMapper
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    public CheckedStream<T, E> reverseSortedBy(@SuppressWarnings("rawtypes") final Function<? super T, ? extends Comparable> keyMapper) {
        final Comparator<? super T> cmpToUse = Comparators.reversedComparingBy(keyMapper);

        return sorted(cmpToUse);
    }

    /**
     *
     * @param op
     * @param sorted
     * @param cmp
     * @return
     */
    private CheckedStream<T, E> lazyLoad(final Function<Object[], Object[]> op, final boolean sorted, final Comparator<? super T> cmp) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<T, E>() {
            private boolean initialized = false;
            private T[] aar;
            private int cursor = 0;
            private int len;

            @Override
            public boolean hasNext() throws E {
                if (!initialized) {
                    init();
                }

                return cursor < len;
            }

            @Override
            public T next() throws E {
                if (!initialized) {
                    init();
                }

                if (cursor >= len) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return aar[cursor++];
            }

            @Override
            public long count() throws E {
                if (!initialized) {
                    init();
                }

                return len - cursor; //NOSONAR
            }

            @Override
            public void advance(long n) throws E {
                checkArgNotNegative(n, "n");

                if (!initialized) {
                    init();
                }

                cursor = n > len - cursor ? len : cursor + (int) n;
            }

            private void init() throws E {
                if (!initialized) {
                    initialized = true;
                    aar = (T[]) op.apply(CheckedStream.this.toArrayForIntermediateOp());
                    len = aar.length;
                }
            }
        }, sorted, cmp, closeHandlers);
    }

    /**
     *
     *
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> cycled() {
        assertNotClosed();

        return newStream(new Throwables.Iterator<T, E>() {
            private Throwables.Iterator<T, E> iter = null;
            private List<T> list = null;
            private T[] a = null;
            private int len = 0;
            private int cursor = -1;
            private T e = null;

            private boolean initialized = false;

            @Override
            public boolean hasNext() throws E {
                if (!initialized) {
                    init();
                }

                if (a != null) {
                    return len > 0;
                } else if (iter.hasNext()) {
                    return true;
                } else {
                    a = (T[]) list.toArray();
                    len = a.length;
                    cursor = 0;

                    return len > 0;
                }
            }

            @Override
            public T next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (len > 0) {
                    if (cursor >= len) {
                        cursor = 0;
                    }

                    return a[cursor++];
                } else {
                    e = iter.next();
                    list.add(e);

                    return e;
                }
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    iter = CheckedStream.this.iteratorEx();
                    list = new ArrayList<>();
                }
            }
        }, false, null, closeHandlers);
    }

    /**
     *
     *
     * @param rounds
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> cycled(long rounds) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<T, E>() {
            private Throwables.Iterator<T, E> iter = null;
            private List<T> list = null;
            private T[] a = null;
            private int len = 0;
            private int cursor = -1;
            private T e = null;
            private long m = 0;

            private boolean initialized = false;

            @Override
            public boolean hasNext() throws E {
                if (!initialized) {
                    init();
                }

                if (m >= rounds) {
                    return false;
                }

                if (a != null) {
                    return cursor < len || rounds - m > 1;
                } else if (iter.hasNext()) {
                    return true;
                } else {
                    a = (T[]) list.toArray();
                    len = a.length;
                    cursor = 0;
                    m++;

                    return m < rounds && len > 0;
                }
            }

            @Override
            public T next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (len > 0) {
                    if (cursor >= len) {
                        cursor = 0;
                        m++;
                    }

                    return a[cursor++];
                } else {
                    e = iter.next();
                    list.add(e);

                    return e;
                }
            }

            private void init() {
                if (!initialized) {
                    initialized = true;
                    iter = CheckedStream.this.iteratorEx();
                    list = new ArrayList<>();
                }
            }
        }, false, null, closeHandlers);
    }

    /**
     *
     *
     * @param permitsPerSecond
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> rateLimited(double permitsPerSecond) {
        return rateLimited(RateLimiter.create(permitsPerSecond));
    }

    /**
     *
     *
     * @param rateLimiter
     * @return
     * @see RateLimiter#create(double)
     */
    @IntermediateOp
    public CheckedStream<T, E> rateLimited(final RateLimiter rateLimiter) {
        assertNotClosed();
        checkArgNotNull(rateLimiter, "rateLimiter");

        final Throwables.Consumer<T, E> action = it -> rateLimiter.acquire();

        return onEach(action);
    }

    /**
     *
     * @param delay
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> delay(final Duration delay) {
        assertNotClosed();
        checkArgNotNull(delay, "delay");

        final long millis = delay.toMillis();
        final Throwables.Consumer<T, E> action = it -> N.sleepUninterruptibly(millis);

        return onEach(action);
    }

    //
    //    /**
    //     *
    //     *
    //     * @return
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<Timed<T>, E> timed() {
    //        return map(Timed::of);
    //    }

    /**
     *
     *
     * @param delimiter
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> intersperse(final T delimiter) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<T, E>() {
            private final Throwables.Iterator<T, E> iter = iteratorEx();
            private boolean toInsert = false;

            @Override
            public boolean hasNext() throws E {
                return iter.hasNext();
            }

            @Override
            public T next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                if (toInsert) {
                    toInsert = false;
                    return delimiter;
                } else {
                    final T res = iter.next();
                    toInsert = true;
                    return res;
                }
            }
        }, closeHandlers);
    }

    /**
     *
     *
     * @param step
     * @return
     */
    @Beta
    @IntermediateOp
    public CheckedStream<T, E> step(final long step) {
        assertNotClosed();

        checkArgPositive(step, "step");

        if (step == 1) {
            return skip(0);
        }

        final long skip = step - 1;
        final Throwables.Iterator<T, E> iter = this.iteratorEx();

        final Throwables.Iterator<T, E> iterator = new Throwables.Iterator<>() {
            public boolean hasNext() throws E {
                return iter.hasNext();
            }

            public T next() throws E {
                final T next = iter.next();
                iter.advance(skip);
                return next;
            }
        };

        return newStream(iterator, sorted, cmp, closeHandlers);
    }

    /**
     *
     *
     * @return
     */
    @Beta
    @IntermediateOp
    public CheckedStream<Indexed<T>, E> indexed() {
        assertNotClosed();

        return map(new Throwables.Function<T, Indexed<T>, E>() {
            private final MutableLong idx = MutableLong.of(0);

            @Override
            public Indexed<T> apply(T t) {
                return Indexed.of(t, idx.getAndIncrement());
            }
        });
    }

    /**
     * Returns a new Stream with elements from a temporary queue which is filled by reading the elements from this Stream asynchronously with a new thread.
     * Default queue size is 64.
     * <br />
     * Mostly it's for {@code read-write with different threads} mode.
     *
     * @return
     * @see #buffered()
     */
    @IntermediateOp
    public CheckedStream<T, E> buffered() {
        assertNotClosed();

        return buffered(DEFAULT_BUFFERED_SIZE_PER_ITERATOR);
    }

    /**
     * Returns a new Stream with elements from a temporary queue which is filled by reading the elements from this Stream asynchronously with a new thread.
     * <br />
     * Mostly it's for {@code read-write with different threads} mode.
     * @param bufferSize
     * @return
     * @see #buffered(int)
     */
    @IntermediateOp
    public CheckedStream<T, E> buffered(int bufferSize) {
        assertNotClosed();

        checkArgPositive(bufferSize, "bufferSize");

        return buffered(new ArrayBlockingQueue<>(bufferSize));
    }

    CheckedStream<T, E> buffered(final BlockingQueue<T> queueToBuffer) {
        checkArgNotNull(queueToBuffer, "queueToBuffer");
        checkArgument(queueToBuffer.isEmpty(), "'queueToBuffer' must be empty");

        final Supplier<Throwables.Iterator<T, E>> supplier = () -> buffered(iteratorEx(), queueToBuffer);

        return CheckedStream.<Supplier<Throwables.Iterator<T, E>>, E> just(supplier) //
                .map(Supplier::get)
                .flatMap(iter -> newStream(iter, sorted, cmp, mergeCloseHandlers(iter::close, closeHandlers, true)));
    }

    /**
     *
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> mergeWith(final Collection<? extends T> b, final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        assertNotClosed();

        return mergeWith(CheckedStream.of(b), nextSelector);
    }

    /**
     *
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> mergeWith(final CheckedStream<? extends T, E> b,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        assertNotClosed();

        return CheckedStream.merge(this, b, nextSelector);
    }

    /**
     *
     * @param <T2>
     * @param <R>
     * @param b
     * @param zipFunction
     * @return
     */
    @IntermediateOp
    public <T2, R> CheckedStream<R, E> zipWith(final Collection<T2> b,
            final Throwables.BiFunction<? super T, ? super T2, ? extends R, ? extends E> zipFunction) {
        assertNotClosed();

        return zip(this, CheckedStream.of(b), zipFunction);
    }

    /**
     *
     * @param <T2>
     * @param <R>
     * @param b
     * @param valueForNoneA
     * @param valueForNoneB
     * @param zipFunction
     * @return
     */
    @IntermediateOp
    public <T2, R> CheckedStream<R, E> zipWith(final Collection<T2> b, final T valueForNoneA, final T2 valueForNoneB,
            final Throwables.BiFunction<? super T, ? super T2, ? extends R, ? extends E> zipFunction) {
        assertNotClosed();

        return zip(this, CheckedStream.of(b), valueForNoneA, valueForNoneB, zipFunction);
    }

    /**
     *
     * @param <T2>
     * @param <T3>
     * @param <R>
     * @param b
     * @param c
     * @param zipFunction
     * @return
     */
    @IntermediateOp
    public <T2, T3, R> CheckedStream<R, E> zipWith(final Collection<T2> b, final Collection<T3> c,
            final Throwables.TriFunction<? super T, ? super T2, ? super T3, ? extends R, ? extends E> zipFunction) {
        assertNotClosed();

        return zip(this, CheckedStream.of(b), CheckedStream.of(c), zipFunction);
    }

    /**
     *
     * @param <T2>
     * @param <T3>
     * @param <R>
     * @param b
     * @param c
     * @param valueForNoneA
     * @param valueForNoneB
     * @param valueForNoneC
     * @param zipFunction
     * @return
     */
    @IntermediateOp
    public <T2, T3, R> CheckedStream<R, E> zipWith(final Collection<T2> b, final Collection<T3> c, final T valueForNoneA, final T2 valueForNoneB,
            final T3 valueForNoneC, final Throwables.TriFunction<? super T, ? super T2, ? super T3, ? extends R, ? extends E> zipFunction) {
        assertNotClosed();

        return zip(this, CheckedStream.of(b), CheckedStream.of(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    /**
     *
     * @param <T2>
     * @param <R>
     * @param b
     * @param zipFunction
     * @return
     */
    @IntermediateOp
    public <T2, R> CheckedStream<R, E> zipWith(final CheckedStream<T2, E> b,
            final Throwables.BiFunction<? super T, ? super T2, ? extends R, ? extends E> zipFunction) {
        assertNotClosed();

        return zip(this, b, zipFunction);
    }

    /**
     *
     * @param <T2>
     * @param <R>
     * @param b
     * @param valueForNoneA
     * @param valueForNoneB
     * @param zipFunction
     * @return
     */
    @IntermediateOp
    public <T2, R> CheckedStream<R, E> zipWith(final CheckedStream<T2, E> b, final T valueForNoneA, final T2 valueForNoneB,
            final Throwables.BiFunction<? super T, ? super T2, ? extends R, ? extends E> zipFunction) {
        assertNotClosed();

        return zip(this, b, valueForNoneA, valueForNoneB, zipFunction);
    }

    /**
     *
     * @param <T2>
     * @param <T3>
     * @param <R>
     * @param b
     * @param c
     * @param zipFunction
     * @return
     */
    @IntermediateOp
    public <T2, T3, R> CheckedStream<R, E> zipWith(final CheckedStream<T2, E> b, final CheckedStream<T3, E> c,
            final Throwables.TriFunction<? super T, ? super T2, ? super T3, ? extends R, ? extends E> zipFunction) {
        assertNotClosed();

        return zip(this, b, c, zipFunction);
    }

    /**
     *
     * @param <T2>
     * @param <T3>
     * @param <R>
     * @param b
     * @param c
     * @param valueForNoneA
     * @param valueForNoneB
     * @param valueForNoneC
     * @param zipFunction
     * @return
     */
    @IntermediateOp
    public <T2, T3, R> CheckedStream<R, E> zipWith(final CheckedStream<T2, E> b, final CheckedStream<T3, E> c, final T valueForNoneA, final T2 valueForNoneB,
            final T3 valueForNoneC, final Throwables.TriFunction<? super T, ? super T2, ? super T3, ? extends R, ? extends E> zipFunction) {
        assertNotClosed();

        return zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    //    // TODO First of all, it only works in sequential Stream, not parallel stream (and maybe not work in some other scenarios as well).
    //    // Secondly, these onErrorXXX methods make it more difficult and complicated to use Stream.
    //    // So, remove them.
    //    /**
    //     * This method should be only applied sequential {@code Stream} and whose up-streams are sequential {@code Streams} as well.
    //     * Because error happening in the operations executed by parallel stream will stop iteration on that {@Stream}, so the down-streams won't be able to continue.
    //     *
    //     * @param errorConsumer
    //     * @return
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, E> onErrorContinue(final Throwables.Consumer<? super Throwable, ? extends E> errorConsumer) {
    //        assertNotClosed();
    //
    //        return newStream(new Throwables.Iterator<T, E>() {
    //            private final Throwables.Iterator<T, E> iter = iteratorEx();
    //            private final T none = (T) NONE;
    //            private T next = none;
    //            private T ret = null;
    //
    //                //            public boolean hasNext() throws E {
    //                if (next == none) {
    //                    while (true) {
    //                        try {
    //                            if (iter.hasNext()) {
    //                                next = iter.next();
    //                            }
    //
    //                            break;
    //                        } catch (Throwable e) {
    //                            logger.warn("ignoring error in onErrorContinue", e);
    //
    //                            errorConsumer.accept(e);
    //                        }
    //                    }
    //                }
    //
    //                return next != none;
    //            }
    //
    //                //            public T next() throws E {
    //                if (!hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                ret = next;
    //                next = none;
    //                return ret;
    //            }
    //        }, sorted, cmp, closeHandlers);
    //    }
    //
    //    /**
    //     * This method should be only applied sequential {@code Stream} and whose up-streams are sequential {@code Streams} as well.
    //     * Because error happening in the operations executed by parallel stream will stop iteration on that {@Stream}, so the down-streams won't be able to continue.
    //     *
    //     * @param type
    //     * @param errorConsumer
    //     * @return
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, E> onErrorContinue(final Class<? extends Throwable> type,
    //            final Throwables.Consumer<? super Throwable, ? extends E> errorConsumer) {
    //        assertNotClosed();
    //
    //        return newStream(new Throwables.Iterator<T, E>() {
    //            private final Throwables.Iterator<T, E> iter = iteratorEx();
    //            private final T none = (T) NONE;
    //            private T next = none;
    //            private T ret = null;
    //
    //                //            public boolean hasNext() throws E {
    //                if (next == none) {
    //                    while (true) {
    //                        try {
    //                            if (iter.hasNext()) {
    //                                next = iter.next();
    //                            }
    //
    //                            break;
    //                        } catch (Throwable e) {
    //                            if (type.isAssignableFrom(e.getClass())) {
    //                                logger.warn("ignoring error in onErrorContinue", e);
    //
    //                                errorConsumer.accept(e);
    //                            } else {
    //                                throwThrowable(e);
    //                            }
    //                        }
    //                    }
    //                }
    //
    //                return next != none;
    //            }
    //
    //                //            public T next() throws E {
    //                if (!hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                ret = next;
    //                next = none;
    //                return ret;
    //            }
    //        }, sorted, cmp, closeHandlers);
    //    }
    //
    //    /**
    //     * This method should be only applied sequential {@code Stream} and whose up-streams are sequential {@code Streams} as well.
    //     * Because error happening in the operations executed by parallel stream will stop iteration on that {@Stream}, so the down-streams won't be able to continue.
    //     *
    //     * @param errorPredicate
    //     * @param errorConsumer
    //     * @return
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, E> onErrorContinue(final Throwables.Predicate<? super Throwable, ? extends E> errorPredicate,
    //            final Throwables.Consumer<? super Throwable, ? extends E> errorConsumer) {
    //        assertNotClosed();
    //
    //        return newStream(new Throwables.Iterator<T, E>() {
    //            private final Throwables.Iterator<T, E> iter = iteratorEx();
    //            private final T none = (T) NONE;
    //            private T next = none;
    //            private T ret = null;
    //
    //                //            public boolean hasNext() throws E {
    //                if (next == none) {
    //                    while (true) {
    //                        try {
    //                            if (iter.hasNext()) {
    //                                next = iter.next();
    //                            }
    //
    //                            break;
    //                        } catch (Throwable e) {
    //                            if (errorPredicate.test(e)) {
    //                                logger.warn("ignoring error in onErrorContinue", e);
    //
    //                                errorConsumer.accept(e);
    //                            } else {
    //                                throwThrowable(e);
    //                            }
    //                        }
    //                    }
    //                }
    //
    //                return next != none;
    //            }
    //
    //                //            public T next() throws E {
    //                if (!hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                ret = next;
    //                next = none;
    //                return ret;
    //            }
    //        }, sorted, cmp, closeHandlers);
    //    }
    //
    //    /**
    //     * This method should be only applied sequential {@code Stream} and whose up-streams are sequential {@code Streams} as well.
    //     * Because error happening in the operations executed by parallel stream will stop iteration on that {@Stream}, so the down-streams won't be able to continue.
    //     *
    //     * @param errorPredicate
    //     * @param errorConsumer
    //     * @param maxErrorCountToStop
    //     * @return
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, E> onErrorContinue(final Throwables.Predicate<? super Throwable, ? extends E> errorPredicate,
    //            final Throwables.Consumer<? super Throwable, ? extends E> errorConsumer, final int maxErrorCountToStop) {
    //        assertNotClosed();
    //        checkArgNotNegative(maxErrorCountToStop, "maxErrorCountToStop");
    //
    //        return newStream(new Throwables.Iterator<T, E>() {
    //            private final AtomicInteger errorCounter = new AtomicInteger(maxErrorCountToStop);
    //            private final Throwables.Iterator<T, E> iter = iteratorEx();
    //            private final T none = (T) NONE;
    //            private T next = none;
    //            private T ret = null;
    //
    //                //            public boolean hasNext() throws E {
    //                if (next == none) {
    //                    while (true) {
    //                        try {
    //                            if (iter.hasNext()) {
    //                                next = iter.next();
    //                            }
    //
    //                            break;
    //                        } catch (Throwable e) {
    //                            if (errorCounter.decrementAndGet() >= 0) {
    //                                if (errorPredicate.test(e)) {
    //                                    logger.warn("ignoring error in onErrorContinue", e);
    //
    //                                    errorConsumer.accept(e);
    //                                } else {
    //                                    throwThrowable(e);
    //                                }
    //                            } else {
    //                                break;
    //                            }
    //                        }
    //                    }
    //                }
    //
    //                return next != none;
    //            }
    //
    //                //            public T next() throws E {
    //                if (!hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                ret = next;
    //                next = none;
    //                return ret;
    //            }
    //        }, sorted, cmp, closeHandlers);
    //    }
    //
    //    /**
    //     * This method should be only applied sequential {@code Stream} and whose up-streams are sequential {@code Streams} as well.
    //     * Because error happening in the operations executed by parallel stream will stop iteration on that {@Stream}, so the down-streams won't be able to continue.
    //     *
    //     * @param fallbackValue
    //     * @return
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, E> onErrorReturn(final T fallbackValue) {
    //        assertNotClosed();
    //
    //        return newStream(new Throwables.Iterator<T, E>() {
    //            private final Throwables.Iterator<T, E> iter = iteratorEx();
    //            private final T none = (T) NONE;
    //            private T next = none;
    //            private T ret = null;
    //
    //                //            public boolean hasNext() {
    //                if (next == none) {
    //                    try {
    //                        if (iter.hasNext()) {
    //                            next = iter.next();
    //                        }
    //                    } catch (Throwable e) {
    //                        logger.warn("ignoring error in onErrorReturn", e);
    //
    //                        next = fallbackValue;
    //                    }
    //                }
    //
    //                return next != none;
    //            }
    //
    //                //            public T next() {
    //                if (!hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                ret = next;
    //                next = none;
    //                return ret;
    //            }
    //        }, closeHandlers);
    //    }
    //
    //    /**
    //     * This method should be only applied sequential {@code Stream} and whose up-streams are sequential {@code Streams} as well.
    //     * Because error happening in the operations executed by parallel stream will stop iteration on that {@Stream}, so the down-streams won't be able to continue.
    //     *
    //     * @param type
    //     * @param fallbackValue
    //     * @return
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, E> onErrorReturn(final Class<? extends Throwable> type, final T fallbackValue) {
    //        assertNotClosed();
    //
    //        return newStream(new Throwables.Iterator<T, E>() {
    //            private final Throwables.Iterator<T, E> iter = iteratorEx();
    //            private final T none = (T) NONE;
    //            private T next = none;
    //            private T ret = null;
    //
    //                //            public boolean hasNext() throws E {
    //                if (next == none) {
    //                    try {
    //                        if (iter.hasNext()) {
    //                            next = iter.next();
    //                        }
    //                    } catch (Throwable e) {
    //                        if (type.isAssignableFrom(e.getClass())) {
    //                            logger.warn("ignoring error in onErrorReturn", e);
    //
    //                            next = fallbackValue;
    //                        } else {
    //                            throwThrowable(e);
    //                        }
    //                    }
    //                }
    //
    //                return next != none;
    //            }
    //
    //                //            public T next() throws E {
    //                if (!hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                ret = next;
    //                next = none;
    //                return ret;
    //            }
    //        }, closeHandlers);
    //    }
    //
    //    /**
    //     * This method should be only applied sequential {@code Stream} and whose up-streams are sequential {@code Streams} as well.
    //     * Because error happening in the operations executed by parallel stream will stop iteration on that {@Stream}, so the down-streams won't be able to continue.
    //     *
    //     * @param predicate
    //     * @param fallbackValue
    //     * @return
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, E> onErrorReturn(final Throwables.Predicate<? super Throwable, ? extends E> predicate, final T fallbackValue) {
    //        assertNotClosed();
    //
    //        return newStream(new Throwables.Iterator<T, E>() {
    //            private final Throwables.Iterator<T, E> iter = iteratorEx();
    //            private final T none = (T) NONE;
    //            private T next = none;
    //            private T ret = null;
    //
    //                //            public boolean hasNext() throws E {
    //                if (next == none) {
    //                    try {
    //                        if (iter.hasNext()) {
    //                            next = iter.next();
    //                        }
    //                    } catch (Throwable e) {
    //                        if (predicate.test(e)) {
    //                            logger.warn("ignoring error in onErrorReturn", e);
    //
    //                            next = fallbackValue;
    //                        } else {
    //                            throwThrowable(e);
    //                        }
    //                    }
    //                }
    //
    //                return next != none;
    //            }
    //
    //                //            public T next() throws E {
    //                if (!hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                ret = next;
    //                next = none;
    //                return ret;
    //            }
    //        }, closeHandlers);
    //    }
    //
    //    /**
    //     * This method should be only applied sequential {@code Stream} and whose up-streams are sequential {@code Streams} as well.
    //     * Because error happening in the operations executed by parallel stream will stop iteration on that {@Stream}, so the down-streams won't be able to continue.
    //     *
    //     * @param predicate
    //     * @param supplierForFallbackValue
    //     * @return
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, E> onErrorReturn(final Throwables.Predicate<? super Throwable, ? extends E> predicate,
    //            final Throwables.Supplier<? extends T, ? extends E> supplierForFallbackValue) {
    //        assertNotClosed();
    //
    //        return newStream(new Throwables.Iterator<T, E>() {
    //            private final Throwables.Iterator<T, E> iter = iteratorEx();
    //            private final T none = (T) NONE;
    //            private T next = none;
    //            private T ret = null;
    //
    //                //            public boolean hasNext() throws E {
    //                if (next == none) {
    //                    try {
    //                        if (iter.hasNext()) {
    //                            next = iter.next();
    //                        }
    //                    } catch (Throwable e) {
    //                        if (predicate.test(e)) {
    //                            logger.warn("ignoring error in onErrorReturn", e);
    //
    //                            next = supplierForFallbackValue.get();
    //                        } else {
    //                            throwThrowable(e);
    //                        }
    //                    }
    //                }
    //
    //                return next != none;
    //            }
    //
    //                //            public T next() throws E {
    //                if (!hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                ret = next;
    //                next = none;
    //                return ret;
    //            }
    //        }, closeHandlers);
    //    }
    //
    //    /**
    //     * This method should be only applied sequential {@code Stream} and whose up-streams are sequential {@code Streams} as well.
    //     * Because error happening in the operations executed by parallel stream will stop iteration on that {@Stream}, so the down-streams won't be able to continue.
    //     *
    //     * @param predicate
    //     * @param mapperForFallbackValue
    //     * @param maxErrorCountToStop
    //     * @return
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, E> onErrorReturn(final Throwables.Predicate<? super Throwable, ? extends E> predicate,
    //            final Throwables.Function<? super Throwable, ? extends T, ? extends E> mapperForFallbackValue, final int maxErrorCountToStop) {
    //        assertNotClosed();
    //        checkArgNotNegative(maxErrorCountToStop, "maxErrorCountToStop");
    //
    //        return newStream(new Throwables.Iterator<T, E>() {
    //            private final AtomicInteger errorCounter = new AtomicInteger(maxErrorCountToStop);
    //            private final Throwables.Iterator<T, E> iter = iteratorEx();
    //            private final T none = (T) NONE;
    //            private T next = none;
    //            private T ret = null;
    //
    //                //            public boolean hasNext() throws E {
    //                if (next == none) {
    //                    try {
    //                        if (iter.hasNext()) {
    //                            next = iter.next();
    //                        }
    //                    } catch (Throwable e) {
    //                        if (errorCounter.decrementAndGet() >= 0) {
    //                            if (predicate.test(e)) {
    //                                logger.warn("ignoring error in onErrorReturn", e);
    //
    //                                next = mapperForFallbackValue.apply(e);
    //                            } else {
    //                                throwThrowable(e);
    //                            }
    //                        } else {
    //                            // break;
    //                        }
    //                    }
    //                }
    //
    //                return next != none;
    //            }
    //
    //                //            public T next() throws E {
    //                if (!hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                ret = next;
    //                next = none;
    //                return ret;
    //            }
    //        }, closeHandlers);
    //    }
    //
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, E> onErrorStop() {
    //        assertNotClosed();
    //
    //        return newStream(new Throwables.Iterator<T, E>() {
    //            private final Throwables.Iterator<T, E> iter = iteratorEx();
    //            private final T none = (T) NONE;
    //            private T next = none;
    //            private T ret = null;
    //
    //                //            public boolean hasNext() {
    //                if (next == none) {
    //                    try {
    //                        if (iter.hasNext()) {
    //                            next = iter.next();
    //                        }
    //                    } catch (Throwable e) {
    //                        logger.warn("ignoring error in onErrorStop", e);
    //                    }
    //                }
    //
    //                return next != none;
    //            }
    //
    //                //            public T next() {
    //                if (!hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                ret = next;
    //                next = none;
    //                return ret;
    //            }
    //        }, sorted, cmp, closeHandlers);
    //    }
    //
    //    private void throwThrowable(Throwable e) throws E {
    //        if (e instanceof Error) {
    //            throw (Error) e;
    //        } else {
    //            throw (E) e;
    //        }
    //    }

    //    @Beta
    //    @TerminalOp
    //    public void foreach(java.util.function.Consumer<? super T> action) throws E {
    //        assertNotClosed();
    //
    //        forEach(Fnn.from(action));
    //    }

    /**
     *
     * @param <E2>
     * @param action
     * @throws E the e
     * @throws E2 the e2
     */
    @TerminalOp
    public <E2 extends Exception> void forEach(Throwables.Consumer<? super T, E2> action) throws E, E2 {
        assertNotClosed();

        checkArgNotNull(action, "action");

        try {
            while (elements.hasNext()) {
                action.accept(elements.next());
            }
        } finally {
            close();
        }
    }

    /**
     *
     * @param <E2>
     * @param action
     * @throws E the e
     * @throws E2 the e2
     */
    @TerminalOp
    public <E2 extends Exception> void forEachIndexed(Throwables.IntObjConsumer<? super T, E2> action) throws E, E2 {
        assertNotClosed();

        checkArgNotNull(action, "action");

        final MutableInt idx = MutableInt.of(0);

        try {
            while (elements.hasNext()) {
                action.accept(idx.getAndIncrement(), elements.next());
            }
        } finally {
            close();
        }
    }

    /**
     * Iterate and execute {@code action} until the flag is set true.
     * Flag can only be set after at least one element is iterated and executed by {@code action}.
     *
     * @param <E2>
     * @param action the second parameter is a flag to break the for-each loop.
     *        Set it to {@code true} to break the loop if you don't want to continue the {@code action}.
     *        Iteration on this stream will also be stopped when this flag is set to {@code true}.
     * @throws E
     * @throws E2
     * @see #forEachUntil(MutableBoolean, Throwables.Consumer)
     */
    @Beta
    @TerminalOp
    public <E2 extends Exception> void forEachUntil(final Throwables.BiConsumer<? super T, MutableBoolean, E2> action) throws E, E2 {
        assertNotClosed();

        final MutableBoolean flagToBreak = MutableBoolean.of(false);

        final Throwables.Consumer<? super T, E2> tmp = t -> action.accept(t, flagToBreak);

        takeWhile(value -> flagToBreak.isFalse()).forEach(tmp);
    }

    /**
     * Iterate and execute {@code action} until {@code flagToBreak} is set true.
     * If {@code flagToBreak} is set to true at the begin, there will be no element iterated from stream before this stream is stopped and closed.
     *
     * @param <E2>
     * @param flagToBreak a flag to break the for-each loop.
     *        Set it to {@code true} to break the loop if you don't want to continue the {@code action}.
     *        Iteration on this stream will also be stopped when this flag is set to {@code true}.
     * @param action
     * @throws E
     * @throws E2
     * @see #forEachUntil(Throwables.BiConsumer)
     */
    @Beta
    @TerminalOp
    public <E2 extends Exception> void forEachUntil(final MutableBoolean flagToBreak, final Throwables.Consumer<? super T, E2> action) throws E, E2 {
        assertNotClosed();

        takeWhile(value -> flagToBreak.isFalse()).forEach(action);
    }

    /**
     *
     * @param <E2>
     * @param <E3>
     * @param action
     * @param onComplete
     * @throws E the e
     * @throws E2 the e2
     * @throws E3 the e3
     */
    @TerminalOp
    public <E2 extends Exception, E3 extends Exception> void forEach(final Throwables.Consumer<? super T, E2> action, final Throwables.Runnable<E3> onComplete)
            throws E, E2, E3 {
        assertNotClosed();

        checkArgNotNull(action, "action");
        checkArgNotNull(onComplete, "onComplete");

        try {
            while (elements.hasNext()) {
                action.accept(elements.next());
            }

            onComplete.run();
        } finally {
            close();
        }
    }

    /**
     *
     * @param <U>
     * @param <E2>
     * @param <E3>
     * @param flatMapper
     * @param action
     * @throws E the e
     * @throws E2 the e2
     * @throws E3 the e3
     */
    @TerminalOp
    public <U, E2 extends Exception, E3 extends Exception> void forEach(final Throwables.Function<? super T, ? extends Iterable<? extends U>, E2> flatMapper,
            final Throwables.BiConsumer<? super T, ? super U, E3> action) throws E, E2, E3 {
        assertNotClosed();

        checkArgNotNull(flatMapper, "flatMapper");
        checkArgNotNull(action, "action");

        Iterable<? extends U> c = null;
        T next = null;

        try {
            while (elements.hasNext()) {
                next = elements.next();
                c = flatMapper.apply(next);

                if (c != null) {
                    for (U u : c) {
                        action.accept(next, u);
                    }
                }
            }
        } finally {
            close();
        }
    }

    /**
     *
     * @param <T2>
     * @param <T3>
     * @param <E2>
     * @param <E3>
     * @param <E4>
     * @param flatMapper
     * @param flatMapper2
     * @param action
     * @throws E the e
     * @throws E2 the e2
     * @throws E3 the e3
     * @throws E4 the e4
     */
    @TerminalOp
    public <T2, T3, E2 extends Exception, E3 extends Exception, E4 extends Exception> void forEach(
            final Throwables.Function<? super T, ? extends Iterable<T2>, E2> flatMapper,
            final Throwables.Function<? super T2, ? extends Iterable<T3>, E3> flatMapper2,
            final Throwables.TriConsumer<? super T, ? super T2, ? super T3, E4> action) throws E, E2, E3, E4 {
        assertNotClosed();

        checkArgNotNull(flatMapper, "flatMapper");
        checkArgNotNull(flatMapper2, "flatMapper2");
        checkArgNotNull(action, "action");

        Iterable<T2> c2 = null;
        Iterable<T3> c3 = null;
        T next = null;

        try {
            while (elements.hasNext()) {
                next = elements.next();
                c2 = flatMapper.apply(next);

                if (c2 != null) {
                    for (T2 t2 : c2) {
                        c3 = flatMapper2.apply(t2);

                        if (c3 != null) {
                            for (T3 t3 : c3) {
                                action.accept(next, t2, t3);
                            }
                        }
                    }
                }
            }
        } finally {
            close();
        }
    }

    /**
     * For each pair.
     *
     * @param <E2>
     * @param action
     * @throws E the e
     * @throws E2 the e2
     */
    @TerminalOp
    public <E2 extends Exception> void forEachPair(final Throwables.BiConsumer<? super T, ? super T, E2> action) throws E, E2 {
        forEachPair(1, action);
    }

    /**
     * For each pair.
     *
     * @param <E2>
     * @param increment
     * @param action
     * @throws E the e
     * @throws E2 the e2
     */
    @TerminalOp
    public <E2 extends Exception> void forEachPair(final int increment, final Throwables.BiConsumer<? super T, ? super T, E2> action) throws E, E2 {
        assertNotClosed();

        final int windowSize = 2;
        checkArgPositive(increment, "increment");

        try {
            boolean isFirst = true;
            T prev = null;

            while (elements.hasNext()) {
                if (increment > windowSize && !isFirst) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.next();
                    }

                    if (!elements.hasNext()) {
                        break;
                    }
                }

                if (increment == 1) {
                    action.accept(isFirst ? elements.next() : prev, (prev = (elements.hasNext() ? elements.next() : null)));
                } else {
                    action.accept(elements.next(), elements.hasNext() ? elements.next() : null);
                }

                isFirst = false;
            }
        } finally {
            close();
        }
    }

    /**
     * For each triple.
     *
     * @param <E2>
     * @param action
     * @throws E the e
     * @throws E2 the e2
     */
    @TerminalOp
    public <E2 extends Exception> void forEachTriple(final Throwables.TriConsumer<? super T, ? super T, ? super T, E2> action) throws E, E2 {
        forEachTriple(1, action);
    }

    /**
     * For each triple.
     *
     * @param <E2>
     * @param increment
     * @param action
     * @throws E the e
     * @throws E2 the e2
     */
    @TerminalOp
    public <E2 extends Exception> void forEachTriple(final int increment, final Throwables.TriConsumer<? super T, ? super T, ? super T, E2> action)
            throws E, E2 {
        assertNotClosed();

        final int windowSize = 3;
        checkArgPositive(increment, "increment");

        try {
            boolean isFirst = true;
            T prev = null;
            T prev2 = null;

            while (elements.hasNext()) {
                if (increment > windowSize && !isFirst) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.next();
                    }

                    if (!elements.hasNext()) {
                        break;
                    }
                }

                if (increment == 1) {
                    action.accept(isFirst ? elements.next() : prev2, (prev2 = (isFirst ? (elements.hasNext() ? elements.next() : null) : prev)),
                            (prev = (elements.hasNext() ? elements.next() : null)));

                } else if (increment == 2) {
                    action.accept(isFirst ? elements.next() : prev, elements.hasNext() ? elements.next() : null,
                            (prev = (elements.hasNext() ? elements.next() : null)));
                } else {
                    action.accept(elements.next(), elements.hasNext() ? elements.next() : null, elements.hasNext() ? elements.next() : null);
                }

                isFirst = false;
            }
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @param <E2>
     * @param threadNum
     * @param action
     * @throws E the e
     * @throws E2 the e2
     */
    @Beta
    @TerminalOp
    public <E2 extends Exception> void forEachInParallel(final int threadNum, Throwables.Consumer<? super T, E2> action) throws E, E2 {
        assertNotClosed();

        checkArgNotNull(action, "action");

        try {
            unchecked().parallel(threadNum).forEach(action);
        } catch (Exception e) {
            throw (E) ExceptionUtil.tryToGetOriginalCheckedException(e);
        }
    }

    /**
     *
     *
     * @param <E2>
     * @param threadNum
     * @param action
     * @param executor
     * @throws E the e
     * @throws E2 the e2
     */
    @Beta
    @TerminalOp
    public <E2 extends Exception> void forEachInParallel(final int threadNum, Throwables.Consumer<? super T, E2> action, final Executor executor) throws E, E2 {
        assertNotClosed();

        checkArgNotNull(action, "action");

        try {
            unchecked().parallel(threadNum, executor).forEach(action);
        } catch (Exception e) {
            throw (E) ExceptionUtil.tryToGetOriginalCheckedException(e);
        }
    }

    /**
     *
     * @param comparator
     * @return
     * @throws E the e
     */
    @TerminalOp
    public Optional<T> min(Comparator<? super T> comparator) throws E {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return Optional.empty();
            } else if (sorted && isSameComparator(comparator, comparator)) {
                return Optional.of(elements.next());
            }

            comparator = comparator == null ? (Comparator<T>) Comparators.nullsLast() : comparator;
            T candidate = elements.next();
            T next = null;

            while (elements.hasNext()) {
                next = elements.next();
                if (comparator.compare(next, candidate) < 0) {
                    candidate = next;
                }
            }

            return Optional.of(candidate);
        } finally {
            close();
        }
    }

    /**
     *
     * @param keyMapper
     * @return
     * @throws E the e
     */
    @TerminalOp
    @SuppressWarnings("rawtypes")
    public Optional<T> minBy(final Function<? super T, ? extends Comparable> keyMapper) throws E {
        assertNotClosed();

        checkArgNotNull(keyMapper, "keyMapper");

        final Comparator<? super T> comparator = Comparators.nullsLastBy(keyMapper);

        try {
            return min(comparator);
        } finally {
            close();
        }
    }

    /**
     *
     * @param comparator
     * @return
     * @throws E the e
     */
    @TerminalOp
    public Optional<T> max(Comparator<? super T> comparator) throws E {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return Optional.empty();
            } else if (sorted && isSameComparator(comparator, comparator)) {
                T next = null;

                while (elements.hasNext()) {
                    next = elements.next();
                }

                return Optional.of(next);
            }

            comparator = comparator == null ? (Comparator<T>) Comparators.nullsFirst() : comparator;
            T candidate = elements.next();
            T next = null;

            while (elements.hasNext()) {
                next = elements.next();

                if (comparator.compare(next, candidate) > 0) {
                    candidate = next;
                }
            }

            return Optional.of(candidate);
        } finally {
            close();
        }
    }

    /**
     *
     * @param keyMapper
     * @return
     * @throws E the e
     */
    @TerminalOp
    @SuppressWarnings("rawtypes")
    public Optional<T> maxBy(final Function<? super T, ? extends Comparable> keyMapper) throws E {
        assertNotClosed();

        checkArgNotNull(keyMapper, "keyMapper");

        final Comparator<? super T> comparator = Comparators.nullsFirstBy(keyMapper);

        try {
            return max(comparator);
        } finally {
            close();
        }
    }

    /**
     *
     * @param <E2>
     * @param predicate
     * @return
     * @throws E
     * @throws E2
     */
    @TerminalOp
    public <E2 extends Exception> boolean anyMatch(final Throwables.Predicate<? super T, E2> predicate) throws E, E2 {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.next())) {
                    return true;
                }
            }

            return false;
        } finally {
            close();
        }
    }

    /**
     *
     * @param <E2>
     * @param predicate
     * @return
     * @throws E
     * @throws E2
     */
    @TerminalOp
    public <E2 extends Exception> boolean allMatch(final Throwables.Predicate<? super T, E2> predicate) throws E, E2 {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (!predicate.test(elements.next())) {
                    return false;
                }
            }

            return true;
        } finally {
            close();
        }
    }

    /**
     *
     * @param <E2>
     * @param predicate
     * @return
     * @throws E
     * @throws E2
     */
    @TerminalOp
    public <E2 extends Exception> boolean noneMatch(final Throwables.Predicate<? super T, E2> predicate) throws E, E2 {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.next())) {
                    return false;
                }
            }

            return true;
        } finally {
            close();
        }
    }

    /**
     *
     * @param <E2>
     * @param atLeast
     * @param atMost
     * @param predicate
     * @return
     * @throws E
     * @throws E2
     */
    @TerminalOp
    public <E2 extends Exception> boolean nMatch(final long atLeast, final long atMost, final Throwables.Predicate<? super T, E2> predicate) throws E, E2 {
        assertNotClosed();

        checkArgNotNegative(atLeast, "atLeast");
        checkArgNotNegative(atMost, "atMost");
        checkArgument(atLeast <= atMost, "'atLeast' must be <= 'atMost'");

        long cnt = 0;

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.next()) && (++cnt > atMost)) {
                    return false;
                }
            }
        } finally {
            close();
        }

        return cnt >= atLeast && cnt <= atMost;
    }

    /**
     *
     * @param <E2>
     * @param predicate
     * @return
     * @throws E
     * @throws E2
     */
    @TerminalOp
    public <E2 extends Exception> Optional<T> findFirst(final Throwables.Predicate<? super T, E2> predicate) throws E, E2 {
        assertNotClosed();

        try {
            T next = null;

            while (elements.hasNext()) {
                next = elements.next();

                if (predicate.test(next)) {
                    return Optional.of(next);
                }
            }

            return Optional.<T> empty();
        } finally {
            close();
        }
    }

    /**
     * Returns the first element matched by {@code predicateForFirst} if found or the first element if this stream is not empty
     * Otherwise an empty {@code Optional<T>} will be returned.
     *
     * @param <E2>
     * @param predicateForFirst
     * @return
     * @throws E
     * @throws E2
     */
    @TerminalOp
    public <E2 extends Exception> Optional<T> findFirstOrAny(final Throwables.Predicate<? super T, E2> predicateForFirst) throws E, E2 {
        assertNotClosed();

        try {
            T ret = (T) NONE;
            T next = null;

            while (elements.hasNext()) {
                next = elements.next();

                if (predicateForFirst.test(next)) {
                    return Optional.of(next);
                } else if (ret == NONE) {
                    ret = next;
                }
            }

            return ret == NONE ? Optional.<T> empty() : Optional.of(ret);
        } finally {
            close();
        }
    }

    /**
     * Returns the first element matched by {@code predicateForFirst} if found or the first element matched by {@code predicateForAny}.
     * Otherwise an empty {@code Optional<T>} will be returned.
     *
     * @param <E2>
     * @param <E3>
     * @param predicateForFirst
     * @param predicateForAny
     * @return
     * @throws E
     * @throws E2
     * @throws E3
     */
    @TerminalOp
    public <E2 extends Exception, E3 extends Exception> Optional<T> findFirstOrAny(final Throwables.Predicate<? super T, E2> predicateForFirst,
            final Throwables.Predicate<? super T, E3> predicateForAny) throws E, E2, E3 {
        assertNotClosed();

        try {
            T ret = (T) NONE;
            T next = null;

            while (elements.hasNext()) {
                next = elements.next();

                if (predicateForFirst.test(next)) {
                    return Optional.of(next);
                } else if (ret == NONE && predicateForAny.test(next)) {
                    ret = next;
                }
            }

            return ret == NONE ? Optional.<T> empty() : Optional.of(ret);
        } finally {
            close();
        }
    }

    /**
     * Returns the first element matched by {@code predicateForFirst} if found or the last element if this stream is not empty
     * Otherwise an empty {@code Optional<T>} will be returned.
     *
     * @param <E2>
     * @param predicateForFirst
     * @return
     * @throws E
     * @throws E2
     */
    @TerminalOp
    public <E2 extends Exception> Optional<T> findFirstOrLast(final Throwables.Predicate<? super T, E2> predicateForFirst) throws E, E2 {
        assertNotClosed();

        try {
            T ret = (T) NONE;
            T next = null;

            while (elements.hasNext()) {
                next = elements.next();

                if (predicateForFirst.test(next)) {
                    return Optional.of(next);
                } else {
                    ret = next;
                }
            }

            return ret == NONE ? Optional.<T> empty() : Optional.of(ret);
        } finally {
            close();
        }
    }

    /**
     * Consider using: {@code stream.reversed().findFirst(predicate)} for better performance if possible.
     *
     * @param <E2>
     * @param predicate
     * @return
     * @throws E
     * @throws E2
     */
    @TerminalOp
    public <E2 extends Exception> Optional<T> findLast(final Throwables.Predicate<? super T, E2> predicate) throws E, E2 {
        assertNotClosed();

        try {
            T result = (T) NONE;
            T next = null;

            while (elements.hasNext()) {
                next = elements.next();

                if (predicate.test(next)) {
                    result = next;
                }
            }

            return result == NONE ? Optional.<T> empty() : Optional.of(result);
        } finally {
            close();
        }
    }

    /**
     * Same as {@code findFirst(Throwables.Predicate)}.
     *
     * @param <E2>
     * @param predicate
     * @return
     * @throws E
     * @throws E2
     * @see #findFirst(Throwables.Predicate)
     */
    @TerminalOp
    public <E2 extends Exception> Optional<T> findAny(final Throwables.Predicate<? super T, E2> predicate) throws E, E2 {
        return findFirst(predicate);
    }

    /**
     *
     *
     * @param a
     * @return
     * @throws E
     */
    @TerminalOp
    @SafeVarargs
    public final boolean containsAll(final T... a) throws E {
        assertNotClosed();

        try {
            if (N.isEmpty(a)) {
                return true;
            } else if (a.length == 1 || (a.length == 2 && N.equals(a[0], a[1]))) {
                return anyMatch(Fnn.<T, E> pp(Fn.<T> equal(a[0])));
            } else if (a.length == 2) {
                return filter(new Throwables.Predicate<T, E>() {
                    private final T val1 = a[0];
                    private final T val2 = a[1];

                    @Override
                    public boolean test(T t) {
                        return N.equals(t, val1) || N.equals(t, val2);
                    }
                }).distinct().limit(2).count() == 2;
            } else {
                return containsAll(N.asSet(a));
            }
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @param c
     * @return
     * @throws E
     */
    @TerminalOp
    public boolean containsAll(final Collection<? extends T> c) throws E {
        assertNotClosed();

        try {
            if (N.isEmpty(c)) {
                return true;
            } else if (c.size() == 1) {
                final T val = c instanceof List ? ((List<T>) c).get(0) : c.iterator().next();
                return anyMatch(Fnn.<T, E> pp(Fn.<T> equal(val)));
            } else {
                final Set<T> set = c instanceof Set ? (Set<T>) c : N.newHashSet(c);
                final int distinctCount = set.size();

                return filter(set::contains).distinct().limit(distinctCount).count() == distinctCount;
            }
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @param a
     * @return
     * @throws E
     */
    @TerminalOp
    @SafeVarargs
    public final boolean containsAny(final T... a) throws E {
        assertNotClosed();

        try {
            if (N.isEmpty(a)) {
                return false;
            } else if (a.length == 1 || (a.length == 2 && N.equals(a[0], a[1]))) {
                return anyMatch(Fnn.<T, E> pp(Fn.<T> equal(a[0])));
            } else if (a.length == 2) {
                return anyMatch(new Throwables.Predicate<T, E>() {
                    private final T val1 = a[0];
                    private final T val2 = a[1];

                    @Override
                    public boolean test(T t) {
                        return N.equals(t, val1) || N.equals(t, val2);
                    }
                });
            } else {
                final Set<T> set = N.asSet(a);

                return anyMatch(set::contains);
            }
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @param c
     * @return
     * @throws E
     */
    @TerminalOp
    public boolean containsAny(final Collection<? extends T> c) throws E {
        assertNotClosed();

        try {
            if (N.isEmpty(c)) {
                return false;
            } else if (c.size() == 1) {
                final T val = c instanceof List ? ((List<T>) c).get(0) : c.iterator().next();
                return anyMatch(Fnn.<T, E> pp(Fn.<T> equal(val)));
            } else {
                final Set<T> set = c instanceof Set ? (Set<T>) c : N.newHashSet(c);

                return anyMatch(set::contains);
            }
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @return
     * @throws E
     */
    @TerminalOp
    public boolean hasDuplicates() throws E {
        assertNotClosed();

        try {
            final Set<T> set = N.newHashSet();

            while (elements.hasNext()) {
                if (!set.add(elements.next())) {
                    return true;
                }
            }

            return false;
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @param k
     * @param comparator
     * @return
     * @throws E
     */
    @TerminalOp
    public Optional<T> kthLargest(int k, Comparator<? super T> comparator) throws E {
        assertNotClosed();

        checkArgPositive(k, "k");

        try {
            if (!elements.hasNext()) {
                return Optional.empty();
            } else if (sorted && isSameComparator(comparator, this.cmp)) {
                final LinkedList<T> queue = new LinkedList<>();

                while (elements.hasNext()) {
                    if (queue.size() >= k) {
                        queue.poll();
                    }

                    queue.offer(elements.next());
                }

                return queue.size() < k ? (Optional<T>) Optional.empty() : Optional.of(queue.peek());
            }

            comparator = comparator == null ? (Comparator<T>) Comparators.nullsFirst() : comparator;
            final Queue<T> queue = new PriorityQueue<>(k, comparator);
            T e = null;

            while (elements.hasNext()) {
                e = elements.next();

                if (queue.size() < k) {
                    queue.offer(e);
                } else {
                    if (comparator.compare(e, queue.peek()) > 0) {
                        queue.poll();
                        queue.offer(e);
                    }
                }
            }

            return queue.size() < k ? (Optional<T>) Optional.empty() : Optional.of(queue.peek());
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @param comparator
     * @return
     * @throws E
     */
    @TerminalOp
    public Optional<Map<Percentage, T>> percentiles(Comparator<? super T> comparator) throws E {
        assertNotClosed();

        try {
            final Object[] a = sorted(comparator).toArray();

            if (N.isEmpty(a)) {
                return Optional.empty();
            }

            return Optional.of((Map<Percentage, T>) N.percentiles(a));
        } finally {
            close();
        }
    }

    /**
     *
     * @return
     * @throws E the e
     */
    @TerminalOp
    public Optional<T> first() throws E {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return Optional.empty();
            }

            return Optional.of(elements.next());
        } finally {
            close();
        }
    }

    /**
     *
     * @return
     * @throws E the e
     */
    @TerminalOp
    public Optional<T> last() throws E {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return Optional.empty();
            }

            T next = elements.next();

            while (elements.hasNext()) {
                next = elements.next();
            }

            return Optional.of(next);
        } finally {
            close();
        }
    }

    /**
     * @param position in current stream(not upstream or origin source). It starts from 0.
     * @return
     * @throws E the e
     */
    @Beta
    @TerminalOp
    public Optional<T> elementAt(final long position) throws E {
        assertNotClosed();

        checkArgNotNegative(position, "position");

        if (position == 0) {
            return first();
        } else {
            return skip(position).first();
        }
    }

    /**
     *
     * @return
     * @throws TooManyElementsException if there are more than one elements.
     * @throws E the e
     */
    @TerminalOp
    public Optional<T> onlyOne() throws TooManyElementsException, E {
        assertNotClosed();

        try {
            Optional<T> result = Optional.empty();

            if (elements.hasNext()) {
                result = Optional.of(elements.next());

                if (elements.hasNext()) {
                    throw new TooManyElementsException("There are at least two elements: " + Strings.concat(result.get(), ", ", elements.next()));
                }
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     *
     * @return
     * @throws E the e
     */
    @TerminalOp
    public long count() throws E {
        assertNotClosed();

        try {
            return elements.count();
        } finally {
            close();
        }
    }

    /**
     *
     * @return
     * @throws E the e
     */
    @TerminalOp
    public Object[] toArray() throws E {
        return toArray(true);
    }

    Object[] toArray(final boolean closeStream) throws E {
        assertNotClosed();

        try {
            return toList().toArray();
        } finally {
            if (closeStream) {
                close();
            }
        }
    }

    Object[] toArrayForIntermediateOp() throws E {
        // return toArray(false);

        return toArray(true);
    }

    /**
     *
     * @param <A>
     * @param generator
     * @return
     * @throws E the e
     */
    @TerminalOp
    public <A> A[] toArray(IntFunction<A[]> generator) throws E {
        assertNotClosed();

        checkArgNotNull(generator, "generator");

        try {
            final List<T> list = toList();

            return list.toArray(generator.apply(list.size()));
        } finally {
            close();
        }
    }

    /**
     *
     * @return
     * @throws E the e
     */
    @TerminalOp
    public List<T> toList() throws E {
        assertNotClosed();

        try {
            final List<T> result = new ArrayList<>();

            while (elements.hasNext()) {
                result.add(elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     *
     * @return
     * @throws E the e
     */
    @TerminalOp
    public Set<T> toSet() throws E {
        assertNotClosed();

        try {
            final Set<T> result = N.newHashSet();

            while (elements.hasNext()) {
                result.add(elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     *
     * @return
     * @throws E the e
     */
    @TerminalOp
    public ImmutableList<T> toImmutableList() throws E {
        return ImmutableList.wrap(toList());
    }

    /**
     *
     * @return
     * @throws E the e
     */
    @TerminalOp
    public ImmutableSet<T> toImmutableSet() throws E {
        return ImmutableSet.wrap(toSet());
    }

    /**
     *
     * @param <C>
     * @param supplier
     * @return
     * @throws E the e
     */
    @TerminalOp
    public <C extends Collection<T>> C toCollection(final Supplier<? extends C> supplier) throws E {
        assertNotClosed();

        checkArgNotNull(supplier, "supplier");

        try {
            final C result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @param <R>
     * @param <E2>
     * @param func
     * @return
     * @throws E
     * @throws E2
     */
    @TerminalOp
    public <R, E2 extends Exception> R toListThenApply(Throwables.Function<? super List<T>, R, E2> func) throws E, E2 {
        assertNotClosed();

        return func.apply(toList());
    }

    /**
     *
     *
     * @param <E2>
     * @param consumer
     * @throws E
     * @throws E2
     */
    @TerminalOp
    public <E2 extends Exception> void toListThenAccept(Throwables.Consumer<? super List<T>, E2> consumer) throws E, E2 {
        assertNotClosed();

        consumer.accept(toList());
    }

    /**
     *
     *
     * @param <R>
     * @param <E2>
     * @param func
     * @return
     * @throws E
     * @throws E2
     */
    @TerminalOp
    public <R, E2 extends Exception> R toSetThenApply(Throwables.Function<? super Set<T>, R, E2> func) throws E, E2 {
        assertNotClosed();

        return func.apply(toSet());
    }

    /**
     *
     *
     * @param <E2>
     * @param consumer
     * @throws E
     * @throws E2
     */
    @TerminalOp
    public <E2 extends Exception> void toSetThenAccept(Throwables.Consumer<? super Set<T>, E2> consumer) throws E, E2 {
        assertNotClosed();

        consumer.accept(toSet());
    }

    /**
     *
     *
     * @param <R>
     * @param <CC>
     * @param <E2>
     * @param supplier
     * @param func
     * @return
     * @throws E
     * @throws E2
     */
    @TerminalOp
    public <R, CC extends Collection<T>, E2 extends Exception> R toCollectionThenApply(Supplier<? extends CC> supplier,
            Throwables.Function<? super CC, R, E2> func) throws E, E2 {
        assertNotClosed();

        return func.apply(toCollection(supplier));
    }

    /**
     *
     *
     * @param <CC>
     * @param <E2>
     * @param supplier
     * @param consumer
     * @throws E
     * @throws E2
     */
    @TerminalOp
    public <CC extends Collection<T>, E2 extends Exception> void toCollectionThenAccept(Supplier<? extends CC> supplier,
            Throwables.Consumer<? super CC, E2> consumer) throws E, E2 {
        assertNotClosed();

        consumer.accept(toCollection(supplier));
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <E2>
     * @param <E3>
     * @param keyMapper
     * @param valueMapper
     * @return
     * @throws E the e
     * @throws E2
     * @throws E3
     * @throws IllegalStateException if there are duplicated keys.
     * @see {@link Fn.Fnn#throwingMerger()}
     * @see {@link Fn.Fnn#replacingMerger()}
     * @see {@link Fn.Fnn#ignoringMerger()}
     */
    @TerminalOp
    public <K, V, E2 extends Exception, E3 extends Exception> ImmutableMap<K, V> toImmutableMap(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Throwables.Function<? super T, ? extends V, E3> valueMapper) throws E, E2, E3, IllegalStateException {
        return ImmutableMap.wrap(toMap(keyMapper, valueMapper));
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <E2>
     * @param <E3>
     * @param <E4>
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @return
     * @throws E the e
     * @throws E2
     * @throws E3
     * @throws E4
     * @see {@link Fn.Fnn#throwingMerger()}
     * @see {@link Fn.Fnn#replacingMerger()}
     * @see {@link Fn.Fnn#ignoringMerger()}
     */
    @TerminalOp
    public <K, V, E2 extends Exception, E3 extends Exception, E4 extends Exception> ImmutableMap<K, V> toImmutableMap(
            final Throwables.Function<? super T, ? extends K, E2> keyMapper, final Throwables.Function<? super T, ? extends V, E3> valueMapper,
            final Throwables.BinaryOperator<V, E4> mergeFunction) throws E, E2, E3, E4 {
        return ImmutableMap.wrap(toMap(keyMapper, valueMapper, mergeFunction));
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <E2>
     * @param <E3>
     * @param keyMapper
     * @param valueMapper
     * @return
     * @throws E the e
     * @throws E2
     * @throws E3
     * @throws IllegalStateException if there are duplicated keys.
     * @see {@link Fn.Fnn#throwingMerger()}
     * @see {@link Fn.Fnn#replacingMerger()}
     * @see {@link Fn.Fnn#ignoringMerger()}
     */
    @TerminalOp
    public <K, V, E2 extends Exception, E3 extends Exception> Map<K, V> toMap(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Throwables.Function<? super T, ? extends V, E3> valueMapper) throws E, E2, E3, IllegalStateException {
        return toMap(keyMapper, valueMapper, Suppliers.<K, V> ofMap());
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param <E2>
     * @param <E3>
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @throws E the e
     * @throws E2
     * @throws E3
     * @throws IllegalStateException if there are duplicated keys.
     * @see {@link Fn.Fnn#throwingMerger()}
     * @see {@link Fn.Fnn#replacingMerger()}
     * @see {@link Fn.Fnn#ignoringMerger()}
     */
    @TerminalOp
    public <K, V, M extends Map<K, V>, E2 extends Exception, E3 extends Exception> M toMap(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Throwables.Function<? super T, ? extends V, E3> valueMapper, final Supplier<? extends M> mapFactory) throws E, E2, E3, IllegalStateException {
        return toMap(keyMapper, valueMapper, Fnn.<V, E> throwingMerger(), mapFactory);
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <E2>
     * @param <E3>
     * @param <E4>
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @return
     * @throws E the e
     * @throws E2
     * @throws E3
     * @throws E4
     * @see {@link Fn.Fnn#throwingMerger()}
     * @see {@link Fn.Fnn#replacingMerger()}
     * @see {@link Fn.Fnn#ignoringMerger()}
     */
    @TerminalOp
    public <K, V, E2 extends Exception, E3 extends Exception, E4 extends Exception> Map<K, V> toMap(
            final Throwables.Function<? super T, ? extends K, E2> keyMapper, final Throwables.Function<? super T, ? extends V, E3> valueMapper,
            final Throwables.BinaryOperator<V, E4> mergeFunction) throws E, E2, E3, E4 {
        return toMap(keyMapper, valueMapper, mergeFunction, Suppliers.<K, V> ofMap());
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param <E2>
     * @param <E3>
     * @param <E4>
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @param mapFactory
     * @return
     * @throws E the e
     * @throws E2
     * @throws E3
     * @throws E4
     * @see {@link Fn.Fnn#throwingMerger()}
     * @see {@link Fn.Fnn#replacingMerger()}
     * @see {@link Fn.Fnn#ignoringMerger()}
     */
    @TerminalOp
    public <K, V, M extends Map<K, V>, E2 extends Exception, E3 extends Exception, E4 extends Exception> M toMap(
            final Throwables.Function<? super T, ? extends K, E2> keyMapper, final Throwables.Function<? super T, ? extends V, E3> valueMapper,
            final Throwables.BinaryOperator<V, E4> mergeFunction, final Supplier<? extends M> mapFactory) throws E, E2, E3, E4 {
        assertNotClosed();

        checkArgNotNull(keyMapper, "keyMapper");
        checkArgNotNull(valueMapper, "valueMapper");
        checkArgNotNull(mergeFunction, "mergeFunction");
        checkArgNotNull(mapFactory, "mapFactory");

        try {
            final M result = mapFactory.get();
            T next = null;

            while (elements.hasNext()) {
                next = elements.next();
                merge(result, keyMapper.apply(next), valueMapper.apply(next), mergeFunction);
            }

            return result;
        } finally {
            close();
        }
    }

    //    /**
    //     *
    //     * @param keyMapper
    //     * @param downstream
    //     * @return
    //     * @see #groupTo(Throwables.Function, Collector)
    //     * @deprecated replaced by {@code groupTo}
    //     */
    //    @Deprecated
    //    @TerminalOp
    //    public final <K, D, E2 extends Exception> Map<K, D> toMap(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
    //            final Collector<? super T, ?, D> downstream) throws E, E2 {
    //        return groupTo(keyMapper, downstream);
    //    }
    //
    //    /**
    //     *
    //     * @param keyMapper
    //     * @param downstream
    //     * @param mapFactory
    //     * @return
    //     * @see #groupTo(Throwables.Function, Collector, Supplier)
    //     * @deprecated replaced by {@code groupTo}
    //     */
    //    @Deprecated
    //    @TerminalOp
    //    public final <K, D, M extends Map<K, D>, E2 extends Exception> M toMap(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
    //            final Collector<? super T, ?, D> downstream, final Supplier<? extends M> mapFactory) throws E, E2 {
    //        return groupTo(keyMapper, downstream, mapFactory);
    //    }
    //
    //    /**
    //     *
    //     * @param keyMapper
    //     * @param valueMapper
    //     * @param downstream
    //     * @return
    //     * @see #groupTo(Throwables.Function, Throwables.Function, Collector)
    //     * @deprecated replaced by {@code groupTo}
    //     */
    //    @Deprecated
    //    @TerminalOp
    //    public final <K, V, D, E2 extends Exception, E3 extends Exception> Map<K, D> toMap(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
    //            final Throwables.Function<? super T, ? extends V, E3> valueMapper, final Collector<? super V, ?, D> downstream) throws E, E2, E3 {
    //        return groupTo(keyMapper, valueMapper, downstream);
    //    }
    //
    //    /**
    //     *
    //     * @param keyMapper
    //     * @param valueMapper
    //     * @param downstream
    //     * @param mapFactory
    //     * @return
    //     * @see #groupTo(Throwables.Function, Throwables.Function, Collector, Supplier)
    //     * @deprecated replaced by {@code groupTo}
    //     */
    //    @Deprecated
    //    @TerminalOp
    //    public final <K, V, D, M extends Map<K, D>, E2 extends Exception, E3 extends Exception> M toMap(
    //            final Throwables.Function<? super T, ? extends K, E2> keyMapper, final Throwables.Function<? super T, ? extends V, E3> valueMapper,
    //            final Collector<? super V, ?, D> downstream, final Supplier<? extends M> mapFactory) throws E, E2, E3 {
    //        return groupTo(keyMapper, valueMapper, downstream, mapFactory);
    //    }

    /**
     *
     *
     * @param <K> the key type
     * @param <E2>
     * @param keyMapper
     * @return
     * @throws E the e
     * @throws E2
     * @see Collectors#groupingBy(Function)
     */
    @TerminalOp
    public <K, E2 extends Exception> Map<K, List<T>> groupTo(Throwables.Function<? super T, ? extends K, E2> keyMapper) throws E, E2 {
        return groupTo(keyMapper, Suppliers.<K, List<T>> ofMap());
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <M>
     * @param <E2>
     * @param keyMapper
     * @param mapFactory
     * @return
     * @throws E the e
     * @throws E2
     * @see Collectors#groupingBy(Function, Supplier)
     */
    @TerminalOp
    public <K, M extends Map<K, List<T>>, E2 extends Exception> M groupTo(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Supplier<? extends M> mapFactory) throws E, E2 {
        final Throwables.Function<T, T, E> valueMapper = Fnn.identity();

        return groupTo(keyMapper, valueMapper, mapFactory);
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <E2>
     * @param <E3>
     * @param keyMapper
     * @param valueMapper
     * @return
     * @throws E the e
     * @throws E2
     * @throws E3
     */
    @TerminalOp
    public <K, V, E2 extends Exception, E3 extends Exception> Map<K, List<V>> groupTo(Throwables.Function<? super T, ? extends K, E2> keyMapper,
            Throwables.Function<? super T, ? extends V, E3> valueMapper) throws E, E2, E3 {
        return groupTo(keyMapper, valueMapper, Suppliers.<K, List<V>> ofMap());
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param <E2>
     * @param <E3>
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @throws E the e
     * @throws E2
     * @throws E3
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    @TerminalOp
    public <K, V, M extends Map<K, List<V>>, E2 extends Exception, E3 extends Exception> M groupTo(Throwables.Function<? super T, ? extends K, E2> keyMapper,
            Throwables.Function<? super T, ? extends V, E3> valueMapper, Supplier<? extends M> mapFactory) throws E, E2, E3 {
        assertNotClosed();

        checkArgNotNull(keyMapper, "keyMapper");
        checkArgNotNull(valueMapper, "valueMapper");
        checkArgNotNull(mapFactory, "mapFactory");

        try {
            final M result = mapFactory.get();
            T next = null;
            K key = null;

            while (elements.hasNext()) {
                next = elements.next();
                key = keyMapper.apply(next);

                if (!result.containsKey(key)) {
                    result.put(key, new ArrayList<>());
                }

                result.get(key).add(valueMapper.apply(next));
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <D>
     * @param <E2>
     * @param keyMapper
     * @param downstream
     * @return
     * @throws E the e
     * @throws E2
     */
    @TerminalOp
    public <K, D, E2 extends Exception> Map<K, D> groupTo(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Collector<? super T, ?, D> downstream) throws E, E2 {
        return groupTo(keyMapper, downstream, Suppliers.<K, D> ofMap());
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <D>
     * @param <M>
     * @param <E2>
     * @param keyMapper
     * @param downstream
     * @param mapFactory
     * @return
     * @throws E the e
     * @throws E2
     */
    @TerminalOp
    public <K, D, M extends Map<K, D>, E2 extends Exception> M groupTo(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Collector<? super T, ?, D> downstream, final Supplier<? extends M> mapFactory) throws E, E2 {
        return groupTo(keyMapper, Fnn.<T, E> identity(), downstream, mapFactory);
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <D>
     * @param <E2>
     * @param <E3>
     * @param keyMapper
     * @param valueMapper
     * @param downstream
     * @return
     * @throws E the e
     * @throws E2
     * @throws E3
     */
    @TerminalOp
    public <K, V, D, E2 extends Exception, E3 extends Exception> Map<K, D> groupTo(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Throwables.Function<? super T, ? extends V, E3> valueMapper, final Collector<? super V, ?, D> downstream) throws E, E2, E3 {
        return groupTo(keyMapper, valueMapper, downstream, Suppliers.<K, D> ofMap());
    }

    /**
     *
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <D>
     * @param <M>
     * @param <E2>
     * @param <E3>
     * @param keyMapper
     * @param valueMapper
     * @param downstream
     * @param mapFactory
     * @return
     * @throws E the e
     * @throws E2
     * @throws E3
     */
    @TerminalOp
    public <K, V, D, M extends Map<K, D>, E2 extends Exception, E3 extends Exception> M groupTo(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Throwables.Function<? super T, ? extends V, E3> valueMapper, final Collector<? super V, ?, D> downstream,
            final Supplier<? extends M> mapFactory) throws E, E2, E3 {
        assertNotClosed();

        checkArgNotNull(keyMapper, "keyMapper");
        checkArgNotNull(valueMapper, "valueMapper");
        checkArgNotNull(downstream, "downstream");
        checkArgNotNull(mapFactory, "mapFactory");

        try {
            final Supplier<Object> downstreamSupplier = (Supplier<Object>) downstream.supplier();
            final BiConsumer<Object, ? super V> downstreamAccumulator = (BiConsumer<Object, ? super V>) downstream.accumulator();
            final Function<Object, D> downstreamFinisher = (Function<Object, D>) downstream.finisher();

            final M result = mapFactory.get();
            final Map<K, Object> tmp = (Map<K, Object>) result;
            T next = null;
            K key = null;
            Object container = null;

            while (elements.hasNext()) {
                next = elements.next();
                key = keyMapper.apply(next);
                container = tmp.get(key);

                if (container == null) {
                    container = downstreamSupplier.get();
                    tmp.put(key, container);
                }

                downstreamAccumulator.accept(container, valueMapper.apply(next));
            }

            for (Map.Entry<K, D> entry : result.entrySet()) {
                entry.setValue(downstreamFinisher.apply(entry.getValue()));
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @param <E2>
     * @param predicate
     * @return
     * @throws E
     * @throws E2
     * @see Collectors#partitioningBy(Predicate)
     */
    @TerminalOp
    public <E2 extends Exception> Map<Boolean, List<T>> partitionTo(final Throwables.Predicate<? super T, E2> predicate) throws E, E2 {
        assertNotClosed();

        return partitionTo(predicate, Collectors.<T> toList());
    }

    /**
     *
     *
     * @param <D>
     * @param <E2>
     * @param predicate
     * @param downstream
     * @return
     * @throws E
     * @throws E2
     * @see Collectors#partitioningBy(Predicate, Collector)
     */
    @TerminalOp
    public <D, E2 extends Exception> Map<Boolean, D> partitionTo(final Throwables.Predicate<? super T, E2> predicate,
            final Collector<? super T, ?, D> downstream) throws E, E2 {
        assertNotClosed();

        final Function<Object, D> downstreamFinisher = (Function<Object, D>) downstream.finisher();

        final Throwables.Function<T, Boolean, E2> keyMapper = predicate::test;
        final Supplier<Map<Boolean, D>> mapFactory = () -> N.<Boolean, D> newHashMap(2);
        final Map<Boolean, D> map = groupTo(keyMapper, downstream, mapFactory);

        if (!map.containsKey(Boolean.TRUE)) {
            map.put(Boolean.TRUE, downstreamFinisher.apply(downstream.supplier().get()));
        } else if (!map.containsKey(Boolean.FALSE)) {
            map.put(Boolean.FALSE, downstreamFinisher.apply(downstream.supplier().get()));
        }

        return map;
    }

    /**
     *
     *
     * @param <K>
     * @param <E2>
     * @param keyMapper
     * @return
     * @throws E
     * @throws E2
     */
    @TerminalOp
    public <K, E2 extends Exception> ListMultimap<K, T> toMultimap(final Throwables.Function<? super T, ? extends K, E2> keyMapper) throws E, E2 {
        return toMultimap(keyMapper, Suppliers.<K, T> ofListMultimap());
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @param <M>
     * @param <E2>
     * @param keyMapper
     * @param mapFactory
     * @return
     * @throws E
     * @throws E2
     */
    @TerminalOp
    public <K, V extends Collection<T>, M extends Multimap<K, T, V>, E2 extends Exception> M toMultimap(
            final Throwables.Function<? super T, ? extends K, E2> keyMapper, Supplier<? extends M> mapFactory) throws E, E2 {
        final Throwables.Function<T, T, E> valueMapper = Fnn.identity();

        return toMultimap(keyMapper, valueMapper, mapFactory);
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @param <E2>
     * @param <E3>
     * @param keyMapper
     * @param valueMapper
     * @return
     * @throws E
     * @throws E2
     * @throws E3
     */
    @TerminalOp
    public <K, V, E2 extends Exception, E3 extends Exception> ListMultimap<K, V> toMultimap(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Throwables.Function<? super T, ? extends V, E3> valueMapper) throws E, E2, E3 {
        return toMultimap(keyMapper, valueMapper, Suppliers.<K, V> ofListMultimap());
    }

    /**
     *
     *
     * @param <K>
     * @param <V>
     * @param <C>
     * @param <M>
     * @param <E2>
     * @param <E3>
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @throws E
     * @throws E2
     * @throws E3
     */
    @TerminalOp
    public <K, V, C extends Collection<V>, M extends Multimap<K, V, C>, E2 extends Exception, E3 extends Exception> M toMultimap(
            final Throwables.Function<? super T, ? extends K, E2> keyMapper, final Throwables.Function<? super T, ? extends V, E3> valueMapper,
            Supplier<? extends M> mapFactory) throws E, E2, E3 {
        assertNotClosed();

        checkArgNotNull(keyMapper, "keyMapper");
        checkArgNotNull(valueMapper, "valueMapper");
        checkArgNotNull(mapFactory, "mapFactory");

        try {
            final M result = mapFactory.get();
            T next = null;

            while (elements.hasNext()) {
                next = elements.next();

                result.put(keyMapper.apply(next), valueMapper.apply(next));
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @return
     * @throws E
     */
    @TerminalOp
    public Multiset<T> toMultiset() throws E {
        return toMultiset(Suppliers.<T> ofMultiset());
    }

    /**
     *
     *
     * @param supplier
     * @return
     * @throws E
     */
    @TerminalOp
    public Multiset<T> toMultiset(Supplier<? extends Multiset<T>> supplier) throws E { //NOSONAR
        assertNotClosed();

        checkArgNotNull(supplier, "supplier");

        try {
            final Multiset<T> result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @return
     * @throws E
     */
    @TerminalOp
    public LongMultiset<T> toLongMultiset() throws E {
        return toLongMultiset(Suppliers.<T> ofLongMultiset());
    }

    /**
     *
     *
     * @param supplier
     * @return
     * @throws E
     */
    @TerminalOp
    public LongMultiset<T> toLongMultiset(Supplier<? extends LongMultiset<T>> supplier) throws E { //NOSONAR
        assertNotClosed();

        checkArgNotNull(supplier, "supplier");

        try {
            final LongMultiset<T> result = supplier.get();

            while (elements.hasNext()) {
                result.add(elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * The first row will be used as column names if its type is array or list,
     * or obtain the column names from first row if its type is bean or map.
     *
     * @return
     * @throws E
     * @see {@link N#newDataSet(Collection)}
     */
    @Beta
    @TerminalOp
    public DataSet toDataSet() throws E {
        return N.newDataSet(toList());
    }

    /**
     * If the specified {@code columnNames} is null or empty, the first row will be used as column names if its type is array or list,
     * or obtain the column names from first row if its type is bean or map.
     *
     *
     * @param columnNames
     * @return
     * @throws E
     * @see {@link N#newDataSet(Collection, Collection)}
     */
    @TerminalOp
    public DataSet toDataSet(List<String> columnNames) throws E {
        return N.newDataSet(columnNames, toList());
    }

    /**
     *
     *
     * @param <E2>
     * @param func
     * @return
     * @throws E the e
     * @throws E2
     */
    @TerminalOp
    public <E2 extends Exception> long sumInt(Throwables.ToIntFunction<? super T, E2> func) throws E, E2 {
        assertNotClosed();

        try {
            long sum = 0;

            while (elements.hasNext()) {
                sum += func.applyAsInt(elements.next());
            }

            return sum;
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @param <E2>
     * @param func
     * @return
     * @throws E the e
     * @throws E2
     */
    @TerminalOp
    public <E2 extends Exception> long sumLong(Throwables.ToLongFunction<? super T, E2> func) throws E, E2 {
        assertNotClosed();

        try {
            long sum = 0;

            while (elements.hasNext()) {
                sum += func.applyAsLong(elements.next());
            }

            return sum;
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @param <E2>
     * @param func
     * @return
     * @throws E the e
     * @throws E2
     */
    @TerminalOp
    public <E2 extends Exception> double sumDouble(Throwables.ToDoubleFunction<? super T, E2> func) throws E, E2 {
        assertNotClosed();

        try {
            final KahanSummation summation = new KahanSummation();

            while (elements.hasNext()) {
                summation.add(func.applyAsDouble(elements.next()));
            }

            return summation.sum();
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @param <E2>
     * @param func
     * @return
     * @throws E the e
     * @throws E2
     */
    @TerminalOp
    public <E2 extends Exception> OptionalDouble averageInt(Throwables.ToIntFunction<? super T, E2> func) throws E, E2 {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalDouble.empty();
            }

            long sum = 0;
            long count = 0;

            do {
                sum += func.applyAsInt(elements.next());
                count++;
            } while (elements.hasNext());

            return OptionalDouble.of(((double) sum) / count);
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @param <E2>
     * @param func
     * @return
     * @throws E the e
     * @throws E2
     */
    @TerminalOp
    public <E2 extends Exception> OptionalDouble averageLong(Throwables.ToLongFunction<? super T, E2> func) throws E, E2 {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalDouble.empty();
            }

            long sum = 0;
            long count = 0;

            do {
                sum += func.applyAsLong(elements.next());
                count++;
            } while (elements.hasNext());

            return OptionalDouble.of(((double) sum) / count);
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @param <E2>
     * @param func
     * @return
     * @throws E the e
     * @throws E2
     */
    @TerminalOp
    public <E2 extends Exception> OptionalDouble averageDouble(Throwables.ToDoubleFunction<? super T, E2> func) throws E, E2 {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return OptionalDouble.empty();
            }

            final KahanSummation summation = new KahanSummation();

            while (elements.hasNext()) {
                summation.add(func.applyAsDouble(elements.next()));
            }

            return summation.average();
        } finally {
            close();
        }
    }

    /**
     *
     * @param <E2>
     * @param accumulator
     * @return
     * @throws E the e
     * @throws E2 the e2
     */
    @TerminalOp
    public <E2 extends Exception> Optional<T> reduce(Throwables.BinaryOperator<T, E2> accumulator) throws E, E2 {
        assertNotClosed();

        checkArgNotNull(accumulator, "accumulator");

        try {
            if (!elements.hasNext()) {
                return Optional.empty();
            }

            T result = elements.next();

            while (elements.hasNext()) {
                result = accumulator.apply(result, elements.next());
            }

            return Optional.of(result);
        } finally {
            close();
        }
    }

    /**
     *
     * @param <U>
     * @param <E2>
     * @param identity
     * @param accumulator
     * @return
     * @throws E the e
     * @throws E2 the e2
     */
    @TerminalOp
    public <U, E2 extends Exception> U reduce(final U identity, final Throwables.BiFunction<U, ? super T, U, E2> accumulator) throws E, E2 {
        assertNotClosed();

        checkArgNotNull(accumulator, "accumulator");

        try {
            U result = identity;

            while (elements.hasNext()) {
                result = accumulator.apply(result, elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     *
     * @param <E2>
     * @param accumulator
     * @param conditionToBreak the input parameter is the return value of {@code accumulator}, not the element from this Stream.
     *        Returns {@code true} to break the loop if you don't want to continue the {@code action}.
     *        Iteration on this stream will also be stopped when this flag is set to {@code true}.
     * @return
     * @throws E
     * @throws E2
     */
    @Beta
    @TerminalOp
    public <E2 extends Exception> Optional<T> reduceUntil(Throwables.BinaryOperator<T, E2> accumulator, Throwables.Predicate<? super T, E2> conditionToBreak)
            throws E, E2 {
        assertNotClosed();

        checkArgNotNull(accumulator, "accumulator");
        checkArgNotNull(conditionToBreak, "conditionToBreak");

        final MutableBoolean flagToBreak = MutableBoolean.of(false);

        final Throwables.BinaryOperator<T, E2> newAccumulator = (t, u) -> {
            final T ret = accumulator.apply(t, u);

            if (flagToBreak.isFalse() && conditionToBreak.test(ret)) {
                flagToBreak.setValue(true);
            }

            return ret;
        };

        return takeWhile(value -> flagToBreak.isFalse()).reduce(newAccumulator);
    }

    /**
     *
     * @param <U>
     * @param <E2>
     * @param identity
     * @param accumulator
     * @param conditionToBreak the input parameter is the return value of {@code accumulator}, not the element from this Stream.
     *        Returns {@code true} to break the loop if you don't want to continue the {@code action}.
     *        Iteration on this stream will also be stopped when this flag is set to {@code true}.
     * @return
     * @throws E
     * @throws E2
     */
    @Beta
    @TerminalOp
    public <U, E2 extends Exception> U reduceUntil(final U identity, final Throwables.BiFunction<U, ? super T, U, E2> accumulator,
            Throwables.Predicate<? super U, E2> conditionToBreak) throws E, E2 {
        assertNotClosed();

        checkArgNotNull(accumulator, "accumulator");
        checkArgNotNull(conditionToBreak, "conditionToBreak");

        final MutableBoolean flagToBreak = MutableBoolean.of(false);

        final Throwables.BiFunction<U, T, U, E2> newAccumulator = (u, t) -> {
            final U ret = accumulator.apply(u, t);

            if (flagToBreak.isFalse() && conditionToBreak.test(ret)) {
                flagToBreak.setValue(true);
            }

            return ret;
        };

        return takeWhile(value -> flagToBreak.isFalse()).reduce(identity, newAccumulator);
    }

    /**
     *
     * @param <R>
     * @param <E2>
     * @param <E3>
     * @param supplier
     * @param accumulator
     * @return
     * @throws E the e
     * @throws E2 the e2
     * @throws E3 the e3
     */
    @TerminalOp
    public <R, E2 extends Exception, E3 extends Exception> R collect(final Throwables.Supplier<R, E2> supplier,
            final Throwables.BiConsumer<? super R, ? super T, E3> accumulator) throws E, E2, E3 {
        assertNotClosed();

        checkArgNotNull(supplier, "supplier");
        checkArgNotNull(accumulator, "accumulator");

        try {
            final R result = supplier.get();

            while (elements.hasNext()) {
                accumulator.accept(result, elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     *
     * @param <R>
     * @param <RR>
     * @param <E2>
     * @param <E3>
     * @param <E4>
     * @param supplier
     * @param accumulator
     * @param finisher
     * @return
     * @throws E the e
     * @throws E2 the e2
     * @throws E3 the e3
     * @throws E4 the e4
     */
    @TerminalOp
    public <R, RR, E2 extends Exception, E3 extends Exception, E4 extends Exception> RR collect(final Throwables.Supplier<R, E2> supplier,
            final Throwables.BiConsumer<? super R, ? super T, E3> accumulator, final Throwables.Function<? super R, ? extends RR, E4> finisher)
            throws E, E2, E3, E4 {
        assertNotClosed();

        checkArgNotNull(supplier, "supplier");
        checkArgNotNull(accumulator, "accumulator");
        checkArgNotNull(finisher, "finisher");

        try {
            final R result = supplier.get();

            while (elements.hasNext()) {
                accumulator.accept(result, elements.next());
            }

            return finisher.apply(result);
        } finally {
            close();
        }
    }

    /**
     *
     * @param <R>
     * @param collector
     * @return
     * @throws E the e
     */
    @TerminalOp
    public <R> R collect(final Collector<? super T, ?, R> collector) throws E {
        assertNotClosed();

        checkArgNotNull(collector, "collector");

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super T> accumulator = (BiConsumer<Object, ? super T>) collector.accumulator();
        final Function<Object, R> finisher = (Function<Object, R>) collector.finisher();

        try {
            final Object container = supplier.get();

            while (elements.hasNext()) {
                accumulator.accept(container, elements.next());
            }

            return finisher.apply(container);
        } finally {
            close();
        }
    }

    /**
     *
     * @param <R>
     * @param <RR>
     * @param <E2>
     * @param collector
     * @param func
     * @return
     * @throws E the e
     * @throws E2 the e2
     */
    @TerminalOp
    public <R, RR, E2 extends Exception> RR collectThenApply(final Collector<? super T, ?, R> collector,
            final Throwables.Function<? super R, ? extends RR, E2> func) throws E, E2 {
        assertNotClosed();

        checkArgNotNull(collector, "collector");
        checkArgNotNull(func, "func");

        return func.apply(collect(collector));
    }

    /**
     *
     * @param <R>
     * @param <E2>
     * @param collector
     * @param consumer
     * @throws E the e
     * @throws E2 the e2
     */
    @TerminalOp
    public <R, E2 extends Exception> void collectThenAccept(Collector<? super T, ?, R> collector, Throwables.Consumer<? super R, E2> consumer) throws E, E2 {
        assertNotClosed();

        checkArgNotNull(collector, "collector");
        checkArgNotNull(consumer, "consumer");

        consumer.accept(collect(collector));
    }

    /**
     *
     * @param delimiter
     * @return
     * @throws E
     */
    @TerminalOp
    public String join(final CharSequence delimiter) throws E {
        return join(delimiter, "", "");
    }

    /**
     *
     *
     * @param delimiter
     * @param prefix
     * @param suffix
     * @return
     * @throws E
     */
    @TerminalOp
    public String join(CharSequence delimiter, CharSequence prefix, CharSequence suffix) throws E {
        assertNotClosed();

        try {
            final Joiner joiner = Joiner.with(delimiter, prefix, suffix).reuseCachedBuffer();

            while (elements.hasNext()) {
                joiner.append(elements.next());
            }

            return joiner.toString();
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @param joiner
     * @return the input {@code joiner}
     * @throws E
     */
    @TerminalOp
    public Joiner joinTo(final Joiner joiner) throws E {
        assertNotClosed();

        checkArgNotNull(joiner, "joiner");

        try {

            while (elements.hasNext()) {
                joiner.append(elements.next());
            }

            return joiner;
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @return
     * @throws E
     */
    public Optional<Map<Percentage, T>> percentiles() throws E {
        assertNotClosed();

        try {
            final Object[] a = sorted().toArray();

            if (N.isEmpty(a)) {
                return Optional.empty();
            }

            return Optional.of((Map<Percentage, T>) N.percentiles(a));
        } finally {
            close();
        }
    }

    /**
     *
     * @param output
     * @return
     * @see #onEach(com.landawn.abacus.util.Throwables.Consumer)
     * @see #onEachE(com.landawn.abacus.util.Throwables.Consumer)
     */
    @Beta
    @IntermediateOp
    public CheckedStream<T, E> saveEach(final File output) {
        return saveEach(N::stringOf, output);
    }

    /**
     *
     * @param toLine
     * @param output
     * @return
     * @see #onEach(com.landawn.abacus.util.Throwables.Consumer)
     * @see #onEachE(com.landawn.abacus.util.Throwables.Consumer)
     */
    @Beta
    @IntermediateOp
    public CheckedStream<T, E> saveEach(final Throwables.Function<? super T, String, E> toLine, final File output) {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = new Throwables.Iterator<>() {
            private final Throwables.Iterator<T, E> iter = iteratorEx();
            private Writer writer = null;
            private BufferedWriter bw = null;
            private boolean initialized = false;

            public boolean hasNext() throws E {
                return iter.hasNext();
            }

            public T next() throws E {
                final T next = iter.next();

                if (initialized == false) {
                    init();
                }

                try {
                    bw.write(toLine.apply(next));
                    bw.write(IOUtil.LINE_SEPARATOR);
                } catch (IOException e) {
                    throw N.toRuntimeException(e);
                }

                return next;
            }

            protected void closeResource() {
                if (writer != null) {
                    try {
                        Objectory.recycle(bw);
                    } finally {
                        IOUtil.close(writer);
                    }
                }
            }

            private void init() {
                initialized = true;

                writer = IOUtil.newFileWriter(output);
                bw = Objectory.createBufferedWriter(writer);
            }
        };

        return newStream(iter, sorted, cmp, mergeCloseHandlers(iter::close, closeHandlers, true)); //NOSONAR
    }

    /**
     *
     * @param toLine
     * @param output
     * @return
     * @see #onEach(com.landawn.abacus.util.Throwables.Consumer)
     * @see #onEachE(com.landawn.abacus.util.Throwables.Consumer)
     */
    @Beta
    @IntermediateOp
    public CheckedStream<T, E> saveEach(final Throwables.Function<? super T, String, E> toLine, final OutputStream output) {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = new Throwables.Iterator<>() {
            private final Throwables.Iterator<T, E> iter = iteratorEx();
            private BufferedWriter bw = null;
            private boolean initialized = false;

            public boolean hasNext() throws E {
                return iter.hasNext();
            }

            public T next() throws E {
                final T next = iter.next();

                if (initialized == false) {
                    init();
                }

                try {
                    bw.write(toLine.apply(next));
                    bw.write(IOUtil.LINE_SEPARATOR);
                } catch (IOException e) {
                    throw N.toRuntimeException(e);
                }

                return next;
            }

            protected void closeResource() {
                Objectory.recycle(bw);
            }

            private void init() {
                initialized = true;

                bw = Objectory.createBufferedWriter(output);
            }
        };

        return newStream(iter, sorted, cmp, mergeCloseHandlers(iter::close, closeHandlers, true)); //NOSONAR
    }

    /**
     *
     * @param toLine
     * @param output
     * @return
     * @see #onEach(com.landawn.abacus.util.Throwables.Consumer)
     * @see #onEachE(com.landawn.abacus.util.Throwables.Consumer)
     */
    @Beta
    @IntermediateOp
    public CheckedStream<T, E> saveEach(final Throwables.Function<? super T, String, E> toLine, Writer output) {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = new Throwables.Iterator<>() {
            private final Throwables.Iterator<T, E> iter = iteratorEx();
            private Writer bw = null;
            private boolean isBufferedWriter = false;
            private boolean initialized = false;

            public boolean hasNext() throws E {
                return iter.hasNext();
            }

            public T next() throws E {
                final T next = iter.next();

                if (initialized == false) {
                    init();
                }

                try {
                    bw.write(toLine.apply(next));
                    bw.write(IOUtil.LINE_SEPARATOR);
                } catch (IOException e) {
                    throw N.toRuntimeException(e);
                }

                return next;
            }

            protected void closeResource() {
                if (isBufferedWriter == false && bw != null) {
                    Objectory.recycle((BufferedWriter) bw);
                }
            }

            private void init() {
                initialized = true;

                isBufferedWriter = IOUtil.isBufferedWriter(output);
                bw = isBufferedWriter ? output : Objectory.createBufferedWriter(output);
            }
        };

        return newStream(iter, sorted, cmp, mergeCloseHandlers(iter::close, closeHandlers, true)); //NOSONAR
    }

    /**
     *
     * @param writeLine
     * @param output
     * @return
     * @see #onEach(com.landawn.abacus.util.Throwables.Consumer)
     * @see #onEachE(com.landawn.abacus.util.Throwables.Consumer)
     */
    @Beta
    @IntermediateOp
    public CheckedStream<T, E> saveEach(final Throwables.BiConsumer<? super T, Writer, IOException> writeLine, final File output) {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = new Throwables.Iterator<>() {
            private final Throwables.Iterator<T, E> iter = iteratorEx();
            private Writer writer = null;
            private BufferedWriter bw = null;
            private boolean initialized = false;

            public boolean hasNext() throws E {
                return iter.hasNext();
            }

            public T next() throws E {
                final T next = iter.next();

                if (initialized == false) {
                    init();
                }

                try {
                    writeLine.accept(next, bw);
                    bw.write(IOUtil.LINE_SEPARATOR);
                } catch (IOException e) {
                    throw N.toRuntimeException(e);
                }

                return next;
            }

            protected void closeResource() {
                if (writer != null) {
                    try {
                        Objectory.recycle(bw);
                    } finally {
                        IOUtil.close(writer);
                    }
                }
            }

            private void init() {
                initialized = true;

                writer = IOUtil.newFileWriter(output);
                bw = Objectory.createBufferedWriter(writer);
            }
        };

        return newStream(iter, sorted, cmp, mergeCloseHandlers(iter::close, closeHandlers, true)); //NOSONAR
    }

    /**
     *
     * @param writeLine
     * @param output
     * @return
     * @see #onEach(com.landawn.abacus.util.Throwables.Consumer)
     * @see #onEachE(com.landawn.abacus.util.Throwables.Consumer)
     */
    @Beta
    @IntermediateOp
    public CheckedStream<T, E> saveEach(final Throwables.BiConsumer<? super T, Writer, IOException> writeLine, final Writer output) {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = new Throwables.Iterator<>() {
            private final Throwables.Iterator<T, E> iter = iteratorEx();
            private Writer bw = null;
            private boolean isBufferedWriter = false;
            private boolean initialized = false;

            public boolean hasNext() throws E {
                return iter.hasNext();
            }

            public T next() throws E {
                final T next = iter.next();

                if (initialized == false) {
                    init();
                }

                try {
                    writeLine.accept(next, bw);
                    bw.write(IOUtil.LINE_SEPARATOR);
                } catch (IOException e) {
                    throw N.toRuntimeException(e);
                }

                return next;
            }

            protected void closeResource() {
                if (isBufferedWriter == false && bw != null) {
                    Objectory.recycle((BufferedWriter) bw);
                }
            }

            private void init() {
                initialized = true;

                isBufferedWriter = IOUtil.isBufferedWriter(output);
                bw = isBufferedWriter ? output : Objectory.createBufferedWriter(output);
            }
        };

        return newStream(iter, sorted, cmp, mergeCloseHandlers(iter::close, closeHandlers, true)); //NOSONAR
    }

    /**
     * To support batch call, please use {@code onEach} or {@code onEachE}.
     *
     * @param stmt
     * @param stmtSetter
     * @return
     * @see #onEach(com.landawn.abacus.util.Throwables.Consumer)
     * @see #onEachE(com.landawn.abacus.util.Throwables.Consumer)
     */
    @Beta
    @IntermediateOp
    public CheckedStream<T, E> saveEach(final PreparedStatement stmt,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = new Throwables.Iterator<>() {
            private final Throwables.Iterator<T, E> iter = iteratorEx();

            public boolean hasNext() throws E {
                return iter.hasNext();
            }

            public T next() throws E {
                final T next = iter.next();

                try {
                    stmtSetter.accept(next, stmt);
                    stmt.execute();
                } catch (SQLException e) {
                    throw N.toRuntimeException(e);
                }

                return next;
            }

        };

        return newStream(iter, sorted, cmp, mergeCloseHandlers(iter::close, closeHandlers, true)); //NOSONAR
    }

    /**
     * To support batch call, please use {@code onEach} or {@code onEachE}.
     *
     * @param conn
     * @param insertSQL
     * @param stmtSetter
     * @return
     * @see #onEach(com.landawn.abacus.util.Throwables.Consumer)
     * @see #onEachE(com.landawn.abacus.util.Throwables.Consumer)
     */
    @Beta
    @IntermediateOp
    public CheckedStream<T, E> saveEach(final Connection conn, final String insertSQL,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = new Throwables.Iterator<>() {
            private final Throwables.Iterator<T, E> iter = iteratorEx();
            private PreparedStatement stmt = null;
            private boolean initialized = false;

            public boolean hasNext() throws E {
                return iter.hasNext();
            }

            public T next() throws E {
                final T next = iter.next();

                if (initialized == false) {
                    init();
                }

                try {
                    stmtSetter.accept(next, stmt);
                    stmt.execute();
                } catch (SQLException e) {
                    throw N.toRuntimeException(e);
                }

                return next;
            }

            protected void closeResource() {
                if (stmt != null) {
                    DataSourceUtil.closeQuietly(stmt);
                }
            }

            private void init() {
                initialized = true;

                try {
                    stmt = conn.prepareStatement(insertSQL);
                } catch (SQLException e) {
                    throw N.toRuntimeException(e);
                }
            }
        };

        return newStream(iter, sorted, cmp, mergeCloseHandlers(iter::close, closeHandlers, true)); //NOSONAR
    }

    /**
     * To support batch/parallel call, please use {@code onEach} or {@code onEachE}.
     *
     * @param ds
     * @param insertSQL
     * @param stmtSetter
     * @return
     * @see #onEach(com.landawn.abacus.util.Throwables.Consumer)
     * @see #onEachE(com.landawn.abacus.util.Throwables.Consumer)
     * @see #spsOnEach(com.landawn.abacus.util.Throwables.Consumer)
     * @see #spsOnEachE(com.landawn.abacus.util.Throwables.Consumer)
     */
    @Beta
    @IntermediateOp
    public CheckedStream<T, E> saveEach(final javax.sql.DataSource ds, final String insertSQL,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = new Throwables.Iterator<>() {
            private final Throwables.Iterator<T, E> iter = iteratorEx();
            private Connection conn = null;
            private PreparedStatement stmt = null;
            private boolean initialized = false;

            public boolean hasNext() throws E {
                return iter.hasNext();
            }

            public T next() throws E {
                final T next = iter.next();

                if (initialized == false) {
                    init();
                }

                try {
                    stmtSetter.accept(next, stmt);
                    stmt.execute();
                } catch (SQLException e) {
                    throw N.toRuntimeException(e);
                }

                return next;
            }

            protected void closeResource() {
                try {
                    if (stmt != null) {
                        DataSourceUtil.closeQuietly(stmt);
                    }
                } finally {
                    DataSourceUtil.releaseConnection(conn, ds);
                }
            }

            private void init() {
                initialized = true;

                try {
                    conn = ds.getConnection();
                    stmt = conn.prepareStatement(insertSQL);
                } catch (SQLException e) {
                    try {
                        if (stmt != null) {
                            DataSourceUtil.closeQuietly(stmt);
                        }
                    } finally {
                        DataSourceUtil.releaseConnection(conn, ds);
                    }

                    throw N.toRuntimeException(e);
                }
            }
        };

        return newStream(iter, sorted, cmp, mergeCloseHandlers(iter::close, closeHandlers, true)); //NOSONAR
    }

    /**
     *
     *
     * @param output
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persist(final File output) throws E, IOException {
        return persist(N::stringOf, output);
    }

    /**
     *
     *
     * @param header
     * @param tail
     * @param output
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persist(final String header, final String tail, final File output) throws E, IOException {
        return persist(header, tail, N::stringOf, output);
    }

    /**
     *
     * @param toLine
     * @param output
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persist(Throwables.Function<? super T, String, E> toLine, final File output) throws E, IOException {
        return persist(null, null, toLine, output);
    }

    /**
     *
     * @param header
     * @param tail
     * @param toLine
     * @param output
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persist(final String header, final String tail, Throwables.Function<? super T, String, E> toLine, final File output) throws E, IOException {
        assertNotClosed();

        final Writer writer = IOUtil.newFileWriter(output);

        try {
            return persist(header, tail, toLine, writer);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     *
     *
     * @param toLine
     * @param output
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persist(Throwables.Function<? super T, String, E> toLine, final OutputStream output) throws E, IOException {
        assertNotClosed();

        final BufferedWriter bw = Objectory.createBufferedWriter(output);

        try {
            return persist(toLine, bw);
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     *
     * @param header
     * @param tail
     * @param toLine
     * @param output
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persist(String header, String tail, Throwables.Function<? super T, String, E> toLine, OutputStream output) throws E, IOException {
        assertNotClosed();

        final BufferedWriter bw = Objectory.createBufferedWriter(output);

        try {
            return persist(header, tail, toLine, bw);
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     *
     * @param toLine
     * @param output
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persist(Throwables.Function<? super T, String, E> toLine, Writer output) throws E, IOException {
        assertNotClosed();

        return persist(null, null, toLine, output);
    }

    /**
     *
     * @param header
     * @param tail
     * @param toLine
     * @param output
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persist(String header, String tail, Throwables.Function<? super T, String, E> toLine, Writer output) throws E, IOException {
        assertNotClosed();

        try {
            boolean isBufferedWriter = IOUtil.isBufferedWriter(output);
            final Writer bw = isBufferedWriter ? output : Objectory.createBufferedWriter(output); //NOSONAR
            final Throwables.Iterator<T, E> iter = iteratorEx();
            long cnt = 0;

            try {
                if (header != null) {
                    bw.write(header);
                    bw.write(IOUtil.LINE_SEPARATOR);
                }

                while (iter.hasNext()) {
                    bw.write(toLine.apply(iter.next()));
                    bw.write(IOUtil.LINE_SEPARATOR);
                    cnt++;
                }

                if (tail != null) {
                    bw.write(tail);
                    bw.write(IOUtil.LINE_SEPARATOR);
                }

                bw.flush();
            } finally {
                if (!isBufferedWriter) {
                    Objectory.recycle((BufferedWriter) bw);
                }
            }

            return cnt;
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @param writeLine
     * @param output
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persist(final Throwables.BiConsumer<? super T, Writer, IOException> writeLine, final File output) throws E, IOException {
        assertNotClosed();

        return persist(null, null, writeLine, output);
    }

    /**
     *
     *
     * @param header
     * @param tail
     * @param writeLine
     * @param output
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persist(final String header, final String tail, final Throwables.BiConsumer<? super T, Writer, IOException> writeLine, final File output)
            throws E, IOException {
        assertNotClosed();

        final Writer writer = IOUtil.newFileWriter(output);

        try {
            return persist(header, tail, writeLine, writer);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     *
     *
     * @param writeLine
     * @param output
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persist(final Throwables.BiConsumer<? super T, Writer, IOException> writeLine, final Writer output) throws E, IOException {
        assertNotClosed();

        return persist(null, null, writeLine, output);
    }

    /**
     *
     *
     * @param header
     * @param tail
     * @param writeLine
     * @param output
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persist(final String header, final String tail, final Throwables.BiConsumer<? super T, Writer, IOException> writeLine, final Writer output)
            throws E, IOException {
        assertNotClosed();

        try {
            boolean isBufferedWriter = IOUtil.isBufferedWriter(output);
            final Writer bw = isBufferedWriter ? output : Objectory.createBufferedWriter(output); //NOSONAR
            final Throwables.Iterator<T, E> iter = iteratorEx();
            long cnt = 0;

            try {
                if (header != null) {
                    bw.write(header);
                    bw.write(IOUtil.LINE_SEPARATOR);
                }

                while (iter.hasNext()) {
                    writeLine.accept(iter.next(), bw);
                    bw.write(IOUtil.LINE_SEPARATOR);
                    cnt++;
                }

                if (tail != null) {
                    bw.write(tail);
                    bw.write(IOUtil.LINE_SEPARATOR);
                }

                bw.flush();
            } finally {
                if (!isBufferedWriter) {
                    Objectory.recycle((BufferedWriter) bw);
                }
            }

            return cnt;
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @param stmt
     * @param batchSize
     * @param batchIntervalInMillis
     * @param stmtSetter
     * @return
     * @throws E
     * @throws SQLException
     */
    @TerminalOp
    public long persist(final PreparedStatement stmt, final int batchSize, final long batchIntervalInMillis,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) throws E, SQLException {
        assertNotClosed();

        checkArgument(batchSize > 0 && batchIntervalInMillis >= 0, "'batchSize'=%s must be greater than 0 and 'batchIntervalInMillis'=%s can't be negative",
                batchSize, batchIntervalInMillis);

        try {
            final Throwables.Iterator<T, E> iter = iteratorEx();
            long cnt = 0;
            while (iter.hasNext()) {
                stmtSetter.accept(iter.next(), stmt);

                stmt.addBatch();

                if ((++cnt % batchSize) == 0) {
                    DataSourceUtil.executeBatch(stmt);

                    if (batchIntervalInMillis > 0) {
                        N.sleep(batchIntervalInMillis);
                    }
                }
            }

            if ((cnt % batchSize) > 0) {
                DataSourceUtil.executeBatch(stmt);
            }

            return cnt;
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @param conn
     * @param insertSQL
     * @param batchSize
     * @param batchIntervalInMillis
     * @param stmtSetter
     * @return
     * @throws E
     * @throws SQLException
     */
    @TerminalOp
    public long persist(final Connection conn, final String insertSQL, final int batchSize, final long batchIntervalInMillis,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) throws E, SQLException {
        assertNotClosed();

        PreparedStatement stmt = null;

        try {
            stmt = conn.prepareStatement(insertSQL);

            return persist(stmt, batchSize, batchIntervalInMillis, stmtSetter);
        } finally {
            DataSourceUtil.closeQuietly(stmt);
        }
    }

    /**
     *
     * @param ds
     * @param insertSQL
     * @param batchSize
     * @param batchIntervalInMillis
     * @param stmtSetter
     * @return
     * @throws E
     * @throws SQLException
     */
    @TerminalOp
    public long persist(final javax.sql.DataSource ds, final String insertSQL, final int batchSize, final long batchIntervalInMillis,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) throws E, SQLException {
        assertNotClosed();

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            conn = ds.getConnection();
            stmt = conn.prepareStatement(insertSQL);

            return persist(stmt, batchSize, batchIntervalInMillis, stmtSetter);
        } finally {
            try {
                DataSourceUtil.closeQuietly(stmt);
            } finally {
                DataSourceUtil.releaseConnection(conn, ds);
            }
        }
    }

    private static final Throwables.TriConsumer<Type<Object>, Object, BufferedJSONWriter, IOException> WRITE_CSV_ELEMENT_WITH_TYPE;
    private static final Throwables.BiConsumer<Object, BufferedJSONWriter, IOException> WRITE_CSV_ELEMENT;
    private static final Throwables.BiConsumer<String, BufferedJSONWriter, IOException> WRITE_CSV_STRING;

    static {
        final JSONParser jsonParser = ParserFactory.createJSONParser();
        final Type<Object> strType = N.typeOf(String.class);
        final JSONSerializationConfig config = JSC.create();
        config.setDateTimeFormat(DateTimeFormat.ISO_8601_TIMESTAMP);
        config.quoteMapKey(true);
        config.quotePropName(true);

        WRITE_CSV_ELEMENT_WITH_TYPE = (type, element, bw) -> {
            if (element == null) {
                bw.write(NULL_CHAR_ARRAY);
            } else {
                if (type.isSerializable()) {
                    type.writeCharacter(bw, element, config);
                } else {
                    strType.writeCharacter(bw, jsonParser.serialize(element, config), config);
                }
            }
        };

        WRITE_CSV_ELEMENT = (element, bw) -> {
            if (element == null) {
                bw.write(NULL_CHAR_ARRAY);
            } else {
                WRITE_CSV_ELEMENT_WITH_TYPE.accept(N.typeOf(element.getClass()), element, bw);
            }
        };

        WRITE_CSV_STRING = (str, bw) -> strType.writeCharacter(bw, str, config);
    }

    /**
     * The first row will be used as field names if its type is array or list,
     * or obtain the column names from first row if its type is bean or map.
     * <br />
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     *
     * @param output
     * @return
     * @throws E
     * @throws IOException
     */
    @Beta
    @TerminalOp
    public long persistToCSV(File output) throws E, IOException {
        final Writer writer = IOUtil.newFileWriter(output);

        try {
            return persistToCSV(writer);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     * @param csvHeaders
     * @param output
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persistToCSV(Collection<String> csvHeaders, File output) throws E, IOException {
        final Writer writer = IOUtil.newFileWriter(output);

        try {
            return persistToCSV(csvHeaders, writer);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     * The first row will be used as field names if its type is array or list,
     * or obtain the column names from first row if its type is bean or map.
     * <br />
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     *
     * @param output
     * @return
     * @throws E
     * @throws IOException
     */
    @Beta
    @TerminalOp
    public long persistToCSV(OutputStream output) throws E, IOException {
        final BufferedWriter bw = Objectory.createBufferedWriter(output);

        try {
            return persistToCSV(bw);
        } finally {
            IOUtil.close(bw);
        }
    }

    /**
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     * @param csvHeaders
     * @param output
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persistToCSV(Collection<String> csvHeaders, OutputStream output) throws E, IOException {
        final BufferedWriter bw = Objectory.createBufferedWriter(output);

        try {
            return persistToCSV(csvHeaders, bw);
        } finally {
            IOUtil.close(bw);
        }
    }

    /**
     * The first row will be used as field names if its type is array or list,
     * or obtain the column names from first row if its type is bean or map.
     * <br />
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     *
     * @param output
     * @return
     * @throws E
     * @throws IOException
     */
    @Beta
    @TerminalOp
    public long persistToCSV(Writer output) throws E, IOException {
        assertNotClosed();

        try {
            final boolean isBufferedWriter = output instanceof BufferedJSONWriter;
            final BufferedJSONWriter bw = isBufferedWriter ? (BufferedJSONWriter) output : Objectory.createBufferedJSONWriter(output);

            final Throwables.Iterator<T, E> iter = iteratorEx();
            long cnt = 0;
            T next = null;
            Class<?> cls = null;

            try {
                if (iter.hasNext()) {
                    next = iter.next();
                    cnt++;
                    cls = next.getClass();

                    if (ClassUtil.isBeanClass(cls)) {
                        final List<PropInfo> propInfoList = ParserUtil.getBeanInfo(cls).propInfoList;
                        final int headerSize = propInfoList.size();
                        PropInfo propInfo = null;

                        for (int i = 0; i < headerSize; i++) {
                            if (i > 0) {
                                bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_STRING.accept(propInfoList.get(i).name, bw);
                        }

                        bw.write(IOUtil.LINE_SEPARATOR);

                        for (int i = 0; i < headerSize; i++) {
                            propInfo = propInfoList.get(i);

                            if (i > 0) {
                                bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_ELEMENT_WITH_TYPE.accept(propInfo.jsonXmlType, propInfo.getPropValue(next), bw);
                        }

                        while (iter.hasNext()) {
                            next = iter.next();
                            cnt++;

                            bw.write(IOUtil.LINE_SEPARATOR);

                            for (int i = 0; i < headerSize; i++) {
                                propInfo = propInfoList.get(i);

                                if (i > 0) {
                                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                                }

                                WRITE_CSV_ELEMENT_WITH_TYPE.accept(propInfo.jsonXmlType, propInfo.getPropValue(next), bw);
                            }
                        }
                    } else if (next instanceof Map) {
                        Map<Object, Object> row = (Map<Object, Object>) next;
                        final List<Object> keys = new ArrayList<>(row.keySet());
                        final int headerSize = keys.size();

                        for (int i = 0; i < headerSize; i++) {
                            if (i > 0) {
                                bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_ELEMENT.accept(keys.get(i), bw);
                        }

                        bw.write(IOUtil.LINE_SEPARATOR);

                        for (int i = 0; i < headerSize; i++) {
                            if (i > 0) {
                                bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_ELEMENT.accept(row.get(keys.get(i)), bw);
                        }

                        while (iter.hasNext()) {
                            row = (Map<Object, Object>) iter.next();
                            cnt++;

                            bw.write(IOUtil.LINE_SEPARATOR);

                            for (int i = 0; i < headerSize; i++) {
                                if (i > 0) {
                                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                                }

                                WRITE_CSV_ELEMENT.accept(row.get(keys.get(i)), bw);
                            }
                        }
                    } else {
                        throw new RuntimeException(cls + " is no supported for CSV format. Only bean/Map are supported");
                    }
                }

                bw.flush();
            } finally {
                if (!isBufferedWriter) {
                    Objectory.recycle((BufferedWriter) bw);
                }
            }

            return cnt;
        } finally {
            close();
        }
    }

    /**
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     * @param csvHeaders
     * @param output
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persistToCSV(Collection<String> csvHeaders, Writer output) throws E, IOException {
        checkArgNotEmpty(csvHeaders, "csvHeaders");
        assertNotClosed();

        try {
            List<String> headers = new ArrayList<>(csvHeaders);
            final boolean isBufferedWriter = output instanceof BufferedJSONWriter;
            final BufferedJSONWriter bw = isBufferedWriter ? (BufferedJSONWriter) output : Objectory.createBufferedJSONWriter(output);

            final int headSize = headers.size();
            final Throwables.Iterator<T, E> iter = iteratorEx();
            long cnt = 0;
            T next = null;
            Class<?> cls = null;

            try {
                if (iter.hasNext()) {
                    next = iter.next();
                    cnt++;
                    cls = next.getClass();

                    if (ClassUtil.isBeanClass(cls)) {
                        final BeanInfo beanInfo = ParserUtil.getBeanInfo(cls);
                        final PropInfo[] propInfos = new PropInfo[headSize];
                        PropInfo propInfo = null;

                        for (int i = 0; i < headSize; i++) {
                            propInfos[i] = beanInfo.getPropInfo(headers.get(i));
                        }

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_STRING.accept(headers.get(i), bw);
                        }

                        bw.write(IOUtil.LINE_SEPARATOR);

                        for (int i = 0; i < headSize; i++) {
                            propInfo = propInfos[i];

                            if (i > 0) {
                                bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_ELEMENT_WITH_TYPE.accept(propInfo.jsonXmlType, propInfo.getPropValue(next), bw);
                        }

                        while (iter.hasNext()) {
                            next = iter.next();
                            cnt++;

                            bw.write(IOUtil.LINE_SEPARATOR);

                            for (int i = 0; i < headSize; i++) {
                                propInfo = propInfos[i];

                                if (i > 0) {
                                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                                }

                                WRITE_CSV_ELEMENT_WITH_TYPE.accept(propInfo.jsonXmlType, propInfo.getPropValue(next), bw);
                            }
                        }
                    } else if (next instanceof Map) {
                        Map<Object, Object> row = (Map<Object, Object>) next;

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_STRING.accept(headers.get(i), bw);
                        }

                        bw.write(IOUtil.LINE_SEPARATOR);

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_ELEMENT.accept(row.get(headers.get(i)), bw);
                        }

                        while (iter.hasNext()) {
                            row = (Map<Object, Object>) iter.next();
                            cnt++;

                            bw.write(IOUtil.LINE_SEPARATOR);

                            for (int i = 0; i < headSize; i++) {
                                if (i > 0) {
                                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                                }

                                WRITE_CSV_ELEMENT.accept(row.get(headers.get(i)), bw);
                            }
                        }
                    } else if (next instanceof Collection) {
                        Collection<Object> row = (Collection<Object>) next;

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_STRING.accept(headers.get(i), bw);
                        }

                        bw.write(IOUtil.LINE_SEPARATOR);

                        Iterator<Object> rowIter = row.iterator();

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_ELEMENT.accept(rowIter.next(), bw);
                        }

                        while (iter.hasNext()) {
                            row = (Collection<Object>) iter.next();
                            rowIter = row.iterator();
                            cnt++;

                            bw.write(IOUtil.LINE_SEPARATOR);

                            for (int i = 0; i < headSize; i++) {
                                if (i > 0) {
                                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                                }

                                WRITE_CSV_ELEMENT.accept(rowIter.next(), bw);
                            }
                        }
                    } else if (next instanceof Object[]) {
                        Object[] row = (Object[]) next;

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_STRING.accept(headers.get(i), bw);
                        }

                        bw.write(IOUtil.LINE_SEPARATOR);

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_ELEMENT.accept(row[i], bw);
                        }

                        while (iter.hasNext()) {
                            row = (Object[]) iter.next();
                            cnt++;

                            bw.write(IOUtil.LINE_SEPARATOR);

                            for (int i = 0; i < headSize; i++) {
                                if (i > 0) {
                                    bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                                }

                                WRITE_CSV_ELEMENT.accept(row[i], bw);
                            }
                        }
                    } else {
                        throw new RuntimeException(cls + " is no supported for CSV format. Only bean/Map are supported");
                    }
                }

                bw.flush();
            } finally {
                if (!isBufferedWriter) {
                    Objectory.recycle((BufferedWriter) bw);
                }
            }

            return cnt;
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @param output
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persistToJSON(final File output) throws E, IOException {
        final Writer writer = IOUtil.newFileWriter(output);

        try {
            return persistToJSON(writer);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     *
     *
     * @param output
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persistToJSON(final OutputStream output) throws E, IOException {
        final BufferedWriter bw = Objectory.createBufferedWriter(output);

        try {
            return persistToJSON(bw);
        } finally {
            IOUtil.close(bw);
        }
    }

    /**
     *
     *
     * @param output
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persistToJSON(final Writer output) throws E, IOException {
        assertNotClosed();

        try {
            final boolean isBufferedWriter = output instanceof BufferedJSONWriter;
            final BufferedJSONWriter bw = isBufferedWriter ? (BufferedJSONWriter) output : Objectory.createBufferedJSONWriter(output); // NOSONAR

            final Throwables.Iterator<T, E> iter = iteratorEx();
            long cnt = 0;

            try {
                bw.write("[");
                bw.write(IOUtil.LINE_SEPARATOR);

                while (iter.hasNext()) {
                    N.toJSON(iter.next(), bw);
                    cnt++;
                }

                bw.write(IOUtil.LINE_SEPARATOR);
                bw.write("]");
                bw.write(IOUtil.LINE_SEPARATOR);

                bw.flush();
            } finally {
                if (!isBufferedWriter) {
                    Objectory.recycle((BufferedWriter) bw);
                }
            }

            return cnt;
        } finally {
            close();
        }
    }

    /**
     *
     *
     * @throws E
     */
    @Beta
    @TerminalOp
    public void println() throws E {
        N.println(join(", ", "[", "]"));
    }

    /**
     *
     *
     * @return
     */
    @Beta
    @IntermediateOp
    public CheckedStream<T, Exception> cast() {
        assertNotClosed();

        return (CheckedStream<T, Exception>) this;
    }

    /**
     *
     *
     * @return
     */
    @IntermediateOp
    public Stream<T> unchecked() {
        assertNotClosed();

        if (N.isEmpty(this.closeHandlers)) {
            return Stream.of(newObjIteratorEx(elements));
        } else {
            return Stream.of(newObjIteratorEx(elements)).onClose(this::close);
        }
    }

    /**
     *
     *
     * @return
     */
    @IntermediateOp
    public java.util.stream.Stream<T> toJdkStream() {
        assertNotClosed();

        final Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(newObjIteratorEx(elements), Spliterator.ORDERED | Spliterator.IMMUTABLE);

        if (isEmptyCloseHandlers(closeHandlers)) {
            return StreamSupport.stream(spliterator, false);
        } else {
            return StreamSupport.stream(spliterator, false).onClose(this::close);
        }
    }

    /**
     * To avoid eager loading by terminal operations invoked in {@code transfer}, we can call {@code stream.transform(s -> CheckedStream.defer(() -> s.someTerminalOperation(...)))}
     *
     * @param <TT>
     * @param <EE>
     * @param transfer
     * @return
     */
    @Beta
    @IntermediateOp
    public <TT, EE extends Exception> CheckedStream<TT, EE> transform(Function<? super CheckedStream<T, E>, CheckedStream<TT, EE>> transfer) { //NOSONAR
        assertNotClosed();
        checkArgNotNull(transfer, "transfer");

        //    final Throwables.Supplier<CheckedStream<TT, EE>, EE> delayInitializer = () -> transfer.apply(this);
        //
        //    return CheckedStream.defer(delayInitializer);

        return transfer.apply(this);
    }

    /**
     *
     * @param <U>
     * @param transfer
     * @return
     */
    @Beta
    @IntermediateOp
    public <U> CheckedStream<U, E> transformB(final Function<? super Stream<T>, ? extends Stream<? extends U>> transfer) {
        // Major reason for commenting out below lines is to keep consistent with method transform.
        //        assertNotClosed();
        //        checkArgNotNull(transfer, "transfer");
        //
        //        final Throwables.Supplier<CheckedStream<U, E>, E> delayInitializer = () -> CheckedStream.from(transfer.apply(this.unchecked()));
        //
        //        return CheckedStream.defer(delayInitializer);

        return transformB(transfer, false);
    }

    /**
     *
     * @param <U>
     * @param transfer
     * @param deferred
     * @return
     */
    @Beta
    @IntermediateOp
    public <U> CheckedStream<U, E> transformB(final Function<? super Stream<T>, ? extends Stream<? extends U>> transfer, final boolean deferred) {
        assertNotClosed();
        checkArgNotNull(transfer, "transfer");

        if (deferred) {
            final Throwables.Supplier<CheckedStream<U, E>, E> delayInitializer = () -> CheckedStream.from(transfer.apply(this.unchecked()));
            return CheckedStream.defer(delayInitializer);
        } else {
            return CheckedStream.from(transfer.apply(this.unchecked()));
        }
    }

    /**
     *
     * @param <TT>
     * @param <EE>
     * @param transfer
     * @return
     * @deprecated replaced by {@link #transform(Function)}
     */
    @Beta
    @IntermediateOp
    @Deprecated
    public <TT, EE extends Exception> CheckedStream<TT, EE> __(Function<? super CheckedStream<T, E>, CheckedStream<TT, EE>> transfer) { //NOSONAR
        return transform(transfer);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code ops} and then switch back to sequence stream.
     * <br />
     *
     *
     * @param <R>
     * @param ops
     * @return
     * @see Stream#sps(Function)
     * @see ExceptionUtil#toRuntimeException(Throwable)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
     * @see ExceptionUtil#hasCause(Throwable, Class)
     * @see ExceptionUtil#hasCause(Throwable, Predicate)
     */
    @Beta
    @IntermediateOp
    public <R> CheckedStream<R, E> sps(final Function<? super Stream<T>, ? extends Stream<? extends R>> ops) {
        assertNotClosed();

        return newStream(((Stream<R>) ops.apply(this.unchecked().parallel())), true);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code ops} and then switch back to sequence stream.
     * <br />
     *
     *
     * @param <R>
     * @param maxThreadNum
     * @param ops
     * @return
     * @see Stream#sps(int, Function)
     * @see ExceptionUtil#toRuntimeException(Throwable)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
     * @see ExceptionUtil#hasCause(Throwable, Class)
     * @see ExceptionUtil#hasCause(Throwable, Predicate)
     */
    @Beta
    @IntermediateOp
    public <R> CheckedStream<R, E> sps(final int maxThreadNum, final Function<? super Stream<T>, ? extends Stream<? extends R>> ops) {
        assertNotClosed();

        return newStream(((Stream<R>) ops.apply(this.unchecked().parallel(maxThreadNum))), true);
    }

    //    /**
    //     *
    //     * @param <R>
    //     * @param maxThreadNum
    //     * @param withVirtualThread
    //     * @param ops
    //     * @return
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, E> sps(final int maxThreadNum, final boolean withVirtualThread,
    //            final Function<? super Stream<T>, ? extends Stream<? extends R>> ops) {
    //        assertNotClosed();
    //
    //        return checked(((Stream<R>) ops.apply(this.unchecked().parallel(maxThreadNum, withVirtualThread))), true);
    //    }
    //
    //    /**
    //     *
    //     * @param <R>
    //     * @param maxThreadNum
    //     * @param executorNumForVirtualThread
    //     * @param ops
    //     * @return
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, E> sps(final int maxThreadNum, final int executorNumForVirtualThread,
    //            final Function<? super Stream<T>, ? extends Stream<? extends R>> ops) {
    //        assertNotClosed();
    //
    //        return checked(((Stream<R>) ops.apply(this.unchecked().parallel(maxThreadNum, executorNumForVirtualThread))), true);
    //    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code filter} and then switch back to sequence stream.
     * <br />
     *
     *
     * @param predicate
     * @return
     * @see Stream#spsFilter(Predicate)
     * @see ExceptionUtil#toRuntimeException(Throwable)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
     * @see ExceptionUtil#hasCause(Throwable, Class)
     * @see ExceptionUtil#hasCause(Throwable, Predicate)
     */
    @Beta
    @IntermediateOp
    public CheckedStream<T, E> spsFilter(final Throwables.Predicate<? super T, E> predicate) {
        final Function<Stream<T>, Stream<T>> ops = s -> s.filter(Fn.pp(predicate));

        return sps(ops);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code map} and then switch back to sequence stream.
     * <br />
     *
     *
     * @param <R>
     * @param mapper
     * @return
     * @see Stream#spsMap(Function)
     * @see ExceptionUtil#toRuntimeException(Throwable)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
     * @see ExceptionUtil#hasCause(Throwable, Class)
     * @see ExceptionUtil#hasCause(Throwable, Predicate)
     */
    @Beta
    @IntermediateOp
    public <R> CheckedStream<R, E> spsMap(final Throwables.Function<? super T, ? extends R, E> mapper) {
        final Function<Stream<T>, Stream<R>> ops = s -> s.map(Fn.ff(mapper));

        return sps(ops);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
     * <br />
     *
     *
     * @param <R>
     * @param mapper
     * @return
     * @see Stream#spsFlatMap(Function)
     * @see ExceptionUtil#toRuntimeException(Throwable)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
     * @see ExceptionUtil#hasCause(Throwable, Class)
     * @see ExceptionUtil#hasCause(Throwable, Predicate)
     */
    @Beta
    @IntermediateOp
    public <R> CheckedStream<R, E> spsFlatMap(final Throwables.Function<? super T, ? extends Stream<? extends R>, E> mapper) {
        final Function<Stream<T>, Stream<R>> ops = s -> s.flatMap(Fn.ff(mapper));

        return sps(ops);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
     * <br />
     *
     *
     * @param <R>
     * @param mapper
     * @return
     * @see Stream#spsFlatmap(Function)
     * @see ExceptionUtil#toRuntimeException(Throwable)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
     * @see ExceptionUtil#hasCause(Throwable, Class)
     * @see ExceptionUtil#hasCause(Throwable, Predicate)
     */
    @Beta
    @IntermediateOp
    public <R> CheckedStream<R, E> spsFlatmap(final Throwables.Function<? super T, ? extends Collection<? extends R>, E> mapper) { //NOSONAR
        final Function<Stream<T>, Stream<R>> ops = s -> s.flatmap(Fn.ff(mapper));

        return sps(ops);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code onEach} and then switch back to sequence stream.
     * <br />
     *
     *
     * @param action
     * @return
     * @see Stream#onEach(Consumer)
     * @see ExceptionUtil#toRuntimeException(Throwable)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
     * @see ExceptionUtil#hasCause(Throwable, Class)
     * @see ExceptionUtil#hasCause(Throwable, Predicate)
     */
    @Beta
    @IntermediateOp
    public CheckedStream<T, E> spsOnEach(final Throwables.Consumer<? super T, E> action) {
        final Function<Stream<T>, Stream<T>> ops = s -> s.onEach(Fn.cc(action));

        return sps(ops);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code filter} and then switch back to sequence stream.
     * <br />
     *
     * @param maxThreadNum
     * @param predicate
     * @return
     * @see Stream#spsFilter(int, Predicate)
     * @see ExceptionUtil#toRuntimeException(Throwable)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
     * @see ExceptionUtil#hasCause(Throwable, Class)
     * @see ExceptionUtil#hasCause(Throwable, Predicate)
     */
    @Beta
    @IntermediateOp
    public CheckedStream<T, E> spsFilter(final int maxThreadNum, final Throwables.Predicate<? super T, E> predicate) {
        final Function<Stream<T>, Stream<T>> ops = s -> s.filter(Fn.pp(predicate));

        return sps(maxThreadNum, ops);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code map} and then switch back to sequence stream.
     * <br />
     *
     * @param <R>
     * @param maxThreadNum
     * @param mapper
     * @return
     * @see Stream#spsMap(int, Function)
     * @see ExceptionUtil#toRuntimeException(Throwable)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
     * @see ExceptionUtil#hasCause(Throwable, Class)
     * @see ExceptionUtil#hasCause(Throwable, Predicate)
     */
    @Beta
    @IntermediateOp
    public <R> CheckedStream<R, E> spsMap(final int maxThreadNum, final Throwables.Function<? super T, ? extends R, E> mapper) {
        final Function<Stream<T>, Stream<R>> ops = s -> s.map(Fn.ff(mapper));

        return sps(maxThreadNum, ops);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
     * <br />
     *
     * @param <R>
     * @param maxThreadNum
     * @param mapper
     * @return
     * @see Stream#spsFlatMap(int, Function)
     * @see ExceptionUtil#toRuntimeException(Throwable)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
     * @see ExceptionUtil#hasCause(Throwable, Class)
     * @see ExceptionUtil#hasCause(Throwable, Predicate)
     */
    @Beta
    @IntermediateOp
    public <R> CheckedStream<R, E> spsFlatMap(final int maxThreadNum, final Throwables.Function<? super T, ? extends Stream<? extends R>, E> mapper) { //NOSONAR
        final Function<Stream<T>, Stream<R>> ops = s -> s.flatMap(Fn.ff(mapper));

        return sps(maxThreadNum, ops);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
     * <br />
     *
     * @param <R>
     * @param maxThreadNum
     * @param mapper
     * @return
     * @see Stream#spsFlatmap(int, Function)
     * @see ExceptionUtil#toRuntimeException(Throwable)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
     * @see ExceptionUtil#hasCause(Throwable, Class)
     * @see ExceptionUtil#hasCause(Throwable, Predicate)
     */
    @Beta
    @IntermediateOp
    public <R> CheckedStream<R, E> spsFlatmap(final int maxThreadNum, final Throwables.Function<? super T, ? extends Collection<? extends R>, E> mapper) { //NOSONAR
        final Function<Stream<T>, Stream<R>> ops = s -> s.flatmap(Fn.ff(mapper));

        return sps(maxThreadNum, ops);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code onEach} and then switch back to sequence stream.
     * <br />
     *
     * @param maxThreadNum
     * @param action
     * @return
     * @see Stream#onEach(int, Consumer)
     * @see ExceptionUtil#toRuntimeException(Throwable)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
     * @see ExceptionUtil#hasCause(Throwable, Class)
     * @see ExceptionUtil#hasCause(Throwable, Predicate)
     */
    @Beta
    @IntermediateOp
    public CheckedStream<T, E> spsOnEach(final int maxThreadNum, final Throwables.Consumer<? super T, E> action) {
        final Function<Stream<T>, Stream<T>> ops = s -> s.onEach(Fn.cc(action));

        return sps(maxThreadNum, ops);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code filter} and then switch back to sequence stream.
     * <br />
     *
     *
     * @param predicate
     * @return
     * @see Stream#spsFilter(Predicate)
     * @see ExceptionUtil#toRuntimeException(Throwable)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
     * @see ExceptionUtil#hasCause(Throwable, Class)
     * @see ExceptionUtil#hasCause(Throwable, Predicate)
     */
    @Beta
    @IntermediateOp
    public CheckedStream<T, Exception> spsFilterE(final Throwables.Predicate<? super T, ? extends Exception> predicate) {
        return newStream(unchecked().spsFilterE(predicate), true);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code map} and then switch back to sequence stream.
     * <br />
     *
     * @param <U>
     * @param mapper
     * @return
     * @see Stream#spsMap(Function)
     * @see ExceptionUtil#toRuntimeException(Throwable)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
     * @see ExceptionUtil#hasCause(Throwable, Class)
     * @see ExceptionUtil#hasCause(Throwable, Predicate)
     */
    @Beta
    @IntermediateOp
    public <U> CheckedStream<U, Exception> spsMapE(final Throwables.Function<? super T, ? extends U, ? extends Exception> mapper) {
        return newStream(unchecked().<U> spsMapE(mapper), true);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
     * <br />
     *
     *
     * @param <R>
     * @param mapper
     * @return
     * @see Stream#spsFlatMap(Function)
     * @see ExceptionUtil#toRuntimeException(Throwable)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
     * @see ExceptionUtil#hasCause(Throwable, Class)
     * @see ExceptionUtil#hasCause(Throwable, Predicate)
     */
    @Beta
    @IntermediateOp
    public <R> CheckedStream<R, Exception> spsFlatMapE(final Throwables.Function<? super T, ? extends Stream<? extends R>, ? extends Exception> mapper) {
        return newStream(unchecked().<R> spsFlatMapE(mapper), true);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
     * <br />
     *
     *
     * @param <R>
     * @param mapper
     * @return
     * @see Stream#spsFlatmap(Function)
     * @see ExceptionUtil#toRuntimeException(Throwable)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
     * @see ExceptionUtil#hasCause(Throwable, Class)
     * @see ExceptionUtil#hasCause(Throwable, Predicate)
     */
    @Beta
    @IntermediateOp
    public <R> CheckedStream<R, Exception> spsFlatmapE( //NOSONAR
            final Throwables.Function<? super T, ? extends Collection<? extends R>, ? extends Exception> mapper) {
        return newStream(unchecked().<R> spsFlatmapE(mapper), true);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code onEach} and then switch back to sequence stream.
     * <br />
     *
     *
     * @param action
     * @return
     * @see Stream#onEach(Consumer)
     * @see ExceptionUtil#toRuntimeException(Throwable)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
     * @see ExceptionUtil#hasCause(Throwable, Class)
     * @see ExceptionUtil#hasCause(Throwable, Predicate)
     */
    @Beta
    @IntermediateOp
    public CheckedStream<T, Exception> spsOnEachE(final Throwables.Consumer<? super T, ? extends Exception> action) {
        return newStream(unchecked().spsOnEachE(action), true);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code filter} and then switch back to sequence stream.
     * <br />
     *
     *
     * @param maxThreadNum
     * @param predicate
     * @return
     * @see Stream#spsFilter(Predicate)
     * @see ExceptionUtil#toRuntimeException(Throwable)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
     * @see ExceptionUtil#hasCause(Throwable, Class)
     * @see ExceptionUtil#hasCause(Throwable, Predicate)
     */
    @Beta
    @IntermediateOp
    public CheckedStream<T, Exception> spsFilterE(final int maxThreadNum, final Throwables.Predicate<? super T, ? extends Exception> predicate) {
        return newStream(unchecked().spsFilterE(maxThreadNum, predicate), true);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code map} and then switch back to sequence stream.
     * <br />
     *
     * @param <U>
     * @param maxThreadNum
     * @param mapper
     * @return
     * @see Stream#spsMap(Function)
     * @see ExceptionUtil#toRuntimeException(Throwable)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
     * @see ExceptionUtil#hasCause(Throwable, Class)
     * @see ExceptionUtil#hasCause(Throwable, Predicate)
     */
    @Beta
    @IntermediateOp
    public <U> CheckedStream<U, Exception> spsMapE(final int maxThreadNum, final Throwables.Function<? super T, ? extends U, ? extends Exception> mapper) {
        return newStream(unchecked().<U> spsMapE(maxThreadNum, mapper), true);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
     * <br />
     *
     *
     * @param <R>
     * @param maxThreadNum
     * @param mapper
     * @return
     * @see Stream#spsFlatMap(Function)
     * @see ExceptionUtil#toRuntimeException(Throwable)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
     * @see ExceptionUtil#hasCause(Throwable, Class)
     * @see ExceptionUtil#hasCause(Throwable, Predicate)
     */
    @Beta
    @IntermediateOp
    public <R> CheckedStream<R, Exception> spsFlatMapE(final int maxThreadNum, //NOSONAR
            final Throwables.Function<? super T, ? extends Stream<? extends R>, ? extends Exception> mapper) {
        return newStream(unchecked().<R> spsFlatMapE(maxThreadNum, mapper), true);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
     * <br />
     *
     *
     * @param <R>
     * @param maxThreadNum
     * @param mapper
     * @return
     * @see Stream#spsFlatmap(Function)
     * @see ExceptionUtil#toRuntimeException(Throwable)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
     * @see ExceptionUtil#hasCause(Throwable, Class)
     * @see ExceptionUtil#hasCause(Throwable, Predicate)
     */
    @Beta
    @IntermediateOp
    public <R> CheckedStream<R, Exception> spsFlatmapE(final int maxThreadNum, //NOSONAR
            final Throwables.Function<? super T, ? extends Collection<? extends R>, ? extends Exception> mapper) {
        return newStream(unchecked().<R> spsFlatmapE(maxThreadNum, mapper), true);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code onEach} and then switch back to sequence stream.
     * <br />
     *
     *
     * @param maxThreadNum
     * @param action
     * @return
     * @see Stream#onEach(Consumer)
     * @see ExceptionUtil#toRuntimeException(Throwable)
     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
     * @see ExceptionUtil#hasCause(Throwable, Class)
     * @see ExceptionUtil#hasCause(Throwable, Predicate)
     */
    @Beta
    @IntermediateOp
    public CheckedStream<T, Exception> spsOnEachE(final int maxThreadNum, final Throwables.Consumer<? super T, ? extends Exception> action) {
        return newStream(unchecked().spsOnEachE(maxThreadNum, action), true);
    }

    /**
     *
     * @param predicate
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Beta
    @IntermediateOp
    public CheckedStream<T, Exception> filterE(final Throwables.Predicate<? super T, ? extends Exception> predicate) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<T, Exception>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() throws Exception {
                if (!hasNext) {
                    while (elements.hasNext()) {
                        next = elements.next();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
                        }
                    }
                }

                return hasNext;
            }

            @Override
            public T next() throws Exception {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }
        }, sorted, cmp, (Deque) closeHandlers);
    }

    /**
     *
     * @param <U>
     * @param mapper
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Beta
    @IntermediateOp
    public <U> CheckedStream<U, Exception> mapE(final Throwables.Function<? super T, ? extends U, ? extends Exception> mapper) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<U, Exception>() {
            @Override
            public boolean hasNext() throws Exception {
                return elements.hasNext();
            }

            @Override
            public U next() throws Exception {
                return mapper.apply(elements.next());
            }
        }, (Deque) closeHandlers);
    }

    /**
     *
     * @param <R>
     * @param mapper
     * @return
     */
    @Beta
    @IntermediateOp
    public <R> CheckedStream<R, Exception> flatMapE(
            final Throwables.Function<? super T, ? extends CheckedStream<? extends R, ? extends Exception>, ? extends Exception> mapper) {
        assertNotClosed();

        final Throwables.Iterator<R, Exception> iter = new Throwables.Iterator<>() {
            private Throwables.Iterator<? extends R, ? extends Exception> cur = null;
            private CheckedStream<? extends R, ? extends Exception> s = null;
            private Deque<LocalRunnable> closeHandle = null;

            public boolean hasNext() throws Exception {
                while (cur == null || !cur.hasNext()) {
                    if (elements.hasNext()) {
                        if (closeHandle != null) {
                            final Deque<LocalRunnable> tmp = closeHandle;
                            closeHandle = null;
                            CheckedStream.close(tmp);
                        }

                        s = mapper.apply(elements.next());

                        if (s == null) {
                            cur = null;
                        } else {
                            if (N.notEmpty(s.closeHandlers)) {
                                closeHandle = s.closeHandlers;
                            }

                            cur = s.elements;
                        }
                    } else {
                        cur = null;
                        break;
                    }
                }

                return cur != null && cur.hasNext();
            }

            public R next() throws Exception {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }

            protected void closeResource() {
                if (closeHandle != null) {
                    CheckedStream.close(closeHandle);
                }
            }
        };

        final Deque<LocalRunnable> newCloseHandlers = new ArrayDeque<>(N.size(closeHandlers) + 1);

        if (N.notEmpty(closeHandlers)) {
            newCloseHandlers.addAll(closeHandlers);
        }

        newCloseHandlers.add(newCloseHandler(iter));

        return newStream(iter, newCloseHandlers);
    }

    /**
     *
     * @param <R>
     * @param mapper
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Beta
    @IntermediateOp
    public <R> CheckedStream<R, Exception> flatmapE(final Throwables.Function<? super T, ? extends Collection<? extends R>, ? extends Exception> mapper) { //NOSONAR
        assertNotClosed();

        return newStream(new Throwables.Iterator<R, Exception>() {
            private Iterator<? extends R> cur = null;
            private Collection<? extends R> c = null;

            @Override
            public boolean hasNext() throws Exception {
                while ((cur == null || !cur.hasNext()) && elements.hasNext()) {
                    c = mapper.apply(elements.next());
                    cur = N.isEmpty(c) ? null : c.iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public R next() throws Exception {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return cur.next();
            }
        }, (Deque) closeHandlers);
    }

    /**
     *
     * @param action
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Beta
    @IntermediateOp
    public CheckedStream<T, Exception> onEachE(final Throwables.Consumer<? super T, ? extends Exception> action) {
        assertNotClosed();

        return newStream(new Throwables.Iterator<T, Exception>() {
            @Override
            public boolean hasNext() throws Exception {
                return elements.hasNext();
            }

            @Override
            public T next() throws Exception {
                final T next = elements.next();
                action.accept(next);
                return next;
            }
        }, sorted, cmp, (Deque) closeHandlers);
    }

    // #######################################9X9#######################################
    // #######################################9X9#######################################

    /**
     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param b
     * @return
     */
    @IntermediateOp
    public <U> CheckedStream<Pair<T, U>, E> crossJoin(final Collection<? extends U> b) {
        return crossJoin(b, Fnn.<T, U, E> pair());
    }

    /**
     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <R>
     * @param b
     * @param func
     * @return
     */
    @IntermediateOp
    public <U, R> CheckedStream<R, E> crossJoin(final Collection<? extends U> b,
            final Throwables.BiFunction<? super T, ? super U, ? extends R, ? extends E> func) {
        assertNotClosed();

        return flatMap(t -> CheckedStream.<U, E> of(b).map(u -> func.apply(t, u)));
    }

    /**
     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <R>
     * @param b will be loaded to memory. If {@code b} is too big to load to memory, please use {@code b.cronJoin(this, ...)}
     * @param func
     * @return
     */
    @IntermediateOp
    public <U, R> CheckedStream<R, E> crossJoin(final CheckedStream<U, ? extends E> b,
            final Throwables.BiFunction<? super T, ? super U, ? extends R, ? extends E> func) {
        assertNotClosed();

        checkArgNotNull(b, "stream 'b' can not be null");

        return flatMap(new Throwables.Function<T, CheckedStream<R, E>, E>() {
            private Collection<? extends U> c = null;

            @Override
            public CheckedStream<R, E> apply(final T t) throws E {
                if (c == null) {
                    c = b.toList();
                }

                return CheckedStream.<U, E> of(c).map(u -> func.apply(t, u));
            }
        }).onClose(newCloseHandler(b));
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <K>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @return
     */
    @IntermediateOp
    public <U, K> CheckedStream<Pair<T, U>, E> innerJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper) {
        return innerJoin(b, leftKeyMapper, rightKeyMapper, Fnn.<T, U, E> pair());
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <K>
     * @param <R>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <U, K, R> CheckedStream<R, E> innerJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper,
            final Throwables.BiFunction<? super T, ? super U, ? extends R, ? extends E> func) {
        assertNotClosed();

        return flatMap(new Throwables.Function<T, CheckedStream<R, E>, E>() {
            private ListMultimap<K, U> rightKeyMap = null;

            @Override
            public CheckedStream<R, E> apply(final T t) throws E {
                if (rightKeyMap == null) {
                    rightKeyMap = createListMultimap(b, rightKeyMapper);
                }

                return CheckedStream.<U, E> of(rightKeyMap.get(leftKeyMapper.apply(t))).map(u -> func.apply(t, u));
            }
        });
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <K>
     * @param b
     * @param keyMapper
     * @return
     */
    public <K> CheckedStream<Pair<T, T>, E> innerJoin(final Collection<? extends T> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper) {
        return innerJoin(b, keyMapper, Fnn.<T, T, E> pair());
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <K>
     * @param <R>
     * @param b
     * @param keyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <K, R> CheckedStream<R, E> innerJoin(final Collection<? extends T> b, final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.BiFunction<? super T, ? super T, ? extends R, ? extends E> func) {
        return innerJoin(b, keyMapper, keyMapper, func);
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <K>
     * @param <R>
     * @param b will be loaded to memory. If {@code b} is too big to load to memory, please use {@code b.innerJoin(this, ...)}
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <U, K, R> CheckedStream<R, E> innerJoin(final CheckedStream<U, ? extends E> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper,
            final Throwables.BiFunction<? super T, ? super U, ? extends R, ? extends E> func) {
        assertNotClosed();

        checkArgNotNull(b, "stream 'b' can not be null");

        return flatMap(new Throwables.Function<T, CheckedStream<R, E>, E>() {
            private ListMultimap<K, U> rightKeyMap = null;

            @Override
            public CheckedStream<R, E> apply(final T t) throws E {
                if (rightKeyMap == null) {
                    rightKeyMap = (ListMultimap<K, U>) b.toMultimap(rightKeyMapper);
                }

                return CheckedStream.<U, E> of(rightKeyMap.get(leftKeyMapper.apply(t))).map(u -> func.apply(t, u));
            }
        }).onClose(newCloseHandler(b));
    }

    /**
     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param b
     * @param predicate
     * @return
     * @deprecated The time complexity is <i>O(n * m). You should try {@code innerJoin(Collection, Function, Function)} first.
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    @IntermediateOp
    public <U> CheckedStream<Pair<T, U>, E> innerJoin(final Collection<? extends U> b,
            final Throwables.BiPredicate<? super T, ? super U, ? extends E> predicate) {
        return innerJoin(b, predicate, (Throwables.BiFunction) Fn.pair());
    }

    /**
     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <R>
     * @param b
     * @param predicate
     * @param func
     * @return
     * @deprecated The time complexity is <i>O(n * m). You should try {@code innerJoin(Collection, Function, Function, BiFunction)} first.
     */
    @Deprecated
    @IntermediateOp
    public <U, R> CheckedStream<R, E> innerJoin(final Collection<? extends U> b, final Throwables.BiPredicate<? super T, ? super U, ? extends E> predicate,
            final Throwables.BiFunction<? super T, ? super U, ? extends R, ? extends E> func) {
        assertNotClosed();

        return flatMap(t -> CheckedStream.<U, E> of(b).filter(u -> predicate.test(t, u)).map(u -> func.apply(t, u)));
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <K>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @return
     */
    @IntermediateOp
    public <U, K> CheckedStream<Pair<T, U>, E> fullJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper) {
        return fullJoin(b, leftKeyMapper, rightKeyMapper, Fnn.<T, U, E> pair());
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <K>
     * @param <R>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <U, K, R> CheckedStream<R, E> fullJoin(final Collection<? extends U> b, final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper,
            final Throwables.BiFunction<? super T, ? super U, ? extends R, ? extends E> func) {
        assertNotClosed();

        final Map<U, U> joinedRights = new IdentityHashMap<>();

        return flatMap(new Throwables.Function<T, CheckedStream<R, E>, E>() {
            private ListMultimap<K, U> rightKeyMap = null;

            @Override
            public CheckedStream<R, E> apply(final T t) throws E {
                if (rightKeyMap == null) {
                    rightKeyMap = createListMultimap(b, rightKeyMapper);
                }

                final List<U> values = rightKeyMap.get(leftKeyMapper.apply(t));

                return N.isEmpty(values) ? CheckedStream.of(func.apply(t, (U) null)) : CheckedStream.<U, E> of(values).map(u -> {
                    joinedRights.put(u, u);

                    return func.apply(t, u);
                });
            }
        }).append(CheckedStream.<U, E> of(b).filter(u -> !joinedRights.containsKey(u)).map(u -> func.apply((T) null, u)));
    }

    @SuppressWarnings("rawtypes")
    private static final Throwables.Function HOLDER_VALUE_GETTER;

    static {
        final Throwables.Function<Holder<Object>, Object, Exception> func = Holder::value;
        HOLDER_VALUE_GETTER = func;
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <K>
     * @param b
     * @param keyMapper
     * @return
     */
    public <K> CheckedStream<Pair<T, T>, E> fullJoin(final Collection<? extends T> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper) {
        return fullJoin(b, keyMapper, Fnn.<T, T, E> pair());
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <K>
     * @param <R>
     * @param b
     * @param keyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <K, R> CheckedStream<R, E> fullJoin(final Collection<? extends T> b, final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.BiFunction<? super T, ? super T, ? extends R, ? extends E> func) {
        return fullJoin(b, keyMapper, keyMapper, func);
    }

    /**
     *
     *
     * @param <U>
     * @param <K>
     * @param <R>
     * @param b will be loaded to memory. If {@code b} is too big to load to memory, please use {@code b.fullJoin(this, ...)}
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <U, K, R> CheckedStream<R, E> fullJoin(final CheckedStream<U, ? extends E> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper,
            final Throwables.BiFunction<? super T, ? super U, ? extends R, ? extends E> func) {
        assertNotClosed();

        checkArgNotNull(b, "stream 'b' can not be null");

        final Map<U, U> joinedRights = new IdentityHashMap<>();
        final Holder<List<U>> holder = new Holder<>();

        return flatMap(new Throwables.Function<T, CheckedStream<R, E>, E>() {
            private List<U> c = null;
            private ListMultimap<K, U> rightKeyMap = null;

            @Override
            public CheckedStream<R, E> apply(final T t) throws E {
                if (rightKeyMap == null) {
                    c = b.toList();
                    rightKeyMap = createListMultimap(c, rightKeyMapper);
                    holder.setValue(c);
                }

                final List<U> values = rightKeyMap.get(leftKeyMapper.apply(t));

                return N.isEmpty(values) ? CheckedStream.of(func.apply(t, (U) null)) : CheckedStream.<U, E> of(values).map(u -> {
                    joinedRights.put(u, u);

                    return func.apply(t, u);
                });
            }
        }).append(CheckedStream.<Holder<List<U>>, E> of(holder)
                .flatmap((Throwables.Function<Holder<List<U>>, List<U>, E>) HOLDER_VALUE_GETTER)
                .filter(u -> !joinedRights.containsKey(u))
                .map(u -> func.apply((T) null, u))).onClose(newCloseHandler(b));
    }

    /**
     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param b
     * @param predicate
     * @return
     * @deprecated The time complexity is <i>O(n * m). You should try {@code fullJoin(Collection, Function, Function)} first.
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    @IntermediateOp
    public <U> CheckedStream<Pair<T, U>, E> fullJoin(final Collection<? extends U> b,
            final Throwables.BiPredicate<? super T, ? super U, ? extends E> predicate) {
        return fullJoin(b, predicate, (Throwables.BiFunction) Fn.pair());
    }

    /**
     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <R>
     * @param b
     * @param predicate
     * @param func
     * @return
     * @deprecated The time complexity is <i>O(n * m). You should try {@code fullJoin(Collection, Function, Function, BiFunction)} first.
     */
    @Deprecated
    @IntermediateOp
    public <U, R> CheckedStream<R, E> fullJoin(final Collection<? extends U> b, final Throwables.BiPredicate<? super T, ? super U, ? extends E> predicate,
            final Throwables.BiFunction<? super T, ? super U, ? extends R, ? extends E> func) {
        assertNotClosed();

        final Map<U, U> joinedRights = new IdentityHashMap<>();

        return flatMap(t -> CheckedStream.<U, E> of(b).filter(u -> predicate.test(t, u)).map(u -> {
            joinedRights.put(u, u);

            return (R) func.apply(t, u);
        }).appendIfEmpty(() -> CheckedStream.<T, E> just(t).map(tt -> func.apply(t, (U) null))))
                .append(CheckedStream.<U, E> of(b).filter(u -> !joinedRights.containsKey(u)).map(u -> func.apply((T) null, u)));
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <K>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @return
     */
    @IntermediateOp
    public <U, K> CheckedStream<Pair<T, U>, E> leftJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper) {
        return leftJoin(b, leftKeyMapper, rightKeyMapper, Fnn.<T, U, E> pair());
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <K>
     * @param <R>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <U, K, R> CheckedStream<R, E> leftJoin(final Collection<? extends U> b, final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper,
            final Throwables.BiFunction<? super T, ? super U, ? extends R, ? extends E> func) {
        assertNotClosed();

        return flatMap(new Throwables.Function<T, CheckedStream<R, E>, E>() {
            private ListMultimap<K, U> rightKeyMap = null;

            @Override
            public CheckedStream<R, E> apply(final T t) throws E {
                if (rightKeyMap == null) {
                    rightKeyMap = createListMultimap(b, rightKeyMapper);
                }

                final List<U> values = rightKeyMap.get(leftKeyMapper.apply(t));

                return N.isEmpty(values) ? CheckedStream.<R, E> of(func.apply(t, (U) null)) : CheckedStream.<U, E> of(values).map(u -> func.apply(t, u));
            }
        });
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <K>
     * @param b
     * @param keyMapper
     * @return
     */
    public <K> CheckedStream<Pair<T, T>, E> leftJoin(final Collection<? extends T> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper) {
        return leftJoin(b, keyMapper, Fnn.<T, T, E> pair());
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <K>
     * @param <R>
     * @param b
     * @param keyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <K, R> CheckedStream<R, E> leftJoin(final Collection<? extends T> b, final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.BiFunction<? super T, ? super T, ? extends R, ? extends E> func) {
        return leftJoin(b, keyMapper, keyMapper, func);
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <K>
     * @param <R>
     * @param b will be loaded to memory. If {@code b} is too big to load to memory, please use {@code b.leftJoin(this, ...)}
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <U, K, R> CheckedStream<R, E> leftJoin(final CheckedStream<U, ? extends E> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper,
            final Throwables.BiFunction<? super T, ? super U, ? extends R, ? extends E> func) {
        assertNotClosed();

        checkArgNotNull(b, "stream 'b' can not be null");

        return flatMap(new Throwables.Function<T, CheckedStream<R, E>, E>() {
            private ListMultimap<K, U> rightKeyMap = null;

            @Override
            public CheckedStream<R, E> apply(final T t) throws E {
                if (rightKeyMap == null) {
                    rightKeyMap = (ListMultimap<K, U>) b.toMultimap(rightKeyMapper);
                }

                final List<U> values = rightKeyMap.get(leftKeyMapper.apply(t));

                return N.isEmpty(values) ? CheckedStream.<R, E> of(func.apply(t, (U) null)) : CheckedStream.<U, E> of(values).map(u -> func.apply(t, u));
            }
        }).onClose(newCloseHandler(b));
    }

    /**
     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param b
     * @param predicate
     * @return
     * @deprecated The time complexity is <i>O(n * m). You should try {@code leftJoin(Collection, Function, Function)} first.
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    @IntermediateOp
    public <U> CheckedStream<Pair<T, U>, E> leftJoin(final Collection<? extends U> b,
            final Throwables.BiPredicate<? super T, ? super U, ? extends E> predicate) {
        return leftJoin(b, predicate, (Throwables.BiFunction) Fn.pair());
    }

    /**
     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <R>
     * @param b
     * @param predicate
     * @param func
     * @return
     * @deprecated The time complexity is <i>O(n * m). You should try {@code leftJoin(Collection, Function, Function, BiFunction)} first.
     */
    @Deprecated
    @IntermediateOp
    public <U, R> CheckedStream<R, E> leftJoin(final Collection<? extends U> b, final Throwables.BiPredicate<? super T, ? super U, ? extends E> predicate,
            final Throwables.BiFunction<? super T, ? super U, ? extends R, ? extends E> func) {
        assertNotClosed();

        return flatMap(t -> CheckedStream.<U, E> of(b)
                .filter(u -> predicate.test(t, u))
                .map(u -> (R) func.apply(t, u))
                .appendIfEmpty(() -> CheckedStream.<T, E> just(t).map(tt -> func.apply(t, (U) null))));
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <K>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @return
     */
    @IntermediateOp
    public <U, K> CheckedStream<Pair<T, U>, E> rightJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper) {
        return rightJoin(b, leftKeyMapper, rightKeyMapper, Fnn.<T, U, E> pair());
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <K>
     * @param <R>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <U, K, R> CheckedStream<R, E> rightJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper,
            final Throwables.BiFunction<? super T, ? super U, ? extends R, ? extends E> func) {
        assertNotClosed();

        final Map<U, U> joinedRights = new IdentityHashMap<>();

        return flatMap(new Throwables.Function<T, CheckedStream<R, E>, E>() {
            private ListMultimap<K, U> rightKeyMap = null;

            @Override
            public CheckedStream<R, E> apply(final T t) throws E {
                if (rightKeyMap == null) {
                    rightKeyMap = createListMultimap(b, rightKeyMapper);
                }

                return CheckedStream.<U, E> of(rightKeyMap.get(leftKeyMapper.apply(t))).map(u -> {
                    joinedRights.put(u, u);

                    return func.apply(t, u);
                });
            }
        }).append(CheckedStream.<U, E> of(b).filter(u -> !joinedRights.containsKey(u)).map(u -> func.apply((T) null, u)));
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <K>
     * @param b
     * @param keyMapper
     * @return
     */
    public <K> CheckedStream<Pair<T, T>, E> rightJoin(final Collection<? extends T> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper) {
        return rightJoin(b, keyMapper, Fnn.<T, T, E> pair());
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <K>
     * @param <R>
     * @param b
     * @param keyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <K, R> CheckedStream<R, E> rightJoin(final Collection<? extends T> b, final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.BiFunction<? super T, ? super T, ? extends R, ? extends E> func) {
        return rightJoin(b, keyMapper, keyMapper, func);
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <K>
     * @param <R>
     * @param b will be loaded to memory. If {@code b} is too big to load to memory, please use {@code b.rightJoin(this, ...)}
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <U, K, R> CheckedStream<R, E> rightJoin(final CheckedStream<U, ? extends E> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper,
            final Throwables.BiFunction<? super T, ? super U, ? extends R, ? extends E> func) {
        assertNotClosed();

        checkArgNotNull(b, "stream 'b' can not be null");

        final Map<U, U> joinedRights = new IdentityHashMap<>();
        final Holder<List<U>> holder = new Holder<>();

        return flatMap(new Throwables.Function<T, CheckedStream<R, E>, E>() {
            private List<U> c = null;
            private ListMultimap<K, U> rightKeyMap = null;

            @Override
            public CheckedStream<R, E> apply(final T t) throws E {
                if (rightKeyMap == null) {
                    c = b.toList();
                    rightKeyMap = createListMultimap(c, rightKeyMapper);
                    holder.setValue(c);
                }

                return CheckedStream.<U, E> of(rightKeyMap.get(leftKeyMapper.apply(t))).map(u -> {
                    joinedRights.put(u, u);

                    return func.apply(t, u);
                });
            }
        }).append(CheckedStream.<Holder<List<U>>, E> of(holder)
                .flatmap((Throwables.Function<Holder<List<U>>, List<U>, E>) HOLDER_VALUE_GETTER)
                .filter(u -> !joinedRights.containsKey(u))
                .map(u -> func.apply((T) null, u))).onClose(newCloseHandler(b));
    }

    /**
     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param b
     * @param predicate
     * @return
     * @deprecated The time complexity is <i>O(n * m). You should try {@code rightJoin(Collection, Function, Function)} first.
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    @IntermediateOp
    public <U> CheckedStream<Pair<T, U>, E> rightJoin(final Collection<? extends U> b,
            final Throwables.BiPredicate<? super T, ? super U, ? extends E> predicate) {
        return rightJoin(b, predicate, (Throwables.BiFunction) Fn.pair());
    }

    /**
     * The time complexity is <i>O(n * m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <R>
     * @param b
     * @param predicate
     * @param func
     * @return
     * @deprecated The time complexity is <i>O(n * m). You should try {@code rightJoin(Collection, Function, Function, BiFunction)} first.
     */
    @Deprecated
    @IntermediateOp
    public <U, R> CheckedStream<R, E> rightJoin(final Collection<? extends U> b, final Throwables.BiPredicate<? super T, ? super U, ? extends E> predicate,
            final Throwables.BiFunction<? super T, ? super U, ? extends R, ? extends E> func) {
        assertNotClosed();

        final Map<U, U> joinedRights = new IdentityHashMap<>();

        return flatMap(t -> CheckedStream.<U, E> of(b).filter(u -> predicate.test(t, u)).map(u -> {
            joinedRights.put(u, u);

            return (R) func.apply(t, u);
        })).append(CheckedStream.<U, E> of(b).filter(u -> !joinedRights.containsKey(u)).map(u -> func.apply((T) null, u)));
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <K>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @return
     */
    @IntermediateOp
    public <U, K> CheckedStream<Pair<T, List<U>>, E> groupJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper) {
        return groupJoin(b, leftKeyMapper, rightKeyMapper, Fnn.<T, List<U>, E> pair());
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <K>
     * @param <R>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <U, K, R> CheckedStream<R, E> groupJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper,
            final Throwables.BiFunction<? super T, ? super List<U>, ? extends R, ? extends E> func) {
        assertNotClosed();

        final Throwables.Function<T, R, E> mapper = new Throwables.Function<>() {
            private volatile boolean initialized = false;
            private volatile Map<K, List<U>> map = null; //NOSONAR
            private List<U> val = null;

            public R apply(T t) throws E {
                if (!initialized) {
                    init();
                }

                val = map.get(leftKeyMapper.apply(t));

                if (val == null) {
                    return func.apply(t, new ArrayList<>(0));
                } else {
                    return func.apply(t, val);
                }
            }

            private void init() throws E {
                if (!initialized) {
                    initialized = true;

                    map = CheckedStream.<U, E> of(b).groupTo(rightKeyMapper);
                }
            }
        };

        return map(mapper);
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <K>
     * @param b
     * @param keyMapper
     * @return
     */
    public <K> CheckedStream<Pair<T, List<T>>, E> groupJoin(final Collection<? extends T> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper) {
        return groupJoin(b, keyMapper, Fnn.<T, List<T>, E> pair());
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <K>
     * @param <R>
     * @param b
     * @param keyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <K, R> CheckedStream<R, E> groupJoin(final Collection<? extends T> b, final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.BiFunction<? super T, ? super List<T>, ? extends R, ? extends E> func) {
        return groupJoin(b, keyMapper, keyMapper, func);
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <K>
     * @param <R>
     * @param b will be loaded to memory. If {@code b} is too big to load to memory, please use {@code b.groupJoin(this, ...)}
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <U, K, R> CheckedStream<R, E> groupJoin(final CheckedStream<U, ? extends E> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper,
            final Throwables.BiFunction<? super T, ? super List<U>, ? extends R, ? extends E> func) {
        assertNotClosed();

        checkArgNotNull(b, "stream 'b' can not be null");

        final Throwables.Function<T, R, E> mapper = new Throwables.Function<>() {
            private volatile boolean initialized = false;
            private volatile Map<K, List<U>> map = null; //NOSONAR
            private List<U> val = null;

            public R apply(T t) throws E {
                if (!initialized) {
                    init();
                }

                val = map.get(leftKeyMapper.apply(t));

                if (val == null) {
                    return func.apply(t, new ArrayList<>(0));
                } else {
                    return func.apply(t, val);
                }
            }

            @SuppressWarnings("rawtypes")
            private void init() throws E {
                if (!initialized) {
                    initialized = true;

                    map = (Map) b.groupTo(rightKeyMapper);
                }
            }
        };

        return map(mapper).onClose(newCloseHandler(b));
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <K>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @param mergeFunction
     * @return
     */
    @IntermediateOp
    public <U, K> CheckedStream<Pair<T, U>, E> groupJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper, final Throwables.BinaryOperator<U, ? extends E> mergeFunction) {
        return groupJoin(b, leftKeyMapper, rightKeyMapper, mergeFunction, Fnn.<T, U, E> pair());
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <K>
     * @param <R>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @param mergeFunction
     * @param func
     * @return
     */
    @IntermediateOp
    public <U, K, R> CheckedStream<R, E> groupJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper, final Throwables.BinaryOperator<U, ? extends E> mergeFunction,
            final Throwables.BiFunction<? super T, ? super U, ? extends R, ? extends E> func) {
        assertNotClosed();

        final Throwables.Function<T, R, E> mapper = new Throwables.Function<>() {
            private volatile boolean initialized = false;
            private volatile Map<K, U> map = null; //NOSONAR
            private U val = null;

            public R apply(T t) throws E {
                if (!initialized) {
                    init();
                }

                val = map.get(leftKeyMapper.apply(t));

                if (val == null) {
                    return func.apply(t, null);
                } else {
                    return func.apply(t, val);
                }
            }

            private void init() throws E {
                if (!initialized) {
                    initialized = true;

                    map = CheckedStream.<U, E> of(b).toMap(rightKeyMapper, Fnn.<U, E> identity(), mergeFunction);
                }
            }
        };

        return map(mapper);
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <K>
     * @param <R>
     * @param b will be loaded to memory. If {@code b} is too big to load to memory, please use {@code b.groupJoin(this, ...)}
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @param mergeFunction
     * @param func
     * @return
     */
    @IntermediateOp
    public <U, K, R> CheckedStream<R, E> groupJoin(final CheckedStream<U, ? extends E> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper, final Throwables.BinaryOperator<U, ? extends E> mergeFunction,
            final Throwables.BiFunction<? super T, ? super U, ? extends R, ? extends E> func) {
        assertNotClosed();

        checkArgNotNull(b, "stream 'b' can not be null");

        final Throwables.Function<T, R, E> mapper = new Throwables.Function<>() {
            private volatile boolean initialized = false;
            private volatile Map<K, U> map = null; //NOSONAR
            private U val = null;

            public R apply(T t) throws E {
                if (!initialized) {
                    init();
                }

                val = map.get(leftKeyMapper.apply(t));

                if (val == null) {
                    return func.apply(t, null);
                } else {
                    return func.apply(t, val);
                }
            }

            private void init() throws E {
                if (!initialized) {
                    initialized = true;

                    map = b.toMap(rightKeyMapper, Fnn.<U, E> identity(), mergeFunction);
                }
            }
        };

        return map(mapper).onClose(newCloseHandler(b));
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <K>
     * @param <D>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @param downstream
     * @return
     */
    @IntermediateOp
    public <U, K, D> CheckedStream<Pair<T, D>, E> groupJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper, final Collector<? super U, ?, D> downstream) {
        return groupJoin(b, leftKeyMapper, rightKeyMapper, downstream, Fnn.<T, D, E> pair());
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <K>
     * @param <D>
     * @param <R>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @param downstream
     * @param func
     * @return
     */
    @IntermediateOp
    public <U, K, D, R> CheckedStream<R, E> groupJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper, final Collector<? super U, ?, D> downstream,
            final Throwables.BiFunction<? super T, ? super D, ? extends R, ? extends E> func) {
        assertNotClosed();

        final Throwables.Function<T, R, E> mapper = new Throwables.Function<>() {
            private volatile boolean initialized = false;
            private volatile Map<K, D> map = null; //NOSONAR
            private D val = null;

            public R apply(T t) throws E {
                if (!initialized) {
                    init();
                }

                val = map.get(leftKeyMapper.apply(t));

                if (val == null) {
                    return func.apply(t, CheckedStream.<U, E> empty().collect(downstream));
                } else {
                    return func.apply(t, val);
                }
            }

            private void init() throws E {
                if (!initialized) {
                    initialized = true;

                    map = CheckedStream.<U, E> of(b).groupTo(rightKeyMapper, Fnn.<U, E> identity(), downstream);
                }
            }
        };

        return map(mapper);
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     *
     * @param <K>
     * @param <D>
     * @param b
     * @param keyMapper
     * @param downstream
     * @return
     */
    @IntermediateOp
    public <K, D> CheckedStream<Pair<T, D>, E> groupJoin(final Collection<? extends T> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper, final Collector<? super T, ?, D> downstream) {
        return groupJoin(b, keyMapper, keyMapper, downstream);
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     *
     * @param <K>
     * @param <D>
     * @param <R>
     * @param b
     * @param keyMapper
     * @param downstream
     * @param func
     * @return
     */
    @IntermediateOp
    public <K, D, R> CheckedStream<R, E> groupJoin(final Collection<? extends T> b, final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Collector<? super T, ?, D> downstream, final Throwables.BiFunction<? super T, ? super D, ? extends R, ? extends E> func) {
        return groupJoin(b, keyMapper, keyMapper, downstream, func);
    }

    /**
     * The time complexity is <i>O(n + m)</i> : <i>n</i> is the size of this <code>Stream</code> and <i>m</i> is the size of specified collection <code>b</code>.
     *
     * @param <U>
     * @param <K>
     * @param <D>
     * @param <R>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @param downstream
     * @param func
     * @return
     */
    @IntermediateOp
    public <U, K, D, R> CheckedStream<R, E> groupJoin(final CheckedStream<U, ? extends E> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper, final Collector<? super U, ?, D> downstream,
            final Throwables.BiFunction<? super T, ? super D, ? extends R, ? extends E> func) {
        assertNotClosed();

        checkArgNotNull(b, "stream 'b' can not be null");

        final Throwables.Function<T, R, E> mapper = new Throwables.Function<>() {
            private volatile boolean initialized = false;
            private volatile Map<K, D> map = null; //NOSONAR
            private D val = null;

            public R apply(T t) throws E {
                if (!initialized) {
                    init();
                }

                val = map.get(leftKeyMapper.apply(t));

                if (val == null) {
                    return func.apply(t, CheckedStream.<U, E> empty().collect(downstream));
                } else {
                    return func.apply(t, val);
                }
            }

            private void init() throws E {
                if (!initialized) {
                    initialized = true;

                    map = b.groupTo(rightKeyMapper, Fnn.<U, E> identity(), downstream);
                }
            }
        };

        return map(mapper).onClose(newCloseHandler(b));
    }

    /**
     *
     * @param <U>
     * @param b should be ordered
     * @param predicate
     * @return
     */
    @Beta
    @IntermediateOp
    public <U> CheckedStream<Pair<T, List<U>>, E> joinByRange(final Iterator<U> b, final Throwables.BiPredicate<? super T, ? super U, ? extends E> predicate) {
        assertNotClosed();

        checkArgNotNull(b, "Iterator 'b' can not be null");
        checkArgNotNull(predicate, "'predicate' can not be null");

        final Throwables.Function<T, Pair<T, List<U>>, E> mapper = new Throwables.Function<>() {
            private final Iterator<U> iter = b;
            private final U none = (U) NONE;
            private U next = none;

            public Pair<T, List<U>> apply(T t) throws E {
                final List<U> list = new ArrayList<>();

                if (next == none) {
                    if (iter.hasNext()) {
                        next = iter.next();
                    } else {
                        return Pair.of(t, list);
                    }
                }

                while (predicate.test(t, next)) {
                    list.add(next);

                    if (iter.hasNext()) {
                        next = iter.next();
                    } else {
                        next = none;
                        break;
                    }
                }

                return Pair.of(t, list);
            }
        };

        return map(mapper);
    }

    /**
     *
     * @param <U>
     * @param <R>
     * @param b should be ordered
     * @param predicate
     * @param collector
     * @return
     */
    @Beta
    @IntermediateOp
    public <U, R> CheckedStream<Pair<T, R>, E> joinByRange(final Iterator<U> b, final Throwables.BiPredicate<? super T, ? super U, ? extends E> predicate,
            final Collector<? super U, ?, R> collector) {
        return joinByRange(b, predicate, collector, Fnn.pair());
    }

    /**
     *
     * @param <U>
     * @param <D>
     * @param <R>
     * @param b should be ordered
     * @param predicate
     * @param collector
     * @param func
     * @return
     */
    @Beta
    @IntermediateOp
    public <U, D, R> CheckedStream<R, E> joinByRange(final Iterator<U> b, final Throwables.BiPredicate<? super T, ? super U, ? extends E> predicate,
            final Collector<? super U, ?, D> collector, final Throwables.BiFunction<? super T, ? super D, ? extends R, ? extends E> func) {
        assertNotClosed();

        checkArgNotNull(b, "Iterator 'b' can not be null");
        checkArgNotNull(predicate, "'predicate' can not be null");
        checkArgNotNull(collector, "'collector' can not be null");
        checkArgNotNull(func, "'func' can not be null");

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super U> accumulator = (BiConsumer<Object, ? super U>) collector.accumulator();
        final Function<Object, D> finisher = (Function<Object, D>) collector.finisher();

        final Throwables.Function<T, R, E> mapper = new Throwables.Function<>() {
            private final Iterator<U> iter = b;
            private final U none = (U) NONE;
            private U next = none;

            public R apply(T t) throws E {
                final Object container = supplier.get();

                if (next == none) {
                    if (iter.hasNext()) {
                        next = iter.next();
                    } else {
                        return func.apply(t, finisher.apply(container));
                    }
                }

                while (predicate.test(t, next)) {
                    accumulator.accept(container, next);

                    if (iter.hasNext()) {
                        next = iter.next();
                    } else {
                        next = none;
                        break;
                    }
                }

                return func.apply(t, finisher.apply(container));
            }
        };

        return map(mapper);
    }

    /**
     *
     * @param <U>
     * @param <D>
     * @param <R>
     * @param b should be ordered
     * @param predicate
     * @param collector
     * @param func
     * @param mapperForUnJoinedEelements
     *       In a lot of scenarios, there could be an previous element which is took out from the specified {@code Iterator b} but not joined, you may need to consider including that element in this {@code mapperForUnJoinedEelements}.
     *       <br />
     *       This input {@code Iterator} is the input {@code b}
     * @return
     */
    @Beta
    @IntermediateOp
    public <U, D, R> CheckedStream<R, E> joinByRange(final Iterator<U> b, final Throwables.BiPredicate<? super T, ? super U, ? extends E> predicate,
            final Collector<? super U, ?, D> collector, final Throwables.BiFunction<? super T, ? super D, ? extends R, ? extends E> func,
            final Throwables.Function<Iterator<U>, CheckedStream<R, E>, ? extends E> mapperForUnJoinedEelements) {
        assertNotClosed();

        checkArgNotNull(b, "Iterator 'b' can not be null");
        checkArgNotNull(predicate, "'predicate' can not be null");
        checkArgNotNull(collector, "'collector' can not be null");
        checkArgNotNull(func, "'func' can not be null");
        checkArgNotNull(mapperForUnJoinedEelements, "'mapperForUnJoinedEelements' can not be null");

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super U> accumulator = (BiConsumer<Object, ? super U>) collector.accumulator();
        final Function<Object, D> finisher = (Function<Object, D>) collector.finisher();

        final U none = (U) NONE;
        final Holder<U> nextValueHolder = Holder.of(none);

        final Throwables.Function<T, R, E> mapper = new Throwables.Function<>() {
            private final Iterator<U> iter = b;
            private U next = none;

            public R apply(T t) throws E {
                final Object container = supplier.get();

                if (next == none) {
                    if (iter.hasNext()) {
                        next = iter.next();
                    } else {
                        return func.apply(t, finisher.apply(container));
                    }
                }

                while (predicate.test(t, next)) {
                    accumulator.accept(container, next);

                    if (iter.hasNext()) {
                        next = iter.next();
                    } else {
                        next = none;
                        break;
                    }
                }

                nextValueHolder.setValue(next);

                return func.apply(t, finisher.apply(container));
            }
        };

        final Throwables.Supplier<CheckedStream<R, E>, E> tmp = () -> nextValueHolder.value() == none ? CheckedStream.<R, E> empty()
                : mapperForUnJoinedEelements.apply(Iterators.concat(ObjIterator.of(nextValueHolder.value()), b));

        return map(mapper).append(CheckedStream.defer(tmp));
    }

    /**
     *
     * @param <U>
     * @param b should be ordered. It will be closed along with this {@code Seq}
     * @param predicate
     * @return
     */
    @Beta
    @IntermediateOp
    public <U> CheckedStream<Pair<T, List<U>>, E> joinByRange(final Stream<U> b, final Throwables.BiPredicate<? super T, ? super U, ? extends E> predicate) {
        assertNotClosed();

        checkArgNotNull(b, "Stream 'b' can not be null");
        checkArgNotNull(predicate, "'predicate' can not be null");

        return joinByRange(b.iterator(), predicate).onClose(newCloseHandler(b));
    }

    /**
     *
     * @param <U>
     * @param <R>
     * @param b should be ordered. It will be closed along with this {@code Seq}
     * @param predicate
     * @param collector
     * @return
     */
    @Beta
    @IntermediateOp
    public <U, R> CheckedStream<Pair<T, R>, E> joinByRange(final Stream<U> b, final Throwables.BiPredicate<? super T, ? super U, ? extends E> predicate,
            final Collector<? super U, ?, R> collector) {
        return joinByRange(b, predicate, collector, Fnn.pair());
    }

    /**
     *
     * @param <U>
     * @param <D>
     * @param <R>
     * @param b should be ordered. It will be closed along with this {@code Seq}
     * @param predicate
     * @param collector
     * @param func
     * @return
     */
    @Beta
    @IntermediateOp
    public <U, D, R> CheckedStream<R, E> joinByRange(final Stream<U> b, final Throwables.BiPredicate<? super T, ? super U, ? extends E> predicate,
            final Collector<? super U, ?, D> collector, final Throwables.BiFunction<? super T, ? super D, ? extends R, ? extends E> func) {
        assertNotClosed();

        checkArgNotNull(b, "Stream 'b' can not be null");
        checkArgNotNull(predicate, "'predicate' can not be null");
        checkArgNotNull(collector, "'collector' can not be null");
        checkArgNotNull(func, "'func' can not be null");

        return ((CheckedStream<R, E>) joinByRange(b.iterator(), predicate, collector, func)).onClose(newCloseHandler(b));
    }

    /**
     *
     * @param <U>
     * @param <D>
     * @param <R>
     * @param b should be ordered. It will be closed along with this {@code Seq}
     * @param predicate
     * @param collector
     * @param func
     * @param mapperForUnJoinedEelements
     *       In a lot of scenarios, there could be an previous element which is took out from the specified {@code Iterator b} but not joined, you may need to consider including that element in this {@code mapperForUnJoinedEelements}.
     *       <br />
     *       This input {@code Iterator} comes from {@code b.iterator()}.
     * @return
     */
    @Beta
    @IntermediateOp
    public <U, D, R> CheckedStream<R, E> joinByRange(final Stream<U> b, final Throwables.BiPredicate<? super T, ? super U, ? extends E> predicate,
            final Collector<? super U, ?, D> collector, final Throwables.BiFunction<? super T, ? super D, ? extends R, ? extends E> func,
            final Throwables.Function<Iterator<U>, CheckedStream<R, E>, ? extends E> mapperForUnJoinedEelements) {
        assertNotClosed();

        checkArgNotNull(b, "Stream 'b' can not be null");
        checkArgNotNull(predicate, "'predicate' can not be null");
        checkArgNotNull(collector, "'collector' can not be null");
        checkArgNotNull(func, "'func' can not be null");
        checkArgNotNull(mapperForUnJoinedEelements, "'mapperForUnJoinedEelements' can not be null");

        return joinByRange(b.iterator(), predicate, collector, func, mapperForUnJoinedEelements).onClose(newCloseHandler(b));
    }

    /**
     *
     * @param <U>
     * @param b should be ordered. It will be closed along with this {@code Seq}
     * @param predicate
     * @return
     */
    @Beta
    @IntermediateOp
    public <U> CheckedStream<Pair<T, List<U>>, E> joinByRange(final CheckedStream<U, ? extends E> b,
            final Throwables.BiPredicate<? super T, ? super U, ? extends E> predicate) {
        assertNotClosed();

        checkArgNotNull(b, "CheckedStream 'b' can not be null");
        checkArgNotNull(predicate, "'predicate' can not be null");

        final Throwables.Function<T, Pair<T, List<U>>, E> mapper = new Throwables.Function<>() {
            private final Throwables.Iterator<U, ? extends E> iter = b.iteratorEx();
            private final U none = (U) NONE;
            private U next = none;

            public Pair<T, List<U>> apply(T t) throws E {
                final List<U> list = new ArrayList<>();

                if (next == none) {
                    if (iter.hasNext()) {
                        next = iter.next();
                    } else {
                        return Pair.of(t, list);
                    }
                }

                while (predicate.test(t, next)) {
                    list.add(next);

                    if (iter.hasNext()) {
                        next = iter.next();
                    } else {
                        next = none;
                        break;
                    }
                }

                return Pair.of(t, list);
            }
        };

        return map(mapper).onClose(newCloseHandler(b));
    }

    /**
     *
     * @param <U>
     * @param <R>
     * @param b should be ordered. It will be closed along with this {@code Seq}
     * @param predicate
     * @param collector
     * @return
     */
    @Beta
    @IntermediateOp
    public <U, R> CheckedStream<Pair<T, R>, E> joinByRange(final CheckedStream<U, ? extends E> b,
            final Throwables.BiPredicate<? super T, ? super U, ? extends E> predicate, final Collector<? super U, ?, R> collector) {
        return joinByRange(b, predicate, collector, Fnn.pair());
    }

    /**
     *
     * @param <U>
     * @param <D>
     * @param <R>
     * @param b should be ordered. It will be closed along with this {@code Seq}
     * @param predicate
     * @param collector
     * @param func
     * @return
     */
    @Beta
    @IntermediateOp
    public <U, D, R> CheckedStream<R, E> joinByRange(final CheckedStream<U, ? extends E> b,
            final Throwables.BiPredicate<? super T, ? super U, ? extends E> predicate, final Collector<? super U, ?, D> collector,
            final Throwables.BiFunction<? super T, ? super D, ? extends R, ? extends E> func) {
        assertNotClosed();

        checkArgNotNull(b, "CheckedStream 'b' can not be null");
        checkArgNotNull(predicate, "'predicate' can not be null");
        checkArgNotNull(collector, "'collector' can not be null");
        checkArgNotNull(func, "'func' can not be null");

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super U> accumulator = (BiConsumer<Object, ? super U>) collector.accumulator();
        final Function<Object, D> finisher = (Function<Object, D>) collector.finisher();

        final Throwables.Function<T, R, E> mapper = new Throwables.Function<>() {
            private final Throwables.Iterator<U, ? extends E> iter = b.iteratorEx();
            private final U none = (U) NONE;
            private U next = none;

            public R apply(T t) throws E {
                final Object container = supplier.get();

                if (next == none) {
                    if (iter.hasNext()) {
                        next = iter.next();
                    } else {
                        return func.apply(t, finisher.apply(container));
                    }
                }

                while (predicate.test(t, next)) {
                    accumulator.accept(container, next);

                    if (iter.hasNext()) {
                        next = iter.next();
                    } else {
                        next = none;
                        break;
                    }
                }

                return func.apply(t, finisher.apply(container));
            }
        };

        return map(mapper).onClose(newCloseHandler(b));
    }

    /**
     *
     * @param <U>
     * @param <D>
     * @param <R>
     * @param b should be ordered. It will be closed along with this {@code Seq}
     * @param predicate
     * @param collector
     * @param func
     * @param mapperForUnJoinedEelements
     *       In a lot of scenarios, there could be an previous element which is took out from the specified {@code Iterator b} but not joined, you may need to consider including that element in this {@code mapperForUnJoinedEelements}.
     *       <br />
     *       This input {@code Throwables.Iterator} comes from {@code b.iterator()}.
     * @return
     */
    @Beta
    @IntermediateOp
    public <U, D, R> CheckedStream<R, E> joinByRange(final CheckedStream<U, ? extends E> b,
            final Throwables.BiPredicate<? super T, ? super U, ? extends E> predicate, final Collector<? super U, ?, D> collector,
            final Throwables.BiFunction<? super T, ? super D, ? extends R, ? extends E> func,
            final Throwables.Function<Throwables.Iterator<U, E>, CheckedStream<R, E>, ? extends E> mapperForUnJoinedEelements) {
        assertNotClosed();

        checkArgNotNull(b, "CheckedStream 'b' can not be null");
        checkArgNotNull(predicate, "'predicate' can not be null");
        checkArgNotNull(collector, "'collector' can not be null");
        checkArgNotNull(func, "'func' can not be null");
        checkArgNotNull(mapperForUnJoinedEelements, "'mapperForUnJoinedEelements' can not be null");

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super U> accumulator = (BiConsumer<Object, ? super U>) collector.accumulator();
        final Function<Object, D> finisher = (Function<Object, D>) collector.finisher();
        final U none = (U) NONE;
        final Holder<U> nextValueHolder = Holder.of(none);
        final Throwables.Iterator<U, ? extends E> iter = b.iteratorEx();

        final Throwables.Function<T, R, E> mapper = new Throwables.Function<>() {
            private U next = none;

            public R apply(T t) throws E {
                final Object container = supplier.get();

                if (next == none) {
                    if (iter.hasNext()) {
                        next = iter.next();
                    } else {
                        return func.apply(t, finisher.apply(container));
                    }
                }

                while (predicate.test(t, next)) {
                    accumulator.accept(container, next);

                    if (iter.hasNext()) {
                        next = iter.next();
                    } else {
                        next = none;
                        break;
                    }
                }

                nextValueHolder.setValue(next);

                return func.apply(t, finisher.apply(container));
            }
        };

        final Throwables.Supplier<CheckedStream<R, E>, E> tmp = () -> nextValueHolder.value() == none ? CheckedStream.<R, E> empty()
                : mapperForUnJoinedEelements.apply(Throwables.Iterator.concat(Throwables.Iterator.of(nextValueHolder.value()), iter));

        return map(mapper).append(CheckedStream.defer(tmp)).onClose(newCloseHandler(b));
    }

    /**
     * If there is no value to join and want to skip that element, {@code joinFunc} can return {@code null} and then skip the {@code null} element by {@code stream.join(b, joinFunc).skipNulls()}.
     *
     *
     * @param <B>
     * @param <R>
     * @param b
     * @param joinFunc
     * @return
     */
    @Beta
    @IntermediateOp
    public <B extends Collection<?>, R> CheckedStream<R, E> join(final B b,
            final Throwables.BiFunction<? super T, ? super B, ? extends R, ? extends E> joinFunc) {
        assertNotClosed();

        checkArgNotNull(b, "Collection 'b' can not be null");
        checkArgNotNull(joinFunc, "'joinFunc' can not be null");

        final Throwables.Function<T, R, E> mapper = t -> joinFunc.apply(t, b);

        return map(mapper);
    }

    /**
     * If there is no value to join and want to skip that element, {@code joinFunc} can return {@code null} and then skip the {@code null} element by {@code stream.join(b, joinFunc).skipNulls()}.
     *
     * @param <B>
     * @param <R>
     * @param b
     * @param joinFunc
     * @return
     */
    @Beta
    @IntermediateOp
    public <B extends Map<?, ?>, R> CheckedStream<R, E> join(final B b, final Throwables.BiFunction<? super T, ? super B, ? extends R, ? extends E> joinFunc) {
        assertNotClosed();

        checkArgNotNull(b, "Map 'b' can not be null");
        checkArgNotNull(joinFunc, "'joinFunc' can not be null");

        final Throwables.Function<T, R, E> mapper = t -> joinFunc.apply(t, b);

        return map(mapper);
    }

    /**
     * If there is no value to join and want to skip that element, {@code joinFunc} can return {@code null} and then skip the {@code null} element by {@code stream.join(b, joinFunc).skipNulls()}.
     *
     *
     * @param <B>
     * @param <R>
     * @param b should be ordered
     * @param joinFunc
     * @return
     */
    @Beta
    @IntermediateOp
    public <B extends Iterator<?>, R> CheckedStream<R, E> join(final B b,
            final Throwables.BiFunction<? super T, ? super B, ? extends R, ? extends E> joinFunc) {
        assertNotClosed();

        checkArgNotNull(b, "Iterator 'b' can not be null");
        checkArgNotNull(joinFunc, "'joinFunc' can not be null");

        final Throwables.Function<T, R, E> mapper = t -> joinFunc.apply(t, b);

        return map(mapper);
    }

    /**
     * If there is no value to join and want to skip that element, {@code joinFunc} can return {@code null} and then skip the {@code null} element by {@code stream.join(b, joinFunc).skipNulls()}.
     *
     * @param <U>
     * @param <B>
     * @param <R>
     * @param b should be ordered
     * @param joinFunc
     * @param mapperForUnJoinedEelements In a lot of scenarios, there could be an previous element which is took out from the specified {@code Iterator b} but not joined, you may need to consider including that element in this {@code mapperForUnJoinedEelements}
     * @return
     */
    @Beta
    @IntermediateOp
    public <U, B extends Iterator<U>, R> CheckedStream<R, E> join(final B b,
            final Throwables.BiFunction<? super T, ? super B, ? extends R, ? extends E> joinFunc,
            final Throwables.Function<? super Iterator<U>, CheckedStream<R, E>, ? extends E> mapperForUnJoinedEelements) {
        assertNotClosed();

        checkArgNotNull(b, "Iterator 'b' can not be null");
        checkArgNotNull(joinFunc, "'joinFunc' can not be null");
        checkArgNotNull(mapperForUnJoinedEelements, "'mapperForUnJoinedEelements' can not be null");

        final Throwables.Function<T, R, E> mapper = t -> joinFunc.apply(t, b);

        Throwables.Supplier<? extends CheckedStream<R, E>, E> supplier = () -> mapperForUnJoinedEelements.apply(b);

        return map(mapper).append(CheckedStream.<R, E> defer(supplier));
    }

    /**
     * If there is no value to join and want to skip that element, {@code joinFunc} can return {@code null} and then skip the {@code null} element by {@code stream.join(b, joinFunc).skipNulls()}.
     *
     * @param <U>
     * @param <R>
     * @param b should be ordered. It will be closed along with this stream. It can also be closed earlier by {@code joinFunc}.
     * @param joinFunc
     * @return
     */
    @Beta
    @IntermediateOp
    public <U, R> CheckedStream<R, E> join(final Stream<U> b, final Throwables.BiFunction<? super T, ? super Iterator<U>, ? extends R, ? extends E> joinFunc) {
        assertNotClosed();

        checkArgNotNull(b, "Stream 'b' can not be null");
        checkArgNotNull(joinFunc, "'joinFunc' can not be null");

        return ((CheckedStream<R, E>) join(b.iterator(), joinFunc)).onClose(newCloseHandler(b));
    }

    /**
     * If there is no value to join and want to skip that element, {@code joinFunc} can return {@code null} and then skip the {@code null} element by {@code stream.join(b, joinFunc).skipNulls()}.
     *
     * @param <U>
     * @param <R>
     * @param b should be ordered. It will be closed along with this stream. It can also be closed earlier by {@code joinFunc}.
     * @param joinFunc
     * @param mapperForUnJoinedEelements In a lot of scenarios, there could be an previous element which is took out from the specified {@code Iterator b} but not joined, you may need to consider including that element in this {@code mapperForUnJoinedEelements}
     * @return
     */
    @Beta
    @IntermediateOp
    public <U, R> CheckedStream<R, E> join(final Stream<U> b, final Throwables.BiFunction<? super T, ? super Iterator<U>, ? extends R, ? extends E> joinFunc,
            final Throwables.Function<? super Iterator<U>, CheckedStream<R, E>, ? extends E> mapperForUnJoinedEelements) {
        assertNotClosed();

        checkArgNotNull(b, "Stream 'b' can not be null");
        checkArgNotNull(joinFunc, "'joinFunc' can not be null");
        checkArgNotNull(mapperForUnJoinedEelements, "'mapperForUnJoinedEelements' can not be null");

        return join(b.iterator(), joinFunc, mapperForUnJoinedEelements).onClose(newCloseHandler(b));
    }

    /**
     * If there is no value to join and want to skip that element, {@code joinFunc} can return {@code null} and then skip the {@code null} element by {@code stream.join(b, joinFunc).skipNulls()}.
     *
     *
     * @param <U>
     * @param <R>
     * @param b should be ordered. It will be closed along with this stream. It can also be closed earlier by {@code joinFunc}.
     * @param joinFunc
     * @return
     */
    @Beta
    @IntermediateOp
    public <U, R> CheckedStream<R, E> join(final CheckedStream<U, ? extends E> b,
            final Throwables.BiFunction<? super T, ? super Throwables.Iterator<U, ? extends E>, ? extends R, ? extends E> joinFunc) {
        assertNotClosed();

        checkArgNotNull(b, "CheckedStream 'b' can not be null");
        checkArgNotNull(joinFunc, "'joinFunc' can not be null");

        final Throwables.Iterator<U, ? extends E> iter = b.iteratorEx();

        final Throwables.Function<T, R, E> mapper = t -> joinFunc.apply(t, iter);

        return map(mapper).onClose(newCloseHandler(b));
    }

    /**
     * If there is no value to join and want to skip that element, {@code joinFunc} can return {@code null} and then skip the {@code null} element by {@code stream.join(b, joinFunc).skipNulls()}.
     *
     *
     * @param <U>
     * @param <R>
     * @param b should be ordered. It will be closed along with this stream. It can also be closed earlier by {@code joinFunc}.
     * @param joinFunc
     * @param mapperForUnJoinedEelements In a lot of scenarios, there could be an previous element which is took out from the specified {@code Iterator b} but not joined, you may need to consider including that element in this {@code mapperForUnJoinedEelements}
     * @return
     */
    @Beta
    @IntermediateOp
    public <U, R> CheckedStream<R, E> join(final CheckedStream<U, ? extends E> b,
            final Throwables.BiFunction<? super T, ? super Throwables.Iterator<U, ? extends E>, ? extends R, ? extends E> joinFunc,
            final Throwables.Function<? super Throwables.Iterator<U, ? extends E>, CheckedStream<R, E>, ? extends E> mapperForUnJoinedEelements) {
        assertNotClosed();

        checkArgNotNull(b, "CheckedStream 'b' can not be null");
        checkArgNotNull(joinFunc, "'joinFunc' can not be null");
        checkArgNotNull(mapperForUnJoinedEelements, "'mapperForUnJoinedEelements' can not be null");

        final Throwables.Iterator<U, ? extends E> iter = b.iteratorEx();

        final Throwables.Function<T, R, E> mapper = t -> joinFunc.apply(t, iter);

        final Throwables.Supplier<? extends CheckedStream<R, E>, E> supplier = () -> mapperForUnJoinedEelements.apply(iter);

        return map(mapper).append(CheckedStream.defer(supplier)).onClose(newCloseHandler(b));
    }

    static <T, K, E extends Exception> ListMultimap<K, T> createListMultimap(final Collection<? extends T> c,
            final Throwables.Function<? super T, ? extends K, E> keyMapper) throws E {
        N.checkArgNotNull(keyMapper);

        final ListMultimap<K, T> multimap = N.newListMultimap(N.size(c));

        if (N.notEmpty(c)) {
            for (T e : c) {
                multimap.put(keyMapper.apply(e), e);
            }
        }

        return multimap;
    }

    /**
     * Attach a new stream with terminal action to consume the elements from upstream.
     * The Intermediate and terminate operations in the attached stream will be executed in a new Thread.
     * The new thread is started when the main stream(the returned stream or its downstream) receives the first element or is closed if there is no element pulled.
     * Elements from upstream pulled by the main stream will be put in a queue for the attached stream to consume.
     * After the main stream is finished, the attached stream will continue to pull remaining elements from upstream if needed.
     * The main stream and the attached stream run independently. Operations in one stream won't impact the elements or final result in another Stream.
     * But when the main stream is to close, it will wait to the attached stream to close before calling close actions.
     *
     * @param consumerForNewStreamWithTerminalAction
     * @return
     * @see #addSubscriber(com.landawn.abacus.util.Throwables.Consumer, int, long, Executor)
     */
    @Beta
    @IntermediateOp
    public CheckedStream<T, E> addSubscriber(
            final Throwables.Consumer<? super CheckedStream<T, E>, ? extends Exception> consumerForNewStreamWithTerminalAction) {
        return addSubscriber(consumerForNewStreamWithTerminalAction, DEFAULT_BUFFERED_SIZE_PER_ITERATOR, MAX_WAIT_TIME_FOR_QUEUE_OFFER_FOR_ADD_SUBSCRIBER,
                Stream.executor());
    }

    /**
     * Attach a new stream with terminal action to consume the elements from upstream.
     * The Intermediate and terminate operations in the attached stream will be executed in a new Thread.
     * The new thread is started when the main stream(the returned stream or its downstream) receives the first element or is closed if there is no element pulled.
     * Elements from upstream pulled by the main stream will be put in a queue for the attached stream to consume.
     * After the main stream is finished, the attached stream will continue to pull remaining elements from upstream if needed.
     * The main stream and the attached stream run independently. Operations in one stream won't impact the elements or final result in another Stream.
     * But when the main stream is to close, it will wait to the attached stream to close before calling close actions.
     *
     * To get the return value of the attached stream, An output parameter can be used. for example:
     * <pre>
     * <code>
     *     final Holder<String> resultHolder = new Holder<>();
     *     thisStream.addSubscriber(newStream -> resultHolder.set(newStream.filter(...).map(...).join(",")))...;
     * </code>
     * </pre>
     *
     * @param consumerForNewStreamWithTerminalAction
     * @param queueSize
     * @param maxWaitForAddingElementToQuery default value is 30000 (unit is milliseconds)
     * @param executor
     * @return
     */
    @Beta
    @SequentialOnly
    @IntermediateOp
    public CheckedStream<T, E> addSubscriber(final Throwables.Consumer<? super CheckedStream<T, E>, ? extends Exception> consumerForNewStreamWithTerminalAction,
            final int queueSize, final long maxWaitForAddingElementToQuery, final Executor executor) {
        assertNotClosed();
        checkArgNotNull(consumerForNewStreamWithTerminalAction, "consumerForNewStreamWithTerminalAction");
        checkArgPositive(queueSize, "queueSize");
        checkArgPositive(maxWaitForAddingElementToQuery, "maxWaitForAddingElementToQuery");
        checkArgNotNull(executor, "executor");

        return addSubscriberForAll(consumerForNewStreamWithTerminalAction, queueSize, maxWaitForAddingElementToQuery, executor);
    }

    private CheckedStream<T, E> addSubscriberForAll(
            final Throwables.Consumer<? super CheckedStream<T, E>, ? extends Exception> consumerForNewStreamWithTerminalAction, final int queueSize,
            final long maxWaitForAddingElementToQuery, final Executor executor) {
        final BlockingQueue<T> queue = new ArrayBlockingQueue<>(queueSize <= 0 ? DEFAULT_BUFFERED_SIZE_PER_ITERATOR : queueSize);
        final Throwables.Iterator<T, E> iter = iteratorEx();
        final T none = (T) NONE;

        final MutableBoolean isMainStreamCompleted = MutableBoolean.of(false);
        final MutableBoolean isSubscriberStreamCompleted = MutableBoolean.of(false);
        final MutableBoolean wasQueueFull = MutableBoolean.of(false);
        final MutableInt nextCallCount = MutableInt.of(0); // it should end with 0 if there is no exception happening during hasNext()/next() call.

        final Throwables.Iterator<T, E> iterForSubscriberStream = new Throwables.Iterator<>() { //NOSONAR
            private T next = null;

            public boolean hasNext() throws E {
                if (next == null) {
                    if (isMainStreamCompleted.isFalse() || queue.size() > 0) {
                        try {
                            do {
                                next = queue.poll(MAX_WAIT_TIME_FOR_QUEUE_POLL, TimeUnit.MILLISECONDS);

                                if (nextCallCount.value() > 1 || (isMainStreamCompleted.isTrue() && nextCallCount.value() > 0)) {
                                    throw new IllegalStateException("Exception happened in calling hasNext()/next()");
                                }
                            } while (next == null && (isMainStreamCompleted.isFalse() || queue.size() > 0));
                        } catch (InterruptedException e) {
                            throw toRuntimeException(e);
                        }
                    }
                }

                if (wasQueueFull.isTrue()) {
                    throw new IllegalStateException("Queue(size=" + queueSize + ") was full at some moment. Elements may be missed");
                }

                if (nextCallCount.value() > 1 || (isMainStreamCompleted.isTrue() && nextCallCount.value() > 0)) {
                    throw new IllegalStateException("Exception happened in calling hasNext()/next()");
                }

                return next != null || (isMainStreamCompleted.isTrue() && elements.hasNext());
            }

            public T next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final T ret = next != null ? (next == none ? null : next) : iter.next();
                next = null;
                return ret;
            }

            @Override
            protected void closeResource() {
                isSubscriberStreamCompleted.setTrue();
                queue.clear();

                if (wasQueueFull.isTrue()) {
                    throw new IllegalStateException("Queue(size=" + queueSize + ") was full at some moment. Elements may be missed");
                }

                if (nextCallCount.value() > 1 || (isMainStreamCompleted.isTrue() && nextCallCount.value() > 0)) {
                    throw new IllegalStateException("Exception happened in calling hasNext()/next()");
                }
            }
        };

        final Throwables.Iterator<T, E> iterA = new Throwables.Iterator<>() { //NOSONAR
            private boolean isNewStreamStarted = false;
            private ContinuableFuture<Void> futureForNewStream = null;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() throws E {
                nextCallCount.increment();

                hasNext = elements.hasNext();

                nextCallCount.decrement();

                return hasNext;
            }

            public T next() throws E {
                nextCallCount.increment();

                final T next = elements.next();

                nextCallCount.decrement();

                if (!isNewStreamStarted) {
                    startNewStream();
                }

                if (isSubscriberStreamCompleted.isFalse()) {
                    try {
                        if (!queue.offer(next == null ? none : next, maxWaitForAddingElementToQuery, TimeUnit.MILLISECONDS)) {
                            wasQueueFull.setTrue();
                        }
                    } catch (InterruptedException e) {
                        throw toRuntimeException(e);
                    }
                }

                return next;
            }

            protected void closeResource() {
                isMainStreamCompleted.setTrue();

                if (!isNewStreamStarted) {
                    startNewStream();
                }

                if (futureForNewStream != null) {
                    try {
                        futureForNewStream.get();
                    } catch (ExecutionException | InterruptedException e) {
                        N.toRuntimeException(e);
                    }
                }
            }

            private void startNewStream() {
                isNewStreamStarted = true;

                if (executor == null) {
                    futureForNewStream = newStream(iterForSubscriberStream).onClose(iterForSubscriberStream::close)
                            .asyncRun(consumerForNewStreamWithTerminalAction); //NOSONAR
                } else {
                    futureForNewStream = newStream(iterForSubscriberStream).onClose(iterForSubscriberStream::close)
                            .asyncRun(consumerForNewStreamWithTerminalAction, executor); //NOSONAR
                }
            }
        };

        return newStream(iterA, sorted, cmp, mergeCloseHandlers(iterA::close, closeHandlers, true));
    }

    /**
     * Attach a new stream with terminal action to consume the elements filtered out by the specified {@code predicate}.
     * The Intermediate and terminate operations in the attached stream will be executed in a new Thread.
     * The new thread is started when the main stream(the returned stream or its downstream) receives the first element or is closed if there is no element pulled.
     * Elements from upstream pulled by the main stream will be put in a queue for the attached stream to consume.
     * After the main stream is finished, the attached stream will continue to pull remaining elements from upstream if needed.
     * The main stream and the attached stream run independently. Operations in one stream won't impact the elements or final result in another Stream.
     * But when the main stream is to close, it will wait to the attached stream to close before calling close actions.
     *
     * @param predicate
     * @param consumerForNewStreamWithTerminalAction
     * @return
     * @see #filterWhileAddSubscriber(com.landawn.abacus.util.Throwables.Consumer, int, long, Executor)
     */
    public CheckedStream<T, E> filterWhileAddSubscriber(final Throwables.Predicate<? super T, ? extends E> predicate,
            final Throwables.Consumer<? super CheckedStream<T, E>, ? extends Exception> consumerForNewStreamWithTerminalAction) {
        return filterWhileAddSubscriber(predicate, consumerForNewStreamWithTerminalAction, DEFAULT_BUFFERED_SIZE_PER_ITERATOR,
                MAX_WAIT_TIME_FOR_QUEUE_OFFER_FOR_ADD_SUBSCRIBER, Stream.executor());
    }

    /**
     * Attach a new stream with terminal action to consume the elements filtered out by the specified {@code predicate}.
     * The Intermediate and terminate operations in the attached stream will be executed in a new Thread.
     * The new thread is started when the main stream(the returned stream or its downstream) receives the first element or is closed if there is no element pulled.
     * Elements from upstream pulled by the main stream will be put in a queue for the attached stream to consume.
     * After the main stream is finished, the attached stream will continue to pull remaining elements from upstream if needed.
     * The main stream and the attached stream run independently. Operations in one stream won't impact the elements or final result in another Stream.
     * But when the main stream is to close, it will wait to the attached stream to close before calling close actions.
     *
     * @param predicate
     * @param consumerForNewStreamWithTerminalAction
     * @param queueSize
     * @param maxWaitForAddingElementToQuery default value is 30000 (unit is milliseconds)
     * @param executor
     * @return
     */
    public CheckedStream<T, E> filterWhileAddSubscriber(final Throwables.Predicate<? super T, ? extends E> predicate,
            final Throwables.Consumer<? super CheckedStream<T, E>, ? extends Exception> consumerForNewStreamWithTerminalAction, final int queueSize,
            final long maxWaitForAddingElementToQuery, final Executor executor) {
        assertNotClosed();
        checkArgNotNull(predicate, "predicate");
        checkArgNotNull(consumerForNewStreamWithTerminalAction, "consumerForNewStreamWithTerminalAction");
        checkArgPositive(queueSize, "queueSize");
        checkArgPositive(maxWaitForAddingElementToQuery, "maxWaitForAddingElementToQuery");
        checkArgNotNull(executor, "executor");

        return addSubscriberForFilter(predicate, consumerForNewStreamWithTerminalAction, queueSize, maxWaitForAddingElementToQuery, executor);
    }

    private CheckedStream<T, E> addSubscriberForFilter(final Throwables.Predicate<? super T, ? extends E> predicate,
            final Throwables.Consumer<? super CheckedStream<T, E>, ? extends Exception> consumerForNewStreamWithTerminalAction, final int queueSize,
            final long maxWaitForAddingElementToQuery, final Executor executor) {
        final BlockingQueue<T> queue = new ArrayBlockingQueue<>(queueSize <= 0 ? DEFAULT_BUFFERED_SIZE_PER_ITERATOR : queueSize);
        final Throwables.Iterator<T, E> iter = iteratorEx();
        final T none = (T) NONE;

        final MutableBoolean isMainStreamCompleted = MutableBoolean.of(false);
        final MutableBoolean isSubscriberStreamCompleted = MutableBoolean.of(false);
        final MutableBoolean wasQueueFull = MutableBoolean.of(false);
        final MutableInt nextCallCount = MutableInt.of(0); // it should end with 0 if there is no exception happening during hasNext()/next() call.

        final Throwables.Iterator<T, E> iterForSubscriberStream = new Throwables.Iterator<>() { //NOSONAR
            private T next = null;

            public boolean hasNext() throws E {
                if (next == null) {
                    if (isMainStreamCompleted.isFalse() || queue.size() > 0) {
                        try {
                            do {
                                next = queue.poll(MAX_WAIT_TIME_FOR_QUEUE_POLL, TimeUnit.MILLISECONDS);

                                if (nextCallCount.value() > 1 || (isMainStreamCompleted.isTrue() && nextCallCount.value() > 0)) {
                                    throw new IllegalStateException("Exception happened in calling hasNext()/next()");
                                }
                            } while (next == null && (isMainStreamCompleted.isFalse() || queue.size() > 0));
                        } catch (InterruptedException e) {
                            throw toRuntimeException(e);
                        }
                    }
                }

                if (next == null && isMainStreamCompleted.isTrue()) {
                    while (elements.hasNext()) {
                        next = elements.next();

                        if (!predicate.test(next)) {
                            next = next == null ? none : next;
                            return true;
                        }
                    }

                    next = null;
                    return false;
                }

                if (wasQueueFull.isTrue()) {
                    throw new IllegalStateException("Queue(size=" + queueSize + ") was full at some moment. Elements may be missed");
                }

                if (nextCallCount.value() > 1 || (isMainStreamCompleted.isTrue() && nextCallCount.value() > 0)) {
                    throw new IllegalStateException("Exception happened in calling hasNext()/next()");
                }

                return next != null;
            }

            public T next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final T ret = next != null ? (next == none ? null : next) : iter.next();
                next = null;
                return ret;
            }

            @Override
            protected void closeResource() {
                isSubscriberStreamCompleted.setTrue();
                queue.clear();

                if (wasQueueFull.isTrue()) {
                    throw new IllegalStateException("Queue(size=" + queueSize + ") was full at some moment. Elements may be missed");
                }

                if (nextCallCount.value() > 1 || (isMainStreamCompleted.isTrue() && nextCallCount.value() > 0)) {
                    throw new IllegalStateException("Exception happened in calling hasNext()/next()");
                }
            }
        };

        final Throwables.Iterator<T, E> iterA = new Throwables.Iterator<>() { //NOSONAR
            private boolean isNewStreamStarted = false;
            private ContinuableFuture<Void> futureForNewStream = null;
            private boolean hasNext = false;
            private T next = null;

            public boolean hasNext() throws E {
                if (!hasNext) {
                    nextCallCount.increment();

                    while (elements.hasNext()) {
                        next = elements.next();

                        if (predicate.test(next)) {
                            hasNext = true;
                            break;
                        } else {
                            if (!isNewStreamStarted) {
                                startNewStream();
                            }

                            if (isSubscriberStreamCompleted.isFalse()) {
                                try {
                                    if (!queue.offer(next == null ? none : next, maxWaitForAddingElementToQuery, TimeUnit.MILLISECONDS)) {
                                        wasQueueFull.setTrue();
                                    }
                                } catch (InterruptedException e) {
                                    throw toRuntimeException(e);
                                }
                            }
                        }
                    }

                    nextCallCount.decrement();
                }

                return hasNext;
            }

            public T next() throws E {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }

            protected void closeResource() {
                isMainStreamCompleted.setTrue();

                if (!isNewStreamStarted) {
                    startNewStream();
                }

                if (futureForNewStream != null) {
                    try {
                        futureForNewStream.get();
                    } catch (ExecutionException | InterruptedException e) {
                        N.toRuntimeException(e);
                    }
                }
            }

            private void startNewStream() {
                isNewStreamStarted = true;

                if (executor == null) {
                    futureForNewStream = newStream(iterForSubscriberStream).onClose(iterForSubscriberStream::close)
                            .asyncRun(consumerForNewStreamWithTerminalAction); //NOSONAR
                } else {
                    futureForNewStream = newStream(iterForSubscriberStream).onClose(iterForSubscriberStream::close)
                            .asyncRun(consumerForNewStreamWithTerminalAction, executor); //NOSONAR
                }
            }
        };

        return newStream(iterA, sorted, cmp, mergeCloseHandlers(iterA::close, closeHandlers, true));
    }

    /**
     * Attach a new stream with terminal action to consume the elements not token by the specified {@code predicate}.
     * The Intermediate and terminate operations in the attached stream will be executed in a new Thread.
     * The new thread is started when the main stream(the returned stream or its downstream) receives the first element or is closed if there is no element pulled.
     * Elements from upstream pulled by the main stream will be put in a queue for the attached stream to consume.
     * After the main stream is finished, the attached stream will continue to pull remaining elements from upstream if needed.
     * The main stream and the attached stream run independently. Operations in one stream won't impact the elements or final result in another Stream.
     * But when the main stream is to close, it will wait to the attached stream to close before calling close actions.
     *
     * @param predicate
     * @param consumerForNewStreamWithTerminalAction
     * @return
     * @see #takeWhileAddSubscriber(com.landawn.abacus.util.Throwables.Consumer, int, long, Executor)
     */
    public CheckedStream<T, E> takeWhileAddSubscriber(final Throwables.Predicate<? super T, ? extends E> predicate,
            final Throwables.Consumer<? super CheckedStream<T, E>, ? extends Exception> consumerForNewStreamWithTerminalAction) {
        return takeWhileAddSubscriber(predicate, consumerForNewStreamWithTerminalAction, Stream.executor());
    }

    /**
     * Attach a new stream with terminal action to consume the elements not token by the specified {@code predicate}.
     * The Intermediate and terminate operations in the attached stream will be executed in a new Thread.
     * The new thread is started when the main stream(the returned stream or its downstream) receives the first element or is closed if there is no element pulled.
     * Elements from upstream pulled by the main stream will be put in a queue for the attached stream to consume.
     * After the main stream is finished, the attached stream will continue to pull remaining elements from upstream if needed.
     * The main stream and the attached stream run independently. Operations in one stream won't impact the elements or final result in another Stream.
     * But when the main stream is to close, it will wait to the attached stream to close before calling close actions.
     *
     * @param predicate
     * @param consumerForNewStreamWithTerminalAction
     * @param executor
     * @return
     */
    public CheckedStream<T, E> takeWhileAddSubscriber(final Throwables.Predicate<? super T, ? extends E> predicate,
            final Throwables.Consumer<? super CheckedStream<T, E>, ? extends Exception> consumerForNewStreamWithTerminalAction, final Executor executor) {
        assertNotClosed();
        checkArgNotNull(predicate, "predicate");
        checkArgNotNull(consumerForNewStreamWithTerminalAction, "consumerForNewStreamWithTerminalAction");
        checkArgNotNull(executor, "executor");

        // There will only one element will be put into queue at most at the begin for take while. Queue won't be used after the first element.
        return addSubscriberForTakeWhile(predicate, consumerForNewStreamWithTerminalAction, DEFAULT_BUFFERED_SIZE_PER_ITERATOR,
                MAX_WAIT_TIME_FOR_QUEUE_OFFER_FOR_ADD_SUBSCRIBER, executor);
    }

    private CheckedStream<T, E> addSubscriberForTakeWhile(final Throwables.Predicate<? super T, ? extends E> predicate,
            final Throwables.Consumer<? super CheckedStream<T, E>, ? extends Exception> consumerForNewStreamWithTerminalAction, final int queueSize,
            final long maxWaitForAddingElementToQuery, final Executor executor) {
        final BlockingQueue<T> queue = new ArrayBlockingQueue<>(queueSize <= 0 ? DEFAULT_BUFFERED_SIZE_PER_ITERATOR : queueSize);
        final Throwables.Iterator<T, E> iter = iteratorEx();
        final T none = (T) NONE;

        final MutableBoolean isMainStreamCompleted = MutableBoolean.of(false);
        final MutableBoolean isTokenInMainStream = MutableBoolean.of(false);
        final MutableBoolean isSubscriberStreamCompleted = MutableBoolean.of(false);
        final MutableBoolean wasQueueFull = MutableBoolean.of(false);
        final MutableInt nextCallCount = MutableInt.of(0); // it should end with 0 if there is no exception happening during hasNext()/next() call.

        final Throwables.Iterator<T, E> iterForSubscriberStream = new Throwables.Iterator<>() { //NOSONAR
            private T next = null;

            public boolean hasNext() throws E {
                if (next == null) {
                    if ((isTokenInMainStream.isFalse() && isMainStreamCompleted.isFalse()) || queue.size() > 0) {
                        try {
                            do {
                                next = queue.poll(MAX_WAIT_TIME_FOR_QUEUE_POLL, TimeUnit.MILLISECONDS);

                                if (nextCallCount.value() > 1 || (isMainStreamCompleted.isTrue() && nextCallCount.value() > 0)) {
                                    throw new IllegalStateException("Exception happened in calling hasNext()/next()");
                                }
                            } while (next == null && ((isTokenInMainStream.isFalse() && isMainStreamCompleted.isFalse()) || queue.size() > 0));
                        } catch (InterruptedException e) {
                            throw toRuntimeException(e);
                        }
                    }
                }

                if (next == null) {
                    if (isTokenInMainStream.isFalse()) { // it also means isMainStreamCompleted.isTrue()
                        while (elements.hasNext()) {
                            next = elements.next();

                            if (!predicate.test(next)) {
                                next = next == null ? none : next;
                                isTokenInMainStream.setTrue();
                                return true;
                            }
                        }

                        next = null;
                        isTokenInMainStream.setTrue();
                        return false;
                    }
                }

                if (wasQueueFull.isTrue()) {
                    throw new IllegalStateException("Queue(size=" + queueSize + ") was full at some moment. Elements may be missed");
                }

                if (nextCallCount.value() > 1 || (isMainStreamCompleted.isTrue() && nextCallCount.value() > 0)) {
                    throw new IllegalStateException("Exception happened in calling hasNext()/next()");
                }

                return next != null || elements.hasNext();
            }

            public T next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final T ret = next != null ? (next == none ? null : next) : iter.next();
                next = null;
                return ret;
            }

            @Override
            protected void closeResource() {
                isSubscriberStreamCompleted.setTrue();
                queue.clear();

                if (wasQueueFull.isTrue()) {
                    throw new IllegalStateException("Queue(size=" + queueSize + ") was full at some moment. Elements may be missed");
                }

                if (nextCallCount.value() > 1 || (isMainStreamCompleted.isTrue() && nextCallCount.value() > 0)) {
                    throw new IllegalStateException("Exception happened in calling hasNext()/next()");
                }
            }
        };

        final Throwables.Iterator<T, E> iterA = new Throwables.Iterator<>() { //NOSONAR
            private boolean isNewStreamStarted = false;
            private ContinuableFuture<Void> futureForNewStream = null;
            private boolean hasMore = true;
            private boolean hasNext = false;
            private T next = null;

            public boolean hasNext() throws E {
                nextCallCount.increment();

                if (!hasNext && hasMore) {
                    nextCallCount.increment();

                    if (elements.hasNext()) {
                        next = elements.next();

                        if (predicate.test(next)) {
                            hasNext = true;
                        } else {
                            hasMore = false;

                            isTokenInMainStream.setTrue();

                            if (!isNewStreamStarted) {
                                startNewStream();
                            }

                            if (isSubscriberStreamCompleted.isFalse()) {
                                try {
                                    if (!queue.offer(next == null ? none : next, maxWaitForAddingElementToQuery, TimeUnit.MILLISECONDS)) {
                                        wasQueueFull.setTrue();
                                    }
                                } catch (InterruptedException e) {
                                    throw toRuntimeException(e);
                                }
                            }
                        }
                    }

                    nextCallCount.decrement();
                }

                return hasNext;
            }

            public T next() throws E {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }

            protected void closeResource() {
                isMainStreamCompleted.setTrue();

                if (!isNewStreamStarted) {
                    startNewStream();
                }

                if (futureForNewStream != null) {
                    try {
                        futureForNewStream.get();
                    } catch (ExecutionException | InterruptedException e) {
                        N.toRuntimeException(e);
                    }
                }
            }

            private void startNewStream() {
                isNewStreamStarted = true;

                if (executor == null) {
                    futureForNewStream = newStream(iterForSubscriberStream).onClose(iterForSubscriberStream::close)
                            .asyncRun(consumerForNewStreamWithTerminalAction); //NOSONAR
                } else {
                    futureForNewStream = newStream(iterForSubscriberStream).onClose(iterForSubscriberStream::close)
                            .asyncRun(consumerForNewStreamWithTerminalAction, executor); //NOSONAR
                }
            }
        };

        return newStream(iterA, sorted, cmp, mergeCloseHandlers(iterA::close, closeHandlers, true));
    }

    /**
     * Attach a new stream with terminal action to consume the elements dropped by the specified {@code predicate}.
     * The Intermediate and terminate operations in the attached stream will be executed in a new Thread.
     * The new thread is started when the main stream(the returned stream or its downstream) receives the first element or is closed if there is no element pulled.
     * Elements from upstream pulled by the main stream will be put in a queue for the attached stream to consume.
     * After the main stream is finished, the attached stream will continue to pull remaining elements from upstream if needed.
     * The main stream and the attached stream run independently. Operations in one stream won't impact the elements or final result in another Stream.
     * But when the main stream is to close, it will wait to the attached stream to close before calling close actions.
     *
     * @param predicate
     * @param consumerForNewStreamWithTerminalAction
     * @return
     * @see #dropWhileAddSubscriber(com.landawn.abacus.util.Throwables.Consumer, int, long, Executor)
     */
    public CheckedStream<T, E> dropWhileAddSubscriber(final Throwables.Predicate<? super T, ? extends E> predicate,
            final Throwables.Consumer<? super CheckedStream<T, E>, ? extends Exception> consumerForNewStreamWithTerminalAction) {
        return dropWhileAddSubscriber(predicate, consumerForNewStreamWithTerminalAction, DEFAULT_BUFFERED_SIZE_PER_ITERATOR,
                MAX_WAIT_TIME_FOR_QUEUE_OFFER_FOR_ADD_SUBSCRIBER, Stream.executor());
    }

    /**
     * Attach a new stream with terminal action to consume the elements dropped by the specified {@code predicate}.
     * The Intermediate and terminate operations in the attached stream will be executed in a new Thread.
     * The new thread is started when the main stream(the returned stream or its downstream) receives the first element or is closed if there is no element pulled.
     * Elements from upstream pulled by the main stream will be put in a queue for the attached stream to consume.
     * After the main stream is finished, the attached stream will continue to pull remaining elements from upstream if needed.
     * The main stream and the attached stream run independently. Operations in one stream won't impact the elements or final result in another Stream.
     * But when the main stream is to close, it will wait to the attached stream to close before calling close actions.
     *
     * @param predicate
     * @param consumerForNewStreamWithTerminalAction
     * @param queueSize
     * @param maxWaitForAddingElementToQuery default value is 30000 (unit is milliseconds)
     * @param executor
     * @return
     */
    public CheckedStream<T, E> dropWhileAddSubscriber(final Throwables.Predicate<? super T, ? extends E> predicate,
            final Throwables.Consumer<? super CheckedStream<T, E>, ? extends Exception> consumerForNewStreamWithTerminalAction, final int queueSize,
            final long maxWaitForAddingElementToQuery, final Executor executor) {
        assertNotClosed();
        checkArgNotNull(predicate, "predicate");
        checkArgNotNull(consumerForNewStreamWithTerminalAction, "consumerForNewStreamWithTerminalAction");
        checkArgPositive(queueSize, "queueSize");
        checkArgPositive(maxWaitForAddingElementToQuery, "maxWaitForAddingElementToQuery");
        checkArgNotNull(executor, "executor");

        return addSubscriberForDropWhile(predicate, consumerForNewStreamWithTerminalAction, queueSize, maxWaitForAddingElementToQuery, executor);
    }

    private CheckedStream<T, E> addSubscriberForDropWhile(final Throwables.Predicate<? super T, ? extends E> predicate,
            final Throwables.Consumer<? super CheckedStream<T, E>, ? extends Exception> consumerForNewStreamWithTerminalAction, final int queueSize,
            final long maxWaitForAddingElementToQuery, final Executor executor) {
        final BlockingQueue<T> queue = new ArrayBlockingQueue<>(queueSize <= 0 ? DEFAULT_BUFFERED_SIZE_PER_ITERATOR : queueSize);
        final Throwables.Iterator<T, E> iter = iteratorEx();
        final T none = (T) NONE;

        final MutableBoolean isMainStreamCompleted = MutableBoolean.of(false);
        final MutableBoolean isDroppedInMainStream = MutableBoolean.of(false);
        final MutableBoolean isSubscriberStreamCompleted = MutableBoolean.of(false);
        final MutableBoolean wasQueueFull = MutableBoolean.of(false);
        final MutableInt nextCallCount = MutableInt.of(0); // it should end with 0 if there is no exception happening during hasNext()/next() call.

        final Throwables.Iterator<T, E> iterForSubscriberStream = new Throwables.Iterator<>() { //NOSONAR
            private T next = null;

            public boolean hasNext() throws E {
                if (next == null) {
                    if ((isDroppedInMainStream.isFalse() && isMainStreamCompleted.isFalse()) || queue.size() > 0) {
                        try {
                            do {
                                next = queue.poll(MAX_WAIT_TIME_FOR_QUEUE_POLL, TimeUnit.MILLISECONDS);

                                if (nextCallCount.value() > 1 || (isMainStreamCompleted.isTrue() && nextCallCount.value() > 0)) {
                                    throw new IllegalStateException("Exception happened in calling hasNext()/next()");
                                }
                            } while (next == null && ((isDroppedInMainStream.isFalse() && isMainStreamCompleted.isFalse()) || queue.size() > 0));
                        } catch (InterruptedException e) {
                            throw toRuntimeException(e);
                        }
                    }
                }

                if (next == null) {
                    if (isDroppedInMainStream.isFalse()) { // it also means isMainStreamCompleted.isTrue()
                        if (elements.hasNext()) {
                            next = elements.next();

                            if (predicate.test(next)) {
                                next = next == null ? none : next;
                                return true;
                            }
                        }

                        next = null;
                        isDroppedInMainStream.setTrue();
                        return false;
                    }
                }

                if (wasQueueFull.isTrue()) {
                    throw new IllegalStateException("Queue(size=" + queueSize + ") was full at some moment. Elements may be missed");
                }

                if (nextCallCount.value() > 1 || (isMainStreamCompleted.isTrue() && nextCallCount.value() > 0)) {
                    throw new IllegalStateException("Exception happened in calling hasNext()/next()");
                }

                return next != null;
            }

            public T next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final T ret = next != null ? (next == none ? null : next) : iter.next();
                next = null;
                return ret;
            }

            @Override
            protected void closeResource() {
                isSubscriberStreamCompleted.setTrue();
                queue.clear();

                if (wasQueueFull.isTrue()) {
                    throw new IllegalStateException("Queue(size=" + queueSize + ") was full at some moment. Elements may be missed");
                }

                if (nextCallCount.value() > 1 || (isMainStreamCompleted.isTrue() && nextCallCount.value() > 0)) {
                    throw new IllegalStateException("Exception happened in calling hasNext()/next()");
                }
            }
        };

        final Throwables.Iterator<T, E> iterA = new Throwables.Iterator<>() { //NOSONAR
            private boolean isNewStreamStarted = false;
            private ContinuableFuture<Void> futureForNewStream = null;
            private boolean hasNext = false;
            private T next = null;
            private boolean dropped = false;

            public boolean hasNext() throws E {
                if (!hasNext) {
                    if (!dropped) {
                        dropped = true;

                        try {
                            nextCallCount.increment();

                            while (elements.hasNext()) {
                                next = elements.next();

                                if (!predicate.test(next)) {
                                    isDroppedInMainStream.setTrue();
                                    hasNext = true;
                                    break;
                                } else {
                                    if (!isNewStreamStarted) {
                                        startNewStream();
                                    }

                                    if (isSubscriberStreamCompleted.isFalse()) {
                                        try {
                                            if (!queue.offer(next == null ? none : next, maxWaitForAddingElementToQuery, TimeUnit.MILLISECONDS)) {
                                                wasQueueFull.setTrue();
                                            }
                                        } catch (InterruptedException e) {
                                            throw toRuntimeException(e);
                                        }
                                    }
                                }
                            }

                            nextCallCount.decrement();
                        } finally {
                            if (nextCallCount.value() > 0) { // exception happened. set nextCallCount to 2.
                                nextCallCount.increment();
                            }
                        }
                    } else if (elements.hasNext()) {
                        next = elements.next();
                        hasNext = true;
                    }
                }

                return hasNext;
            }

            public T next() throws E {
                if (!hasNext && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                hasNext = false;

                return next;
            }

            protected void closeResource() {
                isMainStreamCompleted.setTrue();

                if (!isNewStreamStarted) {
                    startNewStream();
                }

                if (futureForNewStream != null) {
                    try {
                        futureForNewStream.get();
                    } catch (ExecutionException | InterruptedException e) {
                        N.toRuntimeException(e);
                    }
                }
            }

            private void startNewStream() {
                isNewStreamStarted = true;

                if (executor == null) {
                    futureForNewStream = newStream(iterForSubscriberStream).onClose(iterForSubscriberStream::close)
                            .asyncRun(consumerForNewStreamWithTerminalAction); //NOSONAR
                } else {
                    futureForNewStream = newStream(iterForSubscriberStream).onClose(iterForSubscriberStream::close)
                            .asyncRun(consumerForNewStreamWithTerminalAction, executor); //NOSONAR
                }
            }
        };

        return newStream(iterA, sorted, cmp, mergeCloseHandlers(iterA::close, closeHandlers, true));
    }

    Throwables.Iterator<T, E> iteratorEx() {
        return elements;
    }

    /**
     *
     *
     * @param terminalAction a terminal operation should be called.
     * @return
     */
    @Beta
    @TerminalOp
    public ContinuableFuture<Void> asyncRun(final Throwables.Consumer<? super CheckedStream<T, E>, ? extends Exception> terminalAction) {
        assertNotClosed();

        checkArgNotNull(terminalAction, "terminalAction");

        return ContinuableFuture.run(() -> terminalAction.accept(CheckedStream.this));
    }

    /**
     *
     * @param terminalAction a terminal operation should be called.
     * @param executor
     * @return
     */
    @Beta
    @TerminalOp
    public ContinuableFuture<Void> asyncRun(final Throwables.Consumer<? super CheckedStream<T, E>, ? extends Exception> terminalAction,
            final Executor executor) {
        assertNotClosed();

        checkArgNotNull(terminalAction, "terminalAction");
        checkArgNotNull(executor, "executor");

        return ContinuableFuture.run(() -> terminalAction.accept(CheckedStream.this), executor);
    }

    /**
     *
     * @param <R>
     * @param terminalAction a terminal operation should be called.
     * @return
     */
    @Beta
    @TerminalOp
    public <R> ContinuableFuture<R> asyncCall(final Throwables.Function<? super CheckedStream<T, E>, R, ? extends Exception> terminalAction) {
        assertNotClosed();

        checkArgNotNull(terminalAction, "terminalAction");

        return ContinuableFuture.call(() -> terminalAction.apply(CheckedStream.this));
    }

    /**
     *
     * @param <R>
     * @param terminalAction a terminal operation should be called.
     * @param executor
     * @return
     */
    @Beta
    @TerminalOp
    public <R> ContinuableFuture<R> asyncCall(final Throwables.Function<? super CheckedStream<T, E>, R, ? extends Exception> terminalAction,
            final Executor executor) {
        assertNotClosed();

        checkArgNotNull(terminalAction, "terminalAction");
        checkArgNotNull(executor, "executor");

        return ContinuableFuture.call(() -> terminalAction.apply(CheckedStream.this), executor);
    }

    /**
     *
     * @param closeHandler
     * @return
     */
    @IntermediateOp
    public CheckedStream<T, E> onClose(final Runnable closeHandler) {
        assertNotClosed();

        if (isEmptyCloseHandler(closeHandler)) {
            return this;
        }

        final Deque<LocalRunnable> newCloseHandlers = new ArrayDeque<>(N.size(closeHandlers) + 1);

        if (N.notEmpty(this.closeHandlers)) {
            newCloseHandlers.addAll(this.closeHandlers);
        }

        newCloseHandlers.add(new LocalRunnable() {
            private volatile boolean isClosed = false;

            @Override
            public void run() {
                if (isClosed) {
                    return;
                }

                isClosed = true;
                closeHandler.run();
            }
        });

        return newStream(elements, newCloseHandlers);
    }

    /**
     * It will be called by terminal operations in final.
     */
    @Override
    @TerminalOp
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

        logger.debug("Closing Seq");

        close(closeHandlers);

        if (N.notEmpty(closeHandlers)) {
            closeHandlers.clear();
        }
    }

    private void assertNotClosed() {
        if (isClosed) {
            throw new IllegalStateException("This stream is already terminated.");
        }
    }

    private int checkArgPositive(final int arg, final String argNameOrErrorMsg) {
        if (arg <= 0) {
            try {
                N.checkArgPositive(arg, argNameOrErrorMsg);
            } finally {
                close();
            }
        }

        return arg;
    }

    private long checkArgPositive(final long arg, final String argNameOrErrorMsg) {
        if (arg <= 0) {
            try {
                N.checkArgPositive(arg, argNameOrErrorMsg);
            } finally {
                close();
            }
        }

        return arg;
    }

    private int checkArgNotNegative(final int arg, final String argNameOrErrorMsg) {
        if (arg < 0) {
            try {
                N.checkArgNotNegative(arg, argNameOrErrorMsg);
            } finally {
                close();
            }
        }

        return arg;
    }

    private long checkArgNotNegative(final long arg, final String argNameOrErrorMsg) {
        if (arg < 0) {
            try {
                N.checkArgNotNegative(arg, argNameOrErrorMsg);
            } finally {
                close();
            }
        }

        return arg;
    }

    private <ARG> ARG checkArgNotNull(final ARG obj, final String errorMessage) {
        if (obj == null) {
            try {
                N.checkArgNotNull(obj, errorMessage);
            } finally {
                close();
            }
        }

        return obj;
    }

    @SuppressWarnings("rawtypes")
    private <ARG extends Collection> ARG checkArgNotEmpty(final ARG obj, final String errorMessage) {
        if (obj == null || obj.size() == 0) {
            try {
                N.checkArgNotEmpty(obj, errorMessage);
            } finally {
                close();
            }
        }

        return obj;
    }

    private void checkArgument(boolean b, String errorMessage) {
        if (!b) {
            try {
                N.checkArgument(b, errorMessage);
            } finally {
                close();
            }
        }
    }

    private void checkArgument(boolean b, String errorMessageTemplate, int p1, int p2) {
        if (!b) {
            try {
                N.checkArgument(b, errorMessageTemplate, p1, p2);
            } finally {
                close();
            }
        }
    }

    private void checkArgument(boolean b, String errorMessageTemplate, long p1, long p2) {
        if (!b) {
            try {
                N.checkArgument(b, errorMessageTemplate, p1, p2);
            } finally {
                close();
            }
        }
    }

    private ObjIteratorEx<T> newObjIteratorEx(final Throwables.Iterator<T, E> elements) {
        return new ObjIteratorEx<>() {
            public boolean hasNext() {
                try {
                    return elements.hasNext();
                } catch (Exception e) {
                    throw ExceptionUtil.toRuntimeException(e);
                }
            }

            public T next() { // NOSONAR
                try {
                    return elements.next();
                } catch (Exception e) {
                    throw ExceptionUtil.toRuntimeException(e);
                }
            }

            public void advance(long n) {
                try {
                    elements.advance(n);
                } catch (Exception e) {
                    throw ExceptionUtil.toRuntimeException(e);
                }
            }

            public long count() {
                try {
                    return elements.count();
                } catch (Exception e) {
                    throw ExceptionUtil.toRuntimeException(e);
                }
            }
        };
    }

    private static <T, E extends Exception> CheckedStream<T, E> newStream(final Throwables.Iterator<? extends T, ? extends E> iter) {
        return newStream(iter, iter::close);
    }

    private static <T, E extends Exception> CheckedStream<T, E> newStream(final Throwables.Iterator<? extends T, ? extends E> iter,
            final LocalRunnable closeHandler) {
        final Deque<LocalRunnable> closeHandlers = new LocalArrayDeque<>();
        closeHandlers.add(closeHandler);

        return newStream(iter, closeHandlers);
    }

    private static <T, E extends Exception> CheckedStream<T, E> newStream(final Throwables.Iterator<? extends T, ? extends E> iter,
            final Collection<LocalRunnable> closeHandlers) {
        return new CheckedStream<>(iter, closeHandlers);
    }

    private static <T, E extends Exception> CheckedStream<T, E> newStream(final Throwables.Iterator<T, E> iter, final boolean sorted,
            final Comparator<? super T> comparator, final Collection<LocalRunnable> closeHandlers) {
        return new CheckedStream<>(iter, sorted, comparator, closeHandlers);
    }

    private static <T, E extends Exception> CheckedStream<T, E> newStream(final Stream<? extends T> stream, final boolean isForSps) {
        if (stream == null) {
            return empty();
        }

        Throwables.Iterator<T, E> iter = null;

        if (isForSps) {
            iter = new Throwables.Iterator<>() {
                private Stream<? extends T> s = stream;
                private Iterator<? extends T> iter = null;
                private boolean isInitialized = false;

                public boolean hasNext() throws E {
                    try {
                        if (!isInitialized) {
                            init();
                        }

                        return iter.hasNext();
                    } catch (Exception e) {
                        throw (E) ExceptionUtil.tryToGetOriginalCheckedException(e);
                    }
                }

                public T next() throws E {
                    try {
                        if (!isInitialized) {
                            init();
                        }

                        return iter.next();
                    } catch (Exception e) {
                        throw (E) ExceptionUtil.tryToGetOriginalCheckedException(e);
                    }
                }

                public void advance(long n) throws E {
                    try {
                        if (iter == null) {
                            s = s.skip(n);
                        } else {
                            while (n-- > 0 && hasNext()) {
                                next();
                            }
                        }
                    } catch (Exception e) {
                        throw (E) ExceptionUtil.tryToGetOriginalCheckedException(e);
                    }
                }

                public long count() throws E {
                    try {
                        if (iter == null) {
                            return s.count();
                        } else {
                            long result = 0;

                            while (hasNext()) {
                                next();
                                result++;
                            }

                            return result;
                        }
                    } catch (Exception e) {
                        throw (E) ExceptionUtil.tryToGetOriginalCheckedException(e);
                    }
                }

                protected void closeResource() {
                    s.close();
                }

                private void init() {
                    if (!isInitialized) {
                        isInitialized = true;
                        iter = s.iterator();
                    }
                }
            };
        } else {
            iter = new Throwables.Iterator<>() {
                private Stream<? extends T> s = stream;
                private Iterator<? extends T> iter = null;
                private boolean isInitialized = false;

                public boolean hasNext() throws E {
                    if (!isInitialized) {
                        init();
                    }

                    return iter.hasNext();
                }

                public T next() throws E {
                    if (!isInitialized) {
                        init();
                    }

                    return iter.next();
                }

                public void advance(long n) throws E {
                    if (iter == null) {
                        s = s.skip(n);
                    } else {
                        while (n-- > 0 && hasNext()) {
                            next();
                        }
                    }
                }

                public long count() throws E {
                    if (iter == null) {
                        return s.count();
                    } else {
                        long result = 0;

                        while (hasNext()) {
                            next();
                            result++;
                        }

                        return result;
                    }
                }

                protected void closeResource() {
                    s.close();
                }

                private void init() {
                    if (!isInitialized) {
                        isInitialized = true;
                        iter = stream.iterator();
                    }
                }
            };
        }

        final Throwables.Iterator<T, E> tmp = iter;

        return newStream(tmp).onClose(newCloseHandler(tmp));
    }

    private static <T, E extends Exception> Throwables.Iterator<T, E> iterate(final CheckedStream<? extends T, E> s) {
        return s == null ? Throwables.Iterator.empty() : (Throwables.Iterator<T, E>) s.iteratorEx();
    }

    private static <T, E extends Exception> Throwables.Iterator<T, E> buffered(final Throwables.Iterator<T, E> iter, final BlockingQueue<T> queueToBuffer) {
        return buffered(iter, queueToBuffer, null);
    }

    private static <T, E extends Exception> Throwables.Iterator<T, E> buffered(final Throwables.Iterator<T, E> iter, final BlockingQueue<T> queueToBuffer,
            final MutableBoolean hasMore) {
        final MutableBoolean onGoing = MutableBoolean.of(true);

        final T none = (T) NONE;
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean disposableChecked = MutableBoolean.of(false);
        final AtomicInteger threadCounter = new AtomicInteger(1);
        boolean noException = false;

        try {
            N.asyncExecute(() -> {
                try {
                    T next = null;

                    while (onGoing.value() && iter.hasNext()) {
                        next = iter.next();

                        if (next == null) {
                            next = none;
                        } else if (disposableChecked.isFalse()) {
                            disposableChecked.setTrue();

                            if (next instanceof NoCachingNoUpdating) {
                                throw new RuntimeException("Can't run NoCachingNoUpdating Objects in parallel Stream or Queue");
                            }
                        }

                        if (!queueToBuffer.offer(next)) {
                            while (onGoing.value()) {
                                if (queueToBuffer.offer(next, MAX_WAIT_TIME_FOR_QUEUE_OFFER, TimeUnit.MILLISECONDS)) {
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    setError(eHolder, e, onGoing);
                } finally {
                    if (threadCounter.decrementAndGet() == 0) {
                        if (hasMore != null) {
                            hasMore.setFalse();
                        }
                    }
                }
            });

            noException = true;
        } finally {
            if (!noException) {
                onGoing.setFalse();

                if (hasMore != null) {
                    hasMore.setFalse();
                }
            }
        }

        return new Throwables.Iterator<>() {
            private boolean isClosed = false;
            private T next = null;

            public boolean hasNext() throws E {
                try {
                    if (next == null && (next = queueToBuffer.poll()) == null) {
                        while (onGoing.value() && (threadCounter.get() > 0 || queueToBuffer.size() > 0)) { // (queue.size() > 0 || counter.get() > 0) is wrong. has to check counter first
                            if ((next = queueToBuffer.poll(MAX_WAIT_TIME_FOR_QUEUE_POLL, TimeUnit.MILLISECONDS)) != null) {
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    setError(eHolder, e, onGoing);
                }

                if (eHolder.value() != null) {
                    setStopFlagAndThrowException(eHolder, onGoing);
                }

                return next != null;
            }

            public T next() throws E {
                if (next == null && !hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                T result = next == NONE ? null : next;
                next = null;

                return result;
            }

            protected void closeResource() {
                if (isClosed) {
                    return;
                }

                isClosed = true;
                onGoing.setFalse();
            }
        };
    }

    private static <K, V, E extends Exception> void merge(Map<K, V> map, K key, V value, Throwables.BinaryOperator<V, E> remappingFunction) throws E {
        final V oldValue = map.get(key);

        if (oldValue == null && !map.containsKey(key)) {
            map.put(key, value);
        } else {
            map.put(key, remappingFunction.apply(oldValue, value));
        }
    }

    private static void close(final Collection<? extends Runnable> closeHandlers) {
        Exception ex = null;

        for (Runnable closeHandler : closeHandlers) {
            try {
                closeHandler.run();
            } catch (Exception e) {
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
    private static void close(Holder<? extends CheckedStream> holder) {
        if (holder.value() != null) {
            holder.value().close();
        }
    }

    private static boolean isEmptyCloseHandler(final Runnable closeHandler) {
        return closeHandler == null || closeHandler == EMPTY_CLOSE_HANDLER;
    }

    private static boolean isEmptyCloseHandlers(final Collection<? extends Runnable> closeHandlers) {
        return N.isEmpty(closeHandlers) || (closeHandlers.size() == 1 && isEmptyCloseHandler(N.firstOrNullIfEmpty(closeHandlers)));
    }

    private static LocalRunnable newCloseHandler(final Runnable closeHandler) {
        return LocalRunnable.wrap(closeHandler);
    }

    private static LocalRunnable newCloseHandler(final AutoCloseable closeable) {
        return LocalRunnable.wrap(closeable);
    }

    @SuppressWarnings("rawtypes")
    private static LocalRunnable newCloseHandler(final CheckedStream s) {
        if (s == null || isEmptyCloseHandlers(s.closeHandlers)) {
            return EMPTY_CLOSE_HANDLER;
        }

        return s::close;
    }

    @SuppressWarnings("rawtypes")
    private static LocalRunnable newCloseHandler(final Collection<? extends CheckedStream> c) {
        if (N.isEmpty(c)) {
            return EMPTY_CLOSE_HANDLER;
        }

        boolean allEmptyHandlers = true;

        for (CheckedStream s : c) {
            if (!(s == null || s.isClosed || isEmptyCloseHandlers(s.closeHandlers))) {
                allEmptyHandlers = false;
                break;
            }
        }

        if (allEmptyHandlers) {
            return EMPTY_CLOSE_HANDLER;
        }

        return () -> {
            RuntimeException runtimeException = null;

            for (CheckedStream s : c) {
                if (s == null || s.isClosed || isEmptyCloseHandlers(s.closeHandlers)) {
                    continue;
                }

                try {
                    s.close();
                } catch (Exception throwable) {
                    if (runtimeException == null) {
                        runtimeException = toRuntimeException(throwable);
                    } else {
                        runtimeException.addSuppressed(throwable);
                    }
                }
            }

            if (runtimeException != null) {
                throw runtimeException;
            }
        };
    }

    private static Deque<LocalRunnable> mergeCloseHandlers(final Runnable newCloseHandlerToAdd, final Deque<LocalRunnable> closeHandlers) {
        return mergeCloseHandlers(newCloseHandlerToAdd, closeHandlers, false);
    }

    private static Deque<LocalRunnable> mergeCloseHandlers(final Runnable newCloseHandlerToAdd, final Deque<LocalRunnable> closeHandlers,
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

    private static void setError(final Holder<Throwable> errorHolder, Throwable e) {
        synchronized (errorHolder) { //NOSONAR
            if (errorHolder.value() == null) {
                errorHolder.setValue(e);
            } else {
                errorHolder.value().addSuppressed(e);
            }
        }
    }

    private static void setError(final Holder<Throwable> errorHolder, Throwable e, final MutableBoolean onGoing) {
        // Set error handle first, then set onGoing sign.
        // If onGoing sign is set first but error has not been set, threads may stop without throwing exception because errorHolder is empty.
        setError(errorHolder, e);

        onGoing.setFalse();
    }

    private static void setStopFlagAndThrowException(final Holder<Throwable> errorHolder, final MutableBoolean onGoing) {
        onGoing.setFalse();

        synchronized (errorHolder) { //NOSONAR
            if (errorHolder.value() != null) {
                throw toRuntimeException(errorHolder.getAndSet(null), true);
            }
        }
    }

    private static RuntimeException toRuntimeException(final Exception e) {
        return ExceptionUtil.toRuntimeException(e);
    }

    private static RuntimeException toRuntimeException(final Throwable e, final boolean throwIfItIsError) {
        return ExceptionUtil.toRuntimeException(e, throwIfItIsError);
    }

    private static boolean isSameComparator(Comparator<?> a, Comparator<?> b) {
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

    private static Object hashKey(Object obj) {
        return obj == null ? NONE : (obj.getClass().isArray() ? Wrapper.of(obj) : obj);
    }

    private static <T> List<T> subList(final List<T> list, final int fromIndex, final int toIndex) {
        return list.subList(fromIndex, N.min(list.size(), toIndex));
    }

    @Internal
    private interface LocalRunnable extends Runnable {

        static LocalRunnable wrap(Runnable closeHandler) {
            if (closeHandler == null) {
                return EMPTY_CLOSE_HANDLER;
            } else if (closeHandler instanceof LocalRunnable) {
                return (LocalRunnable) closeHandler;
            }

            return new LocalRunnable() {
                private volatile boolean isClosed = false;

                @Override
                public void run() {
                    if (isClosed) {
                        return;
                    }

                    isClosed = true;
                    closeHandler.run();
                }
            };
        }

        static LocalRunnable wrap(final AutoCloseable closeable) {
            return new LocalRunnable() {
                private volatile boolean isClosed = false;

                @Override
                public void run() {
                    if (isClosed) {
                        return;
                    }

                    isClosed = true;
                    IOUtil.close(closeable);
                }
            };
        }
    }

    @Internal
    private static final class LocalArrayDeque<T> extends ArrayDeque<T> {
        private static final long serialVersionUID = -97425473105100734L;

        public LocalArrayDeque() {
        }

        public LocalArrayDeque(int initialCapacity) {
            super(initialCapacity);
        }

        public LocalArrayDeque(Collection<? extends T> c) {
            super(c);
        }
    }

    // CheckedException -> Maybe makes sense. Checked exception...
    // But what does Seq mean? Checked stream ???
    //    public static final class Seq<T, E extends Exception> extends Seq<T, E> {
    //
    //        Seq(Throwables.Iterator<T, E> iter, boolean sorted, Comparator<? super T> comparator, Deque<Throwables.Runnable<? extends E>> closeHandlers) {
    //            super(iter, sorted, comparator, closeHandlers);
    //        }
    //    }

    ///**
    // *
    // * @param <T>
    // * @param <E>
    // */
    //public static final class Seq<T> extends Seq<T, RuntimeException> {
    //
    //    Seq(Throwables.Iterator<T, RuntimeException> iter, boolean sorted, Comparator<? super T> comparator,
    //            Deque<Throwables.Runnable<? extends RuntimeException>> closeHandlers) {
    //        super(iter, sorted, comparator, closeHandlers);
    //    }
    //}

    //    /**
    //     * Mostly it's for android.
    //     *
    //     * @see {@code Seq<T, RuntimeException>}
    //     *
    //     * @deprecated Mostly it's for android.
    //     */
    //    @Deprecated
    //    @Beta
    //    public static final class StreamR extends Seq {
    //        private StreamR() {
    //            // singleton for utility class.
    //        }
    //    }

    //    /**
    //     * Mostly it's for android.
    //     *
    //     * @see {@code Seq<T, RuntimeException>}
    //     *
    //     * @deprecated Mostly it's for android.
    //     */
    //    @Beta
    //    @Deprecated
    //    public static final class Seq {
    //        private Seq() {
    //            // singleton for utility class.
    //        }
    //
    //        public static <T> Seq<T, RuntimeException> empty() {
    //            return Seq.<T, RuntimeException> empty();
    //        }
    //
    //        public static <T> Seq<T, RuntimeException> just(final T e) {
    //            return Seq.<T, RuntimeException> just(e);
    //        }
    //
    //        public static <T> Seq<T, RuntimeException> ofNullable(final T e) {
    //            return Seq.<T, RuntimeException> ofNullable(e);
    //        }
    //
    //        public static <T> Seq<T, RuntimeException> of(final T... a) {
    //            return Seq.<T, RuntimeException> of(a);
    //        }
    //
    //        public static <T> Seq<T, RuntimeException> of(final Collection<? extends T> c) {
    //            return Seq.<T, RuntimeException> of(c);
    //        }
    //
    //        public static <T> Seq<T, RuntimeException> of(final Iterator<? extends T> iter) {
    //            return Seq.<T, RuntimeException> of(iter);
    //        }
    //
    //        public static <T> Seq<T, RuntimeException> of(final Iterable<? extends T> iterable) {
    //            return Seq.<T, RuntimeException> of(iterable);
    //        }
    //
    //        public static <K, V> Seq<Map.Entry<K, V>, RuntimeException> of(final Map<K, V> m) {
    //            return Seq.<K, V, RuntimeException> of(m);
    //        }
    //
    //        public static <T> Seq<T, RuntimeException> of(final Stream<? extends T> stream) {
    //            return Seq.<T, RuntimeException> of(stream);
    //        }
    //
    //        public static <T> Seq<T, RuntimeException> of(final java.util.stream.Stream<? extends T> stream) {
    //            return Seq.<T, RuntimeException> of(stream);
    //        }
    //
    //        public static Seq<Boolean, RuntimeException> of(final boolean[] a) {
    //            return Seq.<RuntimeException> of(a);
    //        }
    //
    //        public static Seq<Character, RuntimeException> of(final char[] a) {
    //            return Seq.<RuntimeException> of(a);
    //        }
    //
    //        public static Seq<Byte, RuntimeException> of(final byte[] a) {
    //            return Seq.<RuntimeException> of(a);
    //        }
    //
    //        public static Seq<Short, RuntimeException> of(final short[] a) {
    //            return Seq.<RuntimeException> of(a);
    //        }
    //
    //        public static Seq<Integer, RuntimeException> of(final int[] a) {
    //            return Seq.<RuntimeException> of(a);
    //        }
    //
    //        public static Seq<Long, RuntimeException> of(final long[] a) {
    //            return Seq.<RuntimeException> of(a);
    //        }
    //
    //        public static Seq<Float, RuntimeException> of(final float[] a) {
    //            return Seq.<RuntimeException> of(a);
    //        }
    //
    //        public static Seq<Double, RuntimeException> of(final double[] a) {
    //            return Seq.<RuntimeException> of(a);
    //        }
    //
    //        public static <T> Seq<T, RuntimeException> of(final Optional<T> op) {
    //            return Seq.<T, RuntimeException> of(op);
    //        }
    //
    //        public static <T> Seq<T, RuntimeException> of(final java.util.Optional<T> op) {
    //            return Seq.<T, RuntimeException> of(op);
    //        }
    //
    //        public static <K> Seq<K, RuntimeException> ofKeys(final Map<K, ?> map) {
    //            return Seq.<K, RuntimeException> ofKeys(map);
    //        }
    //
    //        public static <K, V> Seq<K, RuntimeException> ofKeys(final Map<K, V> map,
    //                final Throwables.Predicate<? super V, RuntimeException> valueFilter) {
    //            return Seq.<K, V, RuntimeException> ofKeys(map, valueFilter);
    //        }
    //
    //        public static <K, V> Seq<K, RuntimeException> ofKeys(final Map<K, V> map,
    //                final Throwables.BiPredicate<? super K, ? super V, RuntimeException> filter) {
    //            return Seq.ofKeys(map, filter);
    //        }
    //
    //        public static <V> Seq<V, RuntimeException> ofValues(final Map<?, V> map) {
    //            return Seq.<V, RuntimeException> ofValues(map);
    //        }
    //
    //        public static <K, V> Seq<V, RuntimeException> ofValues(final Map<K, V> map,
    //                final Throwables.Predicate<? super K, RuntimeException> keyFilter) {
    //            return Seq.<K, V, RuntimeException> ofValues(map, keyFilter);
    //        }
    //
    //        public static <K, V> Seq<V, RuntimeException> ofValues(final Map<K, V> map,
    //                final Throwables.BiPredicate<? super K, ? super V, RuntimeException> filter) {
    //            return Seq.ofValues(map, filter);
    //        }
    //
    //        //    @Beta
    //        //    public static <T> Seq<T, RuntimeException> from(final Throwables.Supplier<Collection<? extends T>, RuntimeException> supplier) {
    //        //        return Seq.<T, RuntimeException> from(supplier);
    //        //    }
    //
    //        public static <T> Seq<T, RuntimeException> defer(
    //                final Throwables.Supplier<Seq<? extends T, ? extends RuntimeException>, RuntimeException> supplier) {
    //            return Seq.<T, RuntimeException> defer(supplier);
    //        }
    //
    //        public static <T> Seq<T, RuntimeException> iterate(final Throwables.BooleanSupplier<? extends RuntimeException> hasNext,
    //                final Throwables.Supplier<? extends T, RuntimeException> next) {
    //            return Seq.<T, RuntimeException> iterate(hasNext, next);
    //        }
    //
    //        public static <T> Seq<T, RuntimeException> iterate(final T init, final Throwables.BooleanSupplier<? extends RuntimeException> hasNext,
    //                final Throwables.UnaryOperator<T, ? extends RuntimeException> f) {
    //            return Seq.<T, RuntimeException> iterate(init, hasNext, f);
    //        }
    //
    //        public static <T> Seq<T, RuntimeException> iterate(final T init, final Throwables.Predicate<? super T, RuntimeException> hasNext,
    //                final Throwables.UnaryOperator<T, RuntimeException> f) {
    //            return Seq.<T, RuntimeException> iterate(init, hasNext, f);
    //        }
    //
    //        public static <T> Seq<T, RuntimeException> iterate(final T init, final Throwables.UnaryOperator<T, RuntimeException> f) {
    //            return Seq.<T, RuntimeException> iterate(init, f);
    //        }
    //
    //        public static <T> Seq<T, RuntimeException> generate(final Throwables.Supplier<T, RuntimeException> supplier) {
    //            return Seq.<T, RuntimeException> generate(supplier);
    //        }
    //
    //        public static <T> Seq<T, RuntimeException> repeat(final T element, final long n) {
    //            return Seq.<T, RuntimeException> repeat(element, n);
    //        }
    //
    //        public static Seq<Integer, RuntimeException> range(final int startInclusive, final int endExclusive) {
    //            return Seq.<RuntimeException> range(startInclusive, endExclusive);
    //        }
    //
    //        public static Seq<Integer, RuntimeException> range(final int startInclusive, final int endExclusive, final int by) {
    //            return Seq.<RuntimeException> range(startInclusive, endExclusive, by);
    //        }
    //
    //        public static Seq<Integer, RuntimeException> rangeClosed(final int startInclusive, final int endExclusive) {
    //            return Seq.<RuntimeException> rangeClosed(startInclusive, endExclusive);
    //        }
    //
    //        public static Seq<Integer, RuntimeException> rangeClosed(final int startInclusive, final int endExclusive, final int by) {
    //            return Seq.<RuntimeException> rangeClosed(startInclusive, endExclusive, by);
    //        }
    //
    //        @SafeVarargs
    //        public static <T> Seq<T, RuntimeException> concat(final T[]... a) {
    //            return Seq.<T, RuntimeException> concat(a);
    //        }
    //
    //        @SafeVarargs
    //        public static <T> Seq<T, RuntimeException> concat(final Iterable<? extends T>... a) {
    //            return Seq.<T, RuntimeException> concat(a);
    //        }
    //
    //        @SafeVarargs
    //        public static <T> Seq<T, RuntimeException> concat(final Iterator<? extends T>... a) {
    //            return Seq.<T, RuntimeException> concat(a);
    //        }
    //
    //        public static <A, B, T> Seq<T, RuntimeException> zip(final A[] a, final B[] b,
    //                final Throwables.BiFunction<? super A, ? super B, T, RuntimeException> zipFunction) {
    //            return Seq.<A, B, T, RuntimeException> zip(a, b, zipFunction);
    //        }
    //
    //        public static <A, B, C, T> Seq<T, RuntimeException> zip(final A[] a, final B[] b, final C[] c,
    //                final Throwables.TriFunction<? super A, ? super B, ? super C, T, RuntimeException> zipFunction) {
    //            return Seq.<A, B, C, T, RuntimeException> zip(a, b, c, zipFunction);
    //        }
    //
    //        public static <A, B, T> Seq<T, RuntimeException> zip(final Iterable<? extends A> a, final Iterable<? extends B> b,
    //                final Throwables.BiFunction<? super A, ? super B, T, RuntimeException> zipFunction) {
    //            return Seq.<A, B, T, RuntimeException> zip(a, b, zipFunction);
    //        }
    //
    //        public static <A, B, C, T> Seq<T, RuntimeException> zip(final Iterable<? extends A> a, final Iterable<? extends B> b,
    //                final Iterable<? extends C> c, final Throwables.TriFunction<? super A, ? super B, ? super C, T, RuntimeException> zipFunction) {
    //            return Seq.<A, B, C, T, RuntimeException> zip(a, b, c, zipFunction);
    //        }
    //
    //        public static <A, B, T> Seq<T, RuntimeException> zip(final Iterator<? extends A> a, final Iterator<? extends B> b,
    //                final Throwables.BiFunction<? super A, ? super B, T, RuntimeException> zipFunction) {
    //            return Seq.<A, B, T, RuntimeException> zip(a, b, zipFunction);
    //        }
    //
    //        public static <A, B, C, T> Seq<T, RuntimeException> zip(final Iterator<? extends A> a, final Iterator<? extends B> b,
    //                final Iterator<? extends C> c, final Throwables.TriFunction<? super A, ? super B, ? super C, T, RuntimeException> zipFunction) {
    //            return Seq.<A, B, C, T, RuntimeException> zip(a, b, c, zipFunction);
    //        }
    //
    //        public static <A, B, T> Seq<T, RuntimeException> zip(final A[] a, final B[] b, final A valueForNoneA, final B valueForNoneB,
    //                final Throwables.BiFunction<? super A, ? super B, T, RuntimeException> zipFunction) {
    //            return Seq.<A, B, T, RuntimeException> zip(a, b, valueForNoneA, valueForNoneB, zipFunction);
    //        }
    //
    //        public static <A, B, C, T> Seq<T, RuntimeException> zip(final A[] a, final B[] b, final C[] c, final A valueForNoneA,
    //                final B valueForNoneB, final C valueForNoneC, final Throwables.TriFunction<? super A, ? super B, ? super C, T, RuntimeException> zipFunction) {
    //            return Seq.<A, B, C, T, RuntimeException> zip(a, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    //        }
    //
    //        public static <A, B, T> Seq<T, RuntimeException> zip(final Iterable<? extends A> a, final Iterable<? extends B> b, final A valueForNoneA,
    //                final B valueForNoneB, final Throwables.BiFunction<? super A, ? super B, T, RuntimeException> zipFunction) {
    //            return Seq.<A, B, T, RuntimeException> zip(a, b, valueForNoneA, valueForNoneB, zipFunction);
    //        }
    //
    //        public static <A, B, C, T> Seq<T, RuntimeException> zip(final Iterable<? extends A> a, final Iterable<? extends B> b,
    //                final Iterable<? extends C> c, final A valueForNoneA, final B valueForNoneB, final C valueForNoneC,
    //                final Throwables.TriFunction<? super A, ? super B, ? super C, T, RuntimeException> zipFunction) {
    //            return Seq.<A, B, C, T, RuntimeException> zip(a, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    //        }
    //
    //        public static <A, B, T> Seq<T, RuntimeException> zip(final Iterator<? extends A> a, final Iterator<? extends B> b, final A valueForNoneA,
    //                final B valueForNoneB, final Throwables.BiFunction<? super A, ? super B, T, RuntimeException> zipFunction) {
    //            return Seq.<A, B, T, RuntimeException> zip(a, b, valueForNoneA, valueForNoneB, zipFunction);
    //        }
    //
    //        public static <A, B, C, T> Seq<T, RuntimeException> zip(final Iterator<? extends A> a, final Iterator<? extends B> b,
    //                final Iterator<? extends C> c, final A valueForNoneA, final B valueForNoneB, final C valueForNoneC,
    //                final Throwables.TriFunction<? super A, ? super B, ? super C, T, RuntimeException> zipFunction) {
    //            return Seq.<A, B, C, T, RuntimeException> zip(a, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    //        }
    //
    //        public static <T> Seq<T, RuntimeException> merge(final T[] a, final T[] b,
    //                final Throwables.BiFunction<? super T, ? super T, MergeResult, RuntimeException> nextSelector) {
    //            return Seq.<T, RuntimeException> merge(a, b, nextSelector);
    //        }
    //
    //        public static <T> Seq<T, RuntimeException> merge(final T[] a, final T[] b, final T[] c,
    //                final Throwables.BiFunction<? super T, ? super T, MergeResult, RuntimeException> nextSelector) {
    //            return Seq.<T, RuntimeException> merge(a, b, c, nextSelector);
    //        }
    //
    //        public static <T> Seq<T, RuntimeException> merge(final Iterable<? extends T> a, final Iterable<? extends T> b,
    //                final Throwables.BiFunction<? super T, ? super T, MergeResult, RuntimeException> nextSelector) {
    //            return Seq.<T, RuntimeException> merge(a, b, nextSelector);
    //        }
    //
    //        public static <T> Seq<T, RuntimeException> merge(final Iterable<? extends T> a, final Iterable<? extends T> b,
    //                final Iterable<? extends T> c, final Throwables.BiFunction<? super T, ? super T, MergeResult, RuntimeException> nextSelector) {
    //            return Seq.<T, RuntimeException> merge(a, b, c, nextSelector);
    //        }
    //
    //        public static <T> Seq<T, RuntimeException> merge(final Iterator<? extends T> a, final Iterator<? extends T> b,
    //                final Throwables.BiFunction<? super T, ? super T, MergeResult, RuntimeException> nextSelector) {
    //            return Seq.<T, RuntimeException> merge(a, b, nextSelector);
    //        }
    //
    //        public static <T> Seq<T, RuntimeException> merge(final Iterator<? extends T> a, final Iterator<? extends T> b,
    //                final Iterator<? extends T> c, final Throwables.BiFunction<? super T, ? super T, MergeResult, RuntimeException> nextSelector) {
    //            return Seq.<T, RuntimeException> merge(a, b, c, nextSelector);
    //        }
    //    }
}
