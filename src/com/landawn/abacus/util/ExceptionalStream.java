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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Executor;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.exception.DuplicatedResultException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.Fn.Factory;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.StringUtil.Strings;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.stream.Collector;
import com.landawn.abacus.util.stream.Collectors;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 * The Stream will be automatically closed after execution(A terminal method is executed/triggered).
 * 
 * @since 1.3
 * 
 * @author Haiyang Li
 */
@SequentialOnly
public class ExceptionalStream<T, E extends Exception> implements AutoCloseable {
    static final Logger logger = LoggerFactory.getLogger(ExceptionalStream.class);

    private final ExceptionalIterator<T, E> elements;
    private final boolean sorted;
    private final Comparator<? super T> comparator;
    private final Deque<Try.Runnable<? extends E>> closeHandlers;
    private boolean isClosed = false;

    ExceptionalStream(final ExceptionalIterator<T, E> iter) {
        this(iter, false, null, null);
    }

    ExceptionalStream(final ExceptionalIterator<T, E> iter, final Deque<Try.Runnable<? extends E>> closeHandlers) {
        this(iter, false, null, closeHandlers);
    }

    ExceptionalStream(final ExceptionalIterator<T, E> iter, final boolean sorted, final Comparator<? super T> comparator,
            final Deque<Try.Runnable<? extends E>> closeHandlers) {
        this.elements = iter;
        this.sorted = sorted;
        this.comparator = comparator;
        this.closeHandlers = closeHandlers;
    }

    public static <T, E extends Exception> ExceptionalStream<T, E> empty() {
        return new ExceptionalStream<>(ExceptionalIterator.EMPTY);
    }

    public static <T, E extends Exception> ExceptionalStream<T, E> just(final T e) {
        return of(e);
    }

    public static <T, E extends Exception> ExceptionalStream<T, E> ofNullable(final T e) {
        if (e == null) {
            return empty();
        }

        return of(e);
    }

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

    public static <T, E extends Exception> ExceptionalStream<T, E> of(final Collection<? extends T> c) {
        if (N.isNullOrEmpty(c)) {
            return empty();
        }

        return of(c.iterator());
    }

    public static <T, E extends Exception> ExceptionalStream<T, E> of(final Iterator<? extends T> iter) {
        if (iter == null) {
            return empty();
        }

        return newStream(new ExceptionalIterator<T, E>() {
            @Override
            public boolean hasNext() throws E {
                return iter.hasNext();
            }

            @Override
            public T next() throws E {
                return iter.next();
            }
        });
    }

    public static <T, E extends Exception> ExceptionalStream<T, E> of(final Iterable<? extends T> iterable) {
        if (iterable == null) {
            return empty();
        }

        return of(iterable.iterator());
    }

    public static <K, V, E extends Exception> ExceptionalStream<Map.Entry<K, V>, E> of(final Map<K, V> m) {
        if (m == null) {
            return empty();
        }

        return of(m.entrySet());
    }

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

    public static <T, E extends Exception> ExceptionalStream<T, E> of(final Collection<? extends T> c, final Class<E> exceptionType) {
        return of(c);
    }

    public static <T, E extends Exception> ExceptionalStream<T, E> of(final Iterator<? extends T> iter, final Class<E> exceptionType) {
        return of(iter);
    }

    public static <T, E extends Exception> ExceptionalStream<T, E> of(final Iterable<? extends T> iterable, final Class<E> exceptionType) {
        return of(iterable);
    }

    public static <K, V, E extends Exception> ExceptionalStream<Map.Entry<K, V>, E> of(final Map<K, V> m, final Class<E> exceptionType) {
        return of(m);
    }

    public static <T, E extends Exception> ExceptionalStream<T, E> of(final Stream<? extends T> stream, final Class<E> exceptionType) {
        return of(stream);
    }

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

    public static ExceptionalStream<String, IOException> lines(final File file) {
        return lines(file, Charsets.UTF_8);
    }

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

    public static ExceptionalStream<String, IOException> lines(final Path path) {
        return lines(path, Charsets.UTF_8);
    }

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

