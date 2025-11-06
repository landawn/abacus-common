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
 * Represents an operation upon three char-valued operands and producing a char-valued result.
 * This is the primitive type specialization of {@link TriFunction} for char.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsChar(char, char, char)}.
 *
 * @see CharBinaryOperator
 * @see CharUnaryOperator
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface CharTernaryOperator extends Throwables.CharTernaryOperator<RuntimeException> { //NOSONAR
    /**
     * Applies this operator to the given char operands.
     * This method performs a ternary operation on three char values and returns a char result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharTernaryOperator firstIfUpperElseSecond = (a, b, c) ->
     *     Character.isUpperCase(a) ? a : b;
     * char result = firstIfUpperElseSecond.applyAsChar('A', 'b', 'c'); // Returns 'A'
     *
     * CharTernaryOperator maxOfThree = (a, b, c) -> (char) Math.max(a, Math.max(b, c));
     * char max = maxOfThree.applyAsChar('A', 'Z', 'M'); // Returns 'Z'
     * }</pre>
     *
     * @param a the first char operand
     * @param b the second char operand
     * @param c the third char operand
     * @return the char result of applying this operator to the three operands
     */
    @Override
    char applyAsChar(char a, char b, char c);

    /**
     * Converts this {@code CharTernaryOperator} to a {@code Throwables.CharTernaryOperator} that can throw a checked exception.
     * This method provides a way to use this operator in contexts that require explicit exception handling.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharTernaryOperator operator = (...) -> { ... };
     * var throwableOperator = operator.toThrowable();
     * // Can now be used in contexts that handle checked exceptions
     * }</pre>
     *
     * @param <E> the type of exception that the returned operator can throw
     * @return a {@code Throwables.CharTernaryOperator} view of this operator that can throw exceptions of type {@code E}
     */
    default <E extends Throwable> Throwables.CharTernaryOperator<E> toThrowable() {
        return (Throwables.CharTernaryOperator<E>) this;
    }

}
