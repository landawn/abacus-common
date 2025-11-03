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
 * Represents an operation upon two double-valued operands and producing a double-valued result.
 * This is the primitive type specialization of {@link java.util.function.BinaryOperator} for double.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsDouble(double, double)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.BinaryOperator
 * @see java.util.function.DoubleBinaryOperator
 * @see DoubleUnaryOperator
 */
@FunctionalInterface
public interface DoubleBinaryOperator extends Throwables.DoubleBinaryOperator<RuntimeException>, java.util.function.DoubleBinaryOperator { //NOSONAR
    /**
     * Applies this operator to the given {@code double}-valued operands and produces a {@code double}-valued result.
     *
     * <p>This method performs an operation on two double operands and returns a double result.
     * Common use cases include:
     * <ul>
     *   <li>Arithmetic operations (addition, subtraction, multiplication, division)</li>
     *   <li>Mathematical functions (power, min, max, average)</li>
     *   <li>Custom binary operations for reduction in streams</li>
     *   <li>Combining or aggregating double values</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleBinaryOperator adder = (a, b) -> a + b;
     * double sum = adder.applyAsDouble(2.5, 3.7); // Returns 6.2
     *
     * DoubleBinaryOperator multiplier = (a, b) -> a * b;
     * double product = multiplier.applyAsDouble(4.0, 2.5); // Returns 10.0
     *
     * DoubleBinaryOperator min = Math::min;
     * double minimum = min.applyAsDouble(5.5, 3.3); // Returns 3.3
     * }</pre>
     *
     * @param left the first operand
     * @param right the second operand
     * @return the operator result
     */
    @Override
    double applyAsDouble(double left, double right);
}
