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
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executor;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.exception.DuplicatedResultException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.Fn.Factory;
import com.landawn.abacus.util.Fn.Fnn;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.StringUtil.Strings;
import com.landawn.abacus.util.u.Holder;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.stream.BaseStream;
import com.landawn.abacus.util.stream.Collector;
import com.landawn.abacus.util.stream.Collectors;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

// TODO: Auto-generated Javadoc
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
@SequentialOnly
public class ExceptionalStream<T, E extends Exception> implements AutoCloseable {

    /** The Constant logger. */
    static final Logger logger = LoggerFactory.getLogger(ExceptionalStream.class);

    static final Random RAND = new SecureRandom();

    /** The elements. */
    private final ExceptionalIterator<T, E> elements;

    /** The sorted. */
    private final boolean sorted;

    /** The comparator. */
    private final Comparator<? super T> comparator;

    /** The close handlers. */
    private final Deque<Try.Runnable<? extends E>> closeHandlers;

    /** The is closed. */
    private boolean isClosed = false;

    /**
     * Instantiates a new exceptional stream.
     *
     * @param iter
     */
    ExceptionalStream(final ExceptionalIterator<T, E> iter) {
        this(iter, false, null, null);
    }

    /**
     * Instantiates a new exceptional stream.
     *
     * @param iter
     * @param closeHandlers
     */
    ExceptionalStream(final ExceptionalIterator<T, E> iter, final Deque<Try.Runnable<? extends E>> closeHandlers) {
        this(iter, false, null, closeHandlers);
    }

