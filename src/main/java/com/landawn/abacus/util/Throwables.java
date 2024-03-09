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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.u.Nullable;

/**
 * Catch checked exception and convert it to <code>RuntimeException</code>.
 *
 * @author Haiyang Li
 * @param <T>
 */
@SuppressWarnings({ "java:S6539" })
public final class Throwables {

    private Throwables() {
        // Singleton for utility class.
    }

    /**
     *
     * @param cmd
     * @throws RuntimeException if some error happens
     */
    public static void run(final Throwables.Runnable<? extends Throwable> cmd) {
        try {
            cmd.run();
        } catch (Throwable e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param cmd
     * @param actionOnError
     */
    public static void run(final Throwables.Runnable<? extends Throwable> cmd, final java.util.function.Consumer<? super Throwable> actionOnError) {
        N.checkArgNotNull(actionOnError);

        try {
            cmd.run();
        } catch (Throwable e) {
            actionOnError.accept(e);
        }
    }

    /**
     *
     * @param <R>
     * @param cmd
     * @return
     * @throws RuntimeException if some error happens
     */
    public static <R> R call(final Throwables.Callable<R, ? extends Throwable> cmd) {
        try {
            return cmd.call();
        } catch (Throwable e) {
            throw ExceptionUtil.toRuntimeException(e, true);
        }
    }

    /**
     *
     * @param <R>
     * @param cmd
     * @param actionOnError
     * @return
     */
    public static <R> R call(final Throwables.Callable<R, ? extends Throwable> cmd,
            final java.util.function.Function<? super Throwable, ? extends R> actionOnError) {
        N.checkArgNotNull(actionOnError);

        try {
            return cmd.call();
        } catch (Throwable e) {
            return actionOnError.apply(e);
        }
    }

    /**
     *
     * @param <R>
     * @param cmd
     * @param supplier
     * @return
     */
    public static <R> R call(final Throwables.Callable<R, ? extends Throwable> cmd, final java.util.function.Supplier<R> supplier) {
        N.checkArgNotNull(supplier);

        try {
            return cmd.call();
        } catch (Throwable e) {
            return supplier.get();
        }
    }

    /**
     *
     * @param <R>
     * @param cmd
     * @param defaultValue
     * @return
     */
    public static <R> R call(final Throwables.Callable<R, ? extends Throwable> cmd, final R defaultValue) {
        try {
            return cmd.call();
        } catch (Throwable e) {
            return defaultValue;
        }
    }

    /**
     *
     * @param <R>
     * @param cmd
     * @param predicate
     * @param supplier
     * @return
     * @throws RuntimeException if some error happens and <code>predicate</code> return false.
     */
    public static <R> R call(final Throwables.Callable<R, ? extends Throwable> cmd, final java.util.function.Predicate<? super Throwable> predicate,
            final java.util.function.Supplier<R> supplier) {
        N.checkArgNotNull(predicate);
        N.checkArgNotNull(supplier);

        try {
            return cmd.call();
        } catch (Throwable e) {
            if (predicate.test(e)) {
                return supplier.get();
            } else {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }
    }

    /**
     *
     * @param <R>
     * @param cmd
     * @param predicate
     * @param defaultValue
     * @return
     * @throws RuntimeException if some error happens and <code>predicate</code> return false.
     */
    public static <R> R call(final Throwables.Callable<R, ? extends Throwable> cmd, final java.util.function.Predicate<? super Throwable> predicate,
            final R defaultValue) {
        N.checkArgNotNull(predicate);

        try {
            return cmd.call();
        } catch (Throwable e) {
            if (predicate.test(e)) {
                return defaultValue;
            } else {
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        }
    }

    @SuppressWarnings({ "java:S6548" })
    public abstract static class ObjIterator<T, E extends Throwable> implements Immutable {

        @SuppressWarnings("rawtypes")
        private static final ObjIterator EMPTY = new ObjIterator() {
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
         * @return
         */
        public static <T, E extends Throwable> ObjIterator<T, E> empty() {
            return EMPTY;
        }

        public static <T, E extends Throwable> ObjIterator<T, E> just(final T val) {
            return new ObjIterator<>() {
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

        public static <T, E extends Throwable> ObjIterator<T, E> of(final Iterable<? extends T> iterable) {
            if (iterable == null) {
                return empty();
            }

            final Iterator<? extends T> iter = iterable.iterator();

            return new ObjIterator<>() {
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

        public abstract boolean hasNext() throws E;

        public abstract T next() throws E;

        /**
         *
         *
         * @param predicate
         * @return
         */
        public ObjIterator<T, E> filter(final Throwables.Predicate<? super T, E> predicate) {
            final ObjIterator<T, E> iter = this;

            return new ObjIterator<>() {
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

        public <U> ObjIterator<U, E> map(final Throwables.Function<? super T, U, E> mapper) {
            final ObjIterator<T, E> iter = this;

            return new ObjIterator<>() {
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
         *
         * @return
         */
        public Nullable<T> first() throws E {
            if (hasNext()) {
                return Nullable.of(next());
            } else {
                return Nullable.<T> empty();
            }
        }

        /**
         *
         *
         * @return
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
         *
         * @return
         */
        public Nullable<T> last() throws E {
            if (hasNext()) {
                T next = next();

                while (hasNext()) {
                    next = next();
                }

                return Nullable.of(next);
            } else {
                return Nullable.<T> empty();
            }
        }

        /**
         *
         *
         * @return
         */
        public Object[] toArray() throws E {
            return toArray(N.EMPTY_OBJECT_ARRAY);
        }

        /**
         *
         *
         * @param <A>
         * @param a
         * @return
         */
        public <A> A[] toArray(A[] a) throws E {
            return toList().toArray(a);
        }

        /**
         *
         *
         * @return
         */
        public List<T> toList() throws E {
            final List<T> list = new ArrayList<>();

            while (hasNext()) {
                list.add(next());
            }

            return list;
        }

        public void forEachRemaining(java.util.function.Consumer<? super T> action) throws E { // NOSONAR
            N.checkArgNotNull(action);

            while (hasNext()) {
                action.accept(next());
            }
        }

        /**
         *
         * @param action
         * @throws E the e
         */
        public void foreachRemaining(Throwables.Consumer<? super T, E> action) throws E { // NOSONAR
            N.checkArgNotNull(action);

            while (hasNext()) {
                action.accept(next());
            }
        }

        /**
         *
         * @param action
         * @throws E the e
         */
        public void foreachIndexed(Throwables.IntObjConsumer<? super T, E> action) throws E {
            N.checkArgNotNull(action);

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
    public interface Runnable<E extends Throwable> {

        /**
         *
         * @throws E the e
         */
        void run() throws E;

        /**
         *
         *
         * @return
         */
        @Beta
        default com.landawn.abacus.util.function.Runnable unchecked() {
            return () -> {
                try {
                    run();
                } catch (Throwable e) {
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
    public interface Callable<R, E extends Throwable> {

        /**
         *
         * @return
         * @throws E the e
         */
        R call() throws E;

        /**
         *
         *
         * @return
         */
        @Beta
        default com.landawn.abacus.util.function.Callable<R> unchecked() {
            return () -> {
                try {
                    return call();
                } catch (Throwable e) {
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
    public interface Supplier<T, E extends Throwable> {

        /**
         *
         * @return
         * @throws E the e
         */
        T get() throws E;

        /**
         *
         *
         * @return
         */
        @Beta
        default com.landawn.abacus.util.function.Supplier<T> unchecked() {
            return () -> {
                try {
                    return get();
                } catch (Throwable e) {
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
    public interface BooleanSupplier<E extends Throwable> {

        /**
         * Gets the as boolean.
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
    public interface CharSupplier<E extends Throwable> {

        /**
         * Gets the as char.
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
    public interface ByteSupplier<E extends Throwable> {

        /**
         * Gets the as byte.
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
    public interface ShortSupplier<E extends Throwable> {

        /**
         * Gets the as short.
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
    public interface IntSupplier<E extends Throwable> {

        /**
         * Gets the as int.
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
    public interface LongSupplier<E extends Throwable> {

        /**
         * Gets the as long.
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
    public interface FloatSupplier<E extends Throwable> {

        /**
         * Gets the as float.
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
    public interface DoubleSupplier<E extends Throwable> {

        /**
         * Gets the as double.
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
    public interface Predicate<T, E extends Throwable> {

        /**
         *
         * @param t
         * @return
         * @throws E the e
         */
        boolean test(T t) throws E;

        /**
         *
         *
         * @return
         */
        default Predicate<T, E> negate() {
            return t -> !test(t);
        }

        /**
         *
         *
         * @return
         */
        @Beta
        default com.landawn.abacus.util.function.Predicate<T> unchecked() {
            return t -> {
                try {
                    return test(t);
                } catch (Throwable e) {
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
    public interface BiPredicate<T, U, E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        boolean test(T t, U u) throws E;

        /**
         *
         *
         * @return
         */
        @Beta
        default com.landawn.abacus.util.function.BiPredicate<T, U> unchecked() {
            return (t, u) -> {
                try {
                    return test(t, u);
                } catch (Throwable e) {
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
    public interface Function<T, R, E extends Throwable> {

        /**
         *
         * @param t
         * @return
         * @throws E the e
         */
        R apply(T t) throws E;

        /**
         *
         *
         * @return
         */
        @Beta
        default com.landawn.abacus.util.function.Function<T, R> unchecked() {
            return t -> {
                try {
                    return apply(t);
                } catch (Throwable e) {
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
    public interface BiFunction<T, U, R, E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        R apply(T t, U u) throws E;

        /**
         *
         *
         * @return
         */
        @Beta
        default com.landawn.abacus.util.function.BiFunction<T, U, R> unchecked() {
            return (t, u) -> {
                try {
                    return apply(t, u);
                } catch (Throwable e) {
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
    public interface Consumer<T, E extends Throwable> {

        /**
         *
         * @param t
         * @throws E the e
         */
        void accept(T t) throws E;

        /**
         *
         *
         * @return
         */
        @Beta
        default com.landawn.abacus.util.function.Consumer<T> unchecked() {
            return t -> {
                try {
                    accept(t);
                } catch (Throwable e) {
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
    public interface BiConsumer<T, U, E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @throws E the e
         */
        void accept(T t, U u) throws E;

        /**
         *
         *
         * @return
         */
        @Beta
        default com.landawn.abacus.util.function.BiConsumer<T, U> unchecked() {
            return (t, u) -> {
                try {
                    accept(t, u);
                } catch (Throwable e) {
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
    public interface UnaryOperator<T, E extends Throwable> extends Function<T, T, E> {
    }

    /**
     * The Interface BinaryOperator.
     *
     * @param <T>
     * @param <E>
     */
    public interface BinaryOperator<T, E extends Throwable> extends BiFunction<T, T, T, E> {
    }

    /**
     * The Interface TernaryOperator.
     *
     * @param <T>
     * @param <E>
     */
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
     * The Interface LongUnaryOperator.
     *
     * @param <E>
     */
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
    public interface IntBiConsumer<E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @throws E the e
         */
        void accept(int t, int u) throws E;
    }

    /**
     * The Interface LongBiConsumer.
     *
     * @param <E>
     */
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
     * @param <R>
     * @param <E>
     */
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
     * @param <R>
     * @param <E>
     */
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
     * @param <R>
     * @param <E>
     */
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

    public interface ObjBiIntFunction<T, R, E extends Throwable> {

        /**
         *
         * @param t
         * @param i
         * @param j
         * @throws E
         */
        R apply(T t, int i, int j) throws E;
    }

    public interface ObjBiIntPredicate<T, E extends Throwable> {

        /**
         *
         * @param t
         * @param i
         * @param j
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

    public interface IntObjConsumer<T, E extends Throwable> {
        static <T, E extends Throwable> IntObjConsumer<T, E> of(IntObjConsumer<T, E> consumer) {
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

    public interface IntObjFunction<T, R, E extends Throwable> {
        static <T, R, E extends Throwable> IntObjFunction<T, R, E> of(IntObjFunction<T, R, E> func) {
            return func;
        }

        R apply(int i, T t) throws E;
    }

    public interface IntObjPredicate<T, E extends Throwable> {
        static <T, E extends Throwable> IntObjPredicate<T, E> of(IntObjPredicate<T, E> predicate) {
            return predicate;
        }

        boolean test(int i, T t) throws E;
    }

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

    public interface IntBiObjFunction<T, U, R, E extends Throwable> {

        R apply(int i, T t, U u) throws E;
    }

    public interface IntBiObjPredicate<T, U, E extends Throwable> {

        boolean test(int i, T t, U u) throws E;
    }

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

    public interface BiIntObjFunction<T, R, E extends Throwable> {

        /**
         *
         * @param i
         * @param j
         * @param t
         * @throws E
         */
        R apply(int i, int j, T t) throws E;
    }

    public interface BiIntObjPredicate<T, E extends Throwable> {

        /**
         *
         * @param i
         * @param j
         * @param t
         * @throws E
         */
        boolean test(int i, int j, T t) throws E;
    }

    public interface LongObjConsumer<T, E extends Throwable> {
        /**
         *
         * @param i
         * @param t
         * @throws E
         */
        void accept(long i, T t) throws E;
    }

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

    public interface DoubleObjConsumer<T, E extends Throwable> {
        /**
         *
         * @param i
         * @param t
         * @throws E
         */
        void accept(double i, T t) throws E;
    }

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

    public interface BooleanNFunction<R, E extends Throwable> {

        /**
         *
         *
         * @param args
         * @return
         * @throws E
         */
        R apply(boolean... args) throws E;

        /**
         *
         *
         * @param <V>
         * @param after
         * @return
         */
        default <V> BooleanNFunction<V, E> andThen(java.util.function.Function<? super R, ? extends V> after) {
            N.checkArgNotNull(after);

            return args -> after.apply(apply(args));
        }
    }

    public interface CharNFunction<R, E extends Throwable> {

        /**
         *
         *
         * @param args
         * @return
         * @throws E
         */
        R apply(char... args) throws E;

        /**
         *
         *
         * @param <V>
         * @param after
         * @return
         */
        default <V> CharNFunction<V, E> andThen(java.util.function.Function<? super R, ? extends V> after) {
            N.checkArgNotNull(after);

            return args -> after.apply(apply(args));
        }
    }

    public interface ByteNFunction<R, E extends Throwable> {

        /**
         *
         *
         * @param args
         * @return
         * @throws E
         */
        R apply(byte... args) throws E;

        /**
         *
         *
         * @param <V>
         * @param after
         * @return
         */
        default <V> ByteNFunction<V, E> andThen(java.util.function.Function<? super R, ? extends V> after) {
            N.checkArgNotNull(after);

            return args -> after.apply(apply(args));
        }
    }

    public interface ShortNFunction<R, E extends Throwable> {

        /**
         *
         *
         * @param args
         * @return
         * @throws E
         */
        R apply(short... args) throws E;

        /**
         *
         *
         * @param <V>
         * @param after
         * @return
         */
        default <V> ShortNFunction<V, E> andThen(java.util.function.Function<? super R, ? extends V> after) {
            N.checkArgNotNull(after);

            return args -> after.apply(apply(args));
        }
    }

    public interface IntNFunction<R, E extends Throwable> {

        /**
         *
         *
         * @param args
         * @return
         * @throws E
         */
        R apply(int... args) throws E;

        /**
         *
         *
         * @param <V>
         * @param after
         * @return
         */
        default <V> IntNFunction<V, E> andThen(java.util.function.Function<? super R, ? extends V> after) {
            N.checkArgNotNull(after);

            return args -> after.apply(apply(args));
        }
    }

    public interface LongNFunction<R, E extends Throwable> {

        /**
         *
         *
         * @param args
         * @return
         * @throws E
         */
        R apply(long... args) throws E;

        /**
         *
         *
         * @param <V>
         * @param after
         * @return
         */
        default <V> LongNFunction<V, E> andThen(java.util.function.Function<? super R, ? extends V> after) {
            N.checkArgNotNull(after);

            return args -> after.apply(apply(args));
        }
    }

    public interface FloatNFunction<R, E extends Throwable> {

        /**
         *
         *
         * @param args
         * @return
         * @throws E
         */
        R apply(float... args) throws E;

        /**
         *
         *
         * @param <V>
         * @param after
         * @return
         */
        default <V> FloatNFunction<V, E> andThen(java.util.function.Function<? super R, ? extends V> after) {
            N.checkArgNotNull(after);

            return args -> after.apply(apply(args));
        }
    }

    public interface DoubleNFunction<R, E extends Throwable> {

        /**
         *
         *
         * @param args
         * @return
         * @throws E
         */
        R apply(double... args) throws E;

        /**
         *
         *
         * @param <V>
         * @param after
         * @return
         */
        default <V> DoubleNFunction<V, E> andThen(java.util.function.Function<? super R, ? extends V> after) {
            N.checkArgNotNull(after);

            return args -> after.apply(apply(args));
        }
    }

    public interface NFunction<T, R, E extends Throwable> {

        /**
         *
         *
         * @param args
         * @return
         * @throws E
         */
        R apply(T... args) throws E;

        /**
         *
         *
         * @param <V>
         * @param after
         * @return
         */
        default <V> NFunction<T, V, E> andThen(java.util.function.Function<? super R, ? extends V> after) {
            N.checkArgNotNull(after);

            return args -> after.apply(apply(args));
        }
    }

    /**
     * The Interface IndexedBooleanConsumer.
     *
     * @param <E>
     */
    public interface IndexedBooleanConsumer<E extends Throwable> {

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
    public interface IndexedCharConsumer<E extends Throwable> {

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
    public interface IndexedByteConsumer<E extends Throwable> {

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
    public interface IndexedShortConsumer<E extends Throwable> {

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
    public interface IndexedIntConsumer<E extends Throwable> {

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
    public interface IndexedLongConsumer<E extends Throwable> {

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
    public interface IndexedFloatConsumer<E extends Throwable> {

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
    public interface IndexedDoubleConsumer<E extends Throwable> {

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

        public interface Runnable<E extends Throwable, E2 extends Throwable> {

            /**
             *
             *
             * @throws E
             * @throws E2
             */
            void run() throws E, E2;
        }

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

    public static final class LazyInitializer<T, E extends Throwable> implements Throwables.Supplier<T, E> {
        private final Supplier<T, E> supplier;
        private volatile boolean initialized = false;
        private volatile T value = null; //NOSONAR

        LazyInitializer(final Throwables.Supplier<T, E> supplier) {
            N.checkArgNotNull(supplier, "supplier");

            this.supplier = supplier;
        }

        /**
         *
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
