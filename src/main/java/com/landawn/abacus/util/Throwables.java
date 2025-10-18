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
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Internal;
import com.landawn.abacus.util.u.Nullable;

/**
 * The Throwables class is a utility class that provides methods for handling exceptions.
 * It includes methods for running commands that may throw exceptions, calling methods that may throw exceptions, and more.
 * It also provides a variety of functional interfaces that can throw exceptions.
 *
 */
@SuppressWarnings({ "java:S6539" })
public final class Throwables {

    private Throwables() {
        // Singleton for utility class.
    }

    /**
     * Executes the specified runnable command that may throw a checked exception.
     * If the command throws an exception, it will be wrapped in a RuntimeException and rethrown.
     *
     * @param cmd the runnable command to execute that may throw a checked exception
     * @throws RuntimeException if the command throws any exception, the original exception will be wrapped in a RuntimeException
     * @throws IllegalArgumentException if cmd is null
     * @see Try#run(Throwables.Runnable)
     */
    @Beta
    public static void run(final Throwables.Runnable<? extends Throwable> cmd) {
        N.checkArgNotNull(cmd, "cmd");

        try {
            cmd.run();
        } catch (final Throwable e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Executes the specified runnable command that may throw a checked exception.
     * If the command throws an exception, the specified error handler will be invoked with the exception.
     *
     * @param cmd the runnable command to execute that may throw a checked exception
     * @param actionOnError the consumer that will handle any exception thrown by the command
     * @throws IllegalArgumentException if cmd or actionOnError is null
     * @see Try#run(Throwables.Runnable, java.util.function.Consumer)
     */
    @Beta
    public static void run(final Throwables.Runnable<? extends Throwable> cmd, final java.util.function.Consumer<? super Throwable> actionOnError) {
        N.checkArgNotNull(cmd, "cmd");
        N.checkArgNotNull(actionOnError, "actionOnError");

        try {
            cmd.run();
        } catch (final Throwable e) {
            actionOnError.accept(e);
        }
    }

    /**
     * Executes the specified callable command that may throw a checked exception and returns its result.
     * If the command throws an exception, it will be wrapped in a RuntimeException and rethrown.
     *
     * @param <R> the type of the result returned by the callable
     * @param cmd the callable command to execute that may throw a checked exception
     * @return the result returned by the callable command
     * @throws RuntimeException if the command throws any exception, the original exception will be wrapped in a RuntimeException
     * @throws IllegalArgumentException if cmd is null
     * @see Try#call(java.util.concurrent.Callable)
     */
    @Beta
    public static <R> R call(final Throwables.Callable<R, ? extends Throwable> cmd) {
        N.checkArgNotNull(cmd, "cmd");

        try {
            return cmd.call();
        } catch (final Throwable e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Executes the specified callable command that may throw a checked exception and returns its result.
     * If the command throws an exception, the specified error handler function will be invoked with the exception
     * and its result will be returned instead.
     *
     * @param <R> the type of the result returned by the callable or the error handler
     * @param cmd the callable command to execute that may throw a checked exception
     * @param actionOnError the function that will handle any exception thrown by the command and provide an alternative result
     * @return the result returned by the callable command if successful, or the result of the error handler if an exception occurs
     * @throws IllegalArgumentException if cmd or actionOnError is null
     * @see Try#call(java.util.concurrent.Callable, java.util.function.Function)
     */
    @Beta
    public static <R> R call(final Throwables.Callable<R, ? extends Throwable> cmd,
            final java.util.function.Function<? super Throwable, ? extends R> actionOnError) {
        N.checkArgNotNull(cmd, "cmd");
        N.checkArgNotNull(actionOnError, "actionOnError");

        try {
            return cmd.call();
        } catch (final Throwable e) {
            return actionOnError.apply(e);
        }
    }

    /**
     * Executes the specified callable command that may throw a checked exception and returns its result.
     * If the command throws an exception, the result from the specified supplier will be returned instead.
     * This method provides a safe way to handle exceptions by providing a fallback value supplier.
     *
     * <p>Example usage:
     * <pre>{@code
     * String result = Throwables.call(
     *     () -> riskyOperation(),
     *     () -> "default value"
     * );
     * }</pre>
     *
     * @param <R> the type of the result returned by the callable or the supplier
     * @param cmd the callable command to execute that may throw a checked exception
     * @param supplier the supplier that provides an alternative result if the command throws an exception
     * @return the result returned by the callable command if successful, or the result from the supplier if an exception occurs
     * @throws IllegalArgumentException if cmd or supplier is null
     * @see Try#call(java.util.concurrent.Callable, java.util.function.Supplier)
     */
    @Beta
    public static <R> R call(final Throwables.Callable<R, ? extends Throwable> cmd, final java.util.function.Supplier<R> supplier) {
        N.checkArgNotNull(cmd, "cmd");
        N.checkArgNotNull(supplier, "supplier");

        try {
            return cmd.call();
        } catch (final Throwable e) {
            return supplier.get();
        }
    }

    /**
     * Executes the specified callable command that may throw a checked exception and returns its result.
     * If the command throws an exception, the specified default value will be returned instead.
     *
     * @param <R> the type of the result returned by the callable or the default value
     * @param cmd the callable command to execute that may throw a checked exception
     * @param defaultValue the default value to return if the command throws an exception
     * @return the result returned by the callable command if successful, or the default value if an exception occurs
     * @throws IllegalArgumentException if cmd is null
     * @see #call(Throwables.Callable, java.util.function.Supplier)
     */
    @Beta
    // <R extends Comparable<? super R>> to avoid ambiguous error with Comparable<R>. Comparable is most common super interface for all types.
    public static <R extends Comparable<? super R>> R call(final Throwables.Callable<R, ? extends Throwable> cmd, final R defaultValue) {
        N.checkArgNotNull(cmd, "cmd");

        try {
            return cmd.call();
        } catch (final Throwable e) {
            return defaultValue;
        }
    }

    /**
     * Executes the specified callable command that may throw a checked exception and returns its result.
     * If the command throws an exception and the predicate returns {@code true} for that exception,
     * the result from the supplier will be returned. If the predicate returns false,
     * the exception will be wrapped in a RuntimeException and rethrown.
     *
     * @param <R> the type of the result returned by the callable or the supplier
     * @param cmd the callable command to execute that may throw a checked exception
     * @param predicate the predicate that tests whether to handle the exception or rethrow it
     * @param supplier the supplier that provides an alternative result if the predicate returns true
     * @return the result returned by the callable command if successful, or the result from the supplier if an exception occurs and the predicate returns true
     * @throws RuntimeException if the command throws an exception and the predicate returns false
     * @throws IllegalArgumentException if cmd, predicate, or supplier is null
     * @see Try#call(java.util.concurrent.Callable, java.util.function.Predicate, java.util.function.Supplier)
     */
    @Beta
    public static <R> R call(final Throwables.Callable<R, ? extends Throwable> cmd, final java.util.function.Predicate<? super Throwable> predicate,
            final java.util.function.Supplier<R> supplier) {
        N.checkArgNotNull(cmd, "cmd");
        N.checkArgNotNull(predicate, "predicate");
        N.checkArgNotNull(supplier, "supplier");

        try {
            return cmd.call();
        } catch (final Throwable e) {
            if (predicate.test(e)) {
                return supplier.get();
            } else {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }
    }

    /**
     * Executes the specified callable command that may throw a checked exception and returns its result.
     * If the command throws an exception and the predicate returns {@code true} for that exception,
     * the specified default value will be returned. If the predicate returns false,
     * the exception will be wrapped in a RuntimeException and rethrown.
     *
     * @param <R> the type of the result returned by the callable or the default value
     * @param cmd the callable command to execute that may throw a checked exception
     * @param predicate the predicate that tests whether to handle the exception or rethrow it
     * @param defaultValue the default value to return if the predicate returns true
     * @return the result returned by the callable command if successful, or the default value if an exception occurs and the predicate returns true
     * @throws RuntimeException if the command throws an exception and the predicate returns false
     * @throws IllegalArgumentException if cmd or predicate is null
     * @see #call(Throwables.Callable, java.util.function.Predicate, java.util.function.Supplier)
     */
    @Beta
    // <R extends Comparable<? super R>> to avoid ambiguous error with Comparable<R>. Comparable is most common super interface for all types.
    public static <R extends Comparable<? super R>> R call(final Throwables.Callable<R, ? extends Throwable> cmd,
            final java.util.function.Predicate<? super Throwable> predicate, final R defaultValue) {
        N.checkArgNotNull(cmd, "cmd");
        N.checkArgNotNull(predicate, "predicate");

        try {
            return cmd.call();
        } catch (final Throwable e) {
            if (predicate.test(e)) {
                return defaultValue;
            } else {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private static final Throwables.Iterator EMPTY = new Throwables.Iterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
        }
    };

    /**
     *
     * @param <T>
     * @param <E>
     * @see ObjIterator
     */
    @SuppressWarnings({ "java:S6548" })
    public abstract static class Iterator<T, E extends Throwable> implements AutoCloseable, Immutable {
        /**
         * Returns an empty iterator that has no elements and whose hasNext() always returns false.
         *
         * @param <T> the type of elements that would be returned by this iterator
         * @param <E> the type of exception that may be thrown
         * @return an empty iterator
         */
        public static <T, E extends Throwable> Throwables.Iterator<T, E> empty() {
            return EMPTY;
        }

        /**
         * Returns an iterator containing only the specified single element.
         *
         * @param <T> the type of the element
         * @param <E> the type of exception that may be thrown
         * @param val the single element to be contained in the iterator
         * @return an iterator containing only the specified element
         */
        public static <T, E extends Throwable> Throwables.Iterator<T, E> just(final T val) {
            return new Throwables.Iterator<>() {
                private boolean done = false;

                @Override
                public boolean hasNext() {
                    return !done;
                }

                @Override
                public T next() {
                    if (done) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    done = true;

                    return val;
                }
            };
        }

        /**
         * Returns an iterator over the specified array of elements.
         *
         * @param <T> the type of elements in the array
         * @param <E> the type of exception that may be thrown
         * @param a the array of elements to iterate over
         * @return an iterator over the elements in the array, or an empty iterator if the array is null or empty
         */
        @SafeVarargs
        public static <T, E extends Exception> Throwables.Iterator<T, E> of(final T... a) {
            return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
        }

        /**
         * Returns an iterator over a range of elements in the specified array.
         *
         * @param <T> the type of elements in the array
         * @param <E> the type of exception that may be thrown
         * @param a the array of elements to iterate over
         * @param fromIndex the starting index (inclusive) of the range to iterate
         * @param toIndex the ending index (exclusive) of the range to iterate
         * @return an iterator over the specified range of elements in the array
         * @throws IndexOutOfBoundsException if fromIndex is negative, toIndex is greater than the array length,
         *         or fromIndex is greater than toIndex
         */
        public static <T, E extends Exception> Throwables.Iterator<T, E> of(final T[] a, final int fromIndex, final int toIndex)
                throws IndexOutOfBoundsException {
            N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

            if (N.isEmpty(a) || fromIndex == toIndex) {
                return EMPTY;
            }

            return new Throwables.Iterator<>() {
                private int cursor = fromIndex;

                @Override
                public boolean hasNext() {
                    return cursor < toIndex;
                }

                @Override
                public T next() {
                    if (cursor >= toIndex) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    return a[cursor++];
                }

                @Override
                public void advance(final long n) throws E {
                    if (n <= 0) {
                        return;
                    }

                    final long remaining = toIndex - cursor;
                    if (n >= remaining) {
                        cursor = toIndex;
                    } else {
                        // Safe cast since n < remaining and remaining fits in int
                        cursor += (int) n;
                    }
                }

                @Override
                public long count() {
                    final int ret = toIndex - cursor; //NOSONAR
                    cursor = toIndex; // Mark as finished.
                    return ret;
                }
            };
        }

        /**
         * Returns an iterator over the elements in the specified Iterable.
         *
         * @param <T> the type of elements in the iterable
         * @param <E> the type of exception that may be thrown
         * @param iterable the iterable whose elements are to be iterated over
         * @return an iterator over the elements in the iterable, or an empty iterator if the iterable is null
         */
        public static <T, E extends Throwable> Iterator<T, E> of(final Iterable<? extends T> iterable) {
            if (iterable == null) {
                return empty();
            }

            final java.util.Iterator<? extends T> iter = iterable.iterator();

            return new Throwables.Iterator<>() {
                @Override
                public boolean hasNext() {
                    return iter.hasNext();
                }

                @Override
                public T next() throws E {
                    return iter.next();
                }
            };
        }

        /**
         * Returns a Throwables.Iterator that wraps the specified java.util.Iterator.
         *
         * @param <T> the type of elements returned by the iterator
         * @param <E> the type of exception that may be thrown
         * @param iter the java.util.Iterator to wrap
         * @return a Throwables.Iterator wrapping the specified iterator, or an empty iterator if iter is null
         */
        public static <T, E extends Exception> Throwables.Iterator<T, E> of(final java.util.Iterator<? extends T> iter) {
            if (iter == null) {
                return EMPTY;
            }

            return new Throwables.Iterator<>() {
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
         * Returns a Throwables.Iterator instance that is created lazily using the provided Supplier.
         * The Supplier is responsible for producing the Iterator instance when the Iterator's methods are first called.
         * This is useful for deferring expensive operations until the iterator is actually used.
         *
         * <p>The iterator is initialized on the first call to {@code hasNext()}, {@code next()}, {@code advance()}, or {@code count()}.
         * The underlying iterator is only closed if it has been initialized when {@code close()} is called.
         *
         * <p>Example usage:
         * <pre>{@code
         * Iterator<String, IOException> iter = Iterator.defer(() ->
         *     Iterator.ofLines(new FileReader("large-file.txt"))
         * );
         * // File is not opened until iter.hasNext() or iter.next() is called
         * }</pre>
         *
         * @param <T> the type of the elements in the Iterator.
         * @param <E> the type of the exception that may be thrown.
         * @param iteratorSupplier a Supplier that provides the Throwables.Iterator when needed.
         * @return a Throwables.Iterator that is initialized on the first call to hasNext() or next().
         * @throws IllegalArgumentException if iteratorSupplier is {@code null}.
         */
        public static <T, E extends Exception> Throwables.Iterator<T, E> defer(final java.util.function.Supplier<Throwables.Iterator<T, E>> iteratorSupplier) {
            N.checkArgNotNull(iteratorSupplier, cs.iteratorSupplier);

            return new Throwables.Iterator<>() {
                private Throwables.Iterator<T, E> iter = null;
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
                public void advance(final long n) throws E {
                    if (n <= 0) {
                        return;
                    }

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
                protected void closeResource() {
                    // Only close if already initialized - don't initialize just to close
                    if (isInitialized && iter != null) {
                        iter.close();
                    }
                }

                private void init() {
                    if (!isInitialized) {
                        isInitialized = true;
                        iter = iteratorSupplier.get();
                    }
                }
            };
        }

        /**
         * Concatenates multiple iterators into a single iterator that iterates over all elements
         * from all the iterators in sequence.
         *
         * @param <T> the type of elements returned by the iterators
         * @param <E> the type of exception that may be thrown
         * @param a the array of iterators to concatenate
         * @return a single iterator that iterates over all elements from all iterators in order
         */
        @SafeVarargs
        public static <T, E extends Exception> Throwables.Iterator<T, E> concat(final Throwables.Iterator<? extends T, ? extends E>... a) {
            return concat(N.asList(a));
        }

        /**
         * Concatenates a collection of iterators into a single iterator that iterates over all elements
         * from all the iterators in sequence.
         *
         * @param <T> the type of elements returned by the iterators
         * @param <E> the type of exception that may be thrown
         * @param c the collection of iterators to concatenate
         * @return a single iterator that iterates over all elements from all iterators in order,
         *         or an empty iterator if the collection is null or empty
         */
        public static <T, E extends Exception> Throwables.Iterator<T, E> concat(final Collection<? extends Throwables.Iterator<? extends T, ? extends E>> c) {
            if (N.isEmpty(c)) {
                return Iterator.empty();
            }

            return new Throwables.Iterator<>() {
                private final java.util.Iterator<? extends Throwables.Iterator<? extends T, ? extends E>> iter = c.iterator();
                private Throwables.Iterator<? extends T, ? extends E> cur;

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
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    return cur.next();
                }
            };
        }

        /**
         * Returns an iterator that reads lines from the specified Reader.
         * The iterator wraps the reader in a BufferedReader if it isn't one already.
         *
         * <p><b>Resource Management:</b> This iterator implements AutoCloseable. When {@code close()} is called,
         * it will close the underlying BufferedReader (and thus the original Reader). It is recommended to use
         * this iterator in a try-with-resources statement to ensure proper resource cleanup.
         *
         * <p>Example usage:
         * <pre>{@code
         * try (Iterator<String, IOException> lines = Iterator.ofLines(new FileReader("file.txt"))) {
         *     while (lines.hasNext()) {
         *         System.out.println(lines.next());
         *     }
         * }
         * }</pre>
         *
         * @param reader the Reader to read lines from
         * @return an iterator over the lines in the reader, or an empty iterator if the reader is null
         * @throws IOException if an I/O error occurs while reading from the reader
         */
        public static Throwables.Iterator<String, IOException> ofLines(final Reader reader) {
            if (reader == null) {
                return empty();
            }

            return new Throwables.Iterator<>() {
                private final BufferedReader br = reader instanceof BufferedReader ? (BufferedReader) reader : new BufferedReader(reader);
                private String cachedLine;
                /** A flag indicating if the iterator has been fully read. */
                private boolean finished = false;

                @Override
                public boolean hasNext() throws IOException {
                    if (cachedLine != null) {
                        return true;
                    } else if (finished) {
                        return false;
                    } else {
                        cachedLine = br.readLine();

                        if (cachedLine == null) {
                            finished = true;
                            return false;
                        } else {
                            return true;
                        }
                    }
                }

                @Override
                public String next() throws IOException {
                    if (!hasNext()) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final String res = cachedLine;
                    cachedLine = null;
                    return res;
                }

                @Override
                protected void closeResource() {
                    try {
                        br.close();
                    } catch (final IOException e) {
                        // Suppress exception during close - closeResource doesn't throw checked exceptions
                    }
                }
            };
        }

        /**
         * Returns true if the iterator has more elements to iterate over.
         *
         * @return {@code true} if there are more elements, {@code false} otherwise
         * @throws E if an exception occurs while checking for more elements
         */
        public abstract boolean hasNext() throws E;

        /**
         * Returns the next element in the iteration.
         *
         * @return the next element in the iteration
         * @throws E if an exception occurs while retrieving the next element
         * @throws NoSuchElementException if there are no more elements
         */
        public abstract T next() throws E;

        /**
         * Advances the iterator by skipping the specified number of elements.
         * If n is greater than the number of remaining elements, the iterator will be positioned at the end.
         *
         * @param n the number of elements to skip, must be non-negative 
         * @throws E if an exception occurs while advancing the iterator
         */
        public void advance(long n) throws E {
            if (n <= 0) {
                return;
            }

            while (n-- > 0 && hasNext()) {
                next();
            }
        }

        /**
         * Returns the count of remaining elements in the iterator.
         * This method will consume all remaining elements in the iterator.
         *
         * @return the number of remaining elements
         * @throws E if an exception occurs while counting the elements
         */
        public long count() throws E {
            long result = 0;

            while (hasNext()) {
                next();
                result++;
            }

            return result;
        }

        private boolean isClosed = false;

        /**
         * Closes this iterator and releases any resources associated with it.
         * If the iterator is already closed, this method has no effect.
         * This method calls closeResource() which can be overridden by subclasses
         * to perform specific cleanup operations.
         */
        @Override
        public final void close() {
            if (isClosed) {
                return;
            }

            isClosed = true;
            closeResource();
        }

        @Internal
        protected void closeResource() {

        }

        /**
         * Returns a new iterator that contains only elements matching the specified predicate.
         * Elements that don't satisfy the predicate will be skipped.
         *
         * @param predicate the predicate to test each element
         * @return a new iterator containing only elements that satisfy the predicate
         * @throws IllegalArgumentException if predicate is null
         */
        public Throwables.Iterator<T, E> filter(final Throwables.Predicate<? super T, E> predicate) {
            final Throwables.Iterator<T, E> iter = this;

            return new Throwables.Iterator<>() {
                private final T NONE = (T) N.NULL_MASK; //NOSONAR
                private T next = NONE;
                private T tmp = null;

                @Override
                public boolean hasNext() throws E {
                    if (next == NONE) {
                        while (iter.hasNext()) {
                            tmp = iter.next();

                            if (predicate.test(tmp)) {
                                next = tmp;
                                break;
                            }
                        }
                    }

                    return next != NONE;
                }

                @Override
                public T next() throws E {
                    if (!hasNext()) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    tmp = next;
                    next = NONE;
                    return tmp;
                }
            };
        }

        /**
         * Returns a new iterator that applies the specified mapping function to each element.
         *
         * @param <U> the type of elements returned by the new iterator
         * @param mapper the function to apply to each element
         * @return a new iterator with the mapping function applied to each element
         * @throws IllegalArgumentException if mapper is null
         */
        public <U> Throwables.Iterator<U, E> map(final Throwables.Function<? super T, U, E> mapper) {
            final Throwables.Iterator<T, E> iter = this;

            return new Throwables.Iterator<>() {
                @Override
                public boolean hasNext() throws E {
                    return iter.hasNext();
                }

                @Override
                public U next() throws E {
                    return mapper.apply(iter.next());
                }
            };
        }

        /**
         * Returns the first element from this iterator wrapped in a Nullable.
         * If the iterator is empty, returns an empty Nullable.
         *
         * @return a Nullable containing the first element if present, otherwise an empty Nullable
         * @throws E if an exception occurs while retrieving the first element
         */
        public Nullable<T> first() throws E {
            if (hasNext()) {
                return Nullable.of(next());
            } else {
                return Nullable.empty();
            }
        }

        /**
         * Returns the first non-null element from this iterator wrapped in an Optional.
         * If no non-null element is found or the iterator is empty, returns an empty Optional.
         *
         * @return an Optional containing the first non-null element if present, otherwise an empty Optional
         * @throws E if an exception occurs while searching for a non-null element
         */
        public u.Optional<T> firstNonNull() throws E {
            T next = null;

            while (hasNext()) {
                next = next();

                if (next != null) {
                    return u.Optional.of(next);
                }
            }

            return u.Optional.empty();
        }

        /**
         * Returns the last element from this iterator wrapped in a Nullable.
         * This method will consume all elements in the iterator.
         * If the iterator is empty, returns an empty Nullable.
         *
         * @return a Nullable containing the last element if present, otherwise an empty Nullable
         * @throws E if an exception occurs while iterating through the elements
         */
        public Nullable<T> last() throws E {
            if (hasNext()) {
                T next = next();

                while (hasNext()) {
                    next = next();
                }

                return Nullable.of(next);
            } else {
                return Nullable.empty();
            }
        }

        /**
         * Returns an array containing all remaining elements in this iterator.
         * This method will consume all remaining elements.
         *
         * @return an array containing all remaining elements
         * @throws E if an exception occurs while iterating through the elements
         */
        public Object[] toArray() throws E {
            return toArray(N.EMPTY_OBJECT_ARRAY);
        }

        /**
         * Returns an array containing all remaining elements in this iterator.
         * If the specified array is large enough, the elements are stored in it.
         * Otherwise, a new array of the same type is created.
         * This method will consume all remaining elements.
         *
         * @param <A> the component type of the array
         * @param a the array into which the elements are to be stored, if it is big enough
         * @return an array containing all remaining elements
         * @throws E if an exception occurs while iterating through the elements
         */
        public <A> A[] toArray(final A[] a) throws E {
            return toList().toArray(a);
        }

        /**
         * Returns a List containing all remaining elements in this iterator.
         * This method will consume all remaining elements.
         *
         * @return a List containing all remaining elements
         * @throws E if an exception occurs while iterating through the elements
         */
        public List<T> toList() throws E {
            final List<T> list = new ArrayList<>();

            while (hasNext()) {
                list.add(next());
            }

            return list;
        }

        /**
         * Performs the given action for each remaining element in this iterator.
         * This method will consume all remaining elements.
         *
         * @param action the action to be performed for each element
         * @throws E if an exception occurs while iterating through the elements
         * @throws IllegalArgumentException if action is null
         */
        public void forEachRemaining(final java.util.function.Consumer<? super T> action) throws E { // NOSONAR
            while (hasNext()) {
                action.accept(next());
            }
        }

        /**
         * Performs the given action for each remaining element in this iterator.
         * This method will consume all remaining elements.
         *
         * @param <E2> the type of exception that the action may throw
         * @param action the action to be performed for each element
         * @throws E if an exception occurs while iterating through the elements
         * @throws E2 if the action throws an exception
         * @throws IllegalArgumentException if action is null
         */
        public <E2 extends Throwable> void foreachRemaining(final Throwables.Consumer<? super T, E2> action) throws E, E2 { // NOSONAR
            while (hasNext()) {
                action.accept(next());
            }
        }

        /**
         * Performs the given action for each remaining element in this iterator,
         * providing both the element and its index (starting from 0).
         * This method will consume all remaining elements.
         *
         * @param <E2> the type of exception that the action may throw
         * @param action the action to be performed for each element with its index
         * @throws E if an exception occurs while iterating through the elements
         * @throws E2 if the action throws an exception
         * @throws IllegalArgumentException if action is null
         */
        public <E2 extends Throwable> void foreachIndexed(final Throwables.IntObjConsumer<? super T, E2> action) throws E, E2 {
            int idx = 0;

            while (hasNext()) {
                if (idx < 0) {
                    throw new IllegalStateException("Index overflow: iterator has more than Integer.MAX_VALUE elements");
                }
                action.accept(idx++, next());
            }
        }
    }

    /**
     * The Interface Runnable.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface Runnable<E extends Throwable> {

        /**
         * Executes this runnable operation.
         *
         * @throws E if an exception occurs during execution
         */
        void run() throws E;

        /**
         * Returns a java.util.function.Runnable that wraps this Throwables.Runnable.
         * Any checked exceptions thrown by this runnable will be wrapped in a RuntimeException.
         *
         * @return a java.util.function.Runnable that executes this runnable and wraps any checked exceptions
         */
        @Beta
        default com.landawn.abacus.util.function.Runnable unchecked() {
            return () -> {
                try {
                    run();
                } catch (final Throwable e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            };
        }
    }

    /**
     * The Interface Callable.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface Callable<R, E extends Throwable> {

        /**
         * Computes a result.
         *
         * @return the computed result
         * @throws E if an exception occurs during computation
         */
        R call() throws E;

        /**
         * Returns a java.util.function.Callable that wraps this Throwables.Callable.
         * Any checked exceptions thrown by this callable will be wrapped in a RuntimeException.
         *
         * @return a java.util.function.Callable that executes this callable and wraps any checked exceptions
         */
        @Beta
        default com.landawn.abacus.util.function.Callable<R> unchecked() {
            return () -> {
                try {
                    return call();
                } catch (final Throwable e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            };
        }
    }

    /**
     * The Interface Supplier.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface Supplier<T, E extends Throwable> {

        /**
         * Gets a result.
         *
         * @return the result
         * @throws E if an exception occurs while getting the result
         */
        T get() throws E;

        /**
         * Returns a java.util.function.Supplier that wraps this Throwables.Supplier.
         * Any checked exceptions thrown by this supplier will be wrapped in a RuntimeException.
         *
         * @return a java.util.function.Supplier that executes this supplier and wraps any checked exceptions
         */
        @Beta
        default com.landawn.abacus.util.function.Supplier<T> unchecked() {
            return () -> {
                try {
                    return get();
                } catch (final Throwable e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            };
        }
    }

    /**
     * The Interface BooleanSupplier.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface BooleanSupplier<E extends Throwable> {

        /**
         * Gets a boolean result.
         *
         * @return the boolean result
         * @throws E if an exception occurs while getting the result
         */
        boolean getAsBoolean() throws E; // NOSONAR
    }

    /**
     * The Interface CharSupplier.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface CharSupplier<E extends Throwable> {

        /**
         * Gets a char result.
         *
         * @return the char result
         * @throws E if an exception occurs while getting the result
         */
        char getAsChar() throws E;
    }

    /**
     * The Interface ByteSupplier.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface ByteSupplier<E extends Throwable> {

        /**
         * Gets a byte result.
         *
         * @return the byte result
         * @throws E if an exception occurs while getting the result
         */
        byte getAsByte() throws E;
    }

    /**
     * The Interface ShortSupplier.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface ShortSupplier<E extends Throwable> {

        /**
         * Gets a short result.
         *
         * @return the short result
         * @throws E if an exception occurs while getting the result
         */
        short getAsShort() throws E;
    }

    /**
     * The Interface IntSupplier.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface IntSupplier<E extends Throwable> {

        /**
         * Gets an int result.
         *
         * @return the int result
         * @throws E if an exception occurs while getting the result
         */
        int getAsInt() throws E;
    }

    /**
     * The Interface LongSupplier.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface LongSupplier<E extends Throwable> {

        /**
         * Gets a long result.
         *
         * @return the long result
         * @throws E if an exception occurs while getting the result
         */
        long getAsLong() throws E;
    }

    /**
     * The Interface FloatSupplier.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface FloatSupplier<E extends Throwable> {

        /**
         * Gets a float result.
         *
         * @return the float result
         * @throws E if an exception occurs while getting the result
         */
        float getAsFloat() throws E;
    }

    /**
     * The Interface DoubleSupplier.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface DoubleSupplier<E extends Throwable> {

        /**
         * Gets a double result.
         *
         * @return the double result
         * @throws E if an exception occurs while getting the result
         */
        double getAsDouble() throws E;
    }

