/*
 * Copyright (C) 2019 HaiYang Li
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

package com.landawn.abacus.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Serial;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.security.SecureRandom;
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
import java.util.function.UnaryOperator;
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
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
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
 * A sequence class that represents a lazy, functional sequence of elements supporting both intermediate 
 * and terminal operations, similar to Java Streams but with support for checked exceptions.
 *
 * <p>The {@code Seq} class is designed to handle stream-like operations while allowing methods to throw 
 * checked exceptions, making it particularly useful for I/O operations, database queries, and other 
 * operations that may throw checked exceptions without requiring wrapping them in unchecked exceptions.
 *
 * <p>Sequences are lazy; computation on the source data is only performed when a terminal operation 
 * is initiated, and source elements are consumed only as needed. The sequence will be automatically 
 * closed after a terminal method is called or triggered.
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li>Supports checked exceptions in stream operations</li>
 *   <li>Lazy evaluation - operations are not executed until a terminal operation is called</li>
 *   <li>Auto-closeable - automatically closes resources when terminal operations complete</li>
 *   <li>Immutable - operations return new sequence instances</li>
 *   <li>Sequential processing only (not parallel)</li>
 *   <li>Easy transformation to standard Java {@link Stream} via {@code stream()} method</li>
 * </ul>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Basic sequence operations with exception handling
 * Seq<String, IOException> lines = Seq.lines(Paths.get("file.txt"))
 *     .filter(line -> !line.isEmpty())
 *     .map(String::trim);
 *
 * // Database operations without wrapping exceptions
 * Seq<User, SQLException> users = Seq.of(userIds)
 *     .map(id -> userDao.findById(id))  // May throw SQLException
 *     .filter(user -> user.isActive());
 *
 * // Conversion to standard Stream
 * Stream<String> stream = Seq.of("a", "b", "c").stream();
 * }</pre>
 *
 * <p><b>Operation Types:</b>
 * <ul>
 *   <li><b>Intermediate Operations:</b> Transform the sequence and return a new {@code Seq} 
 *       (e.g., {@code filter()}, {@code map()}, {@code sorted()})</li>
 *   <li><b>Terminal Operations:</b> Produce a result or side-effect and close the sequence 
 *       (e.g., {@code forEach()}, {@code collect()}, {@code toList()})</li>
 * </ul>
 *
 * @param <T> the type of elements in this sequence
 * @param <E> the type of checked exception that operations in this sequence may throw 
 *
 * @see com.landawn.abacus.util.Throwables
 * @see com.landawn.abacus.util.Fnn
 * @see com.landawn.abacus.util.Fn
 * @see com.landawn.abacus.util.stream.BaseStream
 * @see com.landawn.abacus.util.stream.Stream
 * @see com.landawn.abacus.util.stream.EntryStream
 * @see com.landawn.abacus.util.stream.IntStream
 * @see com.landawn.abacus.util.stream.LongStream
 * @see com.landawn.abacus.util.stream.DoubleStream
 * @see com.landawn.abacus.util.stream.Collectors
 * @see com.landawn.abacus.util.Comparators
 * @see com.landawn.abacus.util.ExceptionUtil
 * @see com.landawn.abacus.util.CSVUtil
 * @see java.util.stream.Stream
 */
@Beta
@LazyEvaluation
@SequentialOnly
@com.landawn.abacus.annotation.Immutable
@SuppressWarnings({ "java:S1192", "java:S1698", "java:S4968", "java:S6539" })
public final class Seq<T, E extends Exception> implements AutoCloseable, Immutable {
    private static final Logger logger = LoggerFactory.getLogger(Seq.class);

    // private static final int BATCH_SIZE_FOR_FLUSH = 1000;

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
    private Deque<LocalRunnable> closeHandlers;
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
     * Returns an empty {@code Seq} with no elements.
     * This is a static factory method that creates a new empty sequence each time it's called.
     *
     * @param <T> the type of elements (never actually contained since the sequence is empty)
     * @param <E> the type of exception that the sequence operations can throw
     * @return an empty {@code Seq}
     */
    public static <T, E extends Exception> Seq<T, E> empty() {
        return new Seq<>(Throwables.Iterator.empty());
    }

    /**
     * Returns a {@code Seq} that is lazily populated by the provided supplier.
     * The supplier is invoked only when a terminal operation is performed on the sequence.
     * This allows for deferred execution and lazy evaluation of the sequence creation.
     *
     * <br />
     * <p><b>Implementation Note:</b>
     * This is equivalent to: {@code Seq.just(supplier).flatMap(Supplier::get)}.</p>
     *
     * @param <T> the type of elements in the sequence
     * @param <E> the type of exception that the sequence operations can throw
     * @param supplier a supplier that provides the sequence when invoked. Must not be {@code null}.
     * @return a lazily populated {@code Seq}
     * @throws IllegalArgumentException if the supplier is null
     */
    public static <T, E extends Exception> Seq<T, E> defer(final Supplier<? extends Seq<? extends T, ? extends E>> supplier) throws IllegalArgumentException {
        N.checkArgNotNull(supplier, cs.supplier);

        final Supplier<? extends Seq<? extends T, ? extends E>> s = Fn.memoize(supplier);
        return Seq.<Supplier<? extends Seq<? extends T, ? extends E>>, E> just(s).flatMap(Supplier::get).onClose(newCloseHandler(s));
    }

    /**
     * Returns a {@code Seq} containing a single element.
     * This is a convenience method equivalent to {@link #of(Object...)}.
     *
     * @param <T> the type of the element
     * @param <E> the type of exception that the sequence operations can throw
     * @param e the single element to be contained in the sequence
     * @return a {@code Seq} containing the specified element
     */
    public static <T, E extends Exception> Seq<T, E> just(final T e) {
        return of(e);
    }

    /**
     * Returns a {@code Seq} containing a single element with the specified exception type.
     * This is a convenience method that allows explicit specification of the exception type
     * even though the actual creation logic is the same as {@link #just(Object)}.
     *
     * @param <T> the type of the element
     * @param <E> the type of exception that the sequence operations can throw
     * @param e the single element to be contained in the sequence
     * @param exceptionType the class of exception type (used for type inference only)
     * @return a {@code Seq} containing the specified element
     */
    public static <T, E extends Exception> Seq<T, E> just(final T e, @SuppressWarnings("unused") final Class<E> exceptionType) { //NOSONAR
        return of(e);
    }

    /**
     * Returns a {@code Seq} containing the specified element if it is not {@code null},
     * otherwise returns an empty {@code Seq}.
     *
     * @param <T> the type of the element
     * @param <E> the type of exception that the sequence operations can throw
     * @param e the element to be contained in the sequence if not null
     * @return a {@code Seq} containing the element if not {@code null}, otherwise an empty {@code Seq}
     */
    public static <T, E extends Exception> Seq<T, E> ofNullable(final T e) {
        if (e == null) {
            return empty();
        }

        return of(e);
    }

    /**
     * Returns a {@code Seq} containing the specified element if it is not {@code null},
     * otherwise returns an empty {@code Seq}. This method allows explicit specification
     * of the exception type.
     *
     * @param <T> the type of the element
     * @param <E> the type of exception that the sequence operations can throw
     * @param e the element to be contained in the sequence if not null
     * @param exceptionType the class of exception type (used for type inference only)
     * @return a {@code Seq} containing the element if not {@code null}, otherwise an empty {@code Seq}
     */
    public static <T, E extends Exception> Seq<T, E> ofNullable(final T e, @SuppressWarnings("unused") final Class<E> exceptionType) { //NOSONAR
        if (e == null) {
            return empty();
        }

        return of(e);
    }

    /**
     * Returns a {@code Seq} containing the specified elements.
     * If the array is {@code null} or empty, an empty sequence is returned.
     *
     * @param <T> the type of elements in the sequence
     * @param <E> the type of exception that the sequence operations can throw
     * @param a the array of elements to be contained in the sequence
     * @return a {@code Seq} containing the specified elements
     * @see #just(Object)
     * @see #just(Object, Class)
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
                if (n <= 0) {
                    return;
                }

                if (n > len - position) {
                    position = len;
                } else {
                    position += (int) n;
                }
            }
        });
    }

    /**
     * Returns a {@code Seq} containing the elements from the specified boolean array.
     * If the array is {@code null} or empty, an empty sequence is returned.
     * Each boolean value is boxed into a Boolean object.
     *
     * @param <E> the type of exception that the sequence operations can throw
     * @param a the boolean array to create the sequence from
     * @return a {@code Seq<Boolean, E>} containing the elements from the array
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
                if (n <= 0) {
                    return;
                }

                if (n > len - position) {
                    position = len;
                } else {
                    position += (int) n;
                }
            }
        });
    }

    /**
     * Returns a {@code Seq} containing the elements from the specified char array.
     * If the array is {@code null} or empty, an empty sequence is returned.
     * Each char value is boxed into a Character object.
     *
     * @param <E> the type of exception that the sequence operations can throw
     * @param a the char array to create the sequence from
     * @return a {@code Seq<Character, E>} containing the elements from the array
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
                if (n <= 0) {
                    return;
                }

                if (n > len - position) {
                    position = len;
                } else {
                    position += (int) n;
                }
            }
        });
    }

    /**
     * Returns a {@code Seq} containing the elements from the specified byte array.
     * If the array is {@code null} or empty, an empty sequence is returned.
     * Each byte value is boxed into a Byte object.
     *
     * @param <E> the type of exception that the sequence operations can throw
     * @param a the byte array to create the sequence from
     * @return a {@code Seq<Byte, E>} containing the elements from the array
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
                if (n <= 0) {
                    return;
                }

                if (n > len - position) {
                    position = len;
                } else {
                    position += (int) n;
                }
            }
        });
    }

    /**
     * Returns a {@code Seq} containing the elements from the specified short array.
     * If the array is {@code null} or empty, an empty sequence is returned.
     * Each short value is boxed into a Short object.
     *
     * @param <E> the type of exception that the sequence operations can throw
     * @param a the short array to create the sequence from
     * @return a {@code Seq<Short, E>} containing the elements from the array
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
                if (n <= 0) {
                    return;
                }

                if (n > len - position) {
                    position = len;
                } else {
                    position += (int) n;
                }
            }
        });
    }

    /**
     * Returns a {@code Seq} containing the elements from the specified int array.
     * If the array is {@code null} or empty, an empty sequence is returned.
     * Each int value is boxed into an Integer object.
     *
     * @param <E> the type of exception that the sequence operations can throw
     * @param a the int array to create the sequence from
     * @return a {@code Seq<Integer, E>} containing the elements from the array
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
                if (n <= 0) {
                    return;
                }

                if (n > len - position) {
                    position = len;
                } else {
                    position += (int) n;
                }
            }
        });
    }

    /**
     * Returns a {@code Seq} containing the elements from the specified long array.
     * If the array is {@code null} or empty, an empty sequence is returned.
     * Each long value is boxed into a Long object.
     *
     * @param <E> the type of exception that the sequence operations can throw
     * @param a the long array to create the sequence from
     * @return a {@code Seq<Long, E>} containing the elements from the array
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
                if (n <= 0) {
                    return;
                }

                if (n > len - position) {
                    position = len;
                } else {
                    position += (int) n;
                }
            }
        });
    }

    /**
     * Returns a {@code Seq} containing the elements from the specified float array.
     * If the array is {@code null} or empty, an empty sequence is returned.
     * Each float value is boxed into a Float object.
     *
     * @param <E> the type of exception that the sequence operations can throw
     * @param a the float array to create the sequence from
     * @return a {@code Seq<Float, E>} containing the elements from the array
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
                if (n <= 0) {
                    return;
                }

                if (n > len - position) {
                    position = len;
                } else {
                    position += (int) n;
                }
            }
        });
    }

    /**
     * Returns a {@code Seq} containing the elements from the specified double array.
     * If the array is {@code null} or empty, an empty sequence is returned.
     * Each double value is boxed into a Double object.
     *
     * @param <E> the type of exception that the sequence operations can throw
     * @param a the double array to create the sequence from
     * @return a {@code Seq<Double, E>} containing the elements from the array
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
                if (n <= 0) {
                    return;
                }

                if (n > len - position) {
                    position = len;
                } else {
                    position += (int) n;
                }
            }
        });
    }

    /**
     * Returns a {@code Seq} containing the value from the specified Optional if present,
     * otherwise returns an empty {@code Seq}.
     *
     * @param <T> the type of the value in the Optional
     * @param <E> the type of exception that the sequence operations can throw
     * @param op the Optional to create the sequence from
     * @return a {@code Seq} containing the Optional value if present, otherwise empty
     */
    public static <T, E extends Exception> Seq<T, E> of(final Optional<T> op) {
        return op == null || op.isEmpty() ? Seq.empty() : Seq.of(op.get()); //NOSONAR
    }

    /**
     * Returns a {@code Seq} containing the value from the specified java.util.Optional if present,
     * otherwise returns an empty {@code Seq}.
     *
     * @param <T> the type of the value in the Optional
     * @param <E> the type of exception that the sequence operations can throw
     * @param op the java.util.Optional to create the sequence from
     * @return a {@code Seq} containing the Optional value if present, otherwise empty
     */
    public static <T, E extends Exception> Seq<T, E> of(final java.util.Optional<T> op) {
        return op == null || op.isEmpty() ? Seq.empty() : Seq.of(op.get()); //NOSONAR
    }

    /**
     * Returns a {@code Seq} containing all elements from the specified Iterable.
     * If the Iterable is {@code null}, an empty sequence is returned.
     *
     * @param <T> the type of elements in the Iterable
     * @param <E> the type of exception that the sequence operations can throw
     * @param iterable the Iterable to create the sequence from
     * @return a {@code Seq} containing all elements from the Iterable
     */
    public static <T, E extends Exception> Seq<T, E> of(final Iterable<? extends T> iterable) {
        if (iterable == null) {
            return empty();
        }

        return of(iterable.iterator());
    }

    /**
     * Returns a {@code Seq} containing all elements from the specified Iterable.
     * This method allows explicit specification of the exception type.
     * If the Iterable is {@code null}, an empty sequence is returned.
     *
     * @param <T> the type of elements in the Iterable
     * @param <E> the type of exception that the sequence operations can throw
     * @param iterable the Iterable to create the sequence from
     * @param exceptionType the class of exception type (used for type inference only)
     * @return a {@code Seq} containing all elements from the Iterable
     */
    public static <T, E extends Exception> Seq<T, E> of(final Iterable<? extends T> iterable, @SuppressWarnings("unused") final Class<E> exceptionType) { //NOSONAR
        return of(iterable);
    }

    /**
     * Returns a {@code Seq} containing all elements from the specified Iterator.
     * If the Iterator is {@code null}, an empty sequence is returned.
     *
     * @param <T> the type of elements in the Iterator
     * @param <E> the type of exception that the sequence operations can throw
     * @param iter the Iterator to create the sequence from
     * @return a {@code Seq} containing all elements from the Iterator
     */
    public static <T, E extends Exception> Seq<T, E> of(final Iterator<? extends T> iter) {
        if (iter == null) {
            return empty();
        }

        return create(Throwables.Iterator.<T, E> of(iter));
    }

    /**
     * Returns a {@code Seq} containing all elements from the specified Throwables.Iterator.
     * If the Iterator is {@code null}, an empty sequence is returned.
     *
     * @param <T> the type of elements in the Iterator
     * @param <E> the type of exception that the sequence operations can throw
     * @param iter the Throwables.Iterator to create the sequence from
     * @return a {@code Seq} containing all elements from the Iterator
     */
    public static <T, E extends Exception> Seq<T, E> of(final Throwables.Iterator<? extends T, ? extends E> iter) {
        if (iter == null) {
            return empty();
        }

        return create(iter);
    }

    /**
     * Returns a {@code Seq} containing all elements from the specified Iterator.
     * This method allows explicit specification of the exception type.
     * If the Iterator is {@code null}, an empty sequence is returned.
     *
     * @param <T> the type of elements in the Iterator
     * @param <E> the type of exception that the sequence operations can throw
     * @param iter the Iterator to create the sequence from
     * @param exceptionType the class of exception type (used for type inference only)
     * @return a {@code Seq} containing all elements from the Iterator
     */
    public static <T, E extends Exception> Seq<T, E> of(final Iterator<? extends T> iter, @SuppressWarnings("unused") final Class<E> exceptionType) { //NOSONAR
        return of(iter);
    }

    /**
     * Returns a {@code Seq} containing all elements from the specified Enumeration.
     * If the Enumeration is {@code null}, an empty sequence is returned.
     *
     * @param <T> the type of elements in the Enumeration
     * @param <E> the type of exception that the sequence operations can throw
     * @param enumeration the Enumeration to create the sequence from
     * @return a {@code Seq} containing all elements from the Enumeration
     */
    public static <T, E extends Exception> Seq<T, E> of(final Enumeration<? extends T> enumeration) {
        if (enumeration == null) {
            return empty();
        }

        return of(Enumerations.toIterator(enumeration));
    }

    /**
     * Returns a {@code Seq} containing all elements from the specified Enumeration.
     * This method allows explicit specification of the exception type.
     * If the Enumeration is {@code null}, an empty sequence is returned.
     *
     * @param <T> the type of elements in the Enumeration
     * @param <E> the type of exception that the sequence operations can throw
     * @param enumeration the Enumeration to create the sequence from
     * @param exceptionType the class of exception type (used for type inference only)
     * @return a {@code Seq} containing all elements from the Enumeration
     */
    public static <T, E extends Exception> Seq<T, E> of(final Enumeration<? extends T> enumeration, @SuppressWarnings("unused") final Class<E> exceptionType) { //NOSONAR
        return of(enumeration);
    }

    /**
     * Returns a {@code Seq} containing all entries from the specified Map.
     * If the Map is {@code null} or empty, an empty sequence is returned.
     *
     * @param <K> the type of keys in the Map
     * @param <V> the type of values in the Map
     * @param <E> the type of exception that the sequence operations can throw
     * @param m the Map to create the sequence from
     * @return a {@code Seq<Map.Entry<K, V>, E>} containing all entries from the Map
     */
    public static <K, V, E extends Exception> Seq<Map.Entry<K, V>, E> of(final Map<K, V> m) {
        if (N.isEmpty(m)) {
            return empty();
        }

        return of(m.entrySet());
    }

    /**
     * Returns a {@code Seq} containing all entries from the specified Map.
     * This method allows explicit specification of the exception type.
     * If the Map is {@code null} or empty, an empty sequence is returned.
     *
     * @param <K> the type of keys in the Map
     * @param <V> the type of values in the Map
     * @param <E> the type of exception that the sequence operations can throw
     * @param m the Map to create the sequence from
     * @param exceptionType the class of exception type (used for type inference only)
     * @return a {@code Seq<Map.Entry<K, V>, E>} containing all entries from the Map
     */
    public static <K, V, E extends Exception> Seq<Map.Entry<K, V>, E> of(final Map<K, V> m, @SuppressWarnings("unused") final Class<E> exceptionType) { //NOSONAR
        return of(m);
    }

    /**
     * Returns a {@code Seq} containing all keys from the specified Map.
     * If the Map is {@code null} or empty, an empty sequence is returned.
     *
     * @param <K> the type of keys in the Map
     * @param <E> the type of exception that the sequence operations can throw
     * @param map the Map whose keys will be used to create the sequence
     * @return a {@code Seq<K, E>} containing all keys from the Map
     */
    public static <K, E extends Exception> Seq<K, E> ofKeys(final Map<K, ?> map) {
        if (N.isEmpty(map)) {
            return empty();
        }

        return of(map.keySet());
    }

    /**
     * Returns a {@code Seq} containing keys from the specified Map where the corresponding
     * values satisfy the given predicate.
     * If the Map is {@code null} or empty, an empty sequence is returned.
     *
     * @param <K> the type of keys in the Map
     * @param <V> the type of values in the Map
     * @param <E> the type of exception that the sequence operations can throw
     * @param map the Map to filter
     * @param valueFilter the predicate to test the values
     * @return a {@code Seq<K, E>} containing keys whose values satisfy the predicate
     */
    public static <K, V, E extends Exception> Seq<K, E> ofKeys(final Map<K, V> map, final Throwables.Predicate<? super V, E> valueFilter) {
        if (N.isEmpty(map)) {
            return empty();
        }

        //noinspection resource
        return Seq.<K, V, E> of(map).filter(Fnn.testByValue(valueFilter)).map(Fnn.key());
    }

    /**
     * Returns a {@code Seq} containing keys from the specified Map where the key-value
     * pairs satisfy the given bi-predicate.
     * If the Map is {@code null} or empty, an empty sequence is returned.
     *
     * @param <K> the type of keys in the Map
     * @param <V> the type of values in the Map
     * @param <E> the type of exception that the sequence operations can throw
     * @param map the Map to filter
     * @param filter the bi-predicate to test the key-value pairs
     * @return a {@code Seq<K, E>} containing keys that satisfy the bi-predicate
     */
    public static <K, V, E extends Exception> Seq<K, E> ofKeys(final Map<K, V> map, final Throwables.BiPredicate<? super K, ? super V, E> filter) {
        if (N.isEmpty(map)) {
            return empty();
        }

        //noinspection resource
        return Seq.<K, V, E> of(map).filter(Fn.Entries.ep(filter)).map(Fnn.key());
    }

    /**
     * Returns a {@code Seq} containing all values from the specified Map.
     * If the Map is {@code null} or empty, an empty sequence is returned.
     *
     * @param <V> the type of values in the Map
     * @param <E> the type of exception that the sequence operations can throw
     * @param map the Map whose values will be used to create the sequence
     * @return a {@code Seq<V, E>} containing all values from the Map
     */
    public static <V, E extends Exception> Seq<V, E> ofValues(final Map<?, V> map) {
        if (N.isEmpty(map)) {
            return empty();
        }

        return of(map.values());
    }

    /**
     * Returns a {@code Seq} containing values from the specified Map where the corresponding
     * keys satisfy the given predicate.
     * If the Map is {@code null} or empty, an empty sequence is returned.
     *
     * @param <K> the type of keys in the Map
     * @param <V> the type of values in the Map
     * @param <E> the type of exception that the sequence operations can throw
     * @param map the Map to filter
     * @param keyFilter the predicate to test the keys
     * @return a {@code Seq<V, E>} containing values whose keys satisfy the predicate
     */
    public static <K, V, E extends Exception> Seq<V, E> ofValues(final Map<K, V> map, final Throwables.Predicate<? super K, E> keyFilter) {
        if (N.isEmpty(map)) {
            return empty();
        }

        //noinspection resource
        return Seq.<K, V, E> of(map).filter(Fnn.testByKey(keyFilter)).map(Fnn.value());
    }

    /**
     * Returns a {@code Seq} containing values from the specified Map where the key-value
     * pairs satisfy the given bi-predicate.
     * If the Map is {@code null} or empty, an empty sequence is returned.
     *
     * @param <K> the type of keys in the Map
     * @param <V> the type of values in the Map
     * @param <E> the type of exception that the sequence operations can throw
     * @param map the Map to filter
     * @param filter the bi-predicate to test the key-value pairs
     * @return a {@code Seq<V, E>} containing values that satisfy the bi-predicate
     */
    public static <K, V, E extends Exception> Seq<V, E> ofValues(final Map<K, V> map, final Throwables.BiPredicate<? super K, ? super V, E> filter) {
        if (N.isEmpty(map)) {
            return empty();
        }

        //noinspection resource
        return Seq.<K, V, E> of(map).filter(Fn.Entries.ep(filter)).map(Fnn.value());
    }

    /**
     * Returns a {@code Seq} containing the elements from the specified array in reverse order.
     *
     * @param <T> the type of elements in the array
     * @param <E> the type of exception that the sequence operations can throw
     * @param array the array whose elements will be reversed
     * @return a {@code Seq} containing the array elements in reverse order
     */
    public static <T, E extends Exception> Seq<T, E> ofReversed(final T[] array) {
        final int len = N.len(array);

        //noinspection resource
        return Seq.<E> range(0, len).map(idx -> array[len - idx - 1]);
    }

    /**
     * Returns a {@code Seq} containing the elements from the specified list in reverse order.
     *
     * @param <T> the type of elements in the list
     * @param <E> the type of exception that the sequence operations can throw
     * @param list the list whose elements will be reversed
     * @return a {@code Seq} containing the list elements in reverse order
     */
    public static <T, E extends Exception> Seq<T, E> ofReversed(final List<? extends T> list) {
        final int size = N.size(list);

        //noinspection resource
        return Seq.<E> range(0, size).map(idx -> list.get(size - idx - 1));
    }

    /**
     * Returns a {@code Seq} that repeats the given element for the specified number of times.
     * If n is 0, an empty sequence is returned.
     *
     * @param <T> the type of the element
     * @param <E> the type of exception that the sequence operations can throw
     * @param element the element to repeat
     * @param n the number of times to repeat the element
     * @return a {@code Seq} containing the element repeated n times
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
     * Returns a {@code Seq} containing a range of integers from startInclusive (inclusive)
     * to endExclusive (exclusive) with increment of 1.
     *
     * @param <E> the type of exception that the sequence operations can throw
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @return a {@code Seq<Integer, E>} containing the range of integers
     */
    @SuppressWarnings("deprecation")
    public static <E extends Exception> Seq<Integer, E> range(final int startInclusive, final int endExclusive) {
        //noinspection resource
        return of(IntStream.range(startInclusive, endExclusive).boxed().iterator());
    }

    /**
     * Returns a {@code Seq} containing a range of integers from startInclusive (inclusive)
     * to endExclusive (exclusive) with the specified increment.
     *
     * @param <E> the type of exception that the sequence operations can throw
     * @param startInclusive the starting value (inclusive)
     * @param endExclusive the ending value (exclusive)
     * @param by the increment value (can be negative but not zero)
     * @return a {@code Seq<Integer, E>} containing the range of integers
     */
    @SuppressWarnings("deprecation")
    public static <E extends Exception> Seq<Integer, E> range(final int startInclusive, final int endExclusive, final int by) {
        //noinspection resource
        return of(IntStream.range(startInclusive, endExclusive, by).boxed().iterator());
    }

    /**
     * Returns a {@code Seq} containing a range of integers from startInclusive (inclusive)
     * to endInclusive (inclusive) with increment of 1.
     *
     * @param <E> the type of exception that the sequence operations can throw
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @return a {@code Seq<Integer, E>} containing the range of integers
     */
    @SuppressWarnings("deprecation")
    public static <E extends Exception> Seq<Integer, E> rangeClosed(final int startInclusive, final int endInclusive) {
        //noinspection resource
        return of(IntStream.rangeClosed(startInclusive, endInclusive).boxed().iterator());
    }

    /**
     * Returns a {@code Seq} containing a range of integers from startInclusive (inclusive)
     * to endInclusive (inclusive) with the specified increment.
     *
     * @param <E> the type of exception that the sequence operations can throw
     * @param startInclusive the starting value (inclusive)
     * @param endInclusive the ending value (inclusive)
     * @param by the increment value (can be negative but not zero)
     * @return a {@code Seq<Integer, E>} containing the range of integers
     */
    @SuppressWarnings("deprecation")
    public static <E extends Exception> Seq<Integer, E> rangeClosed(final int startInclusive, final int endInclusive, final int by) {
        //noinspection resource
        return of(IntStream.rangeClosed(startInclusive, endInclusive, by).boxed().iterator());
    }

    /**
     * Splits the given character sequence into a sequence of strings based on the specified delimiter character.
     *
     * @param <E> the type of exception that the sequence operations can throw
     * @param str the character sequence to split
     * @param delimiter the delimiter character to use for splitting
     * @return a {@code Seq<String, E>} containing the split strings
     */
    public static <E extends Exception> Seq<String, E> split(final CharSequence str, final char delimiter) {
        return of(Splitter.with(delimiter).iterate(str));
    }

    /**
     * Splits the given character sequence into a sequence of strings based on the specified delimiter string.
     *
     * @param <E> the type of exception that the sequence operations can throw
     * @param str the character sequence to split
     * @param delimiter the delimiter string to use for splitting
     * @return a {@code Seq<String, E>} containing the split strings
     */
    public static <E extends Exception> Seq<String, E> split(final CharSequence str, final CharSequence delimiter) {
        return of(Splitter.with(delimiter).iterate(str));
    }

    /**
     * Splits the given character sequence into a sequence of strings based on the specified pattern.
     *
     * @param <E> the type of exception that the sequence operations can throw
     * @param str the character sequence to split
     * @param pattern the pattern to use for splitting
     * @return a {@code Seq<String, E>} containing the split strings
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
     * Line terminators recognized are line feed "\n" (LF), carriage return "\r" (CR),
     * and carriage return followed immediately by a line feed "\r\n" (CRLF).
     *
     * @param <E> the type of exception that the sequence operations can throw
     * @param str the string to split into lines
     * @return a {@code Seq<String, E>} containing the lines
     */
    public static <E extends Exception> Seq<String, E> splitToLines(final String str) {
        return of(lineSplitter.iterate(str));
    }

    /**
     * Splits the given string into a sequence of lines with optional trimming and omission of empty lines.
     * Line terminators recognized are line feed "\n" (LF), carriage return "\r" (CR),
     * and carriage return followed immediately by a line feed "\r\n" (CRLF).
     *
     * @param <E> the type of exception that the sequence operations can throw
     * @param str the string to split into lines
     * @param trim if {@code true}, trims whitespace from the beginning and end of each line
     * @param omitEmptyLines if {@code true}, omits empty lines from the result
     * @return a {@code Seq<String, E>} containing the processed lines
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * final int[] a = Array.rangeClosed(1, 7);
     * splitByChunkCount(7, 5, true, (fromIndex, toIndex) ->  copyOfRange(a, fromIndex, toIndex)); // [[1], [2], [3], [4, 5], [6, 7]]
     * splitByChunkCount(7, 5, false, (fromIndex, toIndex) ->  copyOfRange(a, fromIndex, toIndex)); // [[1, 2], [3, 4], [5], [6], [7]]
     * }</pre>
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
                public void advance(long n) {
                    if (n <= 0) {
                        return;
                    }

                    while (n-- > 0 && cursor < totalSize) {
                        cursor = cnt++ < smallerCount ? cursor + smallerSize : cursor + biggerSize;
                    }
                }

                @Override
                public long count() {
                    return count - cnt;
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
                public void advance(long n) {
                    if (n <= 0) {
                        return;
                    }

                    while (n-- > 0 && cursor < totalSize) {
                        cursor = cnt++ < biggerCount ? cursor + biggerSize : cursor + smallerSize;
                    }
                }

                @Override
                public long count() {
                    return count - cnt;
                }
            };
        }

        return Seq.of(iter);
    }

    /**
     * Creates a {@code Seq} that reads lines from the specified file using the default charset.
     *
     * @param file the file to read lines from
     * @return a {@code Seq<String, IOException>} containing the lines of the file
     */
    public static Seq<String, IOException> ofLines(final File file) {
        return ofLines(file, Charsets.DEFAULT);
    }

    /**
     * Creates a {@code Seq} that reads lines from the specified file using the given charset.
     *
     * @param file the file to read lines from
     * @param charset the charset to use for decoding the file
     * @return a {@code Seq<String, IOException>} containing the lines of the file
     * @throws IllegalArgumentException if the file is null
     */
    public static Seq<String, IOException> ofLines(final File file, final Charset charset) throws IllegalArgumentException {
        N.checkArgNotNull(file, cs.file);

        final Throwables.Iterator<String, IOException> iter = createLazyLineIterator(file, null, charset, null, true);

        return create(iter).onClose(newCloseHandler(iter)); //NOSONAR
    }

    /**
     * Creates a {@code Seq} that reads lines from the specified path using the default charset.
     *
     * @param path the path to read lines from
     * @return a {@code Seq<String, IOException>} containing the lines of the file
     */
    public static Seq<String, IOException> ofLines(final Path path) {
        return ofLines(path, Charsets.DEFAULT);
    }

    /**
     * Creates a {@code Seq} that reads lines from the specified path using the given charset.
     *
     * @param path the path to read lines from
     * @param charset the charset to use for decoding the file
     * @return a {@code Seq<String, IOException>} containing the lines of the file
     * @throws IllegalArgumentException if the path is null
     */
    public static Seq<String, IOException> ofLines(final Path path, final Charset charset) throws IllegalArgumentException {
        N.checkArgNotNull(path, cs.path);

        final Throwables.Iterator<String, IOException> iter = createLazyLineIterator(null, path, charset, null, true);

        return create(iter).onClose(newCloseHandler(iter));
    }

    /**
     * Creates a {@code Seq} that reads lines from the given Reader.
     * <br />
     * It's user's responsibility to close the input {@code reader} after the sequence is completed.
     *
     * @param reader the Reader to read lines from
     * @return a {@code Seq<String, IOException>} containing the lines read from the Reader
     * @throws IllegalArgumentException if the reader is null
     */
    public static Seq<String, IOException> ofLines(final Reader reader) throws IllegalArgumentException {
        return ofLines(reader, false);
    }

    /**
     * Creates a {@code Seq} that reads lines from the given Reader.
     *
     * @param reader the Reader to read lines from
     * @param closeReaderWhenStreamIsClosed if {@code true}, the input {@code Reader} will be closed when the sequence is closed
     * @return a {@code Seq<String, IOException>} containing the lines read from the Reader
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
     * Lists all files in the specified parent directory.
     * If the directory doesn't exist, an empty sequence is returned.
     * This method only lists the immediate children files in the directory (not recursive).
     *
     * @param parentPath the parent directory to list files from. Must not be {@code null}.
     * @return a {@code Seq<File, IOException>} containing all files in the directory.
     *         Returns an empty sequence if the directory doesn't exist or is not a directory.
     * @throws IllegalArgumentException if parentPath is {@code null}
     * @see #listFiles(File, boolean)
     * @see File#listFiles()
     */
    public static Seq<File, IOException> listFiles(final File parentPath) {
        if (!parentPath.exists()) {
            return empty();
        }

        return of(parentPath.listFiles());
    }

