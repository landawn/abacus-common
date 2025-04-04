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

import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Throwables;

/**
 * Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a>
 *
 */
@FunctionalInterface
public interface DoublePredicate extends Throwables.DoublePredicate<RuntimeException>, java.util.function.DoublePredicate { //NOSONAR
    DoublePredicate ALWAYS_TRUE = value -> true;

    DoublePredicate ALWAYS_FALSE = value -> false;

    DoublePredicate IS_ZERO = value -> N.equals(value, 0);

    DoublePredicate NOT_ZERO = value -> N.compare(value, 0) != 0;

    DoublePredicate IS_POSITIVE = value -> N.compare(value, 0) > 0;

    DoublePredicate NOT_POSITIVE = value -> N.compare(value, 0) <= 0;

    DoublePredicate IS_NEGATIVE = value -> N.compare(value, 0) < 0;

    DoublePredicate NOT_NEGATIVE = value -> N.compare(value, 0) >= 0;

    /**
     *
     * @param value
     * @return
     */
    @Override
    boolean test(double value);

    @Override
    default DoublePredicate negate() {
        return value -> !test(value);
    }

    /**
     *
     * @param other
     * @return
     */
    @Override
    default DoublePredicate and(final java.util.function.DoublePredicate other) {
        return value -> test(value) && other.test(value);
    }

    /**
     *
     * @param other
     * @return
     */
    @Override
    default DoublePredicate or(final java.util.function.DoublePredicate other) {
        return value -> test(value) || other.test(value);
    }

    /**
     * Returns the specified instance.
     *
     * @param predicate
     * @return
     */
    static DoublePredicate of(final DoublePredicate predicate) {
        return predicate;
    }

    /**
     *
     * @param targetDouble
     * @return
     */
    static DoublePredicate equal(final double targetDouble) { // NOSONAR
        return value -> N.equals(value, targetDouble);
    }

    /**
     *
     * @param targetDouble
     * @return
     */
    static DoublePredicate notEqual(final double targetDouble) {
        return value -> N.compare(value, targetDouble) != 0;
    }

    /**
     *
     * @param targetDouble
     * @return
     */
    static DoublePredicate greaterThan(final double targetDouble) {
        return value -> N.compare(value, targetDouble) > 0;
    }

    /**
     *
     * @param targetDouble
     * @return
     */
    static DoublePredicate greaterEqual(final double targetDouble) {
        return value -> N.compare(value, targetDouble) >= 0;
    }

    /**
     *
     * @param targetDouble
     * @return
     */
    static DoublePredicate lessThan(final double targetDouble) {
        return value -> N.compare(value, targetDouble) < 0;
    }

    /**
     *
     * @param targetDouble
     * @return
     */
    static DoublePredicate lessEqual(final double targetDouble) {
        return value -> N.compare(value, targetDouble) <= 0;
    }

    /**
     *
     * @param minValue
     * @param maxValue
     * @return
     */
    static DoublePredicate between(final double minValue, final double maxValue) {
        return value -> N.compare(value, minValue) > 0 && N.compare(value, maxValue) < 0;
    }
}
