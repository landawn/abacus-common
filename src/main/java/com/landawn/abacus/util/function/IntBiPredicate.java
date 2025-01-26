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

@FunctionalInterface
public interface IntBiPredicate extends Throwables.IntBiPredicate<RuntimeException> { //NOSONAR

    IntBiPredicate ALWAYS_TRUE = (t, u) -> true;

    IntBiPredicate ALWAYS_FALSE = (t, u) -> false;

    IntBiPredicate EQUAL = (t, u) -> t == u;

    IntBiPredicate NOT_EQUAL = (t, u) -> t != u;

    IntBiPredicate GREATER_THAN = (t, u) -> t > u;

    IntBiPredicate GREATER_EQUAL = (t, u) -> t >= u;

    IntBiPredicate LESS_THAN = (t, u) -> t < u;

    IntBiPredicate LESS_EQUAL = (t, u) -> t <= u;

    /**
     *
     * @param t
     * @param u
     * @return
     */
    @Override
    boolean test(int t, int u);

    default IntBiPredicate negate() {
        return (t, u) -> !test(t, u);
    }

    /**
     *
     * @param other
     * @return
     */
    default IntBiPredicate and(final IntBiPredicate other) {
        return (t, u) -> test(t, u) && other.test(t, u);
    }

    /**
     *
     * @param other
     * @return
     */
    default IntBiPredicate or(final IntBiPredicate other) {
        return (t, u) -> test(t, u) || other.test(t, u);
    }
}
