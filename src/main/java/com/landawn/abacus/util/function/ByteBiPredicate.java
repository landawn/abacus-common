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
public interface ByteBiPredicate extends Throwables.ByteBiPredicate<RuntimeException> { //NOSONAR

    ByteBiPredicate ALWAYS_TRUE = (t, u) -> true;

    ByteBiPredicate ALWAYS_FALSE = (t, u) -> false;

    ByteBiPredicate EQUAL = (t, u) -> t == u;

    ByteBiPredicate NOT_EQUAL = (t, u) -> t != u;

    ByteBiPredicate GREATER_THAN = (t, u) -> t > u;

    ByteBiPredicate GREATER_EQUAL = (t, u) -> t >= u;

    ByteBiPredicate LESS_THAN = (t, u) -> t < u;

    ByteBiPredicate LESS_EQUAL = (t, u) -> t <= u;

    /**
     *
     * @param t
     * @param u
     * @return
     */
    @Override
    boolean test(byte t, byte u);

    default ByteBiPredicate negate() {
        return (t, u) -> !test(t, u);
    }

    /**
     *
     * @param other
     * @return
     */
    default ByteBiPredicate and(final ByteBiPredicate other) {
        return (t, u) -> test(t, u) && other.test(t, u);
    }

    /**
     *
     * @param other
     * @return
     */
    default ByteBiPredicate or(final ByteBiPredicate other) {
        return (t, u) -> test(t, u) || other.test(t, u);
    }
}