    /**
     * Lists all files in the specified parent directory with optional recursive traversal.
     * If recursively is {@code false}, only immediate children files are listed.
     * If recursively is {@code true}, all descendant files are listed in depth-first order.
     * Directories are included in the results when recursively is {@code true}.
     * The method uses a breadth-first traversal when recursive listing is enabled.
     *
     * @param parentPath the parent directory to list files from. Must not be {@code null}.
     * @param recursively if {@code true}, lists files recursively in all subdirectories; 
     *                    if {@code false}, lists only immediate children files
     * @return a sequence containing File objects representing the files in the parent directory.
     *         Returns an empty sequence if the parent directory doesn't exist or is not a directory.
     *         When recursive, the sequence includes both files and directories.
     * @throws IllegalArgumentException if parentPath is {@code null}
     * @see #listFiles(File)
     * @see File#listFiles()
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
     * This is an internal helper method used by various line reading methods.
     * The iterator lazily reads lines from the source as they are requested.
     * The BufferedReader is created lazily on first access and is properly closed when the iterator is closed.
     *
     * @param file the file to read lines from, can be {@code null} if path or reader is provided
     * @param path the path to read lines from, can be {@code null} if file or reader is provided
     * @param charset the charset to use for decoding the file or path. If {@code null}, uses the default charset
     * @param reader the reader to read lines from, can be {@code null} if file or path is provided
     * @param closeReader whether to close the reader when the sequence is closed. Only applicable when reader is provided
     * @return a lazy iterator for reading lines that properly handles resource cleanup
     * @throws IllegalArgumentException if all of file, path, and reader are {@code null}
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
     * The arrays are processed in the order they are provided, from left to right.
     * Null arrays in the varargs are skipped without throwing an exception.
     * The resulting sequence contains all elements from all {@code non-null} arrays in order.
     * This is a lazy operation - arrays are not accessed until the sequence is consumed.
     *
     * @param <T> the type of elements in the arrays
     * @param <E> the type of exception that might be thrown during sequence operations
     * @param a the arrays to be concatenated. Can be empty, contain {@code null} arrays, or be {@code null} itself
     * @return a sequence containing all elements from all provided arrays in order.
     *         Returns an empty sequence if no arrays are provided or all arrays are {@code null}
     * @see #concat(Iterable...)
     * @see #concat(Iterator...)
     * @see #concat(Seq...)
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
     * The Iterables are processed in the order they are provided, from left to right.
     * Null Iterables in the varargs are skipped without throwing an exception.
     * The resulting sequence contains all elements from all {@code non-null} Iterables in order.
     * This is a lazy operation - Iterables are not accessed until the sequence is consumed.
     *
     * @param <T> the type of elements in the Iterables
     * @param <E> the type of exception that might be thrown during sequence operations
     * @param a the Iterables to be concatenated. Can be empty, contain {@code null} Iterables, or be {@code null} itself
     * @return a sequence containing all elements from all provided Iterables in order.
     *         Returns an empty sequence if no Iterables are provided or all Iterables are {@code null}
     * @see #concat(Object[][])
     * @see #concat(Iterator...)
     * @see #concat(Seq...)
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
     * The Iterators are processed in the order they are provided, from left to right.
     * Null Iterators in the varargs are skipped without throwing an exception.
     * The resulting sequence contains all elements from all {@code non-null} Iterators in order.
     * This is a lazy operation - Iterators are not accessed until the sequence is consumed.
     * Note: The iterators are consumed as the sequence is iterated, so they cannot be reused.
     *
     * @param <T> the type of elements in the Iterators
     * @param <E> the type of exception that might be thrown during sequence operations
     * @param a the Iterators to be concatenated. Can be empty, contain {@code null} Iterators, or be {@code null} itself
     * @return a sequence containing all elements from all provided Iterators in order.
     *         Returns an empty sequence if no Iterators are provided or all Iterators are {@code null}
     * @see #concat(Object[][])
     * @see #concat(Iterable...)
     * @see #concat(Seq...)
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
     * The sequences are processed in the order they are provided, from left to right.
     * Null sequences in the varargs are skipped without throwing an exception.
     * All input sequences will be automatically closed when the returned sequence is closed.
     * This is a lazy operation - sequences are not accessed until the returned sequence is consumed.
     * The close handlers of all input sequences are combined into the returned sequence.
     *
     * @param <T> the type of elements in the sequences
     * @param <E> the type of exception that might be thrown during sequence operations
     * @param a the sequences to be concatenated. Can be empty, contain {@code null} sequences, or be {@code null} itself
     * @return a sequence containing all elements from all provided sequences in order.
     *         Returns an empty sequence if no sequences are provided or all sequences are {@code null}.
     *         The returned sequence will close all input sequences when it is closed.
     * @see #concat(Collection)
     */
    @SafeVarargs
    public static <T, E extends Exception> Seq<T, E> concat(final Seq<? extends T, E>... a) {
        if (N.isEmpty(a)) {
            return empty();
        }

        return concat(Array.asList(a));
    }

    /**
     * Concatenates a collection of sequences into a single sequence.
     * The sequences are processed in the order they appear in the collection.
     * Null sequences in the collection are skipped without throwing an exception.
     * All input sequences will be automatically closed when the returned sequence is closed.
     * This is a lazy operation - sequences are not accessed until the returned sequence is consumed.
     * As each sequence is exhausted during iteration, it is immediately closed before moving to the next sequence.
     *
     * @param <T> the type of elements in the sequences
     * @param <E> the type of exception that might be thrown during sequence operations
     * @param c the collection of sequences to be concatenated. Can be empty, contain {@code null} sequences, or be {@code null} itself
     * @return a sequence containing all elements from all provided sequences in order.
     *         Returns an empty sequence if the collection is empty or {@code null}.
     *         The returned sequence will close all input sequences when it is closed.
     * @see #concat(Seq...)
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
     * Zips two arrays into a single sequence by combining corresponding elements using the provided zip function.
     * The resulting sequence length is equal to the length of the shorter input array.
     * Elements are combined by applying the zip function to elements at the same index from both arrays.
     * This is a lazy operation - arrays are not accessed until the sequence is consumed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Integer[] a1 = {1, 2, 3};
     * String[] a2 = {"a", "b", "c", "d"};
     * Seq<String, Exception> result = Seq.zip(a1, a2, (n, s) -> n + s);
     * // Result: ["1a", "2b", "3c"]
     * }</pre>
     *
     * @param <A> the type of elements in the first array
     * @param <B> the type of elements in the second array
     * @param <T> the type of elements in the resulting sequence
     * @param <E> the type of exception that might be thrown by the zip function
     * @param a the first array to zip. Can be {@code null} or empty
     * @param b the second array to zip. Can be {@code null} or empty
     * @param zipFunction a function that combines elements from the two arrays. Must not be {@code null}.
     *                    Takes an element from the first array and an element from the second array,
     *                    and returns the combined result
     * @return a sequence of combined elements. The length equals the shorter array's length.
     *         Returns an empty sequence if either array is {@code null} or empty
     * @throws IllegalArgumentException if zipFunction is {@code null}
     * @see #zip(Object[], Object[], Object, Object, Throwables.BiFunction)
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
     * Zips three arrays into a single sequence by combining corresponding elements using the provided zip function.
     * The resulting sequence length is equal to the length of the shortest input array.
     * Elements are combined by applying the zip function to elements at the same index from all three arrays.
     * This is a lazy operation - arrays are not accessed until the sequence is consumed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Integer[] a1 = {1, 2, 3};
     * String[] a2 = {"a", "b", "c"};
     * Double[] a3 = {1.1, 2.2, 3.3, 4.4};
     * Seq<String, Exception> result = Seq.zip(a1, a2, a3, (n, s, d) -> n + s + d);
     * // Result: ["1a1.1", "2b2.2", "3c3.3"]
     * }</pre>
     *
     * @param <A> the type of elements in the first array
     * @param <B> the type of elements in the second array
     * @param <C> the type of elements in the third array
     * @param <T> the type of elements in the resulting sequence
     * @param <E> the type of exception that might be thrown by the zip function
     * @param a the first array to zip. Can be {@code null} or empty
     * @param b the second array to zip. Can be {@code null} or empty
     * @param c the third array to zip. Can be {@code null} or empty
     * @param zipFunction a function that combines elements from the three arrays. Must not be {@code null}.
     *                    Takes an element from each array and returns the combined result
     * @return a sequence of combined elements. The length equals the shortest array's length.
     *         Returns an empty sequence if any array is {@code null} or empty
     * @throws IllegalArgumentException if zipFunction is {@code null}
     * @see #zip(Object[], Object[], Throwables.BiFunction)
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
     * Zips two iterables into a single sequence by combining corresponding elements using the provided zip function.
     * The resulting sequence length is equal to the length of the shorter input iterable.
     * Elements are combined by applying the zip function to elements at the same position from both iterables.
     * This is a lazy operation - iterables are not accessed until the sequence is consumed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> list1 = Arrays.asList(1, 2, 3);
     * Set<String> set2 = new HashSet<>(Arrays.asList("a", "b", "c", "d"));
     * Seq<String, Exception> result = Seq.zip(list1, set2, (n, s) -> n + s);
     * // Result will contain 3 elements (length of shorter collection)
     * }</pre>
     *
     * @param <A> the type of elements in the first iterable
     * @param <B> the type of elements in the second iterable
     * @param <T> the type of elements in the resulting sequence
     * @param <E> the type of exception that might be thrown by the zip function
     * @param a the first iterable to zip. Can be {@code null} or empty
     * @param b the second iterable to zip. Can be {@code null} or empty
     * @param zipFunction a function that combines elements from the two iterables. Must not be {@code null}.
     *                    Takes an element from the first iterable and an element from the second iterable,
     *                    and returns the combined result
     * @return a sequence of combined elements. The length equals the shorter iterable's length.
     *         Returns an empty sequence if either iterable is {@code null} or empty
     * @throws IllegalArgumentException if zipFunction is {@code null}
     * @see #zip(Iterator, Iterator, Throwables.BiFunction)
     * @see N#zip(Iterable, Iterable, java.util.function.BiFunction)
     */
    public static <A, B, T, E extends Exception> Seq<T, E> zip(final Iterable<? extends A> a, final Iterable<? extends B> b,
            final Throwables.BiFunction<? super A, ? super B, ? extends T, ? extends E> zipFunction) {
        return zip(N.iterate(a), N.iterate(b), zipFunction);
    }

    /**
     * Zips three iterables into a single sequence by combining corresponding elements using the provided zip function.
     * The resulting sequence length is equal to the length of the shortest input iterable.
     * Elements are combined by applying the zip function to elements at the same position from all three iterables.
     * This is a lazy operation - iterables are not accessed until the sequence is consumed.
     *
     * @param <A> the type of elements in the first iterable
     * @param <B> the type of elements in the second iterable
     * @param <C> the type of elements in the third iterable
     * @param <T> the type of elements in the resulting sequence
     * @param <E> the type of exception that might be thrown by the zip function
     * @param a the first iterable to zip. Can be {@code null} or empty
     * @param b the second iterable to zip. Can be {@code null} or empty
     * @param c the third iterable to zip. Can be {@code null} or empty
     * @param zipFunction a function that combines elements from the three iterables. Must not be {@code null}.
     *                    Takes an element from each iterable and returns the combined result
     * @return a sequence of combined elements. The length equals the shortest iterable's length.
     *         Returns an empty sequence if any iterable is {@code null} or empty
     * @throws IllegalArgumentException if zipFunction is {@code null}
     * @see #zip(Iterable, Iterable, Throwables.BiFunction)
     * @see N#zip(Iterable, Iterable, Iterable, TriFunction)
     */
    public static <A, B, C, T, E extends Exception> Seq<T, E> zip(final Iterable<? extends A> a, final Iterable<? extends B> b, final Iterable<? extends C> c,
            final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends T, ? extends E> zipFunction) {
        return zip(N.iterate(a), N.iterate(b), N.iterate(c), zipFunction);
    }

    /**
     * Zips two iterators into a single sequence by combining corresponding elements using the provided zip function.
     * The resulting sequence length is equal to the length of the shorter input iterator.
     * Elements are combined by applying the zip function to elements pulled from both iterators in parallel.
     * This is a lazy operation - iterators are not consumed until the sequence is consumed.
     * Note: The iterators are consumed as the sequence is iterated, so they cannot be reused.
     *
     * @param <A> the type of elements in the first iterator
     * @param <B> the type of elements in the second iterator
     * @param <T> the type of elements in the resulting sequence
     * @param <E> the type of exception that might be thrown by the zip function
     * @param a the first iterator to zip. Can be {@code null} or empty
     * @param b the second iterator to zip. Can be {@code null} or empty
     * @param zipFunction a function that combines elements from the two iterators. Must not be {@code null}.
     *                    Takes an element from the first iterator and an element from the second iterator,
     *                    and returns the combined result
     * @return a sequence of combined elements. The length equals the shorter iterator's length.
     *         Returns an empty sequence if either iterator is {@code null} or empty
     * @throws IllegalArgumentException if zipFunction is {@code null}
     * @see #zip(Iterable, Iterable, Throwables.BiFunction)
     * @see Iterators#zip(Iterable, Iterable, BiFunction) 
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
     * Zips three iterators into a single sequence by combining corresponding elements using the provided zip function.
     * The resulting sequence length is equal to the length of the shortest input iterator.
     * Elements are combined by applying the zip function to elements pulled from all three iterators in parallel.
     * This is a lazy operation - iterators are not consumed until the sequence is consumed.
     * Note: The iterators are consumed as the sequence is iterated, so they cannot be reused.
     *
     * @param <A> the type of elements in the first iterator
     * @param <B> the type of elements in the second iterator
     * @param <C> the type of elements in the third iterator
     * @param <T> the type of elements in the resulting sequence
     * @param <E> the type of exception that might be thrown by the zip function
     * @param a the first iterator to zip. Can be {@code null} or empty
     * @param b the second iterator to zip. Can be {@code null} or empty
     * @param c the third iterator to zip. Can be {@code null} or empty
     * @param zipFunction a function that combines elements from the three iterators. Must not be {@code null}.
     *                    Takes an element from each iterator and returns the combined result
     * @return a sequence of combined elements. The length equals the shortest iterator's length.
     *         Returns an empty sequence if any iterator is {@code null} or empty
     * @throws IllegalArgumentException if zipFunction is {@code null}
     * @see #zip(Iterator, Iterator, Throwables.BiFunction)
     * @see Iterators#zip(Iterator, Iterator, Iterator, TriFunction) 
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
     * Zips two sequences into a single sequence by combining corresponding elements using the provided zip function.
     * The resulting sequence length is equal to the length of the shorter input sequence.
     * Elements are combined by applying the zip function to elements at the same position from both sequences.
     * All input sequences will be automatically closed when the returned sequence is closed.
     * This is a lazy operation - sequences are not consumed until the returned sequence is consumed.
     *
     * @param <A> the type of elements in the first sequence
     * @param <B> the type of elements in the second sequence
     * @param <T> the type of elements in the resulting sequence
     * @param <E> the type of exception that might be thrown by the zip function or sequence operations
     * @param a the first sequence to zip. Can be {@code null} or empty
     * @param b the second sequence to zip. Can be {@code null} or empty
     * @param zipFunction a function that combines elements from the two sequences. Must not be {@code null}.
     *                    Takes an element from the first sequence and an element from the second sequence,
     *                    and returns the combined result
     * @return a sequence of combined elements. The length equals the shorter sequence's length.
     *         Returns an empty sequence if either sequence is {@code null} or empty.
     *         The returned sequence will close both input sequences when it is closed.
     * @throws IllegalArgumentException if zipFunction is {@code null}
     * @see #zip(Seq, Seq, Object, Object, Throwables.BiFunction)
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
     * Zips three sequences into a single sequence by combining corresponding elements using the provided zip function.
     * The resulting sequence length is equal to the length of the shortest input sequence.
     * Elements are combined by applying the zip function to elements at the same position from all three sequences.
     * All input sequences will be automatically closed when the returned sequence is closed.
     * This is a lazy operation - sequences are not consumed until the returned sequence is consumed.
     *
     * @param <A> the type of elements in the first sequence
     * @param <B> the type of elements in the second sequence
     * @param <C> the type of elements in the third sequence
     * @param <T> the type of elements in the resulting sequence
     * @param <E> the type of exception that might be thrown by the zip function or sequence operations
     * @param a the first sequence to zip. Can be {@code null} or empty
     * @param b the second sequence to zip. Can be {@code null} or empty
     * @param c the third sequence to zip. Can be {@code null} or empty
     * @param zipFunction a function that combines elements from the three sequences. Must not be {@code null}.
     *                    Takes an element from each sequence and returns the combined result
     * @return a sequence of combined elements. The length equals the shortest sequence's length.
     *         Returns an empty sequence if any sequence is {@code null} or empty.
     *         The returned sequence will close all input sequences when it is closed.
     * @throws IllegalArgumentException if zipFunction is {@code null}
     * @see #zip(Seq, Seq, Throwables.BiFunction)
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
     * Zips two arrays into a single sequence by combining corresponding elements using the provided zip function.
     * If one array is shorter than the other, the provided default values are used for the missing elements.
     * The resulting sequence length is equal to the length of the longer input array.
     * This is a lazy operation - arrays are not accessed until the sequence is consumed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Integer[] a1 = {1, 2};
     * String[] a2 = {"a", "b", "c"};
     * Seq<String, Exception> result = Seq.zip(a1, a2, 0, "X", (n, s) -> n + s);
     * // Result: ["1a", "2b", "0c"] (0 is used for missing integer)
     * }</pre>
     *
     * @param <A> the type of elements in the first array
     * @param <B> the type of elements in the second array
     * @param <T> the type of elements in the resulting sequence
     * @param <E> the type of exception that might be thrown by the zip function
     * @param a the first array to zip. Can be {@code null} or empty
     * @param b the second array to zip. Can be {@code null} or empty
     * @param valueForNoneA the default value to use when the first array runs out of elements. Can be {@code null}
     * @param valueForNoneB the default value to use when the second array runs out of elements. Can be {@code null}
     * @param zipFunction a function that combines elements from the two arrays. Must not be {@code null}.
     *                    Takes an element (or default value) from each array and returns the combined result
     * @return a sequence of combined elements. The length equals the longer array's length
     * @throws IllegalArgumentException if zipFunction is {@code null}
     * @see #zip(Object[], Object[], Throwables.BiFunction)
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
     * Zips three arrays into a single sequence by combining corresponding elements using the provided zip function.
     * If one array is shorter than the others, the provided default values are used for the missing elements.
     * The resulting sequence length is equal to the length of the longest input array.
     * This is a lazy operation - arrays are not accessed until the sequence is consumed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Integer[] a1 = {1, 2};
     * String[] a2 = {"a", "b", "c"};
     * Double[] a3 = {1.1};
     * Seq<String, Exception> result = Seq.zip(a1, a2, a3, 0, "X", 0.0, (n, s, d) -> n + s + d);
     * // Result: ["1a1.1", "2b0.0", "0c0.0"]
     * }</pre>
     *
     * @param <A> the type of elements in the first array
     * @param <B> the type of elements in the second array
     * @param <C> the type of elements in the third array
     * @param <T> the type of elements in the resulting sequence
     * @param <E> the type of exception that might be thrown by the zip function
     * @param a the first array to zip. Can be {@code null} or empty
     * @param b the second array to zip. Can be {@code null} or empty
     * @param c the third array to zip. Can be {@code null} or empty
     * @param valueForNoneA the default value to use when the first array runs out of elements. Can be {@code null}
     * @param valueForNoneB the default value to use when the second array runs out of elements. Can be {@code null}
     * @param valueForNoneC the default value to use when the third array runs out of elements. Can be {@code null}
     * @param zipFunction a function that combines elements from the three arrays. Must not be {@code null}.
     *                    Takes an element (or default value) from each array and returns the combined result
     * @return a sequence of combined elements. The length equals the longest array's length
     * @throws IllegalArgumentException if zipFunction is {@code null}
     * @see #zip(Object[], Object[], Object[], Throwables.TriFunction)
     * @see N#zip(Object[], Object[], Object[], Object, Object, Object, TriFunction)
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
     * Zips two iterables into a single sequence by combining corresponding elements using the provided zip function.
     * If one iterable is shorter than the other, the provided default values are used for the missing elements.
     * The resulting sequence length is equal to the length of the longer input iterable.
     * This is a lazy operation - iterables are not accessed until the sequence is consumed.
     *
     * @param <A> the type of elements in the first iterable
     * @param <B> the type of elements in the second iterable
     * @param <T> the type of elements in the resulting sequence
     * @param <E> the type of exception that might be thrown by the zip function
     * @param a the first iterable to zip. Can be {@code null} or empty
     * @param b the second iterable to zip. Can be {@code null} or empty
     * @param valueForNoneA the default value to use when the first iterable runs out of elements. Can be {@code null}
     * @param valueForNoneB the default value to use when the second iterable runs out of elements. Can be {@code null}
     * @param zipFunction a function that combines elements from the two iterables. Must not be {@code null}.
     *                    Takes an element (or default value) from each iterable and returns the combined result
     * @return a sequence of combined elements. The length equals the longer iterable's length
     * @throws IllegalArgumentException if zipFunction is {@code null}
     * @see #zip(Iterable, Iterable, Throwables.BiFunction)
     * @see N#zip(Iterable, Iterable, Object, Object, java.util.function.BiFunction)
     */
    public static <A, B, T, E extends Exception> Seq<T, E> zip(final Iterable<? extends A> a, final Iterable<? extends B> b, final A valueForNoneA,
            final B valueForNoneB, final Throwables.BiFunction<? super A, ? super B, ? extends T, ? extends E> zipFunction) {
        return zip(N.iterate(a), N.iterate(b), valueForNoneA, valueForNoneB, zipFunction);
    }

