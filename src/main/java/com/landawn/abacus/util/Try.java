package com.landawn.abacus.util;

import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;

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
    private static final AutoCloseable EMPTY = new AutoCloseable() {
        @Override
        public void close() throws Exception {
            // Do nothing.
        }
    };

    private final T targetResource;
    private final Throwables.Supplier<T, ? extends Exception> targetResourceSupplier;
    private final Runnable finalAction;

    Try(final T targetResource, final Throwables.Supplier<T, ? extends Exception> targetResourceSupplier, final Runnable finalAction) {
        this.targetResource = targetResource;
        this.targetResourceSupplier = targetResourceSupplier;
        this.finalAction = finalAction;
    }

    public static <T extends AutoCloseable> Try<T> with(final T targetResource) {
        N.checkArgNotNull(targetResource, "targetResourceSupplier");

        return new Try<>(targetResource, null, null);
    }

    public static <T extends AutoCloseable> Try<T> with(final T targetResource, final Runnable finalAction) {
        N.checkArgNotNull(targetResource, "targetResourceSupplier");
        N.checkArgNotNull(finalAction, "finalAction");

        return new Try<>(targetResource, null, finalAction);
    }

    public static <T extends AutoCloseable> Try<T> with(final Throwables.Supplier<T, ? extends Exception> targetResourceSupplier) {
        N.checkArgNotNull(targetResourceSupplier, "targetResourceSupplier");
        return new Try<>(null, targetResourceSupplier, null);
    }

    public static <T extends AutoCloseable> Try<T> with(final Throwables.Supplier<T, ? extends Exception> targetResourceSupplier, final Runnable finalAction) {
        N.checkArgNotNull(targetResourceSupplier, "targetResourceSupplier");
        N.checkArgNotNull(finalAction, "finalAction");

        return new Try<>(null, targetResourceSupplier, finalAction);
    }

    /**
     *
     * @param <T>
     * @param targetResource
     * @return
     */
    public static <T extends AutoCloseable> Try<T> with(final Runnable finalAction) {
        N.checkArgNotNull(finalAction);

        return new Try<>(null, null, finalAction);
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
    public static void run(final Throwables.Runnable<? extends Exception> cmd, final Consumer<? super Exception> actionOnError) {
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
    public static <R> R call(final java.util.concurrent.Callable<R> cmd, final Function<? super Exception, R> actionOnError) {
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
    public static <R> R call(final java.util.concurrent.Callable<R> cmd, final Supplier<R> supplier) {
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
    public static <R> R call(final java.util.concurrent.Callable<R> cmd, final Predicate<? super Exception> predicate, final Supplier<R> supplier) {
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
    public static <R> R call(final java.util.concurrent.Callable<R> cmd, final Predicate<? super Exception> predicate, final R defaultValue) {
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
        try (final AutoCloseable c = targetResource == null ? (targetResourceSupplier == null ? EMPTY : targetResourceSupplier.get()) : targetResource) {
            cmd.accept(targetResource);
        } catch (Exception e) {
            throw N.toRuntimeException(e);
        } finally {
            if (finalAction != null) {
                finalAction.run();
            }
        }
    }

    /**
     *
     * @param cmd
     * @param actionOnError
     */
    public void run(final Throwables.Consumer<? super T, ? extends Exception> cmd, final Consumer<? super Exception> actionOnError) {
        try (final AutoCloseable c = targetResource == null ? (targetResourceSupplier == null ? EMPTY : targetResourceSupplier.get()) : targetResource) {
            cmd.accept(targetResource);
        } catch (Exception e) {
            actionOnError.accept(e);
        } finally {
            if (finalAction != null) {
                finalAction.run();
            }
        }
    }

    /**
     *
     * @param <R>
     * @param cmd
     * @return
     */
    public <R> R call(final Throwables.Function<? super T, R, ? extends Exception> cmd) {
        try (final AutoCloseable c = targetResource == null ? (targetResourceSupplier == null ? EMPTY : targetResourceSupplier.get()) : targetResource) {
            return cmd.apply(targetResource);
        } catch (Exception e) {
            throw N.toRuntimeException(e);
        } finally {
            if (finalAction != null) {
                finalAction.run();
            }
        }
    }

    /**
     *
     * @param <R>
     * @param cmd
     * @param actionOnError
     * @return
     */
    public <R> R call(final Throwables.Function<? super T, R, ? extends Exception> cmd, final Function<? super Exception, R> actionOnError) {
        try (final AutoCloseable c = targetResource == null ? (targetResourceSupplier == null ? EMPTY : targetResourceSupplier.get()) : targetResource) {
            return cmd.apply(targetResource);
        } catch (Exception e) {
            return actionOnError.apply(e);
        } finally {
            if (finalAction != null) {
                finalAction.run();
            }
        }
    }

    /**
     *
     * @param <R>
     * @param cmd
     * @param supplier
     * @return
     */
    public <R> R call(final Throwables.Function<? super T, R, ? extends Exception> cmd, final Supplier<R> supplier) {
        try (final AutoCloseable c = targetResource == null ? (targetResourceSupplier == null ? EMPTY : targetResourceSupplier.get()) : targetResource) {
            return cmd.apply(targetResource);
        } catch (Exception e) {
            return supplier.get();
        } finally {
            if (finalAction != null) {
                finalAction.run();
            }
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
        try (final AutoCloseable c = targetResource == null ? (targetResourceSupplier == null ? EMPTY : targetResourceSupplier.get()) : targetResource) {
            return cmd.apply(targetResource);
        } catch (Exception e) {
            return defaultValue;
        } finally {
            if (finalAction != null) {
                finalAction.run();
            }
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
    public <R> R call(final Throwables.Function<? super T, R, ? extends Exception> cmd, final Predicate<? super Exception> predicate,
            final Supplier<R> supplier) {
        try (final AutoCloseable c = targetResource == null ? (targetResourceSupplier == null ? EMPTY : targetResourceSupplier.get()) : targetResource) {
            return cmd.apply(targetResource);
        } catch (Exception e) {
            if (predicate.test(e)) {
                return supplier.get();
            } else {
                throw N.toRuntimeException(e);
            }
        } finally {
            if (finalAction != null) {
                finalAction.run();
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
     */
    public <R> R call(final Throwables.Function<? super T, R, ? extends Exception> cmd, final Predicate<? super Exception> predicate, final R defaultValue) {
        try (final AutoCloseable c = targetResource == null ? (targetResourceSupplier == null ? EMPTY : targetResourceSupplier.get()) : targetResource) {
            return cmd.apply(targetResource);
        } catch (Exception e) {
            if (predicate.test(e)) {
                return defaultValue;
            } else {
                throw N.toRuntimeException(e);
            }
        } finally {
            if (finalAction != null) {
                finalAction.run();
            }
        }
    }
}
