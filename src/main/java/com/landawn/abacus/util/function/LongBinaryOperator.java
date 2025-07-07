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
 * Represents an operation upon two {@code long}-valued operands and producing a
 * {@code long}-valued result. This is the primitive type specialization of
 * {@link java.util.function.BinaryOperator} for {@code long}.
 *
 * <p>This interface extends both {@link Throwables.LongBinaryOperator} with
 * {@link RuntimeException} and {@link java.util.function.LongBinaryOperator},
 * providing compatibility with the Java standard library while supporting the
 * Abacus framework's exception handling capabilities.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #applyAsLong(long, long)}.
 *
 * <p>Refer to JDK API documentation at: 
 * <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a>
 *
 * @see java.util.function.BinaryOperator
 * @see java.util.function.LongBinaryOperator
 * @see LongUnaryOperator
 * @since 1.8
 */
@FunctionalInterface
public interface LongBinaryOperator extends Throwables.LongBinaryOperator<RuntimeException>, java.util.function.LongBinaryOperator { //NOSONAR

    /**
     * Applies this operator to the given operands.
     *
     * <p>The operator combines two long values to produce a single long result.
     * Common implementations include:
     * <ul>
     *   <li>Arithmetic operations: addition, subtraction, multiplication, division</li>
     *   <li>Bitwise operations: AND, OR, XOR, shift operations</li>
     *   <li>Comparison operations: min, max</li>
     *   <li>Custom business logic combining two long values</li>
     *   <li>Aggregation operations for reducing collections of longs</li>
     * </ul>
     *
     * <p>This operator is particularly useful in stream reduction operations and
     * parallel computations where associative operations are required.
     *
     * @param left the first operand, typically the accumulator in reduction operations
     * @param right the second operand, typically the next element in reduction operations
     * @return the operator result, a long value computed from the two operands
     */
    @Override
    long applyAsLong(long left, long right);
}