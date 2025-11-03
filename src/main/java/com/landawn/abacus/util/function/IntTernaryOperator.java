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
 * This is the three-arity primitive specialization of {@link java.util.function.Function}.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsInt(int, int, int)}.
 *
 * @see IntBinaryOperator
 * @see java.util.function.UnaryOperator
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
 */
@FunctionalInterface
public interface IntTernaryOperator extends Throwables.IntTernaryOperator<RuntimeException> { //NOSONAR
    /**
     * Applies this operator to the given operands.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntTernaryOperator summer = (a, b, c) -> a + b + c;
     * int result = summer.applyAsInt(1, 2, 3); // Returns 6
     * }</pre>
     *
     * @param a the first operand
     * @param b the second operand
     * @param c the third operand
     * @return the operator result
     */
    @Override
    int applyAsInt(int a, int b, int c);
}