    /**
     * The Interface Predicate.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface Predicate<T, E extends Throwable> {

        /**
         * Evaluates this predicate on the given argument.
         *
         * @param t the input argument
         * @return {@code true} if the input argument matches the predicate, otherwise false
         * @throws E if an exception occurs during evaluation
         */
        boolean test(T t) throws E;

        /**
         * Returns a predicate that represents the logical negation of this predicate.
         *
         * @return a predicate that represents the logical negation of this predicate
         */
        default Predicate<T, E> negate() {
            return t -> !test(t);
        }

        /**
         * Returns a java.util.function.Predicate that wraps this Throwables.Predicate.
         * Any checked exceptions thrown by this predicate will be wrapped in a RuntimeException.
         *
         * @return a java.util.function.Predicate that executes this predicate and wraps any checked exceptions
         */
        @Beta
        default com.landawn.abacus.util.function.Predicate<T> unchecked() {
            return t -> {
                try {
                    return test(t);
                } catch (final Throwable e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            };
        }
    }

    /**
     * The Interface BiPredicate.
     *
     * @param <T>
     * @param <U>
     * @param <E>
     */
    @FunctionalInterface
    public interface BiPredicate<T, U, E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @return {@code true} if the input arguments match the predicate, otherwise false
         * @throws E if an exception occurs during evaluation
         */
        boolean test(T t, U u) throws E;

