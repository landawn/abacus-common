/*
 * Copyright (C) 2019 HaiYang Li
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
 * Represents an operation on three long-valued operands that produces a long-valued result.
 * This is the three-arity primitive specialization of {@code Function} for long values.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsLong(long, long, long)}.
 *
 * <p>The interface extends {@code Throwables.LongTernaryOperator} with {@code RuntimeException} as the exception type,
 * making it suitable for use in contexts where checked exceptions are not required.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * LongTernaryOperator sum = (a, b, c) -> a + b + c;
 * long result = sum.applyAsLong(10L, 20L, 30L);   // returns 60L
 *
 * LongTernaryOperator median = (a, b, c) -> {
 *     if ((a >= b && a <= c) || (a <= b && a >= c)) return a;
 *     if ((b >= a && b <= c) || (b <= a && b >= c)) return b;
 *     return c;
 * };
 * }</pre>
 *
 * @see java.util.function.LongUnaryOperator
 * @see java.util.function.LongBinaryOperator
 */
@FunctionalInterface
public interface LongTernaryOperator extends Throwables.LongTernaryOperator<RuntimeException> { //NOSONAR
    /**
     * Applies this operator to the given operands.
     *
     * <p>This method takes three long values as input and produces a long result.
     * The implementation defines how the three operands are combined to produce the result.
     *
     * <p>Common implementations might include:
     * <ul>
     *   <li>Mathematical operations (sum, product, average)</li>
     *   <li>Conditional operations (min, max, median)</li>
     *   <li>Bitwise operations</li>
     *   <li>Custom business logic involving three long values</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Sum of three values
     * LongTernaryOperator sum = (a, b, c) -> a + b + c;
     * long total = sum.applyAsLong(10L, 20L, 30L);   // returns 60L
     *
     * // Average of three values
     * LongTernaryOperator average = (a, b, c) -> (a + b + c) / 3;
     * long avg = average.applyAsLong(10L, 20L, 30L);   // returns 20L
     *
     * // Find middle value (median)
     * LongTernaryOperator median = (a, b, c) -> {
     *     if ((a >= b && a <= c) || (a <= b && a >= c)) return a;
     *     if ((b >= a && b <= c) || (b <= a && b >= c)) return b;
     *     return c;
     * };
     * long mid = median.applyAsLong(5L, 15L, 10L);   // returns 10L
     * }</pre>
     *
     * @param a the first operand
     * @param b the second operand
     * @param c the third operand
     * @return the operator result as a long value
     */
    @Override
    long applyAsLong(long a, long b, long c);
}
