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

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Throwables;
import com.landawn.abacus.util.cs;

/**
 * Represents an operation that accepts a {@code long}-valued and an object-valued
 * argument, and returns no result. This is the {@code (long, Object)} specialization
 * of {@link java.util.function.BiConsumer}. Unlike most other functional interfaces,
 * {@code LongObjConsumer} is expected to operate via side-effects.
 *
 * <p>The parameter order follows the {@code LongObj*} convention: the {@code long} argument
 * comes first, followed by the object argument of type {@code T}. This is the reverse of
 * {@link java.util.function.ObjLongConsumer}, which takes the object first.
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
     * @param t the first {@code long} input argument
     * @param u the second input argument of type {@code T}
     */
    @Override
    void accept(long t, T u);

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
     * @return a composed {@code LongObjConsumer} that performs in sequence this
     *         operation followed by the {@code after} operation
     * @throws IllegalArgumentException if {@code after} is null
     */
    default LongObjConsumer<T> andThen(final LongObjConsumer<? super T> after) {
        N.checkArgNotNull(after, cs.after);
        return (t, u) -> {
            accept(t, u);
            after.accept(t, u);
        };
    }

    /**
     * Returns this object as a {@link Throwables.LongObjConsumer} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.LongObjConsumer}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.LongObjConsumer}
     * @return a {@link Throwables.LongObjConsumer} view of this object
     */
    default <E extends Throwable> Throwables.LongObjConsumer<T, E> toThrowable() {
        return (Throwables.LongObjConsumer<T, E>) this;
    }
}
