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
public interface ByteBiPredicate extends Throwables.ByteBiPredicate<RuntimeException> {

    ByteBiPredicate ALWAYS_TRUE = (t, u) -> true;

    ByteBiPredicate ALWAYS_FALSE = (t, u) -> false;

    ByteBiPredicate EQUAL = (t, u) -> t == u;

    ByteBiPredicate NOT_EQUAL = (t, u) -> t != u;

    ByteBiPredicate GREATER_THAN = (t, u) -> t > u;

    ByteBiPredicate GREATER_EQUAL = (t, u) -> t >= u;

    ByteBiPredicate LESS_THAN = (t, u) -> t < u;

    ByteBiPredicate LESS_EQUAL = (t, u) -> t <= u;

    @Override
    boolean test(byte t, byte u);

    default ByteBiPredicate negate() {
        return (t, u) -> !test(t, u);
    }

    default ByteBiPredicate and(ByteBiPredicate other) {
        N.checkArgNotNull(other);

        return (t, u) -> test(t, u) && other.test(t, u);
    }

    default ByteBiPredicate or(ByteBiPredicate other) {
        N.checkArgNotNull(other);

        return (t, u) -> test(t, u) || other.test(t, u);
    }
}
