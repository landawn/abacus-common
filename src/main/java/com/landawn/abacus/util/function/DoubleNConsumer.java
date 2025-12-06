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
 * Represents an operation that accepts a variable number of double-valued arguments and returns no result.
 * This is the N-arity specialization of {@link DoubleConsumer}.
 * Unlike most other functional interfaces, {@code DoubleNConsumer} is expected to operate via side-effects.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(double...)}.
 *
 * @see DoubleConsumer
 * @see DoubleBiConsumer
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface DoubleNConsumer {
    /**
     * Performs this operation on the given arguments.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleNConsumer averager = values -> {
     *     double sum = 0;
     *     for (double v : values) sum += v;
     *     System.out.println("Average: " + (sum / values.length));
     * };
     * averager.accept(1.0, 2.0, 3.0);   // Prints: Average: 2.0
     * }</pre>
     *
     * @param args the double input arguments as a varargs array
     */
    void accept(double... args);

    /**
     * Returns a composed {@code DoubleNConsumer} that performs, in sequence, this operation followed by the {@code after} operation.
     * If performing either operation throws an exception, it is relayed to the caller of the composed operation.
     * If performing this operation throws an exception, the {@code after} operation will not be performed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleNConsumer logger = values -> System.out.println("Values: " + Arrays.toString(values));
     * DoubleNConsumer summer = values -> {
     *     double sum = 0;
     *     for (double v : values) sum += v;
     *     System.out.println("Sum: " + sum);
     * };
     * DoubleNConsumer combined = logger.andThen(summer);
     * combined.accept(1.0, 2.0, 3.0);
     * // Prints:
     * // Values: [1.0, 2.0, 3.0]
     * // Sum: 6.0
     * }</pre>
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code DoubleNConsumer} that performs in sequence this operation followed by the {@code after} operation
     */
    default DoubleNConsumer andThen(final DoubleNConsumer after) {
        return args -> {
            accept(args);
            after.accept(args);
        };
    }
}
