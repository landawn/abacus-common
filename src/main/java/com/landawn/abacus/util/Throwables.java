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

// TODO: Auto-generated Javadoc
/**
 * Catch checked exception and convert it to <code>RuntimeException</code>.
 *
 * @author Haiyang Li
 * @param <T>
 * @since 0.8
 */
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
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     * @param cmd
     * @param actionOnError
     */
    public static void run(final Throwables.Runnable<? extends Throwable> cmd,
            final com.landawn.abacus.util.function.Consumer<? super Throwable> actionOnError) {
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
            throw N.toRuntimeException(e);
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
            final com.landawn.abacus.util.function.Function<? super Throwable, R> actionOnError) {
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
    public static <R> R call(final Throwables.Callable<R, ? extends Throwable> cmd, final com.landawn.abacus.util.function.Supplier<R> supplier) {
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
    public static <R> R call(final Throwables.Callable<R, ? extends Throwable> cmd,
            final com.landawn.abacus.util.function.Predicate<? super Throwable> predicate, final com.landawn.abacus.util.function.Supplier<R> supplier) {
        N.checkArgNotNull(predicate);
        N.checkArgNotNull(supplier);

        try {
            return cmd.call();
        } catch (Throwable e) {
            if (predicate.test(e)) {
                return supplier.get();
            } else {
                throw N.toRuntimeException(e);
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
    public static <R> R call(final Throwables.Callable<R, ? extends Throwable> cmd,
            final com.landawn.abacus.util.function.Predicate<? super Throwable> predicate, final R defaultValue) {
        N.checkArgNotNull(predicate);

        try {
            return cmd.call();
        } catch (Throwable e) {
            if (predicate.test(e)) {
                return defaultValue;
            } else {
                throw N.toRuntimeException(e);
            }
        }
    }

    /**
     * The Interface Runnable.
     *
     * @param <E>
     */
    public static interface Runnable<E extends Throwable> extends EE.Runnable<E, RuntimeException> {

        /**
         *
         * @throws E the e
         */
        @Override
        void run() throws E;
    }

    /**
     * The Interface Callable.
     *
     * @param <R>
     * @param <E>
     */
    public static interface Callable<R, E extends Throwable> extends EE.Callable<R, E, RuntimeException> {

        /**
         *
         * @return
         * @throws E the e
         */
        @Override
        R call() throws E;
    }

    /**
     * The Interface Supplier.
     *
     * @param <T>
     * @param <E>
     */
    public static interface Supplier<T, E extends Throwable> extends EE.Supplier<T, E, RuntimeException> {

        /**
         *
         * @return
         * @throws E the e
         */
        @Override
        T get() throws E;
    }

    /**
     * The Interface BooleanSupplier.
     *
     * @param <E>
     */
    public static interface BooleanSupplier<E extends Throwable> {

        /**
         * Gets the as boolean.
         *
         * @return
         * @throws E the e
         */
        boolean getAsBoolean() throws E;
    }

    /**
     * The Interface CharSupplier.
     *
     * @param <E>
     */
    public static interface CharSupplier<E extends Throwable> {

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
    public static interface ByteSupplier<E extends Throwable> {

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
    public static interface ShortSupplier<E extends Throwable> {

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
    public static interface IntSupplier<E extends Throwable> {

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
    public static interface LongSupplier<E extends Throwable> {

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
    public static interface FloatSupplier<E extends Throwable> {

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
    public static interface DoubleSupplier<E extends Throwable> {

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
    public static interface Predicate<T, E extends Throwable> extends EE.Predicate<T, E, RuntimeException> {

        /**
         *
         * @param t
         * @return true, if successful
         * @throws E the e
         */
        @Override
        boolean test(T t) throws E;
    }

    /**
     * The Interface BiPredicate.
     *
     * @param <T>
     * @param <U>
     * @param <E>
     */
    public static interface BiPredicate<T, U, E extends Throwable> extends EE.BiPredicate<T, U, E, RuntimeException> {

        /**
         *
         * @param t
         * @param u
         * @return true, if successful
         * @throws E the e
         */
        @Override
        boolean test(T t, U u) throws E;
    }

    /**
     * The Interface TriPredicate.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <E>
     */
    public static interface TriPredicate<A, B, C, E extends Throwable> extends EE.TriPredicate<A, B, C, E, RuntimeException> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return true, if successful
         * @throws E the e
         */
        @Override
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
    public static interface QuadPredicate<A, B, C, D, E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @param d
         * @return true, if successful
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
    public static interface Function<T, R, E extends Throwable> extends EE.Function<T, R, E, RuntimeException> {

        /**
         *
         * @param t
         * @return
         * @throws E the e
         */
        @Override
        R apply(T t) throws E;

        /**
         *
         * @param <T>
         * @param <E>
         * @param consumer
         * @return
         */
        public static <T, E extends Throwable> Function<T, Void, E> convert(final Consumer<T, E> consumer) {
            N.checkArgNotNull(consumer);

            return new Function<T, Void, E>() {
                @Override
                public Void apply(T t) throws E {
                    consumer.accept(t);

                    return null;
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
    public static interface BiFunction<T, U, R, E extends Throwable> extends EE.BiFunction<T, U, R, E, RuntimeException> {

        /**
         *
         * @param t
         * @param u
         * @return
         * @throws E the e
         */
        @Override
        R apply(T t, U u) throws E;

        /**
         *
         * @param <T>
         * @param <U>
         * @param <E>
         * @param biConsumer
         * @return
         */
        public static <T, U, E extends Throwable> BiFunction<T, U, Void, E> convert(final BiConsumer<T, U, E> biConsumer) {
            N.checkArgNotNull(biConsumer);

            return new BiFunction<T, U, Void, E>() {
                @Override
                public Void apply(T t, U u) throws E {
                    biConsumer.accept(t, u);

                    return null;
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
    public static interface TriFunction<A, B, C, R, E extends Throwable> extends EE.TriFunction<A, B, C, R, E, RuntimeException> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return
         * @throws E the e
         */
        @Override
        R apply(A a, B b, C c) throws E;

        /**
         *
         * @param <A>
         * @param <B>
         * @param <C>
         * @param <E>
         * @param triConsumer
         * @return
         */
        public static <A, B, C, E extends Throwable> TriFunction<A, B, C, Void, E> convert(final TriConsumer<A, B, C, E> triConsumer) {
            N.checkArgNotNull(triConsumer);

            return new TriFunction<A, B, C, Void, E>() {
                @Override
                public Void apply(A a, B b, C c) throws E {
                    triConsumer.accept(a, b, c);

                    return null;
                }
            };
        }
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
    public static interface QuadFunction<A, B, C, D, R, E extends Throwable> {

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
    public static interface Consumer<T, E extends Throwable> extends EE.Consumer<T, E, RuntimeException> {

        /**
         *
         * @param t
         * @throws E the e
         */
        @Override
        void accept(T t) throws E;

        /**
         *
         * @param <T>
         * @param <R>
         * @param <E>
         * @param func
         * @return
         */
        public static <T, R, E extends Throwable> Consumer<T, E> convert(final Function<T, R, E> func) {
            N.checkArgNotNull(func);

            return new Consumer<T, E>() {
                @Override
                public void accept(T t) throws E {
                    func.apply(t);
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
    public static interface BiConsumer<T, U, E extends Throwable> extends EE.BiConsumer<T, U, E, RuntimeException> {

        /**
         *
         * @param t
         * @param u
         * @throws E the e
         */
        @Override
        void accept(T t, U u) throws E;

        /**
         *
         * @param <T>
         * @param <U>
         * @param <R>
         * @param <E>
         * @param func
         * @return
         */
        public static <T, U, R, E extends Throwable> BiConsumer<T, U, E> convert(final BiFunction<T, U, R, E> func) {
            N.checkArgNotNull(func);

            return new BiConsumer<T, U, E>() {
                @Override
                public void accept(T t, U u) throws E {
                    func.apply(t, u);
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
    public static interface TriConsumer<A, B, C, E extends Throwable> extends EE.TriConsumer<A, B, C, E, RuntimeException> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @throws E the e
         */
        @Override
        void accept(A a, B b, C c) throws E;

        /**
         *
         * @param <A>
         * @param <B>
         * @param <C>
         * @param <R>
         * @param <E>
         * @param func
         * @return
         */
        public static <A, B, C, R, E extends Throwable> TriConsumer<A, B, C, E> convert(final TriFunction<A, B, C, R, E> func) {
            N.checkArgNotNull(func);

            return new TriConsumer<A, B, C, E>() {
                @Override
                public void accept(A a, B b, C c) throws E {
                    func.apply(a, b, c);
                }
            };
        }
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
    public static interface QuadConsumer<A, B, C, D, E extends Throwable> {

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
     * The Interface IndexedConsumer.
     *
     * @param <T>
     * @param <E>
     */
    public static interface IndexedConsumer<T, E extends Throwable> {

        /**
         *
         * @param idx
         * @param e
         * @throws E the e
         */
        void accept(int idx, T e) throws E;
    }

    /**
     * The Interface IndexedBiConsumer.
     *
     * @param <U>
     * @param <T>
     * @param <E>
     */
    public static interface IndexedBiConsumer<U, T, E extends Throwable> {

        /**
         *
         * @param u
         * @param idx
         * @param e
         * @throws E the e
         */
        void accept(U u, int idx, T e) throws E;
    }

    /**
     * The Interface IndexedFunction.
     *
     * @param <T>
     * @param <R>
     * @param <E>
     */
    public static interface IndexedFunction<T, R, E extends Throwable> {

        /**
         *
         * @param idx
         * @param e
         * @return
         * @throws E the e
         */
        R apply(int idx, T e) throws E;
    }

    /**
     * The Interface IndexedBiFunction.
     *
     * @param <U>
     * @param <T>
     * @param <R>
     * @param <E>
     */
    public static interface IndexedBiFunction<U, T, R, E extends Throwable> {

        /**
         *
         * @param u
         * @param idx
         * @param e
         * @return
         * @throws E the e
         */
        R apply(U u, int idx, T e) throws E;
    }

    /**
     * The Interface IndexedPredicate.
     *
     * @param <T>
     * @param <E>
     */
    public static interface IndexedPredicate<T, E extends Throwable> {

        /**
         *
         * @param idx
         * @param e
         * @return true, if successful
         * @throws E the e
         */
        boolean test(int idx, T e) throws E;
    }

    /**
     * The Interface IndexedBiPredicate.
     *
     * @param <U>
     * @param <T>
     * @param <E>
     */
    public static interface IndexedBiPredicate<U, T, E extends Throwable> {

        /**
         *
         * @param u
         * @param idx
         * @param e
         * @return true, if successful
         * @throws E the e
         */
        boolean test(U u, int idx, T e) throws E;
    }

    /**
     * The Interface BooleanPredicate.
     *
     * @param <E>
     */
    public static interface BooleanPredicate<E extends Throwable> {

        /**
         *
         * @param value
         * @return true, if successful
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
    public static interface BooleanFunction<R, E extends Throwable> {

        /**
         *
         * @param value
         * @return
         * @throws E the e
         */
        R apply(boolean value) throws E;
    }

    /**
     * The Interface BooleanConsumer.
     *
     * @param <E>
     */
    public static interface BooleanConsumer<E extends Throwable> {

        /**
         *
         * @param t
         * @throws E the e
         */
        void accept(boolean t) throws E;
    }

    /**
     * The Interface CharPredicate.
     *
     * @param <E>
     */
    public static interface CharPredicate<E extends Throwable> {

        /**
         *
         * @param value
         * @return true, if successful
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
    public static interface CharFunction<R, E extends Throwable> {

        /**
         *
         * @param value
         * @return
         * @throws E the e
         */
        R apply(char value) throws E;
    }

    /**
     * The Interface CharConsumer.
     *
     * @param <E>
     */
    public static interface CharConsumer<E extends Throwable> {

        /**
         *
         * @param t
         * @throws E the e
         */
        void accept(char t) throws E;
    }

    /**
     * The Interface BytePredicate.
     *
     * @param <E>
     */
    public static interface BytePredicate<E extends Throwable> {

        /**
         *
         * @param value
         * @return true, if successful
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
    public static interface ByteFunction<R, E extends Throwable> {

        /**
         *
         * @param value
         * @return
         * @throws E the e
         */
        R apply(byte value) throws E;
    }

    /**
     * The Interface ByteConsumer.
     *
     * @param <E>
     */
    public static interface ByteConsumer<E extends Throwable> {

        /**
         *
         * @param t
         * @throws E the e
         */
        void accept(byte t) throws E;
    }

    /**
     * The Interface ShortPredicate.
     *
     * @param <E>
     */
    public static interface ShortPredicate<E extends Throwable> {

        /**
         *
         * @param value
         * @return true, if successful
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
    public static interface ShortFunction<R, E extends Throwable> {

        /**
         *
         * @param value
         * @return
         * @throws E the e
         */
        R apply(short value) throws E;
    }

    /**
     * The Interface ShortConsumer.
     *
     * @param <E>
     */
    public static interface ShortConsumer<E extends Throwable> {

        /**
         *
         * @param t
         * @throws E the e
         */
        void accept(short t) throws E;
    }

    /**
     * The Interface IntPredicate.
     *
     * @param <E>
     */
    public static interface IntPredicate<E extends Throwable> {

        /**
         *
         * @param value
         * @return true, if successful
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
    public static interface IntFunction<R, E extends Throwable> {

        /**
         *
         * @param value
         * @return
         * @throws E the e
         */
        R apply(int value) throws E;
    }

    /**
     * The Interface IntConsumer.
     *
     * @param <E>
     */
    public static interface IntConsumer<E extends Throwable> {

        /**
         *
         * @param t
         * @throws E the e
         */
        void accept(int t) throws E;
    }

    /**
     * The Interface LongPredicate.
     *
     * @param <E>
     */
    public static interface LongPredicate<E extends Throwable> {

        /**
         *
         * @param value
         * @return true, if successful
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
    public static interface LongFunction<R, E extends Throwable> {

        /**
         *
         * @param value
         * @return
         * @throws E the e
         */
        R apply(long value) throws E;
    }

    /**
     * The Interface LongConsumer.
     *
     * @param <E>
     */
    public static interface LongConsumer<E extends Throwable> {

        /**
         *
         * @param t
         * @throws E the e
         */
        void accept(long t) throws E;
    }

    /**
     * The Interface FloatPredicate.
     *
     * @param <E>
     */
    public static interface FloatPredicate<E extends Throwable> {

        /**
         *
         * @param value
         * @return true, if successful
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
    public static interface FloatFunction<R, E extends Throwable> {

        /**
         *
         * @param value
         * @return
         * @throws E the e
         */
        R apply(float value) throws E;
    }

    /**
     * The Interface FloatConsumer.
     *
     * @param <E>
     */
    public static interface FloatConsumer<E extends Throwable> {

        /**
         *
         * @param t
         * @throws E the e
         */
        void accept(float t) throws E;
    }

    /**
     * The Interface DoublePredicate.
     *
     * @param <E>
     */
    public static interface DoublePredicate<E extends Throwable> {

        /**
         *
         * @param value
         * @return true, if successful
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
    public static interface DoubleFunction<R, E extends Throwable> {

        /**
         *
         * @param value
         * @return
         * @throws E the e
         */
        R apply(double value) throws E;
    }

    /**
     * The Interface DoubleConsumer.
     *
     * @param <E>
     */
    public static interface DoubleConsumer<E extends Throwable> {

        /**
         *
         * @param t
         * @throws E the e
         */
        void accept(double t) throws E;
    }

    /**
     * The Interface ToBooleanFunction.
     *
     * @param <T>
     * @param <E>
     */
    public static interface ToBooleanFunction<T, E extends Throwable> {

        /**
         * Apply as boolean.
         *
         * @param t
         * @return true, if successful
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
    public static interface ToCharFunction<T, E extends Throwable> {

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
    public static interface ToByteFunction<T, E extends Throwable> {

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
    public static interface ToShortFunction<T, E extends Throwable> {

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
    public static interface ToIntFunction<T, E extends Throwable> {

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
    public static interface ToLongFunction<T, E extends Throwable> {

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
    public static interface ToFloatFunction<T, E extends Throwable> {

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
    public static interface ToDoubleFunction<T, E extends Throwable> {

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
     * The Interface UnaryOperator.
     *
     * @param <T>
     * @param <E>
     */
    public static interface UnaryOperator<T, E extends Throwable> extends Function<T, T, E> {
    }

    /**
     * The Interface BinaryOperator.
     *
     * @param <T>
     * @param <E>
     */
    public static interface BinaryOperator<T, E extends Throwable> extends BiFunction<T, T, T, E> {
    }

    /**
     * The Interface TernaryOperator.
     *
     * @param <T>
     * @param <E>
     */
    public static interface TernaryOperator<T, E extends Throwable> extends BiFunction<T, T, T, E> {
    }

    /**
     * The Interface BooleanUnaryOperator.
     *
     * @param <E>
     */
    public static interface BooleanUnaryOperator<E extends Throwable> {

        /**
         * Apply as boolean.
         *
         * @param operand
         * @return true, if successful
         * @throws E the e
         */
        boolean applyAsBoolean(boolean operand) throws E;
    }

    /**
     * The Interface CharUnaryOperator.
     *
     * @param <E>
     */
    public static interface CharUnaryOperator<E extends Throwable> {

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
    public static interface ByteUnaryOperator<E extends Throwable> {

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
    public static interface ShortUnaryOperator<E extends Throwable> {

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
    public static interface IntUnaryOperator<E extends Throwable> {

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
    public static interface LongUnaryOperator<E extends Throwable> {

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
    public static interface FloatUnaryOperator<E extends Throwable> {

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
    public static interface DoubleUnaryOperator<E extends Throwable> {

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
    public static interface BooleanBinaryOperator<E extends Throwable> {

        /**
         * Apply as boolean.
         *
         * @param left
         * @param right
         * @return true, if successful
         * @throws E the e
         */
        boolean applyAsBoolean(boolean left, boolean right) throws E;
    }

    /**
     * The Interface CharBinaryOperator.
     *
     * @param <E>
     */
    public static interface CharBinaryOperator<E extends Throwable> {

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
    public static interface ByteBinaryOperator<E extends Throwable> {

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
    public static interface ShortBinaryOperator<E extends Throwable> {

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
    public static interface IntBinaryOperator<E extends Throwable> {

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
    public static interface LongBinaryOperator<E extends Throwable> {

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
    public static interface FloatBinaryOperator<E extends Throwable> {

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
    public static interface DoubleBinaryOperator<E extends Throwable> {

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
    public static interface BooleanTernaryOperator<E extends Throwable> {

        /**
         * Apply as boolean.
         *
         * @param a
         * @param b
         * @param c
         * @return true, if successful
         * @throws E the e
         */
        boolean applyAsBoolean(boolean a, boolean b, boolean c) throws E;
    }

    /**
     * The Interface CharTernaryOperator.
     *
     * @param <E>
     */
    public static interface CharTernaryOperator<E extends Throwable> {

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
    public static interface ByteTernaryOperator<E extends Throwable> {

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
    public static interface ShortTernaryOperator<E extends Throwable> {

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
    public static interface IntTernaryOperator<E extends Throwable> {

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
    public static interface LongTernaryOperator<E extends Throwable> {

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
    public static interface FloatTernaryOperator<E extends Throwable> {

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
    public static interface DoubleTernaryOperator<E extends Throwable> {

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
    public static interface BooleanBiPredicate<E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return true, if successful
         * @throws E the e
         */
        boolean test(boolean t, boolean u) throws E;
    }

    /**
     * The Interface CharBiPredicate.
     *
     * @param <E>
     */
    public static interface CharBiPredicate<E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return true, if successful
         * @throws E the e
         */
        boolean test(char t, char u) throws E;
    }

    /**
     * The Interface ByteBiPredicate.
     *
     * @param <E>
     */
    public static interface ByteBiPredicate<E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return true, if successful
         * @throws E the e
         */
        boolean test(byte t, byte u) throws E;
    }

    /**
     * The Interface ShortBiPredicate.
     *
     * @param <E>
     */
    public static interface ShortBiPredicate<E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return true, if successful
         * @throws E the e
         */
        boolean test(short t, short u) throws E;
    }

    /**
     * The Interface IntBiPredicate.
     *
     * @param <E>
     */
    public static interface IntBiPredicate<E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return true, if successful
         * @throws E the e
         */
        boolean test(int t, int u) throws E;
    }

    /**
     * The Interface LongBiPredicate.
     *
     * @param <E>
     */
    public static interface LongBiPredicate<E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return true, if successful
         * @throws E the e
         */
        boolean test(long t, long u) throws E;
    }

    /**
     * The Interface FloatBiPredicate.
     *
     * @param <E>
     */
    public static interface FloatBiPredicate<E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return true, if successful
         * @throws E the e
         */
        boolean test(float t, float u) throws E;
    }

    /**
     * The Interface DoubleBiPredicate.
     *
     * @param <E>
     */
    public static interface DoubleBiPredicate<E extends Throwable> {

        /**
         *
         * @param t
         * @param u
         * @return true, if successful
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
    public static interface BooleanBiFunction<R, E extends Throwable> {

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
    public static interface CharBiFunction<R, E extends Throwable> {

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
    public static interface ByteBiFunction<R, E extends Throwable> {

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
    public static interface ShortBiFunction<R, E extends Throwable> {

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
    public static interface IntBiFunction<R, E extends Throwable> {

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
    public static interface LongBiFunction<R, E extends Throwable> {

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
    public static interface FloatBiFunction<R, E extends Throwable> {

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
    public static interface DoubleBiFunction<R, E extends Throwable> {

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
    public static interface BooleanBiConsumer<E extends Throwable> {

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
    public static interface CharBiConsumer<E extends Throwable> {

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
    public static interface ByteBiConsumer<E extends Throwable> {

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
    public static interface ShortBiConsumer<E extends Throwable> {

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
    public static interface IntBiConsumer<E extends Throwable> {

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
    public static interface LongBiConsumer<E extends Throwable> {

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
    public static interface FloatBiConsumer<E extends Throwable> {

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
    public static interface DoubleBiConsumer<E extends Throwable> {

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
    public static interface BooleanTriPredicate<E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return true, if successful
         * @throws E the e
         */
        boolean test(boolean a, boolean b, boolean c) throws E;
    }

    /**
     * The Interface CharTriPredicate.
     *
     * @param <E>
     */
    public static interface CharTriPredicate<E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return true, if successful
         * @throws E the e
         */
        boolean test(char a, char b, char c) throws E;
    }

    /**
     * The Interface ByteTriPredicate.
     *
     * @param <E>
     */
    public static interface ByteTriPredicate<E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return true, if successful
         * @throws E the e
         */
        boolean test(byte a, byte b, byte c) throws E;
    }

    /**
     * The Interface ShortTriPredicate.
     *
     * @param <E>
     */
    public static interface ShortTriPredicate<E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return true, if successful
         * @throws E the e
         */
        boolean test(short a, short b, short c) throws E;
    }

    /**
     * The Interface IntTriPredicate.
     *
     * @param <E>
     */
    public static interface IntTriPredicate<E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return true, if successful
         * @throws E the e
         */
        boolean test(int a, int b, int c) throws E;
    }

    /**
     * The Interface LongTriPredicate.
     *
     * @param <E>
     */
    public static interface LongTriPredicate<E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return true, if successful
         * @throws E the e
         */
        boolean test(long a, long b, long c) throws E;
    }

    /**
     * The Interface FloatTriPredicate.
     *
     * @param <E>
     */
    public static interface FloatTriPredicate<E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return true, if successful
         * @throws E the e
         */
        boolean test(float a, float b, float c) throws E;
    }

    /**
     * The Interface DoubleTriPredicate.
     *
     * @param <E>
     */
    public static interface DoubleTriPredicate<E extends Throwable> {

        /**
         *
         * @param a
         * @param b
         * @param c
         * @return true, if successful
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
    public static interface BooleanTriFunction<R, E extends Throwable> {

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
    public static interface CharTriFunction<R, E extends Throwable> {

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
    public static interface ByteTriFunction<R, E extends Throwable> {

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
    public static interface ShortTriFunction<R, E extends Throwable> {

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
    public static interface IntTriFunction<R, E extends Throwable> {

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
    public static interface LongTriFunction<R, E extends Throwable> {

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
    public static interface FloatTriFunction<R, E extends Throwable> {

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
    public static interface DoubleTriFunction<R, E extends Throwable> {

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
    public static interface BooleanTriConsumer<E extends Throwable> {

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
    public static interface CharTriConsumer<E extends Throwable> {

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
    public static interface ByteTriConsumer<E extends Throwable> {

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
    public static interface ShortTriConsumer<E extends Throwable> {

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
    public static interface IntTriConsumer<E extends Throwable> {

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
    public static interface LongTriConsumer<E extends Throwable> {

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
    public static interface FloatTriConsumer<E extends Throwable> {

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
    public static interface DoubleTriConsumer<E extends Throwable> {

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
    public static interface ObjBooleanConsumer<T, E extends Throwable> {

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
    public static interface ObjCharConsumer<T, E extends Throwable> {

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
    public static interface ObjByteConsumer<T, E extends Throwable> {

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
    public static interface ObjShortConsumer<T, E extends Throwable> {

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
    public static interface ObjIntConsumer<T, E extends Throwable> {

        /**
         *
         * @param t
         * @param value
         * @throws E the e
         */
        void accept(T t, int value) throws E;
    }

    /**
     * The Interface ObjLongConsumer.
     *
     * @param <T>
     * @param <E>
     */
    public static interface ObjLongConsumer<T, E extends Throwable> {

        /**
         *
         * @param t
         * @param value
         * @throws E the e
         */
        void accept(T t, long value) throws E;
    }

    /**
     * The Interface ObjFloatConsumer.
     *
     * @param <T>
     * @param <E>
     */
    public static interface ObjFloatConsumer<T, E extends Throwable> {

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
    public static interface ObjDoubleConsumer<T, E extends Throwable> {

        /**
         *
         * @param t
         * @param value
         * @throws E the e
         */
        void accept(T t, double value) throws E;
    }

    public static final class EE {
        private EE() {
            // Singleton. Utility class.
        }

        public static interface Runnable<E extends Throwable, E2 extends Throwable> extends EEE.Runnable<E, E2, RuntimeException> {

            @Override
            void run() throws E, E2;
        }

        public static interface Callable<R, E extends Throwable, E2 extends Throwable> extends EEE.Callable<R, E, E2, RuntimeException> {

            @Override
            R call() throws E, E2;
        }

        public static interface Supplier<T, E extends Throwable, E2 extends Throwable> extends EEE.Supplier<T, E, E2, RuntimeException> {

            @Override
            T get() throws E, E2;
        }

        public static interface Predicate<T, E extends Throwable, E2 extends Throwable> extends EEE.Predicate<T, E, E2, RuntimeException> {

            @Override
            boolean test(T t) throws E, E2;
        }

        public static interface BiPredicate<T, U, E extends Throwable, E2 extends Throwable> extends EEE.BiPredicate<T, U, E, E2, RuntimeException> {

            @Override
            boolean test(T t, U u) throws E, E2;
        }

        public static interface TriPredicate<A, B, C, E extends Throwable, E2 extends Throwable> extends EEE.TriPredicate<A, B, C, E, E2, RuntimeException> {

            @Override
            boolean test(A a, B b, C c) throws E, E2;
        }

        public static interface Function<T, R, E extends Throwable, E2 extends Throwable> extends EEE.Function<T, R, E, E2, RuntimeException> {

            @Override
            R apply(T t) throws E, E2;
        }

        public static interface BiFunction<T, U, R, E extends Throwable, E2 extends Throwable> extends EEE.BiFunction<T, U, R, E, E2, RuntimeException> {

            @Override
            R apply(T t, U u) throws E, E2;
        }

        public static interface TriFunction<A, B, C, R, E extends Throwable, E2 extends Throwable>
                extends EEE.TriFunction<A, B, C, R, E, E2, RuntimeException> {

            @Override
            R apply(A a, B b, C c) throws E, E2;
        }

        public static interface Consumer<T, E extends Throwable, E2 extends Throwable> extends EEE.Consumer<T, E, E2, RuntimeException> {

            @Override
            void accept(T t) throws E, E2;
        }

        public static interface BiConsumer<T, U, E extends Throwable, E2 extends Throwable> extends EEE.BiConsumer<T, U, E, E2, RuntimeException> {

            @Override
            void accept(T t, U u) throws E, E2;
        }

        public static interface TriConsumer<A, B, C, E extends Throwable, E2 extends Throwable> extends EEE.TriConsumer<A, B, C, E, E2, RuntimeException> {

            @Override
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

        public static interface Runnable<E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            void run() throws E, E2, E3;
        }

        public static interface Callable<R, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            R call() throws E, E2, E3;
        }

        public static interface Supplier<T, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            T get() throws E, E2, E3;
        }

        public static interface Predicate<T, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            boolean test(T t) throws E, E2, E3;
        }

        public static interface BiPredicate<T, U, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            boolean test(T t, U u) throws E, E2, E3;
        }

        public static interface TriPredicate<A, B, C, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            boolean test(A a, B b, C c) throws E, E2, E3;
        }

        public static interface Function<T, R, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            R apply(T t) throws E, E2, E3;
        }

        public static interface BiFunction<T, U, R, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            R apply(T t, U u) throws E, E2, E3;
        }

        public static interface TriFunction<A, B, C, R, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            R apply(A a, B b, C c) throws E, E2, E3;
        }

        public static interface Consumer<T, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            void accept(T t) throws E, E2, E3;
        }

        public static interface BiConsumer<T, U, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            void accept(T t, U u) throws E, E2, E3;
        }

        public static interface TriConsumer<A, B, C, E extends Throwable, E2 extends Throwable, E3 extends Throwable> {

            void accept(A a, B b, C c) throws E, E2, E3;
        }
    }

    public static final class LazyInitializer<T, E extends Throwable> implements Throwables.Supplier<T, E> {
        private volatile boolean initialized = false;
        private volatile T value = null;
        private final Supplier<T, E> supplier;

        LazyInitializer(final Throwables.Supplier<T, E> supplier) {
            N.checkArgNotNull(supplier, "supplier");

            this.supplier = supplier;
        }

        @Override
        public final T get() throws E {
            if (initialized == false) {
                synchronized (this) {
                    if (initialized == false) {
                        value = supplier.get();

                        initialized = true;
                    }

                }
            }

            return value;
        }

        /**
         *
         * @param <T>
         * @param supplier
         * @return
         */
        public static <T, E extends Throwable> LazyInitializer<T, E> of(final Throwables.Supplier<T, E> supplier) {
            N.checkArgNotNull(supplier);

            return new LazyInitializer<>(supplier);
        }
    }
}
