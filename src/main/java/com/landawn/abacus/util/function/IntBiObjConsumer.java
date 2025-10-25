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
 * Represents an operation that accepts one {@code int}-valued argument and two object-valued arguments,
 * and returns no result. This is a three-arity specialization of {@link java.util.function.Consumer}.
 * Unlike most other functional interfaces, {@code IntBiObjConsumer} is expected to operate via side-effects.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(int, Object, Object)}.
 *
 * @param <T> the type of the first object argument to the operation
 * @param <U> the type of the second object argument to the operation
 * @see java.util.function.Consumer
 * @see IntObjConsumer
 */
@FunctionalInterface
public interface IntBiObjConsumer<T, U> extends Throwables.IntBiObjConsumer<T, U, RuntimeException> { //NOSONAR

    /**
     * Performs this operation on the given arguments.
     *
     * @param i the {@code int} argument
     * @param t the first object argument
     * @param u the second object argument
     */
    @Override
    void accept(int i, T t, U u);

    /**
     * Returns a composed {@code IntBiObjConsumer} that performs, in sequence, this operation followed by
     * the {@code after} operation. If performing either operation throws an exception, it is relayed
     * to the caller of the composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code IntBiObjConsumer} that performs in sequence this operation followed by
     *         the {@code after} operation
     */
    default IntBiObjConsumer<T, U> andThen(final IntBiObjConsumer<? super T, ? super U> after) {
        return (i, t, u) -> {
            accept(i, t, u);
            after.accept(i, t, u);
        };
    }
}