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

/**
 * Refer to JDK API documentation at: <a href="https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html">https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html</a>
 *
 * @author Haiyang Li
 */
public interface BiPredicate<T, U> extends Throwables.BiPredicate<T, U, RuntimeException>, java.util.function.BiPredicate<T, U> { //NOSONAR

    /**
     *
     *
     * @param t
     * @param u
     * @return
     */
    @Override
    boolean test(T t, U u);

    /**
     *
     *
     * @return
     */
    @Override
    default BiPredicate<T, U> negate() {
        return (t, u) -> !test(t, u);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    @Override
    default BiPredicate<T, U> and(final java.util.function.BiPredicate<? super T, ? super U> other) {
        return (t, u) -> test(t, u) && other.test(t, u);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    @Override
    default BiPredicate<T, U> or(final java.util.function.BiPredicate<? super T, ? super U> other) {
        return (t, u) -> test(t, u) || other.test(t, u);
    }

    /**
     *
     *
     * @param <E>
     * @return
     */
    default <E extends Throwable> Throwables.BiPredicate<T, U, E> toThrowable() {
        return (Throwables.BiPredicate<T, U, E>) this;
    }
}
