/*
 * Copyright (C) 2016 HaiYang Li
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

import com.landawn.abacus.util.Throwables;

/**
 * Represents an operation that accepts three input arguments and returns no result.
 * This is the three-arity specialization of {@link java.util.function.Consumer}.
 * Unlike most other functional interfaces, TriConsumer is expected to operate via side-effects.
 *
 * <p>This interface extends the Throwables.TriConsumer, providing compatibility
 * with the abacus-common framework's error handling mechanisms while limiting thrown exceptions
 * to RuntimeException.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(Object, Object, Object)}.
 *
 * @param <A> the type of the first argument to the operation
 * @param <B> the type of the second argument to the operation
 * @param <C> the type of the third argument to the operation
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface TriConsumer<A, B, C> extends Throwables.TriConsumer<A, B, C, RuntimeException> { //NOSONAR
    /**
     * Performs this operation on the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TriConsumer<String, Integer, Boolean> logger =
     *     (msg, level, timestamp) -> System.out.println("[" + level + "] " + msg + " @ " + timestamp);
     * logger.accept("Application started", 1, true);
     *
     * TriConsumer<List<String>, String, Integer> listInserter =
     *     (list, element, index) -> list.add(index, element);
     * List<String> myList = new ArrayList<>(Arrays.asList("a", "b", "c"));
     * listInserter.accept(myList, "x", 1);   // list becomes ["a", "x", "b", "c"]
     * }</pre>
     *
     * @param a the first input argument
     * @param b the second input argument
     * @param c the third input argument
     */
    @Override
    void accept(A a, B b, C c);

    /**
     * Returns a composed {@code TriConsumer} that performs, in sequence, this operation followed
     * by the {@code after} operation. If performing either operation throws an exception, it is
     * relayed to the caller of the composed operation. If performing this operation throws
     * an exception, the {@code after} operation will not be performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TriConsumer<String, Integer, Boolean> logger =
     *     (msg, level, flag) -> System.out.println("Log: " + msg);
     * TriConsumer<String, Integer, Boolean> notifier =
     *     (msg, level, flag) -> System.out.println("Notify: " + msg);
     *
     * TriConsumer<String, Integer, Boolean> combined = logger.andThen(notifier);
     * combined.accept("Error occurred", 3, false);
     * // Output:
     * // Log: Error occurred
     * // Notify: Error occurred
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code TriConsumer} that performs in sequence this operation followed by the {@code after} operation
     */
    default TriConsumer<A, B, C> andThen(final TriConsumer<? super A, ? super B, ? super C> after) {
        return (a, b, c) -> {
            accept(a, b, c);
            after.accept(a, b, c);
        };
    }

    /**
     * Converts this TriConsumer to a Throwables.TriConsumer that can throw checked exceptions.
     * This method is useful when you need to use this consumer in a context that expects
     * a Throwables.TriConsumer with a specific exception type.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TriConsumer<String, Integer, Boolean> consumer =
     *     (s, i, b) -> System.out.println(s + ":" + i + ":" + b);
     * var throwableConsumer = consumer.toThrowable();
     * }</pre>
     *
     * @param <E> the type of exception that the returned consumer may throw
     * @return a Throwables.TriConsumer that wraps this consumer
     */
    default <E extends Throwable> Throwables.TriConsumer<A, B, C, E> toThrowable() {
        return (Throwables.TriConsumer<A, B, C, E>) this;
    }
}
