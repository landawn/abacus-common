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
 * Represents an operation upon two float-valued operands and producing a float-valued result.
 * This is the primitive type specialization of {@link java.util.function.BinaryOperator} for {@code float}.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsFloat(float, float)}.</p>
 *
 * <p>This interface extends {@link Throwables.FloatBinaryOperator} with {@link RuntimeException},
 * providing exception handling capabilities while maintaining compatibility with standard functional programming patterns.</p>
 *
 * <p>Unlike {@link FloatBiFunction}, this interface is specifically designed for operations where both
 * inputs and the output are of the same primitive type {@code float}, making it ideal for mathematical
 * and arithmetic operations.</p>
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.BinaryOperator
 * @see FloatUnaryOperator
 * @see FloatBiFunction
 */
@FunctionalInterface
public interface FloatBinaryOperator extends Throwables.FloatBinaryOperator<RuntimeException> { //NOSONAR
    /**
     * Applies this operator to the given operands and returns the float result.
     *
     * <p>This method performs a binary operation on two float values and returns a float result.
     * The operation represents any calculation that combines two float values into a single float value.</p>
     *
     * <p>Common use cases include:</p>
     * <ul>
     *   <li>Basic arithmetic operations (addition, subtraction, multiplication, division)</li>
     *   <li>Mathematical functions (min, max, power, modulo)</li>
     *   <li>Custom aggregation operations (weighted average, geometric mean)</li>
     *   <li>Bitwise operations on float bit representations</li>
     * </ul>
     *
     * <p>Example implementations:</p>
     * <pre>{@code
     * FloatBinaryOperator add = (a, b) -> a + b;
     * FloatBinaryOperator multiply = (a, b) -> a * b;
     * FloatBinaryOperator max = (a, b) -> Math.max(a, b);
     * FloatBinaryOperator safeDivide = (a, b) -> b != 0 ? a / b : Float.NaN;
     * }</pre>
     *
     * @param left the first operand (left-hand side of the operation)
     * @param right the second operand (right-hand side of the operation)
     * @return the result of applying this operator to the operands
     */
    @Override
    float applyAsFloat(float left, float right);
}
