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
 * Represents an operation upon two char-valued operands and producing a char-valued result.
 * This is the primitive type specialization of {@link java.util.function.BinaryOperator} for {@code char}.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsChar(char, char)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see java.util.function.BinaryOperator
 * @see CharUnaryOperator
 */
@FunctionalInterface
public interface CharBinaryOperator extends Throwables.CharBinaryOperator<RuntimeException> { //NOSONAR
    /**
     * Applies this operator to the given char operands.
     * This method performs a binary operation on two char values and returns a char result.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * CharBinaryOperator max = (a, b) -> a > b ? a : b;
     * char result = max.applyAsChar('A', 'Z');  // Returns 'Z'
     *
     * CharBinaryOperator concatenateAsChar = (a, b) -> (char)(a + b);
     * char sum = concatenateAsChar.applyAsChar('A', 'B');  // Returns char sum
     * }</pre>
     *
     * @param left the first char operand
     * @param right the second char operand
     * @return the char result of applying this operator to the two operands
     */
    @Override
    char applyAsChar(char left, char right);
}
