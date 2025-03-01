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

/**
 * Refer to JDK API documentation at: <a href="https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html">https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html</a>
 *
 */
@FunctionalInterface
public interface LongPredicate extends Throwables.LongPredicate<RuntimeException>, java.util.function.LongPredicate { //NOSONAR

    LongPredicate ALWAYS_TRUE = value -> true;

    LongPredicate ALWAYS_FALSE = value -> false;

    LongPredicate IS_ZERO = value -> value == 0;

    LongPredicate NOT_ZERO = value -> value != 0;

    LongPredicate IS_POSITIVE = value -> value > 0;

    LongPredicate NOT_POSITIVE = value -> value <= 0;

    LongPredicate IS_NEGATIVE = value -> value < 0;

    LongPredicate NOT_NEGATIVE = value -> value >= 0;

    /**
     *
     * @param value
     * @return
     */
    @Override
    boolean test(long value);

    @Override
    default LongPredicate negate() {
        return value -> !test(value);
    }

    /**
     *
     * @param other
     * @return
     */
    @Override
    default LongPredicate or(final java.util.function.LongPredicate other) {
        return value -> test(value) || other.test(value);
    }

    /**
     *
     * @param other
     * @return
     */
    @Override
    default LongPredicate and(final java.util.function.LongPredicate other) {
        return value -> test(value) && other.test(value);
    }

    /**
     * Returns the specified instance.
     *
     * @param predicate
     * @return
     */
    static LongPredicate of(final LongPredicate predicate) {
        return predicate;
    }

    /**
     *
     * @param targetLong
     * @return
     */
    static LongPredicate equal(final long targetLong) { //NOSONAR
        return value -> value == targetLong;
    }

    /**
     *
     * @param targetLong
     * @return
     */
    static LongPredicate notEqual(final long targetLong) {
        return value -> value != targetLong;
    }

    /**
     *
     * @param targetLong
     * @return
     */
    static LongPredicate greaterThan(final long targetLong) {
        return value -> value > targetLong;
    }

    /**
     *
     * @param targetLong
     * @return
     */
    static LongPredicate greaterEqual(final long targetLong) {
        return value -> value >= targetLong;
    }

    /**
     *
     * @param targetLong
     * @return
     */
    static LongPredicate lessThan(final long targetLong) {
        return value -> value < targetLong;
    }

    /**
     *
     * @param targetLong
     * @return
     */
    static LongPredicate lessEqual(final long targetLong) {
        return value -> value <= targetLong;
    }

    /**
     *
     * @param minValue
     * @param maxValue
     * @return
     */
    static LongPredicate between(final long minValue, final long maxValue) {
        return value -> value > minValue && value < maxValue;
    }
}
