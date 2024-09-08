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
public interface DoubleTriPredicate extends Throwables.DoubleTriPredicate<RuntimeException> { //NOSONAR

    DoubleTriPredicate ALWAYS_TRUE = (a, b, c) -> true;

    DoubleTriPredicate ALWAYS_FALSE = (a, b, c) -> false;

    /**
     *
     *
     * @param a
     * @param b
     * @param c
     * @return
     */
    @Override
    boolean test(double a, double b, double c);

    /**
     *
     *
     * @return
     */
    default DoubleTriPredicate negate() {
        return (a, b, c) -> !test(a, b, c);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    default DoubleTriPredicate and(final DoubleTriPredicate other) {
        N.checkArgNotNull(other);

        return (a, b, c) -> test(a, b, c) && other.test(a, b, c);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    default DoubleTriPredicate or(final DoubleTriPredicate other) {
        N.checkArgNotNull(other);

        return (a, b, c) -> test(a, b, c) || other.test(a, b, c);
    }
}
