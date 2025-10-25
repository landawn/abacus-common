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
 * Represents an operation that accepts two double-valued arguments and returns no result.
 * This is the two-arity specialization of {@link java.util.function.Consumer}.
 * Unlike most other functional interfaces, {@code DoubleBiConsumer} is expected to operate via side-effects.
 * 
 * <p>This is a functional interface whose functional method is {@link #accept(double, double)}.
 *
 * @see java.util.function.Consumer
 * @see java.util.function.BiConsumer
 * @see DoubleConsumer
 */
@FunctionalInterface
public interface DoubleBiConsumer extends Throwables.DoubleBiConsumer<RuntimeException> { //NOSONAR

    /**
     * Performs this operation on the given arguments.
     *
     * @param t the first double input argument
     * @param u the second double input argument
     */
    @Override
    void accept(double t, double u);

    /**
     * Returns a composed {@code DoubleBiConsumer} that performs, in sequence, this operation followed by the {@code after} operation.
     * If performing either operation throws an exception, it is relayed to the caller of the composed operation.
     * If performing this operation throws an exception, the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code DoubleBiConsumer} that performs in sequence this operation followed by the {@code after} operation
     */
    default DoubleBiConsumer andThen(final DoubleBiConsumer after) {
        return (t, u) -> {
            accept(t, u);
            after.accept(t, u);
        };
    }
}