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
 */
public interface Predicate<T> extends Throwables.Predicate<T, RuntimeException>, java.util.function.Predicate<T> { //NOSONAR

    /**
     *
     *
     * @param value
     * @return
     */
    @Override
    boolean test(T value);

    @Override
    default Predicate<T> negate() {
        return t -> !test(t);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    @Override
    default Predicate<T> and(final java.util.function.Predicate<? super T> other) {
        return t -> test(t) && other.test(t);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    @Override
    default Predicate<T> or(final java.util.function.Predicate<? super T> other) {
        return t -> test(t) || other.test(t);
    }

    /**
     *
     *
     * @param <E>
     * @return
     */
    default <E extends Throwable> Throwables.Predicate<T, E> toThrowable() {
        return (Throwables.Predicate<T, E>) this;
    }
}
