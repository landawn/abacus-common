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
 * Represents an operation that accepts a single float-valued argument and returns no result.
 * This is the primitive type specialization of {@link java.util.function.Consumer} for {@code float}.
 * Unlike most other functional interfaces, {@code FloatConsumer} is expected to operate via side-effects.
 * 
 * <p>This is a functional interface whose functional method is {@link #accept(float)}.</p>
 * 
 * <p>This interface extends {@link Throwables.FloatConsumer} with {@link RuntimeException},
 * providing exception handling capabilities while maintaining compatibility with standard functional programming patterns.</p>
 *
 * @see java.util.function.Consumer
 * @see java.util.function.DoubleConsumer
 * @see java.util.function.IntConsumer
 * @see java.util.function.LongConsumer
 */
@FunctionalInterface
public interface FloatConsumer extends Throwables.FloatConsumer<RuntimeException> { //NOSONAR

    /**
     * Performs this operation on the given float argument.
     * 
     * <p>This method is expected to operate via side-effects, such as modifying external state,
     * printing output, or updating data structures. The specific behavior depends on the implementation.</p>
     * 
     * <p>Common use cases include:</p>
     * <ul>
     *   <li>Accumulating float values (e.g., sum, product, statistics)</li>
     *   <li>Writing float values to output (console, file, network)</li>
     *   <li>Updating state based on float input</li>
     *   <li>Collecting float values in a data structure</li>
     *   <li>Triggering actions based on float value thresholds</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatConsumer printer = value -> System.out.println("Value: " + value);
     * printer.accept(3.14f); // Prints: Value: 3.14
     * }</pre>
     *
     * @param t the float value to be processed if the operation encounters an error during execution
     */
    @Override
    void accept(float t);

    /**
     * Returns a composed {@code FloatConsumer} that performs, in sequence, this operation
     * followed by the {@code after} operation. If performing either operation throws an exception,
     * it is relayed to the caller of the composed operation. If performing this operation throws
     * an exception, the {@code after} operation will not be performed.
     * 
     * <p>This method allows for chaining multiple consumer operations. The composed consumer will:</p>
     * <ol>
     *   <li>First execute this consumer's {@code accept} method with the given argument</li>
     *   <li>Then execute the {@code after} consumer's {@code accept} method with the same argument</li>
     * </ol>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Float> values = new ArrayList<>();
     * FloatConsumer addToList = values::add;
     * FloatConsumer printValue = v -> System.out.println("Processing: " + v);
     * 
     * // This will first add to list, then print the value
     * FloatConsumer combined = addToList.andThen(printValue);
     * combined.accept(42.5f);
     * // The value is added to the list AND printed
     * }</pre>
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code FloatConsumer} that performs in sequence this operation followed by the {@code after} operation
     */
    default FloatConsumer andThen(final FloatConsumer after) {
        return t -> {
            accept(t);
            after.accept(t);
        };
    }
}