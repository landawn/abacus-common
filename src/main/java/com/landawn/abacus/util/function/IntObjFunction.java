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
 * Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a>
 *
 */
@FunctionalInterface
public interface IntObjFunction<T, R> extends Throwables.IntObjFunction<T, R, RuntimeException> { // NOSONAR
    /**
     *
     * @param t
     * @param u
     * @return
     */
    @Override
    R apply(int t, T u);

    /**
     *
     * @param <V>
     * @param after
     * @return
     */
    default <V> IntObjFunction<T, V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (i, t) -> after.apply(apply(i, t));
    }
}
