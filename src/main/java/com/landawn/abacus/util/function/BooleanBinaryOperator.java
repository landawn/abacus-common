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
 */
@FunctionalInterface
public interface BooleanBinaryOperator extends Throwables.BooleanBinaryOperator<RuntimeException> { //NOSONAR

    /**
     * Applies this operator to the given operands.
     *
     * @param left the first operand
     * @param right the second operand
     * @return the operator result
     */
    @Override
    boolean applyAsBoolean(boolean left, boolean right);
}