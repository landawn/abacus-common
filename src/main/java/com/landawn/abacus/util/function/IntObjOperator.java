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
 * <p>The parameter order follows the {@code IntObj*} convention: the {@code int} argument comes
 * first, followed by the object argument. The result is always an {@code int} value.
 *
 * <p>This is a functional interface whose functional method is {@link #applyAsInt(int, Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a></p>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IntObjOperator<String> repeater = (count, str) -> str.length() * count;
     * int result = repeater.applyAsInt(3, "hello");   // Returns 15 (5 * 3)
     * }</pre>
     *
     * @param t the {@code int} operand
     * @param u the object operand of type {@code T}
     * @return the {@code int}-valued result of applying this operator to the given operands
     */
    // @ai-ignore IntObj*/ObjInt* argument order convention - intentional: class name prefix determines parameter order. IntObj* has int first; ObjInt* has object first. Do not suggest reordering.
    @Override
    int applyAsInt(int t, T u);

    /**
     * Returns this object as a {@link Throwables.IntObjOperator} view.
     *
     * <p>The returned object has the same behavior as this one. This method does not translate
     * exceptions or make the original implementation capable of throwing new checked exceptions; the
     * exception type parameter is for target-type compatibility with APIs that accept {@code Throwables.IntObjOperator}.
     *
     * @param <E> the target exception type for compatibility with {@code Throwables.IntObjOperator}
     * @return a {@link Throwables.IntObjOperator} view of this object
     */
    default <E extends Throwable> Throwables.IntObjOperator<T, E> toThrowable() {
        return (Throwables.IntObjOperator<T, E>) this;
    }
}
