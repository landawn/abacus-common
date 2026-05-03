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
 * Represents an operation upon three {@code int}-valued operands and producing an {@code int}-valued result.
 * This is the three-arity extension of {@link IntBinaryOperator} for {@code int} values.
 *
 * <p>Note: arithmetic operations on {@code int} values are subject to integer overflow
 * (silently wraps around). Use {@link Math#addExact(int, int)} or similar methods
 * when overflow detection is required.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsInt(int, int, int)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 *
 * @see IntBinaryOperator
 * @see java.util.function.BinaryOperator
 */
@FunctionalInterface
public interface IntTernaryOperator extends Throwables.IntTernaryOperator<RuntimeException> { //NOSONAR
    /**
     * Applies this operator to the given operands.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntTernaryOperator summer = (a, b, c) -> a + b + c;
     * int result = summer.applyAsInt(1, 2, 3);   // Returns 6
     * }</pre>
     *
     * @param a the first {@code int} operand
     * @param b the second {@code int} operand
     * @param c the third {@code int} operand
     * @return the {@code int}-valued result of applying this operator to the three operands
     */
    @Override
    int applyAsInt(int a, int b, int c);
}
