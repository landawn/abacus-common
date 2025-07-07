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

/**
 * Represents an operation that accepts a variable number of {@code long}-valued
 * arguments and returns no result. This is the N-arity specialization of
 * {@link java.util.function.Consumer} for {@code long} values. Unlike most other
 * functional interfaces, {@code LongNConsumer} is expected to operate via
 * side-effects.
 *
 * <p>This interface is particularly useful when you need to process an arbitrary
 * number of long values as a group, without knowing the exact count at compile time.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #accept(long...)}.
 *
 * @see java.util.function.Consumer
 * @see LongConsumer
 * @see LongBiConsumer
 * @since 1.8
 */
@FunctionalInterface
public interface LongNConsumer {

    /**
     * Performs this operation on the given arguments.
     *
     * <p>The consumer processes a variable number of long values and performs
     * some side-effect operation. The varargs parameter allows for flexible
     * argument counts, from zero to many values.
     *
     * <p>Common use cases include:
     * <ul>
     *   <li>Processing arrays or collections of long values</li>
     *   <li>Aggregating multiple long values (sum, average, etc.)</li>
     *   <li>Logging or recording groups of related long values</li>
     *   <li>Batch processing of long values</li>
     *   <li>Implementing operations that work with any number of long inputs</li>
     * </ul>
     *
     * <p>Example usage:
     * <pre>{@code
     * LongNConsumer sumPrinter = args -> {
     *     long sum = 0;
     *     for (long value : args) {
     *         sum += value;
     *     }
     *     System.out.println("Sum: " + sum);
     * };
     * sumPrinter.accept(1L, 2L, 3L, 4L, 5L); // Prints: Sum: 15
     * }</pre>
     *
     * @param args the input arguments as a varargs array. Can be empty, contain
     *             a single value, or multiple values. The array should not be
     *             modified by the implementation
     */
    void accept(long... args);

    /**
     * Returns a composed {@code LongNConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * <p>This method allows for chaining multiple consumers to create a pipeline
     * of operations that will be executed sequentially on the same array of
     * input values.
     *
     * <p>Example usage:
     * <pre>{@code
     * LongNConsumer logger = args -> System.out.println("Processing " + args.length + " values");
     * LongNConsumer processor = args -> processValues(args);
     * LongNConsumer combined = processor.andThen(logger);
     * combined.accept(10L, 20L, 30L); // First processes, then logs
     * }</pre>
     *
     * @param after the operation to perform after this operation. Must not be null
     * @return a composed {@code LongNConsumer} that performs in sequence this
     *         operation followed by the {@code after} operation
     */
    default LongNConsumer andThen(final LongNConsumer after) {
        return args -> {
            accept(args);
            after.accept(args);
        };
    }
}