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
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.security.SecureRandom;
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
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.stream.StreamSupport;

import com.landawn.abacus.DataSet;
import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.IntermediateOp;
import com.landawn.abacus.annotation.LazyEvaluation;
import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.annotation.TerminalOp;
import com.landawn.abacus.annotation.TerminalOpTriggered;
import com.landawn.abacus.exception.DuplicatedResultException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.Fn.Factory;
import com.landawn.abacus.util.Fn.Fnn;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.StringUtil.Strings;
import com.landawn.abacus.util.u.Holder;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.stream.BaseStream;
import com.landawn.abacus.util.stream.Collector;
import com.landawn.abacus.util.stream.Collectors;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 * The Stream will be automatically closed after execution(A terminal method is executed/triggered).
 *
 * @author Haiyang Li
 * @param <T>
 * @param <E>
 * @since 1.3
 * @see BaseStream
 * @see Stream
 */
@LazyEvaluation
@SequentialOnly
@com.landawn.abacus.annotation.Immutable
public class ExceptionalStream<T, E extends Exception> implements Closeable, Immutable {

    static final Logger logger = LoggerFactory.getLogger(ExceptionalStream.class);

    static final Random RAND = new SecureRandom();

    static final Throwables.Function<Map.Entry<Keyed<Object, Object>, Object>, Object, Exception> KK = new Throwables.Function<Map.Entry<Keyed<Object, Object>, Object>, Object, Exception>() {
        @Override
        public Object apply(Map.Entry<Keyed<Object, Object>, Object> t) throws Exception {
            return t.getKey().val();
        }
    };

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
     *
     * @param <T>
     * @param <E>
     * @param a
     * @return
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
                N.checkArgNotNegative(n, "n");

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
                    N.checkArgNotNegative(n, "n");

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