    /**
     * Zips three iterables into a single sequence by combining corresponding elements using the provided zip function.
     * If one iterable is shorter than the others, the provided default values are used for the missing elements.
     * The resulting sequence length is equal to the length of the longest input iterable.
     * This is a lazy operation - iterables are not accessed until the sequence is consumed.
     *
     * @param <A> the type of elements in the first iterable
     * @param <B> the type of elements in the second iterable
     * @param <C> the type of elements in the third iterable
     * @param <T> the type of elements in the resulting sequence
     * @param <E> the type of exception that might be thrown by the zip function
     * @param a the first iterable to zip. Can be {@code null} or empty
     * @param b the second iterable to zip. Can be {@code null} or empty
     * @param c the third iterable to zip. Can be {@code null} or empty
     * @param valueForNoneA the default value to use when the first iterable runs out of elements. Can be {@code null}
     * @param valueForNoneB the default value to use when the second iterable runs out of elements. Can be {@code null}
     * @param valueForNoneC the default value to use when the third iterable runs out of elements. Can be {@code null}
     * @param zipFunction a function that combines elements from the three iterables. Must not be {@code null}.
     *                    Takes an element (or default value) from each iterable and returns the combined result
     * @return a sequence of combined elements. The length equals the longest iterable's length
     * @throws IllegalArgumentException if zipFunction is {@code null}
     * @see #zip(Iterable, Iterable, Iterable, Throwables.TriFunction)
     * @see N#zip(Iterable, Iterable, Iterable, Object, Object, Object, TriFunction)
     */
    public static <A, B, C, T, E extends Exception> Seq<T, E> zip(final Iterable<? extends A> a, final Iterable<? extends B> b, final Iterable<? extends C> c,
            final A valueForNoneA, final B valueForNoneB, final C valueForNoneC,
            final Throwables.TriFunction<? super A, ? super B, ? super C, ? extends T, ? extends E> zipFunction) {
        return zip(N.iterate(a), N.iterate(b), N.iterate(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    /**
     * Zips two iterators into a single sequence by combining corresponding elements using the provided zip function.
     * If one iterator is shorter than the other, the provided default values are used for the missing elements.
     * The resulting sequence length is equal to the length of the longer input iterator.
     * This is a lazy operation - iterators are not consumed until the sequence is consumed.
     * Note: The iterators are consumed as the sequence is iterated, so they cannot be reused.
     *
     * @param <A> the type of elements in the first iterator
     * @param <B> the type of elements in the second iterator
     * @param <T> the type of elements in the resulting sequence
     * @param <E> the type of exception that might be thrown by the zip function
     * @param a the first iterator to zip. Can be {@code null} or empty
     * @param b the second iterator to zip. Can be {@code null} or empty
     * @param valueForNoneA the default value to use when the first iterator runs out of elements. Can be {@code null}
     * @param valueForNoneB the default value to use when the second iterator runs out of elements. Can be {@code null}
     * @param zipFunction a function that combines elements from the two iterators. Must not be {@code null}.
     *                    Takes an element (or default value) from each iterator and returns the combined result
     * @return a sequence of combined elements. The length equals the longer iterator's length
     * @throws IllegalArgumentException if zipFunction is {@code null}
     * @see #zip(Iterator, Iterator, Throwables.BiFunction)
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
     * Zips three iterators into a single sequence by combining corresponding elements using the provided zip function.
     * If one iterator is shorter than the others, the provided default values are used for the missing elements.
     * The resulting sequence length is equal to the length of the longest input iterator.
     * This is a lazy operation - iterators are not consumed until the sequence is consumed.
     * Note: The iterators are consumed as the sequence is iterated, so they cannot be reused.
     *
     * @param <A> the type of elements in the first iterator
     * @param <B> the type of elements in the second iterator
     * @param <C> the type of elements in the third iterator
     * @param <T> the type of elements in the resulting sequence
     * @param <E> the type of exception that might be thrown by the zip function
     * @param a the first iterator to zip. Can be {@code null} or empty
     * @param b the second iterator to zip. Can be {@code null} or empty
     * @param c the third iterator to zip. Can be {@code null} or empty
     * @param valueForNoneA the default value to use when the first iterator runs out of elements. Can be {@code null}
     * @param valueForNoneB the default value to use when the second iterator runs out of elements. Can be {@code null}
     * @param valueForNoneC the default value to use when the third iterator runs out of elements. Can be {@code null}
     * @param zipFunction a function that combines elements from the three iterators. Must not be {@code null}.
     *                    Takes an element (or default value) from each iterator and returns the combined result
     * @return a sequence of combined elements. The length equals the longest iterator's length
     * @throws IllegalArgumentException if zipFunction is {@code null}
     * @see #zip(Iterator, Iterator, Iterator, Throwables.TriFunction)
     * @see N#zip(Iterable, Iterable, Iterable, Object, Object, Object, TriFunction)
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
     * Zips two sequences into a single sequence by combining corresponding elements using the provided zip function.
     * If one sequence is shorter than the other, the provided default values are used for the missing elements.
     * The resulting sequence length is equal to the length of the longer input sequence.
     * All input sequences will be automatically closed when the returned sequence is closed.
     * This is a lazy operation - sequences are not consumed until the returned sequence is consumed.
     *
     * @param <A> the type of elements in the first sequence
     * @param <B> the type of elements in the second sequence
     * @param <T> the type of elements in the resulting sequence
     * @param <E> the type of exception that might be thrown by the zip function or sequence operations
     * @param a the first sequence to zip. Can be {@code null} or empty
     * @param b the second sequence to zip. Can be {@code null} or empty
     * @param valueForNoneA the default value to use when the first sequence runs out of elements. Can be {@code null}
     * @param valueForNoneB the default value to use when the second sequence runs out of elements. Can be {@code null}
     * @param zipFunction a function that combines elements from the two sequences. Must not be {@code null}.
     *                    Takes an element (or default value) from each sequence and returns the combined result
     * @return a sequence of combined elements. The length equals the longer sequence's length.
     *         The returned sequence will close both input sequences when it is closed.
     * @throws IllegalArgumentException if zipFunction is {@code null}
     * @see #zip(Seq, Seq, Throwables.BiFunction)
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
     * Zips three sequences into a single sequence by combining corresponding elements using the provided zip function.
     * If one sequence is shorter than the others, the provided default values are used for the missing elements.
     * The resulting sequence length is equal to the length of the longest input sequence.
     * All input sequences will be automatically closed when the returned sequence is closed.
     * This is a lazy operation - sequences are not consumed until the returned sequence is consumed.
     *
     * @param <A> the type of elements in the first sequence
     * @param <B> the type of elements in the second sequence
     * @param <C> the type of elements in the third sequence
     * @param <T> the type of elements in the resulting sequence
     * @param <E> the type of exception that might be thrown by the zip function or sequence operations
     * @param a the first sequence to zip. Can be {@code null} or empty
     * @param b the second sequence to zip. Can be {@code null} or empty
     * @param c the third sequence to zip. Can be {@code null} or empty
     * @param valueForNoneA the default value to use when the first sequence runs out of elements. Can be {@code null}
     * @param valueForNoneB the default value to use when the second sequence runs out of elements. Can be {@code null}
     * @param valueForNoneC the default value to use when the third sequence runs out of elements. Can be {@code null}
     * @param zipFunction a function that combines elements from the three sequences. Must not be {@code null}.
     *                    Takes an element (or default value) from each sequence and returns the combined result
     * @return a sequence of combined elements. The length equals the longest sequence's length.
     *         The returned sequence will close all input sequences when it is closed.
     * @throws IllegalArgumentException if zipFunction is {@code null}
     * @see #zip(Seq, Seq, Seq, Throwables.TriFunction)
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
     * Merges two arrays into a single sequence by selecting elements based on the provided next selector function.
     * The next selector determines which element to take next from the two arrays.
     * The resulting sequence contains all elements from both arrays.
     *
     * <p>The merging process works as follows:</p>
     * <ul>
     *   <li>The function starts with cursors at the beginning of both arrays</li>
     *   <li>At each step, it calls the nextSelector with the current elements from both arrays</li>
     *   <li>If nextSelector returns MergeResult.TAKE_FIRST, the element from the first array is taken</li>
     *   <li>Otherwise, the element from the second array is taken</li>
     *   <li>When one array is exhausted, all remaining elements from the other array are included</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Integer[] arr1 = {1, 3, 5};
     * Integer[] arr2 = {2, 4, 6};
     * Seq<Integer, Exception> merged = Seq.merge(arr1, arr2, 
     *     (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
     * // Result: [1, 2, 3, 4, 5, 6]
     * }</pre>
     *
     * @param <T> the type of elements in the arrays and the resulting sequence
     * @param <E> the type of exception that might be thrown
     * @param a the first array to merge
     * @param b the second array to merge
     * @param nextSelector a function that takes two elements (one from each array) and returns MergeResult.TAKE_FIRST
     *                     to select the first element, or any other value to select the second element
     * @return a sequence containing all elements from both arrays in the order determined by the next selector.
     *         If either array is {@code null} or empty, returns a sequence containing the other array's elements
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
     * Merges three arrays into a single sequence by selecting elements based on the provided next selector function.
     * The arrays are merged pair-wise: first a and b are merged, then the result is merged with c.
     * The resulting sequence contains all elements from all three arrays.
     *
     * <p>This method performs a cascading merge operation:</p>
     * <ol>
     *   <li>First merges arrays a and b using the nextSelector</li>
     *   <li>Then merges the result with array c using the same nextSelector</li>
     * </ol>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Integer[] arr1 = {1, 4, 7};
     * Integer[] arr2 = {2, 5, 8};
     * Integer[] arr3 = {3, 6, 9};
     * Seq<Integer, Exception> merged = Seq.merge(arr1, arr2, arr3,
     *     (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
     * // Result: [1, 2, 3, 4, 5, 6, 7, 8, 9]
     * }</pre>
     *
     * @param <T> the type of elements in the arrays and the resulting sequence
     * @param <E> the type of exception that might be thrown
     * @param a the first array to merge
     * @param b the second array to merge
     * @param c the third array to merge
     * @param nextSelector a function that takes two elements and returns MergeResult.TAKE_FIRST
     *                     to select the first element, or any other value to select the second element
     * @return a sequence containing all elements from all three arrays in the order determined by the next selector
     * @see N#merge(Object[], Object[], java.util.function.BiFunction)
     */
    public static <T, E extends Exception> Seq<T, E> merge(final T[] a, final T[] b, final T[] c,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        //noinspection resource
        return mergeIterators(merge(a, b, nextSelector).iteratorEx(), Throwables.Iterator.of(N.iterate(c)), nextSelector);
    }

    /**
     * Merges two iterables into a single sequence by selecting elements based on the provided next selector function.
     * The next selector determines which element to take next from the two iterables.
     * The resulting sequence contains all elements from both iterables.
     *
     * <p>This method converts the iterables to iterators and delegates to the iterator-based merge method.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Integer> list1 = Arrays.asList(1, 3, 5);
     * Set<Integer> set2 = new HashSet<>(Arrays.asList(2, 4, 6));
     * Seq<Integer, Exception> merged = Seq.merge(list1, set2,
     *     (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
     * // Result: [1, 2, 3, 4, 5, 6]
     * }</pre>
     *
     * @param <T> the type of elements in the iterables and the resulting sequence
     * @param <E> the type of exception that might be thrown
     * @param a the first iterable to merge
     * @param b the second iterable to merge
     * @param nextSelector a function that takes two elements (one from each iterable) and returns MergeResult.TAKE_FIRST
     *                     to select the first element, or any other value to select the second element
     * @return a sequence containing all elements from both iterables in the order determined by the next selector
     * @see N#merge(Iterable, Iterable, java.util.function.BiFunction)
     */
    public static <T, E extends Exception> Seq<T, E> merge(final Iterable<? extends T> a, final Iterable<? extends T> b,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return merge(N.iterate(a), N.iterate(b), nextSelector);
    }

    /**
     * Merges three iterables into a single sequence by selecting elements based on the provided next selector function.
     * The iterables are merged pair-wise: first a and b are merged, then the result is merged with c.
     * The resulting sequence contains all elements from all three iterables.
     *
     * <p>This method performs a cascading merge operation by converting iterables to iterators.</p>
     *
     * @param <T> the type of elements in the iterables and the resulting sequence
     * @param <E> the type of exception that might be thrown
     * @param a the first iterable to merge
     * @param b the second iterable to merge
     * @param c the third iterable to merge
     * @param nextSelector a function that takes two elements and returns MergeResult.TAKE_FIRST
     *                     to select the first element, or any other value to select the second element
     * @return a sequence containing all elements from all three iterables in the order determined by the next selector
     * @see N#merge(Iterable, Iterable, java.util.function.BiFunction)
     */
    public static <T, E extends Exception> Seq<T, E> merge(final Iterable<? extends T> a, final Iterable<? extends T> b, final Iterable<? extends T> c,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return merge(N.iterate(a), N.iterate(b), N.iterate(c), nextSelector);
    }

    /**
     * Merges two iterators into a single sequence by selecting elements based on the provided next selector function.
     * The next selector determines which element to take next from the two iterators.
     * The resulting sequence contains all elements from both iterators.
     *
     * <p>This is the core merge implementation that uses a look-ahead mechanism to properly handle the merge logic.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Iterator<String> iter1 = Arrays.asList("apple", "cherry", "grape").iterator();
     * Iterator<String> iter2 = Arrays.asList("banana", "date", "fig").iterator();
     * Seq<String, Exception> merged = Seq.merge(iter1, iter2,
     *     (a, b) -> a.compareTo(b) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
     * // Result: ["apple", "banana", "cherry", "date", "fig", "grape"]
     * }</pre>
     *
     * @param <T> the type of elements in the iterators and the resulting sequence
     * @param <E> the type of exception that might be thrown
     * @param a the first iterator to merge
     * @param b the second iterator to merge
     * @param nextSelector a function that takes two elements (one from each iterator) and returns MergeResult.TAKE_FIRST
     *                     to select the first element, or any other value to select the second element
     * @return a sequence containing all elements from both iterators in the order determined by the next selector
     * @see N#merge(Iterable, Iterable, java.util.function.BiFunction)
     */
    public static <T, E extends Exception> Seq<T, E> merge(final Iterator<? extends T> a, final Iterator<? extends T> b,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return mergeIterators(Throwables.Iterator.<T, E> of(a), Throwables.Iterator.<T, E> of(b), nextSelector);
    }

    /**
     * Merges three iterators into a single sequence by selecting elements based on the provided next selector function.
     * The iterators are merged pair-wise: first a and b are merged, then the result is merged with c.
     * The resulting sequence contains all elements from all three iterators.
     *
     * <p>This method performs a cascading merge operation using the two-iterator merge as a building block.</p>
     *
     * @param <T> the type of elements in the iterators and the resulting sequence
     * @param <E> the type of exception that might be thrown
     * @param a the first iterator to merge
     * @param b the second iterator to merge
     * @param c the third iterator to merge
     * @param nextSelector a function that takes two elements and returns MergeResult.TAKE_FIRST
     *                     to select the first element, or any other value to select the second element
     * @return a sequence containing all elements from all three iterators in the order determined by the next selector
     * @see N#merge(Iterable, Iterable, java.util.function.BiFunction)
     */
    public static <T, E extends Exception> Seq<T, E> merge(final Iterator<? extends T> a, final Iterator<? extends T> b, final Iterator<? extends T> c,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        //noinspection resource
        return mergeIterators(merge(a, b, nextSelector).iteratorEx(), Throwables.Iterator.<T, E> of(c), nextSelector);
    }

    /**
     * Merges two sequences into a single sequence by selecting elements based on the provided next selector function.
     * The next selector determines which element to take next from the two sequences.
     * The resulting sequence contains all elements from both sequences.
     * All input sequences will be automatically closed when the returned sequence is closed.
     *
     * <p>This method ensures proper resource management by registering close handlers for both input sequences.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer, Exception> seq1 = Seq.of(1, 3, 5, 7);
     * Seq<Integer, Exception> seq2 = Seq.of(2, 4, 6, 8);
     * Seq<Integer, Exception> merged = Seq.merge(seq1, seq2,
     *     (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
     * // Result: [1, 2, 3, 4, 5, 6, 7, 8]
     * }</pre>
     *
     * @param <T> the type of elements in the sequences and the resulting sequence
     * @param <E> the type of exception that might be thrown
     * @param a the first sequence to merge
     * @param b the second sequence to merge
     * @param nextSelector a function that takes two elements (one from each sequence) and returns MergeResult.TAKE_FIRST
     *                     to select the first element, or any other value to select the second element
     * @return a sequence containing all elements from both sequences in the order determined by the next selector
     * @see N#merge(Iterable, Iterable, java.util.function.BiFunction)
     */
    public static <T, E extends Exception> Seq<T, E> merge(final Seq<? extends T, E> a, final Seq<? extends T, E> b,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return mergeIterators(iterate(a), iterate(b), nextSelector).onClose(newCloseHandler(Array.asList(a, b)));
    }

    /**
     * Merges three sequences into a single sequence by selecting elements based on the provided next selector function.
     * The sequences are merged pair-wise: first a and b are merged, then the result is merged with c.
     * The resulting sequence contains all elements from all three sequences.
     * All input sequences will be automatically closed when the returned sequence is closed.
     *
     * <p>This method performs a cascading merge and ensures proper resource management for all three sequences.</p>
     *
     * @param <T> the type of elements in the sequences and the resulting sequence
     * @param <E> the type of exception that might be thrown
     * @param a the first sequence to merge
     * @param b the second sequence to merge
     * @param c the third sequence to merge
     * @param nextSelector a function that takes two elements and returns MergeResult.TAKE_FIRST
     *                     to select the first element, or any other value to select the second element
     * @return a sequence containing all elements from all three sequences in the order determined by the next selector
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
     * Filters the elements of this sequence, returning a new sequence containing only the elements that satisfy the given predicate.
     * This is an intermediate operation that does not consume the sequence.
     *
     * <p>The predicate is tested against each element, and only elements for which the predicate returns {@code true}
     * are included in the resulting sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer, Exception> numbers = Seq.of(1, 2, 3, 4, 5, 6);
     * Seq<Integer, Exception> evenNumbers = numbers.filter(n -> n % 2 == 0);
     * // Result: [2, 4, 6]
     * }</pre>
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
     * Filters the elements of this sequence, returning a new sequence containing only the elements that satisfy the given predicate.
     * Additionally, performs an action on each element that is dropped (filtered out).
     * This is an intermediate operation that does not consume the sequence.
     *
     * <p>This method is useful for debugging or tracking which elements are being filtered out.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String, Exception> names = Seq.of("Alice", "Bob", "Charlie", "David");
     * Seq<String, Exception> shortNames = names.filter(
     *     name -> name.length() <= 4,
     *     droppedName -> System.out.println("Filtered out: " + droppedName)
     * );
     * // Prints: "Filtered out: Alice", "Filtered out: Charlie"
     * // Result: ["Bob", "David"]
     * }</pre>
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
     * Takes elements from this sequence while the provided predicate returns {@code true}.
     * Stops taking elements as soon as the predicate returns {@code false} for the first time.
     * This is an intermediate operation that does not consume the sequence.
     *
     * <p>This operation is short-circuiting - it stops evaluating elements once the predicate returns {@code false}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer, Exception> numbers = Seq.of(1, 3, 5, 2, 7, 9);
     * Seq<Integer, Exception> result = numbers.takeWhile(n -> n % 2 == 1);
     * // Result: [1, 3, 5] (stops at 2 because it's even)
     * }</pre>
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
     * Drops (skips) elements from this sequence while the provided predicate returns {@code true}.
     * Starts including elements once the predicate returns {@code false} for the first time.
     * This is an intermediate operation that does not consume the sequence.
     *
     * <p>All elements after the first element that doesn't match the predicate are included,
     * regardless of whether they match the predicate or not.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer, Exception> numbers = Seq.of(1, 3, 5, 2, 7, 9, 4);
     * Seq<Integer, Exception> result = numbers.dropWhile(n -> n % 2 == 1);
     * // Result: [2, 7, 9, 4] (drops 1, 3, 5, then includes everything after 2)
     * }</pre>
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
     * Drops (skips) elements from this sequence while the provided predicate returns {@code true}.
     * Additionally, performs an action on each element that is dropped.
     * Starts including elements once the predicate returns {@code false} for the first time.
     * This is an intermediate operation that does not consume the sequence.
     *
     * <p>This method is useful for debugging or tracking which elements are being dropped.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String, Exception> words = Seq.of("a", "an", "the", "quick", "brown");
     * Seq<String, Exception> result = words.dropWhile(
     *     word -> word.length() <= 3,
     *     droppedWord -> System.out.println("Dropped: " + droppedWord)
     * );
     * // Prints: "Dropped: a", "Dropped: an", "Dropped: the"
     * // Result: ["quick", "brown"]
     * }</pre>
     *
     * @param predicate the predicate to apply to each element to determine if it should be dropped
     * @param actionOnDroppedItem the action to perform on items that are dropped
     * @return a new sequence containing the elements after the predicate becomes false
     * @throws IllegalStateException if the sequence is already closed
     */
    @Beta
    @IntermediateOp
    public Seq<T, E> dropWhile(final Throwables.Predicate<? super T, ? extends E> predicate,
            final Throwables.Consumer<? super T, ? extends E> actionOnDroppedItem) throws IllegalStateException {
        assertNotClosed();

        return dropWhile(value -> {
            if (predicate.test(value)) {
                actionOnDroppedItem.accept(value);
                return true;
            }

            return false;
        });
    }

    /**
     * Skips elements from this sequence until the provided predicate returns {@code true}.
     * This is equivalent to {@code dropWhile(not(predicate))}.
     * This is an intermediate operation that does not consume the sequence.
     *
     * <p>Elements are skipped until the first element that matches the predicate is found,
     * then that element and all subsequent elements are included.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer, Exception> numbers = Seq.of(2, 4, 6, 5, 7, 9);
     * Seq<Integer, Exception> result = numbers.skipUntil(n -> n % 2 == 1);
     * // Result: [5, 7, 9] (skips until finding the first odd number)
     * }</pre>
     *
     * @param predicate the predicate to apply to each element to determine when to stop skipping
     * @return a new sequence containing the elements after the predicate becomes true
     * @throws IllegalStateException if the sequence is already closed
     */
    @Beta
    @IntermediateOp
    public Seq<T, E> skipUntil(final Throwables.Predicate<? super T, ? extends E> predicate) throws IllegalStateException {
        return dropWhile(Fnn.not(predicate));
    }

    /**
     * Returns a sequence consisting of the distinct elements of this sequence.
     * Uses the natural equality (equals/hashCode) of elements to determine uniqueness.
     * This is an intermediate operation that does not consume the sequence.
     *
     * <p>The order of elements is preserved, with the first occurrence of each distinct element retained.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer, Exception> numbers = Seq.of(1, 2, 3, 2, 4, 3, 5);
     * Seq<Integer, Exception> distinct = numbers.distinct();
     * // Result: [1, 2, 3, 4, 5]
     * }</pre>
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
     * Returns a sequence consisting of the distinct elements of this sequence,
     * using the provided merge function to handle duplicates.
     * When duplicate elements are encountered, the merge function determines which element to keep.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the distinct operation.</p>
     *
     * <p>This method is equivalent to: {@code groupBy(Fnn.identity(), Fnn.identity(), mergeFunction).map(Fnn.value())}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String, Exception> words = Seq.of("hello", "HELLO", "world", "WORLD");
     * Seq<String, Exception> distinct = words.distinct((a, b) -> a.toUpperCase());
     * // Result: ["HELLO", "WORLD"] (keeps the uppercase versions)
     * }</pre>
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

    /**
     * Returns a sequence consisting of the distinct elements of this sequence,
     * where distinct elements are determined by the provided key extractor function.
     * Elements with the same key (as determined by the key extractor) are considered duplicates.
     * This is an intermediate operation that does not consume the sequence.
     *
     * <p>The order of elements is preserved, with the first occurrence of each distinct key retained.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Person {
     *     String name;
     *     int age;
     *     // constructor, getters...
     * }
     * 
     * Seq<Person, Exception> people = Seq.of(
     *     new Person("Alice", 30),
     *     new Person("Bob", 25),
     *     new Person("Alice", 35)
     * );
     * Seq<Person, Exception> distinctByName = people.distinctBy(Person::getName);
     * // Result: [Person("Alice", 30), Person("Bob", 25)]
     * }</pre>
     *
     * @param keyMapper the function to extract the key from the elements
     * @return a new sequence containing distinct elements based on the extracted keys
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public Seq<T, E> distinctBy(final Throwables.Function<? super T, ?, ? extends E> keyMapper) throws IllegalStateException {
        assertNotClosed();

        final Set<Object> set = N.newHashSet();

        return filter(value -> set.add(hashKey(keyMapper.apply(value))));
    }

    /**
     * Returns a sequence consisting of the distinct elements of this sequence,
     * where distinct elements are determined by the provided key extractor function.
     * If multiple elements have the same key, they are merged using the provided merge function.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the distinct operation.</p>
     *
     * <p>This method is equivalent to: {@code groupBy(keyMapper, Fnn.identity(), mergeFunction).map(Fnn.value())}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Sale {
     *     String product;
     *     int amount;
     *     // constructor, getters...
     * }
     * 
     * Seq<Sale, Exception> sales = Seq.of(
     *     new Sale("Apple", 10),
     *     new Sale("Banana", 5),
     *     new Sale("Apple", 15)
     * );
     * Seq<Sale, Exception> merged = sales.distinctBy(
     *     Sale::getProduct,
     *     (s1, s2) -> new Sale(s1.getProduct(), s1.getAmount() + s2.getAmount())
     * );
     * // Result: [Sale("Apple", 25), Sale("Banana", 5)]
     * }</pre>
     *
     * @param keyMapper the function to extract the key from the elements
     * @param mergeFunction the function to merge elements that have the same key
     * @return a new sequence containing distinct elements based on the extracted keys
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

    /**
     * Transforms each element of this sequence using the provided mapper function.
     * This is an intermediate operation that does not consume the sequence.
     *
     * <p>The mapper function is applied to each element lazily as elements are consumed from the resulting sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String, Exception> names = Seq.of("Alice", "Bob", "Charlie");
     * Seq<Integer, Exception> nameLengths = names.map(String::length);
     * // Result: [5, 3, 7]
     * 
     * Seq<Integer, Exception> numbers = Seq.of(1, 2, 3, 4);
     * Seq<String, Exception> strings = numbers.map(n -> "Number: " + n);
     * // Result: ["Number: 1", "Number: 2", "Number: 3", "Number: 4"]
     * }</pre>
     *
     * @param <R> the type of the elements in the new sequence
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
     * Transforms the {@code non-null} elements of this sequence using the provided mapper function, skipping {@code null} elements.
     * This is an intermediate operation that does not consume the sequence.
     *
     * <p>This method is equivalent to: {@code skipNulls().map(mapper)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String, Exception> names = Seq.of("Alice", null, "Bob", null, "Charlie");
     * Seq<Integer, Exception> nameLengths = names.mapIfNotNull(String::length);
     * // Result: [5, 3, 7] (nulls are skipped)
     * }</pre>
     *
     * @param <R> the type of the elements in the new sequence
     * @param mapper the function to apply to each {@code non-null} element
     * @return a new sequence containing the transformed {@code non-null} elements
     * @throws IllegalStateException if the sequence is already closed
     */
    @Beta
    @IntermediateOp
    public <R> Seq<R, E> mapIfNotNull(final Throwables.Function<? super T, ? extends R, ? extends E> mapper) throws IllegalStateException {
        //noinspection resource
        return skipNulls().map(mapper);
    }

    /**
     * Returns a sequence consisting of the elements of this sequence with the first element transformed by the given function.
     * All other elements remain unchanged.
     * This is an intermediate operation that does not consume the sequence.
     *
     * <p>If the sequence is empty, the returned sequence is also empty.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String, Exception> words = Seq.of("hello", "world", "java");
     * Seq<String, Exception> result = words.mapFirst(String::toUpperCase);
     * // Result: ["HELLO", "world", "java"]
     * 
     * Seq<Integer, Exception> numbers = Seq.of(1, 2, 3, 4);
     * Seq<Integer, Exception> result2 = numbers.mapFirst(n -> n * 100);
     * // Result: [100, 2, 3, 4]
     * }</pre>
     *
     * @param mapperForFirst a non-interfering, stateless function to apply to the first element of this sequence
     * @return a new sequence consisting of the transformed first element and the unchanged remaining elements of this sequence
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
     * Returns a sequence consisting of the elements of this sequence with the first element transformed by the specified {@code mapperForFirst}
     * and all other elements transformed by {@code mapperForElse}.
     * This is an intermediate operation that does not consume the sequence.
     *
     * <p>This allows different transformations to be applied to the first element versus all subsequent elements.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer, Exception> numbers = Seq.of(1, 2, 3, 4);
     * Seq<String, Exception> result = numbers.mapFirstOrElse(
     *     n -> "First: " + n,
     *     n -> "Other: " + n
     * );
     * // Result: ["First: 1", "Other: 2", "Other: 3", "Other: 4"]
     * }</pre>
     *
     * @param <R> the type of the result elements
     * @param mapperForFirst a non-interfering, stateless function to apply to the first element of this sequence
     * @param mapperForElse a non-interfering, stateless function to apply to all other elements of this sequence
     * @return a new sequence consisting of the results of applying the appropriate mapper function to each element
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
     * Returns a sequence consisting of the elements of this sequence with the last element transformed by the given function.
     * All other elements remain unchanged.
     * This is an intermediate operation that does not consume the sequence.
     *
     * <p>The implementation uses a one-element look-ahead buffer to detect the last element.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String, Exception> words = Seq.of("hello", "world", "java");
     * Seq<String, Exception> result = words.mapLast(s -> s + "!");
     * // Result: ["hello", "world", "java!"]
     * 
     * Seq<Integer, Exception> numbers = Seq.of(1, 2, 3, 4);
     * Seq<Integer, Exception> result2 = numbers.mapLast(n -> n * 100);
     * // Result: [1, 2, 3, 400]
     * }</pre>
     *
     * @param mapperForLast a non-interfering, stateless function to apply to the last element of this sequence
     * @return a new sequence consisting of the unchanged preceding elements and the transformed last element
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
     * Returns a sequence consisting of the elements of this sequence with the last element transformed by the specified {@code mapperForLast}
     * and all other elements transformed by {@code mapperForElse}.
     * This is an intermediate operation that does not consume the sequence.
     *
     * <p>This allows different transformations to be applied to the last element versus all preceding elements.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer, Exception> numbers = Seq.of(1, 2, 3, 4);
     * Seq<String, Exception> result = numbers.mapLastOrElse(
     *     n -> "Last: " + n,
     *     n -> "Other: " + n
     * );
     * // Result: ["Other: 1", "Other: 2", "Other: 3", "Last: 4"]
     * }</pre>
     *
     * @param <R> the type of the result elements
     * @param mapperForLast a non-interfering, stateless function to apply to the last element of this sequence
     * @param mapperForElse a non-interfering, stateless function to apply to all other elements of this sequence
     * @return a new sequence consisting of the results of applying the appropriate mapper function to each element
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
     * Transforms each element of this sequence using the provided mapper function,
     * which returns a new sequence for each element. The resulting sequences
     * are then flattened into a single sequence.
     * This is an intermediate operation that does not consume the sequence.
     *
     * <p>This is the monadic bind operation for sequences. Each sequence returned by the mapper
     * is consumed completely before moving to the next element.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer, Exception> numbers = Seq.of(1, 2, 3);
     * Seq<Integer, Exception> result = numbers.flatMap(n -> Seq.of(n, n * 10));
     * // Result: [1, 10, 2, 20, 3, 30]
     * 
     * Seq<String, Exception> sentences = Seq.of("Hello world", "Java programming");
     * Seq<String, Exception> words = sentences.flatMap(s -> Seq.of(s.split(" ")));
     * // Result: ["Hello", "world", "Java", "programming"]
     * }</pre>
     *
     * @param <R> the type of the elements in the new sequence
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
     * Transforms each element of this sequence using the provided mapper function,
     * which returns a collection of new elements for each element. The resulting collections
     * are then flattened into a single sequence.
     * This is an intermediate operation that does not consume the sequence.
     *
     * <p>This is similar to flatMap but works with collections instead of sequences.
     * It's more efficient when the mapper returns small collections.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer, Exception> numbers = Seq.of(1, 2, 3);
     * Seq<Integer, Exception> result = numbers.flatmap(n -> Arrays.asList(n, n * 10));
     * // Result: [1, 10, 2, 20, 3, 30]
     * 
     * Seq<String, Exception> words = Seq.of("Hello", "World");
     * Seq<Character, Exception> chars = words.flatmap(s -> s.chars()
     *     .mapToObj(c -> (char) c)
     *     .collect(Collectors.toList()));
     * // Result: ['H', 'e', 'l', 'l', 'o', 'W', 'o', 'r', 'l', 'd']
     * }</pre>
     *
     * @param <R> the type of the elements in the new sequence
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

    /**
     * Transforms each element of this sequence using the provided mapper function,
     * which returns an array of new elements for each element. The resulting arrays
     * are then flattened into a single sequence.
     * This is an intermediate operation that does not consume the sequence.
     *
     * <p>This is similar to flatMap but works with arrays instead of sequences.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer, Exception> numbers = Seq.of(1, 2, 3);
     * Seq<Integer, Exception> result = numbers.flattmap(n -> new Integer[] {n, n * 10});
     * // Result: [1, 10, 2, 20, 3, 30]
     * 
     * Seq<String, Exception> words = Seq.of("Hello", "World");
     * Seq<Character, Exception> chars = words.flattmap(s -> s.chars()
     *     .mapToObj(c -> (char) c)
     *     .toArray(Character[]::new));
     * // Result: ['H', 'e', 'l', 'l', 'o', 'W', 'o', 'r', 'l', 'd']
     * }</pre>
     *
     * @param <R> the type of the elements in the new sequence
     * @param mapper the function to apply to each element, which returns an array of new elements
     * @return a new sequence containing the flattened elements
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    public <R> Seq<R, E> flattmap(final Throwables.Function<? super T, R[], ? extends E> mapper) { //NOSONAR
        assertNotClosed();

        return create(new Throwables.Iterator<>() {
            private R[] a = null;
            private Iterator<? extends R> cur = null;

            @Override
            public boolean hasNext() throws E {
                while ((cur == null || !cur.hasNext()) && elements.hasNext()) {
                    a = mapper.apply(elements.next());
                    cur = N.isEmpty(a) ? null : ObjIterator.of(a);
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
     * Transforms the {@code non-null} elements of this sequence using the provided mapper function,
     * which returns a collection of new elements for each {@code non-null} element.
     * The resulting collections are then flattened into a single sequence.
     * Null elements are skipped.
     * This is an intermediate operation that does not consume the sequence.
     *
     * <p>This method is equivalent to: {@code skipNulls().flatmap(mapper)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String, Exception> strings = Seq.of("Hello", null, "World", null);
     * Seq<Character, Exception> chars = strings.flatmapIfNotNull(s -> 
     *     s.chars().mapToObj(c -> (char) c).collect(Collectors.toList())
     * );
     * // Result: ['H', 'e', 'l', 'l', 'o', 'W', 'o', 'r', 'l', 'd']
     * }</pre>
     *
     * @param <R> the type of the elements in the new sequence
     * @param mapper the function to apply to each {@code non-null} element, which returns a collection of new elements
     * @return a new sequence containing the flattened elements from {@code non-null} source elements
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
     * Transforms the {@code non-null} elements of this sequence using two successive mapper functions.
     * First mapper is applied to {@code non-null} elements, then the second mapper is applied to {@code non-null} results from the first mapper.
     * The resulting collections are then flattened into a single sequence.
     * This is an intermediate operation that does not consume the sequence.
     *
     * <p>This method is equivalent to: {@code skipNulls().flatmap(mapper).skipNulls().flatmap(mapper2)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * class Department { 
     *     List<Team> getTeams() { ... } 
     * }
     * class Team { 
     *     List<Employee> getMembers() { ... } 
     * }
     * 
     * Seq<Department, Exception> departments = Seq.of(dept1, null, dept2);
     * Seq<Employee, Exception> allEmployees = departments.flatmapIfNotNull(
     *     Department::getTeams,
     *     Team::getMembers
     * );
     * // Gets all employees from all teams in all non-null departments
     * }</pre>
     *
     * @param <U> the type of the intermediate elements in the new sequence
     * @param <R> the type of the final elements in the new sequence
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

    /**
     * Transforms the elements of this sequence using the provided mapper function,
     * which returns an Optional of new elements for each element. Only present
     * values are included in the resulting sequence.
     * This is an intermediate operation that does not consume the sequence.
     *
     * <p>This method is useful for transformations that may not produce a result for every input element.</p>
     *
     * <p>Note: copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a> under Apache License 2.0 and may be modified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String, Exception> strings = Seq.of("1", "2", "abc", "3");
     * Seq<Integer, Exception> numbers = strings.mapPartial(s -> {
     *     try {
     *         return Optional.of(Integer.parseInt(s));
     *     } catch (NumberFormatException e) {
     *         return Optional.empty();
     *     }
     * });
     * // Result: [1, 2, 3] ("abc" is filtered out)
     * }</pre>
     *
     * @param <R> the type of the elements in the new sequence
     * @param mapper the function to apply to each element, which returns an Optional of new elements
     * @return a new sequence containing the transformed elements that are present
     * @throws IllegalStateException if the sequence is already closed
     */
    @SuppressWarnings("rawtypes")
    @Beta
    @IntermediateOp
    public <R> Seq<R, E> mapPartial(final Throwables.Function<? super T, Optional<R>, E> mapper) throws IllegalStateException {
        //noinspection resource
        return map(mapper).filter((Throwables.Predicate) IS_PRESENT_IT).map(GET_AS_IT);
    }

    /**
     * Transforms the elements of this sequence using the provided mapper function,
     * which returns an OptionalInt of new elements for each element. Only present
     * values are included in the resulting sequence.
     * This is an intermediate operation that does not consume the sequence.
     *
     * <p>Note: copied from StreamEx: <a href="https://github.com/amaembo/streamex">StreamEx</a> under Apache License 2.0 and may be modified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String, Exception> strings = Seq.of("1", "2", "abc", "3");
     * Seq<Integer, Exception> numbers = strings.mapPartialToInt(s -> {
     *     try {
     *         return OptionalInt.of(Integer.parseInt(s));
     *     } catch (NumberFormatException e) {
     *         return OptionalInt.empty();
     *     }
     * });
     * // Result: [1, 2, 3] ("abc" is filtered out)
     * }</pre>
     *
     * @param mapper the function to apply to each element, which returns an OptionalInt of new elements
     * @return a new sequence containing the transformed elements that are present
     * @throws IllegalStateException if the sequence is already closed
     */
    @SuppressWarnings("rawtypes")
    @Beta
    @IntermediateOp
    public Seq<Integer, E> mapPartialToInt(final Throwables.Function<? super T, OptionalInt, E> mapper) throws IllegalStateException {
        //noinspection resource
        return map(mapper).filter((Throwables.Predicate) IS_PRESENT_INT).map(GET_AS_INT);
    }

    /**
     * Transforms the elements of this sequence using the provided mapper function,
     * which returns an OptionalLong for each element. Only elements that map to a present OptionalLong
     * (i.e., non-empty OptionalLong values) are included in the resulting sequence.
     * Elements that map to empty OptionalLong are filtered out.
     *
     * <p>This is an intermediate operation that combines mapping and filtering in one step.
     * It's particularly useful when you have a transformation that may not produce a valid result
     * for every input element.</p>
     *
     * <p>Note: This method is adapted from StreamEx library.</p>
     *
     * @param mapper the function to apply to each element, which returns an OptionalLong.
     *               The function should return OptionalLong.empty() for elements that should be filtered out.
     * @return a new sequence containing Long values extracted from the present OptionalLong results
     * @throws IllegalStateException if the sequence is already closed
     * @see #mapPartialToDouble(Throwables.Function)
     */
    @SuppressWarnings("rawtypes")
    @Beta
    @IntermediateOp
    public Seq<Long, E> mapPartialToLong(final Throwables.Function<? super T, OptionalLong, E> mapper) throws IllegalStateException {
        //noinspection resource
        return map(mapper).filter((Throwables.Predicate) IS_PRESENT_LONG).map(GET_AS_LONG);
    }

    /**
     * Transforms the elements of this sequence using the provided mapper function,
     * which returns an OptionalDouble for each element. Only elements that map to a present OptionalDouble
     * (i.e., non-empty OptionalDouble values) are included in the resulting sequence.
     * Elements that map to empty OptionalDouble are filtered out.
     *
     * <p>This is an intermediate operation that combines mapping and filtering in one step.
     * It's particularly useful when you have a transformation that may not produce a valid result
     * for every input element.</p>
     *
     * <p>Note: This method is adapted from StreamEx library.</p>
     *
     * @param mapper the function to apply to each element, which returns an OptionalDouble.
     *               The function should return OptionalDouble.empty() for elements that should be filtered out.
     * @return a new sequence containing Double values extracted from the present OptionalDouble results
     * @throws IllegalStateException if the sequence is already closed
     * @see #mapPartialToLong(Throwables.Function)
     */
    @SuppressWarnings("rawtypes")
    @Beta
    @IntermediateOp
    public Seq<Double, E> mapPartialToDouble(final Throwables.Function<? super T, OptionalDouble, E> mapper) throws IllegalStateException {
        //noinspection resource
        return map(mapper).filter((Throwables.Predicate) IS_PRESENT_DOUBLE).map(GET_AS_DOUBLE);
    }

    /**
     * Transforms each element of this sequence into zero or more elements using the provided mapper function.
     * The mapper function accepts an element and a consumer, and can add multiple elements to the result
     * by calling the consumer multiple times (or not at all).
     *
     * <p>This is an intermediate operation that provides a flexible way to expand, filter, or transform
     * elements in a sequence. Unlike flatMap, which requires creating collections, this method allows
     * direct emission of elements through the consumer.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Split strings into individual characters
     * seq.mapMulti((str, consumer) -> {
     *     for (char c : str.toCharArray()) {
     *         consumer.accept(c);
     *     }
     * })
     *
     * // Generate multiple elements from one
     * seq.mapMulti((n, consumer) -> {
     *     consumer.accept(n);
     *     consumer.accept(n * 2);
     *     consumer.accept(n * 3);
     * })
     * }</pre>
     *
     * @param <R> the type of elements in the resulting sequence
     * @param mapper the function that accepts an element and a consumer. The function should call
     *               the consumer for each element to be included in the resulting sequence.
     * @return a new sequence containing all elements added via the consumer
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the mapper is null
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
     * Returns a sequence consisting of the results of applying the given function to each adjacent pair of elements in this sequence.
     * For sequences with an odd number of elements, the last element will be paired with {@code null}.
     *
     * <p>This is an intermediate operation that creates a sliding window of size 2 with step 1.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For sequence [1, 2, 3, 4], produces: [f(1,2), f(2,3), f(3,4)]
     * seq.slidingMap((a, b) -> a + b); // Results: [3, 5, 7]
     *
     * // For sequence [1], produces: [f(1,null)]
     * seq.slidingMap((a, b) -> String.valueOf(a) + "," + String.valueOf(b)); // Results: ["1,null"]
     * }</pre>
     *
     * @param <R> the element type of the new stream
     * @param mapper a non-interfering, stateless function to apply to each adjacent pair of this sequence's elements
     * @return a new sequence consisting of the results of applying the mapper function to each adjacent pair of elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the mapper is null
     * @see #slidingMap(int, Throwables.BiFunction)
     */
    @IntermediateOp
    public <R> Seq<R, E> slidingMap(final Throwables.BiFunction<? super T, ? super T, R, ? extends E> mapper) throws IllegalStateException {
        return slidingMap(1, mapper);
    }

    /**
     * Returns a sequence consisting of the results of applying the given function to pairs of elements
     * separated by the specified increment in this sequence. Elements without a pair will be paired with {@code null}.
     *
     * <p>This is an intermediate operation that creates a sliding window of size 2 with the specified step.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For sequence [1, 2, 3, 4] with increment=2, produces: [f(1,2), f(3,4)]
     * seq.slidingMap(2, (a, b) -> a + b); // Results: [3, 7]
     *
     * // For sequence [1, 2, 3, 4, 5] with increment=2, produces: [f(1,2), f(3,4), f(5,null)]
     * seq.slidingMap(2, (a, b) -> a + "," + b); // Results: ["1,2", "3,4", "5,null"]
     * }</pre>
     *
     * @param <R> the element type of the new sequence
     * @param increment the distance between the first elements of each pair (must be positive)
     * @param mapper a non-interfering, stateless function to apply to each pair of elements
     * @return a new sequence consisting of the results of applying the mapper function to each pair of elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the mapper is {@code null} or increment is not positive
     * @see #slidingMap(int, boolean, Throwables.BiFunction)
     */
    @IntermediateOp
    public <R> Seq<R, E> slidingMap(final int increment, final Throwables.BiFunction<? super T, ? super T, R, ? extends E> mapper)
            throws IllegalStateException, IllegalArgumentException {
        return slidingMap(increment, false, mapper);
    }

    /**
     * Returns a sequence consisting of the results of applying the given function to pairs of elements
     * separated by the specified increment in this sequence, with control over unpaired elements.
     *
     * <p>This is an intermediate operation that creates a sliding window of size 2 with the specified step.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For sequence [1, 2, 3, 4, 5] with increment=2, ignoreNotPaired=false
     * seq.slidingMap(2, false, (a, b) -> a + "," + b); // Results: ["1,2", "3,4", "5,null"]
     *
     * // For sequence [1, 2, 3, 4, 5] with increment=2, ignoreNotPaired=true
     * seq.slidingMap(2, true, (a, b) -> a + b); // Results: [3, 7] (element 5 is ignored)
     * }</pre>
     *
     * @param <R> the element type of the new sequence
     * @param increment the distance between the first elements of each pair (must be positive)
     * @param ignoreNotPaired if {@code false}, unpaired elements will be processed with {@code null} as their pair;
     *                        if {@code true}, the last element will be ignored if there is no element to pair with it
     * @param mapper a non-interfering, stateless function to apply to each pair of elements
     * @return a new sequence consisting of the results of applying the mapper function to each pair of elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the mapper is {@code null} or increment is not positive
     */
    @IntermediateOp
    public <R> Seq<R, E> slidingMap(final int increment, final boolean ignoreNotPaired,
            final Throwables.BiFunction<? super T, ? super T, R, ? extends E> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(mapper, cs.mapper);
        checkArgPositive(increment, cs.increment);

        final int windowSize = 2;

        return create(new Throwables.Iterator<>() {
            @SuppressWarnings("unchecked")
            private final T none = (T) NONE;
            private T prev = none;
            private R ret = null;
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

                if (ignoreNotPaired && prev == none && elements.hasNext()) {
                    prev = elements.next();
                }

                return elements.hasNext(); //  || (!ignoreNotPaired && prev != none);
            }

            @Override
            public R next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                toSkip = increment > windowSize;

                if (ignoreNotPaired) {
                    if (increment == 1) {
                        return mapper.apply(prev, ((prev = (elements.hasNext() ? elements.next() : none)) == none) ? null : prev);
                    } else {
                        ret = mapper.apply(prev, elements.next());
                        prev = none;
                        return ret;
                    }
                } else if (increment == 1) {
                    return mapper.apply(prev == none ? elements.next() : prev, ((prev = (elements.hasNext() ? elements.next() : none)) == none) ? null : prev);
                } else {
                    return mapper.apply(elements.next(), elements.hasNext() ? elements.next() : null);
                }
            }
        }, closeHandlers);
    }

    /**
     * Returns a sequence consisting of the results of applying the given function to each adjacent triple of elements in this sequence.
     * For sequences with a number of elements not divisible by 3, the last elements will be paired with {@code null} values.
     *
     * <p>This is an intermediate operation that creates a sliding window of size 3 with step 1.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For sequence [1, 2, 3, 4, 5], produces: [f(1,2,3), f(2,3,4), f(3,4,5)]
     * seq.slidingMap((a, b, c) -> a + b + c); // Results: [6, 9, 12]
     *
     * // For sequence [1], produces: [f(1,null,null)]
     * seq.slidingMap((a, b, c) -> String.format("%s,%s,%s", a, b, c)); // Results: ["1,null,null"]
     * }</pre>
     *
     * @param <R> the element type of the new sequence
     * @param mapper a non-interfering, stateless function to apply to each adjacent triple of this sequence's elements
     * @return a new sequence consisting of the results of applying the mapper function to each adjacent triple of elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the mapper is null
     * @see #slidingMap(int, Throwables.TriFunction)
     */
    @IntermediateOp
    public <R> Seq<R, E> slidingMap(final Throwables.TriFunction<? super T, ? super T, ? super T, R, ? extends E> mapper) throws IllegalStateException {
        return slidingMap(1, mapper);
    }

    /**
     * Returns a sequence consisting of the results of applying the given function to triples of elements
     * separated by the specified increment in this sequence. Elements without a complete triple will be paired with {@code null} values.
     *
     * <p>This is an intermediate operation that creates a sliding window of size 3 with the specified step.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For sequence [1, 2, 3, 4, 5, 6] with increment=3, produces: [f(1,2,3), f(4,5,6)]
     * seq.slidingMap(3, (a, b, c) -> a + b + c); // Results: [6, 15]
     *
     * // For sequence [1, 2, 3, 4, 5, 6, 7, 8] with increment=3, produces: [f(1,2,3), f(4,5,6), f(7,8,null)]
     * seq.slidingMap(3, (a, b, c) -> String.format("%s+%s+%s", a, b, c)); // Results: ["1+2+3", "4+5+6", "7+8+null"]
     * }</pre>
     *
     * @param <R> the element type of the new sequence
     * @param increment the distance between the first elements of each triple (must be positive)
     * @param mapper a non-interfering, stateless function to apply to each triple of elements
     * @return a new sequence consisting of the results of applying the mapper function to each triple of elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the mapper is {@code null} or increment is not positive
     * @see #slidingMap(int, boolean, Throwables.TriFunction)
     */
    @IntermediateOp
    public <R> Seq<R, E> slidingMap(final int increment, final Throwables.TriFunction<? super T, ? super T, ? super T, R, ? extends E> mapper)
            throws IllegalStateException, IllegalArgumentException {
        return slidingMap(increment, false, mapper);
    }

    /**
     * Returns a sequence consisting of the results of applying the given function to triples of elements
     * separated by the specified increment in this sequence, with control over unpaired elements.
     *
     * <p>This is an intermediate operation that creates a sliding window of size 3 with the specified step.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // For sequence [1, 2, 3, 4, 5, 6, 7] with increment=3, ignoreNotPaired=false
     * seq.slidingMap(3, false, (a, b, c) -> String.format("%s+%s+%s", a, b, c)); 
     * // Results: ["1+2+3", "4+5+6", "7+null+null"]
     *
     * // For sequence [1, 2, 3, 4, 5, 6, 7] with increment=3, ignoreNotPaired=true
     * seq.slidingMap(3, true, (a, b, c) -> a + b + c); 
     * // Results: [6, 15] (element 7 is ignored)
     * }</pre>
     *
     * @param <R> the element type of the new sequence
     * @param increment the distance between the first elements of each triple (must be positive)
     * @param ignoreNotPaired if {@code false}, unpaired elements will be processed with {@code null} as their pair;
     *                        if {@code true}, elements that cannot form a complete triple will be ignored
     * @param mapper a non-interfering, stateless function to apply to each triple of elements
     * @return a new sequence consisting of the results of applying the mapper function to each triple of elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the mapper is {@code null} or increment is not positive
     */
    @IntermediateOp
    public <R> Seq<R, E> slidingMap(final int increment, final boolean ignoreNotPaired,
            final Throwables.TriFunction<? super T, ? super T, ? super T, R, ? extends E> mapper) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(mapper, cs.mapper);
        checkArgPositive(increment, cs.increment);

        final int windowSize = 3;

        return create(new Throwables.Iterator<>() {
            @SuppressWarnings("unchecked")
            private final T none = (T) NONE;
            private T prev = none;
            private T prevPrev = none;
            private R ret = null;
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

                if (ignoreNotPaired) {
                    if (prevPrev == none && elements.hasNext()) {
                        prevPrev = elements.next();
                    }

                    if (prev == none && elements.hasNext()) {
                        prev = elements.next();
                    }
                }

                return elements.hasNext(); // || (!ignoreNotPaired && (prev != none));
            }

            @Override
            public R next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException(ERROR_MSG_FOR_NO_SUCH_EX);
                }

                toSkip = increment > windowSize;

                if (ignoreNotPaired) {
                    if (increment == 1) {
                        return mapper.apply(prevPrev, (prevPrev = prev), prev = elements.next());
                    } else if (increment == 2) {
                        ret = mapper.apply(prevPrev, prev, prevPrev = elements.next());
                        prev = none;
                        return ret;
                    } else {
                        ret = mapper.apply(prevPrev, prev, elements.next());
                        prevPrev = none;
                        prev = none;
                        return ret;
                    }
                } else if (increment == 1) {
                    return mapper.apply(prevPrev == none ? elements.next() : prevPrev,
                            (prevPrev = (prev == none ? (elements.hasNext() ? elements.next() : none) : prev)) == none ? null : prevPrev,
                            ((prev = (elements.hasNext() ? elements.next() : none)) == none) ? null : prev);
                } else if (increment == 2) {
                    return mapper.apply(prev == none ? elements.next() : prev, elements.hasNext() ? elements.next() : null,
                            ((prev = (elements.hasNext() ? elements.next() : none)) == none) ? null : prev);
                } else {
                    ret = mapper.apply(elements.next(), elements.hasNext() ? elements.next() : null, elements.hasNext() ? elements.next() : null);
                    prevPrev = none;
                    prev = none;
                    return ret;
                }
            }
        }, closeHandlers);
    }

    /**
     * Groups the elements of this sequence by a key extracted using the provided key extractor function.
     * Returns a sequence of Map.Entry objects where each entry contains a key and a list of all elements
     * that share that key.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the grouping.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group strings by their length
     * seq.groupBy(String::length)
     *    .forEach(entry -> System.out.println("Length " + entry.getKey() + ": " + entry.getValue()));
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param keyMapper the function to extract the key from each element
     * @return a new sequence containing Map.Entry objects where each key maps to a list of elements with that key
     * @throws IllegalStateException if the sequence is already closed
     * @see #groupBy(Throwables.Function, Supplier)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K> Seq<Map.Entry<K, List<T>>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper) throws IllegalStateException {
        return groupBy(keyMapper, Suppliers.ofMap());
    }

    /**
     * Groups the elements of this sequence by a key extracted using the provided key extractor function,
     * using the specified map factory to create the backing map.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the grouping.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group strings by their length using a TreeMap to keep keys sorted
     * seq.groupBy(String::length, TreeMap::new)
     *    .forEach(entry -> System.out.println("Length " + entry.getKey() + ": " + entry.getValue()));
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param keyMapper the function to extract the key from each element
     * @param mapFactory the supplier to create a new map instance for storing the groups
     * @return a new sequence containing Map.Entry objects where each key maps to a list of elements with that key
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
     * and transforms the values using the provided value mapper function.
     * Returns a sequence of Map.Entry objects where each entry contains a key and a list of transformed values.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the grouping.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group persons by age, collecting only their names
     * seq.groupBy(Person::getAge, Person::getName)
     *    .forEach(entry -> System.out.println("Age " + entry.getKey() + ": " + entry.getValue()));
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values after mapping
     * @param keyMapper the function to extract the key from each element
     * @param valueMapper the function to transform each element into the value to be grouped
     * @return a new sequence containing Map.Entry objects where each key maps to a list of transformed values
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
     * and transforms the values using the provided value mapper function, using the specified map factory.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the grouping.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group persons by department, collecting their salaries, using a LinkedHashMap
     * seq.groupBy(Person::getDepartment, Person::getSalary, LinkedHashMap::new)
     *    .forEach(entry -> System.out.println("Dept " + entry.getKey() + 
     *                                         " salaries: " + entry.getValue()));
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values after mapping
     * @param keyMapper the function to extract the key from each element
     * @param valueMapper the function to transform each element into the value to be grouped
     * @param mapFactory the supplier to create a new map instance for storing the groups
     * @return a new sequence containing Map.Entry objects where each key maps to a list of transformed values
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
     * and merges values with the same key using the provided merge function.
     * Returns a sequence of Map.Entry objects where each entry contains a key and a single merged value.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the grouping.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum integers by their remainder when divided by 3
     * seq.groupBy(n -> n % 3, n -> n, Integer::sum)
     *    .forEach(entry -> System.out.println("Remainder " + entry.getKey() + 
     *                                         " sum: " + entry.getValue()));
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values after mapping
     * @param keyMapper the function to extract the key from each element
     * @param valueMapper the function to transform each element into the value to be grouped
     * @param mergeFunction the function to merge values with the same key
     * @return a new sequence containing Map.Entry objects where each key maps to a single merged value
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
     * and merges values with the same key using the provided merge function, using the specified map factory.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the grouping.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Find the maximum salary per department using a TreeMap
     * seq.groupBy(Person::getDepartment, Person::getSalary, 
     *             BinaryOperator.maxBy(naturalOrder()), TreeMap::new)
     *    .forEach(entry -> System.out.println("Dept " + entry.getKey() + 
     *                                         " max salary: " + entry.getValue()));
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values after mapping
     * @param keyMapper the function to extract the key from each element
     * @param valueMapper the function to transform each element into the value to be grouped
     * @param mergeFunction the function to merge values with the same key
     * @param mapFactory the supplier to create a new map instance for storing the groups
     * @return a new sequence containing Map.Entry objects where each key maps to a single merged value
     * @throws IllegalStateException if the sequence is already closed
     * @see Collectors#toMap(Function, Function, BinaryOperator)
     * @see Fnn#throwingMerger()
     * @see Fnn#replacingMerger()
     * @see Fnn#ignoringMerger()
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
     * and collects the grouped elements using the provided downstream collector.
     * Returns a sequence of Map.Entry objects where each entry contains a key and the result of the downstream collector.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the grouping.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Count elements per group
     * seq.groupBy(String::length, Collectors.counting())
     *    .forEach(entry -> System.out.println("Length " + entry.getKey() + 
     *                                         " has " + entry.getValue() + " strings"));
     *
     * // Get average salary per department
     * seq.groupBy(Person::getDepartment, 
     *             Collectors.averagingDouble(Person::getSalary))
     *    .forEach(entry -> System.out.println("Dept " + entry.getKey() + 
     *                                         " avg salary: " + entry.getValue()));
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <D> the type of the downstream result
     * @param keyMapper the function to extract the key from each element
     * @param downstream the collector to use for elements in each group
     * @return a new sequence containing Map.Entry objects where each key maps to the collector result
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
     * and collects the grouped elements using the provided downstream collector, using the specified map factory.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the grouping.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join strings per group using a TreeMap to keep groups sorted
     * seq.groupBy(String::length, 
     *             Collectors.joining(", "),
     *             TreeMap::new)
     *    .forEach(entry -> System.out.println("Length " + entry.getKey() + 
     *                                         ": " + entry.getValue()));
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <D> the type of the downstream result
     * @param keyMapper the function to extract the key from each element
     * @param downstream the collector to use for elements in each group
     * @param mapFactory the supplier to create a new map instance for storing the groups
     * @return a new sequence containing Map.Entry objects where each key maps to the collector result
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
     * transforms values using the value mapper, and collects the transformed values using the downstream collector.
     * Returns a sequence of Map.Entry objects where each entry contains a key and the collector result.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the grouping.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get comma-separated names per age group
     * seq.groupBy(Person::getAgeGroup, 
     *             Person::getName,
     *             Collectors.joining(", "))
     *    .forEach(entry -> System.out.println("Age group " + entry.getKey() + 
     *                                         ": " + entry.getValue()));
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values after mapping
     * @param <D> the type of the downstream result
     * @param keyMapper the function to extract the key from each element
     * @param valueMapper the function to transform each element into the value to be collected
     * @param downstream the collector to use for the transformed values in each group
     * @return a new sequence containing Map.Entry objects where each key maps to the collector result
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
     * transforms values using the value mapper, and collects the transformed values using the downstream collector,
     * using the specified map factory.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the grouping.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Get total salary per department using a LinkedHashMap
     * seq.groupBy(Person::getDepartment, 
     *             Person::getSalary,
     *             Collectors.summingLong(Long::longValue),
     *             LinkedHashMap::new)
     *    .forEach(entry -> System.out.println("Dept " + entry.getKey() + 
     *                                         " total: " + entry.getValue()));
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param <V> the type of the values after mapping
     * @param <D> the type of the downstream result
     * @param keyMapper the function to extract the key from each element
     * @param valueMapper the function to transform each element into the value to be collected
     * @param downstream the collector to use for the transformed values in each group
     * @param mapFactory the supplier to create a new map instance for storing the groups
     * @return a new sequence containing Map.Entry objects where each key maps to the collector result
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
     * Returns a sequence containing exactly two Map.Entry objects:
     * - One with key {@code true} containing a list of elements that satisfy the predicate
     * - One with key {@code false} containing a list of elements that do not satisfy the predicate
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the partitioning.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Partition numbers into even and odd
     * seq.partitionBy(n -> n % 2 == 0)
     *    .forEach(entry -> System.out.println(
     *        (entry.getKey() ? "Even" : "Odd") + ": " + entry.getValue()));
     * }</pre>
     *
     * @param predicate the predicate used to classify elements
     * @return a new sequence containing exactly two entries: true-&gt;matching elements, false-&gt;non-matching elements
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
     * and collects each partition using the provided downstream collector.
     * Returns a sequence containing exactly two Map.Entry objects:
     * - One with key {@code true} containing the result of collecting elements that satisfy the predicate
     * - One with key {@code false} containing the result of collecting elements that do not satisfy the predicate
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the partitioning.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Count elements in each partition
     * seq.partitionBy(n -> n > 0, Collectors.counting())
     *    .forEach(entry -> System.out.println(
     *        (entry.getKey() ? "Positive" : "Non-positive") + " count: " + entry.getValue()));
     *
     * // Join strings in each partition
     * seq.partitionBy(String::isEmpty, Collectors.joining(", "))
     *    .forEach(entry -> System.out.println(
     *        (entry.getKey() ? "Empty" : "Non-empty") + ": " + entry.getValue()));
     * }</pre>
     *
     * @param <D> the type of the downstream result
     * @param predicate the predicate used to classify elements
     * @param downstream the collector to use for elements in each partition
     * @return a new sequence containing exactly two entries with the collector results
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
     * Counts the occurrences of elements in this sequence grouped by a key extracted using the provided key extractor function.
     * Returns a sequence of Map.Entry objects where each entry contains a key and the count of elements with that key.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the counting.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Count words by their length
     * seq.countBy(String::length)
     *    .forEach(entry -> System.out.println("Length " + entry.getKey() + 
     *                                         " appears " + entry.getValue() + " times"));
     *
     * // Count persons by department
     * seq.countBy(Person::getDepartment)
     *    .forEach(entry -> System.out.println("Department " + entry.getKey() + 
     *                                         " has " + entry.getValue() + " employees"));
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param keyMapper the function to extract the key from each element
     * @return a new sequence containing Map.Entry objects where each key maps to its count
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K> Seq<Map.Entry<K, Integer>, E> countBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper) throws IllegalStateException {
        return groupBy(keyMapper, Collectors.countingToInt());
    }

    /**
     * Counts the occurrences of elements in this sequence grouped by a key extracted using the provided key extractor function,
     * using the specified map factory to create the backing map.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the counting.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Count strings by first character, keeping counts in sorted order
     * seq.countBy(s -> s.charAt(0), TreeMap::new)
     *    .forEach(entry -> System.out.println("'" + entry.getKey() + 
     *                                         "' appears " + entry.getValue() + " times"));
     * }</pre>
     *
     * @param <K> the type of the keys
     * @param keyMapper the function to extract the key from each element
     * @param mapFactory the supplier to create a new map instance for storing the counts
     * @return a new sequence containing Map.Entry objects where each key maps to its count
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K> Seq<Map.Entry<K, Integer>, E> countBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Supplier<? extends Map<K, Integer>> mapFactory) throws IllegalStateException {
        return groupBy(keyMapper, Collectors.countingToInt(), mapFactory);
    }

    /**
     * Returns a new sequence containing elements that are present in both this sequence and the specified collection.
     * For elements that appear multiple times, the intersection contains the minimum number of occurrences
     * present in both sources. The order of elements in the original sequence is preserved.
     *
     * <p>This is an intermediate operation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 1, 2, 3);
     * List<Integer> list = Arrays.asList(1, 2, 2, 4);
     * Seq<Integer> result = seq.intersection(list); 
     * // result will be [1, 2]
     * // One occurrence of '1' (min of 2 in seq and 1 in list) 
     * // and one occurrence of '2' (min of 1 in seq and 2 in list)
     *
     * Seq<String> seq2 = Seq.of("a", "a", "b");
     * Set<String> set = new HashSet<>(Arrays.asList("a", "c"));
     * Seq<String> result2 = seq2.intersection(set); 
     * // result will be ["a"]
     * // One occurrence of 'a' (min of 2 in seq and 1 in set)
     * }</pre>
     *
     * @param c the collection to find common elements with this sequence
     * @return a new sequence containing elements present in both this sequence and the specified collection,
     *         considering the minimum number of occurrences in either source
     * @throws IllegalStateException if the sequence is already closed
     * @see N#intersection(Collection, Collection)
     * @see N#commonSet(Collection, Collection)
     * @see Collection#retainAll(Collection)
     * @see N#intersection(int[], int[])
     */
    @IntermediateOp
    public Seq<T, E> intersection(final Collection<?> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        return filter(multiset::remove);
    }

    /**
     * Returns a new sequence containing elements from this sequence that, when mapped by the given function,
     * produce values that are present in the specified collection. For elements that appear multiple times,
     * the intersection contains the minimum number of occurrences present in both sources (after mapping).
     * The order of elements in the original sequence is preserved.
     *
     * <p>This is an intermediate operation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Person> seq = Seq.of(new Person("Alice", 25), 
     *                          new Person("Bob", 30), 
     *                          new Person("Alice", 35));
     * List<String> names = Arrays.asList("Alice", "Charlie");
     * Seq<Person> result = seq.intersection(Person::getName, names); 
     * // result will be [Person("Alice", 25)]
     * // Only the first "Alice" person is included because "Alice" appears only once in names
     *
     * Seq<Product> products = Seq.of(new Product(1, "A"), 
     *                                new Product(2, "B"), 
     *                                new Product(3, "C"));
     * Set<Integer> ids = new HashSet<>(Arrays.asList(1, 3, 4));
     * Seq<Product> result2 = products.intersection(Product::getId, ids); 
     * // result will be [Product(1, "A"), Product(3, "C")]
     * // Only products with IDs in the set are included
     * }</pre>
     *
     * @param <U> the type of the elements after mapping
     * @param mapper the function to apply to elements of this sequence for comparison
     * @param c the collection to find common elements with (after mapping)
     * @return a new sequence containing elements whose mapped values are present in the specified collection,
     *         considering the minimum number of occurrences in either source
     * @throws IllegalStateException if the sequence is already closed
     * @see #intersection(Collection)
     * @see N#intersection(Collection, Collection)
     * @see N#commonSet(Collection, Collection)
     * @see N#intersection(int[], int[])
     */
    @IntermediateOp
    public <U> Seq<T, E> intersection(final Throwables.Function<? super T, ? extends U, E> mapper, final Collection<U> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        return filter(value -> multiset.remove(mapper.apply(value)));
    }

    /**
     * Returns a new sequence containing elements from this sequence that are not in the specified collection,
     * considering the number of occurrences of each element. If an element appears multiple times in this
     * sequence and also in the collection, the result will contain the extra occurrences from this sequence.
     *
     * <p>This is an intermediate operation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq1 = Seq.of(1, 1, 2, 3);
     * List<Integer> list = Arrays.asList(1, 4);
     * Seq<Integer> result = seq1.difference(list); 
     * // result will be [1, 2, 3]
     * // One '1' remains because seq1 has two occurrences and list has one
     *
     * Seq<String> seq2 = Seq.of("apple", "orange");
     * List<String> list2 = Arrays.asList("apple", "apple", "orange");
     * Seq<String> result2 = seq2.difference(list2); 
     * // result will be [] (empty)
     * // No elements remain because list2 has at least as many occurrences of each value as seq2
     * }</pre>
     *
     * @param c the collection to compare against this sequence
     * @return a new sequence containing the elements that are present in this sequence but not in the
     *         specified collection, considering the number of occurrences
     * @throws IllegalStateException if the sequence is already closed
     * @see #difference(Function, Collection)
     * @see N#difference(Collection, Collection)
     * @see N#difference(int[], int[])
     */
    @IntermediateOp
    public Seq<T, E> difference(final Collection<?> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        return filter(value -> !multiset.remove(value));
    }

    /**
     * Returns a new sequence containing elements from this sequence whose mapped values are not in the
     * specified collection, considering the number of occurrences. The mapper function is applied to each
     * element to determine the value used for comparison.
     *
     * <p>This is an intermediate operation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Person> seq1 = Seq.of(new Person("Alice", 25), 
     *                           new Person("Alice", 30), 
     *                           new Person("Bob", 35));
     * List<String> names = Arrays.asList("Alice", "Charlie");
     * Seq<Person> result = seq1.difference(Person::getName, names); 
     * // result will be [Person("Alice", 30), Person("Bob", 35)]
     * // First Alice is removed (one occurrence in names), second Alice and Bob remain
     *
     * Seq<Transaction> seq2 = Seq.of(new Transaction(101), new Transaction(102));
     * List<Integer> ids = Arrays.asList(101, 101, 102);
     * Seq<Transaction> result2 = seq2.difference(Transaction::getId, ids); 
     * // result will be [] (empty)
     * // No elements remain because ids has at least as many occurrences of each mapped value
     * }</pre>
     *
     * @param <U> the type of the elements after mapping
     * @param mapper the function to apply to each element of this sequence for comparison
     * @param c the collection of mapped values to compare against
     * @return a new sequence containing the elements whose mapped values are not in the specified collection,
     *         considering the number of occurrences after mapping
     * @throws IllegalStateException if the sequence is already closed
     * @see #difference(Collection)
     * @see N#difference(Collection, Collection)
     */
    @IntermediateOp
    public <U> Seq<T, E> difference(final Function<? super T, ? extends U> mapper, final Collection<U> c) throws IllegalStateException {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.create(c);

        return filter(value -> multiset.isEmpty() || !multiset.remove(mapper.apply(value)));
    }

    /**
     * Returns a new sequence containing elements that are present in either this sequence or the specified
     * collection, but not in both. This is the set-theoretic symmetric difference operation.
     * For elements that appear multiple times, the result contains the absolute difference in occurrences.
     *
     * <p>The order of elements is preserved, with elements from this sequence appearing first,
     * followed by elements from the specified collection that aren't in this sequence.</p>
     *
     * <p>This is an intermediate operation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 1, 2, 3);
     * List<Integer> list = Arrays.asList(1, 2, 2, 4);
     * Seq<Integer> result = seq.symmetricDifference(list);
     * // result will contain: [1, 3, 2, 4]
     * // Elements explanation:
     * // - 1 appears twice in seq and once in list, so one occurrence remains from seq
     * // - 3 appears only in seq, so it remains
     * // - 2 appears once in seq and twice in list, so one occurrence remains from list
     * // - 4 appears only in list, so it remains
     * }</pre>
     *
     * @param c the collection to find symmetric difference with this sequence
     * @return a new sequence containing elements that are present in either this sequence or the specified
     *         collection, but not in both, considering the number of occurrences
     * @throws IllegalStateException if the sequence is already closed
     * @see N#symmetricDifference(Collection, Collection)
     * @see N#symmetricDifference(int[], int[])
     * @see #intersection(Collection)
     * @see #difference(Collection)
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
     * The elements are prepended in the order they appear in the array.
     *
     * <p>This is an intermediate operation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(3, 4, 5);
     * Seq<Integer> result = seq.prepend(1, 2);
     * // result will be [1, 2, 3, 4, 5]
     * }</pre>
     *
     * @param a the elements to be prepended to this sequence
     * @return a new sequence with the specified elements prepended
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @SafeVarargs
    public final Seq<T, E> prepend(final T... a) throws IllegalStateException {
        return prepend(Seq.of(a));
    }

    /**
     * Prepends the specified collection of elements to the beginning of this sequence.
     * The elements from the collection will appear before the existing elements in the sequence,
     * maintaining their order from the collection.
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(3, 4, 5);
     * List<Integer> toPrepend = Arrays.asList(1, 2);
     * Seq<Integer> result = seq.prepend(toPrepend);
     * // result contains: 1, 2, 3, 4, 5
     * }</pre>
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
     * The elements from the specified sequence will appear before the existing elements,
     * maintaining their order from the source sequence.
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq1 = Seq.of("world");
     * Seq<String> seq2 = Seq.of("hello", " ");
     * Seq<String> result = seq1.prepend(seq2);
     * // result contains: "hello", " ", "world"
     * }</pre>
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
     * If the Optional is empty, the sequence remains unchanged. If the Optional contains a value,
     * that value is prepended to the beginning of the sequence.
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = Seq.of("world");
     * Optional<String> maybeHello = Optional.of("hello");
     * Seq<String> result = seq.prepend(maybeHello);
     * // result contains: "hello", "world"
     * 
     * Optional<String> empty = Optional.empty();
     * Seq<String> result2 = seq.prepend(empty);
     * // result2 contains: "world" (unchanged)
     * }</pre>
     *
     * @param op the optional element to be prepended to this sequence
     * @return a new Seq with the specified optional element prepended if present, otherwise returns this sequence unchanged
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
     * The elements will be added in the order they appear in the array.
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2);
     * Seq<Integer> result = seq.append(3, 4, 5);
     * // result contains: 1, 2, 3, 4, 5
     * }</pre>
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
     * The elements from the collection will be added after the existing elements,
     * maintaining their order from the collection.
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = Seq.of("a", "b");
     * List<String> toAppend = Arrays.asList("c", "d");
     * Seq<String> result = seq.append(toAppend);
     * // result contains: "a", "b", "c", "d"
     * }</pre>
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
     * The elements from the specified sequence will be added after the existing elements,
     * maintaining their order from the source sequence.
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq1 = Seq.of(1, 2);
     * Seq<Integer> seq2 = Seq.of(3, 4);
     * Seq<Integer> result = seq1.append(seq2);
     * // result contains: 1, 2, 3, 4
     * }</pre>
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
     * If the Optional is empty, the sequence remains unchanged. If the Optional contains a value,
     * that value is appended to the end of the sequence.
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = Seq.of("hello");
     * Optional<String> maybeWorld = Optional.of("world");
     * Seq<String> result = seq.append(maybeWorld);
     * // result contains: "hello", "world"
     * 
     * Optional<String> empty = Optional.empty();
     * Seq<String> result2 = seq.append(empty);
     * // result2 contains: "hello" (unchanged)
     * }</pre>
     *
     * @param op the optional element to be appended to this sequence
     * @return a new Seq with the specified optional element appended if present, otherwise returns this sequence unchanged
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
     * If the sequence already contains elements, it remains unchanged.
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> emptySeq = Seq.<Integer>empty();
     * Seq<Integer> result1 = emptySeq.appendIfEmpty(1, 2, 3);
     * // result1 contains: 1, 2, 3
     * 
     * Seq<Integer> nonEmptySeq = Seq.of(4, 5);
     * Seq<Integer> result2 = nonEmptySeq.appendIfEmpty(1, 2, 3);
     * // result2 contains: 4, 5 (unchanged)
     * }</pre>
     *
     * @param a the elements to be appended if this sequence is empty
     * @return a new Seq with the specified elements appended if this sequence is empty, otherwise returns this sequence unchanged
     * @throws IllegalStateException if the sequence is already closed
     */
    @IntermediateOp
    @SafeVarargs
    public final Seq<T, E> appendIfEmpty(final T... a) throws IllegalStateException {
        return appendIfEmpty(Arrays.asList(a));
    }

    /**
     * Appends the elements from the specified collection to the end of this Seq if the Seq is empty.
     * If the sequence already contains elements, it remains unchanged.
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> emptySeq = Seq.<String>empty();
     * List<String> defaults = Arrays.asList("default1", "default2");
     * Seq<String> result1 = emptySeq.appendIfEmpty(defaults);
     * // result1 contains: "default1", "default2"
     * 
     * Seq<String> nonEmptySeq = Seq.of("value");
     * Seq<String> result2 = nonEmptySeq.appendIfEmpty(defaults);
     * // result2 contains: "value" (unchanged)
     * }</pre>
     *
     * @param c the collection of elements to append if the Seq is empty
     * @return a new Seq with the elements from the specified collection appended if the Seq is empty, otherwise returns this sequence unchanged
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
                if (n <= 0) {
                    return;
                }

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
     * If the sequence already contains elements, it remains unchanged. The supplier is only invoked if this
     * sequence is empty, providing lazy evaluation of the default elements.
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> emptySeq = Seq.<Integer>empty();
     * Supplier<Seq<Integer>> defaultSupplier = () -> Seq.of(1, 2, 3);
     * Seq<Integer> result1 = emptySeq.appendIfEmpty(defaultSupplier);
     * // result1 contains: 1, 2, 3
     * 
     * Seq<Integer> nonEmptySeq = Seq.of(4, 5);
     * Seq<Integer> result2 = nonEmptySeq.appendIfEmpty(defaultSupplier);
     * // result2 contains: 4, 5 (unchanged, supplier not invoked)
     * }</pre>
     *
     * @param supplier the supplier that provides a Seq of elements to append if the Seq is empty
     * @return a new Seq with the elements from the Seq provided by the supplier appended if the Seq is empty, otherwise returns this sequence unchanged
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
                if (n <= 0) {
                    return;
                }

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

    /**
     * Returns a new Seq that contains the specified default value if this Seq is empty.
     * This is a convenience method equivalent to {@code appendIfEmpty(defaultValue)}.
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> emptySeq = Seq.<String>empty();
     * Seq<String> result1 = emptySeq.defaultIfEmpty("default");
     * // result1 contains: "default"
     * 
     * Seq<String> nonEmptySeq = Seq.of("value");
     * Seq<String> result2 = nonEmptySeq.defaultIfEmpty("default");
     * // result2 contains: "value" (unchanged)
     * }</pre>
     *
     * @param defaultValue the default value to return if this Seq is empty
     * @return a new Seq containing the default value if this Seq is empty, otherwise returns this sequence unchanged
     * @throws IllegalStateException if the sequence is already closed
     * @see #appendIfEmpty(Object...)
     */
    @IntermediateOp
    public Seq<T, E> defaultIfEmpty(final T defaultValue) throws IllegalStateException {
        return appendIfEmpty(defaultValue);
    }

    /**
     * Returns a new Seq that contains the elements from the Seq provided by the supplier if this Seq is empty.
     * This is a convenience method equivalent to {@code appendIfEmpty(supplier)}.
     * The supplier is only invoked if this sequence is empty, providing lazy evaluation.
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> emptySeq = Seq.<Integer>empty();
     * Supplier<Seq<Integer>> defaultSupplier = () -> Seq.of(1, 2, 3);
     * Seq<Integer> result1 = emptySeq.defaultIfEmpty(defaultSupplier);
     * // result1 contains: 1, 2, 3
     * 
     * Seq<Integer> nonEmptySeq = Seq.of(4, 5);
     * Seq<Integer> result2 = nonEmptySeq.defaultIfEmpty(defaultSupplier);
     * // result2 contains: 4, 5 (unchanged, supplier not invoked)
     * }</pre>
     *
     * @param supplier the supplier that provides a Seq of elements to return if this Seq is empty
     * @return a new Seq with the elements from the Seq provided by the supplier if this Seq is empty, otherwise returns this sequence unchanged
     * @throws IllegalStateException if the sequence is already closed
     * @see #appendIfEmpty(Supplier)
     */
    @IntermediateOp
    public Seq<T, E> defaultIfEmpty(final Supplier<? extends Seq<T, E>> supplier) throws IllegalStateException {
        return appendIfEmpty(supplier);
    }

    /**
     * Throws a {@code NoSuchElementException} in executed terminal operation if this {@code Seq} is empty.
     * This provides a way to ensure that the sequence contains at least one element before processing.
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = getSeq();
     * Integer first = seq.throwIfEmpty().first().orElseThrow();
     * // Throws NoSuchElementException if seq was empty
     * }</pre>
     *
     * @return this {@code Seq} instance
     * @throws IllegalStateException if the sequence is already closed
     * @throws NoSuchElementException if the sequence is empty (thrown when terminal operation is executed)
     */
    @IntermediateOp
    public Seq<T, E> throwIfEmpty() throws IllegalStateException, NoSuchElementException {
        return throwIfEmpty(Suppliers.newNoSuchElementException());
    }

    /**
     * Throws a custom exception provided by the supplier in terminal operation if this {@code Seq} is empty.
     * This allows customization of the exception thrown when the sequence is empty.
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = getSeq();
     * String result = seq.throwIfEmpty(() -> new IllegalStateException("Sequence must not be empty"))
     *                    .join(", ");
     * // Throws IllegalStateException if seq was empty
     * }</pre>
     *
     * @param exceptionSupplier the supplier that provides the exception to throw if this {@code Seq} is empty
     * @return this {@code Seq} instance
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if exceptionSupplier is null
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
     * The action is executed lazily when a terminal operation determines that the sequence is empty.
     * If the sequence contains elements, the action is not executed.
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = getSeq();
     * seq.ifEmpty(() -> System.out.println("Sequence is empty"))
     *    .forEach(System.out::println);
     * // Prints "Sequence is empty" if seq has no elements, otherwise prints each element
     * }</pre>
     *
     * @param action the action to be executed if the sequence is empty
     * @return the current stream
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if action is null
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
                if (n <= 0) {
                    return;
                }

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
     * The action is executed for each element as it is consumed by a terminal operation,
     * allowing for side effects during sequence processing.
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5);
     * List<Integer> sideEffect = new ArrayList<>();
     * List<Integer> result = seq.onEach(sideEffect::add)
     *                           .filter(n -> n % 2 == 0)
     *                           .toList();
     * // result contains: [2, 4]
     * // sideEffect contains: [1, 2, 3, 4, 5] (all elements were processed)
     * }</pre>
     *
     * @param action the action to be performed on the elements pulled by downstream/terminal operation
     * @return this {@code Seq} instance
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if action is null
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
     * The action is executed only for the first element when it is consumed by a terminal operation.
     * Subsequent elements are not affected by this action.
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = Seq.of("first", "second", "third");
     * seq.onFirst(s -> System.out.println("Processing first: " + s))
     *    .forEach(System.out::println);
     * // Output:
     * // Processing first: first
     * // first
     * // second
     * // third
     * }</pre>
     *
     * @param action the action to be performed for the first element
     * @return this {@code Seq} instance
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if action is null
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
     * The action is executed only when the last element is consumed by a terminal operation.
     * The last element is detected when there are no more elements after it.
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = Seq.of("first", "second", "last");
     * seq.onLast(s -> System.out.println("Processing last: " + s))
     *    .forEach(System.out::println);
     * // Output:
     * // first
     * // second
     * // Processing last: last
     * // last
     * }</pre>
     *
     * @param action the action to be performed for the last element
     * @return this {@code Seq} instance
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if action is null
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
     * Performs the given action on the elements pulled by downstream/terminal operation. Most of the time, it's used for debugging.
     * This is an alias for {@link #onEach(Throwables.Consumer)}.
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5);
     * seq.peek(n -> System.out.println("Processing: " + n))
     *    .filter(n -> n % 2 == 0)
     *    .forEach(n -> System.out.println("Even number: " + n));
     * // Output:
     * // Processing: 1
     * // Processing: 2
     * // Even number: 2
     * // Processing: 3
     * // Processing: 4
     * // Even number: 4
     * // Processing: 5
     * }</pre>
     *
     * @param action the action to be performed on the elements pulled by downstream/terminal operation
     * @return this {@code Seq} instance
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if action is null
     * @see #onEach(Throwables.Consumer)
     */
    @IntermediateOp
    public Seq<T, E> peek(final Throwables.Consumer<? super T, ? extends E> action) throws IllegalStateException {
        return onEach(action);
    }

    /**
     * Performs the given action on the first element pulled by downstream/terminal operation. Most of the time, it's used for debugging.
     * This is an alias for {@link #onFirst(Throwables.Consumer)}.
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = Seq.of("first", "second", "third");
     * seq.peekFirst(s -> System.out.println("First element: " + s))
     *    .map(String::toUpperCase)
     *    .forEach(System.out::println);
     * // Output:
     * // First element: first
     * // FIRST
     * // SECOND
     * // THIRD
     * }</pre>
     *
     * @param action the action to be performed on the first element pulled by downstream/terminal operation
     * @return this {@code Seq} instance
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if action is null
     * @see #onFirst(Throwables.Consumer)
     */
    @IntermediateOp
    public Seq<T, E> peekFirst(final Throwables.Consumer<? super T, ? extends E> action) throws IllegalStateException {
        return onFirst(action);
    }

    /**
     * Performs the given action on the last element pulled by downstream/terminal operation. Most of the time, it's used for debugging.
     * This is an alias for {@link #onLast(Throwables.Consumer)}.
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = Seq.of("first", "second", "last");
     * seq.peekLast(s -> System.out.println("Last element: " + s))
     *    .map(String::toUpperCase)
     *    .forEach(System.out::println);
     * // Output:
     * // FIRST
     * // SECOND
     * // Last element: last
     * // LAST
     * }</pre>
     *
     * @param action the action to be performed on the last element pulled by downstream/terminal operation
     * @return this {@code Seq} instance
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if action is null
     * @see #onLast(Throwables.Consumer)
     */
    @IntermediateOp
    public Seq<T, E> peekLast(final Throwables.Consumer<? super T, ? extends E> action) throws IllegalStateException {
        return onLast(action);
    }

    /**
     * Performs the given action on the elements pulled by downstream/terminal operation which matches the given predicate. Most of the time, it's used for debugging.
     * Only elements that satisfy the predicate will have the action performed on them.
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5);
     * seq.peekIf(n -> n % 2 == 0, n -> System.out.println("Even number found: " + n))
     *    .toList();
     * // Output:
     * // Even number found: 2
     * // Even number found: 4
     * }</pre>
     *
     * @param predicate the predicate to test each element
     * @param action the action to be performed on the elements pulled by downstream/terminal operation which matches the given predicate
     * @return this {@code Seq} instance
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if predicate or action is null
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
     * Performs the given action on the elements pulled by downstream/terminal operation which matches the given predicate. Most of the time, it's used for debugging.
     * The predicate receives both the element and the count of iterated elements (starting from 1).
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = Seq.of("a", "b", "c", "d", "e");
     * seq.peekIf((s, index) -> index % 2 == 0, s -> System.out.println("Element at even position: " + s))
     *    .toList();
     * // Output:
     * // Element at even position: b
     * // Element at even position: d
     * }</pre>
     *
     * @param predicate the predicate to test each element. The first parameter is the element, and the second parameter is the count of iterated elements, starting with 1.
     * @param action the action to be performed on the elements pulled by downstream/terminal operation which matches the given predicate
     * @return this {@code Seq} instance
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if predicate or action is null
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

    /**
     * Returns a sequence of Lists, where each List contains a chunk of elements from the original stream.
     * The size of each chunk is specified by the chunkSize parameter. The final chunk may be smaller if there are not enough elements.
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5, 6, 7);
     * Seq<List<Integer>> chunks = seq.split(3);
     * // chunks contains: [[1, 2, 3], [4, 5, 6], [7]]
     * }</pre>
     *
     * @param chunkSize the desired size of each chunk (the last chunk may be smaller)
     * @return a sequence of Lists, each containing a chunk of elements from the original stream
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if chunkSize is less than or equal to 0
     */
    @IntermediateOp
    public Seq<List<T>, E> split(final int chunkSize) throws IllegalStateException {
        return split(chunkSize, IntFunctions.ofList());
    }

    /**
     * Splits the elements of this sequence into subsequences of the specified size. The last subsequence may be smaller than the specified size.
     * Each subsequence is collected into a collection of the specified type.
     *
     * <p>This is an intermediate operation that does not consume the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = Seq.of("a", "b", "c", "d", "e");
     * Seq<Set<String>> chunks = seq.split(2, HashSet::new);
     * // chunks contains: [{"a", "b"}, {"c", "d"}, {"e"}]
     * }</pre>
     *
     * @param <C> the type of the collection to hold the sub-sequences
     * @param chunkSize the desired size of each subsequence (the last subsequence may be smaller)
     * @param collectionSupplier a function that provides a new collection to hold each sub-sequence
     * @return a new sequence where each element is a collection containing a subsequence of the original elements
     * @throws IllegalStateException if the sequence is in an invalid state
     * @throws IllegalArgumentException if chunkSize is less than or equal to 0, or collectionSupplier is null
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
            public void advance(final long n) throws E {
                if (n <= 0) {
                    return;
                }

                elements.advance(n > Long.MAX_VALUE / chunkSize ? Long.MAX_VALUE : n * chunkSize);
            }
        }, closeHandlers);
    }

    /**
     * Splits the sequence into subsequences of the specified size and collects them using the provided collector.
     * Each subsequence is processed by the collector to produce a result of type R.
     *
     * <p>This is an intermediate operation and will not close the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5, 6);
     * Seq<String> joined = seq.split(2, Collectors.joining(","));
     * // joined contains: ["1,2", "3,4", "5,6"]
     * }</pre>
     *
     * @param <R> the type of the result
     * @param chunkSize the desired size of each subsequence (the last may be smaller)
     * @param collector the collector to use for collecting the subsequences
     * @return a new Seq where each element is the result of collecting a subsequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if chunkSize is less than or equal to 0, or collector is null
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
            public void advance(final long n) throws E {
                if (n <= 0) {
                    return;
                }

                elements.advance(n > Long.MAX_VALUE / chunkSize ? Long.MAX_VALUE : n * chunkSize);
            }
        }, closeHandlers);
    }

    /**
     * Splits the sequence into subsequences based on the given predicate.
     * Each subsequence contains consecutive elements that all satisfy or all don't satisfy the predicate.
     * Each subsequence is collected into a List.
     *
     * <p>This is an intermediate operation and will not close the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 3, 5, 2, 4, 6, 7, 9);
     * Seq<List<Integer>> groups = seq.split(n -> n % 2 == 0);
     * // groups contains: [[1, 3, 5], [2, 4, 6], [7, 9]]
     * }</pre>
     *
     * @param predicate the predicate to determine the boundaries of the subsequences
     * @return a new Seq where each element is a List of elements that satisfy the predicate
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if predicate is null
     */
    @IntermediateOp
    public Seq<List<T>, E> split(final Throwables.Predicate<? super T, ? extends E> predicate) throws IllegalStateException {
        assertNotClosed();

        return split(predicate, Suppliers.ofList());
    }

    /**
     * Splits the sequence into subsequences based on the given predicate.
     * Each subsequence contains consecutive elements that all satisfy or all don't satisfy the predicate.
     * Each subsequence is collected into a Collection of the specified type.
     *
     * <p>This is an intermediate operation and will not close the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = Seq.of("a", "ab", "abc", "d", "de");
     * Seq<Set<String>> groups = seq.split(s -> s.length() > 1, HashSet::new);
     * // groups contains: [{"a"}, {"ab", "abc"}, {"d"}, {"de"}]
     * }</pre>
     *
     * @param <C> the type of the Collection to collect the subsequences
     * @param predicate the predicate to determine the boundaries of the subsequences
     * @param collectionSupplier the supplier to provide the Collection to collect the subsequences
     * @return a new Seq where each element is a Collection of elements that satisfy the predicate
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if predicate or collectionSupplier is null
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
     * Each subsequence contains consecutive elements that all satisfy or all don't satisfy the predicate.
     *
     * <p>This is an intermediate operation and will not close the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 3, 5, 2, 4, 6, 7, 9);
     * Seq<String> joined = seq.split(n -> n % 2 == 0, Collectors.joining(","));
     * // joined contains: ["1,3,5", "2,4,6", "7,9"]
     * }</pre>
     *
     * @param <R> the type of the result
     * @param predicate the predicate to determine the boundaries of the subsequences
     * @param collector the collector to use for collecting the subsequences
     * @return a new Seq where each element is the result of collecting a subsequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if predicate or collector is null
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
     * <p>This is an intermediate operation and will not close the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5);
     * Seq<Seq<Integer, E>, E> split = seq.splitAt(3);
     * // split contains two sequences:
     * // First sequence: [1, 2, 3]
     * // Second sequence: [4, 5]
     * }</pre>
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
                if (n <= 0) {
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
     * <p>This is an intermediate operation and will not close the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5);
     * Seq<Seq<Integer, E>, E> split = seq.splitAt(n -> n > 3);
     * // split contains two sequences:
     * // First sequence: [1, 2, 3]
     * // Second sequence: [4, 5]
     * }</pre>
     *
     * @param where the predicate to determine the position at which to split the sequence
     * @return a new Seq containing two subsequences split at the position where the predicate returns true
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if where is null
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
                if (n <= 0) {
                    return;
                } else if ((n == 1) && (cursor == 0)) {
                    while (iter.hasNext()) {
                        next = iter.next();

                        if (where.test(next)) {
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

    /**
     * Creates a sliding window view of the sequence with the specified window size.
     * Each window is a list containing a subset of elements from the sequence.
     * Windows slide by 1 element at a time.
     *
     * <p>This is an intermediate operation and will not close the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5);
     * Seq<List<Integer>> windows = seq.sliding(3);
     * // windows contains: [[1, 2, 3], [2, 3, 4], [3, 4, 5]]
     * }</pre>
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
     * Windows slide by 1 element at a time.
     *
     * <p>This is an intermediate operation and will not close the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = Seq.of("a", "b", "c", "d");
     * Seq<Set<String>> windows = seq.sliding(2, HashSet::new);
     * // windows contains: [{"a", "b"}, {"b", "c"}, {"c", "d"}]
     * }</pre>
     *
     * @param <C> the type of the collection to be used for each window
     * @param windowSize the size of the sliding window
     * @param collectionSupplier a function that provides a new collection instance for each window
     * @return a new Seq where each element is a collection representing a sliding window of the original sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the window size is less than or equal to zero or collectionSupplier is null
     */
    @IntermediateOp
    public <C extends Collection<T>> Seq<C, E> sliding(final int windowSize, final IntFunction<? extends C> collectionSupplier)
            throws IllegalStateException, IllegalArgumentException {
        return sliding(windowSize, 1, collectionSupplier);
    }

    /**
     * Creates a sliding window view of the sequence with the specified window size and collector.
     * Each window is collected into a result container using the provided collector.
     * Windows slide by 1 element at a time.
     *
     * <p>This is an intermediate operation and will not close the sequence.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5);
     * Seq<String> windows = seq.sliding(3, Collectors.joining(","));
     * // windows contains: ["1,2,3", "2,3,4", "3,4,5"]
     * }</pre>
     *
     * @param <R> the type of the resulting elements
     * @param windowSize the size of the sliding window
     * @param collector a Collector that collects the elements of each window into a result container
     * @return a new Seq where each element is a result container representing a sliding window of the original sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the window size is less than or equal to zero or collector is null
     */
    @IntermediateOp
    public <R> Seq<R, E> sliding(final int windowSize, final Collector<? super T, ?, R> collector) throws IllegalStateException, IllegalArgumentException {
        return sliding(windowSize, 1, collector);
    }

    /**
     * Creates a sliding window view of the sequence with the specified window size and increment.
     * Each window is a list containing a subset of elements from the sequence.
     * 
     * <p>For example, {@code Seq.of(1, 2, 3, 4, 5).sliding(3, 1)} produces: {@code [[1, 2, 3], [2, 3, 4], [3, 4, 5]]}</p>
     * <p>And {@code Seq.of(1, 2, 3, 4, 5).sliding(3, 2)} produces: {@code [[1, 2, 3], [3, 4, 5]]}</p>
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param windowSize the size of the sliding window, must be greater than 0
     * @param increment the increment by which the window moves forward, must be greater than 0
     * @return a new Seq where each element is a list representing a sliding window of the original sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the window size or increment is less than or equal to zero
     * @see #sliding(int, int, IntFunction)
     */
    @IntermediateOp
    public Seq<List<T>, E> sliding(final int windowSize, final int increment) throws IllegalStateException, IllegalArgumentException {
        return sliding(windowSize, increment, IntFunctions.ofList());
    }

    /**
     * Creates a sliding window view of the sequence with the specified window size and increment.
     * Each window is a collection containing a subset of elements from the sequence.
     * The type of collection for each window is determined by the provided supplier.
     * 
     * <p>This method allows customization of the collection type used for windows. For example,
     * you can create windows as ArrayList, LinkedList, HashSet, etc.</p>
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param <C> the type of the collection to be returned for each window
     * @param windowSize the size of the sliding window, must be greater than 0
     * @param increment the increment by which the window moves forward, must be greater than 0
     * @param collectionSupplier a function that provides a new collection of type C for each window
     * @return a new Seq where each element is a collection representing a sliding window of the original sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the window size or increment is less than or equal to zero, or if collectionSupplier is null
     * @see #sliding(int, int)
     * @see #sliding(int, int, Collector)
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

                // Seq.of(1, 2, 3).sliding(2, 1) will return [[1, 2], [2, 3]], not [[1, 2], [2, 3], [3]]
                // But Seq.of(1).sliding(2, 1) will return [[1]], not []
                // Why? we need to check if the queue is not empty?
                // Not really, because elements.hasNext() is used to check if there are more elements to process.
                // In first case, elements.hasNext() will return false after processing [2, 3], so hasNext will return false.
                // In second case, elements.hasNext() will return true before processing the first element,
                return elements.hasNext(); // || (queue != null && !queue.isEmpty());
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

                while (cnt < windowSize && elements.hasNext()) {
                    next = elements.next();
                    result.add(next);
                    cnt++;

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
            public void advance(final long n) throws E {
                if (n <= 0) {
                    return;
                }

                if (increment >= windowSize) {
                    //noinspection DuplicateExpressions
                    elements.advance(n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                } else {
                    @SuppressWarnings("DuplicateExpressions")
                    final long m = (n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                    final int prevSize = queue == null ? 0 : queue.size(); //NOSONAR

                    if (m < prevSize) {
                        for (int i = 0; i < m; i++) {
                            queue.removeFirst();
                        }
                    } else {
                        if (N.notEmpty(queue)) {
                            queue.clear();
                        }

                        if (m - prevSize > 0) {
                            elements.advance(m - prevSize);
                        }
                    }
                }

                if (queue == null) {
                    queue = new ArrayDeque<>(windowSize);
                }

                final int countToKeepInQueue = windowSize - increment;
                int cnt = queue.size();

                while (cnt++ < countToKeepInQueue && elements.hasNext()) {
                    queue.add(elements.next());
                }
            }
        }, closeHandlers);
    }

    /**
     * Creates a sliding window view of the sequence with the specified window size and increment.
     * Each window is collected into a result using the provided collector.
     * 
     * <p>This method is useful when you need to apply complex aggregations to each window,
     * such as joining strings, computing statistics, or creating custom data structures.</p>
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param <R> the type of the result produced by the collector
     * @param windowSize the size of the sliding window, must be greater than 0
     * @param increment the increment by which the window moves forward, must be greater than 0
     * @param collector a Collector that collects the elements of each window into a result
     * @return a new Seq where each element is the result of collecting the elements of a sliding window
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the window size or increment is less than or equal to zero, or if collector is null
     * @see #sliding(int, int)
     * @see Collectors
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

                // Seq.of(1, 2, 3).sliding(2, 1) will return [[1, 2], [2, 3]], not [[1, 2], [2, 3], [3]]
                // But Seq.of(1).sliding(2, 1) will return [[1]], not []
                // Why? we need to check if the queue is not empty?
                // Not really, because elements.hasNext() is used to check if there are more elements to process.
                // In first case, elements.hasNext() will return false after processing [2, 3], so hasNext will return false.
                // In second case, elements.hasNext() will return true before processing the first element,
                return elements.hasNext(); // || (queue != null && !queue.isEmpty());
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

                while (cnt < windowSize && elements.hasNext()) {
                    next = elements.next();
                    accumulator.accept(container, next);
                    cnt++;

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
            public void advance(final long n) throws E {
                if (n <= 0) {
                    return;
                }

                if (increment >= windowSize) {
                    //noinspection DuplicateExpressions
                    elements.advance(n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                } else {
                    @SuppressWarnings("DuplicateExpressions")
                    final long m = (n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                    final int prevSize = queue == null ? 0 : queue.size(); //NOSONAR

                    if (m < prevSize) {
                        for (int i = 0; i < m; i++) {
                            queue.removeFirst();
                        }
                    } else {
                        if (N.notEmpty(queue)) {
                            queue.clear();
                        }

                        if (m - prevSize > 0) {
                            elements.advance(m - prevSize);
                        }
                    }
                }

                if (queue == null) {
                    queue = new ArrayDeque<>(windowSize);
                }

                final int countToKeepInQueue = windowSize - increment;
                int cnt = queue.size();

                while (cnt++ < countToKeepInQueue && elements.hasNext()) {
                    queue.add(elements.next());
                }
            }
        }, closeHandlers);
    }

    /**
     * Skips the first <i>n</i> elements of the sequence and returns a new sequence.
     * If <i>n</i> is greater than the number of elements in the sequence, an empty sequence is returned.
     * If <i>n</i> is 0, the original sequence is returned unchanged.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param n the number of elements to skip, must not be negative
     * @return a new sequence with the first <i>n</i> elements skipped
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if <i>n</i> is negative
     * @see #limit(long)
     * @see #skipLast(int)
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
                    elements.advance(n);
                }

                return elements.hasNext();
            }

            @Override
            public T next() throws E {
                if (!skipped) {
                    skipped = true;
                    elements.advance(n);
                }

                return elements.next();
            }
        }, sorted, cmp, closeHandlers);
    }

    /**
     * Skips the first <i>n</i> elements of the sequence and performs the given action on each skipped element.
     * This method is useful when you need to process skipped elements, such as for logging or statistics.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param n the number of elements to skip, must not be negative
     * @param actionOnSkippedItem the action to be performed on each skipped element
     * @return a new sequence where the first <i>n</i> elements are skipped and the action is performed on each skipped element
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if <i>n</i> is negative or actionOnSkippedItem is null
     * @see #skip(long)
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
     * Only {@code non-null} elements will be included in the returned sequence.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @return a new {@code Seq} instance with {@code null} elements removed
     * @see #filter(Throwables.Predicate)
     */
    @IntermediateOp
    public Seq<T, E> skipNulls() {
        return filter(Fnn.notNull());
    }

    /**
     * Skips the last <i>n</i> elements of the sequence and returns a new sequence.
     * This operation requires buffering up to <i>n</i> elements internally to determine which elements are the last ones.
     * If <i>n</i> is greater than the number of elements in the sequence, an empty sequence is returned.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param n the number of elements to skip from the end, must not be negative
     * @return a new sequence where the last <i>n</i> elements are skipped
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if <i>n</i> is negative
     * @see #skip(long)
     * @see #takeLast(int)
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

    /**
     * Limits the number of elements in the sequence to the specified maximum size.
     * If the sequence contains fewer elements than maxSize, all elements are included.
     * This is a short-circuiting stateful intermediate operation.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param maxSize the maximum number of elements to include in the sequence, must not be negative
     * @return a new sequence containing at most maxSize elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if <i>maxSize</i> is negative
     * @see #skip(long)
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

            @Override
            public void advance(final long n) throws E {
                if (n <= 0) {
                    return;
                }

                elements.advance(N.min(n, maxSize - cnt));

                cnt = n >= maxSize - cnt ? maxSize : cnt + n;
            }

        }, sorted, cmp, closeHandlers);
    }

    /**
     * Returns a new {@code Seq} consisting of the last {@code n} elements of this sequence.
     * A queue with size up to {@code n} will be maintained to filter out the last {@code n} elements.
     * It may cause <code>OutOfMemoryError</code> if {@code n} is big enough.
     *
     * <br />
     * All the elements will be loaded to get the last {@code n} elements and the sequence will be closed after that, if a terminal operation is triggered.
     *
     * @param n the number of elements to retain from the end of the sequence
     * @return a new {@code Seq} consisting of the last {@code n} elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if {@code n} is negative
     * @see Stream#takeLast(int)
     * @deprecated Use {@link #takeLast(int)} instead
     */
    @Deprecated
    @Beta
    @IntermediateOp
    public Seq<T, E> last(final int n) throws IllegalStateException, IllegalArgumentException {
        return takeLast(n);
    }

    /**
     * Returns a new {@code Seq} consisting of the last {@code n} elements of this sequence.
     * A queue with size up to {@code n} will be maintained to filter out the last {@code n} elements.
     * It may cause <code>OutOfMemoryError</code> if {@code n} is big enough.
     *
     * <p>All elements must be consumed to determine which are the last n elements.
     * The sequence will be closed after retrieving the last n elements when a terminal operation is triggered.</p>
     *
     * <br />
     * All the elements will be loaded to get the last {@code n} elements and the sequence will be closed after that, if a terminal operation is triggered.
     *
     * @param n the number of elements to retain from the end of the sequence, must not be negative
     * @return a new {@code Seq} consisting of the last {@code n} elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if {@code n} is negative
     * @see Stream#takeLast(int)
     * @see #skipLast(int)
     */
    @Beta
    @IntermediateOp
    public Seq<T, E> takeLast(final int n) throws IllegalStateException, IllegalArgumentException {
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
     * Returns a new {@code Seq} consisting of the top n elements of this {@code Seq}, according to the natural order of the elements.
     * If this {@code Seq} contains fewer elements than {@code n}, all elements are returned.
     * There is no guarantee on the order of the returned elements.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param n the number of top elements to retain, must be positive
     * @return a new {@code Seq} consisting of the top {@code n} elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if {@code n} is not positive
     * @see Stream#top(int)
     * @see #top(int, Comparator)
     */
    @IntermediateOp
    public Seq<T, E> top(final int n) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNegative(n, cs.n);

        return top(n, (Comparator<T>) Comparators.nullsFirst());
    }

    /**
     * Returns a {@code Seq} consisting of the top n elements of this {@code Seq} compared by the provided Comparator. 
     * If this {@code Seq} contains fewer elements than {@code n}, all elements are returned.
     * There is no guarantee on the order of the returned elements.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param n the number of top elements to retain, must be positive
     * @param comparator the comparator to compare the elements
     * @return a new {@code Seq} consisting of the top {@code n} elements according to the comparator
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if {@code n} is not positive
     * @see Stream#top(int, Comparator)
     * @see #kthLargest(int, Comparator)
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

                final long ret = to - cursor;
                cursor = to; // consume all elements
                return ret;
            }

            @Override
            public void advance(final long n) throws E {
                if (n <= 0) {
                    return;
                }

                if (!initialized) {
                    init();
                }

                cursor = n < to - cursor ? cursor + (int) n : to;
            }

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
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to reverse the order.</p>
     *
     * <p>This operation requires O(n) space where n is the number of elements in the sequence.</p>
     *
     * @return a new {@code Seq} with the elements in reverse order
     * @throws IllegalStateException if the sequence is already closed
     * @see #rotated(int)
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
                if (n <= 0) {
                    return;
                }

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
     * 
     * <p>This operation creates a new sequence with the elements rotated by the 
     * specified distance. If the distance is positive, the elements are rotated 
     * to the right. If the distance is negative, the elements are rotated to the left.
     * If the distance is zero, the sequence is not modified.</p>
     * 
     * <p>For example, rotating the sequence [1, 2, 3, 4, 5] by distance 2 results in 
     * [4, 5, 1, 2, 3]. Rotating by -1 results in [2, 3, 4, 5, 1].</p>
     * 
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the rotation.</p>
     *
     * @param distance the distance to rotate the elements. Positive values rotate to the 
     *                 right, negative values rotate to the left
     * @return a new {@code Seq} with the elements rotated by the specified distance
     * @throws IllegalStateException if the sequence is already closed
     * @see #reversed()
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
                if (n <= 0) {
                    return;
                }

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
     * Returns a new {@code Seq} with the elements shuffled in random order.
     * Uses the default random number generator.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the shuffle.</p>
     *
     * @return a new {@code Seq} with the elements shuffled
     * @throws IllegalStateException if the sequence is already closed
     * @see #shuffled(Random)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> shuffled() throws IllegalStateException {
        return shuffled(RAND);
    }

    /**
     * Returns a new {@code Seq} with the elements shuffled using the specified random number generator.
     * The shuffle algorithm used is Fisher-Yates shuffle.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the shuffle.</p>
     *
     * @param rnd the random number generator to use for shuffling the elements
     * @return a new {@code Seq} with the elements shuffled
     * @throws IllegalStateException if the sequence is already closed
     * @see #shuffled()
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
     * Returns a new {@code Seq} with the elements sorted in their natural order.
     * Nulls are considered smaller than {@code non-null} values.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the sort.</p>
     *
     * <p>Elements must implement {@code Comparable} or a {@code ClassCastException} will be thrown
     * when the terminal operation is executed.</p>
     *
     * @return a new {@code Seq} with the elements sorted
     * @throws IllegalStateException if the sequence is already closed
     * @see #sorted(Comparator)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> sorted() throws IllegalStateException {
        return sorted(NATURAL_COMPARATOR);
    }

    /**
     * Returns a new {@code Seq} with the elements sorted using the specified comparator.
     * A {@code null} comparator indicates natural ordering should be used.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the sort.</p>
     *
     * <p>If the sequence is already sorted with the same comparator, this operation is a no-op.</p>
     *
     * @param comparator the comparator to use for sorting the elements, or {@code null} for natural ordering
     * @return a new {@code Seq} with the elements sorted
     * @throws IllegalStateException if the sequence is already closed
     * @see #sorted()
     * @see #reverseSorted(Comparator)
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
     * This method provides better performance than using a general comparator when sorting by integer values.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the sort.</p>
     *
     * @param keyMapper a function that extracts an integer key from each element, which will be used for sorting
     * @return a new {@code Seq} with the elements sorted by the extracted integer key
     * @throws IllegalStateException if the sequence is already closed
     * @see #sorted(Comparator)
     * @see #sortedByLong(ToLongFunction)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> sortedByInt(final ToIntFunction<? super T> keyMapper) throws IllegalStateException {
        final Comparator<? super T> cmpToUse = Comparators.comparingInt(keyMapper);

        return sorted(cmpToUse);
    }

    /**
     * Returns a new {@code Seq} with the elements sorted according to long values extracted by the provided key extractor function.
     * This method provides better performance than using a general comparator when sorting by long values.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the sort.</p>
     *
     * @param keyMapper a function that extracts a long key from each element, which will be used for sorting
     * @return a new {@code Seq} with the elements sorted by the extracted long key
     * @throws IllegalStateException if the sequence is already closed
     * @see #sorted(Comparator)
     * @see #sortedByDouble(ToDoubleFunction)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> sortedByLong(final ToLongFunction<? super T> keyMapper) throws IllegalStateException {
        final Comparator<? super T> cmpToUse = Comparators.comparingLong(keyMapper);

        return sorted(cmpToUse);
    }

    /**
     * Returns a new {@code Seq} with the elements sorted according to double values extracted by the provided key extractor function.
     * This method provides better performance than using a general comparator when sorting by double values.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the sort.</p>
     *
     * @param keyMapper a function that extracts a double key from each element, which will be used for sorting
     * @return a new {@code Seq} with the elements sorted by the extracted double key
     * @throws IllegalStateException if the sequence is already closed
     * @see #sorted(Comparator)
     * @see #sortedBy(Function)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> sortedByDouble(final ToDoubleFunction<? super T> keyMapper) throws IllegalStateException {
        final Comparator<? super T> cmpToUse = Comparators.comparingDouble(keyMapper);

        return sorted(cmpToUse);
    }

    /**
     * Returns a new {@code Seq} with the elements sorted according to natural order of the values extracted by the provided key extractor function.
     * Nulls are considered smaller than {@code non-null} values.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the sort.</p>
     *
     * @param keyMapper a function that extracts a comparable key from each element, which will be used for sorting
     * @return a new {@code Seq} with the elements sorted by the extracted key
     * @throws IllegalStateException if the sequence is already closed
     * @see #sorted(Comparator)
     * @see Comparators#comparingBy(Function)
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
     * Returns a new {@code Seq} with the elements sorted in reverse natural order.
     * Nulls are considered bigger than {@code non-null} values in reverse order.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the sort.</p>
     *
     * @return a new {@code Seq} with the elements sorted in reverse order
     * @throws IllegalStateException if the sequence is already closed
     * @see #sorted()
     * @see #reverseSorted(Comparator)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> reverseSorted() throws IllegalStateException {
        return sorted(REVERSED_COMPARATOR);
    }

    /**
     * Returns a new {@code Seq} with the elements sorted in reverse order using the specified comparator.
     * The comparator's order is reversed before sorting.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the sort.</p>
     *
     * @param comparator the comparator to use for sorting the elements in reverse order
     * @return a new {@code Seq} with the elements sorted in reverse order
     * @throws IllegalStateException if the sequence is already closed
     * @see #sorted(Comparator)
     * @see Comparators#reverseOrder(Comparator)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> reverseSorted(final Comparator<? super T> comparator) throws IllegalStateException {
        final Comparator<? super T> cmpToUse = Comparators.reverseOrder(comparator);

        return sorted(cmpToUse);
    }

    /**
     * Returns a new {@code Seq} with the elements sorted in reverse order according to integer values extracted by the provided key extractor function.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the sort.</p>
     *
     * @param keyMapper a function that extracts an integer key from each element, which will be used for sorting
     * @return a new {@code Seq} with the elements sorted in reverse order by the extracted integer key
     * @throws IllegalStateException if the sequence is already closed
     * @see #sortedByInt(ToIntFunction)
     * @see #reverseSorted(Comparator)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> reverseSortedByInt(final ToIntFunction<? super T> keyMapper) throws IllegalStateException {
        final Comparator<? super T> cmpToUse = Comparators.reversedComparingInt(keyMapper);

        return sorted(cmpToUse);
    }

    /**
     * Returns a new {@code Seq} with the elements sorted in reverse order according to long values extracted by the provided key extractor function.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the sort.</p>
     *
     * @param keyMapper a function that extracts a long key from each element, which will be used for sorting
     * @return a new {@code Seq} with the elements sorted in reverse order by the extracted long key
     * @throws IllegalStateException if the sequence is already closed
     * @see #sortedByLong(ToLongFunction)
     * @see #reverseSorted(Comparator)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> reverseSortedByLong(final ToLongFunction<? super T> keyMapper) throws IllegalStateException {
        final Comparator<? super T> cmpToUse = Comparators.reversedComparingLong(keyMapper);

        return sorted(cmpToUse);
    }

    /**
     * Returns a new {@code Seq} with the elements sorted in reverse order according to double values extracted by the provided key extractor function.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the sort.</p>
     *
     * @param keyMapper a function that extracts a double key from each element, which will be used for sorting
     * @return a new {@code Seq} with the elements sorted in reverse order by the extracted double key
     * @throws IllegalStateException if the sequence is already closed
     * @see #sortedByDouble(ToDoubleFunction)
     * @see #reverseSorted(Comparator)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> reverseSortedByDouble(final ToDoubleFunction<? super T> keyMapper) throws IllegalStateException {
        final Comparator<? super T> cmpToUse = Comparators.reversedComparingDouble(keyMapper);

        return sorted(cmpToUse);
    }

    /**
     * Returns a new {@code Seq} with the elements sorted in reverse order according to natural order of the values extracted by the provided key extractor function.
     * Nulls are considered bigger than {@code non-null} values in reverse order.
     *
     * <p>This is an intermediate operation that triggers terminal evaluation. All elements will be
     * loaded into memory to perform the sort.</p>
     *
     * @param keyMapper a function that extracts a comparable key from each element, which will be used for sorting
     * @return a new {@code Seq} with the elements sorted in reverse order by the extracted key
     * @throws IllegalStateException if the sequence is already closed
     * @see #sortedBy(Function)
     * @see #reverseSorted(Comparator)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public Seq<T, E> reverseSortedBy(@SuppressWarnings("rawtypes") final Function<? super T, ? extends Comparable> keyMapper) throws IllegalStateException {
        final Comparator<? super T> cmpToUse = Comparators.reversedComparingBy(keyMapper);

        return sorted(cmpToUse);
    }

    private Seq<T, E> lazyLoad(final UnaryOperator<Object[]> op, final boolean sorted, final Comparator<? super T> cmp) {
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
            public void advance(final long n) throws E {
                if (n <= 0) {
                    return;
                }

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
     * When the end of the sequence is reached, it starts again from the beginning.
     * The original elements are cached after the first iteration.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @return a new {@code Seq} that cycles through the elements indefinitely
     * @throws IllegalStateException if the sequence is already closed
     * @see #cycled(long)
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
     * Each round consists of iterating through all elements once.
     * If rounds is 0, an empty sequence is returned.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param rounds the number of times to cycle through the elements, must not be negative
     * @return a new {@code Seq} that cycles through the elements for the specified number of rounds
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if rounds is negative
     * @see #cycled()
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
     * Each element emission will be throttled according to the rate limit.
     * Uses a token bucket algorithm for rate limiting.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param permitsPerSecond the number of permits per second to allow, must be positive
     * @return a new {@code Seq} that is rate-limited to the specified number of permits per second
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if permitsPerSecond is not positive
     * @see RateLimiter
     * @see #rateLimited(RateLimiter)
     */
    @IntermediateOp
    public Seq<T, E> rateLimited(final double permitsPerSecond) throws IllegalStateException {
        checkArgPositive(permitsPerSecond, cs.permitsPerSecond);

        return rateLimited(RateLimiter.create(permitsPerSecond));
    }

    /**
     * Returns a new {@code Seq} that is rate-limited using the specified {@link RateLimiter}.
     * Each element emission will acquire a permit from the rate limiter before proceeding.
     * This method is useful for controlling the rate of processing in the sequence pipeline.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param rateLimiter the rate limiter to use
     * @return a new {@code Seq} that is rate-limited to the specified rate limiter
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the rate limiter is null
     * @see #rateLimited(double)
     * @see #delay(Duration)
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
     * The delay is applied before emitting each element after the first one.
     * This method is useful for simulating time-based processing or avoiding overwhelming downstream systems.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param duration the duration to delay each element
     * @return a new {@code Seq} with each element delayed by the specified duration
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the duration is null
     * @see #rateLimited(double)
     */
    @IntermediateOp
    public Seq<T, E> delay(final Duration duration) throws IllegalStateException, IllegalArgumentException {
        assertNotClosed();
        checkArgNotNull(duration, cs.delay);

        final long millis = duration.toMillis();
        final Throwables.Consumer<T, E> action = it -> N.sleepUninterruptibly(millis);

        return onEach(action);
    }

    /**
     * Delays each element in this {@code Seq} by the given {@link Duration} except the first element.
     * The delay is applied before emitting each element after the first one.
     * This method is useful for simulating time-based processing or avoiding overwhelming downstream systems.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @param duration the duration to delay each element
     * @return a new {@code Seq} with each element delayed by the specified duration
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the duration is null
     * @see #rateLimited(double)
     */
    @IntermediateOp
    public Seq<T, E> delay(final java.time.Duration duration) throws IllegalStateException, IllegalArgumentException {
        return delay(Duration.ofMillis(duration.toMillis()));
    }

    /**
     * Intersperses the specified delimiter between each element of this {@code Seq}.
     * The delimiter appears between consecutive elements but not before the first element or after the last element.
     * 
     * <p>For example, {@code Seq.of(1, 2, 3).intersperse(0)} returns {@code [1, 0, 2, 0, 3]}</p>
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
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
     * <p>This is an intermediate operation that creates a new sequence containing every n-th element from the original
     * sequence, where n is the specified step size. The first element is always included.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Returns [1, 3, 5]
     * Seq.of(1, 2, 3, 4, 5).step(2).toList()
     * 
     * // Returns [1, 4]
     * Seq.of(1, 2, 3, 4, 5).step(3).toList()
     * }</pre>
     *
     * @param step the step size to use when iterating through the elements. Must be greater than 0.
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
     * Returns a new {@code Seq} where each element is paired with its index.
     * <p>This is an intermediate operation that transforms each element into an {@link Indexed} object
     * containing the original element and its zero-based position in the sequence.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Returns [Indexed{value=a, index=0}, Indexed{value=b, index=1}, Indexed{value=c, index=2}]
     * Seq.of("a", "b", "c").indexed().toList()
     * }</pre>
     *
     * @return a new {@code Seq} where each element is wrapped in an {@link Indexed} object with its index
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
     * <p>This is an intermediate operation that creates a buffered sequence where elements are read from the source
     * sequence in a separate thread and stored in a queue. This is useful for decoupling slow consumers from fast
     * producers or when implementing read-write with different threads pattern.</p>
     * 
     * <p>The default queue size is 64. If the queue becomes full, the producer thread will block until space becomes available.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Buffer elements for smoother processing
     * seq.buffered().forEach(element -> processSlowly(element));
     * }</pre>
     *
     * @return a new {@code Seq} with elements read asynchronously into a buffer
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
     * <p>This is an intermediate operation that creates a buffered sequence where elements are read from the source
     * sequence in a separate thread and stored in a queue of the specified size. This is useful for decoupling slow
     * consumers from fast producers or when implementing read-write with different threads pattern.</p>
     * 
     * <p>If the queue becomes full, the producer thread will block until space becomes available.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Buffer up to 100 elements
     * seq.buffered(100).forEach(element -> processSlowly(element));
     * }</pre>
     *
     * @param bufferSize the size of the buffer queue. Must be greater than 0.
     * @return a new {@code Seq} with elements read asynchronously into a buffer of the specified size
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
     * <p>This is an intermediate operation that combines elements from this sequence and the provided collection
     * based on the merge strategy determined by the selector function. The selector function is called for each
     * pair of elements and returns a {@link MergeResult} indicating which element(s) to include in the output.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq1 = Seq.of(1, 3, 5);
     * List<Integer> list2 = Arrays.asList(2, 4, 6);
     * 
     * // Merge in ascending order: [1, 2, 3, 4, 5, 6]
     * seq1.mergeWith(list2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     * }</pre>
     *
     * @param b the collection to merge with this sequence
     * @param nextSelector a BiFunction that takes an element from this sequence and an element from the collection,
     *                     and returns a MergeResult indicating which element(s) to select
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
     * <p>This is an intermediate operation that combines elements from two sequences based on the merge strategy
     * determined by the selector function. The selector function is called for each pair of elements and returns
     * a {@link MergeResult} indicating which element(s) to include in the output.</p>
     * 
     * <p>The merge process continues until both sequences are exhausted. The MergeResult values control the behavior:</p>
     * <ul>
     * <li>TAKE_FIRST: Take element from the first sequence only</li>
     * <li>TAKE_SECOND: Take element from the second sequence only</li>
     * <li>TAKE_BOTH: Take both elements (first sequence element followed by second)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq1 = Seq.of(1, 3, 5);
     * Seq<Integer> seq2 = Seq.of(2, 4, 6);
     * 
     * // Merge in ascending order: [1, 2, 3, 4, 5, 6]
     * seq1.mergeWith(seq2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
     * }</pre>
     *
     * @param b the sequence to merge with this sequence
     * @param nextSelector a BiFunction that takes an element from each sequence and returns a MergeResult
     *                     indicating which element(s) to select
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
     * Zips this stream with the given collection using the provided zip function.
     * The zip function takes elements from this stream and the given collection until either the current stream or the given collection runs out of elements.
     * The resulting stream will have the length of the shorter of the current stream and the given collection.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq.of("a", "b", "c")
     *    .zipWith(Arrays.asList(1, 2, 3, 4),
     *             (s, i) -> s + i)
     *    .toList(); // Returns ["a1", "b2", "c3"]
     * }</pre>
     *
     * @param <T2> the type of elements in the given Collection
     * @param <R> the type of elements in the resulting Seq
     * @param b the Collection to be combined with the current Seq. Must be {@code non-null}.
     * @param zipFunction a BiFunction that determines the combination of elements in the combined Seq. Must be {@code non-null}.
     * @return a new Seq that is the result of combining the current Seq with the given Collection
     * @throws IllegalStateException if the sequence is already closed
     * @see #zipWith(Collection, Object, Object, Throwables.BiFunction)
     * @see N#zip(Iterable, Iterable, BiFunction)
     */
    @IntermediateOp
    public <T2, R> Seq<R, E> zipWith(final Collection<T2> b, final Throwables.BiFunction<? super T, ? super T2, ? extends R, ? extends E> zipFunction)
            throws IllegalStateException {
        assertNotClosed();

        return zip(this, Seq.of(b), zipFunction);
    }

    /**
     * Zips this stream with the given collection using the provided zip function.
     * The zip function combines elements from this stream and the given collection until both the current stream or the given collection runs out of elements.
     * The resulting stream will have the length of the longer of the current stream and the given collection.
     * If the current stream or the given collection runs out of elements before the other, the provided default values are used.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq.of("a", "b")
     *    .zipWith(Arrays.asList(1, 2, 3),
     *             "z", 0,
     *             (s, i) -> s + i)
     *    .toList(); // Returns ["a1", "b2", "z3"]
     * }</pre>
     *
     * @param <T2> the type of elements in the given Collection
     * @param <R> the type of elements in the resulting Seq
     * @param b the Collection to be combined with the current Seq. Must be {@code non-null}.
     * @param valueForNoneA the default value to use for the current Seq when it runs out of elements
     * @param valueForNoneB the default value to use for the Collection when it runs out of elements
     * @param zipFunction a BiFunction that determines the combination of elements in the combined Seq. Must be {@code non-null}.
     * @return a new Seq that is the result of combining the current Seq with the given Collection
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
     * Zips this stream with the given collections using the provided zip function.
     * The zip function takes elements from this stream and the given collections until either the current stream or one of the given collections runs out of elements.
     * The resulting stream will have the length of the shortest of the current stream and the given collections.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq.of("a", "b", "c")
     *    .zipWith(Arrays.asList(1, 2),
     *             Arrays.asList(true, false, true),
     *             (s, i, b) -> s + i + b)
     *    .toList(); // Returns ["a1true", "b2false"]
     * }</pre>
     *
     * @param <T2> the type of elements in the first given Collection
     * @param <T3> the type of elements in the second given Collection
     * @param <R> the type of elements in the resulting Seq
     * @param b the first Collection to be combined with the current Seq. Must be {@code non-null}.
     * @param c the second Collection to be combined with the current Seq. Must be {@code non-null}.
     * @param zipFunction a TriFunction that determines the combination of elements in the combined Seq. Must be {@code non-null}.
     * @return a new Seq that is the result of combining the current Seq with the given Collections
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
     * Zips this stream with the given collections using the provided zip function.
     * The zip function combines elements from this stream and the given collections until both the current stream or the given collections runs out of elements.
     * The resulting stream will have the length of the longest of the current stream and the given collections.
     * If the current stream or one of the given collections runs out of elements before the other, the provided default values are used.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq.of("a", "b")
     *    .zipWith(Arrays.asList(1),
     *             Arrays.asList(true, false, true),
     *             "z", 0, false,
     *             (s, i, b) -> s + i + b)
     *    .toList(); // Returns ["a1true", "b0false", "z0true"]
     * }</pre>
     *
     * @param <T2> the type of elements in the first given Collection
     * @param <T3> the type of elements in the second given Collection
     * @param <R> the type of elements in the resulting Seq
     * @param b the first Collection to be combined with the current Seq. Must be {@code non-null}.
     * @param c the second Collection to be combined with the current Seq. Must be {@code non-null}.
     * @param valueForNoneA the default value to use for the current Seq when it runs out of elements
     * @param valueForNoneB the default value to use for the first Collection when it runs out of elements
     * @param valueForNoneC the default value to use for the second Collection when it runs out of elements
     * @param zipFunction a TriFunction that determines the combination of elements in the combined Seq. Must be {@code non-null}.
     * @return a new Seq that is the result of combining the current Seq with the given Collections
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
     * Zips this stream with the given stream using the provided zip function.
     * The zip function takes elements from this stream and the given stream until either the current stream or the given stream runs out of elements.
     * The resulting stream will have the length of the shorter of the current stream and the given stream.
     * This is an intermediate operation.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq.of(1, 2, 3)
     *    .zipWith(Seq.of("a", "b", "c", "d"),
     *             (i, s) -> i + s)
     *    .toList(); // Returns ["1a", "2b", "3c"]
     * }</pre>
     *
     * @param <T2> the type of elements in the given Seq
     * @param <R> the type of elements in the resulting Seq
     * @param b the Seq to be combined with the current Seq. Must be {@code non-null}.
     * @param zipFunction a BiFunction that determines the combination of elements in the combined Seq. Must be {@code non-null}.
     * @return a new Seq that is the result of combining the current Seq with the given Seq
     * @throws IllegalStateException if the sequence is already closed
     * @see #zipWith(Seq, Object, Object, Throwables.BiFunction)
     * @see N#zip(Iterable, Iterable, BiFunction)
     */
    @IntermediateOp
    public <T2, R> Seq<R, E> zipWith(final Seq<T2, E> b, final Throwables.BiFunction<? super T, ? super T2, ? extends R, ? extends E> zipFunction)
            throws IllegalStateException {
        assertNotClosed();

        return zip(this, b, zipFunction);
    }

    /**
     * Zips this stream with the given stream using the provided zip function, with default values for missing elements.
     * This is an intermediate operation that combines elements from two streams pairwise.
     *
     * <p>The resulting stream will have the length of the longer of the two streams.
     * Default values are used when one stream runs out of elements before the other.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq.of(1, 2, 3)
     *    .zipWith(Seq.of("a"), 0, "z", (i, s) -> i + s)
     *    .toList(); // Returns ["1a", "2z", "3z"]
     * }</pre>
     *
     * @param <T2> the type of elements in the given Seq
     * @param <R> the type of elements in the resulting Seq
     * @param b the Seq to be combined with the current Seq. Will be closed along with this Seq.
     * @param valueForNoneA the default value to use for the current Seq when it runs out of elements
     * @param valueForNoneB the default value to use for the given Seq when it runs out of elements
     * @param zipFunction a BiFunction that determines the combination of elements in the combined Seq. Must be {@code non-null}.
     * @return a new Seq that is the result of combining the current Seq with the given Seq
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
     * Zips this stream with two other streams using the provided zip function.
     * This is an intermediate operation that combines elements from three streams.
     *
     * <p>The resulting stream will have the length of the shortest of the three streams.
     * If any stream is longer, its remaining elements are ignored.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq.of(1, 2, 3)
     *    .zipWith(Seq.of("a", "b"), Seq.of(true, false),
     *             (i, s, b) -> i + s + b)
     *    .toList(); // Returns ["1atrue", "2bfalse"]
     * }</pre>
     *
     * @param <T2> the type of elements in the second Seq
     * @param <T3> the type of elements in the third Seq
     * @param <R> the type of elements in the resulting Seq
     * @param b the second Seq to be combined with the current Seq. Will be closed along with this Seq.
     * @param c the third Seq to be combined with the current Seq. Will be closed along with this Seq.
     * @param zipFunction a TriFunction that determines the combination of elements in the combined Seq. Must be {@code non-null}.
     * @return a new Seq that is the result of combining the current Seq with the given Seqs
     * @throws IllegalStateException if the sequence is already closed
     * @see #zipWith(Seq, Seq, Object, Object, Object, Throwables.TriFunction)
     * @see N#zip(Iterable, Iterable, Iterable, TriFunction)
     */
    @IntermediateOp
    public <T2, T3, R> Seq<R, E> zipWith(final Seq<T2, E> b, final Seq<T3, E> c,
            final Throwables.TriFunction<? super T, ? super T2, ? super T3, ? extends R, ? extends E> zipFunction) throws IllegalStateException {
        assertNotClosed();

        return zip(this, b, c, zipFunction);
    }

    /**
     * Zips this stream with two other streams using the provided zip function, with default values for missing elements.
     * This is an intermediate operation that combines elements from three streams.
     *
     * <p>The resulting stream will have the length of the longest of the three streams.
     * Default values are used when any stream runs out of elements before the others.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq.of(1, 2, 3)
     *    .zipWith(Seq.of("a"), Seq.of(true),
     *             0, "z", false,
     *             (i, s, b) -> i + s + b)
     *    .toList(); // Returns ["1atrue", "2zfalse", "3zfalse"]
     * }</pre>
     *
     * @param <T2> the type of elements in the second Seq
     * @param <T3> the type of elements in the third Seq
     * @param <R> the type of elements in the resulting Seq
     * @param b the second Seq to be combined with the current Seq. Will be closed along with this Seq.
     * @param c the third Seq to be combined with the current Seq. Will be closed along with this Seq.
     * @param valueForNoneA the default value to use for the current Seq when it runs out of elements
     * @param valueForNoneB the default value to use for the second Seq when it runs out of elements
     * @param valueForNoneC the default value to use for the third Seq when it runs out of elements
     * @param zipFunction a TriFunction that determines the combination of elements in the combined Seq. Must be {@code non-null}.
     * @return a new Seq that is the result of combining the current Seq with the given Seqs
     * @throws IllegalStateException if the sequence is already closed
     * @see N#zip(Iterable, Iterable, Iterable, Object, Object, Object, TriFunction)
     */
    @IntermediateOp
    public <T2, T3, R> Seq<R, E> zipWith(final Seq<T2, E> b, final Seq<T3, E> c, final T valueForNoneA, final T2 valueForNoneB, final T3 valueForNoneC,
            final Throwables.TriFunction<? super T, ? super T2, ? super T3, ? extends R, ? extends E> zipFunction) throws IllegalArgumentException {
        assertNotClosed();

        return zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    /**
     * Performs the given action for each element of this sequence.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p>This method is an alias for {@link #forEach(Throwables.Consumer)} that accepts a regular {@link Consumer}
     * instead of a {@link Throwables.Consumer}.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * seq.foreach(System.out::println);
     * 
     * // Equivalent to:
     * seq.forEach(System.out::println);
     * }</pre>
     *
     * @param action a non-interfering action to perform on each element
     * @throws E if an exception occurs during iteration
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
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p>The action can throw checked exceptions of type E2, which will be propagated to the caller.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Simple printing
     * seq.forEach(System.out::println);
     * 
     * // With exception handling
     * seq.forEach(element -> {
     *     processElement(element); // may throw IOException
     * });
     * }</pre>
     *
     * @param <E2> the type of exception that the action may throw
     * @param action a non-interfering action to perform on each element
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
     * Performs the given action for each element of this sequence, providing the element's index.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq.of("a", "b", "c").forEachIndexed((index, value) -> 
     *     System.out.println(index + ": " + value));
     * // Output:
     * // 0: a
     * // 1: b
     * // 2: c
     * }</pre>
     *
     * @param <E2> the type of exception that the action may throw
     * @param action a non-interfering action that accepts an index and element
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
     * Iterates and executes the given action until the flag is set to {@code true}.  
     * <p>The flag can only be set after at least one element has been processed. Once the flag is set to
     * {@code true}, iteration stops and the sequence is closed.</p>
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Process elements until sum exceeds 100
     * MutableInt sum = MutableInt.of(0);
     * seq.forEachUntil((value, flag) -> {
     *     sum.add(value);
     *     if (sum.value() > 100) {
     *         flag.setTrue();
     *     }
     * });
     * }</pre>
     *
     * @param <E2> the type of exception that the action may throw
     * @param action a BiConsumer that takes an element and a MutableBoolean flag.
     *               Set the flag to {@code true} to break the loop.
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
     * Iterates and executes the action until the flag is set to {@code true}.  
     * <p>This method is equivalent to {@code takeWhile(value -> flagToBreak.isFalse()).forEach(action)}.</p>
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * MutableBoolean stopFlag = MutableBoolean.of(false);
     * 
     * // In another thread or timer:
     * // stopFlag.setTrue();
     * 
     * seq.forEachUntil(stopFlag, element -> {
     *     processElement(element);
     * });
     * }</pre>
     *
     * @param <E2> the type of exception that the action may throw
     * @param flagToBreak a MutableBoolean flag to control iteration. Set to {@code true} to stop.
     * @param action a Consumer to be applied to each element while the flag is {@code false}
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
     * Performs the given action for each element of this sequence, and then performs the provided onComplete action. 
     * <p>The onComplete action is executed even if no elements are present in the sequence, but is not executed
     * if an exception occurs during element processing.</p>
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> results = new ArrayList<>();
     * seq.forEach(
     *     element -> results.add(process(element)),
     *     () -> System.out.println("Processed " + results.size() + " items")
     * );
     * }</pre>
     *
     * @param <E2> the type of exception that the action may throw
     * @param <E3> the type of exception that the onComplete action may throw
     * @param action a non-interfering action to perform on each element
     * @param onComplete a Runnable to execute after all elements have been processed
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
     * Iterates over the elements, applying a flat-mapping function to each element and executing an action on the results.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Print all person-hobby combinations
     * Seq<Person> people = Seq.of(person1, person2);
     * people.forEach(
     *     person -> person.getHobbies(),
     *     (person, hobby) -> System.out.println(person.getName() + " enjoys " + hobby)
     * );
     * }</pre>
     *
     * @param <U> the type of elements in the iterable returned by the flat-mapper
     * @param <E2> the type of exception that the flat-mapper may throw
     * @param <E3> the type of exception that the action may throw
     * @param flatMapper a function that returns an Iterable for each element
     * @param action a BiConsumer that processes the original element and each item from the Iterable
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
     * Iterates over the elements, applying two levels of flat-mapping and executing an action on the results.
     * <p>This is a terminal operation that combines two levels of flat-mapping with forEach. For each element,
     * the first flatMapper produces an Iterable, then for each item in that Iterable, the second flatMapper
     * produces another Iterable. The action is executed for each combination of the original element and
     * the items from both levels of flat-mapping.</p>
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Print all company-department-employee combinations
     * Seq<Company> companies = Seq.of(company1, company2);
     * companies.forEach(
     *     company -> company.getDepartments(),
     *     department -> department.getEmployees(),
     *     (company, department, employee) -> 
     *         System.out.println(company.getName() + " - " + 
     *                          department.getName() + " - " + 
     *                          employee.getName())
     * );
     * }</pre>
     *
     * @param <T2> the type of elements in the first level iterable
     * @param <T3> the type of elements in the second level iterable
     * @param <E2> the type of exception that the first flat-mapper may throw
     * @param <E3> the type of exception that the second flat-mapper may throw
     * @param <E4> the type of exception that the action may throw
     * @param flatMapper a function that returns an Iterable of T2 for each element
     * @param flatMapper2 a function that returns an Iterable of T3 for each T2
     * @param action a TriConsumer that processes the original element, T2, and T3
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if any of the parameters is null
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
     * Performs the given action on each adjacent pair of elements in this sequence.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p>If the sequence has fewer than 2 elements, no action is performed.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq.of(1, 3, 6, 10).forEachPair((a, b) -> 
     *     System.out.println(b + " - " + a + " = " + (b - a)));
     * // Output:
     * // 3 - 1 = 2
     * // 6 - 3 = 3
     * // 10 - 6 = 4
     * }</pre>
     *
     * @param <E2> type of exception that might be thrown from the action
     * @param action a non-interfering action to perform on each adjacent pair of elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the action is null
     * @throws E if an exception occurs during iteration
     * @throws E2 if the action throws an exception
     */
    @TerminalOp
    public <E2 extends Exception> void forEachPair(final Throwables.BiConsumer<? super T, ? super T, E2> action)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        forEachPair(1, action);
    }

    /**
     * Performs the given action on pairs of elements with the specified increment between pairs.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p>For example, with sequence [a, b, c, d, e, f] and increment=2:</p>
     * <ul>
     * <li>Pairs processed: (a,b), (c,d), (e,f)</li>
     * </ul>
     *
     * <p>With the same sequence and increment=1:</p>
     * <ul>
     * <li>Pairs processed: (a,b), (b,c), (c,d), (d,e), (e,f)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Process non-overlapping pairs
     * Seq.of(1, 2, 3, 4, 5, 6).forEachPair(2, (a, b) -> 
     *     System.out.println("(" + a + "," + b + ")"));
     * // Output: (1,2), (3,4), (5,6)
     * }</pre>
     *
     * @param <E2> type of exception that might be thrown from the action
     * @param increment the distance between the first elements of each pair (must be positive)
     * @param action a non-interfering action to perform on each pair of elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the action is {@code null} or increment is not positive
     * @throws E if an exception occurs during iteration
     * @throws E2 if the action throws an exception
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
     * Performs the given action on each adjacent triple of elements in this sequence.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p>If the sequence has fewer than 3 elements, no action is performed.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq.of(1, 2, 3, 4, 5).forEachTriple((a, b, c) -> 
     *     System.out.println("(" + a + "," + b + "," + c + ") -> avg = " + (a+b+c)/3.0));
     * // Output:
     * // (1,2,3) -> avg = 2.0
     * // (2,3,4) -> avg = 3.0
     * // (3,4,5) -> avg = 4.0
     * }</pre>
     *
     * @param <E2> type of exception that might be thrown from the action
     * @param action a non-interfering action to perform on each adjacent triple of elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the action is null
     * @throws E if an exception occurs during iteration
     * @throws E2 if the action throws an exception
     */
    @TerminalOp
    public <E2 extends Exception> void forEachTriple(final Throwables.TriConsumer<? super T, ? super T, ? super T, E2> action)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        forEachTriple(1, action);
    }

    /**
     * Performs the given action on triples of elements with the specified increment between triples.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p>For example, with sequence [a, b, c, d, e, f, g, h, i] and increment=3:</p>
     * <ul>
     * <li>Triples processed: (a,b,c), (d,e,f), (g,h,i)</li>
     * </ul>
     *
     * <p>With the same sequence and increment=1:</p>
     * <ul>
     * <li>Triples processed: (a,b,c), (b,c,d), (c,d,e), (d,e,f), (e,f,g), (f,g,h), (g,h,i)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Process non-overlapping triples
     * Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9).forEachTriple(3, (a, b, c) -> 
     *     System.out.println("(" + a + "," + b + "," + c + ")"));
     * // Output: (1,2,3), (4,5,6), (7,8,9)
     * }</pre>
     *
     * @param <E2> type of exception that might be thrown from the action
     * @param increment the distance between the first elements of each triple (must be positive)
     * @param action a non-interfering action to perform on each triple of elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the action is {@code null} or increment is not positive
     * @throws E if an exception occurs during iteration
     * @throws E2 if the action throws an exception
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

    /**
     * Returns an Optional containing the minimum element according to the provided comparator.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p>If the sequence is already sorted with the same comparator, this operation will return the first
     * element for optimal performance.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Optional<String> shortest = Seq.of("apple", "pie", "banana")
     *     .min(Comparator.comparingInt(String::length));
     * // Returns Optional.of("pie")
     * 
     * Optional<Integer> min = Seq.<Integer>empty().min(Comparator.naturalOrder());
     * // Returns Optional.empty()
     * }</pre>
     *
     * @param comparator the comparator to determine the order of elements
     * @return an Optional containing the minimum element, or empty if the sequence is empty
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
            } else if (sorted && isSameComparator(comparator, this.cmp)) {
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
     * Returns an Optional containing the minimum element according to the key extracted by the keyMapper function.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p>This is a convenience method equivalent to calling {@code min(Comparators.nullsLastBy(keyMapper))}.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Optional<Person> youngest = people.minBy(Person::getAge);
     * 
     * Optional<String> shortest = words.minBy(String::length);
     * }</pre>
     *
     * @param keyMapper the function to extract the comparable key for comparison
     * @return an Optional containing the element with the minimum key, or empty if the sequence is empty
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
     * Returns an {@code Optional} containing the maximum element of this sequence according to the provided comparator.
     * Returns an empty {@code Optional} if this sequence is empty.
     * 
     * <p>This is a <b>terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(3, 1, 4, 1, 5);
     * Optional<Integer> max = seq.max(Integer::compare);
     * // max.get() == 5
     * }</pre>
     * 
     * @param comparator a non-interfering, stateless {@code Comparator} to compare elements of this sequence.
     *                   If {@code null}, natural ordering is used
     * @return an {@code Optional} describing the maximum element of this sequence, or an empty {@code Optional} if the sequence is empty
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws E if an exception occurs during the computation
     * @see #min(Comparator)
     * @see #maxBy(Function)
     */
    @TerminalOp
    public Optional<T> max(Comparator<? super T> comparator) throws IllegalStateException, E {
        assertNotClosed();

        try {
            if (!elements.hasNext()) {
                return Optional.empty();
            }

            comparator = comparator == null ? (Comparator<T>) Comparators.nullsFirst() : comparator;
            T candidate = elements.next();

            while (elements.hasNext()) {
                final T next = elements.next();

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
     * Returns an {@code Optional} containing the maximum element of this sequence according to the comparable value
     * extracted by the provided key mapper function. Null values are considered to be less than {@code non-null} values.
     * Returns an empty {@code Optional} if this sequence is empty.
     * 
     * <p>This is a <b>terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = Seq.of("apple", "pie", "banana");
     * Optional<String> longest = seq.maxBy(String::length);
     * // longest.get() == "banana"
     * }</pre>
     * 
     * @param keyMapper the function used to extract the {@code Comparable} sort key from each element
     * @return an {@code Optional} describing the element with the maximum extracted key value, 
     *         or an empty {@code Optional} if the sequence is empty
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws IllegalArgumentException if the specified keyMapper is {@code null}
     * @throws E if an exception occurs during the computation
     * @see #max(Comparator)
     * @see #minBy(Function)
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
     * Returns whether any elements of this sequence match the provided predicate.
     * Returns {@code false} if the sequence is empty.
     * 
     * <p>This is a <b>short-circuiting terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5);
     * boolean hasEven = seq.anyMatch(n -> n % 2 == 0);
     * // hasEven == true
     * }</pre>
     * 
     * @param <E2> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements of this sequence
     * @return {@code true} if any elements of the sequence match the provided predicate, otherwise {@code false}
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if the predicate throws an exception
     * @see #allMatch(Throwables.Predicate)
     * @see #noneMatch(Throwables.Predicate)
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
     * Returns whether all elements of this sequence match the provided predicate.
     * Returns {@code true} if the sequence is empty.
     * 
     * <p>This is a <b>short-circuiting terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(2, 4, 6, 8);
     * boolean allEven = seq.allMatch(n -> n % 2 == 0);
     * // allEven == true
     * }</pre>
     * 
     * @param <E2> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements of this sequence
     * @return {@code true} if either all elements of the sequence match the provided predicate or 
     *         the sequence is empty, otherwise {@code false}
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if the predicate throws an exception
     * @see #anyMatch(Throwables.Predicate)
     * @see #noneMatch(Throwables.Predicate)
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
     * Returns whether no elements of this sequence match the provided predicate.
     * Returns {@code true} if the sequence is empty.
     * 
     * <p>This is a <b>short-circuiting terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 3, 5, 7);
     * boolean noEven = seq.noneMatch(n -> n % 2 == 0);
     * // noEven == true
     * }</pre>
     * 
     * @param <E2> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements of this sequence
     * @return {@code true} if either no elements of the sequence match the provided predicate or 
     *         the sequence is empty, otherwise {@code false}
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if the predicate throws an exception
     * @see #anyMatch(Throwables.Predicate)
     * @see #allMatch(Throwables.Predicate)
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
     * Returns whether the number of elements matching the provided predicate is between {@code atLeast} and {@code atMost}, inclusive.
     * The operation stops as soon as the count exceeds {@code atMost}.
     * 
     * <p>This is a <b>short-circuiting terminal operation</b>.</p>
     * 
     * <p>Implementation note: This is equivalent to {@code {@code atLeast} <= stream.filter(predicate).limit(atMost + 1).count() <= atMost}</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5, 6);
     * boolean has2to4Even = seq.nMatch(2, 4, n -> n % 2 == 0);
     * // has2to4Even == true (there are 3 even numbers)
     * }</pre>
     * 
     * @param <E2> the type of exception that the predicate may throw
     * @param atLeast the minimum number of elements that must match the predicate (inclusive)
     * @param atMost the maximum number of elements that can match the predicate (inclusive)
     * @param predicate a non-interfering, stateless predicate to apply to elements of this sequence
     * @return {@code true} if the number of elements matching the predicate is between {@code atLeast} 
     *         and {@code atMost} (inclusive), otherwise {@code false}
     * @throws IllegalArgumentException if {@code atLeast} or {@code atMost} is negative, or if {@code atMost} is less than {@code atLeast}
     * @throws IllegalStateException if the sequence has already been operated upon or closed
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
     * Returns an {@code Optional} describing the first element of this sequence that matches the given predicate,
     * or an empty {@code Optional} if no such element exists.
     * 
     * <p>This is a <b>short-circuiting terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5);
     * Optional<Integer> firstEven = seq.findFirst(n -> n % 2 == 0);
     * // firstEven.get() == 2
     * }</pre>
     * 
     * @param <E2> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements of this sequence
     * @return an {@code Optional} describing the first element that matches the predicate, 
     *         or an empty {@code Optional} if no such element exists
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if the predicate throws an exception
     * @see #findAny(Throwables.Predicate)
     * @see #findLast(Throwables.Predicate)
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
     * Returns an {@code Optional} describing the first element of this sequence that matches the given predicate,
     * or an empty {@code Optional} if no such element exists.
     * 
     * <p>This is a <b>short-circuiting terminal operation</b>.</p>
     * 
     * <p>Note: This method behaves identically to {@link #findFirst(Throwables.Predicate)} in sequential sequences.</p>
     * 
     * @param <E2> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements of this sequence
     * @return an {@code Optional} describing the first element that matches the predicate, 
     *         or an empty {@code Optional} if no such element exists
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if the predicate throws an exception
     * @deprecated replaced by {@link #findFirst(Throwables.Predicate)}
     * @see #findFirst(Throwables.Predicate)
     */
    @Deprecated
    @TerminalOp
    public <E2 extends Exception> Optional<T> findAny(final Throwables.Predicate<? super T, E2> predicate) throws IllegalStateException, E, E2 {
        return findFirst(predicate);
    }

    /**
     * Returns an {@code Optional} describing the last element of this sequence that matches the given predicate,
     * or an empty {@code Optional} if no such element exists.
     * 
     * <p>This is a <b>terminal operation</b>.</p>
     * 
     * <p>Performance note: Consider using {@code seq.reversed().findFirst(predicate)} for better performance 
     * if the sequence supports efficient reversal.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5);
     * Optional<Integer> lastEven = seq.findLast(n -> n % 2 == 0);
     * // lastEven.get() == 4
     * }</pre>
     * 
     * @param <E2> the type of exception that the predicate may throw
     * @param predicate a non-interfering, stateless predicate to apply to elements of this sequence
     * @return an {@code Optional} describing the last element that matches the predicate, 
     *         or an empty {@code Optional} if no such element exists
     * @throws IllegalStateException if the sequence has already been operated upon or closed
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

    /**
     * Returns whether this sequence contains all of the specified elements.
     * 
     * <p>This is a <b>short-circuiting terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5);
     * boolean hasAll = seq.containsAll(2, 4);
     * // hasAll == true
     * }</pre>
     * 
     * @param a the elements to check for containment in this sequence
     * @return {@code true} if this sequence contains all of the specified elements, otherwise {@code false}
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws E if an exception occurs during iteration
     * @see #containsAny(Object[])
     * @see #containsNone(Object[])
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
     * Returns whether this sequence contains all elements in the specified collection.
     * 
     * <p>This is a <b>short-circuiting terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5);
     * List<Integer> list = Arrays.asList(2, 3, 4);
     * boolean hasAll = seq.containsAll(list);
     * // hasAll == true
     * }</pre>
     * 
     * @param c the collection of elements to check for containment in this sequence
     * @return {@code true} if this sequence contains all elements in the specified collection, otherwise {@code false}
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws E if an exception occurs during iteration
     * @see #containsAny(Collection)
     * @see #containsNone(Collection)
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
     * Returns whether this sequence contains any of the specified elements.
     * 
     * <p>This is a <b>short-circuiting terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5);
     * boolean hasAny = seq.containsAny(7, 3, 9);
     * // hasAny == true (contains 3)
     * }</pre>
     * 
     * @param a the elements to check for containment in this sequence
     * @return {@code true} if this sequence contains any of the specified elements, otherwise {@code false}
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws E if an exception occurs during iteration
     * @see #containsAll(Object[])
     * @see #containsNone(Object[])
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
     * Returns whether this sequence contains any element from the specified collection.
     * 
     * <p>This is a <b>short-circuiting terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5);
     * List<Integer> list = Arrays.asList(7, 3, 9);
     * boolean hasAny = seq.containsAny(list);
     * // hasAny == true (contains 3)
     * }</pre>
     * 
     * @param c the collection of elements to check for containment in this sequence
     * @return {@code true} if this sequence contains any element from the specified collection, otherwise {@code false}
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws E if an exception occurs during iteration
     * @see #containsAll(Collection)
     * @see #containsNone(Collection)
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
     * Returns whether this sequence contains none of the specified elements.
     * 
     * <p>This is a <b>short-circuiting terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5);
     * boolean hasNone = seq.containsNone(6, 7, 8);
     * // hasNone == true
     * }</pre>
     * 
     * @param a the elements to check for non-containment in this sequence
     * @return {@code true} if this sequence doesn't contain any of the specified elements, 
     *         or if this sequence is empty, or if {@code a} is {@code null} or empty, otherwise {@code false}
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws E if an exception occurs during iteration
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
     * Returns whether this sequence contains none of the elements from the specified collection.
     * 
     * <p>This is a <b>short-circuiting terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5);
     * List<Integer> list = Arrays.asList(6, 7, 8);
     * boolean hasNone = seq.containsNone(list);
     * // hasNone == true
     * }</pre>
     * 
     * @param c the collection of elements to check for non-containment in this sequence
     * @return {@code true} if this sequence doesn't contain any element from the specified collection, 
     *         or if this sequence is empty, or if {@code c} is {@code null} or empty, otherwise {@code false}
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws E if an exception occurs during iteration
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
     * Returns whether this sequence contains duplicate elements.
     * 
     * <p>This is a <b>short-circuiting terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq1 = Seq.of(1, 2, 3, 2, 4);
     * boolean hasDups1 = seq1.hasDuplicates(); // true
     * 
     * Seq<Integer> seq2 = Seq.of(1, 2, 3, 4, 5);
     * boolean hasDups2 = seq2.hasDuplicates(); // false
     * }</pre>
     * 
     * @return {@code true} if this sequence contains at least one duplicate element, otherwise {@code false}
     * @throws IllegalStateException if the sequence has already been operated upon or closed
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
     * Returns an {@code Optional} containing the k-th largest element in this sequence according to the provided comparator,
     * or an empty {@code Optional} if this sequence has fewer than k elements.
     * 
     * <p>This is a <b>terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(3, 1, 4, 1, 5, 9, 2, 6);
     * Optional<Integer> third = seq.kthLargest(3, Integer::compare);
     * // third.get() == 5 (the 3rd largest element)
     * }</pre>
     * 
     * @param k the position (1-based) of the largest element to find. For example, k=1 finds the largest element,
     *          k=2 finds the second largest, etc.
     * @param comparator the comparator to determine the order of elements. If {@code null}, natural ordering is used
     * @return an {@code Optional} containing the k-th largest element, or an empty {@code Optional} if the sequence 
     *         has fewer than k elements
     * @throws IllegalStateException if the sequence has already been operated upon or closed
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
     * Returns an {@code Optional} containing a map of percentile values and their corresponding elements from this sequence.
     * The percentiles calculated are the standard quartiles and other key percentiles (0th, 25th, 50th, 75th, 100th, etc.).
     * All elements will be loaded into memory and sorted using natural ordering.
     * 
     * <p>This is a <b>terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
     * Optional<Map<Percentage, Integer>> percentiles = seq.percentiles();
     * // percentiles.get() contains mappings like:
     * // {0%=1, 25%=3, 50%=5, 75%=8, 100%=10}
     * }</pre>
     * 
     * @return an {@code Optional} containing a map where keys are {@code Percentage} values and values are the 
     *         corresponding elements at those percentiles, or an empty {@code Optional} if the sequence is empty
     * @throws IllegalStateException if the sequence has already been operated upon or closed
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
     * Returns an {@code Optional} containing a map of percentile values and their corresponding elements from this sequence
     * according to the provided comparator.
     * The percentiles calculated are the standard quartiles and other key percentiles (0th, 25th, 50th, 75th, 100th, etc.).
     * All elements will be loaded into memory and sorted.
     * 
     * <p>This is a <b>terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = Seq.of("apple", "pie", "banana", "zoo");
     * Optional<Map<Percentage, String>> percentiles = seq.percentiles(String::compareTo);
     * // percentiles.get() contains mappings based on alphabetical order
     * }</pre>
     * 
     * @param comparator a comparator to determine the order of elements for percentile calculation
     * @return an {@code Optional} containing a map where keys are {@code Percentage} values and values are the 
     *         corresponding elements at those percentiles, or an empty {@code Optional} if the sequence is empty
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws IllegalArgumentException if the specified comparator is {@code null}
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
     * Returns an {@code Optional} describing the first element of this sequence,
     * or an empty {@code Optional} if the sequence is empty.
     * 
     * <p>This is a <b>short-circuiting terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = Seq.of("first", "second", "third");
     * Optional<String> first = seq.first();
     * // first.get() == "first"
     * }</pre>
     * 
     * @return an {@code Optional} describing the first element of this sequence, 
     *         or an empty {@code Optional} if the sequence is empty
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws E if an exception occurs during iteration
     * @see #last()
     * @see #elementAt(long)
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
     * Returns an {@code Optional} describing the last element of this sequence,
     * or an empty {@code Optional} if the sequence is empty.
     * 
     * <p>This is a <b>terminal operation</b>.</p>
     * 
     * <p>Performance note: Consider using {@code seq.reversed().first()} for better performance 
     * when the sequence supports efficient reversal.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = Seq.of("first", "second", "third");
     * Optional<String> last = seq.last();
     * // last.get() == "third"
     * }</pre>
     * 
     * @return an {@code Optional} describing the last element of this sequence, 
     *         or an empty {@code Optional} if the sequence is empty
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws E if an exception occurs during iteration
     * @see #first()
     * @see #elementAt(long)
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
     * Returns an {@code Optional} describing the element at the specified position in this sequence,
     * or an empty {@code Optional} if the sequence has fewer elements than the specified position.
     * 
     * <p>This is a <b>terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = Seq.of("zero", "one", "two", "three");
     * Optional<String> elem = seq.elementAt(2);
     * // elem.get() == "two"
     * }</pre>
     * 
     * @param position the position of the element to return (0-based index)
     * @return an {@code Optional} describing the element at the specified position, 
     *         or an empty {@code Optional} if the sequence has fewer than {@code position + 1} elements
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws IllegalArgumentException if {@code position} is negative
     * @throws E if an exception occurs during iteration
     * @see #first()
     * @see #last()
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
     * Returns an {@code Optional} describing the only element of this sequence,
     * or an empty {@code Optional} if the sequence is empty.
     * If the sequence contains more than one element, a {@code TooManyElementsException} is thrown.
     * 
     * <p>This is a <b>terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq1 = Seq.of("single");
     * Optional<String> only1 = seq1.onlyOne(); // only1.get() == "single"
     * 
     * Seq<String> seq2 = Seq.of("first", "second");
     * seq2.onlyOne(); // throws TooManyElementsException
     * }</pre>
     * 
     * @return an {@code Optional} describing the only element of this sequence, 
     *         or an empty {@code Optional} if the sequence is empty
     * @throws IllegalStateException if the sequence has already been operated upon or closed
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
     * <p>This is a <b>terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = Seq.of("a", "b", "c", "d");
     * long count = seq.count(); // count == 4
     * }</pre>
     * 
     * @return the count of elements in this sequence
     * @throws IllegalStateException if the sequence has already been operated upon or closed
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
     * Returns an array containing all the elements of this sequence.
     * 
     * <p>This is a <b>terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = Seq.of("a", "b", "c");
     * Object[] array = seq.toArray();
     * // array = ["a", "b", "c"]
     * }</pre>
     * 
     * @return an array containing all the elements of this sequence
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws E if an exception occurs during iteration
     * @see #toArray(IntFunction)
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
     * Returns an array containing all the elements of this sequence, using the provided generator function
     * to allocate the returned array.
     * 
     * <p>This is a <b>terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = Seq.of("a", "b", "c");
     * String[] array = seq.toArray(String[]::new);
     * // array = ["a", "b", "c"]
     * }</pre>
     * 
     * @param <A> the component type of the resulting array
     * @param generator a function which produces a new array of the desired type and the provided length
     * @return an array containing all the elements of this sequence
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws IllegalArgumentException if the generator function is {@code null}
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
     * Returns a {@code List} containing all the elements of this sequence in encounter order.
     * 
     * <p>This is a <b>terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5);
     * List<Integer> list = seq.toList();
     * // list = [1, 2, 3, 4, 5]
     * }</pre>
     * 
     * @return a {@code List} containing all the elements of this sequence
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws E if an exception occurs during iteration
     * @see #toSet()
     * @see #toCollection(Supplier)
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
     * Returns a {@code Set} containing all the distinct elements of this sequence.
     * Duplicate elements are removed based on their {@code equals} method.
     * 
     * <p>This is a <b>terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 2, 4, 3);
     * Set<Integer> set = seq.toSet();
     * // set = [1, 2, 3, 4] (order may vary)
     * }</pre>
     * 
     * @return a {@code Set} containing all the distinct elements of this sequence
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws E if an exception occurs during iteration
     * @see #toList()
     * @see #toCollection(Supplier)
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
     * Returns a collection containing all the elements of this sequence, using the provided supplier
     * to create the collection instance.
     * 
     * <p>This is a <b>terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = Seq.of("a", "b", "c");
     * LinkedList<String> list = seq.toCollection(LinkedList::new);
     * TreeSet<String> set = seq.toCollection(TreeSet::new);
     * }</pre>
     * 
     * @param <C> the type of the resulting collection
     * @param supplier a function which returns a new, empty collection of the appropriate type
     * @return a collection containing all the elements of this sequence
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws IllegalArgumentException if the supplier function is {@code null}
     * @throws E if an exception occurs during iteration
     * @see #toList()
     * @see #toSet()
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
     * Returns an immutable list containing all the elements of this sequence in encounter order.
     * The returned list is unmodifiable.
     * 
     * <p>This is a <b>terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = Seq.of("a", "b", "c");
     * ImmutableList<String> list = seq.toImmutableList();
     * // list.add("d"); // would throw UnsupportedOperationException
     * }</pre>
     * 
     * @return an immutable list containing all the elements of this sequence
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws E if an exception occurs during iteration
     * @see #toList()
     * @see #toImmutableSet()
     */
    @TerminalOp
    public ImmutableList<T> toImmutableList() throws IllegalStateException, E {
        return ImmutableList.wrap(toList());
    }

    /**
     * Returns an immutable set containing all the distinct elements of this sequence.
     * The returned set is unmodifiable.
     * 
     * <p>This is a <b>terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 2, 4);
     * ImmutableSet<Integer> set = seq.toImmutableSet();
     * // set contains [1, 2, 3, 4] and cannot be modified
     * }</pre>
     * 
     * @return an immutable set containing all the distinct elements of this sequence
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws E if an exception occurs during iteration
     * @see #toSet()
     * @see #toImmutableList()
     */
    @TerminalOp
    public ImmutableSet<T> toImmutableSet() throws IllegalStateException, E {
        return ImmutableSet.wrap(toSet());
    }

    /**
     * Collects all elements of this sequence into a list and applies the given function to it.
     * 
     * <p>This is a <b>terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 4, 5);
     * int sum = seq.toListThenApply(list -> list.stream().mapToInt(Integer::intValue).sum());
     * // sum == 15
     * }</pre>
     * 
     * @param <R> the type of the result
     * @param <E2> the type of exception that may be thrown by the function
     * @param func the function to apply to the list of all elements
     * @return the result of applying the function to the list
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws IllegalArgumentException if the specified function is {@code null}
     * @throws E if an exception occurs during iteration
     * @throws E2 if the function throws an exception
     * @see #toListThenAccept(Throwables.Consumer)
     * @see #toSetThenApply(Throwables.Function)
     */
    @TerminalOp
    public <R, E2 extends Exception> R toListThenApply(final Throwables.Function<? super List<T>, R, E2> func)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();
        checkArgNotNull(func, cs.func);

        return func.apply(toList());
    }

    /**
     * Collects all elements of this sequence into a list and performs the given action on it.
     * 
     * <p>This is a <b>terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = Seq.of("apple", "banana", "cherry");
     * seq.toListThenAccept(list -> {
     *     System.out.println("List size: " + list.size());
     *     list.forEach(System.out::println);
     * });
     * }</pre>
     * 
     * @param <E2> the type of exception that may be thrown by the consumer
     * @param consumer the action to perform on the list of all elements
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws IllegalArgumentException if the specified consumer is {@code null}
     * @throws E if an exception occurs during iteration
     * @throws E2 if the consumer throws an exception
     * @see #toListThenApply(Throwables.Function)
     * @see #toSetThenAccept(Throwables.Consumer)
     */
    @TerminalOp
    public <E2 extends Exception> void toListThenAccept(final Throwables.Consumer<? super List<T>, E2> consumer)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();
        checkArgNotNull(consumer, cs.consumer);

        consumer.accept(toList());
    }

    /**
     * Collects all distinct elements of this sequence into a set and applies the given function to it.
     * 
     * <p>This is a <b>terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(1, 2, 3, 2, 4, 3);
     * int distinctCount = seq.toSetThenApply(Set::size);
     * // distinctCount == 4
     * }</pre>
     * 
     * @param <R> the type of the result
     * @param <E2> the type of exception that may be thrown by the function
     * @param func the function to apply to the set of distinct elements
     * @return the result of applying the function to the set
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws IllegalArgumentException if the specified function is {@code null}
     * @throws E if an exception occurs during iteration
     * @throws E2 if the function throws an exception
     * @see #toSetThenAccept(Throwables.Consumer)
     * @see #toListThenApply(Throwables.Function)
     */
    @TerminalOp
    public <R, E2 extends Exception> R toSetThenApply(final Throwables.Function<? super Set<T>, R, E2> func)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();
        checkArgNotNull(func, cs.func);

        return func.apply(toSet());
    }

    /**
     * Collects all distinct elements of this sequence into a set and performs the given action on it.
     * 
     * <p>This is a <b>terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = Seq.of("apple", "banana", "apple", "cherry");
     * seq.toSetThenAccept(set -> {
     *     System.out.println("Distinct count: " + set.size());
     *     set.forEach(System.out::println);
     * });
     * }</pre>
     * 
     * @param <E2> the type of exception that may be thrown by the consumer
     * @param consumer the action to perform on the set of distinct elements
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws IllegalArgumentException if the specified consumer is {@code null}
     * @throws E if an exception occurs during iteration
     * @throws E2 if the consumer throws an exception
     * @see #toSetThenApply(Throwables.Function)
     * @see #toListThenAccept(Throwables.Consumer)
     */
    @TerminalOp
    public <E2 extends Exception> void toSetThenAccept(final Throwables.Consumer<? super Set<T>, E2> consumer)
            throws IllegalStateException, IllegalArgumentException, E, E2 {
        assertNotClosed();
        checkArgNotNull(consumer, cs.consumer);

        consumer.accept(toSet());
    }

    /**
     * Collects all elements of this sequence into a collection created by the supplier and applies the given function to it.
     * 
     * <p>This is a <b>terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String> seq = Seq.of("apple", "banana", "cherry");
     * String first = seq.toCollectionThenApply(
     *     LinkedList::new,
     *     list -> list.getFirst()
     * );
     * // first == "apple"
     * }</pre>
     * 
     * @param <R> the type of the result
     * @param <C> the type of the resulting collection
     * @param <E2> the type of exception that may be thrown by the function
     * @param supplier a function which returns a new, empty collection of the appropriate type
     * @param func the function to apply to the collection
     * @return the result of applying the function to the collection
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws IllegalArgumentException if the specified supplier or function is {@code null}
     * @throws E if an exception occurs during iteration
     * @throws E2 if the function throws an exception
     * @see #toCollectionThenAccept(Supplier, Throwables.Consumer)
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
     * Collects all elements of this sequence into a collection created by the supplier and performs the given action on it.
     * 
     * <p>This is a <b>terminal operation</b>.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer> seq = Seq.of(3, 1, 4, 1, 5);
     * seq.toCollectionThenAccept(
     *     TreeSet::new,
     *     set -> set.forEach(System.out::println) // prints in sorted order
     * );
     * }</pre>
     * 
     * @param <C> the type of the resulting collection
     * @param <E2> the type of exception that may be thrown by the consumer
     * @param supplier a function which returns a new, empty collection of the appropriate type
     * @param consumer the action to perform on the collection
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws IllegalArgumentException if the specified supplier or consumer is {@code null}
     * @throws E if an exception occurs during iteration
     * @throws E2 if the consumer throws an exception
     * @see #toCollectionThenApply(Supplier, Throwables.Function)
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
     * Each element is transformed into a key-value pair where the key is extracted by {@code keyMapper}
     * and the value is extracted by {@code valueMapper}. If duplicate keys are encountered,
     * an {@code IllegalStateException} will be thrown.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Map<String, Integer> map = seq.toMap(
     *     person -> person.getName(),
     *     person -> person.getAge()
     * );
     * }</pre>
     *
     * @param <K> the type of keys in the resulting map
     * @param <V> the type of values in the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param <E3> the type of exception that may be thrown by the value extractor
     * @param keyMapper the function to extract keys from the elements. Must not be {@code null}.
     * @param valueMapper the function to extract values from the elements. Must not be {@code null}.
     * @return a Map containing the elements of this sequence transformed into key-value pairs
     * @throws IllegalStateException if the sequence is already closed or there are duplicate keys
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     * @throws E3 if an exception occurs during value extraction
     * @see Fnn#throwingMerger()
     * @see Fnn#replacingMerger()
     * @see Fnn#ignoringMerger()
     */
    @TerminalOp
    public <K, V, E2 extends Exception, E3 extends Exception> Map<K, V> toMap(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Throwables.Function<? super T, ? extends V, E3> valueMapper) throws IllegalStateException, E, E2, E3 {
        return toMap(keyMapper, valueMapper, Suppliers.ofMap());
    }

    /**
     * Converts the elements in this sequence to a Map using the provided key and value extractors,
     * storing the results in a map created by the specified map factory.
     * Each element is transformed into a key-value pair where the key is extracted by {@code keyMapper}
     * and the value is extracted by {@code valueMapper}. If duplicate keys are encountered,
     * an {@code IllegalStateException} will be thrown.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LinkedHashMap<String, Integer> map = seq.toMap(
     *     person -> person.getName(),
     *     person -> person.getAge(),
     *     LinkedHashMap::new
     * );
     * }</pre>
     *
     * @param <K> the type of keys in the resulting map
     * @param <V> the type of values in the resulting map
     * @param <M> the type of the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param <E3> the type of exception that may be thrown by the value extractor
     * @param keyMapper the function to extract keys from the elements. Must not be {@code null}.
     * @param valueMapper the function to extract values from the elements. Must not be {@code null}.
     * @param mapFactory the supplier to create the resulting map. Must not be {@code null}.
     * @return a Map containing the elements of this sequence transformed into key-value pairs
     * @throws IllegalStateException if the sequence is already closed or there are duplicate keys
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     * @throws E3 if an exception occurs during value extraction
     * @see Fnn#throwingMerger()
     * @see Fnn#replacingMerger()
     * @see Fnn#ignoringMerger()
     */
    @TerminalOp
    public <K, V, M extends Map<K, V>, E2 extends Exception, E3 extends Exception> M toMap(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Throwables.Function<? super T, ? extends V, E3> valueMapper, final Supplier<? extends M> mapFactory) throws IllegalStateException, E, E2, E3 {
        return toMap(keyMapper, valueMapper, Fnn.<V, E> throwingMerger(), mapFactory);
    }

    /**
     * Converts the elements in this sequence to a Map using the provided key and value extractors,
     * with a merge function to handle duplicate keys. When duplicate keys are encountered,
     * the merge function is applied to resolve the conflict between the existing and new values.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum ages for duplicate names
     * Map<String, Integer> map = seq.toMap(
     *     person -> person.getName(),
     *     person -> person.getAge(),
     *     (oldAge, newAge) -> oldAge + newAge
     * );
     * }</pre>
     *
     * @param <K> the type of keys in the resulting map
     * @param <V> the type of values in the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param <E3> the type of exception that may be thrown by the value extractor
     * @param <E4> the type of exception that may be thrown by the merge function
     * @param keyMapper the function to extract keys from the elements. Must not be {@code null}.
     * @param valueMapper the function to extract values from the elements. Must not be {@code null}.
     * @param mergeFunction the function to resolve collisions between values associated with the same key. Must not be {@code null}.
     * @return a Map containing the elements of this sequence transformed into key-value pairs
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     * @throws E3 if an exception occurs during value extraction
     * @throws E4 if an exception occurs during merging
     * @see Fnn#throwingMerger()
     * @see Fnn#replacingMerger()
     * @see Fnn#ignoringMerger()
     */
    @TerminalOp
    public <K, V, E2 extends Exception, E3 extends Exception, E4 extends Exception> Map<K, V> toMap(
            final Throwables.Function<? super T, ? extends K, E2> keyMapper, final Throwables.Function<? super T, ? extends V, E3> valueMapper,
            final Throwables.BinaryOperator<V, E4> mergeFunction) throws IllegalStateException, E, E2, E3, E4 {
        return toMap(keyMapper, valueMapper, mergeFunction, Suppliers.ofMap());
    }

    /**
     * Converts the elements in this sequence to a Map using the provided key and value extractors,
     * with a merge function to handle duplicate keys and a custom map factory.
     * When duplicate keys are encountered, the merge function is applied to resolve the conflict
     * between the existing and new values.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect to TreeMap, keeping the last value for duplicates
     * TreeMap<String, Integer> map = seq.toMap(
     *     person -> person.getName(),
     *     person -> person.getAge(),
     *     (oldAge, newAge) -> newAge,
     *     TreeMap::new
     * );
     * }</pre>
     *
     * @param <K> the type of keys in the resulting map
     * @param <V> the type of values in the resulting map
     * @param <M> the type of the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param <E3> the type of exception that may be thrown by the value extractor
     * @param <E4> the type of exception that may be thrown by the merge function
     * @param keyMapper the function to extract keys from the elements. Must not be {@code null}.
     * @param valueMapper the function to extract values from the elements. Must not be {@code null}.
     * @param mergeFunction the function to resolve collisions between values associated with the same key. Must not be {@code null}.
     * @param mapFactory the supplier to create the resulting map. Must not be {@code null}.
     * @return a Map containing the elements of this sequence transformed into key-value pairs
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     * @throws E3 if an exception occurs during value extraction
     * @throws E4 if an exception occurs during merging
     * @see Fnn#throwingMerger()
     * @see Fnn#replacingMerger()
     * @see Fnn#ignoringMerger()
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

    /**
     * Converts the elements in this sequence to an ImmutableMap using the provided key and value extractors.
     * Each element is transformed into a key-value pair where the key is extracted by {@code keyMapper}
     * and the value is extracted by {@code valueMapper}. The resulting map is immutable and cannot be modified.
     * If duplicate keys are encountered, an {@code IllegalStateException} will be thrown.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ImmutableMap<String, Integer> map = seq.toImmutableMap(
     *     person -> person.getName(),
     *     person -> person.getAge()
     * );
     * }</pre>
     *
     * @param <K> the type of keys in the resulting map
     * @param <V> the type of values in the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param <E3> the type of exception that may be thrown by the value extractor
     * @param keyMapper the function to extract keys from the elements. Must not be {@code null}.
     * @param valueMapper the function to extract values from the elements. Must not be {@code null}.
     * @return an ImmutableMap containing the elements of this sequence transformed into key-value pairs
     * @throws IllegalStateException if the sequence is already closed or there are duplicate keys
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     * @throws E3 if an exception occurs during value extraction
     * @see Fnn#throwingMerger()
     * @see Fnn#replacingMerger()
     * @see Fnn#ignoringMerger()
     */
    @TerminalOp
    public <K, V, E2 extends Exception, E3 extends Exception> ImmutableMap<K, V> toImmutableMap(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Throwables.Function<? super T, ? extends V, E3> valueMapper) throws IllegalStateException, E, E2, E3 {
        return ImmutableMap.wrap(toMap(keyMapper, valueMapper));
    }

    /**
     * Converts the elements in this sequence to an ImmutableMap using the provided key and value extractors,
     * with a merge function to handle duplicate keys. When duplicate keys are encountered,
     * the merge function is applied to resolve the conflict between the existing and new values.
     * The resulting map is immutable and cannot be modified.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create immutable map, concatenating values for duplicate keys
     * ImmutableMap<String, String> map = seq.toImmutableMap(
     *     person -> person.getDepartment(),
     *     person -> person.getName(),
     *     (name1, name2) -> name1 + ", " + name2
     * );
     * }</pre>
     *
     * @param <K> the type of keys in the resulting map
     * @param <V> the type of values in the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param <E3> the type of exception that may be thrown by the value extractor
     * @param <E4> the type of exception that may be thrown by the merge function
     * @param keyMapper the function to extract keys from the elements. Must not be {@code null}.
     * @param valueMapper the function to extract values from the elements. Must not be {@code null}.
     * @param mergeFunction the function to resolve collisions between values associated with the same key. Must not be {@code null}.
     * @return an ImmutableMap containing the elements of this sequence transformed into key-value pairs
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     * @throws E3 if an exception occurs during value extraction
     * @throws E4 if an exception occurs during merging
     * @see Fnn#throwingMerger()
     * @see Fnn#replacingMerger()
     * @see Fnn#ignoringMerger()
     */
    @TerminalOp
    public <K, V, E2 extends Exception, E3 extends Exception, E4 extends Exception> ImmutableMap<K, V> toImmutableMap(
            final Throwables.Function<? super T, ? extends K, E2> keyMapper, final Throwables.Function<? super T, ? extends V, E3> valueMapper,
            final Throwables.BinaryOperator<V, E4> mergeFunction) throws IllegalStateException, E, E2, E3, E4 {
        return ImmutableMap.wrap(toMap(keyMapper, valueMapper, mergeFunction));
    }

    /**
     * Groups the elements in this sequence by the keys extracted using the provided key extractor function.
     * Each element is placed into a list associated with its key. The resulting map contains
     * all unique keys mapped to lists of elements that share that key.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group persons by department
     * Map<String, List<Person>> byDepartment = seq.groupTo(
     *     person -> person.getDepartment()
     * );
     * }</pre>
     *
     * @param <K> the type of keys in the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param keyMapper the function to extract keys from the elements. Must not be {@code null}.
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
     * Groups the elements in this sequence by the keys extracted using the provided key extractor function,
     * storing the results in a map created by the specified map factory.
     * Each element is placed into a list associated with its key.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group persons by age in a TreeMap
     * TreeMap<Integer, List<Person>> byAge = seq.groupTo(
     *     person -> person.getAge(),
     *     TreeMap::new
     * );
     * }</pre>
     *
     * @param <K> the type of keys in the resulting map
     * @param <M> the type of the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param keyMapper the function to extract keys from the elements. Must not be {@code null}.
     * @param mapFactory the supplier to create the resulting map. Must not be {@code null}.
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
     * Groups the elements in this sequence by the keys extracted using the provided key extractor function,
     * and transforms the values using the provided value extractor function.
     * Each transformed value is placed into a list associated with its key.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group persons by department, collecting only names
     * Map<String, List<String>> namesByDept = seq.groupTo(
     *     person -> person.getDepartment(),
     *     person -> person.getName()
     * );
     * }</pre>
     *
     * @param <K> the type of keys in the resulting map
     * @param <V> the type of values in the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param <E3> the type of exception that may be thrown by the value extractor
     * @param keyMapper the function to extract keys from the elements. Must not be {@code null}.
     * @param valueMapper the function to extract values from the elements. Must not be {@code null}.
     * @return a Map where each key is associated with a list of values that share that key
     * @throws IllegalStateException if the sequence is already closed
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
     * Groups the elements in this sequence by the keys extracted using the provided key extractor function,
     * transforms the values using the provided value extractor function, and stores the results
     * in a map created by the specified map factory. Each transformed value is placed into a list
     * associated with its key.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group persons by age range, collecting salaries in a LinkedHashMap
     * LinkedHashMap<String, List<Double>> salariesByAgeRange = seq.groupTo(
     *     person -> person.getAge() < 30 ? "Young" : "Senior",
     *     person -> person.getSalary(),
     *     LinkedHashMap::new
     * );
     * }</pre>
     *
     * @param <K> the type of keys in the resulting map
     * @param <V> the type of values in the resulting map
     * @param <M> the type of the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param <E3> the type of exception that may be thrown by the value extractor
     * @param keyMapper the function to extract keys from the elements. Must not be {@code null}.
     * @param valueMapper the function to extract values from the elements. Must not be {@code null}.
     * @param mapFactory the supplier to create the resulting map. Must not be {@code null}.
     * @return a Map where each key is associated with a list of values that share that key
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the key extractor, value extractor, or map factory is null
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
     * The downstream collector is applied to the elements associated with each key.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group persons by department and count them
     * Map<String, Long> countByDept = seq.groupTo(
     *     person -> person.getDepartment(),
     *     Collectors.counting()
     * );
     * }</pre>
     *
     * @param <K> the type of keys in the resulting map
     * @param <D> the type of values in the resulting map after applying the downstream collector
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param keyMapper the function to extract keys from the elements. Must not be {@code null}.
     * @param downstream the collector to collect the grouped elements. Must not be {@code null}.
     * @return a Map where each key is associated with the result of the downstream collector
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     */
    @TerminalOp
    public <K, D, E2 extends Exception> Map<K, D> groupTo(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Collector<? super T, ?, D> downstream) throws IllegalStateException, E, E2 {
        return groupTo(keyMapper, downstream, Suppliers.ofMap());
    }

    /**
     * Groups the elements in this sequence by the keys extracted using the provided key extractor function,
     * collects the grouped elements using the provided downstream collector, and stores the results
     * in a map created by the specified map factory.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group persons by age and calculate average salary in a TreeMap
     * TreeMap<Integer, Double> avgSalaryByAge = seq.groupTo(
     *     person -> person.getAge(),
     *     Collectors.averagingDouble(Person::getSalary),
     *     TreeMap::new
     * );
     * }</pre>
     *
     * @param <K> the type of keys in the resulting map
     * @param <D> the type of values in the resulting map after applying the downstream collector
     * @param <M> the type of the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param keyMapper the function to extract keys from the elements. Must not be {@code null}.
     * @param downstream the collector to collect the grouped elements. Must not be {@code null}.
     * @param mapFactory the supplier to create the resulting map. Must not be {@code null}.
     * @return a Map where each key is associated with the result of the downstream collector
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during key extraction
     */
    @TerminalOp
    public <K, D, M extends Map<K, D>, E2 extends Exception> M groupTo(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Collector<? super T, ?, D> downstream, final Supplier<? extends M> mapFactory) throws IllegalStateException, E, E2 {
        return groupTo(keyMapper, Fnn.<T, E> identity(), downstream, mapFactory);
    }

    /**
     * Groups the elements in this sequence by the keys extracted using the provided key extractor function,
     * transforms the values using the provided value extractor, and collects the transformed values
     * using the provided downstream collector.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group persons by department, extract names, and join them
     * Map<String, String> namesByDept = seq.groupTo(
     *     person -> person.getDepartment(),
     *     person -> person.getName(),
     *     Collectors.joining(", ")
     * );
     * }</pre>
     *
     * @param <K> the type of keys in the resulting map
     * @param <V> the type of values extracted from elements
     * @param <D> the type of values in the resulting map after applying the downstream collector
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param <E3> the type of exception that may be thrown by the value extractor
     * @param keyMapper the function to extract keys from the elements. Must not be {@code null}.
     * @param valueMapper the function to extract values from the elements. Must not be {@code null}.
     * @param downstream the collector to collect the grouped values. Must not be {@code null}.
     * @return a Map where each key is associated with the result of the downstream collector
     * @throws IllegalStateException if the sequence is already closed
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
     * Groups the elements in this sequence by the keys extracted using the provided key extractor function,
     * transforms the values using the provided value extractor, collects the transformed values
     * using the provided downstream collector, and stores the results in a map created by
     * the specified map factory.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Group employees by department, extract salaries, and get max salary per department
     * LinkedHashMap<String, Optional<Double>> maxSalaryByDept = seq.groupTo(
     *     emp -> emp.getDepartment(),
     *     emp -> emp.getSalary(),
     *     Collectors.maxBy(Double::compare),
     *     LinkedHashMap::new
     * );
     * }</pre>
     *
     * @param <K> the type of keys in the resulting map
     * @param <V> the type of values extracted from elements
     * @param <D> the type of values in the resulting map after applying the downstream collector
     * @param <M> the type of the resulting map
     * @param <E2> the type of exception that may be thrown by the key extractor
     * @param <E3> the type of exception that may be thrown by the value extractor
     * @param keyMapper the function to extract keys from the elements. Must not be {@code null}.
     * @param valueMapper the function to extract values from the elements. Must not be {@code null}.
     * @param downstream the collector to collect the grouped values. Must not be {@code null}.
     * @param mapFactory the supplier to create the resulting map. Must not be {@code null}.
     * @return a Map where each key is associated with the result of the downstream collector
     * @throws IllegalStateException if the sequence is already closed
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
     * The map will always contain exactly two entries: one with key {@code true} containing elements
     * that match the predicate, and one with key {@code false} containing elements that do not match.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Partition persons into adults and minors
     * Map<Boolean, List<Person>> adultPartition = seq.partitionTo(
     *     person -> person.getAge() >= 18
     * );
     * List<Person> adults = adultPartition.get(true);
     * List<Person> minors = adultPartition.get(false);
     * }</pre>
     *
     * @param <E2> the type of exception that may be thrown by the predicate
     * @param predicate the predicate to test elements. Must not be {@code null}.
     * @return a Map with two entries: {@code true} for elements that match the predicate, and {@code false} for elements that do not
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     * @throws E2 if an exception occurs during predicate evaluation
     */
    @TerminalOp
    public <E2 extends Exception> Map<Boolean, List<T>> partitionTo(final Throwables.Predicate<? super T, E2> predicate) throws IllegalStateException, E, E2 {
        assertNotClosed();

        return partitionTo(predicate, Collectors.toList());
    }

    /**
     * Partitions the elements in this sequence into a map of two collected results based on the provided predicate.
     * Elements are first partitioned by the predicate, then each partition is collected using
     * the downstream collector. The map will always contain exactly two entries.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Partition persons and count each group
     * Map<Boolean, Long> adultCount = seq.partitionTo(
     *     person -> person.getAge() >= 18,
     *     Collectors.counting()
     * );
     * long numberOfAdults = adultCount.get(true);
     * long numberOfMinors = adultCount.get(false);
     * }</pre>
     *
     * @param <D> the type of the downstream result
     * @param <E2> the type of exception that may be thrown by the predicate
     * @param predicate the predicate to test elements. Must not be {@code null}.
     * @param downstream the collector to apply to each partition. Must not be {@code null}.
     * @return a Map with two entries: {@code true} for collected result that match the predicate, and {@code false} for collected result that do not
     * @throws IllegalStateException if the sequence is already closed
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
     * Each element is mapped to a key using the key extractor, and the element itself becomes the value.
     * Multiple elements can be associated with the same key.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create multimap of persons by department
     * ListMultimap<String, Person> personsByDept = seq.toMultimap(
     *     person -> person.getDepartment()
     * );
     * }</pre>
     *
     * @param <K> the type of keys in the resulting multimap
     * @param <E2> the type of exception that may be thrown by the key extractor function
     * @param keyMapper the function to extract keys from the elements. Must not be {@code null}.
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
     * Converts the elements in this sequence into a multimap based on the provided key extractor function,
     * using a custom multimap factory. Each element is mapped to a key using the key extractor,
     * and the element itself becomes the value. Multiple elements can be associated with the same key.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create SetMultimap of persons by age
     * SetMultimap<Integer, Person> personsByAge = seq.toMultimap(
     *     person -> person.getAge(),
     *     Suppliers.ofSetMultimap()
     * );
     * }</pre>
     *
     * @param <K> the type of keys in the resulting multimap
     * @param <V> the collection type for values in the multimap
     * @param <M> the type of the resulting multimap
     * @param <E2> the type of exception that may be thrown by the key extractor function
     * @param keyMapper the function to extract keys from the elements. Must not be {@code null}.
     * @param mapFactory the supplier to create the resulting multimap. Must not be {@code null}.
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
     * Each element is transformed into a key-value pair where the key is extracted by {@code keyMapper}
     * and the value is extracted by {@code valueMapper}. Multiple values can be associated with the same key.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create multimap of employee names by department
     * ListMultimap<String, String> namesByDept = seq.toMultimap(
     *     emp -> emp.getDepartment(),
     *     emp -> emp.getName()
     * );
     * }</pre>
     *
     * @param <K> the type of keys in the resulting multimap
     * @param <V> the type of values in the resulting multimap
     * @param <E2> the type of exception that may be thrown by the key extractor function
     * @param <E3> the type of exception that may be thrown by the value extractor function
     * @param keyMapper the function to extract keys from the elements. Must not be {@code null}.
     * @param valueMapper the function to extract values from the elements. Must not be {@code null}.
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
     * Converts the elements in this sequence into a multimap based on the provided key and value extractor functions,
     * using a custom multimap factory. Each element is transformed into a key-value pair where
     * the key is extracted by {@code keyMapper} and the value is extracted by {@code valueMapper}.
     * Multiple values can be associated with the same key.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create SetMultimap of employee IDs by department
     * SetMultimap<String, Long> idsByDept = seq.toMultimap(
     *     emp -> emp.getDepartment(),
     *     emp -> emp.getId(),
     *     Suppliers.ofSetMultimap()
     * );
     * }</pre>
     *
     * @param <K> the type of keys in the resulting multimap
     * @param <V> the type of values in the resulting multimap
     * @param <C> the collection type for values in the multimap
     * @param <M> the type of the resulting multimap
     * @param <E2> the type of exception that may be thrown by the key extractor function
     * @param <E3> the type of exception that may be thrown by the value extractor function
     * @param keyMapper the function to extract keys from the elements. Must not be {@code null}.
     * @param valueMapper the function to extract values from the elements. Must not be {@code null}.
     * @param mapFactory the supplier to create the resulting multimap. Must not be {@code null}.
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
     * A Multiset is a collection that allows duplicate elements and keeps track of their occurrences.
     * The resulting multiset will contain all the elements from this sequence with their counts.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Count occurrences of each element
     * Multiset<String> wordCounts = seq.toMultiset();
     * int countOfHello = wordCounts.count("hello");
     * }</pre>
     *
     * @return a Multiset containing all the elements from this sequence with their occurrence counts
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration
     */
    @TerminalOp
    public Multiset<T> toMultiset() throws IllegalStateException, E {
        return toMultiset(Suppliers.ofMultiset());
    }

    /**
     * Converts the elements in this sequence into a Multiset using the specified supplier.
     * A Multiset is a collection that allows duplicate elements and keeps track of their occurrences.
     * The resulting multiset will contain all the elements from this sequence with their counts.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create a custom multiset implementation
     * Multiset<String> wordCounts = seq.toMultiset(MyMultiset::new);
     * }</pre>
     *
     * @param supplier the supplier to create the resulting multiset. Must not be {@code null}.
     * @return a Multiset containing all the elements from this sequence with their occurrence counts
     * @throws IllegalStateException if the sequence is already closed
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
     * Converts the elements of this sequence into a Dataset.
     * The element type {@code T} in this sequence must be a Map or Bean for retrieving column names. It can't be collection or array.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Convert sequence of Person objects to Dataset
     * Dataset dataset = seq.toDataset();
     * // The Dataset will have columns for each property of Person
     * }</pre>
     *
     * @return a Dataset containing the elements from this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during the conversion
     * @see N#newDataset(Collection)
     */
    @Beta
    @TerminalOp
    public Dataset toDataset() throws IllegalStateException, E {
        return N.newDataset(toList());
    }

    /**
     * Converts the elements of the sequence into a Dataset.
     * The element type {@code T} in this stream must be an array/collection, Map or Bean.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Create Dataset with specific column names
     * List<String> columns = Arrays.asList("Name", "Age", "Department");
     * Dataset dataset = seq.toDataset(columns);
     * }</pre>
     *
     * @param columnNames the list of column names to be used in the Dataset. Must not be {@code null}.
     * @return a Dataset containing the elements from this sequence with the specified column names
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during the conversion
     * @see N#newDataset(Collection, Collection)
     */
    @TerminalOp
    public Dataset toDataset(final List<String> columnNames) throws IllegalStateException, E {
        return N.newDataset(columnNames, toList());
    }

    /**
     * Sums the integer values extracted from the elements in this sequence using the provided function.
     * The function is applied to each element to extract an integer value, and these values are summed.
     * The result is returned as a long to avoid overflow for large sums.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum the ages of all persons
     * long totalAge = seq.sumInt(person -> person.getAge());
     * }</pre>
     *
     * @param <E2> the type of exception that the function may throw
     * @param func the function to extract integer values from the elements. Must not be {@code null}.
     * @return the sum of the integer values as a long
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
     * The function is applied to each element to extract a long value, and these values are summed.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum the IDs of all records
     * long totalIds = seq.sumLong(record -> record.getId());
     * }</pre>
     *
     * @param <E2> the type of exception that the function may throw
     * @param func the function to extract long values from the elements. Must not be {@code null}.
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
     * The function is applied to each element to extract a double value, and these values are summed.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum the salaries of all employees
     * double totalSalary = seq.sumDouble(employee -> employee.getSalary());
     * }</pre>
     *
     * @param <E2> the type of exception that the function may throw
     * @param func the function to extract double values from the elements. Must not be {@code null}.
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
     * Calculates the average of the integer values extracted from the elements in this sequence
     * using the provided function. The function is applied to each element to extract an integer value,
     * and the average of these values is calculated.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate average age
     * OptionalDouble avgAge = seq.averageInt(person -> person.getAge());
     * if (avgAge.isPresent()) {
     *     System.out.println("Average age: " + avgAge.getAsDouble());
     * }
     * }</pre>
     *
     * @param <E2> the type of exception that the function may throw
     * @param func the function to extract integer values from the elements. Must not be {@code null}.
     * @return an OptionalDouble containing the average, or empty if the sequence is empty
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
     * Calculates the average of the long values extracted from the elements in this sequence
     * using the provided function. The function is applied to each element to extract a long value,
     * and the average of these values is calculated.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate average file size
     * OptionalDouble avgSize = seq.averageLong(file -> file.getSize());
     * }</pre>
     *
     * @param <E2> the type of exception that the function may throw
     * @param func the function to extract long values from the elements. Must not be {@code null}.
     * @return an OptionalDouble containing the average, or empty if the sequence is empty
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
     * Calculates the average of the double values extracted from the elements in this sequence
     * using the provided function. The function is applied to each element to extract a double value,
     * and the average of these values is calculated.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Calculate average price
     * OptionalDouble avgPrice = seq.averageDouble(product -> product.getPrice());
     * }</pre>
     *
     * @param <E2> the type of exception that the function may throw
     * @param func the function to extract double values from the elements. Must not be {@code null}.
     * @return an OptionalDouble containing the average, or empty if the sequence is empty
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
     * Performs a reduction on the elements of this sequence using the provided binary operator. 
     * The reduction is performed by repeatedly applying the accumulator function to combine elements,
     * starting with the first element as the initial value.
     * 
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum all numbers
     * Optional<Integer> sum = Seq.of(1, 2, 3, 4, 5)
     *     .reduce((a, b) -> a + b);
     * // Result: Optional[15]
     * 
     * // Find maximum
     * Optional<Integer> max = Seq.of(3, 1, 4, 1, 5)
     *     .reduce(Integer::max);
     * // Result: Optional[5]
     * }</pre>
     *
     * @param <E2> the type of exception that the accumulator function may throw
     * @param accumulator a function for combining two values, must be associative and stateless
     * @return an Optional containing the result of the reduction, or an empty Optional if the sequence is empty
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the accumulator function is null
     * @throws E if an exception occurs during iteration of the sequence
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
     * Performs a reduction on the elements of this sequence using the provided identity value and accumulator function.
     * The reduction is performed by using the identity value as the initial accumulation value,
     * then iteratively applying the accumulator function to combine the current accumulation value with each element.
     * 
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum with initial value
     * Integer sum = Seq.of(1, 2, 3, 4, 5)
     *     .reduce(10, (a, b) -> a + b);
     * // Result: 25 (10 + 1 + 2 + 3 + 4 + 5)
     * 
     * // String concatenation
     * String result = Seq.of("a", "b", "c")
     *     .reduce("Start:", (acc, str) -> acc + str);
     * // Result: "Start:abc"
     * }</pre>
     *
     * @param <U> the type of the result
     * @param <E2> the type of exception that the accumulator function may throw
     * @param identity the initial value for the accumulation
     * @param accumulator a function for combining the current accumulated value with a sequence element
     * @return the result of the reduction
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the accumulator function is null
     * @throws E if an exception occurs during iteration of the sequence
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

    /**
     * Collects the elements of this sequence into a mutable result container.
     * The supplier creates a new result container, and the accumulator incorporates each element into the container.
     * 
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect to List
     * List<String> list = Seq.of("a", "b", "c")
     *     .collect(ArrayList::new, List::add);
     * // Result: [a, b, c]
     * 
     * // Collect to StringBuilder
     * StringBuilder sb = Seq.of("Hello", " ", "World")
     *     .collect(StringBuilder::new, StringBuilder::append);
     * // Result: "Hello World"
     * }</pre>
     *
     * @param <R> the type of the mutable result container
     * @param <E2> the type of exception that the supplier may throw
     * @param <E3> the type of exception that the accumulator may throw
     * @param supplier a function that creates a new mutable result container
     * @param accumulator a function that folds an element into the result container
     * @return the result container with all elements of this sequence
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the supplier or accumulator is null
     * @throws E if an exception occurs during iteration of the sequence
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
     * Collects the elements of this sequence into a mutable result container, then applies a finisher function
     * to transform the container into the final result.
     * 
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect to List then get size
     * Integer size = Seq.of("a", "b", "c")
     *     .collect(ArrayList::new, List::add, List::size);
     * // Result: 3
     * 
     * // Collect to StringBuilder then convert to uppercase
     * String result = Seq.of("hello", " ", "world")
     *     .collect(StringBuilder::new, StringBuilder::append, 
     *              sb -> sb.toString().toUpperCase());
     * // Result: "HELLO WORLD"
     * }</pre>
     *
     * @param <R> the type of the mutable result container
     * @param <RR> the type of the final result
     * @param <E2> the type of exception that the supplier may throw
     * @param <E3> the type of exception that the accumulator may throw
     * @param <E4> the type of exception that the finisher may throw
     * @param supplier a function that creates a new mutable result container
     * @param accumulator a function that folds an element into the result container
     * @param finisher a function that transforms the result container into the final result
     * @return the final result after applying the finisher function
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the supplier, accumulator or finisher is null
     * @throws E if an exception occurs during iteration of the sequence
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
     * Collects the elements of this sequence using the provided Collector.
     * A Collector encapsulates the functions used as arguments to collect(Supplier, BiConsumer, Function),
     * allowing for reuse of collection strategies and composition of collect operations.
     * 
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect to List
     * List<String> list = Seq.of("a", "b", "c")
     *     .collect(Collectors.toList());
     * 
     * // Group by length
     * Map<Integer, List<String>> grouped = Seq.of("a", "bb", "ccc", "dd")
     *     .collect(Collectors.groupingBy(String::length));
     * // Result: {1=[a], 2=[bb, dd], 3=[ccc]}
     * }</pre>
     *
     * @param <R> the type of the result
     * @param collector the Collector describing the reduction
     * @return the result of the reduction
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the collector is null
     * @throws E if an exception occurs during iteration of the sequence
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
     * Collects the elements of this sequence using the provided Collector and then applies
     * the given function to the collected result.
     * 
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect to List then check if contains element
     * Boolean contains = Seq.of("a", "b", "c")
     *     .collectThenApply(Collectors.toList(), list -> list.contains("b"));
     * // Result: true
     * 
     * // Collect to Set then get size
     * Integer uniqueCount = Seq.of("a", "b", "a", "c", "b")
     *     .collectThenApply(Collectors.toSet(), Set::size);
     * // Result: 3
     * }</pre>
     *
     * @param <R> the type of the intermediate result collected
     * @param <RR> the type of the final result after applying the function
     * @param <E2> the type of exception that the function may throw
     * @param collector the Collector describing the reduction
     * @param func the function to apply to the collected result
     * @return the result of applying the function to the collected elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the collector or function is null
     * @throws E if an exception occurs during iteration of the sequence
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
     * Collects the elements of this sequence using the provided Collector and then passes
     * the collected result to the given consumer.
     * 
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Collect to List then print
     * Seq.of("a", "b", "c")
     *     .collectThenAccept(Collectors.toList(), System.out::println);
     * // Prints: [a, b, c]
     * 
     * // Collect to Map then process
     * Seq.of("apple", "banana", "cherry")
     *     .collectThenAccept(
     *         Collectors.toMap(Function.identity(), String::length),
     *         map -> map.forEach((k, v) -> System.out.println(k + " has " + v + " chars"))
     *     );
     * }</pre>
     *
     * @param <R> the type of the result collected
     * @param <E2> the type of exception that the consumer may throw
     * @param collector the Collector describing the reduction
     * @param consumer the consumer to accept the collected result
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the collector or consumer is null
     * @throws E if an exception occurs during iteration of the sequence
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
     * Joins the elements of this sequence into a single String with the specified delimiter.
     * Each element is converted to a String using String.valueOf() before joining.
     * 
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join with comma
     * String result = Seq.of("a", "b", "c").join(", ");
     * // Result: "a, b, c"
     * 
     * // Join numbers
     * String result = Seq.of(1, 2, 3).join("-");
     * // Result: "1-2-3"
     * }</pre>
     *
     * @param delimiter the delimiter to separate each element
     * @return a String containing all elements joined by the delimiter
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration of the sequence
     */
    @TerminalOp
    public String join(final CharSequence delimiter) throws IllegalStateException, E {
        return join(delimiter, "", "");
    }

    /**
     * Joins the elements of this sequence into a single String with the specified delimiter,
     * prefix, and suffix. Each element is converted to a String using String.valueOf() before joining.
     * 
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Join with brackets
     * String result = Seq.of("a", "b", "c").join(", ", "[", "]");
     * // Result: "[a, b, c]"
     * 
     * // Create SQL IN clause
     * String sql = Seq.of(1, 2, 3).join(",", "WHERE id IN (", ")");
     * // Result: "WHERE id IN (1,2,3)"
     * }</pre>
     *
     * @param delimiter the delimiter to separate each element
     * @param prefix the string to be added at the beginning of the result
     * @param suffix the string to be added at the end of the result
     * @return a String containing all elements joined by the delimiter with prefix and suffix
     * @throws IllegalStateException if the sequence is already closed
     * @throws E if an exception occurs during iteration of the sequence
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
     * Joins the elements of this sequence into the provided Joiner.
     * This allows for more control over the joining process and reuse of Joiner instances.
     * 
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Reuse a Joiner
     * Joiner joiner = Joiner.with(", ", "[", "]");
     * Seq.of("a", "b", "c").joinTo(joiner);
     * String result = joiner.toString(); // "[a, b, c]"
     * }</pre>
     *
     * @param joiner the Joiner to append the elements to
     * @return the provided Joiner after appending all elements
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the joiner is null
     * @throws E if an exception occurs during iteration of the sequence
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
     * Prints all elements of this sequence to the standard output (System.out).
     * The elements are formatted as a comma-separated list enclosed in square brackets: {@code [element1, element2, ...]}.
     * This method should be used with caution when the sequence is large, as it will load all elements into memory.
     *
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     *
     * <p><b>Implementation Note:</b> This method is equivalent to {@code System.out.println(seq.join(", ", "[", "]"))}</p> 
     *
     * @throws IllegalStateException if the sequence has already been closed
     * @throws E if an exception occurs during element processing
     */
    @Beta
    @TerminalOp
    public void println() throws IllegalStateException, E {
        N.println(join(", ", "[", "]"));
    }

    /**
     * Casts this sequence to a sequence with exception type {@code Exception}.
     * This is useful when you need to pass the sequence to a method that expects a more general exception type.
     * The operation does not modify the sequence's elements or behavior.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @return this sequence cast to {@code Seq<T, Exception>}
     * @throws IllegalStateException if the sequence has already been closed
     */
    @Beta
    @IntermediateOp
    public Seq<T, Exception> cast() throws IllegalStateException {
        assertNotClosed();

        return (Seq<T, Exception>) this;
    }

    /**
     * Converts this sequence to a sequential {@code Stream}.
     * The returned stream will have the same elements as this sequence.
     * If this sequence has close handlers registered, they will be transferred to the stream
     * and will be invoked when the stream is closed.
     *
     * <br />
     * This is an intermediate operation and will not close the sequence.
     *
     * @return a sequential {@code Stream} containing the same elements as this sequence
     * @throws IllegalStateException if the sequence has already been closed
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

    /**
     * Transforms this sequence into another sequence by applying the provided transformation function.
     * The function receives this sequence as input and returns a new sequence of potentially different element type.
     * This method enables custom sequence transformations and composition of sequence operations.
     *
     * <br />
     * <br />
     * To avoid eager loading by terminal operations invoked within the transfer function, 
     * use {@code Seq.defer(Supplier)} to create a lazily-evaluated sequence. For example:
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     *     seq.transform(s -> Seq.defer(() -> s.filter(...).map(...).someTerminalOperation(...)));
     * }</pre>
     *
     * @param <U> the type of elements in the returned sequence
     * @param transfer the transformation function that takes this sequence and returns a new sequence.
     *                 Must not be {@code null}
     * @return a new sequence resulting from applying the transformation function
     * @throws IllegalStateException if the sequence has already been closed
     * @throws IllegalArgumentException if the transfer function is {@code null}
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

        //    final Supplier<Seq<TT, EE>> delayInitializer = () -> transfer.apply(this);
        //
        //    return Seq.defer(delayInitializer);

        return transfer.apply(this);
    }

    /**
     * Transforms this sequence by converting it to a Stream, applying the provided transformation function,
     * and then converting the result back to a Seq.
     * This method is useful for leveraging Stream operations that are not directly available on Seq.
     *
     * @param <U> the type of elements in the returned sequence
     * @param transfer the transformation function that takes a Stream representation of this sequence 
     *                 and returns a new Stream. Must not be {@code null}
     * @return a new Seq containing the elements from the transformed Stream
     * @throws IllegalStateException if the sequence has already been closed
     * @throws IllegalArgumentException if the transfer function is {@code null}
     * @see #transform(Function)
     * @see #transformB(Function, boolean)
     * @see #sps(Function)
     * @see #sps(int, Function)
     */
    @Beta
    @IntermediateOp
    public <U> Seq<U, E> transformB(final Function<? super Stream<T>, ? extends Stream<? extends U>> transfer)
            throws IllegalStateException, IllegalArgumentException {

        return transformB(transfer, false);
    }

    /**
     * Transforms this sequence by converting it to a Stream, applying the provided transformation function,
     * and then converting the result back to a Seq.
     * This method supports deferred execution, allowing the transformation to be applied lazily
     * when the returned sequence is actually consumed.
     *
     * @param <U> the type of elements in the returned sequence
     * @param transfer the transformation function that takes a Stream representation of this sequence 
     *                 and returns a new Stream. Must not be {@code null}
     * @param deferred if {@code true}, the transformation is deferred and will only be executed when 
     *                 the returned sequence is consumed. If {@code false}, the transformation is applied immediately
     * @return a new Seq containing the elements from the transformed Stream
     * @throws IllegalArgumentException if the transfer function is {@code null}
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
            final Supplier<Seq<U, E>> delayInitializer = () -> create(transfer.apply(this.stream()), true);
            return Seq.defer(delayInitializer);
        } else {
            return create(transfer.apply(this.stream()), true);
        }
    }

    /**
     * Temporarily switches the sequence to a parallel stream for the operation defined by {@code ops}, 
     * and then switches it back to a sequence. This method allows parallel processing of a specific
     * portion of the sequence pipeline while maintaining the overall sequential nature of the Seq.
     * 
     * <p>The provided function receives a parallel stream created from this sequence's current state,
     * performs operations on it, and returns a new stream. The resulting stream is then converted
     * back to a Seq for continued sequential processing.</p>
     * 
     * <p>This is particularly useful when you have a computationally intensive operation that can
     * benefit from parallel processing, but you want to maintain sequential processing for the
     * rest of your pipeline.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<Integer, Exception> result = seq
     *     .sps(s -> s.map(expensiveOperation)
     *                .filter(x -> x > threshold));
     * }</pre>
     *
     * @param <R> the type of elements in the returned sequence
     * @param ops the function to be applied on the parallel stream. This function takes the
     *            current sequence converted to a parallel stream and returns a stream with
     *            elements of type R
     * @return a new sequence containing the elements resulting from the parallel stream operations
     * @throws IllegalStateException if the sequence has already been operated upon or closed
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
     * Temporarily switches the sequence to a parallel stream with a specified maximum number of threads
     * for the operation defined by {@code ops}, and then switches it back to a sequence. This method
     * provides fine-grained control over the parallelism level for specific operations.
     * 
     * <p>The {@code maxThreadNum} parameter controls the maximum number of threads that can be used
     * for parallel processing. This is useful when you want to limit resource consumption or when
     * you know the optimal parallelism level for your specific use case.</p>
     * 
     * <p>The provided function receives a parallel stream created from this sequence's current state
     * with the specified thread limit, performs operations on it, and returns a new stream. The
     * resulting stream is then converted back to a Seq for continued sequential processing.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Process with at most 4 threads
     * Seq<Result, Exception> result = seq
     *     .sps(4, s -> s.map(complexComputation)
     *                   .filter(Objects::nonNull));
     * }</pre>
     *
     * @param <R> the type of the elements in the resulting sequence
     * @param maxThreadNum the maximum number of threads to use for parallel processing. Must be
     *                     a positive integer greater than 0
     * @param ops the function defining the operations to be performed on the parallel stream.
     *            This function takes the current sequence converted to a parallel stream with
     *            the specified thread limit and returns a stream with elements of type R
     * @return a sequence containing the elements resulting from applying the operations defined
     *         by {@code ops} with the specified parallelism level
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws IllegalArgumentException if {@code maxThreadNum} is not positive (less than or equal to 0)
     * @see #sps(Function)
     * @see #sps(int, Executor, Function)
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

    /**
     * Temporarily switches the sequence to a parallel stream with a specified maximum number of threads
     * and custom executor for the operation defined by {@code ops}, and then switches it back to a sequence.
     * This method provides fine-grained control over both the parallelism level and the execution context
     * for specific operations.
     *
     * <p>The {@code maxThreadNum} parameter controls the maximum number of threads that can be used
     * for parallel processing, while the {@code executor} parameter allows you to specify a custom
     * thread pool or execution strategy. This combination is useful when you need precise control
     * over resource allocation, thread naming, priority, or other execution characteristics.</p>
     *
     * <p>The provided function receives a parallel stream created from this sequence's current state
     * with the specified thread limit and executor, performs operations on it, and returns a new stream.
     * The resulting stream is then converted back to a Seq for continued sequential processing.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Process with custom thread pool and thread limit
     * ExecutorService customExecutor = Executors.newFixedThreadPool(4);
     * Seq<Result, Exception> result = seq
     *     .sps(4, customExecutor, s -> s.map(complexComputation)
     *                                   .filter(Objects::nonNull));
     * }</pre>
     *
     * @param <R> the type of the elements in the resulting sequence
     * @param maxThreadNum the maximum number of threads to use for parallel processing. Must be
     *                     a positive integer greater than 0
     * @param executor the custom executor to use for parallel processing. Must not be {@code null}.
     *                 This can be any implementation of {@link Executor}, including thread pools,
     *                 custom schedulers, or specialized execution contexts
     * @param ops the function defining the operations to be performed on the parallel stream.
     *            This function takes the current sequence converted to a parallel stream with
     *            the specified thread limit and executor, and returns a stream with elements of type R
     * @return a sequence containing the elements resulting from applying the operations defined
     *         by {@code ops} with the specified parallelism level and executor
     * @throws IllegalStateException if the sequence has already been operated upon or closed
     * @throws IllegalArgumentException if {@code maxThreadNum} is not positive (less than or equal to 0)
     * @see #sps(Function)
     * @see #sps(int, Function)
     * @see #transform(Function)
     * @see #transformB(Function)
     * @see #transformB(Function, boolean)
     * @see Stream#parallel(int, Executor)
     */
    @Beta
    @IntermediateOp
    public <R> Seq<R, E> sps(final int maxThreadNum, final Executor executor, final Function<? super Stream<T>, ? extends Stream<? extends R>> ops)
            throws IllegalStateException {
        assertNotClosed();
        checkArgPositive(maxThreadNum, cs.maxThreadNum);

        return create(ops.apply(this.stream().parallel(maxThreadNum, executor)), true);
    }

    /**
     * Executes the provided terminal operation asynchronously on this sequence using the default executor.
     * The terminal operation is a consumer function that processes this entire sequence and may throw an exception.
     * 
     * <p>This method is useful for executing time-consuming terminal operations without blocking the
     * current thread. The operation runs asynchronously and returns a {@code ContinuableFuture<Void>}
     * that can be used to check completion status or chain additional operations.</p> 
     * 
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<Void> future = seq.asyncRun(s -> 
     *     s.forEach(item -> processItem(item))
     * );
     * 
     * // Wait for completion
     * future.get();
     * }</pre>
     * 
     *
     * @param terminalAction the terminal operation to be executed on this sequence. The consumer
     *                       receives this sequence as its parameter and may throw an exception
     * @return a ContinuableFuture representing the asynchronous computation. The future completes
     *         with {@code null} when the operation finishes successfully, or completes exceptionally
     *         if the operation throws an exception
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
     * Executes the provided terminal operation asynchronously on this sequence using the specified executor.
     * The terminal operation is a consumer function that processes this entire sequence and may throw an exception.
     * 
     * <p>This method provides control over which executor is used for the asynchronous execution,
     * allowing you to use custom thread pools or executors with specific configurations. This is
     * particularly useful when you need to manage thread resources or prioritize different types
     * of operations.</p>
     * 
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExecutorService customExecutor = Executors.newFixedThreadPool(4);
     * ContinuableFuture<Void> future = seq.asyncRun(s -> 
     *     s.filter(item -> item.isValid())
     *      .forEach(item -> saveToDatabase(item)),
     *     customExecutor
     * );
     * 
     * // Do other work while the operation runs
     * doOtherWork();
     * 
     * // Wait for completion
     * future.get();
     * }</pre>
     *
     * @param terminalAction the terminal operation to be executed on this sequence. The consumer
     *                       receives this sequence as its parameter and may throw an exception
     * @param executor the executor to run the terminal operation. This executor determines which
     *                 thread or thread pool will execute the operation
     * @return a ContinuableFuture representing the asynchronous computation. The future completes
     *         with {@code null} when the operation finishes successfully, or completes exceptionally
     *         if the operation throws an exception
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
     * Executes the provided terminal operation asynchronously on this sequence using the default executor
     * and returns a result. The terminal operation is a function that processes this entire sequence,
     * produces a result of type R, and may throw an exception.
     * 
     * <p>This method is useful for executing time-consuming terminal operations that produce a result
     * without blocking the current thread. Common use cases include asynchronously collecting results,
     * computing aggregates, or transforming the sequence into a different data structure.</p>
     * 
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ContinuableFuture<List<String>> future = seq.asyncCall(s -> 
     *     s.filter(item -> item.isValid())
     *      .map(Item::getName)
     *      .toList()
     * );
     * 
     * // Do other work while collecting
     * doOtherWork();
     * 
     * // Get the result
     * List<String> names = future.get();
     * }</pre>
     *
     * @param <R> the type of the result produced by the terminal operation
     * @param terminalAction the terminal operation to be executed on this sequence. The function
     *                       receives this sequence as its parameter, must return a value of type R,
     *                       and may throw an exception
     * @return a ContinuableFuture representing the asynchronous computation. The future completes
     *         with the result of the terminal operation when it finishes successfully, or completes
     *         exceptionally if the operation throws an exception
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
     * Executes the provided terminal operation asynchronously on this sequence using the specified executor
     * and returns a result. The terminal operation is a function that processes this entire sequence,
     * produces a result of type R, and may throw an exception.
     * 
     * <p>This method provides control over which executor is used for the asynchronous execution while
     * also returning a result. This allows you to use custom thread pools or executors with specific
     * configurations for different types of computations. For example, you might use a CPU-bound thread
     * pool for computational operations and an I/O-bound thread pool for database operations.</p>
     * 
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExecutorService ioExecutor = Executors.newCachedThreadPool();
     * ContinuableFuture<Map<String, List<Item>>> future = seq.asyncCall(s -> 
     *     s.filter(item -> item.isActive())
     *      .groupBy(Item::getCategory),
     *     ioExecutor
     * );
     * 
     * // Process the result when ready
     * future.thenAccept(groupedItems -> {
     *     groupedItems.forEach((category, items) -> 
     *         System.out.println(category + ": " + items.size())
     *     );
     * });
     * }</pre>
     * 
     *
     * @param <R> the type of the result produced by the terminal operation
     * @param terminalAction the terminal operation to be executed on this sequence. The function
     *                       receives this sequence as its parameter, must return a value of type R,
     *                       and may throw an exception
     * @param executor the executor to run the terminal operation. This executor determines which
     *                 thread or thread pool will execute the operation
     * @return a ContinuableFuture representing the asynchronous computation. The future completes
     *         with the result of the terminal operation when it finishes successfully, or completes
     *         exceptionally if the operation throws an exception
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
     * Applies the provided function to this sequence if it contains at least one element and returns
     * the result wrapped in an Optional. If the sequence is empty, returns an empty Optional without
     * executing the function.
     * 
     * <p>This method is useful when you want to perform a terminal operation on a sequence only if
     * it contains elements, avoiding unnecessary computation or side effects for empty sequences.
     * The function receives the entire sequence (including all its elements) as a parameter, not
     * just the first element.</p>
     * 
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Optional<Double> average = seq.applyIfNotEmpty(s -> 
     *     s.mapToDouble(Item::getPrice)
     *      .average()
     *      .orElse(0.0)
     * );
     * 
     * // Using the result
     * average.ifPresent(avg -> 
     *     System.out.println("Average price: " + avg)
     * );
     * 
     * // Another example: getting the first valid item
     * Optional<Item> firstValid = seq.applyIfNotEmpty(s ->
     *     s.filter(Item::isValid)
     *      .findFirst()
     *      .orElse(null)
     * );
     * }</pre>
     *
     * @param <R> the type of the result produced by the function
     * @param <E2> the type of the exception the function may throw
     * @param func the function to be applied to this sequence if it's not empty. The function
     *             receives the entire sequence as its parameter and can return null
     * @return an Optional containing the result of the function application if this sequence
     *         is not empty, or an empty Optional if the sequence is empty. If the function
     *         returns {@code null}, the Optional will contain null
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified function is null
     * @throws E if the underlying sequence operations throw an exception
     * @throws E2 if the provided function throws an exception
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
     * Executes the provided action on this sequence if it contains at least one element. If the
     * sequence is empty, the action is not executed. Returns an OrElse object that indicates
     * whether the action was executed.
     * 
     * <p>This method is useful when you want to perform side effects or terminal operations on a
     * sequence only if it contains elements. The action receives the entire sequence (including
     * all its elements) as a parameter, allowing you to perform any terminal operation on it.</p>
     * 
     * <p>This is a terminal operation. This sequence will be automatically closed after this operation completes, whether normally or exceptionally.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * seq.acceptIfNotEmpty(s -> 
     *     s.forEach(item -> processItem(item))
     * ).orElse(() -> 
     *     System.out.println("No items to process")
     * );
     * 
     * // Another example: saving to database if not empty
     * boolean wasProcessed = seq.acceptIfNotEmpty(s -> {
     *     List<Item> items = s.toList();
     *     database.saveAll(items);
     *     logger.info("Saved " + items.size() + " items");
     * }).isTrue();
     * 
     * if (!wasProcessed) {
     *     logger.info("No items to save");
     * }
     * }</pre>
     *
     * @param <E2> the type of the exception the action may throw
     * @param action the action to be executed on this sequence if it's not empty. The consumer
     *               receives the entire sequence as its parameter
     * @return an OrElse instance that is TRUE if the action was executed (sequence was not empty),
     *         or FALSE if the action was not executed (sequence was empty). This can be used to
     *         chain alternative actions for the empty case
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified action is null
     * @throws E if the underlying sequence operations throw an exception
     * @throws E2 if the provided action throws an exception
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

    /**
     * Registers a close handler to be invoked when the sequence is closed.
     * This method can be called multiple times to register multiple handlers.
     * Handlers are invoked in the order they were added (first-added, first-invoked).
     * 
     * <p>Close handlers are typically used to release resources, close connections, or perform cleanup
     * operations that should happen when the sequence processing is complete. This is particularly
     * useful when working with sequences that wrap external resources like files, network connections,
     * or database cursors.</p> 
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Connection conn = getConnection();
     * PreparedStatement stmt = conn.prepareStatement("SELECT * FROM users");
     * ResultSet rs = stmt.executeQuery();
     * 
     * Seq<User, SQLException> users = Seq.from(rs, User::fromResultSet)
     *     .onClose(() -> closeQuietly(rs))
     *     .onClose(() -> closeQuietly(stmt))
     *     .onClose(() -> closeQuietly(conn));
     * 
     * // Process users - resources will be closed automatically
     * List<String> names = users
     *     .map(User::getName)
     *     .toList();  // Terminal operation triggers close handlers
     * }</pre>
     * 
     * <p><b>Note:</b> Close handlers are executed in a synchronized manner to prevent concurrent
     * execution. If a close handler throws an exception, subsequent handlers will still be executed,
     * and all exceptions will be aggregated.</p>
     *
     * @param closeHandler the Runnable to be executed when the sequence is closed. If {@code null} or
     *                     empty, the sequence is returned unchanged
     * @return a sequence with the close handler registered. This may be the same sequence instance.
     * @throws IllegalStateException if the sequence is already closed
     * @throws IllegalArgumentException if the specified closeHandler is {@code null} (though {@code null} is handled
     *                                  gracefully by returning the sequence unchanged)
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

        this.closeHandlers = newCloseHandlers;

        return this;
    }

    /**
     * Closes the sequence, releasing any system resources associated with it.
     * If the sequence is already closed, then invoking this method has no effect.
     *
     * <p>Sequences are automatically closed when a terminal operation completes, so explicit closing
     * is typically not necessary. However, if a sequence may be abandoned before a terminal operation
     * (e.g., due to an exception), it should be closed explicitly to ensure proper resource cleanup.
     * 
     * <p>Close handlers are typically used to release external resources like file handles, database
     * connections, or network sockets that were used during sequence processing.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Seq<String, IOException> lines = null;
     * try {
     *     lines = Seq.fromFile("data.txt")
     *         .onClose(() -> System.out.println("File closed"));
     *     
     *     // Process some lines
     *     lines.limit(10).forEach(System.out::println);
     * } finally {
     *     if (lines != null) {
     *         lines.close(); // Explicit close in finally block
     *     }
     * }
     * 
     * // Or using try-with-resources (recommended)
     * try (Seq<String, IOException> lines = Seq.fromFile("data.txt")) {
     *     lines.limit(10).forEach(System.out::println);
     * } // Automatically closed here
     * }</pre>
     * 
     * <p><b>Note:</b> After closing, any attempt to perform operations on this sequence will result
     * in an {@link IllegalStateException}.</p>
     *
     * @see #onClose(Runnable)
     * @see AutoCloseable#close()
     */
    @TerminalOp
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
                    if (n <= 0) {
                        return;
                    }

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
                    if (n <= 0) {
                        return;
                    }

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
                if (n <= 0) {
                    return;
                }

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

    @SuppressWarnings("rawtypes")
    static LocalRunnable newCloseHandler(final Supplier<? extends Seq> seqSupplier) {
        return () -> {
            final Seq s = seqSupplier != null ? seqSupplier.get() : null;

            if (s != null && !s.isClosed && !isEmptyCloseHandlers(s.closeHandlers)) {
                s.close();
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
}
