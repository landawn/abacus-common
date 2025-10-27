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
 * Represents an operation that accepts three float-valued arguments and returns no result.
 * This is a functional interface whose functional method is {@link #accept(float, float, float)}.
 * 
 * <p>This is a primitive type specialization of consumer for three {@code float} arguments.
 * Unlike most other functional interfaces, {@code FloatTriConsumer} is expected to operate via side-effects.</p>
 * 
 * @since 1.0
 * @see java.util.function.Consumer
 * @see java.util.function.BiConsumer
 */
@FunctionalInterface
public interface FloatTriConsumer extends Throwables.FloatTriConsumer<RuntimeException> { //NOSONAR

    /**
     * Performs this operation on the given float arguments.
     * 
     * <p>The behavior of this method is implementation-dependent and may process
     * the float values in any manner required by the specific use case, typically
     * involving side-effects such as modifying state or producing output.</p>
     *
     * @param a the first float input argument
     * @param b the second float input argument
     * @param c the third float input argument
     */
    @Override
    void accept(float a, float b, float c);

    /**
     * Returns a composed {@code FloatTriConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation. If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation. Must not be {@code null}.
     * @return a composed {@code FloatTriConsumer} that performs in sequence this
     *         operation followed by the {@code after} operation
     */
    default FloatTriConsumer andThen(final FloatTriConsumer after) {
        return (a, b, c) -> {
            accept(a, b, c);
            after.accept(a, b, c);
        };
    }
}
