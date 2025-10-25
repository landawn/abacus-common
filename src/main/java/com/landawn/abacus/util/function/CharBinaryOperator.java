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
 * @see java.util.function.BinaryOperator
 * @see CharUnaryOperator
 */
@FunctionalInterface
public interface CharBinaryOperator extends Throwables.CharBinaryOperator<RuntimeException> { //NOSONAR

    /**
     * Applies this operator to the given char operands.
     * This method performs a binary operation on two char values and returns a char result.
     *
     * @param left the first char operand
     * @param right the second char operand
     * @return the char result of applying this operator to the two operands
     */
    @Override
    char applyAsChar(char left, char right);
}