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
 * Represents an operation on a single char-valued operand that produces a char-valued result.
 * This is the primitive type specialization of {@link java.util.function.UnaryOperator} for {@code char}.
 * 
 * <p>This is a functional interface whose functional method is {@link #applyAsChar(char)}.
 * 
 * @see java.util.function.UnaryOperator
 * @see CharBinaryOperator
 */
@FunctionalInterface
public interface CharUnaryOperator extends Throwables.CharUnaryOperator<RuntimeException> { //NOSONAR

    /**
     * Applies this operator to the given char operand.
     *
     * @param operand the char operand
     * @return the char result of applying this operator to the operand
     */
    @Override
    char applyAsChar(char operand);

    /**
     * Returns a composed operator that first applies the {@code before} operator to its input,
     * and then applies this operator to the result. If evaluation of either operator throws
     * an exception, it is relayed to the caller of the composed operator.
     *
     * @param before the operator to apply before this operator is applied. Must not be {@code null}.
     * @return a composed operator that first applies the {@code before} operator and then
     *         applies this operator
     * 
     * @see #andThen(CharUnaryOperator)
     */
    default CharUnaryOperator compose(final CharUnaryOperator before) {
        return v -> applyAsChar(before.applyAsChar(v));
    }

    /**
     * Returns a composed operator that first applies this operator to its input, and then
     * applies the {@code after} operator to the result. If evaluation of either operator
     * throws an exception, it is relayed to the caller of the composed operator.
     *
     * @param after the operator to apply after this operator is applied. Must not be {@code null}.
     * @return a composed operator that first applies this operator and then applies the
     *         {@code after} operator
     * 
     * @see #compose(CharUnaryOperator)
     */
    default CharUnaryOperator andThen(final CharUnaryOperator after) {
        return t -> after.applyAsChar(applyAsChar(t));
    }

    /**
     * Returns a unary operator that always returns its input argument.
     * This is the identity function for char values.
     *
     * @return a unary operator that always returns its input argument
     */
    static CharUnaryOperator identity() {
        return t -> t;
    }
}
