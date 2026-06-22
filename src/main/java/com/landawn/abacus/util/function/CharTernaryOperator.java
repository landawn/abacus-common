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
 * Represents an operation upon three {@code char}-valued operands and producing a {@code char}-valued result.
 * This is the three-arity primitive type specialization analogous to {@link CharBinaryOperator}.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsChar(char, char, char)}.
 *
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see CharBinaryOperator
 * @see CharUnaryOperator
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
     * char result = firstIfUpperElseSecond.applyAsChar('A', 'b', 'c');   // Returns 'A'
     *
     * CharTernaryOperator maxOfThree = (a, b, c) -> (char) Math.max(a, Math.max(b, c));
     * char max = maxOfThree.applyAsChar('A', 'Z', 'M');   // Returns 'Z'
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
     * Returns this object as a {@link Throwables.CharTernaryOperator} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.CharTernaryOperator}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.CharTernaryOperator}
     * @return a {@link Throwables.CharTernaryOperator} view of this object
     */
    default <E extends Throwable> Throwables.CharTernaryOperator<E> toThrowable() {
        return (Throwables.CharTernaryOperator<E>) this;
    }
}
