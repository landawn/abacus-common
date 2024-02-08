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

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Throwables;

/**
 *
 *
 * @author Haiyang Li
 */
public interface BooleanBiPredicate extends Throwables.BooleanBiPredicate<RuntimeException> { //NOSONAR

    BooleanBiPredicate ALWAYS_TRUE = (t, u) -> true;

    BooleanBiPredicate ALWAYS_FALSE = (t, u) -> false;

    BooleanBiPredicate BOTH_TRUE = (t, u) -> t && u;

    BooleanBiPredicate BOTH_FALSE = (t, u) -> !t && !u;

    BooleanBiPredicate EQUAL = (t, u) -> t == u;

    BooleanBiPredicate NOT_EQUAL = (t, u) -> t != u;

    /**
     *
     *
     * @param t
     * @param u
     * @return
     */
    @Override
    boolean test(boolean t, boolean u);

    /**
     *
     *
     * @return
     */
    default BooleanBiPredicate negate() {
        return (t, u) -> !test(t, u);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    default BooleanBiPredicate and(BooleanBiPredicate other) {
        N.checkArgNotNull(other);

        return (t, u) -> test(t, u) && other.test(t, u);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    default BooleanBiPredicate or(BooleanBiPredicate other) {
        N.checkArgNotNull(other);

        return (t, u) -> test(t, u) || other.test(t, u);
    }
}
