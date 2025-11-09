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
import com.landawn.abacus.util.Throwables.TernaryOperator;

/**
 * Represents an operation upon three double-valued operands and producing a double-valued result.
 * This is the primitive type specialization of {@link TernaryOperator} for double.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsDouble(double, double, double)}.
 *
 * @see TernaryOperator
 * @see DoubleBinaryOperator
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface DoubleTernaryOperator extends Throwables.DoubleTernaryOperator<RuntimeException> { //NOSONAR
    /**
     * Applies this operator to the given operands.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleTernaryOperator average = (a, b, c) -> (a + b + c) / 3.0;
     * double result = average.applyAsDouble(1.0, 2.0, 3.0); // Returns 2.0
     *
     * DoubleTernaryOperator median = (a, b, c) -> {
     *     double[] values = {a, b, c};
     *     Arrays.sort(values);
     *     return values[1];
     * };
     * double result2 = median.applyAsDouble(3.0, 1.0, 2.0); // Returns 2.0
     * }</pre>
     *
     * @param a the first operand
     * @param b the second operand
     * @param c the third operand
     * @return the operator result
     */
    @Override
    double applyAsDouble(double a, double b, double c);
}
