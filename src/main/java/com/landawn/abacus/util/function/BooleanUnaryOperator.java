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
 * Represents an operation on a single {@code boolean}-valued operand that produces a {@code boolean}-valued result.
 * This is the primitive type specialization of {@link UnaryOperator} for {@code boolean}.
 * 
 * <p>This is a functional interface whose functional method is {@link #applyAsBoolean(boolean)}.
 *
 */
@FunctionalInterface
public interface BooleanUnaryOperator extends Throwables.BooleanUnaryOperator<RuntimeException> { //NOSONAR

    /**
     * Applies this operator to the given operand.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanUnaryOperator not = b -> !b;
     * boolean result = not.applyAsBoolean(true); // Returns false
     * }</pre>
     *
     * @param operand the operand
     * @return the operator result
     */
    @Override
    boolean applyAsBoolean(boolean operand);

    /**
     * Returns a composed operator that first applies the {@code before} operator to its input, and then applies this operator to the result.
     * If evaluation of either operator throws an exception, it is relayed to the caller of the composed operator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanUnaryOperator not = b -> !b;
     * BooleanUnaryOperator doubleNot = not.compose(not); // Identity operation
     * }</pre>
     *
     * @param before the operator to apply before this operator is applied. Must not be {@code null}.
     * @return a composed operator that first applies the {@code before} operator and then applies this operator
     * @see #andThen(BooleanUnaryOperator)
     */
    default BooleanUnaryOperator compose(final BooleanUnaryOperator before) {
        return v -> applyAsBoolean(before.applyAsBoolean(v));
    }

    /**
     * Returns a composed operator that first applies this operator to its input, and then applies the {@code after} operator to the result.
     * If evaluation of either operator throws an exception, it is relayed to the caller of the composed operator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanUnaryOperator not = b -> !b;
     * BooleanUnaryOperator doubleNot = not.andThen(not); // Identity operation
     * }</pre>
     *
     * @param after the operator to apply after this operator is applied. Must not be {@code null}.
     * @return a composed operator that first applies this operator and then applies the {@code after} operator
     * @see #compose(BooleanUnaryOperator)
     */
    default BooleanUnaryOperator andThen(final BooleanUnaryOperator after) {
        return t -> after.applyAsBoolean(applyAsBoolean(t));
    }

    /**
     * Returns a unary operator that always returns its input argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanUnaryOperator id = BooleanUnaryOperator.identity();
     * boolean result = id.applyAsBoolean(true); // Returns true
     * }</pre>
     *
     * @return a unary operator that always returns its input argument
     */
    static BooleanUnaryOperator identity() {
        return t -> t;
    }
}