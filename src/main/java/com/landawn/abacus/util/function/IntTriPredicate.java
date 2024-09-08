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
public interface IntTriPredicate extends Throwables.IntTriPredicate<RuntimeException> { //NOSONAR

    IntTriPredicate ALWAYS_TRUE = (a, b, c) -> true;

    IntTriPredicate ALWAYS_FALSE = (a, b, c) -> false;

    /**
     *
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    @Override
    boolean test(int a, int b, int c);

    /**
     *
     *
     * @return
     */
    default IntTriPredicate negate() {
        return (a, b, c) -> !test(a, b, c);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    default IntTriPredicate and(final IntTriPredicate other) {
        N.checkArgNotNull(other);

        return (a, b, c) -> test(a, b, c) && other.test(a, b, c);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    default IntTriPredicate or(final IntTriPredicate other) {
        N.checkArgNotNull(other);

        return (a, b, c) -> test(a, b, c) || other.test(a, b, c);
    }
}
