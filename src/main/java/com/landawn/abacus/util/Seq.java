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
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Serial;
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
import java.util.Enumeration;
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.regex.Pattern;
import java.util.stream.Collector;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.IntermediateOp;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.annotation.LazyEvaluation;
import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.annotation.TerminalOp;
import com.landawn.abacus.annotation.TerminalOpTriggered;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.exception.UncheckedIOException;
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
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.stream.Collectors;
import com.landawn.abacus.util.stream.IntStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 * The sequence class is an abstract class that represents a sequence of elements and supports different kinds of computations.
 * The sequence operations are divided into intermediate and terminal operations, and are combined to form sequence pipelines.
 *
 * <p>The sequence will be automatically closed after a terminal method is called/triggered</p>
 *
 * <p>{@code Seq} can be easily transformed to {@code Stream} by the {@code transformB} methods</p>
 * <p>Refer to {@code com.landawn.abacus.util.stream.BaseStream} and {@code com.landawn.abacus.util.stream.Stream} for more APIs docs.</p>
 *
 * @param <T> the type of the elements in this stream
 * @param <E> the type of the checked exception this sequence can throw
 *
 * @see com.landawn.abacus.util.stream.BaseStream
 * @see com.landawn.abacus.util.stream.EntryStream
 * @see com.landawn.abacus.util.stream.IntStream
 * @see com.landawn.abacus.util.stream.LongStream
 * @see com.landawn.abacus.util.stream.DoubleStream
 * @see com.landawn.abacus.util.stream.Collectors
 * @see com.landawn.abacus.util.Fn
 * @see com.landawn.abacus.util.Comparators
 * @see com.landawn.abacus.util.ExceptionUtil
 * @see java.util.stream.Stream
 *
 */
@Beta
@LazyEvaluation
@SequentialOnly
@com.landawn.abacus.annotation.Immutable
@SuppressWarnings({ "java:S1192", "java:S1698", "java:S4968", "java:S6539" })
public final class Seq<T, E extends Exception> implements AutoCloseable, Immutable {
    // There are a lot of methods commented out by purpose, because they are not necessary or not recommended to be used in Seq.
    // Additionally, a lot of methods defined Stream but not here can be supported through methods: transformB, sps, etc.
    // If you really need them, please use Stream instead.
    // BE CAUTION ON UN-COMMENTING THEM.
    // Please refer to the latest implementation/doc in Stream for any un-commended methods.

    // Tested performance with below code. It seems there is no meaningful performance improvement brought by Seq, comparing with Stream.
    // Remove the Seq for now???
    //    @Test
    //    public void test_try_catch_perf() {
    //        final int len = 1000_000;
    //        final int loopNum = 100;
    //
    //        Profiler.run(1, loopNum, 3, "noTryCatch", () -> {
    //            final long count = Stream.range(0, len).map(it -> notThrowSQLException()).count();
    //
    //            assertEquals(len, count);
    //        }).printResult();
    //
    //        Profiler.run(1, loopNum, 3, "cmdWithTryCatch", () -> {
    //            final long count = Stream.range(0, len).map(Fn.ff(it -> maybeThrowSQLException())).count();
    //
    //            assertEquals(len, count);
    //        }).printResult();
    //
    //        Profiler.run(1, loopNum, 3, "cmdBySeq", () -> {
    //            try {
    //                final long count = Seq.<SQLException> range(0, len).map(it -> maybeThrowSQLException()).count();
    //                assertEquals(len, count);
    //            } catch (final SQLException e) {
    //                throw ExceptionUtil.toRuntimeException(e, true);
    //            }
    //        }).printResult();
    //
    //    }
    //
    //    @SuppressWarnings("unused")
    //    String maybeThrowSQLException() throws SQLException {
    //        return "abc"; // Strings.uuid();
    //    }
    //
    //    String notThrowSQLException() {
    //        return "abc"; // Strings.uuid();
    //    }
    private static final Logger logger = LoggerFactory.getLogger(Seq.class);

    private static final int BATCH_SIZE_FOR_FLUSH = 200;

    private static final Throwables.Function<OptionalInt, Integer, RuntimeException> GET_AS_INT = OptionalInt::get;

    private static final Throwables.Function<OptionalLong, Long, RuntimeException> GET_AS_LONG = OptionalLong::get;

    private static final Throwables.Function<OptionalDouble, Double, RuntimeException> GET_AS_DOUBLE = OptionalDouble::get;

    @SuppressWarnings("rawtypes")
    private static final Throwables.Function<Optional, Object, RuntimeException> GET_AS_IT = it -> it.orElse(null);

    //    @SuppressWarnings("rawtypes")
    //    private static final Throwables.Function<java.util.Optional, Object, RuntimeException> GET_AS_IT_JDK = it -> it.orElse(null);

    private static final Throwables.Predicate<OptionalInt, RuntimeException> IS_PRESENT_INT = OptionalInt::isPresent;

    private static final Throwables.Predicate<OptionalLong, RuntimeException> IS_PRESENT_LONG = OptionalLong::isPresent;

    private static final Throwables.Predicate<OptionalDouble, RuntimeException> IS_PRESENT_DOUBLE = OptionalDouble::isPresent;

    @SuppressWarnings("rawtypes")
    private static final Throwables.Predicate<Optional, RuntimeException> IS_PRESENT_IT = Optional::isPresent;

    // private static final Throwables.Function<Map.Entry<Keyed<Object, Object>, Object>, Object, Exception> KK = t -> t.getKey().val();

    private static final Object NONE = ClassUtil.createNullMask();

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

    Seq(final Throwables.Iterator<? extends T, ? extends E> iter) {
        this(iter, false, null, null);
    }

    Seq(final Throwables.Iterator<? extends T, ? extends E> iter, final Collection<LocalRunnable> closeHandlers) {
        this(iter, false, null, closeHandlers);
    }

    Seq(final Throwables.Iterator<? extends T, ? extends E> iter, final boolean sorted, final Comparator<? super T> cmp,
            final Collection<LocalRunnable> closeHandlers) {
        elements = (Throwables.Iterator<T, E>) iter;
        this.sorted = sorted;
        this.cmp = cmp;
        this.closeHandlers = isEmptyCloseHandlers(closeHandlers) ? null
                : (closeHandlers instanceof LocalArrayDeque ? (LocalArrayDeque<LocalRunnable>) closeHandlers : new LocalArrayDeque<>(closeHandlers));

    }

    //    @SuppressWarnings("rawtypes")
    //    private static final Seq EMPTY_SEQ = new Seq(Throwables.Iterator.empty());

    /**
     * Returns an empty sequence.
     *
     * @param <T> the type of the sequence elements
     * @param <E> the type of the exception that the sequence can throw
     * @return an empty sequence
     */
    public static <T, E extends Exception> Seq<T, E> empty() {
        return new Seq<>(Throwables.Iterator.empty());
    }

    //    /**
    //     * Creates a Seq from a given Stream.
    //     *
    //     * @param <T> the type of elements in the stream
    //     * @param <E> the type of exception that might be thrown
    //     * @param stream where the returned Seq will load elements from.
    //     * @return a Seq containing the elements of the provided Stream
    //     */
    //    public static <T, E extends Exception> Seq<T, E> from(final Stream<? extends T> stream) {
    //        return create(stream, false);
    //    }
    //
    //    /**
    //     * Creates a Seq from a given Stream.
    //     *
    //     * @param <T> the type of elements in the stream
    //     * @param <E> the type of exception that might be thrown
    //     * @param stream where the returned Seq will load elements from.
    //     * @return a Seq containing the elements of the provided Stream
    //     */
    //    public static <T, E extends Exception> Seq<T, E> from(final java.util.stream.Stream<? extends T> stream) {
    //        if (stream == null) {
    //            return empty();
    //        }
    //
    //        return Seq.<T, E> of(stream.iterator()).onClose(stream::close);
    //    }
    //
    //    /**
    //     * Creates a Seq from a given Stream.
    //     *
    //     * @param <T> the type of elements in the stream
    //     * @param <E> the type of exception that might be thrown
    //     * @param stream where the returned Seq will load elements from.
    //     * @param exceptionType the type of exception that might be thrown
    //     * @return a Seq containing the elements of the provided Stream
    //     */
    //    // Should the name be from?
    //    public static <T, E extends Exception> Seq<T, E> from(final Stream<? extends T> stream,
    //            @SuppressWarnings("unused") final Class<E> exceptionType) { //NOSONAR
    //        return from(stream);
    //    }
    //
    //    /**
    //     * Creates a Seq from a given Stream.
    //     *
    //     * @param <T> the type of elements in the stream
    //     * @param <E> the type of exception that might be thrown
    //     * @param stream where the returned Seq will load elements from.
    //     * @param exceptionType the type of exception that might be thrown
    //     * @return a Seq containing the elements of the provided Stream
    //     */
    //    // Should the name be from?
    //    public static <T, E extends Exception> Seq<T, E> from(final java.util.stream.Stream<? extends T> stream,
    //            @SuppressWarnings("unused") final Class<E> exceptionType) { //NOSONAR
    //        return from(stream);
    //    }

    //    /**
    //     * Creates a Seq from a given Stream.
    //     *
    //     * @param <T> the type of elements in the stream
    //     * @param <E> the type of exception that might be thrown
    //     * @param stream the Stream from which the Seq will be created
    //     * @return a Seq containing the elements of the provided Stream
    //     * @deprecated Use {@link #from(Stream)} instead
    //     * @see #from(Stream)
    //     */
    //    @Deprecated
    //    public static <T, E extends Exception> Seq<T, E> of(final Stream<? extends T> stream) {
    //        return from(stream);
    //    }
    //
    //    /**
    //     * Creates a Seq from a given Stream.
    //     *
    //     * @param <T> the type of elements in the stream
    //     * @param <E> the type of exception that might be thrown
    //     * @param stream the Stream from which the Seq will be created
    //     * @return a Seq containing the elements of the provided Stream
    //     * @deprecated Use {@link #from(java.util.stream.Stream)} instead
    //     * @see #from(java.util.stream.Stream)
    //     */
    //    @Deprecated
    //    public static <T, E extends Exception> Seq<T, E> of(final java.util.stream.Stream<? extends T> stream) {
    //        return from(stream);
    //    }
    //
    //    /**
    //     * Creates a Seq from the provided Stream.
    //     *
    //     * @param <T> the type of elements in the stream
    //     * @param <E> the type of exception that might be thrown
    //     * @param stream the Stream from which the Seq will be created
    //     * @param exceptionType the type of exception that might be thrown (not used)
    //     * @return a Seq containing the elements of the provided Stream
    //     * @deprecated Use {@link #from(Stream, Class)} instead
    //     * @see #from(Stream, Class)
    //     */
    //    // Should the name be from?
    //    @Deprecated
    //    public static <T, E extends Exception> Seq<T, E> of(final Stream<? extends T> stream, @SuppressWarnings("unused") final Class<E> exceptionType) {
    //        return from(stream, exceptionType);
    //    }
    //
    //    /**
    //     * Creates a Seq from a given java.util.stream.Stream.
    //     *
    //     * @param <T> the type of elements in the stream
    //     * @param <E> the type of exception that might be thrown
    //     * @param stream the java.util.stream.Stream from which the Seq will be created
    //     * @param exceptionType the type of exception that might be thrown (not used)
    //     * @return a Seq containing the elements of the provided java.util.stream.Stream
    //     * @deprecated Use {@link #from(java.util.stream.Stream, Class)} instead
    //     * @see #from(java.util.stream.Stream, Class)
    //     */
    //    // Should the name be from?
    //    @Deprecated
    //    public static <T, E extends Exception> Seq<T, E> of(final java.util.stream.Stream<? extends T> stream,
    //            @SuppressWarnings("unused") final Class<E> exceptionType) {
    //        return from(stream, exceptionType);
    //    }

    /**
     * Returns a sequence that is lazily populated by an input supplier.
     *
     * <br />
     * @implNote
     * This is equal to: {@code Seq.just(supplier).flatMap(Throwables.Supplier::get)}.
     *
     * @param <T> the type of the sequence elements
     * @param <E> the type of the exception that the sequence can throw
     * @param supplier the supplier to generate the sequence
     * @return a sequence generated by the supplier
     * @throws IllegalArgumentException if the supplier is null
     */
    public static <T, E extends Exception> Seq<T, E> defer(final Throwables.Supplier<? extends Seq<? extends T, ? extends E>, ? extends E> supplier)
            throws IllegalArgumentException {
        N.checkArgNotNull(supplier, cs.supplier);

        //noinspection resource
        return Seq.<Throwables.Supplier<? extends Seq<? extends T, ? extends E>, ? extends E>, E> just(supplier).flatMap(Throwables.Supplier::get);
    }

    /**
     * Creates a sequence containing a single element.
     *
     * @param <T> the type of the element
     * @param <E> the type of exception that might be thrown
     * @param e the element to be included in the sequence
     * @return a sequence containing the provided element
     */
    public static <T, E extends Exception> Seq<T, E> just(final T e) {
        return of(e);
    }

    /**
     * Creates a sequence containing a single element.
     *
     * @param <T> the type of the element
     * @param <E> the type of exception that might be thrown
     * @param e the element to be included in the sequence
     * @param exceptionType the type of exception that might be thrown
     * @return a sequence containing the provided element
     */
    public static <T, E extends Exception> Seq<T, E> just(final T e, @SuppressWarnings("unused") final Class<E> exceptionType) { //NOSONAR
        return of(e);
    }

    /**
     * Returns an empty sequence if the specified element is {@code null}, otherwise returns a sequence containing the single specified element.
     *
     * @param <T> the type of the element
     * @param <E> the type of exception that might be thrown
     * @param e the element to be included in the sequence if not null
     * @return a sequence containing the provided element if not {@code null}, otherwise an empty sequence
     */
    public static <T, E extends Exception> Seq<T, E> ofNullable(final T e) {
        if (e == null) {
            return empty();
        }

        return of(e);
    }

    /**
     * Returns an empty sequence if the specified element is {@code null}, otherwise returns a sequence containing the single specified element.
     *
     * @param <T> the type of the element
     * @param <E> the type of exception that might be thrown
     * @param e the element to be included in the sequence if not null
     * @param exceptionType the type of exception that might be thrown
     * @return a sequence containing the provided element if not {@code null}, otherwise an empty sequence
     */
    public static <T, E extends Exception> Seq<T, E> ofNullable(final T e, @SuppressWarnings("unused") final Class<E> exceptionType) { //NOSONAR
        if (e == null) {
            return empty();
        }

        return of(e);
    }

    /**
     * Creates a sequence from the provided array of elements.
     *
     * @param <T> the type of elements in the stream
     * @param <E> the type of exception that might be thrown
     * @param a the array of elements to be included in the sequence
     * @return a sequence containing the provided elements
     */
    @SafeVarargs
    public static <T, E extends Exception> Seq<T, E> of(final T... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return create(new Throwables.Iterator<>() {
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
            public void advance(final long n) throws E {
                if (n > len - position) {
                    position = len;
                } else {
                    position += (int) n;
                }
            }
        });
    }

    /**
     * Creates a sequence from the provided boolean array.
     *
     * @param <E> the type of exception that might be thrown
     * @param a the boolean array to be included in the sequence
     * @return a sequence containing the provided boolean elements
     */
    public static <E extends Exception> Seq<Boolean, E> of(final boolean[] a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return create(new Throwables.Iterator<>() {
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
            public void advance(final long n) throws E {
                if (n > len - position) {
                    position = len;
                } else {
                    position += (int) n;
                }
            }
        });
    }

    /**
     * Creates a sequence from the provided char array.
     *
     * @param <E> the type of exception that might be thrown
     * @param a the char array to be included in the sequence
     * @return a sequence containing the provided char elements
     */
    public static <E extends Exception> Seq<Character, E> of(final char[] a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return create(new Throwables.Iterator<>() {
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
            public void advance(final long n) throws E {
                if (n > len - position) {
                    position = len;
                } else {
                    position += (int) n;
                }
            }
        });
    }

    /**
     * Creates a sequence from the provided byte array.
     *
     * @param <E> the type of exception that might be thrown
     * @param a the byte array to be included in the sequence
     * @return a sequence containing the provided byte elements
     */
    public static <E extends Exception> Seq<Byte, E> of(final byte[] a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return create(new Throwables.Iterator<>() {
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
            public void advance(final long n) throws E {
                if (n > len - position) {
                    position = len;
                } else {
                    position += (int) n;
                }
            }
        });
    }

    /**
     * Creates a sequence from the provided short array.
     *
     * @param <E> the type of exception that might be thrown
     * @param a the short array to be included in the sequence
     * @return a sequence containing the provided short elements
     */
    public static <E extends Exception> Seq<Short, E> of(final short[] a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return create(new Throwables.Iterator<>() {
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
            public void advance(final long n) throws E {
                if (n > len - position) {
                    position = len;
                } else {
                    position += (int) n;
                }
            }
        });
    }

    /**
     * Creates a sequence from the provided int array.
     *
     * @param <E> the type of exception that might be thrown
     * @param a the int array to be included in the sequence
     * @return a sequence containing the provided int elements
     */
    public static <E extends Exception> Seq<Integer, E> of(final int[] a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return create(new Throwables.Iterator<>() {
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
            public void advance(final long n) throws E {
                if (n > len - position) {
                    position = len;
                } else {
                    position += (int) n;
                }
            }
        });
    }

    /**
     * Creates a sequence from the provided long array.
     *
     * @param <E> the type of exception that might be thrown
     * @param a the long array to be included in the sequence
     * @return a sequence containing the provided long elements
     */
    public static <E extends Exception> Seq<Long, E> of(final long[] a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return create(new Throwables.Iterator<>() {
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
            public void advance(final long n) throws E {
                if (n > len - position) {
                    position = len;
                } else {
                    position += (int) n;
                }
            }
        });
    }

    /**
     * Creates a sequence from the provided float array.
     *
     * @param <E> the type of exception that might be thrown
     * @param a the float array to be included in the sequence
     * @return a sequence containing the provided float elements
     */
    public static <E extends Exception> Seq<Float, E> of(final float[] a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return create(new Throwables.Iterator<>() {
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
            public void advance(final long n) throws E {
                if (n > len - position) {
                    position = len;
                } else {
                    position += (int) n;
                }
            }
        });
    }

    /**
     * Creates a sequence from the provided double array.
     *
     * @param <E> the type of exception that might be thrown
     * @param a the double array to be included in the sequence
     * @return a sequence containing the provided double elements
     */
    public static <E extends Exception> Seq<Double, E> of(final double[] a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return create(new Throwables.Iterator<>() {
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
            public void advance(final long n) throws E {
                if (n > len - position) {
                    position = len;
                } else {
                    position += (int) n;
                }
            }
        });
    }

    /**
     * Returns a sequence containing the value of the specified Optional if it is present,
     * otherwise returns an empty sequence.
     *
     * @param <T> the type of the sequence elements
     * @param <E> the type of the exception that the sequence can throw
     * @param op the Optional whose value is to be returned as a sequence
     * @return a sequence containing the value of the specified Optional if it is present,
     *         otherwise an empty sequence
     */
    public static <T, E extends Exception> Seq<T, E> of(final Optional<T> op) {
        return op == null || op.isEmpty() ? Seq.empty() : Seq.of(op.get()); //NOSONAR
    }

    /**
     * Returns a sequence containing the value of the specified Optional if it is present,
     * otherwise returns an empty sequence.
     *
     * @param <T> the type of the sequence elements
     * @param <E> the type of the exception that the sequence can throw
     * @param op the Optional whose value is to be returned as a sequence
     * @return a sequence containing the value of the specified Optional if it is present,
     *         otherwise an empty sequence
     */
    public static <T, E extends Exception> Seq<T, E> of(final java.util.Optional<T> op) {
        return op == null || op.isEmpty() ? Seq.empty() : Seq.of(op.get()); //NOSONAR
    }

    /**
     * Creates a sequence from the provided Iterable of elements.
     *
     * @param <T> the type of elements in the stream
     * @param <E> the type of exception that might be thrown
     * @param iterable the Iterable of elements to be included in the sequence
     * @return a sequence containing the provided elements
     */
    public static <T, E extends Exception> Seq<T, E> of(final Iterable<? extends T> iterable) {
        if (iterable == null) {
            return empty();
        }

        return of(iterable.iterator());
    }

    /**
     * Creates a sequence from the provided Iterable of elements.
     *
     * @param <T> the type of elements in the stream
     * @param <E> the type of exception that might be thrown
     * @param iterable the Iterable of elements to be included in the sequence
     * @param exceptionType the type of exception that might be thrown (not used)
     * @return a sequence containing the provided elements
     */
    public static <T, E extends Exception> Seq<T, E> of(final Iterable<? extends T> iterable, @SuppressWarnings("unused") final Class<E> exceptionType) { //NOSONAR
        return of(iterable);
    }

    /**
     * Creates a sequence from the provided Iterator of elements.
     *
     * @param <T> the type of elements in the stream
     * @param <E> the type of exception that might be thrown
     * @param iter the Iterator of elements to be included in the sequence
     * @return a sequence containing the provided elements
     */
    public static <T, E extends Exception> Seq<T, E> of(final Iterator<? extends T> iter) {
        if (iter == null) {
            return empty();
        }

        return create(Throwables.Iterator.<T, E> of(iter));
    }

    /**
     * Creates a sequence from the provided Iterator of elements.
     *
     * @param <T> the type of elements in the stream
     * @param <E> the type of exception that might be thrown
     * @param iter the Iterator of elements to be included in the sequence
     * @return a sequence containing the provided elements
     */
    public static <T, E extends Exception> Seq<T, E> of(final Throwables.Iterator<? extends T, ? extends E> iter) {
        if (iter == null) {
            return empty();
        }

        return create(iter);
    }

    /**
     * Creates a sequence from the provided Iterator of elements.
     *
     * @param <T> the type of elements in the stream
     * @param <E> the type of exception that might be thrown
     * @param iter the Iterator of elements to be included in the sequence
     * @param exceptionType the type of exception that might be thrown (not used)
     * @return a sequence containing the provided elements
     */
    public static <T, E extends Exception> Seq<T, E> of(final Iterator<? extends T> iter, @SuppressWarnings("unused") final Class<E> exceptionType) { //NOSONAR
        return of(iter);
    }

    /**
     * Creates a sequence from the provided Enumeration of elements.
     *
     * @param <T> the type of elements in the stream
     * @param <E> the type of exception that might be thrown
     * @param enumeration the Enumeration of elements to be included in the sequence
     * @return a sequence containing the provided elements
     */
    public static <T, E extends Exception> Seq<T, E> of(final Enumeration<? extends T> enumeration) {
        if (enumeration == null) {
            return empty();
        }

        return of(Enumerations.toIterator(enumeration));
    }

    /**
     * Creates a sequence from the provided Enumeration of elements.
     *
     * @param <T> the type of elements in the stream
     * @param <E> the type of exception that might be thrown
     * @param enumeration the Enumeration of elements to be included in the sequence
     * @param exceptionType the type of exception that might be thrown (not used)
     * @return a sequence containing the provided elements
     */
    public static <T, E extends Exception> Seq<T, E> of(final Enumeration<? extends T> enumeration, @SuppressWarnings("unused") final Class<E> exceptionType) { //NOSONAR
        return of(enumeration);
    }

    /**
     * Creates a sequence from the provided Map of entries.
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param <E> the type of exception that might be thrown
     * @param m the Map of entries to be included in the sequence
     * @return a sequence containing the provided map entries
     */
    public static <K, V, E extends Exception> Seq<Map.Entry<K, V>, E> of(final Map<K, V> m) {
        if (N.isEmpty(m)) {
            return empty();
        }

        return of(m.entrySet());
    }

    /**
     * Creates a sequence from the provided Map of entries.
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param <E> the type of exception that might be thrown
     * @param m the Map of entries to be included in the sequence
     * @param exceptionType the type of exception that might be thrown (not used)
     * @return a sequence containing the provided map entries
     */
    public static <K, V, E extends Exception> Seq<Map.Entry<K, V>, E> of(final Map<K, V> m, @SuppressWarnings("unused") final Class<E> exceptionType) { //NOSONAR
        return of(m);
    }

    /**
     * Creates a sequence from the keys of the provided Map.
     *
     * @param <K> the type of keys in the map
     * @param <E> the type of exception that might be thrown
     * @param map the Map whose keys are to be included in the sequence
     * @return a sequence containing the keys of the provided Map
     */
    public static <K, E extends Exception> Seq<K, E> ofKeys(final Map<K, ?> map) {
        if (N.isEmpty(map)) {
            return empty();
        }

        return of(map.keySet());
    }

    /**
     * Creates a sequence from the keys of the provided Map, filtered by the specified value predicate.
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param <E> the type of exception that might be thrown
     * @param map the Map whose keys are to be included in the sequence
     * @param valueFilter the predicate to filter the values in the map
     * @return a sequence containing the keys of the provided Map that match the value predicate
     */
    public static <K, V, E extends Exception> Seq<K, E> ofKeys(final Map<K, V> map, final Throwables.Predicate<? super V, E> valueFilter) {
        if (N.isEmpty(map)) {
            return empty();
        }

        //noinspection resource
        return Seq.<K, V, E> of(map).filter(Fnn.testByValue(valueFilter)).map(Fnn.key());
    }

    /**
     * Creates a sequence from the keys of the provided Map, filtered by the specified key-value predicate.
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param <E> the type of exception that might be thrown
     * @param map the Map whose keys are to be included in the sequence
     * @param filter the predicate to filter the entries in the map
     * @return a sequence containing the keys of the provided Map that match the key-value predicate
     */
    public static <K, V, E extends Exception> Seq<K, E> ofKeys(final Map<K, V> map, final Throwables.BiPredicate<? super K, ? super V, E> filter) {
        if (N.isEmpty(map)) {
            return empty();
        }

        //noinspection resource
        return Seq.<K, V, E> of(map).filter(Fn.Entries.ep(filter)).map(Fnn.key());
    }

    /**
     * Creates a sequence from the values of the provided Map.
     *
     * @param <V> the type of values in the map
     * @param <E> the type of exception that might be thrown
     * @param map the Map whose values are to be included in the sequence
     * @return a sequence containing the values of the provided
     */
    public static <V, E extends Exception> Seq<V, E> ofValues(final Map<?, V> map) {
        if (N.isEmpty(map)) {
            return empty();
        }

        return of(map.values());
    }

    /**
     * Creates a sequence from the values of the provided Map, filtered by the specified key predicate.
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param <E> the type of exception that might be thrown
     * @param map the Map whose values are to be included in the sequence
     * @param keyFilter the predicate to filter the keys in the map
     * @return a sequence containing the values of the provided Map that match the key predicate
     */
    public static <K, V, E extends Exception> Seq<V, E> ofValues(final Map<K, V> map, final Throwables.Predicate<? super K, E> keyFilter) {
        if (N.isEmpty(map)) {
            return empty();
        }

        //noinspection resource
        return Seq.<K, V, E> of(map).filter(Fnn.testByKey(keyFilter)).map(Fnn.value());
    }

    /**
     * Creates a sequence from the values of the provided Map, filtered by the specified key-value predicate.
     *
     * @param <K> the type of keys in the map
     * @param <V> the type of values in the map
     * @param <E> the type of exception that might be thrown
     * @param map the Map whose values are to be included in the sequence
     * @param filter the predicate to filter the entries in the map
     * @return a sequence containing the values of the provided Map that match the key-value predicate
     */
    public static <K, V, E extends Exception> Seq<V, E> ofValues(final Map<K, V> map, final Throwables.BiPredicate<? super K, ? super V, E> filter) {
        if (N.isEmpty(map)) {
            return empty();
        }

        //noinspection resource
        return Seq.<K, V, E> of(map).filter(Fn.Entries.ep(filter)).map(Fnn.value());
    }

    /**
     * Creates a sequence from the provided array in reverse order.
     *
     * @param <T> the type of elements in the array
     * @param <E> the type of exception that might be thrown
     * @param array the array of elements to be included in the sequence
     * @return a sequence containing the elements of the provided array in reverse order
     */
    public static <T, E extends Exception> Seq<T, E> ofReversed(final T[] array) {
        final int len = N.len(array);

        //noinspection resource
        return Seq.<E> range(0, len).map(idx -> array[len - idx - 1]);
    }

    /**
     * Creates a sequence from the provided list in reverse order.
     *
     * @param <T> the type of elements in the list
     * @param <E> the type of exception that might be thrown
     * @param list the list of elements to be included in the sequence
     * @return a sequence containing the elements of the provided list in reverse order
     */
    public static <T, E extends Exception> Seq<T, E> ofReversed(final List<? extends T> list) {
        final int size = N.size(list);

        //noinspection resource
        return Seq.<E> range(0, size).map(idx -> list.get(size - idx - 1));
    }

    //    /**
    //     * Creates a Seq that iterates using the provided hasNext and next functions.
    //     *
    //     * @param <T> the type of elements in the stream
    //     * @param <E> the type of exception that might be thrown
    //     * @param hasNext a BooleanSupplier that determines if there are more elements
    //     * @param next a Supplier that provides the next element
    //     * @return a Seq that iterates using the provided hasNext and next functions
    //     * @throws IllegalArgumentException if hasNext or next is null
    //     */
    //    public static <T, E extends Exception> Seq<T, E> iterate(final Throwables.BooleanSupplier<? extends E> hasNext,
    //            final Throwables.Supplier<? extends T, E> next) throws IllegalArgumentException {
    //        N.checkArgNotNull(hasNext, cs.hasNext);
    //        N.checkArgNotNull(next, cs.next);
    //
    //        return create(new Throwables.Iterator<T, E>() {
    //            private boolean hasNextVal = false;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                if (!hasNextVal) {
    //                    hasNextVal = hasNext.getAsBoolean();
    //                }
    //
    //                return hasNextVal;
    //            }
    //
    //            @Override
    //            public T next() throws E {
    //                if (!hasNextVal && !hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                hasNextVal = false;
    //                return next.get();
    //            }
    //        });
    //    }
    //
    //    /**
    //     * Creates a Seq that iterates starting from the given initial value,
    //     * using the provided hasNext function to determine if there are more elements
    //     * and the provided function to generate the next element.
    //     *
    //     * @param <T> the type of elements in the stream
    //     * @param <E> the type of exception that might be thrown
    //     * @param init the initial value
    //     * @param hasNext a BooleanSupplier that determines if there are more elements
    //     * @param f a UnaryOperator that provides the next element
    //     * @return a Seq that iterates using the provided hasNext and next functions
    //     * @throws IllegalArgumentException if hasNext or f is null
    //     */
    //    public static <T, E extends Exception> Seq<T, E> iterate(final T init, final Throwables.BooleanSupplier<? extends E> hasNext,
    //            final Throwables.UnaryOperator<T, ? extends E> f) throws IllegalArgumentException {
    //        N.checkArgNotNull(hasNext, cs.hasNext);
    //        N.checkArgNotNull(f, cs.f);
    //
    //        return create(new Throwables.Iterator<T, E>() {
    //            private T cur = (T) NONE;
    //            private boolean hasNextVal = false;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                if (!hasNextVal) {
    //                    hasNextVal = hasNext.getAsBoolean();
    //                }
    //
    //                return hasNextVal;
    //            }
    //
    //            @Override
    //            public T next() throws E {
    //                if (!hasNextVal && !hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                hasNextVal = false;
    //                return cur = (cur == NONE ? init : f.apply(cur));
    //            }
    //        });
    //    }
    //
    //    /**
    //     * Creates a Seq that iterates starting from the given initial value,
    //     * using the provided hasNext function to determine if there are more elements
    //     * and the provided function to generate the next element.
    //     *
    //     * @param <T> the type of elements in the stream
    //     * @param <E> the type of exception that might be thrown
    //     * @param init the initial value
    //     * @param hasNext a Predicate that determines if there are more elements
    //     * @param f a UnaryOperator that provides the next element
    //     * @return a Seq that iterates using the provided hasNext and next functions
    //     * @throws IllegalArgumentException if hasNext or f is null
    //     */
    //    public static <T, E extends Exception> Seq<T, E> iterate(final T init, final Throwables.Predicate<? super T, ? extends E> hasNext,
    //            final Throwables.UnaryOperator<T, ? extends E> f) throws IllegalArgumentException {
    //        N.checkArgNotNull(hasNext, cs.hasNext);
    //        N.checkArgNotNull(f, cs.f);
    //
    //        return create(new Throwables.Iterator<T, E>() {
    //            private T cur = (T) NONE;
    //            private boolean hasMore = true;
    //            private boolean hasNextVal = false;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                if (!hasNextVal && hasMore) {
    //                    hasNextVal = hasNext.test((cur = (cur == NONE ? init : f.apply(cur))));
    //
    //                    if (!hasNextVal) {
    //                        hasMore = false;
    //                    }
    //                }
    //
    //                return hasNextVal;
    //            }
    //
    //            @Override
    //            public T next() throws E {
    //                if (!hasNextVal && !hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                hasNextVal = false;
    //                return cur;
    //            }
    //        });
    //    }
    //
    //    /**
    //     * Creates a Seq that iterates starting from the given initial value,
    //     * using the provided function to generate the next element.
    //     *
    //     * @param <T> the type of elements in the stream
    //     * @param <E> the type of exception that might be thrown
    //     * @param init the initial value
    //     * @param f a UnaryOperator that provides the next element
    //     * @return a Seq that iterates using the provided function
    //     * @throws IllegalArgumentException if f is null
    //     */
    //    public static <T, E extends Exception> Seq<T, E> iterate(final T init, final Throwables.UnaryOperator<T, ? extends E> f) throws IllegalArgumentException {
    //        N.checkArgNotNull(f, cs.f);
    //
    //        return create(new Throwables.Iterator<T, E>() {
    //            private T cur = (T) NONE;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                return true;
    //            }
    //
    //            @Override
    //            public T next() throws E { // NOSONAR
    //                return cur = (cur == NONE ? init : f.apply(cur));
    //            }
    //        });
    //    }
    //
    //    /**
    //     * Generates a Seq using the provided supplier.
    //     *
    //     * @param <T> the type of the stream elements
    //     * @param <E> the type of the exception that the stream can throw
    //     * @param supplier the supplier to generate elements for the stream
    //     * @return a Seq generated by the supplier
    //     * @throws IllegalArgumentException if the supplier is null
    //     */
    //    public static <T, E extends Exception> Seq<T, E> generate(final Throwables.Supplier<T, E> supplier) throws IllegalArgumentException {
    //        N.checkArgNotNull(supplier, cs.supplier);
    //
    //        return create(new Throwables.Iterator<T, E>() {
    //            @Override
    //            public boolean hasNext() throws E {
    //                return true;
    //            }
    //
    //            @Override
    //            public T next() throws E {
    //                return supplier.get();
    //            }
    //        });
    //    }

    /**
     * Creates a sequence that repeats the given element a specified number of times.
     *
     * @param <T> the type of the element
     * @param <E> the type of exception that might be thrown
     * @param element the element to be repeated
     * @param n the number of times to repeat the element
     * @return a sequence that repeats the given element the specified number of times
     * @throws IllegalArgumentException if n is negative
     */
    public static <T, E extends Exception> Seq<T, E> repeat(final T element, final long n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return empty();
        }

        return create(new Throwables.Iterator<>() {
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
     * Returns a sequence that contains a range of integers from startInclusive (inclusive) to endExclusive (exclusive).
     *
     * @param <E> the type of exception that might be thrown
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @return a sequence containing the range of integers
     */
    @SuppressWarnings("deprecation")
    public static <E extends Exception> Seq<Integer, E> range(final int startInclusive, final int endExclusive) {
        //noinspection resource
        return of(IntStream.range(startInclusive, endExclusive).boxed().iterator());
    }

    /**
     * Returns a sequence that contains a range of integers from startInclusive (inclusive) to endExclusive (exclusive),
     * incremented by the specified step.
     *
     * @param <E> the type of exception that might be thrown
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @param by the step value for incrementing, it can be negative but not zero
     * @return a sequence containing the range of integers
     */
    @SuppressWarnings("deprecation")
    public static <E extends Exception> Seq<Integer, E> range(final int startInclusive, final int endExclusive, final int by) {
        //noinspection resource
        return of(IntStream.range(startInclusive, endExclusive, by).boxed().iterator());
    }

    /**
     * Returns a sequence that contains a range of integers from startInclusive (inclusive) to endInclusive (inclusive).
     *
     * @param <E> the type of exception that might be thrown
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @return a sequence containing the range of integers
     */
    @SuppressWarnings("deprecation")
    public static <E extends Exception> Seq<Integer, E> rangeClosed(final int startInclusive, final int endInclusive) {
        //noinspection resource
        return of(IntStream.rangeClosed(startInclusive, endInclusive).boxed().iterator());
    }

    /**
     * Returns a sequence that contains a range of integers from startInclusive (inclusive) to endInclusive (inclusive),
     * incremented by the specified step.
     *
     * @param <E> the type of the exception that might be thrown
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (exclusive)
     * @param by the step value for incrementing, it can be negative but not zero
     * @return a sequence containing the range of integers
     */
    @SuppressWarnings("deprecation")
    public static <E extends Exception> Seq<Integer, E> rangeClosed(final int startInclusive, final int endInclusive, final int by) {
        //noinspection resource
        return of(IntStream.rangeClosed(startInclusive, endInclusive, by).boxed().iterator());
    }

    /**
     * Splits the given character sequence into a sequence of strings based on the specified delimiter.
     *
     * @param <E> the type of the exception that might be thrown
     * @param str the character sequence to be split
     * @param delimiter the delimiter used to split the character sequence
     * @return a sequence of strings resulting from the split operation
     */
    public static <E extends Exception> Seq<String, E> split(final CharSequence str, final char delimiter) {
        return of(Splitter.with(delimiter).iterate(str));
    }

    /**
     * Splits the given character sequence into a sequence of strings based on the specified delimiter.
     *
     * @param <E> the type of the exception that might be thrown
     * @param str the character sequence to be split
     * @param delimiter the delimiter used to split the character sequence
     * @return a sequence of strings resulting from the split operation
     */
    public static <E extends Exception> Seq<String, E> split(final CharSequence str, final CharSequence delimiter) {
        return of(Splitter.with(delimiter).iterate(str));
    }

    /**
     * Splits the given character sequence into a sequence of strings with the specified {@code pattern}.
     *
     * @param <E> the type of the exception that might be thrown
     * @param str the character sequence to be split
     * @param pattern the pattern used to split the character sequence
     * @return a sequence of strings resulting from the split operation
     */
    public static <E extends Exception> Seq<String, E> split(final CharSequence str, final Pattern pattern) {
        return of(Splitter.with(pattern).iterate(str));
    }

    private static final Splitter lineSplitter = Splitter.forLines();
    private static final Splitter trimLineSplitter = Splitter.forLines().trimResults();
    private static final Splitter omitEmptyLinesLineSplitter = Splitter.forLines().omitEmptyStrings();
    private static final Splitter trimAndOmitEmptyLinesLineSplitter = Splitter.forLines().trimResults().omitEmptyStrings();

    /**
     * Splits the given string into a sequence of lines.
     *
     * @param <E> the type of the exception that might be thrown
     * @param str the string to be split into lines
     * @return a sequence of strings, each representing a line from the input string
     */
    public static <E extends Exception> Seq<String, E> splitToLines(final String str) {
        return of(lineSplitter.iterate(str));
    }

    /**
     * Splits the given string into a sequence of lines with optional trimming and omission of empty lines.
     *
     * @param <E> the type of the exception that might be thrown
     * @param str the string to be split into lines
     * @param trim whether to trim the lines
     * @param omitEmptyLines whether to omit empty lines
     * @return a sequence of strings, each representing a line from the input string
     */
    public static <E extends Exception> Seq<String, E> splitToLines(final String str, final boolean trim, final boolean omitEmptyLines) {
        if (trim) {
            if (omitEmptyLines) {
                return of(trimAndOmitEmptyLinesLineSplitter.iterate(str));
            } else {
                return of(trimLineSplitter.iterate(str));
            }
        } else if (omitEmptyLines) {
            return of(omitEmptyLinesLineSplitter.iterate(str));
        } else {
            return of(lineSplitter.iterate(str));
        }
    }

    /**
     * Splits the total size into chunks based on the specified maximum chunk count.
     * <br />
     * The size of the chunks is larger first.
     * <br />
     * The length of returned sequence may be less than the specified {@code maxChunkCount} if the input {@code totalSize} is less than {@code maxChunkCount}.
     *
     * @param <T> the type of the elements in the resulting stream
     * @param <E> the type of the exception that might be thrown
     * @param totalSize the total size to be split. It could be the size of an array, list, etc.
     * @param maxChunkCount the maximum number of chunks to split into
     * @param mapper a function to map the chunk from and to index to an element in the resulting stream
     * @return a sequence of the mapped chunk values
     * @throws IllegalArgumentException if {@code totalSize} is negative or {@code maxChunkCount} is not positive.
     * @see #splitByChunkCount(int, int, boolean, Throwables.IntBiFunction)
     */
    public static <T, E extends Exception> Seq<T, E> splitByChunkCount(final int totalSize, final int maxChunkCount,
            final Throwables.IntBiFunction<? extends T, ? extends E> mapper) {
        return splitByChunkCount(totalSize, maxChunkCount, false, mapper);
    }

    /**
     * Splits the total size into chunks based on the specified maximum chunk count.
     * <br />
     * The size of the chunks can be either smaller or larger first based on the flag.
     * <br />
     * The length of returned sequence may be less than the specified {@code maxChunkCount} if the input {@code totalSize} is less than {@code maxChunkCount}.
     *
     * <pre>
     * <code>
     * final int[] a = Array.rangeClosed(1, 7);
     * splitByChunkCount(7, 5, true, (fromIndex, toIndex) ->  copyOfRange(a, fromIndex, toIndex)); // [[1], [2], [3], [4, 5], [6, 7]]
     * splitByChunkCount(7, 5, false, (fromIndex, toIndex) ->  copyOfRange(a, fromIndex, toIndex)); // [[1, 2], [3, 4], [5], [6], [7]]
     * </code>
     * </pre>
     *
     * @param <T> the type of the elements in the resulting stream
     * @param <E> the type of the exception that might be thrown
     * @param totalSize the total size to be split. It could be the size of an array, list, etc.
     * @param maxChunkCount the maximum number of chunks to split into
     * @param sizeSmallerFirst if {@code true}, smaller chunks will be created first; otherwise, larger chunks will be created first
     * @param mapper a function to map the chunk from and to index to an element in the resulting stream
     * @return a sequence of the mapped chunk values
     * @throws IllegalArgumentException if {@code totalSize} is negative or {@code maxChunkCount} is not positive.
     * @see Stream#splitByChunkCount(int, int, boolean, IntBiFunction)
     */
    public static <T, E extends Exception> Seq<T, E> splitByChunkCount(final int totalSize, final int maxChunkCount, final boolean sizeSmallerFirst,
            final Throwables.IntBiFunction<? extends T, ? extends E> mapper) {
        N.checkArgNotNegative(totalSize, cs.totalSize);
        N.checkArgPositive(maxChunkCount, cs.maxChunkCount);

        if (totalSize == 0) {
            return Seq.empty();
        }

        final int count = Math.min(totalSize, maxChunkCount);
        final int biggerSize = totalSize % maxChunkCount == 0 ? totalSize / maxChunkCount : totalSize / maxChunkCount + 1;
        final int biggerCount = totalSize % maxChunkCount;
        final int smallerSize = Math.max(totalSize / maxChunkCount, 1);
        final int smallerCount = count - biggerCount;

        Throwables.Iterator<T, E> iter = null;

        if (sizeSmallerFirst) {
            iter = new Throwables.Iterator<>() {
                private int cnt = 0;
                private int cursor = 0;

                @Override
                public boolean hasNext() {
                    return cursor < totalSize;
                }

                @Override
                public T next() throws E {
                    if (cursor >= totalSize) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    return mapper.apply(cursor, cursor = (cnt++ < smallerCount ? cursor + smallerSize : cursor + biggerSize));
                }

                @Override
                public void advance(long n) throws IllegalArgumentException {
                    if (n > 0) {
                        while (n-- > 0 && cursor < totalSize) {
                            cursor = cnt++ < smallerCount ? cursor + smallerSize : cursor + biggerSize;
                        }
                    }
                }

                @Override
                public long count() {
                    return count;
                }
            };
        } else {
            iter = new Throwables.Iterator<>() {
                private int cnt = 0;
                private int cursor = 0;

                @Override
                public boolean hasNext() {
                    return cursor < totalSize;
                }

                @Override
                public T next() throws E {
                    if (cursor >= totalSize) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    return mapper.apply(cursor, cursor = (cnt++ < biggerCount ? cursor + biggerSize : cursor + smallerSize));
                }

                @Override
                public void advance(long n) throws IllegalArgumentException {
                    if (n > 0) {
                        while (n-- > 0 && cursor < totalSize) {
                            cursor = cnt++ < biggerCount ? cursor + biggerSize : cursor + smallerSize;
                        }
                    }
                }

                @Override
                public long count() {
                    return count;
                }
            };
        }

        return Seq.of(iter);
    }

    /**
     * Creates a sequence that reads lines from the specified file using the default charset.
     *
     * @param file the file to read lines from
     * @return a sequence containing the lines of the file
     */
    public static Seq<String, IOException> ofLines(final File file) {
        return ofLines(file, Charsets.DEFAULT);
    }

    /**
     * Creates a sequence that reads lines from the specified file using the given charset.
     *
     * @param file the file to read lines from
     * @param charset the charset to use for decoding the file
     * @return a sequence containing the lines of the file
     * @throws IllegalArgumentException if the file is null
     */
    public static Seq<String, IOException> ofLines(final File file, final Charset charset) throws IllegalArgumentException {
        N.checkArgNotNull(file, cs.file);

        final Throwables.Iterator<String, IOException> iter = createLazyLineIterator(file, null, charset, null, true);

        return create(iter).onClose(newCloseHandler(iter)); //NOSONAR
    }

    /**
     * Creates a sequence that reads lines from the specified path using the default charset.
     *
     * @param path the path to read lines from
     * @return a sequence containing the lines of the file
     */
    public static Seq<String, IOException> ofLines(final Path path) {
        return ofLines(path, Charsets.DEFAULT);
    }

    /**
     * Creates a sequence that reads lines from the specified path using the given charset.
     *
     * @param path the path to read lines from
     * @param charset the charset to use for decoding the file
     * @return a sequence containing the lines of the file
     * @throws IllegalArgumentException if the path is null
     */
    public static Seq<String, IOException> ofLines(final Path path, final Charset charset) throws IllegalArgumentException {
        N.checkArgNotNull(path, cs.path);

        final Throwables.Iterator<String, IOException> iter = createLazyLineIterator(null, path, charset, null, true);

        return create(iter).onClose(newCloseHandler(iter));
    }

    /**
     * Creates a sequence of lines from the given Reader.
     * <br />
     * It's user's responsibility to close the input {@code reader} after the sequence is completed.
     *
     * @param reader the Reader to read lines from
     * @return a sequence of lines read from the Reader
     * @throws IllegalArgumentException if the reader is null
     */
    public static Seq<String, IOException> ofLines(final Reader reader) throws IllegalArgumentException {
        return ofLines(reader, false);
    }

    /**
     * Creates a sequence of lines from the given Reader.
     *
     * @param reader the Reader to read lines from
     * @param closeReaderWhenStreamIsClosed if {@code true}, the input {@code Reader} will be closed when the sequence is closed
     * @return a sequence of lines read from the Reader
     * @throws IllegalArgumentException if the reader is null
     */
    public static Seq<String, IOException> ofLines(final Reader reader, final boolean closeReaderWhenStreamIsClosed) throws IllegalArgumentException {
        N.checkArgNotNull(reader);

        final Throwables.Iterator<String, IOException> iter = createLazyLineIterator(null, null, Charsets.DEFAULT, reader, closeReaderWhenStreamIsClosed);

        if (closeReaderWhenStreamIsClosed) {
            return of(iter).onClose(iter::close); //NOSONAR
        } else {
            return of(iter); //NOSONAR
        }
    }

    /**
     * Lists the files in the specified parent directory.
     *
     * @param parentPath the parent directory to list files from
     * @return a sequence containing the files in the parent directory
     */
    public static Seq<File, IOException> listFiles(final File parentPath) {
        if (!parentPath.exists()) {
            return empty();
        }

        return of(parentPath.listFiles());
    }

    /**
     * Lists the files in the specified parent directory.
     *
     * @param parentPath the parent directory to list files from
     * @param recursively if {@code true}, lists files recursively; otherwise, lists files non-recursively
     * @return a sequence containing the files in the parent directory
     */
    public static Seq<File, IOException> listFiles(final File parentPath, final boolean recursively) {
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

        return create(iter);
    }

    /**
     * Creates a lazy iterator for reading lines from a file, path, or reader.
     *
     * @param file the file to read lines from, can be {@code null} if path or reader is provided
     * @param path the path to read lines from, can be {@code null} if file or reader is provided
     * @param charset the charset to use for decoding the file or path
     * @param reader the reader to read lines from, can be {@code null} if file or path is provided
     * @param closeReader whether to close the reader when the sequence is closed
     * @return a lazy iterator for reading lines
     */
    private static Throwables.Iterator<String, IOException> createLazyLineIterator(final File file, final Path path, final Charset charset, final Reader reader,
            final boolean closeReader) {
        return Throwables.Iterator.defer(new Supplier<>() {
            private Throwables.Iterator<String, IOException> lazyIter = null;

            @Override
            public synchronized Throwables.Iterator<String, IOException> get() {
                if (lazyIter == null) {
                    lazyIter = new Throwables.Iterator<>() {
                        private final BufferedReader bufferedReader;

                        { //NOSONAR
                            if (reader != null) {
                                bufferedReader = reader instanceof BufferedReader ? ((BufferedReader) reader) : new BufferedReader(reader);
                            } else if (file != null) {
                                bufferedReader = IOUtil.newBufferedReader(file, charset == null ? Charsets.DEFAULT : charset);
                            } else {
                                bufferedReader = IOUtil.newBufferedReader(path, charset == null ? Charsets.DEFAULT : charset);
                            }
                        }

                        private String cachedLine;
                        private boolean finished = false;

                        public boolean hasNext() throws IOException {
                            if (cachedLine != null) {
                                return true;
                            } else if (finished) {
                                return false;
                            } else {
                                cachedLine = bufferedReader.readLine();
                                if (cachedLine == null) {
                                    finished = true;
                                    return false;
                                } else {
                                    return true;
                                }
                            }
                        }

                        public String next() throws IOException {
                            if (!hasNext()) {
                                throw new NoSuchElementException("No more lines");
                            } else {
                                final String res = cachedLine;
                                cachedLine = null;
                                return res;
                            }
                        }

                        @Override
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
     * Concatenates multiple arrays into a single sequence.
     *
     * @param <T> the type of elements in the arrays
     * @param <E> the type of exception that might be thrown
     * @param a the arrays to be concatenated
     * @return a sequence containing all elements from the provided arrays
     */
    @SafeVarargs
    public static <T, E extends Exception> Seq<T, E> concat(final T[]... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return of(Iterators.concat(a));
    }

    /**
     * Concatenates multiple Iterables into a single sequence.
     *
     * @param <T> the type of elements in the Iterables
     * @param <E> the type of exception that might be thrown
     * @param a the Iterables to be concatenated
     * @return a sequence containing all elements from the provided Iterables
     */
    @SafeVarargs
    public static <T, E extends Exception> Seq<T, E> concat(final Iterable<? extends T>... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return of(Iterators.concat(a));
    }

    /**
     * Concatenates multiple Iterators into a single sequence.
     *
     * @param <T> the type of elements in the Iterators
     * @param <E> the type of exception that might be thrown
     * @param a the Iterators to be concatenated
     * @return a sequence containing all elements from the provided Iterators
     */
    @SafeVarargs
    public static <T, E extends Exception> Seq<T, E> concat(final Iterator<? extends T>... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return of(Iterators.concat(a));
    }

    /**
     * Concatenates multiple sequences into a single sequence.
     *
     * @param <T> the type of elements in the sequences
     * @param <E> the type of exception that might be thrown
     * @param a the sequences to be concatenated
     * @return a sequence containing all elements from the provided sequences
     */
    @SafeVarargs
    public static <T, E extends Exception> Seq<T, E> concat(final Seq<? extends T, E>... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concat(Array.asList(a));
    }

    /**
     * Concatenates multiple sequences into a single sequence.
     *
     * @param <T> the type of elements in the sequences
     * @param <E> the type of exception that might be thrown
     * @param c the sequences to be concatenated
     * @return a sequence containing all elements from the provided sequences
     */
    public static <T, E extends Exception> Seq<T, E> concat(final Collection<? extends Seq<? extends T, E>> c) {
        if (N.isEmpty(c)) {
            return empty();
        }

        return create(new Throwables.Iterator<>() {
            private final Iterator<? extends Seq<? extends T, E>> iterators = c.iterator();
            private Seq<? extends T, E> cur;
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
     * Zips two arrays into a single sequence using the provided zip function.
     * The size of the resulting sequence is equal to the size of the shorter input array.
     *
     * @param <A> the type of elements in the first array
     * @param <B> the type of elements in the second array
     * @param <T> the type of elements in the resulting sequence
     * @param a the first array to zip
     * @param b the second array to zip
     * @param zipFunction a function that combines elements from the two arrays. An empty sequence is returned if the one of input arrays is {@code null} or empty.
     * @return a sequence of combined elements
     * @see N#zip(Object[], Object[], java.util.function.BiFunction)
     */
    public static <A, B, T, E extends Exception> Seq<T, E> zip(final A[] a, final B[] b,
            final Throwables.BiFunction<? super A, ? super B, ? extends T, ? extends E> zipFunction) {
        return create(new Throwables.Iterator<>() {
            private final int len = N.min(N.len(a), N.len(b));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public T next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return zipFunction.apply(a[cursor], b[cursor++]);
            }
        });
    }

    /**
     * Zips three arrays into a single sequence using the provided zip function.
     * The size of the resulting sequence is equal to the size of the shortest input array.
     *
     * @param <A> the type of elements in the first array
     * @param <B> the type of elements in the second array
     * @param <C> the type of elements in the third array
     * @param <T> the type of elements in the resulting sequence
     * @param a the first array to zip
     * @param b the second array to zip
     * @param c the third array to zip
     * @param zipFunction a function that combines elements from the three arrays. An empty sequence is returned if the one of input arrays is {@code null} or empty.
     * @return a sequence of combined elements
     * @see N#zip(Object[], Object[], Object[], TriFunction)
     */
    public static <A, B, C, T, E extends Exception> Seq<T, E> zip(final A[] a, final B[] b, final C[] c,
            final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends T, ? extends E> zipFunction) {
        return create(new Throwables.Iterator<>() {
            private final int len = N.min(N.len(a), N.len(b), N.len(c));
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public T next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                return zipFunction.apply(a[cursor], b[cursor], c[cursor++]);
            }
        });
    }

    /**
     * Zips two iterables into a single sequence using the provided zip function.
     * The size of the resulting sequence is equal to the size of the shorter input iterable.
     *
     * @param <A> the type of elements in the first iterable
     * @param <B> the type of elements in the second iterable
     * @param <T> the type of elements in the resulting sequence
     * @param a the first iterable to zip
     * @param b the second iterable to zip
     * @param zipFunction a function that combines elements from the two iterables. An empty sequence is returned if the one of input iterables is {@code null} or empty.
     * @return a sequence of combined elements
     * @see N#zip(Iterable, Iterable, java.util.function.BiFunction)
     */
    public static <A, B, T, E extends Exception> Seq<T, E> zip(final Iterable<? extends A> a, final Iterable<? extends B> b,
            final Throwables.BiFunction<? super A, ? super B, ? extends T, ? extends E> zipFunction) {
        return zip(N.iterate(a), N.iterate(b), zipFunction);
    }

    /**
     * Zips three iterables into a single sequence using the provided zip function.
     * The size of the resulting sequence is equal to the size of the shortest input iterable.
     *
     * @param <A> the type of elements in the first iterable
     * @param <B> the type of elements in the second iterable
     * @param <C> the type of elements in the third iterable
     * @param <T> the type of elements in the resulting sequence
     * @param a the first iterable to zip
     * @param b the second iterable to zip
     * @param c the third iterable to zip
     * @param zipFunction a function that combines elements from the three iterables. An empty sequence is returned if the one of input iterables is {@code null} or empty.
     * @return a sequence of combined elements
     * @see N#zip(Iterable, Iterable, Iterable, TriFunction)
     */
    public static <A, B, C, T, E extends Exception> Seq<T, E> zip(final Iterable<? extends A> a, final Iterable<? extends B> b, final Iterable<? extends C> c,
            final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends T, ? extends E> zipFunction) {
        return zip(N.iterate(a), N.iterate(b), N.iterate(c), zipFunction);
    }

    /**
     * Zips two iterators into a single sequence using the provided zip function.
     * The size of the resulting sequence is equal to the size of the shorter input iterator.
     *
     * @param <A> the type of elements in the first iterator
     * @param <B> the type of elements in the second iterator
     * @param <T> the type of elements in the resulting sequence
     * @param a the first iterator to zip
     * @param b the second iterator to zip
     * @param zipFunction a function that combines elements from the two iterators. An empty sequence is returned if the one of input iterators is {@code null} or empty.
     * @return a sequence of combined elements
     * @see N#zip(Iterable, Iterable, BiFunction)
     */
    public static <A, B, T, E extends Exception> Seq<T, E> zip(final Iterator<? extends A> a, final Iterator<? extends B> b,
            final Throwables.BiFunction<? super A, ? super B, ? extends T, ? extends E> zipFunction) {
        return create(new Throwables.Iterator<>() {
            private final Iterator<? extends A> iterA = a == null ? ObjIterator.<A> empty() : a;
            private final Iterator<? extends B> iterB = b == null ? ObjIterator.<B> empty() : b;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext();
            }

            @Override
            public T next() throws E {
                return zipFunction.apply(iterA.next(), iterB.next());
            }
        });
    }

    /**
     * Zips three iterators into a single sequence using the provided zip function.
     * The size of the resulting sequence is equal to the size of the shortest input iterator.
     *
     * @param <A> the type of elements in the first iterator
     * @param <B> the type of elements in the second iterator
     * @param <C> the type of elements in the third iterator
     * @param <T> the type of elements in the resulting sequence
     * @param a the first iterator to zip
     * @param b the second iterator to zip
     * @param c the third iterator to zip
     * @param zipFunction a function that combines elements from the three iterators. An empty sequence is returned if the one of input iterators is {@code null} or empty.
     * @return a sequence of combined elements
     * @see N#zip(Iterable, Iterable, Iterable, TriFunction)
     */
    public static <A, B, C, T, E extends Exception> Seq<T, E> zip(final Iterator<? extends A> a, final Iterator<? extends B> b, final Iterator<? extends C> c,
            final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends T, ? extends E> zipFunction) {
        return create(new Throwables.Iterator<>() {
            private final Iterator<? extends A> iterA = a == null ? ObjIterator.<A> empty() : a;
            private final Iterator<? extends B> iterB = b == null ? ObjIterator.<B> empty() : b;
            private final Iterator<? extends C> iterC = c == null ? ObjIterator.<C> empty() : c;

            @Override
            public boolean hasNext() {
                return iterA.hasNext() && iterB.hasNext() && iterC.hasNext();
            }

            @Override
            public T next() throws E {
                return zipFunction.apply(iterA.next(), iterB.next(), iterC.next());
            }
        });
    }

    /**
     * Zips two streams into a single sequence using the provided zip function.
     * The size of the resulting sequence is equal to the size of the shorter input stream.
     *
     * @param <A> the type of elements in the first stream
     * @param <B> the type of elements in the second stream
     * @param <T> the type of elements in the resulting sequence
     * @param a the first sequence to zip
     * @param b the second sequence to zip
     * @param zipFunction a function that combines elements from the two streams. An empty sequence is returned if the one of input streams is {@code null} or empty.
     * @return a sequence of combined elements
     * @see N#zip(Iterable, Iterable, java.util.function.BiFunction)
     */
    public static <A, B, T, E extends Exception> Seq<T, E> zip(final Seq<? extends A, E> a, final Seq<? extends B, E> b,
            final Throwables.BiFunction<? super A, ? super B, ? extends T, ? extends E> zipFunction) {
        return create(new Throwables.Iterator<>() {
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
     * Zips three streams into a single sequence using the provided zip function.
     * The size of the resulting sequence is equal to the size of the shortest input stream.
     *
     * @param <A> the type of elements in the first stream
     * @param <B> the type of elements in the second stream
     * @param <C> the type of elements in the third stream
     * @param <T> the type of elements in the resulting sequence
     * @param a the first sequence to zip
     * @param b the second sequence to zip
     * @param c the third sequence to zip
     * @param zipFunction a function that combines elements from the three streams. An empty sequence is returned if the one of input streams is {@code null} or empty.
     * @return a sequence of combined elements
     * @see N#zip(Iterable, Iterable, Iterable, TriFunction)
     */
    public static <A, B, C, T, E extends Exception> Seq<T, E> zip(final Seq<? extends A, E> a, final Seq<? extends B, E> b, final Seq<? extends C, E> c,
            final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends T, ? extends E> zipFunction) {
        return create(new Throwables.Iterator<>() {
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
     * Zips two arrays into a single sequence using the provided zip function.
     * If one array is shorter, the provided default values are used for the remaining elements.
     * The size of the resulting sequence is equal to the size of the longer input array.
     *
     * @param <A> the type of elements in the first array
     * @param <B> the type of elements in the second array
     * @param <T> the type of elements in the resulting stream
     * @param a the first array to zip
     * @param b the second array to zip
     * @param valueForNoneA the default value to use if the first array is shorter
     * @param valueForNoneB the default value to use if the second array is shorter
     * @param zipFunction a function that combines elements from the two arrays
     * @return a sequence containing the zipped elements
     * @see N#zip(Object[], Object[], Object, Object, java.util.function.BiFunction)
     */
    public static <A, B, T, E extends Exception> Seq<T, E> zip(final A[] a, final B[] b, final A valueForNoneA, final B valueForNoneB,
            final Throwables.BiFunction<? super A, ? super B, ? extends T, ? extends E> zipFunction) {
        return create(new Throwables.Iterator<>() {
            private final int lenA = N.len(a);
            private final int lenB = N.len(b);
            private final int len = N.max(lenA, lenB);
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public T next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final T ret = zipFunction.apply(cursor < lenA ? a[cursor] : valueForNoneA, cursor < lenB ? b[cursor] : valueForNoneB);

                cursor++;

                return ret;
            }
        });
    }

    /**
     * Zips three arrays into a single sequence using the provided zip function.
     * If one array is shorter, the provided default values are used for the remaining elements.
     * The size of the resulting sequence is equal to the size of the longest input array.
     *
     * @param <A> the type of elements in the first array
     * @param <B> the type of elements in the second array
     * @param <C> the type of elements in the third array
     * @param <T> the type of elements in the resulting stream
     * @param a the first array to zip
     * @param b the second array to zip
     * @param c the third array to zip
     * @param valueForNoneA the default value to use if the first array is shorter
     * @param valueForNoneB the default value to use if the second array is shorter
     * @param valueForNoneC the default value to use if the third array is shorter
     * @param zipFunction a function that combines elements from the three arrays
     * @return a sequence containing the zipped elements
     * @see N#zip(Object[], Object[], Object[], Object, Object, Object,TriFunction)
     */
    public static <A, B, C, T, E extends Exception> Seq<T, E> zip(final A[] a, final B[] b, final C[] c, final A valueForNoneA, final B valueForNoneB,
            final C valueForNoneC, final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends T, ? extends E> zipFunction) {
        return create(new Throwables.Iterator<>() {
            private final int lenA = N.len(a);
            private final int lenB = N.len(b);
            private final int lenC = N.len(c);
            private final int len = N.max(lenA, lenB, lenC);
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                return cursor < len;
            }

            @Override
            public T next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                final T ret = zipFunction.apply(cursor < lenA ? a[cursor] : valueForNoneA, cursor < lenB ? b[cursor] : valueForNoneB,
                        cursor < lenC ? c[cursor] : valueForNoneC);

                cursor++;

                return ret;
            }
        });
    }

    /**
     * Zips two iterables into a single sequence using the provided zip function.
     * If one iterable is shorter, the provided default values are used for the remaining elements.
     * The size of the resulting sequence is equal to the size of the longer input iterable.
     *
     * @param <A> the type of elements in the first iterable
     * @param <B> the type of elements in the second iterable
     * @param <T> the type of elements in the resulting stream
     * @param a the first iterable to zip
     * @param b the second iterable to zip
     * @param valueForNoneA the default value to use if the first iterable is shorter
     * @param valueForNoneB the default value to use if the second iterable is shorter
     * @param zipFunction a function that combines elements from the two iterables
     * @return a sequence containing the zipped elements
     * @see N#zip(Iterable, Iterable, Object, Object, java.util.function.BiFunction)
     */
    public static <A, B, T, E extends Exception> Seq<T, E> zip(final Iterable<? extends A> a, final Iterable<? extends B> b, final A valueForNoneA,
            final B valueForNoneB, final Throwables.BiFunction<? super A, ? super B, ? extends T, ? extends E> zipFunction) {
        return zip(N.iterate(a), N.iterate(b), valueForNoneA, valueForNoneB, zipFunction);
    }

    /**
     * Zips three iterables into a single sequence using the provided zip function.
     * If one iterable is shorter, the provided default values are used for the remaining elements.
     * The size of the resulting sequence is equal to the size of the longest input iterable.
     *
     * @param <A> the type of elements in the first iterable
     * @param <B> the type of elements in the second iterable
     * @param <C> the type of elements in the third iterable
     * @param <T> the type of elements in the resulting stream
     * @param a the first iterable to zip
     * @param b the second iterable to zip
     * @param c the third iterable to zip
     * @param valueForNoneA the default value to use if the first iterable is shorter
     * @param valueForNoneB the default value to use if the second iterable is shorter
     * @param valueForNoneC the default value to use if the third iterable is shorter
     * @param zipFunction a function that combines elements from the three iterables
     * @return a sequence containing the zipped elements
     * @see N#zip(Iterable, Iterable, Iterable, Object, Object, Object, TriFunction)
     */
    public static <A, B, C, T, E extends Exception> Seq<T, E> zip(final Iterable<? extends A> a, final Iterable<? extends B> b, final Iterable<? extends C> c,
            final A valueForNoneA, final B valueForNoneB, final C valueForNoneC,
            final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends T, ? extends E> zipFunction) {
        return zip(N.iterate(a), N.iterate(b), N.iterate(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    /**
     * Zips two iterators into a single sequence using the provided zip function.
     * If one iterator is shorter, the provided default values are used for the remaining elements.
     * The size of the resulting sequence is equal to the size of the longer input iterator.
     *
     * @param <A> the type of elements in the first iterator
     * @param <B> the type of elements in the second iterator
     * @param <T> the type of elements in the resulting stream
     * @param a the first iterator to zip
     * @param b the second iterator to zip
     * @param valueForNoneA the default value to use if the first iterator is shorter
     * @param valueForNoneB the default value to use if the second iterator is shorter
     * @param zipFunction a function that combines elements from the two iterators
     * @return a sequence containing the zipped elements
     * @see N#zip(Iterable, Iterable, Object, Object, BiFunction)
     */
    public static <A, B, T, E extends Exception> Seq<T, E> zip(final Iterator<? extends A> a, final Iterator<? extends B> b, final A valueForNoneA,
            final B valueForNoneB, final Throwables.BiFunction<? super A, ? super B, ? extends T, ? extends E> zipFunction) {
        return create(new Throwables.Iterator<>() {
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
     * Zips three iterators into a single sequence using the provided zip function.
     * If one iterator is shorter, the provided default values are used for the remaining elements.
     * The size of the resulting sequence is equal to the size of the longest input iterator.
     *
     * @param <A> the type of elements in the first iterator
     * @param <B> the type of elements in the second iterator
     * @param <C> the type of elements in the third iterator
     * @param <T> the type of elements in the resulting stream
     * @param a the first iterator to zip
     * @param b the second iterator to zip
     * @param c the third iterator to zip
     * @param valueForNoneA the default value to use if the first iterator is shorter
     * @param valueForNoneB the default value to use if the second iterator is shorter
     * @param valueForNoneC the default value to use if the third iterator is shorter
     * @param zipFunction a function that combines elements from the three iterators
     * @return a sequence containing the zipped elements
     * @seeN#zip(Iterable, Iterable, Iterable, Object, Object, Object, TriFunction)
     */
    public static <A, B, C, T, E extends Exception> Seq<T, E> zip(final Iterator<? extends A> a, final Iterator<? extends B> b, final Iterator<? extends C> c,
            final A valueForNoneA, final B valueForNoneB, final C valueForNoneC,
            final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends T, ? extends E> zipFunction) {
        return create(new Throwables.Iterator<>() {
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
     * Zips two streams into a single sequence using the provided zip function.
     * If one sequence is shorter, the provided default values are used for the remaining elements.
     * The size of the resulting sequence is equal to the size of the longer input stream.
     *
     * @param <A> the type of elements in the first stream
     * @param <B> the type of elements in the second stream
     * @param <T> the type of elements in the resulting stream
     * @param a the first sequence to zip
     * @param b the second sequence to zip
     * @param valueForNoneA the default value to use if the first sequence is shorter
     * @param valueForNoneB the default value to use if the second sequence is shorter
     * @param zipFunction a function that combines elements from the two streams
     * @return a sequence containing the zipped elements
     * @see N#zip(Iterable, Iterable, Object, Object, java.util.function.BiFunction)
     */
    public static <A, B, T, E extends Exception> Seq<T, E> zip(final Seq<? extends A, E> a, final Seq<? extends B, E> b, final A valueForNoneA,
            final B valueForNoneB, final Throwables.BiFunction<? super A, ? super B, ? extends T, ? extends E> zipFunction) {
        return create(new Throwables.Iterator<>() {
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
     * Zips three streams into a single sequence using the provided zip function.
     * If one sequence is shorter, the provided default values are used for the remaining elements.
     * The size of the resulting sequence is equal to the size of the longest input stream.
     *
     * @param <A> the type of elements in the first stream
     * @param <B> the type of elements in the second stream
     * @param <C> the type of elements in the third stream
     * @param <T> the type of elements in the resulting stream
     * @param a the first sequence to zip
     * @param b the second sequence to zip
     * @param c the third sequence to zip
     * @param valueForNoneA the default value to use if the first sequence is shorter
     * @param valueForNoneB the default value to use if the second sequence is shorter
     * @param valueForNoneC the default value to use if the third sequence is shorter
     * @param zipFunction a function that combines elements from the three streams
     * @return a sequence containing the zipped elements
     * @see N#zip(Iterable, Iterable, Iterable, Object, Object, Object, TriFunction)
     */
    public static <A, B, C, T, E extends Exception> Seq<T, E> zip(final Seq<? extends A, E> a, final Seq<? extends B, E> b, final Seq<? extends C, E> c,
            final A valueForNoneA, final B valueForNoneB, final C valueForNoneC,
            final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends T, ? extends E> zipFunction) {
        return create(new Throwables.Iterator<>() {
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
     * Merges two arrays into a single sequence using the provided nextSelector function.
     * The size of the resulting sequence is equal to the sum of size of the two input arrays.
     *
     * @param <T> the type of elements in the arrays
     * @param <E> the type of exception
     * @param a the first array to merge
     * @param b the second array to merge
     * @param nextSelector a function to determine which element should be selected as next element.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a sequence of merged elements
     * @see N#merge(Object[], Object[], java.util.function.BiFunction)
     */
    public static <T, E extends Exception> Seq<T, E> merge(final T[] a, final T[] b,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        if (N.isEmpty(a)) {
            return of(b);
        } else if (N.isEmpty(b)) {
            return of(a);
        }

        return create(new Throwables.Iterator<>() {
            private final int lenA = N.len(a);
            private final int lenB = N.len(b);
            private int cursorA = 0;
            private int cursorB = 0;

            @Override
            public boolean hasNext() {
                return cursorA < lenA || cursorB < lenB;
            }

            @Override
            public T next() throws E {
                if (cursorA < lenA) {
                    if ((cursorB >= lenB) || (nextSelector.apply(a[cursorA], b[cursorB]) == MergeResult.TAKE_FIRST)) {
                        return a[cursorA++];
                    } else {
                        return b[cursorB++];
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
     * Merges three arrays into a single sequence using the provided nextSelector function.
     * The size of the resulting sequence is equal to the sum of size of the three input arrays.
     *
     * @param <T> the type of elements in the arrays
     * @param <E> the type of exception
     * @param a the first array to merge
     * @param b the second array to merge
     * @param c the third array to merge
     * @param nextSelector a function to determine which element should be selected as next element.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a sequence of merged elements
     * @see N#merge(Object[], Object[], java.util.function.BiFunction)
     */
    public static <T, E extends Exception> Seq<T, E> merge(final T[] a, final T[] b, final T[] c,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        //noinspection resource
        return mergeIterators(merge(a, b, nextSelector).iteratorEx(), Throwables.Iterator.of(N.iterate(c)), nextSelector);
    }

    /**
     * Merges two iterables into a single sequence using the provided nextSelector function.
     * The size of the resulting sequence is equal to the sum of size of the two input iterables.
     *
     * @param <T> the type of elements in the iterables
     * @param <E> the type of exception
     * @param a the first iterable to merge
     * @param b the second iterable to merge
     * @param nextSelector a function to determine which element should be selected as next element.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a sequence of merged elements
     * @see N#merge(Iterable, Iterable, java.util.function.BiFunction)
     */
    public static <T, E extends Exception> Seq<T, E> merge(final Iterable<? extends T> a, final Iterable<? extends T> b,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return merge(N.iterate(a), N.iterate(b), nextSelector);
    }

    /**
     * Merges three iterables into a single sequence using the provided nextSelector function.
     * The size of the resulting sequence is equal to the sum of size of the three input iterables.
     *
     * @param <T> the type of elements in the iterables
     * @param <E> the type of exception
     * @param a the first iterable to merge
     * @param b the second iterable to merge
     * @param c the third iterable to merge
     * @param nextSelector a function to determine which element should be selected as next element.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a sequence of merged elements
     * @see N#merge(Iterable, Iterable, java.util.function.BiFunction)
     */
    public static <T, E extends Exception> Seq<T, E> merge(final Iterable<? extends T> a, final Iterable<? extends T> b, final Iterable<? extends T> c,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return merge(N.iterate(a), N.iterate(b), N.iterate(c), nextSelector);
    }

    /**
     * Merges two iterators into a single sequence using the provided nextSelector function.
     * The size of the resulting sequence is equal to the sum of size of the two input iterators.
     *
     * @param <T> the type of elements in the iterators
     * @param <E> the type of exception
     * @param a the first iterator to merge
     * @param b the second iterator to merge
     * @param nextSelector a function to determine which element should be selected as next element.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a sequence of merged elements
     * @see N#merge(Iterable, Iterable, java.util.function.BiFunction)
     */
    public static <T, E extends Exception> Seq<T, E> merge(final Iterator<? extends T> a, final Iterator<? extends T> b,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return mergeIterators(Throwables.Iterator.<T, E> of(a), Throwables.Iterator.<T, E> of(b), nextSelector);
    }

    /**
     * Merges three iterators into a single sequence using the provided nextSelector function.
     * The size of the resulting sequence is equal to the sum of size of the three input iterators.
     *
     * @param <T> the type of elements in the iterators
     * @param <E> the type of exception
     * @param a the first iterator to merge
     * @param b the second iterator to merge
     * @param c the third iterator to merge
     * @param nextSelector a function to determine which element should be selected as next element.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a sequence of merged elements
     * @see N#merge(Iterable, Iterable, java.util.function.BiFunction)
     */
    public static <T, E extends Exception> Seq<T, E> merge(final Iterator<? extends T> a, final Iterator<? extends T> b, final Iterator<? extends T> c,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        //noinspection resource
        return mergeIterators(merge(a, b, nextSelector).iteratorEx(), Throwables.Iterator.<T, E> of(c), nextSelector);
    }

    /**
     * Merges two sequences into a single sequence using the provided nextSelector function.
     * The size of the resulting sequence is equal to the sum of size of the two input sequences.
     *
     * @param <T> the type of elements in the sequences
     * @param <E> the type of exception
     * @param a the first sequence to merge
     * @param b the second sequence to merge
     * @param nextSelector a function to determine which element should be selected as next element.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a sequence of merged elements
     * @see N#merge(Iterable, Iterable, java.util.function.BiFunction)
     */
    public static <T, E extends Exception> Seq<T, E> merge(final Seq<? extends T, E> a, final Seq<? extends T, E> b,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return mergeIterators(iterate(a), iterate(b), nextSelector).onClose(newCloseHandler(Array.asList(a, b)));
    }

    /**
     * Merges three sequences into a single sequence using the provided nextSelector function.
     * The size of the resulting sequence is equal to the sum of size of the three input sequences.
     *
     * @param <T> the type of elements in the sequences
     * @param <E> the type of exception
     * @param a the first sequence to merge
     * @param b the second sequence to merge
     * @param c the third sequence to merge
     * @param nextSelector a function to determine which element should be selected as next element.
     *                     The first parameter is selected if {@code MergeResult.TAKE_FIRST} is returned, otherwise the second parameter is selected.
     * @return a sequence of merged elements
     * @see N#merge(Iterable, Iterable, java.util.function.BiFunction)
     */
    public static <T, E extends Exception> Seq<T, E> merge(final Seq<? extends T, E> a, final Seq<? extends T, E> b, final Seq<? extends T, E> c,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return merge(merge(a, b, nextSelector), c, nextSelector);
    }

    static <T, E extends Exception> Seq<T, E> mergeIterators(final Throwables.Iterator<? extends T, E> a, final Throwables.Iterator<? extends T, E> b,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return create(new Throwables.Iterator<>() {
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
     * Filters the elements of this sequence using the provided predicate.
     *
     * @param predicate the predicate to apply to each element to determine if it should be included
     * @return a new sequence containing only the elements that match the predicate
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> filter(final Throwables.Predicate<? super T, ? extends E> predicate) throws IllegalStateException {
        assertNotClosed();

        return create(new Throwables.Iterator<>() {
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
     * Filters the elements of this sequence using the provided predicate.
     *
     * @param predicate the predicate to apply to each element to determine if it should be included
     * @param actionOnDroppedItem the action to perform on items that are dropped by the filter
     * @return a new sequence containing only the elements that match the predicate
     * @throws IllegalStateException if the sequence is already closed
     */
    @Beta
    @IntermediateOp
    public Seq<T, E> filter(final Throwables.Predicate<? super T, ? extends E> predicate, final Throwables.Consumer<? super T, ? extends E> actionOnDroppedItem)
            throws IllegalStateException {
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
     * Takes elements from this sequence while the provided predicate is {@code true}.
     *
     * @param predicate the predicate to apply to each element to determine if it should be included
     * @return a new sequence containing the elements taken while the predicate is true
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> takeWhile(final Throwables.Predicate<? super T, ? extends E> predicate) throws IllegalStateException {
        assertNotClosed();

        return create(new Throwables.Iterator<>() {
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
     * Drops elements from this sequence while the provided predicate is {@code true}.
     *
     * @param predicate the predicate to apply to each element to determine if it should be dropped
     * @return a new sequence containing the elements after the predicate becomes false
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> dropWhile(final Throwables.Predicate<? super T, ? extends E> predicate) throws IllegalStateException {
        assertNotClosed();

        return create(new Throwables.Iterator<>() {
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
     * Drops elements from this sequence while the provided predicate is {@code true}.
     *
     * @param predicate the predicate to apply to each element to determine if it should be dropped
     * @param actionOnDroppedItem the action to perform on items that are dropped by the filter
     * @return a new sequence containing the elements after the predicate becomes false
     * @throws IllegalStateException if the sequence is already closed
     */
    @Beta
    @IntermediateOp
    public Seq<T, E> dropWhile(final Throwables.Predicate<? super T, ? extends E> predicate,
            final Throwables.Consumer<? super T, ? extends E> actionOnDroppedItem) throws IllegalStateException {
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
     * Skips elements from this sequence until the provided predicate is {@code true}.
     *
     * @param predicate the predicate to apply to each element to determine if it should be skipped
     * @return a new sequence containing the elements after the predicate becomes true
     * @throws IllegalStateException if the sequence is already closed
     */
    @Beta
    @IntermediateOp
    public Seq<T, E> skipUntil(final Throwables.Predicate<? super T, ? extends E> predicate) throws IllegalStateException {
        return dropWhile(Fnn.not(predicate));
    }

    /**
     * Returns a sequence consisting of the distinct elements of this stream.
     *
     * @return a new sequence containing distinct elements
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> distinct() throws IllegalStateException {
        assertNotClosed();

        final Set<Object> set = N.newHashSet();

        return filter(value -> set.add(hashKey(value)));
    }

    /**
     * Returns a sequence consisting of the distinct elements of this stream
     *
     * @implNote Equivalent to: {@code groupBy(Fnn.identity(), Fnn.identity(), mergeFunction).map(Fnn.value())}.
     *
     * @param mergeFunction the function to merge elements that are considered equal
     * @return a new sequence containing distinct elements
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> distinct(final Throwables.BinaryOperator<T, ? extends E> mergeFunction) throws IllegalStateException {
        return distinctBy(Fnn.identity(), mergeFunction);
    }

    //    /**
    //     * Returns a stream consisting of the distinct elements of this stream.
    //     * And only keep the elements whose occurrences are satisfied the specified predicate.
    //     *
    //     * @implNote Equivalent to: {@code countBy(Fnn.identity(), supplier).filter(predicate).map(Fnn.key())}.
    //     *
    //     * @param occurrencesFilter the predicate to apply to the count of occurrences of each element
    //     * @return a new Seq containing distinct elements
    //     * @throws IllegalStateException if the sequence is already closed
    //     */
    //    @IntermediateOp
    //    @TerminalOpTriggered
    //    public Seq<T, E> distinct(final Throwables.Predicate<? super Integer, ? extends E> occurrencesFilter) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        final Supplier<? extends Map<T, Integer>> supplier = Suppliers.<T, Integer> ofLinkedHashMap();
    //
    //        final Throwables.Predicate<Map.Entry<T, Integer>, ? extends E> predicate = e -> occurrencesFilter.test(e.getValue());
    //
    //        return create(countBy(Fnn.identity(), supplier).filter(predicate).map(Fnn.key()).iteratorEx(), sorted, cmp, closeHandlers);
    //    }

    /**
     * Returns a sequence consisting of the distinct elements of this stream,
     * where distinct elements are determined by the provided key extractor function.
     *
     * @param keyMapper the function to extract the key from the elements
     * @return a new sequence containing distinct elements
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> distinctBy(final Throwables.Function<? super T, ?, ? extends E> keyMapper) throws IllegalStateException {
        assertNotClosed();

        final Set<Object> set = N.newHashSet();

        return filter(value -> set.add(hashKey(keyMapper.apply(value))));
    }

    /**
     * Returns a sequence consisting of the distinct elements of this stream,
     * where distinct elements are determined by the provided key extractor function.
     * If multiple elements have the same key, they are merged using the provided merge function.
     *
     * @implNote Equivalent to: {@code groupBy(keyMapper, Fnn.identity(), mergeFunction).map(Fnn.value())}.
     *
     * @param keyMapper the function to extract the key from the elements
     * @param mergeFunction the function to merge elements that have the same key
     * @return a new sequence containing distinct elements
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> distinctBy(final Throwables.Function<? super T, ?, ? extends E> keyMapper, final Throwables.BinaryOperator<T, ? extends E> mergeFunction)
            throws IllegalStateException {
        assertNotClosed();

        final Supplier<? extends Map<Object, T>> supplier = Suppliers.ofLinkedHashMap();

        //noinspection resource
        return groupBy(keyMapper, Fnn.identity(), mergeFunction, supplier).map(Fnn.value());
    }

    //    /**
    //     * Returns a stream consisting of the distinct elements of this stream,
    //     * where distinct elements are determined by the provided key extractor function.
    //     * Only keep the elements whose occurrences are satisfied by the specified occurrences filter.
    //     *
    //     * @param <K> the type of the key extracted from the elements
    //     * @param keyMapper the function to extract the key from the elements
    //     * @param occurrencesFilter the predicate to apply to the count of occurrences of each element
    //     * @return a new Seq containing distinct elements
    //     * @throws IllegalStateException if the sequence is already closed
    //     */
    //    @IntermediateOp
    //    @TerminalOpTriggered
    //    @SuppressWarnings("rawtypes")
    //    public <K> Seq<T, E> distinctBy(final Throwables.Function<? super T, K, ? extends E> keyMapper,
    //            final Throwables.Predicate<? super Map.Entry<Keyed<K, T>, Integer>, ? extends E> occurrencesFilter) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        final Supplier<? extends Map<Keyed<K, T>, Integer>> supplier = Suppliers.<Keyed<K, T>, Integer> ofLinkedHashMap();
    //
    //        final Throwables.Function<T, Keyed<K, T>, E> keyedMapper = t -> Keyed.of(keyMapper.apply(t), t);
    //
    //        return create(countBy(keyedMapper, supplier).filter(occurrencesFilter)
    //                .map((Throwables.Function<Map.Entry<Keyed<K, T>, Integer>, T, E>) (Throwables.Function) KK)
    //                .iteratorEx(), sorted, cmp, closeHandlers);
    //    }

    //    /**
    //     * Distinct by the key extracted by {@code keyMapper} and limit the appearance of the elements with same key to the number calculated by {@code limit}
    //     *
    //     * @param <K>
    //     * @param keyMapper
    //     * @param limit
    //     * @return
    //     * @see #groupBy(Throwables.Function, Collector)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    @TerminalOpTriggered
    //    public <K> Seq<T, E> distinctLimitBy(final Throwables.Function<? super T, K, ? extends E> keyMapper,
    //            final Throwables.BiFunction<? super K, ? super List<T>, Integer, ? extends E> limit) {
    //
    //        final Supplier<Map<K, List<T>>> supplier = Suppliers.<K, List<T>> ofLinkedHashMap();
    //
    //        return groupBy(keyMapper, Fnn.identity(), supplier) //
    //                .flatmap(it -> subList(it.getValue(), 0, limit.apply(it.getKey(), it.getValue())));
    //    }

    /**
     * Transforms the elements of this sequence using the provided mapper function.
     *
     * @param <R> the type of the elements in the new stream
     * @param mapper the function to apply to each element
     * @return a new sequence containing the transformed elements
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public <R> Seq<R, E> map(final Throwables.Function<? super T, ? extends R, ? extends E> mapper) throws IllegalStateException {
        assertNotClosed();

        return create(new Throwables.Iterator<>() {
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
     * Transforms the elements of this sequence using the provided mapper function, but only if the elements are not {@code null}.
     * If an element is {@code null}, it is skipped.
     *
     * @implNote Equivalent to: {@code skipNulls().map(mapper)}.
     *
     * @param <R> the type of the elements in the new stream
     * @param mapper the function to apply to each {@code non-null} element
     * @return a new sequence containing the transformed elements
     * @throws IllegalStateException if the sequence is already closed
     */
    @Beta
    @IntermediateOp
    public <R> Seq<R, E> mapIfNotNull(final Throwables.Function<? super T, ? extends R, ? extends E> mapper) throws IllegalStateException {
        //noinspection resource
        return skipNulls().map(mapper);
    }

    /**
     * Transforms the first element of this sequence using the provided mapper function.
     * The remaining elements are unchanged.
     *
     * @param mapperForFirst the function to apply to the first element
     * @return a new sequence containing the transformed first element and the remaining elements
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> mapFirst(final Throwables.Function<? super T, ? extends T, ? extends E> mapperForFirst) throws IllegalStateException {
        assertNotClosed();

        return create(new Throwables.Iterator<>() {
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
     * Transforms the first element of this sequence using the provided mapper function for the first element,
     * and the remaining elements using the provided mapper function for the other elements.
     *
     * @param <R> the type of the elements in the new stream
     * @param mapperForFirst the function to apply to the first element
     * @param mapperForElse the function to apply to the remaining elements
     * @return a new sequence containing the transformed elements
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public <R> Seq<R, E> mapFirstOrElse(final Throwables.Function<? super T, ? extends R, E> mapperForFirst,
            final Throwables.Function<? super T, ? extends R, E> mapperForElse) throws IllegalStateException {
        assertNotClosed();

        return create(new Throwables.Iterator<>() {
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
     * Transforms the last element of this sequence using the provided mapper function.
     * Other elements are unchanged.
     *
     * @param mapperForLast the function to apply to the last element
     * @return a new sequence containing the transformed last element and the remaining elements
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> mapLast(final Throwables.Function<? super T, ? extends T, ? extends E> mapperForLast) throws IllegalStateException {
        assertNotClosed();

        return create(new Throwables.Iterator<>() {
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
     * Transforms the last element of this sequence using the provided mapper function for the last element,
     * and the remaining elements using the provided mapper function for the other elements.
     *
     * @param <R> the type of the elements in the new stream
     * @param mapperForLast the function to apply to the last element
     * @param mapperForElse the function to apply to the remaining elements
     * @return a new sequence containing the transformed elements
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public <R> Seq<R, E> mapLastOrElse(final Throwables.Function<? super T, ? extends R, E> mapperForLast,
            final Throwables.Function<? super T, ? extends R, E> mapperForElse) throws IllegalStateException {
        assertNotClosed();

        return create(new Throwables.Iterator<>() {
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
     * Transforms the elements of this sequence using the provided mapper function,
     * which returns a new sequence for each element. The resulting streams
     * are then flattened into a single stream.
     *
     * @param <R> the type of the elements in the new stream
     * @param mapper the function to apply to each element, which returns a new sequence
     * @return a new sequence containing the flattened elements
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public <R> Seq<R, E> flatMap(final Throwables.Function<? super T, ? extends Seq<? extends R, ? extends E>, ? extends E> mapper)
            throws IllegalStateException {
        assertNotClosed();

        final Throwables.Iterator<R, E> iter = new Throwables.Iterator<>() {
            private Seq<? extends R, ? extends E> s = null;
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

            @Override
            protected void closeResource() {
                if (s != null) {
                    s.close();
                }
            }
        };

        return create(iter, mergeCloseHandlers(iter::close, closeHandlers));
    }

    /**
     * Transforms the elements of this sequence using the provided mapper function,
     * which returns a collection of new elements for each element. The resulting collections
     * are then flattened into a single stream.
     *
     * @param <R> the type of the elements in the new stream
     * @param mapper the function to apply to each element, which returns a collection of new elements
     * @return a new sequence containing the flattened elements
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public <R> Seq<R, E> flatmap(final Throwables.Function<? super T, ? extends Collection<? extends R>, ? extends E> mapper) { //NOSONAR
        assertNotClosed();

        return create(new Throwables.Iterator<>() {
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

    //    /**
    //     * Transforms the elements of this sequence using the provided mapper function,
    //     * which returns an array of new elements for each element. The resulting arrays
    //     * are then flattened into a single stream.
    //     *
    //     * @param <R> the type of the elements in the new stream
    //     * @param mapper the function to apply to each element, which returns an array of new elements
    //     * @return a new sequence containing the flattened elements
    //     * @throws IllegalStateException if the sequence is already closed
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, E> flattMap(final Throwables.Function<? super T, R[], ? extends E> mapper) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return create(new Throwables.Iterator<R, E>() {
    //            private R[] cur = null;
    //            private int len = 0;
    //            private int idx = 0;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                while (idx >= len) {
    //                    if (elements.hasNext()) {
    //                        cur = mapper.apply(elements.next());
    //                        len = N.len(cur);
    //                        idx = 0;
    //                    } else {
    //                        cur = null;
    //                        break;
    //                    }
    //                }
    //
    //                return idx < len;
    //            }
    //
    //            @Override
    //            public R next() throws IllegalStateException, E {
    //                if (idx >= len && !hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                return cur[idx++];
    //            }
    //        }, closeHandlers);
    //    }

    //    /**
    //     * Transforms the elements of this stream using the provided mapper function,
    //     * which returns a stream of new elements for each element. The resulting streams
    //     * are then flattened into a single stream.
    //     *
    //     * @param <R> the type of the elements in the new stream
    //     * @param mapper the function to apply to each element, which returns a stream of new elements
    //     * @return a new sequence containing the flattened elements
    //     * @throws IllegalStateException if the stream is already
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, E> flattmap(final Throwables.Function<? super T, ? extends Stream<? extends R>, ? extends E> mapper) { //NOSONAR
    //        assertNotClosed();
    //
    //        final Throwables.Iterator<R, E> iter = new Throwables.Iterator<>() {
    //            private Stream<? extends R> s = null;
    //            private Iterator<? extends R> cur = null;
    //
    //            public boolean hasNext() throws E {
    //                while (cur == null || !cur.hasNext()) {
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
    //            public R next() throws E {
    //                if ((cur == null || !cur.hasNext()) && !hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                return cur.next();
    //            }
    //
    //            @Override
    //            protected void closeResource() {
    //                if (s != null) {
    //                    s.close();
    //                }
    //            }
    //        };
    //
    //        return create(iter, mergeCloseHandlers(iter::close, closeHandlers));
    //    }

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
    //        return create(iter, newCloseHandlers);
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
    //        return create(iter, newCloseHandlers);
    //    }

    /**
     * Transforms the elements of this sequence using the provided mapper function,
     * which returns a collection of new elements for each {@code non-null} element.
     * The resulting collections are then flattened into a single stream.
     * If an element is {@code null}, it is skipped.
     *
     * @implNote Equivalent to: {@code skipNulls().flatmap(mapper)}.
     *
     * @param <R> the type of the elements in the new stream
     * @param mapper the function to apply to each {@code non-null} element, which returns a collection of new elements
     * @return a new sequence containing the flattened elements
     * @throws IllegalStateException if the sequence is already closed
     */
    @Beta
    @IntermediateOp
    public <R> Seq<R, E> flatmapIfNotNull(final Throwables.Function<? super T, ? extends Collection<? extends R>, ? extends E> mapper)
            throws IllegalStateException {
        //noinspection resource
        return skipNulls().flatmap(mapper);
    }

    /**
     * Transforms the elements of this sequence using the provided mapper functions,
     * which return collections of new elements for each {@code non-null} element.
     * The resulting collections are then flattened into a single stream.
     * If an element is {@code null}, it is skipped.
     *
     * @implNote Equivalent to: {@code skipNulls().flatmap(mapper).skipNulls().flatmap(mapper2)}.
     *
     * @param <U> the type of the intermediate elements in the new stream
     * @param <R> the type of the final elements in the new stream
     * @param mapper the function to apply to each {@code non-null} element, which returns a collection of intermediate elements
     * @param mapper2 the function to apply to each intermediate element, which returns a collection of final elements
     * @return a new sequence containing the flattened elements
     * @throws IllegalStateException if the sequence is already closed
     */
    @Beta
    @IntermediateOp
    public <U, R> Seq<R, E> flatmapIfNotNull(final Throwables.Function<? super T, ? extends Collection<? extends U>, ? extends E> mapper,
            final Throwables.Function<? super U, ? extends Collection<? extends R>, ? extends E> mapper2) throws IllegalStateException {
        //noinspection resource
        return skipNulls().flatmap(mapper).skipNulls().flatmap(mapper2);
    }

    //    /**
    //     * Transforms the elements of this stream using the provided mapper function,
    //     * which returns an array of boolean values for each element. The resulting arrays
    //     * are then flattened into a single stream of boolean values.
    //     *
    //     * @param mapper the function to apply to each element, which returns an array of boolean values
    //     * @return a new Seq containing the flattened boolean values
    //     * @throws IllegalStateException if the sequence is already closed
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<Boolean, E> flatmapToBoolean(final Throwables.Function<? super T, boolean[], ? extends E> mapper) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return create(new Throwables.Iterator<Boolean, E>() {
    //            private boolean[] cur = null;
    //            private int len = 0;
    //            private int idx = 0;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                while (idx >= len) {
    //                    if (elements.hasNext()) {
    //                        cur = mapper.apply(elements.next());
    //                        len = N.len(cur);
    //                        idx = 0;
    //                    } else {
    //                        cur = null;
    //                        break;
    //                    }
    //                }
    //
    //                return idx < len;
    //            }
    //
    //            @Override
    //            public Boolean next() throws E {
    //                if (idx >= len && !hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                return cur[idx++];
    //            }
    //        }, closeHandlers);
    //    }
    //
    //    /**
    //     * Transforms the elements of this stream using the provided mapper function,
    //     * which returns an array of char values for each element. The resulting arrays
    //     * are then flattened into a single stream of char values.
    //     *
    //     * @param mapper the function to apply to each element, which returns an array of char values
    //     * @return a new Seq containing the flattened char values
    //     * @throws IllegalStateException if the sequence is already closed
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<Character, E> flatmapToChar(final Throwables.Function<? super T, char[], ? extends E> mapper) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return create(new Throwables.Iterator<Character, E>() {
    //            private char[] cur = null;
    //            private int len = 0;
    //            private int idx = 0;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                while (idx >= len) {
    //                    if (elements.hasNext()) {
    //                        cur = mapper.apply(elements.next());
    //                        len = N.len(cur);
    //                        idx = 0;
    //                    } else {
    //                        cur = null;
    //                        break;
    //                    }
    //                }
    //
    //                return idx < len;
    //            }
    //
    //            @Override
    //            public Character next() throws E {
    //                if (idx >= len && !hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                return cur[idx++];
    //            }
    //        }, closeHandlers);
    //    }
    //
    //    /**
    //     * Transforms the elements of this stream using the provided mapper function,
    //     * which returns an array of byte values for each element. The resulting arrays
    //     * are then flattened into a single stream of byte values.
    //     *
    //     * @param mapper the function to apply to each element, which returns an array of byte values
    //     * @return a new Seq containing the flattened byte values
    //     * @throws IllegalStateException if the sequence is already closed
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<Byte, E> flatmapToByte(final Throwables.Function<? super T, byte[], ? extends E> mapper) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return create(new Throwables.Iterator<Byte, E>() {
    //            private byte[] cur = null;
    //            private int len = 0;
    //            private int idx = 0;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                while (idx >= len) {
    //                    if (elements.hasNext()) {
    //                        cur = mapper.apply(elements.next());
    //                        len = N.len(cur);
    //                        idx = 0;
    //                    } else {
    //                        cur = null;
    //                        break;
    //                    }
    //                }
    //
    //                return idx < len;
    //            }
    //
    //            @Override
    //            public Byte next() throws E {
    //                if (idx >= len && !hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                return cur[idx++];
    //            }
    //        }, closeHandlers);
    //    }
    //
    //    /**
    //     * Transforms the elements of this stream using the provided mapper function,
    //     * which returns an array of short values for each element. The resulting arrays
    //     * are then flattened into a single stream of short values.
    //     *
    //     * @param mapper the function to apply to each element, which returns an array of short values
    //     * @return a new Seq containing the flattened short values
    //     * @throws IllegalStateException if the sequence is already closed
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<Short, E> flatmapToShort(final Throwables.Function<? super T, short[], ? extends E> mapper) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return create(new Throwables.Iterator<Short, E>() {
    //            private short[] cur = null;
    //            private int len = 0;
    //            private int idx = 0;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                while (idx >= len) {
    //                    if (elements.hasNext()) {
    //                        cur = mapper.apply(elements.next());
    //                        len = N.len(cur);
    //                        idx = 0;
    //                    } else {
    //                        cur = null;
    //                        break;
    //                    }
    //                }
    //
    //                return idx < len;
    //            }
    //
    //            @Override
    //            public Short next() throws E {
    //                if (idx >= len && !hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                return cur[idx++];
    //            }
    //        }, closeHandlers);
    //    }
    //
    //    /**
    //     * Transforms the elements of this stream using the provided mapper function,
    //     * which returns an array of int values for each element. The resulting arrays
    //     * are then flattened into a single stream of int values.
    //     *
    //     * @param mapper the function to apply to each element, which returns an array of int values
    //     * @return a new Seq containing the flattened int values
    //     * @throws IllegalStateException if the sequence is already closed
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<Integer, E> flatmapToInt(final Throwables.Function<? super T, int[], ? extends E> mapper) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return create(new Throwables.Iterator<Integer, E>() {
    //            private int[] cur = null;
    //            private int len = 0;
    //            private int idx = 0;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                while (idx >= len) {
    //                    if (elements.hasNext()) {
    //                        cur = mapper.apply(elements.next());
    //                        len = N.len(cur);
    //                        idx = 0;
    //                    } else {
    //                        cur = null;
    //                        break;
    //                    }
    //                }
    //
    //                return idx < len;
    //            }
    //
    //            @Override
    //            public Integer next() throws E {
    //                if (idx >= len && !hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                return cur[idx++];
    //            }
    //        }, closeHandlers);
    //    }
    //
    //    /**
    //     * Transforms the elements of this stream using the provided mapper function,
    //     * which returns an array of long values for each element. The resulting arrays
    //     * are then flattened into a single stream of long values.
    //     *
    //     * @param mapper the function to apply to each element, which returns an array of long values
    //     * @return a new Seq containing the flattened long values
    //     * @throws IllegalStateException if the sequence is already closed
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<Long, E> flatmapToLong(final Throwables.Function<? super T, long[], ? extends E> mapper) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return create(new Throwables.Iterator<Long, E>() {
    //            private long[] cur = null;
    //            private int len = 0;
    //            private int idx = 0;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                while (idx >= len) {
    //                    if (elements.hasNext()) {
    //                        cur = mapper.apply(elements.next());
    //                        len = N.len(cur);
    //                        idx = 0;
    //                    } else {
    //                        cur = null;
    //                        break;
    //                    }
    //                }
    //
    //                return idx < len;
    //            }
    //
    //            @Override
    //            public Long next() throws E {
    //                if (idx >= len && !hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                return cur[idx++];
    //            }
    //        }, closeHandlers);
    //    }
    //
    //    /**
    //     * Transforms the elements of this stream using the provided mapper function,
    //     * which returns an array of float values for each element. The resulting arrays
    //     * are then flattened into a single stream of float values.
    //     *
    //     * @param mapper the function to apply to each element, which returns an array of float values
    //     * @return a new Seq containing the flattened float values
    //     * @throws IllegalStateException if the sequence is already closed
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<Float, E> flatmapToFloat(final Throwables.Function<? super T, float[], ? extends E> mapper) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return create(new Throwables.Iterator<Float, E>() {
    //            private float[] cur = null;
    //            private int len = 0;
    //            private int idx = 0;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                while (idx >= len) {
    //                    if (elements.hasNext()) {
    //                        cur = mapper.apply(elements.next());
    //                        len = N.len(cur);
    //                        idx = 0;
    //                    } else {
    //                        cur = null;
    //                        break;
    //                    }
    //                }
    //
    //                return idx < len;
    //            }
    //
    //            @Override
    //            public Float next() throws E {
    //                if (idx >= len && !hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                return cur[idx++];
    //            }
    //        }, closeHandlers);
    //    }
    //
    //    /**
    //     * Transforms the elements of this stream using the provided mapper function,
    //     * which returns an array of double values for each element. The resulting arrays
    //     * are then flattened into a single stream of double values.
    //     *
    //     * @param mapper the function to apply to each element, which returns an array of double values
    //     * @return a new Seq containing the flattened double values
    //     * @throws IllegalStateException if the sequence is already closed
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<Double, E> flatmapToDouble(final Throwables.Function<? super T, double[], ? extends E> mapper) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return create(new Throwables.Iterator<Double, E>() {
    //            private double[] cur = null;
    //            private int len = 0;
    //            private int idx = 0;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                while (idx >= len) {
    //                    if (elements.hasNext()) {
    //                        cur = mapper.apply(elements.next());
    //                        len = N.len(cur);
    //                        idx = 0;
    //                    } else {
    //                        cur = null;
    //                        break;
    //                    }
    //                }
    //
    //                return idx < len;
    //            }
    //
    //            @Override
    //            public Double next() throws E {
    //                if (idx >= len && !hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                return cur[idx++];
    //            }
    //        }, closeHandlers);
    //    }

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
     * <p>
     * Note: copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a>
     * </p>
     *
     * Transforms the elements of this sequence using the provided mapper function,
     * which returns an Optional of new elements for each element. Only present
     * values are included in the resulting stream.
     *
     * @param <R> the type of the elements in the new stream
     * @param mapper the function to apply to each element, which returns an Optional of new elements
     * @return a new sequence containing the transformed elements that are present
     * @throws IllegalStateException if the sequence is already closed
     */
    @SuppressWarnings("rawtypes")
    @Beta
    @IntermediateOp
    public <R> Seq<R, E> mapPartial(final Throwables.Function<? super T, Optional<? extends R>, E> mapper) throws IllegalStateException {
        //noinspection resource
        return map(mapper).filter((Throwables.Predicate) IS_PRESENT_IT).map(GET_AS_IT);
    }

    /**
     * <p>
     * Note: copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a>
     * </p>
     *
     * Transforms the elements of this sequence using the provided mapper function,
     * which returns an OptionalInt of new elements for each element. Only present
     * values are included in the resulting stream.
     *
     * @param mapper the function to apply to each element, which returns an OptionalInt of new elements
     * @return a new sequence containing the transformed elements that are present
     * @throws IllegalStateException if the sequence is already
     */
    @SuppressWarnings("rawtypes")
    @Beta
    @IntermediateOp
    public Seq<Integer, E> mapPartialToInt(final Throwables.Function<? super T, OptionalInt, E> mapper) throws IllegalStateException {
        //noinspection resource
        return map(mapper).filter((Throwables.Predicate) IS_PRESENT_INT).map(GET_AS_INT);
    }

    /**
     * <p>
     * Note: copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a>
     * </p>
     *
     * Transforms the elements of this sequence using the provided mapper function,
     * which returns an OptionalLong of new elements for each element. Only present
     * values are included in the resulting stream.
     *
     * @param mapper the function to apply to each element, which returns an OptionalLong of new elements
     * @return a new sequence containing the transformed elements that are present
     * @throws IllegalStateException if the sequence is already
     */
    @SuppressWarnings("rawtypes")
    @Beta
    @IntermediateOp
    public Seq<Long, E> mapPartialToLong(final Throwables.Function<? super T, OptionalLong, E> mapper) throws IllegalStateException {
        //noinspection resource
        return map(mapper).filter((Throwables.Predicate) IS_PRESENT_LONG).map(GET_AS_LONG);
    }

    /**
     * <p>
     * Note: copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a>
     * </p>
     *
     * Transforms the elements of this sequence using the provided mapper function,
     * which returns an OptionalDouble of new elements for each element. Only present
     * values are included in the resulting stream.
     *
     * @param mapper the function to apply to each element, which returns an OptionalDouble of new elements
     * @return a new sequence containing the transformed elements that are present
     * @throws IllegalStateException if the sequence is already
     */
    @SuppressWarnings("rawtypes")
    @Beta
    @IntermediateOp
    public Seq<Double, E> mapPartialToDouble(final Throwables.Function<? super T, OptionalDouble, E> mapper) throws IllegalStateException {
        //noinspection resource
        return map(mapper).filter((Throwables.Predicate) IS_PRESENT_DOUBLE).map(GET_AS_DOUBLE);
    }

    //    /**
    //     * Transforms the elements of this stream using the provided mapper function,
    //     * which returns a java.util.Optional of new elements for each element. Only present
    //     * values are included in the resulting stream.
    //     *
    //     * @param <R> the type of the elements in the new stream
    //     * @param mapper the function to apply to each element, which returns a java.util.Optional of new elements
    //     * @return a new Seq containing the transformed elements that are present
    //     */
    //    @SuppressWarnings("rawtypes")
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, E> mapPartialJdk(final Throwables.Function<? super T, java.util.Optional<? extends R>, E> mapper) {
    //        return map(mapper).filter((Throwables.Predicate) IS_PRESENT_IT_JDK).map(GET_AS_IT_JDK);
    //    }
    //
    //    /**
    //     * Transforms the elements of this stream using the provided mapper function,
    //     * which returns a java.util.OptionalInt of new elements for each element. Only present
    //     * values are included in the resulting stream.
    //     *
    //     * @param mapper the function to apply to each element, which returns a java.util.OptionalInt of new elements
    //     * @return a new Seq containing the transformed elements that are present
    //     */
    //    @SuppressWarnings("rawtypes")
    //    @Beta
    //    @IntermediateOp
    //    public Seq<Integer, E> mapPartialToIntJdk(final Throwables.Function<? super T, java.util.OptionalInt, E> mapper) {
    //        return map(mapper).filter((Throwables.Predicate) IS_PRESENT_INT_JDK).map(GET_AS_INT_JDK);
    //    }
    //
    //    /**
    //     * Transforms the elements of this stream using the provided mapper function,
    //     * which returns a java.util.OptionalLong of new elements for each element. Only present
    //     * values are included in the resulting stream.
    //     *
    //     * @param mapper the function to apply to each element, which returns a java.util.OptionalLong of new elements
    //     * @return a new Seq containing the transformed elements that are present
    //     */
    //    @SuppressWarnings("rawtypes")
    //    @Beta
    //    @IntermediateOp
    //    public Seq<Long, E> mapPartialToLongJdk(final Throwables.Function<? super T, java.util.OptionalLong, E> mapper) {
    //        return map(mapper).filter((Throwables.Predicate) IS_PRESENT_LONG_JDK).map(GET_AS_LONG_JDK);
    //    }
    //
    //    /**
    //     * Transforms the elements of this stream using the provided mapper function,
    //     * which returns a java.util.OptionalDouble of new elements for each element. Only present
    //     * values are included in the resulting stream.
    //     *
    //     * @param mapper the function to apply to each element, which returns a java.util.OptionalDouble of new elements
    //     * @return a new Seq containing the transformed elements that are present
    //     */
    //    @SuppressWarnings("rawtypes")
    //    @Beta
    //    @IntermediateOp
    //    public Seq<Double, E> mapPartialToDoubleJdk(final Throwables.Function<? super T, java.util.OptionalDouble, E> mapper) {
    //        return map(mapper).filter((Throwables.Predicate) IS_PRESENT_DOUBLE_JDK).map(GET_AS_DOUBLE_JDK);
    //    }

    /**
     * Transforms the elements of this sequence using the provided mapper function,
     * which accepts an element and a consumer to add multiple elements to the resulting stream.
     *
     * @param <R> the type of the elements in the new stream
     * @param mapper the function to apply to each element, which accepts an element and a consumer to add multiple elements
     * @return a new sequence containing the transformed elements
     * @throws IllegalStateException if the sequence is already closed
     */
    public <R> Seq<R, E> mapMulti(final Throwables.BiConsumer<? super T, ? super Consumer<R>, ? extends E> mapper) throws IllegalStateException {
        final Deque<R> queue = new ArrayDeque<>();

        final Consumer<R> consumer = queue::offer;

        @SuppressWarnings("resource")
        final Throwables.Iterator<T, E> iter = iteratorEx();

        return create(new Throwables.Iterator<>() {
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
     * Transforms the elements of this sequence using the provided mapper function,
     * which accepts two consecutive elements and returns a new element.
     *
     * @param <R> the type of the elements in the new stream
     * @param mapper the function to apply to each pair of consecutive elements
     * @return a new sequence containing the transformed elements
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public <R> Seq<R, E> slidingMap(final Throwables.BiFunction<? super T, ? super T, R, ? extends E> mapper) throws IllegalStateException {
        return slidingMap(1, mapper);
    }

    /**
     * Transforms the elements of this sequence using the provided mapper function,
     * which accepts two consecutive elements and returns a new element.
     *
     * @param <R> the type of the elements in the new stream
     * @param increment the number of elements to skip between each pair
     * @param mapper the function to apply to each pair of consecutive elements
     * @return a new sequence containing the transformed elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the increment is not positive
     */
    @IntermediateOp
    public <R> Seq<R, E> slidingMap(final int increment, final Throwables.BiFunction<? super T, ? super T, R, ? extends E> mapper)
            throws IllegalStateException, IllegalArgumentException {
        return slidingMap(increment, false, mapper);
    }

    /**
     * Transforms the elements of this sequence using the provided mapper function,
     * which accepts two consecutive elements and returns a new element.
     *
     * @param <R> the type of the elements in the new stream
     * @param increment the number of elements to skip between each pair
     * @param ignoreNotPaired whether to ignore elements that cannot be paired
     * @param mapper the function to apply to each pair of consecutive elements
     * @return a new sequence containing the transformed elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the increment is not positive
     */
    @IntermediateOp
    public <R> Seq<R, E> slidingMap(final int increment, final boolean ignoreNotPaired,
            final Throwables.BiFunction<? super T, ? super T, R, ? extends E> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(increment, cs.increment);

        final int windowSize = 2;

        return create(new Throwables.Iterator<>() {
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
     * Transforms the elements of this sequence using the provided mapper function,
     * which accepts three consecutive elements and returns a new element.
     *
     * @param <R> the type of the elements in the new stream
     * @param mapper the function to apply to each triplet of consecutive elements
     * @return a new sequence containing the transformed elements
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public <R> Seq<R, E> slidingMap(final Throwables.TriFunction<? super T, ? super T, ? super T, R, ? extends E> mapper) throws IllegalStateException {
        return slidingMap(1, mapper);
    }

    /**
     * Transforms the elements of this sequence using the provided mapper function,
     * which accepts three consecutive elements and returns a new element.
     *
     * @param <R> the type of the elements in the new stream
     * @param increment the number of elements to skip between each triplet
     * @param mapper the function to apply to each triplet of consecutive elements
     * @return a new sequence containing the transformed elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the increment is not positive
     */
    @IntermediateOp
    public <R> Seq<R, E> slidingMap(final int increment, final Throwables.TriFunction<? super T, ? super T, ? super T, R, ? extends E> mapper)
            throws IllegalStateException, IllegalArgumentException {
        return slidingMap(increment, false, mapper);
    }

    /**
     * Transforms the elements of this sequence using the provided mapper function,
     * which accepts three consecutive elements and returns a new element.
     *
     * @param <R> the type of the elements in the new stream
     * @param increment the number of elements to skip between each triplet
     * @param ignoreNotPaired whether to ignore elements that cannot be paired
     * @param mapper the function to apply to each triplet of consecutive elements
     * @return a new sequence containing the transformed elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the increment is not positive
     */
    @IntermediateOp
    public <R> Seq<R, E> slidingMap(final int increment, final boolean ignoreNotPaired,
            final Throwables.TriFunction<? super T, ? super T, ? super T, R, ? extends E> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(increment, cs.increment);

        final int windowSize = 3;

        return create(new Throwables.Iterator<>() {
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
     * Groups the elements of this sequence by a key extracted using the provided key extractor function.
     * The resulting sequence contains entries where each key is associated with a list of elements that share that key.
     *
     * @param <K> the type of the keys
     * @param keyMapper the function to extract the key from the elements
     * @return a new sequence containing entries where each key is associated with a list of elements that share that key
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K> Seq<Map.Entry<K, List<T>>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper) throws IllegalStateException {
        return groupBy(keyMapper, Suppliers.ofMap());
    }

    /**
     * Groups the elements of this sequence by a key extracted using the provided key extractor function.
     * The resulting sequence contains entries where each key is associated with a list of elements that share that key.
     *
     * @param <K> the type of the keys
     * @param keyMapper the function to extract the key from the elements
     * @param mapFactory the supplier to create a new map instance
     * @return a new sequence containing entries where each key is associated with a list of elements that share that key
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K> Seq<Map.Entry<K, List<T>>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Supplier<? extends Map<K, List<T>>> mapFactory) throws IllegalStateException {
        return groupBy(keyMapper, Fnn.identity(), mapFactory);
    }

    /**
     * Groups the elements of this sequence by a key extracted using the provided key extractor function
     * and a value extracted using the provided value extractor function.
     * The resulting sequence contains entries where each key is associated with a list of values that share that key.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param keyMapper the function to extract the key from the elements
     * @param valueMapper the function to extract the value from the elements
     * @return a new sequence containing entries where each key is associated with a list of values that share that key
     * @throws IllegalStateException if the sequence is already closed
     * @see Collectors#toMultimap(Function, Function)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K, V> Seq<Map.Entry<K, List<V>>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.Function<? super T, ? extends V, ? extends E> valueMapper) throws IllegalStateException {
        return groupBy(keyMapper, valueMapper, Suppliers.ofMap());
    }

    /**
     * Groups the elements of this sequence by a key extracted using the provided key extractor function
     * and a value extracted using the provided value extractor function.
     * The resulting sequence contains entries where each key is associated with a list of values that share that key.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param keyMapper the function to extract the key from the elements
     * @param valueMapper the function to extract the value from the elements
     * @param mapFactory the supplier to create a new map instance
     * @return a new sequence containing entries where each key is associated with a list of values that share that key
     * @throws IllegalStateException if the sequence is already closed
     * @see Collectors#toMultimap(Function, Function)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K, V> Seq<Map.Entry<K, List<V>>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.Function<? super T, ? extends V, ? extends E> valueMapper, final Supplier<? extends Map<K, List<V>>> mapFactory)
            throws IllegalStateException {
        assertNotClosed();

        return create(new Throwables.Iterator<>() {
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
                    iter = Seq.this.groupTo(keyMapper, valueMapper, mapFactory).entrySet().iterator();
                }
            }
        }, closeHandlers);

    }

    /**
     * Groups the elements of this sequence by a key extracted using the provided key extractor function
     * and a value extracted using the provided value extractor function.
     * The resulting sequence contains entries where each key is associated with a value that share that key.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param keyMapper the function to extract the key from the elements
     * @param valueMapper the function to extract the value from the elements
     * @param mergeFunction the function to merge values with the same key
     * @return a new sequence containing entries where each key is associated with a value that share that key
     * @throws IllegalStateException if the sequence is already closed
     * @see Collectors#toMap(Function, Function, BinaryOperator)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K, V> Seq<Map.Entry<K, V>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.Function<? super T, ? extends V, ? extends E> valueMapper, final Throwables.BinaryOperator<V, ? extends E> mergeFunction)
            throws IllegalStateException {
        return groupBy(keyMapper, valueMapper, mergeFunction, Suppliers.ofMap());
    }

    /**
     * Groups the elements of this sequence by a key extracted using the provided key extractor function
     * and a value extracted using the provided value extractor function.
     * The resulting sequence contains entries where each key is associated with a value that share that key.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param keyMapper the function to extract the key from the elements
     * @param valueMapper the function to extract the value from the elements
     * @param mergeFunction the function to merge values with the same key
     * @param mapFactory the supplier to create a new map instance
     * @return a new sequence containing entries where each key is associated with a value that share that key
     * @throws IllegalStateException if the sequence is already closed
     * @see Collectors#toMap(Function, Function, BinaryOperator)
     * @see {@link Fn.Fnn#throwingMerger()}
     * @see {@link Fn.Fnn#replacingMerger()}
     * @see {@link Fn.Fnn#ignoringMerger()}
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K, V> Seq<Map.Entry<K, V>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.Function<? super T, ? extends V, ? extends E> valueMapper, final Throwables.BinaryOperator<V, ? extends E> mergeFunction,
            final Supplier<? extends Map<K, V>> mapFactory) throws IllegalStateException {
        assertNotClosed();

        return create(new Throwables.Iterator<>() {
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
                    iter = Seq.this.toMap(keyMapper, valueMapper, mergeFunction, mapFactory).entrySet().iterator();
                }
            }
        }, closeHandlers);
    }

    /**
     * Groups the elements of this sequence by a key extracted using the provided key extractor function,
     * and collects the results using the provided downstream collector.
     * The resulting sequence contains entries where each key is associated with the result of the downstream collector.
     *
     * @param <K> the type of the keys
     * @param <D> the type of the downstream collector result
     * @param keyMapper the function to extract the key from the elements
     * @param downstream the collector to accumulate the results
     * @return a new sequence containing entries where each key is associated with the result of the downstream collector
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K, D> Seq<Map.Entry<K, D>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Collector<? super T, ?, D> downstream) throws IllegalStateException {
        return groupBy(keyMapper, downstream, Suppliers.ofMap());
    }

    /**
     * Groups the elements of this sequence by a key extracted using the provided key extractor function,
     * and collects the results using the provided downstream collector.
     * The resulting sequence contains entries where each key is associated with the result of the downstream collector.
     *
     * @param <K> the type of the keys
     * @param <D> the type of the downstream collector result
     * @param keyMapper the function to extract the key from the elements
     * @param downstream the collector to accumulate the results
     * @param mapFactory the supplier to create a new map instance
     * @return a new sequence containing entries where each key is associated with the result of the downstream collector
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K, D> Seq<Map.Entry<K, D>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Collector<? super T, ?, D> downstream, final Supplier<? extends Map<K, D>> mapFactory) throws IllegalStateException {
        return groupBy(keyMapper, Fnn.identity(), downstream, mapFactory);
    }

    /**
     * Groups the elements of this sequence by a key extracted using the provided key extractor function,
     * and collects the values extracted by the provided value extractor using the provided downstream collector.
     * The resulting sequence contains entries where each key is associated with the result of the downstream collector.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param <D> the type of the downstream collector result
     * @param keyMapper the function to extract the key from the elements
     * @param valueMapper the function to extract the value from the elements
     * @param downstream the collector to accumulate the results
     * @return a new sequence containing entries where each key is associated with the result of the downstream collector
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K, V, D> Seq<Map.Entry<K, D>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.Function<? super T, ? extends V, ? extends E> valueMapper, final Collector<? super V, ?, D> downstream)
            throws IllegalStateException {
        return groupBy(keyMapper, valueMapper, downstream, Suppliers.ofMap());
    }

    /**
     * Groups the elements of this sequence by a key extracted using the provided key extractor function,
     * and collects the values extracted by the provided value extractor using the provided downstream collector.
     * The resulting sequence contains entries where each key is associated with the result of the downstream collector.
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values
     * @param <D> the type of the downstream collector result
     * @param keyMapper the function to extract the key from the elements
     * @param valueMapper the function to extract the value from the elements
     * @param downstream the collector to accumulate the results
     * @param mapFactory the supplier to create a new map instance
     * @return a new sequence containing entries where each key is associated with the result of the downstream collector
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K, V, D> Seq<Map.Entry<K, D>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.Function<? super T, ? extends V, ? extends E> valueMapper, final Collector<? super V, ?, D> downstream,
            final Supplier<? extends Map<K, D>> mapFactory) throws IllegalStateException {
        assertNotClosed();

        return create(new Throwables.Iterator<>() {
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
                    iter = Seq.this.groupTo(keyMapper, valueMapper, downstream, mapFactory).entrySet().iterator();
                }
            }
        }, closeHandlers);
    }

    /**
     * Partitions the elements of this sequence into two groups based on the provided predicate.
     * The resulting sequence will always contain two entries where the key is a boolean indicating the group,
     * and the value is a list of elements in that group.
     *
     * @param predicate the predicate to apply to each element to determine the group
     * @return a new sequence containing entries where the key is a boolean indicating the group,
     *         and the value is a list of elements in that group
     * @throws IllegalStateException if the sequence is already closed
     * @see Collectors#partitioningBy(Predicate)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<Map.Entry<Boolean, List<T>>, E> partitionBy(final Throwables.Predicate<? super T, E> predicate) throws IllegalStateException {
        assertNotClosed();

        return partitionBy(predicate, Collectors.toList());
    }

    /**
     * Partitions the elements of this sequence into two groups based on the provided predicate,
     * and collects the results using the provided downstream collector.
     * The resulting sequence will always contain two entries where the key is a boolean indicating the group,
     * and the value is the result of the downstream collector.
     *
     * @param <D> the type of the downstream collector result
     * @param predicate the predicate to apply to each element to determine the group
     * @param downstream the collector to accumulate the results
     * @return a new sequence containing entries where the key is a boolean indicating the group,
     *         and the value is the result of the downstream collector
     * @throws IllegalStateException if the sequence is already closed
     * @see Collectors#partitioningBy(Predicate, Collector)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <D> Seq<Map.Entry<Boolean, D>, E> partitionBy(final Throwables.Predicate<? super T, E> predicate, final Collector<? super T, ?, D> downstream)
            throws IllegalStateException {
        assertNotClosed();

        return create(new Throwables.Iterator<>() {
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
                    iter = Seq.this.partitionTo(predicate, downstream).entrySet().iterator();
                }
            }
        }, closeHandlers);
    }

    /**
     * Counts the elements of this sequence by a key extracted using the provided key extractor function.
     * The resulting sequence contains entries where each key is associated with the count of elements that share that key.
     *
     * @param <K> the type of the keys
     * @param keyMapper the function to extract the key from the elements
     * @return a new sequence containing entries where each key is associated with the count of elements that share that key
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K> Seq<Map.Entry<K, Integer>, E> countBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper) throws IllegalStateException {
        return groupBy(keyMapper, Collectors.countingToInt());
    }

    /**
     * Counts the elements of this sequence by a key extracted using the provided key extractor function.
     * The resulting sequence contains entries where each key is associated with the count of elements that share that key.
     *
     * @param <K> the type of the keys
     * @param keyMapper the function to extract the key from the elements
     * @param mapFactory the supplier to create a new map instance
     * @return a new sequence containing entries where each key is associated with the count of elements that share that key
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K> Seq<Map.Entry<K, Integer>, E> countBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Supplier<? extends Map<K, Integer>> mapFactory) throws IllegalStateException {
        return groupBy(keyMapper, Collectors.countingToInt(), mapFactory);
    }

    //    /**
    //     * Merges a series of adjacent elements in the stream which satisfy the given predicate into a List.
    //     * The predicate takes two parameters: the previous element and the current element in the stream.
    //     * If the predicate returns {@code true}, the current element and its previous element are considered as a series of adjacent elements.
    //     * These elements are then collapsed into a List.
    //     *
    //     * <br />
    //     * This is an intermediate operation and will not close the sequence.
    //     * It's also a stateful operation since it needs to remember the previous element when processing the current element.
    //     *
    //     * This operation is not parallelizable and requires the stream to be ordered.
    //     *
    //     * @param collapsible a BiPredicate that takes two parameters: the previous element and the current element in the stream.
    //     * @return a new Stream where each element is a List of adjacent elements which satisfy the given predicate.
    //     * @see Stream#collapse(java.util.function.BiPredicate)
    //     */
    //    @IntermediateOp
    //    public Seq<List<T>, E> collapse(final Throwables.BiPredicate<? super T, ? super T, ? extends E> collapsible) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return collapse(collapsible, Suppliers.ofList());
    //    }
    //
    //    /**
    //     * Merges a series of adjacent elements in the stream which satisfy the given predicate into a Collection.
    //     * The predicate takes two parameters: the previous element and the current element in the stream.
    //     * If the predicate returns {@code true}, the current element and its previous element are considered as a series of adjacent elements.
    //     * These elements are then collapsed into a Collection.
    //     *
    //     * <br />
    //     * This is an intermediate operation and will not close the sequence.
    //     * It's also a stateful operation since it needs to remember the previous element when processing the current element.
    //     *
    //     * This operation is not parallelizable and requires the stream to be ordered.
    //     *
    //     * @param <C> the type of the Collection into which the adjacent elements will be collapsed.
    //     * @param collapsible a BiPredicate that takes two parameters: the previous element and the current element in the stream.
    //     * @param supplier a Supplier that generates the Collection into which the adjacent elements will be collapsed.
    //     * @return a new Stream where each element is a Collection of adjacent elements which satisfy the given predicate.
    //     * @see Stream#collapse(java.util.function.BiPredicate, Supplier)
    //     */
    //    @IntermediateOp
    //    public <C extends Collection<T>> Seq<C, E> collapse(final Throwables.BiPredicate<? super T, ? super T, ? extends E> collapsible,
    //            final Supplier<? extends C> supplier) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        final Throwables.Iterator<T, E> iter = elements;
    //
    //        return create(new Throwables.Iterator<C, E>() {
    //            private boolean hasNext = false;
    //            private T next = null;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                return hasNext || iter.hasNext();
    //            }
    //
    //            @Override
    //            public C next() throws E {
    //                final C c = supplier.get();
    //                c.add(hasNext ? next : (next = iter.next()));
    //
    //                while ((hasNext = iter.hasNext())) {
    //                    if (collapsible.test(next, (next = iter.next()))) {
    //                        c.add(next);
    //                    } else {
    //                        break;
    //                    }
    //                }
    //
    //                return c;
    //            }
    //        }, closeHandlers);
    //    }
    //
    //    /**
    //     * Merges a series of adjacent elements in the stream which satisfy the given predicate using the merger function and returns a new stream.
    //     * The predicate takes two parameters: the previous element and the current element in the stream.
    //     * If the predicate returns {@code true}, the current element and its previous element are considered as a series of adjacent elements.
    //     * These elements are then merged using the provided BiFunction.
    //     *
    //     * <br />
    //     * This is an intermediate operation and will not close the sequence.
    //     * It's also a stateful operation since it needs to remember the previous element when processing the current element.
    //     *
    //     * This operation is not parallelizable and requires the stream to be ordered.
    //     *
    //     * <p>Example:
    //     * <pre>
    //     * <code>
    //     * Stream.of(new Integer[0]).collapse((p, c) -> p &lt; c, (r, c) -> r + c) => []
    //     * Stream.of(1).collapse((p, c) -> p &lt; c, (r, c) -> r + c) => [1]
    //     * Stream.of(1, 2).collapse((p, c) -> p &lt; c, (r, c) -> r + c) => [3]
    //     * Stream.of(1, 2, 3).collapse((p, c) -> p &lt; c, (r, c) -> r + c) => [6]
    //     * Stream.of(1, 2, 3, 3, 2, 1).collapse((p, c) -> p &lt; c, (r, c) -> r + c) => [6, 3, 2, 1]
    //     * </code>
    //     * </pre>
    //     *
    //     * @param collapsible a BiPredicate that takes two parameters: the previous element and the current element in the stream.
    //     * @param mergeFunction a BiFunction that takes two parameters: the result of the previous merge operation (or the first element if no merge has been performed yet) and the current element, and returns the result of the merge operation.
    //     * @return a new Stream where each element is the result of merging adjacent elements which satisfy the given predicate.
    //     * @see Stream#collapse(java.util.function.BiPredicate, java.util.function.BiFunction)
    //     */
    //    @IntermediateOp
    //    public Seq<T, E> collapse(final Throwables.BiPredicate<? super T, ? super T, ? extends E> collapsible,
    //            final Throwables.BiFunction<? super T, ? super T, T, ? extends E> mergeFunction) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        final Throwables.Iterator<T, E> iter = elements;
    //
    //        return create(new Throwables.Iterator<T, E>() {
    //            private boolean hasNext = false;
    //            private T next = null;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                return hasNext || iter.hasNext();
    //            }
    //
    //            @Override
    //            public T next() throws E {
    //                T res = hasNext ? next : (next = iter.next());
    //
    //                while ((hasNext = iter.hasNext())) {
    //                    if (collapsible.test(next, (next = iter.next()))) {
    //                        res = mergeFunction.apply(res, next);
    //                    } else {
    //                        break;
    //                    }
    //                }
    //
    //                return res;
    //            }
    //        }, closeHandlers);
    //    }
    //
    //    /**
    //     * Merges a series of adjacent elements in the stream which satisfy the given predicate into a single element using the provided operation and returns a new stream.
    //     * The predicate takes two parameters: the previous element and the current element in the stream.
    //     * If the predicate returns {@code true}, the current element and its previous element are considered as a series of adjacent elements.
    //     * These elements are then merged using the provided BiFunction.
    //     *
    //     * <br />
    //     * This is an intermediate operation and will not close the sequence.
    //     * It's also a stateful operation since it needs to remember the previous element when processing the current element.
    //     *
    //     * This operation is not parallelizable and requires the stream to be ordered.
    //     * @param <U> the type of the resulting elements.
    //     * @param collapsible a BiPredicate that takes two parameters: the previous element and the current element in the stream.
    //     * @param init the initial value to be used in the BiFunction for the first element if the predicate returns {@code true}.
    //     * @param mergeFunction a BiFunction that takes two parameters: the initial value or the result of the merge function from the previous step, and the current element. It returns a single element that represents the collapsed elements.
    //     * @return a new Stream where each element is the result of merging adjacent elements which satisfy the given predicate.
    //     * @see Stream#collapse(java.util.function.BiPredicate, java.lang.Object, java.util.function.BiFunction)
    //     */
    //    @IntermediateOp
    //    public <U> Seq<U, E> collapse(final Throwables.BiPredicate<? super T, ? super T, ? extends E> collapsible, final U init,
    //            final Throwables.BiFunction<? super U, ? super T, U, ? extends E> mergeFunction) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        final Throwables.Iterator<T, E> iter = elements;
    //
    //        return create(new Throwables.Iterator<U, E>() {
    //            private boolean hasNext = false;
    //            private T next = null;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                return hasNext || iter.hasNext();
    //            }
    //
    //            @Override
    //            public U next() throws E {
    //                U res = mergeFunction.apply(init, hasNext ? next : (next = iter.next()));
    //
    //                while ((hasNext = iter.hasNext())) {
    //                    if (collapsible.test(next, (next = iter.next()))) {
    //                        res = mergeFunction.apply(res, next);
    //                    } else {
    //                        break;
    //                    }
    //                }
    //
    //                return res;
    //            }
    //        }, closeHandlers);
    //    }
    //
    //    //    /**
    //    //     *
    //    //     *
    //    //     * @param <R>
    //    //     * @param collapsible test the current element with its previous element. The first parameter is the previous element of current element, the second parameter is the current element.
    //    //     * @param supplier
    //    //     * @param accumulator
    //    //     * @return
    //    //     * @throws IllegalStateException
    //    //     * @deprecated use {@linkplain #collapse(com.landawn.abacus.util.Throwables.BiPredicate, Collector)} instead. 1, parameter position is inconsistent? {@code supplier} should be last parameter, 2 Too many overload methods? 3, not frequently used?
    //    //     */
    //    //    @Deprecated
    //    //    @IntermediateOp
    //    //    public <R> Seq<R, E> collapse(final Throwables.BiPredicate<? super T, ? super T, ? extends E> collapsible, final Supplier<R> supplier,
    //    //            final Throwables.BiConsumer<? super R, ? super T, ? extends E> accumulator) throws IllegalStateException {
    //    //        assertNotClosed();
    //    //
    //    //        final Throwables.Iterator<T, E> iter = elements;
    //    //
    //    //        return create(new Throwables.Iterator<R, E>() {
    //    //            private boolean hasNext = false;
    //    //            private T next = null;
    //    //
    //    //            @Override
    //    //            public boolean hasNext() throws E {
    //    //                return hasNext || iter.hasNext();
    //    //            }
    //    //
    //    //            @Override
    //    //            public R next() throws E {
    //    //                final R container = supplier.get();
    //    //                accumulator.accept(container, hasNext ? next : (next = iter.next()));
    //    //
    //    //                while ((hasNext = iter.hasNext())) {
    //    //                    if (collapsible.test(next, (next = iter.next()))) {
    //    //                        accumulator.accept(container, next);
    //    //                    } else {
    //    //                        break;
    //    //                    }
    //    //                }
    //    //
    //    //                return container;
    //    //            }
    //    //        }, closeHandlers);
    //    //    }
    //
    //    /**
    //     * Merges a series of adjacent elements in the stream which satisfy the given predicate into a single element using the provided collector and returns a new stream.
    //     * The predicate takes two parameters: the previous element and the current element in the stream.
    //     * If the predicate returns {@code true}, the current element and its previous element are considered as a series of adjacent elements.
    //     * These elements are then collapsed into a single element using the provided collector.
    //     *
    //     * <br />
    //     * This is an intermediate operation and will not close the sequence.
    //     * It's also a stateful operation since it needs to remember the previous element when processing the current element.
    //     *
    //     * This operation is not parallelizable and requires the stream to be ordered.
    //     *
    //     * <p>Example:
    //     * <pre>
    //     * <code>
    //     * Stream.of(new Integer[0]).collapse((p, c) -> p &lt; c, Collectors.summingInt(Fn.unboxI())) => []
    //     * Stream.of(1).collapse((p, c) -> p &lt; c, Collectors.summingInt(Fn.unboxI())) => [1]
    //     * Stream.of(1, 2).collapse((p, c) -> p &lt; c, Collectors.summingInt(Fn.unboxI())) => [3]
    //     * Stream.of(1, 2, 3).collapse((p, c) -> p &lt; c, Collectors.summingInt(Fn.unboxI())) => [6]
    //     * Stream.of(1, 2, 3, 3, 2, 1).collapse((p, c) -> p &lt; c, Collectors.summingInt(Fn.unboxI())) => [6, 3, 2, 1]
    //     * </code>
    //     * </pre>
    //     *
    //     * @param collapsible a BiPredicate that takes two parameters: the previous element and the current element in the stream.
    //     * @param collector a Collector that collects the adjacent elements into a single element.
    //     * @return a new Stream where each element is the result of collapsing adjacent elements which satisfy the given predicate.
    //     * @see Stream#collapse(java.util.function.BiPredicate, Collector)
    //     */
    //    @IntermediateOp
    //    public <R> Seq<R, E> collapse(final Throwables.BiPredicate<? super T, ? super T, ? extends E> collapsible, final Collector<? super T, ?, R> collector)
    //            throws IllegalStateException {
    //        assertNotClosed();
    //
    //        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
    //        final BiConsumer<Object, ? super T> accumulator = (BiConsumer<Object, ? super T>) collector.accumulator();
    //        final Function<Object, R> finisher = (Function<Object, R>) collector.finisher();
    //
    //        final Throwables.Iterator<T, E> iter = elements;
    //
    //        return create(new Throwables.Iterator<R, E>() {
    //            private boolean hasNext = false;
    //            private T next = null;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                return hasNext || iter.hasNext();
    //            }
    //
    //            @Override
    //            public R next() throws E {
    //                final Object container = supplier.get();
    //                accumulator.accept(container, hasNext ? next : (next = iter.next()));
    //
    //                while ((hasNext = iter.hasNext())) {
    //                    if (collapsible.test(next, (next = iter.next()))) {
    //                        accumulator.accept(container, next);
    //                    } else {
    //                        break;
    //                    }
    //                }
    //
    //                return finisher.apply(container);
    //            }
    //        }, closeHandlers);
    //    }
    //
    //    /**
    //     * Merges a series of adjacent elements in the stream which satisfy the given predicate into a single list and returns a new stream.
    //     * The predicate takes three parameters: the first element of the series, the previous element and the current element in the stream.
    //     * If the predicate returns {@code true}, the current element, its previous element and the first element of the series are considered as a series of adjacent elements.
    //     * These elements are then collapsed into a single list.
    //     *
    //     * <br />
    //     * This is an intermediate operation and will not close the sequence.
    //     * It's also a stateful operation since it needs to remember the first and previous elements when processing the current element.
    //     *
    //     * This operation is not parallelizable and requires the stream to be ordered.
    //     *
    //     * @param collapsible a TriPredicate that takes three parameters: the first element of the series, the previous element and the current element in the stream.
    //     * @return a new Stream where each element is a list that is the result of collapsing adjacent elements which satisfy the given predicate.
    //     * @see Stream#collapse(com.landawn.abacus.util.function.TriPredicate)
    //     */
    //    @IntermediateOp
    //    public Seq<List<T>, E> collapse(final Throwables.TriPredicate<? super T, ? super T, ? super T, ? extends E> collapsible) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        final Throwables.Iterator<T, E> iter = elements;
    //
    //        return create(new Throwables.Iterator<List<T>, E>() {
    //            private boolean hasNext = false;
    //            private T next = null;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                return hasNext || iter.hasNext();
    //            }
    //
    //            @Override
    //            public List<T> next() throws E {
    //                final T first = hasNext ? next : (next = iter.next());
    //                final List<T> c = new ArrayList<>();
    //                c.add(first);
    //
    //                while ((hasNext = iter.hasNext())) {
    //                    if (collapsible.test(first, next, (next = iter.next()))) {
    //                        c.add(next);
    //                    } else {
    //                        break;
    //                    }
    //                }
    //
    //                return c;
    //            }
    //        }, closeHandlers);
    //    }
    //
    //    /**
    //     * Merges a series of adjacent elements in the stream which satisfy the given predicate into a single collection and returns a new stream.
    //     * The predicate takes three parameters: the first element of the series, the previous element and the current element in the stream.
    //     * If the predicate returns {@code true}, the current element, its previous element and the first element of the series are considered as a series of adjacent elements.
    //     * These elements are then collapsed into a single collection.
    //     *
    //     * <br />
    //     * This is an intermediate operation and will not close the sequence.
    //     * It's also a stateful operation since it needs to remember the first and previous elements when processing the current element.
    //     *
    //     * This operation is not parallelizable and requires the stream to be ordered.
    //     *
    //     * @param <C> the type of the Collection into which the adjacent elements will be collapsed
    //     * @param collapsible a TriPredicate that takes three parameters: the first element of the series, the previous element and the current element in the stream.
    //     * @param supplier a Supplier that generates the collection into which the adjacent elements will be collapsed.
    //     * @return a new Stream where each element is a collection that is the result of collapsing adjacent elements which satisfy the given predicate.
    //     * @see Stream#collapse(com.landawn.abacus.util.function.TriPredicate, Supplier)
    //     */
    //    @IntermediateOp
    //    public <C extends Collection<T>> Seq<C, E> collapse(final Throwables.TriPredicate<? super T, ? super T, ? super T, ? extends E> collapsible,
    //            final Supplier<? extends C> supplier) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        final Throwables.Iterator<T, E> iter = elements;
    //
    //        return create(new Throwables.Iterator<C, E>() {
    //            private boolean hasNext = false;
    //            private T next = null;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                return hasNext || iter.hasNext();
    //            }
    //
    //            @Override
    //            public C next() throws E {
    //                final T first = hasNext ? next : (next = iter.next());
    //                final C c = supplier.get();
    //                c.add(first);
    //
    //                while ((hasNext = iter.hasNext())) {
    //                    if (collapsible.test(first, next, (next = iter.next()))) {
    //                        c.add(next);
    //                    } else {
    //                        break;
    //                    }
    //                }
    //
    //                return c;
    //            }
    //        }, closeHandlers);
    //    }
    //
    //    /**
    //     * Merges a series of adjacent elements in the stream which satisfy the given predicate into a single element and returns a new stream.
    //     * The predicate takes three parameters: the first element of the series, the previous element and the current element in the stream.
    //     * If the predicate returns {@code true}, the current element, its previous element and the first element of the series are considered as a series of adjacent elements.
    //     * These elements are then collapsed into a single element using the provided merge function.
    //     *
    //     * <br />
    //     * This is an intermediate operation and will not close the sequence.
    //     * It's also a stateful operation since it needs to remember the first and previous elements when processing the current element.
    //     *
    //     * This operation is not parallelizable and requires the stream to be ordered.
    //     *
    //     * <p>Example:
    //     * <pre>
    //     * <code>
    //     * Stream.of(new Integer[0]).collapse((f, p, c) -> f &lt; c, (r, c) -> r + c) => []
    //     * Stream.of(1).collapse((f, p, c) -> f &lt; c, (r, c) -> r + c) => [1]
    //     * Stream.of(1, 2).collapse((f, p, c) -> f &lt; c, (r, c) -> r + c) => [3]
    //     * Stream.of(1, 2, 3).collapse((f, p, c) -> f &lt; c, (r, c) -> r + c) => [6]
    //     * Stream.of(1, 2, 3, 3, 2, 1).collapse((f, p, c) -> f &lt; c, (r, c) -> r + c) => [11, 1]
    //     * </code>
    //     * </pre>
    //     *
    //     * @param collapsible a TriPredicate that takes three parameters: the first element of the series, the previous element and the current element in the stream.
    //     * @param mergeFunction a BiFunction that takes two parameters: the current element and its previous element. It returns a single element that represents the collapsed elements.
    //     * @return a new Stream where each element is the result of collapsing adjacent elements which satisfy the given predicate.
    //     * @see Stream#collapse(java.util.function.TriPredicate, java.util.function.BiFunction)
    //     */
    //    @IntermediateOp
    //    public Seq<T, E> collapse(final Throwables.TriPredicate<? super T, ? super T, ? super T, ? extends E> collapsible,
    //            final Throwables.BiFunction<? super T, ? super T, T, ? extends E> mergeFunction) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        final Throwables.Iterator<T, E> iter = elements;
    //
    //        return create(new Throwables.Iterator<T, E>() {
    //            private boolean hasNext = false;
    //            private T next = null;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                return hasNext || iter.hasNext();
    //            }
    //
    //            @Override
    //            public T next() throws E {
    //                final T first = hasNext ? next : (next = iter.next());
    //                T res = first;
    //
    //                while ((hasNext = iter.hasNext())) {
    //                    if (collapsible.test(first, next, (next = iter.next()))) {
    //                        res = mergeFunction.apply(res, next);
    //                    } else {
    //                        break;
    //                    }
    //                }
    //
    //                return res;
    //            }
    //        }, closeHandlers);
    //    }
    //
    //    /**
    //     * Merges a series of adjacent elements in the stream which satisfy the given predicate into a single element and returns a new stream.
    //     * The predicate takes three parameters: the first element of the series, the previous element and the current element in the stream.
    //     * If the predicate returns {@code true}, the current element, its previous element and the first element of the series are considered as a series of adjacent elements.
    //     * These elements are then collapsed into a single element using the provided merge function.
    //     *
    //     * <br />
    //     * This is an intermediate operation and will not close the sequence.
    //     * It's also a stateful operation since it needs to remember the first and previous elements when processing the current element.
    //     *
    //     * This operation is not parallelizable and requires the stream to be ordered.
    //     *
    //     * @param collapsible a TriPredicate that takes three parameters: the first element of the series, the previous element and the current element in the stream.
    //     * @param init the initial value to be used in the merge function for the first element in the series.
    //     * @param mergeFunction a BiFunction that takes two parameters: the initial value or the result of the merge function from the previous step, and the current element. It returns a single element that represents the collapsed elements.
    //     * @return a new Stream where each element is the result of collapsing adjacent elements which satisfy the given predicate.
    //     * @see Stream#collapse(java.util.function.TriPredicate, java.lang.Object, java.util.function.BiFunction)
    //     */
    //    @IntermediateOp
    //    public <U> Seq<U, E> collapse(final Throwables.TriPredicate<? super T, ? super T, ? super T, ? extends E> collapsible, final U init,
    //            final Throwables.BiFunction<? super U, ? super T, U, ? extends E> mergeFunction) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        final Throwables.Iterator<T, E> iter = elements;
    //
    //        return create(new Throwables.Iterator<U, E>() {
    //            private boolean hasNext = false;
    //            private T next = null;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                return hasNext || iter.hasNext();
    //            }
    //
    //            @Override
    //            public U next() throws E {
    //                final T first = hasNext ? next : (next = iter.next());
    //                U res = mergeFunction.apply(init, first);
    //
    //                while ((hasNext = iter.hasNext())) {
    //                    if (collapsible.test(first, next, (next = iter.next()))) {
    //                        res = mergeFunction.apply(res, next);
    //                    } else {
    //                        break;
    //                    }
    //                }
    //
    //                return res;
    //            }
    //        }, closeHandlers);
    //    }

    //    /**
    //     *
    //     *
    //     * @param <R>
    //     * @param collapsible test the current element with the first element and previous element in the series. The first parameter is the first element of this series, the second parameter is the previous element and the third parameter is the current element.
    //     * @param supplier
    //     * @param accumulator
    //     * @return
    //     * @throws IllegalStateException
    //     * @throws IllegalStateException
    //     * @deprecated use {@linkplain #collapse(com.landawn.abacus.util.Throwables.TriPredicate, Collector)} instead. 1, parameter position is inconsistent? {@code supplier} should be last parameter, 2 Too many overload methods? 3, not frequently used?
    //     */
    //    @Deprecated
    //    @IntermediateOp
    //    public <R> Seq<R, E> collapse(final Throwables.TriPredicate<? super T, ? super T, ? super T, ? extends E> collapsible, final Supplier<R> supplier,
    //            final Throwables.BiConsumer<? super R, ? super T, ? extends E> accumulator) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        final Throwables.Iterator<T, E> iter = elements;
    //
    //        return create(new Throwables.Iterator<R, E>() {
    //            private boolean hasNext = false;
    //            private T next = null;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                return hasNext || iter.hasNext();
    //            }
    //
    //            @Override
    //            public R next() throws E {
    //                final T first = hasNext ? next : (next = iter.next());
    //                final R container = supplier.get();
    //                accumulator.accept(container, first);
    //
    //                while ((hasNext = iter.hasNext())) {
    //                    if (collapsible.test(first, next, (next = iter.next()))) {
    //                        accumulator.accept(container, next);
    //                    } else {
    //                        break;
    //                    }
    //                }
    //
    //                return container;
    //            }
    //        }, closeHandlers);
    //    }
    //
    //    /**
    //     * Merges a series of adjacent elements in the stream which satisfy the given predicate into a single element and returns a new stream.
    //     * The predicate takes three parameters: the first element of the series, the previous element and the current element in the stream.
    //     * If the predicate returns {@code true}, the current element, its previous element and the first element of the series are considered as a series of adjacent elements.
    //     * These elements are then collapsed into a single element using the provided collector.
    //     *
    //     * <br />
    //     * This is an intermediate operation and will not close the sequence.
    //     * It's also a stateful operation since it needs to remember the first and previous elements when processing the current element.
    //     *
    //     * This operation is not parallelizable and requires the stream to be ordered.
    //     *
    //     * <p>Example:
    //     * <pre>
    //     * <code>
    //     * Stream.of(new Integer[0]).collapse((f, p, c) -> f &lt; c, Collectors.summingInt(Fn.unboxI())) => []
    //     * Stream.of(1).collapse((f, p, c) -> f &lt; c, Collectors.summingInt(Fn.unboxI())) => [1]
    //     * Stream.of(1, 2).collapse((f, p, c) -> f &lt; c, Collectors.summingInt(Fn.unboxI())) => [3]
    //     * Stream.of(1, 2, 3).collapse((f, p, c) -> f &lt; c, Collectors.summingInt(Fn.unboxI())) => [6]
    //     * Stream.of(1, 2, 3, 3, 2, 1).collapse((f, p, c) -> f &lt; c, Collectors.summingInt(Fn.unboxI())) => [11, 1]
    //     * </code>
    //     * </pre>
    //     *
    //     * @param collapsible a TriPredicate that takes three parameters: the first element of the series, the previous element and the current element in the stream.
    //     * @param collector a Collector that collects the elements into a single result container.
    //     * @return a new Stream where each element is the result of collapsing adjacent elements which satisfy the given predicate.
    //     * @see Stream#collapse(com.landawn.abacus.util.function.TriPredicate, Collector)
    //     */
    //    @IntermediateOp
    //    public <R> Seq<R, E> collapse(final Throwables.TriPredicate<? super T, ? super T, ? super T, ? extends E> collapsible,
    //            final Collector<? super T, ?, R> collector) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
    //        final BiConsumer<Object, ? super T> accumulator = (BiConsumer<Object, ? super T>) collector.accumulator();
    //        final Function<Object, R> finisher = (Function<Object, R>) collector.finisher();
    //
    //        final Throwables.Iterator<T, E> iter = elements;
    //
    //        return create(new Throwables.Iterator<R, E>() {
    //            private boolean hasNext = false;
    //            private T next = null;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                return hasNext || iter.hasNext();
    //            }
    //
    //            @Override
    //            public R next() throws E {
    //                final T first = hasNext ? next : (next = iter.next());
    //                final Object container = supplier.get();
    //                accumulator.accept(container, first);
    //
    //                while ((hasNext = iter.hasNext())) {
    //                    if (collapsible.test(first, next, (next = iter.next()))) {
    //                        accumulator.accept(container, next);
    //                    } else {
    //                        break;
    //                    }
    //                }
    //
    //                return finisher.apply(container);
    //            }
    //        }, closeHandlers);
    //    }

    //    /**
    //     * Performs a scan (also known as prefix sum, cumulative sum, running total, or integral) operation on the elements of the stream.
    //     * The scan operation takes a binary operator (the accumulator) and applies it cumulatively on the stream elements,
    //     * successively combining each element in order from the start to produce a stream of accumulated results.
    //     *
    //     * <br />
    //     * This is an intermediate operation and will not close the sequence.
    //     *
    //     * For example, given a stream of numbers [1, 2, 3, 4], and an accumulator that performs addition, the output would be a stream of numbers [1, 3, 6, 10].
    //     *
    //     * <p>Example:
    //     * <pre>
    //     * <code>
    //     * Stream.of(new Integer[0]).scan((r, c) -> r + c) => []
    //     * Stream.of(1).scan((r, c) -> r + c) => [1]
    //     * Stream.of(1, 2).scan((r, c) -> r + c) => [1, 3]
    //     * Stream.of(1, 2, 3).scan((r, c) -> r + c) => [1, 3, 6]
    //     * Stream.of(1, 2, 3, 3, 2, 1).scan((r, c) -> r + c) => [1, 3, 6, 9, 11, 12]
    //     * </code>
    //     * </pre>
    //     *
    //     * @param accumulator a {@code BiFunction} that takes two parameters: the current accumulated value and the current stream element, and returns a new accumulated value.
    //     * @return a new {@code Seq} consisting of the results of the scan operation on the elements of the original stream.
    //     * @see Stream#scan(java.util.function.BiFunction)
    //     */
    //    @IntermediateOp
    //    public Seq<T, E> scan(final Throwables.BiFunction<? super T, ? super T, T, ? extends E> accumulator) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        final Throwables.Iterator<T, E> iter = elements;
    //
    //        return create(new Throwables.Iterator<T, E>() {
    //            private T res = null;
    //            private boolean isFirst = true;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                return iter.hasNext();
    //            }
    //
    //            @Override
    //            public T next() throws E {
    //                if (isFirst) {
    //                    isFirst = false;
    //                    return (res = iter.next());
    //                } else {
    //                    return (res = accumulator.apply(res, iter.next()));
    //                }
    //            }
    //        }, closeHandlers);
    //    }
    //
    //    /**
    //     * Performs a scan (also known as prefix sum, cumulative sum, running total, or integral) operation on the elements of the stream.
    //     * The scan operation takes a binary operator (the accumulator) and applies it cumulatively on the stream elements,
    //     * successively combining each element in order from the start to produce a stream of accumulated results.
    //     *
    //     * <br />
    //     * This is an intermediate operation and will not close the sequence.
    //     *
    //     * For example, given a stream of numbers [1, 2, 3, 4], an initial value of 10, and an accumulator that performs addition, the output would be a stream of numbers [11, 13, 16, 20].
    //     *
    //     * <p>Example:
    //     * <pre>
    //     * <code>
    //     * Stream.of(new Integer[0]).scan(10, (r, c) -> r + c) => []
    //     * Stream.of(1).scan(10, (r, c) -> r + c) => [11]
    //     * Stream.of(1, 2).scan(10, (r, c) -> r + c) => [11, 13]
    //     * Stream.of(1, 2, 3).scan(10, (r, c) -> r + c) => [11, 13, 16]
    //     * Stream.of(1, 2, 3, 3, 2, 1).scan(10, (r, c) -> r + c) => [11, 13, 16, 19, 21, 22]
    //     * </code>
    //     * </pre>
    //     *
    //     * @param init the initial value. It's only used once by the accumulator to calculate the first element in the returned stream. It will be ignored if this stream is empty and won't be the first element of the returned stream.
    //     * @param accumulator a {@code BiFunction} that takes two parameters: the current accumulated value and the current stream element, and returns a new accumulated value.
    //     * @return a new {@code Seq} consisting of the results of the scan operation on the elements of the original stream.
    //     * @see Stream#scan(java.lang.Object, java.util.function.BiFunction)
    //     */
    //    @IntermediateOp
    //    public <U> Seq<U, E> scan(final U init, final Throwables.BiFunction<? super U, ? super T, U, ? extends E> accumulator) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        final Throwables.Iterator<T, E> iter = elements;
    //
    //        return create(new Throwables.Iterator<U, E>() {
    //            private U res = init;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                return iter.hasNext();
    //            }
    //
    //            @Override
    //            public U next() throws E {
    //                return (res = accumulator.apply(res, iter.next()));
    //            }
    //        }, closeHandlers);
    //    }
    //
    //    /**
    //     * Performs a scan (also known as prefix sum, cumulative sum, running total, or integral) operation on the elements of the stream.
    //     * The scan operation takes a binary operator (the accumulator) and applies it cumulatively on the stream elements,
    //     * successively combining each element in order from the start to produce a stream of accumulated results.
    //     *
    //     * <br />
    //     * This is an intermediate operation and will not close the sequence.
    //     *
    //     * <p>Example:
    //     * <pre>
    //     * <code>
    //     * Stream.of(new Integer[0]).scan(10, false, (r, c) -> r + c) => []
    //     * Stream.of(new Integer[0]).scan(10, true, (r, c) -> r + c) => [10]
    //     * Stream.of(1, 2, 3).scan(10, false, (r, c) -> r + c) => [11, 13, 16]
    //     * Stream.of(1, 2, 3).scan(10, true, (r, c) -> r + c) => [10, 11, 13, 16]
    //     * </code>
    //     * </pre>
    //     *
    //     *
    //     * @param init the initial value. It's only used once by the accumulator to calculate the first element in the returned stream.
    //     * @param initIncluded a boolean value that determines if the initial value should be included as the first element in the returned stream.
    //     * @param accumulator a {@code BiFunction} that takes two parameters: the current accumulated value and the current stream element, and returns a new accumulated value.
    //     * @return a new {@code Stream} consisting of the results of the scan operation on the elements of the original stream.
    //     * @see Stream#scan(Object, boolean, BiFunction)
    //     */
    //    @IntermediateOp
    //    public <U> Seq<U, E> scan(final U init, final boolean initIncluded, final Throwables.BiFunction<? super U, ? super T, U, ? extends E> accumulator)
    //            throws IllegalStateException {
    //        assertNotClosed();
    //
    //        if (!initIncluded) {
    //            return scan(init, accumulator);
    //        }
    //
    //        final Throwables.Iterator<T, E> iter = elements;
    //
    //        return create(new Throwables.Iterator<U, E>() {
    //            private boolean isFirst = true;
    //            private U res = init;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                return isFirst || iter.hasNext();
    //            }
    //
    //            @Override
    //            public U next() throws E {
    //                if (isFirst) {
    //                    isFirst = false;
    //                    return init;
    //                }
    //
    //                return (res = accumulator.apply(res, iter.next()));
    //            }
    //        }, closeHandlers);
    //    }

    /**
     * Returns a sequence consisting of the elements of this sequence that are also present in the specified collection.
     * Occurrences are considered. The order of the elements in the sequence is preserved.
     *
     * @param c the collection to be checked for intersection with this sequence
     * @return a new Seq containing the elements that are present in both this sequence and the specified collection
     * @throws IllegalStateException if the sequence is already closed
     * @see N#intersection(int[], int[])
     * @see N#intersection(Collection, Collection)
     * @see N#commonSet(Collection, Collection)
     * @see Collection#retainAll(Collection)
     */
    @IntermediateOp
    public Seq<T, E> intersection(final Collection<?> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        return filter(multiset::remove);
    }

    /**
     * Returns a sequence consisting of the elements of this sequence that are also present in the specified collection.
     * The comparison is based on the values obtained by applying the provided function to each element of the stream.
     * The order of the elements in the sequence is preserved.
     *
     * @param <U> the type of the elements in the specified collection
     * @param mapper the function to apply to each element of this sequence
     * @param c the collection to be checked for intersection with this sequence
     * @return a new Seq containing the elements that are present in both this sequence and the specified collection
     * @throws IllegalStateException if the sequence is already closed
     * @see N#intersection(int[], int[])
     * @see N#intersection(Collection, Collection)
     * @see N#commonSet(Collection, Collection)
     * @see Collection#retainAll(Collection)
     */
    @IntermediateOp
    public <U> Seq<T, E> intersection(final Throwables.Function<? super T, ? extends U, E> mapper, final Collection<U> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        return filter(value -> multiset.remove(mapper.apply(value)));
    }

    /**
     * Returns a sequence consisting of the elements of this sequence that are not present in the specified collection.
     * Occurrences are considered. The order of the elements in the sequence is preserved.
     *
     * @param c the collection to be checked for difference with this sequence
     * @return a new Seq containing the elements that are present in this sequence but not in the specified collection
     * @throws IllegalStateException if the sequence is already closed
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     * @see N#symmetricDifference(Collection, Collection)
     * @see N#excludeAll(Collection, Collection)
     * @see N#excludeAllToSet(Collection, Collection)
     * @see N#removeAll(Collection, Iterable)
     * @see N#intersection(Collection, Collection)
     * @see N#commonSet(Collection, Collection)
     * @see Difference#of(Collection, Collection)
     */
    @IntermediateOp
    public Seq<T, E> difference(final Collection<?> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        return filter(value -> !multiset.remove(value));
    }

    /**
     * Returns a new sequence that contains only the elements that are present in the original sequence but not in the provided collection.
     * The comparison is based on the values obtained by applying the provided function to each element of the stream.
     * Occurrences are considered.
     *
     * @param <U> the type of the elements in the specified collection
     * @param mapper the function to apply to each element of this sequence
     * @param c the collection to be checked for difference with this sequence
     * @return a new Seq containing the elements that are present in this sequence but not in the specified collection
     * @throws IllegalStateException if the sequence is already closed
     * @see IntList#difference(IntList)
     * @see N#difference(Collection, Collection)
     * @see N#symmetricDifference(Collection, Collection)
     * @see N#excludeAll(Collection, Collection)
     * @see N#excludeAllToSet(Collection, Collection)
     * @see N#removeAll(Collection, Iterable)
     * @see N#intersection(Collection, Collection)
     * @see N#commonSet(Collection, Collection)
     * @see Difference#of(Collection, Collection)
     */
    @IntermediateOp
    public <U> Seq<T, E> difference(final Function<? super T, ? extends U> mapper, final Collection<U> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        return filter(value -> multiset.isEmpty() || !multiset.remove(mapper.apply(value)));
    }

    /**
     * Returns a sequence consisting of the elements that are present in either this sequence or the specified collection,
     * but not in both. Occurrences are considered. The order of the elements in the sequence is preserved.
     *
     * @param c the collection to be checked for symmetric difference with this sequence
     * @return a new Seq containing the elements that are present in either this sequence or the specified collection, but not in both
     * @throws IllegalStateException if the sequence is already closed
     * @see N#symmetricDifference(int[], int[])
     * @see N#excludeAll(Collection, Collection)
     * @see N#excludeAllToSet(Collection, Collection)
     * @see N#difference(Collection, Collection)
     * @see Difference#of(Collection, Collection)
     * @see Iterables#symmetricDifference(Set, Set)
     */
    @IntermediateOp
    public Seq<T, E> symmetricDifference(final Collection<T> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        //noinspection resource
        return filter(value -> !multiset.remove(value)).append(Seq.<T, E> of(c).filter(multiset::remove));
    }

    /**
     * Prepends the specified elements to the beginning of this sequence.
     *
     * @param a the elements to be prepended to this sequence
     * @return a new Seq with the specified elements prepended
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @SafeVarargs
    public final Seq<T, E> prepend(final T... a) throws IllegalStateException {
        return prepend(Seq.of(a));
    }

    /**
     * Prepends the specified collection of elements to the beginning of this sequence.
     *
     * @param c the collection of elements to be prepended to this sequence
     * @return a new Seq with the specified collection of elements prepended
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> prepend(final Collection<? extends T> c) throws IllegalStateException {
        return prepend(Seq.of(c));
    }

    /**
     * Prepends the specified sequence to the beginning of this sequence.
     *
     * @param s the sequence to be prepended to this sequence
     * @return a new Seq with the specified sequence prepended
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> prepend(final Seq<T, E> s) throws IllegalStateException {
        assertNotClosed();

        return concat(s, this);
    }

    /**
     * Prepends the specified optional element to the beginning of this sequence if it's not empty.
     *
     * @param op the optional element to be prepended to this sequence
     * @return a new Seq with the specified optional element prepended
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> prepend(final u.Optional<T> op) throws IllegalStateException { //NOSONAR
        assertNotClosed();

        // return prepend(op.isEmpty() ? Seq.<T, E> empty() : Seq.<T, E> just(op.orElseThrow()));

        return op.isEmpty() ? this : prepend(op.orElseThrow());
    }

    /**
     * Appends the specified elements to the end of this sequence.
     *
     * @param a the elements to be appended to this sequence
     * @return a new Seq with the specified elements appended
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @SafeVarargs
    public final Seq<T, E> append(final T... a) throws IllegalStateException {
        return append(Seq.of(a));
    }

    /**
     * Appends the specified collection of elements to the end of this sequence.
     *
     * @param c the collection of elements to be appended to this sequence
     * @return a new Seq with the specified collection of elements appended
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> append(final Collection<? extends T> c) throws IllegalStateException {
        return append(Seq.of(c));
    }

    /**
     * Appends the specified sequence to the end of this sequence.
     *
     * @param s the sequence to be appended to this sequence
     * @return a new Seq with the specified sequence appended
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> append(final Seq<T, E> s) throws IllegalStateException {
        assertNotClosed();

        return concat(this, s);
    }

    /**
     * Appends the specified optional element to the end of this sequence if it's not empty.
     *
     * @param op the optional element to be appended to this sequence
     * @return a new Seq with the specified optional element appended
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> append(final u.Optional<T> op) throws IllegalStateException { //NOSONAR
        assertNotClosed();

        // return append(op.isEmpty() ? Seq.<T, E> empty() : Seq.<T, E> just(op.orElseThrow()));

        return op.isEmpty() ? this : append(op.orElseThrow());
    }

    /**
     * Appends the specified elements to the end of this sequence if this sequence is empty.
     *
     * @param a the elements to be appended to this sequence
     * @return a new Seq with the specified elements appended if this sequence is empty, otherwise, return this sequence.
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @SafeVarargs
    public final Seq<T, E> appendIfEmpty(final T... a) throws IllegalStateException {
        return appendIfEmpty(Arrays.asList(a));
    }

    /**
     * Appends the elements from the specified collection to the end of this Seq if the Seq is empty.
     *
     * @param c the collection of elements to append if the Seq is empty
     * @return a new Seq with the elements from the specified collection appended if the Seq is empty
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> appendIfEmpty(final Collection<? extends T> c) throws IllegalStateException {
        assertNotClosed();

        if (N.isEmpty(c)) {
            return create(elements, closeHandlers);
        }

        return create(new Throwables.Iterator<>() {
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
            public void advance(final long n) throws E {
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
     * Appends the elements from the Seq provided by the supplier to the end of this Seq if the Seq is empty.
     *
     * @param supplier the supplier that provides a Seq of elements to append if the Seq is empty
     * @return a new Seq with the elements from the Seq provided by the supplier appended if the Seq is empty
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> appendIfEmpty(final Supplier<? extends Seq<T, E>> supplier) throws IllegalStateException {
        assertNotClosed();

        final Holder<Seq<T, E>> holder = new Holder<>();

        return create(new Throwables.Iterator<T, E>() {
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
            public void advance(final long n) throws E {
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
                        final Seq<T, E> s = supplier.get();
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
    //        return create(new Throwables.Iterator<T, E>() {
    //            private boolean fallbackValueAvailable = true;
    //
    //                //            public boolean hasNext() throws E {
    //                try {
    //                    return fallbackValueAvailable && elements.hasNext();
    //                } catch (Exception e) {
    //                    return fallbackValueAvailable;
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
    //                    fallbackValueAvailable = false;
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
    //        return create(new Throwables.Iterator<T, E>() {
    //            private boolean fallbackValueAvailable = true;
    //
    //                //            public boolean hasNext() throws E {
    //                try {
    //                    return fallbackValueAvailable && elements.hasNext();
    //                } catch (Exception e) {
    //                    if (ExceptionUtil.hasCause(e, type)) {
    //                        return fallbackValueAvailable;
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
    //                        fallbackValueAvailable = false;
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
    //        this.checkArgNotNull(predicate, s.Predicate);
    //
    //        return create(new Throwables.Iterator<T, E>() {
    //            private boolean fallbackValueAvailable = true;
    //
    //                //            public boolean hasNext() throws E {
    //                try {
    //                    return fallbackValueAvailable && elements.hasNext();
    //                } catch (Exception e) {
    //                    if (predicate.test(e)) {
    //                        return fallbackValueAvailable;
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
    //                        fallbackValueAvailable = false;
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
    //            private boolean fallbackValueAvailable = true;
    //            private Throwables.Iterator<T, E> iter = Seq.this.elements;
    //            private Seq<T, E> s = null;
    //
    //                //            public boolean hasNext() throws E {
    //                try {
    //                    return iter.hasNext();
    //                } catch (Exception e) {
    //                    if (fallbackValueAvailable) {
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
    //                    if (fallbackValueAvailable) {
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
    //                fallbackValueAvailable = false;
    //                s = fallbackStream.get();
    //                iter = s.elements;
    //            }
    //        };
    //
    //        return create(iter).onClose(newCloseHandler(iter)).filter(NOT_NULL_MASK);
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
    //            private boolean fallbackValueAvailable = true;
    //            private Throwables.Iterator<T, E> iter = Seq.this.elements;
    //            private Seq<T, E> s = null;
    //
    //                //            public boolean hasNext() throws E {
    //                try {
    //                    return iter.hasNext();
    //                } catch (Exception e) {
    //                    if (fallbackValueAvailable && ExceptionUtil.hasCause(e, type)) {
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
    //                    if (fallbackValueAvailable && ExceptionUtil.hasCause(e, type)) {
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
    //                fallbackValueAvailable = false;
    //                s = fallbackStream.get();
    //                iter = s.elements;
    //            }
    //        };
    //
    //        return create(iter).onClose(newCloseHandler(iter)).filter(NOT_NULL_MASK);
    //    }
    //
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, E> appendOnError(final Predicate<? super Exception> predicate, final Supplier<Seq<T, E>> fallbackStreamSupplier) {
    //        assertNotClosed();
    //        this.checkArgNotNull(predicate, s.Predicate);
    //        this.checkArgNotNull(fallbackStreamSupplier, "fallbackStreamSupplier");
    //
    //        final Throwables.Iterator<T, E> iter = new Throwables.Iterator<T, E>() {
    //            private boolean fallbackValueAvailable = true;
    //            private Throwables.Iterator<T, E> iter = Seq.this.elements;
    //            private Seq<T, E> s = null;
    //
    //                //            public boolean hasNext() throws E {
    //                try {
    //                    return iter.hasNext();
    //                } catch (Exception e) {
    //                    if (fallbackValueAvailable && predicate.test(e)) {
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
    //                    if (fallbackValueAvailable && predicate.test(e)) {
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
    //                fallbackValueAvailable = false;
    //                s = fallbackStream.get();
    //                iter = s.elements;
    //            }
    //        };
    //
    //        return create(iter).onClose(newCloseHandler(iter)).filter(NOT_NULL_MASK);
    //    }

    /**
     * Returns a new Seq that contains the specified default value if this Seq is empty.
     *
     *
     * @param defaultValue the default value to return if this Seq is empty
     * @return a new Seq containing the default value if this Seq is empty
     * @throws IllegalStateException if the sequence is already closed
     * @see #appendIfEmpty(Object...)
     */
    @IntermediateOp
    public Seq<T, E> defaultIfEmpty(final T defaultValue) throws IllegalStateException {
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
     * Returns a new Seq that contains the elements from the Seq provided by the supplier if this Seq is empty.
     *
     * @param supplier the supplier that provides a Seq of elements to return if this Seq is empty
     * @return a new Seq with the elements from the Seq provided by the supplier if this Seq is empty
     * @throws IllegalStateException if the sequence is already closed
     * @see #appendIfEmpty(Supplier)
     */
    @IntermediateOp
    public Seq<T, E> defaultIfEmpty(final Supplier<? extends Seq<T, E>> supplier) throws IllegalStateException {
        return appendIfEmpty(supplier);
    }

    /**
     * Throws a {@code NoSuchElementException} in executed terminal operation if this {@code Seq} is empty.
     *
     * @return this {@code Seq} instance
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> throwIfEmpty() throws IllegalStateException, NoSuchElementException {
        return throwIfEmpty(Suppliers.newNoSuchElementException());
    }

    /**
     * Throws a custom exception provided by the supplier in terminal operation if this {@code Seq} is empty.
     *
     * @param exceptionSupplier the supplier that provides the exception to throw if this {@code Seq} is empty
     * @return this {@code Seq} instance
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> throwIfEmpty(final Supplier<? extends RuntimeException> exceptionSupplier) throws IllegalStateException {
        checkArgNotNull(exceptionSupplier, cs.exceptionSupplier);

        return ifEmpty(() -> {
            throw exceptionSupplier.get();
        });
    }

    /**
     * Executes the given action if the sequence is empty.
     *
     * @param action the action to be executed if the sequence is empty
     * @return the current stream
     * @throws IllegalStateException if the sequence is already closed
     */
    @Beta
    @IntermediateOp
    public Seq<T, E> ifEmpty(final Throwables.Runnable<? extends E> action) throws IllegalStateException { // should be named as doIfEmpty?
        assertNotClosed();
        checkArgNotNull(action, cs.action);

        return create(new Throwables.Iterator<>() {
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
            public void advance(final long n) throws E {
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
                    iter = elements;

                    if (!iter.hasNext()) {
                        action.run();
                    }
                }
            }
        }, sorted, cmp, closeHandlers);
    }

    /**
     * Performs the given action on the elements pulled by downstream/terminal operation.
     *
     * @param action the action to be performed on the elements pulled by downstream/terminal operation
     * @return this {@code Seq} instance
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> onEach(final Throwables.Consumer<? super T, ? extends E> action) throws IllegalStateException {
        assertNotClosed();

        return create(new Throwables.Iterator<>() {
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
     * Performs the given action for the first element of this {@code Seq}.
     *
     * @param action the action to be performed for the first element
     * @return this {@code Seq} instance
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> onFirst(final Throwables.Consumer<? super T, ? extends E> action) throws IllegalStateException {
        assertNotClosed();

        return create(new Throwables.Iterator<>() {
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
     * Performs the given action for the last element of this {@code Seq}.
     *
     * @param action the action to be performed for the last element
     * @return this {@code Seq} instance
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> onLast(final Throwables.Consumer<? super T, ? extends E> action) throws IllegalStateException {
        assertNotClosed();

        return create(new Throwables.Iterator<>() {
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
     * Performs the given action on the elements pulled by downstream/terminal operation. Most of the time, it's used for debugging
     *
     * @param action the action to be performed on the elements pulled by downstream/terminal operation
     * @return this {@code Seq} instance
     * @throws IllegalStateException if the sequence is already closed
     * @see #onEach(Throwables.Consumer)
     */
    @IntermediateOp
    public Seq<T, E> peek(final Throwables.Consumer<? super T, ? extends E> action) throws IllegalStateException {
        return onEach(action);
    }

    /**
     * Performs the given action on the first element pulled by downstream/terminal operation. Most of the time, it's used for debugging
     *
     * @param action the action to be performed on the first element pulled by downstream/terminal operation
     * @return this {@code Seq} instance
     * @throws IllegalStateException if the sequence is already closed
     * @see #onFirst(Throwables.Consumer)
     */
    @IntermediateOp
    public Seq<T, E> peekFirst(final Throwables.Consumer<? super T, ? extends E> action) throws IllegalStateException {
        return onFirst(action);
    }

    /**
     * Performs the given action on the last element pulled by downstream/terminal operation. Most of the time, it's used for debugging
     *
     * @param action the action to be performed on the last element pulled by downstream/terminal operation
     * @return this {@code Seq} instance
     * @throws IllegalStateException if the sequence is already closed
     * @see #onLast(Throwables.Consumer)
     */
    @IntermediateOp
    public Seq<T, E> peekLast(final Throwables.Consumer<? super T, ? extends E> action) throws IllegalStateException {
        return onLast(action);
    }

    /**
     * Performs the given action on the elements pulled by downstream/terminal operation which matches the given predicate. Most of the time, it's used for debugging
     *
     * @param predicate the predicate to test each element
     * @param action the action to be performed on the elements pulled by downstream/terminal operation which matches the given predicate
     * @return this {@code Seq} instance
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> peekIf(final Throwables.Predicate<? super T, E> predicate, final Throwables.Consumer<? super T, E> action) throws IllegalStateException {
        assertNotClosed();
        checkArgNotNull(predicate, cs.Predicate);
        checkArgNotNull(action, cs.action);

        return peek(it -> {
            if (predicate.test(it)) {
                action.accept(it);
            }
        });
    }

    /**
     * Performs the given action on the elements pulled by downstream/terminal operation which matches the given predicate. Most of the time, it's used for debugging
     *
     * @param predicate The predicate to test each element. The first parameter is the element, and the second parameter is the count of iterated elements, starting with 1.
     * @param action the action to be performed on the elements pulled by downstream/terminal operation which matches the given predicate
     * @return this {@code Seq} instance
     * @throws IllegalStateException if the sequence is already closed
     */
    @Beta
    @IntermediateOp
    public Seq<T, E> peekIf(final Throwables.BiPredicate<? super T, ? super Long, E> predicate, final Consumer<? super T> action) throws IllegalStateException {
        assertNotClosed();
        checkArgNotNull(predicate, cs.Predicate);
        checkArgNotNull(action, cs.action);

        final MutableLong count = MutableLong.of(0);

        return peek(it -> {
            if (predicate.test(it, count.incrementAndGet())) {
                action.accept(it);
            }
        });
    }

    //    /**
    //     *
    //     * @param chunkSize
    //     * @return
    //     * @throws IllegalStateException
    //     */
    //    @IntermediateOp
    //    public Seq<Stream<T>, E> split(final int chunkSize) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return splitToList(chunkSize).map(Stream::of);
    //    }

    /**
     * Returns a sequence of Lists, where each List contains a chunk of elements from the original stream.
     * The size of each chunk is specified by the chunkSize parameter. The final chunk may be smaller if there are not enough elements.
     *
     * @param chunkSize the desired size of each chunk (the last chunk may be smaller)
     * @return a sequence of Lists, each containing a chunk of elements from the original stream
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<List<T>, E> split(final int chunkSize) throws IllegalStateException {
        return split(chunkSize, Factory.ofList());
    }

    //    /**
    //     * Returns sequence of {@code Set<T>} with consecutive sub-sequences of the elements, each of the same size (the final sequence may be smaller).
    //     *
    //     *
    //     * @param chunkSize the desired size of each sub-sequence (the last may be smaller).
    //     * @return
    //     */
    //    @IntermediateOp
    //    public Seq<Set<T>, E> splitToSet(final int chunkSize) {
    //        return split(chunkSize, Factory.<T> ofSet());
    //    }

    /**
     * Splits the elements of this sequence into sub-sequences of the specified size. The last sub-sequence may be smaller than the specified size.
     *
     * @param <C> the type of the collection to hold the sub-sequences
     * @param chunkSize the desired size of each sub-sequence (the last sub-sequence may be smaller)
     * @param collectionSupplier a function that provides a new collection to hold each sub-sequence
     * @return a new sequence where each element is a collection containing a sub-sequence of the original elements
     * @throws IllegalStateException if the sequence is in an invalid state
     */
    @IntermediateOp
    public <C extends Collection<T>> Seq<C, E> split(final int chunkSize, final IntFunction<? extends C> collectionSupplier) throws IllegalStateException {
        assertNotClosed();
        checkArgPositive(chunkSize, cs.chunkSize);
        checkArgNotNull(collectionSupplier, cs.collectionSupplier);

        return create(new Throwables.Iterator<>() {
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
            public void advance(final long n) throws IllegalArgumentException, E {
                checkArgNotNegative(n, cs.n);

                elements.advance(n > Long.MAX_VALUE / chunkSize ? Long.MAX_VALUE : n * chunkSize);
            }
        }, closeHandlers);
    }

    /**
     * Splits the sequence into subsequences of the specified size and collects them using the provided collector.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param <R> the type of the result
     * @param chunkSize the desired size of each subsequence (the last may be smaller)
     * @param collector the collector to use for collecting the subsequences
     * @return a new Seq where each element is the result of collecting a subsequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if chunkSize is less than or equal to 0
     */
    @IntermediateOp
    public <R> Seq<R, E> split(final int chunkSize, final Collector<? super T, ?, R> collector) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(chunkSize, cs.chunkSize);
        checkArgNotNull(collector, cs.collector);

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super T> accumulator = (BiConsumer<Object, ? super T>) collector.accumulator();
        final Function<Object, R> finisher = (Function<Object, R>) collector.finisher();

        return create(new Throwables.Iterator<>() {
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
            public void advance(final long n) throws IllegalArgumentException, E {
                checkArgNotNegative(n, cs.n);

                elements.advance(n > Long.MAX_VALUE / chunkSize ? Long.MAX_VALUE : n * chunkSize);
            }
        }, closeHandlers);
    }

    //    /**
    //     *
    //     * @param predicate
    //     * @return
    //     * @throws IllegalStateException
    //     */
    //    @IntermediateOp
    //    public Seq<Stream<T>, E> split(final Throwables.Predicate<? super T, ? extends E> predicate) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return splitToList(predicate).map(Stream::of);
    //    }

    /**
     * Splits the sequence into subsequences based on the given predicate.
     * Each subsequence is collected into a List.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param predicate the predicate to determine the boundaries of the subsequences
     * @return a new Seq where each element is a List of elements that satisfy the predicate
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<List<T>, E> split(final Throwables.Predicate<? super T, ? extends E> predicate) throws IllegalStateException {
        assertNotClosed();

        return split(predicate, Suppliers.ofList());
    }

    //    /**
    //     *
    //     * @param predicate
    //     * @return
    //     * @throws IllegalStateException
    //     */
    //    @IntermediateOp
    //    public Seq<Set<T>, E> splitToSet(final Throwables.Predicate<? super T, ? extends E> predicate) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return split(predicate, Suppliers.<T> ofSet());
    //    }

    /**
     * Splits the sequence into subsequences based on the given predicate.
     * Each subsequence is collected into a Collection.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param <C> the type of the Collection to collect the subsequences
     * @param predicate the predicate to determine the boundaries of the subsequences
     * @param collectionSupplier the supplier to provide the Collection to collect the subsequences
     * @return a new Seq where each element is a Collection of elements that satisfy the predicate
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public <C extends Collection<T>> Seq<C, E> split(final Throwables.Predicate<? super T, ? extends E> predicate,
            final Supplier<? extends C> collectionSupplier) throws IllegalStateException {
        assertNotClosed();
        checkArgNotNull(predicate, cs.Predicate);
        checkArgNotNull(collectionSupplier, cs.collectionSupplier);

        return create(new Throwables.Iterator<>() {
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
     * Splits the sequence into subsequences based on the given predicate and collects them using the provided collector.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param <R> the type of the result
     * @param predicate the predicate to determine the boundaries of the subsequences
     * @param collector the collector to use for collecting the subsequences
     * @return a new Seq where each element is the result of collecting a subsequence
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public <R> Seq<R, E> split(final Throwables.Predicate<? super T, ? extends E> predicate, final Collector<? super T, ?, R> collector)
            throws IllegalStateException {
        assertNotClosed();
        checkArgNotNull(predicate, cs.Predicate);
        checkArgNotNull(collector, cs.collector);

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super T> accumulator = (BiConsumer<Object, ? super T>) collector.accumulator();
        final Function<Object, R> finisher = (Function<Object, R>) collector.finisher();

        return create(new Throwables.Iterator<>() {
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
     * Splits the sequence at the specified position into two subsequences.
     * The first subsequence contains elements from the start up to the specified position (exclusive),
     * and the second subsequence contains elements from the specified position to the end.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param where the position at which to split the sequence
     * @return a new Seq containing two subsequences split at the specified position
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified position less than 0
     */
    @IntermediateOp
    public Seq<Seq<T, E>, E> splitAt(final int where) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(where, cs.where);

        final Throwables.Iterator<T, E> iter = elements;

        return create(new Throwables.Iterator<>() {
            private int cursor = 0;

            @Override
            public boolean hasNext() throws E {
                return cursor < 2;
            }

            @Override
            public Seq<T, E> next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                Seq<T, E> result = null;

                if (cursor == 0) {
                    final List<T> list = new ArrayList<>();
                    int cnt = 0;

                    while (cnt++ < where && iter.hasNext()) {
                        list.add(iter.next());
                    }

                    result = new Seq<>(Throwables.Iterator.of(list.iterator()), sorted, cmp, null);
                } else {
                    result = new Seq<>(iter, sorted, cmp, null);
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
            public void advance(final long n) throws E {
                if (n == 0) {
                    return;
                } else if ((n == 1) && (cursor == 0)) {
                    iter.advance(where);
                } else {
                    iter.advance(Long.MAX_VALUE);
                }

                cursor = n >= 2 ? 2 : cursor + (int) n;
            }
        }, closeHandlers);
    }

    /**
     * Splits the sequence at the position where the given predicate returns {@code true}.
     * The first subsequence contains elements from the start up to the position where the predicate returns {@code true} (exclusive),
     * and the second subsequence contains elements from the position where the predicate returns {@code true} to the end.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param where the predicate to determine the position at which to split the sequence
     * @return a new Seq containing two subsequences split at the position where the predicate returns true
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<Seq<T, E>, E> splitAt(final Throwables.Predicate<? super T, ? extends E> where) throws IllegalStateException {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = elements;

        return create(new Throwables.Iterator<>() {
            private int cursor = 0;
            private T next = null;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() throws E {
                return cursor < 2;
            }

            @Override
            public Seq<T, E> next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                Seq<T, E> result = null;

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

                    result = new Seq<>(Throwables.Iterator.of(list.iterator()), sorted, cmp, null);
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

                    result = new Seq<>(iterEx, sorted, cmp, null);
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
            public void advance(final long n) throws E {
                if (n == 0) {
                    return;
                } else if ((n == 1) && (cursor == 0)) {
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

                cursor = n >= 2 ? 2 : cursor + (int) n;
            }

        }, closeHandlers);
    }

    //    /**
    //     *
    //     * @param windowSize
    //     * @return
    //     * @see #sliding(int, int)
    //     */
    //    @IntermediateOp
    //    public Seq<Stream<T>, E> sliding(final int windowSize) {
    //        return sliding(windowSize, 1);
    //    }

    /**
     * Creates a sliding window view of the sequence with the specified window size.
     * Each window is a list containing a subset of elements from the sequence.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param windowSize the size of the sliding window
     * @return a new Seq where each element is a list representing a sliding window of the original sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the window size is less than or equal to zero
     */
    @IntermediateOp
    public Seq<List<T>, E> sliding(final int windowSize) throws IllegalStateException {
        return sliding(windowSize, 1);
    }

    /**
     * Creates a sliding window view of the sequence with the specified window size and collection supplier.
     * Each window is a collection containing a subset of elements from the sequence.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param <C> the type of the collection to be used for each window
     * @param windowSize the size of the sliding window
     * @param collectionSupplier a function that provides a new collection instance for each window
     * @return a new Seq where each element is a collection representing a sliding window of the original sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the window size is less than or equal to zero
     */
    @IntermediateOp
    public <C extends Collection<T>> Seq<C, E> sliding(final int windowSize, final IntFunction<? extends C> collectionSupplier)
            throws IllegalStateException, IllegalArgumentException {
        return sliding(windowSize, 1, collectionSupplier);
    }

    /**
     * Creates a sliding window view of the sequence with the specified window size and collector.
     * Each window is collected into a result container using the provided collector.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param <R> the type of the resulting elements
     * @param windowSize the size of the sliding window
     * @param collector a Collector that collects the elements of each window into a result container
     * @return a new Seq where each element is a result container representing a sliding window of the original sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the window size is less than or equal to zero
     */
    @IntermediateOp
    public <R> Seq<R, E> sliding(final int windowSize, final Collector<? super T, ?, R> collector) throws IllegalStateException, IllegalArgumentException {
        return sliding(windowSize, 1, collector);
    }

    //    /**
    //     *
    //     * @param windowSize
    //     * @param increment
    //     * @return
    //     * @throws IllegalStateException
    //     */
    //    @IntermediateOp
    //    public Seq<Stream<T>, E> sliding(final int windowSize, final int increment) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return slidingToList(windowSize, increment).map(Stream::of);
    //    }

    /**
     * Creates a sliding window view of the sequence with the specified window size and increment.
     * Each window is a list containing a subset of elements from the sequence.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param windowSize the size of the sliding window
     * @param increment the increment by which the window moves forward
     * @return a new Seq where each element is a list representing a sliding window of the original sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the window size or increment is less than or equal to zero
     */
    @IntermediateOp
    public Seq<List<T>, E> sliding(final int windowSize, final int increment) throws IllegalStateException, IllegalArgumentException {
        return sliding(windowSize, increment, Factory.ofList());
    }

    //    /**
    //     * Sliding to set.
    //     *
    //     * @param windowSize
    //     * @param increment
    //     * @return
    //     */
    //    @IntermediateOp
    //    public Seq<Set<T>, E> slidingToSet(final int windowSize, final int increment) {
    //        return sliding(windowSize, increment, Factory.<T> ofSet());
    //    }

    /**
     * Creates a sliding window view of the sequence with the specified window size and increment.
     * Each window is a collection containing a subset of elements from the sequence.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param <C> the type of the collection to be returned
     * @param windowSize the size of the sliding window
     * @param increment the increment by which the window moves forward
     * @param collectionSupplier a function that provides a new collection of type C
     * @return a new Seq where each element is a collection representing a sliding window of the original sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the window size or increment is less than or equal to zero
     */
    @IntermediateOp
    public <C extends Collection<T>> Seq<C, E> sliding(final int windowSize, final int increment, final IntFunction<? extends C> collectionSupplier)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);
        checkArgNotNull(collectionSupplier, cs.collectionSupplier);

        return create(new Throwables.Iterator<>() {
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
            public void advance(final long n) throws IllegalArgumentException, E {
                checkArgNotNegative(n, cs.n);

                if (n == 0) {
                    return;
                }

                if (increment >= windowSize) {
                    //noinspection DuplicateExpressions
                    elements.advance(n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                } else {
                    if (N.isEmpty(queue)) {
                        final long m = ((n - 1) > Long.MAX_VALUE / increment ? Long.MAX_VALUE : (n - 1) * increment);
                        elements.advance(m);
                    } else {
                        @SuppressWarnings("DuplicateExpressions")
                        final long m = (n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                        final int prevSize = queue.size(); //NOSONAR

                        if (m < prevSize) {
                            for (int i = 0; i < m; i++) {
                                queue.removeFirst();
                            }
                        } else {
                            if (!queue.isEmpty()) {
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
     * Creates a sliding window view of the sequence with the specified window size and increment.
     * Each window is collected into a result using the provided collector.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param <R> the type of the result
     * @param windowSize the size of the sliding window
     * @param increment the increment by which the window moves forward
     * @param collector a Collector that collects the elements of each window into a result
     * @return a new Seq where each element is the result of collecting the elements of a sliding window
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the window size or increment is less than or equal to zero
     */
    @IntermediateOp
    public <R> Seq<R, E> sliding(final int windowSize, final int increment, final Collector<? super T, ?, R> collector)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);
        checkArgNotNull(collector, cs.collector);

        final Supplier<Object> supplier = (Supplier<Object>) collector.supplier();
        final BiConsumer<Object, ? super T> accumulator = (BiConsumer<Object, ? super T>) collector.accumulator();
        final Function<Object, R> finisher = (Function<Object, R>) collector.finisher();

        return create(new Throwables.Iterator<>() {
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

                    for (final T e : queue) {
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
            public void advance(final long n) throws IllegalArgumentException, E {
                checkArgNotNegative(n, cs.n);

                if (n == 0) {
                    return;
                }

                if (increment >= windowSize) {
                    //noinspection DuplicateExpressions
                    elements.advance(n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                } else {
                    if (N.isEmpty(queue)) {
                        final long m = ((n - 1) > Long.MAX_VALUE / increment ? Long.MAX_VALUE : (n - 1) * increment);
                        elements.advance(m);
                    } else {
                        @SuppressWarnings("DuplicateExpressions")
                        final long m = (n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                        final int prevSize = queue.size(); //NOSONAR

                        if (m < prevSize) {
                            for (int i = 0; i < m; i++) {
                                queue.removeFirst();
                            }
                        } else {
                            if (!queue.isEmpty()) {
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
     * Skips the first <i>n</i> elements of the sequence and returns a new sequence.
     *
     * @param n the number of elements to skip
     * @return a new sequence where the first <i>n</i> elements are skipped
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if <i>n</i> is negative
     */
    @IntermediateOp
    public Seq<T, E> skip(final long n) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        //    if (n == 0) {
        //        return create(elements, sorted, cmp, closeHandlers);
        //    }

        if (n == 0) {
            return this;
        }

        return create(new Throwables.Iterator<>() {
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
     * Skips the first <i>n</i> elements of the sequence and performs the given action on each skipped element.
     *
     * @param n the number of elements to skip
     * @param actionOnSkippedItem the action to be performed on each skipped element
     * @return a new sequence where the first <i>n</i> elements are skipped and the action is performed on each skipped element
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if <i>n</i> is negative
     */
    public Seq<T, E> skip(final long n, final Throwables.Consumer<? super T, ? extends E> actionOnSkippedItem)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);
        checkArgNotNull(actionOnSkippedItem, cs.action);

        if (n == 0) {
            return this;
        }

        final Throwables.Predicate<T, E> filter = new Throwables.Predicate<>() {
            final MutableLong cnt = MutableLong.of(n);

            public boolean test(final T value) {
                return cnt.getAndDecrement() > 0;
            }
        };

        return dropWhile(filter, actionOnSkippedItem);
    }

    /**
     * Filters out {@code null} elements from this {@code Seq}.
     *
     * @return a new {@code Seq} instance with {@code null} elements removed
     */
    @IntermediateOp
    public Seq<T, E> skipNulls() {
        return filter(Fnn.notNull());
    }

    /**
     * Skips the last <i>n</i> elements of the sequence and returns a new sequence.
     *
     * @param n the number of elements to skip from the end
     * @return a new sequence where the last <i>n</i> elements are skipped
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if <i>n</i> is negative
     */
    @IntermediateOp
    public Seq<T, E> skipLast(final int n) throws IllegalStateException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        if (n <= 0) {
            return create(elements, sorted, cmp, closeHandlers);
        }

        return create(new Throwables.Iterator<>() {
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

    //    /**
    //     *
    //     *
    //     * @return
    //     * @deprecated Use {@link #skipNulls()} instead
    //     */
    //    @Deprecated
    //    @IntermediateOp
    //    public Seq<T, E> skipNull() {
    //        return skipNulls();
    //    }

    /**
     * Limits the number of elements in the sequence to the specified maximum size.
     *
     * @param maxSize the maximum number of elements to include in the sequence
     * @return a new sequence where the number of elements is limited to the specified maximum size
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if <i>maxSize</i> is negative
     */
    @IntermediateOp
    public Seq<T, E> limit(final long maxSize) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(maxSize, cs.maxSize);

        return create(new Throwables.Iterator<>() {
            private long cnt = 0;

            @Override
            public boolean hasNext() throws E {
                return cnt < maxSize && elements.hasNext();
            }

            @Override
            public T next() throws IllegalArgumentException, E {
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
    //        checkArgument(to >= from, "'to' can't be less than 'from'");
    //
    //        return from == 0 ? limit(to) : skip(from).limit(to - from);
    //    }

    /**
     * Returns a new {@code Seq} consisting of the last {@code n} elements of this sequence.
     * A queue with size up to {@code n} will be maintained to filter out the last {@code n} elements.
     * It may cause <code>out of memory error</code> if {@code n} is big enough.
     *
     * <br />
     * All the elements will be loaded to get the last {@code n} elements and the sequence will be closed after that, if a terminal operation is triggered.
     *
     * @param n the number of elements to retain from the end of the sequence
     * @return a new {@code Seq} consisting of the last {@code n} elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if {@code n} is negative
     * @see Stream#last(int)
     */
    @Beta
    @IntermediateOp
    public Seq<T, E> last(final int n) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        if (n == 0) {
            return limit(0);
        }

        return create(new Throwables.Iterator<>() {
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
                        Seq.this.close();
                    }

                    iter = deque.iterator();
                }
            }
        }, sorted, cmp, closeHandlers);
    }

    /**
     * Returns a new {@code Seq} consisting of the top {@code n} elements of this sequence based on the natural order. Nulls are considered smaller than non-nulls.
     *
     * @param n the number of elements to retain from the top of the sequence
     * @return a new {@code Seq} consisting of the top {@code n} elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if {@code n} is negative
     * @see Stream#top(int)
     */
    @IntermediateOp
    public Seq<T, E> top(final int n) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        return top(n, (Comparator<T>) Comparators.nullsFirst());
    }

    /**
     * Returns a new {@code Seq} consisting of the top {@code n} elements of this sequence based on the specified comparator.
     *
     * @param n the number of elements to retain from the top of the sequence
     * @param comparator the comparator to compare the elements
     * @return a new {@code Seq} consisting of the top {@code n} elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if {@code n} is negative
     * @see Stream#top(int, Comparator)
     */
    @IntermediateOp
    public Seq<T, E> top(final int n, final Comparator<? super T> comparator) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(n, cs.n);

        return create(new Throwables.Iterator<>() {
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
            public void advance(final long n) throws E {
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
     * Returns a new {@code Seq} with the elements in reverse order.
     * All the elements will be loaded to reverse the order when a terminal operation is triggered.
     *
     * @return a new {@code Seq} with the elements in reverse order
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> reversed() throws IllegalStateException {
        assertNotClosed();

        return create(new Throwables.Iterator<>() {
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
            public void advance(final long n) throws E {
                if (!initialized) {
                    init();
                }

                cursor = n < cursor ? cursor - (int) n : 0;
            }

            private void init() throws E {
                if (!initialized) {
                    initialized = true;
                    aar = (T[]) Seq.this.toArrayForIntermediateOp();
                    cursor = aar.length;
                }
            }
        }, false, null, closeHandlers);
    }

    /**
     * Rotates the elements in this sequence by the specified distance.
     * All elements will be loaded to rotate the elements when a terminal operation is triggered
     *
     * @param distance the distance to rotate the elements. Positive values rotate to the right, negative values rotate to the left.
     * @return a new {@code Seq} with the elements rotated by the specified distance
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> rotated(final int distance) throws IllegalStateException {
        assertNotClosed();

        if (distance == 0) {
            return create(elements, closeHandlers);
        }

        return create(new Throwables.Iterator<>() {
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
            public void advance(final long n) throws E {
                if (!initialized) {
                    init();
                }

                cnt = n < len - cnt ? cnt + (int) n : len;
            }

            private void init() throws E {
                if (!initialized) {
                    initialized = true;
                    aar = (T[]) Seq.this.toArrayForIntermediateOp();
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
     * Returns a new {@code Seq} with the elements shuffled.
     * All elements will be loaded to shuffle the elements when a terminal operation is triggered.
     *
     * @return a new {@code Seq} with the elements shuffled
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> shuffled() throws IllegalStateException {
        return shuffled(RAND);
    }

    /**
     * Returns a new {@code Seq} with the elements shuffled using the specified random number generator.
     * All elements will be loaded to shuffle the elements when a terminal operation is triggered.
     *
     * @param rnd the random number generator to use for shuffling the elements
     * @return a new {@code Seq} with the elements shuffled
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> shuffled(final Random rnd) throws IllegalStateException {
        assertNotClosed();

        return lazyLoad(a -> {
            N.shuffle(a, rnd);
            return a;
        }, false, null);
    }

    /**
     * Returns a new {@code Seq} with the elements sorted in their natural order. Nulls are considered smaller than non-nulls.
     * All elements will be loaded to sort the elements when a terminal operation is triggered.
     *
     * @return a new {@code Seq} with the elements sorted
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> sorted() throws IllegalStateException {
        return sorted(NATURAL_COMPARATOR);
    }

    /**
     * Returns a new {@code Seq} with the elements sorted using the specified comparator.
     * All elements will be loaded to sort the elements when a terminal operation is triggered.
     *
     * @param comparator the comparator to use for sorting the elements
     * @return a new {@code Seq} with the elements sorted
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> sorted(final Comparator<? super T> comparator) throws IllegalStateException {
        assertNotClosed();

        final Comparator<? super T> cmp = comparator == null ? NATURAL_COMPARATOR : comparator; //NOSONAR

        if (sorted && cmp == this.cmp) {
            // return create(elements, sorted, comparator, closeHandlers);
            return this;
        }

        return lazyLoad(a -> {
            N.sort((T[]) a, cmp);

            return a;
        }, true, cmp);
    }

    /**
     * Returns a new {@code Seq} with the elements sorted according to integer values extracted by the provided key extractor function.
     * All elements will be loaded to sort the elements when a terminal operation is triggered.
     *
     * @param keyMapper a function that extracts an integer key from each element, which will be used for sorting
     * @return a new {@code Seq} with the elements sorted by the extracted integer key
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> sortedByInt(final ToIntFunction<? super T> keyMapper) throws IllegalStateException {
        final Comparator<? super T> cmpToUse = Comparators.comparingInt(keyMapper);

        return sorted(cmpToUse);
    }

    /**
     * Returns a new {@code Seq} with the elements sorted according to long values extracted by the provided key extractor function.
     * All elements will be loaded to sort the elements when a terminal operation is triggered.
     *
     * @param keyMapper a function that extracts a long key from each element, which will be used for sorting
     * @return a new {@code Seq} with the elements sorted by the extracted long key
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> sortedByLong(final ToLongFunction<? super T> keyMapper) throws IllegalStateException {
        final Comparator<? super T> cmpToUse = Comparators.comparingLong(keyMapper);

        return sorted(cmpToUse);
    }

    /**
     * Returns a new {@code Seq} with the elements sorted according to double values extracted by the provided key extractor function.
     * All elements will be loaded to sort the elements when a terminal operation is triggered.
     *
     * @param keyMapper a function that extracts a double key from each element, which will be used for sorting
     * @return a new {@code Seq} with the elements sorted by the extracted double key
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> sortedByDouble(final ToDoubleFunction<? super T> keyMapper) throws IllegalStateException {
        final Comparator<? super T> cmpToUse = Comparators.comparingDouble(keyMapper);

        return sorted(cmpToUse);
    }

    /**
     * Returns a new {@code Seq} with the elements sorted according to natural order of the values extracted by the provided key extractor function. Nulls are considered smaller than {@code non-null}.
     * All elements will be loaded to sort the elements when a terminal operation is triggered.
     *
     * @param keyMapper a function that extracts a comparable key from each element, which will be used for sorting
     * @return a new {@code Seq} with the elements sorted by the extracted key
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    @SuppressWarnings("rawtypes")
    public Seq<T, E> sortedBy(final Function<? super T, ? extends Comparable> keyMapper) throws IllegalStateException {
        assertNotClosed();

        final Comparator<? super T> comparator = Comparators.comparingBy(keyMapper);

        return sorted(comparator);
    }

    /**
     * Returns a new {@code Seq} with the elements sorted in reverse natural order. Nulls are considered bigger than non-nulls in reverse order.
     * All elements will be loaded to sort the elements when a terminal operation is triggered.
     *
     * @return a new {@code Seq} with the elements sorted in reverse order
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> reverseSorted() throws IllegalStateException {
        return sorted(REVERSED_COMPARATOR);
    }

    /**
     * Returns a new {@code Seq} with the elements sorted in reverse order using the specified comparator.
     * All elements will be loaded to sort the elements when a terminal operation is triggered.
     *
     * @param comparator the comparator to use for sorting the elements in reverse order
     * @return a new {@code Seq} with the elements sorted in reverse order
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> reverseSorted(final Comparator<? super T> comparator) throws IllegalStateException {
        final Comparator<? super T> cmpToUse = Comparators.reverseOrder(comparator);

        return sorted(cmpToUse);
    }

    /**
     * Returns a new {@code Seq} with the elements sorted in reverse order according to integer values extracted by the provided key extractor function.
     * All elements will be loaded to sort the elements when a terminal operation is triggered.
     *
     * @param keyMapper a function that extracts an integer key from each element, which will be used for sorting
     * @return a new {@code Seq} with the elements sorted in reverse order by the extracted integer key
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> reverseSortedByInt(final ToIntFunction<? super T> keyMapper) throws IllegalStateException {
        final Comparator<? super T> cmpToUse = Comparators.reversedComparingInt(keyMapper);

        return sorted(cmpToUse);
    }

    /**
     * Returns a new {@code Seq} with the elements sorted in reverse order according to long values extracted by the provided key extractor function.
     * All elements will be loaded to sort the elements when a terminal operation is triggered.
     *
     * @param keyMapper a function that extracts a long key from each element, which will be used for sorting
     * @return a new {@code Seq} with the elements sorted in reverse order by the extracted long key
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> reverseSortedByLong(final ToLongFunction<? super T> keyMapper) throws IllegalStateException {
        final Comparator<? super T> cmpToUse = Comparators.reversedComparingLong(keyMapper);

        return sorted(cmpToUse);
    }

    /**
     * Returns a new {@code Seq} with the elements sorted in reverse order according to double values extracted by the provided key extractor function.
     * All elements will be loaded to sort the elements when a terminal operation is triggered.
     *
     * @param keyMapper a function that extracts a double key from each element, which will be used for sorting
     * @return a new {@code Seq} with the elements sorted in reverse order by the extracted double key
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> reverseSortedByDouble(final ToDoubleFunction<? super T> keyMapper) throws IllegalStateException {
        final Comparator<? super T> cmpToUse = Comparators.reversedComparingDouble(keyMapper);

        return sorted(cmpToUse);
    }

    /**
     * Returns a new {@code Seq} with the elements sorted in reverse order according to natural order of the values extracted by the provided key extractor function.
     * Nulls are considered bigger than non-nulls in reverse order.
     * All elements will be loaded to sort the elements when a terminal operation is triggered.
     *
     * @param keyMapper a function that extracts a comparable key from each element, which will be used for sorting
     * @return a new {@code Seq} with the elements sorted in reverse order by the extracted key
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> reverseSortedBy(@SuppressWarnings("rawtypes") final Function<? super T, ? extends Comparable> keyMapper) throws IllegalStateException {
        final Comparator<? super T> cmpToUse = Comparators.reversedComparingBy(keyMapper);

        return sorted(cmpToUse);
    }

    private Seq<T, E> lazyLoad(final Function<Object[], Object[]> op, final boolean sorted, final Comparator<? super T> cmp) {
        assertNotClosed();

        return create(new Throwables.Iterator<>() {
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
            public void advance(final long n) throws IllegalArgumentException, E {
                checkArgNotNegative(n, cs.n);

                if (!initialized) {
                    init();
                }

                cursor = n > len - cursor ? len : cursor + (int) n;
            }

            private void init() throws E {
                if (!initialized) {
                    initialized = true;
                    aar = (T[]) op.apply(Seq.this.toArrayForIntermediateOp());
                    len = aar.length;
                }
            }
        }, sorted, cmp, closeHandlers);
    }

    /**
     * Returns a new {@code Seq} that cycles through the elements of this sequence indefinitely.
     *
     * @return a new {@code Seq} that cycles through the elements indefinitely
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> cycled() throws IllegalStateException {
        assertNotClosed();

        return create(new Throwables.Iterator<>() {
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
                    iter = Seq.this.iteratorEx();
                    list = new ArrayList<>();
                }
            }
        }, false, null, closeHandlers);
    }

    /**
     * Returns a new {@code Seq} that cycles through the elements of this sequence for the specified number of rounds.
     *
     * @param rounds the number of rounds to cycle through the elements
     * @return a new {@code Seq} that cycles through the elements for the specified number of rounds
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> cycled(final long rounds) throws IllegalStateException {
        assertNotClosed();
        checkArgNotNegative(rounds, cs.rounds);

        return create(new Throwables.Iterator<>() {
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
                    iter = Seq.this.iteratorEx();
                    list = new ArrayList<>();
                }
            }
        }, false, null, closeHandlers);
    }

    /**
     * Returns a new {@code Seq} that is rate-limited to the specified number of permits per second.
     *
     * @param permitsPerSecond the number of permits per second to allow
     * @return a new {@code Seq} that is rate-limited to the specified number of permits per second
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> rateLimited(final double permitsPerSecond) throws IllegalStateException {
        checkArgPositive(permitsPerSecond, cs.permitsPerSecond);

        return rateLimited(RateLimiter.create(permitsPerSecond));
    }

    /**
     * Returns a new {@code Seq} that is rate-limited to the specified {@link RateLimiter}.
     *
     * @param rateLimiter the rate limiter to use
     * @return a new {@code Seq} that is rate-limited to the specified rate limiter
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the rate limiter is null
     */
    @IntermediateOp
    public Seq<T, E> rateLimited(final RateLimiter rateLimiter) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(rateLimiter, cs.rateLimiter);

        final Throwables.Consumer<T, E> action = it -> rateLimiter.acquire();

        return onEach(action);
    }

    /**
     * Delays each element in this {@code Seq} by the given {@link Duration} except the first element.
     *
     * @param duration the duration to delay each element
     * @return a new {@code Seq} with each element delayed by the specified duration
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the duration is null
     */
    @IntermediateOp
    public Seq<T, E> delay(final Duration duration) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(duration, cs.delay);

        final long millis = duration.toMillis();
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
     * Intersperses the specified delimiter between each element of this {@code Seq}.
     *
     * @param delimiter the element to intersperse between each element of this {@code Seq}
     * @return a new {@code Seq} with the delimiter interspersed between each element
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> intersperse(final T delimiter) throws IllegalStateException {
        assertNotClosed();

        return create(new Throwables.Iterator<>() {
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
     * Returns a new {@code Seq} that steps through the elements of this sequence with the specified step size.
     *
     * @param step the step size to use when iterating through the elements
     * @return a new {@code Seq} that steps through the elements with the specified step size
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the step size is less than or equal to zero
     */
    @Beta
    @IntermediateOp
    public Seq<T, E> step(final long step) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(step, cs.step);

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

        return create(iterator, sorted, cmp, closeHandlers);
    }

    /**
     * Returns a new {@code Seq} where each element is indexed.
     *
     * @return a new {@code Seq} where each element is indexed
     * @throws IllegalStateException if the sequence is already closed
     */
    @Beta
    @IntermediateOp
    public Seq<Indexed<T>, E> indexed() throws IllegalStateException {
        assertNotClosed();

        return map(new Throwables.Function<>() {
            private final MutableLong idx = MutableLong.of(0);

            @Override
            public Indexed<T> apply(final T t) {
                return Indexed.of(t, idx.getAndIncrement());
            }
        });
    }

    /**
     * Returns a new sequence with elements from a temporary queue which is filled by reading the elements from this sequence asynchronously with a new thread.
     * Default queue size is 64.
     * <br />
     * Mostly it's for {@code read-write with different threads} mode.
     *
     * @return a new {@code Seq} with elements read asynchronously
     * @throws IllegalStateException if the sequence is already closed
     * @see #buffered(int)
     */
    @IntermediateOp
    public Seq<T, E> buffered() throws IllegalStateException {
        assertNotClosed();

        return buffered(DEFAULT_BUFFERED_SIZE_PER_ITERATOR);
    }

    /**
     * Returns a new sequence with elements from a temporary queue which is filled by reading the elements from this sequence asynchronously with a new thread.
     * <br />
     * Mostly it's for {@code read-write with different threads} mode.
     *
     * @param bufferSize the size of the buffer
     * @return a new {@code Seq} with elements read asynchronously
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the buffer size is less than or equal to zero
     */
    @IntermediateOp
    public Seq<T, E> buffered(final int bufferSize) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgPositive(bufferSize, cs.bufferSize);

        return buffered(new ArrayBlockingQueue<>(bufferSize));
    }

    Seq<T, E> buffered(final BlockingQueue<T> queueToBuffer) {
        checkArgNotNull(queueToBuffer, cs.queueToBuffer);
        checkArgument(queueToBuffer.isEmpty(), "'queueToBuffer' must be empty");

        final Supplier<Throwables.Iterator<T, E>> supplier = () -> buffered(iteratorEx(), queueToBuffer);

        //noinspection resource
        return Seq.<Supplier<Throwables.Iterator<T, E>>, E> just(supplier) //
                .map(Supplier::get)
                .flatMap(iter -> create(iter, sorted, cmp, mergeCloseHandlers(iter::close, closeHandlers, true)));
    }

    /**
     * Merges this sequence with the given collection using the provided next selector function.
     * The next selector function determines which element to select from the two sequences.
     *
     * @param b the collection to merge with this sequence
     * @param nextSelector a BiFunction that takes two parameters: an element from this sequence and an element from the given collection. It returns a MergeResult indicating which element to select.
     * @return a new {@code Seq} resulting from merging this sequence with the given collection
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> mergeWith(final Collection<? extends T> b, final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector)
            throws IllegalStateException {
        assertNotClosed();

        return mergeWith(Seq.of(b), nextSelector);
    }

    /**
     * Merges this sequence with the given sequence using the provided next selector function.
     * The next selector function determines which element to select from the two sequences.
     *
     * @param b the sequence to merge with this sequence
     * @param nextSelector a BiFunction that takes two parameters: an element from this sequence and an element from the given sequence. It returns a MergeResult indicating which element to select.
     * @return a new {@code Seq} resulting from merging this sequence with the given sequence
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> mergeWith(final Seq<? extends T, E> b, final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector)
            throws IllegalStateException {
        assertNotClosed();

        return Seq.merge(this, b, nextSelector);
    }

    /**
     * Zips this sequence with the given collection using the provided zip function.
     * The zip function takes elements from this sequence and the given collection until either the current sequence or the given collection runs out of elements.
     * The resulting sequence will have the length of the shorter of the current sequence and the given collection.
     *
     * @param <T2> the type of elements in the given collection
     * @param <R> the type of elements in the resulting sequence
     * @param b the collection to zip with this sequence
     * @param zipFunction a BiFunction that takes two parameters: an element from this sequence and an element from the given collection. It returns a combined element of type R.
     * @return a new {@code Seq} resulting from zipping this sequence with the given collection
     * @throws IllegalStateException if the sequence is already closed
     * @see #zip(Iterable, Iterable, Throwables.BiFunction)
     * @see N#zip(Iterable, Iterable, BiFunction)
     */
    @IntermediateOp
    public <T2, R> Seq<R, E> zipWith(final Collection<T2> b, final Throwables.BiFunction<? super T, ? super T2, ? extends R, ? extends E> zipFunction)
            throws IllegalStateException {
        assertNotClosed();

        return zip(this, Seq.of(b), zipFunction);
    }

    /**
     * Zips this sequence with the given collection using the provided zip function.
     * The zip function combines elements from this sequence and the given collection until both the current sequence or the given collection runs out of elements.
     * The resulting sequence will have the length of the longer of the current sequence and the given collection.
     * If the current sequence or the given collection runs out of elements before the other, the provided default values are used.
     *
     * @param <T2> the type of elements in the given collection
     * @param <R> the type of elements in the resulting sequence
     * @param b the collection to zip with this sequence
     * @param valueForNoneA the default value to use if this sequence is shorter
     * @param valueForNoneB the default value to use if the given collection is shorter
     * @param zipFunction a BiFunction that takes two parameters: an element from this sequence and an element from the given collection. It returns a combined element of type R.
     * @return a new {@code Seq} resulting from zipping this sequence with the given collection
     * @throws IllegalStateException if the sequence is already closed
     * @see N#zip(Iterable, Iterable, Object, Object, BiFunction)
     */
    @IntermediateOp
    public <T2, R> Seq<R, E> zipWith(final Collection<T2> b, final T valueForNoneA, final T2 valueForNoneB,
            final Throwables.BiFunction<? super T, ? super T2, ? extends R, ? extends E> zipFunction) throws IllegalStateException {
        assertNotClosed();

        return zip(this, Seq.of(b), valueForNoneA, valueForNoneB, zipFunction);
    }

    /**
     * Zips this sequence with the given collections using the provided zip function.
     * The zip function takes elements from this sequence and the given collections until either the current sequence or one of the given collections runs out of elements.
     * The resulting sequence will have the length of the shortest of the current sequence and the given collections.
     *
     * @param <T2> the type of elements in the first given collection
     * @param <T3> the type of elements in the second given collection
     * @param <R> the type of elements in the resulting sequence
     * @param b the first collection to zip with this sequence
     * @param c the second collection to zip with this sequence
     * @param zipFunction a TriFunction that takes three parameters: an element from this sequence, an element from the first collection, and an element from the second collection. It returns a combined element of type R.
     * @return a new {@code Seq} resulting from zipping this sequence with the two given collections
     * @throws IllegalStateException if the sequence is already closed
     * @see #zipWith(Collection, Collection, Object, Object, Object, Throwables.TriFunction)
     * @see N#zip(Iterable, Iterable, Iterable, TriFunction)
     */
    @IntermediateOp
    public <T2, T3, R> Seq<R, E> zipWith(final Collection<T2> b, final Collection<T3> c,
            final Throwables.TriFunction<? super T, ? super T2, ? super T3, ? extends R, ? extends E> zipFunction) throws IllegalStateException {
        assertNotClosed();

        return zip(this, Seq.of(b), Seq.of(c), zipFunction);
    }

    /**
     * Zips this sequence with the given collections using the provided zip function.
     * The zip function combines elements from this sequence and the given collections until both the current sequence or the given collections runs out of elements.
     * The resulting sequence will have the length of the longest of the current sequence and the given collections.
     * If the current sequence or one of the given collections runs out of elements before the other, the provided default values are used.
     *
     * @param <T2> the type of elements in the first given collection
     * @param <T3> the type of elements in the second given collection
     * @param <R> the type of elements in the resulting sequence
     * @param b the first collection to zip with this sequence
     * @param c the second collection to zip with this sequence
     * @param valueForNoneA the default value to use if this sequence is shorter
     * @param valueForNoneB the default value to use if the first given collection is shorter
     * @param valueForNoneC the default value to use if the second given collection is shorter
     * @param zipFunction a TriFunction that takes three parameters: an element from this sequence, an element from the first collection, and an element from the second collection. It returns a combined element of type R.
     * @return a new {@code Seq} resulting from zipping this sequence with the two given collections
     * @throws IllegalStateException if the sequence is already closed
     * @see N#zip(Iterable, Iterable, Iterable, Object, Object, Object, TriFunction)
     */
    @IntermediateOp
    public <T2, T3, R> Seq<R, E> zipWith(final Collection<T2> b, final Collection<T3> c, final T valueForNoneA, final T2 valueForNoneB, final T3 valueForNoneC,
            final Throwables.TriFunction<? super T, ? super T2, ? super T3, ? extends R, ? extends E> zipFunction) throws IllegalStateException {
        assertNotClosed();

        return zip(this, Seq.of(b), Seq.of(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    /**
     * Zips this sequence with the given sequence using the provided zip function.
     * The zip function takes elements from this sequence and the given sequence until either the current sequence or the given sequence runs out of elements.
     * The resulting sequence will have the length of the shorter of the current sequence and the given sequence.
     *
     * @param <T2> the type of elements in the given sequence
     * @param <R> the type of elements in the resulting sequence
     * @param b the sequence to zip with this sequence
     * @param zipFunction a BiFunction that takes two parameters: an element from this sequence and an element from the given sequence. It returns a combined element of type R.
     * @return a new {@code Seq} resulting from zipping this sequence with the given sequence
     * @throws IllegalStateException if the sequence is already closed
     * @see #zip(Seq, Seq, Object, Object, Throwables.BiFunction)
     * @see N#zip(Iterable, Iterable, BiFunction)
     */
    @IntermediateOp
    public <T2, R> Seq<R, E> zipWith(final Seq<T2, E> b, final Throwables.BiFunction<? super T, ? super T2, ? extends R, ? extends E> zipFunction)
            throws IllegalStateException {
        assertNotClosed();

        return zip(this, b, zipFunction);
    }

    /**
     * Zips this sequence with the given sequence using the provided zip function.
     * The zip function combines elements from this sequence and the given sequence until both the current sequence or the given sequence runs out of elements.
     * The resulting sequence will have the length of the longer of the current sequence and the given sequence.
     * If the current sequence or the given sequence runs out of elements before the other, the provided default values are used.
     *
     * @param <T2> the type of elements in the given sequence
     * @param <R> the type of elements in the resulting sequence
     * @param b the sequence to zip with this sequence
     * @param valueForNoneA the default value to use if this sequence is shorter
     * @param valueForNoneB the default value to use if the given sequence is shorter
     * @param zipFunction a BiFunction that takes two parameters: an element from this sequence and an element from the given sequence. It returns a combined element of type R.
     * @return a new {@code Seq} resulting from zipping this sequence with the given sequence
     * @throws IllegalStateException if the sequence is already closed
     * @see N#zip(Iterable, Iterable, Object, Object, BiFunction)
     */
    @IntermediateOp
    public <T2, R> Seq<R, E> zipWith(final Seq<T2, E> b, final T valueForNoneA, final T2 valueForNoneB,
            final Throwables.BiFunction<? super T, ? super T2, ? extends R, ? extends E> zipFunction) throws IllegalStateException {
        assertNotClosed();

        return zip(this, b, valueForNoneA, valueForNoneB, zipFunction);
    }

    /**
     * Zips this sequence with the given sequences using the provided zip function.
     * The zip function takes elements from this sequence and the given sequences until either the current sequence or one of the given sequences runs out of elements.
     * The resulting sequence will have the length of the shortest of the current sequence and the given sequences.
     *
     * @param <T2> the type of elements in the first given sequence
     * @param <T3> the type of elements in the second given sequence
     * @param <R> the type of elements in the resulting sequence
     * @param b the first sequence to zip with this sequence
     * @param c the second sequence to zip with this sequence
     * @param zipFunction a TriFunction that takes three parameters: an element from this sequence, an element from the first sequence, and an element from the second sequence. It returns a combined element of type R.
     * @return a new {@code Seq} resulting from zipping this sequence with the two given sequences
     * @throws IllegalStateException if the sequence is already closed
     * @see #zip(Seq, Seq, Seq, Object, Object, Object, Throwables.TriFunction)
     * @see N#zip(Iterable, Iterable, Iterable, TriFunction)
     */
    @IntermediateOp
    public <T2, T3, R> Seq<R, E> zipWith(final Seq<T2, E> b, final Seq<T3, E> c,
            final Throwables.TriFunction<? super T, ? super T2, ? super T3, ? extends R, ? extends E> zipFunction) throws IllegalStateException {
        assertNotClosed();

        return zip(this, b, c, zipFunction);
    }

    /**
     * Zips this sequence with the given sequences using the provided zip function.
     * The zip function combines elements from this sequence and the given sequences until both the current sequence or the given sequences runs out of elements.
     * The resulting sequence will have the length of the longest of the current sequence and the given sequences.
     * If the current sequence or one of the given sequences runs out of elements before the other, the provided default values are used.
     *
     * @param <T2> the type of elements in the first given sequence
     * @param <T3> the type of elements in the second given sequence
     * @param <R> the type of elements in the resulting sequence
     * @param b the first sequence to zip with this sequence
     * @param c the second sequence to zip with this sequence
     * @param valueForNoneA the default value to use if this sequence is shorter
     * @param valueForNoneB the default value to use if the first given sequence is shorter
     * @param valueForNoneC the default value to use if the second given sequence is shorter
     * @param zipFunction a TriFunction that takes three parameters: an element from this sequence, an element from the first sequence, and an element from the second sequence. It returns a combined element of type R.
     * @return a new {@code Seq} resulting from zipping this sequence with the two given sequences
     * @throws IllegalStateException if the sequence is already closed
     * @see N#zip(Iterable, Iterable, Iterable, Object, Object, Object, TriFunction)
     */
    @IntermediateOp
    public <T2, T3, R> Seq<R, E> zipWith(final Seq<T2, E> b, final Seq<T3, E> c, final T valueForNoneA, final T2 valueForNoneB, final T3 valueForNoneC,
            final Throwables.TriFunction<? super T, ? super T2, ? super T3, ? extends R, ? extends E> zipFunction) throws IllegalArgumentException {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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

    //    /**
    //     *
    //     * @param <E2>
    //     * @param threadNum
    //     * @param action
    //     * @throws IllegalArgumentException
    //     * @throws E the e
    //     * @throws E2 the e2
    //     */
    //    @Beta
    //    @TerminalOp
    //    public <E2 extends Exception> void forEachInParallel(final int threadNum, final Throwables.Consumer<? super T, E2> action)
    //            throws IllegalArgumentException, E, E2 {
    //        assertNotClosed();
    //        checkArgNotNull(action, cs.action);
    //
    //        try {
    //            unchecked().parallel(threadNum).forEach(action);
    //        } catch (final Exception e) {
    //            throw (E) ExceptionUtil.tryToGetOriginalCheckedException(e);
    //        }
    //    }
    //
    //    /**
    //     *
    //     * @param <E2>
    //     * @param threadNum
    //     * @param action
    //     * @param executor
    //     * @throws IllegalArgumentException
    //     * @throws E the e
    //     * @throws E2 the e2
    //     */
    //    @Beta
    //    @TerminalOp
    //    public <E2 extends Exception> void forEachInParallel(final int threadNum, final Throwables.Consumer<? super T, E2> action, final Executor executor)
    //            throws IllegalArgumentException, E, E2 {
    //        assertNotClosed();
    //        checkArgNotNull(action, cs.action);
    //
    //        try {
    //            unchecked().parallel(threadNum, executor).forEach(action);
    //        } catch (final Exception e) {
    //            throw (E) ExceptionUtil.tryToGetOriginalCheckedException(e);
    //        }
    //    }

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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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

    /**
     *
     * @param action
     * @throws E
     * @see #forEach(Throwables.Consumer)
     */
    @Beta
    @TerminalOp
    public void foreach(final Consumer<? super T> action) throws E {
        assertNotClosed();
        checkArgNotNull(action, cs.action);

        try {
            while (elements.hasNext()) {
                action.accept(elements.next());
            }
        } finally {
            close();
        }
    }

    /**
     * Performs the given action for each element of this sequence.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <E2> the type of exception that the action may throw
     * @param action a {@code Throwables.Consumer} that will be applied to each element
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the action is null
     * @throws E if an exception occurs during iteration
     * @throws E2 if the action throws an exception
     */
    @TerminalOp
    public <E2 extends Exception> void forEach(final Throwables.Consumer<? super T, E2> action) throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();
        checkArgNotNull(action, cs.action);

        try {
            while (elements.hasNext()) {
                action.accept(elements.next());
            }
        } finally {
            close();
        }
    }

    /**
     * Performs the given action for each element of this sequence, providing the index of the element.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <E2> the type of exception that the action may throw
     * @param action a {@code Throwables.IntObjConsumer} that will be applied to each element along with its index
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the action is null
     * @throws E if an exception occurs during iteration
     * @throws E2 if the action throws an exception
     */
    @TerminalOp
    public <E2 extends Exception> void forEachIndexed(final Throwables.IntObjConsumer<? super T, E2> action)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();
        checkArgNotNull(action, cs.action);

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
     * Iterate and execute the given action until the flag is set to {@code true}.
     * The flag can only be set after at least one element is iterated and executed by the action.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <E2> the type of exception that the action may throw
     * @param action a {@code Throwables.BiConsumer} that takes an element and a {@code MutableBoolean} flag.
     *               Set the flag to {@code true} to break the loop if you don't want to continue the action.
     *               Iteration on this sequence will also be stopped when this flag is set to {@code true}.
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the action is null
     * @throws E if an exception occurs during iteration
     * @throws E2 if the action throws an exception
     * @see #forEachUntil(MutableBoolean, Throwables.Consumer)
     */
    @Beta
    @TerminalOp
    public <E2 extends Exception> void forEachUntil(final Throwables.BiConsumer<? super T, MutableBoolean, E2> action)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();

        final MutableBoolean flagToBreak = MutableBoolean.of(false);
        final Throwables.Consumer<? super T, E2> tmp = t -> action.accept(t, flagToBreak);

        //noinspection resource
        takeWhile(value -> flagToBreak.isFalse()).forEach(tmp);
    }

    /**
     * Iterate and execute {@code action} until {@code flagToBreak} is set {@code true}.
     * If {@code flagToBreak} is set to {@code true} at the begin, there will be no element iterated from sequence.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @implNote it's equivalent to {@code takeWhile(value -> flagToBreak.isFalse()).forEach(action)}.
     *
     * @param <E2> the type of exception that the action may throw
     * @param flagToBreak a {@code MutableBoolean} flag to break the for-each loop.
     *                    Set it to {@code true} to break the loop if you don't want to continue the action.
     *                    Iteration on this sequence will also be stopped when this flag is set to {@code true}.
     * @param action a {@code Throwables.Consumer} that will be applied to each element.
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the action is null
     * @throws E if an exception occurs during iteration
     * @throws E2 if the action throws an exception
     * @see #forEachUntil(Throwables.BiConsumer)
     */
    @Beta
    @TerminalOp
    public <E2 extends Exception> void forEachUntil(final MutableBoolean flagToBreak, final Throwables.Consumer<? super T, E2> action)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();

        //noinspection resource
        takeWhile(value -> flagToBreak.isFalse()).forEach(action);
    }

    /**
     * Performs the given action for each element of this sequence, and then performs the provided {@code onComplete}.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <E2> the type of exception that the action may throw
     * @param <E3> the type of exception that the onComplete action may throw
     * @param action a {@code Throwables.Consumer} that will be applied to each element
     * @param onComplete a {@code Throwables.Runnable} that will be applied after the action is applied to all elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the action or onComplete is null
     * @throws E if an exception occurs during iteration
     * @throws E2 if the action throws an exception
     * @throws E3 if the onComplete action throws an exception
     */
    @TerminalOp
    public <E2 extends Exception, E3 extends Exception> void forEach(final Throwables.Consumer<? super T, E2> action, final Throwables.Runnable<E3> onComplete)
            throws IllegalStateException, IllegalArgumentException, E, E2, E3 {
        assertNotClosed();
        checkArgNotNull(action, cs.action);
        checkArgNotNull(onComplete, cs.onComplete);

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
     * Iterates over the elements of this sequence, applying a flat-mapping function to each element
     * and then executing the given action on each resulting element.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <U> the type of elements in the iterable returned by the flat-mapper
     * @param <E2> the type of exception that the flat-mapper may throw
     * @param <E3> the type of exception that the action may throw
     * @param flatMapper a {@code Throwables.Function} that takes an element of this sequence and returns an {@code Iterable} of elements
     * @param action a {@code Throwables.BiConsumer} that takes an element of this sequence and an element of the iterable returned by the flat-mapper
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the flatMapper or action is null
     * @throws E if an exception occurs during iteration
     * @throws E2 if the flat-mapper throws an exception
     * @throws E3 if the action throws an exception
     */
    @TerminalOp
    public <U, E2 extends Exception, E3 extends Exception> void forEach(final Throwables.Function<? super T, ? extends Iterable<? extends U>, E2> flatMapper,
            final Throwables.BiConsumer<? super T, ? super U, E3> action) throws IllegalStateException, IllegalArgumentException, E, E2, E3 {
        assertNotClosed();
        checkArgNotNull(flatMapper, cs.flatMapper);
        checkArgNotNull(action, cs.action);

        Iterable<? extends U> c = null;
        T next = null;

        try {
            while (elements.hasNext()) {
                next = elements.next();
                c = flatMapper.apply(next);

                if (c != null) {
                    for (final U u : c) {
                        action.accept(next, u);
                    }
                }
            }
        } finally {
            close();
        }
    }

    /**
     * Iterates over the elements of this sequence, applying two flat-mapping functions to each element
     * and then executing the given action on each resulting combination of elements.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <T2> the type of elements in the iterable returned by the first flat-mapper
     * @param <T3> the type of elements in the iterable returned by the second flat-mapper
     * @param <E2> the type of exception that the first flat-mapper may throw
     * @param <E3> the type of exception that the second flat-mapper may throw
     * @param <E4> the type of exception that the action may throw
     * @param flatMapper a {@code Throwables.Function} that takes an element of this sequence and returns an {@code Iterable} of elements
     * @param flatMapper2 a {@code Throwables.Function} that takes an element of the iterable returned by the first flat-mapper and returns an {@code Iterable} of elements
     * @param action a {@code Throwables.TriConsumer} that takes an element of this sequence, an element of the iterable returned by the first flat-mapper, and an element of the iterable returned by the second flat-mapper
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the flatMapper, flatMapper2 or action is null
     * @throws E if an exception occurs during iteration
     * @throws E2 if the first flat-mapper throws an exception
     * @throws E3 if the second flat-mapper throws an exception
     * @throws E4 if the action throws an exception
     */
    @TerminalOp
    public <T2, T3, E2 extends Exception, E3 extends Exception, E4 extends Exception> void forEach(
            final Throwables.Function<? super T, ? extends Iterable<T2>, E2> flatMapper,
            final Throwables.Function<? super T2, ? extends Iterable<T3>, E3> flatMapper2,
            final Throwables.TriConsumer<? super T, ? super T2, ? super T3, E4> action) throws IllegalStateException, IllegalArgumentException, E, E2, E3, E4 {
        assertNotClosed();
        checkArgNotNull(flatMapper, cs.flatMapper);
        checkArgNotNull(flatMapper2, cs.flatMapper2);
        checkArgNotNull(action, cs.action);

        Iterable<T2> c2 = null;
        Iterable<T3> c3 = null;
        T next = null;

        try {
            while (elements.hasNext()) {
                next = elements.next();
                c2 = flatMapper.apply(next);

                if (c2 != null) {
                    for (final T2 t2 : c2) {
                        c3 = flatMapper2.apply(t2);

                        if (c3 != null) {
                            for (final T3 t3 : c3) {
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
     * Iterates over the elements of this sequence in pairs and applies the given action to each pair.
     * The pairs are formed by taking two consecutive elements from the sequence.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <E2> the type of exception that the action may throw
     * @param action a {@code Throwables.BiConsumer} that takes two elements of this sequence and performs an action
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the action is null
     * @throws E if an exception occurs during iteration
     * @throws E2 if the action throws an exception
     * @see #forEachPair(int, Throwables.BiConsumer)
     * @see N#forEachPair(Iterable, Throwables.BiConsumer)
     */
    @TerminalOp
    public <E2 extends Exception> void forEachPair(final Throwables.BiConsumer<? super T, ? super T, E2> action)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        forEachPair(1, action);
    }

    /**
     * Iterates over the elements of this sequence in pairs with a specified increment and applies the given action to each pair.
     * The pairs are formed by taking two elements from the sequence separated by the specified increment.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <E2> the type of exception that the action may throw
     * @param increment the number of elements to skip between pairs
     * @param action a {@code Throwables.BiConsumer} that takes two elements of this sequence and performs an action
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the increment is less than 1 or the action is null
     * @throws E if an exception occurs during iteration
     * @throws E2 if the action throws an exception
     * @see #forEachPair(Throwables.BiConsumer)
     * @see N#forEachPair(Iterable, int, Throwables.BiConsumer)
     */
    @TerminalOp
    public <E2 extends Exception> void forEachPair(final int increment, final Throwables.BiConsumer<? super T, ? super T, E2> action)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();

        final int windowSize = 2;
        checkArgPositive(increment, cs.increment);

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
     * Iterates over the elements of this sequence in triples and applies the given action to each triple.
     * The triples are formed by taking three consecutive elements from the sequence.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <E2> the type of exception that the action may throw
     * @param action a {@code Throwables.TriConsumer} that takes three elements of this sequence and performs an action
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the action is null
     * @throws E if an exception occurs during iteration
     * @throws E2 if the action throws an exception
     * @see #forEachTriple(int, Throwables.TriConsumer)
     * @see N#forEachTriple(Iterable, Throwables.TriConsumer)
     */
    @TerminalOp
    public <E2 extends Exception> void forEachTriple(final Throwables.TriConsumer<? super T, ? super T, ? super T, E2> action)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        forEachTriple(1, action);
    }

    /**
     * Iterates over the elements of this sequence in triples with a specified increment and applies the given action to each triple.
     * The triples are formed by taking three elements from the sequence separated by the specified increment.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <E2> the type of exception that the action may throw
     * @param increment the number of elements to skip between triples
     * @param action a {@code Throwables.TriConsumer} that takes three elements of this sequence and performs an action
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the increment is less than 1 or the action is null
     * @throws E if an exception occurs during iteration
     * @throws E2 if the action throws an exception
     * @see #forEachTriple(Throwables.TriConsumer)
     * @see N#forEachTriple(Iterable, int, Throwables.TriConsumer)
     */
    @TerminalOp
    public <E2 extends Exception> void forEachTriple(final int increment, final Throwables.TriConsumer<? super T, ? super T, ? super T, E2> action)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();

        final int windowSize = 3;
        checkArgPositive(increment, cs.increment);

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

    //    /**
    //     *
    //     * @param <E2>
    //     * @param threadNum
    //     * @param action
    //     * @throws IllegalArgumentException
    //     * @throws E the e
    //     * @throws E2 the e2
    //     */
    //    @Beta
    //    @TerminalOp
    //    public <E2 extends Exception> void forEachInParallel(final int threadNum, final Throwables.Consumer<? super T, E2> action)
    //            throws IllegalArgumentException, E, E2 {
    //        assertNotClosed();
    //        checkArgNotNull(action, cs.action);
    //
    //        try {
    //            unchecked().parallel(threadNum).forEach(action);
    //        } catch (final Exception e) {
    //            throw (E) ExceptionUtil.tryToGetOriginalCheckedException(e);
    //        }
    //    }
    //
    //    /**
    //     *
    //     * @param <E2>
    //     * @param threadNum
    //     * @param action
    //     * @param executor
    //     * @throws IllegalArgumentException
    //     * @throws E the e
    //     * @throws E2 the e2
    //     */
    //    @Beta
    //    @TerminalOp
    //    public <E2 extends Exception> void forEachInParallel(final int threadNum, final Throwables.Consumer<? super T, E2> action, final Executor executor)
    //            throws IllegalArgumentException, E, E2 {
    //        assertNotClosed();
    //        checkArgNotNull(action, cs.action);
    //
    //        try {
    //            unchecked().parallel(threadNum, executor).forEach(action);
    //        } catch (final Exception e) {
    //            throw (E) ExceptionUtil.tryToGetOriginalCheckedException(e);
    //        }
    //    }

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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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
    //        return create(new Throwables.Iterator<T, E>() {
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

    /**
     * Returns an {@code Optional} containing the minimum value of this sequence according to the provided comparator.
     * If this sequence is empty, it returns an empty {@code Optional}.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param comparator the comparator to determine the order of the elements
     * @return an {@code Optional} containing the minimum value or an empty {@code Optional} if the sequence is empty
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified comparator is null
     * @throws E if an exception occurs during iteration
     * @see #minBy(Function)
     * @see Iterables#min(Iterable, Comparator)
     */
    @TerminalOp
    public Optional<T> min(Comparator<? super T> comparator) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();
        checkArgNotNull(comparator, cs.comparator);

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
     * Returns an {@code Optional} containing the minimum value of this sequence according to the key extracted by the {@code keyMapper} function.
     * Null values are considered to be maximum.
     * If this sequence is empty, it returns an empty {@code Optional}.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param keyMapper the function to extract the key for comparison
     * @return an {@code Optional} containing the minimum value or an empty {@code Optional} if the sequence is empty
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified keyMapper is null
     * @throws E if an exception occurs during iteration
     * @see #min(Comparator)
     * @see Iterables#minBy(Iterable, Function)
     */
    @TerminalOp
    @SuppressWarnings("rawtypes")
    public Optional<T> minBy(final Function<? super T, ? extends Comparable> keyMapper) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();
        checkArgNotNull(keyMapper, cs.keyMapper);

        final Comparator<? super T> comparator = Comparators.nullsLastBy(keyMapper);

        try {
            return min(comparator);
        } finally {
            close();
        }
    }

    /**
     * Returns an {@code Optional} containing the maximum value of this sequence according to the provided comparator.
     * If this sequence is empty, it returns an empty {@code Optional}.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param comparator the comparator to determine the order of the elements
     * @return an {@code Optional} containing the maximum value or an empty {@code Optional} if the sequence is empty
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified comparator is null
     * @throws E if an exception occurs during iteration
     * @see #maxBy(Function)
     * @see Iterables#max(Iterable, Comparator)
     */
    @TerminalOp
    public Optional<T> max(Comparator<? super T> comparator) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();
        checkArgNotNull(comparator, cs.comparator);

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
     * Returns an {@code Optional} containing the maximum value of this sequence according to the key extracted by the {@code keyMapper} function. Null values are considered to be minimum.
     * If this sequence is empty, it returns an empty {@code Optional}.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param keyMapper the function to extract the key for comparison
     * @return an {@code Optional} containing the maximum value or an empty {@code Optional} if the sequence is empty
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified keyMapper is null
     * @throws E if an exception occurs during iteration
     * @see #max(Comparator)
     * @see Iterables#maxBy(Iterable, Function)
     */
    @TerminalOp
    @SuppressWarnings("rawtypes")
    public Optional<T> maxBy(final Function<? super T, ? extends Comparable> keyMapper) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();
        checkArgNotNull(keyMapper, cs.keyMapper);

        final Comparator<? super T> comparator = Comparators.nullsFirstBy(keyMapper);

        try {
            return max(comparator);
        } finally {
            close();
        }
    }

    /**
     * Checks if whether any elements of this sequence match the provided predicate.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <E2> the type of exception that the predicate may throw
     * @param predicate a {@code Throwables.Predicate} to apply to elements of this sequence
     * @return {@code true} if any elements match the predicate, otherwise {@code false}
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if the predicate throws an exception
     * @see N#anyMatch(Iterable, Predicate)
     */
    @TerminalOp
    public <E2 extends Exception> boolean anyMatch(final Throwables.Predicate<? super T, E2> predicate) throws IllegalStateException, E, E2 {
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
     * Checks if whether all elements of this sequence match the provided predicate.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <E2> the type of exception that the predicate may throw
     * @param predicate a {@code Throwables.Predicate} to apply to elements of this sequence
     * @return {@code true} if all elements match the predicate or this sequence is empty, otherwise {@code false}
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if the predicate throws an exception
     * @see N#allMatch(Iterable, Predicate)
     */
    @TerminalOp
    public <E2 extends Exception> boolean allMatch(final Throwables.Predicate<? super T, E2> predicate) throws IllegalStateException, E, E2 {
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
     * Checks if whether none elements of this sequence match the provided predicate.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <E2> the type of exception that the predicate may throw
     * @param predicate a {@code Throwables.Predicate} to apply to elements of this sequence
     * @return {@code true} if none elements match the predicate or this sequence is empty, otherwise {@code false}
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if the predicate throws an exception
     * @see N#noneMatch(Iterable, Predicate)
     */
    @TerminalOp
    public <E2 extends Exception> boolean noneMatch(final Throwables.Predicate<? super T, E2> predicate) throws IllegalStateException, E, E2 {
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
     * Checks if the specified number of elements in this sequence match the provided predicate.
     * <br />
     * The operation stops as soon as the number of elements matching the predicate is bigger than {@code atMost}.
     * @implNote it's equivalent to {@code {@code atLeast} <= stream.filter(predicate).limit(atMost + 1).count() <= atMost}
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <E2> the type of exception that the predicate may throw
     * @param {@code atLeast} the minimum number of elements that must match the predicate
     * @param {@code atMost} the maximum number of elements that can match the predicate
     * @param predicate the predicate to apply to elements of this stream
     * @return {@code true} if the number of elements matching the predicate is between {@code atLeast} and {@code atMost}, inclusive
     * @throws IllegalArgumentException if {@code atLeast} or {@code atMost} is negative, or if {@code atMost} is less than {@code atLeast}
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs while processing the stream
     * @throws E2 if the predicate throws an exception
     */
    @TerminalOp
    public <E2 extends Exception> boolean nMatch(final long atLeast, final long atMost, final Throwables.Predicate<? super T, E2> predicate)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();
        checkArgNotNegative(atLeast, cs.atLeast);
        checkArgNotNegative(atMost, cs.atMost);
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
     * Returns the first element in this sequence that matches the specified predicate.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <E2> the type of exception that the predicate may throw
     * @param predicate a {@code Throwables.Predicate} to apply to elements of this sequence
     * @return an {@code Optional} containing the first matching element, or an empty {@code Optional} if no match is found
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if the predicate throws an exception
     * @see #findAny(Throwables.Predicate)
     * @see N#findFirst(Iterable, Predicate)
     */
    @TerminalOp
    public <E2 extends Exception> Optional<T> findFirst(final Throwables.Predicate<? super T, E2> predicate) throws IllegalStateException, E, E2 {
        assertNotClosed();

        try {
            T next = null;

            while (elements.hasNext()) {
                next = elements.next();

                if (predicate.test(next)) {
                    return Optional.of(next);
                }
            }

            return Optional.empty();
        } finally {
            close();
        }
    }

    /**
     * Returns the first element in this sequence that matches the specified predicate.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <E2> the type of exception that the predicate may throw
     * @param predicate a {@code Throwables.Predicate} to apply to elements of this sequence
     * @return an {@code Optional} containing the first matching element, or an empty {@code Optional} if no match is found
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if the predicate throws an exception
     * @see #findFirst(Throwables.Predicate)
     * @deprecated replaced by {@link #findFirst(Throwables.Predicate)}
     */
    @Deprecated
    @TerminalOp
    public <E2 extends Exception> Optional<T> findAny(final Throwables.Predicate<? super T, E2> predicate) throws IllegalStateException, E, E2 {
        return findFirst(predicate);
    }

    /**
     * Returns the last element in this sequence that matches the specified predicate.
     * Consider using: {@code seq.reversed().findFirst(predicate)} for better performance if possible.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <E2> the type of exception that the predicate may throw
     * @param predicate a {@code Throwables.Predicate} to apply to elements of this sequence
     * @return an {@code Optional} containing the last matching element, or an empty {@code Optional} if no match is found
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if the predicate throws an exception
     * @see #reversed()
     * @see #findFirst(Throwables.Predicate)
     * @see N#findLast(Iterable, Predicate)
     */
    @Beta
    @TerminalOp
    public <E2 extends Exception> Optional<T> findLast(final Throwables.Predicate<? super T, E2> predicate) throws IllegalStateException, E, E2 {
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

            return result == NONE ? Optional.empty() : Optional.of(result);
        } finally {
            close();
        }
    }

    //    /**
    //     * Returns the first element matched by {@code predicateForFirst} if found or the first element if this stream is not empty
    //     * Otherwise an empty {@code Optional<T>} will be returned.
    //     *
    //     * @param <E2>
    //     * @param predicateForFirst
    //     * @return
    //     * @throws IllegalStateException
    //     * @throws E
    //     * @throws E2
    //     */
    //    @TerminalOp
    //    public <E2 extends Exception> Optional<T> findFirstOrElseAny(final Throwables.Predicate<? super T, E2> predicateForFirst)
    //            throws IllegalStateException, E, E2 {
    //        assertNotClosed();
    //
    //        try {
    //            T ret = (T) NONE;
    //            T next = null;
    //
    //            while (elements.hasNext()) {
    //                next = elements.next();
    //
    //                if (predicateForFirst.test(next)) {
    //                    return Optional.of(next);
    //                } else if (ret == NONE) {
    //                    ret = next;
    //                }
    //            }
    //
    //            return ret == NONE ? Optional.<T> empty() : Optional.of(ret);
    //        } finally {
    //            close();
    //        }
    //    }
    //
    //    /**
    //     * Returns the first element matched by {@code predicateForFirst} if found or the last element if this stream is not empty
    //     * Otherwise an empty {@code Optional<T>} will be returned.
    //     *
    //     * @param <E2>
    //     * @param predicateForFirst
    //     * @return
    //     * @throws IllegalStateException
    //     * @throws E
    //     * @throws E2
    //     */
    //    @TerminalOp
    //    public <E2 extends Exception> Optional<T> findFirstOrElseLast(final Throwables.Predicate<? super T, E2> predicateForFirst)
    //            throws IllegalStateException, E, E2 {
    //        assertNotClosed();
    //
    //        try {
    //            T ret = (T) NONE;
    //            T next = null;
    //
    //            while (elements.hasNext()) {
    //                next = elements.next();
    //
    //                if (predicateForFirst.test(next)) {
    //                    return Optional.of(next);
    //                } else {
    //                    ret = next;
    //                }
    //            }
    //
    //            return ret == NONE ? Optional.<T> empty() : Optional.of(ret);
    //        } finally {
    //            close();
    //        }
    //    }
    //
    //    /**
    //     * Returns the first element matched by {@code predicateForFirst} if found or the first element matched by {@code predicateForAny}.
    //     * Otherwise an empty {@code Optional<T>} will be returned.
    //     *
    //     * @param <E2>
    //     * @param <E3>
    //     * @param predicateForFirst
    //     * @param predicateForAny
    //     * @return
    //     * @throws IllegalStateException
    //     * @throws E
    //     * @throws E2
    //     * @throws E3
    //     */
    //    @TerminalOp
    //    public <E2 extends Exception, E3 extends Exception> Optional<T> findFirstOrAny(final Throwables.Predicate<? super T, E2> predicateForFirst,
    //            final Throwables.Predicate<? super T, E3> predicateForAny) throws IllegalStateException, E, E2, E3 {
    //        assertNotClosed();
    //
    //        try {
    //            T ret = (T) NONE;
    //            T next = null;
    //
    //            while (elements.hasNext()) {
    //                next = elements.next();
    //
    //                if (predicateForFirst.test(next)) {
    //                    return Optional.of(next);
    //                } else if (ret == NONE && predicateForAny.test(next)) {
    //                    ret = next;
    //                }
    //            }
    //
    //            return ret == NONE ? Optional.<T> empty() : Optional.of(ret);
    //        } finally {
    //            close();
    //        }
    //    }

    /**
     * Checks if this sequence contains all the specified elements.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param a the elements to check for presence in this sequence
     * @return {@code true} if this sequence contains all the specified elements, otherwise {@code false}
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @see N#containsAll(Collection, Object...)
     */
    @TerminalOp
    @SafeVarargs
    public final boolean containsAll(final T... a) throws IllegalStateException, E {
        assertNotClosed();

        try {
            if (N.isEmpty(a)) {
                return true;
            } else if (a.length == 1 || (a.length == 2 && N.equals(a[0], a[1]))) {
                return anyMatch(Fnn.<T, E> pp(Fn.equal(a[0])));
            } else if (a.length == 2) {
                //noinspection resource
                return filter(new Throwables.Predicate<>() {
                    private final T val1 = a[0];
                    private final T val2 = a[1];

                    @Override
                    public boolean test(final T t) {
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
     * Checks if this sequence contains all the elements in the specified collection.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param c the collection of elements to check for presence in this sequence
     * @return {@code true} if this sequence contains all the elements in the specified collection, otherwise {@code false}
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @see N#containsAll(Collection, Collection)
     */
    @TerminalOp
    public boolean containsAll(final Collection<? extends T> c) throws IllegalStateException, E {
        assertNotClosed();

        try {
            if (N.isEmpty(c)) {
                return true;
            } else if (c.size() == 1) {
                final T val = c instanceof List ? ((List<T>) c).get(0) : c.iterator().next();
                return anyMatch(Fnn.<T, E> pp(Fn.equal(val)));
            } else {
                final Set<T> set = c instanceof Set ? (Set<T>) c : N.newHashSet(c);
                final int distinctCount = set.size();

                //noinspection resource
                return filter(set::contains).distinct().limit(distinctCount).count() == distinctCount;
            }
        } finally {
            close();
        }
    }

    /**
     * Checks if this sequence contains any of the specified elements.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param a the elements to check for presence in this sequence
     * @return {@code true} if this sequence contains any of the specified elements, otherwise {@code false}
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @see N#containsAny(Collection, Object...)
     */
    @TerminalOp
    @SafeVarargs
    public final boolean containsAny(final T... a) throws IllegalStateException, E {
        assertNotClosed();

        try {
            if (N.isEmpty(a)) {
                return false;
            } else if (a.length == 1 || (a.length == 2 && N.equals(a[0], a[1]))) {
                return anyMatch(Fnn.<T, E> pp(Fn.equal(a[0])));
            } else if (a.length == 2) {
                return anyMatch(new Throwables.Predicate<T, E>() {
                    private final T val1 = a[0];
                    private final T val2 = a[1];

                    @Override
                    public boolean test(final T t) {
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
     * Checks if this sequence contains any of the elements in the specified collection.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param c the collection of elements to check for presence in this sequence
     * @return {@code true} if this sequence contains any of the elements in the specified collection, otherwise {@code false}
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @see N#containsAny(Collection, Collection)
     */
    @TerminalOp
    public boolean containsAny(final Collection<? extends T> c) throws IllegalStateException, E {
        assertNotClosed();

        try {
            if (N.isEmpty(c)) {
                return false;
            } else if (c.size() == 1) {
                final T val = c instanceof List ? ((List<T>) c).get(0) : c.iterator().next();
                return anyMatch(Fnn.<T, E> pp(Fn.equal(val)));
            } else {
                final Set<T> set = c instanceof Set ? (Set<T>) c : N.newHashSet(c);

                return anyMatch(set::contains);
            }
        } finally {
            close();
        }
    }

    /**
     * Checks if the sequence doesn't contain any of the specified elements.
     *
     * @param a the elements to check for presence in the sequence
     * @return {@code true} if the sequence doesn't contain any of the specified elements, or if this {@code sequence} is empty, or if {@code valuesToFind} is {@code null} or empty, {@code false} otherwise
     * @see #containsAny(Object[])
     */
    @TerminalOp
    @SafeVarargs
    public final boolean containsNone(final T... a) throws IllegalStateException, E {
        assertNotClosed();

        try {
            if (N.isEmpty(a)) {
                return true;
            }

            return !containsAny(a);
        } finally {
            close();
        }
    }

    /**
     * Checks if the sequence doesn't contain any of the specified elements.
     *
     * @param c the elements to check for presence in the sequence
     * @return {@code true} if the sequence doesn't contain any of the specified elements, or if this {@code sequence} is empty, or if {@code valuesToFind} is {@code null} or empty, {@code false} otherwise
     * @see #containsAny(Collection)
     */
    @TerminalOp
    public boolean containsNone(final Collection<? extends T> c) throws IllegalStateException, E {
        assertNotClosed();

        try {
            if (N.isEmpty(c)) {
                return true;
            }

            return !containsAny(c);
        } finally {
            close();
        }
    }

    /**
     * Checks if this sequence contains duplicate elements.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @return {@code true} if this sequence contains duplicate elements, otherwise {@code false}
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @see N#hasDuplicates(Collection)
     */
    @TerminalOp
    public boolean hasDuplicates() throws IllegalStateException, E {
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
     * Returns the <i>k-th</i> largest element in this sequence according to the provided comparator.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param k the position (1-based) of the largest element to find
     * @param comparator the comparator to determine the order of the elements
     * @return an {@code Optional} containing the k-th largest element, or an empty {@code Optional} if the sequence has fewer than k elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if k is less than 1
     * @throws E if an exception occurs during iteration
     * @see N#kthLargest(Collection, int, Comparator)
     */
    @TerminalOp
    public Optional<T> kthLargest(final int k, Comparator<? super T> comparator) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();
        checkArgPositive(k, cs.k);

        try {
            if (!elements.hasNext()) {
                return Optional.empty();
            } else if (sorted && isSameComparator(comparator, cmp)) {
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
     * Calculates and returns the percentiles of the elements in this stream.
     * All elements will be loaded into memory and sorted if not yet.
     * The returned map contains the percentile values as keys and the corresponding elements as values.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @return an {@code Optional} containing a map of percentiles and their corresponding elements, or an empty {@code Optional} if the sequence is empty
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @see N#percentiles(int[])
     */
    public Optional<Map<Percentage, T>> percentiles() throws IllegalStateException, E {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
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
     * Calculates the percentiles of the elements in the sequence according to the provided comparator.
     * All elements will be loaded into memory and sorted if not yet.
     * The returned map contains the percentile values as keys and the corresponding elements as values.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param comparator A comparator to determine the order of the elements.
     * @return An Optional containing a Map where the keys are the percentiles and the values are the corresponding elements, or an empty {@code Optional} if the sequence is empty.
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified comparator
     * @throws E if an exception occurs during iteration
     * @see N#percentiles(int[])
     */
    @TerminalOp
    public Optional<Map<Percentage, T>> percentiles(final Comparator<? super T> comparator) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();
        checkArgNotNull(comparator, cs.comparator);

        try {
            @SuppressWarnings("resource")
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
     * Returns the first element of this sequence.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @return an {@code Optional} containing the first element, or an empty {@code Optional} if the sequence is empty
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     */
    @TerminalOp
    public Optional<T> first() throws IllegalStateException, E {
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
     * Returns the last element of this sequence.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @return an {@code Optional} containing the last element, or an empty {@code Optional} if the sequence is empty
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     */
    @TerminalOp
    public Optional<T> last() throws IllegalStateException, E {
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
     * Returns the element at the specified position in this sequence.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param position the position of the element to return
     * @return an {@code Optional} containing the element at the specified position, or an empty {@code Optional} if the sequence has fewer than {@code position} elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if {@code position} is negative
     * @throws E if an exception occurs during iteration
     */
    @Beta
    @TerminalOp
    public Optional<T> elementAt(final long position) throws IllegalStateException, IllegalArgumentException, E {
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
     * Returns an {@code Optional} containing the only element of this sequence if it contains exactly one element, or an empty {@code Optional} if the sequence is empty
     * If the sequence contains more than one element, an exception is thrown.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @return an {@code Optional} containing the only element of this sequence, or an empty {@code Optional} if the sequence is empty
     * @throws IllegalStateException if the sequence is already closed
     * @throws TooManyElementsException if the sequence contains more than one element
     * @throws E if an exception occurs during iteration
     */
    @TerminalOp
    public Optional<T> onlyOne() throws IllegalStateException, TooManyElementsException, E {
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
     * Returns the count of elements in this sequence.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @return the count of elements in this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     */
    @TerminalOp
    public long count() throws IllegalStateException, E {
        assertNotClosed();

        try {
            return elements.count();
        } finally {
            close();
        }
    }

    /**
     * Returns an array containing all the elements in this sequence.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @return an array containing all elements in this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     */
    @TerminalOp
    public Object[] toArray() throws IllegalStateException, E {
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
     * Returns an array containing all the elements in this sequence,
     * using the provided {@code generator} function to allocate the returned array.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <A> the type of the array elements
     * @param generator a function which produces a new array of the desired type and the provided length
     * @return an array containing all elements in this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the generator function is null
     * @throws E if an exception occurs during iteration
     * @see #toList()
     * @see List#toArray(IntFunction)
     */
    @TerminalOp
    public <A> A[] toArray(final IntFunction<A[]> generator) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();
        checkArgNotNull(generator, cs.generator);

        try {
            final List<T> list = toList();

            return list.toArray(generator.apply(list.size()));
        } finally {
            close();
        }
    }

    /**
     * Returns a list containing all the elements in this sequence.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @return a list containing all elements in this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     */
    @TerminalOp
    public List<T> toList() throws IllegalStateException, E {
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
     * Returns a set containing all the elements in this sequence.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @return a set containing all elements in this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     */
    @TerminalOp
    public Set<T> toSet() throws IllegalStateException, E {
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
     * Returns a collection containing all the elements in this sequence,
     * using the provided {@code supplier} to create the returned collection.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <C> the type of the collection
     * @param supplier a function which produces a new collection of the desired type
     * @return a collection containing all elements in this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the supplier function is null
     * @throws E if an exception occurs during iteration
     */
    @TerminalOp
    public <C extends Collection<T>> C toCollection(final Supplier<? extends C> supplier) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();
        checkArgNotNull(supplier, cs.supplier);

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
     * Returns an immutable list containing all the elements in this sequence.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @return an immutable list containing all elements in this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     */
    @TerminalOp
    public ImmutableList<T> toImmutableList() throws IllegalStateException, E {
        return ImmutableList.wrap(toList());
    }

    /**
     * Returns an immutable set containing all the elements in this sequence.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @return an immutable set containing all elements in this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     */
    @TerminalOp
    public ImmutableSet<T> toImmutableSet() throws IllegalStateException, E {
        return ImmutableSet.wrap(toSet());
    }

    /**
     * Applies the given function to the list of elements in this sequence.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <R> the type of the result
     * @param <E2> the type of exception that may be thrown by the function
     * @param func the function to apply to the list of elements
     * @return the result of applying the function to the list of elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if specified function is null
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during the function application
     */
    @TerminalOp
    public <R, E2 extends Exception> R toListThenApply(final Throwables.Function<? super List<T>, R, E2> func)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();
        checkArgNotNull(func, cs.func);

        return func.apply(toList());
    }

    /**
     * Applies the given consumer to the list of elements in this sequence.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <E2> the type of exception that may be thrown by the consumer
     * @param consumer the consumer to apply to the list of elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if specified consumer is null
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during the consumer application
     */
    @TerminalOp
    public <E2 extends Exception> void toListThenAccept(final Throwables.Consumer<? super List<T>, E2> consumer)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();
        checkArgNotNull(consumer, cs.consumer);

        consumer.accept(toList());
    }

    /**
     * Applies the given function to the set of elements in this sequence.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <R> the type of the result
     * @param <E2> the type of exception that may be thrown by the function
     * @param func the function to apply to the set of elements
     * @return the result of applying the function to the set of elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified function is null
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during the function application
     */
    @TerminalOp
    public <R, E2 extends Exception> R toSetThenApply(final Throwables.Function<? super Set<T>, R, E2> func)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();
        checkArgNotNull(func, cs.func);

        return func.apply(toSet());
    }

    /**
     * Applies the given consumer to the set of elements in this sequence.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <E2> the type of exception that may be thrown by the consumer
     * @param consumer the consumer to apply to the set of elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified consumer is null
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during the consumer application
     */
    @TerminalOp
    public <E2 extends Exception> void toSetThenAccept(final Throwables.Consumer<? super Set<T>, E2> consumer)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();
        checkArgNotNull(consumer, cs.consumer);

        consumer.accept(toSet());
    }

    /**
     * Applies the given function to the collection of elements in this sequence.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <R> the type of the result
     * @param <C> the type of the collection
     * @param <E2> the type of exception that may be thrown by the function
     * @param supplier a function which produces a new collection of the desired type
     * @param func the function to apply to the collection of elements
     * @return the result of applying the function to the collection of elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified supplier or function is null
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during the function application
     */
    @TerminalOp
    public <R, C extends Collection<T>, E2 extends Exception> R toCollectionThenApply(final Supplier<? extends C> supplier,
            final Throwables.Function<? super C, R, E2> func) throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();
        checkArgNotNull(supplier, cs.supplier);
        checkArgNotNull(func, cs.func);

        return func.apply(toCollection(supplier));
    }

    /**
     * Applies the given consumer to the collection of elements in this sequence.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <C> the type of the collection
     * @param <E2> the type of exception that may be thrown by the consumer
     * @param supplier the supplier to create the collection
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified supplier or consumer is null
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during the consumer application
     */
    @TerminalOp
    public <C extends Collection<T>, E2 extends Exception> void toCollectionThenAccept(final Supplier<? extends C> supplier,
            final Throwables.Consumer<? super C, E2> consumer) throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();
        checkArgNotNull(supplier, cs.supplier);
        checkArgNotNull(consumer, cs.consumer);

        consumer.accept(toCollection(supplier));
    }

    /**
     * Converts the elements in this sequence to a Map using the provided key and value extractors.
     *
     * @param <K> the type of keys in the resulting map
     * @param <V> the type of values in the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param <E3> the type of exception that may be thrown by the value extractor
     * @param keyMapper the function to extract keys from the elements
     * @param valueMapper the function to extract values from the elements
     * @return a Map containing the elements of this sequence
     * @throws IllegalStateException if the sequence is already closed or there are duplicated keys
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     * @throws E3 if an exception occurs during value extraction
     * @see Fn.Fnn#throwingMerger()
     * @see Fn.Fnn#replacingMerger()
     * @see Fn.Fnn#ignoringMerger()
     */
    @TerminalOp
    public <K, V, E2 extends Exception, E3 extends Exception> Map<K, V> toMap(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Throwables.Function<? super T, ? extends V, E3> valueMapper) throws IllegalStateException, E, E2, E3 {
        return toMap(keyMapper, valueMapper, Suppliers.ofMap());
    }

    /**
     * Converts the elements in this sequence to a Map using the provided key and value extractors.
     *
     * @param <K> the type of keys in the resulting map
     * @param <V> the type of values in the resulting map
     * @param <M> the type of the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param <E3> the type of exception that may be thrown by the value extractor
     * @param keyMapper the function to extract keys from the elements
     * @param valueMapper the function to extract values from the elements
     * @param mapFactory the supplier to create the resulting map
     * @return a Map containing the elements of this sequence
     * @throws IllegalStateException if the sequence is already closed or there are duplicated keys
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     * @throws E3 if an exception occurs during value extraction
     * @see Fn.Fnn#throwingMerger()
     * @see Fn.Fnn#replacingMerger()
     * @see Fn.Fnn#ignoringMerger()
     */
    @TerminalOp
    public <K, V, M extends Map<K, V>, E2 extends Exception, E3 extends Exception> M toMap(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Throwables.Function<? super T, ? extends V, E3> valueMapper, final Supplier<? extends M> mapFactory) throws IllegalStateException, E, E2, E3 {
        return toMap(keyMapper, valueMapper, Fnn.<V, E> throwingMerger(), mapFactory);
    }

    /**
     * Converts the elements in this sequence to a Map using the provided key and value extractors.
     *
     * @param <K> the type of keys in the resulting map
     * @param <V> the type of values in the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param <E3> the type of exception that may be thrown by the value extractor
     * @param <E4> the type of exception that may be thrown by the merge function
     * @param keyMapper the function to extract keys from the elements
     * @param valueMapper the function to extract values from the elements
     * @param mergeFunction the function to resolve collisions between values associated with the same key
     * @return a Map containing the elements of this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     * @throws E3 if an exception occurs during value extraction
     * @throws E4 if an exception occurs during merging
     * @see Fn.Fnn#throwingMerger()
     * @see Fn.Fnn#replacingMerger()
     * @see Fn.Fnn#ignoringMerger()
     */
    @TerminalOp
    public <K, V, E2 extends Exception, E3 extends Exception, E4 extends Exception> Map<K, V> toMap(
            final Throwables.Function<? super T, ? extends K, E2> keyMapper, final Throwables.Function<? super T, ? extends V, E3> valueMapper,
            final Throwables.BinaryOperator<V, E4> mergeFunction) throws IllegalStateException, E, E2, E3, E4 {
        return toMap(keyMapper, valueMapper, mergeFunction, Suppliers.ofMap());
    }

    /**
     * Converts the elements in this sequence to a Map using the provided key and value extractors.
     *
     * @param <K> the type of keys in the resulting map
     * @param <V> the type of values in the resulting map
     * @param <M> the type of the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param <E3> the type of exception that may be thrown by the value extractor
     * @param <E4> the type of exception that may be thrown by the merge function
     * @param keyMapper the function to extract keys from the elements
     * @param valueMapper the function to extract values from the elements
     * @param mergeFunction the function to resolve collisions between values associated with the same key
     * @param mapFactory the supplier to create the resulting map
     * @return a Map containing the elements of this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     * @throws E3 if an exception occurs during value extraction
     * @throws E4 if an exception occurs during merging
     * @throws IllegalStateException if the sequence is already closed.
     * @see Fn.Fnn#throwingMerger()
     * @see Fn.Fnn#replacingMerger()
     * @see Fn.Fnn#ignoringMerger()
     */
    @TerminalOp
    public <K, V, M extends Map<K, V>, E2 extends Exception, E3 extends Exception, E4 extends Exception> M toMap(
            final Throwables.Function<? super T, ? extends K, E2> keyMapper, final Throwables.Function<? super T, ? extends V, E3> valueMapper,
            final Throwables.BinaryOperator<V, E4> mergeFunction, final Supplier<? extends M> mapFactory) throws IllegalStateException, E, E2, E3, E4 {
        assertNotClosed();
        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(mergeFunction, cs.mergeFunction);
        checkArgNotNull(mapFactory, cs.mapFactory);

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
     * Converts the elements in this sequence to an ImmutableMap using the provided key and value extractors.
     *
     * @param <K> the type of keys in the resulting map
     * @param <V> the type of values in the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param <E3> the type of exception that may be thrown by the value extractor
     * @param keyMapper the function to extract keys from the elements
     * @param valueMapper the function to extract values from the elements
     * @return an ImmutableMap containing the elements of this sequence
     * @throws IllegalStateException if the sequence is already closed or there are duplicated keys
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     * @throws E3 if an exception occurs during value extraction
     * @see Fn.Fnn#throwingMerger()
     * @see Fn.Fnn#replacingMerger()
     * @see Fn.Fnn#ignoringMerger()
     */
    @TerminalOp
    public <K, V, E2 extends Exception, E3 extends Exception> ImmutableMap<K, V> toImmutableMap(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Throwables.Function<? super T, ? extends V, E3> valueMapper) throws IllegalStateException, E, E2, E3 {
        return ImmutableMap.wrap(toMap(keyMapper, valueMapper));
    }

    /**
     * Converts the elements in this sequence to an ImmutableMap using the provided key and value extractors.
     *
     * @param <K> the type of keys in the resulting map
     * @param <V> the type of values in the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param <E3> the type of exception that may be thrown by the value extractor
     * @param <E4> the type of exception that may be thrown by the merge function
     * @param keyMapper the function to extract keys from the elements
     * @param valueMapper the function to extract values from the elements
     * @param mergeFunction the function to resolve collisions between values associated with the same key
     * @return an ImmutableMap containing the elements of this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     * @throws E3 if an exception occurs during value extraction
     * @throws E4 if an exception occurs during merging
     * @see Fn.Fnn#throwingMerger()
     * @see Fn.Fnn#replacingMerger()
     * @see Fn.Fnn#ignoringMerger()
     */
    @TerminalOp
    public <K, V, E2 extends Exception, E3 extends Exception, E4 extends Exception> ImmutableMap<K, V> toImmutableMap(
            final Throwables.Function<? super T, ? extends K, E2> keyMapper, final Throwables.Function<? super T, ? extends V, E3> valueMapper,
            final Throwables.BinaryOperator<V, E4> mergeFunction) throws IllegalStateException, E, E2, E3, E4 {
        return ImmutableMap.wrap(toMap(keyMapper, valueMapper, mergeFunction));
    }

    /**
     * Groups the elements in this sequence by the keys extracted using the provided key extractor function.
     *
     * @param <K> the type of keys in the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param keyMapper the function to extract keys from the elements
     * @return a Map where each key is associated with a list of elements that share that key
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     * @see #groupTo(Throwables.Function, Supplier)
     */
    @TerminalOp
    public <K, E2 extends Exception> Map<K, List<T>> groupTo(final Throwables.Function<? super T, ? extends K, E2> keyMapper)
            throws IllegalStateException, E, E2 {
        return groupTo(keyMapper, Suppliers.ofMap());
    }

    /**
     * Groups the elements in this sequence by the keys extracted using the provided key extractor function.
     *
     * @param <K> the type of keys in the resulting map
     * @param <M> the type of the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param keyMapper the function to extract keys from the elements
     * @param mapFactory the supplier to create the resulting map
     * @return a Map where each key is associated with a list of elements that share that key
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     * @see Collectors#groupingBy(Function, Supplier)
     */
    @TerminalOp
    public <K, M extends Map<K, List<T>>, E2 extends Exception> M groupTo(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Supplier<? extends M> mapFactory) throws IllegalStateException, E, E2 {
        final Throwables.Function<T, T, E> valueMapper = Fnn.identity();

        return groupTo(keyMapper, valueMapper, mapFactory);
    }

    /**
     * Groups the elements in this sequence by the keys extracted using the provided key extractor function.
     *
     * @param <K> the type of keys in the resulting map
     * @param <V> the type of values in the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param <E3> the type of exception that may be thrown by the value extractor
     * @param keyMapper the function to extract keys from the elements
     * @param valueMapper the function to extract values from the elements
     * @return a Map where each key is associated with a list of values that share that key
     * @throws IllegalStateException if the sequence is already
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     * @throws E3 if an exception occurs during value extraction
     * @see #groupTo(Throwables.Function, Throwables.Function, Supplier)
     */
    @TerminalOp
    public <K, V, E2 extends Exception, E3 extends Exception> Map<K, List<V>> groupTo(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Throwables.Function<? super T, ? extends V, E3> valueMapper) throws IllegalStateException, E, E2, E3 {
        return groupTo(keyMapper, valueMapper, Suppliers.ofMap());
    }

    /**
     * Groups the elements in this sequence by the keys extracted using the provided key extractor function.
     *
     * @param <K> the type of keys in the resulting map
     * @param <V> the type of values in the resulting map
     * @param <M> the type of the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param <E3> the type of exception that may be thrown by the value extractor
     * @param keyMapper the function to extract keys from the elements
     * @param valueMapper the function to extract values from the elements
     * @param mapFactory the supplier to create the resulting map
     * @return a Map where each key is associated with a list of values that share that key
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the key extractor or value extractor is null
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     * @throws E3 if an exception occurs during value extraction
     * @see #groupTo(Throwables.Function, Collector, Supplier)
     * @see Collectors#groupingBy(Function, Supplier)
     */
    @TerminalOp
    public <K, V, M extends Map<K, List<V>>, E2 extends Exception, E3 extends Exception> M groupTo(
            final Throwables.Function<? super T, ? extends K, E2> keyMapper, final Throwables.Function<? super T, ? extends V, E3> valueMapper,
            final Supplier<? extends M> mapFactory) throws IllegalStateException, IllegalArgumentException, E, E2, E3 {
        assertNotClosed();
        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(mapFactory, cs.mapFactory);

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
     * Groups the elements in this sequence by the keys extracted using the provided key extractor function
     * and collects the grouped elements using the provided downstream collector.
     *
     * @param <K> the type of keys in the resulting map
     * @param <D> the type of values in the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param keyMapper the function to extract keys from the elements
     * @param downstream the collector to collect the grouped elements
     * @return a Map where each key is associated with the result of the downstream collector
     * @throws IllegalStateException if the sequence is already closed.
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     */
    @TerminalOp
    public <K, D, E2 extends Exception> Map<K, D> groupTo(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Collector<? super T, ?, D> downstream) throws IllegalStateException, E, E2 {
        return groupTo(keyMapper, downstream, Suppliers.ofMap());
    }

    /**
     * Groups the elements in this sequence by the keys extracted using the provided key extractor function
     * and collects the grouped elements using the provided downstream collector.
     *
     * @param <K> the type of keys in the resulting map
     * @param <D> the type of values in the resulting map
     * @param <M> the type of the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param keyMapper the function to extract keys from the elements
     * @param downstream the collector to collect the grouped elements
     * @param mapFactory the supplier to create the resulting map
     * @return a Map where each key is associated with the result of the downstream collector
     * @throws IllegalStateException if the sequence is already closed.
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     */
    @TerminalOp
    public <K, D, M extends Map<K, D>, E2 extends Exception> M groupTo(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Collector<? super T, ?, D> downstream, final Supplier<? extends M> mapFactory) throws IllegalStateException, E, E2 {
        return groupTo(keyMapper, Fnn.<T, E> identity(), downstream, mapFactory);
    }

    /**
     * Groups the elements in this sequence by the keys extracted using the provided key extractor function
     * and collects the grouped elements using the provided downstream collector.
     *
     * @param <K> the type of keys in the resulting map
     * @param <V> the type of values in the resulting map
     * @param <D> the type of values in the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param <E3> the type of exception that may be thrown by the value extractor
     * @param keyMapper the function to extract keys from the elements
     * @param valueMapper the function to extract values from the elements
     * @param downstream the collector to collect the grouped elements
     * @return a Map where each key is associated with the result of the downstream collector
     * @throws IllegalStateException if the sequence is already closed.
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     * @throws E3 if an exception occurs during value extraction
     */
    @TerminalOp
    public <K, V, D, E2 extends Exception, E3 extends Exception> Map<K, D> groupTo(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Throwables.Function<? super T, ? extends V, E3> valueMapper, final Collector<? super V, ?, D> downstream)
            throws IllegalStateException, E, E2, E3 {
        return groupTo(keyMapper, valueMapper, downstream, Suppliers.ofMap());
    }

    /**
     * Groups the elements in this sequence by the keys extracted using the provided key extractor function
     * and collects the grouped elements using the provided downstream collector.
     *
     * @param <K> the type of keys in the resulting map
     * @param <V> the type of values in the resulting map
     * @param <D> the type of values in the resulting map
     * @param <M> the type of the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param <E3> the type of exception that may be thrown by the value extractor
     * @param keyMapper the function to extract keys from the elements
     * @param valueMapper the function to extract values from the elements
     * @param downstream the collector to collect the grouped elements
     * @param mapFactory the supplier to create the resulting map
     * @return a Map where each key is associated with the result of the downstream collector
     * @throws IllegalStateException if the sequence is already closed.
     * @throws IllegalArgumentException if the key extractor, value extractor, downstream collector or map supplier is null
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     * @throws E3 if an exception occurs during value extraction
     */
    @TerminalOp
    public <K, V, D, M extends Map<K, D>, E2 extends Exception, E3 extends Exception> M groupTo(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Throwables.Function<? super T, ? extends V, E3> valueMapper, final Collector<? super V, ?, D> downstream,
            final Supplier<? extends M> mapFactory) throws IllegalStateException, IllegalArgumentException, E, E2, E3 {
        assertNotClosed();
        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(downstream, cs.downstream);
        checkArgNotNull(mapFactory, cs.mapFactory);

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

            for (final Map.Entry<K, D> entry : result.entrySet()) {
                entry.setValue(downstreamFinisher.apply(entry.getValue()));
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     * Partitions the elements in this sequence into a map of two lists based on the provided predicate.
     * The map will always have two entries: one for elements that match the predicate and one for elements that do not.
     *
     * @param <E2> the type of exception that may be thrown by the predicate
     * @param predicate the predicate to partition the elements
     * @return a Map with two entries: {@code true} for elements that match the predicate, and {@code false} for elements that do not
     * @throws IllegalStateException if the sequence is already closed.
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during predicate evaluation
     */
    @TerminalOp
    public <E2 extends Exception> Map<Boolean, List<T>> partitionTo(final Throwables.Predicate<? super T, E2> predicate) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return partitionTo(predicate, Collectors.toList());
    }

    /**
     * Partitions the elements in this sequence into a map of two collected results by the downstream collector based on the provided predicate.
     * The map will always have two entries: one for elements that match the predicate and one for elements that do not.
     *
     * @param <D> the type of the downstream result
     * @param <E2> the type of exception that may be thrown by the predicate
     * @param predicate the predicate to partition the elements
     * @return a Map with two entries: {@code true} for collected result that match the predicate, and {@code false} for collected result that do not
     * @throws IllegalStateException if the sequence is already closed.
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during predicate evaluation
     */
    @TerminalOp
    public <D, E2 extends Exception> Map<Boolean, D> partitionTo(final Throwables.Predicate<? super T, E2> predicate,
            final Collector<? super T, ?, D> downstream) throws IllegalStateException, E, E2 {
        assertNotClosed();

        final Function<Object, D> downstreamFinisher = (Function<Object, D>) downstream.finisher();

        final Throwables.Function<T, Boolean, E2> keyMapper = predicate::test;
        final Supplier<Map<Boolean, D>> mapFactory = () -> N.newHashMap(2);
        final Map<Boolean, D> map = groupTo(keyMapper, downstream, mapFactory);

        if (!map.containsKey(Boolean.TRUE)) {
            map.put(Boolean.TRUE, downstreamFinisher.apply(downstream.supplier().get()));
        }

        if (!map.containsKey(Boolean.FALSE)) {
            map.put(Boolean.FALSE, downstreamFinisher.apply(downstream.supplier().get()));
        }

        return map;
    }

    /**
     * Converts the elements in this sequence into a ListMultimap based on the provided key extractor function.
     * The keys in the resulting multimap are generated by applying the key extractor function to the elements.
     *
     * @param <K> the type of keys in the resulting multimap
     * @param <E2> the type of exception that may be thrown by the key extractor function
     * @param keyMapper the function to extract keys from the elements
     * @return a ListMultimap where the keys are generated by the key extractor function and the values are the elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     */
    @TerminalOp
    public <K, E2 extends Exception> ListMultimap<K, T> toMultimap(final Throwables.Function<? super T, ? extends K, E2> keyMapper)
            throws IllegalStateException, E, E2 {
        return toMultimap(keyMapper, Suppliers.ofListMultimap());
    }

    /**
     * Converts the elements in this sequence into a multimap based on the provided key extractor function.
     * The keys in the resulting multimap are generated by applying the key extractor function to the elements.
     *
     * @param <K> the type of keys in the resulting multimap
     * @param <M> the type of the resulting multimap
     * @param <E2> the type of exception that may be thrown by the key extractor function
     * @param keyMapper the function to extract keys from the elements
     * @param mapFactory the supplier to create the resulting multimap
     * @return a multimap where the keys are generated by the key extractor function and the values are the elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     */
    @TerminalOp
    public <K, V extends Collection<T>, M extends Multimap<K, T, V>, E2 extends Exception> M toMultimap(
            final Throwables.Function<? super T, ? extends K, E2> keyMapper, final Supplier<? extends M> mapFactory) throws IllegalStateException, E, E2 {
        final Throwables.Function<T, T, E> valueMapper = Fnn.identity();

        return toMultimap(keyMapper, valueMapper, mapFactory);
    }

    /**
     * Converts the elements in this sequence into a ListMultimap based on the provided key and value extractor functions.
     * The keys in the resulting multimap are generated by applying the key extractor function to the elements,
     * and the values are generated by applying the value extractor function to the elements.
     *
     * @param <K> the type of keys in the resulting multimap
     * @param <V> the type of values in the resulting multimap
     * @param <E2> the type of exception that may be thrown by the key extractor function
     * @param <E3> the type of exception that may be thrown by the value extractor function
     * @param keyMapper the function to extract keys from the elements
     * @param valueMapper the function to extract values from the elements
     * @return a ListMultimap where the keys are generated by the key extractor function and the values are generated by the value extractor function
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     * @throws E3 if an exception occurs during value extraction
     */
    @TerminalOp
    public <K, V, E2 extends Exception, E3 extends Exception> ListMultimap<K, V> toMultimap(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Throwables.Function<? super T, ? extends V, E3> valueMapper) throws IllegalStateException, E, E2, E3 {
        return toMultimap(keyMapper, valueMapper, Suppliers.ofListMultimap());
    }

    /**
     * Converts the elements in this sequence into a multimap based on the provided key and value extractor functions.
     * The keys in the resulting multimap are generated by applying the key extractor function to the elements,
     * and the values are generated by applying the value extractor function to the elements.
     *
     * @param <K> the type of keys in the resulting multimap
     * @param <V> the type of values in the resulting multimap
     * @param <M> the type of the resulting multimap
     * @param <E2> the type of exception that may be thrown by the key extractor function
     * @param <E3> the type of exception that may be thrown by the value extractor function
     * @param keyMapper the function to extract keys from the elements
     * @param valueMapper the function to extract values from the elements
     * @param mapFactory the supplier to create the resulting multimap
     * @return a multimap where the keys are generated by the key extractor function and the values are generated by the value extractor function
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the key extractor, value extractor or map factory is null
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     * @throws E3 if an exception occurs during value extraction
     */
    @TerminalOp
    public <K, V, C extends Collection<V>, M extends Multimap<K, V, C>, E2 extends Exception, E3 extends Exception> M toMultimap(
            final Throwables.Function<? super T, ? extends K, E2> keyMapper, final Throwables.Function<? super T, ? extends V, E3> valueMapper,
            final Supplier<? extends M> mapFactory) throws IllegalStateException, IllegalArgumentException, E, E2, E3 {
        assertNotClosed();
        checkArgNotNull(keyMapper, cs.keyMapper);
        checkArgNotNull(valueMapper, cs.valueMapper);
        checkArgNotNull(mapFactory, cs.mapFactory);

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
     * Converts the elements in this sequence into a Multiset.
     * The resulting multiset will contain all the elements from this sequence.
     *
     * @return a Multiset containing all the elements from this sequence
     * @throws IllegalStateException if the sequence is already closed.
     * @throws E if an exception occurs during iteration
     */
    @TerminalOp
    public Multiset<T> toMultiset() throws IllegalStateException, E {
        return toMultiset(Suppliers.ofMultiset());
    }

    /**
     * Converts the elements in this sequence into a Multiset.
     * The resulting multiset will contain all the elements from this sequence.
     *
     * @param supplier the supplier to create the resulting multiset
     * @return a Multiset containing all the elements from this sequence
     * @throws IllegalStateException if the sequence is already closed.
     * @throws IllegalArgumentException if the specified supplier is null
     * @throws E if an exception occurs during iteration
     */
    @TerminalOp
    public Multiset<T> toMultiset(final Supplier<? extends Multiset<T>> supplier) throws IllegalStateException, IllegalArgumentException, E { //NOSONAR
        assertNotClosed();
        checkArgNotNull(supplier, cs.supplier);

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
     * Converts the elements in this sequence into a DataSet.
     * The first row will be used as column names if its type is array or list,
     * or obtain the column names from the first row if its type is bean or map.
     *
     * @return a DataSet containing the elements from this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during the conversion
     * @see N#newDataSet(Collection)
     */
    @Beta
    @TerminalOp
    public DataSet toDataSet() throws IllegalStateException, E {
        return N.newDataSet(toList());
    }

    /**
     * Converts the elements in this sequence into a DataSet with the specified column names.
     * If the specified <i>columnNames</i> is <i>null</i> or empty, the first row will be used as column names if its type is array or list,
     * or obtain the column names from the first row if its type is bean or map.
     *
     * @param columnNames the list of column names to be used for the <i>DataSet</i>
     * @return a <i>DataSet</i> containing the elements from this sequence with the specified column names
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during the conversion
     * @see N#newDataSet(Collection, Collection)
     */
    @TerminalOp
    public DataSet toDataSet(final List<String> columnNames) throws IllegalStateException, E {
        return N.newDataSet(columnNames, toList());
    }

    /**
     * Sums the integer values extracted from the elements in this sequence using the provided function.
     *
     * @param <E2> the type of exception that the function may throw
     * @param func the function to extract integer values from the elements
     * @return the sum of the integer values
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during the operation
     * @throws E2 if the provided function throws an exception
     */
    @TerminalOp
    public <E2 extends Exception> long sumInt(final Throwables.ToIntFunction<? super T, E2> func) throws IllegalStateException, E, E2 {
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
     * Sums the long values extracted from the elements in this sequence using the provided function.
     *
     * @param <E2> the type of exception that the function may throw
     * @param func the function to extract long values from the elements
     * @return the sum of the long values
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during the operation
     * @throws E2 if the provided function throws an exception
     */
    @TerminalOp
    public <E2 extends Exception> long sumLong(final Throwables.ToLongFunction<? super T, E2> func) throws IllegalStateException, E, E2 {
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
     * Sums the double values extracted from the elements in this sequence using the provided function.
     *
     * @param <E2> the type of exception that the function may throw
     * @param func the function to extract double values from the elements
     * @return the sum of the double values
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during the operation
     * @throws E2 if the provided function throws an exception
     */
    @TerminalOp
    public <E2 extends Exception> double sumDouble(final Throwables.ToDoubleFunction<? super T, E2> func) throws IllegalStateException, E, E2 {
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
     * Calculates the average of the integer values extracted from the elements in this sequence using the provided function.
     *
     * @param <E2> the type of exception that the function may throw
     * @param func the function to extract integer values from the elements
     * @return the average of the integer values
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during the operation
     * @throws E2 if the provided function throws an exception
     */
    @TerminalOp
    public <E2 extends Exception> OptionalDouble averageInt(final Throwables.ToIntFunction<? super T, E2> func) throws IllegalStateException, E, E2 {
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
     * Calculates the average of the long values extracted from the elements in this sequence using the provided function.
     *
     * @param <E2> the type of exception that the function may throw
     * @param func the function to extract long values from the elements
     * @return the average of the long values
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during the operation
     * @throws E2 if the provided function throws an exception
     */
    @TerminalOp
    public <E2 extends Exception> OptionalDouble averageLong(final Throwables.ToLongFunction<? super T, E2> func) throws IllegalStateException, E, E2 {
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
     * Calculates the average of the double values extracted from the elements in this sequence using the provided function.
     *
     * @param <E2> the type of exception that the function may throw
     * @param func the function to extract double values from the elements
     * @return the average of the double values
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during the operation
     * @throws E2 if the provided function throws an exception
     */
    @TerminalOp
    public <E2 extends Exception> OptionalDouble averageDouble(final Throwables.ToDoubleFunction<? super T, E2> func) throws IllegalStateException, E, E2 {
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
     * Performs a reduction on the elements of this sequence, using the provided accumulator function, and returns the reduced value.
     * The accumulator function takes two parameters: the current reduced value and the current sequence element.
     *
     * @param <E2> the type of exception that the accumulator function may throw
     * @param accumulator the function for combining the current reduced value and the current sequence element
     * @return an Optional containing the result of the reduction, or an empty Optional if the sequence is empty
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the accumulator function is null
     * @throws E if an exception occurs during the reduction
     * @throws E2 if the accumulator function throws an exception
     * @see Stream#reduce(BinaryOperator)
     */
    @TerminalOp
    public <E2 extends Exception> Optional<T> reduce(final Throwables.BinaryOperator<T, E2> accumulator)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();
        checkArgNotNull(accumulator, cs.accumulator);

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
     * Performs a reduction on the elements of this stream, using the provided accumulator function, and returns the reduced value.
     * The accumulator function takes two parameters: the current reduced value (or the initial value for the first element), and the current stream element.
     *
     * @param <U> the type of the result
     * @param <E2> the type of exception that the accumulator function may throw
     * @param identity the initial value of the reduction operation
     * @param accumulator the function for combining the current reduced value and the current sequence element
     * @return the result of the reduction
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the accumulator function is null
     * @throws E if an exception occurs during the reduction
     * @throws E2 if the accumulator function throws an exception
     * @see Stream#reduce(Object, BinaryOperator)
     */
    @TerminalOp
    public <U, E2 extends Exception> U reduce(final U identity, final Throwables.BiFunction<? super U, ? super T, U, E2> accumulator)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();
        checkArgNotNull(accumulator, cs.accumulator);

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

    //    /**
    //     *
    //     * @param <E2>
    //     * @param accumulator
    //     * @param conditionToBreak the input parameter is the return value of {@code accumulator}, not the element from this Stream.
    //     *        Returns {@code true} to break the loop if you don't want to continue the {@code action}.
    //     *        Iteration on this stream will also be stopped when this flag is set to {@code true}.
    //     * @return
    //     * @throws E
    //     * @throws E2
    //     */
    //    @Beta
    //    @TerminalOp
    //    public <E2 extends Exception> Optional<T> reduceUntil(final Throwables.BinaryOperator<T, E2> accumulator,
    //            final Throwables.Predicate<? super T, E2> conditionToBreak) throws E, E2 {
    //        assertNotClosed();
    //        checkArgNotNull(accumulator, cs.accumulator);
    //        checkArgNotNull(conditionToBreak, cs.conditionToBreak);
    //
    //        final MutableBoolean flagToBreak = MutableBoolean.of(false);
    //
    //        final Throwables.BinaryOperator<T, E2> newAccumulator = (t, u) -> {
    //            final T ret = accumulator.apply(t, u);
    //
    //            if (flagToBreak.isFalse() && conditionToBreak.test(ret)) {
    //                flagToBreak.setValue(true);
    //            }
    //
    //            return ret;
    //        };
    //
    //        return takeWhile(value -> flagToBreak.isFalse()).reduce(newAccumulator);
    //    }
    //
    //    /**
    //     *
    //     * @param <U>
    //     * @param <E2>
    //     * @param identity
    //     * @param accumulator
    //     * @param conditionToBreak the input parameter is the return value of {@code accumulator}, not the element from this Stream.
    //     *        Returns {@code true} to break the loop if you don't want to continue the {@code action}.
    //     *        Iteration on this stream will also be stopped when this flag is set to {@code true}.
    //     * @return
    //     * @throws IllegalStateException
    //     * @throws IllegalArgumentException
    //     * @throws E
    //     * @throws E2
    //     */
    //    @Beta
    //    @TerminalOp
    //    public <U, E2 extends Exception> U reduceUntil(final U identity, final Throwables.BiFunction<? super U, ? super T, U, E2> accumulator,
    //            final Throwables.Predicate<? super U, E2> conditionToBreak) throws IllegalStateException, IllegalArgumentException, E, E2 {
    //        assertNotClosed();
    //        checkArgNotNull(accumulator, cs.accumulator);
    //        checkArgNotNull(conditionToBreak, cs.conditionToBreak);
    //
    //        final MutableBoolean flagToBreak = MutableBoolean.of(false);
    //
    //        final Throwables.BiFunction<U, T, U, E2> newAccumulator = (u, t) -> {
    //            final U ret = accumulator.apply(u, t);
    //
    //            if (flagToBreak.isFalse() && conditionToBreak.test(ret)) {
    //                flagToBreak.setValue(true);
    //            }
    //
    //            return ret;
    //        };
    //
    //        return takeWhile(value -> flagToBreak.isFalse()).reduce(identity, newAccumulator);
    //    }

    /**
     * Collects the elements of this sequence into a result container provided by the supplier.
     *
     * @param <R> the type of the result container
     * @param <E2> the type of exception that the supplier may throw
     * @param <E3> the type of exception that the accumulator may throw
     * @param supplier a function that provides a new result container
     * @param accumulator a function that incorporates an element into the result container
     * @return the result container with all elements of this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified supplier or accumulator is null
     * @throws E if an exception occurs during the collection
     * @throws E2 if the supplier throws an exception
     * @throws E3 if the accumulator throws an exception
     */
    @TerminalOp
    public <R, E2 extends Exception, E3 extends Exception> R collect(final Throwables.Supplier<R, E2> supplier,
            final Throwables.BiConsumer<? super R, ? super T, E3> accumulator) throws IllegalStateException, IllegalArgumentException, E, E2, E3 {
        assertNotClosed();
        checkArgNotNull(supplier, cs.supplier);
        checkArgNotNull(accumulator, cs.accumulator);

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
     * Collects the elements of this sequence into a result container provided by the supplier,
     * then applies the finisher function to the result to obtain the final result.
     *
     * @param <R> the type of the result container
     * @param <RR> the type of the final result
     * @param <E2> the type of exception that the supplier may throw
     * @param <E3> the type of exception that the accumulator may throw
     * @param <E4> the type of exception that the finisher may throw
     * @param supplier a function that provides a new result container
     * @param accumulator a function that incorporates an element into the result container
     * @param finisher a function that transforms the result container into the final result
     * @return the final result
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified supplier, accumulator or finisher is null
     * @throws E if an exception occurs during the collection
     * @throws E2 if the supplier throws an exception
     * @throws E3 if the accumulator throws an exception
     * @throws E4 if the finisher throws an exception
     */
    @TerminalOp
    public <R, RR, E2 extends Exception, E3 extends Exception, E4 extends Exception> RR collect(final Throwables.Supplier<R, E2> supplier,
            final Throwables.BiConsumer<? super R, ? super T, E3> accumulator, final Throwables.Function<? super R, ? extends RR, E4> finisher)
            throws E, E2, E3, E4 {
        assertNotClosed();
        checkArgNotNull(supplier, cs.supplier);
        checkArgNotNull(accumulator, cs.accumulator);
        checkArgNotNull(finisher, cs.finisher);

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
     * Collects the elements of this sequence using the provided collector.
     *
     * @param <R> the type of the result
     * @param collector the collector used to accumulate the elements of this sequence
     * @return the result of the collection
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the collector is null
     * @throws E if an exception occurs during the collection
     */
    @TerminalOp
    public <R> R collect(final Collector<? super T, ?, R> collector) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();
        checkArgNotNull(collector, cs.collector);

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
     * Collects the elements of this sequence using the provided collector and then applies the given function to the result.
     *
     * @param <R> the type of the intermediate result
     * @param <RR> the type of the final result
     * @param <E2> the type of exception that the function may throw
     * @param collector the collector used to accumulate the elements of this sequence
     * @param func the function to apply to the result of the collection
     * @return the result of applying the function to the collected elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the collector or function is null
     * @throws E if an exception occurs during the collection
     * @throws E2 if the function throws an exception
     */
    @TerminalOp
    public <R, RR, E2 extends Exception> RR collectThenApply(final Collector<? super T, ?, R> collector,
            final Throwables.Function<? super R, ? extends RR, E2> func) throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();
        checkArgNotNull(collector, cs.collector);
        checkArgNotNull(func, cs.func);

        return func.apply(collect(collector));
    }

    /**
     * Collects the elements of this sequence using the provided collector and then applies the given consumer to the result.
     *
     * @param <R> the type of the result
     * @param <E2> the type of exception that the consumer may throw
     * @param collector the collector used to accumulate the elements of this sequence
     * @param consumer the consumer to apply to the result of the collection
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the collector or consumer is null
     * @throws E if an exception occurs during the collection
     * @throws E2 if the consumer throws an exception
     */
    @TerminalOp
    public <R, E2 extends Exception> void collectThenAccept(final Collector<? super T, ?, R> collector, final Throwables.Consumer<? super R, E2> consumer)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();
        checkArgNotNull(collector, cs.collector);
        checkArgNotNull(consumer, cs.consumer);

        consumer.accept(collect(collector));
    }

    /**
     * Joins the elements of this sequence into a single <i>String</i> with the specified delimiter.
     *
     * @param delimiter the delimiter to separate each element
     * @return a <i>String</i> containing the joined elements
     * @throws IllegalStateException if the sequence is already closed.
     * @throws E if an exception occurs during the operation
     */
    @TerminalOp
    public String join(final CharSequence delimiter) throws IllegalStateException, E {
        return join(delimiter, "", "");
    }

    /**
     * Joins the elements of this sequence into a single <i>String</i> with the specified delimiter, prefix, and suffix.
     *
     * @param delimiter the delimiter to separate each element
     * @param prefix the prefix to be added at the beginning of the resulting string
     * @param suffix the suffix to be added at the end of the resulting string
     * @return a <i>String</i> containing the joined elements with the specified delimiter, prefix, and suffix
     * @throws IllegalStateException if the sequence is already closed.
     * @throws E if an exception occurs during the operation
     */
    @TerminalOp
    public String join(final CharSequence delimiter, final CharSequence prefix, final CharSequence suffix) throws IllegalStateException, E {
        assertNotClosed();

        try {
            @SuppressWarnings("resource")
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
     * Joins the elements of this sequence into the provided <i>Joiner</i>.
     *
     * @param joiner the <i>Joiner</i> to append the elements to
     * @return the <i>Joiner</i> containing the joined elements
     * @throws IllegalStateException if the sequence is already closed.
     * @throws IllegalArgumentException if the provided <i>Joiner</i> is null
     * @throws E if an exception occurs during the operation
     */
    @TerminalOp
    public Joiner joinTo(final Joiner joiner) throws IllegalStateException, IllegalArgumentException, E {
        assertNotClosed();
        checkArgNotNull(joiner, cs.joiner);

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
     * Saves each element of this sequence to the specified file.
     * {@code N.stringOf(Object)} is used to convert each element to a string.
     * {@code UncheckedIOException} may be thrown in the terminal operation if an I/O error occurs.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param output the file to save each element to
     * @return this sequence
     * @throws IllegalStateException if the sequence is already closed.
     * @see #persist(File)
     * @see N#stringOf(Object)
     * @see #onEach(Throwables.Consumer)
     */
    @Beta
    @IntermediateOp
    public Seq<T, E> saveEach(final File output) throws IllegalStateException {
        return saveEach(N::stringOf, output);
    }

    /**
     * Saves each element of this sequence to the specified file using the provided function to convert elements to strings.
     * {@code UncheckedIOException} may be thrown in the terminal operation if an I/O error occurs.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param toLine the function to convert each element to a string
     * @param output the file to save each element to
     * @return this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @see #persist(Throwables.Function, File)
     * @see N#stringOf(Object)
     * @see #onEach(Throwables.Consumer)
     */
    @Beta
    @IntermediateOp
    public Seq<T, E> saveEach(final Throwables.Function<? super T, String, E> toLine, final File output) throws IllegalStateException {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = new Throwables.Iterator<>() {
            private final Throwables.Iterator<T, E> iter = iteratorEx();
            private Writer writer = null;
            private BufferedWriter bw = null;
            private boolean initialized = false;
            long cnt = 0;

            public boolean hasNext() throws E {
                return iter.hasNext();
            }

            public T next() throws E {
                final T next = iter.next();

                if (!initialized) {
                    init();
                }

                try {
                    bw.write(toLine.apply(next));
                    bw.write(IOUtil.LINE_SEPARATOR);
                    cnt++;

                    if (cnt % BATCH_SIZE_FOR_FLUSH == 0) {
                        bw.flush();
                    }
                } catch (final IOException e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }

                return next;
            }

            @Override
            protected void closeResource() {
                if (writer != null) {
                    try {
                        if (bw != null) {
                            try {
                                bw.flush();
                            } catch (final IOException e) {
                                throw new UncheckedIOException(e);
                            } finally {
                                Objectory.recycle(bw);
                            }
                        }
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

        return create(iter, sorted, cmp, mergeCloseHandlers(iter::close, closeHandlers, true)); //NOSONAR
    }

    /**
     * Saves each element of this sequence to the specified output stream using the provided function to convert elements to strings.
     * {@code UncheckedIOException} may be thrown in the terminal operation if an I/O error occurs.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param toLine the function to convert each element to a string
     * @param output the output stream to save each element to
     * @return this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @see #persist(Throwables.Function, OutputStream)
     * @see N#stringOf(Object)
     * @see #onEach(Throwables.Consumer)
     */
    @Beta
    @IntermediateOp
    public Seq<T, E> saveEach(final Throwables.Function<? super T, String, E> toLine, final OutputStream output) throws IllegalStateException {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = new Throwables.Iterator<>() {
            private final Throwables.Iterator<T, E> iter = iteratorEx();
            private BufferedWriter bw = null;
            private boolean initialized = false;
            long cnt = 0;

            public boolean hasNext() throws E {
                return iter.hasNext();
            }

            public T next() throws E {
                final T next = iter.next();

                if (!initialized) {
                    init();
                }

                try {
                    bw.write(toLine.apply(next));
                    bw.write(IOUtil.LINE_SEPARATOR);
                    cnt++;

                    if (cnt % BATCH_SIZE_FOR_FLUSH == 0) {
                        bw.flush();
                    }
                } catch (final IOException e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }

                return next;
            }

            @Override
            protected void closeResource() {
                if (bw != null) {
                    try {
                        bw.flush();
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    } finally {
                        Objectory.recycle(bw);
                    }
                }
            }

            private void init() {
                initialized = true;

                bw = Objectory.createBufferedWriter(output);
            }
        };

        return create(iter, sorted, cmp, mergeCloseHandlers(iter::close, closeHandlers, true)); //NOSONAR
    }

    /**
     * Saves each element of this sequence to the specified writer using the provided function to convert elements to strings.
     * {@code UncheckedIOException} may be thrown in the terminal operation if an I/O error occurs.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param toLine the function to convert each element to a string
     * @param output the writer to save each element to
     * @return this sequence
     * @throws IllegalStateException if the sequence is already
     * @see #persist(Throwables.Function, Writer)
     * @see N#stringOf(Object)
     * @see #onEach(Throwables.Consumer)
     */
    @Beta
    @IntermediateOp
    public Seq<T, E> saveEach(final Throwables.Function<? super T, String, E> toLine, final Writer output) throws IllegalStateException {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = new Throwables.Iterator<>() {
            private final Throwables.Iterator<T, E> iter = iteratorEx();
            private Writer bw = null;
            private boolean isBufferedWriter = false;
            private boolean initialized = false;
            long cnt = 0;

            public boolean hasNext() throws E {
                return iter.hasNext();
            }

            public T next() throws E {
                final T next = iter.next();

                if (!initialized) {
                    init();
                }

                try {
                    bw.write(toLine.apply(next));
                    bw.write(IOUtil.LINE_SEPARATOR);
                    cnt++;

                    if (cnt % BATCH_SIZE_FOR_FLUSH == 0) {
                        bw.flush();
                    }
                } catch (final IOException e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }

                return next;
            }

            @Override
            protected void closeResource() {
                if (!isBufferedWriter && (bw != null)) {
                    try {
                        bw.flush();
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    } finally {
                        Objectory.recycle((BufferedWriter) bw);
                    }
                }
            }

            private void init() {
                initialized = true;

                isBufferedWriter = IOUtil.isBufferedWriter(output);
                bw = isBufferedWriter ? output : Objectory.createBufferedWriter(output);
            }
        };

        return create(iter, sorted, cmp, mergeCloseHandlers(iter::close, closeHandlers, true)); //NOSONAR
    }

    /**
     * Saves each element of this sequence to the specified file using the provided function to write each element.
     * {@code UncheckedIOException} may be thrown in the terminal operation if an I/O error occurs.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param writeLine the function to write each element to the writer
     * @param output the file to save each element to
     * @return this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @see #persist(Throwables.BiConsumer, File)
     * @see N#stringOf(Object)
     * @see #onEach(Throwables.Consumer)
     */
    @Beta
    @IntermediateOp
    public Seq<T, E> saveEach(final Throwables.BiConsumer<? super T, Writer, IOException> writeLine, final File output) throws IllegalStateException {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = new Throwables.Iterator<>() {
            private final Throwables.Iterator<T, E> iter = iteratorEx();
            private Writer writer = null;
            private BufferedWriter bw = null;
            private boolean initialized = false;
            long cnt = 0;

            public boolean hasNext() throws E {
                return iter.hasNext();
            }

            public T next() throws E {
                final T next = iter.next();

                if (!initialized) {
                    init();
                }

                try {
                    writeLine.accept(next, bw);
                    bw.write(IOUtil.LINE_SEPARATOR);
                    cnt++;

                    if (cnt % BATCH_SIZE_FOR_FLUSH == 0) {
                        bw.flush();
                    }
                } catch (final IOException e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }

                return next;
            }

            @Override
            protected void closeResource() {
                if (writer != null) {
                    try {
                        if (bw != null) {
                            try {
                                bw.flush();
                            } catch (final IOException e) {
                                throw new UncheckedIOException(e);
                            } finally {
                                Objectory.recycle(bw);
                            }
                        }
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

        return create(iter, sorted, cmp, mergeCloseHandlers(iter::close, closeHandlers, true)); //NOSONAR
    }

    /**
     * Saves each element of this sequence to the specified writer using the provided function to write each element.
     * {@code UncheckedIOException} may be thrown in the terminal operation if an I/O error occurs.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param writeLine the function to write each element to the writer
     * @param output the writer to save each element to
     * @return this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @see #persist(Throwables.BiConsumer, Writer)
     * @see N#stringOf(Object)
     * @see #onEach(Throwables.Consumer)
     */
    @Beta
    @IntermediateOp
    public Seq<T, E> saveEach(final Throwables.BiConsumer<? super T, Writer, IOException> writeLine, final Writer output) throws IllegalStateException {
        assertNotClosed();

        final Throwables.Iterator<T, E> iter = new Throwables.Iterator<>() {
            private final Throwables.Iterator<T, E> iter = iteratorEx();
            private Writer bw = null;
            private boolean isBufferedWriter = false;
            private boolean initialized = false;
            long cnt = 0;

            public boolean hasNext() throws E {
                return iter.hasNext();
            }

            public T next() throws E {
                final T next = iter.next();

                if (!initialized) {
                    init();
                }

                try {
                    writeLine.accept(next, bw);
                    bw.write(IOUtil.LINE_SEPARATOR);
                    cnt++;

                    if (cnt % BATCH_SIZE_FOR_FLUSH == 0) {
                        bw.flush();
                    }
                } catch (final IOException e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }

                return next;
            }

            @Override
            protected void closeResource() {
                if (!isBufferedWriter && (bw != null)) {
                    try {
                        bw.flush();
                    } catch (final IOException e) {
                        throw new UncheckedIOException(e);
                    } finally {
                        Objectory.recycle((BufferedWriter) bw);
                    }
                }
            }

            private void init() {
                initialized = true;

                isBufferedWriter = IOUtil.isBufferedWriter(output);
                bw = isBufferedWriter ? output : Objectory.createBufferedWriter(output);
            }
        };

        return create(iter, sorted, cmp, mergeCloseHandlers(iter::close, closeHandlers, true)); //NOSONAR
    }

    /**
     * Saves each element of this sequence to the specified prepared statement using the provided statement setter.
     * {@code UncheckedSQLException} may be thrown in the terminal operation if an SQL error happens.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param stmt the prepared statement used to save each element
     * @param stmtSetter the function to set each element to the prepared statement
     * @return this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified prepared statement is null
     * @see #persist(PreparedStatement, int, long, Throwables.BiConsumer)
     * @see #onEach(Throwables.Consumer)
     */
    @Beta
    @IntermediateOp
    public Seq<T, E> saveEach(final PreparedStatement stmt, final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter)
            throws IllegalStateException, IllegalArgumentException {
        return saveEach(stmt, 0, 0, stmtSetter);
    }

    /**
     * Saves each element of this sequence to the specified prepared statement using the provided statement setter.
     * {@code UncheckedSQLException} may be thrown in the terminal operation if an SQL error happens.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param stmt the prepared statement used to save each element
     * @param batchSize the number of elements to include in each batch. If the batch size is less than 2, batch update won't be used.
     * @param batchIntervalInMillis the interval in milliseconds between each batch
     * @param stmtSetter the function to set each element to the prepared statement
     * @return this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified prepared statement is {@code null}, or {@code batchSize} or {@code batchIntervalInMillis} is negative
     * @see #persist(PreparedStatement, int, long, Throwables.BiConsumer)
     * @see #onEach(Throwables.Consumer)
     */
    @Beta
    @IntermediateOp
    public Seq<T, E> saveEach(final PreparedStatement stmt, final int batchSize, final long batchIntervalInMillis,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(stmt, cs.PreparedStatement);
        checkArgNotNegative(batchSize, cs.batchSize);
        checkArgNotNegative(batchIntervalInMillis, cs.batchIntervalInMillis);

        final Throwables.Iterator<T, E> iter = new Throwables.Iterator<>() {
            private final Throwables.Iterator<T, E> iter = iteratorEx();
            private final boolean isBatchUsed = batchSize > 1;
            long cnt = 0;

            public boolean hasNext() throws E {
                return iter.hasNext();
            }

            public T next() throws E {
                final T next = iter.next();

                try {
                    stmtSetter.accept(next, stmt);

                    if (isBatchUsed) {
                        cnt++;
                        stmt.addBatch();

                        if (cnt % batchSize == 0) {
                            DataSourceUtil.executeBatch(stmt);

                            if (batchIntervalInMillis > 0) {
                                N.sleepUninterruptibly(batchIntervalInMillis);
                            }
                        }
                    } else {
                        stmt.execute();
                    }
                } catch (final SQLException e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }

                return next;
            }

            @Override
            protected void closeResource() {
                if (isBatchUsed && (cnt % batchSize) > 0) {
                    try {
                        DataSourceUtil.executeBatch(stmt);
                    } catch (final SQLException e) {
                        throw ExceptionUtil.toRuntimeException(e, true);
                    }
                }
            }
        };

        return create(iter, sorted, cmp, mergeCloseHandlers(iter::close, closeHandlers, true)); //NOSONAR
    }

    /**
     * Saves each element of this sequence to the specified connection using the provided insert SQL and statement setter.
     * {@code UncheckedSQLException} may be thrown in the terminal operation if an SQL error happens.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param conn the connection used to save each element
     * @param insertSQL the SQL insert script used to prepare the statement
     * @param stmtSetter the function to set each element to the prepared statement
     * @return this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified Connection or insert script is null
     * @see #persist(Connection, String, int, long, Throwables.BiConsumer)
     * @see #onEach(Throwables.Consumer)
     */
    @Beta
    @IntermediateOp
    public Seq<T, E> saveEach(final Connection conn, final String insertSQL,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) throws IllegalStateException, IllegalArgumentException {
        return saveEach(conn, insertSQL, 0, 0, stmtSetter);
    }

    /**
     * Saves each element of this sequence to the specified connection using the provided insert SQL and statement setter.
     * {@code UncheckedSQLException} may be thrown in the terminal operation if an SQL error happens.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param conn the connection used to save each element
     * @param insertSQL the SQL insert script used to prepare the statement
     * @param batchSize the number of elements to include in each batch. If the batch size is less than 2, batch update won't be used.
     * @param batchIntervalInMillis the interval in milliseconds between each batch
     * @param stmtSetter the function to set each element to the prepared statement
     * @return this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified Connection or insert script is {@code null}, or {@code batchSize} or {@code batchIntervalInMillis} is negative
     * @see #persist(Connection, String, int, long, Throwables.BiConsumer)
     * @see #onEach(Throwables.Consumer)
     */
    @Beta
    @IntermediateOp
    public Seq<T, E> saveEach(final Connection conn, final String insertSQL, final int batchSize, final long batchIntervalInMillis,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(conn, cs.Connection);
        checkArgNotNull(insertSQL, cs.insertSQL);
        checkArgNotNegative(batchSize, cs.batchSize);
        checkArgNotNegative(batchIntervalInMillis, cs.batchIntervalInMillis);

        final Throwables.Iterator<T, E> iter = new Throwables.Iterator<>() {
            private final Throwables.Iterator<T, E> iter = iteratorEx();
            private PreparedStatement stmt = null;
            private boolean initialized = false;
            private final boolean isBatchUsed = batchSize > 1;
            long cnt = 0;

            public boolean hasNext() throws E {
                return iter.hasNext();
            }

            public T next() throws E {
                final T next = iter.next();

                if (!initialized) {
                    init();
                }

                try {
                    stmtSetter.accept(next, stmt);

                    if (isBatchUsed) {
                        cnt++;
                        stmt.addBatch();

                        if (cnt % batchSize == 0) {
                            DataSourceUtil.executeBatch(stmt);

                            if (batchIntervalInMillis > 0) {
                                N.sleepUninterruptibly(batchIntervalInMillis);
                            }
                        }
                    } else {
                        stmt.execute();
                    }
                } catch (final SQLException e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }

                return next;
            }

            @Override
            protected void closeResource() {
                if (stmt != null) {
                    try {
                        if (isBatchUsed && (cnt % batchSize) > 0) {
                            DataSourceUtil.executeBatch(stmt);
                        }
                    } catch (final SQLException e) {
                        throw ExceptionUtil.toRuntimeException(e, true);
                    } finally {
                        DataSourceUtil.closeQuietly(stmt);
                    }
                }
            }

            private void init() {
                initialized = true;

                try {
                    stmt = conn.prepareStatement(insertSQL);
                } catch (final SQLException e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            }
        };

        return create(iter, sorted, cmp, mergeCloseHandlers(iter::close, closeHandlers, true)); //NOSONAR
    }

    /**
     * Saves each element of this sequence to the specified data source using the provided SQL insert statement.
     * {@code UncheckedSQLException} may be thrown in the terminal operation if an SQL error happens.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param ds the data source used to save each element
     * @param insertSQL the SQL insert script used to prepare the statement
     * @param stmtSetter the function to set each element to the prepared statement
     * @return this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified DataSource or insert script is null
     * @see #persist(javax.sql.DataSource, String, int, long, Throwables.BiConsumer)
     * @see #onEach(Throwables.Consumer)
     */
    @Beta
    @IntermediateOp
    public Seq<T, E> saveEach(final javax.sql.DataSource ds, final String insertSQL,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) throws IllegalStateException, IllegalArgumentException {
        return saveEach(ds, insertSQL, 0, 0, stmtSetter);
    }

    /**
     * Saves each element of this sequence to the specified data source using the provided SQL insert statement.
     * {@code UncheckedSQLException} may be thrown in the terminal operation if an SQL error happens.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param ds the data source used to save each element
     * @param insertSQL the SQL insert script used to prepare the statement
     * @param batchSize the number of elements to include in each batch. If the batch size is less than 2, batch update won't be used.
     * @param batchIntervalInMillis the interval in milliseconds between each batch
     * @param stmtSetter the function to set each element to the prepared statement
     * @return this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified DataSource or insert script is {@code null}, or {@code batchSize} or {@code batchIntervalInMillis} is negative
     * @see #persist(javax.sql.DataSource, String, int, long, Throwables.BiConsumer)
     * @see #onEach(Throwables.Consumer)
     */
    @Beta
    @IntermediateOp
    public Seq<T, E> saveEach(final javax.sql.DataSource ds, final String insertSQL, final int batchSize, final long batchIntervalInMillis,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(ds, cs.DataSource);
        checkArgNotNull(insertSQL, cs.insertSQL);
        checkArgNotNegative(batchSize, cs.batchSize);
        checkArgNotNegative(batchIntervalInMillis, cs.batchIntervalInMillis);

        final Throwables.Iterator<T, E> iter = new Throwables.Iterator<>() {
            private final Throwables.Iterator<T, E> iter = iteratorEx();
            private Connection conn = null;
            private PreparedStatement stmt = null;
            private boolean initialized = false;
            private final boolean isBatchUsed = batchSize > 1;
            long cnt = 0;

            public boolean hasNext() throws E {
                return iter.hasNext();
            }

            public T next() throws E {
                final T next = iter.next();

                if (!initialized) {
                    init();
                }

                try {
                    stmtSetter.accept(next, stmt);

                    if (isBatchUsed) {
                        cnt++;
                        stmt.addBatch();

                        if (cnt % batchSize == 0) {
                            DataSourceUtil.executeBatch(stmt);

                            if (batchIntervalInMillis > 0) {
                                N.sleepUninterruptibly(batchIntervalInMillis);
                            }
                        }
                    } else {
                        stmt.execute();
                    }
                } catch (final SQLException e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }

                return next;
            }

            @Override
            protected void closeResource() {
                try {
                    if (stmt != null) {
                        try {
                            if (isBatchUsed && (cnt % batchSize) > 0) {
                                DataSourceUtil.executeBatch(stmt);
                            }
                        } catch (final SQLException e) {
                            throw ExceptionUtil.toRuntimeException(e, true);
                        } finally {
                            DataSourceUtil.closeQuietly(stmt);
                        }
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
                } catch (final SQLException e) {
                    try {
                        if (stmt != null) {
                            DataSourceUtil.closeQuietly(stmt);
                        }
                    } finally {
                        DataSourceUtil.releaseConnection(conn, ds);
                    }

                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            }
        };

        return create(iter, sorted, cmp, mergeCloseHandlers(iter::close, closeHandlers, true)); //NOSONAR
    }

    /**
     * Persists the sequence to the specified file.
     * {@code N.stringOf(Object)} is used to convert each element to a string.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param output the file to persist the sequence to
     * @return the number of elements persisted
     * @throws IllegalStateException if the sequence is already closed
     * @throws IOException if an I/O error occurs
     * @throws E if an exception occurs during the operation
     * @see N#stringOf(Object)
     */
    @TerminalOp
    public long persist(final File output) throws IllegalStateException, IOException, E {
        return persist(N::stringOf, output);
    }

    /**
     * Persists the sequence to the specified file with a header and tail.
     * {@code N.stringOf(Object)} is used to convert each element to a string.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param headerLine the header line to be written at the beginning of the file
     * @param tailLine the tail line to be written at the end of the file
     * @param output the file to persist the sequence to
     * @return the number of elements persisted
     * @throws IllegalStateException if the sequence is already closed
     * @throws IOException if an I/O error occurs
     * @throws E if an exception occurs during the operation
     * @see N#stringOf(Object)
     */
    @TerminalOp
    public long persist(final String headerLine, final String tailLine, final File output) throws IllegalStateException, IOException, E {
        return persist(headerLine, tailLine, N::stringOf, output);
    }

    /**
     * Persists the sequence to the specified output stream using the provided function to convert elements to strings.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param toLine the function to convert each element to a string
     * @param output the output stream to persist the sequence to
     * @return the number of elements persisted
     * @throws IllegalStateException if the sequence is already closed
     * @throws IOException if an I/O error occurs
     * @throws E if an exception occurs during the operation
     * @see N#stringOf(Object)
     */
    @TerminalOp
    public long persist(final Throwables.Function<? super T, String, E> toLine, final File output) throws IllegalStateException, IOException, E {
        return persist(null, null, toLine, output);
    }

    /**
     * Persists the sequence to the specified output stream with a header and tail using the provided function to convert elements to strings.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param header the header line to be written at the beginning of the file
     * @param tail the tail line to be written at the end of the file
     * @param toLine the function to convert each element to a string
     * @param output the output stream to persist the sequence to
     * @return the number of elements persisted
     * @throws IllegalStateException if the sequence is already closed
     * @throws IOException if an I/O error occurs
     * @throws E if an exception occurs during the operation
     * @see N#stringOf(Object)
     */
    @TerminalOp
    public long persist(final String header, final String tail, final Throwables.Function<? super T, String, E> toLine, final File output)
            throws IllegalStateException, IOException, E {
        assertNotClosed();

        final Writer writer = IOUtil.newFileWriter(output);

        try {
            return persist(header, tail, toLine, writer);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     * Persists the sequence to the specified output stream using the provided function to convert elements to strings.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param toLine the function to convert each element to a string
     * @param output the output stream to persist the sequence to
     * @return the number of elements persisted
     * @throws IllegalStateException if the sequence is already closed
     * @throws IOException if an I/O error occurs
     * @throws E if an exception occurs during the operation
     * @see N#stringOf(Object)
     */
    @TerminalOp
    public long persist(final Throwables.Function<? super T, String, E> toLine, final OutputStream output) throws IllegalStateException, IOException, E {
        assertNotClosed();

        final BufferedWriter bw = Objectory.createBufferedWriter(output);

        try {
            return persist(toLine, bw);
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     * Persists the sequence to the specified output stream with a header and tail using the provided function to convert elements to strings.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param header the header line to be written at the beginning of the file
     * @param tail the tail line to be written at the end of the file
     * @param toLine the function to convert each element to a string
     * @param output the output stream to persist the sequence to
     * @return the number of elements persisted
     * @throws IllegalStateException if the sequence is already closed
     * @throws IOException if an I/O error occurs
     * @throws E if an exception occurs during the operation
     * @see N#stringOf(Object)
     */
    @TerminalOp
    public long persist(final String header, final String tail, final Throwables.Function<? super T, String, E> toLine, final OutputStream output)
            throws IllegalStateException, IOException, E {
        assertNotClosed();

        final BufferedWriter bw = Objectory.createBufferedWriter(output);

        try {
            return persist(header, tail, toLine, bw);
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     * Persists the sequence to the specified writer using the provided function to convert elements to strings.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param toLine the function to convert each element to a string
     * @param output the writer to persist the sequence to
     * @return the number of elements persisted
     * @throws IllegalStateException if the sequence is already closed
     * @throws IOException if an I/O error occurs
     * @throws E if an exception occurs during the operation
     * @see N#stringOf(Object)
     */
    @TerminalOp
    public long persist(final Throwables.Function<? super T, String, E> toLine, final Writer output) throws IllegalStateException, IOException, E {
        assertNotClosed();

        return persist(null, null, toLine, output);
    }

    /**
     * Persists the sequence to the specified writer with a header and tail using the provided function to convert elements to strings.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param header the header line to be written at the beginning of the file
     * @param tail the tail line to be written at the end of the file
     * @param toLine the function to convert each element to a string
     * @param output the writer to persist the sequence to
     * @return the number of elements persisted
     * @throws IllegalStateException if the sequence is already closed
     * @throws IOException if an I/O error occurs
     * @throws E if an exception occurs during the operation
     * @see N#stringOf(Object)
     */
    @TerminalOp
    public long persist(final String header, final String tail, final Throwables.Function<? super T, String, E> toLine, final Writer output)
            throws IllegalStateException, IOException, E {
        assertNotClosed();

        try {
            final boolean isBufferedWriter = IOUtil.isBufferedWriter(output);
            final Writer bw = isBufferedWriter ? output : Objectory.createBufferedWriter(output); //NOSONAR
            long cnt = 0;

            try {
                @SuppressWarnings("resource")
                final Throwables.Iterator<T, E> iter = iteratorEx();

                if (header != null) {
                    bw.write(header);
                    bw.write(IOUtil.LINE_SEPARATOR);
                }

                while (iter.hasNext()) {
                    bw.write(toLine.apply(iter.next()));
                    bw.write(IOUtil.LINE_SEPARATOR);
                    cnt++;

                    if (cnt % BATCH_SIZE_FOR_FLUSH == 0) {
                        bw.flush();
                    }
                }

                if (tail != null) {
                    bw.write(tail);
                    bw.write(IOUtil.LINE_SEPARATOR);
                }
            } finally {
                try {
                    bw.flush();
                } finally {
                    if (!isBufferedWriter) {
                        Objectory.recycle((BufferedWriter) bw);
                    }
                }
            }

            return cnt;
        } finally {
            close();
        }
    }

    /**
     * Persists the sequence to the specified file using the provided function to convert elements to strings.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param writeLine the consumer to write each element to the writer
     * @param output the file to persist the sequence to
     * @return the number of elements persisted
     * @throws IllegalStateException if the sequence is already closed
     * @throws IOException if an I/O error occurs
     * @throws E if an exception occurs during the operation
     */
    @TerminalOp
    public long persist(final Throwables.BiConsumer<? super T, Writer, IOException> writeLine, final File output) throws IllegalStateException, IOException, E {
        assertNotClosed();

        return persist(null, null, writeLine, output);
    }

    /**
     * Persists the sequence to the specified file with a header and tail using the provided function to write each element.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param header the header line to be written at the beginning of the file
     * @param tail the tail line to be written at the end of the file
     * @param writeLine the function to write each element to the writer
     * @param output the file to persist the sequence to
     * @return the number of elements persisted
     * @throws IllegalStateException if the sequence is already closed
     * @throws IOException if an I/O error occurs
     * @throws E if an exception occurs during the operation
     */
    @TerminalOp
    public long persist(final String header, final String tail, final Throwables.BiConsumer<? super T, Writer, IOException> writeLine, final File output)
            throws IOException, E {
        assertNotClosed();

        final Writer writer = IOUtil.newFileWriter(output);

        try {
            return persist(header, tail, writeLine, writer);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     * Persists the sequence to the specified writer using the provided function to write each element.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param writeLine the function to write each element to the writer
     * @param output the writer to persist the sequence to
     * @return the number of elements persisted
     * @throws IllegalStateException if the sequence is already closed
     * @throws IOException if an I/O error occurs
     * @throws E if an exception occurs during the operation
     */
    @TerminalOp
    public long persist(final Throwables.BiConsumer<? super T, Writer, IOException> writeLine, final Writer output)
            throws IllegalStateException, IOException, E {
        assertNotClosed();

        return persist(null, null, writeLine, output);
    }

    /**
     * Persists the sequence to the specified writer with a header and tail using the provided function to write each element.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param header the header line to be written at the beginning of the file
     * @param tail the tail line to be written at the end of the file
     * @param writeLine the function to write each element to the writer
     * @param output the writer to persist the sequence to
     * @return the number of elements persisted
     * @throws IllegalStateException if the sequence is already closed
     * @throws IOException if an I/O error occurs
     * @throws E if an exception occurs during the operation
     */
    @TerminalOp
    public long persist(final String header, final String tail, final Throwables.BiConsumer<? super T, Writer, IOException> writeLine, final Writer output)
            throws IOException, E {
        assertNotClosed();

        try {
            final boolean isBufferedWriter = IOUtil.isBufferedWriter(output);
            final Writer bw = isBufferedWriter ? output : Objectory.createBufferedWriter(output); //NOSONAR
            long cnt = 0;

            try {
                @SuppressWarnings("resource")
                final Throwables.Iterator<T, E> iter = iteratorEx();

                if (header != null) {
                    bw.write(header);
                    bw.write(IOUtil.LINE_SEPARATOR);
                }

                while (iter.hasNext()) {
                    writeLine.accept(iter.next(), bw);
                    bw.write(IOUtil.LINE_SEPARATOR);
                    cnt++;

                    if (cnt % BATCH_SIZE_FOR_FLUSH == 0) {
                        bw.flush();
                    }
                }

                if (tail != null) {
                    bw.write(tail);
                    bw.write(IOUtil.LINE_SEPARATOR);
                }
            } finally {
                try {
                    bw.flush();
                } finally {
                    if (!isBufferedWriter) {
                        Objectory.recycle((BufferedWriter) bw);
                    }
                }
            }

            return cnt;
        } finally {
            close();
        }
    }

    /**
     * Persists the sequence to the specified prepared statement using the provided statement setter.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param stmt the prepared statement used to persist the sequence
     * @param batchSize the number of elements to include in each batch. If the batch size is less than 2, batch update won't be used.
     * @param batchIntervalInMillis the interval in milliseconds between each batch
     * @return the number of elements persisted
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified prepared statement is {@code null}, or {@code batchSize} or {@code batchIntervalInMillis} is negative
     * @throws SQLException if an SQL error occurs
     * @throws E if an exception occurs during the operation
     */
    @TerminalOp
    public long persist(final PreparedStatement stmt, final int batchSize, final long batchIntervalInMillis,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter)
            throws IllegalStateException, IllegalArgumentException, SQLException, E {
        assertNotClosed();
        checkArgNotNull(stmt, cs.PreparedStatement);
        checkArgNotNegative(batchSize, cs.batchSize);
        checkArgNotNegative(batchIntervalInMillis, cs.batchIntervalInMillis);

        try {
            @SuppressWarnings("resource")
            final Throwables.Iterator<T, E> iter = iteratorEx();
            final boolean isBatchUsed = batchSize > 1;
            long cnt = 0;

            while (iter.hasNext()) {
                stmtSetter.accept(iter.next(), stmt);

                if (isBatchUsed) {
                    stmt.addBatch();
                    cnt++;

                    if (cnt % batchSize == 0) {
                        DataSourceUtil.executeBatch(stmt);

                        if (batchIntervalInMillis > 0) {
                            N.sleepUninterruptibly(batchIntervalInMillis);
                        }
                    }
                } else {
                    stmt.execute();
                }
            }

            if (isBatchUsed && cnt % batchSize > 0) {
                DataSourceUtil.executeBatch(stmt);
            }

            return cnt;
        } finally {
            close();
        }
    }

    /**
     * Persists the sequence to the specified connection using the provided insert SQL and statement setter.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param conn the connection used to persist the sequence
     * @param insertSQL the SQL insert script used to prepare the statement
     * @param batchSize the number of elements to include in each batch. If the batch size is less than 2, batch update won't be used.
     * @param batchIntervalInMillis the interval in milliseconds between each batch
     * @return the number of elements persisted
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified Connection or insert script is {@code null}, or {@code batchSize} or {@code batchIntervalInMillis} is negative
     * @throws SQLException if an SQL error occurs
     * @throws E if an exception occurs during the operation
     */
    @TerminalOp
    public long persist(final Connection conn, final String insertSQL, final int batchSize, final long batchIntervalInMillis,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter)
            throws IllegalStateException, IllegalArgumentException, SQLException, E {
        assertNotClosed();
        checkArgNotNull(conn, cs.Connection);
        checkArgNotNull(insertSQL, cs.insertSQL);
        checkArgNotNegative(batchSize, cs.batchSize);
        checkArgNotNegative(batchIntervalInMillis, cs.batchIntervalInMillis);

        PreparedStatement stmt = null;

        try {
            stmt = conn.prepareStatement(insertSQL);

            return persist(stmt, batchSize, batchIntervalInMillis, stmtSetter);
        } finally {
            DataSourceUtil.closeQuietly(stmt);
        }
    }

    /**
     * Persists the sequence to the specified data source using the provided SQL insert statement.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param ds the data source used to persist the sequence
     * @param insertSQL the SQL insert script used to prepare the statement
     * @param batchSize the number of elements to include in each batch. If the batch size is less than 2, batch update won't be used.
     * @param batchIntervalInMillis the interval in milliseconds between each batch
     * @return the number of elements persisted
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified DataSource or insert script is {@code null}, or {@code batchSize} or {@code batchIntervalInMillis} is negative
     * @throws SQLException if an SQL error occurs
     * @throws E if an exception occurs during the operation
     */
    @TerminalOp
    public long persist(final javax.sql.DataSource ds, final String insertSQL, final int batchSize, final long batchIntervalInMillis,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter)
            throws IllegalStateException, IllegalArgumentException, SQLException, E {
        assertNotClosed();
        checkArgNotNull(ds, cs.DataSource);
        checkArgNotNull(insertSQL, cs.insertSQL);
        checkArgNotNegative(batchSize, cs.batchSize);
        checkArgNotNegative(batchIntervalInMillis, cs.batchIntervalInMillis);

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
     * Persists the sequence with CSV format to the specified file.
     *
     * The first row will be used as field names if its type is array or list,
     * or obtain the column names from the first row if its type is bean or map.
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param output the file to persist the sequence to
     * @return the number of elements persisted
     * @throws IllegalStateException if the sequence is already closed
     * @throws IOException if an I/O error occurs
     * @throws E if an exception occurs during the operation
     */
    @Beta
    @TerminalOp
    public long persistToCSV(final File output) throws IllegalStateException, IOException, E {
        final Writer writer = IOUtil.newFileWriter(output);

        try {
            return persistToCSV(writer);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     * Persists the sequence with CSV format to the specified file with the specified headers.
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param csvHeaders the headers to be used for the CSV file
     * @param output the file to persist the sequence to
     * @return the number of elements persisted
     * @throws IllegalStateException if the sequence is already closed
     * @throws IOException if an I/O error occurs
     * @throws E if an exception occurs during the operation
     */
    @TerminalOp
    public long persistToCSV(final Collection<String> csvHeaders, final File output) throws IllegalStateException, IOException, E {
        final Writer writer = IOUtil.newFileWriter(output);

        try {
            return persistToCSV(csvHeaders, writer);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     * Persists the sequence with CSV format to the specified output stream.
     *
     * The first row will be used as field names if its type is array or list,
     * or obtain the column names from the first row if its type is bean or map.
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param output the output stream to persist the sequence to
     * @return the number of elements persisted
     * @throws IllegalStateException if the sequence is already closed
     * @throws IOException if an I/O error occurs
     * @throws E if an exception occurs during the operation
     */
    @Beta
    @TerminalOp
    public long persistToCSV(final OutputStream output) throws IllegalStateException, IOException, E {
        final BufferedWriter bw = Objectory.createBufferedWriter(output);

        try {
            return persistToCSV(bw);
        } finally {
            IOUtil.close(bw);
        }
    }

    /**
     * Persists the sequence with CSV format to the specified output stream with the specified headers.
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param csvHeaders the headers to be used for the CSV file
     * @param output the output stream to persist the sequence to
     * @return the number of elements persisted
     * @throws IllegalStateException if the sequence is already closed
     * @throws IOException if an I/O error occurs
     * @throws E if an exception occurs during the operation
     */
    @TerminalOp
    public long persistToCSV(final Collection<String> csvHeaders, final OutputStream output) throws IllegalStateException, IOException, E {
        final BufferedWriter bw = Objectory.createBufferedWriter(output);

        try {
            return persistToCSV(csvHeaders, bw);
        } finally {
            IOUtil.close(bw);
        }
    }

    /**
     * Persists the sequence with CSV format to the specified writer.
     *
     * The first row will be used as field names if its type is array or list,
     * or obtain the column names from the first row if its type is bean or map.
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param output the writer to persist the sequence to
     * @return the number of elements persisted
     * @throws IllegalStateException if the sequence is already closed
     * @throws IOException if an I/O error occurs
     * @throws E if an exception occurs during the operation
     */
    @Beta
    @TerminalOp
    public long persistToCSV(final Writer output) throws IllegalStateException, IOException, E {
        return persistToCSV(null, output, true);
    }

    /**
     * Persists the sequence with CSV format to the specified writer with the specified headers.
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param csvHeaders the headers to be used for the CSV file
     * @param output the writer to persist the sequence to
     * @return the number of elements persisted
     * @throws IllegalStateException if the sequence is already closed
     * @throws IOException if an I/O error occurs
     * @throws E if an exception occurs during the operation
     */
    @TerminalOp
    public long persistToCSV(final Collection<String> csvHeaders, final Writer output) throws IllegalStateException, IllegalArgumentException, IOException, E {
        return persistToCSV(csvHeaders, output, false);
    }

    private long persistToCSV(final Collection<String> csvHeaders, final Writer output, final boolean canCsvHeadersBeEmpty)
            throws IllegalStateException, IllegalArgumentException, IOException, E {
        assertNotClosed();

        if (!canCsvHeadersBeEmpty) {
            checkArgNotEmpty(csvHeaders, cs.csvHeaders);
        }

        try {
            final List<Object> headers = N.newArrayList(csvHeaders);
            final boolean isBufferedWriter = output instanceof BufferedJSONWriter;
            final BufferedJSONWriter bw = isBufferedWriter ? (BufferedJSONWriter) output : Objectory.createBufferedJSONWriter(output);

            long cnt = 0;
            T next = null;
            Class<?> cls = null;

            try {
                @SuppressWarnings("resource")
                final Throwables.Iterator<T, E> iter = iteratorEx();

                if (iter.hasNext()) {
                    next = iter.next();
                    cnt++;
                    cls = next.getClass();

                    if (ClassUtil.isBeanClass(cls)) {
                        final BeanInfo beanInfo = ParserUtil.getBeanInfo(cls);

                        if (N.isEmpty(headers)) {
                            headers.addAll(beanInfo.propNameList);
                        }

                        final int headSize = headers.size();
                        final PropInfo[] propInfos = new PropInfo[headSize];
                        PropInfo propInfo = null;

                        for (int i = 0; i < headSize; i++) {
                            propInfos[i] = beanInfo.getPropInfo(headers.get(i).toString());
                        }

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_STRING.accept(N.stringOf(headers.get(i)), bw);
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

                        if (N.isEmpty(headers)) {
                            headers.addAll(row.keySet());
                        }

                        final int headSize = headers.size();

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_STRING.accept(N.stringOf(headers.get(i)), bw);
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
                    } else if (N.notEmpty(headers) && next instanceof Collection) {
                        final int headSize = headers.size();
                        Collection<Object> row = (Collection<Object>) next;

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_STRING.accept(N.stringOf(headers.get(i)), bw);
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
                    } else if (N.notEmpty(headers) && next instanceof Object[] row) {
                        final int headSize = headers.size();
                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_STRING.accept(N.stringOf(headers.get(i)), bw);
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
            } finally {
                try {
                    bw.flush();
                } finally {
                    if (!isBufferedWriter) {
                        Objectory.recycle((BufferedWriter) bw);
                    }
                }
            }

            return cnt;
        } finally {
            close();
        }
    }

    /**
     * Persists the sequence with JSON format to the specified file.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param output the file to persist the sequence to
     * @return the number of elements persisted
     * @throws IllegalStateException if the sequence is already closed
     * @throws IOException if an I/O error occurs
     * @throws E if an exception occurs during the operation
     */
    @TerminalOp
    public long persistToJSON(final File output) throws IllegalStateException, IOException, E {
        final Writer writer = IOUtil.newFileWriter(output);

        try {
            return persistToJSON(writer);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     * Persists the sequence with JSON format to the specified output stream.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param output the output stream to persist the sequence to
     * @return the number of elements persisted
     * @throws IllegalStateException if the sequence is already closed
     * @throws IOException if an I/O error occurs
     * @throws E if an exception occurs during the operation
     */
    @TerminalOp
    public long persistToJSON(final OutputStream output) throws IllegalStateException, IOException, E {
        final BufferedWriter bw = Objectory.createBufferedWriter(output);

        try {
            return persistToJSON(bw);
        } finally {
            IOUtil.close(bw);
        }
    }

    /**
     * Persists the sequence with JSON format to the specified writer.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param output the writer to persist the sequence to
     * @return the number of elements persisted
     * @throws IllegalStateException if the sequence is already closed
     * @throws IOException if an I/O error occurs
     * @throws E if an exception occurs during the operation
     */
    @TerminalOp
    public long persistToJSON(final Writer output) throws IllegalStateException, IOException, E {
        assertNotClosed();

        try {
            final boolean isBufferedWriter = output instanceof BufferedJSONWriter;
            final BufferedJSONWriter bw = isBufferedWriter ? (BufferedJSONWriter) output : Objectory.createBufferedJSONWriter(output); // NOSONAR

            long cnt = 0;

            try {
                @SuppressWarnings("resource")
                final Throwables.Iterator<T, E> iter = iteratorEx();

                bw.write("[");
                bw.write(IOUtil.LINE_SEPARATOR);

                while (iter.hasNext()) {
                    N.toJson(iter.next(), bw);
                    cnt++;
                }

                bw.write(IOUtil.LINE_SEPARATOR);
                bw.write("]");
                bw.write(IOUtil.LINE_SEPARATOR);
            } finally {
                try {
                    bw.flush();
                } finally {
                    if (!isBufferedWriter) {
                        Objectory.recycle((BufferedWriter) bw);
                    }
                }
            }

            return cnt;
        } finally {
            close();
        }
    }

    /**
     * Prints the sequence to the standard output.
     * Be caution to use it when the sequence is large.
     *
     * @implNote it's equivalent to {@code System.out.println(seq.join(", ", "[", "]"))}
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during the operation
     */
    @Beta
    @TerminalOp
    public void println() throws IllegalStateException, E {
        N.println(join(", ", "[", "]"));
    }

    /**
     * Casts the sequence to a sequence of type {@code Seq<T, Exception>}.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @return a sequence of type {@code Seq<T, Exception>}
     * @throws IllegalStateException if the sequence is already closed
     */
    @Beta
    @IntermediateOp
    public Seq<T, Exception> cast() throws IllegalStateException {
        assertNotClosed();

        return (Seq<T, Exception>) this;
    }

    /**
     * Returns a sequential {@code Stream} with this sequence as its source.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @return a sequential {@code Stream} with this sequence as its source
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Stream<T> stream() throws IllegalStateException {
        assertNotClosed();

        if (N.isEmpty(closeHandlers)) {
            return Stream.of(newObjIteratorEx(elements));
        } else {
            return Stream.of(newObjIteratorEx(elements)).onClose(this::close);
        }
    }

    //    /**
    //     *
    //     * @return
    //     * @throws IllegalStateException
    //     * @throws IllegalArgumentException
    //     */
    //    @IntermediateOp
    //    public java.util.stream.Stream<T> toJdkStream() throws IllegalStateException, IllegalArgumentException {
    //        assertNotClosed();
    //
    //        final Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(newObjIteratorEx(elements), Spliterator.ORDERED | Spliterator.IMMUTABLE);
    //
    //        if (isEmptyCloseHandlers(closeHandlers)) {
    //            return StreamSupport.stream(spliterator, false);
    //        } else {
    //            return StreamSupport.stream(spliterator, false).onClose(this::close);
    //        }
    //    }

    /**
     * Transforms the current sequence into another sequence by applying the provided function.
     * The function takes the current sequence as input and returns a new sequence.
     *
     * <br />
     * <br />
     * {@code Seq.defer(Supplier)} can be used to avoid eager loading by terminal operations invoked in {@code transfer}. For example:
     * <pre>
     * <code>
     *     seq.transform(s -> Seq.defer(() -> s.(..).someTerminalOperation(...)));
     * </code>
     * </pre>
     *
     * @param <U> The type of elements in the returned stream.
     * @param transfer The function to be applied on the current sequence to produce a new stream.
     * @return A new sequence transformed by the provided function.
     * @throws IllegalStateException if the sequence has already been operated upon or closed.
     * @see #transformB(Function)
     * @see #transformB(Function, boolean)
     * @see #sps(Function)
     * @see #sps(int, Function)
     */
    @Beta
    @IntermediateOp
    public <U> Seq<U, E> transform(final Function<? super Seq<T, E>, Seq<U, E>> transfer) { //NOSONAR
        assertNotClosed();
        checkArgNotNull(transfer, cs.transfer);

        //    final Throwables.Supplier<Seq<TT, EE>, EE> delayInitializer = () -> transfer.apply(this);
        //
        //    return Seq.defer(delayInitializer);

        return transfer.apply(this);
    }

    /**
     * Transforms the current Seq into another Seq by applying the provided function.
     * The function takes a Stream as input and returns a new Stream.
     * The returned Stream is then wrapped into a Seq.
     *
     * @param <U> The type of elements in the returned stream.
     * @param transfer The function to be applied on the current stream to produce a new stream.
     * @return A new Seq transformed by the provided function.
     * @throws IllegalStateException if the stream has already been operated upon or closed.
     * @throws IllegalArgumentException if the provided function is {@code null}.
     * @see #transform(Function)
     * @see #transformB(Function, boolean)
     * @see #sps(Function)
     * @see #sps(int, Function)
     */
    @Beta
    @IntermediateOp
    public <U> Seq<U, E> transformB(final Function<? super Stream<T>, ? extends Stream<? extends U>> transfer)
            throws IllegalStateException, IllegalArgumentException {
        // Major reason for commenting out below lines is to keep consistent with method transform.
        //        assertNotClosed();
        //        checkArgNotNull(transfer, "transfer");
        //
        //        final Throwables.Supplier<Seq<U, E>, E> delayInitializer = () -> Seq.from(transfer.apply(this.unchecked()));
        //
        //        return Seq.defer(delayInitializer);

        return transformB(transfer, false);
    }

    /**
     * Transforms the current Seq into another Seq by applying the provided function.
     * The function takes a Stream as input and returns a new Stream.
     * The returned Stream is then wrapped into a Seq.
     * This method allows for deferred execution, meaning the transformation will not be applied immediately,
     * but only when the returned Seq is consumed.
     *
     * @param <U> The type of elements in the returned stream.
     * @param transfer The function to be applied on the current stream to produce a new stream.
     * @param deferred A boolean flag indicating whether the transformation should be deferred.
     * @return A new Seq transformed by the provided function.
     * @throws IllegalArgumentException if the provided function is {@code null}.
     * @see #transform(Function)
     * @see #transformB(Function)
     * @see #sps(Function)
     * @see #sps(int, Function)
     */
    @Beta
    @IntermediateOp
    public <U> Seq<U, E> transformB(final Function<? super Stream<T>, ? extends Stream<? extends U>> transfer, final boolean deferred)
            throws IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(transfer, cs.transfer);

        if (deferred) {
            final Throwables.Supplier<Seq<U, E>, E> delayInitializer = () -> create(transfer.apply(this.stream()), true);
            return Seq.defer(delayInitializer);
        } else {
            return create(transfer.apply(this.stream()), true);
        }
    }

    //    /**
    //     * Transforms the current Seq into another Seq with potentially different type parameters.
    //     * This method is an intermediate operation, meaning it builds upon the Seq but does not trigger processing of data.
    //     *
    //     * @param <TT> the type of the elements in the returned Seq
    //     * @param <EE> the type of the exception that can be thrown when processing elements in the returned Seq
    //     * @param transfer a function that takes the current Seq and returns a new Seq
    //     * @return a Seq that contains the elements resulting from applying the transfer function to this Seq
    //     * @throws IllegalStateException if the stream has already been operated upon or closed
    //     * @deprecated replaced by {@link #transform(Function)}
    //     */
    //    @Beta
    //    @IntermediateOp
    //    @Deprecated
    //    public <TT, EE extends Exception> Seq<TT, EE> __(final Function<? super Seq<T, E>, Seq<TT, EE>> transfer) { //NOSONAR
    //        return transform(transfer);
    //    }

    /**
     * Temporarily switches the sequence to a parallel stream for the operation defined by {@code ops}, and then switches it back to a sequence.
     *
     * @param <R> The type of elements in the returned stream.
     * @param ops The function to be applied on the parallel stream.
     * @return A sequence with elements of type R.
     * @throws IllegalStateException if the sequence has already been operated upon or closed.
     * @see #sps(int, Function)
     * @see #transform(Function)
     * @see #transformB(Function)
     * @see #transformB(Function, boolean)
     * @see Stream#sps(Function)
     */
    @Beta
    @IntermediateOp
    public <R> Seq<R, E> sps(final Function<? super Stream<T>, ? extends Stream<? extends R>> ops) throws IllegalStateException {
        assertNotClosed();

        return create(ops.apply(this.stream().parallel()), true);
    }

    /**
     * Temporarily switches the sequence to a parallel stream for the operation defined by {@code ops},
     * and then switches it back to a sequence.
     *
     * @param <R> the type of the elements in the resulting sequence
     * @param maxThreadNum the maximum number of threads to use for parallel processing
     * @param ops the function defining the operations to be performed on the parallel stream
     * @return a sequence containing the elements resulting from applying the operations defined by {@code ops}
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @see #sps(Function)
     * @see #transform(Function)
     * @see #transformB(Function)
     * @see #transformB(Function, boolean)
     * @see Stream#sps(int, Function)
     */
    @Beta
    @IntermediateOp
    public <R> Seq<R, E> sps(final int maxThreadNum, final Function<? super Stream<T>, ? extends Stream<? extends R>> ops) throws IllegalStateException {
        assertNotClosed();
        checkArgPositive(maxThreadNum, cs.maxThreadNum);

        return create(ops.apply(this.stream().parallel(maxThreadNum)), true);
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

    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code filter} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param predicate
    //     * @return
    //     * @see Stream#spsFilter(Predicate)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, E> spsFilter(final Throwables.Predicate<? super T, E> predicate) {
    //        final Function<Stream<T>, Stream<T>> ops = s -> s.filter(Fn.pp(predicate));
    //
    //        return sps(ops);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code map} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param <R>
    //     * @param mapper
    //     * @return
    //     * @see Stream#spsMap(Function)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, E> spsMap(final Throwables.Function<? super T, ? extends R, E> mapper) {
    //        final Function<Stream<T>, Stream<R>> ops = s -> s.map(Fn.ff(mapper));
    //
    //        return sps(ops);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param <R>
    //     * @param mapper
    //     * @return
    //     * @see Stream#spsFlatMap(Function)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, E> spsFlatMap(final Throwables.Function<? super T, ? extends Stream<? extends R>, E> mapper) {
    //        final Function<Stream<T>, Stream<R>> ops = s -> s.flatMap(Fn.ff(mapper));
    //
    //        return sps(ops);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param <R>
    //     * @param mapper
    //     * @return
    //     * @see Stream#spsFlatmap(Function)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, E> spsFlatmap(final Throwables.Function<? super T, ? extends Collection<? extends R>, E> mapper) { //NOSONAR
    //        final Function<Stream<T>, Stream<R>> ops = s -> s.flatmap(Fn.ff(mapper));
    //
    //        return sps(ops);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code onEach} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param action
    //     * @return
    //     * @see Stream#onEach(Consumer)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, E> spsOnEach(final Throwables.Consumer<? super T, E> action) {
    //        final Function<Stream<T>, Stream<T>> ops = s -> s.onEach(Fn.cc(action));
    //
    //        return sps(ops);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code filter} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     * @param maxThreadNum
    //     * @param predicate
    //     * @return
    //     * @see Stream#spsFilter(int, Predicate)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, E> spsFilter(final int maxThreadNum, final Throwables.Predicate<? super T, E> predicate) {
    //        final Function<Stream<T>, Stream<T>> ops = s -> s.filter(Fn.pp(predicate));
    //
    //        return sps(maxThreadNum, ops);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code map} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     * @param <R>
    //     * @param maxThreadNum
    //     * @param mapper
    //     * @return
    //     * @see Stream#spsMap(int, Function)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, E> spsMap(final int maxThreadNum, final Throwables.Function<? super T, ? extends R, E> mapper) {
    //        final Function<Stream<T>, Stream<R>> ops = s -> s.map(Fn.ff(mapper));
    //
    //        return sps(maxThreadNum, ops);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     * @param <R>
    //     * @param maxThreadNum
    //     * @param mapper
    //     * @return
    //     * @see Stream#spsFlatMap(int, Function)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, E> spsFlatMap(final int maxThreadNum, final Throwables.Function<? super T, ? extends Stream<? extends R>, E> mapper) { //NOSONAR
    //        final Function<Stream<T>, Stream<R>> ops = s -> s.flatMap(Fn.ff(mapper));
    //
    //        return sps(maxThreadNum, ops);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     * @param <R>
    //     * @param maxThreadNum
    //     * @param mapper
    //     * @return
    //     * @see Stream#spsFlatmap(int, Function)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, E> spsFlatmap(final int maxThreadNum, final Throwables.Function<? super T, ? extends Collection<? extends R>, E> mapper) { //NOSONAR
    //        final Function<Stream<T>, Stream<R>> ops = s -> s.flatmap(Fn.ff(mapper));
    //
    //        return sps(maxThreadNum, ops);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code onEach} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     * @param maxThreadNum
    //     * @param action
    //     * @return
    //     * @see Stream#onEach(int, Consumer)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, E> spsOnEach(final int maxThreadNum, final Throwables.Consumer<? super T, E> action) {
    //        final Function<Stream<T>, Stream<T>> ops = s -> s.onEach(Fn.cc(action));
    //
    //        return sps(maxThreadNum, ops);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code filter} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param predicate
    //     * @return
    //     * @see Stream#spsFilter(Predicate)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, Exception> spsFilterE(final Throwables.Predicate<? super T, ? extends Exception> predicate) {
    //        return create(stream().spsFilterE(predicate), true);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code map} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     * @param <U>
    //     * @param mapper
    //     * @return
    //     * @see Stream#spsMap(Function)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <U> Seq<U, Exception> spsMapE(final Throwables.Function<? super T, ? extends U, ? extends Exception> mapper) {
    //        return create(stream().<U> spsMapE(mapper), true);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param <R>
    //     * @param mapper
    //     * @return
    //     * @see Stream#spsFlatMap(Function)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, Exception> spsFlatMapE(final Throwables.Function<? super T, ? extends Stream<? extends R>, ? extends Exception> mapper) {
    //        return create(stream().<R> spsFlatMapE(mapper), true);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param <R>
    //     * @param mapper
    //     * @return
    //     * @see Stream#spsFlatmap(Function)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, Exception> spsFlatmapE( //NOSONAR
    //            final Throwables.Function<? super T, ? extends Collection<? extends R>, ? extends Exception> mapper) {
    //        return create(stream().<R> spsFlatmapE(mapper), true);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code onEach} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param action
    //     * @return
    //     * @see Stream#onEach(Consumer)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, Exception> spsOnEachE(final Throwables.Consumer<? super T, ? extends Exception> action) {
    //        return create(stream().spsOnEachE(action), true);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code filter} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param maxThreadNum
    //     * @param predicate
    //     * @return
    //     * @see Stream#spsFilter(Predicate)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, Exception> spsFilterE(final int maxThreadNum, final Throwables.Predicate<? super T, ? extends Exception> predicate) {
    //        return create(stream().spsFilterE(maxThreadNum, predicate), true);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code map} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     * @param <U>
    //     * @param maxThreadNum
    //     * @param mapper
    //     * @return
    //     * @see Stream#spsMap(Function)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <U> Seq<U, Exception> spsMapE(final int maxThreadNum, final Throwables.Function<? super T, ? extends U, ? extends Exception> mapper) {
    //        return create(stream().<U> spsMapE(maxThreadNum, mapper), true);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param <R>
    //     * @param maxThreadNum
    //     * @param mapper
    //     * @return
    //     * @see Stream#spsFlatMap(Function)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, Exception> spsFlatMapE(final int maxThreadNum, //NOSONAR
    //            final Throwables.Function<? super T, ? extends Stream<? extends R>, ? extends Exception> mapper) {
    //        return create(stream().<R> spsFlatMapE(maxThreadNum, mapper), true);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param <R>
    //     * @param maxThreadNum
    //     * @param mapper
    //     * @return
    //     * @see Stream#spsFlatmap(Function)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, Exception> spsFlatmapE(final int maxThreadNum, //NOSONAR
    //            final Throwables.Function<? super T, ? extends Collection<? extends R>, ? extends Exception> mapper) {
    //        return create(stream().<R> spsFlatmapE(maxThreadNum, mapper), true);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code onEach} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param maxThreadNum
    //     * @param action
    //     * @return
    //     * @see Stream#onEach(Consumer)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, Exception> spsOnEachE(final int maxThreadNum, final Throwables.Consumer<? super T, ? extends Exception> action) {
    //        return create(stream().spsOnEachE(maxThreadNum, action), true);
    //    }
    //
    //    /**
    //     *
    //     * @param predicate
    //     * @return
    //     * @throws IllegalStateException
    //     */
    //    @Beta
    //    @SuppressWarnings("rawtypes")
    //    @IntermediateOp
    //    public Seq<T, Exception> filterE(final Throwables.Predicate<? super T, ? extends Exception> predicate) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return create(new Throwables.Iterator<T, Exception>() {
    //            private boolean hasNext = false;
    //            private T next = null;
    //
    //            @Override
    //            public boolean hasNext() throws Exception {
    //                if (!hasNext) {
    //                    while (elements.hasNext()) {
    //                        next = elements.next();
    //
    //                        if (predicate.test(next)) {
    //                            hasNext = true;
    //                            break;
    //                        }
    //                    }
    //                }
    //
    //                return hasNext;
    //            }
    //
    //            @Override
    //            public T next() throws Exception {
    //                if (!hasNext && !hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                hasNext = false;
    //
    //                return next;
    //            }
    //        }, sorted, cmp, (Deque) closeHandlers);
    //    }
    //
    //    /**
    //     *
    //     * @param <U>
    //     * @param mapper
    //     * @return
    //     * @throws IllegalStateException
    //     */
    //    @Beta
    //    @SuppressWarnings("rawtypes")
    //    @IntermediateOp
    //    public <U> Seq<U, Exception> mapE(final Throwables.Function<? super T, ? extends U, ? extends Exception> mapper) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return create(new Throwables.Iterator<U, Exception>() {
    //            @Override
    //            public boolean hasNext() throws Exception {
    //                return elements.hasNext();
    //            }
    //
    //            @Override
    //            public U next() throws Exception {
    //                return mapper.apply(elements.next());
    //            }
    //        }, (Deque) closeHandlers);
    //    }
    //
    //    /**
    //     *
    //     * @param <R>
    //     * @param mapper
    //     * @return
    //     * @throws IllegalStateException
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, Exception> flatMapE(final Throwables.Function<? super T, ? extends Seq<? extends R, ? extends Exception>, ? extends Exception> mapper)
    //            throws IllegalStateException {
    //        assertNotClosed();
    //
    //        final Throwables.Iterator<R, Exception> iter = new Throwables.Iterator<>() {
    //            private Throwables.Iterator<? extends R, ? extends Exception> cur = null;
    //            private Seq<? extends R, ? extends Exception> s = null;
    //            private Deque<LocalRunnable> closeHandle = null;
    //
    //            public boolean hasNext() throws Exception {
    //                while (cur == null || !cur.hasNext()) {
    //                    if (elements.hasNext()) {
    //                        if (closeHandle != null) {
    //                            final Deque<LocalRunnable> tmp = closeHandle;
    //                            closeHandle = null;
    //                            Seq.close(tmp);
    //                        }
    //
    //                        s = mapper.apply(elements.next());
    //
    //                        if (s == null) {
    //                            cur = null;
    //                        } else {
    //                            if (N.notEmpty(s.closeHandlers)) {
    //                                closeHandle = s.closeHandlers;
    //                            }
    //
    //                            cur = s.elements;
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
    //            public R next() throws Exception {
    //                if ((cur == null || !cur.hasNext()) && !hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                return cur.next();
    //            }
    //
    //            @Override
    //            protected void closeResource() {
    //                if (closeHandle != null) {
    //                    Seq.close(closeHandle);
    //                }
    //            }
    //        };
    //
    //        final Deque<LocalRunnable> newCloseHandlers = new ArrayDeque<>(N.size(closeHandlers) + 1);
    //
    //        if (N.notEmpty(closeHandlers)) {
    //            newCloseHandlers.addAll(closeHandlers);
    //        }
    //
    //        newCloseHandlers.add(newCloseHandler(iter));
    //
    //        return create(iter, newCloseHandlers);
    //    }
    //
    //    /**
    //     *
    //     * @param <R>
    //     * @param mapper
    //     * @return
    //     */
    //    @Beta
    //    @SuppressWarnings("rawtypes")
    //    @IntermediateOp
    //    public <R> Seq<R, Exception> flatmapE(final Throwables.Function<? super T, ? extends Collection<? extends R>, ? extends Exception> mapper) { //NOSONAR
    //        assertNotClosed();
    //
    //        return create(new Throwables.Iterator<R, Exception>() {
    //            private Iterator<? extends R> cur = null;
    //            private Collection<? extends R> c = null;
    //
    //            @Override
    //            public boolean hasNext() throws Exception {
    //                while ((cur == null || !cur.hasNext()) && elements.hasNext()) {
    //                    c = mapper.apply(elements.next());
    //                    cur = N.isEmpty(c) ? null : c.iterator();
    //                }
    //
    //                return cur != null && cur.hasNext();
    //            }
    //
    //            @Override
    //            public R next() throws Exception {
    //                if ((cur == null || !cur.hasNext()) && !hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                return cur.next();
    //            }
    //        }, (Deque) closeHandlers);
    //    }
    //
    //    /**
    //     *
    //     * @param action
    //     * @return
    //     * @throws IllegalStateException
    //     */
    //    @Beta
    //    @SuppressWarnings("rawtypes")
    //    @IntermediateOp
    //    public Seq<T, Exception> onEachE(final Throwables.Consumer<? super T, ? extends Exception> action) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return create(new Throwables.Iterator<T, Exception>() {
    //            @Override
    //            public boolean hasNext() throws Exception {
    //                return elements.hasNext();
    //            }
    //
    //            @Override
    //            public T next() throws Exception {
    //                final T next = elements.next();
    //                action.accept(next);
    //                return next;
    //            }
    //        }, sorted, cmp, (Deque) closeHandlers);
    //    }

    // #######################################9X9#######################################
    // #######################################9X9#######################################

    /**
     * Executes the provided terminal operation asynchronously on this sequence using the default Executor.
     * The terminal operation is a function that consumes this sequence and may throw an exception.
     * The result of the operation is wrapped in a ContinuableFuture.
     *
     * <br />
     * This is a terminal operation and will close the sequence after execution.
     *
     * @param terminalAction the terminal operation to be executed on this sequence
     * @return a ContinuableFuture representing the result of the asynchronous computation
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if terminalAction is null
     */
    @Beta
    @TerminalOp
    public ContinuableFuture<Void> asyncRun(final Throwables.Consumer<? super Seq<T, E>, ? extends Exception> terminalAction)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(terminalAction, cs.terminalAction);

        return ContinuableFuture.run(() -> terminalAction.accept(Seq.this));
    }

    /**
     * Executes the provided terminal operation asynchronously on this sequence using the provided Executor.
     * The terminal operation is a function that consumes this sequence and may throw an exception.
     * The result of the operation is wrapped in a ContinuableFuture.
     *
     * <br />
     * This is a terminal operation and will close the sequence after execution.
     *
     * @param terminalAction the terminal operation to be executed on this sequence
     * @param executor the executor to run the terminal operation
     * @return a ContinuableFuture representing the result of the asynchronous computation
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if terminalAction or executor is null
     */
    @Beta
    @TerminalOp
    public ContinuableFuture<Void> asyncRun(final Throwables.Consumer<? super Seq<T, E>, ? extends Exception> terminalAction, final Executor executor)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(terminalAction, cs.terminalAction);
        checkArgNotNull(executor, cs.executor);

        return ContinuableFuture.run(() -> terminalAction.accept(Seq.this), executor);
    }

    /**
     * Executes the provided terminal operation asynchronously on this sequence.
     * The terminal operation is a function that consumes this sequence and may throw an exception.
     * The result of the operation is wrapped in a ContinuableFuture.
     *
     * <br />
     * This is a terminal operation and will close the sequence after execution.
     *
     * @param terminalAction the terminal operation to be executed on this sequence
     * @return a ContinuableFuture representing the result of the asynchronous computation
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if terminalAction is null
     */
    @Beta
    @TerminalOp
    public <R> ContinuableFuture<R> asyncCall(final Throwables.Function<? super Seq<T, E>, R, ? extends Exception> terminalAction)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(terminalAction, cs.terminalAction);

        return ContinuableFuture.call(() -> terminalAction.apply(Seq.this));
    }

    /**
     * Executes the provided terminal operation asynchronously on this sequence.
     * The terminal operation is a function that consumes this sequence and may throw an exception.
     * The result of the operation is wrapped in a ContinuableFuture.
     *
     * <br />
     * This is a terminal operation and will close the sequence after execution.
     *
     * @param terminalAction the terminal operation to be executed on this sequence
     * @return a ContinuableFuture representing the result of the asynchronous computation
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if terminalAction or executor is null
     */
    @Beta
    @TerminalOp
    public <R> ContinuableFuture<R> asyncCall(final Throwables.Function<? super Seq<T, E>, R, ? extends Exception> terminalAction, final Executor executor)
            throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(terminalAction, cs.terminalAction);
        checkArgNotNull(executor, cs.executor);

        return ContinuableFuture.call(() -> terminalAction.apply(Seq.this), executor);
    }

    /**
     * Applies the provided function to this sequence if it is not empty.
     * The function is a Throwables.Function that takes this sequence as input and returns a result of type R.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <R> the type of the result
     * @param <E2> the type of the exception the function may throw
     * @param func the function to be applied to this sequence
     * @return an Optional containing the result of the function application if this sequence is not empty,
     *         an empty Optional otherwise
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified function is null
     * @throws E if the action throws an exception
     * @throws E2 if the function throws an exception
     */
    @TerminalOp
    public <R, E2 extends Exception> u.Optional<R> applyIfNotEmpty(final Throwables.Function<? super Seq<T, E>, R, E2> func)
            throws IllegalStateException, E, E2 {
        assertNotClosed();
        checkArgNotNull(func, cs.func);

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
     * Executes the provided action on this sequence if it is not empty.
     * The action is a consumer that takes this sequence as input and may throw an exception.
     *
     * <br />
     * This is a terminal operation and will close the sequence.
     *
     * @param <E2> the type of the exception the action may throw
     * @param action the action to be executed on this sequence
     * @return an OrElse instance representing the result of the operation
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified action is null
     * @throws E if the action throws an exception
     * @throws E2 if the action throws an exception
     */
    @TerminalOp
    public <E2 extends Exception> OrElse acceptIfNotEmpty(final Throwables.Consumer<? super Seq<T, E>, E2> action) throws IllegalStateException, E, E2 {
        assertNotClosed();
        checkArgNotNull(action, cs.action);

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

    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code filter} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param predicate
    //     * @return
    //     * @see Stream#spsFilter(Predicate)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, E> spsFilter(final Throwables.Predicate<? super T, E> predicate) {
    //        final Function<Stream<T>, Stream<T>> ops = s -> s.filter(Fn.pp(predicate));
    //
    //        return sps(ops);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code map} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param <R>
    //     * @param mapper
    //     * @return
    //     * @see Stream#spsMap(Function)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, E> spsMap(final Throwables.Function<? super T, ? extends R, E> mapper) {
    //        final Function<Stream<T>, Stream<R>> ops = s -> s.map(Fn.ff(mapper));
    //
    //        return sps(ops);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param <R>
    //     * @param mapper
    //     * @return
    //     * @see Stream#spsFlatMap(Function)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, E> spsFlatMap(final Throwables.Function<? super T, ? extends Stream<? extends R>, E> mapper) {
    //        final Function<Stream<T>, Stream<R>> ops = s -> s.flatMap(Fn.ff(mapper));
    //
    //        return sps(ops);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param <R>
    //     * @param mapper
    //     * @return
    //     * @see Stream#spsFlatmap(Function)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, E> spsFlatmap(final Throwables.Function<? super T, ? extends Collection<? extends R>, E> mapper) { //NOSONAR
    //        final Function<Stream<T>, Stream<R>> ops = s -> s.flatmap(Fn.ff(mapper));
    //
    //        return sps(ops);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code onEach} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param action
    //     * @return
    //     * @see Stream#onEach(Consumer)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, E> spsOnEach(final Throwables.Consumer<? super T, E> action) {
    //        final Function<Stream<T>, Stream<T>> ops = s -> s.onEach(Fn.cc(action));
    //
    //        return sps(ops);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code filter} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     * @param maxThreadNum
    //     * @param predicate
    //     * @return
    //     * @see Stream#spsFilter(int, Predicate)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, E> spsFilter(final int maxThreadNum, final Throwables.Predicate<? super T, E> predicate) {
    //        final Function<Stream<T>, Stream<T>> ops = s -> s.filter(Fn.pp(predicate));
    //
    //        return sps(maxThreadNum, ops);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code map} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     * @param <R>
    //     * @param maxThreadNum
    //     * @param mapper
    //     * @return
    //     * @see Stream#spsMap(int, Function)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, E> spsMap(final int maxThreadNum, final Throwables.Function<? super T, ? extends R, E> mapper) {
    //        final Function<Stream<T>, Stream<R>> ops = s -> s.map(Fn.ff(mapper));
    //
    //        return sps(maxThreadNum, ops);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     * @param <R>
    //     * @param maxThreadNum
    //     * @param mapper
    //     * @return
    //     * @see Stream#spsFlatMap(int, Function)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, E> spsFlatMap(final int maxThreadNum, final Throwables.Function<? super T, ? extends Stream<? extends R>, E> mapper) { //NOSONAR
    //        final Function<Stream<T>, Stream<R>> ops = s -> s.flatMap(Fn.ff(mapper));
    //
    //        return sps(maxThreadNum, ops);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     * @param <R>
    //     * @param maxThreadNum
    //     * @param mapper
    //     * @return
    //     * @see Stream#spsFlatmap(int, Function)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, E> spsFlatmap(final int maxThreadNum, final Throwables.Function<? super T, ? extends Collection<? extends R>, E> mapper) { //NOSONAR
    //        final Function<Stream<T>, Stream<R>> ops = s -> s.flatmap(Fn.ff(mapper));
    //
    //        return sps(maxThreadNum, ops);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code onEach} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     * @param maxThreadNum
    //     * @param action
    //     * @return
    //     * @see Stream#onEach(int, Consumer)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, E> spsOnEach(final int maxThreadNum, final Throwables.Consumer<? super T, E> action) {
    //        final Function<Stream<T>, Stream<T>> ops = s -> s.onEach(Fn.cc(action));
    //
    //        return sps(maxThreadNum, ops);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code filter} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param predicate
    //     * @return
    //     * @see Stream#spsFilter(Predicate)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, Exception> spsFilterE(final Throwables.Predicate<? super T, ? extends Exception> predicate) {
    //        return create(stream().spsFilterE(predicate), true);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code map} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     * @param <U>
    //     * @param mapper
    //     * @return
    //     * @see Stream#spsMap(Function)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <U> Seq<U, Exception> spsMapE(final Throwables.Function<? super T, ? extends U, ? extends Exception> mapper) {
    //        return create(stream().<U> spsMapE(mapper), true);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param <R>
    //     * @param mapper
    //     * @return
    //     * @see Stream#spsFlatMap(Function)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, Exception> spsFlatMapE(final Throwables.Function<? super T, ? extends Stream<? extends R>, ? extends Exception> mapper) {
    //        return create(stream().<R> spsFlatMapE(mapper), true);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param <R>
    //     * @param mapper
    //     * @return
    //     * @see Stream#spsFlatmap(Function)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, Exception> spsFlatmapE( //NOSONAR
    //            final Throwables.Function<? super T, ? extends Collection<? extends R>, ? extends Exception> mapper) {
    //        return create(stream().<R> spsFlatmapE(mapper), true);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code onEach} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param action
    //     * @return
    //     * @see Stream#onEach(Consumer)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, Exception> spsOnEachE(final Throwables.Consumer<? super T, ? extends Exception> action) {
    //        return create(stream().spsOnEachE(action), true);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code filter} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param maxThreadNum
    //     * @param predicate
    //     * @return
    //     * @see Stream#spsFilter(Predicate)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, Exception> spsFilterE(final int maxThreadNum, final Throwables.Predicate<? super T, ? extends Exception> predicate) {
    //        return create(stream().spsFilterE(maxThreadNum, predicate), true);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code map} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     * @param <U>
    //     * @param maxThreadNum
    //     * @param mapper
    //     * @return
    //     * @see Stream#spsMap(Function)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <U> Seq<U, Exception> spsMapE(final int maxThreadNum, final Throwables.Function<? super T, ? extends U, ? extends Exception> mapper) {
    //        return create(stream().<U> spsMapE(maxThreadNum, mapper), true);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param <R>
    //     * @param maxThreadNum
    //     * @param mapper
    //     * @return
    //     * @see Stream#spsFlatMap(Function)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, Exception> spsFlatMapE(final int maxThreadNum, //NOSONAR
    //            final Throwables.Function<? super T, ? extends Stream<? extends R>, ? extends Exception> mapper) {
    //        return create(stream().<R> spsFlatMapE(maxThreadNum, mapper), true);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param <R>
    //     * @param maxThreadNum
    //     * @param mapper
    //     * @return
    //     * @see Stream#spsFlatmap(Function)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, Exception> spsFlatmapE(final int maxThreadNum, //NOSONAR
    //            final Throwables.Function<? super T, ? extends Collection<? extends R>, ? extends Exception> mapper) {
    //        return create(stream().<R> spsFlatmapE(maxThreadNum, mapper), true);
    //    }
    //
    //    /**
    //     * Temporarily switch the stream to parallel stream for operation {@code onEach} and then switch back to sequence stream.
    //     * <br />
    //     *
    //     *
    //     * @param maxThreadNum
    //     * @param action
    //     * @return
    //     * @see Stream#onEach(Consumer)
    //     * @see ExceptionUtil#toRuntimeException(Throwable)
    //     * @see ExceptionUtil#registerRuntimeExceptionMapper(Class, Function)
    //     * @see ExceptionUtil#hasCause(Throwable, Class)
    //     * @see ExceptionUtil#hasCause(Throwable, Predicate)
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public Seq<T, Exception> spsOnEachE(final int maxThreadNum, final Throwables.Consumer<? super T, ? extends Exception> action) {
    //        return create(stream().spsOnEachE(maxThreadNum, action), true);
    //    }
    //
    //    /**
    //     *
    //     * @param predicate
    //     * @return
    //     * @throws IllegalStateException
    //     */
    //    @Beta
    //    @SuppressWarnings("rawtypes")
    //    @IntermediateOp
    //    public Seq<T, Exception> filterE(final Throwables.Predicate<? super T, ? extends Exception> predicate) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return create(new Throwables.Iterator<T, Exception>() {
    //            private boolean hasNext = false;
    //            private T next = null;
    //
    //            @Override
    //            public boolean hasNext() throws Exception {
    //                if (!hasNext) {
    //                    while (elements.hasNext()) {
    //                        next = elements.next();
    //
    //                        if (predicate.test(next)) {
    //                            hasNext = true;
    //                            break;
    //                        }
    //                    }
    //                }
    //
    //                return hasNext;
    //            }
    //
    //            @Override
    //            public T next() throws Exception {
    //                if (!hasNext && !hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                hasNext = false;
    //
    //                return next;
    //            }
    //        }, sorted, cmp, (Deque) closeHandlers);
    //    }
    //
    //    /**
    //     *
    //     * @param <U>
    //     * @param mapper
    //     * @return
    //     * @throws IllegalStateException
    //     */
    //    @Beta
    //    @SuppressWarnings("rawtypes")
    //    @IntermediateOp
    //    public <U> Seq<U, Exception> mapE(final Throwables.Function<? super T, ? extends U, ? extends Exception> mapper) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return create(new Throwables.Iterator<U, Exception>() {
    //            @Override
    //            public boolean hasNext() throws Exception {
    //                return elements.hasNext();
    //            }
    //
    //            @Override
    //            public U next() throws Exception {
    //                return mapper.apply(elements.next());
    //            }
    //        }, (Deque) closeHandlers);
    //    }
    //
    //    /**
    //     *
    //     * @param <R>
    //     * @param mapper
    //     * @return
    //     * @throws IllegalStateException
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> Seq<R, Exception> flatMapE(final Throwables.Function<? super T, ? extends Seq<? extends R, ? extends Exception>, ? extends Exception> mapper)
    //            throws IllegalStateException {
    //        assertNotClosed();
    //
    //        final Throwables.Iterator<R, Exception> iter = new Throwables.Iterator<>() {
    //            private Throwables.Iterator<? extends R, ? extends Exception> cur = null;
    //            private Seq<? extends R, ? extends Exception> s = null;
    //            private Deque<LocalRunnable> closeHandle = null;
    //
    //            public boolean hasNext() throws Exception {
    //                while (cur == null || !cur.hasNext()) {
    //                    if (elements.hasNext()) {
    //                        if (closeHandle != null) {
    //                            final Deque<LocalRunnable> tmp = closeHandle;
    //                            closeHandle = null;
    //                            Seq.close(tmp);
    //                        }
    //
    //                        s = mapper.apply(elements.next());
    //
    //                        if (s == null) {
    //                            cur = null;
    //                        } else {
    //                            if (N.notEmpty(s.closeHandlers)) {
    //                                closeHandle = s.closeHandlers;
    //                            }
    //
    //                            cur = s.elements;
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
    //            public R next() throws Exception {
    //                if ((cur == null || !cur.hasNext()) && !hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                return cur.next();
    //            }
    //
    //            @Override
    //            protected void closeResource() {
    //                if (closeHandle != null) {
    //                    Seq.close(closeHandle);
    //                }
    //            }
    //        };
    //
    //        final Deque<LocalRunnable> newCloseHandlers = new ArrayDeque<>(N.size(closeHandlers) + 1);
    //
    //        if (N.notEmpty(closeHandlers)) {
    //            newCloseHandlers.addAll(closeHandlers);
    //        }
    //
    //        newCloseHandlers.add(newCloseHandler(iter));
    //
    //        return create(iter, newCloseHandlers);
    //    }
    //
    //    /**
    //     *
    //     * @param <R>
    //     * @param mapper
    //     * @return
    //     */
    //    @Beta
    //    @SuppressWarnings("rawtypes")
    //    @IntermediateOp
    //    public <R> Seq<R, Exception> flatmapE(final Throwables.Function<? super T, ? extends Collection<? extends R>, ? extends Exception> mapper) { //NOSONAR
    //        assertNotClosed();
    //
    //        return create(new Throwables.Iterator<R, Exception>() {
    //            private Iterator<? extends R> cur = null;
    //            private Collection<? extends R> c = null;
    //
    //            @Override
    //            public boolean hasNext() throws Exception {
    //                while ((cur == null || !cur.hasNext()) && elements.hasNext()) {
    //                    c = mapper.apply(elements.next());
    //                    cur = N.isEmpty(c) ? null : c.iterator();
    //                }
    //
    //                return cur != null && cur.hasNext();
    //            }
    //
    //            @Override
    //            public R next() throws Exception {
    //                if ((cur == null || !cur.hasNext()) && !hasNext()) {
    //                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
    //                }
    //
    //                return cur.next();
    //            }
    //        }, (Deque) closeHandlers);
    //    }
    //
    //    /**
    //     *
    //     * @param action
    //     * @return
    //     * @throws IllegalStateException
    //     */
    //    @Beta
    //    @SuppressWarnings("rawtypes")
    //    @IntermediateOp
    //    public Seq<T, Exception> onEachE(final Throwables.Consumer<? super T, ? extends Exception> action) throws IllegalStateException {
    //        assertNotClosed();
    //
    //        return create(new Throwables.Iterator<T, Exception>() {
    //            @Override
    //            public boolean hasNext() throws Exception {
    //                return elements.hasNext();
    //            }
    //
    //            @Override
    //            public T next() throws Exception {
    //                final T next = elements.next();
    //                action.accept(next);
    //                return next;
    //            }
    //        }, sorted, cmp, (Deque) closeHandlers);
    //    }

    /**
     * Registers a close handler to be invoked when the sequence is closed.
     * This method can be called multiple times to register multiple handlers.
     * Handlers are invoked in the order they were added, first-added, first-invoked.
     *
     * @param closeHandler the Runnable to be executed when the sequence is closed
     * @return a sequence with the close handler set
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified closeHandler is null
     */
    @IntermediateOp
    public Seq<T, E> onClose(final Runnable closeHandler) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(closeHandler, cs.closeHandler);

        if (isEmptyCloseHandler(closeHandler)) {
            return this;
        }

        final Deque<LocalRunnable> newCloseHandlers = new ArrayDeque<>(N.size(closeHandlers) + 1);

        if (N.notEmpty(closeHandlers)) {
            newCloseHandlers.addAll(closeHandlers);
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

        return create(elements, newCloseHandlers);
    }

    /**
     * Closes the sequence.
     * This method is synchronized to prevent concurrent modifications.
     * It is automatically called inside terminal operations to ensure resources are released.
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

    Throwables.Iterator<T, E> iteratorEx() {
        return elements;
    }

    private static <T, E extends Exception> Seq<T, E> create(final Throwables.Iterator<? extends T, ? extends E> iter) {
        return create(iter, iter::close);
    }

    private static <T, E extends Exception> Seq<T, E> create(final Throwables.Iterator<? extends T, ? extends E> iter, final LocalRunnable closeHandler) {
        final Deque<LocalRunnable> closeHandlers = new LocalArrayDeque<>();
        closeHandlers.add(closeHandler);

        return create(iter, closeHandlers);
    }

    private static <T, E extends Exception> Seq<T, E> create(final Throwables.Iterator<? extends T, ? extends E> iter,
            final Collection<LocalRunnable> closeHandlers) {
        return new Seq<>(iter, closeHandlers);
    }

    private static <T, E extends Exception> Seq<T, E> create(final Throwables.Iterator<T, E> iter, final boolean sorted, final Comparator<? super T> comparator,
            final Collection<LocalRunnable> closeHandlers) {
        return new Seq<>(iter, sorted, comparator, closeHandlers);
    }

    private static <T, E extends Exception> Seq<T, E> create(final Stream<? extends T> stream, final boolean tryToGetOriginalException) {
        if (stream == null) {
            return empty();
        }

        Throwables.Iterator<T, E> iter = null;

        // Try to get original exception if it is wrapped by SPS operation.
        if (tryToGetOriginalException) {
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
                    } catch (final Exception e) {
                        throw (E) ExceptionUtil.tryToGetOriginalCheckedException(e);
                    }
                }

                public T next() throws E {
                    try {
                        if (!isInitialized) {
                            init();
                        }

                        return iter.next();
                    } catch (final Exception e) {
                        throw (E) ExceptionUtil.tryToGetOriginalCheckedException(e);
                    }
                }

                @Override
                public void advance(long n) throws E {
                    try {
                        if (iter == null) {
                            s = s.skip(n);
                        } else {
                            while (n-- > 0 && hasNext()) {
                                next();
                            }
                        }
                    } catch (final Exception e) {
                        throw (E) ExceptionUtil.tryToGetOriginalCheckedException(e);
                    }
                }

                @Override
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
                    } catch (final Exception e) {
                        throw (E) ExceptionUtil.tryToGetOriginalCheckedException(e);
                    }
                }

                @Override
                protected void closeResource() {
                    s.close();
                }

                @SuppressWarnings("deprecation")
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

                @Override
                public void advance(long n) throws E {
                    if (iter == null) {
                        s = s.skip(n);
                    } else {
                        while (n-- > 0 && hasNext()) {
                            next();
                        }
                    }
                }

                @Override
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

                @Override
                protected void closeResource() {
                    s.close();
                }

                @SuppressWarnings("deprecation")
                private void init() {
                    if (!isInitialized) {
                        isInitialized = true;
                        iter = stream.iterator();
                    }
                }
            };
        }

        return create(iter).onClose(newCloseHandler(iter));
    }

    private static <T, E extends Exception> Throwables.Iterator<T, E> iterate(final Seq<? extends T, E> s) {
        return s == null ? Throwables.Iterator.empty() : (Throwables.Iterator<T, E>) s.iteratorEx();
    }

    private ObjIteratorEx<T> newObjIteratorEx(final Throwables.Iterator<T, E> elements) {
        return new ObjIteratorEx<>() {
            public boolean hasNext() {
                try {
                    return elements.hasNext();
                } catch (final Exception e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            }

            public T next() { // NOSONAR
                try {
                    return elements.next();
                } catch (final Exception e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            }

            @Override
            public void advance(final long n) {
                try {
                    elements.advance(n);
                } catch (final Exception e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            }

            @Override
            public long count() {
                try {
                    return elements.count();
                } catch (final Exception e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            }

            @Override
            public void close() {
                elements.close();
            }
        };
    }

    private void assertNotClosed() {
        if (isClosed) {
            throw new IllegalStateException("This stream is already terminated.");
        }
    }

    private void checkArgPositive(final int arg, final String argNameOrErrorMsg) {
        if (arg <= 0) {
            try {
                N.checkArgPositive(arg, argNameOrErrorMsg);
            } finally {
                close();
            }
        }
    }

    private void checkArgPositive(final long arg, final String argNameOrErrorMsg) {
        if (arg <= 0) {
            try {
                N.checkArgPositive(arg, argNameOrErrorMsg);
            } finally {
                close();
            }
        }
    }

    private void checkArgPositive(final double arg, final String argNameOrErrorMsg) {
        if (arg <= 0) {
            try {
                N.checkArgPositive(arg, argNameOrErrorMsg);
            } finally {
                close();
            }
        }

    }

    private void checkArgNotNegative(final int arg, final String argNameOrErrorMsg) {
        if (arg < 0) {
            try {
                N.checkArgNotNegative(arg, argNameOrErrorMsg);
            } finally {
                close();
            }
        }
    }

    private void checkArgNotNegative(final long arg, final String argNameOrErrorMsg) {
        if (arg < 0) {
            try {
                N.checkArgNotNegative(arg, argNameOrErrorMsg);
            } finally {
                close();
            }
        }
    }

    @SuppressFBWarnings("NP_LOAD_OF_KNOWN_NULL_VALUE")
    private void checkArgNotNull(final Object obj, final String errorMessage) {
        if (obj == null) {
            try {
                //noinspection ConstantValue
                N.checkArgNotNull(obj, errorMessage);
            } finally {
                close();
            }
        }
    }

    private void checkArgNotEmpty(final Collection<?> c, final String errorMessage) {
        if (c == null || c.size() == 0) {
            try {
                N.checkArgNotEmpty(c, errorMessage);
            } finally {
                close();
            }
        }
    }

    private void checkArgument(final boolean b, final String errorMessage) {
        if (!b) {
            try {
                //noinspection ConstantValue,DataFlowIssue
                N.checkArgument(b, errorMessage);
            } finally {
                close();
            }
        }
    }

    private void checkArgument(final boolean b, final String errorMessageTemplate, final int p1, final int p2) {
        if (!b) {
            try {
                //noinspection ConstantValue,DataFlowIssue
                N.checkArgument(b, errorMessageTemplate, p1, p2);
            } finally {
                close();
            }
        }
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
                                throw new IllegalStateException("Can't run NoCachingNoUpdating Objects in parallel Stream or Queue");
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
                } catch (final Exception e) {
                    setError(eHolder, e, onGoing);
                } finally {
                    if ((threadCounter.decrementAndGet() == 0) && (hasMore != null)) {
                        hasMore.setFalse();
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
                } catch (final Exception e) {
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

                final T result = next == NONE ? null : next;
                next = null;

                return result;
            }

            @Override
            protected void closeResource() {
                if (isClosed) {
                    return;
                }

                isClosed = true;
                onGoing.setFalse();
            }
        };
    }

    private static <K, V, E extends Exception> void merge(final Map<K, V> map, final K key, final V value,
            final Throwables.BinaryOperator<V, E> remappingFunction) throws E {
        final V oldValue = map.get(key);

        if (oldValue == null && !map.containsKey(key)) {
            map.put(key, value);
        } else {
            map.put(key, remappingFunction.apply(oldValue, value));
        }
    }

    private static void close(final Collection<? extends Runnable> closeHandlers) {
        Exception ex = null;

        for (final Runnable closeHandler : closeHandlers) {
            try {
                closeHandler.run();
            } catch (final Exception e) {
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
    private static void close(final Holder<? extends Seq> holder) {
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
    private static LocalRunnable newCloseHandler(final Collection<? extends Seq> c) {
        if (N.isEmpty(c)) {
            return EMPTY_CLOSE_HANDLER;
        }

        boolean allEmptyHandlers = true;

        for (final Seq s : c) {
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

            for (final Seq s : c) {
                if (s == null || s.isClosed || isEmptyCloseHandlers(s.closeHandlers)) {
                    continue;
                }

                try {
                    s.close();
                } catch (final Exception throwable) {
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

    private static void setError(final Holder<Throwable> errorHolder, final Throwable e) {
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (errorHolder) { //NOSONAR
            if (errorHolder.value() == null) {
                errorHolder.setValue(e);
            } else {
                errorHolder.value().addSuppressed(e);
            }
        }
    }

    private static void setError(final Holder<Throwable> errorHolder, final Throwable e, final MutableBoolean onGoing) {
        // Set error handle first, then set onGoing sign.
        // If onGoing sign is set first but error has not been set, threads may stop without throwing exception because errorHolder is empty.
        setError(errorHolder, e);

        onGoing.setFalse();
    }

    private static void setStopFlagAndThrowException(final Holder<Throwable> errorHolder, final MutableBoolean onGoing) {
        onGoing.setFalse();

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (errorHolder) { //NOSONAR
            if (errorHolder.value() != null) {
                throw toRuntimeException(errorHolder.getAndSet(null), false);
            }
        }
    }

    private static RuntimeException toRuntimeException(final Exception e) {
        return ExceptionUtil.toRuntimeException(e, true);
    }

    private static RuntimeException toRuntimeException(final Throwable e, final boolean throwIfItIsError) {
        return ExceptionUtil.toRuntimeException(e, true, throwIfItIsError);
    }

    private static boolean isSameComparator(final Comparator<?> a, final Comparator<?> b) {
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

    private static Object hashKey(final Object obj) {
        return obj == null ? NONE : (obj.getClass().isArray() ? Wrapper.of(obj) : obj);
    }

    @Internal
    private interface LocalRunnable extends Runnable {

        static LocalRunnable wrap(final Runnable closeHandler) {
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

        @Serial
        private static final long serialVersionUID = -97425473105100734L;

        public LocalArrayDeque() {
        }

        public LocalArrayDeque(final int initialCapacity) {
            super(initialCapacity);
        }

        public LocalArrayDeque(final Collection<? extends T> c) {
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
