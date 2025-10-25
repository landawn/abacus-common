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
 * Represents an operation that accepts a single {@code byte}-valued argument and returns no result.
 * This is the primitive type specialization of {@link java.util.function.Consumer} for {@code byte}.
 * Unlike most other functional interfaces, {@code ByteConsumer} is expected to operate via side-effects.
 * 
 * <p>This is a functional interface whose functional method is {@link #accept(byte)}.
 *
 */
@FunctionalInterface
public interface ByteConsumer extends Throwables.ByteConsumer<RuntimeException> { //NOSONAR

    /**
     * Performs this operation on the given argument.
     *
     * @param t the input argument
     */
    @Override
    void accept(byte t);

    /**
     * Returns a composed {@code ByteConsumer} that performs, in sequence, this operation followed by the {@code after} operation.
     * If performing either operation throws an exception, it is relayed to the caller of the composed operation.
     * If performing this operation throws an exception, the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code ByteConsumer} that performs in sequence this operation followed by the {@code after} operation
     */
    default ByteConsumer andThen(final ByteConsumer after) {
        return t -> {
            accept(t);
            after.accept(t);
        };
    }
}