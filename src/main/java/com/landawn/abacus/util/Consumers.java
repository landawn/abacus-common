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
            private final MutableInt idx = new MutableInt(0);

            @Override
            public void accept(final T t) {
                action.accept(idx.getAndIncrement(), t);
            }
        };
    }
}
