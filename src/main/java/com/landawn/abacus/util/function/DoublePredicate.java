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
 * Refer to JDK API documentation at: <a href="https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html">https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html</a>
 * @since 0.8
 *
 * @author Haiyang Li
 */
public interface DoublePredicate extends Throwables.DoublePredicate<RuntimeException>, java.util.function.DoublePredicate { //NOSONAR
    DoublePredicate ALWAYS_TRUE = value -> true;

    DoublePredicate ALWAYS_FALSE = value -> false;

    DoublePredicate IS_ZERO = value -> value == 0;

    DoublePredicate NOT_ZERO = value -> value != 0;

    DoublePredicate IS_POSITIVE = value -> value > 0;

    DoublePredicate NOT_POSITIVE = value -> value <= 0;

    DoublePredicate IS_NEGATIVE = value -> value < 0;

    DoublePredicate NOT_NEGATIVE = value -> value >= 0;

    /**
     * 
     *
     * @param value 
     * @return 
     */
    @Override
    boolean test(double value);

    /**
     * 
     *
     * @return 
     */
    @Override
    default DoublePredicate negate() {
        return value -> !test(value);
    }

    /**
     * 
     *
     * @param other 
     * @return 
     */
    @Override
    default DoublePredicate and(java.util.function.DoublePredicate other) {
        N.checkArgNotNull(other);
        return value -> test(value) && other.test(value);
    }

    /**
     * 
     *
     * @param other 
     * @return 
     */
    @Override
    default DoublePredicate or(java.util.function.DoublePredicate other) {
        N.checkArgNotNull(other);
        return value -> test(value) || other.test(value);
    }

    /**
     * Returns the specified instance.
     *
     * @param predicate 
     * @return 
     */
    static DoublePredicate of(final DoublePredicate predicate) {
        N.checkArgNotNull(predicate);

        return predicate;
    }

    /**
     * 
     *
     * @param targetDouble 
     * @return 
     */
    static DoublePredicate equal(double targetDouble) { //NOSONAR
        return value -> value == targetDouble;
    }

    /**
     * 
     *
     * @param targetDouble 
     * @return 
     */
    static DoublePredicate notEqual(double targetDouble) {
        return value -> value != targetDouble;
    }

    /**
     * 
     *
     * @param targetDouble 
     * @return 
     */
    static DoublePredicate greaterThan(double targetDouble) {
        return value -> N.compare(value, targetDouble) > 0;
    }

    /**
     * 
     *
     * @param targetDouble 
     * @return 
     */
    static DoublePredicate greaterEqual(double targetDouble) {
        return value -> N.compare(value, targetDouble) >= 0;
    }

    /**
     * 
     *
     * @param targetDouble 
     * @return 
     */
    static DoublePredicate lessThan(double targetDouble) {
        return value -> N.compare(value, targetDouble) < 0;
    }

    /**
     * 
     *
     * @param targetDouble 
     * @return 
     */
    static DoublePredicate lessEqual(double targetDouble) {
        return value -> N.compare(value, targetDouble) <= 0;
    }

    /**
     * 
     *
     * @param minValue 
     * @param maxValue 
     * @return 
     */
    static DoublePredicate between(double minValue, double maxValue) {
        return value -> N.compare(value, minValue) > 0 && N.compare(value, maxValue) < 0;
    }
}
