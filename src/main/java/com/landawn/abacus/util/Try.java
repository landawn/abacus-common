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

/**
 * This class provides a mechanism to handle operations that may throw exceptions in a controlled manner.
 * It is designed to work with resources that implement the AutoCloseable interface, ensuring that resources are properly closed after use.
 * The class provides static and instance methods to run or call operations that may throw exceptions.
 * If an exception occurs during the operation, it is handled in a manner specified by the user.
 *
 * @param <T> The type of the resource that extends AutoCloseable.
 */
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
     * Creates a new Try instance with the specified target resource.
     *
     * This method is useful for creating a Try instance that manages the specified AutoCloseable resource.
     * The resource will be automatically closed after the operation.
     *
     * @param <T> the type of the resource that extends AutoCloseable
     * @param targetResource the resource to be managed by the Try instance
     * @return a new Try instance managing the specified target resource
     * @throws IllegalArgumentException if the targetResource is null
     */
    public static <T extends AutoCloseable> Try<T> with(final T targetResource) throws IllegalArgumentException {
        N.checkArgNotNull(targetResource, "targetResource");//NOSONAR

        return new Try<>(targetResource, null, null);
    }

    /**
     * Creates a new Try instance with the specified target resource and final action.
     *
     * This method is useful for creating a Try instance that manages the specified AutoCloseable resource.
     * The resource will be automatically closed after the operation, and the specified final action will be executed.
     *
     * @param <T> the type of the resource that extends AutoCloseable
     * @param targetResource the resource to be managed by the Try instance
     * @param finalAction the action to be executed after the resource is closed
     * @return a new Try instance managing the specified target resource and final action
     * @throws IllegalArgumentException if the targetResource or finalAction is null
     */
    public static <T extends AutoCloseable> Try<T> with(final T targetResource, final Runnable finalAction) throws IllegalArgumentException {
        N.checkArgNotNull(targetResource, cs.targetResource);
        N.checkArgNotNull(finalAction, cs.finalAction);

        return new Try<>(targetResource, null, finalAction);
    }

    /**
     * Creates a new Try instance with the specified target resource supplier.
     *
     * This method is useful for creating a Try instance that manages the specified AutoCloseable resource.
     * The resource will be automatically closed after the operation.
     *
     * @param <T> the type of the resource that extends AutoCloseable
     * @param targetResourceSupplier the supplier to provide the resource to be managed by the Try instance, must not be null
     * @return a new Try instance managing the specified target resource supplier
     * @throws IllegalArgumentException if the targetResourceSupplier is null
     */
    public static <T extends AutoCloseable> Try<T> with(final Throwables.Supplier<T, ? extends Exception> targetResourceSupplier)
            throws IllegalArgumentException {
        N.checkArgNotNull(targetResourceSupplier, cs.targetResourceSupplier);

        return new Try<>(null, targetResourceSupplier, null);
    }

    /**
     * Creates a new Try instance with the specified target resource supplier and final action.
     *
     * This method is useful for creating a Try instance that manages the specified AutoCloseable resource.
     * The resource will be automatically closed after the operation, and the specified final action will be executed.
     *
     * @param <T> the type of the resource that extends AutoCloseable
     * @param targetResourceSupplier the supplier to provide the resource to be managed by the Try instance, must not be null
     * @param finalAction the action to be executed after the resource is closed, must not be null
     * @return a new Try instance managing the specified target resource supplier and final action
     * @throws IllegalArgumentException if the targetResourceSupplier or finalAction is null
     */
    public static <T extends AutoCloseable> Try<T> with(final Throwables.Supplier<T, ? extends Exception> targetResourceSupplier, final Runnable finalAction)
            throws IllegalArgumentException {
        N.checkArgNotNull(targetResourceSupplier, cs.targetResourceSupplier);
        N.checkArgNotNull(finalAction, cs.finalAction);

        return new Try<>(null, targetResourceSupplier, finalAction);
    }

    /**
     * Executes the provided {@code cmd} that may throw an exception.
     *
     * This method is useful when you want to run a piece of code that might throw an exception.
     * If an exception occurs during the execution of the {@code cmd}, it is rethrown as a RuntimeException.
     *
     * @param cmd The runnable task that might throw an exception. Must not be {@code null}.
     * @throws RuntimeException if an exception occurs during the execution of the {@code cmd}.
     * @see Throwables#run(Throwables.Runnable)
     */
    public static void run(final Throwables.Runnable<? extends Exception> cmd) {
        N.checkArgNotNull(cmd, cs.cmd);

        try {
            cmd.run();
        } catch (final Exception e) {
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
     * @see Throwables#run(Throwables.Runnable, Consumer)
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
    //            throw ExceptionUtil.toRuntimeException(e, true);
    //        }
    //    }

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
     * @see Throwables#call(Throwables.Callable)
     */
    public static <R> R call(final java.util.concurrent.Callable<R> cmd) {
        N.checkArgNotNull(cmd, cs.cmd);

        try {
            return cmd.call();
        } catch (final Exception e) {
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
     * @see Throwables#call(Throwables.Callable, Function)
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
     * @see Throwables#call(Throwables.Callable, Supplier)
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
     * @see Throwables#call(Throwables.Callable, Object)
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
     * @see Throwables#call(Throwables.Callable, Predicate, Supplier)
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
     * @see Throwables#call(Throwables.Callable, Predicate, Object)
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
                throw ExceptionUtil.toRuntimeException(e, true);
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
    //            throw ExceptionUtil.toRuntimeException(e, true);
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
            throw ExceptionUtil.toRuntimeException(e, true);
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
            throw ExceptionUtil.toRuntimeException(e, true);
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
                throw ExceptionUtil.toRuntimeException(e, true);
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
                throw ExceptionUtil.toRuntimeException(e, true);
            }
        } finally {
            if (finalAction != null) {
                finalAction.run();
            }
        }
    }
}
