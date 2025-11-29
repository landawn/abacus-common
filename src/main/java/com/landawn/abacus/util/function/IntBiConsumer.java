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
 * Represents an operation that accepts two {@code int}-valued arguments and returns no result.
 * This is the two-arity specialization of {@link java.util.function.Consumer} for {@code int} values.
 * Unlike most other functional interfaces, {@code IntBiConsumer} is expected to operate via side-effects.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(int, int)}.
 *
 * @see java.util.function.Consumer
 * @see IntConsumer
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface IntBiConsumer extends Throwables.IntBiConsumer<RuntimeException> { //NOSONAR
    /**
     * Performs this operation on the given arguments.
     * This method is expected to operate via side-effects.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntBiConsumer adder = (a, b) -> System.out.println("Sum: " + (a + b));
     * adder.accept(5, 10);  // Prints: Sum: 15
     *
     * Map<Integer, Integer> map = new HashMap<>();
     * IntBiConsumer mapPutter = (key, value) -> map.put(key, value);
     * mapPutter.accept(1, 100);
     * }</pre>
     *
     * @param t the first {@code int} argument
     * @param u the second {@code int} argument
     */
    @Override
    void accept(int t, int u);

    /**
     * Returns a composed {@code IntBiConsumer} that performs, in sequence, this operation followed by
     * the {@code after} operation. If performing either operation throws an exception, it is relayed
     * to the caller of the composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntBiConsumer logger = (x, y) -> System.out.println("Processing: " + x + ", " + y);
     * IntBiConsumer validator = (x, y) -> { if (x < 0 || y < 0) throw new IllegalArgumentException(); };
     * IntBiConsumer combined = logger.andThen(validator);
     * combined.accept(10, 20);  // Logs then validates
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code IntBiConsumer} that performs in sequence this operation followed by
     *         the {@code after} operation
     */
    default IntBiConsumer andThen(final IntBiConsumer after) {
        return (t, u) -> {
            accept(t, u);
            after.accept(t, u);
        };
    }
}
