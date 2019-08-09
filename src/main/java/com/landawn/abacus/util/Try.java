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

// TODO: Auto-generated Javadoc
/**
 * Catch checked exception and convert it to <code>RuntimeException</code>.
 *
 * @author Haiyang Li
 * @param <T> the generic type
 * @since 0.8
 */
public final class Try<T extends AutoCloseable> {

    /** The t. */
    private final T t;

    /**
     * Instantiates a new try.
     *
     * @param t the t
     */
    Try(final T t) {
        N.checkArgNotNull(t);

        this.t = t;
    }

    /**
     * Of.
     *
     * @param <T> the generic type
     * @param t the t
     * @return the try
     */
    public static <T extends AutoCloseable> Try<T> of(final T t) {
        return new Try<>(t);
    }

    /**
     * Of.
     *
     * @param <T> the generic type
     * @param supplier the supplier
     * @return the try
     */
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
     * Run.
     *
     * @param cmd the cmd
     * @throws RuntimeException if some error happens
     */
    public static void run(final Try.Runnable<? extends Exception> cmd) {
        try {
            cmd.run();
        } catch (Exception e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     * Run.
     *
     * @param cmd the cmd
     * @param actionOnError the action on error
     */
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
     * Call.
     *
     * @param <R> the generic type
     * @param cmd the cmd
     * @return the r
     * @throws RuntimeException if some error happens
     */
    public static <R> R call(final java.util.concurrent.Callable<R> cmd) {
        try {
            return cmd.call();
        } catch (Exception e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     * Call.
     *
     * @param <R> the generic type
     * @param cmd the cmd
     * @param actionOnError the action on error
     * @return the r
     */
    public static <R> R call(final java.util.concurrent.Callable<R> cmd, final com.landawn.abacus.util.function.Function<? super Exception, R> actionOnError) {
        N.checkArgNotNull(actionOnError);

        try {
            return cmd.call();
        } catch (Exception e) {
            return actionOnError.apply(e);
        }
    }

    /**
     * Call.
     *
     * @param <R> the generic type
     * @param cmd the cmd
     * @param supplier the supplier
     * @return the r
     */
    public static <R> R call(final java.util.concurrent.Callable<R> cmd, final com.landawn.abacus.util.function.Supplier<R> supplier) {
        N.checkArgNotNull(supplier);

        try {
            return cmd.call();
        } catch (Exception e) {
            return supplier.get();
        }
    }

    /**
     * Call.
     *
     * @param <R> the generic type
     * @param cmd the cmd
     * @param defaultValue the default value
     * @return the r
     */
    public static <R> R call(final java.util.concurrent.Callable<R> cmd, final R defaultValue) {
        try {
            return cmd.call();
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Call.
     *
     * @param <R> the generic type
     * @param cmd the cmd
     * @param predicate the predicate
     * @param supplier the supplier
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
     * Call.
     *
     * @param <R> the generic type
     * @param cmd the cmd
     * @param predicate the predicate
     * @param defaultValue the default value
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

    /**
     * Val.
     *
     * @return the t
     */
    public T val() {
        return t;
    }

    /**
     * Run.
     *
     * @param cmd the cmd
     */
    public void run(final Try.Consumer<? super T, ? extends Exception> cmd) {
        try {
            cmd.accept(t);
        } catch (Exception e) {
            throw N.toRuntimeException(e);
        } finally {
            IOUtil.close(t);
        }
    }

    /**
     * Run.
     *
     * @param cmd the cmd
     * @param actionOnError the action on error
     */
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

    /**
     * Call.
     *
     * @param <R> the generic type
     * @param cmd the cmd
     * @return the r
     */
    public <R> R call(final Try.Function<? super T, R, ? extends Exception> cmd) {
        try {
            return cmd.apply(t);
        } catch (Exception e) {
            throw N.toRuntimeException(e);
        } finally {
            IOUtil.close(t);
        }
    }

    /**
     * Call.
     *
     * @param <R> the generic type
     * @param cmd the cmd
     * @param actionOnError the action on error
     * @return the r
     */
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

    /**
     * Call.
     *
     * @param <R> the generic type
     * @param cmd the cmd
     * @param supplier the supplier
     * @return the r
     */
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

    /**
     * Call.
     *
     * @param <R> the generic type
     * @param cmd the cmd
     * @param defaultValue the default value
     * @return the r
     */
    public <R> R call(final Try.Function<? super T, R, ? extends Exception> cmd, final R defaultValue) {
        try {
            return cmd.apply(t);
        } catch (Exception e) {
            return defaultValue;
        } finally {
            IOUtil.close(t);
        }
    }

    /**
     * Call.
     *
     * @param <R> the generic type
     * @param cmd the cmd
     * @param predicate the predicate
     * @param supplier the supplier
     * @return the r
     */
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

    /**
     * Call.
     *
     * @param <R> the generic type
     * @param cmd the cmd
     * @param predicate the predicate
     * @param defaultValue the default value
     * @return the r
     */
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

    /**
     * The Interface Runnable.
     *
     * @param <E> the element type
     */
    public static interface Runnable<E extends Exception> {

        /**
         * Run.
         *
         * @throws E the e
         */
        void run() throws E;
    }

    /**
     * The Interface Callable.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface Callable<R, E extends Exception> extends java.util.concurrent.Callable<R> {

        /**
         * Call.
         *
         * @return the r
         * @throws E the e
         */
        @Override
        R call() throws E;
    }

    /**
     * The Interface Supplier.
     *
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface Supplier<T, E extends Exception> {

        /**
         * Gets the.
         *
         * @return the t
         * @throws E the e
         */
        T get() throws E;
    }

    /**
     * The Interface BooleanSupplier.
     *
     * @param <E> the element type
     */
    public static interface BooleanSupplier<E extends Exception> {

        /**
         * Gets the as boolean.
         *
         * @return the as boolean
         * @throws E the e
         */
        boolean getAsBoolean() throws E;
    }

    /**
     * The Interface CharSupplier.
     *
     * @param <E> the element type
     */
    public static interface CharSupplier<E extends Exception> {

        /**
         * Gets the as char.
         *
         * @return the as char
         * @throws E the e
         */
        char getAsChar() throws E;
    }

    /**
     * The Interface ByteSupplier.
     *
     * @param <E> the element type
     */
    public static interface ByteSupplier<E extends Exception> {

        /**
         * Gets the as byte.
         *
         * @return the as byte
         * @throws E the e
         */
        byte getAsByte() throws E;
    }

    /**
     * The Interface ShortSupplier.
     *
     * @param <E> the element type
     */
    public static interface ShortSupplier<E extends Exception> {

        /**
         * Gets the as short.
         *
         * @return the as short
         * @throws E the e
         */
        short getAsShort() throws E;
    }

    /**
     * The Interface IntSupplier.
     *
     * @param <E> the element type
     */
    public static interface IntSupplier<E extends Exception> {

        /**
         * Gets the as int.
         *
         * @return the as int
         * @throws E the e
         */
        int getAsInt() throws E;
    }

    /**
     * The Interface LongSupplier.
     *
     * @param <E> the element type
     */
    public static interface LongSupplier<E extends Exception> {

        /**
         * Gets the as long.
         *
         * @return the as long
         * @throws E the e
         */
        long getAsLong() throws E;
    }

    /**
     * The Interface FloatSupplier.
     *
     * @param <E> the element type
     */
    public static interface FloatSupplier<E extends Exception> {

        /**
         * Gets the as float.
         *
         * @return the as float
         * @throws E the e
         */
        float getAsFloat() throws E;
    }

    /**
     * The Interface DoubleSupplier.
     *
     * @param <E> the element type
     */
    public static interface DoubleSupplier<E extends Exception> {

        /**
         * Gets the as double.
         *
         * @return the as double
         * @throws E the e
         */
        double getAsDouble() throws E;
    }

    /**
     * The Interface Predicate.
     *
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface Predicate<T, E extends Exception> {

        /**
         * Test.
         *
         * @param t the t
         * @return true, if successful
         * @throws E the e
         */
        boolean test(T t) throws E;
    }

    /**
     * The Interface BiPredicate.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param <E> the element type
     */
    public static interface BiPredicate<T, U, E extends Exception> {

        /**
         * Test.
         *
         * @param t the t
         * @param u the u
         * @return true, if successful
         * @throws E the e
         */
        boolean test(T t, U u) throws E;
    }

    /**
     * The Interface TriPredicate.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @param <E> the element type
     */
    public static interface TriPredicate<A, B, C, E extends Exception> {

        /**
         * Test.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return true, if successful
         * @throws E the e
         */
        boolean test(A a, B b, C c) throws E;
    }

    /**
     * The Interface QuadPredicate.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @param <D> the generic type
     * @param <E> the element type
     */
    public static interface QuadPredicate<A, B, C, D, E extends Exception> {

        /**
         * Test.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @param d the d
         * @return true, if successful
         * @throws E the e
         */
        boolean test(A a, B b, C c, D d) throws E;
    }

    /**
     * The Interface Function.
     *
     * @param <T> the generic type
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface Function<T, R, E extends Exception> {

        /**
         * Apply.
         *
         * @param t the t
         * @return the r
         * @throws E the e
         */
        R apply(T t) throws E;

        /**
         * Convert.
         *
         * @param <T> the generic type
         * @param <E> the element type
         * @param consumer the consumer
         * @return the function
         */
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

    /**
     * The Interface BiFunction.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface BiFunction<T, U, R, E extends Exception> {

        /**
         * Apply.
         *
         * @param t the t
         * @param u the u
         * @return the r
         * @throws E the e
         */
        R apply(T t, U u) throws E;

        /**
         * Convert.
         *
         * @param <T> the generic type
         * @param <U> the generic type
         * @param <E> the element type
         * @param biConsumer the bi consumer
         * @return the bi function
         */
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

    /**
     * The Interface TriFunction.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface TriFunction<A, B, C, R, E extends Exception> {

        /**
         * Apply.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return the r
         * @throws E the e
         */
        R apply(A a, B b, C c) throws E;

        /**
         * Convert.
         *
         * @param <A> the generic type
         * @param <B> the generic type
         * @param <C> the generic type
         * @param <E> the element type
         * @param triConsumer the tri consumer
         * @return the tri function
         */
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

    /**
     * The Interface QuadFunction.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @param <D> the generic type
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface QuadFunction<A, B, C, D, R, E extends Exception> {

        /**
         * Apply.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @param d the d
         * @return the r
         * @throws E the e
         */
        R apply(A a, B b, C c, D d) throws E;
    }

    /**
     * The Interface Consumer.
     *
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface Consumer<T, E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @throws E the e
         */
        void accept(T t) throws E;

        /**
         * Convert.
         *
         * @param <T> the generic type
         * @param <R> the generic type
         * @param <E> the element type
         * @param func the func
         * @return the consumer
         */
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

    /**
     * The Interface BiConsumer.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param <E> the element type
     */
    public static interface BiConsumer<T, U, E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @param u the u
         * @throws E the e
         */
        void accept(T t, U u) throws E;

        /**
         * Convert.
         *
         * @param <T> the generic type
         * @param <U> the generic type
         * @param <R> the generic type
         * @param <E> the element type
         * @param func the func
         * @return the bi consumer
         */
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

    /**
     * The Interface TriConsumer.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @param <E> the element type
     */
    public static interface TriConsumer<A, B, C, E extends Exception> {

        /**
         * Accept.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @throws E the e
         */
        void accept(A a, B b, C c) throws E;

        /**
         * Convert.
         *
         * @param <A> the generic type
         * @param <B> the generic type
         * @param <C> the generic type
         * @param <R> the generic type
         * @param <E> the element type
         * @param func the func
         * @return the tri consumer
         */
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

    /**
     * The Interface QuadConsumer.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @param <D> the generic type
     * @param <E> the element type
     */
    public static interface QuadConsumer<A, B, C, D, E extends Exception> {

        /**
         * Accept.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @param d the d
         * @throws E the e
         */
        void accept(A a, B b, C c, D d) throws E;
    }

    /**
     * The Interface IndexedConsumer.
     *
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface IndexedConsumer<T, E extends Exception> {

        /**
         * Accept.
         *
         * @param idx the idx
         * @param e the e
         * @throws E the e
         */
        void accept(int idx, T e) throws E;
    }

    /**
     * The Interface IndexedBiConsumer.
     *
     * @param <U> the generic type
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface IndexedBiConsumer<U, T, E extends Exception> {

        /**
         * Accept.
         *
         * @param u the u
         * @param idx the idx
         * @param e the e
         * @throws E the e
         */
        void accept(U u, int idx, T e) throws E;
    }

    /**
     * The Interface IndexedFunction.
     *
     * @param <T> the generic type
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface IndexedFunction<T, R, E extends Exception> {

        /**
         * Apply.
         *
         * @param idx the idx
         * @param e the e
         * @return the r
         * @throws E the e
         */
        R apply(int idx, T e) throws E;
    }

