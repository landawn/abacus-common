/*
 * Copyright (C) 2024 HaiYang Li
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
 * Represents an operation that accepts a {@code long}-valued and an object-valued
 * argument, and returns no result. This is the {@code (long, Object)} specialization
 * of {@link java.util.function.BiConsumer}. Unlike most other functional interfaces,
 * {@code LongObjConsumer} is expected to operate via side-effects.
 *
 * <p>This interface extends {@link Throwables.LongObjConsumer} with
 * {@link RuntimeException}, providing compatibility with the abacus-common framework's
 * exception handling capabilities.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #accept(long, Object)}.
 *
 * <p>Refer to JDK API documentation at:
 * <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @param <T> the type of the object argument to the operation
 *
 * @see java.util.function.BiConsumer
 * @see java.util.function.ObjLongConsumer
 */
@FunctionalInterface
public interface LongObjConsumer<T> extends Throwables.LongObjConsumer<T, RuntimeException> { // NOSONAR
    /**
     * Performs this operation on the given arguments.
     *
     * <p>The consumer processes a long value and an object value, performing
     * some side-effect operation. This is particularly useful when you need
     * to process a primitive long value alongside an object without boxing
     * the long value.
     *
     * <p>Common use cases include:
     * <ul>
     *   <li>Associating long indices or IDs with objects</li>
     *   <li>Processing timestamps (long) with associated data objects</li>
     *   <li>Updating objects based on long values (e.g., setting timestamps)</li>
     *   <li>Recording metrics where the long represents a measurement and the
     *       object provides context</li>
     *   <li>Implementing indexed operations where the long is an index</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongObjConsumer<String> logger = (timestamp, message) ->
     *     System.out.println("[" + timestamp + "] " + message);
     * logger.accept(System.currentTimeMillis(), "Application started");
     * }</pre>
     *
     * @param i the first input argument (long value)
     * @param t the second input argument (object of type T)
     */
    @Override
    void accept(long i, T t);

    /**
     * Returns a composed {@code LongObjConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * <p>This method allows for chaining multiple consumers to create a pipeline
     * of operations that will be executed sequentially on the same pair of
     * input values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongObjConsumer<User> updateTimestamp = (timestamp, user) ->
     *     user.setLastModified(timestamp);
     * LongObjConsumer<User> logUpdate = (timestamp, user) ->
     *     System.out.println("User " + user.getName() + " updated at " + timestamp);
     *
     * LongObjConsumer<User> combined = updateTimestamp.andThen(logUpdate);
     * combined.accept(System.currentTimeMillis(), user);
     * // First updates the user's timestamp, then logs the update
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     *              Note that the type parameter must be {@code ? super T} to ensure
     *              type safety when composing consumers
     * @return a composed {@code LongObjConsumer} that performs in sequence this
     *         operation followed by the {@code after} operation
     */
    default LongObjConsumer<T> andThen(final LongObjConsumer<? super T> after) {
        return (i, t) -> {
            accept(i, t);
            after.accept(i, t);
        };
    }
}
