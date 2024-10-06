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

import com.landawn.abacus.util.Fn.UnaryOperators;
import com.landawn.abacus.util.Throwables;

/**
 * Refer to JDK API documentation at: <a href="https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html">https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html</a>
 *
 * @author Haiyang Li
 */
public interface UnaryOperator<T> extends Function<T, T>, Throwables.UnaryOperator<T, RuntimeException>, java.util.function.UnaryOperator<T> { //NOSONAR

    /**
     *
     * @param before
     * @return
     */
    default UnaryOperator<T> compose(final java.util.function.UnaryOperator<T> before) {
        return t -> apply(before.apply(t));
    }

    /**
     *
     *
     * @param after
     * @return
     */
    default UnaryOperator<T> andThen(final java.util.function.UnaryOperator<T> after) {
        return t -> after.apply(apply(t));
    }

    /**
     *
     *
     * @param <T>
     * @return
     */
    static <T> UnaryOperator<T> identity() {
        return UnaryOperators.identity();
    }

    /**
     *
     *
     * @param <E>
     * @return
     */
    @Override
    default <E extends Throwable> Throwables.UnaryOperator<T, E> toThrowable() {
        return (Throwables.UnaryOperator<T, E>) this;
    }
}
