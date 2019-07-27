/*
 * Copyright (C) 2016 HaiYang Li
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

/**
 * Catch checked exception and convert it to <code>RuntimeException</code>.
 *
 * @since 0.8
 * 
 * @author Haiyang Li
 */
public final class Try<T extends AutoCloseable> {
    private final T t;

    Try(final T t) {
        N.checkArgNotNull(t);

        this.t = t;
    }

    public static <T extends AutoCloseable> Try<T> of(final T t) {
        return new Try<>(t);
    }

    public static <T extends AutoCloseable> Try<T> of(final Supplier<T, ? extends Exception> supplier) {
        try {
            return new Try<>(supplier.get());
        } catch (Exception e) {
            throw N.toRuntimeException(e);
        }
    }

    //    public static Try<Reader> reader(final File file) {
    //        try {
    //            return of((Reader) new FileReader(file));
    //        } catch (FileNotFoundException e) {
    //            throw N.toRuntimeException(e);
    //        }
    //    }
    //
    //    public static Try<java.io.BufferedReader> bufferedReader(final File file) {
    //        return of(IOUtil.newBufferedReader(file));
    //    }
    //
    //    public static Try<Writer> writer(final File file) {
    //        try {
    //            return of((Writer) new FileWriter(file));
    //        } catch (IOException e) {
    //            throw N.toRuntimeException(e);
    //        }
    //    }
    //
    //    public static Try<java.io.BufferedWriter> bufferedWriter(final File file) {
    //        return of(IOUtil.newBufferedWriter(file));
    //    }
    //
    //    public static Try<Stream<String>> lines(final File file) {
    //        final Reader reader = IOUtil.newBufferedReader(file);
    //
    //        return new Try<>(Stream.of(reader).onClose(new java.lang.Runnable() {
    //            @Override
    //            public void run() {
    //                IOUtil.close(reader);
    //            }
    //        }));
    //    }

    //    public static java.lang.Runnable of(final Try.Runnable run) {
    //        return new java.lang.Runnable() {
    //            @Override
    //            public void run() {
    //                try {
    //                    run.run();
    //                } catch (Exception e) {
    //                    throw N.toRuntimeException(e);
    //                }
    //            }
    //        };
    //    }
    //
    //    public static <R> Try.Callable<R> of(final java.util.concurrent.Callable<R> call) {
    //        return new Try.Callable<R>() {
    //            @Override
    //            public R call() {
    //                try {
    //                    return call.call();
    //                } catch (Exception e) {
    //                    throw N.toRuntimeException(e);
    //                }
    //            }
    //        };
    //    }

    /**
     * 
     * @param cmd
     * @throws RuntimeException if some error happens
     */
    public static void run(final Try.Runnable<? extends Exception> cmd) {
        try {
            cmd.run();
        } catch (Exception e) {
            throw N.toRuntimeException(e);
        }
    }

    public static void run(final Try.Runnable<? extends Exception> cmd, final com.landawn.abacus.util.function.Consumer<? super Exception> actionOnError) {
        N.checkArgNotNull(actionOnError);

        try {
            cmd.run();
        } catch (Exception e) {
            actionOnError.accept(e);
        }
    }

    //    /**
    //     * 
    //     * @param cmd
    //     * @throws RuntimeException if some error happens
    //     */
    //    public static <U> void run(final U init, final Try.Consumer<? super U, ? extends Exception> cmd) {
    //        try {
    //            cmd.accept(init);
    //        } catch (Exception e) {
    //            throw N.toRuntimeException(e);
    //        }
    //    }
    //
    //    public static <U> void run(final U init, final Try.Consumer<? super U, ? extends Exception> cmd,
    //            final com.landawn.abacus.util.function.Consumer<? super Exception> actionOnError) {
    //        N.checkArgNotNull(actionOnError);
    //
    //        try {
    //            cmd.accept(init);
    //        } catch (Exception e) {
    //            actionOnError.accept(e);
    //        }
    //    }

    /**
     * 
     * @param cmd
     * @return
     * @throws RuntimeException if some error happens
     */
    public static <R> R call(final java.util.concurrent.Callable<R> cmd) {
        try {
            return cmd.call();
        } catch (Exception e) {
            throw N.toRuntimeException(e);
        }
    }

