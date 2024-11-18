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

/**
 * Refer to JDK API documentation at: <a href="https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html">https://docs.oracle.com/javase/8/docs/api/java/util/function/package-summary.html</a>
 *
 */
public interface IntPredicate extends Throwables.IntPredicate<RuntimeException>, java.util.function.IntPredicate { //NOSONAR

    IntPredicate ALWAYS_TRUE = value -> true;

    IntPredicate ALWAYS_FALSE = value -> false;

    IntPredicate IS_ZERO = value -> value == 0;

    IntPredicate NOT_ZERO = value -> value != 0;

    IntPredicate IS_POSITIVE = value -> value > 0;

    IntPredicate NOT_POSITIVE = value -> value <= 0;

    IntPredicate IS_NEGATIVE = value -> value < 0;

    IntPredicate NOT_NEGATIVE = value -> value >= 0;

    /**
     *
     * @param value
     * @return
     */
    @Override
    boolean test(int value);

    @Override
    default IntPredicate negate() {
        return value -> !test(value);
    }

    /**
     *
     * @param other
     * @return
     */
    @Override
    default IntPredicate and(final java.util.function.IntPredicate other) {
        return value -> test(value) && other.test(value);
    }

    /**
     *
     * @param other
     * @return
     */
    @Override
    default IntPredicate or(final java.util.function.IntPredicate other) {
        return value -> test(value) || other.test(value);
    }

    /**
     * Returns the specified instance.
     *
     * @param predicate
     * @return
     */
    static IntPredicate of(final IntPredicate predicate) {
        return predicate;
    }

    /**
     *
     * @param targetInt
     * @return
     */
    static IntPredicate equal(final int targetInt) { //NOSONAR
        return value -> value == targetInt;
    }

    /**
     *
     * @param targetInt
     * @return
     */
    static IntPredicate notEqual(final int targetInt) {
        return value -> value != targetInt;
    }

    /**
     *
     * @param targetInt
     * @return
     */
    static IntPredicate greaterThan(final int targetInt) {
        return value -> value > targetInt;
    }

    /**
     *
     * @param targetInt
     * @return
     */
    static IntPredicate greaterEqual(final int targetInt) {
        return value -> value >= targetInt;
    }

    /**
     *
     * @param targetInt
     * @return
     */
    static IntPredicate lessThan(final int targetInt) {
        return value -> value < targetInt;
    }

    /**
     *
     * @param targetInt
     * @return
     */
    static IntPredicate lessEqual(final int targetInt) {
        return value -> value <= targetInt;
    }

    /**
     *
     * @param minValue
     * @param maxValue
     * @return
     */
    static IntPredicate between(final int minValue, final int maxValue) {
        return value -> value > minValue && value < maxValue;
    }
}
