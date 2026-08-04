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
 * Represents a task that takes no arguments and returns no result.
 * This interface extends {@link java.lang.Runnable} and is parameterized so that
 * {@link #run()} may only propagate unchecked exceptions ({@code RuntimeException}).
 * Lambdas that need to throw checked exceptions should use {@link Throwables.Runnable}
 * with an appropriate exception type instead.
 *
 * <p>Instances are usable wherever a {@code java.lang.Runnable} is expected
 * (for example with {@link Thread} or {@link java.util.concurrent.Executor}).
 *
 * <p>This is a functional interface whose functional method is {@link #run()}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Runnable.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Runnable.html</a></p>
 *
 * @see java.lang.Runnable
 * @see Callable
 * @see java.util.concurrent.Executor
 */
@FunctionalInterface
public interface Runnable extends java.lang.Runnable, Throwables.Runnable<RuntimeException> { //NOSONAR
    /**
     * Executes this runnable task.
     * The general contract is that this method may take any action whatsoever.
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
     * executor.shutdown();
     * }</pre>
     *
     * @see java.lang.Thread#run()
     */
    @Override
    void run();

    /**
     * Returns a {@code Callable<Void>} that executes this runnable and returns {@code null}.
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
     * executor.shutdown();
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
     * Returns this runnable as a {@link Throwables.Runnable} view.
     *
     * <p>The returned runnable has the same behavior as this runnable. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept
     * {@code Throwables.Runnable}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Runnable task = () -> System.out.println("Task executed");
     * Throwables.Runnable<RuntimeException> throwableTask = task.toThrowable();
     * throwableTask.run();
     * }</pre>
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.Runnable}
     * @return a {@link Throwables.Runnable} view of this runnable
     */
    default <E extends Throwable> Throwables.Runnable<E> toThrowable() {
        return (Throwables.Runnable<E>) this;
    }
}
