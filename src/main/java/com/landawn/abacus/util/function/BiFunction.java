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

import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.Throwables;

/**
 * Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a>
 *
 */
@SuppressFBWarnings("NM_SAME_SIMPLE_NAME_AS_INTERFACE")
@FunctionalInterface
public interface BiFunction<T, U, R> extends Throwables.BiFunction<T, U, R, RuntimeException>, java.util.function.BiFunction<T, U, R> { //NOSONAR

    /**
     *
     * @param t
     * @param u
     * @return
     */
    @Override
    R apply(T t, U u);

    /**
     *
     * @param <V>
     * @param after
     * @return
     */
    @Override
    default <V> BiFunction<T, U, V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (t, u) -> after.apply(apply(t, u));
    }

    /**
     *
     * @param <E>
     * @return
     */
    default <E extends Throwable> Throwables.BiFunction<T, U, R, E> toThrowable() {
        return (Throwables.BiFunction<T, U, R, E>) this;
    }
}
