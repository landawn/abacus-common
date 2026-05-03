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
 * Represents an operation that accepts a {@code long}-valued argument and a
 * {@link java.util.function.LongConsumer}, and returns no result. This functional interface
 * is used to implement a one-to-many (multi-mapping) transformation, similar to
 * {@code flatMap}. The {@code LongConsumer} parameter may be invoked zero or more times
 * to push any number of {@code long} values downstream for each input value.
 *
 * <p>This interface is the primitive {@code long} specialization of the
 * {@code mapMulti} pattern introduced in Java 16. It allows implementations to
 * conditionally emit multiple output values from a single input — or emit none at all —
 * without creating intermediate collections or streams.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(long, java.util.function.LongConsumer)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.stream.LongStream.LongMapMultiConsumer
 * @see java.util.stream.LongStream#mapMulti(java.util.stream.LongStream.LongMapMultiConsumer)
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
     * <p><b>Usage Examples:</b></p>
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
     * @param value the source value to expand, transform, or suppress
     * @param consumer the downstream consumer that receives each produced value; implementations
     *        may invoke it zero or more times for the same input value
     */
    @Override
    void accept(long value, java.util.function.LongConsumer consumer);
}
