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
 * Represents an operation that accepts a double-valued argument and a DoubleConsumer,
 * and returns no result. This functional interface is used to implement a one-to-many
 * transformation operation, similar to flatMap. The DoubleConsumer parameter can be
 * invoked multiple times to pass multiple values downstream.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(double, java.util.function.DoubleConsumer)}.
 *
 * @see java.util.stream.DoubleStream.DoubleMapMultiConsumer
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface DoubleMapMultiConsumer extends java.util.stream.DoubleStream.DoubleMapMultiConsumer { //NOSONAR
    /**
     * Performs a one-to-many transformation on the given double value.
     *
     * <p>This method accepts a double value and a consumer, allowing the implementation
     * to push zero or more double values to the consumer. This is particularly useful
     * for operations that may produce multiple outputs from a single input, or may
     * filter out certain inputs by not calling the consumer at all.
     *
     * <p>Common use cases include:
     * <ul>
     *   <li>Expanding a single value into multiple values (e.g., generating a sequence)</li>
     *   <li>Conditional mapping where some values produce no output</li>
     *   <li>Flattening nested structures without creating intermediate streams</li>
     *   <li>Implementing custom filtering and transformation logic in one step</li>
     * </ul>
     *
     * <p>Example usage in a stream:
     * <pre>{@code
     * DoubleStream.of(1.5, 2.5, 3.5)
     *     .mapMulti((value, consumer) -> {
     *         // Generate values from 0.0 to value with step 1.0
     *         for (double i = 0.0; i < value; i += 1.0) {
     *             consumer.accept(i);
     *         }
     *     })
     *     .forEach(System.out::println);
     * // Output: 0.0, 0.0, 1.0, 0.0, 1.0, 2.0
     * }</pre>
     *
     * @param value the input double value to be transformed
     * @param consumer the consumer that accepts the transformed values. The implementation
     *                 should call {@code consumer.accept(double)} for each output value. May be
     *                 called zero or more times
     */
    @Override
    void accept(double value, java.util.function.DoubleConsumer consumer);
}
