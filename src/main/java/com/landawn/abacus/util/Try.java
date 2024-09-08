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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class Try<T extends AutoCloseable> {
    private final T targetResource;
    private final Throwables.Supplier<T, ? extends Exception> targetResourceSupplier;
    private final Runnable finalAction;

    Try(final T targetResource, final Throwables.Supplier<T, ? extends Exception> targetResourceSupplier, final Runnable finalAction) {
        this.targetResource = targetResource;
        this.targetResourceSupplier = targetResourceSupplier;
        this.finalAction = finalAction;
    }

    /**
     *
     *
     * @param <T>
     * @param targetResource
     * @return
     * @throws IllegalArgumentException
     */
    public static <T extends AutoCloseable> Try<T> with(final T targetResource) throws IllegalArgumentException {
        N.checkArgNotNull(targetResource, "targetResource");//NOSONAR

        return new Try<>(targetResource, null, null);
    }

    /**
     *
     *
     * @param <T>
     * @param targetResource
     * @param finalAction
     * @return
     * @throws IllegalArgumentException
     */
    public static <T extends AutoCloseable> Try<T> with(final T targetResource, final Runnable finalAction) throws IllegalArgumentException {
        N.checkArgNotNull(targetResource, cs.targetResource);
        N.checkArgNotNull(finalAction, cs.finalAction);

        return new Try<>(targetResource, null, finalAction);
    }

    /**
     *
     *
     * @param <T>
     * @param targetResourceSupplier
     * @return
     * @throws IllegalArgumentException
     */
    public static <T extends AutoCloseable> Try<T> with(final Throwables.Supplier<T, ? extends Exception> targetResourceSupplier)
            throws IllegalArgumentException {
        N.checkArgNotNull(targetResourceSupplier, cs.targetResourceSupplier);

        return new Try<>(null, targetResourceSupplier, null);
    }

    /**
     *
     *
     * @param <T>
     * @param targetResourceSupplier
     * @param finalAction
     * @return
     * @throws IllegalArgumentException
     */
    public static <T extends AutoCloseable> Try<T> with(final Throwables.Supplier<T, ? extends Exception> targetResourceSupplier, final Runnable finalAction)
            throws IllegalArgumentException {
        N.checkArgNotNull(targetResourceSupplier, cs.targetResourceSupplier);
        N.checkArgNotNull(finalAction, cs.finalAction);

        return new Try<>(null, targetResourceSupplier, finalAction);
    }

    /**
     *
     * @param cmd
     * @throws RuntimeException if some error happens
     */
    public static void run(final Throwables.Runnable<? extends Exception> cmd) {
        N.checkArgNotNull(cmd, cs.cmd);

        try {
            cmd.run();
        } catch (final Exception e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     *
     * @param cmd
     * @param actionOnError
     */
    public static void run(final Throwables.Runnable<? extends Exception> cmd, final Consumer<? super Exception> actionOnError) {
        N.checkArgNotNull(cmd, cs.cmd);
        N.checkArgNotNull(actionOnError, cs.actionOnError);

        try {
            cmd.run();
        } catch (final Exception e) {
            actionOnError.accept(e);
        }
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param targetResource
    //     * @param cmd
    //     * @throws RuntimeException if some error happens
    //     */
    //    @Beta
    //    public static <T extends AutoCloseable> void run(final T targetResource, final Throwables.Consumer<? super T, ? extends Exception> cmd) {
    //        try (final T autoCloseable = targetResource) {
    //            cmd.accept(autoCloseable);
    //        } catch (Exception e) {
    //            throw ExceptionUtil.toRuntimeException(e);
    //        }
    //    }

    /**
     *
     * @param <R>
     * @param cmd
     * @return
     * @throws RuntimeException if some error happens
     */
    public static <R> R call(final java.util.concurrent.Callable<R> cmd) {
        N.checkArgNotNull(cmd, cs.cmd);

        try {
            return cmd.call();
        } catch (final Exception e) {
            throw ExceptionUtil.toRuntimeException(e);
        }
    }

    /**
     *
     * @param <R>
     * @param cmd
     * @param actionOnError
     * @return
     */
    public static <R> R call(final java.util.concurrent.Callable<R> cmd, final Function<? super Exception, ? extends R> actionOnError) {
        N.checkArgNotNull(cmd, cs.cmd);
        N.checkArgNotNull(actionOnError, cs.actionOnError);

        try {
            return cmd.call();
        } catch (final Exception e) {
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
        N.checkArgNotNull(cmd, cs.cmd);
        N.checkArgNotNull(supplier, cs.supplier);

        try {
            return cmd.call();
        } catch (final Exception e) {
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
        N.checkArgNotNull(cmd, cs.cmd);

        try {
            return cmd.call();
        } catch (final Exception e) {
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
        N.checkArgNotNull(cmd, cs.cmd);
        N.checkArgNotNull(predicate, cs.Predicate);
        N.checkArgNotNull(supplier, cs.supplier);

        try {
            return cmd.call();
        } catch (final Exception e) {
            if (predicate.test(e)) {
                return supplier.get();
            } else {
                throw ExceptionUtil.toRuntimeException(e);
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
        N.checkArgNotNull(cmd, cs.cmd);
        N.checkArgNotNull(predicate, cs.Predicate);

        try {
            return cmd.call();
        } catch (final Exception e) {
            if (predicate.test(e)) {
                return defaultValue;
            } else {
                throw ExceptionUtil.toRuntimeException(e);
            }
        }
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param <R>
    //     * @param targetResource
    //     * @param cmd
    //     * @throws RuntimeException if some error happens
    //     */
    //    @Beta
    //    public static <T extends AutoCloseable, R> R call(final T targetResource, final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd) {
    //        try (final T autoCloseable = targetResource) {
    //            return cmd.apply(autoCloseable);
    //        } catch (Exception e) {
    //            throw ExceptionUtil.toRuntimeException(e);
    //        }
    //    }

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
        try (final T closeable = targetResource == null ? (targetResourceSupplier == null ? null : targetResourceSupplier.get()) : targetResource) {
            cmd.accept(closeable);
        } catch (final Exception e) {
            throw ExceptionUtil.toRuntimeException(e);
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
        try (final T closeable = targetResource == null ? (targetResourceSupplier == null ? null : targetResourceSupplier.get()) : targetResource) {
            cmd.accept(closeable);
        } catch (final Exception e) {
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
    public <R> R call(final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd) {
        try (final T closeable = targetResource == null ? (targetResourceSupplier == null ? null : targetResourceSupplier.get()) : targetResource) {
            return cmd.apply(closeable);
        } catch (final Exception e) {
            throw ExceptionUtil.toRuntimeException(e);
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
    public <R> R call(final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd,
            final Function<? super Exception, ? extends R> actionOnError) {
        try (final T closeable = targetResource == null ? (targetResourceSupplier == null ? null : targetResourceSupplier.get()) : targetResource) {
            return cmd.apply(closeable);
        } catch (final Exception e) {
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
    public <R> R call(final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd, final Supplier<R> supplier) {
        try (final T closeable = targetResource == null ? (targetResourceSupplier == null ? null : targetResourceSupplier.get()) : targetResource) {
            return cmd.apply(closeable);
        } catch (final Exception e) {
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
    public <R> R call(final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd, final R defaultValue) {
        try (final T closeable = targetResource == null ? (targetResourceSupplier == null ? null : targetResourceSupplier.get()) : targetResource) {
            return cmd.apply(closeable);
        } catch (final Exception e) {
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
    public <R> R call(final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd, final Predicate<? super Exception> predicate,
            final Supplier<R> supplier) {
        try (final T closeable = targetResource == null ? (targetResourceSupplier == null ? null : targetResourceSupplier.get()) : targetResource) {
            return cmd.apply(closeable);
        } catch (final Exception e) {
            if (predicate.test(e)) {
                return supplier.get();
            } else {
                throw ExceptionUtil.toRuntimeException(e);
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
    public <R> R call(final Throwables.Function<? super T, ? extends R, ? extends Exception> cmd, final Predicate<? super Exception> predicate,
            final R defaultValue) {
        try (final T closeable = targetResource == null ? (targetResourceSupplier == null ? null : targetResourceSupplier.get()) : targetResource) {
            return cmd.apply(closeable);
        } catch (final Exception e) {
            if (predicate.test(e)) {
                return defaultValue;
            } else {
                throw ExceptionUtil.toRuntimeException(e);
            }
        } finally {
            if (finalAction != null) {
                finalAction.run();
            }
        }
    }
}
