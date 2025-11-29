/*
 * Copyright (C) 2018 HaiYang Li
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
package com.landawn.abacus.util.function;

import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Throwables;

/**
 * A task that returns a result and may throw a RuntimeException.
 * This interface extends {@link java.util.concurrent.Callable} but restricts the exception type to RuntimeException,
 * making it more convenient to use in contexts where checked exceptions are not desired.
 *
 * <p>This is a functional interface whose functional method is {@link #call()}.
 *
 * @param <R> the result type of method {@code call}
 *
 * @see java.util.concurrent.Callable
 * @see Runnable
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface Callable<R> extends java.util.concurrent.Callable<R>, Throwables.Callable<R, RuntimeException> { //NOSONAR
    /**
     * Computes a result, or throws a RuntimeException if unable to do so.
     * Unlike {@link java.util.concurrent.Callable#call()}, this method only throws RuntimeException,
     * not checked exceptions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Callable<String> task = () -> "Hello, World!";
     * String result = task.call();  // Returns "Hello, World!"
     * }</pre>
     *
     * @return the computed result
     */
    @Override
    R call();

    /**
     * Converts this Callable to a Runnable that executes the call() method but discards the result.
     * The returned Runnable will execute this Callable when run, ignoring any return value.
     * Any RuntimeException thrown by the call() method will be propagated.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Callable<Integer> calculator = () -> 2 + 2;
     * Runnable runnable = calculator.toRunnable();
     * runnable.run();  // Computes 4 but result is discarded
     * }</pre>
     *
     * @return a Runnable that executes this Callable and discards the result
     */
    default Runnable toRunnable() {
        return Fn.c2r(this);
    }

    /**
     * Converts this Callable to a Throwables.Callable with a specified exception type.
     * This method performs an unchecked cast and is useful when you need to adapt this Callable
     * to a context that expects a different exception type.
     *
     * <p>Note: Since this is an unchecked cast, ensure that the actual implementation
     * only throws RuntimeException or its subclasses.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Callable<String> task = () -> "result";
     * var throwableTask = task.toThrowable();
     * // Can now be used in contexts that handle IOException
     * }</pre>
     *
     * @param <E> the type of exception that the returned Callable is declared to throw
     * @return a Throwables.Callable that is functionally equivalent to this Callable
     */
    default <E extends Throwable> Throwables.Callable<R, E> toThrowable() {
        return (Throwables.Callable<R, E>) this;
    }
}
