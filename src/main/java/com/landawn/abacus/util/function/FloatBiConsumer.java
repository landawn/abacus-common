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
 * Represents an operation that accepts two float-valued arguments and returns no result.
 * This is the two-arity specialization of {@link java.util.function.Consumer} for {@code float} values.
 * Unlike most other functional interfaces, {@code FloatBiConsumer} is expected to operate via side-effects.
 * 
 * <p>This is a functional interface whose functional method is {@link #accept(float, float)}.</p>
 * 
 * <p>This interface extends {@link Throwables.FloatBiConsumer} with {@link RuntimeException},
 * providing exception handling capabilities while maintaining compatibility with standard functional programming patterns.</p>
 *
 * @see java.util.function.Consumer
 * @see java.util.function.BiConsumer
 * @see FloatConsumer
 */
@FunctionalInterface
public interface FloatBiConsumer extends Throwables.FloatBiConsumer<RuntimeException> { //NOSONAR

    /**
     * Performs this operation on the given two float arguments.
     * 
     * <p>This method is expected to operate via side-effects, such as modifying external state,
     * printing output, or updating data structures. The specific behavior depends on the implementation.</p>
     * 
     * <p>Common use cases include:</p>
     * <ul>
     *   <li>Processing 2D coordinates (x, y)</li>
     *   <li>Handling pairs of measurements or values</li>
     *   <li>Updating state based on two float inputs</li>
     *   <li>Logging or recording two related float values</li>
     *   <li>Performing calculations with results stored externally</li>
     * </ul>
     *
     * @param t the first float argument
     * @param u the second float argument
     * @throws RuntimeException if the operation encounters an error during execution
     */
    @Override
    void accept(float t, float u);

    /**
     * Returns a composed {@code FloatBiConsumer} that performs, in sequence, this operation
     * followed by the {@code after} operation. If performing either operation throws an exception,
     * it is relayed to the caller of the composed operation. If performing this operation throws
     * an exception, the {@code after} operation will not be performed.
     * 
     * <p>This method allows for chaining multiple consumer operations. The composed consumer will:</p>
     * <ol>
     *   <li>First execute this consumer's {@code accept} method with the given arguments</li>
     *   <li>Then execute the {@code after} consumer's {@code accept} method with the same arguments</li>
     * </ol>
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * FloatBiConsumer logValues = (x, y) -> System.out.printf("Point: (%.2f, %.2f)%n", x, y);
     * FloatBiConsumer calculateDistance = (x, y) -> {
     *     float distance = (float) Math.sqrt(x * x + y * y);
     *     System.out.printf("Distance from origin: %.2f%n", distance);
     * };
     * 
     * // This will first log the coordinates, then calculate and print the distance
     * FloatBiConsumer combined = logValues.andThen(calculateDistance);
     * combined.accept(3.0f, 4.0f);
     * // Output:
     * // Point: (3.00, 4.00)
     * // Distance from origin: 5.00
     * }</pre>
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code FloatBiConsumer} that performs in sequence this operation followed by the {@code after} operation
     */
    default FloatBiConsumer andThen(final FloatBiConsumer after) {
        return (t, u) -> {
            accept(t, u);
            after.accept(t, u);
        };
    }
}