    /**
     * Instantiates a new exceptional stream.
     *
     * @param iter
     * @param sorted
     * @param comparator
     * @param closeHandlers
     */
    ExceptionalStream(final ExceptionalIterator<T, E> iter, final boolean sorted, final Comparator<? super T> comparator,
            final Deque<Try.Runnable<? extends E>> closeHandlers) {
        this.elements = iter;
        this.sorted = sorted;
        this.comparator = comparator;
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
            public void skip(long n) throws E {
                N.checkArgNotNegative(n, "n");

                if (n > len - position) {
                    position = len;
                }

                position += n;
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

        return newStream(ExceptionalIterator.wrap(iter));
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
            public void skip(long n) throws E {
                N.checkArgNotNegative(n, "n");

                if (iter == null) {
                    s = s.skip(n);
                } else {
                    super.skip(n);
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

        return newStream(iter).onClose(new Try.Runnable<E>() {
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
     * @param c
     * @param exceptionType
     * @return
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> of(final Collection<? extends T> c, final Class<E> exceptionType) {
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
    public static <T, E extends Exception> ExceptionalStream<T, E> of(final Iterator<? extends T> iter, final Class<E> exceptionType) {
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
    public static <T, E extends Exception> ExceptionalStream<T, E> of(final Iterable<? extends T> iterable, final Class<E> exceptionType) {
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
    public static <K, V, E extends Exception> ExceptionalStream<Map.Entry<K, V>, E> of(final Map<K, V> m, final Class<E> exceptionType) {
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
    public static <T, E extends Exception> ExceptionalStream<T, E> of(final Stream<? extends T> stream, final Class<E> exceptionType) {
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

        return newStream(new ExceptionalIterator<Integer, E>() {
            private final int len = a.length;
            private int idx = 0;

            @Override
            public boolean hasNext() throws E {
                return idx < len;
            }

            @Override
            public Integer next() throws E {
                return a[idx++];
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

        return newStream(new ExceptionalIterator<Long, E>() {
            private final int len = a.length;
            private int idx = 0;

            @Override
            public boolean hasNext() throws E {
                return idx < len;
            }

            @Override
            public Long next() throws E {
                return a[idx++];
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

        return newStream(new ExceptionalIterator<Double, E>() {
            private final int len = a.length;
            private int idx = 0;

            @Override
            public boolean hasNext() throws E {
                return idx < len;
            }

            @Override
            public Double next() throws E {
                return a[idx++];
            }
        });
    }

    public static <K, E extends Exception> ExceptionalStream<K, E> ofKeys(final Map<K, ?> map) {
        if (N.isNullOrEmpty(map)) {
            return empty();
        }

        return of(map.keySet());
    }

    public static <V, E extends Exception> ExceptionalStream<V, E> ofValues(final Map<?, V> map) {
        if (N.isNullOrEmpty(map)) {
            return empty();
        }

        return of(map.values());
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param hasNext
     * @param next
     * @return
     */
    public static <T, E extends Exception> ExceptionalStream<T, E> iterate(final Try.BooleanSupplier<? extends E> hasNext,
            final Try.Supplier<? extends T, E> next) {
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
    public static <T, E extends Exception> ExceptionalStream<T, E> iterate(final T init, final Try.BooleanSupplier<? extends E> hasNext,
            final Try.UnaryOperator<T, ? extends E> f) {
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
    public static <T, E extends Exception> ExceptionalStream<T, E> iterate(final T init, final Try.Predicate<? super T, ? extends E> hasNext,
            final Try.UnaryOperator<T, ? extends E> f) {
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
    public static <T, E extends Exception> ExceptionalStream<T, E> iterate(final T init, final Try.UnaryOperator<T, ? extends E> f) {
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

    public static <T, E extends Exception> ExceptionalStream<T, E> generate(final Try.Supplier<T, E> supplier) {
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

    public static <T, E extends Exception> ExceptionalStream<T, E> repeat(final T value, final long n) {
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

                return value;
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

        return newStream(iter).onClose(new Try.Runnable<IOException>() {
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

        return newStream(iter).onClose(new Try.Runnable<IOException>() {
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
        return ExceptionalIterator.of(new Try.Supplier<ExceptionalIterator<String, IOException>, IOException>() {
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

    /**
     * It's user's responsibility to close the input <code>resultSet</code> after the stream is finished.
     *
     * @param resultSet
     * @return
     */
    public static ExceptionalStream<Object[], SQLException> rows(final ResultSet resultSet) {
        return rows(Object[].class, resultSet);
    }

    /**
     *
     * @param resultSet
     * @param closeResultSet
     * @return
     * @deprecated
     */
    @Deprecated
    static ExceptionalStream<Object[], SQLException> rows(final ResultSet resultSet, final boolean closeResultSet) {
        return rows(Object[].class, resultSet, closeResultSet);
    }

    /**
     * It's user's responsibility to close the input <code>resultSet</code> after the stream is finished.
     *
     * @param <T>
     * @param targetClass Array/List/Map or Entity with getter/setter methods.
     * @param resultSet
     * @return
     */
    public static <T> ExceptionalStream<T, SQLException> rows(final Class<T> targetClass, final ResultSet resultSet) {
        N.checkArgNotNull(targetClass, "targetClass");
        N.checkArgNotNull(resultSet, "resultSet");

        return rows(resultSet, InternalJdbcUtil.to(targetClass));
    }

    /**
     *
     * @param <T>
     * @param targetClass Array/List/Map or Entity with getter/setter methods.
     * @param resultSet
     * @param closeResultSet
     * @return
     * @deprecated
     */
    @Deprecated
    static <T> ExceptionalStream<T, SQLException> rows(final Class<T> targetClass, final ResultSet resultSet, final boolean closeResultSet) {
        N.checkArgNotNull(targetClass, "targetClass");
        N.checkArgNotNull(resultSet, "resultSet");

        if (closeResultSet) {
            return rows(targetClass, resultSet).onClose(new Try.Runnable<SQLException>() {
                @Override
                public void run() throws SQLException {
                    InternalJdbcUtil.closeQuietly(resultSet);
                }
            });
        } else {
            return rows(targetClass, resultSet);
        }
    }

    /**
     * It's user's responsibility to close the input <code>resultSet</code> after the stream is finished.
     *
     * @param <T>
     * @param resultSet
     * @param rowMapper
     * @return
     */
    public static <T> ExceptionalStream<T, SQLException> rows(final ResultSet resultSet, final Try.Function<ResultSet, T, SQLException> rowMapper) {
        N.checkArgNotNull(resultSet, "resultSet");
        N.checkArgNotNull(rowMapper, "rowMapper");

        final ExceptionalIterator<T, SQLException> iter = new ExceptionalIterator<T, SQLException>() {
            private boolean hasNext;

            @Override
            public boolean hasNext() throws SQLException {
                if (hasNext == false) {
                    hasNext = resultSet.next();
                }

                return hasNext;
            }

            @Override
            public T next() throws SQLException {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                return rowMapper.apply(resultSet);
            }

            @Override
            public void skip(long n) throws SQLException {
                N.checkArgNotNegative(n, "n");

                final long m = hasNext ? n - 1 : n;

                InternalJdbcUtil.skip(resultSet, m);

                hasNext = false;
            }
        };

        return newStream(iter);
    }

    /**
     * It's user's responsibility to close the input <code>resultSet</code> after the stream is finished.
     *
     * @param <T>
     * @param resultSet
     * @param rowMapper
     * @return
     */
    public static <T> ExceptionalStream<T, SQLException> rows(final ResultSet resultSet,
            final Try.BiFunction<ResultSet, List<String>, T, SQLException> rowMapper) {
        N.checkArgNotNull(resultSet, "resultSet");
        N.checkArgNotNull(rowMapper, "rowMapper");

        final ExceptionalIterator<T, SQLException> iter = new ExceptionalIterator<T, SQLException>() {
            private List<String> columnLabels = null;
            private boolean hasNext;

            @Override
            public boolean hasNext() throws SQLException {
                if (hasNext == false) {
                    hasNext = resultSet.next();
                }

                return hasNext;
            }

            @Override
            public T next() throws SQLException {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                hasNext = false;

                if (columnLabels == null) {
                    columnLabels = InternalJdbcUtil.getColumnLabelList(resultSet);
                }

                return rowMapper.apply(resultSet, columnLabels);
            }

            @Override
            public void skip(long n) throws SQLException {
                N.checkArgNotNegative(n, "n");

                final long m = hasNext ? n - 1 : n;

                InternalJdbcUtil.skip(resultSet, m);

                hasNext = false;
            }

            @Override
            public long count() throws SQLException {
                long cnt = 0;

                while (resultSet.next()) {
                    cnt++;
                }

                return cnt;
            }
        };

        return newStream(iter);
    }

    /**
     * It's user's responsibility to close the input <code>resultSet</code> after the stream is finished.
     *
     * @param <T>
     * @param resultSet
     * @param columnIndex starts from 0, not 1.
     * @return
     */
    public static <T> ExceptionalStream<T, SQLException> rows(final ResultSet resultSet, final int columnIndex) {
        N.checkArgNotNull(resultSet, "resultSet");
        N.checkArgNotNegative(columnIndex, "columnIndex");

        final ExceptionalIterator<T, SQLException> iter = new ExceptionalIterator<T, SQLException>() {
            private final int newColumnIndex = columnIndex + 1;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() throws SQLException {
                if (hasNext == false) {
                    hasNext = resultSet.next();
                }

                return hasNext;
            }

            @Override
            public T next() throws SQLException {
                if (!hasNext()) {
                    throw new NoSuchElementException("No more rows");
                }

                final T next = (T) InternalJdbcUtil.getColumnValue(resultSet, newColumnIndex);
                hasNext = false;
                return next;
            }

            @Override
            public void skip(long n) throws SQLException {
                N.checkArgNotNegative(n, "n");

                final long m = hasNext ? n - 1 : n;

                InternalJdbcUtil.skip(resultSet, m);

                hasNext = false;
            }
        };

        return newStream(iter);
    }

    /**
     *
     * @param <T>
     * @param resultSet
     * @param columnIndex starts from 0, not 1.
     * @param closeResultSet
     * @return
     * @deprecated
     */
    @Deprecated
    static <T> ExceptionalStream<T, SQLException> rows(final ResultSet resultSet, final int columnIndex, final boolean closeResultSet) {
        N.checkArgNotNull(resultSet, "resultSet");
        N.checkArgNotNegative(columnIndex, "columnIndex");

        if (closeResultSet) {
            return (ExceptionalStream<T, SQLException>) rows(resultSet, columnIndex).onClose(new Try.Runnable<SQLException>() {
                @Override
                public void run() throws SQLException {
                    InternalJdbcUtil.closeQuietly(resultSet);
                }
            });
        } else {
            return rows(resultSet, columnIndex);
        }
    }

    /**
     * It's user's responsibility to close the input <code>resultSet</code> after the stream is finished.
     *
     * @param <T>
     * @param resultSet
     * @param columnName
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     */
    public static <T> ExceptionalStream<T, SQLException> rows(final ResultSet resultSet, final String columnName) throws UncheckedSQLException {
        N.checkArgNotNull(resultSet, "resultSet");
        N.checkArgNotNullOrEmpty(columnName, "columnName");

        final ExceptionalIterator<T, SQLException> iter = new ExceptionalIterator<T, SQLException>() {
            private int columnIndex = -1;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() throws SQLException {
                if (hasNext == false) {
                    hasNext = resultSet.next();
                }

                return hasNext;
            }

            @Override
            public T next() throws SQLException {
                if (!hasNext()) {
                    throw new NoSuchElementException("No more rows");
                }

                columnIndex = columnIndex == -1 ? InternalJdbcUtil.getColumnIndex(resultSet, columnName) : columnIndex;

                final T next = (T) InternalJdbcUtil.getColumnValue(resultSet, columnIndex);
                hasNext = false;
                return next;
            }

            @Override
            public void skip(long n) throws SQLException {
                N.checkArgNotNegative(n, "n");

                final long m = hasNext ? n - 1 : n;

                InternalJdbcUtil.skip(resultSet, m);

                hasNext = false;
            }
        };

        return newStream(iter);
    }

    /**
     *
     * @param <T>
     * @param resultSet
     * @param columnName
     * @param closeResultSet
     * @return
     * @throws UncheckedSQLException the unchecked SQL exception
     * @deprecated
     */
    @Deprecated
    static <T> ExceptionalStream<T, SQLException> rows(final ResultSet resultSet, final String columnName, final boolean closeResultSet)
            throws UncheckedSQLException {
        N.checkArgNotNull(resultSet, "resultSet");
        N.checkArgNotNullOrEmpty(columnName, "columnName");

        if (closeResultSet) {
            return (ExceptionalStream<T, SQLException>) rows(resultSet, columnName).onClose(new Try.Runnable<SQLException>() {
                @Override
                public void run() throws SQLException {
                    InternalJdbcUtil.closeQuietly(resultSet);
                }
            });
        } else {
            return rows(resultSet, columnName);
        }
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

        final Deque<Try.Runnable<? extends E>> closeHandlers = new ArrayDeque<>();

        for (ExceptionalStream<? extends T, E> e : c) {
            if (N.notNullOrEmpty(e.closeHandlers)) {
                closeHandlers.addAll(e.closeHandlers);
            }
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
        }, closeHandlers);
    }

    /**
     * Zip together the "a" and "b" arrays until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     *
     * @param a
     * @param b
     * @return
     */
    public static <A, B, E extends Exception, T> ExceptionalStream<T, E> zip(final A[] a, final B[] b,
            final Try.BiFunction<? super A, ? super B, T, E> zipFunction) {
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
    public static <A, B, C, E extends Exception, T> ExceptionalStream<T, E> zip(final A[] a, final B[] b, final C[] c,
            final Try.TriFunction<? super A, ? super B, ? super C, T, E> zipFunction) {
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
    public static <A, B, E extends Exception, T> ExceptionalStream<T, E> zip(final Collection<? extends A> a, final Collection<? extends B> b,
            final Try.BiFunction<? super A, ? super B, T, E> zipFunction) {
        return zip(a.iterator(), b.iterator(), zipFunction);
    }

    /**
     * Zip together the "a", "b" and "c" arrays until one of them runs out of values.
     * Each triple of values is combined into a single value using the supplied zipFunction function.
     *
     * @param a
     * @param b
     * @return
     */
    public static <A, B, C, E extends Exception, T> ExceptionalStream<T, E> zip(final Collection<? extends A> a, final Collection<? extends B> b,
            final Collection<? extends C> c, final Try.TriFunction<? super A, ? super B, ? super C, T, E> zipFunction) {
        return zip(a.iterator(), b.iterator(), c.iterator(), zipFunction);
    }

    /**
     * Zip together the "a" and "b" iterators until one of them runs out of values.
     * Each pair of values is combined into a single value using the supplied zipFunction function.
     *
     * @param a
     * @param b
     * @return
     */
    public static <A, B, E extends Exception, T> ExceptionalStream<T, E> zip(final Iterator<? extends A> a, final Iterator<? extends B> b,
            final Try.BiFunction<? super A, ? super B, T, E> zipFunction) {
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
    public static <A, B, C, E extends Exception, T> ExceptionalStream<T, E> zip(final Iterator<? extends A> a, final Iterator<? extends B> b,
            final Iterator<? extends C> c, final Try.TriFunction<? super A, ? super B, ? super C, T, E> zipFunction) {
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
    public static <A, B, E extends Exception, T> ExceptionalStream<T, E> zip(final A[] a, final B[] b, final A valueForNoneA, final B valueForNoneB,
            final Try.BiFunction<? super A, ? super B, T, E> zipFunction) {
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
    public static <A, B, C, E extends Exception, T> ExceptionalStream<T, E> zip(final A[] a, final B[] b, final C[] c, final A valueForNoneA,
            final B valueForNoneB, final C valueForNoneC, final Try.TriFunction<? super A, ? super B, ? super C, T, E> zipFunction) {
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
    public static <A, B, E extends Exception, T> ExceptionalStream<T, E> zip(final Collection<? extends A> a, final Collection<? extends B> b,
            final A valueForNoneA, final B valueForNoneB, final Try.BiFunction<? super A, ? super B, T, E> zipFunction) {
        return zip(a.iterator(), b.iterator(), valueForNoneA, valueForNoneB, zipFunction);
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
    public static <A, B, C, E extends Exception, T> ExceptionalStream<T, E> zip(final Collection<? extends A> a, final Collection<? extends B> b,
            final Collection<? extends C> c, final A valueForNoneA, final B valueForNoneB, final C valueForNoneC,
            final Try.TriFunction<? super A, ? super B, ? super C, T, E> zipFunction) {
        return zip(a.iterator(), b.iterator(), c.iterator(), valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
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
    public static <A, B, E extends Exception, T> ExceptionalStream<T, E> zip(final Iterator<? extends A> a, final Iterator<? extends B> b,
            final A valueForNoneA, final B valueForNoneB, final Try.BiFunction<? super A, ? super B, T, E> zipFunction) {
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
    public static <A, B, C, E extends Exception, T> ExceptionalStream<T, E> zip(final Iterator<? extends A> a, final Iterator<? extends B> b,
            final Iterator<? extends C> c, final A valueForNoneA, final B valueForNoneB, final C valueForNoneC,
            final Try.TriFunction<? super A, ? super B, ? super C, T, E> zipFunction) {
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
     *
     * @param predicate
     * @return
     */
    public ExceptionalStream<T, E> filter(final Try.Predicate<? super T, ? extends E> predicate) {
        checkArgNotNull(predicate, "predicate");

        return newStream(new ExceptionalIterator<T, E>() {
            private final T NONE = (T) N.NULL_MASK;
            private T next = NONE;

            @Override
            public boolean hasNext() throws E {
                while (next == NONE && elements.hasNext()) {
                    next = elements.next();

                    if (predicate.test(next)) {
                        break;
                    }
                }

                return next != NONE;
            }

            @Override
            public T next() throws E {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                final T result = next;
                next = NONE;
                return result;
            }
        }, sorted, comparator, closeHandlers);
    }

    /**
     *
     * @param predicate
     * @return
     */
    public ExceptionalStream<T, E> takeWhile(final Try.Predicate<? super T, ? extends E> predicate) {
        checkArgNotNull(predicate, "predicate");

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
        }, sorted, comparator, closeHandlers);
    }

    /**
     *
     * @param predicate
     * @return
     */
    public ExceptionalStream<T, E> dropWhile(final Try.Predicate<? super T, ? extends E> predicate) {
        checkArgNotNull(predicate, "predicate");

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

        }, sorted, comparator, closeHandlers);
    }

    /**
     * Distinct and filter by occurrences.
     *
     * @return
     */
    public ExceptionalStream<T, E> distinct() {
        final Set<Object> set = N.newHashSet();

        return filter(new Try.Predicate<T, E>() {
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
    public ExceptionalStream<T, E> distinctBy(final Try.Function<? super T, ?, ? extends E> keyMapper) {
        checkArgNotNull(keyMapper, "keyMapper");

        final Set<Object> set = N.newHashSet();

        return filter(new Try.Predicate<T, E>() {
            @Override
            public boolean test(T value) throws E {
                return set.add(hashKey(keyMapper.apply(value)));
            }
        });
    }

    /**
     *
     * @param <U>
     * @param mapper
     * @return
     */
    public <U> ExceptionalStream<U, E> map(final Try.Function<? super T, ? extends U, ? extends E> mapper) {
        checkArgNotNull(mapper, "mapper");

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
    public <R> ExceptionalStream<R, E> flatMap(final Try.Function<? super T, ? extends ExceptionalStream<? extends R, ? extends E>, ? extends E> mapper) {
        checkArgNotNull(mapper, "mapper");

        final ExceptionalIterator<R, E> iter = new ExceptionalIterator<R, E>() {
            private ExceptionalIterator<? extends R, ? extends E> cur = null;
            private ExceptionalStream<? extends R, ? extends E> s = null;
            private Try.Runnable<E> closeHandle = null;

            @Override
            public boolean hasNext() throws E {
                while ((cur == null || cur.hasNext() == false) && elements.hasNext()) {
                    if (closeHandle != null) {
                        final Try.Runnable<E> tmp = closeHandle;
                        closeHandle = null;
                        tmp.run();
                    }

                    s = mapper.apply(elements.next());

                    if (N.notNullOrEmpty(s.closeHandlers)) {
                        closeHandle = new Try.Runnable<E>() {
                            @Override
                            public void run() throws E {
                                s.close();
                            }
                        };
                    }

                    cur = s.elements;
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
                    final Try.Runnable<E> tmp = closeHandle;
                    closeHandle = null;
                    tmp.run();
                }
            }
        };

        final Deque<Try.Runnable<? extends E>> newCloseHandlers = new ArrayDeque<>(N.size(closeHandlers) + 1);

        newCloseHandlers.add(new Try.Runnable<E>() {
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
    public <R> ExceptionalStream<R, E> flattMap(final Try.Function<? super T, ? extends Collection<? extends R>, ? extends E> mapper) {
        checkArgNotNull(mapper, "mapper");

        return flatMap(new Try.Function<T, ExceptionalStream<? extends R, ? extends E>, E>() {
            @Override
            public ExceptionalStream<? extends R, ? extends E> apply(T t) throws E {
                return ExceptionalStream.of(mapper.apply(t));
            }
        });
    }

    /**
     *
     * @param <R>
     * @param mapper
     * @return
     */
    public <R> ExceptionalStream<R, E> slidingMap(Try.BiFunction<? super T, ? super T, R, E> mapper) {
        return slidingMap(mapper, 1);
    }

    /**
     *
     * @param <R>
     * @param mapper
     * @param increment
     * @return
     */
    public <R> ExceptionalStream<R, E> slidingMap(Try.BiFunction<? super T, ? super T, R, E> mapper, int increment) {
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
    public <R> ExceptionalStream<R, E> slidingMap(final Try.BiFunction<? super T, ? super T, R, E> mapper, final int increment, final boolean ignoreNotPaired) {
        final int windowSize = 2;

        checkArgPositive(increment, "increment");

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
    public <R> ExceptionalStream<R, E> slidingMap(Try.TriFunction<? super T, ? super T, ? super T, R, E> mapper) {
        return slidingMap(mapper, 1);
    }

    /**
     *
     * @param <R>
     * @param mapper
     * @param increment
     * @return
     */
    public <R> ExceptionalStream<R, E> slidingMap(Try.TriFunction<? super T, ? super T, ? super T, R, E> mapper, int increment) {
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
    public <R> ExceptionalStream<R, E> slidingMap(final Try.TriFunction<? super T, ? super T, ? super T, R, E> mapper, final int increment,
            final boolean ignoreNotPaired) {
        final int windowSize = 3;

        checkArgPositive(increment, "increment");

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
    public <K> ExceptionalStream<Map.Entry<K, List<T>>, E> groupBy(final Try.Function<? super T, ? extends K, ? extends E> keyMapper) {
        return groupBy(keyMapper, Suppliers.<K, List<T>> ofMap());
    }

    /**
     *
     * @param <K> the key type
     * @param keyMapper
     * @param mapFactory
     * @return
     */
    public <K> ExceptionalStream<Map.Entry<K, List<T>>, E> groupBy(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
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
    public <K, V> ExceptionalStream<Map.Entry<K, List<V>>, E> groupBy(Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            Try.Function<? super T, ? extends V, E> valueMapper) {
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
    public <K, V> ExceptionalStream<Map.Entry<K, List<V>>, E> groupBy(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Try.Function<? super T, ? extends V, ? extends E> valueMapper, final Supplier<? extends Map<K, List<V>>> mapFactory) {
        checkArgNotNull(keyMapper, "keyMapper");
        checkArgNotNull(valueMapper, "valueMapper");
        checkArgNotNull(mapFactory, "mapFactory");

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
    public <K, V> ExceptionalStream<Map.Entry<K, V>, E> groupBy(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Try.Function<? super T, ? extends V, ? extends E> valueMapper, Try.BinaryOperator<V, ? extends E> mergeFunction) {
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
    public <K, V> ExceptionalStream<Map.Entry<K, V>, E> groupBy(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Try.Function<? super T, ? extends V, ? extends E> valueMapper, final Try.BinaryOperator<V, ? extends E> mergeFunction,
            final Supplier<? extends Map<K, V>> mapFactory) {
        checkArgNotNull(keyMapper, "keyMapper");
        checkArgNotNull(valueMapper, "valueMapper");
        checkArgNotNull(mergeFunction, "mergeFunction");
        checkArgNotNull(mapFactory, "mapFactory");

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
    public <K, A, D> ExceptionalStream<Map.Entry<K, D>, E> groupBy(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Collector<? super T, A, D> downstream) throws E {
        return groupBy(keyMapper, downstream, Suppliers.<K, D> ofMap());
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
    public <K, A, D, M extends Map<K, D>> ExceptionalStream<Map.Entry<K, D>, E> groupBy(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Collector<? super T, A, D> downstream, final Supplier<? extends M> mapFactory) throws E {
        return groupBy(keyMapper, Fnn.<T, E> identity(), downstream, mapFactory);
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
     * @return
     * @throws E the e
     */
    public <K, V, A, D, M extends Map<K, D>> ExceptionalStream<Map.Entry<K, D>, E> groupBy(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Try.Function<? super T, ? extends V, ? extends E> valueMapper, final Collector<? super V, A, D> downstream) throws E {
        return groupBy(keyMapper, valueMapper, downstream, Suppliers.<K, D> ofMap());
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
    public <K, V, A, D, M extends Map<K, D>> ExceptionalStream<Map.Entry<K, D>, E> groupBy(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Try.Function<? super T, ? extends V, ? extends E> valueMapper, final Collector<? super V, A, D> downstream,
            final Supplier<? extends M> mapFactory) throws E {
        checkArgNotNull(keyMapper, "keyMapper");
        checkArgNotNull(valueMapper, "valueMapper");
        checkArgNotNull(downstream, "downstream");
        checkArgNotNull(mapFactory, "mapFactory");

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
     * @param collapsible
     * @return
     */
    public ExceptionalStream<Stream<T>, E> collapse(final Try.BiPredicate<? super T, ? super T, ? extends E> collapsible) {
        checkArgNotNull(collapsible, "collapsible");

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
    public <C extends Collection<T>> ExceptionalStream<C, E> collapse(final Try.BiPredicate<? super T, ? super T, ? extends E> collapsible,
            final Supplier<? extends C> supplier) {
        checkArgNotNull(collapsible, "collapsible");
        checkArgNotNull(supplier, "supplier");

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
    public ExceptionalStream<T, E> collapse(final Try.BiPredicate<? super T, ? super T, ? extends E> collapsible,
            final Try.BiFunction<? super T, ? super T, T, ? extends E> mergeFunction) {
        checkArgNotNull(collapsible, "collapsible");
        checkArgNotNull(mergeFunction, "mergeFunction");

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
    public <U> ExceptionalStream<U, E> collapse(final Try.BiPredicate<? super T, ? super T, ? extends E> collapsible, final U init,
            final Try.BiFunction<U, ? super T, U, ? extends E> op) {
        checkArgNotNull(collapsible, "collapsible");
        checkArgNotNull(op, "accumulator");

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
    public <R> ExceptionalStream<R, E> collapse(final Try.BiPredicate<? super T, ? super T, ? extends E> collapsible, final Try.Supplier<R, E> supplier,
            final Try.BiConsumer<? super R, ? super T, ? extends E> accumulator) {
        checkArgNotNull(collapsible, "collapsible");
        checkArgNotNull(supplier, "supplier");
        checkArgNotNull(accumulator, "accumulator");

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
    public <R, A> ExceptionalStream<R, E> collapse(final Try.BiPredicate<? super T, ? super T, ? extends E> collapsible,
            final Collector<? super T, A, R> collector) {
        checkArgNotNull(collapsible, "collapsible");
        checkArgNotNull(collector, "collector");

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
    public ExceptionalStream<T, E> scan(final Try.BiFunction<? super T, ? super T, T, E> accumulator) {
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
    public <U> ExceptionalStream<U, E> scan(final U init, final Try.BiFunction<U, ? super T, U, E> accumulator) {
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
    public <U> ExceptionalStream<U, E> scan(final U init, final Try.BiFunction<U, ? super T, U, E> accumulator, final boolean initIncluded) {
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

    @SafeVarargs
    public final ExceptionalStream<T, E> prepend(final T... a) {
        return prepend(ExceptionalStream.of(a));
    }

    public ExceptionalStream<T, E> prepend(final Collection<? extends T> c) {
        return prepend(ExceptionalStream.of(c));
    }

    /**
     *
     * @param s
     * @return
     */
    public ExceptionalStream<T, E> prepend(final ExceptionalStream<T, E> s) {
        return concat(s, this);
    }

    @SafeVarargs
    public final ExceptionalStream<T, E> append(final T... a) {
        return append(ExceptionalStream.of(a));
    }

    public ExceptionalStream<T, E> append(final Collection<? extends T> c) {
        return append(ExceptionalStream.of(c));
    }

    /**
     *
     * @param s
     * @return
     */
    public ExceptionalStream<T, E> append(final ExceptionalStream<T, E> s) {
        return concat(this, s);
    }

    @SafeVarargs
    public final ExceptionalStream<T, E> appendIfEmpty(final T... a) {
        return appendIfEmpty(Arrays.asList(a));
    }

    public ExceptionalStream<T, E> appendIfEmpty(final Collection<? extends T> c) {
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
            public void skip(long n) throws E {
                if (iter == null) {
                    init();
                }

                iter.skip(n);
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
    public ExceptionalStream<T, E> appendIfEmpty(final Supplier<? extends ExceptionalStream<T, E>> supplier) throws E {
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
            public void skip(long n) throws E {
                if (iter == null) {
                    init();
                }

                iter.skip(n);
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
        }, closeHandlers).onClose(new Try.Runnable<E>() {
            @Override
            public void run() throws E {
                close(holder);
            }
        });
    }

    void close(Holder<? extends ExceptionalStream<T, E>> holder) throws E {
        if (holder.value() != null) {
            holder.value().close();
        }
    }

    public <R> Optional<R> applyIfNotEmpty(final Try.Function<? super ExceptionalStream<T, E>, R, E> func) throws E {
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

    public void acceptIfNotEmpty(Try.Consumer<? super ExceptionalStream<T, E>, E> action) throws E {
        try {
            if (elements.hasNext()) {
                action.accept(this);
            }
        } finally {
            close();
        }
    }

    /**
     *
     * @param action
     * @return
     */
    public ExceptionalStream<T, E> peek(final Try.Consumer<? super T, ? extends E> action) {
        checkArgNotNull(action, "action");

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
        }, sorted, comparator, closeHandlers);
    }

    /**
     *
     * @param chunkSize
     * @return
     */
    public ExceptionalStream<Stream<T>, E> split(final int chunkSize) {
        return splitToList(chunkSize).map(new Try.Function<List<T>, Stream<T>, E>() {
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
    public <C extends Collection<T>> ExceptionalStream<C, E> split(final int chunkSize, final IntFunction<? extends C> collectionSupplier) {
        checkArgPositive(chunkSize, "chunkSize");
        checkArgNotNull(collectionSupplier, "collectionSupplier");

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
            public void skip(long n) throws E {
                checkArgNotNegative(n, "n");

                elements.skip(n > Long.MAX_VALUE / chunkSize ? Long.MAX_VALUE : n * chunkSize);
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
    public <R, A> ExceptionalStream<R, E> split(final int chunkSize, final Collector<? super T, A, R> collector) {
        checkArgPositive(chunkSize, "chunkSize");
        checkArgNotNull(collector, "collector");

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
            public void skip(long n) throws E {
                checkArgNotNegative(n, "n");

                elements.skip(n > Long.MAX_VALUE / chunkSize ? Long.MAX_VALUE : n * chunkSize);
            }
        }, closeHandlers);
    }

    /**
     *
     * @param windowSize
     * @param increment
     * @return
     */
    public ExceptionalStream<Stream<T>, E> sliding(final int windowSize, final int increment) {
        return slidingToList(windowSize, increment).map(new Try.Function<List<T>, Stream<T>, E>() {
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
    public <C extends Collection<T>> ExceptionalStream<C, E> sliding(final int windowSize, final int increment,
            final IntFunction<? extends C> collectionSupplier) {
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);
        checkArgNotNull(collectionSupplier, "collectionSupplier");

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
            public void skip(long n) throws E {
                checkArgNotNegative(n, "n");

                if (n == 0) {
                    return;
                }

                if (increment >= windowSize) {
                    elements.skip(n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                } else {
                    if (N.isNullOrEmpty(queue)) {
                        final long m = ((n - 1) > Long.MAX_VALUE / increment ? Long.MAX_VALUE : (n - 1) * increment);
                        elements.skip(m);
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

                            elements.skip(m - prevSize);
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
    public <A, R> ExceptionalStream<R, E> sliding(final int windowSize, final int increment, final Collector<? super T, A, R> collector) {
        checkArgument(windowSize > 0 && increment > 0, "windowSize=%s and increment=%s must be bigger than 0", windowSize, increment);
        checkArgNotNull(collector, "collector");

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
            public void skip(long n) throws E {
                checkArgNotNegative(n, "n");

                if (n == 0) {
                    return;
                }

                if (increment >= windowSize) {
                    elements.skip(n > Long.MAX_VALUE / increment ? Long.MAX_VALUE : n * increment);
                } else {
                    if (N.isNullOrEmpty(queue)) {
                        final long m = ((n - 1) > Long.MAX_VALUE / increment ? Long.MAX_VALUE : (n - 1) * increment);
                        elements.skip(m);
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

                            elements.skip(m - prevSize);
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
    public ExceptionalStream<T, E> skip(final long n) {
        checkArgNotNegative(n, "n");

        return newStream(new ExceptionalIterator<T, E>() {
            private boolean skipped = false;

            @Override
            public boolean hasNext() throws E {
                if (skipped == false) {
                    skipped = true;
                    skip(n);
                }

                return elements.hasNext();
            }

            @Override
            public T next() throws E {
                if (skipped == false) {
                    skipped = true;
                    skip(n);
                }

                return elements.next();
            }
        }, sorted, comparator, closeHandlers);
    }

    /**
     *
     * @param maxSize
     * @return
     */
    public ExceptionalStream<T, E> limit(final long maxSize) {
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

        }, sorted, comparator, closeHandlers);
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

    public ExceptionalStream<T, E> shuffled() {
        return shuffled(RAND);
    }

    public ExceptionalStream<T, E> shuffled(final Random rnd) {
        return lazyLoad(new Function<Object[], Object[]>() {
            @Override
            public Object[] apply(final Object[] a) {
                N.shuffle(a, rnd);
                return a;
            }
        }, false, null);
    }

    public ExceptionalStream<T, E> rotated(final int distance) {
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
            public void skip(long n) throws E {
                if (initialized == false) {
                    init();
                }

                cnt = n < len - cnt ? cnt + (int) n : len;
            }

            private void init() throws E {
                if (initialized == false) {
                    initialized = true;
                    aar = (T[]) ExceptionalStream.this.toArray();
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
        });
    }

    /**
     *
     * @return
     */
    public ExceptionalStream<T, E> sorted() {
        return sorted(Comparators.NATURAL_ORDER);
    }

    /**
     *
     * @return
     */
    public ExceptionalStream<T, E> reverseSorted() {
        return sorted(Comparators.REVERSED_ORDER);
    }

    /**
     *
     * @param comparator
     * @return
     */
    public ExceptionalStream<T, E> sorted(final Comparator<? super T> comparator) {
        final Comparator<? super T> cmp = comparator == null ? Comparators.NATURAL_ORDER : comparator;

        if (sorted && cmp == this.comparator) {
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
    @SuppressWarnings("rawtypes")
    public ExceptionalStream<T, E> sortedBy(final Function<? super T, ? extends Comparable> keyMapper) {
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
            public void skip(long n) throws E {
                checkArgNotNegative(n, "n");

                if (initialized == false) {
                    init();
                }

                cursor = n > len - cursor ? len : cursor + (int) n;
            }

            private void init() throws E {
                if (initialized == false) {
                    initialized = true;
                    aar = (T[]) op.apply(ExceptionalStream.this.toArray());
                    len = aar.length;
                }
            }
        }, sorted, cmp, closeHandlers);
    }

    public ExceptionalStream<T, E> intersperse(final T delimiter) {
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

    /**
     *
     * @param action
     * @throws E the e
     */
    public void forEach(Try.Consumer<? super T, ? extends E> action) throws E {
        checkArgNotNull(action, "action");
        assertNotClosed();

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
     * @param onComplete
     * @throws E the e
     * @throws E2 the e2
     */
    public <E2 extends Exception> void forEach(final Try.Consumer<? super T, E> action, final Try.Runnable<E2> onComplete) throws E, E2 {
        checkArgNotNull(action, "action");
        checkArgNotNull(onComplete, "onComplete");
        assertNotClosed();

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
     * @param flatMapper
     * @param action
     * @throws E the e
     * @throws E2 the e2
     */
    public <U, E2 extends Exception> void forEach(final Try.Function<? super T, ? extends Collection<U>, E> flatMapper,
            final Try.BiConsumer<? super T, ? super U, E2> action) throws E, E2 {
        checkArgNotNull(flatMapper, "flatMapper");
        checkArgNotNull(action, "action");
        assertNotClosed();

        Collection<U> c = null;
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
     * @param flatMapper
     * @param flatMapper2
     * @param action
     * @throws E the e
     * @throws E2 the e2
     * @throws E3 the e3
     */
    public <T2, T3, E2 extends Exception, E3 extends Exception> void forEach(final Try.Function<? super T, ? extends Collection<T2>, E> flatMapper,
            final Try.Function<? super T2, ? extends Collection<T3>, E2> flatMapper2, final Try.TriConsumer<? super T, ? super T2, ? super T3, E3> action)
            throws E, E2, E3 {
        checkArgNotNull(flatMapper, "flatMapper");
        checkArgNotNull(flatMapper2, "flatMapper2");
        checkArgNotNull(action, "action");
        assertNotClosed();

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
     * @param action
     * @throws E the e
     */
    public void forEachPair(final Try.BiConsumer<? super T, ? super T, E> action) throws E {
        forEachPair(action, 1);
    }

    /**
     * For each pair.
     *
     * @param action
     * @param increment
     * @throws E the e
     */
    public void forEachPair(final Try.BiConsumer<? super T, ? super T, E> action, final int increment) throws E {
        final int windowSize = 2;
        checkArgPositive(increment, "increment");
        assertNotClosed();

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
     * @param action
     * @throws E the e
     */
    public void forEachTriple(final Try.TriConsumer<? super T, ? super T, ? super T, E> action) throws E {
        forEachTriple(action, 1);
    }

    /**
     * For each triple.
     *
     * @param action
     * @param increment
     * @throws E the e
     */
    public void forEachTriple(final Try.TriConsumer<? super T, ? super T, ? super T, E> action, final int increment) throws E {
        final int windowSize = 3;
        checkArgPositive(increment, "increment");
        assertNotClosed();

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
    @SuppressWarnings("rawtypes")

    public Optional<T> minBy(final Function<? super T, ? extends Comparable> keyMapper) throws E {
        checkArgNotNull(keyMapper, "keyMapper");
        assertNotClosed();

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
    @SuppressWarnings("rawtypes")

    public Optional<T> maxBy(final Function<? super T, ? extends Comparable> keyMapper) throws E {
        checkArgNotNull(keyMapper, "keyMapper");
        assertNotClosed();

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
     * @return true, if successful
     * @throws E the e
     */
    public boolean anyMatch(final Try.Predicate<? super T, ? extends E> predicate) throws E {
        checkArgNotNull(predicate, "predicate");
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
     * @return true, if successful
     * @throws E the e
     */
    public boolean allMatch(final Try.Predicate<? super T, ? extends E> predicate) throws E {
        checkArgNotNull(predicate, "predicate");
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
     * @return true, if successful
     * @throws E the e
     */
    public boolean noneMatch(final Try.Predicate<? super T, ? extends E> predicate) throws E {
        checkArgNotNull(predicate, "predicate");
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
     * @param predicate
     * @return
     * @throws E the e
     */
    public Optional<T> findFirst(final Try.Predicate<? super T, ? extends E> predicate) throws E {
        checkArgNotNull(predicate, "predicate");
        assertNotClosed();

        try {
            while (elements.hasNext()) {
                T e = elements.next();

                if (predicate.test(e)) {
                    return Optional.of(e);
                }
            }

            return (Optional<T>) Optional.empty();
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
    public Optional<T> findLast(final Try.Predicate<? super T, ? extends E> predicate) throws E {
        checkArgNotNull(predicate, "predicate");
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return (Optional<T>) Optional.empty();
            }

            boolean hasResult = false;
            T e = null;
            T result = null;

            while (elements.hasNext()) {
                e = elements.next();

                if (predicate.test(e)) {
                    result = e;
                    hasResult = true;
                }
            }

            return hasResult ? Optional.of(result) : (Optional<T>) Optional.empty();
        } finally {
            close();
        }
    }

    /**
     *
     * @return
     * @throws E the e
     */
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
     *
     * @return
     * @throws E the e
     */
    public Object[] toArray() throws E {
        assertNotClosed();

        try {
            return toList().toArray();
        } finally {
            close();
        }
    }

    /**
     *
     * @param <A>
     * @param generator
     * @return
     * @throws E the e
     */
    public <A> A[] toArray(IntFunction<A[]> generator) throws E {
        checkArgNotNull(generator, "generator");
        assertNotClosed();

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
    public ImmutableList<T> toImmutableList() throws E {
        return ImmutableList.of(toList());
    }

    /**
     *
     * @return
     * @throws E the e
     */
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
    public <C extends Collection<T>> C toCollection(final Supplier<? extends C> supplier) throws E {
        checkArgNotNull(supplier, "supplier");
        assertNotClosed();

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
    public <K, V> Map<K, V> toMap(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Try.Function<? super T, ? extends V, ? extends E> valueMapper) throws E, IllegalStateException {
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
    public <K, V, M extends Map<K, V>> M toMap(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Try.Function<? super T, ? extends V, ? extends E> valueMapper, final Supplier<? extends M> mapFactory) throws E, IllegalStateException {
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
    public <K, V> Map<K, V> toMap(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Try.Function<? super T, ? extends V, ? extends E> valueMapper, final Try.BinaryOperator<V, ? extends E> mergeFunction) throws E {
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
    public <K, V, M extends Map<K, V>> M toMap(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Try.Function<? super T, ? extends V, ? extends E> valueMapper, final Try.BinaryOperator<V, ? extends E> mergeFunction,
            final Supplier<? extends M> mapFactory) throws E {
        checkArgNotNull(keyMapper, "keyMapper");
        checkArgNotNull(valueMapper, "valueMapper");
        checkArgNotNull(mergeFunction, "mergeFunction");
        checkArgNotNull(mapFactory, "mapFactory");
        assertNotClosed();

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
    public <K, A, D> Map<K, D> toMap(final Try.Function<? super T, ? extends K, ? extends E> keyMapper, final Collector<? super T, A, D> downstream) throws E {
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
    public <K, A, D, M extends Map<K, D>> M toMap(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
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
    public <K, V, A, D> Map<K, D> toMap(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Try.Function<? super T, ? extends V, ? extends E> valueMapper, final Collector<? super V, A, D> downstream) throws E {
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
    public <K, V, A, D, M extends Map<K, D>> M toMap(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Try.Function<? super T, ? extends V, ? extends E> valueMapper, final Collector<? super V, A, D> downstream,
            final Supplier<? extends M> mapFactory) throws E {
        checkArgNotNull(keyMapper, "keyMapper");
        checkArgNotNull(valueMapper, "valueMapper");
        checkArgNotNull(downstream, "downstream");
        checkArgNotNull(mapFactory, "mapFactory");
        assertNotClosed();

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
    public <K> Map<K, List<T>> groupTo(Try.Function<? super T, ? extends K, ? extends E> keyMapper) throws E {
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
    public <K, M extends Map<K, List<T>>> M groupTo(final Try.Function<? super T, ? extends K, ? extends E> keyMapper, final Supplier<? extends M> mapFactory)
            throws E {
        final Try.Function<T, T, E> valueMapper = Fnn.identity();

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
    public <K, V> Map<K, List<V>> groupTo(Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            Try.Function<? super T, ? extends V, ? extends E> valueMapper) throws E {
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
    public <K, V, M extends Map<K, List<V>>> M groupTo(Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            Try.Function<? super T, ? extends V, ? extends E> valueMapper, Supplier<? extends M> mapFactory) throws E {
        checkArgNotNull(keyMapper, "keyMapper");
        checkArgNotNull(valueMapper, "valueMapper");
        checkArgNotNull(mapFactory, "mapFactory");
        assertNotClosed();

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

    public Multiset<T> toMultiset() throws E {
        return toMultiset(Suppliers.<T> ofMultiset());
    }

    public Multiset<T> toMultiset(Supplier<? extends Multiset<T>> supplier) throws E {
        assertNotClosed();

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
     * @return
     * @throws E the e
     */
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
     * @throws DuplicatedResultException if there are more than one elements.
     * @throws E the e
     */
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
     * @param func
     * @return
     * @throws E the e
     */
    public OptionalLong sumInt(Try.ToIntFunction<? super T, E> func) throws E {
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
    public OptionalLong sumLong(Try.ToLongFunction<? super T, E> func) throws E {
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
    public OptionalDouble sumDouble(Try.ToDoubleFunction<? super T, E> func) throws E {
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
    public OptionalDouble averageInt(Try.ToIntFunction<? super T, E> func) throws E {
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
    public OptionalDouble averageLong(Try.ToLongFunction<? super T, E> func) throws E {
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
    public OptionalDouble averageDouble(Try.ToDoubleFunction<? super T, E> func) throws E {
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
     * @param accumulator
     * @return
     * @throws E the e
     */
    public Optional<T> reduce(Try.BinaryOperator<T, ? extends E> accumulator) throws E {
        checkArgNotNull(accumulator, "accumulator");
        assertNotClosed();

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
     * @param identity
     * @param accumulator
     * @return
     * @throws E the e
     */
    public <U> U reduce(final U identity, final Try.BiFunction<U, ? super T, U, E> accumulator) throws E {
        checkArgNotNull(accumulator, "accumulator");
        assertNotClosed();

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
     * @param supplier
     * @param accumulator
     * @return
     * @throws E the e
     */
    public <R> R collect(Try.Supplier<R, E> supplier, final Try.BiConsumer<? super R, ? super T, ? extends E> accumulator) throws E {
        checkArgNotNull(supplier, "supplier");
        checkArgNotNull(accumulator, "accumulator");
        assertNotClosed();

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
     * @param supplier
     * @param accumulator
     * @param finisher
     * @return
     * @throws E the e
     */
    public <R, RR> RR collect(Try.Supplier<R, E> supplier, final Try.BiConsumer<? super R, ? super T, ? extends E> accumulator,
            final Try.Function<? super R, ? extends RR, E> finisher) throws E {
        checkArgNotNull(supplier, "supplier");
        checkArgNotNull(accumulator, "accumulator");
        checkArgNotNull(finisher, "finisher");
        assertNotClosed();

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
    public <R, A> R collect(final Collector<? super T, A, R> collector) throws E {
        checkArgNotNull(collector, "collector");
        assertNotClosed();

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
    public <R, A> R collect(java.util.stream.Collector<? super T, A, R> collector) throws E {
        checkArgNotNull(collector, "collector");
        assertNotClosed();

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
     * @param collector
     * @param func
     * @return
     * @throws E the e
     */
    public <R, RR, A> RR collectAndThen(final Collector<? super T, A, R> collector, final Try.Function<? super R, ? extends RR, E> func) throws E {
        checkArgNotNull(collector, "collector");
        checkArgNotNull(func, "func");
        assertNotClosed();

        return func.apply(collect(collector));
    }

    /**
     * Collect and then.
     *
     * @param <R>
     * @param <RR>
     * @param <A>
     * @param collector
     * @param func
     * @return
     * @throws E the e
     */
    public <R, RR, A> RR collectAndThen(final java.util.stream.Collector<? super T, A, R> collector, final Try.Function<? super R, ? extends RR, E> func)
            throws E {
        checkArgNotNull(collector, "collector");
        checkArgNotNull(func, "func");
        assertNotClosed();

        return func.apply(collect(collector));
    }

    /**
     *
     * @return
     */
    public Stream<T> unchecked() {
        if (N.isNullOrEmpty(this.closeHandlers)) {
            return Stream.of(new ObjIteratorEx<T>() {
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
                public void skip(long n) {
                    N.checkArgNotNegative(n, "n");

                    try {
                        elements.skip(n);
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
            });
        } else {
            return Stream.of(new ObjIteratorEx<T>() {
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
                public void skip(long n) {
                    N.checkArgNotNegative(n, "n");

                    try {
                        elements.skip(n);
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
            }).onClose(new Runnable() {
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
    //        Deque<Try.Runnable<? extends E2>> newCloseHandlers = null;
    //
    //        if (closeHandlers != null) {
    //            newCloseHandlers = new ArrayDeque<>(1);
    //            newCloseHandlers.add(new Try.Runnable<E2>() {
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
    @Beta
    public <TT, EE extends Exception> ExceptionalStream<TT, EE> __(
            Try.Function<? super ExceptionalStream<T, E>, ExceptionalStream<TT, EE>, ? extends E> transfer) throws E {
        checkArgNotNull(transfer, "transfer");

        return transfer.apply(this);
    }

    /**
     *
     *
     * @param action a terminal operation should be called.
     * @return
     */
    @Beta
    public ContinuableFuture<Void> asyncRun(final Try.Consumer<? super ExceptionalStream<T, E>, E> action) {
        checkArgNotNull(action, "action");

        return N.asyncExecute(new Try.Runnable<E>() {
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
    public ContinuableFuture<Void> asyncRun(final Try.Consumer<? super ExceptionalStream<T, E>, E> action, final Executor executor) {
        checkArgNotNull(action, "action");
        checkArgNotNull(executor, "executor");

        return ContinuableFuture.run(new Try.Runnable<E>() {
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
    public <R> ContinuableFuture<R> asyncCall(final Try.Function<? super ExceptionalStream<T, E>, R, E> action) {
        checkArgNotNull(action, "action");

        return N.asyncExecute(new Try.Callable<R, E>() {
            @Override
            public R call() throws E {
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
    public <R> ContinuableFuture<R> asyncCall(final Try.Function<? super ExceptionalStream<T, E>, R, E> action, final Executor executor) {
        checkArgNotNull(action, "action");
        checkArgNotNull(executor, "executor");

        return ContinuableFuture.call(new Try.Callable<R, E>() {
            @Override
            public R call() throws E {
                return action.apply(ExceptionalStream.this);
            }
        }, executor);
    }

    /**
     *
     * @param closeHandler
     * @return
     */
    public ExceptionalStream<T, E> onClose(final Try.Runnable<? extends E> closeHandler) {
        checkArgNotNull(closeHandler, "closeHandler");

        final Deque<Try.Runnable<? extends E>> newCloseHandlers = new ArrayDeque<>(N.size(closeHandlers) + 1);

        newCloseHandlers.add(new Try.Runnable<E>() {
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
    @Override
    public synchronized void close() throws E {
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

        Throwable ex = null;

        for (Try.Runnable<? extends E> closeHandler : closeHandlers) {
            try {
                closeHandler.run();
            } catch (Exception e) {
                if (ex == null) {
                    ex = e;
                } else {
                    if (ex instanceof RuntimeException && !(ex instanceof RuntimeException)) {
                        e.addSuppressed(ex);
                        ex = e;
                    } else {
                        ex.addSuppressed(e);
                    }
                }
            }
        }

        if (ex != null) {
            if (ex instanceof RuntimeException) {
                throw (RuntimeException) ex;
            } else {
                throw (E) ex;
            }
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

    //    /**
    //     *
    //     * @param b
    //     * @param errorMessage
    //     */
    //    private void checkArgument(boolean b, String errorMessage) {
    //        if (!b) {
    //            try {
    //                N.checkArgument(b, errorMessage);
    //            } finally {
    //                try {
    //                    close();
    //                } catch (Exception e) {
    //                    throw N.toRuntimeException(e);
    //                }
    //            }
    //        }
    //    }

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
            final Deque<Try.Runnable<? extends E>> closeHandlers) {
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
            final Comparator<? super T> comparator, final Deque<Try.Runnable<? extends E>> closeHandlers) {
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
     * The Class StreamE.
     *
     * @param <T>
     * @param <E>
     */
    public static final class StreamE<T, E extends Exception> extends ExceptionalStream<T, E> {

        /**
         * Instantiates a new stream E.
         *
         * @param iter
         * @param sorted
         * @param comparator
         * @param closeHandlers
         */
        StreamE(ExceptionalIterator<T, E> iter, boolean sorted, Comparator<? super T> comparator, Deque<Try.Runnable<? extends E>> closeHandlers) {
            super(iter, sorted, comparator, closeHandlers);
        }
    }

    /**
     * The Class ExceptionalIterator.
     *
     * @param <T>
     * @param <E>
     */
    static abstract class ExceptionalIterator<T, E extends Exception> {

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
        public static <T, E extends Exception> ExceptionalIterator<T, E> of(final Try.Supplier<ExceptionalIterator<T, E>, E> iteratorSupplier) {
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
                public void skip(long n) throws E {
                    N.checkArgNotNegative(n, "n");

                    if (isInitialized == false) {
                        init();
                    }

                    iter.skip(n);
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
        public static <T, E extends Exception> ExceptionalIterator<T, E> oF(final Try.Supplier<T[], E> arraySupplier) {
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
                public void skip(long n) throws E {
                    N.checkArgNotNegative(n, "n");

                    if (isInitialized == false) {
                        init();
                    }

                    if (n <= 0) {
                        return;
                    } else if (n > len - position) {
                        position = len;
                    }

                    position += n;
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
         * @return true, if successful
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
        public void skip(long n) throws E {
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
}
