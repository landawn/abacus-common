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
 * Represents an operation on a single float-valued operand that produces a float-valued result.
 * This is a functional interface whose functional method is {@link #applyAsFloat(float)}.
 * 
 * <p>This is a primitive type specialization of {@link java.util.function.UnaryOperator} for {@code float}.
 * This is also a specialization of {@link FloatFunction} for the case where the operand and result
 * are of the same primitive type.</p>
 * 
 * @since 1.0
 * @see java.util.function.UnaryOperator
 * @see java.util.function.DoubleUnaryOperator
 * @see java.util.function.IntUnaryOperator
 */
@FunctionalInterface
public interface FloatUnaryOperator extends Throwables.FloatUnaryOperator<RuntimeException> { //NOSONAR

    /**
     * Applies this operator to the given float operand.
     * 
     * <p>The implementation should define how the float value is transformed.
     * Common implementations include mathematical operations like negation,
     * absolute value, or other unary transformations.</p>
     *
     * @param operand the float operand
     * @return the float result of applying this operator
     */
    @Override
    float applyAsFloat(float operand);

    /**
     * Returns a composed operator that first applies the {@code before}
     * operator to its input, and then applies this operator to the result.
     * If evaluation of either operator throws an exception, it is relayed to
     * the caller of the composed operator.
     *
     * @param before the operator to apply before this operator is applied
     * @return a composed operator that first applies the {@code before}
     *         operator and then applies this operator
     * 
     * @see #andThen(FloatUnaryOperator)
     */
    default FloatUnaryOperator compose(final FloatUnaryOperator before) {
        return v -> applyAsFloat(before.applyAsFloat(v));
    }

    /**
     * Returns a composed operator that first applies this operator to
     * its input, and then applies the {@code after} operator to the result.
     * If evaluation of either operator throws an exception, it is relayed to
     * the caller of the composed operator.
     *
     * @param after the operator to apply after this operator is applied
     * @return a composed operator that first applies this operator and then
     *         applies the {@code after} operator
     * 
     * @see #compose(FloatUnaryOperator)
     */
    default FloatUnaryOperator andThen(final FloatUnaryOperator after) {
        return t -> after.applyAsFloat(applyAsFloat(t));
    }

    /**
     * Returns a unary operator that always returns its input argument unchanged.
     *
     * @return a unary operator that always returns its input argument
     */
    static FloatUnaryOperator identity() {
        return t -> t;
    }
}