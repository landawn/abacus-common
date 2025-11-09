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
 * Represents an operation upon three short-valued operands and producing a short-valued result.
 * This is the primitive type specialization of ternary operator for {@code short}.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsShort(short, short, short)}.
 *
 * @see java.util.function.BinaryOperator
 * @see ShortBinaryOperator
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ShortTernaryOperator extends Throwables.ShortTernaryOperator<RuntimeException> { //NOSONAR
    /**
     * Applies this operator to the given operands.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortTernaryOperator sum = (a, b, c) -> (short) (a + b + c);
     * short result1 = sum.applyAsShort((short) 10, (short) 20, (short) 30); // returns 60
     *
     * ShortTernaryOperator median = (a, b, c) -> {
     *     if (a >= b && a <= c || a >= c && a <= b) return a;
     *     if (b >= a && b <= c || b >= c && b <= a) return b;
     *     return c;
     * };
     * short result2 = median.applyAsShort((short) 5, (short) 2, (short) 8); // returns 5
     *
     * ShortTernaryOperator clamp = (value, min, max) ->
     *     (short) Math.max(min, Math.min(max, value));
     * short result3 = clamp.applyAsShort((short) 150, (short) 0, (short) 100); // returns 100
     * }</pre>
     *
     * @param a the first operand
     * @param b the second operand
     * @param c the third operand
     * @return the operator result as a short value
     */
    @Override
    short applyAsShort(short a, short b, short c);
}
