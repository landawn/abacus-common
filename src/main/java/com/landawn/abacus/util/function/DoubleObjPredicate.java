/*
 * Copyright (C) 2024 HaiYang Li
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

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Throwables;

/**
 * Refer to JDK API documentation at: <a href="https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html">https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html</a>
 *
 * @author Haiyang Li
 */
public interface DoubleObjPredicate<T> extends Throwables.DoubleObjPredicate<T, RuntimeException> { // NOSONAR
    /**
     *
     *
     * @param t
     * @param u
     * @return
     */
    @Override
    boolean test(double t, T u);

    default DoubleObjPredicate<T> negate() {
        return (i, t) -> !test(i, t);
    }

    default DoubleObjPredicate<T> and(DoubleObjPredicate<T> other) {
        N.checkArgNotNull(other);

        return (i, t) -> test(i, t) && other.test(i, t);
    }

    default DoubleObjPredicate<T> or(DoubleObjPredicate<T> other) {
        N.checkArgNotNull(other);

        return (i, t) -> test(i, t) || other.test(i, t);
    }
}
