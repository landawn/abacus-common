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
 * Represents an operation that accepts a single short-valued argument and returns no result.
 * This is the primitive type specialization of {@link java.util.function.Consumer} for {@code short}.
 * Unlike most other functional interfaces, {@code ShortConsumer} is expected to operate via side-effects.
 * 
 * <p>This is a functional interface whose functional method is {@link #accept(short)}.
 * 
 * @see java.util.function.Consumer
 */
@FunctionalInterface
public interface ShortConsumer extends Throwables.ShortConsumer<RuntimeException> { //NOSONAR

    /**
     * Performs this operation on the given argument.
     *
     * <p>This method consumes a short value, performing some side-effect operation
     * without returning any result. Common use cases include printing values,
     * updating state, or performing I/O operations.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortConsumer printer = value -> System.out.println("Value: " + value);
     * printer.accept((short) 42); // Prints "Value: 42"
     *
     * List<Short> results = new ArrayList<>();
     * ShortConsumer collector = value -> results.add(value);
     * collector.accept((short) 10);
     * collector.accept((short) 20); // results now contains [10, 20]
     * }</pre>
     *
     * @param t the input argument if the operation cannot be completed
     */
    @Override
    void accept(short t);

    /**
     * Returns a composed {@code ShortConsumer} that performs, in sequence, this operation followed by the {@code after} operation.
     * If performing either operation throws an exception, it is relayed to the caller of the composed operation.
     * If performing this operation throws an exception, the {@code after} operation will not be performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortConsumer logger = value -> System.out.println("Processing: " + value);
     * ShortConsumer validator = value -> {
     *     if (value < 0) throw new IllegalArgumentException("Negative value");
     * };
     *
     * ShortConsumer combined = logger.andThen(validator);
     * combined.accept((short) 10); // Prints "Processing: 10", then validates
     * // combined.accept((short) -5); // Prints "Processing: -5", then throws exception
     * }</pre>
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code ShortConsumer} that performs in sequence this operation followed by the {@code after} operation
     */
    default ShortConsumer andThen(final ShortConsumer after) {
        return t -> {
            accept(t);
            after.accept(t);
        };
    }
}