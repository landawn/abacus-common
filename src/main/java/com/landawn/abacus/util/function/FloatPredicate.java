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
public interface FloatPredicate extends Throwables.FloatPredicate<RuntimeException> { //NOSONAR

    FloatPredicate ALWAYS_TRUE = value -> true;

    FloatPredicate ALWAYS_FALSE = value -> false;

    FloatPredicate IS_ZERO = value -> N.equals(value, 0);

    FloatPredicate NOT_ZERO = value -> N.compare(value, 0) != 0;

    FloatPredicate IS_POSITIVE = value -> N.compare(value, 0) > 0;

    FloatPredicate NOT_POSITIVE = value -> N.compare(value, 0) <= 0;

    FloatPredicate IS_NEGATIVE = value -> N.compare(value, 0) < 0;

    FloatPredicate NOT_NEGATIVE = value -> N.compare(value, 0) >= 0;

    /**
     *
     *
     * @param value
     * @return
     */
    @Override
    boolean test(float value);

    /**
     * Returns the specified instance.
     *
     * @param predicate
     * @return
     */
    static FloatPredicate of(final FloatPredicate predicate) {
        N.checkArgNotNull(predicate);

        return predicate;
    }

    /**
     *
     *
     * @return
     */
    default FloatPredicate negate() {
        return t -> !test(t);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    default FloatPredicate and(FloatPredicate other) {
        N.checkArgNotNull(other);

        return t -> test(t) && other.test(t);
    }

    /**
     *
     *
     * @param other
     * @return
     */
    default FloatPredicate or(FloatPredicate other) {
        N.checkArgNotNull(other);

        return t -> test(t) || other.test(t);
    }

    /**
     *
     *
     * @param targetFloat
     * @return
     */
    static FloatPredicate equal(float targetFloat) { // NOSONAR
        return value -> N.equals(value, targetFloat);
    }

    /**
     *
     *
     * @param targetFloat
     * @return
     */
    static FloatPredicate notEqual(float targetFloat) {
        return value -> N.compare(value, targetFloat) != 0;
    }

    /**
     *
     *
     * @param targetFloat
     * @return
     */
    static FloatPredicate greaterThan(float targetFloat) {
        return value -> N.compare(value, targetFloat) > 0;
    }

    /**
     *
     *
     * @param targetFloat
     * @return
     */
    static FloatPredicate greaterEqual(float targetFloat) {
        return value -> N.compare(value, targetFloat) >= 0;
    }

    /**
     *
     *
     * @param targetFloat
     * @return
     */
    static FloatPredicate lessThan(float targetFloat) {
        return value -> N.compare(value, targetFloat) < 0;
    }

    /**
     *
     *
     * @param targetFloat
     * @return
     */
    static FloatPredicate lessEqual(float targetFloat) {
        return value -> N.compare(value, targetFloat) <= 0;
    }

    /**
     *
     *
     * @param minValue
     * @param maxValue
     * @return
     */
    static FloatPredicate between(float minValue, float maxValue) {
        return value -> N.compare(value, minValue) > 0 && N.compare(value, maxValue) < 0;
    }
}
