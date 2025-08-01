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
 * Represents an operation upon three short-valued operands and producing a short-valued result.
 * This is the primitive type specialization of ternary operator for {@code short}.
 * 
 * <p>This is a functional interface whose functional method is {@link #applyAsShort(short, short, short)}.
 * 
 * @see java.util.function.BinaryOperator
 * @see ShortBinaryOperator
 */
@FunctionalInterface
public interface ShortTernaryOperator extends Throwables.ShortTernaryOperator<RuntimeException> { //NOSONAR

    /**
     * Applies this operator to the given operands.
     *
     * @param a the first operand
     * @param b the second operand
     * @param c the third operand
     * @return the operator result as a short value
     */
    @Override
    short applyAsShort(short a, short b, short c);
}