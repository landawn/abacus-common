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
 * Represents an operation that accepts a variable number of short-valued arguments and returns no result.
 * This is the variable-arity specialization of {@link ShortConsumer}.
 * Unlike most other functional interfaces, {@code ShortNConsumer} is expected to operate via side-effects.
 * 
 * <p>This is a functional interface whose functional method is {@link #accept(short...)}.
 * 
 * @see ShortConsumer
 */
@FunctionalInterface
public interface ShortNConsumer {

    /**
     * Performs this operation on the given arguments.
     * The behavior of this operation is generally expected to be non-interfering and stateless.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortNConsumer sumPrinter = args -> {
     *     int sum = 0;
     *     for (short value : args) sum += value;
     *     System.out.println("Sum: " + sum);
     * };
     * sumPrinter.accept((short) 1, (short) 2, (short) 3); // Prints "Sum: 6"
     *
     * ShortNConsumer minMaxPrinter = args -> {
     *     if (args.length == 0) return;
     *     short min = args[0], max = args[0];
     *     for (short v : args) {
     *         if (v < min) min = v;
     *         if (v > max) max = v;
     *     }
     *     System.out.println("Min: " + min + ", Max: " + max);
     * };
     * minMaxPrinter.accept((short) 5, (short) 2, (short) 8, (short) 1);
     * }</pre>
     *
     * @param args the input arguments as a variable-length array of short values if the operation cannot be completed
     */
    void accept(short... args);

    /**
     * Returns a composed {@code ShortNConsumer} that performs, in sequence, this operation followed by the {@code after} operation.
     * If performing either operation throws an exception, it is relayed to the caller of the composed operation.
     * If performing this operation throws an exception, the {@code after} operation will not be performed.
     * 
     * <p>Note that both operations will receive the same array reference, so if either operation
     * modifies the array, the changes will be visible to the other operation.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code ShortNConsumer} that performs in sequence this operation followed by the {@code after} operation
     */
    default ShortNConsumer andThen(final ShortNConsumer after) {
        return args -> {
            accept(args);
            after.accept(args);
        };
    }
}