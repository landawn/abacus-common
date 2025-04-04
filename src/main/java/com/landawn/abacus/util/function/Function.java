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

import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Throwables;

/**
 * Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a>
 *
 */
@FunctionalInterface
public interface Function<T, R> extends Throwables.Function<T, R, RuntimeException>, java.util.function.Function<T, R> { //NOSONAR

    /**
     *
     * @param <T>
     * @return
     */
    static <T> Function<T, T> identity() {
        return Fn.identity();
    }

    /**
     *
     * @param <V>
     * @param before
     * @return
     */
    @Override
    default <V> Function<V, R> compose(final java.util.function.Function<? super V, ? extends T> before) {
        return (final V v) -> apply(before.apply(v));
    }

    /**
     *
     * @param <V>
     * @param after
     * @return
     */
    @Override
    default <V> Function<T, V> andThen(final java.util.function.Function<? super R, ? extends V> after) {
        return (final T t) -> after.apply(apply(t));
    }

    /**
     *
     * @param <E>
     * @return
     */
    default <E extends Throwable> Throwables.Function<T, R, E> toThrowable() {
        return (Throwables.Function<T, R, E>) this;
    }
}
