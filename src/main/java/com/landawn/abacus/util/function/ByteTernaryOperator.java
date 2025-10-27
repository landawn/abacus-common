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
 * Represents an operation upon three byte-valued operands and producing a byte-valued result.
 * This is the primitive type specialization of {@link TriFunction} for byte.
 * 
 * <p>This is a functional interface whose functional method is {@link #applyAsByte(byte, byte, byte)}.
 * 
 * @see ByteBinaryOperator
 * @see ByteUnaryOperator
 */
@FunctionalInterface
public interface ByteTernaryOperator extends Throwables.ByteTernaryOperator<RuntimeException> { //NOSONAR

    /**
     * Applies this operator to the given byte operands.
     * This method performs a ternary operation on three byte values and returns a byte result.
     *
     * @param a the first byte operand
     * @param b the second byte operand
     * @param c the third byte operand
     * @return the byte result of applying this operator to the three operands
     */
    @Override
    byte applyAsByte(byte a, byte b, byte c);
}
