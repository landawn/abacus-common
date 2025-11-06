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
 * Represents an operation on a single long-valued operand that produces a long-valued result.
 * This is the primitive type specialization of {@code UnaryOperator} for {@code long}.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsLong(long)}.
 *
 * <p>The interface extends both {@code Throwables.LongUnaryOperator} with {@code RuntimeException}
 * and {@code java.util.function.LongUnaryOperator}, providing compatibility with the Java standard library
 * while adding function composition capabilities.
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * LongUnaryOperator increment = x -> x + 1;
 * LongUnaryOperator doubleValue = x -> x * 2;
 * LongUnaryOperator incrementThenDouble = increment.andThen(doubleValue);
 *
 * long result = incrementThenDouble.applyAsLong(5L); // returns 12L ((5 + 1) * 2)
 * }</pre>
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.LongUnaryOperator
 * @see java.util.function.UnaryOperator
 */
@FunctionalInterface
public interface LongUnaryOperator extends Throwables.LongUnaryOperator<RuntimeException>, java.util.function.LongUnaryOperator { //NOSONAR
    /**
     * Applies this operator to the given operand.
     *
     * <p>This method takes a long value as input and produces a long result.
     * The implementation defines how the operand is transformed.
     *
     * <p>Common implementations include:
     * <ul>
     *   <li>Mathematical operations (increment, decrement, negation)</li>
     *   <li>Bitwise operations (complement, shifts)</li>
     *   <li>Scaling operations (multiply/divide by constant)</li>
     *   <li>Custom transformations</li>
     * </ul>
     *
     * @param operand the operand
     * @return the operator result
     */
    @Override
    long applyAsLong(long operand);

    /**
     * Returns a composed operator that first applies the {@code before}
     * operator to its input, and then applies this operator to the result.
     *
     * <p>If evaluation of either operator throws an exception, it is relayed to
     * the caller of the composed operator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongUnaryOperator multiplyBy3 = x -> x * 3;
     * LongUnaryOperator add10 = x -> x + 10;
     * LongUnaryOperator add10ThenMultiplyBy3 = multiplyBy3.compose(add10);
     *
     * long result = add10ThenMultiplyBy3.applyAsLong(5L); // returns 45L ((5 + 10) * 3)
     * }</pre>
     *
     * @param before the operator to apply before this operator is applied
     * @return a composed operator that first applies the {@code before}
     *         operator and then applies this operator
     *
     * @see #andThen(java.util.function.LongUnaryOperator)
     */
    @Override
    default LongUnaryOperator compose(final java.util.function.LongUnaryOperator before) {
        return (final long v) -> applyAsLong(before.applyAsLong(v));
    }

    /**
     * Returns a composed operator that first applies this operator to its input,
     * and then applies the {@code after} operator to the result.
     *
     * <p>If evaluation of either operator throws an exception, it is relayed to
     * the caller of the composed operator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongUnaryOperator multiplyBy2 = x -> x * 2;
     * LongUnaryOperator subtract5 = x -> x - 5;
     * LongUnaryOperator multiplyBy2ThenSubtract5 = multiplyBy2.andThen(subtract5);
     *
     * long result = multiplyBy2ThenSubtract5.applyAsLong(10L); // returns 15L ((10 * 2) - 5)
     * }</pre>
     *
     * @param after the operator to apply after this operator is applied
     * @return a composed operator that first applies this operator and then
     *         applies the {@code after} operator
     *
     * @see #compose(java.util.function.LongUnaryOperator)
     */
    @Override
    default LongUnaryOperator andThen(final java.util.function.LongUnaryOperator after) {
        return (final long t) -> after.applyAsLong(applyAsLong(t));
    }

    /**
     * Returns a unary operator that always returns its input argument.
     *
     * <p>This is useful as a default operator or in functional pipelines where
     * no transformation is needed for certain conditions.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongUnaryOperator op = condition ? x -> x * 2 : LongUnaryOperator.identity();
     * long result = op.applyAsLong(5L); // returns 10L if condition is true, 5L otherwise
     * }</pre>
     *
     * @return a unary operator that always returns its input argument
     */
    static LongUnaryOperator identity() {
        return t -> t;
    }

    /**
     * Converts this {@code LongUnaryOperator} to a {@code Throwables.LongUnaryOperator} that can throw a checked exception.
     * This method provides a way to use this operator in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * LongUnaryOperator operator = (...) -> { ... };
     * var throwableOperator = operator.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned operator can throw
     * @return a {@code Throwables.LongUnaryOperator} view of this operator that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.LongUnaryOperator<E> toThrowable() {
        return (Throwables.LongUnaryOperator<E>) this;
    }

}