    public static ExceptionalStream<String, IOException> lines(final Reader reader) {
        N.checkArgNotNull(reader, "reader");

        return newStream(createLazyLineIterator(null, null, Charsets.UTF_8, reader, false));
    }

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
                                IOUtil.close(reader);
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
     * @param targetClass Array/List/Map or Entity with getter/setter methods.
     * @param resultSet
     * @return
     */
    public static <T> ExceptionalStream<T, SQLException> rows(final Class<T> targetClass, final ResultSet resultSet) {
        N.checkArgNotNull(targetClass, "targetClass");
        N.checkArgNotNull(resultSet, "resultSet");

        return rows(resultSet, JdbcUtil.BiRowMapper.to(targetClass));
    }

    /**
     * 
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
                    JdbcUtil.closeQuietly(resultSet);
                }
            });
        } else {
            return rows(targetClass, resultSet);
        }
    }

    /**
     * It's user's responsibility to close the input <code>resultSet</code> after the stream is finished.
     * 
     * @param resultSet
     * @param rowMapper
     * @return
     */
    public static <T> ExceptionalStream<T, SQLException> rows(final ResultSet resultSet, final JdbcUtil.RowMapper<T> rowMapper) {
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

                JdbcUtil.skip(resultSet, m);

                hasNext = false;
            }
        };

        return newStream(iter);
    }

    /**
     * It's user's responsibility to close the input <code>resultSet</code> after the stream is finished.
     * 
     * @param resultSet
     * @param rowMapper
     * @return
     */
    public static <T> ExceptionalStream<T, SQLException> rows(final ResultSet resultSet, final JdbcUtil.BiRowMapper<T> rowMapper) {
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
                    columnLabels = JdbcUtil.getColumnLabelList(resultSet);
                }

                return rowMapper.apply(resultSet, columnLabels);
            }

            @Override
            public void skip(long n) throws SQLException {
                N.checkArgNotNegative(n, "n");

                final long m = hasNext ? n - 1 : n;

                JdbcUtil.skip(resultSet, m);

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

                final T next = (T) JdbcUtil.getColumnValue(resultSet, newColumnIndex);
                hasNext = false;
                return next;
            }

            @Override
            public void skip(long n) throws SQLException {
                N.checkArgNotNegative(n, "n");

                final long m = hasNext ? n - 1 : n;

                JdbcUtil.skip(resultSet, m);

                hasNext = false;
            }
        };

        return newStream(iter);
    }

    /**
     * 
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
                    JdbcUtil.closeQuietly(resultSet);
                }
            });
        } else {
            return rows(resultSet, columnIndex);
        }
    }

    /**
     * It's user's responsibility to close the input <code>resultSet</code> after the stream is finished.
     * 
     * @param resultSet
     * @param columnName
     * @return
     * @throws UncheckedSQLException
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

                columnIndex = columnIndex == -1 ? getColumnIndex(resultSet, columnName) : columnIndex;

                final T next = (T) JdbcUtil.getColumnValue(resultSet, columnIndex);
                hasNext = false;
                return next;
            }

            @Override
            public void skip(long n) throws SQLException {
                N.checkArgNotNegative(n, "n");

                final long m = hasNext ? n - 1 : n;

                JdbcUtil.skip(resultSet, m);

                hasNext = false;
            }
        };

        return newStream(iter);
    }

    /**
     * 
     * @param resultSet
     * @param columnName
     * @param closeResultSet
     * @return
     * @deprecated
     * @throws UncheckedSQLException
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
                    JdbcUtil.closeQuietly(resultSet);
                }
            });
        } else {
            return rows(resultSet, columnName);
        }
    }

    private static int getColumnIndex(final ResultSet resultSet, final String columnName) throws UncheckedSQLException {
        int columnIndex = -1;

        try {
            final ResultSetMetaData rsmd = resultSet.getMetaData();
            final int columnCount = rsmd.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                if (JdbcUtil.getColumnLabel(rsmd, i).equals(columnName)) {
                    columnIndex = i - 1;
                    break;
                }
            }
        } catch (SQLException e) {
            throw new UncheckedSQLException(e);
        }

        N.checkArgument(columnIndex >= 0, "No column found by name %s", columnName);

        return columnIndex;
    }

    @SafeVarargs
    public static <T, E extends Exception> ExceptionalStream<T, E> concat(final ExceptionalStream<? extends T, E>... a) {
        if (N.isNullOrEmpty(a)) {
            return empty();
        }

        return concat(N.asList(a));
    }

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
     * @param occurrencesFilter
     * @return
     */
    public ExceptionalStream<T, E> distinct() {
        final Set<Object> set = new HashSet<>();

        return filter(new Try.Predicate<T, E>() {
            @Override
            public boolean test(T value) {
                return set.add(hashKey(value));
            }
        });
    }

    /**
     * Distinct by the value mapped from <code>keyMapper</code> 
     * 
     * @param keyMapper don't change value of the input parameter.
     * @return
     */
    public ExceptionalStream<T, E> distinctBy(final Try.Function<? super T, ?, ? extends E> keyMapper) {
        checkArgNotNull(keyMapper, "keyMapper");

        final Set<Object> set = new HashSet<>();

        return filter(new Try.Predicate<T, E>() {
            @Override
            public boolean test(T value) throws E {
                return set.add(hashKey(keyMapper.apply(value)));
            }
        });
    }

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

    public <R> ExceptionalStream<R, E> flattMap(final Try.Function<? super T, ? extends Collection<? extends R>, ? extends E> mapper) {
        checkArgNotNull(mapper, "mapper");

        return flatMap(new Try.Function<T, ExceptionalStream<? extends R, ? extends E>, E>() {
            @Override
            public ExceptionalStream<? extends R, ? extends E> apply(T t) throws E {
                return ExceptionalStream.of(mapper.apply(t));
            }
        });
    }

    public <R> ExceptionalStream<R, E> slidingMap(Try.BiFunction<? super T, ? super T, R, E> mapper) {
        return slidingMap(mapper, 1);
    }

    public <R> ExceptionalStream<R, E> slidingMap(Try.BiFunction<? super T, ? super T, R, E> mapper, int increment) {
        return slidingMap(mapper, increment, false);
    }

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

    public <R> ExceptionalStream<R, E> slidingMap(Try.TriFunction<? super T, ? super T, ? super T, R, E> mapper) {
        return slidingMap(mapper, 1);
    }

    public <R> ExceptionalStream<R, E> slidingMap(Try.TriFunction<? super T, ? super T, ? super T, R, E> mapper, int increment) {
        return slidingMap(mapper, increment, false);
    }

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
     * @param keyMapper
     * @return
     */
    public <K> ExceptionalStream<Map.Entry<K, List<T>>, E> groupBy(final Try.Function<? super T, ? extends K, ? extends E> keyMapper) {
        return groupBy(keyMapper, Suppliers.<K, List<T>> ofMap());
    }

    /**
     * 
     * @param keyMapper
     * @param mapFactory
     * @return
     */
    public <K> ExceptionalStream<Map.Entry<K, List<T>>, E> groupBy(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Supplier<? extends Map<K, List<T>>> mapFactory) {
        return groupBy(keyMapper, Fn.FN.<T, E> identity(), mapFactory);
    }

    /**
     * 
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

    public <K, V> ExceptionalStream<Map.Entry<K, V>, E> groupBy(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Try.Function<? super T, ? extends V, ? extends E> valueMapper, Try.BinaryOperator<V, ? extends E> mergeFunction) {
        return groupBy(keyMapper, valueMapper, mergeFunction, Suppliers.<K, V> ofMap());
    }

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @param mapFactory
     * @return
     * @see {@link Fn.FN#throwingMerger()}
     * @see {@link Fn.FN#replacingMerger()}
     * @see {@link Fn.FN#ignoringMerger()}
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

    public <K, A, D> ExceptionalStream<Map.Entry<K, D>, E> groupBy(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Collector<? super T, A, D> downstream) throws E {
        return groupBy(keyMapper, downstream, Suppliers.<K, D> ofMap());
    }

    public <K, A, D, M extends Map<K, D>> ExceptionalStream<Map.Entry<K, D>, E> groupBy(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Collector<? super T, A, D> downstream, final Supplier<? extends M> mapFactory) throws E {
        return groupBy(keyMapper, Fn.FN.<T, E> identity(), downstream, mapFactory);
    }

    public <K, V, A, D, M extends Map<K, D>> ExceptionalStream<Map.Entry<K, D>, E> groupBy(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Try.Function<? super T, ? extends V, ? extends E> valueMapper, final Collector<? super V, A, D> downstream) throws E {
        return groupBy(keyMapper, valueMapper, downstream, Suppliers.<K, D> ofMap());
    }

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

    public ExceptionalStream<T, E> prepend(ExceptionalStream<T, E> s) {
        return concat(s, this);
    }

    public ExceptionalStream<T, E> append(ExceptionalStream<T, E> s) {
        return concat(this, s);
    }

    public ExceptionalStream<T, E> appendIfEmpty(Supplier<? extends ExceptionalStream<T, E>> supplier) throws E {
        if (elements.hasNext() == false) {
            return append(supplier.get());
        } else {
            return this;
        }
    }

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
     * @param chunkSize the desired size of each sub sequence (the last may be smaller).
     * @return
     */
    public ExceptionalStream<List<T>, E> splitToList(final int chunkSize) {
        return split(chunkSize, Factory.<T> ofList());
    }

    /**
     * Returns ExceptionalStream of {@code Set<T>} with consecutive sub sequences of the elements, each of the same size (the final sequence may be smaller).
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

    public ExceptionalStream<Stream<T>, E> sliding(final int windowSize, final int increment) {
        return slidingToList(windowSize, increment).map(new Try.Function<List<T>, Stream<T>, E>() {
            @Override
            public Stream<T> apply(List<T> t) {
                return Stream.of(t);
            }
        });
    }

    public ExceptionalStream<List<T>, E> slidingToList(final int windowSize, final int increment) {
        return sliding(windowSize, increment, Factory.<T> ofList());
    }

    public ExceptionalStream<Set<T>, E> slidingToSet(final int windowSize, final int increment) {
        return sliding(windowSize, increment, Factory.<T> ofSet());
    }

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

    public ExceptionalStream<T, E> slice(final long from, final long to) {
        checkArgNotNegative(from, "from");
        checkArgNotNegative(to, "to");
        checkArgument(to >= from, "'to' can't be less than `from`");

        return from == 0 ? limit(to) : skip(from).limit(to - from);
    }

    public ExceptionalStream<T, E> sorted() {
        return sorted(Comparators.NATURAL_ORDER);
    }

    public ExceptionalStream<T, E> reverseSorted() {
        return sorted(Comparators.REVERSED_ORDER);
    }

    public ExceptionalStream<T, E> sorted(final Comparator<? super T> comparator) {
        final Comparator<? super T> cmp = comparator == null ? Comparators.NATURAL_ORDER : comparator;

        if (sorted && cmp == this.comparator) {
            return this;
        }

        return lazyLoad(new Function<Object[], Object[]>() {
            @Override
            public Object[] apply(final Object[] a) {
                N.sort((T[]) a, cmp);

                return a;
            }
        }, true, cmp);
    }

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

    public void forEachPair(final Try.BiConsumer<? super T, ? super T, E> action) throws E {
        forEachPair(action, 1);
    }

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

    public void forEachTriple(final Try.TriConsumer<? super T, ? super T, ? super T, E> action) throws E {
        forEachTriple(action, 1);
    }

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

    public Object[] toArray() throws E {
        assertNotClosed();

        try {
            return toList().toArray();
        } finally {
            close();
        }
    }

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

    public Set<T> toSet() throws E {
        assertNotClosed();

        try {
            final Set<T> result = new HashSet<>();

            while (elements.hasNext()) {
                result.add(elements.next());
            }

            return result;
        } finally {
            close();
        }
    }

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
     * @param keyMapper
     * @param valueMapper
     * @return
     * @throws E
     * @throws IllegalStateException if there are duplicated keys.
     * @see {@link Fn.FN#throwingMerger()}
     * @see {@link Fn.FN#replacingMerger()}
     * @see {@link Fn.FN#ignoringMerger()}
     */
    public <K, V> Map<K, V> toMap(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Try.Function<? super T, ? extends V, ? extends E> valueMapper) throws E, IllegalStateException {
        return toMap(keyMapper, valueMapper, Suppliers.<K, V> ofMap());
    }

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @throws E
     * @throws IllegalStateException if there are duplicated keys.
     * @see {@link Fn.FN#throwingMerger()}
     * @see {@link Fn.FN#replacingMerger()}
     * @see {@link Fn.FN#ignoringMerger()}
     */
    public <K, V, M extends Map<K, V>> M toMap(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Try.Function<? super T, ? extends V, ? extends E> valueMapper, final Supplier<? extends M> mapFactory) throws E, IllegalStateException {
        return toMap(keyMapper, valueMapper, Fn.FN.<V, E> throwingMerger(), mapFactory);
    }

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @return
     * @throws E
     * @see {@link Fn.FN#throwingMerger()}
     * @see {@link Fn.FN#replacingMerger()}
     * @see {@link Fn.FN#ignoringMerger()}
     */
    public <K, V> Map<K, V> toMap(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Try.Function<? super T, ? extends V, ? extends E> valueMapper, final Try.BinaryOperator<V, ? extends E> mergeFunction) throws E {
        return toMap(keyMapper, valueMapper, mergeFunction, Suppliers.<K, V> ofMap());
    }

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @param mapFactory
     * @return
     * @throws E
     * @see {@link Fn.FN#throwingMerger()}
     * @see {@link Fn.FN#replacingMerger()}
     * @see {@link Fn.FN#ignoringMerger()}
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
     * @param keyMapper
     * @param downstream
     * @return
     * @throws E 
     */
    public <K, A, D> Map<K, D> toMap(final Try.Function<? super T, ? extends K, ? extends E> keyMapper, final Collector<? super T, A, D> downstream) throws E {
        return toMap(keyMapper, downstream, Suppliers.<K, D> ofMap());
    }

    /**
     * 
     * @param keyMapper
     * @param downstream
     * @param mapFactory
     * @return
     * @throws E 
     */
    public <K, A, D, M extends Map<K, D>> M toMap(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Collector<? super T, A, D> downstream, final Supplier<? extends M> mapFactory) throws E {
        return toMap(keyMapper, Fn.FN.<T, E> identity(), downstream, mapFactory);
    }

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param downstream
     * @return
     * @throws E 
     */
    public <K, V, A, D> Map<K, D> toMap(final Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            final Try.Function<? super T, ? extends V, ? extends E> valueMapper, final Collector<? super V, A, D> downstream) throws E {
        return toMap(keyMapper, valueMapper, downstream, Suppliers.<K, D> ofMap());
    }

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param downstream
     * @param mapFactory
     * @return
     * @throws E 
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
     * @param keyMapper
     * @return
     * @see Collectors#groupingBy(Function)
     */
    public <K> Map<K, List<T>> groupTo(Try.Function<? super T, ? extends K, ? extends E> keyMapper) throws E {
        return groupTo(keyMapper, Suppliers.<K, List<T>> ofMap());
    }

    /**
     * 
     * @param keyMapper
     * @param mapFactory
     * @return
     * @see Collectors#groupingBy(Function, Supplier)
     */
    public <K, M extends Map<K, List<T>>> M groupTo(final Try.Function<? super T, ? extends K, ? extends E> keyMapper, final Supplier<? extends M> mapFactory)
            throws E {
        final Try.Function<T, T, E> valueMapper = Fn.FN.identity();

        return groupTo(keyMapper, valueMapper, mapFactory);
    }

    public <K, V> Map<K, List<V>> groupTo(Try.Function<? super T, ? extends K, ? extends E> keyMapper,
            Try.Function<? super T, ? extends V, ? extends E> valueMapper) throws E {
        return groupTo(keyMapper, valueMapper, Suppliers.<K, List<V>> ofMap());
    }

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
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
     * @throws E
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

    public OptionalInt sumInt(Try.ToIntFunction<? super T, E> func) throws E {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return OptionalInt.empty();
            }

            long sum = 0;

            while (elements.hasNext()) {
                sum += func.applyAsInt(elements.next());
            }

            return OptionalInt.of(N.toIntExact(sum));
        } finally {
            close();
        }
    }

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

    public OptionalDouble sumDouble(Try.ToDoubleFunction<? super T, E> func) throws E {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return OptionalDouble.empty();
            }

            final List<Double> list = new ArrayList<>();

            while (elements.hasNext()) {
                list.add(func.applyAsDouble(elements.next()));
            }

            return OptionalDouble.of(N.sumDouble(list));
        } finally {
            close();
        }
    }

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

    public OptionalDouble averageDouble(Try.ToDoubleFunction<? super T, E> func) throws E {
        assertNotClosed();

        try {
            if (elements.hasNext() == false) {
                return OptionalDouble.empty();
            }

            final List<Double> list = new ArrayList<>();

            while (elements.hasNext()) {
                list.add(func.applyAsDouble(elements.next()));
            }

            return N.averageLong(list);
        } finally {
            close();
        }
    }

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

    public <R, RR, A> RR collectAndThen(final Collector<? super T, A, R> collector, final Try.Function<? super R, ? extends RR, E> func) throws E {
        checkArgNotNull(collector, "collector");
        checkArgNotNull(func, "func");
        assertNotClosed();

        return func.apply(collect(collector));
    }

    public <R, RR, A> RR collectAndThen(final java.util.stream.Collector<? super T, A, R> collector, final Try.Function<? super R, ? extends RR, E> func)
            throws E {
        checkArgNotNull(collector, "collector");
        checkArgNotNull(func, "func");
        assertNotClosed();

        return func.apply(collect(collector));
    }

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

    public <R> R __(Try.Function<? super ExceptionalStream<T, E>, R, ? extends E> transfer) throws E {
        checkArgNotNull(transfer, "transfer");

        return transfer.apply(this);
    }

    /** 
     * 
     * @param action a terminal operation should be called.
     * @return
     */
    @Beta
    public ContinuableFuture<Void> asyncRun(final Try.Consumer<? super ExceptionalStream<T, E>, E> action) {
        checkArgNotNull(action, "action");

        return ContinuableFuture.run(new Try.Runnable<E>() {
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
     * @param action a terminal operation should be called.
     * @return
     */
    @Beta
    public <R> ContinuableFuture<R> asyncCall(final Try.Function<? super ExceptionalStream<T, E>, R, E> action) {
        checkArgNotNull(action, "action");

        return ContinuableFuture.call(new Try.Callable<R, E>() {
            @Override
            public R call() throws E {
                return action.apply(ExceptionalStream.this);
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

    void assertNotClosed() {
        if (isClosed) {
            throw new IllegalStateException("This stream has been closed");
        }
    }

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

    static <T, E extends Exception> ExceptionalStream<T, E> newStream(final ExceptionalIterator<T, E> iter) {
        return new ExceptionalStream<>(iter, null);
    }

    static <T, E extends Exception> ExceptionalStream<T, E> newStream(final ExceptionalIterator<T, E> iter,
            final Deque<Try.Runnable<? extends E>> closeHandlers) {
        return new ExceptionalStream<>(iter, closeHandlers);
    }

    static <T, E extends Exception> ExceptionalStream<T, E> newStream(final ExceptionalIterator<T, E> iter, final boolean sorted,
            final Comparator<? super T> comparator, final Deque<Try.Runnable<? extends E>> closeHandlers) {
        return new ExceptionalStream<>(iter, sorted, comparator, closeHandlers);
    }

    static Object hashKey(Object obj) {
        return obj == null || obj.getClass().isArray() == false ? obj : Wrapper.of(obj);
    }

    static boolean isSameComparator(Comparator<?> a, Comparator<?> b) {
        return a == b || (a == null && b == Comparators.NATURAL_ORDER) || (b == null && a == Comparators.NATURAL_ORDER);
    }

    public static final class StreamE<T, E extends Exception> extends ExceptionalStream<T, E> {
        StreamE(ExceptionalIterator<T, E> iter, boolean sorted, Comparator<? super T> comparator, Deque<Try.Runnable<? extends E>> closeHandlers) {
            super(iter, sorted, comparator, closeHandlers);
        }
    }

    static abstract class ExceptionalIterator<T, E extends Exception> {

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

        /**
         * Lazy evaluation.
         * 
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

        public abstract boolean hasNext() throws E;

        public abstract T next() throws E;

        public void skip(long n) throws E {
            N.checkArgNotNegative(n, "n");

            while (n-- > 0 && hasNext()) {
                next();
            }
        }

        public long count() throws E {
            long result = 0;

            while (hasNext()) {
                next();
                result++;
            }

            return result;
        }

        public void close() throws E {
            // Nothing to do by default.
        }
    }
}
