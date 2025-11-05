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
 * A functional interface that represents an operation that accepts two short-valued
 * arguments and returns no result. This is the primitive type specialization of
 * {@link java.util.function.BiConsumer} for {@code short}.
 *
 * <p>Unlike the JDK which only provides primitive specializations for int, long, and
 * double, this interface extends support to short primitives for better type safety
 * and performance when working with short values.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(short, short)}.
 *
 * @see java.util.function.BiConsumer
 * @see IntBiConsumer
 * @see LongBiConsumer
 * @see DoubleBiConsumer
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ShortBiConsumer extends Throwables.ShortBiConsumer<RuntimeException> { //NOSONAR
    /**
     * Performs this operation on the given short arguments.
     *
     * <p>This method consumes two short values, performing some side-effect operation
     * without returning any result. Common use cases include accumulating values,
     * updating state based on two short parameters, or performing operations where
     * memory efficiency is important and values fit within the short range
     * (-32,768 to 32,767).
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortBiConsumer maxTracker = (a, b) -> {
     *     short max = (short) Math.max(a, b);
     *     System.out.println("Maximum: " + max);
     * };
     *
     * ShortBiConsumer coordinateProcessor = (x, y) -> {
     *     processPoint(x, y);
     *     updateDisplay(x, y);
     * };
     *
     * maxTracker.accept((short) 100, (short) 200); // Prints "Maximum: 200"
     * coordinateProcessor.accept((short) 10, (short) 20);
     * }</pre>
     *
     * @param t the first input argument
     * @param u the second input argument
     */
    @Override
    void accept(short t, short u);

    /**
     * Returns a composed {@code ShortBiConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * <p>This method allows for chaining multiple consumers together, where each
     * consumer receives the same two short input arguments. This is useful for
     * performing multiple independent operations on the same pair of values.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortBiConsumer printSum = (a, b) ->
     *     System.out.println("Sum: " + (a + b));
     * ShortBiConsumer printProduct = (a, b) ->
     *     System.out.println("Product: " + (a * b));
     *
     * ShortBiConsumer combined = printSum.andThen(printProduct);
     * combined.accept((short) 5, (short) 3);
     * // Output:
     * // Sum: 8
     * // Product: 15
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code ShortBiConsumer} that performs in sequence this
     *         operation followed by the {@code after} operation
     */
    default ShortBiConsumer andThen(final ShortBiConsumer after) {
        return (t, u) -> {
            accept(t, u);
            after.accept(t, u);
        };
    }

    /**
     * Converts this {@code ShortBiConsumer} to a {@code Throwables.ShortBiConsumer} that can throw a checked exception.
     * This method provides a way to use this consumer in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortBiConsumer consumer = (...) -> { ... };
     * var throwableConsumer = consumer.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned consumer can throw
     * @return a {@code Throwables.ShortBiConsumer} view of this consumer that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.ShortBiConsumer<E> toThrowable() {
        return (Throwables.ShortBiConsumer<E>) this;
    }

}
