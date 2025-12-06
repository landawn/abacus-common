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
 * Represents an operation on a single short-valued operand that produces a short-valued result.
 * This is the primitive type specialization of {@link java.util.function.UnaryOperator} for {@code short}.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsShort(short)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.UnaryOperator
 */
@FunctionalInterface
public interface ShortUnaryOperator extends Throwables.ShortUnaryOperator<RuntimeException> { //NOSONAR
    /**
     * Applies this operator to the given operand.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortUnaryOperator identity = ShortUnaryOperator.identity();
     * short result1 = identity.applyAsShort((short) 42);   // returns 42
     *
     * ShortUnaryOperator doubler = x -> (short) (x * 2);
     * short result2 = doubler.applyAsShort((short) 21);   // returns 42
     *
     * ShortUnaryOperator negate = x -> (short) -x;
     * short result3 = negate.applyAsShort((short) 10);   // returns -10
     *
     * ShortUnaryOperator abs = x -> (short) Math.abs(x);
     * short result4 = abs.applyAsShort((short) -25);   // returns 25
     * }</pre>
     *
     * @param operand the operand
     * @return the operator result
     */
    @Override
    short applyAsShort(short operand);

    /**
     * Returns a composed operator that first applies the {@code before} operator to its input,
     * and then applies this operator to the result. If evaluation of either operator throws an
     * exception, it is relayed to the caller of the composed operator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortUnaryOperator doubler = x -> (short) (x * 2);
     * ShortUnaryOperator addTen = x -> (short) (x + 10);
     * ShortUnaryOperator doubleThenAddTen = addTen.compose(doubler);
     * short result = doubleThenAddTen.applyAsShort((short) 5);   // returns 20 (5*2 + 10)
     * }</pre>
     *
     * @param before the operator to apply before this operator is applied. Must not be {@code null}.
     * @return a composed operator that first applies the {@code before} operator and then applies this operator
     *
     * @see #andThen(ShortUnaryOperator)
     */
    default ShortUnaryOperator compose(final ShortUnaryOperator before) {
        return v -> applyAsShort(before.applyAsShort(v));
    }

    /**
     * Returns a composed operator that first applies this operator to its input, and then applies
     * the {@code after} operator to the result. If evaluation of either operator throws an exception,
     * it is relayed to the caller of the composed operator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortUnaryOperator doubler = x -> (short) (x * 2);
     * ShortUnaryOperator addTen = x -> (short) (x + 10);
     * ShortUnaryOperator doubleThenAddTen = doubler.andThen(addTen);
     * short result = doubleThenAddTen.applyAsShort((short) 5);   // returns 20 (5*2 + 10)
     * }</pre>
     *
     * @param after the operator to apply after this operator is applied. Must not be {@code null}.
     * @return a composed operator that first applies this operator and then applies the {@code after} operator
     *
     * @see #compose(ShortUnaryOperator)
     */
    default ShortUnaryOperator andThen(final ShortUnaryOperator after) {
        return t -> after.applyAsShort(applyAsShort(t));
    }

    /**
     * Returns a unary operator that always returns its input argument.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ShortUnaryOperator identity = ShortUnaryOperator.identity();
     * short result = identity.applyAsShort((short) 42);   // returns 42
     *
     * // Useful in stream operations or as a default operator
     * ShortUnaryOperator op = condition ? doubler : ShortUnaryOperator.identity();
     * }</pre>
     *
     * @return a unary operator that always returns its input argument
     */
    static ShortUnaryOperator identity() {
        return t -> t;
    }
}
