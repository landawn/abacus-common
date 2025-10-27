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
 * Represents an operation that accepts a variable number of float-valued arguments and returns no result.
 * This is a functional interface whose functional method is {@link #accept(float...)}.
 * 
 * <p>This is a primitive type specialization of {@code Consumer} for {@code float} varargs.</p>
 * 
 * @since 1.0
 * @see java.util.function.Consumer
 */
@FunctionalInterface
public interface FloatNConsumer {

    /**
     * Performs this operation on the given float arguments.
     * 
     * <p>The behavior of this method is implementation-dependent and may process
     * the float values in any manner required by the specific use case.</p>
     *
     * @param args the float values to be processed. May be empty, in which case
     *             the consumer should handle the empty array appropriately.
     */
    void accept(float... args);

    /**
     * Returns a composed {@code FloatNConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code FloatNConsumer} that performs in sequence this
     *         operation followed by the {@code after} operation
     */
    default FloatNConsumer andThen(final FloatNConsumer after) {
        return args -> {
            accept(args);
            after.accept(args);
        };
    }
}
