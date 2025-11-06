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

/**
 * Represents an operation on three float-valued operands that produces a float-valued result.
 * This is a functional interface whose functional method is {@link #applyAsFloat(float, float, float)}.
 *
 * <p>This is a primitive type specialization of ternary operator for {@code float}.</p>
 *
 * <p>This interface is similar to {@link java.util.function.BinaryOperator} but accepts three arguments
 * instead of two.</p>
 *
 * @see java.util.function.BinaryOperator
 * @see java.util.function.DoubleBinaryOperator
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface FloatTernaryOperator extends Throwables.FloatTernaryOperator<RuntimeException> { //NOSONAR
    /**
     * Applies this operator to the given float operands.
     *
     * <p>The implementation defines how the three float values are combined to produce
     * the result. Common uses include mathematical operations like fused multiply-add
     * (a * b + c) or conditional operations.</p>
     *
     * @param a the first float operand
     * @param b the second float operand
     * @param c the third float operand
     * @return the float result of applying this operator to the given operands
     */
    @Override
    float applyAsFloat(float a, float b, float c);

    /**
     * Converts this {@code FloatTernaryOperator} to a {@code Throwables.FloatTernaryOperator} that can throw a checked exception.
     * This method provides a way to use this operator in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FloatTernaryOperator operator = (...) -> { ... };
     * var throwableOperator = operator.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned operator can throw
     * @return a {@code Throwables.FloatTernaryOperator} view of this operator that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.FloatTernaryOperator<E> toThrowable() {
        return (Throwables.FloatTernaryOperator<E>) this;
    }

}
