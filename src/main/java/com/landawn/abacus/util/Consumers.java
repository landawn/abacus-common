/*
 * Copyright (c) 2026, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.annotation.Stateful;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.IntObjConsumer;

/**
 * Utility class providing various Consumer implementations and factory methods.
 * This class contains methods for creating indexed consumers.
 *
 * <p>This class is a top-level sibling of {@link Fn} (formerly nested as {@code Fn.Consumers}),
 * not a nested type. Use {@link Fn} for the general functional-interface factory and {@link Fnn}
 * for {@link Throwables} variants that can declare checked exceptions. For two-argument consumers
 * see {@link BiConsumers}; for three-argument consumers see {@link TriConsumers}.</p>
 *
 * @see Fn
 * @see Fnn
 * @see BiConsumers
 * @see TriConsumers
 * @see Functions
 * @see Predicates
 */
public final class Consumers {
    private Consumers() {
    }

    /**
     * Returns a stateful Consumer that accepts elements based on their index position.
     * The consumer maintains an internal counter that increments with each accept call.
     * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Consumers.indexed((i, s) -> System.out.println(i + ":" + s)).accept("hello");  // prints 0:hello
     * }</pre>
     *
     * <p>The returned callback uses indices from zero through {@link Integer#MAX_VALUE}. Later calls
     * throw {@link ArithmeticException} without invoking user code. An invocation consumes its index
     * even when user code throws.</p>
     *
     * @param <T> the type of the input to the consumer
     * @param action the IntObjConsumer that accepts an index and element
     * @return a stateful Consumer that applies the given IntObjConsumer with an incrementing index
     * @throws IllegalArgumentException if {@code action} is {@code null}.
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static <T> Consumer<T> indexed(final IntObjConsumer<T> action) throws IllegalArgumentException {
        N.checkArgNotNull(action, cs.action);

        return new Consumer<>() {
            private long idx;

            /**
             * {@inheritDoc}
             * @throws ArithmeticException if all nonnegative {@code int} indices have already been used by prior invocations
             */
            @Override
            public void accept(final T t) throws ArithmeticException {
                // Keep exhaustion representable and reject before invoking user code.
                if (idx > Integer.MAX_VALUE) {
                    throw new ArithmeticException("Index exceeds Integer.MAX_VALUE");
                }

                action.accept((int) idx++, t);
            }
        };
    }
}
