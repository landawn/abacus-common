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

@FunctionalInterface
public interface DoubleBiPredicate extends Throwables.DoubleBiPredicate<RuntimeException> { //NOSONAR

    DoubleBiPredicate ALWAYS_TRUE = (t, u) -> true;

    DoubleBiPredicate ALWAYS_FALSE = (t, u) -> false;
    DoubleBiPredicate EQUAL = (t, u) -> Double.compare(t, u) == 0;

    DoubleBiPredicate NOT_EQUAL = (t, u) -> Double.compare(t, u) != 0;

    DoubleBiPredicate GREATER_THAN = (t, u) -> Double.compare(t, u) > 0;

    DoubleBiPredicate GREATER_EQUAL = (t, u) -> Double.compare(t, u) >= 0;

    DoubleBiPredicate LESS_THAN = (t, u) -> Double.compare(t, u) < 0;

    DoubleBiPredicate LESS_EQUAL = (t, u) -> Double.compare(t, u) <= 0;

    /**
     *
     * @param t
     * @param u
     * @return
     */
    @Override
    boolean test(double t, double u);

    default DoubleBiPredicate negate() {
        return (t, u) -> !test(t, u);
    }

    /**
     *
     * @param other
     * @return
     */
    default DoubleBiPredicate and(final DoubleBiPredicate other) {
        return (t, u) -> test(t, u) && other.test(t, u);
    }

    /**
     *
     * @param other
     * @return
     */
    default DoubleBiPredicate or(final DoubleBiPredicate other) {
        return (t, u) -> test(t, u) || other.test(t, u);
    }
}
