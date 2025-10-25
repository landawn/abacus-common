/*
 * Copyright (C) 2021 HaiYang Li
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

/**
 * Represents an operation that accepts a long-valued argument and a LongConsumer, 
 * and returns no result. This functional interface is used to implement a one-to-many
 * transformation operation, similar to flatMap. The LongConsumer parameter can be 
 * invoked multiple times to pass multiple values downstream.
 *
 * @see java.util.stream.LongStream.LongMapMultiConsumer
 */
@FunctionalInterface
public interface LongMapMultiConsumer extends java.util.stream.LongStream.LongMapMultiConsumer { //NOSONAR

    /**
     * Performs a one-to-many transformation on the given long value.
     *
     * <p>This method accepts a long value and a consumer, allowing the implementation
     * to push zero or more long values to the consumer. This is particularly useful
     * for operations that may produce multiple outputs from a single input, or may
     * filter out certain inputs by not calling the consumer at all.
     *
     * <p>Common use cases include:
     * <ul>
     *   <li>Expanding a single value into multiple values (e.g., generating a range)</li>
     *   <li>Conditional mapping where some values produce no output</li>
     *   <li>Flattening nested structures without creating intermediate streams</li>
     *   <li>Implementing custom filtering and transformation logic in one step</li>
     * </ul>
     *
     * <p>Example usage in a stream:
     * <pre>{@code
     * LongStream.of(1L, 2L, 3L)
     *     .mapMulti((value, consumer) -> {
     *         // Generate values from 0 to value-1
     *         for (long i = 0; i < value; i++) {
     *             consumer.accept(i);
     *         }
     *     })
     *     .forEach(System.out::println);
     * // Output: 0, 0, 1, 0, 1, 2
     * }</pre>
     *
     * @param value the input long value to be transformed
     * @param ic the consumer that accepts the transformed values. The implementation
     *           should call {@code ic.accept(long)} for each output value. May be
     *           called zero or more times
     */
    @Override
    void accept(long value, java.util.function.LongConsumer ic);
}