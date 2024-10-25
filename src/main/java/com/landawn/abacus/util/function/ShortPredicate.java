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

public interface ShortPredicate extends Throwables.ShortPredicate<RuntimeException> { //NOSONAR

    ShortPredicate ALWAYS_TRUE = value -> true;

    ShortPredicate ALWAYS_FALSE = value -> false;

    ShortPredicate IS_ZERO = value -> value == 0;

    ShortPredicate NOT_ZERO = value -> value != 0;

    ShortPredicate IS_POSITIVE = value -> value > 0;

    ShortPredicate NOT_POSITIVE = value -> value <= 0;

    ShortPredicate IS_NEGATIVE = value -> value < 0;

    ShortPredicate NOT_NEGATIVE = value -> value >= 0;

    /**
     *
     *
     * @param value
     * @return
     */
    @Override
    boolean test(short value);

    /**
     * Returns the specified instance.
     *
     * @param predicate
     * @return
     */
    static ShortPredicate of(final ShortPredicate predicate) {
        return predicate;
    }

    default ShortPredicate negate() {
        return t -> !test(t);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    default ShortPredicate and(final ShortPredicate other) {
        return t -> test(t) && other.test(t);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    default ShortPredicate or(final ShortPredicate other) {
        return t -> test(t) || other.test(t);
    }

    /**
     *
     *
     * @param targetShort
     * @return
     */
    static ShortPredicate equal(final short targetShort) { //NOSONAR
        return value -> value == targetShort;
    }

    /**
     *
     *
     * @param targetShort
     * @return
     */
    static ShortPredicate notEqual(final short targetShort) {
        return value -> value != targetShort;
    }

    /**
     *
     *
     * @param targetShort
     * @return
     */
    static ShortPredicate greaterThan(final short targetShort) {
        return value -> value > targetShort;
    }

    /**
     *
     *
     * @param targetShort
     * @return
     */
    static ShortPredicate greaterEqual(final short targetShort) {
        return value -> value >= targetShort;
    }

    /**
     *
     *
     * @param targetShort
     * @return
     */
    static ShortPredicate lessThan(final short targetShort) {
        return value -> value < targetShort;
    }

    /**
     *
     *
     * @param targetShort
     * @return
     */
    static ShortPredicate lessEqual(final short targetShort) {
        return value -> value <= targetShort;
    }

    /**
     *
     *
     * @param minValue
     * @param maxValue
     * @return
     */
    static ShortPredicate between(final short minValue, final short maxValue) {
        return value -> value > minValue && value < maxValue;
    }
}
