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
public interface FloatPredicate extends Throwables.FloatPredicate<RuntimeException> {

    FloatPredicate ALWAYS_TRUE = value -> true;

    FloatPredicate ALWAYS_FALSE = value -> false;

    FloatPredicate IS_ZERO = value -> value == 0;

    FloatPredicate NOT_ZERO = value -> value != 0;

    FloatPredicate IS_POSITIVE = value -> value > 0;

    FloatPredicate NOT_POSITIVE = value -> value <= 0;

    FloatPredicate IS_NEGATIVE = value -> value < 0;

    FloatPredicate NOT_NEGATIVE = value -> value >= 0;

    @Override
    boolean test(float value);

    /**
     * Returns the specified instance
     *
     * @param predicate
     * @return
     */
    static FloatPredicate of(final FloatPredicate predicate) {
        N.checkArgNotNull(predicate);

        return predicate;
    }

    default FloatPredicate negate() {
        return t -> !test(t);
    }

    default FloatPredicate and(FloatPredicate other) {
        N.checkArgNotNull(other);

        return t -> test(t) && other.test(t);
    }

    default FloatPredicate or(FloatPredicate other) {
        N.checkArgNotNull(other);

        return t -> test(t) || other.test(t);
    }

    static FloatPredicate equal(float targetFloat) {
        return value -> value == targetFloat;
    }

    static FloatPredicate notEqual(float targetFloat) {
        return value -> value != targetFloat;
    }

    static FloatPredicate greaterThan(float targetFloat) {
        return value -> N.compare(value, targetFloat) > 0;
    }

    static FloatPredicate greaterEqual(float targetFloat) {
        return value -> N.compare(value, targetFloat) >= 0;
    }

    static FloatPredicate lessThan(float targetFloat) {
        return value -> N.compare(value, targetFloat) < 0;
    }

    static FloatPredicate lessEqual(float targetFloat) {
        return value -> N.compare(value, targetFloat) <= 0;
    }

    static FloatPredicate between(float minValue, float maxValue) {
        return value -> N.compare(value, minValue) > 0 && N.compare(value, maxValue) < 0;
    }
}
