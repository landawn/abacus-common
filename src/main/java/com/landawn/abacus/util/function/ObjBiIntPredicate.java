/*
 * Copyright (C) 2024 HaiYang Li
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
public interface ObjBiIntPredicate<T> extends Throwables.ObjBiIntPredicate<T, RuntimeException> { // NOSONAR

    /**
     *
     * @param t
     * @param i
     * @param j
     * @return
     */
    @Override
    boolean test(T t, int i, int j);

    default ObjBiIntPredicate<T> negate() {
        return (t, i, j) -> !test(t, i, j);
    }

    /**
     *
     * @param other
     * @return
     */
    default ObjBiIntPredicate<T> and(final ObjBiIntPredicate<T> other) {
        return (t, i, j) -> test(t, i, j) && other.test(t, i, j);
    }

    /**
     *
     * @param other
     * @return
     */
    default ObjBiIntPredicate<T> or(final ObjBiIntPredicate<T> other) {
        return (t, i, j) -> test(t, i, j) || other.test(t, i, j);
    }
}
