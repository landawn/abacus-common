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
 * Represents an operation upon two {@code boolean} operands and producing a {@code boolean} result.
 * This is the primitive type specialization of {@link BinaryOperator} for {@code boolean}.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsBoolean(boolean, boolean)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface BooleanBinaryOperator extends Throwables.BooleanBinaryOperator<RuntimeException> { //NOSONAR
    /**
     * Applies this operator to the given operands.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanBinaryOperator and = (a, b) -> a && b;
     * boolean result = and.applyAsBoolean(true, false); // Returns false
     * }</pre>
     *
     * @param left the first operand
     * @param right the second operand
     * @return the operator result
     */
    @Override
    boolean applyAsBoolean(boolean left, boolean right);

    /**
     * Converts this {@code BooleanBinaryOperator} to a {@code Throwables.BooleanBinaryOperator} that can throw a checked exception.
     * This method provides a way to use this operator in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * BooleanBinaryOperator operator = (...) -> { ... };
     * var throwableOperator = operator.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned operator can throw
     * @return a {@code Throwables.BooleanBinaryOperator} view of this operator that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.BooleanBinaryOperator<E> toThrowable() {
        return (Throwables.BooleanBinaryOperator<E>) this;
    }

}