        /**
         * Returns a java.util.function.BiPredicate that wraps this Throwables.BiPredicate.
         * Any checked exceptions thrown by this predicate will be wrapped in a RuntimeException.
         *
         * @return a java.util.function.BiPredicate that executes this predicate and wraps any checked exceptions
         */
        @Beta
        default com.landawn.abacus.util.function.BiPredicate<T, U> unchecked() {
            return (t, u) -> {
                try {
                    return test(t, u);
                } catch (final Throwable e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            };
        }
    }

    /**
     * The Interface TriPredicate.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <E>
     */
    @FunctionalInterface
    public interface TriPredicate<A, B, C, E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @return {@code true} if the input arguments match the predicate, otherwise false
         * @throws E if an exception occurs during evaluation
         */
        boolean test(A a, B b, C c) throws E;
    }

    /**
     * The Interface QuadPredicate.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <D>
     * @param <E>
     */
    @FunctionalInterface
    public interface QuadPredicate<A, B, C, D, E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @param d the fourth input argument
         * @return {@code true} if the input arguments match the predicate, otherwise false
         * @throws E if an exception occurs during evaluation
         */
        boolean test(A a, B b, C c, D d) throws E;
    }

    /**
     * The Interface Function.
     *
     * @param <T>
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface Function<T, R, E extends Throwable> {

        /**
         * Applies this function to the given argument.
         *
         * @param t the function argument
         * @return the function result
         * @throws E if an exception occurs during function application
         */
        R apply(T t) throws E;

        /**
         * Returns a java.util.function.Function that wraps this Throwables.Function.
         * Any checked exceptions thrown by this function will be wrapped in a RuntimeException.
         *
         * @return a java.util.function.Function that executes this function and wraps any checked exceptions
         */
        @Beta
        default com.landawn.abacus.util.function.Function<T, R> unchecked() {
            return t -> {
                try {
                    return apply(t);
                } catch (final Throwable e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            };
        }
    }

    /**
     * The Interface BiFunction.
     *
     * @param <T>
     * @param <U>
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface BiFunction<T, U, R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the first function argument
         * @param u the second function argument
         * @return the function result
         * @throws E if an exception occurs during function application
         */
        R apply(T t, U u) throws E;

        /**
         * Returns a java.util.function.BiFunction that wraps this Throwables.BiFunction.
         * Any checked exceptions thrown by this function will be wrapped in a RuntimeException.
         *
         * @return a java.util.function.BiFunction that executes this function and wraps any checked exceptions
         */
        @Beta
        default com.landawn.abacus.util.function.BiFunction<T, U, R> unchecked() {
            return (t, u) -> {
                try {
                    return apply(t, u);
                } catch (final Throwable e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            };
        }
    }

    /**
     * The Interface TriFunction.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface TriFunction<A, B, C, R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the function result
         * @throws E if an exception occurs during function application
         */
        R apply(A a, B b, C c) throws E;
    }

    /**
     * The Interface QuadFunction.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <D>
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface QuadFunction<A, B, C, D, R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @param d the fourth function argument
         * @return the function result
         * @throws E if an exception occurs during function application
         */
        R apply(A a, B b, C c, D d) throws E;
    }

    /**
     * The Interface Consumer.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface Consumer<T, E extends Throwable> {

        /**
         * Performs this operation on the given argument.
         *
         * @param t the input argument
         * @throws E if an exception occurs during the operation
         */
        void accept(T t) throws E;

        /**
         * Returns a java.util.function.Consumer that wraps this Throwables.Consumer.
         * Any checked exceptions thrown by this consumer will be wrapped in a RuntimeException.
         *
         * @return a java.util.function.Consumer that executes this consumer and wraps any checked exceptions
         */
        @Beta
        default com.landawn.abacus.util.function.Consumer<T> unchecked() {
            return t -> {
                try {
                    accept(t);
                } catch (final Throwable e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            };
        }
    }

    /**
     * The Interface BiConsumer.
     *
     * @param <T>
     * @param <U>
     * @param <E>
     */
    @FunctionalInterface
    public interface BiConsumer<T, U, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @throws E if an exception occurs during the operation
         */
        void accept(T t, U u) throws E;

        /**
         * Returns a java.util.function.BiConsumer that wraps this Throwables.BiConsumer.
         * Any checked exceptions thrown by this consumer will be wrapped in a RuntimeException.
         *
         * @return a java.util.function.BiConsumer that executes this consumer and wraps any checked exceptions
         */
        @Beta
        default com.landawn.abacus.util.function.BiConsumer<T, U> unchecked() {
            return (t, u) -> {
                try {
                    accept(t, u);
                } catch (final Throwable e) {
                    throw ExceptionUtil.toRuntimeException(e, true);
                }
            };
        }
    }

    /**
     * The Interface TriConsumer.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <E>
     */
    @FunctionalInterface
    public interface TriConsumer<A, B, C, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @throws E if an exception occurs during the operation
         */
        void accept(A a, B b, C c) throws E;
    }

    /**
     * The Interface QuadConsumer.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <D>
     * @param <E>
     */
    @FunctionalInterface
    public interface QuadConsumer<A, B, C, D, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @param d the fourth input argument
         * @throws E if an exception occurs during the operation
         */
        void accept(A a, B b, C c, D d) throws E;
    }

    /**
     * The Interface BooleanConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface BooleanConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given boolean argument.
         *
         * @param t the boolean input argument
         * @throws E if an exception occurs during the operation
         */
        void accept(boolean t) throws E;
    }

    /**
     * The Interface BooleanPredicate.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface BooleanPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given boolean argument.
         *
         * @param value the boolean input argument
         * @return {@code true} if the input argument matches the predicate, otherwise false
         * @throws E if an exception occurs during evaluation
         */
        boolean test(boolean value) throws E;
    }

    /**
     * The Interface BooleanFunction.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface BooleanFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given boolean argument.
         *
         * @param value the boolean function argument
         * @return the function result
         * @throws E if an exception occurs during function application
         */
        R apply(boolean value) throws E;
    }

    /**
     * The Interface CharConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface CharConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given char argument.
         *
         * @param t the char input argument
         * @throws E if an exception occurs during the operation
         */
        void accept(char t) throws E;
    }

    /**
     * The Interface CharPredicate.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface CharPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given char argument.
         *
         * @param value the char input argument
         * @return {@code true} if the input argument matches the predicate, otherwise false
         * @throws E if an exception occurs during evaluation
         */
        boolean test(char value) throws E;
    }

    /**
     * The Interface CharFunction.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface CharFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given char argument.
         *
         * @param value the char function argument
         * @return the function result
         * @throws E if an exception occurs during function application
         */
        R apply(char value) throws E;
    }

    /**
     * The Interface ByteConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface ByteConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given byte argument.
         *
         * @param t the byte input argument
         * @throws E if an exception occurs during the operation
         */
        void accept(byte t) throws E;
    }

    /**
     * The Interface BytePredicate.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface BytePredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given byte argument.
         *
         * @param value the byte input argument
         * @return {@code true} if the input argument matches the predicate, otherwise false
         * @throws E if an exception occurs during evaluation
         */
        boolean test(byte value) throws E;
    }

    /**
     * The Interface ByteFunction.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface ByteFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given byte argument.
         *
         * @param value the byte function argument
         * @return the function result
         * @throws E if an exception occurs during function application
         */
        R apply(byte value) throws E;
    }

    /**
     * The Interface ShortConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface ShortConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given short argument.
         *
         * @param t the short input argument
         * @throws E if an exception occurs during the operation
         */
        void accept(short t) throws E;
    }

    /**
     * The Interface ShortPredicate.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface ShortPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given short argument.
         *
         * @param value the short input argument
         * @return {@code true} if the input argument matches the predicate, otherwise false
         * @throws E if an exception occurs during evaluation
         */
        boolean test(short value) throws E;
    }

    /**
     * The Interface ShortFunction.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface ShortFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given short argument.
         *
         * @param value the short function argument
         * @return the function result
         * @throws E if an exception occurs during function application
         */
        R apply(short value) throws E;
    }

    /**
     * The Interface IntConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface IntConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given int argument.
         *
         * @param t the int input argument
         * @throws E if an exception occurs during the operation
         */
        void accept(int t) throws E;
    }

    /**
     * The Interface IntPredicate.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface IntPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given int argument.
         *
         * @param value the int input argument
         * @return {@code true} if the input argument matches the predicate, otherwise false
         * @throws E if an exception occurs during evaluation
         */
        boolean test(int value) throws E;
    }

    /**
     * The Interface IntFunction.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface IntFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given int argument.
         *
         * @param value the int function argument
         * @return the function result
         * @throws E if an exception occurs during function application
         */
        R apply(int value) throws E;
    }

    /**
     * The Interface IntToLongFunction.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface IntToLongFunction<E extends Throwable> {

        /**
         * Applies this function to the given int argument and produces a long result.
         *
         * @param value the int function argument
         * @return the long function result
         * @throws E if an exception occurs during function application
         */
        long applyAsLong(int value) throws E;
    }

    /**
     * The Interface IntToDoubleFunction.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface IntToDoubleFunction<E extends Throwable> {

        /**
         * Applies this function to the given int argument and produces a double result.
         *
         * @param value the int function argument
         * @return the double function result
         * @throws E if an exception occurs during function application
         */
        double applyAsDouble(int value) throws E;
    }

    /**
     * The Interface LongConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface LongConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given long argument.
         *
         * @param t the long input argument
         * @throws E if an exception occurs during the operation
         */
        void accept(long t) throws E;
    }

    /**
     * The Interface LongPredicate.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface LongPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given long argument.
         *
         * @param value the long input argument
         * @return {@code true} if the input argument matches the predicate, otherwise false
         * @throws E if an exception occurs during evaluation
         */
        boolean test(long value) throws E;
    }

    /**
     * The Interface LongFunction.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface LongFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given long argument.
         *
         * @param value the long function argument
         * @return the function result
         * @throws E if an exception occurs during function application
         */
        R apply(long value) throws E;
    }

    /**
     * The Interface LongToIntFunction.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface LongToIntFunction<E extends Throwable> {

        /**
         * Applies this function to the given long argument and produces an int result.
         *
         * @param value the long function argument
         * @return the int function result
         * @throws E if an exception occurs during function application
         */
        int applyAsInt(long value) throws E;
    }

    /**
     * The Interface LongToDoubleFunction.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface LongToDoubleFunction<E extends Throwable> {

        /**
         * Applies this function to the given long argument and produces a double result.
         *
         * @param value the long function argument
         * @return the double function result
         * @throws E if an exception occurs during function application
         */
        double applyAsDouble(long value) throws E;
    }

    /**
     * The Interface FloatConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface FloatConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given float argument.
         *
         * @param t the float input argument
         * @throws E if an exception occurs during the operation
         */
        void accept(float t) throws E;
    }

    /**
     * The Interface FloatPredicate.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface FloatPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given float argument.
         *
         * @param value the float input argument
         * @return {@code true} if the input argument matches the predicate, otherwise false
         * @throws E if an exception occurs during evaluation
         */
        boolean test(float value) throws E;
    }

    /**
     * The Interface FloatFunction.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface FloatFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given float argument.
         *
         * @param value the float function argument
         * @return the function result
         * @throws E if an exception occurs during function application
         */
        R apply(float value) throws E;
    }

    /**
     * The Interface DoubleConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface DoubleConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given double argument.
         *
         * @param t the double input argument
         * @throws E if an exception occurs during the operation
         */
        void accept(double t) throws E;
    }

    /**
     * The Interface DoublePredicate.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface DoublePredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given double argument.
         *
         * @param value the double input argument
         * @return {@code true} if the input argument matches the predicate, otherwise false
         * @throws E if an exception occurs during evaluation
         */
        boolean test(double value) throws E;
    }

    /**
     * Represents a function that accepts a double-valued argument and produces a result.
     * This is the double-consuming primitive specialization for {@code Function}.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface DoubleFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given double-valued argument.
         *
         * @param value the double value to be processed
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(double value) throws E;
    }

    /**
     * Represents a function that accepts a double-valued argument and produces an int-valued result.
     * This is the double-to-int primitive specialization for {@code Function}.
     *
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface DoubleToIntFunction<E extends Throwable> {

        /**
         * Applies this function to the given double value and returns an int result.
         *
         * @param value the double value to be processed
         * @return the int result
         * @throws E if an error occurs during function execution
         */
        int applyAsInt(double value) throws E;
    }

    /**
     * Represents a function that accepts a double-valued argument and produces a long-valued result.
     * This is the double-to-long primitive specialization for {@code Function}.
     *
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface DoubleToLongFunction<E extends Throwable> {

        /**
         * Applies this function to the given double value and returns a long result.
         *
         * @param value the double value to be processed
         * @return the long result
         * @throws E if an error occurs during function execution
         */
        long applyAsLong(double value) throws E;
    }

    /**
     * Represents a function that produces a boolean-valued result.
     * This is the boolean-producing primitive specialization for {@code Function}.
     *
     * @param <T> the type of the input to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToBooleanFunction<T, E extends Throwable> {

        /**
         * Applies this function to the given argument and returns a boolean result.
         *
         * @param t the input argument
         * @return the boolean result
         * @throws E if an error occurs during function execution
         */
        boolean applyAsBoolean(T t) throws E;
    }

    /**
     * Represents a function that produces a char-valued result.
     * This is the char-producing primitive specialization for {@code Function}.
     *
     * @param <T> the type of the input to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToCharFunction<T, E extends Throwable> {

        /**
         * Applies this function to the given argument and returns a char result.
         *
         * @param t the input argument
         * @return the char result
         * @throws E if an error occurs during function execution
         */
        char applyAsChar(T t) throws E;
    }

    /**
     * Represents a function that produces a byte-valued result.
     * This is the byte-producing primitive specialization for {@code Function}.
     *
     * @param <T> the type of the input to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToByteFunction<T, E extends Throwable> {

        /**
         * Applies this function to the given argument and returns a byte result.
         *
         * @param t the input argument
         * @return the byte result
         * @throws E if an error occurs during function execution
         */
        byte applyAsByte(T t) throws E;
    }

    /**
     * Represents a function that produces a short-valued result.
     * This is the short-producing primitive specialization for {@code Function}.
     *
     * @param <T> the type of the input to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToShortFunction<T, E extends Throwable> {

        /**
         * Applies this function to the given argument and returns a short result.
         *
         * @param t the input argument
         * @return the short result
         * @throws E if an error occurs during function execution
         */
        short applyAsShort(T t) throws E;
    }

    /**
     * Represents a function that produces an int-valued result.
     * This is the int-producing primitive specialization for {@code Function}.
     *
     * @param <T> the type of the input to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToIntFunction<T, E extends Throwable> {

        /**
         * Applies this function to the given argument and returns an int result.
         *
         * @param t the input argument
         * @return the int result
         * @throws E if an error occurs during function execution
         */
        int applyAsInt(T t) throws E;
    }

    /**
     * Represents a function that produces a long-valued result.
     * This is the long-producing primitive specialization for {@code Function}.
     *
     * @param <T> the type of the input to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToLongFunction<T, E extends Throwable> {

        /**
         * Applies this function to the given argument and returns a long result.
         *
         * @param t the input argument
         * @return the long result
         * @throws E if an error occurs during function execution
         */
        long applyAsLong(T t) throws E;
    }

    /**
     * Represents a function that produces a float-valued result.
     * This is the float-producing primitive specialization for {@code Function}.
     *
     * @param <T> the type of the input to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToFloatFunction<T, E extends Throwable> {

        /**
         * Applies this function to the given argument and returns a float result.
         *
         * @param t the input argument
         * @return the float result
         * @throws E if an error occurs during function execution
         */
        float applyAsFloat(T t) throws E;
    }

    /**
     * Represents a function that produces a double-valued result.
     * This is the double-producing primitive specialization for {@code Function}.
     *
     * @param <T> the type of the input to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToDoubleFunction<T, E extends Throwable> {

        /**
         * Applies this function to the given argument and returns a double result.
         *
         * @param t the input argument
         * @return the double result
         * @throws E if an error occurs during function execution
         */
        double applyAsDouble(T t) throws E;
    }

    /**
     * Represents a function that accepts two arguments and produces an int-valued result.
     * This is the int-producing primitive specialization for {@code BiFunction}.
     *
     * @param <A> the type of the first argument to the function
     * @param <B> the type of the second argument to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToIntBiFunction<A, B, E extends Throwable> {

        /**
         * Applies this function to the given arguments and returns an int result.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @return the int result
         * @throws E if an error occurs during function execution
         */
        int applyAsInt(A a, B b) throws E;
    }

    /**
     * Represents a function that accepts two arguments and produces a long-valued result.
     * This is the long-producing primitive specialization for {@code BiFunction}.
     *
     * @param <A> the type of the first argument to the function
     * @param <B> the type of the second argument to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToLongBiFunction<A, B, E extends Throwable> {

        /**
         * Applies this function to the given arguments and returns a long result.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @return the long result
         * @throws E if an error occurs during function execution
         */
        long applyAsLong(A a, B b) throws E;
    }

    /**
     * Represents a function that accepts two arguments and produces a double-valued result.
     * This is the double-producing primitive specialization for {@code BiFunction}.
     *
     * @param <A> the type of the first argument to the function
     * @param <B> the type of the second argument to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToDoubleBiFunction<A, B, E extends Throwable> {

        /**
         * Applies this function to the given arguments and returns a double result.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @return the double result
         * @throws E if an error occurs during function execution
         */
        double applyAsDouble(A a, B b) throws E;
    }

    /**
     * Represents a function that accepts three arguments and produces an int-valued result.
     * This is the int-producing primitive specialization for TriFunction.
     *
     * @param <A> the type of the first argument to the function
     * @param <B> the type of the second argument to the function
     * @param <C> the type of the third argument to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToIntTriFunction<A, B, C, E extends Throwable> {

        /**
         * Applies this function to the given arguments and returns an int result.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the int result
         * @throws E if an error occurs during function execution
         */
        int applyAsInt(A a, B b, C c) throws E;
    }

    /**
     * Represents a function that accepts three arguments and produces a long-valued result.
     * This is the long-producing primitive specialization for TriFunction.
     *
     * @param <A> the type of the first argument to the function
     * @param <B> the type of the second argument to the function
     * @param <C> the type of the third argument to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToLongTriFunction<A, B, C, E extends Throwable> {

        /**
         * Applies this function to the given arguments and returns a long result.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the long result
         * @throws E if an error occurs during function execution
         */
        long applyAsLong(A a, B b, C c) throws E;
    }

    /**
     * Represents a function that accepts three arguments and produces a double-valued result.
     * This is the double-producing primitive specialization for TriFunction.
     *
     * @param <A> the type of the first argument to the function
     * @param <B> the type of the second argument to the function
     * @param <C> the type of the third argument to the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ToDoubleTriFunction<A, B, C, E extends Throwable> {

        /**
         * Applies this function to the given arguments and returns a double result.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the double result
         * @throws E if an error occurs during function execution
         */
        double applyAsDouble(A a, B b, C c) throws E;
    }

    /**
     * Represents an operation on a single operand that produces a result of the same type as its operand.
     * This is a specialization of {@code Function} for the case where the operand and result are of the same type.
     *
     * @param <T> the type of the operand and result of the operator
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface UnaryOperator<T, E extends Throwable> extends Function<T, T, E> {
    }

    /**
     * Represents an operation upon two operands of the same type, producing a result of the same type as the operands.
     * This is a specialization of {@code BiFunction} for the case where the operands and the result are all of the same type.
     *
     * @param <T> the type of the operands and result of the operator
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface BinaryOperator<T, E extends Throwable> extends BiFunction<T, T, T, E> {
    }

    /**
     * Represents an operation upon three operands of the same type, producing a result of the same type as the operands.
     *
     * @param <T> the type of the operands and result of the operator
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface TernaryOperator<T, E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param a the first operand
         * @param b the second operand
         * @param c the third operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        T apply(T a, T b, T c) throws E;
    }

    /**
     * Represents an operation on a single boolean-valued operand that produces a boolean-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface BooleanUnaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operand.
         *
         * @param operand the operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        boolean applyAsBoolean(boolean operand) throws E;
    }

    /**
     * Represents an operation on a single char-valued operand that produces a char-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface CharUnaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operand.
         *
         * @param operand the operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        char applyAsChar(char operand) throws E;
    }

    /**
     * Represents an operation on a single byte-valued operand that produces a byte-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface ByteUnaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operand.
         *
         * @param operand the operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        byte applyAsByte(byte operand) throws E;
    }

    /**
     * Represents an operation on a single short-valued operand that produces a short-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface ShortUnaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operand.
         *
         * @param operand the operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        short applyAsShort(short operand) throws E;
    }

    /**
     * Represents an operation on a single int-valued operand that produces an int-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface IntUnaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operand.
         *
         * @param operand the operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        int applyAsInt(int operand) throws E;
    }

    /**
     * Represents an operation that accepts an int-valued operand and an object operand, and produces an int-valued result.
     *
     * @param <T> the type of the object operand
     * @param <E> the type of exception that the operator may throw
     */
    @Beta
    @FunctionalInterface
    public interface IntObjOperator<T, E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param operand the int operand
         * @param obj the object operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        int applyAsInt(int operand, T obj) throws E;
    }

    /**
     * Represents an operation on a single long-valued operand that produces a long-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface LongUnaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operand.
         *
         * @param operand the operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        long applyAsLong(long operand) throws E;
    }

    /**
     * Represents an operation on a single float-valued operand that produces a float-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface FloatUnaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operand.
         *
         * @param operand the operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        float applyAsFloat(float operand) throws E;
    }

    /**
     * Represents an operation on a single double-valued operand that produces a double-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface DoubleUnaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operand.
         *
         * @param operand the operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        double applyAsDouble(double operand) throws E;
    }

    /**
     * Represents an operation upon two boolean-valued operands and producing a boolean-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface BooleanBinaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param left the first operand
         * @param right the second operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        boolean applyAsBoolean(boolean left, boolean right) throws E;
    }

    /**
     * Represents an operation upon two char-valued operands and producing a char-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface CharBinaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param left the first operand
         * @param right the second operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        char applyAsChar(char left, char right) throws E;
    }

    /**
     * Represents an operation upon two byte-valued operands and producing a byte-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface ByteBinaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param left the first operand
         * @param right the second operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        byte applyAsByte(byte left, byte right) throws E;
    }

    /**
     * Represents an operation upon two short-valued operands and producing a short-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface ShortBinaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param left the first operand
         * @param right the second operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        short applyAsShort(short left, short right) throws E;
    }

    /**
     * Represents an operation upon two int-valued operands and producing an int-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface IntBinaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param left the first operand
         * @param right the second operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        int applyAsInt(int left, int right) throws E;
    }

    /**
     * Represents an operation upon two long-valued operands and producing a long-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface LongBinaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param left the first operand
         * @param right the second operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        long applyAsLong(long left, long right) throws E;
    }

    /**
     * Represents an operation upon two float-valued operands and producing a float-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface FloatBinaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param left the first operand
         * @param right the second operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        float applyAsFloat(float left, float right) throws E;
    }

    /**
     * Represents an operation upon two double-valued operands and producing a double-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface DoubleBinaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param left the first operand
         * @param right the second operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        double applyAsDouble(double left, double right) throws E;
    }

    /**
     * Represents an operation upon three boolean-valued operands and producing a boolean-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface BooleanTernaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param a the first operand
         * @param b the second operand
         * @param c the third operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        boolean applyAsBoolean(boolean a, boolean b, boolean c) throws E;
    }

    /**
     * Represents an operation upon three char-valued operands and producing a char-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface CharTernaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param a the first operand
         * @param b the second operand
         * @param c the third operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        char applyAsChar(char a, char b, char c) throws E;
    }

    /**
     * Represents an operation upon three byte-valued operands and producing a byte-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface ByteTernaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param a the first operand
         * @param b the second operand
         * @param c the third operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        byte applyAsByte(byte a, byte b, byte c) throws E;
    }

    /**
     * Represents an operation upon three short-valued operands and producing a short-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface ShortTernaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param a the first operand
         * @param b the second operand
         * @param c the third operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        short applyAsShort(short a, short b, short c) throws E;
    }

    /**
     * Represents an operation upon three int-valued operands and producing an int-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface IntTernaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param a the first operand
         * @param b the second operand
         * @param c the third operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        int applyAsInt(int a, int b, int c) throws E;
    }

    /**
     * Represents an operation upon three long-valued operands and producing a long-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface LongTernaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param a the first operand
         * @param b the second operand
         * @param c the third operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        long applyAsLong(long a, long b, long c) throws E;
    }

    /**
     * Represents an operation upon three float-valued operands and producing a float-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface FloatTernaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param a the first operand
         * @param b the second operand
         * @param c the third operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        float applyAsFloat(float a, float b, float c) throws E;
    }

    /**
     * Represents an operation upon three double-valued operands and producing a double-valued result.
     *
     * @param <E> the type of exception that the operator may throw
     */
    @FunctionalInterface
    public interface DoubleTernaryOperator<E extends Throwable> {

        /**
         * Applies this operator to the given operands.
         *
         * @param a the first operand
         * @param b the second operand
         * @param c the third operand
         * @return the operator result
         * @throws E if an error occurs during operator execution
         */
        double applyAsDouble(double a, double b, double c) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of two boolean-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface BooleanBiPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(boolean t, boolean u) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of two char-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface CharBiPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(char t, char u) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of two byte-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface ByteBiPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(byte t, byte u) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of two short-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface ShortBiPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(short t, short u) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of two int-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface IntBiPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(int t, int u) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of two long-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface LongBiPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(long t, long u) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of two float-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface FloatBiPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(float t, float u) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of two double-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface DoubleBiPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(double t, double u) throws E;
    }

    /**
     * Represents a function that accepts two boolean-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface BooleanBiFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the first function argument
         * @param u the second function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(boolean t, boolean u) throws E;
    }

    /**
     * Represents a function that accepts two char-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface CharBiFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the first function argument
         * @param u the second function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(char t, char u) throws E;
    }

    /**
     * Represents a function that accepts two byte-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ByteBiFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the first function argument
         * @param u the second function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(byte t, byte u) throws E;
    }

    /**
     * Represents a function that accepts two short-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ShortBiFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the first function argument
         * @param u the second function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(short t, short u) throws E;
    }

    /**
     * Represents a function that accepts two int-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface IntBiFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the first function argument
         * @param u the second function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(int t, int u) throws E;
    }

    /**
     * Represents a function that accepts two long-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface LongBiFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the first function argument
         * @param u the second function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(long t, long u) throws E;
    }

    /**
     * Represents a function that accepts two float-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface FloatBiFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the first function argument
         * @param u the second function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(float t, float u) throws E;
    }

    /**
     * Represents a function that accepts two double-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface DoubleBiFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the first function argument
         * @param u the second function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(double t, double u) throws E;
    }

    /**
     * Represents an operation that accepts two boolean-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface BooleanBiConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(boolean t, boolean u) throws E;
    }

    /**
     * Represents an operation that accepts two char-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface CharBiConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(char t, char u) throws E;
    }

    /**
     * Represents an operation that accepts two byte-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ByteBiConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(byte t, byte u) throws E;
    }

    /**
     * Represents an operation that accepts two short-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ShortBiConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(short t, short u) throws E;
    }

    /**
     * Represents an operation that accepts two int-valued arguments and returns no result.
     * This is the int-specialized primitive type specialization of {@code BiConsumer}.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface IntBiConsumer<E extends Throwable> extends IntIntConsumer<E> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @throws E if an error occurs during operation execution
         */
        @Override
        void accept(int t, int u) throws E;
    }

    /**
     * Represents an operation that accepts two long-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface LongBiConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(long t, long u) throws E;
    }

    /**
     * Represents an operation that accepts two float-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface FloatBiConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(float t, float u) throws E;
    }

    /**
     * Represents an operation that accepts two double-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface DoubleBiConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the first input argument
         * @param u the second input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(double t, double u) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of three boolean-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface BooleanTriPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(boolean a, boolean b, boolean c) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of three char-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface CharTriPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(char a, char b, char c) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of three byte-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface ByteTriPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(byte a, byte b, byte c) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of three short-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface ShortTriPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(short a, short b, short c) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of three int-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface IntTriPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(int a, int b, int c) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of three long-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface LongTriPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(long a, long b, long c) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of three float-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface FloatTriPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(float a, float b, float c) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of three double-valued arguments.
     *
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface DoubleTriPredicate<E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(double a, double b, double c) throws E;
    }

    /**
     * Represents a function that accepts three boolean-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface BooleanTriFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(boolean a, boolean b, boolean c) throws E;
    }

    /**
     * Represents a function that accepts three char-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface CharTriFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(char a, char b, char c) throws E;
    }

    /**
     * Represents a function that accepts three byte-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ByteTriFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(byte a, byte b, byte c) throws E;
    }

    /**
     * Represents a function that accepts three short-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ShortTriFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(short a, short b, short c) throws E;
    }

    /**
     * Represents a function that accepts three int-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface IntTriFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(int a, int b, int c) throws E;
    }

    /**
     * Represents a function that accepts three long-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface LongTriFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(long a, long b, long c) throws E;
    }

    /**
     * Represents a function that accepts three float-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface FloatTriFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(float a, float b, float c) throws E;
    }

    /**
     * Represents a function that accepts three double-valued arguments and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface DoubleTriFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param a the first function argument
         * @param b the second function argument
         * @param c the third function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(double a, double b, double c) throws E;
    }

    /**
     * Represents an operation that accepts three boolean-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface BooleanTriConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(boolean a, boolean b, boolean c) throws E;
    }

    /**
     * Represents an operation that accepts three char-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface CharTriConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(char a, char b, char c) throws E;
    }

    /**
     * Represents an operation that accepts three byte-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ByteTriConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(byte a, byte b, byte c) throws E;
    }

    /**
     * Represents an operation that accepts three short-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ShortTriConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(short a, short b, short c) throws E;
    }

    /**
     * Represents an operation that accepts three int-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface IntTriConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(int a, int b, int c) throws E;
    }

    /**
     * Represents an operation that accepts three long-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface LongTriConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(long a, long b, long c) throws E;
    }

    /**
     * Represents an operation that accepts three float-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface FloatTriConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(float a, float b, float c) throws E;
    }

    /**
     * Represents an operation that accepts three double-valued arguments and returns no result.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface DoubleTriConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param a the first input argument
         * @param b the second input argument
         * @param c the third input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(double a, double b, double c) throws E;
    }

    /**
     * Represents an operation that accepts an object and a boolean value and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ObjBooleanConsumer<T, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the object input argument
         * @param value the boolean input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(T t, boolean value) throws E;
    }

    /**
     * Represents an operation that accepts an object and a char value and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ObjCharConsumer<T, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the object input argument
         * @param value the char input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(T t, char value) throws E;
    }

    /**
     * Represents an operation that accepts an object and a byte value and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ObjByteConsumer<T, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the object input argument
         * @param value the byte input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(T t, byte value) throws E;
    }

    /**
     * Represents an operation that accepts an object and a short value and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ObjShortConsumer<T, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the object input argument
         * @param value the short input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(T t, short value) throws E;
    }

    /**
     * Represents an operation that accepts an object and an int value and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ObjIntConsumer<T, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the object input argument
         * @param u the int input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(T t, int u) throws E;
    }

    /**
     * Represents a function that accepts an object and an int value and produces a result.
     *
     * @param <T> the type of the object argument to the function
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ObjIntFunction<T, R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the object function argument
         * @param u the int function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(T t, int u) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of an object and an int value.
     *
     * @param <T> the type of the object argument to the predicate
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface ObjIntPredicate<T, E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the object input argument
         * @param u the int input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(T t, int u) throws E;
    }

    /**
     * Represents an operation that accepts an object and a long value and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ObjLongConsumer<T, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the object input argument
         * @param u the long input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(T t, long u) throws E;
    }

    /**
     * Represents a function that accepts an object and a long value and produces a result.
     *
     * @param <T> the type of the object argument to the function
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ObjLongFunction<T, R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the object function argument
         * @param u the long function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(T t, long u) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of an object and a long value.
     *
     * @param <T> the type of the object argument to the predicate
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface ObjLongPredicate<T, E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the object input argument
         * @param u the long input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(T t, long u) throws E;
    }

    /**
     * Represents an operation that accepts an object and a float value and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ObjFloatConsumer<T, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the object input argument
         * @param value the float input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(T t, float value) throws E;
    }

    /**
     * Represents an operation that accepts an object and a double value and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ObjDoubleConsumer<T, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the object input argument
         * @param u the double input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(T t, double u) throws E;
    }

    /**
     * Represents a function that accepts an object and a double value and produces a result.
     *
     * @param <T> the type of the object argument to the function
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ObjDoubleFunction<T, R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the object function argument
         * @param u the double function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(T t, double u) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of an object and a double value.
     *
     * @param <T> the type of the object argument to the predicate
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface ObjDoublePredicate<T, E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the object input argument
         * @param u the double input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(T t, double u) throws E;
    }

    /**
     * Represents an operation that accepts an object and two int values and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface ObjBiIntConsumer<T, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the object input argument
         * @param i the first int input argument
         * @param j the second int input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(T t, int i, int j) throws E;
    }

    /**
     * Represents a function that accepts an object and two int values and produces a result.
     *
     * @param <T> the type of the object argument to the function
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ObjBiIntFunction<T, R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param t the object function argument
         * @param i the first int function argument
         * @param j the second int function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(T t, int i, int j) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of an object and two int values.
     *
     * @param <T> the type of the object argument to the predicate
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface ObjBiIntPredicate<T, E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the object input argument
         * @param i the first int input argument
         * @param j the second int input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(T t, int i, int j) throws E;
    }

    /**
     * Represents an operation that accepts two objects and an int value and returns no result.
     * This is the indexed variant of BiConsumer.
     *
     * @param <T> the type of the first object argument to the operation
     * @param <U> the type of the second object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface BiObjIntConsumer<T, U, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param t the first object input argument
         * @param u the second object input argument
         * @param i the int index argument
         * @throws E if an error occurs during operation execution
         */
        void accept(T t, U u, int i) throws E;
    }

    /**
     * Represents a function that accepts two objects and an int value and produces a result.
     * This is the indexed variant of BiFunction.
     *
     * @param <T> the type of the first object argument to the function
     * @param <U> the type of the second object argument to the function
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface BiObjIntFunction<T, U, R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param e the first object function argument
         * @param u the second object function argument
         * @param i the int index argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(T e, U u, int i) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of two objects and an int value.
     * This is the indexed variant of BiPredicate.
     *
     * @param <T> the type of the first object argument to the predicate
     * @param <U> the type of the second object argument to the predicate
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface BiObjIntPredicate<T, U, E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param t the first object input argument
         * @param u the second object input argument
         * @param i the int index argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(T t, U u, int i) throws E;
    }

    /**
     * Represents an operation that accepts an int value and an object and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface IntObjConsumer<T, E extends Throwable> {

        /**
         * Returns an IntObjConsumer that performs the given operation.
         *
         * @param <T> the type of the object argument to the consumer
         * @param <E> the type of exception that the consumer may throw
         * @param consumer the operation to be performed
         * @return the input consumer
         */
        static <T, E extends Throwable> IntObjConsumer<T, E> of(final IntObjConsumer<T, E> consumer) {
            return consumer;
        }

        /**
         * Performs this operation on the given arguments.
         *
         * @param i the int input argument
         * @param t the object input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(int i, T t) throws E;
    }

    /**
     * Represents a function that accepts an int value and an object and produces a result.
     *
     * @param <T> the type of the object argument to the function
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface IntObjFunction<T, R, E extends Throwable> {

        /**
         * Returns an IntObjFunction that performs the given function.
         *
         * @param <T> the type of the object argument to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of exception that the function may throw
         * @param func the function to be performed
         * @return the input function
         */
        static <T, R, E extends Throwable> IntObjFunction<T, R, E> of(final IntObjFunction<T, R, E> func) {
            return func;
        }

        /**
         * Applies this function to the given arguments.
         *
         * @param i the int function argument
         * @param t the object function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(int i, T t) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of an int value and an object.
     *
     * @param <T> the type of the object argument to the predicate
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface IntObjPredicate<T, E extends Throwable> {

        /**
         * Returns an IntObjPredicate that performs the given test.
         *
         * @param <T> the type of the object argument to the predicate
         * @param <E> the type of exception that the predicate may throw
         * @param predicate the predicate to be performed
         * @return the input predicate
         */
        static <T, E extends Throwable> IntObjPredicate<T, E> of(final IntObjPredicate<T, E> predicate) {
            return predicate;
        }

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param i the int input argument
         * @param t the object input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(int i, T t) throws E;
    }

    /**
     * Represents an operation that accepts an int value and two objects and returns no result.
     *
     * @param <T> the type of the first object argument to the operation
     * @param <U> the type of the second object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface IntBiObjConsumer<T, U, E extends Throwable> {
        /**
         * Performs this operation on the given arguments.
         *
         * @param i the int input argument
         * @param t the first object input argument
         * @param u the second object input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(int i, T t, U u) throws E;
    }

    /**
     * Represents a function that accepts an int value and two objects and produces a result.
     *
     * @param <T> the type of the first object argument to the function
     * @param <U> the type of the second object argument to the function
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface IntBiObjFunction<T, U, R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param i the int function argument
         * @param t the first object function argument
         * @param u the second object function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(int i, T t, U u) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of an int value and two objects.
     *
     * @param <T> the type of the first object argument to the predicate
     * @param <U> the type of the second object argument to the predicate
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface IntBiObjPredicate<T, U, E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param i the int input argument
         * @param t the first object input argument
         * @param u the second object input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(int i, T t, U u) throws E;
    }

    /**
     * Represents an operation that accepts two int values and an object and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface BiIntObjConsumer<T, E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param i the first int input argument
         * @param j the second int input argument
         * @param t the object input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(int i, int j, T t) throws E;
    }

    /**
     * Represents a function that accepts two int values and an object and produces a result.
     *
     * @param <T> the type of the object argument to the function
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface BiIntObjFunction<T, R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param i the first int function argument
         * @param j the second int function argument
         * @param t the object function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(int i, int j, T t) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of two int values and an object.
     *
     * @param <T> the type of the object argument to the predicate
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface BiIntObjPredicate<T, E extends Throwable> {

        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param i the first int input argument
         * @param j the second int input argument
         * @param t the object input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(int i, int j, T t) throws E;
    }

    /**
     * Represents an operation that accepts a long value and an object and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface LongObjConsumer<T, E extends Throwable> {
        /**
         * Performs this operation on the given arguments.
         *
         * @param i the long input argument
         * @param t the object input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(long i, T t) throws E;
    }

    /**
     * Represents a function that accepts a long value and an object and produces a result.
     *
     * @param <T> the type of the object argument to the function
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface LongObjFunction<T, R, E extends Throwable> {
        /**
         * Applies this function to the given arguments.
         *
         * @param i the long function argument
         * @param t the object function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(long i, T t) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of a long value and an object.
     *
     * @param <T> the type of the object argument to the predicate
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface LongObjPredicate<T, E extends Throwable> {
        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param i the long input argument
         * @param t the object input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(long i, T t) throws E;
    }

    /**
     * Represents an operation that accepts a double value and an object and returns no result.
     *
     * @param <T> the type of the object argument to the operation
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface DoubleObjConsumer<T, E extends Throwable> {
        /**
         * Performs this operation on the given arguments.
         *
         * @param i the double input argument
         * @param t the object input argument
         * @throws E if an error occurs during operation execution
         */
        void accept(double i, T t) throws E;
    }

    /**
     * Represents a function that accepts a double value and an object and produces a result.
     *
     * @param <T> the type of the object argument to the function
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface DoubleObjFunction<T, R, E extends Throwable> {
        /**
         * Applies this function to the given arguments.
         *
         * @param i the double function argument
         * @param t the object function argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(double i, T t) throws E;
    }

    /**
     * Represents a predicate (boolean-valued function) of a double value and an object.
     *
     * @param <T> the type of the object argument to the predicate
     * @param <E> the type of exception that the predicate may throw
     */
    @FunctionalInterface
    public interface DoubleObjPredicate<T, E extends Throwable> {
        /**
         * Evaluates this predicate on the given arguments.
         *
         * @param i the double input argument
         * @param t the object input argument
         * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
         * @throws E if an error occurs during predicate evaluation
         */
        boolean test(double i, T t) throws E;
    }

    /**
     * Represents a function that accepts a boolean array and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface BooleanNFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given boolean array.
         *
         * @param args the boolean array argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(boolean... args) throws E;

        /**
         * Returns a composed function that first applies this function to its input,
         * and then applies the {@code after} function to the result.
         *
         * @param <V> the type of output of the {@code after} function, and of the composed function
         * @param after the function to apply after this function is applied
         * @return a composed function that first applies this function and then applies the {@code after} function
         */
        default <V> BooleanNFunction<V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) {
            return args -> after.apply(apply(args));
        }
    }

    /**
     * Represents a function that accepts a char array and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface CharNFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given char array.
         *
         * @param args the char array argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(char... args) throws E;

        /**
         * Returns a composed function that first applies this function to its input,
         * and then applies the {@code after} function to the result.
         *
         * @param <V> the type of output of the {@code after} function, and of the composed function
         * @param after the function to apply after this function is applied
         * @return a composed function that first applies this function and then applies the {@code after} function
         */
        default <V> CharNFunction<V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) {
            return args -> after.apply(apply(args));
        }
    }

    /**
     * Represents a function that accepts a byte array and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ByteNFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given byte array.
         *
         * @param args the byte array argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(byte... args) throws E;

        /**
         * Returns a composed function that first applies this function to its input,
         * and then applies the {@code after} function to the result.
         *
         * @param <V> the type of output of the {@code after} function, and of the composed function
         * @param after the function to apply after this function is applied
         * @return a composed function that first applies this function and then applies the {@code after} function
         */
        default <V> ByteNFunction<V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) {
            return args -> after.apply(apply(args));
        }
    }

    /**
     * Represents a function that accepts a short array and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface ShortNFunction<R, E extends Throwable> {
        /**
         * Applies this function to the given short array.
         *
         * @param args the short array argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(short... args) throws E;

        /**
         * Returns a composed function that first applies this function to its input,
         * and then applies the {@code after} function to the result.
         *
         * @param <V> the type of output of the {@code after} function, and of the composed function
         * @param after the function to apply after this function is applied
         * @return a composed function that first applies this function and then applies the {@code after} function
         */
        default <V> ShortNFunction<V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) {
            return args -> after.apply(apply(args));
        }
    }

    /**
     * Represents a function that accepts an int array and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface IntNFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given int array.
         *
         * @param args the int array argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(int... args) throws E;

        /**
         * Returns a composed function that first applies this function to its input,
         * and then applies the {@code after} function to the result.
         *
         * @param <V> the type of output of the {@code after} function, and of the composed function
         * @param after the function to apply after this function is applied
         * @return a composed function that first applies this function and then applies the {@code after} function
         */
        default <V> IntNFunction<V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) {
            return args -> after.apply(apply(args));
        }
    }

    /**
     * Represents a function that accepts a long array and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface LongNFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given long array.
         *
         * @param args the long array argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(long... args) throws E;

        /**
         * Returns a composed function that first applies this function to its input,
         * and then applies the {@code after} function to the result.
         *
         * @param <V> the type of output of the {@code after} function, and of the composed function
         * @param after the function to apply after this function is applied
         * @return a composed function that first applies this function and then applies the {@code after} function
         */
        default <V> LongNFunction<V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) {
            return args -> after.apply(apply(args));
        }
    }

    /**
     * Represents a function that accepts a float array and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface FloatNFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given float array.
         *
         * @param args the float array argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(float... args) throws E;

        /**
         * Returns a composed function that first applies this function to its input,
         * and then applies the {@code after} function to the result.
         *
         * @param <V> the type of output of the {@code after} function, and of the composed function
         * @param after the function to apply after this function is applied
         * @return a composed function that first applies this function and then applies the {@code after} function
         */
        default <V> FloatNFunction<V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) {
            return args -> after.apply(apply(args));
        }
    }

    /**
     * Represents a function that accepts a double array and produces a result.
     *
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface DoubleNFunction<R, E extends Throwable> {

        /**
         * Applies this function to the given double array.
         *
         * @param args the double array argument
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        R apply(double... args) throws E;

        /**
         * Returns a composed function that first applies this function to its input,
         * and then applies the {@code after} function to the result.
         *
         * @param <V> the type of output of the {@code after} function, and of the composed function
         * @param after the function to apply after this function is applied
         * @return a composed function that first applies this function and then applies the {@code after} function
         */
        default <V> DoubleNFunction<V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) {
            return args -> after.apply(apply(args));
        }
    }

    /**
     * Represents a function that accepts a variable number of arguments and produces a result.
     *
     * @param <T> the type of the input arguments to the function
     * @param <R> the type of the result of the function
     * @param <E> the type of exception that the function may throw
     */
    @FunctionalInterface
    public interface NFunction<T, R, E extends Throwable> {

        /**
         * Applies this function to the given arguments.
         *
         * @param args the variable arguments of type T
         * @return the function result
         * @throws E if an error occurs during function execution
         */
        @SuppressWarnings("unchecked")
        R apply(T... args) throws E;

        /**
         * Returns a composed function that first applies this function to its input,
         * and then applies the {@code after} function to the result.
         *
         * @param <V> the type of output of the {@code after} function, and of the composed function
         * @param after the function to apply after this function is applied
         * @return a composed function that first applies this function and then applies the {@code after} function
         */
        default <V> NFunction<T, V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) {
            return args -> after.apply(apply(args));
        }
    }

    /**
     * Represents an operation that accepts an int index and a boolean value and returns no result.
     * This is the indexed variant of BooleanConsumer.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface IntBooleanConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param idx the index
         * @param e the boolean element at the index
         * @throws E if an error occurs during operation execution
         */
        void accept(int idx, boolean e) throws E;
    }

    /**
     * Represents an operation that accepts an int index and a char value and returns no result.
     * This is the indexed variant of CharConsumer.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface IntCharConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param idx the index
         * @param e the char element at the index
         * @throws E if an error occurs during operation execution
         */
        void accept(int idx, char e) throws E;
    }

    /**
     * Represents an operation that accepts an int index and a byte value and returns no result.
     * This is the indexed variant of ByteConsumer.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface IntByteConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param idx the index
         * @param e the byte element at the index
         * @throws E if an error occurs during operation execution
         */
        void accept(int idx, byte e) throws E;
    }

    /**
     * Represents an operation that accepts an int index and a short value and returns no result.
     * This is the indexed variant of ShortConsumer.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface IntShortConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param idx the index
         * @param e the short element at the index
         * @throws E if an error occurs during operation execution
         */
        void accept(int idx, short e) throws E;
    }

    /**
     * Represents an operation that accepts two int values and returns no result.
     * The first int typically represents an index.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface IntIntConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param idx the index
         * @param e the int element at the index
         * @throws E if an error occurs during operation execution
         */
        void accept(int idx, int e) throws E;
    }

    /**
     * Represents an operation that accepts an int index and a long value and returns no result.
     * This is the indexed variant of LongConsumer.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface IntLongConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param idx the index
         * @param e the long element at the index
         * @throws E if an error occurs during operation execution
         */
        void accept(int idx, long e) throws E;
    }

    /**
     * Represents an operation that accepts an int index and a float value and returns no result.
     * This is the indexed variant of FloatConsumer.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface IntFloatConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param idx the index
         * @param e the float element at the index
         * @throws E if an error occurs during operation execution
         */
        void accept(int idx, float e) throws E;
    }

    /**
     * Represents an operation that accepts an int index and a double value and returns no result.
     * This is the indexed variant of DoubleConsumer.
     *
     * @param <E> the type of exception that the consumer may throw
     */
    @FunctionalInterface
    public interface IntDoubleConsumer<E extends Throwable> {

        /**
         * Performs this operation on the given arguments.
         *
         * @param idx the index
         * @param e the double element at the index
         * @throws E if an error occurs during operation execution
         */
        void accept(int idx, double e) throws E;
    }

    /**
     * Utility class containing functional interfaces that can throw two different types of exceptions.
     */
    public static final class EE {
        private EE() {
            // Singleton. Utility class.
        }

        /**
         * Represents a task that returns no result and may throw two types of exceptions.
         *
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         */
        @FunctionalInterface
        public interface Runnable<E extends Throwable, E2 extends Throwable> {

            /**
             * Executes the task.
             *
             * @throws E if the first type of error occurs during execution
             * @throws E2 if the second type of error occurs during execution
             */
            void run() throws E, E2;
        }

        /**
         * Represents a task that returns a result and may throw two types of exceptions.
         *
         * @param <R> the type of the result
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         */
        @FunctionalInterface
        public interface Callable<R, E extends Throwable, E2 extends Throwable> {

            /**
             * Computes a result.
             *
             * @return the computed result
             * @throws E if the first type of error occurs during computation
             * @throws E2 if the second type of error occurs during computation
             */
            R call() throws E, E2;
        }

        /**
         * Represents a supplier of results that may throw two types of exceptions.
         *
         * @param <T> the type of results supplied by this supplier
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         */
        @FunctionalInterface
        public interface Supplier<T, E extends Throwable, E2 extends Throwable> {

            /**
             * Gets a result.
             *
             * @return a result
             * @throws E if the first type of error occurs while getting the result
             * @throws E2 if the second type of error occurs while getting the result
             */
            T get() throws E, E2;
        }

        /**
         * Represents a predicate (boolean-valued function) of one argument that may throw two types of exceptions.
         *
         * @param <T> the type of the input to the predicate
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         */
        @FunctionalInterface
        public interface Predicate<T, E extends Throwable, E2 extends Throwable> {

            /**
             * Evaluates this predicate on the given argument.
             *
             * @param t the input argument
             * @return {@code true} if the input argument matches the predicate, otherwise {@code false}
             * @throws E if the first type of error occurs during evaluation
             * @throws E2 if the second type of error occurs during evaluation
             */
            boolean test(T t) throws E, E2;
        }

        /**
         * Represents a predicate (boolean-valued function) of two arguments that may throw two types of exceptions.
         *
         * @param <T> the type of the first argument to the predicate
         * @param <U> the type of the second argument to the predicate
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         */
        @FunctionalInterface
        public interface BiPredicate<T, U, E extends Throwable, E2 extends Throwable> {

            /**
             * Evaluates this predicate on the given arguments.
             *
             * @param t the first input argument
             * @param u the second input argument
             * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
             * @throws E if the first type of error occurs during evaluation
             * @throws E2 if the second type of error occurs during evaluation
             */
            boolean test(T t, U u) throws E, E2;
        }

        /**
         * Represents a predicate (boolean-valued function) of three arguments that may throw two types of exceptions.
         *
         * @param <A> the type of the first argument to the predicate
         * @param <B> the type of the second argument to the predicate
         * @param <C> the type of the third argument to the predicate
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         */
        @FunctionalInterface
        public interface TriPredicate<A, B, C, E extends Throwable, E2 extends Throwable> {

            /**
             * Evaluates this predicate on the given arguments.
             *
             * @param a the first input argument
             * @param b the second input argument
             * @param c the third input argument
             * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
             * @throws E if the first type of error occurs during evaluation
             * @throws E2 if the second type of error occurs during evaluation
             */
            boolean test(A a, B b, C c) throws E, E2;
        }

        /**
         * Represents a function that accepts one argument and produces a result, and may throw two types of exceptions.
         *
         * @param <T> the type of the input to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         */
        @FunctionalInterface
        public interface Function<T, R, E extends Throwable, E2 extends Throwable> {

            /**
             * Applies this function to the given argument.
             *
             * @param t the function argument
             * @return the function result
             * @throws E if the first type of error occurs during function execution
             * @throws E2 if the second type of error occurs during function execution
             */
            R apply(T t) throws E, E2;
        }

        /**
         * Represents a function that accepts two arguments and produces a result, and may throw two types of exceptions.
         *
         * @param <T> the type of the first argument to the function
         * @param <U> the type of the second argument to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         */
        @FunctionalInterface
        public interface BiFunction<T, U, R, E extends Throwable, E2 extends Throwable> {

            /**
             * Applies this function to the given arguments.
             *
             * @param t the first function argument
             * @param u the second function argument
             * @return the function result
             * @throws E if the first type of error occurs during function execution
             * @throws E2 if the second type of error occurs during function execution
             */
            R apply(T t, U u) throws E, E2;
        }

        /**
         * Represents a function that accepts three arguments and produces a result, and may throw two types of exceptions.
         *
         * @param <A> the type of the first argument to the function
         * @param <B> the type of the second argument to the function
         * @param <C> the type of the third argument to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         */
        @FunctionalInterface
        public interface TriFunction<A, B, C, R, E extends Throwable, E2 extends Throwable> {

            /**
             * Applies this function to the given arguments.
             *
             * @param a the first function argument
             * @param b the second function argument
             * @param c the third function argument
             * @return the function result
             * @throws E if the first type of error occurs during function execution
             * @throws E2 if the second type of error occurs during function execution
             */
            R apply(A a, B b, C c) throws E, E2;
        }

        /**
         * Represents an operation that accepts a single input argument and returns no result, and may throw two types of exceptions.
         *
         * @param <T> the type of the input to the operation
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         */
        @FunctionalInterface
        public interface Consumer<T, E extends Throwable, E2 extends Throwable> {

            /**
             * Performs this operation on the given argument.
             *
             * @param t the input argument
             * @throws E if the first type of error occurs during operation execution
             * @throws E2 if the second type of error occurs during operation execution
             */
            void accept(T t) throws E, E2;
        }

        /**
         * Represents an operation that accepts two input arguments and returns no result, and may throw two types of exceptions.
         *
         * @param <T> the type of the first argument to the operation
         * @param <U> the type of the second argument to the operation
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         */
        @FunctionalInterface
        public interface BiConsumer<T, U, E extends Throwable, E2 extends Throwable> {

            /**
             * Performs this operation on the given arguments.
             *
             * @param t the first input argument
             * @param u the second input argument
             * @throws E if the first type of error occurs during operation execution
             * @throws E2 if the second type of error occurs during operation execution
             */
            void accept(T t, U u) throws E, E2;
        }

        /**
         * Represents an operation that accepts three input arguments and returns no result, and may throw two types of exceptions.
         *
         * @param <A> the type of the first argument to the operation
         * @param <B> the type of the second argument to the operation
         * @param <C> the type of the third argument to the operation
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         */
        @FunctionalInterface
        public interface TriConsumer<A, B, C, E extends Throwable, E2 extends Throwable> {

            /**
             * Performs this operation on the given arguments.
             *
             * @param a the first input argument
             * @param b the second input argument
             * @param c the third input argument
             * @throws E if the first type of error occurs during operation execution
             * @throws E2 if the second type of error occurs during operation execution
             */
            void accept(A a, B b, C c) throws E, E2;
        }
    }

    /**
     * Utility class containing functional interfaces that can throw three different types of exceptions.
     */
    public static final class EEE {

        private EEE() {
            // Singleton. Utility class.
        }

        /**
         * Represents a task that returns no result and may throw three types of exceptions.
         *
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         * @param <E3> the type of the third exception that may be thrown
         */
        @FunctionalInterface
        public interface Runnable<E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             * Executes the task.
             *
             * @throws E if the first type of error occurs during execution
             * @throws E2 if the second type of error occurs during execution
             * @throws E3 if the third type of error occurs during execution
             */
            void run() throws E, E2, E3;
        }

        /**
         * Represents a task that returns a result and may throw three types of exceptions.
         *
         * @param <R> the type of the result
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         * @param <E3> the type of the third exception that may be thrown
         */
        @FunctionalInterface
        public interface Callable<R, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             * Computes a result.
             *
             * @return the computed result
             * @throws E if the first type of error occurs during computation
             * @throws E2 if the second type of error occurs during computation
             * @throws E3 if the third type of error occurs during computation
             */
            R call() throws E, E2, E3;
        }

        /**
         * Represents a supplier of results that may throw three types of exceptions.
         *
         * @param <T> the type of results supplied by this supplier
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         * @param <E3> the type of the third exception that may be thrown
         */
        @FunctionalInterface
        public interface Supplier<T, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             * Gets a result.
             *
             * @return a result
             * @throws E if the first type of error occurs while getting the result
             * @throws E2 if the second type of error occurs while getting the result
             * @throws E3 if the third type of error occurs while getting the result
             */
            T get() throws E, E2, E3;
        }

        /**
         * Represents a predicate (boolean-valued function) of one argument that may throw three types of exceptions.
         *
         * @param <T> the type of the input to the predicate
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         * @param <E3> the type of the third exception that may be thrown
         */
        @FunctionalInterface
        public interface Predicate<T, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             * Evaluates this predicate on the given argument.
             *
             * @param t the input argument
             * @return {@code true} if the input argument matches the predicate, otherwise {@code false}
             * @throws E if the first type of error occurs during evaluation
             * @throws E2 if the second type of error occurs during evaluation
             * @throws E3 if the third type of error occurs during evaluation
             */
            boolean test(T t) throws E, E2, E3;
        }

        /**
         * Represents a predicate (boolean-valued function) of two arguments that may throw three types of exceptions.
         *
         * @param <T> the type of the first argument to the predicate
         * @param <U> the type of the second argument to the predicate
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         * @param <E3> the type of the third exception that may be thrown
         */
        @FunctionalInterface
        public interface BiPredicate<T, U, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             * Evaluates this predicate on the given arguments.
             *
             * @param t the first input argument
             * @param u the second input argument
             * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
             * @throws E if the first type of error occurs during evaluation
             * @throws E2 if the second type of error occurs during evaluation
             * @throws E3 if the third type of error occurs during evaluation
             */
            boolean test(T t, U u) throws E, E2, E3;
        }

        /**
         * Represents a predicate (boolean-valued function) of three arguments that may throw three types of exceptions.
         *
         * @param <A> the type of the first argument to the predicate
         * @param <B> the type of the second argument to the predicate
         * @param <C> the type of the third argument to the predicate
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         * @param <E3> the type of the third exception that may be thrown
         */
        @FunctionalInterface
        public interface TriPredicate<A, B, C, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             * Evaluates this predicate on the given arguments.
             *
             * @param a the first input argument
             * @param b the second input argument
             * @param c the third input argument
             * @return {@code true} if the input arguments match the predicate, otherwise {@code false}
             * @throws E if the first type of error occurs during evaluation
             * @throws E2 if the second type of error occurs during evaluation
             * @throws E3 if the third type of error occurs during evaluation
             */
            boolean test(A a, B b, C c) throws E, E2, E3;
        }

        /**
         * Represents a function that accepts one argument and produces a result, and may throw three types of exceptions.
         *
         * @param <T> the type of the input to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         * @param <E3> the type of the third exception that may be thrown
         */
        @FunctionalInterface
        public interface Function<T, R, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             * Applies this function to the given argument.
             *
             * @param t the function argument
             * @return the function result
             * @throws E if the first type of error occurs during function execution
             * @throws E2 if the second type of error occurs during function execution
             * @throws E3 if the third type of error occurs during function execution
             */
            R apply(T t) throws E, E2, E3;
        }

        /**
         * Represents a function that accepts two arguments and produces a result, and may throw three types of exceptions.
         *
         * @param <T> the type of the first argument to the function
         * @param <U> the type of the second argument to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         * @param <E3> the type of the third exception that may be thrown
         */
        @FunctionalInterface
        public interface BiFunction<T, U, R, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             * Applies this function to the given arguments.
             *
             * @param t the first function argument
             * @param u the second function argument
             * @return the function result
             * @throws E if the first type of error occurs during function execution
             * @throws E2 if the second type of error occurs during function execution
             * @throws E3 if the third type of error occurs during function execution
             */
            R apply(T t, U u) throws E, E2, E3;
        }

        /**
         * Represents a function that accepts three arguments and produces a result, and may throw three types of exceptions.
         *
         * @param <A> the type of the first argument to the function
         * @param <B> the type of the second argument to the function
         * @param <C> the type of the third argument to the function
         * @param <R> the type of the result of the function
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         * @param <E3> the type of the third exception that may be thrown
         */
        @FunctionalInterface
        public interface TriFunction<A, B, C, R, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             * Applies this function to the given arguments.
             *
             * @param a the first function argument
             * @param b the second function argument
             * @param c the third function argument
             * @return the function result
             * @throws E if the first type of error occurs during function execution
             * @throws E2 if the second type of error occurs during function execution
             * @throws E3 if the third type of error occurs during function execution
             */
            R apply(A a, B b, C c) throws E, E2, E3;
        }

        /**
         * Represents an operation that accepts a single input argument and returns no result, and may throw three types of exceptions.
         *
         * @param <T> the type of the input to the operation
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         * @param <E3> the type of the third exception that may be thrown
         */
        @FunctionalInterface
        public interface Consumer<T, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             * Performs this operation on the given argument.
             *
             * @param t the input argument
             * @throws E if the first type of error occurs during operation execution
             * @throws E2 if the second type of error occurs during operation execution
             * @throws E3 if the third type of error occurs during operation execution
             */
            void accept(T t) throws E, E2, E3;
        }

        /**
         * Represents an operation that accepts two input arguments and returns no result, and may throw three types of exceptions.
         *
         * @param <T> the type of the first argument to the operation
         * @param <U> the type of the second argument to the operation
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         * @param <E3> the type of the third exception that may be thrown
         */
        @FunctionalInterface
        public interface BiConsumer<T, U, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             * Performs this operation on the given arguments.
             *
             * @param t the first input argument
             * @param u the second input argument
             * @throws E if the first type of error occurs during operation execution
             * @throws E2 if the second type of error occurs during operation execution
             * @throws E3 if the third type of error occurs during operation execution
             */
            void accept(T t, U u) throws E, E2, E3;
        }

        /**
         * Represents an operation that accepts three input arguments and returns no result, and may throw three types of exceptions.
         *
         * @param <A> the type of the first argument to the operation
         * @param <B> the type of the second argument to the operation
         * @param <C> the type of the third argument to the operation
         * @param <E> the type of the first exception that may be thrown
         * @param <E2> the type of the second exception that may be thrown
         * @param <E3> the type of the third exception that may be thrown
         */
        @FunctionalInterface
        public interface TriConsumer<A, B, C, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             * Performs this operation on the given arguments.
             *
             * @param a the first input argument
             * @param b the second input argument
             * @param c the third input argument
             * @throws E if the first type of error occurs during operation execution
             * @throws E2 if the second type of error occurs during operation execution
             * @throws E3 if the third type of error occurs during operation execution
             */
            void accept(A a, B b, C c) throws E, E2, E3;
        }
    }

    /**
     * A thread-safe lazy initializer that defers the creation of the underlying value until first access.
     * The value is computed exactly once using the provided supplier, and subsequent calls return the cached value.
     *
     * @param <T> the type of the value to be lazily initialized
     * @param <E> the type of exception that may be thrown during initialization
     */
    static final class LazyInitializer<T, E extends Throwable> implements Throwables.Supplier<T, E> {
        private final Supplier<T, E> supplier;
        private volatile boolean initialized = false;
        private volatile T value = null; //NOSONAR

        LazyInitializer(final Throwables.Supplier<T, E> supplier) {
            N.checkArgNotNull(supplier, cs.supplier);

            this.supplier = supplier;
        }

        /**
         * Creates a new LazyInitializer with the specified supplier.
         * If the supplier is already a LazyInitializer, it is returned as-is to avoid double-wrapping.
         *
         * <p>The returned LazyInitializer will call the supplier exactly once on the first invocation of {@code get()},
         * and cache the result for all subsequent calls. The initialization is thread-safe using double-checked locking.
         *
         * <p>Example usage:
         * <pre>{@code
         * LazyInitializer<Database, SQLException> dbInit = LazyInitializer.of(() ->
         *     createExpensiveDatabase()
         * );
         * // Database is not created until first call to dbInit.get()
         * Database db = dbInit.get(); // Initializes once
         * Database sameDb = dbInit.get(); // Returns cached instance
         * }</pre>
         *
         * @param <T> the type of the value to be lazily initialized
         * @param <E> the type of exception that may be thrown during initialization
         * @param supplier the supplier that will provide the value when first requested
         * @return a LazyInitializer that will use the provided supplier
         * @throws IllegalArgumentException if supplier is null
         */
        public static <T, E extends Throwable> LazyInitializer<T, E> of(final Throwables.Supplier<T, E> supplier) {
            N.checkArgNotNull(supplier);

            if (supplier instanceof LazyInitializer) {
                return (LazyInitializer<T, E>) supplier;
            }

            return new LazyInitializer<>(supplier);
        }

        /**
         * Gets the lazily initialized value. On first access, the value is computed using the supplier
         * and cached for subsequent calls. This method is thread-safe.
         *
         * @return the lazily initialized value
         * @throws E if the supplier throws an exception during initialization
         */
        @Override
        public T get() throws E {
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        final T temp = supplier.get();
                        value = temp;
                        initialized = true;
                    }
                }
            }

            return value;
        }
    }
}
