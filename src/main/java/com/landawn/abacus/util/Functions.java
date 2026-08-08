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
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntObjFunction;

/**
 * Utility class providing various Function implementations and factory methods.
 * This class contains methods for creating indexed functions.
 */
public final class Functions {

    private Functions() {
    }

    /**
     * Returns a stateful Function that applies a function based on element index position.
     * The function maintains an internal counter that increments with each apply call.
     * This method is marked as Beta, SequentialOnly, and Stateful, indicating it should not be saved, cached for reuse, or used in parallel streams.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Functions.indexed((i, s) -> i + ":" + s).apply("hello");  // returns "0:hello"
     * }</pre>
     *
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     * @param func the IntObjFunction that accepts an index and element and produces a result
     * @return a stateful Function that applies the given IntObjFunction with an incrementing index
     * @throws IllegalArgumentException if {@code func} is {@code null}.
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static <T, R> Function<T, R> indexed(final IntObjFunction<T, ? extends R> func) throws IllegalArgumentException {
        N.checkArgNotNull(func, cs.func);

        return new Function<>() {
            private final MutableInt idx = new MutableInt(0);

            @Override
            public R apply(final T t) {
                return func.apply(idx.getAndIncrement(), t);
            }
        };
    }
}
