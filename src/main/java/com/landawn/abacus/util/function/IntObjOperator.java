/*
 * Copyright (C) 2024 HaiYang Li
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

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.util.Throwables;

/**
 * Represents an operation upon an {@code int}-valued operand and an object-valued operand which produces
 * an {@code int}-valued result. This is a primitive specialization of {@link java.util.function.BiFunction}
 * for the case where the first argument and the result are of {@code int} type.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsInt(int, Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a>
 *
 * @param <T> the type of the object argument to the operator
 * @see java.util.function.BiFunction
 * @see IntBinaryOperator
 */
@Beta
@FunctionalInterface
public interface IntObjOperator<T> extends Throwables.IntObjOperator<T, RuntimeException> { //NOSONAR

    /**
     * Applies this operator to the given operands.
     *
     * @param operand the {@code int} operand
     * @param obj the object operand
     * @return the operator result
     */
    @Override
    int applyAsInt(int operand, T obj);
}