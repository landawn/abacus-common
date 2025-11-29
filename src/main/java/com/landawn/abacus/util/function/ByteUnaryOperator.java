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
 * Represents an operation on a single byte-valued operand that produces a byte-valued result.
 * This is the primitive type specialization of {@link java.util.function.UnaryOperator} for {@code byte}.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsByte(byte)}.
 *
 * @see java.util.function.UnaryOperator
 * @see ByteBinaryOperator
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface ByteUnaryOperator extends Throwables.ByteUnaryOperator<RuntimeException> { //NOSONAR
    /**
     * Applies this operator to the given byte operand.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteUnaryOperator doubler = value -> (byte) (value * 2);
     * byte result = doubler.applyAsByte((byte) 5);  // Returns 10
     * }</pre>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ByteUnaryOperator doubler = value -> (byte) (value * 2);
     * byte result = doubler.applyAsByte((byte) 5);  // Returns 10
     * }</pre>
     *
     * @param operand the byte operand
     * @return the byte result of applying this operator to the operand
     */
    @Override
    byte applyAsByte(byte operand);

    /**
     * Returns a composed operator that first applies the {@code before} operator to its input,
     * and then applies this operator to the result. If evaluation of either operator throws
     * an exception, it is relayed to the caller of the composed operator.
     *
     * @param before the operator to apply before this operator is applied. Must not be {@code null}.
     * @return a composed operator that first applies the {@code before} operator and then
     *         applies this operator
     *
     * @see #andThen(ByteUnaryOperator)
     */
    default ByteUnaryOperator compose(final ByteUnaryOperator before) {
        return v -> applyAsByte(before.applyAsByte(v));
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
     * @see #compose(ByteUnaryOperator)
     */
    default ByteUnaryOperator andThen(final ByteUnaryOperator after) {
        return t -> after.applyAsByte(applyAsByte(t));
    }

    /**
     * Returns a unary operator that always returns its input argument.
     * This is the identity function for byte values.
     *
     * @return a unary operator that always returns its input argument
     */
    static ByteUnaryOperator identity() {
        return t -> t;
    }
}
