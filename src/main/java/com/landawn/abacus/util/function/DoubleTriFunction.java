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

@FunctionalInterface
public interface DoubleTriFunction<R> extends Throwables.DoubleTriFunction<R, RuntimeException> { //NOSONAR

    /**
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    @Override
    R apply(double a, double b, double c);

    /**
     *
     * @param <V>
     * @param after
     * @return
     */
    default <V> DoubleTriFunction<V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (a, b, c) -> after.apply(apply(a, b, c));
    }
}
