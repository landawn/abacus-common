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

/**
 * Represents an operation that accepts a variable number of {@code int}-valued arguments and returns no result.
 * This is the n-arity specialization of {@link IntConsumer}.
 * Unlike most other functional interfaces, {@code IntNConsumer} is expected to operate via side-effects.
 *
 * <p>This is a functional interface whose functional method is {@link #accept(int...)}.
 *
 * @see IntConsumer
 * @see IntBiConsumer
 */
@FunctionalInterface
public interface IntNConsumer {

    /**
     * Performs this operation on the given arguments.
     *
     * @param args the input arguments as a variable-length array of {@code int} values
     */
    void accept(int... args);

    /**
     * Returns a composed {@code IntNConsumer} that performs, in sequence, this operation followed by
     * the {@code after} operation. If performing either operation throws an exception, it is relayed
     * to the caller of the composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code IntNConsumer} that performs in sequence this operation followed by
     *         the {@code after} operation
     */
    default IntNConsumer andThen(final IntNConsumer after) {
        return args -> {
            accept(args);
            after.accept(args);
        };
    }
}