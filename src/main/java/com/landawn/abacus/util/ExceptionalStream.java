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
import java.util.concurrent.Executor;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
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
import com.landawn.abacus.parser.ParserUtil.EntityInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Fn.Factory;
import com.landawn.abacus.util.Fn.Fnn;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.Strings.StringUtil;
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
 * @see BaseStream
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
public class ExceptionalStream<T, E extends Exception> implements Closeable, Immutable {

    static final Logger logger = LoggerFactory.getLogger(ExceptionalStream.class);

    static final Object NONE = N.NULL_MASK;

    static final Random RAND = new SecureRandom();

    static final Throwables.Function<OptionalInt, Integer, RuntimeException> GET_AS_INT = OptionalInt::get;

    static final Throwables.Function<OptionalLong, Long, RuntimeException> GET_AS_LONG = OptionalLong::get;

    static final Throwables.Function<OptionalDouble, Double, RuntimeException> GET_AS_DOUBLE = OptionalDouble::get;

    @SuppressWarnings("rawtypes")
    static final Throwables.Function<Optional, Object, RuntimeException> GET_AS_IT = it -> it.orElse(null);

    static final Throwables.Function<java.util.OptionalInt, Integer, RuntimeException> GET_AS_INT_JDK = java.util.OptionalInt::getAsInt;

    static final Throwables.Function<java.util.OptionalLong, Long, RuntimeException> GET_AS_LONG_JDK = java.util.OptionalLong::getAsLong;

    static final Throwables.Function<java.util.OptionalDouble, Double, RuntimeException> GET_AS_DOUBLE_JDK = java.util.OptionalDouble::getAsDouble;

    @SuppressWarnings("rawtypes")
    static final Throwables.Function<java.util.Optional, Object, RuntimeException> GET_AS_IT_JDK = it -> it.orElse(null);

    static final Throwables.Predicate<OptionalInt, RuntimeException> IS_PRESENT_INT = OptionalInt::isPresent;

    static final Throwables.Predicate<OptionalLong, RuntimeException> IS_PRESENT_LONG = OptionalLong::isPresent;

    static final Throwables.Predicate<OptionalDouble, RuntimeException> IS_PRESENT_DOUBLE = OptionalDouble::isPresent;

    @SuppressWarnings("rawtypes")
    static final Throwables.Predicate<Optional, RuntimeException> IS_PRESENT_IT = Optional::isPresent;

    static final Throwables.Predicate<java.util.OptionalInt, RuntimeException> IS_PRESENT_INT_JDK = java.util.OptionalInt::isPresent;

    static final Throwables.Predicate<java.util.OptionalLong, RuntimeException> IS_PRESENT_LONG_JDK = java.util.OptionalLong::isPresent;

    static final Throwables.Predicate<java.util.OptionalDouble, RuntimeException> IS_PRESENT_DOUBLE_JDK = java.util.OptionalDouble::isPresent;

    @SuppressWarnings("rawtypes")
    static final Throwables.Predicate<java.util.Optional, RuntimeException> IS_PRESENT_IT_JDK = java.util.Optional::isPresent;

    static final Throwables.Function<Map.Entry<Keyed<Object, Object>, Object>, Object, Exception> KK = t -> t.getKey().val();

    private final ExceptionalIterator<T, E> elements;

    private final boolean sorted;

    private final Comparator<? super T> cmp;

    private final Deque<Throwables.Runnable<? extends E>> closeHandlers;

    private boolean isClosed = false;

    ExceptionalStream(final ExceptionalIterator<T, E> iter) {
        this(iter, false, null, null);
    }

    ExceptionalStream(final ExceptionalIterator<T, E> iter, final Deque<Throwables.Runnable<? extends E>> closeHandlers) {
        this(iter, false, null, closeHandlers);
    }

