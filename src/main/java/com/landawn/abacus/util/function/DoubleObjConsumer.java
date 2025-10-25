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

import com.landawn.abacus.util.Throwables;

/**
 * Represents an operation that accepts a double-valued argument and an object-valued argument,
 * and returns no result. This is the (double, reference) specialization of {@link java.util.function.BiConsumer}.
 * Unlike most other functional interfaces, {@code DoubleObjConsumer} is expected to operate via side-effects.
 * 
 * <p>This is a functional interface whose functional method is {@link #accept(double, Object)}.
 *
 * <p>Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a>
 *
 * @param <T> the type of the object argument to the operation
 *
 * @see java.util.function.BiConsumer
 */
@FunctionalInterface
public interface DoubleObjConsumer<T> extends Throwables.DoubleObjConsumer<T, RuntimeException> { // NOSONAR
    /**
     * Performs this operation on the given arguments.
     *
     * @param i the double input argument
     * @param t the object input argument
     */
    @Override
    void accept(double i, T t);

    /**
     * Returns a composed {@code DoubleObjConsumer} that performs, in sequence, this operation followed by the {@code after} operation.
     * If performing either operation throws an exception, it is relayed to the caller of the composed operation.
     * If performing this operation throws an exception, the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code DoubleObjConsumer} that performs in sequence this operation followed by the {@code after} operation
     */
    default DoubleObjConsumer<T> andThen(final DoubleObjConsumer<? super T> after) {
        return (i, t) -> {
            accept(i, t);
            after.accept(i, t);
        };
    }
}