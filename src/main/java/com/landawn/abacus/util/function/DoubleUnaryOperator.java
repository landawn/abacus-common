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
 * Represents an operation on a single double-valued operand that produces a double-valued result.
 * This is the primitive type specialization of {@link java.util.function.UnaryOperator} for {@code double}.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsDouble(double)}.</p>
 *
 * <p>This interface extends both {@link Throwables.DoubleUnaryOperator} with {@link RuntimeException}
 * and {@link java.util.function.DoubleUnaryOperator}, providing a bridge between the Throwables-based
 * exception handling and the standard Java functional interface.</p>
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.UnaryOperator
 * @see java.util.function.DoubleUnaryOperator
 */
@FunctionalInterface
public interface DoubleUnaryOperator extends Throwables.DoubleUnaryOperator<RuntimeException>, java.util.function.DoubleUnaryOperator { //NOSONAR
    /**
     * Applies this operator to the given operand.
     *
     * <p>This method performs a unary operation on a double value and returns a double result.
     * The operation could be any mathematical or logical transformation of the input value.</p>
     *
     * <p>Common use cases include:</p>
     * <ul>
     *   <li>Mathematical operations (e.g., square, square root, absolute value)</li>
     *   <li>Scaling or normalization (e.g., multiply by constant, convert units)</li>
     *   <li>Rounding or truncation operations</li>
     *   <li>Trigonometric functions</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleUnaryOperator square = x -> x * x;
     * double result = square.applyAsDouble(5.0);   // Returns 25.0
     *
     * DoubleUnaryOperator toRadians = Math::toRadians;
     * double radians = toRadians.applyAsDouble(180.0);   // Returns Math.PI
     * }</pre>
     *
     * @param operand the operand to which the operation is applied
     * @return the result of applying this operator to the operand
     */
    @Override
    double applyAsDouble(double operand);

    /**
     * Returns a composed operator that first applies the {@code before} operator to its input,
     * and then applies this operator to the result. If evaluation of either operator throws an
     * exception, it is relayed to the caller of the composed operator.
     *
     * <p>This method enables right-to-left function composition. The {@code before} operator
     * is applied first, and its result becomes the input to this operator.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleUnaryOperator multiplyBy2 = x -> x * 2;
     * DoubleUnaryOperator add3 = x -> x + 3;
     *
     * // First adds 3, then multiplies by 2: (x + 3) * 2
     * DoubleUnaryOperator composed = multiplyBy2.compose(add3);
     * double result = composed.applyAsDouble(5);   // Result: 16.0
     * }</pre>
     *
     * @param before the operator to apply before this operator is applied
     * @return a composed operator that first applies the {@code before} operator and then applies this operator
     *
     * @see #andThen(java.util.function.DoubleUnaryOperator)
     */
    @Override
    default DoubleUnaryOperator compose(final java.util.function.DoubleUnaryOperator before) {
        return (final double v) -> applyAsDouble(before.applyAsDouble(v));
    }

    /**
     * Returns a composed operator that first applies this operator to its input, and then applies
     * the {@code after} operator to the result. If evaluation of either operator throws an exception,
     * it is relayed to the caller of the composed operator.
     *
     * <p>This method enables left-to-right function composition. This operator is applied first,
     * and its result becomes the input to the {@code after} operator.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleUnaryOperator multiplyBy2 = x -> x * 2;
     * DoubleUnaryOperator add3 = x -> x + 3;
     *
     * // First multiplies by 2, then adds 3: (x * 2) + 3
     * DoubleUnaryOperator composed = multiplyBy2.andThen(add3);
     * double result = composed.applyAsDouble(5);   // Result: 13.0
     * }</pre>
     *
     * @param after the operator to apply after this operator is applied
     * @return a composed operator that first applies this operator and then applies the {@code after} operator
     *
     * @see #compose(java.util.function.DoubleUnaryOperator)
     */
    @Override
    default DoubleUnaryOperator andThen(final java.util.function.DoubleUnaryOperator after) {
        return (final double t) -> after.applyAsDouble(applyAsDouble(t));
    }

    /**
     * Returns a unary operator that always returns its input argument unchanged.
     * This is the identity function for double values.
     *
     * <p>This method is useful in scenarios where a {@code DoubleUnaryOperator} is required
     * but no transformation should be performed on the input value.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * DoubleUnaryOperator identity = DoubleUnaryOperator.identity();
     * double result = identity.applyAsDouble(42.5);   // Returns 42.5
     *
     * // Useful in conditional operations
     * DoubleUnaryOperator operation = shouldTransform ? x -> x * 2 : DoubleUnaryOperator.identity();
     * }</pre>
     *
     * @return a unary operator that always returns its input argument
     */
    static DoubleUnaryOperator identity() {
        return t -> t;
    }
}
