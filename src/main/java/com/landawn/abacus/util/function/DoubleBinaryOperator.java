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
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a>
 *
 * @see java.util.function.BinaryOperator
 * @see java.util.function.DoubleBinaryOperator
 * @see DoubleUnaryOperator
 */
@FunctionalInterface
public interface DoubleBinaryOperator extends Throwables.DoubleBinaryOperator<RuntimeException>, java.util.function.DoubleBinaryOperator { //NOSONAR

    /**
     * Applies this operator to the given operands.
     *
     * @param left the first operand
     * @param right the second operand
     * @return the operator result
     */
    @Override
    double applyAsDouble(double left, double right);
}