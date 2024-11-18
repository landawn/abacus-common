/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util.function;

import com.landawn.abacus.util.Throwables;

public interface TriFunction<A, B, C, R> extends Throwables.TriFunction<A, B, C, R, RuntimeException> { //NOSONAR

    /**
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    @Override
    R apply(A a, B b, C c);

    /**
     *
     * @param <V>
     * @param after
     * @return
     */
    default <V> TriFunction<A, B, C, V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (a, b, c) -> after.apply(apply(a, b, c));
    }

    /**
     *
     * @param <E>
     * @return
     */
    default <E extends Throwable> Throwables.TriFunction<A, B, C, R, E> toThrowable() {
        return (Throwables.TriFunction<A, B, C, R, E>) this;
    }
}
