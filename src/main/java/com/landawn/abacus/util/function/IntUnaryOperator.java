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
 * Represents an operation on a single {@code int}-valued operand that produces
 * an {@code int}-valued result. This is the primitive type specialization of
 * {@link java.util.function.UnaryOperator} for {@code int}.
 *
 * <p>This interface extends both {@link Throwables.IntUnaryOperator} with
 * {@link RuntimeException} and {@link java.util.function.IntUnaryOperator},
 * providing compatibility with the Java standard library while supporting the
 * abacus-common framework's exception handling capabilities.
 *
 * <p>This is a functional interface whose functional method is
 * {@link #applyAsInt(int)}.
 *
 * <p>Refer to JDK API documentation at:
 * <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">
 * https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.UnaryOperator
 * @see java.util.function.IntUnaryOperator
 */
@FunctionalInterface
public interface IntUnaryOperator extends Throwables.IntUnaryOperator<RuntimeException>, java.util.function.IntUnaryOperator { //NOSONAR
    /**
     * Applies this operator to the given operand.
     *
     * <p>The operator transforms an int value into another int value.
     * Common use cases include:
     * <ul>
     *   <li>Mathematical operations (increment, decrement, negation, absolute value)</li>
     *   <li>Bit manipulation (shifting, masking)</li>
     *   <li>Scaling or transformation operations</li>
     *   <li>Applying business rules or constraints to integer values</li>
     * </ul>
     *
     * @param operand the operand to which the operation is applied
     * @return the operator result
     */
    @Override
    int applyAsInt(int operand);

    /**
     * Returns a composed operator that first applies the {@code before}
     * operator to its input, and then applies this operator to the result.
     * If evaluation of either operator throws an exception, it is relayed to
     * the caller of the composed operator.
     *
     * <p>This method enables operator composition, creating a pipeline where
     * the output of the {@code before} operator becomes the input to this operator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntUnaryOperator multiplyBy2 = x -> x * 2;
     * IntUnaryOperator add5 = x -> x + 5;
     * IntUnaryOperator add5ThenMultiplyBy2 = multiplyBy2.compose(add5);
     * int result = add5ThenMultiplyBy2.applyAsInt(10); // (10 + 5) * 2 = 30
     * }</pre>
     *
     * @param before the operator to apply before this operator is applied.
     *               Must not be {@code null}.
     * @return a composed operator that first applies the {@code before}
     *         operator and then applies this operator
     *
     * @see #andThen(java.util.function.IntUnaryOperator)
     */
    @Override
    default IntUnaryOperator compose(final java.util.function.IntUnaryOperator before) {
        return (final int v) -> applyAsInt(before.applyAsInt(v));
    }

    /**
     * Returns a composed operator that first applies this operator to
     * its input, and then applies the {@code after} operator to the result.
     * If evaluation of either operator throws an exception, it is relayed to
     * the caller of the composed operator.
     *
     * <p>This method enables operator composition, creating a pipeline where
     * the output of this operator becomes the input to the {@code after} operator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntUnaryOperator multiplyBy2 = x -> x * 2;
     * IntUnaryOperator add5 = x -> x + 5;
     * IntUnaryOperator multiplyBy2ThenAdd5 = multiplyBy2.andThen(add5);
     * int result = multiplyBy2ThenAdd5.applyAsInt(10); // (10 * 2) + 5 = 25
     * }</pre>
     *
     * @param after the operator to apply after this operator is applied.
     *              Must not be {@code null}.
     * @return a composed operator that first applies this operator and then
     *         applies the {@code after} operator
     *
     * @see #compose(java.util.function.IntUnaryOperator)
     */
    @Override
    default IntUnaryOperator andThen(final java.util.function.IntUnaryOperator after) {
        return (final int t) -> after.applyAsInt(applyAsInt(t));
    }

    /**
     * Returns a unary operator that always returns its input argument.
     *
     * <p>This is useful as a default operator or when an operator is required
     * but no transformation is needed.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntUnaryOperator identity = IntUnaryOperator.identity();
     * int result = identity.applyAsInt(42); // 42
     * }</pre>
     *
     * @return a unary operator that always returns its input argument
     */
    static IntUnaryOperator identity() {
        return t -> t;
    }
}
