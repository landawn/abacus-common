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
 *
 *
 * @author Haiyang Li
 */
public interface FloatBiPredicate extends Throwables.FloatBiPredicate<RuntimeException> { //NOSONAR

    FloatBiPredicate ALWAYS_TRUE = (t, u) -> true;

    FloatBiPredicate ALWAYS_FALSE = (t, u) -> false;

    FloatBiPredicate EQUAL = (t, u) -> Float.compare(t, u) == 0;

    FloatBiPredicate NOT_EQUAL = (t, u) -> Float.compare(t, u) != 0;

    FloatBiPredicate GREATER_THAN = (t, u) -> Float.compare(t, u) > 0;

    FloatBiPredicate GREATER_EQUAL = (t, u) -> Float.compare(t, u) >= 0;

    FloatBiPredicate LESS_THAN = (t, u) -> Float.compare(t, u) < 0;

    FloatBiPredicate LESS_EQUAL = (t, u) -> Float.compare(t, u) <= 0;

    /**
     *
     *
     * @param t
     * @param u
     * @return
     */
    @Override
    boolean test(float t, float u);

    /**
     *
     *
     * @return
     */
    default FloatBiPredicate negate() {
        return (t, u) -> !test(t, u);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    default FloatBiPredicate and(final FloatBiPredicate other) {
        return (t, u) -> test(t, u) && other.test(t, u);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    default FloatBiPredicate or(final FloatBiPredicate other) {
        return (t, u) -> test(t, u) || other.test(t, u);
    }
}