    /**
     * The Interface IndexedBiFunction.
     *
     * @param <U> the generic type
     * @param <T> the generic type
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface IndexedBiFunction<U, T, R, E extends Exception> {

        /**
         * Apply.
         *
         * @param u the u
         * @param idx the idx
         * @param e the e
         * @return the r
         * @throws E the e
         */
        R apply(U u, int idx, T e) throws E;
    }

    /**
     * The Interface IndexedPredicate.
     *
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface IndexedPredicate<T, E extends Exception> {

        /**
         * Test.
         *
         * @param idx the idx
         * @param e the e
         * @return true, if successful
         * @throws E the e
         */
        boolean test(int idx, T e) throws E;
    }

    /**
     * The Interface IndexedBiPredicate.
     *
     * @param <U> the generic type
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface IndexedBiPredicate<U, T, E extends Exception> {

        /**
         * Test.
         *
         * @param u the u
         * @param idx the idx
         * @param e the e
         * @return true, if successful
         * @throws E the e
         */
        boolean test(U u, int idx, T e) throws E;
    }

    /**
     * The Interface BooleanPredicate.
     *
     * @param <E> the element type
     */
    public static interface BooleanPredicate<E extends Exception> {

        /**
         * Test.
         *
         * @param value the value
         * @return true, if successful
         * @throws E the e
         */
        boolean test(boolean value) throws E;
    }

    /**
     * The Interface BooleanFunction.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface BooleanFunction<R, E extends Exception> {

        /**
         * Apply.
         *
         * @param value the value
         * @return the r
         * @throws E the e
         */
        R apply(boolean value) throws E;
    }

    /**
     * The Interface BooleanConsumer.
     *
     * @param <E> the element type
     */
    public static interface BooleanConsumer<E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @throws E the e
         */
        void accept(boolean t) throws E;
    }

    /**
     * The Interface CharPredicate.
     *
     * @param <E> the element type
     */
    public static interface CharPredicate<E extends Exception> {

        /**
         * Test.
         *
         * @param value the value
         * @return true, if successful
         * @throws E the e
         */
        boolean test(char value) throws E;
    }

    /**
     * The Interface CharFunction.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface CharFunction<R, E extends Exception> {

        /**
         * Apply.
         *
         * @param value the value
         * @return the r
         * @throws E the e
         */
        R apply(char value) throws E;
    }

    /**
     * The Interface CharConsumer.
     *
     * @param <E> the element type
     */
    public static interface CharConsumer<E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @throws E the e
         */
        void accept(char t) throws E;
    }

    /**
     * The Interface BytePredicate.
     *
     * @param <E> the element type
     */
    public static interface BytePredicate<E extends Exception> {

        /**
         * Test.
         *
         * @param value the value
         * @return true, if successful
         * @throws E the e
         */
        boolean test(byte value) throws E;
    }

    /**
     * The Interface ByteFunction.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface ByteFunction<R, E extends Exception> {

        /**
         * Apply.
         *
         * @param value the value
         * @return the r
         * @throws E the e
         */
        R apply(byte value) throws E;
    }

    /**
     * The Interface ByteConsumer.
     *
     * @param <E> the element type
     */
    public static interface ByteConsumer<E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @throws E the e
         */
        void accept(byte t) throws E;
    }

    /**
     * The Interface ShortPredicate.
     *
     * @param <E> the element type
     */
    public static interface ShortPredicate<E extends Exception> {

        /**
         * Test.
         *
         * @param value the value
         * @return true, if successful
         * @throws E the e
         */
        boolean test(short value) throws E;
    }

    /**
     * The Interface ShortFunction.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface ShortFunction<R, E extends Exception> {

        /**
         * Apply.
         *
         * @param value the value
         * @return the r
         * @throws E the e
         */
        R apply(short value) throws E;
    }

    /**
     * The Interface ShortConsumer.
     *
     * @param <E> the element type
     */
    public static interface ShortConsumer<E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @throws E the e
         */
        void accept(short t) throws E;
    }

    /**
     * The Interface IntPredicate.
     *
     * @param <E> the element type
     */
    public static interface IntPredicate<E extends Exception> {

        /**
         * Test.
         *
         * @param value the value
         * @return true, if successful
         * @throws E the e
         */
        boolean test(int value) throws E;
    }

    /**
     * The Interface IntFunction.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface IntFunction<R, E extends Exception> {

        /**
         * Apply.
         *
         * @param value the value
         * @return the r
         * @throws E the e
         */
        R apply(int value) throws E;
    }

    /**
     * The Interface IntConsumer.
     *
     * @param <E> the element type
     */
    public static interface IntConsumer<E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @throws E the e
         */
        void accept(int t) throws E;
    }

    /**
     * The Interface LongPredicate.
     *
     * @param <E> the element type
     */
    public static interface LongPredicate<E extends Exception> {

        /**
         * Test.
         *
         * @param value the value
         * @return true, if successful
         * @throws E the e
         */
        boolean test(long value) throws E;
    }

    /**
     * The Interface LongFunction.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface LongFunction<R, E extends Exception> {

        /**
         * Apply.
         *
         * @param value the value
         * @return the r
         * @throws E the e
         */
        R apply(long value) throws E;
    }

    /**
     * The Interface LongConsumer.
     *
     * @param <E> the element type
     */
    public static interface LongConsumer<E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @throws E the e
         */
        void accept(long t) throws E;
    }

    /**
     * The Interface FloatPredicate.
     *
     * @param <E> the element type
     */
    public static interface FloatPredicate<E extends Exception> {

        /**
         * Test.
         *
         * @param value the value
         * @return true, if successful
         * @throws E the e
         */
        boolean test(float value) throws E;
    }

    /**
     * The Interface FloatFunction.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface FloatFunction<R, E extends Exception> {

        /**
         * Apply.
         *
         * @param value the value
         * @return the r
         * @throws E the e
         */
        R apply(float value) throws E;
    }

    /**
     * The Interface FloatConsumer.
     *
     * @param <E> the element type
     */
    public static interface FloatConsumer<E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @throws E the e
         */
        void accept(float t) throws E;
    }

    /**
     * The Interface DoublePredicate.
     *
     * @param <E> the element type
     */
    public static interface DoublePredicate<E extends Exception> {

        /**
         * Test.
         *
         * @param value the value
         * @return true, if successful
         * @throws E the e
         */
        boolean test(double value) throws E;
    }

    /**
     * The Interface DoubleFunction.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface DoubleFunction<R, E extends Exception> {

        /**
         * Apply.
         *
         * @param value the value
         * @return the r
         * @throws E the e
         */
        R apply(double value) throws E;
    }

    /**
     * The Interface DoubleConsumer.
     *
     * @param <E> the element type
     */
    public static interface DoubleConsumer<E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @throws E the e
         */
        void accept(double t) throws E;
    }

    /**
     * The Interface ToBooleanFunction.
     *
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface ToBooleanFunction<T, E extends Exception> {

        /**
         * Apply as boolean.
         *
         * @param t the t
         * @return true, if successful
         * @throws E the e
         */
        boolean applyAsBoolean(T t) throws E;
    }

    /**
     * The Interface ToCharFunction.
     *
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface ToCharFunction<T, E extends Exception> {

        /**
         * Apply as char.
         *
         * @param t the t
         * @return the char
         * @throws E the e
         */
        char applyAsChar(T t) throws E;
    }

    /**
     * The Interface ToByteFunction.
     *
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface ToByteFunction<T, E extends Exception> {

        /**
         * Apply as byte.
         *
         * @param t the t
         * @return the byte
         * @throws E the e
         */
        byte applyAsByte(T t) throws E;
    }

    /**
     * The Interface ToShortFunction.
     *
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface ToShortFunction<T, E extends Exception> {

        /**
         * Apply as short.
         *
         * @param t the t
         * @return the short
         * @throws E the e
         */
        short applyAsShort(T t) throws E;
    }

    /**
     * The Interface ToIntFunction.
     *
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface ToIntFunction<T, E extends Exception> {

        /**
         * Apply as int.
         *
         * @param t the t
         * @return the int
         * @throws E the e
         */
        int applyAsInt(T t) throws E;
    }

    /**
     * The Interface ToLongFunction.
     *
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface ToLongFunction<T, E extends Exception> {

        /**
         * Apply as long.
         *
         * @param t the t
         * @return the long
         * @throws E the e
         */
        long applyAsLong(T t) throws E;
    }

    /**
     * The Interface ToFloatFunction.
     *
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface ToFloatFunction<T, E extends Exception> {

        /**
         * Apply as float.
         *
         * @param t the t
         * @return the float
         * @throws E the e
         */
        float applyAsFloat(T t) throws E;
    }

    /**
     * The Interface ToDoubleFunction.
     *
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface ToDoubleFunction<T, E extends Exception> {

        /**
         * Apply as double.
         *
         * @param t the t
         * @return the double
         * @throws E the e
         */
        double applyAsDouble(T t) throws E;
    }

    /**
     * The Interface UnaryOperator.
     *
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface UnaryOperator<T, E extends Exception> extends Function<T, T, E> {
    }

    /**
     * The Interface BinaryOperator.
     *
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface BinaryOperator<T, E extends Exception> extends BiFunction<T, T, T, E> {
    }

    /**
     * The Interface TernaryOperator.
     *
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface TernaryOperator<T, E extends Exception> extends BiFunction<T, T, T, E> {
    }

    /**
     * The Interface BooleanUnaryOperator.
     *
     * @param <E> the element type
     */
    public static interface BooleanUnaryOperator<E extends Exception> {

        /**
         * Apply as boolean.
         *
         * @param operand the operand
         * @return true, if successful
         * @throws E the e
         */
        boolean applyAsBoolean(boolean operand) throws E;
    }

    /**
     * The Interface CharUnaryOperator.
     *
     * @param <E> the element type
     */
    public static interface CharUnaryOperator<E extends Exception> {

        /**
         * Apply as char.
         *
         * @param operand the operand
         * @return the char
         * @throws E the e
         */
        char applyAsChar(char operand) throws E;
    }

    /**
     * The Interface ByteUnaryOperator.
     *
     * @param <E> the element type
     */
    public static interface ByteUnaryOperator<E extends Exception> {

        /**
         * Apply as byte.
         *
         * @param operand the operand
         * @return the byte
         * @throws E the e
         */
        byte applyAsByte(byte operand) throws E;
    }

    /**
     * The Interface ShortUnaryOperator.
     *
     * @param <E> the element type
     */
    public static interface ShortUnaryOperator<E extends Exception> {

        /**
         * Apply as short.
         *
         * @param operand the operand
         * @return the short
         * @throws E the e
         */
        short applyAsShort(short operand) throws E;
    }

    /**
     * The Interface IntUnaryOperator.
     *
     * @param <E> the element type
     */
    public static interface IntUnaryOperator<E extends Exception> {

        /**
         * Apply as int.
         *
         * @param operand the operand
         * @return the int
         * @throws E the e
         */
        int applyAsInt(int operand) throws E;
    }

    /**
     * The Interface LongUnaryOperator.
     *
     * @param <E> the element type
     */
    public static interface LongUnaryOperator<E extends Exception> {

        /**
         * Apply as long.
         *
         * @param operand the operand
         * @return the long
         * @throws E the e
         */
        long applyAsLong(long operand) throws E;
    }

    /**
     * The Interface FloatUnaryOperator.
     *
     * @param <E> the element type
     */
    public static interface FloatUnaryOperator<E extends Exception> {

        /**
         * Apply as float.
         *
         * @param operand the operand
         * @return the float
         * @throws E the e
         */
        float applyAsFloat(float operand) throws E;
    }

    /**
     * The Interface DoubleUnaryOperator.
     *
     * @param <E> the element type
     */
    public static interface DoubleUnaryOperator<E extends Exception> {

        /**
         * Apply as double.
         *
         * @param operand the operand
         * @return the double
         * @throws E the e
         */
        double applyAsDouble(double operand) throws E;
    }

    /**
     * The Interface BooleanBinaryOperator.
     *
     * @param <E> the element type
     */
    public static interface BooleanBinaryOperator<E extends Exception> {

        /**
         * Apply as boolean.
         *
         * @param left the left
         * @param right the right
         * @return true, if successful
         * @throws E the e
         */
        boolean applyAsBoolean(boolean left, boolean right) throws E;
    }

    /**
     * The Interface CharBinaryOperator.
     *
     * @param <E> the element type
     */
    public static interface CharBinaryOperator<E extends Exception> {

        /**
         * Apply as char.
         *
         * @param left the left
         * @param right the right
         * @return the char
         * @throws E the e
         */
        char applyAsChar(char left, char right) throws E;
    }

    /**
     * The Interface ByteBinaryOperator.
     *
     * @param <E> the element type
     */
    public static interface ByteBinaryOperator<E extends Exception> {

        /**
         * Apply as byte.
         *
         * @param left the left
         * @param right the right
         * @return the byte
         * @throws E the e
         */
        byte applyAsByte(byte left, byte right) throws E;
    }

    /**
     * The Interface ShortBinaryOperator.
     *
     * @param <E> the element type
     */
    public static interface ShortBinaryOperator<E extends Exception> {

        /**
         * Apply as short.
         *
         * @param left the left
         * @param right the right
         * @return the short
         * @throws E the e
         */
        short applyAsShort(short left, short right) throws E;
    }

    /**
     * The Interface IntBinaryOperator.
     *
     * @param <E> the element type
     */
    public static interface IntBinaryOperator<E extends Exception> {

        /**
         * Apply as int.
         *
         * @param left the left
         * @param right the right
         * @return the int
         * @throws E the e
         */
        int applyAsInt(int left, int right) throws E;
    }

    /**
     * The Interface LongBinaryOperator.
     *
     * @param <E> the element type
     */
    public static interface LongBinaryOperator<E extends Exception> {

        /**
         * Apply as long.
         *
         * @param left the left
         * @param right the right
         * @return the long
         * @throws E the e
         */
        long applyAsLong(long left, long right) throws E;
    }

    /**
     * The Interface FloatBinaryOperator.
     *
     * @param <E> the element type
     */
    public static interface FloatBinaryOperator<E extends Exception> {

        /**
         * Apply as float.
         *
         * @param left the left
         * @param right the right
         * @return the float
         * @throws E the e
         */
        float applyAsFloat(float left, float right) throws E;
    }

    /**
     * The Interface DoubleBinaryOperator.
     *
     * @param <E> the element type
     */
    public static interface DoubleBinaryOperator<E extends Exception> {

        /**
         * Apply as double.
         *
         * @param left the left
         * @param right the right
         * @return the double
         * @throws E the e
         */
        double applyAsDouble(double left, double right) throws E;
    }

    /**
     * The Interface BooleanTernaryOperator.
     *
     * @param <E> the element type
     */
    public static interface BooleanTernaryOperator<E extends Exception> {

        /**
         * Apply as boolean.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return true, if successful
         * @throws E the e
         */
        boolean applyAsBoolean(boolean a, boolean b, boolean c) throws E;
    }

    /**
     * The Interface CharTernaryOperator.
     *
     * @param <E> the element type
     */
    public static interface CharTernaryOperator<E extends Exception> {

        /**
         * Apply as char.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return the char
         * @throws E the e
         */
        char applyAsChar(char a, char b, char c) throws E;
    }

    /**
     * The Interface ByteTernaryOperator.
     *
     * @param <E> the element type
     */
    public static interface ByteTernaryOperator<E extends Exception> {

        /**
         * Apply as byte.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return the byte
         * @throws E the e
         */
        byte applyAsByte(byte a, byte b, byte c) throws E;
    }

    /**
     * The Interface ShortTernaryOperator.
     *
     * @param <E> the element type
     */
    public static interface ShortTernaryOperator<E extends Exception> {

        /**
         * Apply as short.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return the short
         * @throws E the e
         */
        short applyAsShort(short a, short b, short c) throws E;
    }

    /**
     * The Interface IntTernaryOperator.
     *
     * @param <E> the element type
     */
    public static interface IntTernaryOperator<E extends Exception> {

        /**
         * Apply as int.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return the int
         * @throws E the e
         */
        int applyAsInt(int a, int b, int c) throws E;
    }

    /**
     * The Interface LongTernaryOperator.
     *
     * @param <E> the element type
     */
    public static interface LongTernaryOperator<E extends Exception> {

        /**
         * Apply as long.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return the long
         * @throws E the e
         */
        long applyAsLong(long a, long b, long c) throws E;
    }

    /**
     * The Interface FloatTernaryOperator.
     *
     * @param <E> the element type
     */
    public static interface FloatTernaryOperator<E extends Exception> {

        /**
         * Apply as float.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return the float
         * @throws E the e
         */
        float applyAsFloat(float a, float b, float c) throws E;
    }

    /**
     * The Interface DoubleTernaryOperator.
     *
     * @param <E> the element type
     */
    public static interface DoubleTernaryOperator<E extends Exception> {

        /**
         * Apply as double.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return the double
         * @throws E the e
         */
        double applyAsDouble(double a, double b, double c) throws E;
    }

    /**
     * The Interface BooleanBiPredicate.
     *
     * @param <E> the element type
     */
    public static interface BooleanBiPredicate<E extends Exception> {

        /**
         * Test.
         *
         * @param t the t
         * @param u the u
         * @return true, if successful
         * @throws E the e
         */
        boolean test(boolean t, boolean u) throws E;
    }

    /**
     * The Interface CharBiPredicate.
     *
     * @param <E> the element type
     */
    public static interface CharBiPredicate<E extends Exception> {

        /**
         * Test.
         *
         * @param t the t
         * @param u the u
         * @return true, if successful
         * @throws E the e
         */
        boolean test(char t, char u) throws E;
    }

    /**
     * The Interface ByteBiPredicate.
     *
     * @param <E> the element type
     */
    public static interface ByteBiPredicate<E extends Exception> {

        /**
         * Test.
         *
         * @param t the t
         * @param u the u
         * @return true, if successful
         * @throws E the e
         */
        boolean test(byte t, byte u) throws E;
    }

    /**
     * The Interface ShortBiPredicate.
     *
     * @param <E> the element type
     */
    public static interface ShortBiPredicate<E extends Exception> {

        /**
         * Test.
         *
         * @param t the t
         * @param u the u
         * @return true, if successful
         * @throws E the e
         */
        boolean test(short t, short u) throws E;
    }

    /**
     * The Interface IntBiPredicate.
     *
     * @param <E> the element type
     */
    public static interface IntBiPredicate<E extends Exception> {

        /**
         * Test.
         *
         * @param t the t
         * @param u the u
         * @return true, if successful
         * @throws E the e
         */
        boolean test(int t, int u) throws E;
    }

    /**
     * The Interface LongBiPredicate.
     *
     * @param <E> the element type
     */
    public static interface LongBiPredicate<E extends Exception> {

        /**
         * Test.
         *
         * @param t the t
         * @param u the u
         * @return true, if successful
         * @throws E the e
         */
        boolean test(long t, long u) throws E;
    }

    /**
     * The Interface FloatBiPredicate.
     *
     * @param <E> the element type
     */
    public static interface FloatBiPredicate<E extends Exception> {

        /**
         * Test.
         *
         * @param t the t
         * @param u the u
         * @return true, if successful
         * @throws E the e
         */
        boolean test(float t, float u) throws E;
    }

    /**
     * The Interface DoubleBiPredicate.
     *
     * @param <E> the element type
     */
    public static interface DoubleBiPredicate<E extends Exception> {

        /**
         * Test.
         *
         * @param t the t
         * @param u the u
         * @return true, if successful
         * @throws E the e
         */
        boolean test(double t, double u) throws E;
    }

    /**
     * The Interface BooleanBiFunction.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface BooleanBiFunction<R, E extends Exception> {

        /**
         * Apply.
         *
         * @param t the t
         * @param u the u
         * @return the r
         * @throws E the e
         */
        R apply(boolean t, boolean u) throws E;
    }

    /**
     * The Interface CharBiFunction.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface CharBiFunction<R, E extends Exception> {

        /**
         * Apply.
         *
         * @param t the t
         * @param u the u
         * @return the r
         * @throws E the e
         */
        R apply(char t, char u) throws E;
    }

    /**
     * The Interface ByteBiFunction.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface ByteBiFunction<R, E extends Exception> {

        /**
         * Apply.
         *
         * @param t the t
         * @param u the u
         * @return the r
         * @throws E the e
         */
        R apply(byte t, byte u) throws E;
    }

    /**
     * The Interface ShortBiFunction.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface ShortBiFunction<R, E extends Exception> {

        /**
         * Apply.
         *
         * @param t the t
         * @param u the u
         * @return the r
         * @throws E the e
         */
        R apply(short t, short u) throws E;
    }

    /**
     * The Interface IntBiFunction.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface IntBiFunction<R, E extends Exception> {

        /**
         * Apply.
         *
         * @param t the t
         * @param u the u
         * @return the r
         * @throws E the e
         */
        R apply(int t, int u) throws E;
    }

    /**
     * The Interface LongBiFunction.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface LongBiFunction<R, E extends Exception> {

        /**
         * Apply.
         *
         * @param t the t
         * @param u the u
         * @return the r
         * @throws E the e
         */
        R apply(long t, long u) throws E;
    }

    /**
     * The Interface FloatBiFunction.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface FloatBiFunction<R, E extends Exception> {

        /**
         * Apply.
         *
         * @param t the t
         * @param u the u
         * @return the r
         * @throws E the e
         */
        R apply(float t, float u) throws E;
    }

    /**
     * The Interface DoubleBiFunction.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface DoubleBiFunction<R, E extends Exception> {

        /**
         * Apply.
         *
         * @param t the t
         * @param u the u
         * @return the r
         * @throws E the e
         */
        R apply(double t, double u) throws E;
    }

    /**
     * The Interface BooleanBiConsumer.
     *
     * @param <E> the element type
     */
    public static interface BooleanBiConsumer<E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @param u the u
         * @throws E the e
         */
        void accept(boolean t, boolean u) throws E;
    }

    /**
     * The Interface CharBiConsumer.
     *
     * @param <E> the element type
     */
    public static interface CharBiConsumer<E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @param u the u
         * @throws E the e
         */
        void accept(char t, char u) throws E;
    }

    /**
     * The Interface ByteBiConsumer.
     *
     * @param <E> the element type
     */
    public static interface ByteBiConsumer<E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @param u the u
         * @throws E the e
         */
        void accept(byte t, byte u) throws E;
    }

    /**
     * The Interface ShortBiConsumer.
     *
     * @param <E> the element type
     */
    public static interface ShortBiConsumer<E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @param u the u
         * @throws E the e
         */
        void accept(short t, short u) throws E;
    }

    /**
     * The Interface IntBiConsumer.
     *
     * @param <E> the element type
     */
    public static interface IntBiConsumer<E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @param u the u
         * @throws E the e
         */
        void accept(int t, int u) throws E;
    }

    /**
     * The Interface LongBiConsumer.
     *
     * @param <E> the element type
     */
    public static interface LongBiConsumer<E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @param u the u
         * @throws E the e
         */
        void accept(long t, long u) throws E;
    }

    /**
     * The Interface FloatBiConsumer.
     *
     * @param <E> the element type
     */
    public static interface FloatBiConsumer<E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @param u the u
         * @throws E the e
         */
        void accept(float t, float u) throws E;
    }

    /**
     * The Interface DoubleBiConsumer.
     *
     * @param <E> the element type
     */
    public static interface DoubleBiConsumer<E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @param u the u
         * @throws E the e
         */
        void accept(double t, double u) throws E;
    }

    /**
     * The Interface BooleanTriPredicate.
     *
     * @param <E> the element type
     */
    public static interface BooleanTriPredicate<E extends Exception> {

        /**
         * Test.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return true, if successful
         * @throws E the e
         */
        boolean test(boolean a, boolean b, boolean c) throws E;
    }

    /**
     * The Interface CharTriPredicate.
     *
     * @param <E> the element type
     */
    public static interface CharTriPredicate<E extends Exception> {

        /**
         * Test.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return true, if successful
         * @throws E the e
         */
        boolean test(char a, char b, char c) throws E;
    }

    /**
     * The Interface ByteTriPredicate.
     *
     * @param <E> the element type
     */
    public static interface ByteTriPredicate<E extends Exception> {

        /**
         * Test.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return true, if successful
         * @throws E the e
         */
        boolean test(byte a, byte b, byte c) throws E;
    }

    /**
     * The Interface ShortTriPredicate.
     *
     * @param <E> the element type
     */
    public static interface ShortTriPredicate<E extends Exception> {

        /**
         * Test.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return true, if successful
         * @throws E the e
         */
        boolean test(short a, short b, short c) throws E;
    }

    /**
     * The Interface IntTriPredicate.
     *
     * @param <E> the element type
     */
    public static interface IntTriPredicate<E extends Exception> {

        /**
         * Test.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return true, if successful
         * @throws E the e
         */
        boolean test(int a, int b, int c) throws E;
    }

    /**
     * The Interface LongTriPredicate.
     *
     * @param <E> the element type
     */
    public static interface LongTriPredicate<E extends Exception> {

        /**
         * Test.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return true, if successful
         * @throws E the e
         */
        boolean test(long a, long b, long c) throws E;
    }

    /**
     * The Interface FloatTriPredicate.
     *
     * @param <E> the element type
     */
    public static interface FloatTriPredicate<E extends Exception> {

        /**
         * Test.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return true, if successful
         * @throws E the e
         */
        boolean test(float a, float b, float c) throws E;
    }

    /**
     * The Interface DoubleTriPredicate.
     *
     * @param <E> the element type
     */
    public static interface DoubleTriPredicate<E extends Exception> {

        /**
         * Test.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return true, if successful
         * @throws E the e
         */
        boolean test(double a, double b, double c) throws E;
    }

    /**
     * The Interface BooleanTriFunction.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface BooleanTriFunction<R, E extends Exception> {

        /**
         * Apply.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return the r
         * @throws E the e
         */
        R apply(boolean a, boolean b, boolean c) throws E;
    }

    /**
     * The Interface CharTriFunction.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface CharTriFunction<R, E extends Exception> {

        /**
         * Apply.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return the r
         * @throws E the e
         */
        R apply(char a, char b, char c) throws E;
    }

    /**
     * The Interface ByteTriFunction.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface ByteTriFunction<R, E extends Exception> {

        /**
         * Apply.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return the r
         * @throws E the e
         */
        R apply(byte a, byte b, byte c) throws E;
    }

    /**
     * The Interface ShortTriFunction.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface ShortTriFunction<R, E extends Exception> {

        /**
         * Apply.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return the r
         * @throws E the e
         */
        R apply(short a, short b, short c) throws E;
    }

    /**
     * The Interface IntTriFunction.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface IntTriFunction<R, E extends Exception> {

        /**
         * Apply.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return the r
         * @throws E the e
         */
        R apply(int a, int b, int c) throws E;
    }

    /**
     * The Interface LongTriFunction.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface LongTriFunction<R, E extends Exception> {

        /**
         * Apply.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return the r
         * @throws E the e
         */
        R apply(long a, long b, long c) throws E;
    }

    /**
     * The Interface FloatTriFunction.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface FloatTriFunction<R, E extends Exception> {

        /**
         * Apply.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return the r
         * @throws E the e
         */
        R apply(float a, float b, float c) throws E;
    }

    /**
     * The Interface DoubleTriFunction.
     *
     * @param <R> the generic type
     * @param <E> the element type
     */
    public static interface DoubleTriFunction<R, E extends Exception> {

        /**
         * Apply.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @return the r
         * @throws E the e
         */
        R apply(double a, double b, double c) throws E;
    }

    /**
     * The Interface BooleanTriConsumer.
     *
     * @param <E> the element type
     */
    public static interface BooleanTriConsumer<E extends Exception> {

        /**
         * Accept.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @throws E the e
         */
        void accept(boolean a, boolean b, boolean c) throws E;
    }

    /**
     * The Interface CharTriConsumer.
     *
     * @param <E> the element type
     */
    public static interface CharTriConsumer<E extends Exception> {

        /**
         * Accept.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @throws E the e
         */
        void accept(char a, char b, char c) throws E;
    }

    /**
     * The Interface ByteTriConsumer.
     *
     * @param <E> the element type
     */
    public static interface ByteTriConsumer<E extends Exception> {

        /**
         * Accept.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @throws E the e
         */
        void accept(byte a, byte b, byte c) throws E;
    }

    /**
     * The Interface ShortTriConsumer.
     *
     * @param <E> the element type
     */
    public static interface ShortTriConsumer<E extends Exception> {

        /**
         * Accept.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @throws E the e
         */
        void accept(short a, short b, short c) throws E;
    }

    /**
     * The Interface IntTriConsumer.
     *
     * @param <E> the element type
     */
    public static interface IntTriConsumer<E extends Exception> {

        /**
         * Accept.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @throws E the e
         */
        void accept(int a, int b, int c) throws E;
    }

    /**
     * The Interface LongTriConsumer.
     *
     * @param <E> the element type
     */
    public static interface LongTriConsumer<E extends Exception> {

        /**
         * Accept.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @throws E the e
         */
        void accept(long a, long b, long c) throws E;
    }

    /**
     * The Interface FloatTriConsumer.
     *
     * @param <E> the element type
     */
    public static interface FloatTriConsumer<E extends Exception> {

        /**
         * Accept.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @throws E the e
         */
        void accept(float a, float b, float c) throws E;
    }

    /**
     * The Interface DoubleTriConsumer.
     *
     * @param <E> the element type
     */
    public static interface DoubleTriConsumer<E extends Exception> {

        /**
         * Accept.
         *
         * @param a the a
         * @param b the b
         * @param c the c
         * @throws E the e
         */
        void accept(double a, double b, double c) throws E;
    }

    /**
     * The Interface ObjBooleanConsumer.
     *
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface ObjBooleanConsumer<T, E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @param value the value
         * @throws E the e
         */
        void accept(T t, boolean value) throws E;
    }

    /**
     * The Interface ObjCharConsumer.
     *
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface ObjCharConsumer<T, E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @param value the value
         * @throws E the e
         */
        void accept(T t, char value) throws E;
    }

    /**
     * The Interface ObjByteConsumer.
     *
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface ObjByteConsumer<T, E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @param value the value
         * @throws E the e
         */
        void accept(T t, byte value) throws E;
    }

    /**
     * The Interface ObjShortConsumer.
     *
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface ObjShortConsumer<T, E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @param value the value
         * @throws E the e
         */
        void accept(T t, short value) throws E;
    }

    /**
     * The Interface ObjIntConsumer.
     *
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface ObjIntConsumer<T, E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @param value the value
         * @throws E the e
         */
        void accept(T t, int value) throws E;
    }

    /**
     * The Interface ObjLongConsumer.
     *
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface ObjLongConsumer<T, E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @param value the value
         * @throws E the e
         */
        void accept(T t, long value) throws E;
    }

    /**
     * The Interface ObjFloatConsumer.
     *
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface ObjFloatConsumer<T, E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @param value the value
         * @throws E the e
         */
        void accept(T t, float value) throws E;
    }

    /**
     * The Interface ObjDoubleConsumer.
     *
     * @param <T> the generic type
     * @param <E> the element type
     */
    public static interface ObjDoubleConsumer<T, E extends Exception> {

        /**
         * Accept.
         *
         * @param t the t
         * @param value the value
         * @throws E the e
         */
        void accept(T t, double value) throws E;
    }

    /**
     * The Class EE.
     */
    public static final class EE {

        /**
         * Instantiates a new ee.
         */
        private EE() {
            // Singleton. Utility class.
        }

        /**
         * The Interface Runnable.
         *
         * @param <E> the element type
         * @param <E2> the generic type
         */
        public static interface Runnable<E extends Exception, E2 extends Exception> {

            /**
             * Run.
             *
             * @throws E the e
             * @throws E2 the e2
             */
            void run() throws E, E2;
        }

        /**
         * The Interface Callable.
         *
         * @param <R> the generic type
         * @param <E> the element type
         * @param <E2> the generic type
         */
        public static interface Callable<R, E extends Exception, E2 extends Exception> extends java.util.concurrent.Callable<R> {

            /**
             * Call.
             *
             * @return the r
             * @throws E the e
             * @throws E2 the e2
             */
            @Override
            R call() throws E, E2;
        }

        /**
         * The Interface Supplier.
         *
         * @param <T> the generic type
         * @param <E> the element type
         * @param <E2> the generic type
         */
        public static interface Supplier<T, E extends Exception, E2 extends Exception> {

            /**
             * Gets the.
             *
             * @return the t
             * @throws E the e
             * @throws E2 the e2
             */
            T get() throws E, E2;
        }

        /**
         * The Interface Predicate.
         *
         * @param <T> the generic type
         * @param <E> the element type
         * @param <E2> the generic type
         */
        public static interface Predicate<T, E extends Exception, E2 extends Exception> {

            /**
             * Test.
             *
             * @param t the t
             * @return true, if successful
             * @throws E the e
             * @throws E2 the e2
             */
            boolean test(T t) throws E, E2;
        }

        /**
         * The Interface BiPredicate.
         *
         * @param <T> the generic type
         * @param <U> the generic type
         * @param <E> the element type
         * @param <E2> the generic type
         */
        public static interface BiPredicate<T, U, E extends Exception, E2 extends Exception> {

            /**
             * Test.
             *
             * @param t the t
             * @param u the u
             * @return true, if successful
             * @throws E the e
             * @throws E2 the e2
             */
            boolean test(T t, U u) throws E, E2;
        }

        /**
         * The Interface TriPredicate.
         *
         * @param <A> the generic type
         * @param <B> the generic type
         * @param <C> the generic type
         * @param <E> the element type
         * @param <E2> the generic type
         */
        public static interface TriPredicate<A, B, C, E extends Exception, E2 extends Exception> {

            /**
             * Test.
             *
             * @param a the a
             * @param b the b
             * @param c the c
             * @return true, if successful
             * @throws E the e
             * @throws E2 the e2
             */
            boolean test(A a, B b, C c) throws E, E2;
        }

        /**
         * The Interface Function.
         *
         * @param <T> the generic type
         * @param <R> the generic type
         * @param <E> the element type
         * @param <E2> the generic type
         */
        public static interface Function<T, R, E extends Exception, E2 extends Exception> {

            /**
             * Apply.
             *
             * @param t the t
             * @return the r
             * @throws E the e
             * @throws E2 the e2
             */
            R apply(T t) throws E, E2;
        }

        /**
         * The Interface BiFunction.
         *
         * @param <T> the generic type
         * @param <U> the generic type
         * @param <R> the generic type
         * @param <E> the element type
         * @param <E2> the generic type
         */
        public static interface BiFunction<T, U, R, E extends Exception, E2 extends Exception> {

            /**
             * Apply.
             *
             * @param t the t
             * @param u the u
             * @return the r
             * @throws E the e
             * @throws E2 the e2
             */
            R apply(T t, U u) throws E, E2;
        }

        /**
         * The Interface TriFunction.
         *
         * @param <A> the generic type
         * @param <B> the generic type
         * @param <C> the generic type
         * @param <R> the generic type
         * @param <E> the element type
         * @param <E2> the generic type
         */
        public static interface TriFunction<A, B, C, R, E extends Exception, E2 extends Exception> {

            /**
             * Apply.
             *
             * @param a the a
             * @param b the b
             * @param c the c
             * @return the r
             * @throws E the e
             * @throws E2 the e2
             */
            R apply(A a, B b, C c) throws E, E2;
        }

        /**
         * The Interface Consumer.
         *
         * @param <T> the generic type
         * @param <E> the element type
         * @param <E2> the generic type
         */
        public static interface Consumer<T, E extends Exception, E2 extends Exception> {

            /**
             * Accept.
             *
             * @param t the t
             * @throws E the e
             * @throws E2 the e2
             */
            void accept(T t) throws E, E2;
        }

        /**
         * The Interface BiConsumer.
         *
         * @param <T> the generic type
         * @param <U> the generic type
         * @param <E> the element type
         * @param <E2> the generic type
         */
        public static interface BiConsumer<T, U, E extends Exception, E2 extends Exception> {

            /**
             * Accept.
             *
             * @param t the t
             * @param u the u
             * @throws E the e
             * @throws E2 the e2
             */
            void accept(T t, U u) throws E, E2;
        }

        /**
         * The Interface TriConsumer.
         *
         * @param <A> the generic type
         * @param <B> the generic type
         * @param <C> the generic type
         * @param <E> the element type
         * @param <E2> the generic type
         */
        public static interface TriConsumer<A, B, C, E extends Exception, E2 extends Exception> {

            /**
             * Accept.
             *
             * @param a the a
             * @param b the b
             * @param c the c
             * @throws E the e
             * @throws E2 the e2
             */
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