    ExceptionalStream(final ExceptionalIterator<T, E> iter, final boolean sorted, final Comparator<? super T> comparator,
            final Deque<Throwables.Runnable<? extends E>> closeHandlers) {
        this.elements = iter;
        this.sorted = sorted;
        this.cmp = comparator;
        this.closeHandlers = closeHandlers;
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @return
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> empty() {
        return new ExceptionalStream<>(ExceptionalIterator.EMPTY);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param e
     * @return
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> just(final T e) {
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
    public static <T, E extends Exception> ExceptionalStream<T, E> just(final T e, @SuppressWarnings("unused") final Class<E> exceptionType) {
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
    public static <T, E extends Exception> ExceptionalStream<T, E> ofNullable(final T e) {
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
     * @return
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> ofNullable(final T e, @SuppressWarnings("unused") final Class<E> exceptionType) {
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
    public static <T, E extends Exception> ExceptionalStream<T, E> of(final T... a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return newStream(new ExceptionalIterator<T, E>() {
            private int position = 0;

            @Override
            public boolean hasNext() throws E {
                return position < len;
            }

            @Override
            public T next() throws E {
                if (position >= len) {
                    throw new NoSuchElementException();
                }

                return a[position++];
            }

            @Override
            public long count() throws E {
                return len - position;
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
     * @param c
     * @return
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> of(final Collection<? extends T> c) {
        if (N.isNullOrEmpty(c)) {
            return empty();
        }

        @SuppressWarnings("deprecation")
        final T[] a = (T[]) InternalUtil.getInternalArray(c);

        if (a != null) {
            final int len = c.size();

            return newStream(new ExceptionalIterator<T, E>() {
                private int position = 0;

                @Override
                public boolean hasNext() throws E {
                    return position < len;
                }

                @Override
                public T next() throws E {
                    if (position >= len) {
                        throw new NoSuchElementException();
                    }

                    return a[position++];
                }

                @Override
                public long count() throws E {
                    return len - position;
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

        return of(c.iterator());
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @return
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> of(final Iterator<? extends T> iter) {
        if (iter == null) {
            return empty();
        }

        return newStream(ExceptionalIterator.<T, E> wrap(iter));
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iterable
     * @return
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> of(final Iterable<? extends T> iterable) {
        if (iterable == null) {
            return empty();
        }

        if (iterable instanceof Collection) {
            return of((Collection<T>) iterable);
        } else {
            return of(iterable.iterator());
        }
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <E>
     * @param m
     * @return
     */
    public static <K, V, E extends Exception> ExceptionalStream<Map.Entry<K, V>, E> of(final Map<K, V> m) {
        if (N.isNullOrEmpty(m)) {
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
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> of(final Stream<? extends T> stream) {
        return checked(stream, false);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param stream
     * @return
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> of(final java.util.stream.Stream<? extends T> stream) {
        if (stream == null) {
            return empty();
        }

        return ExceptionalStream.<T, E> of(stream.iterator()).onClose(stream::close);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param c
     * @param exceptionType
     * @return
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> of(final Collection<? extends T> c,
            @SuppressWarnings("unused") final Class<E> exceptionType) {
        return of(c);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param exceptionType
     * @return
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> of(final Iterator<? extends T> iter,
            @SuppressWarnings("unused") final Class<E> exceptionType) {
        return of(iter);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iterable
     * @param exceptionType
     * @return
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> of(final Iterable<? extends T> iterable,
            @SuppressWarnings("unused") final Class<E> exceptionType) {
        return of(iterable);
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
    public static <K, V, E extends Exception> ExceptionalStream<Map.Entry<K, V>, E> of(final Map<K, V> m,
            @SuppressWarnings("unused") final Class<E> exceptionType) {
        return of(m);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param stream
     * @param exceptionType
     * @return
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> of(final Stream<? extends T> stream,
            @SuppressWarnings("unused") final Class<E> exceptionType) {
        return of(stream);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param stream
     * @param exceptionType
     * @return
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> of(final java.util.stream.Stream<? extends T> stream,
            @SuppressWarnings("unused") final Class<E> exceptionType) {
        return of(stream);
    }

    public static <E extends Exception> ExceptionalStream<Boolean, E> of(final boolean[] a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return newStream(new ExceptionalIterator<Boolean, E>() {
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
                return len - position;
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

    public static <E extends Exception> ExceptionalStream<Character, E> of(final char[] a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return newStream(new ExceptionalIterator<Character, E>() {
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
                return len - position;
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

    public static <E extends Exception> ExceptionalStream<Byte, E> of(final byte[] a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return newStream(new ExceptionalIterator<Byte, E>() {
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
                return len - position;
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

    public static <E extends Exception> ExceptionalStream<Short, E> of(final short[] a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return newStream(new ExceptionalIterator<Short, E>() {
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
                return len - position;
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
    public static <E extends Exception> ExceptionalStream<Integer, E> of(final int[] a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return newStream(new ExceptionalIterator<Integer, E>() {
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
                return len - position;
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
    public static <E extends Exception> ExceptionalStream<Long, E> of(final long[] a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return newStream(new ExceptionalIterator<Long, E>() {
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
                return len - position;
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

    public static <E extends Exception> ExceptionalStream<Float, E> of(final float[] a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return newStream(new ExceptionalIterator<Float, E>() {
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
                return len - position;
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
    public static <E extends Exception> ExceptionalStream<Double, E> of(final double[] a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        final int len = N.len(a);

        return newStream(new ExceptionalIterator<Double, E>() {
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
                return len - position;
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

    public static <T, E extends Exception> ExceptionalStream<T, E> of(final Optional<T> op) {
        return op == null || !op.isPresent() ? ExceptionalStream.<T, E> empty() : ExceptionalStream.<T, E> of(op.get());
    }

    public static <T, E extends Exception> ExceptionalStream<T, E> of(final java.util.Optional<T> op) {
        return op == null || !op.isPresent() ? ExceptionalStream.<T, E> empty() : ExceptionalStream.<T, E> of(op.get());
    }

    public static <K, E extends Exception> ExceptionalStream<K, E> ofKeys(final Map<K, ?> map) {
        if (N.isNullOrEmpty(map)) {
            return empty();
        }

        return of(map.keySet());
    }

    public static <K, V, E extends Exception> ExceptionalStream<K, E> ofKeys(final Map<K, V> map, final Throwables.Predicate<? super V, E> valueFilter) {
        if (N.isNullOrEmpty(map)) {
            return empty();
        }

        return ExceptionalStream.<K, V, E> of(map).filter(Fnn.<K, V, E> testByValue(valueFilter)).map(Fnn.<K, V, E> key());
    }

    public static <K, V, E extends Exception> ExceptionalStream<K, E> ofKeys(final Map<K, V> map,
            final Throwables.BiPredicate<? super K, ? super V, E> filter) {
        if (N.isNullOrEmpty(map)) {
            return empty();
        }

        return ExceptionalStream.<K, V, E> of(map).filter(Fn.Entries.ep(filter)).map(Fnn.<K, V, E> key());
    }

    public static <V, E extends Exception> ExceptionalStream<V, E> ofValues(final Map<?, V> map) {
        if (N.isNullOrEmpty(map)) {
            return empty();
        }

        return of(map.values());
    }

    public static <K, V, E extends Exception> ExceptionalStream<V, E> ofValues(final Map<K, V> map, final Throwables.Predicate<? super K, E> keyFilter) {
        if (N.isNullOrEmpty(map)) {
            return empty();
        }

        return ExceptionalStream.<K, V, E> of(map).filter(Fnn.<K, V, E> testByKey(keyFilter)).map(Fnn.<K, V, E> value());
    }

    public static <K, V, E extends Exception> ExceptionalStream<V, E> ofValues(final Map<K, V> map,
            final Throwables.BiPredicate<? super K, ? super V, E> filter) {
        if (N.isNullOrEmpty(map)) {
            return empty();
        }

        return ExceptionalStream.<K, V, E> of(map).filter(Fn.Entries.ep(filter)).map(Fnn.<K, V, E> value());
    }

    /**
     * Lazy evaluation.
     * <br />
     *
     * This is equal to: {@code ExceptionalStream.just(supplier).flatmap(it -> it.get())}.
     *
     * @param supplier
     * @return
     */
    @Beta
    public static <T, E extends Exception> ExceptionalStream<T, E> from(final Throwables.Supplier<? extends Collection<? extends T>, ? extends E> supplier) {
        N.checkArgNotNull(supplier, "supplier");

        return ExceptionalStream.<Throwables.Supplier<? extends Collection<? extends T>, ? extends E>, E> just(supplier)
                .flatmap(com.landawn.abacus.util.Throwables.Supplier::get);
    }

    /**
     * Lazy evaluation.
     * <br />
     *
     * This is equal to: {@code ExceptionalStream.just(supplier).flatMap(it -> it.get())}.
     *
     * @param supplier
     * @return
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> defer(
            final Throwables.Supplier<? extends ExceptionalStream<? extends T, ? extends E>, ? extends E> supplier) {
        N.checkArgNotNull(supplier, "supplier");

        return ExceptionalStream.<Throwables.Supplier<? extends ExceptionalStream<? extends T, ? extends E>, ? extends E>, E> just(supplier)
                .flatMap(com.landawn.abacus.util.Throwables.Supplier::get);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param hasNext
     * @param next
     * @return
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> iterate(final Throwables.BooleanSupplier<? extends E> hasNext,
            final Throwables.Supplier<? extends T, E> next) {
        N.checkArgNotNull(hasNext, "hasNext");
        N.checkArgNotNull(next, "next");

        return newStream(new ExceptionalIterator<T, E>() {
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
                    throw new NoSuchElementException();
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
    public static <T, E extends Exception> ExceptionalStream<T, E> iterate(final T init, final Throwables.BooleanSupplier<? extends E> hasNext,
            final Throwables.UnaryOperator<T, ? extends E> f) {
        N.checkArgNotNull(hasNext, "hasNext");
        N.checkArgNotNull(f, "f");

        return newStream(new ExceptionalIterator<T, E>() {
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
                    throw new NoSuchElementException();
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
    public static <T, E extends Exception> ExceptionalStream<T, E> iterate(final T init, final Throwables.Predicate<? super T, ? extends E> hasNext,
            final Throwables.UnaryOperator<T, ? extends E> f) {
        N.checkArgNotNull(hasNext, "hasNext");
        N.checkArgNotNull(f, "f");

        return newStream(new ExceptionalIterator<T, E>() {
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
                    throw new NoSuchElementException();
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
    public static <T, E extends Exception> ExceptionalStream<T, E> iterate(final T init, final Throwables.UnaryOperator<T, ? extends E> f) {
        N.checkArgNotNull(f, "f");

        return newStream(new ExceptionalIterator<T, E>() {
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

    public static <T, E extends Exception> ExceptionalStream<T, E> generate(final Throwables.Supplier<T, E> supplier) {
        N.checkArgNotNull(supplier, "supplier");

        return newStream(new ExceptionalIterator<T, E>() {
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

    public static <T, E extends Exception> ExceptionalStream<T, E> repeat(final T element, final long n) {
        N.checkArgNotNegative(n, "n");

        if (n == 0) {
            return empty();
        }

        return newStream(new ExceptionalIterator<T, E>() {
            private long cnt = n;

            @Override
            public boolean hasNext() throws E {
                return cnt > 0;
            }

            @Override
            public T next() throws E {
                if (cnt-- <= 0) {
                    throw new NoSuchElementException();
                }

                return element;
            }
        });
    }

    public static <E extends Exception> ExceptionalStream<Integer, E> range(final int startInclusive, final int endExclusive) {
        return IntStream.range(startInclusive, endExclusive).boxed().<E> checked();
    }

    public static <E extends Exception> ExceptionalStream<Integer, E> range(final int startInclusive, final int endExclusive, final int by) {
        return IntStream.range(startInclusive, endExclusive, by).boxed().<E> checked();
    }

    public static <E extends Exception> ExceptionalStream<Integer, E> rangeClosed(final int startInclusive, final int endExclusive) {
        return IntStream.rangeClosed(startInclusive, endExclusive).boxed().<E> checked();
    }

    public static <E extends Exception> ExceptionalStream<Integer, E> rangeClosed(final int startInclusive, final int endExclusive, final int by) {
        return IntStream.rangeClosed(startInclusive, endExclusive, by).boxed().<E> checked();
    }

    public static <E extends Exception> ExceptionalStream<String, E> split(final CharSequence str, final CharSequence delimiter) {
        return of(Splitter.with(delimiter).iterate(str));
    }

    /**
     *
     * @param file
     * @return
     */
    public static ExceptionalStream<String, IOException> lines(final File file) {
        return lines(file, Charsets.UTF_8);
    }

    /**
     *
     * @param file
     * @param charset
     * @return
     */
    public static ExceptionalStream<String, IOException> lines(final File file, final Charset charset) {
        N.checkArgNotNull(file, "file");

        final ExceptionalIterator<String, IOException> iter = createLazyLineIterator(file, null, charset, null, true);

        return newStream(iter).onClose(newCloseHandler(iter));
    }

    /**
     *
     * @param path
     * @return
     */
    public static ExceptionalStream<String, IOException> lines(final Path path) {
        return lines(path, Charsets.UTF_8);
    }

    /**
     *
     * @param path
     * @param charset
     * @return
     */
    public static ExceptionalStream<String, IOException> lines(final Path path, final Charset charset) {
        N.checkArgNotNull(path, "path");

        final ExceptionalIterator<String, IOException> iter = createLazyLineIterator(null, path, charset, null, true);

        return newStream(iter).onClose(newCloseHandler(iter));
    }

    /**
     *
     * @param reader
     * @return
     */
    public static ExceptionalStream<String, IOException> lines(final Reader reader) {
        N.checkArgNotNull(reader, "reader");

        return newStream(createLazyLineIterator(null, null, Charsets.UTF_8, reader, false));
    }

    public static ExceptionalStream<File, IOException> listFiles(final File parentPath) {
        if (!parentPath.exists()) {
            return empty();
        }

        return of(parentPath.listFiles());
    }

    public static ExceptionalStream<File, IOException> listFiles(final File parentPath, final boolean recursively) {
        if (!parentPath.exists()) {
            return empty();
        } else if (!recursively) {
            return of(parentPath.listFiles());
        }

        final ExceptionalIterator<File, IOException> iter = new ExceptionalIterator<>() {
            private final Queue<File> paths = N.asLinkedList(parentPath);
            private File[] subFiles = null;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                if ((subFiles == null || cursor >= subFiles.length) && paths.size() > 0) {
                    cursor = 0;
                    subFiles = null;

                    while (paths.size() > 0) {
                        subFiles = paths.poll().listFiles();

                        if (N.notNullOrEmpty(subFiles)) {
                            break;
                        }
                    }
                }

                return subFiles != null && cursor < subFiles.length;
            }

            @Override
            public File next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
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
    private static ExceptionalIterator<String, IOException> createLazyLineIterator(final File file, final Path path, final Charset charset, final Reader reader,
            final boolean closeReader) {
        return ExceptionalIterator.from(new Throwables.Supplier<ExceptionalIterator<String, IOException>, IOException>() {
            private ExceptionalIterator<String, IOException> lazyIter = null;

            @Override
            public synchronized ExceptionalIterator<String, IOException> get() {
                if (lazyIter == null) {
                    lazyIter = new ExceptionalIterator<>() {
                        private BufferedReader bufferedReader;

                        {
                            if (reader != null) {
                                bufferedReader = reader instanceof BufferedReader ? ((BufferedReader) reader) : new BufferedReader(reader);
                            } else if (file != null) {
                                bufferedReader = IOUtil.newBufferedReader(file, charset == null ? Charsets.UTF_8 : charset);
                            } else {
                                bufferedReader = IOUtil.newBufferedReader(path, charset == null ? Charsets.UTF_8 : charset);
                            }
                        }

                        private String cachedLine;
                        private boolean finished = false;

                        @Override
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

                        @Override
                        public String next() throws IOException {
                            if (!this.hasNext()) {
                                throw new NoSuchElementException("No more lines");
                            } else {
                                String res = this.cachedLine;
                                this.cachedLine = null;
                                return res;
                            }
                        }

                        @Override
                        public void close() throws IOException {
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

    @SafeVarargs
    public static <T, E extends Exception> ExceptionalStream<T, E> concat(final T[]... a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(Iterators.concat(a));
    }

    @SafeVarargs
    public static <T, E extends Exception> ExceptionalStream<T, E> concat(final Iterable<? extends T>... a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return of(Iterators.concat(a));
    }

    @SafeVarargs
    public static <T, E extends Exception> ExceptionalStream<T, E> concat(final Iterator<? extends T>... a) {
        if (N.isNullOrEmpty(a)) {
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
    public static <T, E extends Exception> ExceptionalStream<T, E> concat(final ExceptionalStream<? extends T, E>... a) {
        if (N.isNullOrEmpty(a)) {
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
    public static <T, E extends Exception> ExceptionalStream<T, E> concat(final Collection<? extends ExceptionalStream<? extends T, E>> c) {
        if (N.isNullOrEmpty(c)) {
            return empty();
        }

        return newStream(new ExceptionalIterator<T, E>() {
            private final Iterator<? extends ExceptionalStream<? extends T, E>> iterators = c.iterator();
            private ExceptionalStream<? extends T, E> cur;
            private ExceptionalIterator<? extends T, E> iter;

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
                    throw new NoSuchElementException();
                }

                return iter.next();
            }
        }, mergeCloseHandlers(c));
    }

    /**
     * Zip together the "a" and "b" arrays until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     *
     * @param a
     * @param b
     * @return
     */
    public static <A, B, T, E extends Exception> ExceptionalStream<T, E> zip(final A[] a, final B[] b,
            final Throwables.BiFunction<? super A, ? super B, T, ? extends E> zipFunction) {
        return zip(ObjIterator.of(a), ObjIterator.of(b), zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     *
     * @param a
     * @param b
     * @return
     */
    public static <A, B, C, T, E extends Exception> ExceptionalStream<T, E> zip(final A[] a, final B[] b, final C[] c,
            final Throwables.TriFunction<? super A, ? super B, ? super C, T, ? extends E> zipFunction) {
        return zip(ObjIterator.of(a), ObjIterator.of(b), ObjIterator.of(c), zipFunction);
    }

    /**
     * Zip together the "a" and "b" arrays until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     *
     * @param a
     * @param b
     * @return
     */
    public static <A, B, T, E extends Exception> ExceptionalStream<T, E> zip(final Iterable<? extends A> a, final Iterable<? extends B> b,
            final Throwables.BiFunction<? super A, ? super B, T, ? extends E> zipFunction) {
        return zip(N.iterate(a), N.iterate(b), zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     *
     * @param a
     * @param b
     * @return
     */
    public static <A, B, C, T, E extends Exception> ExceptionalStream<T, E> zip(final Iterable<? extends A> a, final Iterable<? extends B> b,
            final Iterable<? extends C> c, final Throwables.TriFunction<? super A, ? super B, ? super C, T, ? extends E> zipFunction) {
        return zip(N.iterate(a), N.iterate(b), N.iterate(c), zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     *
     * @param a
     * @param b
     * @return
     */
    public static <A, B, T, E extends Exception> ExceptionalStream<T, E> zip(final Iterator<? extends A> a, final Iterator<? extends B> b,
            final Throwables.BiFunction<? super A, ? super B, T, ? extends E> zipFunction) {
        return newStream(new ExceptionalIterator<T, E>() {
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
     * @param a
     * @param b
     * @return
     */
    public static <A, B, C, T, E extends Exception> ExceptionalStream<T, E> zip(final Iterator<? extends A> a, final Iterator<? extends B> b,
            final Iterator<? extends C> c, final Throwables.TriFunction<? super A, ? super B, ? super C, T, ? extends E> zipFunction) {
        return newStream(new ExceptionalIterator<T, E>() {
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
     * @param a
     * @param b
     * @return
     */
    public static <A, B, T, E extends Exception> ExceptionalStream<T, E> zip(final ExceptionalStream<? extends A, E> a,
            final ExceptionalStream<? extends B, E> b, final Throwables.BiFunction<? super A, ? super B, T, ? extends E> zipFunction) {
        return newStream(new ExceptionalIterator<T, E>() {
            private final ExceptionalIterator<? extends A, E> iterA = iterate(a);
            private final ExceptionalIterator<? extends B, E> iterB = iterate(b);

            @Override
            public boolean hasNext() throws E {
                return iterA.hasNext() && iterB.hasNext();
            }

            @Override
            public T next() throws E {
                return zipFunction.apply(iterA.next(), iterB.next());
            }
        }, mergeCloseHandlers(Array.asList(a, b)));
    }

    /**
     * Zip together the "a", "b" and "c" streams until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static <A, B, C, T, E extends Exception> ExceptionalStream<T, E> zip(final ExceptionalStream<? extends A, E> a,
            final ExceptionalStream<? extends B, E> b, final ExceptionalStream<? extends C, E> c,
            final Throwables.TriFunction<? super A, ? super B, ? super C, T, ? extends E> zipFunction) {
        return newStream(new ExceptionalIterator<T, E>() {
            private final ExceptionalIterator<? extends A, E> iterA = iterate(a);
            private final ExceptionalIterator<? extends B, E> iterB = iterate(b);
            private final ExceptionalIterator<? extends C, E> iterC = iterate(c);

            @Override
            public boolean hasNext() throws E {
                return iterA.hasNext() && iterB.hasNext() && iterC.hasNext();
            }

            @Override
            public T next() throws E {
                return zipFunction.apply(iterA.next(), iterB.next(), iterC.next());
            }
        }, mergeCloseHandlers(Array.asList(a, b, c)));
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     *
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <A, B, T, E extends Exception> ExceptionalStream<T, E> zip(final A[] a, final B[] b, final A valueForNoneA, final B valueForNoneB,
            final Throwables.BiFunction<? super A, ? super B, T, ? extends E> zipFunction) {
        return zip(ObjIterator.of(a), ObjIterator.of(b), valueForNoneA, valueForNoneB, zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     *
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <A, B, C, T, E extends Exception> ExceptionalStream<T, E> zip(final A[] a, final B[] b, final C[] c, final A valueForNoneA,
            final B valueForNoneB, final C valueForNoneC, final Throwables.TriFunction<? super A, ? super B, ? super C, T, ? extends E> zipFunction) {
        return zip(ObjIterator.of(a), ObjIterator.of(b), ObjIterator.of(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     *
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <A, B, T, E extends Exception> ExceptionalStream<T, E> zip(final Iterable<? extends A> a, final Iterable<? extends B> b,
            final A valueForNoneA, final B valueForNoneB, final Throwables.BiFunction<? super A, ? super B, T, ? extends E> zipFunction) {
        return zip(N.iterate(a), N.iterate(b), valueForNoneA, valueForNoneB, zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     *
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <A, B, C, T, E extends Exception> ExceptionalStream<T, E> zip(final Iterable<? extends A> a, final Iterable<? extends B> b,
            final Iterable<? extends C> c, final A valueForNoneA, final B valueForNoneB, final C valueForNoneC,
            final Throwables.TriFunction<? super A, ? super B, ? super C, T, ? extends E> zipFunction) {
        return zip(N.iterate(a), N.iterate(b), N.iterate(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until all of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     *
     * @param a
     * @param b
     * @param valueForNoneA value to fill if "a" runs out of values first.
     * @param valueForNoneB value to fill if "b" runs out of values first.
     * @param zipFunction
     * @return
     */
    public static <A, B, T, E extends Exception> ExceptionalStream<T, E> zip(final Iterator<? extends A> a, final Iterator<? extends B> b,
            final A valueForNoneA, final B valueForNoneB, final Throwables.BiFunction<? super A, ? super B, T, ? extends E> zipFunction) {
        return newStream(new ExceptionalIterator<T, E>() {
            private final Iterator<? extends A> iterA = a == null ? ObjIterator.<A> empty() : a;
            private final Iterator<? extends B> iterB = b == null ? ObjIterator.<B> empty() : b;

            @Override
            public boolean hasNext() throws E {
                return iterA.hasNext() || iterB.hasNext();
            }

            @Override
            public T next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(iterA.hasNext() ? iterA.next() : valueForNoneA, iterB.hasNext() ? iterB.next() : valueForNoneB);
            }
        });
    }

    /**
     * Zip together the "a", "b" and "c" iterators until all of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     *
     * @param a
     * @param b
     * @param c
     * @param valueForNoneA value to fill if "a" runs out of values.
     * @param valueForNoneB value to fill if "b" runs out of values.
     * @param valueForNoneC value to fill if "c" runs out of values.
     * @param zipFunction
     * @return
     */
    public static <A, B, C, T, E extends Exception> ExceptionalStream<T, E> zip(final Iterator<? extends A> a, final Iterator<? extends B> b,
            final Iterator<? extends C> c, final A valueForNoneA, final B valueForNoneB, final C valueForNoneC,
            final Throwables.TriFunction<? super A, ? super B, ? super C, T, ? extends E> zipFunction) {
        return newStream(new ExceptionalIterator<T, E>() {
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
                    throw new NoSuchElementException();
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
     * @param a
     * @param b
     * @return
     */
    public static <A, B, T, E extends Exception> ExceptionalStream<T, E> zip(final ExceptionalStream<? extends A, E> a,
            final ExceptionalStream<? extends B, E> b, final A valueForNoneA, final B valueForNoneB,
            final Throwables.BiFunction<? super A, ? super B, T, ? extends E> zipFunction) {
        return newStream(new ExceptionalIterator<T, E>() {
            private final ExceptionalIterator<? extends A, E> iterA = iterate(a);
            private final ExceptionalIterator<? extends B, E> iterB = iterate(b);

            @Override
            public boolean hasNext() throws E {
                return iterA.hasNext() || iterB.hasNext();
            }

            @Override
            public T next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(iterA.hasNext() ? iterA.next() : valueForNoneA, iterB.hasNext() ? iterB.next() : valueForNoneB);
            }
        }, mergeCloseHandlers(Array.asList(a, b)));
    }

    /**
     * Zip together the "a", "b" and "c" streams until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    public static <A, B, C, T, E extends Exception> ExceptionalStream<T, E> zip(final ExceptionalStream<? extends A, E> a,
            final ExceptionalStream<? extends B, E> b, final ExceptionalStream<? extends C, E> c, final A valueForNoneA, final B valueForNoneB,
            final C valueForNoneC, final Throwables.TriFunction<? super A, ? super B, ? super C, T, ? extends E> zipFunction) {
        return newStream(new ExceptionalIterator<T, E>() {
            private final ExceptionalIterator<? extends A, E> iterA = iterate(a);
            private final ExceptionalIterator<? extends B, E> iterB = iterate(b);
            private final ExceptionalIterator<? extends C, E> iterC = iterate(c);

            @Override
            public boolean hasNext() throws E {
                return iterA.hasNext() || iterB.hasNext() || iterC.hasNext();
            }

            @Override
            public T next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(iterA.hasNext() ? iterA.next() : valueForNoneA, iterB.hasNext() ? iterB.next() : valueForNoneB,
                        iterC.hasNext() ? iterC.next() : valueForNoneC);
            }
        }, mergeCloseHandlers(Array.asList(a, b, c)));
    }

    private static <E extends Exception> Deque<Throwables.Runnable<? extends E>> mergeCloseHandlers(
            Collection<? extends ExceptionalStream<?, E>> closeHandlersList) {
        if (N.isNullOrEmpty(closeHandlersList)) {
            return null;
        }

        int count = 0;

        for (ExceptionalStream<?, E> s : closeHandlersList) {
            count += N.size(s.closeHandlers);
        }

        if (count == 0) {
            return null;
        }

        final Deque<Throwables.Runnable<? extends E>> newCloseHandlers = new ArrayDeque<>(count);

        for (ExceptionalStream<?, E> s : closeHandlersList) {
            if (s.isClosed || isEmptyCloseHandlers(s.closeHandlers)) {
                continue;
            }

            newCloseHandlers.addAll(s.closeHandlers);
        }

        return newCloseHandlers;
    }

    /**
     *
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> merge(final T[] a, final T[] b,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        if (N.isNullOrEmpty(a)) {
            return of(b);
        } else if (N.isNullOrEmpty(b)) {
            return of(a);
        }

        return newStream(new ExceptionalIterator<T, E>() {
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
                    throw new NoSuchElementException();
                }
            }
        });
    }

    /**
     *
     * @param a
     * @param b
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> merge(final T[] a, final T[] b, final T[] c,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return merge(merge(a, b, nextSelector).iteratorEx(), ExceptionalIterator.<T, E> wrap(N.iterate(c)), nextSelector);
    }

    /**
     *
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> merge(final Iterable<? extends T> a, final Iterable<? extends T> b,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return merge(N.iterate(a), N.iterate(b), nextSelector);
    }

    /**
     *
     * @param a
     * @param b
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> merge(final Iterable<? extends T> a, final Iterable<? extends T> b,
            final Iterable<? extends T> c, final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return merge(N.iterate(a), N.iterate(b), N.iterate(c), nextSelector);
    }

    /**
     *
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> merge(final Iterator<? extends T> a, final Iterator<? extends T> b,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return merge(ExceptionalIterator.<T, E> wrap(a), ExceptionalIterator.<T, E> wrap(b), nextSelector);
    }

    /**
     *
     * @param a
     * @param b
     * @param c
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> merge(final Iterator<? extends T> a, final Iterator<? extends T> b,
            final Iterator<? extends T> c, final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return merge(merge(a, b, nextSelector).iteratorEx(), ExceptionalIterator.<T, E> wrap(c), nextSelector);
    }

    /**
     *
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> merge(final ExceptionalStream<? extends T, E> a, final ExceptionalStream<? extends T, E> b,
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

    public static <T, E extends Exception> ExceptionalStream<T, E> merge(final ExceptionalStream<? extends T, E> a, final ExceptionalStream<? extends T, E> b,
            final ExceptionalStream<? extends T, E> c, final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return merge(merge(a, b, nextSelector), c, nextSelector);
    }

    static <T, E extends Exception> ExceptionalStream<T, E> merge(final ExceptionalIterator<? extends T, E> a, final ExceptionalIterator<? extends T, E> b,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        return newStream(new ExceptionalIterator<T, E>() {
            private final ExceptionalIterator<T, E> iterA = a == null ? ExceptionalIterator.EMPTY : (ExceptionalIterator<T, E>) a;
            private final ExceptionalIterator<T, E> iterB = b == null ? ExceptionalIterator.EMPTY : (ExceptionalIterator<T, E>) b;

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
                    throw new NoSuchElementException();
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
    public ExceptionalStream<T, E> filter(final Throwables.Predicate<? super T, ? extends E> predicate) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<T, E>() {
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
                    throw new NoSuchElementException();
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
    public ExceptionalStream<T, E> filter(final Throwables.Predicate<? super T, ? extends E> predicate,
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
    public ExceptionalStream<T, E> takeWhile(final Throwables.Predicate<? super T, ? extends E> predicate) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<T, E>() {
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
                    throw new NoSuchElementException();
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
    public ExceptionalStream<T, E> dropWhile(final Throwables.Predicate<? super T, ? extends E> predicate) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<T, E>() {
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
                    throw new NoSuchElementException();
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
    public ExceptionalStream<T, E> dropWhile(final Throwables.Predicate<? super T, ? extends E> predicate,
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
    public ExceptionalStream<T, E> skipUntil(final Throwables.Predicate<? super T, ? extends E> predicate) {
        return dropWhile(Fnn.not(predicate));
    }

    /**
     * Distinct and filter by occurrences.
     *
     * @return
     */
    @IntermediateOp
    public ExceptionalStream<T, E> distinct() {
        assertNotClosed();

        final Set<Object> set = N.newHashSet();

        return filter(value -> set.add(hashKey(value)));
    }

    /**
     * Distinct and filter by occurrences.
     *
     * @param mergeFunction
     * @return
     * @see #groupBy(Function, Function, BinaryOperator)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public ExceptionalStream<T, E> distinct(final Throwables.BinaryOperator<T, ? extends E> mergeFunction) {
        return distinctBy(Fnn.identity(), mergeFunction);
    }

    /**
     * Distinct and filter by occurrences.
     *
     * @param occurrencesFilter
     * @return
     * @see #groupBy(Function, Collector)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public ExceptionalStream<T, E> distinct(final Throwables.Predicate<? super Long, ? extends E> occurrencesFilter) {
        return distinctBy(Fnn.identity(), occurrencesFilter);
    }

    /**
     * Distinct by the value mapped from <code>keyMapper</code> .
     *
     * @param keyMapper don't change value of the input parameter.
     * @return
     */
    @IntermediateOp
    public <K> ExceptionalStream<T, E> distinctBy(final Throwables.Function<? super T, K, ? extends E> keyMapper) {
        assertNotClosed();

        final Set<Object> set = N.newHashSet();

        return filter(value -> set.add(hashKey(keyMapper.apply(value))));
    }

    /**
     * Distinct and filter by occurrences.
     *
     * @param keyMapper
     * @param mergeFunction
     * @return
     * @see #groupBy(Function, Function, BinaryOperator)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K> ExceptionalStream<T, E> distinctBy(final Throwables.Function<? super T, K, ? extends E> keyMapper,
            final Throwables.BinaryOperator<T, ? extends E> mergeFunction) {
        assertNotClosed();

        final Supplier<? extends Map<K, T>> supplier = Suppliers.<K, T> ofMap();

        return groupBy(keyMapper, Fnn.<T, E> identity(), mergeFunction, supplier).map(Fnn.<K, T, E> value());
    }

    /**
     * Distinct and filter by occurrences.
     *
     * @param keyMapper
     * @param occurrencesFilter
     * @return
     * @see #groupBy(Function, Collector)
     */
    @IntermediateOp
    @TerminalOpTriggered
    @SuppressWarnings("rawtypes")
    public <K> ExceptionalStream<T, E> distinctBy(final Throwables.Function<? super T, K, ? extends E> keyMapper,
            final Throwables.Predicate<? super Long, ? extends E> occurrencesFilter) {
        assertNotClosed();

        final Supplier<? extends Map<Keyed<K, T>, Long>> supplier = Suppliers.<Keyed<K, T>, Long> ofLinkedHashMap();

        final Throwables.Function<T, Keyed<K, T>, E> keyedMapper = t -> Keyed.of(keyMapper.apply(t), t);

        final Throwables.Predicate<Map.Entry<Keyed<K, T>, Long>, ? extends E> predicate = e -> occurrencesFilter.test(e.getValue());

        return newStream(groupBy(keyedMapper, Collectors.counting(), supplier).filter(predicate)
                .map((Throwables.Function<Map.Entry<Keyed<K, T>, Long>, T, E>) (Throwables.Function) KK)
                .iteratorEx(), sorted, cmp, closeHandlers);
    }

    /**
     *
     * @param <U>
     * @param mapper
     * @return
     */
    @IntermediateOp
    public <U> ExceptionalStream<U, E> map(final Throwables.Function<? super T, ? extends U, ? extends E> mapper) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<U, E>() {
            @Override
            public boolean hasNext() throws E {
                return elements.hasNext();
            }

            @Override
            public U next() throws E {
                return mapper.apply(elements.next());
            }
        }, closeHandlers);
    }

    @IntermediateOp
    public ExceptionalStream<T, E> mapFirst(final Throwables.Function<? super T, ? extends T, ? extends E> mapperForFirst) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<T, E>() {
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

    @IntermediateOp
    public <R> ExceptionalStream<R, E> mapFirstOrElse(final Throwables.Function<? super T, ? extends R, E> mapperForFirst,
            final Throwables.Function<? super T, ? extends R, E> mapperForElse) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<R, E>() {
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

    @IntermediateOp
    public ExceptionalStream<T, E> mapLast(final Throwables.Function<? super T, ? extends T, ? extends E> mapperForLast) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<T, E>() {
            private T next = null;

            @Override
            public boolean hasNext() throws E {
                return elements.hasNext();
            }

            @Override
            public T next() throws E {
                next = elements.next();

                if (elements.hasNext()) {
                    return next;
                } else {
                    return mapperForLast.apply(next);
                }
            }
        }, closeHandlers);
    }

    @IntermediateOp
    public <R> ExceptionalStream<R, E> mapLastOrElse(final Throwables.Function<? super T, ? extends R, E> mapperForLast,
            final Throwables.Function<? super T, ? extends R, E> mapperForElse) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<R, E>() {
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
    public <R> ExceptionalStream<R, E> flatMap(
            final Throwables.Function<? super T, ? extends ExceptionalStream<? extends R, ? extends E>, ? extends E> mapper) {
        assertNotClosed();

        final ExceptionalIterator<R, E> iter = new ExceptionalIterator<>() {
            private ExceptionalStream<? extends R, ? extends E> s = null;
            private ExceptionalIterator<? extends R, ? extends E> cur = null;

            @Override
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

            @Override
            public R next() throws E {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException();
                }

                return cur.next();
            }

            @Override
            public void close() throws E {
                if (s != null) {
                    s.close();
                }
            }
        };

        final Deque<Throwables.Runnable<? extends E>> newCloseHandlers = new ArrayDeque<>(N.size(closeHandlers) + 1);

        newCloseHandlers.add(newCloseHandler(iter));

        if (N.notNullOrEmpty(closeHandlers)) {
            newCloseHandlers.addAll(closeHandlers);
        }

        return newStream(iter, newCloseHandlers);
    }

    /**
     *
     * @param <R>
     * @param mapper
     * @return
     */
    @IntermediateOp
    public <R> ExceptionalStream<R, E> flatmap(final Throwables.Function<? super T, ? extends Collection<? extends R>, ? extends E> mapper) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<R, E>() {
            private Collection<? extends R> c = null;
            private Iterator<? extends R> cur = null;

            @Override
            public boolean hasNext() throws E {
                while ((cur == null || !cur.hasNext()) && elements.hasNext()) {
                    c = mapper.apply(elements.next());
                    cur = N.isNullOrEmpty(c) ? null : c.iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public R next() throws E {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException();
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
    public <R> ExceptionalStream<R, E> flattMap(final Throwables.Function<? super T, R[], ? extends E> mapper) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<R, E>() {
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
                    throw new NoSuchElementException();
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
    public <R> ExceptionalStream<R, E> flatMapp(final Throwables.Function<? super T, ? extends Stream<? extends R>, ? extends E> mapper) {
        assertNotClosed();

        final ExceptionalIterator<R, E> iter = new ExceptionalIterator<>() {
            private Stream<? extends R> s = null;
            private Iterator<? extends R> cur = null;

            @Override
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

            @Override
            public R next() throws E {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException();
                }

                return cur.next();
            }

            @Override
            public void close() throws E {
                if (s != null) {
                    s.close();
                }
            }
        };

        final Deque<Throwables.Runnable<? extends E>> newCloseHandlers = new ArrayDeque<>(N.size(closeHandlers) + 1);

        newCloseHandlers.add(newCloseHandler(iter));

        if (N.notNullOrEmpty(closeHandlers)) {
            newCloseHandlers.addAll(closeHandlers);
        }

        return newStream(iter, newCloseHandlers);
    }

    //    /**
    //     *
    //     * @param <R>
    //     * @param mapper
    //     * @return
    //     */
    //    @Beta
    //    @IntermediateOp
    //    public <R> ExceptionalStream<R, E> flatMapByStream(final Throwables.Function<? super T, ? extends Stream<? extends R>, ? extends E> mapper) {
    //        assertNotClosed();
    //        final ExceptionalIterator<R, E> iter = new ExceptionalIterator<R, E>() {
    //            private Stream<? extends R> s = null;
    //            private Iterator<? extends R> cur = null;
    //
    //            @Override
    //            public boolean hasNext() throws E {
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
    //            @Override
    //            public R next() throws E {
    //                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
    //                    throw new NoSuchElementException();
    //                }
    //
    //                return cur.next();
    //            }
    //
    //            @Override
    //            public void close() throws E {
    //                if (s != null) {
    //                    s.close();
    //                }
    //            }
    //        };
    //
    //        final Deque<Throwables.Runnable<? extends E>> newCloseHandlers = new ArrayDeque<>(N.size(closeHandlers) + 1);
    //
    //        newCloseHandlers.add(newCloseHandler(iter));
    //
    //        if (N.notNullOrEmpty(closeHandlers)) {
    //            newCloseHandlers.addAll(closeHandlers);
    //        }
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
    //    public <R> ExceptionalStream<R, E> flatMapByStreamJdk(
    //            final Throwables.Function<? super T, ? extends java.util.stream.Stream<? extends R>, ? extends E> mapper) {
    //        assertNotClosed();
    //        final ExceptionalIterator<R, E> iter = new ExceptionalIterator<R, E>() {
    //            private java.util.stream.Stream<? extends R> s = null;
    //            private Iterator<? extends R> cur = null;
    //
    //            @Override
    //            public boolean hasNext() throws E {
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
    //            @Override
    //            public R next() throws E {
    //                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
    //                    throw new NoSuchElementException();
    //                }
    //
    //                return cur.next();
    //            }
    //
    //            @Override
    //            public void close() throws E {
    //                if (s != null) {
    //                    s.close();
    //                }
    //            }
    //        };
    //
    //        final Deque<Throwables.Runnable<? extends E>> newCloseHandlers = new ArrayDeque<>(N.size(closeHandlers) + 1);
    //
    //        newCloseHandlers.add(newCloseHandler(iter));
    //
    //        if (N.notNullOrEmpty(closeHandlers)) {
    //            newCloseHandlers.addAll(closeHandlers);
    //        }
    //
    //        return newStream(iter, newCloseHandlers);
    //    }

    /**
     * @implNote same as ====>
     * <pre>
     * skipNull().flatmap(mapper)
     * </pre>
     *
     * @param <R>
     * @param mapper
     * @return
     */
    @Beta
    @IntermediateOp
    public <R> ExceptionalStream<R, E> flatMapIfNotNull(final Throwables.Function<? super T, ? extends Collection<? extends R>, ? extends E> mapper) {
        return skipNull().flatmap(mapper);
    }

    /**
     * @implNote same as ====>
     * <pre>
     * skipNull().flatmap(mapper).skipNull().flatmap(mapper2)
     * </pre>
     *
     * @param <U>
     * @param <R>
     * @param mapper
     * @param mapper2
     * @return
     */
    @Beta
    @IntermediateOp
    public <U, R> ExceptionalStream<R, E> flatMapIfNotNull(final Throwables.Function<? super T, ? extends Collection<? extends U>, ? extends E> mapper,
            final Throwables.Function<? super U, ? extends Collection<? extends R>, ? extends E> mapper2) {
        return skipNull().flatmap(mapper).skipNull().flatmap(mapper2);
    }

    /**
     *
     * @param <R>
     * @param mapper
     * @return
     */
    @Beta
    @IntermediateOp
    public ExceptionalStream<Boolean, E> flatMapToBoolean(final Throwables.Function<? super T, boolean[], ? extends E> mapper) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<Boolean, E>() {
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
                    throw new NoSuchElementException();
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
    public ExceptionalStream<Character, E> flatMapToChar(final Throwables.Function<? super T, char[], ? extends E> mapper) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<Character, E>() {
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
                    throw new NoSuchElementException();
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
    public ExceptionalStream<Byte, E> flatMapToByte(final Throwables.Function<? super T, byte[], ? extends E> mapper) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<Byte, E>() {
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
                    throw new NoSuchElementException();
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
    public ExceptionalStream<Short, E> flatMapToShort(final Throwables.Function<? super T, short[], ? extends E> mapper) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<Short, E>() {
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
                    throw new NoSuchElementException();
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
    public ExceptionalStream<Integer, E> flatMapToInteger(final Throwables.Function<? super T, int[], ? extends E> mapper) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<Integer, E>() {
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
                    throw new NoSuchElementException();
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
    public ExceptionalStream<Long, E> flatMapToLong(final Throwables.Function<? super T, long[], ? extends E> mapper) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<Long, E>() {
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
                    throw new NoSuchElementException();
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
    public ExceptionalStream<Float, E> flatMapToFloat(final Throwables.Function<? super T, float[], ? extends E> mapper) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<Float, E>() {
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
                    throw new NoSuchElementException();
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
    public ExceptionalStream<Double, E> flatMapToDouble(final Throwables.Function<? super T, double[], ? extends E> mapper) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<Double, E>() {
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
                    throw new NoSuchElementException();
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
    //    public ExceptionalStream<Integer, E> flatmapToInt(final Throwables.Function<? super T, ? extends int[], ? extends E> mapper) {
    //        final Throwables.Function<T, ExceptionalStream<Integer, E>, E> mapper2 = new Throwables.Function<T, ExceptionalStream<Integer, E>, E>() {
    //            @Override
    //            public ExceptionalStream<Integer, E> apply(T t) throws E {
    //                return ExceptionalStream.of(mapper.apply(t));
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
    //    public ExceptionalStream<Long, E> flatmapToLong(final Throwables.Function<? super T, ? extends long[], ? extends E> mapper) {
    //        final Throwables.Function<T, ExceptionalStream<Long, E>, E> mapper2 = new Throwables.Function<T, ExceptionalStream<Long, E>, E>() {
    //            @Override
    //            public ExceptionalStream<Long, E> apply(T t) throws E {
    //                return ExceptionalStream.of(mapper.apply(t));
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
    //    public ExceptionalStream<Double, E> flatmapToDouble(final Throwables.Function<? super T, ? extends double[], ? extends E> mapper) {
    //        final Throwables.Function<T, ExceptionalStream<Double, E>, E> mapper2 = new Throwables.Function<T, ExceptionalStream<Double, E>, E>() {
    //            @Override
    //            public ExceptionalStream<Double, E> apply(T t) throws E {
    //                return ExceptionalStream.of(mapper.apply(t));
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
    public <R> ExceptionalStream<R, E> mapPartial(final Throwables.Function<? super T, Optional<? extends R>, E> mapper) {
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
    public ExceptionalStream<Integer, E> mapPartialToInt(final Throwables.Function<? super T, OptionalInt, E> mapper) {
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
    public ExceptionalStream<Long, E> mapPartialToLong(final Throwables.Function<? super T, OptionalLong, E> mapper) {
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
    public ExceptionalStream<Double, E> mapPartialToDouble(final Throwables.Function<? super T, OptionalDouble, E> mapper) {
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
    public <R> ExceptionalStream<R, E> mapPartialJdk(final Throwables.Function<? super T, java.util.Optional<? extends R>, E> mapper) {
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
    public ExceptionalStream<Integer, E> mapPartialToIntJdk(final Throwables.Function<? super T, java.util.OptionalInt, E> mapper) {
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
    public ExceptionalStream<Long, E> mapPartialToLongJdk(final Throwables.Function<? super T, java.util.OptionalLong, E> mapper) {
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
    public ExceptionalStream<Double, E> mapPartialToDoubleJdk(final Throwables.Function<? super T, java.util.OptionalDouble, E> mapper) {
        return map(mapper).filter((Throwables.Predicate) IS_PRESENT_DOUBLE_JDK).map(GET_AS_DOUBLE_JDK);
    }

    public <R> ExceptionalStream<R, E> mapMulti(final Throwables.BiConsumer<? super T, ? super Consumer<R>, ? extends E> mapper) {
        final Deque<R> queue = new ArrayDeque<>();

        final Consumer<R> consumer = queue::offer;

        final ExceptionalIterator<T, E> iter = iteratorEx();

        return newStream(new ExceptionalIterator<R, E>() {
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
                    throw new NoSuchElementException();
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
    public <R> ExceptionalStream<R, E> slidingMap(Throwables.BiFunction<? super T, ? super T, R, ? extends E> mapper) {
        return slidingMap(mapper, 1);
    }

    /**
     *
     * @param <R>
     * @param mapper
     * @param increment
     * @return
     */
    @IntermediateOp
    public <R> ExceptionalStream<R, E> slidingMap(Throwables.BiFunction<? super T, ? super T, R, ? extends E> mapper, int increment) {
        return slidingMap(mapper, increment, false);
    }

    /**
     *
     * @param <R>
     * @param mapper
     * @param increment
     * @param ignoreNotPaired
     * @return
     */
    @IntermediateOp
    public <R> ExceptionalStream<R, E> slidingMap(final Throwables.BiFunction<? super T, ? super T, R, ? extends E> mapper, final int increment,
            final boolean ignoreNotPaired) {
        assertNotClosed();

        checkArgPositive(increment, "increment");

        final int windowSize = 2;

        return newStream(new ExceptionalIterator<R, E>() {
            @SuppressWarnings("unchecked")
            private final T none = (T) NONE;
            private T prev = none;
            private T _1 = none;

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
                    throw new NoSuchElementException();
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
    public <R> ExceptionalStream<R, E> slidingMap(Throwables.TriFunction<? super T, ? super T, ? super T, R, ? extends E> mapper) {
        return slidingMap(mapper, 1);
    }

    /**
     *
     * @param <R>
     * @param mapper
     * @param increment
     * @return
     */
    @IntermediateOp
    public <R> ExceptionalStream<R, E> slidingMap(Throwables.TriFunction<? super T, ? super T, ? super T, R, ? extends E> mapper, int increment) {
        return slidingMap(mapper, increment, false);
    }

    /**
     *
     * @param <R>
     * @param mapper
     * @param increment
     * @param ignoreNotPaired
     * @return
     */
    @IntermediateOp
    public <R> ExceptionalStream<R, E> slidingMap(final Throwables.TriFunction<? super T, ? super T, ? super T, R, ? extends E> mapper, final int increment,
            final boolean ignoreNotPaired) {
        assertNotClosed();

        checkArgPositive(increment, "increment");

        final int windowSize = 3;

        return newStream(new ExceptionalIterator<R, E>() {
            @SuppressWarnings("unchecked")
            private final T none = (T) NONE;
            private T prev = none;
            private T prev2 = none;
            private T _1 = none;
            private T _2 = none;

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
                    throw new NoSuchElementException();
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
    public <K> ExceptionalStream<Map.Entry<K, List<T>>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper) {
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
    public <K> ExceptionalStream<Map.Entry<K, List<T>>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
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
    public <K, V> ExceptionalStream<Map.Entry<K, List<V>>, E> groupBy(Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
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
    public <K, V> ExceptionalStream<Map.Entry<K, List<V>>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.Function<? super T, ? extends V, ? extends E> valueMapper, final Supplier<? extends Map<K, List<V>>> mapFactory) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<Map.Entry<K, List<V>>, E>() {
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
                    iter = ExceptionalStream.this.groupTo(keyMapper, valueMapper, mapFactory).entrySet().iterator();
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
    public <K, V> ExceptionalStream<Map.Entry<K, V>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
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
    public <K, V> ExceptionalStream<Map.Entry<K, V>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.Function<? super T, ? extends V, ? extends E> valueMapper, final Throwables.BinaryOperator<V, ? extends E> mergeFunction,
            final Supplier<? extends Map<K, V>> mapFactory) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<Map.Entry<K, V>, E>() {
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
                    iter = ExceptionalStream.this.toMap(keyMapper, valueMapper, mergeFunction, mapFactory).entrySet().iterator();
                }
            }
        }, closeHandlers);
    }

    /**
     *
     * @param <K> the key type
     * @param <A>
     * @param <D>
     * @param keyMapper
     * @param downstream
     * @return
     * @throws E the e
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K, A, D> ExceptionalStream<Map.Entry<K, D>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Collector<? super T, A, D> downstream) {
        return groupBy(keyMapper, downstream, Suppliers.<K, D> ofMap());
    }

    /**
     *
     * @param <K> the key type
     * @param <A>
     * @param <D>
     * @param keyMapper
     * @param downstream
     * @param mapFactory
     * @return
     * @throws E the e
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K, A, D> ExceptionalStream<Map.Entry<K, D>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Collector<? super T, A, D> downstream, final Supplier<? extends Map<K, D>> mapFactory) {
        return groupBy(keyMapper, Fnn.<T, E> identity(), downstream, mapFactory);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <A>
     * @param <D>
     * @param keyMapper
     * @param valueMapper
     * @param downstream
     * @return
     * @throws E the e
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K, V, A, D> ExceptionalStream<Map.Entry<K, D>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.Function<? super T, ? extends V, ? extends E> valueMapper, final Collector<? super V, A, D> downstream) {
        return groupBy(keyMapper, valueMapper, downstream, Suppliers.<K, D> ofMap());
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <A>
     * @param <D>
     * @param keyMapper
     * @param valueMapper
     * @param downstream
     * @param mapFactory
     * @return
     * @throws E the e
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K, V, A, D> ExceptionalStream<Map.Entry<K, D>, E> groupBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.Function<? super T, ? extends V, ? extends E> valueMapper, final Collector<? super V, A, D> downstream,
            final Supplier<? extends Map<K, D>> mapFactory) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<Map.Entry<K, D>, E>() {
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
                    iter = ExceptionalStream.this.groupTo(keyMapper, valueMapper, downstream, mapFactory).entrySet().iterator();
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
    public ExceptionalStream<Map.Entry<Boolean, List<T>>, E> partitionBy(final Throwables.Predicate<? super T, E> predicate) {
        assertNotClosed();

        return partitionBy(predicate, Collectors.<T> toList());
    }

    /**
     *
     * @param predicate
     * @param downstream
     * @return
     * @see Collectors#partitioningBy(Predicate, Collector)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <A, D> ExceptionalStream<Map.Entry<Boolean, D>, E> partitionBy(final Throwables.Predicate<? super T, E> predicate,
            final Collector<? super T, A, D> downstream) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<Entry<Boolean, D>, E>() {
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
                    iter = ExceptionalStream.this.partitionTo(predicate, downstream).entrySet().iterator();
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
    public <K> ExceptionalStream<Map.Entry<K, Integer>, E> countBy(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper) {
        return groupBy(keyMapper, Collectors.countingInt());
    }

    /**
     *
     * @param collapsible test the current element with its previous element. The first parameter is the previous element of current element, the second parameter is the current element.
     * @return
     */
    @IntermediateOp
    public ExceptionalStream<Stream<T>, E> collapse(final Throwables.BiPredicate<? super T, ? super T, ? extends E> collapsible) {
        assertNotClosed();

        final ExceptionalIterator<T, E> iter = elements;

        return newStream(new ExceptionalIterator<Stream<T>, E>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() throws E {
                return hasNext || iter.hasNext();
            }

            @Override
            public Stream<T> next() throws E {
                final List<T> c = new ArrayList<>();
                c.add(hasNext ? next : (next = iter.next()));

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.next()))) {
                        c.add(next);
                    } else {
                        break;
                    }
                }

                return Stream.of(c);
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
    public <C extends Collection<T>> ExceptionalStream<C, E> collapse(final Throwables.BiPredicate<? super T, ? super T, ? extends E> collapsible,
            final Supplier<? extends C> supplier) {
        assertNotClosed();

        final ExceptionalIterator<T, E> iter = elements;

        return newStream(new ExceptionalIterator<C, E>() {
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
     * ExceptionalStream.of(new Integer[0]).collapse((p, c) -> p < c, (r, c) -> r + c) => []
     * ExceptionalStream.of(1).collapse((p, c) -> p < c, (r, c) -> r + c) => [1]
     * ExceptionalStream.of(1, 2).collapse((p, c) -> p < c, (r, c) -> r + c) => [3]
     * ExceptionalStream.of(1, 2, 3).collapse((p, c) -> p < c, (r, c) -> r + c) => [6]
     * ExceptionalStream.of(1, 2, 3, 3, 2, 1).collapse((p, c) -> p < c, (r, c) -> r + c) => [6, 3, 2, 1]
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
    public ExceptionalStream<T, E> collapse(final Throwables.BiPredicate<? super T, ? super T, ? extends E> collapsible,
            final Throwables.BiFunction<? super T, ? super T, T, ? extends E> mergeFunction) {
        assertNotClosed();

        final ExceptionalIterator<T, E> iter = elements;

        return newStream(new ExceptionalIterator<T, E>() {
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
    public <U> ExceptionalStream<U, E> collapse(final Throwables.BiPredicate<? super T, ? super T, ? extends E> collapsible, final U init,
            final Throwables.BiFunction<U, ? super T, U, ? extends E> op) {
        assertNotClosed();

        final ExceptionalIterator<T, E> iter = elements;

        return newStream(new ExceptionalIterator<U, E>() {
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
    public <R> ExceptionalStream<R, E> collapse(final Throwables.BiPredicate<? super T, ? super T, ? extends E> collapsible,
            final Throwables.Supplier<R, E> supplier, final Throwables.BiConsumer<? super R, ? super T, ? extends E> accumulator) {
        assertNotClosed();

        final ExceptionalIterator<T, E> iter = elements;

        return newStream(new ExceptionalIterator<R, E>() {
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
     * @param <A>
     * @param collapsible test the current element with its previous element. The first parameter is the previous element of current element, the second parameter is the current element.
     * @param collector
     * @return
     */
    @IntermediateOp
    public <R, A> ExceptionalStream<R, E> collapse(final Throwables.BiPredicate<? super T, ? super T, ? extends E> collapsible,
            final Collector<? super T, A, R> collector) {
        assertNotClosed();

        final java.util.function.Supplier<A> supplier = collector.supplier();
        final java.util.function.BiConsumer<A, ? super T> accumulator = collector.accumulator();
        final java.util.function.Function<A, R> finisher = collector.finisher();

        final ExceptionalIterator<T, E> iter = elements;

        return newStream(new ExceptionalIterator<R, E>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() throws E {
                return hasNext || iter.hasNext();
            }

            @Override
            public R next() throws E {
                final A container = supplier.get();
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
    public ExceptionalStream<Stream<T>, E> collapse(final Throwables.TriPredicate<? super T, ? super T, ? super T, ? extends E> collapsible) {
        assertNotClosed();

        final ExceptionalIterator<T, E> iter = elements;

        return newStream(new ExceptionalIterator<Stream<T>, E>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() throws E {
                return hasNext || iter.hasNext();
            }

            @Override
            public Stream<T> next() throws E {
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

                return Stream.of(c);
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
    public <C extends Collection<T>> ExceptionalStream<C, E> collapse(final Throwables.TriPredicate<? super T, ? super T, ? super T, ? extends E> collapsible,
            final Supplier<? extends C> supplier) {
        assertNotClosed();

        final ExceptionalIterator<T, E> iter = elements;

        return newStream(new ExceptionalIterator<C, E>() {
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
    public ExceptionalStream<T, E> collapse(final Throwables.TriPredicate<? super T, ? super T, ? super T, ? extends E> collapsible,
            final Throwables.BiFunction<? super T, ? super T, T, ? extends E> mergeFunction) {
        assertNotClosed();

        final ExceptionalIterator<T, E> iter = elements;

        return newStream(new ExceptionalIterator<T, E>() {
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
    public <U> ExceptionalStream<U, E> collapse(final Throwables.TriPredicate<? super T, ? super T, ? super T, ? extends E> collapsible, final U init,
            final Throwables.BiFunction<U, ? super T, U, ? extends E> op) {
        assertNotClosed();

        final ExceptionalIterator<T, E> iter = elements;

        return newStream(new ExceptionalIterator<U, E>() {
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
    public <R> ExceptionalStream<R, E> collapse(final Throwables.TriPredicate<? super T, ? super T, ? super T, ? extends E> collapsible,
            final Throwables.Supplier<R, E> supplier, final Throwables.BiConsumer<? super R, ? super T, ? extends E> accumulator) {
        assertNotClosed();

        final ExceptionalIterator<T, E> iter = elements;

        return newStream(new ExceptionalIterator<R, E>() {
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
     * ExceptionalStream.of(new Integer[0]).collapse((f, p, c) -> f < c, Collectors.summingInt(Fn.unboxI())) => []
     * ExceptionalStream.of(1).collapse((f, p, c) -> f < c, Collectors.summingInt(Fn.unboxI())) => [1]
     * ExceptionalStream.of(1, 2).collapse((f, p, c) -> f < c, Collectors.summingInt(Fn.unboxI())) => [3]
     * ExceptionalStream.of(1, 2, 3).collapse((f, p, c) -> f < c, Collectors.summingInt(Fn.unboxI())) => [6]
     * ExceptionalStream.of(1, 2, 3, 3, 2, 1).collapse((f, p, c) -> f < c, Collectors.summingInt(Fn.unboxI())) => [11, 1]
     * </code>
     * </pre>
     *
     *
     * @param <R>
     * @param <A>
     * @param collapsible test the current element with the first element and previous element in the series. The first parameter is the first element of this series, the second parameter is the previous element and the third parameter is the current element.
     * @param collector
     * @return
     */
    @IntermediateOp
    public <R, A> ExceptionalStream<R, E> collapse(final Throwables.TriPredicate<? super T, ? super T, ? super T, ? extends E> collapsible,
            final Collector<? super T, A, R> collector) {
        assertNotClosed();

        final java.util.function.Supplier<A> supplier = collector.supplier();
        final java.util.function.BiConsumer<A, ? super T> accumulator = collector.accumulator();
        final java.util.function.Function<A, R> finisher = collector.finisher();

        final ExceptionalIterator<T, E> iter = elements;

        return newStream(new ExceptionalIterator<R, E>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() throws E {
                return hasNext || iter.hasNext();
            }

            @Override
            public R next() throws E {
                final T first = hasNext ? next : (next = iter.next());
                final A container = supplier.get();
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
    public ExceptionalStream<T, E> scan(final Throwables.BiFunction<? super T, ? super T, T, ? extends E> accumulator) {
        assertNotClosed();

        final ExceptionalIterator<T, E> iter = elements;

        return newStream(new ExceptionalIterator<T, E>() {
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
    public <U> ExceptionalStream<U, E> scan(final U init, final Throwables.BiFunction<U, ? super T, U, ? extends E> accumulator) {
        assertNotClosed();

        final ExceptionalIterator<T, E> iter = elements;

        return newStream(new ExceptionalIterator<U, E>() {
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
    public <U> ExceptionalStream<U, E> scan(final U init, final Throwables.BiFunction<U, ? super T, U, ? extends E> accumulator, final boolean initIncluded) {
        assertNotClosed();

        if (!initIncluded) {
            return scan(init, accumulator);
        }

        final ExceptionalIterator<T, E> iter = elements;

        return newStream(new ExceptionalIterator<U, E>() {
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
    public ExceptionalStream<T, E> intersection(final Collection<?> c) {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.from(c);

        return filter(value -> multiset.getAndRemove(value) > 0);
    }

    /**
     *
     * @param mapper
     * @param c
     * @return
     * @see N#intersection(Collection, Collection)
     */
    public <U> ExceptionalStream<T, E> intersection(final Throwables.Function<? super T, ? extends U, E> mapper, final Collection<U> c) {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.from(c);

        return filter(value -> multiset.getAndRemove(mapper.apply(value)) > 0);
    }

    /**
     *
     * @param c
     * @return
     * @see N#difference(Collection, Collection)
     */
    public ExceptionalStream<T, E> difference(final Collection<?> c) {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.from(c);

        return filter(value -> multiset.getAndRemove(value) < 1);
    }

    /**
     *
     * @param mapper
     * @param c
     * @return
     * @see N#difference(Collection, Collection)
     */
    public <U> ExceptionalStream<T, E> difference(final Function<? super T, ? extends U> mapper, final Collection<U> c) {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.from(c);

        return filter(value -> multiset.getAndRemove(mapper.apply(value)) < 1);
    }

    /**
     *
     * @param c
     * @return
     * @see N#symmetricDifference(Collection, Collection)
     */
    public ExceptionalStream<T, E> symmetricDifference(final Collection<T> c) {
        assertNotClosed();

        final Multiset<?> multiset = Multiset.from(c);

        return filter(value -> multiset.getAndRemove(value) < 1).append(ExceptionalStream.<T, E> of(c).filter(value -> multiset.getAndRemove(value) > 0));
    }

    /**
     *
     * @param defaultValue
     * @return
     * @see #appendIfEmpty(Object...)
     */
    @IntermediateOp
    public final ExceptionalStream<T, E> defaultIfEmpty(final T defaultValue) {
        return appendIfEmpty(defaultValue);
    }

    //    /**
    //     *
    //     * @param defaultValues
    //     * @return
    //     * @see #appendIfEmpty(Object...)
    //     */
    //    @IntermediateOp
    //    public final ExceptionalStream<T, E> defaultIfEmpty(final Collection<? extends T> defaultValues) {
    //        return appendIfEmpty(defaultValues);
    //    }

    /**
     *
     * @param supplier
     * @return
     * @see #appendIfEmpty(Throwables.Supplier)
     */
    @IntermediateOp
    public final ExceptionalStream<T, E> defaultIfEmpty(final Throwables.Supplier<? extends ExceptionalStream<T, E>, ? extends E> supplier) {
        return appendIfEmpty(supplier);
    }

    @IntermediateOp
    @SafeVarargs
    public final ExceptionalStream<T, E> prepend(final T... a) {
        return prepend(ExceptionalStream.of(a));
    }

    @IntermediateOp
    public ExceptionalStream<T, E> prepend(final Collection<? extends T> c) {
        return prepend(ExceptionalStream.of(c));
    }

    /**
     *
     * @param s
     * @return
     */
    @IntermediateOp
    public ExceptionalStream<T, E> prepend(final ExceptionalStream<T, E> s) {
        assertNotClosed();

        return concat(s, this);
    }

    @IntermediateOp
    @SafeVarargs
    public final ExceptionalStream<T, E> append(final T... a) {
        return append(ExceptionalStream.of(a));
    }

    @IntermediateOp
    public ExceptionalStream<T, E> append(final Collection<? extends T> c) {
        return append(ExceptionalStream.of(c));
    }

    /**
     *
     * @param s
     * @return
     */
    @IntermediateOp
    public ExceptionalStream<T, E> append(final ExceptionalStream<T, E> s) {
        assertNotClosed();

        return concat(this, s);
    }

    @IntermediateOp
    @SafeVarargs
    public final ExceptionalStream<T, E> appendIfEmpty(final T... a) {
        return appendIfEmpty(Arrays.asList(a));
    }

    @IntermediateOp
    public ExceptionalStream<T, E> appendIfEmpty(final Collection<? extends T> c) {
        assertNotClosed();

        if (N.isNullOrEmpty(c)) {
            return newStream(elements, closeHandlers);
        }

        return newStream(new ExceptionalIterator<T, E>() {
            private ExceptionalIterator<T, E> iter;

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
                        iter = ExceptionalIterator.wrap(c.iterator());
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
     * @throws E the e
     */
    @IntermediateOp
    public ExceptionalStream<T, E> appendIfEmpty(final Throwables.Supplier<? extends ExceptionalStream<T, E>, ? extends E> supplier) {
        assertNotClosed();

        final Holder<ExceptionalStream<T, E>> holder = new Holder<>();

        return newStream(new ExceptionalIterator<T, E>() {
            private ExceptionalIterator<T, E> iter;

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
                        final ExceptionalStream<T, E> s = supplier.get();
                        holder.setValue(s);
                        iter = iterate(s);
                    }
                }
            }
        }, closeHandlers).onClose(() -> close(holder));
    }

    //    @SuppressWarnings("rawtypes")
    //    private static final Throwables.Predicate NOT_NULL_MASK = new Throwables.Predicate<Object, RuntimeException>() {
    //        @Override
    //        public boolean test(final Object t) {
    //            return t != NONE;
    //        }
    //    };
    //
    //    @Beta
    //    @IntermediateOp
    //    public ExceptionalStream<T, E> appendOnError(final T fallbackValue) {
    //        assertNotClosed();
    //
    //        return newStream(new ExceptionalIterator<T, E>() {
    //            private boolean fallbackValueAvaiable = true;
    //
    //            @Override
    //            public boolean hasNext() throws E {
    //                try {
    //                    return fallbackValueAvaiable && elements.hasNext();
    //                } catch (Exception e) {
    //                    return fallbackValueAvaiable;
    //                }
    //            }
    //
    //            @Override
    //            public T next() throws E {
    //                if (hasNext() == false) {
    //                    throw new NoSuchElementException();
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
    //    public <XE extends Exception> ExceptionalStream<T, E> appendOnError(final Class<XE> type, final T fallbackValue) {
    //        assertNotClosed();
    //        this.checkArgNotNull(type, "type");
    //
    //        return newStream(new ExceptionalIterator<T, E>() {
    //            private boolean fallbackValueAvaiable = true;
    //
    //            @Override
    //            public boolean hasNext() throws E {
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
    //            @Override
    //            public T next() throws E {
    //                if (hasNext() == false) {
    //                    throw new NoSuchElementException();
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
    //    public ExceptionalStream<T, E> appendOnError(final Predicate<? super Exception> predicate, final T fallbackValue) {
    //        assertNotClosed();
    //        this.checkArgNotNull(predicate, "predicate");
    //
    //        return newStream(new ExceptionalIterator<T, E>() {
    //            private boolean fallbackValueAvaiable = true;
    //
    //            @Override
    //            public boolean hasNext() throws E {
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
    //            @Override
    //            public T next() throws E {
    //                if (hasNext() == false) {
    //                    throw new NoSuchElementException();
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
    //    public ExceptionalStream<T, E> appendOnError(final Supplier<ExceptionalStream<T, E>> fallbackStreamSupplier) {
    //        assertNotClosed();
    //        this.checkArgNotNull(fallbackStreamSupplier, "fallbackStreamSupplier");
    //
    //        final ExceptionalIterator<T, E> iter = new ExceptionalIterator<T, E>() {
    //            private boolean fallbackValueAvaiable = true;
    //            private ExceptionalIterator<T, E> iter = ExceptionalStream.this.elements;
    //            private ExceptionalStream<T, E> s = null;
    //
    //            @Override
    //            public boolean hasNext() throws E {
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
    //            @Override
    //            public T next() throws E {
    //                if (hasNext() == false) {
    //                    throw new NoSuchElementException();
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
    //            @Override
    //            public void close() {
    //                if ((s != null && N.notNullOrEmpty(s.closeHandlers)) || N.notNullOrEmpty(ExceptionalStream.this.closeHandlers)) {
    //                    try {
    //                        if (s != null) {
    //                            s.close();
    //                        }
    //                    } finally {
    //                        ExceptionalStream.this.close();
    //                    }
    //                }
    //            }
    //
    //            private void useFallbackStream(final Supplier<ExceptionalStream<T, E>> fallbackStream) {
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
    //    public <XE extends Exception> ExceptionalStream<T, E> appendOnError(final Class<XE> type, final Supplier<ExceptionalStream<T, E>> fallbackStreamSupplier) {
    //        assertNotClosed();
    //        this.checkArgNotNull(type, "type");
    //        this.checkArgNotNull(fallbackStreamSupplier, "fallbackStreamSupplier");
    //
    //        final ExceptionalIterator<T, E> iter = new ExceptionalIterator<T, E>() {
    //            private boolean fallbackValueAvaiable = true;
    //            private ExceptionalIterator<T, E> iter = ExceptionalStream.this.elements;
    //            private ExceptionalStream<T, E> s = null;
    //
    //            @Override
    //            public boolean hasNext() throws E {
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
    //            @Override
    //            public T next() throws E {
    //                if (hasNext() == false) {
    //                    throw new NoSuchElementException();
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
    //            @Override
    //            public void close() {
    //                if ((s != null && N.notNullOrEmpty(s.closeHandlers)) || N.notNullOrEmpty(ExceptionalStream.this.closeHandlers)) {
    //                    try {
    //                        if (s != null) {
    //                            s.close();
    //                        }
    //                    } finally {
    //                        ExceptionalStream.this.close();
    //                    }
    //                }
    //            }
    //
    //            private void useFallbackStream(final Supplier<ExceptionalStream<T, E>> fallbackStream) {
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
    //    public ExceptionalStream<T, E> appendOnError(final Predicate<? super Exception> predicate, final Supplier<ExceptionalStream<T, E>> fallbackStreamSupplier) {
    //        assertNotClosed();
    //        this.checkArgNotNull(predicate, "predicate");
    //        this.checkArgNotNull(fallbackStreamSupplier, "fallbackStreamSupplier");
    //
    //        final ExceptionalIterator<T, E> iter = new ExceptionalIterator<T, E>() {
    //            private boolean fallbackValueAvaiable = true;
    //            private ExceptionalIterator<T, E> iter = ExceptionalStream.this.elements;
    //            private ExceptionalStream<T, E> s = null;
    //
    //            @Override
    //            public boolean hasNext() throws E {
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
    //            @Override
    //            public T next() throws E {
    //                if (hasNext() == false) {
    //                    throw new NoSuchElementException();
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
    //            @Override
    //            public void close() {
    //                if ((s != null && N.notNullOrEmpty(s.closeHandlers)) || N.notNullOrEmpty(ExceptionalStream.this.closeHandlers)) {
    //                    try {
    //                        if (s != null) {
    //                            s.close();
    //                        }
    //                    } finally {
    //                        ExceptionalStream.this.close();
    //                    }
    //                }
    //            }
    //
    //            private void useFallbackStream(final Supplier<ExceptionalStream<T, E>> fallbackStream) {
    //                fallbackValueAvaiable = false;
    //                s = fallbackStream.get();
    //                iter = s.elements;
    //            }
    //        };
    //
    //        return newStream(iter).onClose(newCloseHandler(iter)).filter(NOT_NULL_MASK);
    //    }

    @IntermediateOp
    public ExceptionalStream<T, E> throwIfEmpty(final Supplier<? extends E> exceptionSupplier) {
        this.checkArgNotNull(exceptionSupplier, "exceptionSupplier");

        final Throwables.Supplier<ExceptionalStream<T, E>, E> tmp = () -> {
            throw exceptionSupplier.get();
        };

        return this.appendIfEmpty(tmp);
    }

    void close(Holder<? extends ExceptionalStream<T, E>> holder) {
        if (holder.value() != null) {
            holder.value().close();
        }
    }

    @TerminalOp
    public <R, E2 extends Exception> Optional<R> applyIfNotEmpty(final Throwables.Function<? super ExceptionalStream<T, E>, R, E2> func) throws E, E2 {
        assertNotClosed();

        try {
            if (elements.hasNext()) {
                return Optional.of(func.apply(this));
            } else {
                return Optional.empty();
            }
        } finally {
            close();
        }
    }

    @TerminalOp
    public <E2 extends Exception> OrElse acceptIfNotEmpty(Throwables.Consumer<? super ExceptionalStream<T, E>, E2> action) throws E, E2 {
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
    public ExceptionalStream<T, E> onEach(final Throwables.Consumer<? super T, ? extends E> action) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<T, E>() {
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
    public ExceptionalStream<T, E> peek(final Throwables.Consumer<? super T, ? extends E> action) {
        return onEach(action);
    }

    @IntermediateOp
    public ExceptionalStream<T, E> peekFirst(final Throwables.Consumer<? super T, ? extends E> action) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<T, E>() {
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

    @IntermediateOp
    public ExceptionalStream<T, E> peekLast(final Throwables.Consumer<? super T, ? extends E> action) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<T, E>() {
            private T next = null;

            @Override
            public boolean hasNext() throws E {
                return elements.hasNext();
            }

            @Override
            public T next() throws E {
                next = elements.next();

                if (elements.hasNext()) {
                    return next;
                } else {
                    final T e = elements.next();
                    action.accept(e);
                    return e;
                }
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
    public ExceptionalStream<T, E> peekIf(final Throwables.Predicate<? super T, E> predicate, final Throwables.Consumer<? super T, E> action) {
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
    public ExceptionalStream<T, E> peekIf(final Throwables.BiPredicate<? super T, ? super Long, E> predicate, final Consumer<? super T> action) {
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
    public ExceptionalStream<Stream<T>, E> split(final int chunkSize) {
        assertNotClosed();

        return splitToList(chunkSize).map(Stream::of);
    }

    /**
     * Returns ExceptionalStream of {@code List<T>} with consecutive sub sequences of the elements, each of the same size (the final sequence may be smaller).
     *
     *
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @return
     */
    @IntermediateOp
    public ExceptionalStream<List<T>, E> splitToList(final int chunkSize) {
        return split(chunkSize, Factory.<T> ofList());
    }

    /**
     * Returns ExceptionalStream of {@code Set<T>} with consecutive sub sequences of the elements, each of the same size (the final sequence may be smaller).
     *
     *
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @return
     */
    @IntermediateOp
    public ExceptionalStream<Set<T>, E> splitToSet(final int chunkSize) {
        return split(chunkSize, Factory.<T> ofSet());
    }

    /**
     * Returns ExceptionalStream of {@code C} with consecutive sub sequences of the elements, each of the same size (the final sequence may be smaller).
     *
     *
     * @param <C>
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @param collectionSupplier
     * @return
     */
    @IntermediateOp
    public <C extends Collection<T>> ExceptionalStream<C, E> split(final int chunkSize, final IntFunction<? extends C> collectionSupplier) {
        assertNotClosed();

        checkArgPositive(chunkSize, "chunkSize");

        return newStream(new ExceptionalIterator<C, E>() {
            @Override
            public boolean hasNext() throws E {
                return elements.hasNext();
            }

            @Override
            public C next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException();
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
     * @param <A>
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @param collector
     * @return
     */
    @IntermediateOp
    public <R, A> ExceptionalStream<R, E> split(final int chunkSize, final Collector<? super T, A, R> collector) {
        assertNotClosed();

        checkArgPositive(chunkSize, "chunkSize");

        final java.util.function.Supplier<A> supplier = collector.supplier();
        final java.util.function.BiConsumer<A, ? super T> accumulator = collector.accumulator();
        final java.util.function.Function<A, R> finisher = collector.finisher();

        return newStream(new ExceptionalIterator<R, E>() {
            @Override
            public boolean hasNext() throws E {
                return elements.hasNext();
            }

            @Override
            public R next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                final A container = supplier.get();
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

    @IntermediateOp
    public ExceptionalStream<ExceptionalStream<T, E>, E> splitAt(final int where) {
        assertNotClosed();

        checkArgNotNegative(where, "where");

        final ExceptionalIterator<T, E> iter = elements;

        return newStream(new ExceptionalIterator<ExceptionalStream<T, E>, E>() {
            private int cursor = 0;

            @Override
            public boolean hasNext() throws E {
                return cursor < 2;
            }

            @Override
            public ExceptionalStream<T, E> next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                ExceptionalStream<T, E> result = null;

                if (cursor == 0) {
                    final List<T> list = new ArrayList<>();
                    int cnt = 0;

                    while (cnt++ < where && iter.hasNext()) {
                        list.add(iter.next());
                    }

                    result = new ExceptionalStream<>(ExceptionalIterator.wrap(list.iterator()), sorted, cmp, null);
                } else {
                    result = new ExceptionalStream<>(iter, sorted, cmp, null);
                }

                cursor++;

                return result;
            }

            @Override
            public long count() throws E {
                iter.count();

                return 2 - cursor;
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

    @IntermediateOp
    public ExceptionalStream<ExceptionalStream<T, E>, E> splitAt(final Throwables.Predicate<? super T, ? extends E> where) {
        assertNotClosed();

        final ExceptionalIterator<T, E> iter = elements;

        return newStream(new ExceptionalIterator<ExceptionalStream<T, E>, E>() {
            private int cursor = 0;
            private T next = null;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() throws E {
                return cursor < 2;
            }

            @Override
            public ExceptionalStream<T, E> next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }

                ExceptionalStream<T, E> result = null;

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

                    result = new ExceptionalStream<>(ExceptionalIterator.wrap(list.iterator()), sorted, cmp, null);
                } else {
                    ExceptionalIterator<T, E> iterEx = iter;

                    if (hasNext) {
                        iterEx = new ExceptionalIterator<>() {
                            private boolean isFirst = true;

                            @Override
                            public boolean hasNext() throws E {
                                return isFirst || iter.hasNext();
                            }

                            @Override
                            public T next() throws E {
                                if (!hasNext()) {
                                    throw new NoSuchElementException();
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

                    result = new ExceptionalStream<>(iterEx, sorted, cmp, null);
                }

                cursor++;

                return result;
            }

            @Override
            public long count() throws E {
                iter.count();

                return 2 - cursor;
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
     * @param increment
     * @return
     */
    @IntermediateOp
    public ExceptionalStream<Stream<T>, E> sliding(final int windowSize, final int increment) {
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
    public ExceptionalStream<List<T>, E> slidingToList(final int windowSize, final int increment) {
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
    public ExceptionalStream<Set<T>, E> slidingToSet(final int windowSize, final int increment) {
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
    public <C extends Collection<T>> ExceptionalStream<C, E> sliding(final int windowSize, final int increment,
            final IntFunction<? extends C> collectionSupplier) {
        assertNotClosed();

        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        return newStream(new ExceptionalIterator<C, E>() {
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
                    throw new NoSuchElementException();
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
                    if (N.isNullOrEmpty(queue)) {
                        final long m = ((n - 1) > Long.MAX_VALUE / increment ? Long.MAX_VALUE : (n - 1) * increment);
                        elements.advance(m);
                    } else {
                        final long m = (n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                        final int prevSize = increment >= windowSize ? 0 : (queue == null ? 0 : queue.size());

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
     * @param <A>
     * @param <R>
     * @param windowSize
     * @param increment
     * @param collector
     * @return
     */
    @IntermediateOp
    public <A, R> ExceptionalStream<R, E> sliding(final int windowSize, final int increment, final Collector<? super T, A, R> collector) {
        assertNotClosed();

        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);

        final java.util.function.Supplier<A> supplier = collector.supplier();
        final java.util.function.BiConsumer<A, ? super T> accumulator = collector.accumulator();
        final java.util.function.Function<A, R> finisher = collector.finisher();

        return newStream(new ExceptionalIterator<R, E>() {
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
                    throw new NoSuchElementException();
                }

                if (increment < windowSize && queue == null) {
                    queue = new ArrayDeque<>(windowSize - increment);
                }

                final A container = supplier.get();
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
                    if (N.isNullOrEmpty(queue)) {
                        final long m = ((n - 1) > Long.MAX_VALUE / increment ? Long.MAX_VALUE : (n - 1) * increment);
                        elements.advance(m);
                    } else {
                        final long m = (n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                        final int prevSize = increment >= windowSize ? 0 : (queue == null ? 0 : queue.size());

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
    public ExceptionalStream<T, E> skip(final long n) {
        assertNotClosed();

        checkArgNotNegative(n, "n");

        if (n == 0) {
            return newStream(elements, sorted, cmp, closeHandlers);
        }

        return newStream(new ExceptionalIterator<T, E>() {
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

    @IntermediateOp
    public ExceptionalStream<T, E> skipNull() {
        return filter(Fnn.notNull());
    }

    /**
     *
     * @param maxSize
     * @return
     */
    @IntermediateOp
    public ExceptionalStream<T, E> limit(final long maxSize) {
        assertNotClosed();

        checkArgNotNegative(maxSize, "maxSize");

        return newStream(new ExceptionalIterator<T, E>() {
            private long cnt = 0;

            @Override
            public boolean hasNext() throws E {
                return cnt < maxSize && elements.hasNext();
            }

            @Override
            public T next() throws E {
                if (cnt >= maxSize) {
                    throw new NoSuchElementException();
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
    //    public ExceptionalStream<T, E> slice(final long from, final long to) {
    //        checkArgNotNegative(from, "from");
    //        checkArgNotNegative(to, "to");
    //        checkArgument(to >= from, "'to' can't be less than `from`");
    //
    //        return from == 0 ? limit(to) : skip(from).limit(to - from);
    //    }

    @IntermediateOp
    public ExceptionalStream<T, E> top(int n) {
        assertNotClosed();

        return top(n, Comparators.NATURAL_ORDER);
    }

    @IntermediateOp
    public ExceptionalStream<T, E> top(final int n, final Comparator<? super T> comparator) {
        assertNotClosed();

        checkArgPositive(n, "n");

        return newStream(new ExceptionalIterator<T, E>() {
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
                    throw new NoSuchElementException();
                }

                return aar[cursor++];
            }

            @Override
            public long count() throws E {
                if (!initialized) {
                    init();
                }

                return to - cursor;
            }

            @Override
            public void advance(long n) throws E {
                if (!initialized) {
                    init();
                }

                cursor = n < to - cursor ? cursor + (int) n : to;
            }

            //    @Override
            //    public <A> A[] toArray(A[] b) throws E {
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
    public ExceptionalStream<T, E> last(final int n) {
        assertNotClosed();

        checkArgNotNegative(n, "n");

        if (n == 0) {
            return limit(0);
        }

        return newStream(new ExceptionalIterator<T, E>() {
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
                        ExceptionalStream.this.close();
                    }

                    iter = deque.iterator();
                }
            }
        }, sorted, cmp, closeHandlers);
    }

    @IntermediateOp
    public ExceptionalStream<T, E> skipLast(final int n) {
        assertNotClosed();

        if (n <= 0) {
            return newStream(elements, sorted, cmp, closeHandlers);
        }

        return newStream(new ExceptionalIterator<T, E>() {
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
                    throw new NoSuchElementException();
                }

                deque.offerLast(elements.next());

                return deque.pollFirst();
            }

        }, sorted, cmp, closeHandlers);
    }

    @IntermediateOp
    @TerminalOpTriggered
    public ExceptionalStream<T, E> reversed() {
        assertNotClosed();

        return newStream(new ExceptionalIterator<T, E>() {
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
                    throw new NoSuchElementException();
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
                    aar = (T[]) ExceptionalStream.this.toArrayForIntermediateOp();
                    cursor = aar.length;
                }
            }
        }, false, null, closeHandlers);
    }

    @IntermediateOp
    @TerminalOpTriggered
    public ExceptionalStream<T, E> rotated(final int distance) {
        assertNotClosed();

        if (distance == 0) {
            return newStream(elements, closeHandlers);
        }

        return newStream(new ExceptionalIterator<T, E>() {
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
                    throw new NoSuchElementException();
                }

                return aar[(start + cnt++) % len];
            }

            @Override
            public long count() throws E {
                if (!initialized) {
                    init();
                }

                return len - cnt;
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
                    aar = (T[]) ExceptionalStream.this.toArrayForIntermediateOp();
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

    @IntermediateOp
    @TerminalOpTriggered
    public ExceptionalStream<T, E> shuffled() {
        return shuffled(RAND);
    }

    @IntermediateOp
    @TerminalOpTriggered
    public ExceptionalStream<T, E> shuffled(final Random rnd) {
        assertNotClosed();

        return lazyLoad(a -> {
            N.shuffle(a, rnd);
            return a;
        }, false, null);
    }

    @IntermediateOp
    @TerminalOpTriggered
    public ExceptionalStream<T, E> sorted() {
        return sorted(Comparators.NATURAL_ORDER);
    }

    /**
     *
     * @param comparator
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    public ExceptionalStream<T, E> sorted(final Comparator<? super T> comparator) {
        assertNotClosed();

        final Comparator<? super T> cmp = comparator == null ? Comparators.NATURAL_ORDER : comparator;

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
     * @param keyMapper
     * @return
     */
    @IntermediateOp
    @TerminalOpTriggered
    @SuppressWarnings("rawtypes")
    public ExceptionalStream<T, E> sortedBy(final Function<? super T, ? extends Comparable> keyMapper) {
        assertNotClosed();

        final Comparator<? super T> comparator = (o1, o2) -> N.compare(keyMapper.apply(o1), keyMapper.apply(o2));

        return sorted(comparator);
    }

    @IntermediateOp
    @TerminalOpTriggered
    public ExceptionalStream<T, E> reverseSorted() {
        return sorted(Comparators.REVERSED_ORDER);
    }

    @IntermediateOp
    @TerminalOpTriggered
    public ExceptionalStream<T, E> reverseSorted(Comparator<? super T> comparator) {
        final Comparator<? super T> cmp = comparator == null ? Comparators.REVERSED_ORDER : comparator.reversed();

        return sorted(cmp);
    }

    /**
     *
     * @param op
     * @param sorted
     * @param cmp
     * @return
     */
    private ExceptionalStream<T, E> lazyLoad(final Function<Object[], Object[]> op, final boolean sorted, final Comparator<? super T> cmp) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<T, E>() {
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
                    throw new NoSuchElementException();
                }

                return aar[cursor++];
            }

            @Override
            public long count() throws E {
                if (!initialized) {
                    init();
                }

                return len - cursor;
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
                    aar = (T[]) op.apply(ExceptionalStream.this.toArrayForIntermediateOp());
                    len = aar.length;
                }
            }
        }, sorted, cmp, closeHandlers);
    }

    @SequentialOnly
    @IntermediateOp
    public ExceptionalStream<T, E> cycled() {
        assertNotClosed();

        return newStream(new ExceptionalIterator<T, E>() {
            private ExceptionalIterator<T, E> iter = null;
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

                if (a == null && !iter.hasNext()) {
                    a = (T[]) list.toArray();
                    len = a.length;
                    cursor = 0;
                }

                return cursor < len || iter.hasNext();
            }

            @Override
            public T next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException();
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
                    iter = ExceptionalStream.this.iteratorEx();
                    list = new ArrayList<>();
                }
            }
        }, false, null, closeHandlers);
    }

    @SequentialOnly
    @IntermediateOp
    public ExceptionalStream<T, E> cycled(long times) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<T, E>() {
            private ExceptionalIterator<T, E> iter = null;
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

                if (a == null && !iter.hasNext()) {
                    a = (T[]) list.toArray();
                    len = a.length;
                    cursor = 0;
                    m = 1;
                }

                return m < times && (cursor < len || times - m > 1) && (len > 0 || iter.hasNext());
            }

            @Override
            public T next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException();
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
                    iter = ExceptionalStream.this.iteratorEx();
                    list = new ArrayList<>();
                }
            }
        }, false, null, closeHandlers);
    }

    /**
     *
     * @param rateLimiter
     * @return
     */
    @IntermediateOp
    public ExceptionalStream<T, E> rateLimited(double permitsPerSecond) {
        return rateLimited(RateLimiter.create(permitsPerSecond));
    }

    /**
     *
     * @param permitsPerSecond
     * @return
     * @see RateLimiter#create(double)
     */
    @IntermediateOp
    public ExceptionalStream<T, E> rateLimited(final RateLimiter rateLimiter) {
        assertNotClosed();
        checkArgNotNull(rateLimiter, "rateLimiter");

        final Throwables.Consumer<T, E> action = it -> rateLimiter.acquire();

        return onEach(action);
    }

    @IntermediateOp
    public ExceptionalStream<T, E> intersperse(final T delimiter) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<T, E>() {
            private final ExceptionalIterator<T, E> iter = iteratorEx();
            private boolean toInsert = false;

            @Override
            public boolean hasNext() throws E {
                return iter.hasNext();
            }

            @Override
            public T next() throws E {
                if (!hasNext()) {
                    throw new NoSuchElementException();
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

    @Beta
    @IntermediateOp
    public ExceptionalStream<T, E> step(final long step) {
        assertNotClosed();

        checkArgPositive(step, "step");

        if (step == 1) {
            return skip(0);
        }

        final long skip = step - 1;
        final ExceptionalIterator<T, E> iter = this.iteratorEx();

        final ExceptionalIterator<T, E> iterator = new ExceptionalIterator<>() {
            @Override
            public boolean hasNext() throws E {
                return iter.hasNext();
            }

            @Override
            public T next() throws E {
                final T next = iter.next();
                iter.advance(skip);
                return next;
            }
        };

        return newStream(iterator, sorted, cmp, closeHandlers);
    }

    @Beta
    @IntermediateOp
    public ExceptionalStream<Indexed<T>, E> indexed() {
        assertNotClosed();

        return map(new Throwables.Function<T, Indexed<T>, E>() {
            private final MutableLong idx = new MutableLong(0);

            @Override
            public Indexed<T> apply(T t) {
                return Indexed.of(t, idx.getAndIncrement());
            }
        });
    }

    /**
     *
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    @IntermediateOp
    public ExceptionalStream<T, E> mergeWith(final Collection<? extends T> b, final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        assertNotClosed();

        return mergeWith(ExceptionalStream.of(b), nextSelector);
    }

    /**
     *
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    @IntermediateOp
    public ExceptionalStream<T, E> mergeWith(final ExceptionalStream<? extends T, E> b,
            final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
        assertNotClosed();

        return ExceptionalStream.merge(this, b, nextSelector);
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
    public <T2, R> ExceptionalStream<R, E> zipWith(final Collection<T2> b, final Throwables.BiFunction<? super T, ? super T2, R, ? extends E> zipFunction) {
        assertNotClosed();

        return zip(this, ExceptionalStream.of(b), zipFunction);
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
    public <T2, R> ExceptionalStream<R, E> zipWith(final Collection<T2> b, final T valueForNoneA, final T2 valueForNoneB,
            final Throwables.BiFunction<? super T, ? super T2, R, ? extends E> zipFunction) {
        assertNotClosed();

        return zip(this, ExceptionalStream.of(b), valueForNoneA, valueForNoneB, zipFunction);
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
    public <T2, T3, R> ExceptionalStream<R, E> zipWith(final Collection<T2> b, final Collection<T3> c,
            final Throwables.TriFunction<? super T, ? super T2, ? super T3, R, ? extends E> zipFunction) {
        assertNotClosed();

        return zip(this, ExceptionalStream.of(b), ExceptionalStream.of(c), zipFunction);
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
    public <T2, T3, R> ExceptionalStream<R, E> zipWith(final Collection<T2> b, final Collection<T3> c, final T valueForNoneA, final T2 valueForNoneB,
            final T3 valueForNoneC, final Throwables.TriFunction<? super T, ? super T2, ? super T3, R, ? extends E> zipFunction) {
        assertNotClosed();

        return zip(this, ExceptionalStream.of(b), ExceptionalStream.of(c), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
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
    public <T2, R> ExceptionalStream<R, E> zipWith(final ExceptionalStream<T2, E> b,
            final Throwables.BiFunction<? super T, ? super T2, R, ? extends E> zipFunction) {
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
    public <T2, R> ExceptionalStream<R, E> zipWith(final ExceptionalStream<T2, E> b, final T valueForNoneA, final T2 valueForNoneB,
            final Throwables.BiFunction<? super T, ? super T2, R, ? extends E> zipFunction) {
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
    public <T2, T3, R> ExceptionalStream<R, E> zipWith(final ExceptionalStream<T2, E> b, final ExceptionalStream<T3, E> c,
            final Throwables.TriFunction<? super T, ? super T2, ? super T3, R, ? extends E> zipFunction) {
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
    public <T2, T3, R> ExceptionalStream<R, E> zipWith(final ExceptionalStream<T2, E> b, final ExceptionalStream<T3, E> c, final T valueForNoneA,
            final T2 valueForNoneB, final T3 valueForNoneC, final Throwables.TriFunction<? super T, ? super T2, ? super T3, R, ? extends E> zipFunction) {
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
    //    public ExceptionalStream<T, E> onErrorContinue(final Throwables.Consumer<? super Throwable, ? extends E> errorConsumer) {
    //        assertNotClosed();
    //
    //        return newStream(new ExceptionalIterator<T, E>() {
    //            private final ExceptionalIterator<T, E> iter = iteratorEx();
    //            private final T none = (T) NONE;
    //            private T next = none;
    //            private T ret = null;
    //
    //            @Override
    //            public boolean hasNext() throws E {
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
    //            @Override
    //            public T next() throws E {
    //                if (!hasNext()) {
    //                    throw new NoSuchElementException();
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
    //    public ExceptionalStream<T, E> onErrorContinue(final Class<? extends Throwable> type,
    //            final Throwables.Consumer<? super Throwable, ? extends E> errorConsumer) {
    //        assertNotClosed();
    //
    //        return newStream(new ExceptionalIterator<T, E>() {
    //            private final ExceptionalIterator<T, E> iter = iteratorEx();
    //            private final T none = (T) NONE;
    //            private T next = none;
    //            private T ret = null;
    //
    //            @Override
    //            public boolean hasNext() throws E {
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
    //            @Override
    //            public T next() throws E {
    //                if (!hasNext()) {
    //                    throw new NoSuchElementException();
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
    //    public ExceptionalStream<T, E> onErrorContinue(final Throwables.Predicate<? super Throwable, ? extends E> errorPredicate,
    //            final Throwables.Consumer<? super Throwable, ? extends E> errorConsumer) {
    //        assertNotClosed();
    //
    //        return newStream(new ExceptionalIterator<T, E>() {
    //            private final ExceptionalIterator<T, E> iter = iteratorEx();
    //            private final T none = (T) NONE;
    //            private T next = none;
    //            private T ret = null;
    //
    //            @Override
    //            public boolean hasNext() throws E {
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
    //            @Override
    //            public T next() throws E {
    //                if (!hasNext()) {
    //                    throw new NoSuchElementException();
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
    //    public ExceptionalStream<T, E> onErrorContinue(final Throwables.Predicate<? super Throwable, ? extends E> errorPredicate,
    //            final Throwables.Consumer<? super Throwable, ? extends E> errorConsumer, final int maxErrorCountToStop) {
    //        assertNotClosed();
    //        checkArgNotNegative(maxErrorCountToStop, "maxErrorCountToStop");
    //
    //        return newStream(new ExceptionalIterator<T, E>() {
    //            private final AtomicInteger errorCounter = new AtomicInteger(maxErrorCountToStop);
    //            private final ExceptionalIterator<T, E> iter = iteratorEx();
    //            private final T none = (T) NONE;
    //            private T next = none;
    //            private T ret = null;
    //
    //            @Override
    //            public boolean hasNext() throws E {
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
    //            @Override
    //            public T next() throws E {
    //                if (!hasNext()) {
    //                    throw new NoSuchElementException();
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
    //    public ExceptionalStream<T, E> onErrorReturn(final T fallbackValue) {
    //        assertNotClosed();
    //
    //        return newStream(new ExceptionalIterator<T, E>() {
    //            private final ExceptionalIterator<T, E> iter = iteratorEx();
    //            private final T none = (T) NONE;
    //            private T next = none;
    //            private T ret = null;
    //
    //            @Override
    //            public boolean hasNext() {
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
    //            @Override
    //            public T next() {
    //                if (!hasNext()) {
    //                    throw new NoSuchElementException();
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
    //    public ExceptionalStream<T, E> onErrorReturn(final Class<? extends Throwable> type, final T fallbackValue) {
    //        assertNotClosed();
    //
    //        return newStream(new ExceptionalIterator<T, E>() {
    //            private final ExceptionalIterator<T, E> iter = iteratorEx();
    //            private final T none = (T) NONE;
    //            private T next = none;
    //            private T ret = null;
    //
    //            @Override
    //            public boolean hasNext() throws E {
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
    //            @Override
    //            public T next() throws E {
    //                if (!hasNext()) {
    //                    throw new NoSuchElementException();
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
    //    public ExceptionalStream<T, E> onErrorReturn(final Throwables.Predicate<? super Throwable, ? extends E> predicate, final T fallbackValue) {
    //        assertNotClosed();
    //
    //        return newStream(new ExceptionalIterator<T, E>() {
    //            private final ExceptionalIterator<T, E> iter = iteratorEx();
    //            private final T none = (T) NONE;
    //            private T next = none;
    //            private T ret = null;
    //
    //            @Override
    //            public boolean hasNext() throws E {
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
    //            @Override
    //            public T next() throws E {
    //                if (!hasNext()) {
    //                    throw new NoSuchElementException();
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
    //    public ExceptionalStream<T, E> onErrorReturn(final Throwables.Predicate<? super Throwable, ? extends E> predicate,
    //            final Throwables.Supplier<? extends T, ? extends E> supplierForFallbackValue) {
    //        assertNotClosed();
    //
    //        return newStream(new ExceptionalIterator<T, E>() {
    //            private final ExceptionalIterator<T, E> iter = iteratorEx();
    //            private final T none = (T) NONE;
    //            private T next = none;
    //            private T ret = null;
    //
    //            @Override
    //            public boolean hasNext() throws E {
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
    //            @Override
    //            public T next() throws E {
    //                if (!hasNext()) {
    //                    throw new NoSuchElementException();
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
    //    public ExceptionalStream<T, E> onErrorReturn(final Throwables.Predicate<? super Throwable, ? extends E> predicate,
    //            final Throwables.Function<? super Throwable, ? extends T, ? extends E> mapperForFallbackValue, final int maxErrorCountToStop) {
    //        assertNotClosed();
    //        checkArgNotNegative(maxErrorCountToStop, "maxErrorCountToStop");
    //
    //        return newStream(new ExceptionalIterator<T, E>() {
    //            private final AtomicInteger errorCounter = new AtomicInteger(maxErrorCountToStop);
    //            private final ExceptionalIterator<T, E> iter = iteratorEx();
    //            private final T none = (T) NONE;
    //            private T next = none;
    //            private T ret = null;
    //
    //            @Override
    //            public boolean hasNext() throws E {
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
    //            @Override
    //            public T next() throws E {
    //                if (!hasNext()) {
    //                    throw new NoSuchElementException();
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
    //    public ExceptionalStream<T, E> onErrorStop() {
    //        assertNotClosed();
    //
    //        return newStream(new ExceptionalIterator<T, E>() {
    //            private final ExceptionalIterator<T, E> iter = iteratorEx();
    //            private final T none = (T) NONE;
    //            private T next = none;
    //            private T ret = null;
    //
    //            @Override
    //            public boolean hasNext() {
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
    //            @Override
    //            public T next() {
    //                if (!hasNext()) {
    //                    throw new NoSuchElementException();
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
    public <E2 extends Exception> void forEachIndexed(Throwables.IndexedConsumer<? super T, E2> action) throws E, E2 {
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
     * @see #forEachUntil(MutableBoolean, com.landawn.abacus.util.Throwables.Consumer)
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
     * @see #forEachUntil(com.landawn.abacus.util.Throwables.BiConsumer)
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
    public <U, E2 extends Exception, E3 extends Exception> void forEach(final Throwables.Function<? super T, ? extends Collection<? extends U>, E2> flatMapper,
            final Throwables.BiConsumer<? super T, ? super U, E3> action) throws E, E2, E3 {
        assertNotClosed();

        checkArgNotNull(flatMapper, "flatMapper");
        checkArgNotNull(action, "action");

        Collection<? extends U> c = null;
        T next = null;

        try {
            while (elements.hasNext()) {
                next = elements.next();
                c = flatMapper.apply(next);

                if (N.notNullOrEmpty(c)) {
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
            final Throwables.Function<? super T, ? extends Collection<T2>, E2> flatMapper,
            final Throwables.Function<? super T2, ? extends Collection<T3>, E3> flatMapper2,
            final Throwables.TriConsumer<? super T, ? super T2, ? super T3, E4> action) throws E, E2, E3, E4 {
        assertNotClosed();

        checkArgNotNull(flatMapper, "flatMapper");
        checkArgNotNull(flatMapper2, "flatMapper2");
        checkArgNotNull(action, "action");

        Collection<T2> c2 = null;
        Collection<T3> c3 = null;
        T next = null;

        try {
            while (elements.hasNext()) {
                next = elements.next();
                c2 = flatMapper.apply(next);

                if (N.notNullOrEmpty(c2)) {
                    for (T2 t2 : c2) {
                        c3 = flatMapper2.apply(t2);

                        if (N.notNullOrEmpty(c3)) {
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
        forEachPair(action, 1);
    }

    /**
     * For each pair.
     *
     * @param <E2>
     * @param action
     * @param increment
     * @throws E the e
     * @throws E2 the e2
     */
    @TerminalOp
    public <E2 extends Exception> void forEachPair(final Throwables.BiConsumer<? super T, ? super T, E2> action, final int increment) throws E, E2 {
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
        forEachTriple(action, 1);
    }

    /**
     * For each triple.
     *
     * @param <E2>
     * @param action
     * @param increment
     * @throws E the e
     * @throws E2 the e2
     */
    @TerminalOp
    public <E2 extends Exception> void forEachTriple(final Throwables.TriConsumer<? super T, ? super T, ? super T, E2> action, final int increment)
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
     * @param <E2>
     * @param action
     * @throws E the e
     * @throws E2 the e2
     */
    @Beta
    @TerminalOp
    public <E2 extends Exception> void forEachInParallel(Throwables.Consumer<? super T, E2> action, final int threadNum) throws E, E2 {
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
     * @param <E2>
     * @param action
     * @throws E the e
     * @throws E2 the e2
     */
    @Beta
    @TerminalOp
    public <E2 extends Exception> void forEachInParallel(Throwables.Consumer<? super T, E2> action, final int threadNum, final Executor executor) throws E, E2 {
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

            comparator = comparator == null ? Comparators.NATURAL_ORDER : comparator;
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

        try {
            final Comparator<? super T> comparator = Fn.comparingBy(keyMapper);

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

            comparator = comparator == null ? Comparators.NATURAL_ORDER : comparator;
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

        try {
            final Comparator<? super T> comparator = Fn.comparingBy(keyMapper);

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
                } else if (ret == NONE) {
                    if (predicateForAny.test(next)) {
                        ret = next;
                    }
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
     * @see #findFirst(com.landawn.abacus.util.Throwables.Predicate)
     */
    @TerminalOp
    public <E2 extends Exception> Optional<T> findAny(final Throwables.Predicate<? super T, E2> predicate) throws E, E2 {
        return findFirst(predicate);
    }

    @TerminalOp
    @SafeVarargs
    public final boolean containsAll(final T... a) throws E {
        assertNotClosed();

        try {
            if (N.isNullOrEmpty(a)) {
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

    @TerminalOp
    public boolean containsAll(final Collection<? extends T> c) throws E {
        assertNotClosed();

        try {
            if (N.isNullOrEmpty(c)) {
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

    @TerminalOp
    @SafeVarargs
    public final boolean containsAny(final T... a) throws E {
        assertNotClosed();

        try {
            if (N.isNullOrEmpty(a)) {
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

    @TerminalOp
    public boolean containsAny(final Collection<? extends T> c) throws E {
        assertNotClosed();

        try {
            if (N.isNullOrEmpty(c)) {
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

            comparator = comparator == null ? Comparators.NATURAL_ORDER : comparator;
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

    @TerminalOp
    public Optional<Map<Percentage, T>> percentiles(Comparator<? super T> comparator) throws E {
        assertNotClosed();

        try {
            final Object[] a = sorted(comparator).toArray();

            if (N.isNullOrEmpty(a)) {
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
                    throw new TooManyElementsException("There are at least two elements: " + StringUtil.concat(result.get(), ", ", elements.next()));
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

    // It won't work for findFirst/only/anyMatch...
    //    public <R> Pair<Long, R> countAnd(final Throwables.Function<? super ExceptionalStream<T, E>, R, E> terminalAction) throws E {
    //        checkArgNotNull(terminalAction, "terminalAction");
    //
    //        final MutableLong count = MutableLong.of(0);
    //
    //        final Throwables.Consumer<? super T, E> action = new Throwables.Consumer<T, E>() {
    //            @Override
    //            public void accept(T t) {
    //                count.incrementAndGet();
    //            }
    //        };
    //
    //        final R r = terminalAction.apply(this.onEach(action));
    //
    //        return Pair.of(count.value(), r);
    //    }

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
        return ImmutableList.of(toList());
    }

    /**
     *
     * @return
     * @throws E the e
     */
    @TerminalOp
    public ImmutableSet<T> toImmutableSet() throws E {
        return ImmutableSet.of(toSet());
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

    @TerminalOp
    public <R, E2 extends Exception> R toListAndThen(Throwables.Function<? super List<T>, R, E2> func) throws E, E2 {
        assertNotClosed();

        return func.apply(toList());
    }

    @TerminalOp
    public <R, E2 extends Exception> R toSetAndThen(Throwables.Function<? super Set<T>, R, E2> func) throws E, E2 {
        assertNotClosed();

        return func.apply(toSet());
    }

    @TerminalOp
    public <R, CC extends Collection<T>, E2 extends Exception> R toCollectionAndThen(Supplier<? extends CC> supplier,
            Throwables.Function<? super CC, R, E2> func) throws E, E2 {
        assertNotClosed();

        return func.apply(toCollection(supplier));
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyMapper
     * @param valueMapper
     * @return
     * @throws E the e
     * @throws IllegalStateException if there are duplicated keys.
     * @see {@link Fn.Fnn#throwingMerger()}
     * @see {@link Fn.Fnn#replacingMerger()}
     * @see {@link Fn.Fnn#ignoringMerger()}
     */
    @TerminalOp
    public <K, V, E2 extends Exception, E3 extends Exception> ImmutableMap<K, V> toImmutableMap(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Throwables.Function<? super T, ? extends V, E3> valueMapper) throws E, E2, E3, IllegalStateException {
        return ImmutableMap.of(toMap(keyMapper, valueMapper));
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @return
     * @throws E the e
     * @see {@link Fn.Fnn#throwingMerger()}
     * @see {@link Fn.Fnn#replacingMerger()}
     * @see {@link Fn.Fnn#ignoringMerger()}
     */
    @TerminalOp
    public <K, V, E2 extends Exception, E3 extends Exception, E4 extends Exception> ImmutableMap<K, V> toImmutableMap(
            final Throwables.Function<? super T, ? extends K, E2> keyMapper, final Throwables.Function<? super T, ? extends V, E3> valueMapper,
            final Throwables.BinaryOperator<V, E4> mergeFunction) throws E, E2, E3, E4 {
        return ImmutableMap.of(toMap(keyMapper, valueMapper, mergeFunction));
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyMapper
     * @param valueMapper
     * @return
     * @throws E the e
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
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @throws E the e
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
     * @param <K> the key type
     * @param <V> the value type
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @return
     * @throws E the e
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
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @param mapFactory
     * @return
     * @throws E the e
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
                Maps.merge(result, keyMapper.apply(next), valueMapper.apply(next), mergeFunction);
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     *
     * @param keyMapper
     * @param downstream
     * @return
     * @see #groupTo(com.landawn.abacus.util.Throwables.Function, Collector)
     * @deprecated replaced by {@code groupTo}
     */
    @Deprecated
    @TerminalOp
    public final <K, A, D, E2 extends Exception> Map<K, D> toMap(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Collector<? super T, A, D> downstream) throws E, E2 {
        return groupTo(keyMapper, downstream);
    }

    /**
     *
     * @param keyMapper
     * @param downstream
     * @param mapFactory
     * @return
     * @see #groupTo(com.landawn.abacus.util.Throwables.Function, Collector, Supplier)
     * @deprecated replaced by {@code groupTo}
     */
    @Deprecated
    @TerminalOp
    public final <K, A, D, M extends Map<K, D>, E2 extends Exception> M toMap(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Collector<? super T, A, D> downstream, final Supplier<? extends M> mapFactory) throws E, E2 {
        return groupTo(keyMapper, downstream, mapFactory);
    }

    /**
     *
     * @param keyMapper
     * @param valueMapper
     * @param downstream
     * @return
     * @see #groupTo(com.landawn.abacus.util.Throwables.Function, com.landawn.abacus.util.Throwables.Function, Collector)
     * @deprecated replaced by {@code groupTo}
     */
    @Deprecated
    @TerminalOp
    public final <K, V, A, D, E2 extends Exception, E3 extends Exception> Map<K, D> toMap(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Throwables.Function<? super T, ? extends V, E3> valueMapper, final Collector<? super V, A, D> downstream) throws E, E2, E3 {
        return groupTo(keyMapper, valueMapper, downstream);
    }

    /**
     *
     * @param keyMapper
     * @param valueMapper
     * @param downstream
     * @param mapFactory
     * @return
     * @see #groupTo(com.landawn.abacus.util.Throwables.Function, com.landawn.abacus.util.Throwables.Function, Collector, Supplier)
     * @deprecated replaced by {@code groupTo}
     */
    @Deprecated
    @TerminalOp
    public final <K, V, A, D, M extends Map<K, D>, E2 extends Exception, E3 extends Exception> M toMap(
            final Throwables.Function<? super T, ? extends K, E2> keyMapper, final Throwables.Function<? super T, ? extends V, E3> valueMapper,
            final Collector<? super V, A, D> downstream, final Supplier<? extends M> mapFactory) throws E, E2, E3 {
        return groupTo(keyMapper, valueMapper, downstream, mapFactory);
    }

    /**
     *
     * @param <K> the key type
     * @param keyMapper
     * @return
     * @throws E the e
     * @see Collectors#groupingBy(Function)
     */
    @TerminalOp
    public <K, E2 extends Exception> Map<K, List<T>> groupTo(Throwables.Function<? super T, ? extends K, E2> keyMapper) throws E, E2 {
        return groupTo(keyMapper, Suppliers.<K, List<T>> ofMap());
    }

    /**
     *
     * @param <K> the key type
     * @param <M>
     * @param keyMapper
     * @param mapFactory
     * @return
     * @throws E the e
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
     * @param <K> the key type
     * @param <V> the value type
     * @param keyMapper
     * @param valueMapper
     * @return
     * @throws E the e
     */
    @TerminalOp
    public <K, V, E2 extends Exception, E3 extends Exception> Map<K, List<V>> groupTo(Throwables.Function<? super T, ? extends K, E2> keyMapper,
            Throwables.Function<? super T, ? extends V, E3> valueMapper) throws E, E2, E3 {
        return groupTo(keyMapper, valueMapper, Suppliers.<K, List<V>> ofMap());
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @throws E the e
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
     * @param <K> the key type
     * @param <A>
     * @param <D>
     * @param keyMapper
     * @param downstream
     * @return
     * @throws E the e
     */
    @TerminalOp
    public <K, A, D, E2 extends Exception> Map<K, D> groupTo(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Collector<? super T, A, D> downstream) throws E, E2 {
        return groupTo(keyMapper, downstream, Suppliers.<K, D> ofMap());
    }

    /**
     *
     * @param <K> the key type
     * @param <A>
     * @param <D>
     * @param <M>
     * @param keyMapper
     * @param downstream
     * @param mapFactory
     * @return
     * @throws E the e
     */
    @TerminalOp
    public <K, A, D, M extends Map<K, D>, E2 extends Exception> M groupTo(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Collector<? super T, A, D> downstream, final Supplier<? extends M> mapFactory) throws E, E2 {
        return groupTo(keyMapper, Fnn.<T, E> identity(), downstream, mapFactory);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <A>
     * @param <D>
     * @param keyMapper
     * @param valueMapper
     * @param downstream
     * @return
     * @throws E the e
     */
    @TerminalOp
    public <K, V, A, D, E2 extends Exception, E3 extends Exception> Map<K, D> groupTo(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Throwables.Function<? super T, ? extends V, E3> valueMapper, final Collector<? super V, A, D> downstream) throws E, E2, E3 {
        return groupTo(keyMapper, valueMapper, downstream, Suppliers.<K, D> ofMap());
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <A>
     * @param <D>
     * @param <M>
     * @param keyMapper
     * @param valueMapper
     * @param downstream
     * @param mapFactory
     * @return
     * @throws E the e
     */
    @TerminalOp
    public <K, V, A, D, M extends Map<K, D>, E2 extends Exception, E3 extends Exception> M groupTo(
            final Throwables.Function<? super T, ? extends K, E2> keyMapper, final Throwables.Function<? super T, ? extends V, E3> valueMapper,
            final Collector<? super V, A, D> downstream, final Supplier<? extends M> mapFactory) throws E, E2, E3 {
        assertNotClosed();

        checkArgNotNull(keyMapper, "keyMapper");
        checkArgNotNull(valueMapper, "valueMapper");
        checkArgNotNull(downstream, "downstream");
        checkArgNotNull(mapFactory, "mapFactory");

        try {
            final java.util.function.Supplier<A> downstreamSupplier = downstream.supplier();
            final java.util.function.BiConsumer<A, ? super V> downstreamAccumulator = downstream.accumulator();
            final java.util.function.Function<A, D> downstreamFinisher = downstream.finisher();

            final M result = mapFactory.get();
            final Map<K, A> tmp = (Map<K, A>) result;
            T next = null;
            K key = null;
            A container = null;

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
                entry.setValue(downstreamFinisher.apply((A) entry.getValue()));
            }

            return result;
        } finally {
            close();
        }
    }

    /**
     *
     * @param predicate
     * @return
     * @see Collectors#partitioningBy(Predicate)
     */
    @TerminalOp
    public <E2 extends Exception> Map<Boolean, List<T>> partitionTo(final Throwables.Predicate<? super T, E2> predicate) throws E, E2 {
        assertNotClosed();

        return partitionTo(predicate, Collectors.<T> toList());
    }

    /**
     *
     * @param predicate
     * @param downstream
     * @return
     * @see Collectors#partitioningBy(Predicate, Collector)
     */
    @TerminalOp
    public <A, D, E2 extends Exception> Map<Boolean, D> partitionTo(final Throwables.Predicate<? super T, E2> predicate,
            final Collector<? super T, A, D> downstream) throws E, E2 {
        assertNotClosed();

        final Throwables.Function<T, Boolean, E2> keyMapper = t -> predicate.test(t);

        final Supplier<Map<Boolean, D>> mapFactory = () -> N.<Boolean, D> newHashMap(2);

        final Map<Boolean, D> map = groupTo(keyMapper, downstream, mapFactory);

        if (!map.containsKey(Boolean.TRUE)) {
            map.put(Boolean.TRUE, downstream.finisher().apply(downstream.supplier().get()));
        } else if (!map.containsKey(Boolean.FALSE)) {
            map.put(Boolean.FALSE, downstream.finisher().apply(downstream.supplier().get()));
        }

        return map;
    }

    @TerminalOp
    public <K, E2 extends Exception> ListMultimap<K, T> toMultimap(final Throwables.Function<? super T, ? extends K, E2> keyMapper) throws E, E2 {
        return toMultimap(keyMapper, Suppliers.<K, T> ofListMultimap());
    }

    @TerminalOp
    public <K, V extends Collection<T>, M extends Multimap<K, T, V>, E2 extends Exception> M toMultimap(
            final Throwables.Function<? super T, ? extends K, E2> keyMapper, Supplier<? extends M> mapFactory) throws E, E2 {
        final Throwables.Function<T, T, E> valueMapper = Fnn.identity();

        return toMultimap(keyMapper, valueMapper, mapFactory);
    }

    @TerminalOp
    public <K, V, E2 extends Exception, E3 extends Exception> ListMultimap<K, V> toMultimap(final Throwables.Function<? super T, ? extends K, E2> keyMapper,
            final Throwables.Function<? super T, ? extends V, E3> valueMapper) throws E, E2, E3 {
        return toMultimap(keyMapper, valueMapper, Suppliers.<K, V> ofListMultimap());
    }

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

    @TerminalOp
    public Multiset<T> toMultiset() throws E {
        return toMultiset(Suppliers.<T> ofMultiset());
    }

    @TerminalOp
    public Multiset<T> toMultiset(Supplier<? extends Multiset<T>> supplier) throws E {
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

    @TerminalOp
    public LongMultiset<T> toLongMultiset() throws E {
        return toLongMultiset(Suppliers.<T> ofLongMultiset());
    }

    @TerminalOp
    public LongMultiset<T> toLongMultiset(Supplier<? extends LongMultiset<T>> supplier) throws E {
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
     * or obtain the column names from first row if its type is entity or map.
     *
     * @return
     * @throws E
     * @see {@link N#newDataSet(Collection)}
     */
    @TerminalOp
    public DataSet toDataSet() throws E {
        return N.newDataSet(toList());
    }

    /**
     * If the specified {@code columnNames} is null or empty, the first row will be used as column names if its type is array or list,
     * or obtain the column names from first row if its type is entity or map.
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
     * @param func
     * @return
     * @throws E the e
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
     * @param func
     * @return
     * @throws E the e
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
     * @param func
     * @return
     * @throws E the e
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
     * @param func
     * @return
     * @throws E the e
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
     * @param func
     * @return
     * @throws E the e
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
     * @param func
     * @return
     * @throws E the e
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
     * @param <A>
     * @param collector
     * @return
     * @throws E the e
     */
    @TerminalOp
    public <R, A> R collect(final Collector<? super T, A, R> collector) throws E {
        assertNotClosed();

        checkArgNotNull(collector, "collector");

        try {
            final A container = collector.supplier().get();
            final java.util.function.BiConsumer<A, ? super T> accumulator = collector.accumulator();

            while (elements.hasNext()) {
                accumulator.accept(container, elements.next());
            }

            return collector.finisher().apply(container);
        } finally {
            close();
        }
    }

    /**
     * Collect and then.
     *
     * @param <R>
     * @param <RR>
     * @param <A>
     * @param <E2>
     * @param collector
     * @param func
     * @return
     * @throws E the e
     * @throws E2 the e2
     */
    @TerminalOp
    public <R, RR, A, E2 extends Exception> RR collectAndThen(final Collector<? super T, A, R> collector,
            final Throwables.Function<? super R, ? extends RR, E2> func) throws E, E2 {
        assertNotClosed();

        checkArgNotNull(collector, "collector");
        checkArgNotNull(func, "func");

        return func.apply(collect(collector));
    }

    @TerminalOp
    public String join(final CharSequence delimiter) throws E {
        return join(delimiter, "", "");
    }

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

    private static final Throwables.Function<Object, String, IOException> TO_LINE_OF_STRING = N::stringOf;

    @TerminalOp
    public long persist(final File file) throws E, IOException {
        return persist(TO_LINE_OF_STRING, file);
    }

    @TerminalOp
    public long persist(final String header, final String tail, final File file) throws E, IOException {
        return persist(TO_LINE_OF_STRING, header, tail, file);
    }

    /**
     * toCSV:
     * <pre>
     * final JSONSerializationConfig jsc = JSC.create().setBracketRootValue(false);
     * final Throwables.Function<? super T, String, IOException> toLine = it -> N.toJSON(it, jsc);
     * stream.persist(toLine, header, outputFile);
     * </pre>
     *
     * @param toLine
     * @param file
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persist(final Throwables.Function<? super T, String, IOException> toLine, final File file) throws E, IOException {
        return persist(toLine, null, null, file);
    }

    @TerminalOp
    public long persist(final Throwables.Function<? super T, String, IOException> toLine, final String header, final String tail, final File file)
            throws E, IOException {
        assertNotClosed();

        final Writer writer = IOUtil.newFileWriter(file);

        try {
            return persist(toLine, header, tail, writer);
        } finally {
            IOUtil.close(writer);
        }
    }

    @TerminalOp
    public long persist(final Throwables.Function<? super T, String, IOException> toLine, final OutputStream os) throws E, IOException {
        assertNotClosed();

        final BufferedWriter bw = Objectory.createBufferedWriter(os);

        try {
            return persist(toLine, bw);
        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     * toCSV:
     * <pre>
     * final JSONSerializationConfig jsc = JSC.create().setBracketRootValue(false);
     * final Throwables.Function<? super T, String, IOException> toLine = it -> N.toJSON(it, jsc);
     * stream.persist(toLine, header, outputFile);
     * </pre>
     *
     * @param toLine
     * @param writer
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persist(Throwables.Function<? super T, String, IOException> toLine, Writer writer) throws E, IOException {
        assertNotClosed();

        return persist(toLine, null, null, writer);
    }

    @TerminalOp
    public long persist(Throwables.Function<? super T, String, IOException> toLine, String header, String tail, Writer writer) throws E, IOException {
        assertNotClosed();

        try {
            boolean isBufferedWriter = writer instanceof BufferedWriter || writer instanceof java.io.BufferedWriter;
            final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer);
            final ExceptionalIterator<T, E> iter = iteratorEx();
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

    @TerminalOp
    public long persist(final Throwables.BiConsumer<? super T, Writer, IOException> writeLine, final File file) throws E, IOException {
        assertNotClosed();

        return persist(writeLine, null, null, file);
    }

    @TerminalOp
    public long persist(final Throwables.BiConsumer<? super T, Writer, IOException> writeLine, final String header, final String tail, final File file)
            throws E, IOException {
        assertNotClosed();

        final Writer writer = IOUtil.newFileWriter(file);

        try {
            return persist(writeLine, header, tail, writer);
        } finally {
            IOUtil.close(writer);
        }
    }

    @TerminalOp
    public long persist(final Throwables.BiConsumer<? super T, Writer, IOException> writeLine, final Writer writer) throws E, IOException {
        assertNotClosed();

        return persist(writeLine, null, null, writer);
    }

    @TerminalOp
    public long persist(final Throwables.BiConsumer<? super T, Writer, IOException> writeLine, final String header, final String tail, final Writer writer)
            throws E, IOException {
        assertNotClosed();

        try {
            boolean isBufferedWriter = writer instanceof BufferedWriter || writer instanceof java.io.BufferedWriter;
            final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer);
            final ExceptionalIterator<T, E> iter = iteratorEx();
            long cnt = 0;

            try {
                if (header != null) {
                    bw.write(header);
                    bw.write(IOUtil.LINE_SEPARATOR);
                }

                while (iter.hasNext()) {
                    writeLine.accept(iter.next(), writer);
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

    @TerminalOp
    public long persist(final Connection conn, final String insertSQL, final int batchSize, final int batchInterval,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) throws E, SQLException {
        assertNotClosed();

        PreparedStatement stmt = null;

        try {
            stmt = conn.prepareStatement(insertSQL);

            return persist(stmt, batchSize, batchInterval, stmtSetter);
        } finally {
            IOUtil.closeQuietly(stmt);
        }
    }

    @TerminalOp
    public long persist(final PreparedStatement stmt, final int batchSize, final int batchInterval,
            final Throwables.BiConsumer<? super T, ? super PreparedStatement, SQLException> stmtSetter) throws E, SQLException {
        assertNotClosed();

        checkArgument(batchSize > 0 && batchInterval >= 0, "'batchSize'=%s must be greater than 0 and 'batchInterval'=%s can't be negative", batchSize,
                batchInterval);

        try {
            final ExceptionalIterator<T, E> iter = iteratorEx();
            long cnt = 0;
            while (iter.hasNext()) {
                stmtSetter.accept(iter.next(), stmt);

                stmt.addBatch();

                if ((++cnt % batchSize) == 0) {
                    executeBatch(stmt);

                    if (batchInterval > 0) {
                        N.sleep(batchInterval);
                    }
                }
            }

            if ((cnt % batchSize) > 0) {
                executeBatch(stmt);
            }

            return cnt;
        } finally {
            close();
        }
    }

    private int[] executeBatch(final PreparedStatement stmt) throws SQLException {
        try {
            return stmt.executeBatch();
        } finally {
            try {
                stmt.clearBatch();
            } catch (SQLException e) {
                logger.error("Failed to clear batch parameters after executeBatch", e);
            }
        }
    }

    /**
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     *
     * @param file
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persistToCSV(File file) throws E, IOException {
        final Writer writer = IOUtil.newFileWriter(file);

        try {
            return persistToCSV(writer);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     *
     * @param csvHeaders
     * @param file
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persistToCSV(Collection<String> csvHeaders, File file) throws E, IOException {
        final Writer writer = IOUtil.newFileWriter(file);

        try {
            return persistToCSV(csvHeaders, writer);
        } finally {
            IOUtil.close(writer);
        }
    }

    /**
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     *
     * @param os
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persistToCSV(OutputStream os) throws E, IOException {
        final BufferedWriter bw = Objectory.createBufferedWriter(os);

        try {
            return persistToCSV(bw);
        } finally {
            IOUtil.close(bw);
        }
    }

    /**
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     *
     * @param csvHeaders
     * @param os
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persistToCSV(Collection<String> csvHeaders, OutputStream os) throws E, IOException {
        final BufferedWriter bw = Objectory.createBufferedWriter(os);

        try {
            return persistToCSV(csvHeaders, bw);
        } finally {
            IOUtil.close(bw);
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
        config.setQuoteMapKey(true);
        config.setQuotePropName(true);

        WRITE_CSV_ELEMENT_WITH_TYPE = (type, element, bw) -> {
            if (element == null) {
                bw.write(Strings.NULL_CHAR_ARRAY);
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
                bw.write(Strings.NULL_CHAR_ARRAY);
            } else {
                WRITE_CSV_ELEMENT_WITH_TYPE.accept(N.typeOf(element.getClass()), element, bw);
            }
        };

        WRITE_CSV_STRING = (str, bw) -> strType.writeCharacter(bw, str, config);
    }

    /**
     * Each line in the output file/Writer is an array of JSON String without root bracket.
     *
     * @param writer
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persistToCSV(Writer writer) throws E, IOException {
        assertNotClosed();

        try {
            final boolean isBufferedWriter = writer instanceof BufferedJSONWriter;
            final BufferedJSONWriter bw = isBufferedWriter ? (BufferedJSONWriter) writer : Objectory.createBufferedJSONWriter(writer);

            final ExceptionalIterator<T, E> iter = iteratorEx();
            long cnt = 0;
            T next = null;
            Class<?> cls = null;

            try {
                if (iter.hasNext()) {
                    next = iter.next();
                    cnt++;
                    cls = next.getClass();

                    if (ClassUtil.isEntity(cls)) {
                        final List<PropInfo> propInfoList = ParserUtil.getEntityInfo(cls).propInfoList;
                        final int headerSize = propInfoList.size();
                        PropInfo propInfo = null;

                        for (int i = 0; i < headerSize; i++) {
                            if (i > 0) {
                                bw.write(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_STRING.accept(propInfoList.get(i).name, bw);
                        }

                        bw.write(IOUtil.LINE_SEPARATOR);

                        for (int i = 0; i < headerSize; i++) {
                            propInfo = propInfoList.get(i);

                            if (i > 0) {
                                bw.write(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
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
                                    bw.write(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
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
                                bw.write(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_ELEMENT.accept(keys.get(i), bw);
                        }

                        bw.write(IOUtil.LINE_SEPARATOR);

                        for (int i = 0; i < headerSize; i++) {
                            if (i > 0) {
                                bw.write(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_ELEMENT.accept(row.get(keys.get(i)), bw);
                        }

                        while (iter.hasNext()) {
                            row = (Map<Object, Object>) iter.next();
                            cnt++;

                            bw.write(IOUtil.LINE_SEPARATOR);

                            for (int i = 0; i < headerSize; i++) {
                                if (i > 0) {
                                    bw.write(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
                                }

                                WRITE_CSV_ELEMENT.accept(row.get(keys.get(i)), bw);
                            }
                        }
                    } else {
                        throw new RuntimeException(cls + " is no supported for CSV format. Only entity/Map are supported");
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
     *
     * @param headers
     * @param writer
     * @return
     * @throws E
     * @throws IOException
     */
    @TerminalOp
    public long persistToCSV(Collection<String> csvHeaders, Writer writer) throws E, IOException {
        checkArgNotNullOrEmpty(csvHeaders, "csvHeaders");
        assertNotClosed();

        try {
            List<String> headers = new ArrayList<>(csvHeaders);
            final boolean isBufferedWriter = writer instanceof BufferedJSONWriter;
            final BufferedJSONWriter bw = isBufferedWriter ? (BufferedJSONWriter) writer : Objectory.createBufferedJSONWriter(writer);

            final int headSize = headers.size();
            final ExceptionalIterator<T, E> iter = iteratorEx();
            long cnt = 0;
            T next = null;
            Class<?> cls = null;

            try {
                if (iter.hasNext()) {
                    next = iter.next();
                    cnt++;
                    cls = next.getClass();

                    if (ClassUtil.isEntity(cls)) {
                        final EntityInfo entityInfo = ParserUtil.getEntityInfo(cls);
                        final PropInfo[] propInfos = new PropInfo[headSize];
                        PropInfo propInfo = null;

                        for (int i = 0; i < headSize; i++) {
                            propInfos[i] = entityInfo.getPropInfo(headers.get(i));
                        }

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_STRING.accept(headers.get(i), bw);
                        }

                        bw.write(IOUtil.LINE_SEPARATOR);

                        for (int i = 0; i < headSize; i++) {
                            propInfo = propInfos[i];

                            if (i > 0) {
                                bw.write(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
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
                                    bw.write(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
                                }

                                WRITE_CSV_ELEMENT_WITH_TYPE.accept(propInfo.jsonXmlType, propInfo.getPropValue(next), bw);
                            }
                        }
                    } else if (next instanceof Map) {
                        Map<Object, Object> row = (Map<Object, Object>) next;

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_STRING.accept(headers.get(i), bw);
                        }

                        bw.write(IOUtil.LINE_SEPARATOR);

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_ELEMENT.accept(row.get(headers.get(i)), bw);
                        }

                        while (iter.hasNext()) {
                            row = (Map<Object, Object>) iter.next();
                            cnt++;

                            bw.write(IOUtil.LINE_SEPARATOR);

                            for (int i = 0; i < headSize; i++) {
                                if (i > 0) {
                                    bw.write(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
                                }

                                WRITE_CSV_ELEMENT.accept(row.get(headers.get(i)), bw);
                            }
                        }
                    } else if (next instanceof Collection) {
                        Collection<Object> row = (Collection<Object>) next;

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_STRING.accept(headers.get(i), bw);
                        }

                        bw.write(IOUtil.LINE_SEPARATOR);

                        Iterator<Object> rowIter = row.iterator();

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
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
                                    bw.write(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
                                }

                                WRITE_CSV_ELEMENT.accept(rowIter.next(), bw);
                            }
                        }
                    } else if (next instanceof Object[]) {
                        Object[] row = (Object[]) next;

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_STRING.accept(headers.get(i), bw);
                        }

                        bw.write(IOUtil.LINE_SEPARATOR);

                        for (int i = 0; i < headSize; i++) {
                            if (i > 0) {
                                bw.write(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
                            }

                            WRITE_CSV_ELEMENT.accept(row[i], bw);
                        }

                        while (iter.hasNext()) {
                            row = (Object[]) iter.next();
                            cnt++;

                            bw.write(IOUtil.LINE_SEPARATOR);

                            for (int i = 0; i < headSize; i++) {
                                if (i > 0) {
                                    bw.write(Strings.ELEMENT_SEPARATOR_CHAR_ARRAY);
                                }

                                WRITE_CSV_ELEMENT.accept(row[i], bw);
                            }
                        }
                    } else {
                        throw new RuntimeException(cls + " is no supported for CSV format. Only entity/Map are supported");
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

    @Beta
    @TerminalOp
    public void println() throws E {
        N.println(join(", ", "[", "]"));
    }

    @IntermediateOp
    public Stream<T> unchecked() {
        assertNotClosed();

        if (N.isNullOrEmpty(this.closeHandlers)) {
            return Stream.of(newObjIteratorEx(elements));
        } else {
            return Stream.of(newObjIteratorEx(elements)).onClose(() -> {
                try {
                    ExceptionalStream.this.close();
                } catch (Exception e) {
                    throw ExceptionUtil.toRuntimeException(e);
                }
            });
        }
    }

    @IntermediateOp
    public java.util.stream.Stream<T> toJdkStream() {
        assertNotClosed();

        final Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(newObjIteratorEx(elements), Spliterator.ORDERED | Spliterator.IMMUTABLE);

        if (isEmptyCloseHandlers(closeHandlers)) {
            return StreamSupport.stream(spliterator, false);
        } else {
            return StreamSupport.stream(spliterator, false).onClose(() -> {
                try {
                    ExceptionalStream.this.close();
                } catch (Exception e) {
                    throw ExceptionUtil.toRuntimeException(e);
                }
            });
        }
    }

    //    public <E2 extends Exception> ExceptionalStream<T, E2> __(final Class<E2> targetExceptionType) {
    //        checkArgNotNull(targetExceptionType, "targetExceptionType");
    //
    //        final Constructor<E2> msgCauseConstructor = ClassUtil.getDeclaredConstructor(targetExceptionType, String.class, Throwable.class);
    //        final Constructor<E2> causeOnlyConstructor = ClassUtil.getDeclaredConstructor(targetExceptionType, Throwable.class);
    //
    //        checkArgument(msgCauseConstructor != null || causeOnlyConstructor != null,
    //                "No constructor found with parameters: (String.class, Throwable.class), or (Throwable.class)");
    //
    //        final Function<Exception, E2> convertE = msgCauseConstructor != null ? new Function<Exception, E2>() {
    //            @Override
    //            public E2 apply(Exception e) {
    //                return ClassUtil.invokeConstructor(msgCauseConstructor, e.getMessage(), e);
    //            }
    //        } : new Function<Exception, E2>() {
    //            @Override
    //            public E2 apply(Exception e) {
    //                return ClassUtil.invokeConstructor(causeOnlyConstructor, e);
    //            }
    //        };
    //
    //        Deque<Throwables.Runnable<E2>> newCloseHandlers = null;
    //
    //        if (closeHandlers != null) {
    //            newCloseHandlers = new ArrayDeque<>(1);
    //            newCloseHandlers.add(new Throwables.Runnable<E2>() {
    //                @Override
    //                public void run() throws E2 {
    //                    try {
    //                        close();
    //                    } catch (Exception e) {
    //                        throw convertE.apply(e);
    //                    }
    //                }
    //            });
    //        }
    //
    //        return newStream(new ExceptionalIterator<T, E2>() {
    //            private ExceptionalIterator<T, E> iter = null;
    //            private boolean initialized = false;
    //
    //            @Override
    //            public boolean hasNext() throws E2 {
    //                if (initialized == false) {
    //                    init();
    //                }
    //
    //                try {
    //                    return iter.hasNext();
    //                } catch (Exception e) {
    //                    throw convertE.apply(e);
    //                }
    //            }
    //
    //            @Override
    //            public T next() throws E2 {
    //                if (initialized == false) {
    //                    init();
    //                }
    //
    //                try {
    //                    return iter.next();
    //                } catch (Exception e) {
    //                    throw convertE.apply(e);
    //                }
    //            }
    //
    //                @Override
    //                public void skip(long n) throws E2 {
    //                    checkArgNotNegative(n, "n");
    //
    //                    if (initialized == false) {
    //                        init();
    //                    }
    //
    //                    try {
    //                        iter.skip(n);
    //                    } catch (Exception e) {
    //                        throw convertE.apply(e);
    //                    }
    //                }
    //
    //            @Override
    //            public long count() throws E2 {
    //                if (initialized == false) {
    //                    init();
    //                }
    //
    //                try {
    //                    return iter.count();
    //                } catch (Exception e) {
    //                    throw convertE.apply(e);
    //                }
    //            }
    //
    //            private void init() {
    //                if (initialized == false) {
    //                    initialized = true;
    //
    //                    iter = ExceptionalStream.this.elements;
    //
    //                }
    //            }
    //        }, sorted, comparator, newCloseHandlers);
    //    }

    @Beta
    @IntermediateOp
    public ExceptionalStream<T, Exception> cast() {
        assertNotClosed();

        return (ExceptionalStream<T, Exception>) this;
    }

    /**
     *
     * @param <SS>
     * @param transfer
     * @return
     * @throws E the e
     */
    @Beta
    @IntermediateOp
    public <TT, EE extends Exception> ExceptionalStream<TT, EE> __(Function<? super ExceptionalStream<T, E>, ExceptionalStream<TT, EE>> transfer) {
        assertNotClosed();

        checkArgNotNull(transfer, "transfer");

        return transfer.apply(this);
    }

    //    /**
    //     *
    //     * @param <U>
    //     * @param <R>
    //     * @param terminalOp should be terminal operation.
    //     * @param mapper
    //     * @return
    //     */
    //    @TerminalOp
    //    @Beta
    //    public <U, R> R __(final Function<? super ExceptionalStream<T, E>, U> terminalOp, final Throwables.Function<U, R, E> mapper) throws E {
    //        return mapper.apply(terminalOp.apply(this));
    //    }
    //
    //    /**
    //     *
    //     * @param <R>
    //     * @param terminalOp should be terminal operation.
    //     * @param action
    //     * @return
    //     */
    //    @TerminalOp
    //    @Beta
    //    public <R> R __(final Function<? super ExceptionalStream<T, E>, R> terminalOp, final Throwables.Consumer<R, E> action) throws E {
    //        final R result = terminalOp.apply(this);
    //        action.accept(result);
    //        return result;
    //    }

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
    public <R> ExceptionalStream<R, E> sps(final Function<? super Stream<T>, ? extends Stream<? extends R>> ops) {
        assertNotClosed();

        return checked(((Stream<R>) ops.apply(this.unchecked().parallel())), true);
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
    public <R> ExceptionalStream<R, E> sps(final int maxThreadNum, final Function<? super Stream<T>, ? extends Stream<? extends R>> ops) {
        assertNotClosed();

        return checked(((Stream<R>) ops.apply(this.unchecked().parallel(maxThreadNum))), true);
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
    //    public <R> ExceptionalStream<R, E> sps(final int maxThreadNum, final boolean withVirtualThread,
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
    //    public <R> ExceptionalStream<R, E> sps(final int maxThreadNum, final int executorNumForVirtualThread,
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
    public ExceptionalStream<T, E> spsFilter(final Throwables.Predicate<? super T, E> predicate) {
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
    public <R> ExceptionalStream<R, E> spsMap(final Throwables.Function<? super T, ? extends R, E> mapper) {
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
    public <R> ExceptionalStream<R, E> spsFlatMap(final Throwables.Function<? super T, ? extends Stream<? extends R>, E> mapper) {
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
    public <R> ExceptionalStream<R, E> spsFlatmap(final Throwables.Function<? super T, ? extends Collection<? extends R>, E> mapper) {
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
    public ExceptionalStream<T, E> spsOnEach(final Throwables.Consumer<? super T, E> action) {
        final Function<Stream<T>, Stream<T>> ops = s -> s.onEach(Fn.cc(action));

        return sps(ops);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code filter} and then switch back to sequence stream.
     * <br />
     *
     *
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
    public ExceptionalStream<T, E> spsFilter(final int maxThreadNum, final Throwables.Predicate<? super T, E> predicate) {
        final Function<Stream<T>, Stream<T>> ops = s -> s.filter(Fn.pp(predicate));

        return sps(maxThreadNum, ops);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code map} and then switch back to sequence stream.
     * <br />
     *
     *
     * @param <R>
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
    public <R> ExceptionalStream<R, E> spsMap(final int maxThreadNum, final Throwables.Function<? super T, ? extends R, E> mapper) {
        final Function<Stream<T>, Stream<R>> ops = s -> s.map(Fn.ff(mapper));

        return sps(maxThreadNum, ops);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
     * <br />
     *
     *
     * @param <R>
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
    public <R> ExceptionalStream<R, E> spsFlatMap(final int maxThreadNum, final Throwables.Function<? super T, ? extends Stream<? extends R>, E> mapper) {
        final Function<Stream<T>, Stream<R>> ops = s -> s.flatMap(Fn.ff(mapper));

        return sps(maxThreadNum, ops);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code flatMap} and then switch back to sequence stream.
     * <br />
     *
     *
     * @param <R>
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
    public <R> ExceptionalStream<R, E> spsFlatmap(final int maxThreadNum, final Throwables.Function<? super T, ? extends Collection<? extends R>, E> mapper) {
        final Function<Stream<T>, Stream<R>> ops = s -> s.flatmap(Fn.ff(mapper));

        return sps(maxThreadNum, ops);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code onEach} and then switch back to sequence stream.
     * <br />
     *
     *
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
    public ExceptionalStream<T, E> spsOnEach(final int maxThreadNum, final Throwables.Consumer<? super T, E> action) {
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
    public ExceptionalStream<T, Exception> spsFilterE(final Throwables.Predicate<? super T, ? extends Exception> predicate) {
        return checked(unchecked().spsFilterE(predicate), true);
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
    public <U> ExceptionalStream<U, Exception> spsMapE(final Throwables.Function<? super T, ? extends U, ? extends Exception> mapper) {
        return checked(unchecked().<U> spsMapE(mapper), true);
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
    public <R> ExceptionalStream<R, Exception> spsFlatMapE(final Throwables.Function<? super T, ? extends Stream<? extends R>, ? extends Exception> mapper) {
        return checked(unchecked().<R> spsFlatMapE(mapper), true);
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
    public <R> ExceptionalStream<R, Exception> spsFlatmapE(
            final Throwables.Function<? super T, ? extends Collection<? extends R>, ? extends Exception> mapper) {
        return checked(unchecked().<R> spsFlatmapE(mapper), true);
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
    public ExceptionalStream<T, Exception> spsOnEachE(final Throwables.Consumer<? super T, ? extends Exception> action) {
        return checked(unchecked().spsOnEachE(action), true);
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
    public ExceptionalStream<T, Exception> spsFilterE(final int maxThreadNum, final Throwables.Predicate<? super T, ? extends Exception> predicate) {
        return checked(unchecked().spsFilterE(maxThreadNum, predicate), true);
    }

    /**
     * Temporarily switch the stream to parallel stream for operation {@code map} and then switch back to sequence stream.
     * <br />
     *
     *
     * @param <R>
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
    public <U> ExceptionalStream<U, Exception> spsMapE(final int maxThreadNum, final Throwables.Function<? super T, ? extends U, ? extends Exception> mapper) {
        return checked(unchecked().<U> spsMapE(maxThreadNum, mapper), true);
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
    public <R> ExceptionalStream<R, Exception> spsFlatMapE(final int maxThreadNum,
            final Throwables.Function<? super T, ? extends Stream<? extends R>, ? extends Exception> mapper) {
        return checked(unchecked().<R> spsFlatMapE(maxThreadNum, mapper), true);
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
    public <R> ExceptionalStream<R, Exception> spsFlatmapE(final int maxThreadNum,
            final Throwables.Function<? super T, ? extends Collection<? extends R>, ? extends Exception> mapper) {
        return checked(unchecked().<R> spsFlatmapE(maxThreadNum, mapper), true);
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
    public ExceptionalStream<T, Exception> spsOnEachE(final int maxThreadNum, final Throwables.Consumer<? super T, ? extends Exception> action) {
        return checked(unchecked().spsOnEachE(maxThreadNum, action), true);
    }

    static <T, E extends Exception> ExceptionalStream<T, E> checked(final Stream<? extends T> stream, final boolean isForSps) {
        if (stream == null) {
            return empty();
        }

        ExceptionalIterator<T, E> iter = null;

        if (isForSps) {
            iter = new ExceptionalIterator<>() {
                private Stream<? extends T> s = stream;
                private Iterator<? extends T> iter = null;
                private boolean isInitialized = false;

                @Override
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

                @Override
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

                @Override
                public void advance(long n) throws E {
                    try {
                        if (iter == null) {
                            s = s.skip(n);
                        } else {
                            super.advance(n);
                        }
                    } catch (Exception e) {
                        throw (E) ExceptionUtil.tryToGetOriginalCheckedException(e);
                    }
                }

                @Override
                public long count() throws E {
                    try {
                        if (iter == null) {
                            return s.count();
                        } else {
                            return super.count();
                        }
                    } catch (Exception e) {
                        throw (E) ExceptionUtil.tryToGetOriginalCheckedException(e);
                    }
                }

                @Override
                public void close() throws E {
                    try {
                        s.close();
                    } catch (Exception e) {
                        throw (E) ExceptionUtil.tryToGetOriginalCheckedException(e);
                    }
                }

                private void init() {
                    if (!isInitialized) {
                        isInitialized = true;
                        iter = s.iterator();
                    }
                }
            };
        } else {
            iter = new ExceptionalIterator<>() {
                private Stream<? extends T> s = stream;
                private Iterator<? extends T> iter = null;
                private boolean isInitialized = false;

                @Override
                public boolean hasNext() throws E {
                    if (!isInitialized) {
                        init();
                    }

                    return iter.hasNext();
                }

                @Override
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
                        super.advance(n);
                    }
                }

                @Override
                public long count() throws E {
                    if (iter == null) {
                        return s.count();
                    } else {
                        return super.count();
                    }
                }

                @Override
                public void close() throws E {
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

        final ExceptionalIterator<T, E> tmp = iter;

        return newStream(tmp).onClose(newCloseHandler(tmp));
    }

    /**
     *
     * @param predicate
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Beta
    @IntermediateOp
    public ExceptionalStream<T, Exception> filterE(final Throwables.Predicate<? super T, ? extends Exception> predicate) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<T, Exception>() {
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
                    throw new NoSuchElementException();
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
    public <U> ExceptionalStream<U, Exception> mapE(final Throwables.Function<? super T, ? extends U, ? extends Exception> mapper) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<U, Exception>() {
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
    public <R> ExceptionalStream<R, Exception> flatMapE(
            final Throwables.Function<? super T, ? extends ExceptionalStream<? extends R, ? extends Exception>, ? extends Exception> mapper) {
        assertNotClosed();

        final ExceptionalIterator<R, Exception> iter = new ExceptionalIterator<>() {
            private ExceptionalIterator<? extends R, ? extends Exception> cur = null;
            private ExceptionalStream<? extends R, ? extends Exception> s = null;
            private Deque<? extends Throwables.Runnable<? extends Exception>> closeHandle = null;

            @Override
            public boolean hasNext() throws Exception {
                while (cur == null || !cur.hasNext()) {
                    if (elements.hasNext()) {
                        if (closeHandle != null) {
                            final Deque<? extends Throwables.Runnable<? extends Exception>> tmp = closeHandle;
                            closeHandle = null;
                            ExceptionalStream.close(tmp);
                        }

                        s = mapper.apply(elements.next());

                        if (s == null) {
                            cur = null;
                        } else {
                            if (N.notNullOrEmpty(s.closeHandlers)) {
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

            @Override
            public R next() throws Exception {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException();
                }

                return cur.next();
            }

            @Override
            public void close() throws Exception {
                if (closeHandle != null) {
                    ExceptionalStream.close(closeHandle);
                }
            }
        };

        final Deque<Throwables.Runnable<? extends Exception>> newCloseHandlers = new ArrayDeque<>(N.size(closeHandlers) + 1);

        newCloseHandlers.add(newCloseHandler(iter));

        if (N.notNullOrEmpty(closeHandlers)) {
            newCloseHandlers.addAll(closeHandlers);
        }

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
    public <R> ExceptionalStream<R, Exception> flatmapE(final Throwables.Function<? super T, ? extends Collection<? extends R>, ? extends Exception> mapper) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<R, Exception>() {
            private Iterator<? extends R> cur = null;
            private Collection<? extends R> c = null;

            @Override
            public boolean hasNext() throws Exception {
                while ((cur == null || !cur.hasNext()) && elements.hasNext()) {
                    c = mapper.apply(elements.next());
                    cur = N.isNullOrEmpty(c) ? null : c.iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public R next() throws Exception {
                if ((cur == null || !cur.hasNext()) && !hasNext()) {
                    throw new NoSuchElementException();
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
    public ExceptionalStream<T, Exception> onEachE(final Throwables.Consumer<? super T, ? extends Exception> action) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<T, Exception>() {
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

    // #################################################################################################################################
    // #################################################################################################################################

    /**
     *
     *
     * @param terminalAction a terminal operation should be called.
     * @return
     */
    @Beta
    @TerminalOp
    public ContinuableFuture<Void> asyncRun(final Throwables.Consumer<? super ExceptionalStream<T, E>, ? extends Exception> terminalAction) {
        assertNotClosed();

        checkArgNotNull(terminalAction, "terminalAction");

        return ContinuableFuture.run(() -> terminalAction.accept(ExceptionalStream.this));
    }

    /**
     *
     * @param terminalAction a terminal operation should be called.
     * @param executor
     * @return
     */
    @Beta
    @TerminalOp
    public ContinuableFuture<Void> asyncRun(final Throwables.Consumer<? super ExceptionalStream<T, E>, ? extends Exception> terminalAction,
            final Executor executor) {
        assertNotClosed();

        checkArgNotNull(terminalAction, "terminalAction");
        checkArgNotNull(executor, "executor");

        return ContinuableFuture.run(() -> terminalAction.accept(ExceptionalStream.this), executor);
    }

    /**
     *
     * @param <R>
     * @param terminalAction a terminal operation should be called.
     * @return
     */
    @Beta
    @TerminalOp
    public <R> ContinuableFuture<R> asyncCall(final Throwables.Function<? super ExceptionalStream<T, E>, R, ? extends Exception> terminalAction) {
        assertNotClosed();

        checkArgNotNull(terminalAction, "terminalAction");

        return ContinuableFuture.call(() -> terminalAction.apply(ExceptionalStream.this));
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
    public <R> ContinuableFuture<R> asyncCall(final Throwables.Function<? super ExceptionalStream<T, E>, R, ? extends Exception> terminalAction,
            final Executor executor) {
        assertNotClosed();

        checkArgNotNull(terminalAction, "terminalAction");
        checkArgNotNull(executor, "executor");

        return ContinuableFuture.call(() -> terminalAction.apply(ExceptionalStream.this), executor);
    }

    /**
     *
     * @param closeHandler
     * @return
     */
    @IntermediateOp
    public ExceptionalStream<T, E> onClose(final Throwables.Runnable<? extends E> closeHandler) {
        assertNotClosed();

        checkArgNotNull(closeHandler, "closeHandler");

        final Deque<Throwables.Runnable<? extends E>> newCloseHandlers = new ArrayDeque<>(N.size(closeHandlers) + 1);

        newCloseHandlers.add(new Throwables.Runnable<E>() {
            private volatile boolean isClosed = false;

            @Override
            public void run() throws E {
                if (isClosed) {
                    return;
                }

                isClosed = true;
                closeHandler.run();
            }
        });

        if (N.notNullOrEmpty(this.closeHandlers)) {
            newCloseHandlers.addAll(this.closeHandlers);
        }

        return newStream(elements, newCloseHandlers);
    }

    /**
     *
     * It will be called by terminal operations in final.
     *
     * @throws E the e
     */
    @TerminalOp
    @Override
    public synchronized void close() {
        if (isClosed) {
            return;
        }

        if (isEmptyCloseHandlers(closeHandlers)) {
            if (N.notNullOrEmpty(closeHandlers)) {
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

        logger.debug("Closing ExceptionalStream");

        close(closeHandlers);

        if (N.notNullOrEmpty(closeHandlers)) {
            closeHandlers.clear();
        }
    }

    static boolean isEmptyCloseHandlers(final Collection<? extends Throwables.Runnable<?>> closeHandlers) {
        return N.isNullOrEmpty(closeHandlers) || (closeHandlers.size() == 1 && N.firstOrNullIfEmpty(closeHandlers) == EMPTY_CLOSE_HANDLER);
    }

    @SuppressWarnings("rawtypes")
    private static final Throwables.Runnable EMPTY_CLOSE_HANDLER = () -> {
        // do nothing.
    };

    private static <E extends Exception, T> Throwables.Runnable<E> newCloseHandler(final ExceptionalIterator<T, E> iter) {
        if (iter == null) {
            return EMPTY_CLOSE_HANDLER;
        }

        return iter::close;
    }

    static <E extends Exception> void close(final Deque<? extends Throwables.Runnable<? extends E>> closeHandlers) {
        Throwable ex = null;

        for (Throwables.Runnable<? extends E> closeHandler : closeHandlers) {
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
            throw ExceptionUtil.toRuntimeException(ex);
        }
    }

    ExceptionalIterator<T, E> iteratorEx() {
        return elements;
    }

    /**
     * Assert not closed.
     */
    void assertNotClosed() {
        if (isClosed) {
            throw new IllegalStateException("This stream has been closed");
        }
    }

    /**
     * Check arg positive.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     */
    private int checkArgPositive(final int arg, final String argNameOrErrorMsg) {
        if (arg <= 0) {
            try {
                N.checkArgPositive(arg, argNameOrErrorMsg);
            } finally {
                try {
                    close();
                } catch (Exception e) {
                    throw ExceptionUtil.toRuntimeException(e);
                }
            }
        }

        return arg;
    }

    private long checkArgPositive(final long arg, final String argNameOrErrorMsg) {
        if (arg <= 0) {
            try {
                N.checkArgPositive(arg, argNameOrErrorMsg);
            } finally {
                try {
                    close();
                } catch (Exception e) {
                    throw ExceptionUtil.toRuntimeException(e);
                }
            }
        }

        return arg;
    }

    /**
     * Check arg not negative.
     *
     * @param arg
     * @param argNameOrErrorMsg
     * @return
     */
    private long checkArgNotNegative(final long arg, final String argNameOrErrorMsg) {
        if (arg < 0) {
            try {
                N.checkArgNotNegative(arg, argNameOrErrorMsg);
            } finally {
                try {
                    close();
                } catch (Exception e) {
                    throw ExceptionUtil.toRuntimeException(e);
                }
            }
        }

        return arg;
    }

    /**
     * Check arg not null.
     *
     * @param <ARG>
     * @param obj
     * @param errorMessage
     * @return
     */
    private <ARG> ARG checkArgNotNull(final ARG obj, final String errorMessage) {
        if (obj == null) {
            try {
                N.checkArgNotNull(obj, errorMessage);
            } finally {
                try {
                    close();
                } catch (Exception e) {
                    throw ExceptionUtil.toRuntimeException(e);
                }
            }
        }

        return obj;
    }

    @SuppressWarnings("rawtypes")
    <ARG extends Collection> ARG checkArgNotNullOrEmpty(final ARG obj, final String errorMessage) {
        if (obj == null || obj.size() == 0) {
            try {
                N.checkArgNotNullOrEmpty(obj, errorMessage);
            } finally {
                try {
                    close();
                } catch (Exception e) {
                    throw ExceptionUtil.toRuntimeException(e);
                }
            }
        }

        return obj;
    }

    /**
     *
     * @param b
     * @param errorMessage
     */
    private void checkArgument(boolean b, String errorMessage) {
        if (!b) {
            try {
                N.checkArgument(b, errorMessage);
            } finally {
                try {
                    close();
                } catch (Exception e) {
                    throw ExceptionUtil.toRuntimeException(e);
                }
            }
        }
    }

    /**
     *
     * @param b
     * @param errorMessageTemplate
     * @param p1
     * @param p2
     */
    private void checkArgument(boolean b, String errorMessageTemplate, int p1, int p2) {
        if (!b) {
            try {
                N.checkArgument(b, errorMessageTemplate, p1, p2);
            } finally {
                try {
                    close();
                } catch (Exception e) {
                    throw ExceptionUtil.toRuntimeException(e);
                }
            }
        }
    }

    ObjIteratorEx<T> newObjIteratorEx(final ExceptionalIterator<T, E> elements) {
        return new ObjIteratorEx<>() {
            @Override
            public boolean hasNext() {
                try {
                    return elements.hasNext();
                } catch (Exception e) {
                    throw ExceptionUtil.toRuntimeException(e);
                }
            }

            @Override
            public T next() {
                try {
                    return elements.next();
                } catch (Exception e) {
                    throw ExceptionUtil.toRuntimeException(e);
                }
            }

            @Override
            public void advance(long n) {
                try {
                    elements.advance(n);
                } catch (Exception e) {
                    throw ExceptionUtil.toRuntimeException(e);
                }
            }

            @Override
            public long count() {
                try {
                    return elements.count();
                } catch (Exception e) {
                    throw ExceptionUtil.toRuntimeException(e);
                }
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @return
     */
    static <T, E extends Exception> ExceptionalStream<T, E> newStream(final ExceptionalIterator<T, E> iter) {
        return new ExceptionalStream<>(iter, null);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param closeHandlers
     * @return
     */
    static <T, E extends Exception> ExceptionalStream<T, E> newStream(final ExceptionalIterator<T, E> iter,
            final Deque<Throwables.Runnable<? extends E>> closeHandlers) {
        return new ExceptionalStream<>(iter, closeHandlers);
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param iter
     * @param sorted
     * @param comparator
     * @param closeHandlers
     * @return
     */
    static <T, E extends Exception> ExceptionalStream<T, E> newStream(final ExceptionalIterator<T, E> iter, final boolean sorted,
            final Comparator<? super T> comparator, final Deque<Throwables.Runnable<? extends E>> closeHandlers) {
        return new ExceptionalStream<>(iter, sorted, comparator, closeHandlers);
    }

    /**
     *
     * @param obj
     * @return
     */
    static Object hashKey(Object obj) {
        return obj == null || !obj.getClass().isArray() ? obj : Wrapper.of(obj);
    }

    /**
     * Checks if is same comparator.
     *
     * @param a
     * @param b
     * @return true, if is same comparator
     */
    static boolean isSameComparator(Comparator<?> a, Comparator<?> b) {
        return a == b || (a == null && b == Comparators.NATURAL_ORDER) || (b == null && a == Comparators.NATURAL_ORDER);
    }

    static <T, E extends Exception> ExceptionalIterator<T, E> iterate(final ExceptionalStream<? extends T, E> s) {
        return s == null ? ExceptionalIterator.EMPTY : (ExceptionalIterator<T, E>) s.iteratorEx();
    }

    /**
     * The Class ExceptionalIterator.
     *
     * @param <T>
     * @param <E>
     */
    @Internal
    @com.landawn.abacus.annotation.Immutable
    public static abstract class ExceptionalIterator<T, E extends Exception> implements Immutable {

        /** The Constant EMPTY. */
        @SuppressWarnings("rawtypes")
        private static final ExceptionalIterator EMPTY = new ExceptionalIterator() {
            @Override
            public boolean hasNext() throws Exception {
                return false;
            }

            @Override
            public Object next() throws Exception {
                throw new NoSuchElementException();
            }
        };

        public static <T, E extends Exception> ExceptionalIterator<T, E> empty() {
            return EMPTY;
        }

        /**
         *
         * @param <T>
         * @param val
         * @return
         */
        public static <T, E extends Exception> ExceptionalIterator<T, E> just(final T val) {
            return new ExceptionalIterator<>() {
                private boolean done = false;

                @Override
                public boolean hasNext() {
                    return !done;
                }

                @Override
                public T next() {
                    if (done) {
                        throw new NoSuchElementException();
                    }

                    done = true;

                    return val;
                }
            };
        }

        /**
         *
         * @param <T>
         * @param a
         * @return
         */
        @SafeVarargs
        public static <T, E extends Exception> ExceptionalIterator<T, E> of(final T... a) {
            return N.isNullOrEmpty(a) ? EMPTY : of(a, 0, a.length);
        }

        /**
         *
         * @param <T>
         * @param a
         * @param fromIndex
         * @param toIndex
         * @return
         */
        public static <T, E extends Exception> ExceptionalIterator<T, E> of(final T[] a, final int fromIndex, final int toIndex) {
            N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

            if (fromIndex == toIndex) {
                return EMPTY;
            }

            return new ExceptionalIterator<>() {
                private int cursor = fromIndex;

                @Override
                public boolean hasNext() {
                    return cursor < toIndex;
                }

                @Override
                public T next() {
                    if (cursor >= toIndex) {
                        throw new NoSuchElementException();
                    }

                    return a[cursor++];
                }

                @Override
                public void advance(long n) throws E {
                    if (n > toIndex - cursor) {
                        cursor = toIndex;
                    } else {
                        cursor += n;
                    }
                }

                @Override
                public long count() {
                    return toIndex - cursor;
                }
            };
        }

        /**
         * Lazy evaluation.
         *
         * @param <T>
         * @param <E>
         * @param arraySupplier
         * @return
         */
        static <T, E extends Exception> ExceptionalIterator<T, E> of(final Throwables.Supplier<T[], E> arraySupplier) {
            N.checkArgNotNull(arraySupplier, "arraySupplier");

            return new ExceptionalIterator<>() {
                private T[] a;
                private int len;
                private int position = 0;
                private boolean isInitialized = false;

                @Override
                public boolean hasNext() throws E {
                    if (!isInitialized) {
                        init();
                    }

                    return position < len;
                }

                @Override
                public T next() throws E {
                    if (!isInitialized) {
                        init();
                    }

                    if (position >= len) {
                        throw new NoSuchElementException();
                    }

                    return a[position++];
                }

                @Override
                public long count() throws E {
                    if (!isInitialized) {
                        init();
                    }

                    return len - position;
                }

                @Override
                public void advance(long n) throws E {
                    N.checkArgNotNegative(n, "n");

                    if (!isInitialized) {
                        init();
                    }

                    if (n > len - position) {
                        position = len;
                    } else {
                        position += n;
                    }

                }

                private void init() throws E {
                    if (!isInitialized) {
                        isInitialized = true;
                        a = arraySupplier.get();
                        len = N.len(a);
                    }
                }
            };
        }

        /**
         * Lazy evaluation.
         *
         * @param <T>
         * @param <E>
         * @param iteratorSupplier
         * @return
         */
        static <T, E extends Exception> ExceptionalIterator<T, E> from(final Throwables.Supplier<ExceptionalIterator<T, E>, E> iteratorSupplier) {
            N.checkArgNotNull(iteratorSupplier, "iteratorSupplier");

            return new ExceptionalIterator<>() {
                private ExceptionalIterator<T, E> iter = null;
                private boolean isInitialized = false;

                @Override
                public boolean hasNext() throws E {
                    if (!isInitialized) {
                        init();
                    }

                    return iter.hasNext();
                }

                @Override
                public T next() throws E {
                    if (!isInitialized) {
                        init();
                    }

                    return iter.next();
                }

                @Override
                public void advance(long n) throws E {
                    N.checkArgNotNegative(n, "n");

                    if (!isInitialized) {
                        init();
                    }

                    iter.advance(n);
                }

                @Override
                public long count() throws E {
                    if (!isInitialized) {
                        init();
                    }

                    return iter.count();
                }

                @Override
                public void close() throws E {
                    if (!isInitialized) {
                        init();
                    }

                    iter.close();
                }

                private void init() throws E {
                    if (!isInitialized) {
                        isInitialized = true;
                        iter = iteratorSupplier.get();
                    }
                }
            };
        }

        public static <T, E extends Exception> ExceptionalIterator<T, E> wrap(final Iterator<? extends T> iter) {
            if (iter == null) {
                return EMPTY;
            }

            return new ExceptionalIterator<>() {
                @Override
                public boolean hasNext() throws E {
                    return iter.hasNext();
                }

                @Override
                public T next() throws E {
                    return iter.next();
                }
            };
        }

        public static <T, E extends Exception> ExceptionalIterator<T, E> concat(final ExceptionalIterator<? extends T, ? extends E>... a) {
            return concat(N.asList(a));
        }

        public static <T, E extends Exception> ExceptionalIterator<T, E> concat(final Collection<? extends ExceptionalIterator<? extends T, ? extends E>> c) {
            if (N.isNullOrEmpty(c)) {
                return ExceptionalIterator.empty();
            }

            return new ExceptionalIterator<>() {
                private final Iterator<? extends ExceptionalIterator<? extends T, ? extends E>> iter = c.iterator();
                private ExceptionalIterator<? extends T, ? extends E> cur;

                @Override
                public boolean hasNext() throws E {
                    while ((cur == null || !cur.hasNext()) && iter.hasNext()) {
                        cur = iter.next();
                    }

                    return cur != null && cur.hasNext();
                }

                @Override
                public T next() throws E {
                    if ((cur == null || !cur.hasNext()) && !hasNext()) {
                        throw new NoSuchElementException();
                    }

                    return cur.next();
                }
            };
        }

        /**
         * Checks for next.
         *
         * @return
         * @throws E the e
         */
        public abstract boolean hasNext() throws E;

        /**
         *
         * @return
         * @throws E the e
         */
        public abstract T next() throws E;

        /**
         *
         * @param n
         * @throws E the e
         */
        public void advance(long n) throws E {
            N.checkArgNotNegative(n, "n");

            while (n-- > 0 && hasNext()) {
                next();
            }
        }

        /**
         *
         * @return
         * @throws E the e
         */
        public long count() throws E {
            long result = 0;

            while (hasNext()) {
                next();
                result++;
            }

            return result;
        }

        /**
         *
         * @throws E the e
         */
        public void close() throws E {
            // Nothing to do by default.
        }
    }

    // CheckedException -> Maybe makes sense. Checked exception...
    // But what does CheckedStream mean? Checked stream ???
    //    public static final class CheckedStream<T, E extends Exception> extends ExceptionalStream<T, E> {
    //
    //        CheckedStream(ExceptionalIterator<T, E> iter, boolean sorted, Comparator<? super T> comparator, Deque<Throwables.Runnable<? extends E>> closeHandlers) {
    //            super(iter, sorted, comparator, closeHandlers);
    //        }
    //    }

    /**
     *
     * @param <T>
     * @param <E>
     */
    public static final class CheckedStream<T, E extends Exception> extends ExceptionalStream<T, E> {

        CheckedStream(ExceptionalIterator<T, E> iter, boolean sorted, Comparator<? super T> comparator, Deque<Throwables.Runnable<? extends E>> closeHandlers) {
            super(iter, sorted, comparator, closeHandlers);
        }
    }

    //    /**
    //     * Mostly it's for android.
    //     *
    //     * @see {@code ExceptionalStream<T, RuntimeException>}
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

    /**
     * Mostly it's for android.
     *
     * @see {@code ExceptionalStream<T, RuntimeException>}
     *
     * @deprecated Mostly it's for android.
     */
    @Beta
    @Deprecated
    public final static class Seq {
        private Seq() {
            // singleton for utility class.
        }

        public static <T> ExceptionalStream<T, RuntimeException> empty() {
            return ExceptionalStream.<T, RuntimeException> empty();
        }

        public static <T> ExceptionalStream<T, RuntimeException> just(final T e) {
            return ExceptionalStream.<T, RuntimeException> just(e);
        }

        public static <T> ExceptionalStream<T, RuntimeException> ofNullable(final T e) {
            return ExceptionalStream.<T, RuntimeException> ofNullable(e);
        }

        public static <T> ExceptionalStream<T, RuntimeException> of(final T... a) {
            return ExceptionalStream.<T, RuntimeException> of(a);
        }

        public static <T> ExceptionalStream<T, RuntimeException> of(final Collection<? extends T> c) {
            return ExceptionalStream.<T, RuntimeException> of(c);
        }

        public static <T> ExceptionalStream<T, RuntimeException> of(final Iterator<? extends T> iter) {
            return ExceptionalStream.<T, RuntimeException> of(iter);
        }

        public static <T> ExceptionalStream<T, RuntimeException> of(final Iterable<? extends T> iterable) {
            return ExceptionalStream.<T, RuntimeException> of(iterable);
        }

        public static <K, V> ExceptionalStream<Map.Entry<K, V>, RuntimeException> of(final Map<K, V> m) {
            return ExceptionalStream.<K, V, RuntimeException> of(m);
        }

        public static <T> ExceptionalStream<T, RuntimeException> of(final Stream<? extends T> stream) {
            return ExceptionalStream.<T, RuntimeException> of(stream);
        }

        public static <T> ExceptionalStream<T, RuntimeException> of(final java.util.stream.Stream<? extends T> stream) {
            return ExceptionalStream.<T, RuntimeException> of(stream);
        }

        public static ExceptionalStream<Boolean, RuntimeException> of(final boolean[] a) {
            return ExceptionalStream.<RuntimeException> of(a);
        }

        public static ExceptionalStream<Character, RuntimeException> of(final char[] a) {
            return ExceptionalStream.<RuntimeException> of(a);
        }

        public static ExceptionalStream<Byte, RuntimeException> of(final byte[] a) {
            return ExceptionalStream.<RuntimeException> of(a);
        }

        public static ExceptionalStream<Short, RuntimeException> of(final short[] a) {
            return ExceptionalStream.<RuntimeException> of(a);
        }

        public static ExceptionalStream<Integer, RuntimeException> of(final int[] a) {
            return ExceptionalStream.<RuntimeException> of(a);
        }

        public static ExceptionalStream<Long, RuntimeException> of(final long[] a) {
            return ExceptionalStream.<RuntimeException> of(a);
        }

        public static ExceptionalStream<Float, RuntimeException> of(final float[] a) {
            return ExceptionalStream.<RuntimeException> of(a);
        }

        public static ExceptionalStream<Double, RuntimeException> of(final double[] a) {
            return ExceptionalStream.<RuntimeException> of(a);
        }

        public static <T> ExceptionalStream<T, RuntimeException> of(final Optional<T> op) {
            return ExceptionalStream.<T, RuntimeException> of(op);
        }

        public static <T> ExceptionalStream<T, RuntimeException> of(final java.util.Optional<T> op) {
            return ExceptionalStream.<T, RuntimeException> of(op);
        }

        public static <K> ExceptionalStream<K, RuntimeException> ofKeys(final Map<K, ?> map) {
            return ExceptionalStream.<K, RuntimeException> ofKeys(map);
        }

        public static <K, V> ExceptionalStream<K, RuntimeException> ofKeys(final Map<K, V> map,
                final Throwables.Predicate<? super V, RuntimeException> valueFilter) {
            return ExceptionalStream.<K, V, RuntimeException> ofKeys(map, valueFilter);
        }

        public static <K, V> ExceptionalStream<K, RuntimeException> ofKeys(final Map<K, V> map,
                final Throwables.BiPredicate<? super K, ? super V, RuntimeException> filter) {
            return ExceptionalStream.ofKeys(map, filter);
        }

        public static <V> ExceptionalStream<V, RuntimeException> ofValues(final Map<?, V> map) {
            return ExceptionalStream.<V, RuntimeException> ofValues(map);
        }

        public static <K, V> ExceptionalStream<V, RuntimeException> ofValues(final Map<K, V> map,
                final Throwables.Predicate<? super K, RuntimeException> keyFilter) {
            return ExceptionalStream.<K, V, RuntimeException> ofValues(map, keyFilter);
        }

        public static <K, V> ExceptionalStream<V, RuntimeException> ofValues(final Map<K, V> map,
                final Throwables.BiPredicate<? super K, ? super V, RuntimeException> filter) {
            return ExceptionalStream.ofValues(map, filter);
        }

        @Beta
        public static <T> ExceptionalStream<T, RuntimeException> from(final Throwables.Supplier<Collection<? extends T>, RuntimeException> supplier) {
            return ExceptionalStream.<T, RuntimeException> from(supplier);
        }

        public static <T> ExceptionalStream<T, RuntimeException> defer(
                final Throwables.Supplier<ExceptionalStream<? extends T, ? extends RuntimeException>, RuntimeException> supplier) {
            return ExceptionalStream.<T, RuntimeException> defer(supplier);
        }

        public static <T> ExceptionalStream<T, RuntimeException> iterate(final Throwables.BooleanSupplier<? extends RuntimeException> hasNext,
                final Throwables.Supplier<? extends T, RuntimeException> next) {
            return ExceptionalStream.<T, RuntimeException> iterate(hasNext, next);
        }

        public static <T> ExceptionalStream<T, RuntimeException> iterate(final T init, final Throwables.BooleanSupplier<? extends RuntimeException> hasNext,
                final Throwables.UnaryOperator<T, ? extends RuntimeException> f) {
            return ExceptionalStream.<T, RuntimeException> iterate(init, hasNext, f);
        }

        public static <T> ExceptionalStream<T, RuntimeException> iterate(final T init, final Throwables.Predicate<? super T, RuntimeException> hasNext,
                final Throwables.UnaryOperator<T, RuntimeException> f) {
            return ExceptionalStream.<T, RuntimeException> iterate(init, hasNext, f);
        }

        public static <T> ExceptionalStream<T, RuntimeException> iterate(final T init, final Throwables.UnaryOperator<T, RuntimeException> f) {
            return ExceptionalStream.<T, RuntimeException> iterate(init, f);
        }

        public static <T> ExceptionalStream<T, RuntimeException> generate(final Throwables.Supplier<T, RuntimeException> supplier) {
            return ExceptionalStream.<T, RuntimeException> generate(supplier);
        }

        public static <T> ExceptionalStream<T, RuntimeException> repeat(final T element, final long n) {
            return ExceptionalStream.<T, RuntimeException> repeat(element, n);
        }

        public static ExceptionalStream<Integer, RuntimeException> range(final int startInclusive, final int endExclusive) {
            return ExceptionalStream.<RuntimeException> range(startInclusive, endExclusive);
        }

        public static ExceptionalStream<Integer, RuntimeException> range(final int startInclusive, final int endExclusive, final int by) {
            return ExceptionalStream.<RuntimeException> range(startInclusive, endExclusive, by);
        }

        public static ExceptionalStream<Integer, RuntimeException> rangeClosed(final int startInclusive, final int endExclusive) {
            return ExceptionalStream.<RuntimeException> rangeClosed(startInclusive, endExclusive);
        }

        public static ExceptionalStream<Integer, RuntimeException> rangeClosed(final int startInclusive, final int endExclusive, final int by) {
            return ExceptionalStream.<RuntimeException> rangeClosed(startInclusive, endExclusive, by);
        }

        @SafeVarargs
        public static <T> ExceptionalStream<T, RuntimeException> concat(final T[]... a) {
            return ExceptionalStream.<T, RuntimeException> concat(a);
        }

        @SafeVarargs
        public static <T> ExceptionalStream<T, RuntimeException> concat(final Iterable<? extends T>... a) {
            return ExceptionalStream.<T, RuntimeException> concat(a);
        }

        @SafeVarargs
        public static <T> ExceptionalStream<T, RuntimeException> concat(final Iterator<? extends T>... a) {
            return ExceptionalStream.<T, RuntimeException> concat(a);
        }

        public static <A, B, T> ExceptionalStream<T, RuntimeException> zip(final A[] a, final B[] b,
                final Throwables.BiFunction<? super A, ? super B, T, RuntimeException> zipFunction) {
            return ExceptionalStream.<A, B, T, RuntimeException> zip(a, b, zipFunction);
        }

        public static <A, B, C, T> ExceptionalStream<T, RuntimeException> zip(final A[] a, final B[] b, final C[] c,
                final Throwables.TriFunction<? super A, ? super B, ? super C, T, RuntimeException> zipFunction) {
            return ExceptionalStream.<A, B, C, T, RuntimeException> zip(a, b, c, zipFunction);
        }

        public static <A, B, T> ExceptionalStream<T, RuntimeException> zip(final Iterable<? extends A> a, final Iterable<? extends B> b,
                final Throwables.BiFunction<? super A, ? super B, T, RuntimeException> zipFunction) {
            return ExceptionalStream.<A, B, T, RuntimeException> zip(a, b, zipFunction);
        }

        public static <A, B, C, T> ExceptionalStream<T, RuntimeException> zip(final Iterable<? extends A> a, final Iterable<? extends B> b,
                final Iterable<? extends C> c, final Throwables.TriFunction<? super A, ? super B, ? super C, T, RuntimeException> zipFunction) {
            return ExceptionalStream.<A, B, C, T, RuntimeException> zip(a, b, c, zipFunction);
        }

        public static <A, B, T> ExceptionalStream<T, RuntimeException> zip(final Iterator<? extends A> a, final Iterator<? extends B> b,
                final Throwables.BiFunction<? super A, ? super B, T, RuntimeException> zipFunction) {
            return ExceptionalStream.<A, B, T, RuntimeException> zip(a, b, zipFunction);
        }

        public static <A, B, C, T> ExceptionalStream<T, RuntimeException> zip(final Iterator<? extends A> a, final Iterator<? extends B> b,
                final Iterator<? extends C> c, final Throwables.TriFunction<? super A, ? super B, ? super C, T, RuntimeException> zipFunction) {
            return ExceptionalStream.<A, B, C, T, RuntimeException> zip(a, b, c, zipFunction);
        }

        public static <A, B, T> ExceptionalStream<T, RuntimeException> zip(final A[] a, final B[] b, final A valueForNoneA, final B valueForNoneB,
                final Throwables.BiFunction<? super A, ? super B, T, RuntimeException> zipFunction) {
            return ExceptionalStream.<A, B, T, RuntimeException> zip(a, b, valueForNoneA, valueForNoneB, zipFunction);
        }

        public static <A, B, C, T> ExceptionalStream<T, RuntimeException> zip(final A[] a, final B[] b, final C[] c, final A valueForNoneA,
                final B valueForNoneB, final C valueForNoneC, final Throwables.TriFunction<? super A, ? super B, ? super C, T, RuntimeException> zipFunction) {
            return ExceptionalStream.<A, B, C, T, RuntimeException> zip(a, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
        }

        public static <A, B, T> ExceptionalStream<T, RuntimeException> zip(final Iterable<? extends A> a, final Iterable<? extends B> b, final A valueForNoneA,
                final B valueForNoneB, final Throwables.BiFunction<? super A, ? super B, T, RuntimeException> zipFunction) {
            return ExceptionalStream.<A, B, T, RuntimeException> zip(a, b, valueForNoneA, valueForNoneB, zipFunction);
        }

        public static <A, B, C, T> ExceptionalStream<T, RuntimeException> zip(final Iterable<? extends A> a, final Iterable<? extends B> b,
                final Iterable<? extends C> c, final A valueForNoneA, final B valueForNoneB, final C valueForNoneC,
                final Throwables.TriFunction<? super A, ? super B, ? super C, T, RuntimeException> zipFunction) {
            return ExceptionalStream.<A, B, C, T, RuntimeException> zip(a, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
        }

        public static <A, B, T> ExceptionalStream<T, RuntimeException> zip(final Iterator<? extends A> a, final Iterator<? extends B> b, final A valueForNoneA,
                final B valueForNoneB, final Throwables.BiFunction<? super A, ? super B, T, RuntimeException> zipFunction) {
            return ExceptionalStream.<A, B, T, RuntimeException> zip(a, b, valueForNoneA, valueForNoneB, zipFunction);
        }

        public static <A, B, C, T> ExceptionalStream<T, RuntimeException> zip(final Iterator<? extends A> a, final Iterator<? extends B> b,
                final Iterator<? extends C> c, final A valueForNoneA, final B valueForNoneB, final C valueForNoneC,
                final Throwables.TriFunction<? super A, ? super B, ? super C, T, RuntimeException> zipFunction) {
            return ExceptionalStream.<A, B, C, T, RuntimeException> zip(a, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
        }

        public static <T> ExceptionalStream<T, RuntimeException> merge(final T[] a, final T[] b,
                final Throwables.BiFunction<? super T, ? super T, MergeResult, RuntimeException> nextSelector) {
            return ExceptionalStream.<T, RuntimeException> merge(a, b, nextSelector);
        }

        public static <T> ExceptionalStream<T, RuntimeException> merge(final T[] a, final T[] b, final T[] c,
                final Throwables.BiFunction<? super T, ? super T, MergeResult, RuntimeException> nextSelector) {
            return ExceptionalStream.<T, RuntimeException> merge(a, b, c, nextSelector);
        }

        public static <T> ExceptionalStream<T, RuntimeException> merge(final Iterable<? extends T> a, final Iterable<? extends T> b,
                final Throwables.BiFunction<? super T, ? super T, MergeResult, RuntimeException> nextSelector) {
            return ExceptionalStream.<T, RuntimeException> merge(a, b, nextSelector);
        }

        public static <T> ExceptionalStream<T, RuntimeException> merge(final Iterable<? extends T> a, final Iterable<? extends T> b,
                final Iterable<? extends T> c, final Throwables.BiFunction<? super T, ? super T, MergeResult, RuntimeException> nextSelector) {
            return ExceptionalStream.<T, RuntimeException> merge(a, b, c, nextSelector);
        }

        public static <T> ExceptionalStream<T, RuntimeException> merge(final Iterator<? extends T> a, final Iterator<? extends T> b,
                final Throwables.BiFunction<? super T, ? super T, MergeResult, RuntimeException> nextSelector) {
            return ExceptionalStream.<T, RuntimeException> merge(a, b, nextSelector);
        }

        public static <T> ExceptionalStream<T, RuntimeException> merge(final Iterator<? extends T> a, final Iterator<? extends T> b,
                final Iterator<? extends T> c, final Throwables.BiFunction<? super T, ? super T, MergeResult, RuntimeException> nextSelector) {
            return ExceptionalStream.<T, RuntimeException> merge(a, b, c, nextSelector);
        }

    }
}
