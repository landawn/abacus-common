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
 * Represents an operation that accepts three long-valued arguments and returns no result.
 * This is the three-arity primitive specialization of {@code Consumer} for long values.
 * Unlike most other functional interfaces, {@code LongTriConsumer} is expected to operate via side-effects.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(long, long, long)}.
 *
 * <p>The interface extends {@code Throwables.LongTriConsumer} with {@code RuntimeException} as the exception type,
 * making it suitable for use in contexts where checked exceptions are not required.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * LongTriConsumer printSum = (a, b, c) -> System.out.println("Sum: " + (a + b + c));
 * printSum.accept(10L, 20L, 30L); // prints "Sum: 60"
 *
 * LongTriConsumer storeValues = (x, y, z) -> {
 *     System.out.println("x: " + x);
 *     System.out.println("y: " + y);
 *     System.out.println("z: " + z);
 * };
 * }</pre>
 *
 * @see java.util.function.LongConsumer
 * @see java.util.function.Consumer
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface LongTriConsumer extends Throwables.LongTriConsumer<RuntimeException> { //NOSONAR
    /**
     * Performs this operation on the given arguments.
     *
     * <p>This method processes three long values, typically producing side effects
     * such as writing to output, modifying state, or storing values.
     *
     * <p>Common use cases include:
     * <ul>
     *   <li>Logging or printing three related values</li>
     *   <li>Storing coordinates (x, y, z) in three-dimensional space</li>
     *   <li>Updating multiple related fields in an object</li>
     *   <li>Performing batch operations with three parameters</li>
     * </ul>
     *
     * @param a the first input argument
     * @param b the second input argument
     * @param c the third input argument
     */
    @Override
    void accept(long a, long b, long c);

    /**
     * Returns a composed {@code LongTriConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation.
     *
     * <p>If performing either operation throws an exception, it is relayed to the
     * caller of the composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * <p>This method is useful for chaining multiple operations that need to process
     * the same three long values in sequence.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongTriConsumer logValues = (a, b, c) ->
     *     System.out.println("Processing: " + a + ", " + b + ", " + c);
     * LongTriConsumer storeSum = (a, b, c) ->
     *     System.out.println("Sum: " + (a + b + c));
     *
     * LongTriConsumer combined = logValues.andThen(storeSum);
     * combined.accept(10L, 20L, 30L);
     * // First logs: "Processing: 10, 20, 30"
     * // Then logs: "Sum: 60"
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code LongTriConsumer} that performs in sequence this
     *         operation followed by the {@code after} operation
     */
    default LongTriConsumer andThen(final LongTriConsumer after) {
        return (a, b, c) -> {
            accept(a, b, c);
            after.accept(a, b, c);
        };
    }

    /**
     * Converts this {@code LongTriConsumer} to a {@code Throwables.LongTriConsumer} that can throw a checked exception.
     * This method provides a way to use this consumer in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongTriConsumer consumer = (...) -> { ... };
     * var throwableConsumer = consumer.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned consumer can throw
     * @return a {@code Throwables.LongTriConsumer} view of this consumer that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.LongTriConsumer<E> toThrowable() {
        return (Throwables.LongTriConsumer<E>) this;
    }

}
