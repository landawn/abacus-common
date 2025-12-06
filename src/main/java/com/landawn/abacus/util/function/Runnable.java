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
 * A functional interface that represents a task to be executed without arguments and
 * without returning a result. This interface extends both {@link java.lang.Runnable}
 * and {@link Throwables.Runnable}, providing compatibility with the standard Java
 * Runnable interface while adding exception handling capabilities.
 *
 * <p>This interface is particularly useful in contexts where you need to execute
 * code that may throw checked exceptions, but still want to use it in places that
 * expect a standard {@link java.lang.Runnable}.
 *
 * <p>This is a functional interface whose functional method is {@link #run()}.
 *
 * @see java.lang.Runnable
 * @see Callable
 * @see java.util.concurrent.Executor
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface Runnable extends java.lang.Runnable, Throwables.Runnable<RuntimeException> { //NOSONAR
    /**
     * Executes this runnable task.
     *
     * <p>When an object implementing interface {@code Runnable} is used to create
     * a thread, starting the thread causes the object's {@code run} method to be
     * called in that separately executing thread.
     *
     * <p>The general contract of the method {@code run} is that it may take any
     * action whatsoever. This method performs its task without returning any result
     * and without accepting any parameters.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Runnable printTask = () -> System.out.println("Hello, World!");
     * Runnable complexTask = () -> {
     *     System.out.println("Starting task...");
     *     performComplexOperation();
     *     System.out.println("Task completed!");
     * };
     *
     * // Execute directly
     * printTask.run();
     *
     * // Execute in a thread
     * new Thread(complexTask).start();
     *
     * // Execute with an executor
     * ExecutorService executor = Executors.newSingleThreadExecutor();
     * executor.execute(printTask);
     * }</pre>
     *
     * @see java.lang.Thread#run()
     */
    @Override
    void run();

    /**
     * Converts this {@code Runnable} to a {@code Callable<Void>}.
     *
     * <p>This method wraps the runnable in a callable that executes the runnable's
     * {@code run} method and returns {@code null}. This is useful when you need to
     * submit a runnable task to an {@link java.util.concurrent.ExecutorService} that
     * only accepts {@link Callable} tasks, or when you need to obtain a
     * {@link java.util.concurrent.Future} for a task that doesn't produce a result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Runnable task = () -> System.out.println("Executing task");
     * Callable<Void> callable = task.toCallable();
     *
     * ExecutorService executor = Executors.newSingleThreadExecutor();
     * Future<Void> future = executor.submit(callable);
     *
     * // Wait for completion
     * future.get();   // Returns null when task completes
     * }</pre>
     *
     * @return a {@code Callable<Void>} that executes this runnable and returns {@code null}
     * @see Callable
     * @see java.util.concurrent.Executors#callable(java.lang.Runnable)
     */
    default Callable<Void> toCallable() {
        return Fn.r2c(this);
    }

    /**
     * Converts this runnable to a {@link Throwables.Runnable} that can throw
     * checked exceptions.
     *
     * <p>This method allows the runnable to be used in contexts where checked
     * exceptions need to be handled. The returned runnable will have the same
     * behavior as this runnable but with the ability to throw the specified
     * exception type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Runnable task = () -> System.out.println("Task executed");
     * var throwableTask = task.toThrowable();
     *
     * // Can now be used in contexts that handle IOException
     * try {
     *     throwableTask.run();
     * } catch (IOException e) {
     *     // Handle the exception
     * }
     * }</pre>
     *
     * @param <E> the type of exception that the returned runnable can throw
     * @return a {@link Throwables.Runnable} version of this runnable
     */
    default <E extends Throwable> Throwables.Runnable<E> toThrowable() {
        return (Throwables.Runnable<E>) this;
    }
}
