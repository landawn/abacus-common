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
 * Represents an operation that accepts two {@code byte}-valued arguments and returns no result.
 * This is the primitive type specialization of {@link BiConsumer} for {@code byte}.
 * 
 * <p>This is a functional interface whose functional method is {@link #accept(byte, byte)}.
 *
 */
@FunctionalInterface
public interface ByteBiConsumer extends Throwables.ByteBiConsumer<RuntimeException> { //NOSONAR

    /**
     * Performs this operation on the given arguments.
     *
     * @param t the first input argument
     * @param u the second input argument
     */
    @Override
    void accept(byte t, byte u);

    /**
     * Returns a composed {@code ByteBiConsumer} that performs, in sequence, this operation followed by the {@code after} operation.
     * If performing either operation throws an exception, it is relayed to the caller of the composed operation.
     * If performing this operation throws an exception, the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code ByteBiConsumer} that performs in sequence this operation followed by the {@code after} operation
     */
    default ByteBiConsumer andThen(final ByteBiConsumer after) {
        return (t, u) -> {
            accept(t, u);
            after.accept(t, u);
        };
    }
}