        return of(iterable.iterator());
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
        if (m == null) {
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
        if (stream == null) {
            return empty();
        }

        final ExceptionalIterator<T, E> iter = new ExceptionalIterator<T, E>() {
            private Stream<? extends T> s = stream;
            private Iterator<? extends T> iter = null;
            private boolean isInitialized = false;

            @Override
            public boolean hasNext() throws E {
                if (isInitialized == false) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public T next() throws E {
                if (isInitialized == false) {
                    init();
                }

                return iter.next();
            }

            @Override
            public void advance(long n) throws E {
                N.checkArgNotNegative(n, "n");

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
                if (isInitialized == false) {
                    isInitialized = true;
                    iter = stream.iterator();
                }
            }
        };

        return newStream(iter).onClose(new Throwables.Runnable<E>() {
            @Override
            public void run() throws E {
                iter.close();
            }
        });
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

        return ExceptionalStream.<T, E> of(stream.iterator()).onClose(new Throwables.Runnable<E>() {
            @Override
            public void run() throws E {
                stream.close();
            }
        });
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
                N.checkArgNotNegative(n, "n");

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
                N.checkArgNotNegative(n, "n");

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
                N.checkArgNotNegative(n, "n");

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

    /**
     * Lazy evaluation.
     * <br />
     *  
     * This is equal to: {@code ExceptionalStream.just(supplier).flattMap(it -> it.get())}.
     * 
     * @param supplier
     * @return
     */
    @Beta
    public static <T, E extends Exception> ExceptionalStream<T, E> of(final Throwables.Supplier<Collection<? extends T>, ? extends E> supplier) {
        N.checkArgNotNull(supplier, "supplier");

        return ExceptionalStream.<Throwables.Supplier<Collection<? extends T>, ? extends E>, E> just(supplier)
                .flattMap(new Throwables.Function<Throwables.Supplier<Collection<? extends T>, ? extends E>, Collection<? extends T>, E>() {
                    @Override
                    public Collection<? extends T> apply(Throwables.Supplier<Collection<? extends T>, ? extends E> t) throws E {
                        return t.get();
                    }
                });
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
    public static <T, E extends Exception> ExceptionalStream<T, E> from(
            final Throwables.Supplier<ExceptionalStream<? extends T, ? extends E>, ? extends E> supplier) {
        N.checkArgNotNull(supplier, "supplier");

        return ExceptionalStream.<Throwables.Supplier<ExceptionalStream<? extends T, ? extends E>, ? extends E>, E> just(supplier)
                .flatMap(
                        new Throwables.Function<Throwables.Supplier<ExceptionalStream<? extends T, ? extends E>, ? extends E>, ExceptionalStream<? extends T, ? extends E>, E>() {
                            @Override
                            public ExceptionalStream<? extends T, ? extends E> apply(
                                    Throwables.Supplier<ExceptionalStream<? extends T, ? extends E>, ? extends E> t) throws E {
                                return t.get();
                            }
                        });
    }

    public static <K, E extends Exception> ExceptionalStream<K, E> ofKeys(final Map<K, ?> map) {
        if (N.isNullOrEmpty(map)) {
            return empty();
        }

        return of(map.keySet());
    }

    public static <K, V, E extends Exception> ExceptionalStream<K, E> ofKeys(final Map<K, V> map, final Throwables.Predicate<? super V, E> valueFilter) {
        if (map == null || map.size() == 0) {
            return empty();
        }

        return ExceptionalStream.<K, V, E> of(map).filter(Fnn.<K, V, E> testByValue(valueFilter)).map(Fnn.<K, V, E> key());
    }

    public static <V, E extends Exception> ExceptionalStream<V, E> ofValues(final Map<?, V> map) {
        if (N.isNullOrEmpty(map)) {
            return empty();
        }

        return of(map.values());
    }

    public static <K, V, E extends Exception> ExceptionalStream<V, E> ofValues(final Map<K, V> map, final Throwables.Predicate<? super K, E> keyFilter) {
        if (map == null || map.size() == 0) {
            return empty();
        }

        return ExceptionalStream.<K, V, E> of(map).filter(Fnn.<K, V, E> testByKey(keyFilter)).map(Fnn.<K, V, E> value());
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
                if (hasNextVal == false) {
                    hasNextVal = hasNext.getAsBoolean();
                }

                return hasNextVal;
            }

            @Override
            public T next() throws E {
                if (hasNextVal == false && hasNext() == false) {
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
            private final T NONE = (T) N.NULL_MASK;
            private T t = NONE;
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() throws E {
                if (hasNextVal == false) {
                    hasNextVal = hasNext.getAsBoolean();
                }

                return hasNextVal;
            }

            @Override
            public T next() throws E {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNextVal = false;
                return t = (t == NONE) ? init : f.apply(t);
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
            private final T NONE = (T) N.NULL_MASK;
            private T t = NONE;
            private T cur = NONE;
            private boolean hasMore = true;
            private boolean hasNextVal = false;

            @Override
            public boolean hasNext() throws E {
                if (hasNextVal == false && hasMore) {
                    hasNextVal = hasNext.test((cur = (t == NONE ? init : f.apply(t))));

                    if (hasNextVal == false) {
                        hasMore = false;
                    }
                }

                return hasNextVal;
            }

            @Override
            public T next() throws E {
                if (hasNextVal == false && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                t = cur;
                cur = NONE;
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
            private final T NONE = (T) N.NULL_MASK;
            private T t = NONE;

            @Override
            public boolean hasNext() throws E {
                return true;
            }

            @Override
            public T next() throws E {
                return t = t == NONE ? init : f.apply(t);
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

        return newStream(iter).onClose(new Throwables.Runnable<IOException>() {
            @Override
            public void run() throws IOException {
                iter.close();
            }
        });
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

        return newStream(iter).onClose(new Throwables.Runnable<IOException>() {
            @Override
            public void run() throws IOException {
                iter.close();
            }
        });
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
        } else if (recursively == false) {
            return of(parentPath.listFiles());
        }

        final ExceptionalIterator<File, IOException> iter = new ExceptionalIterator<File, IOException>() {
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
                if (hasNext() == false) {
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
        return ExceptionalIterator.of(new Throwables.Supplier<ExceptionalIterator<String, IOException>, IOException>() {
            private ExceptionalIterator<String, IOException> lazyIter = null;

            @Override
            public synchronized ExceptionalIterator<String, IOException> get() {
                if (lazyIter == null) {
                    lazyIter = new ExceptionalIterator<String, IOException>() {
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
    public static <T, E extends Exception> ExceptionalStream<T, E> concat(final Collection<? extends T>... a) {
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
                while ((iter == null || iter.hasNext() == false) && iterators.hasNext()) {
                    if (cur != null) {
                        cur.close();
                    }

                    cur = iterators.next();
                    iter = cur.elements;
                }

                return iter != null && iter.hasNext();
            }

            @Override
            public T next() throws E {
                if ((iter == null || iter.hasNext() == false) && hasNext() == false) {
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
    public static <A, B, T, E extends Exception> ExceptionalStream<T, E> zip(final Collection<? extends A> a, final Collection<? extends B> b,
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
    public static <A, B, C, T, E extends Exception> ExceptionalStream<T, E> zip(final Collection<? extends A> a, final Collection<? extends B> b,
            final Collection<? extends C> c, final Throwables.TriFunction<? super A, ? super B, ? super C, T, ? extends E> zipFunction) {
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
            @Override
            public boolean hasNext() throws E {
                return a.hasNext() && b.hasNext();
            }

            @Override
            public T next() throws E {
                return zipFunction.apply(a.next(), b.next());
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
            @Override
            public boolean hasNext() throws E {
                return a.hasNext() && b.hasNext() && c.hasNext();
            }

            @Override
            public T next() throws E {
                return zipFunction.apply(a.next(), b.next(), c.next());
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
            private final ExceptionalIterator<? extends A, E> iterA = a.elements;
            private final ExceptionalIterator<? extends B, E> iterB = b.elements;

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
            private final ExceptionalIterator<? extends A, E> iterA = a.elements;
            private final ExceptionalIterator<? extends B, E> iterB = b.elements;
            private final ExceptionalIterator<? extends C, E> iterC = c.elements;

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
    public static <A, B, T, E extends Exception> ExceptionalStream<T, E> zip(final Collection<? extends A> a, final Collection<? extends B> b,
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
    public static <A, B, C, T, E extends Exception> ExceptionalStream<T, E> zip(final Collection<? extends A> a, final Collection<? extends B> b,
            final Collection<? extends C> c, final A valueForNoneA, final B valueForNoneB, final C valueForNoneC,
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
            @Override
            public boolean hasNext() throws E {
                return a.hasNext() || b.hasNext();
            }

            @Override
            public T next() throws E {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.next() : valueForNoneA, b.hasNext() ? b.next() : valueForNoneB);
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
            @Override
            public boolean hasNext() throws E {
                return a.hasNext() || b.hasNext() || c.hasNext();
            }

            @Override
            public T next() throws E {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return zipFunction.apply(a.hasNext() ? a.next() : valueForNoneA, b.hasNext() ? b.next() : valueForNoneB,
                        c.hasNext() ? c.next() : valueForNoneC);
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
            private final ExceptionalIterator<? extends A, E> iterA = a.elements;
            private final ExceptionalIterator<? extends B, E> iterB = b.elements;

            @Override
            public boolean hasNext() throws E {
                return iterA.hasNext() || iterB.hasNext();
            }

            @Override
            public T next() throws E {
                if (hasNext() == false) {
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
            private final ExceptionalIterator<? extends A, E> iterA = a.elements;
            private final ExceptionalIterator<? extends B, E> iterB = b.elements;
            private final ExceptionalIterator<? extends C, E> iterC = c.elements;

            @Override
            public boolean hasNext() throws E {
                return iterA.hasNext() || iterB.hasNext() || iterC.hasNext();
            }

            @Override
            public T next() throws E {
                if (hasNext() == false) {
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
            if (N.notNullOrEmpty(s.closeHandlers)) {
                newCloseHandlers.addAll(s.closeHandlers);
            }
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
        return merge(merge(a, b, nextSelector).iterator(), ExceptionalIterator.<T, E> wrap(N.iterate(c)), nextSelector);
    }

    /**
     *
     * @param a
     * @param b
     * @param nextSelector first parameter is selected if <code>Nth.FIRST</code> is returned, otherwise the second parameter is selected.
     * @return
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> merge(final Collection<? extends T> a, final Collection<? extends T> b,
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
    public static <T, E extends Exception> ExceptionalStream<T, E> merge(final Collection<? extends T> a, final Collection<? extends T> b,
            final Collection<? extends T> c, final Throwables.BiFunction<? super T, ? super T, MergeResult, E> nextSelector) {
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
        return merge(merge(a, b, nextSelector).iterator(), ExceptionalIterator.<T, E> wrap(c), nextSelector);
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
        return merge(a.iterator(), b.iterator(), nextSelector).onClose(new Throwables.Runnable<E>() {
            @Override
            public void run() throws E {
                try {
                    a.close();
                } finally {
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
            private T nextA = null;
            private T nextB = null;
            private boolean hasNextA = false;
            private boolean hasNextB = false;

            @Override
            public boolean hasNext() throws E {
                return hasNextA || hasNextB || a.hasNext() || b.hasNext();
            }

            @Override
            public T next() throws E {
                if (hasNextA) {
                    if (b.hasNext()) {
                        if (nextSelector.apply(nextA, (nextB = b.next())) == MergeResult.TAKE_FIRST) {
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
                    if (a.hasNext()) {
                        if (nextSelector.apply((nextA = a.next()), nextB) == MergeResult.TAKE_FIRST) {
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
                } else if (a.hasNext()) {
                    if (b.hasNext()) {
                        if (nextSelector.apply((nextA = a.next()), (nextB = b.next())) == MergeResult.TAKE_FIRST) {
                            hasNextB = true;
                            return nextA;
                        } else {
                            hasNextA = true;
                            return nextB;
                        }
                    } else {
                        return a.next();
                    }
                } else if (b.hasNext()) {
                    return b.next();
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
                if (hasNext == false) {
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
                if (hasNext == false && hasNext() == false) {
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

        return filter(new Throwables.Predicate<T, E>() {
            @Override
            public boolean test(T value) throws E {
                if (!predicate.test(value)) {
                    actionOnDroppedItem.accept(value);
                    return false;
                }

                return true;
            }
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
                if (hasNext == false && hasMore && elements.hasNext()) {
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
                if (hasNext == false && hasNext() == false) {
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
                if (hasNext == false) {
                    if (dropped == false) {
                        dropped = true;

                        while (elements.hasNext()) {
                            next = elements.next();

                            if (predicate.test(next) == false) {
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
                if (hasNext == false && hasNext() == false) {
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

        return filter(new Throwables.Predicate<T, E>() {
            @Override
            public boolean test(T value) throws E {
                if (!predicate.test(value)) {
                    actionOnDroppedItem.accept(value);
                    return false;
                }

                return true;
            }
        });
    }

    /**
     *
     * @param predicate
     * @return
     */
    @IntermediateOp
    @Beta
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

        return filter(new Throwables.Predicate<T, E>() {
            @Override
            public boolean test(T value) {
                return set.add(hashKey(value));
            }
        });
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

        return filter(new Throwables.Predicate<T, E>() {
            @Override
            public boolean test(T value) throws E {
                return set.add(hashKey(keyMapper.apply(value)));
            }
        });
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

        final Throwables.Function<T, Keyed<K, T>, E> keyedMapper = new Throwables.Function<T, Keyed<K, T>, E>() {
            @Override
            public Keyed<K, T> apply(T t) throws E {
                return Keyed.of(keyMapper.apply(t), t);
            }
        };

        final Throwables.Predicate<Map.Entry<Keyed<K, T>, Long>, ? extends E> predicate = new Throwables.Predicate<Map.Entry<Keyed<K, T>, Long>, E>() {
            @Override
            public boolean test(Map.Entry<Keyed<K, T>, Long> e) throws E {
                return occurrencesFilter.test(e.getValue());
            }
        };

        return groupBy(keyedMapper, Collectors.counting(), supplier).filter(predicate)
                .map((Throwables.Function<Map.Entry<Keyed<K, T>, Long>, T, E>) (Throwables.Function) KK);
    }

    /**
     * Distinct and filter by occurrences.
     *
     * @param keyMapper
     * @param occurrencesFilter
     * @return
     * @see #groupBy(Function, Function, BinaryOperator)
     */
    @IntermediateOp
    @TerminalOpTriggered
    public <K> ExceptionalStream<T, E> distinctBy(final Throwables.Function<? super T, K, ? extends E> keyMapper,
            final Throwables.BinaryOperator<T, ? extends E> mergeFunction) {
        assertNotClosed();

        final Supplier<? extends Map<K, T>> supplier = Suppliers.<K, T> ofLinkedHashMap();

        return groupBy(keyMapper, Fnn.<T, E> identity(), mergeFunction, supplier).map(Fnn.<K, T, E> value());
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

        final ExceptionalIterator<R, E> iter = new ExceptionalIterator<R, E>() {
            private ExceptionalIterator<? extends R, ? extends E> cur = null;
            private ExceptionalStream<? extends R, ? extends E> s = null;
            private Deque<? extends Throwables.Runnable<? extends E>> closeHandle = null;

            @Override
            public boolean hasNext() throws E {
                while (cur == null || cur.hasNext() == false) {
                    if (elements.hasNext()) {
                        if (closeHandle != null) {
                            final Deque<? extends Throwables.Runnable<? extends E>> tmp = closeHandle;
                            closeHandle = null;
                            ExceptionalStream.close(tmp);
                        }

                        s = mapper.apply(elements.next());

                        if (N.notNullOrEmpty(s.closeHandlers)) {
                            closeHandle = s.closeHandlers;
                        }

                        cur = s.elements;
                    } else {
                        cur = null;
                        break;
                    }
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public R next() throws E {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.next();
            }

            @Override
            public void close() throws E {
                if (closeHandle != null) {
                    ExceptionalStream.close(closeHandle);
                }
            }
        };

        final Deque<Throwables.Runnable<? extends E>> newCloseHandlers = new ArrayDeque<>(N.size(closeHandlers) + 1);

        newCloseHandlers.add(new Throwables.Runnable<E>() {
            @Override
            public void run() throws E {
                iter.close();
            }
        });

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
    public <R> ExceptionalStream<R, E> flattMap(final Throwables.Function<? super T, ? extends Collection<? extends R>, ? extends E> mapper) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<R, E>() {
            private Iterator<? extends R> cur = null;
            private Collection<? extends R> c = null;

            @Override
            public boolean hasNext() throws E {
                while ((cur == null || cur.hasNext() == false) && elements.hasNext()) {
                    c = mapper.apply(elements.next());
                    cur = N.isNullOrEmpty(c) ? null : c.iterator();
                }

                return cur != null && cur.hasNext();
            }

            @Override
            public R next() throws E {
                if ((cur == null || cur.hasNext() == false) && hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return cur.next();
            }
        }, closeHandlers);
    }

    //    /**
    //     * 
    //     * @param mapper
    //     * @return
    //     */
    //    @IntermediateOp
    //    public ExceptionalStream<Integer, E> flattMapToInt(final Throwables.Function<? super T, ? extends int[], ? extends E> mapper) {
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
    //    public ExceptionalStream<Long, E> flattMapToLong(final Throwables.Function<? super T, ? extends long[], ? extends E> mapper) {
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
    //    public ExceptionalStream<Double, E> flattMapToDouble(final Throwables.Function<? super T, ? extends double[], ? extends E> mapper) {
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
            private final T NONE = (T) N.NULL_MASK;
            private T prev = NONE;
            private T _1 = NONE;

            @Override
            public boolean hasNext() throws E {
                if (increment > windowSize && prev != NONE) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.next();
                    }

                    prev = NONE;
                }

                if (ignoreNotPaired && _1 == NONE && elements.hasNext()) {
                    _1 = elements.next();
                }

                return elements.hasNext();
            }

            @Override
            public R next() throws E {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                if (ignoreNotPaired) {
                    final R res = mapper.apply(_1, (prev = elements.next()));
                    _1 = increment == 1 ? prev : NONE;
                    return res;
                } else {
                    if (increment == 1) {
                        return mapper.apply(prev == NONE ? elements.next() : prev, (prev = (elements.hasNext() ? elements.next() : null)));
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
            private final T NONE = (T) N.NULL_MASK;
            private T prev = NONE;
            private T prev2 = NONE;
            private T _1 = NONE;
            private T _2 = NONE;

            @Override
            public boolean hasNext() throws E {
                if (increment > windowSize && prev != NONE) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.next();
                    }

                    prev = NONE;
                }

                if (ignoreNotPaired) {
                    if (_1 == NONE && elements.hasNext()) {
                        _1 = elements.next();
                    }

                    if (_2 == NONE && elements.hasNext()) {
                        _2 = elements.next();
                    }
                }

                return elements.hasNext();
            }

            @Override
            public R next() throws E {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                if (ignoreNotPaired) {
                    final R res = mapper.apply(_1, _2, (prev = elements.next()));
                    _1 = increment == 1 ? _2 : (increment == 2 ? prev : NONE);
                    _2 = increment == 1 ? prev : NONE;
                    return res;
                } else {
                    if (increment == 1) {
                        return mapper.apply(prev2 == NONE ? elements.next() : prev2,
                                (prev2 = (prev == NONE ? (elements.hasNext() ? elements.next() : null) : prev)),
                                (prev = (elements.hasNext() ? elements.next() : null)));

                    } else if (increment == 2) {
                        return mapper.apply(prev == NONE ? elements.next() : prev, elements.hasNext() ? elements.next() : null,
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
                    iter = ExceptionalStream.this.toMap(keyMapper, valueMapper, downstream, mapFactory).entrySet().iterator();
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
     * @param collapsible
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
                if (hasNext == false) {
                    next = iter.next();
                }

                final List<T> c = new ArrayList<>();
                c.add(next);

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
     * @param collapsible
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
                if (hasNext == false) {
                    next = iter.next();
                }

                final C c = supplier.get();
                c.add(next);

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
     * Stream.of(new Integer[0]).collapse((a, b) -> a < b, (a, b) -> a + b) => []
     * Stream.of(1).collapse((a, b) -> a < b, (a, b) -> a + b) => [1]
     * Stream.of(1, 2).collapse((a, b) -> a < b, (a, b) -> a + b) => [3]
     * Stream.of(1, 2, 3).collapse((a, b) -> a < b, (a, b) -> a + b) => [6]
     * Stream.of(1, 2, 3, 3, 2, 1).collapse((a, b) -> a < b, (a, b) -> a + b) => [6, 3, 2, 1]
     * </code>
     * </pre>
     *
     * <br />
     * This method only run sequentially, even in parallel stream.
     *
     * @param collapsible
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
     * @param collapsible
     * @param init
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
     * @param collapsible
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
     * @param collapsible
     * @param collector
     * @return
     */
    @IntermediateOp
    public <R, A> ExceptionalStream<R, E> collapse(final Throwables.BiPredicate<? super T, ? super T, ? extends E> collapsible,
            final Collector<? super T, A, R> collector) {
        assertNotClosed();

        final Supplier<A> supplier = collector.supplier();
        final BiConsumer<A, ? super T> accumulator = collector.accumulator();
        final Function<A, R> finisher = collector.finisher();

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

        if (initIncluded == false) {
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
    * @param defaultValue
    * @return
    * @see #appendIfEmpty(Object...)
    */
    @IntermediateOp
    public final ExceptionalStream<T, E> defaultIfEmpty(final T defaultValue) {
        return appendIfEmpty(defaultValue);
    }

    /**
    *
    * @param supplier
    * @return
    * @see #appendIfEmpty(Supplier)
    */
    @IntermediateOp
    public final ExceptionalStream<T, E> defaultIfEmpty(final Supplier<? extends ExceptionalStream<T, E>> supplier) {
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
    public ExceptionalStream<T, E> appendIfEmpty(final Supplier<? extends ExceptionalStream<T, E>> supplier) {
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
                        iter = s.iterator();
                    }
                }
            }
        }, closeHandlers).onClose(new Throwables.Runnable<E>() {
            @Override
            public void run() throws E {
                close(holder);
            }
        });
    }

    void close(Holder<? extends ExceptionalStream<T, E>> holder) {
        if (holder.value() != null) {
            holder.value().close();
        }
    }

    @TerminalOp
    public <R> Optional<R> applyIfNotEmpty(final Throwables.Function<? super ExceptionalStream<T, E>, R, ? extends E> func) throws E {
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
    public OrElse acceptIfNotEmpty(Throwables.Consumer<? super ExceptionalStream<T, E>, ? extends E> action) throws E {
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
        return peek(action);
    }

    /**
     *
     * @param action
     * @return
     */
    @IntermediateOp
    public ExceptionalStream<T, E> peek(final Throwables.Consumer<? super T, ? extends E> action) {
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
     * @param chunkSize
     * @return
     */
    @IntermediateOp
    public ExceptionalStream<Stream<T>, E> split(final int chunkSize) {
        assertNotClosed();

        return splitToList(chunkSize).map(new Throwables.Function<List<T>, Stream<T>, E>() {
            @Override
            public Stream<T> apply(List<T> t) {
                return Stream.of(t);
            }
        });
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
                if (hasNext() == false) {
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

        final Supplier<A> supplier = collector.supplier();
        final BiConsumer<A, ? super T> accumulator = collector.accumulator();
        final Function<A, R> finisher = collector.finisher();

        return newStream(new ExceptionalIterator<R, E>() {
            @Override
            public boolean hasNext() throws E {
                return elements.hasNext();
            }

            @Override
            public R next() throws E {
                if (hasNext() == false) {
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

    /**
     *
     * @param windowSize
     * @param increment
     * @return
     */
    @IntermediateOp
    public ExceptionalStream<Stream<T>, E> sliding(final int windowSize, final int increment) {
        assertNotClosed();

        return slidingToList(windowSize, increment).map(new Throwables.Function<List<T>, Stream<T>, E>() {
            @Override
            public Stream<T> apply(List<T> t) {
                return Stream.of(t);
            }
        });
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
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                if (queue == null) {
                    queue = new ArrayDeque<>(N.max(0, windowSize - increment));
                }

                final C result = collectionSupplier.apply(windowSize);
                int cnt = 0;

                if (queue.size() > 0 && increment < windowSize) {
                    cnt = queue.size();

                    for (T e : queue) {
                        result.add(e);
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

        final Supplier<A> supplier = collector.supplier();
        final BiConsumer<A, ? super T> accumulator = collector.accumulator();
        final Function<A, R> finisher = collector.finisher();

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
                if (hasNext() == false) {
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

        return newStream(new ExceptionalIterator<T, E>() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() throws E {
                if (skipped == false) {
                    skipped = true;
                    advance(n);
                }

                return elements.hasNext();
            }

            @Override
            public T next() throws E {
                if (skipped == false) {
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
                if (initialized == false) {
                    init();
                }

                return cursor < to;
            }

            @Override
            public T next() throws E {
                if (initialized == false) {
                    init();
                }

                if (cursor >= to) {
                    throw new NoSuchElementException();
                }

                return aar[cursor++];
            }

            @Override
            public long count() throws E {
                if (initialized == false) {
                    init();
                }

                return to - cursor;
            }

            @Override
            public void advance(long n) throws E {
                if (initialized == false) {
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
                if (initialized == false) {
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
                if (initialized == false) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public T next() throws E {
                if (initialized == false) {
                    init();
                }

                return iter.next();
            }

            private void init() throws E {
                if (initialized == false) {
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
                if (hasNext() == false) {
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
                if (initialized == false) {
                    init();
                }

                return cursor > 0;
            }

            @Override
            public T next() throws E {
                if (initialized == false) {
                    init();
                }

                if (cursor <= 0) {
                    throw new NoSuchElementException();
                }

                return aar[--cursor];
            }

            @Override
            public long count() throws E {
                if (initialized == false) {
                    init();
                }

                return cursor;
            }

            @Override
            public void advance(long n) throws E {
                if (initialized == false) {
                    init();
                }

                cursor = n < cursor ? cursor - (int) n : 0;
            }

            private void init() throws E {
                if (initialized == false) {
                    initialized = true;
                    aar = (T[]) ExceptionalStream.this.toArray(false);
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
                if (initialized == false) {
                    init();
                }

                return cnt < len;
            }

            @Override
            public T next() throws E {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return aar[(start + cnt++) % len];
            }

            @Override
            public long count() throws E {
                if (initialized == false) {
                    init();
                }

                return len - cnt;
            }

            @Override
            public void advance(long n) throws E {
                if (initialized == false) {
                    init();
                }

                cnt = n < len - cnt ? cnt + (int) n : len;
            }

            private void init() throws E {
                if (initialized == false) {
                    initialized = true;
                    aar = (T[]) ExceptionalStream.this.toArray(false);
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

        return lazyLoad(new Function<Object[], Object[]>() {
            @Override
            public Object[] apply(final Object[] a) {
                N.shuffle(a, rnd);
                return a;
            }
        }, false, null);
    }

    @IntermediateOp
    @TerminalOpTriggered
    public ExceptionalStream<T, E> sorted() {
        return sorted(Comparators.NATURAL_ORDER);
    }

    @IntermediateOp
    @TerminalOpTriggered
    public ExceptionalStream<T, E> reverseSorted() {
        return sorted(Comparators.REVERSED_ORDER);
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

        return lazyLoad(new Function<Object[], Object[]>() {
            @Override
            public Object[] apply(final Object[] a) {
                N.sort((T[]) a, cmp);

                return a;
            }
        }, true, cmp);
    }

    /**
     *
     * @param keyMapper
     * @return
     */
    @IntermediateOp
    @SuppressWarnings("rawtypes")
    public ExceptionalStream<T, E> sortedBy(final Function<? super T, ? extends Comparable> keyMapper) {
        assertNotClosed();

        final Comparator<? super T> comparator = new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return N.compare(keyMapper.apply(o1), keyMapper.apply(o2));
            }
        };

        return sorted(comparator);
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
                if (initialized == false) {
                    init();
                }

                return cursor < len;
            }

            @Override
            public T next() throws E {
                if (initialized == false) {
                    init();
                }

                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                return aar[cursor++];
            }

            @Override
            public long count() throws E {
                if (initialized == false) {
                    init();
                }

                return len - cursor;
            }

            @Override
            public void advance(long n) throws E {
                checkArgNotNegative(n, "n");

                if (initialized == false) {
                    init();
                }

                cursor = n > len - cursor ? len : cursor + (int) n;
            }

            private void init() throws E {
                if (initialized == false) {
                    initialized = true;
                    aar = (T[]) op.apply(ExceptionalStream.this.toArray(false));
                    len = aar.length;
                }
            }
        }, sorted, cmp, closeHandlers);
    }

    @IntermediateOp
    public ExceptionalStream<T, E> intersperse(final T delimiter) {
        assertNotClosed();

        return newStream(new ExceptionalIterator<T, E>() {
            private final ExceptionalIterator<T, E> iter = iterator();
            private boolean toInsert = false;

            @Override
            public boolean hasNext() throws E {
                return iter.hasNext();
            }

            @Override
            public T next() throws E {
                if (hasNext() == false) {
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
        });
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
    @SequentialOnly
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
    @SequentialOnly
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

    /**
     * 
     * @param <U>
     * @param b
     * @return
     */
    @IntermediateOp
    public <U> ExceptionalStream<Pair<T, U>, E> crossJoin(final Collection<? extends U> b) {
        return crossJoin(b, Fnn.<T, U, E> pair());
    }

    /**
     * 
     * @param <U>
     * @param <R>
     * @param b
     * @param func
     * @return
     */
    @IntermediateOp
    public <U, R> ExceptionalStream<R, E> crossJoin(final Collection<? extends U> b, final Throwables.BiFunction<? super T, ? super U, R, ? extends E> func) {
        assertNotClosed();

        return flatMap(new Throwables.Function<T, ExceptionalStream<R, E>, E>() {
            @Override
            public ExceptionalStream<R, E> apply(final T t) throws E {
                return ExceptionalStream.<U, E> of(b).map(new Throwables.Function<U, R, E>() {
                    @Override
                    public R apply(U u) throws E {
                        return func.apply(t, u);
                    }
                });
            }
        });
    }

    /**
     * 
     * @param <U>
     * @param <R>
     * @param b
     * @param func
     * @return
     */
    @IntermediateOp
    public <U, R> ExceptionalStream<R, E> crossJoin(final ExceptionalStream<? extends U, E> b,
            final Throwables.BiFunction<? super T, ? super U, R, ? extends E> func) {
        assertNotClosed();

        checkArgNotNull(b, "stream");

        return flatMap(new Throwables.Function<T, ExceptionalStream<R, E>, E>() {
            private Collection<? extends U> c = null;

            @Override
            public ExceptionalStream<R, E> apply(final T t) throws E {
                if (c == null) {
                    c = b.toList();
                }

                return ExceptionalStream.<U, E> of(c).map(new Throwables.Function<U, R, E>() {
                    @Override
                    public R apply(U u) throws E {
                        return func.apply(t, u);
                    }
                });
            }
        });
    }

    /**
     *
     * @param <U>
     * @param <K>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @return
     */
    @IntermediateOp
    public <U, K> ExceptionalStream<Pair<T, U>, E> innerJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper) {
        return innerJoin(b, leftKeyMapper, rightKeyMapper, Fnn.<T, U, E> pair());
    }

    /**
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
    public <U, K, R> ExceptionalStream<R, E> innerJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper,
            final Throwables.BiFunction<? super T, ? super U, R, ? extends E> func) {
        assertNotClosed();

        return flatMap(new Throwables.Function<T, ExceptionalStream<R, E>, E>() {
            private ListMultimap<K, U> rightKeyMap = null;

            @Override
            public ExceptionalStream<R, E> apply(final T t) throws E {
                if (rightKeyMap == null) {
                    rightKeyMap = ListMultimap.from(b, rightKeyMapper);
                }

                return ExceptionalStream.<U, E> of(rightKeyMap.get(leftKeyMapper.apply(t))).map(new Throwables.Function<U, R, E>() {
                    @Override
                    public R apply(U u) throws E {
                        return func.apply(t, u);
                    }
                });
            }
        });
    }

    /**
     * 
     * @param <K>
     * @param b
     * @param keyMapper
     * @return
     */
    public <K> ExceptionalStream<Pair<T, T>, E> innerJoin(final Collection<? extends T> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper) {
        return innerJoin(b, keyMapper, Fnn.<T, T, E> pair());
    }

    /**
     * 
     * @param <K>
     * @param <R>
     * @param b
     * @param keyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <K, R> ExceptionalStream<R, E> innerJoin(final Collection<? extends T> b, final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.BiFunction<? super T, ? super T, R, ? extends E> func) {
        return innerJoin(b, keyMapper, keyMapper, func);
    }

    /**
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
    public <U, K, R> ExceptionalStream<R, E> innerJoin(final ExceptionalStream<? extends U, E> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper,
            final Throwables.BiFunction<? super T, ? super U, R, ? extends E> func) {
        assertNotClosed();

        checkArgNotNull(b, "stream");

        return flatMap(new Throwables.Function<T, ExceptionalStream<R, E>, E>() {
            private ListMultimap<K, U> rightKeyMap = null;

            @Override
            public ExceptionalStream<R, E> apply(final T t) throws E {
                if (rightKeyMap == null) {
                    rightKeyMap = (ListMultimap<K, U>) b.toMultimap(rightKeyMapper);
                }

                return ExceptionalStream.<U, E> of(rightKeyMap.get(leftKeyMapper.apply(t))).map(new Throwables.Function<U, R, E>() {
                    @Override
                    public R apply(U u) throws E {
                        return func.apply(t, u);
                    }
                });
            }
        });
    }

    /**
     *
     * @param <U>
     * @param b
     * @param predicate
     * @return
     */
    @IntermediateOp
    public <U> ExceptionalStream<Pair<T, U>, E> innerJoin(final Collection<? extends U> b,
            final Throwables.BiPredicate<? super T, ? super U, ? extends E> predicate) {
        assertNotClosed();

        return flatMap(new Throwables.Function<T, ExceptionalStream<Pair<T, U>, E>, E>() {
            @Override
            public ExceptionalStream<Pair<T, U>, E> apply(final T t) {
                return ExceptionalStream.<U, E> of(b).filter(new Throwables.Predicate<U, E>() {
                    @Override
                    public boolean test(final U u) throws E {
                        return predicate.test(t, u);
                    }
                }).map(new Throwables.Function<U, Pair<T, U>, E>() {
                    @Override
                    public Pair<T, U> apply(U u) {
                        return Pair.of(t, u);
                    }
                });
            }
        });
    }

    /**
     *
     * @param <K>
     * @param <U>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @return
     */
    @IntermediateOp
    public <U, K> ExceptionalStream<Pair<T, U>, E> fullJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper) {
        return fullJoin(b, leftKeyMapper, rightKeyMapper, Fnn.<T, U, E> pair());
    }

    /**
     *
     * @param <K>
     * @param <U>
     * @param <R>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <U, K, R> ExceptionalStream<R, E> fullJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper,
            final Throwables.BiFunction<? super T, ? super U, R, ? extends E> func) {
        assertNotClosed();

        final Map<U, U> joinedRights = new IdentityHashMap<>();

        return flatMap(new Throwables.Function<T, ExceptionalStream<R, E>, E>() {
            private ListMultimap<K, U> rightKeyMap = null;

            @Override
            public ExceptionalStream<R, E> apply(final T t) throws E {
                if (rightKeyMap == null) {
                    rightKeyMap = ListMultimap.from(b, rightKeyMapper);
                }

                final List<U> values = rightKeyMap.get(leftKeyMapper.apply(t));

                return N.isNullOrEmpty(values) ? ExceptionalStream.of(func.apply(t, (U) null))
                        : ExceptionalStream.<U, E> of(values).map(new Throwables.Function<U, R, E>() {
                            @Override
                            public R apply(U u) throws E {
                                joinedRights.put(u, u);

                                return func.apply(t, u);
                            }
                        });
            }
        }).append(ExceptionalStream.<U, E> of(b).filter(new Throwables.Predicate<U, E>() {
            @Override
            public boolean test(U u) {
                return joinedRights.containsKey(u) == false;
            }
        }).map(new Throwables.Function<U, R, E>() {
            @Override
            public R apply(U u) throws E {
                return func.apply((T) null, u);
            }
        }));
    }

    @SuppressWarnings("rawtypes")
    private static Throwables.Function HOLDER_VALUE_GETTER = new Throwables.Function<Holder<Object>, Object, RuntimeException>() {
        @Override
        public Object apply(Holder<Object> t) {
            return t.value();
        }
    };

    /**
     * 
     * @param <K>
     * @param b
     * @param keyMapper
     * @return
     */
    public <K> ExceptionalStream<Pair<T, T>, E> fullJoin(final Collection<? extends T> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper) {
        return fullJoin(b, keyMapper, Fnn.<T, T, E> pair());
    }

    /**
     * 
     * @param <K>
     * @param <R>
     * @param b
     * @param keyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <K, R> ExceptionalStream<R, E> fullJoin(final Collection<? extends T> b, final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.BiFunction<? super T, ? super T, R, ? extends E> func) {
        return fullJoin(b, keyMapper, keyMapper, func);
    }

    /**
     *
     * @param <K>
     * @param <U>
     * @param <R>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <U, K, R> ExceptionalStream<R, E> fullJoin(final ExceptionalStream<? extends U, E> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper,
            final Throwables.BiFunction<? super T, ? super U, R, ? extends E> func) {
        assertNotClosed();

        checkArgNotNull(b, "stream");

        final Map<U, U> joinedRights = new IdentityHashMap<>();
        final Holder<List<U>> holder = new Holder<>();

        return flatMap(new Throwables.Function<T, ExceptionalStream<R, E>, E>() {
            private List<U> c = null;
            private ListMultimap<K, U> rightKeyMap = null;

            @Override
            public ExceptionalStream<R, E> apply(final T t) throws E {
                if (rightKeyMap == null) {
                    c = (List<U>) b.toList();
                    rightKeyMap = ListMultimap.from(c, rightKeyMapper);
                    holder.setValue(c);
                }

                final List<U> values = rightKeyMap.get(leftKeyMapper.apply(t));

                return N.isNullOrEmpty(values) ? ExceptionalStream.of(func.apply(t, (U) null))
                        : ExceptionalStream.<U, E> of(values).map(new Throwables.Function<U, R, E>() {
                            @Override
                            public R apply(U u) throws E {
                                joinedRights.put(u, u);

                                return func.apply(t, u);
                            }
                        });
            }
        }).append(ExceptionalStream.<Holder<List<U>>, E> of(holder)
                .flattMap((Throwables.Function<Holder<List<U>>, List<U>, E>) HOLDER_VALUE_GETTER)
                .filter(new Throwables.Predicate<U, E>() {
                    @Override
                    public boolean test(U u) {
                        return joinedRights.containsKey(u) == false;
                    }
                })
                .map(new Throwables.Function<U, R, E>() {
                    @Override
                    public R apply(U u) throws E {
                        return func.apply((T) null, u);
                    }
                }));
    }

    /**
     *
     * @param <U>
     * @param b
     * @param predicate
     * @return
     */
    @IntermediateOp
    public <U> ExceptionalStream<Pair<T, U>, E> fullJoin(final Collection<? extends U> b,
            final Throwables.BiPredicate<? super T, ? super U, ? extends E> predicate) {
        assertNotClosed();

        final Map<U, U> joinedRights = new IdentityHashMap<>();

        return flatMap(new Throwables.Function<T, ExceptionalStream<Pair<T, U>, E>, E>() {
            @Override
            public ExceptionalStream<Pair<T, U>, E> apply(final T t) {
                return ExceptionalStream.<U, E> of(b).filter(new Throwables.Predicate<U, E>() {
                    @Override
                    public boolean test(final U u) throws E {
                        return predicate.test(t, u);
                    }
                }).map(new Throwables.Function<U, Pair<T, U>, E>() {
                    @Override
                    public Pair<T, U> apply(U u) {
                        joinedRights.put(u, u);

                        return Pair.of(t, u);
                    }
                }).appendIfEmpty(Pair.of(t, (U) null));
            }
        }).append(ExceptionalStream.<U, E> of(b).filter(new Throwables.Predicate<U, E>() {
            @Override
            public boolean test(U u) {
                return joinedRights.containsKey(u) == false;
            }
        }).map(new Throwables.Function<U, Pair<T, U>, E>() {
            @Override
            public Pair<T, U> apply(U u) {
                return Pair.of((T) null, u);
            }
        }));
    }

    /**
     *
     * @param <K>
     * @param <U>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @return
     */
    @IntermediateOp
    public <U, K> ExceptionalStream<Pair<T, U>, E> leftJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper) {
        return leftJoin(b, leftKeyMapper, rightKeyMapper, Fnn.<T, U, E> pair());
    }

    /**
     *
     * @param <K>
     * @param <U>
     * @param <R>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <U, K, R> ExceptionalStream<R, E> leftJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper,
            final Throwables.BiFunction<? super T, ? super U, R, ? extends E> func) {
        assertNotClosed();

        return flatMap(new Throwables.Function<T, ExceptionalStream<R, E>, E>() {
            private ListMultimap<K, U> rightKeyMap = null;

            @Override
            public ExceptionalStream<R, E> apply(final T t) throws E {
                if (rightKeyMap == null) {
                    rightKeyMap = ListMultimap.from(b, rightKeyMapper);
                }

                final List<U> values = rightKeyMap.get(leftKeyMapper.apply(t));

                return N.isNullOrEmpty(values) ? ExceptionalStream.<R, E> of(func.apply(t, (U) null))
                        : ExceptionalStream.<U, E> of(values).map(new Throwables.Function<U, R, E>() {
                            @Override
                            public R apply(U u) throws E {
                                return func.apply(t, u);
                            }
                        });
            }
        });
    }

    /**
     * 
     * @param <K>
     * @param b
     * @param keyMapper
     * @return
     */
    public <K> ExceptionalStream<Pair<T, T>, E> leftJoin(final Collection<? extends T> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper) {
        return leftJoin(b, keyMapper, Fnn.<T, T, E> pair());
    }

    /**
     * 
     * @param <K>
     * @param <R>
     * @param b
     * @param keyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <K, R> ExceptionalStream<R, E> leftJoin(final Collection<? extends T> b, final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.BiFunction<? super T, ? super T, R, ? extends E> func) {
        return leftJoin(b, keyMapper, keyMapper, func);
    }

    /**
     *
     * @param <K>
     * @param <U>
     * @param <R>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <U, K, R> ExceptionalStream<R, E> leftJoin(final ExceptionalStream<? extends U, E> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper,
            final Throwables.BiFunction<? super T, ? super U, R, ? extends E> func) {
        assertNotClosed();

        checkArgNotNull(b, "stream");

        return flatMap(new Throwables.Function<T, ExceptionalStream<R, E>, E>() {
            private ListMultimap<K, U> rightKeyMap = null;

            @Override
            public ExceptionalStream<R, E> apply(final T t) throws E {
                if (rightKeyMap == null) {
                    rightKeyMap = (ListMultimap<K, U>) b.toMultimap(rightKeyMapper);
                }

                final List<U> values = rightKeyMap.get(leftKeyMapper.apply(t));

                return N.isNullOrEmpty(values) ? ExceptionalStream.<R, E> of(func.apply(t, (U) null))
                        : ExceptionalStream.<U, E> of(values).map(new Throwables.Function<U, R, E>() {
                            @Override
                            public R apply(U u) throws E {
                                return func.apply(t, u);
                            }
                        });
            }
        });
    }

    /**
     *
     * @param <U>
     * @param b
     * @param predicate
     * @return
     */
    @IntermediateOp
    public <U> ExceptionalStream<Pair<T, U>, E> leftJoin(final Collection<? extends U> b,
            final Throwables.BiPredicate<? super T, ? super U, ? extends E> predicate) {
        assertNotClosed();

        return flatMap(new Throwables.Function<T, ExceptionalStream<Pair<T, U>, E>, E>() {
            @Override
            public ExceptionalStream<Pair<T, U>, E> apply(final T t) {
                return ExceptionalStream.<U, E> of(b).filter(new Throwables.Predicate<U, E>() {
                    @Override
                    public boolean test(final U u) throws E {
                        return predicate.test(t, u);
                    }
                }).map(new Throwables.Function<U, Pair<T, U>, E>() {
                    @Override
                    public Pair<T, U> apply(U u) {
                        return Pair.of(t, u);
                    }
                }).appendIfEmpty(Pair.of(t, (U) null));
            }
        });
    }

    /**
     *
     * @param <K>
     * @param <U>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @return
     */
    @IntermediateOp
    public <U, K> ExceptionalStream<Pair<T, U>, E> rightJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper) {
        return rightJoin(b, leftKeyMapper, rightKeyMapper, Fnn.<T, U, E> pair());
    }

    /**
     *
     * @param <K>
     * @param <U>
     * @param <R>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <U, K, R> ExceptionalStream<R, E> rightJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper,
            final Throwables.BiFunction<? super T, ? super U, R, ? extends E> func) {
        assertNotClosed();

        final Map<U, U> joinedRights = new IdentityHashMap<>();

        return flatMap(new Throwables.Function<T, ExceptionalStream<R, E>, E>() {
            private ListMultimap<K, U> rightKeyMap = null;

            @Override
            public ExceptionalStream<R, E> apply(final T t) throws E {
                if (rightKeyMap == null) {
                    rightKeyMap = ListMultimap.from(b, rightKeyMapper);
                }

                return ExceptionalStream.<U, E> of(rightKeyMap.get(leftKeyMapper.apply(t))).map(new Throwables.Function<U, R, E>() {
                    @Override
                    public R apply(U u) throws E {
                        joinedRights.put(u, u);

                        return func.apply(t, u);
                    }
                });
            }
        }).append(ExceptionalStream.<U, E> of(b).filter(new Throwables.Predicate<U, E>() {
            @Override
            public boolean test(U u) {
                return joinedRights.containsKey(u) == false;
            }
        }).map(new Throwables.Function<U, R, E>() {
            @Override
            public R apply(U u) throws E {
                return func.apply((T) null, u);
            }
        }));
    }

    /**
     * 
     * @param <K>
     * @param b
     * @param keyMapper
     * @return
     */
    public <K> ExceptionalStream<Pair<T, T>, E> rightJoin(final Collection<? extends T> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper) {
        return rightJoin(b, keyMapper, Fnn.<T, T, E> pair());
    }

    /**
     * 
     * @param <K>
     * @param <R>
     * @param b
     * @param keyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <K, R> ExceptionalStream<R, E> rightJoin(final Collection<? extends T> b, final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.BiFunction<? super T, ? super T, R, ? extends E> func) {
        return rightJoin(b, keyMapper, keyMapper, func);
    }

    /**
     *
     * @param <K>
     * @param <U>
     * @param <R>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <U, K, R> ExceptionalStream<R, E> rightJoin(final ExceptionalStream<? extends U, E> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper,
            final Throwables.BiFunction<? super T, ? super U, R, ? extends E> func) {
        assertNotClosed();

        checkArgNotNull(b, "stream");

        final Map<U, U> joinedRights = new IdentityHashMap<>();
        final Holder<List<U>> holder = new Holder<>();

        return flatMap(new Throwables.Function<T, ExceptionalStream<R, E>, E>() {
            private List<U> c = null;
            private ListMultimap<K, U> rightKeyMap = null;

            @Override
            public ExceptionalStream<R, E> apply(final T t) throws E {
                if (rightKeyMap == null) {
                    c = (List<U>) b.toList();
                    rightKeyMap = ListMultimap.from(c, rightKeyMapper);
                    holder.setValue(c);
                }

                return ExceptionalStream.<U, E> of(rightKeyMap.get(leftKeyMapper.apply(t))).map(new Throwables.Function<U, R, E>() {
                    @Override
                    public R apply(U u) throws E {
                        joinedRights.put(u, u);

                        return func.apply(t, u);
                    }
                });
            }
        }).append(ExceptionalStream.<Holder<List<U>>, E> of(holder)
                .flattMap((Throwables.Function<Holder<List<U>>, List<U>, E>) HOLDER_VALUE_GETTER)
                .filter(new Throwables.Predicate<U, E>() {
                    @Override
                    public boolean test(U u) {
                        return joinedRights.containsKey(u) == false;
                    }
                })
                .map(new Throwables.Function<U, R, E>() {
                    @Override
                    public R apply(U u) throws E {
                        return func.apply((T) null, u);
                    }
                }));
    }

    /**
     *
     * @param <U>
     * @param b
     * @param predicate
     * @return
     */
    @IntermediateOp
    public <U> ExceptionalStream<Pair<T, U>, E> rightJoin(final Collection<? extends U> b,
            final Throwables.BiPredicate<? super T, ? super U, ? extends E> predicate) {
        assertNotClosed();

        final Map<U, U> joinedRights = new IdentityHashMap<>();

        return flatMap(new Throwables.Function<T, ExceptionalStream<Pair<T, U>, E>, E>() {
            @Override
            public ExceptionalStream<Pair<T, U>, E> apply(final T t) {
                return ExceptionalStream.<U, E> of(b).filter(new Throwables.Predicate<U, E>() {
                    @Override
                    public boolean test(final U u) throws E {
                        return predicate.test(t, u);
                    }
                }).map(new Throwables.Function<U, Pair<T, U>, E>() {
                    @Override
                    public Pair<T, U> apply(U u) {
                        joinedRights.put(u, u);

                        return Pair.of(t, u);
                    }
                });
            }
        }).append(ExceptionalStream.<U, E> of(b).filter(new Throwables.Predicate<U, E>() {
            @Override
            public boolean test(U u) {
                return joinedRights.containsKey(u) == false;
            }
        }).map(new Throwables.Function<U, Pair<T, U>, E>() {
            @Override
            public Pair<T, U> apply(U u) {
                return Pair.of((T) null, u);
            }
        }));
    }

    /**
     * 
     * @param <U>
     * @param <K>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @return
     */
    @IntermediateOp
    public <U, K> ExceptionalStream<Pair<T, List<U>>, E> groupJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper) {
        return groupJoin(b, leftKeyMapper, rightKeyMapper, Fnn.<T, List<U>, E> pair());
    }

    /**
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
    public <U, K, R> ExceptionalStream<R, E> groupJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper,
            final Throwables.BiFunction<? super T, ? super List<U>, R, ? extends E> func) {
        assertNotClosed();

        final Throwables.Function<T, R, E> mapper = new Throwables.Function<T, R, E>() {
            private volatile boolean initialized = false;
            private volatile Map<K, List<U>> map = null;
            private List<U> val = null;

            @Override
            public R apply(T t) throws E {
                if (initialized == false) {
                    init();
                }

                val = map.get(leftKeyMapper.apply(t));

                if (val == null) {
                    return func.apply(t, new ArrayList<U>(0));
                } else {
                    return func.apply(t, val);
                }
            }

            private void init() throws E {
                if (initialized == false) {
                    initialized = true;

                    map = ExceptionalStream.<U, E> of(b).groupTo(rightKeyMapper);
                }
            }
        };

        return map(mapper);
    }

    /**
     * 
     * @param <K>
     * @param b
     * @param keyMapper
     * @return
     */
    public <K> ExceptionalStream<Pair<T, List<T>>, E> groupJoin(final Collection<? extends T> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper) {
        return groupJoin(b, keyMapper, Fnn.<T, List<T>, E> pair());
    }

    /**
     * 
     * @param <K>
     * @param <R>
     * @param b
     * @param keyMapper
     * @param func
     * @return
     */
    @IntermediateOp
    public <K, R> ExceptionalStream<R, E> groupJoin(final Collection<? extends T> b, final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.BiFunction<? super T, ? super List<T>, R, ? extends E> func) {
        return groupJoin(b, keyMapper, keyMapper, func);
    }

    /**
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
    public <U, K, R> ExceptionalStream<R, E> groupJoin(final ExceptionalStream<? extends U, E> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper,
            final Throwables.BiFunction<? super T, ? super List<U>, R, ? extends E> func) {
        assertNotClosed();

        checkArgNotNull(b, "stream");

        final Throwables.Function<T, R, E> mapper = new Throwables.Function<T, R, E>() {
            private volatile boolean initialized = false;
            private volatile Map<K, List<U>> map = null;
            private List<U> val = null;

            @Override
            public R apply(T t) throws E {
                if (initialized == false) {
                    init();
                }

                val = map.get(leftKeyMapper.apply(t));

                if (val == null) {
                    return func.apply(t, new ArrayList<U>(0));
                } else {
                    return func.apply(t, val);
                }
            }

            @SuppressWarnings("rawtypes")
            private void init() throws E {
                if (initialized == false) {
                    initialized = true;

                    map = (Map) b.groupTo(rightKeyMapper);
                }
            }
        };

        return map(mapper);
    }

    /**
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
    public <U, K> ExceptionalStream<Pair<T, U>, E> groupJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper, final Throwables.BinaryOperator<U, ? extends E> mergeFunction) {
        return groupJoin(b, leftKeyMapper, rightKeyMapper, mergeFunction, Fnn.<T, U, E> pair());
    }

    /**
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
    public <U, K, R> ExceptionalStream<R, E> groupJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper, final Throwables.BinaryOperator<U, ? extends E> mergeFunction,
            final Throwables.BiFunction<? super T, ? super U, R, ? extends E> func) {
        assertNotClosed();

        final Throwables.Function<T, R, E> mapper = new Throwables.Function<T, R, E>() {
            private volatile boolean initialized = false;
            private volatile Map<K, U> map = null;
            private U val = null;

            @Override
            public R apply(T t) throws E {
                if (initialized == false) {
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
                if (initialized == false) {
                    initialized = true;

                    map = ExceptionalStream.<U, E> of(b).toMap(rightKeyMapper, Fnn.<U, E> identity(), mergeFunction);
                }
            }
        };

        return map(mapper);
    }

    /**
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
    public <U, K, R> ExceptionalStream<R, E> groupJoin(final ExceptionalStream<? extends U, E> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper, final Throwables.BinaryOperator<U, ? extends E> mergeFunction,
            final Throwables.BiFunction<? super T, ? super U, R, ? extends E> func) {
        assertNotClosed();

        checkArgNotNull(b, "stream");

        final Throwables.Function<T, R, E> mapper = new Throwables.Function<T, R, E>() {
            private volatile boolean initialized = false;
            private volatile Map<K, U> map = null;
            private U val = null;

            @Override
            public R apply(T t) throws E {
                if (initialized == false) {
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
                if (initialized == false) {
                    initialized = true;

                    map = b.toMap(rightKeyMapper, Fnn.<U, E> identity(), mergeFunction);
                }
            }
        };

        return map(mapper);
    }

    /**
     * 
     * @param <U>
     * @param <K>
     * @param <A>
     * @param <D>
     * @param b
     * @param leftKeyMapper
     * @param rightKeyMapper
     * @param downstream
     * @return
     */
    @IntermediateOp
    public <U, K, A, D> ExceptionalStream<Pair<T, D>, E> groupJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper, final Collector<? super U, A, D> downstream) {
        return groupJoin(b, leftKeyMapper, rightKeyMapper, downstream, Fnn.<T, D, E> pair());
    }

    /**
     * 
     * @param <U>
     * @param <K>
     * @param <A>
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
    public <U, K, A, D, R> ExceptionalStream<R, E> groupJoin(final Collection<? extends U> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper, final Collector<? super U, A, D> downstream,
            final Throwables.BiFunction<? super T, ? super D, R, ? extends E> func) {
        assertNotClosed();

        final Throwables.Function<T, R, E> mapper = new Throwables.Function<T, R, E>() {
            private volatile boolean initialized = false;
            private volatile Map<K, D> map = null;
            private D val = null;

            @Override
            public R apply(T t) throws E {
                if (initialized == false) {
                    init();
                }

                val = map.get(leftKeyMapper.apply(t));

                if (val == null) {
                    return func.apply(t, ExceptionalStream.<U, E> empty().collect(downstream));
                } else {
                    return func.apply(t, val);
                }
            }

            private void init() throws E {
                if (initialized == false) {
                    initialized = true;

                    map = ExceptionalStream.<U, E> of(b).toMap(rightKeyMapper, Fnn.<U, E> identity(), downstream);
                }
            }
        };

        return map(mapper);
    }

    /**
     * 
     * @param <U>
     * @param <K>
     * @param <A>
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
    public <U, K, A, D, R> ExceptionalStream<R, E> groupJoin(final ExceptionalStream<? extends U, E> b,
            final Throwables.Function<? super T, ? extends K, ? extends E> leftKeyMapper,
            final Throwables.Function<? super U, ? extends K, ? extends E> rightKeyMapper, final Collector<? super U, A, D> downstream,
            final Throwables.BiFunction<? super T, ? super D, R, ? extends E> func) {
        assertNotClosed();

        checkArgNotNull(b, "stream");

        final Throwables.Function<T, R, E> mapper = new Throwables.Function<T, R, E>() {
            private volatile boolean initialized = false;
            private volatile Map<K, D> map = null;
            private D val = null;

            @Override
            public R apply(T t) throws E {
                if (initialized == false) {
                    init();
                }

                val = map.get(leftKeyMapper.apply(t));

                if (val == null) {
                    return func.apply(t, ExceptionalStream.<U, E> empty().collect(downstream));
                } else {
                    return func.apply(t, val);
                }
            }

            private void init() throws E {
                if (initialized == false) {
                    initialized = true;

                    map = b.toMap(rightKeyMapper, Fnn.<U, E> identity(), downstream);
                }
            }
        };

        return map(mapper);
    }

    /**
     *
     * @param <E2>
     * @param action
     * @throws E the e
     * @throws E2 the e2
     */
    @TerminalOp
    public <E2 extends Exception> void forEach(Throwables.Consumer<? super T, ? extends E2> action) throws E, E2 {
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
    public <E2 extends Exception> void forEachIndexed(Throwables.IndexedConsumer<? super T, ? extends E2> action) throws E, E2 {
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

    @TerminalOp
    @Beta
    public void forEachToBreak(final Throwables.BiConsumer<? super T, MutableBoolean, E> action) throws E {
        assertNotClosed();

        final MutableBoolean flagToBreak = MutableBoolean.of(false);

        final Throwables.Consumer<? super T, E> tmp = new Throwables.Consumer<T, E>() {
            @Override
            public void accept(T t) throws E {
                action.accept(t, flagToBreak);
            }
        };

        takeWhile(new Throwables.Predicate<T, E>() {
            @Override
            public boolean test(T value) {
                return flagToBreak.isFalse();
            }
        }).forEach(tmp);
    }

    @TerminalOp
    @Beta
    public void forEachToBreak(final MutableBoolean flagToBreak, final Throwables.Consumer<? super T, E> action) throws E {
        assertNotClosed();

        takeWhile(new Throwables.Predicate<T, E>() {
            @Override
            public boolean test(T value) {
                return flagToBreak.isFalse();
            }
        }).forEach(action);
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
    public <E2 extends Exception, E3 extends Exception> void forEach(final Throwables.Consumer<? super T, ? extends E2> action,
            final Throwables.Runnable<? extends E3> onComplete) throws E, E2, E3 {
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
    public <U, E2 extends Exception, E3 extends Exception> void forEach(
            final Throwables.Function<? super T, ? extends Collection<? extends U>, ? extends E2> flatMapper,
            final Throwables.BiConsumer<? super T, ? super U, ? extends E3> action) throws E, E2, E3 {
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
            final Throwables.Function<? super T, ? extends Collection<T2>, ? extends E2> flatMapper,
            final Throwables.Function<? super T2, ? extends Collection<T3>, ? extends E3> flatMapper2,
            final Throwables.TriConsumer<? super T, ? super T2, ? super T3, ? extends E4> action) throws E, E2, E3, E4 {
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
    public <E2 extends Exception> void forEachPair(final Throwables.BiConsumer<? super T, ? super T, ? extends E2> action) throws E, E2 {
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
    public <E2 extends Exception> void forEachPair(final Throwables.BiConsumer<? super T, ? super T, ? extends E2> action, final int increment) throws E, E2 {
        assertNotClosed();

        final int windowSize = 2;
        checkArgPositive(increment, "increment");

        try {
            boolean isFirst = true;
            T prev = null;

            while (elements.hasNext()) {
                if (increment > windowSize && isFirst == false) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.next();
                    }

                    if (elements.hasNext() == false) {
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
    public <E2 extends Exception> void forEachTriple(final Throwables.TriConsumer<? super T, ? super T, ? super T, ? extends E2> action) throws E, E2 {
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
    public <E2 extends Exception> void forEachTriple(final Throwables.TriConsumer<? super T, ? super T, ? super T, ? extends E2> action, final int increment)
            throws E, E2 {
        assertNotClosed();

        final int windowSize = 3;
        checkArgPositive(increment, "increment");

        try {
            boolean isFirst = true;
            T prev = null;
            T prev2 = null;

            while (elements.hasNext()) {
                if (increment > windowSize && isFirst == false) {
                    int skipNum = increment - windowSize;

                    while (skipNum-- > 0 && elements.hasNext()) {
                        elements.next();
                    }

                    if (elements.hasNext() == false) {
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
     * @param comparator
     * @return
     * @throws E the e
     */
    @TerminalOp
    public Optional<T> min(Comparator<? super T> comparator) throws E {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
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
            if (elements.hasNext() == false) {
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
     * @param predicate
     * @return
     * @throws E the e
     */
    @TerminalOp
    public boolean anyMatch(final Throwables.Predicate<? super T, ? extends E> predicate) throws E {
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
     * @param predicate
     * @return
     * @throws E the e
     */
    @TerminalOp
    public boolean allMatch(final Throwables.Predicate<? super T, ? extends E> predicate) throws E {
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.next()) == false) {
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
     * @param predicate
     * @return
     * @throws E the e
     */
    @TerminalOp
    public boolean noneMatch(final Throwables.Predicate<? super T, ? extends E> predicate) throws E {
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
     * @param atLeast
     * @param atMost
     * @param predicate
     * @return
     * @throws E
     */
    @TerminalOp
    public boolean nMatch(final long atLeast, final long atMost, final Throwables.Predicate<? super T, ? extends E> predicate) throws E {
        assertNotClosed();

        checkArgNotNegative(atLeast, "atLeast");
        checkArgNotNegative(atMost, "atMost");
        checkArgument(atLeast <= atMost, "'atLeast' must be <= 'atMost'");

        long cnt = 0;

        try {
            while (elements.hasNext()) {
                if (predicate.test(elements.next())) {
                    if (++cnt > atMost) {
                        return false;
                    }
                }
            }
        } finally {
            close();
        }

        return cnt >= atLeast && cnt <= atMost;
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

                return filter(new Throwables.Predicate<T, E>() {
                    @Override
                    public boolean test(T t) {
                        return set.contains(t);
                    }
                }).distinct().limit(distinctCount).count() == distinctCount;
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

                return anyMatch(new Throwables.Predicate<T, E>() {

                    @Override
                    public boolean test(T t) {
                        return set.contains(t);
                    }

                });
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

                return anyMatch(new Throwables.Predicate<T, E>() {
                    @Override
                    public boolean test(T t) {
                        return set.contains(t);
                    }
                });
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
                if (set.add(elements.next()) == false) {
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
            if (elements.hasNext() == false) {
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
            if (elements.hasNext() == false) {
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
            if (elements.hasNext() == false) {
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
    @TerminalOp
    @Beta
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
     * @throws DuplicatedResultException if there are more than one elements.
     * @throws E the e
     */
    @TerminalOp
    public Optional<T> onlyOne() throws DuplicatedResultException, E {
        assertNotClosed();

        try {
            Optional<T> result = Optional.empty();

            if (elements.hasNext()) {
                result = Optional.of(elements.next());

                if (elements.hasNext()) {
                    throw new DuplicatedResultException("There are at least two elements: " + Strings.concat(result.get(), ", ", elements.next()));
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

    private Object[] toArray(final boolean closeStream) throws E {
        assertNotClosed();

        try {
            return toList().toArray();
        } finally {
            if (closeStream) {
                close();
            }
        }
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
    public <R> R toListAndThen(Throwables.Function<? super List<T>, R, E> func) throws E {
        assertNotClosed();

        return func.apply(toList());
    }

    @TerminalOp
    public <R> R toSetAndThen(Throwables.Function<? super Set<T>, R, E> func) throws E {
        assertNotClosed();

        return func.apply(toSet());
    }

    @TerminalOp
    public <R, CC extends Collection<T>> R toCollectionAndThen(Supplier<? extends CC> supplier, Throwables.Function<? super CC, R, E> func) throws E {
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
    public <K, V> Map<K, V> toMap(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.Function<? super T, ? extends V, ? extends E> valueMapper) throws E, IllegalStateException {
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
    public <K, V, M extends Map<K, V>> M toMap(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.Function<? super T, ? extends V, ? extends E> valueMapper, final Supplier<? extends M> mapFactory)
            throws E, IllegalStateException {
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
    public <K, V> Map<K, V> toMap(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.Function<? super T, ? extends V, ? extends E> valueMapper, final Throwables.BinaryOperator<V, ? extends E> mergeFunction)
            throws E {
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
    public <K, V, M extends Map<K, V>> M toMap(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.Function<? super T, ? extends V, ? extends E> valueMapper, final Throwables.BinaryOperator<V, ? extends E> mergeFunction,
            final Supplier<? extends M> mapFactory) throws E {
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
     * @param <K> the key type
     * @param <A>
     * @param <D>
     * @param keyMapper
     * @param downstream
     * @return
     * @throws E the e
     */
    @TerminalOp
    public <K, A, D> Map<K, D> toMap(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper, final Collector<? super T, A, D> downstream)
            throws E {
        return toMap(keyMapper, downstream, Suppliers.<K, D> ofMap());
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
    public <K, A, D, M extends Map<K, D>> M toMap(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Collector<? super T, A, D> downstream, final Supplier<? extends M> mapFactory) throws E {
        return toMap(keyMapper, Fnn.<T, E> identity(), downstream, mapFactory);
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
    public <K, V, A, D> Map<K, D> toMap(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.Function<? super T, ? extends V, ? extends E> valueMapper, final Collector<? super V, A, D> downstream) throws E {
        return toMap(keyMapper, valueMapper, downstream, Suppliers.<K, D> ofMap());
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
    public <K, V, A, D, M extends Map<K, D>> M toMap(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.Function<? super T, ? extends V, ? extends E> valueMapper, final Collector<? super V, A, D> downstream,
            final Supplier<? extends M> mapFactory) throws E {
        assertNotClosed();

        checkArgNotNull(keyMapper, "keyMapper");
        checkArgNotNull(valueMapper, "valueMapper");
        checkArgNotNull(downstream, "downstream");
        checkArgNotNull(mapFactory, "mapFactory");

        try {
            final Supplier<A> downstreamSupplier = downstream.supplier();
            final BiConsumer<A, ? super V> downstreamAccumulator = downstream.accumulator();
            final Function<A, D> downstreamFinisher = downstream.finisher();

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
     * @param <K> the key type
     * @param keyMapper
     * @return
     * @throws E the e
     * @see Collectors#groupingBy(Function)
     */
    @TerminalOp
    public <K> Map<K, List<T>> groupTo(Throwables.Function<? super T, ? extends K, ? extends E> keyMapper) throws E {
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
    public <K, M extends Map<K, List<T>>> M groupTo(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Supplier<? extends M> mapFactory) throws E {
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
    public <K, V> Map<K, List<V>> groupTo(Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            Throwables.Function<? super T, ? extends V, ? extends E> valueMapper) throws E {
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
    public <K, V, M extends Map<K, List<V>>> M groupTo(Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            Throwables.Function<? super T, ? extends V, ? extends E> valueMapper, Supplier<? extends M> mapFactory) throws E {
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

                if (result.containsKey(key) == false) {
                    result.put(key, new ArrayList<V>());
                }

                result.get(key).add(valueMapper.apply(next));
            }

            return result;
        } finally {
            close();
        }
    }

    @TerminalOp
    public <K> ListMultimap<K, T> toMultimap(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper) throws E {
        return toMultimap(keyMapper, Suppliers.<K, T> ofListMultimap());
    }

    @TerminalOp
    public <K, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            Supplier<? extends M> mapFactory) throws E {
        final Throwables.Function<T, T, E> valueMapper = Fnn.identity();

        return toMultimap(keyMapper, valueMapper, mapFactory);
    }

    @TerminalOp
    public <K, V> ListMultimap<K, V> toMultimap(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.Function<? super T, ? extends V, ? extends E> valueMapper) throws E {
        return toMultimap(keyMapper, valueMapper, Suppliers.<K, V> ofListMultimap());
    }

    @TerminalOp
    public <K, V, C extends Collection<V>, M extends Multimap<K, V, C>> M toMultimap(final Throwables.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Throwables.Function<? super T, ? extends V, ? extends E> valueMapper, Supplier<? extends M> mapFactory) throws E {
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
    public OptionalLong sumInt(Throwables.ToIntFunction<? super T, ? extends E> func) throws E {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return OptionalLong.empty();
            }

            long sum = 0;

            while (elements.hasNext()) {
                sum += func.applyAsInt(elements.next());
            }

            return OptionalLong.of(sum);
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
    public OptionalLong sumLong(Throwables.ToLongFunction<? super T, ? extends E> func) throws E {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return OptionalLong.empty();
            }

            long sum = 0;

            while (elements.hasNext()) {
                sum += func.applyAsLong(elements.next());
            }

            return OptionalLong.of(sum);
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
    public OptionalDouble sumDouble(Throwables.ToDoubleFunction<? super T, ? extends E> func) throws E {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return OptionalDouble.empty();
            }

            final KahanSummation summation = new KahanSummation();

            while (elements.hasNext()) {
                summation.add(func.applyAsDouble(elements.next()));
            }

            return OptionalDouble.of(summation.sum());
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
    public OptionalDouble averageInt(Throwables.ToIntFunction<? super T, ? extends E> func) throws E {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return OptionalDouble.empty();
            }

            long sum = 0;
            long count = 0;

            while (elements.hasNext()) {
                sum += func.applyAsInt(elements.next());
                count++;
            }

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
    public OptionalDouble averageLong(Throwables.ToLongFunction<? super T, ? extends E> func) throws E {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return OptionalDouble.empty();
            }

            long sum = 0;
            long count = 0;

            while (elements.hasNext()) {
                sum += func.applyAsLong(elements.next());
                count++;
            }

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
    public OptionalDouble averageDouble(Throwables.ToDoubleFunction<? super T, ? extends E> func) throws E {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
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
    public <E2 extends Exception> Optional<T> reduce(Throwables.BinaryOperator<T, ? extends E2> accumulator) throws E, E2 {
        assertNotClosed();

        checkArgNotNull(accumulator, "accumulator");

        try {
            if (elements.hasNext() == false) {
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
    public <U, E2 extends Exception> U reduce(final U identity, final Throwables.BiFunction<U, ? super T, U, ? extends E2> accumulator) throws E, E2 {
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
    public <R, E2 extends Exception, E3 extends Exception> R collect(final Throwables.Supplier<R, ? extends E2> supplier,
            final Throwables.BiConsumer<? super R, ? super T, ? extends E3> accumulator) throws E, E2, E3 {
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
    public <R, RR, E2 extends Exception, E3 extends Exception, E4 extends Exception> RR collect(final Throwables.Supplier<R, ? extends E2> supplier,
            final Throwables.BiConsumer<? super R, ? super T, ? extends E3> accumulator,
            final Throwables.Function<? super R, ? extends RR, ? extends E4> finisher) throws E, E2, E3, E4 {
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
            final BiConsumer<A, ? super T> accumulator = collector.accumulator();

            while (elements.hasNext()) {
                accumulator.accept(container, elements.next());
            }

            return collector.finisher().apply(container);
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
    public <R, A> R collect(java.util.stream.Collector<? super T, A, R> collector) throws E {
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
            final Throwables.Function<? super R, ? extends RR, ? extends E2> func) throws E, E2 {
        assertNotClosed();

        checkArgNotNull(collector, "collector");
        checkArgNotNull(func, "func");

        return func.apply(collect(collector));
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
    public <R, RR, A, E2 extends Exception> RR collectAndThen(final java.util.stream.Collector<? super T, A, R> collector,
            final Throwables.Function<? super R, ? extends RR, ? extends E2> func) throws E, E2 {
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

    @TerminalOp
    @Beta
    public void println() throws E {
        N.println(join(", ", "[", "]"));
    }

    @IntermediateOp
    public Stream<T> unchecked() {
        assertNotClosed();

        if (N.isNullOrEmpty(this.closeHandlers)) {
            return Stream.of(newObjIteratorEx(elements));
        } else {
            return Stream.of(newObjIteratorEx(elements)).onClose(new Runnable() {
                @Override
                public void run() {
                    try {
                        ExceptionalStream.this.close();
                    } catch (Exception e) {
                        throw N.toRuntimeException(e);
                    }
                }
            });
        }
    }

    public java.util.stream.Stream<T> toJdkStream() {
        assertNotClosed();

        final Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(newObjIteratorEx(elements), Spliterator.ORDERED);

        if (N.isNullOrEmpty(closeHandlers)) {
            return StreamSupport.stream(() -> spliterator, Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL, false);
        } else {
            return StreamSupport.stream(() -> spliterator, Spliterator.ORDERED | Spliterator.IMMUTABLE | Spliterator.NONNULL, false).onClose(new Runnable() {
                @Override
                public void run() {
                    try {
                        ExceptionalStream.this.close();
                    } catch (Exception e) {
                        throw N.toRuntimeException(e);
                    }
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
    //        Deque<Throwables.Runnable<? extends E2>> newCloseHandlers = null;
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

    /**
     *
     * @param <SS>
     * @param transfer
     * @return
     * @throws E the e
     */
    @IntermediateOp
    @Beta
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
     *
     *
     * @param action a terminal operation should be called.
     * @return
     */
    @Beta
    public ContinuableFuture<Void> asyncRun(final Throwables.Consumer<? super ExceptionalStream<T, E>, ? extends E> action) {
        assertNotClosed();

        checkArgNotNull(action, "action");

        return N.asyncExecute(new Throwables.Runnable<E>() {
            @Override
            public void run() throws E {
                action.accept(ExceptionalStream.this);
            }
        });
    }

    /**
     *
     * @param action a terminal operation should be called.
     * @param executor
     * @return
     */
    @Beta
    public ContinuableFuture<Void> asyncRun(final Throwables.Consumer<? super ExceptionalStream<T, E>, ? extends E> action, final Executor executor) {
        assertNotClosed();

        checkArgNotNull(action, "action");
        checkArgNotNull(executor, "executor");

        return ContinuableFuture.run(new Throwables.Runnable<E>() {
            @Override
            public void run() throws E {
                action.accept(ExceptionalStream.this);
            }
        }, executor);
    }

    /**
     *
     * @param <R>
     * @param action a terminal operation should be called.
     * @return
     */
    @Beta
    public <R> ContinuableFuture<R> asyncCall(final Throwables.Function<? super ExceptionalStream<T, E>, R, ? extends E> action) {
        assertNotClosed();

        checkArgNotNull(action, "action");

        return N.asyncExecute(new Callable<R>() {
            @Override
            public R call() throws Exception {
                return action.apply(ExceptionalStream.this);
            }
        });
    }

    /**
     *
     * @param <R>
     * @param action a terminal operation should be called.
     * @param executor
     * @return
     */
    @Beta
    public <R> ContinuableFuture<R> asyncCall(final Throwables.Function<? super ExceptionalStream<T, E>, R, ? extends E> action, final Executor executor) {
        assertNotClosed();

        checkArgNotNull(action, "action");
        checkArgNotNull(executor, "executor");

        return ContinuableFuture.call(new Callable<R>() {
            @Override
            public R call() throws Exception {
                return action.apply(ExceptionalStream.this);
            }
        }, executor);
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
     * @throws E the e
     */
    @TerminalOp
    @Override
    public synchronized void close() {
        if (isClosed) {
            return;
        }

        if (N.isNullOrEmpty(closeHandlers)) {
            isClosed = true;
            return;
        }

        //    // Only mark the stream closed if closeHandlers are not empty.
        //    if (isClosed || N.isNullOrEmpty(closeHandlers)) {
        //        return;
        //    }

        logger.info("Closing ExceptionalStream");

        isClosed = true;

        close(closeHandlers);
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
            throw N.toRuntimeException(ex);
        }
    }

    ExceptionalIterator<T, E> iterator() {
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
                    throw N.toRuntimeException(e);
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
                    throw N.toRuntimeException(e);
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
                    throw N.toRuntimeException(e);
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
                    throw N.toRuntimeException(e);
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
                    throw N.toRuntimeException(e);
                }
            }
        }
    }

    ObjIteratorEx<T> newObjIteratorEx(final ExceptionalIterator<T, E> elements) {
        return new ObjIteratorEx<T>() {
            @Override
            public boolean hasNext() {
                try {
                    return elements.hasNext();
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }

            @Override
            public T next() {
                try {
                    return elements.next();
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }

            @Override
            public void advance(long n) {
                N.checkArgNotNegative(n, "n");

                try {
                    elements.advance(n);
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }

            @Override
            public long count() {
                try {
                    return elements.count();
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
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
        return obj == null || obj.getClass().isArray() == false ? obj : Wrapper.of(obj);
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

    /**
     * The Class ExceptionalIterator.
     *
     * @param <T>
     * @param <E>
     */
    @com.landawn.abacus.annotation.Immutable
    static abstract class ExceptionalIterator<T, E extends Exception> implements Immutable {

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

        public static <T, E extends Exception> ExceptionalIterator<T, E> wrap(final Iterator<? extends T> iter) {
            if (iter == null) {
                return EMPTY;
            }

            return new ExceptionalIterator<T, E>() {
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

        /**
         * Lazy evaluation.
         *
         * @param <T>
         * @param <E>
         * @param iteratorSupplier
         * @return
         */
        public static <T, E extends Exception> ExceptionalIterator<T, E> of(final Throwables.Supplier<ExceptionalIterator<T, E>, E> iteratorSupplier) {
            N.checkArgNotNull(iteratorSupplier, "iteratorSupplier");

            return new ExceptionalIterator<T, E>() {
                private ExceptionalIterator<T, E> iter = null;
                private boolean isInitialized = false;

                @Override
                public boolean hasNext() throws E {
                    if (isInitialized == false) {
                        init();
                    }

                    return iter.hasNext();
                }

                @Override
                public T next() throws E {
                    if (isInitialized == false) {
                        init();
                    }

                    return iter.next();
                }

                @Override
                public void advance(long n) throws E {
                    N.checkArgNotNegative(n, "n");

                    if (isInitialized == false) {
                        init();
                    }

                    iter.advance(n);
                }

                @Override
                public long count() throws E {
                    if (isInitialized == false) {
                        init();
                    }

                    return iter.count();
                }

                @Override
                public void close() throws E {
                    if (isInitialized == false) {
                        init();
                    }

                    iter.close();
                }

                private void init() throws E {
                    if (isInitialized == false) {
                        isInitialized = true;
                        iter = iteratorSupplier.get();
                    }
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
        public static <T, E extends Exception> ExceptionalIterator<T, E> oF(final Throwables.Supplier<T[], E> arraySupplier) {
            N.checkArgNotNull(arraySupplier, "arraySupplier");

            return new ExceptionalIterator<T, E>() {
                private T[] a;
                private int len;
                private int position = 0;
                private boolean isInitialized = false;

                @Override
                public boolean hasNext() throws E {
                    if (isInitialized == false) {
                        init();
                    }

                    return position < len;
                }

                @Override
                public T next() throws E {
                    if (isInitialized == false) {
                        init();
                    }

                    if (position >= len) {
                        throw new NoSuchElementException();
                    }

                    return a[position++];
                }

                @Override
                public long count() throws E {
                    if (isInitialized == false) {
                        init();
                    }

                    return len - position;
                }

                @Override
                public void advance(long n) throws E {
                    N.checkArgNotNegative(n, "n");

                    if (isInitialized == false) {
                        init();
                    }

                    if (n > len - position) {
                        position = len;
                    } else {
                        position += n;
                    }

                }

                private void init() throws E {
                    if (isInitialized == false) {
                        isInitialized = true;
                        a = arraySupplier.get();
                        len = N.len(a);
                    }
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

    public static final class StreamE<T, E extends Exception> extends ExceptionalStream<T, E> {

        StreamE(ExceptionalIterator<T, E> iter, boolean sorted, Comparator<? super T> comparator, Deque<Throwables.Runnable<? extends E>> closeHandlers) {
            super(iter, sorted, comparator, closeHandlers);
        }
    }

    /**
     * Mostly it's for android.
     * 
     * @see {@code ExceptionalStream<T, RuntimeException>}
     * 
     * @deprecated Mostly it's for android.
     */
    @Deprecated
    @Beta
    public static final class StreamR extends Seq {
        private StreamR() {
            // singleton for utility class.
        }
    }

    /**
     * Mostly it's for android.
     * 
     * @see {@code ExceptionalStream<T, RuntimeException>}
     * 
     * @deprecated Mostly it's for android.
     */
    @Deprecated
    @Beta
    static class Seq {
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

        public static ExceptionalStream<Integer, RuntimeException> of(final int[] a) {
            return ExceptionalStream.<RuntimeException> of(a);
        }

        public static ExceptionalStream<Long, RuntimeException> of(final long[] a) {
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

        public static <T> ExceptionalStream<T, RuntimeException> of(final Throwables.Supplier<Collection<? extends T>, RuntimeException> supplier) {
            return ExceptionalStream.<T, RuntimeException> of(supplier);
        }

        public static <T> ExceptionalStream<T, RuntimeException> from(
                final Throwables.Supplier<ExceptionalStream<? extends T, ? extends RuntimeException>, RuntimeException> supplier) {
            return ExceptionalStream.<T, RuntimeException> from(supplier);
        }

        public static <K> ExceptionalStream<K, RuntimeException> ofKeys(final Map<K, ?> map) {
            return ExceptionalStream.<K, RuntimeException> ofKeys(map);
        }

        public static <K, V> ExceptionalStream<K, RuntimeException> ofKeys(final Map<K, V> map,
                final Throwables.Predicate<? super V, RuntimeException> valueFilter) {
            return ExceptionalStream.<K, V, RuntimeException> ofKeys(map, valueFilter);
        }

        public static <V> ExceptionalStream<V, RuntimeException> ofValues(final Map<?, V> map) {
            return ExceptionalStream.<V, RuntimeException> ofValues(map);
        }

        public static <K, V> ExceptionalStream<V, RuntimeException> ofValues(final Map<K, V> map,
                final Throwables.Predicate<? super K, RuntimeException> keyFilter) {
            return ExceptionalStream.<K, V, RuntimeException> ofValues(map, keyFilter);
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

        @SafeVarargs
        public static <T> ExceptionalStream<T, RuntimeException> concat(final T[]... a) {
            return ExceptionalStream.<T, RuntimeException> concat(a);
        }

        @SafeVarargs
        public static <T> ExceptionalStream<T, RuntimeException> concat(final Collection<? extends T>... a) {
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

        public static <A, B, T> ExceptionalStream<T, RuntimeException> zip(final Collection<? extends A> a, final Collection<? extends B> b,
                final Throwables.BiFunction<? super A, ? super B, T, RuntimeException> zipFunction) {
            return ExceptionalStream.<A, B, T, RuntimeException> zip(a, b, zipFunction);
        }

        public static <A, B, C, T> ExceptionalStream<T, RuntimeException> zip(final Collection<? extends A> a, final Collection<? extends B> b,
                final Collection<? extends C> c, final Throwables.TriFunction<? super A, ? super B, ? super C, T, RuntimeException> zipFunction) {
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

        public static <A, B, T> ExceptionalStream<T, RuntimeException> zip(final Collection<? extends A> a, final Collection<? extends B> b,
                final A valueForNoneA, final B valueForNoneB, final Throwables.BiFunction<? super A, ? super B, T, RuntimeException> zipFunction) {
            return ExceptionalStream.<A, B, T, RuntimeException> zip(a, b, valueForNoneA, valueForNoneB, zipFunction);
        }

        public static <A, B, C, T> ExceptionalStream<T, RuntimeException> zip(final Collection<? extends A> a, final Collection<? extends B> b,
                final Collection<? extends C> c, final A valueForNoneA, final B valueForNoneB, final C valueForNoneC,
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

        public static <T> ExceptionalStream<T, RuntimeException> merge(final Collection<? extends T> a, final Collection<? extends T> b,
                final Throwables.BiFunction<? super T, ? super T, MergeResult, RuntimeException> nextSelector) {
            return ExceptionalStream.<T, RuntimeException> merge(a, b, nextSelector);
        }

        public static <T> ExceptionalStream<T, RuntimeException> merge(final Collection<? extends T> a, final Collection<? extends T> b,
                final Collection<? extends T> c, final Throwables.BiFunction<? super T, ? super T, MergeResult, RuntimeException> nextSelector) {
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