    public static <R> R call(final java.util.concurrent.Callable<R> cmd, final com.landawn.abacus.util.function.Function<? super Exception, R> actionOnError) {
        N.checkArgNotNull(actionOnError);

        try {
            return cmd.call();
        } catch (Exception e) {
            return actionOnError.apply(e);
        }
    }

    public static <R> R call(final java.util.concurrent.Callable<R> cmd, final com.landawn.abacus.util.function.Supplier<R> supplier) {
        N.checkArgNotNull(supplier);

        try {
            return cmd.call();
        } catch (Exception e) {
            return supplier.get();
        }
    }

    public static <R> R call(final java.util.concurrent.Callable<R> cmd, final R defaultValue) {
        try {
            return cmd.call();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 
     * @param cmd
     * @param predicate
     * @param supplier
     * @return the value returned <code>Supplier.get()</code> if some error happens and <code>predicate</code> return true.
     * @throws RuntimeException if some error happens and <code>predicate</code> return false.
     */
    public static <R> R call(final java.util.concurrent.Callable<R> cmd, final com.landawn.abacus.util.function.Predicate<? super Exception> predicate,
            final com.landawn.abacus.util.function.Supplier<R> supplier) {
        N.checkArgNotNull(predicate);
        N.checkArgNotNull(supplier);

        try {
            return cmd.call();
        } catch (Exception e) {
            if (predicate.test(e)) {
                return supplier.get();
            } else {
                throw N.toRuntimeException(e);
            }
        }
    }

    /**
     * 
     * @param cmd
     * @param predicate
     * @param defaultValue
     * @return the <code>defaultValue()</code> if some error happens and <code>predicate</code> return true.
     * @throws RuntimeException if some error happens and <code>predicate</code> return false.
     */
    public static <R> R call(final java.util.concurrent.Callable<R> cmd, final com.landawn.abacus.util.function.Predicate<? super Exception> predicate,
            final R defaultValue) {
        N.checkArgNotNull(predicate);

        try {
            return cmd.call();
        } catch (Exception e) {
            if (predicate.test(e)) {
                return defaultValue;
            } else {
                throw N.toRuntimeException(e);
            }
        }
    }

    //    /**
    //     * @param init
    //     * @param cmd
    //     * @return
    //     * @throws RuntimeException if some error happens
    //     */
    //    public static <U, R> R call(final U init, final Try.Function<? super U, R, ? extends Exception> cmd) {
    //        try {
    //            return cmd.apply(init);
    //        } catch (Exception e) {
    //            throw N.toRuntimeException(e);
    //        }
    //    }
    //
    //    /**
    //     * 
    //     * @param init
    //     * @param cmd
    //     * @param actionOnError
    //     * @return
    //     */
    //    public static <U, R> R call(final U init, final Try.Function<? super U, R, ? extends Exception> cmd,
    //            final com.landawn.abacus.util.function.Function<? super Exception, R> actionOnError) {
    //        N.checkArgNotNull(actionOnError);
    //
    //        try {
    //            return cmd.apply(init);
    //        } catch (Exception e) {
    //            return actionOnError.apply(e);
    //        }
    //    }
    //
    //    /**
    //     * 
    //     * @param init
    //     * @param cmd
    //     * @param supplier
    //     * @return
    //     */
    //    public static <U, R> R call(final U init, final Try.Function<? super U, R, ? extends Exception> cmd,
    //            final com.landawn.abacus.util.function.Supplier<R> supplier) {
    //        N.checkArgNotNull(supplier);
    //
    //        try {
    //            return cmd.apply(init);
    //        } catch (Exception e) {
    //            return supplier.get();
    //        }
    //    }
    //
    //    /**
    //     * 
    //     * @param init
    //     * @param cmd
    //     * @param defaultValue
    //     * @return
    //     */
    //    public static <U, R> R call(final U init, final Try.Function<? super U, R, ? extends Exception> cmd, final R defaultValue) {
    //        try {
    //            return cmd.apply(init);
    //        } catch (Exception e) {
    //            return defaultValue;
    //        }
    //    }
    //
    //    /**
    //     * 
    //     * @param init
    //     * @param cmd
    //     * @param predicate
    //     * @param supplier
    //     * @return the value returned <code>Supplier.get()</code> if some error happens and <code>predicate</code> return true.
    //     * @throws RuntimeException if some error happens and <code>predicate</code> return false.
    //     */
    //    public static <U, R> R call(final U init, final Try.Function<? super U, R, ? extends Exception> cmd,
    //            final com.landawn.abacus.util.function.Predicate<? super Exception> predicate, final com.landawn.abacus.util.function.Supplier<R> supplier) {
    //        N.checkArgNotNull(predicate);
    //        N.checkArgNotNull(supplier);
    //
    //        try {
    //            return cmd.apply(init);
    //        } catch (Exception e) {
    //            if (predicate.test(e)) {
    //                return supplier.get();
    //            } else {
    //                throw N.toRuntimeException(e);
    //            }
    //        }
    //    }
    //
    //    /**
    //     * 
    //     * @param init
    //     * @param cmd
    //     * @param predicate
    //     * @param defaultValue
    //     * @return the <code>defaultValue()</code> if some error happens and <code>predicate</code> return true.
    //     * @throws RuntimeException if some error happens and <code>predicate</code> return false.
    //     */
    //    public static <U, R> R call(final U init, final Try.Function<? super U, R, ? extends Exception> cmd,
    //            final com.landawn.abacus.util.function.Predicate<? super Exception> predicate, final R defaultValue) {
    //        N.checkArgNotNull(predicate);
    //
    //        try {
    //            return cmd.apply(init);
    //        } catch (Exception e) {
    //            if (predicate.test(e)) {
    //                return defaultValue;
    //            } else {
    //                throw N.toRuntimeException(e);
    //            }
    //        }
    //    }

    //    public static <E extends Exception> Try.Callable<Void, E> callable(final Try.Runnable<E> cmd) {
    //        N.checkArgNotNull(cmd);
    //
    //        return new Try.Callable<Void, E>() {
    //            @Override
    //            public Void call() throws E {
    //                cmd.run();
    //                return null;
    //            }
    //        };
    //    }

    public T val() {
        return t;
    }

    public void run(final Try.Consumer<? super T, ? extends Exception> cmd) {
        try {
            cmd.accept(t);
        } catch (Exception e) {
            throw N.toRuntimeException(e);
        } finally {
            IOUtil.close(t);
        }
    }

    public void run(final Try.Consumer<? super T, ? extends Exception> cmd, final com.landawn.abacus.util.function.Consumer<? super Exception> actionOnError) {
        N.checkArgNotNull(actionOnError);

        try {
            cmd.accept(t);
        } catch (Exception e) {
            actionOnError.accept(e);
        } finally {
            IOUtil.close(t);
        }
    }

    public <R> R call(final Try.Function<? super T, R, ? extends Exception> cmd) {
        try {
            return cmd.apply(t);
        } catch (Exception e) {
            throw N.toRuntimeException(e);
        } finally {
            IOUtil.close(t);
        }
    }

    public <R> R call(final Try.Function<? super T, R, ? extends Exception> cmd,
            final com.landawn.abacus.util.function.Function<? super Exception, R> actionOnError) {
        N.checkArgNotNull(actionOnError);

        try {
            return cmd.apply(t);
        } catch (Exception e) {
            return actionOnError.apply(e);
        } finally {
            IOUtil.close(t);
        }
    }

    public <R> R call(final Try.Function<? super T, R, ? extends Exception> cmd, final com.landawn.abacus.util.function.Supplier<R> supplier) {
        N.checkArgNotNull(supplier);

        try {
            return cmd.apply(t);
        } catch (Exception e) {
            return supplier.get();
        } finally {
            IOUtil.close(t);
        }
    }

    public <R> R call(final Try.Function<? super T, R, ? extends Exception> cmd, final R defaultValue) {
        try {
            return cmd.apply(t);
        } catch (Exception e) {
            return defaultValue;
        } finally {
            IOUtil.close(t);
        }
    }

    public <R> R call(final Try.Function<? super T, R, ? extends Exception> cmd, final com.landawn.abacus.util.function.Predicate<? super Exception> predicate,
            final com.landawn.abacus.util.function.Supplier<R> supplier) {
        N.checkArgNotNull(predicate);
        N.checkArgNotNull(supplier);

        try {
            return cmd.apply(t);
        } catch (Exception e) {
            if (predicate.test(e)) {
                return supplier.get();
            } else {
                throw N.toRuntimeException(e);
            }
        } finally {
            IOUtil.close(t);
        }
    }

    public <R> R call(final Try.Function<? super T, R, ? extends Exception> cmd, final com.landawn.abacus.util.function.Predicate<? super Exception> predicate,
            final R defaultValue) {
        N.checkArgNotNull(predicate);

        try {
            return cmd.apply(t);
        } catch (Exception e) {
            if (predicate.test(e)) {
                return defaultValue;
            } else {
                throw N.toRuntimeException(e);
            }
        } finally {
            IOUtil.close(t);
        }
    }

    public static interface Runnable<E extends Exception> {
        void run() throws E;
    }

    public static interface Callable<R, E extends Exception> extends java.util.concurrent.Callable<R> {

        @Override
        R call() throws E;
    }

    public static interface Supplier<T, E extends Exception> {
        T get() throws E;
    }

    public static interface BooleanSupplier<E extends Exception> {
        boolean getAsBoolean() throws E;
    }

    public static interface CharSupplier<E extends Exception> {
        char getAsChar() throws E;
    }

    public static interface ByteSupplier<E extends Exception> {
        byte getAsByte() throws E;
    }

    public static interface ShortSupplier<E extends Exception> {
        short getAsShort() throws E;
    }

    public static interface IntSupplier<E extends Exception> {
        int getAsInt() throws E;
    }

    public static interface LongSupplier<E extends Exception> {
        long getAsLong() throws E;
    }

    public static interface FloatSupplier<E extends Exception> {
        float getAsFloat() throws E;
    }

    public static interface DoubleSupplier<E extends Exception> {
        double getAsDouble() throws E;
    }

    public static interface Predicate<T, E extends Exception> {
        boolean test(T t) throws E;
    }

    public static interface BiPredicate<T, U, E extends Exception> {
        boolean test(T t, U u) throws E;
    }

    public static interface TriPredicate<A, B, C, E extends Exception> {
        boolean test(A a, B b, C c) throws E;
    }

    public static interface QuadPredicate<A, B, C, D, E extends Exception> {
        boolean test(A a, B b, C c, D d) throws E;
    }

    public static interface Function<T, R, E extends Exception> {
        R apply(T t) throws E;

        public static <T, E extends Exception> Function<T, Void, E> convert(final Consumer<T, E> consumer) {
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

    public static interface BiFunction<T, U, R, E extends Exception> {
        R apply(T t, U u) throws E;

        public static <T, U, E extends Exception> BiFunction<T, U, Void, E> convert(final BiConsumer<T, U, E> biConsumer) {
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

    public static interface TriFunction<A, B, C, R, E extends Exception> {
        R apply(A a, B b, C c) throws E;

        public static <A, B, C, E extends Exception> TriFunction<A, B, C, Void, E> convert(final TriConsumer<A, B, C, E> triConsumer) {
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

    public static interface QuadFunction<A, B, C, D, R, E extends Exception> {
        R apply(A a, B b, C c, D d) throws E;
    }

    public static interface Consumer<T, E extends Exception> {
        void accept(T t) throws E;

        public static <T, R, E extends Exception> Consumer<T, E> convert(final Function<T, R, E> func) {
            N.checkArgNotNull(func);

            return new Consumer<T, E>() {
                @Override
                public void accept(T t) throws E {
                    func.apply(t);
                }
            };
        }
    }

    public static interface BiConsumer<T, U, E extends Exception> {
        void accept(T t, U u) throws E;

        public static <T, U, R, E extends Exception> BiConsumer<T, U, E> convert(final BiFunction<T, U, R, E> func) {
            N.checkArgNotNull(func);

            return new BiConsumer<T, U, E>() {
                @Override
                public void accept(T t, U u) throws E {
                    func.apply(t, u);
                }
            };
        }
    }

    public static interface TriConsumer<A, B, C, E extends Exception> {
        void accept(A a, B b, C c) throws E;

        public static <A, B, C, R, E extends Exception> TriConsumer<A, B, C, E> convert(final TriFunction<A, B, C, R, E> func) {
            N.checkArgNotNull(func);

            return new TriConsumer<A, B, C, E>() {
                @Override
                public void accept(A a, B b, C c) throws E {
                    func.apply(a, b, c);
                }
            };
        }
    }

    public static interface QuadConsumer<A, B, C, D, E extends Exception> {
        void accept(A a, B b, C c, D d) throws E;
    }

    public static interface IndexedConsumer<T, E extends Exception> {
        void accept(int idx, T e) throws E;
    }

    public static interface IndexedBiConsumer<U, T, E extends Exception> {
        void accept(U u, int idx, T e) throws E;
    }

    public static interface IndexedFunction<T, R, E extends Exception> {
        R apply(int idx, T e) throws E;
    }

    public static interface IndexedBiFunction<U, T, R, E extends Exception> {
        R apply(U u, int idx, T e) throws E;
    }

    public static interface IndexedPredicate<T, E extends Exception> {
        boolean test(int idx, T e) throws E;
    }

    public static interface IndexedBiPredicate<U, T, E extends Exception> {
        boolean test(U u, int idx, T e) throws E;
    }

    public static interface BooleanPredicate<E extends Exception> {
        boolean test(boolean value) throws E;
    }

    public static interface BooleanFunction<R, E extends Exception> {
        R apply(boolean value) throws E;
    }

    public static interface BooleanConsumer<E extends Exception> {
        void accept(boolean t) throws E;
    }

    public static interface CharPredicate<E extends Exception> {
        boolean test(char value) throws E;
    }

    public static interface CharFunction<R, E extends Exception> {
        R apply(char value) throws E;
    }

    public static interface CharConsumer<E extends Exception> {
        void accept(char t) throws E;
    }

    public static interface BytePredicate<E extends Exception> {
        boolean test(byte value) throws E;
    }

    public static interface ByteFunction<R, E extends Exception> {
        R apply(byte value) throws E;
    }

    public static interface ByteConsumer<E extends Exception> {
        void accept(byte t) throws E;
    }

    public static interface ShortPredicate<E extends Exception> {
        boolean test(short value) throws E;
    }

    public static interface ShortFunction<R, E extends Exception> {
        R apply(short value) throws E;
    }

    public static interface ShortConsumer<E extends Exception> {
        void accept(short t) throws E;
    }

    public static interface IntPredicate<E extends Exception> {
        boolean test(int value) throws E;
    }

    public static interface IntFunction<R, E extends Exception> {
        R apply(int value) throws E;
    }

    public static interface IntConsumer<E extends Exception> {
        void accept(int t) throws E;
    }

    public static interface LongPredicate<E extends Exception> {
        boolean test(long value) throws E;
    }

    public static interface LongFunction<R, E extends Exception> {
        R apply(long value) throws E;
    }

    public static interface LongConsumer<E extends Exception> {
        void accept(long t) throws E;
    }

    public static interface FloatPredicate<E extends Exception> {
        boolean test(float value) throws E;
    }

    public static interface FloatFunction<R, E extends Exception> {
        R apply(float value) throws E;
    }

    public static interface FloatConsumer<E extends Exception> {
        void accept(float t) throws E;
    }

    public static interface DoublePredicate<E extends Exception> {
        boolean test(double value) throws E;
    }

    public static interface DoubleFunction<R, E extends Exception> {
        R apply(double value) throws E;
    }

    public static interface DoubleConsumer<E extends Exception> {
        void accept(double t) throws E;
    }

    public static interface ToBooleanFunction<T, E extends Exception> {
        boolean applyAsBoolean(T t) throws E;
    }

    public static interface ToCharFunction<T, E extends Exception> {
        char applyAsChar(T t) throws E;
    }

    public static interface ToByteFunction<T, E extends Exception> {
        byte applyAsByte(T t) throws E;
    }

    public static interface ToShortFunction<T, E extends Exception> {
        short applyAsShort(T t) throws E;
    }

    public static interface ToIntFunction<T, E extends Exception> {
        int applyAsInt(T t) throws E;
    }

    public static interface ToLongFunction<T, E extends Exception> {
        long applyAsLong(T t) throws E;
    }

    public static interface ToFloatFunction<T, E extends Exception> {
        float applyAsFloat(T t) throws E;
    }

    public static interface ToDoubleFunction<T, E extends Exception> {
        double applyAsDouble(T t) throws E;
    }

    public static interface UnaryOperator<T, E extends Exception> extends Function<T, T, E> {
    }

    public static interface BinaryOperator<T, E extends Exception> extends BiFunction<T, T, T, E> {
    }

    public static interface TernaryOperator<T, E extends Exception> extends BiFunction<T, T, T, E> {
    }

    public static interface BooleanUnaryOperator<E extends Exception> {
        boolean applyAsBoolean(boolean operand) throws E;
    }

    public static interface CharUnaryOperator<E extends Exception> {
        char applyAsChar(char operand) throws E;
    }

    public static interface ByteUnaryOperator<E extends Exception> {
        byte applyAsByte(byte operand) throws E;
    }

    public static interface ShortUnaryOperator<E extends Exception> {
        short applyAsShort(short operand) throws E;
    }

    public static interface IntUnaryOperator<E extends Exception> {
        int applyAsInt(int operand) throws E;
    }

    public static interface LongUnaryOperator<E extends Exception> {
        long applyAsLong(long operand) throws E;
    }

    public static interface FloatUnaryOperator<E extends Exception> {
        float applyAsFloat(float operand) throws E;
    }

    public static interface DoubleUnaryOperator<E extends Exception> {
        double applyAsDouble(double operand) throws E;
    }

    public static interface BooleanBinaryOperator<E extends Exception> {
        boolean applyAsBoolean(boolean left, boolean right) throws E;
    }

    public static interface CharBinaryOperator<E extends Exception> {
        char applyAsChar(char left, char right) throws E;
    }

    public static interface ByteBinaryOperator<E extends Exception> {
        byte applyAsByte(byte left, byte right) throws E;
    }

    public static interface ShortBinaryOperator<E extends Exception> {
        short applyAsShort(short left, short right) throws E;
    }

    public static interface IntBinaryOperator<E extends Exception> {
        int applyAsInt(int left, int right) throws E;
    }

    public static interface LongBinaryOperator<E extends Exception> {
        long applyAsLong(long left, long right) throws E;
    }

    public static interface FloatBinaryOperator<E extends Exception> {
        float applyAsFloat(float left, float right) throws E;
    }

    public static interface DoubleBinaryOperator<E extends Exception> {
        double applyAsDouble(double left, double right) throws E;
    }

    public static interface BooleanTernaryOperator<E extends Exception> {
        boolean applyAsBoolean(boolean a, boolean b, boolean c) throws E;
    }

    public static interface CharTernaryOperator<E extends Exception> {
        char applyAsChar(char a, char b, char c) throws E;
    }

    public static interface ByteTernaryOperator<E extends Exception> {
        byte applyAsByte(byte a, byte b, byte c) throws E;
    }

    public static interface ShortTernaryOperator<E extends Exception> {
        short applyAsShort(short a, short b, short c) throws E;
    }

    public static interface IntTernaryOperator<E extends Exception> {
        int applyAsInt(int a, int b, int c) throws E;
    }

    public static interface LongTernaryOperator<E extends Exception> {
        long applyAsLong(long a, long b, long c) throws E;
    }

    public static interface FloatTernaryOperator<E extends Exception> {
        float applyAsFloat(float a, float b, float c) throws E;
    }

    public static interface DoubleTernaryOperator<E extends Exception> {
        double applyAsDouble(double a, double b, double c) throws E;
    }

    public static interface BooleanBiPredicate<E extends Exception> {
        boolean test(boolean t, boolean u) throws E;
    }

    public static interface CharBiPredicate<E extends Exception> {
        boolean test(char t, char u) throws E;
    }

    public static interface ByteBiPredicate<E extends Exception> {
        boolean test(byte t, byte u) throws E;
    }

    public static interface ShortBiPredicate<E extends Exception> {
        boolean test(short t, short u) throws E;
    }

    public static interface IntBiPredicate<E extends Exception> {
        boolean test(int t, int u) throws E;
    }

    public static interface LongBiPredicate<E extends Exception> {
        boolean test(long t, long u) throws E;
    }

    public static interface FloatBiPredicate<E extends Exception> {
        boolean test(float t, float u) throws E;
    }

    public static interface DoubleBiPredicate<E extends Exception> {
        boolean test(double t, double u) throws E;
    }

    public static interface BooleanBiFunction<R, E extends Exception> {
        R apply(boolean t, boolean u) throws E;
    }

    public static interface CharBiFunction<R, E extends Exception> {
        R apply(char t, char u) throws E;
    }

    public static interface ByteBiFunction<R, E extends Exception> {
        R apply(byte t, byte u) throws E;
    }

    public static interface ShortBiFunction<R, E extends Exception> {
        R apply(short t, short u) throws E;
    }

    public static interface IntBiFunction<R, E extends Exception> {
        R apply(int t, int u) throws E;
    }

    public static interface LongBiFunction<R, E extends Exception> {
        R apply(long t, long u) throws E;
    }

    public static interface FloatBiFunction<R, E extends Exception> {
        R apply(float t, float u) throws E;
    }

    public static interface DoubleBiFunction<R, E extends Exception> {
        R apply(double t, double u) throws E;
    }

    public static interface BooleanBiConsumer<E extends Exception> {
        void accept(boolean t, boolean u) throws E;
    }

    public static interface CharBiConsumer<E extends Exception> {
        void accept(char t, char u) throws E;
    }

    public static interface ByteBiConsumer<E extends Exception> {
        void accept(byte t, byte u) throws E;
    }

    public static interface ShortBiConsumer<E extends Exception> {
        void accept(short t, short u) throws E;
    }

    public static interface IntBiConsumer<E extends Exception> {
        void accept(int t, int u) throws E;
    }

    public static interface LongBiConsumer<E extends Exception> {
        void accept(long t, long u) throws E;
    }

    public static interface FloatBiConsumer<E extends Exception> {
        void accept(float t, float u) throws E;
    }

    public static interface DoubleBiConsumer<E extends Exception> {
        void accept(double t, double u) throws E;
    }

    public static interface BooleanTriPredicate<E extends Exception> {
        boolean test(boolean a, boolean b, boolean c) throws E;
    }

    public static interface CharTriPredicate<E extends Exception> {
        boolean test(char a, char b, char c) throws E;
    }

    public static interface ByteTriPredicate<E extends Exception> {
        boolean test(byte a, byte b, byte c) throws E;
    }

    public static interface ShortTriPredicate<E extends Exception> {
        boolean test(short a, short b, short c) throws E;
    }

    public static interface IntTriPredicate<E extends Exception> {
        boolean test(int a, int b, int c) throws E;
    }

    public static interface LongTriPredicate<E extends Exception> {
        boolean test(long a, long b, long c) throws E;
    }

    public static interface FloatTriPredicate<E extends Exception> {
        boolean test(float a, float b, float c) throws E;
    }

    public static interface DoubleTriPredicate<E extends Exception> {
        boolean test(double a, double b, double c) throws E;
    }

    public static interface BooleanTriFunction<R, E extends Exception> {
        R apply(boolean a, boolean b, boolean c) throws E;
    }

    public static interface CharTriFunction<R, E extends Exception> {
        R apply(char a, char b, char c) throws E;
    }

    public static interface ByteTriFunction<R, E extends Exception> {
        R apply(byte a, byte b, byte c) throws E;
    }

    public static interface ShortTriFunction<R, E extends Exception> {
        R apply(short a, short b, short c) throws E;
    }

    public static interface IntTriFunction<R, E extends Exception> {
        R apply(int a, int b, int c) throws E;
    }

    public static interface LongTriFunction<R, E extends Exception> {
        R apply(long a, long b, long c) throws E;
    }

    public static interface FloatTriFunction<R, E extends Exception> {
        R apply(float a, float b, float c) throws E;
    }

    public static interface DoubleTriFunction<R, E extends Exception> {
        R apply(double a, double b, double c) throws E;
    }

    public static interface BooleanTriConsumer<E extends Exception> {
        void accept(boolean a, boolean b, boolean c) throws E;
    }

    public static interface CharTriConsumer<E extends Exception> {
        void accept(char a, char b, char c) throws E;
    }

    public static interface ByteTriConsumer<E extends Exception> {
        void accept(byte a, byte b, byte c) throws E;
    }

    public static interface ShortTriConsumer<E extends Exception> {
        void accept(short a, short b, short c) throws E;
    }

    public static interface IntTriConsumer<E extends Exception> {
        void accept(int a, int b, int c) throws E;
    }

    public static interface LongTriConsumer<E extends Exception> {
        void accept(long a, long b, long c) throws E;
    }

    public static interface FloatTriConsumer<E extends Exception> {
        void accept(float a, float b, float c) throws E;
    }

    public static interface DoubleTriConsumer<E extends Exception> {
        void accept(double a, double b, double c) throws E;
    }

    public static interface ObjBooleanConsumer<T, E extends Exception> {
        void accept(T t, boolean value) throws E;
    }

    public static interface ObjCharConsumer<T, E extends Exception> {
        void accept(T t, char value) throws E;
    }

    public static interface ObjByteConsumer<T, E extends Exception> {
        void accept(T t, byte value) throws E;
    }

    public static interface ObjShortConsumer<T, E extends Exception> {
        void accept(T t, short value) throws E;
    }

    public static interface ObjIntConsumer<T, E extends Exception> {
        void accept(T t, int value) throws E;
    }

    public static interface ObjLongConsumer<T, E extends Exception> {
        void accept(T t, long value) throws E;
    }

    public static interface ObjFloatConsumer<T, E extends Exception> {
        void accept(T t, float value) throws E;
    }

    public static interface ObjDoubleConsumer<T, E extends Exception> {
        void accept(T t, double value) throws E;
    }

    public static final class EE {
        private EE() {
            // Singleton. Utility class.
        }

        public static interface Runnable<E extends Exception, E2 extends Exception> {
            void run() throws E, E2;
        }

        public static interface Callable<R, E extends Exception, E2 extends Exception> extends java.util.concurrent.Callable<R> {
            @Override
            R call() throws E, E2;
        }

        public static interface Supplier<T, E extends Exception, E2 extends Exception> {
            T get() throws E, E2;
        }

        public static interface Predicate<T, E extends Exception, E2 extends Exception> {
            boolean test(T t) throws E, E2;
        }

        public static interface BiPredicate<T, U, E extends Exception, E2 extends Exception> {
            boolean test(T t, U u) throws E, E2;
        }

        public static interface TriPredicate<A, B, C, E extends Exception, E2 extends Exception> {
            boolean test(A a, B b, C c) throws E, E2;
        }

        public static interface Function<T, R, E extends Exception, E2 extends Exception> {
            R apply(T t) throws E, E2;
        }

        public static interface BiFunction<T, U, R, E extends Exception, E2 extends Exception> {
            R apply(T t, U u) throws E, E2;
        }

        public static interface TriFunction<A, B, C, R, E extends Exception, E2 extends Exception> {
            R apply(A a, B b, C c) throws E, E2;
        }

        public static interface Consumer<T, E extends Exception, E2 extends Exception> {
            void accept(T t) throws E, E2;
        }

        public static interface BiConsumer<T, U, E extends Exception, E2 extends Exception> {
            void accept(T t, U u) throws E, E2;
        }

        public static interface TriConsumer<A, B, C, E extends Exception, E2 extends Exception> {
            void accept(A a, B b, C c) throws E, E2;
        }
    }

    //    public static final class EEE {
    //        private EEE() {
    //            // Singleton. Utility class.
    //        }
    //
    //        public static interface Runnable<E extends Exception, E2 extends Exception, E3 extends Exception> {
    //            void run() throws E, E2, E3;
    //        }
    //
    //        public static interface Callable<R, E extends Exception, E2 extends Exception, E3 extends Exception> extends java.util.concurrent.Callable<R> {
    //            @Override
    //            R call() throws E, E2, E3;
    //        }
    //
    //        public static interface Supplier<T, E extends Exception, E2 extends Exception, E3 extends Exception> {
    //            T get() throws E, E2, E3;
    //        }
    //
    //        public static interface Predicate<T, E extends Exception, E2 extends Exception, E3 extends Exception> {
    //            boolean test(T t) throws E, E2, E3;
    //        }
    //
    //        public static interface BiPredicate<T, U, E extends Exception, E2 extends Exception, E3 extends Exception> {
    //            boolean test(T t, U u) throws E, E2, E3;
    //        }
    //
    //        public static interface TriPredicate<A, B, C, E extends Exception, E2 extends Exception, E3 extends Exception> {
    //            boolean test(A a, B b, C c) throws E, E2, E3;
    //        }
    //
    //        public static interface Function<T, R, E extends Exception, E2 extends Exception, E3 extends Exception> {
    //            R apply(T t) throws E, E2, E3;
    //        }
    //
    //        public static interface BiFunction<T, U, R, E extends Exception, E2 extends Exception, E3 extends Exception> {
    //            R apply(T t, U u) throws E, E2, E3;
    //        }
    //
    //        public static interface TriFunction<A, B, C, R, E extends Exception, E2 extends Exception, E3 extends Exception> {
    //            R apply(A a, B b, C c) throws E, E2, E3;
    //        }
    //
    //        public static interface Consumer<T, E extends Exception, E2 extends Exception, E3 extends Exception> {
    //            void accept(T t) throws E, E2, E3;
    //        }
    //
    //        public static interface BiConsumer<T, U, E extends Exception, E2 extends Exception, E3 extends Exception> {
    //            void accept(T t, U u) throws E, E2, E3;
    //        }
    //
    //        public static interface TriConsumer<A, B, C, E extends Exception, E2 extends Exception, E3 extends Exception> {
    //            void accept(A a, B b, C c) throws E, E2, E3;
    //        }
    //    }
}
