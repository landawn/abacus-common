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
 * @since 0.8
 *
 * @author Haiyang Li
 */
public interface ShortBiPredicate extends Throwables.ShortBiPredicate<RuntimeException> {

    ShortBiPredicate ALWAYS_TRUE = (t, u) -> true;

    ShortBiPredicate ALWAYS_FALSE = (t, u) -> false;

    ShortBiPredicate EQUAL = (t, u) -> t == u;

    ShortBiPredicate NOT_EQUAL = (t, u) -> t != u;

    ShortBiPredicate GREATER_THAN = (t, u) -> t > u;

    ShortBiPredicate GREATER_EQUAL = (t, u) -> t >= u;

    ShortBiPredicate LESS_THAN = (t, u) -> t < u;

    ShortBiPredicate LESS_EQUAL = (t, u) -> t <= u;

    @Override
    boolean test(short t, short u);

    default ShortBiPredicate negate() {
        return (t, u) -> !test(t, u);
    }

    default ShortBiPredicate and(ShortBiPredicate other) {
        N.checkArgNotNull(other);

        return (t, u) -> test(t, u) && other.test(t, u);
    }

    default ShortBiPredicate or(ShortBiPredicate other) {
        N.checkArgNotNull(other);

        return (t, u) -> test(t, u) || other.test(t, u);
    }
}
