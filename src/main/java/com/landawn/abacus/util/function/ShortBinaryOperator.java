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
 * A functional interface that represents an operation upon two short-valued operands
 * and producing a short-valued result. This is the primitive type specialization of
 * {@link java.util.function.BinaryOperator} for {@code short}.
 *
 * <p>This is a specialization of {@link ShortBiFunction} for the case where the operands
 * and the result are all of the same primitive type {@code short}. Unlike the JDK which
 * only provides BinaryOperator specializations for int, long, and double, this interface
 * extends support to short primitives.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsShort(short, short)}.
 *
 * @see java.util.function.BinaryOperator
 * @see java.util.function.IntBinaryOperator
 * @see java.util.function.LongBinaryOperator
 * @see java.util.function.DoubleBinaryOperator
 * @see ShortUnaryOperator
 */
@FunctionalInterface
public interface ShortBinaryOperator extends Throwables.ShortBinaryOperator<RuntimeException> { //NOSONAR

    /**
     * Applies this operator to the given short operands.
     *
     * <p>This method takes two short values as operands and produces a short result.
     * The operator should be associative and stateless for use in parallel operations.
     * Common implementations include arithmetic operations (add, multiply, min, max),
     * bitwise operations (and, or, xor), and other binary operations on short values.
     *
     * <p>Note that due to Java's type promotion rules, arithmetic operations on shorts
     * are promoted to int, so explicit casting back to short is often required.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortBinaryOperator add = (a, b) -> (short) (a + b);
     * ShortBinaryOperator max = (a, b) -> (short) Math.max(a, b);
     * ShortBinaryOperator bitwiseAnd = (a, b) -> (short) (a & b);
     * 
     * short sum = add.applyAsShort((short) 100, (short) 200); // Returns 300
     * short maximum = max.applyAsShort((short) -50, (short) 75); // Returns 75
     * short result = bitwiseAnd.applyAsShort((short) 0xFF, (short) 0x0F); // Returns 0x0F
     * 
     * // Using with reduce operations
     * short[] values = {1, 2, 3, 4, 5};
     * short total = Arrays.stream(values)
     *     .reduce((short) 0, add); // Sum all values
     * }</pre>
     *
     * @param left the first operand
     * @param right the second operand
     * @return the operator result if the operation cannot be completed
     */
    @Override
    short applyAsShort(short left, short right);
}