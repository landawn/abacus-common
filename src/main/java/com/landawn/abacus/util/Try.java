package com.landawn.abacus.util;

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

public final class Try<T extends AutoCloseable> {

    /** The t. */
    private final T t;

    /**
     * Instantiates a new try.
     *
     * @param t
     */
    Try(final T t) {
        N.checkArgNotNull(t);

        this.t = t;
    }

    /**
     *
     * @param <T>
     * @param t
     * @return
     */
    public static <T extends AutoCloseable> Try<T> with(final T t) {
        return new Try<>(t);
    }

    /**
     *
     * @param <T>
     * @param supplier
     * @return
     */
    public static <T extends AutoCloseable> Try<T> with(final Throwables.Supplier<T, ? extends Exception> supplier) {
        try {
            return new Try<>(supplier.get());
        } catch (Exception e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     * @param cmd
     * @throws RuntimeException if some error happens
     */
    public static void run(final Throwables.Runnable<? extends Exception> cmd) {
        try {
            cmd.run();
        } catch (Exception e) {
            throw N.toRuntimeException(e);
        }
    }

    /**
     *
     * @param cmd
     * @param actionOnError
     */
    public static void run(final Throwables.Runnable<? extends Exception> cmd,
            final com.landawn.abacus.util.function.Consumer<? super Exception> actionOnError) {
        N.checkArgNotNull(actionOnError);

        try {
            cmd.run();
        } catch (Exception e) {
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
    public static <R> R call(final java.util.concurrent.Callable<R> cmd) {
        try {
            return cmd.call();
        } catch (Exception e) {
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
    public static <R> R call(final java.util.concurrent.Callable<R> cmd, final com.landawn.abacus.util.function.Function<? super Exception, R> actionOnError) {
        N.checkArgNotNull(actionOnError);

        try {
            return cmd.call();
        } catch (Exception e) {
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
    public static <R> R call(final java.util.concurrent.Callable<R> cmd, final com.landawn.abacus.util.function.Supplier<R> supplier) {
        N.checkArgNotNull(supplier);

        try {
            return cmd.call();
        } catch (Exception e) {
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
    public static <R> R call(final java.util.concurrent.Callable<R> cmd, final R defaultValue) {
        try {
            return cmd.call();
        } catch (Exception e) {
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
     * @param <R>
     * @param cmd
     * @param predicate
     * @param defaultValue
     * @return
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
    //     *
    //     * @return
    //     */
    //    public T val() {
    //        return t;
    //    }

    /**
     *
     * @param cmd
     */
    public void run(final Throwables.Consumer<? super T, ? extends Exception> cmd) {
        try {
            cmd.accept(t);
        } catch (Exception e) {
            throw N.toRuntimeException(e);
        } finally {
            IOUtil.close(t);
        }
    }

    /**
     *
     * @param cmd
     * @param actionOnError
     */
    public void run(final Throwables.Consumer<? super T, ? extends Exception> cmd,
            final com.landawn.abacus.util.function.Consumer<? super Exception> actionOnError) {
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
     *
     * @param <R>
     * @param cmd
     * @return
     */
    public <R> R call(final Throwables.Function<? super T, R, ? extends Exception> cmd) {
        try {
            return cmd.apply(t);
        } catch (Exception e) {
            throw N.toRuntimeException(e);
        } finally {
            IOUtil.close(t);
        }
    }

    /**
     *
     * @param <R>
     * @param cmd
     * @param actionOnError
     * @return
     */
    public <R> R call(final Throwables.Function<? super T, R, ? extends Exception> cmd,
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
     *
     * @param <R>
     * @param cmd
     * @param supplier
     * @return
     */
    public <R> R call(final Throwables.Function<? super T, R, ? extends Exception> cmd, final com.landawn.abacus.util.function.Supplier<R> supplier) {
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
     *
     * @param <R>
     * @param cmd
     * @param defaultValue
     * @return
     */
    public <R> R call(final Throwables.Function<? super T, R, ? extends Exception> cmd, final R defaultValue) {
        try {
            return cmd.apply(t);
        } catch (Exception e) {
            return defaultValue;
        } finally {
            IOUtil.close(t);
        }
    }

    /**
     *
     * @param <R>
     * @param cmd
     * @param predicate
     * @param supplier
     * @return
     */
    public <R> R call(final Throwables.Function<? super T, R, ? extends Exception> cmd,
            final com.landawn.abacus.util.function.Predicate<? super Exception> predicate, final com.landawn.abacus.util.function.Supplier<R> supplier) {
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
     *
     * @param <R>
     * @param cmd
     * @param predicate
     * @param defaultValue
     * @return
     */
    public <R> R call(final Throwables.Function<? super T, R, ? extends Exception> cmd,
            final com.landawn.abacus.util.function.Predicate<? super Exception> predicate, final R defaultValue) {
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
}
