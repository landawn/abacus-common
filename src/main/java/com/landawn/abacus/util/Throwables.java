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
     * Executes the provided {@code cmd} that may throw an exception.
     *
     * This method is useful when you want to run a piece of code that might throw an exception.
     * If an exception occurs during the execution of the {@code cmd}, it is rethrown as a RuntimeException.
     *
     * @param cmd The runnable task that might throw an exception. Must not be {@code null}.
     * @throws RuntimeException if an exception occurs during the execution of the {@code cmd}.
     * @see Try#run(Throwables.Runnable)
     */
    @Beta
    public static void run(final Throwables.Runnable<? extends Throwable> cmd) {
        try {
            cmd.run();
        } catch (final Throwable e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Executes the provided {@code cmd} and if an exception occurs, applies the {@code actionOnError} consumer on the exception.
     *
     * <p>This method is useful when you want to run a piece of code that might throw an exception, and you want to handle that exception in a specific way.</p>
     *
     * @param cmd The runnable task that might throw an exception, must not be {@code null}.
     * @param actionOnError The consumer to handle any exceptions thrown by the {@code cmd}, must not be {@code null}.
     * @see Try#run(Throwables.Runnable, java.util.function.Consumer)
     */
    @Beta
    public static void run(final Throwables.Runnable<? extends Throwable> cmd, final java.util.function.Consumer<? super Throwable> actionOnError) {
        try {
            cmd.run();
        } catch (final Throwable e) {
            actionOnError.accept(e);
        }
    }

    /**
     * Executes the provided {@code cmd} that may throw an exception and returns the result.
     *
     * This method is useful when you want to run a piece of code that might throw an exception, and you need the result of that code.
     * If an exception occurs during the execution of the {@code cmd}, it is rethrown as a RuntimeException.
     *
     * @param <R> The type of the result.
     * @param cmd The callable task that might throw an exception and returns a result. Must not be {@code null}.
     * @return The result of the {@code cmd}.
     * @throws RuntimeException if an exception occurs during the execution of the {@code cmd}.
     * @see Try#call(java.util.concurrent.Callable)
     */
    @Beta
    public static <R> R call(final Throwables.Callable<R, ? extends Throwable> cmd) {
        try {
            return cmd.call();
        } catch (final Throwable e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     * Executes the provided {@code cmd} that may throw an exception and returns the result.
     * If an exception occurs during the execution of the {@code cmd}, the {@code actionOnError} function is applied to the exception to provide a return value.
     *
     * This method is useful when you want to run a piece of code that might throw an exception, and you need the result of that code.
     * It allows you to handle exceptions in a specific way by providing a function that can transform an exception into a return value.
     *
     * @param <R> The type of the result.
     * @param cmd The callable task that might throw an exception and returns a result. Must not be {@code null}.
     * @param actionOnError The function to apply to the exception if one is thrown by the {@code cmd}. Must not be {@code null}.
     * @return The result of the {@code cmd} or the result of applying the {@code actionOnError} function to the exception if one is thrown.
     * @see Try#call(java.util.concurrent.Callable, java.util.function.Function)
     */
    @Beta
    public static <R> R call(final Throwables.Callable<R, ? extends Throwable> cmd,
            final java.util.function.Function<? super Throwable, ? extends R> actionOnError) {
        try {
            return cmd.call();
        } catch (final Throwable e) {
            return actionOnError.apply(e);
        }
    }

    /**
     * Executes the provided {@code cmd} that may throw an exception and returns the result.
     * If an exception occurs during the execution of the {@code cmd}, the {@code supplier} is used to provide a return value.
     *
     * This method is useful when you want to run a piece of code that might throw an exception, and you need the result of that code.
     * It allows you to handle exceptions in a specific way by providing a supplier that can provide a return value when an exception occurs.
     *
     * @param <R> The type of the result.
     * @param cmd The callable task that might throw an exception and returns a result. Must not be {@code null}.
     * @param supplier The supplier to provide a return value when an exception occurs. Must not be {@code null}.
     * @return The result of the {@code cmd} or the result of the {@code supplier} if an exception occurs.
     * @see Try#call(java.util.concurrent.Callable, java.util.function.Supplier)
     */
    @Beta
    public static <R> R call(final Throwables.Callable<R, ? extends Throwable> cmd, final java.util.function.Supplier<R> supplier) {
        N.checkArgNotNull(supplier);

        try {
            return cmd.call();
        } catch (final Throwable e) {
            return supplier.get();
        }
    }

    /**
     * Executes the provided {@code cmd} that may throw an exception and returns the result.
     * If an exception occurs during the execution of the {@code cmd}, the provided default value is returned.
     *
     * This method is useful when you want to run a piece of code that might throw an exception, and you need the result of that code.
     * It allows you to handle exceptions in a specific way by providing a default value that will be returned when an exception occurs.
     *
     * @param <R> The type of the result.
     * @param cmd The callable task that might throw an exception and returns a result. Must not be {@code null}.
     * @param defaultValue The default value to return if an exception occurs during the execution of the {@code cmd}.
     * @return The result of the {@code cmd} or the default value if an exception occurs.
     * @see Try#call(java.util.concurrent.Callable, Object)
     */
    @Beta
    public static <R> R call(final Throwables.Callable<R, ? extends Throwable> cmd, final R defaultValue) {
        try {
            return cmd.call();
        } catch (final Throwable e) {
            return defaultValue;
        }
    }

    /**
     * Executes the provided {@code cmd} and if an exception occurs, applies the {@code supplier} to provide a return value.
     * The {@code predicate} is used to test the exception. If the {@code predicate} returns {@code true}, the {@code supplier} is used to provide a return value.
     * If the {@code predicate} returns {@code false}, the exception is rethrown as a RuntimeException.
     *
     * @param <R> The type of the result.
     * @param cmd The callable task that might throw an exception, must not be {@code null}.
     * @param predicate The predicate to test the exception, must not be {@code null}.
     * @param supplier The supplier to provide a return value when an exception occurs and the {@code predicate} returns {@code true}, must not be {@code null}.
     * @return The result of the {@code cmd} or the result of the {@code supplier} if an exception occurs and the {@code predicate} returns {@code true}.
     * @throws RuntimeException if an exception occurs and the {@code predicate} returns {@code false}.
     * @see Try#call(java.util.concurrent.Callable, java.util.function.Predicate, java.util.function.Supplier)
     */
    @Beta
    public static <R> R call(final Throwables.Callable<R, ? extends Throwable> cmd, final java.util.function.Predicate<? super Throwable> predicate,
            final java.util.function.Supplier<R> supplier) {
        N.checkArgNotNull(supplier);

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
     * Executes the provided {@code cmd} that may throw an exception and returns the result.
     * If an exception occurs during the execution of the {@code cmd}, the provided default value is returned if the {@code predicate} returns {@code true}.
     * If the {@code predicate} returns {@code false}, the exception is rethrown as a RuntimeException.
     *
     * This method is useful when you want to run a piece of code that might throw an exception, and you need the result of that code.
     * It allows you to handle exceptions in a specific way by providing a default value that will be returned when an exception occurs and the {@code predicate} returns {@code true}.
     *
     * @param <R> The type of the result.
     * @param cmd The callable task that might throw an exception and returns a result. Must not be {@code null}.
     * @param predicate The predicate to test the exception. If it returns {@code true}, the default value is returned. If it returns {@code false}, the exception is rethrown. Must not be {@code null}.
     * @param defaultValue The default value to return if an exception occurs during the execution of the {@code cmd} and the {@code predicate} returns {@code true}.
     * @return The result of the {@code cmd} or the default value if an exception occurs and the {@code predicate} returns {@code true}.
     * @throws RuntimeException if an exception occurs and the {@code predicate} returns {@code false}.
     * @see Try#call(java.util.concurrent.Callable, java.util.function.Predicate, Object)
     */
    @Beta
    public static <R> R call(final Throwables.Callable<R, ? extends Throwable> cmd, final java.util.function.Predicate<? super Throwable> predicate,
            final R defaultValue) {
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
         *
         * @param <T>
         * @param <E>
         * @return
         */
        public static <T, E extends Throwable> Throwables.Iterator<T, E> empty() {
            return EMPTY;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param val
         * @return
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
         *
         * @param <T>
         * @param <E>
         * @param a
         * @return
         */
        @SafeVarargs
        public static <T, E extends Exception> Throwables.Iterator<T, E> of(final T... a) {
            return N.isEmpty(a) ? EMPTY : of(a, 0, a.length);
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param a
         * @param fromIndex
         * @param toIndex
         * @return
         * @throws IndexOutOfBoundsException
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
                    N.checkArgNotNegative(n, cs.n);

                    if (n > toIndex - cursor) {
                        cursor = toIndex;
                    } else {
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
         *
         * @param <T>
         * @param <E>
         * @param iterable
         * @return
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
         *
         * @param <T>
         * @param <E>
         * @param iter
         * @return
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
         *
         * @param <T> The type of the elements in the Iterator.
         * @param <E> The type of the exception that may be thrown.
         * @param iteratorSupplier A Supplier that provides the Throwables.Iterator when needed.
         * @return A Throwables.Iterator that is initialized on the first call to hasNext() or next().
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
                public void advance(final long n) throws IllegalArgumentException, E {
                    N.checkArgNotNegative(n, cs.n);

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
                    if (!isInitialized) {
                        init();
                    }

                    if (iter != null) {
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
         *
         * @param <T>
         * @param <E>
         * @param a
         * @return
         */
        @SafeVarargs
        public static <T, E extends Exception> Throwables.Iterator<T, E> concat(final Throwables.Iterator<? extends T, ? extends E>... a) {
            return concat(N.asList(a));
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param c
         * @return
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
         * It's caller's responsibility to close the specified {@code reader}.
         *
         * @param reader
         * @return
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
                        throw new NoSuchElementException("No more lines");
                    }

                    final String res = cachedLine;
                    cachedLine = null;
                    return res;
                }
            };
        }

        /**
         *
         * @return
         * @throws E
         */
        public abstract boolean hasNext() throws E;

        /**
         *
         * @return
         * @throws E
         */
        public abstract T next() throws E;

        /**
         *
         * @param n
         * @throws IllegalArgumentException
         * @throws E the e
         */
        public void advance(long n) throws IllegalArgumentException, E {
            N.checkArgNotNegative(n, cs.n);

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

        private boolean isClosed = false;

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
         *
         * @param predicate
         * @return
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
         *
         * @param <U>
         * @param mapper
         * @return
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
         *
         * @return
         * @throws E
         */
        public Nullable<T> first() throws E {
            if (hasNext()) {
                return Nullable.of(next());
            } else {
                return Nullable.empty();
            }
        }

        /**
         *
         * @return
         * @throws E
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
         *
         * @return
         * @throws E
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
         *
         * @return
         * @throws E
         */
        public Object[] toArray() throws E {
            return toArray(N.EMPTY_OBJECT_ARRAY);
        }

        /**
         *
         * @param <A>
         * @param a
         * @return
         * @throws E
         */
        public <A> A[] toArray(final A[] a) throws E {
            return toList().toArray(a);
        }

        /**
         *
         * @return
         * @throws E
         */
        public List<T> toList() throws E {
            final List<T> list = new ArrayList<>();

            while (hasNext()) {
                list.add(next());
            }

            return list;
        }

        /**
         *
         * @param action
         * @throws E
         */
        public void forEachRemaining(final java.util.function.Consumer<? super T> action) throws E { // NOSONAR
            while (hasNext()) {
                action.accept(next());
            }
        }

        /**
         *
         * @param <E2>
         * @param action
         * @throws E the e
         * @throws E2
         */
        public <E2 extends Throwable> void foreachRemaining(final Throwables.Consumer<? super T, E2> action) throws E, E2 { // NOSONAR
            while (hasNext()) {
                action.accept(next());
            }
        }

        /**
         *
         * @param <E2>
         * @param action
         * @throws E the e
         * @throws E2
         */
        public <E2 extends Throwable> void foreachIndexed(final Throwables.IntObjConsumer<? super T, E2> action) throws E, E2 {
            int idx = 0;

            while (hasNext()) {
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
         *
         * @throws E the e
         */
        void run() throws E;

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
         *
         * @return
         * @throws E the e
         */
        R call() throws E;

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
         *
         * @return
         * @throws E the e
         */
        T get() throws E;

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
         * Returns the value as boolean.
         *
         * @return
         * @throws E the e
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
         * Return the value as char.
         *
         * @return
         * @throws E the e
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
         * Return the value as byte.
         *
         * @return
         * @throws E the e
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
         * Return the value as short.
         *
         * @return
         * @throws E the e
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
         * Return the value as int.
         *
         * @return
         * @throws E the e
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
         * Return the value as long.
         *
         * @return
         * @throws E the e
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
         * Return the value as float.
         *
         * @return
         * @throws E the e
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
         * Return the value as double.
         *
         * @return
         * @throws E the e
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
         *
         * @param t
         * @return
         * @throws E the e
         */
        boolean test(T t) throws E;

        default Predicate<T, E> negate() {
            return t -> !test(t);
        }

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
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        boolean test(T t, U u) throws E;

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
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
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
         *
         * @param a
         * @param b
         * @param c
         * @param d
         * @return
         * @throws E the e
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
         *
         * @param t
         * @return
         * @throws E the e
         */
        R apply(T t) throws E;

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
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        R apply(T t, U u) throws E;

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
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
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
         *
         * @param a
         * @param b
         * @param c
         * @param d
         * @return
         * @throws E the e
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
         *
         * @param t
         * @throws E the e
         */
        void accept(T t) throws E;

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
         *
         * @param t
         * @param u
         * @throws E the e
         */
        void accept(T t, U u) throws E;

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
         *
         * @param a
         * @param b
         * @param c
         * @throws E the e
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
         *
         * @param a
         * @param b
         * @param c
         * @param d
         * @throws E the e
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
         *
         * @param t
         * @throws E the e
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
         *
         * @param value
         * @return
         * @throws E the e
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
         *
         * @param value
         * @return
         * @throws E the e
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
         *
         * @param t
         * @throws E the e
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
         *
         * @param value
         * @return
         * @throws E the e
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
         *
         * @param value
         * @return
         * @throws E the e
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
         *
         * @param t
         * @throws E the e
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
         *
         * @param value
         * @return
         * @throws E the e
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
         *
         * @param value
         * @return
         * @throws E the e
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
         *
         * @param t
         * @throws E the e
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
         *
         * @param value
         * @return
         * @throws E the e
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
         *
         * @param value
         * @return
         * @throws E the e
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
         *
         * @param t
         * @throws E the e
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
         *
         * @param value
         * @return
         * @throws E the e
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
         *
         * @param value
         * @return
         * @throws E the e
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
         *
         * @param value
         * @return
         * @throws E the e
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
         *
         * @param value
         * @return
         * @throws E the e
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
         *
         * @param t
         * @throws E the e
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
         *
         * @param value
         * @return
         * @throws E the e
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
         *
         * @param value
         * @return
         * @throws E the e
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
         *
         * @param value
         * @return
         * @throws E the e
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
         *
         * @param value
         * @return
         * @throws E the e
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
         *
         * @param t
         * @throws E the e
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
         *
         * @param value
         * @return
         * @throws E the e
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
         *
         * @param value
         * @return
         * @throws E the e
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
         *
         * @param t
         * @throws E the e
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
         *
         * @param value
         * @return
         * @throws E the e
         */
        boolean test(double value) throws E;
    }

    /**
     * The Interface DoubleFunction.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface DoubleFunction<R, E extends Throwable> {

        /**
         *
         * @param value
         * @return
         * @throws E the e
         */
        R apply(double value) throws E;
    }

    /**
     * The Interface DoubleToIntFunction.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface DoubleToIntFunction<E extends Throwable> {

        /**
         *
         * @param value
         * @return
         * @throws E the e
         */
        int applyAsInt(double value) throws E;
    }

    /**
     * The Interface DoubleToLongFunction.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface DoubleToLongFunction<E extends Throwable> {

        /**
         *
         * @param value
         * @return
         * @throws E the e
         */
        long applyAsLong(double value) throws E;
    }

    /**
     * The Interface ToBooleanFunction.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface ToBooleanFunction<T, E extends Throwable> {

        /**
         * Apply as boolean.
         *
         * @param t
         * @return
         * @throws E the e
         */
        boolean applyAsBoolean(T t) throws E;
    }

    /**
     * The Interface ToCharFunction.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface ToCharFunction<T, E extends Throwable> {

        /**
         * Apply as char.
         *
         * @param t
         * @return
         * @throws E the e
         */
        char applyAsChar(T t) throws E;
    }

    /**
     * The Interface ToByteFunction.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface ToByteFunction<T, E extends Throwable> {

        /**
         * Apply as byte.
         *
         * @param t
         * @return
         * @throws E the e
         */
        byte applyAsByte(T t) throws E;
    }

    /**
     * The Interface ToShortFunction.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface ToShortFunction<T, E extends Throwable> {

        /**
         * Apply as short.
         *
         * @param t
         * @return
         * @throws E the e
         */
        short applyAsShort(T t) throws E;
    }

    /**
     * The Interface ToIntFunction.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface ToIntFunction<T, E extends Throwable> {

        /**
         * Apply as int.
         *
         * @param t
         * @return
         * @throws E the e
         */
        int applyAsInt(T t) throws E;
    }

    /**
     * The Interface ToLongFunction.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface ToLongFunction<T, E extends Throwable> {

        /**
         * Apply as long.
         *
         * @param t
         * @return
         * @throws E the e
         */
        long applyAsLong(T t) throws E;
    }

    /**
     * The Interface ToFloatFunction.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface ToFloatFunction<T, E extends Throwable> {

        /**
         * Apply as float.
         *
         * @param t
         * @return
         * @throws E the e
         */
        float applyAsFloat(T t) throws E;
    }

    /**
     * The Interface ToDoubleFunction.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface ToDoubleFunction<T, E extends Throwable> {

        /**
         * Apply as double.
         *
         * @param t
         * @return
         * @throws E the e
         */
        double applyAsDouble(T t) throws E;
    }

    /**
     * The Interface TriIntFunction.
     *
     * @param <A>
     * @param <B>
     * @param <E>
     */
    @FunctionalInterface
    public interface ToIntBiFunction<A, B, E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @return
         * @throws E the e
         */
        int applyAsInt(A a, B b) throws E;
    }

    /**
     * The Interface TriLongFunction.
     *
     * @param <A>
     * @param <B>
     * @param <E>
     */
    @FunctionalInterface
    public interface ToLongBiFunction<A, B, E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @return
         * @throws E the e
         */
        long applyAsLong(A a, B b) throws E;
    }

    /**
     * The Interface TriDoubleFunction.
     *
     * @param <A>
     * @param <B>
     * @param <E>
     */
    @FunctionalInterface
    public interface ToDoubleBiFunction<A, B, E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @return
         * @throws E the e
         */
        double applyAsDouble(A a, B b) throws E;
    }

    /**
     * The Interface TriIntFunction.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <E>
     */
    @FunctionalInterface
    public interface ToIntTriFunction<A, B, C, E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        int applyAsInt(A a, B b, C c) throws E;
    }

    /**
     * The Interface TriLongFunction.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <E>
     */
    @FunctionalInterface
    public interface ToLongTriFunction<A, B, C, E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        long applyAsLong(A a, B b, C c) throws E;
    }

    /**
     * The Interface TriDoubleFunction.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <E>
     */
    @FunctionalInterface
    public interface ToDoubleTriFunction<A, B, C, E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        double applyAsDouble(A a, B b, C c) throws E;
    }

    /**
     * The Interface UnaryOperator.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface UnaryOperator<T, E extends Throwable> extends Function<T, T, E> {
    }

    /**
     * The Interface BinaryOperator.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface BinaryOperator<T, E extends Throwable> extends BiFunction<T, T, T, E> {
    }

    /**
     * The Interface TernaryOperator.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface TernaryOperator<T, E extends Throwable> {

        /**
         * Apply as char.
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        T apply(T a, T b, T c) throws E;
    }

    /**
     * The Interface BooleanUnaryOperator.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface BooleanUnaryOperator<E extends Throwable> {

        /**
         * Apply as boolean.
         *
         * @param operand
         * @return
         * @throws E the e
         */
        boolean applyAsBoolean(boolean operand) throws E;
    }

    /**
     * The Interface CharUnaryOperator.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface CharUnaryOperator<E extends Throwable> {

        /**
         * Apply as char.
         *
         * @param operand
         * @return
         * @throws E the e
         */
        char applyAsChar(char operand) throws E;
    }

    /**
     * The Interface ByteUnaryOperator.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface ByteUnaryOperator<E extends Throwable> {

        /**
         * Apply as byte.
         *
         * @param operand
         * @return
         * @throws E the e
         */
        byte applyAsByte(byte operand) throws E;
    }

    /**
     * The Interface ShortUnaryOperator.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface ShortUnaryOperator<E extends Throwable> {

        /**
         * Apply as short.
         *
         * @param operand
         * @return
         * @throws E the e
         */
        short applyAsShort(short operand) throws E;
    }

    /**
     * The Interface IntUnaryOperator.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface IntUnaryOperator<E extends Throwable> {

        /**
         * Apply as int.
         *
         * @param operand
         * @return
         * @throws E the e
         */
        int applyAsInt(int operand) throws E;
    }

    /**
     * The Interface IntObjOperator.
     *
     * @param <E>
     */
    @Beta
    @FunctionalInterface
    public interface IntObjOperator<T, E extends Throwable> {

        /**
         * Apply as int.
         *
         * @param operand
         * @param obj
         * @return
         * @throws E the e
         */
        int applyAsInt(int operand, T obj) throws E;
    }

    /**
     * The Interface LongUnaryOperator.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface LongUnaryOperator<E extends Throwable> {

        /**
         * Apply as long.
         *
         * @param operand
         * @return
         * @throws E the e
         */
        long applyAsLong(long operand) throws E;
    }

    /**
     * The Interface FloatUnaryOperator.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface FloatUnaryOperator<E extends Throwable> {

        /**
         * Apply as float.
         *
         * @param operand
         * @return
         * @throws E the e
         */
        float applyAsFloat(float operand) throws E;
    }

    /**
     * The Interface DoubleUnaryOperator.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface DoubleUnaryOperator<E extends Throwable> {

        /**
         * Apply as double.
         *
         * @param operand
         * @return
         * @throws E the e
         */
        double applyAsDouble(double operand) throws E;
    }

    /**
     * The Interface BooleanBinaryOperator.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface BooleanBinaryOperator<E extends Throwable> {

        /**
         * Apply as boolean.
         *
         * @param left
         * @param right
         * @return
         * @throws E the e
         */
        boolean applyAsBoolean(boolean left, boolean right) throws E;
    }

    /**
     * The Interface CharBinaryOperator.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface CharBinaryOperator<E extends Throwable> {

        /**
         * Apply as char.
         *
         * @param left
         * @param right
         * @return
         * @throws E the e
         */
        char applyAsChar(char left, char right) throws E;
    }

    /**
     * The Interface ByteBinaryOperator.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface ByteBinaryOperator<E extends Throwable> {

        /**
         * Apply as byte.
         *
         * @param left
         * @param right
         * @return
         * @throws E the e
         */
        byte applyAsByte(byte left, byte right) throws E;
    }

    /**
     * The Interface ShortBinaryOperator.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface ShortBinaryOperator<E extends Throwable> {

        /**
         * Apply as short.
         *
         * @param left
         * @param right
         * @return
         * @throws E the e
         */
        short applyAsShort(short left, short right) throws E;
    }

    /**
     * The Interface IntBinaryOperator.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface IntBinaryOperator<E extends Throwable> {

        /**
         * Apply as int.
         *
         * @param left
         * @param right
         * @return
         * @throws E the e
         */
        int applyAsInt(int left, int right) throws E;
    }

    /**
     * The Interface LongBinaryOperator.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface LongBinaryOperator<E extends Throwable> {

        /**
         * Apply as long.
         *
         * @param left
         * @param right
         * @return
         * @throws E the e
         */
        long applyAsLong(long left, long right) throws E;
    }

    /**
     * The Interface FloatBinaryOperator.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface FloatBinaryOperator<E extends Throwable> {

        /**
         * Apply as float.
         *
         * @param left
         * @param right
         * @return
         * @throws E the e
         */
        float applyAsFloat(float left, float right) throws E;
    }

    /**
     * The Interface DoubleBinaryOperator.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface DoubleBinaryOperator<E extends Throwable> {

        /**
         * Apply as double.
         *
         * @param left
         * @param right
         * @return
         * @throws E the e
         */
        double applyAsDouble(double left, double right) throws E;
    }

    /**
     * The Interface BooleanTernaryOperator.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface BooleanTernaryOperator<E extends Throwable> {

        /**
         * Apply as boolean.
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        boolean applyAsBoolean(boolean a, boolean b, boolean c) throws E;
    }

    /**
     * The Interface CharTernaryOperator.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface CharTernaryOperator<E extends Throwable> {

        /**
         * Apply as char.
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        char applyAsChar(char a, char b, char c) throws E;
    }

    /**
     * The Interface ByteTernaryOperator.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface ByteTernaryOperator<E extends Throwable> {

        /**
         * Apply as byte.
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        byte applyAsByte(byte a, byte b, byte c) throws E;
    }

    /**
     * The Interface ShortTernaryOperator.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface ShortTernaryOperator<E extends Throwable> {

        /**
         * Apply as short.
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        short applyAsShort(short a, short b, short c) throws E;
    }

    /**
     * The Interface IntTernaryOperator.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface IntTernaryOperator<E extends Throwable> {

        /**
         * Apply as int.
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        int applyAsInt(int a, int b, int c) throws E;
    }

    /**
     * The Interface LongTernaryOperator.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface LongTernaryOperator<E extends Throwable> {

        /**
         * Apply as long.
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        long applyAsLong(long a, long b, long c) throws E;
    }

    /**
     * The Interface FloatTernaryOperator.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface FloatTernaryOperator<E extends Throwable> {

        /**
         * Apply as float.
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        float applyAsFloat(float a, float b, float c) throws E;
    }

    /**
     * The Interface DoubleTernaryOperator.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface DoubleTernaryOperator<E extends Throwable> {

        /**
         * Apply as double.
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        double applyAsDouble(double a, double b, double c) throws E;
    }

    /**
     * The Interface BooleanBiPredicate.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface BooleanBiPredicate<E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        boolean test(boolean t, boolean u) throws E;
    }

    /**
     * The Interface CharBiPredicate.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface CharBiPredicate<E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        boolean test(char t, char u) throws E;
    }

    /**
     * The Interface ByteBiPredicate.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface ByteBiPredicate<E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        boolean test(byte t, byte u) throws E;
    }

    /**
     * The Interface ShortBiPredicate.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface ShortBiPredicate<E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        boolean test(short t, short u) throws E;
    }

    /**
     * The Interface IntBiPredicate.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface IntBiPredicate<E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        boolean test(int t, int u) throws E;
    }

    /**
     * The Interface LongBiPredicate.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface LongBiPredicate<E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        boolean test(long t, long u) throws E;
    }

    /**
     * The Interface FloatBiPredicate.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface FloatBiPredicate<E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        boolean test(float t, float u) throws E;
    }

    /**
     * The Interface DoubleBiPredicate.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface DoubleBiPredicate<E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        boolean test(double t, double u) throws E;
    }

    /**
     * The Interface BooleanBiFunction.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface BooleanBiFunction<R, E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        R apply(boolean t, boolean u) throws E;
    }

    /**
     * The Interface CharBiFunction.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface CharBiFunction<R, E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        R apply(char t, char u) throws E;
    }

    /**
     * The Interface ByteBiFunction.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface ByteBiFunction<R, E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        R apply(byte t, byte u) throws E;
    }

    /**
     * The Interface ShortBiFunction.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface ShortBiFunction<R, E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        R apply(short t, short u) throws E;
    }

    /**
     * The Interface IntBiFunction.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface IntBiFunction<R, E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        R apply(int t, int u) throws E;
    }

    /**
     * The Interface LongBiFunction.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface LongBiFunction<R, E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        R apply(long t, long u) throws E;
    }

    /**
     * The Interface FloatBiFunction.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface FloatBiFunction<R, E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        R apply(float t, float u) throws E;
    }

    /**
     * The Interface DoubleBiFunction.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface DoubleBiFunction<R, E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        R apply(double t, double u) throws E;
    }

    /**
     * The Interface BooleanBiConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface BooleanBiConsumer<E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @throws E the e
         */
        void accept(boolean t, boolean u) throws E;
    }

    /**
     * The Interface CharBiConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface CharBiConsumer<E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @throws E the e
         */
        void accept(char t, char u) throws E;
    }

    /**
     * The Interface ByteBiConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface ByteBiConsumer<E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @throws E the e
         */
        void accept(byte t, byte u) throws E;
    }

    /**
     * The Interface ShortBiConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface ShortBiConsumer<E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @throws E the e
         */
        void accept(short t, short u) throws E;
    }

    /**
     * The Interface IntBiConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface IntBiConsumer<E extends Throwable> extends IntIntConsumer<E> {

        /**
         *
         * @param t
         * @param u
         * @throws E the e
         */
        @Override
        void accept(int t, int u) throws E;
    }

    /**
     * The Interface LongBiConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface LongBiConsumer<E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @throws E the e
         */
        void accept(long t, long u) throws E;
    }

    /**
     * The Interface FloatBiConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface FloatBiConsumer<E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @throws E the e
         */
        void accept(float t, float u) throws E;
    }

    /**
     * The Interface DoubleBiConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface DoubleBiConsumer<E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @throws E the e
         */
        void accept(double t, double u) throws E;
    }

    /**
     * The Interface BooleanTriPredicate.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface BooleanTriPredicate<E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        boolean test(boolean a, boolean b, boolean c) throws E;
    }

    /**
     * The Interface CharTriPredicate.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface CharTriPredicate<E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        boolean test(char a, char b, char c) throws E;
    }

    /**
     * The Interface ByteTriPredicate.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface ByteTriPredicate<E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        boolean test(byte a, byte b, byte c) throws E;
    }

    /**
     * The Interface ShortTriPredicate.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface ShortTriPredicate<E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        boolean test(short a, short b, short c) throws E;
    }

    /**
     * The Interface IntTriPredicate.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface IntTriPredicate<E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        boolean test(int a, int b, int c) throws E;
    }

    /**
     * The Interface LongTriPredicate.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface LongTriPredicate<E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        boolean test(long a, long b, long c) throws E;
    }

    /**
     * The Interface FloatTriPredicate.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface FloatTriPredicate<E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        boolean test(float a, float b, float c) throws E;
    }

    /**
     * The Interface DoubleTriPredicate.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface DoubleTriPredicate<E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        boolean test(double a, double b, double c) throws E;
    }

    /**
     * The Interface BooleanTriFunction.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface BooleanTriFunction<R, E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        R apply(boolean a, boolean b, boolean c) throws E;
    }

    /**
     * The Interface CharTriFunction.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface CharTriFunction<R, E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        R apply(char a, char b, char c) throws E;
    }

    /**
     * The Interface ByteTriFunction.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface ByteTriFunction<R, E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        R apply(byte a, byte b, byte c) throws E;
    }

    /**
     * The Interface ShortTriFunction.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface ShortTriFunction<R, E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        R apply(short a, short b, short c) throws E;
    }

    /**
     * The Interface IntTriFunction.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface IntTriFunction<R, E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        R apply(int a, int b, int c) throws E;
    }

    /**
     * The Interface LongTriFunction.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface LongTriFunction<R, E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        R apply(long a, long b, long c) throws E;
    }

    /**
     * The Interface FloatTriFunction.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface FloatTriFunction<R, E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        R apply(float a, float b, float c) throws E;
    }

    /**
     * The Interface DoubleTriFunction.
     *
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface DoubleTriFunction<R, E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        R apply(double a, double b, double c) throws E;
    }

    /**
     * The Interface BooleanTriConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface BooleanTriConsumer<E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @throws E the e
         */
        void accept(boolean a, boolean b, boolean c) throws E;
    }

    /**
     * The Interface CharTriConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface CharTriConsumer<E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @throws E the e
         */
        void accept(char a, char b, char c) throws E;
    }

    /**
     * The Interface ByteTriConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface ByteTriConsumer<E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @throws E the e
         */
        void accept(byte a, byte b, byte c) throws E;
    }

    /**
     * The Interface ShortTriConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface ShortTriConsumer<E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @throws E the e
         */
        void accept(short a, short b, short c) throws E;
    }

    /**
     * The Interface IntTriConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface IntTriConsumer<E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @throws E the e
         */
        void accept(int a, int b, int c) throws E;
    }

    /**
     * The Interface LongTriConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface LongTriConsumer<E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @throws E the e
         */
        void accept(long a, long b, long c) throws E;
    }

    /**
     * The Interface FloatTriConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface FloatTriConsumer<E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @throws E the e
         */
        void accept(float a, float b, float c) throws E;
    }

    /**
     * The Interface DoubleTriConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface DoubleTriConsumer<E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @throws E the e
         */
        void accept(double a, double b, double c) throws E;
    }

    /**
     * The Interface ObjBooleanConsumer.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface ObjBooleanConsumer<T, E extends Throwable> {

        /**
         *
         * @param t
         * @param value
         * @throws E the e
         */
        void accept(T t, boolean value) throws E;
    }

    /**
     * The Interface ObjCharConsumer.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface ObjCharConsumer<T, E extends Throwable> {

        /**
         *
         * @param t
         * @param value
         * @throws E the e
         */
        void accept(T t, char value) throws E;
    }

    /**
     * The Interface ObjByteConsumer.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface ObjByteConsumer<T, E extends Throwable> {

        /**
         *
         * @param t
         * @param value
         * @throws E the e
         */
        void accept(T t, byte value) throws E;
    }

    /**
     * The Interface ObjShortConsumer.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface ObjShortConsumer<T, E extends Throwable> {

        /**
         *
         * @param t
         * @param value
         * @throws E the e
         */
        void accept(T t, short value) throws E;
    }

    /**
     * The Interface ObjIntConsumer.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface ObjIntConsumer<T, E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @throws E the e
         */
        void accept(T t, int u) throws E;
    }

    /**
     * The Interface ObjIntConsumer.
     *
     * @param <T>
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface ObjIntFunction<T, R, E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        R apply(T t, int u) throws E;
    }

    /**
     * The Interface ObjIntPredicate.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface ObjIntPredicate<T, E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        boolean test(T t, int u) throws E;
    }

    /**
     * The Interface ObjLongConsumer.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface ObjLongConsumer<T, E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @throws E the e
         */
        void accept(T t, long u) throws E;
    }

    /**
     * The Interface ObjLongFunction.
     *
     * @param <T>
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface ObjLongFunction<T, R, E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        R apply(T t, long u) throws E;
    }

    /**
     * The Interface ObjLongPredicate.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface ObjLongPredicate<T, E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        boolean test(T t, long u) throws E;
    }

    /**
     * The Interface ObjFloatConsumer.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface ObjFloatConsumer<T, E extends Throwable> {

        /**
         *
         * @param t
         * @param value
         * @throws E the e
         */
        void accept(T t, float value) throws E;
    }

    /**
     * The Interface ObjDoubleConsumer.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface ObjDoubleConsumer<T, E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @throws E the e
         */
        void accept(T t, double u) throws E;
    }

    /**
     * The Interface ObjDoubleFunction.
     *
     * @param <T>
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface ObjDoubleFunction<T, R, E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        R apply(T t, double u) throws E;
    }

    /**
     * The Interface ObjDoublePredicate.
     *
     * @param <T>
     * @param <E>
     */
    @FunctionalInterface
    public interface ObjDoublePredicate<T, E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        boolean test(T t, double u) throws E;
    }

    @FunctionalInterface
    public interface ObjBiIntConsumer<T, E extends Throwable> {

        /**
         *
         * @param t
         * @param i
         * @param j
         * @throws E
         */
        void accept(T t, int i, int j) throws E;
    }

    @FunctionalInterface
    public interface ObjBiIntFunction<T, R, E extends Throwable> {

        /**
         *
         * @param t
         * @param i
         * @param j
         * @return
         * @throws E
         */
        R apply(T t, int i, int j) throws E;
    }

    @FunctionalInterface
    public interface ObjBiIntPredicate<T, E extends Throwable> {

        /**
         *
         * @param t
         * @param i
         * @param j
         * @return
         * @throws E
         */
        boolean test(T t, int i, int j) throws E;
    }

    /**
     * The Interface IndexedBiConsumer.
     *
     * @param <T>
     * @param <U>
     * @param <E>
     */
    @FunctionalInterface
    public interface BiObjIntConsumer<T, U, E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @param i
         * @throws E the e
         */
        void accept(T t, U u, int i) throws E;
    }

    /**
     * The Interface IndexedBiFunction.
     *
     * @param <T>
     * @param <U>
     * @param <R>
     * @param <E>
     */
    @FunctionalInterface
    public interface BiObjIntFunction<T, U, R, E extends Throwable> {

        /**
         *
         * @param e
         * @param u
         * @param i
         * @return
         * @throws E the e
         */
        R apply(T e, U u, int i) throws E;
    }

    /**
     * The Interface IndexedBiPredicate.
     *
     * @param <T>
     * @param <U>
     * @param <E>
     */
    @FunctionalInterface
    public interface BiObjIntPredicate<T, U, E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @param i
         * @return
         * @throws E the e
         */
        boolean test(T t, U u, int i) throws E;
    }

    @FunctionalInterface
    public interface IntObjConsumer<T, E extends Throwable> {

        /**
         *
         * @param <T>
         * @param <E>
         * @param consumer
         * @return
         */
        static <T, E extends Throwable> IntObjConsumer<T, E> of(final IntObjConsumer<T, E> consumer) {
            return consumer;
        }

        /**
         *
         * @param i
         * @param t
         * @throws E
         */
        void accept(int i, T t) throws E;
    }

    @FunctionalInterface
    public interface IntObjFunction<T, R, E extends Throwable> {

        /**
         *
         * @param <T>
         * @param <R>
         * @param <E>
         * @param func
         * @return
         */
        static <T, R, E extends Throwable> IntObjFunction<T, R, E> of(final IntObjFunction<T, R, E> func) {
            return func;
        }

        /**
         *
         * @param i
         * @param t
         * @return
         * @throws E
         */
        R apply(int i, T t) throws E;
    }

    @FunctionalInterface
    public interface IntObjPredicate<T, E extends Throwable> {

        /**
         *
         * @param <T>
         * @param <E>
         * @param predicate
         * @return
         */
        static <T, E extends Throwable> IntObjPredicate<T, E> of(final IntObjPredicate<T, E> predicate) {
            return predicate;
        }

        /**
         *
         * @param i
         * @param t
         * @return
         * @throws E
         */
        boolean test(int i, T t) throws E;
    }

    @FunctionalInterface
    public interface IntBiObjConsumer<T, U, E extends Throwable> {
        /**
         *
         * @param i
         * @param t
         * @param u
         * @throws E
         */
        void accept(int i, T t, U u) throws E;
    }

    @FunctionalInterface
    public interface IntBiObjFunction<T, U, R, E extends Throwable> {

        /**
         *
         * @param i
         * @param t
         * @param u
         * @return
         * @throws E
         */
        R apply(int i, T t, U u) throws E;
    }

    @FunctionalInterface
    public interface IntBiObjPredicate<T, U, E extends Throwable> {

        /**
         *
         * @param i
         * @param t
         * @param u
         * @return
         * @throws E
         */
        boolean test(int i, T t, U u) throws E;
    }

    @FunctionalInterface
    public interface BiIntObjConsumer<T, E extends Throwable> {

        /**
         *
         * @param i
         * @param j
         * @param t
         * @throws E
         */
        void accept(int i, int j, T t) throws E;
    }

    @FunctionalInterface
    public interface BiIntObjFunction<T, R, E extends Throwable> {

        /**
         *
         * @param i
         * @param j
         * @param t
         * @return
         * @throws E
         */
        R apply(int i, int j, T t) throws E;
    }

    @FunctionalInterface
    public interface BiIntObjPredicate<T, E extends Throwable> {

        /**
         *
         * @param i
         * @param j
         * @param t
         * @return
         * @throws E
         */
        boolean test(int i, int j, T t) throws E;
    }

    @FunctionalInterface
    public interface LongObjConsumer<T, E extends Throwable> {
        /**
         *
         * @param i
         * @param t
         * @throws E
         */
        void accept(long i, T t) throws E;
    }

    @FunctionalInterface
    public interface LongObjFunction<T, R, E extends Throwable> {
        /**
         *
         * @param i
         * @param t
         * @return
         * @throws E
         */
        R apply(long i, T t) throws E;
    }

    @FunctionalInterface
    public interface LongObjPredicate<T, E extends Throwable> {
        /**
         *
         * @param i
         * @param t
         * @return
         * @throws E
         */
        boolean test(long i, T t) throws E;
    }

    @FunctionalInterface
    public interface DoubleObjConsumer<T, E extends Throwable> {
        /**
         *
         * @param i
         * @param t
         * @throws E
         */
        void accept(double i, T t) throws E;
    }

    @FunctionalInterface
    public interface DoubleObjFunction<T, R, E extends Throwable> {
        /**
         *
         * @param i
         * @param t
         * @return
         * @throws E
         */
        R apply(double i, T t) throws E;
    }

    @FunctionalInterface
    public interface DoubleObjPredicate<T, E extends Throwable> {
        /**
         *
         * @param i
         * @param t
         * @return
         * @throws E
         */
        boolean test(double i, T t) throws E;
    }

    @FunctionalInterface
    public interface BooleanNFunction<R, E extends Throwable> {

        /**
         *
         * @param args
         * @return
         * @throws E
         */
        R apply(boolean... args) throws E;

        /**
         *
         * @param <V>
         * @param after
         * @return
         */
        default <V> BooleanNFunction<V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) {
            return args -> after.apply(apply(args));
        }
    }

    @FunctionalInterface
    public interface CharNFunction<R, E extends Throwable> {

        /**
         *
         * @param args
         * @return
         * @throws E
         */
        R apply(char... args) throws E;

        /**
         *
         * @param <V>
         * @param after
         * @return
         */
        default <V> CharNFunction<V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) {
            return args -> after.apply(apply(args));
        }
    }

    @FunctionalInterface
    public interface ByteNFunction<R, E extends Throwable> {

        /**
         *
         * @param args
         * @return
         * @throws E
         */
        R apply(byte... args) throws E;

        /**
         *
         * @param <V>
         * @param after
         * @return
         */
        default <V> ByteNFunction<V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) {
            return args -> after.apply(apply(args));
        }
    }

    @FunctionalInterface
    public interface ShortNFunction<R, E extends Throwable> {
        /**
         *
         * @param args
         * @return
         * @throws E
         */
        R apply(short... args) throws E;

        /**
         *
         * @param <V>
         * @param after
         * @return
         */
        default <V> ShortNFunction<V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) {
            return args -> after.apply(apply(args));
        }
    }

    @FunctionalInterface
    public interface IntNFunction<R, E extends Throwable> {

        /**
         *
         * @param args
         * @return
         * @throws E
         */
        R apply(int... args) throws E;

        /**
         *
         * @param <V>
         * @param after
         * @return
         */
        default <V> IntNFunction<V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) {
            return args -> after.apply(apply(args));
        }
    }

    @FunctionalInterface
    public interface LongNFunction<R, E extends Throwable> {

        /**
         *
         * @param args
         * @return
         * @throws E
         */
        R apply(long... args) throws E;

        /**
         *
         * @param <V>
         * @param after
         * @return
         */
        default <V> LongNFunction<V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) {
            return args -> after.apply(apply(args));
        }
    }

    @FunctionalInterface
    public interface FloatNFunction<R, E extends Throwable> {

        /**
         *
         * @param args
         * @return
         * @throws E
         */
        R apply(float... args) throws E;

        /**
         *
         * @param <V>
         * @param after
         * @return
         */
        default <V> FloatNFunction<V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) {
            return args -> after.apply(apply(args));
        }
    }

    @FunctionalInterface
    public interface DoubleNFunction<R, E extends Throwable> {

        /**
         *
         * @param args
         * @return
         * @throws E
         */
        R apply(double... args) throws E;

        /**
         *
         * @param <V>
         * @param after
         * @return
         */
        default <V> DoubleNFunction<V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) {
            return args -> after.apply(apply(args));
        }
    }

    @FunctionalInterface
    public interface NFunction<T, R, E extends Throwable> {

        /**
         *
         * @param args
         * @return
         * @throws E
         */
        @SuppressWarnings("unchecked")
        R apply(T... args) throws E;

        /**
         *
         * @param <V>
         * @param after
         * @return
         */
        default <V> NFunction<T, V, E> andThen(final java.util.function.Function<? super R, ? extends V> after) {
            return args -> after.apply(apply(args));
        }
    }

    /**
     * The Interface IntBooleanConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface IntBooleanConsumer<E extends Throwable> {

        /**
         *
         * @param idx
         * @param e
         * @throws E the e
         */
        void accept(int idx, boolean e) throws E;
    }

    /**
     * The Interface IndexedCharConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface IntCharConsumer<E extends Throwable> {

        /**
         *
         * @param idx
         * @param e
         * @throws E the e
         */
        void accept(int idx, char e) throws E;
    }

    /**
     * The Interface IndexedByteConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface IntByteConsumer<E extends Throwable> {

        /**
         *
         * @param idx
         * @param e
         * @throws E the e
         */
        void accept(int idx, byte e) throws E;
    }

    /**
     * The Interface IndexedShortConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface IntShortConsumer<E extends Throwable> {

        /**
         *
         * @param idx
         * @param e
         * @throws E the e
         */
        void accept(int idx, short e) throws E;
    }

    /**
     * The Interface IndexedIntConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface IntIntConsumer<E extends Throwable> {

        /**
         *
         * @param idx
         * @param e
         * @throws E the e
         */
        void accept(int idx, int e) throws E;
    }

    /**
     * The Interface IndexedLongConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface IntLongConsumer<E extends Throwable> {

        /**
         *
         * @param idx
         * @param e
         * @throws E the e
         */
        void accept(int idx, long e) throws E;
    }

    /**
     * The Interface IndexedFloatConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface IntFloatConsumer<E extends Throwable> {

        /**
         *
         * @param idx
         * @param e
         * @throws E the e
         */
        void accept(int idx, float e) throws E;
    }

    /**
     * The Interface IndexedDoubleConsumer.
     *
     * @param <E>
     */
    @FunctionalInterface
    public interface IntDoubleConsumer<E extends Throwable> {

        /**
         *
         * @param idx
         * @param e
         * @throws E the e
         */
        void accept(int idx, double e) throws E;
    }

    public static final class EE {
        private EE() {
            // Singleton. Utility class.
        }

        @FunctionalInterface
        public interface Runnable<E extends Throwable, E2 extends Throwable> {

            /**
             *
             *
             * @throws E
             * @throws E2
             */
            void run() throws E, E2;
        }

        @FunctionalInterface
        public interface Callable<R, E extends Throwable, E2 extends Throwable> {

            /**
             *
             *
             * @return
             * @throws E
             * @throws E2
             */
            R call() throws E, E2;
        }

        @FunctionalInterface
        public interface Supplier<T, E extends Throwable, E2 extends Throwable> {

            /**
             *
             *
             * @return
             * @throws E
             * @throws E2
             */
            T get() throws E, E2;
        }

        @FunctionalInterface
        public interface Predicate<T, E extends Throwable, E2 extends Throwable> {

            /**
             *
             *
             * @param t
             * @return
             * @throws E
             * @throws E2
             */
            boolean test(T t) throws E, E2;
        }

        @FunctionalInterface
        public interface BiPredicate<T, U, E extends Throwable, E2 extends Throwable> {

            /**
             *
             *
             * @param t
             * @param u
             * @return
             * @throws E
             * @throws E2
             */
            boolean test(T t, U u) throws E, E2;
        }

        @FunctionalInterface
        public interface TriPredicate<A, B, C, E extends Throwable, E2 extends Throwable> {

            /**
             *
             *
             * @param a
             * @param b
             * @param c
             * @return
             * @throws E
             * @throws E2
             */
            boolean test(A a, B b, C c) throws E, E2;
        }

        @FunctionalInterface
        public interface Function<T, R, E extends Throwable, E2 extends Throwable> {

            /**
             *
             *
             * @param t
             * @return
             * @throws E
             * @throws E2
             */
            R apply(T t) throws E, E2;
        }

        @FunctionalInterface
        public interface BiFunction<T, U, R, E extends Throwable, E2 extends Throwable> {

            /**
             *
             *
             * @param t
             * @param u
             * @return
             * @throws E
             * @throws E2
             */
            R apply(T t, U u) throws E, E2;
        }

        @FunctionalInterface
        public interface TriFunction<A, B, C, R, E extends Throwable, E2 extends Throwable> {

            /**
             *
             *
             * @param a
             * @param b
             * @param c
             * @return
             * @throws E
             * @throws E2
             */
            R apply(A a, B b, C c) throws E, E2;
        }

        @FunctionalInterface
        public interface Consumer<T, E extends Throwable, E2 extends Throwable> {

            /**
             *
             *
             * @param t
             * @throws E
             * @throws E2
             */
            void accept(T t) throws E, E2;
        }

        @FunctionalInterface
        public interface BiConsumer<T, U, E extends Throwable, E2 extends Throwable> {

            /**
             *
             *
             * @param t
             * @param u
             * @throws E
             * @throws E2
             */
            void accept(T t, U u) throws E, E2;
        }

        @FunctionalInterface
        public interface TriConsumer<A, B, C, E extends Throwable, E2 extends Throwable> {

            /**
             *
             *
             * @param a
             * @param b
             * @param c
             * @throws E
             * @throws E2
             */
            void accept(A a, B b, C c) throws E, E2;
        }
    }

    /**
     * The Class EEE.
     */
    public static final class EEE {

        private EEE() {
            // Singleton. Utility class.
        }

        @FunctionalInterface
        public interface Runnable<E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             *
             *
             * @throws E
             * @throws E2
             * @throws E3
             */
            void run() throws E, E2, E3;
        }

        @FunctionalInterface
        public interface Callable<R, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             *
             *
             * @return
             * @throws E
             * @throws E2
             * @throws E3
             */
            R call() throws E, E2, E3;
        }

        @FunctionalInterface
        public interface Supplier<T, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             *
             *
             * @return
             * @throws E
             * @throws E2
             * @throws E3
             */
            T get() throws E, E2, E3;
        }

        @FunctionalInterface
        public interface Predicate<T, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             *
             *
             * @param t
             * @return
             * @throws E
             * @throws E2
             * @throws E3
             */
            boolean test(T t) throws E, E2, E3;
        }

        @FunctionalInterface
        public interface BiPredicate<T, U, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             *
             *
             * @param t
             * @param u
             * @return
             * @throws E
             * @throws E2
             * @throws E3
             */
            boolean test(T t, U u) throws E, E2, E3;
        }

        @FunctionalInterface
        public interface TriPredicate<A, B, C, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             *
             *
             * @param a
             * @param b
             * @param c
             * @return
             * @throws E
             * @throws E2
             * @throws E3
             */
            boolean test(A a, B b, C c) throws E, E2, E3;
        }

        @FunctionalInterface
        public interface Function<T, R, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             *
             *
             * @param t
             * @return
             * @throws E
             * @throws E2
             * @throws E3
             */
            R apply(T t) throws E, E2, E3;
        }

        @FunctionalInterface
        public interface BiFunction<T, U, R, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             *
             *
             * @param t
             * @param u
             * @return
             * @throws E
             * @throws E2
             * @throws E3
             */
            R apply(T t, U u) throws E, E2, E3;
        }

        @FunctionalInterface
        public interface TriFunction<A, B, C, R, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             *
             *
             * @param a
             * @param b
             * @param c
             * @return
             * @throws E
             * @throws E2
             * @throws E3
             */
            R apply(A a, B b, C c) throws E, E2, E3;
        }

        @FunctionalInterface
        public interface Consumer<T, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             *
             *
             * @param t
             * @throws E
             * @throws E2
             * @throws E3
             */
            void accept(T t) throws E, E2, E3;
        }

        @FunctionalInterface
        public interface BiConsumer<T, U, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             *
             *
             * @param t
             * @param u
             * @throws E
             * @throws E2
             * @throws E3
             */
            void accept(T t, U u) throws E, E2, E3;
        }

        @FunctionalInterface
        public interface TriConsumer<A, B, C, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            /**
             *
             *
             * @param a
             * @param b
             * @param c
             * @throws E
             * @throws E2
             * @throws E3
             */
            void accept(A a, B b, C c) throws E, E2, E3;
        }
    }

    static final class LazyInitializer<T, E extends Throwable> implements Throwables.Supplier<T, E> {
        private final Supplier<T, E> supplier;
        private volatile boolean initialized = false;
        private volatile T value = null; //NOSONAR

        LazyInitializer(final Throwables.Supplier<T, E> supplier) {
            N.checkArgNotNull(supplier, cs.supplier);

            this.supplier = supplier;
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param supplier
         * @return
         */
        public static <T, E extends Throwable> LazyInitializer<T, E> of(final Throwables.Supplier<T, E> supplier) {
            N.checkArgNotNull(supplier);

            if (supplier instanceof LazyInitializer) {
                return (LazyInitializer<T, E>) supplier;
            }

            return new LazyInitializer<>(supplier);
        }

        /**
         *
         * @return
         * @throws E
         */
        @Override
        public T get() throws E {
            if (!initialized) {
                synchronized (this) {
                    if (!initialized) {
                        value = supplier.get();

                        initialized = true;
                    }

                }
            }

            return value;
        }
    }
}
